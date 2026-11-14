#!/usr/bin/env bash
# Run the frozen queue once; inspect status every ten minutes. Bash job control only.
set -uo pipefail
export LC_ALL=C
export JAVA_HOME=/Users/ktwitchell001/.local/jdk-21
export PATH="$JAVA_HOME/bin:/Users/ktwitchell001/.local/sbt/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
repo=/Users/ktwitchell001/workspace/sharealedger/ledger-lab
queue=/Users/ktwitchell001/workspace/ckb_expanded_queue_20261113_ready
monitor=/Users/ktwitchell001/workspace/ckb_expanded_queue_20261113_ready.monitor.log
snapshot() {
  date -u '+CHECK %Y-%m-%dT%H:%M:%SZ'
  [[ ! -f "$queue/status.csv" ]] || tail -n 8 "$queue/status.csv"
  df -Pk /Users/ktwitchell001/workspace | tail -n 1
  for console in "$queue"/*/console.log; do
    [[ ! -f "$console" ]] || { printf '%s\n' "$console"; tail -n 3 "$console"; }
  done
}
exec >> "$monitor" 2>&1
printf 'Background supervisor started; check interval=600 seconds; no engine-failure retries.\n'
caffeinate -i -s -w $$ &
bash "$repo/scripts/run_corpus_queue.sh" \
  --root /Users/ktwitchell001/workspace/va_fresh_root \
  --queueRoot "$queue" \
  --plan "$repo/data/test_evidence/intel_preflight_20261113/plan.tsv" \
  --vendorMaster "$repo/data/VendorMaster.csv" \
  > /Users/ktwitchell001/workspace/ckb_expanded_queue_20261113_ready.driver.log 2>&1 &
worker=$!
printf 'queue_pid=%s supervisor_pid=%s\n' "$worker" "$$"
snapshot
ticks=0
while kill -0 "$worker" 2>/dev/null; do
  sleep 10
  ticks=$((ticks + 1))
  if (( ticks % 60 == 0 )); then snapshot; fi
done
wait "$worker"
rc=$?
snapshot
printf 'FINISHED rc=%s at %s\n' "$rc" "$(date -u +%FT%TZ)"
if (( rc != 0 )); then
  printf 'Stopped; evidence retained. No blind restart or reuse of partial financial outputs.\n'
fi
exit "$rc"
