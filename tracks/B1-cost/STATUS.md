# Track B1: Paper 1 cost curve (Virginia public data)

Updated 2026-11-12. Hypothesis under test: keeping events in an instrument-keyed ledger (the CKB approach) sits at the minimum cost, and each added capability then costs little more, while a GL pays extra to rebuild what it lost. Cost points CCP1 to CCP5 each add one capability. Status words as in [`docs/CURRENT_RESULTS.md`](../../docs/CURRENT_RESULTS.md).

## State

| Item | Status | Evidence |
|---|---|---|
| Three full 14-year runs, FY2003 to FY2016 (baseline `20261110_210721`, runs 3 and 4 in the run-2 layout) | Measured. All exit 0, 42 of 42 controls PASS. | Tag `companion-measured-baseline-20261111`; [`docs/COMPANION_BASELINE_RECORD_2026-11-12.md`](../../docs/COMPANION_BASELINE_RECORD_2026-11-12.md) |
| Run 3 and run 4 give identical outputs | `verified` for 74 final data files and the merged-SJE hash list (14 of 14) | Register entries "Full corpus run 4" and "Merged SJE hash comparison" |
| CCP1 to CCP3 cost points | Numbers exist; comparison not made. Three replicates not run, so no cost is quotable. | `data/test_evidence/corpus_runs/` |
| CCP4 (revaluation), CCP5 (allocation) | Fixture-scale only; no corpus run | [`docs/EXPERIMENT_STATE_2026-11-11.md`](../../docs/EXPERIMENT_STATE_2026-11-11.md) section 2 |
| Mix graph | Model run only (measured corpus constants, assumed unit rates) | [`docs/COST_GRAPH_DESIGN.md`](../../docs/COST_GRAPH_DESIGN.md) |
| Engine graph | Needs a second substrate: Track D result | [`../D-control/STATUS.md`](../D-control/STATUS.md) |
| Process graph | Needs CCP4 and CCP5 on the corpus | |

Read the numbers by CPU seconds and total wall time, not per-step times; contention inflates per-step times. Revenue was not processed in any run.

## Next actions

0. ~~**Build the paper revision 2 evidence folder** for TDS~~ Done 2026-11-12 (`docs/tds-evidence/paper-1-rev2-ledger-lab/`: README index, `01-measured-results-summary.md`, `02-mix-cost-curve-model.md`, `03-charlie-anderson-model.xlsx`, `04-one-page-explainer.html`, `05-limits-and-phase2-roadmap.md`).
1. ~~**Refresh `CURRENT_RESULTS.md`**~~ Done 2026-11-12 with completed Run 4 evidence, standardized naming, and "measured, shows, does not show" summary.
2. Owner review and transfer of the evidence folder into TDS (`01-evidence/practitioner/Sharealedger/`).
3. Phase 2 execution: 3-replicate isolated cost runs and revenue transformation TTL.

## Decisions waiting (owner)

Which layout is the reference for replicates; whether to fuse steps `3`, `20`, `21` (changes what CCP1 measures); the one outside-reader name for the payments subledger; the web addresses for the explainer footer.

## Key files

[`docs/RESEARCH_OBJECTIVES.md`](../../docs/RESEARCH_OBJECTIVES.md), [`docs/TEST_DESIGN.md`](../../docs/TEST_DESIGN.md), [`docs/CKB_LAYER_EXECUTION_MODEL.md`](../../docs/CKB_LAYER_EXECUTION_MODEL.md), [`docs/ENGINE_IMPLEMENTATION_PLAN.md`](../../docs/ENGINE_IMPLEMENTATION_PLAN.md), [`docs/FULL_CORPUS_RUN_CHECKLIST.md`](../../docs/FULL_CORPUS_RUN_CHECKLIST.md), [`docs/MEASUREMENT_AND_LOGGING.md`](../../docs/MEASUREMENT_AND_LOGGING.md), [`docs/AGENTIC_ARCHITECTURE_GATES.md`](../../docs/AGENTIC_ARCHITECTURE_GATES.md). To-dos: `TODO.md`, sections "Explainer", "Cost graphs", "Engine and cost-curve items".
