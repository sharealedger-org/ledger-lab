#!/usr/bin/env python3
"""
prepare_vendormaster_round1.py — Fill Round 1 gaps in VendorMaster.csv.

Rules applied (see docs/PHASE2_SYNTHETIC_DATA_PLAN.md §2.1):

  REV instruments (232 rows, blank instNIGPClass/instNIGPDesc/instGeoRegion):
    instNIGPClass  → "GOV-RECEIPT"
    instNIGPDesc   → "Government Receipt — Revenue Source"
    instGeoRegion  → "GOV"

  EXP instruments with blank instGeoRegion (183 rows, 99xxx series):
    instGeoRegion  → derived from instVendorState using the US region mapping
                     below.  Rows with no mappable state keep blank (logged).
    instNIGPClass, instNIGPDesc → unchanged (also blank for the 99xxx series;
                     a separate enrichment pass using PO attributes would be
                     needed to fill them).

  instAuditTrail  → updated to this script's run date (YYYY-MM-DD).

Output:  data/VendorMaster.csv  (overwrites in place; backup written first)

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2026 — Phase 2 SAL enrichment Round 1 (Bob AI)
"""

import argparse
import csv
import os
import shutil
import sys
from datetime import date

# ── US state → geographic region ────────────────────────────────────────────
STATE_TO_REGION = {
    "AL": "SE", "AR": "SE", "FL": "SE", "GA": "SE", "KY": "SE",
    "LA": "SE", "MS": "SE", "NC": "SE", "SC": "SE", "TN": "SE",
    "VA": "SE", "WV": "SE",
    "CT": "NE", "DC": "NE", "DE": "NE", "MA": "NE", "MD": "NE",
    "ME": "NE", "NH": "NE", "NJ": "NE", "NY": "NE", "PA": "NE",
    "RI": "NE", "VT": "NE",
    "IA": "MW", "IL": "MW", "IN": "MW", "KS": "MW", "MI": "MW",
    "MN": "MW", "MO": "MW", "ND": "MW", "NE": "MW", "OH": "MW",
    "SD": "MW", "WI": "MW",
    "AZ": "SW", "CO": "SW", "NM": "SW", "OK": "SW", "TX": "SW",
    "UT": "SW",
    "AK": "W",  "CA": "W",  "HI": "W",  "ID": "W",  "MT": "W",
    "NV": "W",  "OR": "W",  "WA": "W",  "WY": "W",
}

DEFAULT_VM  = os.path.join("data", "VendorMaster.csv")
BACKUP_STEM = "_round1_backup"


def run(vm_path: str, dry_run: bool) -> None:
    run_date = date.today().isoformat()

    rows = []
    with open(vm_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = list(reader.fieldnames)
        for row in reader:
            rows.append(row)

    rev_filled = exp_geo_filled = 0
    unmapped_states = []

    for row in rows:
        inst_type   = row.get("instTypeID", "").strip()
        nigp_class  = row.get("instNIGPClass", "").strip()
        nigp_desc   = row.get("instNIGPDesc", "").strip()
        geo_region  = row.get("instGeoRegion", "").strip()
        vendor_state = row.get("instVendorState", "").strip().upper()

        if inst_type == "REV":
            # Rule: assign GOV-RECEIPT / GOV sentinels to all REV instruments
            row["instNIGPClass"]  = "GOV-RECEIPT"
            row["instNIGPDesc"]   = "Government Receipt \u2014 Revenue Source"
            row["instGeoRegion"]  = "GOV"
            row["instAuditTrail"] = run_date
            rev_filled += 1

        elif inst_type == "EXP" and geo_region == "":
            # Rule: derive instGeoRegion from instVendorState
            region = STATE_TO_REGION.get(vendor_state, "")
            if region:
                row["instGeoRegion"]  = region
                row["instAuditTrail"] = run_date
                exp_geo_filled += 1
            else:
                unmapped_states.append((row.get("instID"), vendor_state))

    # ── Report ──────────────────────────────────────────────────────────────
    print("=" * 70)
    print("VendorMaster Round 1 — sentinel fill")
    print(f"  Input          : {vm_path}")
    print(f"  Total rows     : {len(rows):,}")
    print(f"  REV filled     : {rev_filled:,}  (GOV-RECEIPT / GOV sentinels)")
    print(f"  EXP geo filled : {exp_geo_filled:,}  (from instVendorState)")
    if unmapped_states:
        print(f"  Unmapped states: {len(unmapped_states)}")
        for inst_id, st in unmapped_states[:10]:
            print(f"    instID={inst_id}  state={st!r}")
    print(f"  Dry run        : {dry_run}")
    print("=" * 70)

    if dry_run:
        print("--dry-run: no file written.")
        return

    # ── Backup then overwrite ───────────────────────────────────────────────
    backup_path = vm_path.replace(".csv", f"{BACKUP_STEM}.csv")
    if not os.path.exists(backup_path):
        shutil.copy2(vm_path, backup_path)
        print(f"Backup written : {backup_path}")
    else:
        print(f"Backup exists  : {backup_path}  (not overwritten)")

    with open(vm_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)

    print(f"Written        : {vm_path}  ({len(rows):,} rows)")
    print("Done.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vendormaster", default=DEFAULT_VM,
                        help=f"Path to VendorMaster.csv (default: {DEFAULT_VM})")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print changes without writing the file")
    args = parser.parse_args()

    if not os.path.exists(args.vendormaster):
        print(f"ERROR: not found: {args.vendormaster}", file=sys.stderr)
        sys.exit(1)

    run(args.vendormaster, args.dry_run)


if __name__ == "__main__":
    main()
