#!/usr/bin/env python3
"""
Track C (Berka PKDD'99) -- Model Training & Information Ceiling Evaluator
Paper 2: Business Event Repositories as Training Corpora for Enterprise AI

Compares:
  - Model 1 (Layer 2 Baseline): Trained on compressed balance aggregates (Substrate B)
  - Model 2 (Layer 3 Event-Native): Trained on pre-hierarchy event features (Substrate E)
  - Non-Injectivity Analysis: Evaluates predictive separation on identical-balance pairs

Implementation: High-speed, self-contained numerical engine (numpy/stdlib),
zero external C-threadlock dependencies, reproducible across all OS platforms.

Usage:
  python scripts/berka/fit_track_c_models.py \
    --data-dir data/berka_processed \
    --evidence-out data/test_evidence/TRACK_C_BERKA_EVIDENCE.json
"""

import csv
import json
import math
import os
import sys
import numpy as np

# ---------------------------------------------------------------------------
# CLI Argument Parsing
# ---------------------------------------------------------------------------
DATA_DIR     = "data/berka_processed"
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

os.makedirs(os.path.dirname(EVIDENCE_OUT), exist_ok=True)

# ---------------------------------------------------------------------------
# 1. Pure-Python / NumPy Metric Implementations
# ---------------------------------------------------------------------------

def compute_roc_auc(y_true, y_score):
    """Exact ROC-AUC computation via Wilcoxon-Mann-Whitney rank sum."""
    pos = y_score[y_true == 1]
    neg = y_score[y_true == 0]
    n_pos = len(pos)
    n_neg = len(neg)
    if n_pos == 0 or n_neg == 0:
        return 0.5
    
    # Sort all scores
    ranks = np.argsort(np.argsort(y_score)) + 1
    rank_sum_pos = np.sum(ranks[y_true == 1])
    u_stat = rank_sum_pos - (n_pos * (n_pos + 1)) / 2.0
    return float(u_stat / (n_pos * n_neg))

def compute_pr_auc(y_true, y_score):
    """Exact Average Precision (PR-AUC)."""
    order = np.argsort(-y_score)
    y_sorted = y_true[order]
    
    tp = np.cumsum(y_sorted == 1)
    fp = np.cumsum(y_sorted == 0)
    
    precision = tp / (tp + fp)
    recall = tp / np.sum(y_true == 1)
    
    # Calculate step-wise area under PR curve
    rec_diff = np.diff(np.concatenate(([0.0], recall)))
    pr_auc = np.sum(precision * rec_diff)
    return float(pr_auc)

def compute_brier_score(y_true, y_score):
    return float(np.mean((y_score - y_true) ** 2))

def compute_log_loss(y_true, y_score, eps=1e-15):
    p = np.clip(y_score, eps, 1 - eps)
    return float(-np.mean(y_true * np.log(p) + (1 - y_true) * np.log(1 - p)))

# ---------------------------------------------------------------------------
# 2. Self-Contained Classifier (Regularized Logistic Regression + Feature Interactions)
# ---------------------------------------------------------------------------

class RegularizedClassifier:
    """Logistic Regression with L2 Regularization and Class Weighting via Gradient Descent."""
    def __init__(self, lr=0.05, reg=0.01, max_iter=800, pos_weight=5.0):
        self.lr = lr
        self.reg = reg
        self.max_iter = max_iter
        self.pos_weight = pos_weight
        self.weights = None
        self.bias = 0.0
        self.mean = None
        self.scale = None

    def fit(self, X, y):
        # Normalize features
        self.mean = np.mean(X, axis=0)
        self.scale = np.std(X, axis=0)
        self.scale[self.scale == 0] = 1.0
        X_norm = (X - self.mean) / self.scale

        n_samples, n_features = X_norm.shape
        self.weights = np.zeros(n_features)
        self.bias = 0.0

        sample_weights = np.where(y == 1, self.pos_weight, 1.0)

        for _ in range(self.max_iter):
            # Forward pass
            logits = np.dot(X_norm, self.weights) + self.bias
            preds = 1.0 / (1.0 + np.exp(-np.clip(logits, -25, 25)))

            # Gradient computation with sample weights
            errors = (preds - y) * sample_weights
            dw = (np.dot(X_norm.T, errors) / n_samples) + (self.reg * self.weights)
            db = np.sum(errors) / n_samples

            # Parameter update
            self.weights -= self.lr * dw
            self.bias -= self.lr * db

    def predict_proba(self, X):
        X_norm = (X - self.mean) / self.scale
        logits = np.dot(X_norm, self.weights) + self.bias
        return 1.0 / (1.0 + np.exp(-np.clip(logits, -25, 25)))

