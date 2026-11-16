# Ledger Lab Handoff

## 2026-11-16 preparation checkpoint before owner reboot

Latest owner sequence supersedes earlier pre-commit-run notes: preparation commit,
owner reboot, fresh full raw transformation/posting/reporting, validation and a
separate final-results commit; Zenodo upload script after validation. Authorized
derived-corpus retirement completed. Raw VA, v6, historical reports/GL/control
summaries remain; original report path resolves into
`session-run.tmp/retained_phase1_evidence_v1`. About 345 GiB remains after prep.

Wholly-only subset complete: 2,394,936 retained, 49,091 reused copies excluded.
New Scala normalizer assigns suppliers once per paired source, rekeys known IDs,
shifts historical clock +28 years, restores financial order. Metadata/report and
GL/row controls implemented. 25 focused tests and unchanged 22-test baseline pass;
actual raw-to-sequential-reporting fixture passes. Driver defaults to check-only;
large replay NOT launched. Evidence and resume commands:
`data/test_evidence/phase2_derived_preparation_20261116/README.md`.

Conditional journal count 266,319,818; ledger/report count unknown. The old
280,369,115 append estimate is withdrawn. V6 is addition-only evidence. After
reboot verify code/input integrity, AC/free space and materialization admission,
then one integrated workflow with passive storage monitoring. No Q2 model,
Control access, Paper 1/2 editing, cloud copy or publication. Preserve failures.

Start with [STATUS](STATUS.md), then [Track D](tracks/D-experiment/STATUS.md) and
its [CCP](tracks/D-experiment/ccp/STATUS.md) or [AIM](tracks/D-experiment/aim/STATUS.md)
workstream. This file records recent transitions, not current authority or the task list.
The [unchanged previous handoff](docs/archive/collaborator_review_2026-11-15/SESSION_HANDOFF.md)
preserves the detailed chronology through the initial consolidation at `ddde891`.
Earlier archives remain linked there; no session reasoning or evidence was discarded.

## 2026-11-15 (TRACK D) - Autonomous synthetic-reference v2 work in progress

**Full queue launched 13:53:10 AZT:** revision b540e5e72f2ecf02f0e39e4966dc80be5d081044,
driver PID 86722, exec session 56032; monitor exec session 36070 with a 20-GiB
free-space stop, own driver group 86722. Root:
`session-run.tmp/synthetic_reference_corpus_v2_20261115/`; monthly_a/b/c are three
identical financial + expanded-report executions. Config/status/driver.pid and
minute monitoring are there. Source snapshot is corrected admission inputs.
All gates passed for first launch; 225-GiB plus first-run-allocation/reserve
checks precede subsequent runs. Conditional all-three plan is one to two days.
After evaluation, storage sharing preserves all paths/bytes across repeats and
the two own admission profiles (rejected profile stays rejected).

Archival supervisor runtime `archive_after_queue.sh` waits for
THREE_EXECUTIONS_EVALUATED and then calls the verified archival/upload checker.
Destination: OneDrive-Personal/LedgerLabOutputs-SyntheticReference-v2-20261115,
separate from historical Gold. See archive_supervisor.pid/log and archive_console.log.
Do not change HEAD or engine/orchestration files while this queue runs. Documentation
updates may remain uncommitted until all executions finish. Do not send a final
response until evaluation/upload complete or a blocker requires owner input.

Latest: corrected FY2012 financial/source checks passed; 2,745,656 receipt legs
all zero, GL byte-identical to the first financial profile. The report failed on
valid literal quotes in canonical SJE fields at 11:30:33 AZT. ReportingOrder's
canonical movement/balance/opening path now matches existing literal delimiter
semantics, with strict RFC CSV retained for dimensions/references. Final 47 tests
passed at 13:20:31 AZT. Report-only replay is exec session 89527, runtime
`retry_literal_report.sh`, monitor exec session 96271. Latest PID is literal_retry.pid;
output `fy12/report_literal_v3`, summary `fy12/admission_summary_literal_v3`.
Earlier failure marker renamed PREFLIGHT_FAILED_original_quote; old monitor
storage history preserved as monitor_storage_financial_and_failed_quote.csv.
Use LITERAL_RETRY_FAILED / PREFLIGHT_COMPLETE for the latest attempt. Wait for
actual report controls/cardinalities/time/scratch before full admission.

