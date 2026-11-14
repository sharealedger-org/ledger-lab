# Synthetic data generation on Intel

Prepared 2026-11-14 for Bob as the synthetic-data builder. This is a standalone generation assignment, separate from paper editing and from any Control implementation role. Its purpose is to produce a reproducible, clearly synthetic enrichment scenario alongside the existing Virginia corpus. Generation is not a prerequisite for the current paper review. This plan supersedes the earlier pause on generation as a workstream, without making it a publication dependency. No generation has been executed by writing this plan.

## Assignment and information boundary

Bob may inspect the available corpus and its schemas, design the generation scheme, implement reusable generation components, validate fixtures, and execute a bounded scenario on Intel. The owner does not expect to specify distributions or individual synthetic records; Bob should make defensible choices and record them. Preserve all original inputs, completed outputs and previous evidence. Stop for missing source contracts, failed controls or insufficient resources rather than inventing facts, bypassing controls or deleting data.

Use a dedicated generation session/workspace. Do not continue an existing Control-builder conversation into companion work. Do not read companion view implementations, model designs, reflections, private paper drafts or comparative findings for this assignment. If broader companion material has already been read, record that exposure; a new session does not erase it. Coordinate any later Control role through the established exposure process. This document can be handed to the generation worker on its own; it does not authorize disclosure of the companion repository as a whole.

The later Control package consists of generation specifications, reusable scripts/configuration, provenance, validation receipts and the generated dataset. Control can consume that exact dataset without rerunning generation. Do not send the package or write into Control from this task. Record delivery and confirmed reading separately when transfer is authorized. Any later Control workload requirements are a different document.

## Inputs and first inspection

Intel is the execution location because it holds the completed data. The recorded candidate root is `/Users/ktwitchell001/workspace/ckb_expanded_queue_20261113_ready/monthly_c/output`; source files were staged under `/Users/ktwitchell001/workspace/va_fresh_root`. These are historical locations to verify, not guarantees of current availability. Resolve output symlinks to immutable physical generation directories before using them. Do not combine repeated runs as additional observations.

Inventory file families, schemas, fiscal coverage, source identifiers, ordering, completion receipts, byte sizes and available free disk/RAM. Confirm upstream completion and hash the selected inputs. Preserve explicit exclusions and quarantine records. The compact Gold copy is not a substitute for full inputs: it does not include the complete canonical SJE streams. Do not launch another financial corpus replay merely to inspect or enrich it.

Determine which records actually lack identity, distinguishing blank identifiers, zero sentinels, placeholders, genuinely GL-grain activity and identifiable counterparties. Profile source families separately. Determine whether journal grouping and source-event lineage permit a defensible scenario assignment. Do not assume every zero identifier denotes an unknown vendor; some events should remain at their original unattributed grain.

Produce a bounded profiling summary and a generation specification before implementation. Identify exact fields and consumers, not just filenames. State one falsifiable claim and run its cheapest check; for example, whether all lines of a selected source event can be identified consistently without assuming journal IDs are globally unique.

## Proposed first scenario

Begin with synthetic vendor/contract enrichment and attribution, not new monetary activity. Existing real identities remain intact. Assign only eligible unattributed events to clearly marked synthetic entities; retain ineligible activity as unattributed with a reason. Generated attributes are scenario assumptions, never recovered Virginia facts.

Create a distribution of entity sizes, including ordinary and large entities. Do not tune the population to fit a processing limit or to improve a particular model score. Specify vendor count, size distribution, contract multiplicity, date ranges and eligible populations in versioned configuration. Select values after profiling and document the choice. The default first scenario should avoid deliberately planted predictive outcomes. Any later planted patterns need separate scenario IDs and a truth manifest.

Use a deterministic assignment derived from the seed, scenario version and stable source-event identity. Input order, chunk size and thread count must not change assignments. Use a disjoint synthetic namespace compatible with the receiving schema; if identifier width prevents a prefixed ID, reserve a collision-checked range and make synthetic status explicit in the mapping/master data. Generate visibly synthetic names; do not attribute invented behavior to actual businesses.

Preserve original identifiers, source file digest and record/event references in a sidecar lineage map. Map all related journal lines consistently, preserving rule identity, effective dates, amounts, currencies and debit/offset semantics. Do not redistribute money across actual agency, fund, account or period dimensions to make the scenario convenient. If grouping is ambiguous, exclude that event and count it.

