#!/usr/bin/env python3
"""
extract_po_attributes.py — Extract vendor CAR attributes from VA eVA PO files.

Reads VA_opendata_FYxxxx.txt files (tab-delimited PO data) from data/VAPOData/
and produces a VendorMaster patch CSV containing the most-frequently-observed
NIGP class code and state for each VENDORID.

These attributes are CAR (Contract Attributes Record) dimensions for
Experiment 2 of the Universal Ledger POC:
  instNIGPClass  — top-3-digit NIGP commodity class (Round 1 of CAR expansion)
  instGeoRegion  — VENDORSTATE from order address   (Round 3 of CAR expansion)

OUTPUT
------
  data/VAPOData/vendor_po_attributes.csv
    Columns: VENDORID, VENDORNAME, instNIGPClass, instNIGPDesc, instGeoRegion,
             po_line_count, years_seen

  data/VAPOData/po_attributes_summary.txt
    Control report: records read per file, distinct vendors, NIGP coverage.

USAGE
-----
  python scripts/extract_po_attributes.py
  python scripts/extract_po_attributes.py --po-dir data/VAPOData --out data/VAPOData/vendor_po_attributes.csv
  python scripts/extract_po_attributes.py --dry-run

FILES PROCESSED
---------------
  VA_opendata_FY2001.txt … VA_opendata_FY2016.txt  (tab-delimited, header row)
  Fields used: VENDORID, VENDORNAME, VENDORSTATE, NIGPCODE, NIGPDESCRIPTION

  FY2001 and FY2002 are small / stub files — included but may contribute few rows.
  FY2009 is absent from the txt set.

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2025 — PO attribute extraction for CAR enrichment (Bob AI)
"""

import argparse
import csv
import os
import sys
from collections import Counter, defaultdict

DEFAULT_PO_DIR = os.path.join("data", "VAPOData")
DEFAULT_OUT    = os.path.join("data", "VAPOData", "vendor_po_attributes.csv")

# Files to process — tab-delimited VA_opendata_FYxxxx.txt
# We derive this dynamically from what's actually present.
EXPECTED_YEARS = list(range(2001, 2017))


def po_txt_files(po_dir: str) -> list:
    """Return sorted list of VA_opendata_FYxxxx.txt paths that exist."""
    found = []
    for yr in EXPECTED_YEARS:
        path = os.path.join(po_dir, f"VA_opendata_FY{yr}.txt")
        if os.path.exists(path):
            size = os.path.getsize(path)
            found.append((yr, path, size))
    return found


def process_files(po_dir: str, dry_run: bool, verbose: bool):
    """
    Single pass over all PO txt files.
    Returns (vendor_nigp, vendor_state, vendor_name_map, summary_lines).

    vendor_nigp  : dict  VENDORID → Counter {nigp_class_3digit: count}
    vendor_state : dict  VENDORID → Counter {state: count}
    vendor_names : dict  VENDORID → most-seen VENDORNAME
    """
    vendor_nigp  = defaultdict(Counter)   # VENDORID → {nigp3: count}
    vendor_state = defaultdict(Counter)   # VENDORID → {state: count}
    vendor_names = defaultdict(Counter)   # VENDORID → {name: count}
    summary      = []

    files = po_txt_files(po_dir)
    if not files:
        print(f"ERROR: No VA_opendata_FYxxxx.txt files found in {po_dir}", file=sys.stderr)
        sys.exit(1)

    total_records = 0
    total_with_nigp = 0

    for yr, path, size_bytes in files:
        size_mb = size_bytes / 1024 / 1024
        if size_mb < 0.5:
            line = f"FY{yr}: {os.path.basename(path)}  {size_mb:.1f} MB — STUB/EMPTY, skipped"
            summary.append(line)
            print(line)
            continue

        print(f"FY{yr}: reading {os.path.basename(path)}  ({size_mb:.0f} MB) …", end=" ", flush=True)

        file_records = 0
        file_with_nigp = 0

        with open(path, newline="", encoding="utf-8-sig", errors="replace") as f:
            # Files have a blank first line before the header row — skip blanks
            non_blank = (line for line in f if line.strip())
            reader = csv.DictReader(non_blank, delimiter="\t")
            for row in reader:
                vendor_id   = (row.get("VENDORID") or "").strip().strip('"')
                vendor_name = (row.get("VENDORNAME") or "").strip().strip('"').upper()
                state       = (row.get("VENDORSTATE") or "").strip().strip('"').upper()
                nigp_raw    = (row.get("NIGPCODE") or "").strip().strip('"')

                if not vendor_id:
                    continue

                file_records += 1
                vendor_names[vendor_id][vendor_name] += 1

                if state:
                    vendor_state[vendor_id][state] += 1

                if nigp_raw:
                    # NIGP class = first 3 digits (commodity class level)
                    nigp_class = nigp_raw[:3]
                    vendor_nigp[vendor_id][nigp_class] += 1
                    file_with_nigp += 1

        total_records   += file_records
        total_with_nigp += file_with_nigp
        pct = int(100 * file_with_nigp / max(file_records, 1))
        line = f"FY{yr}: {file_records:,} records  {file_with_nigp:,} with NIGP ({pct}%)"
        summary.append(line)
        print(line)

    summary.append("")
    summary.append(f"Total records read:      {total_records:,}")
    summary.append(f"Total with NIGP code:    {total_with_nigp:,}  "
                   f"({100*total_with_nigp//max(total_records,1)}%)")
    summary.append(f"Distinct VENDORID values: {len(vendor_names):,}")

    return vendor_nigp, vendor_state, vendor_names, summary


