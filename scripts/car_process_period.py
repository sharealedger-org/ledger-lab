#!/usr/bin/env python3
"""
car_process_period.py — Chronological per-period CAR ingestion and roll-forward.

Performs the Partition C (Attribute History / CAR) update for a specific fiscal period T:
  1. Ingests the PO file for period T (e.g. data/VAPOData/VA_opendata_FY{T}.txt).
  2. Extracts vendor attributes (NIGP Class, Geo Region, modal names) observed in period T.
  3. Compares period T attributes against the accumulated VendorMaster state up to T-1.
  4. Emits VendorUpdate_{T}.csv capturing all detected changes effective as of period T.
  5. Rolls forward VendorMaster.csv (closing prior date-effective records and inserting new versions).

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2025 — Per-period chronological CAR processor (Bob AI)
"""

import argparse
import csv
import os
import sys
from collections import Counter, defaultdict

DEFAULT_PO_DIR = os.path.join("data", "VAPOData")
DEFAULT_VM_DIR = os.path.join("data")


def extract_period_po_attributes(po_path: str) -> dict:
    """
    Reads a single PO txt file for period T.
    Returns dict: norm_vendor_id -> { "name": str, "nigp": str, "state": str, "po_lines": int }
    """
    if not os.path.exists(po_path):
        print(f"INFO: No PO file found at {po_path} for this period — empty PO delta.")
        return {}

    size_mb = os.path.getsize(po_path) / 1024 / 1024
    if size_mb < 0.5:
        print(f"INFO: PO file {po_path} is stub/empty ({size_mb:.1f} MB) — empty PO delta.")
        return {}

    vendor_nigp  = defaultdict(Counter)
    vendor_state = defaultdict(Counter)
    vendor_names = defaultdict(Counter)
    record_count = 0

    with open(po_path, newline="", encoding="utf-8-sig", errors="replace") as f:
        non_blank = (line for line in f if line.strip())
        reader = csv.DictReader(non_blank, delimiter="\t")
        for row in reader:
            vid = (row.get("VENDORID") or "").strip().strip('"')
            if not vid:
                continue
            try:
                norm_id = str(int(vid)).zfill(10)
            except ValueError:
                norm_id = vid

            vname = (row.get("VENDORNAME") or "").strip().strip('"').upper()
            state = (row.get("VENDORSTATE") or "").strip().strip('"').upper()
            nigp  = (row.get("NIGPCODE") or "").strip().strip('"')

            record_count += 1
            vendor_names[norm_id][vname] += 1
            if state:
                vendor_state[norm_id][state] += 1
            if nigp:
                vendor_nigp[norm_id][nigp[:3]] += 1

    print(f"  PO records read: {record_count:,} across {len(vendor_names):,} vendors")

    period_vendors = {}
    all_vids = set(vendor_names.keys()) | set(vendor_nigp.keys()) | set(vendor_state.keys())
    for vid in all_vids:
        n_ctr = vendor_names.get(vid, Counter())
        g_ctr = vendor_nigp.get(vid, Counter())
        s_ctr = vendor_state.get(vid, Counter())

        best_name  = n_ctr.most_common(1)[0][0] if n_ctr else ""
        best_nigp  = g_ctr.most_common(1)[0][0] if g_ctr else ""
        best_state = s_ctr.most_common(1)[0][0] if s_ctr else ""
        lines      = sum(n_ctr.values())

        period_vendors[vid] = {
            "name": best_name,
            "nigp": best_nigp,
            "state": best_state,
            "po_lines": lines
        }
    return period_vendors


def load_vendormaster_history(vm_path: str) -> tuple[dict, list]:
    """
    Loads VendorMaster CSV.
    Returns:
      vm_history: { norm_id: [ list of row dicts sorted by instEffectDate ] }
      fieldnames: list of column names
    """
    vm_history = defaultdict(list)
    fieldnames = [
        "instID", "instEffectDate", "instEffectEndDate", "instHolderName",
        "instTypeID", "instVendorAddress", "instVendorCity", "instVendorState",
        "instVendorPostalCode", "instAuditTrail", "instNIGPClass", "instNIGPDesc", "instGeoRegion"
    ]
    if not os.path.exists(vm_path):
        return vm_history, fieldnames

    with open(vm_path, newline="", encoding="utf-8-sig") as f:
        lines = [l for l in f if not l.startswith("#")]
    reader = csv.DictReader(lines)
    if reader.fieldnames:
        fieldnames = list(reader.fieldnames)
    for row in reader:
        vid = (row.get("instID") or "").strip().strip('"')
        if not vid:
            continue
        try:
            norm_id = str(int(vid)).zfill(10)
        except ValueError:
            norm_id = vid
        vm_history[norm_id].append(row)

    # Sort each vendor history by instEffectDate
    for vid in vm_history:
        vm_history[vid].sort(key=lambda r: r.get("instEffectDate", "0000-00-00"))

    return vm_history, fieldnames


