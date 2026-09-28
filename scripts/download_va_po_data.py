#!/usr/bin/env python3
"""
download_va_po_data.py — Downloader for Virginia eVA Procurement / Purchase-Order data.

Downloads eVA procurement data from the Virginia Open Data Portal (data.virginia.gov)
and records a manifest of every attempt.

Coverage by source:
  FY2003–FY2015  Not publicly accessible via the Virginia Open Data Portal.
                 These files are already included in the repository under
                 data/VAPOData/ (2003 Combined.xlsx … 2015 Combined.xlsx and
                 VA_opendata_FY200x.txt).  This script will record a manifest
                 entry for each missing year explaining why it is skipped.

  FY2016         Available on data.virginia.gov (CKAN slug: eva-procurement-data-2016).
                 Schema matches the FY2003-FY2015 dictionary fields.

  FY2017+        Also on data.virginia.gov with an expanded schema (NIGP, address,
                 contract type).  Included so the script is useful beyond the
                 FY2003–FY2016 target range.

Usage:
  python scripts/download_va_po_data.py
  python scripts/download_va_po_data.py --dest data/VA_raw_downloads/PO --years 2016
  python scripts/download_va_po_data.py --years 2016-2018
  python scripts/download_va_po_data.py --dry-run

Output:
  <dest>/eVA_procurement_<YEAR>.csv   — downloaded data file
  <dest>/manifest.csv                 — one row per year: status, URLs, sizes
"""

import argparse
import csv
import json
import os
import ssl
import sys
import time
import urllib.parse
import urllib.request

# ---------------------------------------------------------------------------
# Configuration
# ---------------------------------------------------------------------------

CKAN_BASE = "https://data.virginia.gov"
CKAN_API  = CKAN_BASE + "/api/3/action"

# CKAN dataset slug for each year that is available on the portal.
# Years 2003-2015 are absent from the portal; they are pre-bundled in the repo.
CKAN_SLUGS = {
    2016: "eva-procurement-data-2016",
    2017: "eva-procurement-data-2017",
    2018: "eva-procurement-data-2018",
    2019: "eva-procurement-data-2019",
    2020: "eva-procurement-data-2020",
    2021: "eva-procurement-data-2021",
    2022: "eva-procurement-data-2022",
    2023: "eva-procurement-data-2023",
    2024: "eva-procurement-data-2024",
    2025: "eva-procurement-data-2025-virginia",
    2026: "eva-procurement-data-2026-virginia",
}

# Years covered by our target range that are NOT on the portal.
YEARS_BUNDLED_IN_REPO = list(range(2001, 2016))

# Default output directory
DEFAULT_DEST = os.path.join("data", "VA_raw_downloads", "PO")

# Datastore page size for paginated download
PAGE_SIZE = 50_000

# ---------------------------------------------------------------------------
# HTTP helpers
# ---------------------------------------------------------------------------

_SSL_CTX = ssl.create_default_context()
_HEADERS  = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
        "AppleWebKit/537.36 (KHTML, like Gecko) "
        "Chrome/120.0.0.0 Safari/537.36"
    ),
    "Accept": "application/json",
}


def _get_json(url: str) -> dict:
    req = urllib.request.Request(url, headers=_HEADERS)
    with urllib.request.urlopen(req, context=_SSL_CTX, timeout=30) as resp:
        return json.loads(resp.read())


def _resolve_resource(slug: str) -> tuple[str, str, str]:
    """Return (dataset_url, resource_id, resource_name) for a CKAN slug.

    Picks the first resource whose format is CSV or whose datastore is active,
    ignoring data-dictionary resources.
    """
    api_url = f"{CKAN_API}/package_show?id={slug}"
    data = _get_json(api_url)
    if not data.get("success"):
        raise ValueError(f"CKAN returned success=false for slug '{slug}'")
    pkg = data["result"]
    dataset_url = f"{CKAN_BASE}/dataset/{pkg['name']}"

    for res in pkg.get("resources", []):
        name_lower = (res.get("name") or "").lower()
        fmt_lower  = (res.get("format") or "").lower()
        # Skip data-dictionary resources
        if "dictionary" in name_lower or "dictionary" in fmt_lower:
            continue
        if res.get("datastore_active") or fmt_lower == "csv":
            return dataset_url, res["id"], res.get("name", "")

    raise ValueError(f"No suitable CSV/datastore resource found for slug '{slug}'")


