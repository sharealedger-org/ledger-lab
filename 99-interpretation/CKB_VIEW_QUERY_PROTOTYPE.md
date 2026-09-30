# CKB View-Query Prototype Plan

**Status:** Proposed research plan on `work/99-reporting`; not implemented or verified.

## Aim

Test whether a user can ask for a reporting view in natural language, have AI translate the request into a constrained view contract, and have a deterministic CKB/view process scan immutable event or ledger files and stream a reproducible result. AI is the request planner and explanation layer; it does not calculate financial results.

The prototype does not add a database or make 99 a financial-processing layer. The 99 interaction layer may resolve a request against existing evidence or submit an explicit read-only query process. Any scan or aggregation is separately identified and measured, never hidden inside report interpretation.

## Architecture Claim

For a fixed input snapshot, a constrained view request can be executed by a declared ordered process with correct selection, aggregation, controls, bounded memory, and traceable output. Requests arriving during a scan can be served by a captured high-water mark and replay without omitting or duplicating the join-boundary record. Appended partitions after the captured mark do not silently alter a snapshot answer.

A passing legacy VA fixture alone is not evidence that the generalized 01-05 CKB contract is implemented. Before treating the prototype as CKB evidence, identify an actual ordered-stream consumer and downstream result consumer; otherwise label the execution as a legacy fixture probe.

## Existing UL Reporting Baseline

The current VA prototype's Step 5 `ViewSpec.csv` defines nine output cuts:

| Cut | Grain / attributes |
|---|---|
| `TOP50` | Top vendors by absolute spend, with vendor name. |
| `DETAIL_BAL` | Instrument × nominal account × period × ledger × book code. |
| `SUMMARY_BAL` | Agency × center × project × product × nominal account × period × ledger × book code. |
| `BY_INST_TYPE` | Instrument × account × period, with `instTypeID`. |
| `SUMMARY_BY_INST_TYPE` | Agency × account × period, with `instTypeID`. |
| `BY_NIGP_CLASS` | Account × period, with instrument type and NIGP class. |
| `SUMMARY_BY_NIGP` | Agency × account × period, with NIGP class. |
| `BY_GEO_REGION` | Account × period, with instrument type, NIGP class, and geography. |
| `SUMMARY_BY_GEO` | Agency × account × period, with geography. |

A recorded small Step 5 run read 126 ledger rows and emitted respectively 16, 64, 95, 64, 29,
29, 29, 29, and 29 data rows for these cuts. This is a historical fixture observation, not a
current production result or a scale claim. The specs are in [ViewSpec.csv](../data/ViewSpec.csv);
the implementation is the legacy [Step 5 Scala aggregator](../02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/dataAggregation.scala).

The current Scala module can load enabled view definitions, group ledger fields and VendorMaster
attributes, apply simple ledger/CAR equality filters, limit a top-N view, write CSV outputs, and
append a pivot-results record. Its execution is not a streaming CKB implementation: it loads all
ledger rows for a year into an in-memory buffer, then iterates that buffer once per view. The
existing detail-to-summary “proof” also builds both maps from the same loaded rows, so it is not an
independent reconciliation control. 99 must report these as legacy capabilities and gaps, not as
validated CKB or reconciliation evidence.

## VA Accounting Model in the New Workbook

The newly added [VA Data Samples and Specs workbook](../docs/VA%20Data%20Samples%20and%20Specs.xlsx)
contains an intended VA-to-Universal-Journal mapping that was not implemented by the current UL
prototype. Its `VA to UnivJE` sheet maps:

| Universal field | FY03-FY12 expense and FY12+ expense | Revenue |
|---|---|---|
| `legalEntityID` | `AGY_AGENCY_KEY` | `AGY_AGENCY_KEY` |
| `centerID` | `FNDDTL_FUND_DETAIL_KEY` | `FNDDTL_FUND_DETAIL_KEY` |
| `projectID` | `PROGRAM` | blank/default in the mapping |
| `accountID` | `OBJECT` | `SOURCE` |
| `transAmount` | `AMOUNT` | `AMOUNT` |

The mapping examples show expense project `2701` with account/object `56`. The current
`standardizeAndSort` path instead sets project from the object and, via the active
`VAAccountingRules.csv`, derives the expense account as `EXP+object`; it does not retain Program.
Revenue similarly uses project `0000` and derives `REV+source`, rather than using the workbook's
direct Source-to-Account mapping. Therefore the cardinality estimate below describes the
**workbook-intended model**, not current UL output. This is a material implementation gap, not a
minor naming difference.

