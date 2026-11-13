# Unit Rate Sources — CKB Cost Model (Paper 1, Rev 2)

**Status:** `not retrieved`. None of the unit rates below has yet been pulled from a dated public source. The values in `03-ckb-cost-model.xlsx` (sheet `Model_Parameters`, cells B16 to B20) are assumptions. This document records what each rate needs and where to get it. Pull each price on a named date, record the region and tier, and keep an archived copy of the page.

The measured corpus constants (row counts, GB, CPU seconds, controls) do not depend on these rates. Only the dollar figures do.

## 1. Rates in the model

| Model cell | Rate | Current value | Kind | Candidate public source to pull | Status |
|---|---|---|---|---|---|
| B16 | Scan, per GB scanned | $0.02 | Assumed | Pay-per-scan query service list price (AWS Athena, per TB scanned): https://aws.amazon.com/athena/pricing/ . The conservative case in Paper 1 uses the published $5 per TB, which is $0.005 per GB. | `not retrieved` |
| B17 | Seek, per indexed lookup | $0.0001 | Assumed | No public source. Replace with a measured lookup time on the ledger store, or state it as a labelled assumption. | `not retrieved` |
| B18 | Storage, per GB-month | $0.023 | Assumed | Standard object storage list price (AWS S3 Standard, first 50 TB tier, one named region): https://aws.amazon.com/s3/pricing/ | `not retrieved` |
| B19 | Reconciliation, per master pair per month | $50 | Assumed | No public source. Needs a stated basis from practitioner evidence (for example, reconciliation staff time per pair). The conservative case in Paper 1 uses $0. | `not retrieved` |
| B20 | Ingest compute, per CPU-hour | $0.05 | Assumed | General-purpose on-demand compute list price (AWS EC2, one named instance type and region): https://aws.amazon.com/ec2/pricing/on-demand/ . Negligible at this scale. | `not retrieved` |

## 2. Query volumes (Model_Parameters B22 to B24)

1,000 annual, 500 sub-annual and 100 dimensional queries a year are assumptions. They have no public source. They should come from a stated basis (the practitioner's reporting calendar, or a count of statutory and management reports). Until then, Paper 1 presents the sub-annual dependence explicitly, because the monthly minimum holds only if sub-annual questions are asked.

## 3. Recording format for each pulled rate

- Provider, product, page URL, and region or tier.
- Price and unit, as printed.
- Date pulled, and the name of the person who pulled it.
- An archived copy (PDF or saved HTML) placed in the evidence folder.
- The cell in the workbook it feeds.

## 4. Shared rate card

`tracks/D-control/COMPARISON_PLAN.md` item 3 asks for one written rate card, applied to both the companion and the control. Use the same dated card for both. Then the cost comparison does not depend on which vendor's prices were chosen.

## 5. Conservative case (for the paper, until this is complete)

Paper 1 uses two settings chosen to favour the alternatives: scan at $0.005 per GB and reconciliation at $0. Under those settings the monthly point stays the lowest-cost configuration, at $16.29 per year. The workbook does not yet hold this as a scenario. Adding it as a second column in `Model_Parameters` is an open item in `TODO.md`.
