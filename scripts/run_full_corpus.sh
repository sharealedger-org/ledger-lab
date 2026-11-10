#!/usr/bin/env bash
# =============================================================================
#  run_full_corpus.sh — full-corpus VA run orchestration (Bash: job control only)
#
#  Topology (all financial work happens in compiled Scala steps invoked by run_pipeline.sh):
#
#    PREP lanes (parallel, any number)   per year:  ttl,2p,2m      -> $OUT_ROOT/FY<yy>
#    CHAIN     (serial, year order)      per year:  3,20,21        -> continues $OUT_ROOT/FY<yy>
#                                        starts a year as soon as that year's prep has committed
#    VIEWS     (one pass, after chain)   5v over every year's merged file, k-way merged by engine key
#                                        -> $OUT_ROOT/VIEWS   (cross-year view; see CKBViews.scala)
#
#  Timing: per-year step rows stay in each year's cost_surface.csv (written by run_pipeline.sh).
#  Corpus wall-clock is a separate record, $LOGS/corpus_wallclock_<stamp>.csv, from start/end stamps
#  taken around the whole launch (plus phase stamps). With overlapping lanes the per-step elapsed_s
#  values are NOT comparable with serial baselines; the wall-clock record is the corpus-level figure.
#
#  Stop rule: the first failing step stops new work (running steps finish), and the VIEWS pass is
#  not started. A year whose prep failed is never chained.
#
#  USAGE:
#    bash scripts/run_full_corpus.sh [--root DIR] [--logs DIR] [--years 03,04,...,16] [--lanes N]
#                                    [--periodGranularity monthly|annual]
#    Defaults: root=$HOME/workspace/va_fresh_root  logs=$HOME/workspace/va_fresh_logs
#              years=03..16  lanes=4  periodGranularity=monthly
#    The output root ($ROOT/output_full) must hold no FY* entries; move or delete the old output first.
#
#  Nothing here selects or computes financial data. Do not edit Scala while this runs (sbt would
#  recompile under the lanes).
# =============================================================================
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/.." && pwd)"

# ── worker entry points (re-invocations of this script by xargs / the chain) ──
if [[ "${1:-}" == "--worker" ]]; then
  kind="$2"; y="$3"
  say() { echo "[$(date '+%F %T')] $*"; }
  cd "$REPO" || exit 255
  case "$kind" in
    prep)
      # Stagger sbt starts so several launchers do not hit project/target at the same moment.
      sleep $(( (10#$y % LANES) * 20 ))
      say "=== FY20$y prep start (ttl,2p,2m) ==="
      bash scripts/run_pipeline.sh \
        --inPath "$ROOT/VARawFiles" --outPath "$OUT_ROOT/FY$y" \
        --years "$y" --steps ttl,2p,2m --curve CCP2 \
        --config "fullcorpus-run2-prep-fy$y" --periodGranularity "$GRANULARITY" \
        > "$LOGS/run2_prep_fy$y.log" 2>&1
      rc=$?
      say "=== FY20$y prep exit $rc ==="
      if [[ $rc -eq 0 ]]; then : > "$MARKERS/prep_$y.ok"; else : > "$MARKERS/prep_$y.fail"; exit 255; fi
      exit 0
      ;;
  esac
  exit 1
fi

ROOT="$HOME/workspace/va_fresh_root"
LOGS="$HOME/workspace/va_fresh_logs"
YEARS="03,04,05,06,07,08,09,10,11,12,13,14,15,16"
LANES=4
GRANULARITY="monthly"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root)  ROOT="$2";  shift 2 ;;
    --logs)  LOGS="$2";  shift 2 ;;
    --years) YEARS="$2"; shift 2 ;;
    --lanes) LANES="$2"; shift 2 ;;
    --periodGranularity) GRANULARITY="$2"; shift 2 ;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,2\}//'; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

OUT_ROOT="$ROOT/output_full"
STAMP="$(date '+%Y%m%d_%H%M%S')"
MARKERS="$LOGS/markers_$STAMP"
WALL="$LOGS/corpus_wallclock_$STAMP.csv"
export REPO ROOT LOGS OUT_ROOT MARKERS LANES GRANULARITY
say() { echo "[$(date '+%F %T')] $*"; }

mkdir -p "$LOGS" "$MARKERS" "$OUT_ROOT"
if compgen -G "$OUT_ROOT/FY*" >/dev/null; then
  echo "ERROR: $OUT_ROOT already holds FY* entries; move or delete them before a from-scratch run." >&2
  exit 1
