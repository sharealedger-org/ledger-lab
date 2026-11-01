# Vendor identity investigation - 2026-10-31

Status: partial investigation. Public specifications establish a concrete identifier bridge, but no public historical bridge extract or validated PO/payment linkage has been obtained. No financial code or source data was changed. No outreach was sent.

## Questions and cheapest checks

Claim tested: historical PO VENDORID is a tax identifier and unrelated to payment-side identifiers. Cheapest falsifiers: inspect the local source dictionary, inspect official Cardinal extract fields, and compare exact historical identifiers against public PO files. These checks concern source semantics and linkage feasibility, not measured financial execution; no engine compile or smoke run was needed.

Reproduce from ledger-lab with Python 3 (standard library only):

    py -3 scripts/investigate_vendor_identifiers.py --specs

The script emits JSON to stdout. It does not write source files or output raw vendor IDs. --specs reads three public spreadsheets over HTTPS in memory. The default local run scans FY2016 and FY2018 PO files plus EV02/EV06 in the historical Box folder. Paths and years can be overridden with --data-root, --historical-root, and --years. The saved aggregate output is VENDOR_IDENTIFIER_EVIDENCE_2026-10-31.json; it contains input/specification SHA-256 hashes and exact spreadsheet cell references. Remote specification hashes identify the versions actually read, not guaranteed historical applicability.

## Findings

### 1. The local PO dictionary says DUNS, not TIN

In data/VAPOData/data-dictionary.xlsx, worksheet sheet1 row 8 explicitly maps VENDORID (VARCHAR2, length 15) to "Vendor DUNS #". This contradicts the prior handoff's shape-based tax-ID hypothesis as the default documented interpretation. The dictionary has no independently established acquisition date/source URL, so its applicability to every year remains partially verified. Do not relabel every value as a validated DUNS: placeholders, alphanumeric values, malformed rows, and possible agency-local identifiers remain.

A historical university manual independently describes TIN and DUNS as different supplier information and says the nonregistered-vendor DUNS field may contain a local G# identifier. This supports investigating mixed semantics rather than inferring tax IDs from nine-digit shape:
https://fiscal.gmu.edu/wp-content/uploads/2013/10/eVAManual.pdf

### 2. AP962 explicitly documents the missing namespace bridge

Official spreadsheet:
https://www.cardinalproject.virginia.gov/sites/default/files/AgencyInterfaceLayouts/AP962-Vendor-Data-Extract-Agencies.xlsx

Its VENDOR_ADDR record provides:

| Field | Documented meaning | Fixed positions in this layout |
|---|---|---|
| VENDOR_ID | Cardinal vendor identifier | 9-18 |
| ADDRESS_SEQ_NUM | Vendor address sequence | 19-23 |
| EFFDT | Effective date | 24-33 |
| ADDRESS3 | Contains VLIN | 302-341 |
| COUNTY | Contains DUNS number | 412-441 |

COUNTY is not geographic county in this extract. This is the strongest new finding: the documented schema can connect a DUNS-bearing PO identifier to VLIN and Cardinal vendor ID while retaining address identity. The main vendor record also describes parent/child vendor relationships; do not collapse legal entities or ordering locations merely because their names resemble each other.

This is a schema bridge, not possession of an open crosswalk. AP962 describes current and future dated addresses; a present-day extract must not be assumed to reconstruct historical addresses or identities. Public data availability, population coverage, and actual field quality are unknown.

### 3. Payment and voucher specifications retain usable identifiers

AP705 Payment Reconciliation Extract:
https://www.cardinalproject.virginia.gov/sites/default/files/AgencyInterfaceLayouts/AP705-Payment-Recon-Extract-Agencies.xlsx

- PAYMENT_TBL.REMIT_VENDOR identifies the remit vendor.
- PAYMENT_TBL.ADDRESS3 represents VLIN for voucher payments.
- PYMNT_ADVICE.PAY_DOC_ID identifies the voucher/report/advance; the notes explicitly allow multiple transactions consolidated onto one payment.
- INVOICE_ID identifies the invoice on voucher payments.

AP965 Voucher Extract:
https://www.cardinalproject.virginia.gov/sites/default/files/AgencyInterfaceLayouts/AP965-Voucher-Extract-Agencies-File-Layout.xlsx

- Voucher records identify agency business unit, voucher ID, invoice ID, and address sequence.
- ADDRESS3 contains VLIN based on the remit vendor/address sequence.
- VOUCHER_LINE.DESCR may contain goods/services description or the associated PO ID; this also passes to the GL journal-line reference field.

