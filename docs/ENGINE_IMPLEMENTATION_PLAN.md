# Engine Implementation Plan — CKB Pipeline (Papers 1 & 2)

**Status:** In progress
**Revised:** 2026-11-05
**Scope:** Implement and verify all Scala engine sub-tasks required for the Paper 1 cost curve
experiment (CCP1–CCP5) and the Paper 2 AI evidence suite. Paper 2 has two parts:
- **Part 1 (AIM illustrations):** AIM1, AIM3, AIM4 run against two substrates; gap illustrates
  information destruction. AIM2 and AIM5 are secondary (AIM5 is already complete).
- **Part 2 (primary Paper 2 evidence):** Three-model experiment — Posted-FineTune, IL-FineTune,
  IL-Native — trained on the pipeline's outputs, evaluated on the same downstream tasks.

Covers instrument ID fix, PO stream ingestion, CKB view emission, cost surface axis design,
Python AIM statistical scripts (Sub-Task 4), and Python three-model experiment (Sub-Task 4b).

**Language:** Scala for all financial and feature extraction work (AGENTS.md §Language Boundaries).
Python only for post-engine work: bounded statistical fitting on small summary tables (Sub-Task 4)
and ML model training on pipeline outputs (Sub-Task 4b). Both require owner approval before
writing any Python (AGENTS.md).

---

## Critical methodological constraint from Paper 2 §5.2

Paper 2's Track A (insurance) corpus has a structural property the VA corpus does not: its primary
sort key is a **stable surrogate identifier carrying no business semantics** — not date, geography,
risk tier, or line of business. 25 years of surrogate assignments across product and system changes
produces a distribution that is effectively uniform across all model features. This gives two things
for free: (1) no explicit shuffle needed before ML training, and (2) a free deterministic
train/validate/test split by `policyID mod N`.

**The VA corpus does not have this property.** `VENDORID` is assigned at eVA vendor registration
time — early registrants have low numbers, vendors may cluster by registration era, region, or
agency onboarding wave. A model trained on records ordered by `VENDORID` could learn
registration-era artifacts rather than business signal.

**Required fix before any ML model is trained:** assign a stable, semantically-neutral surrogate
key to each vendor — a deterministic hash of `VENDORID` (not of any business attribute) — and use
*that* as the ML sort key and train/validate/test split key. This is the VA analog of the insurance
corpus surrogate key. It must be produced by the Scala pass and carried on every view record so
all four Python fitting scripts receive it automatically.

---

## The architectural design

The experiment has two parallel tracks that run against the same input snapshot:

**Track 1 — CCP1: Status-quo baseline (GL architecture)**

```
Original transactions
    │
    ├──► GL post ──► GL balances (GL20YY.csv)
    │                    │
    │                    ├──► AIM1/3/4 against GL [all miss — Part 1 illustrations]
    │                    └──► Posted-FineTune training substrate [three-model experiment]
    │
    └──► Permutation generator ──► Key-embedded permutation table (PERM_TBL20YY.csv)
                                        │
                                        └──► AIM1/3/4 against permutations [all miss]
```

CCP1 measures the cost of today's systems. The permutation table is the legacy architecture's
best possible response to the information-loss problem — all dimensions expanded, but event
sequence still gone. Both substrates fail the AIM Part 1 tests for different and instructive
reasons. GL20YY.csv is also the Posted-FineTune training substrate.

**Track 2 — CCP2–CCP5: CKB instrument-ledger architecture**

```
PO events (sorted by VENDORID, fiscal_year, ORDEREDDATE)  ──┐
                                                             ├──► CKB pass (CCP2) ──► Instrument Ledger (LDGR20YY.csv)
Payment events (sorted by VENDORID, fiscal_period)        ──┘                              │
                                                                                            ├──► AIM1 [succeeds — Part 1]
                                                                                            ├──► IL-FineTune training substrate
                                                                                            └──► IL-Native training substrate

CCP3: same pass + Step 5v views ──► View A: monthly order volume    ──► AIM4 [Part 1]
                                 ──► View B: vendor-year survival    ──► AIM3 [Part 1]; IL-Native eval
                                 ──► View C: agency-year commodity   ──► AIM2 [secondary]; IL-Native eval
                                 ──► View D: agency-year payments    ──► AIM2 baseline [secondary]
```

Views A–D are bounded summary tables (thousands of rows, not millions). The delta CCP2→CCP3
is the measured cost of producing all Paper 2 substrate — both the AIM illustration tables and
the IL-Native evaluation inputs.

**Paper 2 evidence hierarchy:**
- **Primary (Part 2):** Three-model experiment (Sub-Task 4b) — Posted-FineTune vs IL-FineTune
  vs IL-Native; gaps 1→2, 2→3, 1→3; Net Income closing argument.
- **Primary (Part 1):** AIM1, AIM3, AIM4 against both substrates — same algorithm, gap shows
  what survived posting.
