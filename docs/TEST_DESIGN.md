# VA Corpus — Unified Test Design (CCP Cost Points + AIM Model Evidence)

**Status:** Authoritative. Supersedes `TEST_DESIGN_PAPER_1.md` and `TEST_DESIGN_PAPER_2.md`.  
**Prepared:** 2026-11-04  
**Companion:** [`docs/RESEARCH_OBJECTIVES.md`](RESEARCH_OBJECTIVES.md) — master objectives and run-order map

---

## 1. One Corpus. One Run. Two Facets of One Finding.

The CCP cost measurements and the AIM model evidence tests are not two separate campaigns.
They draw from the same pipeline run against the same corpus. CCP1 produces the GL output
and permutation table — the substrates against which the AIM tests fail. CCP2 produces the
instrument ledger — the substrate against which AIM1 succeeds. CCP3 adds the Step 5v views
alongside the ledger pass — the feature tables AIM2–AIM4 consume. The delta CCP2→CCP3 is the
measured cost of producing all AI evidence substrate. That cost is expected to be small.

**The two papers are two facets of the same architectural failure, measured from the same data.**
Paper 1 states the cost claim. Paper 2 states the AI-consequence claim. Both are proven by the
same run.

---

## 2. The Two-Substrate Design

Every AIM test (except AIM0, which is closed) runs **twice** against outputs of the same pipeline:

| Substrate | Produced by | Architecture | Expected AIM result |
|-----------|------------|--------------|---------------------|
| GL output (`GL20YY.csv`) | CCP1 step 20 | Account-keyed; instrument identity discarded | All AIM tests miss — vendor identity gone |
| Permutation table (`PERM_TBL20YY.csv`) | CCP1 step 21 | All key-embedded permutations from original transactions | All AIM tests still miss — event sequence unrecoverable |
| Instrument Ledger (`LDGR20YY.csv`) | CCP2 step 3 | Instrument-keyed; vendor identity preserved | AIM1 succeeds |
| Step 5v views (`view_*.csv`) | CCP3 step 5v | Bounded aggregation views of the CKB stream | AIM2, AIM3, AIM4 succeed |

The gap between the CCP1-substrate results and the CCP2/CCP3-substrate results is the
empirical proof of Theorem 1: the posting process is lossy compression, and the information
it destroys is predictively useful.

---

## 3. The Three-Layer Framework

From Paper 2 §5.3:

| Layer | Training data | Cannot see |
|-------|--------------|------------|
| Layer 1 | Text descriptions of transactions (verbalized records) | Event structure, field-level values, temporal sequence |
| Layer 2 | Pre-computed balance totals from posted accounting records | Any attribute discarded at posting time |
| Layer 3 | Raw pre-hierarchy events before any posting or aggregation | Nothing — this is the full record |

AIM2 is specifically the Layer 2 vs. Layer 3 comparison: does adding pre-hierarchy commitment
features to a posted-layer baseline improve expenditure forecasting?

---

## 4. CCP Workload Definitions

### What "CCP" means

`CCP` = **C**ost **C**urve **P**oint. Each is a verified, reproducible position on the minimum
cost curve (CCP2–CCP5) or the status-quo baseline (CCP1). Three replicates of each workload
on the same input snapshot are required before any cost comparison is published.

### The cost surface has three independent axes

The CCP labels (CCP1–CCP5) define Axis 1 — *capability layer* (which steps run). They are
**not** the only dimension of the cost surface. Two additional axes vary independently of
capability layer and must be treated as explicit experimental parameters:

**Axis 1 — Capability layer** (current CCP1–CCP5)
Which processing steps are active. This is the existing axis.

**Axis 2 — Time partition granularity** (`--periodGranularity`)
How finely `transFiscalPeriod` subdivides time. This determines the number of distinct ledger
rows per key combination, the number of CKB passes `post.scala` makes, and the period
resolution of all downstream analytical outputs.

