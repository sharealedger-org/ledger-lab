# Proposed Phase 2 synthetic generation CKB

**Bounded implementation evidence, 2026-11-15:** The
[FY2003–FY2004 pilot](../data/test_evidence/component_pilot_0304_20261115/README.md)
ran successfully with an additional determining-key assignment sort/merge between
context and entity order. Its bounded contract is in
[COMPONENT_PILOT_0304_CONTRACT.md](COMPONENT_PILOT_0304_CONTRACT.md). The wider design
below remains a proposal beyond that evidence. Consolidate repository documents
in a separate session before larger execution.

2026-11-15. Design proposal only, before test code. This generator supports
integrated experiment Phase 2: initial vendor-centered synthetic work, likely
informing Papers 3 and 4 on improved cost curves and AI models. Phase 1 is direct
VA corpus work; Phase 3 broadens the topology from allocations to reclass. See the
[connected-process strategy](SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md).
No population size, new schema adapter or
runtime capability is accepted merely by appearing in this proposal.

## Workload and ownership

Produce a reproducible vendor-centered synthetic input package with a limited
employee and revenue population, related through ordinary business evidence.
The immediate question is whether useful temporal and cross-process evidence can
reach the early AI experiments without first building allocation/reclass engines.

Use a one-off compiled Scala generation job with two declared CKB orders. Reuse
existing sort, stream merge, effective-date and control contracts where they fit.
Generation owns source preparation in 01; sorting/control mechanics belong to 02.
Do not create a general-purpose simulation framework for the first slice.
Bash wires the processes and partitions. Financial events enter the existing
TTL/ARE and posting path; generation does not write posted ledger state.

## Inputs and source policy

Freeze source hashes, generation rules/version, seed, horizon, reference histories,
namespace reservations and population/relationship distributions in a manifest.
Separate these inputs:

| Input | Purpose |
|---|---|
| Immutable observed VA sources | Preserve original facts, grains and controls |
| Existing master/alias evidence | Anchor eligible vendor identities without claiming unresolved aliases are proven parties |
| Small scenario specification | Declare contexts, mechanisms, dated changes, noise and population sizes |
| Existing reference/SAL/CAR/party/hierarchy templates | Reuse valid history structures, with explicit adapters |
| Independently reserved evaluation cases | Test mechanisms beyond the initial generator assumptions |

Recommendation: preserve the observed corpus and generate a separately labeled
synthetic companion population. Synthetic vendor histories may be anchored to
declared observed patterns or supported IDs, but employee/revenue links remain
fictional. Do not attach invented employee behavior to a real named person.
Do not turn agency-grain observed receipts into taxpayer-grain facts. Any amount
decomposition of an observed parent needs a separate explicit conservation policy;
the first slice should use additive synthetic events with separate totals instead.
Select companion scale after reviewing the scenario matrix and source profile.

## Topology

**Adapter refinement from owner discussion, 2026-11-15:** Before choosing a
lookup map, consider sorting by the determining keys, merging source/identity/
reference/rule partitions, emitting resolved records with explicit ambiguity
statuses, then sorting into the receiving process's CKB order. Component order,
assignment order and entity execution order can differ. Share compatible orders;
declare additional sorts when required. This is a design requirement to evaluate,
not evidence that existing adapters already implement a sorted assignment join.

```text
Frozen sources + manifest + scenario/reference specifications
          |
bounded preparation / declared sort to context order
          |
G1: context CKB -- source-pattern evidence + participant slots + dated context rules
          |                         |
          |                         +--> withheld truth and generation controls
          v
entity-addressed event and change instructions
          |
external sort by process / entity / effective date / precedence / stable ID
          |
G2: entity-temporal CKB, with distinct process sections
          +--> vendor source events and histories
          +--> employee source events and histories
          +--> revenue source events and histories
          +--> validation controls and provenance
          |
declared source adapters --> TTL/ARE --> existing financial CKB --> views
                                                        |
                                     cutoff-safe evidence --> named AI evaluator
```

Two generation orders are proposed because neither context order nor entity order
provides the other. They are not a claim that all subsequent financial processing
fits into two passes. TTL/SJE sorts, posting, dimension routing and report reductions
retain their own declared boundaries. Compatible output emission shares a pass;
no extra pass is added merely for another logical layer.

## G1: generate coherent business context

Owner clarification, 2026-11-15: these contexts may be synthetic business
components cutting across conventional business processes, similar in spirit to
component business models. Invent the component/activity model first, then emit
its proposed activity into the vendor, employee and revenue flows. This is a
generation hypothesis, not a claim that actual VA organizational components have
been recovered. Contexts need not correspond one-to-one with processes or entities.

Common key: `(generation_population, context_id)`. A context could be an agency
project, procurement engagement or service activity. A context need not be an
accounting instrument. Include independent background contexts and unrelated
activity, rather than connecting every participant to the vendor story.

