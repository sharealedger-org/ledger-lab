# Project Brief: Ledger Lab — Universal Ledger POC

> **Repo:** `ledger-lab` (derived from the public `KipTwitchell/universal_ledger` POC) — **Apache 2.0**
> **Author:** Kip M. Twitchell
> **Document status:** Historical Universal Ledger POC brief. Its nine-FSP implementation claims, C1–C4 experiment taxonomy, and process map are not the current Ledger Lab architecture or implementation status. For the current design, read [README.md](README.md), the 01–05 layer READMEs, [99-interpretation](99-interpretation/README.md), and [MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md).
> **Monograph:** *"The Architecture of Economic State"* (Twitchell) — link will be added upon publication.

---

## ⚠ TECHNOLOGY INVARIANT — NON-NEGOTIABLE

**This project proves a theorem about computational architecture. The proof is only valid if the pipeline is built with the correct architecture.**

| Layer | Technology | Rationale |
|---|---|---|
| **All mainline financial flow** | **Scala** | Steps 2 (standardize+sort), 3 (post), 5 (aggregation), 6 (allocation), 7 (consolidation), 8 (forecasting), 9 (reclass). This IS the GenevaERS engine. Measuring it requires running it. |
| **Pipeline orchestration** | **Bash** | JCL analog. `set -e`, sequential step invocations, exit codes. No runtime, no subprocess management. |
| **Logging / metrics append only** | **Python** | Appending rows to `pipeline_log.csv`, `cost_surface.csv`, `pivot_results.csv`. Never in the mainline data flow. |

**The cost surface (Experiment 1) measures the Scala CKB engine.** If any mainline step runs in Python, the timing includes Python interpreter overhead. C1 through C4 are no longer measuring the same substrate. The curves cannot be compared. The theorem cannot be proven.

**If you are about to write Python for mainline flow — stop. You are invalidating the proof.**

No exceptions without Kip's explicit approval recorded in `BOB_WORK_LOG.md`.

---

## What This Repo Is, and Why It Exists

This is not a teaching tool. It is not a standalone demo. It is the **empirical laboratory for the nine-paper monograph** *"The Architecture of Economic State"* (Twitchell).

The monograph makes two large theoretical claims:

1. **The Minimum Cost Curve** (Paper 1): There is a mathematically defined interior minimum to the total cost of maintaining enterprise financial data. Virtually all enterprises have drifted far past that minimum by over-materializing balance structures. The cost of that drift compounds in two directions: direct storage and reconciliation overhead, and structural inability to answer queries the posting key doesn't support.

2. **The Instrument Pivot Theorem** (Papers 1 §3.4 and 5 §§3.3, 5.0a — formalized Session 39): A balance store keyed on Instrument ID, joined to a Contract Attributes Record (CAR), supports every possible reporting cut across all m classifying attributes at **zero marginal storage cost per new reporting dimension**. This is the symmetric positive form of the permutation explosion theorem. The legacy architecture is permanently blind to questions not anticipated at posting time. The Instrument Ledger architecture is **omnidirectionally queryable** — any dimension attachable to an Instrument ID in the CAR is a free pivot on all audited data back to inception.

This POC is the only working, public, fully executable system that can demonstrate both claims empirically. The Virginia government vendor data (FY2003–FY2016) is public domain — any result is publishable without anonymization.

---

## The Financial Process Map — Architecture Context

The pipeline implements a subset of the full CKB (Common Key Buffering) process map, which organizes financial processes into passes over three input partitions:

| Partition | Content | VA pipeline equivalent |
|---|---|---|
| **A** — Transaction History | Inbound SJEs from source systems | `SortedJE{year}.csv` |
| **B** — Balance History | Accumulated balances from prior runs | `LDGR{year}.csv` |
| **C** — Attribute History (SAL) | Date-effective instrument attributes | `VendorMaster.csv` |

Each CKB pass reads some combination of A, B, C and writes outputs that become inputs to the next pass. The key architectural insight: **partition A is optional for any given pass**. Revaluation-class processes (interest, multi-currency, forecasting, reclass) read only B and C and *generate* new transactions — no inbound feed required. This is why the transform step before the CKB is "optional" — not because data might already be formatted, but because some passes have no A input at all.

