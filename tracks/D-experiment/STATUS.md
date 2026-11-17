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

**Full four-worker fused replay active:** Session11522, group42994, root
`session-run.tmp/phase2_fused_baseline_v1`, launched from `da99dfe` with unchanged
admitted `a0dae7c` financial classes. FY03/04/05/06 statuses and four live JVMs
(43062–43065) confirmed. First wave budget116,415,527KiB plus40GiB reserve;
315,384,272KiB free at admission. Existing dashboard window504 switched to full
run. Next financial waves, global controls, dimensions and reporting are automatic
and gated; all full acceptance/result review/Zenodo script remain pending.

**FY03 admission completed:** v3/session11936 exit0; native+combined GL controls
passed, 13,850,802 journal rows seen by both actual posting observers, one input
open, 1,105,396 ledger rows, 364,216 GL keys. Process elapsed663.05sec,
maximum RSS709,038,080 bytes. Evidence in
`data/test_evidence/phase2_derived_baseline_20261116/fused_admission_v3/`.
Admits the next bounded full replay; not full population/report acceptance.

**FY03 native gate passed; fused posting active:** Admission v3 produced the exact
retained FY03 GL SHA-256 `f86fafe44e6fb6e0e3a1878001cf6bb02fa59af63b223cf62dda2d6f15d98160`.
Source controls: 13,508,560 historical plus 342,242 wholly generated journal rows;
posting consumes their 13,850,802 combined rows. Source cache: 29,248,690 hits,
567 misses, peak567/4096 entries. Final combined GL/posting/resource acceptance
pending; partial evidence under `.../phase2_derived_baseline_20261116/fused_admission_v3/`.

**Two-worker controller fixture passed:** Actual Bash controller executed two raw
years in one wave, verifying 16 historical + 8 wholly generated = 24 journal rows
and exact GL controls. Missing-reference fixture caused the worker failure and
isolated scheduler group stop (exit143), without shared completion. See
`data/test_evidence/phase2_derived_baseline_20261116/controller_fixture/`.
Real FY03 v3 remains active; full scheduler/resource/report acceptance pending.

**Corrected real-year admission restarted:** Frozen `a0dae7c`, session 11936,
`session-run.tmp/phase2_fused_admission_v3`; starting resource gate passed with
321,813,064 KiB free, 29,217,327-KiB worker budget and 40-GiB reserve. Dashboard
window 504 selects v3; class hashes recorded. Native/combined GL controls,
posting/permutation and resource acceptance remain pending. No timing claim.

**Real-year gate caught invocation mismatch:** Admission v2 source counts/cache
checks passed, but strict native GL hash correctly failed before posting. New
wrapper used long years2003/2031; real Bash adapter contract is03/31, affecting APA
period/date formatting. Corrected parameter and reference fixture; 47 tests pass
with explicit 0031-period/date assertions. Native hash gate unchanged. Fresh v3
admission next; failed attempts are not full acceptance. Details in audit register.

**Date hot-path correction verified:** Stopped first fused admission after five
JVM samples showed four in date parsing/exception creation. No financial failure
or accepted full-year result. Added equivalent ISO dispatch, bounded positive/
negative date cache (4,096 entries, key limit 128 chars), and period memo (64).
Kept the narrower attribution parser unchanged. 47 tests pass against the frozen
parser and financial/report controls; fresh real-year admission follows this
checkpoint. Prior stopped attempt and stacks preserved under `date_hotpath/`.

**21:40 AZT real FY03 fused admission active:** `f53ac26`, execution session
37834, driver 36376/worker 36419, root `session-run.tmp/phase2_fused_admission_v1`.
Input hashes/AC and provisional ~28-GiB worker budget plus 40-GiB reserve pass
(~315 GiB free). Compiled Scala runs directly at 2-GiB max heap/one active CPU;
live minute process/storage samples and 40-GiB process-group stop enabled.
Dashboard switched to actual fused profile. Native/combined controls, actual
RSS/scratch acceptance and subsequent scheduling/reporting remain pending.

