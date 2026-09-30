# Ledger Lab

Ledger Lab is the public research successor to the Universal Ledger proof of concept. It is an
Apache-2.0 research prototype for examining how financial events, instrument-level state,
calculation engines, and reporting perspectives fit together.

The repository uses public Virginia expenditure data and includes small sanitized fixtures.
Full historical inputs are external to Git and must be supplied separately. A successful fixture
run demonstrates behavior on that fixture; it does not by itself establish scale, cost, or the
general validity of the research claims.

## Research Questions

- How do compute, retained data, and reconciliation obligations change as capabilities and
  materialization are added?
- Can an instrument-anchored ledger joined to effective-dated attributes answer specified reporting
  questions without maintaining a separate balance store for each reporting cut?

These are hypotheses to evaluate with comparable workloads, explicit controls, and reproducible
measurements. See [99-interpretation](99-interpretation/README.md) for how results should be
interpreted and what the evidence must contain.

## Strategic Boundary

Ledger Lab is the minimum-cost-curve research and measurement repository. It is not the
generalized GenevaERS execution engine and must not grow into a replacement for that project.
The Scala VA implementation is a domain prototype and historical baseline used to test financial
contracts, materialization choices, controls, and cost hypotheses.

The GenevaERS organization owns the generalized execution substrate: Workbench metadata authoring,
Run-Control Apps, the Java/Performance Engine, CKB read/lookup/write extensions, DevOps, demos, and
documentation. Future Ledger Lab workloads may be translated into GenevaERS metadata and run
through those repositories, but engine implementation belongs there.

Sharealedger is the intended future home for open ERP financial metadata and configuration: event
definitions, ARE rules, instrument/CAR structures, effective-dated references, calculation-engine
definitions, perspectives, and workload manifests. Ledger Lab supplies measured research fixtures
and evidence for those configurations; it does not become the ERP runtime.

The cost-curve experiment does not use Spark, Vagrant, Play, or Derby. Spark does not provide the
required CKB contract for multiple structured files co-located by common key, nor does it inherit
pre-sorted-file order without repartitioning and resorting. Vagrant, Play, and Derby are historical
prototype infrastructure, not experiment substrates. PostgreSQL may serve as an optional landing
and access layer for retained event copies or view outputs, but it is not an integral part of the
measured CKB cost path.

---

## Architecture and Status

The 01–05 directories describe logical ownership, not a promise of five processes or five passes.
The current VA implementation predates this organization and combines responsibilities. A legacy
process that resembles a target layer does not mean that layer's generalized implementation exists.

| Area | Intended responsibility | Current status |
|---|---|---|
| [01 Transformation](01-transformation/README.md) | Normalize source records and emit balanced, traceable SJE partitions. | Contract documented; the VA transform is combined with sorting in the existing Scala path. |
| [02 Foundation](02-foundation/README.md) | Sort contracts, CKB mechanics, state application, and controls. | Current physical VA engine and orchestration live under `02-foundation/va_pipeline/` and `scripts/`. |
| [03 Instrument Ledger](03-instrument-ledger/README.md) | Preserve canonical journal history and instrument-level state. | Represented by the current VA journal and LDGR files; not a separate processing application. |
| [04 Engines](04-engines/calculation_engines/README.md) | Read SJE structures and emit balanced, traceable SJE structures. | Target boundary is documented; generalized engine implementation is not complete. |
| [05 Perspectives](05-perspectives/README.md) | Aggregate selected ledger and engine outputs into domain views. | Legacy VA aggregation exists; the generalized perspective engine is a design scaffold. |
| [99 Interpretation](99-interpretation/README.md) | Interpret completed or partial run evidence for researchers. | First canonical evidence producers and automatic report hook exist for fixture workloads; generalized reporting remains planned. |

For execution topology, sort boundaries, and process memory contracts, see
[CKB_LAYER_EXECUTION_MODEL.md](docs/CKB_LAYER_EXECUTION_MODEL.md). The planned evidence records are
defined in [MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md); they are not all emitted
by the current runners.

## Where Things Live

- `02-foundation/va_pipeline/`: current Scala implementation of the Virginia research baseline.
- `02-foundation/sort_specs/`: external-sort contracts used by the Scala pipeline.
- `data/`: small sanitized fixtures and rule/view configuration; full historical inputs are external.
- `scripts/`: Bash orchestration plus Python data preparation and post-run utilities.
- `docs/`: architecture decisions, data contracts, measurement design, and historical specifications.
- `99-interpretation/`: first terminal post-run interpretation and automatic report hook for emitted evidence bundles.

