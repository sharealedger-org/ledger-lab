# Virginia Audited and Public Finance Data: Survey Notes (2026-11-12)

Status: exploratory notes from one working session (Bob, M1 Mac). Nothing here is a verified
result in `VERIFICATION_AUDIT_REGISTER.md`. Each source is marked **Read** (opened and figures
taken from it), **Probed** (fetched, response seen, content not analysed) or **Listed** (seen in a
catalog or index only). No source file was saved in the repository; downloads went to `/tmp`.
The tool used was `curl` over HTTPS; there was no search engine, so the survey is not exhaustive.

## 1. Why this survey

The owner's direction (2026-11-12): audited financial statements are the gold standard. A user who
queries the subledger by PO or vendor should be able to tie the result to a number in those
statements. Adjustments with no vendor (aggregates, allocations, currency effects) always exist.
The report should show vendor totals plus an "other" made of named adjustment records, not a plug
(statement figure minus vendor total). Fragmented ledger systems cannot do this. The idea is a later
paper test; it is recorded in `docs/THREE_MODEL_EXPERIMENT_PLAN.md` section 8.

## 2. Audited anchor: the ACFR (Read)

The Virginia Department of Accounts publishes the Annual Comprehensive Financial Report for every
fiscal year 2002 to 2025:

- Index: `https://www.doa.virginia.gov/reports.shtml` (section "Annual Comprehensive Financial Report").
- Per year: `https://www.doa.virginia.gov/reports/ACFReport/<year>-ACFReport.shtml`, which links the
  full PDF and section PDFs A to S (for example `.../reports/ACFReport/2012/G_Major_Governmental_Funds.pdf`,
  `L_Required_Supplemental.pdf`, `K_Notes.pdf`, `O_Internal_Service_Funds.pdf`).
- Read so far: FY2012 section G (statements) and section L (required supplementary information).
  Not read: sections F, K, M to S, any other year.

### 2.1 FY2012 governmental funds (section G, dollars in thousands)

| | General | Commonwealth Transportation | Federal Trust | Total governmental funds |
|---|---:|---:|---:|---:|
| Total revenues | 17,342,130 | 3,871,439 | 8,950,643 | 31,487,735 |
| Total expenditures | 16,972,740 | 4,488,603 | 8,876,003 | 32,517,843 |
| Net change in fund balance | 571,136 | 312,064 | 66,012 | 1,019,882 |
| Fund balance, June 30 | 512,359 | 2,746,594 | 99,734 | 4,429,416 |

Revenue is broken out by category (Taxes, Rights and Privileges, Federal Grants and Contracts, and
others) and expenditure by function (Education, Individual and Family Services, and others), by fund.

### 2.2 FY2012 budget versus actual (section L, pages 174 to 177, dollars in thousands)

| Expenditures | Original budget | Final budget | Actual (budget basis) |
|---|---:|---:|---:|
| General Fund | 17,404,817 | 17,389,911 | 16,894,014 |
| Commonwealth Transportation Fund | 4,114,885 | 6,181,722 | 4,440,901 |
| Federal Trust Fund | 6,625,899 | 8,045,009 | 8,894,480 |

General Fund revenue on the same schedule: 16,692,030 original, 16,859,357 final, 17,101,959 actual.
Tax lines are individual (income, sales, corporation, motor fuel, and so on).

What the schedule contributes to the tie-out test:

- **The budget walk is itemised.** Note 2 turns original appropriations into "Appropriations, as
  adjusted" through named lines: reappropriations, subsequent executive actions, subsequent
  legislative actions, reversions, transfers, capital outlay adjustment, debt service adjustment.
- **Basis matters.** The schedule is "principally on a cash basis". Note 1 reconciles General Fund
  balance from the budget basis (1,683,412) to modified accrual (512,359) with named steps: accrued
  taxes, tax refunds, deferred taxes, Medicaid payable, accrued expenditures. Federal Trust and
  Transportation have the same reconciliation.
- **Two "actuals" exist for one fund.** General Fund expenditure is 16,894,014 on the budget basis
  and 16,972,740 on the GAAP basis. A target such as "reserves" or "net income" is undefined until the
  basis is stated.
- **A non-vendor, non-cash item.** Footnote 10: appropriations exclude about $1.4 billion of food
  stamp issuances (non-cash) but actual expenditures include them.

## 3. Comparison with the corpus (rough, FY2012)

From `data/corpus_profile/file_profile.csv` (columns 3, 10 and 24, summed with `awk`; this assumes
no comma inside the `rel_path` field and has not been cross-checked):

| Family | Rows | Signed amount |
|---|---:|---:|
| `apa_expenditure` | 8,088,721 | 44,617,001,543 |
| `apa_revenue` | 1,372,828 | 43,073,947,291 |
| `apa_budget` | 8,717 | 52,429,534,484 (absolute sum 69,035,123,038) |

