# Research Objectives — Master Experimental Map

**Prepared:** 2026-11-04  
**Status:** Authoritative. Supersedes `docs/next-session-plan.md` and
`org-members/research/PAPER_02_NOTES.md` as the source of truth for test IDs, objectives,
run order, and open decisions.  
**Audience:** Any new contributor, paper reviewer, or session starting cold.

---

## 1 — The Dual-Paper Research Programme

This project produces two papers from a single implementation of the Universal Ledger (CKB)
pipeline running on publicly available Virginia state procurement data (FY2003–FY2016, ~133M PO
lines). **Paper 1** establishes the minimum-cost curve: each successive capability layer (post
only → aggregation → revaluation → allocation) adds a precisely measured marginal cost, and if
the full pipeline lands at approximately the same cost per event as the baseline, that is the
empirical proof of single-pass universality. **Paper 2** uses the same pipeline's output to test
the AI-consequence claim of Theorem 1: the posting process is lossy compression, and the
pre-hierarchy commitment record carries signal that the posted record permanently discards. The
Virginia corpus is chosen because it is the only available dataset where both the pre-hierarchy
event (the PO line) and the posted expenditure record coexist and are joinable — making the
information loss directly measurable rather than merely inferred.

---

## 2 — Paper 1 Objectives: Minimum Cost Curve

### What CCP1–CCP4 measure

Each **Cost Curve Point** (CCP) is a verified, reproducible position on the minimum cost curve.
The prefix "CCP" signals immediately to any developer: this is a measured cost point, not a
hypothesis test or an AI evaluation. A CCP is not valid until three replicates on the same input
snapshot have been run and their metrics are stable.

Four capabilities are layered in sequence. Each new workload runs the full prior pipeline plus
one additional capability; the delta in records/cost/time is the finding. The cumulative picture
is the minimum cost curve.

### What "done" looks like for Paper 1

Paper 1 is done when CCP1–CCP4 each have three replicates with non-zero, stable values for
`records_read`, `bytes_written`, `wall_seconds`, and `peak_rss_bytes` in `process_metrics.csv`,
and the cost delta between CCP3 and CCP4 can be stated with confidence. If CCP4 ≈ CCP3, that
is the empirical proof of single-pass universality. If CCP4 > CCP3, the divergence and its
cause are the finding.

### ⚠ Open prerequisite blocking all CCP points

**`process_metrics.csv` emission is currently broken.** The `records_read`, `bytes_written`,
`wall_seconds`, and `peak_rss_bytes` fields are emitted as zero on every run. No CCP is valid
until this is repaired. Repairing the measurement substrate is the first implementation task of
the session after this taxonomy work completes. See `SESSION_HANDOFF.md` 2026-11-03 entry for
context.

---

## 3 — CCP1–CCP4 Table

| ID | Name | Capability added over previous | What the delta measures | Status |
|----|------|-------------------------------|------------------------|--------|
| **CCP1** | Post only | — (baseline) | Baseline: sort + CKB post + ledger. Records read/written, sort boundaries, elapsed time, peak RSS. The CKB pass at CCP1 also produces the joined commitment+expenditure stream consumed by AIM1–AIM4. | Blocked — `process_metrics.csv` broken |
| **CCP2** | Post + perspective aggregation | Step 5 aggregation output | Marginal cost of one reporting materialisation: storage, compute, reconciliation obligation added. | Blocked — depends on CCP1 |
| **CCP3** | Post + aggregation + revaluation | Booked FX revaluation engine | Marginal cost of one generated-SJE engine: additional SJEs, any new sort boundary, additional ledger rows. | Blocked — depends on CCP1 |
| **CCP4** | Full pipeline including allocation | Prior-period allocation engine | If CCP4 ≈ CCP3: empirical proof of single-pass universality. If CCP4 > CCP3: the divergence and its cause are the finding. | Blocked — depends on CCP1 |

*Three replicates of each workload on the same input snapshot are required before any cost
comparison is published. A single run is not a cost point.*

---

