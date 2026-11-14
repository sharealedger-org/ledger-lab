#!/usr/bin/env python3
"""
prepare_vendormaster_rounds3_4.py — Add instContractType, instPaymentChannel
(Round 3) and instCounterpartySector, instExposureType, instMaturityProxy,
instRiskWeightProxy (Round 4) to VendorMaster.csv.

Rules (see docs/PHASE2_SYNTHETIC_DATA_PLAN.md §2.3–2.4):

Round 3 — Contract type and payment channel
  instContractType:
    NIGP class 918                    → INTERAGENCY
    NIGP class 001–899 (all EXP)      → CONTRACTED (services) or PURCHASE-ORDER
       if NIGP class is in GOODS_CLASSES → PURCHASE-ORDER, else → CONTRACTED
    REV instruments                   → RECEIPT

  instPaymentChannel (derived from 14-year median annual spend from Round 2 tier):
    instSizeTier == LARGE             → WIRE
    instSizeTier == SMALL             → EFT
    instSizeTier == MICRO             → CHECK
    REV instruments                   → ACH
    (Fallback for no tier: CHECK)

Round 4 — Basel bridge attributes
  instCounterpartySector:
    GOV-RECEIPT / NIGP prefix → see SECTOR_MAP below
  instExposureType:
    EXP → OPERATING-EXP (unless capital NIGP class → CAPITAL-EXP)
    REV → REVENUE
  instMaturityProxy:
    INTERAGENCY → PERPETUAL; PURCHASE-ORDER → SHORT-TERM;
    CONTRACTED (capital) → LONG-TERM; CONTRACTED (other) → MEDIUM-TERM
  instRiskWeightProxy (Basel standardized approach):
    GOVERNMENT  → 0%
    FINANCIAL   → 20%
    NONPROFIT   → 50%
    CORPORATE   → 100%
    INDIVIDUAL  → 75%
    UNKNOWN     → 100%  (conservative)

Prerequisite: Rounds 1 and 2 must have been run first.

Output: data/VendorMaster.csv (overwrites in place; backup written first)

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2026 — Phase 2 SAL enrichment Rounds 3 and 4 (Bob AI)
"""

import argparse
import csv
import os
import shutil
import sys
from datetime import date

DEFAULT_VM  = os.path.join("data", "VendorMaster.csv")
BACKUP_STEM = "_rounds34_backup"

# NIGP 3-digit classes that represent goods (physical products)
GOODS_CLASSES = {
    "020", "035", "040", "045", "055", "060", "065", "070", "075",
    "080", "085", "090", "105", "110", "115", "120", "125", "130",
    "140", "145", "150", "155", "160", "165", "170", "175", "180",
    "185", "190", "195", "200", "205", "210", "215", "220", "225",
    "230", "235", "240", "245", "250", "255", "260", "265", "270",
    "275", "280", "285", "290", "295", "300", "305", "310", "315",
    "320", "325", "330", "335", "340", "345", "350", "355", "360",
    "365", "370", "375", "380", "385", "390", "395", "400", "405",
    "410", "415", "420", "425", "430", "435", "440", "445", "450",
    "455", "460", "465", "470", "475", "480", "485", "490", "495",
    "500", "505", "510", "515", "520", "525", "530", "535", "540",
    "545", "550", "555", "560", "565", "570", "575", "580", "585",
    "590", "595", "600", "605", "610", "615", "620", "625", "630",
    "635", "640", "645", "650", "655", "660", "665", "670", "675",
    "680", "685", "690", "695", "700", "705", "710", "715", "720",
    "725", "730", "735", "740", "745", "750", "755", "760", "765",
    "770", "775", "780", "785", "790", "795", "800", "805", "810",
    "815", "820", "825", "830", "835", "840", "845", "850", "855",
    "860", "865", "870", "875", "880", "885", "890", "895",
}

# Capital-expenditure NIGP classes (approximation; true capital from object codes)
CAPITAL_CLASSES = {"018", "019", "021", "022", "023", "024", "025", "026",
                   "028", "029", "031"}

# NIGP 3-digit prefix → counterparty sector
# Grouped by dominant economic category observed in VA procurement data
SECTOR_MAP: dict[str, str] = {}

# Federal/government-to-government
for c in ["918"]:           # Interagency
    SECTOR_MAP[c] = "GOVERNMENT"

# Financial services
for c in ["945", "950", "955", "946", "947"]:
    SECTOR_MAP[c] = "FINANCIAL"

# Nonprofit / education / professional associations
for c in ["030", "031", "032", "033", "952", "953"]:
    SECTOR_MAP[c] = "NONPROFIT"

# Individual / staffing
for c in ["961", "962", "963", "964", "965"]:
    SECTOR_MAP[c] = "INDIVIDUAL"

