#!/usr/bin/env bash
# Owner-approved exact13-file lossless archival; no financial processing.
set -euo pipefail
repo=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
mode=${1:-check}; receipt=${2:-"$repo/session-run.tmp/phase2_baseline_archive_v1"}
proposal="$repo/data/test_evidence/phase2_derived_baseline_20261116/rerun_archive_proposal/files.tsv"
approved=7e25d04872c3a97597c0a616e7fe5d6697418e324e062e554625823c83aad9f2
digest() { local value; value=$(LC_ALL=C shasum -a 256 "$1") || return 1; printf '%s\n' "${value%% *}"; }
archive_one() {
  local source=$1 expected_bytes=$2 expected_sha=$3 archive=$4 before modified permissions links original decoded packed free available
  if [[ ! -e "$source" ]]; then
    [[ -f "$archive" ]] || return 1
    packed=$(digest "$archive") || return 1
    awk -F '\t' -v s="$source" -v h="$expected_sha" -v a="$archive" -v p="$packed" '$1==s && $3==h && $4==a && $6==p && $10=="VERIFIED"{ok=1}END{exit !ok}' "$receipt/manifest.tsv" || return 1
    return 0
  fi
  [[ -f "$source" && ! -L "$source" && ! -e "$archive.pending" ]] || return 1
  [[ $(stat -f %z "$source") == "$expected_bytes" ]] || return 1
  before=$(stat -f '%z:%m:%i' "$source") || return 1
  modified=$(stat -f %m "$source") || return 1
  permissions=$(stat -f %Lp "$source") || return 1
  links=$(stat -f '%d:%i:%l' "$source") || return 1
  original=$(digest "$source") || return 1
  [[ "$original" == "$expected_sha" ]] || return 1
  if [[ ! -e "$archive" ]]; then
    free=$(df -Pk "$repo" | awk 'NR==2{print $4}') || return 1
    available=$((free-41943040-262144)); ((available>0)) || return 1
    # Conservative file-size ceiling keeps a failed/incompressible pending copy
    # below available reserve margin. Small originals are processed first.
    if ! (ulimit -f "$((available/2))"; gzip -n -1 -c "$source") > "$archive.pending"; then
      rm -f "$archive.pending"; return 1
    fi
    decoded=$(gzip -dc "$archive.pending" | LC_ALL=C shasum -a 256) || return 1
    decoded=${decoded%% *}
    [[ "$decoded" == "$expected_sha" && $(stat -f '%z:%m:%i' "$source") == "$before" ]] || return 1
    touch -r "$source" "$archive.pending" || return 1
    chmod "$permissions" "$archive.pending" || return 1
    ln "$archive.pending" "$archive" || return 1
    rm "$archive.pending" || return 1
  else
    [[ -f "$archive" && ! -L "$archive" ]] || return 1
    decoded=$(gzip -dc "$archive" | LC_ALL=C shasum -a 256) || return 1
    decoded=${decoded%% *}
    [[ "$decoded" == "$expected_sha" && $(stat -f '%z:%m:%i' "$source") == "$before" ]] || return 1
  fi
  packed=$(digest "$archive") || return 1
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\tVERIFIED\n' "$source" "$expected_bytes" "$expected_sha" "$archive" "$(stat -f %z "$archive")" "$packed" "$modified" "$permissions" "$links" >> "$receipt/manifest.tsv" || return 1
  sync || return 1
  [[ $(stat -f '%z:%m:%i' "$source") == "$before" ]] || return 1
  rm "$source" || return 1
  printf '%s\t%s\tARCHIVED\n' "$(date -u +%FT%TZ)" "$source" >> "$receipt/events.tsv" || return 1
  printf 'ARCHIVED %s\n' "$source"
}
if [[ "$mode" == selftest ]]; then
  receipt=$(mktemp -d "$repo/session-run.tmp/baseline_archive_test.XXXXXX")
  printf 'source\tbytes\tsha256\tarchive\tarchive_bytes\tarchive_sha256\tmtime\tmode\tlinks\tstatus\n' > "$receipt/manifest.tsv"
  printf 'fixture bytes\n' > "$receipt/payload.csv"
  expected=$(digest "$receipt/payload.csv")
  archive_one "$receipt/payload.csv" "$(stat -f %z "$receipt/payload.csv")" "$expected" "$receipt/payload.csv.gz"
  [[ ! -e "$receipt/payload.csv" ]]
  archive_one "$receipt/payload.csv" 14 "$expected" "$receipt/payload.csv.gz"
  printf 'keep original\n' > "$receipt/collision.csv"; printf 'bad archive\n' > "$receipt/collision.csv.gz"
  if archive_one "$receipt/collision.csv" "$(stat -f %z "$receipt/collision.csv")" "$(digest "$receipt/collision.csv")" "$receipt/collision.csv.gz"; then exit 1; fi
  [[ -f "$receipt/collision.csv" ]]
  echo 'PASS lossless recovery, resume and corrupt-archive original retention'; exit 0
fi
[[ "$mode" == check || "$mode" == run ]]
[[ $(digest "$proposal") == "$approved" ]]
[[ $(awk 'END{print NR}' "$proposal") == 14 ]]
[[ -f "$repo/session-run.tmp/phase2_fused_reporting_v2/RESULT_REVIEW_COMPLETE" ]]
[[ "$mode" != check ]] || { echo 'PASS exact approved13-file manifest and accepted baseline'; exit 0; }
[[ "$receipt" == "$repo/session-run.tmp/"* ]]
mkdir -p "$receipt"
if [[ -f "$receipt/approved_manifest.tsv" ]]; then cmp "$proposal" "$receipt/approved_manifest.tsv"; else cp "$proposal" "$receipt/approved_manifest.tsv"; fi
[[ -f "$receipt/manifest.tsv" ]] || printf 'source\tbytes\tsha256\tarchive\tarchive_bytes\tarchive_sha256\tmtime\tmode\tlinks\tstatus\n' > "$receipt/manifest.tsv"
tail -n +2 "$proposal" | LC_ALL=C sort -t $'\t' -k2,2n > "$receipt/ordered.tsv"
cp "$0" "$receipt/executed_archiver.sh"
while IFS=$'\t' read -r source bytes sha archive; do
  archive_one "$source" "$bytes" "$sha" "$archive"
done < "$receipt/ordered.tsv"
printf 'all13 approved outputs decoded/SHA256 verified before original removal\n' > "$receipt/ARCHIVE_COMPLETE"
df -Pk "$repo" > "$receipt/storage_after.txt"
