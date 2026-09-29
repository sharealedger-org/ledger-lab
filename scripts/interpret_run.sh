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

strip_quotes() {
  local value="$1"
  value="${value#\"}"
  value="${value%\"}"
  printf '%s' "$value"
}

run_id="unknown"
run_status="partial"
curve=""
config=""
start_time=""
end_time=""
wall_seconds=""
git_revision=""
if [[ -f "$MANIFEST" ]]; then
  manifest_row="$(sed -n '2p' "$MANIFEST")"
  IFS=',' read -r run_id _ _ point_id curve _ _ _ _ git_revision _ _ _ _ _ start_time end_time wall_seconds run_status _ <<< "$manifest_row"
  run_id="$(strip_quotes "$run_id")"
  point_id="$(strip_quotes "${point_id:-}")"
  curve="$(strip_quotes "${curve:-}")"
  git_revision="$(strip_quotes "${git_revision:-}")"
  start_time="$(strip_quotes "${start_time:-}")"
  end_time="$(strip_quotes "${end_time:-}")"
  wall_seconds="$(strip_quotes "${wall_seconds:-}")"
  run_status="$(strip_quotes "${run_status:-partial}")"
  config="${point_id#*-}"
fi

count_rows() {
  local file="$1"
  [[ -f "$file" ]] && awk 'NR > 1 { count++ } END { print count + 0 }' "$file" || printf '0\n'
}

process_count="$(count_rows "$PROCESSES")"
partition_count="$(count_rows "$PARTITIONS")"
control_count="$(count_rows "$CONTROLS")"
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
run_id,implementation_maturity,run_activity,evidence_assessment,process_count,partition_count,reconciliation_count,reconciliation_failures,missing_evidence,git_revision
"$run_id","partial","$run_activity","$evidence_assessment","$process_count","$partition_count","$control_count","$failed_controls","$missing_evidence","$git_revision"
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

The evidence bundle records $process_count process rows and $partition_count partition rows.
The financial path is the legacy VA implementation; this report does not infer generalized 04
Engine or 05 Perspective maturity from similarly named legacy steps.

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
