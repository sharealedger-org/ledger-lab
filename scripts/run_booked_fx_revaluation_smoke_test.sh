#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
KEEP_OUTPUT=0

if [[ $# -gt 1 || ( $# -eq 1 && "$1" != "--keep-output" ) ]]; then
  echo "Usage: bash scripts/run_booked_fx_revaluation_smoke_test.sh [--keep-output]" >&2
  exit 1
fi
[[ $# -eq 0 ]] || KEEP_OUTPUT=1

SCRATCH_ROOT="$REPO_ROOT/session-run.tmp"
mkdir -p "$SCRATCH_ROOT"
RUN_ROOT="$(mktemp -d "$SCRATCH_ROOT/booked-fx.XXXXXX")"
INPUT="$RUN_ROOT/input"
OUTPUT="$RUN_ROOT/output"
mkdir -p "$INPUT" "$OUTPUT"

cleanup() {
  if [[ "$KEEP_OUTPUT" == "1" ]]; then
    printf 'BOOKED_FX_RUN_ROOT=%s\n' "$RUN_ROOT"
  else
    rm -rf "$RUN_ROOT"
  fi
}
trap cleanup EXIT

cp "$REPO_ROOT/data/synthetic/booked_fx_rates.csv" "$INPUT/"
cp "$REPO_ROOT/data/synthetic/booked_fx_rules.csv" "$INPUT/"
cp "$REPO_ROOT/data/synthetic/booked_fx_opening_ledger.csv" "$OUTPUT/LDGR2025_OPENING.csv"
cp "$REPO_ROOT/data/synthetic/booked_fx_opening_ledger.csv" "$OUTPUT/LDGR2025.csv"

bash "$SCRIPT_DIR/run_pipeline.sh" \
  --inPath "$INPUT" \
  --outPath "$OUTPUT" \
  --years 25 \
  --steps 12,3 \
  --curve CFX \
  --config booked-currency-revaluation \
  --profile booked-currency-revaluation \
  --materialization persist-sjes

SJE_FILE="$OUTPUT/SortedJEFY25_FXR.csv"
LEDGER_FILE="$OUTPUT/LDGR2025.csv"
METRICS="$OUTPUT/metrics"
INTERPRETATION="$OUTPUT/interpretation"

[[ -f "$SJE_FILE" && -f "$LEDGER_FILE" ]]
[[ -f "$METRICS/run_manifest.csv" && -f "$METRICS/sje_engine_metrics.csv" ]]
[[ -f "$INTERPRETATION/interpretation.csv" && -f "$INTERPRETATION/run_report.md" ]]

awk -F',' '
  NF != 39 { printf "SJE row %d has %d fields, expected 39\n", NR, NF; bad = 1 }
  { rows++; groups[$4] += $26; total += $26 }
  END {
    for (journal in groups) {
      if (groups[journal] < -0.01 || groups[journal] > 0.01) {
        printf "Journal %s is unbalanced: %.2f\n", journal, groups[journal]
        bad = 1
      }
    }
    if (rows != 4 || length(groups) != 2 || total < -0.01 || total > 0.01) {
      printf "Unexpected SJE result: rows=%d groups=%d total=%.2f\n", rows, length(groups), total
      bad = 1
    }
    exit bad
  }
' "$SJE_FILE"

awk -F',' '
  NR > 1 {
    total += $20
    if ($1 == "INST-1001" && $13 == "EXP265" && $15 == "CAD" && $17 == "USD") cad = $20
    if ($1 == "INST-1002" && $13 == "EXP265" && $15 == "EUR" && $17 == "USD") eur = $20
    if ($1 == "INST-1003" && $13 == "EXP6" && $15 == "USD") domestic = $20
  }
  END {
    if (cad != "570.66" || eur != "556.67" || domestic != "300.00" || total < 1418.93 || total > 1418.95) {
      printf "Unexpected posted ledger: CAD=%s EUR=%s USD=%s total=%.2f\n", cad, eur, domestic, total
      exit 1
    }
  }
' "$LEDGER_FILE"

awk -F',' 'NR > 1 { status = $9; gsub(/"/, "", status); if (status != "PASS") { print "Failed control:", $2, status; bad = 1 } } END { exit bad }' \
  "$METRICS/reconciliation_results.csv"
grep -q '"complete"' "$METRICS/run_manifest.csv"
grep -q '"supports"' "$INTERPRETATION/interpretation.csv"
grep -q '1 SJE-engine invocations' "$INTERPRETATION/run_report.md"

printf 'Booked FX revaluation smoke test passed.\n'