### The single-pass universality claim — and allocation

Every FSP runs in a **single pass** through the CKB. Allocation appears to be an exception because it requires a divisor total (e.g. total headcount) before it can compute any individual receiver's share — seemingly requiring a pre-scan pass.

**Resolution (Session 9):** The divisor is a *balance*. It was accumulated in the prior day's run and already exists in partition B. Using yesterday's divisor to allocate today's transactions introduces immateriality of one day's change — which is acceptable because allocation rules are management approximations, not statements of physical law. This collapses allocation back to a single pass: `B (divisor balance) × A (transactions) × C (allocation rules) → N (allocated transactions)`. Single-pass universality holds for all nine FSPs.

This is the architectural fact the cost curve experiment must demonstrate empirically.

---

## The Two Experiments This Repo Must Run

### Experiment 1 — The Four-Curve Cost Surface

**Question:** How does total pipeline cost change as process configurations are added, and does the single-pass universality claim hold empirically across all configurations?

**The four configurations:**

| Curve | Steps active | Process layers | What it demonstrates |
|---|---|---|---|
| **C1 — Post only** | 2, 3 | Transform → Post | Baseline: pure transaction → balance accumulation. Leftmost, cheapest. |
| **C2 — Post + Analysis** | 2, 3, 5 | + Analytical pivot | Marginal cost of reporting materialization (ViewSpec views). |
| **C3 — Post + Analysis + Reval-class** | 2, 3, 5, 8, 9 | + Forecasting + Reclass | Cost of generated-transaction processes. Hypothesis: flat vs. C2 — no new master files. |
| **C4 — Full pipeline** | 2, 3, 5, 6, 7, 8, 9, 4 | + Allocation + Consolidation | With prior-day divisor design, C4 cost point should equal C3. That equality IS the proof of single-pass universality. |

**The key result:** If C3 and C4 overlap on the declared workload and materialization profile, that
supports the prior-day divisor design for that workload. It does not prove universal equality. If
they diverge, the evidence must identify whether the cause is a required sort, materialized side
input, generated SJE volume, report-time derivation, or reconciliation obligation. That divergence
is itself a publishable finding.

### The Materialization Frontier

The cost experiment is not simply a staircase of adding financial functions. It compares where a
design stores state and where it recomputes derived results:

```text
immutable events + rules/reference history
  -> report-time replay and derivation

immutable events + selected ledger/side-input state
  -> fast common perspectives

immutable events + broad precomputed balances/views
  -> low query cost, high storage/update/reconciliation cost
```

The measured frontier must separate:

- source transformation and posting compute;
- report-time replay and aggregation compute;
- retained ledger, CAR, divisor, driver, and rate storage;
- generated SJE and other materialized-row volume;
- external sort, spill, and side-input join cost;
- reconciliation, approval, and master-file obligations;
- update and report latency.

Currency conversion and elimination are examples of conditional materialization. If they are
presentation-only, retain the source facts, effective rates/rules, and candidate evidence and derive
the result at report time. If they affect canonical state, an approved close, a future partition,
or a downstream engine, materialize a balanced SJE and measure that cost separately.

**Measurement per run:**
- `C_compute` — elapsed time per step (`pipeline_results.log`)
- `C_storage` — bytes of each intermediate and final file written (`pipeline_results.log`)
- `C_reconcile` — count of enabled=Y rows in `ViewSpec.csv` (master files that must stay synchronized; the AHI metric from Paper 1 §5.2)
- `total_cost_proxy` — weighted sum of above, accumulated in `cost_surface.csv`

**Within each curve, vary the materialization axis:**

