# Ledger Lab — What We're Trying to Show

**Start here if you haven't read the papers.**
This document explains what Ledger Lab is testing and why, in plain English. No academic background required. The papers are referenced at the bottom for anyone who wants the full argument.

---

## The one-paragraph version

Every major financial institution has spent decades summarizing its raw business records into pre-computed totals — balances, subledgers, General Ledgers. Those summaries are useful for accounting, but they permanently throw away information: *who* ordered what, *when* orders were placed, *what sequence* of events preceded an outcome, *which vendor relationships* lasted and which didn't. Ledger Lab tests the cost and AI consequences of that throw-away decision. Paper 1 measures what it costs — it turns out most institutions are paying far more than they need to. Paper 2 tests what an AI model can and cannot learn once that information is gone.

---

## The two questions Ledger Lab answers

> **Paper 1:** What is the minimum cost to maintain a financial ledger that can answer any question you might want to ask? Most institutions are far past that minimum.

> **Paper 2:** No model — trained on any substrate — can answer Net Income from raw events alone. Net Income is not a fact about the world; it is a classification decision an organisation made through its chart of accounts. AI completes accounting. It does not replace it.

---

## The two substrates: what survives posting, and what doesn't

When Virginia processes a purchase order and issues a payment, the data passes through a posting step. That step is one-way:

| Data | What it contains | What posting discards |
|------|-----------------|----------------------|
| **Posted data** (`GL20YY.csv`) | Payments summarized by account code | Order dates, commodity types, vendor identity, revision history, vendor lifecycle |
| **Instrument Ledger** (`LDGR20YY.csv`) | Vendor-keyed movements — vendor identity preserved | Pre-accumulation sequence only |

**Ledger Lab uses public Virginia procurement data because both sides of this discarding are available** — raw purchase orders and posted expenditure records. This makes the destruction measurable.

---

## Paper 1 — The cost experiment (CCP1 through CCP5)

Ledger Lab runs the Virginia corpus through five capability levels (CCP1–CCP5) and measures compute time, storage, and peak memory at each level.

- **CCP1** is the GL baseline — the most collapsed instance of the fixed-posting-key class (GL, sub-ledger, data warehouse, data mart all sit in the same structural position). It measures the cost of what institutions run today.
- **CCP2** is the Instrument Ledger — vendor identity preserved, one movement row per vendor-period. This is the bowl minimum.
- **CCP3–CCP5** add reporting views, revaluation engines, and allocation — each one measured as a marginal cost on top of CCP2.

The finding: CCP1 → CCP2 is the architectural leap. CCP2 → CCP5 is a small additional cost. Adding the full enterprise capability stack costs almost nothing once you're at the bowl minimum.

---

## Paper 2 — The AI experiment (three models + AIM illustrations)

Paper 2 has two parts.

### Part 1 — What a posting system structurally cannot know (AIM illustrations)

Three tests run against both the CCP1 posted output and the CCP2/CCP3 instrument ledger. The same model, same algorithm — different substrate. The gap illustrates what posting permanently destroys:

| Test | Question | Why posting can't answer it |
|------|----------|-----------------------------|
| **AIM1** — Non-injectivity | How many distinct order patterns collapsed to the same posted key? | Posting discards vendor identity; the collapse is unmeasurable from the GL |
| **AIM3** — Vendor survival | Can we predict which vendors will stop doing business with the state? | Posted records have no concept of vendor entry or exit — only transactions |
| **AIM4** — Ordering seasonality | Does *when* orders are placed carry fiscal-year-end budget signal? | Posted payment dates carry none of the ordering-date timing |

These are illustrations of the information-destruction claim. The structural proof is Paper 1's permutation table — the AIM tests make it concrete.

### Part 2 — The three-model experiment (primary Paper 2 evidence)

The dominant institution practice today: take a pretrained LLM — trained on the internet, which contains language — and fine-tune it on your financial data. The problem is that the language prior survives fine-tuning. The model learns spurious associations between words that happen to appear in customer names and geographic identifiers. More data does not fix it. Better algorithms do not fix it. It is a contamination problem, not a scale problem.

Ledger Lab tests this with three models on the same corpus:

| # | Model | What it trains on | Language prior? |
|---|-------|--------------------|----------------|
| 1 | **Posted-FineTune** | Pretrained LLM fine-tuned on posted data (`GL20YY.csv`) | Yes — survives fine-tuning |
| 2 | **IL-FineTune** | Pretrained LLM fine-tuned on Instrument Ledger (`LDGR20YY.csv`) | Yes — survives fine-tuning |
| 3 | **IL-Native** | Structured model built directly from the Instrument Ledger — no pretrained LLM | None |

- **Gap 1→2** = the substrate effect: better data, same contaminated prior
- **Gap 2→3** = the language prior effect: same data, prior removed
- **Gap 1→3** = total cost of today's institution pattern

**IL-Native closes the experiment with Net Income.** It can answer it — because the Instrument Ledger is the posting layer, and Net Income is a chart-of-accounts classification available there. Posted-FineTune cannot answer Net Income from raw events alone — that classification is not in the events, it is in the posting rules.

---

## What counts as overall success

**Paper 1:** The delta CCP1→CCP2 is measurable, non-trivial, and stable across three replicates. CCP5 ≈ CCP4 (single-pass universality holds).

**Paper 2 Part 1:** AIM1, AIM3, and AIM4 each show a non-zero gap between the CCP1 substrate result and the CCP2/CCP3 result, using the same algorithm on both.

**Paper 2 Part 2:** IL-Native outperforms Posted-FineTune on vendor survival and expenditure forecasting. The Net Income closing argument holds logically.

---

## Suggested run order

| Step | Run | Why |
|------|-----|-----|
| 0 | **AIM5** (agency name classifier) | No corpus run needed. Produces agency reconciliation table that unblocks AIM2 and CCP5. Cheapest possible first result. |
| 1 | **CCP1** (GL baseline + permutation table) | Establishes status-quo cost and produces posted substrates for AIM1/3/4 comparison. |
| 2 | **AIM1 against CCP1** | Run non-injectivity count against GL output and permutation table — both expected to show high collapse. |
| 3 | **CCP2** (Instrument Ledger) | Left anchor of the minimum cost curve. |
| 4 | **AIM1 against CCP2** | Non-injectivity now measurable with full fidelity. Compare gap to CCP1 result. |
| 5 | **CCP3** (IL + Step 5v views) | Step 5v views are the AIM3/AIM4 feature tables. Delta CCP2→CCP3 = cost of producing all illustration substrate. |
| 6 | **AIM4** (ordering seasonality) | Cheapest AIM on CCP3 views — no join required. |
| 7 | **AIM3** (vendor survival) | PO-only survival model against CCP3 view. |
| 8 | **AIM2** (commodity forecasting) | Secondary. Requires AIM5 reconciliation table. |
| 9 | **CCP4, CCP5** | Revaluation + allocation cost points. AIM5 prerequisite for CCP5. |
| 10 | **Three-model experiment** | Primary Paper 2 Part 2 evidence. Posted-FineTune / IL-FineTune / IL-Native on downstream tasks. Owner approval required before writing Python training scripts. |

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

*This document is part of the Ledger Lab repository. Last updated: 2026-11-05 — three-model experiment replaces AIM-centric Paper 2 framing; model names Posted-FineTune / IL-FineTune / IL-Native introduced.*