Latest continuation: first FY2012 financial run completed at 10:19:33 AZT,
but its fresh profile falsified receipt source policy: 2,452,576/2,745,656 receipt
SJE rows mapped to legacy REV agency-name aliases. SOURCE_POLICY_FAILED marks
the first admission root; only its process group was stopped, retaining financial
outputs and partial report evidence. This is not an accepted future population.
New ReceiptSafeMaster excludes typed REV aliases in a versioned snapshot, checks
remaining names against agency keys/zero fallback, and records original/exclusion
lineage. No financial engine change. The new preparation requires all actual
receipts zero and caps IDs at 256 characters; new post-engine summary scans actual
report row/byte/zero cardinalities. 46 tests passed, followed by 4 focused checks.
Corrected replay: runtime `preflight_receipts_v2.sh`, exec session 2592, under
`session-run.tmp/corpus_admission_20261115_receipts_v2/`; PID is in preflight.pid.
Its monitor is exec session 89368. First profile sessions 93557/26250 are stopped.
Wait for corrected PREFLIGHT_COMPLETE, then evaluate live time/scratch before
launching all three runs. All 385 raw hashes passed in the first admission root.

Owner authorized three synthetic-reference executions, new reports, evaluation
and verified OneDrive archival, remaining active until complete or input is
required. After discovering the only old assignment CSV came from the excluded
producer, owner explicitly chose "Implement and verify the new preparation path."
The new contract and producer are committed; latest execution-support revision
`b98581b`. New assignments derive from actual committed financial IDs with bounded
literal-ID sorting/deduplication and explicit synthetic template cycling, never
from the old draft/candidate inventory. Excluded artifacts remain untouched.

Unchanged baseline 42 tests passed (one environment-dependent case canceled),
two new preparation tests passed. Actual lineage-v2 fixture outputs were consumed
by new preparation and existing separate reporting; 26 reconciliation, 20 history,
96 conservation checks passed. All 385 raw catalog hashes now pass freshly.
Fresh input snapshot: `session-run.tmp/corpus_admission_20261115/inputs_v2/`.
FY2012 profile started 09:50:52 AZT via runtime `preflight.sh` under that admission
root; PID 72162, exec session 93557. It will automatically finish financial
validation, new preparation, actual report and ConnectedViewsCheck. Passive
monitor exec session 26250 logs storage/process samples every minute. Full queue
must wait for PREFLIGHT_COMPLETE and measured time/scratch admission. Preflight
financial launch predates code commits; financial engine bytes were not changed.

Prepared tracked wrappers: `run_synthetic_reference_corpus_v2.sh` runs three
isolated monthly both-family executions plus reports and comparisons; verified
post-evaluation hard links retain every path/byte while conserving local storage.
`archive_synthetic_reference_corpus_v2.sh` packages only evaluated results and
checks copied hashes in a fresh OneDrive destination. Small storage/archive
exercises passed. Local sync-folder integrity is distinct from remote upload
confirmation. No corpus queue or OneDrive copy has started yet. Do not commit
new revisions while the full three-run wrapper is active; it pins HEAD.

## 2026-11-15 09:26:41 AZT (UTC-0700) (TRACK D) - Clarified next workload

Owner confirms the downloaded VA corpus execution is complete, with evidence
on OneDrive. Historical evidence remains valid within its recorded scope. The
next intended execution is the prepared synthetic-reference corpus, supporting
the Phase 1 conclusion and preparing data for Phase 2 Q2 development. Codex's
initial direct-VA interpretation in this session was incorrect. Updated the
runbook's current checklist and Track D status to prevent that repetition.
Current HEAD contains later parser/lineage/reporting changes; no claim of unchanged
code relative to a historical run was verified. OneDrive contents were not rescanned.