## 4 — Paper 2 Objectives: AI Consequence of Posting as Lossy Compression

### What AIM0–AIM5 measure

Each **AI Model evidence test** (AIM) trains or evaluates a model on pre-hierarchy vs. posted
data to measure whether the pre-hierarchy layer carries signal the posted record cannot provide.
The prefix "AIM" signals immediately: this is an AI model evidence test on the open Virginia
corpus (Paper 2, Track B), not a cost measurement.

AIM0 is a closed feasibility gate. AIM1 is a count (no ML required) and the most direct proof
of Theorem 1. AIM2–AIM5 each test the same architectural claim from a different angle: that
commitment-layer features carry signal invisible in the posted record.

### What "done" looks like for Paper 2

Paper 2 is done when: AIM1 has reported a non-injectivity distribution (any non-zero fraction
of posted keys with N≥2 distinct pre-hierarchy antecedents constitutes evidence); and AIM2–AIM5
have each produced a comparison of pre-hierarchy vs. posted-layer models whose delta meets the
accepted threshold (thresholds currently proposed — see Section 6). AIM5 must complete before
AIM2 (agency name disambiguation unblocks the agency-level commodity mix feature construction).

---

## 5 — AIM0–AIM5 Table

| ID | Question | Paper 2 claim tested | Status |
|----|----------|---------------------|--------|
| **AIM0** | Can PO orders be linked to payments? | Feasibility gate — determines which other tests are buildable. | **CLOSED 2026-11-01** — linkage via aggregate `(AGENCY, fiscal_year)` key is available; direct `PONUMBER` join is not. |
| **AIM1** | How many distinct PO patterns collapse to the same posted key? | Theorem 1, §2.1 — "The Posting Process as Lossy Compression." Direct empirical measurement; no ML required — a count. | Not yet run — depends on CCP1 (CKB pass produces the joined stream) |
| **AIM2** | Does knowing *what* an agency ordered improve forecasting of *what* it will spend next year? | §5.3 "Does adding pre-hierarchy event features improve on that ceiling?" | Not yet run — depends on AIM1; blocked until AIM5 produces clean agency identifiers |
| **AIM3** | Can we predict which vendors will stop doing business with the state, using only order history? | §5.3 "Does the event stream contain patterns that no balance-derived feature can surface?" | Not yet run — depends on AIM1 |
| **AIM4** | Does *when* an agency orders carry information that *when* it pays does not? | §1.1 and §5.1 Track B — ordering seasonality directly demonstrates the Layer 2/Layer 3 information gap. | Not yet run — depends on AIM1 |
| **AIM5** | Can an AI model tell the difference between "same agency, different spelling" and "actually a different entity"? | §3.3 "The Data-Type Navigation Problem" — preserved names carry information account codes erase. | Not yet run — depends on CCP1; must complete before AIM2 |

---

## 6 — Run Order

```
CCP1  ──►  AIM1  ──►  AIM3
      │          └──►  AIM4
      │
      └──►  AIM5  ──►  AIM2
```

| Constraint | Reason |
|-----------|--------|
| **CCP1 before AIM1–AIM4** | The CKB pass at CCP1 produces the joined commitment+expenditure stream. AIM1–AIM4 all consume that stream. Nothing downstream runs until CCP1 is valid. |
| **AIM1 before AIM2–AIM5** | AIM1 establishes the non-injectivity distribution and validates the aggregate join key. AIM2–AIM5 are built on the same joined stream and assume AIM1's join key has been confirmed. |
| **AIM5 before AIM2** | AIM2 (agency commodity mix forecasting) requires clean agency identifiers. AIM5 (agency name disambiguation) produces them. |
| CCP2, CCP3, CCP4 depend on CCP1 | Each layers capability onto the prior CKB pass output. |

*CCP2–CCP4 and AIM1–AIM5 have no ordering constraint relative to each other — they can proceed
in parallel once their respective CCP1 prerequisite is met.*

---

## 7 — Open Decisions (pending owner confirmation)

