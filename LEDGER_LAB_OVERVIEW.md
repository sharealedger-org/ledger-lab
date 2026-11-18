# Ledger Lab: Following a financial question where the evidence leads

A financial system is usually built around reports people already know they
need. Ledger Lab asks what it would cost to preserve enough business detail to
answer questions nobody specified when the ledger was designed. It also asks
whether an AI can use that history to propose a question, test an idea, and
decide what to investigate next while making clear what the evidence supports.

## An investigation you could run

Suppose an AI notices that a shared expense appears to track a different
activity from the one used to allocate it. It proposes a new rule. The
financial engine applies both rules to the same records, reconciles the results,
and shows which recipients would bear more or less cost. The result suggests
a follow-up: does the proposed driver still hold in another period?

- **Financial history** preserves the identities, dates, and relationships that
  let an investigator move from a total to the activity behind it.
- **Reusable processing** lets the engine reuse preparation when a rule changes.
  The cost experiment asks how much work each additional question or rule
  iteration requires.
- **Measurement** establishes the consequences of the proposed rule. Whether
  the driver reflects how the business works is a separate empirical question.
  Observations, rule-based calculations, learned associations, forecasts, and
  scenario assumptions each call for different evidence.

The [allocation feedback loop](docs/STREAM_LAYER_EVOLUTION_AND_ALLOCATION_FEEDBACK_PLAN.md)
is a proposed implementation of this example. The engine would apply a prior
committed allocation basis, derive updated weights and eligibility from new
data, and measure how the updated basis would change each recipient's allocation.

## What the AI experiment adds

AI-generated SQL is a strong candidate benchmark for open-ended reporting and
can support parts of the investigation. The proposed test is whether a reusable
learned model helps identify worthwhile relationships or follow-up questions
across unfamiliar combinations of dates, dimensions, and measures. Concepts
such as churn or seasonality could become part of an ongoing investigation,
with associated financial measures calculated under declared rules and
predictions accompanied by their horizon and uncertainty.

A proposed evaluation would hold back whole investigations, including cases
where new evidence changes the next question or leaves a relationship
unresolved. It could compare the usefulness of what each approach finds,
whether its calculations reconcile, how often it makes unsupported claims,
and how much preparation and runtime it takes to reach a usable result.
Existing single-task prediction tests are initial probes. The
[Codex reflections](tracks/D-experiment/CODEX_REFLECTIONS.md)
describe the broader research objective. [STATUS.md](STATUS.md) gives the
current checkpoint.

## The distinction behind the research

Posting accumulates detailed events into balances at a chosen grain: the dimensions that
identify one balance record, such as vendor, account and period. A balance
can omit vendor identity, timing, event sequence or other distinctions needed by
later questions. That does not establish that an institution deleted its raw
history: the question is which retained representation answers a request, and
what reconstruction, maintenance and reconciliation it requires.

An instrument is an identified business entity, such as a vendor or contract,
whose state is carried forward. An instrument ledger carries identity and declared balance dimensions forward,
then resolves attributes under an explicit date policy. Detailed events remain
available for audit and questions that the balance grain cannot answer. A joined
attribute does not restore an event sequence already absent from that balance.
Instruments with no new activity, changed hierarchies and late information are part of the
research problem, not incidental edge cases.

The execution proposal is Common Key Buffer (CKB) processing, also called common key
buffering: bring compatible
sorted records, state and reference data together, execute dependent process
sections and let downstream sections consume generated Standard Journal Entry (SJE) records
in the declared flow. These are accounting lines in a common format; the debit/credit lines
in each journal group must balance.
A key need not be vendor ID. Multiple business processes may need their own keys,
CKB flows and connecting sorts. A logical capability need not add a physical scan;
its compute, state, output and any new sort/materialization still have a cost.

The [execution model](docs/CKB_LAYER_EXECUTION_MODEL.md) defines these conditions.
The [reference map](docs/REFERENCE_ARCHITECTURE_MAP.md) and dated reflections
preserve the reasoning and its development. They explain the research position;
measurements and independent controls must test it.

## Two workstreams inside one experiment

