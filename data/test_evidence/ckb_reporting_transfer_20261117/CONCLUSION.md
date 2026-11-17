# Completed observational baseline — CKB reporting transfer case

2026-11-17. Original workflow session84820 exited0. Its completion marker is
2026-11-17T15:17:18Z; final receipt and semantic review followed. The read-only
observer captured conclusion/ and its file hashes verified. This is the current
baseline conclusion, before any improved implementation or comparative rerun.

All14 financial years passed:266,319,818 journal rows plus19,957,916 ledger input
rows. The exact28 sorted reporting partitions,286,277,734 financial rows and
102,677,962,038 bytes match the accepted producers and actual extractor hashes.
All consumed side-input/class hashes pass. Exhaustive Scala checks passed
19,943,155 reconciliation keys,3,163,680 conservation checks and20 reference cases.
The census completed; four selected identity classes and date behaviors passed
completed-generation inspection. See ../phase2_derived_baseline_20261116/
full_reporting/ACCEPTANCE.md for the acceptance chain and explicit scope limits.

| Reporting stage | OS elapsed seconds |
|---|---:|
| Dimensions/reference preparation |81.27|
| Reporting-order sort/materialization |4603.96|
| Extraction/internal views |17416.02|
| Separate global reduction |32.16|
| Independent checks |112.13|
| Output census/hashes |414.21|

These stage timers sum to about6h17m40s. The observed monitor-to-workflow window
08:59:37–15:17:18UTC is6h17m41s; it is not the full original raw-replay/retry
history. Extraction's Scala timer is17403.32s, reduction's20.81s; timer boundaries
exclude wrapper/startup work. Extractor peak RSS885,792,768B. The configured
reporting JVM is1 active processor/2GiB heap on a2-physical/4-logical/16GiB host.
Host details were captured mid-run, not retrospectively measured at launch.

The synthetic accumulators wrote172,057,919,331 logical partial bytes, including
merge outputs; ordinary accumulators wrote5,205,090,962B. The separate reducer
started with345,985,746B/118 files and wrote812,741,417B intermediates in2levels.
These are different writer scopes, not device reads+writes. The1476 synthetic
and40 ordinary merge-level sums are across vendor folds, not corpus traversals.
The finished temporal view is119,783,906 rows/64,074,370,604B; master-attribute
view51,229,802 rows/7,738,311,183B. No per-view CPU attribution was measured.

Interpretation: this execution provides an internal architectural comparator
outside the separate Control repository. Available CKB descriptions and the
fused posting path were not fully transferred into reporting's dependent-view
and materialization design. Existing positive mechanisms remain: ordered merge,
source identity, streaming leaf records, retained vendor targets and in-memory
summary collapse. The failure is partial architecture transfer, not evidence
that all150million zero-vendor rows were retained or that no CKB behavior exists.

Owner reports complete builder access throughout; exact launch-tree architecture
material is retained in access_provenance/. Access restrictions alone cannot
explain this case. Hidden training, retrieval, reasoning and implementation-gate
causes remain unisolated. A source inspection and one measured run cannot prove
training causation or a speedup. The withdrawn byte-rate ETA and its correction
are recorded in the protocol; input percentage was not completed-work percentage.

Next comparison: preserve these inputs, output capabilities and controls; test
identified dependent views, current-full-key folds and reduced materialization.
Compare extract/reduce on the same presorted inputs separately from any changed
sort-preparation profile. Register changed resources/materializations, retain
failures and missing metrics, and qualify a single ordered pair for cache/order
confounding. Improved implementation and timing remain pending. No manuscript,
Q2 model, target-runtime maturity or publication is promoted by this conclusion.
