# Three-Model Experiment: Working Plan

Status: **plan with decisions recorded, 2026-11-11, branch `aim-three-model`.** Nothing here has been run. The experiment definition is in [TEST_DESIGN.md](TEST_DESIGN.md) §2 and §5a and [RESEARCH_OBJECTIVES.md](RESEARCH_OBJECTIVES.md) (Part 2). This file does not change that definition; it turns it into ordered work and records the decisions the design leaves open. The owner delegated D1-D7 to the agent on 2026-11-11 ("I have no opinions... your guidance will be accepted on all the points"), so they are the agent's judgment calls, written down with reasons so a human reviewer can challenge them. They are fixed before any real-data run; a change after that is logged as a deviation, with the reason.

## 1. Approvals and boundaries

- **Python is approved by the owner for this experiment (2026-11-11 session): model training and evaluation, post-engine, not timed.** Per AGENTS.md the scope is restated here and again at the top of each script before it is written.
- **Scope of the approval as understood:** verbalization of GL/LDGR records, label and split construction from the committed outputs, fine-tuning and evaluation of Posted-FineTune and IL-FineTune, building and evaluating IL-Native, and writing `data/test_evidence/three_model_experiment_results.csv`.
- **Not in scope:** any posting, CKB, step-up or aggregation that feeds the models. If a model input needs aggregation over the corpus, it is asked "could Scala/`5v` do this?" first and, if yes, goes through the Scala path (AGENTS.md GenevaERS simulation rule). Python reads the committed output files only.
- **Measurement:** not timed. The experiment is outside the measured financial path and contributes nothing to any cost curve. Run time, machine and library versions are recorded only so the run can be repeated.

## 2. Inputs (from the run archive; all from the same corpus run)

| Model | Reads | Known columns (source) |
|---|---|---|
| Posted-FineTune | `GL<yyyy>.csv` (step 20) | `agency, nominal_account_id, fund, object_id, fiscal_year, dir_flag, total_amount` (`GLPost.scala` line 151). No vendor, no instrument id. |
| IL-FineTune | `LDGR<yyyy>.csv` (step 3) | 30 named columns (the header line ends in a trailing comma), starting `ldgrIPID, ldgrContractID, ldgrCommitmentID, ldgrLdgrlID, ldgrSourceSystemID, ldgrLedgerID, ldgrJrnlType, ldgrBookCodeID, ldgrLegalEntityID, ldgrCenterID, ldgrProjectID, ldgrProductID, ldgrNominalAccountID, ..., ldgrLedgerPeriod, ldgrTransAmount, ..., ldgrDirVsOffsetFlg` (`post.scala` `ldgrHeader`). |
| IL-Native | `LDGR<yyyy>.csv` and the four `view_*.csv` files (step `5v`) | as above, plus the view keys in TEST_DESIGN.md. |

- The standard reference copy is the OneDrive gold set (`LedgerLabOutputs-Gold`; 104 files, 2.5 GB), listed with SHA-256 in `data/test_evidence/corpus_runs/run_20261111_014229/gold/MANIFEST.csv`. **Not yet on this machine** (the upload on the other machine had not completed).
- Run 3 and the baseline are byte-identical on `LDGR*`, `PERM_SUMMARY*` and the views, so either serves. `GL*` and `PERM_TBL*` were not compared between them; the gold copy is run 3's.
- Row counts to check on load: ledger 10,651,908 rows in total; GL keys 6,433,714 in total (`corpus_totals.csv`).

## 3. Controls before any model sees data

1. Every input file's SHA-256 equals its `MANIFEST.csv` entry. Stop on any difference.
2. Per-year row counts equal the run's `cost_surface` and `process_metrics` figures.
3. No bare-year or short-form fiscal periods and no row with the wrong field count (the old PO reader defect; the fixed reader gave none in run 3).
4. The same train/test split, label rule and censoring rule are applied to all three models and written to the results file.

## 4. Decisions (delegated by the owner; agent's judgment, 2026-11-11)