**Fused financial build ready for real-year admission:** 45 tests pass after
resource cleanup checks. FusedFinancialYear includes original-pair native GL
hash checks, shared source sort, actual posting observations, bounded permutation
fan-in and exact combined GL/row controls. New admission wrapper runs compiled
Scala directly with 2-GiB heap/one logical processor and 40-GiB live scratch stop;
FY03 provisional worker budget ~28 GiB, not a measured footprint. Fused dashboard
shows shared/in-Post work without implying separate scans. Freeze this checkpoint,
then run real FY03 before any four-worker/full-corpus admission. No cost claim.

**Owner reprioritization — fused implementation now:** Stop v2 before full
completion; this is synthetic generation proof, not a cost-curve measurement.
Verified process group 26340 terminated; session 21872 exited 143. Marked
`STOPPED_BY_OWNER_REORG`; preserve completed raw controls/interrupted attempt.
Implement/verify the [fused process contract](../../docs/PHASE_TWO_FUSED_PROCESS_CONTRACT.md)
on bounded fixtures before new full admission. Pair normalization before initial
sort, shared Pay/PO sort, GL/permutation observations in posting; required bounded
reductions remain explicit. Preserve Paper 1 independent/default path. Goal active;
no large replay currently running, no fused success claim or Q2/publication.

**21:21 AZT bounded source slice:** Shared pair-policy exit and pre-sort APA/PO
output exits now feed one bounded shared financial sorter. New named fixture
matches independent journal multisets and actual ledger/GL/permutation/report
outputs; 26 tests pass, including unchanged baseline. Posting observers/native
control branch/full orchestration remain unimplemented. Evidence in
`phase2_derived_baseline_20261116/fused_source_tests.txt`; no full admission yet.

**21:32 AZT fused financial fixture:** Posting now supplies actual journal-read
observations to shared GL/permutation accumulators. The no-reread falsifier and
bounded permutation merges pass; complete FusedFinancialYear adds native raw-pair
GL hash and combined row/GL controls. 45 tests pass across eight suites. State
caps are explicit; GL cap overflow fails instead of spilling. Actual FY03
resource/control admission and full controller/reporting remain pending; no cost
claim. See `fused_financial_year_tests.txt` and the audit register.

**20:55 AZT — corrected full v2 active:** `465fe21`, execution session 21872,
root `session-run.tmp/phase2_derived_baseline_v2`; fresh raw replay, not an in-place
retry. Frozen hashes/AC/starting gate passed (~326 GiB free); stage reserve and
monitoring enabled. Live Terminal dashboard restored stage columns and compact
class rows; active pointer selects v2. V1 failure remains preserved. Full financial
and reporting acceptance pending. Owner has resumed the persistent app goal;
autonomous agent follow-through and the financial driver are both active.

**Owner resumes, evening 2026-11-16 AZT:** V1 stopped at posting because the
launcher did not seed its isolated generation with the prepared merged journals.
Preserved failure logs prove the missing consumer input; no combined acceptance.
Fixed only the Bash handoff (`--seedFromCommitted`); actual Bash-to-Scala test
passes (1,520 journals consumed, ledger/GL/permutation produced, input hash
unchanged), alongside 22 unchanged baseline tests plus the integrated fixture.
Fresh full v2 replay follows the correction checkpoint; no Scala financial change.
Dashboard restored stage/time/count columns, with historical/generated/combined
class rows. Queued years stay compact; active-year counts expand below each row.
All previous active-v1 statements below are superseded; full acceptance pending.

**Post-reboot launch, 2026-11-16 15:26 AZT:** Prepared driver from `3f2e3ab`
is active in execution session 25831 at `session-run.tmp/phase2_derived_baseline_v1`;
initial stage `historical_03`. Input hashes, AC and 300-GiB start gate passed
(~346 GiB free). Caffeinate, minute process/storage monitoring and 40-GiB stage
gates enabled. Post-reboot cloud-service contention observed; timing exploratory.
Initial detached launcher exited before admission; persistent launch is the actual
attempt. Full controls/interpretation pending; preserve engine/config while active.
No Q2 evaluation or publication. This supersedes reboot-wait instructions below.

