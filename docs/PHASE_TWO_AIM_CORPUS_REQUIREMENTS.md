# Synthetic corpus requirements for AIM inquiry and outcome evaluation

2026-11-16. Status: `design_only`. Owner direction and proposed experimental
requirements for Phase 2 preparation, with future Phase 3 consumers. No new
corpus, AIM model, forecasting engine or target-runtime capability is implemented
by this document. Phase 1 edits of Papers 1/2 proceed independently using their
[reporting evidence addendum](tds-evidence/reporting-20261116-paper-updates/README.md).

This preparation has not included extensive design of the envisioned AIM models
or their features. Their architectures, feature representations, learning
objectives and training procedures remain undetermined. The focus is imagining
what source-data conditions might provide a useful foundation for richer economic
inquiry. Possible functionality motivates the corpus conditions below; it is not
a specification of models already designed or evidence that those conditions
will be sufficient for the desired capabilities.

The synthetic population should support reconstruction of connected economic
activity from historically disconnected process records, followed by evaluation
of tentative outputs against later evidence and realized outcomes. It should also
support conditional projections and probabilistic forecasts under temporary,
future-dated reference and rule versions. These capabilities connect financial
and risk inquiry through shared economic evidence without making scenario
arithmetic itself a probability model.

The owner identifies Papers 5/6 in Phase 3 as a home for the later AIM results,
with a possible addition to Paper 5's engine work. This extends the earlier
Phase 2/Papers 3/4 preparation rather than moving every future engine into the
initial generator. Exact manuscript structure remains with the writing sessions.

## Self contained population and empirical calibration

Build one versioned fictional population containing source events, masters,
reference histories, process-specific identifiers, generation specifications and
controls. Preserve the observed Virginia corpus separately. The completed
[reporting execution](../data/test_evidence/report_sorted_v3_20261116/README.md)
can inform activity distributions and workloads; it does not identify the missing
real actors or prove component relationships. Generated vendor, employee and
revenue-party identities retain distinct roles and namespaces. Preserve controlled
missing identity, ambiguous relationships and legitimate summary-grain records.

A component business model supplies a generating account of an activity spanning
several processes. The simulated operational systems need not know that complete
account: some grew independently, retain incompatible identifiers or classify the
same activity differently. A private generating context ID must not become a
universal join key in the model's evidence. A known private connection is not
necessarily inferable from the exported records.

## Component and process conditions

| Condition | Generated evidence and source behavior | Candidate AIM or engine question | Evidence needed to evaluate it |
|---|---|---|---|
| Connected component with integrated sources | Procurement, delivery, payment, staffing and receipt records share supported business references | What economic activity and obligations does this component represent? | Declared relationships, actual process outputs and independent totals |
| Historically disconnected sources | Local identifiers, different classifications, partial crosswalks, inconsistent names and process-specific dates | Which records can reasonably be connected, and what remains unresolved? | Private relationship truth plus an explicit inferability class |
| Partial modernization | New business references arrive prospectively; earlier history has only incomplete evidence | Which source-system changes improve linkage and inquiry? | Matched scenario variants with the same underlying activity and declared added fields |
| Tentative component output | Proposal, commitment, expected delivery, staffing plan and projected receipts precede actual activity | What was expected, on what evidence, and with what uncertainty? | Versioned proposal, assumptions, forecast issue time and intended outcome measure |
| Later completion or correction | Delivery, invoice, settlement, cancellation, revised responsibility or late reference records arrive afterward | What happened, how did it differ, and which earlier interpretation should change? | Stable lineage to the tentative output; separate effective and availability dates |
| Misleading overlap | Shared location, person name or organizational membership without the hypothesized relationship | Should the model abstain or reject a connection? | Negative and ambiguous cases across independent generation mechanisms |
| Partial or never observed outcome | A commitment is canceled, measurement arrives late or an outcome remains unavailable | Can the model distinguish uncertainty, censoring and failure? | Outcome observation status and evaluation rules that do not score missing as zero |

Include multiple business components and independent background activity; do not
make every employee, payer or expense part of one vendor story. Components may
share participants, divide or merge over time, and produce outputs consumed by
several processes. Preserve many-to-many relationships rather than replacing them
with an always-correct one-to-one linkage.

