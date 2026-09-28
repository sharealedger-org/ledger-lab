#!/usr/bin/env python3
"""
standardize_and_sort.py — Step 2: vendor-keyed SortedJE production
                          with instrument ID assignment from VendorMaster.

WHAT THIS DOES
--------------
The Scala transStandardize (Step 2) aggregates expense transactions by
(agency × fund × obj × program), losing the vendor name.  This means every
journal entry gets transIPID = "0" — the instrument dimension is gone before
posting, so the LDGR has no ldgrIPID values.

This script does the same job correctly:
  1. Reads raw VA expenditure file(s) (tab-delimited FY03–FY16 formats A/B/C).
  2. Looks up UPPER(vendor_name) → instID in VendorMaster (O(1) per row).
  3. Emits two journal lines per transaction (debit + credit) keyed on instID.
  4. Sorts the output on the full posting key using an external merge-sort
     (chunk → sort → spill → merge) so RAM usage is O(chunk_size), not O(N).
  5. Writes SortedJE{year}.csv directly — no intermediate unsorted JE file.

The output is a drop-in replacement for the Scala Step 2 + sortJE output.
post.scala (Step 3) reads it unchanged.

EXTERNAL MERGE-SORT DESIGN
---------------------------
Input rows are processed in fixed-size chunks (default 200 000 rows ≈ 60–80 MB
of journal output).  Each chunk is sorted in memory and spilled to a temp file.
After all chunks are spilled, a k-way merge pass reads one line at a time from
each spill file, always advancing the smallest key.  Peak RAM = one chunk +
one row per spill file — constant regardless of total input size.

USAGE
-----
  python scripts/standardize_and_sort.py \
      --infile  data/VARawFiles/FY03q1exp.txt \
      --outfile data/output/SortedJE2003q1.csv \
      --vendormaster data/VendorMaster_full.csv \
      --year 2003 --quarter 1 --type E

  python scripts/standardize_and_sort.py \
      --infile  data/VARawFiles/FY14q1exp.csv \
      --outfile data/output/SortedJE2014q1.csv \
      --year 2014 --quarter 1 --type E

ARGUMENTS
---------
  --infile        Path to raw VA expenditure/revenue file (tab-delimited or CSV)
  --outfile       Output SortedJE CSV path
  --vendormaster  Path to VendorMaster CSV (default: data/VendorMaster_full.csv)
  --year          4-digit fiscal year (e.g. 2003)
  --quarter       Fiscal quarter 1–4 (expense) or 5 (revenue)
  --type          E=Expense, R=Revenue (default: E)
  --recformat     A/B/C schema variant (default: auto-detect from year)
  --chunk-size    Rows per sort chunk (default: 200000; reduce if RAM-constrained)
  --dry-run       Print control totals without writing output

RECORD FORMATS
--------------
  A  FY03–FY11 expense:  AGY, FUND, OBJ, PROG, VENDOR_NAME, AMOUNT        (tab)
  B  FY12–FY13 expense:  AGY, FUND, OBJ, PROG, VENDOR_NAME, AMOUNT, DATE  (tab)
  C  FY14–FY16 expense:  AGY, AMOUNT, FUND, OBJ, PROG, VENDOR_NAME, DATE  (csv)
  RA FY03–FY13 revenue:  AGY, FUND, SRC, AMOUNT                           (tab)
  RC FY14–FY16 revenue:  AGY, AMOUNT, FUND, SRC, DATE                     (csv)

SORTEDJE COLUMN ORDER (39 columns, matches Transaction.scala):
  0  transIPID              ← instID from VendorMaster lookup
  1  transContractID
  2  transCommitmentID
  3  transJrnlID
  4  transJrnlLineID
  5  transBusinessEventCode
  6  transSourceSystemID
  7  transOriginalDocID
  8  transJrnlDescript
  9  transLedgerID
  10 transJrnlType
  11 transBookCodeID
  12 transLegalEntityID     ← agency
  13 transCenterID          ← fund
  14 transProjectID         ← object
  15 transProductID
  16 transNominalAccountID  ← EXP{prog} or REV{src} or 0000 (offset)
  17 transAltAccountID
  18 transCurrencyCodeSourceID
  19 transCurrencyTypeCodeSourceID
  20 transCurrencyCodeTargetID
  21 transCurrencyTypeCodeTargetID
  22 transFiscalPeriod      ← year
  23 transAcctDate          ← derived from quarter
  24 transTransDate         ← same as acctDate (or file DATE col if present)
  25 transTransAmount
  26 transUnitOfMeasure
  27 transUnitPrice
  28 transStatisticAmount
  29 tranRuleSetID
  30 transRuleID
  31 transDirVsOffsetFlg
  32 transReconcileFlg
  33 transAdjustFlg
  34 transExtensionIDAuditTrail
  35 transExtensionIDSource
  36 transExtensionIDClass
  37 transExtensionIDDates
  38 transExtensionIDCustom

SORT KEY (matches post.scala's testJrnlFullKey):
  transIPID + transLedgerID + transJrnlType + transBookCodeID +
  transLegalEntityID + transCenterID + transProjectID + transProductID +
  transNominalAccountID + transAltAccountID + transCurrencyCodeSourceID +
  transCurrencyTypeCodeSourceID + transCurrencyCodeTargetID +
  transCurrencyTypeCodeTargetID + transFiscalPeriod

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2025 — Python implementation replacing Scala Step 2 + sortJE (Bob AI)
       External merge-sort replaces in-memory sort for O(1) RAM scaling
"""

