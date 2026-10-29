# Ledger Lab — Contributor Onboarding

This document is written for a technical contributor — a student, researcher, or engineer — who
knows databases, distributed systems, or financial software and is encountering this repository for
the first time. It tells you honestly what exists, what does not, and what the work is.

---

## What This Is Trying to Prove

Two claims about enterprise financial data architecture:

**Claim 1 — The Minimum Cost Curve.**
There is a mathematically interior minimum to the total cost of maintaining enterprise financial
state. Most organizations have drifted far past it by materializing too many balance structures.
The cost of that drift compounds: more storage, more reconciliation, and structural inability to
answer questions that weren't anticipated when the posting key was designed.

**Claim 2 — The Instrument Pivot Theorem.**
A balance store keyed on Instrument ID, joined at report time to a Contract Attributes Record
(CAR) with *m* classifying attributes, supports all *O(v^m)* reporting cuts across those
attributes at zero marginal storage cost per new reporting dimension. The legacy architecture
is permanently blind to questions not in the original posting key. The instrument ledger
architecture is omnidirectionally queryable — any dimension you attach to the CAR opens every
historical period without rebuilding anything.

These are not opinions. They are hypotheses being tested empirically on 14 years of public
Virginia state vendor payment data. The purpose of this repository is to produce the measurements
that confirm or qualify them.

---

## What the Architecture Looks Like

The processing model is called **CKB — Common Key Buffering**. It is the pattern behind
IBM GenevaERS and shares lineage with the IBM Performance Engine. The core idea:

- Sort source events on a common key (Instrument ID + period + account).
- Stream through the sorted data once, maintaining only the current instrument's in-flight state.
- At each instrument break, emit updated balances, apply any generated events, and release the
  per-instrument buffer. Never load the whole file into memory.
- Pipe generated events (allocations, revaluations, reclassifications) back into the same ordered
  flow rather than opening new passes over the full history.

The five logical layers describe who owns what, not five required scans of the data:

| Layer | Logical responsibility |
|---|---|
| `01-transformation` | Normalize raw source records into balanced, traceable SJE partitions. |
| `02-foundation` | Sort contracts, CKB mechanics, state application, and controls. |
| `03-instrument-ledger` | Canonical journal history and instrument-level balance state. |
| `04-engines` | Read SJE structures, apply rules, emit balanced generated SJE structures. |
| `05-perspectives` | Aggregate outputs into reporting views. No accounting transactions here. |

A physical pass boundary exists only when a new sort order or unbounded memory requirement makes
it necessary. More logical layers do not automatically mean more passes. The cost curves measure
sort and materialization boundaries, not layer counts.

The language boundary is non-negotiable:

| Role | Language |
|---|---|
| All mainline financial processing | **Scala** |
| Pipeline orchestration (ordering, process wiring, exits) | **Bash** |
| Data preparation, post-run inspection, fixture generation | **Python only** |

Python in the mainline financial path invalidates the cost measurement. The curves measure the
Scala CKB substrate. A Python implementation measures a different runtime.

---

## What Actually Exists Today

The honest status as of the last audit:

**What runs and has been adversarially verified (at fixture scale):**
- Step 2: raw VA expenditure records → 39-field balanced SJE partitions, externally sorted on the
  CKB posting key. 1,520 SJE rows, 0 unbalanced journal groups, 0 sort-key violations.
- Step 3: sorted SJE + prior ledger → instrument-level ledger state. 59 ledger rows, ledger
  total $0.00, conservation controls pass.
- Active cost allocation smoke profile: 45 generated balanced allocation SJEs, rounding residual
  recorded, next-period divisor regenerated.
- Booked currency revaluation: 4 balanced SJEs generated, applied through the poster, controls pass.
- Canonical evidence bundle: run manifest, process metrics, sort events, partition catalog,
  reconciliation results — emitted for the baseline and allocation profiles.
- Post-run interpretation report: states what ran, which controls passed, which evidence is missing.

**What exists as code but has not been fully audited:**
- Reclassification (`arrangementReclass.scala`), forecasting/budgeting, and consolidation steps.
- Legacy aggregation step (Step 5 / `dataAggregation.scala`) — in-memory prototype, not
  the bounded perspective design.

