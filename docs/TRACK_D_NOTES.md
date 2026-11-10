# Track D: Working Notes (2026-11-10)

State: **idea stage. Nothing has been run. No AI subject has been used.** Branch `trackD`, cut from `main`
and fast-forwarded to `be1c150` on 2026-11-10. Work on this track waits until `5v` finishes on the old Mac
(owner decision, 2026-11-10). Nothing here changes the Scala engine, Bash orchestration or any measured
cost curve.

This file is a record of a design discussion. It is not a pre-registration. Predictions below are the
owner's stated bets, written down before any run, and they are not results.

## 1. Genesis

Track D came out of a discussion about how much a full-corpus run achieved on a small, old laptop, and a
Paper 2 note about the pull of AI toward summarizing or accumulating early. Spark is the contrast case.

Related material already in the repo or the TDS repo:

- `PROJECT_BRIEF.md` (Paper 1 §3.2a): unscheduled engine-dependency test, `post.scala` against
  `DPBSpark.scala`. `README.md` excludes Spark from the cost-curve experiment.
- `docs/BOB_METHOD_NOTE_CKB_EFFICIENCY.md`: Paper 4 §6.2, Spark cannot be told input is already sorted.
- TDS repo `03-argument/02-business-event-repositories.md`, line 304 (Paper 2 methodological footnote):
  AI coding assistants twice put an in-memory accumulator before a sort boundary. Both were reverted.
  Line 410-412: hardware-parameterized bowl minimum.
- `SESSION_HANDOFF.md`: Spark source folders and three SAFR-on-Spark requirements awaiting an owner decision.

## 2. Reference run (Scala sorted-file path)

Run 2, committed on `main` (`a4d4ec7`): `data/test_evidence/corpus_runs/run_20261110_123303/`.

| Item | Value | Source |
|---|---|---|
| Years | FY2003-FY2016, all 14 through `ttl,2p,2m` and `3,20,21` | `RUN_SUMMARY.md` |
| Scale | about 100M source rows, about 190M journal lines, 109 GB output on disk | `SESSION_HANDOFF.md` (rough, from chat) |
| Host | old Mac, i7-5557U, 4 threads, 16 GB RAM, 1 GB JVM heap per step, load 30-50 | `SESSION_HANDOFF.md` |
| Start / `chain_all_done` | 12:33:03 / 14:01:28 | `corpus_wallclock.csv` |
| `5v` | started 14:01:28, stopped 14:28:20, no output, `corpus_end_FAILED` | `corpus_wallclock.csv` |
| Controls | 13 of 14 years balance to 0.00; FY2010 off by 60.00 (PO reader defect, since fixed) | `RUN_SUMMARY.md` |

Limits on this reference:

- `peak_rss_bytes` is a JVM heap fallback, not OS RSS. Memory used is not measured.
- Four prep lanes ran at load 30-50, so per-step times are not clean cost points. Only the corpus wall
  clock is comparable.
- `5v` has not completed. It is being worked on `main` on the old Mac (GERS-003, `be1c150`).
- The cost estimate for a Spark cluster in the handoff (8-16 workers, $5-25 per run) is an assumption, not
  a measurement. Do not cite it as evidence.
- Correction recorded: an earlier statement in this session that only four years had run came from stale
  register text and was wrong. All 14 years went through posting in Run 2.

Hardware facts to keep straight:

- This machine (`MacBookAir`): Apple M1, 8 cores, 16 GB RAM, about 33 GB free disk. It cannot hold Run 2's
  output. It is not the measurement platform.
- Target machine (old Mac): 16 GB RAM per the handoff. Owner states it has about 400 GB (disk) and is the
  only machine available for this work. The earlier throttled-CPU finding (about 0.79 GHz, battery fault)
  is in `docs/VERIFICATION_AUDIT_REGISTER.md`, section "2026-11-10 `ttl` folder-match fix".

## 3. Owner's predictions for Spark (written before any run)

Pre-registered wording, tightened in discussion:

| # | Prediction | How it would be scored |
|---|---|---|
| P1 | Spark re-shuffles pre-sorted multi-file input and cannot manage many files CKB-style (single driver). | Shuffle stages and shuffle-read bytes from Spark event log. Record any use of bucketing as a counterexample. |
| P2 | It may complete, but takes a long time. | Wall clock against Run 2 under a fixed cap. The multiple that counts as "slow" is not yet chosen by the owner. |
| P3 | A single JVM process loses its time to garbage collection, especially if Bash orchestration is not specified. | GC share of task time, peak RSS, Spark mode (local or standalone), one `spark-submit` against one per year. |

Open design points:

- Spark local mode runs executors as threads in one driver JVM, which supports P3. A standalone cluster of
  several worker JVMs on one laptop would partly avoid it. Record the mode chosen.
- If the subject AI never picks Spark, P1-P3 are untested. A forced-Spark arm is needed.
- Disk is a separate failure class: shuffle spill for the full corpus can exhaust free space before memory.
- No usable Scala baseline exists for `5v` until it completes, so a time multiple cannot be fixed yet.

## 4. Designs considered

1. **Blind test (rejected as primary).** Give an AI the task, no architecture knowledge. Expected to fail on
   small hardware. Low information, because the failure is close to a foregone conclusion.
2. **Knowledge ladder (parked).** Rung 0 nothing; 1 the CKB principle only, tool-neutral; 2 principle plus the
   Spark limitation; 3 full contract. Reason for rung 1 over full CKB: the claim is whether the principle
   changes the design, and the Spark steer must be its own rung. Rung 1 text must not use repo vocabulary
   (SJE, ARE, GenevaERS, layer numbers, Scala). The session that has read the repo should not write it.
3. **Bounded-hardware design test (rejected).** Whether an AI can design within a stated resource bound is an
   engineering-competence test and does not support the paper. Arm A (no bound stated) also needs hardware
   the owner does not have.
4. **Historical-choice test (current candidate).** See section 5.

## 5. Current candidate: does the historical choice persist?

Reasoning from the owner: Paper 2 is about historical choices (summarize early, keep balances, drop the event)
being carried into financial processes. AI training data is another instance of the same inheritance,
continuing into the AI era. A test should look for that persistence. It does not need a hardware test.

Sketch:

1. State generous resources (for example 400 GB disk, ample time) so the old cost reason is absent.
2. Ask a business question that needs event detail, with no architecture named. Example: which vendors appear
   in both Agency A and Agency B spending, and which stopped trading with the state.
3. Design only. No build, no run.
4. Score the design for the habit: aggregates to balances before keeping vendor or instrument detail; drops
   event order or the original record; treats a summary table as the primary store; justifies this by cost
   or space after being told they are not scarce.
5. Compare against a variant adding "keep every source event".

Cautions:

- Neutral wording needs several variants, tested by the owner first.
- A few runs show a tendency, not a rate. Keep claims modest.
- Scoring is judgment. Write the checklist before any run. A second scorer who has not read the repo is better.
- This tests the Paper 2 thesis. It is separate from the CKB code anecdote (footnote, line 304) and from the
  Spark engine comparison (Paper 1), which belongs with the `5v` and Run 2 reference work.

## 6. Open decisions (owner)

- Which claim Track D supports: the Paper 2 thesis (habit persists), the narrower CKB anecdote, or the Paper 1
  Spark engine comparison. They are separate tests.
- Subject tool or tools (Codex was mentioned) and how many replicates.
- Whether any Spark run needs an explicit exception to `README.md` (Spark excluded from the cost curves).
- What multiple of the Scala wall clock counts as "slow" (P2).
- Who writes any knowledge text given to a subject, and who scores the outputs.

## 7. Rules that still apply

- Python needs owner approval before any script is written (`AGENTS.md`). None has been proposed here.
- Any subject-AI run happens in an empty directory with no `AGENTS.md` above it, and is saved here afterward
  as evidence labelled illustrative. It stays outside the measured cost curves.
- Do not compare Spark and Scala runs as the same engine measurement (`AGENTS.md`, Measurement Integrity).
- Do not edit `5v`, `CKBViews.scala` or pipeline scripts on this branch while `main` is being worked.

## 8. Next steps

1. Wait for `5v` to finish on the old Mac; pull `main` into `trackD`.
2. Update section 2 with the `5v` outcome and wall clock.
3. Owner picks the claim in section 6, then a pre-registration (claim, prediction, outcome classes, scoring
   checklist) is committed before any subject run.
