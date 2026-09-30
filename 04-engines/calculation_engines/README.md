# Calculation Engines

Calculation engines operate only on SJE/Universal Journal structures. They read SJE partitions
and generate new balanced SJE partitions; they do not emit aggregated perspectives.

An engine may run as a CKB section and pipe generated SJEs into a later section. Generating a new
SJE partition does not by itself require a new physical pass.

Initial classes:

- FTP and funding cost.
- Credit reserves and impairment.
- Currency revaluation.
- Consolidation and eliminations (legacy VA FSP classification; target processing is source- and perspective-dependent).
- Allocation divisors and allocated transactions.

## Booked Currency Revaluation

The initial booked-revaluation slice is implemented in `CurrencyRevaluation.scala`. It reads a
declared opening ledger partition, effective-dated prior/current rates, and versioned rules. For
eligible foreign-source/book-currency balances, it applies the VA layout contract:

```text
revaluation amount = booked balance × (current rate − prior rate)
```

It rounds at the declared book scale, emits a balanced SJE group to the affected balance and the
configured offset account, externally sorts the records with the existing posting key, and leaves
the output for the existing Step 3 poster to apply. The synthetic one-period fixture reproduces the
VA spec's 559.47 × 0.02 = 11.19 example. Domestic balances are not revalued.

The fixture demonstrates a booked SJE and downstream posting on the legacy VA substrate. It does
not establish generalized GenevaERS CKB placement, rate governance, missing-rate operations,
backdated walk-forward behavior, or production-scale memory/runtime behavior.

The VA Step 7 `consolidation.scala` module is a legacy balance-level implementation that pairs
posted agency balances. It is not the target CKB implementation contract. In the target design,
ARE generates intra-unit entries before CKB and those SJEs enter its common flow. Inter-unit
elimination is a source-selected, report-scope sum view in that same CKB path. Both record grains
use the same CKB/view code; grain does not create a separate engine.

Each engine must declare whether it consumes only today's inputs or requires historical state,
identify its generated-event class, preserve source lineage and Instrument ID when present (or the
declared GL-grain identity otherwise), emit balanced transactions at the declared grain, and return
outputs to the Foundation CKB apply-back path. It must not invent Instrument IDs for GL-grain rows.
Perspective aggregation belongs in `05-perspectives`.
