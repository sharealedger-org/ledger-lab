#!/usr/bin/env python3
"""
AIM3 balance-derived baseline — compares posted-ledger features against the
commitment-pattern model (fit_vendor_survival_windows.py).

Uses the same eligible-year rule, hold-out split, churn definition, and
evaluation protocol as the commitment-pattern model.  Features are derived
from the Instrument Ledger (LDGR) per vendor-year rather than from the
order-pattern view.

Purpose: answer whether the AIM3 signal (C = 0.7527) exceeds what is
visible from posted totals alone.  If the balance-derived C is close to
0.7527, the commitment-pattern view adds no information beyond posting.
If it is substantially lower, the view features carry information lost at
posting time.

Inputs:
  --ldgr-root  PATH   directory containing FY{YY}/LDGR20{YY}.csv Gold files
  --view       PATH   view_vendor_year_survival.csv (for the churn label and
                      eligible-year coverage check)

Outputs:
  --evidence-out PATH  (default data/test_evidence/AIM3_balance_baseline.csv)

Features (balance-derived, per vendor-year, from LDGR):
  debit_amount    sum of ldgrTransAmount where ldgrDirVsOffsetFlg == 'D'
  credit_amount   sum of ldgrTransAmount where ldgrDirVsOffsetFlg == 'O'
  row_count       number of LDGR rows
  distinct_accts  distinct ldgrNominalAccountID values
  distinct_periods distinct ldgrLedgerPeriod values

Time-trend control (reported separately): logistic on [fiscal_year] only.

AGENTS.md: Python for post-engine bounded summary work.  Owner approval for
AIM4/AIM5 Python received in the 2026-11-10 session; restatement per
AGENTS.md: this is post-engine analysis on bounded LDGR summary tables.
Could Scala do this? -- The STL or logistic fit is Python; the LDGR
aggregation feeding it could be a Scala step, but the bounded table (one
row per vendor-year per year) is small (< 50K rows per year) and is read
directly here.  Approved under the same terms as fit_vendor_survival_windows.py.

Reads only LDGR20YY.csv files and the survival view CSV.  Does not touch
the raw corpus or the measured financial path.
"""

import csv
import datetime
import os
import sys
from collections import defaultdict

import numpy as np

# ---------------------------------------------------------------------------
# Defaults
# ---------------------------------------------------------------------------
LDGR_ROOT    = os.path.expanduser(
    "~/Library/CloudStorage/OneDrive-Personal/LedgerLabOutputs-Gold")
VIEW_PATH    = os.path.expanduser(
    "~/Library/CloudStorage/OneDrive-Personal/LedgerLabOutputs-Gold/"
    "VIEWS/view_vendor_year_survival.csv")
EVIDENCE_OUT = "data/test_evidence/AIM3_balance_baseline.csv"
ELIGIBLE_FY_SPEC = "2006-2008,2010-2017"
HOLDOUT_SPEC     = "2015-2016"
MIN_TRAIN_YEARS  = 2
N_PERM           = 999
SEED             = 42
# FY range to load from LDGR (inclusive); extend if data is outside FY03-FY16
FY_RANGE_START   = 3
FY_RANGE_END     = 16

# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
i = 1
while i < len(sys.argv):
    a = sys.argv[i]
    nxt = sys.argv[i + 1] if i + 1 < len(sys.argv) else None
    if   a == "--ldgr-root"   and nxt: LDGR_ROOT = nxt;       i += 2
    elif a == "--view"        and nxt: VIEW_PATH = nxt;        i += 2
    elif a == "--evidence-out"and nxt: EVIDENCE_OUT = nxt;     i += 2
    elif a == "--eligible-fy" and nxt: ELIGIBLE_FY_SPEC = nxt; i += 2
    elif a == "--perm"        and nxt: N_PERM = int(nxt);      i += 2
    else:
        print("Unknown argument: " + a, file=sys.stderr); sys.exit(1)

try:
    from sklearn.linear_model import LogisticRegression
    from sklearn.preprocessing import StandardScaler
    from scipy.stats import rankdata
except ImportError:
    print("ERROR: numpy, scikit-learn and scipy required", file=sys.stderr)
    sys.exit(1)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def parse_fy_spec(spec):
    out = set()
    for part in spec.split(","):
        lo, _, hi = part.strip().partition("-")
        out.update(range(int(lo), int(hi or lo) + 1))
    return out


