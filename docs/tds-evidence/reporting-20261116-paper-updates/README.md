# Reporting execution evidence for Papers 1 and 2

Prepared 2026-11-16 for the private 00–02 revision process. Evidence cutoff:
2026-11-16 04:46:40 AZT (UTC−07:00), when the reporting driver completed with rc=0.

The expanded reporting workload now completes at full FY2003–FY2016 scope. Paper 1
can add a measured example of producing additional analytical outputs from retained
financial records. Paper 2 can report a functioning, reconciled analytical
foundation for future inquiry. This execution does not evaluate a new AI model or
generate a new synthetic transaction population.

This is an editing input, not a revision of the private chapters. Use it with the
[member editing plan](../../../tracks/D-experiment/MEMBER_REPO_EDITING_PLAN_2026-11-14.md).
Keep the older baseline and combined-source results attached to their original
populations and revisions. The concise [run receipt](../../../data/test_evidence/report_sorted_v3_20261116/README.md)
and source artifacts below supply the evidence.

## Execution and population

The run reused the validated first financial execution's retained monthly SJE
(Standard Journal Entry) and actual ledger outputs. It created separate analytical
sort copies, then ran one sequential common-key reporting traversal and a separate
spill reducer. It did not replay source transformation or posting.

The financial population consists of 261,529,946 journal rows and 11,850,542 ledger
rows, totaling 273,380,488 financial input rows in 28 files. Journal rows are
accounting lines, not counts of distinct source transactions. The files contain
94,710,620,118 bytes, including their format overhead. The reporting counter of
273,447,217 also includes dimension/assignment rows and must not be substituted
for the financial count. Sources: [sort manifest](../../../data/test_evidence/report_sorted_v3_20261116/sort_manifest.csv),
[extract metrics](../../../data/test_evidence/report_sorted_v3_20261116/extract_metrics.csv),
and [connected checks](../../../data/test_evidence/report_sorted_v3_20261116/corpus_checks.log).

Synthetic reference histories and deterministic template assignments augment
observed-data-derived financial outputs. Those relationships and histories are
simulated; they do not establish real Virginia party relationships. The receipt-safe
input policy leaves agency-grain receipt identities unattributed and differs from
some historical master snapshots. See the [execution contract](../../SYNTHETIC_REFERENCE_CORPUS_V2_CONTRACT.md).

Code identity is b540e5e72f2ecf02f0e39e4966dc80be5d081044 plus the retained reporting
patch and ReportingSort source, not that commit alone. The receipt retains
[source changes](../../../data/test_evidence/report_sorted_v3_20261116/source_changes.patch),
[sorter source](../../../data/test_evidence/report_sorted_v3_20261116/ReportingSort.scala),
[reporting source hashes](../../../data/test_evidence/report_sorted_v3_20261116/reporting_source_sha256.txt),
[input hashes and counts](../../../data/test_evidence/report_sorted_v3_20261116/input_manifest.csv),
and the [actual launcher](../../../data/test_evidence/report_sorted_v3_20261116/reboot_run.sh).

## Suggested Paper 1 insertion

Place after the existing measured observations, with its own workload and revision
label. Preserve the earlier failed reporting attempt as history; replace any
current statement that full expanded reporting has never completed.

> A subsequent reporting experiment processed the retained FY2003–FY2016 journal
> and ledger outputs to produce expanded histories, relationships and temporal
> summaries. The workload contained 261.5 million journal lines and 11.85 million
> ledger rows. A separate external sort established the reporting order, after
> which one sequential common-key traversal supplied the expanded reporting
> outputs and a separate spill reducer completed the analytical summaries. Sorting
> took 79.7 minutes, the reporting process 209.2 minutes, and the reduction process
> 24.9 seconds. The reporting process used approximately 1.30 GiB peak resident
> memory and completed a largest common-key group of 217.4 million input rows.
> Reconciliation reported no failures; subsequent checks covered all 273.4 million
> financial input rows. This single-host execution demonstrates the feasibility
> and measured processing requirements of the stated reporting workload. Its sort
> and materialization costs are part of that result; it does not identify a new
> minimum-cost configuration or establish a comparative architecture ranking.

Use this measurement table with the paragraph. Wall and CPU columns are process
measurements from /usr/bin/time and include sbt/JVM overhead. Internal engine
elapsed counters have a separate column; do not mix the two boundaries in a total.

| Stage | Process wall seconds | Process user CPU seconds | Process system CPU seconds | Maximum process RSS bytes | Internal elapsed seconds |
|---|---:|---:|---:|---:|---:|
| External reporting sort | 4,784.58 | 3,466.63 | 918.62 | 1,119,121,408 | Per-file values in sort manifest |
| Sequential expanded extract | 12,552.42 | 11,963.67 | 605.00 | 1,400,717,312 | 12,541.516 |
| Spill reduction | 24.87 | 40.41 | 3.65 | 1,041,752,064 | 13.774 |
| Connected-view checks | 95.45 | 109.28 | 2.73 | 988,360,704 | Not separately reported |

Sources: [sort process log](../../../data/test_evidence/report_sorted_v3_20261116/corpus_sort.log),
[extract log](../../../data/test_evidence/report_sorted_v3_20261116/extract.log),
[reduce log](../../../data/test_evidence/report_sorted_v3_20261116/reduce.log),
[checks log](../../../data/test_evidence/report_sorted_v3_20261116/corpus_checks.log).
The final output census is a separate post-engine validation activity recorded in
[its process log](../../../data/test_evidence/report_sorted_v3_20261116/corpus_summary.log).
Financial execution, assignment preparation, FY2012 admission, failed attempts,
archive copying and paper preparation are outside these four rows.

