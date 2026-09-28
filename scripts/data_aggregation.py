#!/usr/bin/env python3
"""
data_aggregation.py — Step 5: ViewSpec-driven aggregation of LDGR + VendorMaster.

Replaces data_aggregation.ps1 with a Python implementation.

CKB discipline: one sequential pass per LDGR file builds all view aggregations
simultaneously.  O(M) time, O(V) RAM (V = distinct view-key combinations).
No re-reads, no intermediate files.

WHAT THIS DOES
--------------
For each fiscal year:
  1. Load VendorMaster into a dict keyed on zero-padded instID (once, shared across years).
  2. Read LDGR{year}.csv sequentially.  For each balance row:
     a. Look up VendorMaster by ldgrIPID → get CAR attributes.
     b. For every enabled ViewSpec view: apply LDGR and CAR filters, build composite
        group key, accumulate ldgrTransAmount into a running sum dict.
  3. Write one output CSV per view per year.
  4. Append rows to pivot_results.csv (Experiment 2 accumulator).
  5. Append one row to cost_surface.csv (Experiment 1 accumulator).

USAGE
-----
  python scripts/data_aggregation.py
  python scripts/data_aggregation.py --years 2003
  python scripts/data_aggregation.py --years 2003,2004,2005 --curve C2 --config full-views
  python scripts/data_aggregation.py --vendormaster data/VendorMaster_enriched.csv --dry-run

ARGUMENTS
---------
  --ldgr-dir       Directory containing LDGR{year}.csv files  (default: data/output)
  --vendormaster   Path to VendorMaster CSV                   (default: data/VendorMaster.csv)
  --viewspec       Path to ViewSpec.csv                       (default: data/ViewSpec.csv)
  --out-dir        Output directory for view CSVs             (default: data/output)
  --years          Comma-separated 4-digit years              (default: all LDGR files found)
  --curve          Experiment curve label C1/C2/C3/C4         (default: C2)
  --config         Short config description                   (default: full-views)
  --dry-run        Aggregate and print totals; do not write output files

(c) Copyright IBM Corporation. 2018
SPDX-License-Identifier: Apache-2.0
By Kip Twitchell
2025 — Python replacement for data_aggregation.ps1 (Bob AI)
"""

import argparse
import csv
import math
import os
import sys
import time
from collections import defaultdict
from datetime import datetime

DEFAULT_LDGR_DIR   = os.path.join("data", "output")
DEFAULT_VM         = os.path.join("data", "VendorMaster.csv")
DEFAULT_VIEWSPEC   = os.path.join("data", "ViewSpec.csv")
DEFAULT_OUT_DIR    = os.path.join("data", "output")
PIVOT_FILE         = os.path.join("data", "output", "pivot_results.csv")
COST_SURFACE_FILE  = os.path.join("data", "cost_surface.csv")
PIPELINE_LOG_FILE  = os.path.join("data", "pipeline_log.csv")

# LDGR column name → 0-based index (matches post.scala ldgrHeader)
LDGR_IDX = {
    "ldgrIPID":                    0,
    "ldgrContractID":              1,
    "ldgrCommitmentID":            2,
    "ldgrLdgrlID":                 3,
    "ldgrSourceSystemID":          4,
    "ldgrLedgerID":                5,
    "ldgrJrnlType":                6,
    "ldgrBookCodeID":              7,
    "ldgrLegalEntityID":           8,
    "ldgrCenterID":                9,
    "ldgrProjectID":               10,
    "ldgrProductID":               11,
    "ldgrNominalAccountID":        12,
    "ldgrAltAccountID":            13,
    "ldgrCurrencyCodeSourceID":    14,
    "ldgrCurrencyTypeCodeSourceID":15,
    "ldgrCurrencyCodeTargetID":    16,
    "ldgrCurrencyTypeCodeTargetID":17,
    "ldgrLedgerPeriod":            18,
    "ldgrTransAmount":             19,
    "ldgrUnitOfMeasure":           20,
    "ldgrStatisticAmount":         21,
    "ldgrDirVsOffsetFlg":          22,
    "ldgrReconcileFlg":            23,
    "ldgrAdjustFlg":               24,
}
AMOUNT_IDX = LDGR_IDX["ldgrTransAmount"]
IPID_IDX   = LDGR_IDX["ldgrIPID"]