# ---------------------------------------------------------------------------
# Download logic
# ---------------------------------------------------------------------------

def download_year(year: int, dest_dir: str, dry_run: bool) -> dict:
    """Download one year's eVA data.  Returns a manifest row dict."""
    slug = CKAN_SLUGS.get(year)

    # ------------------------------------------------------------------
    # Years not on the portal
    # ------------------------------------------------------------------
    if slug is None:
        msg = (
            "Not available on data.virginia.gov. "
            "Files for FY2003-FY2015 are pre-bundled in data/VAPOData/ "
            "(Combined.xlsx and VA_opendata_FYxxxx.txt)."
        )
        print(f"  FY{year}: SKIPPED — {msg}")
        return {
            "year": year,
            "status": "skipped_not_on_portal",
            "dataset_url": "",
            "resource_id": "",
            "download_url": "",
            "local_file": "",
            "content_length_reported": "",
            "bytes_downloaded": "",
            "rows_downloaded": "",
            "note": msg,
        }

    # ------------------------------------------------------------------
    # Resolve CKAN resource
    # ------------------------------------------------------------------
    print(f"  FY{year}: Resolving CKAN slug '{slug}' …")
    try:
        dataset_url, resource_id, resource_name = _resolve_resource(slug)
    except Exception as exc:
        print(f"  FY{year}: ERROR resolving resource — {exc}", file=sys.stderr)
        return {
            "year": year,
            "status": "error_resolving",
            "dataset_url": f"{CKAN_BASE}/dataset/{slug}",
            "resource_id": "",
            "download_url": "",
            "local_file": "",
            "content_length_reported": "",
            "bytes_downloaded": "",
            "rows_downloaded": "",
            "note": str(exc),
        }

    datastore_url = f"{CKAN_API}/datastore_search?resource_id={resource_id}&limit={PAGE_SIZE}"
    print(f"  FY{year}: dataset={dataset_url}")
    print(f"  FY{year}: resource_id={resource_id}  ('{resource_name}')")

    # ------------------------------------------------------------------
    # Dry run — just probe row count
    # ------------------------------------------------------------------
    if dry_run:
        try:
            probe_url = f"{CKAN_API}/datastore_search?resource_id={resource_id}&limit=0"
            probe = _get_json(probe_url)
            total = probe["result"]["total"]
            print(f"  FY{year}: DRY RUN — {total:,} rows available")
        except Exception as exc:
            total = "?"
            print(f"  FY{year}: DRY RUN — could not probe total: {exc}", file=sys.stderr)
        return {
            "year": year,
            "status": "dry_run",
            "dataset_url": dataset_url,
            "resource_id": resource_id,
            "download_url": datastore_url,
            "local_file": "",
            "content_length_reported": "",
            "bytes_downloaded": "",
            "rows_downloaded": str(total),
            "note": "dry-run only",
        }

    # ------------------------------------------------------------------
    # Paginated download via datastore_search
    # ------------------------------------------------------------------
    os.makedirs(dest_dir, exist_ok=True)
    local_file = os.path.join(dest_dir, f"eVA_procurement_{year}.csv")
    print(f"  FY{year}: Downloading to {local_file} …")

    total_rows = 0
    offset = 0
    fieldnames_written = False
    bytes_written = 0

    try:
        with open(local_file, "w", newline="", encoding="utf-8") as fout:
            writer = None
            while True:
                page_url = f"{datastore_url}&offset={offset}"
                page_data = _get_json(page_url)
                result = page_data["result"]
                records = result.get("records", [])
                if not records:
                    break

                if not fieldnames_written:
                    # Use field order from the API (skip internal _id field)
                    fields = [f["id"] for f in result.get("fields", []) if f["id"] != "_id"]
                    writer = csv.DictWriter(fout, fieldnames=fields, extrasaction="ignore", lineterminator="\n")
                    writer.writeheader()
                    fieldnames_written = True

                writer.writerows(records)
                total_rows += len(records)

                # Progress indicator
                total_available = result.get("total", "?")
                print(f"    … {total_rows:,} / {total_available} rows", end="\r")

                if len(records) < PAGE_SIZE:
                    break  # last page
                offset += PAGE_SIZE
                time.sleep(0.2)   # be polite

        bytes_written = os.path.getsize(local_file)
        print(f"\n  FY{year}: Done — {total_rows:,} rows, {bytes_written / (1024*1024):.2f} MB")
        status = "ok"
        note = ""

    except Exception as exc:
        print(f"\n  FY{year}: ERROR during download — {exc}", file=sys.stderr)
        status = "error_downloading"
        note = str(exc)

    return {
        "year": year,
        "status": status,
        "dataset_url": dataset_url,
        "resource_id": resource_id,
        "download_url": datastore_url,
        "local_file": local_file if status == "ok" else "",
        "content_length_reported": "",
        "bytes_downloaded": str(bytes_written) if status == "ok" else "",
        "rows_downloaded": str(total_rows) if status == "ok" else "",
        "note": note,
    }