| Label | `transFiscalPeriod` value | Ledger rows per key | CKB passes per FY |
|-------|--------------------------|---------------------|-------------------|
| `T-annual`    | `"2003"`      | 1 per key    | 1 merged pass |
| `T-quarterly` | `"2003/Q2"`   | Up to 4      | 4–5 files |
| `T-monthly`   | `"2003/07"`   | Up to 12     | 12–14 files |

`T-monthly` is the **AIM test substrate default**: AIM4 STL decomposition requires monthly
order volume; AIM2 and AIM3 benefit from monthly commitment balance resolution; Papers 3–5
(allocation, FX, elimination) require a month-end close granularity for standard accounting
practice. `T-annual` is the minimum-materialization anchor for cost-curve comparison.

**Axis 3 — Materialization scope** (`--materialization`)
Which aggregated outputs are pre-computed in the same pass vs. derived on demand from movement
rows. All outputs are *movements* (period totals), never balances. Running balances for any
date range are derived by query-time summation of movement rows — no pre-computed balance cache
is needed or stored.

| Label | What is produced | Storage cost | Read-time cost |
|-------|-----------------|-------------|----------------|
| `movement-only` | `LDGR20YY.csv` (one movement row per key-period) | Minimum | Any aggregate recomputed on read |
| `movement+annual` | `LDGR` + one extra pass producing annual movement totals per key | +1 pass + annual summary file | Annual queries O(1) per key |
| `movement+all-rollups` | `LDGR` + monthly + quarterly + annual + YTD summaries | Maximum | All period queries O(1) |

The Paper 1 claim is: **the minimum cost of producing any analytical result is at most the cost
of one sorted pass over the source data, regardless of how many analytical questions you want to
answer.** The experimental test of this claim is:

```
Run A:  T-annual  + movement-only     → minimum possible output (left anchor)
Run B:  T-monthly + movement-only     → richest granularity, no pre-computation
Run C:  T-monthly + movement+annual   → richest granularity + pre-computed annual cache
Run D:  T-monthly + movement+all-rollups → everything pre-computed (right anchor)
```

If cost(A) ≈ cost(B) ≈ cost(C), this proves the universality claim: richer time resolution
costs no more than annual. If cost(B) ≈ cost(D), pre-computation also costs no more than
movement-only. If cost(D) > cost(B), the gap is the cost of the pre-computation — the time
dimension analog of the permutation explosion on the capability dimension.

**The ledger is a movement store, not a balance store.** Each row in `LDGR20YY.csv` represents
the net of all journal movements for one (key, period) combination. It is not an opening
balance, closing balance, or year-to-date balance. Running balance for any period range =
`SUM(ldgrTransAmount) WHERE period <= target AND key = target`. No pre-computed balance cache
is stored in the ledger.

### `cost_surface.csv` schema — required columns

The current schema (`run_id, run_timestamp, curve, config_description, years, steps,
total_compute_s, total_storage_bytes, master_file_count, total_cost_proxy`) captures Axis 1
only. Before the full corpus run, add explicit columns for Axes 2 and 3:

```
run_id, run_timestamp, capability_layer, period_granularity, materialization_scope,
years, steps, total_compute_s, total_storage_bytes, ledger_row_count, ldgr_file_bytes,
view_file_bytes, permutation_rows, peak_rss_bytes, total_cost_proxy
```

**`capability_layer`** replaces `curve` (same values: CCP1–CCP5).
**`period_granularity`** = `annual` | `quarterly` | `monthly`.
**`materialization_scope`** = `movement-only` | `movement+annual` | `movement+all-rollups`.
**`ledger_row_count`** = number of rows in `LDGR20YY.csv` (diagnostic for period granularity impact).
**`ldgr_file_bytes`** = size of ledger file (separated from total_storage_bytes for analysis).
**`view_file_bytes`** = size of all view files combined.
**`permutation_rows`** = rows in `PERM_TBL20YY.csv` (CCP1 only; `0` for CCP2–CCP5).