## Temporal and outcome contracts

Retain the following separately, using explicit versioned fields or sidecars
before choosing executable headers and parsers:

- Economic event/effective time: when an event or business version applies.
- Availability/recorded time: when the evidence became available to the consumer.
- Proposal or forecast issue time and target horizon: when an expectation was made
  and what later period or outcome it concerns.
- Scenario identity, parent baseline and reference/rule versions: which conditional
  world produced the output, including temporary future-dated versions.
- Proposal/output lineage and later resolution: what a completion, correction,
  replacement or cancellation refers to, including partial and disputed matches.
- Outcome occurrence, outcome availability and observation status: what happened,
  when it could be evaluated, and which outcomes remain unknown.

Produce three distinct evaluation views: the evidence available at decision time;
the later interpretation of that same historical activity using subsequently
available evidence; and realized outcomes at a declared observation cutoff. A
recast view may revise interpretation or classification without rewriting the
original evidence available to a historical model. An outcome match is itself
versioned evidence when it depends on inferred linkage.

Interpret “projected recast condition” here as a scenario's proposed interpretation
or classification of economic state. It is not automatically an accounting
reclassification journal. If a policy requires reclass, a separately declared SJE
engine and posting contract must implement it. If it requires only a conditional
view, keep the result analytical.

## Forecasting and risk conditions

| Candidate experiment | Corpus requirements | Conditional calculation and probabilistic question |
|---|---|---|
| Payment and delivery timing | Due dates, expected and realized delivery/payment dates, amendments and partial settlement | Calculate scheduled exposure; estimate delay or noncompletion risk |
| Cash and funding pressure | Obligations, expected receipts, opening state and compatible scope/currency | Project cash under a scenario; estimate likelihood and severity of a shortfall |
| Concentration and shared dependencies | Supported vendor/component/agency links plus common drivers and misleading overlaps | Calculate linked exposure; estimate joint disruption rather than assume independence |
| Future reference change | Temporary hierarchy, ownership, responsibility or rule versions with explicit issue/effective dates | Recast future financial views; evaluate forecast changes against the relevant realized outcomes |
| Stress and regime change | Explicit driver scenarios, probability assumptions where supplied, alternative mechanisms and rare cases | Show conditional losses; separately test calibration and sensitivity of probability estimates |

Scenario weights may be assumptions, generator frequencies or estimated
probabilities; label their source. Executing a balanced stress scenario establishes
its conditional accounting consequence, not its probability or causal realism.
Use realized observations and independently specified mechanisms to assess
probabilistic claims. Rare-event evidence and sample sufficiency constrain which
risk results are defensible.

Financial and risk outputs should refer to the same exposure/event lineage,
cutoff, units, scope and scenario version. Risk measures remain distinct from
accounting balances. A valuation, provision or other risk-related accounting
consequence requires its own policy, balanced SJE rules and actual downstream
posting consumer; a risk score does not silently create a journal.

## Proposed lifecycle inspection case

A fictional public-service component plans delivery, procures a vendor service,
assigns employee responsibility and anticipates a program receipt. Its separate
systems initially lack a complete shared business reference. It emits a tentative
cost, completion date and expected receipt schedule. Later, a partial delivery,
revised invoice, changed employee responsibility and delayed receipt complete or
contradict parts of that view. Some evidence arrives after its effective date.
An unrelated component shares an address to provide a plausible false match.

At the earlier cutoff, ask for the supported obligations, conditional cash view,
uncertain connections and forecast. Then introduce a temporary future hierarchy
and revised delivery assumptions in a separate scenario. After the later evidence
arrives, measure the actual outcome, forecast error and supported explanation of
the differences. Re-run matched variants with a new source-system correlation
field and observation timestamps to measure what those changes enable. Keep the
underlying economic activity fixed in these source-quality comparisons.

This is an inspection case, not a frozen Q2 task or sufficient model evaluation.
Include cases where improved fields do not resolve the question and where a
plausible projection fails under a held-out mechanism.

## Source system implications to test

