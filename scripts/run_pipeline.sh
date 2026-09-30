#!/usr/bin/env bash
# =============================================================================
#  run_pipeline.sh — Universal Ledger ARE/AL/ALE Pipeline Runner
#
#  Runs the full Financial System Patterns pipeline (or a subset) for one
#  or more fiscal years.  Each step is independently scriptable so a human
#  learner, a shell script, or an AI agent can drive it the same way.
#
#  USAGE:
#    bash run_pipeline.sh [OPTIONS]
#
#  OPTIONS:
#    -i, --inPath   DIR    Input data root  (default: /VAdata/data/)
#    -o, --outPath  DIR    Output data root (default: /VAdata/data/)
#    -y, --years    LIST   Comma-separated two-digit fiscal years (default: 03)
#                          e.g.  --years 03,04,05
#    -s, --steps    LIST   Comma-separated step numbers to run   (default: 2,3)
#                          e.g.  --steps 2,3,5,4
#                          Steps: 1=poToPayMatch(Spark), 2=transStandardize,
#                                 3=post, 4=contraCreation(LAST),
#                                 5=dataAggregation(ViewSpec-driven pivot),
#                                 6=financialAllocation, 7=consolidation,
#                                 8=forecastingBudgeting,
#                                 9=arrangementReclass(before 4 and 5),
#                                 10=initFiles, 11=DPBSpark(Spark),
#                                 12=booked currency revaluation engine
#                          Experiment curve configurations:
#                            C1 (Post only):              --steps 2,3
#                            C2 (Post+Analysis):          --steps 2,3,5
#                            C3 (Post+Analysis+Reval):    --steps 2,3,5,8,9
#                            C4 (Full pipeline):          --steps 2,3,5,6,7,8,9,4
#    -c, --curve    LABEL  Experiment curve label: C1, C2, C3, C4 (default: C1)
#    -d, --config   DESC   Short config description written to cost_surface.csv
#                          (e.g. "minimal-materialization" or "full-views")
#        --profile  NAME   Named workload profile for evidence and interpretation
#        --materialization NAME  Materialization policy (derive-report, persist-sjes, etc.)
#    -l, --logFile  FILE   Append control-total output to this file
#                          (default: pipeline_results.log in outPath)
#    -h, --help            Print this help and exit
#
#  STEP GUIDE (no external data or Spark required):
#    Step 2 reads FY<YY>q<quarter>exp.txt plus VendorMaster.csv from --inPath
#    and writes SortedJE*.csv to --outPath. Step 3 reads those SortedJE files
#    from --outPath and writes LDGR<YYYY>.csv. For the checked-in fixture, run:
#      bash scripts/run_repo_fixture_smoke_test.sh
#
#  EXAMPLES:
#    # C1 baseline (fixture data):
#    bash run_pipeline.sh --inPath ./data --outPath ./data/output \
#                         --years 03 --steps 2,3 --curve C1 --config post-only
#
#    # C2 Post+Analysis — measures marginal cost of reporting materialization:
#    bash run_pipeline.sh --inPath ./data --outPath ./data/output \
#                         --years 03 --steps 2,3,5 --curve C2 --config full-views
#
#    # C4 full pipeline (fixture data — requires AllocationRules.csv etc.):
#    bash run_pipeline.sh --inPath ./data --outPath ./data/output \
#                         --years 03 --steps 2,3,5,6,7,8,9,4 --curve C4 --config full
#
#    # Run FY03–FY06 C2 (full VA data required):
#    bash run_pipeline.sh --inPath /VAdata/data --outPath /VAdata/data \
#                         --years 03,04,05,06 --steps 2,3,5 --curve C2
#
#    # Run just contra creation (step 4) to verify reconciliation:
#    bash run_pipeline.sh --steps 4 --years 05
#
#  OUTPUTS (in addition to pipeline_results.log):
#    cost_surface.csv — one row per run, accumulates across runs:
#      run_id, run_timestamp, curve, config_description, years, steps,
#      total_compute_s, total_storage_bytes, master_file_count, total_cost_proxy
#    The master_file_count (AHI metric) = count of enabled=Y rows in ViewSpec.csv.
#    The total_cost_proxy = total_compute_s + (total_storage_bytes / 1e6) +
#                           (master_file_count * 10).
#    An agent can drive successive rows by varying --curve, --config, and
#    ViewSpec.csv between runs.
#
#  AGENTIC / PROGRAMMATIC USE:
#    Each step writes its control totals to stdout AND appends a structured
#    summary line to the log file.  An agent can:
#      - Call this script with different --years, --steps, --curve, --config
#      - Read pipeline_results.log for per-step timing and storage
#      - Read cost_surface.csv for the full cross-run experiment record
#      - Edit ViewSpec.csv between runs to vary materialization level
#      - Vary reference data (AllocationRules.csv etc.) between runs
#
#  NOTES:
#    - Steps 1 and 11 require Apache Spark to be installed.
#    - Steps 2 and 3 are pure Scala — no Spark required.
#    - Run step 10 (initFiles) once before the first pipeline run on new data.
#    - Step 3 (post) reads from outPath (where step 2 writes SortedJE files).
#
#  *(c) Copyright IBM Corporation. 2018
#  * SPDX-License-Identifier: Apache-2.0
#  * By Kip Twitchell
#  2025 - Scriptable pipeline runner added (Bob AI)
# =============================================================================

