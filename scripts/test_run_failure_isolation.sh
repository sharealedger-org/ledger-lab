#!/usr/bin/env bash
# Verify that failed pipeline attempts are isolated from the committed output root
# and a normal fixture run still succeeds.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

TMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/ledger-lab-failure-isolation.XXXXXX")"
FINAL_ROOT="$TMP_ROOT/final"
INPUT_ROOT="$TMP_ROOT/input"
trap 'rm -rf "$TMP_ROOT"' EXIT
mkdir -p "$FINAL_ROOT" "$INPUT_ROOT"
cp "$REPO_ROOT/data/FY03q1exp_small.txt" "$INPUT_ROOT/FY03q1exp.txt"
cp "$REPO_ROOT/data/VendorMaster.csv" "$REPO_ROOT/data/VAAccountingRules.csv" "$INPUT_ROOT/"

bash "$REPO_ROOT/scripts/run_pipeline.sh" \
  --inPath "$INPUT_ROOT" --outPath "$FINAL_ROOT" --years 03 --steps 2,3 \
  --curve C1 --config baseline >"$TMP_ROOT/baseline.log" 2>&1
[[ -L "$FINAL_ROOT" && -s "$FINAL_ROOT/LDGR2003.csv" ]]
committed_target="$(readlink "$FINAL_ROOT")"
committed_checksum="$(cksum "$FINAL_ROOT/LDGR2003.csv")"
committed_generation_count="$(find "$FINAL_ROOT.generations" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')"

if bash "$REPO_ROOT/scripts/run_pipeline.sh" \
  --inPath "$INPUT_ROOT" \
  --outPath "$FINAL_ROOT" \
  --years 03 \
  --steps 2,3,12 \
  --curve C1 \
  --config failure-check >"$TMP_ROOT/failed.log" 2>&1; then
  echo "Expected failure injection to exit non-zero" >&2
  exit 1
fi

grep -q 'RESULT step=3 .* rc=0' "$TMP_ROOT/failed.log" || {
  echo "Failure occurred before terminal ledger output" >&2
  exit 1
}
[[ "$(readlink "$FINAL_ROOT")" == "$committed_target" && \
   "$(cksum "$FINAL_ROOT/LDGR2003.csv")" == "$committed_checksum" ]] || {
  echo "Failed run changed the committed ledger generation" >&2
  exit 1
}
[[ "$(find "$FINAL_ROOT.generations" -mindepth 1 -maxdepth 1 -type d | wc -l | tr -d ' ')" == "$committed_generation_count" ]] || {
  echo "Failed attempt was not cleaned" >&2
  exit 1
}
[[ "$(find "$FINAL_ROOT.generations" -maxdepth 1 -name '*.failed.log' | wc -l | tr -d ' ')" == 1 ]] || {
  echo "Failed attempt log was not retained" >&2
  exit 1
}

printf 'RUN_FAILURE_ISOLATION: PASS\n'
