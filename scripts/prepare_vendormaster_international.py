#!/usr/bin/env python3
"""
prepare_vendormaster_international.py — Add instFXCurrency, instFederalCFDA,
and instFederalAgency to VendorMaster.csv (Round A5a + A5b).

Rules (see docs/PHASE2_SYNTHETIC_DATA_PLAN.md §2.5):

instFXCurrency:
  instGeoRegion == "INT"       → AUD  (HALLPRINT PTY LTD, Australian publisher)
  all other EXP / REV          → USD

instFederalCFDA and instFederalAgency (REV instruments only):
  instHolderName (the fund-code number) is not a CFDA code — REV instruments
  are keyed by fund code, not source code.  This field is set to the declared
  source class prefix when the fund code maps to a known federal-grant fund,
  or left blank pending the ACFR Strand B enrichment that will join apa_revenue
  source codes to REV instruments.

  For this initial pass, all REV instruments:
    instFederalCFDA   → ""   (populated later from apa_revenue source enrichment)
    instFederalAgency → ""   (populated later)
    instGeoRegion     → "FED" only for instHolderName values in KNOWN_FEDERAL_FUNDS
                        (the Federal Trust Fund, Transportation Trust Fund codes);
                        all other REV keep "GOV" from Round 1.

  KNOWN_FEDERAL_FUNDS: fund codes whose majority revenue is from federal grants
  (class 10–99).  These are populated here as a named set; the full fund→source
  linkage comes from the apa_revenue join in Strand B.

Prerequisite: Rounds 1–4 must have been run first.

Output: data/VendorMaster.csv (overwrites in place; backup written first)

Also writes: data/FX_RATES.csv (synthetic AUD/USD quarterly rates FY2003–FY2016)
  Rate source: synthetic rates constructed from published RBA annual averages.
  Format: currency_pair,period,rate  (e.g. AUD/USD,2003-01,0.5500)
  DECLARED SYNTHETIC: these are approximations for engine demonstration purposes;
  the architecture demonstration does not require precision in the rate values.

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2026 — Phase 2 SAL enrichment Round A5a/A5b (Bob AI)
"""

import argparse
import csv
import os
import shutil
import sys
from datetime import date

DEFAULT_VM  = os.path.join("data", "VendorMaster.csv")
FX_RATES_PATH = os.path.join("data", "FX_RATES.csv")
BACKUP_STEM = "_international_backup"

# Fund codes known to be predominantly federal-grant funded
# Source: VA ACFR fund structure (Federal Trust Fund = code range 50xx–59xx)
KNOWN_FEDERAL_FUNDS = {
    "5001", "5002", "5003", "5004", "5005", "5006", "5007", "5008",
    "5101", "5102", "5103", "5104", "5105", "5201", "5202", "5301",
    "5302", "5401", "5402", "5501", "5502", "5601", "5701", "5801",
    "5901", "5902",
}

# CFDA prefix → federal agency name
CFDA_AGENCY_MAP = {
    "10": "USDA",
    "12": "Dept of Defense",
    "14": "HUD",
    "15": "Dept of Interior",
    "16": "Dept of Justice",
    "17": "Dept of Labor",
    "20": "Dept of Transportation",
    "23": "Appalachian Regional Commission",
    "43": "NASA",
    "45": "NEH/NEA",
    "47": "NSF",
    "59": "SBA",
    "64": "VA (Veterans Affairs)",
    "66": "EPA",
    "81": "Dept of Energy",
    "84": "Dept of Education",
    "93": "HHS",
    "94": "AmeriCorps",
    "97": "FEMA/DHS",
}

# Synthetic AUD/USD quarterly rates FY2003–FY2016
# Based on approximate RBA annual averages (declared synthetic for demonstration)
# Format: (calendar_year, rate_approx) — applied to all quarters of that year
_AUD_USD_ANNUAL = {
    2002: 0.5500, 2003: 0.6500, 2004: 0.7400, 2005: 0.7600,
    2006: 0.7500, 2007: 0.8400, 2008: 0.8500, 2009: 0.7900,
    2010: 0.9200, 2011: 1.0300, 2012: 1.0300, 2013: 0.9700,
    2014: 0.9000, 2015: 0.7500, 2016: 0.7400,
}


