# CKB Layer Execution Model

The numbered repository layers are logical ownership boundaries. They are not a promise that each layer causes a separate pass over the data.

## Core Rule

A CKB process continues as one ordered execution flow until an operation requires a new sort order that the current stream no longer provides.

```text
logical layer boundary != automatic data pass
sort boundary        == required CKB pass break
```

CKB can execute work from multiple logical layers in one ordered flow, and a CKB run may be
composed of multiple bounded compiled Scala processes. A process may read a record, resolve
attributes, generate a new SJE, pipe that SJE into a downstream process, and select it for
additional work without loading the full history into one JVM.

The process topology is deliberately not one monolithic Scala application. A 100-million-row
input must not require one JVM to retain unbounded intermediate state. Bash owns process launch,
ordering, admission, and pipe/partition wiring; each Scala process owns its financial logic and
has a declared memory envelope.

## Normal Flow

```text
Raw source/product records
        |
        v
01 Transformation / ARE
        |
        v
sort required to establish the CKB key
        |
        v
02 Foundation / CKB
  read sorted partitions A, B, C
        update in-flight balances and attributes
        pipe generated records and view inputs downstream
        |
        +--> 03 Instrument Ledger views/state
        |
        +--> 04 Engine: read SJE structure
        |       emit generated SJE records into the pipe
        |          |
        |          +--> select and apply in the current CKB flow
        |
        +--> 05 Perspective / terminal output stage
                select generated SJE and updated ledger balances
                aggregate requested views
                write required view and next-run ledger outputs
```

This is a logical flow, not a fixed sequence of physical files or independent scans.

## Record Grains and Shared CKB Code

The CKB path supports both instrument-grain and GL-grain records. Instrument-grain records carry an
Instrument ID and preserve vendor or other instrument-level attribution. GL-grain records are
aggregated balances from systems whose costs are not tracked to an instrument or whose value does
not justify that detail; they may have no Instrument ID.

These are two record grains in the same CKB process, not separate engines or a new execution model.
The same CKB and elimination-view code handles both. Source configuration selects which partitions
and views apply, and records retain their actual identity and grain rather than receiving synthetic
Instrument IDs. Memory and ordering constraints remain declared and measured for the selected
workload; a different grain does not by itself justify forking the code.

## Revised 02-05 Flow

The practical 02-05 topology is an ordered CKB flow with canonical state, engine sections, and
perspective views sharing the same record movement:

```text
A_t: today's ARE SJEs --------------------+
B_t: prior Instrument Ledger state -------+|
C_t: CAR, rates, rules, divisors --------+||
                                          vvv
                              02 Foundation / CKB
                              ordered stream + bounded in-flight state
                                          |
                 +------------------------+-------------------------+
                 |                        |                         |
                 v                        v                         v
       03 Instrument Ledger       04 Engine/view sections       05 Perspectives
       update in-flight state     allocation, FX,                select generated SJE
       preserve SJE lineage       revaluation, candidates        and updated balances,
                                  and generated SJEs              summarize, report
                 |                        |                         |
                 +---------- all records/state remain in CKB flow --+
                                          |
                         terminal outputs written in required order
```

The ownership boundaries are semantic inside this flow:

| Concern | Owner | Output choice |
|---|---|---|
| Sort, buffer, spill, partition order | `02-foundation` | Stream or substantiated next-pass partition |
| Canonical balances and SJE replay | `03-instrument-ledger` | In-flight state; persisted by the terminal stage |
| History/rule-dependent generated events or candidate views | `04-engines` | In-flow SJE or candidate records |
| Reporting scope, CAR pivots, presentation-only translation, inter-unit elimination sums, summarization | `05-perspectives` | Selects in-flow records and writes requested outputs at the terminal stage |

### Terminal Output Boundary

After the initial CKB inputs are established, engine sections do not write generated-SJE output
files and the Instrument Ledger is not written as an intermediate result. Engine-generated SJE,
in-flight Instrument Ledger balance updates, CAR/rule data, and view inputs are piped through the
ordered flow to its final perspective/output stage.

