# CKB architecture transfer: reporting baseline and improved rerun

2026-11-17 06:32:06 AZT (UTC−07:00) — Study designation during execution.
Author: Codex; owner-directed evidence capture. This is a study protocol and
case record, not a manuscript edit or an assertion about hidden model training.

## Research question and designation

Can an AI-assisted implementation transfer the available CKB architecture from
the financial post flow into dependent reporting views, and what resource cost
results when the reporting implementation instead materializes intermediate
summaries? The owner identifies this as bridge evidence between papers2 and3,
associated with the transition into Phase2. Preserve that research placement
without changing the historical phase labels of the current synthetic execution.

The owner designates the ongoing attempt as a formally measured observational
baseline now. It was not prospectively started as this experiment. Preserve its
actual start, earlier diagnostic purpose, configuration and incomplete metrics;
do not backdate registration or invent missing measurements. This designation
supersedes earlier planning statements that no formal comparison was intended.
It does not retrospectively establish a replicated paper cost-curve point.

Study ID: `CKB-REPORT-TRANSFER-20261117` (case-study ID, not an existing AIM/CCP
point). Baseline attempt: `session-run.tmp/phase2_fused_reporting_v2`.
Current input population:28 ordered journal/ledger partitions,286,277,734 rows,
102,677,962,038 bytes, subject to final manifest validation. Retain the original
run/partition IDs and add the study link; never relabel an attempt as a new run.

## What this case currently establishes

The owner described identified input records, dependent views emitting identified
records into the common buffer, cross-file summarization, streaming leaf SJEs,
retained effective-dated join targets and per-view cross-vendor outputs. These
requirements are recorded in the conversation and in section9 of the
[layer plan](STREAM_LAYER_EVOLUTION_AND_ALLOCATION_FEEDBACK_PLAN.md).

Direct source observations: ReportingSums flushes all accumulated keys and later
merges partials; SyntheticReporting reduces fine-grained facts before expanding
three date modes/thirteen attribute views and reducing again; ReportingSort
materializes its last merge; ReportingReduce writes a merged file and rereads it
to reduce. The actual Financial.source/Input retains immediate source identity,
and historical/wholly inputs already share accumulators. Those partial features
do not implement generated-record dependent-view routing. The financial posting
path supplies a concrete local contrast: GL/permutation observers consume the
actual posting journal read, rather than independent journal rereads.

Interpretation: this is an observed architecture-transfer/implementation failure
case and an internal architectural control independent of the separate Control
repository. The owner's hypothesis is inadequate AI training on CKB. Hidden
training coverage is unknown; this observation cannot distinguish insufficient
training from context retrieval, reasoning, specification or implementation-gate
failure. No general model-capability score or causal training claim is justified.
Source inspection identifies optimization candidates, not their measured benefit.

Additional candidate from the actual contract: the presorted merge key equals
vendor plus the ordinary bucket's fourteen dimensions, but bucket finalization
still waits for a vendor break and uses ReportingSums. A direct current-full-key
fold may remove those partials. This is a proposed change requiring equivalence,
consumer-order and generated-record-readiness checks, not an implemented speedup.

### Access restriction is not a sufficient explanation

Owner clarification,2026-11-17: building agents had complete access to the CKB
method throughout this process. Record this as an owner-reported exposure
condition, distinct from a reconstructed per-turn retrieval trace. This case
therefore does not have the restricted-access condition that was a concern for
the separate Control work. Restricted access alone cannot explain the observed
reporting design failure: the failure occurred with access available.

Availability did not ensure retrieval, understanding or application. The remaining
questions concern effective use of available material, architectural reasoning,
implementation decisions and enforcement of the design gates. This does not
establish which mechanism failed or that access restrictions have no effect in
other experiments. For the bridge evidence, distinguish access permitted,
material actually consulted, design proposed and architecture implemented. Trace
those separately where records exist; leave missing exposure/retrieval history
unknown rather than treating permission as proof of comprehension.

## Baseline evidence and measurement coverage

Capture root run configuration, executed driver, class hashes, source hashes and
diff, input/sort manifests, stage logs and existing time/resource/storage logs.
Freeze the actual executed engine identity separately from later documentation
commits. Preserve original logs and snapshot hashes. The observer never changes
the running classes, input files or engine settings. Its additional file metadata
sampling every60 seconds is observation overhead and is recorded as such.

The existing `/usr/bin/time -l` wrappers will supply complete stage elapsed, CPU,
RSS and other OS counters at stage exit if the wrapper completes. Existing
storage sampling began before this designation. Current file inventories show
live/retained bytes, not cumulative spill traffic or disk-write totals. Deleted
partials cannot be reconstructed from snapshots. Use emitted Scala metrics where
available; missing per-view collapse/CPU/I/O, historical target/leaf memory peaks,
cache state, thermal state and competing workload measurements remain missing.
OS RSS is not a measurement of individual join-target or summary-map state.

