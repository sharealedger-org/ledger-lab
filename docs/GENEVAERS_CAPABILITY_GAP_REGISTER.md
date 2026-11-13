# GenevaERS Capability Gap Register

Ledger Lab measures financial architecture and cost curves. GenevaERS owns the generalized execution substrate. When a Ledger Lab workload requires a runtime capability that GenevaERS does not currently provide, record the gap here instead of implementing a one-off substitute in the Scala prototype.

A gap is not a Ledger Lab defect by default. It becomes a GenevaERS enhancement candidate with a defined contract, implementation location, and validation workload.

## Gap Record Template

For each candidate enhancement, record:

- capability name and owning GenevaERS repository;
- financial workload that requires it;
- metadata/control artifact that requests it;
- execution phase and CKB boundary;
- input fields and output record contract;
- determinism, restart, retry, and parallelism requirements;
- lineage and audit requirements;
- collision, overflow, and failure behavior;
- process, memory, I/O, and materialization cost to measure;
- minimum test fixture and downstream consumer;
- whether the capability is required for a cost-curve point or only for a later production workload.

## GERS-001: Deterministic Unique Identifier Generation

**Status:** `candidate enhancement`

**Requirement:** A GenevaERS process must be able to generate a unique identifier for a newly
created contract, arrangement, or other persistent business entity derived from input data.

**Why it matters to Ledger Lab:** Some event and calculation workloads create new contractual or
instrument-level entities rather than only generating additional movements against existing IDs.
The resulting identifier must be available to downstream CKB sections, ledger state, lineage,
reports, and future runs.

**Required properties:**

1. **Uniqueness:** No two committed generated entities may receive the same identifier within the declared namespace and retention horizon.
2. **Determinism or durable allocation:** A retry of the same input must either reproduce the same identifier or recover the previously committed allocation without creating a duplicate.
3. **Parallel safety:** Separate readers, partitions, and concurrent jobs must not collide or depend on unsafe shared mutable state.
4. **Restart safety:** A failed run must define whether allocated identifiers are reusable, reserved, or recorded as gaps.
5. **Lineage:** The identifier must link to source event IDs, input partition, run/process identity, rule/version identity, and the generating view or exit.
6. **Ordering contract:** If downstream files require sorted identifiers or key placement, generation must preserve or declare the required sort/materialization boundary.
7. **Format and capacity:** Width, character set, namespace, rollover behavior, and collision detection must be explicit for z/OS and non-z/OS execution.
8. **Auditability:** The allocation decision must be observable in control output and replayable from the declared inputs and metadata.

**Possible implementation surfaces:** Run Control metadata/compiler support, Performance Engine
runtime services, or a CKB/write extension. The choice must be made by GenevaERS maintainers after
tracing the actual execution and restart model; Ledger Lab must not embed an ad hoc allocator.

**Ledger Lab validation contract:** A named synthetic workload creates entities from multiple
partitions and retries selected records. The acceptance test verifies uniqueness, retry behavior,
parallel/restart semantics, lineage, downstream consumption, and unchanged baseline results.

**Cost evidence:** Record allocation CPU/wall time, memory or shared-state use, generated rows,
spill/materialization, collision/retry counts, and whether the capability introduces a new physical
pass. Do not include a helper implementation in a comparable cost curve unless the GenevaERS
runtime enhancement is part of the declared engine configuration.

## GERS-002: Sorted-Merge Reference Join for Large Lookup Tables

**Status:** `candidate enhancement`

**Requirement:** A GenevaERS process must be able to join a large reference table to a primary
stream using a sorted-merge CKB pattern — both inputs sorted on the same join key, advancing in
lockstep — rather than requiring a pre-loaded in-memory hash map. This is the correct pattern
when the reference table cardinality is unknown, potentially in the millions, or may grow with
corpus size.

**Why it matters to Ledger Lab:** The current Scala Step 2 (`standardizeAndSort`) resolves
`VENDOR_NAME → VENDORID` using a `HashMap` loaded from `VendorMaster.csv`. This is architecturally
incorrect for full-corpus execution because:

1. Distinct vendor name strings across 14 years of APA expenditure data may be in the hundreds of
   thousands — a hash map of that size is not a GenevaERS-native pattern.
2. The GenevaERS target architecture resolves reference attributes through sorted-merge join passes,
   not in-memory maps. Using a hash map in the Scala prototype does not simulate GenevaERS cost.
3. The measured pass count for the CCP2 cost curve must include the vendor-name resolution pass.
   Hiding that pass inside a hash map understates the true sort/materialization cost.

**Required execution pattern:**

```text
Pass 1 — vendor name resolution:
  Stream (payment SJEs) sorted by VENDOR_NAME
  VendorMaster sorted by VENDOR_NAME (reference entity)
    → CKB merge on VENDOR_NAME → assign VENDORID
    → unmatched records receive UNMATCHED_<hex8> key
  Output: payment SJEs with VENDORID populated

Pass 2 — CKB instrument pass:
  Re-sort Pass 1 output by VENDORID
  Merge with PO SJEs (already VENDORID-keyed)
    → CKB processes both event types in instrument order
```

**Memory contract:** O(1) per record in both passes. No hash map. Sort cost is the dominant cost
and must be measured as two separate sort/materialization boundaries in `process_metrics.csv`.

**Execution phase:** Layer 01-transformation / Step 2 — after source standardization, before
the CKB instrument pass. The name-resolution pass is a pre-CKB sort boundary.

**Input contract:**
- Payment SJE stream with `VENDOR_NAME` populated (col 0 or declared field)
- VendorMaster reference file sorted by normalized `VENDOR_NAME`

