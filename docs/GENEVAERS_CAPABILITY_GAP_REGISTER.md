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

## Boundary Rule

When a required capability is absent:

- Ledger Lab records the workload requirement and a falsifying acceptance test.
- GenevaERS receives the runtime enhancement request.
- Sharealedger may define the metadata/configuration contract once the runtime capability exists.
- Ledger Lab may measure the workload only with a declared capability version, or mark the workload `blocked_by_engine_capability`.

A Scala helper that bypasses the GenevaERS contract is not evidence that the target architecture ran.
