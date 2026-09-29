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

# Windows machine test

Here is the test from the smoke test command.

$ bash scripts/run_repo_fixture_smoke_test.sh
Running checked-in FY03 small fixture through Steps 2 and 3...
[2026-09-28 18:44:07] ======================================================================
[2026-09-28 18:44:07] Universal Ledger Pipeline Run  [run_20260928_184407]
[2026-09-28 18:44:07]   curve   = C1
[2026-09-28 18:44:08]   config  = repo-fixture
[2026-09-28 18:44:08]   inPath  = /tmp/tmp.KFx4yiIAcG/input
[2026-09-28 18:44:08]   outPath = /tmp/tmp.KFx4yiIAcG/output
[2026-09-28 18:44:08]   years   = 03
[2026-09-28 18:44:08]   steps   = 2,3
[2026-09-28 18:44:08]   logFile = /tmp/tmp.KFx4yiIAcG/output/pipeline_results.log
[2026-09-28 18:44:08] ======================================================================
[2026-09-28 18:44:08] ------  Year 03  -------------------------------------------------------
[2026-09-28 18:44:08] START  step=2  year=03  quarter=01
[info] welcome to sbt 1.10.10 (Microsoft Java 21.0.11)
[info] loading settings for project va_pipeline-build from assembly.sbt...
[info] loading project definition from C:\Users\0D8297897\Workspace\ledger-lab\02-foundation\va_pipeline\project
[info] loading settings for project root from build.sbt...
[info] set current project to va_pipeline (in build file:/C:/Users/0D8297897/Workspace/ledger-lab/02-foundation/va_pipeline/)
[info] compiling 1 Scala source to C:\Users\0D8297897\Workspace\ledger-lab\02-foundation\va_pipeline\target\scala-2.12\classes ...
[info] done compiling
[info] running org.universalledger.foundation.va.ledger.LedgerApp --step 2 --inPath /tmp/tmp.KFx4yiIAcG/input --outPath /tmp/tmp.KFx4yiIAcG/output --year 03 --quarter 01 --vendormaster /tmp/tmp.KFx4yiIAcG/input/VendorMaster.csv
****************************************************************************************************
                                Universal Ledger
                                   Demo System
                  Using State of Virginia Public Financial Data
****************************************************************************************************
[CLI] step=2  inPath=/tmp/tmp.KFx4yiIAcG/input  outPath=/tmp/tmp.KFx4yiIAcG/output  year=03  quarter=01
-----------------------------------
****************************************************************************************************
                         Step 2 — Standardize & Sort (standardizeAndSort)
Start Time: Mon Sep 28 18:44:27 MST 2026
****************************************************************************************************
  Input file   : /tmp/tmp.KFx4yiIAcG/input/FY03q1exp.txt
  Output file  : /tmp/tmp.KFx4yiIAcG/output/SortedJEFY03q1exp.csv
  Year         : 03  Quarter: 1  Type: E  RecFormat: A
  AcctDate     : 03/03/01
  SortSpec     : ../sort_specs/SortedJE.sortspec  (15 fields)
  ChunkSize    : 200000
  DryRun       : false
WARNING: VendorMaster not found at data/VendorMaster_full.csv — instIDs will be 0000000000
  VendorMaster : data/VendorMaster_full.csv  (0 entries)
ERROR: Input file not found: /tmp/tmp.KFx4yiIAcG/input/FY03q1exp.txt
