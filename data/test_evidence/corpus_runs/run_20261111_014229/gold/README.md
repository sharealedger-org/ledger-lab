# LedgerLabOutputs-Gold

Reference copy of the final outputs of the Scala (GenevaERS-simulation) engine over the Virginia corpus, FY2003 to FY2016, monthly granularity. Use it as the standard to compare another engine's output against.

- **Source run:** tag `20261111_014229`, repository `ledger-lab` at commit `d500af4`, run-2 layout (chain pool width 2, early `5v`).
- **Verified:** orchestrator exit 0; 42 of 42 reconciliation controls PASS; the `LDGR*`, `PERM_SUMMARY*` and four view files are byte-identical to the earlier serial run `20261110_210721` (the first valid full run).
- **Evidence in the repository:** `data/test_evidence/corpus_runs/run_20261111_014229/` (control results, `corpus_totals.csv`, wall-clock, cost surface). Audit register entry "2026-11-11 Full corpus run 3".
- **Contents:** per year `FY03` to `FY16`: `LDGR<yyyy>.csv` (ledger), `GL<yyyy>.csv`, `PERM_TBL<yyyy>.csv`, `PERM_SUMMARY<yyyy>.csv`, `POQuarantineFY<yy>.csv`, `cost_surface.csv`, `pipeline_results.log`; and `VIEWS/` with the four `view_*.csv` files. 104 files, about 2.5 GB.
- **Not included:** the merged SJE (`SortedJEFY<yy>merged.csv`, 56.7 GB) and the sorted prep files (about 8.5 GB). They can be regenerated from the raw files with the repository scripts. The raw Virginia files are not here.
- **Integrity:** `MANIFEST.csv` lists `rel_path,bytes,sha256` for every file. Check with `shasum -a 256` after OneDrive finishes syncing.
- `cost_surface.csv` and `pipeline_results.log` carry timings and run ids; they will differ between runs and should not be used for a byte comparison.

Do not edit or add files here.

---

**About this copy (in git).** This directory holds only the small files from the reference copy: this README, `MANIFEST.csv`, and each year's `cost_surface.csv` and `pipeline_results.log` (15 pairs with `VIEWS`). The 15 `pipeline_results.log` files are packed in `run_logs_gold_20261111_014229.tar.gz` in this directory, so no loose `.log` file is tracked; unpack it here to restore them. 18 files in all: this README, `MANIFEST.csv`, 15 `cost_surface.csv` and the archive. `MANIFEST.csv` lists all 104 files of the reference copy, so most of its rows have no file here. The data files themselves (about 2.5 GB) are in OneDrive at `LedgerLabOutputs-Gold` and are not in git; verify them against the manifest's `sha256` column. (`.gitignore` excludes `*.log`, hence the archive.)
