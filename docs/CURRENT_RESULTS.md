# Ledger Lab: Where Things Stand

**As of 2026-11-12, `main` at `ef78576` (Baseline Tag: `companion-measured-baseline-20261111` on `c3667e4`).**
**Audience:** someone new to the repository, probably an IT professional who knows GenevaERS concepts (sorted files, keyed merges, reference data, control totals) but is not an AI specialist. The papers are read alongside this page.

This page is a map of what has been run, what it showed and how far to trust it. It does not replace the evidence. Each line points to the entry in [`VERIFICATION_AUDIT_REGISTER.md`](VERIFICATION_AUDIT_REGISTER.md) that holds the proof. If this page and the register disagree, the register wins; please fix this page.

## How to read the status words

The register uses these words, and so does this page.

| Word | Meaning |
|---|---|
| `verified` | A named check was run and passed. The check is in the register. |
| `partially_verified` | The check passed, but on less than the claim covers (one run, two years, final files only). |
| `inferred` | Reasoned from the code or the numbers, not tested. |
| `contradicted` | Something earlier believed was shown to be wrong. Kept so nobody repeats it. |
| `not run` | Planned or coded, but no result exists. |

## 1. What the project is trying to show

A financial system posts business events to a General Ledger (GL) and keeps the totals. Posting is one-way: the vendor, the order date and the event sequence are gone from the GL. Ledger Lab uses 14 years of public Virginia purchase order and expenditure data (FY2003 to FY2016) to measure two consequences.

- **Paper 1, cost.** The hypothesis is that keeping the events in an instrument-keyed ledger (the CKB approach, as in GenevaERS) sits at the minimum cost for any ledger, and that each added capability then costs little more, while a GL pays extra to rebuild the answers it lost. The measure is a set of cost points, CCP1 to CCP5, each adding one capability.
- **Paper 2, what a model can learn.** A model trained on posted data cannot recover what posting threw away. The primary test compares three models on the same tasks (section 6). A set of smaller tests, AIM1 to AIM5, illustrates the loss.

These are hypotheses under test. Nothing on this page should be quoted as a paper result unless it says `verified` and you have read its caveats.

[`LEDGER_LAB_OVERVIEW.md`](../LEDGER_LAB_OVERVIEW.md) explains the same thing in more detail. One warning: its "The finding" paragraph under Paper 1 states the expected outcome, not a measured one (see section 4).

## 2. Terms you will meet

| Term | Meaning here |
|---|---|
| SJE | Standardised journal entry. A balanced pair of lines in a fixed 39-field layout. Every source record becomes SJEs. |
| ARE | The rule engine that turns a source record into SJEs. |
| CKB | Common key buffer. The GenevaERS idea of sorted inputs merged by key, so one pass serves many outputs. |
| LDGR (Instrument Ledger) | The ledger keyed by instrument (here, the vendor). It keeps what the GL discards. |
| GL | The conventional posted ledger: agency, fund, object, account, period. No vendor. |
| Permutation capacity | For each GL key, how many distinct combinations of the other fields exist. A measure of how many balances a GL would need to hold to answer the questions the LDGR answers. Reported as an upper bound. |
| CCP1 to CCP5 | Cost Curve Points. CCP1 is the GL baseline. CCP2 is the LDGR. CCP3 adds summary views. CCP4 adds revaluation. CCP5 adds allocation. |
| AIM0 to AIM5 | Small model tests that show what the GL lost. |
| Fine-tuning | Taking a large pretrained language model and training it further on your own records. |
| C-statistic | Probability that the model ranks a random "survivor" above a random "churner". 0.5 is a coin flip, 1.0 is perfect. |
| Permutation test | Shuffle the answers many times. The p-value is how often a shuffle scores as well as the real model. |

## 3. The pipeline: what has been run

The Scala pipeline reads the raw files, produces SJEs, posts the LDGR and the GL, counts permutation capacity and builds four summary views (step `5v`). Bash orchestrates it. See [`README.md`](../README.md) and [`CKB_LAYER_EXECUTION_MODEL.md`](CKB_LAYER_EXECUTION_MODEL.md).

Three full 14-year runs exist (FY2003 to FY2016). Per the 2026-11-12 naming standard, these represent three measurements at the **Monthly** point:

