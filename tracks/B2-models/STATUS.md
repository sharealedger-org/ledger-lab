# Track B2: Paper 2 models (AIM tests and the three-model experiment)

Updated 2026-11-13. Claim under test: a model trained on posted (GL) data cannot recover what posting threw away; a model trained on the instrument ledger can.

## Three-model experiment

Posted-FineTune (on GL), IL-FineTune (on the ledger `LDGR`), IL-Native (structured model, no language model), same tasks. Plan and decisions D1 to D8: [`docs/THREE_MODEL_EXPERIMENT_PLAN.md`](../../docs/THREE_MODEL_EXPERIMENT_PLAN.md).

| Step | State |
|---|---|
| 1 Plan, 2 Verbalizer, 3 Labels/splits/metrics/results writer | Done on fixtures; 40 tests pass on `main` (`scripts/test_three_model_*.py`) |
| 4 Gold data checks | Done: 104 of 104 SHA-256; GL and LDGR agree 14 of 14 years on the expenditure basis |
| 5 IL-Native, 6 Model choice (Apache 2.0, check advisories first), 7 Fine-tune and score | Not started. No model has been trained. |

**Blocked on the owner: D9, net income.** Gold holds only `EXP*`, `COMMIT` and offset accounts; revenue minus expense cannot be derived from it. Reopened 2026-11-12: the raw corpus has `apa_revenue` (17.75M rows), so this is a Gold limit, not a corpus limit. Candidates: net expenditure per agency and year (proposed), reserves per fund, or the statement tie-out. A reserves or net income target must name its basis (budget, GAAP, encumbrance); the ACFR shows the General Fund as 1,683,412 on the budget basis and 512,359 on the GAAP basis for FY2012 ($ thousands). See [`docs/VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md`](../../docs/VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md).

## AIM tests

Updated 2026-11-13. Intel run (CSV timestamps 2026-11-12 20:37 to 20:43) reproduced on the ARM Mac (2026-11-12 21:32 to 21:35 local); AIM1, AIM3 and AIM4 results are identical. 44 Gold input files verified against the Gold `MANIFEST.csv`. ARM evidence: `data/test_evidence/aim_run_arm64_20261113/` (`RUN_ENV.txt`, `RUN_INPUTS.csv`); command: `bash scripts/run_aim_tests.sh`.

| Test | Result | Note |
|---|---|---|
| AIM0 | closed | — |
| AIM1 | **`partially_verified`** — non-zero gap in all 14 years | Intel run (CSV 2026-11-12): all 14 years, IL keys with multiple patterns vs. PERM keys with multiple vendors: gap -55,915 (FY2003) to -92,562 (FY2012). Evidence: `data/test_evidence/AIM1_noninjectivity_all_years.csv`. |
| AIM2 | not run | needs AIM5 reconciliation table first |
| AIM3 | **PASS** C = 0.7527, p = 0.001 | Intel run (CSV 2026-11-12): re-run on Gold final views, identical to the 2026-11-10 result. Walk-forward mean C = 0.7629. Evidence: `data/test_evidence/AIM3_survival_windows.csv`. |
| AIM3 balance baseline | **`partially_verified` (provisional)** — commitment model alone does not exceed balance on a matched split; balance plus view features does | ARM run, train FY2010-2013 and test FY2014-2015 as the commitment model: balance C = 0.7683 (commitment 0.7527, delta +0.0156), p = 0.001. Time-trend control C = 0.4999 (FAIL). The Intel figure 0.7783 is superseded: its training set included FY2014, which was also a test year. Combined model (`scripts/fit_vendor_survival_combined.py`): C = 0.7761, delta +0.0078 vs balance, bootstrap interval [+0.0066, +0.0091], p = 0.001. Provisional: one split, no stability check, no multiple-comparison correction, practical size not established. Evidence: `data/test_evidence/AIM3_balance_baseline.csv`, `data/test_evidence/AIM3_combined_model.csv`. |
| AIM4 | **FAIL** (Q4-ratio, not locked STL) | Intel run (CSV 2026-11-12): 2 of 5 top classes significant (threshold ≥ 3). Uses Q4-ratio test, not the locked STL method. Evidence: `data/test_evidence/AIM4_seasonality_results.csv`. |
| AIM5 | verified for its stated test | `scripts/fit_agency_name_classifier.py`, Precision 1.0 |

## Next actions (when D9 decided)

1. **Owner decisions:** D9, verbalizer visibility of account text, period text normalisation, first anchor figure for the tie-out, where the raw `VARawFiles` and budget files are.
2. ~~Finish AIM3 balance baseline~~ Done on ARM with a matched split (audit register, last entry). Open: combined balance plus view model against balance alone (owner approval needed before writing the Python). TDS evidence copies and the Paper 2 text are refreshed after owner review.
3. **AIM4 locked STL:** implement using `statsmodels` STL (now installed), majority-of-agencies rule, locked thresholds; owner approval required before writing (`AGENTS.md`).
4. Steps 5 to 7.
5. Revenue through the Scala TTL for one year needs the architecture-gate declaration first (`docs/AGENTIC_ARCHITECTURE_GATES.md`).

Python scripts here are post-engine analysis; the owner approved them earlier, and a new one needs fresh approval and the "could Scala do this?" question (`AGENTS.md`). To-dos: `TODO.md` sections "Three-model experiment" and "Model tests (AIM)".
