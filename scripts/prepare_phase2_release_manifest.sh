#!/usr/bin/env bash
# Package metadata only: use validated source hashes and verified gzip receipts.
set -euo pipefail
plan=; base=; receipt=; out=; jq_bin=${JQ:-jq}
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || exit 2
  case "$1" in --plan) plan=$2;; --base-dir) base=$2;; --archive-receipt) receipt=$2;; --out) out=$2;; *) exit 2;; esac
  shift 2
done
fail() { echo "Release manifest: $*" >&2; exit 1; }
[[ -f "$plan" && -d "$base" && -n "$out" && ! -e "$out" && ! -e "$out.pending" ]] || fail 'plan/base/new output required'
base=$(cd "$base" && pwd -P)
"$jq_bin" -e '.release_version|test("^[A-Za-z0-9][A-Za-z0-9._-]*$")' "$plan" >/dev/null
"$jq_bin" -e '.sources|length>0 and all(.[]; (.path|startswith("/")|not) and (.path|split("/")|index(".."))==null and (.path|test("[\t\r\n]")|not) and (.original_bytes|type=="number" and .>=0 and floor==.) and (.original_sha256|test("^[0-9a-f]{64}$")))' "$plan" >/dev/null
version=$("$jq_bin" -r .release_version "$plan")
temporary=$(mktemp -d "${TMPDIR:-/tmp}/ledger-release-manifest.XXXXXX")
cleanup() { rm -f "$temporary/artifacts.jsonl" "$temporary/records.jsonl"; rmdir "$temporary"; }
trap cleanup EXIT
: > "$temporary/artifacts.jsonl"; : > "$temporary/records.jsonl"
part=1; part_bytes=0; part_files=0
printf -v record 'part%03d' "$part"
"$jq_bin" -n --arg key "$record" '{key:$key,deposition_id:null}' >> "$temporary/records.jsonl"
while IFS=$'\t' read -r relative original_bytes original_sha; do
  physical=$relative; encoding=plain; sha=$original_sha; bytes=$original_bytes
  if [[ -f "$base/$relative.gz" ]]; then
    [[ -f "$receipt" ]] || fail "gzip requires verified archive receipt: $relative"
    physical="$relative.gz"; encoding=gzip
    real_archive="$(cd "$(dirname "$base/$physical")" && pwd -P)/$(basename "$physical")"
    read -r bytes sha <<< "$(awk -F '\t' -v file="$real_archive" -v original="$original_sha" '$4==file && $3==original && $10=="VERIFIED"{b=$5;h=$6;ok=1}END{if(!ok)exit 1;print b,h}' "$receipt")" || fail "unverified gzip: $relative"
    [[ "$bytes" =~ ^[0-9]+$ && "$sha" =~ ^[0-9a-f]{64}$ ]] || fail "bad gzip receipt: $relative"
  fi
  [[ -f "$base/$physical" && $(stat -f %z "$base/$physical") == "$bytes" ]] || fail "missing/size-changed source: $physical"
  ((bytes<=45000000000)) || fail "archive oversized source before preparation: $relative"
  if ((part_files>=95 || part_bytes+bytes>45000000000)); then
    part=$((part+1)); part_bytes=0; part_files=0
    printf -v record 'part%03d' "$part"
    "$jq_bin" -n --arg key "$record" '{key:$key,deposition_id:null}' >> "$temporary/records.jsonl"
  fi
  name=${physical//\//-}
  "$jq_bin" -n --arg record "$record" --arg name "$name" --arg physical "$physical" --arg sha "$sha" --arg original "$relative" --arg original_sha "$original_sha" --arg encoding "$encoding" --argjson bytes "$bytes" --argjson original_bytes "$original_bytes" \
    '{record:$record,name:$name,path:$physical,bytes:$bytes,sha256:$sha,encoding:$encoding,original_path:$original,original_bytes:$original_bytes,original_sha256:$original_sha}' >> "$temporary/artifacts.jsonl"
  part_files=$((part_files+1)); part_bytes=$((part_bytes+bytes))
done < <("$jq_bin" -r '.sources[]|[.path,.original_bytes,.original_sha256]|@tsv' "$plan")
"$jq_bin" -n --arg version "$version" --slurpfile records "$temporary/records.jsonl" --slurpfile artifacts "$temporary/artifacts.jsonl" \
  '{release_version:$version,status:"payload_manifest_prepared_not_uploaded",records:$records,artifacts:$artifacts}' > "$out.pending"
mv "$out.pending" "$out"
printf 'Prepared %s existing-draft groups; no network or publication.\n' "$part"
