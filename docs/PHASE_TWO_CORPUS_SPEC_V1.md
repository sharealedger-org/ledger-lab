# Phase 2 fictional corpus specification v1

2026-11-16; preparation from `38a41c6`. Status: `partially_verified` for the
implemented source slice and actual financial/report consumers. Full v6 completed
generation, source inspection, one financial/report workflow and the read-only
classification/recurrence audit. Population/recipe-seed separation passed the
corrected pilot and cross-seed checks. The broad AIM capabilities
remain requirements, not model implementations.
Entry: [current execution evidence](../data/test_evidence/phase2_generation_20261116/README.md)
and [historical design sample](../data/synthetic/phase2_inspection_v1/README.md).
Authority: [AIM requirements](PHASE_TWO_AIM_CORPUS_REQUIREMENTS.md) and
[generation design](PHASE_TWO_GENERATION_CKB_PROPOSAL.md). This specification
supersedes the older mixed observed/companion recommendation for this workload.

## Population and completeness

`fictional-phase2-v1` is a new, self-contained modeled population. All modeled
actors are fictional. Its two classes are `wholly_generated` and owner-approved
`reassigned_activity`: the latter reuses eligible zero-vendor expense amounts,
classifications and calendar-translated dates with constructed suppliers. Original
source descriptions/locators are private provenance, not public actor identities
or historical-model evidence. Financial consumption requires no external lookup
or hidden local execution dependency. Empirical aggregates calibrate workload
and monetary envelopes; they do not supply fictional relationship truth. Complete
means every declared source family, reference version, opening
state, generation parameter, private truth family and control is present, or its
absence is an explicit observation condition. It does not mean every identity or
outcome is known. Original VA data/evidence remain immutable.

Use primary fiscal labels SY01–SY14 (July–June), USD integer cents, UTC timestamps
and declared fictional scopes. ISO backing fiscal years 2031–2044 serve existing
adapters only; calendar.csv maps them explicitly. Preserve a mapping from
calibration FY2003–2016 without claiming historical equivalence. The earlier
hand-authored inspection covers January–June 2032. Calendar generation covers
leap days, fiscal boundaries, quiet periods and late observations. No current clock
selects a reference version. Finite full-package horizon and observation cutoff
must be frozen before scale generation; outcomes beyond cutoff are censored.

Namespaces are `V` vendors, `E` employees, `R` receipt parties, and `A` agencies.
Summary-grain receipts use an agency key and no invented party. Unknown vendor
identity stays empty with a reason in public source evidence; raw financial
adapters retain their established zero sentinel. Local system IDs differ from canonical
private generation IDs. IDs must not encode mechanisms or labels. The package
includes masters, histories and scope/currency definitions; it requires no VA
master at runtime. Participant overlap, concurrent obligations, partial outcomes
and unrelated background activity are in the bounded slice. Generalized mergers,
splits and rich settlement engines remain future extensions. Explicit population
identity is separate from the recipe seed: changing activity draws must retain
actor IDs; changing population deliberately selects another identity namespace.

The initial scale ladder is hand inspection, then a deterministic 100-component
adapter fixture, then profiled 10,000-component generation, then a separately
admitted full package. These are planning sizes, not measured capacities. Full
row count is selected from the calibration map after width/key/scratch profiling;
273,380,488 reporting input rows are not a source-event generation target.

## Data families and exact inspection contracts

All sample files are UTF-8 headered CSV, one physical line per row, no embedded
commas/newlines; empty fields mean unknown/not applicable as specified by kind.
Dates are ISO dates, instants ISO UTC, money signed integer cents; no floating
point. IDs are unique within their family; version records have distinct IDs.
Headers in the sample are the v1 inspection schemas, not canonical SJE schemas.
A new executable adapter must validate headers/types and reject invalid records.

| File | Grain and semantics | Intended consumer; current state |
|---|---|---|
| evidence/parties.csv | One local party record; role, name and location are evidence, not truth keys | Source adapter; not implemented |
| evidence/records.csv | One immutable local process record/version; amount semantics depend on kind | Process-specific Scala adapters; not implemented |
| evidence/proposals.csv | One issued tentative output; cost/receipt/target are expectations | Cutoff-safe evidence export; not implemented |
| evidence/references.csv | One issued reference version with effective interval and scenario parent | Temporal export/scenario view; not implemented |
| evidence/scenarios.csv | One conditional world; weights explicitly absent | Isolated scenario consumer; not implemented |
| evidence/openings.csv | One opening scope/currency cash observation | Conditional cash consumer; not implemented |
| evidence/modernization.csv | Optional fields over the same records in a separate source profile | Matched profile exporter; not implemented |
| private/relationships.csv | One precise relationship predicate with inferability and truth | Controlled inspection only; no evaluator |
| private/outcomes.csv | One outcome measure/resolution per proposal at cutoff | Controlled inspection only; no reconciler |