*Implementation note:* `--periodGranularity` and `--materialization` parameters are to be added
to `run_pipeline.sh` and threaded through the Scala dispatchers. Until wired, capture values in
`config_description`. The schema columns above define the target; backfill from `config_description`
for any runs made before the columns are added.

### Summary table

| ID | Name | Steps (actual step IDs) | What it measures | AIM pairing |
|----|------|------------------------|-----------------|-------------|
| **CCP1** | General Ledger (status quo) | 2, 2p, 2m, 3, 20, 21 | Cost of what every institution already runs. Permutation table size = cost-explosion finding. | AIM1–AIM5 against GL + permutation table — all miss |
| **CCP2** | Instrument Ledger | 2, 2p, 2m, 3 | Left anchor of minimum cost curve. Delta CCP1→CCP2 = cost of architectural change. | AIM1 against IL — succeeds |
| **CCP3** | IL + perspective aggregation | 2, 2p, 2m, 3, 5v | Marginal cost of producing all AI evidence substrate. Delta CCP2→CCP3 expected small. | AIM2, AIM3, AIM4 consume step 5v view outputs |
| **CCP4** | CCP3 + revaluation engines | 2, 2p, 2m, 3, 5v, 8, 9 | Marginal cost of generated-SJE engines. | — |
| **CCP5** | Full pipeline | 2, 2p, 2m, 3, 5v, 6, 7, 8, 9 | CCP5 ≈ CCP4 = empirical proof of single-pass universality. | AIM5 prerequisite (clean agency keys for allocation) |

**Step IDs:** 2 = payment standardise+sort, 2p = PO standardise+sort, 2m = merge payment+PO
streams, 3 = CKB post → Instrument Ledger, 5v = CKB view extraction (Step 5, new ID),
6 = allocation, 7 = consolidation, 8 = FX revaluation, 9 = reclass, 20 = GL post (CCP1),
21 = permutation generator (CCP1).

### CCP1 — General Ledger (status quo)

**Architecture:** Account-keyed GL post. Vendor identity is not an input to the accumulation
key. Steps 20 and 21 read the same SortedJE stream independently.

**Step 20 — GL post:** sort by account key `(AGENCY, OBJECT_NAME)`; accumulate amounts;
write `GL20YY.csv`. Instrument identity (`transIPID`) intentionally excluded from the key.

**Step 21 — Permutation generator:** for each distinct `(account_key)` in the GL output,
enumerate all distinct `(VENDORID, NIGPCODE_3digit, revision_suffix)` combinations that
contributed to it. Write `PERM_TBL20YY.csv`. The permutation row count is the empirical
size of the upstream materialisation burden the GL architecture externalises onto source systems.

**Known measurement conservatism:** The CCP1 sort (Step 2) uses the same vendor-bearing
SortedJE key as CCP2, which slightly overstates legacy GL sort cost. The CCP1→CCP2 delta is
therefore a lower bound on the true architectural cost difference.

**Required metrics (`process_metrics.csv`):** `records_read`, `output_rows` (GL rows for
step 20; permutation rows for step 21), `wall_seconds`, `peak_rss_bytes`.

**Status:** ✅ Steps 20 and 21 implemented, tested (7/7 GLPostSpec). Not yet run on corpus.

### CCP2 — Instrument Ledger

**Architecture:** CKB post, instrument-keyed. VENDORID is the primary sort and ledger key.

**Steps 2, 2p, 2m:** Payment files → Step 2 → `SortedJEFY{yr}q{n}exp.csv`. PO files →
Step 2p → `SortedJEFY{yr}q{n}PO.csv`. Step 2m merges both into `SortedJEFY{yr}merged.csv`.

**Step 3:** CKB match-merge post reads merged stream → writes `LDGR20YY.csv`.

**Required metrics:** `records_read`, `bytes_written`, `wall_seconds`, `peak_rss_bytes`.

**Acceptance:** Three `process_metrics.csv` rows for each step, all non-zero, all replicates
within 15% of the median. Input snapshot hashes committed in workload manifest.

**Status:** ✅ Steps 2p, 2m implemented and tested. Step 2 and 3 already operational.
Not yet run on corpus.

