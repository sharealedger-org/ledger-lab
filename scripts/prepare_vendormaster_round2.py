#!/usr/bin/env python3
"""
prepare_vendormaster_round2.py — Add instSizeTier to VendorMaster.csv.

Rule (see docs/PHASE2_SYNTHETIC_DATA_PLAN.md §2.2):

  For each EXP instrument, compute its total 14-year spend from the Gold LDGR
  files (FY03–FY16).  Classify by percentile within NIGP class:

    MICRO   below 25th percentile of total spend within NIGP class
    SMALL   25th to 75th percentile (inclusive)
    LARGE   above 75th percentile

  REV instruments: assign sentinel "GOVT-AGENCY".

  EXP instruments with no LDGR spend and no NIGP class (the 99xxx series):
    assign "SMALL" as a neutral default and flag in instAuditTrail.

Prerequisite: prepare_vendormaster_round1.py must have been run first.

LDGR files:
  Reads all LDGR{yr}.csv files found under --gold-dir (default: auto-detected
  OneDrive LedgerLabOutputs-Gold path).  Uses ldgrIPID (instrument ID) and
  ldgrTransAmount (debit amounts only: ldgrDirVsOffsetFlg == "D").

Output: data/VendorMaster.csv (overwrites in place; backup written first)

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2026 — Phase 2 SAL enrichment Round 2 (Bob AI)
"""

import argparse
import csv
import glob
import os
import shutil
import sys
from collections import defaultdict
from datetime import date


DEFAULT_VM   = os.path.join("data", "VendorMaster.csv")
BACKUP_STEM  = "_round2_backup"

# Try to find the OneDrive Gold path automatically
def _find_gold_dir() -> str:
    onedrive_root = os.path.expanduser(
        "~/Library/CloudStorage"
    )
    if os.path.isdir(onedrive_root):
        for entry in sorted(os.listdir(onedrive_root)):
            candidate = os.path.join(onedrive_root, entry, "LedgerLabOutputs-Gold")
            if os.path.isdir(candidate):
                return candidate
    return ""


