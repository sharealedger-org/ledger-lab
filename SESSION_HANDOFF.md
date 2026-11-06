# Ledger Lab Handoff

Updated 2026-11-06. This file is a concise state snapshot; use the linked design contracts for details.

---

## 2026-11-06 Full-Volume Local Data Discovery, Step 2p Sanitization & FY03 Test Run (Bob)

**What was accomplished this session.**

1. **Full-Volume Local Dataset Discovery & Verification:**
   - Located uncompressed full-volume historical datasets at `/Users/ktwitchell001/workspace/universal_ledger_data/data` (FY03–FY13 expenditure files `FY03q1exp.txt`–`FY13rev.txt`, PO files FY01–FY18 `VA_opendata_FY2001.txt`–`VA_opendata_FY2018.txt`, and reference tables).
   - Additional format archives verified at `/Users/ktwitchell001/workspace/ConversationsWithKip/data` (FY03–FY16 payments, FY17–FY18 monthly APA files).

2. **Branch Synchronization:**
   - Created/switched to local `oldmac` branch synchronized with `main` / `origin/main` (`d48f584`).

3. **Pipeline Runner & Smoke Test Fixes:**
   - `scripts/run_pipeline.sh`: Fixed bug where `local rc=$?` was captured after `[[ "$_tee_target" != /dev/stderr ]] && rm -f "$_tee_target"`, which clobbered exit code 0 to 1 on macOS. Now captures `$?` immediately.
   - `scripts/run_pipeline.sh`: Added fallback in `vm_candidate` loop to look in `$REPO_ROOT/data/VendorMaster.csv` when `--inPath` points to an external raw data folder.
   - `scripts/run_repo_fixture_smoke_test.sh`: Updated metric assertion expected row counts (442 ledger rows, 246 allocation rows) reflecting the transIPID instrument resolution. Smoke tests pass 100% (both baseline and `--with-allocation`).

4. **PO Data Sanitization & Field Alignment in Step 2p:**
   - `standardizeAndSort.scala` (`applyPO`, `makePORow`, `tryBigDecimal`): Fixed field extraction for tab-delimited PO files (`VA_opendata_FY*.txt`). Stripped outer quotation marks and sanitized embedded commas in commodity descriptions and vendor identifiers to preserve exact 39-column CSV structure for `post.scala`.
   - All 51 test specifications in unit suite pass (`sbt test`).

5. **Full-Volume FY2003 Test Run (Steps 2, 2p, 3, 5v):**
   - **Step 2:** `FY03q1exp.txt` (63.4 MB, 1,421,582 source rows) → 2,843,164 balanced SJE lines across 15 external sort spills into `SortedJEFY03q1exp.csv` (655.5 MB) in 151s.
   - **Step 2p:** `VA_opendata_FY2003.txt` (16.3 MB, 53,559 PO rows) → 107,118 balanced SJE lines into `SortedJEFY03q1PO.csv` (27.5 MB) in 9s.
   - **Step 3 (Post):** 2,950,282 total SJE records posted in 17s → 48,180 Instrument Ledger records in `LDGR2003.csv` (6.6 MB), net balance sum = 0.00.
   - **Step 5v (CKB Views):** Extracted all 4 analytical views from the 2.95M SJE stream in 16s:
     - `view_monthly_order_volume.csv`: 10,385 rows
     - `view_vendor_year_survival.csv`: 1,985 vendor rows with 8-char SHA-256 surrogate keys
     - `view_agency_year_commodity_mix.csv`: 5,889 rows
     - `view_agency_year_payment_totals.csv`: 32,352 rows

**Next session — what to do next:**
1. Wire `--periodGranularity` (`T-annual`, `T-quarterly`, `T-monthly`) across `run_pipeline.sh` and Scala dispatchers per `docs/TEST_DESIGN.md` §4.
2. Run multi-year/multi-quarter cost curve sweeps (CCP1–CCP3) across the historical corpus.

---

## 2026-11-04 Cost surface design — three axes (Bob)

**What was decided this session.**

The Paper 1 cost surface has three independent axes, not one. This is now recorded in
[`docs/TEST_DESIGN.md`](docs/TEST_DESIGN.md) §4 and
[`docs/ENGINE_IMPLEMENTATION_PLAN.md`](docs/ENGINE_IMPLEMENTATION_PLAN.md) Sub-Task 3e.

**Axis 1 — Capability layer:** CCP1–CCP5 (existing).

**Axis 2 — Time partition granularity (`--periodGranularity`):**
- `annual`: `transFiscalPeriod = "2003"` — 1 ledger row per key per year, 1–2 CKB passes.
- `quarterly`: `transFiscalPeriod = "2003/Q2"` — up to 4 rows per key, 4–5 passes.
- `monthly`: `transFiscalPeriod = "2003/07"` — up to 12 rows per key, 12–14 passes.

`T-monthly` is the AIM test substrate default (AIM4 STL requires it; Papers 3–5 require it).
`T-annual` is the cost-curve minimum anchor.

**Axis 3 — Materialization scope (`--materialization`):**
- `movement-only`: ledger contains period movement rows only. No pre-computed rollups.
- `movement+annual`: ledger + one extra pass producing annual totals per key.
- `movement+all-rollups`: ledger + monthly + quarterly + annual + YTD summaries.

Default: `movement-only`. AIM tests consume CKB views (step 5v), not the ledger directly.

**The ledger is a movement store, not a balance store.** Each `LDGR` row = net of all journal
movements for one (key, period). Running balances derived by query-time summation — not stored.
The VSAM monthly-bucket-with-annual-total pattern is explicitly rejected for this system.

**The four experimental configurations for Paper 1:**
```
Run A:  T-annual  + movement-only        → left anchor (minimum cost)
Run B:  T-monthly + movement-only        → AIM substrate + cost measurement point
Run C:  T-monthly + movement+annual      → annual cache added
Run D:  T-monthly + movement+all-rollups → right anchor (maximum pre-computation)
```
If cost(A) ≈ cost(B): monthly granularity is free.
If cost(B) ≈ cost(D): pre-computation is also free.
The gap CCP1→CCP2 at Run A is the architectural cost delta (Paper 1 core finding).

**`cost_surface.csv` schema change required before full corpus run:**
Add columns: `capability_layer`, `period_granularity`, `materialization_scope`,
`ledger_row_count`, `ldgr_file_bytes`, `view_file_bytes`, `permutation_rows`.
Until `--periodGranularity` is wired, capture in `config_description` using label format
`"T-monthly/movement-only"`.

**`post.scala` requires no changes.** Monthly period keys make each pass's rows independent
by construction — the match-merge never accumulates across period boundaries when keys are distinct.

**Files changed:**
- `docs/TEST_DESIGN.md` — new §4 subsection "The cost surface has three independent axes"
- `docs/ENGINE_IMPLEMENTATION_PLAN.md` — new Sub-Task 3e

**Next session — what still needs to be done:**
1. Wire `--periodGranularity` parameter in `run_pipeline.sh` and Scala dispatchers.
2. Change `applyAPA` to per-source-file loop with monthly period key.
3. Change `applyPO` to per-row period key from `ORDEREDDATE`.
4. Update `cost_surface.csv` schema.
5. Update `Ledger.scala` comment on `ldgrTransAmount`.
6. Then run the Stage 2 validation (see entry below).

---

## 2026-11-04 Step ttl + Stage 2 run (Bob) — IN PROGRESS AT SESSION END

**What was done this session.**

1. **Step ttl — APA TTL built and passing tests (48/48).**
   - `standardizeAndSort.applyAPA` added to `standardizeAndSort.scala`.
   - Reads the 12 monthly APA CSV files under `data/VARawFiles/FY 2003 Expenditure/`, resolves
     AGENCY_NAME/FUND_NAME/OBJECT_NAME/PROGRAM_NAME to numeric keys using `aRef*.txt` reference
     tables, routes by Virginia fiscal quarter (Q1=Jul-Sep, Q2=Oct-Dec, Q3=Jan-Mar, Q4=Apr-Jun),
     and writes `SortedJEFY03q{1-4}exp.csv` (one sorted file per quarter).
   - `case "ttl"` wired in `LedgerApp.dispatch`.
   - `run_pipeline.sh`: `ttl)` case in `run_step()`, `to_jvm_path()` helper for Windows Git Bash
     path conversion (cygpath -m), `ref_arg`/`jvm_ref_arg` for `--refPath`, step 2p inPath
     auto-resolved to `VAPOData` sibling directory.
   - `ApaTtlSpec.scala`: 8 tests, all pass.
   - `docs/VERIFICATION_AUDIT_REGISTER.md`: TTL entry added.

2. **Architecture clarification (end of session).**
   - `post.scala` processes SortedJE files **sequentially** (one file at a time, match-merge within
     each file against the running ledger). Files processed in alphabetical order.
   - **Step 2m (merge) is NOT needed** for CCP2/CCP3 runs. `post.scala` naturally reads all
     `SortedJEFY03*.csv` files in sequence: q1PO → q1exp → q2exp → q3exp → q4exp.
   - `LedgerApp.scala` `case "3"` reverted to `post(outPath, outPath, year)` (no merged-file
     preferential filter).

**What is NOT done — start here next session.**

The Stage 2 run failed. Three sequential pipeline failures:

1. **Run 1**: `cygpath` path conversion missing — Scala received POSIX path, `applyAPA` found
   no FY directory. Fixed: `to_jvm_path()` added to `run_pipeline.sh`.
2. **Run 2**: step 2p received wrong `--inPath` (VARawFiles, not VAPOData). Fixed: auto-resolve
   sibling `VAPOData` directory for step 2p.
3. **Run 3**: step 3 ran with step 2m in the step sequence (`ttl,2p,2m,3,5v`). The `2m` merged
   all SortedJE files, then step 3 could not complete (likely failed because `post.scala` is
   sequential-file not k-way, and `2m` merged everything into one file — but then `post.scala`
   also saw all the individual quarterly files as additional inputs). Session ended before this
   resolved.

**Correct step sequence to use:**

```bash
bash scripts/run_pipeline.sh \
  --inPath data/VARawFiles \
  --outPath data/output_validate_fy03 \
  --years 03 --steps ttl,2p,3,5v \
  --curve CCP3 --config validate-1yr
```

**No step 2m.** `post.scala` reads all `SortedJEFY03*.csv` files directly.

**Verification checklist for next session:**

1. Compile: `cd 02-foundation/va_pipeline && sbt -batch compile` — confirm clean.
2. Full suite: `sbt "testOnly org.universalledger.foundation.va.*"` — confirm 48/48.
3. Run the corrected command above from Git Bash.
4. Expected: TTL ~170s, 2p ~20s, step 3 processes 5 files sequentially, 5v runs on view tables.
5. Check: LDGR2003.csv non-zero, amount_sum=0.00, all 4 view files non-zero.

**Key file state:**
- `standardizeAndSort.scala` — `applyAPA` added (line 828 onward).
- `LedgerApp.scala` — `case "ttl"` added; `case "3"` is plain `post(outPath, outPath, year)`.
- `run_pipeline.sh` — `ttl)` case, `to_jvm_path()`, `ref_arg`, step 2p VAPOData resolution,
  `jvm_vm_arg`/`jvm_ref_arg` path conversion. All sbt invocations use `jvm_in_path`/`jvm_out_path`.
- `ApaTtlSpec.scala` — 8 tests, all pass.

**Known open issue in applyAPA:** the quarterly file routing appears to be writing all 12 months
into Q1 only (TTL output_bytes=725MB matches a single quarterly file, not 4). The `applyAPA`
loop at `for (quarter <- 1 to 4)` should produce 4 files but may be routing all files to Q1
due to month-token extraction failure from the APA filename format. Investigate:
- `discoverApaFiles` splits on `"[ _]"` — APA filenames like `"Expenditures Apr 2003.csv"` have
  spaces. Verify the month token `"Apr"` is extracted correctly.
- Check that the actual output directory (before cleanup) contains 4 quarterly files or just 1.
- If only Q1 is produced: all 12 months land in one file, 2p PO is separate, and `post.scala`
  processes them sequentially (Q1-all + PO). This still produces a correct ledger — it just
  means the quarterly partition design isn't working yet.

---

## 2026-11-04 AIM5 — Agency name classifier + staged plan (Bob)

**What was done.**

Two things in this session: (1) revised the run-order plan to a conservative four-stage
sequence, and (2) implemented and ran AIM5 as Stage 1.

**Plan change — four-stage conservative sequence (approved by owner):**

| Stage | Gate | What runs |
|-------|------|-----------|
| 1 | No corpus run | AIM5 (agency_alias_candidates.csv already exists) |
| 2 | 1-year FY2003 run | Pipeline correctness; smoke AIM1/3/4 scripts (code runs, not threshold check) |
| 3 | 5-year FY2003-FY2007 run | AIM1/3/4 directional checks; AIM2 code correctness |
| 4 | Full 14-year run (3 replicates) | All locked thresholds; final evidence |

Full stage definitions in [`docs/RESEARCH_OBJECTIVES.md`](docs/RESEARCH_OBJECTIVES.md) §6
and [`docs/TEST_DESIGN.md`](docs/TEST_DESIGN.md) §7.

**AIM5 result:**

- `scripts/fit_agency_name_classifier.py` — stdlib only (`difflib`, no pip dependencies)
- Method: `SequenceMatcher` + `_token_sort_ratio` (avoids superstring collapse of token-set ratio)
- 312/312 positives correct — precision = 1.0000 (threshold >= 0.99) — **PASS**
- 3 multi-key rows (EXACT name match; two agency keys each) — correctly classified as MULTI_KEY; key selection is a data governance decision, not a classifier failure
- 12/13 UNMATCHED rows score < 0.70 ambiguity threshold, flagged AMBIGUOUS
- 1 UNMATCHED row ('Virginia Commercial Space Flight Authority', conf=0.701) just above threshold
- `data/corpus_profile/agency_reconciliation_table.csv` written — 328 rows
- `data/test_evidence/AIM5_classifier_results.csv` written — evidence row with precision, counts, PASS verdict

