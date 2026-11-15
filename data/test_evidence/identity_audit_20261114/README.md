# Identity audit evidence — 2026-11-14

Read-only compiled Scala audit; full findings in
`docs/IDENTITY_AUDIT_FINDINGS_2026-11-14.md` and scope/limits in
`docs/IDENTITY_AUDIT_CONTRACT.md`.

This compact copy contains master comparisons, source/count summaries, canonical
input manifests and order findings, independent count controls, top group/name
tables, parser-lookup counterfactuals, resource logs and 26 passing focused tests.
The financial producer, posting, reporting and original financial outputs were
not changed. No vendors, financial journals or corrected ledger were generated.

The full diagnostic root is
`/Users/ktwitchell001/workspace/identity_audit_20261114/`.
Large per-name/global-name/vendor-year/segment tables remain there rather than in
Git. This directory is not a full substitute input root for rerunning the summary.
`diagnostics_manifest.csv` fingerprints the external diagnostic CSV artifacts,
including input master snapshots and failed/probe artifacts; it is not a manifest
of completed financial outputs. No OneDrive/cloud copy of this new audit is claimed.

Execution history:

- Initial Bash launcher completed master/raw phases with audit v1. All 14 accepted
  source counts match recorded TTL metrics.
- Its canonical phase stopped at an ordering assertion. Initial cleanup masked
  the underlying error as an incomplete read; the probe preserves the original
  `c12464` → `c1246` failure and marks the incomplete digest `PARTIAL_READ:`.
- Canonical v2 records literal decreases and externally combines metadata counts.
  It completed all 14 journals and actual posted ledgers. It changes neither
  financial sorting nor reporting. There were 214 literal decreases and zero
  vendor/year/side re-entry groups in the scanned files.
- Separate Scala summaries completed independent source/journal identity and
  recorded canonical-row checks with no mismatches. Parser comparisons concern
  already accepted physical source rows, not newly recovered financial data.
- The final reusable Bash entry point now runs all five diagnostic stages. The
  full execution recorded here used the preserved initial launcher plus explicit
  canonical/summary recovery commands in the logs; the final entry point was
  syntax-checked, not used to repeat all input scans unnecessarily.

`source_versions/IdentityAudit_v1.scala.txt` preserves the initial diagnostic.
`final_source_sha256.txt` fingerprints the final utility sources/contract; initial
and canonical-v2 source receipts retain their original fingerprints. Final tests
include 13 unchanged TTL/one-read tests plus audit/parser/summary fixtures.

Heuristic classifications are deliberately separate: the raw v1
`meaningful_name_candidate` label is broad. Use the refined
`summary/candidate_name_summary.csv` and published `review_label_rules.csv` when
discussing candidate population size. Even its 3,794,594 missing source-name
candidates are not verified legal parties or a prescribed synthetic vendor count.
Literal payment/PO key overlap, padding and c-prefix numeric resemblance do not
establish new identity bridges.

`summary/COUNT_CHECKS_PASS` certifies diagnostic row counts only. It does not
certify source attribution, financial dimensions, accounting correctness, bounded
production reporting or a new measured cost point.