# ---------------------------------------------------------------------------
# Manifest writer
# ---------------------------------------------------------------------------

MANIFEST_FIELDS = [
    "year", "status", "dataset_url", "resource_id", "download_url",
    "local_file", "content_length_reported", "bytes_downloaded",
    "rows_downloaded", "note",
]


def write_manifest(rows: list[dict], dest_dir: str):
    os.makedirs(dest_dir, exist_ok=True)
    manifest_path = os.path.join(dest_dir, "manifest.csv")
    with open(manifest_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=MANIFEST_FIELDS)
        writer.writeheader()
        writer.writerows(rows)
    print(f"\nManifest written: {manifest_path}")


# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------

def parse_years(spec: str) -> list[int]:
    spec = spec.strip()
    if "-" in spec and not spec.startswith("-"):
        parts = spec.split("-")
        start, end = int(parts[0]), int(parts[-1])
        return list(range(start, end + 1))
    return [int(y.strip()) for y in spec.split(",")]


def main():
    parser = argparse.ArgumentParser(
        description="Download Virginia eVA Purchase-Order data (FY2001–FY2016 target)."
    )
    parser.add_argument(
        "--dest",
        default=DEFAULT_DEST,
        help=f"Destination directory (default: {DEFAULT_DEST})",
    )
    parser.add_argument(
        "--years",
        default="2001-2016",
        help="Year range or list, e.g. 2016 or 2016,2017 or 2001-2016 (default: 2001-2016)",
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Probe availability and row counts without downloading",
    )
    args = parser.parse_args()

    years = parse_years(args.years)

    print("=" * 70)
    print("Virginia eVA Procurement Data Downloader")
    print(f"  Destination : {args.dest}")
    print(f"  Years       : {years[0]}–{years[-1]}  ({len(years)} years)")
    print(f"  Dry run     : {args.dry_run}")
    print("=" * 70)
    print()
    print("Note: FY2003–FY2015 are not on data.virginia.gov.")
    print("      Those files are pre-bundled in data/VAPOData/ in this repo.")
    print()

    manifest_rows = []
    for year in years:
        row = download_year(year, args.dest, dry_run=args.dry_run)
        manifest_rows.append(row)
        print()

    write_manifest(manifest_rows, args.dest)
    print("\nAll years processed.")


if __name__ == "__main__":
    main()
