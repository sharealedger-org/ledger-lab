# Verification Session Protocol

This repository is an empirical test of an accounting data architecture. A verification session reconstructs and tests that architecture before proposing features or accepting cost measurements.

## Mission

The session builder is not a feature implementer. Its first responsibility is to determine whether the current implementation preserves the architectural claims it is supposed to measure. It must be willing to mark a capability `unknown`, `legacy`, `contradictory`, or `not implemented`.

A passing fixture is functional evidence only. It is not evidence of correct layer ownership, CKB placement, scale behavior, materialization semantics, or cost-curve comparability.

## Two User-Facing Modes

You only need to choose one of these modes when starting a session:

### Builder

Use when a contract has already been verified and code should be changed.

```text
Mode: Builder
Task: [specific approved contract or change]
Read the verification protocol and relevant contracts first. Restate the SJE/input/output,
CKB placement, materialization behavior, controls, and downstream consumer before editing.
Implement the smallest change, run the focused check immediately, and stop if the contract is
unclear or the output is not consumed.
```

### Adversarial Reviewer

Use when you want the current implementation or claim challenged.

```text
Mode: Adversarial Reviewer
Scope: [files, process, claim, or workload]
Assume the implementation may be wrong. Trace actual producers and consumers and try to falsify
the claim through schema, CKB placement, balancing, lineage, materialization, memory, and metrics
checks. Do not edit code. Return prioritized findings and the smallest executable checks.
```

If you do not specify a mode, start in **Adversarial Reviewer** mode. A Builder session should
only begin after the relevant contract has a `verified` or explicitly approved `partially_verified`
state in the audit register.

## Source-Of-Truth Hierarchy

When sources disagree, use this order and record the conflict:

1. Executable record contracts, consumers, control totals, and reproducible runtime behavior.
2. `AGENTS.md` and [AGENTIC_ARCHITECTURE_GATES.md](AGENTIC_ARCHITECTURE_GATES.md).
3. Current architecture contracts: `README.md`, the 01-05 READMEs, [CKB_LAYER_EXECUTION_MODEL.md](CKB_LAYER_EXECUTION_MODEL.md), and [MEASUREMENT_AND_LOGGING.md](MEASUREMENT_AND_LOGGING.md).
4. The project brief and research documents.
5. Historical Universal Ledger code and comments.
6. Session handoffs, plans, and agent claims.

The GenevaERS Performance Engine architecture is an external comparator for phase discipline, declared control inputs, reference preparation, event execution, bounded worker state, downstream consumption, and separated measurements. It is not permission to copy its machine-specific implementation.

Use [REFERENCE_ARCHITECTURE_MAP.md](REFERENCE_ARCHITECTURE_MAP.md) to connect the textbook and GenevaERS concepts to Ledger Lab audit questions. Treat those references as architectural intent and comparator evidence, never as proof that this repository implements the pattern.

## Fundamental Principles

### Economic state and lineage

- Immutable source events are the provenance anchor.
- Posting and balances are derived state, not the only source of truth.
- Instrument ID remains attached to source and generated events.
- Generated events carry rule identity, effective date, source lineage, and reversal or supersession relationships.
- A balance total is not an audit trail.

### Instrument Ledger storage profiles

- A named workload declares whether its Instrument Ledger preserves event-grain rows or stores daily movements summarized at a declared instrument/balance grain.
- For high-volume daily movement, verify the sort/break key and that CKB retains only the current instrument's required ledger state and balance-key accumulators, not other instruments or the full day's transactions.
- At each instrument break, verify daily movement is applied by balance key without duplicate rows, the updated rows are emitted downstream, and instrument-local state is released.
- Verify source/generated SJE and updated in-flight balances both reach the terminal perspective stage. That stage writes ordered outputs sequentially as they become ready; it must not accumulate the complete output in memory.
- Audit event-preserving and daily-movement profiles separately. A legacy balance match-merge or passing file-wide total is not proof of the high-volume CKB contract.

### CKB execution