Interpret the emitted metrics using their actual producer definitions:

| Producer field | Meaning and exclusion |
|---|---|
| extract `reporting_input_passes` | Traversals of its selected ordered financial inputs:1 for this presorted profile. Excludes reporting-order preparation, analytical partials and downstream scans. |
| extract `aggregate_spill_bytes` | Writes of bucket, relationship and vendor-cross partials, including their intermediate merge outputs. Excludes the synthetic accumulators and terminal reducer. |
| extract `spill_bytes` | Initial cross-vendor spill writes for the separate reducer. Not the size of all report products. |
| extract `aggregate_merge_passes` | Sum of accumulator merge levels across vendor folds; not a count of complete corpus traversals. |
| synthetic `partial_spill_bytes` | Writes summed across raw, temporal, master and their two summary accumulators, including intermediate merge outputs. Excludes final report CSV writes and reads. |
| synthetic `partial_merge_passes` | Sum of merge levels across those accumulators and vendor folds. It cannot be multiplied by full corpus size to estimate I/O. |
| reduce `merge_passes` | Levels of its global cross-vendor merge tree. Each level includes a materialized merge and a separate reduction read/write in this implementation. |
| reduce `intermediate_bytes` | Sum of merged and reduced intermediate files written. Excludes final CSV output, initial spills and read bytes. |

These writer counters are cumulative logical file bytes, not device traffic,
retained footprint or independently measured reads+writes. Keep overlapping
scope and final-output exclusions explicit when constructing a totals table.
Scala elapsed fields exclude JVM/SBT startup; OS stage timers include it. Do not
sum nested timers or per-vendor levels as if they were independent full runs.

Evidence root:
`data/test_evidence/ckb_reporting_transfer_20261117/`.
Observer: `scripts/capture_reporting_ckb_study.sh`; initial and conclusion snapshots
have SHA256SUMS. Capture completion/failure/unknown status, not just success.
Completion markers mean workflow completion with result validation pending.

## Matched comparison registered before improved execution

Primary outcomes: reporting elapsed/CPU, peak RSS, retained/intermediate bytes,
physical materialization boundaries and correctness/control equivalence. Secondary
outcomes, where both profiles measure them: cumulative spill bytes, merge work,
view collapse ratios, time to corpus EOF and final acceptance latency. Report
baseline coverage gaps rather than substituting improved-only counters as a
matched baseline measurement. Financial preparation is a separately recorded,
reusable prerequisite, not included in a reporting-only speed ratio.

Use identical frozen financial inputs, effective-dated masters, assignments,
view definitions, output schemas, decimal/date semantics and controls. Pin JDK,
Scala/build/classes, heap2GiB, ActiveProcessorCount1, fan-in16, host and resource
settings for the architecture comparison. Document observer and other processes,
available disk, cache/warmup conditions and run order. A larger memory budget or
parallelism is a separate factor, not an unexplained architecture improvement.

Compare two scopes separately: (1) extract/reduce over identical ordered inputs;
(2) reporting-order preparation through accepted reporting outputs, including
the materialization removed by a final-merge exit. Show checks/census time
separately. Preserve all existing view capabilities; no performance credit for
dropping date modes, attributes, provenance or validation. Test baseline fixtures
and `reporting_ckb_dependency_equivalence` before subset/full reruns as section9
requires. New allocation/basis functionality is a later workload extension and
must not be mixed into an equal-capability reporting timing ratio.

The current baseline plus one improved run is a paired case observation, with
cache/order/background confounding. Before a paper-level speedup claim, schedule
matched baseline/improved replicates with predeclared run order and resource
conditions. Report failures/retry cost and dispersion; never discard a slow or
failed attempt because it contradicts the hypothesis. Distinguish architecture
changes from the extra owner guidance supplied to the implementing agent.

## Required conclusion capture

At current-attempt termination, the read-only observer captures terminal logs,
metrics, manifests, output file sizes and status. A human/agent review must then
append: actual process boundaries and phase timings; final controls and semantic
acceptance; output/spill sizes with coverage limits; failures/retry history;
what the case supports about architecture transfer; alternative explanations;
and the matched rerun's frozen input/configuration references. Do not fill these
conclusions before results exist. Capture the same evidence for the improved
attempt, then compare outputs and measurements before interpreting performance.

No manuscript is changed, no new model is trained and no publication/upload is
authorized here. Layer02/05 redesign and later03/04/99 integration remain subject
to architecture gates and target-runtime capability qualification.
