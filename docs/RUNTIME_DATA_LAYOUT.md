# External Runtime Data

The repository keeps only small fixtures and metadata. Full Virginia expenditure, revenue,
budget, and purchase-order files belong outside Git.

## Directory layout

Set `UL_DATA_ROOT` or pass `--data-root` to a pipeline runner. The root should contain:

```text
<UL_DATA_ROOT>/
  VARawFiles/                 # FY03-FY16 expenditure and revenue files
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

## Downloading expenditure data

The downloader writes to the repository-relative `data/VARawFiles` by default. Point it at the
external root instead:

```bash
python3 scripts/download_va_data.py \
  --dest "$UL_DATA_ROOT/VARawFiles" \
  --years 2003-2016 \
  --types exp,rev
```

Purchase-order data uses the same pattern:

```bash
python3 scripts/download_va_po_data.py \
  --dest "$UL_DATA_ROOT/VAPOData" \
  --years 2016
```

## Virginia eVA PO data: FY2003–FY2015

FY2003–FY2015 eVA procurement files are **no longer available** on data.virginia.gov. They
are archived on Zenodo as Virginia public-domain government records:

> **Zenodo archive:** https://doi.org/10.5281/zenodo.PLACEHOLDER
> *(DOI pending upload — will be updated when archive is published)*

To obtain the files:

```bash
# Manual: download the zip from the Zenodo record above, extract into VAPOData/
# The archive contains VA_opendata_FY2003.txt … VA_opendata_FY2015.txt
# plus the data-dictionary.xlsx and Combined.xlsx files.

# Verify the layout matches what download_va_po_data.py expects:
python3 scripts/download_va_po_data.py --dry-run --years 2003-2015
```

**License note:** Virginia state government procurement records are public domain (no
copyright). The archive contains vendor business contact information as published in
Virginia's eVA system. NIGP commodity code *descriptions* are not redistributed
separately — only the codes and descriptions embedded in transaction rows.

Do not commit the extracted files; they are covered by `.gitignore` (`VAPOData/`).

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
