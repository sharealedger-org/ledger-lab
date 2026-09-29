# Ledger Lab — Quick Start

This guide orients a technical contributor to the repository's architecture, current
implementation, and validation status. It distinguishes the target 01–05 design from the legacy
Virginia executable path.

---

## What This Is

Ledger Lab studies the cost and capability of representing financial events as instrument-anchored
state and deriving reporting views from effective-dated attributes. The intended responsibilities
are organized into 01 Transformation, 02 Foundation, 03 Instrument Ledger, 04 Engines, and 05
Perspectives. They are logical boundaries, not a required sequence of physical passes.

The current Virginia Scala application is a legacy research baseline whose code combines some of
those responsibilities. It can provide useful fixture and control evidence, but its numbered
options are not a faithful implementation map for 01–05. See [the layer status](README.md#architecture-and-status)
and [the CKB execution model](docs/CKB_LAYER_EXECUTION_MODEL.md).

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| JDK | 21 | Current locally validated toolchain; cross-platform validation is pending. |
| sbt | 1.10.10 | Pinned in `02-foundation/va_pipeline/project/build.properties`. |
| Scala | 2.12.18 | Configured by the VA subproject. |
| PostgreSQL | Not required to compile | A JDBC dependency is present; the planned database-backed Step 5 is not implemented. |

Spark-dependent sources are excluded from the default build. Do not infer that an excluded path has
a Scala replacement simply because its source files are written in Scala.

---

## Build Check

```bash
cd 02-foundation/va_pipeline
sbt -batch compile
cd ../..
```

Run the checked-in fixture through the no-Spark runtime path:

```bash
bash scripts/run_repo_fixture_smoke_test.sh
```

This stages `data/FY03q1exp_small.txt` under the production filename, runs Steps 2 and 3, checks
that sorted journal and ledger files are written, and verifies the ledger amount sum is balanced.
It needs only JDK 21, sbt, Bash, and the files in this repository. Add `--keep-output` to retain
the temporary output and print a small journal/ledger sample, row counts, and both run summaries.

### Inspect A Successful Run

Run the fixture with output retained:

```bash
bash scripts/run_repo_fixture_smoke_test.sh --keep-output
```

The printed output directory contains a sorted journal, a ledger, and two small run summaries. The
script displays representative rows and summaries automatically. For further inspection, copy the
actual directory from the `Fixture output retained at:` line. The `/path/printed/output` text below
is only a placeholder; do not type it literally. In Git Bash, verify the directory first:

```bash
output_dir="/the/actual/path/printed/by/the/script/output"
ls -la "$output_dir"
head -n 3 "$output_dir/SortedJEFY03q1exp.csv"
head -n 3 "$output_dir/LDGR2003.csv"
wc -l "$output_dir/SortedJEFY03q1exp.csv" "$output_dir/LDGR2003.csv"
cat "$output_dir/pipeline_results.log"
cat "$output_dir/cost_surface.csv"
```

The sorted journal shows the generated debit and credit lines that Step 2 sends to posting. The
ledger shows the accumulated balance rows produced by Step 3. The log records the selected curve,
steps, elapsed time, and output location; `cost_surface.csv` is a compatibility summary, not the
complete measurement contract. These commands inspect small prefixes and summaries rather than
loading an entire history into an analysis engine.

---

## Understanding the Output

The current VA path uses three important artifact families:

- Sorted journal entries carry transaction-level attributes and amounts into posting.
- `LDGR*.csv` holds balances at the VA implementation's current ledger grain.
- `VendorMaster.csv` supplies vendor attributes used by the legacy aggregation and reclassification
  code.

These files are relevant evidence for the Instrument Ledger and attribute-reporting questions, but
their existence does not establish the full 03 contract, a generalized 05 engine, or zero marginal
storage for every possible reporting dimension. See [99-interpretation](99-interpretation/README.md)
for required controls and evidence status.

---

## Legacy VA Runner

The existing `scripts/run_pipeline.sh` accepts an input directory, output directory, fiscal years,
and selected VA application options. The two useful no-Spark steps are:

| Step | Reads | Writes | Purpose |
|---|---|---|---|
| `2` | `FY<YY>q<quarter>exp.txt` or revenue input plus `VendorMaster.csv` | `SortedJEFY<YY>q<quarter>exp.csv` | Standardize source rows, create balanced journal lines, and externally sort them. |
| `3` | `SortedJE*.csv` in `outPath` | `LDGR<YYYY>.csv` | Post sorted journal lines into the legacy instrument ledger. |

Steps 4–9 are legacy follow-on operations and require outputs or configuration from earlier steps.
Steps 1, 10, and 11 require Spark and are excluded from the default build. The `--curve` field is
retained for compatibility with existing logs; it is not the research taxonomy. Neither runner
interface currently emits the complete canonical logging contract.

Inspect the available options without starting a run:

```bash
bash scripts/run_pipeline.sh --help
bash scripts/run_pipeline_orchestrator.sh --help
```

For the checked-in small data, use `scripts/run_repo_fixture_smoke_test.sh`; it performs the staging
the production runner requires. Full historical runs use the external layout described in
[RUNTIME_DATA_LAYOUT.md](docs/RUNTIME_DATA_LAYOUT.md).

---

## Current Research Questions

The repository's questions concern the relationship between economic events, canonical
instrument-level state, generated events, and reporting capabilities:

- Can the ledger state be reconstructed from balanced, traceable source events?
- Which configured views can be derived from ledger state plus effective-dated attributes, and
  what additional data is actually materialized?
- Can calculation engines emit balanced, lineage-preserving SJE partitions that are applied back
  to canonical state?
- What compute, storage, sort, spill, and reconciliation costs accompany each capability?

These are questions for experiments, not conclusions implied by successful compilation or a small
fixture. See the [99 interpretation contract](99-interpretation/README.md) for evidence and status
requirements.

## Legacy VA Implementation Map

This map helps navigate the current executable; it does not equate VA files with the target
architecture.

| Current code area | Existing behavior | Design status / caution |
|---|---|---|
| `standardizeAndSort.scala` and `post.scala` | Transform VA expenditure records, externally sort journal entries, and accumulate ledger balances. | A physical VA path that combines transformation and foundation responsibilities. |
| `dataAggregation.scala` | Read ledger output and configured views, resolve vendor attributes, emit CSV summaries. | Existing in-memory implementation is a prototype; PostgreSQL selection and bounded cursor design is deferred in `docs/step5-postgres-design.md`. |
| Allocation, consolidation, budget, reclassification, and contra modules | Legacy balance-driven financial operations and reconciliation. | These do not by themselves establish generalized 04 Engine contracts. Test both active and no-op cases. |
| `04-engines/` and `05-perspectives/` | Target ownership boundaries and planned experiment scaffolds. | Generalized implementations are incomplete. |

Browse the [Scala entry point](02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/LedgerApp.scala)
and the relevant 01–05 layer README before changing a process.

## Evidence and Measurements

The schemas in [MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md) define the intended
run manifest, process metrics, sort events, partition catalog, engine metrics, perspective metrics,
and reconciliation results. Current compatibility logs do not yet emit this full contract. A
post-run report should separate active work, zero-work stages, failures, missing evidence, and
architecture conclusions. Keep VA-source and synthetic-fixture results distinct.

The `cost_surface.csv` and `pivot_results.csv` files are legacy summaries. A modeled permutation
ratio is not a measured rebuild, and a successful view is not proof of all possible reporting
capabilities. Cost claims need comparable runtime-data runs, explicit weights, resource metrics,
controls, and replication.

## Runtime Data

Small sanitized fixtures live in `data/`; full source and PO histories do not. Use the external
directory contract and download instructions in [RUNTIME_DATA_LAYOUT.md](docs/RUNTIME_DATA_LAYOUT.md).
Do not run a full historical workload with output directed into the Git repository.

## Reading Order

1. [Repository architecture and implementation status](README.md#architecture-and-status)
2. The relevant [01–05 layer README](README.md#architecture-and-status)
3. [CKB execution and physical pass model](docs/CKB_LAYER_EXECUTION_MODEL.md)
4. [Measurement and logging contract](docs/MEASUREMENT_AND_LOGGING.md)
5. [99 interpretation boundary](99-interpretation/README.md)

`PROJECT_BRIEF.md` and `docs/universal-ledger-DESIGN.md` preserve historical design material.
Use the current layer READMEs and the status statements above for the repository's present design
and implementation state.