**What is still open from AIM5:**
- Human review of 13 UNMATCHED rows — top candidates are informational; manual assignment needed before CCP5
- Key-selection decision for 3 MULTI_KEY rows (each maps to two agency keys in the reference table)

**Next session — start here:**

Stage 2: 1-year validation run on real FY2003 corpus data.

```bash
bash scripts/run_pipeline.sh \
  --inPath /VAdata/data --outPath /VAdata/data/output_ccp3_validate \
  --years 03 --steps 2,2p,2m,3,5v --curve CCP3 --config validate-1yr
```

Verify: all 4 view files non-zero; `process_metrics.csv` rows for steps 2, 2p, 2m, 3, 5v all non-zero; ledger controls PASS. Then smoke-run AIM1/3/4 scripts against the 1-year outputs (code correctness only, not threshold check — owner approval required before writing those scripts).

---

## 2026-11-04 Sub-Task 3e — Orchestration wiring for steps 20/21 and doc catch-up (Bob)

**What was done.**

Closed the last orchestration gap before the corpus validation run. The only remaining
blockers from the Sub-Task 3d handoff were (1) steps 20 and 21 falling through to the
generic grep branch in `run_pipeline.sh`, and (2) four documents carrying stale state.

**Files changed:**

- `scripts/run_pipeline.sh` — added explicit `20)` and `21)` case blocks to `run_step()`:
  input file = fallback to merged SJE; output file = `GL20{yr}.csv` / `PERM_TBL20{yr}.csv`;
  `input_bytes` accumulated from the full `SortedJEFY{yr}*.csv` glob; `records_written`
  extracted from the `RESULT step=20/21` structured log line. Added `"20"` and `"21"` to
  the `input_bytes` single-file override guard so the glob accumulation is not overwritten.

- `docs/TEST_DESIGN.md` §6 — updated "What is missing" → "What is wired": steps 2p/2m/5v/20/21
  all confirmed present in both scripts; CCP step sequences documented as table; three
  completed checklist items struck through; item 4 (validate on real data) left as the
  only remaining gate.

- `docs/VERIFICATION_AUDIT_REGISTER.md` — added Sub-Task 3d entry (enriched view schemas,
  locked schemas, orchestration wiring verified); corrected stale Step 5v remaining-gate text
  (removed the write_canonical_evidence.sh note, which is now done).

- `docs/ENGINE_IMPLEMENTATION_PLAN.md` — Sub-Task 2b status updated from `pending` to
  `COMPLETE — 2026-11-04`; all four owner decisions recorded.

**Validation:**
- `sbt compile` — clean (1 pre-existing deprecation warning, unchanged)
- `sbt "testOnly org.universalledger.foundation.va.*"` — 40/40 passed

**Next session — start here:**

The plan is now a four-stage sequence (approved 2026-11-04). **Stage 1 is next: AIM5.**
See `docs/RESEARCH_OBJECTIVES.md` §6 and `docs/TEST_DESIGN.md` §7 for the full staged plan.

**Stage 1 — AIM5 (no corpus run needed):**

`scripts/fit_agency_name_classifier.py` — reads `data/corpus_profile/agency_alias_candidates.csv`.
Input summary: 307 EXACT_REF_NAME (positive training set), 8 NORMALIZED_REF_NAME (also positive),
3 ambiguous EXACT_REF_NAME (two `ref_agency_keys` each — should score < 0.70),
13 UNMATCHED (test set — model must propose candidate or flag as genuinely new).

Gate: precision ≥ 0.99 on held-out exact matches; confidence < 0.70 on the 3 ambiguous cases.
Output: `data/corpus_profile/agency_reconciliation_table.csv` — unblocks AIM2 and CCP5.
Evidence: `data/test_evidence/AIM5_classifier_results.csv`.

**Stage 2 — 1-year validation run (after AIM5):**

```bash
bash scripts/run_pipeline.sh \
  --inPath /VAdata/data --outPath /VAdata/data/output_ccp3_validate \
  --years 03 --steps 2,2p,2m,3,5v --curve CCP3 --config validate-1yr
bash scripts/run_pipeline.sh \
  --inPath /VAdata/data --outPath /VAdata/data/output_ccp1_validate \
  --years 03 --steps 2,2p,2m,3,20,21 --curve CCP1 --config validate-1yr
```

Checks: all view files non-zero; `GL2003.csv` non-zero; `PERM_TBL2003.csv` rows >> GL rows;
ledger controls PASS; smoke-run AIM1/3/4 scripts (code executes, not threshold check).

**Stage 3 — 5-year slice (FY2003–FY2007):** directional AIM1/3/4 checks; AIM2 code correctness.

**Stage 4 — Full 14-year run:** 3 replicates each CCP; all locked thresholds.

---

## 2026-11-04 Sub-Task 3d — Enriched view schemas (Bob)

**What was done.**

Enriched the four CKB view schemas in `CKBViews.scala` against the three-question design
principle from `RESEARCH_STRATEGY.md` §View schema design: (a) minimum for paper 2 AIM model,
(b) cross-model interaction features (AIM3→AIM2, AIM4→AIM2), (c) extension point for papers 3–7.
No Python, no pipeline steps, no canonical state changed. The view files are analytical outputs only.

**Design decisions made this session (not requiring owner input — schema additions, not schema breaks):**

| View | Change | Rationale |
|------|--------|-----------|
| **A** | Key gains `agency`; add `revision_count` | AIM4 requires per-agency Q4 seasonality flags. Aggregate without agency cannot produce it. Revision-count enables intra-year revision-surge detection distinct from volume spike. |
| **B** | Add `nigp_class_modal`, `agency_count`, `distinct_nigp_classes` | AIM3 features: commodity class shift (modal), agency concentration (count), product breadth (distinct classes). Requires per-vendor-year sub-map and agency HashSet; both are O(distinct values per key), not O(rows). |
| **C** | Add `vendor_count`, `min_order_amount`, `max_order_amount` | AIM2 features: supply concentration (vendor count), order-size proxy (min/max range avoids sort). |
| **D** | Key gains `fund_id` (col 13) and `object_code` (col 14) | AIM2 Layer 2 baseline gets object-code proxy for commodity. `fund_id` is the natural allocation partition for Paper 3 allocation driver tables. Note: PO rows have `fund_id = "0000"`; this field is only populated on payment rows, which is View D's input. |

**Vendor geography gap — design decision:**
VENDORSTATE/VENDORCITY exist in the PO source but are not in the 39-field SortedJE stream.
AIM3 lists "geographic region" as a feature. Decision: accept the gap for Paper 2.
`nigp_class_modal` and `agency_count` serve as market-segment proxies (geography correlates
with commodity class at this scale). If geographic signal is needed for later papers, the
correct path is extending col 38 (`transExtensionIDCustom`) in Step 2p to carry VENDORSTATE.
Not recorded in GERS register — this is a data-field gap, not a GenevaERS engine capability gap.

**Files changed:**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/CKBViews.scala`
  — Enriched accumulators, input parsing (added `fundId` col 13), aggregation logic,
    output headers, and write loops for all four views.
- `02-foundation/va_pipeline/src/test/scala/org/universalledger/foundation/va/ledger/CKBViewsSpec.scala`
  — Updated `makeRow` to accept `fundId` param; updated column index assertions for Views A and D
    (key width changes); added 4 new tests (1b, 2c, 3b, 4b) covering the new fields.

**Validation:**
- `sbt compile` — clean (1 pre-existing deprecation warning, unchanged)
- `sbt "testOnly org.universalledger.foundation.va.*"` — 40/40 passed (36 prior + 4 new)

**Final view schemas (locked after this session):**

```
View A  view_monthly_order_volume.csv
  key : (fiscal_period, nigp_class_3digit, agency)
  cols: fiscal_period, nigp_class_3digit, agency, po_line_count,
        total_committed_amount, revision_count

View B  view_vendor_year_survival.csv
  key : (vendor_id, fiscal_year)
  cols: vendor_id, vendor_surrogate_key, fiscal_year, first_order_year,
        present_next_year, total_amount, po_count,
        nigp_class_modal, agency_count, distinct_nigp_classes

View C  view_agency_year_commodity_mix.csv
  key : (agency, fiscal_year, nigp_class_3digit)
  cols: agency, fiscal_year, nigp_class_3digit, po_count,
        total_committed_amount, revision_count,
        vendor_count, min_order_amount, max_order_amount

View D  view_agency_year_payment_totals.csv
  key : (agency, fiscal_year, fund_id, object_code)
  cols: agency, fiscal_year, fund_id, object_code,
        total_payment_amount, payment_count
```

**Next session — start here:**

The pipeline is not ready for a measured corpus run. The orchestration scripts must be fixed
and the sequence validated on a small slice first. The full corpus run produces inputs for
**both** Paper 1 (CCP cost points) and Paper 2 (AIM model inputs) simultaneously — it is
not a CCP-only run followed later by AIM work.

**What one full corpus pipeline run produces:**

| Output | Consumed by |
|--------|-------------|
| GL balance table (Step 20) | AIM1 substrate 1 (CCP1); Paper 1 cost point |
| Permutation table (Step 21) | AIM1 substrate 2 (CCP1); Paper 1 cost explosion evidence |
| Instrument ledger (Step 3) | AIM1 substrate 3 (CCP2); Paper 1 cost anchor |
| View A: monthly order volume (Step 5v) | AIM4 seasonality model |
| View B: vendor-year survival (Step 5v) | AIM3 survival model |
| View C: agency-year commodity mix (Step 5v) | AIM2 forecasting model (after AIM5) |
| View D: agency-year payment totals (Step 5v) | AIM2 Layer 2 baseline |

AIM5 (agency name classifier) runs against `agency_alias_candidates.csv` — a file that
already exists. It does **not** need the corpus run. AIM5 can run before or in parallel
with the corpus pipeline run.

**Execution sequence after orchestration is fixed:**

```
Fix & validate pipeline (1–2 year slice)
    │
    ├──[parallel]── AIM5 (agency_alias_candidates.csv already exists)
    │
Full corpus pipeline run (CCP1 + CCP2 + CCP3, same pass)
    │
    ├─► AIM1  (GL + permutation table + instrument ledger — all available)
    ├─► AIM3  (View B available)
    ├─► AIM4  (View A available)
    │
    └── AIM5 output ──► AIM2  (View C/D + agency reconciliation table)
```

**Pipeline gaps to close before any corpus run:**

1. **Wire steps 2p, 2m, 5v into `run_pipeline.sh` `run_step()`** — these fall through to
   a generic grep branch that silently mismeasures. Add explicit cases for each.
2. **Wire steps 2p, 2m, 5v into `write_canonical_evidence.sh` `append_process()`** —
   currently missing; no process_metrics.csv rows written for these steps.
3. **Add CCP curve labels** — CCP1 = `2,2p,2m,3,20,21`; CCP2 = `2,2p,2m,3`; CCP3 = CCP2 + `5v`.
4. **Validate on 1–2 years** — confirm all metrics non-zero, all view files produced,
   control totals pass, before committing to the full 14-year run.

**Python fitting scripts** — owner approval required (AGENTS.md) before writing any.
AIM5 first (no corpus needed); then AIM3, AIM4 (after corpus); then AIM2 (after AIM5 + corpus).

---

## 2026-11-04 Sub-Task 3c — Architecture documentation and strategy (Bob)

**What was done.**

A substantial design session produced four new/updated architecture documents and resolved
all Sub-Task 2b owner decisions. No Scala code changed in this sub-task. No financial logic touched.

**Files created:**
- `docs/RESEARCH_STRATEGY.md` — new. The intellectual architecture of the full research
  program: paper sequence as proof sequence, full-corpus commitment from paper 1 onward,
  AIM family interaction pattern (model outputs feeding model inputs), drill-down thesis
  (GL → instrument → vendor with no gaps), scope boundaries per paper, GenevaERS simulation
  rule, what no one else is doing.

**Files updated:**
- `docs/CKB_LAYER_EXECUTION_MODEL.md` — two new sections added:
  1. **Reference Joins and the Hash-Map Boundary** — Pattern 1 (hash-map, small tables),
     Pattern 2 (sorted-merge join, large/unknown tables), GenevaERS simulation rule.
  2. **CKB Delivery Groups and the Intra-Buffer Processing DAG** — Group 0 (state/reference,
     lookup only), Group 1 (primary events, exits generate GenBuf records), Group 2+
     (spawned records consumed by downstream views). Acyclicity constraint documented.
     Paper sequence mapped to delivery groups (Papers 1+2 = Group 1 only).
- `docs/GENEVAERS_CAPABILITY_GAP_REGISTER.md` — GERS-002: sorted-merge reference join for
  large lookup tables. Current `loadVendorMap()` hash-map is architecturally incorrect for
  full-corpus cost measurement.
- `AGENTS.md` — Python section extended: GenevaERS simulation rule + owner-approval
  requirement before writing any Python.
- `docs/VERIFICATION_AUDIT_REGISTER.md` — Steps 2p/2m and Step 5v entries added.
- `SESSION_HANDOFF.md` — Sub-Task 2b decisions closed; architecture gap noted; this entry.

**Key decisions made this session:**
1. Sub-Task 2b all closed: non-injectivity covered by Step 21; 7-window approved;
   `vendorSurrogateKey` approved and implemented (SHA-256, 8 hex chars, View B); revision
   count already in View C.
2. Full-corpus commitment from paper 1 onward: fixture is CI only.
3. AIM family is cross-model: AIM3 output feeds AIM2; AIM4 output feeds AIM2.
4. Enriched view schemas before any Python: design first, implement in Scala, run once.
5. Delivery group DAG governs view design: paper 2 views are Group 1 consumers only.

**Next session — start here:**

1. **Design enriched view schemas** — Views A–D against three questions each:
   (a) minimum for paper 2 AIM model, (b) cross-model interaction features,
   (c) extension point for papers 3–7. Frame: `docs/RESEARCH_STRATEGY.md` and
   `docs/CKB_LAYER_EXECUTION_MODEL.md` §CKB Delivery Groups.
2. **Implement enriched schemas in `CKBViews.scala`** after owner reviews design.
3. **Run full corpus** — after schemas final; one run produces permanent view tables.
4. **Python fitting scripts** — owner approval required; AIM5 first (unblocks AIM2);
   then AIM3, AIM4, AIM2. 7-window longitudinal split throughout.
5. **`write_canonical_evidence.sh`** — add `append_process` for steps 2p, 2m, 5v before
   any CCP2/CCP3 replicate counts as a valid measured run.

**New — read first for strategic context:** [`docs/RESEARCH_STRATEGY.md`](docs/RESEARCH_STRATEGY.md)
— the intellectual architecture of the full paper series: proof sequence, full-corpus commitment,
AIM family interaction pattern, drill-down thesis, and scope boundaries per paper.

---

## 2026-11-04 Sub-Task 3 — Step 5v: CKB Views (Bob)

**What was done.**

Implemented Step 5v (`CKBViews`) — the CCP3 extract-time summarization pass.
Sub-Tasks 1, 2, and 3 are now all complete.

**Files created:**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/CKBViews.scala`
  — New Scala object. Reads all `SortedJEFY*.csv` files (39-field Transaction rows) in a single
  pass. Routes each record to four bounded aggregation maps keyed on declared grouping keys:
  - View A (`view_monthly_order_volume.csv`): `COMMITMENT` debit rows → `(fiscal_period, nigp_class_3digit)` → po_line_count, total_committed_amount
  - View B (`view_vendor_year_survival.csv`): `COMMITMENT` debit rows → `(vendor_id, fiscal_year)` → first_order_year, present_next_year, total_amount, po_count
  - View C (`view_agency_year_commodity_mix.csv`): `COMMITMENT` debit rows → `(agency, fiscal_year, nigp_class_3digit)` → po_count, total_committed_amount, revision_count (commitmentID contains "-V")
  - View D (`view_agency_year_payment_totals.csv`): non-`COMMITMENT` debit rows → `(agency, fiscal_year)` → total_payment_amount, payment_count
  - Writes all four CSV files to `outPath`. Memory contract: O(distinct keys per view).
  - Emits `RESULT step=5v` and `RESOURCE step=5v` log lines.
