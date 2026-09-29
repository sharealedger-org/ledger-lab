# Virginia Accounting Classification

Ledger Lab now keeps a broad, source-backed vocabulary for Virginia expenditure classification in
[data/VAAccountingClassificationRules.csv](../data/VAAccountingClassificationRules.csv).

## Public Source

The vocabulary is derived from the checked-in Virginia APA object/source reference table:

```text
data/VARefObjSrcFixedLength.txt
```

The historical expenditure, revenue, and budget packages are available from the Virginia APA open
data portal used by the repository downloader:

```text
https://www.datapoint.apa.virginia.gov/
```

The downloader records the historical package contract in
[scripts/download_va_data.py](../scripts/download_va_data.py), covering FY2003 through FY2016.

## Source Dimensions

A FY03-FY13 expenditure record has the shape:

```text
agency, fund, object, program, vendor, amount
```

These fields have different meanings and must not be collapsed into one account expression:

| Source field | Role in the ARE | Example use |
|---|---|---|
| `agency` | Legal entity / organizational owner | Agency 267 |
| `fund` | Fund or funding dimension | Fund 1552 |
| `object` | Virginia expenditure classification | Salary, supplies, equipment, transfer |
| `program` | Program/subprogram dimension | Program 2701 |
| `vendor` | CAR lookup to Instrument ID | Vendor/instrument identity |
| `amount` | Source monetary event | Debit/credit amount |

The current prototype preserves `object` in `transProjectID` but derives
`transNominalAccountID` from `program`. That is a compatibility shortcut, not a validated
accounting interpretation. A rule-driven ARE should resolve the object through the VA reference
vocabulary, preserve program as its own dimension, and derive canonical accounting fields through
an explicit versioned rule.

## Classification Versus Posting

`VAAccountingClassificationRules.csv` is a vocabulary layer. It says what the VA reference class
means and gives a default accounting role. It does not by itself decide whether a class is eligible
for allocation, revaluation, consolidation, or a particular perspective.

For example, major class `11` is `PERSONAL SERVICES`. The object-level reference table then
contains more specific codes such as:

```text
EXP6    SALARIES
EXP5    EMPLOYEE BENEFITS
EXP243  EMPLOYER RETIRE CONTRIBUTIONS-DEFINED BENEFITS PRG
EXP253  ANNUAL EMPLOYER HEALTH INSURANCE PREMIUM
EXP265  SALARIES, CLASSIFIED
EXP270  SALARIES, OVERTIME
EXP321  SALARIES, INFORMATION TECHNOLOGY EMPLOYEES
EXP524  EMPLOYEE RETIRE CONTRIBUTIONS-DEFINED BENEFITS PRG
```

Those can support a `PEOPLE_AND_COMPENSATION` source classification without erasing the original
object account or program dimension.

## ARE Rule Boundary

The intended rule sequence is:

```text
raw VA event
  -> parse agency/fund/object/program/vendor/amount
  -> resolve object through VA reference table
  -> classify the event
  -> derive canonical account and dimensions
  -> resolve Instrument ID through CAR
  -> emit balanced SJE with rule identity and lineage
```

A future rule specification should identify at least:

- source system and record type;
- object/reference lookup;
- classification ID;
- canonical account expression;
- retained agency, fund, and program dimensions;
- allocation eligibility as a separate policy decision;
- rule-set ID, rule version, and effective dates;
- reject behavior when an object code is unmapped.

This keeps public-source meaning separate from later Finance, allocation, and perspective policy.