import argparse
import csv
import heapq
import io
import os
import sys
import tempfile
import time


# ── Constants matching Transaction.scala defaults ────────────────────────────
LEDGER_ID      = "ACTUALS"
JRNL_TYPE      = "FIN"
BOOK_CODE      = "SHRD-3RD-PARTY"
PRODUCT_ID     = "0"
ALT_ACCOUNT    = "0"
CCY_SRC        = "USD"
CCY_TYPE_SRC   = "TXN"
CCY_TGT        = "USD"
CCY_TYPE_TGT   = "BASE-LE"
UOM            = " "
UNIT_PRICE     = "0"
STAT_AMT       = "0"
RULE_SET       = " "
RULE_ID        = " "
DIR_OFFSET     = "D"
RECONCILE      = "N"
ADJUST         = "N"
EXT_AUDIT      = " "
EXT_SRC        = " "
EXT_CLASS      = " "
EXT_DATES      = " "
EXT_CUSTOM     = " "

# Sort key column indices (0-based) in the 39-element row
# transIPID(0) + transLedgerID(9) + transJrnlType(10) + transBookCodeID(11)
# + transLegalEntityID(12) + transCenterID(13) + transProjectID(14)
# + transProductID(15) + transNominalAccountID(16) + transAltAccountID(17)
# + transCurrencyCodeSourceID(18) + transCurrencyTypeCodeSourceID(19)
# + transCurrencyCodeTargetID(20) + transCurrencyTypeCodeTargetID(21)
# + transFiscalPeriod(22)
SORT_KEY_COLS = [0, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22]

DEFAULT_CHUNK  = 200_000   # rows per spill chunk
DEFAULT_VM     = os.path.join("data", "VendorMaster_full.csv")


# ── Helpers ──────────────────────────────────────────────────────────────────

def quarter_to_date(year: str, quarter: int) -> str:
    month = quarter * 3
    return f"{year}/{month:02d}/01"


def auto_recformat(year: int, rec_type: str) -> str:
    if rec_type == "R":
        return "C" if year >= 2014 else "A"
    if year >= 2014:
        return "C"
    if year >= 2012:
        return "B"
    return "A"


def row_sort_key(row_str: str) -> str:
    """Compute sort key from a comma-joined row string without re-splitting all 39 cols."""
    # Fast path: split only to the columns we need (max index = 22)
    cols = row_str.split(",", 23)
    return "".join(cols[i] for i in SORT_KEY_COLS)


def load_vendormaster(path: str) -> dict:
    """
    Return dict: UPPER(instHolderName) → instID (zero-padded 10-digit string).
    Accepts VendorMaster.csv (many columns) and VendorMaster_full.csv (4 cols).
    """
    vm = {}
    fallback = path
    # Try the given path; fall back to VendorMaster_full.csv then VendorMaster.csv
    for candidate in [path, DEFAULT_VM, os.path.join("data", "VendorMaster.csv")]:
        if os.path.exists(candidate):
            fallback = candidate
            break

    try:
        with open(fallback, newline="", encoding="utf-8-sig") as f:
            lines = [l for l in f if not l.startswith("#")]
        reader = csv.DictReader(lines)
        for row in reader:
            name = row.get("instHolderName", "").strip().upper().strip('"')
            inst_id = row.get("instID", "").strip().strip('"')
            if name and inst_id:
                try:
                    vm[name] = str(int(inst_id)).zfill(10)
                except ValueError:
                    vm[name] = inst_id
    except FileNotFoundError:
        print(f"WARNING: VendorMaster not found at {fallback} — instIDs will be 0000000000",
              file=sys.stderr)
    print(f"VendorMaster loaded: {len(vm):,} entries from {fallback}")
    return vm


