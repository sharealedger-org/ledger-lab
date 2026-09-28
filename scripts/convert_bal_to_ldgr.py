#!/usr/bin/env python3
"""
convert_bal_to_ldgr.py — Convert B### balance shards to LDGR{year}.csv

GenevaERS CKB discipline: one pass, all shards, inline column remap, no intermediate files.
Writes a single LDGR{year}.csv per fiscal year, ready for dataAggregation.scala (Step 5).

B### column layout (19 cols, no header, comma-delimited):
  0  ldgrIPID               (zero-padded 10-digit instrument ID)
  1  ldgrLdgrlID            (sequential ledger record ID)
  2  ldgrLedgerID           (e.g. ACTUALS)
  3  ldgrJrnlType           (e.g. FIN)
  4  ldgrBookCodeID         (e.g. SHRD-3RD-PARTY)
  5  ldgrLegalEntityID      (agency code)
  6  ldgrCenterID
  7  ldgrProjectID
  8  ldgrProductID
  9  ldgrNominalAccountID   (e.g. EXP151)
  10 ldgrCurrencyCodeSourceID     (USD)
  11 ldgrCurrencyTypeCodeSourceID (TXN)
  12 ldgrCurrencyCodeTargetID     (USD)
  13 ldgrCurrencyTypeCodeTargetID (BASE-LE)
  14 ldgrLedgerPeriod       (fiscal year, e.g. 2003)
  15 ldgrTransAmount        (balance amount)
  16 ldgrDirVsOffsetFlg     (N)
  17 ldgrReconcileFlg       (blank)
  18 ldgrAdjustFlg          (blank)

LDGR header (31 cols):
  ldgrIPID,ldgrContractID,ldgrCommitmentID,ldgrLdgrlID,ldgrSourceSystemID,
  ldgrLedgerID,ldgrJrnlType,ldgrBookCodeID,ldgrLegalEntityID,ldgrCenterID,
  ldgrProjectID,ldgrProductID,ldgrNominalAccountID,ldgrAltAccountID,
  ldgrCurrencyCodeSourceID,ldgrCurrencyTypeCodeSourceID,
  ldgrCurrencyCodeTargetID,ldgrCurrencyTypeCodeTargetID,
  ldgrLedgerPeriod,ldgrTransAmount,ldgrUnitOfMeasure,ldgrStatisticAmount,
  ldgrDirVsOffsetFlg,ldgrReconcileFlg,ldgrAdjustFlg,
  ldgrExtensionIDAuditTrail,ldgrExtensionIDSource,ldgrExtensionIDClass,
  ldgrExtensionIDDates,ldgrExtensionIDCustom, (trailing comma in original)

Usage:
  python scripts/convert_bal_to_ldgr.py --baldir data/ParallelProcessRunSept9 --outdir data/output --years 2003
  python scripts/convert_bal_to_ldgr.py --baldir data/ParallelProcessRunSept9 --outdir data/output
"""

import argparse
import csv
import os
import sys
from collections import defaultdict

LDGR_HEADER = (
    "ldgrIPID,ldgrContractID,ldgrCommitmentID,ldgrLdgrlID,ldgrSourceSystemID,"
    "ldgrLedgerID,ldgrJrnlType,ldgrBookCodeID,ldgrLegalEntityID,ldgrCenterID,"
    "ldgrProjectID,ldgrProductID,ldgrNominalAccountID,ldgrAltAccountID,"
    "ldgrCurrencyCodeSourceID,ldgrCurrencyTypeCodeSourceID,"
    "ldgrCurrencyCodeTargetID,ldgrCurrencyTypeCodeTargetID,"
    "ldgrLedgerPeriod,ldgrTransAmount,ldgrUnitOfMeasure,ldgrStatisticAmount,"
    "ldgrDirVsOffsetFlg,ldgrReconcileFlg,ldgrAdjustFlg,"
    "ldgrExtensionIDAuditTrail,ldgrExtensionIDSource,ldgrExtensionIDClass,"
    "ldgrExtensionIDDates,ldgrExtensionIDCustom,"
)

