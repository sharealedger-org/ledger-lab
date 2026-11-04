# Verification Session Protocol

This repository is an empirical test of an accounting data architecture. A verification session reconstructs and tests that architecture before proposing features or accepting cost measurements.

## Mission

The session agent is not a feature implementer. Its first responsibility is to determine whether the current implementation preserves the architectural claims it is supposed to measure. It must be willing to mark a capability `unknown`, `legacy`, `contradicted`, or `not_implemented`.

A passing fixture is functional evidence only. It is not evidence of correct layer ownership, CKB placement, scale behavior, materialization semantics, or cost-curve comparability.

---

## When to Run a Verification Session

A verification session **must** precede any Builder/Implementer session when:

- a new capability, engine, or perspective is being added;
- the workload contract, sort key, memory bound, or materialization profile of an existing step is changing;
- a claim in the audit register is being upgraded from `partially_verified` to `verified`;
- a new CCP or AIM run is being prepared.

A verification session is **not** required for:

- pure housekeeping (dependency bumps, dead-code removal, documentation rewording, comment cleanup);
- test-fixture additions that do not change production Scala logic;
- documentation that explicitly records `design_only` or `not_implemented` state.

If you are uncertain which category applies, start in **Adversarial Reviewer** mode and stop if no code changes are needed.

---

## What Changed Since the Original Protocol Was Written

The repository has accumulated significant new work since the last Adversarial Review pass. Before starting a new verification session, a session agent should be aware of the following structural changes:

### New steps added (not yet in the audit register)

| Step | File | What it does | Audit state |
|------|------|-------------|-------------|
| Step 20 — GLPost | `ledger/GLPost.scala` | Reads `SortedJEFY*.csv`, accumulates account-keyed balances (instrument identity discarded), writes `GL20YY.csv`. Emits `RESULT step=20` and `RESOURCE step=20`. | `not_audited` — functional tests pass; no contract audit, no CKB placement claim, no memory-bound declaration. |
| Step 21 — PermutationGenerator | `ledger/PermutationGenerator.scala` | Reads same `SortedJEFY*.csv`, accumulates `(account_key, vendor_id)` permutation counts, writes `PERM_TBL20YY.csv`. | `not_audited` — same. |
| process_metrics RESULT/RESOURCE emission | `standardizeAndSort.scala`, `post.scala` | Steps 2 and 3 now emit `RESULT` and `RESOURCE` structured log lines; `write_canonical_evidence.sh` grep extracts them into `process_metrics.csv`. | `partially_verified` — emission confirmed by fixture test; CPU fields, spill evidence, and manifest identity hashes remain unverified. |
| `transIPID` fix | `transStandardize.scala` | Legacy `transStandardize` path (not the live Step 2 path): PO path now populates `transIPID = rec.poVendorID`; payment path does VendorMaster name-lookup. | `partially_verified` — 21/21 fixture tests pass; no runtime fixture run with `--with-po-fixture`; `transIPID` non-zero on real data not confirmed. |
| `POSortedJE.sortspec` | `02-foundation/sort_specs/` | Declares PO SJE sort key `(transIPID, transFiscalPeriod, transTransDate)`. | `design_only` — file exists, not yet wired to any running step. |

### Evidence infrastructure: what `write_canonical_evidence.sh` actually emits

`write_canonical_evidence.sh` is the only script that writes canonical evidence. It emits:

| File | Populated? | What is missing |
|------|-----------|-----------------|
| `run_manifest.csv` | Partially | `java_version`, `engine_version`, `configuration_hash`, `rules_hash`, `viewspec_hash`, `input_snapshot_id`, `replicate_number` all written as empty strings. `scala_version` is hardcoded to `2.12.18` (now stale — actual version is `2.12.20`). |
| `process_metrics.csv` | Partially | `cpu_user_seconds`, `cpu_system_seconds`, `cpu_total_seconds`, `peak_jvm_heap_bytes`, `pageins`, `context_switches`, `spill_bytes` all written as empty. `wall_seconds` is inferred from `elapsed_s` in the log. Process start/end times are both set to the run timestamp, not step-level timestamps. |
| `partition_catalog.csv` | Partially | `first_key`, `last_key`, `min_period`, `max_period` all empty. `checksum` computed only for files that exist at write time; skips Step 3 input when Step 12 is active. |
| `reconciliation_results.csv` | Partially | SJE and ledger balance controls present. Missing: per-group balance, ledger reconstruction from supporting SJEs, generated-event lineage coverage, shared-core equality, perspective totals vs. ledger totals, rounding residuals. |
| `sort_events.csv` | Stub | Sort key written as a literal string (not from the `.sortspec` file). `spill_file_count`, `peak_memory_bytes`, `chunk_size` are empty. `sort_reason` and `input_scope` absent. |
| `sje_engine_metrics.csv` | Written only when Step 12 active | `source_sje_rows` hardcoded to `0`. `lineage_coverage` hardcoded to `1.0`. |
| Steps 20, 21 | Not handled | `write_canonical_evidence.sh` has no `append_process` case for steps 20 or 21. Their `RESULT`/`RESOURCE` lines are not extracted. |

