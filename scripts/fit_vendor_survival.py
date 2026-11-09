#!/usr/bin/env python3
"""
AIM3 -- Vendor survival model
Paper 2 Part 1 illustration §5.3

Fits a discrete-time survival model predicting whether a vendor will be present
next year (present_next_year == 1) based on features in view_vendor_year_survival.csv.

Input:
  --view  PATH   view_vendor_year_survival.csv (CCP3 step 5v output)

Output:
  data/test_evidence/AIM3_survival_results.csv

Acceptance thresholds (confirmed 2026-11-04, locked):
  C-statistic > 0.65
  Permutation p-value < 0.05
  Note: thresholds require >= 5 fiscal years.  1-year smoke run reports
        descriptive statistics only; C-statistic and p-value marked "n/a (1yr)".

Dependencies (optional):
  sklearn  -- for logistic regression and C-statistic (AUC)
  scipy    -- for permutation p-value
  If not available, descriptive statistics are reported instead.

AGENTS.md: Python only for post-engine bounded summary work.  Owner approval received
2026-11-05.  Reads only view_vendor_year_survival.csv (~6,850 rows for FY2003).
"""

import csv
import datetime
import os
import sys
from collections import defaultdict

# ---------------------------------------------------------------------------
# CLI arguments
# ---------------------------------------------------------------------------
VIEW_PATH    = "data/output_validate_fy03/output_validate_fy03.next.834/view_vendor_year_survival.csv"
EVIDENCE_OUT = "data/test_evidence/AIM3_survival_results.csv"
HEALTH_OUT   = None   # optional: vendor_health_score output path
# Eligible fiscal years (owner decision 2026-11-09): PO files are usable only for
# FY2006-08 and FY2010 onward (FY2001-05 are stubs/partial; FY2009 has no file).
ELIGIBLE_FY_SPEC = "2006-2008,2010-2017"

def parse_fy_spec(spec):
    """'2006-2008,2010-2017' -> {2006, 2007, 2008, 2010, ..., 2017}."""
    out = set()
    for part in spec.split(","):
        lo, _, hi = part.strip().partition("-")
        out.update(range(int(lo), int(hi or lo) + 1))
    return out

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--view" and i + 1 < len(sys.argv):
        VIEW_PATH = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--eligible-fy" and i + 1 < len(sys.argv):
        ELIGIBLE_FY_SPEC = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--evidence-out" and i + 1 < len(sys.argv):
        EVIDENCE_OUT = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--health-out" and i + 1 < len(sys.argv):
        HEALTH_OUT = sys.argv[i + 1]; i += 2
    else:
        print("Unknown argument: " + sys.argv[i], file=sys.stderr)
        sys.exit(1)

# ---------------------------------------------------------------------------
# Optional dependency check
# ---------------------------------------------------------------------------
try:
    from sklearn.linear_model import LogisticRegression
    from sklearn.metrics import roc_auc_score
    import scipy.stats as stats
    HAS_SKLEARN = True
except ImportError:
    HAS_SKLEARN = False
    print("INFO: sklearn/scipy not available — descriptive statistics only", file=sys.stderr)

# ---------------------------------------------------------------------------
# Load view
# ---------------------------------------------------------------------------

print("=" * 70)
print("AIM3 — Vendor survival model")
print("=" * 70)
print(f"\nInput: {VIEW_PATH}")

if not os.path.isfile(VIEW_PATH):
    print(f"ERROR: file not found: {VIEW_PATH}", file=sys.stderr)
    sys.exit(1)

rows = []
with open(VIEW_PATH, newline="", encoding="utf-8-sig") as fh:
    reader = csv.DictReader(fh)
    for row in reader:
        rows.append(row)

print(f"  Loaded {len(rows)} rows")

if not rows:
    print("ERROR: view file is empty", file=sys.stderr)
    sys.exit(1)

# ---------------------------------------------------------------------------
# Descriptive statistics (always — no dependency needed)
# ---------------------------------------------------------------------------

def safe_int(s, default=0):
    try: return int(s)
    except (ValueError, TypeError): return default

def safe_float(s, default=0.0):
    try: return float(s)
    except (ValueError, TypeError): return default

# The view's fiscal_year column may hold monthly periods ("YYYY/MM", calendar year/month)
# since monthly granularity.  Roll up to Virginia fiscal years (Jul-Jun; FY2003 =
# Jul 2002 - Jun 2003) so "present next year" means the next FISCAL YEAR, not the next month.
def fiscal_year_of(period):
    """'YYYY/MM' -> Virginia fiscal year (int); a bare 'YYYY' is taken as the fiscal year."""
    parts = period.split("/")
    try:
        year = int(parts[0])
        if len(parts) == 2:
            return year + 1 if int(parts[1]) >= 7 else year
        return year
    except ValueError:
        return None