Pre-interruption checks: prepared reference/master/history hash list passed;
about 450 GiB free, AC power, 16 GiB RAM/two physical/four logical CPUs; process
check found no matching pipeline/JVM. Prepared root lacks INPUTS_COMPLETE and
contains assignments produced by the excluded older draft: neither those artifacts
nor the prepared FY2012 scenario are thereby admitted. Focused sbt tests did not
start because sandbox denied the sbt boot lock; no test failure or passing baseline
is claimed. Logs are under session-run.tmp/corpus_admission_20261115/.
No new code, financial profile, full-corpus launch, archive copy or Control read.
Continue synthetic admission from the current checklist; leave excluded drafts
and their existing artifacts untouched.

## 2026-11-15 09:21:57 AZT (UTC-0700) (TRACK D) - Unused assignment drafts removed

Owner authorized deletion if no active dependencies. Search found only the draft
Scala source, its uncommitted launcher invocation and historical records. Deleted
`SyntheticVendorAssignments.scala` and restored `prepare_synthetic_corpus_inputs.sh`
to HEAD, removing only the uncommitted invocation/hash additions. No active source
or script references remain; historical records are retained. Earlier instructions
to preserve these drafts are superseded. Accepted generator/report code unchanged.

## 2026-11-15 09:20:25 AZT (UTC-0700) (TRACK D) - Corpus-run session boundary

Owner will start a fresh session to complete the corpus-run checklist and, if
admitted, launch for the rest of the day. Latest measured probe/anchor checkpoint
is `6b6a561`; document cleanup/transition reflection through `b0c667d` is preserved.
Read current root/Track D status and the latest canonical reflection first. Validate
a representative current real-year zero-ID count/key/byte profile, source policies,
downstream controls and live time/scratch admission before full execution. Zero-only
3.2-3.3 h matched and 10-11 h distinct-heavy figures are conditional projections,
not whole-run guarantees. Use the runbook with current contracts; do not assume
the prepared FY2012 component generator is already implemented.

No full-corpus run has started. If gates pass, run locally with caffeinate and
background status monitoring. Owner stopped Dropbox; OneDrive is only a possible
verified post-success archive, with destination/capacity checked before copying.
Preserve excluded `SyntheticVendorAssignments.scala` and preparation-script drafts;
do not execute or stage them as accepted design. This boundary adds documentation
only, with no new execution, paper edit or Control access.

## 2026-11-15 (TRACK D) — Reflection on the approaching Phase 1 conclusion

Owner asked about the reflection journal's path and requested a short reflection
on the project transition, rather than a cleanup log. Verified the journal moved
byte-identically at `9cab0c0`; the later deletion removed only the old-path pointer.
Appended the reflection at 09:15:42 AZT, obtained from the live clock, to the
canonical `tracks/D-experiment/CODEX_REFLECTIONS.md`. Earlier entries are unchanged.
It interprets the shift from startup to one continuing experiment, with CCP/AIM
inside D and a bounded Phase 1 account approaching completion; it does not declare
phase closure or upgrade results. Added an explicit per-entry timestamp rule to
AGENTS and the contributor guide; historical missing times are not reconstructed.
No engine edits, runtime/model execution or new Control read; inherited assignment
draft remains unchanged. Existing implementation/admission gates still apply.

## 2026-11-15 (TRACK D) — Retired plans and redundant pointer files

Owner requested fewer active documents and direct archive links. Archived six
superseded documents: Track D's pre-measurement predictions, organization proposal,
early strategy, enhancement plan, engine implementation plan and expanded-source
rerun handoff. Their prose/code remain intact; only relative Markdown link targets
were rebased. [Archive index](docs/archive/retired_plans_2026-11-15/README.md) maps
old paths and current work. Removed sixteen compatibility pointer files, including
the root brief and old track/plan aliases, and rewrote maintained links directly.

Kept current protocols, workload contracts, attributed reflections, evidence and
open TODO work. Earlier frozen snapshots and historical exposure/source-comment
path mentions remain unchanged as provenance. Refreshed the catalog to the actual
active document set. No engine changes, runtime execution, new Control read or
evidence upgrade; inherited older assignment draft remains unchanged/uncommitted.
Existing Track D implementation/admission gates still govern next execution.

## 2026-11-15 (TRACK D) — First-use terminology and project-brief reading review

Owner flagged undefined SJE/CKB on the suggested reading path. Walked the root
status → overview → Track D → CCP/AIM branches and contributor guide, also checking
the README entry route. Added definitions before checkpoint detail and local
first-use explanations in standalone entry pages; clarified grain, instruments,
Control/Gold and result notation. README orientation now precedes download setup.
The overview serves as the current short project explanation. Archived the full
old PROJECT_BRIEF unchanged and left its public path as a historical pointer.

