# Perspective Layer

This directory is the experimental home for the domain perspective layer of Ledger Lab. It
depends on the Foundation, Instrument Ledger, and Engines layers for canonical state, generated
SJE partitions, and step-up inputs. This layer aggregates only; it does not generate or apply
accounting transactions.

Perspective aggregation may run as a view inside the current ordered CKB process. It becomes a
new physical pass only when a required grouping/order cannot be maintained within the memory
contract and an external sort or other materialization boundary is required.

It extends the existing Instrument Ledger demonstration from:

```text
customer events -> instrument ledger -> analytical views
```

to:

```text
customer events -> shared core -> generated step-ups -> perspectives
                                               |-> Finance
                                               |-> Tax
                                               |-> MIS
                                               |-> Risk
                                               `-> Liquidity
```

The purpose is to measure two things together:

1. The cost of maintaining customer balances, generated transactions, and perspective outputs.
2. The additional economic perspectives made feasible by sharing a common core and reusable step-ups.

This is infrastructure and experiment design only at the current stage. The existing VA pipeline remains the public-data base experiment. Perspective fixtures will be synthetic unless a source dataset provides the required economics, and they must be labeled accordingly.

## Design Principles

- Preserve the existing technology invariant: mainline financial processing is Scala; Bash orchestrates; Python records and analyzes metrics only.
- Keep atomic source events and their Instrument IDs as the provenance anchor.
- Treat generated step-up transactions as first-class, balanced, traceable events.
- Classify generated economics as `CORE`, `SHARED_STEP_UP`, or perspective-specific.
- Generate shared economics once and reuse them across perspectives where the economics are genuinely common.
- Keep Finance, Tax, MIS, Risk, and Liquidity outputs separately addressable without rebuilding the shared core.
- Measure capability as well as cost. A lower cost that cannot answer the required perspective is not an equivalent result.
- Do not assume the total cost function is globally U-shaped. Report interior minima, boundary minima, monotone behavior, concavity, noise, and insufficient evidence separately.

## Directory Map

- `specs/`: machine-readable experiment plans and configuration contracts.
- `schemas/`: record, step-up, perspective, provenance, and metric schemas.
- `rules/`: rule definitions for core and perspective-specific generated economics.
- `fixtures/`: small synthetic fixtures for perspective behavior and control totals.
- `engine/`: future Scala implementation boundary for step-up generation and application.
- `results/`: generated experiment output; never source input.
- `tests/`: focused tests and control-total fixtures.

## Planned Canonical Concepts

### Source Event

An immutable economic event with an `event_id`, `instrument_id`, effective date, amount, source system, and complete provenance.

### Core Balance

A reusable economic position derived from source events and applicable common rules. The core is the gray overlap shared by multiple perspectives.

### Step-Up Transaction

A balanced generated event derived from a source event, core balance, contract attribute, or prior step-up. Every step-up must identify its source, rule version, effective date, perspective scope, and reversal/supersession relationship.

### Perspective

A named output interpretation such as Finance, Tax, MIS, Risk, or Liquidity. A perspective may consume core balances, shared step-ups, and perspective-specific step-ups.

## Initial Step-Up Classes

| Class | Examples | Intended reuse |
|---|---|---|
| `CORE` | Cash movement, principal, customer position | All applicable perspectives |
| `SHARED_STEP_UP` | Common funding cost, common entity amount | Multiple perspectives |
| `FINANCE_ONLY` | IFRS or LGAP reserve treatment | Finance |
| `TAX_ONLY` | Tax basis or tax timing treatment | Tax |
| `MIS_ONLY` | Allocated expense or customer profitability | MIS |
| `RISK_ONLY` | Risk-based loss contingency or exposure | Risk |
| `LIQUIDITY_ONLY` | Liquidity horizon or funding treatment | Liquidity |

These names are a starting taxonomy, not a claim that every rule belongs to exactly one class. The rule specification must record the economic rationale and reuse scope.

## Experiment Axes

The agent will vary configurations while the laptop spends runtime executing them:

1. Customer-event materialization.
2. Instrument-level balance materialization.
3. Core-only versus core-plus-step-up processing.
4. Shared step-up reuse versus independent perspective generation.
5. Perspective-specific balance materialization.
6. On-demand perspective derivation.
7. Attribute and balance-key permutations.

Each run should record:

- Compute time and wall-clock time.
- Input, intermediate, and output storage.
- Source-event, core-balance, step-up, and perspective row counts.
- Maintained balance/master-file count.
- Reconciliation obligations.
- Perspectives and reporting cuts available.
- Shared economic coverage.
- Cost per newly enabled perspective or reporting capability.

## Planned Experiment Families

### A. Balance Permutation Experiment

Compare key-embedded balance permutations against Instrument ID plus CAR step-up resolution.

### B. Step-Up Reuse Experiment

Compare independent Finance, Risk, MIS, Tax, and Liquidity pipelines against a shared core with reusable step-ups.

### C. Perspective Capability Frontier

Measure which perspectives become answerable at each cost point. This is not only a cost curve: it is a cost-capability surface.

### D. Engine and Materialization Experiment

Vary recompute and materialization levels while preserving the same workload and control totals.

## Evidence Rules

- VA source-data results and synthetic perspective-fixture results must be reported separately.
- A finite sweep may identify observed shape over a sampled domain; it does not prove a global U-shape.
- An interior minimum requires replication or neighboring runs to establish stability.
- Every generated transaction must balance and trace to source events and rule versions.
- Shared economics must be demonstrated by identical control totals across consuming perspectives.
- Any cost proxy must expose its weights and must not be presented as an intrinsic economic cost.

## Implementation Order

1. Freeze this directory boundary and configuration vocabulary.
2. Define schemas and control-total expectations.
3. Build the smallest synthetic fixture containing one core and multiple step-up classes.
4. Implement the Scala step-up generation/application boundary.
5. Add perspective-specific outputs and reconciliation checks.
6. Connect the runtime to the agentic experiment manifest.
7. Run repeated sweeps and analyze observed cost-capability surfaces.

## Current Status

- Directory and planning boundary: established.
- Scala perspective engine: not started.
- Perspective schemas: not started.
- Synthetic fixtures: not started.
- Agentic sweep contract: provisional; do not treat existing scripts as final until the schemas are approved.
