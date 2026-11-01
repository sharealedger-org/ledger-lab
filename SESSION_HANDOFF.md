# Ledger Lab Handoff

Updated 2026-10-31. This file is a concise state snapshot; use the linked design contracts for details.

---

## 2026-10-31 Bob Corpus Work: Status (tasks 1-8)

Implemented and measured; nothing here changes the financial engine, Parquet was not used, and full data stays outside Git. Read [docs/CORPUS_FINDINGS.md](docs/CORPUS_FINDINGS.md) first.

| Task | Result | Where |
|---|---|---|
| 1 Inventory/coverage | Done. 385 files cataloged with SHA-256; 367 profiled; coverage matrix by family x FY; FY2005 not a stub, FY2009 has no `.txt`; stale-claim table added to PROJECT_BRIEF.md | `data/corpus_profile/`, `scripts/profile_corpus.sh`, `CorpusProfile.scala` (10 tests) |
| 2 Schemas/semantics | Done for the four header schemas on disk; no tab-delimited-FY2003-13 / reordered-FY2014-16 schemas exist here, contrary to DATA_ENHANCEMENT_PLAN.md | [docs/CORPUS_SCHEMAS.md](docs/CORPUS_SCHEMAS.md) |
| 3 Quality/denominators | Done. 133.79M parsed = 133.46M accepted + 326,356 quarantined; 0 line-reconciliation failures; 99% vs 94% NIGP reconciled (per line vs per vendor-year row) | [docs/CORPUS_FINDINGS.md](docs/CORPUS_FINDINGS.md) |
| 4 Identity | Specified; locator verified on 2,037 real records; per-record IDs deliberately not materialized | [docs/CORPUS_RECORD_IDENTITY.md](docs/CORPUS_RECORD_IDENTITY.md) |
| 5 Reference data | Inventoried. Agency names across all 350 APA files related to the reference table (`scripts/build_agency_alias_table.py` -> `data/corpus_profile/agency_alias_candidates.csv`): 99.1% of rows match exactly, 13 names (20,512 rows) unmatched, 3 names ambiguous between two keys (131,809 rows); validity periods all `unknown`. Fund/object: 3-month spot check only, **no fund/object alias evidence yet** | [docs/CORPUS_SCHEMAS.md](docs/CORPUS_SCHEMAS.md) |
| 6 Vendor observations | Documented only: by-year file has 8,101 column-shifted rows and 1,392 names with several IDs; `enrich_vendormaster.py` last-wins overwrite. **No code changed, no richer observation table built** | [docs/CORPUS_FINDINGS.md](docs/CORPUS_FINDINGS.md) |
| 7 Access | Done: `scripts/corpus_inspect.py files/fields/fetch/sample` (deterministic sample, checksum-verified fetch) | [docs/CORPUS_ACCESS.md](docs/CORPUS_ACCESS.md) |
| 8 Fixtures/release | Redistribution notes and Zenodo check done. **Research sample package not built**; the existing tiny fixture is unchanged | [docs/CORPUS_ACCESS.md](docs/CORPUS_ACCESS.md) |

Findings the owner should see:

- **Zenodo `10.5281/zenodo.23846362` is published but incomplete.** 47 files, all identical to local copies by MD5, but none of the 336 monthly APA expenditure/revenue files. Text says "public domain", license field says `cc-by-4.0`. Record not modified. With a 100-files-per-record limit (owner-reported, unchecked) the 385-file corpus needs about six records; the defect list, measured split and checklist are in [docs/ZENODO_ARCHIVE_PLAN.md](docs/ZENODO_ARCHIVE_PLAN.md). **Owner decisions pending:** license wording, vendor email addresses (13.35M populated PO rows), new-version versus new-records scheme. Nothing uploaded.
- PO files are not strictly by order-date fiscal year: 43,318 rows fall outside their file's FY (592 of 1,037 in FY2005).
- FY2017 PO extended price total (43.2B) is 71% six identical lines of 5,140,972,285.66 to health plans; meaning unknown. Do not quote PO dollar totals without checking extremes.
- 326,356 PO/APA records are quarantined (325,843 PO, split by embedded line breaks). Counts are fragments, not logical lines.

Not verified (see also the register): per-file source URL/acquisition date; completeness of any year; `.xlsx` PO copies vs `.txt`; whether APA expenditure/revenue overlap with transfers; reference-table validity by FY; real-corpus byte-for-byte rerun determinism; multi-line record re-read by offset.

Known boundary issue left alone: `run_pipeline_orchestrator.sh` Step 5 still calls Python `scripts/data_aggregation.py`.

Suggested next: resolve the 13 unmatched and 3 ambiguous agency names and add fund/object alias evidence; a vendor-observation table keyed by `source_record_id` (so name conflicts surface instead of being overwritten); the research sample package with a manifest; work the Zenodo checklist in docs/ZENODO_ARCHIVE_PLAN.md; M1-M4 log validator and M8 extractor regression tests.

---
## 2026-10-31 Public Supplier Snapshot Completed (Codex)

A dated eVA public-main-directory reference is now available at `data/VendorDownload/supplier_snapshot_2026-10-31/suppliers.csv` (ignored by Git). It contains **230,128 supplier/location observations**, **230,126 nonblank eVA IDs**, and **2 missing IDs retained with flags**. HTML vendor statuses are 230,068 Active and 60 Pending Approval. Ten focused tests and the final full saved-artifact integrity audit passed.

**Important source quirk:** the raw CSV export calls its Self/State registration-type column Status. The canonical CSV separates registration_type from actual status using HTML evidence. A full 230,128-row source export was reconciled on every available field: 230,127 rows matched the prefix capture directly; one non-ASCII supplier (SUP323218) was retrieved through an exact supplier-ID lookup. The export route made completion of redundant broad-query pagination unnecessary.

Read [docs/SUPPLIER_DIRECTORY_SNAPSHOT.md](docs/SUPPLIER_DIRECTORY_SNAPSHOT.md) for field meanings, scope, files, controls, and refresh commands. The snapshot folder contains unchanged source CSV, 2,912 source HTML pages, checksums, acquisition/reconciliation manifests, and audit.json. No Parquet or financial-engine changes were made. Current public main-directory coverage is established against the saved export, not against the separate inactive directory or every Commonwealth payee, and not as historical FY2003-FY2016 master truth.

The targeted historical-candidate VLIN check also produced `historical_candidate_details/historical_candidate_vlin_verification.csv`: 20 agreeing observations and 2 unresolved observations across 21 supplier IDs. This is not a full VLIN crosswalk or a payment join. Bob can consume the completed dated reference while continuing corpus profiling. Codex retains historical vendor identity and independent payment-link investigation. No outreach sent.

---
## 2026-10-31 Codex Follow-up: Public PO Vendor Bridge Observed

