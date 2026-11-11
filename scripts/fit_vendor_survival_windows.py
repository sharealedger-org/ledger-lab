#!/usr/bin/env python3
"""
AIM3 -- Vendor survival model, walk-forward windows + FY2015-16 hold-out
Paper 2 Part 1 illustration §5.3

Companion to fit_vendor_survival.py (which is fit in-sample and is left unchanged so earlier
evidence stays reproducible).  This script implements the locked evaluation design from
docs/TEST_DESIGN.md (AIM3): rolling train/test windows and a final FY2015-2016 hold-out, with
the locked thresholds (C-statistic > 0.65, permutation p < 0.05), unchanged.

Input:
  --view  PATH   view_vendor_year_survival.csv (CCP3 step 5v output)

Design decisions (fixed before the first run on real data, 2026-11-10; owner to confirm):
  1. Observable vendor-year: fiscal year t is observable only if t+1 is in the eligible set AND
     t+1 <= the last fiscal year that has data in the view.  Anything else is right-censored and
     excluded, never counted as churn.  (fit_vendor_survival.py did not apply the second test,
     so it counted every last-year vendor as churned.)
  2. The locked "7 windows" needs 8 contiguous observable fiscal years.  The script finds the
     longest contiguous run of observable years and uses an expanding window inside it: for each
     test year t after --min-train-years, train on all earlier years in the run, test on t.
     The number of windows actually available is printed.
  3. PRIMARY VERDICT = the hold-out: vendor-years whose outcome year (t+1) is in --holdout-outcome-fy
     (default 2015-2016), tested on a model trained on all earlier years of the run.  The walk-forward
     windows are reported as a stability check and do not change the verdict.
  4. Permutation test: shuffle the test-set labels and recompute the C-statistic of the fixed
     predictions (null: the predictions carry no information about outcomes).  This is valid for an
     out-of-sample fit and avoids refitting 999 models.
  5. Features and model are the same as fit_vendor_survival.py (po_count, agency_count,
     distinct_nigp_classes, total_amount; logistic regression), with standardisation fitted on the
     training years only.

Output:
  --evidence-out PATH  (default data/test_evidence/AIM3_survival_windows.csv)

AGENTS.md: Python only for post-engine bounded summary work.  Owner approval received 2026-11-10
for implementing the locked protocol.  Reads only view_vendor_year_survival.csv.
"""

import csv
import datetime
import os
import sys
from collections import defaultdict

import numpy as np

VIEW_PATH        = "data/output_validate_fy03/output_validate_fy03.next.834/view_vendor_year_survival.csv"
EVIDENCE_OUT     = "data/test_evidence/AIM3_survival_windows.csv"
ELIGIBLE_FY_SPEC = "2006-2008,2010-2017"
HOLDOUT_SPEC     = "2015-2016"
MIN_TRAIN_YEARS  = 2
N_PERM           = 999
SEED             = 42
LEGACY_CENSORING = False   # diagnostic only: reproduce fit_vendor_survival.py's censoring

def parse_fy_spec(spec):
    out = set()
    for part in spec.split(","):
        lo, _, hi = part.strip().partition("-")
        out.update(range(int(lo), int(hi or lo) + 1))
    return out

i = 1
while i < len(sys.argv):
    a = sys.argv[i]
    nxt = sys.argv[i + 1] if i + 1 < len(sys.argv) else None
    if a == "--view" and nxt:                  VIEW_PATH = nxt; i += 2
    elif a == "--evidence-out" and nxt:        EVIDENCE_OUT = nxt; i += 2
    elif a == "--eligible-fy" and nxt:         ELIGIBLE_FY_SPEC = nxt; i += 2
    elif a == "--holdout-outcome-fy" and nxt:  HOLDOUT_SPEC = nxt; i += 2
    elif a == "--min-train-years" and nxt:     MIN_TRAIN_YEARS = int(nxt); i += 2
    elif a == "--perm" and nxt:                N_PERM = int(nxt); i += 2
    elif a == "--legacy-censoring":            LEGACY_CENSORING = True; i += 1
    else:
        print("Unknown argument: " + a, file=sys.stderr); sys.exit(1)

try:
    from sklearn.linear_model import LogisticRegression
    from sklearn.preprocessing import StandardScaler
    from scipy.stats import rankdata
except ImportError:
    print("ERROR: numpy, scikit-learn and scipy are required", file=sys.stderr)
    sys.exit(1)

print("=" * 70)
print("AIM3 -- Vendor survival: walk-forward windows + hold-out")
print("=" * 70)
print(f"\nInput: {VIEW_PATH}")
if not os.path.isfile(VIEW_PATH):
    print(f"ERROR: file not found: {VIEW_PATH}", file=sys.stderr); sys.exit(1)

def fiscal_year_of(period):
    """'YYYY/MM' -> Virginia fiscal year (Jul-Jun); bare 'YYYY' is taken as the fiscal year."""
    parts = period.split("/")
    try:
        year = int(parts[0])
        if len(parts) == 2:
            return year + 1 if int(parts[1]) >= 7 else year
        return year
    except ValueError:
        return None