The workbook's `New Unv Data Model` also sketches a Universal Balance with Involved Party,
Contract, Commitment, and Balance IDs; ledger/journal/book and chartfield dimensions; a fiscal-year
field; and `transAmountPer01` through `transAmountPer12`. Those 12 period amounts appear to be
measure slots, not 12 row-key dimensions. The mapping does not yet define the VA population or
reference domains for Contract, Commitment, and Balance IDs, nor fully settle whether Account IDs
are one shared numeric namespace or type-qualified by expense/object versus revenue/source.
Current `post.scala` instead includes `ldgrLedgerPeriod` in its match key and stores one amount per
row. These are distinct balance layouts and must not be blended into a single cardinality claim.

## Workbook-Intended Candidate Domains

Reference-file inspection gives these candidate values for the workbook mapping:

| Dimension | Candidate values | Evidence and caveat |
|---|---:|---|
| Instrument ID | 1,066 | 1,065 checked-in VendorMaster IDs plus the `0000000000` unknown-ID sentinel; fixture-scale, not the full runtime population. |
| Legal entity | 326 | Distinct `AGY_AGENCY_KEY` values in `aRefAgencyCSV.txt`. |
| Center / fund detail | 10,645 | Distinct `FNDDTL_FUND_DETAIL_KEY` values in `aRefFundCSV.txt`; add zero if the offset row participates in the cut. |
| Project / program | 2,486 | Distinct `SPRG_SUB_PROGRAM_KEY` values in `aRefProgramCSV.txt`; the FY03 fixture contains 28, with `2802` unmapped. |
| Account / object or source | 2,280 raw numeric values | Union of 472 object keys and 2,275 source keys, with 467 overlapping numeric IDs. Add zero for offsets. If the model preserves type-qualified namespaces, the upper candidate is 2,748 plus offset. |
| Fiscal year | 14 | FY2003-FY2016, the available public-data span. In the workbook's wide-balance sketch, 12 period amounts are measures within the year, not extra key members. |
| Ledger, journal type, book, product, currency/type | One or a few | Workbook mapping mostly specifies constants; product/default handling and any future multi-ledger/basis variants still need an explicit contract. |

Using only the candidate dimensions Instrument × Legal Entity × Center × Project × Account ×
Fiscal Year gives a dense independent-domain ceiling of roughly `2.94e17` cells for a shared
numeric Account namespace, or `3.54e17` if expense and revenue account namespaces stay distinct.
These are intentionally loose mathematical ceilings, **not** expected materialized row counts.
The actual account/project combinations are constrained by transaction and reference mappings;
the current VendorMaster is only a small checked-in sample; and the workbook's additional balance
identifiers remain unquantified.

The first FDS experiment should count **observed sparse support** under this workbook mapping, not
materialize a dense cube. For each selected chartfield grouping, record unique keys, rows, bytes,
and independent reconciliation controls. Include a separate comparison between the workbook's
12-wide period measures and the current long-form period-key ledger. Keep CAR pivots such as
`instTypeID`, `instNIGPClass`, and `instGeoRegion` as a separate axis, not as base chartfields.

## 99 Reporting Plan

### Phase 1: Evidence Catalog, No New Financial Scan

- Let the user select a completed `run_id` and one of its produced cuts or evidence artifacts.
- Show the nine existing cut definitions, their source generation, row counts, and availability;
	distinguish outputs actually present from cuts merely enabled in a spec.
- Answer only from immutable run artifacts and metadata. Link every displayed value to its source
	file, row/control identity, and run manifest. Mark missing perspective metrics or controls as
	unavailable rather than recomputing them inside 99.
- Permit deterministic comparisons of existing cuts across runs only when their input snapshot,
	rules, hierarchy versions, and view definitions are compatible.

### Phase 2: Constrained View Requests

- Expose an allowlisted request over the existing view catalog: named cut, run/snapshot, period,
	permitted filters, and top-N where defined.
- Have AI translate natural language to that request schema. Show the normalized request for
	confirmation when the interpretation is ambiguous; reject unsupported dimensions and measures.
- Initially serve materialized outputs from the selected run. Do not silently invoke a fresh scan
	or claim that a precomputed result is live.

### Phase 3: Explicit CKB/View Execution

- Only after a declared CKB/view process and downstream consumer are identified, submit supported
	read-only requests as separately named query processes.
- Capture input partitions/checksums, hierarchy version, high-water mark, view request, controls,
	process identity, scan counts, elapsed time, memory, spill, and output artifact.
- Prototype a request joining mid-scan and replaying to its committed high-water mark; assert the
	join-boundary record is included exactly once and later appends do not alter snapshot results.
- Compare shared-stream fan-out against a fresh scan per request. Report actual extra record work,
	buffering, memory, latency, and output costs.

