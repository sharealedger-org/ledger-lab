# Track D: Codex unconstrained control

Updated 2026-11-12. **PAUSED by the owner (2026-11-12) while the companion measured-results paper is drafted; see the resume checklist below.** A separate repository, `sharealedger-org/ledger-lab-control`, with its own handoff and journal. This page is Bob's digest of it. Do not copy its journal here and do not re-read it whole.

## What it is

An unconstrained AI subject (Codex, Python and SQLite, Arm B) builds and runs a pipeline on the same Virginia corpus under stated hardware limits (16 GB, 4 threads). It gives the Engine graph a second curve. Two Codex instances: **Development (Intel)** implements and runs; **Origin (ARM64)** reviews. Bob audits.

## Digest procedure (the context saver)

1. Last revision of the control repo that Bob read: **`3c50c19`** (2026-11-12 12:26 -0700). Read: the two new handoff entries, the two journal additions, and `docs/OFFICIAL_MEASUREMENT_20261112/README.md`. Not read: the JSON/CSV evidence, plots, code.
2. Next session: `git log --format='%h %ad %an %s' 3c50c19..origin/main` on a clone, read only those entries' diffs, then update the revision above.
3. Log each reading in `docs/COMPANION_BASELINE_RECORD_2026-11-12.md` section 2 (delivery, confirmed reading, incorporation are separate events). Do not write into the control repository from here without the owner's say-so; its journal is append-only and authored by each agent.

## State at `3c50c19`: official attempt complete (Codex's report; Bob has not verified the data)

- Official attempt on frozen implementation `892a068`: controller PASS, reconciliation PASS, disk accounting PASS. Launch 04:33 AZT, completion marker 11:58 AZT, about 7 h 25 min end to end, Intel host (2 cores, 4 threads, 16 GiB).
- Population: 349 files, 130,861,904 accepted events (97,516,306 expenditure, 17,750,109 revenue, 15,595,489 PO), 8,064 quarantined candidates.
- Measured phases (wall; peak RSS): build incl. ingestion, indexes, validation 17,687 s (164 MB); full six-report export 8,730 s (138 MB); keyed query driver 194 s; 6,660 requests at Q = 10, 100, 1,000 for six variants; plotting 14 s.
- Retained storage 183.5 GB (database and indexes 182.9 GB). W1 debit = credit = 1,927,539,113,246.80; W2/W3/W4 commitment totals agree at 64,021,717,273.67.
- Codex states: one run; no companion advantage and no repeatability claim; the next step is interpretation against permitted baseline evidence after reconciling workload, input and accounting differences. Evidence is committed under `docs/OFFICIAL_MEASUREMENT_20261112/`; the full database and exports stay on the Intel host.
- Earlier: the control's 35 tests, 7 controller tests under `python3 -O` and the fixture check passed in a throw-away copy (Bob, at `51f16a6`). Not reviewed by Bob: `src/ledger_control.py` and the other scripts.

## Two comparisons, kept apart

1. **Original companion against control (now).** Companion baseline = tag `companion-measured-baseline-20261111` (`c3667e4`). Chronology is owner-reported; commit times are consistent with it.
2. **Informed companion against original (later).** Any change that follows a control finding is logged in the baseline record section 2 with its source entry. Codex gets access to this repository for that phase; the owner said it is cooperative, not adversarial, and the timing is the owner's.

## Resume checklist (Track D is paused; this is everything needed to restart)