by_vendor_fy = {}   # (vendor_id, fy) -> annual row
for r in rows:
    fy = fiscal_year_of(r.get("fiscal_year", ""))
    if fy is None:
        continue
    key = (r.get("vendor_id", ""), fy)
    a = by_vendor_fy.get(key)
    if a is None:
        a = by_vendor_fy[key] = {
            "vendor_id": key[0], "fiscal_year": fy, "po_count": 0.0, "total_amount": 0.0,
            "agency_count": 0.0, "distinct_nigp_classes": 0.0,
            "nigp_class_modal": r.get("nigp_class_modal", ""), "_modal_po": -1.0,
            "first_order_year": fiscal_year_of(r.get("first_order_year", "")),
        }
    pc = safe_float(r.get("po_count", 0))
    a["po_count"] += pc
    a["total_amount"] += safe_float(r.get("total_amount", 0))
    # Monthly distinct counts cannot be summed; the max is a lower bound for the year.
    a["agency_count"] = max(a["agency_count"], safe_float(r.get("agency_count", 0)))
    a["distinct_nigp_classes"] = max(a["distinct_nigp_classes"], safe_float(r.get("distinct_nigp_classes", 0)))
    if pc > a["_modal_po"]:
        a["_modal_po"], a["nigp_class_modal"] = pc, r.get("nigp_class_modal", "")
    foy = fiscal_year_of(r.get("first_order_year", ""))
    if foy is not None and (a["first_order_year"] is None or foy < a["first_order_year"]):
        a["first_order_year"] = foy

# Keep only eligible fiscal years; others are source gaps, not vendor churn.
ELIGIBLE_FY = parse_fy_spec(ELIGIBLE_FY_SPEC)
by_vendor_fy = {k: v for k, v in by_vendor_fy.items() if k[1] in ELIGIBLE_FY}

distinct_years = sorted({fy for (_, fy) in by_vendor_fy})
year_count     = len(distinct_years)
last_fy        = distinct_years[-1] if distinct_years else None
print(f"  Eligible fiscal years          : {ELIGIBLE_FY_SPEC}")

# present_next_year is derived from the data: vendor has a row in fiscal year + 1.
# A vendor-year is observable only if fiscal year + 1 is also eligible; otherwise the
# outcome is right-censored (last year, or the year before a source gap) and excluded.
all_annual = list(by_vendor_fy.values())
for a in all_annual:
    a["present_next_year"] = 1 if (a["vendor_id"], a["fiscal_year"] + 1) in by_vendor_fy else 0
    a["first_order_year"] = a["first_order_year"] if a["first_order_year"] is not None else a["fiscal_year"]
rows = [a for a in all_annual if (a["fiscal_year"] + 1) in ELIGIBLE_FY]

# Count vendors, churn rate (observed, non-censored vendor-years only)
total_vendor_years = len(rows)
present_next       = sum(1 for r in rows if r["present_next_year"] == 1)
churn_count        = total_vendor_years - present_next
churn_rate         = churn_count / total_vendor_years if total_vendor_years > 0 else None

distinct_vendors   = len(set(r["vendor_id"] for r in all_annual))

# Feature availability
feature_cols = ["po_count", "nigp_class_modal", "agency_count", "distinct_nigp_classes",
                "total_amount", "first_order_year"]
available_features = [c for c in feature_cols if c in (all_annual[0].keys() if all_annual else [])]

print(f"\nDescriptive statistics:")
print(f"  Total vendor-year observations : {total_vendor_years}  (excl. censored FY{last_fy})")
print(f"  Distinct vendors               : {distinct_vendors}")
print(f"  Fiscal years in data           : {year_count}  ({', '.join('FY%d' % y for y in distinct_years[:5])}{'...' if year_count > 5 else ''})")
print(f"  Present next year              : {present_next}")
print(f"  Churned (not present next yr)  : {churn_count}")
if churn_rate is None:
    print(f"  Annual churn rate              : n/a (needs >= 2 fiscal years)")
else:
    print(f"  Annual churn rate              : {churn_rate:.3f}  ({churn_rate * 100:.1f}%)")
print(f"  Available features             : {available_features}")

# Churn by first_order_year cohort
cohort_churn = defaultdict(lambda: [0, 0])
for r in rows:
    foy = r["first_order_year"]
    cohort_churn[foy][0] += 1
    if r["present_next_year"] == 0:
        cohort_churn[foy][1] += 1

print(f"\n  Churn by entry cohort (first 5):")
for cohort in sorted(cohort_churn)[:5]:
    total_c, churned_c = cohort_churn[cohort]
    print(f"    FY{cohort}: {churned_c}/{total_c} churned ({churned_c/total_c*100:.0f}%)")

# ---------------------------------------------------------------------------
# C-statistic via sklearn (if available and multi-year data)
# ---------------------------------------------------------------------------
c_stat   = None
perm_p   = None
n_perm   = None
method   = "descriptive_only"
verdict  = "n/a"
note     = ""

