# Reporting output audit contract

2026-11-17. Post-engine verification only; outside measured financial processing.
The frozen baseline and CKB producers emit CSV views. Their physical row order
may differ inside a vendor. The falsifiable claim is equality of every complete
record, including duplicate multiplicity, for all 21 declared financial,
synthetic, reference and coverage outputs. Metrics and input/config provenance
have separate comparisons because implementation counters intentionally differ.

`tools/ReportingOutputEquivalence.scala` reads either CSV or its verified gzip
archive, requires identical headers, and compares complete UTF-8 records after
lossless Base64 encoding and canonical sorting. Vendor-first files must remain
vendor ordered; only one vendor group is sorted at a time. Other outputs use
a global sort. No arithmetic, financial rules or new perspective is introduced.
The existing Scala SortEngine supplies chunk ordering and merge mechanics, with
explicit 4096-record chunks, 4096-character record bound, 16-reader fan-in and
100000-run path bound. No unbounded history map is used. Both sides of each group
are compared record by record, retaining duplicates. Only audit-owned scratch is
removed; sources and archives are read-only. Failed verification does not accept
the run. Reserve admission must precede the full audit; allow at least 16 GiB
scratch above the retained 40 GiB reserve for this frozen corpus's largest group.

Compile and execute this separate audit after the timed workflow, outside its
frozen build. Test falsifiers cover changed records, lost duplicates, header
changes, reordered rows, quoted vendor IDs, gzip, global groups and forced
multi-run sorting. The complete full-corpus comparison remains pending until its
actual receipt exists. Byte/record identity is deliberately stronger than totals;
it is not an independent validation of accounting rules shared by both runs.