def build_fx_rates() -> list[dict]:
    """Build monthly AUD/USD rate rows for FY2003–FY2016 (Jul 2002–Jun 2016)."""
    rows = []
    for cal_year in range(2002, 2017):
        rate = _AUD_USD_ANNUAL.get(cal_year, 0.75)
        for month in range(1, 13):
            rows.append({
                "currency_pair": "AUD/USD",
                "period": f"{cal_year:04d}-{month:02d}",
                "rate": f"{rate:.4f}",
                "source": "SYNTHETIC-RBA-APPROX",
            })
    return rows


def run(vm_path: str, fx_rates_path: str, dry_run: bool) -> None:
    run_date = date.today().isoformat()

    rows = []
    with open(vm_path, newline="", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = list(reader.fieldnames)
        for row in reader:
            rows.append(row)

    new_cols = ["instFXCurrency", "instFederalCFDA", "instFederalAgency"]
    for col in new_cols:
        if col not in fieldnames:
            fieldnames.append(col)
            for row in rows:
                row[col] = ""

    counts = {"AUD": 0, "USD": 0, "FED_geo": 0}

    for row in rows:
        inst_type  = row.get("instTypeID", "").strip()
        geo_region = row.get("instGeoRegion", "").strip()
        holder     = row.get("instHolderName", "").strip()

        # FX currency
        if geo_region == "INT":
            row["instFXCurrency"] = "AUD"
            counts["AUD"] += 1
        else:
            row["instFXCurrency"] = "USD"
            counts["USD"] += 1

        # Federal enrichment (REV only, placeholder for Strand B)
        if inst_type == "REV":
            row["instFederalCFDA"]   = ""
            row["instFederalAgency"] = ""
            # Override instGeoRegion to FED for known federal-fund codes
            if holder in KNOWN_FEDERAL_FUNDS and geo_region == "GOV":
                row["instGeoRegion"] = "FED"
                counts["FED_geo"] += 1

        row["instAuditTrail"] = run_date

    # ── Build FX rate table ──────────────────────────────────────────────────
    fx_rows = build_fx_rates()

    print("=" * 70)
    print("VendorMaster Round A5a/A5b — FX currency + federal enrichment")
    print(f"  Input          : {vm_path}  ({len(rows):,} rows)")
    print(f"  instFXCurrency = AUD : {counts['AUD']:,}  (INT region)")
    print(f"  instFXCurrency = USD : {counts['USD']:,}  (all others)")
    print(f"  FED geo overrides    : {counts['FED_geo']:,}  (known federal funds)")
    print(f"  FX rate rows         : {len(fx_rows):,}  (AUD/USD monthly FY2003-2016)")
    print(f"  FX rates path        : {fx_rates_path}")
    print(f"  Dry run              : {dry_run}")
    print("=" * 70)

    if dry_run:
        print("--dry-run: no files written.")
        return

    # Backup and write VendorMaster
    backup_path = vm_path.replace(".csv", f"{BACKUP_STEM}.csv")
    if not os.path.exists(backup_path):
        shutil.copy2(vm_path, backup_path)
        print(f"Backup written : {backup_path}")

    with open(vm_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)
    print(f"Written        : {vm_path}  ({len(rows):,} rows)")

    # Write FX rates
    fx_fields = ["currency_pair", "period", "rate", "source"]
    with open(fx_rates_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fx_fields)
        writer.writeheader()
        writer.writerows(fx_rows)
    print(f"Written        : {fx_rates_path}  ({len(fx_rows):,} rows)")
    print("Done.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--vendormaster", default=DEFAULT_VM)
    parser.add_argument("--fx-rates", default=FX_RATES_PATH)
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()

    if not os.path.exists(args.vendormaster):
        print(f"ERROR: not found: {args.vendormaster}", file=sys.stderr)
        sys.exit(1)

    run(args.vendormaster, args.fx_rates, args.dry_run)


if __name__ == "__main__":
    main()
