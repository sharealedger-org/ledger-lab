# Cost Graph Design — Working Notes

**Status:** In-progress design conversation, 2026-11-05.
**Purpose:** Capture the agreed graph designs so this conversation can be resumed in a new session.
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

## Open questions from the design conversation

1. **Engine Graph — Python implementation:** to run this experiment requires a Python
   implementation of the same financial processing logic. Owner approval required before
   writing (AGENTS.md). The graph design is agreed; the experiment is not yet scheduled.

2. **The movements footnote on the Mix graph** is a design principle, not a paper claim. It
   explains why the bowl minimum is a movement store. It does not need its own graph.

3. **AIM tests (Paper 2)** sit underneath these three graphs but are not shown on them. A
   separate one-pager maps CCP labels and AIM tests to the graphs — see the artifact
   produced in this session.

4. **Combination graph** — a fourth artifact overlaying all three dials (Mix, Engine,
   Process) onto one surface. Design not yet started; depends on the three standalone graphs
   being settled first.
