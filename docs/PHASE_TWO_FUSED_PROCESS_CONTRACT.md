# Phase 2 fused process candidate

Owner direction, 2026-11-16. **Bounded financial fixture verified; real-year/full admission pending.**
Owner explicitly stopped v2 before completion to prioritize this reorganization:
the workload proves synthetic generation, not a Paper 1 cost-curve measurement.
V2's completed evidence and interrupted attempt remain preserved. This candidate
preserves Paper 1's independent-stage profile and introduces a separately named
Phase 2 topology only after the gates below pass. No manuscript or Q2 changes.

## Workload and ownership

Same accepted historical sources plus the frozen wholly-generated subset;
reused financial copies remain non-additive. Same identity policy, +28-year
historical backing calendar, source/rule lineage, signed amounts and 39-field
canonical SJE boundary. Conditional full journal count remains 266,319,818.
The change concerns physical processing, not new accounting or source acceptance.

01 owns APA/PO parsing and ARE pair creation. The existing structural identity/
calendar policy remains a 02-owned SJE-to-SJE operation invoked at the paired
ARE output boundary, before initial financial key extraction. 03 owns posting;
GL/permutation are analytical observations over the SJE input, not newly generated
accounting entries. Bash orders compiled Scala processes and manages partitions.

## Candidate physical topology

1. Historical and wholly-generated Pay/PO adapters retain separate reference,
   rejection, source-acceptance and provenance controls. They feed a shared
   bounded financial sorter rather than materializing separately sorted terminal
   Pay/PO partitions that a later step rereads and merges.
2. At each historical journal-pair exit, apply one identity decision consistently
   to nominal and offset rows and shift dates/periods. Preserve the original source
   locator, document and rule identity. Wholly-generated pairs keep their existing
   identities/calendar. The pair already exists before sorting; this exit is not
   itself evidence of a sorted CKB join.
3. Financial sort-run merge emits the established vendor-first, 15-field ordered
   SJE stream into posting. Required sorted runs, merge levels and audit journal
   retention remain explicit physical I/O; fusion does not make sort disappear.
4. Posting's actual ordered journal-read boundary supplies each SJE exactly once
   to ledger processing and declared GL/permutation observers. Observers consume
   journal-grain records, not the summarized ledger (which loses dimension facts).
5. Bounded partial aggregation/distinct-fact spills and reducers remain separate
   processes/passes where their key order or memory contract requires them. They
   consume declared partial/fact partitions, not another scan of canonical SJEs.
   A same-JVM call after posting finishes is not evidence of this placement.

A final financial merge cannot recode vendor IDs in place while claiming its
output remains sorted: identity changes the leading key. The normalization exit
must precede key extraction, or its output needs a declared downstream sorter.

## Actual insertion points traced

- `standardizeAndSort.applyAPA`: `buildJEPairsAPA` returns the complete pair before
  the loop inserts rows into `chunk`; `flushChunk` extracts financial sort keys.
- `standardizeAndSort.applyPO`: `debitRow` and `creditRow` exist together before
  `pending` is drained into `SortEngine.externalSort`.
- `SortEngine.mergeSpills`: dequeues already-keyed rows and writes them unchanged;
  it currently has no normalization exit contract.
- `post.readJrnl`: reads/parses each SJE before the ordered posting loop consumes
  it; this is a candidate observation boundary.
- `GLPost`: currently scans SJEs into a year-wide GL-key hash map.
- `PermutationGenerator`: currently scans SJEs into bounded dimension-fact spills
  plus a GL-key index, dependency trackers and a seen-vendor set, then reduces.
  These state structures cannot simply be stacked inside posting without bounds.

## State, materialization and restart contract

Keep the paired-source bound (two rows), 100,000-row/32-MiB sort buffers and
bounded fan-in (16) unless a separately tested version changes them. Declare
shared-sort adapter order and tie ordering; canonical financial order is required,
while lineage-file physical order may change only with an explicit consumer
contract. Do not create an unbounded all-vendor/history map to avoid a sort.

