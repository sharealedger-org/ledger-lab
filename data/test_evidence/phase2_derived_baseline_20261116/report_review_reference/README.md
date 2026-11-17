# Reference facts for completed-report semantic review

2026-11-17. These are selections of actual retained reference inputs, not accepted
report results. Selection only; no replacement financial computations.

ReportingPass.masterAttribute selects a vendor dimension at fiscal-year June30,
not each transaction date. The wholly target's master is SMALL through2032-05-31,
LARGE from2032-06-01. Thus FY2031 attribute views should select SMALL and FY2032
onward LARGE where the target has financial rows. Do not claim event-date master
selection from these outputs. Reassigned target type is FICTIONAL_REASSIGNED.

Synthetic temporal views separately select EVENT_DATE, fixed AS_OF2031-06-30,
and supplied RUN_DATE2045-07-01. SIM_I000001 SAL risk changes LOW to HIGH on
2042-03-03; owner changes SIM_P000001 to SIM_P000002 on2042-07-01. The fixed
as-of view should use LOW and owner1, run-date HIGH and owner2; event-date follows
the declared date segment, including before_baseline where appropriate. Actual
profile versions/hierarchy and vendor-scoped owner IDs must be inspected after
completion. Conserved financial measures come from Scala controls, not this note.

Selected observed/reassigned templates SIM_I000011/SIM_I000030 have baseline
SAL/CAR records retained here. No private scenario truth or evaluation keys added.
The four-target completed-generation review remains pending.

Prepared during a verified wait on live reporting session84820. These reference
facts and the API-interface receipt are independent preparation, not a substitute
for completion of the running financial/reporting workflow.