# ── Permutation cost (Instrument Pivot Theorem, Paper 1 §3.4) ───────────────

def combinations(n: int, k: int) -> int:
    if k > n or k < 0:
        return 0
    if k == 0 or k == n:
        return 1
    k = min(k, n - k)
    result = 1
    for i in range(k):
        result = result * (n - i) // (i + 1)
    return result


def permutation_cost(m: int, v: int) -> int:
    """Sum(k=1..m) C(m,k) * v^k  — key-embedded balance records required."""
    if m <= 0 or v <= 0:
        return 0
    return sum(combinations(m, k) * (v ** k) for k in range(1, m + 1))


# ── ViewSpec loader ───────────────────────────────────────────────────────────

def load_viewspec(path: str) -> list:
    """Return list of view dicts for all enabled=Y rows."""
    views = []
    with open(path, newline="", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split(",", 10)
            if len(parts) < 10:
                continue
            if parts[0].strip() == "view_name":
                continue  # header row
            enabled = parts[9].strip().upper()
            if enabled != "Y":
                continue

            group_by_ldgr = [c for c in parts[1].strip().split("|") if c]
            group_by_sal  = [c for c in parts[2].strip().split("|") if c]
            top_n = int(parts[7].strip()) if parts[7].strip().isdigit() else 0
            sal_attr = int(parts[8].strip()) if parts[8].strip().isdigit() else 0

            views.append({
                "name":             parts[0].strip(),
                "group_by_ldgr":    group_by_ldgr,
                "group_by_sal":     group_by_sal,
                "filter_ldgr_col":  parts[3].strip(),
                "filter_ldgr_val":  parts[4].strip(),
                "filter_sal_col":   parts[5].strip(),
                "filter_sal_val":   parts[6].strip(),
                "top_n":            top_n,
                "sal_attr_count":   sal_attr,
                "description":      parts[10].strip() if len(parts) > 10 else "",
            })
    return views


# ── VendorMaster loader ───────────────────────────────────────────────────────

def load_vendormaster(path: str) -> tuple[dict, list]:
    """
    Return (vm_history, fieldnames).
    vm_history: zero-padded-10-digit instID -> list of {start_date, end_date, row_dict}
    sorted by start_date ascending. Supports date-effective as-of join.
    """
    vm_history = defaultdict(list)
    fieldnames = []
    if not os.path.exists(path):
        print(f"WARNING: VendorMaster not found at {path} — CAR joins will be empty",
              file=sys.stderr)
        return vm_history, fieldnames

    with open(path, newline="", encoding="utf-8-sig") as f:
        lines = [l for l in f if not l.startswith("#")]
    reader = csv.DictReader(lines)
    fieldnames = list(reader.fieldnames or [])
    total_rows = 0
    for row in reader:
        raw_id = row.get("instID", "").strip().strip('"')
        if not raw_id:
            continue
        try:
            key = str(int(raw_id)).zfill(10)
        except ValueError:
            key = raw_id
        start_date = (row.get("instEffectDate") or "0000-00-00").strip().replace("/", "-")
        end_date   = (row.get("instEffectEndDate") or "9999-99-99").strip().replace("/", "-")
        vm_history[key].append({
            "start": start_date,
            "end": end_date,
            "row": row
        })
        total_rows += 1

    # Sort each vendor's history by start date ascending
    for key in vm_history:
        vm_history[key].sort(key=lambda item: item["start"])

    print(f"VendorMaster loaded: {len(vm_history):,} distinct instruments ({total_rows:,} history rows) ({path})")
    return vm_history, fieldnames


def lookup_sal_row(vm_history: dict, ip_id: str, as_of_date: str) -> dict:
    """
    Find date-effective VendorMaster row where start <= as_of_date <= end.
    Falls back to single entry or closest entry if dates are unpopulated.
    """
    entries = vm_history.get(ip_id)
    if not entries:
        try:
            entries = vm_history.get(str(int(ip_id)).zfill(10))
        except ValueError:
            pass
    if not entries:
        return None

    if len(entries) == 1 or not as_of_date:
        return entries[0]["row"]

    # Match range
    for item in entries:
        if item["start"] <= as_of_date <= item["end"]:
            return item["row"]

    # If before first start date, return earliest; if after last end date, return latest
    if as_of_date < entries[0]["start"]:
        return entries[0]["row"]
    return entries[-1]["row"]


# ── Single-year aggregation pass ──────────────────────────────────────────────

def run_year(year: str, ldgr_dir: str, vm: dict, views: list,
             out_dir: str, dry_run: bool) -> dict:
    """One sequential pass over LDGR{year}.csv, all views simultaneously."""
    ldgr_path = os.path.join(ldgr_dir, f"LDGR{year}.csv")
    if not os.path.exists(ldgr_path):
        print(f"  LDGR{year}.csv not found — skipping")
        return None

    t0 = time.time()
    print(f"  FY{year}: reading {ldgr_path} ({os.path.getsize(ldgr_path)/1024/1024:.1f} MB) ...")

    # One dict per view: composite_key -> running sum
    aggs      = {v["name"]: defaultdict(float) for v in views}
    # Track first ldgr row for each key (for output column reconstruction)
    key_cols  = {v["name"]: {} for v in views}

    row_count  = 0
    vm_misses  = 0

    with open(ldgr_path, newline="", encoding="utf-8") as f:
        reader = csv.reader(f)
        next(reader, None)  # skip header
        for cols in reader:
            if len(cols) <= AMOUNT_IDX:
                continue

            # VendorMaster as-of lookup (period e.g. "200301" or year e.g. "2003" -> "2003-01-01")
            ip_id = cols[IPID_IDX].strip()
            period_val = cols[LDGR_IDX["ldgrLedgerPeriod"]].strip() if len(cols) > LDGR_IDX["ldgrLedgerPeriod"] else ""
            if len(period_val) == 6 and period_val.isdigit():
                as_of_date = f"{period_val[:4]}-{period_val[4:6]}-01"
            elif len(period_val) == 4 and period_val.isdigit():
                as_of_date = f"{period_val}-07-01"
            elif len(year) == 4 and year.isdigit():
                as_of_date = f"{year}-07-01"
            else:
                as_of_date = ""

            sal_row = lookup_sal_row(vm, ip_id, as_of_date)
            if sal_row is None:
                vm_misses += 1

            try:
                amount = float(cols[AMOUNT_IDX])
            except ValueError:
                amount = 0.0

            for v in views:
                # LDGR filter
                fc = v["filter_ldgr_col"]
                if fc:
                    fi = LDGR_IDX.get(fc)
                    if fi is not None and (fi >= len(cols) or cols[fi].strip() != v["filter_ldgr_val"]):
                        continue

                # CAR filter
                fsc = v["filter_sal_col"]
                if fsc:
                    if sal_row is None or sal_row.get(fsc, "").strip() != v["filter_sal_val"]:
                        continue

                # Skip CAR-dimension views when vendor not found
                if v["group_by_sal"] and sal_row is None:
                    continue

                # Build composite key
                key_parts = []
                for col in v["group_by_ldgr"]:
                    idx = LDGR_IDX.get(col)
                    key_parts.append(cols[idx].strip() if idx is not None and idx < len(cols) else "")
                for col in v["group_by_sal"]:
                    key_parts.append((sal_row.get(col) or "").strip() if sal_row else "")
                key = "|".join(key_parts)

                aggs[v["name"]][key] += amount
                if key not in key_cols[v["name"]]:
                    key_cols[v["name"]][key] = cols

            row_count += 1

    elapsed = time.time() - t0
    print(f"  FY{year}: {row_count:,} rows | VM misses: {vm_misses:,} | {elapsed:.1f}s")

    # Write view output files
    total_storage = 0
    view_results  = []

    for v in views:
        agg = aggs[v["name"]]
        vname = v["name"]

        # Sort by abs(amount) descending
        sorted_keys = sorted(agg.keys(), key=lambda k: abs(agg[k]), reverse=True)
        if v["top_n"] > 0:
            sorted_keys = sorted_keys[:v["top_n"]]

        # Instrument count = distinct first element of key (ldgrIPID or first group col)
        instr_set = {k.split("|")[0] for k in agg}
        instr_count = len(instr_set)
        perm_cost = permutation_cost(v["sal_attr_count"], instr_count)

        out_cols = v["group_by_ldgr"] + v["group_by_sal"] + ["ldgrTransAmount"]

        if not dry_run:
            out_path = os.path.join(out_dir, f"{vname}_{year}.csv")
            with open(out_path, "w", newline="", encoding="utf-8") as f:
                writer = csv.writer(f)
                writer.writerow(out_cols)
                for key in sorted_keys:
                    writer.writerow(key.split("|") + [f"{agg[key]:.2f}"])
            size = os.path.getsize(out_path)
            total_storage += size
            print(f"    {vname}_{year}.csv: {len(sorted_keys):,} rows | "
                  f"instr={instr_count:,} | permCost={perm_cost:,}")
        else:
            size = 0
            print(f"    {vname}_{year} [dry-run]: {len(sorted_keys):,} rows | "
                  f"instr={instr_count:,} | permCost={perm_cost:,}")

        view_results.append({
            "year":         year,
            "view_name":    vname,
            "sal_attr_count": v["sal_attr_count"],
            "view_rows":    len(sorted_keys),
            "instr_count":  instr_count,
            "perm_cost":    perm_cost,
            "ldgr_rows":    row_count,
            "scope":        f"FY{year}",
            "description":  v["description"],
        })

    return {
        "year":          year,
        "row_count":     row_count,
        "elapsed_s":     elapsed,
        "storage_bytes": total_storage,
        "view_results":  view_results,
    }


# ── Accumulator writers ───────────────────────────────────────────────────────

PIVOT_HEADER = ("run_timestamp,year,view_name,sal_attr_count,view_rows,"
                "instrument_count,key_embedded_equivalent_balances,ratio,ldgr_rows,"
                "recon_obligations_sal,recon_obligations_keyembedded,rebuild_cost_ratio,scope,description")

COST_HEADER  = ("run_id,run_timestamp,curve,config_description,years,steps,"
                "total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy")
PIPELINE_LOG_HEADER = ("run_id,run_timestamp,step,file_label,elapsed_s,"
                       "rows_in,rows_out,storage_bytes")


def append_pivot_rows(view_results: list, run_ts: str, dry_run: bool):
    if dry_run:
        return
    os.makedirs(os.path.dirname(PIVOT_FILE), exist_ok=True)
    write_header = not os.path.exists(PIVOT_FILE)
    with open(PIVOT_FILE, "a", newline="", encoding="utf-8") as f:
        if write_header:
            f.write(PIVOT_HEADER + "\n")
        for vr in view_results:
            ratio = round(vr["perm_cost"] / max(vr["instr_count"], 1), 2)
            ldgr_rows = vr.get("ldgr_rows", 0)
            recon_sal = 1
            recon_keyemb = vr["perm_cost"]
            rebuild_ratio = round(vr["perm_cost"] / max(ldgr_rows, 1), 4) if ldgr_rows > 0 else ratio
            scope = vr.get("scope", f"FY{vr['year']}")
            f.write(f"{run_ts},{vr['year']},{vr['view_name']},"
                    f"{vr['sal_attr_count']},{vr['view_rows']},"
                    f"{vr['instr_count']},{vr['perm_cost']},{ratio},"
                    f"{ldgr_rows},{recon_sal},{recon_keyemb},{rebuild_ratio},{scope},"
                    f"\"{vr['description']}\"\n")


def append_pipeline_log_rows(run_id, run_ts, step_rows: list, dry_run: bool):
    """Append per-step timing rows to pipeline_log.csv.

    step_rows: list of dicts with keys:
        step, file_label, elapsed_s, rows_in, rows_out, storage_bytes
    """
    if dry_run:
        return
    write_header = not os.path.exists(PIPELINE_LOG_FILE)
    with open(PIPELINE_LOG_FILE, "a", newline="", encoding="utf-8") as f:
        if write_header:
            f.write(PIPELINE_LOG_HEADER + "\n")
        for r in step_rows:
            f.write(f"{run_id},{run_ts},{r['step']},{r['file_label']},"
                    f"{r['elapsed_s']:.2f},{r.get('rows_in','')},"
                    f"{r.get('rows_out','')},{r.get('storage_bytes','')}\n")


def append_cost_row(run_id, run_ts, curve, config, years_str, steps_str,
                    compute_s, storage_bytes, view_count, dry_run: bool):
    if dry_run:
        return
    os.makedirs(os.path.dirname(COST_SURFACE_FILE) or ".", exist_ok=True)
    write_header = not os.path.exists(COST_SURFACE_FILE)
    storage_mb   = storage_bytes / 1_000_000
    proxy        = round(compute_s + storage_mb + (view_count * 10), 2)
    with open(COST_SURFACE_FILE, "a", newline="", encoding="utf-8") as f:
        if write_header:
            f.write(COST_HEADER + "\n")
        f.write(f"{run_id},{run_ts},{curve},{config},"
                f"\"{years_str}\",\"{steps_str}\","
                f"{compute_s:.2f},{storage_bytes},{view_count},{proxy}\n")


# ── CLI ───────────────────────────────────────────────────────────────────────

def discover_years(ldgr_dir: str) -> list:
    years = []
    for fn in sorted(os.listdir(ldgr_dir)):
        if fn.startswith("LDGR") and fn.endswith(".csv"):
            years.append(fn[4:-4])
    return years


def main():
    parser = argparse.ArgumentParser(
        description="Step 5: ViewSpec-driven LDGR aggregation (Python replacement for data_aggregation.ps1)."
    )
    parser.add_argument("--ldgr-dir",     default=DEFAULT_LDGR_DIR)
    parser.add_argument("--vendormaster", default=DEFAULT_VM)
    parser.add_argument("--viewspec",     default=DEFAULT_VIEWSPEC)
    parser.add_argument("--out-dir",      default=DEFAULT_OUT_DIR)
    parser.add_argument("--years",        default="")
    parser.add_argument("--curve",        default="C2")
    parser.add_argument("--config",       default="full-views")
    parser.add_argument("--dry-run",      action="store_true")
    # Per-step timing passthrough — elapsed seconds from Steps 2 and 3
    # passed in from the orchestrating script so cost_surface.csv captures
    # the full pipeline compute cost, not just Step 5.
    parser.add_argument("--step2-s",      type=float, default=0.0,
                        help="Total elapsed seconds for all Step 2 (standardize_and_sort) runs")
    parser.add_argument("--step3-s",      type=float, default=0.0,
                        help="Elapsed seconds for Step 3 (post.scala) run")
    # Per-step log rows from Steps 2 and 3: pass as file path or inline JSON
    parser.add_argument("--pipeline-log-file", default=None,
                        help="Path to JSON file: list of step-log dicts to prepend to pipeline_log.csv")
    parser.add_argument("--pipeline-log-json", default=None,
                        help="JSON string: list of step-log dicts to prepend to pipeline_log.csv")
    args = parser.parse_args()

    print("=" * 70)
    print("Step 5: ViewSpec-driven LDGR Aggregation")
    print(f"  ldgr-dir    : {args.ldgr_dir}")
    print(f"  vendormaster: {args.vendormaster}")
    print(f"  viewspec    : {args.viewspec}")
    print(f"  out-dir     : {args.out_dir}")
    print(f"  curve       : {args.curve}")
    print(f"  config      : {args.config}")
    print(f"  dry-run     : {args.dry_run}")
    print("=" * 70)

    t_total = time.time()

    views = load_viewspec(args.viewspec)
    print(f"ViewSpec: {len(views)} enabled views")
    for v in views:
        print(f"  {v['name']:30s}  ldgr={v['group_by_ldgr']}  sal={v['group_by_sal']}")

    vm, _ = load_vendormaster(args.vendormaster)

    years = [y.strip() for y in args.years.split(",")] if args.years else discover_years(args.ldgr_dir)
    print(f"\nYears to process: {years}")

    os.makedirs(args.out_dir, exist_ok=True)

    run_ts  = datetime.now().strftime("%Y-%m-%dT%H:%M:%S")
    run_id  = "run_" + datetime.now().strftime("%Y%m%d_%H%M%S")
    grand_compute  = args.step2_s + args.step3_s   # seed with upstream step times
    grand_storage  = 0
    all_view_results = []

    # Write any upstream step rows passed in from the orchestrator
    prior_log_rows = []
    import json
    if args.pipeline_log_file:
        with open(args.pipeline_log_file, "r", encoding="utf-8") as _f:
            prior_log_rows = json.loads(_f.read())
    elif args.pipeline_log_json:
        prior_log_rows = json.loads(args.pipeline_log_json)
    step5_log_rows = []

    for year in years:
        result = run_year(year, args.ldgr_dir, vm, views, args.out_dir, args.dry_run)
        if result is None:
            continue
        grand_compute += result["elapsed_s"]
        grand_storage += result["storage_bytes"]
        all_view_results.extend(result["view_results"])
        append_pivot_rows(result["view_results"], run_ts, args.dry_run)
        step5_log_rows.append({
            "step":          "5",
            "file_label":    f"LDGR{year}",
            "elapsed_s":     result["elapsed_s"],
            "rows_in":       result.get("row_count", ""),
            "rows_out":      sum(vr["view_rows"] for vr in result["view_results"]),
            "storage_bytes": result["storage_bytes"],
        })

    # Determine steps label from what was actually provided
    steps_parts = []
    if args.step2_s > 0:
        steps_parts.append("2")
    if args.step3_s > 0:
        steps_parts.append("3")
    steps_parts.append("5")
    steps_str = "+".join(steps_parts)

    append_pipeline_log_rows(run_id, run_ts, prior_log_rows + step5_log_rows, args.dry_run)
    append_cost_row(run_id, run_ts, args.curve, args.config,
                    "+".join(years), steps_str, grand_compute, grand_storage,
                    len(views), args.dry_run)

    total_elapsed = time.time() - t_total
    step5_s = total_elapsed
    print()
    print("=" * 70)
    print(f"Step 5 complete  {step5_s:.1f}s")
    if args.step2_s or args.step3_s:
        print(f"  Step 2 (passed in) : {args.step2_s:.1f}s")
        print(f"  Step 3 (passed in) : {args.step3_s:.1f}s")
    print(f"  Total pipeline     : {grand_compute:.1f}s  (steps {steps_str})")
    print(f"  Enabled views : {len(views)}")
    print(f"  Storage written: {grand_storage/1024/1024:.1f} MB")
    if not args.dry_run:
        print(f"  pipeline_log  : {PIPELINE_LOG_FILE}")
        print(f"  pivot_results : {PIVOT_FILE}")
        print(f"  cost_surface  : {COST_SURFACE_FILE}")
    print("STATUS: COMPLETE")


if __name__ == "__main__":
    main()
