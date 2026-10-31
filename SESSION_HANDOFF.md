# Ledger Lab Handoff

Updated 2026-10-31. This file is a concise state snapshot; use the linked design contracts for details.

---

## 2026-10-31 Windows Validation, PO Data Pipeline, and Zenodo Archive

**Context.** This session ran on the Windows machine holding the full Virginia PO dataset.
It completed the PO attribute extraction pipeline, established the Zenodo data archive,
and cleaned the PO attribute extraction logic. The downstream PyTorch ML work (Tests 2 and 3)
remains the next milestone.

**What was done.**

1. **Pulled and synced main** — picked up 55 commits including the macOS measurement fixes
   (Step 3 `records_read=0` repair, Step 2 header mismatch fix) from the prior session.

2. **Committed synthetic FY03 PO fixture** (`data/VAPOData_fixture/VA_opendata_FY2003_small.txt`):
   - 10 synthetic rows matching the FY03 expense fixture vendor/agency (VCU, BLACKWELLS BOOK SERVICES).
   - Added `--with-po-fixture` flag to `run_repo_fixture_smoke_test.sh` so any machine can exercise
     the transStandardize PO branch without the real external data.

3. **Ran PO attribute extraction** (`extract_po_attributes.py --by-year`) over the full
   15.6M-record VA eVA dataset (FY2002–FY2016, 13 years):
   - Output: `data/VAPOData/vendor_po_attributes_by_year.csv` (210,496 vendor-year rows, git-ignored).
   - Fixed a validation bug: the original `len(nigp_raw)==3` check fired on the full 5-digit NIGP
     code before the `[:3]` class-prefix slice, zeroing all NIGP coverage. Fixed to `nigp_raw[:3].isdigit()`.
   - Added column-shift cleaning: `VENDORSTATE` values that are not 2-letter alpha codes (address
     strings, UOM values) are nulled. Result: 99% NIGP coverage, 94% geo coverage on vendor-year rows.

4. **Ran CAR change detection** (`car_detect_changes.py`) → `data/VendorUpdate.csv` (committed):
   - 64,850 date-effective attribute changes: 63,238 ATTR_NIGP, 1,612 ATTR_GEO.
   - Geo change count dropped from 6,115 (dirty) to 1,612 (clean) after the column-shift fix.

5. **Downloaded full Virginia APA expenditure/revenue/budget data** to `data/VARawFiles/`
   (git-ignored): 350 files, 20.3 GB, FY2003–FY2016, all still live on datapoint.apa.virginia.gov.

6. **Established Zenodo archive** for long-term reproducibility:
   - DOI: `10.5281/zenodo.23846362` (upload in progress as of session end)
   - Contains: `VARawFiles/` (APA exp/rev/budget FY2003–FY2016) + `VAPOData/` (eVA PO FY2001–FY2018)
   - Virginia state government records are public domain; NIGP descriptions not redistributed separately.
   - Known gaps: FY2005 and FY2009 eVA files are zero-row stubs (documented in scripts and RUNTIME_DATA_LAYOUT.md).
   - DOI reference committed to `RUNTIME_DATA_LAYOUT.md`, `download_va_data.py`, `download_va_po_data.py`.

**Papers reviewed this session (both read in full).**

- [`org-members/research/01-minimum-cost-curve.md`](../org-members/research/01-minimum-cost-curve.md) — Paper 1
- [`org-members/research/02-pre-hierarchy-event-repositories.md`](../org-members/research/02-pre-hierarchy-event-repositories.md) — Paper 2
- [`org-members/research/PAPER_02_NOTES.md`](../org-members/research/PAPER_02_NOTES.md) — Paper 2 working notes with VA test design

**What ledger-lab must produce for Paper 1 (Minimum Cost Curve).**

