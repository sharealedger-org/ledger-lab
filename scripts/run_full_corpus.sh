#!/usr/bin/env bash
# =============================================================================
#  run_full_corpus.sh — full-corpus VA run orchestration (Bash: job control only)
#
#  Topology (all financial work happens in compiled Scala steps invoked by run_pipeline.sh):
#
#    PREP lanes (parallel, any number)   per year:  ttl,2p,2m      -> $OUT_ROOT/FY<yy>
#    CHAIN     (pool of --chainWidth)    per year:  3,20,21        -> continues $OUT_ROOT/FY<yy>
#                                        years start in order; each waits for its own prep_<yy>.ok
#    VIEWS     (one pass)                5v over every year's merged file, k-way merged by engine key
#                                        -> $OUT_ROOT/VIEWS   (cross-year view; see CKBViews.scala)
#                                        starts when EVERY year's prep has committed, overlapping posting
#
#  Run-2 orchestration changes against the 2026-11-10 21:07 baseline (serial chain, 5v after chain):
#    1. chain is a pool (default width 2) instead of one serial loop;
#    2. 5v starts when all prep_<yy>.ok exist instead of after the chain;
#    3. caffeinate -i -s -w <this pid> keeps the Mac awake for the run.
#  5v reads only SortedJE* files, opens all of them once at start, and those files are hard links
#  with one inode in the prep and posting generations (checked for all 14 years), so the FY<yy>
#  symlink swap at posting commit cannot change what an open 5v reads.
#
#  Timing: per-year step rows stay in each year's cost_surface.csv (written by run_pipeline.sh).
#  Corpus wall-clock is a separate record, $LOGS/corpus_wallclock_<stamp>.csv, from start/end stamps
#  taken around the whole launch (plus phase stamps). With overlapping lanes and an overlapping 5v the
#  per-step elapsed_s values are NOT comparable with serial baselines; use the RESOURCE CPU seconds
#  and the wall-clock record.
#
#  Stop rule: the first failing step writes a *.fail marker; running steps finish, no new chain year
#  or 5v is started after that. A year whose prep failed is never chained. If 5v is already running
#  when a chain year fails, it finishes and the run is reported as failed.
#
#  USAGE:
#    bash scripts/run_full_corpus.sh [--root DIR] [--logs DIR] [--years 03,04,...,16] [--lanes N]
#                                    [--chainWidth N] [--periodGranularity monthly|annual]
#    Defaults: root=$HOME/workspace/va_fresh_root  logs=$HOME/workspace/va_fresh_logs
#              years=03..16  lanes=4  chainWidth=2  periodGranularity=monthly
#    The output root ($ROOT/output_full) must hold no FY* entries; move or delete the old output first.
#
#  Nothing here selects or computes financial data. Do not edit Scala while this runs (sbt would
#  recompile under the lanes).
# =============================================================================
set -u

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/.." && pwd)"

# ── worker entry points (re-invocations of this script by xargs) ──
if [[ "${1:-}" == "--worker" ]]; then
  kind="$2"; y="$3"
  say() { echo "[$(date '+%F %T')] $*"; }
  stamp() { printf '%s,%s,%s\n' "$1" "$(date +%s)" "$(date '+%F %T')" >> "$WALL"; }
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
    chain)
      while [[ ! -f "$MARKERS/prep_$y.ok" ]]; do
        if compgen -G "$MARKERS/*.fail" >/dev/null; then say "chain FY20$y: a lane failed; not started"; exit 255; fi
        sleep 10
      done
      if compgen -G "$MARKERS/*.fail" >/dev/null; then say "chain FY20$y: a lane failed; not started"; exit 255; fi
      stamp "chain_start_fy$y"
      say "=== FY20$y chain start (3,20,21) ==="
      bash scripts/run_pipeline.sh \
        --inPath "$ROOT/VARawFiles" --outPath "$OUT_ROOT/FY$y" --seedFromCommitted \
        --years "$y" --steps 3,20,21 --curve CCP1 \
        --config "fullcorpus-run2-chain-fy$y" --periodGranularity "$GRANULARITY" \
        > "$LOGS/run2_chain_fy$y.log" 2>&1
      rc=$?
      stamp "chain_end_fy$y"
      say "=== FY20$y chain exit $rc ==="
      if [[ $rc -ne 0 ]]; then : > "$MARKERS/chain_$y.fail"; exit 255; fi
      exit 0
      ;;
  esac
  exit 1
fi

ROOT="$HOME/workspace/va_fresh_root"
LOGS="$HOME/workspace/va_fresh_logs"
YEARS="03,04,05,06,07,08,09,10,11,12,13,14,15,16"
LANES=4
CHAIN_WIDTH=2
GRANULARITY="monthly"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root)  ROOT="$2";  shift 2 ;;
    --logs)  LOGS="$2";  shift 2 ;;
    --years) YEARS="$2"; shift 2 ;;
    --lanes) LANES="$2"; shift 2 ;;
    --chainWidth) CHAIN_WIDTH="$2"; shift 2 ;;
    --periodGranularity) GRANULARITY="$2"; shift 2 ;;
    -h|--help) grep '^#' "$0" | sed 's/^# \{0,2\}//'; exit 0 ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

OUT_ROOT="$ROOT/output_full"
STAMP="$(date '+%Y%m%d_%H%M%S')"
MARKERS="$LOGS/markers_$STAMP"
WALL="$LOGS/corpus_wallclock_$STAMP.csv"
export REPO ROOT LOGS OUT_ROOT MARKERS WALL LANES GRANULARITY
say() { echo "[$(date '+%F %T')] $*"; }

