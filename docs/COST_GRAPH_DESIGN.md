# Cost Graph Design — Working Notes

**Status:** Updated design conversation, 2026-11-11. Corpus constants and the Mix-graph points were updated the same day to the measured values from the three full 14-year runs (see "Mix graph: model run with measured corpus constants" below). The bowl curves are model output, not measurements.
**Purpose:** Capture the agreed graph designs, mathematical formulas, and spreadsheet model specifications so reviewers and other researchers have the exact calculated values and equations.
**Companion:** [`docs/TEST_DESIGN.md`](TEST_DESIGN.md) — CCP/AIM measurement specs that sit underneath these graphs.

---

## The argument these graphs make

Three graphs — **Mix**, **Engine**, **Process** — constitute the Paper 1 argument. They are
the argument. CCP labels, AIM tests, and pipeline step IDs are evidence that sits underneath
them. Start here when explaining the research to any new audience.

The y-axis on every graph is **total cost to produce** (batch + storage + query + reconciliation).
Not benefit — cost. Each graph uses the same bowl shape; only the x-axis dial changes.

---

## Mix Graph — The Cost Bowl: Events ↔ Balances

**Shape:** U-shaped bowl. Both walls are very high. The minimum is interior.

**X-axis:** dial from "use all transactions" (left) to "pre-compute all balances" (right).

**Left wall:** answer every question by replaying all transactions from inception.
IBM's balance sheet requires touching every transaction since 1911. Enormous compute cost.

**Right wall:** maintain a pre-computed answer for every anticipated question, kept
synchronized on every new transaction. Enormous storage + reconciliation cost. The number
of pre-computed balances grows combinatorially with reporting dimensions — this is the
permutation explosion.

The right wall is not a single system — it is a class. The GL, purchasing sub-ledger,
data warehouse, and data mart all sit on the right wall for the same structural reason:
each pre-computes answers to a fixed set of questions defined at design time and cannot
answer unanticipated questions without returning to raw events. They differ in which
dimensions they preserve, not in their structural position on the cost surface.

**Bowl minimum:** the Instrument Ledger — movement rows per (instrument, period), questions
answered by joining the CAR at query time. No pre-computation of the question required.
The key stays narrow; attributes are added to the CAR without widening the posting key.
This is why the bowl minimum answers a broader set of questions than any fixed-key system
at lower cost — not all questions (questions outside the preserved dimensions still require
event replay), but all questions formable from instrument identity, period, and CAR attributes.

**Status quo:** most institutions have accumulated decades of "add a reporting dimension by
widening the posting key" decisions. GL, sub-ledgers, data warehouses, and data marts all
represent positions on the right wall — some more collapsed than others, all paying
combinatorial cost for pre-computed answers to questions defined at design time.

**What it proves (Paper 1):** An interior minimum exists. Most institutions are past it,
paying combinatorial cost for pre-computed answers to questions that could have been answered
at query time from a narrower, cheaper store.

### Mathematical Formulation of the Cost Bowl (CKB Cost Model)

Total lifecycle cost for any architectural point $P$ across the mix dial is decomposed into four explicit terms:

$$\text{Total Cost}(P) = \text{Cost}_{\text{batch}}(P) + \text{Cost}_{\text{storage}}(P) + \text{Cost}_{\text{query}}(P) + \text{Cost}_{\text{reconciliation}}(P)$$

#### 1. The Left Wall: Pure Event Replay ($X = 0$)
* **Mechanism:** Store zero ledgers/balances. Retain only raw transaction events (`SortedJE`).
* **Batch Cost:** $\text{Cost}_{\text{batch}} = 0$.
* **Storage Cost:** $\text{Cost}_{\text{storage}} = \text{Size}(\text{Raw Events}) \times \text{Unit Storage Rate}$.
* **Query Cost:** Paid at **query time**. Every financial question (annual balance sheet, monthly actuals, vendor spend) must scan the full historical transaction corpus:
  $$\text{Cost}_{\text{query}} = \sum_{q \in Q} \Big( \text{Size}(\text{Raw Events}) \times \text{Unit Scan Cost} \Big) = |Q| \times \text{Scan Cost}$$
* **Reconciliation Cost:** $0$ (single source of truth).
* **Behavior:** As query volume $|Q|$ rises, total cost shoots vertically up the left wall.