Thus a better direct-link search target is voucher-line description / GL journal-line reference, not only VOUCHER_NUMBER. Actual population of this free-text field remains unverified, and it is not present in the APA file schema recorded in the prior handoff.

### 4. Exact historical ID overlap is substantial, but proves no matching method

Independent local scan:

| Quantity | FY2016 | FY2018 |
|---|---:|---:|
| Parsed PO records | 1,713,138 | 880,819 |
| Records with missing/extra fields | 26,999 | 15,354 |
| Nine-digit VENDORID values, record count | 1,499,858 | 750,476 |
| Two-letter/seven-digit values, record count | 72,244 | 71,465 |
| 12-nine placeholder, record count | 114,127 | 43,547 |
| Distinct historical EV02/EV06 IDs present, including placeholder | 128 / 128 | 102 / 128 |

There are 127 distinct nonplaceholder historical IDs, all present in FY2016; 101 appear in FY2018. EV06 itself has the same 128 distinct IDs. These are exact string overlaps, not externally authenticated business identities. The scan includes malformed rows in the raw-field counts and flags them; field positions on those rows may be unreliable. Namespace overlap does not establish an independent payment source: the 2020 author may have assigned PO-side IDs after a name-based match. Payment-side provenance remains unknown.

Our parsed row counts differ slightly from the earlier temporary probes (FY2016 +12, FY2018 +6). Our name-ambiguity counts also differ substantially from the handoff because normalization/parsing/placeholder treatment differ. Do not combine the two sets as one validated result. Bob should resolve parser and denominator differences before corpus publication.

### 5. Bob's historical amount probe reproduces

Using Decimal arithmetic, sum qunatity * unit_price by (poNumber, ip_rec_id, org_unit_cd) in EV02. Compare EV06 by (ip_rec_id, org_unit_cd, txn_amt):

- 583 PO lines produce 248 PO groups, with zero amount parse failures.
- 196 of 248 payments have an exact candidate.
- 13 matching payment rows have multiple PO candidates.
- Two payment rows carry the 12-nine placeholder.

Agreement with the earlier count is reproduction of a candidate rule only. It does not validate the selected pairs, explain the remaining 52, or establish payment-side origin for the IDs. No precision, recall, or substitution label is established.

### 6. Historical boundaries materially limit modern specifications

The official 2019 Cardinal history says all CARS agencies migrated by February 2016 and Cardinal became the Commonwealth's system of record at the start of FY2017:
https://rga.lis.virginia.gov/Published/2019/RD238/PDF

Therefore modern Cardinal layouts cannot simply be projected across FY2003-FY2016. Ask separately about legacy CARS identifiers, conversion crosswalks, and Cardinal-era extracts. FY2016 is a transition case.

The Box folder was enumerated recursively: it contains historical workbooks, presentations, and EV CSV files, but no separately named SQL or source-extract scripts. The large March 2020 workbook contains the EV datasets as sheets; it is not yet established as their upstream source. An attempted deeper workbook inspection was stopped before completion, so no source-method conclusion follows.

## Implications for Bob's corpus work

- Preserve raw VENDORID and separate documented namespace from validated identifier status. Record the dictionary's DUNS description and its uncertain temporal provenance.
- Keep party, ordering location, remit location, and agency-local identifiers separate. Store identifiers as strings, never inferred numeric tax IDs.
- Do not overwrite name collisions or treat the 12-nine placeholder as a party identity.
- Keep vendor identity links separate from transaction links; vendor equality cannot be a prerequisite for discovering vendor substitution.
- Retain source/schema versions and malformed-row dispositions. Reconcile our parser differences with earlier figures before promoting coverage claims.
- No Parquet conversion is required or proposed by this investigation.

## Next investigation priorities

1. Locate any public historical AP962-derived directory or DUNS/VLIN/Cardinal conversion extract; validate real rows and dates before building a crosswalk.
2. Investigate agency-specific public voucher/journal exports containing line description/reference, VLIN, or remit vendor ID, with FY2016/FY2017 transition coverage made explicit.
3. Inspect the APA purchase-card exports for actual identifiers and PO references; they have only been located, not profiled.
4. Inspect selected historical workbook metadata/formulas through bounded reads to recover the EV producer or mapping logic.
5. If public access fails, prepare a narrow, reviewable request for historical non-sensitive identifiers (DUNS, VLIN, Cardinal vendor IDs, names, address sequences/effective dates, voucher/PO references). Do not send a request without owner authorization. Do not request tax identifiers when non-tax bridge identifiers suffice.