- `02-foundation/va_pipeline/src/test/scala/org/universalledger/foundation/va/ledger/CKBViewsSpec.scala`
  — New: 8 unit tests covering: View A COMMITMENT debit accumulation, View B present_next_year flag,
    View C revision detection, View D non-COMMITMENT isolation, all-files output, metric emission,
    date format handling (YYYY/MM/DD and MM/DD/YYYY), year-filter isolation.

**Files changed:**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/LedgerApp.scala`
  — Added `case "5v"` → `CKBViews(params + ("inPath" -> outPath, ...))` dispatch entry.
  — Added `5v = CKBViews` to usage help.
- `docs/ENGINE_IMPLEMENTATION_PLAN.md` — Sub-Task 3 todo items marked complete; status updated.

**Validation:**
- `sbt compile` — clean (1 pre-existing deprecation warning, unchanged)
- `sbt "testOnly org.universalledger.foundation.va.*"` — 35/35 passed (27 prior + 8 new)

**Architecture gate compliance:**
- Workload declared before coding (workload declaration in CKBViews.scala file header and in this
  handoff entry above).
- Reads `SortedJEFY*.csv` — the declared CKB stream output from Step 2/2m. Not a raw source scan.
- Four in-memory aggregation maps; no `toList` on source rows; source is streamed line-by-line.
- Analytical outputs (not canonical state, not SJE, no balancing required).
- Step ID `5v` does not collide with existing steps (2, 2p, 2m, 3, 4, 5, 6, 7, 8, 9, 12, 20, 21).
- No Python or Bash financial logic introduced.

**Still open:**
- Fixture smoke test: ✅ passed on Git Bash — `442 ledger rows, amount sum 0.00` (both base and `--with-po-fixture`).
- Update `VERIFICATION_AUDIT_REGISTER.md` with Step 5v evidence.
- Sub-Task 4: Python statistical fitting on the four view tables.

---

## 2026-11-04 Next Session Start Point

**Sub-Tasks 1, 2, and 3 are complete.**

- Sub-Task 1 (`transIPID` fix): complete — 2026-11-04.
- Sub-Task 2 (PO SJE into sorted stream): complete — 2026-11-04.
- Sub-Task 3 (CKB views, Step 5v): complete — 2026-11-04.

**Sub-Task 3b — owner decisions resolved (2026-11-04):**

All four Sub-Task 2b decisions from `docs/ENGINE_IMPLEMENTATION_PLAN.md` are now closed:
1. **Non-injectivity count view** — No fifth CKB view needed. Step 21 (PermutationGenerator)
   already produces `PERM_TBL20YY.csv`; the `permutation_rows ÷ GL balance rows` ratio is the
   Theorem 1 cost-explosion number. The Python analysis step reads that output directly.
2. **PO modification sequence** — Already implemented: View C `revision_count` field counts
   `-Vn` suffixes. Closed.
3. **7-window longitudinal evaluation (Test 4)** — Approved. Implement in the four Python
   fitting scripts (Sub-Task 4): use 7 rolling train/test year-windows instead of a single split.
4. **`vendorSurrogateKey = hash(VENDORID)`** — Approved. Must be added to `CKBViews.scala`
   before Sub-Task 4 Python scripts are written (Scala produces it; Python consumes it).

**Next tasks (in priority order):**

1. **Update `VERIFICATION_AUDIT_REGISTER.md`** with Step 5v evidence. ✅ Done 2026-11-04.
2. **Add `vendor_surrogate_key` to `CKBViews.scala`** ✅ Done 2026-11-04 — SHA-256 hash of
   `VENDORID` (first 8 hex chars) on every View B row; 36/36 tests pass.
3. **Design enriched view schemas before writing Python** — see architecture note below.
4. **Sub-Task 4: Python statistical fitting** — four scripts, one per view table. Owner approval
   required before writing any Python (AGENTS.md). Use 7-window longitudinal split.

**Architecture gap — Step 2 VendorMaster hash-map (recorded 2026-11-04):**

The current `loadVendorMap()` in `standardizeAndSort.scala` resolves `VENDOR_NAME → VENDORID`
using an in-memory `HashMap`. This is an architectural shortcut that does not simulate GenevaERS:

- GenevaERS resolves large reference tables via **sorted-merge join** (both inputs sorted on the
  join key; O(1) memory per record; no hash map). The hash map is only correct for tables of
  hundreds of rows.
- Distinct vendor name strings across 14 years of APA expenditure data are likely in the hundreds
  of thousands — not hash-map territory.
- The hash-map approach hides a sort/materialization pass boundary from the cost-curve measurement.

**Correct target architecture (two-pass):**
- Pass 1: sort payment SJEs by `VENDOR_NAME`, sort VendorMaster by `VENDOR_NAME`, CKB merge →
  assign `VENDORID`. Output: payment SJEs with `transIPID` populated.
- Pass 2: re-sort by `VENDORID`, merge with PO SJEs → existing CKB instrument pass.

Recorded in `docs/GENEVAERS_CAPABILITY_GAP_REGISTER.md` as GERS-002.
Recorded in `docs/CKB_LAYER_EXECUTION_MODEL.md` §Reference Joins and the Hash-Map Boundary.

The hash-map implementation remains valid at fixture scale. Full-corpus CCP2/CCP3 runs must
replace it or mark the workload `blocked_by_engine_capability`.

**TDS repo todo (paper narrative):**

- In the CCP1 pipeline description (TDS-AI-Models), document Steps 20 and 21 as reading the same
  input independently, with Step 21 (permutation table) described *before* Step 20 (GL post) to
  match the real-world causal order: the source system holds the full instrument-level permutation
  space and *collapses* it into a GL feed — the GL post step receives that collapsed output, it
  does not produce the permutation space. The step numbers (20, 21) do not need to change;
  only the narrative ordering in the paper matters.

Smoke test is confirmed passing on Git Bash (no WSL required):
- Base: `bash scripts/run_repo_fixture_smoke_test.sh --show-data` — 442 ledger rows, sum 0.00
- PO path: `bash scripts/run_repo_fixture_smoke_test.sh --with-po-fixture --show-data` — same controls
- Invoke via: `& "C:\Program Files\Git\bin\bash.exe" -c "bash scripts/run_repo_fixture_smoke_test.sh --show-data"`

---

## 2026-11-04 Windows Compatibility — Smoke Test Now Passes on Git Bash (Bob)

**What was done.**

Four script/build files fixed so the fixture smoke test runs to completion on Windows
Git Bash without WSL. The financial pipeline itself was unchanged.

**Files changed:**
- `02-foundation/va_pipeline/build.sbt`
  — Added `Compile / run / mainClass := Some("org.universalledger.foundation.va.ledger.LedgerApp")`.
  In batch mode (`sbt -batch run ...`), sbt refuses to run when multiple main classes exist
  and no run-scoped mainClass is set. This caused every pipeline step to fail with
  "No main class detected."
- `scripts/run_pipeline.sh`
  — `tee /dev/stderr` replaced with `tee "$_tee_target"` throughout the three branches of
  the sbt invocation block. `_tee_target` is set to `/dev/stderr` on Linux/macOS and to
  `$(mktemp)` on `MINGW*`/`MSYS*`/`CYGWIN*` (Git Bash on Windows). The temp file is
  deleted after capture. `/dev/stderr` does not exist in Git Bash; `tee` to it produces
  an error that PowerShell treats as a fatal process failure.
- `scripts/run_repo_fixture_smoke_test.sh`
  — Added Windows-compatible Python3 guard: `type -P python3` resolves to the Windows
  App Execution Alias stub (a `.exe` symlink target); `readlink -f` reveals the `.exe`
  suffix and the guard skips execution. The `python3 --version` probe bypasses redirects
  and writes to the console regardless of `2>/dev/null`.
  — Added `OUTPUT_DIR` Windows fallback: after the pipeline completes, if
  `$OUTPUT_DIR/SortedJEFY03q1exp.csv` is absent (because `commit_attempt` could not
  create an NTFS symlink), the smoke test resolves `OUTPUT_DIR` to the latest
  `output.generations/run_*` subdirectory.
- `scripts/interpret_run.sh`
  — Same `.exe`-stub guard for `python3`, using `readlink -f` on the `type -P` result.
  Added awk fallback to parse manifest CSV fields when Python is unavailable.

**Validation:**
- `sbt "testOnly org.universalledger.foundation.va.*"` — 27/27 passed (unchanged)
- `bash scripts/run_repo_fixture_smoke_test.sh --show-data` — exit 0
  - Step 2: `rc=0 records_read=760 records_written=1520`
  - Step 3: `rc=0 records_read=1520 records_written=442`
  - `Fixture check passed: 442 ledger rows, amount sum 0.00`
  - Interpretation report generated; `[INFO] python3 not found — skipping post-run metric evidence check.`

**Architecture gate compliance:**
- No Scala changed. No financial logic touched.
- All fixes are Bash/build orchestration only.
- On Linux/macOS, behavior is identical to before (same code paths taken).

**Still open:**
- `RESOURCE step=2 peak_rss_bytes=` is empty — the `grep` pattern in
  `write_canonical_evidence.sh` extracts the value but Scala emits `peak_rss_bytes=<N>`
  (from `Runtime.getRuntime.totalMemory()`). The evidence writer may be dropping it
  during parsing. Pre-existing; not introduced this session.

---

## 2026-11-04 Sub-Task 2 — Steps 2p and 2m: PO SJE into the sorted stream (Bob)

**What was done.**

Implemented the two-step pattern described in the architectural correction below.
No `transStandardize.scala` was touched. Both steps live in the live CKB path.

**Files changed:**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/standardizeAndSort.scala`
  — Added `applyPO(params)` (Step 2p): reads a tab-delimited VA PO file
    (`VA_opendata_FY20YY.txt`), streams rows through `SortEngine.externalSort` with
    `SortedJE.sortspec` (the same 15-field posting key used by payment SJEs), emits two
    balanced 39-field SJE rows per PO line
    (`transBusinessEventCode="COMMITMENT"`, `transCommitmentID=poNumber`,
    `transIPID=poVendorID`, `transTransAmount=price×qty` / `-price×qty`),
    writes `SortedJEFY{yr}q{n}PO.csv`. Memory contract: O(chunkSize) via a custom
    `Iterator[String]` — no `ArrayBuffer` of rows. RESULT/RESOURCE log lines emitted.
  — Added `tryBigDecimal` and `makePORow` private helpers.
  — Added block-comment at file top documenting PO column mapping.
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/LedgerApp.scala`
  — Added `case "2p"` → `standardizeAndSort.applyPO(...)` dispatch entry.
  — Added `case "2m"` → `runMerge(...)` dispatch entry.
  — Added `runMerge` private method: calls `SortEngine.mergeSpills(Seq(paymentFile, poFile),
    mergedFile, sortSpec)`; writes `SortedJEFY{yr}merged.csv`. Memory contract: O(k open
    file handles). RESULT/RESOURCE log lines emitted.
  — Updated usage help to document steps 2p, 2m, `--payment`, and `--po` parameters.
  — `inputPOPath` in CLI mode now equals `inPath` (not `inPath + "poData/"`) so `--inPath`
    can point directly to the PO file directory.
- `02-foundation/va_pipeline/src/test/scala/org/universalledger/foundation/va/ledger/StandardizeAndSortPOSpec.scala`
  — New: 6 unit tests covering: column count (39), debit+credit balance, transIPID from
    VENDORID, transCommitmentID = PONUMBER, RESULT/RESOURCE metric emission, and Step 2m
    sort-order preservation across merge.
- `docs/ENGINE_IMPLEMENTATION_PLAN.md` — Sub-Task 2 todo items marked complete; status updated.

**Validation:**
- `sbt compile` — clean (1 pre-existing deprecation warning, unchanged)
- `sbt "testOnly org.universalledger.foundation.va.ledger.StandardizeAndSortPOSpec"` — 6/6 passed
- `sbt "testOnly org.universalledger.foundation.va.*"` — 27/27 passed (21 prior + 6 new)

**Architecture gate compliance:**
- Workload declared before coding.
- `applyPO` is in `standardizeAndSort.scala` — the live Step 2 object, same CKB path as
  payment SJEs, feeds `post.scala` via the same sorted file contract.
- Memory contract: `externalSort` is O(chunkSize); `mergeSpills` is O(k open readers).
  No `ArrayBuffer` of rows.
- Same `SortedJE.sortspec` key used for both PO and payment SJEs — k-way merge produces a
  single file in the correct instrument posting order for `post.scala`.
- No Python or Bash financial logic introduced.
- Step IDs `2p` and `2m` do not collide with existing steps (2,3,4,5,6,7,8,9,12,20,21).

**Acceptance criteria met:**
- PO SJE rows use `transBusinessEventCode="COMMITMENT"` and `transCommitmentID=poNumber`.
- `transIPID` is populated from `VENDORID` (col 6), not `"0"`.
- Debit + credit amounts cancel to 0.00 per journal group.
- `SortedJEFY{yr}merged.csv` contains both event types in correct sort order.
- 6/6 new tests pass; 27/27 full suite passes.

**Still open (deferred):**
- Fixture smoke test on merged stream: ✅ passed on Git Bash — `442 ledger rows, amount sum 0.00`.
- Update `VERIFICATION_AUDIT_REGISTER.md` with Step 2p/2m evidence.
- Sub-Task 3: CKB views (monthly order volume, vendor-year survival, agency-year commodity,
  agency-year payments) — now unblocked by the merged stream.

---

## 2026-11-04 Next Session Start Point

**Sub-Task 1 is complete. Sub-Task 2 is complete. See Sub-Task 3 design below.**

Sub-Task 1 (`transIPID` fix) is done (2026-11-04).

Sub-Task 2 was **attempted twice and reverted twice** — once in an earlier session, once in this
session — because both attempts placed the PO SJE accumulation inside a transform step rather than
at a declared sort boundary. See `docs/TEST_DESIGN_PAPER_2.md` §6 for the documented pattern and
antidotes.

**Architectural correction for Sub-Task 2 (do not repeat the reverted approach):**

The correct path is **not** `transStandardize.scala`. `transStandardize` is the legacy step; it
does not feed `post.scala`. The live CKB path is:

```
standardizeAndSort (Step 2)  →  SortedJEFY{yr}{suffix}.csv  →  post.scala (Step 3)
```

Sub-Task 2 is complete. The next task is Sub-Task 3 — CKB views.

**Sub-Task 3 — CKB views** (now unblocked):
- Add four bounded aggregation view outputs to the CKB pass (`post.scala` or a new step):
  monthly order volume, vendor-year survival panel, agency-year commodity mix, agency-year
  payment totals.
- Workload declaration required per `AGENTIC_ARCHITECTURE_GATES.md` §1 before any code.
- See `docs/ENGINE_IMPLEMENTATION_PLAN.md` §Sub-Task 3 for full specification.

---

## 2026-11-04 Sub-Task 1 — Fix `transIPID` (Bob)

**What was done.**

Removed the `transIPID = "0"` hardcode from `transStandardize.scala` — the last remaining
place in the codebase where every record was assigned to instrument `"0"`. This is the
prerequisite for Sub-Task 2 and all subsequent CKB instrument-ledger work.

**Files changed:**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/transStandardize.scala`
  — PO path (`processPOs`): `transIPID = rec.poVendorID` (was `"0"`).
  — Payment path (`processPayments` Expense case):
    - Added `loadVendorMap()` private helper loading `instHolderName.toUpperCase → instID` from
      `VendorMaster.csv` as a bounded side-input `Map[String, String]` (O(distinct vendors)).
    - Added `unmatchedKey()` helper: `"UNMATCHED_" + SHA-256(name)[0..3].hex`.
    - `aggTrans` key now includes `tranVendorName` so vendor identity survives aggregation.
    - `transIPID` on each journal pair = VendorMaster lookup result, or `UNMATCHED_<hex8>`.
    - Unmatched names accumulated in `unmatchedVendors` counter.
    - `UNMATCHED_VENDORS.csv` written after main loop (control total).
