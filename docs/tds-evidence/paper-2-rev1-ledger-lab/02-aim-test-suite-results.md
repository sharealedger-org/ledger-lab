# AIM Test Suite Results & Specifications (Paper 2, Rev 1)

**Status:** `verified` / `partially_verified` (as noted per test)  
**Reference Document:** [`docs/TEST_DESIGN.md`](../../TEST_DESIGN.md) (AIM Test Specifications)  
**Corpus Scope:** State of Virginia Public Procurement & Expenditure Data (FY2003–FY2016)  

---

## 1. Executive Summary of AIM Test Suite

The **AIM (Accounting Information Loss in Machine Learning)** test suite provides concrete, falsifiable empirical tests evaluating what information traditional posting discards and what machine learning models gain from pre-posting event structures:

Updated 2026-11-13 (Intel Mac, Gold final views).

| Test ID | Task Description | Empirical Result | Locked Threshold | Result Status |
|---|---|---|---|---|
| **AIM0** | Purchase Order to Payment Linking | Linkable only at Agency/Fiscal Year grain | N/A (Structural discovery) | `closed` |
| **AIM1** | Information Loss in General Ledger Posting | Non-zero gap in **all 14 years** (FY2003–FY2016); FY2012: 17,163 IL keys vs. 109,725 GL keys with multiple vendors | Non-injective mapping (measured) | **`partially_verified`** |
| **AIM2** | Multi-Horizon Expenditure Forecasting | Subsumed into the Three-Model Task A benchmark | $R^2 > 0.70$ | `subsumed` |
| **AIM3** | Vendor Churn Prediction (Hold-out Test) | **C-statistic = 0.7527**, Permutation $p = 0.001$ | $C > 0.65$, $p < 0.05$ | **`partially_verified` (PASS)** |
| **AIM3 balance baseline** | Does the commitment signal exceed posted totals? | Balance C = 0.7783 vs. commitment C = 0.7527 (delta +0.026). Training splits differ (7 vs. 4 years). Time-trend control C = 0.50 (FAIL). | — | **`not_verified` — no measured gain from commitment patterns over posted totals on this unmatched comparison** |
| **AIM4** | Year-End Budget Spending Spike Detection | Q4-ratio test: 2 of 5 top classes significant (threshold ≥ 3). FAIL. STL method not yet implemented. | ≥ 3 of top 5 with p < 0.05 | `not run` (locked STL pending) |
| **AIM5** | Agency Name Entity Disambiguation | **Precision = 1.0** on 312 matches; 12 of 13 unmatched flagged ambiguous | Precision > 0.95 | **`verified` (PASS)** |

---

## 2. Detailed Empirical Findings

### AIM1: Information Loss in Posting (Non-Injectivity Proof)
* **Hypothesis:** General Ledger posting is a non-injective mapping ($f: \text{Events} \to \text{Balances}$) that permanently collapses distinct event sequences into identical balance keys.
* **Measured Result (All 14 years, FY2003–FY2016, 2026-11-13 re-run on Gold final data):**
  * Non-zero gap confirmed in every year. Sample: FY2012: 17,163 IL keys with multiple commitment/account patterns vs. 109,725 GL keys with multiple vendors (19.9% of all GL keys). FY2003: gap -55,915; FY2016: gap -65,975.
  * Every IL vendor key (ldgrIPID) maps to multiple distinct (commitmentID, account) pattern tuples (100% collapse ratio in all years).
  * Evidence: `data/test_evidence/AIM1_noninjectivity_all_years.csv` (42 rows, 3 substrates × 14 years).
* **Significance:** Non-injectivity is a property of the posting key, so every collapsed key is a case where distinct commitment patterns share one balance key. The counts above measure how often that happens each year. They do not test whether a model trained on GL balances fails to distinguish those histories; that test has not been run.

---

### AIM3: Longitudinal Vendor Churn Prediction
* **Hypothesis:** Pre-posting purchase order commitment patterns (amendment frequency, order lead-time variance, late-quarter order shifts) contain predictive signal for vendor relationship termination that is absent from posted expenditure balances.
* **Experimental Protocol:**
  * **Training Cohort:** FY2010–FY2013 purchase order event sequences.
  * **Hold-Out Evaluation Set:** FY2014–FY2015 vendor activity.
  * **Target Definition:** Vendor churning in FY2015 (zero active transactions following regular historical activity).
  * **Validation Windows:** Four expanding walk-forward windows (C from 0.7546 to 0.7713), not the seven originally locked. FY2009 has no PO file, which leaves FY2010–FY2015 as the only contiguous eligible run.
* **Measured Result:**
  * **C-Statistic (AUC-ROC):** **`0.7527`** (exceeds locked threshold $C > 0.65$).
  * **Permutation Significance:** **`p = 0.001`** across 1,000 random label permutations (exceeds locked threshold $p < 0.05$).
* **Significance:** Commitment-pattern model passes the locked threshold (C = 0.7527 > 0.65; p = 0.001 < 0.05). Result confirmed on the Gold final views (2026-11-13 re-run, Intel Mac). Walk-forward mean C = 0.7629 (4 windows, stable).
* **Balance-derived baseline (2026-11-13, new):** A logistic model on posted LDGR features (debit_amount, credit_amount, row_count, distinct_accts, distinct_periods) achieves C = 0.7783 on the same hold-out (different training split: FY2006–2014 vs. FY2010–2013). Delta commitment vs. balance: -0.0256. Time-trend control (year only): C = 0.50 (FAIL, p = 0.518). **Interpretation: the balance model scores higher on this comparison, and the commitment-pattern view adds no measured gain over posted totals. The splits are not matched, so this is not yet a like-for-like result.** A matched-split comparison is the open next step. Evidence: `data/test_evidence/AIM3_balance_baseline.csv`.
* **Caveats:** Four training years (commitment model), four features, linear model; churn rate rises from 24% to 33% over FY2012–FY2015 (time trend not yet isolated). The AIM3 claim that "pre-posting patterns contain signal absent from posted balances" is `not_verified` pending a matched-split comparison with balance features as a covariate.

---

### AIM5: Agency Name Entity Disambiguation
* **Hypothesis:** Deterministic entity resolution rules on noisy administrative agency names can reliably map legacy department identifiers to standardized state agency codes without corrupting financial lineage.
* **Measured Result:**
  * **Precision:** **`1.000`** on 312 held-out agency name test pairs.
  * **Ambiguity Handling:** 12 of 13 unmatched or conflicting strings were correctly flagged as ambiguous rather than falsely mapped.
