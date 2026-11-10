#!/usr/bin/env python3
"""
Track C (Berka PKDD'99) -- v3 protocol (2026-11-10)
Paper 2: Business Event Repositories as Training Corpora for Enterprise AI

v3 adds four things the v2 run lacked:
  1. Locked acceptance thresholds (declared below, before any run).
  2. Nested-CV lambda tuning (inner 3-fold inside each outer fold to pick lambda
     from a grid; headline uses the per-fold best lambda, not a fixed one).
  3. Temporal split evaluation (train on loans granted in earlier years, test on
     later years; in addition to, not replacing, the repeated-CV result).
  4. Permutation test on the 22 event-only columns (shuffle each column
     independently 999 times; p = share of permuted E-B gains >= real gain).

All four are declared here; no result may change these constants.

Locked acceptance thresholds (v3, declared 2026-11-10):
  T1. ROC-AUC E-B paired 95% CI lower bound > 0.00 (interval excludes zero).
  T2. Temporal split ROC-AUC E > B (sign positive; no CI required given n is small).
  T3. Permutation p < 0.05 on the 22 event-only columns (one-sided, right tail).
These are illustrative thresholds: passing all three on one corpus with 76 defaults
does not prove Theorem 1. A result that fails one or more is also informative.

Usage:
  python scripts/berka/fit_track_c_v3.py \\
    --data-dir data/berka_processed \\
    --evidence-out data/test_evidence/TRACK_C_BERKA_V3_EVIDENCE.json
"""

import csv
import json
import os
import sys

import numpy as np

# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
DATA_DIR = "data/berka_processed"
EVIDENCE_OUT = "data/test_evidence/TRACK_C_BERKA_V3_EVIDENCE.json"

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--data-dir" and i + 1 < len(sys.argv):
        DATA_DIR = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--evidence-out" and i + 1 < len(sys.argv):
        EVIDENCE_OUT = sys.argv[i + 1]; i += 2
    else:
        print(f"Unknown argument: {sys.argv[i]}", file=sys.stderr)
        sys.exit(1)

# ---------------------------------------------------------------------------
# Locked protocol constants (do not change after first run)
# ---------------------------------------------------------------------------
LAMBDA_GRID = [0.1, 1.0, 10.0, 100.0]    # nested-CV picks from this grid
N_OUTER_SPLITS = 5
N_OUTER_REPEATS = 20
N_INNER_SPLITS = 3
N_BOOT = 2000
N_PERM = 999
K_NEIGHBOURS = 5
BOOT_SEED = 99
PERM_SEED = 77
BALANCE_PROFILE_COLS = ["bal_mean", "bal_min", "bal_std", "bal_last", "bal_last30_mean"]
LOAN_TERM_COLS = ["loan_amount", "duration", "payments"]
ID_COLS = ["loan_id", "account_id", "grant_date", "target_default"]

# Locked acceptance thresholds
THRESHOLD_CI_LB = 0.00        # T1: paired 95% CI lower bound must exceed this
THRESHOLD_PERM_P = 0.05       # T3: permutation p must be below this

# ---------------------------------------------------------------------------
# Metrics
# ---------------------------------------------------------------------------

def average_ranks(x):
    order = np.argsort(x, kind="mergesort")
    xs = x[order]
    ranks = np.empty(len(x))
    n = len(x)
    start = 0
    while start < n:
        end = start
        while end + 1 < n and xs[end + 1] == xs[start]:
            end += 1
        ranks[order[start:end + 1]] = (start + end) / 2.0 + 1.0
        start = end + 1
    return ranks


def roc_auc(y, s):
    n_pos = int((y == 1).sum())
    n_neg = len(y) - n_pos
    if n_pos == 0 or n_neg == 0:
        return float("nan")
    r = average_ranks(s)
    return float((r[y == 1].sum() - n_pos * (n_pos + 1) / 2.0) / (n_pos * n_neg))


def brier(y, p):
    return float(np.mean((p - y) ** 2))


# ---------------------------------------------------------------------------
# Learner (same IRLS logit as v2)
# ---------------------------------------------------------------------------

def slog(x):
    return np.sign(x) * np.log1p(np.abs(x))


