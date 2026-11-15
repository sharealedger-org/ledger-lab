# Documents map

Updated 2026-11-15. Start at [`../STATUS.md`](../STATUS.md), then the relevant
track status. The map below identifies current authority; older results and plans
remain dated records rather than current run instructions.

## Current plan and contract authority

| Role | Current entry point | Boundary |
|---|---|---|
| Roadmap / phase scope | [Connected-process strategy](SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md) | Phase 1 direct VA; Phase 2 vendor-centered synthetic; Phase 3 broader topology |
| Generation design | [Phase 2 CKB proposal](PHASE_TWO_GENERATION_CKB_PROPOSAL.md) | Wider proposal; implementation bounded by pilot below |
| Implemented pilot contract | [FY2003/04 component contract](COMPONENT_PILOT_0304_CONTRACT.md) | Tiny synthetic envelopes, no verified VA anchors or scale claim |
| Pilot result at `213b20f` | [Evidence receipt](../data/test_evidence/component_pilot_0304_20261115/README.md) | Actual downstream flow; partial source/rule lineage, no AI result |
| Paper writing | [Member editing handoff](../tracks/D-control/MEMBER_REPO_EDITING_PLAN_2026-11-14.md) | Private source workflow, phase introductions; independent writing sessions |
| Comparison / exposure | [Comparison plan](../tracks/D-control/COMPARISON_PLAN.md), [baseline record](COMPANION_BASELINE_RECORD_2026-11-12.md) | Reconciliation/Q2 decisions open, one-way Control boundary |
| Identity / reference contracts | [Identity recovery](IDENTITY_RECOVERY_V1_DESIGN.md), [reference architecture](REFERENCE_ARCHITECTURE_MAP.md) | Workload-specific limits remain; not a replacement roadmap |
| Qualitative design history | [Reflections](../tracks/D-control/CODEX_REFLECTIONS.md), [working list](../tracks/D-control/CODEX_PHASE2_WORKING_LIST.md), [handoff](../SESSION_HANDOFF.md) | Attributed, dated, CKB-side; not Control delivery or runtime evidence |

Superseded entry paths remain as pointers: [broad strand plan](PHASE2_SYNTHETIC_DATA_PLAN.md),
[Intel Phase 1b handoff](PHASE_1B_SYNTHETIC_DATA_INTEL_PLAN.md),
[earlier paper plan](../tracks/D-control/PAPER_VERSION_2_RESEARCH_PLAN.md), and
[old generation proposal name](PHASE_ONE_GENERATION_CKB_PROPOSAL.md).
Their uncompleted ideas remain historical backlog, not newly authorized work.
[Consolidation snapshots and receipt](archive/consolidation_2026-11-15/README.md)
preserve original wording; evidence, negative results and reflections remain in place.
The older uncommitted assignment producer/launcher draft remains excluded.

## Results and evidence (read when a track page points to them)

| File | Use |
|---|---|
| [`CURRENT_RESULTS.md`](CURRENT_RESULTS.md) | Where things stand, status per claim, for a newcomer. Refreshed 2026-11-12 with completed run 4 evidence. |
| [`tds-evidence/paper-1-rev2-ledger-lab/`](tds-evidence/paper-1-rev2-ledger-lab/README.md) | Paper 1 (Revision 2) Evidence Package for TDS: measured results summary, Mix cost curve model, CKB Cost Model Excel workbook, one-page explainer, limits and Phase 2 roadmap. |
| [`tds-evidence/paper-2-rev1-ledger-lab/`](tds-evidence/paper-2-rev1-ledger-lab/README.md) | Paper 2 (Revision 1) Evidence Package for TDS: Three-Model experiment design, AIM test suite results (AIM1 non-injectivity, AIM3 churn C=0.7527), Gold data ground-truth verification, limits and Phase 2 roadmap. |
| [`VERIFICATION_AUDIT_REGISTER.md`](VERIFICATION_AUDIT_REGISTER.md) | The evidence ledger, newest entries last. The register wins any disagreement. |
| [`COMPANION_BASELINE_RECORD_2026-11-12.md`](COMPANION_BASELINE_RECORD_2026-11-12.md) | The preserved baseline for the control comparison, and the exposure log. |
| [`EXPERIMENT_STATE_2026-11-11.md`](EXPERIMENT_STATE_2026-11-11.md) | Measured facts and what each Paper 1 graph can show. |
| [`CORPUS_FINDINGS.md`](CORPUS_FINDINGS.md) | What was measured in the raw corpus (2026-10-31). |

## Track B1: Paper 1 cost curve

