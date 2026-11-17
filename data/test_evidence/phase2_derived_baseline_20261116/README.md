# Integrated derived baseline: execution in progress

V1 subsequently **failed at posting**. Its isolated posting generation did not
receive the merged journals because the driver omitted `--seedFromCommitted`.
The prior Scala integrated fixture did not exercise this Bash generation handoff.
Failure logs and the captured driver are retained under `failed_v1/`; completed
raw/normalization evidence remains valid only for those scopes. V1 is not accepted.

Owner paused and then resumed work. The orchestration-only correction passes the
real Bash handoff test (`posting_handoff_test_v2.txt`) and all 23 baseline/integrated
Scala tests (`baseline_recheck.txt`). The handoff consumes 1,520 journals, produces
ledger/GL/permutation and leaves merged input bytes unchanged. Initial test-harness
function loading failed before posting; a saved function file replaced process
substitution and the actual test passed. No new Python or Scala financial logic.
Fresh full attempt v2 is planned after this correction checkpoint; do not combine
failed v1 timing with v2 as an uninterrupted replicate.

Started 2026-11-16 15:26 AZT after owner reboot, on prepared commit `3f2e3ab`.
This receipt is **pending**, not full acceptance or a new cost point.

Contract: [derived baseline](../../../docs/PHASE_TWO_DERIVED_BASELINE_CONTRACT.md).
Preparation: [bounded evidence](../phase2_derived_preparation_20261116/README.md).
Runtime root: `session-run.tmp/phase2_derived_baseline_v1`.
Execution session: 25831; driver PID at launch: 1513.

The committed Bash driver invokes Scala raw TTL/ARE, paired source-group historical
identity/calendar normalization, financial merge, posting/GL, reporting external
sort and sequential expanded reporting. Inputs are original historical sources
and 2,394,936 wholly-generated raw events. The 49,091 sampled reused events are
excluded from additive generation; fictional attribution replaces eligible
historical attribution instead. Conditional expected journals: 266,319,818.
Actual ledger/report cardinalities remain unmeasured until processing completes.

Admission: check-only passed, run-mode historical/subset hash verification passed,
AC attached, 363,024,400 KiB free at the first stage (300-GiB starting gate).
Caffeinate and minute process/storage monitoring started; each stage checks the
40-GiB reserve. Cloud/file-provider startup contention was directly observed;
this is exploratory timing, not a clean replicate. The first detached launcher
exited before creating an attempt; the persistent execution is the actual run.

Unified read-only dashboard: `bash scripts/dashboard_full_corpus.sh`; explicit
selection `--derived [absolute-root] [seconds]`, original view `--legacy [seconds]`.
Default refresh is 30 seconds. Syntax and one-render routing checks passed.
The dashboard reads operational logs and does not invoke financial processing.

Final acceptance requires exact raw historical GL hash agreement, combined GL
key conservation, row-count controls, normalization controls, actual posting and
report input manifests, reporting reconciliation/history/conservation checks,
source/storage/RSS evidence and report interpretation. A completed workflow marker
alone will not satisfy those requirements. No Q2 model evaluation or publication.

First cheap financial falsifier: fresh raw FY03 GL and retained original have the
same SHA-256 (`fy03_raw_gl_sha256.txt`). This establishes FY03 raw GL equivalence
only; combined normalization/posting and other years are still pending. Existing
canonical-evidence wrapper diagnostics include extra journal scans; they are
outside Scala engine timing and are not substitutes for exact Scala acceptance
controls or per-source normalization checks.

At 15:40 AZT: historical FY03 stage returned successfully, 13,508,560 merged
journals. Wholly-generated SY01 returned successfully with 342,242 journals.
Historical normalization is active; combined posting remains pending. Stage gate
records show native stage 22:26:53–22:38:46 UTC; native engine-step wall total is
342 seconds versus 713 seconds before the next stage. The difference includes
orchestration and canonical evidence work and must not be called engine compute.

At 16:30 AZT: FY03 normalization completed, preserving all 13,508,560 journal
rows. Source-group classifications: 702,224 observed rekeyed, 2,232,134 reassigned,
3,819,922 unresolved preserved (6,754,280 paired sources in total). No unparseable
date counter was emitted. These are source-group counts, not distinct actors.
The two external sorts each consumed/emitted 13,508,560 rows; 32-MiB buffer cap,
fan-in 16, 255/287 spills and 2/3 merge levels. Normalizer process wall 2,995.93 s,
maximum RSS 1,051,447,296 bytes. Compact controls, sort metrics and process log are
retained here. The driver reached actual combined SY01 posting; its completion
and GL conservation are still pending. Full corpus/reporting remains pending.
