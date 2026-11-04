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

For the current verified status of every architectural claim, see
[`docs/VERIFICATION_AUDIT_REGISTER.md`](docs/VERIFICATION_AUDIT_REGISTER.md). For the
research programme state and what to run next, see
[`docs/RESEARCH_OBJECTIVES.md`](docs/RESEARCH_OBJECTIVES.md) and
[`SESSION_HANDOFF.md`](../SESSION_HANDOFF.md) (top entry = start here).

**Implemented and test-verified (2026-11-04, 48/48 tests pass):**
- Step 2: raw VA expenditure/APA records → 39-field balanced SJE partitions, externally sorted.
- Step 2p: VA PO commitment events → sorted SJE with `transIPID` from `VENDORID`.
- Step 2m: k-way merge of payment + PO SortedJE streams.
- Step 3: CKB match-merge post → instrument-keyed ledger (`LDGR20YY.csv`). Controls pass.
- Step 5v: single-pass CKB view extraction → four bounded analytical tables (Views A–D).
- Step 20: GL post → account-keyed balances (`GL20YY.csv`). CCP1 baseline.
- Step 21: Permutation generator → `PERM_TBL20YY.csv`. CCP1 cost-explosion evidence.
- Step ttl: APA direct-download TTL — resolves name columns to keys, routes by fiscal month.
- Process metrics (`RESULT`/`RESOURCE` log lines): non-zero for all active steps.
- AIM5: agency name classifier — 312/312 positives correct, precision 1.0000. ✅ PASS

**Implemented but not yet run on full corpus:**
- Full 14-year VA corpus run (FY2003–FY2016) — Stage 2 validation (1-year) in progress.
- CCP1–CCP3 three-replicate cost points on full corpus.
- AIM1–AIM4 Python fitting scripts (to be written after corpus run).

**Documented but not yet verified for cost curve contribution:**
- Reclassification, forecasting/budgeting, consolidation (code exists; not audit-registered).
- Booked currency revaluation (code exists; engine contract audit in progress).
- CCP4 and CCP5 (blocked on CCP3 and engine contract audits).

---

## What the Goal Is

Two published papers from a single run of the pipeline against the full Virginia procurement
corpus (FY2003–FY2016, ~133M PO lines):

**Paper 1 — Minimum cost curve.** Measure the cost of CCP1 (legacy GL), CCP2 (instrument
ledger), CCP3 (+ perspective views), CCP4 (+ revaluation), CCP5 (+ allocation). Show that
CCP2–CCP5 cost no more than CCP1. The cost of each added capability layer is the finding.

**Paper 2 — AI consequence of posting as lossy compression.** Run AIM1–AIM5 against the CCP1
substrates (GL output, permutation table) and the CCP2/CCP3 substrates (instrument ledger,
views). All AIM tests fail on CCP1. All pass on CCP2/CCP3. The gap is the empirical proof
that posting destroys predictively useful information.

The cost surface has three independent axes — capability layer (CCP1–CCP5), time partition
granularity (`annual`/`quarterly`/`monthly`), and materialization scope (`movement-only`
through `movement+all-rollups`). See [`docs/TEST_DESIGN.md`](docs/TEST_DESIGN.md) §4 for
the full experimental design.

---

## Where the Work Is

See [`SESSION_HANDOFF.md`](../SESSION_HANDOFF.md) top entry for the current next step.
See [`docs/ENGINE_IMPLEMENTATION_PLAN.md`](docs/ENGINE_IMPLEMENTATION_PLAN.md) for the ordered
sub-task list. The staged run plan (Stage 1 AIM5 complete → Stage 2 1-year validation →
Stage 3 5-year slice → Stage 4 full corpus) is in
[`docs/RESEARCH_OBJECTIVES.md`](docs/RESEARCH_OBJECTIVES.md) §6.

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

> The engine is built and the measurement substrate is working. The next milestone is the
> first full-corpus run — FY2003 validation, then 5-year slice, then 14 years × 3 replicates
> per CCP — producing the cost surface and AIM evidence that ground both papers.
