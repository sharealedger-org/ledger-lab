# Accepted three-worker reporting result

Accepted at 2026-11-17T22:36:37Z after a separately documented precision correction.

All 21 declared outputs passed strict full-record multiset comparison, including duplicates. Exact input receipts, original frozen classes, financial/synthetic controls and all seven census profiles pass. Financial input: 286,277,734 rows in 28 partitions; 98,865 vendors.

| Reporting scope | Seconds |
|---|---:|
| Vendor range preparation | 740.76 |
| Three workers, extraction and local reduction wall time | 4837 |
| Final assembly/global collapse | 358.55 |
| Report-only precision correction | 161.66 |

Observed preparation + worker wave + combination + correction: 6097.97 seconds (101.63 minutes), versus 17448.18 seconds (290.80 minutes) baseline extract/reduce; engineering ratio 2.861x.

Checks, census, regression tests and full-record equivalence audits are separate verification costs, retained in their logs. Worker wall timing has one-second resolution. Individual JVM elapsed/RSS and worker spill counters remain separately recorded.

Precision exception: original e788d67 output used a shorter MathContext for 303 relationship shares. A bounded Scala report-only correction (two input scans plus output hash/count verification) matches the archived baseline exactly. The original report and initial census rejection remain preserved. This is not a claim that uncorrected e788d67 is fully equivalent. Isolated fix 9bc12a3 passes 20 reporting/precision tests; its future fused runtime has not been remeasured.

Limits: one observational engineering pair, designated mid-baseline, not replicated paper evidence. Three 2GiB workers, compressed intermediates, direct Java launch and extra range preparation differ from the serial profile; improvement cannot be attributed to one factor. Accepted financial preparation/original sorts were reused; no sort-reuse saving is claimed. Thermal/cache conditions are not isolated. Combiner accumulator spill-byte counters were not emitted by the frozen build; do not infer zero. Target GenevaERS capability gaps and allocation/basis feedback remain open. No upload or publication.