| Run Tag / Layout | Description | Wall Clock | Step CPU | Result |
|---|---|---|---|---|
| `20261110_210721` (Baseline) | Serial execution (years in series, `5v` last) | 11,695 s | 10,670 s | Exit 0. 42 of 42 balance controls pass. |
| `20261111_014229` (Run 3) | 2-chain parallel, early `5v` | 8,147 s | 11,179 s | Exit 0. 42 of 42 pass. |
| `20261111_064052` (Run 4) | 2-chain parallel, `caffeinate` active | 8,544 s | 11,207 s | Exit 0. 42 of 42 pass. |

What the three runs establish:

| Claim | Status | Detail |
|---|---|---|
| The pipeline runs the whole corpus on one 4-CPU, 16 GB host with a 1 GB JVM heap | `verified` | Three runs. Highest process memory was ~1.36 GB. |
| Every year's SJE and ledger balance to zero | `verified` | 42 of 42 balance controls pass in all three runs. |
| Runs 3 and 4 give identical outputs | `verified` | All 74 output data files in `MANIFEST.csv` and 14/14 merged SJE SHA-256 hashes are byte-identical. |
| Volumes | `verified` | 385 raw files, 27.8 GB. 224,072,410 SJE rows. 10,651,908 ledger rows. 6,433,714 GL keys. Output on disk 110–116 GB ([`corpus_totals.csv`](../data/test_evidence/corpus_runs/run_20261111_014229/corpus_totals.csv)). |
| Permutation capacity summed over 14 years is 503,118,463,033,110 | `verified` | Measured upper bound across 14 years. |
| Malformed purchase order rows quarantined (~32k) | `verified` | 14 files, rejected for malformed fields with exact counts in the register. |
| Gold reference copy archived | `verified` | 104 data files uploaded and verified against `MANIFEST.csv`. |

Evidence: register entries "2026-11-11 Full corpus run FY2003-FY2016", "Full corpus run 3", and "Full corpus run 4". Tests: 81 Scala unit tests pass.

## 4. Cost curves (Paper 1): Measured, Shows, Does Not Show

Standard naming convention (2026-11-12):
- **Graph Points (Mix Dial):** Events (P1), Annual (P2), Monthly (P3, bowl minimum), Cubes (P4), Posted (P5), Permuted (P6).
- **Process Points (Layers):** Posted GL (CCP1), Ledger (CCP2), Views (CCP3), Revaluation (CCP4), Allocation (CCP5).

### Measured, Shows, Does Not Show Summary

| Point / Layer | Measured Evidence | What it Shows | What it Does NOT Show |
|---|---|---|---|
| **P1: Events** (Left Wall) | 224,072,410 SJE rows (56.69 GB raw events). | Establishing pure event replay requires full historical scan per analytical query. | Does not show empirical query latency without an external replay benchmark. |
| **P2: Annual** (`T-annual`) | Sizing model assumption (1/3 of monthly rows). | Analytical queries at sub-annual grain must fall back to raw transaction scans. | Not measured at corpus scale (no 14-year annual run exists). |
| **P3: Monthly** / **CCP2: Ledger** (Bowl Minimum) | 3 full corpus runs: 10,651,908 rows (1.715 GB), 11,207 s CPU over 14 years. | CKB produces monthly movement ledger in a single streaming pass within 1 GB heap. Zero query fallback tax for monthly/quarterly/annual queries. | Does not quote multi-node distributed scaling limits. |
| **P4: Cubes** | Model costing (2 parallel summary balance stores). | Proliferation of fixed rollup structures adds multi-master write and reconciliation overhead. | Not instantiated as a physical corpus database. |
| **P5: Posted** / **CCP1: Posted GL** | 6,433,714 keys across 14 years. | Discards instrument identity, order date, and event sequence. | Does not quote isolated GL-only compute time without CKB baseline. |
| **P6: Permuted** | Combinatorial capacity sum: `503,118,463,033,110`. | Structural sizing bound for multi-master balance tables required to answer LDGR queries in GL space. | Not an architecture anyone physically instantiates. |
| **CCP3: Views** | Step `5v` (4 summary views over 14 years, 1,285 s to 1,696 s). | Bounded aggregations produced in the same CKB pass add minimal batch cost. | Formal 3-replicate isolated cost comparison pending. |
| **CCP4: Revaluation** | Fixture-scale tests pass. | SJEs generate balancing entries without new balance tables. | Not run at corpus scale. |
| **CCP5: Allocation** | Fixture-scale tests pass. | Prior-day divisor allocation preserves single-pass universality. | Not run at corpus scale. |

