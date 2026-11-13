# Full Corpus Run Checklist

Working checklist for a full FY2003-FY2016 corpus run on the Zenodo-restored data root. Written
2026-11-10 from the first attempt. Items marked `[x]` were done and observed in that session;
items marked `[ ]` are pending or unverified. Nothing here has been promoted to `verified` in
[VERIFICATION_AUDIT_REGISTER.md](VERIFICATION_AUDIT_REGISTER.md).

Policy for this run: **stop at the first failure, fix the cause, rerun, and repeat until a run is
clean.** Do not skip a failing year and do not continue past a nonzero exit.

Related: [CORPUS_ACCESS.md](CORPUS_ACCESS.md), [RUNTIME_DATA_LAYOUT.md](RUNTIME_DATA_LAYOUT.md),
[TEST_DESIGN.md](TEST_DESIGN.md) section 7 (staged sequence), [AGENTIC_ARCHITECTURE_GATES.md](AGENTIC_ARCHITECTURE_GATES.md).

## 0. Boundaries

- [x] Download and checksum verification are data preparation. Their time and storage are **not**
      measured and must not enter `cost_surface.csv`.
- [x] Orchestration stays in Bash (`scripts/run_pipeline.sh`). All parsing and financial work stays
      in Scala. No Python on the measured path.
- [ ] Before editing any Scala to fix a parse failure, read `AGENTIC_ARCHITECTURE_GATES.md` and
      declare the workload, record contracts and controls (AGENTS.md "Before Editing").

## 1. Data root

- [x] Data root is outside the repo: `~/workspace/va_fresh_root`.
- [x] Download finished: `downloaded_verified=383 already_present_or_placeholder=2 failed=0`.
- [x] Immediate rerun is a no-op: `downloaded_verified=0 already_present_or_placeholder=385 failed=0`.
- [x] No leftover `.part` files under the root.
- [x] No download or other pipeline process running (`pgrep` for `run_pipeline`, `sbt`, `java`,
      `python`, `download_corpus`).
- [x] Free disk checked before starting (400 GB free at start; roughly 6 GB per fiscal year of
      pipeline output was seen in earlier FY2003 runs).

Commands:

```bash
bash scripts/download_corpus_zenodo.sh --data-root $HOME/workspace/va_fresh_root   # rerun = no-op check
find $HOME/workspace/va_fresh_root -name '*.part'
pgrep -fl "run_pipeline|sbt|java|python|download_corpus"
df -h $HOME
```

## 2. Known source gaps to handle before launching

- [x] **FY2009 has no PO file.** Step `2p` calls `sys.exit(1)` if `VA_opendata_FY2009.txt` is
      missing, so an empty file was created at
      `~/workspace/va_fresh_root/VAPOData/VA_opendata_FY2009.txt` (0 bytes) so FY2009 reaches
      end-of-file processing.
      - This file is local only. It is not in the Zenodo records or the SHA-256 catalog.
        Recreate it after any fresh download.
      - Code reading only: `applyPO` guards the header read with `if (lines.hasNext)`, so an empty
        file should not throw. **Not yet confirmed by a run.** Confirm when `2p` first reaches FY2009.
- [ ] FY2005 PO file exists (348,924 bytes) but earlier notes call FY2001-FY2005 stubs or partial.
      Expect small counts; do not treat low counts as a failure without checking the source.
- [ ] `2005 Combined Empty File.csv` and `2009 Combined no VA Data.csv` are the two zero-byte
      placeholders; they are not pipeline inputs.

```bash
: > ~/workspace/va_fresh_root/VAPOData/VA_opendata_FY2009.txt
```

## 3. Order of work: parsing first

Parsing failures are the most likely problem, so the first pass runs `ttl` alone over every year.
Running all steps year by year would reach later steps for early years before exposing a parse
failure in a later year.

- [ ] ~~Skip years already proven (FY2003).~~ **Withdrawn 2026-11-10.** The FY2003 `ttl` run
      `run_20261109_205425_98121` is not proven, see "Year-to-folder defect" below.

### Year-to-folder defect (found 2026-11-10, stopped the first sweep) — FIXED and proven the same day

