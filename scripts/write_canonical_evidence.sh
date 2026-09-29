#!/usr/bin/env bash
# Write the first canonical evidence bundle for a completed VA fixture run.

set -euo pipefail

RUN_ID=""
RUN_TIMESTAMP=""
IN_PATH=""
OUT_PATH=""
YEARS=""
STEPS=""
CURVE=""
CONFIG_DESC=""
START_EPOCH=""
END_EPOCH=""
TOTAL_COMPUTE_S=""
LOG_FILE=""
STATUS="complete"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --run-id) RUN_ID="$2"; shift 2 ;;
    --run-timestamp) RUN_TIMESTAMP="$2"; shift 2 ;;
    --in-path) IN_PATH="$2"; shift 2 ;;
    --out-path) OUT_PATH="$2"; shift 2 ;;
    --years) YEARS="$2"; shift 2 ;;
    --steps) STEPS="$2"; shift 2 ;;
    --curve) CURVE="$2"; shift 2 ;;
    --config) CONFIG_DESC="$2"; shift 2 ;;
    --start-epoch) START_EPOCH="$2"; shift 2 ;;
    --end-epoch) END_EPOCH="$2"; shift 2 ;;
    --compute-seconds) TOTAL_COMPUTE_S="$2"; shift 2 ;;
    --log-file) LOG_FILE="$2"; shift 2 ;;
    --status) STATUS="$2"; shift 2 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

for required_value in RUN_ID RUN_TIMESTAMP IN_PATH OUT_PATH YEARS STEPS START_EPOCH END_EPOCH TOTAL_COMPUTE_S LOG_FILE; do
  if [[ -z "${!required_value}" ]]; then
    echo "Missing required option for $required_value" >&2
    exit 1
  fi
done

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
METRICS_ROOT="${OUT_PATH%/}/metrics"
mkdir -p "$METRICS_ROOT"

csv_quote() {
  local value="${1//\"/\"\"}"
  printf '"%s"' "$value"
}

append_csv_row() {
  local output_file="$1"
  shift
  local row=""
  local value
  for value in "$@"; do
    [[ -z "$row" ]] || row+=","
    row+="$(csv_quote "$value")"
  done
  printf '%s\n' "$row" >> "$output_file"
}

init_file() {
  local file="$1"
  local header="$2"
  [[ -f "$file" ]] || printf '%s\n' "$header" > "$file"
}

file_rows() {
  local file="$1"
  [[ -f "$file" ]] && awk 'END { print NR }' "$file" || printf '0\n'
}

file_bytes() {
  local file="$1"
  [[ -f "$file" ]] && wc -c < "$file" | tr -d ' ' || printf '0\n'
}

file_checksum() {
  local file="$1"
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$file" | awk '{print $1}'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$file" | awk '{print $1}'
  else
    printf 'unavailable\n'
  fi
}

RUN_END_TIMESTAMP="$(date '+%Y-%m-%d %H:%M:%S')"
WALL_SECONDS=$((END_EPOCH - START_EPOCH))
GIT_REVISION="$(git -C "$REPO_ROOT" rev-parse HEAD 2>/dev/null || printf 'unknown')"
HOST_ID="$(hostname 2>/dev/null || printf 'unknown')"
OS_VERSION="$(uname -srm 2>/dev/null || printf 'unknown')"

RUN_MANIFEST="$METRICS_ROOT/run_manifest.csv"
PROCESS_METRICS="$METRICS_ROOT/process_metrics.csv"
PARTITION_CATALOG="$METRICS_ROOT/partition_catalog.csv"
RECONCILIATIONS="$METRICS_ROOT/reconciliation_results.csv"
SORT_EVENTS="$METRICS_ROOT/sort_events.csv"

init_file "$RUN_MANIFEST" 'run_id,experiment_id,experiment_version,point_id,curve,configuration_hash,rules_hash,viewspec_hash,input_snapshot_id,git_revision,engine_version,host_id,os_version,java_version,scala_version,start_time,end_time,wall_seconds,status,replicate_number'
init_file "$PROCESS_METRICS" 'run_id,process_id,parent_process_id,layer,process_name,engine_name,input_partition_ids,output_partition_ids,ckb_process_id,pass_id,start_time,end_time,wall_seconds,cpu_user_seconds,cpu_system_seconds,cpu_total_seconds,peak_rss_bytes,peak_jvm_heap_bytes,pageins,context_switches,input_rows,input_bytes,output_rows,output_bytes,spill_bytes,status'
init_file "$PARTITION_CATALOG" 'run_id,partition_id,partition_type,logical_layer,producer_process_id,parent_partition_ids,path,row_count,byte_count,checksum,sorted,sort_spec_id,first_key,last_key,min_period,max_period,created_at'
init_file "$RECONCILIATIONS" 'run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason'
init_file "$SORT_EVENTS" 'run_id,sort_id,process_id,pass_id,input_partition_id,output_partition_id,sort_spec_id,sort_spec_hash,sort_key,input_rows,output_rows,input_bytes,output_bytes,chunk_size,spill_file_count,spill_bytes,peak_memory_bytes,elapsed_seconds,status'

append_csv_row "$RUN_MANIFEST" \
  "$RUN_ID" "legacy-va" "1" "$CURVE-$CONFIG_DESC" "$CURVE" "" "" "" "" \
  "$GIT_REVISION" "" "$HOST_ID" "$OS_VERSION" "" "2.12.18" "$RUN_TIMESTAMP" \
  "$RUN_END_TIMESTAMP" "$WALL_SECONDS" "$STATUS" "1"