These gaps mean **no run currently produces a complete evidence bundle** as defined by `MEASUREMENT_AND_LOGGING.md`. Cost-curve comparisons based on existing evidence are limited to `input_rows`, `output_rows`, `elapsed_s`, and `peak_rss_bytes` for Steps 2, 3, 6, and 12.

---

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

---

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

---

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

---

## Verification States

Every claim in an audit register receives one state:

- `verified`: executable check passes and the implementation matches the declared contract;
- `partially_verified`: some controls pass, but a required boundary or evidence dimension is missing;
- `contradicted`: executable behavior conflicts with the declared architecture;
- `legacy`: behavior or a limitation exists in the old VA path but is not evidence of generalized 01-05 capability, and does not by itself contradict the target architecture;
- `not_implemented`: the contract is documented but no implementation exists;
- `not_audited`: code exists and may run, but no verification session has traced producers/consumers, schema, CKB placement, memory contract, and controls for this capability;
- `design_only`: a specification or sort spec file exists but is not wired to any running step;
- `unknown`: insufficient evidence; do not infer success.

`not_audited` is the correct initial state for any new code added without a prior verification session. It differs from `not_implemented` (no code) and `unknown` (some evidence exists but is inconclusive). A Builder session that adds code without a verification session must set the new capability to `not_audited` in the register.

---

## Session Sequence

### 0. Establish the baseline

Run the unchanged compile and canonical fixture smoke test. Record rows, balances, artifacts, controls, runtime, and known gaps. Do not modify production code before this record exists.

### 1. State one architecture claim

Example: "The Step 3 posting process consumes sorted SJE partition A and prior ledger partition B under the declared CKB key, producing reconstructible instrument state." Split compound claims.

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

Leave the claim, evidence paths, commands, state, limitations, and next falsifier. Do not leave "implemented" where only a helper or fixture was tested.

---

## Priority Order for Next Verification Work

The following is the ordered queue based on what blocks the research programme. Work through it top-to-bottom; do not skip ahead to build a higher CCP until the items it depends on are `verified` or explicitly approved `partially_verified`.

### Priority 1 — Evidence infrastructure gaps (blocks all CCPs)

**What:** `write_canonical_evidence.sh` emits empty fields for the columns that make runs comparable and reproducible: `scala_version` is stale (`2.12.18` → should be `2.12.20`), `replicate_number` is hardcoded to `1`, `cpu_*` and `peak_jvm_heap_bytes` are empty, `sort_events` are stubs, Steps 20 and 21 have no `append_process` case.

**Cheapest falsifier:** Run `run_c1_smoke_test.sh`, read the resulting `process_metrics.csv`, and check whether `cpu_user_seconds`, `peak_jvm_heap_bytes`, and `spill_bytes` are non-empty and whether Steps 20 and 21 appear.

**Gate:** No CCP run counts as a replicate until process_metrics rows for all active steps have non-empty `wall_seconds`, `peak_rss_bytes`, and at minimum a recorded `status`. CPU and spill fields are desirable but may be deferred with explicit documentation.

### Priority 2 — Steps 2 and 3 contract (blocks CCP2)

**What:** The live pipeline path (`standardizeAndSort.scala` → `post.scala`) is `partially_verified`. The cheapest unrun falsifier is prior-ledger replay: run Step 3 with a non-empty opening `LDGR` file, verify each output ledger row can be reconstructed from its supporting SJEs.

**Additional unrun checks:**
- Per-group balance on the sorted SJE output (not just file total).
- Sort-key equivalence between the Step 2 comparator and the Step 3 consumer for adversarial keys (duplicate keys, numeric fields, multi-field tie-breaks).
- Empty-input behavior: Step 2 with zero source rows, Step 3 with zero SJE rows and a non-empty opening ledger.
- Vendor-miss handling: source rows whose VENDORID has no VendorMaster entry.