# ── Row builder ──────────────────────────────────────────────────────────────

def make_row_str(inst_id, jrnl_id, line_id, biz_event, src_sys, descript,
                 agency, fund, obj_code, nominal_acct, year, acct_date,
                 trans_date, amount) -> str:
    """Build one comma-joined journal line (no quoting — matches post.scala reader)."""
    # Sanitise any commas in free-text fields
    descript_clean = str(descript).replace(",", " ")
    return ",".join([
        inst_id,          # 0  transIPID
        "0",              # 1  transContractID
        "0",              # 2  transCommitmentID
        jrnl_id,          # 3  transJrnlID
        line_id,          # 4  transJrnlLineID
        biz_event,        # 5  transBusinessEventCode
        src_sys,          # 6  transSourceSystemID
        "Unknown",        # 7  transOriginalDocID
        descript_clean,   # 8  transJrnlDescript
        LEDGER_ID,        # 9  transLedgerID
        JRNL_TYPE,        # 10 transJrnlType
        BOOK_CODE,        # 11 transBookCodeID
        agency,           # 12 transLegalEntityID
        fund,             # 13 transCenterID
        obj_code,         # 14 transProjectID
        PRODUCT_ID,       # 15 transProductID
        nominal_acct,     # 16 transNominalAccountID
        ALT_ACCOUNT,      # 17 transAltAccountID
        CCY_SRC,          # 18 transCurrencyCodeSourceID
        CCY_TYPE_SRC,     # 19 transCurrencyTypeCodeSourceID
        CCY_TGT,          # 20 transCurrencyCodeTargetID
        CCY_TYPE_TGT,     # 21 transCurrencyTypeCodeTargetID
        year,             # 22 transFiscalPeriod
        acct_date,        # 23 transAcctDate
        trans_date,       # 24 transTransDate
        amount,           # 25 transTransAmount
        UOM,              # 26 transUnitOfMeasure
        UNIT_PRICE,       # 27 transUnitPrice
        STAT_AMT,         # 28 transStatisticAmount
        RULE_SET,         # 29 tranRuleSetID
        RULE_ID,          # 30 transRuleID
        DIR_OFFSET,       # 31 transDirVsOffsetFlg
        RECONCILE,        # 32 transReconcileFlg
        ADJUST,           # 33 transAdjustFlg
        EXT_AUDIT,        # 34 transExtensionIDAuditTrail
        EXT_SRC,          # 35 transExtensionIDSource
        EXT_CLASS,        # 36 transExtensionIDClass
        EXT_DATES,        # 37 transExtensionIDDates
        EXT_CUSTOM,       # 38 transExtensionIDCustom
    ])


# ── External merge-sort ──────────────────────────────────────────────────────

def spill_chunk(chunk: list, tmp_dir: str, spill_index: int) -> str:
    """Sort a chunk of row strings and write to a temp spill file. Returns path."""
    chunk.sort(key=row_sort_key)
    path = os.path.join(tmp_dir, f"spill_{spill_index:04d}.tmp")
    with open(path, "w", encoding="utf-8") as f:
        f.write("\n".join(chunk))
        f.write("\n")
    return path


def merge_spills(spill_paths: list, out_path: str) -> int:
    """
    k-way merge of sorted spill files into out_path.
    Uses heapq.merge — reads one line at a time from each file.
    Returns total rows written.
    """
    handles = [open(p, "r", encoding="utf-8") for p in spill_paths]
    # Strip trailing newlines; skip blank lines
    iterables = (line.rstrip("\n") for h in handles for line in h if line.strip())

    # Re-open per-file iterators for heapq.merge (need separate iterables)
    def file_iter(h):
        for line in h:
            s = line.rstrip("\n")
            if s:
                yield s

    os.makedirs(os.path.dirname(os.path.abspath(out_path)), exist_ok=True)
    rows_written = 0
    with open(out_path, "w", encoding="utf-8") as fout:
        for row_str in heapq.merge(
            *(file_iter(h) for h in handles),
            key=row_sort_key
        ):
            fout.write(row_str + "\n")
            rows_written += 1

    for h in handles:
        h.close()
    return rows_written