That final stage receives generated SJE and updated in-flight ledger balances as ordered streams.
Each requested view selects the records it needs and performs its declared joins and aggregation.
As ordered output records become ready, the stage writes them sequentially to the required
perspective outputs and updated Instrument Ledger files or partitions for the next run; it does not
accumulate the complete output in memory first. Every persisted output follows the order required by
its declared next consumer. "Updated" ledger state before this boundary means in-flight state;
durable ledger files are produced only at the terminal stage.

Thus an engine-generated SJE can be selected by downstream views in the same CKB flow without an
intermediate file or a new scan. A declared scratch spill or sort may still be needed to satisfy a
bounded-memory or ordering contract, but it is not an intermediate durable result. A new physical
pass is required only when a required output order, memory bound, or restart contract cannot be
satisfied by the current flow.

## Run Attempt and Restart Contract

The CKB recovery unit is the whole ordered CKB process, not an engine section, instrument range,
or completed prefix of the stream. Each attempt starts from the same declared, durable input
partitions: today's ARE SJE, prior Instrument Ledger state, and the required CAR/rate/rule/side-input
generations. CKB does not persist intermediate state for restart. Instrument-break buffers and sort
or overflow spills are working state only, not checkpoints.

The attempt writes terminal-stage outputs sequentially as records become ready, but those outputs
belong exclusively to that attempt until the full process completes and its controls pass. The
orchestrator then makes the new ledger and perspective generations eligible as inputs to a later
run. If the process fails, the attempt's new output generations are deleted or uncataloged; the
previous committed generations and immutable inputs remain untouched. A retry gets a new attempt
identity and replays the whole CKB flow from the same input generations. It must not resume from
partial terminal files or silently skip completed engine/view sections.

Failure logs and resource evidence survive cleanup and identify the failed attempt. Scratch spill
files are cleaned separately and are never promoted to durable run inputs. On a GDG-backed runtime,
cleanup is scoped to generations allocated by that attempt; on other runtimes, the execution adapter
must provide equivalent attempt isolation. In either case, no partial output may resolve as the
current Instrument Ledger or as a valid perspective result.

## Instrument Ledger Detail and Daily Movement Profiles

Instrument Ledger storage grain is a workload choice. A low-volume industry, such as insurance or
annual-renewal payments, may preserve individual business events in the Instrument Ledger. A
high-volume industry may instead summarize a day's activity into movements at the declared
instrument/balance grain, while retaining source events and SJE lineage for event-level traceability.

For the high-volume profile, CKB updates in-flight Instrument Ledger state at each Instrument ID
break:

1. The ordered input must make each Instrument ID and its declared balance keys contiguous.
2. While processing one instrument, retain only that instrument's existing ledger state and
   per-balance-key daily movement accumulators. Consume SJE/movement records incrementally; do not
   hold other instruments or the day's full transaction set in memory.
3. At the instrument break, apply the collapsed daily movement to that instrument's in-flight ledger
   state and emit one updated row per resulting balance key, replacing prior rows rather than
   appending duplicate balances for each source transaction.
4. Pipe those updated rows onward with the generated/source SJE stream. The terminal perspective
   stage selects either event records or updated balances and writes each ordered output sequentially
   as it becomes ready.
5. Release the instrument-local state before the next Instrument ID; do not retain prior instruments or the complete output in memory.

The break key, balance grain, buffer bound, collapse rule, and lineage from each summarized movement
to its contributing events must be explicit. Memory is bounded by the current instrument's required
ledger state and balance-key accumulators, not by total input volume. If one instrument's working
set exceeds the bound, a declared scratch spill must preserve the same replacement semantics and be
measured. The event-preserving profile does not apply this daily collapse; both profiles retain the
same terminal durable-write boundary.

## Sort Boundaries

### Required Initial Sort

The ordinary first hard boundary occurs after the ARE. The ARE receives source/product records in source-system order and emits SJEs. Those SJEs must be sorted on the CKB posting key before the CKB process begins.

