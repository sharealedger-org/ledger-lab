#!/usr/bin/env bash
# =============================================================================
#  download_corpus_zenodo.sh -- canonical clean-start download of the Virginia corpus
#
#  Restores the full source corpus (385 files, about 27 GB) into an external data root
#  from the Zenodo records published 2026-10-31 / 2026-11-06, and verifies every file
#  against the SHA-256 in data/corpus_profile/source_catalog.csv.
#
#  This is file movement and checksum verification only. It contains no financial logic.
#  It is data preparation: do not include its time or storage in cost_surface.csv.
#
#  Record map (verified 2026-11-09 against the public Zenodo API; membership of the four
#  APA records equals the catalog split by fiscal year, with matching sizes):
#
#    23846362  eVA PO .txt (FY2001-FY2018), Budget FY20NN.csv, xlsx, dictionary   47 files
#    24015791  APA expenditure FY2003-FY2009                                      84 files
#    24021539  APA expenditure FY2010-FY2016                                      84 files
#    24022681  APA revenue     FY2003-FY2009                                      84 files
#    24022832  APA revenue     FY2010-FY2016                                      84 files
#    (24023167 eVA supplier directory snapshot: not a pipeline input; not fetched here)
#
#  Five partial duplicate records (23851758, 24014676, 24014839, 24014938, 24015422) from the
#  2026-11-06 retry failures were deleted on 2026-11-09; they now return HTTP 410 (tombstone).
#  The two zero-byte placeholder CSVs are not on Zenodo; they are recreated empty.
#
#  Files land at the catalog's rel_path under the data root:
#      <root>/VARawFiles/<per-FY folder>/Expenditures Jul 2002.csv, Budget FY2003.csv ...
#      <root>/VAPOData/VA_opendata_FY2003.txt ...
#
#  USAGE
#    bash scripts/download_corpus_zenodo.sh --data-root DIR [--types LIST] [--years SPEC]
#                                           [--dry-run] [--no-disk-check]
#
#    --data-root DIR   target root (default: $UL_DATA_ROOT; there is no default into the repo)
#    --types LIST      comma list of: exp,rev,budget,po,meta   (default: all five)
#    --years SPEC      2003 | 2003,2004 | 2003-2016  (default: all; 'meta' ignores years)
#    --dry-run         print the plan and byte totals; download nothing
#    --no-disk-check   skip the free-space check
#
#  Resumable and idempotent: a file already present with the catalog SHA-256 is skipped;
#  an interrupted download resumes from its .part file; a mismatch deletes the .part and fails.
#  Appends one row per file to <root>/logs/download_corpus_manifest.csv (Zenodo record,
#  bytes, sha256, status, UTC time). The catalog's source_url/acquisition_date stay blank on
#  purpose; this manifest is the acquisition evidence for this root.
#
#  Exit code: 0 all selected files verified; 1 any failure; 2 usage error.
#
#  Direct-from-source alternative (APA only, no Zenodo): scripts/download_va_data.py.
# =============================================================================

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
CATALOG="$REPO_ROOT/data/corpus_profile/source_catalog.csv"
ZENODO_API="https://zenodo.org/api/records"

PARENT_RECORD=23846362
EXP_EARLY=24015791
EXP_LATE=24021539
REV_EARLY=24022681
REV_LATE=24022832

DATA_ROOT="${UL_DATA_ROOT:-}"
TYPES="exp,rev,budget,po,meta"
YEARS=""
DRY_RUN=0
DISK_CHECK=1

usage() { sed -n '2,/^# =====*$/p' "$0" | sed -n '1,48p' | sed 's/^#  \{0,1\}//'; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --data-root)     DATA_ROOT="${2:-}"; shift 2 ;;
    --types)         TYPES="${2:-}"; shift 2 ;;
    --years)         YEARS="${2:-}"; shift 2 ;;
    --dry-run)       DRY_RUN=1; shift ;;
    --no-disk-check) DISK_CHECK=0; shift ;;
    -h|--help)       usage; exit 0 ;;
    *) echo "Unknown argument: $1" >&2; exit 2 ;;
  esac
done

[[ -n "$DATA_ROOT" ]] || { echo "ERROR: --data-root (or UL_DATA_ROOT) is required; there is no default into the repo." >&2; exit 2; }
[[ -f "$CATALOG" ]]   || { echo "ERROR: catalog not found: $CATALOG" >&2; exit 2; }
command -v curl >/dev/null || { echo "ERROR: curl is required" >&2; exit 2; }

if command -v shasum >/dev/null; then SHA256() { shasum -a 256 "$1" | cut -c1-64; }
elif command -v sha256sum >/dev/null; then SHA256() { sha256sum "$1" | cut -c1-64; }
else echo "ERROR: need shasum or sha256sum" >&2; exit 2; fi