def fiscal_year_of(period):
    """'YYYY/MM' or '00YY/MM' -> Virginia fiscal year (Jul=month 7 starts new FY)."""
    parts = period.strip().split("/")
    try:
        yr = int(parts[0])
        if yr < 100:
            yr += 2000
        if len(parts) == 2:
            return yr + 1 if int(parts[1]) >= 7 else yr
        return yr
    except (ValueError, IndexError):
        return None


def sf(s):
    try: return float(s)
    except (ValueError, TypeError): return 0.0


ELIGIBLE = parse_fy_spec(ELIGIBLE_FY_SPEC)
HOLDOUT  = parse_fy_spec(HOLDOUT_SPEC)

# ---------------------------------------------------------------------------
# 1. Load churn labels from the survival view
# ---------------------------------------------------------------------------
print("=" * 70)
print("AIM3 balance-derived baseline")
print("=" * 70)
print(f"\nView (labels): {VIEW_PATH}")
if not os.path.isfile(VIEW_PATH):
    print(f"ERROR: view not found: {VIEW_PATH}", file=sys.stderr); sys.exit(1)

# Build the set of (vendor_id, fiscal_year) pairs present in the view
present_in_view = set()
months_by_fy = defaultdict(set)
n_view_rows = 0
with open(VIEW_PATH, newline="", encoding="utf-8-sig") as fh:
    for r in csv.DictReader(fh):
        n_view_rows += 1
        period = r.get("fiscal_year", "")
        fy = fiscal_year_of(period)
        if fy is None:
            continue
        months_by_fy[fy].add(period)
        vid = r.get("vendor_id", "")
        present_in_view.add((vid, fy))

complete = sorted(fy for fy, m in months_by_fy.items() if len(m) >= 12)
data_last_fy = complete[-1] if complete else None
print(f"  View rows: {n_view_rows}  vendor-year pairs: {len(present_in_view)}")
print(f"  Last complete FY from view: {data_last_fy}")

# ---------------------------------------------------------------------------
# 2. Load LDGR files and build balance features per (vendor_id, fiscal_year)
# ---------------------------------------------------------------------------
print(f"\nLDGR root: {LDGR_ROOT}")

# feats[vendor_id][fiscal_year] = [debit_amount, credit_amount, row_count,
#                                   distinct_accts (set), distinct_periods (set)]
feats_raw = defaultdict(lambda: defaultdict(lambda: [0.0, 0.0, 0, set(), set()]))

years_loaded = []
for yy in range(FY_RANGE_START, FY_RANGE_END + 1):
    yyyy = 2000 + yy
    path = os.path.join(LDGR_ROOT, f"FY{yy:02d}", f"LDGR{yyyy}.csv")
    if not os.path.isfile(path):
        continue
    n = 0
    with open(path, newline="", encoding="utf-8-sig") as fh:
        reader = csv.DictReader(fh)
        if reader.fieldnames is None:
            print(f"  SKIP {path}: no header")
            continue
        fnames = reader.fieldnames
        has_dir = "ldgrDirVsOffsetFlg" in fnames
        has_acct = "ldgrNominalAccountID" in fnames
        has_period = "ldgrLedgerPeriod" in fnames
        has_amount = "ldgrTransAmount" in fnames
        if not ("ldgrIPID" in fnames and has_amount and has_period):
            print(f"  SKIP {path}: missing required columns")
            continue
        for row in reader:
            n += 1
            vid = row["ldgrIPID"]
            period = row["ldgrLedgerPeriod"] if has_period else ""
            fy = fiscal_year_of(period)
            if fy is None:
                continue
            amt = sf(row["ldgrTransAmount"] if has_amount else "0")
            flag = (row.get("ldgrDirVsOffsetFlg", "D") if has_dir else "D").strip()
            f = feats_raw[vid][fy]
            if flag == "D":
                f[0] += amt
            else:
                f[1] += amt
            f[2] += 1
            if has_acct:
                f[3].add(row["ldgrNominalAccountID"])
            if has_period:
                f[4].add(period)
    years_loaded.append(yyyy)
    print(f"  FY{yyyy}: {n} rows loaded")

if not feats_raw:
    print("ERROR: no LDGR files loaded", file=sys.stderr); sys.exit(1)

# Collapse sets to counts
feats = {}   # (vendor_id, fy) -> [debit, credit, rows, accts, periods]
for vid, fy_dict in feats_raw.items():
    for fy, f in fy_dict.items():
        feats[(vid, fy)] = [f[0], f[1], f[2], len(f[3]), len(f[4])]

print(f"\n  LDGR years loaded: {years_loaded}")
print(f"  Vendor-year records from LDGR: {len(feats)}")

# ---------------------------------------------------------------------------
# 3. Build panel: observable vendor-years with LDGR features + churn label
# ---------------------------------------------------------------------------

