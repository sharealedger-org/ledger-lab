# Completed-generation semantic review

Actual report: phase2_fused_reporting_v2/report; workflow session84820 exited0.
Raw rows were selected by ../sample_review.sh after the timed census completed.
The files retain producer rows and decimal values; no financial totals were
recomputed outside Scala. Signature selection chooses representative rows, not
new totals for those signatures. Source CSV has bounded flat fields in the
selected rows; schema guards passed. The timing wrapper returned1 after its
kern.clockrate read was sandbox-denied; all three sample files were produced.
Do not treat that wrapper return as an engine failure or claim missing sampler
RSS counters. Independent semantic_checks.txt checks passed on the actual rows.

* Zero0000000000: all three sampled date modes remain unattributed, blank template
  and blank party. Final coverage150,838,846 mixed journal/ledger rows matches the
  early observation. This shared key does not identify a legal party or provenance
  class by itself.
* Observed v253147...: final coverage2,487,019/SIM_I000011 agrees with prepared
  assignment and the source grouping JOURNAL ENTRY. Master type EXP is preserved
  at FY cutoff. Template attribution is not real-party resolution or new activity.
* Reassigned v777305...: final coverage449,292/SIM_I000030 agrees with original
  zero-key normalization classreassigned_activity and FICTIONAL_REASSIGNED master.
  Reporting identity_rekeyedfalse describes the overlay; upstream reassignment
  remains explicit. Private actor keys were not copied into the review projection.
* Wholly v992291...: final coverage978/SIM_I000001. Fixed AS_OF2031-06-30 selects
  LOW/owner1; RUN_DATE2045-07-01 selects HIGH/owner2. EVENT_DATE samples include
  before_baseline with empty enrichment, LOW/owner1, HIGH/owner1 after the SAL
  change, and HIGH/owner2 after the later CAR change, with selected version IDs.
  Master instSizeTier is SMALL at FY2031 cutoff and LARGE from FY2032 onward,
  matching the2032-06-01 input change. Master selection is at June30 FY cutoff,
  not transaction date. Balance event-date mode uses posted-period cutoff.

Coverage counts are financial input rows, not independent events. The source
lineage target N2c011cf1b86addd4b77bded26a51c661/effective2030-07-24/27.53 remains
a selected wholly-generation provenance example, not a distinct required report
row: views summarize at their declared grain. Reference facts are retained in
../../report_review_reference and early_report_review; complete financial values
are supported by the exhaustive Scala reconciliation/conservation scans.

These views implement supplied effective-date cuts; they are not proof of an
availability-safe forecasting dataset, allocation/basis loop, legal identity,
generalized walk-forward ledger, target GenevaERS runtime or Q2 model evaluation.