mkdir -p "$DATA_ROOT"
DATA_ROOT="$(cd "$DATA_ROOT" && pwd)"
case "$DATA_ROOT" in
  "$REPO_ROOT"|"$REPO_ROOT"/*) echo "ERROR: data root must be outside the repository: $DATA_ROOT" >&2; exit 2 ;;
esac

# --- selection helpers -------------------------------------------------------
YEAR_LIST=""
if [[ -n "$YEARS" ]]; then
  if [[ "$YEARS" == *-* ]]; then
    for ((y=${YEARS%-*}; y<=${YEARS#*-}; y++)); do YEAR_LIST="$YEAR_LIST $y"; done
  else
    YEAR_LIST=" $(echo "$YEARS" | tr ',' ' ')"
  fi
  YEAR_LIST="$YEAR_LIST "
fi
TYPE_LIST=",$TYPES,"

type_of()   { case "$1" in apa_expenditure) echo exp;; apa_revenue) echo rev;; apa_budget) echo budget;;
                            eva_po) echo po;; *) echo meta;; esac; }
record_of() { # family fiscal_year
  case "$1" in
    apa_expenditure) if (( $2 <= 2009 )); then echo $EXP_EARLY; else echo $EXP_LATE; fi ;;
    apa_revenue)     if (( $2 <= 2009 )); then echo $REV_EARLY; else echo $REV_LATE; fi ;;
    *)               echo $PARENT_RECORD ;;
  esac
}

# Catalog row -> pipe-separated fields. The only quoted field (source_note) is blanked first;
# it is the sole field containing commas and no field contains an embedded double quote.
catalog_rows() {
  tr -d '\r' < "$CATALOG" | tail -n +2 | sed -E 's/"[^"]*"/Q/g' | awk -F, '{print $1"|"$2"|"$6"|"$7"|"$8}'
}

# --- plan ---------------------------------------------------------------------
PLAN="$(mktemp)"; trap 'rm -f "$PLAN"' EXIT
total_files=0; total_bytes=0
while IFS='|' read -r rel fam bytes sha fy; do
  t="$(type_of "$fam")"
  [[ "$TYPE_LIST" == *",$t,"* ]] || continue
  if [[ -n "$YEAR_LIST" && "$t" != "meta" && "$YEAR_LIST" != *" $fy "* ]]; then continue; fi
  rec="$(record_of "$fam" "${fy:-0}")"
  printf '%s|%s|%s|%s|%s\n' "$rel" "$rec" "$bytes" "$sha" "$t" >> "$PLAN"
  total_files=$((total_files+1)); total_bytes=$((total_bytes+bytes))
done < <(catalog_rows)

printf 'Data root : %s\nTypes     : %s\nYears     : %s\nSelected  : %d files, %d bytes (%.2f GB)\n' \
  "$DATA_ROOT" "$TYPES" "${YEARS:-all}" "$total_files" "$total_bytes" "$(awk -v b="$total_bytes" 'BEGIN{print b/1e9}')"
[[ $total_files -gt 0 ]] || { echo "ERROR: nothing selected" >&2; exit 2; }

if [[ $DRY_RUN -eq 1 ]]; then
  echo "Per Zenodo record:"; cut -d'|' -f2 "$PLAN" | sort | uniq -c | sed 's/^/  /'
  echo "Dry run: nothing downloaded."; exit 0
fi

if [[ $DISK_CHECK -eq 1 ]]; then
  free_kb="$(df -Pk "$DATA_ROOT" | awk 'NR==2{print $4}')"
  need_kb=$(( total_bytes / 1024 + 2*1024*1024 ))   # selected bytes + 2 GB headroom
  if (( free_kb < need_kb )); then
    echo "ERROR: need about $((need_kb/1048576)) GB free, have $((free_kb/1048576)) GB (use --no-disk-check to override)" >&2; exit 1
  fi
fi

# --- download -----------------------------------------------------------------
mkdir -p "$DATA_ROOT/logs"
MANIFEST="$DATA_ROOT/logs/download_corpus_manifest.csv"
[[ -f "$MANIFEST" ]] || echo "utc_time,rel_path,zenodo_record,bytes,sha256,status" > "$MANIFEST"
log_row() { echo "$(date -u +%Y-%m-%dT%H:%M:%SZ),$1,$2,$3,$4,$5" >> "$MANIFEST"; }

ok=0; skipped=0; failed=0; n=0
while IFS='|' read -r rel rec bytes sha t; do
  n=$((n+1)); dest="$DATA_ROOT/$rel"; mkdir -p "$(dirname "$dest")"; key="$(basename "$rel")"

  if [[ "$bytes" == "0" ]]; then                    # zero-byte placeholders are not on Zenodo
    : > "$dest"; log_row "$rel" "-" 0 "$sha" "placeholder_created"; skipped=$((skipped+1)); continue
  fi
  if [[ -f "$dest" && "$(wc -c < "$dest" | tr -d ' ')" == "$bytes" && "$(SHA256 "$dest")" == "$sha" ]]; then
    log_row "$rel" "$rec" "$bytes" "$sha" "already_verified"; skipped=$((skipped+1)); continue
  fi

  url="$ZENODO_API/$rec/files/${key// /%20}/content"
  printf '[%d/%d] %s  (record %s, %d bytes)\n' "$n" "$total_files" "$rel" "$rec" "$bytes"
  part="$dest.part"; got=0
  for attempt in 1 2 3 4 5; do
    if curl -fL --silent --show-error --retry 3 --retry-delay 5 -C - -o "$part" "$url"; then got=1; break; fi
    echo "  attempt $attempt failed; retrying" >&2; sleep $((attempt*5))
  done
  if [[ $got -ne 1 ]]; then
    echo "  FAILED download: $rel" >&2; log_row "$rel" "$rec" "$bytes" "$sha" "download_failed"; failed=$((failed+1)); continue
  fi
  actual="$(SHA256 "$part")"
  if [[ "$actual" != "$sha" ]]; then
    echo "  FAILED sha256: expected $sha got $actual" >&2; rm -f "$part"
    log_row "$rel" "$rec" "$bytes" "$sha" "sha256_mismatch"; failed=$((failed+1)); continue
  fi
  mv "$part" "$dest"; log_row "$rel" "$rec" "$bytes" "$sha" "downloaded_verified"; ok=$((ok+1))
done < "$PLAN"

echo "Done: downloaded_verified=$ok already_present_or_placeholder=$skipped failed=$failed (manifest: $MANIFEST)"
[[ $failed -eq 0 ]]
