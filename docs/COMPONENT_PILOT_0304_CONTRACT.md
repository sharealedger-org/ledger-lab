# Component pilot 0304 contract

**Latest bounded result, 2026-11-15:** lineage-v2 completed the same financial
workload with exact snapshot-scoped source/document/rule provenance, unchanged
SJE monetary projection and byte-identical ledger/GL. See the
[v2 receipt](../data/test_evidence/component_pilot_0304_20261115/README.md#lineage-v2-continuation-after-consolidation-2026-11-15).
The following initial result describes v1 historically; scale and VA anchors are
still unverified. A richer permutation metadata population is not a new cost point.

Result: bounded actual run completed; see
[evidence](../data/test_evidence/component_pilot_0304_20261115/README.md). Source
assignment, consumption and controls passed within the declared fixture envelope.
End-to-end explicit event/rule lineage remains partial: APA drops raw event IDs
and legacy PO SJE rule fields are blank. No corpus-scale or AI claim is made.

2026-11-15. Named workload `component-pilot-0304-v1`. Owner authorized a bounded
FY2003/FY2004 test. Source preparation only; no financial engine change.

Falsifiable claim: context-generated records can resolve identities through a
sorted determination-key merge, be re-sorted into process/entity/date order, and
reach existing source adapters without changing financial identities or source
totals. Cheapest pre-edit falsifier: baseline adapter/report tests and actual
producer/consumer inspection. Baseline: 32 tests passed, log
`/private/tmp/phase2-0304-baseline.log`; source inspection identifies APA/PO parsers,
39-column SJE, SortEngine and expanded reporting. Employee financial adapter is
not implemented; employee responsibility evidence is an explicitly nonfinancial
input to the pilot checker. No model-effectiveness claim.

Inputs: the exact headers in `data/synthetic/component_pilot_0304_v1/`, plus
immutable reference/rules from `connected_reporting_v1` and the committed dated
history pack. IDs are supplied by the registry, not allocated by a new helper.
Budgets are synthetic envelopes, not verified FY2003/04 ACFR targets. Cross-context
relationships are proposed synthetic context, not journal allocations.

Topology: Scala context generation (sort context/year/family/record-kind/event),
Scala sorted assignment merge (source key/date/event), Scala entity emission
(process/entity/date/event), then existing Bash financial orchestration. Use
SortEngine with fixed-width concatenation-safe encoded sort keys, at most 4096
input records per file, 64 slots per context/year/family and 64 spill readers.
Reject exceeding the fixture envelope, missing/ambiguous identities, duplicate
event IDs, unsupported family/process mappings and invalid fiscal/availability
dates. Context and ID diagnostic sets are bounded by this declared fixture cap;
not a full-corpus registry implementation.

Materialization: retain context-sorted work, assignment-sorted instructions,
resolved entity-sorted instructions, generated APA/PO inputs, separate employee
evidence and withheld truth. No pipe profile claimed. All source files live under
a new attempt root; completion is atomic rename, failures retain partial evidence.
Record process rows/time, sort chunks/spills, partition bytes, hashes; Bash time
records process RSS separately. Generation cost is outside financial cost curves.

Outputs: APA expenditure/revenue exact existing schemas; PO exact 19-column input;
VendorMaster existing columns; explicit reporting template assignment; employee
responsibility evidence and visible event lineage with process/entity/event,
effective/available dates, project and amount. Private context and seed metadata
remain in withheld lineage. SJE lineage through source-file/row is verified by
the checker, and known totals/journal balance are checked at posting outputs.
The existing TTL still uses bounded lookup maps; this test establishes sorted
generation assignment, not a replacement corpus-scale TTL identity resolver.

Required checks: unchanged baseline, missing/duplicate assignment failure, slot
and sort-bound failures, input-order determinism, context envelope conservation,
cross-context vendor history, quiet master date change, receipt zero attribution,
cutoff-safe employee/event evidence, separate actual posting and expanded reporting
with tiny spill limits, source/posted reconciliation and dated master cuts.
Full-corpus capacity, target GenevaERS unique-ID/sort-runtime capabilities,
verified VA anchors, payroll engine, allocation/reclass engine and AI results
remain outside this pilot. Apply gap register GERS-001/002/003 qualifications.

## Next bounded slice: source/rule lineage v2

2026-11-15, continuation after consolidation. Claim to falsify: exact source-record
and rule/version provenance can reach the merged 39-column SJE stream without
changing monetary fields, posting keys, balances or process boundaries. Logical
owner is 01 TTL/ARE; insertion points are existing APA pair emission and PO pair
emission before their existing external sorts. No new financial rule or lookup
substrate is introduced. Determination-key assignment in generation remains the
sorted merge proven by v1; existing bounded TTL maps are not made corpus-scale.

Record contract: reuse SJE col 7 for `DOC1:<base64url original document>` when
present, col 34 for `RULE1:<base64url rule version>`, col 35 for
`SRC1:<base64url canonical family-relative path>:<source data record ordinal>`.
Original document remains Unknown where source has none. APA ordinals count data
physical lines, including rejected/blank rows; PO ordinals follow PORecordReader's
logical records, including rejected records. Locators are scoped to the immutable
input snapshot/manifest, not claimed globally unique across changed source files.
No new persistent entity-ID allocator is needed. Encoding preserves commas/Unicode
without changing the existing headerless comma-separated SJE parser contract.

APA records retain the selected rule-set/rule ID and now its configured version.
PO records identify the existing built-in quantity-times-price commitment rule,
version 1; do not pretend it was selected from VAAccountingRules.csv. This slice
does not implement effective-date rule selection or configurable PO accounting.
Event dates, rule selection, accounting dimensions, all 15 sort fields and the
30-column balance schema stay unchanged. Per-event provenance remains in retained
SJEs; aggregate balances are not asserted to preserve every event identifier.

No extra scan, history load, materialization profile or sort is required. Live
state is one source record plus existing chunk buffers; metadata increases SJE
bytes and must be included in storage/I/O metrics. Run metadata, source hashes
and existing process RSS remain separate from generation costs. Baseline and v2
are capability versions, not interchangeable cost replicates.

Cheapest falsifier already exists in v1: raw vouchers become Unknown and PO rule
fields are blank. Before edits rerun unchanged APA/PO/report/pilot tests. Add
focused controls for duplicate vouchers, rejected/blank rows, quoted document
values, identical filenames under different fiscal folders and relocated input
roots. Rerun the same generated FY2003/04 workload through actual orchestration;
verify two balanced SJE legs for each exact source locator, expected rule version,
original documents, and unchanged monetary/SJE projections and persisted balance
outputs against v1. Preserve both versions. VA anchors, payroll, AI and scale
remain explicitly unresolved after a successful lineage slice.

Consumer trace refinement before execution: PermutationGenerator includes original
document and source-extension fields among potential dimensions. Its metadata
cardinalities/capacity can change legitimately. Require byte-identical ledger/GL,
stable permutation financial keys/row counts, and independently checked document/
source distinct counts from the bounded SJE fixture; do not require byte-identical
permutation artifacts or hide their additional cost. The monetary task is unchanged
but the retained information capability is richer.