Merge conceptual partitions A/B/C: context definitions and selected source-pattern
anchors; participant memberships; dated context rules and changes. Sort each by
context, effective date, declared record precedence and stable record identity.
These are proposed logical partitions, not a replacement for existing financial
A/B/C contracts.

Generate coherent event instructions at each context/date boundary. An example:
a vendor delivers a service, an employee has a dated responsibility for the
project, and receipts occur for the same program. Some cases have explicit order
or project evidence; others offer only partial overlaps or misleading similarities.
This expresses a possible business connection without assigning vendor costs to
revenue or employees.

Emit one instruction per entity/process event or history change. Multiple
instructions from one context can share a private provenance link, but AI-visible
records share a business identifier only when the scenario deliberately makes
that evidence observable. Do not expose the private context/scenario key as a
universal join key. Membership in the same generated context is not automatically
the evaluator's positive label; specify the exact relationship being assessed.

Live state: current context's bounded participant slots, current rule/reference
versions, counters and deterministic random state. Stream instructions rather than
buffering full event history. Declare a participant limit and fail on overflow;
do not silently split a context or replace a many-party relationship with pairs.
If the required context is larger, redesign its routing/sort contract before code.

## G2: generate temporal entity histories

Common key: `(process_kind, entity_namespace, entity_id)`. Within a key, order by
effective date, explicit change/event precedence and stable event ID. A vendor can
appear in several contexts; this sort reunites those instructions. Distinct process
sections preserve their own grains and state, even when sharing a generic merge
loop. Revenue keys follow its declared synthetic grain; they need not be parties.

Merge conceptual partitions A/B/C: entity seed/master versions; G1 event
instructions; routed reference/relationship changes and observation-cutoff markers.
Route required changes to affected keys through declared membership partitions;
do not assume an unbounded reference-to-entity reverse map. Bounded global reference
tables may be shared only under explicit row/byte limits.

At a date boundary apply effective changes before events that use them; reject
unresolved same-date ties. Generate vendor, employee and receipt source records,
dated SAL/CAR/party outputs as appropriate, and lineage. Maintain only the current
entity's finite process state and running controls. Oversized entities remain
streamable; do not load an entire vendor lifetime to generate it. Quiet entities
must still receive dated history changes without requiring a transaction.

Financial processing remains downstream. A future reclass section would evaluate
changed reference/rule versions against entity state inside the relevant financial
CKB, generating balanced SJEs there. G2 prepares its input histories; it does not
implement that future engine in the generator.

## Proposed record families and consumers

| Family | Minimum fields beyond schema/version | Consumer |
|---|---|---|
| Context definition | population, private context ID, type, horizon, mechanism/version | G1 |
| Membership | context, process, namespace, entity, role, effective start | G1; change-routing preparation |
| Instruction | process/entity key, event ID, effective date, availability date, kind, business payload, source lineage, rule/version | G2 |
| Entity/reference history | declared business key, effective start, version/change identity, attributes | Existing history adapters, then declared temporal consumers |
| Generated source event | stable source event ID, event date, process-specific business fields, amount/currency where applicable | Existing TTL adapter or a named new Scala adapter |
| Withheld truth | private context/event lineage, relationship predicate, validity interval, mechanism, inferability class | Controlled evaluator only |
| Controls/manifest | run/process/partition IDs, hashes, rows, amounts, rejects, state and materialization metrics | Validation and run evidence |

These are field requirements, not final executable schemas. Before test code,
enumerate exact headers/types and identify actual Scala parsers for every output.
Vendor adapters may be reusable; employee and synthetic receipt adapters are not
assumed to exist. Unsupported input must block its downstream slice rather than
be treated as consumed. Private lineage may live in a sidecar so ordinary source
schemas need not gain AI-visible answer fields. SJE output retains the established
contract and joins to that lineage through stable source-event identity.

## Financial bounds and bridge to VA summaries

Owner direction, 2026-11-15: keep the synthetic population independent and
recognizable, but where feasible generate explicit adjustments and reconciliation
records connecting it to higher-level VA financial information. Published totals
should constrain generation, rather than merely decorate an unconstrained result.

Reuse the existing Strand B ACFR work as a candidate starting point. Its current
[fixture manifest](../data/acfr_anchors/ACFR_FIXTURE_MANIFEST.md) distinguishes
derived values, estimates and missing detailed support; it is not a verified
tie-out. In particular, a total basis difference is not an independently explained
adjustment, and aggregate differences must not be added to their own components.

Proposed preparation before G1:

1. Select a small set of compatible financial anchors. Record fiscal year, fund
   and reporting scope, basis, currency/units, source location, provenance status
   and tolerance. Verify candidate figures and mappings before using them as hard
   constraints. A statewide total cannot silently bound a differently scoped fund.
2. Build a bridge specification identifying observed coverage, synthetic coverage,
   basis/timing/scope adjustments and unresolved remainder. Distinguish movements,
   balances and opening/closing-state identities. No implied counterparty identity
   follows from a summary-level anchor.
