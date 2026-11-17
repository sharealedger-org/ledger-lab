# Validated historical-plus-wholly-generated baseline

Accepted2026-11-17 after original session84820 exited0, successful workflow/check/
census markers, exact input receipt verification and completed-generation semantic
review. Actual run: session-run.tmp/phase2_fused_reporting_v2, consuming accepted
phase2_fused_baseline_v1 financial partitions. This supersedes pending language
in the stage receipts; it does not relabel earlier failed/stopped attempts.

14 financial years:261,529,946 preserved historical journal rows plus4,789,872
wholly-generated journal rows =266,319,818. The49,091 reused v6 copies are excluded
from additive inputs. Ledger input19,957,916 rows. Exact28 reporting input paths,
counts/bytes and SHA256 match accepted producers/sorter:286,277,734 financial
input rows,102,677,962,038 bytes. All consumed side-input hashes and frozen class
hashes verified; receipts/ contains the actual successful comparison.

Scala checks:19,943,155 report-key reconciliation rows PASS;3,163,680 synthetic
conservation checks PASS;20 reference cases PASS. Financial/coverage counts agree;
98,865 vendors,131,935,804 attributed input rows and154,341,930 unattributed.
Temporal summary984 rows; attribute summary5,180. Input ledger rows, report keys
and expanded view rows have different grains and are not interchangeable counts.

Census scans/hashes all seven named vendor-grain products:119,783,906 temporal
rows;51,229,802 master-attribute rows; remaining inventories in census_report_profile.
Selected zero/observed/reassigned/wholly identities and date modes passed semantic
review. Final coverage reproduces the earlier scoped observations. Identity and
lineage meanings/limits are explicit in semantic_review/README.md.

Actual OS timings: metadata81.27s; reporting sort4,603.96s; extraction17,416.02s;
reduction32.16s; checks112.13s; census414.21s. Scala extractor17,403.32s and reducer
20.81s exclude their wrapper/startup boundaries. Peak RSS: extractor885,792,768B,
reducer437,993,472B, checker208,236,544B, census213,708,800B. Separate earlier
financial producer metrics are retained; this is not one uninterrupted raw replay.

Owner designated this reporting attempt mid-run as an observational measured
baseline. Study protocol/conclusion retain missing-counter and causal limits.
No replicated paper speedup or hidden-training cause is established. Current
execution contains no allocation/new-basis loop, generalized03 walk-forward,
complete99 integration or Q2 model run. Improved reporting is a subsequent
owner-authorized workload; it must preserve these outputs and controls.

Source build d9dbd2d/financial engine a0dae7c; original driver/extract configurations
and class hashes are retained in the study snapshots. No upload/publication has
occurred. Upload-helper preparation remains a separate deliverable.