`records.local_ref` names only a supported same-system predecessor; empty means
no such link. `proposal_ref` is populated only when the operational record carries
that reference. `business_ref` is an ordinary observable reference; blank does
not authorize joining by private context. `supersedes` replaces the named record
in interpretation after availability; immutable originals remain. Invoice amounts
are total obligation versions, not additive cash payments. Payment/receipt amounts
are realized cash movements. Staffing is responsibility evidence, not payroll.
Delivery amounts are absent: delivered units are in `quantity`, with `unit`.
Records and proposals both retain effective and availability times; proposals
also retain issue time and target date. No cash, cost or outcome is inferred by
adding heterogeneous record kinds. No financial posting is claimed for this CSV.

References use half-open effective intervals. At cutoff T select only versions
issued and available at or before T, then apply effective date and scenario.
Reject overlapping applicable versions without a declared supersession. Baseline
never reads a child scenario version. Future recast is analytical classification,
not reclass accounting. Source-quality variants add observation fields, never
change activity, money, target dates or outcome truth.

## Scenario coverage and sampling

Include procurement/service delivery, facilities, recurring supply, independent
staffing, agency-grain receipts and independent payer activity. Freeze a mechanism
matrix before generation: integrated/disconnected/partially modernized evidence;
clean/ambiguous/missing identity; timely/late/disputed/canceled/partial/unobserved
outcomes; shared dependency versus incidental overlap; stable versus shifted
regimes. Cross these selectively and publish coverage counts, rather than assert
that a tiny sample covers the Cartesian product. Mechanism frequencies and
monetary envelopes are invented assumptions until qualified empirical calibration
exists. Keep negative cases from independent mechanisms, not just corrupted
positives. Hold out whole mechanism families and connected groups for possible
future research; do not call this public inspection package a blind evaluation.

## Generation topology and implementation admission

Logical owner 01 source preparation; 02 provides external ordering/controls.
G1 common key `(population, private_component)` merges definitions, memberships,
dated rules; emits entity-addressed instructions and private truth separately.
Determining-key sort/merge resolves process-local identity and dated routing with
explicit ambiguous/unmatched statuses. G2 sorts `(process, namespace, entity,
effective_date, precedence, stable_id)` and emits source/history versions. Knowledge
time export needs another declared order/merge when effective order is insufficient.
No universal component key enters evidence. Multiple source bridges require actual
sort boundaries; two conceptual generation orders are not a two-pass total claim.

Proposed bounds: at most 64 participant slots and 256 current rule/reference
versions per context/entity; fail on overflow, do not silently split. Stream event
history. External sorting starts with the reporting precedent of 100,000-row
chunks/fan-in 16, subject to measured byte caps for the new schema. These are
proposed limits, not tested generator settings. Retain instructions, assignment
results and source partitions for first review; publish new immutable attempt
roots only after controls. Failure outputs cannot become next-run inputs.

No existing parser is asserted compatible with these new headers. Trace the pilot
producer and TTL/ARE consumers before choosing adapters; unsupported employee,
receipt, proposal and outcome families remain source evidence until named Scala
consumers exist. Financial consumption must emit canonical balanced SJE groups,
with source/version/rule lineage, then reach actual posting and reporting. Risk
scores, scenario residuals and private resolutions do not create journals.

Per-process manifests must carry revision, seed, input hashes, parameter/rule
versions, partition order, rows/bytes, CPU/wall/RSS, state high-water marks, sort,
spill and retained bytes, controls and downstream receipts. Separate generation,
financial engine, orchestration and validation costs. No Python is proposed;
Scala generates/processes, Bash orchestrates only.

Before implementation: trace actual producers/consumers; run unchanged baseline
and cheapest adversarial shared-vendor/two-context fixture. Then check reorder
invariance, duplicate IDs, same-date ties, quiet reference changes, orphan links,
cutoff leaks, oversized keys, matched-profile invariance, monetary controls and
actual downstream consumption. Assess target GenevaERS gaps before measured claims.
Full-scale admission additionally requires current disk/time estimates and bounds,
calendar, anchor-ordered calibration, uniqueness registry, bounded merge and
manifest-driven controls. No reporting repeat, Q2 model evaluation, model/feature
freeze, manuscript edit, Control access or publication belongs to this slice.