def sf(s):
    try: return float(s)
    except (ValueError, TypeError): return 0.0

# ---------------------------------------------------------------------------
# Roll the monthly view up to (vendor, fiscal year); track months seen per fiscal year
# ---------------------------------------------------------------------------
annual = {}
months_by_fy = defaultdict(set)
n_rows = 0
with open(VIEW_PATH, newline="", encoding="utf-8-sig") as fh:
    for r in csv.DictReader(fh):
        n_rows += 1
        period = r.get("fiscal_year", "")
        fy = fiscal_year_of(period)
        if fy is None:
            continue
        months_by_fy[fy].add(period)
        key = (r.get("vendor_id", ""), fy)
        a = annual.get(key)
        if a is None:
            a = annual[key] = [0.0, 0.0, 0.0, 0.0]   # po_count, agency_count, distinct_nigp, total_amount
        a[0] += sf(r.get("po_count"))
        a[1]  = max(a[1], sf(r.get("agency_count")))
        a[2]  = max(a[2], sf(r.get("distinct_nigp_classes")))
        a[3] += sf(r.get("total_amount"))
print(f"  Loaded {n_rows} rows -> {len(annual)} vendor-fiscal-year records")

ELIGIBLE = parse_fy_spec(ELIGIBLE_FY_SPEC)
HOLDOUT  = parse_fy_spec(HOLDOUT_SPEC)
# Last fiscal year with a full 12 months of data is the last year the outcome can be observed.
complete = sorted(fy for fy, m in months_by_fy.items() if len(m) >= 12)
data_last_fy = complete[-1] if complete else None
print(f"  Eligible fiscal years : {ELIGIBLE_FY_SPEC}")
print(f"  Last complete FY      : {data_last_fy}  (outcome observable up to this year)")
if LEGACY_CENSORING:
    print("  WARNING: --legacy-censoring: counting vendor-years with no observable next year as churn")
    data_last_fy = max(ELIGIBLE)

present = {k for k in annual if k[1] in ELIGIBLE}
def observable(fy):
    return fy in ELIGIBLE and (fy + 1) in ELIGIBLE and (fy + 1) <= data_last_fy

# ---------------------------------------------------------------------------
# Build the panel (one row per observable vendor-year)
# ---------------------------------------------------------------------------
fys, X, y = [], [], []
for (vid, fy), feats in annual.items():
    if not observable(fy):
        continue
    fys.append(fy); X.append(feats)
    y.append(1 if (vid, fy + 1) in present else 0)
fys = np.array(fys); X = np.array(X, dtype=float); y = np.array(y, dtype=int)
obs_years = sorted(set(fys.tolist()))
print(f"\nObservable vendor-years : {len(y)}  present next year {int(y.sum())}  "
      f"churn {100 * (1 - y.mean()):.1f}%")
for fy in obs_years:
    m = fys == fy
    print(f"    FY{fy}: n={int(m.sum()):6d}  churn {100 * (1 - y[m].mean()):5.1f}%")

# Longest contiguous run of observable years (ties -> the later run)
runs, cur = [], []
for fy in obs_years:
    if cur and fy == cur[-1] + 1: cur.append(fy)
    else:
        if cur: runs.append(cur)
        cur = [fy]
if cur: runs.append(cur)
run = max(runs, key=lambda r: (len(r), r[-1])) if runs else []
print(f"\nContiguous observable runs : {runs}")
print(f"Run used                   : FY{run[0]}-FY{run[-1]} ({len(run)} years)" if run else "No run")
print(f"Locked protocol needs 8 contiguous years for 7 windows; available run gives "
      f"{max(len(run) - MIN_TRAIN_YEARS, 0)} walk-forward windows (min train {MIN_TRAIN_YEARS}).")

rng = np.random.default_rng(SEED)

def fit_predict(train_mask, test_mask):
    sc = StandardScaler().fit(X[train_mask])
    clf = LogisticRegression(max_iter=1000, solver="lbfgs")
    clf.fit(sc.transform(X[train_mask]), y[train_mask])
    return clf.predict_proba(sc.transform(X[test_mask]))[:, 1]

def auc_perm(yt, s, n_perm):
    n_pos = int(yt.sum()); n_neg = len(yt) - n_pos
    if n_pos == 0 or n_neg == 0:
        return None, None
    r = rankdata(s)
    base = n_pos * (n_pos + 1) / 2.0
    auc = (r[yt == 1].sum() - base) / (n_pos * n_neg)
    if n_perm <= 0:
        return auc, None
    ge = 0
    for _ in range(n_perm):
        idx = rng.choice(len(yt), n_pos, replace=False)
        if (r[idx].sum() - base) / (n_pos * n_neg) >= auc:
            ge += 1
    return auc, (ge + 1) / (n_perm + 1)

