# Corpus Access Package

How to select years, inspect fields, reproduce the profile, and take a stable sample of the
Virginia source corpus. Everything is plain CSV/text. No database, Parquet conversion or query
service is involved, and nothing here changes the Scala/Bash input contracts described in
[RUNTIME_DATA_LAYOUT.md](RUNTIME_DATA_LAYOUT.md).

This is data preparation and inspection. It is **not** part of any measured cost-curve run: do not
include its time or storage in `cost_surface.csv`.

Related: [CORPUS_FINDINGS.md](CORPUS_FINDINGS.md) (what was measured),
[CORPUS_SCHEMAS.md](CORPUS_SCHEMAS.md) (fields and reference data),
[CORPUS_RECORD_IDENTITY.md](CORPUS_RECORD_IDENTITY.md) (IDs and reason codes).

## Prerequisites

| Need | Used for |
|---|---|
| Java 21, sbt 1.10.x, Bash | `scripts/profile_corpus.sh` (the Scala profiler) |
| Python 3 (`py -3` on Windows) | `scripts/corpus_inspect.py` (standard library only) |
| About 0.8 GB heap | `SBT_OPTS="-Xmx768m"` is the wrapper default |

Windows PowerShell runs these from the `ledger-lab` directory. Use `py -3 scripts/corpus_inspect.py`
there; plain `python` may resolve to a Store stub.

## 1. Reproduce the profile

```bash
bash scripts/profile_corpus.sh                       # whole corpus, about 11 minutes
bash scripts/profile_corpus.sh --only FY2005         # subset by path substring
bash scripts/profile_corpus.sh --data-root /path/to/root --out /path/to/root/corpus_profile
```

Writes seven files to `<data root>/corpus_profile/`:

| File | One row per | Use |
|---|---|---|
| `source_catalog.csv` | file | checksum, bytes, family, claimed FY/period, schema, inspection status, completeness |
| `file_profile.csv` | profiled file | denominators, dispositions, control totals, key shapes, date range |
| `file_field_profile.csv` | file column | blank / numeric-looking / leading-zero / space-padded counts |
| `schema_catalog.csv` | distinct header | delimiter, column count, files, header |
| `coverage_matrix.csv` | family × fiscal year | present / missing / empty, periods present and missing |
| `quarantine.csv` | quarantined record | locator, reason code, original text |
| `corpus_summary.txt` | run | roll-up counts |

Outputs contain no timestamps and are deterministic for identical input bytes. To confirm on your
machine, run twice into two folders and compare hashes.

Git policy: `quarantine.csv` (about 92 MB) is git-ignored. The other six are small enough to be
versioned (the largest, `file_field_profile.csv`, is about 0.3 MB). Commit them only with the run's
`profiler_version` and say which data snapshot produced them; a catalog from another machine's
download is a different snapshot.

`source_url` and `acquisition_date` in the catalog are blank on purpose. Fill them only from
evidence that exists for the specific file.

## 2. Select years and files

```bash
py -3 scripts/corpus_inspect.py files --family eva_po --fy 2016
py -3 scripts/corpus_inspect.py files --family apa_expenditure
```

Columns: path, 16-character file ID, bytes, family, fiscal year, period, inspection status.
Families: `apa_expenditure`, `apa_revenue`, `apa_budget`, `eva_po`.

Check `coverage_matrix.csv` before relying on a year. FY2009 has no PO `.txt`, and every row says
`PRESENT_COMPLETENESS_UNKNOWN`. A file name is not a filter on the date column (see FINDINGS).

## 3. Inspect fields

```bash
py -3 scripts/corpus_inspect.py fields --file VAPOData/VA_opendata_FY2005.txt
```

Prints the schema ID, then each column with non-blank, blank, min and max length, numeric-looking,
leading-zero and space-padded counts. Use it before assuming a column is a number: PO `VENDORID`
and `VENDORPOSTALCODE`, and APA `VOUCHER_NUMBER` and `GROUPING_CODE`, look numeric and have
leading zeros. Read them as strings.

Header-only reading of a tab-delimited PO file needs care: there is a blank first line before the
header. The helper skips it; a naive `head -1` does not.

## 4. Re-read a record from its locator

Quarantine rows carry `start_byte_offset`, and file IDs are the first 16 hex characters of the
catalog `sha256`.

```bash
py -3 scripts/corpus_inspect.py fetch --file VAPOData/VA_opendata_FY2002.txt \
    --offset 531 --lines 1 --verify-checksum
```

`--verify-checksum` hashes the whole file against the catalog and stops with an error if the
bytes differ. Without it, an offset into a different snapshot returns an unrelated line silently.
Offsets count the BOM. `--lines` returns that many physical lines; quoted multi-line records need
more than one.