Fixed in `standardizeAndSort.scala` (4-digit match, folder logged, per-file reconciliation line,
`--parseOnly true`), test 9 in `ApaTtlSpec`. Parse-only over FY2003-FY2016: 336 of 336 files
reconcile. FY2003-FY2006 `ttl,2p,2m` committed under `output_full/FYyy`; FY04 and FY06 evidence
incomplete; FY07-FY16 not run. See `VERIFICATION_AUDIT_REGISTER.md` and `SESSION_HANDOFF.md`
(2026-11-10 evening). The text below is the original defect record.

`discoverApaFiles` in `standardizeAndSort.scala` picks the source folder with
`dirName.contains(yearInt.toString)`, where `yearInt` is `"--year 04".toInt` = `4`. So `--year 04`
matches every expenditure folder whose name contains the digit `4`, and `candidates.head` takes
whichever the filesystem lists first. It does not match the 4-digit year.

| `--year` | digit matched | folders matched | Evidence |
|---|---|---|---|
| 04 | `4` | `FY 2004 Expenditure` and `All Expenditures for Fiscal Year 2014` | The sweep's "FY2004" month counts equal the **FY2014** files: Jul `650,501` lines = `source 649,422 + unresolved 1,078 + header`, and Aug `664,527` likewise. The real `FY 2004` Jul/Aug files have 447,908 and 436,808 lines. |
| 03 | `3` | `FY 2003 Expenditure` and `FY 2013 Expenditure` | The FY2003 run read 8,006,618 `ttl` source rows. Folder row counts: FY2003 = 5,817,081, FY2013 = 8,049,644, FY2004 = 5,602,210, FY2014 = 8,142,512. The locked 2026-11-06 FY2003 baseline was 5,758,480. **Strongly suggests the 2026-11-10 "FY2003" run read FY2013.** Not confirmed from the file dates (that check was declined). |

- The output name `SortedJEFY04_0004_07_exp.csv` also uses `yearInt`, so the period label `0004`
  looks plausible and hides the problem. I first read it as the known FY2003 naming pattern; that
  was wrong.
- The sweep was stopped and its output deleted. It produced nothing usable.
- **Everything recorded from the 2026-11-10 FY2003 verification run is suspect**, including the
  `VERIFICATION_AUDIT_REGISTER.md` entry for it. Do not use it as a baseline.
- Fix is a match on the 4-digit fiscal year in the folder name, then a test with two folders whose
  names share a digit (for example `FY 2003` and `FY 2013`). Same check is needed for the revenue
  path and for step 2p if it matches by digit.
- Proof that parsing reads the right data: per month, `source + unresolved_ref + blank rows` must
  equal the line count of the named source file, and the folder chosen must be logged.
- [x] Launch `ttl` only, all remaining years, monthly granularity, stop on first failure.
- [ ] Sweep completes for FY2004-FY2016 with exit 0 and no `ERROR` / `Exception` lines.
- [ ] For each year, record `source`, `sje`, `unresolved_ref`, and spill counts from the
      per-month console lines and compare against neighbouring years.
- [ ] Only after the sweep is clean, run `2p`, then `2m`, then `3` (see section 6).

Launch command used (the log path is what you tail):

```bash
cd ~/workspace/sharealedger/ledger-lab
bash scripts/run_pipeline.sh \
  --inPath  $HOME/workspace/va_fresh_root/VARawFiles \
  --outPath $HOME/workspace/va_fresh_root/output_ttl_sweep \
  --years 04,05,06,07,08,09,10,11,12,13,14,15,16 \
  --steps ttl --curve CCP3 --config ttl-parse-sweep-fy04-16-run1 \
  --periodGranularity monthly \
  > $HOME/workspace/va_fresh_logs/ttl_sweep_run1.log 2>&1
```

## 4. Watching a run

- [x] Start long runs so the console is visible to the owner, not only to the agent. The agent
      cannot open a terminal window. Use a log file and `tail -f` in a separate terminal.
- [x] Per-month engine lines to expect for each year (seen for FY2004):

