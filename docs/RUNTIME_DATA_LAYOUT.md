# External Runtime Data

The repository keeps only small fixtures and metadata. Full Virginia expenditure, revenue,
budget, and purchase-order files belong outside Git.

## Directory layout

Set `UL_DATA_ROOT` or pass `--data-root` to a pipeline runner. The root should contain:

```text
<UL_DATA_ROOT>/
  VARawFiles/                 # FY03-FY16 expenditure, revenue and budget files
    FY 2003 Expenditure/Expenditures Jul 2002.csv   # as downloaded: one folder per FY and type,
    ...                                             # 12 monthly CSVs each; Budget FY20NN.csv at top level
    # The FY03q1exp.txt-style names below are the repo's small-fixture / Step 2 input names,
    # not the names of the downloaded files (checked 2026-10-31: 350 files, none named that way).
    FY03q1exp.txt
    FY03q2exp.txt
    FY03q3exp.txt
    FY03q4exp.txt
    FY03rev.txt
    ...
  VAPOData/                   # period PO files used by the CAR update cycle
    VA_opendata_FY2003.txt
    ...
  VendorMaster_full.csv       # full instrument master used for Step 2
  VendorMaster_enriched.csv   # rolling/enriched master used by the orchestrator
  ViewSpec.csv                # copy from the repository when changing views
  output/                     # generated SortedJE, LDGR, and view files
  logs/                       # generated run logs
```

The source files may use `.csv` rather than `.txt` for later fiscal years. The orchestrator
checks both extensions for Step 2 input files.

## Zenodo archive (Virginia APA + eVA data, FY2003–FY2016)

Start from a clean repository checkout and a clean external data root. Do not mix older
raw-data directories or previously downloaded archive trees into the same working copy.
The project is designed to run from a reproducible external data root, not from a
repository-managed copy of the historical archive.

