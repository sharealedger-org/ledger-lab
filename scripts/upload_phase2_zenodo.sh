#!/usr/bin/env bash
# Existing-draft file uploads only. No create, publish or delete operation.
set -euo pipefail
mode=check; manifest=; base=; state=; jq_bin=${JQ:-jq}; curl_bin=${CURL:-curl}
usage() { echo 'upload_phase2_zenodo.sh --manifest FILE --base-dir DIR [--mode check|upload] [--state-dir DIR]'; }
while [[ $# -gt 0 ]]; do
  [[ "$1" != --help ]] || { usage; exit 0; }
  [[ $# -ge 2 ]] || exit 2
  case "$1" in --manifest) manifest=$2;; --base-dir) base=$2;; --mode) mode=$2;; --state-dir) state=$2;; *) exit 2;; esac
  shift 2
done
fail() { printf 'Zenodo preparation: %s\n' "$*" >&2; exit 1; }
[[ "$mode" == check || "$mode" == upload ]] || fail 'mode must be check or upload'
[[ -f "$manifest" && -d "$base" ]] || fail 'manifest and base directory required'
command -v "$jq_bin" >/dev/null || fail 'jq required; set JQ to its executable path'
base=$(cd "$base" && pwd -P)
"$jq_bin" -e '
  .release_version as $v | .records as $r | .artifacts as $a |
  ($v|type=="string" and test("^[A-Za-z0-9][A-Za-z0-9._-]*$")) and ($r|type=="array" and length>0) and
  ($a|type=="array" and length>0) and
  (all($r[]; (.key|test("^[A-Za-z0-9_-]+$")) and
    (.deposition_id==null or (.deposition_id|type=="number" and .>0 and floor==.)))) and
  (($r|map(.key)|unique|length)==($r|length)) and
  (all($a[]; . as $f | ($r|map(.key)|index($f.record))!=null and
    (.name|test("^[A-Za-z0-9][A-Za-z0-9._-]*$")) and
    (.path|type=="string" and length>0 and (startswith("/")|not) and
      (test("[\t\r\n]")|not) and (split("/")|index(".."))==null) and
    (.bytes|type=="number" and .>=0 and .<=50000000000 and floor==.) and
    (.sha256|test("^[0-9a-f]{64}$")))) and
  (($a|map([.record,.name])|unique|length)==($a|length)) and
  (all($r[]; .key as $k | [$a[]|select(.record==$k)] as $files |
    ($files|length)>0 and ($files|length)<=100 and
    ([$files[].bytes]|add)<=50000000000))' "$manifest" >/dev/null || fail 'invalid version/artifact/record manifest or record budget'
version=$("$jq_bin" -r .release_version "$manifest")
manifest_sha=$(LC_ALL=C shasum -a 256 "$manifest"); manifest_sha=${manifest_sha%% *}
file_bytes() { if [[ $(uname -s) == Darwin ]]; then stat -f %z "$1"; else stat -c %s "$1"; fi; }
while IFS=$'\t' read -r path bytes sha; do
  file="$base/$path"
  [[ -f "$file" && $(file_bytes "$file") == "$bytes" ]] || fail "missing or size-changed artifact: $path"
  actual=$(LC_ALL=C shasum -a 256 "$file"); actual=${actual%% *}
  [[ "$actual" == "$sha" ]] || fail "SHA256 mismatch: $path"
