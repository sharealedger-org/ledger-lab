# Ledger Lab Handoff

Updated 2026-09-30. This file is a concise state snapshot; use the linked design contracts for details.

## 2026-09-30 Conceptual Model and 99 Handoff

- Current branch: `work/99-reporting`, based on the pushed pre-99 evidence commit. The current 99 work is planning/documentation, not an implemented query service.
- A public [Sharealedger Conceptual Model repo](https://github.com/sharealedger-org/sharealedger-conceptual-model) now holds the conceptual model discussion, authorized April 2020 lecture slides, and a GenevaERS ERP discussion starter.
- The new [CKB view-query prototype plan](99-interpretation/CKB_VIEW_QUERY_PROTOTYPE.md) scopes 99 as a request/evidence interaction layer and treats scans as separately identified execution work.
- `docs/VA Data Samples and Specs.xlsx` exposes a VA-to-Universal-Journal mapping not implemented in the current Scala transform: intended Project=Program and Account=Object/Source; current code maps Project=Object, derives EXP/REV accounts, and drops Program. The workbook's wide Universal Balance also sketches Contract/Commitment/Balance IDs and 12 period amount slots; these are not yet settled as keys versus measures.
- The workbook-based reference cardinalities and dense bounds in the prototype plan are provisional domain ceilings, not observed FDS sizes or implementation evidence. The checked-in VendorMaster and VA data are fixture-scale.
- No UL accounting code changed for this model review. Do not upgrade audit states or claim the generalized CKB/query behavior until balance identity, account namespace, period representation, source mapping, and a falsifiable producer/consumer workload are agreed and tested.

### Next Model Decisions

1. Define the VA universal balance grain and the role of Contract, Commitment, Balance ID, and Instrument/Involved Party IDs.
2. Decide whether Account is a shared numeric namespace or type-qualified Object/Source domain, and define the crosswalk.
3. Decide whether the canonical balance is long-form by period or the workbook's wide period-slot representation; treat report pivots as a separate materialization choice.
4. Reconcile workbook mapping rows against source and reference data, including the program reference miss, before implementing the transformation change.
5. Select one named chartfield cut and prove its source, grouping, reconciliation, update/replay, and measured storage behavior before connecting it to 99.

## Architecture Source of Truth

- `01-transformation/` through `05-perspectives/` define logical ownership, not a fixed sequence of processes or physical passes.
- `99-interpretation/` defines the terminal post-run interpretation contract. A first interpreter and automatic run hook now exist for emitted evidence bundles, including the booked currency-revaluation fixture.
- The current Virginia Scala application is a legacy physical baseline with mixed responsibilities. Its numbered options are not a one-to-one implementation of layers 01-05.
- The original [Universal Ledger prototype](https://github.com/KipTwitchell/universal_ledger) is the domain lineage bridge: raw VA transaction assignment, Universal Journal generation, CKB-style balance updates, and multiple outputs from one pass. It is a POC, not the generalized GenevaERS engine.
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
- Spark, Vagrant, Play, and Derby are excluded from the cost-curve experiment. PostgreSQL is optional only as a later landing/access layer for event and view copies, not as part of the measured CKB path.

## Validation Status

- The Scala project compiled on macOS with JDK 21.
- A small FY2003 fixture run exercised the current VA path through its legacy financial operations on macOS. It verified selected balance and reconciliation controls, but does not validate full-scale performance or generalized 04/05 implementations.
- A separate Windows handoff branch contains fixes from that run and a fixture runner check. Windows validation remains pending.
- The canonical run, process, sort, partition, and reconciliation evidence is emitted for the baseline and active allocation smoke profiles. Engine/perspective evidence is still partial.
- The active allocation profile generates 45 balanced allocation SJEs, records an explicit rounding residual, applies them, and regenerates 27 next-period divisor groups.
- The first 99 interpreter and automatic end-of-run hook report active/partial status and missing future evidence rather than overstating coverage. The booked FX smoke emits engine metrics, reconciliation controls, and a run report with fixture-level `supports` evidence.
- The agentic architecture gates in [AGENTIC_ARCHITECTURE_GATES.md](docs/AGENTIC_ARCHITECTURE_GATES.md) are now mandatory after a rejected engine attempt exposed raw-event input, non-SJE output, orphaned materialization, and false CKB placement.

## Next Work

1. Validate the current fixture and active allocation profile on Windows using the agreed Bash environment.
2. Audit and repair the measurement substrate: process rows, artifact bytes, sort/pass records, controls, and replication identity.
3. Build named minimum-cost workloads and materialization profiles without turning the Scala prototype into a generalized engine.
4. Use GenevaERS Workbench, Run-Control Apps, Performance Engine, and CKB extensions as the future execution target when a workload is ready for cross-engine validation.
5. Keep Sharealedger as the future metadata/configuration home for the open ERP model; keep Ledger Lab focused on research fixtures, controls, and cost evidence.
6. Defer PostgreSQL until the file-based perspective/report contracts and materialization profiles are stable.

## Current Session Boundary

The project now has a coherent pre-99 foundation:

- Rule-based VA ARE output with explicit debit/offset legs and rule identity.
- Public VA object classification vocabulary separate from policy.
- Reproducible allocation divisor generation from raw or ledger input.
- Active allocation smoke profile with generated SJEs, rounding control, and next-period divisor handoff.
- Canonical evidence and first interpretation report.
- Named workload and materialization controls.
- Synthetic, clearly labeled inputs for intercompany candidates, USD/EUR/BTC currency views, and booked currency revaluation.
- A booked currency-revaluation engine slice that generates balanced SJE, applies it through the existing poster, and passes fixture controls; baseline and allocation regressions pass after the streaming merge fix.

The next work is validation of booked-revaluation rate/period edge cases, walk-forward behavior, and
target CKB placement, followed by the scoped elimination design. Do not treat the synthetic fixtures
as public Virginia evidence or the legacy poster path as generalized GenevaERS CKB evidence.

## Rejected Engine Attempt

The 2026-09-29 attempt was discarded. It passed a synthetic balance check but violated the architecture: it read raw fixture CSVs from an engine package, emitted a custom non-SJE file, ran after the post merge loop, never applied generated rows to ledger state, eagerly loaded inputs, and ignored materialization behavior. A same-process call is not a CKB engine section. The replacement must pass [AGENTIC_ARCHITECTURE_GATES.md](docs/AGENTIC_ARCHITECTURE_GATES.md) before any currency/elimination result is interpreted as evidence.

## First Adversarial Audit: Steps 2 and 3

Audit role: `Adversarial Reviewer`
Claim: Step 2 produces a valid sorted SJE partition that Step 3 can post into conserved instrument-level ledger state.
Command: `bash scripts/run_pipeline.sh --inPath "$RUN_ROOT/input" --outPath "$RUN_ROOT/output" --years 03 --steps 2,3 --curve audit --config step2-step3 --profile audit-baseline --materialization baseline`
Evidence root: `/tmp/ledger-lab-audit.styOkT`

Verified for the staged FY03 fixture:

- 1,520 SJE rows;
- 39 fields on every SJE row;
- 0 unbalanced journal groups;
- 0 sort-key monotonicity violations;
- 59 ledger rows;
- ledger amount total `0.00`.

Audit classifications:

- Step 2 transformation: `partially_verified`; the basic schema and balance invariant pass, but record-format coverage, vendor misses, lineage semantics, and scale remain unverified.
- Step 2 sort: `partially_verified`; the fixture key check passes, but adversarial multi-spill, numeric/order, and memory tests remain.
- Step 3 posting: `partially_verified`; the fixture reconstructs a conserved ledger, but prior-state replay, duplicate keys, multi-period behavior, and full lineage remain unverified.
- Process metrics: `contradicted` on macOS; the log reports Step 3 `records_read=0` despite 1,520 journals, storage is `?`, and Step 2 canonical input rows include the header (`761` reported versus `760` data rows).

This audit does not establish production scale, GenevaERS compatibility, generalized 04/05 maturity,
or a cost-curve result. The next audit should repair metric production before measuring workloads.

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
