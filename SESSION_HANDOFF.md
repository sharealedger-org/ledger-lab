# Ledger Lab Handoff

## 2026-11-13 14:54:05 AZT (TRACK D) Reflection before reporting-pass design

Owner requested reflection before design/build/execution. Appended timestamped entry to `tracks/D-control/CODEX_REFLECTIONS.md`: corrects my narrow AIM-test framing and premature Phase 2 freeze. Clarified sequence: three runs through FY2016 comparable to Phase 1 with revenue/fiscal-date changes; then one expanded reporting execution, measured separately. Reporting reads canonical SJEs AND posted balances, vendor/IP and available contract dimensions, and VA reference lookups. Vendor-cut outputs close at vendor boundary; larger cross-vendor cuts require bounded spills and explicit sort/merge/reduction. Exploration and refinement precede Phase 2. This supersedes earlier three-capability-run suggestions and the two-feed-only scope. No new design implementation or execution in this entry. Next: detailed design, build, execute, and a second reflection comparing actual results with this pre-design account.

## 2026-11-13 14:20:49 AZT (TRACK B1/B2) Intel handoff: AIM3 two-feed clarification; queue rehearsal passed

**Read this first on Intel:** [connected-run handoff](docs/CKB_REVENUE_RERUN_2026-11-13.md), especially its final section. Owner clarified that "movements and balances" means the prior combined AIM3 result. Implement **movement features from the canonical-SJE pass and balance features from the final reporting pass**, then compare movement-only/balance-only/combined on identical rows, periods, labels and splits. Not a new liquidity prediction task. Fiscal-period fix and exact feature definitions are explicit in the handoff. The two new Scala feature feeds are NOT implemented yet; vendor names still need real-master coverage and consumer linkage. Owner will continue on Intel after push and authorizes recovery of space from identified generated control outputs; no deletion here.

Queue wrapper completed two tiny complete runs automatically; both validated and final hashes matched. Evidence: `data/test_evidence/corpus_queue_20261113/`, with run archives `run_20261113_141607` and `run_20261113_141827`. Revenue mapping fix/launch options are committed in `bdeaad4`; no corpus-scale rerun. Current queue retains independent full SJE outputs: add explicit immutable-input reuse for reporting-only variants before assuming lower disk requirements. All new feature/profile code must be ready before freezing an unattended plan.

## 2026-11-13 14:14:19 AZT (TRACK B1/B2) Intel expanded-run preparation

Owner will launch on Intel after push; this Mac lacks raw files and disk space. Added explicit revenue/master selection and separate full-run output roots. Found and corrected APA revenue normalized-to-RC field ordering: previous balanced pairs could use fund key as amount. 13 Scala checks pass; connected synthetic run `run_20261113_141215` passed all existing engine controls through posting/GL/permutation and cross-year 5v. New metadata/artifact validator accepts it and rejects injected failed controls. Queue wrapper adds isolated run directories, space/revision checks, failed-run preservation, and same-configuration hash comparison; full wrapper test pending. See `docs/CKB_REVENUE_RERUN_2026-11-13.md` for exact boundaries and remaining Intel work. No control outputs deleted, no corpus launch, no claim that names reach AI inputs. Latest discussion proposes expanding 5v outputs in its existing canonical-SJE pass to establish incremental-cost evidence and the Phase 2 frozen foundation; view definitions/profile plan still need settling.

## 2026-11-13 13:36:01 AZT (TRACK B2) Training-language pilot prepared; execution permission declined

Owner authorized a field-label pilot. Design: `docs/TRAINING_LANGUAGE_PILOT.md`; code: `scripts/training_language_pilot.py`. Verified Gold survival-view hash and prepared 512/128/256 examples using latest monthly records without financial aggregation. Existing annual AIM3 baseline reproduced 0.7527; 43 preparation tests pass. Isolated model libraries installed under `/private/tmp/ckb-language-pilot-venv`; dependency audit clear. Download/training execution was declined, so no model scores or trained adapters exist; runtime implementation is unverified. Resume command and limitations are in the design. Owner subsequently suggested connecting the CKB passes and rerunning: discussion, not an implementation directive. Payment fiscal-period discrepancy remains unresolved; this pilot uses PO periods only.

## 2026-11-13 12:49:02 AZT (TRACK D) Interim paper plan and corrected reflection

Replaced the earlier revision brief with a concrete plan for circulation-ready Phase 1 Papers 1 and 2. Added reflection on the cross-domain pivot and foundation-before-Q2 sequence. At the owner's invitation abridged the mistaken-direction reflection and corrected its status/TODO/handoff consequences; no formal Track B closure is asserted. No papers drafted, source copies moved, experiments run, or control disclosures made. Next: evidence table and private-source drafting.

## 2026-11-13 07:34:14 AZT (TRACK D/B) Version 2 research direction

Correction recorded 2026-11-13: this entry originally mislabeled a suggested track organization as owner direction. The resulting documentation is corrected; no formal Track B closure was agreed. The revision plan now reflects the subsequent discussion: interim Phase 1 papers first, broader Phase 2 foundation before Q2. No experimental result was changed.

## 2026-11-13 07:13:47 AZT (TRACK D) Full monograph reading

Codex completed the owner-supplied introduction, Papers 1–9, epilogue, navigation guide, vocabulary, and README. Original analysis: `tracks/D-control/MONOGRAPH_READING_2026-11-13.md`; timestamped reflection and Track D status updated. Emphasis shifts toward validated discovery and cumulative cost under financial changes; incidental mathematical, scope, and evidence-version inconsistencies recorded. No private paper text copied, paper edits, control-side transfer, or engine/test changes. This is reading coverage, not external citation verification or implementation certification.

## 2026-11-12 22:37:06 AZT (TRACK B2/D) Paper 2 evidence assessment

Codex reviewed current AIM results, selected supplied Paper 2 passages, and the unrun three-model plan. Assessment: `tracks/D-control/PAPER2_ASSESSMENT_2026-11-12.md`; timestamped reflection added. Source discrepancy in balance/combined fiscal-year helpers is recorded in the audit register and TODO; the existing shared-core suite passed 28 tests. No new Python, model training, evidence rerun, or result edits. The proposed model gaps conflate inputs/model families and cannot yet isolate language prior. Follow-up belongs with Track B's ongoing evidence work; no control-side disclosure.

