# AIM Test Suite Results & Specifications (Paper 2, Rev 1)

**Status:** `verified` / `partially_verified` (as noted per test)  
**Reference Document:** [`docs/TEST_DESIGN.md`](../../TEST_DESIGN.md) (AIM Test Specifications)  
**Corpus Scope:** State of Virginia Public Procurement & Expenditure Data (FY2003–FY2016)  

---

## 1. Executive Summary of AIM Test Suite

The **AIM (Accounting Information Loss in Machine Learning)** test suite provides concrete, falsifiable empirical tests evaluating what information traditional posting discards and what machine learning models gain from pre-posting event structures:

Results from the Intel Mac run on Gold final views (CSV run timestamps 2026-11-12 20:37 and 20:43), reproduced on an ARM Mac (2026-11-12 21:32 to 21:35 local) with identical AIM1, AIM3 and AIM4 results. The AIM3 balance baseline is the ARM matched-split run; the Intel balance figure (C = 0.7783) is withdrawn because FY2014 was in both train and test. Run record: `data/test_evidence/aim_run_arm64_20261113/` in the ledger-lab repository.

| Test ID | Task Description | Empirical Result | Locked Threshold | Result Status |
|---|---|---|---|---|
| **AIM0** | Purchase Order to Payment Linking | Linkable only at Agency/Fiscal Year grain | N/A (Structural discovery) | `closed` |
| **AIM1** | Information Loss in General Ledger Posting | Non-zero gap in **all 14 years** (FY2003–FY2016); FY2012: 17,163 IL keys vs. 109,725 GL keys with multiple vendors | Non-injective mapping (measured) | **`partially_verified`** |
| **AIM2** | Multi-Horizon Expenditure Forecasting | Subsumed into the Three-Model Task A benchmark | $R^2 > 0.70$ | `subsumed` |
| **AIM3** | Vendor Churn Prediction (Hold-out Test) | **C-statistic = 0.7527**, Permutation $p = 0.001$ | $C > 0.65$, $p < 0.05$ | **`partially_verified` (PASS)** |
| **AIM3 balance baseline** | Does the commitment signal exceed posted totals? | Matched split (train FY2010–2013, test FY2014–2015): balance C = 0.7683 vs. commitment C = 0.7527 (delta +0.016). Time-trend control C = 0.50 (FAIL). | — | **`partially_verified` (provisional) — commitment model alone does not exceed posted totals; balance plus view features adds C +0.008 on the matched split (see Combined model below)** |
| **AIM4** | Year-End Budget Spending Spike Detection | Q4-ratio test: 2 of 5 top classes significant (threshold ≥ 3). FAIL. STL method not yet implemented. | ≥ 3 of top 5 with p < 0.05 | `not run` (locked STL pending) |
| **AIM5** | Agency Name Entity Disambiguation | **Precision = 1.0** on 312 matches; 12 of 13 unmatched flagged ambiguous | Precision > 0.95 | **`verified` (PASS)** |

---

## 2. Detailed Empirical Findings

### AIM1: Information Loss in Posting (Non-Injectivity Proof)
* **Hypothesis:** General Ledger posting is a non-injective mapping ($f: \text{Events} \to \text{Balances}$) that permanently collapses distinct event sequences into identical balance keys.
* **Measured Result (All 14 years, FY2003–FY2016, Intel Mac run on Gold final data, CSV timestamps 2026-11-12):**
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
* **Significance:** Commitment-pattern model passes the locked threshold (C = 0.7527 > 0.65; p = 0.001 < 0.05). Result confirmed on the Gold final views on two machines: Intel Mac (CSV timestamps 2026-11-12 20:37 to 20:43) and ARM Mac (2026-11-12 21:33 local), with identical results and the 44 Gold input files verified by SHA-256 against the Gold manifest. Walk-forward mean C = 0.7629 (4 windows, stable).
* **Balance-derived baseline (ARM Mac run, matched split):** A logistic model on posted LDGR features (debit_amount, credit_amount, row_count, distinct_accts, distinct_periods) trained on FY2010–2013 and tested on FY2014–2015, the same split as the commitment model, achieves C = 0.7683 (permutation p = 0.001). Delta balance vs. commitment: +0.0156. Over the same four walk-forward windows the balance mean C is 0.7780 against 0.7629. Time-trend control (year only): C = 0.50 (FAIL, p = 0.530). **Interpretation: on a matched split the commitment model alone does not exceed the balance model.** An earlier Intel-run figure of C = 0.7783 is withdrawn: its training years included FY2014, which was also a test year. Evidence: `data/test_evidence/AIM3_balance_baseline.csv`; run record `data/test_evidence/aim_run_arm64_20261113/`.
* **Caveats:** Four training years (commitment model), four features, linear model; churn rate rises from 24% to 33% over FY2012–FY2015 (time trend not isolated beyond the year-only control).
* **Combined model (balance plus view features, matched split):** Balance features plus four view features (po_count, agency_count, distinct_nigp_classes, total_amount) reach C = 0.7761 against 0.7683 for balance alone: delta +0.0078, bootstrap 95% interval [+0.0066, +0.0091] (resampling hold-out rows), permutation p = 0.001. Script `scripts/fit_vendor_survival_combined.py`; output `data/test_evidence/AIM3_combined_model.csv`. Pre-declared rule (positive delta, interval above zero) reports COMMITMENT_ADDS_SIGNAL. **Provisional:** one split; the bootstrap does not vary training years or the fit; no multiple-comparison correction; no stability check across training windows; practical size under one point of C is not established. The AIM3 claim that "pre-posting patterns contain signal absent from posted balances" is therefore `partially_verified` (provisional).

---

### AIM5: Agency Name Entity Disambiguation
* **Hypothesis:** Deterministic entity resolution rules on noisy administrative agency names can reliably map legacy department identifiers to standardized state agency codes without corrupting financial lineage.
* **Measured Result:**
  * **Precision:** **`1.000`** on 312 held-out agency name test pairs.
  * **Ambiguity Handling:** 12 of 13 unmatched or conflicting strings were correctly flagged as ambiguous rather than falsely mapped.
