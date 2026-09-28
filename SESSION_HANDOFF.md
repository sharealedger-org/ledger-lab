# Ledger Lab Session Handoff

## Current state

This repository is the fresh local staging repo for the public `sharealedger-org/ledger-lab` project.
It is derived from the original Universal Ledger POC, which remains the historical lineage.

Included:

- Transformation/ARE boundary documented in `01-transformation/README.md`
- Foundation-layer boundary documented in `02-foundation/README.md`
- Instrument Ledger boundary documented in `03-instrument-ledger/README.md`
- Engines scaffold under `04-engines/`
- Perspective-layer design scaffold under `05-perspectives/`
- Virginia Scala pipeline under `02-foundation/va_pipeline/`
- sbt build files
- Agentic runners under `scripts/`
- Sort specifications under `02-foundation/sort_specs/`
- Sanitized small fixtures and configuration data under `data/`
- Sanitized workbook: `docs/Universal Journal Instrument Ledger Model v0.7.xlsx`
- Curated design documentation under `docs/`

Excluded:

- Full raw Virginia expenditure and PO datasets
- Generated outputs and logs
- Prototype VM/session artifacts
- Local Git history from the source repository

## Runtime data

Large datasets must be supplied outside Git. Use either:

```bash
export UL_DATA_ROOT=/path/to/va-runtime-data
bash scripts/run_pipeline_orchestrator.sh --years 2003,2004 --curve C2
```

or:

```bash
bash scripts/run_pipeline_orchestrator.sh \
  --data-root /path/to/va-runtime-data \
  --years 2003,2004 \
  --curve C2
```

See `docs/RUNTIME_DATA_LAYOUT.md`.

## Validation completed

- Shell syntax checks pass.
- Python syntax checks pass.
- Orchestrator and C1 smoke-test dry runs pass with an external data root.
- Fixture contact/address scrubbing completed.
- Workbook ZIP/XML integrity validated; all 16 sheets preserved.
- No copied-source matches for the audited credential, SSH, or internal-host patterns.

A full Scala compile has not yet run because `sbt` is not currently available on the terminal `PATH`.

The repository now has a conceptual two-layer architecture. The existing implementation remains
at its current paths as the Foundation Layer until the first compile and path-migration review.
Perspective-layer engine code has not started.

## Suggested next steps

1. Open this repository as the active VS Code workspace.
2. Install or expose Java and sbt, then run `sbt -batch compile` from `02-foundation/va_pipeline/`.
3. Run the fixture C1 smoke test.
4. Supply the external runtime dataset and validate the full C1/C2 pipeline.
5. Review the agent interface and add any configuration needed for cost-curve experiments.
6. Decide which source files and fixtures belong in the first public commit.
