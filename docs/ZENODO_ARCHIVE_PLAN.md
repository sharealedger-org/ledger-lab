# Zenodo Archive: Findings and To-Do (updated 2026-11-01)

Existing record: `10.5281/zenodo.23846362` (concept DOI `10.5281/zenodo.23846361`), published
2026-10-31, 47 files, 6.03 GB. Background and the file-by-file comparison are in
[CORPUS_ACCESS.md](CORPUS_ACCESS.md#zenodo-record-check-2026-10-31-via-the-public-zenodo-api).

Upload script: `scripts/zenodo_upload.py` — reads `ZENODO_TOKEN` from the environment; never
writes the token to a file or stdout. Run from `ledger-lab/`.

## Owner decisions resolved (2026-11-01)

| Decision | Resolution |
|---|---|
| License | **Public domain — released government data.** No known license from the source; Virginia state records asserted public domain by the repo. Use Zenodo license id `other-pd`. |
| Vendor contact data (PO email column) | Included as published in the original government source. Keep as-is; note in description. |
| NIGP descriptions | Retained as embedded fields in transaction rows only, not as a standalone code table. |
| `20NN Combined.xlsx` files | Already in the existing record; retain. Not parsed; relationship to `.txt` files unverified. |
| Zero-byte placeholders | **Not uploaded** (Zenodo may reject 0-byte files). Documented in the manifest instead. |
| New-version vs. new-record | Existing record `23846362` gets a **new version** (corrected metadata + APA files added). APA split into two new linked records. eVA supplier snapshot is a new separate record. |

## What is wrong with the existing record and how it is fixed

| # | Problem | Fix in new version |
|---|---|---|
| 1 | 336 monthly APA expenditure and revenue files (21.82 GB) missing | Added in four new linked records (2 expenditure, 2 revenue; 84 files each) |
| 2 | Description says FY2005 eVA "zero rows"; FY2009 "empty file" | Corrected: FY2005 has 1,037 parsed records; FY2009 has no `.txt` at all |
| 3 | Description says APA fields include "agency code" and "sub-program" | Corrected: fields are names only (`AGENCY_NAME`, `FUND_NAME`, `OBJECT_NAME`, `PROGRAM_NAME`, `VENDOR_NAME`) — no codes |
| 4 | License field `cc-by-4.0` contradicts "public domain" in text | Set to `other-pd`; description says "Released government data, public domain" |
| 5 | `vendor_po_attributes_by_year.csv` archived beside source files | Noted in description as a derived file with known defects (8,101 column-shifted rows, no provenance header); to be moved to a metadata record in a future version |
| 6 | Per-file checksums MD5 only | SHA-256 per file now in the manifest uploaded with each record |
| 7 | Directory structure flattened | Manifest maps each flat file name to its original relative path |
| 8 | Title says FY2003–FY2016; PO files span FY2001–FY2018 | Title corrected to FY2001–FY2018 |

## Record plan (measured counts and sizes)

| Record | Zenodo ID | Contents | Files | Size | Status |
|---|---|---|---:|---:|---|
| A. eVA PO + metadata (existing, new version) | 23846362 | 17 PO `.txt`, 12 xlsx, dict, contract list, summary, derived vendor file, 2 empty-file markers in manifest | 47 (unchanged) | 6.03 GB | **new version with corrected metadata** |
| B. APA expenditure FY2003–FY2009 | new | 84 monthly CSVs | 84 | 8.43 GB | new record |
| C. APA expenditure FY2010–FY2016 | new | 84 monthly CSVs | 84 | 10.63 GB | new record |
| D. APA revenue FY2003–FY2009 | new | 84 monthly CSVs | 84 | 1.18 GB | new record |
| E. APA revenue FY2010–FY2016 | new | 84 monthly CSVs | 84 | 1.58 GB | new record |
| F. eVA supplier directory snapshot 2026-10-31 | new | `suppliers.csv` (230,128 rows), `audit.json`, `manifest.json`, `source_export/suppliers_source.csv`, `source_export/manifest.json`, `source_export/reconciliation.json`, `historical_candidate_details/historical_candidate_vlin_verification.csv` | 7 | ~100 MB | new record |

Records B–E are linked to record A via `related_identifiers` (relation `isPartOf`/`hasPart`).
Record F is linked to record A as a related dataset.

## Upload outcome (checked against the public Zenodo API 2026-11-09)

`zenodo_upload_progress.json` (git-ignored, local only) records five new records, all
`published` on 2026-11-06 (UTC):

| Label | Zenodo ID | Files | Bytes | Matches catalog |
|---|---|---:|---:|---|
| `apa_exp_early` | 24015791 | 84 | 8,426,163,742 | names and sizes, all 84 |
| `apa_exp_late` | 24021539 | 84 | 10,631,758,063 | names and sizes, all 84 |
| `apa_rev_early` | 24022681 | 84 | 1,183,854,971 | names and sizes, all 84 |
| `apa_rev_late` | 24022832 | 84 | 1,575,983,398 | names and sizes, all 84 |
| `supplier_snapshot` | 24023167 | 7 | 127,879,693 | not a catalog family |

Findings that differ from the plan above:

1. **The parent was not versioned (fixed 2026-11-09).** The upload left record 23846362 with 1
   version, 47 files, license `cc-by-4.0`, the old title and description (FY2005 "zero rows"),
   and no `related_identifiers`; draft `step1_draft_id` 23850746 returned 404. Corrected
   metadata was published in place on 2026-11-09 (see "Metadata correction" below), so plan
   rows 2, 4 and 8 are now fixed. No new version was needed; the record is still version 1.
2. **No cross-links (fixed 2026-11-09).** None of the five new records had `related_identifiers`
   and none had a license; the `crosslink_and_publish` step did not complete. All six records
   now carry `other-pd` and the links described below.
3. **Abandoned duplicate records: deleted 2026-11-09.** Five published records with the
   early-expenditure title held 1, 1, 2, 31 and 48 files (`23851758`, `24014839`, `24014676`,
   `24015422`, `24014938`). They were leftovers from the retries; only `24015791` is complete
   (84 files). Each was removed through the record's `request-deletion` API link (reason
   `duplicate`, policy `grace-period-v1`; Zenodo accepted all five at once). Each now returns
   HTTP 410 with a tombstone page, so the DOIs still resolve to a "removed" notice. All six
   canonical records were re-read afterwards and are intact (84, 84, 84, 84, 7 and 47 files).
