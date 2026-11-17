# Ledger Lab: Status (read this first)

Updated 2026-11-17 (Arizona). Start here, then read the relevant track status.
The audit register wins any evidence disagreement.

## Terms used on this path

| Term | Meaning here |
|---|---|
| SJE | Standard Journal Entry: the common record format for accounting lines. A journal group contains debit/credit lines that balance; an individual row need not balance by itself. |
| CKB | Common Key Buffer processing (also called common key buffering): bring sorted input streams together by a shared key and process the current key's records and state together. |
| GL / FY / VA | General ledger / fiscal year / Virginia. |
| CCP / AIM | Cost Curve Point identifiers / AI-model evidence test identifiers; the two workstreams inside Track D. |
| Fixture / lineage | A small declared test dataset / the recorded links from an output back to its source records and rules. |
| Control / Gold | The separately implemented comparison system / verified reference outputs retained for comparison. |

## Current checkpoint

**2026-11-17 allocation-feedback assessment:** Keep the current reporting
configuration unchanged. Financial controls passed for all 14 years. All 28
reporting sorts passed external row/path/size checks: 286,277,734 rows.
Extraction is live (JVM82547/session84820), dashboard97164 running, confirmed
2026-11-17T11:38:36Z. Input reads and report partials advanced across observations;
this is progress evidence, not completed-row acceptance. Actual storage admission
passed with40GiB reserve intact.
Final reporting acceptance remains pending. Owner explicitly approved verified
lossless archival of89 selected inactive-attempt files; the archive receipt now
contains `ARCHIVE_COMPLETE`. This supersedes the older archival rejection and
pause descriptions below. The [next-phase plan](docs/STREAM_LAYER_EVOLUTION_AND_ALLOCATION_FEEDBACK_PLAN.md)
separates reusable core preparation from allocation/analytic iterations and
requires final-rule verification using the newly generated receiver basis.
Three-hour iterations/eight daily cycles are planning assumptions, not timings
of an implemented allocation loop. No live financial changes or AIM execution.

**Owner-authorized reporting restart:** All14 financial years and shared controls
passed, totaling266,319,818 journal rows. Metadata reference-schema repair now
passes74 tests across12 suites in two runs, including the unchanged financial
baseline and raw-to-report fixtures. All financial compiled bytes remain frozen;
only the dimensions adapter differs. Fresh reporting continuation is being
started from successful partitions; no financial replay. Reporting and final
deliverables remain pending. No archival/deletion is authorized by this restart.

**Paused by owner; financial replay passed, reporting not resumed:** All14 years
and shared controls passed:261,529,946 historical +4,789,872 wholly generated =
266,319,818 journal rows. Driver then failed in dimensions on historical5-column/
generated3-column fund headers. The tested metadata repair is now applied to
production source;46 tests across6 suites passed. Changes are uncommitted; no
financial/test JVM remains live at the diagnostic check. Further baseline checks,
reporting, result review and final deliverables remain pending. Proposed archival
was rejected by automatic approval review; no attempt files were archived/deleted.
This supersedes the active-worker and unapplied-patch checkpoints below.

**Twelve years accepted; final two active:** FY03–FY14 passed native and combined
GL/row/observer controls. FY15/FY16 now run as two verified live JVMs in the same
session11522/group42994. Final-wave admission:210,080,804KiB free,
90,225,074KiB worker reservation plus40GiB reserve. Metadata repair remains
isolated/unapplied; apply/test it only after all financial workers and shared
controls finish, then use the prepared fresh reporting continuation. Full
financial/report acceptance and final deliverables remain pending.

**Full-run continuation:** FY04 and FY05 financial controls have passed; FY03/06
still posting. Metadata preflight found historical fund references have5 columns
but generated have3; current dimensions code would reject them. A prefix-schema
repair and regression pass3 tests in an isolated build only. Frozen production
classes remain unchanged. Apply/check this metadata-only patch after financial
processing, then continue reporting; full acceptance remains pending.

