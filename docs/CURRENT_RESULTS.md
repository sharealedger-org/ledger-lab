# Ledger Lab: Where Things Stand

**As of 2026-11-11, `main` at `9d54cd8`.** A final full-corpus run is still in progress on a second machine and is not counted here.
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

Two full 14-year runs exist.

| Run | Layout | Result |
|---|---|---|
| Baseline `20261110_210721` | Years posted one after another, `5v` last | Exit 0. 42 of 42 balance controls pass. |
| Run 3 `20261111_014229` | Two years posted at once, `5v` started early | Exit 0. 42 of 42 pass. |

What the two runs establish:

| Claim | Status | Detail |
|---|---|---|
| The pipeline runs the whole corpus on one 4-CPU, 16 GB laptop with a 1 GB JVM heap | `partially_verified` | Two runs, one machine. Highest process memory was about 1.35 GB. |
| Every year's SJE and ledger balance to zero | `verified` for these runs | The control totals are `awk` sums in Bash. AGENTS.md puts such controls in Scala; the owner decided they stay for now. |
| The two runs give the same answer | `partially_verified` | `LDGR*`, `PERM_SUMMARY*` and the four views are byte-identical. Merged SJE, `GL` and `PERM_TBL` were not compared. |
| Volumes | `partially_verified` | 385 raw files, 27.8 GB. 224,072,410 SJE rows. 10,651,908 ledger rows. 6,433,714 GL keys. Output on disk 116 GB. Computed after the run from archived tables ([`corpus_totals.csv`](../data/test_evidence/corpus_runs/run_20261111_014229/corpus_totals.csv)). |
| Permutation capacity summed over 14 years is 503,118,463,033,110 | `partially_verified` | An upper bound. Some varying fields are not tracked (`fd_untracked_dims` is non-empty in every year), so the true figure is lower by an unknown amount. |
| About 32,000 purchase order rows are quarantined | `verified` | Rejected for malformed fields, 14 files. Counts per year are in the register. |

Evidence: register entries "2026-11-11 Full corpus run FY2003-FY2016" and "Full corpus run 3". Tests: 81 Scala unit tests passed at the last full check.

## 4. Cost curves (Paper 1)

| Point | What exists | Status |
|---|---|---|
| CCP1 and CCP2 | A `cost_surface.csv` row per year from both full runs: seconds, CPU seconds, bytes, peak memory. | Numbers exist; the comparison has not been made. |
| CCP3 | Step `5v` ran over all years: 1,285 s (baseline), 1,551 s (run 3). | Same. |
| CCP4, CCP5 | Revaluation and allocation pass fixture-scale tests only. | No corpus run is recorded. |
| Three replicates of each point | The design requires them before any cost is published. | `not run`. The two full runs differ in layout, so they are not replicates. |

What can and cannot be said:

- Run 3 took 8,147 s of wall time against 11,695 s for the baseline (-30%), and used 4.8% more CPU (11,179 s against 10,670 s). More work per step, less total time. **Compare CPU seconds and total wall time, not per-step times**, because parallel lanes inflate per-step times.
- `total_compute_s` and `total_cost_proxy` in `cost_surface.csv` are not comparable between these two runs. They sum elapsed step time, which contention inflates.
- In run 3, 54% of the chain wall time came after the pipeline logged "Pipeline complete". The code that runs there is mostly the Bash control-total pass. That split is inferred from the code, not timed, and none of it appears in a step metric. Timing it is the next job once the second machine is idle.
- The statement that CCP1 to CCP2 is the large step and CCP2 to CCP5 is small is the hypothesis. It is **not** a result. Register row "Cost curves": `not_verified`.

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
| Track C (Berka bank data) | Tried the same idea on a public Czech bank data set. | Illustrative only, left out of Paper 2 by owner decision. Last protocol: 2 of 3 locked tests passed, overall FAIL. Its code is not on `main`. |
| Track D | A deliberately conventional relational engine (Python and SQLite) in a companion repository, to give a comparison cost curve. See [`TRACK_D_NOTES.md`](TRACK_D_NOTES.md). | Passes its small fixture. Not run on the corpus. Its cost figures in the notes are projections, not measurements. |

## 8. Earlier statements that are wrong

You may meet these in older documents or in the handoff log.

| Statement | Status |
|---|---|
| "The permutation row count is the cost explosion measure." | `contradicted`. That count cannot exceed the ledger row count. The measure is now permutation capacity (section 3). |
| "AIM3 failed." | `contradicted` as a statement about the hypothesis (section 5). |
| "Chain wall time is engine time." | `contradicted`. A large share is after the engine finishes (section 4). |
| The 2026-11-06 CCP1 to CCP3 cost points. | Superseded. They used a purchase order reader that read only half the records (25,129 against 52,430 for FY2003). Do not compare against them. |
| "Debit only" filters in steps 20 and 21. | `contradicted`. Both lines of every pair were flagged as debits, so the filter removed nothing. Fixed. |

## 9. Limits of the data

- Purchase order files for FY2001 to FY2005 are stubs or partial, FY2009 is absent and FY2018 is partial. Any test that needs purchase orders is limited to FY2006 to FY2008 and FY2010 to FY2017. A vendor missing in a gap year is a data gap, not churn.
- Completeness against the State's own books has not been established. Every file family is recorded as `PRESENT_COMPLETENESS_UNKNOWN` ([`CORPUS_FINDINGS.md`](CORPUS_FINDINGS.md)).
- The standard reference copy of the run outputs (104 small final files, 2.5 GB, with a SHA-256 manifest in `data/test_evidence/corpus_runs/run_20261111_014229/gold/`) is on OneDrive. Its upload had not been confirmed complete when this page was written.

## 10. What is open

In rough order:

1. Finish and archive the run now in progress. Then time the post-pipeline control pass, and run one year alone on a quiet machine to separate contention from real cost.
2. Run each cost point three times, in a defined layout, before quoting any cost.
3. Re-run AIM1, AIM3 and AIM4 on the new corpus, using the locked protocols where the data allows.
4. Run the three-model experiment. An owner decision is needed on how Net Income is derived.
5. Decide whether the Bash control totals move into Scala.

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
