# Companion baseline record and exposure log

Written 2026-11-12 by Bob (M1 Mac, UTC-07:00), in answer to Codex Origin's request in `ledger-lab-control`, `docs/EXPERIMENT_BASELINE_AND_KNOWLEDGE_SHARING.md` (commit `7554427`, section "Requests to Bob and Development"). This file belongs to the companion repository. It does not speak for Codex Origin or Codex Development.

Status words follow [`CURRENT_RESULTS.md`](CURRENT_RESULTS.md): `verified` means a named check was run and passed; `owner-reported` means the owner said so and no artifact establishes it.

## 1. The preserved baseline

Annotated tag **`companion-measured-baseline-20261111`** on commit **`c3667e4`** (local, **not pushed** when written; the owner decides when to push).

`c3667e4` is a merge, made 2026-11-12, of the two `main` lines that existed before Codex Development (Intel) made its first commit in the control repository (`cbbc03c`, 2026-11-11 14:56 -0700):

| Parent | Time (-0700) | Content |
|---|---|---|
| `727f2e9` (`origin/main`) | 2026-11-11 14:19 | Runs 3 and 4, timing tests 1 and 2, disk measures, merged-SJE hashes, the audit register entries |
| `0ff9255` (local `main`, never pushed before the merge) | 2026-11-11 07:34 | `docs/CURRENT_RESULTS.md`, the run-result assessment, and its README link |

Why a merge: `origin/main` did not contain `CURRENT_RESULTS.md`, so tagging `727f2e9` alone would have left out the assessment. `git diff 727f2e9 c3667e4` is exactly `README.md` and `docs/CURRENT_RESULTS.md` (`verified`).

**Measured code.** `git diff 9d54cd8 c3667e4`, excluding `docs`, `data`, `SESSION_HANDOFF.md` and `README.md`, is empty: no engine or script changed between the start of run 4 and the tag (`verified`). Scripts did change between the baseline run's start (`70b6781`) and run 3's start (`d500af4`): `run_full_corpus.sh`, `run_pipeline.sh`, `dashboard_full_corpus.sh`, `archive_corpus_run_evidence.sh`, and the new `corpus_totals.py` (the run-2 layout and evidence archiving) (`verified` by `git diff --stat`).

**Earlier tag kept.** `ccp-baseline-20261111` (`63e7900`, pushed) is the code preflight before any full run. It is not the measured state and is not moved.

### Measured runs

| Run tag | Start commit | Layout | Wall s | Step CPU s | Result |
|---|---|---|---|---|---|
| `20261110_210721` | `70b6781` | Years in series, `5v` last | 11,695 | 10,670 | Exit 0, 42 of 42 controls PASS |
| `20261111_014229` | `d500af4` | Two chains, early `5v` | 8,147 | 11,179 | Exit 0, 42 of 42 PASS; `LDGR*`, `PERM_SUMMARY*`, four views identical to the baseline |
| `20261111_064052` | `9d54cd8` | Same as run 3 | 8,544 | 11,207 | Exit 0, 42 of 42 PASS; 74 final data files byte-identical to the gold copy |

Also measured: timing test 1 (FY12 post-pipeline tail), timing test 2 (FY12 chain alone: 267.87 CPU s against 266.85 serial), disk measures, merged-SJE SHA-256 list for runs 3 and 4 (14 of 14 identical). Runs 3 and 4 are replicates of each other, not of the baseline. Nothing is replicated three times, so no cost is final.

### Where the results and analysis are

| What | Where |
|---|---|
| Per-run evidence | `data/test_evidence/corpus_runs/run_20261110_210721/`, `run_20261111_014229/`, `run_20261111_064052/` (cost surfaces, process metrics, wall-clock, `corpus_totals.csv`, logs as `run_logs_*.tar.gz`) |
| Gold reference outputs | `data/test_evidence/corpus_runs/run_20261111_014229/gold/` (`MANIFEST.csv`, 104 files, SHA-256; `MANIFEST_SJE.csv`). The data files are on OneDrive `LedgerLabOutputs-Gold`, outside git. |
| Analysis and status per claim | [`VERIFICATION_AUDIT_REGISTER.md`](VERIFICATION_AUDIT_REGISTER.md), entries from "Full corpus run FY2003-FY2016 (`20261110_210721`)" to "Merged SJE hash comparison, run 3 against run 4"; [`CURRENT_RESULTS.md`](CURRENT_RESULTS.md) |
| Prediction made before any control measurement | [`TRACK_D_NOTES.md`](TRACK_D_NOTES.md), section 2 (commit `7981964`, 2026-11-10 22:16): the 5 to 15 times ingestion penalty and the shape of the Curve 3 bowl are projections, not measurements |

