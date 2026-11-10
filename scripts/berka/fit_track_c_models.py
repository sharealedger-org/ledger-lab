#!/usr/bin/env python3
"""
Track C (Berka PKDD'99) -- Model fit and evaluation (v2, 2026-11-10)
Paper 2: Business Event Repositories as Training Corpora for Enterprise AI

Compares, on the SAME loans, folds and learner:
  B  = product-system view: daily closing balance series + loan terms
  E  = event view: every B column plus columns derived from the transaction rows
       (operation, k_symbol, counterparty, intra-day multiplicity, ...)
Baselines: the pre-loan overdraft rule (days_negative > 0) and a loan-terms-only model.

Protocol fixed before the first run (no tuning on results):
  * Learner: L2-regularised logistic regression, IRLS (Newton), unweighted classes,
    features pass through sign(x)*log1p(|x|) then are standardised on the training fold.
    Primary penalty LAMBDA = 10 (sum-loss scale). Other penalties are reported as sensitivity only.
  * Folds: stratified 5-fold, repeated 20 times (seeds 0..19). B and E share every fold.
  * Headline: ROC-AUC of out-of-fold scores averaged over the repeats for each loan.
    Interval: 2000 stratified bootstrap resamples of loans, paired (same resample for B and E).
    This captures loan-sampling uncertainty for the fixed procedure; it does not refit per resample.
  * Subsets: all 682 loans, and the loans with no negative pre-loan daily closing balance.
  * Test 2 (non-injectivity): for each default, its K=5 nearest NON-default loans in
    balance-profile space (standardised sign-log of bal_mean, bal_min, bal_std, bal_last,
    bal_last30_mean). No distance cutoff. Per default: the default's score minus the mean score
    of its neighbours, for B and for E. Unit of analysis is the default (76), not the pair.

Usage:
  python scripts/berka/fit_track_c_models.py \
    --data-dir data/berka_processed \
    --evidence-out data/test_evidence/TRACK_C_BERKA_EVIDENCE.json
"""

import csv
import json
import os
import sys

import numpy as np

DATA_DIR = "data/berka_processed"
EVIDENCE_OUT = "data/test_evidence/TRACK_C_BERKA_EVIDENCE.json"

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--data-dir" and i + 1 < len(sys.argv):
        DATA_DIR = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--evidence-out" and i + 1 < len(sys.argv):
        EVIDENCE_OUT = sys.argv[i + 1]; i += 2
    else:
        print(f"Unknown argument: {sys.argv[i]}", file=sys.stderr)
        sys.exit(1)

LAMBDA = 10.0
LAMBDA_SENSITIVITY = [1.0, 100.0]
N_SPLITS = 5
N_REPEATS = 20
N_BOOT = 2000
K_NEIGHBOURS = 5
BOOT_SEED = 12345
LOAN_TERM_COLS = ["loan_amount", "duration", "payments"]
BALANCE_PROFILE_COLS = ["bal_mean", "bal_min", "bal_std", "bal_last", "bal_last30_mean"]
ID_COLS = ["loan_id", "account_id", "grant_date", "target_default"]

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


def pr_auc(y, s):
    order = np.argsort(-s, kind="mergesort")
    ys = y[order]
    tp = np.cumsum(ys == 1)
    prec = tp / np.arange(1, len(ys) + 1)
    return float((prec * (ys == 1)).sum() / max(1, (y == 1).sum()))


def brier(y, p):
    return float(np.mean((p - y) ** 2))


def log_loss(y, p, eps=1e-15):
    p = np.clip(p, eps, 1 - eps)
    return float(-np.mean(y * np.log(p) + (1 - y) * np.log(1 - p)))


# ---------------------------------------------------------------------------
# Learner
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


def folds_for(y, seed):
    rng = np.random.RandomState(seed)
    pos = rng.permutation(np.where(y == 1)[0])
    neg = rng.permutation(np.where(y == 0)[0])
    return [np.concatenate([a, b]) for a, b in zip(np.array_split(pos, N_SPLITS), np.array_split(neg, N_SPLITS))]