class Logit:
    def __init__(self, lam):
        self.lam = lam

    def fit(self, X, y):
        X = slog(X)
        self.mu = X.mean(axis=0)
        self.sd = X.std(axis=0)
        self.sd[self.sd == 0] = 1.0
        Z = np.hstack([np.ones((len(X), 1)), (X - self.mu) / self.sd])
        w = np.zeros(Z.shape[1])
        pen = np.full(Z.shape[1], self.lam)
        pen[0] = 0.0
        for _ in range(60):
            p = 1.0 / (1.0 + np.exp(-np.clip(Z @ w, -30, 30)))
            g = Z.T @ (p - y) + pen * w
            H = (Z * (p * (1 - p))[:, None]).T @ Z + np.diag(pen) + 1e-9 * np.eye(Z.shape[1])
            step = np.linalg.solve(H, g)
            w -= step
            if np.max(np.abs(step)) < 1e-8:
                break
        self.w = w
        return self

    def predict(self, X):
        Z = np.hstack([np.ones((len(X), 1)), (slog(X) - self.mu) / self.sd])
        return 1.0 / (1.0 + np.exp(-np.clip(Z @ self.w, -30, 30)))


def stratified_folds(y, seed, n_splits):
    rng = np.random.RandomState(seed)
    pos = rng.permutation(np.where(y == 1)[0])
    neg = rng.permutation(np.where(y == 0)[0])
    return [np.concatenate([a, b]) for a, b in
            zip(np.array_split(pos, n_splits), np.array_split(neg, n_splits))]


def nested_oof(X, y):
    """Nested-CV: outer N_OUTER_SPLITS x N_OUTER_REPEATS; inner N_INNER_SPLITS to pick lambda.
    Returns average out-of-fold scores (one per loan) and the lambda chosen per outer fold."""
    out = np.zeros((N_OUTER_REPEATS, len(y)))
    chosen_lambdas = []
    for rep in range(N_OUTER_REPEATS):
        rep_lambdas = []
        for val in stratified_folds(y, rep, N_OUTER_SPLITS):
            tr = np.setdiff1d(np.arange(len(y)), val)
            # inner CV on training fold to pick lambda
            best_lam, best_auc = LAMBDA_GRID[0], -1.0
            for lam in LAMBDA_GRID:
                inner_scores = np.zeros(len(tr))
                for iv in stratified_folds(y[tr], rep * 100 + len(val), N_INNER_SPLITS):
                    itr = np.setdiff1d(np.arange(len(tr)), iv)
                    inner_scores[iv] = Logit(lam).fit(X[tr][itr], y[tr][itr]).predict(X[tr][iv])
                a = roc_auc(y[tr], inner_scores)
                if not (a != a) and a > best_auc:  # nan-safe
                    best_auc = a
                    best_lam = lam
            out[rep, val] = Logit(best_lam).fit(X[tr], y[tr]).predict(X[val])
            rep_lambdas.append(best_lam)
        chosen_lambdas.append(rep_lambdas)
    return out, chosen_lambdas


def paired_boot(y, s_a, s_b, n_boot=N_BOOT, seed=BOOT_SEED):
    rng = np.random.RandomState(seed)
    pos = np.where(y == 1)[0]
    neg = np.where(y == 0)[0]
    d = np.empty(n_boot)
    for k in range(n_boot):
        idx = np.concatenate([rng.choice(pos, len(pos)), rng.choice(neg, len(neg))])
        d[k] = roc_auc(y[idx], s_b[idx]) - roc_auc(y[idx], s_a[idx])
    return d


def ci(d):
    return [float(np.percentile(d, 2.5)), float(np.percentile(d, 97.5))]


# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

def load_csv(path):
    with open(path, "r", encoding="utf-8") as f:
        return list(csv.DictReader(f))


b_path = os.path.join(DATA_DIR, "substrate_b_daily_balance.csv")
e_path = os.path.join(DATA_DIR, "substrate_e_events.csv")
if not (os.path.exists(b_path) and os.path.exists(e_path)):
    print(f"Error: substrate files not found in {DATA_DIR}. Run build_berka_substrates.py first.",
          file=sys.stderr)
    sys.exit(1)

rows_b = load_csv(b_path)
rows_e = load_csv(e_path)
assert [r["loan_id"] for r in rows_b] == [r["loan_id"] for r in rows_e]

if "grant_date" not in rows_b[0]:
    print("Error: grant_date column missing — re-run build_berka_substrates.py (v3 adds this column).",
          file=sys.stderr)
    sys.exit(1)