- **Secondary:** AIM2 (subsumed by three-model forecasting comparison); AIM5 (complete ✅).

The Scala pipeline produces all required substrates. Python consumes them post-engine.

---

## Relationship to the VA Data Model Contract

`docs/VA_DATA_MODEL_CONTRACT.md` §Source event types already specifies that a PO record is a
commitment event with `Contract or PO number`, `Involved Party ID`, `legal entity`, `order date`,
`NIGP description`, and `quantity/UOM`. It must not be treated as a settled position. The plan
below implements that contract.

The contract also states: "A payment without a matched PO must not be silently discarded." The
transform fix must make unmatched payments explicit with a declared unmatched status — not dropped.

---

## Sub-Task 0 — Confirm and commit acceptance thresholds

**Status:** ✅ **COMPLETE — 2026-11-04**

All thresholds confirmed by owner. Recorded in `docs/TEST_DESIGN.md` §9 and
`docs/RESEARCH_OBJECTIVES.md` §7. Do not adjust after any test run.

---

## Sub-Task 0b — Build CCP1: GL post step and permutation generator

**Intent:** CCP1 is the status-quo baseline — the General Ledger architecture every institution
already runs. It produces two substrates: the GL output (account-keyed balances, instrument
identity discarded) and a permutation table (every key-embedded combination that a GL
architecture would need to materialise to answer all the questions the instrument ledger answers
for free). Both substrates are run through the full AIM1–AIM5 test suite. All AIM tests are
expected to fail or degrade on both CCP1 substrates — that failure, documented and compared
against the CCP2/CCP3 results, is the empirical proof of Theorem 1.

**The GL post step:**
- Sort input transactions by account key `(AGENCY_NAME, OBJECT_NAME, FUND_NAME)` — not by
  instrument ID. Instrument identity (VENDORID) is not carried into the GL output.
- Accumulate account-keyed balances. Write GL output: `(account_key, fiscal_year, total_amount)`.
- Emit `process_metrics.csv` rows with the same four fields as CCP2 for comparability.

**The permutation generator (follow-on step, reads original transactions):**
- For each distinct `(account_key)` in the GL output, join back to the original transactions
  and enumerate all distinct combinations of instrument attributes
  `(VENDORID, NIGPCODE_3digit, geo_region)` that contributed to that balance.
- Write permutation table: `(account_key, permutation_key, record_count)`.
- Record `permutation_rows` in `process_metrics.csv`.

**Expected outcome:** The permutation table will be large — potentially orders of magnitude
larger than the GL balance rows. That ratio is the Paper 1 cost-explosion finding.

**Todo list:**
- [x] Design the GL-post Scala step (sort spec: account key; accumulator: amount by account)
- [x] Implement `GLPost.scala` in `02-foundation/va_pipeline/` — step 20
- [x] Implement `PermutationGenerator.scala` in `02-foundation/va_pipeline/` — step 21
- [x] Register steps 20 and 21 in `LedgerApp.scala` dispatcher
- [x] Emit RESULT/RESOURCE log lines (same format as steps 2 and 3) for both steps
- [x] 7/7 unit tests pass (GLPostSpec.scala); 21/21 suite total pass
- [ ] Run three replicates on real data; confirm permutation_rows is non-zero and GL balance rows are correct
- [ ] Run AIM1–AIM5 against GL output and permutation table; record results in
  `data/test_evidence/` with `substrate = CCP1_GL` and `substrate = CCP1_PERM`

**Status:** ✅ **Implementation complete — 2026-11-04.** Steps 20 (GLPost) and 21
(PermutationGenerator) are built, tested, and dispatched. Three-replicate evidence runs
and AIM pairing remain as next tasks.

---

## Sub-Task 1 — Fix `transIPID`: populate instrument ID from VENDORID

**Intent:** The instrument ID (`transIPID`) is currently hardcoded to `"0"` in both the PO path
and the payment path of `transStandardize.scala`. This means every record sorts to the same
instrument key. No CKB instrument-break logic, no instrument ledger, and no per-vendor view is
possible until this is fixed. This is the prerequisite for every subsequent sub-task.

**The fix:**
- PO path (`processPOs` in `transStandardize.scala`): set `transIPID = rec.poVendorID` directly.
  The nine-digit eVA `VENDORID` is the natural instrument key for commitment events.
- Payment path (`processPayments`): payments have `VENDOR_NAME` but no `VENDORID`. Look up the
  instrument ID from VendorMaster (keyed on normalized vendor name) during the transform step,
  consistent with the historical UL demo step 3 described in `VA_DATA_MODEL_CONTRACT.md`.
  Unmatched vendor names receive an explicit `UNMATCHED_` + hash key; they are not dropped.

**Expected outcomes:**
- `transIPID` is populated from `VENDORID` for all PO SJE rows.
- `transIPID` is populated from a VendorMaster name-lookup for payment SJE rows, with unmatched
  vendors given an explicit `UNMATCHED_` key and recorded in a control total.
