#!/usr/bin/env python3
"""
enrich_vendormaster.py — Join PO attributes onto VendorMaster to produce
                          an enriched VendorMaster with instNIGPClass and
                          instGeoRegion columns.

WHAT THIS DOES
--------------
VendorMaster_full.csv   — 762K vendors keyed on instHolderName (UPPER)
vendor_po_attributes.csv — 69K distinct VENDORID values with modal NIGP class
                           and state derived from 15.6M PO line items.

The join key is: UPPER(VENDORNAME) from PO attributes → UPPER(instHolderName)
from VendorMaster.  VendorMaster_full was built from the same raw data so names
match closely.

OUTPUT
------
  data/VendorMaster_enriched.csv  — full VendorMaster plus two new columns:
    instNIGPClass   3-digit NIGP commodity class (blank if no PO data)
    instGeoRegion   2-letter US state from vendor order address (blank if missing)

USAGE
-----
  python scripts/enrich_vendormaster.py
  python scripts/enrich_vendormaster.py --dry-run
  python scripts/enrich_vendormaster.py \
      --vendormaster data/VendorMaster_full.csv \
      --po-attrs     data/VAPOData/vendor_po_attributes.csv \
      --out          data/VendorMaster_enriched.csv

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2025 — VendorMaster CAR enrichment (Bob AI)
"""

import argparse
import csv
import os
import sys

DEFAULT_VM       = os.path.join("data", "VendorMaster_full.csv")
DEFAULT_PO_ATTRS = os.path.join("data", "VAPOData", "vendor_po_attributes.csv")
DEFAULT_OUT      = os.path.join("data", "VendorMaster_enriched.csv")


def load_po_attrs(path: str) -> dict:
    """
    Return dict: UPPER(VENDORNAME) → {'instNIGPClass': ..., 'instGeoRegion': ...}
    Only includes rows where VENDORNAME looks like an actual name (not a dollar amount).
    """
    attrs = {}
    skipped = 0
    with open(path, newline="", encoding="utf-8") as f:
        for row in csv.DictReader(f):
            name = (row.get("VENDORNAME") or "").strip().strip('"').upper()
            # Skip rows where VENDORNAME is blank, numeric, or starts with $ (data quality)
            if not name or name.startswith("$") or name.replace(".", "").replace(",", "").isnumeric():
                skipped += 1
                continue
            attrs[name] = {
                "instNIGPClass": (row.get("instNIGPClass") or "").strip(),
                "instGeoRegion": (row.get("instGeoRegion") or "").strip(),
            }
    print(f"PO attrs loaded: {len(attrs):,} name-keyed entries  ({skipped} skipped bad rows)")
    return attrs


def main():
    parser = argparse.ArgumentParser(
        description="Enrich VendorMaster with instNIGPClass + instGeoRegion from PO data."
    )
    parser.add_argument("--vendormaster", default=DEFAULT_VM)
    parser.add_argument("--po-attrs",     default=DEFAULT_PO_ATTRS)
    parser.add_argument("--out",          default=DEFAULT_OUT)
    parser.add_argument("--dry-run",      action="store_true")
    args = parser.parse_args()

    print("=" * 70)
    print("VendorMaster CAR Enrichment")
    print(f"  VendorMaster : {args.vendormaster}")
    print(f"  PO attributes: {args.po_attrs}")
    print(f"  Output       : {args.out}")
    print(f"  Dry run      : {args.dry_run}")
    print("=" * 70)

    po_attrs = load_po_attrs(args.po_attrs)

    out_fields = None
    matched = 0
    total = 0
    rows_out = []

    with open(args.vendormaster, newline="", encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        base_fields = [fn.strip().strip('"') for fn in reader.fieldnames]
        out_fields = base_fields + ["instNIGPClass", "instGeoRegion"]

        for row in reader:
            name_key = (row.get("instHolderName") or "").strip().strip('"').upper()
            pa = po_attrs.get(name_key, {})
            row["instNIGPClass"] = pa.get("instNIGPClass", "")
            row["instGeoRegion"] = pa.get("instGeoRegion", "")
            if pa:
                matched += 1
            total += 1
            rows_out.append(row)

    nigp_filled  = sum(1 for r in rows_out if r["instNIGPClass"])
    state_filled = sum(1 for r in rows_out if r["instGeoRegion"])

    print(f"\nControl totals:")
    print(f"  VendorMaster rows:       {total:,}")
    print(f"  Rows matched to PO data: {matched:,}  ({100*matched//max(total,1)}%)")
    print(f"  instNIGPClass filled:    {nigp_filled:,}  ({100*nigp_filled//max(total,1)}%)")
    print(f"  instGeoRegion filled:    {state_filled:,}  ({100*state_filled//max(total,1)}%)")

    if args.dry_run:
        print("\n--dry-run: no output written.")
        return

    os.makedirs(os.path.dirname(os.path.abspath(args.out)) or ".", exist_ok=True)
    with open(args.out, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=out_fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows_out)

    size_mb = os.path.getsize(args.out) / 1024 / 1024
    print(f"\nWritten: {args.out}  ({total:,} rows, {size_mb:.1f} MB)")
    print("Done.")


if __name__ == "__main__":
    main()
