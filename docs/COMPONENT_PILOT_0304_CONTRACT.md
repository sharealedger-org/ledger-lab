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

## Prepared next workload: FY2012 anchor and bounded scale

2026-11-15 08:05 AZT. Owner requested push and next-test preparation. `5e44a8e`
is pushed; no new financial execution accompanies this section. Inputs are in
`data/synthetic/component_anchor_2012_v1/`; verified facts are the two versioned
General Fund CSVs in `data/acfr_anchors/`, with source/page/hash records in their
existing manifest. Three offline Scala tests validate flow identities, six-item
closing-balance reconciliation, unit conversion and scope/sign rejection. These
checks do not reconcile observed APA data or constitute a posted adjustment engine.

Source verification: FY2012 General Fund budget Actual (L p174), modified-accrual
statement (G p50), and signed budget-to-modified-accrual closing balance adjustments
(L p177). Food-stamp estimate excluded: the source footnote is Federal Trust Fund,
not General Fund. The six bridge items concern closing balances, not a movement
breakdown. Older estimated/derived CSVs remain historical and are not added to
these six items. Source PDFs are optional provenance, not runtime dependencies.

Workload: a fictional panel calibrated to 1% of the published General Fund budget
values. Convert thousands to USD once, then apply the fraction. Revenue envelope
USD 171,019,590; expenditure USD 168,940,140, constant across population sizes.
No additive union with observed VA data. Opening balance, other financing and
closing balance are separate control measures. Bridge records remain diagnostic
until explicit balanced accounting offsets and adapters are declared. No allocation
or reclass engine is introduced as a prerequisite. PO commitments have their own
synthetic policy and do not count as additional recognized expenditures.

Admission ladder: three components/90 events/168 SJE rows, then 100/3,000/5,600,
then 1,000/30,000/56,000. Each component has 12 expense events, 12 receipts,
four commitments and two nonfinancial employee responsibility events. The next
scenario matrix must define component shares, repeated/shared participants,
incidental/ambiguous connections, date changes and observation cutoffs without
answer-bearing labels. Holdout mechanisms remain separate from these inspection
cases. Supply namespace-checked identities as inputs; no runtime entity allocator.

Required implementation before launch:

1. Generalize the hardcoded 2003/04 calendar and source-family routing while
   retaining the unchanged lineage-v2 regression. Do not retime old history packs
   silently or equate an earlier record's availability with its effective date.
2. Determine component budgets in anchor/year/fund/basis/measure order by merging
   anchors and declared synthetic shares; control share totals and cent remainders.
   Emit budgets into context order, then retain the determining-key assignment
   merge and receiving entity/date order. These are preparation/calibration rules,
   not asserted economic allocations or new accounting journals.
3. Replace population-wide event-ID/entity-ID sets with declared externally sorted
   uniqueness/coalescing controls. Keep per-context recipe state bounded; emit
   master/assignment rows in registry/entity order rather than collecting all
   vendors. Resolve ambiguous assignments explicitly and reconcile every emitted
   record. Do not materialize an entity lifetime into a JVM collection.
4. Use bounded multi-level sort merging with at most 16 live readers and a fixed
   256-row chunk in scale cases. Current SortEngine opens every spill reader and
   the pilot has fixed row/byte caps; simply raising those caps is not admission.
   Record levels, passes, rows/bytes, scratch, wall/CPU and process RSS separately.
   Respect target-runtime capability gaps; input-preparation prototype results do
   not establish a new measured GenevaERS capability or cost point.
5. Replace hardcoded 36-event checker assumptions with manifest-driven expected
   counts and sorted control streams. Retain independent known totals, exact
   source/rule lineage, period/side/currency reconciliation, temporal cases,
   correct rejection of scope/sign/duplicate/cutoff faults, and provenance-aware
   permutation checks. Keep financial amount and model-evidence evaluations distinct.

