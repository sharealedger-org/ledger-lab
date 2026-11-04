# Research Strategy — Ledger Lab

**Status:** Working strategy document. Updated 2026-11-04 from design session.
**Audience:** Project owner, coauthors, and AI agents working in this repository.

This document captures the intellectual architecture of the research program — what each paper
proves, why the full corpus is the measurement target from paper 1 onward, how the AIM test
family is designed to interact, and what the drill-down thesis ultimately demonstrates.

It is not an implementation spec. For implementation details see `paper2-tests-plan.md`,
`TEST_DESIGN_PAPER_2.md`, and the architecture documents. For tactical session state see
`SESSION_HANDOFF.md`.

---

## The Proof Sequence

The paper series is a **proof sequence**, not a collection of independent papers. Each paper
proves one financial process applied correctly at instrument grain. Each result depends on and
extends the prior one. A reader cannot accept paper 4 without first accepting papers 1 and 2.

| Paper | What it proves | Financial process | Corpus requirement |
|-------|---------------|-------------------|--------------------|
| 1 | Preserving instrument identity through posting costs less than the permutation explosion it avoids | GL post vs. instrument ledger (no financial processes) | Full corpus — toy ratios are not publishable |
| 2 | Pre-hierarchy event repositories enable AI that posted records structurally cannot | Raw commitment + payment events, instrument-keyed | Full corpus — survival/seasonality models need 14 years × 65K vendors |
| 3 | Cost allocations applied at instrument grain preserve analytical traceability; GL-grain allocation destroys it | Allocation (step-up, cost distribution) | Full corpus + allocation rules |
| 4 | Multicurrency revaluation at instrument grain enables currency-adjusted AI; GL-grain FX destroys instrument identity | FX revaluation | Full corpus + rate history |
| 5 | Intercompany eliminations at instrument grain preserve both sides of the transaction; GL eliminations do not | Intra- and inter-unit elimination | Full corpus + entity structure |
| 6+ | Reclassification, consolidation, reserves, FTP — same instrument-grain vs. GL-grain argument repeated for each process | Each process in turn | Full corpus + process-specific rules |

**The architecture does not change between papers.** The instrument ledger is the same substrate
in every paper. Each new paper adds one financial process and shows it works at instrument grain
without breaking anything the prior papers proved.

---

## The Full-Corpus Commitment

**Fixture scale is for development and CI only. Every published result comes from the full corpus.**

This is not a convenience — it is a methodological requirement:

- **Paper 1:** The permutation explosion ratio (permutation rows ÷ GL balance rows) is only
  compelling at real scale. 65,000 vendors across 14 fiscal years produces a ratio that makes
  the argument. A 5-vendor fixture produces a toy number that no reviewer will cite.
  Cost-curve elapsed times (sort, CKB pass, materialization) are meaningless at fixture scale.

- **Paper 2:** The vendor survival model requires enough vendors and enough years to produce
  statistically credible C-statistics. 27,736 single-year vendors and 7,553 vendors with 8+
  years of history is the dataset. The 7-window longitudinal validation requires at least 8
  usable fiscal years. None of that exists at fixture scale.

- **All subsequent papers:** Any cost-curve delta (CCP_N vs CCP_{N+1}) must be measured on
  real data. A delta measured on a fixture is not a measurement — it is a demonstration that
  the code runs.

The VA corpus on this Windows laptop is the measurement platform. That matters: results
produced on a laptop make the cost argument *conservative*. If the architecture is efficient
enough to demonstrate the cost curves on a Windows laptop with a spinning-disk I/O profile,
the production case on a GenevaERS mainframe is even stronger. Do not apologize for the
hardware — it is a feature of the experimental design.

---

## The GenevaERS Simulation Rule

In this codebase, **Scala = GenevaERS**. Every Scala process simulates what a GenevaERS
job would do at production scale. The cost curves are measuring what GenevaERS would cost.
The AIM tests are measuring what GenevaERS would enable.

This has two consequences:

1. **Architectural correctness matters more than development convenience.** A Scala hash-map
   that works at fixture scale but does not simulate GenevaERS sorted-merge join behavior is
   not a correct simulation. It must be recorded as a gap (see `GENEVAERS_CAPABILITY_GAP_REGISTER.md`)
   and replaced before full-corpus cost measurements are valid.

2. **Python is the last resort.** Push every computation that GenevaERS could do into Scala.
   Python touches only the bounded summary tables the Scala pass emits, and only for statistical
   fitting that has no GenevaERS analog (survival model fitting, STL decomposition, classifier
   training). Owner approval is required before writing any Python (see `AGENTS.md`).

---

## The AIM Test Family

The AIM tests are not independent. They are a **family** where model outputs become features
for other models, and where the interaction pattern between models is itself a research finding.

### The interaction architecture

```
AIM3 — Vendor survival model
  Input:  View B (vendor-year lifecycle: entry, activity, exit)
  Output: vendor_health_score per vendor-year
              │
              ▼
AIM2 — Agency expenditure forecasting                AIM4 — Ordering seasonality
  Input:  View C (agency-year commodity mix)           Input:  View A (monthly order volume)
          View D (agency-year payment totals)          Output: budget_execution_risk_flag
          vendor_health_score from AIM3  ◄────────────────────────────────────────┘
          budget_execution_risk from AIM4
  Output: RMSE improvement L2→L3

AIM5 — Agency name classifier
  Input:  agency_alias_candidates.csv (328 names, 121M rows affected)
  Output: agency_reconciliation_table.csv
              │
              ▼
          feeds canonical agency_key into Views C and D
          (unlocks AIM2 cross-file joins)
```