### CCP3 — Instrument Ledger + Perspective Aggregation

**Steps:** CCP2 steps + Step 5v (`CKBViews`).

**Step 5v:** Single pass over `SortedJEFY*.csv`. Four bounded aggregation maps. Writes:
- `view_monthly_order_volume.csv` — key `(fiscal_period, nigp_class_3digit, agency)`
- `view_vendor_year_survival.csv` — key `(vendor_id, fiscal_year)`
- `view_agency_year_commodity_mix.csv` — key `(agency, fiscal_year, nigp_class_3digit)`
- `view_agency_year_payment_totals.csv` — key `(agency, fiscal_year, fund_id, object_code)`

The views ARE the AIM2–AIM4 feature tables. Not a separate AI pipeline. Natural CKB output.

**Status:** ✅ Step 5v implemented and tested (40/40 tests). Not yet run on corpus.

### CCP4 and CCP5

**Status:** ⚠ Blocked on CCP3. Steps 8, 9, 6, 7 require engine contract audit in
`VERIFICATION_AUDIT_REGISTER.md` before contributing to a cost point. AIM5 required before
CCP5 (clean agency keys for allocation).

---

## 5. AIM Test Specifications

### AIM0 — CLOSED 2026-11-01

No deterministic PO-to-payment link exists in the public corpus. AIM1–AIM5 are designed
around what the data actually provides (aggregate joins at `(agency, vendor, fiscal_year)`).

### AIM1 — Non-injectivity count (no ML required)

**Paper 2 claim:** Posting is non-injective — multiple distinct pre-hierarchy patterns map to
the same posted key. AIM1 measures that directly. No statistical model needed.

**Two-substrate design:**

| Substrate | Method | Expected result |
|-----------|--------|----------------|
| CCP1 GL output | Count distinct `(VENDORID, NIGPCODE_3digit, revision_suffix)` per GL key | High collapse — vendor identity gone |
| CCP1 permutation table | Same question against permutation keys | Reduced collapse — but event sequence still unrecoverable |
| CCP2 instrument ledger | Count distinct PO patterns per `(VENDORID, fiscal_year)` ledger key | Non-injectivity measurable with full fidelity |

**Acceptance:** No pass/fail threshold. Any non-zero gap between CCP1 and CCP2 substrates
is evidence. A large gap is strong evidence of structural information loss.

**Output:** `data/test_evidence/AIM1_noninjectivity_distribution.csv`

### AIM2 — Agency expenditure forecasting (Layer 2 vs Layer 3)

**Paper 2 claim (§5.3):** Commitment-layer features carry predictive signal for future
expenditure invisible in posted records. Layer 3 model outperforms Layer 2.

**Inputs:**
- Layer 2: `view_agency_year_payment_totals.csv` (CCP3 output)
- Layer 3: `view_agency_year_commodity_mix.csv` + Layer 2 (CCP3 output) + AIM3 vendor health
  score + AIM4 budget-execution risk flag
- Target: next-year agency expenditure by goods category
- Agency join: requires AIM5 reconciliation table (run AIM5 first)

**Method:** Train Model L2 (posted features only) and Model L3 (posted + PO features) on
7 rolling train/test windows across FY2003–FY2016. Report RMSE and R² per window.
Permutation test on the Layer 3 feature block.

**Acceptance (confirmed 2026-11-04, locked):** RMSE improvement ≥ 5% over L2 baseline;
permutation p < 0.05.

**Output:** `data/test_evidence/AIM2_forecasting_results.csv`

### AIM3 — Vendor survival model

**Paper 2 claim (§5.3):** Pre-hierarchy event record preserves vendor lifecycle (entry,
activity, exit). Posted records have no concept of vendor lifecycle.

**Input:** `view_vendor_year_survival.csv` (CCP3 output)

**Features:** `first_order_year`, `po_count`, `nigp_class_modal`, `agency_count`,
`distinct_nigp_classes`, year-over-year class shift, annual trajectory.