### Timing & Contention Findings
- **Contention in Parallel Chains:** The 2-chain layout reduced wall time by ~30% (8,147 s vs 11,695 s) but increased chain CPU by +15% to +17% (steps 3, 20, 21: 4,593 s vs 3,913 s). Controlled quiet-machine testing confirmed this is CPU contention between lanes on a 4-CPU host, not engine inefficiency.
- **Unmetered Chain Tail:** About 54% of chain wall time occurs after "Pipeline complete" during single-threaded `awk` control-total balancing in Bash.
- **Cost Metrics:** Summed step times (`total_compute_s`, `total_cost_proxy`) are inflated by contention; compare total CPU seconds and wall clock instead.

## 5. Model tests (Paper 2, part 1: AIM)

These ran as Python scripts on the summary views. **All are provisional.** The input views were built before the final re-run of the chain, and the AIM re-run on the new corpus is pending.

| Test | Question | Result so far | Status and caveats |
|---|---|---|---|
| AIM0 | Can purchase orders be linked to payments? | Only at the (agency, year) level. | Closed 2026-11-01. |
| AIM1 | How many different order patterns collapse to one posted key? | FY2012 only: 17,163 ledger keys hold more than one pattern; 12,965 permutation keys hold more than one vendor. | `partially_verified`. No threshold; any gap is evidence. One year, first pass, run before the GL key and permutation steps were redefined. |
| AIM3 | Can we predict which vendors stop trading with the state? | Hold-out FY2014 to FY2015: C = 0.7527, permutation p = 0.001. Locked thresholds are C above 0.65 and p below 0.05, so PASS. | `partially_verified`. See below. |
| AIM4 | Does order timing show a year-end budget pattern? | FAIL: 2 of the top 5 commodity classes show it; the locked rule needs 3. | The script uses a simple Q4 test, not the locked STL method. A FAIL here is not a verdict on the hypothesis. |
| AIM5 | Can a model tell a misspelled agency name from a different agency? | Precision 1.0 on 312 held-out matches; 12 of 13 unmatched names flagged ambiguous. | `verified` for the stated test. It is data-quality work, not a headline result. |
| AIM2 | Does knowing what an agency ordered improve expenditure forecasts? | `not run`. | Subsumed by the three-model forecasting task. |

AIM3 caveats, because it is the strongest result:

- Four training years (FY2010 to FY2013), not the locked seven-window protocol. Purchase order data is usable only for FY2006 to FY2008 and FY2010 onward.
- The vendor churn rate rises from 24% to 33% over FY2012 to FY2015, so part of the signal could be a time trend rather than vendor-specific information. The test does not separate the two.
- Four simple features and a linear model.
- An earlier AIM3 FAIL (C = 0.6125) is `contradicted`: the old script counted every FY2016 vendor as churned because FY2017 does not exist. Do not cite it.

## 6. The three-model experiment (Paper 2, part 2): not yet run

This is the primary Paper 2 evidence. Three models are given the same tasks:

1. **Posted-FineTune:** a pretrained language model fine-tuned on the GL.
2. **IL-FineTune:** the same model fine-tuned on the LDGR.
3. **IL-Native:** an ordinary structured model built on the LDGR, with no language model.

The gap between 1 and 2 shows what posting lost. The gap between 2 and 3 shows what the language model's prior knowledge costs. Net Income closes the argument: it is a chart-of-accounts classification, not a fact in the events, so no model can derive it from events alone.

**Status: `not run`.** No model has been trained. Python scripts for preparing the data, splitting it, scoring it and writing the results file exist on branch `aim-three-model` and pass their tests on small fixtures. The plan, the decisions D1 to D8 and the open items are in `docs/THREE_MODEL_EXPERIMENT_PLAN.md` on that branch. The work waits for the final output files to finish syncing.

## 7. Other tracks

