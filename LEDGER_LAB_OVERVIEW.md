# Ledger Lab: The research argument and what tests it

Ledger Lab studies whether a financial architecture can preserve useful business
detail, support new accounting/reporting questions and reduce the total cost of
maintaining and using that information. The central proposal is an instrument
ledger with effective-dated attributes and reusable, ordered financial processing.
The experiment must establish where this helps, where it costs more and what it
cannot answer. [STATUS.md](STATUS.md) gives the current checkpoint.

## The distinction behind the research

Posting accumulates detailed events into balances at a chosen grain. A balance
can omit vendor identity, timing, event sequence or other distinctions needed by
later questions. That does not establish that an institution deleted its raw
history: the question is which retained representation answers a request, and
what reconstruction, maintenance and reconciliation it requires.

An instrument ledger carries identity and declared balance dimensions forward,
then resolves attributes under an explicit date policy. Detailed events remain
available for audit and questions that the balance grain cannot answer. A joined
attribute does not restore an event sequence already absent from that balance.
Quiet instruments, changed hierarchies and late information are part of the
research problem, not incidental edge cases.

The execution proposal is Common Key Buffer (CKB) processing: bring compatible
sorted records, state and reference data together, execute dependent process
sections and let downstream sections consume generated SJEs in the declared flow.
A key need not be vendor ID. Multiple business processes may need their own keys,
CKB flows and connecting sorts. A logical capability need not add a physical scan;
its compute, state, output and any new sort/materialization still have a cost.

The [execution model](docs/CKB_LAYER_EXECUTION_MODEL.md) defines these conditions.
The [reference map](docs/REFERENCE_ARCHITECTURE_MAP.md) and dated reflections
preserve the reasoning and its development. They explain the research position;
measurements and independent controls must test it.

## Two workstreams inside one experiment

[Track D](tracks/D-experiment/STATUS.md) is the main path. CCP and AIM are its
complementary workstreams, formerly B1 and B2.

| Workstream | Question | Evidence required |
|---|---|---|
| [CCP: cost/capability](tracks/D-experiment/ccp/STATUS.md) | How do retained outputs, sorting, state and enabled financial capabilities affect total cost? | Comparable workloads, actual resource/pass/storage measurements, controls and replicates |
| [AIM: AI/exploration](tracks/D-experiment/aim/STATUS.md) | Which distinctions support prediction or reusable financial inquiry, and what is lost at a declared representation? | Matched populations/tasks, valid temporal splits, held-out questions, baselines and explicit uncertainty |

The Mix graph studies the event/balance materialization trade-off; the Process
graph studies added financial capabilities; the Engine graph compares execution
approaches. Their [design](docs/COST_GRAPH_DESIGN.md) contains models as well as
measured constants. A modeled minimum is not an observed universal minimum.
The old CCP/AIM IDs remain useful protocol/evidence identifiers.

## What the recorded work currently supports

- Monthly VA pipelines completed at corpus scale, including a later expanded
  expenditure/revenue/PO workload. Named output sets matched across selected
  executions. Scope, implementation and timing differ between checkpoints.
- The separate expanded reporting corpus attempt failed. Later bounded fixtures
  exercise revised reporting and synthetic connections; they do not establish
  successful corpus reporting or a new cost point.
- AIM3 has task-specific predictive signal. Posted-total/instrument-balance
  baselines and combined features complicate any broad advantage claim; a fiscal-
  period discrepancy leaves the reported incremental gain provisional.
- AIM4 failed its implemented Q4-ratio test; the locked STL method was not run.
  A three-model training comparison and a language-prior effect are not established.
- The FY2003/04 component pilot completed through real downstream reporting with
  controls, while explicit source/rule lineage remains partial. Its amounts are
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

Start with Track D, then the workstream relevant to your task. The
[contributor guide](CONTRIBUTOR_ONBOARDING.md) explains the implementation and
reading discipline. Preserve a failed test or alternative explanation so another
researcher can examine it. The archives retain the fuller development of the
argument; agreement with the thesis is not a condition for contributing.

The [original overview](docs/archive/collaborator_review_2026-11-15/LEDGER_LAB_OVERVIEW.md)
is retained as research history. It stated several expected results as findings;
this overview distinguishes the motivating claims from evidence collected so far.
