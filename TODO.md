# Ledger Lab To-Do List

This is the single list of open work. `SESSION_HANDOFF.md` is a dated log and is long; do not leave to-dos only there. When an item is done, tick it and add the date and a pointer to the evidence. Owner decisions are marked **Owner**. Each section heading names its track (see [`STATUS.md`](STATUS.md)); the track page says what comes first.

Seeded 2026-11-11 from the handoff entries. On the same day the handoff was trimmed and its older entries moved unchanged to `docs/archive/SESSION_HANDOFF_ARCHIVE_2026-11.md`; the open items found in them are in the last two sections.

## Explainer and outside communication (Track B1)

- [x] 2026-11-12: **One-page explainer** updated with 2026-11-12 naming standards (P1: Events to P6: Permuted, CCP1/CCP2) and staged to TDS evidence folder (`04-one-page-explainer.html`).
- [ ] **Explainer footer:** add the Sharealedger web address and the repo address. **Owner:** supply both; they are not recorded in the repo.
- [x] 2026-11-12: **Explainer graph:** viewBox / transform scale checked (0.9474 scaling fits canvas cleanly).
- [ ] **Explainer numbers:** review the assumed unit rates and query volumes behind the bowl before the page is shown as more than an illustration (`docs/COST_GRAPH_DESIGN.md`, "Mix graph: model run").

## Cost graphs (Track B1)

- [x] 2026-11-12: **Refresh `docs/CURRENT_RESULTS.md`** with completed Run 4 evidence, baseline tag, standardized naming, and "measured, shows, does not show" table per cost point.
- [x] 2026-11-12: **TDS Paper Revision 2 Evidence Folder** created at `docs/tds-evidence/paper-1-rev2-ledger-lab/` (README index, `01-measured-results-summary.md`, `02-mix-cost-curve-model.md`, `03-ckb-cost-model.xlsx` 4-sheet model workbook, `04-one-page-explainer.html`, `05-limits-and-phase2-roadmap.md`).
- [ ] **Draw the Process, Mix and Engine one-page sheets** from the archived CSVs. No script without owner approval (`AGENTS.md`); `awk` numbers plus static pages first.

- [ ] Engine and Process graphs have no model run. Engine needs the Track D corpus run; Process needs CCP4 and CCP5 on the corpus (`docs/EXPERIMENT_STATE_2026-11-11.md`, section 2).
- [ ] Replicate each cost point three times before any cost is quoted. **Owner:** which layout is the reference.
- [ ] Measure an annual-granularity (Run A) corpus run, or keep it labelled as an assumption.
- [ ] **Owner:** P6 basis. The docs say $22.4M; `03-ckb-cost-model.xlsx` computes $22,107,000 from a hardcoded 80,000,000 GB, and its note G7 still says $22.4M. Decide the GB figure and its source, then correct the workbook, `02-mix-cost-curve-model.md` and the explainer label (2026-11-12 handoff, item 1).
- [ ] **Owner:** raw storage in the bowl. P2–P5 charge the full 56.69 GB raw SJE store (P3: $16.12 of $16.29). Decide whether raw storage stays in the cost points, and document the choice in `02-mix-cost-curve-model.md` (2026-11-12 handoff, item 2).
- [ ] Pull each unit rate into `docs/tds-evidence/paper-1-rev2-ledger-lab/RATE_SOURCES.md` from a dated source (price page, date retrieved, unit). Until then the rates are assumptions. Needs web access.
- [ ] Add a conservative-case column to `Model_Parameters` in `03-ckb-cost-model.xlsx` (scan $0.005/GB, reconciliation $0) so both cases are in the workbook, not only the text.
- [ ] Create a named test for full-corpus determinism (byte-identity of the two Monthly executions, per `gold/MANIFEST.csv`). Today only fixture-level determinism tests are named (2026-11-12 handoff, item 3).


