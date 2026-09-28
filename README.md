# ShareALedger Ledger Lab

> **Ledger Lab** is the public research successor to the **Universal Ledger** POC.

> **This is a public repository.** All code, data, and results are open under Apache 2.0.
> The Virginia government expenditure data is public domain. Every result is independently
> reproducible from the raw data and this pipeline alone — no companion repository is required.

This repository is an executable empirical proof of two claims about enterprise financial data
architecture described in the monograph *"The Architecture of Economic State"* (Kip M. Twitchell).
The monograph will be linked here upon publication.
See also: [ledgerlearning.com](https://ledgerlearning.com/whitepapers/minimum-costs-for-financial-system-data-maintenance/)
The repository includes sanitized fixtures for
development; the full raw dataset remains external to Git and is supplied at runtime.

The data is Virginia state government vendor expenditure records, FY2003–FY2016 (~100 million
transactions, 380,885 distinct vendors), publicly available at
[data.virginia.gov](https://data.virginia.gov). The repository includes sanitized fixtures for
development; the full raw dataset remains external to Git and is supplied at runtime.

---

## The Two Claims Being Demonstrated

**Claim 1 — The Minimum Cost Curve.**
There is a mathematically definable interior minimum to the total cost of maintaining enterprise
financial data (compute + storage + reconciliation obligation). Most enterprises have drifted
past that minimum by pre-materializing balance structures for every anticipated reporting cut.
The pipeline demonstrates where the minimum lies by running the same data through four process
configurations (C1–C4) and measuring all three cost dimensions at each.

**Claim 2 — The Instrument Pivot Theorem.**
A balance store keyed on Instrument ID, joined at query time to a Subledger Attribute Ledger
(SAL) containing instrument attributes, answers *every possible reporting cut* across *all*
combinations of those attributes at **zero marginal storage cost per new reporting dimension**.
The equivalent key-embedded architecture requires O(v^m) balance records. At m=3 attributes and
v~480 attribute values on FY2003 data, that is a **1,378× forced-rebuild amplification factor**
— measurable directly from the pipeline output.

The same design choice (Instrument ID as anchor, SAL as attribute reservoir) simultaneously
minimizes cost *and* maximizes query completeness. The legacy architecture fails on both
simultaneously.

---

## Repository Structure

Contributor and AI-agent language rules are defined in [AGENTS.md](AGENTS.md). In particular,
Scala is the measured financial engine, Bash is the stable orchestration language, and Python is
restricted to editing, inspection, and ad hoc operations.

Ledger Lab is organized conceptually as five execution layers. The current physical paths remain stable
while the fresh repository is being compiled and corrected:

```text
01-Transformation      01-transformation/README.md
  Raw source/product records -> normalized events -> balanced SJEs

02-Foundation           02-foundation/README.md
  Current implementation: 02-foundation/va_pipeline/, scripts/, 02-foundation/sort_specs/, data/

03-Instrument Ledger    03-instrument-ledger/README.md
  Canonical Universal Journal and instrument-level state

04-Engines              04-engines/calculation_engines/README.md
  SJE-in / SJE-out history-dependent calculation processes

05-Perspectives         05-perspectives/README.md
  Aggregation-only Finance, Tax, MIS, Risk, and Liquidity outputs
```

Transformation creates SJEs. Foundation and the Instrument Ledger preserve and apply them.
Engines read and generate SJE partitions. Perspectives aggregate only. See
[01-transformation/README.md](01-transformation/README.md),
[02-foundation/README.md](02-foundation/README.md),
[03-instrument-ledger/README.md](03-instrument-ledger/README.md),
[04-engines/calculation_engines/README.md](04-engines/calculation_engines/README.md), and
[05-perspectives/README.md](05-perspectives/README.md) for the boundaries.

These are logical layers, not mandatory physical passes. See
[docs/CKB_LAYER_EXECUTION_MODEL.md](docs/CKB_LAYER_EXECUTION_MODEL.md) for the sort-boundary,
pipe, and in-memory execution model.

See [docs/MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md) for the canonical run,
process, partition, sort, engine, perspective, and reconciliation metrics.

The implementation paths below are currently the physical Foundation Layer paths:

```
02-foundation/va_pipeline/  Virginia Scala pipeline — all nine Financial System Patterns (FSPs)
  src/main/scala/
    ledger/
      LedgerApp.scala        CLI dispatcher (entry point)
      standardizeAndSort.scala  Step 2: raw → SortedJE (ARE/Transform, external merge-sort)
      post.scala             Step 3: SortedJE + LDGR → new LDGR (AL/Post, CKB match-merge)
      dataAggregation.scala  Step 5: LDGR × VendorMaster × ViewSpec → view CSVs
      arrangementReclass.scala  Step 9: instrument reclass + backdated audit trail
      SortEngine.scala       Parameterized external merge-sort (DFSORT analog)
      ...                    Steps 4,6,7,8
    datatypes/               Scala case classes for all record types

02-foundation/sort_specs/ Sort specification files (DFSORT analog — one per logical sort)
  SortedJE.sortspec          15-field posting key
  VendorMaster.sortspec      instID + instEffectDate
  LDGR.sortspec              ldgrIPID + ldgrLedgerPeriod

data/
  VARawFiles/                Raw VA expenditure files (FY03–FY04 included; FY05–FY16 downloaded)
  VendorMaster_enriched.csv  763,358 vendors with NIGP class + geo region attributes
  ViewSpec.csv               Declarative view specification (9 enabled reporting views)
  AllocationRules.csv        Step 6 fixture
  InteragencyTransfers.csv   Step 7 fixture
  BudgetRules.csv            Step 8 fixture
  cost_surface.csv           Experiment 1 accumulator — one row per pipeline run
  pivot_results.csv          Experiment 2 accumulator — one row per SAL round per run
  pipeline_log.csv           Per-step timing log

scripts/
  run_c1_smoke_test.sh       C1 smoke test (FY2003+FY2004, Steps 2+3)
  run_pipeline_orchestrator.sh  Full multi-year orchestrator
  standardize_and_sort.py    [Reference only — superseded by standardizeAndSort.scala]
  data_aggregation.py        [Reference only — superseded by dataAggregation.scala]
  extract_po_attributes.py   Builds vendor_po_attributes_by_year.csv from PO files
  car_process_period.py      CAR update cycle: detects attribute migrations, rolls VendorMaster
  enrich_vendormaster.py     Joins PO attributes → VendorMaster
  download_va_data.py        Downloads raw VA expenditure files from APA portal
  download_va_po_data.py     Downloads VA PO files from CKAN API

docs/
  universal-ledger-DESIGN.md   Full design document for all 9 pipeline steps
  TDS_vs_IL_architecture.md    ARE/IL distinction: transaction store vs instrument ledger
  BOB_METHOD_NOTE_CKB_EFFICIENCY.md  CKB method and scale-invariant streaming contract
  VA_Data_Layouts_and_Specs.md       VA data field specifications (converted from original docx)
```

---

## Technology Stack

| Layer | Technology | Why |
|---|---|---|
| All mainline financial flow | **Scala 2.12** | Cost surface measures the Scala CKB engine — mixing languages contaminates timing |
| Pipeline orchestration | **Bash** | JCL analog: sequential step invocations, exit codes, nothing more |
| Logging / metrics | **Python 3** | Append-only writes to `pipeline_log.csv`, `cost_surface.csv` |
| Build | **sbt** | Standard Scala build tool |

> **Technology invariant:** Steps 2–9 (all mainline financial flow) must remain Scala.
> Python in any mainline step contaminates the cost surface timing and invalidates the proof.

---

## Quick Start

### Prerequisites

- Java 11 or 21 (OpenJDK)
- [sbt](https://www.scala-sbt.org/download.html)
- Python 3.9+

### Build

```bash
cd 02-foundation/va_pipeline
sbt assembly
```

This produces `target/scala-2.12/va_pipeline-assembly-*.jar`.

### Download data (FY2003 included; full range optional)

```bash
python3 scripts/download_va_data.py --output data/VARawFiles
```

### Run the C1 smoke test (FY2003 + FY2004, Steps 2 + 3)

```bash
bash scripts/run_c1_smoke_test.sh
```

Pass criteria:
- Sum of all `ldgrTransAmount` = 0.00 per year (zero-sum proof)
- FY2003 LDGR rows = 345,955 (established benchmark)
- `data/cost_surface.csv` gains a C1 row

### Run the full pipeline

```bash
bash scripts/run_pipeline_orchestrator.sh --curve C2 --years 2003-2016
```

For the full dataset, keep large inputs and generated outputs outside the repository and pass
`--data-root PATH` (or set `UL_DATA_ROOT`). See [docs/RUNTIME_DATA_LAYOUT.md](docs/RUNTIME_DATA_LAYOUT.md)
for the expected directory layout.

For the full dataset, keep large inputs and generated outputs outside the repository and pass
`--data-root PATH` (or set `UL_DATA_ROOT`). See [docs/RUNTIME_DATA_LAYOUT.md](docs/RUNTIME_DATA_LAYOUT.md)
for the expected directory layout.

---

## The Four Experiment Curves

| Curve | Steps | What it measures |
|---|---|---|
| C1 | 2 + 3 | Baseline: post only (SortedJE → LDGR) |
| C2 | 2 + 3 + 5 | + SAL pivot views (marginal cost should be near-zero) |
| C3 | 2 + 3 + 5 + 8 + 9 | + Reval-class processes |
| C4 | 2 + 3 + 5 + 6 + 7 + 8 + 9 + 4 | Full pipeline — hypothesis: C4 ≈ C3 |

The overlap of C3 and C4 on the cost surface is the empirical proof that the SAL design collapses
key-widening cost to zero while maintaining full reporting completeness.

---

## Output Files

After a full C2 run, the key output files are:

| File | Contents |
|---|---|
| `data/output/LDGR{year}.csv` | Instrument Ledger — one balance row per vendor per period |
| `data/cost_surface.csv` | C_compute, C_storage, C_reconcile per run |
| `data/pivot_results.csv` | ldgr_rows (Curve A), key_embedded_equivalent (Curve B), rebuild_cost_ratio |
| `data/pipeline_log.csv` | Per-step wall-clock timing and record counts |

The `rebuild_cost_ratio` in `pivot_results.csv` is the empirical proof of the Instrument Pivot
Theorem: it is the amplification factor by which every real update is multiplied in a key-embedded
architecture vs. the CAR design. At Round 3 on FY2003 data, this ratio is ~1,378.

---

## Contributing

Contributions welcome. Please read `docs/universal-ledger-DESIGN.md` before opening a pull
request — the architecture constraints are not style preferences.

Key invariants:
1. All mainline Steps 2–9 must remain Scala
2. All sort operations must use `SortEngine` with a `.sortspec` file — no hardcoded sort keys
3. All streaming operations must be O(1) memory — no loading of full files into memory

Sign-off required per the DCO:

```
git commit -s
```

---

## License

Apache 2.0 — see [LICENSE](LICENSE).