# ── Raw file parsers ─────────────────────────────────────────────────────────

def iter_expense_rows(infile: str, vendor_map: dict, year: str,
                      recformat: str, acct_date: str):
    """
    Generator: yield (row_str, matched_bool) for each input transaction.
    Emits two rows per source record (debit + credit).
    """
    delimiter = "\t" if recformat in ("A", "B") else ","
    jrnl_id = 0

    with open(infile, newline="", encoding="utf-8-sig", errors="replace") as f:
        reader = csv.reader(f, delimiter=delimiter)
        next(reader, None)  # skip header
        for cols in reader:
            cols = [c.strip().strip('"') for c in cols]
            if len(cols) < 5:
                continue
            try:
                if recformat == "A":
                    agency, fund, obj_code, prog, vendor, amount = (
                        cols[0], cols[1], cols[2], cols[3], cols[4], cols[5])
                    trans_date = acct_date
                elif recformat == "B":
                    agency, fund, obj_code, prog, vendor, amount = (
                        cols[0], cols[1], cols[2], cols[3], cols[4], cols[5])
                    trans_date = cols[6] if len(cols) > 6 else acct_date
                elif recformat == "C":
                    agency, amount, fund, obj_code, prog, vendor = (
                        cols[0], cols[1], cols[2], cols[3], cols[4], cols[5])
                    trans_date = cols[6] if len(cols) > 6 else acct_date
                else:
                    continue

                amt = float(amount.replace(",", ""))
            except (IndexError, ValueError):
                continue

            inst_id = vendor_map.get(vendor.upper(), "0000000000")
            matched = inst_id != "0000000000"

            jrnl_id += 1
            jid = f"ID{jrnl_id}{acct_date}"
            vendor_clean = vendor.replace(",", " ")

            yield (make_row_str(inst_id, jid, "1", "Procurement",
                                "Payment Sys", vendor_clean,
                                agency, fund, obj_code,
                                f"EXP{prog}", year, acct_date, trans_date,
                                str(amt)),
                   matched)

            yield (make_row_str(inst_id, jid, "2", "Cash Disbursement",
                                "Payment Sys", "Check to vendor",
                                agency, "0000", "0000",
                                "0000", year, acct_date, trans_date,
                                str(-amt)),
                   matched)


def iter_revenue_rows(infile: str, vendor_map: dict, year: str,
                      recformat: str, acct_date: str):
    """Generator: yield (row_str, matched_bool) for each revenue record."""
    delimiter = "\t" if recformat == "A" else ","
    jrnl_id = 0

    with open(infile, newline="", encoding="utf-8-sig", errors="replace") as f:
        reader = csv.reader(f, delimiter=delimiter)
        next(reader, None)
        for cols in reader:
            cols = [c.strip().strip('"') for c in cols]
            if len(cols) < 4:
                continue
            try:
                if recformat == "A":
                    agency, fund, src, amount = cols[0], cols[1], cols[2], cols[3]
                    trans_date = acct_date
                elif recformat == "C":
                    agency, amount, fund, src = cols[0], cols[1], cols[2], cols[3]
                    trans_date = cols[4] if len(cols) > 4 else acct_date
                else:
                    continue
                amt = float(amount.replace(",", ""))
            except (IndexError, ValueError):
                continue

            inst_id = vendor_map.get(agency.upper(), "0000000000")
            matched = inst_id != "0000000000"
            jrnl_id += 1
            jid = f"ID{jrnl_id}{acct_date}REV"

            yield (make_row_str(inst_id, jid, "1", "Tax Receipt",
                                "Revenue Sys", "Tax Receipt from Taxpayer",
                                agency, fund, " ",
                                f"REV{src}", year, acct_date, trans_date,
                                str(amt)),
                   matched)

            yield (make_row_str(inst_id, jid, "2", "Cash Receipt",
                                "Revenue Sys", "Tax Cash or Check",
                                agency, "0000", "0000",
                                "0000", year, acct_date, trans_date,
                                str(-amt)),
                   matched)


# ── Main pipeline ─────────────────────────────────────────────────────────────