- Existing fixture smoke test (`run_repo_fixture_smoke_test.sh`) still passes.
- A new VendorMaster for the fixture is created with at least the vendors present in the small
  fixture files.

**Todo list:**
- [x] Declare the workload per `AGENTIC_ARCHITECTURE_GATES.md` §1 before editing Scala
- [x] Update `transStandardize.scala` PO path: `transIPID = rec.poVendorID`
- [x] Update `transStandardize.scala` payment path: load VendorMaster as a bounded side-input
  map keyed on normalized vendor name; populate `transIPID` from lookup; record unmatched vendors
  in a control-total counter and an `UNMATCHED_VENDORS.csv` output
- [x] Update the existing fixture VendorMaster (`data/VendorMaster.csv`) with VENDORID entries
  matching the fixture vendor names (183 entries added; BLACKWELLS BOOK SERVICES → c9545)
- [x] Add a `.sortspec` for the PO-keyed sort: `(transIPID, transFiscalPeriod, transTransDate)`
  in `02-foundation/sort_specs/POSortedJE.sortspec`
- [x] Compile (`sbt compile` — clean, 1 pre-existing deprecation warning unchanged)
- [x] All 21/21 tests pass (`sbt "testOnly org.universalledger.foundation.va.*"`)
- [ ] Run the new VendorMaster-lookup path on the fixture; confirm `transIPID` is non-zero
- [ ] Update `VERIFICATION_AUDIT_REGISTER.md` with the fix and evidence

**Relevant context:**
- `transStandardize.scala` — `processPOs()` at line 112, `processPayments()` at line 219
- `Transaction.scala` — `transIPID` is field 0 (col index 0 in sorted output)
- `VendorMaster.scala` — `instID` is the lookup key; `instNIGPClass` and `instGeoRegion` are
  already declared fields ready to carry PO-derived attributes
- `SortEngine.scala` — `externalSort()` and `loadSpec()` are available; a new `.sortspec` file
  is all that is needed for the PO sort order
- `VA_DATA_MODEL_CONTRACT.md` §Required invariants — "Contract or Instrument IDs are not replaced
  by nominal account codes"

**Status:** ✅ **Implementation complete — 2026-11-04.** PO path fixed (`transIPID = rec.poVendorID`);
payment path fixed (VendorMaster name-lookup with `UNMATCHED_<hex8>` fallback + `UNMATCHED_VENDORS.csv`
control total); fixture VendorMaster updated (183 entries added); `POSortedJE.sortspec` created.
Compile clean; 21/21 tests pass. VendorMaster-lookup fixture run and VERIFICATION_AUDIT_REGISTER
update remain as next tasks.

---

## Sub-Task 2 — Bring PO commitment events into the sorted SJE stream

**Intent:** The PO path in `transStandardize.scala` currently writes to a separate `PO_YYYY.csv`
file that never enters the CKB flow. PO records are commitment events; they should be sorted and
merged into the same stream as payment SJEs, keyed on `transIPID`, so the CKB pass sees both event
types in instrument order. This is what makes Layer 3 features available to the CKB pass at no
extra scan cost.

**Expected outcomes:**
- PO records emit standardized SJE rows using `transBusinessEventCode = "COMMITMENT"` and
  `transCommitmentID = rec.poNumber` (the PO number becomes the commitment identity).
- PO SJEs are sorted by the same `SortedJE.sortspec` key as payment SJEs (or a declared combined
  key that keeps them contiguous by instrument).
- The merged sort output contains both event types interleaved in instrument order.
- The CKB `post.scala` pass can distinguish commitment from actuals by `transBusinessEventCode`.
- Control totals: PO record count, payment record count, merged total, distinct instrument IDs.

**Todo list:**
- [x] Declare the workload per architecture gates before editing Scala
- [x] Implement `standardizeAndSort.applyPO` (Step 2p): reads VA_opendata_FY20YY.txt,
  streams rows through `SortEngine.externalSort` with `SortedJE.sortspec`,
  emits 2 × 39-field balanced SJE rows per PO line (`transBusinessEventCode="COMMITMENT"`,
  `transCommitmentID=poNumber`, `transIPID=poVendorID`), writes `SortedJEFY{yr}q{n}PO.csv`.
  Memory contract: O(chunkSize) — no ArrayBuffer of all rows.
- [x] Implement `LedgerApp.runMerge` (Step 2m): calls `SortEngine.mergeSpills` on the sorted
  payment and PO partitions; writes `SortedJEFY{yr}merged.csv` for `post.scala`.
  Memory contract: O(k open file handles).
- [x] Register `case "2p"` and `case "2m"` in `LedgerApp.dispatch`
- [x] 6/6 new unit tests pass (`StandardizeAndSortPOSpec.scala`); 27/27 suite total pass
- [x] `sbt compile` — clean (1 pre-existing deprecation warning, unchanged)
- [x] RESULT/RESOURCE log lines emitted for both steps 2p and 2m
- [x] Run fixture smoke test on merged stream; confirm existing ledger controls still pass
  — ✅ passed on Git Bash: `442 ledger rows, amount sum 0.00`
