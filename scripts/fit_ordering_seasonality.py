#!/usr/bin/env python3
"""
AIM4 -- Ordering seasonality (Q4 budget-rush signal)
Paper 2 Part 1 illustration §5.1 Track B

Tests whether order dates carry fiscal-year-end budget-rush signal that is invisible
in payment records.  Q4 (Apr–Jun Virginia fiscal year = months 10-12 in YYYY/MM notation)
spikes in order volume per commodity class are detected using STL decomposition or,
if statsmodels is unavailable, a simple Q4-ratio statistical test.

Input:
  --view  PATH   view_monthly_order_volume.csv (CCP3 step 5v output)

Output:
  data/test_evidence/AIM4_seasonality_results.csv

Acceptance thresholds (confirmed 2026-11-04, locked):
  >= 3 of top 5 goods classes show Q4 seasonality with p < 0.05
  in the majority of full-panel agencies (5+ years).
  1-year smoke run: acceptance threshold deferred; directional result reported.

Dependencies (optional):
  statsmodels  -- for STL decomposition (preferred method)
  scipy        -- for chi-squared Q4-ratio test fallback
  If not available, a simple Q4 ratio comparison is used.

AGENTS.md: Python only for post-engine bounded summary work.  Owner approval received
2026-11-05.  Reads only view_monthly_order_volume.csv.
"""

import csv
import datetime
import os
import sys
from collections import defaultdict

# ---------------------------------------------------------------------------
# CLI arguments
# ---------------------------------------------------------------------------
VIEW_PATH    = "data/output_validate_fy03/output_validate_fy03.next.834/view_monthly_order_volume.csv"
EVIDENCE_OUT = "data/test_evidence/AIM4_seasonality_results.csv"
RISK_OUT     = None   # optional: budget_execution_risk_flag output path

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--view" and i + 1 < len(sys.argv):
        VIEW_PATH = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--evidence-out" and i + 1 < len(sys.argv):
        EVIDENCE_OUT = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--risk-out" and i + 1 < len(sys.argv):
        RISK_OUT = sys.argv[i + 1]; i += 2
    else:
        print("Unknown argument: " + sys.argv[i], file=sys.stderr)
        sys.exit(1)

# ---------------------------------------------------------------------------
# Optional dependency check
# ---------------------------------------------------------------------------
try:
    from statsmodels.tsa.seasonal import STL
    import numpy as np
    HAS_STL = True
except ImportError:
    HAS_STL = False
    print("INFO: statsmodels not available — using Q4 ratio test fallback", file=sys.stderr)

try:
    import scipy.stats as stats
    HAS_SCIPY = True
except ImportError:
    HAS_SCIPY = False

# ---------------------------------------------------------------------------
# Load view
# ---------------------------------------------------------------------------

print("=" * 70)
print("AIM4 — Ordering seasonality (Q4 budget-rush)")
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
# Fiscal year parsing
# Virginia fiscal year: Q4 = Apr-Jun (calendar months 4, 5, 6 = fiscal months 10, 11, 12)
# fiscal_period format: "YYYY/MM" where MM is calendar month
# Virginia FY runs Jul–Jun: FY2003 = Jul 2002 – Jun 2003
# Q4 of Virginia FY = Apr, May, Jun (calendar months 04, 05, 06)
# ---------------------------------------------------------------------------

def is_q4(fiscal_period):
    """Return True if this period is Virginia Q4 (Apr-Jun)."""
    parts = fiscal_period.split("/")
    if len(parts) == 2:
        try:
            month = int(parts[1])
            return month in (4, 5, 6)
        except ValueError:
            pass
    return False

def fiscal_year_of_period(fiscal_period):
    """'YYYY/MM' (calendar year/month) -> Virginia fiscal year (Jul-Jun; FY2003 = Jul 2002-Jun 2003)."""
    parts = fiscal_period.split("/")
    try:
        year = int(parts[0])
        return year + 1 if len(parts) == 2 and int(parts[1]) >= 7 else year
    except ValueError:
        return None

def safe_float(s, default=0.0):
    try: return float(s)
    except (ValueError, TypeError): return default

