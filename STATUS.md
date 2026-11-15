# Ledger Lab: Status (read this first)

Updated 2026-11-15 (Arizona). Start here, then read the relevant track status.
The audit register wins any evidence disagreement.

**Current checkpoint: `213b20f`.** The bounded FY2003–FY2004 synthetic component
pilot completed generation, sorted assignment, posting and expanded reporting.
[Evidence and limits](data/test_evidence/component_pilot_0304_20261115/README.md):
32 baseline tests and four generator tests passed; 30 financial events produced
60 balanced SJE rows. Source/rule lineage remains partial, VA anchors unverified,
and scale/AI effectiveness untested. This is fixture evidence, not a cost point.

Initial Track D consolidation: `ddde891` ([receipt](docs/archive/consolidation_2026-11-15/README.md)).
The broader collaborator review/path reorganization is recorded in its
[review receipt](docs/COLLABORATOR_REVIEW_2026-11-15.md) and [document map](docs/README.md). No engine changes or
larger execution in this session. The older assignment producer and its launcher
edits remain uncommitted and excluded.

## Current direction

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
unfinished CCP/AIM tasks or claim Track A corroboration. Old B1/B2 and `D-control`
paths are compatibility pointers. Evidence locations and protocol IDs remain stable.
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