def run(infile, outfile, vendormaster, year, quarter, rec_type,
        recformat, chunk_size, dry_run):

    acct_date = quarter_to_date(year, quarter)

    print(f"standardize_and_sort: {infile}")
    print(f"  year={year}  quarter={quarter}  type={rec_type}  recformat={recformat}")
    print(f"  acct_date={acct_date}  outfile={outfile}")
    print(f"  vendormaster={vendormaster}  chunk_size={chunk_size:,}")

    vendor_map = load_vendormaster(vendormaster)

    row_iter = (iter_expense_rows if rec_type == "E" else iter_revenue_rows)(
        infile, vendor_map, year, recformat, acct_date
    )

    # ── Chunk → spill loop ───────────────────────────────────────────────────
    tmp_dir = tempfile.mkdtemp(prefix="sortedJE_spill_")
    spill_paths = []
    chunk = []
    total_rows = 0
    matched_rows = 0
    total_amt = 0.0
    unmatched_names: set = set()

    try:
        for row_str, matched in row_iter:
            chunk.append(row_str)
            total_rows += 1
            if matched:
                matched_rows += 1
            # Accumulate sum from the amount field (col 25) for control total
            amt_str = row_str.split(",")[25]
            try:
                total_amt += float(amt_str)
            except ValueError:
                pass

            if len(chunk) >= chunk_size:
                spill_paths.append(spill_chunk(chunk, tmp_dir, len(spill_paths)))
                chunk.clear()
                print(f"  Spilled chunk {len(spill_paths)}  ({total_rows:,} rows so far)", end="\r")

        # Final partial chunk
        if chunk:
            spill_paths.append(spill_chunk(chunk, tmp_dir, len(spill_paths)))
            chunk.clear()

        print(f"\n  {len(spill_paths)} spill file(s), {total_rows:,} total rows")

        # ── Control totals ────────────────────────────────────────────────────
        print(f"\nControl totals:")
        print(f"  Journal lines produced:  {total_rows:,}")
        print(f"  Lines with instID match: {matched_rows:,}  "
              f"({100 * matched_rows // max(total_rows, 1)}%)")
        print(f"  Lines without match:     {total_rows - matched_rows:,}")
        print(f"  Sum of all amounts:      {total_amt:,.2f}  "
              f"(should be ~0 — debits cancel credits)")

        if dry_run:
            print("\n--dry-run: no output written.")
            return

        # ── k-way merge to final output ───────────────────────────────────────
        print(f"\nMerging {len(spill_paths)} spill(s) -> {outfile} ...")
        rows_written = merge_spills(spill_paths, outfile)
        size_mb = os.path.getsize(outfile) / 1024 / 1024
        print(f"Written: {outfile} ({size_mb:.1f} MB, {rows_written:,} rows)")

    finally:
        # Always clean up spill files
        for p in spill_paths:
            try:
                os.remove(p)
            except OSError:
                pass
        try:
            os.rmdir(tmp_dir)
        except OSError:
            pass


# ── CLI ───────────────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(
        description="Vendor-keyed SortedJE with external merge-sort (Step 2 replacement)."
    )
    parser.add_argument("--infile",       required=True)
    parser.add_argument("--outfile",      required=True)
    parser.add_argument("--vendormaster", default=DEFAULT_VM)
    parser.add_argument("--year",         required=True)
    parser.add_argument("--quarter",      type=int, default=1)
    parser.add_argument("--type",         default="E", choices=["E", "R"])
    parser.add_argument("--recformat",    default=None)
    parser.add_argument("--chunk-size",   type=int, default=DEFAULT_CHUNK,
                        help=f"Rows per sort chunk (default {DEFAULT_CHUNK:,})")
    parser.add_argument("--dry-run",      action="store_true")
    parser.add_argument("--logfile",      default=None)
    args = parser.parse_args()

    if args.logfile:
        os.makedirs(os.path.dirname(os.path.abspath(args.logfile)), exist_ok=True)
        log_fh = open(args.logfile, "w", buffering=1, encoding="utf-8")
        sys.stdout = log_fh
        sys.stderr = log_fh

    t_start = time.time()

    year_int   = int(args.year)
    rec_type   = args.type.upper()
    recformat  = args.recformat or auto_recformat(year_int, rec_type)

    run(
        infile       = args.infile,
        outfile      = args.outfile,
        vendormaster = args.vendormaster,
        year         = args.year,
        quarter      = args.quarter,
        rec_type     = rec_type,
        recformat    = recformat,
        chunk_size   = args.chunk_size,
        dry_run      = args.dry_run,
    )

    elapsed = round(time.time() - t_start, 1)
    print(f"Elapsed: {elapsed}s")
    print("STATUS: COMPLETE")


if __name__ == "__main__":
    main()