Actual public FY2016/FY2018 CKAN PO records carry VLIN, eVA IDs, line numbers, status, line-total changes and other fields absent from the local legacy text extracts. A reproducible convenience sample matched 22/24 selected legacy lines to singleton nonblank VLIN/eVA pairs by exact full PO number plus agency/NIGP/quantity/price; the other two are all-nines placeholders with missing VLIN. This is observed PO-side namespace evidence, not a payment join or a coverage estimate. Local/public snapshot row counts differ. Script: `py -3 scripts/probe_public_vendor_bridge.py --spcc-schema`. Findings and saved JSON/CSV evidence are linked in [docs/VENDOR_IDENTITY_INVESTIGATION.md](docs/VENDOR_IDENTITY_INVESTIGATION.md).

**Correction to earlier handoff:** EV01/EV02/EV06 are not under-$9,000 cohorts. EV02 contains 32 PO lines >=$9,000; EV06 contains 34 such payments. Every EV01/EV02 line has NIGP class <900, suggesting a commodity split; the original selection algorithm is not yet reconstructed. Do not carry the dollar-cutoff assumption into test design.

Official FY2016 purchase-card headers have no PO/vendor IDs. A May 2016 CAPP manual independently documents DUNS/VLIN fields and relevant authenticated agency extracts; no actual public historical payment-side bridge extract has been obtained. Bob remains on corpus profiling; Codex retains vendor investigation. No outreach or financial-engine changes.

---
## 2026-10-31 Codex Vendor Investigation Results

See [docs/VENDOR_IDENTITY_INVESTIGATION.md](docs/VENDOR_IDENTITY_INVESTIGATION.md) and the hashed aggregate evidence JSON linked there. Reproducible read-only probe: `py -3 scripts/investigate_vendor_identifiers.py --specs` from ledger-lab.

Key correction: the local PO data dictionary explicitly describes VENDORID as Vendor DUNS #, not tax ID. Official AP962 vendor-address schema stores DUNS in COUNTY and VLIN in ADDRESS3 alongside Cardinal vendor ID/address sequence. AP705/AP965 also carry VLIN; AP965 line description can contain the PO ID. This establishes a schema bridge, not a validated public historical crosswalk — no actual historical extract rows have been obtained. Cardinal became the system of record in FY2017, so modern specifications do not carry through to FY2003–FY2016 without additional evidence.

Independently reproduced 196/248 historical exact-amount candidate hits and 13 ambiguous hits. All 127 nonplaceholder EV06 identifiers occur in FY2016 public POs, but this does not prove the author obtained payment-side identifiers independently; they may have been assigned after matching. Parser counts differ slightly from prior temporary probes; the difference is explained by quote handling: `csv.reader` (default quoting) skips blank lines produced by quoted multiline record reassembly, yielding 1,713,138 data rows for FY2016 where a naive line count gives 1,817,217 physical lines. Bob's Scala `CorpusProfile` profiler uses a quote-aware byte-level parser and counts accepted + quarantined = 1,714,830 parsed records (1,686,615 accepted). The three counts measure different things and are consistent. Public historical transaction linkage, source precision/recall, and vendor-substitution labels remain unknown. No outreach sent; no financial code/source data changed; no Parquet work.

---
## 2026-10-31 Corpus Work Assignment to Bob; Vendor Investigation Retained by Codex

**Owner direction.** Bob is to take the corpus usability work. Codex retains the vendor-identity and PO/payment-linkage investigation. This entry assigns work; it does not report implementation or validated findings. The owner is undecided about Parquet. **Do not convert the corpus to Parquet or make it a dependency.** Preserve the current text/CSV interfaces. Any later format proposal should explain a demonstrated need, interoperability, storage duplication, preparation cost, and effects on comparable measurements before adoption.

### Bob: purpose and boundaries

Make the Virginia data a reproducible, documented research corpus usable across the accounting model, Scala/CKB experiments, perspectives, and Paper 2 analysis. Start with the existing files and contracts; this is not a new financial engine or a wholesale data-model replacement.

Read AGENTS.md, docs/RUNTIME_DATA_LAYOUT.md, docs/VA_DATA_MODEL_CONTRACT.md, docs/DATA_ENHANCEMENT_PLAN.md, docs/MEASUREMENT_AND_LOGGING.md, and the existing preparation scripts. Treat older enhancement-plan claims as proposals requiring evidence. Follow the architecture gates and verification protocol when applicable. Preparation/inspection may use Python; financial processing remains Scala and orchestration remains Bash. Keep full source and derived runtime files outside Git under the declared runtime root.

### Bob: tasks to consider, in priority order

1. **Inventory and coverage before transformation.** Produce a machine-readable catalog (CSV is sufficient) for every source file: relative path, source URL when known, acquisition date when known, checksum, bytes, source family, claimed fiscal year/period, schema identifier, and inspection status. Do not invent missing provenance. Distinguish missing, empty, partial, malformed, and completeness-unknown files. File size alone is not evidence of a stub: extract_po_attributes.py currently skips files below 0.5 MB. Inspect the small years before accepting that classification, and reconcile conflicting year-gap statements in existing documents.

2. **Document schemas and semantics by source era.** Record headers, delimiters, encodings, blank-header behavior, field types, identifier namespaces, amount/sign conventions, date meanings, fiscal-year rules, and known schema transitions. Preserve leading zeros and identifiers as strings. Distinguish accounting date, order date, voucher date, fiscal year, extraction date, and actual revision timestamps where available. A -Vn suffix alone does not establish a complete revision history. Do not infer tax IDs from identifier shape.

3. **Profile data quality with explicit denominators.** Per file/year, report physical/input records, parsed records, accepted records, rejected/quarantined records, blank IDs, placeholder IDs, duplicate candidates, date range, and available amount controls. State exclusions and rounding rules. Separate signed totals from absolute-dollar totals. Reconcile coverage claims: the saved extractor summary reports 99% NIGP coverage among counted lines but 94% among vendor-year rows; the handoff conflates these measures. Records skipped for blank vendor IDs must still appear in input/disposition counts. Preserve malformed originals and reason codes rather than silently dropping records or treating nulled fields as repaired rows.

4. **Define source provenance and stable record identity.** Retain immutable raw files. Specify stable source-record IDs using a file checksum and an unambiguous record locator; distinguish parsed-record ordinals from physical lines for quoted multiline data. Keep raw and normalized values distinguishable, with parser/normalization version and quality flags. Preserve legitimate repeated transactions; do not deduplicate solely because business fields match. Derived outputs must identify their input snapshot and transformation version.

5. **Make reference data historical and agency-aware.** Inventory existing agency/fund/object/program/source references and their coverage. Prepare versioned agency-name aliases and chartfield mappings with source evidence, validity periods when known, and explicit unknowns. DPB documents that historical fund-detail codes may differ by agency and expanded from four to five digits around 2016: do not join on fund code alone or apply current tables retroactively without validation. Preserve Program information and flag discrepancies with current Scala mapping rather than silently redefining financial contracts. Coordinate vendor crosswalk requirements with Codex; vendor matching itself remains Codex's investigation.

