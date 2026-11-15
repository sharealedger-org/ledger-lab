# Component pilot FY2003–FY2004, input specification

All activity is fictional and separate from observed VA data. Three components:
service delivery, supply support and independent background activity. A supplier
participates in the first two; a second supplier is unrelated background. Employee
records describe dated responsibilities, not payroll. Revenue remains agency/fund/
source grain. Shared project evidence is emitted only where specified; it is not
an allocation instruction. Private context IDs remain outside model-visible files.

`budgets.csv` contains synthetic USD envelopes, not verified VA financial totals.
FY2012 ACFR fixtures cannot bound these years without further source verification.
No summary residual is posted as an accounting adjustment. `slots.csv` specifies
stable authored event IDs, dates, relative weights and visible project evidence.
`identities.csv` is a supplied assignment registry; this pilot does not implement
the missing target-engine unique-ID allocation capability.

Generation sorts context/budget/slot records, emits weighted activity, sorts on
source assignment keys, merges the supplied registry, then sorts by process/entity/
date. Financial outputs go through existing APA/PO transformation and posting.
Employee evidence and event lineage are consumed by the pilot verification, not
represented as a completed employee financial engine or evaluated AI model.
