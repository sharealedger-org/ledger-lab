#!/usr/bin/env bash
# =============================================================================
# Track C (Berka PKDD'99) -- Paper 2 Empirical Pipeline Runner (v2, 2026-11-10)
# Design: docs/TEST_DESIGN_TRACK_C_BERKA.md
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_ROOT}"

echo "====================================================================="
echo "  Track C: Retail Banking Benchmark (Berka PKDD'99 Corpus)"
echo "  Paper 2: B (daily balances) vs E (transaction events), same rows"
echo "====================================================================="

# Activate virtual environment if present
if [ -d ".venv" ]; then
  # shellcheck source=/dev/null
  source .venv/bin/activate
fi

# Step 1: Download and verify the public Berka files (Bash; checksum-pinned).
# Needs BERKA_DATA_ROOT (a directory outside the repository); files land in $BERKA_DATA_ROOT/berka.
: "${BERKA_DATA_ROOT:?Set BERKA_DATA_ROOT to a directory outside the repository}"
echo ""
echo "[Step 1/3] Downloading and verifying Berka raw datasets..."
bash scripts/download_berka.sh --data-root "${BERKA_DATA_ROOT}"

# Step 2: Build the two views of the same pre-loan transaction rows
echo ""
echo "[Step 2/3] Building Substrate E (transactions) and Substrate B (daily balances)..."
python3 scripts/berka/build_berka_substrates.py \
  --berka-dir "${BERKA_DATA_ROOT}/berka" \
  --out-dir data/berka_processed

# Step 3: Fit and evaluate (shared folds, paired intervals, baselines, overdraft-free subset)
echo ""
echo "[Step 3/3] Evaluating B vs E..."
python3 scripts/berka/fit_track_c_models.py \
  --data-dir data/berka_processed \
  --evidence-out data/test_evidence/TRACK_C_BERKA_EVIDENCE.json

echo ""
echo "====================================================================="
echo "  Track C run complete! Evidence saved to:"
echo "  data/test_evidence/TRACK_C_BERKA_EVIDENCE.json"
echo "====================================================================="
