# Actual Bash scheduler fixture

Two independent raw-input years (FY03/04 and wholly generated FY31/32) were
prepared by `FusedDriverFixture` using the existing short-year APA/PO adapters,
independent sort and standalone GL. Main financial classes stayed frozen at
`a0dae7c`; their admission hash manifest rechecked unchanged after test compile.

The actual `scripts/run_phase2_fused_baseline.sh` ran `--years 03,04 --workers 2
--through financial`, with fixture `--historical`, `--wholly`, `--retained`,
`--historicalManifest`, `--whollyManifest` and `--admission` paths under
`session-run.tmp/fused_controller_fixture_v2`. Its small completed financial
admission is explicitly labelled fixture-only, not real-year resource admission.

Both workers ran in one wave and completed. Shared controls verified 16 historical
plus 8 wholly generated journal rows, 24 combined rows, and exact GL conservation
for both years. Each posting opened its journal once. Evidence is adjacent.
Run root: `session-run.tmp/fused_controller_fixture_run_v1`.

A second attempt used a copied fixture reference tree with FY04's GL deliberately
removed. The actual worker failed at the native GL gate; the scheduler detected
its nonzero result and terminated its own process group (exit 143). Its FAILED
marker and worker log are retained. No shared financial completion was published.
The first failure attempt stopped correctly but exposed a missing failure marker
on SIGTERM. Explicit TERM/INT exit handling was added and the check repeated;
the second attempt wrote `FAILED rc=143` as expected.
Run roots: `session-run.tmp/fused_controller_fixture_failure_v1` (initial) and
`session-run.tmp/fused_controller_fixture_failure_v2` (corrected signal handling).

This proves bounded financial orchestration and one worker-failure path. It does
not prove real-year memory/scratch acceptance, full population controls, reporting
or cost behavior. Reporting storage projections are planning gates only, with
the existing live 40-GiB disk stop retained.
