#!/usr/bin/env bash
# Verified post-success packaging/copying only. Active runs never execute on OneDrive.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
root=""; destination=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) root="$2"; shift 2 ;;
    --destination) destination="$2"; shift 2 ;;
    *) exit 2 ;;
  esac
done
[[ -s "$root/THREE_EXECUTIONS_EVALUATED" && "$destination" = /* && ! -e "$destination" ]] || exit 2
inputs="$(sed -n 's/^inputs=//p' "$root/config.txt")"
[[ -s "$inputs/INPUTS_COMPLETE" ]] || exit 2
for name in monthly_a monthly_b monthly_c; do
  [[ -s "$root/$name/EVALUATED" ]] || exit 2
  cmp "$root/reference.sha256" "$root/$name/products.sha256" || exit 2
done
mkdir -p "$root/archives"
mkdir "$destination"
if command -v caffeinate >/dev/null; then caffeinate -i -s -w $$ & fi
printf 'archive,bytes,sha256,destination_sha256,status\n' > "$root/archive_receipt.csv"
package() {
  local name="$1" source="$2" dereference="$3" archive="$root/archives/$1.tar.gz"
  [[ ! -e "$archive" && ! -e "$archive.partial" ]] || exit 2
  local free archive_limit_kib
  free="$(df -Pk "$root" | awk 'END {print $4}')"
  ((free > 20 * 1024 * 1024)) || { echo "Archive scratch insufficient for $name" >&2; exit 2; }
  archive_limit_kib=$((free - 10 * 1024 * 1024))
  # Dereference final-run FY links so archives restore without depending on local absolute paths.
  # Prepared-input source links intentionally remain links; raw sources are separately catalogued.
  # Bash -f limits the compressed writer in KiB, retaining a 10-GiB disk reserve.
  # Compression streams directly to disk; no uncompressed tar is materialized.
  if [[ "$dereference" == yes ]]; then tar -chf - -C "$source" . | (ulimit -f "$archive_limit_kib"; exec gzip -1) > "$archive.partial"
  else tar -cf - -C "$source" . | (ulimit -f "$archive_limit_kib"; exec gzip -1) > "$archive.partial"; fi
  gzip -t "$archive.partial"
  tar -tzf "$archive.partial" > "$root/archives/$name.contents.txt"
  mv "$archive.partial" "$archive"
  local bytes digest copied
  bytes="$(stat -f %z "$archive")"
  free="$(df -Pk "$(dirname "$destination")" | awk 'END {print $4}')"
  ((free * 1024 > bytes + 10 * 1024 * 1024)) || { echo "Destination capacity insufficient" >&2; exit 2; }
  digest="$(LC_ALL=C shasum -a 256 "$archive")"; digest="${digest%% *}"
  cp "$archive" "$destination/$name.tar.gz.partial"
  copied="$(LC_ALL=C shasum -a 256 "$destination/$name.tar.gz.partial")"; copied="${copied%% *}"
  [[ "$digest" == "$copied" ]] || exit 1
  mv "$destination/$name.tar.gz.partial" "$destination/$name.tar.gz"
  printf '%s,%s,%s,%s,PASS\n' "$name.tar.gz" "$bytes" "$digest" "$copied" >> "$root/archive_receipt.csv"
}
package prepared_inputs "$inputs" no
for name in monthly_a monthly_b monthly_c; do package "$name" "$root/$name" yes; done
revision="$(sed -n 's/^revision=//p' "$root/config.txt")"
[[ "$revision" =~ ^[a-f0-9]{40}$ ]] || exit 2
git -C "$repo" archive --format=tar "$revision" | gzip -1 > "$root/archives/source_revision.tar.gz"
gzip -t "$root/archives/source_revision.tar.gz"
cp "$root/archives/source_revision.tar.gz" "$destination/source_revision.tar.gz.partial"
source_digest="$(LC_ALL=C shasum -a 256 "$root/archives/source_revision.tar.gz")"; source_digest="${source_digest%% *}"
copied_digest="$(LC_ALL=C shasum -a 256 "$destination/source_revision.tar.gz.partial")"; copied_digest="${copied_digest%% *}"
[[ "$source_digest" == "$copied_digest" ]] || exit 1
mv "$destination/source_revision.tar.gz.partial" "$destination/source_revision.tar.gz"
printf 'source_revision.tar.gz,%s,%s,%s,PASS\n' "$(stat -f %z "$root/archives/source_revision.tar.gz")" "$source_digest" "$copied_digest" >> "$root/archive_receipt.csv"
cp "$root/archive_receipt.csv" "$destination/"
cp "$root/config.txt" "$root/status.csv" "$root/reference.sha256" "$destination/"
cat > "$destination/README.txt" <<'EOF'
Synthetic-reference corpus v2: three evaluated executions, including expanded views.
Each run archive contains its controls, financial outputs, reporting outputs,
preparation manifests, timing/memory logs and post-evaluation storage receipts.
Run FY symlinks were dereferenced; prepared raw-source links are provenance pointers.
Original raw data is the separately checksummed VA corpus, not synthesized facts.
archive_receipt.csv verifies local archive bytes against the OneDrive-folder copies.
This receipt alone does not certify completion of the provider's remote upload.
EOF
date -u +%FT%TZ > "$root/ONEDRIVE_LOCAL_COPY_VERIFIED"
cp "$root/ONEDRIVE_LOCAL_COPY_VERIFIED" "$destination/"
# File Provider exposes OneDrive's upload completion separately from local copy integrity.
if command -v fileproviderctl >/dev/null; then
  mkdir -p "$root/archives/upload_status"
  while :; do
    pending=0; failed=0
    while IFS= read -r -d '' file; do
      name="${file##*/}"
      fileproviderctl attributes "$file" > "$root/archives/upload_status/$name.txt" 2>&1 || { pending=1; continue; }
      grep -q '^NSURLUbiquitousItemIsUploadedKey: 1$' "$root/archives/upload_status/$name.txt" || pending=1
      if grep '^NSURLUbiquitousItemUploadingErrorKey:' "$root/archives/upload_status/$name.txt" | grep -qv ': nil$'; then failed=1; fi
    done < <(find "$destination" -maxdepth 1 -type f -print0)
    ((failed == 0)) || { echo "OneDrive reports an upload error; see archives/upload_status" >&2; exit 3; }
    ((pending)) || break
    printf '%s Waiting for OneDrive upload confirmation\n' "$(date -u +%FT%TZ)"
    sleep 60
  done
  date -u +%FT%TZ > "$root/ONEDRIVE_UPLOAD_CONFIRMED"
fi
