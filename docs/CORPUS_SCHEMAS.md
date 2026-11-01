# Corpus Schemas and Reference Data

Measured 2026-10-31 from `data/corpus_profile/schema_catalog.csv`, `file_field_profile.csv`, the
files themselves and `data/aRef*.txt`. Counts and definitions of reason codes are in
[CORPUS_FINDINGS.md](CORPUS_FINDINGS.md) and [CORPUS_RECORD_IDENTITY.md](CORPUS_RECORD_IDENTITY.md).

Four distinct header schemas exist across the 367 profiled files. Within each family the header is
identical in every file (one `schema_id` per family), so there is no column-order drift to handle
**inside the files on this machine**. This contradicts
[DATA_ENHANCEMENT_PLAN.md](DATA_ENHANCEMENT_PLAN.md) section 1.1, which describes FY2003–FY2013 as
tab-delimited with Agency / Fund Detail / Sub-Program columns and FY2014–FY2016 as a different
column order. No file here matches that description. All 336 APA transaction files are
comma-delimited with the same 9 (expenditure) or 7 (revenue) columns. If an older format existed,
it is not in this corpus.

## Families

All APA files are comma-delimited, double-quote-quoted, start with a UTF-8 BOM, and use
`MM-DD-YY` dates. The eVA PO files are tab-delimited, `MM/DD/YYYY` dates (character encoding not verified; the profiler reads bytes as UTF-8), with a blank
first line before the header.

### `apa_expenditure` (168 files; schema `apa_expenditure-d80a1829`)

`AGENCY_NAME | AMOUNT | FUND_NAME | LOCALITY | OBJECT_NAME | PROGRAM_NAME | VENDOR_NAME | VOUCHER_NUMBER | VOUCHER_DATE`

- One file per month, FY2003–FY2016, 12 months each. Folder names change: `FY 2003 Expenditure`
  … `FY 2013 Expenditure`, then `All Expenditures for Fiscal Year 2014` … `2016`. Month tokens
  include `Sept`.
- `VOUCHER_NUMBER` is space-padded to a fixed width in 97,242,843 of 97,516,306 accepted rows. Raw
  value keeps the padding; a trimmed form is a `_norm` column. 14.28 million have leading zeros.
  Treat as a string.
- `VENDOR_NAME` is never blank in accepted rows. 641,016 values have leading or trailing spaces.
  Values such as `Payroll` are not vendors; the field mixes payees, payroll and aggregate labels.
- There is **no vendor identifier, agency code, fund code or object code** in these files. Every
  chartfield is a *name*. Joins to the reference tables are by name (see below).
- `AMOUNT` is signed decimal; 9,489,970 negative (9.7%), 464,764 zero.
- `LOCALITY` is a name (`VIRGINIA` in the sample rows).
- Declared row totals (`N rows selected.`) exist in the SQL*Plus-era files and all 263 match.

### `apa_revenue` (168 files; schema `apa_revenue-da2433fe`)

`AGENCY_NAME | AMOUNT | FUND_NAME | LOCALITY | SOURCE_TYPE | SOURCE | DEPOSIT_DATE`

Same folder and month structure as expenditure (`All Revenues for Fiscal Year 2014` …). No
vendor/payer field exists in this family.

### `apa_budget` (14 files; schema `apa_budget-42d44ef5`)

`AGENCY_NAME | AMOUNT | FUND_NAME | LOCALITY | GROUPING_CODE | PROGRAM_NAME | TRANSACTION_TYPE | BATCH_DATE`

- One file per fiscal year, `Budget FY20NN.csv`, FY2003–FY2016.
- Differs from the two transaction families: `GROUPING_CODE` (always a digit string with leading
  zeros, e.g. `0406`; keep as string), `TRANSACTION_TYPE` (e.g. `AGENCY TRANSACTION VOUCHER`), and
  `BATCH_DATE` instead of voucher/deposit date. No `OBJECT_NAME`.
- Rows are budget transactions, not balances. Signed sums across them are not a budget total (see
  CORPUS_FINDINGS).