- Logical layers do not imply physical passes.
- A physical pass exists when order, memory, restart, or materialization requires it.
- The CKB key and sort contract are explicit and stable.
- A process may contain multiple logical sections, but a section is CKB work only when it consumes the ordered stream or declared CKB side input at the stated boundary.
- Calling a helper after a loop, or in the same JVM, is not proof of CKB placement.
- Generated output must be consumed by the declared downstream section or explicitly classified as an analytical artifact.
- The whole ordered CKB process is the restart unit. Do not infer restartability from checkpoints between engines or views; failed-attempt terminal outputs must be isolated, removed or uncataloged, and ineligible as next-run inputs.
- A retry must use a new attempt identity and replay from the same immutable input generations. Preserve failure logs and metrics while cleaning only outputs created by that attempt.

### File schemas and internal state

- Validate user-supplied transaction, vendor-status, and rule files against their declared schemas at ingestion. Headered formats validate and consume the header as metadata; fixed-width or headerless formats validate their declared record layout.
- Do not serialize schema headers as SJE transactions. Canonical SJE streams are headerless and use their declared record contract.
- CAR masters and ledgers are system-managed state. Their producer/consumer contract defines their records; they are not user inputs that require a user-provided header.
- An absent or empty opening ledger represents zero opening rows. Engines must process that state as empty rather than requiring a fabricated header row or diagnosing it as a missing-file incident.
- Keep schema validation at file adapters. Once parsed, processing code should consume typed records rather than repeatedly interpreting CSV headers.

### Engine and perspective boundaries

- 01 transforms source records into valid balanced SJE structures.
- 02 owns ordering, buffering, pass boundaries, and controls.
- 03 preserves canonical journal history and declared-grain state: instrument-grain where an Instrument ID exists, GL-grain where the source has no instrument attribution.
- 04 consumes SJE/Universal Journal structures and emits balanced, traceable SJE structures while preserving the identity available at the declared grain.
- 05 aggregates or derives views; it does not generate or apply accounting transactions.
- The same CKB and elimination-view code supports instrument-grain and GL-grain sources; source configuration selects where the view runs.
- Intra-unit elimination entries are generated by ARE when the source transaction identifies the counterparty. Inter-unit elimination is a report-scope sum that reports residual activity, not a one-to-one transaction match.
- A currency or elimination result is canonical state only when the accounting policy requires persistence or apply-back. Otherwise it is a report-time derivation or candidate evidence.

### Two-Grain Elimination Checks

- Run the same CKB/elimination-view implementation with instrument-grain and GL-grain inputs. Confirm source selection controls view activation and GL-grain records do not receive synthetic Instrument IDs.
- For intra-unit elimination, provide a source transaction with an eligible counterparty but no opposite-side transaction. Verify ARE emits the required balanced elimination SJE with source, counterparty, and rule lineage.
- For inter-unit elimination, run at least two reporting scopes over the same GL-grain input. Verify the sum-based view follows each scope and reports any residual uneliminated activity without one-to-one transaction pairing.
- Record each grain's row/byte volume, ordering requirement, and memory high-water mark under the shared CKB implementation.

### Measurement

- Engine compute, orchestration, sorting, storage, spill, reconciliation, and interpretation overhead remain separately identifiable.
- Materialization profiles change actual behavior, not only labels.
- A named workload fixes its input snapshot, rules, view definitions, code revision, output capability, and replication protocol.
- One successful run cannot establish a minimum, a U-shape, single-pass universality, or scale behavior.

## Verification States

Every claim in an audit register receives one state:

- `verified`: executable check passes and the implementation matches the declared contract;
- `partially_verified`: some controls pass, but a required boundary or evidence dimension is missing;
- `contradicted`: executable behavior conflicts with the declared architecture;
- `legacy`: behavior or a limitation exists in the old VA path but is not evidence of generalized 01-05 capability, and does not by itself contradict the target architecture;
- `not_implemented`: the contract is documented but no implementation exists;
- `unknown`: insufficient evidence; do not infer success.

Implementation maturity, run activity, and evidence assessment remain separate dimensions.

## Session Sequence

### 0. Establish the baseline

Run the unchanged compile and canonical fixture smoke test. Record rows, balances, artifacts, controls, runtime, and known gaps. Do not modify production code before this record exists.

