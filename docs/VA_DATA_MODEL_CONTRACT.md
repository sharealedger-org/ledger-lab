# VA Data Model Contract for Ledger Lab

**Status:** Working replacement for the legacy Universal Ledger fixture assumptions. Derived from the source workbook `VA Data Samples and Specs.xlsx`, now preserved in the Sharealedger conceptual-model repository. This is a lab contract for measured experiments, not a ratified Sharealedger standard.

## Purpose

Ledger Lab should use the Virginia data to test a better accounting and data model, not merely reproduce the original Universal Ledger demonstration. The first model must preserve the source business events, connect them to contracts and involved parties, and derive accounting and reporting outputs without making one chart of accounts the definition of the business.

The source workbook contains the relevant design material: VA source structures, payment and purchase-order layouts, Universal Journal fields, chart-of-accounts mappings, commitment/report structures, T-account examples, NIGP reference data, and VA-to-journal transformations.

## Historical UL demo baseline

The companion VA UL demo transcripts in the conceptual-model repository document the prototype
sequence that this replacement must explain before it changes:

1. Normalize inconsistent VA source files.
2. Partition and sort the normalized records for scalable processing.
3. Assign stable Vendor/Involved Party IDs from a master file, retaining the original source
	identity and creating explicit IDs for new parties.
4. Create Universal Journal records with the enhanced source attributes.
5. Post journals into Universal Balance records.
6. Join journal or balance data back to the Vendor Master for enriched analysis.
7. Produce trial-balance, top-party, consolidated, and drill-down reports.

This sequence is historical evidence, not a requirement to preserve the old implementation. The
new contract replaces implicit vendor-name identity with explicit Involved Party and Contract or
Instrument relationships, and it requires unmatched records and lineage to remain visible.
Hawk, first-letter partitioning, in-memory report tables, top-50 thresholds, and the SMS/RSC demo
are implementation or demonstration choices, not conceptual invariants.

## FPEM reference

