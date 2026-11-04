# Ledger Lab — What We're Trying to Show

**Start here if you haven't read the papers.**  
This document explains what Ledger Lab is testing and why, in plain English. No academic background required. The papers are referenced at the bottom for anyone who wants the full argument.

---

## The one-paragraph version

Every major financial institution has spent decades summarizing its raw business records into pre-computed totals — balances, subledgers, General Ledgers. Those summaries are useful for accounting, but they permanently throw away information: *who* ordered what, *when* orders were placed, *what sequence* of events preceded an outcome, *which vendor relationships* lasted and which didn't. Ledger Lab tests whether that thrown-away information would have been useful for prediction. If it would — if a model trained on the raw records before they were summarized performs better than a model trained on the summaries — then every institution that summarized its data has an AI disadvantage that it cannot fix retroactively.

---

## The one question behind all the tests

> **Does preserving raw business events, before they are summarized into account balances, give you information that the summaries cannot provide?**

All four tests below are different ways of answering this question using the Virginia state government procurement data — a public dataset where both the raw ordering records (purchase orders) and the summarized payment records (expenditures) are available for comparison.

---

## The three-layer picture

When an organization processes a financial transaction, the data passes through several stages. Each stage summarizes and discards:

| Layer | What it contains | What gets discarded |
|-------|-----------------|---------------------|
| **Layer 3 — Raw commitment data** | Purchase orders: what was ordered, when, from whom, what commodity, revised how many times | Nothing yet — this is the full record |
| **Layer 2 — Posted payment data** | Expenditure records: what was paid, to which vendor, from which agency | Order date, commodity type, order revision history, vendor entry/exit |
| **Layer 1 — Text-serialized summaries** | Natural language descriptions of transactions | Field-level structure, numeric precision, temporal sequence |

**Ledger Lab uses public Virginia procurement data because it has both Layer 3 (purchase orders) and Layer 2 (expenditure records).** This lets us directly measure what Layer 3 knows that Layer 2 does not — by training models on each and comparing their predictive power.

---

## The tests

The tests are grouped by what they are trying to show. You don't need to run them in order, but Test 2d unblocks Tests 2a and 3 (see below).

---

### Group A — *The commitment record knows things the payment record doesn't*

These tests compare prediction performance between a model that only sees payment totals (Layer 2) and a model that also sees ordering data (Layer 3).

#### Test 2a (AIM2): Does knowing *what* an agency ordered help predict *what* it will spend next year?

**The idea:** An agency's purchase order history — what commodity categories it bought, how often it revised orders, what mix of goods vs. services it ordered — is visible in the ordering system. None of this appears in the payment record, which only shows what was eventually paid to which vendor. If a model that uses ordering data forecasts next-year spending better than a model that uses only payment history, the ordering data carries predictive signal that the payment system discards.

**Data:** ~240 agency-year pairs, FY2006–FY2016.  
**Success:** The ordering-data model reduces forecast error by at least 5% over the payment-only model (threshold to be confirmed before running).  
**Full spec:** [`docs/TEST_DESIGN.md`](./docs/TEST_DESIGN.md) — Test 2a

---

#### Test 2c (AIM4): Does *when* an agency places orders tell you something that *when* it pays does not?

**The idea:** Virginia's fiscal year ends June 30. Agencies that are about to lose unspent budget are likely to place orders in Q4 (April–June) at higher-than-normal rates. This "year-end budget rush" signal lives in order dates — not payment dates. The payment record carries the date the check was cut, which might be weeks or months later. If ordering volume shows a statistically significant Q4 spike and payment volume does not, the ordering record contains timing information that the posted payment system permanently loses.

**Data:** PO files FY2006–FY2016, ~130M records, top 5 goods commodity classes.  
**Success:** At least 3 of the top 5 goods classes show a statistically significant Q4 spike in ordering volume.  
**Why run this first:** Cheapest test — requires no join between the PO and expenditure files.  
**Full spec:** [`docs/TEST_DESIGN.md`](./docs/TEST_DESIGN.md) — Test 2c