The external merge-sort is therefore a real job boundary because it changes the physical order contract. The resulting sorted SJE partition is a Foundation input to CKB.

The first sort has a specific scope: it orders **today's ARE output** on the CKB posting key. It
does not sort the full historical ledger. That distinction matters when comparing it with later
view work.

### Conditional Later Sort

A later sort is required only when a downstream operation needs an order that the current CKB stream does not provide and the required working state cannot be retained within the available memory contract. Required next-run output order is part of the terminal output contract; intermediate CKB sections do not persist results to establish it.

For example:

```text
ordered perspective extract
        |
        +--> aggregate in memory if bounded
        |
        `--> spill and sort if grouping state overflows memory
```

This is not automatically a new layer pass. It is a conditional materialization and sort boundary determined by data volume, grouping cardinality, output order, and the scale-invariant memory contract.

## Extract-Time Summarization

CKB views can summarize while the ordered stream is being processed. The view allocates a bounded
aggregation buffer based on the declared memory envelope and collapses records by the view's group
key, normally applying a sum or another associative metric:

```text
sorted CKB stream
        -> bounded key -> aggregate buffer
        -> final perspective/output stage emits groups when the stream completes

buffer overflow
        -> flush partial aggregates to declared scratch spill, not raw input rows
        -> continue the stream with the bounded buffer
        -> sort/reduce only the much smaller partial-aggregate file