# Default for unrecognized prefixes → CORPORATE
_DEFAULT_SECTOR = "CORPORATE"

# Risk weights by sector (Basel standardized approach)
RISK_WEIGHT = {
    "GOVERNMENT": "0%",
    "FINANCIAL":  "20%",
    "NONPROFIT":  "50%",
    "CORPORATE":  "100%",
    "INDIVIDUAL": "75%",
    "UNKNOWN":    "100%",
}


def classify_contract_type(inst_type: str, nigp_class: str) -> str:
    if inst_type == "REV":
        return "RECEIPT"
    if nigp_class == "918":
        return "INTERAGENCY"
    if nigp_class in GOODS_CLASSES:
        return "PURCHASE-ORDER"
    return "CONTRACTED"


def classify_payment_channel(inst_type: str, size_tier: str) -> str:
    if inst_type == "REV":
        return "ACH"
    if size_tier == "LARGE":
        return "WIRE"
    if size_tier == "SMALL":
        return "EFT"
    return "CHECK"


def classify_counterparty_sector(inst_type: str, nigp_class: str) -> str:
    if inst_type == "REV" or nigp_class == "GOV-RECEIPT":
        return "GOVERNMENT"
    return SECTOR_MAP.get(nigp_class, _DEFAULT_SECTOR)


def classify_exposure_type(inst_type: str, nigp_class: str) -> str:
    if inst_type == "REV":
        return "REVENUE"
    if nigp_class in CAPITAL_CLASSES:
        return "CAPITAL-EXP"
    return "OPERATING-EXP"


def classify_maturity(contract_type: str, exposure_type: str) -> str:
    if contract_type == "INTERAGENCY":
        return "PERPETUAL"
    if contract_type == "PURCHASE-ORDER":
        return "SHORT-TERM"
    if exposure_type == "CAPITAL-EXP":
        return "LONG-TERM"
    if contract_type == "RECEIPT":
        return "PERPETUAL"
    return "MEDIUM-TERM"


def run(vm_path: str, dry_run: bool) -> None:
    run_date = date.today().isoformat()

    rows = []
    with open(vm_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = list(reader.fieldnames)
        for row in reader:
            rows.append(row)

    # Add new columns if absent
    new_cols = [
        "instContractType", "instPaymentChannel",
        "instCounterpartySector", "instExposureType",
        "instMaturityProxy", "instRiskWeightProxy",
    ]
    for col in new_cols:
        if col not in fieldnames:
            fieldnames.append(col)
            for row in rows:
                row[col] = ""

    counts: dict[str, dict[str, int]] = {c: {} for c in new_cols}

    for row in rows:
        inst_type  = row.get("instTypeID", "").strip()
        nigp_class = row.get("instNIGPClass", "").strip()
        size_tier  = row.get("instSizeTier", "").strip()

        ct = classify_contract_type(inst_type, nigp_class)
        pc = classify_payment_channel(inst_type, size_tier)
        cs = classify_counterparty_sector(inst_type, nigp_class)
        et = classify_exposure_type(inst_type, nigp_class)
        mp = classify_maturity(ct, et)
        rw = RISK_WEIGHT.get(cs, "100%")

        row["instContractType"]       = ct
        row["instPaymentChannel"]     = pc
        row["instCounterpartySector"] = cs
        row["instExposureType"]       = et
        row["instMaturityProxy"]      = mp
        row["instRiskWeightProxy"]    = rw
        row["instAuditTrail"]         = run_date

        for col, val in [("instContractType", ct), ("instPaymentChannel", pc),
                         ("instCounterpartySector", cs), ("instExposureType", et),
                         ("instMaturityProxy", mp), ("instRiskWeightProxy", rw)]:
            counts[col][val] = counts[col].get(val, 0) + 1

    print("=" * 70)
    print("VendorMaster Rounds 3 & 4 — contract/channel/Basel bridge")
    print(f"  Input : {vm_path}  ({len(rows):,} rows)")
    for col in new_cols:
        print(f"\n  {col}:")
        for val, n in sorted(counts[col].items()):
            print(f"    {val:<20} {n:,}")
    print(f"\n  Dry run : {dry_run}")
    print("=" * 70)

    if dry_run:
        print("--dry-run: no file written.")
        return

    backup_path = vm_path.replace(".csv", f"{BACKUP_STEM}.csv")
    if not os.path.exists(backup_path):
        shutil.copy2(vm_path, backup_path)
        print(f"Backup written : {backup_path}")

    with open(vm_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)

    print(f"Written : {vm_path}  ({len(rows):,} rows)")
    print("Done.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vendormaster", default=DEFAULT_VM)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not os.path.exists(args.vendormaster):
        print(f"ERROR: not found: {args.vendormaster}", file=sys.stderr)
        sys.exit(1)

    run(args.vendormaster, args.dry_run)


if __name__ == "__main__":
    main()