4. **The "canonical = parent DOI only" rule in earlier docs was wrong.** The parent holds none
   of the 336 APA files. The canonical set is the five records in the table in
   [RUNTIME_DATA_LAYOUT.md](RUNTIME_DATA_LAYOUT.md#zenodo-archive-virginia-apa--eva-data-fy2003fy2016).
5. **Size parity, not byte parity.** Names and sizes of all 336 APA files match the catalog.
   Zenodo's MD5 values were not compared against local files in this check. The download
   script verifies SHA-256 per file instead.

## Metadata correction (published 2026-11-09)

The exact submitted metadata for each record is in [zenodo_metadata/](zenodo_metadata/)
(`<record id>.json`, deposit-API format). Changes: parent title FY2001–FY2018 and a rewritten
description (47-file scope, PO gaps as measured, restore script, companion DOIs, no "zero rows"
or "agency code" claims); license `other-pd` on all six; parent `hasPart` the four APA records and
`isReferencedBy` the supplier snapshot; each APA record `isPartOf` the parent, and each
early/late pair `continues`/`isContinuedBy`; supplier snapshot `references` the parent with
corrected file names and counts. Re-editing is possible later through the same edit/PUT/publish
sequence.

## Checklist

- [x] Owner decisions resolved (2026-11-01)
- [x] Script written: `scripts/zenodo_upload.py`
- [x] Upload run; four APA records and the supplier snapshot published 2026-11-06
- [x] Verify record contents post-upload (names and sizes, 2026-11-09); per-file SHA-256 is verified at download time by `scripts/download_corpus_zenodo.sh`
- [x] Canonical download script written and tested on FY2003: `scripts/download_corpus_zenodo.sh`
- [x] Update download scripts and `RUNTIME_DATA_LAYOUT.md`, `README.md` to the five-record set
- [x] Corrected metadata prepared for all six records in `docs/zenodo_metadata/<record id>.json` (2026-11-09). Zenodo's deposit API accepted each file (HTTP 200) in a throwaway edit session that was then discarded; public metadata was re-hashed afterwards and is unchanged. Published records can be edited in place, so the parent needs no new version.
- [x] Published the six metadata files in place on 2026-11-09 (order: 24023167, 24015791, then 24021539, 24022681, 24022832, then the parent 23846362). After each publish the public record was re-read: file counts and byte totals unchanged, license `other-pd`, links present, still version 1. The parent's 47 file checksums were identical before and after. No draft is left open.
- [x] Delete the five partial duplicate records (done 2026-11-09 via the API; tombstones remain)
- [ ] Run the full 385-file download into a fresh root and compare the manifest to the catalog
- [ ] Update `VERIFICATION_AUDIT_REGISTER.md`: "Zenodo record is a complete archive" row (see 2026-11-09 entry)

## Not yet verified

- Zenodo's per-record file limit (100 reported by owner; 84-file records leave headroom).
- Whether Zenodo accepts creating a new version via the REST API without the UI (script tests this).
- Whether `.xlsx` PO copies match the `.txt` files.