[Track D](tracks/D-experiment/STATUS.md) is the main path. Cost Curve Points (CCP) and AI/model evidence tests (AIM) are its complementary workstreams, formerly B1 and B2.

| Workstream | Question | Evidence required |
|---|---|---|
| [CCP: cost/capability](tracks/D-experiment/ccp/STATUS.md) | How do retained outputs, sorting, state and enabled financial capabilities affect total cost? | Comparable workloads, actual resource/pass/storage measurements, controls and replicates |
| [AIM: AI/exploration](tracks/D-experiment/aim/STATUS.md) | Which distinctions support prediction or reusable financial inquiry, and what information does a given representation lose? | Matched populations/tasks, valid temporal splits, held-out questions, baselines and explicit uncertainty |

The Mix graph studies the event/balance materialization trade-off; the Process
graph studies added financial capabilities; the Engine graph compares execution
approaches. Their [design](docs/COST_GRAPH_DESIGN.md) contains models as well as
measured constants. A modeled minimum is not an observed universal minimum.
The old CCP/AIM IDs remain useful protocol/evidence identifiers.

## What the recorded work currently supports

- Monthly Virginia (VA) pipelines completed at corpus scale, including a later expanded
  expenditure/revenue/purchase-order workload. Named output sets matched across selected
  executions. Scope, implementation and timing differ between checkpoints.
- An earlier expanded reporting attempt failed. A later three-worker reporting
  run passed all 21 full-record comparisons against its baseline after a documented
  precision correction. The comparison changed several factors at once, so it
  does not isolate the effect of a single design change. See the
  [acceptance record](data/test_evidence/reporting_three_way_20261117/completion/ACCEPTANCE.md).
- AIM3 has task-specific predictive signal. Posted-total/instrument-balance
  baselines and combined features complicate any broad advantage claim; a fiscal-
  period discrepancy leaves the reported incremental gain provisional.
- AIM4 failed its implemented fourth-quarter-ratio test; the predeclared seasonal-decomposition method was not run.
  A three-model training comparison and a language-prior effect are not established.
- The fiscal-year (FY) 2003/04 component pilot completed through real downstream reporting with
  controls. The later lineage-v2 continuation verified exact source/document/rule links at fixture scale ([receipt](data/test_evidence/component_pilot_0304_20261115/README.md#lineage-v2-continuation-after-consolidation-2026-11-15)); this does not establish corpus-scale lineage. Its amounts are
  synthetic envelopes, not verified VA financial anchors. No payroll or AI result
  follows from the employee responsibility records.

Use each workstream's status for exact receipts and limitations. The
[audit register](docs/VERIFICATION_AUDIT_REGISTER.md) remains the evidence ledger.
These summaries do not substitute for checking a numerical claim at its source.

## Phases and papers

Phase 1 is direct VA corpus work supporting results-based revisions of Papers 1/2.
Phase 2 is vendor-centered synthetic work, likely informing Papers 3/4 on improved
cost curves and AI models. Phase 3 expands the business-process topology from
allocations to reclass. Further versioned data design may precede formal protocol
freeze; the [current strategy](docs/SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md) owns
that sequence. Paper writing proceeds independently in the private member repository.

Track A's external insurer material is intended for the paper's conclusion;
it is not local corroborating evidence until a qualified result is supplied.
Berka/Track C is retained as a possible exploratory footnote with unknown source
lineage and a failed locked protocol, outside the main evidence path.

## How to collaborate or challenge the approach

After this overview, start with [Track D](tracks/D-experiment/STATUS.md), then the workstream relevant to your task. The
[contributor guide](CONTRIBUTOR_ONBOARDING.md) explains the implementation and
reading discipline. Preserve a failed test or alternative explanation so another
researcher can examine it. The archives retain the fuller development of the
argument; agreement with the thesis is not a condition for contributing.

The [original overview](docs/archive/collaborator_review_2026-11-15/LEDGER_LAB_OVERVIEW.md)
is retained as research history. It stated several expected results as findings;
this overview distinguishes the motivating claims from evidence collected so far.
