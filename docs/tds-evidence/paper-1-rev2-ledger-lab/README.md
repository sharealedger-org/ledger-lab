# TDS Evidence Package — Paper 1 (Revision 2): Ledger Lab Cost Curve

**Version:** Paper 1, Revision 2  
**Date:** 2026-11-12  
**Repository Source:** `sharealedger/ledger-lab` (Commit: `ef78576`, Baseline Tag: `companion-measured-baseline-20261111` on `c3667e4`)  
**Target TDS Path:** `01-evidence/practitioner/Sharealedger/`  

---

## 1. Package Purpose & Scope

This package contains the empirical evidence, mathematical cost models, and reference data for **Paper 1 (Revision 2)**. It presents the cost-curve comparison between traditional posted General Ledger architectures and the Common Key Buffer (CKB) Instrument Ledger architecture using 14 years of public Virginia accounting and purchasing data (FY2003–FY2016).

*Note on Revision 2 Scope:* In accordance with the experiment design rules, Revision 2 presents the measured CKB baseline and mathematical cost-bowl model independently of the Track D unconstrained control repository results and deferred Part B novel-permutation questions.

---

## 2. Document Inventory

| Filename | Description | Primary Evidence / Purpose |
|---|---|---|
| [`README.md`](README.md) | Package index, evidence taxonomy, and standardized naming key. | Orientation & alignment. |
| [`01-measured-results-summary.md`](01-measured-results-summary.md) | Measured corpus statistics across 14 years (Monthly point runs, GL keys, views, capacity bound, deterministic output verifications). | Empirical measurements. |
| [`02-mix-cost-curve-model.md`](02-mix-cost-curve-model.md) | Mathematical formulation of the Mix Cost Curve (Events ↔ Balances), measured constants, assumed unit rates, and coordinates. | Analytical model. |
| [`03-charlie-anderson-model.xlsx`](03-charlie-anderson-model.xlsx) | Four-sheet interactive Excel workbook (`Model_Parameters`, `Cost_Equations`, `Cost_Bowl_Data`, `Multi_Substrate_Comparison`). | Formula verification. |
| [`04-one-page-explainer.html`](04-one-page-explainer.html) | Self-contained, browser-viewable visual summary of the cost bowl, architectural layers, and findings. | Executive summary. |
| [`05-limits-and-phase2-roadmap.md`](05-limits-and-phase2-roadmap.md) | Known experimental limits, unmetered orchestration overheads, and the Phase 2 execution roadmap. | Scope bounds & roadmap. |

---

## 3. Evidence Status Taxonomy

Every claim and figure in this package is tagged with one of five standardized status classifications:

| Status Word | Formal Definition |
|---|---|
| `verified` | Directly measured, checked against formal verification rules, and passing 100% of determinism and control assertions. |
| `partially_verified` | Measured and passing on a subset of the full scope (e.g., fixture tests, specific years, or single-host configuration). |
| `owner-reported` | Information or design parameters provided by the repository owner. |
| `inferred` | Reasoned analytically from code structure or execution profiles rather than isolated stopwatch timing. |
| `not run` | Formally planned or coded, but not executed at full 14-year corpus scale. |

---

## 4. Standardized Naming Key (2026-11-12 Standard)

To prevent confusion between run numbers, cost points, and graph axes, the following standard naming keys apply across all Revision 2 artifacts:

### A. Graph Points (Mix Dial: Events ↔ Balances)
* **Events (P1 / $X=0$):** Pure event replay from raw transaction store.
* **Annual (P2 / $X=15$):** Annual movement store (`T-annual`) + sub-annual query fallback scans.
* **Monthly (P3 / $X=30$):** Monthly movement Instrument Ledger (`LDGR` + CAR) — **The Cost Bowl Minimum**.
* **Cubes (P4 / $X=60$):** Movement ledger with auxiliary pre-computed rollup cubes.
* **Posted (P5 / $X=90$):** Status-quo General Ledger + 5 independent sub-ledgers.
* **Permuted (P6 / $X=100$):** Combinatorial multi-master balance tables (storing full permutation capacity bound).

### B. Process Points (Financial Processing Layers)
* **Posted GL (CCP1):** Baseline General Ledger posting discarding instrument identity.
* **Ledger (CCP2):** Instrument Ledger posting preserving instrument, account, and period.
* **Views (CCP3):** Bounded analytical summary views generated in the same pass.
* **Revaluation (CCP4):** Balance revaluation and reclassification SJE generation.
* **Allocation (CCP5):** Prior-day divisor cost allocation and balance distribution.

### C. Corpus Runs (Measurements at the Monthly Point)
* **Baseline (`run_20261110_210721`):** Serial 14-year run (`5v` view pass executed after chain completion).
* **Run 3 (`run_20261111_014229`):** 2-lane parallel chain layout with early `5v` start.
* **Run 4 (`run_20261111_064052`):** 2-lane parallel chain layout with active OS keep-awake assertion (`caffeinate`).