3. Give synthetic components explicit aggregate envelopes or shares, leaving a
   separately identified background/remainder population where needed. G1 emits
   activity inside those budgets; rounding and residual assignment use declared,
   reproducible rules. This is synthetic calibration, not an inferred economic
   allocation or evidence that the proposed component relationships are true.
4. Reconcile downstream results back to the same anchors at the same grain and
   basis, reporting residuals and violated bounds rather than forcing a zero.

Use separate comparison profiles. A synthetic-only profile reconciles synthetic
detail plus supported bridge items and an explicit remainder to its chosen
anchor. An observed-plus-synthetic profile can claim the same anchor only when
the synthetic detail covers a demonstrably excluded population or replaces an
explicitly removed amount in that comparison view. The independent companion
must not simply be added to an already overlapping observed population and called
a VA tie-out. Raw observed inputs remain unchanged in either case.

Bridge records need anchor ID, scope/basis, signed amount, effective period,
adjustment category, source/derivation, confidence/status, rule version and
overlap/exclusivity group. Separate supported accounting adjustments, declared
synthetic calibration items and unexplained residuals. A residual is a diagnostic,
not automatically a journal. Unsupported estimates remain estimates.

Where a supported adjustment is to be posted, use a declared adjustment source
adapter and ARE rules to produce independently balanced SJEs with named lineage.
Retain summary/GL grain when no entity is supported. Accounting offsets and sign
conventions need explicit contracts; a signed reconciliation difference alone is
not a journal specification. Financial logic remains Scala. Adjustment emissions
may share a compatible stream or require a summary-key boundary; declare the
actual topology rather than forcing them into an invented vendor key.

First target: one fiscal year and one well-defined fund/basis, with a few revenue,
expenditure and balance anchors if available. Verify the current FY2012 candidates
before choosing that year. Budget constraints and bridge specifications are input
contracts; final anchor checks consume actual generated/posting outputs. Do not
backsolve the intended AI relationship labels from the target financial totals.

## Reproducibility, dating and evaluation

Derive random choices from seed, stable context/entity/event identity, rule version
and purpose. No single global random stream whose results change with file order
or worker count. Persist identity assignments; avoid sequential renumbering when
new participants are added. IDs must not encode relationship labels or mechanisms.

Effective date selects the business version; availability/recorded date controls
what evidence a model can know. The existing start-date histories do not by
themselves provide a knowledge-time snapshot: cutoff-safe exports need an explicit
availability contract. Fixed as-of dates, event dates and supplied run dates stay
separate. Future scenarios later add parent-baseline and proposed-version identity.

Split evaluation at private connected-context groups before exporting evidence,
so shared events do not trivially cross train/test boundaries. An entity spanning
groups requires an explicit overlap policy. Hold out mechanisms as well as random
rows. Generated truth, evidence-supported inference and an AI proposal are separate
objects. Include justified unknowns, contradictory evidence and incidental matches.

## Controls, materialization and acceptance

Use a new immutable output root and attempt publication. Retain frozen inputs,
the sorted instruction partition for the first review, generated source/history
partitions and compact truth/control evidence. A later pipe profile can avoid
retaining intermediates only where order and restart contracts permit it. Measure
generation separately from financial engine compute; preserve source-population
and output-capability comparability in cost experiments.

Record per-process wall/CPU time, RSS, maximum live participants/state, sort and
spill bytes, emitted/retained bytes, row counts and lineage. Count expected rows
from scenario cardinalities and reconcile at context, entity and process breaks.
For financial inputs reconcile generated-event totals to transformed movements,
balanced journal groups and posted balances by currency and declared dimensions.
Synthetic companion totals are separate from unchanged observed totals.

Cheapest design falsifier: one vendor shared by two contexts, two employees with
different membership dates, one revenue stream, a quiet-period reference change
and a coincidental unrelated match. Trace it manually through both orders. The
result must preserve one vendor history, date-correct membership, shared observable
evidence only where intended, and no automatic allocation or label leakage.

After design agreement, test unchanged baseline first and a named
`connected-generation-phase2-v1` workload through actual orchestration. Verify
determinism under input reordering, same-date changes, unmatched keys, duplicate
events, oversized keys, downstream schema consumption, and withheld-truth isolation.
Record missing target GenevaERS capabilities before claiming measured CKB support.
Fixture success alone is not corpus-scale or AI-effectiveness evidence.

## Scope to finalize before test implementation

The owner agreed independent, recognizable synthetic companion inputs and the
two-order structure, and authorized further generation work on 2026-11-15.
Finalize initial business contexts, entity/process grains and small population
sizes; confirm employee/revenue source
adapters and which evidence reaches the first AI evaluator. Then finalize the
scenario matrix, exact schemas, key encodings, state limits and expected controls.
Keep Phase 3 reusability in stable identities, versioned records, temporal
operands, relationship provenance and declared process interfaces. Defer a generic
rule language, allocation engine and scenario scheduler until their workloads are
defined. No generation/test code or runtime execution accompanies this proposal.
