# Phase 2 fictional source generation and financial admission

2026-11-16. **Full v6 completed and ready for owner validation.**
See the [full receipt](full_v6/README.md) and [capacity assessment](full_v6/CAPACITY.md).
2,444,027 financial sources -> 4,888,054 SJEs -> 6,988,627 financial reporting
rows; all declared source/financial/report checks pass. The read-only audit verifies
all 49,091 reused classifications and actual multi-year/context supplier recurrence.
About 73 GiB is free; a comparable next pass fits the reserved-space budget.
Earlier checkpoints below are retained implementation history, not current status.
Owner authorized execution through complete fictional generation, one financial
pass, evaluation and justified versioned iteration. Paper 3 receives methodology;
no Q2 model evaluation, private manuscript edit, old reporting repeat or deletion.

Latest checkpoint: variable calibrated 100-component/two-year run
`session-run.tmp/phase2_variable_profile_v4` completed actual source, financial and
expanded-report controls (3,451 financial sources, 6,902 SJE rows). Its source
assessment records lifecycle/inferability classes and bounded treasury schedules.
`source_freeze_tests.txt` records eleven focused tests passing after source-schema
refinement; `combined_source_acceptance_tests.txt` records four combined tests,
including conservative supplier eligibility and rejection of missing schedules.
The separately versioned Phase 1 identity upgrade has its own evidence directory.
Read-only zero-vendor selection is finishing; the combined and full corpus runs
are not yet accepted. Earlier prototype counts/statuses below are historical.

Contracts: docs/PHASE_TWO_EXECUTION_PLAN.md and PHASE_TWO_GENERATOR_CONTRACT_V1.md.
All new generation/inspection is Scala; Bash orchestrates. Existing financial
adapters/ARE/post/reporting remain unchanged. New source families are not silently
posted. Hash manifests and source snapshots distinguish dirty working code from
base revision `38a41c6`.

Baseline: unchanged component generation/financial/report outputs completed under
session-run.tmp/phase2_unchanged_baseline_v1. The first final checker invocation
encountered a compile error in unrelated in-progress new code (LocalDate Ordering).
After fixing it, checker resumed against the same existing outputs and passed:
36 generated events, 30 financial sources / 60 SJE legs, 35 cutoff-visible events;
86 financial reporting input rows, reconciliation/history/conservation controls.
`baseline_check_resumed.txt` preserves that receipt. Original FAILED marker/log
remain; do not call the original driver an uninterrupted successful execution.

Focused tests: 42 passed across five suites: PhaseTwoCorpusSpec (5),
ComponentPilotSpec (4), StandardizeAndSortSpec (2), StandardizeAndSortPOSpec (16),
ReportingPassSpec (15). Includes deterministic multi-level sorting, calendar leap
and fiscal boundaries, cutoff/private isolation, source hash corruption and actual
large legitimate/unattributed reporting groups. Test run ended 07:34:19 AZT.

First integration fixture at session-run.tmp/phase2_fixture_v1 generated and posted
224 financial sources / 448 SJE rows across FY2031–32. Its new financial checker
failed because it incorrectly assumed D/C sides; existing canonical output is
D/O. Corrected the checker without changing financial processing. Preserve failure.
Review also replaced matching sequence-number process aliases, rebuilt translated
history hashes, added independent background activity and supplied exact evidence
schema/uniqueness consumers and packaged source/calibration provenance.

Reviewed 100-component/two-year execution is active at
session-run.tmp/phase2_profile_v2. Larger source admission and full 10,000-component,
14-year execution remain pending this run. Initial disk observation 109 GiB free;
40-GiB reserve gate before stages. No remote archival or local deletion performed.
All timing is exploratory; concurrent development/builds and a clock/log gap mean
these runs are not clean timing replicates or cost points.
