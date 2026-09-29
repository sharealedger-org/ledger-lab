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
#                                 10=initFiles, 11=DPBSpark(Spark)
#                          Experiment curve configurations:
#                            C1 (Post only):              --steps 2,3
#                            C2 (Post+Analysis):          --steps 2,3,5
#                            C3 (Post+Analysis+Reval):    --steps 2,3,5,8,9
#                            C4 (Full pipeline):          --steps 2,3,5,6,7,8,9,4
#    -c, --curve    LABEL  Experiment curve label: C1, C2, C3, C4 (default: C1)
#    -d, --config   DESC   Short config description written to cost_surface.csv
#                          (e.g. "minimal-materialization" or "full-views")
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

# ── helpers ───────────────────────────────────────────────────────────────────
log() {
  local msg="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
  echo "$msg"
  echo "$msg" >> "$LOG_FILE"
}

run_step() {
  local step="$1" year="$2" quarter="${3:-01}"
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

  local start_epoch
  start_epoch=$(date +%s)

  # Capture output while still streaming it to terminal
  local out
  out=$(cd "$SBT_PROJECT" && sbt -batch \
    "run --step $step --inPath $IN_PATH --outPath $OUT_PATH --year $year --quarter $quarter$vm_arg" \
    2>&1 | tee /dev/stderr)

  local rc=$?
  local end_epoch
  end_epoch=$(date +%s)
  local elapsed=$(( end_epoch - start_epoch ))
  LAST_ELAPSED=$elapsed  # exported for TOTAL_COMPUTE_S accumulation in main loop

  # Extract key control-total lines from output for structured logging
  local records_read records_written
  records_read=$(echo "$out"   | grep -i "Records Read:"    | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
  records_written=$(echo "$out" | grep -i "Records Written:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")

  # Measure output size if outPath exists
  local out_bytes="?"
  if [[ -d "$OUT_PATH" ]]; then
    out_bytes=$(du -sb "$OUT_PATH" 2>/dev/null | awk '{print $1}' || echo "?")
  fi

  # Structured result line — readable by agents and humans
  local result_line="RESULT step=$step year=$year quarter=$quarter elapsed_s=$elapsed rc=$rc records_read=$records_read records_written=$records_written outPath_bytes=$out_bytes"
  log "$result_line"

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

# ── post-run metrics ──────────────────────────────────────────────────────────
FINAL_STORAGE_BYTES="?"
if [[ -d "$OUT_PATH" ]]; then
  FINAL_STORAGE_BYTES=$(du -sb "$OUT_PATH" 2>/dev/null | awk '{print $1}' || echo "?")
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
  --log-file "$LOG_FILE"
