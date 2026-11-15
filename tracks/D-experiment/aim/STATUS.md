# Track D / AIM: AI and financial exploration

Updated 2026-11-15. Former Track B2 is a workstream inside the
[primary experiment](../STATUS.md). The owner's broader objective is reusable
conditional exploration of dimensions, measures and learned concepts; earlier
single-task tests remain evidence for their stated tasks. See the
[attributed reflection](../CODEX_REFLECTIONS.md#2026-11-14--native-ai-for-financial-exploration-owners-correction-and-codex-reflection).
Earlier "Phase 1b" wording is historical; current synthetic work is Phase 2.

## Evidence and limits

| Item | Recorded result and limitation |
|---|---|
| AIM1 | Non-zero count gap across 14 years; partially verified for the specified key/count comparison, not predictive superiority |
| AIM3 | Commitment hold-out C = 0.7527, p = 0.001; walk-forward mean C = 0.7629 |
| AIM3 balance / combined | Matched-split balance C = 0.7683; combined C = 0.7761. Provisional: unresolved fiscal-period handling, one split, no stability check. The old overlapping-split 0.7783 is superseded |
| AIM4 | FAIL under the implemented Q4-ratio method (2/5); the locked STL method was not run |
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