- [ ] Update `VERIFICATION_AUDIT_REGISTER.md` with Step 2p/2m evidence

**Relevant context:**
- `SortEngine.scala` — `mergeSpills()` at line 172 performs a k-way merge; two sorted spill files
  are a valid input (no re-read of the source required)
- `post.scala` — the match-merge loop at line 255 already handles records by sort key order;
  it will process commitment and payment records in instrument sequence once they share the key
- `VA_DATA_MODEL_CONTRACT.md` §Purchase Order — "The PO is a commitment or arrangement event.
  It is not a payment and must not be treated as a settled position."

**Status:** ✅ **Implementation complete — 2026-11-04.**
Step 2p (`standardizeAndSort.applyPO`) and Step 2m (`LedgerApp.runMerge`) are built, tested,
and dispatched. Fixture smoke test passed on Git Bash (`442 ledger rows, amount sum 0.00`).
VERIFICATION_AUDIT_REGISTER update remains as a next task.

---

## Sub-Task 2b — Resolve test alignment with PAPER_02_NOTES.md (owner decision required)

**Intent:** Document a known gap between the current `TEST_DESIGN.md` test set and the
original test design in `org-members/research/PAPER_02_NOTES.md`. Must be resolved before the
CKB views in Sub-Task 3 are finalized, because the views need to carry the right fields.

**The gap — four tests in the notes vs. what is currently specified:**

| Notes test | Status in current spec | Gap |
|---|---|---|
| Test 1: direct count of posting non-injectivity — how many distinct pre-hierarchy patterns map to the same posted key | Not in current spec | This is a *direct empirical measurement* of Theorem 1, not inference. Cheap to add as a CKB view. |
| Test 2: vendor substitution prediction | Closed as Test 0 — no independent payment-side vendor ID | Correctly closed. |
| Test 3: PO modification rate predicting expenditure deviation | Partially covered by "does knowing what an agency ordered improve forecasting?" | Current spec uses commodity mix; notes specify the *modification sequence* (`-Vn` PONUMBER suffix) as the key feature. |
| Test 4: 14-year longitudinal robustness across 7 train/test year-windows | Not in current spec | Required for credibility with skeptical reviewers. Changes the train/test split strategy. |

**Surrogate key decision (from Paper 2 §5.2 point 4):** The insurance corpus uses a surrogate key
that is effectively uniform across all features — giving free shuffling and free train/test splits.
`VENDORID` is not this: it is assigned at eVA registration time and may carry systematic ordering
artifacts. A deterministic hash of `VENDORID` (not of any business attribute) must be added as a
`vendorSurrogateKey` field on every view record so Python fitting scripts can use it for shuffling
and `vendorSurrogateKey mod 5` train/validate/test splits. This field must be produced by the
Scala CKB pass, not added in Python after the fact.

**Owner decisions required:**
1. Add Test 1 (non-injectivity count) as a fifth CKB view? — cheap, directly proves Theorem 1.
2. Add PO modification sequence (`-Vn` count) to the agency-year commodity mix view for Test 3?
3. Adopt 7-window longitudinal evaluation (Test 4) instead of a single train/test split?
4. Confirm `vendorSurrogateKey = hash(VENDORID)` is the correct VA analog of the insurance
   surrogate key — or propose an alternative.

**Status:** ✅ **COMPLETE — 2026-11-04 (confirmed by owner)**

All four decisions confirmed:
1. Non-injectivity count (Test 1) — covered by Step 21 `PermutationGenerator` (permutation row count directly measures non-injectivity without a fifth view).
2. PO modification sequence (`-Vn` suffix) added to View C `revision_count` field — implemented in Sub-Task 3.
3. 7-window longitudinal evaluation adopted — confirmed, locked; applies to AIM2 and AIM3 on both substrates.
4. `vendorSurrogateKey = SHA256(VENDORID)` first 8 hex chars confirmed — implemented in View B; formula locked.

---


## Sub-Task 3 — Add CKB views that emit the four research feature tables

**Intent:** With PO and payment events in a single sorted stream keyed on `transIPID`, the CKB
pass can emit multiple output views simultaneously while streaming through the data once. Each view
is a bounded aggregation buffer that collapses records as they arrive, per
`CKB_LAYER_EXECUTION_MODEL.md` §Extract-Time Summarization. This produces the four small summary
tables the research tests need — without any additional file scans.

**Four views to add (all emitted in one CKB pass alongside the existing ledger output):**