mkdir -p "$LOGS" "$MARKERS" "$OUT_ROOT"
if compgen -G "$OUT_ROOT/FY*" >/dev/null; then
  echo "ERROR: $OUT_ROOT already holds FY* entries; move or delete them before a from-scratch run." >&2
  exit 1
fi

# The dashboard and the evidence archive read run2_*.log by fixed name. A log left from an earlier run
# would be shown as this run's result, so move any stale ones aside (not deleted) before starting.
if compgen -G "$LOGS/run2_*.log" >/dev/null; then
  STALE="$LOGS/prev_run2_before_$STAMP"
  mkdir -p "$STALE" && mv "$LOGS"/run2_*.log "$STALE"/
  echo "Moved stale run2_*.log files to $STALE"
fi

stamp() {  # event
  printf '%s,%s,%s\n' "$1" "$(date +%s)" "$(date '+%F %T')" >> "$WALL"
}
echo "event,epoch_s,local_time" > "$WALL"
stamp corpus_start
# Keep the Mac awake until this process exits (an idle sleep of ~7 s fell inside the 2026-11-10 baseline).
CAFFEINATE=off
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & CAFFEINATE=on; fi
{
  echo "# years=$YEARS lanes=$LANES periodGranularity=$GRANULARITY"
  echo "# chain_width=$CHAIN_WIDTH views_early=Y caffeinate=$CAFFEINATE"
  echo "# git=$(cd "$REPO" && git rev-parse --short HEAD 2>/dev/null) dirty_files=$(cd "$REPO" && git status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
  echo "# host=$(uname -n) cpus=$(sysctl -n hw.ncpu 2>/dev/null || nproc)"
} >> "$WALL"

IFS=',' read -ra YEAR_LIST <<< "$YEARS"

# ── chain pool: 3,20,21 per year, years started in order, CHAIN_WIDTH at a time ──
# Each worker waits for its own year's prep marker; a failing worker exits 255 so xargs starts no more.
chain_pool() {
  printf '%s\n' "${YEAR_LIST[@]}" | xargs -n1 -P"$CHAIN_WIDTH" -I{} bash "$SCRIPT_DIR/run_full_corpus.sh" --worker chain {}
}

# ── cross-year 5v: starts when every year's prep has committed (reads SortedJE* only, see header) ──
views_pass() {
  local y
  for y in "${YEAR_LIST[@]}"; do
    while [[ ! -f "$MARKERS/prep_$y.ok" ]]; do
      if compgen -G "$MARKERS/*.fail" >/dev/null; then say "views: a lane failed; 5v not started"; return 255; fi
      sleep 10
    done
  done
  if compgen -G "$MARKERS/*.fail" >/dev/null; then say "views: a lane failed; 5v not started"; return 255; fi
  local view_paths="" last_year="${YEAR_LIST[${#YEAR_LIST[@]}-1]}" rc
  for y in "${YEAR_LIST[@]}"; do view_paths+="${view_paths:+,}$OUT_ROOT/FY$y"; done
  stamp views_start
  say "=== VIEWS (5v) over ${#YEAR_LIST[@]} years (all prep committed; posting may still be running) ==="
  ( cd "$REPO" && bash scripts/run_pipeline.sh \
      --inPath "$ROOT/VARawFiles" --outPath "$OUT_ROOT/VIEWS" --viewInPaths "$view_paths" \
      --years "$last_year" --steps 5v --curve CCP3 \
      --config "fullcorpus-run2-views" --periodGranularity "$GRANULARITY" \
      > "$LOGS/run2_views.log" 2>&1 )
  rc=$?
  stamp views_end
  say "=== VIEWS (5v) exit $rc ==="
  [[ $rc -eq 0 ]] || : > "$MARKERS/views.fail"
  return $rc
}

say "corpus start: years=$YEARS lanes=$LANES chainWidth=$CHAIN_WIDTH  wall-clock record: $WALL"
chain_pool &
CHAIN_PID=$!
views_pass &
VIEWS_PID=$!

printf '%s\n' "${YEAR_LIST[@]}" | xargs -n1 -P"$LANES" -I{} bash "$SCRIPT_DIR/run_full_corpus.sh" --worker prep {}
PREP_RC=$?
stamp prep_all_done
say "prep lanes finished rc=$PREP_RC"

wait "$CHAIN_PID"
CHAIN_RC=$?
stamp chain_all_done
say "chain finished rc=$CHAIN_RC"

wait "$VIEWS_PID"
VIEWS_RC=$?
stamp views_all_done
say "views finished rc=$VIEWS_RC"

# ── Archive evidence to repository on exit ──
archive_evidence() {
  say "Archiving run evidence into repository..."
  bash "$SCRIPT_DIR/archive_corpus_run_evidence.sh" \
    --root "$OUT_ROOT" \
    --logs "$LOGS" \
    --tag "$STAMP" || say "WARNING: Evidence archiving encountered an error"
}

if [[ $PREP_RC -ne 0 || $CHAIN_RC -ne 0 || $VIEWS_RC -ne 0 ]]; then
  stamp corpus_end_FAILED
  say "STOPPED: prep rc=$PREP_RC chain rc=$CHAIN_RC views rc=$VIEWS_RC"
  archive_evidence
  exit 1
fi

stamp corpus_end
archive_evidence
say "ALL YEARS COMPLETE; wall-clock record: $WALL"
exit 0
