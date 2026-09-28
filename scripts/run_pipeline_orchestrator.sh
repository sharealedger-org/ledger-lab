#!/usr/bin/env bash
# =============================================================================
#  run_pipeline_orchestrator.sh -- Universal Ledger Chronological Orchestrator
#
#  Features:
#    1. Chronological per-period CAR processing (PO ingestion + date-effective history roll-forward)
#    2. Concurrency-throttled parallel Step 2 sorts
#    3. Incremental Step 3 ledger posting (LDGR{T-1} + SortedJE{T} -> LDGR{T})
#    4. Step 5 Date-effective ViewSpec aggregation with full timing accumulators
#
#  USAGE:
#    bash scripts/run_pipeline_orchestrator.sh [OPTIONS]
#
#  OPTIONS:
#    --years          YEARS   Comma-separated years (default: 2003,2004)
#    --curve          LABEL   C1, C2, C3, C4 (default: C2)
#    --config         DESC    Config description (default: chronological-sal-pipeline)
#    --max-concurrent N       Max parallel Step 2 sort jobs (default: 2)
#    --data-root PATH         External runtime data root (default: $UL_DATA_ROOT or data)
#    --wipe-output            Wipe data/output before running
#    --dry-run                Print configuration and exit
# =============================================================================

set -euo pipefail

YEARS="2003,2004"
CURVE="C2"
CONFIG_DESC="chronological-sal-pipeline"
MAX_CONCURRENT=2
DATA_ROOT="${UL_DATA_ROOT:-data}"
WIPE_OUTPUT=0
DRY_RUN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --years)
      YEARS="$2"
      shift 2
      ;;
    --curve)
      CURVE="$2"
      shift 2
      ;;
    --config)
      CONFIG_DESC="$2"
      shift 2
      ;;
    --max-concurrent)
      MAX_CONCURRENT="$2"
      shift 2
      ;;
    --data-root)
      DATA_ROOT="$2"
      shift 2
      ;;
    --wipe-output)
      WIPE_OUTPUT=1
      shift
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      echo "Usage: bash scripts/run_pipeline_orchestrator.sh [--years 2003,2004] [--curve C2] [--config <desc>] [--max-concurrent 2] [--data-root PATH] [--wipe-output] [--dry-run]"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "DRY RUN -- Years: $YEARS | Curve: $CURVE | Config: $CONFIG_DESC"
  exit 0
fi

# Detect Python command
if command -v py &>/dev/null; then
  PYTHON="py -3"
elif command -v python3 &>/dev/null; then
  PYTHON="python3"
else
  PYTHON="python"
fi

# Detect sbt command
if command -v sbt &>/dev/null; then
  SBT="sbt"
elif [[ -f "/c/Program Files (x86)/sbt/bin/sbt" ]]; then
  SBT="/c/Program Files (x86)/sbt/bin/sbt"
elif [[ -f "C:/Program Files (x86)/sbt/bin/sbt.bat" ]]; then
  SBT="C:/Program Files (x86)/sbt/bin/sbt.bat"
else
  SBT="sbt"
fi

DATA_ROOT="${DATA_ROOT%/}"
RAW="$DATA_ROOT/VARawFiles"
PO_DIR="$DATA_ROOT/VAPOData"
OUT="$DATA_ROOT/output"
LOGS="$DATA_ROOT/logs/orchestrator"
VM_BASE="$DATA_ROOT/VendorMaster_full.csv"
VM_RUNNING="$DATA_ROOT/VendorMaster_enriched.csv"
VIEWSPEC="$DATA_ROOT/ViewSpec.csv"

echo "======================================================================"
echo "Universal Ledger Chronological Pipeline Orchestrator (Bash)"
echo "  Years          : $YEARS"
echo "  Curve          : $CURVE"
echo "  Config         : $CONFIG_DESC"
echo "  MaxConcurrent  : $MAX_CONCURRENT"
echo "======================================================================"