append_partition() {
  local partition_id="$1" partition_type="$2" logical_layer="$3"
  local producer="$4" parents="$5" path="$6" rows="$7" sorted="$8" sort_spec="$9"
  [[ -f "$path" ]] || return 0
  append_csv_row "$PARTITION_CATALOG" "$RUN_ID" "$partition_id" "$partition_type" \
    "$logical_layer" "$producer" "$parents" "$path" "$rows" "$(file_bytes "$path")" \
    "$(file_checksum "$path")" "$sorted" "$sort_spec" "" "" "" "" "$RUN_END_TIMESTAMP"
}

append_process() {
  local step="$1" year="$2" short_year="${year: -2}"
  local process_id="${RUN_ID}-step${step}-fy${short_year}"
  local input_file="" output_file="" input_partition="" output_partition=""
  local input_rows="" output_rows="" input_bytes="" output_bytes=""
  case "$step" in
    2)
      input_file="$IN_PATH/FY${short_year}q1exp.txt"
      output_file="$OUT_PATH/SortedJEFY${short_year}q1exp.csv"
      input_partition="raw_fy${short_year}q1exp"
      output_partition="sje_fy${short_year}q1exp"
      input_rows="$(file_rows "$input_file")"
      output_rows="$(file_rows "$output_file")"
      ;;
    3)
      input_file="$OUT_PATH/SortedJEFY${short_year}q1exp.csv"
      output_file="$OUT_PATH/LDGR20${short_year}.csv"
      input_partition="sje_fy${short_year}q1exp"
      output_partition="ledger_fy20${short_year}"
      input_rows="$(file_rows "$input_file")"
      output_rows="$(($(file_rows "$output_file") - 1))"
      ;;
    *) return 0 ;;
  esac
  input_bytes="$(file_bytes "$input_file")"
  output_bytes="$(file_bytes "$output_file")"
  step_elapsed="$(grep -E "RESULT step=${step} year=${year} " "$LOG_FILE" | \
    sed -E 's/.*elapsed_s=([0-9]+).*/\1/' | tail -1)"
  [[ "$step_elapsed" =~ ^[0-9]+$ ]] || step_elapsed=""
  peak_rss_bytes="$(grep -E "RESOURCE step=${step} year=${year} " "$LOG_FILE" | \
    sed -E 's/.*peak_rss_bytes=([0-9]+).*/\1/' | tail -1)"
  [[ "$peak_rss_bytes" =~ ^[0-9]+$ ]] || peak_rss_bytes=""
  append_csv_row "$PROCESS_METRICS" "$RUN_ID" "$process_id" "" "02-foundation" \
    "step-${step}" "legacy-va" "$input_partition" "$output_partition" "" "$step" \
    "$RUN_TIMESTAMP" "$RUN_END_TIMESTAMP" "$step_elapsed" "" "" "" "$peak_rss_bytes" "" "" "" \
    "" "" "" "" "$input_rows" "$input_bytes" "$output_rows" "$output_bytes" "" "complete"
  if [[ "$step" == "2" ]]; then
    append_partition "$input_partition" "raw_source" "01-transformation" "$process_id" "" \
      "$input_file" "$input_rows" "" ""
    append_partition "$output_partition" "sje" "02-foundation" "$process_id" "$input_partition" \
      "$output_file" "$output_rows" "Y" "SortedJE"
    append_csv_row "$SORT_EVENTS" "$RUN_ID" "${RUN_ID}-sort-fy${short_year}q1exp" \
      "$process_id" "1" "$input_partition" "$output_partition" "SortedJE" "" \
      "instrument,ledger,journal,book,agency,fund,object,product,nominal,alt,currency-source,currency-type-source,currency-target,currency-type-target,fiscal-period" \
      "$input_rows" "$output_rows" "$input_bytes" "$output_bytes" "200000" "" "" "" \
      "$step_elapsed" "complete"
  else
    append_partition "$output_partition" "ledger_master" "03-instrument-ledger" "$process_id" \
      "$input_partition" "$output_file" "$output_rows" "" ""
  fi
}

append_control() {
  local check_id="$1" partition_id="$2" expected="$3" actual="$4" rows="$5"
  local delta status
  delta=$(awk -v expected="$expected" -v actual="$actual" 'BEGIN { printf "%.2f", actual - expected }')
  status=$(awk -v delta="$delta" 'BEGIN { print (delta < -0.01 || delta > 0.01) ? "FAIL" : "PASS" }')
  append_csv_row "$RECONCILIATIONS" "$RUN_ID" "$check_id" "fixture" "$partition_id" \
    "$expected" "$actual" "$delta" "$rows" "$status" ""
}

IFS=',' read -ra YEAR_LIST <<< "$YEARS"
IFS=',' read -ra STEP_LIST <<< "$STEPS"
for year in "${YEAR_LIST[@]}"; do
  for step in "${STEP_LIST[@]}"; do
    append_process "$step" "$year"
  done
  short_year="${year: -2}"
  sorted_file="$OUT_PATH/SortedJEFY${short_year}q1exp.csv"
  ledger_file="$OUT_PATH/LDGR20${short_year}.csv"
  if [[ -f "$sorted_file" ]]; then
    append_control "sje_balance" "sje_fy${short_year}q1exp" "0" \
      "$(awk -F',' '{ total += $26 } END { printf "%.2f", total }' "$sorted_file")" \
      "$(file_rows "$sorted_file")"
  fi
  if [[ -f "$ledger_file" ]]; then
    append_control "ledger_balance" "ledger_fy20${short_year}" "0" \
      "$(awk -F',' 'NR > 1 { total += $20 } END { printf "%.2f", total }' "$ledger_file")" \
      "$(($(file_rows "$ledger_file") - 1))"
  fi
done

printf 'Canonical evidence written to %s\n' "$METRICS_ROOT"