rows = []
def record(kind, train_fys, test_fys, ntr, nte, npos, c, p, verdict):
    rows.append({"test": "AIM3", "kind": kind,
                 "train_fys": train_fys, "test_fys": test_fys,
                 "n_train": ntr, "n_test": nte, "n_test_present": npos,
                 "c_statistic": round(c, 4) if c is not None else "n/a",
                 "permutation_p": round(p, 4) if p is not None else "n/a",
                 "n_permutations": N_PERM if p is not None else "n/a",
                 "threshold_c_stat": "> 0.65", "threshold_perm_p": "< 0.05",
                 "verdict": verdict})

# ---------------------------------------------------------------------------
# In-sample reference (same panel, no permutations): sanity check only
# ---------------------------------------------------------------------------
if len(y) > 20 and y.sum() > 5:
    allm = np.ones(len(y), dtype=bool)
    c_in, _ = auc_perm(y, fit_predict(allm, allm), 0)
    print(f"\nIn-sample reference (all observable vendor-years, not a verdict): C = {c_in:.4f}")
    record("in_sample_reference", f"{obs_years[0]}-{obs_years[-1]}", "same", len(y), len(y),
           int(y.sum()), c_in, None, "reference only")

# ---------------------------------------------------------------------------
# Walk-forward windows (expanding) inside the contiguous run
# ---------------------------------------------------------------------------
print("\nWalk-forward windows (stability check; do not change the verdict):")
print(f"  {'train FYs':<14} {'test FY':<8} {'n_train':>8} {'n_test':>8} {'C':>7} {'perm p':>8}")
wf_c = []
for k in range(MIN_TRAIN_YEARS, len(run)):
    tr_years, te_year = run[:k], run[k]
    trm = np.isin(fys, tr_years); tem = fys == te_year
    c, p = auc_perm(y[tem], fit_predict(trm, tem), N_PERM)
    if c is None: continue
    wf_c.append(c)
    print(f"  {str(tr_years[0]) + '-' + str(tr_years[-1]):<14} {te_year:<8} {int(trm.sum()):>8} {int(tem.sum()):>8} {c:>7.4f} {p:>8.4f}")
    record("walk_forward", f"{tr_years[0]}-{tr_years[-1]}", str(te_year), int(trm.sum()), int(tem.sum()),
           int(y[tem].sum()), c, p, "stability only")
if wf_c:
    print(f"  mean window C = {np.mean(wf_c):.4f}  (min {min(wf_c):.4f}, max {max(wf_c):.4f}, {len(wf_c)} windows)")

# ---------------------------------------------------------------------------
# Hold-out: outcome year in HOLDOUT, trained on earlier years of the run  -> VERDICT
# ---------------------------------------------------------------------------
ho_test = sorted(t for t in run if (t + 1) in HOLDOUT)
ho_train = [t for t in run if t + 1 < min(HOLDOUT)]
verdict = "n/a"
print(f"\nHold-out (outcome years FY{min(HOLDOUT)}-FY{max(HOLDOUT)}):")
if ho_test and ho_train:
    trm = np.isin(fys, ho_train); tem = np.isin(fys, ho_test)
    c, p = auc_perm(y[tem], fit_predict(trm, tem), N_PERM)
    verdict = "PASS" if (c is not None and c > 0.65 and p < 0.05) else "FAIL"
    print(f"  train vendor-years FY{ho_train[0]}-FY{ho_train[-1]}  n={int(trm.sum())}")
    print(f"  test  vendor-years FY{ho_test[0]}-FY{ho_test[-1]}  n={int(tem.sum())}  present {int(y[tem].sum())}")
    print(f"  C-statistic : {c:.4f}  ({'PASS' if c > 0.65 else 'FAIL'} -- threshold > 0.65)")
    print(f"  Permutation p : {p:.4f}  ({'PASS' if p < 0.05 else 'FAIL'} -- threshold < 0.05, {N_PERM} permutations)")
    print(f"  Verdict (hold-out): {verdict}")
    record("holdout_verdict", f"{ho_train[0]}-{ho_train[-1]}", f"{ho_test[0]}-{ho_test[-1]}",
           int(trm.sum()), int(tem.sum()), int(y[tem].sum()), c, p, verdict)
else:
    print("  not computable: no test years or no training years inside the contiguous run")

# ---------------------------------------------------------------------------
# Evidence
# ---------------------------------------------------------------------------
ts = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
for r in rows:
    r["run_timestamp"] = ts; r["view_path"] = VIEW_PATH
    r["eligible_fy"] = ELIGIBLE_FY_SPEC; r["last_complete_fy"] = data_last_fy
    r["legacy_censoring"] = LEGACY_CENSORING
if os.path.dirname(EVIDENCE_OUT):
    os.makedirs(os.path.dirname(EVIDENCE_OUT), exist_ok=True)
with open(EVIDENCE_OUT, "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()) if rows else ["test"])
    w.writeheader(); w.writerows(rows)
print(f"\nEvidence written -> {EVIDENCE_OUT}")
print("AIM3 (windows) complete.")