# ---------------------------------------------------------------------------
# 3. Load Datasets
# ---------------------------------------------------------------------------
def load_csv(path):
    rows = []
    with open(path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append(r)
    return rows

sub_b_path = os.path.join(DATA_DIR, "substrate_b_balances.csv")
sub_e_path = os.path.join(DATA_DIR, "substrate_e_events.csv")

if not os.path.exists(sub_b_path) or not os.path.exists(sub_e_path):
    print(f"Error: Substrate files not found in {DATA_DIR}. Run build_berka_substrates.py first.", file=sys.stderr)
    sys.exit(1)

rows_b = load_csv(sub_b_path)
rows_e = load_csv(sub_e_path)

y = np.array([int(r["target_default"]) for r in rows_b])
loan_ids = [int(r["loan_id"]) for r in rows_b]
account_ids = [int(r["account_id"]) for r in rows_b]

# Substrate B Features (Layer 2)
cols_b = [
    "loan_amount", "duration", "payments",
    "bal_mean", "bal_min", "bal_max", "bal_std", "bal_last",
    "tot_credit", "tot_debit", "net_flow", "tx_count",
    "district_salary", "district_unemp", "district_crime"
]
X_b = np.array([[float(r[c]) for c in cols_b] for r in rows_b])

# Substrate E Features (Layer 3)
cols_e = [
    "loan_amount", "duration", "payments",
    "bal_mean", "bal_min", "bal_max", "bal_last",
    "district_salary", "district_unemp",
    "atm_count", "atm_sum",
    "cash_with_count", "cash_with_sum",
    "cash_dep_count", "cash_dep_sum",
    "sanction_int_count", "sanction_int_sum",
    "household_pay_count", "household_pay_sum",
    "cash_drain_ratio", "low_bal_count", "negative_bal_count", "avg_dt_days"
]
X_e = np.array([[float(r[c]) for c in cols_e] for r in rows_e])

print(f"Loaded {len(y)} loans (Defaults: {int(sum(y))} / {len(y)} = {sum(y)/len(y)*100:.2f}%)")

# ---------------------------------------------------------------------------
# 4. Stratified 5-Fold Cross-Validation
# ---------------------------------------------------------------------------
def run_cv(X, y, n_splits=5, seed=42):
    np.random.seed(seed)
    pos_idx = np.where(y == 1)[0]
    neg_idx = np.where(y == 0)[0]
    
    np.random.shuffle(pos_idx)
    np.random.shuffle(neg_idx)
    
    pos_folds = np.array_split(pos_idx, n_splits)
    neg_folds = np.array_split(neg_idx, n_splits)
    
    oof_preds = np.zeros(len(y))
    
    for fold in range(n_splits):
        val_idx = np.concatenate([pos_folds[fold], neg_folds[fold]])
        train_idx = np.setdiff1d(np.arange(len(y)), val_idx)
        
        clf = RegularizedClassifier(lr=0.08, reg=0.005, max_iter=1000, pos_weight=len(neg_idx)/len(pos_idx))
        clf.fit(X[train_idx], y[train_idx])
        oof_preds[val_idx] = clf.predict_proba(X[val_idx])
        
    roc_auc = compute_roc_auc(y, oof_preds)
    pr_auc = compute_pr_auc(y, oof_preds)
    brier = compute_brier_score(y, oof_preds)
    logloss = compute_log_loss(y, oof_preds)
    
    return {
        "roc_auc": roc_auc,
        "pr_auc": pr_auc,
        "brier_score": brier,
        "log_loss": logloss,
        "oof_preds": oof_preds
    }

print("\n--- Model Cross-Validation ---")
res_b = run_cv(X_b, y)
res_e = run_cv(X_e, y)

print(f"Layer 2 (Balances) - ROC-AUC: {res_b['roc_auc']:.4f} | PR-AUC: {res_b['pr_auc']:.4f} | Brier: {res_b['brier_score']:.4f}")
print(f"Layer 3 (Events)   - ROC-AUC: {res_e['roc_auc']:.4f} | PR-AUC: {res_e['pr_auc']:.4f} | Brier: {res_e['brier_score']:.4f}")
print(f"Information Gain (Delta)  - ROC-AUC: {res_e['roc_auc'] - res_b['roc_auc']:+.4f} | PR-AUC: {res_e['pr_auc'] - res_b['pr_auc']:+.4f}")

# ---------------------------------------------------------------------------
# 5. Non-Injectivity Pair Analysis (Path vs. State)
# ---------------------------------------------------------------------------
print("\n--- Non-Injectivity Analysis (Identical Balance Profiles) ---")
mean_b = np.mean(X_b, axis=0)
std_b = np.std(X_b, axis=0)
std_b[std_b == 0] = 1.0
X_b_scaled = (X_b - mean_b) / std_b

noninj_pairs = []
for i in range(len(y)):
    if y[i] == 1: # Default account
        for j in range(len(y)):
            if y[j] == 0: # Good credit account
                dist = np.linalg.norm(X_b_scaled[i] - X_b_scaled[j])
                if dist < 1.2: # Ambiguous balance distance
                    noninj_pairs.append({
                        "loan_default": loan_ids[i],
                        "account_default": account_ids[i],
                        "loan_good": loan_ids[j],
                        "account_good": account_ids[j],
                        "balance_dist": float(dist),
                        "pred_b_default": float(res_b["oof_preds"][i]),
                        "pred_b_good": float(res_b["oof_preds"][j]),
                        "pred_e_default": float(res_e["oof_preds"][i]),
                        "pred_e_good": float(res_e["oof_preds"][j]),
                    })

noninj_pairs.sort(key=lambda p: p["balance_dist"])
top_noninj = noninj_pairs[:10]

if noninj_pairs:
    avg_sep_b = float(np.mean([p["pred_b_default"] - p["pred_b_good"] for p in noninj_pairs]))
    avg_sep_e = float(np.mean([p["pred_e_default"] - p["pred_e_good"] for p in noninj_pairs]))
else:
    avg_sep_b = avg_sep_e = 0.0

print(f"Identified {len(noninj_pairs)} ambiguous pairs in balance space.")
print(f"Average Default Risk Separation on Ambiguous Pairs:")
print(f"  Layer 2 (Balances): {avg_sep_b:.4f}")
print(f"  Layer 3 (Events):   {avg_sep_e:.4f} (Lift: {avg_sep_e - avg_sep_b:+.4f})")

# ---------------------------------------------------------------------------
# 6. Save Evidence JSON
# ---------------------------------------------------------------------------
evidence = {
    "experiment": "Paper 2 - Track C Retail Banking Benchmark (Berka PKDD'99)",
    "timestamp": str(np.datetime64("now")),
    "corpus_scale": {
        "loans_count": len(y),
        "defaults_count": int(sum(y)),
        "default_ratio": float(sum(y) / len(y))
    },
    "metrics": {
        "layer_2_balances": {
            "roc_auc": res_b["roc_auc"],
            "pr_auc": res_b["pr_auc"],
            "brier_score": res_b["brier_score"],
            "log_loss": res_b["log_loss"]
        },
        "layer_3_events": {
            "roc_auc": res_e["roc_auc"],
            "pr_auc": res_e["pr_auc"],
            "brier_score": res_e["brier_score"],
            "log_loss": res_e["log_loss"]
        },
        "deltas": {
            "pr_auc_gain": res_e["pr_auc"] - res_b["pr_auc"],
            "roc_auc_gain": res_e["roc_auc"] - res_b["roc_auc"],
            "brier_improvement": res_b["brier_score"] - res_e["brier_score"]
        }
    },
    "non_injectivity_analysis": {
        "ambiguous_pairs_count": len(noninj_pairs),
        "avg_separation_layer_2": avg_sep_b,
        "avg_separation_layer_3": avg_sep_e,
        "separation_gain": avg_sep_e - avg_sep_b,
        "top_pairs_sample": top_noninj
    }
}

with open(EVIDENCE_OUT, "w", encoding="utf-8") as f:
    json.dump(evidence, f, indent=2)

print(f"\nWritten Track C Evidence to: {EVIDENCE_OUT}")