#### 2. Run A (`T-annual`) vs. Run B (`T-monthly`): Where Run A Actually Sits
* **Run A is NOT on the Left Wall:** Run A produces an Instrument Ledger at annual granularity (`LDGR20YY.csv`, 1 row per instrument-year). It sits at the **left edge of the bowl bottom**, not at $X = 0$.
* **Batch Time Equivalence (not measured at corpus scale):** Under CKB streaming (`post.scala`), reading the sorted SJE stream to produce annual movements takes the same single pass and elapsed time as producing monthly movements. This was stated from the early FY2003 work and has not been re-measured. **No annual-granularity run exists at corpus scale.** The corpus runs are all monthly (Run B): 345,609 to 979,181 ledger rows per year, 10,651,908 in total, 1.71 GB (about 161 bytes per row). The earlier figures in this note ($\approx 350\text{k}$ and $\approx 1.2\text{M}$ rows, $40\text{ MB}$ and $120\text{ MB}$, $\approx 50\text{ s}$) were earlier estimates and are superseded by the corpus runs. The Run A size used in the model below (0.57 GB, one third of Run B) is an assumption carried over from that ratio, not a measurement.
* **Query Time Divergence (The Transaction Fallback Tax):**
  * **Run B (`T-monthly`):** Answers monthly, quarterly, and annual questions directly by range-summing 1 to 12 movement rows from the monthly ledger (1.71 GB over 14 years). $\text{Scan Cost} = 0$.
  * **Run A (`T-annual`):** Can answer annual questions directly, but **cannot answer monthly or quarterly questions** because it destroyed sub-annual time boundaries. Any sub-annual query must fall back to scanning the multi-gigabyte raw `SortedJE` files:
    $$\text{Cost}_{\text{query}}(\text{Run A}) = |Q_{\text{annual}}| \times \text{Seek}(\text{LDGR}) + |Q_{\text{subannual}}| \times \text{Scan}(\text{Raw SortedJE})$$
* **Finding:** Run B is the natural bowl minimum for the Virginia corpus because it completely eliminates the transaction fallback tax at zero extra batch pass cost.

#### 3. The Right Wall: Steps 20, 21, and Multi-Master Proliferation ($X \to 100$)
* **Step 20 (`GLPost`):** Collapses dimensions into an account key `(agency, fund, object, nominal, period, dir_flag)`, producing 276,850 to 564,242 keys per year (6,433,714 over 14 years; about 0.58 of the ledger row count in the years with purchase orders). It is cheap to compute, but discards vendor, order date and event sequence. The AIM tests that illustrate the loss (AIM1, AIM3, AIM4) are provisional and are not shown on these graphs; the AIM3 hold-out passes (C = 0.7527), so do not cite an AIM "fail" here.
* **Step 21 (`PermutationGenerator`):** Acts as a **structural sizing instrument** for externalized upstream cost. It calculates the combinatorial capacity:
  $$\text{Capacity} = \sum_{k \in \text{GL Keys}} \left( \prod_{d \in \text{Independent Dims}} |V_d(k)| \right)$$
* **Physical Enterprise Reality:** To answer queries outside the GL key in balance space, enterprises instantiate $M$ separate subledgers, OLAP cubes, and data marts.
* **Storage Cost:** $\text{Cost}_{\text{storage}} = (\text{GL Size} + M \times \text{Subledger Sizes} + \text{Capacity Footprint}) \times \text{Unit Storage Rate}$.
* **Batch Write Amplification:** Every source transaction must update $M$ distinct balance stores with row locking and index maintenance:
  $$\text{Cost}_{\text{batch}} = M \times \text{Base Ingestion Cost}$$
* **Reconciliation Tax:** Paid continuously at batch/month-end close to synchronize and audit $M$ disparate balance stores:
  $$\text{Cost}_{\text{reconciliation}} = \binom{M}{2} \times \text{Reconciliation Sync Cost} = \frac{M(M-1)}{2} \times \text{Unit Sync Cost}$$
* **Behavior:** As dimensional reporting requirements grow, storage and reconciliation explode combinatorially up the right wall.

### Footnote on Graph 1 — why the minimum is a movement store, not a balance store

Once you have decided to store something at the bowl minimum (rather than replay raw events),
the choice is: store **movements** (net of all journals per key-period) or store **running
balances** (pre-computed cumulative totals at each period-end).

A pure movement vs. stored-balance graph (granularity fixed) is **upward sloping** — not a
bowl. Movements are always cheaper:
- Storage cost: movements are minimal (one row per key-period added, nothing updated)
- Query cost: bounded sum of a date range — cheap for any reasonable range
- Update cost: zero — new transactions add new movement rows, no pre-computed balance needs updating
- Pre-computed balances add storage + reconciliation cost on every new transaction, with no
  query-cost saving that justifies it

