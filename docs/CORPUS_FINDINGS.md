# Corpus Findings (measured 2026-10-31)

Source: `data/corpus_profile/` produced by `scripts/profile_corpus.sh` (profiler version
`corpus-profile-1`, one full pass, about 11 minutes). Files named here are generated and can be
regenerated; see [CORPUS_ACCESS.md](CORPUS_ACCESS.md). Identity and reason-code definitions are in
[CORPUS_RECORD_IDENTITY.md](CORPUS_RECORD_IDENTITY.md).

Everything below is a measurement of what is on this machine. None of it establishes that the
files are complete relative to the State's books. Every family is `PRESENT_COMPLETENESS_UNKNOWN`
in `coverage_matrix.csv`, and `source_url` / `acquisition_date` are blank in `source_catalog.csv`
because nothing in the repo proves per-file acquisition.

## Denominators

| Quantity | Value |
|---|---:|
| Files cataloged (SHA-256, size) | 385 |
| Files profiled (parsed) | 367 |
| Files cataloged only (`.xlsx`, by-year CSV, summary text, reference workbooks) | 16 |
| Zero-byte files (`EMPTY_FILE`) | 2 |
| Bytes cataloged | 27,843,834,731 |
| Data records parsed | 133,791,291 |
| Accepted | 133,464,935 |
| Quarantined | 326,356 |
| Files failing physical-line reconciliation | 0 |

`data_records_parsed = accepted + quarantined` holds overall and per file. The 16 cataloged-only
files are not in any record count. The `status.*` counters in `corpus_summary.txt` (88 clean,
279 with quarantine, 16 cataloged only, 2 empty) add to 385.

### Per family

| Family | Files | Parsed | Accepted | Quarantined | Declared-row check |
|---|---:|---:|---:|---:|---|
| `apa_expenditure` | 168 | 97,516,567 | 97,516,306 | 261 | see below |
| `apa_revenue` | 168 | 17,750,361 | 17,750,109 | 252 | see below |
| `apa_budget` | 14 | 116,782 | 116,782 | 0 | no trailer |
| `eva_po` | 17 | 18,407,581 | 18,081,738 | 325,843 | no trailer |

APA quarantine is entirely `EXTRACT_ARTIFACT` (513 lines in total). Those are SQL*Plus trailer
lines written by the extract tool, not data. For the 263 APA files that carry an `N rows selected.`
trailer, `N` equals the parsed data-record count in all 263. The FY2014–FY2016 files and one FY2003
file have no trailer, so no declared total exists to check them against; that is *absence of a
check*, not a pass.

APA budget and revenue files have `date_min`/`date_max` inside the claimed fiscal year in every
case (`out_of_fy_window = 0`, `files_with_fy_mismatch = 0`), and no expenditure row has a blank
`VENDOR_NAME`.

### Amount control totals (decimal, from `file_profile.csv`)

Sums are in dollars as written in the files. `amount_basis` is `AMOUNT` for APA and
`QUANTITYORDERED*PRICE` for PO. Totals are control totals for comparing runs, not audited
financial totals.

| Family | Signed sum | Absolute sum | Negative rows | Zero rows |
|---|---:|---:|---:|---:|
| `apa_expenditure` | 557,211,382,901.81 | 942,808,569,015.95 | 9,489,970 | 464,764 |
| `apa_revenue` | 536,125,577,596.72 | 984,730,544,230.84 | 1,454,696 | 37,427 |
| `apa_budget` | 628,029,834,726.75 | 894,977,787,193.61 | 31,289 | 324 |
| `eva_po` (qty × price) | about 115.83 billion | about 115.86 billion | 2,568 | 539,908 |

Negative-amount share per APA expenditure fiscal year is 7.6%–11.7%. Negatives are reversals,
corrections or credits in a payment system; they are not errors and are not removed.

The signed sum of an APA family is not a net "State spend". Budget rows are mixed transaction
types (`TRANSACTION_TYPE`), and whether expenditure and revenue overlap with transfers is not
established. Do not quote these sums outside this document without that caveat.

## eVA purchase-order files (`VAPOData/VA_opendata_FY*.txt`)

Seventeen files exist: FY2001–FY2008 and FY2010–FY2018. **There is no FY2009 `.txt`.**
`VAPOData/2009 Combined no VA Data.csv` and `VAPOData/2005 Combined Empty File.csv` are the only
zero-byte files.