cols_b = [c for c in rows_b[0] if c not in ID_COLS]
cols_e = [c for c in rows_e[0] if c not in ID_COLS]
assert set(cols_b) <= set(cols_e), "E must contain every B column"
cols_e_extra = [c for c in cols_e if c not in cols_b]

y = np.array([int(r["target_default"]) for r in rows_b])
grant_dates = [r["grant_date"] for r in rows_b]   # ISO strings, sortable
X_b = np.array([[float(r[c]) for c in cols_b] for r in rows_b])
X_e = np.array([[float(r[c]) for c in cols_e] for r in rows_e])
days_neg_idx = cols_b.index("days_negative")
overdraft = (X_b[:, days_neg_idx] > 0).astype(int)

base_rate = float(y.mean())
print(f"Loans: {len(y)}; defaults: {int(y.sum())}; B cols: {len(cols_b)}; E-extra cols: {len(cols_e_extra)}")
print(f"Pre-loan overdraft (any neg daily close): defaults {int(overdraft[y==1].sum())}, "
      f"non-defaults {int(overdraft[y==0].sum())}")

evidence = {
    "experiment": "Paper 2 - Track C Retail Banking Benchmark (Berka PKDD'99), v3 protocol",
    "protocol": {
        "version": "v3",
        "learner": "L2 logistic regression (IRLS), nested-CV lambda selection",
        "lambda_grid": LAMBDA_GRID,
        "outer_folds": f"stratified {N_OUTER_SPLITS}-fold x {N_OUTER_REPEATS} repeats",
        "inner_folds": f"stratified {N_INNER_SPLITS}-fold within each outer training set",
        "interval": f"{N_BOOT} stratified paired bootstrap resamples of loans",
        "permutation_n": N_PERM,
        "neighbours_k": K_NEIGHBOURS,
    },
    "locked_thresholds": {
        "T1_ci_lb": THRESHOLD_CI_LB,
        "T1_desc": "paired 95% CI lower bound on E-B ROC-AUC must exceed this",
        "T2_desc": "temporal split E ROC-AUC must exceed B (sign positive)",
        "T3_perm_p": THRESHOLD_PERM_P,
        "T3_desc": "permutation p on event-only columns must be below this",
    },
    "columns": {"b": cols_b, "e_extra": cols_e_extra},
    "corpus_scale": {"loans": int(len(y)), "defaults": int(y.sum()), "default_ratio": base_rate},
}

# ---------------------------------------------------------------------------
# Repeated nested-CV (all loans and overdraft-free subset)
# ---------------------------------------------------------------------------

def run_cv(mask, label):
    ys = y[mask]
    Xb, Xe = X_b[mask], X_e[mask]
    print(f"\n=== {label}: {int(mask.sum())} loans, {int(ys.sum())} defaults ===")

    oof_b, lam_b = nested_oof(Xb, ys)
    oof_e, lam_e = nested_oof(Xe, ys)

    avg_b = oof_b.mean(axis=0)
    avg_e = oof_e.mean(axis=0)

    auc_b = roc_auc(ys, avg_b)
    auc_e = roc_auc(ys, avg_e)
    d = paired_boot(ys, avg_b, avg_e)
    gain = auc_e - auc_b
    ci95 = ci(d)

    verdict_T1 = "PASS" if ci95[0] > THRESHOLD_CI_LB else "FAIL"
    print(f"B ROC-AUC {auc_b:.4f}  E ROC-AUC {auc_e:.4f}  gain {gain:+.4f}  CI95 {np.round(ci95,4).tolist()}  T1:{verdict_T1}")

    # lambda selection summary
    flat_b = [lam for rep in lam_b for lam in rep]
    flat_e = [lam for rep in lam_e for lam in rep]

    return {
        "loans": int(mask.sum()), "defaults": int(ys.sum()),
        "B_roc_auc": auc_b, "E_roc_auc": auc_e,
        "E_minus_B_roc_auc": gain,
        "E_minus_B_ci95": ci95,
        "T1_verdict": verdict_T1,
        "brier_B": brier(ys, avg_b), "brier_E": brier(ys, avg_e),
        "lambda_B_counts": {str(lam): flat_b.count(lam) for lam in LAMBDA_GRID},
        "lambda_E_counts": {str(lam): flat_e.count(lam) for lam in LAMBDA_GRID},
    }, avg_b, avg_e, ys