| View | CKB break key | What it accumulates | Research question it serves |
|---|---|---|---|
| Monthly order volume | fiscal_month × NIGP_class_3digit | PO line count per month | Does when an agency orders carry info payment timing doesn't? |
| Vendor-year survival panel | VENDORID × fiscal_year | first-year attributes, presence/absence next year | Can we predict which vendors will stop doing business with the state? |
| Agency-year commodity mix | AGENCY × fiscal_year | NIGP class shares, revision rate, PO count | Does knowing what was ordered improve expenditure forecasting? |
| Agency-year payment totals | AGENCY × fiscal_year | total posted expenditure amount | Same — Layer 2 baseline for forecasting comparison |

Each view follows the bounded aggregation buffer pattern from `CKB_LAYER_EXECUTION_MODEL.md`:
declare grouping key, accumulate associative metrics, spill to a declared scratch file if the
buffer overflows, and write the reduced output at the terminal stage.

**Expected outcomes:**
- Four new CSV output files written by the terminal stage of the CKB pass:
  - `data/output/view_monthly_order_volume.csv`
  - `data/output/view_vendor_year_survival.csv`
  - `data/output/view_agency_year_commodity_mix.csv`
  - `data/output/view_agency_year_payment_totals.csv`
- Each view file is small (thousands of rows, not millions) — a bounded aggregate of the 133M
  source records.
- The existing `LDGR{year}.csv` ledger output is unchanged and its controls still pass.
- Process metrics emitted for the view pass: records read, records per view, buffer spill count.

**Todo list:**
- [x] Declare the workload per architecture gates; document all four view contracts
- [x] Add a new `CKBViews` Scala object with a bounded aggregation map per view
  (grouping key → accumulator; declared grouping keys; O(distinct keys) memory)
- [x] Reads `SortedJEFY*.csv` files in a single pass; routes each record to the appropriate
  view accumulator(s) based on `transBusinessEventCode` (COMMITMENT vs payment)
- [x] At end-of-stream, write all four view output files in declared key order
- [x] Declare and emit process metrics: records_read, view row counts, elapsed_s (RESULT/RESOURCE)
- [x] Register `case "5v"` in `LedgerApp.dispatch`; document in usage help
- [x] 8/8 new unit tests pass (`CKBViewsSpec.scala`); 35/35 suite total pass
- [x] `sbt compile` — clean (1 pre-existing deprecation warning, unchanged)
- [x] Run fixture smoke test on merged stream; confirm existing ledger controls still pass
  — ✅ passed on Git Bash: `442 ledger rows, amount sum 0.00`
- [ ] Verify view totals match independent control counts from `vendor_po_attributes_by_year.csv`
  and the APA corpus (sanity cross-check, not a full reconciliation)
- [ ] Update `VERIFICATION_AUDIT_REGISTER.md`

**Memory contract:** O(distinct keys per view). At fixture scale (hundreds of vendors, a few
agencies, ~14 fiscal years, ~100 NIGP 3-digit classes) all four maps fit easily in JVM heap.
No spill implemented; if a real-data run exhausts heap, the declared fix is to lower chunkSize
or add a spill-to-file path analogous to SortEngine. Buffered character-by-character source
codec prevents heap load of the full file.

**Relevant context:**
- `CKBViews.scala` — new `02-foundation/va_pipeline/` object; step ID `5v`
- `LedgerApp.scala` — `case "5v"` routes to `CKBViews(params + (...))`
- `CKBViewsSpec.scala` — 8 tests: Views A–D correctness, all-files output, metrics, date formats,
  year-filter isolation

**Status:** ✅ **Implementation complete — 2026-11-04.**
Steps 20/21 (CCP1) and Step 5v (CCP3 views) are built, tested, and dispatched.
Fixture smoke test passed on Git Bash (`442 ledger rows, amount sum 0.00`).
VERIFICATION_AUDIT_REGISTER update remains as a next task.

---

## Sub-Task 3e — Cost surface axes: time partition granularity and materialization scope

**Status:** [x] Implementation complete 2026-11-05.

**Intent:** The CCP1–CCP5 capability-layer axis is the first dimension of the cost surface.
Two additional orthogonal axes must be explicitly varied to produce the full empirical test of
Paper 1's universality claim. This sub-task defines those axes, their experimental run
configurations, and the `cost_surface.csv` schema changes needed to record them. It does not
change any engine code; it documents what must be wired before the full-corpus run.

### Background — the movement architecture contract

The instrument ledger is a **movement store**, not a balance store. Every row in
`LDGR20YY.csv` carries the net of journal movements for one (key, period) combination.
Running balances, year-to-date totals, and balance-at-any-date are all derived by query-time
summation of movement rows. No pre-computed balance cache is stored in the ledger.

This is a first-class design decision: the VSAM/BDAM pattern of storing monthly buckets plus
a pre-computed annual total in the same master record exists to reduce read-time compute cost
when the same total is queried many times online. This system's query pattern is batch
analytical — one pass of the ledger produces all needed aggregations. Redundant pre-computed
totals are not needed, and adding them would obscure the cost measurement.

### Axis 2 — Time partition granularity (`--periodGranularity`)

