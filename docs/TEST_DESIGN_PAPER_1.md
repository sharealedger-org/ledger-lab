# Test Design — Paper 1: The Minimum Cost Curve

**Document status:** Active specification. Supersedes the C1–C4 prose in `CONTRIBUTOR_ONBOARDING.md` §"Seven Steps to a Verified Cost Point" and `PROJECT_BRIEF.md` §"The Four Configurations" as the authoritative source of record for Paper 1 workload definitions.  
**Prepared:** 2026-11-04  
**Companion documents:** `docs/TEST_DESIGN_PAPER_2.md` (Paper 2 AI model evidence tests), `docs/RESEARCH_OBJECTIVES.md` (master objectives and run-order map)

---

## 1. Preamble

### What Paper 1 measures

Paper 1 (§3.2) establishes a **minimum cost curve** for enterprise financial data pipelines.
The thesis is that there is a mathematically defined interior minimum to the total cost of
maintaining an enterprise financial dataset, and that virtually all real-world implementations
have drifted above that minimum by over-materialising balance structures.

The experiment traces that curve in two parts:

1. **CCP1 — the status-quo baseline.** Every institution already runs something like CCP1: sort
   events, post to a General Ledger keyed on nominal account codes, write GL balances. The
   instrument identity (vendor, counterparty) is discarded at posting time. CCP1 measures the
   cost of this exactly. It is not a point on the minimum cost curve — it is the point that
   represents *where everyone already is*. A second follow-on step (the permutation generator)
   reads the original transactions and computes the full set of key-embedded balance permutations
   a GL-based architecture would need to materialise to answer all the questions the instrument
   ledger answers for free. The permutation table size is the Paper 1 cost-explosion finding.

2. **CCP2–CCP5 — the minimum cost curve.** Starting from CCP2 (the instrument ledger, the
   irreducible minimum of the CKB architecture), each successive CCP adds one capability layer.
   The deltas between consecutive points constitute the minimum cost curve. CCP5 ≈ CCP4 is
   the empirical proof of single-pass universality.

### What "CCP" stands for

`CCP` = **C**ost **C**urve **P**oint. Each ID is a *verified, reproducible position* on the
minimum cost curve (CCP2–CCP5) or the status-quo baseline (CCP1). The prefix tells a new
developer, reviewer, or paper reader immediately: this is a *measured cost point*, not a test,
not a hypothesis, and not a performance benchmark.

The old labels `C1`–`C4` are replaced by `CCP2`–`CCP5` throughout this repo. CCP1 is new —
it did not exist under the old labelling.

### Why three replicates are required

A single run is not a cost point. Wall time and peak RSS vary run-to-run due to JVM warm-up,
OS scheduling, and I/O buffering. **Three replicates of each workload on the same input snapshot
are required before any cost comparison is published.** The published cost point is the median
across three replicates; variance across replicates is also reported. If the three replicates do
not agree within an acceptable variance band, the workload declaration or the measurement
substrate has a defect — the cost point is not yet valid.

---

## 2. CCP1 and CCP2–CCP5 Summary Table

| ID | Name | Architecture | Steps active | Capability | What it measures | AIM pairing |
|----|------|-------------|-------------|-----------|-----------------|-------------|
| **CCP1** | General Ledger (status quo) | GL, account-keyed | 2, GL-post | Sort → GL post → GL balances. Instrument identity discarded at posting. Plus permutation generator (separate step, reads original transactions). | Cost of what every institution already runs. Permutation table size = cost of answering all IL questions from a GL architecture. | AIM1 family run as post-hoc steps against GL output and permutation table. All miss. |
| **CCP2** | Instrument Ledger | CKB, instrument-keyed | 2, 3 | Sort → CKB post → Instrument Ledger (vendor-keyed balances). | Left anchor of the minimum cost curve. Delta CCP1→CCP2 = cost of the architectural change. | AIM1 run as post-hoc step against IL output. Succeeds. |
| **CCP3** | IL + perspective aggregation | CKB + Step 5 | 2, 3, 5 | Step 5 aggregation views alongside the ledger pass. The Step 5 views ARE the AIM2–AIM4 feature tables. | Marginal cost of reporting materialisation. Delta CCP2→CCP3 = cost of producing all AI evidence substrate. | AIM2, AIM3, AIM4 consume CCP3 view outputs. |
| **CCP4** | CCP3 + revaluation engines | CKB + Steps 8, 9 | 2, 3, 5, 8, 9 | Booked FX revaluation and reclass engines (Steps 8, 9). | Marginal cost of generated-SJE engines. | — |
| **CCP5** | Full pipeline | CKB + Steps 6, 7 | 2, 3, 5, 6, 7, 8, 9 | Prior-period allocation and consolidation (Steps 6, 7). | CCP5 ≈ CCP4 = proof of single-pass universality. | AIM5 (agency disambiguation) must complete first — clean agency keys prerequisite for allocation. |