**Target:** `present_next_year` (binary, already in View B).

**Method:** Discrete-time survival model or Cox proportional hazards. 7-window longitudinal
evaluation. Hold out FY2015–2016 for final validation.

**Acceptance (confirmed 2026-11-04, locked):** C-statistic > 0.65; permutation p < 0.05.

**Key corpus finding regardless of outcome:** 43% single-year vendor churn rate, composition
by commodity class and entry cohort — no posted-record equivalent exists.

**Output:** `data/test_evidence/AIM3_survival_results.csv`

Also produces `vendor_health_score` per vendor-year → fed into AIM2 as a feature.

### AIM4 — Ordering seasonality

**Paper 2 claim (§1.1, §5.1 Track B):** Order dates carry fiscal-year-end budget-rush signal
that payment dates do not. Q4 (Apr–Jun) spikes in order volume are invisible in payment records.

**Input:** `view_monthly_order_volume.csv` (CCP3 output). No cross-file join needed.

**Method:** STL decomposition of monthly order volume per agency × NIGP class. Test whether
seasonal component shows significant Q4 spike.

**Acceptance (confirmed 2026-11-04, locked):** ≥ 3 of top 5 goods classes show Q4
seasonality with p < 0.05 in the majority of full-panel agencies (5+ years).

**Output:** `data/test_evidence/AIM4_seasonality_results.csv`

Also produces `budget_execution_risk_flag` per agency-year → fed into AIM2 as a feature.

**Can run immediately after CCP3 corpus run. No agency join required.**

### AIM5 — Agency name classifier

**Paper 2 claim (§3.3):** A pre-hierarchy architecture preserves name strings that a
balance-posting system discards into account codes. An AI model can distinguish spelling
variation from genuine organisational ambiguity.

**Input:** `data/corpus_profile/agency_alias_candidates.csv` — **already exists**.
Does not require the corpus pipeline run. Can run in parallel with CCP1–CCP3.

**Data:**
- 99.1% exact-match pairs → labeled positive training set
- 13 names (20,512 rows) unmatched → test set
- 3 names (131,809 rows) genuinely ambiguous between two reference keys

**Method:** Binary similarity classifier. Apply to 16 unresolved names. Report confidence
and top reference candidate per name.

**Acceptance (confirmed 2026-11-04, locked):** Precision ≥ 0.99 on held-out exact matches;
cases with confidence < 0.70 flagged as genuine ambiguity, not force-resolved.

**Output:** `data/corpus_profile/agency_reconciliation_table.csv` → unblocks AIM2 and CCP5.

---

## 6. Pipeline Readiness

The Scala implementation of all steps is complete. The orchestration layer is wired and
ready for a validation run.

### What is built (Scala — verified)

| Step | Status | Tests |
|------|--------|-------|
| Step 2 — payment standardise+sort | ✅ operational | existing suite |
| Step 2p — PO standardise+sort | ✅ implemented 2026-11-04 | 6/6 StandardizeAndSortPOSpec |
| Step 2m — merge payment+PO streams | ✅ implemented 2026-11-04 | included above |
| Step 3 — CKB post → Instrument Ledger | ✅ operational | existing suite |
| Step 5v — CKB view extraction | ✅ implemented 2026-11-04 | 40/40 suite (CKBViewsSpec — enriched schemas) |
| Step 20 — GL post (CCP1) | ✅ implemented 2026-11-04 | 7/7 GLPostSpec |
| Step 21 — Permutation generator (CCP1) | ✅ implemented 2026-11-04 | included above |
| All 40/40 suite tests pass | ✅ | 2026-11-04 |

### What is wired (orchestration — updated 2026-11-04)

**`run_pipeline.sh` `run_step()` function:**
Steps 2p, 2m, 5v, 20, 21 all have explicit `case` blocks with correct input/output file
derivation and RESULT-line metric extraction. No step falls through to the generic grep branch.