`transFiscalPeriod` (col 22 of the SortedJE schema) is the ledger posting period key. Its
granularity determines how many distinct rows the ledger produces per key combination and how
many sequential CKB passes `post.scala` makes per fiscal year.

| Parameter value | `transFiscalPeriod` | Source derivation |
|----------------|---------------------|-------------------|
| `annual`    | `"2003"`       | Year parameter only |
| `quarterly` | `"2003/Q2"`    | Quarter of fiscal year |
| `monthly`   | `"2003/07"`    | Calendar month of event date |

**Derivation rules per stream:**
- **APA expenditures:** month derived from source filename (`"Expenditures Jul 2002.csv"` →
  `"2002/07"`). One SortedJE output file per source month under `T-monthly`.
- **PO data:** month derived per-row from `ORDEREDDATE` (`"07/22/2002"` → `"2002/07"`).
  All rows remain in one `SortedJEFY03_PO.csv`; period key varies per row.
- **Revenue:** month derived per-row from `DEPOSIT_DATE`. Annual file, per-row period keys.

**`T-monthly` is the default for AIM test substrates.** AIM4 STL decomposition requires
monthly order volume; with quarterly or annual granularity the seasonal signal is too coarse.
AIM2 and AIM3 also benefit from monthly commitment resolution. Papers 3–5 (allocation, FX,
elimination) require month-end close granularity matching standard accounting practice.

**`T-annual` is the minimum-cost anchor.** Use for cost-curve left anchor and for runs where
year-end totals are sufficient (e.g. Paper 1 cost delta CCP1→CCP2).

**CKB pass count implications:**
- `T-monthly`: ~14 passes per FY (12 expenditure months + 1 PO + 1 revenue)
- `T-quarterly`: ~5 passes per FY (4 quarterly + 1 PO)
- `T-annual`: 1–2 passes per FY (1 merged + 1 PO, or 1 combined merged)

More passes = more sequential I/O. The cost of finer granularity is measured by the
`total_compute_s` and `total_storage_bytes` columns in `cost_surface.csv`. If cost is flat
across granularities, the richer granularity is free. If cost grows, the growth is a finding.

### Axis 3 — Materialization scope (`--materialization`)

Whether aggregated period summaries are pre-computed in a follow-on pass or derived on demand
from the movement ledger.

| Parameter value | What is produced beyond `LDGR` | Extra passes |
|----------------|-------------------------------|-------------|
| `movement-only`       | None — movements only | 0 |
| `movement+annual`     | Annual summary: one total per key across all periods | 1 |
| `movement+all-rollups` | Monthly + quarterly + annual + YTD summaries | 3 |

**Default for all CCP runs:** `movement-only`. Pre-computed rollups are not needed for any
AIM test — all AIM scripts consume the CKB views (step 5v), not the ledger directly.

### The four experimental configurations

```
Run A:  T-annual  + movement-only     → minimum output (Paper 1 left anchor)
Run B:  T-monthly + movement-only     → AIM substrate (richest granularity, no extra cost)
Run C:  T-monthly + movement+annual   → richest granularity + annual cache
Run D:  T-monthly + movement+all-rollups → maximal pre-computation (right anchor)
```

The Paper 1 claim: if cost(A) ≈ cost(B), monthly granularity is free. If cost(B) ≈ cost(D),
pre-computation is also free. If cost(D) > cost(B), the excess is the pre-computation cost —
the time-dimension analog of the permutation explosion on the capability dimension.

Every CCP level (CCP1–CCP5) should eventually be measured at configurations A and B at minimum.
Configuration B is also the AIM test substrate — it is both the research instrument and a cost
curve point.

### `cost_surface.csv` schema changes required before full corpus run

Current columns: `run_id, run_timestamp, curve, config_description, years, steps,
total_compute_s, total_storage_bytes, master_file_count, total_cost_proxy`

Required additions:

| New column | Values | Notes |
|-----------|--------|-------|
| `capability_layer` | CCP1–CCP5 | Replaces `curve`; same values |
| `period_granularity` | `annual` \| `quarterly` \| `monthly` | Axis 2 |
| `materialization_scope` | `movement-only` \| `movement+annual` \| `movement+all-rollups` | Axis 3 |
| `ledger_row_count` | integer | Rows in `LDGR20YY.csv` — granularity impact diagnostic |
| `ldgr_file_bytes` | integer | Ledger file size — separated from total for analysis |
| `view_file_bytes` | integer | Sum of all step 5v view file sizes |
| `permutation_rows` | integer | Rows in `PERM_TBL20YY.csv` (CCP1 only; 0 otherwise) |

*Until `--periodGranularity` and `--materialization` are wired as CLI parameters, capture
their values in `config_description` using the labels above (e.g. `"T-monthly/movement-only"`).*

### Implementation todo list

