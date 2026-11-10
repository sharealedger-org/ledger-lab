#!/usr/bin/env bash
# =============================================================================
# Track C (Berka PKDD'99) -- Paper 2 Empirical Pipeline Runner (v3, 2026-11-10)
# Design: docs/TEST_DESIGN_TRACK_C_BERKA.md
# Steps:
#   1  Download and verify raw Berka files (Bash)
#   2  Build Substrate B (daily balances) and E (transaction events) (Python)
#   3  v2 fit: repeated-CV, paired bootstrap, post-hoc ablation (Python)
#   4  Direct non-injectivity count of the daily-balance grain (Python, stdlib only)
#   5  v3 fit: nested-CV lambda, temporal split, permutation test, locked thresholds (Python)
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
echo "[Step 1/5] Downloading and verifying Berka raw datasets..."
bash scripts/download_berka.sh --data-root "${BERKA_DATA_ROOT}"

# Step 2: Build the two views of the same pre-loan transaction rows
echo ""
echo "[Step 2/5] Building Substrate E (transactions) and Substrate B (daily balances)..."
python3 scripts/berka/build_berka_substrates.py \
  --berka-dir "${BERKA_DATA_ROOT}/berka" \
  --out-dir data/berka_processed

# Step 3: Fit and evaluate (shared folds, paired intervals, baselines, overdraft-free subset)
echo ""
echo "[Step 3/5] v2 fit: repeated-CV, paired bootstrap, ablation..."
python3 scripts/berka/fit_track_c_models.py \
  --data-dir data/berka_processed \
  --evidence-out data/test_evidence/TRACK_C_BERKA_EVIDENCE.json

# Step 4: Direct non-injectivity count of the daily-balance grain (no model, standard library only)
echo ""
echo "[Step 4/5] Counting event patterns collapsed by the daily balance..."
python3 scripts/berka/count_noninjectivity.py \
  --berka-dir "${BERKA_DATA_ROOT}/berka" \
  --evidence-out data/test_evidence/TRACK_C_BERKA_NONINJECTIVITY.json

# Step 5: v3 fit — nested-CV lambda tuning, temporal split, permutation test, locked thresholds
# Note: the permutation test runs N_PERM=999 full nested-CV fits on permuted features.
# Expect this step to take substantially longer than step 3.
echo ""
echo "[Step 5/5] v3 fit: nested-CV tuning, temporal split, permutation test..."
python3 scripts/berka/fit_track_c_v3.py \
  --data-dir data/berka_processed \
  --evidence-out data/test_evidence/TRACK_C_BERKA_V3_EVIDENCE.json

echo ""
echo "====================================================================="
echo "  Track C run complete! Evidence saved to:"
echo "  data/test_evidence/TRACK_C_BERKA_EVIDENCE.json"
echo "  data/test_evidence/TRACK_C_BERKA_NONINJECTIVITY.json"
echo "  data/test_evidence/TRACK_C_BERKA_V3_EVIDENCE.json"
echo "====================================================================="
