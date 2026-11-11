# Cost Graph Design — Working Notes

**Status:** Updated design conversation, 2026-11-11.
**Purpose:** Capture the agreed graph designs, mathematical formulas, and spreadsheet model specifications so Charlie Anderson and other researchers have the exact calculated values and equations.
**Companion:** [`docs/TEST_DESIGN.md`](TEST_DESIGN.md) — CCP/AIM measurement specs that sit underneath these graphs.

---

## The argument these graphs make

Three graphs — **Mix**, **Engine**, **Process** — constitute the Paper 1 argument. They are
the argument. CCP labels, AIM tests, and pipeline step IDs are evidence that sits underneath
them. Start here when explaining the research to any new audience.

The y-axis on every graph is **total cost to produce** (compute + storage + reconciliation).
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

### Mathematical Formulation of the Cost Bowl (For Charlie Anderson's Model)

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
* **Batch Time Equivalence:** Under CKB streaming (`post.scala`), reading the sorted SJE stream to produce annual movements (Run A, $\approx 350\text{k}$ rows, $40\text{ MB}$) takes the same single pass and elapsed time ($\approx 50\text{ s}$) as producing monthly movements (Run B, $\approx 1.2\text{M}$ rows, $120\text{ MB}$).
* **Query Time Divergence (The Transaction Fallback Tax):**
  * **Run B (`T-monthly`):** Answers monthly, quarterly, and annual questions directly by range-summing 1 to 12 movement rows from the $120\text{ MB}$ ledger. $\text{Scan Cost} = 0$.
  * **Run A (`T-annual`):** Can answer annual questions directly, but **cannot answer monthly or quarterly questions** because it destroyed sub-annual time boundaries. Any sub-annual query must fall back to scanning the multi-gigabyte raw `SortedJE` files:
    $$\text{Cost}_{\text{query}}(\text{Run A}) = |Q_{\text{annual}}| \times \text{Seek}(\text{LDGR}) + |Q_{\text{subannual}}| \times \text{Scan}(\text{Raw SortedJE})$$
* **Finding:** Run B is the natural bowl minimum for the Virginia corpus because it completely eliminates the transaction fallback tax at zero extra batch pass cost.

#### 3. The Right Wall: Steps 20, 21, and Multi-Master Proliferation ($X \to 100$)
* **Step 20 (`GLPost`):** Collapses dimensions into an account key `(agency, fund, object, nominal, period, dir_flag)`, producing $\approx 300\text{k}$ rows. It is cheap to compute, but suffers complete structural information loss (AIM1/3/4 fail).
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

## Spreadsheet Model Specification for Charlie Anderson

To allow Charlie Anderson to execute and manipulate the underlying mathematical model in Excel, the workbook is organized into four interconnected sheets:

### Sheet 1: `Model_Parameters` (Corpus Constants & Unit Rates)
* `N_events_yr`: Raw SJE transaction volume per year ($\approx 16\text{M}$ rows/yr).
* `S_raw_GB_yr`: Raw SJE file size per year ($\approx 5.0\text{ GB}$/yr).
* `N_vendors`: Distinct active instrument count ($\approx 65\text{k}$).
* `N_ldgr_ann_yr`: Run A annual ledger row count ($\approx 350\text{k}$ rows/yr, $40\text{ MB}$).
* `N_ldgr_mon_yr`: Run B monthly ledger row count ($\approx 1.2\text{M}$ rows/yr, $120\text{ MB}$).
* `Perm_Capacity`: Step 21 combinatorial capacity bound ($\approx 10^{9} \text{ to } 10^{12}$).
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
4. **Point 4 ($X=60$):** Run D (Movement Ledger + Pre-computed Rollup Cubes).
5. **Point 5 ($X=90$):** CCP1 Status Quo GL + 5 Subledgers.
6. **Point 6 ($X=100$):** Full Key Permutation Explosion (Right Wall).

### Sheet 4: `Multi_Substrate_Comparison`
Plotted comparison of Curve 1 (CKB / GenevaERS on Laptop), Curve 2 (GenevaERS on z/OS Mainframe), and Curve 3 (Track D Unrestrained AI / Shuffle Engine).

---

## Open questions from the design conversation

1. **Engine Graph — Python implementation:** to run this experiment requires a Python
   implementation of the same financial processing logic. Owner approval required before
   writing (AGENTS.md). The graph design is agreed; the experiment is not yet scheduled.

2. **The movements footnote on the Mix graph** is a design principle, not a paper claim. It
   explains why the bowl minimum is a movement store. It does not need its own graph.

3. **AIM tests (Paper 2)** sit underneath these three graphs but are not shown on them. A
   separate one-pager maps CCP labels and AIM tests to the graphs.

4. **Combination graph** — a fourth artifact overlaying all three dials (Mix, Engine,
   Process) onto one surface.

5. **Track D Separate Repo Setup:** Track D (unrestrained AI subject engine) will be executed in a clean, isolated repository with gated information feeds to prevent contamination from Ledger Lab CKB architecture rules.