Owner requests autonomous continuation while away. Unified live terminal display:
`bash scripts/dashboard_full_corpus.sh` automatically selects this attempt;
`--derived [absolute-root] [seconds]` selects explicitly and `--legacy [seconds]`
retains the original view (also selected by VA_LOGS/VA_OUT overrides). Display
refreshes every 30 seconds; it reads logs only. Syntax and one-render checks pass.
Financial driver/config are unchanged. Autonomous monitoring and validation active.

First full-scale year progress: native FY03 produced 13,508,560 journals and a GL
whose hash matches retained FY03 exactly. Wholly-generated SY01 produced 342,242
journals; historical normalization is active. See the pending
[execution receipt](../../data/test_evidence/phase2_derived_baseline_20261116/README.md).
These are intermediate checks; combined posting/full-corpus acceptance is pending.

**16:30 AZT:** FY03 normalization completed with all 13,508,560 journal rows
preserved, 2,232,134 source groups reassigned and bounded sort buffers. Peak RSS
1,051,447,296 bytes; process wall 2,995.93 seconds. Combined SY01 posting is active.
Compact controls/sort metrics/log retained in the pending execution receipt.
All-year financial controls and reporting remain pending; no new cost-point claim.

**Latest owner sequence:** Complete integration preparation and bounded checks,
make a preparation commit, wait for owner reboot, then run fresh raw
transformation/posting/reporting. Validate/iterate as needed and commit accepted
results separately; prepare a Zenodo upload script after validation. See
[derived baseline contract](../../docs/PHASE_TWO_DERIVED_BASELINE_CONTRACT.md).
The earlier pre-commit-run requirement and 280,369,115-row simple append are
superseded. Exclude 49,091 reused financial copies from the additive source set;
apply fictional attribution to eligible original historical records. Conditional
journal count 266,319,818; ledger/report counts remain unknown.

Authorized retirement of prior derived corpora completed. Original raw VA data,
v6 package, historical report tables, GL/control summaries and preparation
snapshots are retained in their declared locations. Historical report path is
preserved by symlink; about 345 GiB free after preparation. No cloud movement.
Wholly-source subset completed: 2,394,936 retained and 49,091 copies excluded.
Historical SJE normalizer, metadata, financial controls and check-default runner
implemented; 25 focused tests and unchanged 22-test baseline pass, including the
actual raw-to-sequential-reporting fixture. See the
[preparation receipt](../../data/test_evidence/phase2_derived_preparation_20261116/README.md).
Full admission/validation awaits reboot. No large run before reboot,
Q2 model work, Control read or Paper 1/2 edit.

**Owner scope correction:** The intended pre-commit full validation combines the
historical corpus and synthetic addition. Completed v6 is addition-only validation,
not the requested combined run. Expected retained reporting-input sum:
273,380,488 + 6,988,627 = 280,369,115 journal/ledger rows. Preserve both completed
receipts; do not relabel either as combined evidence. Integration must declare
population/source namespaces, identity upgrade application, historical versus SY
calendar semantics and reused-activity provenance before the run. Re-admit storage
for the combined workload; the v6-only 26.37-GiB forecast does not cover it. No
commit or additional execution yet; combined validation and paper interpretation
remain outstanding. This supersedes the broad readiness claim below.

**Completed Phase 2 v6 — ready for owner validation:**
[Full receipt](../../data/test_evidence/phase2_generation_20261116/full_v6/README.md)
and [Paper 3 addition](../../docs/tds-evidence/phase2-generation-paper3/README.md).
10,000 components/SY01–SY14; 2,444,027 financial sources, 280,000 responsibility
records, 4,888,054 SJEs and 6,988,627 reporting financial rows. One uninterrupted
financial/report workflow; all source/hash/history/conservation/reconciliation
controls pass. Original/public/actual nominal classifications match for all 49,091
reused expenses. Their 5,744 invented suppliers include 2,966 multi-year and 2,670
multi-AFO suppliers. Fifteen frozen source/identity tests and two adversarial
read-only audit tests pass. Actor IDs are independent of activity seed; no natural
ordering fields are encoded. Broader AIM inference/forecasting sufficiency and
target GenevaERS runtime gaps remain open.