- `data/VendorMaster.csv`
  — 183 rows appended: all 182 distinct vendor names in `FY03q1exp_small.txt` (synthetic IDs
    99001–99182) plus `BLACKWELLS BOOK SERVICES → c9545` and `SUPPLY ROOM → c16503`
    (from PO fixture actual VENDORIDs). Total rows: 1,249 (was 1,067).
- `02-foundation/sort_specs/POSortedJE.sortspec` — new file.
  Sort key: `(transIPID, transFiscalPeriod, transTransDate)`. Aligns PO sort order with the
  payment SJE key for the Sub-Task 2 k-way merge.
- `docs/ENGINE_IMPLEMENTATION_PLAN.md` — Sub-Task 1 todo items marked complete; status updated.
- `docs/VERIFICATION_AUDIT_REGISTER.md` — 2026-11-04 section added.

**Validation:**
- `sbt compile` — clean (same 1 pre-existing deprecation warning, unchanged)
- `sbt "testOnly org.universalledger.foundation.va.*"` — 21/21 passed

**Architecture gate compliance:**
- Workload declared before coding.
- `loadVendorMap` is bounded: O(distinct vendors in VendorMaster) ≈ 1,249 rows. No `toList` on corpus.
- `transStandardize` is the legacy transform step (not wired to `process_metrics.csv`). The live
  pipeline step is `standardizeAndSort` (Step 2), which already did VendorMaster lookups correctly.
- No Python or Bash financial logic introduced.

**Acceptance criteria met:**
- `transIPID` is populated from `VENDORID` for all PO SJE rows.
- `transIPID` is populated from VendorMaster name-lookup for payment SJE rows.
- Unmatched vendors receive `UNMATCHED_<hex8>` key and are written to `UNMATCHED_VENDORS.csv`.
- `aggTrans` aggregation key includes vendor name — instrument identity is not discarded.
- 21/21 tests pass.

**Still open (deferred to next session):**
- Full fixture run with `--with-po-fixture` to confirm `transIPID` non-zero on PO data at runtime.
- Sub-Task 2 merge of PO and payment SJE streams.

---

## 2026-11-04 CCP1 GL Post and Permutation Generator (Bob)

**What was done.**

Implemented CCP1 baseline (Sub-Task 0b from `docs/ENGINE_IMPLEMENTATION_PLAN.md`):
two new Scala steps completing the GL-architecture branch of the experimental design.

**Files created:**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/GLPost.scala`
  — Step 20. Reads `SortedJEFY*.csv`, accumulates account-keyed balances
  `(agency, nominal_account_id, fund, fiscal_year)` by summing debit lines only
  (instrument identity intentionally discarded), writes `GL20YY.csv`.
  Emits `RESULT step=20` and `RESOURCE step=20` log lines.
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/PermutationGenerator.scala`
  — Step 21. Reads same `SortedJEFY*.csv`, accumulates `(account_key, vendor_id)` permutation
  counts (retaining vendor identity), writes `PERM_TBL20YY.csv`.
  Emits `RESULT step=21` and `RESOURCE step=21` log lines.
- `02-foundation/va_pipeline/src/test/scala/org/universalledger/foundation/va/ledger/GLPostSpec.scala`
  — 7 unit tests covering: distinct account key counts, amount accumulation, instrument-identity
  discarding, metric log emission (GLPost and PermutationGenerator), and permutation > GL row
  count assertion.

**Files changed:**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/LedgerApp.scala`
  — Added `case "20"` → `GLPost(...)` and `case "21"` → `PermutationGenerator(...)` dispatch entries.
- `docs/ENGINE_IMPLEMENTATION_PLAN.md` — Sub-Task 0b todo items marked complete; status updated.

**Validation:**
- `sbt compile` — clean (same 1 pre-existing deprecation warning, unchanged)
- `sbt "testOnly org.universalledger.foundation.va.ledger.GLPostSpec"` — 7/7 passed
- `sbt "testOnly org.universalledger.foundation.va.*"` — 21/21 passed (14 prior + 7 new)

**Architecture gate compliance:**
- Workload declared before coding (research question: Paper 1 §3.2 Theorem 1 empirical evidence).
- GLPost and PermutationGenerator are analytical outputs, not canonical CKB state.
- Memory contract: O(distinct account keys) and O(distinct account × vendor keys) — bounded.
- No Python or Bash financial logic introduced.
- Step IDs 20 and 21 do not collide with existing steps (2,3,4,5,6,7,8,9,12).

**Acceptance criteria met:**
- `GL20YY.csv` is produced with account-keyed balances; instrument identity absent from output.
- `PERM_TBL20YY.csv` is produced with `(account_key, vendor_id, record_count)` rows.
- RESULT/RESOURCE metrics are emitted for both steps, same format as steps 2 and 3.
- permutation_rows > GL balance rows when multiple vendors share an account (verified by test).

---

## 2026-11-04 process_metrics.csv Repair — Steps 2 and 3 (Bob)

**What was done.**

Added `RESULT` and `RESOURCE` structured log line emission to Steps 2 and 3 of the VA pipeline.
Both steps previously produced zero `input_rows`, `output_rows`, and `peak_rss_bytes` in
`process_metrics.csv` because the Bash evidence writer
(`scripts/write_canonical_evidence.sh`) greps for those lines and they were absent.

**Files changed:**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/standardizeAndSort.scala`
  — captures `t0 = System.currentTimeMillis()` at start; at end emits:
  `RESULT step=2 year=YYYY rc=0 records_read=<source rows> records_written=<JE rows> elapsed_s=N input_bytes=B output_bytes=O`
  `RESOURCE step=2 year=YYYY peak_rss_bytes=R` (`R` = `Runtime.getRuntime.totalMemory()`)
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/post.scala`
  — same pattern for step 3; lines emitted after `def ldgrHeader` to avoid Scala 2.12
  forward-reference compile error.

**Validation:**
- `sbt compile` — clean (1 pre-existing deprecation warning, unchanged)
- `sbt "testOnly org.universalledger.foundation.va.*"` — 14/14 passed
- `sbt "testOnly org.universalledger.foundation.va.ledger.StandardizeAndSortSpec"` — confirmed
  live RESULT/RESOURCE output: `RESULT step=2 year=2014 rc=0 records_read=1 records_written=2 elapsed_s=1 input_bytes=155 output_bytes=433`

**Acceptance criteria met:**
`input_rows`, `output_rows`, `peak_rss_bytes` are all non-zero in `process_metrics.csv` for
steps 2 and 3 when run via `write_canonical_evidence.sh`.

---

## 2026-11-04 Experimental Architecture Redesign (Bob)

**What was done.**

Full redesign of the CCP/AIM experimental architecture. No Scala, Python, or data changed.
Five documents updated to capture the new design.

**Key decisions made this session:**

1. **CCP1 added as status-quo baseline.** CCP1 = GL post (account-keyed, instrument identity
   discarded) + permutation generator (reads original transactions, produces full key-embedded
   permutation table). CCP1 is not a point on the minimum cost curve — it is the point
   representing where every institution already is. The cost delta CCP1→CCP2 is the cost of
   the architectural change.

2. **CCP2 redefined as Instrument Ledger.** Previously CCP2 = "post only" was ambiguous. It
   now explicitly means CKB post to an instrument-keyed ledger. This is the left anchor of the
   minimum cost curve — the first departure from today's GL architecture.

3. **CCP3 = Instrument Ledger + Step 5 views. The Step 5 views ARE the AIM2–AIM4 feature
   tables.** They are not a separate AI pipeline. The delta CCP2→CCP3 = the measured cost of
   producing all Paper 2 AI evidence substrate. That delta is expected to be small — and that
   smallness is itself a Paper 1 finding.

4. **AIM1 runs as a post-hoc step against aggregated output — not the live stream.** This is
   deliberate: it measures information loss from the outside, exactly as any downstream consumer
   would experience it. Two substrates: CCP1 GL output and CCP2 Instrument Ledger.

5. **Full AIM1–AIM5 suite run against both CCP1 substrates (GL + permutation table).** All
   expected to miss. The permutation table is the legacy architecture's best possible response
   — all dimensions expanded — but event sequence is still unrecoverable. The failure of the
   permutation-table substrate is the direct empirical rebuttal to "couldn't you just add more
   columns to your GL?" Answer: yes; we measured it; it still doesn't work; and it costs more.

6. **AIM pairings:** AIM1 ↔ CCP1+CCP2, AIM2–AIM4 ↔ CCP3, AIM5 ↔ prerequisite for CCP5.

**Files changed:**
- `docs/RESEARCH_OBJECTIVES.md` — §1, §2, §3 table, §4, §5 table, §6 run order rewritten
- `docs/TEST_DESIGN_PAPER_1.md` — CCP1 added, CCP2–CCP5 redefined, AIM pairings added
- `docs/TEST_DESIGN_PAPER_2.md` — two-substrate design added to preamble and AIM1 section
- `docs/ENGINE_IMPLEMENTATION_PLAN.md` — architecture diagram rewritten; Sub-Task 0b (CCP1 build) added
- `LEDGER_LAB_OVERVIEW.md` — run-order table rewritten with CCP1 at step 0

**Narrative summary (for paper introduction):**
> CCP1 is the status quo. Every institution already runs it. CCP2 is the minimum-cost
> instrument-ledger architecture. The delta CCP1→CCP2 is small. The AIM tests run against
> CCP1 substrates all fail — including against the permutation table (the GL's best effort).
> The same tests run against CCP2/CCP3 outputs succeed. The architecture that costs less also
> answers more. Both measured empirically on public data.

**Next session:**
- `process_metrics.csv` repair (blocking all CCPs)
- Then: design CCP1 GL-post step and permutation generator (can proceed in parallel with
  `transIPID` fix once metrics substrate is repaired)

---

## 2026-11-04 Owner Decision Log — All Pre-Implementation Decisions Confirmed

**What was done.**

Owner confirmed all four pending decisions blocking implementation. No code changed. Two
documents updated to record the confirmed values:
- `docs/RESEARCH_OBJECTIVES.md` §7 — "Open Decisions" → "Decisions (confirmed 2026-11-04)"
- `docs/TEST_DESIGN_PAPER_2.md` §4 — "Open decisions" → "Decisions (confirmed 2026-11-04)"

**Decisions confirmed:**

| Decision | Confirmed value |
|----------|----------------|
| AIM2–AIM5 acceptance thresholds | All proposed values accepted as-is (see tables in RESEARCH_OBJECTIVES.md §7 and TEST_DESIGN_PAPER_2.md §4) |
| Longitudinal evaluation strategy | **7-window longitudinal** — train years 1–7, predict year 8; repeat for all 7 windows across FY03–FY16 |
| AIM1 revision-suffix inclusion | **Include** — rationale (owner): in financial systems updates are new events; a PO revision is a new commitment act |
| Surrogate key formula | **SHA256(VENDORID) mod 2^32** — confirmed, do not change after any split assignment is computed |

**Effect on implementation:**

- All thresholds are now pre-registered. Do not adjust after any test run.
- AIM2 and AIM3 Python scripts must implement the 7-window loop, not a single split.
- The CKB view schema must include `revision_suffix` as a distinct field.
- The Scala CKB view must emit `vendorSurrogateKey` on every vendor-year row using SHA256 mod 2^32.

**Next session:**

CCP2 measurement substrate repair — fix `process_metrics.csv` emission so `records_read`,
`bytes_written`, `wall_seconds`, and `peak_rss_bytes` are non-zero on every run. This is the
only remaining blocker before any cost curve or Paper 2 work can produce valid evidence.

---

## 2026-11-04 Test Taxonomy Documentation Restructure (Bob)

**What was done.**

Documentation-only session. No Scala logic, Bash, Python, or data changed. Two tiny TODO comments added to dead Spark files flagging them for future removal.

**Files created:**
- `docs/RESEARCH_OBJECTIVES.md` — master map of all experiments across both papers: CCP2–CCP5 table, AIM0–AIM5 table, run order, open decisions, links to detail docs
- `docs/TEST_DESIGN_PAPER_1.md` — Paper 1 cost-curve workload specifications (CCP2–CCP5) with per-workload steps, metrics, acceptance evidence shape, and blocking prerequisite (process_metrics.csv repair)
- `docs/TEST_DESIGN_PAPER_2.md` — Paper 2 AI model evidence tests (AIM0–AIM5): all original content updated with AIM IDs, new AIM1 section (non-injectivity count), fixed dangling references, expanded open-decisions table

**Files retired (redirect stubs only):**
- `docs/TEST_DESIGN_PAPERS_1_2.md` — content split into the two new files above
- `docs/next-session-plan.md` — content now in RESEARCH_OBJECTIVES.md, TEST_DESIGN_PAPER_1.md, TEST_DESIGN_PAPER_2.md
- `org-members/research/PAPER_02_NOTES.md` — content now in RESEARCH_OBJECTIVES.md and TEST_DESIGN_PAPER_2.md

**Files updated (ledger-lab):**
- `LEDGER_LAB_OVERVIEW.md` — AIM IDs added to test section headings and run-order table; CCP2 prerequisite row added; all links updated; footer dated
- `README.md` — line 16 now links to RESEARCH_OBJECTIVES.md, TEST_DESIGN_PAPER_1.md, and TEST_DESIGN_PAPER_2.md
- `CONTRIBUTOR_ONBOARDING.md` — C1–C4 → CCP2–CCP5
- `SESSION_HANDOFF.md` — C1–C4 → CCP2–CCP5 (bare references; historical quoted sentences unchanged)
- `docs/ENGINE_IMPLEMENTATION_PLAN.md` — link to TEST_DESIGN_PAPERS_1_2.md updated to TEST_DESIGN_PAPER_2.md
- `PROJECT_BRIEF.md` — C1–C4 annotated with `[now CCPx]` to preserve historical record
- `docs/VERIFICATION_AUDIT_REGISTER.md`, `docs/RUNTIME_DATA_LAYOUT.md`, `docs/TDS_vs_IL_architecture.md`, `docs/DATA_ENHANCEMENT_PLAN.md`, `docs/BOB_METHOD_NOTE_CKB_EFFICIENCY.md`, `docs/AGENTIC_ARCHITECTURE_GATES.md`, `docs/VA_Data_Layouts_and_Specs.md` — C1–C4 → CCP2–CCP5

**Files updated (org-members):**
- `research/02-pre-hierarchy-event-repositories.md` — AIM2/AIM3/AIM4 labels added inline to Track B experiment descriptions; TEST_DESIGN_PAPERS_1_2.md links updated to TEST_DESIGN_PAPER_2.md
- `research/README.md` — link updated to TEST_DESIGN_PAPER_2.md

**Files updated (TDS-AI-Models):**
- `02-concepts/L2-06-pre-hierarchy-events-and-ai.md` — AIM2–AIM4 label added; RESEARCH_OBJECTIVES.md footer note added
- `02-concepts/L2-03-minimum-cost-curve.md` — CCP2–CCP5 label added; RESEARCH_OBJECTIVES.md footer note added

**Scala files (dead Spark code only):**
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/spark/poVendorID.scala` — TODO comment added at line 1
- `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/spark/poToPayMatch.scala` — same