| # | Decision | Why it matters | Decision |
|---|---|---|---|
| D1 | Which pretrained LLM, and which fine-tuning method, for models 1 and 2. | The design names none. Both must use the same base model and settings, or the 1→2 gap measures the setup. The 16 GB M1 here limits this to small models and parameter-efficient methods; the other machine is smaller. | One small open-weight base model under **Apache 2.0** (the project's licence; Sharealedger uses Apache 2.0), the same LoRA adapter fine-tune for both models, same hyperparameters, fixed seeds. Candidate families: IBM Granite small models, or Qwen2.5 sizes that are Apache 2.0 (not every Qwen2.5 size is). Training stack: Hugging Face `transformers` + `peft` in a virtual environment with a lock file, run on this M1 only (not timed). **Not yet verified, and verified when the model is chosen at step 6, not before:** the exact model identifier and release, that its licence is Apache 2.0 on the model card, that it is maintained (release within about 6 months), and the latest stable library versions with no open high-severity advisories (security.md). No model or library has been downloaded or installed. |
| D2 | Verbalization schema (structured record to text). | TEST_DESIGN.md §5a defers it to "when scripts are written". It decides what each fine-tuned model can see. | One template per substrate listing only that substrate's columns, in fixed order; no derived fields. Written and fixture-tested before any training. |
| D3 | Task definitions and labels. | "Same tasks" needs identical targets. | Vendor survival: the AIM3 label and censoring rule from `fit_vendor_survival_windows.py`. Forecasting: next fiscal year amount per agency, from the same split. Net Income: the logical derivation, scored pass/fail per model. |
| D4 | Train/test split. | Prevents leakage; the locked 7 windows is not available on PO-derived tests. | Reuse the hold-out already used for AIM3: train FY2010-FY2013, test FY2014-FY2015 (4 walk-forward windows as a stability check). Same split for all three models. |
| D5 | Metrics. | The gap columns need one metric per task. | Survival: C-statistic and the AIM3 permutation test. Forecasting: sMAPE and RMSE, with a naive last-year baseline. Net Income: correct or not. |
| D6 | Form of IL-Native. | "Structured model, no pretrained LLM" can be many things. | A plain structured model on IL-derived features (the AIM3 features are the starting point), so the 2→3 gap isolates the language prior and not a bigger model. |
| D7 | How "cannot answer" is scored. | Posted-FineTune has no vendor field, so survival is not answerable from its substrate. | Report `not_answerable` as a result, not as a score of zero. |

## 5. Work in order

Steps 1-3 need no corpus data and can run on this machine now (fixtures only):

1. D1-D7 are decided in section 4 (done 2026-11-11). Open check carried into step 3: the forecasting target is computed from `GL` (for model 1) and from `LDGR` (models 2 and 3); the two must agree on a common basis (for example debit lines only), or the difference is reported. If they cannot be reconciled, the target is redefined on the common basis and logged as a deviation.
2. **Done 2026-11-11:** verbalizer `scripts/three_model_verbalize.py` with 12 unit tests (`python3 -m unittest scripts/test_three_model_verbalize.py`, all pass), using fixture rows in `data/three_model_fixture/` (`GL2003_sample.csv`, 27 rows; `LDGR2003_sample.csv`, 8 rows; from the fixture pipeline, steps 2,3,20,21). It refuses a file whose header differs from the expected one, so the headers in the real gold files are checked at load. They are taken from the Scala sources and match the fixture output, but have not been compared with the real corpus files, which are not on this machine yet. **Open item for step 3, new:** an LDGR record is about 700 characters of text and the corpus has 10.65M LDGR rows and 6.43M GL keys, so no model can be fine-tuned on every record. Step 3 must define the training sample (size, fixed-seed selection, same rule for both substrates and the same source events where possible) before any real-data run, and record it as decision D8.
3. Write the label, split and metric code with tests on fixtures, and the results-file writer (one row per model, task, metric; gap columns computed from model pairs).
4. Once the gold copy has synced: run controls in section 3 on the real files.
5. Build IL-Native and score it (cheapest real run, no LLM).
6. Fine-tune and score Posted-FineTune and IL-FineTune.
7. Write `data/test_evidence/three_model_experiment_results.csv`, then the audit register entry and handoff. Each claim carries a status; "implemented" is not claimed without the output and its check.

## 6. Gate declaration (AGENTIC_ARCHITECTURE_GATES.md §1, adapted: this is not an engine)

- **Research question:** how much do substrate (posted vs instrument ledger) and a surviving language prior each cost, on the same downstream tasks?
- **Layer:** post-engine analysis outside `01-` to `05-`; it consumes committed outputs and writes evidence only.
- **CKB placement / pass boundary:** none; it never re-reads source transactions.
- **Schemas:** inputs as in section 2; output `three_model_experiment_results.csv` (columns fixed by step 3).
- **Controls:** section 3.
- **Evidence:** audit register entry per step; seeds, base-model identifier and library versions recorded in the results file.

## 7. Other items that also wait for the data

AIM1, AIM3 and AIM4 re-runs on the new corpus (the AIM1, AIM3 and AIM4 results to date are provisional; see SESSION_HANDOFF.md), and the AIM4 STL and AIM5 reconciliation work. They are not part of this plan.