Preserved the latest lineage/anchor/probe evidence at `6b6a561`; clarified historical
v1 wording where it conflicted with the already-recorded v2 continuation. No new
engine changes, model fit, corpus execution, Control access or evidence upgrade.
Older assignment draft remains unchanged/uncommitted. Reading scope and validation
are appended to `docs/COLLABORATOR_REVIEW_2026-11-15.md`. Existing next implementation
and admission gates remain in Track D status.

## 2026-11-15 (TRACK D) — Collaborator review and primary experiment paths

Owner clarified that Track D is the primary research path, with CCP/AIM (formerly
B1/B2) inside it; external Track A belongs in the paper's conclusion and Berka/C
is at most a footnote candidate. Canonical paths are `tracks/D-experiment/`,
`ccp/STATUS.md` and `aim/STATUS.md`; old paths retain pointers and section anchors.
This reorganizes ongoing work without closing unresolved tasks or claiming new evidence.

Broader document inventory and targeted content review exposed obsolete first-run
queues and expected results presented as findings in newcomer documents. Rewrote
the overview/contributor path, labeled dated plans/results, aligned contradictory
layer descriptions with AGENTS and cataloged non-archived prose documents. Preserved
original entry pages/status/handoff snapshots and moved D reasoning records intact.
[Review receipt](docs/COLLABORATOR_REVIEW_2026-11-15.md) records coverage, checks and
remaining historical-link/content limits; this is not an audit of every factual claim.

Latest runtime evidence remains `213b20f`; latest Control digest remains `ab0f7f0`.
No engine/code change, new model fit, corpus execution, private-paper edit or Control
read. The older assignment source/launcher draft remains uncommitted and unchanged.
Next work remains the declared next versioned slice and its admission checks;
paper-writing sessions proceed independently. Open actions stay in [TODO](TODO.md).

## 2026-11-15 (TRACK D) — Reflection on the synthetic/topology conversation

Owner requested a broader reflection on this conversation's insights. Appended
“From vendor enrichment to a connected experimental foundation” to the canonical
`tracks/D-experiment/CODEX_REFLECTIONS.md`. Covers owner corrections and Codex
interpretations around cross-process components, tentative allocations and future
scenarios, financial bounds and bias, integrated phases and paper introductions,
determination/execution order, and document/context management. Qualifies the
pilot's actual evidence and unresolved lineage/anchor/scale/AI limits. Acknowledges
that consolidation/path review now exist at `ddde891`/`9cab0c0`, superseding the
older conversational statement that infrastructure had not begun. Journal/status
only; no code, run, paper source or Control disclosure.

## 2026-11-15 (TRACK D) — Synthetic continuation: bounded lineage-v2

Owner resumed implementation here after infrastructure completion. Reviewed current
entry points and pilot limitations; declared source/rule lineage v2 in the existing
pilot contract and audit register before code. Baseline 45 tests passed; final
relevant suites 48 passed. APA preserves document and source-record locators plus
configured rule version in existing SJE metadata; PO records its existing built-in
rule identity/version and source locator. Encoding is reversible and CSV-safe.
No new financial calculation, sort, ID allocator or history map; current ordered
assignment topology remains intact. Existing effective-date rule-selection behavior
is not extended by this slice.

Actual command: `bash scripts/run_component_pilot.sh --out "$PWD/session-run.tmp/component_pilot_0304_20261115_lineage_v2" --baselineRoot "$PWD/session-run.tmp/component_pilot_0304_20261115_v1"`.
Financial pipeline 03:16:04–03:18:33 AZT; PILOT_COMPLETE published. All 50 generated
inputs equal v1. All 30 financial raw locators have two balanced SJE legs with exact
document/rule versions. Monetary SJE projection and four ledger/GL files match v1;
old report/temporal/cutoff checks pass. Two separately copied fixtures corrupt
source locator or rule version without altering amounts; both fail strict checks.
Receipts/logs: `data/test_evidence/component_pilot_0304_20261115/lineage_v2/`;
auto financial archive `data/test_evidence/corpus_runs/run_20261115_031604/`.

