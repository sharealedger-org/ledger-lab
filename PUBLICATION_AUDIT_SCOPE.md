# Publication Audit Scope

This repository is a runnable staging copy for the proposed public
`sharealedger-org/ledger-lab` repository.

Included:

- Public-facing README, project brief, and quickstart
- Curated Markdown design and specification documents
- Sanitized `Universal Journal Instrument Ledger Model v0.7.xlsx` workbook
- Apache License 2.0
- External runtime-data layout instructions
- Scala pipeline, orchestration scripts, sort specifications, and sanitized fixtures

Intentionally excluded:

- Raw Virginia expenditure and purchase-order datasets
- Generated outputs and logs
- Prototype session logs and VM artifacts
- Other spreadsheets and binary documents
- The local Git history of the source repository

The workbook retains its analytical sheets and sample models, but direct vendor
emails, street addresses, and local authoring metadata were removed or replaced.
The full runtime dataset remains external to Git and is supplied through
`--data-root` or `UL_DATA_ROOT`. The staging repository includes the small
fixtures needed for development and smoke-test work.
