# Full monograph reading — Codex

Recorded 2026-11-13 07:13:47 AZT (UTC−07:00). CKB-side analysis only; do not deliver to the control.

## Scope

Read the supplied introduction, Papers 1–9, epilogue, navigation guide, vocabulary, and README in `session-run.tmp`, including the papers' references and acknowledgments. Earlier targeted readings are superseded in coverage, not silently upgraded retrospectively. Truncated reads were followed by smaller reads of the missing material. This is a reading of the supplied drafts, not verification of their external citations, confidential production records, accounting-standard interpretations, or current technology comparisons. The private source files remain ignored and unmodified.

## Argument across the collection

| Document | Role in the argument | Implication for Phase 2 |
|---|---|---|
| Introduction | Connects preservation of economic facts, cost, enterprise intelligence, and shared state | Evaluate the connected architecture rather than make one prediction task carry the whole thesis |
| Paper 1 | Tradeoff between recomputation and maintained derived state; instrument identity and reference joins | Measure an actual workload-dependent cost surface, including change and controls |
| Paper 2 | Preserved event context expands the information available for learning and discovery | Separate answerability, discovery, predictive value, and model/input design |
| Paper 3 | Enterprise cycle and consolidation of independently maintained data supply chains | Test whether successive consumers reuse a common controlled foundation |
| Paper 4 | Execution mechanisms: ordered streams, shared lookups, compiled views, generated records | Declare sort, buffering, output, and dependency boundaries; measure the actual path |
| Paper 5 | Accounting interpretation and generated financial activity anchored to instruments | Add a dependent financial process, not just another report |
| Paper 6 | Switch, Recast, Reclass; reference changes as events; late arrivals | Make time and interpretation explicit; distinguish a reporting projection from a booked transfer |
| Paper 7 | Adjustments, rejection/recycling, reference governance, and controls | Include correction and failure handling in both correctness and total cost |
| Paper 8 | Discover semantics through data, financial reconciliation, and iterative exposure | Evaluate AI's ability to form and validate useful questions, including recognizing insufficient evidence |
| Paper 9 | Agreed shared events with separate private accounting interpretations | Future extension: common facts need not imply identical accounting perspectives |
| Epilogue | Event/position foundation as a broader system of business measurement | Treat this as the research horizon, distinct from current demonstrations |

## What changed in my assessment

My earlier assessment concentrated too much on the immediate model comparison. Paper 2 explicitly makes novel discovery its primary hypothesis. Paper 6's iterative development of reference classifications and Paper 8's data-first discovery method explain how that ambition connects to actual financial work. The framework is about retaining the ability to ask and investigate new questions, not merely improving a score on a question selected in advance.

However, the draft itself repeatedly narrows that ambition to predictive superiority and broad claims about language priors. The owner's clarification and the written experimental claims are therefore not yet fully aligned. A discovery study would be a faithful development of the monograph, but its criteria must be declared before observing success. Otherwise every failed prediction could be redescribed as successful discovery after the fact.

The useful unit of evaluation could be a discovery episode: an agent inspects unfamiliar data, proposes a relationship or interpretation, identifies the records and temporal assumptions needed, tests it, and either establishes a useful result or explains why the evidence cannot support it. Novelty alone is insufficient. We should record correctness, provenance, held-out confirmation where applicable, false discoveries, and the cost of preparing and maintaining the necessary data. Human review can assess business usefulness without being substituted for factual validation.

The connection to the cost paper is cumulative. A new classification, a late correction, a rule change, and a new downstream calculation should reveal whether prior work remains reusable or whether independent representations require separate repair. Extra files or indexes are not automatically fragmentation. The relevant distinction is independently maintained economic state and the work required to keep it consistent. Both CKB and control must be assessed by that definition.

## Incidental issues worth resolving before stronger claims

1. **A conditional optimum is sometimes presented as a proved universal minimum.** Paper 1's cost decomposition and opposing tendencies alone do not establish convexity, an interior minimum, or that the instrument ledger occupies it. Its practical sizing method and query-frequency sensitivity are more defensible starting points. Production feasibility is not proof of global cost optimality.
2. **Possible aggregate cells are not required physical records.** The permutation expression counts a dense, fully materialized space under its stated assumptions. It does not show that every alternative must materialize every subtotal. A fine-grained table can support aggregation; sparse populations and functional dependencies matter. CAR storage and maintenance also grow with attributes and version history even when ledger row count stays constant.
3. **Information loss and usefulness need different propositions.** Non-injectivity permits indistinguishable histories. Positive conditional entropy additionally depends on the source distribution. Strict target-information loss requires residual target information conditional on the retained representation, not merely a target mentioning a discarded field. One fitted model is not an information ceiling, and preserved sequences do not by themselves identify causation.
4. **Posting, retaining, and discarding are distinct actions.** A system can retain source events while posting balances. The monograph sometimes treats posting as necessarily destroying the only event copy, while Paper 8 discusses recoverable logs. Tests must inventory the whole permitted information substrate, not just compare one output table from each system.
5. **Historical interpretation and generated transfers need a consistent boundary.** Paper 3 broadly describes reclassification without adjusting entries; Papers 5–6 explicitly require generated entries when a balance bucket changes. Paper 6 also mixes inclusive interval examples with half-open formal intervals. Valid-time classification alone does not reproduce what was known before a late reference correction; that needs the system-time history developed in Paper 7.
6. **Avoid categorical free/zero claims.** Adding a reference dimension can avoid new balance rows while still costing acquisition, validation, storage, joins, aggregation, and output. Sharing a scan removes repeated base reads, not all incremental work. A common source removes some independent-book reconciliation obligations, not the need to verify completeness, mappings, controls, and perspective semantics. Paper 7 itself supplies those controls.
7. **The evidence snapshots differ across papers.** Papers 1–2 include full-corpus results while Papers 3–4 retain fixture/in-progress descriptions. Paper 6 describes Option 9 as unimplemented; the current checkout has an unverified legacy path. Paper 5 contains different allocation benchmarks in different sections and both 100% and 80% coverage language. Paper 4 alternates billions and trillions of joins. These require source reconciliation, not selecting the more impressive number.
8. **Two simple numerical/definition checks.** Papers 3/8 define AHI as stores divided by business lines: one enterprise-wide store implies 1 divided by the number of lines, not generally 1. Paper 9's displayed savings formula is at least 70% for nonnegative reconciliation overhead, not 60–70%; counts of unequal operations do not establish an equal reduction in money or labor.
9. **Comparator and control boundaries remain important.** Modern database, Spark, language-model, and regulatory claims need separate primary-source review before publication. The observed assistant mistakes do not establish what was absent from a model's training corpus. Internal partitioning is not by itself evidence that cross-party agreement, disputes, privacy, and atomic settlement have been solved.

These are review candidates, not edits to the papers or findings about measured engine behavior. No new runtime audit was performed.

## Provisional direction

Continue the papers, while distinguishing demonstrated mechanisms, measured outcomes, conditional theory, and research hypotheses. Build Phase 2 around controlled changes to financial meaning and the cost of supporting newly discovered questions. Retain fixed prediction tests as bounded evidence; do not make their success a prerequisite for every other architectural claim. Keep synthetic demonstrations of capability separate from evidence that useful unknown relationships exist in real data. Q2 questions and acceptance criteria remain unapproved.