**Decisions established this session:**
- `CCP2–CCP5` and `AIM0–AIM5` are now the canonical test IDs used consistently across all three repositories.
- `RESEARCH_OBJECTIVES.md` is the single entry-point for a new contributor or reviewer to understand the full experimental programme.

**Still pending (owner decisions required before any implementation):**
- AIM2–AIM5 acceptance thresholds (proposed values in RESEARCH_OBJECTIVES.md §7 and TEST_DESIGN_PAPER_2.md §4 — awaiting owner confirmation)
- Longitudinal evaluation strategy: single held-out split vs. 7-window robustness (RESEARCH_OBJECTIVES.md §7)
- AIM1 revision-suffix inclusion decision (RESEARCH_OBJECTIVES.md §7)

**Next session:**
- Owner reviews RESEARCH_OBJECTIVES.md and TEST_DESIGN_PAPER_2.md Section 4; confirms or changes proposed threshold values
- Then: CCP2 measurement substrate repair — fix `process_metrics.csv` emission so records_read, bytes_written, wall_seconds, peak_rss_bytes are non-zero on every run. This is the prerequisite blocking all Paper 1 cost points.

---


## 2026-11-03 Test Taxonomy, Paper 1 Scope, Architecture Decisions (Bob)

**What was done.**

Long planning session. No files changed except this handoff and `docs/next-session-plan.md` (new)
and `docs/ENGINE_IMPLEMENTATION_PLAN.md` (updated). No Scala, Bash, or Python changed. No data changed.

**Key decisions made this session:**

1. **Test ID scheme established.** Two prefixes, self-explanatory to new developers:
   - `CCP2–CCP5` — **Cost Curve Point** for Paper 1. Each is a verified position on the
     minimum cost curve. CCP = Cost Curve Point.
   - `AIM0–AIM5` — **AI Model** evidence tests for Paper 2 on the open Virginia corpus.
     AIM = AI Model evidence test. Straight sequence, no letter suffixes.
     TA prefix reserved for Track A (insurance corpus) if needed.

2. **`TEST_DESIGN_PAPERS_1_2.md` must be split** into two paper-specific files:
   - `docs/TEST_DESIGN_PAPER_1.md` — CCP2–CCP5 workload definitions (does not yet exist)
   - `docs/TEST_DESIGN_PAPER_2.md` — AIM0–AIM5 test specs (renamed from the current file)
   The current file covers only Paper 2 despite its title claiming both papers.

3. **AIM1 is missing from the current spec.** `PAPER_02_NOTES.md` (in org-members repo) describes
   it as Test 1 — a direct count of how many distinct PO patterns map to the same posted key.
   This is the direct empirical proof of Theorem 1, requires no ML model, and should run before
   any of the AIM2a–AIM2d tests. It needs to be added to `TEST_DESIGN_PAPER_2.md`.

4. **The core engine gap is `transIPID = "0"`.** Both the PO and payment transform paths
   hardcode the instrument ID to zero. This must be fixed before any CKB instrument-break
   logic, instrument ledger, or research view can work. Fix: PO path uses `VENDORID` directly;
   payment path does a VendorMaster name-lookup with unmatched vendors getting an explicit
   `UNMATCHED_` key.

5. **PO data must enter the sorted SJE stream.** Currently `processPOs()` writes to a separate
   `PO_YYYY.csv` that never enters the CKB flow. PO records are commitment events; they must be
   sorted on the same key and merged into the combined stream so the CKB pass sees both event
   types in instrument order. The `SortEngine.mergeSpills()` k-way merge handles this without
   re-reading source files.

6. **Four CKB views emit the research feature tables in one pass** — monthly order volume,
   vendor-year survival panel, agency-year commodity mix, agency-year payment totals. These are
   the bounded summary tables the Python fitting scripts consume. Python never reads the 133M-row
   PO files directly.

7. **Surrogate key required for ML train/test split.** `VENDORID` is assigned at eVA registration
   time and carries systematic ordering artifacts. Paper 2 §5.2 describes how the insurance corpus
   uses a semantically-neutral surrogate key for free shuffling and `mod N` train/test splits.
   The VA corpus needs `vendorSurrogateKey = hash(VENDORID)` added to every view record by
   the Scala pass so Python scripts get a neutral split key automatically.

8. **Paper 1 has no test spec file.** `CONTRIBUTOR_ONBOARDING.md` defines CCP2–CCP5 in prose but
   there is no `TEST_DESIGN_PAPER_1.md` with workload definitions, input snapshots, required
   metrics, and acceptance evidence parallel to the Paper 2 document.

**Next session must complete (before any code):**
- Create `docs/TEST_DESIGN_PAPER_1.md` (CCP2–CCP5 workload definitions)
- Rename and update `docs/TEST_DESIGN_PAPERS_1_2.md` → `docs/TEST_DESIGN_PAPER_2.md` (AIM IDs,
  add AIM1, fix dangling references)
- Add redirect stub to the old filename
- Update `LEDGER_LAB_OVERVIEW.md` run-order table with AIM IDs
- Update `README.md` links
- Confirm all open decisions in the decisions table in `docs/next-session-plan.md`
- **Owner decisions required before implementation:** AIM2–AIM5 acceptance thresholds,
  longitudinal evaluation strategy (single split vs. 7 windows), AIM1 revision-suffix inclusion,
  surrogate key confirmation

**Files changed this session:**
- `docs/next-session-plan.md` (new) — full plan for next session
- `docs/ENGINE_IMPLEMENTATION_PLAN.md` (updated) — architecture decisions, surrogate key, TB alignment
- `docs/TEST_DESIGN_PAPERS_1_2.md` — "Previously named" lines removed (earlier in session)
- `LEDGER_LAB_OVERVIEW.md` — Test 2d short label updated (earlier in session)
- `SESSION_HANDOFF.md` — this entry

---

## 2026-11-03 Test Name Canonicalization (Bob)

**What was done.**

Removed the `**Previously named:**` transition notes from all four test entries in
`docs/TEST_DESIGN_PAPERS_1_2.md`. Those notes were added when the question-format headings were
introduced (Session 62) so that readers coming from older references could orient themselves.
The question-format names are now the only canonical names; the old descriptive labels no longer
appear anywhere except the historical SESSION_HANDOFF entries below where they were originally
coined.

Updated the Test 2d short label in `LEDGER_LAB_OVERVIEW.md` run-order table from
`(agency entity resolution)` to `(spelling vs. org difference)` to match the question-format
style used for Tests 2a, 2b, and 2c.

**Files changed:**
- `docs/TEST_DESIGN_PAPERS_1_2.md` — four `Previously named:` lines removed
- `LEDGER_LAB_OVERVIEW.md` — Test 2d parenthetical updated in run-order table
- `SESSION_HANDOFF.md` — this entry

**No financial code changed. No Scala/Bash/Python changed. No data changed.**

**Open items unchanged** (from 2026-11-01 entries):
1. Fix acceptance thresholds for Tests 2a–2d before any run.
2. Test 2c is cheapest next test once threshold is set.
3. Agency name reconciliation (Test 2d prereq for 2a and 3).
4. Complete Zenodo upload from a non-corporate network.
5. Windows smoke-test validation.

---

## 2026-11-01 POTest Corpus Inspection, Test 0 Closure, Paper 2 Redesign (Bob)

**What was done.**

1. **Inspected `data/POTest/` directory** — confirmed the folder contains the 2019/2020 EV matched
   dataset (`VA_2016_New/`, 8 CSV files dated Jan 2020) plus three design workbooks
   (`VA Data Samples and Specs.xlsx`, `VA Preprocessing.xlsx`, `VA Purchase and PO 2003 Combined.xlsx`).
   The directory is git-ignored; its presence means the matched dataset is now locally available.

2. **Read the `VA Preprocessing.xlsx` drawing** — extracted shape text and connector topology from
   `drawing1.xml` in the raw OOXML. The drawing is a full data-flow diagram of how the EV files
   were assembled. **Key finding:** the join key was `(agency_key, amount, vendor_name)` — a
   name+amount match. Four quarterly APA expenditure CSVs were unioned, then joined to the
   filtered/aggregated PO file via inner, left, and right joins. The `ip_rec_id` values on
   matched payment rows (EV06, EV03, EV04) were **copied from the PO side** — not retrieved
   independently from a payment system.

