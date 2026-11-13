#!/usr/bin/env python3
"""
AIM3 combined model: posted-ledger (LDGR) features plus commitment (PO view) features.

Question: do pre-posting commitment patterns add predictive signal beyond posted totals?

Panel and split follow fit_vendor_survival_balance.py exactly: rows are vendor-years
observed in the survival view with LDGR features; the label is presence next year;
training and hold-out years use the same rule as the balance baseline.

Models compared on the same rows and split:
  balance_only   : 5 LDGR features (replicates AIM3_balance_baseline.csv)
  combined       : 5 LDGR features + 4 view features (po_count, agency_count,
                   distinct_nigp_classes, total_amount)
  time_trend     : fiscal_year only

Decision rule, declared before the run: commitment features add signal only if
combined C exceeds balance_only C and the bootstrap 95% interval for the difference
(resampling hold-out rows) lies entirely above zero. Otherwise the result is reported
as no reliable gain.

Output: data/test_evidence/AIM3_combined_model.csv
Run from the ledger-lab root with the ARM venv:
  ~/.venvs/ledger-lab-arm/bin/python scripts/fit_vendor_survival_combined.py
"""
import csv
import datetime
import os
import sys
from collections import defaultdict

import numpy as np
from scipy.stats import rankdata
from sklearn.linear_model import LogisticRegression
from sklearn.preprocessing import StandardScaler

LDGR_ROOT = os.path.expanduser("~/Library/CloudStorage/OneDrive-Personal/LedgerLabOutputs-Gold")
VIEW_PATH = os.path.join(LDGR_ROOT, "VIEWS", "view_vendor_year_survival.csv")
OUT = "data/test_evidence/AIM3_combined_model.csv"
ELIGIBLE_FY_SPEC = "2006-2008,2010-2017"
HOLDOUT_SPEC = "2015-2016"
FY_RANGE_START, FY_RANGE_END = 3, 16
N_PERM = 999
N_BOOT = 200
SEED = 42


def parse_fy_spec(spec):
    out = set()
    for part in spec.split(","):
        lo, _, hi = part.strip().partition("-")
        out.update(range(int(lo), int(hi or lo) + 1))
    return out


def fiscal_year_of(period):
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
    try:
        return float(s)
    except (ValueError, TypeError):
        return 0.0


def auc(yt, s):
    n_pos = int(yt.sum())
    n_neg = len(yt) - n_pos
    r = rankdata(s)
    return (r[yt == 1].sum() - n_pos * (n_pos + 1) / 2.0) / (n_pos * n_neg)


def perm_p(yt, s, c_obs, n_perm, rng):
    r = rankdata(s)
    n_pos = int(yt.sum())
    n_neg = len(yt) - n_pos
    base = n_pos * (n_pos + 1) / 2.0
    ge = 0
    for _ in range(n_perm):
        idx = rng.choice(len(yt), n_pos, replace=False)
        if (r[idx].sum() - base) / (n_pos * n_neg) >= c_obs:
            ge += 1
    return (ge + 1) / (n_perm + 1)


ELIGIBLE = parse_fy_spec(ELIGIBLE_FY_SPEC)
HOLDOUT = parse_fy_spec(HOLDOUT_SPEC)

# 1. View: presence (labels), per-vendor-year view features, last complete FY
if not os.path.isfile(VIEW_PATH):
    print(f"ERROR: view not found: {VIEW_PATH}", file=sys.stderr)
    sys.exit(1)
present = set()
view_feats = {}
months_by_fy = defaultdict(set)
with open(VIEW_PATH, newline="", encoding="utf-8-sig") as fh:
    for r in csv.DictReader(fh):
        period = r.get("fiscal_year", "")
        fy = fiscal_year_of(period)
        if fy is None:
            continue
        months_by_fy[fy].add(period)
        key = (r.get("vendor_id", ""), fy)
        present.add(key)
        a = view_feats.setdefault(key, [0.0, 0.0, 0.0, 0.0])
        a[0] += sf(r.get("po_count"))
        a[1] = max(a[1], sf(r.get("agency_count")))
        a[2] = max(a[2], sf(r.get("distinct_nigp_classes")))
        a[3] += sf(r.get("total_amount"))