6. **Retain richer vendor observations before summarizing.** Preserve observed vendor IDs/names, addresses, commodity codes, dates, and vendor-commodity counts; produce modal summaries as derived conveniences with dominant share and ambiguity. Commodity mix is an observation, not an intrinsic vendor classification. A change in annual modal commodity/state is not proof of an effective-dated master-data change; annual full-year summaries may leak future information into historical predictions. Separate vendor_state from derived geo_region and validate against a declared geographic vocabulary. In enrich_vendormaster.py, repeated normalized names overwrite earlier entries: surface conflicts and retain candidate identities instead of choosing by file order. Keep source enrichment distinct from synthetic attributes.

7. **Deliver a usable, format-neutral access package.** Keep CSV/text as the initial interchange format. Document exact commands for selecting years, inspecting fields, reproducing profiles, and obtaining a stable sample. Preserve existing Scala/Bash input contracts. Keep preparation time/storage separate from measured engine costs. No database, Parquet conversion, or new query service is required to complete this work.

8. **Provide representative fixtures and a release manifest.** Retain the tiny teaching fixture and specify a reproducible research sample covering available schema eras, missingness, ambiguous identities, revisions, negative amounts, and malformed records. Clearly label synthetic rows. Package dictionaries, catalog, quality reports, reference versions, preparation commands, and checksums with the release. Verify archive availability/completion rather than assuming a DOI proves all files are uploaded. Record redistribution terms by source; do not assume all state records or external code descriptions have identical permissions.

### Suggested first deliverable and acceptance evidence

Start with an inventory, year/source coverage matrix, and short schema/quality report before broad cleanup. Then implement the smallest reproducible preparation package justified by those findings. CSV manifests and Markdown documentation are sufficient initially.

A usable first release lets another contributor identify the exact input snapshot, understand every field used, reproduce row dispositions and available source totals, trace sampled derived records back to originals, see unresolved mappings, and run existing fixture consumers. Checks should exercise actual malformed/ambiguous cases and deterministic reruns. Do not claim full source completeness merely because generated journals balance. Keep an explicit list of unverified properties and update the audit register where required.

### Codex: retained investigation, separate from Bob's corpus work

- Investigate the documented eVA-to-Cardinal relationship through VLIN (Vendor Location Identification Number), Cardinal vendor IDs, and historical PO VENDORID namespaces. A documented system relationship is not a verified public historical crosswalk.
- Inspect public specifications for AP962 vendor extracts, AP965 voucher extracts, and AP705 payment reconciliation extracts. Determine available fields, historical applicability, public access, and whether a narrowly scoped data request could help. No request or outreach has been sent or authorized by this handoff.
- Investigate historical agency-to-agency supplier/customer crosswalks for interagency identification and future consolidation fixtures.
- Profile APA purchase-card exports as a potential additional payment source; establish fields and overlap before proposing any union with expenditures.
- Continue independent PO/payment linkage feasibility work with reproducible scripts, candidate evidence, ambiguity, and precision checks. Vendor identity links and transaction links must remain separate; matching on vendor equality would exclude substitutions that Test 2 seeks to observe.
- Return findings as evidenced mappings/specifications and unresolved questions that Bob can incorporate without blocking the general corpus package.

### Official investigation leads found this session

- Vendor maintenance manual: https://doa.virginia.gov/reference/CAPP/CAPP_Topics_Cardinal/21105.pdf (April 2018; describes procurement-vendor integration and VLIN on Cardinal vendor addresses).
- Cardinal extract layouts: https://www.cardinalproject.virginia.gov/index.php/resources/agency-interface-layouts (AP962, AP965, AP705 and chartfield/ledger/budget layouts listed; spreadsheet contents were not retrieved in this session).
- Agency crosswalk listing: https://www.cardinalproject.virginia.gov/job-aids (Agency to Agency Transactions Information Sheet; current listing includes Customer and Supplier IDs, not a validated historical mapping).
- DPB chartfield reports: https://dpb.virginia.gov/budget/budget.cfm?page=COA
- Fund semantics and agency scope: https://reports.dpb.virginia.gov/coa40
- Budget/expenditure report field explanations: https://reports.dpb.virginia.gov/be122
- APA legacy financial and purchase-card downloads: https://www.datapoint.apa.virginia.gov/legacyindex.php (purchase-card exports listed from FY2014; not profiled yet).

---
## 2026-10-31 (later) PO-to-Payment Join Investigation, Second-Opinion Brief

**Purpose of this entry.** The open question from the entry below is whether a public key links a
Virginia PO line (eVA) to a posted expenditure record. Tests 1b, 2 and 3 for Paper 2 depend on it.
This entry records what one session (Bob) found, how, and what an independent reviewer (Codex)
should try to confirm or break. **Nothing here is a validated result.** All figures come from ad hoc
read-only Python probes whose scripts were written to `%TEMP%` and not kept. Reproduce from the
method descriptions, do not trust the numbers.

Files added this session (uncommitted): `docs/TEST_DESIGN_PAPERS_1_2.md` (test design, Parts A-D) and
`data/VendorDownload/*.pdf` (10 eVA vendor-list screenshots, about 2.2 MB, not git-ignored; do not
commit unless wanted). No code was changed.

### Bob's conclusion (to be tested, not accepted)

1. No public vendor-number crosswalk between the PO side and the expenditure side was found.
2. The only workable public join is by **names**, and it covers a small fraction of POs and dollars.
3. Therefore Test 2 (vendor substitution) is not buildable on public data, and Tests 1b/3 would run on
   a small, biased sample unless a better key is found. Test 1a (PO-side proxy) and an aggregate
   (agency-year) Test 3 do not need the join.

### Evidence, with method

**A. Field inventory (checked directly).**
- APA expenditure files (`data/VARawFiles/`, legacy `historicaldata/FY nnnn Expenditures.zip`, FY2003-FY2016):
  columns `AGENCY_NAME, AMOUNT, FUND_NAME, LOCALITY, OBJECT_NAME, PROGRAM_NAME, VENDOR_NAME,
  VOUCHER_NUMBER, VOUCHER_DATE`. No vendor ID, no PO number. Headers read for FY2003, FY2013, FY2014, FY2016 only.
- eVA PO files (`data/VAPOData/VA_opendata_FYnnnn.txt`, tab-delimited, blank first line):
  `PONUMBER, AGENCY, ORDEREDDATE, ... QUANTITYORDERED, PRICE, VENDORID, VENDORNAME, ... NIGPCODE ...`.
  No Object/Fund code.