```text
Discovered 12 E files for FY4
Processing E 0004/07 -> .../SortedJEFY04_0004_07_exp.csv  (1 file(s))
0004/07: source=649422 sje=1298844 unresolved_ref=1078 spills=7
Merging 7 spill(s) -> .../SortedJEFY04_0004_07_exp.csv ...
```

- [x] Startup is slow: about 3 minutes passes in sbt loading before the engine prints anything.
      Do not treat that as a hang.
- [x] The `ttl` engine is not a heavy CPU user (it is spill, merge and disk bound). Low CPU is
      expected; judge progress by new console lines and output file timestamps.
- [x] The machine can show a very high load average from other applications. Check `ps` before
      blaming the pipeline.

```bash
tail -f ~/workspace/va_fresh_logs/ttl_sweep_run1.log
tail -f ~/workspace/va_fresh_logs/ttl_sweep_run1.log | grep --line-buffered -E "START|RESULT|FAIL|ERROR|Exception|source="
kill -0 <pid> 2>/dev/null && echo running || echo exited
```

Keep agent-side waits short. Long `sleep` commands in the tool were cancelled several times; poll
with sleeps of about 25 seconds or less.

### 4.1 Full-corpus launch and watch (`scripts/run_full_corpus.sh`)

Added 2026-11-10 from the run that used 4 prep lanes plus a serial posting chain. **Changed
2026-11-11 (run 2):** posting is a pool (`--chainWidth`, default 2), `5v` starts when every
`prep_<yy>.ok` exists and overlaps posting, and the launcher runs `caffeinate -i -s -w <pid>`.
The 2026-11-10 21:07 run (serial chain, `5v` after the chain, no `caffeinate` for the first
~1 h) is the baseline for comparison, commit `70b6781`. The wall-clock header records
`chain_width=`, `views_early=Y` and `caffeinate=` so the two layouts cannot be confused.
The launcher needs an empty `output_full`: it refuses to start while any `FY*` entry exists. It also moves any
existing `run2_*.log` in the logs directory to `prev_run2_before_<stamp>/` (never deletes), because
the dashboard and the evidence archive read those logs by fixed name and a stale one is shown as
this run's result (seen 2026-11-10: a new run's dashboard reported `DONE` from the previous run's logs).

Before launching: quit Dropbox and OneDrive (the run is disk-bound), check free disk (about
6 GB per fiscal year seen so far; the 14-year run had written 35 GB about 23 minutes in),
and clear old output. Confirm with the owner before deleting a previous run's output.

```bash
cd ~/workspace/sharealedger/ledger-lab
rm -rf ~/workspace/va_fresh_root/output_full/*
bash scripts/run_full_corpus.sh > ~/workspace/va_fresh_logs/run2_corpus.log 2>&1 < /dev/null &
```

**Launching from an agent tool:** use the tool's background mode (`background: true`). A plain
`nohup ... &` inside one tool call died when that call ended, and the run was lost silently on
2026-11-10. After launching, wait past the first 60 seconds and confirm the lanes are alive
(`pgrep -fl run_pipeline`) before reporting it as running.

**Dashboard (automatic on macOS).** The full-corpus launcher opens a Terminal dashboard
using the selected log/output directories, including custom queue directories. Each queued
run opens its own dashboard. Pass `--noDashboard` to `run_full_corpus.sh` for headless
execution. Terminal automation permission may be requested on first use; a failed or
unavailable dashboard is logged in `dashboard_startup.log` and does not stop processing.

One row per fiscal year; columns are the six steps, grouped as JE
generation (`ttl` = Pay JEs, `2p` = PO JEs, `2m` = Merge) and Posting (`3` = Post, `20` = GL Bal,
`21` = Permute). It is read-only, parses only the START and RESULT lines in the run logs, and costs
well under one CPU-second per refresh.

```bash
bash scripts/dashboard_full_corpus.sh         # refreshes every 30 s by default; pass a number of seconds to change it
```

For custom directories, set `VA_LOGS` and `VA_OUT` when reopening a dashboard or
running `scripts/open_run_windows.sh`. The latter opens both dashboard and event
windows by default; `30 --dashboard-only` opens only the dashboard.