GL partials require declared key/byte caps and overflow policy. Permutation
fact buffers, GL-key indexing, dependency tracking and merge fan-in need explicit
caps or an externalized design. Preserve the existing hash-based dimension and
capacity semantics when claiming equivalence; do not silently call capacity an
observed combination count. Record whether reduction spilled or stayed in memory.

The integrated posting/observer topology is one restart unit. Failed partial
ledger/GL/permutation products stay isolated and cannot become next-run inputs.
Pipe/partition backpressure, failure propagation and publication require tests.

Native historical GL equivalence remains a required control. Candidate fusion
must produce a bounded, independently checkable historical control branch from
unaltered accepted source pairs, or retain the independent comparator execution.
Dropping that control to eliminate a scan is not accepted. Any partial-control
sort/reduction is separately measured; no Bash/Python financial aggregation.

## Gates before replacing the active baseline

- Unchanged baseline tests and the real Bash posting-generation handoff pass.
- Named fused workload contains Pay and PO, historical and wholly-generated
  sources, negative amounts, known/unresolved/reassigned identities, receipts,
  date boundaries, non-monotone recoded IDs and repeated keys across adapters.
- Actual shared-sort output reaches actual posting and sequential reporting.
- Journal groups remain balanced; source acceptance and full journal multiset,
  rule/source lineage and effective dates match the existing contract.
- Ledger business values, GL rows and permutation tables/summaries match the
  independent profile. Compare deterministic bytes where contracts require them;
  declare benign within-key/lineage ordering differences rather than hiding them.
- Force multiple source runs, aggregation overflows, distinct-fact spills and
  merge levels; results remain invariant across declared buffer sizes.
- Trace actual producer/consumer boundaries and count journal source reads.
  Invoking observers after a completed scan does not meet the fusion claim.
- Record process/pipe/partition boundaries, per-process RSS and heap, CPU/wall,
  sort input/output/spill bytes, buffer peaks and distinct reduction costs.
- Preserve the independent Paper 1 profile and keep new measurements separately
  labeled. A faster single exploratory execution is not a new accepted cost point.

## Scheduling and target-runtime limits

Owner's four-CPU scheduling target is separate from fusion. Host inspection shows
four logical CPUs/two physical cores and 16 GiB RAM. Admit worker counts using
concurrent RSS and transient scratch, preserve the 40-GiB reserve, and account
for shared sbt launch behavior. Do not equate four logical CPUs with guaranteed
fourfold throughput or four safe copies of the largest sort footprint.

Existing GERS-001/002/003 target limitations remain open. Pair exits/shared Scala
sorters and bounded Scala observers would establish prototype behavior only;
they do not demonstrate equivalent GenevaERS identifier, reference-join or
extract/reduce runtime support. Record new target capability gaps if the candidate
requires an operation outside those contracts.

## Bounded implementation checkpoint, 2026-11-16 21:35 AZT

Implemented HistoricalJournalExit, optional paired APA/PO exits, SharedJournalSort,
FusedSourcePrepare, incremental GLPost/PermutationGenerator accumulators,
post.Observer and FusedFinancialYear. The independent profile delegates to the
same algorithms; it retains separate source scans and remains available.

The fused profile uses GL and permutation GL-key caps of 1,000,000 and vendor-
tracking cap of 1,000,000. GL currently fails on key overflow; it does not claim
partial-spill GL support. Permutation fact buffers retain the existing default
1,000,000 records/20,000 determinant values and add bounded merge fan-in (16).
The two-way fixture forces 47 spills/six levels and compares the exact table and
summary. Source-sort byte/row caps remain 32 MiB/100,000, fan-in 16.

Native historical/wholly GL controls consume original pairs before normalization;
GL maps are cleared after deterministic output. Posting emits observations from
its actual journal read and records actual file opens and ledger writes. A fixture
renames the input after opening to disprove any later GL/permutation file reread.
Shared controls verify raw historical GL hash, source-to-post counts and combined
GL keys before atomic year publication. 45 bounded/baseline tests pass. Real-year
heap/RSS, scratch peaks and source/report scales remain unverified; run a single
FY03 admission before any multi-worker corpus execution. Timing is diagnostic only.