def bal_to_ldgr_row(cols):
    """Remap 19 B### columns to 31-column LDGR CSV row. Returns CSV string."""
    if len(cols) < 19:
        return None
    # fmt: off
    return ",".join([
        cols[0].strip(),   # 0  ldgrIPID
        "",                # 1  ldgrContractID
        "",                # 2  ldgrCommitmentID
        cols[1].strip(),   # 3  ldgrLdgrlID
        "",                # 4  ldgrSourceSystemID
        cols[2].strip(),   # 5  ldgrLedgerID
        cols[3].strip(),   # 6  ldgrJrnlType
        cols[4].strip(),   # 7  ldgrBookCodeID
        cols[5].strip(),   # 8  ldgrLegalEntityID
        cols[6].strip(),   # 9  ldgrCenterID
        cols[7].strip(),   # 10 ldgrProjectID
        cols[8].strip(),   # 11 ldgrProductID
        cols[9].strip(),   # 12 ldgrNominalAccountID
        "",                # 13 ldgrAltAccountID
        cols[10].strip(),  # 14 ldgrCurrencyCodeSourceID
        cols[11].strip(),  # 15 ldgrCurrencyTypeCodeSourceID
        cols[12].strip(),  # 16 ldgrCurrencyCodeTargetID
        cols[13].strip(),  # 17 ldgrCurrencyTypeCodeTargetID
        cols[14].strip(),  # 18 ldgrLedgerPeriod
        cols[15].strip(),  # 19 ldgrTransAmount
        "",                # 20 ldgrUnitOfMeasure
        "0",               # 21 ldgrStatisticAmount
        cols[16].strip(),  # 22 ldgrDirVsOffsetFlg
        cols[17].strip(),  # 23 ldgrReconcileFlg
        cols[18].strip(),  # 24 ldgrAdjustFlg
        "",                # 25 ldgrExtensionIDAuditTrail
        "",                # 26 ldgrExtensionIDSource
        "",                # 27 ldgrExtensionIDClass
        "",                # 28 ldgrExtensionIDDates
        "",                # 29 ldgrExtensionIDCustom
        "",                # 30 trailing (matches Scala ldgrHeader trailing comma)
    ])
    # fmt: on


def main():
    parser = argparse.ArgumentParser(description="Convert B### shards to LDGR{year}.csv")
    parser.add_argument("--baldir", default="data/ParallelProcessRunSept9",
                        help="Directory containing B###bal.txtInstID#.txt files")
    parser.add_argument("--outdir", default="data/output",
                        help="Output directory for LDGR{year}.csv files")
    parser.add_argument("--years", default="",
                        help="Comma-separated 4-digit years to process (e.g. 2003,2004). Default: all found.")
    parser.add_argument("--dry-run", action="store_true",
                        help="Print control totals only; do not write output files")
    args = parser.parse_args()

    os.makedirs(args.outdir, exist_ok=True)

    # Discover all B### shard files grouped by fiscal year
    # Filename pattern: B{YY+100}bal.txtInstID{N}.txt  e.g. B103bal.txtInstID0.txt = FY2003
    year_shards = defaultdict(list)
    for fname in sorted(os.listdir(args.baldir)):
        if fname.startswith("B") and "bal.txtInstID" in fname and fname.endswith(".txt"):
            # Extract YY from B{103}bal... → 103 - 100 = 03 → 2003
            try:
                seq = int(fname[1:4])
                year = str(2000 + (seq - 100))
                year_shards[year].append(os.path.join(args.baldir, fname))
            except ValueError:
                pass

    if args.years:
        filter_years = set(args.years.split(","))
        year_shards = {y: v for y, v in year_shards.items() if y in filter_years}

    if not year_shards:
        print("No B### files found.", file=sys.stderr)
        sys.exit(1)

    print(f"Found {len(year_shards)} fiscal year(s): {sorted(year_shards)}")

    for year in sorted(year_shards):
        shards = sorted(year_shards[year])
        out_path = os.path.join(args.outdir, f"LDGR{year}.csv")
        rows_read = 0
        rows_written = 0
        rows_skipped = 0

        if args.dry_run:
            out = None
        else:
            out = open(out_path, "w", newline="", encoding="utf-8")
            out.write(LDGR_HEADER + "\n")

        for shard in shards:
            with open(shard, "r", encoding="utf-8", errors="replace") as f:
                for line in f:
                    line = line.rstrip("\n\r")
                    if not line.strip():
                        continue
                    cols = line.split(",")
                    rows_read += 1
                    ldgr_row = bal_to_ldgr_row(cols)
                    if ldgr_row is None:
                        rows_skipped += 1
                        continue
                    if out:
                        out.write(ldgr_row + "\n")
                    rows_written += 1

        if out:
            out.close()
            size_kb = round(os.path.getsize(out_path) / 1024, 1)
            print(f"  FY{year}: {rows_read:,} rows read, {rows_written:,} written -> {out_path} ({size_kb} KB)")
        else:
            print(f"  FY{year}: {rows_read:,} rows read, {rows_written:,} would write [dry-run]")

    print("Done.")


if __name__ == "__main__":
    main()
