# Combined baseline report acceptance — pending

2026-11-17. Current continuation: session-run.tmp/phase2_fused_reporting_v2.
This checklist is preparation, not a passed report receipt.

Accepted native/combined Scala year summaries establish the required reporting
population:14 years;266,319,818 movement rows plus19,957,916 period-movement
ledger rows =286,277,734 financial input rows. No opening partitions are scheduled.
These are rows across two record types, not independent events. The historical
portion is261,529,946 journal rows and the wholly-generated addition4,789,872;
49,091 reused source copies must not be added again.

Before acceptance:

1. Verify original session exit, WORKFLOW_COMPLETE, REPORT_CHECKS_COMPLETE,
   CENSUS_COMPLETE, report EXTRACT_COMPLETE/REPORT_COMPLETE and no failure marker.
   A status file by itself is insufficient.
2. Compare actual28 sorted movement/balance partition manifests, each row count
   and source path, with all14 accepted financial year summaries. Check summed
   financial input286,277,734 against actual report input_manifest, metrics and
   synthetic coverage. The existing checker establishes internal consistency;
   this external population check prevents a consistently incomplete report
   being mistaken for the full corpus.
3. Inspect all statuses/row counts in reconciliation.csv,
   synthetic_validation.csv and synthetic_conservation.csv and the actual Scala
   ConnectedViewsCheck log, including nonempty outputs. Retain checksums and
   census. A successful extraction without reduction is not a completed report.
4. Inspect useful output examples across wholly generated, reassigned historical,
   real and unresolved identities; dated attributes, synthetic temporal cuts,
   aggregate views, attribution and effective-date behavior. Trace selected
   examples back to declared dimensions/lineage. Do not interpret signed journal
   totals as spending or assume the financial periods prove walk-forward state.
5. Recheck frozen compiled class manifest and actual executed driver/configuration;
   retain process/time/RSS/storage evidence separately. Record any missing metric
   rather than infer a cost point. Current timing is diagnostic, not controlled.
6. Record explicit scope limits: no allocation/denominator loop, generalized
   instrument-ledger walk-forward, complete99 integration or Q2 model evaluation.
   Prepare the validated-results commit only after all required evidence passes.
7. Then create the owner-authorized Zenodo upload script, explicit version/file
   manifests, checksums and resumable state with environment token. The existing
   scripts/zenodo_upload.py is a different raw-source publication workflow and
   must not be run for this task. No upload or publication is authorized here.

2026-11-17 owner update: this attempt was designated mid-run as a formally
measured observational baseline for CKB-REPORT-TRANSFER-20261117. This supersedes
the earlier diagnostic-only intent in item5; it does not supply missing metrics
or establish a replicated cost point. Protocol:
docs/REPORTING_CKB_TRANSFER_STUDY.md. Retain concluding evidence and interpretation
as part of final result review, including implementation transfer failure despite
owner-confirmed complete builder access. Do not infer hidden training causation.

Prepared receipt helper: scripts/verify_phase2_report_receipts.sh REPORT_ROOT
FINANCIAL_ROOT NEW_EVIDENCE_DIRECTORY (all absolute). It rejects unfinished or
failed attempts before creating evidence, compares all28 financial partitions
and producer/extract hashes, checks actual source/sorted sizes, verifies small
side-input hashes, requires nonempty Scala-parser control scans and frozen class
hashes. Metadata-fixture evidence: ../receipt_verifier/. It does not replace
session exit verification, final semantic review or census inspection, and has
not accepted the running report. No financial logic is computed by the helper.