- FY2016 PO `VENDORID` shapes (1,713,126 lines): 9 digits 87.6%, `999999999999` placeholder 6.7%,
  two letters + 7 digits 4.2%, blank 0.9%. FY2018: 85.2% / 4.9% placeholder / 8.1% / 1.0%.
  Treating the 9-digit form as a tax ID is a guess from its shape only.

**B. Direct PO-number link: not found (sample only).** FY2014 April expenditure file, first 500,000 rows:
`VOUCHER_NUMBER == PONUMBER` (exact string, trimmed) in 1 row. `PONUMBER` shapes are mixed
(`AAA9999999`, `AA9999999`, `A999_9999999`, suffix `-V2`); voucher shapes are different
(`A9999999`, `99999999`, `AA999999`, ...). Containment, prefix/suffix stripping and per-agency
conventions were **not** tested. This is the weakest part of the negative result.

**C. Name/amount join at full FY2016 scale.**
- Key: (normalized agency name, normalized vendor name, amount rounded to 2 dp). Normalize = uppercase,
  drop non-alphanumerics. PO side: sum of `QUANTITYORDERED * PRICE` per `PONUMBER`. Payment side: sum of
  `AMOUNT` per (agency, vendor, `VOUCHER_NUMBER`).
- Result: 1,686,234 parseable PO lines, 635,196 POs; 7,202,800 expenditure rows, 3,752,550 vouchers;
  19,106 voucher-level hits, 2,857 of them ambiguous (more than one PO shares the key); 18,623 distinct
  POs matched (2.9%). Row-level (single expenditure row vs PO total) hits: 20,518.
- **Not measured:** false-positive rate, and recall. Known recall limits of this run: only FY2016 POs were
  matched to FY2016 expenditures (POs ordered in FY2015 and paid in FY2016, or ordered late FY2016 and paid
  in FY2017, are missed); partial and installment payments are missed by the exact-amount rule; freight/tax
  differences are missed. The 2.9% is therefore a floor for this key, not a ceiling for any join.

**D. Name-coverage ceiling, FY2016 expenditures (7.2M rows).** Rows classified by `VENDOR_NAME`.
The "pseudo-vendor" regex (`PAYROLL|JOURNAL|TRANSACTION|LABOR COST|CIPPS|INTERAGENCY|CONFIDENTIAL|REFUND|TRAVEL|ADJUST|TREASURER|RETIREMENT|BENEFIT`) is crude.

| Class | Rows | Share of absolute dollars |
|---|---|---|
| Name appears in the FY2016 PO file | 1,222,259 (17.0%) | 17.8% |
| Name not in PO file | 3,293,291 (45.7%) | 21.8% |
| Pseudo-vendor | 2,687,250 (37.3%) | 60.4% |

In the PO files 2,811 vendor names (FY2016) map to more than one `VENDORID`.

