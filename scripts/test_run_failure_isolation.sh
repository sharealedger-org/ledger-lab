#!/usr/bin/env bash
# Verify that failed pipeline attempts are isolated from the committed output root
# and a normal fixture run still succeeds.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

TMP_ROOT="$(mktemp -d "${TMPDIR:-/tmp}/ledger-lab-failure-isolation.XXXXXX")"
FINAL_ROOT="$TMP_ROOT/final"
mkdir -p "$FINAL_ROOT"
printf 'keep-me\n' > "$FINAL_ROOT/keep.txt"

if bash "$REPO_ROOT/scripts/run_pipeline.sh" \
  --inPath "$TMP_ROOT/missing" \
  --outPath "$FINAL_ROOT" \
  --years 03 \
  --steps 2,3 \
  --curve C1 \
  --config failure-check >/tmp/ledger-lab-failure-isolation.log 2>&1; then
  echo "Expected failure injection to exit non-zero" >&2
  exit 1
fi

[[ -f "$FINAL_ROOT/keep.txt" ]] || {
  echo "Failed run removed the previous committed output" >&2
  exit 1
}

# A valid fixture run should still succeed and leave the output root intact.
bash "$REPO_ROOT/scripts/run_repo_fixture_smoke_test.sh" --show-data >/tmp/ledger-lab-fixture.log 2>&1

printf 'RUN_FAILURE_ISOLATION: PASS\n'
