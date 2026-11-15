# Document map

Start with [root STATUS](../STATUS.md), then the
[primary Track D experiment](../tracks/D-experiment/STATUS.md) and its
[CCP](../tracks/D-experiment/ccp/STATUS.md) or [AIM](../tracks/D-experiment/aim/STATUS.md)
workstream. This map routes a task; the [complete prose-document catalog](DOCUMENT_CATALOG.md)
locates individual records. Neither is a request to read every document.

## Understand the argument and contribute

| Need | Entry point | Scope |
|---|---|---|
| Understand the research position | [Overview](../LEDGER_LAB_OVERVIEW.md) | Motivating claims, evidence limits and current phase structure |
| Work effectively with or without AI | [Contributor guide](../CONTRIBUTOR_ONBOARDING.md) | Bounded reading path, architecture rationale, disagreement and session discipline |
| Build or inspect a tiny fixture | [Quickstart](../QUICKSTART.md) | Setup/run examples, not current research priorities |
| Understand ordered processing | [Execution model](CKB_LAYER_EXECUTION_MODEL.md) | Keys, temporal state, consumers, process/sort/materialization boundaries |
| Understand the intellectual sources | [Reference architecture map](REFERENCE_ARCHITECTURE_MAP.md) | Sources and correspondence, not proof of implementation |
| Understand computational discipline | [Method note](BOB_METHOD_NOTE_CKB_EFFICIENCY.md) | Preserved agentic rationale; current AGENTS rules govern older examples |
| Understand retained events versus state | [TDS/IL discussion](TDS_vs_IL_architecture.md) | Historical rationale; zero-cost and legacy runtime claims are qualified at entry |

## Current experiment direction and work

| Authority | Document | Qualification |
|---|---|---|
| Shared experiment | [Track D status](../tracks/D-experiment/STATUS.md) | CCP and AIM inside D; Control comparison is one component |
| Roadmap and phase scope | [Connected-process strategy](SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md) | Phase 1 direct VA, Phase 2 vendor-centered synthetic, Phase 3 broader topology |
| Generation design | [Phase 2 CKB proposal](PHASE_TWO_GENERATION_CKB_PROPOSAL.md) | Wider proposal beyond bounded implementation evidence |
| Bounded workload | [Component contract](COMPONENT_PILOT_0304_CONTRACT.md) | Named FY2003/04 fixture; partial lineage, unverified VA anchors |
| Open work | [TODO](../TODO.md) | Tasks and owner decisions; handoff is a dated transition log |
| Paper writing | [Member handoff](../tracks/D-experiment/MEMBER_REPO_EDITING_PLAN_2026-11-14.md) | Private sources, independent writing sessions, phase introductions |
| Comparison and exposure | [Comparison plan](../tracks/D-experiment/COMPARISON_PLAN.md), [baseline record](COMPANION_BASELINE_RECORD_2026-11-12.md) | Reconciliation and Q2 decisions open; one-way Control access |

CCP uses [graph design](COST_GRAPH_DESIGN.md) and the
[measurement contract](MEASUREMENT_AND_LOGGING.md). AIM uses the
[field-label pilot](TRAINING_LANGUAGE_PILOT.md),
[three-model protocol](THREE_MODEL_EXPERIMENT_PLAN.md) and
[validity assessment](../tracks/D-experiment/PAPER2_ASSESSMENT_2026-11-12.md).
Read the selected workstream status before choosing an older test or run plan.

Track A's external insurer work is intended conclusion material, with no local
execution page. [Track C](../tracks/C-berka/STATUS.md) retains Berka as a possible
footnote, explicitly exploratory and excluded from the main evidence path.

## Contracts and verification