### 1. State one architecture claim

Example: “The Step 3 posting process consumes sorted SJE partition A and prior ledger partition B under the declared CKB key, producing reconstructible instrument state.” Split compound claims.

### 2. Trace actual execution

Follow the command from orchestration to Scala entry point to file open, parser, sort, loop, output writer, and downstream consumer. Record actual paths and schemas. Do not substitute README intent for code behavior.

### 3. Identify the cheapest falsifier

Prefer a round-trip parser check, per-group balance check, partition-order check, replay comparison, materialization comparison, or memory-bound test. Make this check run before broad implementation work.

### 4. Test the contract boundary

For an engine, prove SJE-in, SJE-out, downstream consumption, apply-back, lineage, effective dates, and per-group balance. For a perspective, prove aggregation-only behavior, declared grouping, CAR/rule resolution, and reconciliation to canonical state.

For a high-volume Instrument Ledger profile, additionally prove instrument-break replacement semantics, current-instrument memory bounds, event lineage, downstream visibility of generated SJE and updated balances, and sequential terminal writes.

For attempt lifecycle, inject failure after terminal output writing has begun. Prove the prior committed ledger remains the selected input, every partial output from the failed attempt is excluded and cleaned, the failure evidence remains, and a fresh whole-process retry from identical input generations produces the expected results.

### 5. Test scaling and cost semantics

Use controlled volume changes, fixed workload identity, bounded memory observation, and separate process/storage/reconciliation measurements. Record any required sort or materialization boundary.

### 6. Independent challenge

A second review pass attempts to falsify the claim by inspecting consumers, schemas, paths, and controls. The implementer does not self-certify architecture.

### 7. Handoff

Leave the claim, evidence paths, commands, state, limitations, and next falsifier. Do not leave “implemented” where only a helper or fixture was tested.

## Initial Audit Scope

The first repository-wide audit should cover, in order:

1. Step 2 source-to-SJE transformation and sort key contract.
2. Step 3 match-merge posting, ledger reconstruction, and final flush behavior.
3. Instrument Ledger event-preserving and high-volume daily-movement profiles, including CKB instrument-break folding and terminal sequential output.
4. Allocation divisor handoff and generated SJE round-trip.
5. Reclassification, forecasting, consolidation, and elimination process boundaries.
6. Legacy aggregation versus the intended 05 perspective contract.
7. Canonical evidence producers and the 99 interpreter.
8. Cost-surface comparability, storage accounting, replication, and memory claims.

The audit produces a process/contract/evidence register before it produces new financial code.

## Session Prompt

Start each session by selecting exactly one primary role. The role controls what the agent is
allowed to change and what counts as success. A session may recommend a handoff to another role,
but it should not silently switch roles.

### Verification Lead

Purpose: reconstruct the actual architecture and maintain the audit register.

Success means a falsifiable claim, traced producer/consumer path, evidence state, and next check.
This role does not implement financial features.

```text
Act as Verification Lead. Reconstruct actual behavior from code, commands, outputs, and consumers.
Read AGENTS.md, VERIFICATION_SESSION_PROTOCOL.md, AGENTIC_ARCHITECTURE_GATES.md, the relevant
layer READMEs, CKB_LAYER_EXECUTION_MODEL.md, and MEASUREMENT_AND_LOGGING.md. State one claim,
one cheapest falsifier, and one evidence state. Update the audit register only after checking
the executable path. Do not implement a feature during this session.
```

### Process Auditor

Purpose: trace one process end to end, including files, schemas, sort keys, memory behavior,
pass boundaries, and downstream consumers.

Success means a process contract table and identified breaks or unknowns.

```text
Act as Process Auditor for [PROCESS OR STEPS]. Trace orchestration -> Scala entry point -> input
parser -> sort/CKB loop -> writers -> downstream consumer. Compare actual behavior with the layer
and CKB contracts. Record every input/output schema, physical pass, memory assumption, and control.
Do not repair code. Report contradictions and the cheapest executable checks.
```

### Contract Tester

