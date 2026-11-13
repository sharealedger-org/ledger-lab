# AIM Test Suite Results & Specifications (Paper 2, Rev 1)

**Status:** `verified` / `partially_verified` (as noted per test)  
**Reference Document:** [`docs/TEST_DESIGN.md`](../../TEST_DESIGN.md) (AIM Test Specifications)  
**Corpus Scope:** State of Virginia Public Procurement & Expenditure Data (FY2003–FY2016)  

---

## 1. Executive Summary of AIM Test Suite

The **AIM (Accounting Information Loss in Machine Learning)** test suite provides concrete, falsifiable empirical tests evaluating what information traditional posting discards and what machine learning models gain from pre-posting event structures:

| Test ID | Task Description | Empirical Result | Locked Threshold | Result Status |
|---|---|---|---|---|
| **AIM0** | Purchase Order to Payment Linking | Linkable only at Agency/Fiscal Year grain | N/A (Structural discovery) | `closed` |
| **AIM1** | Information Loss in General Ledger Posting | **17,163 GL keys** hold multiple order patterns; **12,965 keys** hold multiple vendors | Non-injective mapping proof | `partially_verified` |
| **AIM2** | Multi-Horizon Expenditure Forecasting | Subsumed into the Three-Model Task A benchmark | $R^2 > 0.70$ | `subsumed` |
| **AIM3** | Vendor Churn Prediction (Hold-out Test) | **C-statistic = 0.7527**, Permutation $p = 0.001$ | $C > 0.65$, $p < 0.05$ | **`partially_verified` (PASS)** |
| **AIM4** | Year-End Budget Spending Spike Detection | Fail under simple Q4 heuristic; pending STL re-run | F1 > 0.70 | `not run` (STL pending) |
| **AIM5** | Agency Name Entity Disambiguation | **Precision = 1.0** on 312 matches; 12 of 13 unmatched flagged ambiguous | Precision > 0.95 | **`verified` (PASS)** |

---

## 2. Detailed Empirical Findings

### AIM1: Information Loss in Posting (Non-Injectivity Proof)
* **Hypothesis:** General Ledger posting is a non-injective mapping ($f: \text{Events} \to \text{Balances}$) that permanently collapses distinct event sequences into identical balance keys.
* **Measured Result (FY2012 Corpus):**
  * **17,163 GL keys** aggregate transactions originating from multiple distinct purchase order commitment histories.
  * **12,965 permutation keys** aggregate expenditures across multiple distinct vendors into a single aggregated balance total.
* **Significance:** Mathematically proves that an AI model trained strictly on GL balances cannot distinguish between smooth multi-month vendor contracts and volatile lump-sum orders that arrive at year-end.

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
* **Significance:** Shows that the commitment-pattern model passes the locked threshold on this hold-out. It does not yet show that commitment sequences add signal beyond posted totals, because no balance-derived baseline has been run. Caveats: four training years, four features, linear model; the churn rate rises from 24% to 33% over FY2012–FY2015, so some signal may be a time trend. The balance-derived baseline comparison and a re-run on the final views are pending (ledger-lab `TODO.md`).

---

### AIM5: Agency Name Entity Disambiguation
* **Hypothesis:** Deterministic entity resolution rules on noisy administrative agency names can reliably map legacy department identifiers to standardized state agency codes without corrupting financial lineage.
* **Measured Result:**
  * **Precision:** **`1.000`** on 312 held-out agency name test pairs.
  * **Ambiguity Handling:** 12 of 13 unmatched or conflicting strings were correctly flagged as ambiguous rather than falsely mapped.
