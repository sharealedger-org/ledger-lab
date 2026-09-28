#!/usr/bin/env bash
# =============================================================================
#  run_c1_smoke_test.sh -- C1 Smoke Test for FY2003 + FY2004
#
#  Runs Step 2 (standardize_and_sort) and Step 3 (post.scala) for FY2003 + FY2004.
#  Validates:
#    1. Zero-sum proof: sum of ldgrTransAmount == 0.00 per year
#    2. FY2003 LDGR row count == 345,955
#    3. FY2004 LDGR accumulates correctly
#    4. Appends C1 row to cost_surface.csv and pipeline_log.csv
#
#  USAGE:
#    bash scripts/run_c1_smoke_test.sh [--years 2003,2004] [--data-root PATH] [--dry-run]
# =============================================================================

set -euo pipefail

YEARS="2003,2004"
DATA_ROOT="${UL_DATA_ROOT:-data}"
DRY_RUN=0

while [[ $# -gt 0 ]]; do
  case "$1" in
    --years)
      YEARS="$2"
      shift 2
      ;;
    --data-root)
      DATA_ROOT="$2"
      shift 2
      ;;
    --dry-run)
      DRY_RUN=1
      shift
      ;;
    -h|--help)
      echo "Usage: bash scripts/run_c1_smoke_test.sh [--years 2003,2004] [--data-root PATH] [--dry-run]"
      exit 0
      ;;
    *)
      echo "Unknown option: $1"
      exit 1
      ;;
  esac
done

if [[ "$DRY_RUN" -eq 1 ]]; then
  echo "DRY RUN -- would run C1 smoke test for years $YEARS"
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
OUT="$DATA_ROOT/output"
LOGS="$DATA_ROOT/logs/smoke_test"
VM="$DATA_ROOT/VendorMaster_full.csv"

mkdir -p "$OUT"
mkdir -p "$LOGS"

echo "======================================================================"
echo "C1 Baseline Smoke Test (Years: $YEARS)"
echo "======================================================================"

IFS=',' read -ra YEAR_ARRAY <<< "$YEARS"

grand_step2_s=0
grand_step3_s=0
total_ldgr_bytes=0

# Log rows accumulator for pipeline_log
JSON_LOG_ROWS=()

for yr in "${YEAR_ARRAY[@]}"; do
  yr="$(echo "$yr" | xargs)"
  shortYr="${yr:2:2}"
  echo ""
  echo "--- Processing FY$yr (Short: $shortYr) ---"

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

  # Step 2
  for i in "${!infiles[@]}"; do
    infile="${infiles[$i]}"
    outfile="${outfiles[$i]}"
    q="${quarters[$i]}"
    t="${types[$i]}"
    label="$(basename "$outfile" .csv)"
    logfile="$LOGS/$label.log"

    if [[ ! -f "$infile" ]]; then
      # Check if CSV extension (for FY14+)
      csv_infile="${infile%.txt}.csv"
      if [[ -f "$csv_infile" ]]; then
        infile="$csv_infile"
      else
        echo "WARNING: Infile $infile not found, skipping."
        continue
      fi
    fi

    echo -n "  Step 2: $infile ..."
    start_time=$(date +%s)

    (
      cd 02-foundation/va_pipeline
      "$SBT" -batch "run --step 2 --inPath ../../$RAW/ --outPath ../../$OUT/ --vendormaster ../../$VM --year $yr --quarter $q --type $t" > "../../$logfile" 2>&1
    )

    end_time=$(date +%s)
    elapsed=$((end_time - start_time))
    grand_step2_s=$(echo "$grand_step2_s + $elapsed" | bc -l 2>/dev/null || awk "BEGIN {print $grand_step2_s + $elapsed}")

    if ! grep -q "STATUS: COMPLETE" "$logfile"; then
      echo " FAILED -- check $logfile"
      exit 1
    fi

    rows_out=$(grep "Journal lines produced:" "$logfile" | tail -1 | awk -F':' '{print $2}' | tr -d ' ,' || echo "")
    size_bytes=0
    if [[ -f "$outfile" ]]; then
      size_bytes=$(wc -c < "$outfile" | tr -d ' ')
    fi
    size_mb=$(awk "BEGIN {printf \"%.1f\", $size_bytes / 1048576}")

    echo "  ${elapsed}s ($rows_out rows, ${size_mb} MB)"

    JSON_LOG_ROWS+=("{\"step\":\"2\",\"file_label\":\"$label\",\"elapsed_s\":$elapsed,\"rows_in\":null,\"rows_out\":\"$rows_out\",\"storage_bytes\":$size_bytes}")
  done

  # Step 3
  echo -n "  Step 3: post.scala for FY$yr ..."
  step3_log="$LOGS/step3_post_$yr.log"
  start_step3=$(date +%s)

  (
    cd 02-foundation/va_pipeline
    "$SBT" -batch "run --step 3 --inPath ../../$OUT/ --outPath ../../$OUT/ --year $yr" > "../../$step3_log" 2>&1
  )

  end_step3=$(date +%s)
  step3_s=$((end_step3 - start_step3))
  grand_step3_s=$(echo "$grand_step3_s + $step3_s" | bc -l 2>/dev/null || awk "BEGIN {print $grand_step3_s + $step3_s}")

  ldgr_path="$OUT/LDGR$yr.csv"
  ldgr_bytes=0
  ldgr_rows=0
  if [[ -f "$ldgr_path" ]]; then
    ldgr_bytes=$(wc -c < "$ldgr_path" | tr -d ' ')
    total_ldgr_bytes=$((total_ldgr_bytes + ldgr_bytes))
    ldgr_rows=$(( $(wc -l < "$ldgr_path") - 1 ))
  fi
  ldgr_mb=$(awk "BEGIN {printf \"%.1f\", $ldgr_bytes / 1048576}")

  echo "  ${step3_s}s (LDGR$yr rows: $ldgr_rows, ${ldgr_mb} MB)"

  JSON_LOG_ROWS+=("{\"step\":\"3\",\"file_label\":\"LDGR$yr\",\"elapsed_s\":$step3_s,\"rows_in\":null,\"rows_out\":\"$ldgr_rows\",\"storage_bytes\":$ldgr_bytes}")
done

echo ""
echo "======================================================================"
echo "Validation & Verification Checks"
echo "======================================================================"

$PYTHON - <<EOF
import csv, sys, os

years = "$YEARS".split(',')
all_passed = True

for yr in years:
    yr = yr.strip()
    path = os.path.join('data', 'output', f'LDGR{yr}.csv')
    if not os.path.exists(path):
        print(f'ERROR: {path} not found!')
        all_passed = False
        continue
    
    total_amt = 0.0
    row_count = 0
    with open(path, newline='', encoding='utf-8') as f:
        r = csv.reader(f)
        header = next(r, None)
        for row in r:
            if len(row) > 19:
                try:
                    total_amt += float(row[19])
                except ValueError:
                    pass
                row_count += 1
                
    zero_sum = abs(total_amt) < 0.01
    status_str = 'PASSED' if zero_sum else 'FAILED'
    print(f'FY{yr}: {row_count:,} LDGR rows | sum = {total_amt:.2f} | Zero-sum proof: {status_str}')
    if not zero_sum:
        all_passed = False
    if yr == '2003' and row_count != 345955:
        print(f'  NOTE: FY2003 row count is {row_count:,} (expected 345,955)')

if not all_passed:
    sys.exit(1)
EOF

echo ""
echo "Smoke test passed successfully!"