This is a design principle, not a paper claim. It justifies why `LDGR20YY.csv` stores net
movements per key-period, not running balances.

### Footnote on Graph 1 — granularity choice

Graph 2 (a family of bowls by granularity level) is dropped. Granularity is a one-time
design decision, not a paper variable:

The VA source data is monthly at its finest. Storing monthly movements is therefore the
natural bowl minimum for this corpus — it matches the source and answers any monthly or
annual output question directly without event replay. There is no empirical case for
testing annual vs. monthly granularity as an experimental axis because the source
constrains the choice: monthly is both necessary (to answer monthly questions) and
sufficient (it is the finest grain the source provides).

The A vs. B CCP comparison (T-annual vs. T-monthly period keys) remains in the test plan
as a check that the period-key wiring is correct — not as a cost-of-granularity claim.

---

## Mix graph: model run with measured corpus constants

This section turns the formulas above into numbers. **Measured inputs** are from the full runs (evidence in `data/test_evidence/corpus_runs/`). **Assumed inputs** are unit rates and query volumes chosen for illustration and are the owner's to change. Cost is dollars per year of operation over the 14-year history. The curve is therefore a model; only the corpus constants are measured.

| Input | Value | Basis |
|---|---|---|
| Raw events (merged SJE) | 56.69 GB, 224,072,410 rows | measured |
| Monthly ledger (Run B) | 1.715 GB, 10,651,908 rows | measured |
| GL | 6,433,714 keys, about 1.04 GB at 161 B per row | rows measured; size estimated at the ledger row width |
| Permutation capacity | 503,118,463,033,110 | measured, upper bound |
| Engine CPU (all steps) | 11,207 s over 14 years | measured (run 4) |
| Scan, seek, storage, reconcile rates | $0.02/GB, $0.0001/query, $0.023/GB-month, $50/pair-month | assumed (Sheet 1) |
| Ingest rate | $0.05 per CPU-hour | assumed |
| Queries per year | 1,000 annual, 500 sub-annual, 100 dimensional | assumed |

| X | Point | Stores (M) | Cost per year | log10 |
|---|---|---|---|---|
| 0 | P1 Pure event store | 0 | $1,830 | 3.26 |
| 15 | P2 Run A, annual ledger + sub-annual scans | 1 | $583 | 2.77 |
| 30 | P3 Run B / CCP2, monthly ledger + CAR | 1 | **$16.29** | **1.21** |
| 60 | P4 Ledger + rollup cubes | 2 | $617 | 2.79 |
| 90 | P5 CCP1 status quo, GL + 5 sub-ledgers | 6 | $9,018 | 3.96 |
| 100 | P6 Full key permutation (storing the capacity bound) | | $22.4 million | 7.35 |

Reading it:

- The minimum is at P3. Everything to its right is dominated by reconciliation ($600 per pair per year at the assumed rate) and, at P6, by storing the capacity bound, which is an upper bound and not a design anyone would build.
- **The left wall is set by the sub-annual query volume, not by the ledger.** With 500 sub-annual queries a year P2 costs $583. With none, P2 costs $15.92 and sits slightly below P3 ($16.29). The case for monthly granularity therefore rests on sub-annual questions being asked, which is an assumption about the user, not a corpus measurement.
- At corpus scale the CKB batch cost is cents a year. The model's walls come from assumed query and reconciliation rates. Changing `Cost_Reconcile_per_Pair` or the query mix moves the walls and the minimum's depth; it does not move the minimum from P3 unless sub-annual queries fall to zero.
- Plot on a log y-axis; the points span eight orders of magnitude.

---

## Engine Graph — Pattern: Naive → CKB

**Shape:** The same bowl as the Mix graph. Single curve. Same y-axis (total cost to produce).

**X-axis:** engine dial — right = naive multi-pass (multiple scans, full data load, O(N)
working set). Left = CKB engine (sorted-merge, streaming, O(1) memory). Companies start
top-right because most production systems use naive multi-pass processing.

**CKB pattern steps along the curve (right → left):**
1. + sorted input → one pass possible instead of many
2. + streaming merge → no full load into memory
3. + O(1) memory → linear scaling, not O(N)

Each step is an independent improvement; they compose. The bowl minimum is defined by
adopting all three — it is architecture-determined, not language-determined.

