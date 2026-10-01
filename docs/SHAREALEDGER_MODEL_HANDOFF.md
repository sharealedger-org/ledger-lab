# Sharealedger Model Handoff

**Purpose:** Give Ledger Lab a short implementation entry point for the better VA accounting/data model developed in the Sharealedger conceptual-model repository.

**Status:** Working implementation handoff. This is not a ratified standard or product commitment.

## Source material

- Detailed lab contract: [VA Data Model Contract](VA_DATA_MODEL_CONTRACT.md)
- Conceptual framework: `sharealedger-conceptual-model/model/open-accounting-framework.md`
- Practical roadmap: `sharealedger-conceptual-model/model/practical-implementation-roadmap.md`
- VA source workbook: `sharealedger-conceptual-model/model/source/VA Data Samples and Specs.xlsx`
- Historical FPEM, UL, McCarthy, and MSU presentation materials are preserved in the conceptual-model repository.

## First implementation target

Adapt the existing Scala fixture path to process a small VA workload containing:

1. one Purchase Order matched to one Payment;
2. one Payment without a matched Purchase Order;
3. one Revenue or Receipt event;
4. explicit Involved Party and Contract or Instrument IDs;
5. Group Event, Subgroup Event, and Event Distribution lineage where available;
6. one generated balanced journal transformation;
7. one Contract or Instrument Position;
8. one report view; and
9. control totals and an unmatched-record report.

## Required behavior

The implementation must preserve:

- source event identity and provenance;
- source-system and accounting dates separately;
- explicit unmatched records;
- Contract or Instrument relationships;
- participant/reference-data mappings;
- generated journal rule and lineage identity;
- balance controls separate from source completeness controls; and
- the selected physical access strategy for Contract and Resource processing.

Involved Party reference data may be loaded for direct lookup. It should not become a competing CKB sort path unless the workload demonstrates that it cannot remain memory-resident.

## Evidence to emit

The first run should produce or identify:

- normalized source records;
- generated journal lines;
- ledger or position output;
- matched and unmatched relationships;
- source and generated control totals;
- rule and reference-data versions;
- lineage from report values back to source events; and
- process, storage, and memory measurements.

The existing Ledger Lab evidence and interpretation contracts remain authoritative for run reporting.

## Deliberate non-goals

Do not begin by implementing:

- the full Universal Ledger vision;
- shared counterparty settlement;
- a complete ERP application;
- AI model training;
- GenevaERS integration; or
- every VA source variation.

Those are later experiments. The first goal is a reproducible, balanced, traceable calculation on a small workload using the improved model.

## Completion test

The handoff is complete when an independent contributor can run the named fixture, explain each output record, identify the source event and rule that produced it, distinguish a missing match from a zero amount, and reproduce the control totals.