**E. The 2020 matched dataset** (`C:\Users\0D8297897\Box\CommonKeyBufferingData\VA_2016_New\`, files dated
Dec 2019 - Jan 2020, author unavailable, method undocumented). Event files EV00-EV07 with one running
`event_id` sequence. EV01 = 42,175 PO lines under $9,000 (20,309 POs, ordered 2013-07 to 2016-06);
EV02 = 583 lines / 248 POs (all in EV01); EV06 = 248 payments under $9,000 (`contract_id` literally `"VA"`);
EV03 = 5,520 and EV04 = 5,706 matched payments; EV05 = 3,548,091 unmatched payments (`contract_id` blank,
vendor blank in the first 500,000 rows); EV07 = tax receipts. Payment rows carry `fundDtlKey, objectKey,
objectCode, subPrgmKey`, `org_unit_cd` and a vendor ID (`ip_rec_id`) in the same formats as the PO `VENDORID`.
- Reverse-engineered key: (`ip_rec_id`, `org_unit_cd`, payment amount == sum of `qunatity*unit_price`
  over the PO's lines). 196 of 248 EV06 payments match an EV02 PO exactly; 13 of those keys are ambiguous;
  the other 52 do not match (near misses such as 33.92 vs 23.92; no 2-3 PO sum explains them).
  Payment date minus order date: median 259 days, minimum -36, maximum 488.
- Inconsistencies, unexplained: EV04 (5,706) is not EV03 + EV06 (5,768) and shares no `event_id`;
  EV03 is named "greater than 9000" but only 919 of 5,520 rows exceed 9,000; EV05 sums to $42.15B net.
- Implication: the author had a payment-side vendor ID that the public APA files lack. Source unknown.

**F. eVA Transparency Reports vendor list** (`logi.cgieva.com/External/rdPage.aspx?rdReport=Public.Reports.Report9001_Data`).
Public Logi report. Fields: status, HQ flag, eVA vendor ID (`SUP######`, `VS#######`, `E#####`, `VC...`),
name, address, SWaM type, card/e-order flags; detail page adds VLIN and NIGP codes. No tax ID/FEIN/DUNS.
Display cap 1,000 vendors per query (10 pages of about 100); the 10 PDFs hold one such query (alphabetical
from `# 1 OrderAnything` to `4Front...`). Searching 8 IDs from the PDFs (`SUP365210, SUP354912, VS0000033394,
SUP306409, SUP317717, VS0000340540, E52135, E28599`) in the FY2016 and FY2018 PO files gave 0 hits (plain-text
search). Counted properly: 0 of 1,713,126 FY2016 and 0 of 880,813 FY2018 PO lines have a `VENDORID` starting
with `SUP`, `VS0` or `VC0`. Four vendor names from PDF page 1 gave 0 hits in FY2018 (FY2016 not checked).
So the eVA ID namespace is different from PO `VENDORID`. **Correction:** my earlier "search for TSRC / 799849187"
probes against this site returned the default first page every time, so GET search parameters are ignored
(probably a form POST with a CSRF key). Those probes showed nothing either way.

**G. Public portals checked (shallow).** data.virginia.gov CKAN API (`package_search`): eVA datasets exist for
2016-2027 (the FY2016 CSV resource reports size 0); queries for expenditures, vendor payments, checkbook, Cardinal,
Auditor of Public Accounts found no state vendor-payment dataset with vendor IDs. datapoint.apa.virginia.gov:
legacy zips FY2003-FY2016 and a new extract path pattern `data/extracts/{fiscalYear}/All {Expenditures|Revenue|Budget} for Fiscal Year {fiscalYear}.zip`
(my direct URL guess for that path returned 404; the repo's FY2014-FY2016 folders have that naming).
`legacydatapoint.apa.virginia.gov` timed out once. Only a handful of search terms were tried.

### What Codex should do to confirm or refute (suggested, in priority order)

Run these independently and write your own scripts; do not reuse numbers above. Report agreement or
disagreement per item, and say what you could not check.

1. **Look harder for a direct key (could overturn the conclusion).** For several fiscal years and several
   agencies, test whether `VOUCHER_NUMBER` contains or embeds `PONUMBER` (strip `-Vn`, leading letters,
   underscores, zero padding; try substring and suffix/prefix). Compare per agency, since universities and
   executive agencies may use different conventions. Also test whether `VOUCHER_DATE` clusters shortly after
   `ORDEREDDATE` for candidate pairs.
2. **Find the payment-side vendor ID.** Search for any public Virginia source of vendor payments keyed by
   vendor ID/FEIN or including PO number: Cardinal/DOA vendor-payment or "checkbook" files, FOIA responses,
   Wayback Machine copies of old `datapoint.apa.virginia.gov` or `data.virginia.gov` extracts, eVA reports
   other than Report9001 (the Logi site has numbered public reports, e.g. 9004 NIGP lookup, 9016 SBSD list,
   9020 inactive vendors), DOA "Commonwealth Data Point", any dataset with a PO number field on payments.
   Also look in the rest of the Box folder `CommonKeyBufferingData\` (only `VA_2016_New` was opened) for the
   2020 author's source extracts, SQL or scripts.
3. **Reproduce and stress the name-join.** Recompute C for FY2016. Then (a) add a placebo (shuffle vendor
   names or shift the date window by one year) to estimate the false-positive floor; (b) allow POs from the
   prior fiscal year to match; (c) allow installments (several vouchers summing to the PO total) and small
   tolerance for freight/tax; (d) report match rate, ambiguity and implied precision per year FY2006-FY2016
   (FY2004 has 227 and FY2005 has 1,037 parsed PO records, so they are small, not missing; FY2009 has no `.txt` -- see docs/CORPUS_FINDINGS.md). Say whether the rate is stable across years.
4. **Check the 2020 key and its sample selection.** Confirm 196/248 on EV02 vs EV06, then test the same
   (vendor ID, agency, amount) rule on the full public FY2016 data. Explain the EV03/EV04/EV05 inconsistencies
   if you can. Check whether the under-$9,000 cut and exact-amount rule bias the sample toward single-payment POs.
5. **Challenge the ceiling.** Check the pseudo-vendor regex and the 17.8% / 21.8% / 60.4% dollar split with a
   better vendor-name classifier. Say what share of non-pseudo dollars is name-matchable after fuzzy matching
   (suffix stripping such as INC/LLC, punctuation, DBA names), and the false-merge risk it adds.
6. **Check the claim that Test 2 cannot be built.** Argue for or against: is there any public way to observe
   "paid vendor differs from ordered vendor" without a vendor ID on the payment side?
7. **Review `docs/TEST_DESIGN_PAPERS_1_2.md`** for errors, especially Part C (Test 0, Test 1a/1b, Tests 3-4)
   and the Part A reading of `data/pipeline_log.csv`.

### Decisions still open for the project owner

- Whether to scrape or request bulk eVA vendor data (site terms not reviewed).
- Acceptance thresholds for Tests 2-4 and the C1 replicate stability threshold (unchanged from `docs/TEST_DESIGN_PAPERS_1_2.md`).
- Whether Test 2 is dropped from Paper 2 if no payment-side vendor ID is found.
- Whether to keep `data/VendorDownload/` and git-ignore it.

---


## 2026-10-31 Windows Validation, PO Data Pipeline, and Zenodo Archive

**Context.** This session ran on the Windows machine holding the full Virginia PO dataset.
It completed the PO attribute extraction pipeline, established the Zenodo data archive,
and cleaned the PO attribute extraction logic. The downstream PyTorch ML work (Tests 2 and 3)
remains the next milestone.

**What was done.**

1. **Pulled and synced main** — picked up 55 commits including the macOS measurement fixes
   (Step 3 `records_read=0` repair, Step 2 header mismatch fix) from the prior session.

2. **Committed synthetic FY03 PO fixture** (`data/VAPOData_fixture/VA_opendata_FY2003_small.txt`):
   - 10 synthetic rows matching the FY03 expense fixture vendor/agency (VCU, BLACKWELLS BOOK SERVICES).
   - Added `--with-po-fixture` flag to `run_repo_fixture_smoke_test.sh` so any machine can exercise
     the transStandardize PO branch without the real external data.

3. **Ran PO attribute extraction** (`extract_po_attributes.py --by-year`) over the full
   15.6M-record VA eVA dataset (FY2002–FY2016, "13 years" as reported then; CORRECTED 2026-10-31: by the 0.5 MB cutoff the extractor skipped FY2001/FY2004/FY2005, and the by-year output covers 12 fiscal years -- 2002, 2003, 2006-2008, 2010-2016):
   - Output: `data/VAPOData/vendor_po_attributes_by_year.csv` (210,496 vendor-year rows, git-ignored).
   - Fixed a validation bug: the original `len(nigp_raw)==3` check fired on the full 5-digit NIGP
     code before the `[:3]` class-prefix slice, zeroing all NIGP coverage. Fixed to `nigp_raw[:3].isdigit()`.
   - Added column-shift cleaning: `VENDORSTATE` values that are not 2-letter alpha codes (address
     strings, UOM values) are nulled. Result: 99% NIGP coverage, 94% geo coverage on vendor-year rows.

4. **Ran CAR change detection** (`car_detect_changes.py`) → `data/VendorUpdate.csv` (committed):
   - 64,850 date-effective attribute changes: 63,238 ATTR_NIGP, 1,612 ATTR_GEO.
   - Geo change count dropped from 6,115 (dirty) to 1,612 (clean) after the column-shift fix.

5. **Downloaded full Virginia APA expenditure/revenue/budget data** to `data/VARawFiles/`
   (git-ignored): 350 files, 20.3 GB, FY2003–FY2016, all still live on datapoint.apa.virginia.gov.

6. **Established Zenodo archive** for long-term reproducibility:
   - DOI: `10.5281/zenodo.23846362` (upload in progress as of session end)
   - Contains: `VARawFiles/` (APA exp/rev/budget FY2003–FY2016) + `VAPOData/` (eVA PO FY2001–FY2018)
   - Virginia state government records are public domain; NIGP descriptions not redistributed separately.
   - Known gaps (CORRECTED 2026-10-31): the FY2005 `.txt` is not a stub (1,037 records); FY2009 has no `.txt`. The record lists 47 files and omits the 336 monthly APA expenditure/revenue files (see docs/CORPUS_ACCESS.md).
   - DOI reference committed to `RUNTIME_DATA_LAYOUT.md`, `download_va_data.py`, `download_va_po_data.py`.

**Papers reviewed this session (both read in full).**

- [`org-members/research/01-minimum-cost-curve.md`](../org-members/research/01-minimum-cost-curve.md) — Paper 1
- [`org-members/research/02-pre-hierarchy-event-repositories.md`](../org-members/research/02-pre-hierarchy-event-repositories.md) — Paper 2
- [`org-members/research/PAPER_02_NOTES.md`](../org-members/research/PAPER_02_NOTES.md) — Paper 2 working notes with VA test design

**What ledger-lab must produce for Paper 1 (Minimum Cost Curve).**

Paper 1's VA empirical contribution is the **Instrument Pivot Theorem demonstration** (§3.5):
- Vendor ID = Instrument ID; LDGR = balance store $\mathcal{L}$; VendorMaster.csv = CAR.
- Run C1 (Steps 2,3 — post only) → cost_surface.csv row: compute_s, storage_bytes, master_file_count=1.
- Run C2 (Steps 2,3,5 — post + views) → cost_surface.csv row with additional master files.
- Show C1→C2 delta = measured marginal cost of one reporting view.
- Add a CAR attribute to VendorMaster (e.g. instNIGPClass from vendor_po_attributes_by_year.csv),
  re-run C2 → zero new balance records, zero reconciliation cost. That ratio vs. key-embedded
  alternative ($(1+v)^m - 1$ formula) is the empirical proof of the theorem.
- Paper 1 conclusion explicitly calls for an agentic cost-curve sweep: agent changes ViewSpec,
  re-runs pipeline, observes costs. `run_cost_curve_sweep.sh` and `cost_surface.csv` are designed
  for exactly this.
- **Three C1 replicates** are needed to establish measurement stability before C2.

**What ledger-lab must produce for Paper 2 (Pre-Hierarchy Event Repositories).**

Paper 2 proposes a two-corpus structure: insurance (proprietary, 25.5B records) + VA (public,
reproducible). The VA corpus carries the scientific reproducibility and direct falsifiability weight.
Four tests, all against VA FY2003–FY2016:

| Test | What it proves | Method |
|------|---------------|--------|
| **Test 1** | Direct empirical count of Theorem 1 non-injectivity | Python: for every PO line, construct its posted image (Agency+Object+Amount); count how many distinct PO patterns map to the same key. Produce the distribution. No ML. |
| **Test 2** | Vendor substitution undetectable from posting (RQ4) | ML: given commitment event (Agency, NIGP, original vendor, price, date), predict whether a *different* vendor fulfilled the order. Balance-trained model literally cannot form this prediction — the feature does not exist. |
| **Test 3** | PO modification rate → budget deviation year N+1 (RQ2/RQ3) | ML: Layer 2 = aggregate expenditure ratios; Layer 3 = PO modification sequence + timing. If Layer 3 wins, clean empirical RQ2/RQ3 proof. |
| **Test 4** | 14-year longitudinal robustness (RQ3) | Train on years 1–7, predict year 8. Repeat for all 7 windows FY03–FY16. 7 independent replications. |

**Critical open question before Tests 2–4 can be built:**

The PO–expenditure **join key is unverified**. Expenditure files use `Agency/Fund/Object/SubProgram`
coded keys, not PO numbers. The PO files have `PONUMBER`. Whether a `PONUMBER → expenditure record`
linkage exists in the full-year data must be confirmed before Tests 2–4 can be scoped.
See PAPER_02_NOTES.md open question #2.

**Next steps — in order.**

1. **Confirm Windows pipeline compiles**: run `bash scripts/run_repo_fixture_smoke_test.sh --show-data`
   on this machine. sbt 1.x and Java 21 are confirmed available.
2. **Run 3× C1 replicates** against full FY03 data in `data/VARawFiles/` to establish measurement
   stability and generate Paper 1 cost-surface baseline rows.
3. **Run C2** (add Step 5) against same FY03 data → measure C1→C2 marginal cost.
4. **Build and run Paper 2 Test 1** (Python counting script, no ML) → produces the non-injectivity
   distribution table for Paper 2 §5.1b, and simultaneously checks if the PO→expenditure join key
   exists in practice.
5. **Install PyTorch** (2.15.0 available for Python 3.14 on Windows — confirmed) and build
   Tests 2–4 once the join key question is resolved.
6. **Build VendorMaster_enriched.csv** from PO attributes + existing VendorMaster.csv so
   `car_detect_changes.py` has a real instTypeID baseline (currently defaults all vendors to EXP).

## 2026-10-31 Measurement Correction and Downstream PyTorch ML Strategy

**Context.** This session repaired the critical measurement gaps on the macOS baseline and established the strategic integration of a downstream PyTorch-based machine learning prediction framework (Test 2 and Test 3 for Paper 02) to be executed on a standard PC/Windows machine containing the full Virginia purchase order (PO) commitment dataset.

**What was done.**

1. **Repaired Step 3 Metrics (`records_read=0` macOS blocker)**:
   - Identified that `grep -i "Records Read:" | tail -1` on the `post.scala` module matched both the `Journal Records Read` and `Ledger Records Read` outputs, greedily returning `0` as the final match.
   - Updated `scripts/run_pipeline.sh` with a multi-layered fallback strategy parsing `Journal Records Read:` and `Ledger Records Written:` directly from standard output, making metrics extraction completely bulletproof across all environments.
2. **Resolved Step 2 Header Mismatch (761 vs 760 rows)**:
   - Eliminated the historical dummy element initialization pattern in `transStandardize.scala` across `poRaw` and `transRaw` (Expenses and Revenue) buffers, which was inflating the JVM-logged read counts by 1.
   - Replaced `count_file_rows` (which counts raw text lines including headers) with `count_data_rows` in `run_pipeline.sh` and `file_data_rows` in `write_canonical_evidence.sh` for Step 2's raw input. Both Step 2 and Step 3 now report perfectly synchronized, exact data row metrics (e.g., 760 and 1520 rows).
3. **Formulated the Downstream PyTorch Plan**:
   - Outlined how the Scala-generated output partitions (Layer 3 raw PO events vs. Layer 2 posted GL balances) will act as the training inputs for a compact PyTorch model on a PC.
   - Defined **Test 2 (Vendor Substitution Predictor)** and **Test 3 (Budget Deviation Predictor)** as the operational proofs of Theorem 1 (Posting Non-Injectivity).

**Next steps on the Windows machine containing the PO data:**

1. Run PO Attribute Extraction with `--by-year` flag to generate yearly PO attributes.
2. Build the CAR Update Cycle (`car_detect_changes.py` and temporal as-of-date join in Scala `VendorMaster` and Python Step 5).
3. Set up the PyTorch Dataset Loader and Model script on Windows to run Tests 2 and 3.
4. Execute C1 replicates to establish hardware cost-surface baseline data.

---

## 2026-10-28 Contributor Onboarding Session

**Context.** This session addressed the question of how to hand the repository off to a broader
technical community — likely students working under a professor's direction if the papers are
judged worthwhile. The developer (Kip) is stepping back from being the primary implementer.

**What was done.**

- Wrote and committed [`CONTRIBUTOR_ONBOARDING.md`](CONTRIBUTOR_ONBOARDING.md) (commit `3883d4e`).
  This document explains the two research claims, the CKB architecture pattern, honest
  implementation status (including the known `records_read=0` metric breakage), the cost-curve
  agent goal and what it needs to be true, the priority work items, and what AI assistants will
  get wrong about the repository.
- Linked it from the `README.md` Contributing section.

**Key framing established for incoming contributors.**

> The architecture is sound and partially verified at fixture scale. The measurement substrate
> needed to produce comparable cost points is not yet reliable. The next milestone is not a new
> financial feature — it is making the existing pipeline emit trustworthy evidence that an agent
> can read and interpret without human intervention.

**Priority work items for the next contributor (unchanged from previous session).**

1. Fix Step 3 process metrics emission — `records_read=0` on macOS renders the cost surface
   untrustworthy. This is the blocker for all cost-curve work.
2. Validate evidence schemas on failed/partial/no-op runs.
3. Define machine-readable workload manifests (input snapshot hash, steps, materialization
   profile, code revision) so two runs are verifiably comparable.
4. Define the C1 named workload and run three replicates to establish measurement stability.
5. Define and run C2 (add Step 5). The C1→C2 delta is the measured marginal cost of reporting
   materialization.

**What was not changed.** No financial code was modified. The audit register states are unchanged.
The architectural gate documents remain in force.


---

## Status for External Reviewers (added for first Sharealedger member circulation, June 2026)

This repo is the empirical laboratory for the nine-paper monograph *The Architecture of Economic State* (Twitchell). It is **active research infrastructure, not a finished demonstration**. What it is and is not at this point:

**What works.** The Scala pipeline runs on the public Virginia state vendor payment dataset (FY2003). Steps 2 (standardize + sort) and 3 (post to instrument-level ledger) have been adversarially audited at fixture scale — 1,520 SJE rows, zero unbalanced journals, 59 ledger rows, ledger total $0.00. Active cost allocation, booked FX revaluation, and an interpretation/reporting layer are implemented. The architecture correctly separates the Accounting Rules Engine (ARE), the Instrument Ledger (IL), and the reporting layer — which is the core claim Paper 1 makes about the minimum cost architecture.

**What is not yet done.** Full-scale performance measurement (the cost curve experiments Papers 1 and 2 reference) has not been run. Several model decisions are open — balance grain, account namespace, period representation — documented in the Next Model Decisions section below. Windows validation is pending. The GenevaERS cross-engine validation step is future work.

**What a reviewer can usefully do.** Read `README.md` and the five layer READMEs (`01-transformation/` through `05-perspectives/`) to understand the architecture. Run the fixture smoke test (`bash scripts/run_repo_fixture_smoke_test.sh --show-data`) to see the pipeline execute. Treat the current implementation as a working architectural proof-of-concept at fixture scale, not a completed empirical result.

---

## 2026-09-30 Conceptual Model and 99 Handoff

- Branch history: this work was developed on `work/99-reporting`, now merged to `main`. The current 99 work is planning/documentation, not an implemented query service.
- A public [Sharealedger Conceptual Model repo](https://github.com/sharealedger-org/sharealedger-conceptual-model) now holds the conceptual model discussion, authorized April 2020 lecture slides, and a GenevaERS ERP discussion starter.
- The new [CKB view-query prototype plan](99-interpretation/CKB_VIEW_QUERY_PROTOTYPE.md) scopes 99 as a request/evidence interaction layer and treats scans as separately identified execution work.
- `docs/VA Data Samples and Specs.xlsx` exposes a VA-to-Universal-Journal mapping not implemented in the current Scala transform: intended Project=Program and Account=Object/Source; current code maps Project=Object, derives EXP/REV accounts, and drops Program. The workbook's wide Universal Balance also sketches Contract/Commitment/Balance IDs and 12 period amount slots; these are not yet settled as keys versus measures.
- The workbook-based reference cardinalities and dense bounds in the prototype plan are provisional domain ceilings, not observed FDS sizes or implementation evidence. The checked-in VendorMaster and VA data are fixture-scale.
- No UL accounting code changed for this model review. Do not upgrade audit states or claim the generalized CKB/query behavior until balance identity, account namespace, period representation, source mapping, and a falsifiable producer/consumer workload are agreed and tested.

### Next Model Decisions

1. Define the VA universal balance grain and the role of Contract, Commitment, Balance ID, and Instrument/Involved Party IDs.
2. Decide whether Account is a shared numeric namespace or type-qualified Object/Source domain, and define the crosswalk.
3. Decide whether the canonical balance is long-form by period or the workbook's wide period-slot representation; treat report pivots as a separate materialization choice.
4. Reconcile workbook mapping rows against source and reference data, including the program reference miss, before implementing the transformation change.
5. Select one named chartfield cut and prove its source, grouping, reconciliation, update/replay, and measured storage behavior before connecting it to 99.

## Architecture Source of Truth

- `01-transformation/` through `05-perspectives/` define logical ownership, not a fixed sequence of processes or physical passes.
- `99-interpretation/` defines the terminal post-run interpretation contract. A first interpreter and automatic run hook now exist for emitted evidence bundles, including the booked currency-revaluation fixture.
- The current Virginia Scala application is a legacy physical baseline with mixed responsibilities. Its numbered options are not a one-to-one implementation of layers 01-05.
- The original [Universal Ledger prototype](https://github.com/KipTwitchell/universal_ledger) is the domain lineage bridge: raw VA transaction assignment, Universal Journal generation, CKB-style balance updates, and multiple outputs from one pass. It is a POC, not the generalized GenevaERS engine.
- Start with [README.md](README.md), the five layer READMEs, [CKB_LAYER_EXECUTION_MODEL.md](docs/CKB_LAYER_EXECUTION_MODEL.md), and [MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md).

## Current Implementation and Build

- Mainline financial processing is Scala; Bash orchestrates; Python is restricted to preparation, inspection, and post-run analysis. See [AGENTS.md](AGENTS.md).
- The current Scala project is `02-foundation/va_pipeline/`, using Scala 2.12.18 and sbt 1.10.10.
- Local compile command:

  ```bash
  cd 02-foundation/va_pipeline
  sbt -batch compile
  ```

- `data/` contains small sanitized samples and configuration fixtures. Full VA and PO histories remain external; see [RUNTIME_DATA_LAYOUT.md](docs/RUNTIME_DATA_LAYOUT.md).
- The PostgreSQL JDBC dependency is present in the build, but the database-backed Perspective aggregation design is not implemented. Decisions are tracked in [step5-postgres-design.md](docs/step5-postgres-design.md).
- PostgreSQL is intentionally deferred. The current user-facing smoke path is the compact `--show-data` output and the file-based interpretation report.
- Spark, Vagrant, Play, and Derby are excluded from the cost-curve experiment. PostgreSQL is optional only as a later landing/access layer for event and view copies, not as part of the measured CKB path.

## Validation Status

- The Scala project compiled on macOS with JDK 21.
- A small FY2003 fixture run exercised the current VA path through its legacy financial operations on macOS. It verified selected balance and reconciliation controls, but does not validate full-scale performance or generalized 04/05 implementations.
- A separate Windows handoff branch contains fixes from that run and a fixture runner check. Windows validation remains pending.
- The canonical run, process, sort, partition, and reconciliation evidence is emitted for the baseline and active allocation smoke profiles. Engine/perspective evidence is still partial.
- The active allocation profile generates 45 balanced allocation SJEs, records an explicit rounding residual, applies them, and regenerates 27 next-period divisor groups.
- The first 99 interpreter and automatic end-of-run hook report active/partial status and missing future evidence rather than overstating coverage. The booked FX smoke emits engine metrics, reconciliation controls, and a run report with fixture-level `supports` evidence.
- The populated booked FX orchestrator smoke now stages its opening ledger, generates 4 balanced SJEs, posts them with 3 opening rows into 5 ledger rows, and passes conservation controls. Prior committed ledger state is staged per attempt; a failed run after Step 3 output leaves the published generation unchanged, removes the failed attempt, and retains its log.
- Successful runs publish a complete generation behind the output path. Older generations remain on disk for readers and are not counted in the active output's cost record. First-time migration of an existing directory to a symlink has a brief non-atomic interval; concurrent-reader behavior, retention costs, input-generation identity, and whole-process replay remain unverified.
- The agentic architecture gates in [AGENTIC_ARCHITECTURE_GATES.md](docs/AGENTIC_ARCHITECTURE_GATES.md) are now mandatory after a rejected engine attempt exposed raw-event input, non-SJE output, orphaned materialization, and false CKB placement.

## Next Work

1. Validate the current fixture and active allocation profile on Windows using the agreed Bash environment.
2. Audit and repair the measurement substrate: process rows, artifact bytes, sort/pass records, controls, and replication identity.
3. Build named minimum-cost workloads and materialization profiles without turning the Scala prototype into a generalized engine.
4. Use GenevaERS Workbench, Run-Control Apps, Performance Engine, and CKB extensions as the future execution target when a workload is ready for cross-engine validation.
5. Keep Sharealedger as the future metadata/configuration home for the open ERP model; keep Ledger Lab focused on research fixtures, controls, and cost evidence.
6. Defer PostgreSQL until the file-based perspective/report contracts and materialization profiles are stable.
7. Evaluate using the VA fixture data as a test case for generating a GenevaERS business-data model from scratch — i.e., deriving chartfields, account structure, and processing rules bottom-up from observed transaction data rather than from a pre-specified schema. This would exercise the open ERP model derivation path and could inform how Sharealedger's conceptual model handles onboarding from raw financial history.

## Current Session Boundary

The project now has a coherent pre-99 foundation:

- Rule-based VA ARE output with explicit debit/offset legs and rule identity.
- Public VA object classification vocabulary separate from policy.
- Reproducible allocation divisor generation from raw or ledger input.
- Active allocation smoke profile with generated SJEs, rounding control, and next-period divisor handoff.
- Canonical evidence and first interpretation report.
- Named workload and materialization controls.
- Synthetic, clearly labeled inputs for intercompany candidates, USD/EUR/BTC currency views, and booked currency revaluation.
- A booked currency-revaluation engine slice that generates balanced SJE, applies it through the existing poster, and passes fixture controls; baseline and allocation regressions pass after the streaming merge fix.

The next work is validation of booked-revaluation rate/period edge cases, walk-forward behavior, and
target CKB placement, followed by the scoped elimination design. Do not treat the synthetic fixtures
as public Virginia evidence or the legacy poster path as generalized GenevaERS CKB evidence.

## Rejected Engine Attempt

The 2026-09-29 attempt was discarded. It passed a synthetic balance check but violated the architecture: it read raw fixture CSVs from an engine package, emitted a custom non-SJE file, ran after the post merge loop, never applied generated rows to ledger state, eagerly loaded inputs, and ignored materialization behavior. A same-process call is not a CKB engine section. The replacement must pass [AGENTIC_ARCHITECTURE_GATES.md](docs/AGENTIC_ARCHITECTURE_GATES.md) before any currency/elimination result is interpreted as evidence.

## First Adversarial Audit: Steps 2 and 3

Audit role: `Adversarial Reviewer`
Claim: Step 2 produces a valid sorted SJE partition that Step 3 can post into conserved instrument-level ledger state.
Command: `bash scripts/run_pipeline.sh --inPath "$RUN_ROOT/input" --outPath "$RUN_ROOT/output" --years 03 --steps 2,3 --curve audit --config step2-step3 --profile audit-baseline --materialization baseline`
Evidence root: `/tmp/ledger-lab-audit.styOkT`

Verified for the staged FY03 fixture:

- 1,520 SJE rows;
- 39 fields on every SJE row;
- 0 unbalanced journal groups;
- 0 sort-key monotonicity violations;
- 59 ledger rows;
- ledger amount total `0.00`.

Audit classifications:

- Step 2 transformation: `partially_verified`; the basic schema and balance invariant pass, but record-format coverage, vendor misses, lineage semantics, and scale remain unverified.
- Step 2 sort: `partially_verified`; the fixture key check passes, but adversarial multi-spill, numeric/order, and memory tests remain.
- Step 3 posting: `partially_verified`; the fixture reconstructs a conserved ledger, but prior-state replay, duplicate keys, multi-period behavior, and full lineage remain unverified.
- Process metrics: `contradicted` on macOS; the log reports Step 3 `records_read=0` despite 1,520 journals, storage is `?`, and Step 2 canonical input rows include the header (`761` reported versus `760` data rows).

This audit does not establish production scale, GenevaERS compatibility, generalized 04/05 maturity,
or a cost-curve result. The next audit should repair metric production before measuring workloads.

## Windows Validation (pending)

An earlier Windows attempt pasted placeholder paths from the smoke-test output as literal commands, so every `head`/`wc`/`cat` failed with "No such file or directory". That was a usage error, not a pipeline result. Run the smoke test itself and copy the actual retained path only when using `--keep-output`:

```bash
bash scripts/run_repo_fixture_smoke_test.sh --show-data
```

## Starting Points for a New Collaborator

1. Read [AGENTS.md](AGENTS.md) (language boundaries, review gate) and [README.md](README.md).
2. Read the five layer READMEs, then [CKB_LAYER_EXECUTION_MODEL.md](docs/CKB_LAYER_EXECUTION_MODEL.md) and [MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md).
3. Run the fixture smoke test above, then see "Next Model Decisions" and "Next Work" in this file for open items.
4. Before any engine or perspective work, follow [AGENTIC_ARCHITECTURE_GATES.md](docs/AGENTIC_ARCHITECTURE_GATES.md) and [VERIFICATION_SESSION_PROTOCOL.md](docs/VERIFICATION_SESSION_PROTOCOL.md).