def observable(fy):
    return fy in ELIGIBLE and (fy + 1) in ELIGIBLE and (fy + 1) <= data_last_fy

fys_list, X_list, y_list = [], [], []
skipped_no_ldgr = 0
for (vid, fy) in present_in_view:
    if not observable(fy):
        continue
    feat = feats.get((vid, fy))
    if feat is None:
        skipped_no_ldgr += 1
        continue
    fys_list.append(fy)
    X_list.append(feat)
    y_list.append(1 if (vid, fy + 1) in present_in_view else 0)

fys_arr = np.array(fys_list)
X_arr   = np.array(X_list, dtype=float)
y_arr   = np.array(y_list, dtype=int)

obs_years = sorted(set(fys_arr.tolist()))
print(f"\nPanel size: {len(y_arr)} observable vendor-years "
      f"(skipped {skipped_no_ldgr} with no LDGR match)")
print(f"  Present next year: {int(y_arr.sum())}  churn: {100*(1-y_arr.mean()):.1f}%")
for fy in obs_years:
    m = fys_arr == fy
    print(f"    FY{fy}: n={int(m.sum()):6d}  churn {100*(1-y_arr[m].mean()):5.1f}%")

# ---------------------------------------------------------------------------
# 4. Fit logistic regression models
# ---------------------------------------------------------------------------
rng = np.random.default_rng(SEED)

def fit_predict(X, y, train_mask, test_mask):
    sc  = StandardScaler().fit(X[train_mask])
    clf = LogisticRegression(max_iter=1000, solver="lbfgs")
    clf.fit(sc.transform(X[train_mask]), y[train_mask])
    return clf.predict_proba(sc.transform(X[test_mask]))[:, 1]


def auc_perm(yt, s, n_perm):
    n_pos = int(yt.sum()); n_neg = len(yt) - n_pos
    if n_pos == 0 or n_neg == 0:
        return None, None
    r    = rankdata(s)
    base = n_pos * (n_pos + 1) / 2.0
    auc  = (r[yt == 1].sum() - base) / (n_pos * n_neg)
    if n_perm <= 0:
        return auc, None
    ge = 0
    for _ in range(n_perm):
        idx = rng.choice(len(yt), n_pos, replace=False)
        if (r[idx].sum() - base) / (n_pos * n_neg) >= auc:
            ge += 1
    return auc, (ge + 1) / (n_perm + 1)


# -- Walk-forward windows (stability) --
runs, cur = [], []
for fy in obs_years:
    if cur and fy == cur[-1] + 1: cur.append(fy)
    else:
        if cur: runs.append(cur)
        cur = [fy]
if cur: runs.append(cur)
run = max(runs, key=lambda r: (len(r), r[-1])) if runs else []

print(f"\nWalk-forward (balance features):")
wf_rows = []
for i_w, test_fy in enumerate(run[MIN_TRAIN_YEARS:], start=MIN_TRAIN_YEARS):
    train_fys = set(run[:i_w])
    tr = np.isin(fys_arr, list(train_fys))
    te = fys_arr == test_fy
    if tr.sum() < 2 or te.sum() < 2:
        continue
    scores = fit_predict(X_arr, y_arr, tr, te)
    c, p   = auc_perm(y_arr[te], scores, 0)  # no perm for walk-forward
    print(f"  train FY{min(train_fys)}-{max(train_fys)}  test FY{test_fy}  "
          f"n_train={int(tr.sum())}  n_test={int(te.sum())}  C={c:.4f}")
    wf_rows.append({"test_fy": test_fy, "c": c})

if wf_rows:
    wf_mean = sum(r["c"] for r in wf_rows) / len(wf_rows)
    print(f"  mean window C = {wf_mean:.4f}")

# -- Hold-out (primary verdict) --
print("\nHold-out (balance features):")
holdout_train_fys = set(obs_years) - HOLDOUT
holdout_test_fys  = set(obs_years) & {fy for fy in obs_years if fy + 1 in HOLDOUT}
tr_mask = np.isin(fys_arr, list(holdout_train_fys))
te_mask = np.isin(fys_arr, list(holdout_test_fys))
print(f"  train FYs: {sorted(holdout_train_fys)}  n={int(tr_mask.sum())}")
print(f"  test FYs:  {sorted(holdout_test_fys)}   n={int(te_mask.sum())}")

if tr_mask.sum() < 2 or te_mask.sum() < 2:
    print("  ERROR: not enough data for hold-out", file=sys.stderr)
    sys.exit(1)

