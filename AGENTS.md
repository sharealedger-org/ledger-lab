# Ledger Lab Agent Rules

These rules apply to every AI agent and contributor working in this repository.

## Language Boundaries

### Scala: Mainline Engine

Scala is the code base for all mainline financial processing. Use Scala for:

- source-to-SJE transformation and the ARE;
- Universal Journal and Instrument Ledger processing;
- CKB sorting/application contracts;
- generated SJE engines;
- step-ups, allocations, FTP, reserves, revaluation, consolidation, and eliminations;
- perspective aggregation and view generation when it is part of the measured engine;
- control totals that are produced as part of financial processing.

Do not replace Scala engine work with Python, Bash, PowerShell, JavaScript, notebooks, or another runtime. Code-generation speed is never an engine-design criterion. Correct architecture, reproducibility, scale behavior, and measured runtime are the criteria.

### Bash: Stable Orchestration

Bash is the stable scripting language for pipeline orchestration. Use Bash for:

- ordered job control;
- process configuration;
- file and partition movement;
- exit-code handling;
- workload sequencing and admission control;
- invoking compiled Scala process components through established build/entry-point contracts;
- connecting bounded Scala processes with ordered streams, named pipes, or declared partition files;
- stable laptop and batch execution interfaces.

Do not put financial logic, accounting rules, CKB behavior, view calculations, or engine algorithms in Bash. Do not introduce PowerShell or another platform-specific runtime for pipeline orchestration.

Bash orchestration may invoke compiled Scala engine processes, but must not embed Scala logic,
generate ad hoc Scala programs, or become a second implementation of the engine. The mainline
engine is a Scala process topology, not necessarily one monolithic Scala application.

### Python: Editing and Ad Hoc Operations Only

Python is not acceptable for mainline financial flow. It may be used only for:

- editing or generating configuration and fixture files;
- data inspection and one-off research analysis;
- repository audits and migration utilities;
- append-only ad hoc metrics/logging where the result is not part of the measured financial engine;
- test-data preparation and sanitization.

Python must not read source transactions and perform accounting, posting, CKB, step-up, calculation-engine, or perspective-processing work in place of Scala. A Python implementation of an engine invalidates the cost experiment because it measures a different substrate.

## Measurement Integrity

- Do not include code-generation speed in engine design or cost-curve reasoning.
- The measured financial path must remain Scala.
- Keep orchestration overhead, engine compute, storage, sorting, and reconciliation metrics separately identifiable.
- Record process boundaries, pipe/partition boundaries, and per-process memory high-water marks.
- Prefer bounded Scala process piping or declared partition materialization over loading unbounded history into one JVM.
- Do not compare runs that use different mainline implementation languages as though they measured the same engine.
- Preserve the distinction between logical layers and physical passes. A layer does not imply a scan; an external sort or required materialization may create a pass boundary.

## Architecture Layers

The numbered directories describe ownership and execution responsibility, not automatic scan boundaries:

```text
01-transformation   TTL + ARE: source/product records -> balanced SJE partitions
02-foundation       sort contracts, CKB mechanics, controls, replacement processing
03-instrument-ledger canonical Universal Journal and Instrument Ledger state
04-engines           SJE structures in -> balanced SJE structures out
05-perspectives      aggregation-only domain outputs
```

Engines read and generate SJE partitions. Perspectives aggregate only; they do not generate or apply accounting transactions. CKB may pipe work between these logical layers in one ordered process. A new physical pass is required only when a new sort order or an unbounded materialization requirement makes it necessary.

## Before Editing

- Read the relevant layer README and nearby implementation before changing code.
- Preserve the Scala/Bash/Python boundary above.
- Prefer existing sort, CKB, SJE, CAR, and control-total contracts over new abstractions.
- Keep public paths and agent interfaces stable unless the task explicitly changes them.
- Add or update focused control-total tests for changes to financial behavior.

## Review Gate

Before accepting a change, verify:

1. No Python or Bash mainline substitute was introduced.
2. No financial logic moved into orchestration scripts.
3. SJE lineage, balancing, rule identity, and effective dates remain explicit.
4. Sort boundaries and memory/materialization assumptions are documented.
5. The change preserves comparable engine measurements across cost-curve runs.