Generated contracts must have internally consistent parties and effective intervals. Do not claim that a generated contract explains the historical source event. Events outside the scenario's valid interval must be rejected or explicitly unmatched. For balances, do not independently assign a new vendor to a pre-aggregated balance: derive scenario balances from the assigned event structures through the declared existing posting consumer and reconcile them after collapsing synthetic identities. If a valid financial consumer is unavailable, deliver an enrichment overlay only and label materialized scenario ledgers as not produced.

## Implementation and execution sequence

1. Freeze the selected input manifest, configuration, generation specification and expected control identities. Declare workload, record contracts, ownership, sort/partition boundaries, materialization, lineage and evidence before introducing engine behavior. Follow applicable repository architecture and verification rules in the implementation workspace.
2. Implement in Scala, with Bash for stable orchestration. Existing typed SJE, sorting, posting and control contracts take precedence over new formats. No financial logic in shell. Do not add Python without explicit owner approval; no such approval is supplied by this plan.
3. Keep processing bounded: ordered joins or declared external sorts/partitions instead of whole-corpus maps. Record each physical pass and materialization. Do not embed ad hoc generated Scala programs in Bash. Record target-runtime capability gaps rather than claiming an auxiliary implementation proves target-engine behavior.
4. Exercise a named small fixture through actual producers and consumers. Include multiple source files sharing local IDs, paired journal lines, zero and negative amounts, an ineligible GL-grain record, date boundaries, identifier collision, missing contract and a large entity. The unchanged baseline must retain its controls.
5. Run a bounded real-data pilot with a fresh attempt ID. Project disk use from observed bytes, including sorting and temporary output; check capacity before the full scenario. Historical free-space receipts are not current capacity. Do not overlap unrelated heavy processing during measured execution.
6. Run the declared scenario into a new external output root. Preserve failed attempts and their diagnostics; rerun with a new attempt ID from the same frozen inputs. Publish completion only after all required controls pass. Hash source inputs again to detect accidental mutation.

Use conceptual directories such as `generation/<scenario>/<attempt>/{config,manifests,data,lineage,controls,metrics,logs}`. Pick the actual writable Intel path after preflight. Keep bulk data outside Git. Store code, compact evidence, configuration and instructions in the designated generation workspace. Never overwrite original source files, completed financial generations or Gold snapshots.

## Acceptance and measurements

A completed delivery requires:

- Every selected event has exactly one disposition: unchanged, synthetically assigned, or explicitly excluded. Counts reconcile to the selected input population.
- Original values and lineage are recoverable from the mapping. No accidental duplication, dropped journal lines, synthetic-ID collision or unintended modification of real identities.
- Journal-group balancing and amounts by original fiscal period, agency, fund, account and currency are unchanged for enrichment-only work. No summation across incompatible currencies. Scenario posted totals reconcile after removing synthetic grouping dimensions.
- Dimension joins meet their declared cardinality and effective-date contracts. The intended consumer parses the generated records successfully.
- Repeated fixture/pilot generation with the same inputs/configuration/seed produces identical data hashes across different chunk sizes; operational timestamps may differ and are excluded from data-identity comparisons.
- Completion status is withheld on failed controls. Partial files are not eligible downstream inputs.

Measure input/output rows and bytes, generation compute, orchestration, sorting, validation, elapsed time, per-process OS memory high-water marks, spill/temporary peaks and retained storage separately. Use established measurement schemas where available and state the exact timing scope. These are enhancement-cost documentation only: do not fold them into baseline measurements or use them to claim architectural superiority. Synthetic datasets have separate population identities from observed data.

## Deliverables and handoff

Deliver a generation design, compiled Scala implementation and reusable Bash entry point, versioned configurations/seeds, input/output SHA-256 manifests, synthetic masters and assignment maps, generated scenario data, controls, resource logs, known limitations and a concise README with exact reproduction and consumption commands. Mark which outputs actually exist and which are proposed.

Provide a clean export manifest for the later Control package containing only the generation materials and dataset. Keep private analyses and companion implementation materials outside it. Report measured size/runtime and the next useful scenario, but do not automatically expand into model training, reporting implementation or formal Phase 2.

The assignment is complete when the bounded first scenario is reproducible, validated, documented and available for consumption. If data or contracts block it, preserve the completed inventory/specification and state the exact unmet dependency; do not silently substitute another population.
