# Measured Results Summary — 14-Year Virginia Corpus (Paper 1, Rev 2)

**Status:** `verified` / `partially_verified` (as indicated per metric)  
**Corpus Scope:** State of Virginia Public Data, Fiscal Years 2003 through 2016 (14 years)  
**Measured Infrastructure:** Single 4-CPU, 16 GB RAM Host; 1 GB JVM Max Heap  
**Source Records:** Zenodo parent `10.5281/zenodo.23846362` (eVA purchase orders FY2001–FY2018, APA budget files, metadata) and APA expenditure and revenue records `24015791`, `24021539`, `24022681`, `24022832`; restored and SHA-256 verified by `scripts/download_corpus_zenodo.sh` against `data/corpus_profile/source_catalog.csv`.  

---

## 1. Corpus Scale & Volume Totals

The Scala CKB engine processed the full 14-year historical expenditure and purchase order corpus with the following physical and logical counts:

| Dimension / Metric | Measured Value | Unit / Format | Status |
|---|---|---|---|
| **Raw Input Files** | `385` | CSV files | `verified` |
| **Raw Input Volume** | `27.8` | Gigabytes (GB) | `verified` |
| **Standardised Journal Entries (SJE)** | `224,072,410` | 39-field balanced rows | `verified` |
| **Raw Merged SJE Data Size** | `56.69` | Gigabytes (GB) | `verified` |
| **Monthly Instrument Ledger (`LDGR`) Rows** | `10,651,908` | Key-period movement rows | `verified` |
| **Monthly Instrument Ledger Size** | `1.715` | Gigabytes (GB) (~161 B/row) | `verified` |
| **Posted General Ledger (`GL`) Keys** | `6,433,714` | Distinct account keys | `verified` |
| **Total Committed Output Footprint** | `110 – 116` | Gigabytes (GB) on disk | `verified` |
| **Quarantined PO Rows** | `32,000` | Malformed source rows (14 files) | `verified` |
| **Permutation Capacity Upper Bound** | `503,118,463,033,110` | Combinatorial capacity sum | `verified` |

---

## 2. Monthly Point Measurements (Three 14-Year Corpus Runs)

Three complete 14-year executions were performed at the **Monthly** point ($X=30$ / P3 / CCP2). All three runs completed with zero errors and passed all financial control balancing totals.

| Run Identifier | Execution Topology | Wall Time (s) | Step CPU (s) | Peak Memory | Balance Controls |
|---|---|---|---|---|---|
| **Baseline (`20261110_210721`)** | Serial (years in sequence, `5v` last) | 11,695 s | 10,670 s | 1.35 GB | 42 / 42 PASS (`verified`) |
| **Run 3 (`20261111_014229`)** | 2-chain parallel, early `5v` start | 8,147 s | 11,179 s | 1.36 GB | 42 / 42 PASS (`verified`) |
| **Run 4 (`20261111_064052`)** | 2-chain parallel, `caffeinate` active | 8,544 s | 11,207 s | 1.36 GB | 42 / 42 PASS (`verified`) |

### Deterministic Output Equivalence (`verified`)
* **Data File Identity:** All 74 data files across 14 fiscal years listed in [`gold/MANIFEST.csv`](../../data/test_evidence/corpus_runs/run_20261111_014229/gold/MANIFEST.csv) (`LDGR*`, `GL*`, `PERM_TBL*`, `PERM_SUMMARY*`, `POQuarantineFY*`, and 4 summary views) are **100% byte-identical (0 diffs, 0 missing)** across Run 3 and Run 4.
* **Merged SJE Hash Identity:** All 14 merged SJE files produced in Run 3 and Run 4 share identical SHA-256 digests (`MANIFEST_SJE.csv`).

---

## 3. Financial Process Layers (CCP1 through CCP5)

| Process Point | Engine Layer / Description | Measured Status | Key Measurement |
|---|---|---|---|
| **Posted GL (CCP1)** | `GLPost` collapsing dimensions to `(agency, fund, object, nominal, period, dir)` | `verified` | 6,433,714 keys over 14 years. Discards instrument identity. |
| **Ledger (CCP2)** | `post.scala` streaming Instrument Ledger movements | `verified` | 10,651,908 movement rows, 1.715 GB total store. |
| **Views (CCP3)** | `CKBViews.scala` generating 4 analytical summary views | `verified` | Step `5v` elapsed: 1,285 s (Baseline) to 1,696 s (Run 4). |
| **Revaluation (CCP4)** | Multi-currency revaluation / period reclassification SJE generation | `not run` at corpus scale | Verified on unit test fixtures only. |
| **Allocation (CCP5)** | Prior-day divisor allocation across cost centers | `not run` at corpus scale | Verified on unit test fixtures only. |

---

## 4. Hardware & Execution Diagnostics

1. **JVM Memory Ceiling:** The entire 224-million-row pipeline executed with a **1 GB JVM heap** (`-Xmx1024m`). Peak process memory observed across all OS monitoring was ~1.36 GB.
2. **Contention Premium:** Parallel execution across 2 lanes on a 4-CPU host reduced elapsed wall clock time by 30.3% but increased step CPU by ~15% to 17% due to CPU cache and thread contention. Controlled quiet-host testing confirmed the single-chain CPU is identical to baseline (+0.4%).
3. **Orchestration & Verification Tail:** Approximately 54% of chain elapsed wall time occurs after the Scala engine completes ("Pipeline complete"), driven by single-threaded `awk` control-total summation in Bash.