def oof_scores(X, y, lam):
    """Returns (n_repeats, n) out-of-fold probabilities."""
    out = np.zeros((N_REPEATS, len(y)))
    for rep in range(N_REPEATS):
        for val in folds_for(y, rep):
            tr = np.setdiff1d(np.arange(len(y)), val)
            out[rep, val] = Logit(lam).fit(X[tr], y[tr]).predict(X[val])
    return out


def summarise(y, oof):
    per_rep = [roc_auc(y, oof[r]) for r in range(oof.shape[0])]
    per_rep_pr = [pr_auc(y, oof[r]) for r in range(oof.shape[0])]
    avg = oof.mean(axis=0)
    return {
        "roc_auc_avg_oof": roc_auc(y, avg),
        "roc_auc_per_repeat_mean": float(np.mean(per_rep)),
        "roc_auc_per_repeat_sd": float(np.std(per_rep, ddof=1)),
        "pr_auc_avg_oof": pr_auc(y, avg),
        "pr_auc_per_repeat_mean": float(np.mean(per_rep_pr)),
        "brier": brier(y, avg),
        "log_loss": log_loss(y, avg),
    }, avg


def paired_boot(y, s_a, s_b, fn=roc_auc, n_boot=N_BOOT, seed=BOOT_SEED):
    """Distribution of fn(y, s_b) - fn(y, s_a) over stratified loan resamples."""
    rng = np.random.RandomState(seed)
    pos = np.where(y == 1)[0]
    neg = np.where(y == 0)[0]
    d = np.empty(n_boot)
    for k in range(n_boot):
        idx = np.concatenate([rng.choice(pos, len(pos)), rng.choice(neg, len(neg))])
        d[k] = fn(y[idx], s_b[idx]) - fn(y[idx], s_a[idx])
    return d


def ci(d):
    return [float(np.percentile(d, 2.5)), float(np.percentile(d, 97.5))]


def single_boot(y, s, n_boot=N_BOOT, seed=BOOT_SEED):
    rng = np.random.RandomState(seed)
    pos = np.where(y == 1)[0]
    neg = np.where(y == 0)[0]
    a = np.empty(n_boot)
    for k in range(n_boot):
        idx = np.concatenate([rng.choice(pos, len(pos)), rng.choice(neg, len(neg))])
        a[k] = roc_auc(y[idx], s[idx])
    return a


# ---------------------------------------------------------------------------
# Data
# ---------------------------------------------------------------------------

def load_csv(path):
    with open(path, "r", encoding="utf-8") as f:
        return list(csv.DictReader(f))


b_path = os.path.join(DATA_DIR, "substrate_b_daily_balance.csv")
e_path = os.path.join(DATA_DIR, "substrate_e_events.csv")
if not (os.path.exists(b_path) and os.path.exists(e_path)):
    print(f"Error: substrate files not found in {DATA_DIR}. Run build_berka_substrates.py first.", file=sys.stderr)
    sys.exit(1)

rows_b = load_csv(b_path)
rows_e = load_csv(e_path)
assert [r["loan_id"] for r in rows_b] == [r["loan_id"] for r in rows_e]

cols_b = [c for c in rows_b[0] if c not in ID_COLS]
cols_e = [c for c in rows_e[0] if c not in ID_COLS]
assert set(cols_b) <= set(cols_e), "E must contain every B column"
cols_e_extra = [c for c in cols_e if c not in cols_b]

y = np.array([int(r["target_default"]) for r in rows_b])
loan_ids = np.array([int(r["loan_id"]) for r in rows_b])
col_idx_b = {c: k for k, c in enumerate(cols_b)}
X_b = np.array([[float(r[c]) for c in cols_b] for r in rows_b])
X_e = np.array([[float(r[c]) for c in cols_e] for r in rows_e])
X_terms = X_b[:, [col_idx_b[c] for c in LOAN_TERM_COLS]]
X_e_extra = X_e[:, [cols_e.index(c) for c in cols_e_extra]]
days_neg = X_b[:, col_idx_b["days_negative"]]
overdraft = (days_neg > 0).astype(int)

print(f"Loans: {len(y)}; defaults: {int(y.sum())} ({y.mean():.4f}); B columns: {len(cols_b)}; E columns: {len(cols_e)}")
print(f"Pre-loan overdraft (any negative daily close): defaults {int(overdraft[y == 1].sum())}, "
      f"non-defaults {int(overdraft[y == 0].sum())}")