if year_count < 5:
    note = f"{year_count} fiscal year(s); C-statistic deferred until >= 5 fiscal years"
    print(f"\nINFO: {note}")
elif not HAS_SKLEARN:
    note = "sklearn/scipy not available; install to compute C-statistic"
    print(f"\nINFO: {note}")
else:
    # Build feature matrix and target
    X = []
    y = []
    valid_rows = [
        r for r in rows
        if "po_count" in r and "agency_count" in r and "distinct_nigp_classes" in r
    ]
    for r in valid_rows:
        feats = [
            safe_float(r.get("po_count", 0)),
            safe_float(r.get("agency_count", 0)),
            safe_float(r.get("distinct_nigp_classes", 0)),
            safe_float(r.get("total_amount", 0)),
        ]
        X.append(feats)
        y.append(safe_int(r.get("present_next_year", 0)))

    if len(X) < 20 or sum(y) < 5:
        note = f"Insufficient positive cases ({sum(y)}) or observations ({len(X)})"
        print(f"\nINFO: {note}", file=sys.stderr)
    else:
        method = "logistic_regression"
        clf = LogisticRegression(max_iter=1000, solver="lbfgs")
        clf.fit(X, y)
        probs = clf.predict_proba(X)[:, 1]
        c_stat = roc_auc_score(y, probs)

        # Permutation test
        import random
        random.seed(42)
        n_perm = 999
        y_shuf = y[:]
        c_perm_better = 0
        for _ in range(n_perm):
            random.shuffle(y_shuf)
            clf2 = LogisticRegression(max_iter=500, solver="lbfgs")
            clf2.fit(X, y_shuf)
            p2 = clf2.predict_proba(X)[:, 1]
            if roc_auc_score(y_shuf, p2) >= c_stat:
                c_perm_better += 1
        perm_p = (c_perm_better + 1) / (n_perm + 1)

        # Verdict
        passed = c_stat > 0.65 and perm_p < 0.05
        verdict = "PASS" if passed else "FAIL"
        note = f"C-stat={c_stat:.4f} (threshold>0.65), p={perm_p:.4f} (threshold<0.05)"
        print(f"\nModel results (logistic regression, in-sample):")
        print(f"  C-statistic : {c_stat:.4f}  ({'PASS' if c_stat > 0.65 else 'FAIL'} — threshold > 0.65)")
        print(f"  Permutation p : {perm_p:.4f}  ({'PASS' if perm_p < 0.05 else 'FAIL'} — threshold < 0.05)")
        print(f"  Verdict     : {verdict}")

if not note:
    note = "descriptive only"

# ---------------------------------------------------------------------------
# Vendor health score (simple: po_count × (1 - churn_indicator))
# ---------------------------------------------------------------------------
health_rows = []
for r in rows:
    vid = r["vendor_id"]
    fyr = r["fiscal_year"]
    pc  = r["po_count"]
    pny = r["present_next_year"]
    # Health score: log(po_count + 1) × 2 × present_next_year (simple proxy)
    import math
    hs = round(math.log(pc + 1) * 2 * pny, 4)
    health_rows.append({"vendor_id": vid, "fiscal_year": fyr, "vendor_health_score": hs})

if HEALTH_OUT:
    os.makedirs(os.path.dirname(HEALTH_OUT), exist_ok=True)
    with open(HEALTH_OUT, "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=["vendor_id", "fiscal_year", "vendor_health_score"])
        w.writeheader()
        w.writerows(health_rows)
    print(f"\nVendor health scores written -> {HEALTH_OUT}")

# ---------------------------------------------------------------------------
# Write evidence file
# ---------------------------------------------------------------------------
os.makedirs(os.path.dirname(EVIDENCE_OUT), exist_ok=True)

ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
evidence = {
    "test":                    "AIM3",
    "verdict":                 verdict,
    "method":                  method,
    "c_statistic":             round(c_stat, 4) if c_stat is not None else "n/a",
    "permutation_p":           round(perm_p, 4) if perm_p is not None else "n/a",
    "n_permutations":          n_perm if n_perm is not None else "n/a",
    "total_vendor_years":      total_vendor_years,
    "distinct_vendors":        distinct_vendors,
    "year_count":              year_count,
    "annual_churn_rate":       round(churn_rate, 4) if churn_rate is not None else "n/a",
    "threshold_c_stat":        "> 0.65",
    "threshold_perm_p":        "< 0.05",
    "note":                    note,
    "run_timestamp":           ts,
    "view_path":               VIEW_PATH,
}

fieldnames = list(evidence.keys())
with open(EVIDENCE_OUT, "w", newline="", encoding="utf-8") as fh:
    writer = csv.DictWriter(fh, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerow(evidence)

print(f"\nEvidence written -> {EVIDENCE_OUT}")
print("AIM3 complete.")
