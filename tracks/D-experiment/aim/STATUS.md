# Track D / AIM: AI and financial exploration

AIM is the identifier prefix for the AI/model evidence tests. C below denotes
the reported concordance/discrimination score; p denotes the statistical test
p-value. Gold refers to verified reference outputs used for comparison.

Updated 2026-11-15. Former Track B2 is a workstream inside the
[primary experiment](../STATUS.md). The owner's broader objective is reusable
conditional exploration of dimensions, measures and learned concepts; earlier
single-task tests remain evidence for their stated tasks. See the
[attributed reflection](../CODEX_REFLECTIONS.md#2026-11-14--native-ai-for-financial-exploration-owners-correction-and-codex-reflection).
Earlier "Phase 1b" wording is historical; current synthetic work is Phase 2.

## Evidence and limits

2026-11-17: Owner-designated
[CKB architecture-transfer case](../../../docs/REPORTING_CKB_TRANSFER_STUDY.md)
records an AI-assisted reporting implementation failing to transfer available
CKB mechanisms, despite a contrasting fused posting flow. This is a measured
observational case outside the separate Control repository, not a new model
score. Inadequate training is the owner's hypothesis; its causal attribution
remains unknown. A matched improved-architecture rerun is specified before
execution, with correctness equivalence and measurement coverage limits.

2026-11-17: [Allocation feedback and stream evolution](../../../docs/STREAM_LAYER_EVOLUTION_AND_ALLOCATION_FEEDBACK_PLAN.md)
describes a proposed reusable engine-backed rule experimentation loop. Unchanged
core preparation may be reused across downstream candidates. Final-rule
verification must compare full receiver bases and allocation results, rather
than denominator totals alone. Three-hour cycles are an owner planning estimate;
the new loop is not implemented or timed. Repeated rule-search loops are
development examples, with separate held-out evaluation required. No AIM run.

| Item | Recorded result and limitation |
|---|---|
| AIM1 | Non-zero count gap across 14 years; partially verified for the specified key/count comparison, not predictive superiority |
| AIM3 | Commitment hold-out C = 0.7527, p = 0.001; walk-forward mean C = 0.7629 |
| AIM3 balance / combined | Matched-split balance C = 0.7683; combined C = 0.7761. Provisional: unresolved fiscal-period handling, one split, no stability check. The old overlapping-split 0.7783 is superseded |
| AIM4 | FAIL under the implemented fourth-quarter-ratio method (2/5); the predeclared seasonal-decomposition method was not run |
| AIM5 | Precision 1.0 for its stated agency-name test; data-quality evidence |
| Three-model experiment | Plan/preparation/Gold checks completed; no trained-model comparison or language-prior effect established |
| Field-label pilot | 512/128/256 examples prepared; 43 preparation tests passed. Training not run after download/training permission was declined |
| New synthetic inputs | Bounded component reporting completed at `213b20f`; no AI result or corpus-capacity evidence follows |

Sources: [ARM reproduction](../../../data/test_evidence/aim_run_arm64_20261113/),
[Paper 2 assessment](../PAPER2_ASSESSMENT_2026-11-12.md),
[field-label contract](../../../docs/TRAINING_LANGUAGE_PILOT.md), and
[component evidence](../../../data/test_evidence/component_pilot_0304_20261115/README.md).
The [prior B2 status](../../../docs/archive/collaborator_review_2026-11-15/tracks/B2-models/STATUS.md)
retains detailed result paths, decisions and corrections.

## Next work and decisions

**2026-11-18 owner clarification and sharing:** Q2 targets learned prediction of
the next useful financial response with an associated measure, not merely
translation into predefined filters. [Q2 process](Q2_PROCESS.md) logs this
discussion and proposes a 6–10-conversation development pilot before choosing a
training design. Owner requested both OneDrive starter packages be versioned;
see [shared data and launcher](../../../data/q2-development/README.md).
Source baseline `7b0f626`; no model training/scoring or payroll result. Next:
review pilot conversations and exact-versus-predicted response contract. Model
and numerical architecture remain undecided.

Sharing verification: original archives, four split parts, reconstructed gzip,
and all 16 inner starter manifest entries passed checksums. Shared Bash launcher
passed syntax and end-to-end sbt smoke checks, loading 80 cells and exiting 0.

**2026-11-18 ARM explorer launch:** At source commit `7b0f626`, verified the
OneDrive `explorer_20261118_v1` archive against its `SHA256SUMS`, extracted it
locally, and compiled the standalone `02-foundation/va_pipeline` project.
The documented FY2044 vendor loaded 80 report cells and exited cleanly in a
smoke check. Opened the interactive launcher in Terminal; local launcher and
report are in `session-run.tmp/q2-explorer/`. Root sbt launch failed on missing
assembly-plugin symbols; standalone build succeeded. Next: interactive
development exploration; no training or scoring ran and no new decision waits.

**2026-11-18 Q2 exploration started:** The [Q2 process](Q2_PROCESS.md) records
the verbalized versus native tuple comparison, a separate numerical path, and
the first accepted-report interaction. Changing from signed amount to
contributing row count changed the leading agency in a selected FY2044
commitment slice. Amount ranges selected different agencies and an empty range.
The initial placeholder match exposed the need to carry attribution explicitly.
Exploratory exact-data results only. Allocation and reclass are outside the
proposed Phase 2 scope. No training or scoring ran.

**Interactive checkpoint:** The Scala explorer supports filters, rank and range
queries, and paired prose, tuple, and typed-numeric examples. It exported 240
masked development examples from 80 FY2044 cells and exercised a separate
negative expense-payment case. Fresh compilation and ScalaTest reported 165
passed, and the connected identity fixture passed separately. Launch steps, data boundaries, and implications for
the first learned range test are in [Q2 process](Q2_PROCESS.md).

**2026-11-18 Q2 proposal, revised after reading the Codex reflections:** Test
whether one reusable representation supports investigations that change
dimensions, measures, learned concepts, periods and business domains as evidence
emerges. Explore conditional completion with associated metrics and uncertainty.
Score whole held-out inquiry sequences, including unsupported combinations and
revisions under later evidence.
The existing vendor activity predictor is one possible probe of that capability.
Local development inspection found 491,385 combined feature rows across
FY2030–FY2044, of which 269,711 carry the existing matched-eligibility flag.
Delivery and settlement require independent lifecycle evidence. Allocation and
basis feedback remain planned. No evaluation or training ran.

**Owner scope clarification, 2026-11-16:** Current preparation imagines source-data
conditions that might support envisioned AIM functionality. Extensive design of
these new models and their features has not occurred; their eventual architectures
and feature representations remain unknown. The capability matrix is a set of
functional hypotheses, not a completed model/feature design. This qualification
applies to the new work, not a retraction of the separately scoped historical
tests above.

**2026-11-16 owner extension:** New synthetic/AIM design proceeds in parallel
with independent Paper 1/2 editing sessions. See the
[capability and corpus requirements](../../../docs/PHASE_TWO_AIM_CORPUS_REQUIREMENTS.md):
component outputs across historically disconnected sources; later evidence
resolving tentative outputs; temporal recasts, forecasting and risk under temporary
future reference versions; matched source-quality comparisons. Later AIM work is
placed by owner in Papers 5/6, Phase 3, with a possible Paper 5 engine extension.
Status is design_only; no new model, training permission or performance result.
Corpus/model co-design must be disclosed and independently held-out mechanisms
reserved before formal evaluation. Earlier AIM findings above are unchanged.

1. Resolve the `00YY/MM` fiscal-period discrepancy in balance/combined analyses
   before relying on the reported gain; preserve prior evidence. No new Python
   approval is conferred here.
2. Design a shared exploration representation, exact-versus-estimated answer
   contract and held-out question combinations against versioned inputs. Avoid
   exposing synthetic truth/component labels as model features by accident.
3. Produce matched movement/balance inputs in Scala at declared boundaries;
   verify linkage, periods and populations before model comparison. Bounded
   reporting success does not repair the earlier corpus attempt retroactively.
4. Keep the field-label pilot separate from broad model claims. Model/runtime
   selection and capacity remain open; prior declined execution is not resumed
   by this documentation session.

Owner decisions still include D9's target/accounting basis and verbalizer account/
period visibility. D9 does not block the separate vendor-presence field-label pilot.
Use [TODO.md](../../../TODO.md) for remaining actions. Paper revisions proceed
independently through the [member handoff](../MEMBER_REPO_EDITING_PLAN_2026-11-14.md).