The model is informed by the FPEM system built by Rakesh Kant and the Svayam Infoware team,
described in Kip Twitchell's [Introduction to FPEM: Financial Period End
Management](https://ledgerlearning.com/2020/04/30/introduction-to-fpem-financial-period-end-management/).
The article describes a data-structure-independent platform organized around Source Fields,
Registered Sources, Source Events, Rule Lists, and Accounting Rules. It can create associated
reference data and balanced journal entries from originating business events, maintain selectable
ledger layouts, generate temporal and analytical events, and produce reporting views over the same
rich data environment.

The article also describes a practical adoption path: adjunct reporting ledger, parallel ledger,
then book of record. Ledger Lab should use these as model and migration references, not as a claim
that it reproduces FPEM or has access to its implementation. FPEM's Spark and Drools technology
choices are separate from Ledger Lab's Scala/CKB measurement path; the lab should compare data
contracts and behavior before comparing execution performance.

The accompanying FPEM video transcript adds useful observed behavior. FPEM source definitions
describe record types, source fields, business events, and input layouts. Rules select records by
field values and can populate structures from source fields, constants, and logic constructs. In
the demonstrated payment rule, an `EV03` record creates a debit and credit journal pair together
with Arrangement, Involved Party, and audit-trail records. The system then exposes trial-balance,
balance-sheet, income-statement, Involved Party, and transaction drill-down reporting from the
same processed data.

These observations strengthen the replacement model's requirements for explicit event typing,
generated journal lineage, reference-data creation, audit records, and report drill-down. They are
reference behavior to reproduce or falsify in Ledger Lab, not an assumption that the lab already
implements FPEM behavior.

## McCarthy discussion reference

The added McCarthy discussion transcript is a partial dialogue overlay on the MSU presentation,
not a standalone specification. Together, the slides and dialogue reinforce a practical boundary
for the lab. The choice is
not between retaining every transaction for every report and maintaining only high-level balances.
The useful target is a measured combination of detailed source events, lower-level Contract or
Instrument Positions, and just-in-time aggregation for views that do not justify permanent
materialization.

The transcript also emphasizes that capture is not the same as useful data. The model must retain
where an event came from, how it entered the system, and which business process produced it. This
provenance is essential when reconstructing relationships across source systems or explaining a
derived position.

For the VA workload, the first replacement should therefore measure both sides of the boundary:

- detailed event and journal lineage for matched, unmatched, and generated records; and
- selected Contract/Instrument balances and report views, with their storage and reconciliation
	obligations made explicit.

This keeps the lab focused on a practical materialization decision rather than turning the model
into an all-or-nothing Universal Ledger claim.

## UL prototype expert-review reference

The [2019 expert review article](https://ledgerlearning.com/2019/09/24/shared-ledger-rd-results-a-recent-expert-review/)
and its full [Geneva/SAFR review recording](https://youtu.be/twissr4WFQU) document the prototype
that preceded this lab. The prototype explored a shared ledger as a reporting environment first,
then as a place for new adjustments and transactions, then as counterparty-specific shared
partitions, and eventually as a settlement environment.

The review also identifies the intended economic mechanism: shared transaction data can reduce
counterparty processing and reconciliation work, while balances remain a technical materialization
choice used to tune computation for periodic analysis. A shared ledger is not merely a shared
transaction file; it must include posting, balance derivation, rules, reference data, and reporting
behavior.

The prototype used historical Vagrant, Play, and Spark infrastructure. Those artifacts are valuable
historical evidence and demonstrate the design intent, but they are not the current Ledger Lab
measurement substrate. The present lab must reproduce the relevant data contracts and controls in
its declared Scala/CKB path before making performance comparisons.

## Source event types

The first fixture should represent these source event types explicitly:

### Payment

A payment record should retain or derive:

- source system;
- Contract or Instrument ID, using a PO number when one exists;
- Involved Party or vendor ID;
- legal entity or agency;
- event type and event ID;
- accounting date;
- currency and transaction amount;
- account, fund, object, and other source chart fields; and
- source-file and source-row provenance.

A payment without a matched PO must not be silently discarded. It receives an explicit unmatched or generated-instrument status and remains traceable.

### Purchase Order

A PO record should retain:

- Contract or PO number;
- Involved Party ID and party name;
- legal entity or agency;
- event type and event ID;
- order/accounting date;
- source system;
- product description;
- quantity and unit of measure; and
- source provenance.

The PO is a commitment or arrangement event. It is not a payment and must not be treated as a settled position merely because it exists.

### Revenue or receipt event

Revenue source records should retain their source-system identity, Involved Party, account or source category, amount, currency, accounting date, and provenance. Revenue mappings should remain separate from expense/payment mappings even when they later share the same ledger machinery.

### Generated events

The calculator may generate balanced journal events for accounting transformations, allocations, reclassifications, or other approved rules. Generated events must identify:

- the rule version;
- the originating source event or events;
- the Contract or Instrument affected;
- the generation run and snapshot; and
- whether the event is a movement, correction, reversal, or accumulation.

## Canonical journal record

The Universal Journal record is a processing representation, not the conceptual definition of the business. The initial Scala contract should preserve these fields where present:

- `source_system_id`;
- `instrument_or_contract_id`;
- `legal_entity_id`;
- `center_id`;
- `affiliate_owner_id`;
- `ledger_id`;
- `nominal_account`;
- `movement_flag`;
- `transaction_amount`;
- `unit_of_measure`;
- `statistical_amount`;
- `fiscal_period`;
- `business_event_or_journal_id`;
- `group_event_id`, `subgroup_event_id`, and `event_distribution_id` where the source provides a
	header/line/distribution hierarchy;
- `journal_line_number`;
- `accounting_date`;
- `source_currency` and `target_currency`;
- `journal_type`;
- `book_code_or_basis`;
- `source_event_type`;
- `source_record_id`;
- `rule_version` when generated; and
- `lineage_id` or equivalent source reference.

Field names may follow the existing Scala contracts where that preserves compatibility. The implementation must document any renaming or loss of source information.

## Event hierarchy

The source model may contain a hierarchy rather than one flat event row:

- an Arrangement or Contract anchors a Group Event, such as an order or settlement cycle;
- a Group Event contains Subgroup Events, such as order lines; and
- a Subgroup Event contains Event Distributions at the lowest processing granularity, such as
	tax, shipping, allocation, or accounting components.

Ledger Lab must preserve these relationships when they exist. An event-type code is a
classification of a record, not a substitute for the record's stable identity.

## Accounting interpretation

The model must keep these concerns separate:

1. **Source event:** what the VA system recorded.
2. **Contract or Instrument:** the relationship that connects related events.
3. **Involved Party:** the participant or counterparty.
4. **Reference data:** agency, fund, object, NIGP, account, and other classifications.
5. **Accounting rule:** how a participant projects the event.
6. **Journal lines:** the balanced processing output.
7. **Position:** the derived contract, resource, or participant state.
8. **Report view:** a selected perspective over those positions.

A VA COA value is a participant accounting mapping. It is not the universal meaning of the source event. Different projections may map the same source event differently while retaining the same event and instrument lineage.

## Reference data

The initial reference-data package should include, as applicable:

- agency and legal-entity crosswalks;
- fund and fund-detail mappings;
- object and expense mappings;
- source-system crosswalks;
- chart-of-accounts structure and account types;
- Involved Party/vendor data;
- NIGP codes and descriptions, subject to their licensing notice; and
- report and commitment categories.

Reference data must have a declared version or effective period. A change in a classification must be visible as a new reference generation; it must not rewrite the source event.

The NIGP workbook content carries a copyright warning. Do not redistribute the NIGP code table as an open dataset without confirming the applicable license. A fixture may use permitted samples or identifiers and document the external dependency.

## Required invariants

The first implementation must test:

- source events have stable IDs and provenance;
- related payment and PO records retain their relationship when matched;
- unmatched payments remain explicit and traceable;
- Contract or Instrument IDs are not replaced by nominal account codes;
- accounting dates and fiscal periods are distinct fields;
- generated journal lines identify their rule and source lineage;
- debit/credit or equivalent control totals balance for each declared book;
- source completeness is tested separately from journal balance;
- Involved Party attributes may be loaded for direct lookup rather than becoming a competing CKB sort path; and
- Contract and Resource processing records the chosen access strategy when their CKB sort orders differ.

## First measurable workload

The first VA workload should be deliberately small:

1. One PO matched to one payment.
2. One payment without a matched PO.
3. One revenue or receipt event.
4. One local chart-of-accounts mapping for each participant perspective.
5. One generated balanced journal transformation.
6. One instrument-level position and one report view.
7. Control totals, source lineage, and a clear unmatched-record report.

The output should include the existing Ledger Lab evidence bundle plus a short model report showing:

- input records and source IDs;
- matched and unmatched relationships;
- generated journal lines;
- ledger positions;
- mapping and reference-data versions;
- control totals; and
- resource and contract access-path choices.

## Execution boundary

The model contract is independent of a particular runtime. The first implementation remains in Ledger Lab's Scala financial path, with Bash orchestration and Python limited to preparation or inspection under the repository rules.

The conceptual-model repository owns the source interpretation and model contract. Ledger Lab owns the executable fixture, calculator, control evidence, and measurements. GenevaERS may later execute a compatible metadata/configuration package, but GenevaERS integration is not required to validate this first model.

## Success condition

The replacement is useful when the same small VA workload can be run reproducibly, balanced journal output can be traced to source events, unmatched source records are visible rather than lost, and a report can be regenerated from the declared instrument, reference-data, and rule inputs.
