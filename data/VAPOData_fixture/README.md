# VAPOData_fixture

Small synthetic PO fixture files for smoke tests. Compatible with the FY03 expense fixture in `data/FY03q1exp_small.txt`.

These files are committed to the repository and safe to use on any machine without external data.  
They are **not** derived from the real Virginia PO open dataset.

## Files

| File | Description |
|------|-------------|
| `VA_opendata_FY2003_small.txt` | 10 synthetic FY2003 PO rows. Agency = Virginia Commonwealth University (key 267), vendor = BLACKWELLS BOOK SERVICES and Supply Room. Tab-delimited, quoted fields, 19 columns matching the real VA PO schema. |

## Real data

Full VA PO data lives in `data/VAPOData/` (git-ignored). That directory is populated manually or via download — see `RUNTIME_DATA_LAYOUT.md` for the layout contract.

The Scala pipeline expects PO files at `<inPath>/poData/VA_opendata_FY20YY.txt` (set in `LedgerApp.scala`).
