# VA Corpus — Unified Test Design (CCP Cost Points + Paper 2 Model Evidence)

**Status:** Authoritative. Supersedes `TEST_DESIGN_PAPER_1.md` and `TEST_DESIGN_PAPER_2.md`.
**Prepared:** 2026-11-04. **Revised:** 2026-11-05 — four-model experiment replaces AIM-centric Paper 2 design.
**Companion:** [`docs/RESEARCH_OBJECTIVES.md`](RESEARCH_OBJECTIVES.md) — master objectives and run-order map

---

## 1. One Corpus. One Run. Two Papers. Two Kinds of Evidence.

The CCP cost measurements and the Paper 2 model experiment draw from the same pipeline run
against the same corpus. They are not two separate campaigns. But they produce two different
kinds of evidence:

**Paper 1 — Cost evidence:** CCP1 (GL baseline) vs CCP2–CCP5 (CKB instrument-ledger
architecture). The delta CCP1→CCP2 is the architectural cost gap. The delta CCP2→CCP5 tests
single-pass universality. Measured in compute time, storage, and peak memory.

**Paper 2 — AI consequence evidence:** Two parts, one overriding claim.

*Part 1 — Question space (structural proof):* The posting architecture defines a question
space smaller than the space of economically meaningful questions. AIM1, AIM3, and AIM4
illustrate this by running the same model against two substrates — the CCP1 posted output
and the CCP2/CCP3 instrument ledger — and showing the gap. These are illustrations of a
claim Paper 1's permutation table already proves structurally. They are not the primary
Paper 2 evidence.

*Part 2 — Substrate quality (the three-model experiment):* The primary Paper 2 evidence.
Three models, same corpus, same downstream tasks. The gaps between models measure the cost
of today's institution pattern — fine-tuning a pretrained LLM on posted data.

**The two papers are two facets of the same architectural failure, measured from the same data.**
Paper 1 states the cost claim. Paper 2 states the AI-consequence claim.

---

## 2. The Three-Model Experiment (primary Paper 2 Part 2 evidence)

Paper 2's primary experiment trains three models on the same VA corpus and evaluates them on
the same downstream tasks. The models differ only in training substrate and whether a language
prior is present:

| # | Model name | Training substrate | Language prior | Can answer Net Income? | Can answer vendor survival? |
|---|------------|-------------------|---------------|----------------------|----------------------------|
| 1 | **Posted-FineTune** | Pretrained LLM fine-tuned on posted data (`GL20YY.csv`) | Yes — survives fine-tuning | From posting layer only | No — field absent |
| 2 | **IL-FineTune** | Pretrained LLM fine-tuned on Instrument Ledger (`LDGR20YY.csv`) | Yes — survives fine-tuning | Yes | Yes |
| 3 | **IL-Native** | Structured model built directly from Instrument Ledger — no pretrained LLM | None | Yes | Yes |

**Gap 1→2** = substrate effect: same contaminated language prior, better data
**Gap 2→3** = language prior effect: same data (IL), prior removed ("See Spot Run")
**Gap 1→3** = total cost of the current institution paradigm

**Net Income closes the experiment:** IL-Native answers it (IL is the posting layer). Posted-FineTune
cannot answer it from events alone — Net Income is a chart-of-accounts classification, not an
economic fact in the raw data. This demonstrates that AI completes accounting; it does not replace it.

**Pipeline inputs:**

| Model | Pipeline output consumed |
|-------|-------------------------|
| Posted-FineTune | `GL20YY.csv` (CCP1 step 20) |
| IL-FineTune | `LDGR20YY.csv` (CCP2 step 3) |
| IL-Native | `LDGR20YY.csv` + Step 5v views (CCP2 + CCP3) |

**Output:** `data/test_evidence/three_model_experiment_results.csv`

**Status:** Design agreed 2026-11-05. Python model training scripts not yet written —
owner approval required before implementation (AGENTS.md).

---

## 3. The Two-Substrate Design (AIM tests — Paper 2 Part 1 illustrations)

AIM1, AIM3, and AIM4 each run **twice** against outputs of the same pipeline — once against
the CCP1 posted substrates, once against the CCP2/CCP3 instrument ledger outputs. The gap
illustrates the information the posting process permanently destroys. AIM2 and AIM5 have
reduced status (see §5).