**What it proves (Paper 1):** Engine improvement is an independent dial, separate from the
mix dial (Mix graph) and the process dial (Process graph). A company can descend the Engine
curve without changing their mix or their process layer. The Combination graph will show how
all three dials interact.

**Note:** This is not yet a CCP label. It is a new experimental axis — same financial logic,
two engine substrates, CKB patterns introduced incrementally. Requires owner approval before
writing any Python mainline code (see AGENTS.md).

**Measured state (2026-11-11):** only the CKB end of the dial is measured: three full 14-year
runs on one 4-CPU, 16 GB laptop, 1 GB heap, highest process memory 1.36 GB. The naive end has no
corpus run; the "10x to 50x" lift below is a design estimate with no measurement behind it.

**Second curve — mainframe / TDS (open invitation):** The Engine graph currently shows one
curve — VA corpus, laptop, measured. A second curve representing a z/OS mainframe running
GenevaERS on the insurance company's TDS (25.5 billion versioned state-change events) would
show the hardware effect: same CKB architecture, same bowl shape, but the minimum sits
further left. On z/OS, the scan cost per question is low enough that preserving full event
sequence (no accumulation) becomes feasible — the bowl minimum migrates leftward toward the
raw event record. The two curves together make the hardware-parameterization of the bowl
minimum empirically visible: same architecture, different hardware, different minimum. This
remains conceptual until the insurance company engages. It is recorded here as an open
invitation for collaboration, not a gap in the current experiment.

**Third curve — Unrestrained AI / Shuffle Engine (Track D - "Twisting of the Curve"):**
When an unrestrained AI coding assistant (or conventional data engineer) builds a financial
engine without CKB architecture rules, it falls into historical habits:
1. It uses in-memory accumulators, DataFrames, and shuffle stages (`groupBy`, Spark shuffles).
2. It aggregates early and discards event detail, building fixed summary tables.

This produces a **twisted and elevated cost bowl**:
- **Left Wall Twists Vertically:** An event-replay query triggers distributed shuffles, memory thrashing, and disk spill buffers instead of streaming sequential reads.
- **Bowl Minimum Lifts:** The bottom of the bowl is elevated by $10\times$ to $50\times$ in compute and memory footprint (requiring a multi-node cluster to process what CKB streams on 1 laptop thread).
- **Right Wall Accelerates Outward:** The AI proliferates auxiliary summary tables and cubes, rapidly triggering write amplification and permutation capacity explosion.

---

## Process Graph — CCP Layers: CCP1 → CCP5

**Shape:** The same bowl as the Mix and Engine graphs. CCP1–CCP5 plotted as positions.

**The insight:** adding financial process layers (views, revaluation, allocation,
consolidation) generates more outputs — which could mean more pre-computed balances, pushing
rightward up the right wall. The claim is that each layer stays near the bowl bottom because
each is one more bounded pass over movement rows, not a new balance table.

**Measured state (2026-11-11):** CCP1 and CCP2 have a `cost_surface.csv` row per year in each of
three runs; CCP3 (`5v`) ran over all years (1,285 s baseline, 1,696 s run 4). CCP4 and CCP5 have
fixture-scale tests only. No point has three replicates. Use CPU seconds and total wall time, not
`total_compute_s` or `total_cost_proxy`, which contention inflates.

- **CCP1 (GL baseline):** on the right wall. The GL architecture pre-computes account-keyed
  balances and discards instrument identity — exactly the permutation explosion the bowl
  describes.
- **CCP2 (Instrument Ledger):** at the bowl bottom. One movement row per key-period.
- **CCP3 (+ Views):** slightly rightward. The Step 5v views are bounded aggregations produced
  in the same pass — not new balance tables.
- **CCP4 (+ Reval/Reclass):** slightly more rightward. Generated SJEs add rows but not new
  balance tables.
- **CCP5 (+ Allocation/Consolidation):** ≈ same position as CCP4 if single-pass universality
  holds. Allocation via prior-day divisor (already a balance in partition B) adds no pre-scan
  pass and no new balance table.

**CCP5 ≈ CCP4 is the empirical proof of single-pass universality.**

**What it proves (Paper 1):** The complete financial process stack stays within the bowl
bottom. CCP1 → CCP2 is the architectural leap from right wall to bowl bottom. CCP2 → CCP5
is a small rightward slide along the bottom. Adding full enterprise capability does not push
cost up the right wall.

---

## Spreadsheet Model Specification: CKB Cost Model

To allow a reader to execute and manipulate the underlying mathematical model in Excel, the workbook is organized into four interconnected sheets:

### Sheet 1: `Model_Parameters` (Corpus Constants & Unit Rates)
Measured values from the full runs (run 4, `20261111_064052`), 2026-11-11:

* `N_events_yr`: merged SJE rows, 9.3M to 19.5M per year, 224,072,410 over 14 years (average 16.0M).
* `S_raw_GB_yr`: merged SJE size, 56.69 GB over 14 years (average 4.05 GB per year).
* `N_vendors`: not measured as a distinct count. The ledger holds 199,727 vendor-year pairs over FY03-FY16.
* `N_ldgr_ann_yr`: **not measured** (no annual run at corpus scale). Model assumption: one third of the monthly size.
* `N_ldgr_mon_yr`: Run B monthly ledger, 345,609 to 979,181 rows per year, 10,651,908 total, 1.715 GB.
* `N_gl_keys`: GL keys, 276,850 to 564,242 per year, 6,433,714 total.
* `Perm_Capacity`: Step 21 capacity bound, 10^8 to 10^14 per year, 503,118,463,033,110 summed; upper bound.
* `Cost_Scan_per_GB`: Unit scan cost for raw events (e.g. $\$0.02/\text{GB}$).
* `Cost_Seek_per_Q`: Unit lookup cost in narrow LDGR + CAR (e.g. $\$0.0001/\text{query}$).
* `Cost_Store_per_GB_mo`: Enterprise storage rate (e.g. $\$0.023/\text{GB-month}$).
* `Cost_Reconcile_per_Pair`: Monthly reconciliation cost per master pair (e.g. $\$50.00/\text{pair-month}$).
* `Q_annual`, `Q_subannual`, `Q_dimensional`: Annual, sub-annual, and AI query volumes.
* `M_subledgers`: Count of parallel legacy balance stores ($M = 5$).

### Sheet 2: `Cost_Equations` (Cell Formulas)
* `Batch_Cost(i)`: Model ingestion / CKB pass / multi-master write amplification.
* `Storage_Cost(i)`: $\text{Total Stored GB} \times \text{Cost\_Store\_per\_GB\_mo} \times 12$.
* `Query_Cost(i)`: $\sum (\text{Queries} \times \text{Scan/Seek Unit Costs})$.
* `Reconcile_Cost(i)`: $\frac{M(M-1)}{2} \times \text{Cost\_Reconcile\_per\_Pair} \times 12$.
* `Total_Cost(i)`: Sum of all four cost components.

### Sheet 3: `Cost_Bowl_Data` (Calculated Curve Coordinates)
Coordinates plotted across the Mix Dial ($X \in [0, 100]$):
1. **Point 1 ($X=0$):** Pure Event Store (Left Wall).
2. **Point 2 ($X=15$):** Run A (`T-annual` Movement Store + Sub-annual Event Scans).
3. **Point 3 ($X=30$):** Run B / CCP2 (Instrument Ledger Monthly Movements + CAR — Bowl Minimum).
4. **Point 4 ($X=60$):** Run D (Movement Ledger + Pre-computed Rollup Cubes). No Run D exists; the model costs it as two stores.
5. **Point 5 ($X=90$):** CCP1 Status Quo GL + 5 Subledgers.
6. **Point 6 ($X=100$):** Full Key Permutation Explosion (Right Wall).

### Sheet 4: `Multi_Substrate_Comparison`
Plotted comparison of Curve 1 (CKB / GenevaERS on Laptop), Curve 2 (GenevaERS on z/OS Mainframe), and Curve 3 (Track D Unrestrained AI / Shuffle Engine).

---

## Open questions from the design conversation

1. **Engine Graph — conventional implementation (superseded by Track D):** the naive engine is
   now Track D, run in a separate repository on another machine; it has not run on the corpus.
   Original note: to run this experiment requires a Python
   implementation of the same financial processing logic. Owner approval required before
   writing (AGENTS.md). The graph design is agreed; the experiment is not yet scheduled.

2. **The movements footnote on the Mix graph** is a design principle, not a paper claim. It
   explains why the bowl minimum is a movement store. It does not need its own graph.

3. **AIM tests (Paper 2)** sit underneath these three graphs but are not shown on them. A
   separate one-pager maps CCP labels and AIM tests to the graphs.

4. **Combination graph** — a fourth artifact overlaying all three dials (Mix, Engine,
   Process) onto one surface.

5. **Track D Separate Repo Setup:** Track D (unrestrained AI subject engine) will be executed in a clean, isolated repository with gated information feeds to prevent contamination from Ledger Lab CKB architecture rules.
