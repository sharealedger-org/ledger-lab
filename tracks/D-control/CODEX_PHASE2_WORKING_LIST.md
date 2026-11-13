# Codex temporary Phase 2 working list

Created 2026-11-12 (Arizona), at the owner's request. Maintained only by CKB-side Codex. Provisional proposals, not Q2-draft, accepted work, audit findings, or material for the control. Keep this file in this repository. See [reflections](CODEX_REFLECTIONS.md) for reasoning; promote agreed actions to the Track D section of `../../TODO.md` without duplicating their detailed tracking here.

## Objective

Help formulate useful Phase 2 tests of the owner's stated premise: consistent streaming financial data supports adaptation and stronger AI training signals while reducing total maintenance and use costs. Seek evidence that could contradict as well as support that premise.

## Candidate work, in priority order

| ID | Provisional action | Why it matters / cheapest next check | State |
|---|---|---|---|
| P2-01 | Map each paper claim to an observable outcome and a rejection criterion. | Read the exact supplied paper versions next; distinguish owner interpretation, paper wording, and testable scope. | Open; papers not freshly read in this session. |
| P2-02 | Establish a common accounting population and timed boundary. | Start with existing manifests and quarantine summaries; distinguish input acceptance from accounting transformations before proposing reruns. | Known open comparison prerequisite. |
| P2-03 | Trace one existing CKB capability from producer to downstream consumer and its metrics. | Resolve what already shares a stream, what rereads materialized data, and what requires sorting. Use a narrow executable check before proposing changes. | Static reclass/5v trace completed; [review](CKB_REVIEW_2026-11-12.md). Runtime falsifiers remain open. |
| P2-04 | Propose a new grouping/feature that can share an existing order, plus one requiring a new order. | Tests both the favorable case and a real streaming boundary. Count added scans, sorts, state, outputs, and full-workload cost. | Candidate family only; choose from actual available fields. |
| P2-05 | Propose one source correction or effective-dated rule/attribute change. | Tests maintenance, lineage, replay/backfill, and whether unaffected outputs remain stable. Define independent expected answers first. | Candidate family; inspect supported contracts before selecting. |
| P2-06 | Connect an added financial capability to a matched AI feature comparison. | Coordinate with completed Track B evidence. Hold population, labels, time splits, model, and evaluation budget constant; check leakage and balance-derived/time-only baselines. | Await current B evidence; no duplicate runs. |
| P2-07 | Define a service workload and accounting of total cost. | Specify horizon, update cadence, query mix, output/feature refresh, rebuilds and retained storage. Report resource quantities before any assumed price schedule. | Proposal; common I/O measure and cold/marginal refinements remain open. |
| P2-08 | Separate the architecture comparator from the fragmented-ledger comparator. | Source-preserving SQLite is not automatically a fragmented-ledger baseline. Identify what each comparison can establish; document substrate confounding. | Design distinction to resolve. |

## Process expansion candidates — 2026-11-12 21:43:40 AZT

Owner direction: broaden Phase 2 to financial processes, especially reclass and rule changes. The following are design candidates informed by selected passages of Papers 5 and 6 in the owner-supplied, Git-ignored `session-run.tmp`; they are not approved accounting rules or claims of implementation.

| ID | Candidate challenge | Correctness and cost questions |
|---|---|---|
| P2-09 | One classification change with Switch, Recast, and Reclass outputs. | Keep the economic source history unchanged; declare the perspective date. For Reclass, negate the eligible old position and open the new one with balanced, traceable generated SJEs consumed downstream. Choose debit/credit signs from the actual balance, not a fixed example. |
| P2-10 | Late-arriving change, late source event, then a second classification change. | Define effective date, arrival/knowledge date, same-day ordering, period-close policy, and original-versus-corrected output semantics. Check affected-period balances, unaffected history, reversibility, and no duplicate transfer on replay. Vary backdating distance and affected population. |
| P2-11 | Effective-dated mapping change followed by a calculation-logic change. | Distinguish prospective adoption, retrospective correction, and replay under the original rule. Record rule/version lineage, reversal or supersession, rounding, and actual backfill/rebuild work. Include an inactive instrument with an open balance so detection cannot rely only on new activity. |
| P2-12 | Feed corrected state into one additional process, such as allocation or booked revaluation. | Select from available data and existing contracts; do not assume procurement contains lending/risk inputs. Prove downstream consumption and independent expected answers; measure the combined workload rather than summing isolated demonstrations. |
| P2-13 | Observe whether successive requirements produce fragmentation. | Inventory independently maintained financial representations, repeated rules, and reconciliation dependencies after each change on both systems. Track correction propagation and retained bytes; do not equate extra tables or outputs with fragmentation. |
| P2-14 | Reproduce training features as known before and after the changes. | Exclude future-arriving facts/rules from historical prediction cutoffs. Distinguish temporal correctness from measured predictive improvement; reuse Track B evaluation design where applicable. |

**Cheapest next checks:** read the actual `arrangementReclass.scala` producer/consumer path and its audit entry; establish which stock balances and identities are available; then draft a small independently calculable scenario covering old/new classification, an inactive balance, and one late event. Declare any synthetic inputs explicitly. No implementation before the architecture gates.

## Incidental inconsistencies to revisit only when relevant

- **Focused CKB follow-up (22:01:26 AZT):** before extending reclass, test its current rewrite-then-post path on one instrument with explicit expected account balances; verify group identity, effective date, and rule lineage. Resolve no-activity triggers, cross-instrument generated records, and row-retention/spill semantics in the delivery-group contract. Inspect the `5v` completed-vendor set's growth separately from bounded View D aggregation. Detailed evidence and next checks are in the [CKB review](CKB_REVIEW_2026-11-12.md), with static findings entered in the audit register. No broad rewrite proposed.

- **Status freshness:** root status says B2 is on hold while the owner reports active Paper 2 evidence work. Let the Track B owner settle current status; do not overwrite concurrent work.
- **Pass language:** Track D already records “same pass” versus “one more bounded pass” language in cost-graph documentation. P2-03 should establish actual boundaries before any claim of free adaptation.
- **Replication language:** the baseline record distinguishes two paired-layout executions from the serial execution, while Track D uses a broader “single runs” caveat. Preserve the exact workload/layout identity when describing repeatability; this is not a claim of adequate replication.
- **Precision and population:** the control journal reports subcent revenue and parser recovery; current CKB baseline excludes revenue. Do not treat totals or rejection counts as comparable until definitions and source identities are reconciled (P2-02).

No item authorizes a broad audit, engine change, Python implementation, or control-side disclosure. Select a small test set with the owner and consult Bob on the CKB side before Q2-draft is finalized.