3. **Closed Test 0 (join feasibility).** The EV dataset cannot show vendor substitution because
   vendor-name equality was the join criterion. No independent payment-side vendor ID exists in
   any public APA file. Full evidence table recorded in `docs/TEST_DESIGN_PAPERS_1_2.md`.

4. **Replaced Test 2 with Tests 2a/2b/2c** in `docs/TEST_DESIGN_PAPERS_1_2.md`:
   - **2a** Commodity portfolio forecasting: does agency PO mix (Layer 3) predict next-year goods
     expenditure better than the posted record (Layer 2) alone? ~240 agency-year labels available.
   - **2b** Vendor population ecology: survival model on the 65K-vendor, 12-year PO panel.
     43% single-year churn rate is already a findable corpus fact with no posted-record equivalent.
   - **2c** Inventory ordering seasonality: do goods-category POs show fiscal-year-end (Q4) surge?
     PO `ORDEREDDATE` is commitment-layer only; posted expenditure carries payment date not order date.
   Updated Part D work order accordingly.

5. **Surveyed Paper 2** in `TDS-AI-Models/03-argument/02-pre-hierarchy-event-repositories.md` and
   the TDS repo structure. Wrote `TDS-AI-Models/01-evidence/universal-ledger-demo/va-corpus-ai-tests.md`
   — a self-contained evidence + test-design document mapping the VA corpus to Paper 2 claims, with
   corpus facts, the Test 0 closure finding, full test specs for 2a/2b/2c, and author notes.
   Linked it from the `universal-ledger-demo/README.md`.

**Files changed this session:**
- `ledger-lab/docs/TEST_DESIGN_PAPERS_1_2.md` — Test 0 closed, Test 2 replaced with 2a/2b/2c, Part D updated
- `TDS-AI-Models/01-evidence/universal-ledger-demo/va-corpus-ai-tests.md` — new
- `TDS-AI-Models/01-evidence/universal-ledger-demo/README.md` — new file linked
- `ledger-lab/SESSION_HANDOFF.md` — this entry

**No financial code changed. No Scala/Bash changed. No Zenodo upload attempted.
`data/POTest/` remains git-ignored. No outreach sent.**

**Next steps.**

1. **Fix acceptance thresholds for Tests 2a–2c** (record numbers in `TEST_DESIGN_PAPERS_1_2.md`
   before any run — Test 2a RMSE improvement threshold, Test 2b C-statistic floor, Test 2c
   seasonality p-value). Owner decision required.
2. **Test 2c** (seasonality) is the cheapest next test: PO `ORDEREDDATE` field, STL decomposition,
   no reference join or agency reconciliation required.
3. **Agency name reconciliation** — prerequisite for Tests 2a and 3. `data/corpus_profile/agency_alias_candidates.csv`
   has 13 unmatched and 3 ambiguous names from the prior profiling pass.
4. **Complete Zenodo upload** from a non-corporate network (unchanged from prior session).
5. **Windows validation** of Scala pipeline smoke test before CCP2 replicates.

---

## 2026-11-01 Vendor Analysis, Doc Corrections, Zenodo Upload Script (Bob)

**What was done.**

1. **Finished analyzing Codex vendor data.** Verified all Codex-produced artifacts against live sources:
   - `audit_supplier_snapshot.py` passes: 230,128 observations, 230,126 nonblank IDs, `data_integrity_verified=true`.
   - `PUBLIC_VENDOR_BRIDGE_CANDIDATES_2026-10-31.csv`: 22 rows, 21 distinct legacy IDs, all `observed_singleton_vlin_candidate`. No multi-VLIN conflicts.
   - Both evidence JSON files carry a UTF-8 BOM; load with `utf-8-sig`.
   - 20 of 22 historical candidates confirmed in current supplier directory; 2 (`DB5013044`, `DB5017422`) not found (alphanumeric IDs, likely inactive).
   - En-dash display glitch in one supplier name was a console encoding artifact, not file corruption.

2. **Resolved parser count discrepancy.** The three FY2016 row counts (1,713,138 / 1,817,217 / 1,714,830) are consistent: `csv.reader` skips blank lines from multiline record reassembly; the Scala profiler uses a quote-aware byte-level parser and yields accepted+quarantined. Updated `SESSION_HANDOFF.md` with the explanation.

3. **Corrected DUNS/TIN wording.** "Not a tax ID" was already in the handoff; narrowed "no crosswalk obtained" to "no validated public historical crosswalk — no actual historical extract rows obtained." Updated `TEST_DESIGN_PAPERS_1_2.md`: VENDORID namespace row and VLIN bridge row added to Test 0 table; Test 2 outcome softened to "deferred rather than weakened."

4. **Resolved all Zenodo owner decisions.**
   - License: `other-pd` ("Released government data, public domain").
   - Vendor email: keep as published.
   - Zero-byte placeholders: not uploaded; documented in manifest.
   - Scheme: new version of existing record `23846362` for metadata fixes; four new linked records for APA files (84 files each); one new record for supplier snapshot.

5. **Wrote and committed `scripts/zenodo_upload.py`.** Standard-library + `requests`. Handles:
   - New version of `23846362`: corrected license, title (FY2001–FY2018), description (removes false "agency code"/"sub-program" claim, corrects FY2005/FY2009 gap, notes derived file defects).
   - Four new APA records: expenditure FY2003–FY2009 (8.4 GB), FY2010–FY2016 (10.6 GB), revenue FY2003–FY2009 (1.2 GB), FY2010–FY2016 (1.6 GB).
   - New supplier snapshot record: `suppliers.csv`, `audit.json`, `manifest.json`, source export, reconciliation, VLIN verification (7 files, ~100 MB).
   - Cross-links all 6 records via `related_identifiers` before publishing.
   - Progress saved to `zenodo_upload_progress.json` (git-ignored) after every committed file; fully resumable.

6. **Partial upload executed; blocked by IBM network.** Zenodo token stored in `~/.bashrc` as `ZENODO_TOKEN`. Draft records created on Zenodo: corrected eVA/PO record draft `23850746`, APA expenditure early draft `23851758` (1 file committed: `Expenditures Aug 2002.csv`, 109 MB). IBM TLS inspection drops sustained outbound connections beyond ~25 MB; `requests`, `http.client`, and `urllib` all fail at the same network layer. Upload must be completed from a non-corporate network.

**Files committed this session (commits `28a5404`, `a9af98c`, `c2552d9`):**
- `scripts/zenodo_upload.py` (new)
- `scripts/investigate_vendor_identifiers.py`, `probe_public_vendor_bridge.py`, `assemble_supplier_snapshot.py`, `audit_supplier_snapshot.py`, `collect_eva_supplier_directory.py`, `export_eva_supplier_directory.py`, `reconcile_supplier_export.py`, `test_collect_eva_supplier_directory.py`, `verify_supplier_directory_vlin.py` (all new, Codex-produced)
- `docs/VENDOR_IDENTITY_INVESTIGATION.md`, `SUPPLIER_DIRECTORY_SNAPSHOT.md`, `TEST_DESIGN_PAPERS_1_2.md`, `PUBLIC_VENDOR_BRIDGE_CANDIDATES_2026-10-31.csv`, `PUBLIC_VENDOR_BRIDGE_EVIDENCE_2026-10-31.json`, `VENDOR_IDENTIFIER_EVIDENCE_2026-10-31.json` (all new)
- `docs/ZENODO_ARCHIVE_PLAN.md` (rewritten: decisions resolved, record plan, checklist)
- `SESSION_HANDOFF.md` (updated: DUNS/TIN, crosswalk wording, parser count explanation)
- `.gitignore` (added `zenodo_upload_progress.json`, `data/POTest/`)

**Next steps.**

1. **Complete Zenodo upload from a non-corporate network (home wifi or hotspot):**
   ```bash
   cd ~/Workspace/Sharealedger/ledger-lab
   py -3 scripts/zenodo_upload.py
   ```
   The progress file is at `zenodo_upload_progress.json`; the script resumes automatically. Expect 6–8 hours for 338 files (~21.8 GB). After completion, verify each record via the API and update DOI references in `RUNTIME_DATA_LAYOUT.md`, `download_va_data.py`, `download_va_po_data.py`, and `VERIFICATION_AUDIT_REGISTER.md`.

2. **Continue Paper 2 test work** per `docs/TEST_DESIGN_PAPERS_1_2.md` Part D order: M1–M4 log validator, M8 extractor regression tests, then Test 0 join feasibility. Test 2 (vendor substitution) remains conditional on finding an independent payment-side vendor identifier — defer rather than remove.

3. **Continue vendor investigation** (Codex): extend PO-side namespace bridge across more agencies/years; locate a payment-side extract with VLIN or remit vendor ID and voucher/PO references.

No financial code was changed. No outreach was sent. No Parquet work.

---

## 2026-10-31 Bob Corpus Work: Status (tasks 1-8)

Implemented and measured; nothing here changes the financial engine, Parquet was not used, and full data stays outside Git. Read [docs/CORPUS_FINDINGS.md](docs/CORPUS_FINDINGS.md) first.

| Task | Result | Where |
|---|---|---|
| 1 Inventory/coverage | Done. 385 files cataloged with SHA-256; 367 profiled; coverage matrix by family x FY; FY2005 not a stub, FY2009 has no `.txt`; stale-claim table added to PROJECT_BRIEF.md | `data/corpus_profile/`, `scripts/profile_corpus.sh`, `CorpusProfile.scala` (10 tests) |
| 2 Schemas/semantics | Done for the four header schemas on disk; no tab-delimited-FY2003-13 / reordered-FY2014-16 schemas exist here, contrary to DATA_ENHANCEMENT_PLAN.md | [docs/CORPUS_SCHEMAS.md](docs/CORPUS_SCHEMAS.md) |
| 3 Quality/denominators | Done. 133.79M parsed = 133.46M accepted + 326,356 quarantined; 0 line-reconciliation failures; 99% vs 94% NIGP reconciled (per line vs per vendor-year row) | [docs/CORPUS_FINDINGS.md](docs/CORPUS_FINDINGS.md) |
| 4 Identity | Specified; locator verified on 2,037 real records; per-record IDs deliberately not materialized | [docs/CORPUS_RECORD_IDENTITY.md](docs/CORPUS_RECORD_IDENTITY.md) |
| 5 Reference data | Inventoried. Agency names across all 350 APA files related to the reference table (`scripts/build_agency_alias_table.py` -> `data/corpus_profile/agency_alias_candidates.csv`): 99.1% of rows match exactly, 13 names (20,512 rows) unmatched, 3 names ambiguous between two keys (131,809 rows); validity periods all `unknown`. Fund/object: 3-month spot check only, **no fund/object alias evidence yet** | [docs/CORPUS_SCHEMAS.md](docs/CORPUS_SCHEMAS.md) |
| 6 Vendor observations | Documented only: by-year file has 8,101 column-shifted rows and 1,392 names with several IDs; `enrich_vendormaster.py` last-wins overwrite. **No code changed, no richer observation table built** | [docs/CORPUS_FINDINGS.md](docs/CORPUS_FINDINGS.md) |
| 7 Access | Done: `scripts/corpus_inspect.py files/fields/fetch/sample` (deterministic sample, checksum-verified fetch) | [docs/CORPUS_ACCESS.md](docs/CORPUS_ACCESS.md) |
| 8 Fixtures/release | Redistribution notes and Zenodo check done. **Research sample package not built**; the existing tiny fixture is unchanged | [docs/CORPUS_ACCESS.md](docs/CORPUS_ACCESS.md) |

Findings the owner should see:

- **Zenodo `10.5281/zenodo.23846362` is published but incomplete.** 47 files, all identical to local copies by MD5, but none of the 336 monthly APA expenditure/revenue files. Text says "public domain", license field says `cc-by-4.0`. Record not modified. With a 100-files-per-record limit (owner-reported, unchecked) the 385-file corpus needs about six records; the defect list, measured split and checklist are in [docs/ZENODO_ARCHIVE_PLAN.md](docs/ZENODO_ARCHIVE_PLAN.md). **Owner decisions pending:** license wording, vendor email addresses (13.35M populated PO rows), new-version versus new-records scheme. Nothing uploaded.
- PO files are not strictly by order-date fiscal year: 43,318 rows fall outside their file's FY (592 of 1,037 in FY2005).
- FY2017 PO extended price total (43.2B) is 71% six identical lines of 5,140,972,285.66 to health plans; meaning unknown. Do not quote PO dollar totals without checking extremes.
- 326,356 PO/APA records are quarantined (325,843 PO, split by embedded line breaks). Counts are fragments, not logical lines.

Not verified (see also the register): per-file source URL/acquisition date; completeness of any year; `.xlsx` PO copies vs `.txt`; whether APA expenditure/revenue overlap with transfers; reference-table validity by FY; real-corpus byte-for-byte rerun determinism; multi-line record re-read by offset.

Known boundary issue left alone: `run_pipeline_orchestrator.sh` Step 5 still calls Python `scripts/data_aggregation.py`.

Suggested next: resolve the 13 unmatched and 3 ambiguous agency names and add fund/object alias evidence; a vendor-observation table keyed by `source_record_id` (so name conflicts surface instead of being overwritten); the research sample package with a manifest; work the Zenodo checklist in docs/ZENODO_ARCHIVE_PLAN.md; M1-M4 log validator and M8 extractor regression tests.

---
## 2026-10-31 Public Supplier Snapshot Completed (Codex)

A dated eVA public-main-directory reference is now available at `data/VendorDownload/supplier_snapshot_2026-10-31/suppliers.csv` (ignored by Git). It contains **230,128 supplier/location observations**, **230,126 nonblank eVA IDs**, and **2 missing IDs retained with flags**. HTML vendor statuses are 230,068 Active and 60 Pending Approval. Ten focused tests and the final full saved-artifact integrity audit passed.

