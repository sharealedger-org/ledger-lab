# Track D: Codex unconstrained control

Updated 2026-11-12. A separate repository, `sharealedger-org/ledger-lab-control`, with its own handoff and journal. This page is Bob's digest of it. Do not copy its journal here and do not re-read it whole.

## What it is

An unconstrained AI subject (Codex, Python and SQLite, Arm B) builds and runs a pipeline on the same Virginia corpus under stated hardware limits (16 GB, 4 threads). It gives the Engine graph a second curve. Two Codex instances: **Development (Intel)** implements and runs; **Origin (ARM64)** reviews. Bob audits.

## Digest procedure (the context saver)

1. Last revision of the control repo that Bob read: **`51f16a6`** (2026-11-12 04:52 -0700).
2. Next session: `git log --format='%h %ad %an %s' 51f16a6..origin/main` on a clone, read only those entries' diffs, then update the revision above.
3. Log each reading in `docs/COMPANION_BASELINE_RECORD_2026-11-12.md` section 2 (delivery, confirmed reading, incorporation are separate events). Do not write into the control repository from here without the owner's say-so; its journal is append-only and authored by each agent.

## State at `51f16a6`

- Fresh official full-corpus attempt (`892a068`) **RUNNING** on the Intel host, output outside this machine. No accepted result exists.
- Controller fixes verified by Bob in a throw-away copy: the control's 35 tests, 7 controller tests under `python3 -O`, and the six-output fixture check pass.
- Preflight counts: 130,861,904 accepted events (97,516,306 expenditure, 17,750,109 revenue, 15,595,489 PO) over 349 files, 26,887,407,880 bytes.
- Not reviewed by Bob: `src/ledger_control.py` and the other scripts.

## Two comparisons, kept apart

1. **Original companion against control (now).** Companion baseline = tag `companion-measured-baseline-20261111` (`c3667e4`). Chronology is owner-reported; commit times are consistent with it.
2. **Informed companion against original (later).** Any change that follows a control finding is logged in the baseline record section 2 with its source entry. Codex gets access to this repository for that phase; the owner said it is cooperative, not adversarial, and the timing is the owner's.

## Next actions

1. Owner: pointer in the control handoff (repository name, tag, commit; no results). Not written.
2. Before any whole-run comparison, reconcile (baseline record section 3): revenue (companion did not process it), PO quarantine sets (about 32,000 companion rows against 7,551 control candidates after quote-aware recovery), year populations (FY2009 PO absent), timed scope (control starts at ingestion; companion at the Scala pipeline), hardware.
3. When the official result is accepted: a one-page comparison on reconciled populations, not a diff of totals. The Engine graph (B1) waits for it.

Documents here: [`docs/TRACK_D_NOTES.md`](../../docs/TRACK_D_NOTES.md) (summary and the pre-measurement projections, which are predictions, not results), [`docs/COMPANION_BASELINE_RECORD_2026-11-12.md`](../../docs/COMPANION_BASELINE_RECORD_2026-11-12.md).