complete = sorted(fy for fy, m in months_by_fy.items() if len(m) >= 12)
data_last_fy = complete[-1] if complete else None
print(f"View vendor-year pairs: {len(present)}  last complete FY: {data_last_fy}")


def observable(fy):
    return fy in ELIGIBLE and (fy + 1) in ELIGIBLE and (fy + 1) <= data_last_fy


# 2. LDGR: balance features per (vendor, fiscal year)
feats_raw = defaultdict(lambda: defaultdict(lambda: [0.0, 0.0, 0, set(), set()]))
for yy in range(FY_RANGE_START, FY_RANGE_END + 1):
    path = os.path.join(LDGR_ROOT, f"FY{yy:02d}", f"LDGR{2000 + yy}.csv")
    if not os.path.isfile(path):
        continue
    with open(path, newline="", encoding="utf-8-sig") as fh:
        reader = csv.DictReader(fh)
        if reader.fieldnames is None:
            continue
        fn = reader.fieldnames
        has_dir = "ldgrDirVsOffsetFlg" in fn
        has_acct = "ldgrNominalAccountID" in fn
        if not ("ldgrIPID" in fn and "ldgrTransAmount" in fn and "ldgrLedgerPeriod" in fn):
            continue
        for row in reader:
            period = row["ldgrLedgerPeriod"]
            fy = fiscal_year_of(period)
            if fy is None:
                continue
            amt = sf(row["ldgrTransAmount"])
            flag = (row.get("ldgrDirVsOffsetFlg", "D") if has_dir else "D").strip()
            f = feats_raw[row["ldgrIPID"]][fy]
            if flag == "D":
                f[0] += amt
            else:
                f[1] += amt
            f[2] += 1
            if has_acct:
                f[3].add(row["ldgrNominalAccountID"])
            f[4].add(period)
feats = {(vid, fy): [f[0], f[1], f[2], len(f[3]), len(f[4])]
         for vid, d in feats_raw.items() for fy, f in d.items()}
print(f"LDGR vendor-year records: {len(feats)}")

# 3. Panel: observable view vendor-years with LDGR match
fys_l, xb_l, xv_l, y_l = [], [], [], []
skipped = 0
for (vid, fy) in present:
    if not observable(fy):
        continue
    f = feats.get((vid, fy))
    if f is None:
        skipped += 1
        continue
    fys_l.append(fy)
    xb_l.append(f)
    xv_l.append(view_feats[(vid, fy)])
    y_l.append(1 if (vid, fy + 1) in present else 0)
fys = np.array(fys_l)
X_bal = np.array(xb_l, dtype=float)
X_view = np.array(xv_l, dtype=float)
X_comb = np.hstack([X_bal, X_view])
y = np.array(y_l, dtype=int)
print(f"Panel: {len(y)} rows (skipped {skipped} with no LDGR match); churn {100 * (1 - y.mean()):.1f}%")

# 4. Matched split: identical rule to the balance baseline
runs, cur = [], []
for fy in sorted(set(fys.tolist())):
    if cur and fy == cur[-1] + 1:
        cur.append(fy)
    else:
        if cur:
            runs.append(cur)
        cur = [fy]
if cur:
    runs.append(cur)
run = max(runs, key=lambda r: (len(r), r[-1])) if runs else []
train_fys = {t for t in run if t + 1 < min(HOLDOUT)}
test_fys = {t for t in run if t + 1 in HOLDOUT}
if train_fys & test_fys:
    print(f"ERROR: train and test overlap: {sorted(train_fys & test_fys)}", file=sys.stderr)
    sys.exit(1)