Checked on real data: all 2,037 single-line FY2002 PO quarantine records re-read from their
offsets matched the quarantined text. Multi-line records and other files were not checked.

## 5. Stable sample

```bash
py -3 scripts/corpus_inspect.py sample --family eva_po --fy 2005 --every 50 \
    --quarantine-per-reason 3 --out sample_eva_2005.csv
```

Selection is by `SHA-256("<file_id>@<start_byte_offset>")`, not by position or randomness. Same
file bytes, same `--every`, same sample. Rerunning writes an identical file (checked by hash).
Adding or removing other files does not change which rows from a given file are chosen.

The output lists, per row, `sample_id`, path, file ID, byte offset, start line, a disposition
hint, the reason code for quarantined rows, a `synthetic` flag (always `false` here), and the
raw line.

Limits:

- Only single-line records with the right field count are drawn from the clean population. Records
  spanning several lines are not sampled by this command. Quarantined originals are added, up to
  `--quarantine-per-reason` per file and reason.
- The sample is a convenience for inspection and tests. It is not a statistically representative
  sample and is not weighted by dollar value.
- The sampler rereads each selected file in full. A large PO file takes some minutes; narrow with
  `--fy`.

## 6. Existing pipeline inputs

Unchanged. The Scala pipeline still reads the Step 2 inputs documented in
`RUNTIME_DATA_LAYOUT.md`. The profile files are not inputs to it. The tiny teaching fixture
`data/VAPOData_fixture/VA_opendata_FY2003_small.txt` (10 synthetic rows, not real PO data)
remains the offline test input.

## Redistribution notes

These are the project's current statements, not legal findings.

| Source | What the repo says | Caution |
|---|---|---|
| APA expenditure / revenue / budget | Virginia state government records, public domain (`RUNTIME_DATA_LAYOUT.md`) | Public-domain status is asserted by the repo; no license text from the source was found. |
| eVA purchase orders | Same statement | Contains vendor business contact data, including email addresses (4.73 of 18.08 million records blank, so most rows carry one). Review before any redistribution. |
| NIGP commodity descriptions | "Not redistributed separately" | NIGP is a third-party coding system; the descriptions in transaction rows are retained, but their redistribution terms are not established. |
| `aRef*.txt` reference tables | DPB chartfield reference tables | Source, date and license not recorded in the repo. |
| `data/synthetic/`, `VAPOData_fixture/` | Synthetic, authored for tests | Safe to ship; must stay labeled synthetic. |

### Zenodo record check (2026-10-31, via the public Zenodo API)

`10.5281/zenodo.23846362` is **published** (status `published`, created and modified 2026-10-31,
no downloads yet), access `open`, license metadata `cc-by-4.0`. It lists 47 files, 6.03 GB. Compared
against this machine's files by name, size and MD5:

- All 47 listed files exist locally with identical size and MD5 (12 files over 100 MB included).
- Listed: 17 PO `.txt` files (FY2001–FY2008, FY2010–FY2018), 12 `20NN Combined.xlsx`, `StateContractList.xlsx`,
  `data-dictionary.xlsx`, `po_attributes_by_year_summary.txt`, `vendor_po_attributes_by_year.csv`,
  and 14 `Budget FY20NN.csv`.
- **Not listed: all 336 monthly APA expenditure and revenue files** (168 + 168), although the record
  description and `RUNTIME_DATA_LAYOUT.md` say the archive contains them. About 14 GB of the local corpus
  is therefore not in this record. The two zero-byte placeholder CSVs are also not listed.
- Record metadata says FY2005 and FY2009 eVA files "contain zero rows (published as empty files by
  the source)". The record's own FY2005 file (`VA_opendata_FY2005.txt`, 348,924 bytes, 1,037 parsed
  records) contradicts that for FY2005. FY2009 has no `.txt`; only the empty placeholder exists locally.
- The record says "public domain" in text but its license field is `cc-by-4.0`. Those are different
  statements. Which one the owner intends is not decided here.
- `vendor_po_attributes_by_year.csv` is archived as if it were source data. It is a derived file with the
  defects listed in CORPUS_FINDINGS.md and no provenance header.

Correcting the above probably requires a new Zenodo version (the record exposes a file-modification request link; its rules were not checked). Do not
describe the archive as a complete copy of the corpus until that is done. I did not modify the
record.

Zenodo limits the number of files per record (the owner reports 100), and the corpus has 385 files, so
completing the archive needs several records. The measured split, the list of record defects and the
upload checklist are in [ZENODO_ARCHIVE_PLAN.md](ZENODO_ARCHIVE_PLAN.md).
