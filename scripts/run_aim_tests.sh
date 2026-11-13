#!/usr/bin/env bash
# =============================================================================
#  run_aim_tests.sh -- orchestrate the AIM1, AIM3, AIM3-balance and AIM4 runs
#
#  Orchestration only: hashes the Gold inputs against the Gold MANIFEST.csv,
#  runs the post-engine Python analysis scripts in order, and records the host,
#  package versions, git revision and timestamps.  No analysis logic is here.
#
#  USAGE:
#    bash scripts/run_aim_tests.sh [--gold PATH] [--out DIR] [--python PATH]
#
#  Outputs go to DIR (default data/test_evidence/aim_run_<host-arch>_<UTC time>)
#  so earlier evidence CSVs are not overwritten:
#    RUN_ENV.txt            host, python, packages, git revision, start/end time
#    RUN_INPUTS.csv         rel_path, bytes, sha256, manifest_match
#    AIM3_survival_windows.csv, AIM3_balance_baseline.csv,
#    AIM4_seasonality_results.csv, AIM4_risk_table.csv (if produced),
#    AIM1_all_years/AIM1_noninjectivity_FYyyyy.csv, AIM1_noninjectivity_all_years.csv
# =============================================================================

set -euo pipefail

GOLD="${HOME}/Library/CloudStorage/OneDrive-Personal/LedgerLabOutputs-Gold"
PY="${HOME}/.venvs/ledger-lab-arm/bin/python"
OUT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --gold)   GOLD="$2"; shift 2 ;;
    --out)    OUT="$2";  shift 2 ;;
    --python) PY="$2";   shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done

REPO="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO"

START_UTC="$(date -u +%Y%m%dT%H%M%SZ)"
[[ -n "$OUT" ]] || OUT="data/test_evidence/aim_run_$(uname -m)_${START_UTC}"
mkdir -p "$OUT/AIM1_all_years"

[[ -x "$PY" ]] || { echo "Python not found: $PY" >&2; exit 1; }
[[ -f "$GOLD/MANIFEST.csv" ]] || { echo "Gold MANIFEST.csv not found under $GOLD" >&2; exit 1; }

# --- Environment record -------------------------------------------------------
{
  echo "start_local: $(date '+%Y-%m-%d %H:%M:%S')"
  echo "start_utc: ${START_UTC}"
  echo "host_arch: $(uname -m)"
  echo "os: $(uname -sr)"
  echo "python: $("$PY" --version 2>&1)"
  "$PY" -c "import numpy,sklearn,scipy,statsmodels;print('packages: numpy',numpy.__version__,'scikit-learn',sklearn.__version__,'scipy',scipy.__version__,'statsmodels',statsmodels.__version__)"
  echo "git_head: $(git rev-parse HEAD)"
  echo "git_dirty: $(git status --porcelain scripts | wc -l | tr -d ' ') changed file(s) under scripts/"
  echo "gold_root: ${GOLD}"
} > "$OUT/RUN_ENV.txt"

# --- Input hashes against the Gold MANIFEST.csv -------------------------------
# Only the files the four tests read are hashed.
INPUTS=()
for yy in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do
  for f in LDGR GL PERM_TBL; do INPUTS+=("FY${yy}/${f}20${yy}.csv"); done
done
INPUTS+=("VIEWS/view_vendor_year_survival.csv" "VIEWS/view_monthly_order_volume.csv")

echo "rel_path,bytes,sha256,manifest_match" > "$OUT/RUN_INPUTS.csv"
bad=0
for rel in "${INPUTS[@]}"; do
  [[ -f "$GOLD/$rel" ]] || { echo "MISSING: $GOLD/$rel" >&2; bad=1; continue; }
  size="$(stat -f %z "$GOLD/$rel")"
  sha="$(shasum -a 256 "$GOLD/$rel" | awk '{print $1}')"
  want="$(awk -F, -v p="$rel" '$1==p {print $2","$3}' "$GOLD/MANIFEST.csv")"
  if [[ "$want" == "${size},${sha}" ]]; then match=yes; else match=NO; bad=1; fi
  echo "${rel},${size},${sha},${match}" >> "$OUT/RUN_INPUTS.csv"
done
if [[ $bad -ne 0 ]]; then
  echo "Input check failed (missing file or hash mismatch); see $OUT/RUN_INPUTS.csv" >&2
  exit 2
fi
echo "Inputs verified: ${#INPUTS[@]} files match Gold MANIFEST.csv"

# --- AIM3 commitment windows --------------------------------------------------
"$PY" scripts/fit_vendor_survival_windows.py \
  --view "$GOLD/VIEWS/view_vendor_year_survival.csv" \
  --evidence-out "$OUT/AIM3_survival_windows.csv" | tee "$OUT/AIM3_survival_windows.txt"

# --- AIM3 balance baseline (reads the commitment holdout row just written) ----
"$PY" scripts/fit_vendor_survival_balance.py \
  --ldgr-root "$GOLD" \
  --view "$GOLD/VIEWS/view_vendor_year_survival.csv" \
  --commitment-csv "$OUT/AIM3_survival_windows.csv" \
  --evidence-out "$OUT/AIM3_balance_baseline.csv" | tee "$OUT/AIM3_balance_baseline.txt"

# --- AIM4 seasonality ---------------------------------------------------------
"$PY" scripts/fit_ordering_seasonality.py \
  --view "$GOLD/VIEWS/view_monthly_order_volume.csv" \
  --evidence-out "$OUT/AIM4_seasonality_results.csv" \
  --risk-out "$OUT/AIM4_risk_table.csv" | tee "$OUT/AIM4_seasonality.txt"

# --- AIM1 non-injectivity, one run per fiscal year ----------------------------
for yy in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do
  "$PY" scripts/fit_noninjectivity.py \
    --ldgr "$GOLD/FY${yy}/LDGR20${yy}.csv" \
    --perm "$GOLD/FY${yy}/PERM_TBL20${yy}.csv" \
    --gl   "$GOLD/FY${yy}/GL20${yy}.csv" \
    --evidence-out "$OUT/AIM1_all_years/AIM1_noninjectivity_FY20${yy}.csv" \
    > "$OUT/AIM1_all_years/AIM1_noninjectivity_FY20${yy}.txt"
done

# Consolidate: fiscal_year + the first nine columns of each per-year file
CONS="$OUT/AIM1_noninjectivity_all_years.csv"
echo "fiscal_year,label,key_count,total_distinct_patterns,mean_patterns_per_key,max_patterns_per_key,keys_with_multiple_patterns,collapse_ratio,run_timestamp" > "$CONS"
for yy in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do
  tail -n +2 "$OUT/AIM1_all_years/AIM1_noninjectivity_FY20${yy}.csv" \
    | awk -F, -v y="20${yy}" 'BEGIN{OFS=","} {print y,$1,$2,$3,$4,$5,$6,$7,$8}' >> "$CONS"
done

echo "end_local: $(date '+%Y-%m-%d %H:%M:%S')" >> "$OUT/RUN_ENV.txt"
echo "Done. Evidence in $OUT"
