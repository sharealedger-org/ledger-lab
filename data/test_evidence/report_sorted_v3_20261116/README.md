# Corpus sorted/sequential reporting continuation

Owner requested the post-reboot corpus run and active progress monitoring.
Actual run: `session-run.tmp/report_sorted_v3_20261115/reboot_attempt/`.
Launcher retained here; financial inputs are validated FY03–FY16 Run 1 outputs
under `session-run.tmp/synthetic_reference_corpus_v2_20261115/monthly_a/`.
This continues that execution's reporting; it is not a fresh financial replay,
three completed executions, a target GenevaERS capability closure, or a cost point.
Code checkpoint b540e5e plus retained reporting patch and ReportingSort source;
18 focused tests pass. The new equivalence fixture needed reportProfile=expanded.

Producer/consumer: canonical SJE and actual ledger -> bounded Scala external
ReportingSort (100,000 rows/chunk, fan-in 16) -> presorted sequential expanded
ReportingPass 5v -> bounded spill reduction 5r -> ConnectedViewsCheck and bounded
CorpusAdmissionSummary. Analytical sort copies preserve canonical fields, literal
IDs/quotes, rows and byte counts; original financial outputs remain intact.
No Python, financial Bash logic, generated accounting, or identity fabrication.

All 28 sorts passed conservation: 273,380,488 financial rows, 94,710,620,118 bytes.
Sort wall 4,784.58 seconds (~79m45s), separately measured from extract/reduce.
Extract: 273,447,217 total input rows including dimensions, 65,716 groups, one
financial input traversal, zero order-index blocks/bytes, zero reconciliation
failures. Largest group: 217,406,124 rows. Aggregate capacity 50,000 keys; feature
capacity 250,000, measured peak 46,404. Extract 12,541.516 seconds (~3h29m),
1,400,717,312 bytes maximum RSS. Reduction 13.774 engine seconds (24.87 process
wall including sbt); two merge passes, 1,556,150 reduced output rows.

ConnectedViewsCheck passed 11,835,781 reconciliation rows, 20 history oracles,
2,102,912 conservation rows, all 273,380,488 financial rows (52,471,280 attributed;
220,909,208 unattributed), 984 temporal-summary and 4,396 attribute-summary rows.
Final bounded output census/completion marker was pending when this receipt was
first written; use completion evidence appended below before claiming it complete.

Runtime qualifications: owner reported machine sleep during FY2012 admission.
Full-corpus stage follows that admission; treat host conditions as exploratory,
not clean replicated cost evidence. Battery throttling cause is unverified.
Monitor lost samples after 04:38:32 AZT because du raced with engine spill cleanup;
replacement PID 35362 resumed at 04:43:20 AZT. Patched sampler preserves existing
samples and leaves allocation unknown on transient du failure, while independent
free-space stop remains active. No engine code changed during corpus execution.
Disk free about 109 GiB near validation. Original failed/superseded report scratch
and all financial outputs retained. No archive copied; further independent
executions and verified archival remain outstanding.

Completion verified: CORPUS_EVALUATED and EXIT_STATUS rc=0 at
2026-11-16 04:46:40 AZT (11:46:40Z). Final census is retained in report_profile.csv;
71,119,226 temporal-cut rows and 12,985,882 attribute-total rows. This supersedes
the pending-census statement above. One financial execution's reporting is now
complete; additional executions and OneDrive archival remain outstanding.