Not on `main`: `docs/EXPERIMENT_STATE_2026-11-11.md` and the cost-graph model run exist only on branch `aim-three-model`, committed after Intel began (`e17da96`). They are later analysis of the same measurements, not part of the baseline.

### Execution and input conditions

- **Host:** an Intel Mac with 4 CPUs and 16 GB RAM, the same class of machine as the control's host, per the owner (2026-11-12). The run evidence records "4-CPU, 16 GB" but not the CPU model. (An earlier version of this line said Apple Silicon M1; that was wrong. The M1 is only the machine Bob's later sessions ran on.) JVM heap 1 GB, a Scala process topology orchestrated by Bash. Run tag, start commit and header (`chain_width`, `views_early`, `caffeinate`) are in each register entry.
- **Input:** FY2003 to FY2016, expenditure and purchase orders. 385 raw files, 27.8 GB in the Zenodo archive; 224,072,410 SJE rows; 10,651,908 ledger rows; 6,433,714 GL keys (`partially_verified`, from `corpus_totals.csv`). **Revenue was not processed.** The raw corpus has `apa_revenue` (17.75M rows), but only expenditure and purchase orders reached Gold.
- **Launch commands:** in the handoff entry for each run and in `docs/FULL_CORPUS_RUN_CHECKLIST.md`, not collected in one place here.
- **Scope timed:** the Scala pipeline from raw files to the four views. The Bash control passes sit outside every step metric (about 54% of chain wall time in run 3, `inferred` from the code; timing test 1 put 396 s of a 449 s FY12 tail in the `sje_balance` awk).

### Chronology: what is established

- **Owner-reported:** measurements and analysis were complete before Intel development began on 2026-11-11.
- **Established by commit times (-0700):** the last measurement or assessment commit on either `main` line is `727f2e9` (14:19); Intel's first control commit is `cbbc03c` (14:56). Run 4 was archived at `262b976` (09:23), timing tests at 11:16, disk measures at 11:26, SJE hashes at 14:12.
- **Not established by commit times:** that no later measurement happened. A commit time shows when evidence was recorded, not when it was produced. The runs carry their own timestamps in their evidence folders.
- **Earlier contact, for the reader to weigh:** Bob authored the control's Gates 0 to 2, answered Codex's audits, and audited the Gate 3 Arm B prototype (`c3ec922`) on 2026-11-10 to 11, so the companion's author had seen the control's design before runs 3 and 4. The run-2 orchestration change (`2fac4c1`, chain pool and early `5v`) was committed 2026-11-11 00:57, after that audit. Nothing in the record shows the control influenced it, and nothing shows it did not.

## 2. Exposure log (control material read by the companion)

Delivery, confirmed reading and incorporation are different events.

| Field | Entry |
|---|---|
| When | 2026-11-12, about 12:00 to 12:30 UTC (05:00 to 05:30 -0700) |
| Sender and recipient | The owner made the checkout available through the shared Dropbox folder. Recipient: Bob (Agent mode, M1 Mac, this session). |
| Source revision | Local checkout `ledger-lab-control` at `7554427` (branch `baseline-knowledge-sharing`), and GitHub `origin/main` `51f16a6`, read from a temporary clone in `/tmp`, not through the synced checkout |
| Read | `README.md`; `SESSION_HANDOFF.md` lines 1 to 140 and 520 to 636; `CODEX_REFLECTIONS.md` lines 150 to 156; `docs/ACTIVE_WORK_20261111.md`; `docs/EXPERIMENT_BASELINE_AND_KNOWLEDGE_SHARING.md`; the `51f16a6` diff. Event type: **confirmed reading**. |
| Executed | The control's own 35 tests, its 7 controller tests under `python3 -O`, and `scripts/verify_fixture.py`, in a throw-away `git archive` copy in `/tmp`. All passed. The synced checkout was not modified and no control code was written. |
| Not read | `src/ledger_control.py` and the other scripts; the compact JSON evidence; handoff lines 140 to 520 (the earlier Bob and Codex exchange); the live official-run output, which is on Intel's host and not reachable from here |
| Purpose | Oversight, as the owner intended: assess progress and record the baseline |
| Incorporation | **None.** No companion commit, decision or number changed because of this reading. |
| Earlier exposure | Not established by this record; see the chronology note above. Whether another Bob session read Intel-phase material before today cannot be determined from this session. |

### Second reading: completed official control result

