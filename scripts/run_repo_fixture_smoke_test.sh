#!/usr/bin/env bash
# Run the checked-in FY03 small fixture through the no-Spark VA path.
# This intentionally uses a temporary staging directory so the repository data
# and generated outputs are not modified.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
TEMP_ROOT="${TMPDIR:-$REPO_ROOT/session-run.tmp}"
mkdir -p "$TEMP_ROOT"
RUN_ROOT="$(mktemp -d "${TEMP_ROOT%/}/ledger-lab-fixture.XXXXXX")"
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
    --show-results|--show-data)
      SHOW_RESULTS=1
      shift
      ;;
    --with-allocation)
      WITH_ALLOCATION=1
      shift
      ;;
    -h|--help)
      cat <<'EOF'
Usage: bash scripts/run_repo_fixture_smoke_test.sh [--show-data] [--keep-output] [--with-allocation]

Stages data/FY03q1exp_small.txt as FY03q1exp.txt, runs VA Steps 2 and 3,
and verifies that sorted journal and ledger output are produced and balanced.
No external VA data, Spark, PostgreSQL, or AI engine is required.

Use --show-data (alias: --show-results) to print small journal, ledger, count, and summary results.
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
  if [[ -n "$(tail -c 1 "$INPUT_DIR/FY03q1exp.txt")" ]]; then
    printf '\n' >> "$INPUT_DIR/FY03q1exp.txt"
  fi
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

python3 - "$OUTPUT_DIR" <<'PY'
import csv
import re
import sys
from pathlib import Path

output = Path(sys.argv[1])
metrics = output / "metrics"

def rows(path):
    with path.open(newline="", encoding="utf-8") as source:
        return list(csv.DictReader(source))

manifest = rows(metrics / "run_manifest.csv")[0]
assert manifest["status"] == "complete"
assert manifest["wall_seconds"].isdigit()
assert manifest["git_revision"]

processes = rows(metrics / "process_metrics.csv")
allocation_enabled = manifest["workload_profile"] == "allocation-active"
assert len(processes) == (3 if allocation_enabled else 2)
assert all(None not in process and all(value is not None for value in process.values()) for process in processes)
with (metrics / "process_metrics.csv").open(newline="", encoding="utf-8") as source:
  process_header = next(csv.reader(source))
assert len(processes[0]) == len(process_header)
step_two = next(process for process in processes if process["process_name"] == "step-2")
step_three = next(process for process in processes if process["process_name"] == "step-3")
assert step_three["input_rows"] == step_two["output_rows"]
if allocation_enabled:
  assert int(step_two["output_rows"]) > 1520
else:
  assert step_two["output_rows"] == "1520"
  assert step_three["output_rows"] == "59"
assert all(int(process[field]) > 0 for process in processes for field in ("input_bytes", "output_bytes"))
if allocation_enabled:
  step_six = next(process for process in processes if process["process_name"] == "allocation")
  assert step_six["input_rows"] == step_three["output_rows"]
  assert step_six["output_rows"] == "45"

cost = rows(output / "cost_surface.csv")[0]
assert cost["total_storage_bytes"].isdigit() and int(cost["total_storage_bytes"]) > 0

interpretation = rows(output / "interpretation" / "interpretation.csv")[0]
assert interpretation["run_activity"] == "active"
assert interpretation["git_revision"] == manifest["git_revision"]
report = (output / "interpretation" / "run_report.md").read_text(encoding="utf-8")
assert f"- Wall seconds: {manifest['wall_seconds']}" in report

log = (output / "pipeline_results.log").read_text(encoding="utf-8")
step_three_result = next(line for line in log.splitlines() if "RESULT step=3 " in line)
assert re.search(
  rf"records_read={step_three['input_rows']} records_written={step_three['output_rows']} input_bytes=\d+ output_bytes=\d+ outPath_bytes=\d+",
  step_three_result,
)
print("Metric checks passed: process rows, byte counts, manifest interpretation")
PY

if [[ "$SHOW_RESULTS" -eq 1 ]]; then
  bash "$SCRIPT_DIR/show_repo_fixture_results.sh" "$OUTPUT_DIR" \
    "$INPUT_DIR/AllocationDivisors_FY02_fixture.csv"
fi

if [[ "$KEEP_OUTPUT" -eq 1 ]]; then
  echo
  echo "Inspect the retained directory with:"
  echo "  ls -la \"$OUTPUT_DIR\""
fi
