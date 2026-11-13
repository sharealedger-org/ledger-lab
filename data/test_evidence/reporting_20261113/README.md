# Expanded reporting fixture evidence

2026-11-13. Synthetic data only. `ReportingPassSpec` generates expenditure,
commitments, reversal and unattributed revenue across two fiscal years, posts
them with the actual Scala posting engine, and supplies versioned dimensions.

`tests.txt`: 42 passing tests (34 existing baseline tests and 8 reporting tests).
Negative cases include corrupt/missing balances, bad amounts, unordered sources,
duplicate inputs and exceeded state bounds. Small/large spill configurations
produce identical tested products. Explicit opening-plus-movement is tested
separately from the default period-movement basis.

CSV files and process logs come from the Bash launcher using separately committed
fixture posting outputs, two JVM invocations and an output path containing spaces.
Extraction: 34 records, 3 vendor keys, 20 spill chunks, zero reconciliation failures.
Reduction: fan-in 2, five merge levels, 14 final cross-cut rows. Retained logs include
OS resource measurements; these tiny runs include sbt/JVM overhead and are not
corpus cost points. Intermediate partitions are omitted from this evidence copy.
Absolute input paths in the manifest identify temporary local synthetic fixtures;
the repeatable fixture producer is the Scala test suite.

No AI fitting, real vendor linkage, full VA-reference coverage, corpus-scale run,
or GenevaERS runtime capability is established here. See
[design](../../../docs/REPORTING_PASS_DESIGN.md).