**The interaction is the finding.** Each individual model demonstrates that pre-hierarchy
events carry signal unavailable in posted records. The interaction demonstrates that the
pre-hierarchy repository is a *compound analytical substrate* — not just a collection of
better inputs, but a platform where models reinforce each other in ways that a GL-based
architecture structurally cannot support.

### The three classes of AI pre-hierarchy enables

**Class 1 — Lifecycle models (AIM3)**
A vendor enters, operates, and exits. The commitment layer preserves that arc. A posted
balance has no concept of vendor lifecycle — it records dollar amounts to account codes.
Churn prediction, entry cohort analysis, and vendor health scoring are structurally impossible
from GL data — not just harder, but impossible, because posting permanently destroys the
temporal event sequence.

**Class 2 — Intent vs. execution models (AIM2, AIM4)**
What was *ordered* vs. what was *paid* are different signals. Budget-rush ordering in Q4,
commodity mix shifts year-over-year, revision chains on POs — these describe *planning
behavior*, not *payment behavior*. A GL shows only what cleared. Models that predict
execution from intent, detect anomalies between ordering and payment patterns, or flag
agencies whose planning diverges from execution are not buildable from posted records.

**Class 3 — Cross-model compound signals**
The vendor health score (AIM3 output) feeds the agency forecasting model (AIM2) as a
feature — agencies dependent on at-risk vendors should show higher expenditure uncertainty.
The budget-execution risk flag (AIM4 output) also feeds AIM2. No individual model produces
this. The compound signal emerges from the interaction. A GL-based system cannot produce
any of the three individual signals, so the compound signal is doubly impossible.

### Forward pointer to later papers

Paper 2 demonstrates the interaction pattern using raw commitment and payment data only.
The natural extension — explicitly not required for paper 2 — is:

> When allocation, FX revaluation, and elimination are applied at instrument grain (papers 3–5),
> the same AIM model family receives richer inputs without architectural change. The vendor
> health score now reflects post-allocation actuals, not just raw PO amounts. The agency
> forecasting model sees allocated overhead per agency-vendor pair. The interaction pattern
> scales with the financial process depth — because the instrument ledger preserves instrument
> identity through every process.

This sentence belongs in paper 2's conclusion. It points forward without requiring future
papers to be written first.

---

## The Drill-Down Thesis

The ultimate demonstration of the instrument ledger is:

**Start with a GL total. Follow the instrument ledger down through every financial process —
allocation, FX, elimination — to the individual vendor. At every step the data is complete,
the lineage is traceable, and the AI models work. No current architecture provides this path
without gaps.**

Today the GL world and the procurement world are completely disconnected:
- Finance runs allocations, FX, eliminations at account grain — instrument identity gone
- Procurement runs vendor analysis on raw PO data — never sees the financial processes applied on top

The gap between them is where analytical value disappears. An agency's "IT spend" in the GL
is a number that has been allocated across cost centers, revalued across currencies, and
partially eliminated across entities — none of which the procurement analyst can see or trace
back to the original vendor.

The instrument ledger closes that gap. Every financial process is applied at instrument grain.
The drill-down path from GL total → agency → cost center → vendor → individual PO line is
fully traversable. The AIM tests demonstrate that this traceability is not just an audit
feature — it enables AI that the disconnected architecture structurally cannot build.

**Papers 1+2 prove the foundation of this thesis** using the simplest possible corpus (raw
VA public procurement data, no financial processing complexity). The simplicity is a feature:
it isolates the architectural claim from process-specific complexity. Subsequent papers add
one process at a time and show the drill-down path extending without gaps.

---

## Scope Boundaries

### Paper 2 scope (current work)

**In scope:**
- Raw PO commitment events + raw payment events, instrument-keyed through the CKB pass
- Views A–D produced in one CKB pass over the full corpus
- AIM3 (vendor survival), AIM4 (ordering seasonality), AIM2 (agency forecasting), AIM5 (name classifier)
- Cross-model interaction: AIM3 output feeding AIM2, AIM4 output feeding AIM2
- Drill-down demonstration: agency GL total → vendor, using raw instrument ledger

**Out of scope for paper 2 (future papers):**
- Post-allocation vendor balances
- Multicurrency revaluation at instrument grain
- Intercompany eliminations
- Reclassification, consolidation, reserves, FTP

### View schema design principle

Design each view schema against three questions:
1. What does the paper 2 AIM model need as minimum?
2. What does the cross-model interaction (AIM feeding AIM) need as features?
3. What field would a papers 3–7 reader expect to see here as a natural extension point?

The CKB pass costs the same whether View B carries 6 fields or 12. Design the richest
defensible schema that answers all three questions, run the full corpus once, and the
view tables are permanent inputs to all current and future models.

---

## What No One Else Is Doing

The research program's claim is not "we found better procurement analytics." It is:

1. **Structural impossibility, not difficulty.** The signal is not missing from GL data because
   no one looked hard enough. It is missing because posting is a lossy compression function
   (Theorem 1, Paper 1). The information is gone. Better queries don't recover it.

2. **The instrument ledger is a compound analytical platform.** Individual models are useful.
   The interaction between models — where each model's output becomes another's input — is
   only possible because the substrate preserves the full event record. A GL-based system
   cannot support this interaction at any cost.

3. **The cost argument closes the loop.** Paper 1 proves it costs *less* to preserve instrument
   identity than to reconstruct it via permutation explosion. Papers 2+ prove what that
   preservation *enables*. Together they answer both the "can we afford it?" and the
   "why would we?" questions in the same experimental framework.

4. **GenevaERS is the production path.** The Scala prototype simulates GenevaERS behavior.
   The cost curves are measuring what GenevaERS would cost at production scale on a mainframe.
   The VA corpus on a Windows laptop is the conservative lower bound on what is achievable.
