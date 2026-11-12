# Ledger Lab To-Do List

This is the single list of open work. `SESSION_HANDOFF.md` is a dated log and is long; do not leave to-dos only there. When an item is done, tick it and add the date and a pointer to the evidence. Owner decisions are marked **Owner**.

Seeded 2026-11-11 from the handoff entries. On the same day the handoff was trimmed and its older entries moved unchanged to `docs/archive/SESSION_HANDOFF_ARCHIVE_2026-11.md`; the open items found in them are in the last two sections.

## Explainer and outside communication

- [ ] **One-page explainer** (`docs/LEDGER_LAB_ONE_PAGE_EXPLAINER.html`): show where each ledger sits and what it is called: the general ledger (GL, CCP1), the payments subledger (called "Instrument Ledger" or `LDGR` in the repo), the other sub-ledgers, and how they relate. **Owner:** pick one name for outside readers and say whether the repo docs and file names follow.
- [ ] **Explainer footer:** add the Sharealedger web address and the repo address. **Owner:** supply both; they are not recorded in the repo.
- [ ] **Explainer graph:** check it in the browser used for showing it (an earlier version was cut off when the SVG scaled by `viewBox`).
- [ ] **Explainer numbers:** review the assumed unit rates and query volumes behind the bowl before the page is shown as more than an illustration (`docs/COST_GRAPH_DESIGN.md`, "Mix graph: model run").

## Cost graphs (Paper 1)

- [ ] Engine and Process graphs have no model run. Engine needs the Track D corpus run; Process needs CCP4 and CCP5 on the corpus (`docs/EXPERIMENT_STATE_2026-11-11.md`, section 2).
- [ ] Replicate each cost point three times before any cost is quoted. **Owner:** which layout is the reference.
- [ ] Measure an annual-granularity (Run A) corpus run, or keep it labelled as an assumption.

## Three-model experiment (Paper 2)

- [ ] **Owner:** decide D9, net income (proposed: net expenditure per agency and fiscal year, `EXP*` debits only; `docs/THREE_MODEL_EXPERIMENT_PLAN.md`). Reopened 2026-11-12: raw `apa_revenue` exists (17.75M rows) and the TTL has a revenue path with no real-corpus run, so "no revenue" is a Gold limit, not a corpus limit. Candidates: reserves per fund (cash or encumbrance basis) or the statement tie-out (plan section 8).
- [ ] **Owner:** pick the first anchor figure for the statement tie-out (plan section 8). The ACFR is published for FY2002 to FY2025; FY2012 sections G and L were read. See `docs/VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md`.
- [ ] **Owner:** state the basis for any reserves or net income target (cash/budget, encumbrance, modified accrual); the same fund differs by basis in the ACFR.
- [ ] **Owner:** say where the raw `VARawFiles` (including the 14 `Budget FY20NN.csv`) are; they were not found on the M1 Mac. Budget has no Gold stage and no workload declared.
- [ ] Draft a one-fund, one-year ACFR-to-corpus mapping (General Fund FY2012) with the difference itemised; read ACFR sections K and O first.
- [ ] Compare USAspending federal awards to Virginia state recipients (FY2012) with the ACFR federal revenue line and the `apa_revenue` Federal source rows.
- [ ] Run APA revenue through the Scala TTL for one year (for example FY2012) so Gold has `REV*` accounts; check GL, LDGR and raw revenue sums agree. Declare workload and CKB placement per `docs/AGENTIC_ARCHITECTURE_GATES.md` first.
- [ ] **Owner:** decide whether the account text stays in the verbalized records, and whether the period text (`0016/01`, `2015/07`) is normalised.
- [ ] Step 5, IL-Native on fixtures, then on Gold.
- [ ] Step 6, choose the model (Apache 2.0; check version and advisories first).
- [ ] Step 7, fine-tune, score, write `data/test_evidence/three_model_experiment_results.csv`.
- [ ] Re-run AIM1, AIM3, AIM4 on the final views.

## Repo and machine housekeeping

