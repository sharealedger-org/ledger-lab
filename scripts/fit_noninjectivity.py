#!/usr/bin/env python3
"""
AIM1 -- Non-injectivity count (no ML required)
Paper 2 Part 1, Section §5.2

Measures how many distinct vendors (transIPID) map to the same posted key in CCP1 (GL)
versus how many are preserved in CCP2 (Instrument Ledger).

Inputs (resolved relative to repo root unless overridden):
  --ldgr  PATH   LDGR20YY.csv (CCP2 step 3 output)
  --perm  PATH   PERM_TBL20YY.csv (CCP1 step 21 output): one row per GL key
                 (agency, nominal_account_id, fund, object_id, fiscal_year, dir_flag),
                 with a d_transIPID column = distinct vendors under that key.  The column
                 is absent when vendor is constant in the year, which means 1 vendor per key.
  --gl    PATH   GL20YY.csv (CCP1 step 20 output), same GL key plus total_amount

Output:
  data/test_evidence/AIM1_noninjectivity_distribution.csv

Acceptance: No pass/fail threshold.  Any non-zero gap between CCP1 and CCP2 substrates
is evidence.  A large gap is strong evidence of structural information loss.

AGENTS.md: Python only for post-engine bounded summary work.  Owner approval received
2026-11-05.  Reads only small Scala-produced summary tables; does not touch raw corpus.
"""

import csv
import datetime
import os
import sys

# ---------------------------------------------------------------------------
# CLI arguments
# ---------------------------------------------------------------------------
LDGR_PATH    = "data/output_validate_fy03/output_validate_fy03.next.834/LDGR2003.csv"
PERM_PATH    = "data/output_validate_fy03/output_validate_fy03.next.834/PERM_TBL2003.csv"
GL_PATH      = "data/output_validate_fy03/output_validate_fy03.next.834/GL2003.csv"
EVIDENCE_OUT = "data/test_evidence/AIM1_noninjectivity_distribution.csv"

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--ldgr" and i + 1 < len(sys.argv):
        LDGR_PATH = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--perm" and i + 1 < len(sys.argv):
        PERM_PATH = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--gl" and i + 1 < len(sys.argv):
        GL_PATH = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--evidence-out" and i + 1 < len(sys.argv):
        EVIDENCE_OUT = sys.argv[i + 1]; i += 2
    else:
        print("Unknown argument: " + sys.argv[i], file=sys.stderr)
        sys.exit(1)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def open_csv(path):
    """Open a CSV file with UTF-8-sig encoding (handles BOM)."""
    return open(path, newline="", encoding="utf-8-sig")


def count_distinct_patterns(path, key_col, pattern_cols):
    """
    Count distinct tuples of pattern_cols values per key_col value.
    Returns {key: count_of_distinct_pattern_tuples}.
    """
    from collections import defaultdict
    key_to_patterns = defaultdict(set)
    missing_cols = []
    with open_csv(path) as fh:
        reader = csv.DictReader(fh)
        if reader.fieldnames is None:
            print(f"  WARNING: {path} has no header row", file=sys.stderr)
            return {}
        for col in [key_col] + pattern_cols:
            if col not in reader.fieldnames:
                missing_cols.append(col)
        if missing_cols:
            print(f"  WARNING: {path} missing columns {missing_cols}; skipping", file=sys.stderr)
            return {}
        for row in reader:
            key = row[key_col]
            pattern = tuple(row[c] for c in pattern_cols)
            key_to_patterns[key].add(pattern)
    return {k: len(v) for k, v in key_to_patterns.items()}


def summarise(counts, label):
    """Compute summary statistics over a {key: distinct_count} dict."""
    if not counts:
        return {"label": label, "key_count": 0, "total_distinct_patterns": 0,
                "mean_patterns_per_key": 0.0, "max_patterns_per_key": 0,
                "keys_with_multiple_patterns": 0, "collapse_ratio": 0.0}
    vals = list(counts.values())
    total = sum(vals)
    n     = len(vals)
    multi = sum(1 for v in vals if v > 1)
    return {
        "label":                     label,
        "key_count":                 n,
        "total_distinct_patterns":   total,
        "mean_patterns_per_key":     round(total / n, 4),
        "max_patterns_per_key":      max(vals),
        "keys_with_multiple_patterns": multi,
        "collapse_ratio":            round(multi / n, 4) if n > 0 else 0.0,
    }

# ---------------------------------------------------------------------------
# AIM1 analysis
# ---------------------------------------------------------------------------

print("=" * 70)
print("AIM1 — Non-injectivity count")
print("=" * 70)

results = []

# CCP2 substrate: Instrument Ledger
# Key: (ldgrIPID, ldgrLedgerPeriod)   — instrument-keyed
# Pattern dims: ldgrCommitmentID (PO number), ldgrNominalAccountID
print(f"\n[1] CCP2 Instrument Ledger: {LDGR_PATH}")
if os.path.isfile(LDGR_PATH):
    ldgr_counts = count_distinct_patterns(
        LDGR_PATH,
        key_col="ldgrIPID",
        pattern_cols=["ldgrCommitmentID", "ldgrNominalAccountID"]
    )
    ldgr_summary = summarise(ldgr_counts, "CCP2_LDGR")
    for k, v in ldgr_summary.items():
        print(f"  {k}: {v}")
    results.append(ldgr_summary)
