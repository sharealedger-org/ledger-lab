# Synthetic Elimination and Multi-Currency Inputs

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