| Axis | Left extreme | Right extreme | Minimum hypothesis |
|---|---|---|---|
| Transaction materialization | Recompute SJEs from raw each run | Pre-materialize SJEs permanently | Keep SortedJE; discard raw each run |
| Balance granularity | No LDGR; sum SJEs at query time | LDGR + all ViewSpec views | LDGR only; derive aggregates on demand |
| Intermediate stores | Write every step to disk | Write nothing (stream-only) | Write only LDGR and SJE |
| Engine | Pure Scala sorted-file (Step 3) | Spark shuffle (Step 11) | Scala; Spark is the engine-dependency baseline |
| Memory scaling contract | In-memory heap/hash table buffering ($O(N)$ RAM) | Scale-invariant streaming external merge-sort ($O(1)$ RAM) | Strict $O(1)$ streaming; linear wall-clock scaling across $10\times$–$100\times$ volume |

Each configuration = one point. The four curves show how the minimum point shifts as process layers are added.

**Engine-dependency test (Paper 1 §3.2a):** Run C1 and C4 with both `post.scala` (pure Scala) and `DPBSpark.scala` (Spark). The minimum point should shift leftward on the Scala engine. This is a direct empirical test of the engine-dependency claim.

---

### Experiment 2 — The Output-Side Pivot Theorem (SAL Attribute Expansion)

**Question:** As SAL attributes are added to VendorMaster one at a time, how many new audited reporting views become answerable, and what would the equivalent cost be in a key-embedded balance architecture?

This is the experiment that demonstrates the **Instrument Pivot Theorem** — formalized in Papers 1 §3.4 and 5 §§3.3, 5.0a of the monograph. The POC provides the empirical proof on public data.

**The VA pipeline's mapping to the theorem:**

| Theorem construct | VA pipeline equivalent |
|---|---|
| Instrument ID | Vendor ID (ldgrIPID) |
| Instrument Ledger (L) | LDGR{year}.csv |
| Contract Attributes Record (CAR) | VendorMaster.csv |
| CAR attribute (m=1, current) | EXP/REV nominal account prefix (instTypeID) |
| Step-up join | dataAggregation.scala — ViewSpec-driven pass over LDGR + VendorMaster |

**The experiment:**

Starting from the current VendorMaster (1 SAL attribute: vendor type → EXP/REV), add attributes one at a time by uncommenting rounds in `ViewSpec.csv`:

| Round | New SAL attribute | Source | New reporting views unlocked | Equivalent key-embedded cost |
|---|---|---|---|---|
| 0 (baseline) | instTypeID (EXP/REV) | Real — existing field | Expense vs. revenue split | 1 extra balance dimension |
| 1 | instNIGPClass | Real — derivable from PO file NIGP codes | Spend by commodity class × all existing dims | v^2 permutations if key-embedded |
| 2 | instSizeTier | Enriched — SBA size classification or spend-derived | Spend by vendor size × class × type | v^3 permutations |
| 3 | instGeoRegion | Real — from vendor address (instVendorState) | Regional breakdowns × all above | v^4 permutations |
| 4 | instContractType | Enriched — PO type from PO file | Procurement channel analysis | v^5 permutations |

**Note on data enrichment:** Round 1 (NIGP class) and Round 3 (geo region) are derivable from real data already in the pipeline. Rounds 2 and 4 require enrichment from external sources (SBA database, USASpending.gov) or synthetic assignment. The theorem holds regardless of attribute source — but real attributes are preferable for publishability. See data strategy section below.

**Measurement per round — `pivot_results.csv` schema (updated Session 19):**

| Column | Value | Role |
|---|---|---|
| `run_timestamp` | ISO timestamp | Run identity |
| `year` | Fiscal year or `cumulative` | Scope |
| `scope` | `FY{year}` or `FY2003-FY2016-cumulative` | Explicit scope label |
| `view_name` | ViewSpec view name | View identity |
| `sal_attr_count` | Number of SAL attributes active | Round indicator |
| `view_rows` | Rows in this view's output | Materialization cost |
| `ldgr_rows` | Actual LDGR balance record count | **Curve A — the flat SAL line** |
| `instrument_count` | Distinct Instrument IDs in view | Reference count |
| `key_embedded_equivalent_balances` | `Σ C(m,k)·v^k` | **Curve B — key-embedded outer bound** |
| `ratio` | `key_embedded / instrument_count` | Legacy ratio (kept for continuity) |
| `rebuild_cost_ratio` | `key_embedded / ldgr_rows` | **Forced-rebuild amplification factor** |
| `recon_obligations_sal` | Always 1 | SAL reconciliation obligation |
| `recon_obligations_keyembedded` | = `key_embedded_equivalent_balances` | Key-embedded reconciliation obligation |
| `description` | Human-readable view label | Documentation |

