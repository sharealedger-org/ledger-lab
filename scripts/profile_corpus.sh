#!/usr/bin/env bash
# =============================================================================
#  profile_corpus.sh -- inventory and profile the external VA source corpus
#
#  Runs the Scala CorpusProfile inspector (single streaming pass per file).
#  Data inspection only: it posts nothing and is not part of any cost-curve
#  measurement. Outputs are deterministic for identical inputs.
#
#  USAGE:
#    bash scripts/profile_corpus.sh [--data-root PATH] [--out PATH]
#                                   [--dirs VARawFiles,VAPOData] [--only SUBSTRING]
#
#  Defaults: data root = $UL_DATA_ROOT or data ; out = <data root>/corpus_profile
#  Outputs (see docs/CORPUS_ACCESS.md): source_catalog.csv, file_profile.csv,
#  file_field_profile.csv, schema_catalog.csv, coverage_matrix.csv,
#  quarantine.csv, corpus_summary.txt
# =============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DATA_ROOT="${UL_DATA_ROOT:-$REPO_ROOT/data}"
OUT=""
DIRS="VARawFiles,VAPOData"
ONLY=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --data-root) DATA_ROOT="$2"; shift 2 ;;
    --out)       OUT="$2"; shift 2 ;;
    --dirs)      DIRS="$2"; shift 2 ;;
    --only)      ONLY="$2"; shift 2 ;;
    -h|--help)
      echo "Usage: bash scripts/profile_corpus.sh [--data-root PATH] [--out PATH] [--dirs VARawFiles,VAPOData] [--only SUBSTRING]"
      exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

[[ -n "$OUT" ]] || OUT="$DATA_ROOT/corpus_profile"
mkdir -p "$OUT"

# Absolute paths: sbt runs from the Scala project directory.
DATA_ROOT="$(cd "$DATA_ROOT" && pwd)"
OUT="$(cd "$OUT" && pwd)"

SBT_PROJECT="$REPO_ROOT/02-foundation/va_pipeline"
echo "[profile_corpus] root=$DATA_ROOT out=$OUT dirs=$DIRS only='$ONLY'"

cd "$SBT_PROJECT"
ARGS="--root $DATA_ROOT --out $OUT --dirs $DIRS"
[[ -z "$ONLY" ]] || ARGS="$ARGS --only $ONLY"
SBT_OPTS="${SBT_OPTS:--Xmx768m}" sbt -batch \
  "runMain org.universalledger.foundation.va.corpus.CorpusProfile $ARGS" \
  2>&1 | tee "$OUT/profile_run.log"