| Substrate | Produced by | Architecture | Expected AIM result |
|-----------|------------|--------------|---------------------|
| GL output (`GL20YY.csv`) | CCP1 step 20 | Account-keyed; instrument identity discarded | AIM1/3/4 miss — vendor identity and order dates gone |
| Permutation table (`PERM_TBL20YY.csv`) | CCP1 step 21 | All key-embedded permutations from original transactions | AIM1/3/4 still miss — event sequence unrecoverable |
| Instrument Ledger (`LDGR20YY.csv`) | CCP2 step 3 | Instrument-keyed; vendor identity preserved | AIM1 succeeds |
| Step 5v views (`view_*.csv`) | CCP3 step 5v | Bounded aggregation views of the CKB stream | AIM3, AIM4 succeed |

The gap between the two substrates is not a comparison of algorithms — the same algorithm
runs against both. The difference is what survived posting.

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
**`permutation_rows`** = sum over GL keys of distinct vendors, kept for continuity with the earlier
row count (CCP1 only; `0` for CCP2–CCP5).
**`permutation_capacity`** = sum over GL keys of the product of per-dimension distinct counts, with
dimensions determined by another dimension removed (CCP1 only; `0` for CCP2–CCP5). Stored as a
decimal string because it can exceed 64-bit integers. The cross-year sum is computed in Scala
(step `21s`, BigInt); the shell only forwards the `RESULT` line.

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

### CCP1 — General Ledger (right-wall anchor)

**Architecture:** Account-keyed GL post — the most collapsed instance of the fixed-posting-key
class. Vendor identity is not an input to the accumulation key. A purchasing sub-ledger, data
warehouse, or data mart would preserve more dimensions but shares the same structural property:
pre-computed answers to questions fixed at design time. CCP1 measures the GL as a right-wall
proxy for this class. Steps 20 and 21 read the same SortedJE stream independently.

**Step 20 — GL post (revised 2026-11-11):** key = agency, fund, object, nominal account, period,
debit/offset flag (`transDirVsOffsetFlg`, an important GL field). Accumulate the amounts of every
line into `GL20YY.csv` (`dir_flag` column). Instrument identity (`transIPID`) is intentionally
excluded from the key. The flag is now `D` on the debit line and `O` on the offset line in
`transStandardize` and `standardizeAndSort` (it was `D` on both lines, which is why the old
"debit only" filter selected nothing). `RESULT` carries `net_amount`, which must be 0.00.

**Step 21 — Permutation capacity (revised 2026-11-11):** per GL key, count the distinct values of
every other SJE column, excluding journal ID (3), amount (25), unit price (27) and statistic (28).
While reading, Scala detects year-wide functional dependencies (A -> B when every value of A co-occurred
with one value of B). A dimension that another one determines adds no combinations and is dropped.
`capacity` = the product of the remaining distinct counts: the number of balance rows a fixed-key
system would have to hold to answer a question on any of those attributes. `capacity_independent`
(nothing dropped) is reported beside it. Only counts are kept; nothing is materialised.
`PERM_TBL20YY.csv` has one row per GL key (`row_count`, `d_<column>` for each column that varies,
`capacity`); `PERM_SUMMARY20YY.csv` has the year totals. Constant columns contribute a factor of 1.
`RESULT` lists `car_dims`, `dropped_dims` and `capacity_dims`.

**CAR fields:** any dimension determined by the vendor is a CAR field (`car_dims`, detected), and
`transContractID` is CAR by definition (declared; constant `"0"` in every current producer, and no
producer varies contract by PO or payment). NIGP and geo region are functions of vendor, so they are CAR.

**Reading the three numbers:** (1) GL rows = what a typical GL holds and which questions it can
answer; (2) `capacity` = the pre-materialised space needed to answer arbitrary pivots (still an upper
bound: dependencies are removed, the remaining dimensions are multiplied as independent); (3) ledger
rows (step 3) = what the Instrument Ledger holds, which are only the combinations that occurred. A
pivot that is not CAR (for example injectivity) is one the IL cannot answer.

**Superseded:** until 2026-11-10 step 21 keyed on GL key plus vendor and reported 1.2–2.5x the GL
rows. That number could never exceed the ledger row count, so it is not an explosion measure and
must not be cited for Paper 1.

**Known measurement conservatism:** The CCP1 sort (Step 2) uses the same vendor-bearing
SortedJE key as CCP2, which slightly overstates legacy GL sort cost. The CCP1→CCP2 delta is
therefore a lower bound on the true architectural cost difference.