## Three-model experiment (Track B2, on hold 2026-11-12)

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
- [x] 2026-11-13: Re-run AIM1 (all 14 years), AIM3 (hold-out C = 0.7527, p = 0.001, PASS), AIM4 (FAIL, 2/5 Q4-ratio) on the Gold final views (Intel Mac). Evidence: `data/test_evidence/AIM1_noninjectivity_all_years.csv`, `data/test_evidence/AIM3_survival_windows.csv`, `data/test_evidence/AIM4_seasonality_results.csv`.
- [x] 2026-11-13: **AIM3 balance-derived baseline** — `scripts/fit_vendor_survival_balance.py` on Gold LDGR files: balance C = 0.7783 (commitment 0.7527, delta +0.0256), p = 0.001. Time-trend control C = 0.4999 (FAIL). The view signal is present in posted LDGR totals; the view does not show additional information over posted totals on this hold-out (training splits differ). Evidence: `data/test_evidence/AIM3_balance_baseline.csv`. A matched-split comparison is the open next step. Also add to `docs/tds-evidence/paper-2-rev1-ledger-lab/02-aim-test-suite-results.md`.
- [ ] **Requested by TDS Paper 2 (2026-11-13):** AIM3 walk-forward on the final views. Done: 2026-11-13 walk-forward also confirmed (mean C = 0.7629, 4 windows). Update the Paper 2 text once the balance baseline result is recorded.
- [x] 2026-11-13: Reconcile the eVA PO counts. `TEST_DESIGN.md` §8 corrected: "133.46M" was the all-family total, not PO alone. PO: 18,407,581 parsed / 18,081,738 accepted (all 17 files, `CORPUS_FINDINGS.md`); pipeline FY2003–FY2016: 15,603,041 PO source rows (audit register). eVA PO file range corrected to FY2001–FY2018 (17 files, no FY2009; FY2001/2003/2004/2005 stubs or partial). FY2002 is not usable (64,743 rows, majority outside FY); the pipeline eligible-year rule (FY2006–2008 + FY2010–2016) applies regardless.

## Track D: control comparison (Track D)

- [ ] **Owner:** pointer in the control repository's `SESSION_HANDOFF.md`, written when this repository is opened to Codex (owner, 2026-11-12): repository name, tag `companion-measured-baseline-20261111` (`c3667e4`), and **the SHA of the commit that holds the results evaluation of the control's work** (the comparison, once written), so that Codex sees the assessment of its own work. The evaluation commit does not exist yet; fill in its SHA then. Branch and wording are the owner's call. Not written.
- [ ] Digest the control repository from `3c50c19` at the start of each Track D session; update the revision in `tracks/D-control/STATUS.md` and log the reading (`docs/COMPANION_BASELINE_RECORD_2026-11-12.md` section 2).
- [ ] **Owner:** approve [`tracks/D-control/COMPARISON_PLAN.md`](tracks/D-control/COMPARISON_PLAN.md) (Part A compare; Part B novel-permutation challenge before exposing Codex).
- [ ] Reconcile before any whole-run comparison: revenue, PO quarantine sets, year populations, timed scope (baseline record section 3). Hardware is the same class, both Intel, so it is not a difference. Write `tracks/D-control/COMPARISON_PLAN.md` when the first item is closed.
- [ ] When the official attempt is accepted: one-page comparison on reconciled populations. The Engine graph waits for it.
- [ ] **Owner:** when to give Codex access to this repository for the informed phase.
- [ ] **Owner:** accept or change the Phase 2 design in `tracks/D-control/STATUS.md` ("Phase 2 design"): Bob extends the control, Codex extends the CKB, questions formed on the CKB side and committed at gates Q2-draft and Q2-final (with the evaluation criteria), then given to the control. Measure (owner, 2026-11-13): total processing of Phase 1 + Phase 2 together on each system, every Phase 1 change noted; Bob proposes cold-total and marginal numbers, a capabilities axis, Phase 1 regression checks and a change log. Decide: accept those refinements, and whether a post-Q2 cold set goes to both builders.
- [ ] **Owner:** run the memory test (plant a fact, new session, ask) for Codex first (then Bob and the other Codex instance), including how its memory can be controlled; log the result as evidence. If satisfied, engage Codex in this repository to discuss the next-stage (Q2-draft) questions, and log that exposure in `docs/COMPANION_BASELINE_RECORD_2026-11-12.md` section 2. Engage Bob again before Q2-draft is finalised (plan in `tracks/D-control/STATUS.md`, "Sequence the owner plans").
- [ ] Before Phase 2: fetch `ledger-lab-control` to `origin/main`, run the digest from `3c50c19`, tag the control's final accepted Phase 1 revision. Isolation (owner, 2026-11-13): no fresh clone and no separate machine; Bob builds in the existing control working copy with the hook and rules installed (uncommitted until agreed); open the control folder alone. Done 2026-11-13: control folder trusted (owner); the corpus and output roots from the control's environment notes added to `.bob/hooks/allowed_roots.txt` (paths are on the Intel host, `/Users/ktwitchell001/workspace/...`; they do not exist on this M1 Mac, so confirm which machine runs Bob's control sessions). Still to do: run the startup probes and the memory smoke test in the live Bob control session (owner, in development, on the Intel host). Done later 2026-11-13: owner agreed the package; control runs on the Intel host; header fixed; `scripts/run_control.sh` launcher added (owner chose it over a read-only mount); Bob's own fetch and digest done and logged (third reading); Bob's concurrence entry written in the control handoff. Still uncommitted (owner's step). Python in the control repository is fine (owner).