set -euo pipefail

# ── defaults ──────────────────────────────────────────────────────────────────
IN_PATH="/VAdata/data/"
OUT_PATH="/VAdata/data/"
YEARS="03"
STEPS="2,3"
LOG_FILE=""
CURVE="C1"
CONFIG_DESC="default"
WORKLOAD_PROFILE="legacy"
MATERIALIZATION_PROFILE="unspecified"

# ── arg parsing ───────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    -i|--inPath)   IN_PATH="$2";     shift 2 ;;
    -o|--outPath)  OUT_PATH="$2";    shift 2 ;;
    -y|--years)    YEARS="$2";       shift 2 ;;
    -s|--steps)    STEPS="$2";       shift 2 ;;
    -l|--logFile)  LOG_FILE="$2";    shift 2 ;;
    -c|--curve)    CURVE="$2";       shift 2 ;;
    -d|--config)   CONFIG_DESC="$2"; shift 2 ;;
    --profile)     WORKLOAD_PROFILE="$2"; shift 2 ;;
    --materialization) MATERIALIZATION_PROFILE="$2"; shift 2 ;;
    -h|--help)     grep '^#' "$0" | sed 's/^# \{0,2\}//'; exit 0 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# ── derived values ────────────────────────────────────────────────────────────
[[ -z "$LOG_FILE" ]] && LOG_FILE="${OUT_PATH%/}/pipeline_results.log"
COST_SURFACE="${OUT_PATH%/}/cost_surface.csv"
VIEWSPEC="${IN_PATH%/}/ViewSpec.csv"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SBT_PROJECT="$REPO_ROOT/02-foundation/va_pipeline"

