#!/usr/bin/env bash
# =============================================================================
#  archive_corpus_run_evidence.sh — Archive full-corpus run evidence into the repo
#
#  Collects lightweight canonical metrics, manifests, cost surfaces, sort events,
#  reconciliation results, and wallclock CSVs from an external run root and logs dir,
#  and commits them into data/test_evidence/corpus_runs/<run_tag>/.
#
#  USAGE:
#    bash scripts/archive_corpus_run_evidence.sh [--root DIR] [--logs DIR] [--tag NAME] [--out DIR]
#
#  Defaults:
#    root=$HOME/workspace/va_fresh_root/output_full
#    logs=$HOME/workspace/va_fresh_logs
#    out=$REPO/data/test_evidence/corpus_runs/<tag>
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/.." && pwd)"

ROOT="$HOME/workspace/va_fresh_root/output_full"
LOGS="$HOME/workspace/va_fresh_logs"
TAG=""
OUT=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) ROOT="$2"; shift 2 ;;
    --logs) LOGS="$2"; shift 2 ;;
    --tag)  TAG="$2";  shift 2 ;;
    --out)  OUT="$2";  shift 2 ;;
    -h|--help)
      grep '^#' "$0" | sed 's/^# \{0,2\}//'
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      exit 1
      ;;
  esac
done

# Find latest wallclock file if tag not specified
if [[ -z "$TAG" ]]; then
  LATEST_WALL="$(ls -t "$LOGS"/corpus_wallclock_*.csv 2>/dev/null | head -n 1 || true)"
  if [[ -n "$LATEST_WALL" ]]; then
    TAG="$(basename "$LATEST_WALL" .csv | sed 's/corpus_wallclock_//')"
  else
    TAG="$(date '+%Y%m%d_%H%M%S')"
  fi
fi

if [[ -z "$OUT" ]]; then
  OUT="$REPO/data/test_evidence/corpus_runs/run_$TAG"
fi

mkdir -p "$OUT"

echo "Archiving corpus run evidence to $OUT ..."

# 1. Wallclock CSV
WALL_FILE="$LOGS/corpus_wallclock_$TAG.csv"
if [[ -f "$WALL_FILE" ]]; then
  cp "$WALL_FILE" "$OUT/corpus_wallclock.csv"
  echo "  Copied wallclock record: $WALL_FILE"
fi

# 2. Collect per-year metrics and combine them
YEAR_DIRS=($(find "$ROOT" -maxdepth 1 -type d -o -type l | grep -E '/FY[0-9]{2}$' | sort))

if [[ ${#YEAR_DIRS[@]} -eq 0 ]]; then
  echo "WARNING: No FY* output directories found under $ROOT"
fi

combine_csv() {
  local target_name="$1"
  local subpath="$2"
  local out_file="$OUT/$target_name"
  local header_written=0

  : > "$out_file"
  for ydir in "${YEAR_DIRS[@]}"; do
    local f="$ydir/$subpath"
    if [[ -f "$f" ]]; then
      if [[ $header_written -eq 0 ]]; then
        head -n 1 "$f" >> "$out_file"
        header_written=1
      fi
      tail -n +2 "$f" >> "$out_file"
    fi
  done
  if [[ -s "$out_file" ]]; then
    local count=$(tail -n +2 "$out_file" | wc -l | tr -d ' ')
    echo "  Combined $target_name ($count rows)"
  else
    rm -f "$out_file"
  fi
}

combine_csv "cost_surface_all_years.csv" "cost_surface.csv"
combine_csv "reconciliation_results_all_years.csv" "metrics/reconciliation_results.csv"
combine_csv "process_metrics_all_years.csv" "metrics/process_metrics.csv"
combine_csv "run_manifest_all_years.csv" "metrics/run_manifest.csv"
combine_csv "partition_catalog_all_years.csv" "metrics/partition_catalog.csv"
combine_csv "sort_events_all_years.csv" "metrics/sort_events.csv"

# The cross-year 5v pass writes to $ROOT/VIEWS, not to a FY<yy> tree, so it is not in the combined tables above.
if [[ -f "$ROOT/VIEWS/cost_surface.csv" ]]; then
  cp "$ROOT/VIEWS/cost_surface.csv" "$OUT/views_cost_surface.csv"
  echo "  Copied 5v cost surface"
fi

# 3. Quarantine summary
QUARANTINE_SUMMARY="$OUT/quarantine_summary.csv"
echo "fiscal_year,quarantine_file,rows" > "$QUARANTINE_SUMMARY"
for ydir in "${YEAR_DIRS[@]}"; do
  fy="$(basename "$ydir")"
  for qfile in "$ydir"/POQuarantine*.csv; do
    if [[ -f "$qfile" ]]; then
      qrows=$(wc -l < "$qfile" | tr -d ' ')
      # subtract header if present
      (( qrows > 0 )) && qrows=$(( qrows - 1 ))
      echo "$fy,$(basename "$qfile"),$qrows" >> "$QUARANTINE_SUMMARY"
    fi
  done
done
echo "  Written quarantine summary ($QUARANTINE_SUMMARY)"

# 3b. Corpus-level size and row totals (post-run Python reporting, not part of the measured engine)
if command -v python3 >/dev/null 2>&1; then
  python3 "$SCRIPT_DIR/corpus_totals.py" --archive "$OUT" --root "$ROOT" \
    --catalog "$REPO/data/corpus_profile/source_catalog.csv" > /dev/null \
    && echo "  Written corpus totals ($OUT/corpus_totals.csv)" \
    || echo "  WARNING: corpus_totals.py failed; no corpus_totals.csv" >&2
fi

# 4. Generate Markdown summary
cat <<EOF > "$OUT/RUN_SUMMARY.md"
# Full Corpus Run Summary: $TAG

- **Run Tag / Timestamp**: \`$TAG\`
- **Source Root**: \`$ROOT\`
- **Logs Root**: \`$LOGS\`
- **Archived Date**: $(date '+%Y-%m-%d %H:%M:%S')

## Reconciliation Controls Summary

\`\`\`csv
$(if [[ -f "$OUT/reconciliation_results_all_years.csv" ]]; then cat "$OUT/reconciliation_results_all_years.csv"; else echo "No reconciliation results found."; fi)
\`\`\`

## Cost Surface Summary

\`\`\`csv
$(if [[ -f "$OUT/cost_surface_all_years.csv" ]]; then cat "$OUT/cost_surface_all_years.csv"; else echo "No cost surface found."; fi)
\`\`\`

## Quarantine File Row Counts

\`\`\`csv
$(cat "$QUARANTINE_SUMMARY")
\`\`\`

## Wallclock Execution Timeline

\`\`\`csv
$(if [[ -f "$OUT/corpus_wallclock.csv" ]]; then cat "$OUT/corpus_wallclock.csv"; else echo "No wallclock record found."; fi)
\`\`\`
EOF

echo "Archive complete: $OUT/RUN_SUMMARY.md"
