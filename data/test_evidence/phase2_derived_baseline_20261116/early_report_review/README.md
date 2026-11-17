# Early semantic observations — final review pending

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

## Early observed historical grouping

The planned observed targetv253147763127656255907762210784412714 has now emitted
coverage:attributed, SIM_I000011, prepared_sorted_assignment_stream,
2,487,019 financial input rows, identity_rekeyedfalse. Original source key
0000099085 and the normalized key both map toSIM_I000011 in actual retained/
prepared assignments. The normalization candidate explicitly saysobserved_rekeyed.
The combined master retains JOURNAL ENTRY, effective2031-01-01, rather than
inventing a supplier identity for that source grouping.

Here attributed means that an analytical template is assigned. It does not turn
JOURNAL ENTRY into a resolved legal party, establish real business attributes,
or itself create financial activity. identity_rekeyedfalse means the reporting
overlay preserves its financial input identity; the earlier structural stable-ID
recode did occur, as the normalization candidate records. The count mixes
journals and ledgers, not independent transactions.

observed_*.csv retain actual emitted coverage, original/prepared assignment,
combined master and one actual normalization candidate. Full attribute/date-view
checks and recheck against the final completed generation remain pending, along
with all financial/report controls and the reassigned/wholly targets. No final
RESULT_REVIEW_COMPLETE marker or maturity upgrade is justified by this sample.
