# Repository consolidation receipt, 2026-11-15

Documentation-only continuation from `213b20f`, after the bounded component pilot.
No engine/code changes, larger corpus run, paper editing, Control access or evidence
maturity upgrade. No financial fixtures, measurements, failed results or attributed
reflections were removed. Mainline remains Scala with Bash orchestration.

## Authority and retained history

[Current map](../../README.md), [root status](../../../STATUS.md) and
[Track D status](../../../tracks/D-control/STATUS.md) now point to one roadmap,
one generation proposal and one current member editing handoff. Current contracts
and evidence retain their original locations. Historical broad strand ideas are
retained for future selection, not silently marked completed or canceled.

These snapshots are byte-for-byte copies of the checkpoint documents, at their
original repository-relative paths below. They preserve conflicting decisions and
corrections as history. **Their relative links retain the original file's base**;
use the current document map to navigate, or view the original file at `213b20f`.
They are frozen records, not maintained navigation pages or execution instructions.

- [Root status](STATUS.md)
- [TODO](TODO.md)
- [Track D status](tracks/D-control/STATUS.md)
- [Broad synthetic/engine strand plan](docs/PHASE2_SYNTHETIC_DATA_PLAN.md)
- [Intel Phase 1b assignment](docs/PHASE_1B_SYNTHETIC_DATA_INTEL_PLAN.md)
- [Earlier paper revision plan](tracks/D-control/PAPER_VERSION_2_RESEARCH_PLAN.md)

Old plan paths remain pointers. Current scope follows integrated Phase 1/2/3
numbering and independent paper-writing sessions. Phase introductions are already
in the member handoff. No old Python approval statement is expanded by cleanup.
Track B/C statuses, dated results packages and comparison evidence are not rewritten
as newer measurements. Detailed Control proposals remain in the prior D status;
current D status retains unresolved decisions and digest/exposure boundaries.

## Excluded working-tree draft

Preserved uncommitted and unstaged, with pre/post SHA-256:

| Path | SHA-256 |
|---|---|
| `scripts/prepare_synthetic_corpus_inputs.sh` | `832ddeb278601bcded88d553aeac12cad6e207564bc92af22db52a3900418d74` |
| `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/SyntheticVendorAssignments.scala` | `ed97c71ca7ad8a26cbd42f15d133c8e49f4301d279a778eff3cc430e0fc528d3` |

## Validation scope

Check snapshot equality against `213b20f`, local Markdown destinations/fragments
in the maintained consolidation entry points, whitespace/diff scope and draft
hashes. Frozen snapshots and unchanged dated evidence have historical links and
are excluded from maintained-link validation. No engine tests rerun: executable
behavior is unchanged; the pilot receipt continues to record prior runtime tests.

Validation completed: all six snapshots match `git show 213b20f:<original path>`
byte-for-byte; 133 local Markdown destinations/fragments passed across 11 maintained
entry points (root/D status, document map, three replacement pointers, strategy,
generation proposal, this receipt, comparison plan and member editing handoff).
Both excluded draft hashes match the pre-edit values; `git diff --check` passed.
Historical snapshot links are intentionally interpreted at their original bases.
