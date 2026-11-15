# Synthetic-reference v2 admission evidence

## Corrected admission completed

All 2,745,656 FY2012 receipt SJE legs retain zero identity. The complete financial
population is 22,260,988 movements plus 1,031,381 posted rows. The zero group has
17,592,858 movements and 335,103 posted keys, with 6,094,299,500 movement bytes
and 48,258,968 posted bytes. Maximum zero movement width is 409 bytes.
GL is byte-identical across the receipt input-policy correction.

Corrected actual reporting passes 1,031,381 reconciliation checks, 20 history
oracles and 548,608 conservation checks, covering all 23,292,369 financial rows.
See real_year_controls.log and the metric/profile CSVs here. Final diagnostic
census selected vendor_attribute_totals rather than the global attribute summary;
the initial header mismatch was diagnostic only. No financial/report rerun needed.
Named quote-preservation and unchanged baseline regression: 47 tests pass.

Extract 1,479.796 s, reduce 1.731 s; index preparation 549.667 s is included in
extract, with two reporting input passes. Extract maximum RSS 1,347,203,072 bytes
under a 1-GiB heap/four logical CPUs. Report retained allocation 3,342,428 KiB;
synthetic cumulative spill writes 7,159,567,051 bytes are not peak scratch.
Actual output cardinalities and zero-byte widths are in real_year_report_profile.csv.

Three new runs are admitted conditionally on live per-run resource checks, with
identical inputs/code/settings, actual downstream reports and repeat comparison.
Planning allowance is roughly 200–220 GiB per retained full run before sharing,
and approximately one to two days for all three cycles including reporting and
archival. These are projections from one year, not bounds or measured full runs.
Initial full-run minimum free space is 225 GiB; subsequent admission also requires
the actual first-run allocation plus 20 GiB. A separate monitor stops the driver
at 20 GiB free. Current free space is about 418 GiB on AC power, Intel i7-5557U,
16 GiB RAM/two physical/four logical CPUs. All 385 raw hashes pass; no .part files.

After the first full run evaluates, identical files in these task-created admission
profiles may share its hard links after SHA-256 and byte comparison. This preserves
every path/byte and each profile's accepted/rejected status, recovers overlapping
storage, and does not make a failed population an accepted input. Later full runs
still require whole-product equality; only post-evaluation storage is shared.
Actual allocation/free-space guards remain authoritative if projections miss.
No formal cost point, target-runtime capability closure, ACFR tie-out or AI result.

## Earlier preparation and failed attempts

Admission in progress, 2026-11-15. Three full executions and OneDrive archival
have not started. Existing historical VA results remain separate and valid for
their original scope.

The first FY2012 monetary controls passed but receipt identity policy failed.
`failed_policy_fy12_profile.csv` is the actual Scala journal/ledger census;
SOURCE_POLICY_FAILED records the rejection. The entire failed attempt remains
local, including financial outputs and stopped report scratch. It is not the
selected population. `receipt_policy_manifest.csv` records the corrected master
snapshot's 232 excluded REV aliases and 1,016 retained rows. Original rows and
inclusion/exclusion lineage remain in the new input snapshot.

`receipt_policy_tests.log`: 46 tests passed, none failed/canceled. Corrected fresh
FY2012 replay and report cardinality/time/scratch admission remain pending under
`session-run.tmp/corpus_admission_20261115_receipts_v2/`. The contract is
`docs/SYNTHETIC_REFERENCE_CORPUS_V2_CONTRACT.md`; GERS-002/003 remain open.