### `eva_po` (17 files; schema `eva_po-02e8d547`)

`PONUMBER | AGENCY | ORDEREDDATE | VENDORCOMMODITYDESC | QUANTITYORDERED | PRICE | VENDORID | VENDORNAME | VENDORADDRESS | VENDORCITY | VENDORSTATE | VENDORPOSTALCODE | VENDORLOC_EMAILADDRESS | NIGPCODE | NIGPDESCRIPTION | UOMCode | UOMDesc | VENDORPARTNUMBER | MANPARTNUMBER`

Nineteen columns. Selected field behaviour over 18,081,738 accepted records:

| Column | Observation |
|---|---|
| `PONUMBER` | 4 blank. Treat as string; a `-Vn` suffix is a raw attribute. Shapes vary (e.g. `PCO1825381`, `EP2539756`, `A501_0001143903`). |
| `ORDEREDDATE` | 10 blank, 24 unparseable overall (all are counted, none dropped). |
| `AGENCY` | 8 blank. A name, not a code. |
| `QUANTITYORDERED`, `PRICE` | numeric in all accepted rows (non-numeric would be `BAD_AMOUNT`). 539,908 rows have zero extended price; 2,568 negative. |
| `VENDORID` | never blank. 17.6 million numeric-looking; 7.88 million with leading zeros. **Must be a string.** Contains the `999999999999` placeholder. |
| `VENDORNAME` | 70 blank. 13,987 have leading or trailing spaces. |
| `VENDORSTATE` | 11,625 blank. Non-two-letter values exist; the extractor blanks them (a deletion, not a repair). |
| `VENDORPOSTALCODE` | 16.59 million numeric-looking; 730,264 with leading zero. **String**, otherwise ZIPs starting `0` lose a digit. |
| `VENDORLOC_EMAILADDRESS` | 4.73 million blank. Contact addresses; treat as personal data in any release (see CORPUS_ACCESS.md). |
| `NIGPCODE` | never blank. NIGP *descriptions* are subject to a copyright notice (see below). |
| `VENDORPARTNUMBER` | 8.24 million blank. |

The PO files carry no payment, voucher or invoice field. There is no key in this corpus that links
a PO row to an APA expenditure row. That linkage is Codex's investigation and is not attempted here.

## Cross-family joins that the schemas do and do not support

| Join | Supported by the data? |
|---|---|
| APA expenditure ↔ APA revenue ↔ budget on agency / fund / program | By *name* only. Names are free text, not keys. |
| APA expenditure ↔ PO | **No shared key.** `VENDOR_NAME` ↔ `VENDORNAME` is a string match across systems with different normalization. |
| APA ↔ reference tables | By name, spot-checked (below). |
| PO `AGENCY` ↔ APA `AGENCY_NAME` | Both are names. Not checked here. |

## Reference data

`data/aRef*.txt` are comma-delimited with a BOM and a header. They are the DPB chartfield reference
tables. They carry **no validity dates** and no source or version. They are snapshots; whether they
are the version as of any particular fiscal year is unknown.

| File | Header | Rows |
|---|---|---:|
| `aRefAgencyCSV.txt` | `AGY_AGENCY_CODE, AGY_AGENCY_KEY, AGY_AGENCY_NAME` | 326 |
| `aRefFundCSV.txt` | `FNDDTL_FUND_DETAIL_KEY, FNDDTL_FUND_DETAIL_CODE, FNDDTL_FUND_DETAIL_NAME, FND_FUND_CODE, FND_FUND_NAME` | 10,645 |
| `aRefObjectCSV.txt` | `OBJ_OBJECT_KEY, 2-DIGIT OBJECT CODE, 2-DIGIT OBJECT NAME, 4-DIGIT OBJECT CODE, 4-DIGIT OBJECT NAME` | 472 |
| `aRefProgramCSV.txt` | `SPRG_SUB_PROGRAM_KEY, PFCN_FUNCTION_CODE, PFCN_FUNCTION_NAME, PRG_PROGRAM_CODE, PRG_PROGRAM_NAME, SPRG_SUB_PROGRAM_CODE, <blank>, SPRG_SUB_PROGRAM_NAME` | 2,486 |
| `aRefSourceCSV.txt` | `SRC_SOURCE_KEY, SRCCLS_SOURCE_CLASS_CODE, SRCCLS_SOURCE_CLASS_NAME, SRC_SOURCE_CODE, <blank>, SRC_SOURCE_NAME` | 2,275 |

