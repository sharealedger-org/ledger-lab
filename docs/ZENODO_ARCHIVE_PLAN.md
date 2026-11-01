# Zenodo Archive: Findings and To-Do (2026-10-31)

Status: plan and checklist. **Nothing here has been uploaded or changed on Zenodo.** Measurements are
from the public Zenodo API and the local catalog (`data/corpus_profile/source_catalog.csv`).

Existing record: `10.5281/zenodo.23846362` (concept DOI `10.5281/zenodo.23846361`), published
2026-10-31, 47 files, 6.03 GB. Background and the file-by-file comparison are in
[CORPUS_ACCESS.md](CORPUS_ACCESS.md#zenodo-record-check-2026-10-31-via-the-public-zenodo-api).

## Constraint

The owner reports a limit of 100 files per Zenodo record, so the corpus (385 local files, 27.84 GB)
cannot be one record. Zenodo's current file-count and size limits were **not** checked against its
documentation here; confirm them before uploading. The plan below keeps every record at or under 84
files, which leaves headroom if the limit is 100.

## What is wrong with the existing record

| # | Problem | Evidence | Fix |
|---|---|---|---|
| 1 | The 336 monthly APA expenditure and revenue files (21.8 GB) are not in the record | API file list has 47 files; none are `Expenditures ...` or `Revenues ...` | Upload them in the new records below |
| 2 | Description says FY2005 and FY2009 eVA files "contain zero rows" | FY2005 `.txt` has 1,037 parsed records; FY2009 has no `.txt` (only a zero-byte placeholder locally) | Rewrite the known-gaps sentence from `CORPUS_FINDINGS.md` |
| 3 | Description says APA fields include "agency code" and "sub-program" | The expenditure header has `AGENCY_NAME`, `FUND_NAME`, `OBJECT_NAME`, `PROGRAM_NAME`, `VENDOR_NAME` only: names, no codes, no sub-program | Correct the field list; point to `CORPUS_SCHEMAS.md` |
| 4 | Text says "public domain" but the license field is `cc-by-4.0` | Record metadata | **Owner decision** (see below) |
| 5 | `vendor_po_attributes_by_year.csv` is archived beside source files | It is a derived file: 8,101 column-shifted rows, no provenance header | Move to the derived/metadata record, labeled derived, with inputs and version; or omit |
| 6 | Per-file checksums are MD5 only | Zenodo lists `md5:...` | Include a manifest with SHA-256 (the catalog's) and MD5 |
| 7 | Directory structure is flattened | Zenodo records are flat; local files sit in `FY 2003 Expenditure/` etc. | Manifest maps each file name to its original relative path. Base names are unique across all 385 files (checked), so no collisions |
| 8 | Title says FY2003–FY2016 but the PO files span FY2001–FY2018 | Record title | Retitle |
| 9 | Creator is listed only as "Commonwealth of Virginia"; no preparer, acquisition date or source URL per file | Record metadata; `source_url`/`acquisition_date` are blank in the catalog | Owner decision; do not invent acquisition dates |
| 10 | Vendor business email addresses are included | 13.35 million of 18.08 million accepted PO records have a non-blank `VENDORLOC_EMAILADDRESS` | **Owner decision** (see below) |
| 11 | Repo docs and scripts cite one DOI as the whole archive | `RUNTIME_DATA_LAYOUT.md` lines about 34–57, `download_va_data.py` lines 11–12, `download_va_po_data.py` lines 11 and 141–142 | Update after the new DOIs exist |

## Owner decisions needed before any upload

1. **License wording.** "Public domain" and `cc-by-4.0` are different statements. Choose one for the
   record. The repo asserts public-domain status for Virginia records but holds no license text from
   the source (see "Redistribution notes" in `CORPUS_ACCESS.md`).
2. **Vendor contact data.** Keep, drop or blank the email column for the PO files. Dropping it changes
   the files, so the archived bytes would no longer equal the source bytes. If you do, publish the
   modified files under a different name and say so, and keep the SHA-256 of the originals in the manifest.
3. **NIGP descriptions.** They sit inside every PO row. The repo states they are not redistributed
   separately; their permissions are not established.
4. **Whether to keep the 12 `20NN Combined.xlsx` files.** They are in the current record, cataloged
   but not parsed, and not compared to the `.txt` files.
5. **Zero-byte placeholders.** `2005 Combined Empty File.csv` and `2009 Combined no VA Data.csv` are
   local evidence of source gaps. Check whether Zenodo accepts empty files; if not, record them in the
   manifest instead.

## Proposed split (measured counts and sizes)

| Record | Contents | Files | Size |
|---|---|---:|---:|
| 1. APA expenditure FY2003–FY2009 | monthly CSVs | 84 | 8.43 GB |
| 2. APA expenditure FY2010–FY2016 | monthly CSVs | 84 | 10.63 GB |
| 3. APA revenue FY2003–FY2009 | monthly CSVs | 84 | 1.18 GB |
| 4. APA revenue FY2010–FY2016 | monthly CSVs | 84 | 1.58 GB |
| 5. eVA purchase orders | 17 `VA_opendata_FY*.txt`, plus the files in decision 4 and the data dictionary and contract list if kept | 17 to 31 | about 6 GB |
| 6. Metadata and derived | 14 `Budget FY20NN.csv`, 5 `aRef*.txt`, corpus profile outputs, manifest, key docs, labeled derived vendor file | about 35 | about 0.05 GB (about 0.14 GB with `quarantine.csv`, 88 MB; estimate, derived vendor file and docs not summed) |

Notes:

- Four APA records at 84 files each is a choice of convenience. Splitting by fiscal-year group instead
  (75 files per group of three years, five groups, 3.4 to 5.5 GB each) is equally valid and keeps each
  record to a time span, but mixes expenditure with revenue. Pick one scheme and use it consistently.
- Budget files are in record 6 rather than their own record to keep the count of records at six.
- Zipping per fiscal year would cut the file count sharply, but it hides per-file checksums and
  per-file download. It is an alternative, not the recommendation.
- Largest group is 10.63 GB, far below what a 50 GB size limit would impose, though that limit was
  also not checked here.

## How to link the records

Give every record a `related identifiers` entry pointing to the others and to the manifest in
record 6, and consider a Zenodo community to group them. Whether to upload the new files as a **new
version** of the existing record or as separate new records is an owner decision: a new version
keeps the existing DOI lineage, while separate records give clean, independent DOIs. Record 5 and
record 6 together replace the current record's contents, so the existing record's next version
should become one of them.

## To-do checklist

- [ ] Decide license wording (owner)
- [ ] Decide handling of vendor email addresses (owner)
- [ ] Decide new-version versus new-record scheme and the split scheme (owner)
- [ ] Confirm Zenodo's current per-record file and size limits and zero-byte file policy
- [ ] Produce a `release_manifest.csv`: file name, original relative path, bytes, SHA-256, MD5, family,
      fiscal year, assigned record. The catalog already has everything except MD5 and record assignment
- [ ] Write a script that builds the manifest and **checks** the split (file count at most 84 per
      record, every catalog file assigned exactly once, sizes sum to the catalog total)
- [ ] Draft corrected descriptions for each record (fix items 2, 3, 8 above; mention quarantine and the
      known data caveats in `CORPUS_FINDINGS.md`)
- [ ] Upload with a Zenodo access token read from an environment variable. Never write the token to a
      file or commit it
- [ ] After upload, re-run the API comparison used on 2026-10-31 (name, size, MD5) for every record
      and confirm total files equal the manifest
- [ ] Update the DOI references listed in item 11 and `RUNTIME_DATA_LAYOUT.md`
- [ ] Update `VERIFICATION_AUDIT_REGISTER.md`: the "Zenodo record is a complete archive" row stays
      `contradicted` until the post-upload comparison passes
- [ ] Record the post-upload state in `SESSION_HANDOFF.md`

## Not verified

- Zenodo's current limits (taken from the owner's statement).
- Whether `.xlsx` PO copies match the `.txt` files, which bears on whether to publish them.
- Whether Zenodo permits changing the license and description of a published record without a new
  version. The record exposes a file-modification request link; its rules were not read.