Step numbers: Step 2 = standardise+sort, Step 3 = CKB post, Step 5 = aggregation/perspectives,
Step 6 = allocation, Step 7 = consolidation, Step 8 = forecasting, Step 9 = reclass.

---

## 3. Per-Workload Specifications

---

### CCP1 — General Ledger (Status Quo)

**Architecture:** GL post, account-keyed. This is how financial systems work today. The
instrument identity (VENDORID) is not preserved in the output.

**Steps active:** Step 2 (standardise + sort by account key), GL-post (write account-keyed GL
balances). This is a *different sort key* from CCP2: account code, not instrument ID.

**Permutation generator (separate follow-on step):**  
Reads the original transactions (same input snapshot as all CCPs). For each distinct
`(account_code)` GL balance, enumerates every combination of instrument attributes
(VENDORID × NIGP_class × geo_region × …) that collapsed into it. Produces a permutation
table: `(account_code, permutation_key, permutation_count)`. This table's row count is the
empirical size of the key-embedded architecture that would be required to answer all the same
questions the instrument ledger answers for free.

**Metrics that must be emitted (`process_metrics.csv`):**

| Metric | Column name | Notes |
|--------|-------------|-------|
| Records read | `records_read` | Inbound transaction count |
| GL balance rows written | `bytes_written` | GL output size in bytes |
| Permutation table rows | `permutation_rows` | Size of the key-embedded equivalent |
| Elapsed wall time | `wall_seconds` | Step-level |
| Peak resident set size | `peak_rss_bytes` | JVM heap + off-heap at peak |

**AIM pairing:**  
After CCP1 completes, the AIM1 family of tests runs as post-hoc steps reading the GL output
and the permutation table. The same AIM1–AIM5 tests are run against these CCP1 substrates
as are run against the CCP2/CCP3 substrates. All CCP1-substrate runs are expected to miss —
for different instructive reasons documented per test in `TEST_DESIGN_PAPER_2.md`.

**Current status:** ⚠ Not yet implemented. GL-post step and permutation generator require
design and build. `process_metrics.csv` substrate repair is a prerequisite.

---

### CCP2 — Instrument Ledger

**Architecture:** CKB post, instrument-keyed. The instrument identity (VENDORID) is preserved
as the primary sort and ledger key. This is the left anchor of the minimum cost curve.

**Steps active:** 2 (standardise + sort by instrument key), 3 (CKB post + Instrument Ledger write)

**Input snapshot required:**  
`SortedJE{year}.csv` (transaction history, Partition A), `LDGR{year}.csv` (prior balance,
Partition B), `VendorMaster.csv` (instrument attributes, Partition C). The exact snapshot must
be declared in the workload manifest (input file hashes committed alongside the run evidence)
so that CCP3, CCP4, and CCP5 can be run against exactly the same input.

**Metrics that must be emitted (`process_metrics.csv`):**

| Metric | Column name | Notes |
|--------|-------------|-------|
| Records read | `records_read` | Must be non-zero |
| Bytes written | `bytes_written` | Instrument Ledger output size in bytes |
| Elapsed wall time | `wall_seconds` | Step-level |
| Peak resident set size | `peak_rss_bytes` | JVM heap + off-heap at peak |

**AIM pairing:**  
After CCP2 completes, AIM1 runs as a post-hoc step reading the Instrument Ledger output
(not the live event stream). AIM1 counts how many distinct pre-hierarchy patterns
(VENDORID, NIGP_class_3digit, revision_suffix) collapsed to each posted key in the ledger.
This is the same question asked of the CCP1 substrates, now answered against the instrument
ledger where the information is preserved.

**Acceptance evidence shape:**  
Three `process_metrics.csv` rows for steps 2 and 3, all with non-zero values, from three
separate runs against the same input snapshot manifest. Median and variance reported. No run
more than 15% above/below the median.