base_rate = float(y.mean())
evidence = {
    "experiment": "Paper 2 - Track C Retail Banking Benchmark (Berka PKDD'99), v2 protocol",
    "protocol": {
        "learner": "L2 logistic regression (IRLS), unweighted, sign-log features",
        "primary_lambda": LAMBDA,
        "sensitivity_lambdas": LAMBDA_SENSITIVITY,
        "folds": f"stratified {N_SPLITS}-fold x {N_REPEATS} repeats, shared by B and E",
        "interval": f"{N_BOOT} stratified paired bootstrap resamples of loans over averaged out-of-fold scores",
        "neighbours_k": K_NEIGHBOURS,
    },
    "columns": {"b": cols_b, "e_extra": cols_e_extra},
    "corpus_scale": {"loans": int(len(y)), "defaults": int(y.sum()), "default_ratio": base_rate},
    "base_rate_reference": {"brier": base_rate * (1 - base_rate), "log_loss": log_loss(y, np.full(len(y), base_rate))},
}


# ---------------------------------------------------------------------------
# Experiment on a subset
# ---------------------------------------------------------------------------

def experiment(mask, label):
    ys = y[mask]
    Xb, Xe, Xt = X_b[mask], X_e[mask], X_terms[mask]
    print(f"\n=== {label}: {int(mask.sum())} loans, {int(ys.sum())} defaults ===")
    res = {"loans": int(mask.sum()), "defaults": int(ys.sum())}
    avg_scores = {}
    for name, X in (("loan_terms_only", Xt), ("B_daily_balance", Xb), ("E_events", Xe)):
        oof = oof_scores(X, ys, LAMBDA)
        s, avg = summarise(ys, oof)
        s["auc_ci95"] = ci(single_boot(ys, avg))
        res[name] = s
        avg_scores[name] = avg
        print(f"{name:18s} ROC-AUC {s['roc_auc_avg_oof']:.4f} [{s['auc_ci95'][0]:.3f},{s['auc_ci95'][1]:.3f}] "
              f"(per-repeat {s['roc_auc_per_repeat_mean']:.4f} sd {s['roc_auc_per_repeat_sd']:.4f}) "
              f"PR-AUC {s['pr_auc_avg_oof']:.4f} Brier {s['brier']:.4f} LogLoss {s['log_loss']:.4f}")

    rule = overdraft[mask].astype(float)
    res["overdraft_rule"] = {
        "roc_auc": roc_auc(ys, rule),
        "flagged_defaults": int(rule[ys == 1].sum()),
        "flagged_non_defaults": int(rule[ys == 0].sum()),
    }
    print(f"overdraft rule     ROC-AUC {res['overdraft_rule']['roc_auc']:.4f}; flags "
          f"{res['overdraft_rule']['flagged_defaults']} defaults / {res['overdraft_rule']['flagged_non_defaults']} non-defaults")

    sa, sb = avg_scores["B_daily_balance"], avg_scores["E_events"]
    d_roc = paired_boot(ys, sa, sb, roc_auc)
    d_pr = paired_boot(ys, sa, sb, pr_auc)
    res["E_minus_B"] = {
        "roc_auc_gain": roc_auc(ys, sb) - roc_auc(ys, sa), "roc_auc_gain_ci95": ci(d_roc),
        "pr_auc_gain": pr_auc(ys, sb) - pr_auc(ys, sa), "pr_auc_gain_ci95": ci(d_pr),
        "share_of_resamples_E_better_roc": float((d_roc > 0).mean()),
    }
    t = paired_boot(ys, avg_scores["loan_terms_only"], sa, roc_auc)
    res["B_minus_loan_terms"] = {"roc_auc_gain": roc_auc(ys, sa) - roc_auc(ys, avg_scores["loan_terms_only"]),
                                 "roc_auc_gain_ci95": ci(t)}
    print(f"E minus B  ROC-AUC {res['E_minus_B']['roc_auc_gain']:+.4f} {np.round(res['E_minus_B']['roc_auc_gain_ci95'], 4).tolist()}; "
          f"PR-AUC {res['E_minus_B']['pr_auc_gain']:+.4f} {np.round(res['E_minus_B']['pr_auc_gain_ci95'], 4).tolist()}")
    print(f"B minus loan-terms-only ROC-AUC {res['B_minus_loan_terms']['roc_auc_gain']:+.4f} "
          f"{np.round(res['B_minus_loan_terms']['roc_auc_gain_ci95'], 4).tolist()}")

    # POST-HOC ablation (added after the first run; not part of the primary protocol):
    # where does the E-over-B gain come from?
    groups = {
        "counterparty": ["distinct_partner_accounts", "distinct_partner_banks", "partner_txn_per30", "partner_txn_share"],
        "penalty_interest": ["sanction_per30"],
    }
    abl = {}
    for gname, gcols in groups.items():
        keep_without = [k for k, c in enumerate(cols_e) if c not in gcols]
        keep_only = [k for k, c in enumerate(cols_e) if c in cols_b or c in gcols]
        for tag, keep in ((f"E_without_{gname}", keep_without), (f"B_plus_{gname}_only", keep_only)):
            s_abl = oof_scores(Xe[:, keep], ys, LAMBDA).mean(axis=0)
            dd = paired_boot(ys, sa, s_abl, roc_auc)
            abl[tag] = {"roc_auc": roc_auc(ys, s_abl), "gain_over_B": roc_auc(ys, s_abl) - roc_auc(ys, sa),
                        "gain_over_B_ci95": ci(dd)}
            print(f"  [post-hoc] {tag:34s} ROC-AUC {abl[tag]['roc_auc']:.4f}  vs B {abl[tag]['gain_over_B']:+.4f} "
                  f"{np.round(abl[tag]['gain_over_B_ci95'], 4).tolist()}")
    res["post_hoc_ablation"] = abl

    # POST-HOC arm 2 (added after the ablation above): events only, no balance-derived columns.
    # Keeps loan terms and history_days so the comparison with B is on the same footing.
    ev_keep = [k for k, c in enumerate(cols_e)
               if c in LOAN_TERM_COLS or c == "history_days" or c in cols_e_extra]
    s_ev = oof_scores(Xe[:, ev_keep], ys, LAMBDA).mean(axis=0)
    d_b_ev = paired_boot(ys, s_ev, sa, roc_auc)      # B minus events-only
    d_e_ev = paired_boot(ys, s_ev, sb, roc_auc)      # E minus events-only
    res["post_hoc_events_only"] = {
        "roc_auc": roc_auc(ys, s_ev),
        "B_minus_events_only": roc_auc(ys, sa) - roc_auc(ys, s_ev), "B_minus_events_only_ci95": ci(d_b_ev),
        "E_minus_events_only": roc_auc(ys, sb) - roc_auc(ys, s_ev), "E_minus_events_only_ci95": ci(d_e_ev),
        "columns": [cols_e[k] for k in ev_keep],
    }
    print(f"  [post-hoc] events only (no balance columns) ROC-AUC {roc_auc(ys, s_ev):.4f}; "
          f"B minus events-only {res['post_hoc_events_only']['B_minus_events_only']:+.4f} {np.round(ci(d_b_ev), 4).tolist()}; "
          f"E minus events-only {res['post_hoc_events_only']['E_minus_events_only']:+.4f} {np.round(ci(d_e_ev), 4).tolist()}")



    # Sensitivity to the penalty (reported, never used to pick the headline)
    sens = {}
    for lam in LAMBDA_SENSITIVITY:
        ab = oof_scores(Xb, ys, lam).mean(axis=0)
        ae = oof_scores(Xe, ys, lam).mean(axis=0)
        sens[str(lam)] = {"B": roc_auc(ys, ab), "E": roc_auc(ys, ae), "E_minus_B": roc_auc(ys, ae) - roc_auc(ys, ab)}
        print(f"  lambda={lam:g}: B {sens[str(lam)]['B']:.4f}  E {sens[str(lam)]['E']:.4f}  gain {sens[str(lam)]['E_minus_B']:+.4f}")
    res["penalty_sensitivity"] = sens

    # Event-only columns: single-feature AUC (direction by max(AUC, 1-AUC)); descriptive, not a model
    feat = {}
    for k, c in enumerate(cols_e_extra):
        x = X_e_extra[mask][:, k]
        a = roc_auc(ys, x)
        boots = single_boot(ys, x)
        d = max(a, 1 - a)
        lo, hi = (np.percentile(boots, 2.5), np.percentile(boots, 97.5))
        feat[c] = {"auc_dir": float(d), "higher_ranks_default": bool(a >= 0.5),
                   "ci95": [float(lo if a >= 0.5 else 1 - hi), float(hi if a >= 0.5 else 1 - lo)]}
    res["event_only_single_feature_auc"] = feat
    top = sorted(feat.items(), key=lambda kv: -kv[1]["auc_dir"])[:8]
    print("  top event-only single-feature AUCs:", ", ".join(f"{c} {v['auc_dir']:.3f}" for c, v in top))
    return res, avg_scores