| FY | Parsed | Accepted | Quarantined | Order-date min → max | Dates outside FY | Notes |
|---|---:|---:|---:|---|---:|---|
| 2001 | 312 | 312 | 0 | 2001-03-02 → 2001-06-29 | 0 | about four months of dates |
| 2002 | 64,743 | 62,706 | 2,037 | 2001-03-12 → 2002-06-28 | 3 | |
| 2003 | 52,666 | 51,463 | 1,203 | 2001-12-10 → 2003-02-06 | 136 | ends 2003-02-06, not 2003-06-30 |
| 2004 | 227 | 224 | 3 | 2002-10-11 → 2004-06-30 | 142 | small; 142 of 227 outside FY |
| 2005 | 1,037 | 1,013 | 24 | 2002-11-20 → 2005-04-27 | 592 | **not a zero-row stub**; 592 of 1,037 outside FY |
| 2006 | 1,212,237 | 1,160,896 | 51,341 | 2003-01-22 → 2006-06-30 | 1,493 | |
| 2007 | 1,231,902 | 1,213,158 | 18,744 | 2002-05-23 → 2007-06-30 | 4,262 | |
| 2008 | 1,580,047 | 1,559,710 | 20,337 | 2003-11-05 → 2008-06-30 | 3,198 | |
| 2009 | – | – | – | – | – | **no file** (`MISSING`) |
| 2010 | 1,551,934 | 1,527,501 | 24,433 | 2002-07-19 → 2010-06-30 | 4,216 | |
| 2011 | 1,685,674 | 1,656,494 | 29,180 | 2003-11-13 → 2011-06-30 | 3,253 | |
| 2012 | 1,689,614 | 1,658,944 | 30,670 | 2003-11-19 → 2012-06-30 | 3,585 | |
| 2013 | 1,644,951 | 1,620,638 | 24,313 | 2004-11-04 → 2013-06-30 | 4,114 | |
| 2014 | 1,691,779 | 1,664,246 | 27,533 | 2003-07-23 → 2014-06-30 | 3,479 | |
| 2015 | 1,669,909 | 1,643,476 | 26,433 | 2007-06-18 → 2015-06-30 | 4,040 | |
| 2016 | 1,714,830 | 1,686,615 | 28,215 | 2004-08-27 → 2016-06-30 | 3,638 | |
| 2017 | 1,734,457 | 1,707,580 | 26,877 | 2007-04-09 → 2017-06-30 | 4,802 | |
| 2018 | 881,262 | 866,762 | 14,500 | 2012-10-03 → 2018-01-09 | 2,365 | ends 2018-01-09, so partial year |

Reading the table:

- "Dates outside FY" counts rows whose `ORDEREDDATE` falls outside the fiscal year claimed by the
  file name. It is nonzero in every year from FY2002. The most plausible reading is that files hold
  purchase orders *active or amended* in the fiscal year rather than strictly ordered in it, but the
  repo has no documentation that says so. It is listed as unverified.
- **Do not treat a file name as a fiscal-year filter on `ORDEREDDATE`.** Filter on the date column,
  and decide explicitly what to do with the two populations.
- Earlier statements in repo documents that FY2005 and FY2009 are "zero-row stubs" were wrong for
  FY2005 (1,037 records) and right only in the sense that FY2009 has no `.txt` at all. The
  "13 years processed" statement in the extractor handoff is not reproducible: by the extractor's
  `size_mb < 0.5` cutoff it processed 12 files and skipped FY2001, FY2004 and FY2005 for size alone.
  The cutoff is in `scripts/extract_po_attributes.py` (lines about 92 and 184).
- FY2003 ends in February 2003 and FY2018 in January 2018, so those two are partial relative to a
  full fiscal year. Whether the source itself was cut there is not known.
- FY2017 has an absolute extended-price sum of about 43.2 billion, roughly four times FY2016's
  11.7 billion with a similar record count. A one-off scan of the file (not part of the profiler)
  shows why: six lines each have quantity 1 and price 5,140,972,285.66 (`EP2539756`, `EP2539757`,
  `EP2539759`, `EP2539761`, `EP2539762`, `EP2539787`), all to health-plan vendors. Together they
  are about 30.8 billion, or 71% of the year's total. Two further lines are 280 million and
  167 million. What that price represents (a contract ceiling or capacity amount rather than an
  expenditure is plausible) is **not established** by the data. Do not use PO extended-price
  totals for any year without first looking at the extreme values.

### PO identifier shapes (profile counts, not identity)

| Shape | Meaning in the profile | Behaviour |
|---|---|---|
| `999999999999` | placeholder `VENDORID` | present in every year (1.73 million rows overall) |
| 9 digits | most common from FY2006 | about 15.87 million rows |
| two letters + seven digits | appears from FY2013 only | 389,965 rows |
| other | everything else | dominant in FY2001–FY2003 |