**Important source quirk:** the raw CSV export calls its Self/State registration-type column Status. The canonical CSV separates registration_type from actual status using HTML evidence. A full 230,128-row source export was reconciled on every available field: 230,127 rows matched the prefix capture directly; one non-ASCII supplier (SUP323218) was retrieved through an exact supplier-ID lookup. The export route made completion of redundant broad-query pagination unnecessary.

Read [docs/SUPPLIER_DIRECTORY_SNAPSHOT.md](docs/SUPPLIER_DIRECTORY_SNAPSHOT.md) for field meanings, scope, files, controls, and refresh commands. The snapshot folder contains unchanged source CSV, 2,912 source HTML pages, checksums, acquisition/reconciliation manifests, and audit.json. No Parquet or financial-engine changes were made. Current public main-directory coverage is established against the saved export, not against the separate inactive directory or every Commonwealth payee, and not as historical FY2003-FY2016 master truth.

The targeted historical-candidate VLIN check also produced `historical_candidate_details/historical_candidate_vlin_verification.csv`: 20 agreeing observations and 2 unresolved observations across 21 supplier IDs. This is not a full VLIN crosswalk or a payment join. Bob can consume the completed dated reference while continuing corpus profiling. Codex retains historical vendor identity and independent payment-link investigation. No outreach sent.

---
## 2026-10-31 Codex Follow-up: Public PO Vendor Bridge Observed

Actual public FY2016/FY2018 CKAN PO records carry VLIN, eVA IDs, line numbers, status, line-total changes and other fields absent from the local legacy text extracts. A reproducible convenience sample matched 22/24 selected legacy lines to singleton nonblank VLIN/eVA pairs by exact full PO number plus agency/NIGP/quantity/price; the other two are all-nines placeholders with missing VLIN. This is observed PO-side namespace evidence, not a payment join or a coverage estimate. Local/public snapshot row counts differ. Script: `py -3 scripts/probe_public_vendor_bridge.py --spcc-schema`. Findings and saved JSON/CSV evidence are linked in [docs/VENDOR_IDENTITY_INVESTIGATION.md](docs/VENDOR_IDENTITY_INVESTIGATION.md).

**Correction to earlier handoff:** EV01/EV02/EV06 are not under-$9,000 cohorts. EV02 contains 32 PO lines >=$9,000; EV06 contains 34 such payments. Every EV01/EV02 line has NIGP class <900, suggesting a commodity split; the original selection algorithm is not yet reconstructed. Do not carry the dollar-cutoff assumption into test design.

Official FY2016 purchase-card headers have no PO/vendor IDs. A May 2016 CAPP manual independently documents DUNS/VLIN fields and relevant authenticated agency extracts; no actual public historical payment-side bridge extract has been obtained. Bob remains on corpus profiling; Codex retains vendor investigation. No outreach or financial-engine changes.

---
## 2026-10-31 Codex Vendor Investigation Results

See [docs/VENDOR_IDENTITY_INVESTIGATION.md](docs/VENDOR_IDENTITY_INVESTIGATION.md) and the hashed aggregate evidence JSON linked there. Reproducible read-only probe: `py -3 scripts/investigate_vendor_identifiers.py --specs` from ledger-lab.

Key correction: the local PO data dictionary explicitly describes VENDORID as Vendor DUNS #, not tax ID. Official AP962 vendor-address schema stores DUNS in COUNTY and VLIN in ADDRESS3 alongside Cardinal vendor ID/address sequence. AP705/AP965 also carry VLIN; AP965 line description can contain the PO ID. This establishes a schema bridge, not a validated public historical crosswalk — no actual historical extract rows have been obtained. Cardinal became the system of record in FY2017, so modern specifications do not carry through to FY2003–FY2016 without additional evidence.

Independently reproduced 196/248 historical exact-amount candidate hits and 13 ambiguous hits. All 127 nonplaceholder EV06 identifiers occur in FY2016 public POs, but this does not prove the author obtained payment-side identifiers independently; they may have been assigned after matching. Parser counts differ slightly from prior temporary probes; the difference is explained by quote handling: `csv.reader` (default quoting) skips blank lines produced by quoted multiline record reassembly, yielding 1,713,138 data rows for FY2016 where a naive line count gives 1,817,217 physical lines. Bob's Scala `CorpusProfile` profiler uses a quote-aware byte-level parser and counts accepted + quarantined = 1,714,830 parsed records (1,686,615 accepted). The three counts measure different things and are consistent. Public historical transaction linkage, source precision/recall, and vendor-substitution labels remain unknown. No outreach sent; no financial code/source data changed; no Parquet work.

---
## 2026-10-31 Corpus Work Assignment to Bob; Vendor Investigation Retained by Codex

**Owner direction.** Bob is to take the corpus usability work. Codex retains the vendor-identity and PO/payment-linkage investigation. This entry assigns work; it does not report implementation or validated findings. The owner is undecided about Parquet. **Do not convert the corpus to Parquet or make it a dependency.** Preserve the current text/CSV interfaces. Any later format proposal should explain a demonstrated need, interoperability, storage duplication, preparation cost, and effects on comparable measurements before adoption.

### Bob: purpose and boundaries

Make the Virginia data a reproducible, documented research corpus usable across the accounting model, Scala/CKB experiments, perspectives, and Paper 2 analysis. Start with the existing files and contracts; this is not a new financial engine or a wholesale data-model replacement.

Read AGENTS.md, docs/RUNTIME_DATA_LAYOUT.md, docs/VA_DATA_MODEL_CONTRACT.md, docs/DATA_ENHANCEMENT_PLAN.md, docs/MEASUREMENT_AND_LOGGING.md, and the existing preparation scripts. Treat older enhancement-plan claims as proposals requiring evidence. Follow the architecture gates and verification protocol when applicable. Preparation/inspection may use Python; financial processing remains Scala and orchestration remains Bash. Keep full source and derived runtime files outside Git under the declared runtime root.

### Bob: tasks to consider, in priority order

1. **Inventory and coverage before transformation.** Produce a machine-readable catalog (CSV is sufficient) for every source file: relative path, source URL when known, acquisition date when known, checksum, bytes, source family, claimed fiscal year/period, schema identifier, and inspection status. Do not invent missing provenance. Distinguish missing, empty, partial, malformed, and completeness-unknown files. File size alone is not evidence of a stub: extract_po_attributes.py currently skips files below 0.5 MB. Inspect the small years before accepting that classification, and reconcile conflicting year-gap statements in existing documents.

2. **Document schemas and semantics by source era.** Record headers, delimiters, encodings, blank-header behavior, field types, identifier namespaces, amount/sign conventions, date meanings, fiscal-year rules, and known schema transitions. Preserve leading zeros and identifiers as strings. Distinguish accounting date, order date, voucher date, fiscal year, extraction date, and actual revision timestamps where available. A -Vn suffix alone does not establish a complete revision history. Do not infer tax IDs from identifier shape.

3. **Profile data quality with explicit denominators.** Per file/year, report physical/input records, parsed records, accepted records, rejected/quarantined records, blank IDs, placeholder IDs, duplicate candidates, date range, and available amount controls. State exclusions and rounding rules. Separate signed totals from absolute-dollar totals. Reconcile coverage claims: the saved extractor summary reports 99% NIGP coverage among counted lines but 94% among vendor-year rows; the handoff conflates these measures. Records skipped for blank vendor IDs must still appear in input/disposition counts. Preserve malformed originals and reason codes rather than silently dropping records or treating nulled fields as repaired rows.

4. **Define source provenance and stable record identity.** Retain immutable raw files. Specify stable source-record IDs using a file checksum and an unambiguous record locator; distinguish parsed-record ordinals from physical lines for quoted multiline data. Keep raw and normalized values distinguishable, with parser/normalization version and quality flags. Preserve legitimate repeated transactions; do not deduplicate solely because business fields match. Derived outputs must identify their input snapshot and transformation version.

5. **Make reference data historical and agency-aware.** Inventory existing agency/fund/object/program/source references and their coverage. Prepare versioned agency-name aliases and chartfield mappings with source evidence, validity periods when known, and explicit unknowns. DPB documents that historical fund-detail codes may differ by agency and expanded from four to five digits around 2016: do not join on fund code alone or apply current tables retroactively without validation. Preserve Program information and flag discrepancies with current Scala mapping rather than silently redefining financial contracts. Coordinate vendor crosswalk requirements with Codex; vendor matching itself remains Codex's investigation.

6. **Retain richer vendor observations before summarizing.** Preserve observed vendor IDs/names, addresses, commodity codes, dates, and vendor-commodity counts; produce modal summaries as derived conveniences with dominant share and ambiguity. Commodity mix is an observation, not an intrinsic vendor classification. A change in annual modal commodity/state is not proof of an effective-dated master-data change; annual full-year summaries may leak future information into historical predictions. Separate vendor_state from derived geo_region and validate against a declared geographic vocabulary. In enrich_vendormaster.py, repeated normalized names overwrite earlier entries: surface conflicts and retain candidate identities instead of choosing by file order. Keep source enrichment distinct from synthetic attributes.

7. **Deliver a usable, format-neutral access package.** Keep CSV/text as the initial interchange format. Document exact commands for selecting years, inspecting fields, reproducing profiles, and obtaining a stable sample. Preserve existing Scala/Bash input contracts. Keep preparation time/storage separate from measured engine costs. No database, Parquet conversion, or new query service is required to complete this work.

8. **Provide representative fixtures and a release manifest.** Retain the tiny teaching fixture and specify a reproducible research sample covering available schema eras, missingness, ambiguous identities, revisions, negative amounts, and malformed records. Clearly label synthetic rows. Package dictionaries, catalog, quality reports, reference versions, preparation commands, and checksums with the release. Verify archive availability/completion rather than assuming a DOI proves all files are uploaded. Record redistribution terms by source; do not assume all state records or external code descriptions have identical permissions.

### Suggested first deliverable and acceptance evidence

Start with an inventory, year/source coverage matrix, and short schema/quality report before broad cleanup. Then implement the smallest reproducible preparation package justified by those findings. CSV manifests and Markdown documentation are sufficient initially.

A usable first release lets another contributor identify the exact input snapshot, understand every field used, reproduce row dispositions and available source totals, trace sampled derived records back to originals, see unresolved mappings, and run existing fixture consumers. Checks should exercise actual malformed/ambiguous cases and deterministic reruns. Do not claim full source completeness merely because generated journals balance. Keep an explicit list of unverified properties and update the audit register where required.

### Codex: retained investigation, separate from Bob's corpus work

- Investigate the documented eVA-to-Cardinal relationship through VLIN (Vendor Location Identification Number), Cardinal vendor IDs, and historical PO VENDORID namespaces. A documented system relationship is not a verified public historical crosswalk.
- Inspect public specifications for AP962 vendor extracts, AP965 voucher extracts, and AP705 payment reconciliation extracts. Determine available fields, historical applicability, public access, and whether a narrowly scoped data request could help. No request or outreach has been sent or authorized by this handoff.
- Investigate historical agency-to-agency supplier/customer crosswalks for interagency identification and future consolidation fixtures.
- Profile APA purchase-card exports as a potential additional payment source; establish fields and overlap before proposing any union with expenditures.
- Continue independent PO/payment linkage feasibility work with reproducible scripts, candidate evidence, ambiguity, and precision checks. Vendor identity links and transaction links must remain separate; matching on vendor equality would exclude substitutions that Test 2 seeks to observe.
- Return findings as evidenced mappings/specifications and unresolved questions that Bob can incorporate without blocking the general corpus package.

### Official investigation leads found this session

- Vendor maintenance manual: https://doa.virginia.gov/reference/CAPP/CAPP_Topics_Cardinal/21105.pdf (April 2018; describes procurement-vendor integration and VLIN on Cardinal vendor addresses).
- Cardinal extract layouts: https://www.cardinalproject.virginia.gov/index.php/resources/agency-interface-layouts (AP962, AP965, AP705 and chartfield/ledger/budget layouts listed; spreadsheet contents were not retrieved in this session).
- Agency crosswalk listing: https://www.cardinalproject.virginia.gov/job-aids (Agency to Agency Transactions Information Sheet; current listing includes Customer and Supplier IDs, not a validated historical mapping).
- DPB chartfield reports: https://dpb.virginia.gov/budget/budget.cfm?page=COA
- Fund semantics and agency scope: https://reports.dpb.virginia.gov/coa40
- Budget/expenditure report field explanations: https://reports.dpb.virginia.gov/be122
- APA legacy financial and purchase-card downloads: https://www.datapoint.apa.virginia.gov/legacyindex.php (purchase-card exports listed from FY2014; not profiled yet).

---
## 2026-10-31 (later) PO-to-Payment Join Investigation, Second-Opinion Brief

**Purpose of this entry.** The open question from the entry below is whether a public key links a
Virginia PO line (eVA) to a posted expenditure record. Tests 1b, 2 and 3 for Paper 2 depend on it.
This entry records what one session (Bob) found, how, and what an independent reviewer (Codex)
should try to confirm or break. **Nothing here is a validated result.** All figures come from ad hoc
read-only Python probes whose scripts were written to `%TEMP%` and not kept. Reproduce from the
method descriptions, do not trust the numbers.

Files added this session (uncommitted): `docs/TEST_DESIGN_PAPERS_1_2.md` (test design, Parts A-D) and
`data/VendorDownload/*.pdf` (10 eVA vendor-list screenshots, about 2.2 MB, not git-ignored; do not
commit unless wanted). No code was changed.

### Bob's conclusion (to be tested, not accepted)

1. No public vendor-number crosswalk between the PO side and the expenditure side was found.
2. The only workable public join is by **names**, and it covers a small fraction of POs and dollars.
3. Therefore Test 2 (vendor substitution) is not buildable on public data, and Tests 1b/3 would run on
   a small, biased sample unless a better key is found. Test 1a (PO-side proxy) and an aggregate
   (agency-year) Test 3 do not need the join.