else:
    print(f"  SKIP — file not found: {LDGR_PATH}", file=sys.stderr)
    results.append({"label": "CCP2_LDGR", "key_count": 0, "note": "file not found"})

GL_KEY_COLS = ["agency", "nominal_account_id", "fund", "object_id", "fiscal_year", "dir_flag"]


def gl_key(row):
    return tuple(row[c] for c in GL_KEY_COLS)


def perm_vendor_counts(path):
    """
    Read PERM_TBL (one row per GL key) and return {gl_key: distinct vendors}.
    d_transIPID is absent when vendor is constant in the year; every key then has 1 vendor.
    Returns None when the key columns are missing (old-schema file).
    """
    counts = {}
    with open_csv(path) as fh:
        reader = csv.DictReader(fh)
        if reader.fieldnames is None or any(c not in reader.fieldnames for c in GL_KEY_COLS):
            print(f"  WARNING: {path} lacks GL key columns {GL_KEY_COLS}; old schema? skipping",
                  file=sys.stderr)
            return None
        has_vendor_col = "d_transIPID" in reader.fieldnames
        for row in reader:
            counts[gl_key(row)] = int(row["d_transIPID"]) if has_vendor_col else 1
    return counts


# CCP1 substrate A: GL output
# GL key: (agency, nominal_account_id, fund, object_id, fiscal_year, dir_flag) -- one balance
# row per key.  The GL accumulates a single total_amount per key, so vendor identity is gone.
# The PERM table is the answer key: d_transIPID is the number of vendors that fed each GL key.
# A GL balance row with >1 vendor is a row where distinct pre-hierarchy patterns were
# collapsed into one posted number.
print(f"\n[2] CCP1 GL output (vendors per GL balance row, via PERM): {GL_PATH}")
perm_vendors = perm_vendor_counts(PERM_PATH) if os.path.isfile(PERM_PATH) else None
if os.path.isfile(GL_PATH) and perm_vendors is not None:
    gl_keys = set()
    with open_csv(GL_PATH) as fh:
        for row in csv.DictReader(fh):
            gl_keys.add(gl_key(row))
    # GL keys with no PERM row have 0 vendors; count them as 0 so key_count is the GL row count.
    gl_counts = {k: perm_vendors.get(k, 0) for k in gl_keys}
    gl_summary = summarise(gl_counts, "CCP1_GL")
    print(f"  GL rows with no PERM vendor: {sum(1 for v in gl_counts.values() if v == 0)}")
    for k, v in gl_summary.items():
        print(f"  {k}: {v}")
    results.append(gl_summary)
else:
    print(f"  SKIP — file not found: {GL_PATH} or {PERM_PATH}", file=sys.stderr)
    results.append({"label": "CCP1_GL", "key_count": 0, "note": "file not found"})

# CCP1 substrate B: Permutation table
# Key: the GL key; pattern: vendors under it (d_transIPID).  Taken from PERM alone, so it also
# covers any GL key the GL file might lack (the two key sets are equal by construction).
print(f"\n[3] CCP1 Permutation table: {PERM_PATH}")
if perm_vendors is not None:
    perm_summary = summarise(perm_vendors, "CCP1_PERM")
    for k, v in perm_summary.items():
        print(f"  {k}: {v}")
    results.append(perm_summary)
else:
    print(f"  SKIP — file not found: {PERM_PATH}", file=sys.stderr)
    results.append({"label": "CCP1_PERM", "key_count": 0, "note": "file not found"})

# CCP1 vs CCP2 gap summary
print("\n--- Gap summary ---")
ldgr_r = next((r for r in results if r.get("label") == "CCP2_LDGR"), {})
perm_r = next((r for r in results if r.get("label") == "CCP1_PERM"), {})
ldgr_multi = ldgr_r.get("keys_with_multiple_patterns", 0)
perm_multi  = perm_r.get("keys_with_multiple_patterns", 0)
if ldgr_multi and perm_multi:
    gap = ldgr_multi - perm_multi
    print(f"  IL keys with >1 distinct pattern  : {ldgr_multi}")
    print(f"  PERM keys with >1 vendor per key  : {perm_multi}")
    print(f"  Gap (IL - PERM)                   : {gap}")
    print(f"  Evidence: {'non-zero gap confirms non-injectivity' if gap != 0 else 'zero gap (check data)'}")

# ---------------------------------------------------------------------------
# Write evidence file
# ---------------------------------------------------------------------------
os.makedirs(os.path.dirname(EVIDENCE_OUT), exist_ok=True)

fieldnames = [
    "label", "key_count", "total_distinct_patterns",
    "mean_patterns_per_key", "max_patterns_per_key",
    "keys_with_multiple_patterns", "collapse_ratio",
    "run_timestamp", "ldgr_path", "perm_path", "gl_path"
]

ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
with open(EVIDENCE_OUT, "w", newline="", encoding="utf-8") as fh:
    writer = csv.DictWriter(fh, fieldnames=fieldnames, extrasaction="ignore")
    writer.writeheader()
    for r in results:
        r["run_timestamp"] = ts
        r["ldgr_path"]     = LDGR_PATH
        r["perm_path"]     = PERM_PATH
        r["gl_path"]       = GL_PATH
        writer.writerow(r)

print(f"\nEvidence written -> {EVIDENCE_OUT}")
print("AIM1 complete.")
