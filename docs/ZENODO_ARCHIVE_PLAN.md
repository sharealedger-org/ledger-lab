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

## Checklist

- [x] Owner decisions resolved (2026-11-01)
- [x] Script written: `scripts/zenodo_upload.py`
- [ ] Set `ZENODO_TOKEN` environment variable and run script
- [ ] Verify each record post-upload (name, size, MD5 comparison against API)
- [ ] Update DOI references in `RUNTIME_DATA_LAYOUT.md`, `download_va_data.py`, `download_va_po_data.py`
- [ ] Update `VERIFICATION_AUDIT_REGISTER.md`: "Zenodo record is a complete archive" row stays `contradicted` until post-upload comparison passes
- [ ] Record new DOIs in `SESSION_HANDOFF.md`

## Not yet verified

- Zenodo's per-record file limit (100 reported by owner; 84-file records leave headroom).
- Whether Zenodo accepts creating a new version via the REST API without the UI (script tests this).
- Whether `.xlsx` PO copies match the `.txt` files.