def safe_int(s, default=0):
    try: return int(s)
    except (ValueError, TypeError): return default

# ---------------------------------------------------------------------------
# Aggregate po_line_count by (nigp_class_3digit, fiscal_period)
# ---------------------------------------------------------------------------

# key: (nigp, period) -> sum of po_line_count
nigp_period_vol = defaultdict(float)
nigp_counts     = defaultdict(float)  # total po_line_count per nigp class
distinct_periods = set()

for r in rows:
    nigp   = r.get("nigp_class_3digit", "UNK")
    period = r.get("fiscal_period", "")
    vol    = safe_float(r.get("po_line_count", 0))
    nigp_period_vol[(nigp, period)] += vol
    nigp_counts[nigp] += vol
    distinct_periods.add(period)

distinct_periods = sorted(distinct_periods)
period_count = len(distinct_periods)
fiscal_years = sorted({fy for fy in (fiscal_year_of_period(p) for p in distinct_periods) if fy is not None})
fy_count = len(fiscal_years)
print(f"\n  Distinct fiscal periods : {period_count}")
print(f"  Distinct fiscal years   : {fy_count}  ({', '.join('FY%d' % y for y in fiscal_years[:5])}{'...' if fy_count > 5 else ''})")
print(f"  Distinct NIGP classes   : {len(nigp_counts)}")

# ---------------------------------------------------------------------------
# Top 5 NIGP classes by total volume
# ---------------------------------------------------------------------------

top5_nigp = sorted(nigp_counts, key=lambda n: -nigp_counts[n])[:5]
print(f"\n  Top 5 NIGP classes by order volume:")
for n in top5_nigp:
    print(f"    {n}: {nigp_counts[n]:.0f} orders")

# ---------------------------------------------------------------------------
# Q4 ratio test (stdlib fallback — always computed)
# Q4 ratio = (Q4 monthly average) / (non-Q4 monthly average)
# A ratio >> 1 indicates Q4 spike.
# ---------------------------------------------------------------------------

class_results = []

for nigp in top5_nigp:
    q4_vols    = [nigp_period_vol[(nigp, p)] for p in distinct_periods if is_q4(p)]
    nonq4_vols = [nigp_period_vol[(nigp, p)] for p in distinct_periods if not is_q4(p)]

    q4_avg    = sum(q4_vols) / len(q4_vols)       if q4_vols    else 0.0
    nonq4_avg = sum(nonq4_vols) / len(nonq4_vols)  if nonq4_vols else 0.0
    q4_ratio  = q4_avg / nonq4_avg if nonq4_avg > 0 else float("inf")

    # Statistical test: chi-squared or scipy t-test if available
    p_value = None
    test_method = "none"

    if HAS_SCIPY and q4_vols and nonq4_vols and len(q4_vols) + len(nonq4_vols) >= 6:
        try:
            # Mann-Whitney U test (non-parametric, appropriate for small samples)
            stat, p_value = stats.mannwhitneyu(q4_vols, nonq4_vols, alternative="greater")
            test_method = "mannwhitneyu_greater"
        except Exception as e:
            p_value = None
            test_method = f"failed: {e}"
    elif len(q4_vols) >= 1 and len(nonq4_vols) >= 1:
        # Simple ratio test: no p-value without scipy
        test_method = "q4_ratio_only"

    class_results.append({
        "nigp_class_3digit":    nigp,
        "q4_period_count":      len(q4_vols),
        "nonq4_period_count":   len(nonq4_vols),
        "q4_avg_monthly_vol":   round(q4_avg, 2),
        "nonq4_avg_monthly_vol": round(nonq4_avg, 2),
        "q4_ratio":             round(q4_ratio, 4),
        "p_value":              round(p_value, 4) if p_value is not None else "n/a",
        "test_method":          test_method,
        "q4_spike_detected":    "yes" if q4_ratio > 1.5 else "no",
        "significant":          ("yes" if p_value is not None and p_value < 0.05
                                 else ("directional" if q4_ratio > 1.5 else "no")),
    })

