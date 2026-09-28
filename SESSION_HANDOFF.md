# Ledger Lab Handoff

Updated 2026-09-28. This file is a concise state snapshot; use the linked design contracts for details.

## Architecture Source of Truth

- `01-transformation/` through `05-perspectives/` define logical ownership, not a fixed sequence of processes or physical passes.
- `99-interpretation/` defines the planned terminal, post-run interpretation contract. Its producers and automatic run hook are not implemented.
- The current Virginia Scala application is a legacy physical baseline with mixed responsibilities. Its numbered options are not a one-to-one implementation of layers 01-05.
- Start with [README.md](README.md), the five layer READMEs, [CKB_LAYER_EXECUTION_MODEL.md](docs/CKB_LAYER_EXECUTION_MODEL.md), and [MEASUREMENT_AND_LOGGING.md](docs/MEASUREMENT_AND_LOGGING.md).

## Current Implementation and Build

- Mainline financial processing is Scala; Bash orchestrates; Python is restricted to preparation, inspection, and post-run analysis. See [AGENTS.md](AGENTS.md).
- The current Scala project is `02-foundation/va_pipeline/`, using Scala 2.12.18 and sbt 1.10.10.
- Local compile command:

  ```bash
  cd 02-foundation/va_pipeline
  sbt -batch compile
  ```

- `data/` contains small sanitized samples and configuration fixtures. Full VA and PO histories remain external; see [RUNTIME_DATA_LAYOUT.md](docs/RUNTIME_DATA_LAYOUT.md).
- The PostgreSQL JDBC dependency is present in the build, but the database-backed Perspective aggregation design is not implemented. Decisions are tracked in [step5-postgres-design.md](docs/step5-postgres-design.md).

## Validation Status

- The Scala project compiled on macOS with JDK 21.
- A small FY2003 fixture run exercised the current VA path through its legacy financial operations on macOS. It verified selected balance and reconciliation controls, but does not validate full-scale performance or generalized 04/05 implementations.
- A separate Windows handoff branch contains fixes from that run and a fixture runner check. It has not yet been validated on Windows.
- The canonical run/process/sort/partition/engine/perspective/reconciliation records are specified, but current runners do not emit the complete contract.
- The 99 interpretation report and automatic end-of-run hook remain unimplemented.

## Next Work

1. Validate the fixture handoff on Windows using the agreed Bash environment.
2. Resolve the open PostgreSQL lifecycle and table-layout decisions before implementing the Perspective database path.
3. Map current physical processes to 01-05 and label mixed or missing implementation explicitly.
4. Add active small fixtures for allocation, interagency elimination, effective-dated change, and perspective controls.
5. Implement canonical evidence producers, then the 99 interpreter and run hook.
6. Define named research workloads and only then run comparable, replicated runtime studies.
