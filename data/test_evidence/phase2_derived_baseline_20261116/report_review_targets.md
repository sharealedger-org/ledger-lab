# Selected combined-report review targets — pending report output

2026-11-17. These are selected from actual source/normalization/dimension
metadata, not claims that unfinished report outputs passed. No private actor
recipe keys are copied into this review list. Full controls/population checks
remain necessary; these examples add semantic inspection.

| Population | Stable vendor | Template | Provenance and finished-output checks |
|---|---|---|---|
| Observed historical grouping | v253147763127656255907762210784412714 | Inspect actual assignment if any | FY31 normalization actor_candidates maps original0000099085 as observed_rekeyed. Combined master retains label JOURNAL ENTRY and shifted effective start2031-01-01. Treat it as a preserved source grouping, not proof of a resolved real supplier. Check reporting identity and missing/pre-effective attribute behavior without adding synthetic activity. |
| Reassigned historical activity | v777305469290280721924923592149096066 | SIM_I000030 | Actual FY31 normalization candidate originates from zero vendor, classreassigned_activity. Combined master is FICTIONAL_REASSIGNED; assignment confirmed. Check separate fictional attribution and coverage, with no additive reuse of the historical source copy. |
| Wholly generated activity with dated attributes | v992291562825503351596510708858841042 | SIM_I000001 | RegistryV00000001 and actual source_lineage EXP eventN2c011cf1b86addd4b77bded26a51c661, effective2030-07-24 /available2030-07-25, generation_classwholly_generated. Combined master changes SMALL toLARGE at2032-06-01. Check temporal views follow declared cuts and preserve identity; do not treat template context as observed business truth. |
| Unattributed aggregate | 0000000000 | None permitted | Combined assignment file has no zero-key row. Check coverage remains unattributed with blank template and no invented party identity. A large aggregated key is not a resolved actor. |

The wholly event source locator is
`SRC1:VkFSYXdGaWxlcy9GWSAyMDMxIEV4cGVuZGl0dXJlL0V4cGVuZGl0dXJlcyBKdWwgMjAzMC5jc3Y:6119`.
Its source lineage amount is27.53; reporting aggregates are not expected to retain
this individual amount as a distinct row. Use retained financial lineage for
an individual event trace, and Scala-produced reconciliation/conservation for
aggregated financial values. Do not compute replacement financial totals in Bash.

Selection evidence locations under the repository runtime tree:

* phase2_fused_baseline_v1/years/FY31/source/normalizations/FY31/actor_candidates.csv
* phase2_full_v6/inputs/generation/registry.csv and inputs/source_lineage.csv
* phase2_fused_reporting_v2/dimensions/VendorMaster.csv and synthetic_assignments.csv

All runtime paths above are under session-run.tmp/. Finished extraction/reduction,
input-manifest hash/count comparisons and report controls are still pending.

Early emitted-row observation,2026-11-17: zero0000000000 hasattributionunattributed,
blank template, no_synthetic_party_for_unattributed rule andidentity_rekeyedfalse.
Retained in early_report_review/zero_coverage.csv. Its150,838,846 financial input
rows are mixed journal/ledger records, not transactions. This one-key observation
must be rechecked against the completed report; it does not complete this review.