**The forced-rebuild argument:** Take the LDGR balance rows at conclusion (`ldgr_rows`). A single update batch touching every instrument forces: SAL architecture → one pass O(M); key-embedded architecture → rebuild of every `key_embedded_equivalent_balances` record. The `rebuild_cost_ratio` is the amplification factor on every real update. At Round 2 (m=3), that ratio is ~317 on FY2003 data. The 109M absolute permutation count is the theoretical outer bound — acknowledged in the paper as such; the `rebuild_cost_ratio` is the operational proof.

**Scope boundary (static vs. temporal pivot):** The current demo demonstrates the **static pivot** — at a fixed point in time, m SAL attributes support O(v^m) reporting cuts at zero marginal balance cost. The **temporal pivot** (as-of-date join using `instEffectDate`/`instEffectEndDate`) is schema-ready and source-data-ready (pending SAL update cycle implementation). `arrangementReclass.scala` already demonstrates the mechanism for the `instTypeID` attribute. The temporal layer is implemented as part of the SAL update cycle (Phase 0, next session).

**The key result:** After m=4 attributes, the SAL contains ~50,000 vendor rows × 5 columns. The equivalent key-embedded balance store, to answer all the same views on 12M balance rows across 13 years, would require orders of magnitude more records. The `rebuild_cost_ratio` grows exponentially with each round and IS the theorem made measurable without building a single key-embedded record.

---

## The Agentic Loop

Both experiments can be driven by an agentic AI system. The agent:

1. **Selects a configuration** — chooses curve (C1–C4), materialization level, SAL attribute round, or engine
2. **Writes the config** — edits `ViewSpec.csv` (enabled flags, attribute columns) and `run_pipeline.sh` arguments
3. **Runs the pipeline** — calls `run_pipeline.sh` with the chosen `--steps` sequence
4. **Reads the results** — parses `pipeline_results.log` for compute time, storage bytes, master file count; reads `pivot_results.csv` for view counts and permutation ratios
5. **Records the point** — one row in `cost_surface.csv`
6. **Iterates** — varies the next configuration, re-runs, observes how the cost surface changes

The agent interface is intentionally simple: edit two CSV files, run one shell script, read two log files. No Scala code involvement.

---

## What Needs to Be Built — Grand Plan

### Phase 0 — Data foundation (prerequisite for everything) 🔴
- [x] **Automated Portal Ingestion (`scripts/download_va_data.py`)** — Direct download & extraction of raw Expenditures, Revenues, and Budgets (FY2003–FY2016) from Virginia APA Open Data portal (`datapoint.apa.virginia.gov`).
- [x] **Scale-Invariant Sort Engine** — External merge-sort implemented in `standardize_and_sort.py` (Session 17). O(1) RAM, chunked spill-to-disk, k-way heapq merge. Mandate extended to `arrangementReclass.scala` reclass SJE sort (Session 19 — next session work item).
- [x] **Memory-Aware Parallel Admission Scheduler** — WLM-style admission loop implemented in `run_all_years.ps1` (Session 18). Named Win32 semaphore, free-RAM gate, MaxConcurrent=8.
- [x] **Purchase Order (PO) Attribute Extraction** — `scripts/extract_po_attributes.py` produces `vendor_po_attributes.csv` (69,813 vendors, 97% NIGP coverage). Static/collapsed output (modal attr across all years). **By-year output (`--by-year` flag) is next-session work item — required for SAL update cycle.**
- [ ] **CAR Update Cycle** ← **NEXT SESSION — builds before C1 baseline run**
  - `extract_po_attributes.py --by-year` → `vendor_po_attributes_by_year.csv` (VENDORID, fiscal_year, instNIGPClass, instNIGPDesc, instGeoRegion, po_line_count)
  - `scripts/car_detect_changes.py` (new) — sorted two-pointer walk over by-year PO data + current VendorMaster → `VendorUpdate.csv` with `change_type` (`RECLASS` vs `ATTR_ONLY`), effective fiscal year; O(1) memory
  - `VendorMaster.scala` datatype — add `instNIGPClass`, `instNIGPDesc`, `instGeoRegion` fields; VendorMaster becomes a history file (multi-row per instID, sorted by instID + instEffectDate)
  - `arrangementReclass.scala` — two scale-invariant fixes: (1) replace in-memory `vendorMasterMap` with streaming two-pointer walk; (2) replace `sortSJEFile()` in-memory sort with chunked external merge-sort
  - `scripts/data_aggregation.py` Step 5 — replace point-in-time dict lookup with as-of-date join (instEffectDate ≤ period ≤ instEffectEndDate)
  - **`instTypeID` changes** → trigger reclass SJEs via `arrangementReclass.scala`; **`instNIGPClass` / `instGeoRegion` changes** → VendorMaster history only, no SJEs
