# TDS Evidence Package — Paper 2 (Revision 1): AI Information Loss & Three-Model Experiment

**Version:** Paper 2, Revision 1  
**Date:** 2026-11-13  
**Repository Source:** `sharealedger/ledger-lab` (Commit: `6b53bd9`, Baseline Tag: `companion-measured-baseline-20261111` on `c3667e4`)  
**Target TDS Path:** `01-evidence/practitioner/Sharealedger/paper-2-rev1-ledger-lab/`  

---

## 1. Package Purpose & Scope

This package contains the empirical evidence, experimental designs, validation proofs, and preliminary test results for **Paper 2**: *Business Event Repositories as Training Corpora for Enterprise AI: An Information-Theoretic Analysis of Structured Data Compression in Business Systems*.

It provides the quantitative foundation for the central claim of Paper 2: that traditional financial posting is **lossy compression in the Shannon sense** that permanently destroys causal sequence, counterparty identity, and contract context—placing a hard ceiling on what AI models trained on posted General Ledger summaries can learn.

---

## 2. Document Inventory

| Filename | Description | Primary Evidence / Purpose |
|---|---|---|
| [`README.md`](README.md) | Package index, evidence taxonomy, and model/test naming standard. | Alignment & navigation. |
| [`01-three-model-experiment-design.md`](01-three-model-experiment-design.md) | Formal design of the 3-model experiment (Posted-FineTune vs. IL-FineTune vs. IL-Native), "See Spot Run" verbalization trap, entity splits, and decisions D1–D8. | Core experimental design. |
| [`02-aim-test-suite-results.md`](02-aim-test-suite-results.md) | Detailed empirical results and protocol definitions for AIM0 through AIM5 (non-injectivity, vendor churn C=0.7527, entity disambiguation). | Empirical test evidence. |
| [`03-gold-data-ground-truth-validation.md`](03-gold-data-ground-truth-validation.md) | Validation of the 14-year Gold reference dataset (104/104 files SHA-256 verified, GL vs. LDGR exact-cent debits tie-out across all 14 fiscal years). | Data integrity proof. |
| [`04-limits-and-phase2-roadmap.md`](04-limits-and-phase2-roadmap.md) | Experimental limits (D9 net income decision, expenditure basis), and the execution roadmap for Steps 5–7 (model training and scoring). | Boundary & execution roadmap. |

---

## 3. Evidence Status Taxonomy

| Status Word | Formal Definition |
|---|---|
| `verified` | Directly measured, passing automated test assertions with 100% determinism and control verification. |
| `partially_verified` | Verified on a specific subset (e.g. FY2012 non-injectivity, hold-out FY14–FY15 churn, single host). |
| `owner-reported` | Strategic direction or domain design parameters established by the principal investigator. |
| `inferred` | Derived analytically from code logic or database structure. |
| `not run` | Formally planned or coded, but execution awaiting prerequisite decisions (e.g. LLM fine-tuning). |

---

## 4. Standardized Model & Test Naming Key

### A. Three-Model Comparison Setup
1. **Model 1 (Posted-FineTune):** Pretrained Apache 2.0 LLM fine-tuned on verbalized General Ledger (`GL`) data. Represents the industry status quo (verbalized text on compressed balance records).
2. **Model 2 (IL-FineTune):** Identical pretrained LLM fine-tuned on verbalized Instrument Ledger (`LDGR`) records. Measures the **substrate gain** (Instrument Ledger vs. GL with language prior held constant).
3. **Model 3 (IL-Native):** Structured sequence/tabular model built directly from Instrument Ledger records with **no natural language prior**. Measures the **"See Spot Run" language prior penalty** (IL-FineTune vs. IL-Native).

### B. AIM (Accounting Information Loss in Machine Learning) Test Suite
* **AIM0 (PO to Payment Linking):** Entity resolution between purchase orders and disbursements.
* **AIM1 (Information Loss in Posting):** Mathematical and empirical proof of non-injectivity in GL posting.
* **AIM2 (Expenditure Forecasting from PO Commitments):** Multi-horizon expenditure forecasting.
* **AIM3 (Vendor Churn Prediction):** Longitudinal classification of supplier relationship termination.
* **AIM4 (Year-End Spending Spikes):** Detection of budget exhaustion surges using STL seasonal decomposition.
* **AIM5 (Agency Entity Disambiguation):** Cross-system entity resolution on noisy agency names.