- [x] Add `--periodGranularity annual|quarterly|monthly` to `run_pipeline.sh` (default: monthly)
- [x] Thread `periodGranularity` through `LedgerApp` dispatcher to `applyAPA` and `applyPO`
- [x] In `applyAPA`: replace quarterly loop with per-source-file loop; derive period from
      calendar month already available from `discoverApaFiles`; name output file
      `SortedJEFY{yr}_{YYYY}_{MM}_exp.csv`; set `transFiscalPeriod = "YYYY/MM"`
- [x] In `applyPO`: derive `transFiscalPeriod` per-row from `ORDEREDDATE` when
      `periodGranularity = monthly` (already available in `transTransDate`; just promote to col 22)
- [x] Update `cost_surface.csv` init to new schema (17 columns)
- [x] Update `record_cost_point` to emit new columns
- [x] Update `write_canonical_evidence.sh` to capture `ledger_row_count`, `ldgr_file_bytes`,
      `view_file_bytes`, `permutation_rows`, `period_granularity` in `run_manifest.csv`
- [x] Add `period_granularity` and `materialization_scope` as labelled fields in the pipeline
      run header log
- [x] Update `Ledger.scala` comment on `ldgrTransAmount`: clarify it is a period movement total,
      not a balance — `"net movement amount for this (key, period) bucket — NOT a running balance"`

**Note:** `post.scala` requires **no changes** for monthly granularity. With distinct period
keys per month, the match-merge loop naturally produces period-isolated movement rows. The
"balance accumulation" behaviour (prior period's row added to current period's journal) only
fires when a prior SortedJE file wrote the same full key — which only happens when period
granularity is annual (all months share one period key). Monthly period keys make each pass's
rows independent by construction.

---

## Sub-Task 4 — Python: AIM statistical tests (Paper 2 Part 1 illustrations)

**Intent:** Three primary AIM tests (AIM1, AIM3, AIM4) and two secondary tests (AIM2, AIM5)
run against the Scala-produced view tables. All Python scripts operate on small bounded
summary tables — hundreds to thousands of rows — not on 133M raw records.

**Primary AIM tests (Paper 2 Part 1 — run these first):**
- `scripts/fit_noninjectivity.py` — reads `LDGR20YY.csv` and GL output; counts distinct PO
  patterns per posted key; reports CCP1-vs-CCP2 gap. **AIM1.**
- `scripts/fit_vendor_survival.py` — reads `view_vendor_year_survival.csv`; fits discrete-time
  survival model; reports C-statistic and permutation p-value. **AIM3.**
- `scripts/fit_ordering_seasonality.py` — reads `view_monthly_order_volume.csv`; runs STL
  decomposition; reports Q4 significance per commodity class. **AIM4.**

**Secondary tests (run after primary AIM tests):**
- `scripts/fit_agency_name_classifier.py` — reads `data/corpus_profile/agency_alias_candidates.csv`;
  trains similarity classifier; produces `agency_reconciliation_table.csv`. **AIM5 — infrastructure.**
- `scripts/fit_agency_forecasting.py` — reads `view_agency_year_commodity_mix.csv` and
  `view_agency_year_payment_totals.csv`; trains posted-layer baseline and commitment-layer model;
  reports RMSE improvement. **AIM2 — secondary, subsumed by Sub-Task 4b.**

Each script writes a structured evidence file to `data/test_evidence/` with: inputs used,
method, key metric, threshold, pass/fail verdict, and run timestamp. No script reads raw
PO `.txt` files or APA expenditure CSVs.

**Todo list:**
- [x] Confirm Sub-Task 3 view tables are available before writing any Python
- [ ] Set up `.venv` with required libraries: `statsmodels`, `scikit-learn`, `scipy` (deferred — scripts degrade gracefully)
- [x] Write and run `scripts/fit_noninjectivity.py` (AIM1 — primary) — smoke-run 2026-11-06; gap=714 confirmed
- [x] Write and run `scripts/fit_vendor_survival.py` (AIM3 — primary) — smoke-run 2026-11-06; descriptive stats only (1-yr)
- [x] Write and run `scripts/fit_ordering_seasonality.py` (AIM4 — primary) — smoke-run 2026-11-06; Q4 deferred (1-yr)
- [x] Write and run `scripts/fit_agency_name_classifier.py` (AIM5 — infrastructure) — done 2026-11-04; precision=1.0000
- [ ] Write and run `scripts/fit_agency_forecasting.py` (AIM2 — secondary)
- [x] Write evidence files to `data/test_evidence/`

**Status:** [x] AIM1/3/4/5 scripts complete. Full acceptance deferred to Stage 3 (5+ years) + dependency install.

---

## Sub-Task 4b — Python: three-model experiment (Paper 2 Part 2 — primary evidence)

**Intent:** Train and evaluate three models (Posted-FineTune, IL-FineTune, IL-Native) on
the same downstream tasks from the same VA corpus. This is the primary Paper 2 Part 2
evidence. Owner approval required before writing any Python (AGENTS.md).

**Models:**