Treat source-system improvements as experimental variables: stable local record
identity, explicit obligation-to-settlement references, dated participant roles,
versioned corrections, effective/availability timestamps, forecast versions and
resolution links. Compare disconnected, partially connected and integrated
source profiles over matched generated activity. Evaluate both utility and the
additional capture, maintenance, processing and storage requirements.

The resulting evidence could identify which changes are useful prerequisites for
an AIM capability and which are unnecessary. Synthetic results alone do not
establish that an organization can obtain the required fields accurately or
inexpensively; that remains a question for observed deployments.

## Envisioned AIM functionality and possible evaluation

These are functional hypotheses and candidate comparisons to inform data
preparation, not selected model designs, feature sets or a frozen evaluation.

| Prototype capability | Initial comparison | Measures to define before evaluation |
|---|---|---|
| Supported relationship reconstruction | Declared exact/rule linkage, selected current-model baseline and proposed AIM approach | Precision/recall where truth is observable, calibration, supported abstention and false assertions |
| Historical and later-completed inquiry | Exact reconciled query/reference results and matched model inputs | Numerical correctness, evidence support, cutoff violations, coverage and response consistency |
| Tentative-to-realized outcome explanation | Explicit lifecycle matching plus selected model baselines | Resolution accuracy, error attribution, unsupported causal claims and usefulness under a stated rubric |
| Conditional financial projection | Declared deterministic engine/reference outputs | Balance, lineage, scope/currency agreement, reproducibility and measured processing cost |
| Probabilistic forecasting and risk | Selected simple forecasts/risk baselines and matched information sets | Forecast error, proper probability scores where applicable, interval coverage/sharpness and tail uncertainty |

“More useful” must specify the task, user decision and scoring rubric. “Trusted”
means demonstrated properties under stated conditions, including uncertainty and
abstention; it is not a general label earned by balancing arithmetic. Separate
model architecture comparisons on identical evidence from source-quality
comparisons and combined system comparisons.

Envisioned AIM functionality intentionally influences source-condition and corpus
design. Describe that influence in Papers 5/6 without implying that extensive model
or feature design preceded generation; record the resulting scope and selection
bias. Detailed model/feature design and corpus inspection can subsequently inform
each other. Freeze named corpus,
feature, baseline and evaluation versions after exploratory inspection. Reserve
independent mechanism families and context groups, not just shuffled rows or new
seeds from the same recipes. Keep outcome truth out of historical model features;
expose training labels only under the declared training contract. Publication of
an inspection corpus does not make it a blind test set.

## Architecture and implementation boundary

Generation remains source preparation in Scala, with Bash orchestration. Context
order emits process/entity-addressed instructions; determining-key assignment and
entity/effective-date order remain declared sort/merge boundaries. Disconnected
business references may require multiple sorted bridges. A conceptual business
component does not imply that every process shares one execution key or physical
scan. Current context/entity state must remain bounded; declare additional sorts
or materializations where required.

Future financial engines consume canonical SJE/ledger contracts and produce
balanced, traceable SJE structures where accounting is generated. Perspectives
aggregate only. Scenario state is isolated from accepted baseline state, with
explicit parentage and publication/completion status. Later outcome evidence
reaches its declared reconciliation/evaluation consumer through a versioned
resolution contract; merely writing an engine output is insufficient.

Before implementing any new slice, enumerate schemas/parsers, CKB keys and order,
state limits, process/partition boundaries, lineage, temporal selection, controls
and actual downstream consumers under the [architecture gates](AGENTIC_ARCHITECTURE_GATES.md).
Run the unchanged baseline and the cheapest adversarial fixture first. Existing
bounded pilot and full reporting results do not verify these proposed capabilities.
Record any missing target-runtime capability in the
[gap register](GENEVAERS_CAPABILITY_GAP_REGISTER.md) before claiming measured support.

The immediate deliverable is an inspectable, self-contained synthetic sample and
its capability/evidence matrix. Define data families needed for later scenarios
now, but do not require full forecasting, allocation or reclass engines before
initial inspection. Full-scale generation, Q2 protocol freeze and later Phase 3
execution retain separate admission and evidence requirements.