Paper 1's VA empirical contribution is the **Instrument Pivot Theorem demonstration** (§3.5):
- Vendor ID = Instrument ID; LDGR = balance store $\mathcal{L}$; VendorMaster.csv = CAR.
- Run C1 (Steps 2,3 — post only) → cost_surface.csv row: compute_s, storage_bytes, master_file_count=1.
- Run C2 (Steps 2,3,5 — post + views) → cost_surface.csv row with additional master files.
- Show C1→C2 delta = measured marginal cost of one reporting view.
- Add a CAR attribute to VendorMaster (e.g. instNIGPClass from vendor_po_attributes_by_year.csv),
  re-run C2 → zero new balance records, zero reconciliation cost. That ratio vs. key-embedded
  alternative ($(1+v)^m - 1$ formula) is the empirical proof of the theorem.
- Paper 1 conclusion explicitly calls for an agentic cost-curve sweep: agent changes ViewSpec,
  re-runs pipeline, observes costs. `run_cost_curve_sweep.sh` and `cost_surface.csv` are designed
  for exactly this.
- **Three C1 replicates** are needed to establish measurement stability before C2.

**What ledger-lab must produce for Paper 2 (Pre-Hierarchy Event Repositories).**

Paper 2 proposes a two-corpus structure: insurance (proprietary, 25.5B records) + VA (public,
reproducible). The VA corpus carries the scientific reproducibility and direct falsifiability weight.
Four tests, all against VA FY2003–FY2016:

| Test | What it proves | Method |
|------|---------------|--------|
| **Test 1** | Direct empirical count of Theorem 1 non-injectivity | Python: for every PO line, construct its posted image (Agency+Object+Amount); count how many distinct PO patterns map to the same key. Produce the distribution. No ML. |
| **Test 2** | Vendor substitution undetectable from posting (RQ4) | ML: given commitment event (Agency, NIGP, original vendor, price, date), predict whether a *different* vendor fulfilled the order. Balance-trained model literally cannot form this prediction — the feature does not exist. |
| **Test 3** | PO modification rate → budget deviation year N+1 (RQ2/RQ3) | ML: Layer 2 = aggregate expenditure ratios; Layer 3 = PO modification sequence + timing. If Layer 3 wins, clean empirical RQ2/RQ3 proof. |
| **Test 4** | 14-year longitudinal robustness (RQ3) | Train on years 1–7, predict year 8. Repeat for all 7 windows FY03–FY16. 7 independent replications. |

**Critical open question before Tests 2–4 can be built:**

The PO–expenditure **join key is unverified**. Expenditure files use `Agency/Fund/Object/SubProgram`
coded keys, not PO numbers. The PO files have `PONUMBER`. Whether a `PONUMBER → expenditure record`
linkage exists in the full-year data must be confirmed before Tests 2–4 can be scoped.
See PAPER_02_NOTES.md open question #2.

**Next steps — in order.**

1. **Confirm Windows pipeline compiles**: run `bash scripts/run_repo_fixture_smoke_test.sh --show-data`
   on this machine. sbt 1.x and Java 21 are confirmed available.
2. **Run 3× C1 replicates** against full FY03 data in `data/VARawFiles/` to establish measurement
   stability and generate Paper 1 cost-surface baseline rows.
3. **Run C2** (add Step 5) against same FY03 data → measure C1→C2 marginal cost.
4. **Build and run Paper 2 Test 1** (Python counting script, no ML) → produces the non-injectivity
   distribution table for Paper 2 §5.1b, and simultaneously checks if the PO→expenditure join key
   exists in practice.
5. **Install PyTorch** (2.15.0 available for Python 3.14 on Windows — confirmed) and build
   Tests 2–4 once the join key question is resolved.
6. **Build VendorMaster_enriched.csv** from PO attributes + existing VendorMaster.csv so
   `car_detect_changes.py` has a real instTypeID baseline (currently defaults all vendors to EXP).

## 2026-10-31 Measurement Correction and Downstream PyTorch ML Strategy

**Context.** This session repaired the critical measurement gaps on the macOS baseline and established the strategic integration of a downstream PyTorch-based machine learning prediction framework (Test 2 and Test 3 for Paper 02) to be executed on a standard PC/Windows machine containing the full Virginia purchase order (PO) commitment dataset.

**What was done.**