**Required metrics (`process_metrics.csv`):** `records_read`, `output_rows` (GL rows for
step 20; GL keys for step 21), `wall_seconds`, `cpu_user_seconds`, `cpu_system_seconds`,
`cpu_total_seconds`, `peak_rss_bytes`. CPU seconds come from `/usr/bin/time` around the `sbt` process
tree (includes sbt/JVM start-up) and do not inflate when lanes overlap, unlike wall seconds. Step 21
`RESULT` also carries `vendor_rows`, `capacity`, `capacity_independent`, `varying_dims`, `car_dims`
and `dropped_dims`.

**Status:** Steps 20 and 21 revised 2026-11-11 and unit tested (GLPostSpec 13, suite 78/78). Run on
the FY03 small fixture only (27 GL keys, capacity 6,292 = independent bound, CPU seconds populated).
Not yet run on the corpus in the revised form.

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

**Role of AIM tests in the research (2026-11-05 reframe):**

AIM tests are illustrations of the Paper 2 Part 1 claim — they make the information
destruction caused by posting concrete and specific. They are *not* the primary evidence
for Paper 2. The primary evidence is Paper 1's permutation table (structural proof) for
Part 1, and the four-model experiment (§5a below) for Part 2.

- **AIM1, AIM3, AIM4** — primary Part 1 illustrations. Run against both substrates;
  the gap between CCP1 and CCP2/CCP3 results demonstrates structural information loss.
- **AIM2** — secondary. Subsumed by the Model 1 vs. Model 4 forecasting comparison in
  the four-model experiment. Remains in the test plan; not a standalone primary result.
- **AIM5** — secondary infrastructure. Unblocks AIM2 and CCP5 (data quality step).
  Not a primary Paper 2 result.

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

### AIM2 — Agency expenditure forecasting (Layer 2 vs Layer 3) *(secondary)*

**Role:** Secondary. Subsumed by the Model 1 vs. Model 4 forecasting comparison in the
four-model experiment (§5a). Remains in the test plan as a standalone check; not a
primary Paper 2 result.

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

### AIM5 — Agency name classifier *(secondary — infrastructure)*

**Role:** Secondary infrastructure. Unblocks AIM2 (agency join) and CCP5 (allocation
step). Not a primary Paper 2 result. Can run before the corpus pipeline — no pipeline
dependency.

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

## 5a. Three-Model Experiment (primary Paper 2 Part 2 evidence)

This section expands the model table introduced in §2. See §2 for the full model definitions,
pipeline inputs, and output file. Key details repeated here for completeness:

**Models:** Posted-FineTune (1), IL-FineTune (2), IL-Native (3)

**Gaps:**
- Gap 1→2 = substrate effect (same language prior, better data)
- Gap 2→3 = language prior effect ("See Spot Run" — same IL data, prior removed)
- Gap 1→3 = total cost of the current institution paradigm

### Downstream evaluation tasks (same tasks, all three models)

- Vendor survival prediction (AIM3 question — does the model answer it from its substrate?)
- Expenditure forecasting (AIM2 question — supersedes standalone AIM2 as primary result)
- Net Income derivation (logical proof — IL-Native answers it via the posting layer;
  Posted-FineTune cannot, because Net Income is a chart-of-accounts classification, not an
  economic fact in the events)

### Verbalization approach

Posted-FineTune and IL-FineTune consume verbalized records (structured fields →
natural-language descriptions) for fine-tuning the pretrained LLM. IL-Native consumes the
IL directly as structured tabular input — no verbalization. The verbalization schema is to
be defined when Python training scripts are written.

### Output

`data/test_evidence/three_model_experiment_results.csv` — per-model, per-task performance
metrics; gap columns computed across model pairs.

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
| eVA PO files | 17 files, FY2001–FY2018 (no FY2009 file; FY2001/2003/2004/2005 are stubs or partial); pipeline uses FY2006–FY2008 and FY2010–FY2016 (eligible-year rule) | `data/VAPOData/` |
| eVA PO columns | `PONUMBER, AGENCY, ORDEREDDATE, QUANTITYORDERED, PRICE, VENDORID, VENDORNAME, NIGPCODE, …` | No Object/Fund code |
| Total parsed PO records | **18,407,581 parsed; 18,081,738 accepted** (all 17 files, FY2001–FY2018 per `docs/CORPUS_FINDINGS.md`). Pipeline run FY2003–FY2016: 15,603,041 PO source rows (audit register, corpus run). *Note: the earlier figure "133.46M" was the all-family total (expenditure + revenue + budget + PO combined), not PO alone.* | `docs/CORPUS_FINDINGS.md`; `docs/VERIFICATION_AUDIT_REGISTER.md` |
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
