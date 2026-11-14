# Expanded reporting corpus attempt — 2026-11-14

Outcome: FAILED, no published reporting output and no model-ready result.

The run-once post-queue gate selected only monthly_c after all three executions
validated, rechecked its 14 years, and pinned physical posting generations.
Gate output: `gate.txt`; exact inputs/configuration: `run_config.txt`.
Financial sources are the unchanged reporting implementation from `b10b480`;
orchestration source revision was isolated commit `052c087`.

Expanded 5v failed at `ReportingPass.scala:228` with:

`Vendor working-state bound exceeded: 0000000000`

The existing default bound is 250,000 input rows per vendor; the shared
unattributed sentinel exceeded it. No guard was raised or removed, no retry was
attempted, and reduction 5r did not start. `extract.txt` contains the Scala error
and OS resource measurements. `input_manifest.csv` is partial, not proof that
all financial inputs were traversed. Partial CSVs remain only in the failed
external `.attempt` directory and are not completed analytical evidence.

Retained attempt:
`/Users/ktwitchell001/workspace/ckb_expanded_queue_20261113_ready/expanded_reporting.attempt`.

The three corpus runs are unaffected and retain their successful validation and
46-file repeat comparisons. Reporting scale behavior remains unverified.
Next work requires a bounded-state design for large unattributed groups; do not
blindly raise a row limit or run an unbounded aggregate in another runtime.

Supervisor correction: the initial `launchctl submit` jobs restarted exited
wrappers. Existing queue/output guards prevented financial replay. These jobs
were unloaded. The included plist explicitly uses `KeepAlive=false` and ran this
attempt once; it was unloaded after failure. No background financial job remains.