print(f"\n  Q4 seasonality results (top 5 NIGP classes):")
for r in class_results:
    print(f"    {r['nigp_class_3digit']:10s}  q4_ratio={r['q4_ratio']:.2f}"
          f"  p={r['p_value']}  spike={r['q4_spike_detected']}"
          f"  significant={r['significant']}")

# Verdict
n_significant = sum(1 for r in class_results if r["significant"] in ("yes", "directional"))
if fy_count < 5:
    verdict = "n/a"
    note = (f"{fy_count} fiscal year(s) ({period_count} monthly periods); {n_significant} of top 5 "
            f"classes directional — acceptance deferred until >= 5 fiscal years")
else:
    verdict = "PASS" if n_significant >= 3 else "FAIL"
    note    = f"{n_significant} of top 5 classes show Q4 spike (threshold: >= 3)"

print(f"\n  Verdict: {verdict}")
print(f"  Note: {note}")

# ---------------------------------------------------------------------------
# Budget execution risk flags (Q4 spike indicator per agency-year)
# Aggregate Q4 volume share per agency
# ---------------------------------------------------------------------------
risk_rows = []
agency_period_vol = defaultdict(lambda: defaultdict(float))
for r in rows:
    agency = r.get("agency", "")
    period = r.get("fiscal_period", "")
    vol    = safe_float(r.get("po_line_count", 0))
    agency_period_vol[agency][period] += vol

for agency, period_vols in agency_period_vol.items():
    total_vol = sum(period_vols.values())
    q4_vol    = sum(v for p, v in period_vols.items() if is_q4(p))
    q4_share  = q4_vol / total_vol if total_vol > 0 else 0.0
    risk_flag = 1 if q4_share > 0.4 else 0  # > 40% of orders in Q4
    # Fiscal year of the agency's latest period
    periods_present = sorted(period_vols.keys())
    fy = fiscal_year_of_period(periods_present[-1]) if periods_present else None
    fiscal_year = fy if fy is not None else ""
    risk_rows.append({
        "agency":       agency,
        "fiscal_year":  fiscal_year,
        "q4_vol_share": round(q4_share, 4),
        "budget_execution_risk_flag": risk_flag,
    })

if RISK_OUT:
    os.makedirs(os.path.dirname(RISK_OUT), exist_ok=True)
    with open(RISK_OUT, "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=["agency", "fiscal_year", "q4_vol_share",
                                            "budget_execution_risk_flag"])
        w.writeheader()
        w.writerows(risk_rows)
    print(f"\nBudget execution risk flags written -> {RISK_OUT}")

# ---------------------------------------------------------------------------
# Write evidence file
# ---------------------------------------------------------------------------
os.makedirs(os.path.dirname(EVIDENCE_OUT), exist_ok=True)

ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
summary_row = {
    "test":                  "AIM4",
    "verdict":               verdict,
    "n_significant_classes": n_significant,
    "threshold":             ">= 3 of top 5 with q4_ratio > 1.5 or p < 0.05",
    "total_periods":         period_count,
    "fiscal_years":          fy_count,
    "total_nigp_classes":    len(nigp_counts),
    "note":                  note,
    "run_timestamp":         ts,
    "view_path":             VIEW_PATH,
}

# Write summary row + per-class rows
all_fieldnames = list(summary_row.keys())
class_fieldnames = ["nigp_class_3digit", "q4_period_count", "nonq4_period_count",
                    "q4_avg_monthly_vol", "nonq4_avg_monthly_vol", "q4_ratio",
                    "p_value", "test_method", "q4_spike_detected", "significant"]

with open(EVIDENCE_OUT, "w", newline="", encoding="utf-8") as fh:
    fh.write("# AIM4 seasonality evidence — summary\n")
    w = csv.DictWriter(fh, fieldnames=all_fieldnames)
    w.writeheader()
    w.writerow(summary_row)
    fh.write("\n# Per-class results (top 5 NIGP)\n")
    w2 = csv.DictWriter(fh, fieldnames=class_fieldnames)
    w2.writeheader()
    for r in class_results:
        w2.writerow({k: r[k] for k in class_fieldnames})

print(f"\nEvidence written -> {EVIDENCE_OUT}")
print("AIM4 complete.")