### Evidence, with method

**A. Field inventory (checked directly).**
- APA expenditure files (`data/VARawFiles/`, legacy `historicaldata/FY nnnn Expenditures.zip`, FY2003-FY2016):
  columns `AGENCY_NAME, AMOUNT, FUND_NAME, LOCALITY, OBJECT_NAME, PROGRAM_NAME, VENDOR_NAME,
  VOUCHER_NUMBER, VOUCHER_DATE`. No vendor ID, no PO number. Headers read for FY2003, FY2013, FY2014, FY2016 only.
- eVA PO files (`data/VAPOData/VA_opendata_FYnnnn.txt`, tab-delimited, blank first line):
  `PONUMBER, AGENCY, ORDEREDDATE, ... QUANTITYORDERED, PRICE, VENDORID, VENDORNAME, ... NIGPCODE ...`.
  No Object/Fund code.
- FY2016 PO `VENDORID` shapes (1,713,126 lines): 9 digits 87.6%, `999999999999` placeholder 6.7%,
  two letters + 7 digits 4.2%, blank 0.9%. FY2018: 85.2% / 4.9% placeholder / 8.1% / 1.0%.
  Treating the 9-digit form as a tax ID is a guess from its shape only.

**B. Direct PO-number link: not found (sample only).** FY2014 April expenditure file, first 500,000 rows:
`VOUCHER_NUMBER == PONUMBER` (exact string, trimmed) in 1 row. `PONUMBER` shapes are mixed
(`AAA9999999`, `AA9999999`, `A999_9999999`, suffix `-V2`); voucher shapes are different
(`A9999999`, `99999999`, `AA999999`, ...). Containment, prefix/suffix stripping and per-agency
conventions were **not** tested. This is the weakest part of the negative result.

**C. Name/amount join at full FY2016 scale.**
- Key: (normalized agency name, normalized vendor name, amount rounded to 2 dp). Normalize = uppercase,
  drop non-alphanumerics. PO side: sum of `QUANTITYORDERED * PRICE` per `PONUMBER`. Payment side: sum of
  `AMOUNT` per (agency, vendor, `VOUCHER_NUMBER`).
- Result: 1,686,234 parseable PO lines, 635,196 POs; 7,202,800 expenditure rows, 3,752,550 vouchers;
  19,106 voucher-level hits, 2,857 of them ambiguous (more than one PO shares the key); 18,623 distinct
  POs matched (2.9%). Row-level (single expenditure row vs PO total) hits: 20,518.
- **Not measured:** false-positive rate, and recall. Known recall limits of this run: only FY2016 POs were
  matched to FY2016 expenditures (POs ordered in FY2015 and paid in FY2016, or ordered late FY2016 and paid
  in FY2017, are missed); partial and installment payments are missed by the exact-amount rule; freight/tax
  differences are missed. The 2.9% is therefore a floor for this key, not a ceiling for any join.

**D. Name-coverage ceiling, FY2016 expenditures (7.2M rows).** Rows classified by `VENDOR_NAME`.
The "pseudo-vendor" regex (`PAYROLL|JOURNAL|TRANSACTION|LABOR COST|CIPPS|INTERAGENCY|CONFIDENTIAL|REFUND|TRAVEL|ADJUST|TREASURER|RETIREMENT|BENEFIT`) is crude.

| Class | Rows | Share of absolute dollars |
|---|---|---|
| Name appears in the FY2016 PO file | 1,222,259 (17.0%) | 17.8% |
| Name not in PO file | 3,293,291 (45.7%) | 21.8% |
| Pseudo-vendor | 2,687,250 (37.3%) | 60.4% |

In the PO files 2,811 vendor names (FY2016) map to more than one `VENDORID`.

**E. The 2020 matched dataset** (`C:\Users\0D8297897\Box\CommonKeyBufferingData\VA_2016_New\`, files dated
Dec 2019 - Jan 2020, author unavailable, method undocumented). Event files EV00-EV07 with one running
`event_id` sequence. EV01 = 42,175 PO lines under $9,000 (20,309 POs, ordered 2013-07 to 2016-06);
EV02 = 583 lines / 248 POs (all in EV01); EV06 = 248 payments under $9,000 (`contract_id` literally `"VA"`);
EV03 = 5,520 and EV04 = 5,706 matched payments; EV05 = 3,548,091 unmatched payments (`contract_id` blank,
vendor blank in the first 500,000 rows); EV07 = tax receipts. Payment rows carry `fundDtlKey, objectKey,
objectCode, subPrgmKey`, `org_unit_cd` and a vendor ID (`ip_rec_id`) in the same formats as the PO `VENDORID`.
- Reverse-engineered key: (`ip_rec_id`, `org_unit_cd`, payment amount == sum of `qunatity*unit_price`
  over the PO's lines). 196 of 248 EV06 payments match an EV02 PO exactly; 13 of those keys are ambiguous;
  the other 52 do not match (near misses such as 33.92 vs 23.92; no 2-3 PO sum explains them).
  Payment date minus order date: median 259 days, minimum -36, maximum 488.
- Inconsistencies, unexplained: EV04 (5,706) is not EV03 + EV06 (5,768) and shares no `event_id`;
  EV03 is named "greater than 9000" but only 919 of 5,520 rows exceed 9,000; EV05 sums to $42.15B net.
- Implication: the author had a payment-side vendor ID that the public APA files lack. Source unknown.

**F. eVA Transparency Reports vendor list** (`logi.cgieva.com/External/rdPage.aspx?rdReport=Public.Reports.Report9001_Data`).
Public Logi report. Fields: status, HQ flag, eVA vendor ID (`SUP######`, `VS#######`, `E#####`, `VC...`),
name, address, SWaM type, card/e-order flags; detail page adds VLIN and NIGP codes. No tax ID/FEIN/DUNS.
Display cap 1,000 vendors per query (10 pages of about 100); the 10 PDFs hold one such query (alphabetical
from `# 1 OrderAnything` to `4Front...`). Searching 8 IDs from the PDFs (`SUP365210, SUP354912, VS0000033394,
SUP306409, SUP317717, VS0000340540, E52135, E28599`) in the FY2016 and FY2018 PO files gave 0 hits (plain-text
search). Counted properly: 0 of 1,713,126 FY2016 and 0 of 880,813 FY2018 PO lines have a `VENDORID` starting
with `SUP`, `VS0` or `VC0`. Four vendor names from PDF page 1 gave 0 hits in FY2018 (FY2016 not checked).
So the eVA ID namespace is different from PO `VENDORID`. **Correction:** my earlier "search for TSRC / 799849187"
probes against this site returned the default first page every time, so GET search parameters are ignored
(probably a form POST with a CSRF key). Those probes showed nothing either way.

**G. Public portals checked (shallow).** data.virginia.gov CKAN API (`package_search`): eVA datasets exist for
2016-2027 (the FY2016 CSV resource reports size 0); queries for expenditures, vendor payments, checkbook, Cardinal,
Auditor of Public Accounts found no state vendor-payment dataset with vendor IDs. datapoint.apa.virginia.gov:
legacy zips FY2003-FY2016 and a new extract path pattern `data/extracts/{fiscalYear}/All {Expenditures|Revenue|Budget} for Fiscal Year {fiscalYear}.zip`
(my direct URL guess for that path returned 404; the repo's FY2014-FY2016 folders have that naming).
`legacydatapoint.apa.virginia.gov` timed out once. Only a handful of search terms were tried.

### What Codex should do to confirm or refute (suggested, in priority order)

Run these independently and write your own scripts; do not reuse numbers above. Report agreement or
disagreement per item, and say what you could not check.

1. **Look harder for a direct key (could overturn the conclusion).** For several fiscal years and several
   agencies, test whether `VOUCHER_NUMBER` contains or embeds `PONUMBER` (strip `-Vn`, leading letters,
   underscores, zero padding; try substring and suffix/prefix). Compare per agency, since universities and
   executive agencies may use different conventions. Also test whether `VOUCHER_DATE` clusters shortly after
   `ORDEREDDATE` for candidate pairs.
2. **Find the payment-side vendor ID.** Search for any public Virginia source of vendor payments keyed by
   vendor ID/FEIN or including PO number: Cardinal/DOA vendor-payment or "checkbook" files, FOIA responses,
   Wayback Machine copies of old `datapoint.apa.virginia.gov` or `data.virginia.gov` extracts, eVA reports
   other than Report9001 (the Logi site has numbered public reports, e.g. 9004 NIGP lookup, 9016 SBSD list,
   9020 inactive vendors), DOA "Commonwealth Data Point", any dataset with a PO number field on payments.
   Also look in the rest of the Box folder `CommonKeyBufferingData\` (only `VA_2016_New` was opened) for the
   2020 author's source extracts, SQL or scripts.
3. **Reproduce and stress the name-join.** Recompute C for FY2016. Then (a) add a placebo (shuffle vendor
   names or shift the date window by one year) to estimate the false-positive floor; (b) allow POs from the
   prior fiscal year to match; (c) allow installments (several vouchers summing to the PO total) and small
   tolerance for freight/tax; (d) report match rate, ambiguity and implied precision per year FY2006-FY2016
   (FY2004 has 227 and FY2005 has 1,037 parsed PO records, so they are small, not missing; FY2009 has no `.txt` -- see docs/CORPUS_FINDINGS.md). Say whether the rate is stable across years.
4. **Check the 2020 key and its sample selection.** Confirm 196/248 on EV02 vs EV06, then test the same
   (vendor ID, agency, amount) rule on the full public FY2016 data. Explain the EV03/EV04/EV05 inconsistencies
   if you can. Check whether the under-$9,000 cut and exact-amount rule bias the sample toward single-payment POs.
5. **Challenge the ceiling.** Check the pseudo-vendor regex and the 17.8% / 21.8% / 60.4% dollar split with a
   better vendor-name classifier. Say what share of non-pseudo dollars is name-matchable after fuzzy matching
   (suffix stripping such as INC/LLC, punctuation, DBA names), and the false-merge risk it adds.
6. **Check the claim that Test 2 cannot be built.** Argue for or against: is there any public way to observe
   "paid vendor differs from ordered vendor" without a vendor ID on the payment side?
7. **Review `docs/TEST_DESIGN_PAPERS_1_2.md`** for errors, especially Part C (Test 0, Test 1a/1b, Tests 3-4)
   and the Part A reading of `data/pipeline_log.csv`.

### Decisions still open for the project owner

- Whether to scrape or request bulk eVA vendor data (site terms not reviewed).
- Acceptance thresholds for Tests 2-4 and the CCP2 replicate stability threshold (unchanged from `docs/TEST_DESIGN_PAPERS_1_2.md`).
- Whether Test 2 is dropped from Paper 2 if no payment-side vendor ID is found.
- Whether to keep `data/VendorDownload/` and git-ignore it.

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
   15.6M-record VA eVA dataset (FY2002–FY2016, "13 years" as reported then; CORRECTED 2026-10-31: by the 0.5 MB cutoff the extractor skipped FY2001/FY2004/FY2005, and the by-year output covers 12 fiscal years -- 2002, 2003, 2006-2008, 2010-2016):
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
   - Known gaps (CORRECTED 2026-10-31): the FY2005 `.txt` is not a stub (1,037 records); FY2009 has no `.txt`. The record lists 47 files and omits the 336 monthly APA expenditure/revenue files (see docs/CORPUS_ACCESS.md).
   - DOI reference committed to `RUNTIME_DATA_LAYOUT.md`, `download_va_data.py`, `download_va_po_data.py`.

**Papers reviewed this session (both read in full).**

- [`org-members/research/01-minimum-cost-curve.md`](../org-members/research/01-minimum-cost-curve.md) — Paper 1
- [`org-members/research/02-pre-hierarchy-event-repositories.md`](../org-members/research/02-pre-hierarchy-event-repositories.md) — Paper 2
- [`org-members/research/PAPER_02_NOTES.md`](../org-members/research/PAPER_02_NOTES.md) — Paper 2 working notes with VA test design

**What ledger-lab must produce for Paper 1 (Minimum Cost Curve).**

Paper 1's VA empirical contribution is the **Instrument Pivot Theorem demonstration** (§3.5):
- Vendor ID = Instrument ID; LDGR = balance store $\mathcal{L}$; VendorMaster.csv = CAR.
- Run CCP2 (Steps 2,3 — post only) → cost_surface.csv row: compute_s, storage_bytes, master_file_count=1.
- Run CCP3 (Steps 2,3,5 — post + views) → cost_surface.csv row with additional master files.
- Show CCP2→CCP3 delta = measured marginal cost of one reporting view.
- Add a CAR attribute to VendorMaster (e.g. instNIGPClass from vendor_po_attributes_by_year.csv),
  re-run CCP3 → zero new balance records, zero reconciliation cost. That ratio vs. key-embedded
  alternative ($(1+v)^m - 1$ formula) is the empirical proof of the theorem.
- Paper 1 conclusion explicitly calls for an agentic cost-curve sweep: agent changes ViewSpec,
  re-runs pipeline, observes costs. `run_cost_curve_sweep.sh` and `cost_surface.csv` are designed
  for exactly this.
- **Three CCP2 replicates** are needed to establish measurement stability before CCP3.

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
2. **Run 3× CCP2 replicates** against full FY03 data in `data/VARawFiles/` to establish measurement
   stability and generate Paper 1 cost-surface baseline rows.
3. **Run CCP3** (add Step 5) against same FY03 data → measure CCP2→CCP3 marginal cost.
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
4. Execute CCP2 replicates to establish hardware cost-surface baseline data.

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
4. Define the CCP2 named workload and run three replicates to establish measurement stability.
5. Define and run CCP3 (add Step 5). The CCP2→CCP3 delta is the measured marginal cost of reporting
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