def process_files_by_year(po_dir: str):
    """
    Single pass over all PO txt files, tracking vendor attributes per fiscal year.
    Returns list of dicts ready for CSV output:
    [
      {
        "VENDORID": vid,
        "fiscal_year": yr,
        "VENDORNAME": best_name,
        "instNIGPClass": best_nigp,
        "instGeoRegion": best_state,
        "po_line_count": po_lines,
      }, ...
    ]
    and summary lines.
    """
    files = po_txt_files(po_dir)
    if not files:
        print(f"ERROR: No VA_opendata_FYxxxx.txt files found in {po_dir}", file=sys.stderr)
        sys.exit(1)

    summary = []
    total_records = 0
    total_with_nigp = 0
    all_year_rows = []

    for yr, path, size_bytes in files:
        size_mb = size_bytes / 1024 / 1024
        if size_mb < 0.5:
            line = f"FY{yr}: {os.path.basename(path)}  {size_mb:.1f} MB — STUB/EMPTY, skipped"
            summary.append(line)
            print(line)
            continue

        print(f"FY{yr}: reading {os.path.basename(path)}  ({size_mb:.0f} MB) …", end=" ", flush=True)

        file_records = 0
        file_with_nigp = 0

        vendor_nigp  = defaultdict(Counter)   # VENDORID -> {nigp3: count}
        vendor_state = defaultdict(Counter)   # VENDORID -> {state: count}
        vendor_names = defaultdict(Counter)   # VENDORID -> {name: count}

        with open(path, newline="", encoding="utf-8-sig", errors="replace") as f:
            non_blank = (line for line in f if line.strip())
            reader = csv.DictReader(non_blank, delimiter="\t")
            for row in reader:
                vendor_id   = (row.get("VENDORID") or "").strip().strip('"')
                vendor_name = (row.get("VENDORNAME") or "").strip().strip('"').upper()
                state       = (row.get("VENDORSTATE") or "").strip().strip('"').upper()
                nigp_raw    = (row.get("NIGPCODE") or "").strip().strip('"')

                if not vendor_id:
                    continue

                file_records += 1
                vendor_names[vendor_id][vendor_name] += 1

                if state:
                    vendor_state[vendor_id][state] += 1

                if nigp_raw:
                    nigp_class = nigp_raw[:3]
                    vendor_nigp[vendor_id][nigp_class] += 1
                    file_with_nigp += 1

        total_records += file_records
        total_with_nigp += file_with_nigp
        pct = int(100 * file_with_nigp / max(file_records, 1))
        line = f"FY{yr}: {file_records:,} records  {file_with_nigp:,} with NIGP ({pct}%)"
        summary.append(line)
        print(line)

        # Build rows for this fiscal year
        all_ids = set(vendor_names.keys()) | set(vendor_nigp.keys()) | set(vendor_state.keys())
        for vid in sorted(all_ids):
            name_ctr  = vendor_names.get(vid, Counter())
            nigp_ctr  = vendor_nigp.get(vid, Counter())
            state_ctr = vendor_state.get(vid, Counter())

            best_name   = name_ctr.most_common(1)[0][0] if name_ctr else ""
            best_nigp   = nigp_ctr.most_common(1)[0][0] if nigp_ctr else ""
            best_state  = state_ctr.most_common(1)[0][0] if state_ctr else ""
            po_lines    = sum(name_ctr.values())

            all_year_rows.append({
                "VENDORID":      vid,
                "fiscal_year":   str(yr),
                "VENDORNAME":    best_name,
                "instNIGPClass": best_nigp,
                "instGeoRegion": best_state,
                "po_line_count": po_lines,
            })

    summary.append("")
    summary.append(f"Total records read:      {total_records:,}")
    summary.append(f"Total with NIGP code:    {total_with_nigp:,}  "
                   f"({100*total_with_nigp//max(total_records,1)}%)")
    summary.append(f"Total yearly vendor rows: {len(all_year_rows):,}")

    return all_year_rows, summary


