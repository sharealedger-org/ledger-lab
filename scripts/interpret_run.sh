#!/usr/bin/env bash
# Interpret a canonical evidence bundle without rereading financial artifacts.

set -euo pipefail

if [[ $# -lt 1 || $# -gt 2 ]]; then
  echo "Usage: bash scripts/interpret_run.sh METRICS_DIR [REPORT_DIR]" >&2
  exit 1
fi

METRICS_DIR="${1%/}"
REPORT_DIR="${2:-$(dirname "$METRICS_DIR")/interpretation}"
mkdir -p "$REPORT_DIR"

MANIFEST="$METRICS_DIR/run_manifest.csv"
PROCESSES="$METRICS_DIR/process_metrics.csv"
PARTITIONS="$METRICS_DIR/partition_catalog.csv"
CONTROLS="$METRICS_DIR/reconciliation_results.csv"
SJE_ENGINE_METRICS="$METRICS_DIR/sje_engine_metrics.csv"

missing_evidence=""
for evidence_file in "$MANIFEST" "$PROCESSES" "$PARTITIONS" "$CONTROLS"; do
  if [[ ! -f "$evidence_file" ]]; then
    name="$(basename "$evidence_file")"
    [[ -z "$missing_evidence" ]] || missing_evidence+=","
    missing_evidence+="$name"
  fi
done

optional_missing=""
for optional_file in sje_engine_metrics.csv perspective_metrics.csv; do
  if [[ ! -f "$METRICS_DIR/$optional_file" ]]; then
    [[ -z "$optional_missing" ]] || optional_missing+=","
    optional_missing+="$optional_file"
  fi
done

run_id="unknown"
run_status="partial"
curve=""
config=""
start_time=""
end_time=""
wall_seconds=""
git_revision=""
# Detect a real python3 binary (not the Windows App Execution Alias .exe stub).
# Resolve symlinks: the alias is a .exe at the readlink-resolved target.
_py3_interp=""
_py3_interp=$(type -P python3 2>/dev/null || true)
if [[ -n "$_py3_interp" ]]; then
  _py3_resolved=$(readlink -f "$_py3_interp" 2>/dev/null || echo "$_py3_interp")
  case "$_py3_resolved" in *.exe) _py3_interp="" ;; esac
fi

if [[ -f "$MANIFEST" ]]; then
  if [[ -n "$_py3_interp" ]]; then
    manifest_values="$(python3 - "$MANIFEST" <<'PY'
import csv
import sys

with open(sys.argv[1], newline="", encoding="utf-8") as manifest:
    row = next(csv.DictReader(manifest), {})

fields = ("run_id", "point_id", "curve", "git_revision", "start_time", "end_time", "wall_seconds", "status")
print("\t".join(row.get(field, "") for field in fields))
PY
)"
    IFS=$'\t' read -r run_id point_id curve git_revision start_time end_time wall_seconds run_status <<< "$manifest_values"
  else
    # Fallback: parse the manifest CSV with awk when python3 is unavailable.
    manifest_values="$(awk -F',' 'NR==2 { print $1"\t"$2"\t"$3"\t"$4"\t"$5"\t"$6"\t"$7"\t"$8 }' "$MANIFEST")"
    IFS=$'\t' read -r run_id point_id curve git_revision start_time end_time wall_seconds run_status <<< "$manifest_values"
  fi
  [[ -n "$run_id" ]] || run_id="unknown"
  [[ -n "$run_status" ]] || run_status="partial"
  config="${point_id#*-}"
fi

count_rows() {
  local file="$1"
  [[ -f "$file" ]] && awk 'NR > 1 { count++ } END { print count + 0 }' "$file" || printf '0\n'
}

process_count="$(count_rows "$PROCESSES")"
partition_count="$(count_rows "$PARTITIONS")"
control_count="$(count_rows "$CONTROLS")"
engine_count="$(count_rows "$SJE_ENGINE_METRICS")"
failed_controls="0"
passed_controls="0"
if [[ -f "$CONTROLS" ]]; then
  failed_controls="$(awk -F',' 'NR > 1 && $9 ~ /FAIL/ { count++ } END { print count + 0 }' "$CONTROLS")"
  passed_controls="$(awk -F',' 'NR > 1 && $9 ~ /PASS/ { count++ } END { print count + 0 }' "$CONTROLS")"
fi

if [[ "$process_count" -gt 0 && "$run_status" == "complete" ]]; then
  run_activity="active"
else
  run_activity="failed"
fi

if [[ "$failed_controls" -gt 0 ]]; then
  evidence_assessment="contradicts"
elif [[ "$control_count" -gt 0 && -z "$missing_evidence" ]]; then
  evidence_assessment="supports"
else
  evidence_assessment="inconclusive"
fi

interpretation_csv="$REPORT_DIR/interpretation.csv"
cat > "$interpretation_csv" <<EOF
run_id,implementation_maturity,run_activity,evidence_assessment,process_count,partition_count,engine_count,reconciliation_count,reconciliation_failures,missing_evidence,git_revision
"$run_id","partial","$run_activity","$evidence_assessment","$process_count","$partition_count","$engine_count","$control_count","$failed_controls","$missing_evidence","$git_revision"
EOF

report_file="$REPORT_DIR/run_report.md"
cat > "$report_file" <<EOF
# Ledger Lab Run Report

- Run ID: $run_id
- Curve: $curve
- Configuration: $config
- Git revision: $git_revision
- Run status: $run_status
- Run activity: $run_activity
- Evidence assessment: $evidence_assessment
- Wall seconds: $wall_seconds

## Actual execution

The evidence bundle records $process_count process rows, $partition_count partition rows, and
$engine_count SJE-engine invocations. Engine metrics describe the booked-currency-revaluation
process actually run. This workload uses the legacy VA posting substrate and does not establish
generalized GenevaERS CKB or 05 Perspective maturity.

## Controls

- Reconciliation controls recorded: $control_count
- Controls passed: $passed_controls
- Controls failed: $failed_controls

## Evidence completeness

Required first-slice evidence: ${missing_evidence:-present}

Expected future evidence not emitted by this workload: ${optional_missing:-none}

## Interpretation

This run provides functional fixture evidence for the recorded process, partition, and balance
controls. It does not establish production-scale cost behavior, complete layer coverage, or the
presence of sort, engine, and perspective metrics that were not emitted by this workload.
EOF

printf 'Interpretation written to %s and %s\n' "$interpretation_csv" "$report_file"
