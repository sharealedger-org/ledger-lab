# Test Design — Paper 1: The Minimum Cost Curve

**Document status:** Active specification. Supersedes the C1–C4 prose in `CONTRIBUTOR_ONBOARDING.md` §"Seven Steps to a Verified Cost Point" and `PROJECT_BRIEF.md` §"The Four Configurations" as the authoritative source of record for Paper 1 workload definitions.  
**Prepared:** 2026-11-04  
**Companion documents:** `docs/TEST_DESIGN_PAPER_2.md` (Paper 2 AI model evidence tests), `docs/RESEARCH_OBJECTIVES.md` (master objectives and run-order map)

---

## 1. Preamble

### What Paper 1 measures

Paper 1 (§3.2) establishes a **minimum cost curve** for enterprise financial data pipelines. The thesis is that there is a mathematically defined interior minimum to the total cost of maintaining an enterprise financial dataset, and that virtually all real-world implementations have drifted above that minimum by over-materialising balance structures. The cost curve experiment is the empirical demonstration of that claim: it shows where the minimum lies, how each added process layer shifts the cost point, and whether the single-pass universality claim holds for the allocation case.

The curve is traced by running four progressively richer pipeline configurations — **CCP1 through CCP4** — on the same input snapshot and comparing measured cost dimensions across them.

### What "CCP" stands for

`CCP` = **C**ost **C**urve **P**oint. Each ID is a *verified, reproducible position* on the minimum cost curve. The prefix is chosen deliberately: it tells a new developer, a code reviewer, or a paper reader immediately that this is a *measured cost point*, not a test, not a hypothesis, and not a performance benchmark. The four points together constitute the cost curve; the deltas between consecutive points are the measurements the paper reports.

The old labels `C1`–`C4` are replaced by `CCP1`–`CCP4` throughout this repo. Anywhere you see `C1`, `C2`, `C3`, or `C4` in older documents, read it as `CCP1`, `CCP2`, `CCP3`, `CCP4` respectively.

### Why three replicates are required

A single run is not a cost point. Wall time and peak RSS vary run-to-run due to JVM warm-up, OS scheduling, and I/O buffering. **Three replicates of each workload on the same input snapshot are required before any cost comparison is published.** The published cost point is the median across three replicates; variance across replicates is also reported. If the three replicates do not agree within an acceptable variance band, the workload declaration or the measurement substrate has a defect — the cost point is not yet valid.

---

## 2. CCP1–CCP4 Summary Table

| ID | Name | Steps active | Capability added over previous | What the delta measures |
|----|------|-------------|-------------------------------|------------------------|
| **CCP1** | Post only | 2, 3 | — (baseline) | Baseline: sort + CKB post + ledger write. Records read/written, sort boundaries, elapsed time, peak RSS. The leftmost, cheapest position on the curve. |
| **CCP2** | Post + perspective aggregation | 2, 3, 5 | Step 5 analytical pivot output | Marginal cost of one reporting materialisation: additional storage, additional compute, reconciliation obligation added by the ViewSpec views. |
| **CCP3** | Post + aggregation + revaluation | 2, 3, 5, 8, 9 | Booked FX revaluation engine (Steps 8, 9) | Marginal cost of one generated-SJE engine: additional SJEs generated, any new sort boundary, additional ledger rows. Hypothesis: flat vs. CCP2 — no new master files required. |
| **CCP4** | Full pipeline including allocation | 2, 3, 5, 6, 7, 8, 9 | Prior-period allocation engine (Steps 6, 7) | If CCP4 ≈ CCP3, that equality is the empirical proof of single-pass universality. If CCP4 > CCP3, the divergence and its identified cause are the finding. |

Step numbers correspond to the VA pipeline step numbering used in `PROJECT_BRIEF.md` and the pipeline orchestrator (`run_pipeline_orchestrator.sh`): Step 2 = standardise+sort, Step 3 = CKB post, Step 5 = aggregation/perspectives, Step 6 = allocation, Step 7 = consolidation, Step 8 = forecasting, Step 9 = reclass.

---

## 3. Per-Workload Specifications

---

### CCP1 — Post Only

**Steps selected:** 2 (standardise + sort), 3 (CKB post + ledger write)

**Input snapshot required:**  
The standard fixture snapshot: `SortedJE{year}.csv` (transaction history, Partition A), `LDGR{year}.csv` (prior balance, Partition B), `VendorMaster.csv` (instrument attributes, Partition C). The exact snapshot must be declared in the workload manifest (input file hashes committed alongside the run evidence) so that CCP2, CCP3, and CCP4 can be run against exactly the same input. Any change to the input snapshot invalidates the cost comparison.

**Metrics that must be emitted (`process_metrics.csv`):**

| Metric | Column name | Notes |
|--------|-------------|-------|
| Records read | `records_read` | Must be non-zero; equals the inbound transaction count for this year-slice |
| Bytes written | `bytes_written` | Ledger output size in bytes |
| Elapsed wall time | `wall_seconds` | Step-level, not total pipeline |
| Peak resident set size | `peak_rss_bytes` | JVM heap + off-heap at peak |

