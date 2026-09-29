# Synthetic Elimination and Multi-Currency Inputs

These files are synthetic capability fixtures. They are deliberately separate from the public Virginia
baseline under `data/` and must not be presented as observed VA transactions.

They exercise three questions:

1. Can explicit counterparty rules identify elimination candidates without relying on amount-only matching?
2. Can a report derive book-currency amounts from effective-dated rates without storing generated conversion SJEs?
3. Can a virtual currency such as BTC be carried as a high-precision source currency while USD remains the book currency?

## Files

- `intercompany_events.csv` contains paired expense/recovery and revenue events plus a non-eliminating external transfer.
- `intercompany_rules.csv` identifies the approved counterparties, account prefixes, and match groups.
- `multicurrency_events.csv` contains USD, EUR, and BTC source events with book currency USD.
- `fx_rates.csv` contains effective-dated USD, EUR, and BTC rates.

The expected processing order is:

```text
source events + rules/rates
    -> elimination candidates and conversion calculations
    -> reconciliation evidence
    -> approved elimination/conversion SJE only if the chosen materialization profile requires it
```

The synthetic fixtures are not a claim that Virginia public files contain these counterparties,
EUR transactions, or BTC holdings. They are controlled inputs for testing accounting mechanics and
materialization choices.
