#!/usr/bin/env bash
# Passive process/storage samples, outside every engine timing.
set -euo pipefail
root="${1:?absolute run root}"; pid="${2:?driver PID}"; interval="${3:-60}"; minimum="${4:-0}"
[[ "$root" = /* && -d "$root" && "$pid" =~ ^[0-9]+$ && "$interval" =~ ^[0-9]+$ && "$minimum" =~ ^[0-9]+$ ]] || exit 2
[[ -s "$root/monitor_storage.csv" ]] || printf 'utc,free_kib,root_allocated_kib\n' > "$root/monitor_storage.csv"
while kill -0 "$pid" 2>/dev/null; do
  free="$(df -Pk "$root" | awk 'END {print $4}')"
  # Engine spill cleanup can remove a directory while du traverses it. Keep the
  # independent free-space stop alive; leave this allocation sample unknown.
  allocated="$(du -sk "$root" 2>> "$root/monitor_du_warnings.log" | awk 'END {print $1}')" || allocated=""
  printf '%s,%s,%s\n' "$(date -u +%FT%TZ)" "$free" "$allocated" >> "$root/monitor_storage.csv"
  if ((minimum > 0 && free < minimum)); then
    printf 'free_kib=%s minimum_kib=%s utc=%s\n' "$free" "$minimum" "$(date -u +%FT%TZ)" > "$root/LOW_SCRATCH_STOP"
    group="$(ps -p "$pid" -o pgid= | tr -d ' ')"
    [[ "$group" == "$pid" ]] && kill -TERM -- "-$pid"
    exit 3
  fi
  {
    date -u +%FT%TZ
    ps -axo pid,ppid,rss,etime,command | awk '/[j]ava|[c]affeinate|[r]un_synthetic_reference_corpus_v2|[p]reflight.sh/ {print}'
  } >> "$root/monitor_processes.log"
  sleep "$interval"
done
date -u +%FT%TZ > "$root/MONITOR_STOPPED"