### 99 Interaction Acceptance

For the first user-facing slice, a user can ask for or select a supported cut, choose a completed
run, see the exact normalized request and source generation, and receive a reproducible result with
links to its artifacts and controls. Unsupported query semantics, absent artifacts, incompatible
runs, or unverified controls must be stated explicitly. The interaction layer does not generate or
apply accounting entries; Reclass remains a separate event-generation workload.

## Request Contract

The first version should be allowlisted and declarative, not arbitrary model-generated SQL. Each request should identify:

- `request_id`, `run_id`, source snapshot and committed partition IDs/checksums;
- snapshot high-water mark, or explicit live-subscription start point;
- named view, allowed predicates, grouping dimensions, measures, and output format;
- hierarchy/reference-data version and perspective semantics: Switch, Recast, or Reclass;
- requested period/close boundary, materialization mode, and control expectations.

The validator rejects unknown fields, unsupported groupings, unavailable source attributes, and ambiguous temporal semantics before execution.

## Execution Semantics

### Snapshot request during a scan

Capture both the committed snapshot high-water mark `H` and the reader position `P` when the request is accepted. The view consumes eligible records after `P` through `H`, then wraps to the beginning and consumes the earlier prefix through `P`, including the join-boundary record exactly once. It then completes. If `P` is already beyond `H`, replay only the snapshot bounded by `H`.

A later append belongs to a later snapshot. Store a stable partition identity plus record ordinal; do not rely on a mutable file length or an offset whose meaning changes if the source is rewritten. The source prefix is assumed immutable, and committed partition manifests define visibility.

### Live subscription

A live view starts from an explicit committed offset and receives each later committed transaction once. It does not have a finite completion marker; cancellation, retention, restart, and deduplication behavior must be defined separately from snapshot queries.

### End-of-day close

An ordered end-of-day marker closes the view's period buffer only after every event through the declared cutoff is visible. Emit and checksum the extract, record its control totals and checkpoint, then continue or stop according to the subscription contract. Define how late/backdated events reopen a prior generation or create a later-period adjustment.

## Prototype Stages

1. **Freeze a small workload.** Use one immutable fixture, one declared perspective, one or two groupings, and known control totals. Record source partition identities and an input high-water mark.
2. **Define and validate a view request.** Start with a hand-authored request; add AI translation only after the contract validator exists. Retain the natural-language prompt, normalized request, and validation result as evidence.
3. **Execute a deterministic read-only view.** Use an explicit CKB/view process over the declared source partition. If no generalized CKB implementation is available, stop short of calling the legacy path CKB evidence and record the capability gap.
4. **Test joins and replay.** Add a view before scanning, midway through a scan, at EOF, and after an append. Verify snapshot high-water-mark behavior and exact-once treatment of the boundary record. Compare against an independent reference result for the fixture.
5. **Test multiple active views.** Route the same input stream to several allowlisted requests. Measure records read, per-view records considered, fan-out work, accumulator memory, output bytes, elapsed time, and any spill or additional sort.
6. **Test close and restart.** Emit an end-of-day marker, validate outputs and controls, inject a failure, then replay from the same immutable inputs and checkpoint contract. Prove there are no missing or duplicated records.
7. **Add the 99 interaction.** Present the request form and result through 99 with links to source controls and output artifacts. Report the query process separately from original financial compute and interpretation overhead.

## Required Controls and Evidence

- Input snapshot, partition IDs/checksums, hierarchy version, request ID, and high-water mark.
- Rows selected, rows scanned, rows emitted, per-group controls, and reconciliation result.
- Join-boundary and append-after-snapshot tests, including exact-once assertions.
- Per-process elapsed time, peak memory, spill bytes, output bytes, and active-view count.
- Failure/replay identity, checkpoint, and proof that published outputs are complete generations.
- Explicit classification of Switch/Recast as report semantics and Reclass as generated accounting events; this read-only prototype must not generate or apply Reclass SJEs.

## Guardrails

- No model-authored arithmetic, unrestricted SQL, or silent inference of hierarchy dates.
- No hidden rescan attributed to 99; replay is an explicit query process with its own process identity and cost.
- No database required for the first prototype. Files remain the source of truth; any later index or retained-state experiment is a separate materialization profile.
- No claim of production scale, generalized GenevaERS capability, or CKB placement until actual producers, consumers, controls, memory, and recovery are verified.

## Decision After Prototype

The prototype should answer whether shared streaming plus replay is functionally correct and what it costs relative to a fresh scan per request. Decide whether to pursue a started-task interface, retain materialized views, add bounded checkpoints, or stop based on measured work and the capability gaps it exposes.