if [[ "$WIPE_OUTPUT" -eq 1 ]]; then
  echo ""
  echo "=== Wiping output directory $OUT ==="
  if [[ -d "$OUT" ]]; then
    rm -rf "${OUT:?}"/*
  fi
  rm -f "$DATA_ROOT/cost_surface.csv"
  rm -f "$DATA_ROOT/pipeline_log.csv"
  rm -f "$DATA_ROOT/output/pivot_results.csv"
fi

mkdir -p "$OUT"
mkdir -p "$LOGS"

pipeline_start=$(date +%s)
grand_step2_s=0
grand_step3_s=0
grand_step5_s=0
grand_storage_bytes=0

LOG_STEP_ROWS=()

IFS=',' read -ra YEAR_ARRAY <<< "$YEARS"

for yr in "${YEAR_ARRAY[@]}"; do
  yr="$(echo "$yr" | xargs)"
  shortYr="${yr:2:2}"
  echo ""
  echo "======================================================================"
  echo "$(date +'%Y-%m-%d %H:%M:%S') [JOB START] Processing Period FY$yr (Short: $shortYr)"
  echo "======================================================================"

  # ── Step 1: CAR Per-Period Ingestion & Delta Detection ───────────────────
  echo ""
  echo "[Step 1 / CAR] Ingesting PO data & rolling forward VendorMaster history for FY$yr ..."
  sal_log="$LOGS/sal_FY$yr.log"
  sal_start=$(date +%s)

  $PYTHON scripts/car_process_period.py \
    --year "$yr" \
    --po-dir "$PO_DIR" \
    --vendormaster "$VM_RUNNING" \
    --update-out "$OUT/VendorUpdate_FY$yr.csv" > "$sal_log" 2>&1

  sal_end=$(date +%s)
  sal_s=$((sal_end - sal_start))
  echo "  CAR processing complete: ${sal_s}s (log: $sal_log)"

  # ── Step 2: Concurrency-Throttled Standardize & Sort ──────────────────────
  echo ""
  echo "[Step 2 / Sort] Standardizing and sorting transactions for FY$yr (Max Concurrent: $MAX_CONCURRENT) ..."

  infiles=(
    "$RAW/FY${shortYr}q1exp.txt"
    "$RAW/FY${shortYr}q2exp.txt"
    "$RAW/FY${shortYr}q3exp.txt"
    "$RAW/FY${shortYr}q4exp.txt"
    "$RAW/FY${shortYr}rev.txt"
  )
  outfiles=(
    "$OUT/SortedJEFY${shortYr}q1exp.csv"
    "$OUT/SortedJEFY${shortYr}q2exp.csv"
    "$OUT/SortedJEFY${shortYr}q3exp.csv"
    "$OUT/SortedJEFY${shortYr}q4exp.csv"
    "$OUT/SortedJEFY${shortYr}rev.csv"
  )
  quarters=(1 2 3 4 5)
  types=("E" "E" "E" "E" "R")

  step2_start=$(date +%s)

  # Collect job parameters
  active_pids=()
  active_labels=()
  active_start_times=()

  job_infiles=()
  job_outfiles=()
  job_quarters=()
  job_types=()
  job_labels=()

  for i in "${!infiles[@]}"; do
    inf="${infiles[$i]}"
    outf="${outfiles[$i]}"
    q="${quarters[$i]}"
    t="${types[$i]}"
    lbl="$(basename "$outf" .csv)"

    if [[ ! -f "$inf" ]]; then
      csv_inf="${inf%.txt}.csv"
      if [[ -f "$csv_inf" ]]; then
        inf="$csv_inf"
      else
        echo "  WARNING: Input file $inf not found, skipping."
        continue
      fi
    fi

    job_infiles+=("$inf")
    job_outfiles+=("$outf")
    job_quarters+=("$q")
    job_types+=("$t")
    job_labels+=("$lbl")
  done

  # Run jobs with bounded concurrency pool
  job_idx=0
  num_jobs=${#job_infiles[@]}

  while [[ $job_idx -lt $num_jobs || ${#active_pids[@]} -gt 0 ]]; do
    # Launch new jobs if slots available
    while [[ ${#active_pids[@]} -lt $MAX_CONCURRENT && $job_idx -lt $num_jobs ]]; do
      inf="${job_infiles[$job_idx]}"
      outf="${job_outfiles[$job_idx]}"
      q="${job_quarters[$job_idx]}"
      t="${job_types[$job_idx]}"
      lbl="${job_labels[$job_idx]}"
      logfile="$LOGS/$lbl.log"

      now_ts=$(date +"%H:%M:%S")
      echo "  $now_ts [SUPERVISOR] Launched $lbl (slot $((${#active_pids[@]} + 1))/$MAX_CONCURRENT)"

      (
        cd 02-foundation/va_pipeline
        "$SBT" -batch "run --step 2 --inPath ../../$RAW/ --outPath ../../$OUT/ --vendormaster ../../$VM_RUNNING --year $yr --quarter $q --type $t" > "../../$logfile" 2>&1
      ) &

      pid=$!
      start_t=$(date +%s)
      active_pids+=("$pid")
      active_labels+=("$lbl")
      active_start_times+=("$start_t")

      job_idx=$((job_idx + 1))
    done

    # Check for completed active jobs
    new_active_pids=()
    new_active_labels=()
    new_active_start_times=()

    for p_i in "${!active_pids[@]}"; do
      pid="${active_pids[$p_i]}"
      lbl="${active_labels[$p_i]}"
      st="${active_start_times[$p_i]}"

      if kill -0 "$pid" 2>/dev/null; then
        # Still running
        new_active_pids+=("$pid")
        new_active_labels+=("$lbl")
        new_active_start_times+=("$st")
      else
        # Process completed
        wait "$pid" || {
          echo "  [SUPERVISOR ALERT] Job $lbl failed with non-zero exit code. Check $LOGS/$lbl.log"
          exit 1
        }
        logfile="$LOGS/$lbl.log"
        if [[ -f "$logfile" ]] && ! grep -q "STATUS: COMPLETE" "$logfile"; then
          echo "  [SUPERVISOR ALERT] Job $lbl did not complete cleanly. Check $logfile"
          exit 1
        fi
        done_t=$(date +%s)
        dur=$((done_t - st))
        now_ts=$(date +"%H:%M:%S")
        echo "  $now_ts [SUPERVISOR] Finished $lbl in ${dur}s"
      fi
    done

    active_pids=("${new_active_pids[@]}")
    active_labels=("${new_active_labels[@]}")
    active_start_times=("${new_active_start_times[@]}")

    if [[ ${#active_pids[@]} -gt 0 ]]; then
      sleep 2
    fi
  done

  step2_end=$(date +%s)
  step2_wall_s=$((step2_end - step2_start))

  # Collect Step 2 metrics from logs
  step2_cpu_total_s=0
  for lbl in "${job_labels[@]}"; do
    logfile="$LOGS/$lbl.log"
    outfile="$OUT/$lbl.csv"
    elapsed=0
    if [[ -f "$logfile" ]]; then
      elapsed_str=$(grep "^Elapsed:" "$logfile" | tail -1 | sed 's/Elapsed: //; s/s//' | tr -d ' \r\n' || echo "0")
      elapsed=$(awk "BEGIN {print ($elapsed_str + 0)}")
    fi
    step2_cpu_total_s=$(awk "BEGIN {print $step2_cpu_total_s + $elapsed}")

    rows_out=""
    if [[ -f "$logfile" ]]; then
      rows_out=$(grep "Journal lines produced:" "$logfile" | tail -1 | awk -F':' '{print $2}' | tr -d ' ,\r\n' || echo "")
    fi

    size_bytes=0
    if [[ -f "$outfile" ]]; then
      size_bytes=$(wc -c < "$outfile" | tr -d ' ')
    fi
    grand_storage_bytes=$((grand_storage_bytes + size_bytes))

    LOG_STEP_ROWS+=("{\"step\":\"2\",\"file_label\":\"$lbl\",\"elapsed_s\":$elapsed,\"rows_in\":null,\"rows_out\":\"$rows_out\",\"storage_bytes\":$size_bytes}")
  done

  grand_step2_s=$(awk "BEGIN {print $grand_step2_s + $step2_cpu_total_s}")
  echo "  Step 2 finished: ${step2_wall_s}s wall-clock, ${step2_cpu_total_s}s CPU time."

  # ── Step 3: Incremental Ledger Post ──────────────────────────────────────
  echo ""
  echo "[Step 3 / Post] Incremental CKB posting for FY$yr ..."
  step3_log="$LOGS/step3_post_$yr.log"
  step3_start=$(date +%s)

  (
    cd 02-foundation/va_pipeline
    "$SBT" -batch "run --step 3 --inPath ../../$OUT/ --outPath ../../$OUT/ --year $yr" > "../../$step3_log" 2>&1
  )

  step3_end=$(date +%s)
  step3_s=$((step3_end - step3_start))
  grand_step3_s=$(awk "BEGIN {print $grand_step3_s + $step3_s}")

  ldgr_path="$OUT/LDGR$yr.csv"
  ldgr_bytes=0
  ldgr_rows=0
  if [[ -f "$ldgr_path" ]]; then
    ldgr_bytes=$(wc -c < "$ldgr_path" | tr -d ' ')
    grand_storage_bytes=$((grand_storage_bytes + ldgr_bytes))
    ldgr_rows=$(( $(wc -l < "$ldgr_path") - 1 ))
  fi
  ldgr_mb=$(awk "BEGIN {printf \"%.1f\", $ldgr_bytes / 1048576}")
  echo "  Step 3 finished: ${step3_s}s (LDGR${yr}: $ldgr_rows rows, ${ldgr_mb} MB)"

  LOG_STEP_ROWS+=("{\"step\":\"3\",\"file_label\":\"LDGR$yr\",\"elapsed_s\":$step3_s,\"rows_in\":null,\"rows_out\":\"$ldgr_rows\",\"storage_bytes\":$ldgr_bytes}")
  echo "$(date +'%Y-%m-%d %H:%M:%S') [JOB END] Finished Period FY$yr -- LDGR${yr} written ($ldgr_rows records)"
done

# ── Step 5: Date-Effective ViewSpec Aggregation (C2 / Reporting) ─────────────
if [[ "$CURVE" =~ ^(C2|C3|C4)$ ]]; then
  echo ""
  echo "======================================================================"
  echo "[Step 5 / Reporting] Generating Date-Effective ViewSpec Aggregations ..."
  echo "======================================================================"

  pipe_json_file="$LOGS/pipeline_log_input.json"
  IFS=','
  echo "[${LOG_STEP_ROWS[*]}]" > "$pipe_json_file"
  unset IFS

  step5_start=$(date +%s)

  $PYTHON scripts/data_aggregation.py \
    --years "$YEARS" \
    --vendormaster "$VM_RUNNING" \
    --viewspec "$VIEWSPEC" \
    --ldgr-dir "$OUT" \
    --out-dir "$OUT" \
    --curve "$CURVE" \
    --config "$CONFIG_DESC" \
    --step2-s "$grand_step2_s" \
    --step3-s "$grand_step3_s" \
    --pipeline-log-file "$pipe_json_file"

  step5_end=$(date +%s)
  grand_step5_s=$((step5_end - step5_start))
else
  # C1 baseline cost row only
  echo ""
  echo "Writing C1 cost row to cost_surface.csv ..."
  run_ts=$(date +"%Y-%m-%dT%H:%M:%S")
  run_id="run_$(date +"%Y%m%d_%H%M%S")_C1"
  total_c1_compute=$(awk "BEGIN {printf \"%.2f\", $grand_step2_s + $grand_step3_s}")
  c1_proxy=$(awk "BEGIN {printf \"%.2f\", $total_c1_compute + ($grand_storage_bytes / 1000000)}")

  cost_hdr="run_id,run_timestamp,curve,config_description,years,steps,total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy"
  cost_row="$run_id,$run_ts,C1,$CONFIG_DESC,\"$YEARS\",\"2+3\",$total_c1_compute,$grand_storage_bytes,0,$c1_proxy"

  if [[ ! -f "$DATA_ROOT/cost_surface.csv" ]]; then
    echo "$cost_hdr" > "$DATA_ROOT/cost_surface.csv"
  fi
  echo "$cost_row" >> "$DATA_ROOT/cost_surface.csv"
fi

pipeline_end=$(date +%s)
total_wall_s=$((pipeline_end - pipeline_start))

echo ""
echo "======================================================================"
echo "Pipeline Execution Completed Successfully!"
echo "  Total Wall-Clock Time : ${total_wall_s}s"
echo "  Step 2 Compute (CPU)  : ${grand_step2_s}s"
echo "  Step 3 Post Compute   : ${grand_step3_s}s"
echo "  Step 5 Aggregation    : ${grand_step5_s}s"
echo "======================================================================"