## 2026-11-12 22:01:26 AZT (TRACK D) CKB execution-model evaluation

Read the CKB execution model and traced `run_pipeline`/`LedgerApp` into legacy reclass and `5v`. Findings and Phase 2 implications are in `tracks/D-control/CKB_REVIEW_2026-11-12.md`; static source findings appended to the audit register. Reclass materializes/sorts/reposts rather than using the proposed delivery-group chain; its rewrite-before-post behavior needs a one-instrument expected-balance test. `5v` retains a completed-vendor set across groups. No code changed or runtime test run. Updated the timestamped Codex journal and provisional list; no control material sent or Track B evidence modified.

Updated 2026-11-13. This file is a dated log, newest entry first; use the linked design contracts for details.

- **Open work is in [`TODO.md`](TODO.md).** Add new to-dos there, not here.
- **Older entries** (run 3 preflight and everything before it, back to 2026-09-30) were moved unchanged to [`docs/archive/SESSION_HANDOFF_ARCHIVE_2026-11.md`](docs/archive/SESSION_HANDOFF_ARCHIVE_2026-11.md). Other documents that cite a `SESSION_HANDOFF.md` entry by name (for example "Corpus Work Assignment to Bob", "Plan for the next session", the 2026-11-06 entries) mean the archive.
- When this file passes about 500 lines, move all but the newest few entries to the archive in the same way.

## 2026-11-13 (TRACK B2, combined model) Commitment features add a small measured gain on the matched split

**Run.** `scripts/fit_vendor_survival_combined.py` (ARM venv, OneDrive Gold views). Same panel and split as the balance baseline; balance-only replicates 0.7683 with n_train 71,220 and n_test 52,448. Evidence: `data/test_evidence/AIM3_combined_model.csv`.

**Results (matched split, train FY2010–FY2013, test FY2014–FY2015).**
- Balance only: C = 0.7683.
- Combined (balance plus four view features: po_count, agency_count, distinct_nigp_classes, total_amount): **C = 0.7761**, permutation p = 0.001.
- Delta combined minus balance: **+0.0078**, bootstrap 95% interval [+0.0066, +0.0091] (resampling hold-out rows).
- Time-trend control: C = 0.4999 (FAIL).

**Reading.** The decision rule was declared in the script header before the run: sign positive and interval above zero. Both hold, so the script reports COMMITMENT_ADDS_SIGNAL. The effect is small (under one point of C), and it is the first result in this track that supports the Paper 2 hypothesis at all. It replaces the "no measured gain" reading in the balance-only entries.

**Caveats before anyone cites it.**
1. One split. The bootstrap resamples rows and does not vary training years or the fit itself, so the interval is narrower than the true uncertainty.
2. No multiple-comparison correction across the models run.
3. Linear model with four added features; no tuning was done, but the result has not been tested for stability across training windows.
4. Practical size is not established. A gain of 0.008 in C may not matter for the claim in Paper 2.

**Open (owner decisions).**
1. Whether Paper 2's AIM3 wording moves from "no measured gain" to "small measured gain, provisional".
2. Whether to run a stability check (leave-one-year-out or training-window bootstrap) before citing the number.
3. TDS evidence copies are not refreshed for this result yet.

## 2026-11-13 (TRACK B2) ARM rerun done; balance baseline corrected to a matched split (Bob, M1 Mac)

**Read first next time:** [`tracks/B2-models/STATUS.md`](tracks/B2-models/STATUS.md); audit register entry "2026-11-13 ARM rerun of AIM1, AIM3, AIM4".

**Done.**
- Gold inputs: 44 files used by the tests (LDGR, GL, PERM_TBL FY03-FY16, two views) match the Gold `MANIFEST.csv` by SHA-256. Survival view `174ef5c6…`.
- Ran AIM1, AIM3, AIM4 and the balance baseline on this ARM Mac (venv at `~/.venvs/ledger-lab-arm`, outside Dropbox; same package versions as Intel) with the new `scripts/run_aim_tests.sh`. Evidence: `data/test_evidence/aim_run_arm64_20261113/`. AIM1, AIM3 and AIM4 results are identical to the Intel CSVs (timestamps and paths aside).
- **Found and fixed a leak in the balance baseline** (owner approved the fix). The Intel run trained on FY2006-2014 and tested on FY2014-2015; FY2014 was in both. The script now uses the commitment model's split (train FY2010-2013, test FY2014-2015), reads the commitment reference from its CSV, and stops on overlap. **Matched result: balance C = 0.7683, commitment 0.7527, delta +0.0156** (was 0.7783 and +0.0256). The reading is unchanged in direction: no measured gain from the commitment view over posted totals.
- Updated: audit register, `TODO.md`, B2 `STATUS.md`, and `docs/tds-evidence/paper-2-rev1-ledger-lab/02-aim-test-suite-results.md`.

**Labels.** The handoff listed `docs/COMPANION_BASELINE_RECORD_2026-11-12.md` and `tracks/D-control/STATUS.md` as carrying the stale "2026-11-13 run" label for AIM; they do not (their 2026-11-13 dates are Track D events). Fixed in the register, `TODO.md` and the balance script.

