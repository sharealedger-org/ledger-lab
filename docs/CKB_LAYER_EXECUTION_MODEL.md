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
  update balances and attributes
  emit views and generated records through pipes
        |
        +--> 03 Instrument Ledger views/state
        |
        +--> 04 Engine: read SJE structure
        |       generate SJE partition in the pipe
        |          |
        |          +--> select and apply in the current CKB flow
        |
        +--> 05 Perspective: aggregate selected records
                |
                +--> emit perspective output
                |
                +--> sort only if the next operation requires
```

This is a logical flow, not a fixed sequence of physical files or independent scans.

## Sort Boundaries

### Required Initial Sort

The ordinary first hard boundary occurs after the ARE. The ARE receives source/product records in source-system order and emits SJEs. Those SJEs must be sorted on the CKB posting key before the CKB process begins.

The external merge-sort is therefore a real job boundary because it changes the physical order contract. The resulting sorted SJE partition is a Foundation input to CKB.

### Conditional Later Sort

A later sort is required only when a downstream operation needs an order that the current CKB stream does not provide and the required working state cannot be retained within the available memory contract.

For example:

```text
ordered perspective extract
        |
        +--> aggregate in memory if bounded
        |
        `--> spill and sort if grouping state overflows memory
```

This is not automatically a new layer pass. It is a conditional materialization and sort boundary determined by data volume, grouping cardinality, output order, and the scale-invariant memory contract.

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

`D_t` is a first-class generated partition with lineage, rule identity, effective date, and
balancing controls. Materializing it for tomorrow is a time-partition handoff, not a second scan of
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
| Elimination candidate match | Evidence record, not ledger state | It is an analytical proposal until the accounting rule approves it. |
| Approved elimination SJE | Conditional | Store it when an approved close or downstream ledger process applies it; otherwise generate the adjustment at consolidated-report time from retained pair evidence and rules. |
| Consolidated financial statement | No canonical copy required | It is a perspective that can be regenerated from ledger plus approved events. |
| Dashboard or reporting aggregate | No canonical copy required | It is analytical output; retain a cache only when operationally useful. |

The elimination flow is therefore:

```text
ledger + counterparty rules
        -> elimination candidate analysis
        -> candidate/reconciliation evidence
        -> approved balanced elimination SJE
        -> canonical ledger/consolidated perspective
```

Candidate analysis may be persisted for audit and interpretation, but it must not silently become
an accounting entry. Approval, rule identity, source lineage, and the zero-sum control are the
boundary between analytical output and stored accounting state.

The default materialization policy is therefore:

```text
retain immutable source facts + effective rules/rates + match evidence
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

A CKB section or compiled Scala process may emit records into a pipe for a later section. Bash
connects the processes and enforces their declared order; the processes do not exchange unbounded
in-memory collections. The emitted record can be:

- a generated SJE for FTP, reserve, revaluation, allocation, consolidation, or elimination;
- a divisor or trigger record for a later allocation section;
- a selected instrument-ledger view record;
- a perspective aggregation input;
- an audit or reconciliation record.

The generated record remains a first-class record with source lineage, rule identity, effective
date, and balancing information. It does not become a new physical pass merely because it was
generated by a different logical layer or process. A new pass is still required when a new sort
order or required materialization boundary occurs.

## Process Memory Contract

Each compiled Scala process must declare:

- input partition contract;
- output stream or partition contract;
- sort order, if any;
- maximum in-memory grouping/buffer state;
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
- perspective output volume;
- replacement master and pivot storage;
- elapsed time and memory pressure.

A design with more logical engines may still be a single CKB pass. A design with fewer logical layers may require multiple passes if it introduces extra sorts.

The cost curve must therefore count **sort/materialization boundaries**, not simply count architectural layers.

The complete run, process, partition, sort, SJE, perspective, and reconciliation logging contract
is defined in [MEASUREMENT_AND_LOGGING.md](MEASUREMENT_AND_LOGGING.md).