**Full fused replay active:** Frozen financial build `a0dae7c`, driver/evidence
checkpoint `da99dfe`; session11522, root `session-run.tmp/phase2_fused_baseline_v1`.
Four live FY03–FY06 workers confirmed in process group42994; wave reserve
116,415,527KiB with315,384,272KiB free and40GiB reserve. Dashboard window504 now
selects this full run. Full financial/report acceptance and validated-results
commit/Zenodo script remain pending; no publication/Q2/cost-point claim.

**Real fused FY03 admission passed:** `a0dae7c`, v3/session11936 exit0. Exact
native and combined GL controls pass; 13,850,802 journal rows observed once by
posting/GL/permutation; 1,105,396 ledger rows. Peak RSS709,038,080 bytes within
the declared2GiB heap profile. Full14-year bounded replay/reporting is next;
no full-population acceptance or cost-point claim yet.

**Corrected fused FY03 admission restarted:** Frozen `a0dae7c`, execution
session 11936, root `session-run.tmp/phase2_fused_admission_v3`. Starting resource
gate passed; source processing active and live dashboard switched to v3. Strict
native GL, combined controls and resource acceptance remain pending. This
supersedes the active-run descriptions below; no full-year or speedup claim.

**Hot-path fix ready for restart:** First fused admission deliberately stopped
after stack samples identified repeated date-parse exceptions. Equivalent ISO
dispatch and bounded date/period memoization pass 47 tests. Restart real FY03
from a new frozen build; no full financial/resource acceptance yet. No cost claim.

**Real fused FY03 admission active, 2026-11-16 21:40 AZT:** Frozen `f53ac26`,
session 37834, `session-run.tmp/phase2_fused_admission_v1`. Input hash/AC/resource
checks pass; source ingestion is active with native controls and pre-sort paired
normalization. Live dashboard selects this profile. One year only; full financial
controls, resource acceptance, multi-worker/full corpus and reporting still pending.

**Fused financial fixture verified:** 45 focused/baseline tests pass. The complete
bounded source/sort/post/GL/permutation workflow and native/combined controls are
implemented. Real FY03 memory/scratch/control admission is next, followed by full
generation proof with bounded scheduling and reporting. No cost-curve claim.

**Owner requests reorganization now:** Large v2 replay stopped deliberately before
completion. Work proves synthetic generation, not Paper 1 cost curves. Bounded
implementation/verification of the fused TTL/ARE/shared-sort/posting topology is
now the priority; see Track D and `docs/PHASE_TWO_FUSED_PROCESS_CONTRACT.md`.
Preserve independent baseline profile and interrupted evidence. No large run active.

**Fresh corrected replay active, 2026-11-16 20:55 AZT:** Full v2 attempt runs
from committed `465fe21` at `session-run.tmp/phase2_derived_baseline_v2`, beginning
fresh FY03 raw transformation. Frozen hashes/AC/300-GiB starting gate pass
(~326 GiB free). Unified detailed dashboard is open and selects v2 through
`session-run.tmp/phase2_derived_baseline.current`. Full controls/report acceptance
pending. Owner has resumed the application's persistent goal; autonomous agent
follow-through and the financial driver are both active.

**Corrected replay preparation, 2026-11-16 evening AZT:** First attempt stopped
at SY01 posting: the Bash handoff omitted `--seedFromCommitted`, so its isolated
posting generation received no merged journals. No final financial/report claim.
The orchestration-only fix passes an actual Bash-to-Scala handoff test and all
23 baseline/integrated Scala tests. Owner resumed work after pausing. Fresh
attempt `phase2_derived_baseline_v2` is next; preserve v1 failure/normalization
evidence. The unified dashboard now uses historical/generated/combined rows and
Pay JEs, PO JEs, Merge, ID/Date, Post, GL Bal and Permute columns.

**Post-reboot replay started, 2026-11-16 15:26 AZT:** Prepared `3f2e3ab`
driver is active at `session-run.tmp/phase2_derived_baseline_v1`, beginning
historical FY03 transformation. Frozen input hashes, AC and starting disk gate
pass (~346 GiB free); process/storage monitoring and stage reserves enabled.
Full validation remains pending. Track D records startup details; reboot-wait
instructions below are superseded by this launch.