Retained root 27.93 GiB, reusable inputs 6.83 GiB, free space 73.16 GiB. A
comparable same-input next pass budgets 26.37 GiB with 25% margin and leaves
46.79 GiB free, above 40 GiB. No offload/deletion needed; new Phase 3 engine/model
scopes need their own admission. Original Phase 1 results remain untouched; its
separate identity registry/utility has not been applied as a full historical replay.
No Q2 evaluation, Control read or private manuscript editing. The checkpoints
below retain chronology; their active/pending statements are superseded here.

**Full Phase 2 v6 active:** `session-run.tmp/phase2_full_v6` runs frozen generation
for 10,000 components/SY01–SY14, then one actual financial/report workflow and
source/storage assessment. Corrected v6 pilot passed; its FY31/FY32 GL and
cross-vendor aggregate report are byte-identical to v5. Population identity is now
independent of recipe seed (15 focused tests pass). Completed v5 scale admission:
350,326 sources, 700,652 SJE rows, 1,020,536 reporting financial rows, zero
reconciliation failures, 4,301,916 KiB retained. Full admitted at ~100 GiB free
with a conservative 35.9-GiB run budget and 40-GiB stage floor. Preserve frozen
main code/config/driver while active. Full/Phase 3 acceptance remain pending;
no Q2 model, historical replay, archival or deletion.