Conclusion: the earlier negative result should be narrowed to "no validated public historical extract obtained." A documented DUNS-to-VLIN-to-Cardinal bridge exists in official schema, and an additional potential PO reference exists on voucher lines. Neither is yet a proven usable public-data join.
## Follow-up: observed public PO-to-VLIN bridge (2026-10-31)

**This supersedes the schema-only status above for a small PO-side sample.** The payment-side join remains unknown.

### Public records found, not just specifications

The Virginia CKAN datastore exposes richer PO representations for the same fiscal years as the local text files:

- FY2016 resource: 4f72e337-db1a-4dfc-b4b9-1096a56cc7b2
- FY2018 resource: 9b5b3b80-8edc-453b-960f-0a7203aa9353
- API: https://data.virginia.gov/api/3/action/datastore_search
- Dataset: https://data.virginia.gov/dataset/eva-procurement-data-2016

The public schema includes Entity Code, full Order #, Order Line Number, VLIN ID, eVA ID, vendor location, order status, line total/change, requisition submission/approval dates, receiving date, contract fields, and SWaM flags. The local legacy text files lack many of these fields. Their absence locally is not evidence that they are unavailable publicly.

FY2016 returned 1,882,100 total rows and FY2018 returned 1,946,652. These differ from local snapshot counts; do not replace or union the extracts without reconciling snapshot/version and scope differences. The sample demonstrates overlapping records, not equivalence of the complete datasets. Order-status and receiving fields are not validated settlement evidence; SWaM flags require temporal interpretation, and line-total changes do not by themselves establish a complete revision history.

### Reproduced sample and candidate crosswalk

Run from ledger-lab:

    py -3 scripts/probe_public_vendor_bridge.py --spcc-schema

Saved evidence: PUBLIC_VENDOR_BRIDGE_EVIDENCE_2026-10-31.json. The probe selects the first well-formed record for each of 12 distinct nonblank legacy vendor IDs per year, including the all-nines placeholder as a negative control. This is a convenience sample, not a random sample or coverage estimate.

It queries the exact full PO number, retaining revision suffixes. Candidate lines must also agree on normalized agency, NIGP identifier, and Decimal quantity/unit price. Vendor names are checked separately, not used to select matches. It refuses classification if a response is truncated. No payment records participate in this match.

| Result | FY2016 | FY2018 |
|---|---:|---:|
| Sampled legacy records | 12 | 12 |
| Matched line, one nonblank VLIN/eVA-ID pair, nonplaceholder legacy ID | 11 | 11 |
| Matched line but missing VLIN/eVA ID; legacy ID is placeholder | 1 | 1 |
| Conflicting candidate identifier pairs | 0 | 0 |

One FY2016 line signature has two matching rows with the same identifier pair. This is an identity candidate, not a unique line-identity proof. The old text files do not carry the public order-line number. The 22 accepted sample observations are also written to PUBLIC_VENDOR_BRIDGE_CANDIDATES_2026-10-31.csv for inspection; it is an evidence sample, not a production master or verified full crosswalk.

Examples verified through public order records:

| Legacy PO VENDORID | VLIN | eVA ID | Evidence order |
|---|---|---|---|
| 799849187 | VA00080260 | VS0000142896 | PCO1825381, FY2016 |
| 195583778 | VA10050123 | C16503 | PCO1825380, FY2016; A207_1874596, FY2018 |
| DB5013044 | VA10077586 | VC0000161739 | EP2341729, FY2016 |
| DB5017422 | VA10081964 | VC0000166153 | A207_1874659, FY2018 |

These examples also show why an exclusively nine-digit/DUNS interpretation is insufficient. Preserve alphanumeric legacy identifiers as observed; do not manufacture numeric equivalents.

A direct public supplier-detail lookup independently confirms the TSRC supplier ID/VLIN pair and supplier name; it does not expose the legacy DUNS value:
https://logi.cgieva.com/External/rdPage.aspx?rdReport=Public.Reports.Report9001_VendorDetail&rdAgReset=True&lnkVendCustCd=VS0000142896&LinkHref=True

### Historical evidence is stronger than previously established

The official CAPP 70210 manual is dated May 2016 and already documents the DUNS/VLIN vendor-address fields and payment/voucher extracts. The bridge is therefore documented in the target period, not only in modern spreadsheets. This does not extend evidence to every prior year or prove field completeness:
https://www.doa.virginia.gov/reference/CAPP/CAPP_Topics_Cardinal/70210.pdf