PermutationGenerator includes provenance as dimensions, so richer metadata changes
its capacities. Checker verifies financial populations and independently derived
source/document distinct counts, not stale capacity equality. This needs query-
capability scope clarification before cost interpretation. VA anchors, corpus-scale
assignment/sort admission, payroll and AI remain open. The excluded old assignment
producer/launcher draft was neither invoked nor edited. Next: verified financial
bounds and a versioned scenario/scale slice before larger execution. No paper edits,
Control read/disclosure or full-corpus run.

## 2026-11-15 (TRACK D) — Pushed lineage, prepared anchors, measured zero-key practicality

Owner requested push/next-test preparation, then specifically required projecting
remaining zero-ID runtime on this hardware. Existing checkpoints through `5e44a8e`
were pushed. Official FY2012 General Fund anchors and six signed closing-balance
bridge items verified against G p50/L p174/177; source PDFs rendered locally after
web screenshot failure. New versioned CSVs, 1% synthetic-only panel config and
90/3,000/30,000-event ladder prepared. Three offline Scala tests pass. Food-stamp
estimate belongs to Federal Trust scope and is excluded; no observed APA tie-out
or posted adjustment is claimed. Details extend the existing pilot contract.

Owner stopped Dropbox and left OneDrive as a potential verified post-success
archive; processing stayed local, no copy/upload initiated. The component generator
still needs general calendar, anchor-order calibration, sorted uniqueness/master
emission, bounded merge fan-in and manifest-driven controls before its next run.

Zero-key report probes ran separately through actual existing post + expanded
report/reduce, using only the zero ID, fixed 1-GiB heap/four logical CPUs and
production-like buffer settings. All three profiles passed controls within probe
limits. Runtime root `session-run.tmp/report_zero_key_20261115/` has PROBE_COMPLETE.
Receipt: `data/test_evidence/component_pilot_0304_20261115/zero_key_projection/README.md`.
500k repetitive movement rows: 26.349 engine seconds; 2M at historical-like posted-
key ratio: 109.314 s; 500k distinct-heavy: 130.163 s. Extract RSS about 1.03–1.06 GiB.
Host verified 16 GiB, two physical/four logical CPUs, x86_64/Java21.

Historical zero financial inputs 185.3M; current-policy scenarios 215–222M are not
fresh counts. Matched zero-only projections 3.2–3.3 h; 2x merge-depth sensitivity
6.4–6.6 h. Distinct-heavy stress 10–11 h, deliberately higher key density, not a
forecast interval/upper bound. Retained files, cumulative writes and sampled peak
occupancy are distinct; wider real records, changed reference keys, multi-year
input and thermal/I/O contention remain sensitivities. Report source fixture
preparation is not a new TTL/financial cost point. Projection-only labels/extreme
case refined after timings; measured/final source hashes recorded separately.

Next admission gate: profile a representative current real year with current
parser/reference/source policies, zero rows, byte widths and posting/output key
diversity. Do not run full corpus just because heap is bounded, split zeros into
fake vendors, or silently reduce output. True GL-grain/common-key/materialization
alternatives need declared semantics and controls if projections are impractical.
No full-corpus run, paper edit or Control access/disclosure. Older assignment draft
remains excluded and unchanged.

## 2026-11-15 (TRACK D) — Owner-requested post-reboot corpus continuation

Owner rebooted after suspected battery throttling and requested the corpus run.
Observed AC attached and 244 GiB free; throttling cause remains unverified.
Inherited sequential-report implementation and interrupted verification. Cheapest
falsifier exposed a fixture selecting legacy views because reportProfile was
missing; added expanded profile in that test only. 18 tests across three matched
suites pass, including external-sort/sequential versus indexed equivalence.
Evidence: session-run.tmp/report_sorted_v3_20261115/reboot_tests.log.

