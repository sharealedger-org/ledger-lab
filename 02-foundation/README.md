# Foundation Layer

The Foundation Layer is the stable, perspective-neutral substrate for Ledger Lab.

It is the execution and measurement base on which the transformation and perspective experiments depend. It preserves economic events, translates source/product events through the Accounting Rules Engine (ARE), identifies instruments, maintains the canonical ledger state, applies generic balanced transactions, and exposes control totals. It does not decide how Finance, Tax, MIS, Risk, or Liquidity interpret that state.

## Current Implementation Map

The repository is intentionally retaining its existing paths while the fresh project is being reorganized:

| Foundation responsibility | Current location |
|---|---|
| Scala financial processing engine | `02-foundation/va_pipeline/` |
| Transformation/ARE boundary | `01-transformation/are/README.md` and Step 2 in `va_pipeline/` |
| Pipeline orchestration | `scripts/run_pipeline.sh`, `scripts/run_pipeline_orchestrator.sh` |
| Data ingestion and CAR preparation | `scripts/download_*.py`, `scripts/extract_po_attributes.py`, `scripts/car_*.py` |
| External sorting and sort specifications | `scripts/standardize_and_sort.py`, `02-foundation/sort_specs/` |
| Public and fixture inputs | `data/` |
| Runtime data contract | `docs/RUNTIME_DATA_LAYOUT.md` |
| Cost and control metrics | `data/cost_surface.csv`, `data/pipeline_log.csv`, `scripts/data_aggregation.py` |

These paths are the current physical implementation of the Foundation Layer. They are not yet moved because the build files, shell runners, documentation, and relative data paths still depend on them.

## Foundation Responsibilities

- Preserve source events and provenance.
- Translate source/product events into balanced Universal Journal/SJE groups through the ARE.
- Keep ARE rule identity and version explicit for deterministic replay.
- Standardize source data into Universal Journal entries.
- Maintain Instrument ID and CAR relationships.
- Post movements into the canonical Instrument Ledger.
- Support effective-dated attribute resolution.
- Provide generic balanced transaction mechanics for future step-ups.
- Execute the reusable CKB/file-processing substrate.
- Produce deterministic control totals and reconciliation evidence.
- Measure compute, storage, maintained master files, and generated rows.
- Provide the runtime interface used by the agentic experiment layer.

## Boundary With the Perspective Layer

The Foundation Layer owns **how source events become accounting events and how economic state is represented and processed**.

The Foundation Layer is not necessarily a separate pass. CKB can execute Foundation, Instrument
Ledger, Engine, and Perspective sections in one ordered process. A physical pass boundary is
required when a new sort order is required; logical layer ownership alone does not create one.

The [Instrument Ledger Layer](../03-instrument-ledger/README.md) owns the canonical state boundary.
The [Engines Layer](../04-engines/calculation_engines/README.md) reads and generates SJE partitions.
The [Perspective Layer](../05-perspectives/README.md) aggregates only.

The Foundation Layer may provide generic primitives for:

- balanced generated transactions;
- source-event lineage;
- rule versioning;
- effective dates;
- application and reversal;
- control-total reporting.

The ARE boundary now lives in `01-transformation/are/README.md`. Foundation provides the
sorting, CKB, replacement-master, and control mechanics consumed by the Instrument Ledger.

It must not encode Finance, Tax, MIS, Risk, or Liquidity policy as implicit mainline behavior.

## Target Physical Shape

The eventual physical layout may become:

```text
02-foundation/
  va_pipeline/             # Virginia implementation of the Scala engine
  scripts/                 # orchestration and metric adapters
  sort_specs/              # external-sort contracts used by va_pipeline
  data_contracts/          # shared input/output contracts
  tests/                   # foundation invariants

03-instrument-ledger/      # canonical state boundary

04-engines/
  calculation_engines/     # SJE-in / SJE-out history-dependent generated-event mechanics

05-perspectives/
  view_engine/             # aggregation-only generalized view execution
  specs/                   # experiment configurations
  schemas/                 # perspective contracts
  rules/                   # step-up policy definitions
  fixtures/                # synthetic perspective data
  engine/                  # perspective implementation boundary
  tests/                   # perspective invariants
  results/                 # generated experiment outputs
```

The target shape is documented now, but the physical relocation should follow the first successful compile and a path migration checklist. A move must preserve reproducibility and must not silently change runtime data roots.

## Foundation Invariants

- Every journal batch balances.
- Every ledger output is reproducible from declared inputs and rules.
- Every generated transaction has source lineage.
- Instrument ID remains available at the canonical balance grain.
- Effective-dated reference data does not rewrite source history.
- Perspective-specific rules cannot silently alter unrelated foundation outputs.
- Cost metrics identify their units and weighting assumptions.

## Status

- Named architectural boundary: established.
- Existing implementation mapped: established.
- Physical relocation: intentionally deferred.
- Generic step-up primitives: not implemented.
- Foundation compile baseline: pending local sbt availability.