Use a 1,024 MiB generation heap, explicit scratch/retention caps and a fresh local
output root; resource admission must recheck the live host. Repeat a named case
with reordered inputs and a different chunk size after primary correctness passes.
Abort on any failed control or resource allowance; preserve failed attempts. Before
full-corpus admission, separately test a legitimate and an unattributed key above
250,000 reporting input rows. The planned 30,000-event case does not cover that.

Owner stopped Dropbox; OneDrive remains a potential post-success archive. Keep
active processing local. Verify archive destination/capacity and hashes before
any future copy. No OneDrive upload or full-corpus run was started by preparation.

## Mandatory unattributed-key practicality projection

Owner direction, 2026-11-15: bounded memory/spill support is insufficient if a
remaining zero-ID group takes impractical time. Add actual report probes and a
conditional full-corpus projection before admission. Preserve the zero sentinel;
do not invent vendors or split identities to make the test pass.

Historical audited population: 181,736,476 zero-ID movement rows and 3,559,680
posted rows, 185,296,156 total. The largest-group CSV omits a nine-zero PO key
(58 rows); use canonical summary/breakdown totals. Parser summary still reports
87,815,525 unmatched expense source records; enrichment changed zero name lookups.
Those inputs predate current adapters and are not a fresh future-run census.
If all agency-grain revenue is retained unattributed, as intended, current policy
can increase zero counts rather than reduce them. Model explicit historical,
quote-aware-expense/all-revenue-zero and frozen-accepted-APA ceiling scenarios;
refresh actual counts before full admission. Do not call historical counts an
upper bound on the changed workload.

Named workload `report-zero-key-projection-v1`: synthetic, balanced 39-column SJE
snapshots, posted by the existing Scala post process, then actual expanded 5v/5r
with temporal/master cuts, default prospective report capacities (50,000 base /
10,000 synthetic keys, fan-in 16). Fixture source preparation is outside report
timings, not a replacement TTL engine or a comparable cost-curve run. Fixed 1 GiB
JVM heap / four active processors, local paths, supplied zero ID only. Source
preparation sort has 50,000-row chunks, at most 64 readers and at most 2M rows;
report does not acquire a new sort implementation or changed financial logic.

Probe cases: 500k movement rows with repeating keys; 2M movement rows with roughly
2% posted-key ratio like the historical group; and 500k movement rows with one
unique direct posting context per source pair to stress distinct-key/output cost.
Keep date/agency/fund/account diversity explicit. The last case is intentionally
more diverse than the historical average, not a claim about real VA relationships.
Profile with original zero identity, not an assigned synthetic vendor. Every probe
must complete reconciliation/coverage/conservation controls; retain abort/failure
receipts. Per-probe runtime limit 15 minutes and root size 20 GiB are engineering
probe bounds, not an owner-approved full-run window.

Record extraction order-index scan time (report has two input passes), aggregation,
merge levels/passes, cumulative spill writes, output bytes, reduction time, OS wall/
CPU/RSS and sampled root occupancy. Derive row-rate and diversity stress scenarios
separately; include larger merge-depth sensitivity, full-input read volume and
output/scratch expansion. A worst-observed-rate extrapolation is not a guaranteed
upper bound or confidence interval. Do not equate cumulative bytes written with
peak scratch, or extrapolate the small two-row-buffer pilot as production settings.
This projection covers the zero-key reporting component only, not generation,
posting, the remaining 85M baseline report rows, or the entire run.

Practicality evidence completed: the
[zero-key receipt](../data/test_evidence/component_pilot_0304_20261115/zero_key_projection/README.md)
records all three profiles, controls, OS metrics and conditional projections.
Matched-density scenarios are hours, not instant completion; distinct-heavy
sensitivity is substantially higher. Larger reference-key diversity can increase
posted/output keys even with a fixed zero source population. A fresh current-year
profile is therefore required before full execution. A next implementation must
classify true GL-grain versus unresolved-party records before changing their
common key, and preserve identities/output controls under any new profile.
