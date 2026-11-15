# Contributing to the Ledger Lab research experiment

Start with [STATUS.md](STATUS.md), then [Track D](tracks/D-experiment/STATUS.md)
and its [CCP](tracks/D-experiment/ccp/STATUS.md) or
[AIM](tracks/D-experiment/aim/STATUS.md) workstream. Read the
[plain-English overview](LEDGER_LAB_OVERVIEW.md) once for the research rationale.
[QUICKSTART.md](QUICKSTART.md) supplies technical setup; it is not the research queue.

## Why this architecture is being studied

Ledger Lab tests a particular financial-processing proposition: retain auditable
business events, carry forward instrument-level state, resolve dated attributes,
and execute compatible financial processes together in a declared sorted flow.
A common key can be any declared attribute set; vendor order is one workload,
not the definition of CKB. Different business processes may require different
CKB flows and explicit sorts between them.

The proposed economy comes from reusing records, state and generated SJEs while
they are available to compatible consumers. It does not require a new history scan
for every logical layer. New key orders, unavailable state or unbounded groups
can require sorts, spills or separate processes; those costs must be measured.
The numbered 01–05 directories express ownership, not five mandatory passes.

These choices differ from the usual database/query or distributed-data defaults.
A contributor need not agree with the hypotheses, but must preserve the declared
substrate and contracts while testing them. A replacement implemented in Python,
Spark or a database would answer a different cost question. Scala simulates the
GenevaERS mainline; Bash orchestrates. Python requires owner approval for its
permitted preparation/post-engine scope. See [AGENTS.md](AGENTS.md).

For the detailed reasoning, use the [execution model](docs/CKB_LAYER_EXECUTION_MODEL.md)
and [reference architecture map](docs/REFERENCE_ARCHITECTURE_MAP.md).
The [method note](docs/BOB_METHOD_NOTE_CKB_EFFICIENCY.md) records the agentic
computational discipline; interpret its older examples through current agent rules.
The [TDS/IL discussion](docs/TDS_vs_IL_architecture.md) preserves the rationale for
retaining both transaction history and accumulated state; it is a discussion
record, not evidence of zero-cost pivots or a current Python execution contract.

## Choose the reading needed for your task

| Task | Read after Track D and the workstream status |
|---|---|
| Understand findings | Named result receipt and relevant audit entry; check population, dates, timing boundary and limitations |
| Change financial processing | Relevant layer README and nearby Scala implementation, execution model, architecture gates and verification protocol |
| Change orchestration | Actual Bash producer/consumer chain, workload contract and measurement boundary |
| Prepare synthetic inputs | Current strategy, generation proposal and the selected versioned workload contract |
| Work on AI | AIM status, input producer/feature contract and relevant validity assessment; separate exact accounting from estimated answers |
| Revise papers | Member editing handoff; evidence actually cited; private sources remain in their own repository |
| Understand a disputed choice | Linked design/reflection entry, then its archive if necessary; preserve attribution and corrections |

Do not read the whole handoff, audit register or archive by default. The
[document map](docs/README.md) routes tasks; the [catalog](docs/DOCUMENT_CATALOG.md)
is for finding a particular record, not a required reading list.

## Authority, evidence and disagreement

[AGENTS.md](AGENTS.md) sets repository rules. Current status selects the task and
current plans; contracts define intended behavior. The audit register and named
receipts establish what was actually checked. A plan, a confident explanation or
a passing fixture cannot upgrade implementation maturity.

Record a falsifiable claim and run the cheapest falsifier before changing a
financial contract. Reconstruct actual producers and consumers. An emitted file
must reach its declared downstream process; two sections in one JVM alone do not
establish CKB placement. Declare key order, effective dates, bounds, controls,
materialization and per-process metrics. Follow the
[architecture gates](docs/AGENTIC_ARCHITECTURE_GATES.md) and
[verification protocol](docs/VERIFICATION_SESSION_PROTOCOL.md).
Missing target-runtime capabilities belong in the
[GenevaERS gap register](docs/GENEVAERS_CAPABILITY_GAP_REGISTER.md).

If a claim fails, retain the failed approach and explain what changed. Distinguish
a contradiction of the hypothesis from a parser, population or protocol defect.
Do not change locked thresholds after observing results. A model comparison that
changes both features and model family does not isolate either causal effect.

## Working locations and session records

The Scala prototype lives mainly in `02-foundation/va_pipeline/`; logical owners
are 01 transformation/ARE, 02 sort/CKB/controls, 03 canonical state, 04 SJE engines,
and 05 aggregation-only perspectives. `99-interpretation` explains run evidence.
Full raw corpus data and large generated outputs are external to Git.

The Control lives in another repository. This CKB-informed workspace must not be
used as its uninformed builder. Read Control material only when needed, using
Track D's digest/exposure procedure. Do not transfer reflections, provisional
questions or revised papers without the established owner-directed process.

At session end, update the relevant status, place open actions in TODO, and leave
a short handoff with evidence, limitations and next step. Keep detailed receipts
with their evidence, and attributed reasoning in its journal. Retain superseded
plans and failed approaches in clearly labeled archives; link them when their
reasoning matters. Archive retention supports scrutiny without making old
instructions part of the default reading path.

The [earlier contributor guide](docs/archive/collaborator_review_2026-11-15/CONTRIBUTOR_ONBOARDING.md)
is preserved unchanged. Its predicted outcomes, first-corpus-run queue and older
technology statements are historical, not current instructions.