---

## Technology Stack

| Layer | Technology | Why |
|---|---|---|
| Financial processing | **Scala 2.12.18** | Mainline accounting, state, engines, and perspective calculations stay in the measured Scala path. |
| Orchestration | **Bash** | Orders bounded processes, moves declared partitions, and handles exits. |
| Post-run inspection | **Python 3** | May inspect and summarize evidence; must not replace financial processing. |
| Database infrastructure | **PostgreSQL** | JDBC driver is in the build; the planned Step 5 database execution is not implemented. |
| Build | **sbt 1.10.10** | Version pinned in the VA subproject. |

Spark-dependent sources remain excluded from the default build. A logical layer is not a language,
process, or pass; see [AGENTS.md](AGENTS.md) for the technology boundaries.

---

## Quick Start

### Prerequisites

- JDK 21 is the locally validated toolchain; Windows validation is pending.
- sbt 1.10.10 is pinned in `02-foundation/va_pipeline/project/build.properties`.
- No PostgreSQL server is needed to compile the current VA application. The PostgreSQL-backed
  Step 5 design is not yet implemented.

### Compile

```bash
cd 02-foundation/va_pipeline
sbt -batch compile
```

To inspect the legacy VA runner options without starting a data run:

```bash
bash scripts/run_pipeline.sh --help
```

### Data and Runtime

The checked-in `data/` directory contains sanitized examples such as
`FY03q1exp_small.txt`, `VendorMaster.csv`, and the small rule/view fixtures. Full historical VA
files are not in Git. Their external directory contract is documented in
[RUNTIME_DATA_LAYOUT.md](docs/RUNTIME_DATA_LAYOUT.md).

Run `bash scripts/run_repo_fixture_smoke_test.sh` for a repository-only Steps 2 and 3 runtime
check. It stages the small fixture under the filenames expected by the legacy runner, writes to a
temporary directory, and verifies a balanced ledger. The build and fixture checks are functional
smoke tests, not scale or architecture validation.

For the intended architecture and its current implementation status, start with
[Architecture and Status](#architecture-and-status). For evidence requirements, see
[99-interpretation](99-interpretation/README.md).

---

## Current VA Workload Profiles

These descriptive profiles explain what the existing VA implementation can run. The versioned
research taxonomy and interpretation contract live in
[99-interpretation](99-interpretation/README.md).

| Workload profile | Current VA workload | Evidence question |
|---|---|---|
| Instrument-state baseline | Convert source expenditure to balanced journal events, order them for posting, and produce instrument-level balances. | Are source-to-ledger records balanced and reconstructible, and what baseline resources do they require? |
| Attribute-derived reporting | Aggregate ledger balances with configured VendorMaster/CAR attributes. | Which configured reporting cuts are supported, what controls reconcile them, and what additional storage is materialized? |
| Temporal treatments | Produce budget/variance outputs and apply effective-dated instrument reclassification through balanced generated events. | Does the run preserve history and lineage while applying dated changes, and what work do these treatments add? |
| Integrated financial close | Exercise allocation, consolidation/elimination, and final contra reconciliation around the same ledger. | Which controls and capabilities work together, and which stages had meaningful input rather than a no-op? |

These profiles describe legacy VA job compositions, not the target process topology. Running the
existing view or reclassification code does not establish that the generalized 04 Engine or 05
Perspective implementations are complete. Checked-in fixtures support functional and control-flow
tests; cost conclusions require comparable runtime-data runs with the required evidence.

---

## Output Files

The legacy VA runner may write compatibility outputs such as:

| File | Contents |
|---|---|
| `data/output/LDGR{year}.csv` | Instrument Ledger — one balance row per vendor per period |
| `data/cost_surface.csv` | Compatibility run summary; not the complete canonical metrics record |
| `data/pivot_results.csv` | Legacy pivot experiment calculations |
| `data/pipeline_log.csv` | Compatibility per-step log |

These compatibility files do not replace the canonical evidence described in
[MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md). Derived ratios quantify the declared
model; they are not, by themselves, measured rebuilds or proof of universal reporting coverage.

---

## Contributing

Contributions welcome. Start with the [01–05 layer READMEs](#architecture-and-status),
[CKB execution model](docs/CKB_LAYER_EXECUTION_MODEL.md), and [agent rules](AGENTS.md). Older
Universal Ledger design documents describe historical implementation proposals and should not be
treated as current implementation status.

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
