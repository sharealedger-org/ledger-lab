# Early semantic observation — final review pending

2026-11-17. Extraction remains active. A complete emitted coverage row for
vendor0000000000 is retained in zero_coverage.csv, copied from the actual Scala
synthetic_vendor_coverage.csv output. Attribution isunattributed, template is
empty, assignment ruleno_synthetic_party_for_unattributed, identity_rekeyedfalse.
This matches the planned unresolved-key semantic check.

Its150,838,846 financial_rows cover journal and ledger input records; they are
not a count of independent events, suppliers or economic transactions. This
shared aggregate key does not by itself identify historical versus wholly
synthetic provenance. No amounts were recomputed in Bash or another runtime.

The growing coverage file had3,932 physical lines at observation (including
header, with output buffering); this is an activity sample, not the final number
of vendors. No EXTRACT_COMPLETE, REPORT_COMPLETE, full population/hash comparison
or completed full report/control acceptance is claimed. Recheck the final
published report generation, all target classes and full controls before marking
result review complete. Source: session-run.tmp/phase2_fused_reporting_v2/
report.attempt/synthetic_vendor_coverage.csv.
