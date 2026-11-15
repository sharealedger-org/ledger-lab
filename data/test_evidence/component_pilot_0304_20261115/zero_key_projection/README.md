# Remaining zero-ID group: measured report probes and conditional projection

2026-11-15. Owner requested a practicality check before full-corpus admission.
No original corpus rekeying, full-corpus run or cloud copy. Active roots stayed
local. Runtime: `session-run.tmp/report_zero_key_20261115/`; PROBE_COMPLETE exists.

## Actual measured work

Command: `bash scripts/run_reporting_hot_key_probe.sh --out "$PWD/session-run.tmp/report_zero_key_20261115"`.
Scala supplies balanced 39-column SJE input snapshots and invokes existing post;
actual unchanged expanded 5v/5r then runs in separate JVMs through run_reporting.sh.
These are report-mechanics fixtures, not TTL/generation or new cost-point evidence.
Each contains one original zero ID only, no assigned template or fabricated vendor.
All reporting reconciliation, synthetic conservation, history oracles and input/
coverage counts passed. Full logs and measurements are in each case subdirectory.

| Shape | Movement rows | Posted rows | Extract + reduce seconds | Retained report bytes | Cumulative spill writes |
|---|---:|---:|---:|---:|---:|
| Repetitive | 500,000 | 64 | 26.349 | 241,863 | 256,697 |
| Matched posting-key ratio | 2,000,000 | 40,384 | 109.314 | 67,442,536 | 201,295,353 |
| Distinct-heavy stress | 500,000 | 250,384 | 130.163 | 408,092,435 | 1,389,188,934 |

Intel x86_64 host, 17,179,869,184 bytes RAM, two physical/four logical CPUs;
Temurin Java 21.0.8. JVM set to 1 GiB heap/four active processors. Base aggregate
capacity 50,000; synthetic capacity 10,000; fan-in 16; spillRows 50,000. Probes had
900-second/20-GiB engineering limits and no limit trips. OS wall/CPU/RSS and sampled
root occupancy are in os_metrics.csv. Extract RSS 1,101,742,080–1,143,222,272 bytes.
Sampled occupancy includes fixture inputs, is not an exact transient peak and can
miss short peaks. Cumulative spill writes are not simultaneous scratch occupancy.

## Population assumptions

Audit inputs: identity_audit_20261114 canonical/summary and parser_summary. Historical
zero totals are 181,736,476 movement + 3,559,680 posted = 185,296,156. Largest-group
CSV excludes a separate nine-zero PO sentinel's 58 rows; canonical summary includes
it. Current synthetic enrichment does not itself recover observed identities.

The parser audit still has 87,815,525 unattributed expense source records. Keeping
all 17,699,374 audited revenue source records unattributed at agency grain gives
211,029,798 movement rows; assuming either old zero posted rows or all old posted
rows gives 214,589,478–222,170,024 financial inputs. These are scenarios, not a fresh
count or a bound after reference/parser changes. More validly parsed rows, changed
reference keys and synthetic additions can alter both counts and key cardinality.

Projection.csv also retains an all-old-accepted-APA sensitivity, and an extreme
one-posted-row-per-movement case. The latter is conditional on the old accepted
population, unchanged PO contribution, no openings and no new source records; it
is not a full-future-corpus ceiling. Do not call all old posted rows an upper bound
on changed reference-key populations.

## Conditional results and admission decision

Matched-density extrapolation: historical zero group 2.758 hours; 215–222M policy
scenarios 3.194–3.307 hours. Matched report retention scales to roughly 6.6–6.8 GiB
and cumulative spill writes 19.7–20.4 GiB for those policy scenarios. A 2x merge-
depth sensitivity gives 6.387–6.613 hours; this multiplier is an explicit planning
sensitivity, not calibrated uncertainty or a guaranteed upper bound.

Applying distinct-heavy per-row rates instead gives 10.340–10.705 hours and
108.7–112.5 GiB retained report for those counts, with 370–383 GiB cumulative spill
writes. This deliberately has far more keys per input row than the historical
ratio; it is a stress scenario, not a likely forecast or statistical interval.
The fact that every small probe terminates does not establish acceptable full-run
time. The zero component alone may occupy an overnight window.

These figures cover zero-ID reporting only: not generation, sorting/posting,
other vendor groups (historically about 85M more report rows), archive copying or
the entire experiment. Reporting makes two source passes, including order-index
preparation; projections include those engine timings. Inputs here are shorter
and references smaller than some real/new-provenance records; full multi-year
files, cache/thermal/disk contention, extra merge levels and revised output
capability can change rates. Refresh source counts and actual key/output diversity
before admitting a full run. Do not multiply the tiny two-key pilot buffer costs
or pretend bounded heap removes the I/O/output cost.

Next gate: profile one representative current real year with current parsing,
reference preparation and source-policy decisions; measure zero movements,
posted-key counts, byte widths and view output cardinalities. Compare those with
the matched/stress profiles, live scratch and the practical run window. If too
costly, evaluate a declared GL-grain/common-key or view-materialization profile
with controls, after distinguishing true GL-grain records from unresolved parties.
Do not split zeros into fake vendors or drop required output to obtain a pass.

Measured producer/inspection source hash is saved in measured_source_sha256.txt;
its runtime snapshot predates a projection-only label/extreme-scenario refinement.
Final projection source hash is separate. Preparation/report algorithms were not
changed between probes. The final projection was recompiled/re-run after all
measurement jobs finished. This distinction preserves reproducibility without
claiming altered analytical labels were the source used to time the producer.