all_mask = np.ones(len(y), dtype=bool)
free_mask = overdraft == 0

cv_all, sb_all, se_all, y_all = run_cv(all_mask, "All loans")
cv_free, sb_free, se_free, y_free = run_cv(free_mask, "Loans with no pre-loan overdraft")
evidence["cv_all_loans"] = cv_all
evidence["cv_overdraft_free_loans"] = cv_free

# ---------------------------------------------------------------------------
# Temporal split
# ---------------------------------------------------------------------------
print("\n=== Temporal split ===")
# Sort loans by grant_date; split into first 60% train / last 40% test (by grant date)
order = np.argsort(grant_dates)    # ISO dates sort correctly as strings
n = len(order)
split_idx = int(n * 0.60)
train_idx = order[:split_idx]
test_idx = order[split_idx:]

y_tr = y[train_idx]; y_te = y[test_idx]
print(f"Train: {len(train_idx)} loans ({int(y_tr.sum())} defaults)  "
      f"Test: {len(test_idx)} loans ({int(y_te.sum())} defaults)  "
      f"cutoff date: {grant_dates[order[split_idx]]}")

def temporal_eval(Xtr, Xte, ytr, yte, label):
    if ytr.sum() < 2 or yte.sum() < 2:
        print(f"  {label}: too few defaults in one split — skipped")
        return {"skipped": True}
    # Use inner CV on training set only to pick lambda
    best_lam, best_auc = LAMBDA_GRID[0], -1.0
    for lam in LAMBDA_GRID:
        inner_scores = np.zeros(len(ytr))
        for iv in stratified_folds(ytr, 42, N_INNER_SPLITS):
            itr = np.setdiff1d(np.arange(len(ytr)), iv)
            if ytr[itr].sum() < 1:
                continue
            inner_scores[iv] = Logit(lam).fit(Xtr[itr], ytr[itr]).predict(Xtr[iv])
        a = roc_auc(ytr, inner_scores)
        if not (a != a) and a > best_auc:
            best_auc = a; best_lam = lam
    s = Logit(best_lam).fit(Xtr, ytr).predict(Xte)
    a = roc_auc(yte, s)
    print(f"  {label}: lambda={best_lam}  test ROC-AUC {a:.4f}  ({int(yte.sum())} defaults)")
    return {"lambda": best_lam, "test_roc_auc": a, "test_defaults": int(yte.sum()), "test_loans": int(len(yte))}

ts_b = temporal_eval(X_b[train_idx], X_b[test_idx], y_tr, y_te, "B")
ts_e = temporal_eval(X_e[train_idx], X_e[test_idx], y_tr, y_te, "E")
ts_gain = (ts_e["test_roc_auc"] - ts_b["test_roc_auc"]
           if not ts_b.get("skipped") and not ts_e.get("skipped") else float("nan"))
verdict_T2 = "PASS" if ts_gain > 0 else ("FAIL" if ts_gain <= 0 else "SKIP")
print(f"  E-B gain {ts_gain:+.4f}  T2:{verdict_T2}")
evidence["temporal_split"] = {
    "train_n": int(len(train_idx)), "train_defaults": int(y_tr.sum()),
    "test_n": int(len(test_idx)), "test_defaults": int(y_te.sum()),
    "cutoff_date": grant_dates[order[split_idx]],
    "train_end_date": grant_dates[order[split_idx - 1]],
    "B": ts_b, "E": ts_e,
    "E_minus_B_test_roc_auc": ts_gain,
    "T2_verdict": verdict_T2,
}

# ---------------------------------------------------------------------------
# Permutation test on the 22 event-only columns
# ---------------------------------------------------------------------------
print(f"\n=== Permutation test on {len(cols_e_extra)} event-only columns ({N_PERM} permutations) ===")
# Real E-B gain on all loans
real_gain = cv_all["E_minus_B_roc_auc"]

e_extra_idx = [cols_e.index(c) for c in cols_e_extra]
rng_perm = np.random.RandomState(PERM_SEED)
perm_gains = np.empty(N_PERM)
for k in range(N_PERM):
    Xe_perm = X_e.copy()
    for j in e_extra_idx:
        Xe_perm[:, j] = rng_perm.permutation(Xe_perm[:, j])
    oof_perm, _ = nested_oof(X_b, y)          # B unchanged
    oof_e_perm, _ = nested_oof(Xe_perm, y)
    avg_bp = oof_perm.mean(axis=0)
    avg_ep = oof_e_perm.mean(axis=0)
    perm_gains[k] = roc_auc(y, avg_ep) - roc_auc(y, avg_bp)
    if (k + 1) % 100 == 0:
        print(f"  permutation {k+1}/{N_PERM}  running p={float((perm_gains[:k+1] >= real_gain).mean()):.3f}")

