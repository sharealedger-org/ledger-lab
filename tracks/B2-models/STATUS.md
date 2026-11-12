# Track B2: Paper 2 models (AIM tests and the three-model experiment)

Updated 2026-11-12. **On hold** during the reorganisation (owner, 2026-11-12). Claim under test: a model trained on posted (GL) data cannot recover what posting threw away; a model trained on the instrument ledger can.

## Three-model experiment

Posted-FineTune (on GL), IL-FineTune (on the ledger `LDGR`), IL-Native (structured model, no language model), same tasks. Plan and decisions D1 to D8: [`docs/THREE_MODEL_EXPERIMENT_PLAN.md`](../../docs/THREE_MODEL_EXPERIMENT_PLAN.md).

| Step | State |
|---|---|
| 1 Plan, 2 Verbalizer, 3 Labels/splits/metrics/results writer | Done on fixtures; 40 tests pass on `main` (`scripts/test_three_model_*.py`) |
| 4 Gold data checks | Done: 104 of 104 SHA-256; GL and LDGR agree 14 of 14 years on the expenditure basis |
| 5 IL-Native, 6 Model choice (Apache 2.0, check advisories first), 7 Fine-tune and score | Not started. No model has been trained. |

**Blocked on the owner: D9, net income.** Gold holds only `EXP*`, `COMMIT` and offset accounts; revenue minus expense cannot be derived from it. Reopened 2026-11-12: the raw corpus has `apa_revenue` (17.75M rows), so this is a Gold limit, not a corpus limit. Candidates: net expenditure per agency and year (proposed), reserves per fund, or the statement tie-out. A reserves or net income target must name its basis (budget, GAAP, encumbrance); the ACFR shows the General Fund as 1,683,412 on the budget basis and 512,359 on the GAAP basis for FY2012 ($ thousands). See [`docs/VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md`](../../docs/VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md).

## AIM tests (provisional until re-run on the final views)

AIM0 closed. AIM1 first pass on FY2012 only. AIM3 hold-out C = 0.7527, p = 0.001 (PASS, with the caveats in `CURRENT_RESULTS.md` section 5). AIM4 FAIL on a simple test, not the locked STL method. AIM5 verified for its stated test. AIM2 not run.

## Next actions (when un-paused)

1. Owner decisions: D9, verbalizer visibility of account text, period text normalisation, first anchor figure for the tie-out, where the raw `VARawFiles` and budget files are.
2. Steps 5 to 7. Re-run AIM1, AIM3, AIM4 on the final views.
3. Revenue through the Scala TTL for one year needs the architecture-gate declaration first (`docs/AGENTIC_ARCHITECTURE_GATES.md`).

Python scripts here are post-engine analysis; the owner approved them earlier, and a new one needs fresh approval and the "could Scala do this?" question (`AGENTS.md`). To-dos: `TODO.md` sections "Three-model experiment" and "Model tests (AIM)".