| Field | Entry |
|---|---|
| When | 2026-11-12, about 19:40 to 19:50 UTC (12:40 to 12:50 -0700) |
| Sender and recipient | Codex Development (Intel) published the entries "Information-only progress report to AI Bob" (08:07 AZT) and "Completed official result" (12:25 PM AZT), stating that publication does not establish receipt. Recipient: Bob, via the owner's prompt in this session. |
| Source revision | GitHub `origin/main` `3c50c19`, read from a temporary clone in `/tmp` |
| Read | The two new `SESSION_HANDOFF.md` entries, the new `CODEX_REFLECTIONS.md` entry, the `docs/ACTIVE_WORK_20261111.md` change, `docs/OFFICIAL_MEASUREMENT_20261112/README.md`; later in the session, `review.json` (phase metrics), `plot_data.json`, `query_summary.csv`, the first part of `query_metadata.json`, and the host lines of `status.json`. Event type: **confirmed reading**. |
| Not read | `final_validation.json`, `disk_measures.txt`, the plots, and all code |
| Incorporation | **None.** No companion commit, decision or number changed. |

### Third reading: fetch and digest from 3c50c19 (Bob's own receipt)

| Field | Entry |
|---|---|
| When | 2026-11-13, about 03:00 to 03:30 UTC (2026-11-12 evening -0700) |
| Sender and recipient | Already published on `origin/main`; Codex Origin's digest of the same revision sits in the control handoff (uncommitted). Recipient: Bob, via the owner's prompt in this session. Bob's receipt is independent of Codex Origin's fetch. |
| Source revision | `git fetch origin` in the owner's `ledger-lab-control` checkout (HEAD stays `7554427`; no merge), reading `origin/main` `3c50c19` with `git show` |
| Read | `docs/OFFICIAL_MEASUREMENT_20261112/` `README.md`, `final_validation.json`, `disk_measures.txt`, `query_summary.csv`, `status.json`; the control's working-tree handoff entries by Codex Origin (digest and remaining steps) and its isolation package |
| Not read | Plots, `review.json` (read in the second reading), code |
| Result | Figures match the Track D page and Codex Origin's digest; no correction. New detail: `status.json` lists `partial_po_years` 2002 (137) and 2009 (3,904). |
| Incorporation | **None.** No companion commit, decision or number changed. |
| Written to the control | By the owner's decision, Bob wrote only Bob-side configuration (`.bob/`, one `.gitignore` line, `scripts/run_control.sh`, `tests/test_run_control.py`) and one concurrence entry in the control `SESSION_HANDOFF.md`. All uncommitted; the commit is the owner's step. |


### Informed changes after the baseline

None yet. Any companion change that follows from a control finding must be listed here with its commit, the control entry it came from (author, date, revision), and a statement that the baseline above was left unchanged.

## 3. Comparability items noted while reading (not resolved)

Facts from the two records side by side. None is a conclusion.

- **Streams.** The control accepts expenditure, revenue and purchase orders: 97,516,306 + 17,750,109 + 15,595,489 = 130,861,904 events (Codex Development's final preflights). The companion processed expenditure and purchase orders only.
- **Purchase order recovery.** The companion quarantines about 32,000 PO rows as malformed (`CURRENT_RESULTS.md`). The control reports 7,551 PO candidates quarantined after quote-aware recovery, which accepted 5,368 additional candidates in FY2012. Whether the companion's quarantined rows are the ones the control recovers has not been checked.
- **Subcent revenue.** The control found 43,599 subcent revenue records. Revenue was not run in the companion, so the question does not arise there yet.
- **Years.** Companion: FY2003 to FY2016. Control: 349 benchmark files, FY2009 PO absent, partial years kept without being declared complete. The populations have not been reconciled.
- **What is timed.** The control's official measurement starts at ingestion; development is not measured. The companion's cost points start at the Scala pipeline on files already present, with the Bash control passes outside the step metrics.
- **Machines.** Same class: both on Intel Macs with 16 GB and four logical threads (owner, 2026-11-12). The control host has two physical cores. The companion runs do not record the CPU model, so the match rests on the owner's statement.
- **Control status.** At `51f16a6` the control's fresh official attempt (`892a068`) was RUNNING. No accepted result existed, so no comparison of results is made.

## 4. Open (owner decides)

1. Push the tag `companion-measured-baseline-20261111` and the `main` commits (the merge `c3667e4` and this record).
2. Whether a pointer entry goes into the control's `SESSION_HANDOFF.md` (repository name, tag, commit only, no results), and on which branch.
3. When the companion is opened to Codex for the informed phase.
4. Reconcile the PO quarantine sets and the year populations before any whole-run comparison.