ho_scores = fit_predict(X_arr, y_arr, tr_mask, te_mask)
ho_c, ho_p = auc_perm(y_arr[te_mask], ho_scores, N_PERM)
print(f"  C-statistic (balance): {ho_c:.4f}  (commitment model: 0.7527)")
print(f"  Permutation p          : {ho_p:.4f}  ({N_PERM} permutations)")
verdict_bal = "PASS" if (ho_c is not None and ho_c > 0.65) else "FAIL"
print(f"  Verdict                : {verdict_bal}")
delta = ho_c - 0.7527 if ho_c is not None else None
print(f"  Delta vs commitment    : {delta:+.4f}" if delta is not None else "")
if delta is not None:
    if delta >= 0.01:
        print("  Interpretation: balance features match or exceed commitment model")
    elif delta >= -0.02:
        print("  Interpretation: balance and commitment models are similar; "
              "view adds little beyond posted totals")
    else:
        print("  Interpretation: commitment model exceeds balance baseline; "
              "order-pattern view carries information lost at posting")

# -- Time-trend control: year feature only --
print("\nTime-trend control (year as sole feature):")
X_trend = fys_arr.reshape(-1, 1).astype(float)
tr_scores_trend = fit_predict(X_trend, y_arr, tr_mask, te_mask)
tr_c, tr_p = auc_perm(y_arr[te_mask], tr_scores_trend, N_PERM)
print(f"  C-statistic (year only): {tr_c:.4f}")
print(f"  Permutation p           : {tr_p:.4f}")
verdict_trend = "PASS" if (tr_c is not None and tr_c > 0.65) else "FAIL"
print(f"  Verdict                 : {verdict_trend}")

# ---------------------------------------------------------------------------
# 5. Write evidence
# ---------------------------------------------------------------------------
os.makedirs(os.path.dirname(EVIDENCE_OUT) if os.path.dirname(EVIDENCE_OUT) else ".", exist_ok=True)

ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
fieldnames = [
    "run_timestamp", "model", "train_fy_range", "test_fy_range",
    "n_train", "n_test", "c_statistic", "perm_p", "n_perm",
    "delta_vs_commitment", "verdict", "note"
]
wf_n = len(wf_rows)
rows = [
    {
        "run_timestamp": ts,
        "model": "balance_holdout",
        "train_fy_range": f"{min(holdout_train_fys)}-{max(holdout_train_fys)}",
        "test_fy_range": f"{min(holdout_test_fys)}-{max(holdout_test_fys)}",
        "n_train": int(tr_mask.sum()),
        "n_test": int(te_mask.sum()),
        "c_statistic": round(ho_c, 4) if ho_c is not None else "",
        "perm_p": round(ho_p, 4) if ho_p is not None else "",
        "n_perm": N_PERM,
        "delta_vs_commitment": round(delta, 4) if delta is not None else "",
        "verdict": verdict_bal,
        "note": "balance-derived features: debit_amount, credit_amount, row_count, "
                "distinct_accts, distinct_periods from LDGR"
    },
    {
        "run_timestamp": ts,
        "model": "time_trend_control",
        "train_fy_range": f"{min(holdout_train_fys)}-{max(holdout_train_fys)}",
        "test_fy_range": f"{min(holdout_test_fys)}-{max(holdout_test_fys)}",
        "n_train": int(tr_mask.sum()),
        "n_test": int(te_mask.sum()),
        "c_statistic": round(tr_c, 4) if tr_c is not None else "",
        "perm_p": round(tr_p, 4) if tr_p is not None else "",
        "n_perm": N_PERM,
        "delta_vs_commitment": round(tr_c - 0.7527, 4) if tr_c is not None else "",
        "verdict": verdict_trend,
        "note": "time-trend control: fiscal_year as sole feature"
    },
    {
        "run_timestamp": ts,
        "model": "commitment_model_reference",
        "train_fy_range": "2010-2013",
        "test_fy_range": "2014-2015",
        "n_train": 71220,
        "n_test": 52448,
        "c_statistic": 0.7527,
        "perm_p": 0.001,
        "n_perm": 999,
        "delta_vs_commitment": 0.0,
        "verdict": "PASS",
        "note": "reference: fit_vendor_survival_windows.py on Gold views (2026-11-13)"
    },
]

with open(EVIDENCE_OUT, "w", newline="", encoding="utf-8") as fh:
    writer = csv.DictWriter(fh, fieldnames=fieldnames)
    writer.writeheader()
    writer.writerows(rows)

print(f"\nEvidence written -> {EVIDENCE_OUT}")
print("AIM3 balance-derived baseline complete.")
