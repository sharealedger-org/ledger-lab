# Financial profile passed; source-policy admission failed

This is one FY2012 admission profile, not a full-corpus run or a new cost point.
The existing financial pipeline completed, but the fresh source-policy check
found that legacy REV agency aliases attributed 2,452,576 of 2,745,656 receipt
SJE rows to master IDs. The intended workload leaves these receipts unattributed.
Do not use this population to admit the three synthetic-reference executions.

The financial outputs and stopped partial reporting attempt are preserved under
`session-run.tmp/corpus_admission_20261115/`. Compact source-policy evidence:
`data/test_evidence/synthetic_reference_corpus_v2_20261115/`.
The corrected replay uses a fresh root and a receipt-safe input snapshot, with
the original master and all exclusion lineage preserved. Historical VA baseline
results are unaffected. See `docs/SYNTHETIC_REFERENCE_CORPUS_V2_CONTRACT.md`.
