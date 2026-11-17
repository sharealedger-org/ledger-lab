# Reporting reference-schema preflight — repair isolated, not yet applied

Actual historical `aRefFundCSV.txt` has five columns: detail key/code/name and
parent fund code/name. Immutable v6 generated reference has only the first three.
The current `PhaseTwoBaselineDimensions` requires identical headers and would
fail before publishing dimensions. The other four reference headers match.

An isolated copy of the existing metadata class and its test suite was compiled
with the existing Scala2.12.20 compiler, 512MiB heap and one active processor,
against the frozen test/main classpath. Output classes live only in
`session-run.tmp/fused_metadata_schema_fix_v1/classes`, which is not on any
financial worker or production driver's classpath. Full frozen-class checksum
verification still passed after this check. No immutable input changed.

`before_fix.txt` demonstrates the actual fund-schema case fails on frozen code;
the existing regression and incompatible-column rejection pass (2/3). The proposed
repair accepts exact prefixes only, pads missing trailing fields with empty values,
retains historical parent fields, checks row width and preserves ambiguous labels.
It neither infers a generated parent-fund relationship nor discards historical
columns. `after_fix.txt` records all three tests passing on the isolated version.

`proposed.patch` includes the repair and regression tests. **It is not applied to
production source/classes yet.** Keep the active financial build frozen. After
financial processing and global controls finish, apply the patch, run required
baseline/integrated checks, then continue dimensions and reporting from the
successful financial partitions with a newly recorded metadata build. Preserve
the original metadata failure attempt if the current driver reaches it first;
use a fresh dimensions output generation. No successful financial replay is needed.

Acceptance remains pending: actual full metadata publication, reporting and final
result review. This is a reference adapter correction, not financial or Q2 work.

## Actual first-wave metadata preflight

The isolated version subsequently completed exit0 against immutable actual
historical/v6 master/reference/assignment snapshots and the four completed
FY31–FY34 actor-candidate files. Output generation:
`session-run.tmp/fused_metadata_schema_fix_v1/actual_first_wave_dimensions`.
It published DIMENSIONS_COMPLETE, master, assignments and all five references.
The master sort consumed435,183 candidate rows/125,704,736 bytes; assignments
494,880 rows/71,262,720 bytes, with32MiB bounded buffers and fan-in16. Actual
sort metrics, completion marker and output hashes are retained adjacent.

This preflight used512MiB/one CPU and ran concurrently as post-engine metadata
inspection; it is not a financial worker or cost measurement. Financial source
and class files remained unchanged, and the full class checksum manifest passed
again afterward. The production patch remains unapplied. The actual all-year
metadata generation and reporting consumers still require later acceptance.

## Prepared reporting continuation — not executed yet

`scripts/run_phase2_fused_reporting.sh` defaults to check-only. Its run mode
requires all14 completed years, the actual shared Scala financial completion
marker with all three full-population counts, unchanged financial main source
and class bytes (except the declared dimensions adapter), AC and resource gates.
It links accepted financial partitions/controls into a fresh reporting generation;
it never invokes transformation or posting. Original financial and failed metadata
attempts remain intact. The new dashboard can show its explicit continuation scope.

Shell syntax checks pass. Actual incomplete full-root and completed two-year
fixture guards each exited1 before creating an output directory; their captured
messages are adjacent. These prove admission rejection only. The continuation's
actual dimensions/sort/report/check/census workflow has not run or been accepted.

After all financial work ends, inspect the full controls, apply and verify the
metadata patch, commit that repair, then launch in its own process group:

```bash
exec bash scripts/run_phase2_fused_reporting.sh --mode run \
  --financialRoot "$PWD/session-run.tmp/phase2_fused_baseline_v1" \
  --out "$PWD/session-run.tmp/phase2_fused_reporting_v2"
```

Switch the live dashboard pointer to the new root after startup. Keep the
original financial root and its failure marker/evidence; full acceptance and
validated-results commit still require actual reporting and result inspection.