1. **Repaired Step 3 Metrics (`records_read=0` macOS blocker)**:
   - Identified that `grep -i "Records Read:" | tail -1` on the `post.scala` module matched both the `Journal Records Read` and `Ledger Records Read` outputs, greedily returning `0` as the final match.
   - Updated `scripts/run_pipeline.sh` with a multi-layered fallback strategy parsing `Journal Records Read:` and `Ledger Records Written:` directly from standard output, making metrics extraction completely bulletproof across all environments.
2. **Resolved Step 2 Header Mismatch (761 vs 760 rows)**:
   - Eliminated the historical dummy element initialization pattern in `transStandardize.scala` across `poRaw` and `transRaw` (Expenses and Revenue) buffers, which was inflating the JVM-logged read counts by 1.
   - Replaced `count_file_rows` (which counts raw text lines including headers) with `count_data_rows` in `run_pipeline.sh` and `file_data_rows` in `write_canonical_evidence.sh` for Step 2's raw input. Both Step 2 and Step 3 now report perfectly synchronized, exact data row metrics (e.g., 760 and 1520 rows).
3. **Formulated the Downstream PyTorch Plan**:
   - Outlined how the Scala-generated output partitions (Layer 3 raw PO events vs. Layer 2 posted GL balances) will act as the training inputs for a compact PyTorch model on a PC.
   - Defined **Test 2 (Vendor Substitution Predictor)** and **Test 3 (Budget Deviation Predictor)** as the operational proofs of Theorem 1 (Posting Non-Injectivity).

**Next steps on the Windows machine containing the PO data:**

1. Run PO Attribute Extraction with `--by-year` flag to generate yearly PO attributes.
2. Build the CAR Update Cycle (`car_detect_changes.py` and temporal as-of-date join in Scala `VendorMaster` and Python Step 5).
3. Set up the PyTorch Dataset Loader and Model script on Windows to run Tests 2 and 3.
4. Execute C1 replicates to establish hardware cost-surface baseline data.

---

## 2026-10-28 Contributor Onboarding Session

**Context.** This session addressed the question of how to hand the repository off to a broader
technical community — likely students working under a professor's direction if the papers are
judged worthwhile. The developer (Kip) is stepping back from being the primary implementer.

**What was done.**

- Wrote and committed [`CONTRIBUTOR_ONBOARDING.md`](CONTRIBUTOR_ONBOARDING.md) (commit `3883d4e`).
  This document explains the two research claims, the CKB architecture pattern, honest
  implementation status (including the known `records_read=0` metric breakage), the cost-curve
  agent goal and what it needs to be true, the priority work items, and what AI assistants will
  get wrong about the repository.
- Linked it from the `README.md` Contributing section.

**Key framing established for incoming contributors.**

> The architecture is sound and partially verified at fixture scale. The measurement substrate
> needed to produce comparable cost points is not yet reliable. The next milestone is not a new
> financial feature — it is making the existing pipeline emit trustworthy evidence that an agent
> can read and interpret without human intervention.

**Priority work items for the next contributor (unchanged from previous session).**

1. Fix Step 3 process metrics emission — `records_read=0` on macOS renders the cost surface
   untrustworthy. This is the blocker for all cost-curve work.
2. Validate evidence schemas on failed/partial/no-op runs.
3. Define machine-readable workload manifests (input snapshot hash, steps, materialization
   profile, code revision) so two runs are verifiably comparable.
4. Define the C1 named workload and run three replicates to establish measurement stability.
5. Define and run C2 (add Step 5). The C1→C2 delta is the measured marginal cost of reporting
   materialization.

**What was not changed.** No financial code was modified. The audit register states are unchanged.
The architectural gate documents remain in force.


---

## Status for External Reviewers (added for first Sharealedger member circulation, June 2026)

This repo is the empirical laboratory for the nine-paper monograph *The Architecture of Economic State* (Twitchell). It is **active research infrastructure, not a finished demonstration**. What it is and is not at this point:

**What works.** The Scala pipeline runs on the public Virginia state vendor payment dataset (FY2003). Steps 2 (standardize + sort) and 3 (post to instrument-level ledger) have been adversarially audited at fixture scale — 1,520 SJE rows, zero unbalanced journals, 59 ledger rows, ledger total $0.00. Active cost allocation, booked FX revaluation, and an interpretation/reporting layer are implemented. The architecture correctly separates the Accounting Rules Engine (ARE), the Instrument Ledger (IL), and the reporting layer — which is the core claim Paper 1 makes about the minimum cost architecture.

**What is not yet done.** Full-scale performance measurement (the cost curve experiments Papers 1 and 2 reference) has not been run. Several model decisions are open — balance grain, account namespace, period representation — documented in the Next Model Decisions section below. Windows validation is pending. The GenevaERS cross-engine validation step is future work.

**What a reviewer can usefully do.** Read `README.md` and the five layer READMEs (`01-transformation/` through `05-perspectives/`) to understand the architecture. Run the fixture smoke test (`bash scripts/run_repo_fixture_smoke_test.sh --show-data`) to see the pipeline execute. Treat the current implementation as a working architectural proof-of-concept at fixture scale, not a completed empirical result.

---

## 2026-09-30 Conceptual Model and 99 Handoff

