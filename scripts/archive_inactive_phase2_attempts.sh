#!/usr/bin/env bash
# Lossless storage recovery for four explicitly failed/stopped derived attempts.
# Raw inputs, successful runs, controls and logs are never selected.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mode=check; receipt=""
while [[ $# -gt 0 ]];do
  [[ $# -ge 2 ]] || exit 2
  case "$1" in --mode) mode="$2";; --out) receipt="$2";; *) exit 2;;esac
  shift 2
done
[[ "$mode" == check || "$mode" == run || "$mode" == selftest ]]
archive_one() {
  local file="$1" archive="$1.gz" pending="$1.gz.pending" before bytes original decoded packed packed_bytes stamp permissions links
  [[ -f "$file" && ! -L "$file" && ! -e "$archive" && ! -e "$pending" ]] || return 1
  before="$(stat -f '%z:%m:%i' "$file")" || return 1
  bytes="$(stat -f %z "$file")" || return 1
  stamp="$(stat -f %m "$file")" || return 1
  permissions="$(stat -f %Lp "$file")" || return 1
  links="$(stat -f '%d:%i:%l' "$file")" || return 1
  original="$(shasum -a 256 "$file")" || return 1
  original="${original%% *}"
  gzip -n -1 -c "$file" > "$pending" || return 1
  # gzip validates CRC while decoding; pipefail also rejects truncated archives.
  decoded="$(gzip -d -c "$pending" | shasum -a 256)" || return 1
  decoded="${decoded%% *}"
  [[ "$decoded" == "$original" && "$(stat -f '%z:%m:%i' "$file")" == "$before" ]] || return 1
  touch -r "$file" "$pending" || return 1
  chmod "$permissions" "$pending" || return 1
  # Atomic no-clobber publication. The original remains until verification and
  # the durable receipt have both been written; a failed step retains its data.
  ln "$pending" "$archive" || return 1
  rm "$pending" || return 1
  packed="$(shasum -a 256 "$archive")" || return 1
  packed="${packed%% *}"
  packed_bytes="$(stat -f %z "$archive")" || return 1
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tVERIFIED\n' "$file" "$bytes" "$original" "$archive" "$packed_bytes" "$packed" "$stamp" "$permissions" "$links" >> "$receipt/manifest.tsv" || return 1
  rm "$file" || return 1
  printf '%s\t%s\tARCHIVED\n' "$(date -u +%FT%TZ)" "$file" >> "$receipt/events.tsv" || return 1
}
if [[ "$mode" == selftest ]];then
  receipt="$(mktemp -d "$repo/session-run.tmp/archive_safety_check.XXXXXX")"
  printf 'source\tbytes\tsha256\tarchive\tarchive_bytes\tarchive_sha256\tmtime\tmode\tdevice_inode_links\tstatus\n' > "$receipt/manifest.tsv"
  printf 'utc\tsource\tstatus\n' > "$receipt/events.tsv"
  printf 'opaque fixture bytes, no financial calculations\n' > "$receipt/payload.csv"
  expected="$(shasum -a 256 "$receipt/payload.csv")";expected="${expected%% *}"
  archive_one "$receipt/payload.csv"
  [[ ! -e "$receipt/payload.csv" && -f "$receipt/payload.csv.gz" ]]
  restored="$(gzip -d -c "$receipt/payload.csv.gz" | shasum -a 256)"
  [[ "${restored%% *}" == "$expected" ]]
  printf 'preserve original\n' > "$receipt/collision.csv"
  printf 'preserve existing archive\n' > "$receipt/collision.csv.gz"
  if archive_one "$receipt/collision.csv";then exit 1;fi
  [[ -f "$receipt/collision.csv" && "$(cat "$receipt/collision.csv.gz")" == 'preserve existing archive' ]]
  printf 'PASS exact recovery and existing-archive rejection; receipt=%s\n' "$receipt"
  exit 0
fi
roots=("$repo/session-run.tmp/phase2_derived_baseline_v1" "$repo/session-run.tmp/phase2_derived_baseline_v2" "$repo/session-run.tmp/phase2_fused_admission_v1" "$repo/session-run.tmp/phase2_fused_admission_v2")
for source_root in "${roots[@]}";do
  [[ "$(cd "$source_root" && pwd -P)" == "$source_root" ]]
  [[ -f "$source_root/FAILED" || -f "$source_root/STOPPED_BY_OWNER_REORG" || -f "$source_root/STOPPED_FOR_DATE_HOTPATH" ]]
done
list="$(mktemp "$repo/session-run.tmp/archive_candidates.XXXXXX")"
trap 'rm -f "$list"' EXIT
rg --files --hidden --no-ignore "${roots[@]}" | while IFS= read -r file;do
  [[ -f "$file" && ! -L "$file" ]] || continue
  case "${file##*/}" in
    SortedJE*.csv|identity_lineage.csv|source_order_input.csv|source_order_sorted.csv|financial_order_input.csv|financial_order_sorted.csv) ;;
    spill_*.tmp|merge_*.tmp) [[ "$file" == */shared_sort_scratch/* ]] || continue ;;
    *) continue;;
  esac
  [[ "$file" =~ ^/[A-Za-z0-9_./-]+$ ]]
  bytes="$(stat -f %z "$file")"
  ((bytes>=16777216)) || continue
  printf '%s\t%s\n' "$bytes" "$file"
done | sort -nr > "$list"
awk -F '\t' '{n++;b+=$1}END{printf "mode=%s candidates=%d logical_bytes=%.0f\n",mode,n,b}' mode="$mode" "$list"
[[ "$mode" == run ]] || exit 0
[[ "$receipt" == "$repo/session-run.tmp/"* && ! -e "$receipt" ]]
mkdir -p "$receipt"
cp "$list" "$receipt/selected.tsv"
cp "$0" "$receipt/executed_archiver.sh"
printf 'source\tbytes\tsha256\tarchive\tarchive_bytes\tarchive_sha256\tmtime\tmode\tdevice_inode_links\tstatus\n' > "$receipt/manifest.tsv"
printf 'utc\tsource\tstatus\n' > "$receipt/events.tsv"
: > "$receipt/open_files.txt";: > "$receipt/lsof_warnings.txt"
for source_root in "${roots[@]}";do
  if lsof -nP +D "$source_root" >> "$receipt/open_files.txt" 2>> "$receipt/lsof_warnings.txt";then
    echo 'Inactive-attempt files are open; archival refused.' >&2;exit 1
  else [[ $? == 1 ]];fi
done
[[ ! -s "$receipt/open_files.txt" && ! -s "$receipt/lsof_warnings.txt" ]]
caffeinate -i -s -w $$ &
df -Pk "$repo" > "$receipt/storage_before.txt"
while IFS=$'\t' read -r bytes file;do
  archive_one "$file"
done < "$list"
df -Pk "$repo" > "$receipt/storage_after.txt"
printf 'all_selected_archives_decoded_and_sha256_verified_before_original_unlink\n' > "$receipt/ARCHIVE_COMPLETE"