| Track | What it is | Status |
|---|---|---|
| Track C (Berka bank data) | Tried the same idea on a public Czech bank data set. | Illustrative only, left out of Paper 2 by owner decision. Stalled. |
| Track D (Control Repository) | Codex unconstrained control implementation (`sharealedger-org/ledger-lab-control`). | Official attempt completed and PASS by Codex report (`3c50c19`). Paused for Paper Revision 2. |

## 8. Earlier statements that are wrong

You may meet these in older documents or in the handoff log.

| Statement | Status |
|---|---|
| "The permutation row count is the cost explosion measure." | `contradicted`. That count cannot exceed the ledger row count. The measure is now permutation capacity (section 3). |
| "AIM3 failed." | `contradicted` as a statement about the hypothesis (section 5). |
| "Chain wall time is engine time." | `contradicted`. A large share is after the engine finishes (section 4). |
| The 2026-11-06 CCP1 to CCP3 cost points. | Superseded. They used a purchase order reader that read only half the records (25,129 against 52,430 for FY2003). Do not compare against them. |
| "Debit only" filters in steps 20 and 21. | `contradicted`. Both lines of every pair were flagged as debits, so the filter removed nothing. Fixed. |

## 9. Limits of the data and experiment

- **No Revenue in Gold:** Gold datasets currently contain only `EXP*`, `COMMIT`, and offset accounts (`0000`). While raw files contain 17.75M revenue rows (`apa_revenue`), revenue has not yet been processed through the Scala TTL.
- **No Corpus Replicates per Cost Point:** Although three full Monthly runs exist, formal three-replicate cost runs under a single fixed layout have not been completed for CCP1 through CCP5.
- **Unmeasured Points:** Annual (P2) and Cubes (P4) are model approximations, not physical 14-year corpus measurements.
- **Unmetered Tail:** Single-threaded `awk` control calculations in Bash account for ~54% of chain wall time.
- **Hardware Specification:** The companion baseline was measured on a single 4-CPU, 16 GB laptop host.
- **Purchase Order Gaps:** FY2001–FY2005 are stubs/partial, FY2009 is absent, and FY2018 is partial. PO-dependent tests are restricted to FY2006–FY2008 and FY2010–FY2017.

## 10. What is open (Next Phase)

In rough order:

1. **Paper Revision 2 Evidence Package:** Deliver TDS evidence folder with Mix curve model, Charlie Anderson spreadsheet, one-page explainer, and Phase 2 roadmap.
2. **Replicates & Points:** Run three replicates for CCP1 to CCP5 under a chosen reference layout.
3. **Three-Model Experiment (Track B2):** Resolve decision D9 (Net Income ground truth), implement IL-Native, select Apache 2.0 base model, and execute fine-tuning.
4. **Revenue TTL:** Process `apa_revenue` through Scala transformation to add `REV*` accounts to Gold.
5. **Track D Informed Phase:** Resume Track D comparison after Paper Revision 2.

## 11. Reading order

1. [`LEDGER_LAB_OVERVIEW.md`](../LEDGER_LAB_OVERVIEW.md): the idea in plain English.
2. This page.
3. [`RESEARCH_OBJECTIVES.md`](RESEARCH_OBJECTIVES.md): every test, its threshold and the run order.
4. [`TEST_DESIGN.md`](TEST_DESIGN.md): the specifications.
5. [`CKB_LAYER_EXECUTION_MODEL.md`](CKB_LAYER_EXECUTION_MODEL.md) and [`AGENTIC_ARCHITECTURE_GATES.md`](AGENTIC_ARCHITECTURE_GATES.md): how the engine is laid out and what a change must show.
6. [`VERIFICATION_AUDIT_REGISTER.md`](VERIFICATION_AUDIT_REGISTER.md): the evidence, newest entries at the bottom.
7. [`SESSION_HANDOFF.md`](../SESSION_HANDOFF.md): the working log. Long. Read the top entry only.
8. [`AGENTS.md`](../AGENTS.md): the rules every contributor and tool follows. Scala is the engine, Bash only orchestrates, and Python is for analysis after the engine.

## 12. Keeping this page honest

Update it after every full run and after any change to a status in the register. Give the date and the commit at the top. Do not move a line to `verified` here unless the register already says so.
