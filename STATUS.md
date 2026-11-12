# Ledger Lab: Status (read this first)

Updated 2026-11-12. One page. A session starts here, then reads only the `STATUS.md` of the track it works on. Do not read the whole `SESSION_HANDOFF.md` or the audit register unless a track page points to an entry. If this page and the register disagree, the register wins.

## Tracks

| Track | What | State | Page |
|---|---|---|---|
| **A** | Private P&C insurer corpus (25.5 billion rows). Protected data. | Progresses in another repository. Results come back here in synthesised form when a result is complete. **No status page in this repo (owner, 2026-11-12).** | none |
| **B1** | Virginia public data, Paper 1: cost curve (CCP1 to CCP5) | Baseline measured and tagged. CCP4/CCP5 corpus runs and replicates not done. | [`tracks/B1-cost/STATUS.md`](tracks/B1-cost/STATUS.md) |
| **B2** | Virginia public data, Paper 2: AIM tests and the three-model experiment | Steps 1 to 4 done. **On hold** during the reorganisation; D9 decision open. | [`tracks/B2-models/STATUS.md`](tracks/B2-models/STATUS.md) |
| **C** | Berka bank data | Stalled. Illustrative only; not Paper 2 evidence. | [`tracks/C-berka/STATUS.md`](tracks/C-berka/STATUS.md) |
| **D** | Codex unconstrained control, a separate repository (`sharealedger-org/ledger-lab-control`) | Official attempt complete and PASS by Codex's report (`3c50c19`); comparison not yet made. | [`tracks/D-control/STATUS.md`](tracks/D-control/STATUS.md) |

## Where things are

- **Working copy:** the Dropbox folder, on branch `main`. There are no long-lived feature branches; `aim-three-model` was merged into `main` on 2026-11-12 and is kept only as history. `trackC` and `trackD` branches are old; their content is on `main` or is stalled.
- **Baseline for the control comparison:** annotated tag `companion-measured-baseline-20261111` on `c3667e4`. Record: [`docs/COMPANION_BASELINE_RECORD_2026-11-12.md`](docs/COMPANION_BASELINE_RECORD_2026-11-12.md).
- **Open work:** [`TODO.md`](TODO.md), grouped by track. **Dated log:** [`SESSION_HANDOFF.md`](SESSION_HANDOFF.md) (newest first; older entries in `docs/archive/`). **Evidence:** [`docs/VERIFICATION_AUDIT_REGISTER.md`](docs/VERIFICATION_AUDIT_REGISTER.md). **Results for a newcomer:** [`docs/CURRENT_RESULTS.md`](docs/CURRENT_RESULTS.md) (needs a refresh). **All documents:** [`docs/README.md`](docs/README.md).
- **Rules:** [`AGENTS.md`](AGENTS.md). Scala is the engine, Bash orchestrates, Python only after owner approval for post-engine analysis.

## Ground rules for sessions

1. Read this page and one track page. Open other files only when a track page points to them.
2. Before ending a session, update that track's `STATUS.md`: last evidence commit, next action, decisions waiting. Add a short handoff entry titled with the track id (for example `(TRACK B1)`).
3. New to-dos go in `TODO.md` under the track heading, not in the handoff.
4. Track D material from the control repository is read as a digest from the last recorded revision (see its page), and every reading is logged.

## Waiting on the owner (all tracks)

- Push `main` and the baseline tag (22 local commits; nothing pushed).
- Pointer text and branch for the control repository's handoff (repository name, tag, commit; no results).
- Whether `AGENTS.md` "Before Editing" gains a line pointing here.
- Track decisions are listed on each track page.

## Next session

Session 2 of [`docs/PROJECT_ORGANIZATION_PLAN.md`](docs/PROJECT_ORGANIZATION_PLAN.md) is done in part (the merge). Remaining in order: refresh `CURRENT_RESULTS.md` and add one-page results (B1); draw the three graph sheets from archived CSVs (B1); Track D digest and comparison plan (D); three-model steps once D9 is decided (B2).