- [ ] **Assess USASpending.gov** — evaluate whether federal contract data provides richer instrument attributes for Rounds 2 and 4

### Phase 1 — Complete the single-curve infrastructure (Experiment 1 baseline) 🟡
*These items make C1 and C2 fully measurable.*
- [ ] **`cost_surface.csv` accumulator** — one row per pipeline run: `[run_id, curve, config_description, total_compute_s, total_storage_bytes, master_file_count, total_cost_proxy]`
- [ ] **Reconciliation obligation counter** — add enabled=Y ViewSpec row count to `pipeline_results.log` (AHI metric)
- [ ] **`--materialize` flag** — add to `LedgerApp.scala` and `run_pipeline.sh` to suppress intermediate files for "compute-through" configuration
- [ ] **Engine comparison harness** — document C1 run with `post.scala` vs. `DPBSpark.scala`; log both to `pipeline_results.log`
- [ ] **Fixture config files** — `data/AllocationRules.csv`, `data/InteragencyTransfers.csv`, `data/BudgetRules.csv` (needed for C4)

### Phase 2 — Complete the process layer pipeline (Curves C3 and C4) 🟡
*These items make revaluation-class and allocation passes runnable on fixture data.*
- [ ] **`dataAggregation.scala` — Postgres query engine rewrite** ← design complete; resolve open questions before building. See `docs/step5-postgres-design.md`. Replaces multi-pass in-memory design and `data_aggregation.py` (Technology Invariant). Four open questions: Postgres as required dependency, LDGR table lifetime, per-year vs. partitioned table, JDBC driver version.
- [ ] **`financialAllocation.scala`** — implement prior-day divisor design: read divisor balance from LDGR (partition B), join to AllocationRules.csv (partition C), generate allocated transactions in one pass
- [ ] **`forecastingBudgeting.scala`** — implement balance-read → growth-factor → budget balance pass (no A partition input; reads B and C only)
- [ ] **Verify `arrangementReclass.scala` + `consolidation.scala`** — confirm both run end-to-end on fixture data
- [ ] **Full C3 run** — steps 2,3,5,8,9 on fixture data; log to `cost_surface.csv`
- [ ] **Full C4 run** — steps 2,3,5,6,7,8,9,4 on fixture data; confirm C4 cost ≈ C3 cost

### Phase 3 — SAL enrichment and Experiment 2 (Pivot Theorem) 🟢
*These items make the four-round SAL expansion measurable.*
- [ ] **VendorMaster enrichment script** — derive `instNIGPClass` and `instGeoRegion` from existing data files; output enriched `VendorMaster.csv`
- [ ] **Uncomment ViewSpec.csv Round 1** — activate `instNIGPClass` views; run step 5; record `pivot_results.csv` row
- [ ] **Add Rounds 2–4** — add remaining SAL attributes (enriched or assessed from USASpending.gov); uncomment ViewSpec rows sequentially
- [ ] **Pivot theorem chart** — plot `views_answerable` and `key_embedded_equivalent_balances` vs. `sal_attr_count` across rounds; the exponential divergence is the theorem