---

### Group B — *Relationship history knows things balance snapshots don't*

This test uses only the purchase order data — no join to expenditure records required. It tests whether the full lifecycle of a vendor relationship (how they entered, how active they were, how they changed over time, and when they left) is predictive — and, crucially, whether that lifecycle information is something that a payment-summary system could ever capture.

#### Test 2b (AIM3): Can we predict which vendors will stop doing business with the state?

**The idea:** A conventional accounting system records payments. It does not record vendor relationships — it has no concept of a vendor "entering" the procurement market or "exiting" it. The purchase order data does. We have 65,000 vendors with up to 12 years of annual order history. 43% of vendors appear in only one year. The question is whether features observable in a vendor's first year of ordering — what they sold, how concentrated their business was, which agencies they served — predict whether they'll still be selling to the state three years later.

If those features are predictive (C-statistic > 0.65 on held-out years), it demonstrates that vendor lifecycle information exists in pre-hierarchy data and carries real signal. That signal is structurally absent from any payment-summary system, not merely missing from this dataset — because payment systems record transactions, not relationships.

**Data:** 65,000 vendors × up to 12 fiscal years, FY2006–FY2016.  
**Success:** C-statistic > 0.65 on held-out years (FY2015–2016), permutation p < 0.05.  
**Full spec:** [`docs/TEST_DESIGN.md`](./docs/TEST_DESIGN.md) — Test 2b

---

### Group C — *Preserved names carry information that account codes erase*

When a payment system summarizes transactions to an account code, it collapses all agency name variants to a single code. A reorganization, a renaming, a funding boundary change — all invisible. The pre-hierarchy record preserves the original name string. This test asks whether an AI model can tell the difference between "same agency, different spelling" and "actually a different legal entity."

#### Test 2d (AIM5): Can an AI model distinguish a spelling variant from a genuine organizational difference?

**The idea:** 99.1% of Virginia expenditure records match agency names exactly to a reference table. 13 names (20,512 rows) don't match at all. 3 names (131,809 rows) are ambiguous — they could belong to two different reference agencies. A payment-summary system would have resolved these by picking one code and moving on, silently hiding the ambiguity. A pre-hierarchy architecture preserved the original strings, so the ambiguity is visible.

The test trains a classifier on the 99.1% exact-match cases (labeled: same agency), generates hard negatives (near-miss strings from different agencies), and applies the trained model to the 16 unresolved cases. For the 3 genuinely ambiguous cases, the model should produce low confidence — not because it failed, but because the ambiguity is real. Low confidence is the correct answer.

**Why this matters beyond data cleaning:** Any balance-posting system would have collapsed these 131,809 ambiguous rows to a single account code. The ambiguity itself — which the model detects as genuine — is information about an organizational boundary or reorganization that the posted record permanently erases.

**Data:** `agency_alias_candidates.csv` — 16 unresolved agency names, 3 genuinely ambiguous.  
**Success:** Precision ≥ 0.99 on held-out exact-match pairs; the 3 ambiguous cases produce confidence < 0.7 (flagged rather than force-resolved).  
**This test also unblocks Tests 2a and 3** by producing the agency reconciliation table cross-file joins require.  
**Full spec:** [`docs/TEST_DESIGN.md`](./docs/TEST_DESIGN.md) — Test 2d

---

## What counts as overall success

Each test has its own acceptance threshold (see [`docs/TEST_DESIGN.md`](./docs/TEST_DESIGN.md)). The overall claim Ledger Lab is testing is:

> **Pre-hierarchy data (orders, relationships, raw names) contains predictive signal that is absent from post-hierarchy data (payments, balances, account codes) — not just missing from this particular dataset, but structurally absent from any system that summarizes transactions before storing them.**

A single test confirming its hypothesis is evidence. Multiple tests confirming across different domains (forecasting, lifecycle, timing, entity resolution) is strong evidence. The tests are designed to fail independently — a negative result on one does not invalidate the others.

