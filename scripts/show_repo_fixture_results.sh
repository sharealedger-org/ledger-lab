#!/usr/bin/env bash
# Print a compact, readable summary of repository fixture output.

set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: bash scripts/show_repo_fixture_results.sh OUTPUT_DIR" >&2
  exit 1
fi

OUTPUT_DIR="$1"
SORTED_FILE="$OUTPUT_DIR/SortedJEFY03q1exp.csv"
LEDGER_FILE="$OUTPUT_DIR/LDGR2003.csv"
LOG_FILE="$OUTPUT_DIR/pipeline_results.log"
COST_FILE="$OUTPUT_DIR/cost_surface.csv"
REPORT_FILE="$OUTPUT_DIR/interpretation/run_report.md"

for required_file in "$SORTED_FILE" "$LEDGER_FILE" "$LOG_FILE" "$COST_FILE"; do
  if [[ ! -f "$required_file" ]]; then
    echo "Missing fixture output: $required_file" >&2
    exit 1
  fi
done

echo "=== Sorted journal sample ==="
printf '%-12s %-8s %-14s %-8s %-12s %12s\n' \
  "journal" "line" "agency" "account" "instrument" "amount"
awk -F',' 'NR <= 3 {
  printf "%-12s %-8s %-14s %-8s %-12s %12.2f\n", $4, $5, $13, $17, $1, $26
}' "$SORTED_FILE"

echo
echo "=== Ledger sample ==="
printf '%-8s %-8s %-8s %-12s %-8s %14s\n' \
  "ledger" "agency" "center" "account" "period" "balance"
awk -F',' 'NR > 1 && NR <= 3 {
  printf "%-8s %-8s %-8s %-12s %-8s %14.2f\n", $4, $9, $10, $13, $19, $20
}' "$LEDGER_FILE"

echo
echo "=== Counts and control ==="
printf 'sorted journal rows: '
awk 'END { print NR }' "$SORTED_FILE"
printf 'ledger rows:         '
awk 'END { print NR - 1 }' "$LEDGER_FILE"
printf 'ledger amount sum:    '
awk -F',' 'NR > 1 { total += $20 } END { printf "%.2f\n", total }' "$LEDGER_FILE"

echo
echo "=== Run summary ==="
grep -E 'START  step=|RESULT step=|Pipeline complete|total_compute_s|master_file_count' "$LOG_FILE" || true

echo
echo "=== Cost record ==="
tail -n 1 "$COST_FILE"

if [[ -f "$REPORT_FILE" ]]; then
  echo
  echo "=== Interpretation ==="
  cat "$REPORT_FILE"
fi