**`write_canonical_evidence.sh` `append_process()` function:**
Steps 2p, 2m, 5v, 20, 21 all have explicit `case` blocks producing `process_metrics.csv` rows
and `partition_catalog.csv` entries. The `*) return 0` fallthrough is only reached for
unimplemented legacy steps (1, 4, 5, 7, 8, 9, 11).

**CCP curve labels in `run_pipeline.sh` header:**
CCP1, CCP2, CCP3 are documented as named `--steps` sequences. `--curve` is a free label;
pass the CCP name as the label for evidence tracing.

| CCP | `--steps` value | `--curve` label |
|-----|----------------|----------------|
| CCP1 | `2,2p,2m,3,20,21` | `CCP1` |
| CCP2 | `2,2p,2m,3` | `CCP2` |
| CCP3 | `2,2p,2m,3,5v` | `CCP3` |

### Required work before corpus run

1. ✅ ~~Add `case "2p"`, `case "2m"`, `case "5v"` to `run_step()` in `run_pipeline.sh`~~ — done.
2. ✅ ~~Add `case "2p"`, `case "2m"`, `case "5v"` to `append_process()` in `write_canonical_evidence.sh`~~ — done.
3. ✅ ~~Add CCP curve step sequences to `run_pipeline.sh`~~ — documented in header.
4. **Validate on 1–2 years of real data:** confirm all `process_metrics.csv` rows non-zero,
   all four view files produced, GL and permutation tables produced, control totals pass.
5. Only then run the full 14-year corpus (three replicates each).

---

## 7. Execution Sequence

Conservative staged sequence — approved 2026-11-04. Model code is validated on small data
before any full-corpus commitment.

```
Stage 1 — No corpus run (now)
  AIM5: fit_agency_name_classifier.py
        input:  data/corpus_profile/agency_alias_candidates.csv  [exists]
        output: data/corpus_profile/agency_reconciliation_table.csv
        gate:   precision ≥ 0.99 on held-out exact matches;
                ambiguous cases (confidence < 0.70) flagged, not force-resolved

Stage 2 — 1-year validation run (FY2003)
  bash scripts/run_pipeline.sh --years 03 --steps 2,2p,2m,3,5v --curve CCP3 \
       --config validate-1yr
  bash scripts/run_pipeline.sh --years 03 --steps 2,2p,2m,3,20,21 --curve CCP1 \
       --config validate-1yr
  Gates: process_metrics.csv non-zero for all steps; ledger controls PASS;
         all 4 view files produced; GL + PERM_TBL produced.
  Smoke AIM1/3/4 scripts against 1-year outputs:
    confirm code executes without error — not a threshold check

Stage 3 — 5-year slice (FY2003–FY2007)
  bash scripts/run_pipeline.sh --years 03,04,05,06,07 --steps 2,2p,2m,3,5v \
       --curve CCP3 --config slice-5yr
  Run AIM1, AIM3, AIM4 against slice outputs — directional check:
    AIM1: non-zero gap between CCP1 and CCP2 substrates
    AIM3: C-stat computable and > 0
    AIM4: ≥1 commodity class shows Q4 seasonal pattern
  Run AIM2 against slice — code correctness only:
    7-window loop executes; RMSE outputs are numeric
    (final threshold verdict requires full 14 years)

Stage 4 — Full 14-year corpus (FY2003–FY2016), 3 replicates each CCP
  All AIM tests run against locked thresholds.
  Final evidence for both papers.
  AIM2 final verdict (7-window, full corpus, AIM5 reconciliation table required).
```

**Constraints:**
- AIM5 has no corpus dependency — run first
- 1-year validation before any slice or full run — pipeline correctness gate
- FY2003–FY2007 slice needed before full run — model code correctness gate
- AIM2 final verdict requires full 14 years (7-window protocol)
- AIM5 before AIM2 (agency reconciliation table required)
- AIM5 before CCP5 (clean agency keys for allocation)
- CCP4, CCP5 blocked on CCP3 and engine contract audits in VERIFICATION_AUDIT_REGISTER.md

---

## 8. Corpus Facts

