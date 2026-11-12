# Ledger Lab Handoff

Updated 2026-11-11. This file is a dated log, newest entry first; use the linked design contracts for details.

- **Open work is in [`TODO.md`](TODO.md).** Add new to-dos there, not here.
- **Older entries** (run 3 preflight and everything before it, back to 2026-09-30) were moved unchanged to [`docs/archive/SESSION_HANDOFF_ARCHIVE_2026-11.md`](docs/archive/SESSION_HANDOFF_ARCHIVE_2026-11.md). Other documents that cite a `SESSION_HANDOFF.md` entry by name (for example "Corpus Work Assignment to Bob", "Plan for the next session", the 2026-11-06 entries) mean the archive.
- When this file passes about 500 lines, move all but the newest few entries to the archive in the same way.

## 2026-11-11 (BRANCH `aim-three-model`) One-page explainer saved; further edits are a to-do (Bob, M1 Mac)

**Done.** `main` merged into this branch (clean). Charts and the summary `docs/EXPERIMENT_STATE_2026-11-11.md` written. `docs/COST_GRAPH_DESIGN.md` updated to the measured corpus constants and given a Mix-graph model run (model output; unit rates and query volumes are assumptions). The one-page explainer for outside readers is saved as `docs/LEDGER_LAB_ONE_PAGE_EXPLAINER.html` (static, opens in a browser; its graph is drawn as SVG with fixed pixel size).

**To do.** Moved to the new list [`TODO.md`](TODO.md) (explainer edits, cost graphs, three-model experiment, housekeeping), so they are not lost as this file grows. Add new to-dos there, not here.


## 2026-11-11 (BRANCH `aim-three-model`) Step 4 second pass: forecasting target fixed on the expenditure basis, net income proposal D9; start the next session here (Bob, M1 Mac)

**Done.** Forecasting target changed (plan D3, logged deviation) to expenditure debit lines: dir flag `D` on a nominal account starting `EXP`, per numeric agency code and fiscal year. On it `GL` and `LDGR` agree to the cent in 14 of 14 years (awk) and with maximum difference 0.00 in FY2003, FY2010 and FY2016 (Python). `scripts/three_model_core.py` and its tests updated, 40 tests pass. Evidence: audit register entry "Forecasting target moved to the expenditure basis". Branch pushed.

**Found.** The Gold GL and LDGR contain only `EXP*`, `COMMIT` and the `0000` offset accounts in all 14 years. No revenue accounts, so a revenue-minus-expense net income cannot be derived.

**Open, in order (next session starts here).**
1. **Owner: D9, net income.** Proposed: net expenditure per agency and fiscal year, `EXP*` debits only, `COMMIT` and offset excluded (full text in `docs/THREE_MODEL_EXPERIMENT_PLAN.md` D9). Also decide whether the `nominal_account_id` / `ldgrNominalAccountID` text stays in the verbalized records; if it stays, the "cannot answer from events alone" claim holds only for raw events.
2. **Verbalizer on real headers: done.** `read_records` accepted the real `GL` and `LDGR` headers for FY2003, FY2009 and FY2016 (2 records read from each). The text shows the period as stored, so payment rows read `0016/01` and PO rows `2015/07`; both forms reach the models unless the verbalizer is told to normalise them. Decide with D9 (same question: what the model is allowed to see). `PERM_TBL` FY2009 has 11 columns (others 13); not used by this experiment.
3. **Step 5 IL-Native** on fixtures first, then on Gold. **Step 6** model choice (Apache 2.0, version and advisories verified first). **Step 7** fine-tune, score, results file `data/test_evidence/three_model_experiment_results.csv`.
4. **Step 3 note:** D4 split (train FY2010-FY2013, test FY2014-FY2015) is unchanged; PO rows near a year boundary fall in a neighbouring fiscal year (about 0.5%), as the first-pass entry says.

**Other machine.** Track D (Codex) is in development there; run 3 and run 4 outputs were deleted (458 GiB free). Not touched from here. `main` has the merged-SJE hashes and the deletion record, merged into this branch.

**Python note.** This session used short Python and awk one-offs for read-only data inspection and for doc edits; no Python was added to the measured path.

## 2026-11-11 (BRANCH `aim-three-model`) Step 4 first pass: Gold set verified, period format defect fixed, forecasting target needs an owner decision (Bob, M1 Mac)

