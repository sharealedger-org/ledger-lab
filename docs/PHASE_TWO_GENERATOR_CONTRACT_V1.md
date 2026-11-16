# Phase 2 generator executable slice v1

2026-11-16. Builder contract after the 22-test baseline and APA/PO trace in the
execution plan. Workload `fictional-phase2-v1`; Scala source preparation only.
Current variable/two-class refinements below supersede the prototype fixed counts
and short instruction layout. Combined pilot/admission and full v6 generation,
financial/report controls and the read-only classification audit passed. No
financial engine implementation is replaced. See the permanent full-v6 receipt.
G1 context/year order emits 30 instructions in the original fixed prototype:
12 cash expenses, 12 receipts, 4 commitments and 2 responsibility events. Current
variable mode bounds each financial family at 24 events plus two responsibility
events (at most 74 instructions per context/year); manifests record actual counts.
Context envelopes use
integer cents, proportional calendar slots and exact last-slot remainders.
No official financial-anchor tie-out is asserted; calibration is workload shape.
The full profile is 10,000 components/FY2031–44, pending resource admission.

Inputs: bounded headered key/value config (seed, components, firstYear, lastYear,
expenseCents, receiptCents, commitmentCents), self-contained reference templates
and bounded synthetic history recipes. Monetary envelopes are per component/year.
Config row/byte cap 32 rows/16 KiB. Dates/fiscal boundaries are explicit.
Current parser accepts exactly the seven required keys plus optional `variability`
and `population` (7–9 rows, 16-KiB limit). Population defaults to
`phase2-fictional-population-v1`; accepted full/profile configs declare it explicitly.
It determines canonical and process-local actor IDs. Recipe `seed` determines
activity variation and event identities, independently of actor allocation.

G1 emits generation/instructions.csv, headerless, with fields:
`sort_key,source_key,event_id,effective_date,available_date,family,cents,visible_ref,private_context,fiscal_year`.
Determining-key order sorts by encoded source key/date/event. A sorted registry
`encoded_source_key,source_key,process,entity_id,name,template_id` resolves keys by
streaming merge. Then entity order sorts
`process,entity,effective_date,event_id`; resolved rows are
`entity_key,process,entity_id,name,template_id,event_id,effective_date,available_date,family,cents,visible_ref,private_context,fiscal_year,source_key`.
Keys hex-encode UTF-8 fields with `!` separators. Hash-derived event IDs do not
contain component labels; external duplicate checking is mandatory before publish.
Registry identifiers use private deterministic namespace assignments; shared vendor
slots are explicit. No global event/entity set or whole-lifetime buffer.

External sorting uses SortEngine spillChunk/mergeSpills with bounded 16-reader
multi-level merges, chunk <=100,000 rows, per-line <=16 KiB, <=100,000 run paths.
Only current context and current registry row are live. Generated raw sources
are partitioned fiscal-year/month with <=400 concurrent writers across 14 years.
Private lineage is separate. Metrics count physical sorts/merge levels, rows,
bytes and elapsed time; /usr/bin/time supplies process CPU/RSS outside JVM.
Initial profile materializes all boundaries; no pipe profile comparison claimed.

Financial recognition: EXP are cash disbursements, REV are actual agency-grain
receipts, PO are commitments with their existing separate accounting treatment.
No invoice, proposal, staff plan, private outcome, conditional recast or risk score
is independently posted. Output uses the pilot's exact 9/7-column APA CSV and
19-column PO tab format, existing VendorMaster schema and canonical existing ARE
rule templates. Every emitted financial source has an expected source-row locator,
amount, entity policy, date and rule in source_lineage.csv. Existing TTL/ARE ->
39-column SJE -> merge -> posting -> expanded report are the financial consumers.
No employee financial adapter is introduced. Distinct employee/payer namespaces
remain in source evidence and are not misrepresented as vendor Instrument IDs.

Rich lifecycle evidence initially follows the static inspection schema. Its named
consumer is the new Scala inspection/cutoff checker, not the financial engine or
an AIM evaluator. Later output expansion must retain effective/availability/issue/
target/cutoff distinctions, negative cases, outcome status and optional matched
source-quality fields. Scenario recasts remain analytical reference evidence.

Controls: external event/registry uniqueness; context envelope conservation;
exact source locator count; two balanced SJE legs per locator with correct date,
entity, source, rule/version and retained document; posted/report reconciliation;
cutoff exclusion of later knowledge; manifest counts; hashes; paired profile
invariance. Reject invalid dates, duplicate/missing identity or unsupported schema.
Actual engine validation uses Scala checkers, not Bash accounting. Failed attempts
remain unpublished; completion requires atomic new-root publication and checks.

This is prototype input preparation under existing GERS-001/002/003 limitations.
It does not close target-runtime gaps. New implementation begins not_audited until
its real downstream checks pass. Quiet master changes, shared vendors, absent
identity, summary-grain receipts and harmless lookalikes are required fixture cases.