all_mask = np.ones(len(y), dtype=bool)
free_mask = overdraft == 0

res_all, scores_all = experiment(all_mask, "All loans")
res_free, scores_free = experiment(free_mask, "Loans with no pre-loan overdraft")
evidence["all_loans"] = res_all
evidence["overdraft_free_loans"] = res_free

# ---------------------------------------------------------------------------
# Test 2: non-injectivity on balance-profile neighbours (K nearest non-defaults per default)
# ---------------------------------------------------------------------------
print("\n=== Test 2: defaults vs their nearest non-defaults in balance-profile space ===")
prof = slog(X_b[:, [col_idx_b[c] for c in BALANCE_PROFILE_COLS]])
prof = (prof - prof.mean(axis=0)) / np.where(prof.std(axis=0) == 0, 1.0, prof.std(axis=0))


def neighbour_test(mask, scores, label):
    idx = np.where(mask)[0]
    ys = y[idx]
    P = prof[idx]
    defaults = np.where(ys == 1)[0]
    goods = np.where(ys == 0)[0]
    sb, se = scores["B_daily_balance"], scores["E_events"]
    diff_b, diff_e, win_b, win_e, dists = [], [], [], [], []
    for d in defaults:
        dist = np.linalg.norm(P[goods] - P[d], axis=1)
        nn = goods[np.argsort(dist)[:K_NEIGHBOURS]]
        dists.append(float(np.sort(dist)[:K_NEIGHBOURS].mean()))
        diff_b.append(sb[d] - sb[nn].mean())
        diff_e.append(se[d] - se[nn].mean())
        win_b.append(float((sb[d] > sb[nn]).mean()))
        win_e.append(float((se[d] > se[nn]).mean()))
    diff_b, diff_e = np.array(diff_b), np.array(diff_e)
    rng = np.random.RandomState(BOOT_SEED)
    gains = np.array([(diff_e - diff_b)[rng.randint(0, len(diff_b), len(diff_b))].mean() for _ in range(N_BOOT)])
    out = {
        "defaults": int(len(defaults)),
        "mean_neighbour_distance": float(np.mean(dists)),
        "mean_score_gap_B": float(diff_b.mean()),
        "mean_score_gap_E": float(diff_e.mean()),
        "gap_gain_E_minus_B": float((diff_e - diff_b).mean()),
        "gap_gain_ci95": ci(gains),
        "mean_win_rate_B": float(np.mean(win_b)),
        "mean_win_rate_E": float(np.mean(win_e)),
    }
    print(f"{label}: {len(defaults)} defaults; gap B {out['mean_score_gap_B']:+.4f}, E {out['mean_score_gap_E']:+.4f}, "
          f"gain {out['gap_gain_E_minus_B']:+.4f} {np.round(out['gap_gain_ci95'], 4).tolist()}; "
          f"win-rate B {out['mean_win_rate_B']:.3f}, E {out['mean_win_rate_E']:.3f}")
    return out


evidence["non_injectivity"] = {
    "all_loans": neighbour_test(all_mask, scores_all, "all loans        "),
    "overdraft_free_loans": neighbour_test(free_mask, scores_free, "overdraft-free   "),
}

os.makedirs(os.path.dirname(EVIDENCE_OUT) or ".", exist_ok=True)
with open(EVIDENCE_OUT, "w", encoding="utf-8") as f:
    json.dump(evidence, f, indent=2)
print(f"\nWritten Track C evidence to: {EVIDENCE_OUT}")