**Latest owner sequence — preparation, commit, reboot, full raw replay:** Finish
integration preparation and its bounded checks, commit that preparation, then
leave large execution stopped for owner reboot. After reboot run one fresh
historical-plus-wholly-generated transformation/posting/reporting baseline;
validate and commit final results separately. Create the Zenodo upload script
after validation. No Q2 model evaluation. The
[derived baseline contract](docs/PHASE_TWO_DERIVED_BASELINE_CONTRACT.md) supersedes
the pre-commit-run and simple-append arithmetic below: reused financial copies
replace historical attribution rather than add activity. Conditional expected
journal rows: 266,319,818; ledger/report row counts await actual posting.

Owner-authorized prior derived-corpus retirement completed. Raw VA originals,
v6 inputs, prior GL/control summaries and successful historical reports remain.
Report paths resolve through a preserved alias into
`session-run.tmp/retained_phase1_evidence_v1`. About 347 GiB free at the resumed
check. Integration preparation now passes 25 focused tests and the unchanged
22-test baseline, including a fresh raw-to-sequential-reporting fixture.
[Preparation receipt](data/test_evidence/phase2_derived_preparation_20261116/README.md)
contains resume instructions. Full admission/validation awaits reboot; no large
run started.

**Scope correction — combined-corpus validation pending:** Owner clarifies that
the pre-commit full run must include retained historical data plus the new synthetic
population. V6 validated only the synthetic addition. Its controls remain valid,
but it does not satisfy the combined run. Retained historical reporting input is
273,380,488 rows; v6 adds 6,988,627: expected combined financial reporting input
280,369,115 rows before identity/calendar integration adjustments. These include
journal and ledger records, not independent source transactions. Keep package
uncommitted; combined contract, storage admission, execution and interpretation
remain pending. The 26.37-GiB next-pass estimate below applies only to v6 alone.

**Phase 2 v6 completed, 2026-11-16 — ready for owner validation:** Full
10,000-component/SY01–SY14 corpus generated; one uninterrupted financial/report
workflow passed: 2,444,027 sources, 4,888,054 SJEs, 6,988,627 reporting financial
rows, zero reconciliation failures. Read-only audit confirms all 49,091 reused
classifications and supplier recurrence. Fifteen frozen source/identity tests and
two post-audit tests pass. [Full receipt](data/test_evidence/phase2_generation_20261116/full_v6/README.md),
[Paper 3 addition](docs/tds-evidence/phase2-generation-paper3/README.md).
73.16 GiB free; a comparable next pass fits with margin and the 40-GiB reserve.
No Q2 models, old reporting repeat, retained-corpus deletion or Paper 1/2 edits.
Historical launch/discussion checkpoints below are superseded by this receipt.

