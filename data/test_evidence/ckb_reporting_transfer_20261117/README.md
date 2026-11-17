# CKB-REPORT-TRANSFER-20261117 evidence

Owner designation occurred during the ongoing reporting attempt on2026-11-17.
Protocol: docs/REPORTING_CKB_TRANSFER_STUDY.md. Initial snapshot files are copies
of actual ongoing producer evidence, not final accepted output. Each snapshot
has SHA256SUMS. Producer timers/metrics become complete only at process exit.
Do not infer cumulative I/O from inventories of currently existing files.

Read-only observer18501 samples reducer-input file count/bytes every60 seconds
and captures a conclusion snapshot on workflow completion/failure/driver loss.
Its own PID/log are operational records, not engine metrics. The conclusion
marker explicitly distinguishes workflow completion from result acceptance.
Existing producer storage/process monitoring remains the source for earlier
observations. No mainline financial code or process settings changed.

Verification: Bash syntax passed. A temporary completed-workflow fixture with
one reducer spill exercised initial capture, terminal capture and success outcome;
both snapshot SHA256SUMS verified. This tests the observer, not financial behavior.
Independent final interpretation and matched rerun results remain pending.

Final baseline: workflow completed15:17:18UTC; original session84820 exit0.
CONCLUSION.md records actual timings, controls and interpretation. conclusion/
contains the observer's final snapshot with verified SHA256SUMS. Observer18501
finished normally; its retained PID is historical, not current liveness. Final
financial/report acceptance is recorded separately in full_reporting/ACCEPTANCE.md.
Improved implementation and paired timing have not yet run.