```

When the final grouped output fits in the buffer, no later sort is required. When it does not, the
overflow file contains at most one partial row per observed group per buffer flush, so its size can
be dramatically smaller than the source stream. The final sort/sum is conditional and operates on
that reduced file. This is an adaptive CKB view strategy, not a full-history in-memory aggregation
and not an automatic new architectural pass.

The view must declare its grouping key, associative metric, buffer capacity, spill format, and
overflow reduction behavior. It must also record whether the result was completed in memory or
required partial aggregation and final reduction. This makes the buffer size itself an experiment
variable in the materialization frontier.

The conditional sort in this technique has a different scope from the ARE sort: it orders only the
reduced partial aggregates emitted by an overflowing view buffer. Its input is not today's raw
transaction stream and not the full ledger history. A later sort may also substantiate a view for a
downstream CKB section, but that sort is charged to that view's new order contract.

## Prior-State Divisors

An allocation does not need to calculate a same-day global divisor by rescanning the current
period. It may read the prior period's driver balances from a separate time partition B and apply
the resulting divisor to the current period's source pool in partition C. The divisor is not at the
full instrument key: it is keyed by an allocation group such as effective period, receiver agency,
driver account, and rule version. The driver numerators or receiver weights still need to be
available at the receiver/instrument grain when costs are allocated to instruments.

Therefore B is an auxiliary side input to the allocation section, not automatically a primary CKB
stream input. If the divisor and driver lookup tables fit the declared memory envelope, the section
may load them as bounded maps keyed by allocation group and receiver. Otherwise, materialize and
externally sort them by that group key, then perform a group-key merge/join with the instrument-level
current source stream. Neither option requires a second scan of the current period, but the latter
does create a real sort/materialization boundary that must be recorded.

This is an intentional management approximation: the divisor is lagged by one period and must be
recorded with its effective period, partition-B identity, allocation-group key, rule identity, and rounding residual. The
tradeoff is acceptable when the allocation policy permits prior-period drivers; it is not a license
to substitute stale state where the business rule requires same-day weights. A same-day divisor is a
new materialization requirement unless it can be produced from state already present in the current
ordered flow.

The temporal handoff is explicit:

```text
B_t (prior driver side input at receiver/group grain) + C_t (today's instrument source pool)
        -> D_t (today's generated allocation SJE output)
        -> C_(t+1) (tomorrow's source pool partition)
```

`D_t` is a first-class generated record set with lineage, rule identity, effective date, and
balancing controls. It remains in the current CKB flow until the terminal output stage, which
materializes it for tomorrow only when required. This time-partition handoff is not a second scan of
today's `B_t + C_t` input. A new physical pass is required only if the next day's consumer needs a
new sort order or another declared materialization contract.

## Canonical State Versus Analytical Output

Not every engine result belongs in the canonical ledger. The persistence decision follows whether
the result changes accounting state or merely explains it:

| Result | Store as canonical state? | Reason |
|---|---|---|
| Source event | Yes | Authoritative input and replay lineage. |
| ARE-generated SJE and offset | Yes | Accounting event required to reconstruct balances. |
| Allocation-generated SJE | Yes | Changes instrument balances and becomes a future source partition. |
| Currency conversion/revaluation SJE | Conditional | Store it when book-currency state or a close requires it; otherwise derive the presentation amount from the source event, retained rate history, and rule at report time. |
| Prior divisor and driver side input | Yes, as a partition or snapshot | Required to reproduce the allocation decision and next-period handoff. |
| Intra-unit ARE elimination SJE | Yes when required by the accounting rule | Generated when the source transaction identifies the eligible counterparty; no opposite-side transaction match is required. |
| Inter-unit elimination view | No | A source-selected, report-scope sum that reports residual uneliminated activity. |
| Consolidated financial statement | No canonical copy required | It is a perspective that can be regenerated from ledger plus approved events. |
| Dashboard or reporting aggregate | No canonical copy required | It is analytical output; retain a cache only when operationally useful. |

Elimination has two distinct business processes, while retaining the same CKB execution and view
code across instrument-grain and GL-grain sources:

```text
intra-unit source event + identified counterparty
        -> ARE generates balanced elimination SJE during translation
        -> SJE enters the common sorted CKB flow

inter-unit source records + requested reporting scope
        -> source-selected elimination view
        -> sum activity for that scope and report uneliminated residuals
```

Record grain and elimination scope are independent. The same view code may process instrument-grain
or GL-grain records, and source configuration may enable the view only for applicable partitions.
No one-to-one transaction pair is a prerequisite for the inter-unit report-time sum.

The legacy VA Step 7 module is a separate historical implementation: it matches posted balances
using `InteragencyTransfers.csv` or a same-amount heuristic, then generates and posts reversal SJEs.
That behavior documents the VA workload; it does not define the target CKB contract.

The default materialization policy is therefore:

```text
retain immutable source facts + effective rules/rates + elimination rules + source-grain metadata
        |
        +--> derive at report time when output is presentation-only
        |
        `--> materialize a balanced SJE when state, close, replay, or a downstream engine requires it
```

Report-time generation is valid only when the retained facts are sufficient to reproduce the same
result: source event identity, effective-dated rate or counterparty rule, rule version, grouping
key, and rounding policy must all be available. A report-time adjustment is still an auditable
calculation; it is simply not part of the canonical ledger state.

## Pipes and Spawned Records

A CKB section or compiled Scala process emits records into a pipe for a later section. Bash
connects the processes and enforces their declared order; the processes do not exchange unbounded
in-memory collections. These are in-flight records, not intermediate durable outputs. The emitted
record can be:

- a generated SJE for FTP, reserve, revaluation, allocation, consolidation, or elimination;
- a divisor or trigger record for a later allocation section;
- a selected instrument-ledger view record;
- a perspective aggregation input;
- an audit or reconciliation record.

The generated record remains a first-class record with source lineage, rule identity, effective
date, and balancing information. It continues through CKB so the terminal perspective stage can
select it alongside updated in-flight Instrument Ledger balances. Only that terminal stage writes
required perspective outputs and next-run ledger partitions/files. Generation by another logical
layer or process does not create a physical pass; a new pass is still required when a new sort order
or required materialization boundary occurs.

## CKB Views for Allocation, Currency, and Elimination

Allocation, currency treatment, and intercompany elimination analysis are not automatically
separate pipeline passes. They can be sections of the same ordered CKB process, each reading the
current record plus its declared side inputs and writing a view or generated record into the CKB
buffer:

| CKB section/view | Inputs | In-flow output to terminal stage |
|---|---|---|
| Allocation | current source pool, prior driver/divisor partition, allocation rules | receiver allocations and generated allocation SJEs |
| Currency translation | source amount/currency, effective rate table, report date | translated report view rows |
| Intra-unit elimination | source event, identified counterparty, ARE rules | balanced elimination SJE in the common CKB flow |
| Inter-unit elimination view | configured source partition at either grain, requested reporting scope | sum-based report of eliminated activity and remaining residuals |

These sections may execute in one CKB flow and pipe different output contracts downstream. They do
not require a separate engine for each record grain. They do not persist intermediate results. The
final perspective/output stage selects generated SJE and updated ledger balances, then writes the
requested view and next-run ledger outputs in their declared order. A new physical pass is introduced
only by a required sort, unbounded materialization, or a different input-order contract.

## Materialization as a View Contract

Durable view outputs and updated ledger files/partitions are written only by the terminal
perspective/output stage. Earlier sort or overflow files, when required, are declared scratch
artifacts and are not intermediate next-run outputs.

Every materialization is an additional named view of the CKB flow, whether it is retained as a file,
kept as a next-pass partition, or streamed through a pipe. Examples include:

- an SJE view for generated accounting events;
- a divisor or driver view for the next allocation period;
- a translated-currency view for a report date;
- an intercompany-candidate view containing paired and unmatched sides;
- a ledger view after apply-back;
- a sorted partition substantiated for a downstream CKB section.

Persisting one of these views creates storage and reconciliation obligations, but it does not by
itself create another logical process or financial layer. The physical cost boundary is recorded
when the view must be materialized, sorted, spilled, or replayed as input to a later pass.

## Process Memory Contract

Each compiled Scala process must declare:

- input partition contract;
- output stream or partition contract;
- sort order, if any;
- Instrument Ledger output grain and instrument-break key, when daily movement summarization is used;
- maximum in-memory grouping/buffer state;
- maximum rows or bytes retained for the current instrument break;
- spill behavior and spill directory;
- expected row and byte counters;
- process-level resource metrics.

The contract is what makes memory predictable. Piping bounds the lifetime of intermediate records;
partition files bound restart and replay scope; neither approach requires a single JVM to retain
100 million input rows.

## Layer Responsibilities in One CKB Process

| Layer | Logical responsibility | May execute inside current CKB flow? |
|---|---|---|
| `01-transformation` | Source normalization and ARE SJE generation | Yes, before the initial sort; generated output feeds sorted CKB input |
| `02-foundation` | Sort contract, CKB mechanics, state application, controls | Yes, the core ordered process |
| `03-instrument-ledger` | Canonical instrument state and ledger views | Yes, as CKB sections or piped outputs |
| `04-engines` | Read SJE structures and generate SJE structures | Yes, through pipes and spawned records |
| `05-perspectives` | Select, step up, aggregate, and emit outputs | Yes, while grouping state fits the memory contract |

## Engine and Perspective Distinction

Engines consume and produce SJE structures. Their output partitions may be piped back into Foundation CKB sections for application.

Perspectives aggregate only. They may select records, resolve CAR attributes, calculate presentation fields, and aggregate outputs, but they do not generate or apply accounting transactions.

The distinction is semantic, not a claim about separate physical passes.

## Cost Experiment Implications

The agentic cost experiments must measure both logical work and physical boundaries:

- number of CKB processes;
- number and location of external sorts;
- records read and written per sorted partition;
- records spawned and piped in memory;
- maximum grouping state or spill volume;
- generated SJE volume by engine;
- Instrument Ledger input/output rows and event-to-movement reduction by storage profile;
- peak per-instrument break buffer rows/bytes;
- perspective output volume;
- replacement master and pivot storage;
- elapsed time and memory pressure.

A design with more logical engines may still be a single CKB pass. A design with fewer logical layers may require multiple passes if it introduces extra sorts.

The cost curve must therefore count **sort/materialization boundaries**, not simply count architectural layers.

The complete run, process, partition, sort, SJE, perspective, and reconciliation logging contract
is defined in [MEASUREMENT_AND_LOGGING.md](MEASUREMENT_AND_LOGGING.md).
