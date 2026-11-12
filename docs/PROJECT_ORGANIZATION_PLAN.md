# Project organization plan (proposal, not yet adopted)

Drafted 2026-11-12 by Bob at the owner's request. Purpose: let a session start in a few thousand tokens instead of reading a 3,718-line handoff, and make the baseline results approachable. Nothing below is done except where marked. Each step needs the owner's yes before it is built; changes to `AGENTS.md` and any new Python need explicit approval.

## 1. What the work actually is

| Track | What | State | Where it lives |
|---|---|---|---|
| A | P&C insurer, 25.5 billion rows. Primary corpus for the papers. | Outside this repo; not tracked here. **Question for owner: does it need a status page here?** | Unknown to this repo |
| B | Virginia public data. The open, checkable version of the design. | Active. Two streams, below. | This repo, `main` |
| B1 | Paper 1: cost curve (CCP1 to CCP5, engine, runs) | Baseline measured and tagged (`companion-measured-baseline-20261111`). CCP4/CCP5 corpus runs and 3 replicates not done. | `main` |
| B2 | Paper 2: AIM tests and the three-model experiment | Steps 1 to 4 done; D9 decision blocks 5 to 7. | Branch `aim-three-model` |
| C | Berka bank data | Stalled; left out of Paper 2 by owner decision. | Branch `trackC` |
| D | Codex unconstrained control (cost-curve comparator) | Official run in progress on Intel host. Separate repo `ledger-lab-control`, with its own handoff and journal. | Other repo, plus `docs/TRACK_D_NOTES.md` here |

## 2. Recommendation: tracks as folders, not branches

- **Branches hide work.** Today `main` and `aim-three-model` each hold a rewritten handoff, and a local `main` commit sat unpushed while `origin/main` moved 9 commits. Branches should last days and end in a merge.
- **Use `tracks/<id>/` on `main`.** Each holds `STATUS.md` (one page: state, last evidence commit, next action, owner decisions waiting) and `HANDOFF.md` (the newest entry only; older entries move to `docs/archive/`).
- **Track D is the exception that proves the rule.** It is already its own repository. Here it needs only a digest, so Bob learns from Codex without re-reading 130 KB of journal.
- **Root `STATUS.md`** is the one file a new session reads first: the table above, last commit per track, and the next action per track. About one page.

## 3. Track D as a formal track

- `tracks/D/STATUS.md`: control repo name and last revision Bob read (today `51f16a6`), official-run state, comparison readiness.
- **Digest procedure.** Each session, run `git log <last-read>..origin/main` on the control repo and read only the new entries. Record the new SHA. This is the context saver.
- `tracks/D/COMPARISON_PLAN.md`: the comparability items now in `COMPANION_BASELINE_RECORD_2026-11-12.md` section 3 (streams, years, PO recovery, timed scope, hardware), each with the check that would close it.
- **Pointer in the control handoff**: repo name, tag, commit; no results. Branch, wording and timing are the owner's call. Not written.
- **Two phases kept apart**: original companion against control (now); informed companion against original (later). `COMPANION_BASELINE_RECORD` section 2 logs every informed change.
- When the official result lands: a one-page comparison with the reconciled populations, not a diff of totals.

## 4. Work order (each line is roughly one session)

| # | Session goal | Output | Needs from owner |
|---|---|---|---|
| 1 | **Skeleton.** Root `STATUS.md`; `tracks/` folders with `STATUS.md`; move handoff entries older than the newest 3 to `docs/archive/`; `docs/README.md` doc map marking each of the 37 files live, reference or archive; one line in the audit register header saying how to find an entry. | A session can start from `STATUS.md` alone. | Yes to folders vs branches; yes to editing the `AGENTS.md` "Before Editing" line to say read `STATUS.md` first |
| 2 | **Merge `aim-three-model` into `main`.** It already archived older handoff entries and added `TODO.md`, `EXPERIMENT_STATE_2026-11-11.md`, the cost-graph model and the explainer. Reconcile its handoff with `main`'s new entry. Push `main` and the baseline tag. | One trunk. | Push approval; confirm merge now, not after D9 |
| 3 | **Results for a newcomer.** Refresh `CURRENT_RESULTS.md` (still says "as of `9d54cd8`, a final run in progress"). Add a one-page "what was measured, what it shows, what it does not" per Paper 1 point. | Approachable baseline summary. | Review of wording on claims |
| 4 | **Graphics, one page each.** The three graphs (Process, Mix, Engine) from `COST_GRAPH_DESIGN.md`, drawn only from archived CSVs. Charts made in earlier chat sessions were never saved to the repo. | Static one-pagers committed under `docs/figures/`. | See Python note below |
| 5 | **Three-model experiment.** Decide D9 and verbalizer visibility; then steps 5 to 7 (IL-Native, base model with Apache 2.0 check, fine-tune and score). Fold in the Virginia ACFR survey (basis for reserves; statement tie-out as a later paper test). | Results file or a documented block. | D9 decision |
| 6+ | **Track D comparison** when the official result is accepted; **CCP4/CCP5 and replicates** if commissioned. | Comparison page; cost points. | Commissioning decision |

## 5. Rules that keep sessions small

1. Start with root `STATUS.md`, then only the track you are working on. Do not read the full handoff or register.
2. End every session by updating that track's `STATUS.md` (last commit, next action, decisions waiting).
3. Handoff entries stay short; evidence goes to the audit register, results to `CURRENT_RESULTS.md`.
4. One working checkout per branch, written down in `STATUS.md`. Today the Dropbox checkout sits on `aim-three-model` and the `main` worktree is `~/workspace/lab-main-wt`. A `.git` inside Dropbox is a sync risk; consider moving the working copy out.
5. Control-repo material is read as a digest with a recorded SHA; exposure stays logged.

## 6. Python and the graphics step

`AGENTS.md` allows Python for post-engine analysis only after the owner approves the specific work. Two routes for session 4:
- **No script:** compute the numbers with `awk` over the archived CSVs (as the experiment-state page did) and draw static SVG pages by hand or with the chart tool.
- **Small script:** a plotting script reading only the archived summary CSVs. Needs approval, and it must say it is not part of the measured path.

I recommend the no-script route for the first sheets.

## 7. Decisions needed from the owner

1. Folders under `main` rather than long-lived branches?
2. Merge `aim-three-model` into `main` now (session 2), or after D9?
3. Push `main` and the baseline tag now?
4. Pointer text and branch for the control's handoff.
5. Does Track A need a status page here?
6. Graphics route: no script, or approve a plotting script?
7. May `AGENTS.md` "Before Editing" gain a line pointing to `STATUS.md`?