- Branch history: this work was developed on `work/99-reporting`, now merged to `main`. The current 99 work is planning/documentation, not an implemented query service.
- A public [Sharealedger Conceptual Model repo](https://github.com/sharealedger-org/sharealedger-conceptual-model) now holds the conceptual model discussion, authorized April 2020 lecture slides, and a GenevaERS ERP discussion starter.
- The new [CKB view-query prototype plan](99-interpretation/CKB_VIEW_QUERY_PROTOTYPE.md) scopes 99 as a request/evidence interaction layer and treats scans as separately identified execution work.
- `docs/VA Data Samples and Specs.xlsx` exposes a VA-to-Universal-Journal mapping not implemented in the current Scala transform: intended Project=Program and Account=Object/Source; current code maps Project=Object, derives EXP/REV accounts, and drops Program. The workbook's wide Universal Balance also sketches Contract/Commitment/Balance IDs and 12 period amount slots; these are not yet settled as keys versus measures.
- The workbook-based reference cardinalities and dense bounds in the prototype plan are provisional domain ceilings, not observed FDS sizes or implementation evidence. The checked-in VendorMaster and VA data are fixture-scale.
- No UL accounting code changed for this model review. Do not upgrade audit states or claim the generalized CKB/query behavior until balance identity, account namespace, period representation, source mapping, and a falsifiable producer/consumer workload are agreed and tested.

### Next Model Decisions

1. Define the VA universal balance grain and the role of Contract, Commitment, Balance ID, and Instrument/Involved Party IDs.
2. Decide whether Account is a shared numeric namespace or type-qualified Object/Source domain, and define the crosswalk.
3. Decide whether the canonical balance is long-form by period or the workbook's wide period-slot representation; treat report pivots as a separate materialization choice.
4. Reconcile workbook mapping rows against source and reference data, including the program reference miss, before implementing the transformation change.
5. Select one named chartfield cut and prove its source, grouping, reconciliation, update/replay, and measured storage behavior before connecting it to 99.

## Architecture Source of Truth

- `01-transformation/` through `05-perspectives/` define logical ownership, not a fixed sequence of processes or physical passes.
- `99-interpretation/` defines the terminal post-run interpretation contract. A first interpreter and automatic run hook now exist for emitted evidence bundles, including the booked currency-revaluation fixture.
- The current Virginia Scala application is a legacy physical baseline with mixed responsibilities. Its numbered options are not a one-to-one implementation of layers 01-05.
- The original [Universal Ledger prototype](https://github.com/KipTwitchell/universal_ledger) is the domain lineage bridge: raw VA transaction assignment, Universal Journal generation, CKB-style balance updates, and multiple outputs from one pass. It is a POC, not the generalized GenevaERS engine.
- Start with [README.md](README.md), the five layer READMEs, [CKB_LAYER_EXECUTION_MODEL.md](docs/CKB_LAYER_EXECUTION_MODEL.md), and [MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md).

## Current Implementation and Build

- Mainline financial processing is Scala; Bash orchestrates; Python is restricted to preparation, inspection, and post-run analysis. See [AGENTS.md](AGENTS.md).
- The current Scala project is `02-foundation/va_pipeline/`, using Scala 2.12.18 and sbt 1.10.10.
- Local compile command:

  ```bash
  cd 02-foundation/va_pipeline
  sbt -batch compile
  ```

- `data/` contains small sanitized samples and configuration fixtures. Full VA and PO histories remain external; see [RUNTIME_DATA_LAYOUT.md](docs/RUNTIME_DATA_LAYOUT.md).
- The PostgreSQL JDBC dependency is present in the build, but the database-backed Perspective aggregation design is not implemented. Decisions are tracked in [step5-postgres-design.md](docs/step5-postgres-design.md).
- PostgreSQL is intentionally deferred. The current user-facing smoke path is the compact `--show-data` output and the file-based interpretation report.
- Spark, Vagrant, Play, and Derby are excluded from the cost-curve experiment. PostgreSQL is optional only as a later landing/access layer for event and view copies, not as part of the measured CKB path.

## Validation Status

- The Scala project compiled on macOS with JDK 21.
- A small FY2003 fixture run exercised the current VA path through its legacy financial operations on macOS. It verified selected balance and reconciliation controls, but does not validate full-scale performance or generalized 04/05 implementations.
- A separate Windows handoff branch contains fixes from that run and a fixture runner check. Windows validation remains pending.
- The canonical run, process, sort, partition, and reconciliation evidence is emitted for the baseline and active allocation smoke profiles. Engine/perspective evidence is still partial.
- The active allocation profile generates 45 balanced allocation SJEs, records an explicit rounding residual, applies them, and regenerates 27 next-period divisor groups.
- The first 99 interpreter and automatic end-of-run hook report active/partial status and missing future evidence rather than overstating coverage. The booked FX smoke emits engine metrics, reconciliation controls, and a run report with fixture-level `supports` evidence.
- The populated booked FX orchestrator smoke now stages its opening ledger, generates 4 balanced SJEs, posts them with 3 opening rows into 5 ledger rows, and passes conservation controls. Prior committed ledger state is staged per attempt; a failed run after Step 3 output leaves the published generation unchanged, removes the failed attempt, and retains its log.
- Successful runs publish a complete generation behind the output path. Older generations remain on disk for readers and are not counted in the active output's cost record. First-time migration of an existing directory to a symlink has a brief non-atomic interval; concurrent-reader behavior, retention costs, input-generation identity, and whole-process replay remain unverified.
- The agentic architecture gates in [AGENTIC_ARCHITECTURE_GATES.md](docs/AGENTIC_ARCHITECTURE_GATES.md) are now mandatory after a rejected engine attempt exposed raw-event input, non-SJE output, orphaned materialization, and false CKB placement.

## Next Work

1. Validate the current fixture and active allocation profile on Windows using the agreed Bash environment.
2. Audit and repair the measurement substrate: process rows, artifact bytes, sort/pass records, controls, and replication identity.
3. Build named minimum-cost workloads and materialization profiles without turning the Scala prototype into a generalized engine.
4. Use GenevaERS Workbench, Run-Control Apps, Performance Engine, and CKB extensions as the future execution target when a workload is ready for cross-engine validation.
5. Keep Sharealedger as the future metadata/configuration home for the open ERP model; keep Ledger Lab focused on research fixtures, controls, and cost evidence.
6. Defer PostgreSQL until the file-based perspective/report contracts and materialization profiles are stable.
7. Evaluate using the VA fixture data as a test case for generating a GenevaERS business-data model from scratch — i.e., deriving chartfields, account structure, and processing rules bottom-up from observed transaction data rather than from a pre-specified schema. This would exercise the open ERP model derivation path and could inform how Sharealedger's conceptual model handles onboarding from raw financial history.

## Current Session Boundary

The project now has a coherent pre-99 foundation:

- Rule-based VA ARE output with explicit debit/offset legs and rule identity.
- Public VA object classification vocabulary separate from policy.
- Reproducible allocation divisor generation from raw or ledger input.
- Active allocation smoke profile with generated SJEs, rounding control, and next-period divisor handoff.
- Canonical evidence and first interpretation report.
- Named workload and materialization controls.
- Synthetic, clearly labeled inputs for intercompany candidates, USD/EUR/BTC currency views, and booked currency revaluation.
- A booked currency-revaluation engine slice that generates balanced SJE, applies it through the existing poster, and passes fixture controls; baseline and allocation regressions pass after the streaming merge fix.

The next work is validation of booked-revaluation rate/period edge cases, walk-forward behavior, and
target CKB placement, followed by the scoped elimination design. Do not treat the synthetic fixtures
as public Virginia evidence or the legacy poster path as generalized GenevaERS CKB evidence.

## Rejected Engine Attempt

The 2026-09-29 attempt was discarded. It passed a synthetic balance check but violated the architecture: it read raw fixture CSVs from an engine package, emitted a custom non-SJE file, ran after the post merge loop, never applied generated rows to ledger state, eagerly loaded inputs, and ignored materialization behavior. A same-process call is not a CKB engine section. The replacement must pass [AGENTIC_ARCHITECTURE_GATES.md](docs/AGENTIC_ARCHITECTURE_GATES.md) before any currency/elimination result is interpreted as evidence.

## First Adversarial Audit: Steps 2 and 3

Audit role: `Adversarial Reviewer`
Claim: Step 2 produces a valid sorted SJE partition that Step 3 can post into conserved instrument-level ledger state.
Command: `bash scripts/run_pipeline.sh --inPath "$RUN_ROOT/input" --outPath "$RUN_ROOT/output" --years 03 --steps 2,3 --curve audit --config step2-step3 --profile audit-baseline --materialization baseline`
Evidence root: `/tmp/ledger-lab-audit.styOkT`

Verified for the staged FY03 fixture:

- 1,520 SJE rows;
- 39 fields on every SJE row;
- 0 unbalanced journal groups;
- 0 sort-key monotonicity violations;
- 59 ledger rows;
- ledger amount total `0.00`.

Audit classifications:

- Step 2 transformation: `partially_verified`; the basic schema and balance invariant pass, but record-format coverage, vendor misses, lineage semantics, and scale remain unverified.
- Step 2 sort: `partially_verified`; the fixture key check passes, but adversarial multi-spill, numeric/order, and memory tests remain.
- Step 3 posting: `partially_verified`; the fixture reconstructs a conserved ledger, but prior-state replay, duplicate keys, multi-period behavior, and full lineage remain unverified.
- Process metrics: `contradicted` on macOS; the log reports Step 3 `records_read=0` despite 1,520 journals, storage is `?`, and Step 2 canonical input rows include the header (`761` reported versus `760` data rows).

This audit does not establish production scale, GenevaERS compatibility, generalized 04/05 maturity,
or a cost-curve result. The next audit should repair metric production before measuring workloads.

## Windows Validation (pending)

An earlier Windows attempt pasted placeholder paths from the smoke-test output as literal commands, so every `head`/`wc`/`cat` failed with "No such file or directory". That was a usage error, not a pipeline result. Run the smoke test itself and copy the actual retained path only when using `--keep-output`:

```bash
bash scripts/run_repo_fixture_smoke_test.sh --show-data
```

## Starting Points for a New Collaborator

1. Read [AGENTS.md](AGENTS.md) (language boundaries, review gate) and [README.md](README.md).
2. Read the five layer READMEs, then [CKB_LAYER_EXECUTION_MODEL.md](docs/CKB_LAYER_EXECUTION_MODEL.md) and [MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md).
3. Run the fixture smoke test above, then see "Next Model Decisions" and "Next Work" in this file for open items.
4. Before any engine or perspective work, follow [AGENTIC_ARCHITECTURE_GATES.md](docs/AGENTIC_ARCHITECTURE_GATES.md) and [VERIFICATION_SESSION_PROTOCOL.md](docs/VERIFICATION_SESSION_PROTOCOL.md).