Cell values: `-` not started, `RUN m:ss` running, `NNs ok` finished with its elapsed seconds,
`FAIL rc=N` failed. The `Total` column is the sum of the year's finished step times plus the
running step's elapsed time (step time, not wall clock). State values: `queued`, `JE gen`, `JEs done` (waiting its turn to post),
`Posting`, `DONE`, `FAILED`. A `rows` line under each year shows records read>written for each
finished step, because row counts drive step time (for `2m` only the written count is reported;
for `3`, `20` and `21` the first number is the SJE rows read and the second the ledger, GL or
permutation rows written). Under `Total` the same line shows all rows read plus written across the
year's finished steps. A row that passes through several steps is counted once per step, so this
is volume handled, not distinct records. The footer adds the same sum over all years. It also shows JE generation and posting counts, whether the `5v`
views pass has started, launch retries, fail markers, output size, free disk, load and the number
of Java processes.

**Projection line (rough).** For each of the six steps the dashboard takes the mean of the last 3
completed years, sums what is left (a running step counts only its remaining part), divides JE
generation by the lane count and posting by the chain width. Posting work per year uses the
observed chain wall time (`chain_end_fy` minus `chain_start_fy` in the wall-clock record, mean of
the last 3 years), so the non-engine evidence time is included; before any year has finished it
falls back to the sum of step times, which was 2.5 times too small in the baseline. It prints an
estimated clock time for the end of JE generation and of posting. `5v` is shown but not projected,
the projection assumes today's contention, and later fiscal years are larger, so expect the real
finish to be later. Posting finishing well after JE generation means posting is the bottleneck.
`VA_LOGS` and `VA_OUT` override the log and output directories (used to watch a test root).

**Open the watch windows automatically (macOS).** After launching the run:

```bash
bash scripts/open_run_windows.sh              # dashboard refresh defaults to 30 s
```

This opens two Terminal.app windows: the dashboard above, and an events-only stream (RESULT, RETRY,
nonzero exits, STOPPED, ALL YEARS, ERROR and Exception lines from every run log, including logs
that do not exist yet). The first use may prompt macOS to allow automation of Terminal. Closing
the windows does not affect the run. An agent can run this script too, so the owner sees the
windows without opening them by hand.

**Raw log commands** (all logs are in `~/workspace/va_fresh_logs`):

```bash
tail -F ~/workspace/va_fresh_logs/run2_corpus.log                                  # orchestrator: lane starts, exits, STOPPED
tail -F ~/workspace/va_fresh_logs/run2_prep_fy*.log                                # JE-generation lanes live (glob fixed at launch of tail)
tail -F ~/workspace/va_fresh_logs/run2_*.log | grep --line-buffered -E "RESULT step=|RETRY|exit [1-9]|ERROR|Exception"
grep -h RETRY ~/workspace/va_fresh_logs/run2_*.log                                 # sbt launch retries (expected: few or none)
ls "$(ls -dt ~/workspace/va_fresh_logs/markers_* | head -1)"                       # prep_<yy>.ok / .fail, chain_<yy>.fail for this run
cat ~/workspace/va_fresh_logs/corpus_wallclock_*.csv                               # corpus-level start/end stamps (the comparable figure)
```

**What the files are.** `run2_corpus.log` is the orchestrator. `run2_prep_fy<yy>.log` holds the
`ttl,2p,2m` steps for a year, `run2_chain_fy<yy>.log` holds `3,20,21`, and `run2_views.log` holds
the final cross-year `5v` pass. Markers live in `markers_<stamp>/`; the newest directory is the
current run, and older ones belong to earlier attempts.

**Reading a failure.** The first failing step writes a `*.fail` marker. No new chain year starts and
`5v` is not started if it has not begun (`STOPPED: prep rc=... chain rc=... views rc=...` in the
orchestrator log). Running steps, including a `5v` already under way, finish. Find the
year with `FAIL` on the dashboard or a `.fail` marker, then read the tail of that year's log.
`rc=2` from a step is not necessarily an engine error: on 2026-11-10 it was an sbt boot-socket
collision (`ServerAlreadyBootingException ... Address already in use`) when two lanes launched
sbt at the same instant. `run_pipeline.sh` now relaunches on exactly that message, up to 6 times,
and logs a `RETRY` line each time; any other nonzero exit stops the run.