All Virginia source data used by this project is archived on Zenodo as Virginia
public-domain government records, in **five records** (checked against the public Zenodo API
2026-11-09; every file's size matches `data/corpus_profile/source_catalog.csv`):

| Zenodo record | Contents | Files |
|---|---|---:|
| [`23846362`](https://doi.org/10.5281/zenodo.23846362) (parent) | eVA PO `VA_opendata_FY2001–FY2018.txt` (no FY2009), `Budget FY20NN.csv`, `20NN Combined.xlsx`, dictionary, contract list, derived vendor files | 47 |
| [`24015791`](https://doi.org/10.5281/zenodo.24015791) | APA expenditure FY2003–FY2009 | 84 |
| [`24021539`](https://doi.org/10.5281/zenodo.24021539) | APA expenditure FY2010–FY2016 | 84 |
| [`24022681`](https://doi.org/10.5281/zenodo.24022681) | APA revenue FY2003–FY2009 | 84 |
| [`24022832`](https://doi.org/10.5281/zenodo.24022832) | APA revenue FY2010–FY2016 | 84 |

The parent record alone is **not** the whole corpus: it has no monthly APA files. Record
`24023167` (eVA supplier directory snapshot, 7 files) is not a pipeline input. Five partial
duplicates with the same expenditure title (`23851758`, `24014676`, `24014839`, `24014938`,
`24015422`, 1–48 files each) came from the 2026-11-06 network retries and were deleted on
2026-11-09; their DOIs now show a tombstone. Zenodo serves the four APA records as flat file
names, so the per-FY folder structure is restored from the catalog's `rel_path`, not from Zenodo.

The parent archive contains the downstream dataset bundle for the project:

| Directory | Source | Years | Status on original site |
|-----------|--------|-------|--------------------------|
| `VARawFiles/` | Virginia APA `datapoint.apa.virginia.gov` | FY2003–FY2016 | Available (may be removed) |
| `VAPOData/` | Virginia eVA `data.virginia.gov` | FY2001–FY2018; no FY2009 `.txt` (see "PO coverage") | FY2003–FY2015 already removed |

> **Checked 2026-10-31 against the Zenodo API:** the parent record lists 47 files, all matching
> local copies by size and MD5, but it does **not** list the 336 monthly APA expenditure/revenue
> files (only the 14 `Budget FY20NN.csv` files from `VARawFiles/`). The four APA records
> published 2026-11-06 supply those 336 files (checked 2026-11-09: all 336 present, none
> missing, sizes equal to the catalog). Details in
> [CORPUS_ACCESS.md](CORPUS_ACCESS.md#zenodo-record-check-2026-10-31-via-the-public-zenodo-api).
> The parent record's metadata was corrected in place on 2026-11-09 (license `other-pd`, FY2001–FY2018 title,
> FY2005/FY2009 wording fixed, `hasPart` links to the four APA records); see
> [ZENODO_ARCHIVE_PLAN.md](ZENODO_ARCHIVE_PLAN.md#metadata-correction-published-2026-11-09).

### Canonical clean-start download

```bash
export UL_DATA_ROOT=$HOME/workspace/va_fresh_root      # must be outside the repository
bash scripts/download_corpus_zenodo.sh --data-root "$UL_DATA_ROOT" --dry-run   # plan and byte totals
bash scripts/download_corpus_zenodo.sh --data-root "$UL_DATA_ROOT" --years 2003 --types rev,budget,po  # stage 1
bash scripts/download_corpus_zenodo.sh --data-root "$UL_DATA_ROOT"             # full corpus
bash scripts/run_pipeline_orchestrator.sh --years 2003-2016 --curve CCP3
```

[`scripts/download_corpus_zenodo.sh`](../scripts/download_corpus_zenodo.sh) is Bash file movement
plus SHA-256 verification. It reads `data/corpus_profile/source_catalog.csv` for the 385 expected
paths, sizes and hashes; maps each file to its Zenodo record by family and fiscal year; downloads
with resume and retry; verifies; and appends `logs/download_corpus_manifest.csv` under the data
root as acquisition evidence. A rerun skips files that already verify. A hash mismatch fails the
run with exit 1 and leaves no file. The two zero-byte placeholder CSVs are not on Zenodo and are
recreated empty. Selected total is 385 files, 27.84 GB; its time and storage are data preparation
and are excluded from `cost_surface.csv`.

Tested 2026-11-09 on FY2003 (`--types rev,budget,po`, 14 files, 3 min): clean download, no-op
rerun, a same-size one-byte corruption detected and replaced, resume from a truncated `.part`,
and a wrong catalog hash failing with exit 1. The full 385-file download has not been run.

**License note:** Virginia state government records are public domain (no copyright).
The archive contains vendor business contact information as published in Virginia's
eVA system. NIGP commodity code *descriptions* are not redistributed separately —
only the codes and descriptions embedded in transaction rows.

Do not commit extracted files; `VARawFiles/` and `VAPOData/` are covered by `.gitignore`.

## PO coverage (measured 2026-10-31)

From `data/corpus_profile/coverage_matrix.csv` (see [CORPUS_FINDINGS.md](CORPUS_FINDINGS.md)).
Earlier text here and in other documents called FY2005 and FY2009 "zero-row stubs" or "gaps". That is
corrected as follows.

| Fiscal year | State of `VAPOData/VA_opendata_FY*.txt` |
|---|---|
| 2001 | present, 312 records (small; dates 2001-03 to 2001-06) |
| 2002, 2003 | present, about 65k and 53k records; FY2003 dates end 2003-02-06 |
| 2004 | present, 227 records |
| 2005 | present, 1,037 records. **Not empty.** `2005 Combined Empty File.csv` is a separate zero-byte file |
| 2006–2008 | present, 1.2–1.6 million records each |
| 2009 | **no `.txt` file**. Only the zero-byte `2009 Combined no VA Data.csv` |
| 2010–2017 | present, 1.55–1.73 million records each |
| 2018 | present, 881,262 records; dates end 2018-01-09 (partial) |

Small size is not evidence of an empty file. Files hold `ORDEREDDATE` values outside the year in
their name in every year from FY2002 (3k–5k rows for FY2006 onward), so the file name is not a
filter on order date. Every year is `PRESENT_COMPLETENESS_UNKNOWN`: presence on disk does not show
that the State's source was complete.

## Downloading expenditure data directly (alternative, APA only)

FY2003–FY2016 expenditure, revenue, and budget files are still served by
`datapoint.apa.virginia.gov` (all 42 zip URLs returned HTTP 200 on 2026-11-09). This is the
direct-from-source check, not the clean-start path: it has no checksum verification and
no PO data.

```bash
python3 scripts/download_va_data.py \
  --dest "$UL_DATA_ROOT/VARawFiles" \
  --years 2003-2016 \
  --types exp,rev,budget
```

Checked 2026-11-09 for FY2003 revenue and budget (13 files): the State's folder and file names
match the catalog `rel_path`, and all 13 SHA-256 values equal the catalog. Expenditure files and
other years were not compared. Different bytes would mean the State republished a file.

## Downloading PO data

The pipeline reads the tab-delimited `VAPOData/VA_opendata_FY*.txt` files, which come from the
parent Zenodo record via `download_corpus_zenodo.sh --types po`.
[`scripts/download_va_po_data.py`](../scripts/download_va_po_data.py) fetches FY2016+ from
`data.virginia.gov` as `eVA_procurement_YYYY.csv`, a different format whose schema has not been
compared with the `.txt` files. Do not substitute it for them in a clean-start run.

## Running with external data

The default remains `data/`, so existing fixture commands continue to work. For a full runtime
corpus, use either form:

```bash
export UL_DATA_ROOT=/path/to/va-runtime-data
bash scripts/run_c1_smoke_test.sh --years 2003,2004
bash scripts/run_pipeline_orchestrator.sh --years 2003-2016 --curve CCP3
```

or:

```bash
bash scripts/run_pipeline_orchestrator.sh \
  --data-root /path/to/va-runtime-data \
  --years 2003-2016 \
  --curve CCP3
```

Generated outputs and logs stay under the external root. The repository fixtures are not
modified by an external-data run.

## Release boundary

Before publishing a new repository, verify that the public copy contains no `VARawFiles/`,
`VAPOData/`, `output/`, or runtime logs. These paths are already ignored by the repository's
`.gitignore`; the external root exists only for local execution.
