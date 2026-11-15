# Synthetic Elimination and Multi-Currency Inputs

## Reclassification reference history v1 (owner authorized 2026-11-14)

New input-generation workload, not a financial engine or perspective. A compiled
Scala generator will produce deterministic dated reference attributes (agency,
fund, object, program, source), SAL, CAR/party relationships and hierarchy edges.
Official VA publications informed the scenarios; their URLs and the chosen
assumptions are provenance, never runtime inputs or validity requirements.
No network lookup, downloaded PDF, real-reference ambiguity resolution, whole
database snapshot, random seed or machine-current date is needed to regenerate.

The versioned package `reclass_history_v1` will contain start-dated rows sorted by
business key then effective start, a change journal with separate recorded dates,
declared lookup-date cases and expected selected versions/paths. Main histories
use start dates only: select the greatest start not later than the requested date;
a later start supersedes an earlier one. Duplicate key/start combinations and
cycles/orphans at change boundaries must fail validation. No explicit end-date
semantics are claimed until a target-runtime boundary contract is verified.

The two families are reference/entity attribute and relationship histories, and
hierarchy child-parent histories. Multiple labels may map to one key; the same
label may occur for distinct contextual keys. Neither pattern alone implies a
date sequence. Generated keys use a visibly synthetic namespace and do not re-key
the measured VA corpus. Baseline date, change dates, scale and supplied RUNDATE
are fixed generation parameters. Record source inspiration and every invented
date as a synthetic assumption. Include a leaf move, unchanged leaf with changed
ancestor, attribute-only change, party/role change, late-recorded reference change,
before/on/after dates, fixed-date and RUNDATE lookups, no-prior-version result and
a reference-only change affecting an instrument with no new activity.

Physical boundary: standalone bounded fixture preparation, outside all measured
financial processes. No new SJE/posting algorithm, money calculation or reclass
journal generation is authorized by this data slice. Future 04 reclass processing
must emit the existing balanced SJE contract and consume these histories through
an explicit adapter; 05 Switch/Recast remain aggregation-only. Generated expected
lookup rows are fixture oracles, not measured engine results. Verify byte-identical
regeneration, all declared temporal/path expectations, positive hierarchy integrity
and negative duplicate/cycle/orphan fixtures. Keep the unchanged APA baseline tests.

Existing elimination and currency fixtures follow below.

These files are synthetic capability fixtures. They are deliberately separate from the public Virginia
baseline under `data/` and must not be presented as observed VA transactions.

They exercise separate presentation-time and booked-currency questions, plus elimination candidates:

1. Can explicit counterparty rules identify elimination candidates without relying on amount-only matching?
2. Can booked currency revaluation generate balanced SJEs from opening ledger balances and prior/current rates, then apply them through the existing poster?
3. Can a virtual currency such as BTC be carried as a high-precision source currency while USD remains the book currency?

## Files

- `intercompany_events.csv` contains paired expense/recovery and revenue events plus a non-eliminating external transfer.
- `intercompany_rules.csv` identifies the approved counterparties, account prefixes, and match groups.
- `multicurrency_events.csv` contains USD, EUR, and BTC source events with book currency USD.
- `fx_rates.csv` contains effective-dated USD, EUR, and BTC rates.
- `booked_fx_opening_ledger.csv`, `booked_fx_rates.csv`, and `booked_fx_rules.csv` form the booked revaluation workload. The opening book balances and rate changes reproduce the VA spec's `$559.47 × 0.02 = $11.19` example and include an EUR loss and a domestic-currency control row.
- Run `bash scripts/run_booked_fx_revaluation_smoke_test.sh` to generate balanced revaluation SJEs, sort and post them, verify ledger conservation, and produce canonical evidence plus a 99 interpretation report.

The presentation-time and elimination fixtures remain distinct from the booked revaluation flow:

```text
source events + rules/rates
    -> presentation-time currency calculations or elimination candidates
    -> report/reconciliation evidence

opening booked-currency ledger + prior/current rates + effective rules
    -> Scala currency-revaluation engine
    -> balanced, sorted CURRENCY_REVALUATION SJEs
    -> existing Step 3 poster
    -> updated booked balances + reconciliation evidence
```

The synthetic fixtures are not a claim that Virginia public files contain these counterparties,
EUR transactions, or BTC holdings. They are controlled inputs for testing accounting mechanics and
materialization choices. The booked fixture tests one rate interval; it does not validate backdated
walk-forward revaluation, production rate governance, scale, or generalized GenevaERS CKB placement.

### Generated reclass pack and reproducibility

The [reclass history pack](reclass_history_v1/README.txt) is now generated by
`bash scripts/generate_reclass_history.sh --out NEW_ABSOLUTE_DIRECTORY --instruments 32`.
Its six histories cover reference attributes, SAL, parties, CAR owner relationships,
contextual aliases and hierarchy edges. `schema_contract.csv`, `assumptions.csv`,
`generation_config.csv`, `change_events.csv` and the source-inspiration register
make all choices explicit. The default contains 207 history rows, 14 lookup cases,
six hierarchy cases, a quiet-instrument context and three invalid hierarchy variants.
The bounded Scala validator consumes the actual CSV contracts and verifies temporal
selection, change lineage, referential integrity, roots and cycles at every change
boundary. It is a fixture validator, not a measured financial lookup engine.

Official publications are optional inspiration records. Their absence or website
failure does not invalidate these synthetic records or prevent regeneration.
Source-inspired dates in fictional scenarios are still synthetic data; the quiet
parent move explicitly adopts a reported action date as an invented effective date.
No real VA agency/fund/party identity or historical crosswalk is asserted. Future
SJE/reclass processing, balance transfers, backdated walk-forward and Switch/Recast
corpus measurements remain unimplemented by this input-generation slice.
