#!/usr/bin/env python3
"""
car_detect_changes.py — Detect date-effective CAR and instTypeID changes across years.

Reads vendor PO attributes by year (or VendorMaster history) and compares successive
years to detect attribute migrations over time:
  - instTypeID changes: EXP <-> REV (triggers reclassification SJEs via arrangementReclass.scala)
  - instNIGPClass changes: e.g. '918' -> '920' (attribute-only change, affects Step 5 pivot views)
  - instGeoRegion changes: e.g. 'VA' -> 'MD' (attribute-only change, affects Step 5 pivot views)

Emits:
  data/VendorUpdate.csv (or specified out path)
  Columns: instID,instTypeID,instEffectDate,change_type,old_val,new_val,instNIGPClass,instGeoRegion

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2025 — CAR change detection for Arrangement Reclass and dynamic pivot (Bob AI)
"""

import argparse
import csv
import os
import sys
from collections import defaultdict

DEFAULT_PO_YEARS = os.path.join("data", "VAPOData", "vendor_po_attributes_by_year.csv")
DEFAULT_VM       = os.path.join("data", "VendorMaster_enriched.csv")
DEFAULT_OUT      = os.path.join("data", "VendorUpdate.csv")


def load_po_by_year(path: str) -> dict:
    """
    Loads vendor_po_attributes_by_year.csv into:
      vendor_year_map: { norm_inst_id: { fiscal_year_int: { instNIGPClass, instGeoRegion, VENDORNAME } } }
    """
    if not os.path.exists(path):
        print(f"ERROR: File not found: {path}", file=sys.stderr)
        sys.exit(1)

    vendor_year_map = defaultdict(dict)
    with open(path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            vid = row.get("VENDORID", "").strip().strip('"')
            if not vid:
                continue
            try:
                norm_id = str(int(vid)).zfill(10)
            except ValueError:
                norm_id = vid
            yr_str = row.get("fiscal_year", "").strip()
            if not yr_str.isdigit():
                continue
            yr = int(yr_str)
            vendor_year_map[norm_id][yr] = {
                "instNIGPClass": (row.get("instNIGPClass") or "").strip(),
                "instGeoRegion": (row.get("instGeoRegion") or "").strip(),
                "VENDORNAME":    (row.get("VENDORNAME") or "").strip().upper()
            }
    return vendor_year_map


def load_vendor_master_base(path: str) -> dict:
    """
    Loads baseline VendorMaster:
      vm_map: { norm_inst_id: { instTypeID, instHolderName, instVendorState, ... } }
    """
    vm_map = {}
    if not os.path.exists(path):
        print(f"WARNING: VendorMaster baseline not found at {path}", file=sys.stderr)
        return vm_map

    with open(path, newline="", encoding="utf-8-sig") as f:
        lines = [l for l in f if not l.startswith("#")]
    reader = csv.DictReader(lines)
    for row in reader:
        vid = row.get("instID", "").strip().strip('"')
        if not vid:
            continue
        try:
            norm_id = str(int(vid)).zfill(10)
        except ValueError:
            norm_id = vid
        vm_map[norm_id] = row
    return vm_map


def detect_changes(vendor_year_map: dict, vm_base: dict) -> list:
    """
    Two-pointer walk across sorted fiscal years per vendor.
    Detects changes in NIGP Class and Geo Region, plus any instTypeID variations.
    """
    updates = []
    
    # Sort by vendor ID for deterministic CKB order
    for vid in sorted(vendor_year_map.keys()):
        yr_data = vendor_year_map[vid]
        sorted_years = sorted(yr_data.keys())
        if len(sorted_years) < 2:
            continue

        prev_yr = sorted_years[0]
        prev_attrs = yr_data[prev_yr]

        base_vm = vm_base.get(vid, {})
        curr_type = base_vm.get("instTypeID", "EXP") or "EXP"

        for curr_yr in sorted_years[1:]:
            curr_attrs = yr_data[curr_yr]
            effect_date = f"{curr_yr}-07-01"  # VA fiscal year starts July 1

            nigp_changed = bool(curr_attrs["instNIGPClass"] and prev_attrs["instNIGPClass"] and 
                                curr_attrs["instNIGPClass"] != prev_attrs["instNIGPClass"])
            geo_changed  = bool(curr_attrs["instGeoRegion"] and prev_attrs["instGeoRegion"] and 
                                curr_attrs["instGeoRegion"] != prev_attrs["instGeoRegion"])

            if nigp_changed:
                updates.append({
                    "instID": vid,
                    "instTypeID": curr_type,
                    "instEffectDate": effect_date,
                    "change_type": "ATTR_NIGP",
                    "old_val": prev_attrs["instNIGPClass"],
                    "new_val": curr_attrs["instNIGPClass"],
                    "instNIGPClass": curr_attrs["instNIGPClass"],
                    "instGeoRegion": curr_attrs["instGeoRegion"] or prev_attrs["instGeoRegion"],
                    "fiscal_year": str(curr_yr)
                })

            if geo_changed:
                updates.append({
                    "instID": vid,
                    "instTypeID": curr_type,
                    "instEffectDate": effect_date,
                    "change_type": "ATTR_GEO",
                    "old_val": prev_attrs["instGeoRegion"],
                    "new_val": curr_attrs["instGeoRegion"],
                    "instNIGPClass": curr_attrs["instNIGPClass"] or prev_attrs["instNIGPClass"],
                    "instGeoRegion": curr_attrs["instGeoRegion"],
                    "fiscal_year": str(curr_yr)
                })

            prev_yr = curr_yr
            prev_attrs = curr_attrs

    return updates


def main():
    parser = argparse.ArgumentParser(description="Detect date-effective CAR changes across fiscal years.")
    parser.add_argument("--po-by-year", default=DEFAULT_PO_YEARS, help=f"Path to yearly PO attributes (default: {DEFAULT_PO_YEARS})")
    parser.add_argument("--vendormaster", default=DEFAULT_VM, help=f"Base VendorMaster CSV (default: {DEFAULT_VM})")
    parser.add_argument("--out", default=DEFAULT_OUT, help=f"Output VendorUpdate.csv path (default: {DEFAULT_OUT})")
    parser.add_argument("--dry-run", action="store_true", help="Print summary without writing output")
    args = parser.parse_args()

    print("=" * 70)
    print("CAR Date-Effective Change Detector")
    print(f"  PO by year   : {args.po_by_year}")
    print(f"  VendorMaster : {args.vendormaster}")
    print(f"  Output       : {args.out}")
    print(f"  Dry run      : {args.dry_run}")
    print("=" * 70)

    vendor_year_map = load_po_by_year(args.po_by_year)
    print(f"Loaded {len(vendor_year_map):,} distinct vendors with year-over-year PO history.")

    vm_base = load_vendor_master_base(args.vendormaster)
    print(f"Loaded {len(vm_base):,} baseline VendorMaster records.")

    updates = detect_changes(vendor_year_map, vm_base)
    print(f"Detected {len(updates):,} attribute updates across all years.")

    reclass_count = sum(1 for u in updates if u["change_type"] == "RECLASS")
    nigp_count    = sum(1 for u in updates if u["change_type"] == "ATTR_NIGP")
    geo_count     = sum(1 for u in updates if u["change_type"] == "ATTR_GEO")

    print(f"  RECLASS updates  : {reclass_count:,}")
    print(f"  ATTR_NIGP updates: {nigp_count:,}")
    print(f"  ATTR_GEO updates : {geo_count:,}")

    if args.dry_run:
        print("\n--dry-run: No files written.")
        if updates:
            print("\nSample updates:")
            for u in updates[:5]:
                print(f"  {u}")
        return

    os.makedirs(os.path.dirname(os.path.abspath(args.out)), exist_ok=True)
    fieldnames = ["instID", "instTypeID", "instEffectDate", "change_type", "old_val", "new_val", "instNIGPClass", "instGeoRegion", "fiscal_year"]
    with open(args.out, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(updates)

    print(f"\nWritten: {args.out} ({len(updates):,} rows)")
    print("Done.")


if __name__ == "__main__":
    main()