tr = np.isin(fys, list(train_fys))
te = np.isin(fys, list(test_fys))
print(f"Train FYs {sorted(train_fys)} n={int(tr.sum())}; test FYs {sorted(test_fys)} n={int(te.sum())}")


def fit_scores(X):
    sc = StandardScaler().fit(X[tr])
    clf = LogisticRegression(max_iter=1000, solver="lbfgs")
    clf.fit(sc.transform(X[tr]), y[tr])
    return clf.predict_proba(sc.transform(X[te]))[:, 1]


yt = y[te]
s_bal = fit_scores(X_bal)
s_comb = fit_scores(X_comb)
s_tt = fit_scores(fys.reshape(-1, 1).astype(float))
c_bal = auc(yt, s_bal)
c_comb = auc(yt, s_comb)
c_tt = auc(yt, s_tt)
rng = np.random.default_rng(SEED)
p_comb = perm_p(yt, s_comb, c_comb, N_PERM, rng)

# Bootstrap the difference in C over hold-out rows (same resample for both models)
rng_b = np.random.default_rng(SEED)
n = len(yt)
deltas = []
for _ in range(N_BOOT):
    idx = rng_b.integers(0, n, n)
    yy = yt[idx]
    if yy.sum() in (0, n):
        continue
    deltas.append(auc(yy, s_comb[idx]) - auc(yy, s_bal[idx]))
lo, hi = np.percentile(deltas, [2.5, 97.5])
delta = c_comb - c_bal
gain = delta > 0 and lo > 0
verdict = "COMMITMENT_ADDS_SIGNAL" if gain else "NO_RELIABLE_GAIN"

print(f"\nbalance_only C = {c_bal:.4f}  (balance baseline reported 0.7683)")
print(f"combined     C = {c_comb:.4f}  perm p = {p_comb:.4f}")
print(f"time_trend   C = {c_tt:.4f}")
print(f"delta combined - balance = {delta:+.4f}  bootstrap 95% [{lo:+.4f}, {hi:+.4f}]")
print(f"verdict: {verdict}")

stamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
train_s = f"{min(train_fys)}-{max(train_fys)}"
test_s = f"{min(test_fys)}-{max(test_fys)}"
rows = [
    {"run_timestamp": stamp, "model": "balance_only", "train_fy_range": train_s,
     "test_fy_range": test_s, "n_train": int(tr.sum()), "n_test": int(te.sum()),
     "c_statistic": round(c_bal, 4), "delta_vs_balance": 0.0, "ci95_low": "",
     "ci95_high": "", "perm_p": "", "verdict": "reference",
     "note": "5 LDGR features; should replicate AIM3_balance_baseline.csv (0.7683)"},
    {"run_timestamp": stamp, "model": "combined", "train_fy_range": train_s,
     "test_fy_range": test_s, "n_train": int(tr.sum()), "n_test": int(te.sum()),
     "c_statistic": round(c_comb, 4), "delta_vs_balance": round(delta, 4),
     "ci95_low": round(lo, 4), "ci95_high": round(hi, 4), "perm_p": round(p_comb, 4),
     "verdict": verdict,
     "note": "5 LDGR + 4 view features (po_count, agency_count, distinct_nigp_classes, total_amount)"},
    {"run_timestamp": stamp, "model": "time_trend_control", "train_fy_range": train_s,
     "test_fy_range": test_s, "n_train": int(tr.sum()), "n_test": int(te.sum()),
     "c_statistic": round(c_tt, 4), "delta_vs_balance": round(c_tt - c_bal, 4),
     "ci95_low": "", "ci95_high": "", "perm_p": "",
     "verdict": "FAIL" if c_tt <= 0.65 else "PASS",
     "note": "fiscal_year as sole feature"},
]
os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", newline="", encoding="utf-8") as fh:
    w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
    w.writeheader()
    w.writerows(rows)
print(f"Wrote {OUT}")