The manual describes authenticated agency file transfers, compressed archival, and agency requests for archived extracts. These are agency interfaces rather than an anonymous bulk-download service. Present availability and historical retention are unverified. AP962 also excludes one-time vendors, limiting achievable coverage even if an extract is obtained.

### Correction: the historical cohorts are not under-$9,000 samples

The filename-based interpretation in the earlier session handoff is contradicted by actual amounts:

| Historical file | Rows | Amount >= $9,000 | Maximum amount |
|---|---:|---:|---:|
| EV01 PO lines | 42,175 | 433 | $5,079,800 |
| EV02 matched PO lines | 583 | 32 | $146,700 |
| EV06 payments | 248 | 34 | $146,700 |
| EV03 payments | 5,520 | 922 | $5,148,293 |

For PO lines, amount is Decimal quantity * unit price, not a PO-total measure. All 42,175 EV01 lines and all 583 EV02 lines have NIGP three-digit class <900. These controls refute the claimed dollar cut; they suggest a commodity-class split but do not fully reconstruct the original selection algorithm. Do not infer sampling rules from the LESS-THAN-9000/GREATER-THAN-9000 filenames. In particular, the earlier proposed single-payment bias caused by an under-$9,000 cutoff is not established. The saved probe reproduces these counts and hashes the cohort files.

Additional historical working files were located through workbook external-link metadata in the owner's Box folder. They remain outside the repository; no confidential source documents were copied into the evidence package. The provenance of independently captured payment-side identifiers remains unverified. Historical design material must not substitute for reconstructing the actual matching program.

### Payment-source checks and remaining bottleneck

The official FY2016 purchase-card archive was downloaded into memory for header inspection only (17,022,718 bytes; SHA-256 in the evidence JSON). All 12 monthly CSV members have exactly:

    AGENCY_NAME,VENDOR_NAME,POST_DATE,FISCAL_YEAR,AMOUNT

Source: https://www.datapoint.apa.virginia.gov/historicaldata/FY%202016%20SPCC.zip

There is no PO number, voucher number, VLIN, DUNS, or supplier ID in these headers. This source alone does not supply the missing independent payment link. No purchase-card archive was saved and no transaction totals were combined with expenditures.

The Partnership for Open Government Data supplies a later-period vendor-searchable expenditure mirror and an older purchase-card compilation. Its visible application fields still use vendor names; internal website row IDs are not documented vendor identifiers. It is a potential historical availability aid, not a verified identity bridge:
https://www.opengovdata.info/projects/virginiadatapoint/
https://www.opengovdata.info/projects/virginiaspcc/

Targeted Virginia CKAN catalog searches for VLIN, Cardinal, vendor payments, and checkbook did not identify an applicable statewide vendor-ID payment extract. This is a bounded negative search, not proof that no public source exists.

### Revised next steps

1. Extend the observed PO-side namespace bridge across more agencies, identifier shapes, years, and revisions, with explicit conflicting mappings and snapshot controls. Current sample evidence supports doing this; full-population coverage is not established.
2. Obtain or locate a payment-side extract with VLIN or remit vendor ID AND voucher/PO references. A DUNS/VLIN crosswalk alone cannot connect name-only APA payments with sufficient precision for substitution labels.
3. Prefer a narrow historical extract investigation over more name-normalization experiments: AP965 voucher headers/lines and AP705 payment advice, with archived AP962 address identifiers when needed.
4. If outreach becomes necessary, prepare a request scoped to non-tax business identifiers and PO/voucher references; send only with owner authorization. No request has been sent.

Bob can continue file profiling independently. Findings to consume when convenient: richer public FY2016/FY2018 schemas exist, the legacy/public snapshots differ, preserve VLIN/eVA IDs separately, and remove the under-$9,000 cohort assumption. No format conversion or financial-engine change is required.
## Public supplier reference acquired - 2026-10-31

The directory investigation produced a complete capture relative to the saved public report export: 230,128 canonical observations, 230,126 nonblank eVA IDs, two explicitly flagged missing IDs, and a passing full saved-artifact audit. See [SUPPLIER_DIRECTORY_SNAPSHOT.md](SUPPLIER_DIRECTORY_SNAPSHOT.md) and the completed session handoff entry for files and reproduction.

The report's documented CSV format provided an unchanged 230,128-row source file. Its Status column actually carries Self/State registration type, so active status was preserved separately from HTML. All exported fields were reconciled to HTML observations; the one supplier missed by the alphabetic capture was obtained by targeted ID lookup. This resolves the current public-directory acquisition task, while historical vendor identity and independent payment linkage remain separate open investigations. No outreach was sent.