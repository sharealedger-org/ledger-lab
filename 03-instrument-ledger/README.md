# Instrument Ledger Layer

This layer names the canonical state represented by the Foundation processing substrate.

It is the stable state boundary between Foundation mechanics and SJE-generating engines:

```text
02-foundation
  journal schemas, CKB, sort contracts, controls
        |
        v
03-instrument-ledger
  Universal Journal history, Instrument Ledger balances,
  CAR-linked state, replacement masters and audit state
        |
        v
04-engines
  read SJE structures and generate SJE partitions
```

The Instrument Ledger is not a domain perspective and does not aggregate Finance, Tax, MIS,
Risk, or Liquidity outputs. It preserves the canonical instrument-level economic state from
which those perspectives can be derived.

Instrument Ledger state and perspective pivots may be emitted as views inside a CKB process. A
new physical pass is required only when the next operation needs a different sort order or its
working state exceeds the available memory contract.

## Responsibilities

- Maintain the Instrument ID anchor and balance-bucket grain.
- Preserve Universal Journal/SJE history and lineage.
- Represent replacement master files produced by CKB processing.
- Keep CAR/effective-dated attributes joinable to ledger state.
- Expose the SJE and ledger partitions consumed by calculation engines.
- Provide control totals proving that balances are reconstructible from supporting events.

## Non-Responsibilities

- Source parsing and accounting translation belong to `01-transformation`.
- Generic CKB mechanics and sort contracts belong to `02-foundation`.
- History-dependent SJE generation belongs to `04-engines`.
- Aggregation-only Finance, Tax, MIS, Risk, and Liquidity outputs belong to `05-perspectives`.