perm_p = float((perm_gains >= real_gain).mean())
verdict_T3 = "PASS" if perm_p < THRESHOLD_PERM_P else "FAIL"
print(f"Real gain {real_gain:+.4f}; permutation p={perm_p:.4f}  T3:{verdict_T3}")
evidence["permutation_test"] = {
    "event_only_columns": cols_e_extra,
    "n_permutations": N_PERM,
    "real_E_minus_B_roc_auc": real_gain,
    "perm_mean": float(perm_gains.mean()),
    "perm_sd": float(perm_gains.std(ddof=1)),
    "perm_p_one_sided": perm_p,
    "T3_verdict": verdict_T3,
}

# ---------------------------------------------------------------------------
# Test 2: nearest-neighbour non-injectivity (same as v2, for continuity)
# ---------------------------------------------------------------------------
print("\n=== Test 2: defaults vs nearest non-defaults in balance-profile space ===")
bp_cols_idx = [cols_b.index(c) for c in BALANCE_PROFILE_COLS]
prof = slog(X_b[:, bp_cols_idx])
prof = (prof - prof.mean(axis=0)) / np.where(prof.std(axis=0) == 0, 1.0, prof.std(axis=0))


def neighbour_test(mask, sb, se, ys_full, label):
    idx = np.where(mask)[0]
    ys = ys_full
    P = prof[idx]
    defaults = np.where(ys == 1)[0]
    goods = np.where(ys == 0)[0]
    diff_b, diff_e = [], []
    for d in defaults:
        dist = np.linalg.norm(P[goods] - P[d], axis=1)
        nn = goods[np.argsort(dist)[:K_NEIGHBOURS]]
        diff_b.append(sb[d] - sb[nn].mean())
        diff_e.append(se[d] - se[nn].mean())
    diff_b, diff_e = np.array(diff_b), np.array(diff_e)
    rng2 = np.random.RandomState(BOOT_SEED)
    gains = np.array([(diff_e - diff_b)[rng2.randint(0, len(diff_b), len(diff_b))].mean()
                      for _ in range(N_BOOT)])
    gain_ei = ci(gains)
    g = float((diff_e - diff_b).mean())
    print(f"{label}: {len(defaults)} defaults  gap B {float(diff_b.mean()):+.4f} E {float(diff_e.mean()):+.4f}  "
          f"E-B {g:+.4f} {np.round(gain_ei,4).tolist()}")
    return {"defaults": int(len(defaults)), "mean_gap_B": float(diff_b.mean()),
            "mean_gap_E": float(diff_e.mean()), "gap_gain": g, "gap_gain_ci95": gain_ei,
            "win_rate_B": float((diff_b > 0).mean()), "win_rate_E": float((diff_e > 0).mean())}


evidence["non_injectivity_test2"] = {
    "all_loans": neighbour_test(all_mask, sb_all, se_all, y_all, "all loans        "),
    "overdraft_free": neighbour_test(free_mask, sb_free, se_free, y_free, "overdraft-free   "),
}

# ---------------------------------------------------------------------------
# Summary verdict
# ---------------------------------------------------------------------------
t1 = cv_all["T1_verdict"]
t2 = evidence["temporal_split"]["T2_verdict"]
t3 = evidence["permutation_test"]["T3_verdict"]
overall = "PASS" if t1 == "PASS" and t2 == "PASS" and t3 == "PASS" else "FAIL"
evidence["verdict"] = {"T1": t1, "T2": t2, "T3": t3, "overall": overall}
print(f"\n=== Summary: T1={t1}  T2={t2}  T3={t3}  overall={overall} ===")
print("(Illustrative only; one corpus, 76 defaults; lineage of Berka files is unknown.)")

os.makedirs(os.path.dirname(EVIDENCE_OUT) or ".", exist_ok=True)
with open(EVIDENCE_OUT, "w", encoding="utf-8") as f:
    json.dump(evidence, f, indent=2)
print(f"\nWritten v3 evidence to: {EVIDENCE_OUT}")
