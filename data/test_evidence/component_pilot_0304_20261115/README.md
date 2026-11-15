# Component pilot FY2003–FY2004 evidence

Workload: `component-pilot-0304-v1`; contract:
`docs/COMPONENT_PILOT_0304_CONTRACT.md`. All generated financial amounts are
synthetic envelopes, not verified VA financial anchors.

- `baseline.log`: unchanged 32-test baseline passed before implementation.
- `generator_tests.log`: four new contract tests passed, including input reorder/
  sort chunk equivalence, missing/duplicate assignment, bad dates/duplicate events
  and declared sort fan-in overflow.
- Runtime root: `session-run.tmp/component_pilot_0304_20261115_v1/`.
- Command: `bash scripts/run_component_pilot.sh --out "$PWD/session-run.tmp/component_pilot_0304_20261115_v1"`.

The runtime test status and receipts are appended after actual completion.
This small fixture tests data flow and controls, not AI performance or corpus scale.
Employee records are responsibility evidence, not payroll accounting. Existing
APA TTL drops event IDs and legacy PO SJE rule fields are blank: explicit downstream
lineage remains partial even if amount/date/identity signature multisets reconcile.
The existing reference template's 2014/15 changes are not moved to 2003/04; this
pilot tests a new 2004 quiet-period master change and existing history adapter
compatibility, not newly generated 2003/04 hierarchy-change scenarios.
Fixture component labels and hand-authored event patterns are for functional
inspection; do not treat this package as an AI training/evaluation benchmark.

Next checkpoint: separate repository consolidation session before any larger
Phase 2 execution; retain the experiment evidence and dated reflections.

## Completed runtime result

`PILOT_COMPLETE` published. Actual financial pipeline ran 02:15:50–02:18:42 AZT;
independent checker completed 02:19:17 on 2026-11-15. Both FY03/FY04 completed TTL,
PO, merged sorting, posting, GL, permutation and legacy views; expanded 5v/5r then
completed. Financial compact archive: `../corpus_runs/run_20261115_021550/`.

| Financial family | FY2003 USD | FY2004 USD |
|---|---:|---:|
| Expenditure | 1,600 | 1,920 |
| Receipts | 2,400 | 2,880 |
| Purchase commitments | 400 | 480 |

36 generated events: 30 financial and six nonfinancial responsibility records.
Financial inputs produced 60 SJE rows in 30 independently balanced groups. Exact
pre-TTL source positions and downstream signature multisets passed. Expanded
reporting consumed 86 financial rows (60 movements plus 26 period-balance rows),
passed all 96 synthetic conservation checks and all 20 existing history oracles.
No missing transaction-date fallback occurred. Peak synthetic partial aggregation
keys: two; 64 recorded partial reduction passes; 1,894,817 partial spill bytes.
These deliberately tiny buffers exercise reduction behavior, not optimal cost.

The checker verified that c930001 spans two components, c930002 remains in the
background component, both masters change SMALL→LARGE at 2004-06-01 after their
last event, and only 35/36 evidence records are available at 2004-01-20. The delayed
2004-02-01 receipt evidence is excluded from that cutoff export. Source checksums
matched after execution; code snapshots are retained under the runtime root.

`pilot_checks.csv` keeps source/rule lineage PARTIAL, VA anchors NOT_VERIFIED and
scale/AI effectiveness NOT_TESTED. No employee financial engine, allocation,
reclass, target-runtime capability or new cost-curve point is established.
No original VA corpus files or prior financial outputs were modified.

## Lineage-v2 continuation after consolidation, 2026-11-15

Evidence: `lineage_v2/`. Workload `component-pilot-0304-lineage-v2`; generator and
all 50 generated input files are byte-identical to v1. Metadata is enriched in
existing APA/PO emission before their existing sorts, without another source scan,
new allocation rule, new ID allocator or financial calculation change.

Command:

```sh
bash scripts/run_component_pilot.sh \
  --out "$PWD/session-run.tmp/component_pilot_0304_20261115_lineage_v2" \
  --baselineRoot "$PWD/session-run.tmp/component_pilot_0304_20261115_v1"
```

Before edits, 45 relevant baseline tests passed. Final producer/generator/report
suite: 48 passed, including three new tests for exact lineage, duplicate/quoted
Unicode documents, rejected/blank source rows, root relocation and repeated
filenames/PO documents. Checker refinement for permutation cardinalities compiled
and then ran in the real pipeline.

Actual financial pipeline ran 03:16:04–03:18:33 AZT. `PILOT_COMPLETE` exists. The
checker verified 30 exact snapshot-scoped source locators, two SJE legs each,
original documents when present, and configured APA/built-in PO rule versions.
All prior financial, temporal, cutoff and reporting controls passed. SJE columns
outside declared metadata fields match v1; all four ledger/GL files are
byte-identical. Source hashes match after execution. Event provenance is in
retained SJE, not asserted to be preserved per event in aggregate balances.

Two separately copied negative fixtures preserve the amounts and prior reporting
controls but alter one retained source locator or rule-version value. Both checker
runs exit 1 at the corresponding lineage check; logs and exit receipts are in
`lineage_v2/`. This tests a stronger requirement than balance/signature equivalence.

Permutation financial keys and row counts match v1. Document/source distinct
counts independently match the bounded SJE rows. The richer metadata changes
reported capacities, as expected from the consumer's dimension definition:

| Year | v1 capacity | v2 capacity | v1 capacity_independent | v2 capacity_independent |
|---|---:|---:|---:|---:|
| 2003 | 34 | 36 | 214 | 16,536 |
| 2004 | 40 | 36 | 166 | 3,642 |

These figures are this report's observed outputs, not new cost points or evidence
of greater business/AI utility. Agree the role of audit identifiers in the query
capability definition before comparing curves. Original v1 partial-lineage
receipts remain historical evidence; v2 closes those checks only at fixture scale.
Verified VA financial anchors, corpus-scale assignment/sort admission, payroll,
allocation/reclass and model effectiveness remain unresolved. The unused older
assignment producer/launcher draft was not invoked.
