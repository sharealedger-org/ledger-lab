# Verification Audit Register

Status as of 2026-09-29. This is an evidence register, not a declaration that the repository is production-correct.

| Area | Claim under test | Current state | Evidence | Open falsifier or gap |
|---|---|---|---|---|
| Step 2 transformation | Raw VA input becomes 39-field `Transaction`-compatible SJE rows with instrument identity and balanced debit/offset pairs. | `partially_verified` | Audit run `audit-baseline` produced 1,520 rows; 0 bad-arity rows; 0 unbalanced journal groups; source total 0.00. | Test every record format and accounting-rule branch independently; verify vendor misses and offset rows; test field-level schema rather than relying on comments. |
| Step 2 external sort | Sorted output follows the exact Step 3 posting key and uses bounded spill/merge behavior. | `partially_verified` | Audit run found 0 sort-key violations against the declared 15-field concatenated key; fixture completes with one spill. | Prove comparator equivalence against Step 3 for adversarial keys, duplicate keys, multiple spills, numeric sort fields, and large-volume memory bounds. |
| Step 3 posting | Sorted SJE plus prior ledger state reconstructs instrument-level ledger balances and preserves conservation. | `partially_verified` | Existing fixture produced 59 ledger rows and amount sum 0.00; Step 3 parses the 39-field transaction contract. | Run prior-ledger replay, duplicate-key, empty-input, multi-period, and final-flush tests; verify every ledger row can be reconstructed from supporting SJEs. |
| Step 3 process metrics | Structured process metrics report actual rows, bytes, elapsed time, and resource use. | `contradicted` on macOS | Audit run log reported `records_read=0` although Step 3 printed `Journal Records Read: 1520`; Step 2 reported `records_read=?`; `outPath_bytes` and final storage were `?`. Canonical Step 2 input row count was 761 although the staged raw file contains 760 data rows, indicating header-inclusive counting. | Replace text scraping with Scala-emitted metrics or portable structured events; make header/data-row semantics explicit; rerun and compare every metric to artifact counts. |
| Allocation | Prior-period divisor input produces balanced allocation SJEs, applies them, and regenerates the next-period divisor partition. | `partially_verified` | Existing active allocation smoke profile reports generated rows, rounding residual, apply-back, and next-period divisor groups. | Independently verify generated SJE schema, per-group balance, source lineage, rule/effective period, and comparable persist/derive profiles. |
| Reclassification, forecasting, consolidation | Legacy steps implement the declared CKB contracts and preserve lineage/control behavior. | `unknown` | Documentation and source modules exist. | Trace actual producers/consumers and run focused round-trip controls; do not infer generalized 04/05 maturity from legacy step names. |
| Generalized 04 engines | Engines consume SJE/Universal Journal structures and emit consumable balanced SJE structures. | `not_implemented` | `04-engines/calculation_engines/README.md` defines the boundary; no accepted implementation remains after the rejected synthetic attempt. | Define a machine-readable workload and SJE-in/SJE-out round-trip test before writing engine code. |
| Generalized 05 perspectives | Perspectives aggregate canonical ledger/engine outputs without generating or applying accounting entries. | `not_implemented` | `05-perspectives` contains design scaffolding and schemas/readme contracts. | Build a bounded view contract and prove reconciliation to canonical state before adding currency or elimination reporting. |
| Canonical evidence | Run evidence is emitted with stable run/process/partition identities and interpreted without overstating maturity. | `partially_verified` | Baseline run emits an evidence bundle and 99 report. | Repair row/byte metric production, validate all schema columns, test failed/partial/no-op runs, and link every conclusion to a source record. |
| Cost curves | C1-C4 points are comparable measurements of the same Scala substrate with separated compute, storage, sort, reconciliation, and replication costs. | `not_verified` | Cost surface and workload labels exist. | First make process metrics reliable, define named workloads/materialization profiles, run replicated points, and audit physical pass/materialization assumptions. |

## Immediate Order

1. Repair the process/storage metric producer and add executable schema checks.
2. Complete Step 2/3 contract tests and replay controls.
3. Audit allocation round-trip behavior.
4. Audit legacy reclassification, forecasting, consolidation, and aggregation without calling them generalized layers.
5. Define the real SJE-in/SJE-out engine workload.
6. Only then implement currency/elimination engines and reporting perspectives.

## Additional Draft-Paper Gates

The remaining private drafts add these checks to the audit scope:

- classify any hierarchy-change implementation as Switch, Recast, or Reclass, including event-date versus arrival-date behavior;
- validate effective-dated reference intervals, bitemporal assertions, hierarchy acyclicity, leaf coverage, and promotion pre-flight controls;
- verify manual adjustments are instrument-anchored or explicitly suspense-anchored, and that technical/accounting rejects are quarantined and recyclable;
- verify shadow-ledger certification against legacy outputs before any master-file sunsetting claim;
- distinguish shared inter-enterprise event state from private party accounting state if shared-ledger work is added.
