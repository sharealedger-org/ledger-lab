# Track D: Primary research experiment

Updated 2026-11-15 (Arizona). Latest bounded financial-lineage checkpoint:
**`5e44a8e`** (lineage-v2, pushed). Initial Track D consolidation: `ddde891`; broader collaborator review and path reorganization follow here;
those infrastructure commits made no engine changes or financial executions.
Subsequent bounded work is recorded below; no paper edits or new Control read.

## Workstreams and reading path

Owner direction, 2026-11-15: Track D is the main research path. Former B1/B2 now
sit inside it as [CCP cost/capability](ccp/STATUS.md) and
[AIM / financial exploration](aim/STATUS.md). The Control is the separately implemented comparison system; its comparison is one component of Track D. `D-control` and B1/B2 paths remain
compatibility pointers; research IDs CCP/AIM and evidence paths stay unchanged.

Track A's external insurer material is intended for the paper's conclusion;
its results are not local evidence until supplied and qualified. Track C/Berka
is retained as an exploratory footnote candidate, outside the active work path;
its source-lineage limits and failed locked protocol remain explicit. No footnote
or conclusion is written by this reorganization.

For a new collaborator, start with the [core terms in root status](../../STATUS.md#terms-used-on-this-path),
read [the research overview](../../LEDGER_LAB_OVERVIEW.md) once, then this page and
only the relevant workstream. Returning contributors can skip the overview. The
[old project brief](../../PROJECT_BRIEF.md) is now an archive pointer, not another
required orientation document. The
[contributor guide](../../CONTRIBUTOR_ONBOARDING.md) explains the architecture's
rationale and routes implementation work to its contracts. Read history when a
specific decision needs explanation; do not reconstruct the current plan from it.

## Current plans and evidence

**2026-11-15 zero-key practicality measured:** [Receipt](../../data/test_evidence/component_pilot_0304_20261115/zero_key_projection/README.md).
Three actual report probes passed under fixed 1-GiB heap/four logical CPUs.
Historical zero workload is 185.3M rows; current-policy sensitivities 215–222M
are not a fresh census. Matched-density zero-only report projection 3.2–3.3 h,
2x merge-depth sensitivity 6.4–6.6 h; distinct-heavy stress 10–11 h, not an expected
interval or guaranteed bound. Counts, input widths, key/output cardinality and
scratch must be profiled on a representative current real year before full run.
Do not fabricate vendor IDs to evade the hot group. No full-corpus/cloud run.

**2026-11-15 next test prepared, not executed:** Official FY2012 General Fund
anchors and six signed closing-balance bridge items verified from published
statement sections G (page 50) and L (pages 174/177); three offline Scala checks pass. [Receipt](../../data/test_evidence/component_pilot_0304_20261115/anchor_preparation_2012/README.md).
Food-stamp estimate excluded as Federal Trust scope. Prepared a 1% synthetic-only
panel and 90/3,000/30,000-event ladder; see the existing pilot contract and
`data/synthetic/component_anchor_2012_v1/`. Before launch implement calendar,
anchor-ordered calibration, sorted uniqueness/registry emission, bounded merge
fan-in and manifest-driven controls; do not raise fixture caps alone. Processing
stays local; owner stopped Dropbox and left OneDrive as a possible verified archive.

**2026-11-15 lineage-v2 bounded continuation:** [Receipt](../../data/test_evidence/component_pilot_0304_20261115/README.md#lineage-v2-continuation-after-consolidation-2026-11-15).
48 relevant tests passed. Actual FY2003/04 pipeline verifies all 30 financial
source locators, two balanced legs each and original document/rule versions;
four ledger/GL files are byte-identical to v1 and all 50 generated inputs match.
Corrupted locator/rule fixtures correctly fail. This closes v1's specific lineage
checks at fixture scale only. Permutation includes audit fields, so metadata
changes its capacities; financial keys/counts and provenance cardinalities pass.
Next: verified financial-anchor coverage and the next scenario/scale contract;
do not treat the 4,096-row generator as corpus-ready or adopt the excluded draft.

**2026-11-15 reflection:** Appended “From vendor enrichment to a connected
experimental foundation” to [Codex reflections](CODEX_REFLECTIONS.md). It records
the owner's component/topology, tentative relationship, financial grounding,
experiment-phase and context-management insights, distinguishing interpretation
from bounded pilot evidence. Qualitative CKB-side context only; no new execution
or Control disclosure, and no change to the next implementation gates below.


The [strategy](../../docs/SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md) owns the roadmap:
Phase 1 direct VA corpus, Phase 2 vendor-centered synthetic work likely informing
Papers 3/4, Phase 3 broader topology from tentative allocations to reclass.
The [generation proposal](../../docs/PHASE_TWO_GENERATION_CKB_PROPOSAL.md) owns
wider design; the [pilot contract](../../docs/COMPONENT_PILOT_0304_CONTRACT.md)
and [evidence](../../data/test_evidence/component_pilot_0304_20261115/README.md)
bound the implementation claims. The following describes v1 at `213b20f`;
the v2 lineage checks are recorded above. Context order → determining-key assignment
sort/merge → entity temporal order reached the Technical Transform Layer (TTL), which
standardizes source records,\npurchase-order (PO) processing, posting/GL/permutation
and expanded reporting. 32 unchanged baseline and four generator tests passed;
36 events, 30 financial, 60 balanced SJE rows, 96 conservation checks and 20 history
oracles passed. At v1, Virginia Auditor of Public Accounts (APA) event-ID lineage and PO
rule lineage were partial; FY2003/04
VA financial anchors are unverified. No payroll, scale, AI or new cost point proved.

Paper revision proceeds independently in separate sessions under the
[member editing handoff](MEMBER_REPO_EDITING_PLAN_2026-11-14.md), including phase
introductions before 01/02, 03/04 and subsequent boundaries. Moving B1/B2 into
CCP/AIM reorganizes ongoing work; it does not close unresolved research. Private chapters
stay outside this repository.

[Document map](../../docs/README.md) classifies plans, contracts, evidence,
reflections and superseded proposals. [Consolidation receipt](../../docs/archive/consolidation_2026-11-15/README.md)
records retained snapshots and checks. [Dated prior status](../../docs/archive/consolidation_2026-11-15/tracks/D-control/STATUS.md)
preserves the full decision/exposure chronology and unaccepted proposals.
[Codex reflections](CODEX_REFLECTIONS.md) and the [working list](CODEX_PHASE2_WORKING_LIST.md)
remain CKB-side qualitative context, outside the Control package; no evidence upgrade.

## Next actions and decisions waiting

1. In a separate implementation session, review pilot limitations and define the
   next versioned scenario/schema/adapter slice before scale-up. Resolve explicit
   corpus lineage and verified financial-anchor coverage; the v2 fixture checks
   above do not establish corpus-scale admission. Responsibility evidence is not
   payroll. Preserve observed inputs and separate withheld truth from AI evidence.
2. Before larger execution, satisfy architecture gates, actual downstream-consumer
   checks, key/date/identity contracts, measured disk/scratch admission and bounded
   reporting controls. Fixture success does not establish corpus capacity or
   target GenevaERS support. Do not adopt the excluded older assignment draft.
3. Continue independent writing sessions from the member handoff. Paper mapping,
   later introduction filenames and experimental questions remain open.
4. Develop questions/evaluation criteria after versioned data inspection and
   foundation work; consult Bob before Q2-draft (the first recorded version of the next question set). No Q2-final package has been
   approved or sent. Reclass engine work is outside current preparation.
5. Phase 1 comparison remains open under [COMPARISON_PLAN.md](COMPARISON_PLAN.md):
   reconcile revenue, PO quarantine populations, eligible years and timing scope
   before comparison. Hardware is the same class (Intel, 16 GB, four threads);
   recorded CPU detail differs in completeness. Monetary rates/population choice,
   common I/O measure, cold/marginal refinements and independent gold remain open.

[TODO.md](../../TODO.md#track-d-primary-experiment-and-shared-work) retains focused open
work, including PO recovery timing and real-reference ambiguity. Earlier broad
strand plans are historical backlog, not current run instructions. Original
baseline `c3667e4`, expanded financial evidence `17bd5e7`, and bounded pilot
`213b20f` have different populations and scope; consolidation does not merge them.

## Phase 2 design (owner discussion, 2026-11-13; design still requires owner acceptance)

This heading is retained for existing links. Codex extends the CKB; Bob extends
the Control. Access stays one-way: the Control does not read the CKB. Develop
Q2-draft then Q2-final with evaluation criteria; log changes, proposer and exposure.
The Control receives only the approved final questions/criteria, data and allowed
specifications. Owner's headline measure is total Phase 1 + new capability work,
recording any Phase 1 definition changes. Incremental physical passes, sorts,
rows/bytes, time, per-process memory, storage and reconciliation explain cost.
Cold/marginal reporting and simultaneous cold questions remain proposals, not
adopted gates. The [preserved discussion](../../docs/archive/consolidation_2026-11-15/tracks/D-control/STATUS.md#phase-2-design-owner-discussion-2026-11-13-design-still-requires-owner-acceptance)
retains detailed qualifications, isolation limits and unaccepted refinements.

Control implementation `892a068` and official evidence are historical frozen
results, not independently reverified here. Preserve the official Control database
pending owner retention direction; obsolete blind Part B requires redesign.
No revised-paper disclosure is authorized. The introduction was deliberately
withheld; this CKB-informed session must not become the Control builder.

## Digest procedure (the context saver)

- Latest recorded Codex digest: **`ab0f7f0`**. On an authorized future reading,
  inspect only `ab0f7f0..origin/main` changes and record reader/revision in the
  [baseline/exposure record](../../docs/COMPANION_BASELINE_RECORD_2026-11-12.md),
  section 2. Delivery, confirmed reading and incorporation are separate events.
- Bob's earlier receipt at `3c50c19` is distinct. No new Control checkout was read
  for consolidation. Digest only when Control changes are relevant to the task.
- Do not write into Control without owner instruction. Existing hook tests do
  not verify live hook loading, memory isolation or OS enforcement. Keep CKB
  reflections/private plans outside its delivery package.
- Owner pointer to the baseline and future results-evaluation commit in Control's
  handoff remains open; do not imply that evaluation commit exists.
