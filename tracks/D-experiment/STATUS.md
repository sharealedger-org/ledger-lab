# Track D: Primary research experiment

Updated 2026-11-16 (Arizona). Latest bounded financial-lineage checkpoint:
**`5e44a8e`** (lineage-v2, pushed). Initial Track D consolidation: `ddde891`; broader collaborator review and path reorganization follow here;
those infrastructure commits made no engine changes or financial executions.
Subsequent bounded work is recorded below; no paper edits or new Control read.

## Workstreams and reading path

Owner direction, 2026-11-15: Track D is the main research path. Former B1/B2 now
sit inside it as [CCP cost/capability](ccp/STATUS.md) and
[AIM / financial exploration](aim/STATUS.md). The Control is the separately
implemented comparison system; its comparison is one component of Track D.
Redundant `D-control` and B1/B2 pointer paths are retired; research IDs CCP/AIM
and evidence paths stay unchanged.

Track A's external insurer material is intended for the paper's conclusion;
its results are not local evidence until supplied and qualified. Track C/Berka
is retained as an exploratory footnote candidate, outside the active work path;
its source-lineage limits and failed locked protocol remain explicit. No footnote
or conclusion is written by this reorganization.

For a new collaborator, start with the [core terms in root status](../../STATUS.md#terms-used-on-this-path),
read [the research overview](../../LEDGER_LAB_OVERVIEW.md) once, then this page and
only the relevant workstream. Returning contributors can skip the overview. The
[old project brief](../../docs/archive/PROJECT_BRIEF_PRE_READABILITY_2026-11-15.md) is archived directly; the overview supplies current orientation. The [contributor guide](../../CONTRIBUTOR_ONBOARDING.md) explains the architecture's
rationale and routes implementation work to its contracts. Read history when a
specific decision needs explanation; do not reconstruct the current plan from it.

Superseded plans and the pre-measurement Track D notes now live in the
[retired-plans archive](../../docs/archive/retired_plans_2026-11-15/README.md).
Redundant pointer files have been removed; current links go directly to retained
documents. The archive keeps their reasoning and unfinished ideas without making
those old assignments part of the active reading path. No runtime evidence changes.

**2026-11-15 reflection on the transition:** [From establishing the laboratory to carrying an experiment forward](CODEX_REFLECTIONS.md#2026-11-15-091542-azt-utc0700--from-establishing-the-laboratory-to-carrying-an-experiment-forward)
considers the end of startup and approaching Phase 1 conclusion, with CCP/AIM as
parts of one inquiry. Qualitative interpretation, not formal phase closure or new
evidence. The journal moved intact from `D-control` to `D-experiment` at `9cab0c0`;
`484925d` removed only the old-path pointer. Timestamp requirements are now explicit
in AGENTS and the contributor guide. Runtime checkpoints and next gates are unchanged.

## Current plans and evidence

**Post-reboot restart, owner requested:** 18 verification tests pass across three
matched suites, including indexed versus sorted/sequential report equivalence.
Corrected the new fixture's missing `reportProfile=expanded` (previously selected
legacy views before reduction). Detached driver PID 1984 is active under
`session-run.tmp/report_sorted_v3_20261115/reboot_attempt/`, with caffeinate,
minute process/storage samples and a 20-GiB low-scratch stop. FY2012 sort/report
and connected checks gate automatic FY03–FY16 sort/report over retained Run 1
financial outputs. No financial replay or archival yet. AC attached; prior
battery throttling is an owner hypothesis. Completion remains pending.
The older three-execution driver below remains stopped.

**Running, 2026-11-15 13:53 AZT:** Three synthetic-reference v2 executions plus
expanded reports launched locally on frozen b540e5e, PID 86722, with caffeinate
and minute monitoring/20-GiB scratch stop. Root
`session-run.tmp/synthetic_reference_corpus_v2_20261115/` holds status, logs and
live storage/process samples. OneDrive archival is armed only after all three
evaluations and comparisons pass. No archival copy started yet. Preserve HEAD
and code while the queue runs; completion/results remain pending.

**2026-11-15 corrected real-year admission passed:** All financial/source-policy,
actual report and cardinality checks pass; [receipt](../../data/test_evidence/synthetic_reference_corpus_v2_20261115/README.md).
FY2012 has 17,592,858 zero movements and 335,103 zero posted keys; all receipt
legs remain zero and GL is unchanged. Report checks: 1,031,381 reconciliation,
20 history, 548,608 conservation PASS; 23,292,369 financial rows covered. Extract
and reduce 24.7 minutes, 3.2 GiB retained, peak RSS 1.25 GiB. Ready to launch three
identical full executions plus reports with live 225-GiB/first-run-allocation
admission and a 20-GiB low-disk stop. One-to-two-day planning sensitivity; no
same-day guarantee or new formal cost point. Verified sharing preserves every
result/profile path and byte; no local result deletion. Archival follows success.

**2026-11-15 canonical quote boundary:** Corrected FY2012 financial/source-policy
checks pass: all 2,745,656 receipt legs zero; GL byte-identical across the identity
policy change. Reporting exposed literal quotes in canonical SJE fields being
misread as RFC quoting. ReportingOrder now follows the existing literal canonical
contract for movement/balance/opening while retaining strict CSV for dimensions.
47 tests pass, including actual post/report exact-quote preservation. Reporting
only is replaying from the same committed inputs into `fy12/report_literal_v3`
under the corrected admission root. Earlier failed report scratch is retained;
full-corpus admission and OneDrive archival are still pending.

**2026-11-15 source-policy correction during admission:** First FY2012 profile
passed monetary controls but exposed legacy REV master aliases attributing
2,452,576 agency-grain receipt SJE rows. It is not admitted; financial/profile
evidence and stopped report scratch remain intact. New receipt-safe preparation
preserves the original master, excludes typed REV aliases with lineage, rejects
remaining agency-key/name collisions and requires actual receipt SJE IDs to stay
zero. No financial engine code changed. 46 regression tests and four focused
preparation checks pass. Corrected fresh FY2012 replay is now under
`session-run.tmp/corpus_admission_20261115_receipts_v2/`, with caffeinate and
minute-by-minute passive monitoring. Full queue admission is still pending.

**2026-11-15 autonomous execution preparation:** Owner authorized three executions
of the synthetic-reference corpus with expanded reports, evaluation and verified
OneDrive archival, and explicitly authorized a new preparation path after the
excluded-draft dependency was found. New
[v2 contract](../../docs/SYNTHETIC_REFERENCE_CORPUS_V2_CONTRACT.md): assignments
come from actual committed SJE/ledger IDs through bounded literal sorting and
declared synthetic template cycling. No excluded artifacts are consumed.
Unchanged baseline 42 tests passed (one environment-dependent case canceled),
two new preparation tests passed; actual connected reporting passed 26
reconciliation, 20 history and 96 conservation checks. FY2012 current-policy
financial/report admission profile is running with caffeinate under
`session-run.tmp/corpus_admission_20261115/`. Full execution/time/scratch admission
and archival remain pending; these are exploratory results, not new cost points.


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
standardizes source records,
purchase-order (PO) processing, posting/GL/permutation
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

**Owner clarification, 2026-11-15 09:26 AZT:** The downloaded VA corpus run is
complete; retained historical evidence/Gold (including OneDrive) remains the
record for that measured workload. The next requested run is the prepared
synthetic-reference corpus, supporting the Phase 1 conclusion and preparation
for Phase 2 Q2 development, not another direct-VA baseline. The
[current pre-run checklist](../../docs/FULL_CORPUS_RUN_CHECKLIST.md#current-run-selection--owner-clarification-2026-11-15)
now distinguishes these workloads. Later parser/lineage/reporting changes mean
HEAD must not be assumed identical to historical measured code. Synthetic
admission remains pending; no full run launched or OneDrive copy made this session.
Older excluded assignment drafts/artifacts remain outside accepted execution.

**Draft cleanup, 2026-11-15:** Owner authorized deleting the unused assignment
draft. Only its own uncommitted launcher and historical records referenced it.
Removed the untracked Scala source and restored the launcher to its committed
version; no active dependencies remain. This supersedes earlier preserve-draft
instructions. Accepted component generation/reporting code is unchanged.

**Session boundary, 2026-11-15:** Owner plans a fresh corpus-run session. Start
with current status/reflection and checklist; validate representative current-year
zero counts/key diversity, source policies and live time/scratch before launch.
No full run started. Use local execution/caffeinate/monitoring if admitted; any
OneDrive archive follows success and verification. Old assignment drafts stay
excluded. Measured checkpoint `6b6a561`; document transition through `b0c667d`.

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

**Restart timing qualification:** owner reported sleep during battery-symbol
troubleshooting. Post-wake driver 1984 and Java 2510 remain active (95.7% CPU);
caffeinate assertions remain present. Monitor gap 06:39:27Z–06:41:06Z; exact sleep
duration unverified. Run continues; elapsed timing is sleep-interrupted exploratory
evidence rather than an uninterrupted timing replicate. Attempt operator_events.log
records the observation.

**Full-corpus reporting released:** FY2012 sorted/sequential extract, reduction
and ConnectedViewsCheck passed. Driver entered CORPUS_SORT at
2026-11-16T06:52:30Z (2026-11-15 23:52:30 AZT), over retained FY03–FY16 Run 1
financial outputs. Corpus analytical completion/controls remain pending. Live
status/logs remain under report_sorted_v3_20261115/reboot_attempt.

**2026-11-16 monitoring:** FY03–FY09 reporting sorts completed (seven of fourteen
years); corpus analytical pass remains pending all sorts. Requested live Terminal
dashboard opened using `session-run.tmp/report_sorted_v3_20261115/dashboard.sh`
(30-second refresh, reads only logs/storage samples). Run remains active; no
financial replay or archive. Owner requested an open monitoring turn.

**2026-11-16 01:12:15 AZT — corpus report active:** All fourteen years completed
ReportingSort with 28 source files, 273,380,488 rows and 94,710,620,118 bytes;
row/byte conservation passed for every sort. Sort stage 2026-11-15 23:52:30 to
2026-11-16 01:12:15 AZT (79m45s wall, exploratory host conditions). Driver has
started corpus sequential expanded 5v, followed automatically by 5r, connected
checks and summary. ~143 GiB free at transition; 20-GiB monitor stop remains.
Final controls/completion, further independent executions and archive remain open.

**2026-11-16 04:41 AZT:** Full-corpus sequential 5v completed: 273,447,217 input
rows including dimensions, 65,716 vendor groups, zero reconciliation failures;
one financial input pass, zero index blocks/bytes, bounded spill and 1,400,717,312
bytes peak RSS. Extract elapsed 12,541.5 seconds (~3h29m); 5r reduction active.
217,406,124-row largest group processed without inventing identities. Final
ConnectedViewsCheck/summary remain pending. Monitor's last sample 04:38:32 AZT
preceded a du-versus-spill-cleanup race; restarted around 04:42 with samples
preserved and unknown allocation on transient du errors, keeping independent disk
stop active. Engine source unchanged. Sampler PID 35208, driver 1984.

**2026-11-16 04:46:40 AZT — corpus reporting complete:** CORPUS_EVALUATED published,
driver rc=0. Connected checks pass for all 273,380,488 financial rows, 11,835,781
reconciliation rows, 20 history oracles and 2,102,912 conservation rows. Final
census includes 71,119,226 temporal-cut and 12,985,882 attribute-total rows.
Permanent receipt: data/test_evidence/report_sorted_v3_20261116/ (repository root).
This completes expanded reporting for retained Run 1, not three independent
executions. Runs 2/3 and OneDrive archival have not started; about 109 GiB free
at completion. Prior monitor restart PID was 35362 (35208 did not survive launch);
samples resumed 04:43:20 AZT. All prior timing/host qualifications remain.

**Owner priority clarification, 2026-11-16:** Further identical executions are
not the next useful step. Owner wants newly generated synthetic inputs inspected
before starting Q2 development. Completed reporting establishes corpus-scale
analytical execution/conservation over existing VA-derived financial outputs and
synthetic reference assignments; it does not establish a newly generated economic
scenario corpus. Defer runs 2/3; focus next on the component generator and an
inspectable versioned synthetic sample. Existing FY2012 anchor/scale preparation
still requires the calendar, calibration, registry, bounded merge and manifest
checks declared in COMPONENT_PILOT_0304_CONTRACT.md before enlargement. No new
synthetic corpus generated by this clarification; Q2 remains unopened.

**Paper revision input prepared, 2026-11-16:** Owner requested using the completed
reporting execution in the Paper 1/2 updates. Added
`docs/tds-evidence/reporting-20261116-paper-updates/README.md` with suggested
passages, exact population and stage boundaries, output/control table,
claim–evidence mapping and qualifications. Linked it at the top of the member
editing handoff. No private chapter edit, circulation, new model result or engine
change. Full-corpus completion is evidence for this reporting workload only;
new synthetic generation/inspection remains the next execution priority.

**Q2 preparation discussion, 2026-11-16:** Owner proposes a complete, self-contained
new synthetic corpus for Phase 2, using the completed reporting results to guide
generation, replacing most unresolved parties within the fictional population,
and considering a later Zenodo release. This revises the earlier companion/mixed
population proposal as a design direction under discussion; no generation or
publication launched. Keep original VA evidence immutable. Separate aggregate
calibration evidence from invented entity connections, distinguish vendor,
employee and revenue-party roles, and retain deliberate missing-identity cases.
The reported 220,909,208 unattributed input rows are not a count of unidentified
vendors. Next useful preparation: versioned calibration/scenario contracts and a
self-contained inspection sample before full-scale generation and Q2 freeze.

**Q2 corpus and AIM design extension, 2026-11-16:** Owner adds component business
model outputs across historically disconnected process systems, later evidence
completing tentative outcomes, and forecasting/risk under projected recasts and
temporary future-dated references. Added docs/PHASE_TWO_AIM_CORPUS_REQUIREMENTS.md
and linked the strategy/generation proposal and AIM status. Includes candidate
source-system improvements, matched source-quality experiments, historical versus
later-known views, outcome lineage, conditional-versus-probabilistic evaluation
and accounting boundaries. Owner maps later AIM work to Papers 5/6, Phase 3, with
possible new Paper 5 engine outcome-reconciliation work. Explicitly design_only;
no implementation, new corpus, training or target-runtime support claim. Paper
1/2 edits proceed in independent sessions; no private manuscript or Control read.

**AIM design scope clarified by owner, 2026-11-16:** Preparation has not included
extensive design of the envisioned AIM models or their features; their eventual
form remains unknown. The focus is imagining source-data conditions that might
support possible functionality. Updated the corpus requirements, strategy,
generation proposal and AIM status so functional hypotheses and suggested
comparisons are not presented as completed model/feature design. No model or
corpus execution; existing historical AIM evidence unchanged.

**Shared checkpoint for ARM writing, 2026-11-16:** Owner requested commit/push of
reporting code/evidence, Paper 1/2 revision input and current Q2 source-condition
design. Reporting source hashes and retained sorter match; recorded 18 tests and
corpus controls pass, Bash syntax/whitespace checks pass. Compact cited logs are
included despite the generic log ignore. ARM can follow MEMBER_REPO_EDITING_PLAN
and the reporting addendum without copying the large execution datasets. Synthetic
preparation remains the local next work; new AIM model/feature design is still open.