**Active Phase 2 preparation, 2026-11-16:** Owner resumed autonomous execution.
The [current plan](../../docs/PHASE_TWO_EXECUTION_PLAN.md#active-revision--autonomous-execution-resumed)
supersedes discussion pauses below. Phase 1 opaque identity registry: 65,716 old
keys, unique mapped identities, three special/unresolved keys preserved; actual
bounded post/GL equivalence tested, no historical replay. Variable calibrated
100-component/two-year generation and actual posting/reporting passed. Thirteen
focused tests pass. Read-only bounded zero-vendor activity selection is finishing;
combined two-class admission and full 14-year run remain pending. Keep the original
corpus and failed/interrupted prototype receipts. Phase 3 disk reserve is a gate;
no retained corpus deletion, forecasting model or Q2 evaluation is authorized here.

**Combined pilot accepted:** `session-run.tmp/phase2_combined_profile_v5` completed
without orchestration interruption: 100 components/SY01–SY02, 11,147 financial
sources, 22,294 SJE legs, actual posting/report reconciliation and cutoff/source
controls. Allocated root 251,860 KiB. Initial 14-year selection completed read-only;
conservative v2 retains 49,091 expenses with signed amounts and source periods.
The 10,000-component/two-year combined admission is active under
`session-run.tmp/phase2_combined_admission_v5`; full generation awaits its measured
storage/financial/report gates. Permanent compact receipt is in the Phase 2
evidence directory; no historical financial replay.

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

**Phase 2 inspection preparation from `38a41c6`, 2026-11-16:** Added
[corpus specification v1](../../docs/PHASE_TWO_CORPUS_SPEC_V1.md),
[calibration map v1](../../docs/PHASE_TWO_CALIBRATION_MAP_V1.md) and
[self-contained inspection package](../../data/synthetic/phase2_inspection_v1/README.md).
The hand-authored sample separates disconnected process evidence, proposals,
late corrections/outcomes, optional modernization fields and private truth; includes
cancellation, censoring and future scenario references. Structural CSV widths,
record references, composite party/reference uniqueness and CSV hashes checked.
This is an inspectable initial slice, not a full-scale generated corpus. Aggregate
reporting anchors calibrate workload planning; unmeasured economic distributions
remain unknown and sample amounts are illustrative. Generator, adapters, cutoff
exporter and outcome consumer remain design_only/not_implemented. Next: inspect
contracts and trace actual Scala producer/consumers before the generator fixture;
then complete coverage and resource admission before full scale. No Python,
financial runtime change, Q2 model evaluation, reporting repeat, Control access,
publication or Paper 1/2 edit. ARM writing remains independent.

**Autonomous Phase 2 execution started, 2026-11-16:** Owner authorized the plan
through full fictional generation, one financial pass, evaluation and justified
versioned iteration, with Paper 3 methodology as the writing outcome. Storage gates
must preserve Phase 3 capacity; no retained corpus deletion without explicit
approval. See docs/PHASE_TWO_EXECUTION_PLAN.md and
[data receipt](../../data/test_evidence/phase2_generation_20261116/README.md).
42 focused tests pass; unchanged pilot financial/reporting outputs pass the resumed
checker. Initial new fixture exposed/fixed a D/C-versus-canonical-D/O validator
assumption; financial engine unchanged and failed evidence retained. Reviewed
100-component/two-year driver active at session-run.tmp/phase2_profile_v2;
full generation/financial run not yet admitted. New Scala source preparation,
external sorting and cutoff/lineage consumers exist; measured financial path
remains existing Scala TTL/ARE/post/reporting. No Q2 model work, Control read,
private manuscript edit, archived-data deletion or cloud copy.

**Owner discussion pause, 2026-11-16:** Owner recalls the early no-natural-bunching
vendor-ID requirement. Current sequential canonical IDs/adjacent-component vendor
assignment fail that requirement despite stable hashed process aliases. Added a
mandatory correction/validation gate to PHASE_TWO_EXECUTION_PLAN.md before full
freeze. Owner suggests artificial IDs with a separate VA crosswalk for real
vendors; mixed-population inclusion remains under discussion, not implemented.
Clarified the proposed FY2031 envelope as an annual fictional activity total,
using shifted historical reporting calibration, not a bond maturity or forecast.
No calibrated fixture/full corpus should launch while discussion is paused;
inspect previously started admission/reporting state before any later resume.

**Synthetic year naming, 2026-11-16:** Owner favors an immediately recognizable
synthetic calendar for results review. Plan now uses short `SY01`–`SY14` display
labels. ISO backing dates, fiscal/month conventions and leap-year behavior require
an explicit versioned mapping before implementation; labels do not indicate AD 1
or an actual future forecast. Discussion pause remains in effect.

**Finance/treasury direction, 2026-11-16 discussion:** Owner proposes these as
useful component domains. Execution plan now records candidate cash/liquidity,
debt/funding, investment/counterparty and financial-planning source workflows.
These require distinct business recipes, role namespaces and temporal/outcome
contracts; no treasury engine or AIM model is implemented by the plan. Initial
slice selection remains discussion work. Execution pause remains in effect.

**Long-term treasury inquiry, 2026-11-16 discussion:** Owner adds future funding
needs and twenty-year proposed-portfolio risk under expense growth and existing
bond obligations. Plan distinguishes historical and forward horizons (illustratively
SY14 cutoff -> SY34 target), contractual liabilities, proposed assets, funding
assumptions and growth conventions. Conditional funding calculations and
probabilistic risk claims remain separate. No new projection/model/financial engine
execution; discussion pause remains in effect.

**CKB/laptop scope discussion, 2026-11-16:** Owner asks to structure expanded
preparation around bounded common-key processes and necessary resorts. Added
[proposed topology](../../docs/PHASE_TWO_CKB_GENERATION_TOPOLOGY.md); current recipe
loop is not promoted to a generalized context CKB. Completed two-year admission
covers 1,440,072 financial rows, 504,072 largest group and zero report failures;
generation/extract process peaks ~1.06 GiB RSS, retained root ~4.8 GiB. Reporting
completed after the documented launcher interruption/resume. These support tested
prototype feasibility, not full treasury/model capacity or a cost point. Identifier,
calendar, recipe diversity and new actual-consumer gates precede full generation.
No new execution while discussion remains paused.

**Zero-vendor CKB refinement, 2026-11-16:** Owner proposes agency or full agency/
fund/object breaks for large unresolved populations and recurring suppliers across
years. Topology plan now separates pre-assignment context processing from post-
assignment vendor/date CKB, with persistent supplier registry/shared memberships,
streamed large groups and explicit block-reuse lineage. Candidate key needs actual
grain/cardinality profiling; no supplier assignment or new run implemented by this
discussion. Original VA data and summary-grain receipts remain unchanged.

**Owner scope/identity clarification, 2026-11-16:** Bounded component/treasury scope
accepted; refunds/reversals optional if inexpensive. Stable vendor identity is an
explicit Phase 1 upgrade to audit/version separately and then preserve in synthetic
work. Distinguish ID recoding, supported real-identity resolution and fictional
zero-record attribution; old baseline remains retained. Plan calls for targeted
affected-result assessment, not automatic reporting repeats. Provenance should be
proportionate: stored bytes/hashes, recipe/seed/map versions, actual reused-block
mappings and financial lineage; no exhaustive random-draw log. Discussion pause
remains; no identity upgrade or new full run implemented by this note.

**Separate synthetic actors, 2026-11-16 owner clarification:** Initial corpus should
use its own stable actor identities and process/event links for traceability;
synthetic activity need not be mixed into existing VA vendors. This supersedes
candidate mixed-actor plans. Phase 1 identity upgrade stays separate; any supported
VA crosswalk belongs there, not as a required synthetic-vendor correspondence.
Aggregate/pattern calibration can inform fictional activity without transferring
actor identity. Plan updated; discussion pause remains in effect.

**Owner resumes autonomous execution, 2026-11-16:** Two synthetic classes accepted:
reassigned zero-vendor activity and wholly generated actors/activity. Both use stable
non-bunched identities; real actors receive no synthetic additions. Active plan
revision supersedes discussion pauses. Start with separately versioned Phase 1
identity audit/upgrade, then CKB-oriented reassignment and bounded operating/cash/
simple-funding recipes, SY labels, sample controls, full admission/execution and
Paper 3 evidence. No Q2 modeling or automatic old reporting repeats. No new full
corpus launched at resumption.

**Phase 1 identity upgrade bounded evidence, 2026-11-16:** Full compact mapping of
65,716 existing reporting keys unique, three special/unresolved keys preserved.
22 baseline and three focused tests pass, including actual post/GL consumption,
unchanged GL and inverse-mapped ledger business equivalence. Existing groupings
preserved without party inference. Explicit vendor-order sort precedes mapping;
financial sort restored afterward. [Receipt](../../data/test_evidence/phase1_identity_upgrade_20261116/README.md).
Full historical replay and AIM comparisons not performed. Shared stable-ID contract
now ready to integrate into the separate two-class synthetic preparation.

**Resumed preparation progress, 2026-11-16:** Shared stable actor IDs, SY source
calendar and bounded variable recipes pass unit/control checks. Actual calibrated
100-component/two-year variable sample completes financial/reporting execution:
3,451 financial sources / 6,902 SJE rows, exact annual controls, dates/posting periods,
source/rule lineage and report reconciliation pass. Receipt subset in
 data/test_evidence/phase2_generation_20261116/variable_profile_v4/.
Read-only bottom-hash source selection is active across retained Run 1 FY03–16,
5,000 eligible zero-vendor expense records/year, bounded sample state; original
financial outputs unchanged. Mixed-class unit tests cover recurring suppliers
across years/funds and joint budget conservation. Actual mixed-class/full execution
still pending frozen selection/admission. Q2/portfolio modeling remains unopened.

Owner follow-up during v2: prior four-CPU scheduling is the target after the
corrected first-year posting gate; host has four logical CPUs/two physical cores,
16 GiB RAM. Owner also questions separate ID/date passes. Code trace confirms
complete journal pairs exist before APA/PO sorting, giving a candidate pair-level
fusion point. Current financial merge cannot rekey vendor IDs without restoring
sort order. Fusion/parallel admission remain unimplemented and must pass versioned
consumer/control/materialization gates; current v2 engine/config remain frozen.
