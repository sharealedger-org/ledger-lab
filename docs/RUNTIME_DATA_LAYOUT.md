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

All Virginia source data used by this project is archived on Zenodo as Virginia
public-domain government records:

> **Zenodo archive:** https://doi.org/10.5281/zenodo.23846362

The archive contains two datasets in a single Zenodo record:

| Directory | Source | Years | Status on original site |
|-----------|--------|-------|--------------------------|
| `VARawFiles/` | Virginia APA `datapoint.apa.virginia.gov` | FY2003–FY2016 | Available (may be removed) |
| `VAPOData/` | Virginia eVA `data.virginia.gov` | FY2001–FY2018; no FY2009 `.txt` (see "PO coverage") | FY2003–FY2015 already removed |

> **Checked 2026-10-31 against the Zenodo API:** the published record lists 47 files, all matching
> local copies by size and MD5, but it does **not** list the 336 monthly APA expenditure/revenue
> files (only the 14 `Budget FY20NN.csv` files from `VARawFiles/`). Treat the record as a partial
> archive. Details in [CORPUS_ACCESS.md](CORPUS_ACCESS.md#zenodo-record-check-2026-10-31-via-the-public-zenodo-api).

To use the archive:

```bash
# Download the zip from the Zenodo record, extract, then point the pipeline at it:
export UL_DATA_ROOT=/path/to/extracted/virginia-apa-eva-data
bash scripts/run_pipeline_orchestrator.sh --years 2003-2016 --curve C2
```

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

## Downloading expenditure data directly

FY2003–FY2016 expenditure, revenue, and budget files are still available on
`datapoint.apa.virginia.gov`. To download fresh copies instead of using the Zenodo archive:

```bash
python3 scripts/download_va_data.py \
  --dest "$UL_DATA_ROOT/VARawFiles" \
  --years 2003-2016 \
  --types exp,rev
```

## Downloading PO data

FY2016+ eVA procurement files are still on `data.virginia.gov`:

```bash
python3 scripts/download_va_po_data.py \
  --dest "$UL_DATA_ROOT/VAPOData" \
  --years 2016
```

FY2003–FY2015 PO files are no longer on the portal — use the Zenodo archive above.

## Running with external data

The default remains `data/`, so existing fixture commands continue to work. For a full runtime
corpus, use either form:

```bash
export UL_DATA_ROOT=/path/to/va-runtime-data
bash scripts/run_c1_smoke_test.sh --years 2003,2004
bash scripts/run_pipeline_orchestrator.sh --years 2003-2016 --curve C2
```

or:

```bash
bash scripts/run_pipeline_orchestrator.sh \
  --data-root /path/to/va-runtime-data \
  --years 2003-2016 \
  --curve C2
```

Generated outputs and logs stay under the external root. The repository fixtures are not
modified by an external-data run.

## Release boundary

Before publishing a new repository, verify that the public copy contains no `VARawFiles/`,
`VAPOData/`, `output/`, or runtime logs. These paths are already ignored by the repository's
`.gitignore`; the external root exists only for local execution.