All four fields must be non-null and non-zero for the cost point to be valid. A row with zeros or nulls indicates a measurement substrate defect, not a successful run.

**Acceptance evidence shape:**  
A valid CCP1 cost point consists of:
- Three `process_metrics.csv` rows for steps 2 and 3, all with non-zero values, from three separate runs against the same input snapshot manifest
- Median values computed across the three replicates
- Replicate variance within an acceptable band (no run more than 15% above/below the median — proposed; pending owner confirmation)
- `pipeline_log.csv` entries for all three runs showing clean exit codes
- No assertion failures in per-group balancing or sort contract checks

The cost point is reported as: `(records_read_median, bytes_written_median, wall_seconds_median, peak_rss_bytes_median)` ± variance.

**Current status:** ⚠ **Blocked** — `process_metrics.csv` currently emits zeros on macOS (see §4 Open Prerequisites). No valid CCP1 cost point exists yet. The workload manifest has not been formally declared. See `CONTRIBUTOR_ONBOARDING.md` §"The most urgent open item: repair the measurement substrate" and `SESSION_HANDOFF.md` → Next Work item 2.

---

### CCP2 — Post + Perspective Aggregation

**Steps selected:** 2, 3, 5 (aggregation/perspectives)

**Input snapshot required:**  
Identical to CCP1 — same input files, same committed hashes. Step 5 reads the ledger output produced by Step 3 in the same run; no additional external input is required. Note: `run_pipeline_orchestrator.sh` currently calls `scripts/data_aggregation.py` (Python) for Step 5 in the CCP2/CCP3/CCP4 paths. Per `PROJECT_BRIEF.md` §"Technology Invariant", mainline cost-curve steps must run in Scala for the curves to be comparable. If Step 5 runs via Python for the CCP2 measurement, that must be flagged in the evidence metadata — the delta from CCP1 would include Python interpreter overhead and is not a clean Scala-vs-Scala comparison. Resolution requires owner decision before CCP2 is run.

**Metrics that must be emitted (`process_metrics.csv`):**  
Same four metrics as CCP1, with step-level rows for steps 2, 3, and 5 separately. The delta of interest is Step 5 alone: `(Δrecords_read, Δbytes_written, Δwall_seconds, Δpeak_rss_bytes)` = (CCP2 total − CCP1 total). Step 5 rows must be emitted at the same granularity as steps 2 and 3.

**Acceptance evidence shape:**  
Three replicates with non-zero step-5 metrics; median and variance reported per step; delta from CCP1 computed; delta must be positive (Step 5 adds cost, not removes it); ViewSpec output files present in expected output directory.

**Current status:** ⚠ **Blocked** — depends on CCP1 being valid first, and on the measurement substrate repair described in §4. Additionally, the Step 5 substrate (Python vs. Scala) requires clarification before CCP2 is a clean measurement point.

---

### CCP3 — Post + Aggregation + Revaluation

**Steps selected:** 2, 3, 5, 8 (forecasting / revaluation-class), 9 (reclass)

**Input snapshot required:**  
Identical to CCP1 and CCP2. Steps 8 and 9 are revaluation-class processes: they read only Partition B (balance history) and Partition C (attribute history) and *generate* new SJEs — no additional inbound feed required. The generated SJEs from steps 8/9 are written back into the CKB pass as additional transaction volume, which is why CCP3 may produce more ledger rows than CCP2 even on the same inbound input.

**Metrics that must be emitted (`process_metrics.csv`):**  
Step-level rows for steps 2, 3, 5, 8, and 9 separately. The delta of interest is steps 8+9: `(Δrecords_generated, Δbytes_written, Δwall_seconds, Δpeak_rss_bytes)` = (CCP3 total − CCP2 total). `records_read` for steps 8/9 measures Partition B + C input; `records_written` measures the volume of generated SJEs added to the stream.

**Acceptance evidence shape:**  
Three replicates with non-zero step-8 and step-9 metrics; generated SJE count > 0 (if zero, the revaluation engine did not fire); delta from CCP2 computed and reported; delta sign and magnitude reported without a pass/fail threshold (the curve shape, not a threshold, is the measurement). Ledger output row count must be ≥ CCP2 output row count (generated SJEs add rows, they do not remove them).

**Current status:** ⚠ **Blocked** — depends on CCP2; additionally, `CONTRIBUTOR_ONBOARDING.md` notes that Steps 8 and 9 (forecasting, reclass) require an engine contract audit before they can contribute to a cost curve (`docs/VERIFICATION_AUDIT_REGISTER.md` should reflect audit status). No workload manifest declared.

---

### CCP4 — Full Pipeline Including Allocation

**Steps selected:** 2, 3, 5, 6 (allocation), 7 (consolidation), 8, 9

**Input snapshot required:**  
Identical to CCP1–CCP3. Steps 6 and 7 use the prior-period divisor design: the allocation divisor (e.g. total headcount) is a balance already in Partition B from the previous run, so no pre-scan pass is required. The single-pass universality claim is that this design collapses allocation back to one pass — the same CKB architecture as all other FSPs.