**Done.** `main` merged into this branch (`942d8ee`, conflicts in this file and the audit register resolved by keeping both sides; pushed). Gold set on this machine: 104 of 104 files match `MANIFEST.csv`. Real-file checks, evidence in the audit register entry "Gold set checks for the three-model experiment":
- **Defect found and fixed:** periods are not all `YYYY/MM`. Payment rows carry `00YY/MM` (`0003/05`), PO rows `YYYY/MM`. The old `fiscal_year_of` did not raise on `0003/05`; it returned year 3 (about half of all ledger rows would have had a wrong fiscal year). Fixed in `scripts/three_model_core.py` (`00YY/MM` is 2000+YY; `last_complete_fy` counts months), 2 tests added, 39 tests pass.
- **GL vs LDGR debit totals agree** per agency and fiscal year, 14 of 14 years (awk), FY2003 and FY2016 also in Python. Use `compare_totals(..., tolerance=Decimal("0.005"))`; the default 0 flags 2e-20 noise.
- **LDGR vendor presence equals the `5v` view:** 199,727 vendor-year pairs each, 0 on either side only. The survival label can come from the view.

**Open, needs the owner.** The forecasting target "debit lines per agency and fiscal year" is not coherent: payments (`EXP`) use numeric agency codes, PO rows (`COMMIT`) use agency names, no value in common, and the sum adds commitments to expenditures. Proposed (agent's judgment): `EXP` debits per agency code. It changes D3, so it is not applied until the owner agrees. Then: net income ground truth (still undefined), step 5 IL-Native, step 6 model choice (verify first), step 7 results.

**Other machine.** One more test is running there and will be committed to `main`; this branch is pushed so that `main` can be updated from it, but `main` itself was not touched here.


## 2026-11-11 (BRANCH `aim-three-model`) Step 3 done: labels, splits, D8 sample, metrics and results writer, fixtures only (Bob, M1 Mac)

**Done (uncommitted).** `scripts/three_model_core.py`, `scripts/three_model_results.py`, `scripts/test_three_model_core.py`. `python3 -m unittest scripts/test_three_model_core.py scripts/test_three_model_verbalize.py` from the repo root: 37 tests pass (25 new). Python standard library only (numpy / scikit-learn are not installed). Owner approved Python for this work; scope restated at the top of each script. No corpus data read, nothing downloaded or installed.
- **D8 recorded** in `docs/THREE_MODEL_EXPERIMENT_PLAN.md` section 4: entity-hash sample, seed 42, one fraction for FY2010-FY2013, target about 20,000 records per substrate; LDGR entity = `ldgrIPID` (which is the vendor id), GL entity = agency|account|fund|object. The target of 20,000 and the choice of unit are the agent's judgment (owner delegated D1-D8); change before the first real run, not after.
- **Forecasting-target check:** both sides use debit lines (`D`) per agency and fiscal year; `compare_totals` reports any difference. Proven only on constructed fixtures. Not yet run on real files.
- **Split check:** with eligible FY 2006-2008,2010-2017 and last complete FY 2016 the code gives train 2010-2013, test 2014-2015, 4 walk-forward windows, as D4 says.

**Open, in order.**
1. **Data (step 4).** Gold set must be on this machine; verify SHA-256 and row counts (plan section 3), then check: real GL `fiscal_year` / LDGR `ldgrLedgerPeriod` really are `YYYY/MM` (the code raises on anything else; the fixtures have two-digit periods); the GL-vs-LDGR debit totals agree; LDGR `ldgrIPID` presence by fiscal year matches the `5v` vendor-year view (the survival label comes from the view, as AIM3).
2. **Net income ground truth** is undefined (chart-of-accounts classification); only the scoring function exists. Needs a definition before the task can be run; ask the owner or record it as a decision.
3. Step 5 IL-Native, step 6 model choice (verify Apache 2.0 model, versions, advisories first), step 7 results and audit register entry. Not started.
4. The older items (last CCP run timing checks, merge conflicts at the top of this file and the audit register) are in the entry below.


## 2026-11-11 (BRANCH `aim-three-model`) Three-model experiment: plan written, decisions D1-D7 recorded, step 2 (verbalizer) done; start the next session here (Bob, M1 Mac)

**Where to work.** Branch `aim-three-model` (pushed by the owner at `73a1d11`; later commits are local until pushed). `main` was left alone because the other machine (4 CPUs) is running the last CCP run and will commit its evidence to `main`; the top of this file and the end of `docs/VERIFICATION_AUDIT_REGISTER.md` will conflict on merge, so resolve those two by hand. Do not touch the branch or working tree on the machine that is running.

**Read first:** `docs/THREE_MODEL_EXPERIMENT_PLAN.md` (the working plan; section 4 has D1-D7, section 5 the ordered steps, section 3 the controls). Experiment definition: `docs/TEST_DESIGN.md` §2 and §5a, `docs/RESEARCH_OBJECTIVES.md` Part 2.

**Done this session.**
- Run 3 compared with the baseline (read-only): outputs identical, CPU +4.8% (chain steps 3/20/21 +15%, inferred contention), wall -30.3%, and a 7,635 s unmetered tail after "Pipeline complete" (54% of chain wall). Run 3 is an output replicate of the baseline, so no CCP output or AIM status change; its cost point is separate and not comparable by `total_compute_s` or `total_cost_proxy`. Entry at the top of this file's previous section and in the audit register.
- The three-model experiment (Posted-FineTune, IL-FineTune, IL-Native) has not been run; nothing in run 3 bears on it. The earlier AIM1/AIM3/AIM4 outputs remain provisional.
- **Owner approvals recorded:** Python is approved for the three-model training and evaluation scripts, not timed; the owner delegated D1-D7 to the agent ("your guidance will be accepted on all the points"), so they are the agent's judgment calls, fixed before any real-data run, with later changes logged as deviations. Base model must be Apache 2.0 (the project licence).
- Step 2 done: `scripts/three_model_verbalize.py` + `scripts/test_three_model_verbalize.py` (12 tests, pass; `python3 -m unittest scripts/test_three_model_verbalize.py` from the repo root) + fixture rows in `data/three_model_fixture/`.

**Open, in order.**
1. **Step 3** (fixtures only, no corpus data needed): label, split and metric code with tests, and the results-file writer for `data/test_evidence/three_model_experiment_results.csv`. Carries two items: **D8**, the training-sample rule (an LDGR text is about 700 characters; the corpus has 10.65M ledger rows and 6.43M GL keys, so a fixed-seed sample is needed, same rule for both substrates), and the **forecasting-target check** (GL-derived and LDGR-derived targets must agree on a common basis, or the difference is reported).
2. **Data:** the OneDrive gold set (`~/Library/CloudStorage/OneDrive-Personal/LedgerLabOutputs-Gold/`, 104 files, 2.5 GB, `MANIFEST.csv` has SHA-256) was not on this machine when checked and the upload on the other machine may not be finished. Steps 4-7 of the plan need it. The verbalizer's expected headers come from the Scala sources and the fixture; they have not been compared with the real files (the verbalizer refuses a mismatch).
3. **When the last CCP run ends and the other machine is idle** (owner asked for these to be logged, not started; details in the previous section): time `write_canonical_evidence.sh` and `interpret_run.sh` part by part on FY12; run one chain alone to compare CPU; add the last run to the run 3 comparison.
4. **Before choosing a model (step 6):** verify the exact model identifier and release, Apache 2.0 on the model card, maintenance (release within about 6 months), and latest stable library versions with no high-severity advisories. Nothing has been downloaded or installed; `python3` is 3.13.0 with none of the ML packages installed; `ollama` is present. The 16 GB M1 limits models 1 and 2 to small models with LoRA.


## 2026-11-11 (RUN 3 ANALYSIS, TO DO AFTER THE LAST CCP RUN) Run 3 against the baseline: what the numbers say, and three checks to run once the machine is idle (Bob, analysis only)

**Nothing was run or changed.** Read-only comparison of `run_20261111_014229` (run 3) against `run_20261110_210721` (baseline). The Mac running the last CCP run is a different machine, so the checks below wait for it to finish. Full evidence table: `docs/VERIFICATION_AUDIT_REGISTER.md`, "2026-11-11 Run 3 against the baseline: CPU and the unmetered chain tail".

**Findings.**
- Outputs, ledger rows (10,651,908), ledger bytes, quarantine counts and committed bytes are the same. The baseline `permutation_rows` / capacity sum was short by exactly FY03 (396,858 rows; 1,544,407,169 capacity); run 3 is complete.
- Wall clock -30.3% (11,695 s to 8,147 s); step CPU +4.8% (10,670 s to 11,179 s). Prep CPU is -1.2% (6,757 to 6,673). Chain CPU (steps 3, 20, 21) is +15.2% (3,913 to 4,506): step 3 +17.5%, step 20 +9.7%, step 21 +17.1%. Engine code is unchanged, so this is contention. By year: FY03-10 +9%, FY11-14 +31% (the years that overlapped `5v`), FY15-16 +8%. The `5v` link is an inference, not tested. CPU seconds are therefore not contention-proof on this 4-CPU machine.
- `cost_surface` `total_compute_s` is summed step wall time: CCP1 +50% (3,861 to 5,808 s), CCP2 +7%. `total_cost_proxy` = compute seconds + storage MB, and storage (about 95% of it) is unchanged, so the CCP1 proxy rise of +1.6% is an artifact of contention-inflated elapsed seconds. Do not quote either as a cost change between these runs.
- **Unmetered chain tail.** The two chain lanes were about 97% busy, so chain throughput is the critical path. Over 14 years the chains' wall time was 14,102 s: 6,464 s inside the pipeline (to "Pipeline complete") and 7,635 s after it (255 s for FY16 to 844 s for FY08 per year). The tail is the post-pipeline part of `run_pipeline.sh`: `write_canonical_evidence.sh` (single-threaded `awk` over each merged SJE file for `sje_balance`, plus `wc -l` / `shasum`), `interpret_run.sh`, and the generation commit and prune. That is inferred from the code, not timed. It appears in no step metric and no `cost_surface` field. The baseline's per-year logs are not in git, so its tail can only be estimated (chain wall 9,355 s minus step wall 3,861 s).

**To do when the last CCP run has finished and the machine is idle (owner asked for these to be logged, not started).**
1. **Time the tail.** Run `write_canonical_evidence.sh` alone on one year's output (FY12 suggested: 19.4M merged rows) and time `wc -l`, `shasum`, each `awk` control, and `interpret_run.sh` separately. Falsifier: if the controls are less than about half of the 415 s FY12 tail, the cause is elsewhere (commit or prune). Awk controls stay in Bash (owner decision); this only measures them so orchestration overhead is identifiable, as AGENTS.md requires. Optional follow-up for the owner: stamp "pipeline complete" and "chain end" in the wall-clock CSV so the tail is recorded every run.
2. **Isolate the contention.** Run one chain alone (for example FY12 `3,20,21`, `--seedFromCommitted`) on the quiet machine and compare its CPU with run 3's FY12 (steps 3 / 20 / 21: 96 / 91 / 162 CPU s) and the baseline's (66 / 75 / 125). If the quiet CPU matches the baseline, the +15% is overlap, not a code or data change.
3. **Record the CPU caveat.** The audit register entry above does this; when the last run is archived, add its chain CPU and tail to the same comparison, and say whether it ran in the run-2 layout.

**Status effect (owner discussion).** Run 3 is an output replicate of the baseline (`LDGR*`, `PERM_SUMMARY*`, four views byte-identical; merged SJE, `GL`, `PERM_TBL` not compared). No change to CCP output status or AIM status; the AIM re-run is still pending. For cost it is a separate, differently labelled point (run-2 layout), not comparable to the baseline by `total_compute_s` or `total_cost_proxy`. Whether it replaces the baseline on the cost curve is the owner's call.

**AIM scope (owner).** The three-model experiment (train Posted-FineTune and IL-FineTune on the data and compare them with the natively built IL-Native models; `docs/RESEARCH_OBJECTIVES.md` §4) has not been run. Nothing in run 3 bears on it. The earlier AIM1/AIM3/AIM4 script outputs stay provisional.


**Not examined:** the earlier run `20261110_123303` (old step 20/21 definition, failed `5v`, no CPU data) was used only as context.


## 2026-11-11 (RUN 4 DONE) Last CCP run finished: exit 0, 42/42 controls PASS, 74 final data files byte-identical to the gold copy; evidence archived; next: idle-machine timing tests, then Track D (Bob, Mac)

**Result (run tag `20261111_064052`, start commit `9d54cd8`, header `chain_width=2 views_early=Y caffeinate=on`).** Orchestrator exit 0, 06:40:52 to 09:03:16. Corpus wall clock 8,544 s (run 3: 8,147 s, +4.9%); step CPU 11,207 s (run 3: 11,179 s, +0.3%); chain CPU (steps 3, 20, 21) 4,593 s (run 3: 4,506 s, +1.9%; baseline 3,913 s). Prep done at 6,388 s, `5v` 1,696 s. No machine sleep during the run (`caffeinate` held both assertions; `pmset -g log` shows none between 06:40 and 09:03). No `RETRY`, `WARNING`, `FAIL` or nonzero `rc` in any log. `permutation_capacity_sum` equals the `cost_surface` sum (503,118,463,033,110).
- **Compared with the gold copy:** the 74 data files in `gold/MANIFEST.csv` other than `cost_surface.csv` and `pipeline_results.log` (`LDGR*`, `GL*`, `PERM_TBL*`, `PERM_SUMMARY*`, `POQuarantineFY*`, four views) have identical SHA-256: 0 different, 0 missing. This now covers `GL*` and `PERM_TBL*`, which runs 3 and the baseline were not compared on. Merged SJE not compared.
- **The +15% chain CPU over the baseline repeats** (run 3 +15.2%, run 4 +17.4%), so it is a property of the run-2 layout on this 4-CPU machine, not a one-off. Isolating it is test 2 below.
- Conditions to remember when reading the wall clock: battery at 0% on AC ("not charging", 402 cycles, service recommended) for the whole run; swap 2.2 GB of 3 GB used mid-run; `XprotectService` and the IBM Bob helpers used some CPU. Not tested as causes of the 397 s wall difference from run 3.
- **Evidence:** `data/test_evidence/corpus_runs/run_20261111_064052/` (archived automatically, includes `corpus_totals.csv`, `quarantine_summary.csv`) plus `run_logs_20261111_064052.tar.gz` (29 run logs, launcher stdout, wall-clock CSV, exit marker, markers; archived as a tarball because `.gitignore` excludes `*.log`). Audit register entry "2026-11-11 Full corpus run 4". Raw logs stay in `~/workspace/va_fresh_logs/`. The baseline's and run 3's raw logs were archived the same way afterwards: `run_logs_20261110_210721.tar.gz` and `run_logs_20261111_014229.tar.gz` in their evidence folders (commit `c43c5b5`). No loose `.log` file is tracked in this repo; logs are kept as `run_logs_*.tar.gz` so they are not confused with handoff or evidence text.
- **Output:** `~/workspace/va_fresh_root/output_full/` (110 GB, run 4) and `output_full_run3_20261111_014229/` (110 GB, run 3). Both are kept until the timing tests below have run (test 2 needs a committed `FY12` tree to seed from); delete both after that, per the earlier plan. 213 GB free.
- **OneDrive:** started by a watcher at 09:03:40 when the run exited; `LedgerLabOutputs-Gold` shows 106 files locally (104 data files plus README/MANIFEST). That is local presence, not confirmed upload completion.

**Done after the run (same day).** Timing test 1 (FY12 tail): the `sje_balance` awk is about 396 s of a 449 s tail, so the controls are the tail (audit register "Timing test 1"). Timing test 2 (FY12 chain alone, quiet machine, Spotlight off, OneDrive and VS Code closed): CPU 267.87 s for steps 3, 20 and 21 against 266.85 s for the serial baseline (+0.4%), 349.29 s for run 3 (+30.9%) and 357.69 s for run 4 (+34.0%); outputs byte-identical to run 4. So the chain CPU premium of the run-2 layout is contention between lanes, not a code or data change (audit register "Timing test 2"). Track D input check: all 385 files in `va_control_root` match the manifest; `ENVIRONMENT.md` free disk updated. Gold copy: OneDrive upload complete (owner confirmed), 104 files match `MANIFEST.csv` locally.

**To do next.**
1. Owner: re-enable Spotlight with `sudo mdutil -i on /` (it was turned off for test 2).
2. DONE 2026-11-11 14:19: both 110 GB outputs (`output_full` run 4 and `output_full_run3_20261111_014229`) deleted at the owner's instruction after test 2, the SJE hashes (`gold/MANIFEST_SJE.csv`) and the gold verification; a new empty `output_full/` is ready. Free space 241 GiB to 458 GiB. The merged SJE can only be regenerated from the raw files with the pipeline scripts; its SHA-256 values are in `MANIFEST_SJE.csv`.
3. Reboot, then hand Track D to Codex (`~/workspace/va_track_d_handoff/`, own repo `ledger-lab-control` cloned at `~/workspace/sharealedger/ledger-lab-control`). Gate 0 in that repo uses file paths that do not exist in `va_control_root`; Codex needs the manifest, not the Gate 0 example.
4. Merge `aim-three-model` when the owner decides (worktree at `../ledger-lab-aim`, branch tracks `origin/aim-three-model`).

**Open.** Failure path (a failing year stops new years) not run; a designed symlink-swap test; merged SJE comparison; awk control totals remain in Bash (owner decision: they stay).



## 2026-11-11 (RUN 3 DONE) 14-year run with the run-2 layout finished: exit 0, 42/42 controls PASS, outputs identical to the baseline; evidence archived (Bob, Mac)

**Result (run tag `20261111_014229`, start commit `d500af4`, header `chain_width=2 views_early=Y caffeinate=on`).** Orchestrator exit 0. Corpus wall clock 8,147 s (2 h 15 m 47 s) against the baseline's 11,695 s (3 h 14 m 55 s), -30.3%; 8,102 s after subtracting a 45 s machine sleep (02:07:41 to 02:08:26; the true loss is 45 to 75 s because DarkWake began 02:07:11). Prep done at 6,191 s (baseline 5,710 s), `5v` 1,551 s (baseline 1,285 s), step CPU 11,179 s (baseline 10,670 s, +4.8%). More work per step, less total time. Compare by CPU seconds and total wall clock, not per-step `elapsed_s`.
- No `RETRY`, `WARNING` (including `step=21s`), `FAIL` or nonzero `rc` in any log. `permutation_capacity_sum` equals the `cost_surface` sum (503,118,463,033,110), so the FY03 baseline gap did not recur. The retry path itself was not triggered.
- `cksum` of `LDGR*` and `PERM_SUMMARY*` (14 years, 28 files) and the four view files are identical to the baseline. Not compared this time: merged SJE, `GL`, `PERM_TBL`.
- Chains for FY11 to FY14 committed while `5v` was reading (03:25:43 to 03:51:39); views identical, so no harm seen. This is not a designed test of the symlink swap.
- Peak RSS maxima come from different processes (step 21 1,351,610,368 at FY12; `step-ttl` 1,350,295,552).
- Evidence: `data/test_evidence/corpus_runs/run_20261111_014229/` (archived automatically; includes `corpus_totals.csv` and `sleep_windows_20261111_014229.csv`). Audit register entry "2026-11-11 Full corpus run 3". Output: `~/workspace/va_fresh_root/output_full_run3_20261111_014229/` (110 GB; renamed from `output_full` at 05:56 with its 15 absolute symlinks repointed, all 15 resolve). The baseline output `output_full_baseline_20261110_210721` was deleted at about 05:57 (owner chose this option); before deleting, run 3 matched it byte for byte on `LDGR*`, `PERM_SUMMARY*` and the four views (merged SJE, `GL`, `PERM_TBL` not compared), and no inodes were shared. The large outputs were not copied to OneDrive (too slow), but the small final files (104 files, 2.5 GB: `LDGR*`, `GL*`, `PERM_TBL*`, `PERM_SUMMARY*`, quarantine files, the four views, per-year `cost_surface.csv` and `pipeline_results.log`) were: **`~/Library/CloudStorage/OneDrive-Personal/LedgerLabOutputs-Gold/`, the standard reference copy** (`MANIFEST.csv` has size and SHA-256 for every file, all verified against the source; OneDrive upload completion was not confirmed). Its README, the manifest and the 15 per-year `cost_surface.csv` files and one tarball of the 15 `pipeline_results.log` files are in git at `data/test_evidence/corpus_runs/run_20261111_014229/gold/` (the 15 `pipeline_results.log` files are in `gold/run_logs_gold_20261111_014229.tar.gz`, because `.gitignore` excludes `*.log`). Run 3's output is the reference. Plan: after the last run, compare its small files with the gold copy, then delete both large outputs; Track D needs the machine idle, not the old output. A new empty `output_full/` is ready for the next run; 324 GB free.
- Misplaced log: the launch command redirected the launcher's stdout to `run2_corpus.log`, and the launcher then moved that file aside with the other stale `run2_*.log` files, so the run's own log is `~/workspace/va_fresh_logs/prev_run2_before_20261111_014229/run2_corpus.log`. Next launch: redirect to `launch_stdout.out`, not a `run2_*.log` name. The archive and evidence are unaffected.
- Sleep: `caffeinate` held both assertions and the Mac still slept once (cause not proven; the lid is the likely one). Keep the lid open or use an external display on long runs.

**Open.** Failure path (a failing year stops new years) not run; a designed symlink-swap test; a replicate of this layout; awk control totals remain in Bash (owner decision: they stay). One more full run is planned.