**Current status:** ⚠ **Not yet run** — `process_metrics.csv` repair is complete (2026-11-04);
the substrate is now measurable. First valid CCP2 run can proceed.

---

### CCP3 — Instrument Ledger + Perspective Aggregation

**Steps active:** 2, 3, 5 (aggregation/perspectives)

**Key design point:** The Step 5 aggregation views produced at CCP3 are the same views that
serve as AIM2–AIM4 feature tables. They are not a separate AI pipeline — they are the natural
output of Step 5 run alongside the ledger pass. The delta CCP2→CCP3 is the cost of producing
all Paper 2 AI evidence substrate. That cost is expected to be small relative to the sort and
post steps.

**Input snapshot required:** Identical to CCP2.

**Note on Step 5 substrate:** `run_pipeline_orchestrator.sh` currently dispatches Step 5 to
`scripts/data_aggregation.py` (Python). Per the Technology Invariant, mainline cost-curve
steps must run in Scala. A CCP3 measured with Python Step 5 includes Python interpreter
overhead in the delta. This must be flagged in evidence metadata until resolved.

**AIM pairing:** AIM2, AIM3, and AIM4 consume the Step 5 view outputs from this run.

**Current status:** ⚠ **Blocked** — depends on CCP2.

---

### CCP4 — CCP3 + Revaluation-Class Engines

**Steps active:** 2, 3, 5, 8 (forecasting/revaluation-class), 9 (reclass)

Steps 8 and 9 read only Partition B (balance history) and Partition C (attribute history) and
*generate* new SJEs — no additional inbound feed required. Generated SJEs are written back
into the CKB pass as additional transaction volume.

**AIM pairing:** None — this step adds generated SJEs, not new event-layer data. CCP3 view
outputs remain the AIM2–AIM4 input.

**Current status:** ⚠ **Blocked** — depends on CCP3. Steps 8 and 9 require engine contract
audit (`docs/VERIFICATION_AUDIT_REGISTER.md`) before contributing to a cost point.

---

### CCP5 — Full Pipeline Including Allocation

**Steps active:** 2, 3, 5, 6 (allocation), 7 (consolidation), 8, 9

Steps 6 and 7 use the prior-period divisor design: the allocation divisor is a balance already
in Partition B from the previous run. No pre-scan pass required. If CCP5 ≈ CCP4, that equality
is the empirical proof of single-pass universality.

**AIM pairing:** AIM5 (agency name disambiguation) must complete before CCP5 runs. Clean
agency identifiers are a prerequisite for the agency-keyed allocation in Steps 6 and 7 to be
meaningful.

**Current status:** ⚠ **Blocked** — depends on CCP4 and AIM5.

---

## 4. Open Prerequisites

### ✅ P1 — `process_metrics.csv` emission repaired (2026-11-04)

Steps 2 and 3 now emit `RESULT` and `RESOURCE` structured log lines. All required fields
(`records_read`, `records_written`, `elapsed_s`, `input_bytes`, `output_bytes`, `peak_rss_bytes`)
are non-zero. See `SESSION_HANDOFF.md` session log for validation output. Unblocked.

### P2 — CCP1 GL-post step and permutation generator not yet built

CCP1 requires a GL-post step (sort by account key, write account-keyed balances) and a
permutation generator (separate step reading original transactions). Neither exists yet.
CCP1 can be designed and built in parallel with the `process_metrics.csv` repair.

### P3 — Step 5 substrate decision required before CCP3

Python vs. Scala for Step 5. See §3 CCP3 note. Owner decision required before CCP3 is run.

### P4 — Steps 8, 9, 6, 7 require engine contract audit before contributing to CCP4/CCP5

The audit register (`docs/VERIFICATION_AUDIT_REGISTER.md`) must show a passing engine
contract entry for each step before that step contributes to a published cost point.

---

## 5. Workload Manifest Declaration (Template)

Before any CCP run is filed as evidence, a workload manifest must be committed:

```
workload_id:        CCP2-YYYY-MM-DD-001
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

Two runs are only comparable when their manifests match on `pipeline_revision`, all three
`input_snapshot` hashes, `steps_active`, and `materialization_profile`.

---

*See also: [`docs/RESEARCH_OBJECTIVES.md`](RESEARCH_OBJECTIVES.md) — master objectives and run-order map for the full experimental programme.*