Detached driver PID/process group 1984 started with caffeinate and minute storage/
process monitoring (20-GiB stop). Root:
session-run.tmp/report_sorted_v3_20261115/reboot_attempt/.
Launcher: parent reboot_run.sh. FY2012 sort, sequential extract/reduce and actual
ConnectedViewsCheck gate automatic corpus sorting/reporting for FY03–FY16 using
retained monthly_a financial outputs and original assignments. Stages are timed
separately, fixed 1-GiB heap/four logical CPUs; source patch/untracked sorter copied
into the attempt. Financial outputs and stopped report scratch preserved. Old
queue/archival remain stopped. No new financial execution, completed corpus
report, archival, or cost-point claim. Further independent executions/evaluation
and verified archival remain outstanding after this reporting continuation.

Post-reboot continuation update: FY2012 sequential report and ConnectedViewsCheck
passed. At 2026-11-15 23:52:30 AZT driver automatically started full FY03–FY16
ReportingSort. Owner requested keeping the turn open for periodic progress checks;
monitoring continues. Corpus analytical controls/completion remain pending.

2026-11-16 01:12:15 AZT: All fourteen report sorts passed (28 files, 273,380,488
rows, 94,710,620,118 bytes). Driver entered CORPUS_REPORT; sequential expanded
extract now active. Sort stage wall interval 79m45s; no cost-point claim. Monitoring
continues with requested Terminal dashboard. Completion/controls remain pending.

2026-11-16 04:46:40 AZT: corpus reporting continuation completed with rc=0 and
CORPUS_EVALUATED. All connected checks and final census pass; permanent receipt
in data/test_evidence/report_sorted_v3_20261116/. Owner asked status; reported
Run 1 reporting complete, further executions/OneDrive archival not started.

## 2026-11-16 (TRACK D) — Reporting evidence input to Papers 1 and 2

Owner requested concrete input to the paper revision process. Created
`docs/tds-evidence/reporting-20261116-paper-updates/README.md`, linked from the
member editing plan. Includes paste-ready paragraphs for Papers 1/2 and a phase
introduction sentence, exact process wall/CPU/RSS versus internal compute table,
financial population breakdown, output/control counts and claim–evidence map.
Separates journal/ledger input rows from distinct source transactions and report
aggregation rows; qualifies simulated references, identity gaps, comparison scope
and monitor coverage. No private manuscripts read/edited or external delivery.

## 2026-11-16 (TRACK D) — Component outcomes and risk in Q2 corpus preparation

Owner extended the self-contained corpus/AIM design to historically disconnected
component/process systems, tentative outputs later completed by evidence, and
forecasting/risk using projected recasts and temporary future references. Recorded
requirements and candidate experiments in docs/PHASE_TWO_AIM_CORPUS_REQUIREMENTS.md;
linked the canonical strategy, generation proposal, AIM and root/track status.
Distinguishes effective/availability/forecast/outcome dates, inferred linkage from
private generation truth, conditional engine arithmetic from calibrated risk,
and analytical recasts from policy-required balanced reclass journals. Proposed
source-system improvements are compared on matched underlying synthetic activity.
Owner places later AIM work in Phase 3/Papers 5–6; potential Paper 5 engine slice
reconciles tentative outputs with later outcomes. Data/model co-design is explicit;
independent mechanisms and historical cutoffs remain evaluation requirements.
Documentation only: no financial code, Python, new corpus/model execution,
private-paper edit or Control disclosure. Phase 1 writing proceeds independently.

## 2026-11-16 (TRACK D) — Source conditions precede detailed AIM design

Owner clarified that we have envisioned functionality, not extensively designed
the new AIM models/features. Updated PHASE_TWO_AIM_CORPUS_REQUIREMENTS.md,
SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md, PHASE_TWO_GENERATION_CKB_PROPOSAL.md and
AIM/Track D status. The corpus explores conditions that might support future
capabilities; sufficiency remains unproven. Renamed the prototype matrix to
“Envisioned AIM functionality and possible evaluation” and qualified prior
co-development language. Documentation only; no change to historical AIM results.

## 2026-11-16 (TRACK D) — Commit and push for ARM writing handoff

Owner requested a shared checkpoint so Paper 1/2 editing can proceed on ARM while
synthetic preparation continues here. Reviewed the pending reporting implementation,
compact financial/reporting evidence and Q2 design documents. Reporting source
hashes match the completed execution; sorter equals the retained source. Existing
18-test fixture evidence and full-corpus controls pass; Bash syntax and whitespace
checks pass. Explicitly include ignored compact evidence logs so the paper
addendum's citations survive a fresh checkout. Large runtime datasets stay local.
ARM entry is the member editing handoff plus reporting addendum, then the separate
members/research review protocol. No private manuscript edit or model execution.