**Phase 2 autonomous preparation resumed, 2026-11-16:** The owner-approved
[execution plan](docs/PHASE_TWO_EXECUTION_PLAN.md#active-revision--autonomous-execution-resumed)
now covers two synthetic actor classes, a separately versioned Phase 1 identity
upgrade, SY dates, bounded operating/treasury sources, combined admission, full
generation and one new financial/report pass. The opaque identity registry and
bounded tests are complete; combined/full acceptance remains pending. Historical
reporting is preserved. No Q2 model evaluation or Paper 1/2 edits.

**Q2 preparation, 2026-11-16:** The working design is a complete fictional
Phase 2 corpus, calibrated using the reporting evidence and inspected before
experimental freeze. [AIM corpus requirements](docs/PHASE_TWO_AIM_CORPUS_REQUIREMENTS.md)
cover disconnected component/process records, later completion of tentative
outputs, and future-reference forecasting/risk. Phase 1 paper edits proceed in
separate sessions. These requirements are design_only; no new corpus/model run.

**2026-11-16 04:46 AZT — completed:** Full FY03–FY16 sorted/sequential reporting
for retained Run 1 finished with rc=0; all checks pass for 273,380,488 financial
rows. [Receipt](data/test_evidence/report_sorted_v3_20261116/README.md).
Further independent executions and OneDrive archival have not started. This
supersedes the active/pending reporting entries below; no new cost-point claim.

**2026-11-16 01:12 AZT:** All FY03–FY16 reporting sorts passed: 28 files,
273,380,488 rows, 94,710,620,118 bytes. Full-corpus sequential expanded reporting
is now active under `session-run.tmp/report_sorted_v3_20261115/reboot_attempt/`
(driver 1984). FY2012 admission checks passed. Corpus report/reduction/controls
remain pending; sleep-interrupted exploratory timing, not a clean replicate.

**Post-reboot restart:** owner requested corpus continuation. Detached sorted/
sequential reporting driver 1984 runs the FY2012 control gate, then automatically
FY03–FY16 reporting from retained Run 1 financial outputs. 18 verification tests
pass. Live root: `session-run.tmp/report_sorted_v3_20261115/reboot_attempt/`.
Caffeinate and 20-GiB scratch stop enabled; AC attached. Completion pending;
the historical queue below remains stopped and is not the active driver.

**Running 2026-11-15 13:53 AZT:** Three admitted synthetic-reference v2 executions
with expanded reports are active on frozen b540e5e, caffeinate and monitored
scratch. [Track D](tracks/D-experiment/STATUS.md) has the root/status pointers and
source-policy/quote-boundary fixes. Historical VA results remain separate. Planned
OneDrive archival waits for successful evaluation of all three; nothing copied yet.

**Autonomous synthetic-reference preparation, 2026-11-15:** The downloaded VA
corpus results are already complete and retained for their frozen scope. Owner
authorized three new synthetic-reference executions with expanded reporting,
evaluation and verified OneDrive archival, including a new preparation path to
avoid the excluded older assignments. New bounded preparation and actual connected
report controls pass; FY2012 current-policy time/scratch admission is running.
See [Track D](tracks/D-experiment/STATUS.md) and the
[v2 contract](docs/SYNTHETIC_REFERENCE_CORPUS_V2_CONTRACT.md). Full queue admission
is pending; this is not the wider component generator or a new cost-point claim.

**Latest bounded continuation:** lineage-v2 verifies exact source/document/rule
provenance on the FY2003/04 pilot, with unchanged ledger/GL and 48 passing tests.
[Receipt](data/test_evidence/component_pilot_0304_20261115/README.md#lineage-v2-continuation-after-consolidation-2026-11-15).
Corpus-scale generation and VA financial anchors remain open; no new cost point.

**Prior pilot checkpoint: `213b20f`.** The bounded FY2003–FY2004 synthetic component
pilot completed generation, sorted assignment, posting and expanded reporting.
[Evidence and limits](data/test_evidence/component_pilot_0304_20261115/README.md):
32 baseline tests and four generator tests passed; 30 financial events produced
60 SJE rows in balanced journal groups. At that v1 checkpoint, source/rule lineage
was partial; the bounded v2 checks above subsequently passed. VA anchors remained
unverified, and scale/AI effectiveness untested. This is fixture evidence, not a cost point.

Initial Track D consolidation: `ddde891` ([receipt](docs/archive/consolidation_2026-11-15/README.md)).
The broader collaborator review/path reorganization is recorded in its
[review receipt](docs/COLLABORATOR_REVIEW_2026-11-15.md) and [document map](docs/README.md). Those infrastructure commits made no engine changes or
workload executions; subsequent probes are recorded below. The older assignment producer and its launcher
edits remain uncommitted and excluded.

## Current direction

The zero key is a placeholder for records without a resolved vendor identity;
many such records can form one large reporting group. Bounded probes measured
reporting practicality for that group. Conditional zero-only runtime is hours,
with strong distinct-key sensitivity. See
[receipt](data/test_evidence/component_pilot_0304_20261115/zero_key_projection/README.md).
A representative current-year profile of zero rows/keys/bytes is required before
full-corpus admission; spill capability alone is insufficient.

Next test preparation: FY2012 General Fund facts and closing-balance bridge
verified, with three offline checks passing; proposed scale ladder is not yet run.
See [Track D](tracks/D-experiment/STATUS.md) for its implementation/admission gates.
Latest tested implementation `5e44a8e` is pushed. Active processing stays local;
OneDrive is only a potential verified post-success archive.

- Phase 1: direct VA corpus; revise Papers 1/2 from results independently.
- Phase 2: vendor-centered synthetic work, likely Papers 3/4 on improved cost
  curves and AI models. Use the [strategy](docs/SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md)
  and [generation design](docs/PHASE_TWO_GENERATION_CKB_PROPOSAL.md). Further
  versioned design/inspection may precede formal corpus/protocol freeze.
- Phase 3: broader CKB topology, tentative allocations through reclass. These
  engine demonstrations are not prerequisites for current generation.
- Paper editing: use the [member handoff](tracks/D-experiment/MEMBER_REPO_EDITING_PLAN_2026-11-14.md)
  in separate writing sessions, including introductions at phase boundaries.
  Private sources remain in the member repository; owner review precedes circulation.

## Primary experiment and supporting material

| Path | Role | Start page |
|---|---|---|
| **D — primary experiment** | Shared design, synthetic inputs, papers and Control comparison | [D status](tracks/D-experiment/STATUS.md) |
| D / **CCP** (formerly B1) | Cost and capability measurements | [CCP status](tracks/D-experiment/ccp/STATUS.md) |
| D / **AIM** (formerly B2) | AI tests and reusable financial exploration | [AIM status](tracks/D-experiment/aim/STATUS.md) |
| A — conclusion material | External private insurer work; qualified results may inform the paper's conclusion | No local execution/status page |
| C — footnote candidate | Berka exploratory record, outside the active experiment | [Historical C status](tracks/C-berka/STATUS.md) |

Owner direction, 2026-11-15. This reorganizes ongoing work; it does not close
unfinished CCP/AIM tasks or claim Track A corroboration. Former B1/B2 and
`D-control` labels are historical; current links use the canonical Track D paths.
Evidence locations and protocol IDs remain stable.
The [contributor guide](CONTRIBUTOR_ONBOARDING.md) provides a bounded reading path;
the [catalog](docs/DOCUMENT_CATALOG.md) helps find a particular document.

## Session entry points and safeguards

[TODO.md](TODO.md) holds open actions; [SESSION_HANDOFF.md](SESSION_HANDOFF.md)
is the dated log. [Audit register](docs/VERIFICATION_AUDIT_REGISTER.md) holds
claim evidence; [CURRENT_RESULTS.md](docs/CURRENT_RESULTS.md) is a dated results
summary, not a replacement for newer receipts. Read [AGENTS.md](AGENTS.md) before
editing: Scala financial engine, Bash orchestration, Python only with owner approval.
Update the working track status and add a short handoff before ending a session.
Read only relevant documents; control access follows Track D's digest procedure.

Baseline tag `companion-measured-baseline-20261111` identifies `c3667e4`.
[Baseline/exposure record](docs/COMPANION_BASELINE_RECORD_2026-11-12.md) preserves
comparison limits. The three expanded Monthly executions are repeated measurements
at one point, not new cost points. Their generated datasets were retired by owner
on 2026-11-15; evidence/logs/Gold retained ([receipt](data/test_evidence/corpus_cleanup_20261115/README.txt)).
452 GiB free was a historical receipt, not current admission evidence.

Work proceeds on `main`; old branches are history. Graph names remain Events,
Annual, Monthly, Cubes, Posted, Permuted; process names Posted GL, Ledger, Views,
Revaluation, Allocation. CCP tags remain lookup aliases.

Prior status and superseded sequencing are [preserved unchanged](docs/archive/consolidation_2026-11-15/STATUS.md).
