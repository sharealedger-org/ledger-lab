# Component pilot 0304 contract

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
