# Member repository editing plan for the introduction and Papers 1 and 2

Prepared 2026-11-14 from the owner's extended discussion with Codex. The deliverable is a coherent private review package for Eric Denna, who may pass it to William E. McCarthy at the owner's request. Revise the monograph 00 introduction and Papers 1 and 2 together. This is an evidence-led revision of an ongoing collaborative research program, not a claim that every proposed capability has been built.

This plan is the current editing handoff. It supersedes older sequencing that made all Phase 1b engineering a prerequisite to revision. Bob's synthetic generation can proceed independently on Intel; expanded reporting repair, broad native-AI implementation and formal Phase 2 are not required for this review package. Do not close research tracks or discard unfinished work. This document does not authorize sending the package to reviewers.

## Destination and source discipline

**Destination confirmed at handoff:** sibling repository `members`, directory `research/`. Read its `PAPER_EDIT_REVIEW_PROTOCOL.md` first. It identifies members as the active editing copy and TDS `03-argument/` as the pre-edit review baseline. Papers 00–02 are already staged as baseline synchronization; preserve those changes and establish the baseline commit before editing. Do not assume this handoff committed the members repository. Bob will categorize changes against TDS, flag empirical claims and softened caveats, and prepare an accept/reject/review list. Owner approval precedes propagation of accepted edits back to TDS. The current editing scope is only 00–02; flag navigation/bibliography changes outside those files as follow-up rather than modifying other papers. This confirmed protocol takes precedence over the generic discovery instructions below.

Edit the authoritative member-repository sources in the editing session, after reading that repository's instructions and status. Confirm its actual root and current filenames from the owner-selected workspace; do not assume the historic TDS path `/Users/kiptwitchell/Workspace/GHEGenevaERS/TDS-AI-Models` is the current member checkout. Locate the introduction and Papers 1/2 with repository search before proposing edits. The ignored copies in this experiment's `session-run.tmp` are reading references, not authoritative editing targets.

Keep private monograph prose in the member repository. This experiment repository supplies evidence and handoff instructions; do not copy private chapters into it. Preserve previous versions and record the new evidence cutoff and revision identity. Respect existing authorship conventions; describe this as collaborative work while attributing foundational scholarship and practitioner contributions accurately.

Read the current 00 introduction in full before editing. Its known sections include the central argument, Metric Engine preface, historical genesis, architectural context, nine-paper overview, shared empirical laboratory and conclusion. The current source may differ from the previously read copy. Read Papers 1/2 completely and inspect the relevant paper summaries, bibliography and navigation guide. Limit other-paper changes to necessary cross-reference corrections; substantial revision of Papers 3–9 is a separate task.

## Establish the evidence and lineage

Build one shared claim–evidence table before drafting numerical conclusions. For each proposed claim include its destination, original wording, source artifact/revision, population, period, units, actual timing boundary, maturity, limitation and revised wording. Distinguish observed data, derived accounting, model assumptions, proposed capabilities and practitioner accounts. Reuse recorded evidence; resolve questionable figures at their producers rather than multiplying summaries.

Important source groups:

- Original 2018 paper, *A Proposed Approach to Minimum Costs for Financial System Data Maintenance*: https://ledgerlearning.com/whitepapers/minimum-costs-for-financial-system-data-maintenance/ . The owner corrected the earlier January 2019 recollection to 2018. The cost framework predates these experiments.
- *Balancing Act*: https://ledgerlearning.com/books/balancing-act-financial-systems-textbook/on-line-balancing-act-text-book/ . The current selective scan covered the Preface, chapters 3, 6, 8, 52, 58, 62 and Acknowledgements. Do not call it a complete reading. Chapter 6 documents the fall 1988 first class with Denna and Denna's McCarthy connection; chapter 3 already articulates quantitative search; chapter 8 distinguishes operational events from their financial consequences; chapters 52/58/62 develop execution, arrangement detail and reporting.
- The owner states the 2018 paper honored Denna's first class, which he attended. Treat that dedication history as owner testimony unless a documentary dedication is located. Consider a concise cover-note acknowledgment rather than making personal history carry empirical claims.
- McCarthy's REA, Denna and colleagues' REAL work, and subsequent REA scholarship. The AAA description of McCarthy/Geerts/Gal's 2022 monograph has been checked, not its full text: https://aaahqbookstore.org/catalog/book/rea-accounting-model-accounting-and-economic-ontology . Obtain accessible author manuscripts/repository copies where possible. Distinguish actual reading from abstracts or descriptions and retain unresolved literature gaps.
- Experiment `STATUS.md`, B1/B2/D track status, `docs/CURRENT_RESULTS.md`, `docs/COMPANION_BASELINE_RECORD_2026-11-12.md`, relevant audit entries, original baseline archives and the three expanded-source archives `run_20261113_155156`, `run_20261113_182856`, `run_20261113_210544`. Queue/hash receipts are under `data/test_evidence/intel_preflight_20261113/`; separate reporting failure under `data/test_evidence/reporting_corpus_20261114/`.
- The earlier `docs/tds-evidence/paper-1-rev2-ledger-lab/` package is a starting artifact, not a current unquestioned source. It contains older expenditure-only figures and stronger prose that must be qualified. Legacy labels in new `corpus_totals.csv` can call combined TTL counts expenditure; verify source-family claims separately. Do not add non-additive storage totals or interpret a wall-time proxy as CPU.
- `tracks/D-control/CODEX_REFLECTIONS.md` preserves the owner's broad native-AI statement and the later changes of interpretation. It is companion-private working context, not a Control delivery or an independent scientific authority.

## Revise 00 as the shared frame

Give the historical background and common research purpose a home in 00 so Papers 1/2 can remain focused. Preserve its role as the introduction to all nine papers; do not turn it into only a laboratory status report.

Trace the lineage from REA and Denna's teaching, through the work of Roth, Poulos, Kunkel and broader implementation teams documented in *Balancing Act*, to the 2018 framework and current experiments. Verify dates and references. The quantitative-search ambition is inherited; our proposed learned inquiry extends it. Avoid presenting longstanding goals as newly invented because AI is now involved.

Rework the central information argument. Aggregation can remove distinctions from a representation without proving that every system discards its source history. An instrument ledger can itself contain summarized movements; it is not synonymous with raw events. Distinguish information never captured, information lost from a particular representation, information retained elsewhere, and information present but not yet useful to a model. Review the RAW/JPEG analogy against these distinctions. Retain it only where it clarifies and does not imply universal or inevitable predictive superiority.

State the shared research program as questions about economically preserving useful information, producing controlled financial interpretations, and supporting trustworthy inquiry. Separate conceptual reasoning, practitioner experience, measured experiments and future hypotheses. Replace categorical inevitability, unique superiority or widening-AI-gap claims where current evidence does not support them.

Update the empirical-laboratory section with the original baseline, the newer combined-source runs, the limited AIM results, the separate reporting failure and formal Phase 2's future status. Explain Phase 1b as refinement/assessment without requiring every exploratory idea to be finished before circulation. Generated scenarios, if Bob later produces them, remain separate and enter this version only by an explicit evidence-cutoff decision.

Refresh the nine-paper map: Paper 1 supplies the measurement framework and empirical demonstration; Paper 2 assesses the limited AI tests and proposes the broader inquiry; Paper 5 can carry further engine/view marginal-cost demonstrations. Preserve each other paper's intended role without claiming experiments were run there. Update the conclusion to match the evidence-qualified opening.

## Revise Paper 1 around demonstrated framework utility

Proposed contribution: building on the 2018 transaction–balance cost framework, operationalize it for reproducible financial-system measurement and demonstrate usefulness in the tested setting. Do not claim a newly discovered framework, a fully measured cost field, a universal bowl shape or a globally optimal architecture.

Explain what the transaction–balance design space measures and which workloads/assumptions condition a minimum. Separate retained financial data from temporary and total processing storage, elapsed time from CPU, engine work from orchestration/controls, and measured configurations from modeled alternatives. A permutation capacity is an upper bound, not proof that another system stores all combinations. Multiple runs at Monthly are repeat observations at that point, not a sweep across cost points.

Present older and expanded-source experiments separately, with dates, source populations and processing revisions. The three updated runs validated and matched 46 compared products (14 each LDGR/GL/PERM_TBL plus four legacy views); do not generalize that hash scope to all SJE or all files. Keep the older broader hash comparison attached to its own runs. Passing controls and repeated output identity are evidence for their stated checks, not an independent audit of every accounting interpretation.