def collect_spend(gold_dir: str) -> dict[str, float]:
    """Return dict instID → total debit spend across all LDGR files."""
    spend: dict[str, float] = defaultdict(float)
    ldgr_files = sorted(
        glob.glob(os.path.join(gold_dir, "FY*", "LDGR*.csv"))
    )
    if not ldgr_files:
        raise FileNotFoundError(f"No LDGR*.csv files found under {gold_dir}/FY*/")

    print(f"Reading {len(ldgr_files)} LDGR files from {gold_dir} ...")
    for path in ldgr_files:
        yr_label = os.path.basename(os.path.dirname(path))
        print(f"  {yr_label}: {os.path.basename(path)}", end=" ", flush=True)
        rows_read = rows_used = 0
        with open(path, newline="", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                rows_read += 1
                # EXP instruments only: debit direction (ldgrDirVsOffsetFlg == "D")
                if row.get("ldgrDirVsOffsetFlg", "").strip() != "D":
                    continue
                # LDGR uses 10-digit zero-padded IDs (e.g. "0000000088");
                # VendorMaster instID is unpadded (e.g. "88") — strip leading zeros.
                raw_id = row.get("ldgrIPID", "").strip()
                inst_id = raw_id.lstrip("0") or "0"
                if not inst_id or inst_id == "0":
                    continue
                try:
                    amount = float(row.get("ldgrTransAmount", "0") or "0")
                except ValueError:
                    continue
                if amount > 0:
                    spend[inst_id] += amount
                    rows_used += 1
        print(f"rows={rows_read:,}  debit_rows={rows_used:,}")

    print(f"Total instruments with spend: {len(spend):,}")
    return spend


def percentile_tiers(values: list[float]) -> tuple[float, float]:
    """Return (p25, p75) of a sorted list of values."""
    if not values:
        return 0.0, 0.0
    s = sorted(values)
    n = len(s)
    # linear interpolation
    def pct(p: float) -> float:
        idx = p * (n - 1)
        lo = int(idx)
        hi = min(lo + 1, n - 1)
        return s[lo] + (s[hi] - s[lo]) * (idx - lo)
    return pct(0.25), pct(0.75)


def run(vm_path: str, gold_dir: str, dry_run: bool) -> None:
    run_date = date.today().isoformat()

    # ── Load VendorMaster ───────────────────────────────────────────────────
    rows = []
    with open(vm_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = list(reader.fieldnames)
        for row in reader:
            rows.append(row)

    # Add instSizeTier column if not present
    if "instSizeTier" not in fieldnames:
        fieldnames.append("instSizeTier")
        for row in rows:
            row["instSizeTier"] = ""

    # ── Collect spend from Gold LDGR ────────────────────────────────────────
    spend = collect_spend(gold_dir)

    # ── Build NIGP-class spend lists ────────────────────────────────────────
    nigp_spend: dict[str, list[float]] = defaultdict(list)
    for row in rows:
        if row.get("instTypeID", "").strip() != "EXP":
            continue
        inst_id    = row.get("instID", "").strip()
        nigp_class = row.get("instNIGPClass", "").strip()
        if nigp_class and nigp_class != "GOV-RECEIPT":
            total = spend.get(inst_id, 0.0)
            nigp_spend[nigp_class].append(total)

    # ── Compute per-class thresholds ────────────────────────────────────────
    thresholds: dict[str, tuple[float, float]] = {}
    for nigp, vals in nigp_spend.items():
        thresholds[nigp] = percentile_tiers(vals)

    # ── Assign tiers ────────────────────────────────────────────────────────
    tier_counts: dict[str, int] = defaultdict(int)
    for row in rows:
        inst_type  = row.get("instTypeID", "").strip()
        inst_id    = row.get("instID", "").strip()
        nigp_class = row.get("instNIGPClass", "").strip()

        if inst_type == "REV":
            row["instSizeTier"] = "GOVT-AGENCY"
            row["instAuditTrail"] = run_date
            tier_counts["GOVT-AGENCY"] += 1

        elif inst_type == "EXP":
            total = spend.get(inst_id, 0.0)
            if nigp_class and nigp_class != "GOV-RECEIPT" and nigp_class in thresholds:
                p25, p75 = thresholds[nigp_class]
                if total < p25:
                    tier = "MICRO"
                elif total <= p75:
                    tier = "SMALL"
                else:
                    tier = "LARGE"
            else:
                # No NIGP class (99xxx instruments) or no thresholds: neutral default
                tier = "SMALL"
            row["instSizeTier"] = tier
            row["instAuditTrail"] = run_date
            tier_counts[tier] += 1

    # ── Report ──────────────────────────────────────────────────────────────
    print("\n" + "=" * 70)
    print("VendorMaster Round 2 — size tier")
    print(f"  Input          : {vm_path}")
    print(f"  Gold dir       : {gold_dir}")
    print(f"  Total rows     : {len(rows):,}")
    for t in ["MICRO", "SMALL", "LARGE", "GOVT-AGENCY"]:
        print(f"  {t:<12}  : {tier_counts[t]:,}")
    print(f"  NIGP classes   : {len(thresholds):,}")
    print(f"  Dry run        : {dry_run}")
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

    print(f"Written        : {vm_path}  ({len(rows):,} rows)")
    print("Done.")


def main() -> None:
    default_gold = _find_gold_dir()
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vendormaster", default=DEFAULT_VM)
    parser.add_argument(
        "--gold-dir", default=default_gold,
        help=f"Path to LedgerLabOutputs-Gold (auto-detected: {default_gold or 'not found'})"
    )
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not os.path.exists(args.vendormaster):
        print(f"ERROR: not found: {args.vendormaster}", file=sys.stderr)
        sys.exit(1)
    if not args.gold_dir or not os.path.isdir(args.gold_dir):
        print(f"ERROR: Gold dir not found: {args.gold_dir!r}  "
              f"-- pass --gold-dir explicitly", file=sys.stderr)
        sys.exit(1)

    run(args.vendormaster, args.gold_dir, args.dry_run)


if __name__ == "__main__":
    main()
