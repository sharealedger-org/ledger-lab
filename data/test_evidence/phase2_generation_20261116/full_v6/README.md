# Full synthetic corpus v6: accepted execution

Completed 2026-11-16. Runtime package:
`session-run.tmp/phase2_full_v6`. One full generation and one uninterrupted actual
financial/report workflow; no full financial replay. The subsequent audit is
read-only validation, not another financial execution or model evaluation.

| Item | Actual result |
|---|---:|
| Recipe components / synthetic fiscal years | 10,000 / SY01–SY14 |
| Financial sources | 2,444,027 |
| Wholly generated / reassigned financial sources | 2,394,936 / 49,091 |
| Separate responsibility sources | 280,000 |
| Actual SJE rows | 4,888,054 |
| Reporting financial rows | 6,988,627 |
| Reporting vendor groups, including zero | 10,745 |
| Largest vendor group / peak vendor state keys | 1,024,364 / 3,560 |
| Reconciliation failures | 0 |
| Outcome records | 420,000 |
| Reused fictional suppliers | 5,744 |
| Suppliers in multiple years / AFO contexts | 2,966 / 2,670 |
| Reused original/public/actual classification matches | 49,091 |
| Package files hash-verified at each cutoff | 475 |

Generation process wall 832.23 s, maximum RSS 1,234,575,360 bytes. Reporting
extract engine elapsed 1,114.24 s; whole extract process wall 1,125.02 s, maximum
RSS 1,199,091,712 bytes. Audit process wall 88.64 s, maximum RSS 1,034,301,440
bytes. Source sorts retain at most 100,000 rows and 32 MiB payload, fan-in 16;
observed maximum buffer 33,554,423 bytes. Process/build overhead, source
preparation, validation and financial/report execution remain separate receipts.
These are engineering observations, not clean replicated cost points or target
GenevaERS runtime capability closure.

`financial_checks.csv` verifies source pairs, amounts, signs, dates, posting
periods, party, document and rule/version. Cutoff checks verify all annual
envelopes, source identities, censoring and immutable file hashes. Report history,
conservation and reconciliation checks passed. The full conservation data remain
in the runtime report; `conservation.sha256` records their hash rather than copying
the large table here. `post_audit/` records the additional original/public/actual
agency/fund/object comparison and supplier recurrence, with its input hashes and
captured Scala source. Two adversarial audit tests passed in addition to the
fifteen frozen source/identity tests. A local compile syntax error in the new
post-audit checker was corrected before tests/execution; it did not alter or
interrupt the already completed financial workflow.

The original VA corpus and historical results remain unchanged. The separate
Phase 1 identity registry upgrade preserves 65,716 literal keys, including three
special/unresolved keys; it is not a completed historical reprocessing or legal
party bridge. No Python, Q2 model evaluation, Control read, private manuscript
editing, archival or retained-corpus deletion was performed.

Public evidence dates are SY labels; raw financial and report contracts retain
legacy backing dates/FY codes, mapped by `calendar.csv`. All modeled actors are
fictional. Reused amounts/classifications and translated dates do not establish
real supplier identity. Proposals, cash plans and contractual funding schedules
do not silently post; debt valuation, portfolio/risk and forecasting engines remain
future work. Published source/control/inspection material is not a blind AIM set.

See [capacity assessment](CAPACITY.md) and the
[Paper 3 addition](../../../../docs/tds-evidence/phase2-generation-paper3/README.md).