The Program and Source files each have an unnamed (blank-header) column between the code and name
columns. A reader that rejects blank headers fails on them; the profiler reports
`blank_header_names` per schema.

### Agency aliases

Seven agency *codes* appear more than once, with different keys and different names, and three
*names* appear on two codes. This is the reference table recording renames and re-organizations
without validity dates. Evidence from the file itself:

| Code | Key → name |
|---|---|
| 128 | 279 → Virginia Veterans Care Center Board of Trustees; 371 → Virginia Veterans Care Center |
| 232 | 258 and 361 → Department of Minority Business Enterprise (same name, two keys) |
| 720 | 205 → Dept. of Mental Health Mental Retardation and Substance Abuse Services - Central Office; 406 → Dept. of Behavioral Health & Developmental Services |
| 740 | 233 → Southampton Reception and Classification Center; 327 → Southampton Work Center |
| 912 | 160 and 360 → Department of Veterans Services (same name, two keys) |
| 934 | 162 → Innovative Technology Authority; 397 → Innovation & Entreprenuership Investment Authority |
| 936 | 167 → Southeastern University Research Association Inc.; 398 → Jefferson Science Associates LLC |

Implications:

- A code is not a unique key. `AGY_AGENCY_KEY` is unique and is the row key, but the APA transaction
  files carry only the *name*, so the key cannot be recovered directly.
- Two keys sharing a code *and* a name (232, 912) are duplicates; the reason is not documented.
- Do not collapse by code. Where a code carries two names, an alias table needs a validity period
  for each, and the period is not in the data. Entries in any alias table must record `evidence`,
  `valid_from`, `valid_to` and may leave the dates `unknown`.
- Names contain a misspelling (`Entreprenuership`). That is a raw value; do not correct it in place.

### Fund

- `FNDDTL_FUND_DETAIL_CODE` is a 4-digit string with leading zeros (`0100`). 558 distinct codes
  across 10,645 rows; 166 of those codes map to more than one distinct fund-detail name.
- The reference table has no agency column. Fund-detail codes are reused across agencies, so the
  same code can mean different funds for different agencies.
- DPB documentation (cited in `SESSION_HANDOFF.md`) says fund-detail codes expanded from 4 to 5
  digits around 2016. The reference table here is 4-digit; no 5-digit form is present. **Do not
  join on fund code alone, and do not apply this table to later years without checking.**
- The APA files carry `FUND_NAME` only, with no code.

### Object, program, source

- Object: 472 rows, no duplicate 4-digit codes; two levels (2-digit category over 4-digit object).
- Program: 2,486 rows, three levels (function → program → sub-program); one duplicate
  sub-program code.
- Source: 2,275 rows, two levels (class → source). Not analysed further.
- Program information is preserved; the Scala mapping is not redefined here. Any discrepancy
  between these tables and the Scala `chartfield` mapping is not yet checked.

### Name-join spot check

For three months of expenditures (Dec 2002, Dec 2007, Dec 2013), exact case-insensitive match of
transaction names against the reference names, by row:

| Month | Agency | Fund (detail or fund name) | Object (4-digit name) |
|---|---:|---:|---:|
| Dec 2002 (FY2003) | 99.0% (245 of 252 distinct) | 99.9998% (204 of 205) | 99.8% (319 of 322) |
| Dec 2007 (FY2008) | 96.2% (251 of 258) | 99.9998% (246 of 247) | 99.9998% (326 of 327) |
| Dec 2013 (FY2014) | 99.7% (246 of 252) | 99.996% (310 of 312) | 100% (326 of 326) |