fi

stamp() {  # event
  printf '%s,%s,%s\n' "$1" "$(date +%s)" "$(date '+%F %T')" >> "$WALL"
}
echo "event,epoch_s,local_time" > "$WALL"
stamp corpus_start
{
  echo "# years=$YEARS lanes=$LANES periodGranularity=$GRANULARITY"
  echo "# git=$(cd "$REPO" && git rev-parse --short HEAD 2>/dev/null) dirty_files=$(cd "$REPO" && git status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
  echo "# host=$(uname -n) cpus=$(sysctl -n hw.ncpu 2>/dev/null || nproc)"
} >> "$WALL"

IFS=',' read -ra YEAR_LIST <<< "$YEARS"

# ── serial chain: 3,20,21 per year, in year order, as each year's prep commits ──
chain() {
  local y rc
  for y in "${YEAR_LIST[@]}"; do
    say "chain: waiting for FY20$y prep"
    while [[ ! -f "$MARKERS/prep_$y.ok" ]]; do
      if compgen -G "$MARKERS/prep_*.fail" >/dev/null; then say "chain: a prep lane failed; stopping"; return 1; fi
      sleep 10
    done
    stamp "chain_start_fy$y"
    say "=== FY20$y chain start (3,20,21) ==="
    ( cd "$REPO" && bash scripts/run_pipeline.sh \
        --inPath "$ROOT/VARawFiles" --outPath "$OUT_ROOT/FY$y" --seedFromCommitted \
        --years "$y" --steps 3,20,21 --curve CCP1 \
        --config "fullcorpus-run2-chain-fy$y" --periodGranularity "$GRANULARITY" \
        > "$LOGS/run2_chain_fy$y.log" 2>&1 )
    rc=$?
    stamp "chain_end_fy$y"
    say "=== FY20$y chain exit $rc ==="
    [[ $rc -eq 0 ]] || { : > "$MARKERS/chain_$y.fail"; return 1; }
  done
  return 0
}

say "corpus start: years=$YEARS lanes=$LANES  wall-clock record: $WALL"
chain &
CHAIN_PID=$!

printf '%s\n' "${YEAR_LIST[@]}" | xargs -n1 -P"$LANES" -I{} bash "$SCRIPT_DIR/run_full_corpus.sh" --worker prep {}
PREP_RC=$?
stamp prep_all_done
say "prep lanes finished rc=$PREP_RC"

wait "$CHAIN_PID"
CHAIN_RC=$?
stamp chain_all_done
say "chain finished rc=$CHAIN_RC"

# ── Archive evidence to repository on exit ──
archive_evidence() {
  say "Archiving run evidence into repository..."
  bash "$SCRIPT_DIR/archive_corpus_run_evidence.sh" \
    --root "$OUT_ROOT" \
    --logs "$LOGS" \
    --tag "$STAMP" || say "WARNING: Evidence archiving encountered an error"
}

if [[ $PREP_RC -ne 0 || $CHAIN_RC -ne 0 ]]; then
  stamp corpus_end_FAILED
  say "STOPPED: prep rc=$PREP_RC chain rc=$CHAIN_RC; VIEWS pass not started"
  archive_evidence
  exit 1
fi

# ── one cross-year 5v pass over every year's merged file ──
VIEW_PATHS=""
for y in "${YEAR_LIST[@]}"; do VIEW_PATHS+="${VIEW_PATHS:+,}$OUT_ROOT/FY$y"; done
LAST_YEAR="${YEAR_LIST[${#YEAR_LIST[@]}-1]}"
stamp views_start
say "=== VIEWS (5v) over ${#YEAR_LIST[@]} years ==="
( cd "$REPO" && bash scripts/run_pipeline.sh \
    --inPath "$ROOT/VARawFiles" --outPath "$OUT_ROOT/VIEWS" --viewInPaths "$VIEW_PATHS" \
    --years "$LAST_YEAR" --steps 5v --curve CCP3 \
    --config "fullcorpus-run2-views" --periodGranularity "$GRANULARITY" \
    > "$LOGS/run2_views.log" 2>&1 )
VIEWS_RC=$?
stamp views_end
if [[ $VIEWS_RC -ne 0 ]]; then
  stamp corpus_end_FAILED
  say "VIEWS FAILED rc=$VIEWS_RC"
  archive_evidence
  exit 1
fi

stamp corpus_end
archive_evidence
say "ALL YEARS COMPLETE; wall-clock record: $WALL"
exit 0
