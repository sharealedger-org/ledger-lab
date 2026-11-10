#!/usr/bin/env bash
# =============================================================================
#  download_berka.sh -- fetch the Berka PKDD'99 Financial dataset for Track C
#
#  Downloads the original data_berka.zip (Petr Berka and Marta Sochorova, PKDD'99
#  Discovery Challenge) from the Internet Archive capture of the authors' site, verifies
#  the zip and every extracted file against pinned SHA-256 values, and writes the files
#  into an external data root as <root>/berka/<name>.asc.txt (the names that
#  scripts/berka/build_berka_substrates.py reads). Nothing is redistributed from this repo.
#
#  Source URL (host sorry.vse.cz no longer answers directly; checked 2026-11-10):
#    https://web.archive.org/web/2024id_/https://sorry.vse.cz/~berka/challenge/pkdd1999/data_berka.zip
#  Expected: 18,074,591 bytes. The extracted file hashes equal the "original, uncompressed
#  files" SHA-256 list in the members repository example banking-berka/README.md.
#
#  TERMS OF USE: the dataset carries no stated licence in the files or the saved description
#  page. This script only fetches from a public source for the user's own research use.
#  Confirm the authors' terms before redistributing the data or derived row-level files.
#
#  This is file movement and checksum verification only. It contains no financial logic.
#
#  USAGE
#    bash scripts/download_berka.sh --data-root DIR [--dry-run]
#
#    --data-root DIR   target root (default: $BERKA_DATA_ROOT; no default into the repo)
#    --dry-run         print the plan; download nothing
#
#  Idempotent: if all eight files are already present with the pinned SHA-256, nothing is
#  downloaded. A zip or file hash mismatch deletes the bad file and fails.
#  Appends to <root>/logs/download_berka_manifest.csv (UTC time, name, bytes, sha256, status).
#
#  Exit code: 0 all files verified; 1 any failure; 2 usage error.
# =============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ZIP_URL="https://web.archive.org/web/2024id_/https://sorry.vse.cz/~berka/challenge/pkdd1999/data_berka.zip"
ZIP_BYTES=18074591
ZIP_SHA="affa4477572e9d8f2de6beff8a525d37c056d5d8e4a9670dd8c7636208c002ce"

# name|sha256 of the extracted original file (<name>.asc inside the zip)
FILES="account|58d7f50abd72e9b1a5568346f74bb54cd71224ee1db9f09a27d7cac563f38cc6
card|fc669bde6adf6457d87421c0bfb218e9c384a7032c6accd348d207a405e72109
client|e435c6b92d246f4f0dfd5e2827469d745c06238714c32b3ffb415eebe794e1a7
disp|ebd801f77b6d322e8ebc08e52f188e7c8fca539325f85f57f8c73434da9d32d8
district|7f03cf3b9b82f0fdcc3abdf6cc716f145db8e9875c68e2d2e2f7151e9ecf4df3
loan|68535f609a254aa7a3f03dd8e27dcb822b532df12a0d6046f0666b8dc0b8ae8e
order|035930fa6acd2ca42a935e654b21e1bb260248f49b6dc6e7de6351b7c4d56d02
trans|75ab2f39df9d79d79c5c900de90ddd28248b689f214598ac9fa2ff0f574a70d2"

DATA_ROOT="${BERKA_DATA_ROOT:-}"
DRY_RUN=0

usage() { sed -n '2,/^# =====*$/p' "$0" | sed -n '1,31p' | sed 's/^#  \{0,1\}//'; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --data-root) DATA_ROOT="${2:-}"; shift 2 ;;
    --dry-run)   DRY_RUN=1; shift ;;
    -h|--help)   usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

[[ -n "$DATA_ROOT" ]] || { echo "ERROR: --data-root (or BERKA_DATA_ROOT) is required; there is no default into the repo." >&2; exit 2; }
command -v curl  >/dev/null || { echo "ERROR: curl is required" >&2; exit 2; }
command -v unzip >/dev/null || { echo "ERROR: unzip is required" >&2; exit 2; }