done < <("$jq_bin" -r '.artifacts[]|[.path,.bytes,.sha256]|@tsv' "$manifest")
[[ "$mode" != check ]] || { printf 'PASS local release %s: files, SHA256 and record budgets verified; no network.\n' "$version"; exit 0; }
[[ -n "$state" && -n "${ZENODO_TOKEN:-}" ]] || fail 'upload requires state-dir and environment ZENODO_TOKEN'
[[ "$ZENODO_TOKEN" != *$'\n'* && "$ZENODO_TOKEN" != *$'\r'* && "$ZENODO_TOKEN" != *'"'* && "$ZENODO_TOKEN" != *'\'* ]] || fail 'invalid token encoding'
endpoint=${ZENODO_BASE_URL:-https://zenodo.org}
case "$endpoint" in https://zenodo.org|https://sandbox.zenodo.org) ;; *) fail 'unsupported API host';; esac
"$jq_bin" -e 'all(.records[]; .deposition_id!=null)' "$manifest" >/dev/null || fail 'supply existing draft IDs for every record'
umask 077
mkdir -p "$state"
mkdir "$state/lock" 2>/dev/null || fail 'state directory is already in use'
auth=$(mktemp "${TMPDIR:-/tmp}/ledger-zenodo-auth.XXXXXX")
cleanup() { rm -f "$auth"; rmdir "$state/lock" 2>/dev/null || true; }
trap cleanup EXIT
printf 'header = "Authorization: Bearer %s"\n' "$ZENODO_TOKEN" > "$auth"
if [[ -f "$state/context.json" ]]; then
  "$jq_bin" -e --arg sha "$manifest_sha" --arg endpoint "$endpoint" '.manifest_sha256==$sha and .endpoint==$endpoint' "$state/context.json" >/dev/null || fail 'resume context differs from pinned manifest/host'
else
  "$jq_bin" -n --arg sha "$manifest_sha" --arg endpoint "$endpoint" --arg v "$version" '{manifest_sha256:$sha,endpoint:$endpoint,release_version:$v}' > "$state/context.pending"
  mv "$state/context.pending" "$state/context.json"
fi
request() { "$curl_bin" --silent --show-error --fail-with-body --config "$auth" -H 'User-Agent: LedgerLabPhase2Uploader/1.0' "$@"; }
md5_file() { if command -v md5 >/dev/null; then md5 -q "$1"; else md5sum "$1" | awk '{print $1}'; fi; }
verify_remote() {
  "$jq_bin" -e --arg name "$1" --arg md5 "$2" --argjson bytes "$3" '
    (.key//.filename)==$name and ((.size//.filesize)|tonumber)==$bytes and
    (.checksum|sub("^md5:";""))==$md5' "$4" >/dev/null
}
while IFS=$'\t' read -r record name path bytes sha; do
  id=$("$jq_bin" -r --arg k "$record" '.records[]|select(.key==$k)|.deposition_id' "$manifest")
  draft_url="$endpoint/api/deposit/depositions/$id"
  request "$draft_url" > "$state/draft.pending.json"
  "$jq_bin" -e --arg version "$version" --argjson id "$id" '.id==$id and .state=="unsubmitted" and .submitted==false and .metadata.version==$version' "$state/draft.pending.json" >/dev/null || fail "draft/version state mismatch: $record"
  bucket=$("$jq_bin" -r .links.bucket "$state/draft.pending.json")
  [[ "$bucket" == "$endpoint/api/files/"* ]] || fail 'bucket host/path mismatch'
  bucket_id=${bucket#"$endpoint/api/files/"}
  [[ "$bucket_id" =~ ^[A-Za-z0-9-]+$ ]] || fail 'invalid bucket identifier'
  md5=$(md5_file "$base/$path")
  "$jq_bin" --arg name "$name" '[.files[]?|select(.filename==$name)]' "$state/draft.pending.json" > "$state/matches.pending.json"
  matches=$("$jq_bin" length "$state/matches.pending.json")
  if [[ "$matches" == 1 ]]; then
    "$jq_bin" '.[0]' "$state/matches.pending.json" > "$state/file.pending.json"
    verify_remote "$name" "$md5" "$bytes" "$state/file.pending.json" || fail "existing remote file differs; no overwrite: $record/$name"
  elif [[ "$matches" == 0 ]]; then
    request --upload-file "$base/$path" "$bucket/$name" > "$state/file.pending.json"
    verify_remote "$name" "$md5" "$bytes" "$state/file.pending.json" || fail "uploaded checksum/size mismatch: $record/$name"
  else fail 'duplicate remote filename'; fi
  "$jq_bin" -n --arg name "$name" --arg sha "$sha" --arg md5 "$md5" --arg manifest "$manifest_sha" --argjson id "$id" --argjson bytes "$bytes" \
    '{status:"verified",name:$name,sha256:$sha,md5:$md5,bytes:$bytes,deposition_id:$id,manifest_sha256:$manifest}' > "$state/receipt.pending.json"
  mv "$state/receipt.pending.json" "$state/$record--$name.json"
  printf 'VERIFIED %s/%s\n' "$record" "$name"
done < <("$jq_bin" -r '.artifacts[]|[.record,.name,.path,.bytes,.sha256]|@tsv' "$manifest")
printf 'All declared files verified in existing drafts. Records remain unpublished.\n'