**What is documented but not implemented:**
- Generalized 04 engine contracts (one rejected implementation; the gates are now documented).
- Generalized 05 perspective engine.
- Postgres-backed perspective aggregation (design exists in `docs/step5-postgres-design.md`).
- The two elimination processes (intra-unit ARE elimination, inter-unit scope sum).
- Named C1–C4 workload manifests and the full cost surface.

**What is broken and known:**
- Process metrics on macOS: Step 3 reports `records_read=0`, storage `?`. The pipeline ran but
  the measurement didn't. This is the most important thing to fix before any cost curve work.

---

## What the Goal Is

The goal is an **agentic cost-curve agent** — a system that can:

1. Declare a named workload (input snapshot, steps, materialization profile, code revision).
2. Run the pipeline through the Bash orchestrator.
3. Read the canonical evidence bundle.
4. Assert that process metrics are non-null, per-group balancing holds, and sort contracts matched.
5. Emit a verified cost point — only if all assertions pass.
6. Vary one axis (add Step 5, change materialization profile, change to a prior-period allocation)
   and compare the resulting cost dimensions.
7. Eventually compare C1 (post only), C2 (post + analysis), C3 (post + analysis + revaluation),
   and C4 (full pipeline including allocation) on the same input snapshot to produce the four-curve
   cost surface.

The C3/C4 overlap is the empirical proof of single-pass universality. If allocation collapses to
one pass via prior-period divisors (as the architecture claims), C4 should not cost more than C3.
If it does diverge, that divergence — and its cause — is itself a publishable finding.

The agent cannot produce meaningful results until the measurement substrate is reliable. An agent
reading `records_read=0` from a successful run will mis-state the cost of every step it measures.

---

## Where the Work Is

### The most urgent open item: repair the measurement substrate

Fix the process metrics emission in the Scala pipeline so that every run produces trustworthy
`process_metrics.csv` rows with actual `records_read`, `records_written`, `bytes_written`,
`wall_seconds`, and `peak_rss_bytes`. This is tracked in `SESSION_HANDOFF.md` → Next Work → item 2.
Until this is done, no cost point is credible.

### The next open items in priority order

1. **Validate evidence schemas on failed/partial/no-op runs.** An agent driving the pipeline will
   hit all of these. The evidence bundle must be interpretable for every terminal state.
2. **Define machine-readable workload manifests.** A workload manifest fixes: input snapshot hash,
   sbt revision, steps selected, materialization profile, and declared output capability. Two runs
   are only comparable when their manifests match on all fixed dimensions.
3. **Audit legacy steps (reclass, forecasting, consolidation).** These exist in code. Before they
   can contribute to a cost curve, their actual producers, consumers, schema, CKB placement, and
   memory behavior must be traced and recorded in the audit register.
4. **Define the C1 named workload and run three replicates.** Steps 2 and 3 on the fixture
   snapshot. The first verifiable cost point. Three replicates establish that the measurement
   is stable before comparing to anything else.
5. **Define and run C2.** Add Step 5 aggregation. The delta from C1 is the measured marginal cost
   of reporting materialization.

### The further items that lead to the theorem

- Complete the engine contract tests for booked revaluation (rate/period edge cases, CKB placement).
- Implement the two elimination processes through the shared CKB path.
- Define C3 and C4 workload manifests.
- Run C3 and C4 on the same input snapshot. Document the delta and its source.

---

## How the Repository Is Organized

```
01-transformation/          Logical: source normalization and ARE
02-foundation/
  va_pipeline/              Current Scala implementation (this is where the financial code lives)
  sort_specs/               External sort contracts
03-instrument-ledger/       Logical: canonical journal and balance state
04-engines/                 Logical: SJE-in/SJE-out engine boundary
05-perspectives/            Logical: aggregation-only views
99-interpretation/          Post-run evidence interpreter
data/                       Small sanitized fixtures (full VA history is external)
docs/                       Architecture, contracts, measurement design
scripts/                    Bash orchestration + Python data prep utilities
```