[`COST_GRAPH_DESIGN.md`](COST_GRAPH_DESIGN.md), [`RESEARCH_OBJECTIVES.md`](RESEARCH_OBJECTIVES.md), [`TEST_DESIGN.md`](TEST_DESIGN.md) (CCP cost points and Paper 2 model evidence), [`RESEARCH_STRATEGY.md`](RESEARCH_STRATEGY.md), [`FULL_CORPUS_RUN_CHECKLIST.md`](FULL_CORPUS_RUN_CHECKLIST.md), [`MEASUREMENT_AND_LOGGING.md`](MEASUREMENT_AND_LOGGING.md), [`ZENODO_ARCHIVE_PLAN.md`](ZENODO_ARCHIVE_PLAN.md), and the draft outside-reader page [`LEDGER_LAB_ONE_PAGE_EXPLAINER.html`](LEDGER_LAB_ONE_PAGE_EXPLAINER.html) (open items in `TODO.md`, "Explainer").

## Track B2: Paper 2 models

[`THREE_MODEL_EXPERIMENT_PLAN.md`](THREE_MODEL_EXPERIMENT_PLAN.md), [`VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md`](VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md), [`TEST_DESIGN.md`](TEST_DESIGN.md) (AIM sections), [`VENDOR_IDENTITY_INVESTIGATION.md`](VENDOR_IDENTITY_INVESTIGATION.md), [`SUPPLIER_DIRECTORY_SNAPSHOT.md`](SUPPLIER_DIRECTORY_SNAPSHOT.md), and the vendor-bridge evidence files (`PUBLIC_VENDOR_BRIDGE_*`, `VENDOR_IDENTIFIER_EVIDENCE_*`).

## Track C (parked)

[`TEST_DESIGN_TRACK_C_BERKA.md`](TEST_DESIGN_TRACK_C_BERKA.md), [`PAPER_2_TRACK_C_INSERTION_DRAFT.md`](PAPER_2_TRACK_C_INSERTION_DRAFT.md).

## Track D (control)

Start with the [Track D status](../tracks/D-control/STATUS.md) and [current comparison entry point](../tracks/D-control/COMPARISON_PLAN.md). The original pre-exposure comparison draft is archived; it is not the Phase 2 run plan.

[`TRACK_D_NOTES.md`](TRACK_D_NOTES.md): summary and pre-measurement projections (predictions, not results). The control's own documents are in its repository.

## Engine, rules and method (stable; read before editing the engine)

[`AGENTIC_ARCHITECTURE_GATES.md`](AGENTIC_ARCHITECTURE_GATES.md) (mandatory gates for any new engine or perspective), [`VERIFICATION_SESSION_PROTOCOL.md`](VERIFICATION_SESSION_PROTOCOL.md), [`GENEVAERS_CAPABILITY_GAP_REGISTER.md`](GENEVAERS_CAPABILITY_GAP_REGISTER.md), [`CKB_LAYER_EXECUTION_MODEL.md`](CKB_LAYER_EXECUTION_MODEL.md), [`ENGINE_IMPLEMENTATION_PLAN.md`](ENGINE_IMPLEMENTATION_PLAN.md), [`REFERENCE_ARCHITECTURE_MAP.md`](REFERENCE_ARCHITECTURE_MAP.md), [`TDS_vs_IL_architecture.md`](TDS_vs_IL_architecture.md) (flagged for the monograph), [`BOB_METHOD_NOTE_CKB_EFFICIENCY.md`](BOB_METHOD_NOTE_CKB_EFFICIENCY.md).

## Corpus and data contracts

[`CORPUS_ACCESS.md`](CORPUS_ACCESS.md), [`CORPUS_SCHEMAS.md`](CORPUS_SCHEMAS.md), [`CORPUS_RECORD_IDENTITY.md`](CORPUS_RECORD_IDENTITY.md), [`RUNTIME_DATA_LAYOUT.md`](RUNTIME_DATA_LAYOUT.md), [`VA_DATA_MODEL_CONTRACT.md`](VA_DATA_MODEL_CONTRACT.md), [`VA_ACCOUNTING_CLASSIFICATION.md`](VA_ACCOUNTING_CLASSIFICATION.md), [`VA_Data_Layouts_and_Specs.md`](VA_Data_Layouts_and_Specs.md) (a 2017 SAFR-on-Spark functional definition, background), [`DATA_ENHANCEMENT_PLAN.md`](DATA_ENHANCEMENT_PLAN.md) (an early plan under `PROJECT_BRIEF.md`; not checked against current work).

## Archive

[Documentation cleanup snapshots, 2026-11-12](archive/document_cleanup_2026-11-12/README.md): the superseded Track D comparison draft and the verification protocol before removal of historical status tables. Current verification rules and unresolved gates remain in the active protocol.

[`archive/SESSION_HANDOFF_ARCHIVE_2026-11.md`](archive/SESSION_HANDOFF_ARCHIVE_2026-11.md): handoff entries to 2026-11-10, moved unchanged. Other documents that cite old handoff entries by name now mean this file.

## Other files in `docs/`

[`PROJECT_ORGANIZATION_PLAN.md`](PROJECT_ORGANIZATION_PLAN.md): the proposal this structure follows. `Universal Journal Instrument Ledger Model v0.7.xlsx` and `zenodo_metadata/`: reference material, not classified here.