**Output contract:**
- Payment SJE stream with `transIPID` (VENDORID) populated
- `UNMATCHED_VENDORS.csv` control output for unresolved names
- `RESULT step=2n` log line with matched/unmatched counts

**Determinism:** Both inputs are immutable; output is deterministic given the same inputs.

**Lineage:** Matched records carry the VendorMaster `instID` as `transIPID`. Unmatched records
carry a declared `UNMATCHED_<hex8>` key derived from the name hash — reproducible on retry.

**Ledger Lab validation contract:** A fixture with 5+ vendors, including one unmatched name,
proves that VENDORID is correctly assigned, unmatched records are explicit, and the control
total (matched + unmatched = total payment records) passes.

**Cost evidence:** Record sort cost for Pass 1 (name sort), merge pass row count, matched/unmatched
split, and re-sort cost for Pass 2 (VENDORID sort). Compare against the current hash-map
implementation to quantify the architectural difference in the cost curve.

**Current Scala approximation:** `loadVendorMap()` in `standardizeAndSort.scala` — valid at
fixture scale (183 VendorMaster rows); not valid for full-corpus cost measurement.

**Required before:** Full-corpus CCP2/CCP3 cost-curve runs can claim to simulate GenevaERS
execution costs. The hash-map implementation must be replaced or the workload marked
`blocked_by_engine_capability` for the sort-merge join capability.

## GERS-003: Extract-Time Summarization With Bounded Buffer, Overflow Flush and Reduce

**Status:** `prototype verified in Scala; candidate GenevaERS enhancement` (implemented in `CKBViews.scala` 2026-11-10; verified with unit test 16 in `CKBViewsSpec`)

**Requirement:** A GenevaERS extract-phase view must be able to summarize records by a declared group key into a bounded aggregation buffer while the ordered CKB stream is processed. When the buffer reaches its declared capacity it flushes partial aggregates (not raw rows) to declared scratch. A reduce step then sorts and collapses only the partial-aggregate file. Records are touched once by the extract; the reduce reads far fewer rows than the source.

**Why it matters to Ledger Lab:** `CKB_LAYER_EXECUTION_MODEL.md` ("Extract-Time Summarization") and `MEASUREMENT_AND_LOGGING.md` already specify this behaviour and its metrics (`aggregation_buffer_peak_keys`, `buffer_overflow`, `partial_aggregate_rows`, `final_reduce_rows`, `final_reduce_seconds`, `reduction_ratio`). `CKBViews` (step `5v`) does not implement it: View D is an unbounded `mutable.HashMap`. On the 14-year run (2026-11-10) it reached about 2.8M entries and filled a 1 GB heap; the step was stopped after 27 minutes with no output. See `VERIFICATION_AUDIT_REGISTER.md`, "2026-11-10 Full-corpus run 2".

**Owner notes (2026-11-10):**
- Views A and C have small, known key spaces; leave them unbounded and do not add a follow-on process for them (to be confirmed with counts, see the audit entry).
- The GenevaERS reduce process (MR88) is believed to run as a sort exit (E35 under the sort product), so the data are touched once. Confirm the exact mechanism with GenevaERS maintainers before relying on it; the Ledger Lab prototype has no sort exit hook (`SortEngine.mergeSpills` writes each dequeued row unchanged).

**Required properties:**
1. The view declares group key, associative metric (sum, count), buffer capacity (keys or bytes), spill format, and overflow reduction behaviour.
2. Overflow policy is declared (flush all, or flush least-used entries first). The choice is a design decision for the owner.
3. The final reduce sorts the partial-aggregate file by group key and collapses equal keys. If nothing spilled, no reduce is needed.
4. The result states whether it completed in memory or needed a reduce, and records the metrics above.
5. Deterministic: the same inputs and buffer size give the same final rows. Partial-aggregate rows carry the key and the associative state only.

**Ledger Lab validation contract:** a named workload whose group key space exceeds the declared buffer, with the final view equal to a full in-memory computation of the same view. Acceptance: row-for-row equality with the in-memory result on a fixture, control totals (row count and amount sum) equal on a full year, buffer size recorded as an experiment variable.

**Cost evidence:** buffer capacity, peak keys, flush count, partial-aggregate rows and bytes, reduce sort time, reduce output rows, and whether the reduce is a new physical pass.

**Required before:** a 14-year `5v` run can be called a CCP3 cost point. Until then `5v` over all 14 years is `blocked_by_engine_capability` (or must run with a declared, larger heap and be labelled as a different configuration).


## Boundary Rule

**2026-11-13 expanded reporting application (design/construction):** `ReportingPass`/`ReportingReduce` apply GERS-003 to all cross-vendor reporting cuts with a bounded extract buffer and separate bounded-fan-in Scala merge/reduction processes. Input streams (SJEs, actual posted balances, sorted IP/contract dimensions) meet at the vendor boundary. These are explicitly exploratory prototype measurements, not proof of a GenevaERS MR88 sort-exit implementation or a new cost-curve point. The generic ordered entity join in GERS-002 remains a target capability limitation: no whole-population dimension map is used here, and required dimension sorting is an explicit input preparation boundary. Tests must force multiple spills and merge levels and check equivalent final output before promoting fixture status. No target-runtime gap is closed by this implementation.

When a required capability is absent:

- Ledger Lab records the workload requirement and a falsifying acceptance test.
- GenevaERS receives the runtime enhancement request.
- Sharealedger may define the metadata/configuration contract once the runtime capability exists.
- Ledger Lab may measure the workload only with a declared capability version, or mark the workload `blocked_by_engine_capability`.

A Scala helper that bypasses the GenevaERS contract is not evidence that the target architecture ran.