| # | Name | Training substrate | Input file |
|---|------|--------------------|------------|
| 1 | Posted-FineTune | Pretrained LLM fine-tuned on posted data | `GL20YY.csv` (CCP1 step 20) |
| 2 | IL-FineTune | Pretrained LLM fine-tuned on Instrument Ledger | `LDGR20YY.csv` (CCP2 step 3) |
| 3 | IL-Native | Structured model built directly from IL — no LLM | `LDGR20YY.csv` + Step 5v views |

**Downstream evaluation tasks (same for all three models):**
- Vendor survival prediction (AIM3 question)
- Expenditure forecasting (supersedes standalone AIM2 as primary result)
- Net Income derivation (logical proof — IL-Native answers it; Posted-FineTune cannot)

**Verbalization:** Posted-FineTune and IL-FineTune require verbalized records (structured
fields → natural-language descriptions). IL-Native consumes the IL as structured tabular
input. Verbalization schema to be defined when scripts are written.

**Expected outcomes:**
- `scripts/train_posted_finetune.py` — fine-tunes pretrained LLM on `GL20YY.csv`
- `scripts/train_il_finetune.py` — fine-tunes pretrained LLM on `LDGR20YY.csv`
- `scripts/train_il_native.py` — trains structured model on IL + views
- `scripts/evaluate_models.py` — runs all three on downstream tasks, computes gaps 1→2, 2→3, 1→3
- Output: `data/test_evidence/three_model_experiment_results.csv`

**Todo list (blocked on owner approval):**
- [ ] Owner approves Python model training work (AGENTS.md gate)
- [ ] Define verbalization schema for Posted-FineTune and IL-FineTune
- [ ] Select pretrained LLM base model
- [ ] Write training scripts for all three models
- [ ] Write evaluation harness
- [ ] Run on Stage 3 slice (FY2003–FY2007) for directional check
- [ ] Run on full 14-year corpus for final evidence

**Status:** [ ] blocked — owner approval required

---

## Sub-Task 5 — Record results and update documentation

**Intent:** After all tests have run, record pass/fail results in `TEST_DESIGN.md`
and update `SESSION_HANDOFF.md`.

**Todo list:**
- [ ] Add `**Result:**` block to each test section in `TEST_DESIGN.md`:
  pass/fail, metric value, evidence file path, date run
- [ ] Confirm `data/test_evidence/` is git-tracked (add `.gitkeep`; evidence JSON files are
  small and should be committed)
- [ ] Add session entry to `SESSION_HANDOFF.md`

**Status:** [ ] pending

---

## What this plan is not

- It does not introduce a new logical architectural layer. All new Scala code lives in the
  existing `02-foundation/va_pipeline/` path.
- It does not use Python for any file reading of the 133M-record PO corpus.
- It does not create a separate "AI pipeline" outside the CKB flow.
- The three-model experiment (Sub-Task 4b) **does** compare runs using different training
  substrates (GL output vs IL vs IL + views) — that comparison is the primary Paper 2 Part 2
  evidence. This is intentional and by design; it is not engine cross-contamination.
- The AIM tests and the three-model experiment are the *justification* for bringing PO data
  into the engine correctly, not a separate project that happens alongside it. The Scala pass
  produces the substrates; Python consumes them post-engine without touching the 133M raw records.

---

## Data dependency map

| File | Produced by | Consumed by |
|---|---|---|
| `SortedJEFY*.csv` (PO + payment merged) | Sub-Task 2 | Sub-Task 3 CKB pass |
| `GL20YY.csv` | Sub-Task 3 step 20 (CCP1) | Sub-Task 4b Posted-FineTune training |
| `LDGR20YY.csv` | Sub-Task 3 step 3 (CCP2) | Sub-Task 4 AIM1; Sub-Task 4b IL-FineTune + IL-Native training |
| `view_monthly_order_volume.csv` | Sub-Task 3 step 5v | Sub-Task 4 `fit_ordering_seasonality.py` (AIM4) |
| `view_vendor_year_survival.csv` | Sub-Task 3 step 5v | Sub-Task 4 `fit_vendor_survival.py` (AIM3); Sub-Task 4b IL-Native eval |
| `view_agency_year_commodity_mix.csv` | Sub-Task 3 step 5v | Sub-Task 4 `fit_agency_forecasting.py` (AIM2); Sub-Task 4b IL-Native eval |
| `view_agency_year_payment_totals.csv` | Sub-Task 3 step 5v | Sub-Task 4 `fit_agency_forecasting.py` (AIM2) |
| `agency_alias_candidates.csv` | Already built | Sub-Task 4 `fit_agency_name_classifier.py` (AIM5) |
| `agency_reconciliation_table.csv` | Sub-Task 4 AIM5 script | Sub-Task 4 `fit_agency_forecasting.py` (AIM2) |
| `data/test_evidence/AIM*.csv` | Sub-Task 4 scripts | Sub-Task 5 documentation |
| `data/test_evidence/three_model_experiment_results.csv` | Sub-Task 4b `evaluate_models.py` | Sub-Task 5 documentation |
