# Completed extraction/reduction — final acceptance pending

2026-11-17. Actual report generation now lives at phase2_fused_reporting_v2/report.
EXTRACT_COMPLETE and REPORT_COMPLETE produced; driver has moved into independent
Scala report checks. No final workflow/result acceptance is claimed by this receipt.

Financial input286,277,734; extract total records_read286,415,761 also includes
selected dimensions/assignments. Vendors98,865; zero reconciliation failures.
Selected coverage preserves all four planned identities, including wholly target
SIM_I000001 with978 mixed journal/ledger input rows. Counts are not event counts.

Scala extraction17,403.32s; OS process17,416.02s (4h50m16s), max RSS885,792,768B.
Scala reduction20.81s; OS process32.16s, max RSS437,993,472B. These are actual
observational baseline timings, not replicated paper cost points.

Ordinary accumulator writes5,205,090,962B; synthetic accumulator writes
172,057,919,331B; initial separate-reducer spills345,985,746B across118 files.
Reducer intermediate writes812,741,417B and two merge levels. Counter scopes
are defined in docs/REPORTING_CKB_TRANSFER_STUDY.md:1476 synthetic accumulator
merge levels and40 ordinary levels sum work across vendor folds, not corpus
traversals. The counters exclude different final-output/read categories; do not
call these device I/O or attribute all elapsed time to one writer family.

Extract consumes one traversal of its sorted financial inputs. This excludes
report-order sorting/materialization, partial-file merges and subsequent checks.
Current code already streams consumed leaf SJEs and retains vendor join targets;
peak_vendor_rows150,838,846 does not mean that many resident records. The redesign
addresses identified dependent views and avoidable summarization materialization.

Producer logs/metrics and final input manifest copied here. External28-partition
hash/count check, complete verdict scan results, census, selected dated-view
inspection, final class check and workflow exit remain required before acceptance.

## Final acceptance supersedes the earlier pending checkpoint

Completed workflow exit0, independent checks/census, exact producer/input/hash
comparison and four-class/date semantic review now pass. See ACCEPTANCE.md,
receipts/ and semantic_review/. The earlier text above preserves the stage-time
observation before final checks; it is not the current acceptance status. No
upload or improved rerun has occurred.