def process_period_sal(year: int, po_dir: str, vm_path: str, update_out: str, dry_run: bool):
    """
    Executes the chronological CAR step for fiscal year `year`.
    """
    po_file = os.path.join(po_dir, f"VA_opendata_FY{year}.txt")
    effect_date = f"{year}-07-01"
    prior_end_date = f"{year}-06-30"

    print("=" * 70)
    print(f"Chronological CAR Update — Fiscal Year {year}")
    print(f"  PO Source File : {po_file}")
    print(f"  VendorMaster   : {vm_path}")
    print(f"  Updates Output : {update_out}")
    print(f"  Effective Date : {effect_date}")
    print("=" * 70)

    period_po = extract_period_po_attributes(po_file)
    vm_history, fieldnames = load_vendormaster_history(vm_path)

    updates = []
    new_vendors_count = 0
    attr_changed_count = 0

    for vid, po_info in period_po.items():
        if vid not in vm_history or not vm_history[vid]:
            # Brand new vendor discovered in this period
            new_row = {
                "instID": vid,
                "instEffectDate": effect_date,
                "instEffectEndDate": "9999-99-99",
                "instHolderName": po_info["name"],
                "instTypeID": "EXP",
                "instVendorAddress": " ",
                "instVendorCity": " ",
                "instVendorState": po_info["state"],
                "instVendorPostalCode": " ",
                "instAuditTrail": f"FY{year}_PO_INIT",
                "instNIGPClass": po_info["nigp"],
                "instNIGPDesc": " ",
                "instGeoRegion": po_info["state"]
            }
            vm_history[vid].append(new_row)
            new_vendors_count += 1
        else:
            # Existing vendor — check if NIGP, Geo Region, or Name changed
            latest_row = vm_history[vid][-1]
            old_nigp = (latest_row.get("instNIGPClass") or "").strip()
            old_geo  = (latest_row.get("instGeoRegion") or latest_row.get("instVendorState") or "").strip()
            old_type = (latest_row.get("instTypeID") or "EXP").strip()

            new_nigp = po_info["nigp"] if po_info["nigp"] else old_nigp
            new_geo  = po_info["state"] if po_info["state"] else old_geo

            nigp_changed = bool(new_nigp and old_nigp and new_nigp != old_nigp)
            geo_changed  = bool(new_geo and old_geo and new_geo != old_geo)

            if nigp_changed or geo_changed:
                attr_changed_count += 1
                change_type = "ATTR_NIGP" if nigp_changed else "ATTR_GEO"
                updates.append({
                    "instID": vid,
                    "instTypeID": old_type,
                    "instEffectDate": effect_date,
                    "change_type": change_type,
                    "old_val": old_nigp if nigp_changed else old_geo,
                    "new_val": new_nigp if nigp_changed else new_geo,
                    "instNIGPClass": new_nigp,
                    "instGeoRegion": new_geo,
                    "fiscal_year": str(year)
                })

                # Close prior effective record
                latest_row["instEffectEndDate"] = prior_end_date

                # Append new date-effective version
                new_version = dict(latest_row)
                new_version["instEffectDate"] = effect_date
                new_version["instEffectEndDate"] = "9999-99-99"
                new_version["instNIGPClass"] = new_nigp
                new_version["instGeoRegion"] = new_geo
                new_version["instVendorState"] = new_geo
                new_version["instAuditTrail"] = f"FY{year}_PO_UPDATE"
                vm_history[vid].append(new_version)

    print(f"  New vendors added: {new_vendors_count:,}")
    print(f"  Attribute updates detected: {len(updates):,}")

    if dry_run:
        print("\n--dry-run: No files written.")
        return

    # Write VendorUpdate_{year}.csv
    if update_out:
        os.makedirs(os.path.dirname(os.path.abspath(update_out)), exist_ok=True)
        up_fields = ["instID", "instTypeID", "instEffectDate", "change_type", "old_val", "new_val", "instNIGPClass", "instGeoRegion", "fiscal_year"]
        with open(update_out, "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=up_fields)
            w.writeheader()
            w.writerows(updates)
        print(f"  Written updates: {update_out} ({len(updates):,} rows)")

    # Write updated VendorMaster.csv (all history records, sorted by instID + instEffectDate)
    os.makedirs(os.path.dirname(os.path.abspath(vm_path)), exist_ok=True)
    all_rows = []
    for vid in sorted(vm_history.keys()):
        for rec in vm_history[vid]:
            all_rows.append(rec)

    with open(vm_path, "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        w.writeheader()
        w.writerows(all_rows)

    print(f"  Written VendorMaster: {vm_path} ({len(all_rows):,} total history rows)")
    print("STATUS: COMPLETE")


def main():
    parser = argparse.ArgumentParser(description="Chronological per-period CAR ingestion and roll-forward.")
    parser.add_argument("--year", type=int, required=True, help="Fiscal year to process (e.g. 2003)")
    parser.add_argument("--po-dir", default=DEFAULT_PO_DIR, help=f"PO data directory (default: {DEFAULT_PO_DIR})")
    parser.add_argument("--vendormaster", default=os.path.join(DEFAULT_VM_DIR, "VendorMaster_enriched.csv"), help="Path to VendorMaster CSV")
    parser.add_argument("--update-out", default=None, help="Path to output VendorUpdate CSV for this period")
    parser.add_argument("--dry-run", action="store_true", help="Do not write files")
    args = parser.parse_args()

    if args.update_out is None:
        args.update_out = os.path.join("data", "output", f"VendorUpdate_FY{args.year}.csv")

    process_period_sal(args.year, args.po_dir, args.vendormaster, args.update_out, args.dry_run)


if __name__ == "__main__":
    main()