**Gate:** CCP2's first replicate requires `partially_verified` with explicit approval from the project owner for the remaining gaps.

### Priority 3 — Steps 20 and 21 contract (blocks CCP1)

**What:** GLPost and PermutationGenerator are `not_audited`. They run, produce output, and have 7 unit tests.

**2026-11-04 adversarial audit findings (see audit register §"2026-11-04 Adversarial audit"):

- ✅ Column indices verified against `standardizeAndSort.scala` schema — consistent across three sources.
- ✅ Debit-only accumulation semantics correct; 7/7 unit tests pass.
- ✅ **FINDING 1 (RESOLVED):** CCP1 sort-key and GL architecture semantics clarified. In a real legacy GL, vendor identity is not an input to the posting step. `GLPost` correctly ignores `transIPID`; shared SortedJE partition introduces a known measurement conservatism (CCP1 sort cost ≥ true legacy GL sort cost), making the CCP1→CCP2 delta a lower bound. `PermutationGenerator` measures externalised upstream materialisation cost, not a GL step. `TEST_DESIGN_PAPER_1.md` updated; framing confirmed by project owner.
- ✅ **FINDING 2 (FIXED):** Output filenames standardised to two-digit year convention: `GL20YY.csv`, `PERM_TBL20YY.csv`.
- ✅ **FINDING 3 (RESOLVED):** Memory bound not applicable to Steps 20/21 — both are measurement instruments / GL replicators, not CKB engines. Full-corpus accumulation is architecturally correct. `peak_rss_bytes` metric limitation (JVM heap, not OS RSS) noted in documentation.
- ✅ **FINDING 4 (FIXED):** `write_canonical_evidence.sh` `scala_version` updated to `2.12.20`.
- ✅ **FINDING 5 (DEFERRED):** `GL20YY.csv` and `PERM_TBL20YY.csv` are correct terminal CCP1 outputs; AIM1 consumer is a future gate, not a current blocker.

**Remaining checks before CCP1 gate:**
1. ~~Fix `scala_version` hardcode~~ **Done.**
2. ~~Rename outputs and add `append_process` cases for steps 20/21~~ **Done.**
3. ~~CCP1 sort-key gap~~ **Resolved** — conservatism documented; delta is a lower bound; confirmed by project owner.
4. ~~Memory bound for PermutationGenerator~~ **Not applicable** — measurement instrument, not a CKB engine; full-corpus map is correct.
5. ~~AIM1 consumer~~ **Deferred** — terminal outputs correctly produced; AIM1 is a future gate.

**Gate:** CCP1 baseline run requires `partially_verified` for Steps 20 and 21. The only outstanding item is a first real-corpus run confirming non-zero output and non-empty `process_metrics.csv` rows for steps 20 and 21.

### Priority 4 — Whole-process attempt lifecycle (blocks any publication claim)

**What:** The symlink-swap publication model is `partially_verified`. The non-atomic window during first migration from a real output directory to a symlink has not been tested. Concurrent-reader behavior during the swap has not been verified.

**Gate:** Deferred until CCP2 first replicate is in hand — lifecycle correctness is a pre-publication gate, not a first-run gate.

### Priority 5 — Allocation, revaluation, reclassification (blocks CCP4/CCP5)

**What:** Steps 6, 8, 9 are `partially_verified` or `unknown`. None can contribute to a published cost point until their engine contract entries in the audit register are at least `partially_verified` with explicit owner approval.

---

## Initial Audit Scope (original, preserved)

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

---

## Session Roles

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

---

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

---

## Session Start Prompt

At the start of each verification session, the agent should be given:

```text
You are verifying Ledger Lab's architecture, not adding a feature.
Read AGENTS.md, VERIFICATION_SESSION_PROTOCOL.md, the relevant layer README,
CKB_LAYER_EXECUTION_MODEL.md, MEASUREMENT_AND_LOGGING.md, and the nearby code.
State one falsifiable architecture claim and one cheap check before editing.
Trace actual producers and consumers. Treat undocumented behavior as unknown.
New code added without a prior verification session is not_audited, not verified.
Do not claim a cost point or implementation maturity until the contract, consumer,
controls, materialization, and evidence gates pass.
```
