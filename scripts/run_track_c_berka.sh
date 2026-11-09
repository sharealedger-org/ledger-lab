#!/usr/bin/env bash
# =============================================================================
# Track C (Berka PKDD'99) -- Paper 2 Empirical Pipeline Runner
# =============================================================================
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${REPO_ROOT}"

echo "====================================================================="
echo "  Track C: Retail Banking Benchmark (Berka PKDD'99 Corpus)"
echo "  Paper 2: Information Ceiling & Lossy Compression Test"
echo "====================================================================="

# Activate virtual environment if present
if [ -d ".venv" ]; then
  # shellcheck source=/dev/null
  source .venv/bin/activate
fi

# Step 1: Resolve / Unpack fixtures
echo ""
echo "[Step 1/3] Resolving Berka raw datasets..."
python3 scripts/berka/fetch_berka_data.py --target-dir data/berka

# Step 2: Build Substrates (Layer 2 Balances vs Layer 3 Events)
echo ""
echo "[Step 2/3] Building Substrates B (Layer 2) and E (Layer 3)..."
python3 scripts/berka/build_berka_substrates.py \
  --berka-dir data/berka \
  --out-dir data/berka_processed

# Step 3: Train & Evaluate Information Loss & Non-Injectivity
echo ""
echo "[Step 3/3] Evaluating Layer 2 vs Layer 3 models..."
python3 scripts/berka/fit_track_c_models.py \
  --data-dir data/berka_processed \
  --evidence-out data/test_evidence/TRACK_C_BERKA_EVIDENCE.json

echo ""
echo "====================================================================="
echo "  Track C run complete! Evidence saved to:"
echo "  data/test_evidence/TRACK_C_BERKA_EVIDENCE.json"
echo "====================================================================="