## 2026-11-16 (TRACK D) — Phase 2 specification and inspectable sample

Continued from `38a41c6`; read root/Track D status, AIM requirements, linked
generation proposal, architecture gates and verification protocol. Added
PHASE_TWO_CORPUS_SPEC_V1.md, PHASE_TWO_CALIBRATION_MAP_V1.md and static fictional
CSV package data/synthetic/phase2_inspection_v1/ with capability matrix and hashes.
Checked CSV widths, internal local/supersession links, composite party/reference
uniqueness and SHA-256 verification. No production code changed, so no engine
baseline replay was needed. Reporting aggregates support workload calibration,
not missing-party recovery or empirical probabilities. Source adapters, generator,
cutoff-safe export and outcome consumers remain absent; full-scale generation
requires subsequent fixture/coverage and disk/time admission. Next session should
inspect this slice before implementing Scala source generation and actual adapters.
Keep private truth excluded from any historical evidence export. No Python, Q2
model evaluation, further reporting repeat, Control read or private manuscript edit.

## 2026-11-16 (TRACK D) — Discussion pause and vendor-ID correction

Owner paused execution to discuss early non-bunching requirement. Sequential
canonical vendor IDs and adjacent-component supplier assignment do not satisfy it;
add explicit replacement/validation gate before full generation. Owner proposes
artificial IDs plus a map to VA vendor IDs for real actors; do not infer permission
to mix fictional behavior with real identities. FY2031 envelope means fictional
annual family total calibrated from FY2003 reporting, not bond maturity. Calendar
shift and 1% scale are draft choices. Discussion continues; no new calibrated/full
run launched. Inspect previously started admission/reporting state before resuming.

## 2026-11-16 (TRACK D) — Owner clarification of Phase 1 identity scope

Bounded operating/cash/simple-funding scope accepted. Refund variety optional,
not a new gate. Owner asks to fix stable vendor identity as a separately versioned
Phase 1 upgrade and preserve it in synthetic preparation. Audit current guarantees
before implementation; distinguish recoding from real-identity inference and from
fictional attribution of unresolved records. Keep original baseline/evidence and
assess affected AIM/Phase 1 claims before any targeted re-evaluation. Discussed
proportionate ancestry: frozen dataset/hashes suffice for rerunning that dataset;
seed/rules/maps aid debugging and controlled regeneration, copied blocks need
source-family lineage for future split integrity. No per-random-draw audit needed.
Execution remains paused for discussion; no new full run or identity change.

## 2026-11-16 (TRACK D) — Autonomous two-class preparation resumed

Owner explicitly resumed execution; earlier pause statements are historical.
Current execution plan covers combined 100-component pilot, 10,000-component
two-year admission, complete SY01–SY14 generation and one new-corpus financial/
report pass, then source/results/storage assessment and Paper 3 handoff. Phase 1
opaque identity upgrade has its own registry/evidence (65,716 source keys, three
special/unresolved keys preserved, bounded actual post/GL equivalence); no old
financial replay. Variable calibrated source pilot passed actual financial/report
controls. Combined source tests also reject unsupported supplier categories and
missing contractual schedules. Full read-only source selection is active under
session-run.tmp/reassigned_activity_selection_v1; its .attempt must never be
consumed. Once complete, conservative eligibility filtering produces v2 input.
No Python, Q2 model evaluation, old reporting repeats, retained deletion or private
manuscript edits. Root/Track D statuses and execution plan now carry current links.

Full v6 is now active at session-run.tmp/phase2_full_v6 (tool session 37392).
Freeze main code/config/driver. Corrected cross-seed population identity passed
15 focused tests and actual two-year pilot; FY31/FY32 GL and cross-vendor totals
byte-identical to v5. Resource admission v5 finished at 4,301,916 KiB, 350,326
sources / 700,652 SJE / 1,020,536 report financial rows, zero reconciliation errors.
Full admitted with ~100 GiB free, 35.9-GiB conservative budget, 40-GiB floor.
Monitor until full controls complete, then update Paper 3 methods/results receipt
and actual Phase 3 disk budget. New full/model completion is not yet claimed.

