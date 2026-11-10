#!/usr/bin/env bash
# Open Terminal.app windows for watching a full-corpus run (macOS only; Bash job control, no engine work).
#   Window 1: per-year dashboard      (scripts/dashboard_full_corpus.sh)
#   Window 2: events only             (RESULT / RETRY / failures from every run2_*.log)
# Usage: bash scripts/open_run_windows.sh [dashboard-refresh-seconds]   (default 30)
# The first use may prompt macOS to allow your terminal/Bob to control Terminal (System Settings >
# Privacy & Security > Automation). Closing the windows does not affect the run.
set -u
[[ "$(uname -s)" == "Darwin" ]] || { echo "macOS only; run the commands in docs/FULL_CORPUS_RUN_CHECKLIST.md section 4.1 by hand." >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO="$(cd "$SCRIPT_DIR/.." && pwd)"
LOGS="$HOME/workspace/va_fresh_logs"
N="${1:-30}"

DASH="cd '$REPO' && bash scripts/dashboard_full_corpus.sh $N"
# tail -F waits for files that do not exist yet, so name every log a run can create (years 03-16);
# a shell glob would be expanded once and miss the chain and views logs that appear later.
NAMES="run2_corpus.log run2_views.log"
for y in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do NAMES="$NAMES run2_prep_fy$y.log run2_chain_fy$y.log"; done
EVENTS="cd '$LOGS' && tail -n 0 -F $NAMES 2>/dev/null | grep --line-buffered -E 'RESULT step=|RETRY|exit [1-9]|STOPPED|ALL YEARS|ERROR|Exception'"

osascript <<EOF
tell application "Terminal"
  activate
  do script "$DASH"
  do script "$EVENTS"
end tell
EOF