**Open.**
1. **TDS Paper 2 text** still carries the unmatched wording (C = 0.7783, "splits not matched", local commit `5914ded`). It needs the matched figure 0.7683 (delta +0.016) and the withdrawal of 0.7783. TDS evidence copies are refreshed after owner review.
2. The old `AIM3_balance_baseline.csv` figures are in git history only; the file now holds the corrected run.
3. Combined balance-plus-view model against balance alone on the matched split (owner approval needed before the Python).
4. AIM4 locked STL, AIM2, D9, and the earlier deferred items (move the working copy out of Dropbox; hashes inside each script's own CSV).
5. All of this is uncommitted in the working tree.


## 2026-11-12 (TRACK D) Codex reflection journal and Phase 2 working list

**21:43:40 AZT update:** owner supplied monograph papers locally and prioritized reclass/rule changes. Selected Paper 5/6 reading informed candidate challenges P2-09–P2-14 and a timestamped reflection. Source exposure and hashes are logged; papers remain Git-ignored. No implementation or control-side disclosure. Next: narrow reclass contract trace, then a small scenario with temporal semantics and independent expected answers.

Owner directed Codex to formulate useful Phase 2 tests of adaptability, AI signal, and total maintenance/use cost, with incidental audit supporting that objective. Created `tracks/D-control/CODEX_REFLECTIONS.md` and `CODEX_PHASE2_WORKING_LIST.md`, maintained only by CKB-side Codex and not for transfer to the control. Targeted reading of the control journal at `ab0f7f0` is logged in the exposure record. Proposals are not finalized questions or verified findings. Concurrent Track B scripts/evidence were left untouched; no tests or engine runs were performed. Next work is recorded in the provisional list and linked from `TODO.md`.

## 2026-11-13 (TRACK B2, TDS session 87) Handoff: repo of record; ARM rerun first; labels and Paper 2 wording

**Decisions (owner, 2026-11-13).** This repo is the repo of record for all AIM results; TDS takes evidence from here. Next work is a Bob session in this repo on the ARM (M1) machine that reruns AIM1, AIM3 and AIM4 and the AIM3 balance baseline against the OneDrive Gold views. TDS evidence copies are refreshed only after that rerun. Deferred, not dropped: moving this working copy out of Dropbox, and recording SHA-256 hashes of the Gold inputs in each AIM output.

**Done this session.**
- AIM result labels now cite the CSV `run_timestamp` values (2026-11-12 20:37 and 20:43, Intel Mac), not "2026-11-13 re-run". Fixed in `docs/tds-evidence/paper-2-rev1-ledger-lab/02-aim-test-suite-results.md` and `tracks/B2-models/STATUS.md`.
- The AIM1 row in `tracks/B2-models/STATUS.md` is `partially_verified`, not `verified`.
- The AIM3 balance-baseline wording is calibrated: "no measured gain from the commitment view over posted totals on this unmatched comparison". The splits are not matched.
- Paper 2 (TDS) was updated to match; committed locally in TDS as `5914ded`, push pending.

**Still open (labels not yet fixed).** `docs/VERIFICATION_AUDIT_REGISTER.md`, `docs/COMPANION_BASELINE_RECORD_2026-11-12.md`, `tracks/D-control/STATUS.md`, `TODO.md`, and `scripts/fit_vendor_survival_balance.py` still use "2026-11-13" for the run. Fix them in the ARM session, after the rerun, so the new run's timestamps are the ones recorded.

**Next session, in order.**
1. Confirm the OneDrive Gold files are fully synced here (not placeholders); check `VIEWS/view_vendor_year_survival.csv` against `data/test_evidence/corpus_runs/run_20261111_014229/gold/MANIFEST.csv` (SHA-256 `174ef5c6…`, matched on this Mac at session end).
2. Run AIM1, AIM3, AIM4 and the balance baseline on this machine. Record run timestamps and input SHA-256 in the outputs.
3. Compare with the Intel CSVs. Record any differences in the audit register.
4. Refresh TDS evidence copies only after step 3.

**Ownership of the ARM rerun.** Bob session in this repo, not TDS.

## 2026-11-13 (TRACK B2) AIM tests run on Intel host; AIM3 balance baseline new result (Bob, Intel Mac)

**Read first in the next session:** [`STATUS.md`](STATUS.md), then [`tracks/B2-models/STATUS.md`](tracks/B2-models/STATUS.md).

**What was done this session (Intel Mac).**
- Installed Python ML packages: numpy 2.5.4, scikit-learn 1.9.1, scipy 1.18.1, statsmodels 0.15.0.
- **AIM3 re-run** (`scripts/fit_vendor_survival_windows.py`) on Gold final views: **PASS, C = 0.7527, p = 0.001**. Identical to the 2026-11-10 M1 result. Walk-forward 4-window mean C = 0.7629. Confirms reproducibility. Evidence: `data/test_evidence/AIM3_survival_windows.csv`.
- **AIM4 re-run** (`scripts/fit_ordering_seasonality.py`) on Gold monthly order volume: **FAIL, 2 of 5 top classes** significant at p < 0.05 (threshold ≥ 3). Q4-ratio test, not the locked STL. Same as the earlier result.  Evidence: `data/test_evidence/AIM4_seasonality_results.csv`.
- **AIM1 re-run** (`scripts/fit_noninjectivity.py`) across **all 14 years** on Gold: non-zero gap confirmed in every year. Sample: FY2012 gap -92,562; FY2016 gap -65,975. All 14 per-year files in `data/test_evidence/AIM1_all_years/`; consolidated: `data/test_evidence/AIM1_noninjectivity_all_years.csv`.
- **AIM3 balance-derived baseline** (new script `scripts/fit_vendor_survival_balance.py`): loaded all 14 LDGR files (10.65M rows). Balance model C = **0.7783** vs. commitment model 0.7527 (delta +0.026). Time-trend control C = 0.50 (FAIL, p = 0.518). **Key finding: the churn signal is already present in posted LDGR totals.** The commitment-pattern view does not add information above posted totals on this comparison. Training split differs (7 years for balance vs. 4 for commitment); a matched-split comparison is the open next step. Evidence: `data/test_evidence/AIM3_balance_baseline.csv`.

**Updated:** `tracks/B2-models/STATUS.md` (AIM table updated), `docs/VERIFICATION_AUDIT_REGISTER.md` (new 2026-11-13 entry), `docs/tds-evidence/paper-2-rev1-ledger-lab/02-aim-test-suite-results.md` (AIM1 all-years, balance baseline), `TODO.md`.

**Next steps:**
1. **Owner:** note the AIM3 balance-derived finding (the AIM3 hypothesis is `not_verified` pending a matched-split comparison). The Paper 2 AIM3 paragraph should be updated to cite the balance baseline result.
2. **AIM3 matched-split comparison:** re-run the balance baseline with the same FY2010–2013 training set as the commitment model, then compare C values on a level footing.
3. **AIM4 STL:** implement the locked STL + majority-of-agencies protocol (requires owner approval before writing per AGENTS.md, `statsmodels` is now installed).
4. Three-model experiment steps 5–7 remain blocked on D9 (owner decision on net income definition).

## 2026-11-12 (TRACK D) Documentation cleanup

Owner requested cleanup/archive of outdated documents. Preserved exact snapshots of the old comparison draft and verification protocol under `docs/archive/document_cleanup_2026-11-12/`; replaced the comparison at its stable path with current navigation and unresolved decisions. Removed historical implementation tables from the active protocol while retaining verification rules and unresolved gates; marked its old priority queue and the foundation README inventory/status as historical. Updated the docs map and corrected Track D's next Codex digest start to the existing `ab0f7f0` receipt. No control checkout read, financial code changed, evidence upgraded, or run artifacts deleted. Next: Q2 design and population/timing reconciliation; owner decisions remain open.

## 2026-11-12 (TRACK D) Codex orientation; incidental review only

Owner asked for familiarity with the experiment repository, primarily Track D, with inexpensive incidental audit. Read root/track status first, Track D comparison and baseline records, architecture gates and verification protocol, foundation README, CKB execution-model introduction, and the opening of Scala `post.scala`. HEAD was `0c05037`; existing documentation edits were preserved. No engine changes, tests, corpus runs, control-repository access, or implementation-status upgrades. The old comparison plan's pre-exposure sequence is already superseded by Track D's current direction; older README/protocol status text is navigation context, not fresh executable evidence. Next work is owner-led Q2 question/evaluation development and the outstanding population/timing reconciliation.

## 2026-11-12 (TRACK D) Handoff: control merge digested at `ab0f7f0`

**Verified in the adjacent checkout:** `main` and its `origin/main` remote-tracking ref point to `ab0f7f0`, a merge of `3c50c19` and `8edf787`. The checkout is clean, and no side-branch ref remains. The Q2 collaboration/isolation package previously inspected on the side branch is now on `main`; the merge adds no `src/` changes. The control handoff reports 54 tests passing; this session did not rerun them. Previously digested Phase 1 evidence was not re-read.

**Record:** the `3c50c19..ab0f7f0` digest is logged in `docs/COMPANION_BASELINE_RECORD_2026-11-12.md`, section 2. The next digest starts at `ab0f7f0`. No files or refs in the control repository were changed by this session.


## 2026-11-13 (TRACK D, merge) Handoff: control sharing and isolation package merged to the control's `main` (Bob, M1 Mac)

**Owner found that the agreed package was not on `main`.** It was one commit, `8edf787`, on a remote branch literally named `origin/baseline-knowledge-sharing` (hence `origin/origin/...`), forked from `7554427`. Its files were disjoint from the three Codex commits that moved `main` to `3c50c19`, except `SESSION_HANDOFF.md`, which merged without conflict.

**Done, in `ledger-lab-control`, by owner instruction.** Fetched; local `main` fast-forwarded to `3c50c19`; merged the branch with `--no-ff` as **`ab0f7f0`**; all 54 control tests passed on the merged tree; pushed `main` (`3c50c19..ab0f7f0`); deleted the stray remote branch and the local branch of the same name (`8edf787` is now an ancestor of `main`). The control checkout is on `main`, tracking `origin/main`, clean. The hook, rules, launcher and `AGENTS.md` are now part of `main`. No source, test or journal file was edited by hand; the merge touched no `src/` file.

**Not done.** Bob has not read the merged result beyond the file list and the tests. The live hook check in a fresh control-only Bob task, the memory smoke test and the Q2-draft questions are still open. The digest revision in `tracks/D-control/STATUS.md` stays `3c50c19`; the next digest `3c50c19..origin/main` will show the merge `ab0f7f0` and `8edf787` (content already known to Bob, apart from Codex Origin's later hook edits).


## 2026-11-12 (TRACK D) Handoff: Track D active; first Codex CKB access; control branch retained

**Owner clarification:** Track D is active. This session is Codex's first read access to the CKB for Phase 2 collaboration. The CKB documentation and the control branch inspection are recorded in `docs/COMPANION_BASELINE_RECORD_2026-11-12.md`, section 2.

**Control branch:** In the adjacent checkout, the `origin/main` remote-tracking ref remains at `3c50c19`; the separate `origin/origin/baseline-knowledge-sharing` ref is at `8edf787`. It contains a substantive 14-file, 1,058-insertion Q2 collaboration/isolation package, including safeguards, a launcher, and tests. It is not empty and was not deleted or otherwise changed.

**Boundary and next work:** No control files were changed and no Q2 questions or evaluation criteria were finalized. The Phase 1 population/timing comparison remains unreconciled. The earlier Part B plan's blind-before-CKB-exposure order no longer applies unchanged; revisit it with the owner before relying on it. This supersedes the older handoff sequence below that made CKB access conditional on a memory test; this session does not establish whether that test ran. This CKB collaboration session must not become Bob's separate control-implementation session.


## 2026-11-13 (TRACK B2) Handoff: Track B2 resumed for AIM tests; handoff to Intel host (Bob, M1 Mac)

**Read first in the next session:** [`STATUS.md`](STATUS.md), then [`tracks/B2-models/STATUS.md`](tracks/B2-models/STATUS.md).

**Summary:** Track B2 opened for AIM test suite execution (AIM1, AIM3, AIM4, AIM3 balance-derived baseline, and walk-forward verification). Checked environment on M1 Mac: Python ML packages (`numpy`, `scikit-learn`, `scipy`, `statsmodels`) are not installed in this environment, but are present on the Intel host machine. Located full 14-year Gold data and views at `/Users/kiptwitchell/Library/CloudStorage/OneDrive-Personal/LedgerLabOutputs-Gold/` (104/104 files present, including `VIEWS/view_vendor_year_survival.csv` and `VIEWS/view_monthly_order_volume.csv`).

**Next on Intel host:**
1. Execute `scripts/fit_vendor_survival_windows.py` with `--view` pointing to Gold `view_vendor_year_survival.csv`.
2. Implement and run the AIM3 balance-derived baseline (GL/LDGR vendor-year aggregates vs. commitment-pattern model, plus time-trend control).
3. Re-run AIM1 (`scripts/fit_noninjectivity.py`) across all 14 years and AIM4 (`scripts/fit_ordering_seasonality.py`) on Gold monthly order volume.

## 2026-11-13 (TRACK D, later) Handoff: control package agreed; launcher added; Bob's concurrence written (Bob, M1 Mac)

Owner agreed the control-side package, said the control runs on the Intel host, and chose a launcher script over a read-only mount. In `ledger-lab-control` (all uncommitted): rule-file header now credits Codex Origin's amendments; `scripts/run_control.sh` (resolves `@CORPUS@`/`@OUT@` from `allowed_roots.txt`; only `python3 scripts|src/*.py` and `bash scripts/*.sh`; no `..`; logs to the output root) with `tests/test_run_control.py`; rule file gained a launcher section; Bob's own `git fetch origin` and digest from `3c50c19` (no correction to Codex Origin's digest; logged as the third reading in the baseline record); Bob's concurrence entry at the top of the control `SESSION_HANDOFF.md`, which marks hook loading in the live client, memory isolation and the OS gap as NOT VERIFIED. All 54 control tests pass on this machine (the launcher's real `check` exits 66 here because the Intel paths do not exist, as expected). **Not done:** commit (owner), live hook check and memory smoke test (fresh control-only task on the Intel host), Q2-draft questions and evaluation criteria. This session still knows the experiment and does not become the control builder. Python and Bash edits were confined to the control repository.



## 2026-11-13 (TRACK D) Handoff: Phase 2 design discussed (agents swapped, change response); nothing started (Bob, M1 Mac)

**Done.** Discussion, a read of the control's uncommitted Phase 2 package (checkout at `7554427`; the digest from `3c50c19` was **not** run), and Bob's protections added to the control working tree, uncommitted (`.bob/settings.json`, `.bob/hooks/`, `.bob/rules/control-boundary.md`, one `.gitignore` line; 33 standalone probes passed; not yet verified in a live Bob session). No control source, test or journal file was changed, and Bob's concurrence entry is not written. Recorded the Phase 2 design in `tracks/D-control/STATUS.md` and the to-dos in `TODO.md` (Track D).

**Decided by the owner:** Bob builds and extends the control as a continuation of the existing control repository; Codex extends the CKB. Access stays one-way (CKB side reads the control, never the reverse). Questions are formulated on the CKB side during the build and given to the control afterwards; two gates on the CKB side, Q2-draft (initial questions identified) and Q2-final (finalised from what is learned, with the evaluation criteria; "Q2" = Phase 2, matching the control repository). Bob and Codex may both contribute to the next tests.

**Proposed, awaiting the owner:** record the reason for every Q2-draft to Q2-final change; record who proposed each question; define how gold is derived; cold-total and marginal numbers, a capabilities axis, Phase 1 reconciliation after every change, and a change log (the owner's measure: total processing of Phase 1 + Phase 2 together, every Phase 1 change noted; agent time stays out, per `AGENTS.md`); optional post-Q2 cold set for both builders; memory test for Bob and both Codex instances before the swap. Owner decisions (2026-11-13): no separate machine and no fresh clone, because the risk is judged modest; Bob builds on the existing control working copy; the evaluation criteria are part of the Q2 process. Bob's rule for that setup: open the control folder alone, never its parent or a multi-root workspace file (the workspace file in the parent lists the experiment repository as a root).

**Next.** Owner plan (2026-11-13): (1) test Codex's cross-session memory and how it can be controlled; (2) if satisfied, engage Codex in this repository to discuss the next-stage questions (log the exposure); (3) consult Bob again before Q2-draft is finalised. Also: the owner corrected the hardware record (both Phase 1 runs on Intel Macs; the M1 is only Bob's later sessions), fixed in `tracks/D-control/STATUS.md` and `TODO.md`; and the cost of a capability is its incremental cost (passes, input rows and bytes, time, memory, output, storage), not files read. Then fetch the control repo, run the digest, tag the control's Phase 1 end.


## 2026-11-13 (TRACK B2) Handoff: Paper 2 Dedicated Evidence Package Created & Ingested into TDS (Bob, M1 Mac)

**Read first in the next session:** [`STATUS.md`](STATUS.md), then [`tracks/B2-models/STATUS.md`](tracks/B2-models/STATUS.md).

**Delivered this session:**
- **Created TDS Paper 2 Evidence Folder (`docs/tds-evidence/paper-2-rev1-ledger-lab/`):**
  1. [`README.md`](docs/tds-evidence/paper-2-rev1-ledger-lab/README.md): Document inventory, status taxonomy (`verified`, `partially_verified`, `not run`), and model naming key (Model 1: Posted-FineTune, Model 2: IL-FineTune, Model 3: IL-Native).
  2. [`01-three-model-experiment-design.md`](docs/tds-evidence/paper-2-rev1-ledger-lab/01-three-model-experiment-design.md): Three-Model comparison setup, "See Spot Run" language prior trap, deterministic entity sampling (D8), longitudinal splits (D4), evaluation scoring framework, and decisions D1–D8.
  3. [`02-aim-test-suite-results.md`](docs/tds-evidence/paper-2-rev1-ledger-lab/02-aim-test-suite-results.md): Empirical test evidence: AIM1 non-injectivity proof (17,163 GL keys with multiple order patterns in FY2012), AIM3 vendor churn prediction ($C = 0.7527$, $p = 0.001$ on hold-out FY14–FY15), AIM5 agency disambiguation (Precision 1.0).
  4. [`03-gold-data-ground-truth-validation.md`](docs/tds-evidence/paper-2-rev1-ledger-lab/03-gold-data-ground-truth-validation.md): 14-year Gold reference dataset verification (104/104 files SHA-256 match, GL vs. LDGR exact-cent $0.00 debit tie-out).
  5. [`04-limits-and-phase2-roadmap.md`](docs/tds-evidence/paper-2-rev1-ledger-lab/04-limits-and-phase2-roadmap.md): Boundaries (audited expenditure basis, D9 open) and Phase 2 roadmap (Steps 5–7 model training).
- **Ingested into TDS Repository:** Copied to `/Users/kiptwitchell/Workspace/GHEGenevaERS/TDS-AI-Models/01-evidence/practitioner/Sharealedger/paper-2-rev1-ledger-lab/` and updated `sharealedger-evidence-summary.md` and `project/session-log.md` (Session 84).

## 2026-11-12 (TRACK B1, LATER) Handoff: Evidence folder corrected; four open model items (Bob, M1 Mac)

**Read first in the next session:** [`TODO.md`](TODO.md) (Cost graphs section), then [`docs/tds-evidence/paper-1-rev2-ledger-lab/RATE_SOURCES.md`](docs/tds-evidence/paper-1-rev2-ledger-lab/RATE_SOURCES.md).

**Delivered this session:**
- Renamed the workbook `03-charlie-anderson-model.xlsx` to `03-ckb-cost-model.xlsx` (`git mv`, staged). Every "Charlie Anderson" reference in the repo now reads "CKB Cost Model". The Charlie acknowledgement stays in the TDS paper.
- Fixed P5 in `02-mix-cost-curve-model.md` and `04-one-page-explainer.html`: the workbook gives $9,130.84 (not $9,018), about 560 times P3 (not 553). The sub-annual sentence was corrected: with zero sub-annual queries, the annual point (P2) costs $15.92; the monthly minimum holds only when sub-annual questions are asked.
- Determinism wording corrected in `01-measured-results-summary.md`, `CURRENT_RESULTS.md` and this file. The byte-identity result covers the two Monthly executions (`20261111_014229`, `20261111_064052`), checked by `gold/MANIFEST.csv` and `MANIFEST_SJE.csv`. It is **not** a named test. Named tests that cover determinism on fixtures: `CorpusProfileSpec`, `GLPostSpec`, `CKBViewsSpec`, and `scripts/test_three_model_verbalize.py` (`Determinism.test_same_input_same_output`).
- Unit rates: the repo had no source for them. Created [`RATE_SOURCES.md`](docs/tds-evidence/paper-1-rev2-ledger-lab/RATE_SOURCES.md), which lists each rate as `not retrieved`, with candidate AWS pricing pages and a recording format. No prices were retrieved (no web access this session). Until they are, the rates are assumptions, not published prices.
- VA data source confirmed from `scripts/download_corpus_zenodo.sh`: Zenodo parent `10.5281/zenodo.23846362` (eVA purchase orders FY2001–FY2018, budget files, metadata) and four APA records (`24015791`, `24021539`, `24022681`, `24022832`). Publishers: Virginia Department of General Services (eVA) and the Virginia Auditor of Public Accounts. The TDS paper's "Department of Accounts open data portal" wording is wrong and is being corrected there.

**Open model items found this session (do not treat as settled):**
1. **P6 needs a look.** The docs say $22.4M. The workbook's `Cost_Bowl_Data` computes $22,107,000.27 from a hardcoded 80,000,000 GB stored, and its own note (G7) still says $22.4M. The basis for 80,000,000 GB is not written down. The explainer label ("$22 million") was left as is until this is decided.
2. **Raw storage is charged in P2–P5.** Each of these points includes the full 56.69 GB raw SJE store in storage cost. Of P3's $16.29, $16.12 is that raw store; the Instrument Ledger itself costs about $0.47/year. P1 also stores raw. Whether raw storage belongs in the bowl is a modelling decision and must be made and documented.
3. **Full-corpus determinism has no named test.** The byte-identity check is a manifest comparison run by hand, not a test in the suite.
4. **Unit rates have no dated source.** The scan rate ($0.005/GB) and storage rate are assumptions. The conservative case (scan $0.005/GB, reconciliation $0) gives P1 $469, P2 $158, P3 $16.29, P4 $16.78, P5 $46; P3 stays the minimum and P4 is about 3% above P3. Pin the rates to dated list prices before the paper calls them published.

**Not changed:** workbook base rates, P6, and the explainer's P6 label. These wait for the owner.


## 2026-11-12 (TRACK B1) Handoff: Paper Revision 2 Evidence Folder Completed (Bob, M1 Mac)

**Read first in the next session:** [`STATUS.md`](STATUS.md), then [`tracks/B1-cost/STATUS.md`](tracks/B1-cost/STATUS.md).

**Delivered this session:**
- **Refreshed [`docs/CURRENT_RESULTS.md`](docs/CURRENT_RESULTS.md):** Updated baseline tag (`companion-measured-baseline-20261111` on `c3667e4`), incorporated Run 4 completed evidence, added full "Measured, Shows, Does Not Show" table per cost point (P1–P6, CCP1–CCP5), updated timing & contention diagnostics, and documented experiment limits.
- **Created TDS Paper Revision 2 Evidence Folder (`docs/tds-evidence/paper-1-rev2-ledger-lab/`):**
  1. [`README.md`](docs/tds-evidence/paper-1-rev2-ledger-lab/README.md): Document inventory, status taxonomy (`verified`, `partially_verified`, etc.), and 2026-11-12 naming key.
  2. [`01-measured-results-summary.md`](docs/tds-evidence/paper-1-rev2-ledger-lab/01-measured-results-summary.md): 14-year corpus metrics (224.07M SJE rows, 10.65M LDGR rows, 6.43M GL keys, 1.715 GB ledger store, 503.1T capacity bound, 3 Monthly runs, byte-identical deterministic outputs between the two Monthly corpus executions, checked by manifest, with named tests for fixtures).
  3. [`02-mix-cost-curve-model.md`](docs/tds-evidence/paper-1-rev2-ledger-lab/02-mix-cost-curve-model.md): Mathematical formulations, measured constants vs. assumed rates, cost coordinates across dial $X \in [0, 100]$.
  4. [`03-ckb-cost-model.xlsx`](docs/tds-evidence/paper-1-rev2-ledger-lab/03-ckb-cost-model.xlsx): 4-sheet interactive Excel workbook (`Model_Parameters`, `Cost_Equations`, `Cost_Bowl_Data`, `Multi_Substrate_Comparison`) built and validated with Office tools, 0 formula errors.
  5. [`04-one-page-explainer.html`](docs/tds-evidence/paper-1-rev2-ledger-lab/04-one-page-explainer.html): Browser-ready one-page visual explainer updated with standard naming conventions.
  6. [`05-limits-and-phase2-roadmap.md`](docs/tds-evidence/paper-1-rev2-ledger-lab/05-limits-and-phase2-roadmap.md): Scope limits (no revenue in gold, unmeasured annual/cubes runs, 54% Bash tail) and Phase 2 milestone plan.
- Updated [`tracks/B1-cost/STATUS.md`](tracks/B1-cost/STATUS.md) and [`TODO.md`](TODO.md).

## 2026-11-12 (END OF SESSION, TRACKS B1 + D + INFRASTRUCTURE) Handoff: paper revision 2 evidence folder is next; Track D paused (Bob, M1 Mac)

**Read first in the next session:** [`STATUS.md`](STATUS.md), then [`tracks/B1-cost/STATUS.md`](tracks/B1-cost/STATUS.md). Track D is paused; its page carries a resume checklist.

**What happened this session (all committed locally on `main`; nothing pushed, 30 commits ahead of `origin/main` including this entry).**
- Tagged the companion baseline `companion-measured-baseline-20261111` on `c3667e4`, a merge of `origin/main` (`727f2e9`) and the never-pushed local commit `0ff9255` (`CURRENT_RESULTS.md`). Record, exposure log and comparability items: [`docs/COMPANION_BASELINE_RECORD_2026-11-12.md`](docs/COMPANION_BASELINE_RECORD_2026-11-12.md). **Correction made:** the companion runs were on an Intel Mac (4 CPUs, 16 GB), the same class as the control host, per the owner; the run evidence records only "4-CPU, 16 GB", not the CPU model. An earlier line saying "Apple Silicon M1" was wrong and is fixed.
- Merged `aim-three-model` into `main` (`d356143`); the Dropbox folder is on `main`; the second worktree is gone. The local `aim-three-model` branch is now redundant (delete after pushing).
- Reorganised: root [`STATUS.md`](STATUS.md), [`tracks/`](tracks) pages for B1, B2, C, D, [`docs/README.md`](docs/README.md) document map, `TODO.md` grouped by track. Track A has no page here (protected data; results return in synthesised form).
- **Track D result digested** (control repo `3c50c19`): Codex's official attempt passed (implementation `892a068`). Read twice, logged, nothing incorporated. Facts and the CPU table are in [`tracks/D-control/STATUS.md`](tracks/D-control/STATUS.md) and [`tracks/D-control/COMPARISON_PLAN.md`](tracks/D-control/COMPARISON_PLAN.md). On CPU seconds the control used about 1.6 times the companion's, with 8 times less memory and 0.65 cores busy on average; scopes differ (revenue, six full reports, no Bash control passes counted in the companion), so no efficiency conclusion is drawn yet.
- **Track D paused by the owner** while the paper is revised.

**Owner decisions made this session (do not re-ask).**
1. Tracks are folders on `main`, not branches. Track A needs no page.
2. Paper 1 is replaced by an updated version (**revision 2**), keeping earlier versions. Papers are edited in TDS, `/Users/kiptwitchell/Workspace/GHEGenevaERS/TDS-AI-Models` (behind the IBM firewall, with the source documents). This repository supplies an **evidence folder** for TDS's open tier, `01-evidence/practitioner/Sharealedger/` (convention: `<name>-evidence-summary.md`). **Copy nothing from TDS into this public repository.**
3. Revision 2 is written **without the control's influence** (the control result will shape a later revision, with Track D). Blindness matters for the control, not for the CKB experiment; the CKB should adapt to new information, and the two are not in competition. Informed changes are still logged.
4. Part B (novel-permutation challenge) is **deferred**; its questions stay out of anything the control can see. Idea for later: give one Codex instance **read access to CKB** (not write access to the control) to help formulate the questions and reduce AI-engine bias. Also on resume: ask Codex Development to preserve the control's 183.5 GB database.
5. The control has seen Papers 1 and 2 only (Gate 1). The monograph introduction (`00-…`) was withheld on purpose.
6. **Names.** Stop numbering runs ("run 3", "run 4" collide with "Run A/B" on the graph). Name the points on the graphs. Prefer the CCP **names** in prose; keep the CCP numbers as tags for finding material.
   - Cost-curve (Mix) points: **Events** (P1), **Annual** (P2, Run A), **Monthly** (P3, Run B, the bowl minimum), **Cubes** (P4), **Posted** (P5, GL plus sub-ledgers), **Permuted** (P6).
   - Process points: **Posted GL** (CCP1), **Ledger** (CCP2), **Views** (CCP3), **Revaluation** (CCP4), **Allocation** (CCP5).
   - The three complete corpus runs are three measurements at the **Monthly** point (years in series once; two chains twice). The early incomplete or failed runs are left out of the paper and the methods say why. Run tags remain only in evidence paths, with one mapping table.

**Next work: build the revision 2 evidence folder** (staging path proposed: `docs/tds-evidence/paper-1-rev2-ledger-lab/`; the owner copies it into TDS). Not polished; each document carries a status. Planned contents:
1. `README` index: what each document is, status words (`verified`, `partially_verified`, `owner-reported`, `inferred`, `not run`), the naming key (graph name, CCP name, CCP tag, run tags).
2. Measured results summary: Monthly point (three runs, CPU seconds and wall, 10,651,908 ledger rows, 224,072,410 SJE rows, identical outputs), Posted GL (6,433,714 keys), Views, permutation capacity as an upper bound (503,118,463,033,110). Revaluation and Allocation marked "fixture only, not run at corpus scale".
3. The **Mix curve**, labelled a model with assumed rates and measured constants (`docs/COST_GRAPH_DESIGN.md`, "Mix graph: model run").
4. A **spreadsheet with formulas** for the CKB Cost Model (four sheets specified in `COST_GRAPH_DESIGN.md`: `Model_Parameters`, `Cost_Equations`, `Cost_Bowl_Data`, `Multi_Substrate_Comparison`). Build with the Office tools (the `office-insights` skill), no Python. The multi-substrate sheet holds only the measured CKB curve until the control is added in a later revision.
5. The **one-pager** for human readers: `docs/LEDGER_LAB_ONE_PAGE_EXPLAINER.html`. Its open items are in `TODO.md` (one outside-reader name for the payments subledger, footer addresses, browser check of the graph, review of the assumed rates).
6. A refreshed `CURRENT_RESULTS.md` (still says "as of `9d54cd8`, a final run in progress") and a limits section: no revenue, no replicates per point beyond the three Monthly measurements, Bash control passes outside the step metrics (about 54% of chain wall time), Annual and Cubes not measured, hardware recorded only as "4-CPU, 16 GB".
7. **Phase 2** list: the future points (Annual, Cubes, Revaluation and Allocation at corpus scale, revenue, replicates, then Track D and Part B).
8. **Leave out** all control results and the Part B questions in revision 2.

**Done after this entry (2026-11-12, owner asked):** `main` and the baseline tag pushed (`main` at `ef78576`); `aim-three-model` deleted locally and on the remote; `AGENTS.md` now says to read `STATUS.md` first. **Still open for the owner.** Pointer in the control's `SESSION_HANDOFF.md`, written when the repository is opened to Codex: repository name, tag, and the SHA of the commit holding the results evaluation of the control's work, so Codex sees the assessment of its own work. Three-model work (B2) stays on hold; D9 is open. `sudo mdutil -i on /` still pending.

**Housekeeping notes.** Temporary clones `/tmp/llc_remote` and `/tmp/llc_verify` can be deleted. One `python3` call with an empty script ran by mistake in a shell command (no Python was written); all other Python use was the control's own existing tests, run in `/tmp`. Slow Dropbox-wide searches time out; search `ledger-lab` or a named folder instead.


## 2026-11-12 (INFRASTRUCTURE) Session 1 of the reorganisation: root status page, track folders, document map (Bob, M1 Mac)

**Owner decisions (2026-11-12).** Tracks are folders on `main`, not long-lived branches. Track A (protected P&C data) gets no status page here; it progresses in another repository and returns results in synthesised form. Work is organised under B1 (Paper 1 cost), B2 (Paper 2 models, on hold) and D (control), with C parked.

**Created.** [`STATUS.md`](STATUS.md) (read first), [`tracks/B1-cost/`](tracks/B1-cost/STATUS.md), [`tracks/B2-models/`](tracks/B2-models/STATUS.md), [`tracks/C-berka/`](tracks/C-berka/STATUS.md), [`tracks/D-control/`](tracks/D-control/STATUS.md) (each one page), [`docs/README.md`](docs/README.md) (every file in `docs/` sorted by track and purpose). `TODO.md` headings now name their track, with new Track D and refresh/graphics items. Track D page records the digest procedure and the last control revision read, `51f16a6`.

**Not done, needs the owner.** Push `main` and the tag (22 local commits, nothing pushed). Pointer text in the control's handoff. A line in `AGENTS.md` pointing to `STATUS.md` (not edited without approval). The older handoff entries were already archived on 2026-11-11, so no further split was made: this file is about 180 lines.

**Next.** Refresh `docs/CURRENT_RESULTS.md` and write the one-page results (B1), then the three graph sheets, then the Track D comparison plan. Entries from now on carry a track id in the heading.


## 2026-11-12 (BRANCH `main`) Companion baseline tagged and recorded for the control comparison; Codex Track D progress read (Bob, M1 Mac)

**Done.** Followed the request in `ledger-lab-control` (`docs/EXPERIMENT_BASELINE_AND_KNOWLEDGE_SHARING.md`, `7554427`). Local `main` had diverged from `origin/main` (ahead 1, behind 9): `0ff9255` (`docs/CURRENT_RESULTS.md`, the run-result assessment) had never been pushed, and `origin/main` (`727f2e9`) lacked it. Merged them cleanly into **`c3667e4`** (tree = `727f2e9` + `README.md` + `CURRENT_RESULTS.md`; no engine or script change since run 4 started at `9d54cd8`). Annotated tag **`companion-measured-baseline-20261111`** on it. The older `ccp-baseline-20261111` (`63e7900`, code preflight) is unchanged. Record: [`docs/COMPANION_BASELINE_RECORD_2026-11-12.md`](docs/COMPANION_BASELINE_RECORD_2026-11-12.md) (measured runs and start commits, where the evidence is, chronology, exposure log, comparability items).

**Not pushed.** The merge, the tag and the record are local. Pushing and the wording of a pointer in the control's handoff are the owner's call.

**Owner direction (2026-11-12).** Codex need not be told anything about the results now. The control's handoff needs only a pointer to this repository (name, tag, commit) so that, when Codex is given access for the informed phase, it can assess against this baseline. Codex reading `ledger-lab` is not a concern; the control's journal shows a cooperative, not adversarial, role.

**Control progress (read-only; Codex Development's host is not reachable from here).** At `51f16a6` the fresh official attempt (`892a068`, output on Intel's host) was RUNNING; no accepted result exists. Its own 35 tests, 7 controller tests under `python3 -O`, and the six-output fixture check pass in a throw-away copy. Preflights count 130,861,904 accepted events (97,516,306 expenditure, 17,750,109 revenue, 15,595,489 PO). Nothing measured to compare yet. Exposure logged in the record, section 2: confirmed reading only, nothing incorporated.

**Open (owner).** Push the tag and `main`; decide the pointer text and branch for the control handoff; when to open the companion to Codex. `aim-three-model` was merged into `main` the same day (no code conflicts; only this file conflicted); three-model work is on hold during the reorganisation in [`docs/PROJECT_ORGANIZATION_PLAN.md`](docs/PROJECT_ORGANIZATION_PLAN.md).

**Python note.** No Python written. Ran the control's existing tests and fixture check in `/tmp`.


## 2026-11-12 (BRANCH `aim-three-model`) Statement tie-out idea recorded; Virginia audited and public data surveyed; D9 reopened (Bob, M1 Mac)

**Owner direction.** Audited financial statements are the gold standard. A subledger query by PO or vendor should tie to a statement number, with vendor totals plus an "other" built from named adjustment records, never a computed plug. This is a candidate later paper test: `docs/THREE_MODEL_EXPERIMENT_PLAN.md` section 8.

**Found.** The raw corpus has `apa_revenue` (17.75M rows) and `apa_budget` (116,782 rows); only expenditure and PO reached Gold, so "no revenue" is a Gold limit. The Department of Accounts publishes the ACFR for FY2002 to FY2025. FY2012 sections G and L were read: statements by fund, and an itemised original-to-final budget walk with a cash-basis to GAAP fund balance reconciliation. The same General Fund shows 1,683,412 on the budget basis and 512,359 on the GAAP basis ($ thousands), so any reserves target needs a named basis. Other sources (USAspending, Taxation, APA local comparative report, Data Point salaries, DPB chart of accounts, eVA 2016 to 2027) are in `docs/VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md`, each marked Read, Probed or Listed.

**Not found / limits.** Raw `VARawFiles` (including the budget files) are not on the M1 Mac. Corpus FY2012 expenditure and revenue sums are about $12B above the ACFR governmental-funds totals; the reason is not established. The sums come from `file_profile.csv` with `awk` and were not cross-checked. No source file was saved in the repository.

**Open.** Moved to [`TODO.md`](TODO.md): D9 basis decision, anchor figure, location of the raw files, FY2012 General Fund mapping, USAspending comparison, one-year revenue run (architecture-gate declaration first).

**Python note.** One short inline `python3` snippet listed links from a downloaded HTML page before owner approval; read-only, nothing saved, nothing in the measured path.



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