Blank `VENDORID` is zero in all accepted records. These are shape statistics. They do not say what
the identifier is (see the "do not infer tax IDs" rule in `CORPUS_RECORD_IDENTITY.md`).

### PO quarantine

| Reason | Records |
|---|---:|
| `FIELD_COUNT` | 306,626 |
| `UNBALANCED_QUOTE_RESYNC` | 14,927 |
| `UNTERMINATED_QUOTE` | 4,290 |

Quarantine is 1.8% of PO records overall (325,843 of 18,407,581). The mechanism is embedded line
breaks inside unquoted free-text fields, which split one logical record across several physical
lines. I inspected only a small number of samples directly. The count of *logical purchase
lines* lost to quarantine is therefore somewhere below 325,843, because one broken record produces
more than one quarantined fragment, and is not known.

`multiline_accepted` is 1,131,277 for PO: records with embedded line breaks inside a correctly
quoted field that were reassembled and accepted. The quarantine count is thus a floor on how many
lines have embedded breaks, not a measure of it.

### The 99% and 94% NIGP coverage figures

Earlier documents quote both. They are different measures and are not in conflict:

- 99%: share of PO *lines* with a non-blank NIGP code.
- 94%: share of *vendor-year rows* in `vendor_po_attributes_by_year.csv` with a non-blank class.

A vendor-year row is the unit of analysis for the by-year file, and rows from a vendor with few
lines weigh the same as large ones. Quote either with its denominator.

The extractor's own "records" counts are within 0.6%–3.8% of accepted counts here. They exclude
blank-`VENDORID` rows and use a different record reader. Matching to the unit is not expected.

## `vendor_po_attributes_by_year.csv`

An extractor derived file, not source data. 210,496 rows, fiscal years 2002, 2003, 2006–2008 and
2010–2016 (FY2001, FY2004, FY2005, FY2009, FY2017 and FY2018 are absent).

Problems measured:

- 8,101 rows have a city name in `VENDORID` and a state in `VENDORNAME`, a column shift from rows
  split by embedded line breaks. The extractor treated shifted records as real vendors.
- A further 5,250 rows have a "other" ID shape.
- 20 distinct `instGeoRegion` values are not US states or territories: Canadian provinces plus
  junk values such as `ZZ` and `XX`.
- After excluding implausible IDs, 1,392 normalized names map to more than one `VENDORID`, and
  1,128 of those carry different NIGP classes. 11,620 of 63,293 IDs have more than one name.
- No provenance header: no input checksums, extractor version, date or counts. It does not meet the
  "Derived outputs" requirement in `CORPUS_RECORD_IDENTITY.md`.

Consequences:

- Do not use this file as ground truth for vendor identity. It is a lossy, partly shifted summary.
- `scripts/enrich_vendormaster.py` keys on `UPPER(name)`. Where several by-year rows share a name,
  **the last one in file order silently overwrites the others.** Its default inputs
  (`VendorMaster_full.csv`, `vendor_po_attributes.csv`, `VendorMaster_enriched.csv`) do not exist
  on this machine, so it has not been run here.
- Neither of these is fixed in this work. Vendor identity and PO/payment linkage are Codex's
  investigation. The findings here are inputs to it.

## Reference data (`data/aRef*.txt`)

Detail and join cautions are in [CORPUS_SCHEMAS.md](CORPUS_SCHEMAS.md#reference-data).

| File | Rows | Observation |
|---|---:|---|
| Agency | 326 | 3-digit codes; 7 duplicate codes; 3 duplicate names |
| Fund | 10,645 | 558 distinct 4-digit codes; 166 of those map to more than one distinct name |
| Object | 472 | no duplicate 4-digit codes |
| Program | 2,486 | blank header column; 1 duplicate sub-program code |
| Source | 2,275 | not analysed further |

## Not verified

- Per-file acquisition: `source_url` and `acquisition_date` are blank by design.
- Completeness of any family relative to the State's own totals.
- Whether APA expenditure and revenue overlap with transfers, and what a net figure means.
- Why PO files hold dates outside their fiscal year (active versus ordered).
- Whether `.xlsx` PO copies match the `.txt` files. They are cataloged but not parsed.
- What the FY2017 5.14-billion-dollar PO prices represent (the lines are identified above; their meaning is not).
- Zenodo `10.5281/zenodo.23846362` was checked through its public API (see `CORPUS_ACCESS.md`): published; its 47 files match local copies by size and MD5; the 336 monthly APA expenditure/revenue files are **not** in it.
- Stability of results across reruns on the real corpus. Determinism is unit-tested on a fixture;
  a second full run was not compared byte for byte.