This is three files, not a coverage claim for fund and object. Agency was then run over every
APA file (next section).

### Agency observations over all APA files (`build_agency_alias_table.py`, `agency-alias-1`)

Run on all 350 APA files (expenditure, revenue, budget). `rows_observed` = 115,383,197, which
equals the sum of accepted APA rows in `file_profile.csv` (97,516,306 + 17,750,109 + 116,782).
Outputs in `data/corpus_profile/` (`agency_name_observations.csv`, `agency_alias_candidates.csv`,
`agency_alias_run.txt`; the run file records the digest of the catalog it used).

| Relation to `aRefAgencyCSV.txt` | Distinct names | Rows |
|---|---:|---:|
| `EXACT_REF_NAME` (case/space-insensitive) | 307 | 114,328,166 (99.1%) |
| `NORMALIZED_REF_NAME` (punctuation, "&", THE/OF/AND ignored) | 8 | 1,034,519 (0.9%) |
| `UNMATCHED` | 13 | 20,512 (0.02%) |
| Total | 328 | 115,383,197 |

Findings:

- **Eight punctuation variants.** For example `Department of Mines, Minerals and Energy` versus the
  reference `Department of Mines Minerals and Energy` (259,382 rows), and the large Virginia Tech
  Cooperative Extension name (654,352 rows). The match is by normalization, so the table marks it
  `NORMALIZED_REF_NAME`, not exact.
- **Renames across the period.** `Dept. of Mental Health, Mental Retardation and Substance Abuse Services - Central Office`
  is observed FY2003–FY2010 (76,907 rows) and `Dept. of Behavioral Health & Developmental Services`
  FY2005–FY2016 (86,659 rows). Both relate to reference code 720 (keys 205 and 406). The two names
  are **both present in FY2005–FY2010**, so the data shows a transition period, not a clean
  cutover, and does not say why (rename, dual reporting, or different agencies sharing a code).
  `valid_from`/`valid_to` therefore remain `unknown`.
- **Thirteen names not in the reference table**, almost all first seen FY2014–FY2016
  (for example `Fort Monroe Authority`, `Office of Children's Services`,
  `Secretary of Veterans and Defense Affairs`), plus `Office of the State Inspector General`
  (5,847 rows from FY2004) and `Department of Small Business and Supplier Diversity` (8,735 rows from FY2010).
  They look like entities newer than the reference snapshot, which fits a snapshot taken before them, but
  that is an inference. Several carry spelling errors in the source (`Accountablility`, `Instituition`);
  they are raw values and are not corrected.
- **Three names are ambiguous in the reference table** and cannot be resolved to one key from the
  name alone: `Department of Veterans Services` (keys 160, 360, code 912; 100,203 rows),
  `Department of Minority Business Enterprise` (keys 258, 361, code 232; 12,635 rows), and
  `Department of Motor Vehicles Transfer Payment` (keys 372, 407 with **different codes**, 510 and 530;
  18,971 rows). The APA files carry only the name, so these 131,809 rows cannot be assigned to a single reference
  key without outside evidence. A join on name would double the rows for them.
- **Eight reference agencies never appear** by name in any APA file, for example
  `Virginia Baseball Stadium Authority` (code 854) and the two capital clearing accounts. The
  reference table is a superset; absence from the APA files is not evidence they never existed.

The alias candidates file is evidence, not a decision. No alias is asserted, `valid_from` and
`valid_to` are `unknown`, and `observed_first_fy`/`observed_last_fy` are an observation window in
these particular files, not an organizational validity period.

## Not verified

- Whether the reference tables are valid for any particular fiscal year.
- The unmatched **fund and object** names, and whether they are renames, typos or deleted entities
  (agency was done over all files; fund and object were only spot-checked on three months).
- Whether the 13 unmatched agency names are new entities or aliases of existing ones.
- Whether `LOCALITY` ever has values other than `VIRGINIA`.
- The Scala chartfield mapping against these tables.