## Repo and machine housekeeping (all tracks)

- [x] 2026-11-12: `aim-three-model` merged into `main` (`d356143`); the working copy is on `main`. Only `SESSION_HANDOFF.md` conflicted.
- [x] 2026-11-12: root `STATUS.md`, `tracks/*/STATUS.md` and `docs/README.md` created (session 1 of `docs/PROJECT_ORGANIZATION_PLAN.md`).
- [x] 2026-11-12: `main` and the tag `companion-measured-baseline-20261111` pushed (`main` at `ef78576`).
- [x] 2026-11-12: `AGENTS.md` "Before Editing" now says to read `STATUS.md` first.
- [x] 2026-11-12: `aim-three-model` deleted (local and remote; it was merged into `main`). `trackC` and `trackD` stay as history.
- [ ] **Owner:** re-enable Spotlight with `sudo mdutil -i on /` (turned off for timing test 2).
- [x] Hand Track D to Codex: done earlier; the control repository and its Intel host are running (see `tracks/D-control/STATUS.md`).
- [x] `SESSION_HANDOFF.md` trimmed from 3,794 to about 135 lines on 2026-11-11; older entries moved unchanged to `docs/archive/SESSION_HANDOFF_ARCHIVE_2026-11.md` (byte-checked: the two files together equal the original).
- [ ] Other documents cite old handoff entries by name (`docs/CORPUS_SCHEMAS.md`, `docs/FULL_CORPUS_RUN_CHECKLIST.md`, `docs/VERIFICATION_AUDIT_REGISTER.md`, Scala comments in `CorpusProfile.scala` and `CKBViews.scala`). The handoff header says these now mean the archive. Update the links if wanted.

## Engine and cost-curve items (Track B1; from the handoff, collected 2026-11-11)

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

## Model tests (AIM) from the archive (Track B2)

- [ ] **AIM4** (STL decomposition per agency x NIGP class, Q4 test, majority-of-agencies rule) and **AIM5** agency reconciliation (`scripts/fit_agency_name_classifier.py` exists), then AIM2: not run to the locked protocol. Python was approved earlier for these; restate the approval and ask "could Scala do this?" before writing anything (`AGENTS.md`).
- [ ] AIM3 caveat: churn rises from 24% to 33% over FY2012-FY2015, so some of the 0.7527 hold-out signal could be a time trend, not vendor information. The script does not separate the two. The hold-out also uses only four training years and four features.
- [ ] The AIM3 walk-forward uses 4 windows in place of the locked 7. **Owner:** agreed "for now", not independently confirmed (archive, 2026-11-11 PLAN entry).

## Low priority and unchecked (mixed; Track C for Berka)

- [ ] Berka authors' terms of use (Track C is illustrative only, not Paper 2 evidence).
- [ ] Older unresolved items last mentioned in the 2026-11-04 to 2026-11-06 entries, not re-checked since: stale seed ledger gives a ledger sum of 6.00; the PRICE guard quarantines 17 rows, not 4 (empty-price rows); Windows `commit_attempt` copies instead of symlinking. Check whether later work resolved them before spending time.
