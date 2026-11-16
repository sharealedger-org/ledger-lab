# Ledger Lab: Status (read this first)

Updated 2026-11-16 (Arizona). Start here, then read the relevant track status.
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