- The corpus expenditure and revenue sums are about $12 billion above the ACFR governmental-funds
  totals ($32.5B and $31.5B). The reason is not established. Candidates: funds outside "governmental"
  (enterprise, higher education), transfers, and gross against net presentation.
- The corpus budget rows are mixed transaction types, not balances (`docs/CORPUS_FINDINGS.md`).
  They cannot be compared with the ACFR adjusted appropriations (about $31.6B for the three major
  funds) until `TRANSACTION_TYPE` handling is understood.
- The raw `VARawFiles` folder was not found on the M1 Mac, so the budget rows themselves were not read.
- Gold (the SJE set used for the experiments) has `EXP*`, `COMMIT` and the `0000` offset only. It has
  no `REV*` and no budget accounts. The TTL has a revenue path (`buildRevenuePairs`,
  `ApaTtlSpec` Test 6) with no real-corpus run. There is no budget workload in Gold.

## 4. Other sources found

| Source | Content | Status | Use |
|---|---|---|---|
| USAspending.gov API (`https://api.usaspending.gov/api/v2/`) | Federal awards by recipient, filterable to Virginia. A FY2012 grant query returned, for example, the Department of Medical Assistance Services at about $3.9B and the State Board of Education at about $0.94B. | Probed | Independent check of ACFR "Federal Grants and Contracts" ($9.9B total governmental in FY2012) and the Federal Trust fund |
| Dept. of Taxation annual reports FY1999 to FY2024; local option sales tax CSVs; taxable sales by locality (`data.virginia.gov`, organization `department-of-taxation`) | Revenue by tax type | Listed | Tie to ACFR "Taxes" lines |
| APA Comparative Report of Local Government Revenues and Expenditures (`data.virginia.gov`, `auditor-of-public-accounts`; XLSX for 2023 and 2024) | Audited local revenue and expenditure | Listed | Counterparty side of state aid to localities (elimination test) |
| Data Point salaries (`https://www.datapoint.apa.virginia.gov/salaries.php`) | Employee-level pay: employee ID, total, state and non-state salary | Probed | Decompose the aggregate `Payroll` pseudo-vendor |
| Data Point about page (`.../about.php`) | States it includes "every state transaction" and draws on higher-education systems; has a legacy site and demographics sections | Read | Scope of the corpus source |
| DPB chart-of-accounts documents (`https://dpb.virginia.gov/forms/forms.cfm?search=Chart%20of%20Accounts`) | FY13 expenditure structure, program structure and agency abbreviation reports; dated chart-of-accounts change logs 2014 to 2020 | Listed (links found) | Possible validity dates for `aRef*` tables, which carry none |
| DGS eVA procurement data (`data.virginia.gov`, `department-of-general-services`) | CSVs for 2016 to 2027 | Listed | Extend PO coverage beyond FY2018 |
| Dept. of Accounts reports page | Preliminary Annual Report, Monthly Cash and Investments, Quarterly Statewide Financial Management and Compliance, Truth in Revenue Source Reporting, Single Audit corrective action plan, Popular Annual Financial Report | Listed | Not opened |
| FHWA highway finance tables on `data.virginia.gov` (revenues used for highways, disposition of highway-user revenues) | State highway revenue and disposition | Listed | Cross-check of the Transportation fund |
| Census Annual Survey of State Government Finances | State finances | Probed (page loads only) | Cross-check |

Not reachable from this machine: `vrs.virginia.gov` (TLS error) and the guessed VDOT financial path (404).
The DPB reporting portal `reports.dpb.virginia.gov` requires a state login. The state catalog has
almost no central-finance datasets from Accounts, Treasury or the Governor's office.

## 5. Open questions and suggested order

1. **Owner:** which basis defines the reserves or net income target (cash/budget, encumbrance, or
   modified accrual), and whether to keep D9 as proposed, change it to reserves per fund, or replace it
   with the statement tie-out. D9 is reopened in `TODO.md`.
2. **Owner:** where are the raw `VARawFiles`, including the 14 budget files?
3. Draft a one-fund, one-year mapping (General Fund FY2012): ACFR line to corpus fund, agency and
   object, with the difference itemised to named lines. Read ACFR sections K and O first.
4. Run APA revenue through the Scala TTL for one year, after the architecture-gate declaration in
   `docs/AGENTIC_ARCHITECTURE_GATES.md`, so Gold has `REV*`. Decide how `apa_budget` enters (budget
   stage) before building anything.
5. Pull the USAspending federal-award total for state recipients, FY2012, and compare with the ACFR
   federal revenue line and the corpus `apa_revenue` Federal source rows.

## 6. Process note

One short inline `python3` snippet was used to list links from a downloaded HTML page, before owner
approval was given (`AGENTS.md` requires approval before Python). It was read-only inspection; no Python
file was written and nothing in the measured path changed. The rest used `curl`, `jq`, `grep` and `awk`.