## Rich source producer/consumer refinement

`PhaseTwoLifecycle` emits exact inspection headers declared as constants for
proposals, operational records and private outcomes. Additional families are party
records, optional modernization correlations, future reference/scenario versions,
opening cash and a private mechanism inventory. IDs use independent purpose hashes;
private context IDs do not appear in evidence. Due dates are target/schedule dates.
The operational commitment is tentative source evidence, not an additional PO
posting. Invoices are interpreted versions, not additive disbursements. Canceled
completion may retain sunk costs; unobserved completion does not erase cash events.
Component expected cost intentionally differs from realized cash/invoice totals;
these differences are invented mechanisms, not fitted VA distributions.

`PhaseTwoCheck.sources` is an actual bounded consumer of the new source evidence.
It validates manifest hashes, counts and missing-outcome statuses and exports an
explicit allowlist at supplied UTC availability cutoffs. It does not traverse
private truth or export optional integrated fields into disconnected snapshots.
Exports retain known future schedules/references; a future effective date does not
make a proposal unknowable before that date. Scenario reference applicability is
still separate from knowledge availability. It does not estimate risk or reconcile
outcomes using an AIM model. `PhaseTwoCheck.financial` externally sorts expected
source locators and actual SJE lineage, then checks exactly two sides/amount/date/
entity/document/rule/version per source. Report controls independently consume
posted ledger outputs. These consumers have distinct receipts; writing a source
family alone is not a completed financial engine capability.

## Calibrated generation refinement before full freeze

Optional bounded annual table: exact header is `PhaseTwoCalibration.header`, at
most 28 rows/64 KiB, EXP/REV USD only with complete coverage of configured years.
It is copied into the immutable package. Context envelopes are determined from
that table in year/family order, divided across configured components with exact
integer-cent remainders, then emitted through the same determining/entity orders.
No population-wide map is introduced: at most 28 annual controls are resident.
A published source hash, original net amount, scale fraction and rounding residual
accompany every envelope. Source inspection sums actual emitted source amounts by
fiscal year/family and checks those controls; per-source SJE checks extend the
conservation to real financial processing. Independent output contexts, dated
memberships and private party crosswalks make the generating account inspectable
without exposing a universal key to historical consumers. Component types are
private recipe classifications; coarse financial source classification remains
separate. Operational cancellation cancels the tentative deliverable, not every
annual financial commitment or cash settlement.

Final source-sort hardening before full freeze: chunks obey both 100,000-row and
32-MiB payload limits, with at most 16 merge readers and 100,000 run paths.
Record peak buffered bytes, and verify output row/byte conservation in a separate
source-preparation read after each sort. Internal partitions require LF-terminated
records. This verification traversal is physical preparation work, not a financial
engine scan. Actual source/financial inspection uses bounded year/family totals;
private context definitions, membership intervals and role crosswalks are retained
for self-contained truth inspection and excluded from cutoff exports.

**Resumed two-class implementation:** Source evidence v2 uses explicit SY dates
through SyntheticCalendar and a retained ISO backing/calendar map. Canonical raw
financial adapters retain their established date encodings. Vendor and other actor
identities use StablePartyIdentity, shared with the separately versioned Phase 1
upgrade; no sequential public vendor IDs. Reassigned activity is defined in
PHASE_TWO_REASSIGNED_ACTIVITY_CONTRACT.md, including five routed instruction
attributes, source eligibility, a bounded bottom-hash sample and combined annual
budget conservation. Prior fixed counts describe the prototype; a manifest must
supply new class counts before a combined workload is admitted.

**Bounded variability recipe v2:** Optional config `variability,component-variability-v2`
selects persistent component-size weights plus annual variation, an integer-shape
Gamma/Poisson mixture explicitly truncated to 1..24 records, lognormal amount
weights, seasonal dates and separate bounded arrival lags. All parameters are
synthetic assumptions. Small year/family denominator/remainder controls allocate
exact calibrated annual cents over components; per-component weights allocate
those cents over its bounded events. Cardinality manifest replaces prototype fixed
30/28-event assumptions. EMP stays two dated role/independent-assignment records.
No default/refund model, probabilistic risk claim or AIM fitting is introduced.

**Bounded source-family completion:** Variable recipe mode distinguishes service
orders/acceptance, facility work orders/inspection and supply orders/delivery/
consumption evidence. Inventory consumption is quantity evidence without stock
valuation or generated journals. A separate treasury source case supplies fixed-
rate opening-contract terms and forty semiannual contractual payments over twenty
synthetic years, known at the historical endpoint. It is an explicitly separate
analytical scope with no debt adapter/recognition in the current financial pass.
Cash plans in the operating scope are assumed monthly schedules conditioned on
annual controls, not forecast results. Their exact headers and actual availability-
cutoff consumer are in TreasurySourceCase/PhaseTwoSchemas/PhaseTwoCheck. No interest,
valuation, portfolio-risk or long-term forecasting model is claimed or evaluated.