**Timing.** Per-step `elapsed_s` is inflated when lanes overlap (load average was 30 to 55 on
4 CPUs) and is not comparable with serial baselines. Compare runs by the wall-clock CSV and by the
`RESOURCE` CPU seconds. In run 2 the `5v` pass overlaps posting, so its `elapsed_s` is not
comparable with the baseline's 1,285 s; compare its CPU seconds (baseline 1,433 s).
The dashboard and the log commands read files only and start no engine step.

## 5. When a run fails or is stopped

- [x] Stopping a run: `kill -TERM -<pid>` kills the whole process group.
- [x] **A killed or failed run can still be committed.** The first aborted attempt left the output
      symlink pointing at a partial generation (one month of FY2009, 388 MB, with a spill
      directory). It was removed by hand:
      `rm <outPath>` (the symlink) and `rm -r <outPath>.generations/<run_id>`.
- [ ] Before relaunching, confirm `<outPath>` and `<outPath>.generations/` hold no partial run, so
      the next run does not inherit stale files. Prior-ledger replay already caused a false
      `$18.00` residual once (SESSION_HANDOFF 2026-11-06).
- [ ] Record the failing file, month, error text and the fix in
      `VERIFICATION_AUDIT_REGISTER.md` before relaunching.
- [ ] Rerun the **same** year range after the fix. A fixed parser changes the workload; earlier
      successful years are not comparable to later ones unless rerun.

## 6. After the `ttl` sweep (pending, not yet decided)

These follow the staged sequence in `TEST_DESIGN.md` section 7. The code review below is from
reading source only; none of it has been run.

- [ ] `2p` for each year, including the empty FY2009 file.
- [ ] `2m` merge, which auto-discovers all `SortedJEFY{yr}*.csv` in the output directory.
- [ ] `3` post, then control total: ledger amount sum must be zero.
- [ ] **`5v` has two defects, found by reading source and existing FY2003 output (2026-11-10).
      Nothing has been changed; no Scala is edited while a sweep is running (sbt would recompile
      under the later years).** Fix both before relying on `5v` in a multi-year run, and add a
      control-total test.
      1. **Multi-year overwrite.** `CKBViews` rewrites its four `view_*.csv` files on every call
         (`new PrintWriter`). `run_pipeline.sh` calls it once per year with a year filter, so a
         multi-year run in one output directory keeps only the last year's views, and
         `present_next_year` can only see years in the filtered file set. Needs an all-years mode
         or a single pass with no year filter.
      2. **Merged file double-counted.** `CKBViews` selects every file starting with `SortedJE`
         whose name contains `FY{yy}`. That includes both the source files
         (`SortedJEFY03_*_exp.csv`, `SortedJEFY03q1PO.csv`) and `SortedJEFY03merged.csv`, which
         holds the same rows. In the FY2003 run `records_read=32,236,192` for `5v`, `3`, `20` and
         `21` and the merged file has 16,118,096 rows, so each row is read twice (the same factor
         applies to `post.scala`, which uses the same filter). View counts are therefore
         doubled: views A, B and C each sum to 106,796 PO lines, and the PO file has 51,462
         positive rows, 51,462 negative rows and 1,936 zero rows; debits include zero, so one
         read would give 53,398 (not checked against the views beyond this arithmetic).
         Whether step 3 also double-posts is **not established**: the earlier zero-balance results
         may hide it because both reads carry offsetting rows. Check before trusting any ledger
         row count. Needs the reader to choose either the source files or the merged file, not both.
      - The FY2003 numbers in this note come from
        `~/workspace/va_fresh_root/output_test_fy03` (`run_20261109_205425_98121`).
      - Record in `VERIFICATION_AUDIT_REGISTER.md` and, if it is an engine gap,
        `GENEVAERS_CAPABILITY_GAP_REGISTER.md`.
