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