Full generation finished: 2,444,027 financial sources plus 280,000 responsibility
records, 832.23 s process wall, 1,234,575,360-byte maximum RSS, ~6.9 GiB inputs.
Both cutoff inspections passed all 475 hashes; source assessment completed.
Full financial years are now processing sequentially. After the frozen workflow
finishes, implement/run the read-only audit in
docs/PHASE_TWO_REASSIGNED_AUDIT_CONTRACT.md to close reused AFO preservation and
measure actual supplier multi-year/context recurrence. Existing checker lacks
explicit AFO assertions. Cheap FY31 spot check passed; do not promote that to full
closure. No main code edits until full driver ends. Then finish compact receipts,
Paper 3 results, disk/Phase 3 admission, Track D/root status and final validation.

All fourteen financial years completed successfully; whole-corpus exact financial
lineage check is active (then reporting sort/report/control/census). Keep waiting
on tool session 37392 or inspect STATUS; do not replay financial years. Full source
conditions: 140,000 operating context-years, 7,475 reassigned AFO contexts,
420,000 outcomes (344,819 observed / 25,181 partial / 17,500 canceled / 32,500
unobserved). Registry coalesces 49,091 reused rows into 5,744 fictional suppliers.
After full workflow, the new audit can select actual reused SJEs with the
actor-ordered R registry and vendor-ordered canonical streams per FY, then sort
only the 49,091 reused debits by locator. Avoid sorting all actual debits or a global
party map. SourceLineage and financial_events share event order, enabling a bounded
zip bridge to original selected source rows in event-ID order. Explicit AFO audit
and recurrence counts remain to be implemented only after frozen driver ends.

## 2026-11-16 (TRACK D) — Full v6 complete; owner validation next

Full driver and read-only audit finished successfully. No jobs need resuming and
no financial replay is needed. Root session-run.tmp/phase2_full_v6 contains the
immutable inputs, actual fourteen-year outputs, complete report and audit.
2,444,027 sources / 4,888,054 SJEs / 6,988,627 reporting financial rows; zero
reconciliation failures. Both cutoffs verify 475 package hashes. All 49,091 reused
original/public/posted nominal classifications match; 5,744 fictional suppliers,
2,966 multi-year and 2,670 multi-context. Fifteen frozen source/identity tests and
two adversarial audit tests pass; original completed source hashes still match.

Permanent receipt: data/test_evidence/phase2_generation_20261116/full_v6/README.md.
Paper 3 copy-ready methods/parameter/results addition:
docs/tds-evidence/phase2-generation-paper3/README.md. Execution plan has a current
completion table above the retained discussion history. Owner validation can
motivate a scoped new version; no present integrity defect requires another full
generation or pass. Broad model/feature design and Q2 evaluation remain future.

Retained root 27.93 GiB; reusable inputs 6.83 GiB; free 73.16 GiB. Additional
comparable same-input pass budget 26.37 GiB with 25% margin leaves 46.79 GiB free,
above 40 GiB. No offloading/deletion needed; re-admit newly designed Phase 3
engines/models separately. Old corpora, observed Phase 1 results and Papers 1/2
remain intact. Phase 1 identity registry/utility prepared, not historically replayed.
Scala remains source/financial/control substrate; Bash orchestrates; no Python.

## Owner correction — combined historical plus synthetic run still required

The agent interpreted “full” as the whole synthetic addition. Owner clarifies
historical plus synthetic, over 100M records. Historical reporting input
273,380,488 + v6 6,988,627 = 280,369,115 financial journal/ledger rows. Synthetic
source count 2,444,027 is a different grain. Preserve v6 as successful addition-
only evidence; do not claim integrated validation or commit readiness. Root/Track
status, plan and Paper 3 draft now carry this correction. No new execution started.
Combined producer/consumer, identity/calendar/provenance contracts, bounded fixture,
storage admission/offload plan and actual reporting evaluation remain required.
Current ~73 GiB free; prior 26.37-GiB next-pass forecast applies only to v6 and must
not be reused for historical+synthetic. No Q2 model evaluation or deletion approval.