# The engine now lives below the repository root. Normalize relative data paths
# before invoking sbt so callers can continue using --inPath data.
is_absolute_path() {
  [[ "$1" = /* || "$1" =~ ^[A-Za-z]:[\\/] || "$1" = //* ]]
}

is_absolute_path "$IN_PATH" || IN_PATH="$REPO_ROOT/$IN_PATH"
is_absolute_path "$OUT_PATH" || OUT_PATH="$REPO_ROOT/$OUT_PATH"
IN_PATH="${IN_PATH%/}/"
OUT_PATH="${OUT_PATH%/}/"

# ── helpers ───────────────────────────────────────────────────────────────────
log() {
  local msg="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
  echo "$msg"
  echo "$msg" >> "$LOG_FILE"
}

count_file_rows() {
  local file="$1"
  [[ -f "$file" ]] && awk 'END { print NR + 0 }' "$file" || printf '0\n'
}

count_data_rows() {
  local file="$1"
  [[ -f "$file" ]] && awk 'END { print (NR > 0 ? NR - 1 : 0) }' "$file" || printf '0\n'
}

count_file_bytes() {
  local file="$1"
  [[ -f "$file" ]] && wc -c < "$file" | tr -d ' ' || printf '0\n'
}

directory_bytes() {
  local directory="$1"
  [[ -d "$directory" ]] || { printf '0\n'; return; }
  find "$directory" -type f -exec wc -c {} \; | awk '{ total += $1 } END { printf "%.0f\n", total }'
}

run_step() {
  local step="$1" year="$2" quarter="${3:-01}"
  local quarter_number=$((10#$quarter))
  log "START  step=$step  year=$year  quarter=$quarter"

  local vm_arg=""
  if [[ "$step" == "2" ]]; then
    local vm_candidate
    for vm_candidate in \
      "$IN_PATH/VendorMaster_full.csv" \
      "$IN_PATH/VendorMaster_enriched.csv" \
      "$IN_PATH/VendorMaster.csv"; do
      if [[ -f "$vm_candidate" ]]; then
        vm_arg=" --vendormaster $vm_candidate"
        break
      fi
    done
  fi
  local allocation_rules_arg=""
  if [[ "$step" == "6" && -f "$IN_PATH/AllocationRules.csv" ]]; then
    allocation_rules_arg=" --allocationRules $IN_PATH"
  fi
  local fx_args=""
  if [[ "$step" == "12" ]]; then
    fx_args=" --fxRates $IN_PATH/booked_fx_rates.csv --fxRules $IN_PATH/booked_fx_rules.csv"
  fi

  local input_file="" output_file="" input_bytes="0" output_file_bytes="0"
  local records_read="?" records_written="?"
  case "$step" in
    2)
      input_file="$IN_PATH/FY${year}q${quarter_number}exp.txt"
      output_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}exp.csv"
      records_read=$(count_file_rows "$input_file")
      ;;
    3)
      input_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}exp.csv"
      output_file="$OUT_PATH/LDGR20${year}.csv"
      records_read=0
      input_bytes=0
      for sje_file in "$OUT_PATH/SortedJEFY${year}"*.csv; do
        [[ -f "$sje_file" ]] || continue
        records_read=$((records_read + $(count_file_rows "$sje_file")))
        input_bytes=$((input_bytes + $(count_file_bytes "$sje_file")))
      done
      ;;
    6)
      input_file="$OUT_PATH/LDGR20${year}.csv"
      output_file="$OUT_PATH/ALLOC_SJE20${year}.csv"
      records_read=$(count_data_rows "$input_file")
      ;;
    12)
      input_file="$OUT_PATH/LDGR20${year}_OPENING.csv"
      output_file="$OUT_PATH/SortedJEFY${year}_FXR.csv"
      records_read=$(count_data_rows "$input_file")
      ;;
  esac
  if [[ "$step" != "3" ]]; then
    input_bytes=$(count_file_bytes "$input_file")
  fi

  local start_epoch
  start_epoch=$(date +%s)

  # Capture output while still streaming it to terminal
  local out
  local time_mode=""
  if [[ "$(uname -s 2>/dev/null)" == "Darwin" && -x /usr/bin/time ]]; then
    time_mode="macos"
  elif /usr/bin/time --version >/dev/null 2>&1; then
    time_mode="gnu"
  fi
  if [[ "$time_mode" == "macos" ]]; then
    out=$(cd "$SBT_PROJECT" && /usr/bin/time -l sbt -batch \
      "run --step $step --inPath $IN_PATH --outPath $OUT_PATH --year $year --quarter $quarter$vm_arg$allocation_rules_arg$fx_args" \
      2>&1 | tee /dev/stderr)
  elif [[ "$time_mode" == "gnu" ]]; then
    out=$(cd "$SBT_PROJECT" && /usr/bin/time -v sbt -batch \
      "run --step $step --inPath $IN_PATH --outPath $OUT_PATH --year $year --quarter $quarter$allocation_rules_arg$fx_args" \
      2>&1 | tee /dev/stderr)
  else
    out=$(cd "$SBT_PROJECT" && sbt -batch \
      "run --step $step --inPath $IN_PATH --outPath $OUT_PATH --year $year --quarter $quarter$vm_arg$allocation_rules_arg$fx_args" \
      2>&1 | tee /dev/stderr)
  fi

  local rc=$?
  local end_epoch
  end_epoch=$(date +%s)
  local elapsed=$(( end_epoch - start_epoch ))
  LAST_ELAPSED=$elapsed  # exported for TOTAL_COMPUTE_S accumulation in main loop
  LAST_PEAK_RSS_BYTES=""
  if [[ "$time_mode" == "macos" ]]; then
    LAST_PEAK_RSS_BYTES=$(printf '%s\n' "$out" | awk '/maximum resident set size/ { print $1; exit }')
  elif [[ "$time_mode" == "gnu" ]]; then
    local rss_kb
    rss_kb=$(printf '%s\n' "$out" | awk -F': ' '/Maximum resident set size/ { print $2; exit }' | awk '{print $1}')
    [[ "$rss_kb" =~ ^[0-9]+$ ]] && LAST_PEAK_RSS_BYTES=$((rss_kb * 1024))
  fi

  local records_written
  case "$step" in
    2)
      records_written=$(count_file_rows "$output_file")
      ;;
    3)
      records_written=$(count_data_rows "$output_file")
      ;;
    6)
      records_written=$(count_data_rows "$output_file")
      ;;
    12)
      records_written=$(count_file_rows "$output_file")
      ;;
    *)
      records_read=$(echo "$out" | grep -i "Records Read:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      records_written=$(echo "$out" | grep -i "Records Written:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      ;;
  esac
  output_file_bytes=$(count_file_bytes "$output_file")

  local out_bytes
  out_bytes=$(directory_bytes "$OUT_PATH")

  # Structured result line — readable by agents and humans
  local result_line="RESULT step=$step year=$year quarter=$quarter elapsed_s=$elapsed rc=$rc records_read=$records_read records_written=$records_written input_bytes=$input_bytes output_bytes=$output_file_bytes outPath_bytes=$out_bytes"
  if [[ "$step" == "12" ]]; then
    local engine_result
    engine_result=$(printf '%s\n' "$out" | grep -E 'RESULT step=12 ' | tail -1 || true)
    for field in generated_groups balanced_groups unbalanced_groups amount_delta adjustment_total spills rule_set_id rule_version; do
      local value
      value=$(printf '%s\n' "$engine_result" | sed -nE "s/.*(^| )${field}=([^ ]+).*/\\2/p" | tail -1)
      [[ -z "$value" ]] || result_line+=" $field=$value"
    done
  fi
  log "$result_line"
  log "RESOURCE step=$step year=$year peak_rss_bytes=${LAST_PEAK_RSS_BYTES:-}"

  if [[ $rc -ne 0 ]]; then
    log "ERROR  step=$step year=$year exited with code $rc — pipeline halted"
    exit $rc
  fi
}

# ── cost surface helpers ──────────────────────────────────────────────────────

# Count enabled=Y rows in ViewSpec.csv (AHI metric — reconciliation obligation count).
# Skips comment lines and the header; counts only rows where the 'enabled' column = Y.
count_enabled_views() {
  local viewspec="$1"
  if [[ ! -f "$viewspec" ]]; then
    echo "0"
    return
  fi
  # Column index of 'enabled' (1-based) from the header row
  local col_idx
  col_idx=$(grep -v '^#' "$viewspec" | head -1 | tr ',' '\n' | grep -n '^enabled$' | cut -d: -f1)
  if [[ -z "$col_idx" ]]; then
    echo "0"
    return
  fi
  grep -v '^#' "$viewspec" | tail -n +2 | awk -F',' -v col="$col_idx" '$col == "Y"' | wc -l | tr -d ' '
}

# Initialise cost_surface.csv header if file does not exist.
init_cost_surface() {
  local f="$1"
  if [[ ! -f "$f" ]]; then
    echo "\"run_id\",\"run_timestamp\",\"curve\",\"config_description\",\"years\",\"steps\",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy" > "$f"
  fi
}

# Append one row to cost_surface.csv.
record_cost_point() {
  local f="$1" run_id="$2" ts="$3" curve="$4" cfg="$5" years="$6" steps="$7"
  local compute_s="$8" storage_bytes="$9" master_count="${10}"
  # total_cost_proxy: compute(s) + storage(MB) + master_files × 10
  local proxy
  if [[ "$storage_bytes" =~ ^[0-9]+$ ]]; then
    proxy=$(awk -v c="$compute_s" -v s="$storage_bytes" -v m="$master_count" \
      'BEGIN { printf "%.2f", c + (s/1000000) + (m*10) }')
  else
    proxy="?"
  fi
  # Quote fields that may contain spaces or commas
  echo "\"${run_id}\",\"${ts}\",\"${curve}\",\"${cfg}\",\"${years}\",\"${steps}\",${compute_s},${storage_bytes},${master_count},${proxy}" >> "$f"
}

# ── main loop ─────────────────────────────────────────────────────────────────
mkdir -p "$OUT_PATH"
init_cost_surface "$COST_SURFACE"

RUN_ID="run_$(date '+%Y%m%d_%H%M%S')"
RUN_TS=$(date '+%Y-%m-%d %H:%M:%S')
RUN_START_EPOCH=$(date +%s)
TOTAL_COMPUTE_S=0

log "======================================================================"
log "Universal Ledger Pipeline Run  [$RUN_ID]"
log "  curve   = $CURVE"
log "  config  = $CONFIG_DESC"
log "  profile = $WORKLOAD_PROFILE"
log "  materialization = $MATERIALIZATION_PROFILE"
log "  inPath  = $IN_PATH"
log "  outPath = $OUT_PATH"
log "  years   = $YEARS"
log "  steps   = $STEPS"
log "  logFile = $LOG_FILE"
log "======================================================================"

IFS=',' read -ra YEAR_LIST <<< "$YEARS"
IFS=',' read -ra STEP_LIST <<< "$STEPS"

for year in "${YEAR_LIST[@]}"; do
  log "------  Year $year  -------------------------------------------------------"
  for step in "${STEP_LIST[@]}"; do
    run_step "$step" "$year"
    # Accumulate compute time — run_step sets last elapsed in LAST_ELAPSED
    TOTAL_COMPUTE_S=$(( TOTAL_COMPUTE_S + ${LAST_ELAPSED:-0} ))
  done
done

# Allocation output becomes the next period's divisor side input before evidence is flushed.
if [[ ",$STEPS," == *,6,* ]]; then
  for year in "${YEAR_LIST[@]}"; do
    short_year="${year: -2}"
    next_year=$(printf "%02d" $((10#$short_year + 1)))
    ledger_after_allocation="$OUT_PATH/LDGR20${short_year}.csv"
    if [[ -f "$ledger_after_allocation" ]]; then
      bash "$SCRIPT_DIR/generate_allocation_divisors.sh" \
        --source-kind ledger \
        --input "$ledger_after_allocation" \
        --output "$OUT_PATH/AllocationDivisors_FY${short_year}_after_allocation.csv" \
        --divisor-period "FY${short_year}Q1" \
        --source-period "FY${next_year}Q1" \
        --source-partition-id "LDGR20${short_year}_after_allocation"
    fi
  done
fi

# ── post-run metrics ──────────────────────────────────────────────────────────
FINAL_STORAGE_BYTES="?"
if [[ -d "$OUT_PATH" ]]; then
  FINAL_STORAGE_BYTES=$(directory_bytes "$OUT_PATH")
fi

MASTER_FILE_COUNT=$(count_enabled_views "$VIEWSPEC")

log "======================================================================"
log "Pipeline complete.  [$RUN_ID]"
log "  total_compute_s     = $TOTAL_COMPUTE_S"
log "  total_storage_bytes = $FINAL_STORAGE_BYTES"
log "  master_file_count   = $MASTER_FILE_COUNT  (enabled=Y rows in ViewSpec.csv)"
log "  cost_surface        = $COST_SURFACE"
log "======================================================================"

record_cost_point "$COST_SURFACE" "$RUN_ID" "$RUN_TS" "$CURVE" "$CONFIG_DESC" \
  "$YEARS" "$STEPS" "$TOTAL_COMPUTE_S" "$FINAL_STORAGE_BYTES" "$MASTER_FILE_COUNT"

log "Cost point recorded → $COST_SURFACE"

RUN_END_EPOCH=$(date +%s)
bash "$SCRIPT_DIR/write_canonical_evidence.sh" \
  --run-id "$RUN_ID" \
  --run-timestamp "$RUN_TS" \
  --in-path "$IN_PATH" \
  --out-path "$OUT_PATH" \
  --years "$YEARS" \
  --steps "$STEPS" \
  --curve "$CURVE" \
  --config "$CONFIG_DESC" \
  --start-epoch "$RUN_START_EPOCH" \
  --end-epoch "$RUN_END_EPOCH" \
  --compute-seconds "$TOTAL_COMPUTE_S" \
  --log-file "$LOG_FILE" \
  --workload-profile "$WORKLOAD_PROFILE" \
  --materialization-profile "$MATERIALIZATION_PROFILE"

bash "$SCRIPT_DIR/interpret_run.sh" \
  "${OUT_PATH%/}/metrics" \
  "${OUT_PATH%/}/interpretation"