- [ ] Steps `20` and `21` filter on `SortedJEFY{yy}` per year; confirm output naming per year.
- [ ] Replicates: Stage 4 calls for 3 replicates for each of CCP1, CCP2 and CCP3.
- [ ] Re-baseline: the `applyPO` fix changed the PO workload, so the locked 2026-11-06 CCP figures
      are not comparable. Record these runs as a new baseline, not a replicate.
- [ ] **Parallel posting: implemented 2026-11-11 and tested on FY04+FY05 only; not yet run on the
      corpus.** Owner approved width 2. `run_full_corpus.sh` now runs the `3,20,21` chains through
      `xargs -P` (`--chainWidth`, default 2), each worker waiting for its own `prep_<yy>.ok`;
      a failing worker exits 255 so no further year starts. Bounded test (separate root
      `~/workspace/va_test_root`, logs `~/workspace/va_test_logs`, `--years 04,05 --lanes 2`): exit 0;
      FY05's chain started at 00:42:31 while FY04's prep was still running; `LDGR`, `GL`, `PERM_TBL`,
      `PERM_SUMMARY` and the merged SJE of both years are byte-identical (`cksum`) to the 21:07
      baseline. Not tested: more than two years, a failing year, 14-year load. Steps `20` and `21`
      within a year still run one after the other (not changed). Original investigation notes
      follow. `chain()` in
      `scripts/run_full_corpus.sh` ran `3,20,21` for one year at a time. Once prep ends the 4 CPUs
      are mostly idle during posting (about 3 minutes per year in the 12:33 run, 10 years left at
      that point), so running more than one posting lane may shorten the corpus wall clock. Not
      measured. Do not edit the script while a run is in progress.
      - **Investigation findings (2026-11-10, code and logs read only; nothing run or changed).**
        - *Why it is serial:* `846e952` states "parallel prep lanes, serial 3/20/21 chain" with no
          reason given. The owner's to-do in `SESSION_HANDOFF.md` ("next full run, post years in
          parallel and start `5v` when JE generation ends") already proposes this and lists the
          checks below. Item 3 of the 2026-11-11 "Not done" list adds "run 20 and 21 in parallel".
        - *Years look independent (code reading):* `post.scala` derives the ledger name from each
          journal file's own year (`LDGR20{yy}`) and opens only that year's `LDGR` as the prior
          ledger; every chain log so far says "No existing ledger file. File will be created:
          LDGR20{yy}.csv" and none opens another year. Steps `3`, `20`, `21` take
          `--outPath` = that year's own generation directory and select `SortedJEFY{yy}` files;
          `GLPost` and `PermutationGenerator` write only `GL20{yy}`, `PERM_TBL20{yy}`,
          `PERM_SUMMARY20{yy}` and spill files (`perm_spill_{yy}_{n}.tmp`) inside that directory.
          Each year has its own `FY<yy>` output directory, `.generations` folder and
          `cost_surface.csv`. `--seedFromCommitted` copies only that year's own committed output.
          Not read: all of `post.scala`, `GLPost` and `LedgerApp` step 3/20/21 dispatch beyond
          the lines above; and `CurrencyRevaluation` opening-ledger reads, which are not in the
          `3,20,21` path.
        - *Steps 20 and 21 within a year:* `GLPost` has no reference to `PERM_TBL` and
          `PermutationGenerator` does not read `GL20{yy}`, so neither looks like it needs the
          other's output. Both read the same merged file, so they could run side by side. Not
          proven by a run.
        - *Capacity, measured in this run (FY06-FY08, load was light):* each step uses about
          1.0 to 1.3 GB peak RSS (heap is fixed at 1 GB) and 66 to 207 s wall for 80 to 160 CPU
          seconds. Wall is close to CPU seconds, so each step is mostly one busy core, which is the
          single-core pattern seen once JE generation finished. The machine has 4 CPUs and 16 GB,
          so 2 to 4 concurrent steps fit in memory (about 5 GB at four). A whole year's three
          steps use about 240 to 350 CPU seconds.
        - *Upper bound on the gain (estimate, not measured):* with serial posting at about 5 min
          per year, 8 years remaining at 22:45 is about 40 min. Two years in parallel would
          approach half of that; the limit is CPU (4), disk (about 108 GB written so far) and the
          1 GB heap per JVM. Do not expect a straight 2x.
      - *Still to check before any change:* the sbt boot-socket collision (more concurrent
        launches; `RETRY` covers the exact message, one retry seen in this run); whether `5v`
        and the posting steps compete for disk; and the dashboard (`dashboard_full_corpus.sh`)
        assumes one chain and must be changed with the launcher. CPU seconds per step are now
        captured in `RESOURCE` lines, so step cost stays comparable across serial and parallel
        runs on CPU seconds even where wall time is not.
      - If years are independent (as above), test two posting lanes on a bounded set of years
        first, and compare control totals (`net_amount=0` per year, ledger row counts) and the
        output files with the serial result before using it on the corpus.
      - It changes the workload shape (overlapping posting). Record any parallel run as a new
        baseline, not a replicate, and keep the serial figures separate (see the re-baseline item).
      - Keep it in Bash job control; no financial logic in the script.