---

## Suggested run order

| Step | Run | AIM ID | Why |
|------|-----|--------|-----|
| 0 | **CCP1** (GL baseline + permutation table) | — | Establishes the status-quo cost and produces the legacy substrates for the full AIM comparison. |
| 1 | **AIM1–AIM5 against CCP1 substrates** | All | Run the full test suite against GL output and permutation table. All expected to miss — document the failure modes. |
| 2 | **CCP2** (Instrument Ledger baseline) | — | Left anchor of the minimum cost curve. Instrument identity now preserved. |
| 3 | **AIM1 against CCP2** | AIM1 | Post-hoc step reading the Instrument Ledger. Non-injectivity now directly measurable. Compare to CCP1 result. |
| 4 | **AIM5** (spelling vs. org difference) | AIM5 | Produces agency reconciliation table needed by AIM2 and CCP5. |
| 5 | **CCP3** (IL + Step 5 views) | — | Step 5 views ARE the AIM2–AIM4 feature tables. Delta CCP2→CCP3 = cost of producing all AI evidence. |
| 6 | **AIM4** (Q4 ordering seasonality) | AIM4 | Cheapest AIM on CCP3 views — no join, no reconciliation. |
| 7 | **AIM3** (vendor survival) | AIM3 | PO-only. Tests lifecycle/relationship claim. |
| 8 | **AIM2** (commodity forecasting) | AIM2 | Requires AIM5 agency reconciliation. Tests Layer 3 vs Layer 2 forecasting directly. |
| 9 | **CCP4, CCP5** | — | Revaluation + allocation cost points. AIM5 prerequisite for CCP5. |

---

## Going deeper — the papers

If you want the full argument behind these tests:

| Paper | What it argues | Read for... |
|-------|---------------|-------------|
| **Paper 1: The Minimum Cost Curve** ([`org-members/research/01-minimum-cost-curve.md`](https://github.com/sharealedger-org/org-members/blob/main/research/01-minimum-cost-curve.md)) | Most enterprises are maintaining far too many pre-computed balance tables. The cost of those tables (storage, updates, reconciliation) now exceeds the cost of recomputing from raw events. There is a mathematically identifiable optimal point, and virtually every major enterprise has drifted past it. | *Why the architecture problem exists and how to measure it.* |
| **Paper 2: Pre-Hierarchy Event Repositories** ([`org-members/research/02-pre-hierarchy-event-repositories.md`](https://github.com/sharealedger-org/org-members/blob/main/research/02-pre-hierarchy-event-repositories.md)) | The same information that balance-posting discards is exactly what AI models need. A model trained on raw events before posting is provably capable of learning things that no model trained on posted balances can learn — regardless of algorithm quality. Ledger Lab is the reproducible test of how large that gap is. | *Why the AI consequence follows from the architecture problem, and what the experiment is designed to show.* |

The VA corpus tests (this document) are the **Track B** open-data implementation of the experimental design in Paper 2 §5. Track A (the P&C insurer's 25.5-billion-row event repository) is the primary corpus; Track B is the publicly verifiable version. A result that holds on both tracks is materially stronger than either alone.

---

## Technical details and open decisions

Full test specifications, data paths, acceptance thresholds, and the Test 0 feasibility findings (what cross-file joins are and aren't possible in the public corpus) are in:

- **[`docs/TEST_DESIGN.md`](./docs/TEST_DESIGN.md)** — unified CCP cost-point and AIM model evidence test design (authoritative)
- **[`docs/RESEARCH_OBJECTIVES.md`](./docs/RESEARCH_OBJECTIVES.md)** — master map of all experiments across both papers, run order, and open decisions
- **[`ckb-analysis.md`](./ckb-analysis.md)** — code-to-paper mapping for the GenevaERS pipeline implementation

---

*This document is part of the Ledger Lab repository. Last updated: Session 70, 2026-11-04.*
