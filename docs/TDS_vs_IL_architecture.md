# TDS vs IL: Transaction Store vs Instrument Ledger Architecture

> **Status:** Documented from session discussion — flag for inclusion in the monograph
> **Relates to:** Papers 1, 4, 5 of the monograph; VA pipeline Steps 2 and 3

---

## The Core Distinction

The ARE/AL distinction is not about whether you keep history. **All transactions are always retained as permanent, auditable records of financial fact.** The distinction is about what unit of history the *processing* architecture operates against, and what structure carries the system-of-record forward.

### ARE — Arrangement Event (insurance company TDS pattern)

The Transform processes at a large US insurance company run **daily against today's activity only**. They:

1. Read raw input from today's source system feed
2. Standardize and transform into the TDS (Transaction Data Store) structure
3. Load today's standardized events into the TDS

The Transform itself has no concept of history — it is a pure present-tense operation. History accumulates in the TDS as a side effect of repeated daily loads, not because the ARE process reaches back. The TDS is the system of record at the transaction level.

**Key property:** Because every standardized transaction is retained in the TDS, the full balance history can be reconstructed from inception by summing TDS records. This is always possible — it is just not how reporting is normally driven.

### AL — Instrument Ledger (global bank pattern)

At banking transaction volumes, summing all transactions from inception to answer every balance query is not feasible within a run window. The Ledger accumulates:

1. Read today's new transactions (Partition A)
2. Read the prior period's balance (Partition B — the Ledger)
3. Post: merge and accumulate → write new Ledger

The Ledger balance IS the pre-computed answer. The transaction detail is retained in the audit record, but the operational system answers balance queries from the Ledger, not by re-summing transactions.

**Key property:** The Ledger is an optimization over the transaction record, not a replacement for it. It answers balance questions in O(1) lookups rather than O(N) transaction sums. The underlying transactions are always available for audit and reconstruction.

---

## Why Both Keep Everything

These are auditable systems of financial record. The raw input files, every transformed transaction, and every posted balance are retained permanently. The architecture choice governs what is *processed* each run — not what is *kept*.

- Raw input → **always retained**
- Standardized transactions (TDS / SortedJE) → **always retained**
- Accumulated balances (Ledger / LDGR) → **always retained**

This means:
- Any prior balance can be independently verified by summing transactions from inception
- Any transaction can be traced back to its raw source event
- The audit trail is unbroken from raw input through to the final reporting view

---

## The VA Pipeline Maps Both Patterns

The VA simulation deliberately implements both steps:

| Step | Pattern | Input | Output | Description |
|---|---|---|---|---|
| **Step 2** (`standardize_and_sort.py`) | ARE / Transform | Raw VARawFiles (today's quarter) | SortedJE — standardized transaction store | Reads only current period's raw activity; produces the TDS-equivalent transaction record |
| **Step 3** (`post.scala`) | AL / Post | SortedJE (Partition A) + prior LDGR (Partition B) | New LDGR | Two-pointer streaming merge-accumulate; produces the balance store |
| **Step 5** (`data_aggregation.py`) | Reporting | LDGR (Partition B) + VendorMaster (Partition C) | View CSVs | Reads balance store + SAL attributes; no transaction re-scan needed |

**SortedJE is not a gratuitous intermediate file.** It is the legitimate Partition A materialization — the standardized transaction record (TDS equivalent) that must persist as an auditable file and as the hand-off between the Python transform layer and the Scala posting engine.

---

## The Volume Argument

| Dimension | Large insurance company (ARE/TDS) | Large global bank (AL/Ledger) |
|---|---|---|
| Transaction volume | Relatively low | Extremely high |
| Practical history unit | Transaction-level TDS | Balance-level Ledger |
| Run window constraint | Can afford TDS re-scan for reporting | Cannot afford transaction re-sum for every query |
| Balance reconstruction | Sum TDS from inception (always possible) | Read Ledger directly (operationally necessary) |

The VA dataset sits between these extremes: ~100M transactions across 14 years. At this scale, the LDGR is operationally necessary for Step 5 reporting — scanning all SortedJE files for every reporting query would be prohibitive. But the SortedJE files are retained and could reconstruct the LDGR from inception if needed.

---

## Connection to the Instrument Pivot Theorem

The SAL join in Step 5 (VendorMaster → LDGR) does not replace the transaction audit trail. It adds a zero-marginal-cost reporting dimension on top of an already-complete, already-accumulated record. The theorem's claim — that every O(v^m) reporting cut is answerable from a single balance store — holds precisely because the balance store is complete. The audit trail behind it makes it trustworthy.

This is why the minimum cost point claim and the query-completeness claim must be stated together (Paper 1 §3.4): the same design choice (Instrument ID as anchor, Contract Attributes Record as attribute reservoir, Instrument Ledger as accumulated balance) simultaneously minimizes storage cost *and* maximizes reporting completeness *and* preserves the full audit chain.

---

## The Posting Key Trade-Off: The Interior Minimum Made Concrete

The AL design requires choosing a **posting key** — the set of dimensions accumulated into each balance record. This choice defines the cost minimum problem precisely:

### Too narrow a key

A narrow posting key is cheap to maintain — few balance records, low reconciliation overhead. But some reports cannot be answered from the balance store because the required dimensions were not in the key at posting time. Those reports require a **transaction fallback**: scanning the retained transaction history (SortedJE / TDS) to answer the query from source events.

The transaction fallback is always available — the transactions are always kept. But it is expensive when needed operationally, and it scales with transaction volume, not balance volume.

### Too wide a key

A wide posting key pre-materializes many reporting dimensions directly into the balance. Queries are answered cheaply. But:

- Balance record count grows combinatorially with each additional key dimension
- Every new dimension added to the key creates a new reconciliation obligation — a new master file that must stay synchronized
- The storage and maintenance cost compounds across the full balance history

This is the over-materialization failure mode most enterprises are actually in — decades of "add a new reporting dimension by widening the posting key" decisions have compounded into balance structures that are simultaneously expensive to maintain and still incomplete for outlying queries.

### The interior minimum

The minimum cost point is where:

> (marginal cost of widening the key by one dimension)
> =
> (marginal cost of the transaction fallback for the outlying reports that dimension would have answered)

In practice: design the posting key to answer the **bulk of operational reporting** directly from the balance store. Accept a transaction fallback for the rare outlying queries whose frequency doesn't justify the permanent key-widening cost.

### How the Instrument ID + Contract Attributes Record design shifts the minimum

The Instrument Pivot Theorem (Paper 1 §3.4) changes the terms of this trade-off:

- The posting key stays **narrow** — just Instrument ID
- Attributes are joined at query time from the VendorMaster (Contract Attributes Record / CAR)
- This answers the "outlying" dimensions **without widening the posting key** and **without a transaction fallback**

The CAR step-up join is O(1) per balance record — a hash lookup on Instrument ID. It costs nothing in storage (no new balance records written) and nothing in reconciliation (no new master file to maintain). Every attribute added to the CAR is a free pivot on all accumulated balance history back to inception.

This is why the minimum cost point shifts dramatically leftward: the Contract Attributes Record design decouples reporting completeness from posting key width. The cost curve's right-hand tail — previously driven by key explosion — flattens to near-zero. The transaction fallback is still available for audit and true edge cases, but it is no longer the operational path for any query answerable via a CAR attribute.

### The four-curve experiment as empirical proof

The C1–C4 cost surface experiment in this repository measures exactly this trade-off:

| Curve | Configuration | What it measures |
|---|---|---|
| C1 | Post only | Baseline balance maintenance cost |
| C2 | Post + CAR pivot views | Marginal cost of CAR (Contract Attributes Record) reporting (should be near-zero per new dimension) |
| C3 | + Reval-class processes | Cost of generated-transaction processes |
| C4 | Full pipeline | With CAR/Instrument Ledger design, C4 ≈ C3 — single-pass universality holds |

The overlap of C3 and C4 on the cost surface is the empirical proof that the Contract Attributes Record design has collapsed the key-widening cost to zero while maintaining full reporting completeness.

---

*Captured from session discussion. For inclusion in the monograph — relevant to Papers 1 §§3.2–3.4, 4 §4, 5 §3.3.*
*See also: `PROJECT_BRIEF.md`, `BOB_WORK_LOG.md`*