[AGENTS](../AGENTS.md), [architecture gates](AGENTIC_ARCHITECTURE_GATES.md),
[verification protocol](VERIFICATION_SESSION_PROTOCOL.md) and the
[GenevaERS capability gaps](GENEVAERS_CAPABILITY_GAP_REGISTER.md) govern financial
changes. Read the relevant [layer README](../README.md#architecture-and-status)
and actual producers/consumers before implementation. The old 05 `engine/` and
step-up scaffolds are labeled historical; 04 generates SJEs and 05 aggregates only.

Corpus contracts: [external layout](RUNTIME_DATA_LAYOUT.md), [access](CORPUS_ACCESS.md),
[schemas](CORPUS_SCHEMAS.md), [record identity](CORPUS_RECORD_IDENTITY.md),
[VA data model](VA_DATA_MODEL_CONTRACT.md) and [accounting classification](VA_ACCOUNTING_CLASSIFICATION.md).
Workload-specific contracts: [identity audit](IDENTITY_AUDIT_CONTRACT.md),
[identity recovery](IDENTITY_RECOVERY_V1_DESIGN.md), [reporting](REPORTING_PASS_DESIGN.md).
These define scope; they do not establish all proposed behavior as implemented.

## Evidence and research provenance

Use the relevant named receipt and audit entry, not a plan's confidence, to assess
an implementation claim. The [audit register](VERIFICATION_AUDIT_REGISTER.md)
contains entries in more than one chronological arrangement; find by workload/date
and read later scoped corrections. It is not a next-task list.

| Evidence group | Entry point | Scope to preserve |
|---|---|---|
| Original comparison baseline | [Baseline/exposure record](COMPANION_BASELINE_RECORD_2026-11-12.md) | Frozen tag and population/timing limits |
| Expanded runs | [Queue receipt](../data/test_evidence/intel_preflight_20261113/README.md) | Three validated runs, 46 compared products; not three cost points |
| Failed reporting attempt | [Failure receipt](../data/test_evidence/reporting_corpus_20261114/README.md) | No completed corpus report/model inputs |
| Identity audit / recovery | [Findings](IDENTITY_AUDIT_FINDINGS_2026-11-14.md), [recovery receipt](../data/test_evidence/identity_recovery_20261114/README.md) | Diagnostic scope versus fixture fixes and admission limits |
| Bounded synthetic component | [Pilot receipt](../data/test_evidence/component_pilot_0304_20261115/README.md) | Actual downstream controls; no AI/scale/VA-anchor result |
| Older summaries | [2026-11-12 synthesis](CURRENT_RESULTS.md), [2026-11-11 experiment state](EXPERIMENT_STATE_2026-11-11.md) | Dated baseline analysis, not live status |
| Paper evidence packages | [Paper 1 rev2](tds-evidence/paper-1-rev2-ledger-lab/README.md), [Paper 2 rev1](tds-evidence/paper-2-rev1-ledger-lab/README.md) | Dated packages; later caveats must be checked before citation/circulation |
| Qualitative reasoning | [Codex reflections](../tracks/D-experiment/CODEX_REFLECTIONS.md), [working list](../tracks/D-experiment/CODEX_PHASE2_WORKING_LIST.md) | Attributed observations/proposals; CKB-side only, not Control delivery |

## Historical plans and archives

Retain the unusual approach's rationale, rejected alternatives, failed approaches,
original thresholds and corrections so collaborators can scrutinize the research.
Archive material is available on demand; it is outside the default session reading
path. Current documents should explain the decision well enough to use without
reconstructing the whole chronology. If reasoning is disputed, follow the specific
history link and preserve attribution when recording a correction.

[Phase 1 objectives](RESEARCH_OBJECTIVES.md), [test design](TEST_DESIGN.md),
[early strategy](RESEARCH_STRATEGY.md), [implementation plan](ENGINE_IMPLEMENTATION_PLAN.md),
[enhancement plan](DATA_ENHANCEMENT_PLAN.md), [organization proposal](PROJECT_ORGANIZATION_PLAN.md),
[full-corpus runbook](FULL_CORPUS_RUN_CHECKLIST.md), [rerun handoff](CKB_REVENUE_RERUN_2026-11-13.md)
and [Control projections](TRACK_D_NOTES.md) have dated roles. Their historical
queues, branch paths and expected outcomes do not override current status/evidence.
[Original VA specifications](VA_Data_Layouts_and_Specs.md) remain source design history,
including historical technology choices, rather than a current runtime mandate.

Superseded pointer paths: [broad strand plan](PHASE2_SYNTHETIC_DATA_PLAN.md),
[Intel Phase 1b handoff](PHASE_1B_SYNTHETIC_DATA_INTEL_PLAN.md),
[old proposal name](PHASE_ONE_GENERATION_CKB_PROPOSAL.md),
[earlier paper plan](../tracks/D-experiment/PAPER_VERSION_2_RESEARCH_PLAN.md).
Old track paths likewise point to the current experiment/workstreams.

Preserved archives: [2026-11-12 cleanup](archive/document_cleanup_2026-11-12/README.md),
[initial Track D consolidation](archive/consolidation_2026-11-15/README.md),
[broader collaborator review](archive/collaborator_review_2026-11-15/README.md).
The [review receipt](COLLABORATOR_REVIEW_2026-11-15.md) states coverage and remaining
limitations. Evidence and reflections retain their contents; documentation cleanup
never upgrades a result or authorizes a financial run.