def build_output_rows(vendor_nigp, vendor_state, vendor_names) -> list:
    """
    For each VENDORID choose the modal NIGP class and modal state across all years.
    Returns list of dicts ready for csv.DictWriter.
    """
    all_ids = set(vendor_names.keys()) | set(vendor_nigp.keys()) | set(vendor_state.keys())
    rows = []
    for vid in sorted(all_ids):
        name_ctr  = vendor_names.get(vid, Counter())
        nigp_ctr  = vendor_nigp.get(vid, Counter())
        state_ctr = vendor_state.get(vid, Counter())

        best_name   = name_ctr.most_common(1)[0][0] if name_ctr else ""
        best_nigp   = nigp_ctr.most_common(1)[0][0] if nigp_ctr else ""
        best_state  = state_ctr.most_common(1)[0][0] if state_ctr else ""
        po_lines    = sum(name_ctr.values())

        rows.append({
            "VENDORID":       vid,
            "VENDORNAME":     best_name,
            "instNIGPClass":  best_nigp,
            "instGeoRegion":  best_state,
            "po_line_count":  po_lines,
        })
    return rows


def write_output(rows: list, out_path: str, fields: list = None):
    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    if fields is None:
        fields = ["VENDORID", "VENDORNAME", "instNIGPClass", "instGeoRegion", "po_line_count"]
    with open(out_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)
    print(f"\nWritten: {out_path}  ({len(rows):,} rows)")


def write_summary(summary: list, out_path: str, rows: list):
    nigp_covered  = sum(1 for r in rows if r["instNIGPClass"])
    state_covered = sum(1 for r in rows if r["instGeoRegion"])
    total         = len(rows)

    summary.append(f"NIGP class coverage:     {nigp_covered:,} / {total:,} rows "
                   f"({100*nigp_covered//max(total,1)}%)")
    summary.append(f"State coverage:          {state_covered:,} / {total:,} rows "
                   f"({100*state_covered//max(total,1)}%)")

    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    with open(out_path, "w", encoding="utf-8") as f:
        f.write("\n".join(summary) + "\n")
    print(f"Summary: {out_path}")
    for line in summary[-6:]:
        print(f"  {line}")


def main():
    parser = argparse.ArgumentParser(
        description="Extract NIGP class + geo-region from VA PO files → vendor_po_attributes.csv"
    )
    parser.add_argument("--po-dir", default=DEFAULT_PO_DIR,
                        help=f"Directory containing VA_opendata_FYxxxx.txt (default: {DEFAULT_PO_DIR})")
    parser.add_argument("--out", default=None,
                        help=f"Output CSV path (default: data/VAPOData/vendor_po_attributes.csv or vendor_po_attributes_by_year.csv)")
    parser.add_argument("--by-year", action="store_true",
                        help="Extract attributes per vendor per fiscal year")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print control totals; do not write output")
    parser.add_argument("--verbose", action="store_true")
    args = parser.parse_args()

    if args.out is None:
        if args.by_year:
            args.out = os.path.join(args.po_dir, "vendor_po_attributes_by_year.csv")
        else:
            args.out = DEFAULT_OUT

    print("=" * 70)
    print("VA eVA PO Attribute Extractor")
    print(f"  PO dir : {args.po_dir}")
    print(f"  Output : {args.out}")
    print(f"  By year: {args.by_year}")
    print(f"  Dry run: {args.dry_run}")
    print("=" * 70)

    summary_path = os.path.join(os.path.dirname(args.out), "po_attributes_by_year_summary.txt" if args.by_year else "po_attributes_summary.txt")

    if args.by_year:
        rows, summary = process_files_by_year(args.po_dir)
        if args.dry_run:
            print(f"\n--dry-run: {len(rows):,} yearly vendor rows found, no files written.")
            for line in summary:
                print(f"  {line}")
            return
        fields = ["VENDORID", "fiscal_year", "VENDORNAME", "instNIGPClass", "instGeoRegion", "po_line_count"]
        write_output(rows, args.out, fields)
        write_summary(summary, summary_path, rows)
    else:
        vendor_nigp, vendor_state, vendor_names, summary = process_files(
            args.po_dir, args.dry_run, args.verbose
        )
        rows = build_output_rows(vendor_nigp, vendor_state, vendor_names)
        if args.dry_run:
            print(f"\n--dry-run: {len(rows):,} vendors found, no files written.")
            for line in summary:
                print(f"  {line}")
            return
        write_output(rows, args.out)
        write_summary(summary, summary_path, rows)

    print("\nDone.")


if __name__ == "__main__":
    main()