| Fact | Value | Source |
|------|-------|--------|
| APA expenditure files | FY2003–FY2016, 350 files, 20.3 GB, tab-delimited | `data/VARawFiles/` |
| APA expenditure columns | `AGENCY_NAME, AMOUNT, FUND_NAME, LOCALITY, OBJECT_NAME, PROGRAM_NAME, VENDOR_NAME, VOUCHER_NUMBER, VOUCHER_DATE` | Header inspection |
| eVA PO files | FY2002–FY2016, 12 usable fiscal years (FY2004/2005/2009 absent or stub) | `data/VAPOData/` |
| eVA PO columns | `PONUMBER, AGENCY, ORDEREDDATE, QUANTITYORDERED, PRICE, VENDORID, VENDORNAME, NIGPCODE, …` | No Object/Fund code |
| Total parsed PO records | 133.46M accepted + 326K quarantined (FY2006–FY2016) | `docs/CORPUS_FINDINGS.md` |
| Vendor-year rows | 210,496 (FY2002–FY2016) | `data/VAPOData/vendor_po_attributes_by_year.csv` |
| Unique vendors | ~65,000; 27,736 single-year; 7,553 appear 8+ years | derived |
| Single-year vendor churn | 43% | derived |
| Agency name reconciliation | 99.1% exact match; 13 unmatched (20,512 rows); 3 ambiguous (131,809 rows) | `agency_alias_candidates.csv` |
| Zenodo DOI | `10.5281/zenodo.23846362` | `docs/ZENODO_ARCHIVE_PLAN.md` |

**AIM0 finding (closed 2026-11-01):** No deterministic PO-to-payment link exists in the
public corpus. AIM1–AIM5 use aggregate joins at `(agency, vendor, fiscal_year)` only.

---

## 9. Confirmed Decisions (locked 2026-11-04)

All values below were confirmed by the project owner on 2026-11-04. Do not adjust after any
test has been run.

| Decision | Tests affected | Confirmed value |
|----------|---------------|----------------|
| AIM2 RMSE improvement threshold | AIM2 | ≥ 5% over posted-layer baseline |
| AIM2 permutation p-value | AIM2 | < 0.05 |
| AIM3 C-statistic floor | AIM3 | > 0.65 |
| AIM3 permutation p-value | AIM3 | < 0.05 |
| AIM4 seasonality class count + p | AIM4 | ≥ 3 of top 5 classes, p < 0.05 |
| AIM5 precision floor | AIM5 | ≥ 0.99 on held-out exact matches |
| AIM5 ambiguity threshold | AIM5 | < 0.70 confidence = genuine ambiguity |
| Surrogate key formula | AIM2, AIM3, AIM4 | `SHA256(VENDORID)` first 8 hex chars; locked — do not change after any split assignment |
| Longitudinal evaluation | AIM2, AIM3 | 7 rolling train/test windows, FY2003–FY2016 |
| AIM1 revision suffix inclusion | AIM1, AIM2 | Include `-Vn` suffix as distinct dimension |
| Three replicates required | CCP1–CCP5 | Median across three replicates; no single run is a cost point |

---

## 10. Workload Manifest Template

Before any CCP run is filed as evidence, commit a workload manifest:

```
workload_id:          CCP2-YYYY-MM-DD-001
pipeline_revision:    <git SHA>
input_snapshot:
  SortedJE_hash:      <SHA-256 of merged SortedJEFY{yr}merged.csv>
  LDGR_hash:          <SHA-256 of LDGR{yr}.csv>
  VendorMaster_hash:  <SHA-256 of VendorMaster_full.csv>
steps_active:         [2, 2p, 2m, 3]
ccp_label:            CCP2
run_date:             YYYY-MM-DD
replicate_count:      3
```

Two runs are only comparable when their manifests agree on `pipeline_revision`, all
`input_snapshot` hashes, `steps_active`, and `ccp_label`.

---

*See also: [`docs/RESEARCH_OBJECTIVES.md`](RESEARCH_OBJECTIVES.md) — master map of all
experiments, run order, and open decisions.*