if command -v shasum >/dev/null; then SHA256() { shasum -a 256 "$1" | cut -c1-64; }
elif command -v sha256sum >/dev/null; then SHA256() { sha256sum "$1" | cut -c1-64; }
else echo "ERROR: need shasum or sha256sum" >&2; exit 2; fi

mkdir -p "$DATA_ROOT"
DATA_ROOT="$(cd "$DATA_ROOT" && pwd)"
case "$DATA_ROOT" in
  "$REPO_ROOT"|"$REPO_ROOT"/*) echo "ERROR: data root must be outside the repository: $DATA_ROOT" >&2; exit 2 ;;
esac

OUT="$DATA_ROOT/berka"
LOGS="$DATA_ROOT/logs"
MANIFEST="$LOGS/download_berka_manifest.csv"

printf 'Data root : %s\nOutput dir: %s\nSource    : %s\nZip       : %d bytes, sha256 %s\n' \
  "$DATA_ROOT" "$OUT" "$ZIP_URL" "$ZIP_BYTES" "$ZIP_SHA"
if [[ $DRY_RUN -eq 1 ]]; then echo "Dry run: nothing downloaded."; exit 0; fi

mkdir -p "$OUT" "$LOGS"
[[ -f "$MANIFEST" ]] || echo "utc_time,name,bytes,sha256,status" > "$MANIFEST"
log_row() { echo "$(date -u +%Y-%m-%dT%H:%M:%SZ),$1,$2,$3,$4" >> "$MANIFEST"; }

all_present() {
  local name sha dest
  while IFS='|' read -r name sha; do
    dest="$OUT/$name.asc.txt"
    [[ -f "$dest" && "$(SHA256 "$dest")" == "$sha" ]] || return 1
  done <<< "$FILES"
}

if all_present; then
  log_row "all_8_files" "-" "-" "already_verified"
  echo "Done: all 8 files already present and verified (no download)."
  exit 0
fi

TMP="$(mktemp -d "$DATA_ROOT/.berka_tmp.XXXXXX")"; trap 'rm -rf "$TMP"' EXIT
ZIP="$TMP/data_berka.zip"

got=0
for attempt in 1 2 3 4 5; do
  if curl -fL --silent --show-error --retry 3 --retry-delay 5 --max-time 600 -o "$ZIP" "$ZIP_URL"; then got=1; break; fi
  echo "attempt $attempt failed; retrying" >&2; sleep $((attempt*5))
done
if [[ $got -ne 1 ]]; then
  echo "FAILED download: $ZIP_URL" >&2; log_row "data_berka.zip" "-" "$ZIP_SHA" "download_failed"; exit 1
fi

actual_bytes="$(wc -c < "$ZIP" | tr -d ' ')"
actual_sha="$(SHA256 "$ZIP")"
if [[ "$actual_bytes" != "$ZIP_BYTES" || "$actual_sha" != "$ZIP_SHA" ]]; then
  echo "FAILED zip check: expected $ZIP_BYTES bytes / $ZIP_SHA, got $actual_bytes bytes / $actual_sha" >&2
  log_row "data_berka.zip" "$actual_bytes" "$actual_sha" "zip_mismatch"; exit 1
fi
log_row "data_berka.zip" "$actual_bytes" "$actual_sha" "downloaded_verified"

unzip -o -q "$ZIP" -d "$TMP/x"

failed=0
while IFS='|' read -r name sha; do
  src="$TMP/x/$name.asc"; dest="$OUT/$name.asc.txt"
  if [[ ! -f "$src" ]]; then
    echo "FAILED: $name.asc missing from zip" >&2; log_row "$name" "-" "$sha" "missing_in_zip"; failed=$((failed+1)); continue
  fi
  actual="$(SHA256 "$src")"
  if [[ "$actual" != "$sha" ]]; then
    echo "FAILED sha256 for $name: expected $sha got $actual" >&2
    log_row "$name" "$(wc -c < "$src" | tr -d ' ')" "$actual" "sha256_mismatch"; failed=$((failed+1)); continue
  fi
  mv "$src" "$dest"
  log_row "$name" "$(wc -c < "$dest" | tr -d ' ')" "$actual" "extracted_verified"
done <<< "$FILES"

echo "Done: failed=$failed (manifest: $MANIFEST)"
[[ $failed -eq 0 ]]