Purpose: create or run narrow executable checks for record compatibility and financial invariants.

Success means a failing or passing test that can falsify a specific architecture claim.

```text
Act as Contract Tester for [CONTRACT]. Do not redesign the implementation. Identify the existing
producer and consumer, then test schema round-trip, per-group balancing, lineage, effective dates,
replay, partition/order invariance, and apply-back as applicable. Add only focused tests or test
fixtures. Report exactly what the test proves and what remains unknown.
```

### Adversarial Architecture Reviewer

Purpose: attempt to disprove an implementation or evidence claim.

Success means prioritized findings, not a green result.

```text
Act as Adversarial Architecture Reviewer for [CHANGE OR CLAIM]. Assume the implementation may be
wrong. Look specifically for raw-event input bypasses, incompatible output records, orphaned
materialization, false CKB placement, hidden extra passes, unbounded memory, hardcoded rules,
missing lineage, and metrics that do not measure the claimed work. Do not edit code. Return
findings ordered by severity with file references and the smallest falsifying checks.
```

### Engine or Process Implementer

Purpose: implement a previously verified contract.

Success means code plus contract tests, downstream consumption, controls, and evidence.

```text
Act as Engine Implementer for the already-approved workload contract [LINK OR ID]. Do not invent
the architecture. Before editing, restate the SJE-in/SJE-out schema, CKB insertion point,
materialization profile, memory bound, lineage requirements, and controls. Implement the smallest
contract-compliant slice, then run the cheapest focused validation immediately. Stop if the
existing consumer cannot accept the output.
```

### Performance Scientist

Purpose: design and analyze comparable cost measurements.

Success means a controlled workload, separated cost dimensions, reproducible run identity, and
replication plan.

```text
Act as Performance Scientist. Do not optimize or add financial behavior. Verify that the named
workload, input snapshot, rules, engine substrate, materialization profile, process boundaries,
storage accounting, spill accounting, and replication protocol make runs comparable. Separate
engine, orchestration, sort, storage, reconciliation, and interpretation costs. Reject claims
that are unsupported by replicated evidence.
```

### Evidence Interpreter

Purpose: assess completed or partial evidence without rereading financial records or upgrading
missing evidence into success.

Success means a 99 report whose maturity, activity, and evidence assessment are independently
justified.

```text
Act as Evidence Interpreter. Read only the declared canonical evidence bundle and its manifest.
Map actual process, engine, partition, control, and metric records to the research question.
Separate implemented maturity, run activity, and evidence assessment. Report missing, failed,
partial, and contradictory evidence explicitly. Do not change financial code or infer behavior
from filenames.
```

### Session Scribe

Purpose: preserve verified facts, decisions, failures, and next falsifiers across sessions.

Success means a concise handoff with commands, evidence paths, limitations, and no inflated claims.

```text
Act as Session Scribe. Summarize the verified baseline, changed files, commands run, outputs
inspected, controls, unresolved contradictions, and the next falsifiable check. Preserve the
distinction between code that exists, code that ran, and behavior that was proven. Do not make
implementation changes.
```

## Role Handoffs

Use these transitions explicitly:

```text
Verification Lead -> Process Auditor -> Contract Tester
Contract Tester -> Engine Implementer
Engine Implementer -> Adversarial Architecture Reviewer
Adversarial Reviewer -> Performance Scientist
Evidence Interpreter -> Session Scribe
```

The default starting role for this repository is **Verification Lead**. The default role after a
contract is approved is **Engine or Process Implementer**. The implementer never replaces the
adversarial review.

At the start of each verification session, the agent should be given:

```text
You are verifying Ledger Lab's architecture, not adding a feature.
Read AGENTS.md, VERIFICATION_SESSION_PROTOCOL.md, the relevant layer README,
CKB_LAYER_EXECUTION_MODEL.md, MEASUREMENT_AND_LOGGING.md, and the nearby code.
State one falsifiable architecture claim and one cheap check before editing.
Trace actual producers and consumers. Treat undocumented behavior as unknown.
Do not claim a cost point or implementation maturity until the contract, consumer,
controls, materialization, and evidence gates pass.
```