The external sort used a 100,000-row chunk and fan-in 16. Extract aggregate buffers
were configured for 50,000 keys and synthetic buffers for 10,000; feature capacity
was 250,000 with a measured peak of 46,404. The extract reported one financial
input traversal and zero order-index blocks/bytes. This one-pass statement applies
only after sorting: the complete topology includes sort materializations,
intermediate merge work and subsequent reduction. It is not a one-scan claim for
the entire financial/reporting pipeline or one independently measured cost per view.

The host was the recorded Intel laptop with 16 GiB RAM, two physical/four logical
CPUs, a 1-GiB JVM heap and ActiveProcessorCount=4. This is one exploratory
execution, not a clean replicated benchmark. Owner-reported sleep occurred during
the preceding FY2012 admission; it is not evidence of sleep during the later
full-corpus interval. Battery throttling was suspected but not established. A
sampler gap from 04:38:32 to 04:43:20 AZT prevents claiming continuous scratch
occupancy coverage. No peak scratch or cumulative I/O-write figure is established
by the retained size or the source-file byte counts.

## Suggested Paper 2 insertion

Place in the empirical foundation or limitations section, separately from existing
AIM model scores and the proposed broader inquiry experiment.

> The expanded reporting execution establishes a practical analytical foundation
> for further inquiry: reconciled movement and balance histories, relationship
> summaries and temporal and attribute views were produced across the fourteen-year
> financial population. The controls distinguish preservation and numerical
> consistency from economic interpretation. Of 273.4 million financial input rows,
> 52.5 million were attributed and 220.9 million remained unattributed. Synthetic
> reference histories and assigned templates exercise temporal processing but do
> not recover missing real identities or demonstrate real economic relationships.
> These results support the feasibility of preparing controlled analytical inputs;
> predictive improvement, reusable inquiry and novel discovery remain separate
> empirical questions.

The attribution counts refer to input rows, including both journal and ledger
records; they are not percentages of dollars, transactions or people. The run
recorded 65,716 reporting groups, not 65,716 independently verified real parties.

## Output and control evidence

| Output or control | Observed rows or result | Meaning for the revision |
|---|---:|---|
| Movement history | 11,835,781 | Analytical aggregation grain, not source-event count |
| Balance history | 11,835,781 | Report grain; do not equate with 11,850,542 physical ledger input rows |
| Reconciliation | 11,835,781; zero extract failures | Declared movement/balance comparisons passed |
| Relationships | 3,034,575 | Reported associations, not causal conclusions |
| Vendor temporal cuts | 71,119,226 | Output cardinality, not that many independent questions or discoveries |
| Vendor attribute totals | 12,985,882 | Attribute aggregation output |
| Vendor coverage | 65,716 | Reporting groups, including unresolved identities |
| History validation | 20 PASS | Declared reference-history oracles |
| Synthetic conservation | 2,102,912 PASS | Declared synthetic reporting conservation checks |
| Temporal summaries | 984 PASS | Connected summary checks |
| Attribute summaries | 4,396 PASS | Connected summary checks |
| Financial coverage | 273,380,488 PASS | Input-row coverage at the declared grain |

Exact bytes and zero-key output populations are in the
[final output census](../../../data/test_evidence/report_sorted_v3_20261116/report_profile.csv).
The connected checker and the financial/reporting implementation share this
repository; passing these checks is not an independent external accounting audit.
Row counts demonstrate delivered capability and volume, not scientific importance.

## Claim and evidence mapping

| Claim | Destination | Primary evidence | Supported status and boundary |
|---|---|---|---|
| Expanded reporting completed for the declared corpus | Paper 1 results | CORPUS_EVALUATED, EXIT_STATUS, corpus_checks.log | Verified for this execution; not a historical-baseline replicate |
| Large common-key group processed with bounded configured buffers | Paper 1 method/results | extract_metrics.csv and extract.log | Observed largest group and RSS; one workload, not a universal complexity bound |
| Additional outputs have separately identifiable sort/extract/reduce costs | Paper 1 cost discussion | Stage logs and sort_manifest.csv | Measured batch costs; no isolated per-view or cold/marginal comparison |
| Reconciled views can be produced for later inquiry | Paper 2 empirical foundation | report_profile.csv and corpus_checks.log | Delivered analytical products; no new model or discovery evaluation |
| Identity coverage constrains interpretation | Paper 2 limitations | corpus_checks.log and synthetic_vendor_coverage output | Input-row attribution only; no invented receipt identities |
| Synthetic histories preserve declared reporting totals | Paper 2 method/limits | synthetic conservation and history checks | Simulation consistency, not recovery of actual economic truth |

All filenames in the mapping refer to the
[permanent execution receipt](../../../data/test_evidence/report_sorted_v3_20261116/).
Retain the overall architecture status as partially verified: runtime capability
gaps, wider generator readiness and comparative cost claims are outside this
execution's successful functional/control evidence.

## Revision instructions

Update the shared evidence cutoff in the phase introduction and Papers 1/2 if
these observations are adopted. A suitable introduction sentence is: “A subsequent
full-corpus reporting experiment also demonstrated the production of reconciled
analytical histories and temporal views; evaluation of their use in broader AI
inquiry remains future work.”

Keep the earlier reporting guard failure and this later completion as distinct
observations. The new topology/input policy changes what was executed; success
here does not retroactively validate the failed workload or make its timings
comparable. Preserve the original measured financial results and AIM findings.
Do not change model scores, the cost-curve minimum, Control comparison conclusions,
or the new synthetic-generator status on the strength of this report.

Detailed output schemas and spill mechanics can remain supporting material or
inform Paper 5. Runs 2/3 are deferred under the owner's current priority: generate
and inspect a separately labeled synthetic population before starting Q2 work.
No new synthetic transaction corpus or external manuscript edit accompanies this
package.
