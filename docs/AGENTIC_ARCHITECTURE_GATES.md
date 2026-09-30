# Agentic Architecture Gates

This repository measures an accounting architecture, not merely whether a program produces plausible numbers. An agent may produce a passing fixture while still invalidating the experiment through a wrong process boundary, an incompatible record contract, an unmeasured materialization, or an unbounded implementation.

These gates are mandatory before claiming that a new engine, perspective, or cost point is implemented.

## 1. Declare the Workload Before Code

Write down the following before changing Scala:

- research question and capability being measured;
- logical owner: 01 transformation, 02 foundation, 03 instrument ledger, 04 engine, or 05 perspective;
- physical CKB process, input partitions A/B/C, sort order, and pass boundary;
- exact input and output schemas, including the existing Scala type or parser that consumes them;
- canonical state versus analytical output;
- materialization profiles and the behavior that changes between them;
- required controls, lineage identity, effective dates, and expected row counts;
- metrics that must be emitted and how they roll into the run manifest and cost surface.

A synthetic fixture is an input snapshot, not permission to bypass the layer contract. Raw synthetic events belong at a declared transformation boundary. An engine must consume the same SJE/Universal Journal contract as the measured engine.

## 2. Engine Contract Gates

An engine change is not accepted unless all of these are true:

1. **SJE in:** the engine consumes parsed SJE/Universal Journal or canonical ledger structures. It does not read ad hoc raw event CSVs directly from an engine package.
2. **SJE out:** generated records use the existing `Transaction`/SJE contract or a versioned schema with an explicit adapter. They can be parsed by the downstream consumer without a special test-only reader.
3. **Balanced groups:** every generated journal group balances independently, not only in a file-wide total.
4. **Lineage:** every generated row retains source event/instrument identity, rule set/version, effective date, and supersession/reversal relationship where applicable.
5. **Apply or declared view:** generated SJEs are applied through the CKB path or the output is explicitly classified as report-time analytical evidence. A file that is written but never consumed is an orphan, not a completed engine.
6. **CKB placement:** the engine runs at the declared ordered-stream/buffer boundary. Calling code from the same JVM after the CKB loop does not establish CKB execution.
7. **Memory contract:** input and intermediate state obey the declared bounded-memory or external-sort contract. `toList`, whole-file maps, and eager history loads require explicit approval and measurement.
8. **Materialization:** `derive-report`, `persist-sjes`, and other profiles produce observably different declared outputs and costs. A profile label that does not change behavior is invalid evidence.

## 3. Evidence and 99 Gates

- Engine, process, partition, sort, and reconciliation records use the schemas in `MEASUREMENT_AND_LOGGING.md`.
- Engine metrics reference the exact `process_id`, `partition_id`, and `run_id` that produced them.
- Controls are emitted by the financial path, not inferred later from a diagnostic summary.
- 99 reports implementation maturity, run activity, and evidence assessment separately.
- 99 never upgrades maturity because a helper produced rows or because a similarly named legacy step ran.
- A missing or unconsumed output is reported as missing evidence, not as support.

## 4. Required Validation Sequence

For each engine or perspective slice, run these checks in order:

1. compile the touched Scala project;
2. run the unchanged baseline fixture and compare its controls;
3. run the new named workload through the real orchestrator;
4. parse generated output with the existing downstream consumer or prove the declared adapter;
5. verify per-group balancing, lineage, effective-date, and reconciliation controls;
6. compare materialization profiles on rows, bytes, elapsed time, and physical passes;
7. inspect the canonical evidence bundle and 99 report;
8. perform an independent architecture review before calling the slice complete.

A successful fixture run is functional evidence only. It is not scale evidence, CKB evidence, or cost-curve evidence until the process, memory, materialization, and metric gates pass.

## 5. Cost-Curve Discipline

Cost points are comparable only when they use the same input snapshot, rules, output capability, engine substrate, and replication protocol. Keep engine compute, orchestration, sorting, storage, reconciliation, and interpretation overhead separately identifiable. Do not compare a synthetic helper or a different language/runtime to the Scala financial path. Do not claim C3/C4 equality, a minimum, or a theorem from one run.

## 6. Session Handoff Discipline

Every session that changes financial architecture must leave:

- the workload declaration and contract links;
- the exact command used for validation;
- controls and evidence paths inspected;
- known limitations and unimplemented layers;
- a clean distinction between verified behavior and planned behavior.

When an implementation fails these gates, discard or quarantine it before adding more features. Preserve the failure as a documented lesson, not as a partially trusted engine.