1. **Control repo digest:** last read `3c50c19`. Run the digest procedure above.
2. **Frozen control result:** implementation `892a068`; evidence committed in the control repo under `docs/OFFICIAL_MEASUREMENT_20261112/`; full run (183.5 GB database, exports, traces) at `/Users/ktwitchell001/workspace/ledger-control-output/official-20261112-892a068` on the Intel host. **Keep that directory until Part B of the comparison plan is done**: the novel-permutation challenge needs the control's database. **Decide on resume (owner, noted 2026-11-12): ask Codex Development to preserve it.** Bob does not write to the control repo.
3. **Companion baseline:** tag `companion-measured-baseline-20261111` (`c3667e4`). The final outputs were deleted to free disk; Part B needs the chain re-run (about 2.5 h on the paired layout). Gold hashes: `data/test_evidence/corpus_runs/run_20261111_014229/gold/MANIFEST.csv`, `MANIFEST_SJE.csv`.
4. **Plan:** [`COMPARISON_PLAN.md`](COMPARISON_PLAN.md) (Part A compare, Part B blind novel-permutation challenge, decisions 1 to 5 still open).
5. **Papers as the control saw them:** Papers 1 and 2 only, as `docs/GATE_1_PAPER_1_MINIMUM_COST_CURVE.md` and `docs/GATE_1_PAPER_2_BUSINESS_EVENT_REPOSITORIES.md` (Gate 1, 2026-11-10). Paper 1 is byte-identical to `members/research/01-minimum-cost-curve.md`; the originals stay in `members/research`. **The monograph introduction (`00-monograph-introduction-and-master-framework.md`) was withheld on purpose: it exposes too much about the experiment.** Paper 1 v0 is the control's specification and the pre-measurement statement of the claims to test. Delivery of any revised paper to the control is an owner decision, not made yet.
6. **Exposure so far:** Codex has seen no companion code or results; Bob has read the control's results twice (baseline record section 2). When the new measured-results draft goes to Codex, log it there as a delivery event.
7. **Keep the Part B question set out of anything the control can see**, until Part B runs. Part B is deferred (owner, 2026-11-12).
8. **Principle (owner, 2026-11-12):** blindness matters for the **control**, which plays the uninformed build. It does not matter for the **CKB** experiment, which should adapt to new information; the two are not in competition. Informed changes are still logged (baseline record section 2) so the baseline stays distinguishable.
9. **Idea to decide on resume (owner):** give one Codex instance **read access to CKB** (this repository) while it does **not** write into the control, so it can help formulate the Part B questions and reduce AI-engine bias in the experiment. Points to settle then: it must not be the instance that implements the control, its questions reach the control only when Part B runs, and its access is logged as an exposure event.
10. **Papers are edited in TDS**, not here: `/Users/kiptwitchell/Workspace/GHEGenevaERS/TDS-AI-Models` (behind the IBM firewall, with the source documents). This repository supplies an evidence summary for its open tier, `01-evidence/practitioner/Sharealedger/`. Nothing from TDS is copied into this public repository.

## Next actions

0. Draft for the owner: [`COMPARISON_PLAN.md`](COMPARISON_PLAN.md) (Part A compare what is measured; Part B a blind novel-permutation challenge before any exposure of this repository to Codex).

1. Owner: pointer in the control's `SESSION_HANDOFF.md`, written when this repository is opened to Codex: repository name, tag `companion-measured-baseline-20261111`, and the SHA of the commit holding the **results evaluation** of the control's work (owner, 2026-11-12), so Codex can see the assessment. That commit does not exist yet. Not written.
2. Before any whole-run comparison, reconcile (baseline record section 3): revenue (companion did not process it), PO quarantine sets (about 32,000 companion rows against 7,551 control candidates after quote-aware recovery), year populations (FY2009 PO absent), timed scope (control starts at ingestion; companion at the Scala pipeline), hardware.
3. When the official result is accepted: a one-page comparison on reconciled populations, not a diff of totals. The Engine graph (B1) waits for it.

Documents here: [`docs/TRACK_D_NOTES.md`](../../docs/TRACK_D_NOTES.md) (summary and the pre-measurement projections, which are predictions, not results), [`docs/COMPANION_BASELINE_RECORD_2026-11-12.md`](../../docs/COMPANION_BASELINE_RECORD_2026-11-12.md).
