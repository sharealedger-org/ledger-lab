# Track D / CCP: Cost and capability measurements

Updated 2026-11-15. Former Track B1 is a workstream inside the
[primary experiment](../STATUS.md). This is a navigation change, not track closure,
a new measurement or a claim that a cost minimum has been demonstrated.

## Evidence to use

- Original expense/PO baseline: tag `companion-measured-baseline-20261111`
  (`c3667e4`); [baseline record](../../../docs/COMPANION_BASELINE_RECORD_2026-11-12.md).
  The 74 named final files and 14 merged-SJE hashes matched between the two
  compared Monthly executions. Earlier layouts are not isolated CCP replicates.
- Expanded expenditure/revenue/PO runs: evidence checkpoint `17bd5e7`;
  [queue receipt](../../../data/test_evidence/intel_preflight_20261113/README.md).
  All three validated; 46 compared final-product hashes matched. This is one
  workload at Monthly, not three different cost points. Combined TTL totals
  have legacy expenditure labels; verify source families independently.
- Separate expanded reporting failed on the zero-vendor row guard:
  [failure](../../../data/test_evidence/reporting_corpus_20261114/README.md).
  Later [identity-recovery fixture](../../../data/test_evidence/identity_recovery_20261114/README.md)
  and [bounded component pilot](../../../data/test_evidence/component_pilot_0304_20261115/README.md)
  exercise revised handling, not a successful corpus reporting run.
- The [Mix design](../../../docs/COST_GRAPH_DESIGN.md) combines measured constants
  with assumed rates/query volumes. Annual sizing and dense permutation capacity
  are not measured implementations of alternative systems. CCP4/CCP5 have fixture
  evidence, not comparable corpus cost points. GERS target-runtime gaps stay open.

## Next work and decisions

Follow the primary experiment's versioned synthetic-design and admission sequence.
Before new cost claims, select comparable workloads/retained outputs and a reference
layout; separate engine, sorts, orchestration, controls and storage. Record actual
physical passes and per-process peak memory. Do not derive a pass count from a
logical layer or interpret wall-time proxies as CPU.

Open: CCP replicates, annual-granularity measurement, reconciled Control comparison,
common I/O measure, independently sourced rates and model sensitivity, and exact
retained-output scope. Broader allocation/reclass work belongs to Phase 3; it is
not a prerequisite added by this page. Larger execution is outside this session.

Open tasks remain in [TODO.md](../../../TODO.md). Use the
[prior B1 status](../../../docs/archive/collaborator_review_2026-11-15/tracks/B1-cost/STATUS.md)
for historical layout/timing details and superseded next actions, not current queue
instructions. [Measurement contract](../../../docs/MEASUREMENT_AND_LOGGING.md)
and relevant audit entries govern any implementation or measurement change.
