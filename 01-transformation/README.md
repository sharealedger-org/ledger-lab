# Transformation Layer

This is the first execution layer. It converts raw source or product-system records into canonical Universal Journal/SJE structures.

```text
raw product/source records
        -> Technical Transform Layer (TTL)
        -> Accounting Rules Engine (ARE)
        -> balanced SJE partitions
```

## Responsibilities

- Parse source formats and normalize technical fields.
- Validate source records and isolate technical rejects.
- Resolve Instrument ID and effective source references.
- Apply accounting rule specifications.
- Perform the accounting perspective shift from a source transaction to debit/credit SJE lines.
- Assign business-event, journal-line, source, rule, and provenance identifiers.
- Emit balanced SJE partitions for Foundation sorting and CKB processing.

The ARE is documented in [are/README.md](are/README.md). The current VA implementation combines much of this layer with external sorting in `02-foundation/va_pipeline`; the conceptual boundary is established before implementation is split.

## Non-Responsibilities

- CKB sorting, master-file replacement, and canonical state mechanics belong to `02-foundation`.
- Instrument Ledger state and replacement balances are represented in `03-instrument-ledger`.
- History-dependent calculation engines belong to `04-engines`.
- Finance, Tax, MIS, Risk, and Liquidity aggregation belongs to `05-perspectives`.

## Output Contract

Every accepted source event produces one or more SJE lines with:

- stable business-event/journal ID;
- journal line number;
- Instrument ID;
- debit/credit or offset designation;
- accounting dimensions and amount;
- source event and source system identifiers;
- rule-set and rule version;
- balanced-group control total.

The output is an SJE partition, not a perspective report and not a directly updated balance.