### Phase 4 — Agentic loop and publication readiness 🟢
- [ ] **`scripts/AGENT_INTERFACE.md`** — API contract: what files to read/write, what commands to call, what log format means
- [ ] **Update `QUICKSTART.md`** — reflect full pipeline, two experiments, four curves
- [ ] **Verify compile + end-to-end run in Vagrant VM** — confirm full pipeline runs on clean environment
- [ ] **Cost surface charts** — plot C1–C4 cost curves on same axes; the C3/C4 overlap is the single-pass universality proof

---

## Relationship to the Monograph Papers

| This POC | Monograph paper | Specific claim being demonstrated |
|---|---|---|
| C1 cost curve | Paper 1 §§3.2, 3.2a | Interior minimum exists on the posting-only curve |
| C1 vs. C4 engine comparison | Paper 1 §3.2a | Engine-specific minimum — Scala vs. Spark minimum points differ |
| C3 vs. C4 cost equality | Paper 1 §3.2 | Single-pass universality — allocation collapses to single pass via prior-day divisor |
| Experiment 2: SAL expansion | Paper 1 §3.4 | **Instrument Pivot Theorem** — O(v^m) views at O(1) marginal balance cost |
| Experiment 2: permutation ratio | Paper 5 §§5.0a, 5.1 | Step-up is the operational mechanism; §5.0a states the economic consequence |
| Agentic loop | Paper 1 §7, Paper 2 §8 | Agentic AI navigates the minimum empirically, not just describes it |
| All runs | Paper 3 §3.3 | VA dataset as replicable controlled benchmark |
| VendorMaster as SAL | Paper 5 §3.3 | Two-level key: Vendor ID = Instrument ID; Nominal Account = balance bucket |
| Step 9 (arrangementReclass) | Paper 6 §§3.3, 5 | Reclass View as first-class business event, generated from SAL change |
| Financial Process Map mapping | Paper 4 §4 | CKB pass structure: A/B/C partitions, optional A, single-pass universality |

---

## The Theoretical Gap This POC Closes

The monograph proves the minimum cost point **exists** (two production natural experiments: insurer TDS and global bank AL). This POC **navigates** to it on public data, demonstrates that single-pass universality holds across all nine FSPs including allocation, and simultaneously demonstrates the Instrument Pivot Theorem — the output-side completeness result formalized in Papers 1 and 5 (Session 39).

The theorem, as stated in Paper 1 §3.4, is:

> The Instrument Ledger + Contract Attributes Record (CAR) is simultaneously **cost-minimizing** (fewest master files, zero reconciliation obligation) and **query-complete** (all O(v^m) reporting cuts answerable from a single balance store). These two properties are achieved by the same design choice — Instrument ID as anchor, CAR as attribute reservoir — which is why they must be stated together and why the legacy architecture's failure is doubly compounded: it costs more *and* answers less.

The four-curve experiment produces the measured cost surface that grounds the input-side claim. The SAL expansion experiment produces the measured permutation ratio that grounds the output-side claim. Both on public data. Both runnable by an agentic AI system.

---

*For session history, timing data, control totals, and open items, see `BOB_WORK_LOG.md`.*
*For the monograph papers this POC supports — link will be added upon publication.*
*For the VA data enhancement plan — GL/source-system gap demonstration, Basel bridge, phased CAR enrichment roadmap — see `docs/DATA_ENHANCEMENT_PLAN.md`.*
*For token-efficient agentic file access on this repo's large datasets, activate the `ckb-efficiency` skill at session start (full reference: `docs/BOB_METHOD_NOTE_CKB_EFFICIENCY.md`).*
*Current status: FY2003 C1/C2 baseline complete (Session 18). First all-years run (FY2003–FY2016) ready to execute via `run_all_years.ps1` — admission scheduler now in place. Phase 0 (data foundation) is the current blocker.*
