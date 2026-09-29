# Ledger Lab Handoff

Updated 2026-09-28. This file is a concise state snapshot; use the linked design contracts for details.

## Architecture Source of Truth

- `01-transformation/` through `05-perspectives/` define logical ownership, not a fixed sequence of processes or physical passes.
- `99-interpretation/` defines the terminal post-run interpretation contract. A first interpreter and automatic run hook now exist for the emitted evidence bundle; richer currency/elimination interpretation remains deferred.
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
- PostgreSQL is intentionally deferred. The current user-facing smoke path is the compact `--show-data` output and the file-based interpretation report.

## Validation Status

- The Scala project compiled on macOS with JDK 21.
- A small FY2003 fixture run exercised the current VA path through its legacy financial operations on macOS. It verified selected balance and reconciliation controls, but does not validate full-scale performance or generalized 04/05 implementations.
- A separate Windows handoff branch contains fixes from that run and a fixture runner check. Windows validation remains pending.
- The canonical run, process, sort, partition, and reconciliation evidence is emitted for the baseline and active allocation smoke profiles. Engine/perspective evidence is still partial.
- The active allocation profile generates 45 balanced allocation SJEs, records an explicit rounding residual, applies them, and regenerates 27 next-period divisor groups.
- The first 99 interpreter and automatic end-of-run hook are implemented for current evidence; it reports active/partial status and missing future evidence rather than overstating coverage.

## Next Work

1. Validate the current fixture and active allocation profile on Windows using the agreed Bash environment.
2. Implement currency translation and intercompany candidate views as inline CKB/report sections; keep their synthetic inputs separate from VA data.
3. Extend evidence for those views, including unmatched-counterparty gaps and effective-dated FX controls.
4. Build the richer 99 report only after those process records exist.
5. Defer PostgreSQL until the file-based perspective/report contracts and materialization profiles are stable.
6. Define named research workloads and only then run comparable, replicated runtime studies.

## Current Session Boundary

The project now has a coherent pre-99 foundation:

- Rule-based VA ARE output with explicit debit/offset legs and rule identity.
- Public VA object classification vocabulary separate from policy.
- Reproducible allocation divisor generation from raw or ledger input.
- Active allocation smoke profile with generated SJEs, rounding control, and next-period divisor handoff.
- Canonical evidence and first interpretation report.
- Named workload and materialization controls.
- Synthetic, clearly labeled inputs for intercompany candidates and USD/EUR/BTC currency views.

The next process work is report-time currency and intercompany candidate views inside the CKB flow.
Do not treat the synthetic fixtures as public Virginia evidence, and do not expand the 99 narrative
until those view and gap records are emitted.

# Windows machine test

Here is the test from the smoke test command.

head -n 3 /path/printed/output/SortedJEFY03q1exp.csv
head -n 3 /path/printed/output/LDGR2003.csv
wc -l /path/printed/output/SortedJEFY03q1exp.csv /path/printed/output/LDGR2003.csv
cat /path/printed/output/pipeline_results.log
cat /path/printed/output/cost_surface.csv

The paths above are illustrative placeholders, not literal commands. The current Windows test
should use `bash scripts/run_repo_fixture_smoke_test.sh --show-data` and copy the actual retained
path only when running with `--keep-output`.
head: cannot open '/path/printed/output/SortedJEFY03q1exp.csv' for reading: No such file or directory
head: cannot open '/path/printed/output/LDGR2003.csv' for reading: No such file or directory
wc: /path/printed/output/SortedJEFY03q1exp.csv: No such file or directory
wc: /path/printed/output/LDGR2003.csv: No such file or directory
0 total
cat: /path/printed/output/pipeline_results.log: No such file or directory
cat: /path/printed/output/cost_surface.csv: No such file or directory