- [ ] Commit the 2026-11-11 documentation changes on `aim-three-model` (the `main` merge is already committed).
- [ ] **Owner:** decide when to merge `aim-three-model` into `main`.
- [ ] **Owner:** re-enable Spotlight with `sudo mdutil -i on /` (turned off for timing test 2).
- [ ] Hand Track D to Codex (`~/workspace/va_track_d_handoff/`); it needs the manifest, not the Gate 0 example.
- [x] `SESSION_HANDOFF.md` trimmed from 3,794 to about 135 lines on 2026-11-11; older entries moved unchanged to `docs/archive/SESSION_HANDOFF_ARCHIVE_2026-11.md` (byte-checked: the two files together equal the original).
- [ ] Other documents cite old handoff entries by name (`docs/CORPUS_SCHEMAS.md`, `docs/FULL_CORPUS_RUN_CHECKLIST.md`, `docs/VERIFICATION_AUDIT_REGISTER.md`, Scala comments in `CorpusProfile.scala` and `CKBViews.scala`). The handoff header says these now mean the archive. Update the links if wanted.

## Engine and cost-curve items (from the handoff, collected 2026-11-11)

Source entries are in `SESSION_HANDOFF.md` (run 3 and run 4) or in the archive (the others). Check the source before acting; some were written before runs 3 and 4.

- [ ] **Owner:** fuse steps `3`, `20`, `21` into one CKB pass? It changes what the CCP1 cost point measures, so it needs a decision first (archive, "Fusing steps 3, 20, 21").
- [ ] Estimate what the full corpus would need without CKB (cloud sizing and time). A candidate analysis; nothing measured (archive, 2026-11-10 to-do).
- [ ] Consider initializing binary-search paths in the CKB to speed it further. Not discussed; declare the workload and CKB placement per `docs/AGENTIC_ARCHITECTURE_GATES.md` first (archive, 2026-11-10 to-do).
- [ ] Failure path not run: a failing year should stop new years.
- [ ] Designed test of the `FY<yy>` symlink swap while `5v` reads. Runs 3 and 4 committed chains during `5v` with no harm seen, but that was not a designed test.
- [ ] Compare the merged SJE across runs (only the SHA-256 list `gold/MANIFEST_SJE.csv` exists; the files themselves were deleted).
- [ ] A replicate of the run-2 layout on a quiet machine (runs 3 and 4 are replicates of each other, not of the baseline).
- [ ] Optional: stamp "pipeline complete" and "chain end" in the wall-clock CSV so the unmetered post-pipeline tail is recorded every run (about 54% of chain wall in run 3; the `sje_balance` awk is most of it). **Owner:** the awk controls stay in Bash.
- [ ] Windows validation is still pending (archive, "Windows Validation (pending)").

## Model tests (AIM) from the archive

- [ ] **AIM4** (STL decomposition per agency x NIGP class, Q4 test, majority-of-agencies rule) and **AIM5** agency reconciliation (`scripts/fit_agency_name_classifier.py` exists), then AIM2: not run to the locked protocol. Python was approved earlier for these; restate the approval and ask "could Scala do this?" before writing anything (`AGENTS.md`).
- [ ] AIM3 caveat: churn rises from 24% to 33% over FY2012-FY2015, so some of the 0.7527 hold-out signal could be a time trend, not vendor information. The script does not separate the two. The hold-out also uses only four training years and four features.
- [ ] The AIM3 walk-forward uses 4 windows in place of the locked 7. **Owner:** agreed "for now", not independently confirmed (archive, 2026-11-11 PLAN entry).

## Low priority and unchecked

- [ ] Berka authors' terms of use (Track C is illustrative only, not Paper 2 evidence).
- [ ] Older unresolved items last mentioned in the 2026-11-04 to 2026-11-06 entries, not re-checked since: stale seed ledger gives a ledger sum of 6.00; the PRICE guard quarantines 17 rows, not 4 (empty-price rows); Windows `commit_attempt` copies instead of symlinking. Check whether later work resolved them before spending time.