The runnable code is almost entirely under `02-foundation/va_pipeline/`. The other numbered
directories are logical ownership boundaries and design contracts. Do not infer that a layer
directory contains a working implementation.

---

## How to Get Started

**Prerequisites:** JDK 21, sbt 1.10.10, Bash. No Postgres, no Spark.

```bash
# Compile
cd 02-foundation/va_pipeline
sbt -batch compile
cd ../..

# Run the fixture smoke test
bash scripts/run_repo_fixture_smoke_test.sh --show-data

# Run the allocation profile
bash scripts/run_repo_fixture_smoke_test.sh --with-allocation --show-data
```

A passing smoke test is a functional check, not architecture validation or a cost point.

---

## Before Changing Any Financial Code

Read these documents in order:

1. [`AGENTS.md`](AGENTS.md) — language boundaries and review gate. Non-negotiable.
2. [`README.md`](README.md) — architecture overview and honest implementation status table.
3. The relevant layer README (e.g., `02-foundation/README.md`).
4. [`docs/CKB_LAYER_EXECUTION_MODEL.md`](docs/CKB_LAYER_EXECUTION_MODEL.md) — physical pass
   and sort boundary model.
5. [`docs/MEASUREMENT_AND_LOGGING.md`](docs/MEASUREMENT_AND_LOGGING.md) — canonical evidence
   schemas.
6. [`docs/AGENTIC_ARCHITECTURE_GATES.md`](docs/AGENTIC_ARCHITECTURE_GATES.md) — the mandatory
   pre-implementation checklist. A previous engine attempt was discarded because it bypassed these.
7. [`docs/VERIFICATION_SESSION_PROTOCOL.md`](docs/VERIFICATION_SESSION_PROTOCOL.md) — the
   session roles and audit sequence.
8. [`docs/VERIFICATION_AUDIT_REGISTER.md`](docs/VERIFICATION_AUDIT_REGISTER.md) — current state
   of every architectural claim under test.

The audit register is the shared truth document. If a claim is marked `unknown` or
`not_implemented`, the next session for that claim is a **Verification Lead** session, not a
builder session. A Builder session only begins once the relevant contract is `verified` or
approved `partially_verified` in the register.

---

## What AI Will Get Wrong

If you use an AI assistant on this repository, it will likely:

- Present the historical `PROJECT_BRIEF.md` — which describes a complete grand plan — as the
  current implementation status. It is not. Read the audit register.
- Suggest Python for something in the financial path. That invalidates the cost measurement.
  The AGENTS.md rule is absolute.
- Treat a passing fixture run as architecture or cost evidence. It is a functional smoke test.
- Describe five implemented processing layers. Only `02-foundation/va_pipeline/` runs. The other
  four directories are contracts and scaffolding.
- Propose a new engine or perspective without following the architecture gates. One engine attempt
  was already discarded for this reason.

The safest way to use AI here is to start it with the text in
[`docs/VERIFICATION_SESSION_PROTOCOL.md`](docs/VERIFICATION_SESSION_PROTOCOL.md) → Session Prompt,
and explicitly assign it a role (Verification Lead, Process Auditor, etc.) rather than letting it
default to feature implementer.

---

## The Relationship to Broader Projects

- **GenevaERS** owns the generalized execution engine: Workbench, Run-Control Apps, the Java
  Performance Engine, and CKB extensions. Future Ledger Lab workloads may be translated to
  GenevaERS metadata and run there. Engine implementation does not belong in this repository.
- **Sharealedger** is the intended future home for open ERP financial metadata and configuration.
  Ledger Lab produces measured fixtures and evidence; it does not become an ERP runtime.

Ledger Lab's job is exactly one thing: produce the reproducible, comparable measurements that
ground the cost-curve and pivot-theorem claims in the monograph.

---

## The One-Sentence Summary

> The architecture is sound and partially verified at fixture scale. The measurement substrate
> needed to produce comparable cost points is not yet reliable. The next milestone is not a new
> financial feature — it is making the existing pipeline emit trustworthy evidence that an agent
> can read and interpret without human intervention.