Document the separate expanded-reporting attempt briefly. It stopped at a configured 250,000-input-row per-key guard for unattributed activity; this was not an established out-of-memory failure. It did not invalidate successful earlier stages or legacy views. Full expanded-reporting completion and marginal costs remain unmeasured. Paper 1 does not need that demonstration to establish the framework's practical utility; place the further experiment with Paper 5/future work.

Include Control's reported feasibility where appropriate, preserving unresolved population and timing differences. Do not silently promote a reported Control result to independently verified evidence or a matched architecture ranking. No new Control reading is needed merely to rewrite this section from already recorded evidence.

Proposed flow: inherited framework and question; measurement method; completed observations; modeled cost relationships/sensitivity; what the framework helped distinguish; limits and further tests. Ensure charts, tables, abstract and conclusion all use the same scope and evidence cutoff.

## Revise Paper 2 as findings and a research invitation

Preserve actual AIM results and their narrow questions. Retain failed, superseded, unrun and provisional status. Resolve the known fiscal-period discrepancy before using affected magnitudes as affirmative evidence; if unresolved, qualify or omit those numerical comparisons. Do not imply that planned fine-tuning or broader native-AI training occurred. Representation, feature and model-family differences must not be described as isolating a language-prior effect.

Explain the conceptual limitation: separate churn/seasonality predictors do not test reusable exploration or sustained inquiry. Useful information preservation and improved prediction are different claims. The absence of a broad test is neither proof nor disproof of the larger proposition.

Develop the proposed native-AI objective beyond autocomplete: exploration across dimensions/dates/measures, learned relationships, competing explanations, changes of accounting interpretation, explicit scenarios, and identification of missing evidence. A shared representation should support changes of question without a new bespoke extract/model each time. Field completion is one candidate learning objective, not the definition of success. Names such as “Native AI for Economic Understanding” and “Native AI for Financial Exploration” remain working titles pending editorial choice.

Separate exact observed/derived quantities, inferred relationships, forecasts and scenario assumptions in the proposed answer contract. Discuss provenance, numerical correctness, calibration where applicable, abstention, coverage, temporal leakage and false assertions. Distinguish legitimate inference from fabricated facts and causal claims from associations. Ontological consistency and balanced arithmetic cannot establish the truth of an economic explanation. Synthetic-rule recovery, if later studied, is not discovery about actual Virginia behavior.

Ground the conceptual vocabulary in REA/REAL scholarship without claiming those works already implement our proposed AI system. Frame the research invitation around specific contributions: economic semantics, suitable learning architectures, independent evaluation, richer data and replication. A proposed Intel resource envelope is an investigation target, not a measured feasibility claim. No requirement to build the full system before review.

## Editing passes and delivery

1. Inventory authoritative sources and preserve the starting revision; construct the shared claim–evidence table and citation gaps list.
2. Prepare a coordinated outline for 00/1/2. Assign shared background to 00, cost evidence to 1 and AI findings/research questions to 2; each paper must still be readable alone.
3. Revise 00's framing and the two contribution statements, then the evidence-bearing bodies. Rewrite abstracts and conclusions after the bodies agree. Keep edit rationale in a change log, not scattered through finished prose.
4. Check every figure/table against source units, population and timing scope. Check citations against passages actually read. Do not use generated-data plans or reflected proposals as execution evidence.
5. Review cross-document consistency, bibliography, titles, terminology, hyperlinks and numbering. Check relevant navigation/overview references. Render through the member repository's established process; inspect layout, charts, captions and links. Keep canonical sources and rendered artifacts aligned.
6. Deliver the edited 00/1/2 sources, review copies, concise change log, evidence table, unresolved issues and draft cover note. Do not email or publish them from this session.

The cover note should explain the 2018 lineage and relationship to Denna's teaching, the empirical progress and limits, and ask focused questions: Is the measurement framework formulated soundly? Does the REA/REAL lineage and interpretation hold? Does the proposed trustworthy-inquiry agenda identify a worthwhile research gap, and what scholarship/collaborators would improve it? McCarthy's review is possible onward review, not a commitment already obtained.

Completion means a coherent, readable package whose claims are traceable and whose open questions invite substantive review. It does not mean all hypotheses confirmed, research tracks closed, or formal Phase 2 begun.
