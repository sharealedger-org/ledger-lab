# Identity recovery v1 — 2026-11-14

Status: fixture-verified changes; full-corpus admission blocked. No replacement of
prior measured financial outputs or Gold. No complete synthetic vendor population.

* Unchanged focused baseline: 28 tests passed before edits (`baseline_tests.log`).
* Final focused parser/report/audit/legacy-view/one-read suite: 56 passed
  (`focused_tests_and_reference_audit.log`). Additional spill-capacity, byte-hash
  and index-capacity checks plus independent connected fixture controls: 12 passed
  (`connected_controls_and_spill_regressions.log`). Counts overlap; do not sum them.
* Unchanged repository baseline orchestrator/metrics checks passed
  (`baseline_orchestrator.log`).
* Named input: `data/synthetic/identity_recovery_v1/`, three simulated identities,
  two fiscal years, 20 actual SJE lines, payment/reversal/receipt/PO cases. Fresh
  real orchestrator completed at 20:47:03 AZT. Both posting chains, GL, permutation
  and legacy views passed. Compact pipeline archive:
  `../corpus_runs/run_20261114_204428/`. Full fixture generations remain under
  `session-run.tmp/identity_recovery_v1_final/`.
* Independent Scala controls checked expected expense 160 / receipt 200 /
  commitment 120 in FY03, expense 30 in FY04, preserved literal IDs, balancing,
  reference keys, rule/date presence, actual posted state and matched coverage.
* Separate JVM extraction and reduction passed (`report/`): 38 records, four
  groups, 13 indexed blocks / 476 index bytes, aggregation capacity one, 30
  cross-cut partials, reduction fan-in two / five passes, zero reconciliation
  failures. Process RSS/CPU are in extract/reduce logs. These are fixture costs,
  not corpus measurements. Reporting tests separately consume 500,004 SJE rows,
  two groups each above the previous 250,000-row guard, through actual posting.
* Preliminary fixture `../corpus_runs/run_20261114_194233/` failed; see its
  `ATTEMPT_STATUS.md`. Reject evidence accidentally matched the SJE selector.
  Corrected `APARejects_` filenames and independent row-count/schema checks prevent
  treating that attempt's passing amount controls as completed-run evidence.
* `source_sha256.txt` identifies the connected run's frozen financial/reporting
  source. A subsequent reject-evidence fix counts blank physical lines correctly;
  its focused regression and final source hashes are recorded separately. It
  changes reject evidence only; the connected fixture contains no blank rows.

## Admission failures

The real current master loads, but strict reference lookup fails before financial
execution. The read-only inventory finds **371 names with multiple keys**: agency
3, fund 268, object 2, program 80, source 18. `conflicts.csv` preserves every
candidate and source row; `input_manifest.csv` hashes the actual snapshots.
Example: Department of Veterans Services has keys 160 and 360 for agency code 912.
The snapshots do not supply an adequate effective-date selection contract. No
last-row override or new logical-key crosswalk was silently applied. Resolve
these mappings before attempting a new financial corpus.

Only 75 GiB is free; the previous full run occupies about 128 GiB before additional
working space. No old corpus generations were deleted. Independent architecture
review and target GenevaERS GERS-002/003 capabilities remain open. All full-corpus,
new-population and model-benefit claims remain unverified.

Final physical-line regression: 11 APA tests passed; the environment-dependent
connected checker was canceled in that last command because its path variable was
not supplied. Its earlier executed connected check passed in the 12-test suite.
`final_source_sha256.txt` records the final reject-line correction. The earlier
connected snapshot hashes remain in `source_sha256.txt` and the report manifest.
