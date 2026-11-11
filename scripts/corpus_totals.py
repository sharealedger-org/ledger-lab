#!/usr/bin/env python3
"""Corpus-level size and row totals for one archived full-corpus run.

Post-run reporting only (AGENTS.md: Python for data inspection / ad hoc metrics, owner approved
2026-11-11). It reads the archived summary tables written by archive_corpus_run_evidence.sh and
measures file sizes on disk. It does not read source transactions, post, or compute any financial
result, and it is not part of the measured engine path.

Usage:
  python3 scripts/corpus_totals.py --archive DIR [--root OUTPUT_ROOT] [--catalog CSV]

  --archive  directory written by archive_corpus_run_evidence.sh (reads *_all_years.csv there)
  --root     the run's output root (output_full); sizes its FY<yy> and VIEWS trees. Optional.
  --catalog  data/corpus_profile/source_catalog.csv (raw input file count and bytes). Optional.

Writes DIR/corpus_totals.csv (metric,value,unit,source,note) and prints it.
Every row says where the number came from. Nothing is estimated.
"""
import argparse
import csv
import os
import re
import sys


def read_rows(path):
    if not os.path.isfile(path):
        return None
    with open(path, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def num(x):
    try:
        return float(x)
    except (TypeError, ValueError):
        return 0.0


def tree_sizes(paths):
    """Apparent bytes and allocated bytes over the given trees, each inode counted once."""
    seen = set()
    apparent = allocated = files = 0
    for top in paths:
        for dirpath, _dirs, names in os.walk(top, followlinks=False):
            for n in names:
                p = os.path.join(dirpath, n)
                try:
                    st = os.lstat(p)
                except OSError:
                    continue
                if os.path.islink(p):
                    continue
                key = (st.st_dev, st.st_ino)
                if key in seen:
                    continue
                seen.add(key)
                files += 1
                apparent += st.st_size
                allocated += st.st_blocks * 512
    return files, apparent, allocated


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--archive", required=True)
    ap.add_argument("--root")
    ap.add_argument("--catalog")
    a = ap.parse_args()

    out = []

    def add(metric, value, unit, source, note=""):
        out.append((metric, value, unit, source, note))

    # ── raw input ─────────────────────────────────────────────────────────────
    if a.catalog and os.path.isfile(a.catalog):
        cat = read_rows(a.catalog)
        add("raw_input_files", len(cat), "files", "source_catalog.csv",
            "whole published corpus (350 VARawFiles + 35 VAPOData), not only the files a run read")
        add("raw_input_bytes", int(sum(num(r["bytes"]) for r in cat)), "bytes", "source_catalog.csv",
            "same scope; SHA-256 of every file is verified against the catalog by the download script")

    # ── step tables ───────────────────────────────────────────────────────────
    pm = read_rows(os.path.join(a.archive, "process_metrics_all_years.csv"))
    if pm is not None:
        by = {}
        for r in pm:
            by.setdefault(r["process_name"], []).append(r)

        def total(proc, col):
            return int(sum(num(r[col]) for r in by.get(proc, [])))

        def zeros(proc, col):
            rs = by.get(proc, [])
            return sum(1 for r in rs if num(r[col]) == 0), len(rs)

        z, n = zeros("step-ttl", "input_bytes")
        add("source_rows_expenditure", total("step-ttl", "input_rows"), "rows", "process_metrics step-ttl input_rows",
            "records read from the expenditure files")
        add("source_bytes_expenditure_reported", total("step-ttl", "input_bytes"), "bytes",
            "process_metrics step-ttl input_bytes",
            f"UNRELIABLE: step-ttl reports 0 input bytes in {z} of {n} years; do not quote as the input size")
        add("source_rows_po", total("po-standardize", "input_rows"), "rows", "process_metrics po-standardize input_rows",
            "records read from the purchase-order files")
        add("source_bytes_po", total("po-standardize", "input_bytes"), "bytes", "process_metrics po-standardize input_bytes", "")
        add("journal_lines_from_ttl", total("step-ttl", "output_rows"), "rows", "process_metrics step-ttl output_rows",
            "balanced journal lines from the expenditure side")
        add("merged_sje_rows", total("merge-payment-po", "output_rows"), "rows", "process_metrics merge-payment-po output_rows",
            "the merged SJE the posting steps and 5v read, all years")
        add("merged_sje_bytes", total("merge-payment-po", "output_bytes"), "bytes", "process_metrics merge-payment-po output_bytes", "")
        add("ledger_rows_step3", total("step-3", "output_rows"), "rows", "process_metrics step-3 output_rows", "")
        add("gl_keys_step20", total("gl-post", "output_rows"), "rows", "process_metrics gl-post output_rows", "")
        add("permutation_gl_keys_step21", total("permutation-generator", "output_rows"), "rows",
            "process_metrics permutation-generator output_rows", "")
        add("step_cpu_seconds_all", round(sum(num(r["cpu_total_seconds"]) for r in pm), 2), "cpu_s",
            "process_metrics cpu_total_seconds, all steps", "excludes the 5v pass (its metrics are not in this table)")
        add("max_step_peak_rss_bytes", int(max((num(r["peak_rss_bytes"]) for r in pm), default=0)), "bytes",
            "process_metrics peak_rss_bytes", "highest of any step in the table")

    cs = read_rows(os.path.join(a.archive, "cost_surface_all_years.csv"))
    if cs is not None:
        chain = [r for r in cs if r.get("curve") == "CCP1"]
        add("ledger_rows", int(sum(num(r["ledger_row_count"]) for r in chain)), "rows", "cost_surface CCP1 ledger_row_count", "")
        add("ledger_file_bytes", int(sum(num(r["ldgr_file_bytes"]) for r in chain)), "bytes", "cost_surface CCP1 ldgr_file_bytes", "")
        add("permutation_rows", int(sum(num(r["permutation_rows"]) for r in chain)), "rows", "cost_surface CCP1 permutation_rows", "")
        add("permutation_capacity_sum_cost_surface", sum(int(num(r["permutation_capacity"])) for r in chain), "count",
            "cost_surface CCP1 permutation_capacity",
            "can be short: the step 21s summary call can fail silently (FY03 in the 2026-11-10 baseline); "
            "prefer permutation_capacity_sum from PERM_SUMMARY files when --root is given")
        add("cost_surface_total_storage_bytes_NOT_ADDITIVE",
            int(sum(num(r["total_storage_bytes"]) for r in cs)), "bytes",
            "cost_surface total_storage_bytes, all rows",
            "DO NOT QUOTE: each row is a whole generation directory, so prep files that posting hard-links are counted again")

    vs = read_rows(os.path.join(a.archive, "views_cost_surface.csv"))
    if vs:
        add("view_file_bytes", int(sum(num(r["view_file_bytes"]) for r in vs)), "bytes", "VIEWS cost_surface view_file_bytes", "the four view files")
        add("views_pass_compute_s", int(sum(num(r["total_compute_s"]) for r in vs)), "s", "VIEWS cost_surface total_compute_s", "wall seconds of 5v")

    pc = read_rows(os.path.join(a.archive, "partition_catalog_all_years.csv"))
    if pc is not None:
        layers = {}
        for r in pc:
            d = layers.setdefault(r["logical_layer"], [0, 0, 0])
            d[0] += 1
            d[1] += int(num(r["row_count"]))
            d[2] += int(num(r["byte_count"]))
        add("catalogued_partitions", len(pc), "partitions", "partition_catalog", "")
        add("catalogued_partition_rows_all", sum(v[1] for v in layers.values()), "rows", "partition_catalog row_count",
            "a row appears once per partition it is written to, so this is volume written, not distinct records")
        add("catalogued_partition_bytes_all", sum(v[2] for v in layers.values()), "bytes", "partition_catalog byte_count",
            "logical sum over catalogued partitions; can differ from bytes on disk")
        for layer in sorted(layers):
            add(f"catalogued_bytes_{layer}", layers[layer][2], "bytes", "partition_catalog byte_count",
                f"{layers[layer][0]} partitions")

    # ── bytes on disk ─────────────────────────────────────────────────────────
    if a.root and os.path.isdir(a.root):
        committed, all_trees = [], []
        for name in sorted(os.listdir(a.root)):
            p = os.path.join(a.root, name)
            if re.fullmatch(r"FY\d\d|VIEWS", name) and os.path.islink(p):
                committed.append(os.path.realpath(p))
            if name.endswith(".generations") and os.path.isdir(p):
                all_trees.append(p)
        if committed:
            gl = vend = cap = nyears = 0
            for tree in committed:
                for n in os.listdir(tree):
                    if re.fullmatch(r"PERM_SUMMARY\d{4}\.csv", n):
                        for r in read_rows(os.path.join(tree, n)) or []:
                            gl += int(r["gl_keys"])
                            vend += int(r["vendor_rows"])
                            cap += int(r["capacity"])
                            nyears += 1
            if nyears:
                add("permutation_years_summarised", nyears, "years", "PERM_SUMMARY<yyyy>.csv in the committed FY trees", "")
                add("permutation_gl_keys", gl, "rows", "PERM_SUMMARY files", "authoritative step 21 output")
                add("permutation_vendor_rows", vend, "rows", "PERM_SUMMARY files", "")
                add("permutation_capacity_sum", cap, "count", "PERM_SUMMARY files",
                    "upper bound, per-year values summed (exact integers); see fd_untracked_dims in the step 21 RESULT lines")
            fl, ap_b, al_b = tree_sizes(committed)
            add("output_committed_files", fl, "files", "walk of FY<yy> and VIEWS symlink targets", "each inode once")
            add("output_committed_apparent_bytes", ap_b, "bytes", "walk of FY<yy> and VIEWS symlink targets",
                "final state of the run: what a comparison with another engine's output should use")
            add("output_committed_allocated_bytes", al_b, "bytes", "walk of FY<yy> and VIEWS symlink targets", "blocks allocated on disk")
        if all_trees:
            fl, ap_b, al_b = tree_sizes(all_trees)
            add("output_retained_apparent_bytes", ap_b, "bytes", "walk of all *.generations",
                "includes superseded generations kept by the prune rule (last two); hard links counted once")
            add("output_retained_allocated_bytes", al_b, "bytes", "walk of all *.generations", "blocks allocated on disk")

    path = os.path.join(a.archive, "corpus_totals.csv")
    with open(path, "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["metric", "value", "unit", "source", "note"])
        w.writerows(out)
    for m, v, u, _s, note in out:
        print(f"{m:46s} {v!s:>20s} {u:10s} {note}")
    print(f"\nWrote {path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
