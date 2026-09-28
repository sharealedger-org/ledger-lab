#!/usr/bin/env python3
"""
download_va_data.py — Automated downloader for Virginia APA Open Data portal datasets.

Downloads and extracts:
  - Expenditures (FY2003 - FY2016)
  - Revenues (FY2003 - FY2016)
  - Budget (FY2003 - FY2016)

Usage:
  python scripts/download_va_data.py --dest data/VARawFiles --years 2003,2004 --types exp,rev
  python scripts/download_va_data.py --dest data/VARawFiles --all
  python scripts/download_va_data.py --dry-run
"""

import argparse
import os
import sys
import urllib.parse
import urllib.request
import zipfile
import ssl

BASE_URL = "https://www.datapoint.apa.virginia.gov/"

DATASET_CONFIG = {
    "exp": {
        "label": "Expenditures",
        "pattern": "historicaldata/FY {year} Expenditures.zip",
    },
    "rev": {
        "label": "Revenues",
        "pattern": "historicaldata/FY {year} Revenues.zip",
    },
    "budget": {
        "label": "Budget",
        "pattern": "historicaldata/FY {year} Budget.zip",
    },
}

ALL_YEARS = [str(y) for y in range(2003, 2017)]


def get_url(path: str) -> str:
    quoted_path = urllib.parse.quote(path)
    return urllib.parse.urljoin(BASE_URL, quoted_path)


def download_and_extract(url: str, dest_dir: str, dry_run: bool = False, keep_zip: bool = False):
    ctx = ssl.create_default_context()
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"})

    print(f"Connecting to: {url}")
    try:
        with urllib.request.urlopen(req, context=ctx, timeout=30) as resp:
            content_length = resp.headers.get("Content-Length")
            size_mb = f"{int(content_length) / (1024 * 1024):.2f} MB" if content_length else "Unknown size"
            print(f"  Status: {resp.status} | Size: {size_mb}")

            if dry_run:
                return

            zip_filename = os.path.basename(urllib.parse.unquote(url))
            zip_dest = os.path.join(dest_dir, zip_filename)

            os.makedirs(dest_dir, exist_ok=True)
            print(f"  Downloading to: {zip_dest} ...")

            with open(zip_dest, "wb") as f_out:
                block_size = 64 * 1024
                while True:
                    chunk = resp.read(block_size)
                    if not chunk:
                        break
                    f_out.write(chunk)

            print(f"  Extracting to: {dest_dir} ...")
            with zipfile.ZipFile(zip_dest, "r") as zip_ref:
                zip_ref.extractall(dest_dir)

            if not keep_zip:
                os.remove(zip_dest)
                print(f"  Removed temp archive: {zip_filename}")

            print("  Done.")
    except Exception as e:
        print(f"  ERROR downloading {url}: {e}", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description="Download and extract Virginia APA Open Data files")
    parser.add_argument("--dest", default="data/VARawFiles", help="Destination directory (default: data/VARawFiles)")
    parser.add_argument("--years", default="2003-2016", help="Years to download (e.g. 2003 or 2003,2004 or 2003-2016)")
    parser.add_argument("--types", default="exp,rev,budget", help="Types to download: exp, rev, budget (comma-separated)")
    parser.add_argument("--all", action="store_true", help="Download all FY2003-FY2016 exp, rev, and budget files")
    parser.add_argument("--keep-zip", action="store_true", help="Keep downloaded .zip files after extraction")
    parser.add_argument("--dry-run", action="store_true", help="Check availability and file sizes without downloading")

    args = parser.parse_args()

    # Parse years
    if args.all or args.years == "2003-2016":
        years = ALL_YEARS
    elif "-" in args.years:
        start, end = args.years.split("-")
        years = [str(y) for y in range(int(start), int(end) + 1)]
    else:
        years = [y.strip() for y in args.years.split(",")]

    # Parse types
    selected_types = [t.strip().lower() for t in args.types.split(",")]

    print("=" * 70)
    print("Virginia APA Open Data Downloader")
    print(f"  Destination: {args.dest}")
    print(f"  Years:       {', '.join(years)}")
    print(f"  Types:       {', '.join(selected_types)}")
    print(f"  Dry Run:     {args.dry_run}")
    print("=" * 70)

    for year in years:
        for t in selected_types:
            if t not in DATASET_CONFIG:
                print(f"Unknown type '{t}'. Available: exp, rev, budget", file=sys.stderr)
                continue
            cfg = DATASET_CONFIG[t]
            rel_path = cfg["pattern"].format(year=year)
            url = get_url(rel_path)
            download_and_extract(url, args.dest, dry_run=args.dry_run, keep_zip=args.keep_zip)

    print("\nAll downloads finished.")


if __name__ == "__main__":
    main()