**Metrics that must be emitted (`process_metrics.csv`):**  
Step-level rows for all active steps: 2, 3, 5, 6, 7, 8, 9. The delta of interest is steps 6+7: `(Δrecords_read, Δbytes_written, Δwall_seconds, Δpeak_rss_bytes)` = (CCP4 total − CCP3 total).

**Acceptance evidence shape:**  
Three replicates with non-zero step-6 and step-7 metrics; delta from CCP3 computed. The key measurement is the sign and magnitude of that delta:
- **If CCP4 ≈ CCP3** (delta is within the variance band of the replicates): single-pass universality is supported for this workload and profile. That equality is the finding the paper reports.
- **If CCP4 > CCP3** (delta is positive and outside the variance band): the divergence is a finding. The evidence must identify the cause — a required sort, materialised side input, generated SJE volume, or reconciliation obligation — and that cause and its magnitude become the reported result.

Neither outcome is a failure. Both are publishable findings; they answer different questions.

**Current status:** ⚠ **Blocked** — depends on CCP3; Steps 6 and 7 are in the engine audit backlog. No workload manifest declared.

---

## 4. Open Prerequisites — Blocks All Paper 1 Work

### ⚠ P1 — `process_metrics.csv` emission must be repaired before any CCP point is valid

**Status: BLOCKING**

The Scala pipeline currently emits zeros for `records_read`, `bytes_written`, `wall_seconds`, and `peak_rss_bytes` on macOS. This has been confirmed and recorded in `SESSION_HANDOFF.md`. A run that produces `records_read=0` is not a failed run — it exits clean — but its cost evidence is fabricated. An agent or developer reading those zeros will mis-state the cost of every step measured.

**No CCP cost point is valid until this is repaired.** Repro steps, suspected location in the Scala emission code, and the required fix are tracked in `SESSION_HANDOFF.md` → Next Work item 2 and `CONTRIBUTOR_ONBOARDING.md` §"The most urgent open item".

This repair is the first item on the next implementation session's agenda. Until it is done, CCP1–CCP4 workload manifests may be declared and documented, but no run evidence may be filed as a valid cost point.

### P2 — Step 5 substrate decision required before CCP2

**Status: Owner decision required**

`run_pipeline_orchestrator.sh` currently dispatches Step 5 to `scripts/data_aggregation.py` (Python) for the CCP2/CCP3/CCP4 paths. The technology invariant (`PROJECT_BRIEF.md` §"Technology Invariant") requires all mainline cost-curve steps to run in Scala. A CCP2 measured with Python Step 5 includes Python interpreter overhead in the delta, making it non-comparable to the Scala steps. Options: (a) accept the current Python Step 5 with a caveat flagged in evidence metadata, or (b) block CCP2 until `05-perspectives` is implemented in Scala. Owner decision required; record in `SESSION_HANDOFF.md` before CCP2 runs.

### P3 — Steps 8, 9, 6, 7 require engine contract audit before contributing to CCP3/CCP4

**Status: Pending** (non-blocking for CCP1 and CCP2)

The audit register (`docs/VERIFICATION_AUDIT_REGISTER.md`) must show a passing engine contract entry for each step before that step contributes to a published cost point.

---

## 5. CCP1 as AIM Prerequisite

**CCP1 must complete before AIM1–AIM4 can run.**

The CKB post pass (Steps 2 and 3, which is CCP1's pipeline) produces the joined stream that all downstream Paper 2 evidence tests consume. Specifically:
- The merged transaction + attribute stream emitted by Step 3 is the source data for the AIM1 non-injectivity count (which is computed as a CKB view alongside the ledger pass).
- AIM2, AIM3, and AIM4 all train on features derived from the same joined stream.

Running AIM1–AIM4 before CCP1 is valid would require re-running the CKB pass anyway. The correct run order is: CCP1 (with repaired metrics) → AIM1 (free, as a view on the CCP1 run) → AIM2–AIM4 (use the joined stream CCP1 produced).

For the full dependency map across all six AIM tests and CCP1–CCP4, see [`docs/RESEARCH_OBJECTIVES.md`](RESEARCH_OBJECTIVES.md).

---

## 6. Workload Manifest Declaration (Template)

Before any CCP run is filed as evidence, a workload manifest must be committed. Minimum fields:

```
workload_id:        CCP1-YYYY-MM-DD-001
pipeline_revision:  <git SHA of ledger-lab at time of run>
input_snapshot:
  SortedJE_hash:    <SHA-256 of input SortedJE{year}.csv>
  LDGR_hash:        <SHA-256 of LDGR{year}.csv>
  VendorMaster_hash: <SHA-256 of VendorMaster.csv>
steps_active:       [2, 3]
materialization_profile: <name or description>
run_date:           YYYY-MM-DD
replicate_count:    3
```

Two runs are only comparable when their manifests match on `pipeline_revision`, all three `input_snapshot` hashes, `steps_active`, and `materialization_profile`. Any mismatch means the delta is not a clean measurement of the added capability.

---

*See also: [`docs/RESEARCH_OBJECTIVES.md`](RESEARCH_OBJECTIVES.md) — master objectives and run-order map for the full experimental programme.*
