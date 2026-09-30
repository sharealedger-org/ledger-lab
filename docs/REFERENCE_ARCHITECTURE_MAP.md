# Reference Architecture Map

This map grounds Ledger Lab verification in two external references:

- [Balancing Act: Financial Systems textbook](https://ledgerlearning.com/books/balancing-act-financial-systems-textbook/on-line-balancing-act-text-book/)
- [GenevaERS Performance Engine architecture](https://github.com/genevaers/Performance-Engine/blob/docs/architecture-and-postscript/ARCHITECTURE.md)
- [Original Universal Ledger prototype](https://github.com/KipTwitchell/universal_ledger)

These references explain the architecture being studied. They do not prove that the current Ledger Lab code implements it. Code, consumers, controls, and measured runs remain the evidence source.

The unpublished monograph papers in the local TDS-AI-Models workspace are also internal design
references. Their ideas are paraphrased below; their wording, unpublished results, and claims are
not to be quoted or presented as public evidence.

## Repository Boundaries

Ledger Lab is the research measurement harness. Its purpose is to test the minimum-cost
architecture with named workloads, public/synthetic fixtures, materialization profiles, controls,
and reproducible cost evidence. Its Scala code is a prototype and baseline, not a generalized
GenevaERS implementation.

The generalized execution target is the [GenevaERS organization](https://github.com/genevaers):

- [Workbench](https://github.com/genevaers/Workbench) authors metadata and views;
- [Run-Control-Apps](https://github.com/genevaers/Run-Control-Apps) builds run control;
- [Performance-Engine](https://github.com/genevaers/Performance-Engine) supplies the execution engine;
- [Performance-Engine-Extensions](https://github.com/genevaers/Performance-Engine-Extensions) supplies research and CKB extensions;
- [DevOps](https://github.com/genevaers/DevOps), [Demo](https://github.com/genevaers/Demo), and documentation support operation and validation.

Sharealedger is the intended metadata/configuration home for the open ERP model. Ledger Lab may
produce workload definitions and evidence consumed by that future configuration, but it does not
own the ERP runtime or the generalized engine.

Potential runtime limitations belong in [GENEVAERS_CAPABILITY_GAP_REGISTER.md](GENEVAERS_CAPABILITY_GAP_REGISTER.md), not in ad hoc Ledger Lab helpers. The first registered example is deterministic unique identifier generation for newly created contracts or arrangements.

## Universal Ledger Prototype as a Navigation Bridge

The original `universal_ledger` repository is a historical domain prototype, not a generalized
engine. Its stated proof sequence is the bridge for auditing Ledger Lab and later translating its
workloads into GenevaERS:

1. assign raw Virginia transactions to stable involved-party/instrument IDs;
2. generate Universal Journal entries with a simple balanced posting model;
3. update instrument-level balances using Common Key Buffering concepts;
4. produce multiple outputs from a single pass over the data.

Use its `VA_periodic_update`, data, scripts, and Scala modules to identify intended financial
behavior and historical naming. The old `SAFRonSpark`, Play, Derby, PostgreSQL, and Vagrant pieces
are historical prototype infrastructure, not Ledger Lab experiment substrates. Spark is not a
substitute for CKB: it does not provide the required multi-file common-key co-location contract
or preserve pre-sorted input order without repartitioning/resorting. PostgreSQL may be used later
as an optional landing/access layer for event and view copies, outside the measured CKB path.
Then verify each retained behavior against the actual producer, consumer, schema, sort order, CKB
boundary, and evidence contract.

## Core Correspondence

| Reference principle | Ledger Lab verification question |
|---|---|
| Business events are richer than accounting classifications | Does source/SJE lineage retain event identity and attributes that are not present in the ledger balance? |
| Journal events can support multiple views | Can the same canonical event/detail basis produce GL, instrument, finance, risk, and operational views without independent balance stores? |
| Balances retain computational investment | Is each balance reproducible from supporting events, and is the cost of maintaining that investment measured separately from report-time derivation? |
| Arrangement Ledger summarizes at arrangement/account grain | Is the instrument/arrangement ID preserved while account and other posting dimensions form the balance key? |
| SAL stores descriptive attributes once with effective dates | Are attributes normalized, historically preserved, joined by instrument ID, and not copied into every balance permutation? |
| Select, sort, summarize is a deliberate processing pattern | Does each sort have a declared key, scope, spill/materialization reason, and downstream consumer? |
| Reference-file phase builds only the data needed by selected views | Does a view workload identify required reference fields and measure reference preparation separately from event execution? |
| Piping passes records between view sections without unnecessary disk I/O | Can a declared producer/consumer path show records moving through a bounded stream or buffer, rather than an orphaned file? |
| Tokens are bounded same-thread working records | Are temporary generated records bounded and dependency-safe, with no hidden feedback loop or unbounded buffer growth? |
| Common Key Data Buffering joins related sorted records in memory | Does the implementation prove common-key order, bounded per-key state, and correct flush behavior? |
| Paired write exits can insert virtual records into the current key buffer | Do generated events enter the current CKB flow where downstream sections can consume them, or is a new physical pass explicitly required? |
| Sorted master files preserve merge/update capability | Are persistent outputs written in the sort order required by their next consumer, avoiding gratuitous resorting? |
| Calculation-engine output may be retained or combined at report time | Is the decision to apply generated events to canonical state versus derive them for a report declared and measured? |
| Reporting is select/sort/summarize/format, not ledger mutation | Does the perspective path remain aggregation-only and reconcile to canonical state? |
| Order of operations controls scale | Does selection happen before expensive sort/aggregation where the architecture permits, and is that order visible in evidence? |
| Production systems separate reference preparation, event execution, and output formatting | Are those phases distinct in process metrics even when they share a physical process or CKB stream? |

## Internal Research Framework

The papers sharpen the repository's research questions into additional verification obligations:

- Treat posting as a projection from a richer event history to a narrower balance representation. Audit whether the projection is injective for the questions the workload claims to answer; a balanced result alone does not establish information preservation.
- Treat the cost curve as a surface over compute, retained balance/master-file inventory, storage, reconciliation, query/report work, and engine capability. The location of a minimum is workload- and engine-dependent; it cannot be assumed from a single curve point or a fixed materialization recipe.
- Treat the Instrument ID as the stable quantitative pivot and the effective-dated CAR/SAL as the normalized attribute reservoir. Verify both sides: balances retain the pivot and attributes preserve history without being copied into every balance permutation.
- Treat output completeness as a separate claim from storage reduction. An architecture is not successful merely because it stores fewer rows; it must answer the declared reporting and analytical capability set, including dimensions that were not anticipated when an event was posted.
- Treat pre-hierarchy event preservation as an information ceiling for later analytics. AI and advanced analytics are a motivation for preserving event, sequence, contract, and causal context, but no model-performance claim is accepted without a defined baseline and experiment.
- Treat generated calculations as a controlled choice among canonical apply-back, piped/virtual records, persisted partitions, and report-time derivation. The choice must be explicit because it changes both accounting meaning and measured cost.

The later papers add further operational gates:

- **Temporal views:** every classification change must distinguish Switch (as recorded), Recast (as of a selected current/reference date), and Reclass (an explicit balanced transfer event). Event date and system-arrival date must not be conflated.
- **Effective-dated reference state:** reference changes are append-only, interval-valid, non-overlapping, and auditable. Pre-flight checks must reject orphaned hierarchy nodes, cycles, incomplete mappings, and temporal overlaps before promotion.
- **Operational integrity:** manual adjustments require an instrument anchor or explicit suspense arrangement; technical rejects and accounting rejects follow different quarantine/recycling paths; the mainline batch must not abort because one event is unmapped.
- **Reconciliation:** reconciliation is a transition control and diagnostic mechanism, not a permanent substitute for shared canonical state. When independent books remain, controls must trace differences to source event, instrument, rule, and process identities.
- **Migration:** requirements are discovered from actual data, code paths, controls, and certified outputs. A shadow ledger must run in parallel and prove zero drift across entities, accounts, periods, and materialization profiles before a legacy master is retired.
- **Shared state:** inter-enterprise sharing is selective. Mutually agreed event/position state may be shared while proprietary classifications and internal offsets remain private; generic blockchain consensus is not a substitute for an accounting ledger, position engine, or control framework.

## What the Textbook Adds to the Audit

The Universal Ledger/SAFR lineage is not simply “read a file, run accounting rules, write a ledger.” It is a processing architecture that manages where information is retained, where it is summarized, how sorted streams are joined, and when derived records are allowed to exist only in memory.

The audit must therefore inspect more than financial totals:

- event detail lost or preserved at each boundary;
- the exact arrangement/instrument and accounting-code-block keys;
- effective-dated SAL/reference behavior;
- record types and filters in shared streams;
- reference preparation and lookup memory;
- generated-record insertion, consumption, and dependency order;
- sorted output guarantees for the next run;
- disk I/O, spill, buffer, and thread contention costs;
- whether a calculation result is canonical, piped, tokenized, or report-time only.

## What the Textbook Does Not Establish

The prose and historical architecture establish intended patterns and useful comparators. They do not establish:

- that the current Scala implementation preserves every field or key;
- that the current sort comparator matches every consumer;
- that the current CKB loop is bounded or correctly flushed;
- that legacy VA modules implement generalized engines or perspectives;
- that a fixture result scales to 100 million transactions;
- that any reported cost curve is comparable until metric production is independently verified.

Those remain executable audit claims in [VERIFICATION_AUDIT_REGISTER.md](VERIFICATION_AUDIT_REGISTER.md).