All items below are **proposed values only**. None are fixed. Implementation must not begin
until the owner has reviewed and confirmed or changed each item.

### Acceptance thresholds — proposed, pending owner decision

| Decision | AIM tests affected | Proposed value | Status |
|----------|--------------------|----------------|--------|
| AIM2 RMSE improvement threshold | AIM2 | ≥ 5% improvement over posted-layer baseline | **Proposed — pending owner decision** |
| AIM2 permutation p-value | AIM2 | < 0.05 | **Proposed — pending owner decision** |
| AIM3 C-statistic floor | AIM3 | > 0.65 | **Proposed — pending owner decision** |
| AIM3 permutation p-value | AIM3 | < 0.05 | **Proposed — pending owner decision** |
| AIM4 seasonality p-value and class count | AIM4 | p < 0.05, ≥ 3 of top 5 classes distinct | **Proposed — pending owner decision** |
| AIM5 precision floor | AIM5 | ≥ 0.99 | **Proposed — pending owner decision** |
| AIM5 ambiguity confidence threshold | AIM5 | < 0.70 | **Proposed — pending owner decision** |

*AIM1 has no pass/fail threshold — it is a measurement. Any non-zero fraction of posted keys
with N≥2 distinct pre-hierarchy antecedents is evidence of non-injectivity. A large fraction is
strong evidence.*

### Longitudinal evaluation strategy — pending owner decision

`PAPER_02_NOTES.md` (Test 4) proposes training on years 1–7 and predicting year 8, then
repeating for each of the 7 possible train/test year-windows across FY03–FY16. This produces
7 independent time-window replications, which is the stronger robustness result. The alternative
is a single held-out split. **Owner must decide before AIM2 and AIM3 Python scripts are written.**

| Option | Replication windows | Effort | Result type |
|--------|--------------------|---------|----|
| Single held-out split | 1 | Low | Point estimate |
| 7-window longitudinal | 7 | Medium | Robustness claim across independent windows |

### AIM1 revision-suffix inclusion — pending owner decision

Should the `PONUMBER` revision suffix (`-Vn`, e.g. `-V2`, `-V3`) be included as a separate
dimension in the non-injectivity count? Including it adds one field to the CKB view and makes
the count reflect the full modification sequence (a different revision of the same PO order is a
distinct commitment event). Excluding it treats all revisions to the same PO as a single event.
**This decision also affects the feature set available to AIM2.** Owner decision required before
the CKB view schema is finalised.

### Surrogate key confirmation — pending owner decision

`VENDORID` is assigned at eVA registration time and carries systematic ordering artifacts that
would leak chronological information into a random-looking train/test split. The proposed
surrogate is `vendorSurrogateKey = SHA256(VENDORID) mod 2^32` — stable across runs, semantically
neutral, and consistent with the pattern used by the insurance corpus (Paper 2 §5.2). **Owner
must confirm before the Scala CKB view emits the field**, because changing the surrogate key
formula invalidates all downstream split assignments.

---

## 8 — Related Documents

| Document | Location | What it contains |
|----------|----------|-----------------|
| `TEST_DESIGN_PAPER_1.md` | `ledger-lab/docs/` | Full Paper 1 spec: CCP1–CCP4 workload definitions, input snapshots, required metrics, acceptance evidence shape |
| `TEST_DESIGN_PAPER_2.md` | `ledger-lab/docs/` | Full Paper 2 spec: AIM0–AIM5 test specifications including AIM1 section, open-decisions table, author notes |
| `paper2-tests-plan.md` | `ledger-lab/docs/` | Implementation sub-tasks for Paper 2 pipeline: `transIPID` fix, CKB view emission, Python fitting scripts |
| `SESSION_HANDOFF.md` | `ledger-lab/` | 2026-11-03 entry: key decisions, rationale, context for each item above |
| `PAPER_02_NOTES.md` | `org-members/research/` | Original working notes; superseded by this document and `TEST_DESIGN_PAPER_2.md` |
| `next-session-plan.md` | `ledger-lab/docs/` | Planning document; superseded by this document |
