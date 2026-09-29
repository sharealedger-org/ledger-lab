#!/usr/bin/env bash
# Run the checked-in FY03 small fixture through the no-Spark VA path.
# This intentionally uses a temporary staging directory so the repository data
# and generated outputs are not modified.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
RUN_ROOT="$(mktemp -d)"
KEEP_OUTPUT=0
SHOW_RESULTS=0
WITH_ALLOCATION=0

cleanup() {
  if [[ "$KEEP_OUTPUT" -eq 0 ]]; then
    rm -rf "$RUN_ROOT"
  else
    echo "Fixture output retained at: $RUN_ROOT/output"
  fi
}
trap cleanup EXIT

while [[ $# -gt 0 ]]; do
  case "$1" in
    --keep-output)
      KEEP_OUTPUT=1
      shift
      ;;
    --show-results)
      SHOW_RESULTS=1
      shift
      ;;
    --with-allocation)
      WITH_ALLOCATION=1
      shift
      ;;
    -h|--help)
      cat <<'EOF'
Usage: bash scripts/run_repo_fixture_smoke_test.sh [--show-results] [--keep-output] [--with-allocation]

Stages data/FY03q1exp_small.txt as FY03q1exp.txt, runs VA Steps 2 and 3,
and verifies that sorted journal and ledger output are produced and balanced.
No external VA data, Spark, PostgreSQL, or AI engine is required.

Use --show-results to print small journal, ledger, count, and summary results.
Use --keep-output to retain generated files for later inspection. The options
may be combined. Use --with-allocation to add synthetic EXP6 salary source rows
and run the active Step 6 allocation path.
EOF
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

INPUT_DIR="$RUN_ROOT/input"
OUTPUT_DIR="$RUN_ROOT/output"
mkdir -p "$INPUT_DIR" "$OUTPUT_DIR"

cp "$REPO_ROOT/data/FY03q1exp_small.txt" "$INPUT_DIR/FY03q1exp.txt"
cp "$REPO_ROOT/data/VendorMaster.csv" "$INPUT_DIR/VendorMaster.csv"
cp "$REPO_ROOT/data/VAAccountingRules.csv" "$INPUT_DIR/VAAccountingRules.csv"
cp "$REPO_ROOT/data/AllocationRules.csv" "$INPUT_DIR/AllocationRules.csv"

STEPS="2,3"
CONFIG="repo-fixture"
PROFILE="repo-fixture"
MATERIALIZATION="baseline"
if [[ "$WITH_ALLOCATION" -eq 1 ]]; then
  printf '267\t1552\t6\t2701\tBLACKWELLS BOOK SERVICES\t1000.00\n' >> "$INPUT_DIR/FY03q1exp.txt"
  printf '267\t1552\t6\t2703\tBLACKWELLS BOOK SERVICES\t500.00\n' >> "$INPUT_DIR/FY03q1exp.txt"
  STEPS="2,3,6"
  CONFIG="repo-fixture-allocation"
  PROFILE="allocation-active"
  MATERIALIZATION="persist-ledger-derive-divisor"
fi

bash "$REPO_ROOT/scripts/generate_allocation_divisors.sh" \
  --input "$INPUT_DIR/FY03q1exp.txt" \
  --output "$INPUT_DIR/AllocationDivisors_FY02_fixture.csv" \
  --divisor-period FY02 \
  --source-period FY03Q1 \
  --source-partition-id LDGR_FY02_partition_B

# Git Bash may report /tmp paths that the Windows JVM resolves differently.
# Use a drive-qualified path when cygpath is available.
RUNNER_INPUT_DIR="$INPUT_DIR"
RUNNER_OUTPUT_DIR="$OUTPUT_DIR"
if command -v cygpath >/dev/null 2>&1; then
  RUNNER_INPUT_DIR="$(cygpath -m "$INPUT_DIR")"
  RUNNER_OUTPUT_DIR="$(cygpath -m "$OUTPUT_DIR")"
fi

echo "Running checked-in FY03 small fixture through Steps 2 and 3..."
bash "$REPO_ROOT/scripts/run_pipeline.sh" \
  --inPath "$RUNNER_INPUT_DIR" \
  --outPath "$RUNNER_OUTPUT_DIR" \
  --years 03 \
  --steps "$STEPS" \
  --curve C1 \
  --config "$CONFIG" \
  --profile "$PROFILE" \
  --materialization "$MATERIALIZATION"

SORTED_FILE="$OUTPUT_DIR/SortedJEFY03q1exp.csv"
LEDGER_FILE="$OUTPUT_DIR/LDGR2003.csv"

[[ -s "$SORTED_FILE" ]] || { echo "Missing sorted journal: $SORTED_FILE" >&2; exit 1; }
[[ -s "$LEDGER_FILE" ]] || { echo "Missing ledger output: $LEDGER_FILE" >&2; exit 1; }

if [[ "$WITH_ALLOCATION" -eq 1 ]]; then
  [[ -s "$OUTPUT_DIR/ALLOC_SJE2003.csv" ]] || { echo "Missing allocation SJE output" >&2; exit 1; }
  [[ -s "$OUTPUT_DIR/SortedJEFY03_ALLOC.csv" ]] || { echo "Missing sorted allocation output" >&2; exit 1; }
  allocation_rows=$(awk 'NR > 1 { count++ } END { print count + 0 }' "$OUTPUT_DIR/ALLOC_SJE2003.csv")
  [[ "$allocation_rows" -gt 0 ]] || {
    echo "Allocation produced no generated SJEs" >&2
    exit 1
  }
  next_divisor_rows=$(awk 'NR > 1 { count++ } END { print count + 0 }' \
    "$OUTPUT_DIR/AllocationDivisors_FY03_after_allocation.csv")
  [[ "$next_divisor_rows" -gt 0 ]] || { echo "Missing next-period divisor groups" >&2; exit 1; }
fi

awk -F, '
  NR > 1 { rows++; total += $20 }
  END {
    if (rows == 0 || total < -0.01 || total > 0.01) exit 1
    printf "Fixture check passed: %d ledger rows, amount sum %.2f\n", rows, total
  }
' "$LEDGER_FILE"

if [[ "$SHOW_RESULTS" -eq 1 ]]; then
  bash "$SCRIPT_DIR/show_repo_fixture_results.sh" "$OUTPUT_DIR" \
    "$INPUT_DIR/AllocationDivisors_FY02_fixture.csv"
fi

if [[ "$KEEP_OUTPUT" -eq 1 ]]; then
  echo
  echo "Inspect the retained directory with:"
  echo "  ls -la \"$OUTPUT_DIR\""
fi