- [ ] **`5v` in parallel with posting: implemented 2026-11-11 and tested on FY04+FY05 only.**
      The launcher starts `5v` when every `prep_<yy>.ok` exists (`views_start`, `views_end` stamps).
      In the bounded test `5v` started 00:44:01 with both chains running and finished 00:47:44
      (222 s for 20.4M records, exit 0); its four view files are byte-identical to a separate
      serial `5v` over the same two years (`test_root/output_5v_serial`). Symlink-swap safety:
      checked by file identity, not by a race. For all 14 years the `SortedJE*` files in the prep and
      posting generations are the same inode (hard links, `ls -i`), `CKBViews` opens every input once at
      start, and `commit_attempt` swaps the link with an atomic `mv`. In the test, though, `5v` ended
      before either chain committed, so a swap while `5v` was reading was **not** exercised.
      Open: first corpus run is the real check. Original notes follow.
      `run_full_corpus.sh` started the VIEWS pass only after the whole chain
      (`chain_all_done`), but `5v` may not need posting output at all.
      - Read from `CKBViews.scala` (header and file selection): it reads only `SortedJE*` files,
        one merged file per year (`SortedJEInputs.oneRead`), and no ledger, GL or permutation
        file. Its header says it runs "after Step 3 (post) or Step 2/2m as a separate
        view-extraction pass", so the dependency on posting looks like a launcher choice and not a
        data dependency. Not yet confirmed by a run.
      - `5v` is one k-way merge across **all** years, so it can start once every year's prep has
        committed (all `prep_<yy>.ok`), not earlier. It does not have to wait for the last post;
        it could overlap the whole posting chain. In the 2026-11-10 21:07 run that is about
        an hour of overlap (estimate: prep done about 22:30, posting about 23:00 to 23:20).
      - Check before relying on it: posting commits a new generation per year and swaps the
        `FY<yy>` symlink (`commit_attempt` in `run_pipeline.sh`) while `5v` reads through those
        paths. Posting hard-links the data files and `prune_old_generations` keeps two
        `run_*` directories, which looks safe, but confirm that `5v` opens its inputs before the
        swap, or give it the prep generation's directory path directly.
      - Cost: it competes with posting for CPU and disk, so per-step times are no longer
        comparable with a serial run. Keep the `5v` and posting timings separately identifiable,
        and record an overlapped run as a new baseline, not a replicate.
      - The 12:33 run's `5v` failed (27 min, `corpus_end_FAILED`); fix and prove `5v` on its own
        before overlapping it with anything.
      - Bash job control only (a second background job and a wait on it); no logic in the script.
      - Related to the parallel-posting item above; do both together, since the posting lanes and
        `5v` would share the same CPUs.

## 7. Evidence to record when a run is clean

- [ ] Run id, command line, log path, exit code.
- [ ] Per-year record counts and `unresolved_ref` counts.
- [ ] `cost_surface.csv` row, `process_metrics.csv`, peak RSS per step.
- [ ] Control totals (SJE balance, ledger sum) for any run that includes `3`.
- [ ] Entry in `VERIFICATION_AUDIT_REGISTER.md` with state `partially_verified` until the owner
      accepts it.
- [ ] Update `SESSION_HANDOFF.md`.
