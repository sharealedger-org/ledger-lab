#!/usr/bin/env bash
# Passive process/storage samples, outside every engine timing.
set -euo pipefail
root="${1:?absolute run root}"; pid="${2:?driver PID}"; interval="${3:-60}"
[[ "$root" = /* && -d "$root" && "$pid" =~ ^[0-9]+$ && "$interval" =~ ^[0-9]+$ ]] || exit 2
printf 'utc,free_kib,root_allocated_kib\n' > "$root/monitor_storage.csv"
while kill -0 "$pid" 2>/dev/null; do
  free="$(df -Pk "$root" | awk 'END {print $4}')"
  allocated="$(du -sk "$root" | awk '{print $1}')"
  printf '%s,%s,%s\n' "$(date -u +%FT%TZ)" "$free" "$allocated" >> "$root/monitor_storage.csv"
  {
    date -u +%FT%TZ
    ps -axo pid,ppid,rss,etime,command | awk '/[j]ava|[c]affeinate|[r]un_synthetic_reference_corpus_v2|[p]reflight.sh/ {print}'
  } >> "$root/monitor_processes.log"
  sleep "$interval"
done
date -u +%FT%TZ > "$root/MONITOR_STOPPED"
