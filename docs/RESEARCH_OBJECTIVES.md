# Research Objectives — Master Experimental Map

**Prepared:** 2026-11-04  
**Status:** Authoritative. Supersedes `docs/next-session-plan.md` and
`org-members/research/PAPER_02_NOTES.md` as the source of truth for test IDs, objectives,
run order, and open decisions.  
**Audience:** Any new contributor, paper reviewer, or session starting cold.

---

## 1 — The Dual-Paper Research Programme

This project produces two papers from a single Virginia state procurement corpus
(FY2003–FY2016, ~133M PO lines). **Paper 1** establishes the minimum-cost curve: starting
from CCP1 (the General Ledger baseline every institution already runs), each successive
capability layer of the CKB instrument-ledger architecture adds a precisely measured marginal
cost, and if the full pipeline lands at approximately the same cost as CCP2 (the instrument
ledger baseline), that is the empirical proof of single-pass universality. **Paper 2** uses the
same corpus to test the AI-consequence claim of Theorem 1: the posting process is lossy
compression, and the pre-hierarchy commitment record carries signal the posted record
permanently discards.

The two papers are two facets of the same architectural failure, measured from the same data:
CCP1 is simultaneously the cost-curve anchor (Paper 1) and the substrate against which all AIM
tests fail (Paper 2). The CKB architecture (CCP2–CCP5) simultaneously achieves lower cost
(Paper 1) and produces AI-ready evidence as a natural by-product (Paper 2).

---

## 2 — Paper 1 Objectives: Minimum Cost Curve

### What CCP1 and CCP2–CCP5 measure

**CCP1** is not a point on the minimum cost curve — it is the status-quo baseline every
institution already operates: sort events → post to a General Ledger (account-keyed) →
write GL balances. The instrument identity (vendor, counterparty) is discarded at posting
time. CCP1 measures the cost of this exactly. It also drives the permutation generator: a
separate follow-on step reads the original transactions and computes every key-embedded
balance permutation that a GL-based architecture would need to materialise to answer all
the questions the instrument ledger answers for free. The permutation table size is the
Paper 1 cost explosion evidence; the AIM test suite run against both the GL output and the
permutation table is the Paper 2 information-loss evidence.

**CCP2–CCP5** are verified, reproducible positions on the minimum cost curve of the CKB
instrument-ledger architecture. CCP2 is the left anchor — the irreducible minimum cost of
the new architecture. Each successive CCP adds one capability layer; the delta in
records/cost/time is the finding. The cumulative picture is the minimum cost curve.

### What "done" looks like for Paper 1

Paper 1 is done when:
- CCP1 has three replicates with stable GL output and permutation table size recorded
- CCP2–CCP5 each have three replicates with non-zero, stable values for `records_read`,
  `bytes_written`, `wall_seconds`, and `peak_rss_bytes` in `process_metrics.csv`
- The cost delta CCP1 − CCP2 can be stated (cost of the architectural change)
- The cost delta CCP4 − CCP5 can be stated with confidence (single-pass universality test)

If CCP5 ≈ CCP4, that equality is the empirical proof of single-pass universality. If
CCP5 > CCP4, the divergence and its cause are the finding.

### ✅ Prerequisite resolved — 2026-11-04

**`process_metrics.csv` emission repaired.** Steps 2 and 3 now emit `RESULT` and `RESOURCE`
structured log lines; `records_read`, `records_written`, `elapsed_s`, `input_bytes`,
`output_bytes`, and `peak_rss_bytes` are non-zero. See `SESSION_HANDOFF.md` session log for
details and validation output. CCP cost points are now measurable.

---

## 3 — CCP1 and CCP2–CCP5 Table

| ID | Name | Architecture | Capability | What it measures | AIM pairing |
|----|------|-------------|-----------|-----------------|-------------|
| **CCP1** | General Ledger (status quo) | GL, account-keyed | Sort → GL post → GL balances. Instrument identity discarded. | **Status-quo baseline.** Cost of what every institution already runs. Plus: permutation generator reads original transactions, produces full key-embedded permutation table. | AIM1 family run against GL output AND against permutation table — all variants miss. Demonstrates structural information loss from the outside. |
| **CCP2** | Instrument Ledger (curve anchor) | CKB, instrument-keyed | Sort → CKB post → Instrument Ledger (vendor-keyed balances). Instrument identity preserved. | **Minimum-cost anchor of the new architecture.** Delta CCP1→CCP2 = cost of the architectural change. | — |
| **CCP3** | Instrument Ledger + perspective aggregation | CKB + Step 5 views | Add Step 5 aggregation views alongside the ledger pass. | Marginal cost of reporting materialisation. **The Step 5 views ARE the AIM2–AIM4 feature tables** — same mechanism, same pass, same cost measurement. | AIM2, AIM3, AIM4 run against CCP3 view outputs. Delta CCP2→CCP3 = cost of producing all AI evidence substrate. |
| **CCP4** | CCP3 + revaluation-class engines | CKB + Steps 8, 9 | Add booked FX revaluation and reclass engines (Steps 8, 9). | Marginal cost of generated-SJE engines: additional SJEs, any new sort boundary, additional ledger rows. | — |
| **CCP5** | Full pipeline including allocation | CKB + Steps 6, 7 | Add prior-period allocation and consolidation (Steps 6, 7). | If CCP5 ≈ CCP4: empirical proof of single-pass universality. If CCP5 > CCP4: divergence and cause are the finding. | AIM5 (agency name disambiguation) is prerequisite for clean agency keys in allocation; run before CCP5. |

*Three replicates of each workload on the same input snapshot are required before any cost
comparison is published. A single run is not a cost point.*

---

## 4 — Paper 2 Objectives: AI Consequence of Posting as Lossy Compression

### The two-substrate experiment design

Every AIM test (except AIM0, which is closed) is run **twice**:

1. **Against the CCP1 substrates** (GL output and permutation table): the legacy architecture's
   best effort. The GL is too collapsed to answer the question. The permutation table has more
   dimensions but still cannot recover event-level signal, because permutations multiply posting
   keys but do not restore the original event sequence. Both substrates fail — for different and
   instructive reasons.

2. **Against the CCP2–CCP5 outputs** (instrument ledger and CCP3 views): the CKB architecture.
   Each AIM test succeeds or produces a meaningful result.

The gap between the two substrates is the empirical proof of Theorem 1. It is not a comparison
of algorithms — the same algorithm runs against both. The difference is what survived posting.

### What "done" looks like for Paper 2

Paper 2 is done when each AIM test has been run against both substrates (CCP1 and CCP2/CCP3),
the results reported side by side, and the gap documented. AIM1 has no pass/fail threshold —
it is a measurement; any non-zero gap between the CCP1 and CCP2 counts is evidence. AIM2–AIM5
must each meet their confirmed acceptance thresholds on the CCP2/CCP3 substrate while failing
to meet them on the CCP1 substrate.

---

## 5 — AIM0–AIM5 Table

| ID | Question | Paper 2 claim tested | CCP1 result | CCP2/3 result | Status |
|----|----------|---------------------|------------|--------------|--------|
| **AIM0** | Can PO orders be linked to payments? | Feasibility gate. | **CLOSED 2026-11-01** — aggregate `(AGENCY, fiscal_year)` join available; direct PONUMBER join is not. | — | Closed |
| **AIM1** | How many distinct PO patterns collapse to the same posted key? | Theorem 1, §2.1 — direct non-injectivity count; no ML required. Run as a post-hoc step reading aggregated output. | Run against GL output: counts highly collapsed (many-to-one). Run against permutation table: counts appear to improve but event sequence still unrecoverable. | Run against Instrument Ledger: non-injectivity directly measurable; revision sequence preserved. | Not yet run |
| **AIM2** | Does knowing *what* an agency ordered improve expenditure forecasting? | §5.3 "Does adding pre-hierarchy event features improve on that ceiling?" | Run against permutation table: no commodity sequence; dimensional explosion without event recovery. | CCP3 view C (agency-year commodity mix) enables direct Layer 3 vs. Layer 2 comparison. | Not yet run |
| **AIM3** | Can we predict which vendors will stop doing business with the state? | §5.3 "Does the event stream contain patterns no balance-derived feature can surface?" | Run against GL: no vendor lifecycle concept exists in GL balances. | CCP3 view B (vendor-year survival panel) enables survival model. | Not yet run |
| **AIM4** | Does *when* an agency orders carry information that *when* it pays does not? | §1.1 and §5.1 Track B — ordering seasonality. | Run against GL: only payment dates survive; order dates discarded at posting. | CCP3 view A (monthly order volume) enables STL seasonality test. | Not yet run |
| **AIM5** | Can an AI model tell the difference between "same agency, different spelling" and "actually a different entity"? | §3.3 "The Data-Type Navigation Problem" — preserved names carry information account codes erase. | GL collapses all name variants to one account code; ambiguity silently hidden. | Pre-hierarchy strings preserved; model can flag genuine ambiguity. Also unblocks AIM2 and CCP5. | Not yet run |

---

## 6 — Run Order

```
CCP1  ──► AIM1 variants (vs GL + vs permutation table)  [all miss — Paper 2 evidence]
      │
      └──► permutation table size recorded               [Paper 1 cost explosion evidence]

CCP2  ──► [instrument ledger valid]
      │
      └──► AIM1 (vs instrument ledger)  ──►  AIM3
                                        └──►  AIM4

CCP3  ──► AIM2 (requires AIM5 first)
      │
      └──► AIM5  ──►  AIM2

CCP5  ──► requires AIM5 (clean agency keys for allocation)
```

| Constraint | Reason |
|-----------|--------|
| **CCP1 before any AIM run** | CCP1 produces the GL output and permutation table that are the first substrate for every AIM test. |
| **CCP2 before AIM1 (IL substrate)** | The instrument ledger is the second AIM1 substrate. CCP2 produces it. |
| **CCP3 before AIM2–AIM4** | The Step 5 views (CCP3 outputs) are the feature tables for AIM2–AIM4. |
| **AIM5 before AIM2** | AIM2 requires clean agency identifiers. AIM5 produces them. |
| **AIM5 before CCP5** | Allocation requires clean agency keys. AIM5 provides the reconciliation table. |
| **CCP2 before CCP3, CCP4, CCP5** | Each layers capability onto the prior CKB pass output. |

---

## 7 — Decisions (confirmed 2026-11-04)

All decisions in this section were confirmed by the project owner on 2026-11-04. They are now
fixed. Implementation may proceed on the basis of these values.

### Acceptance thresholds — **CONFIRMED 2026-11-04**

| Decision | AIM tests affected | Confirmed value |
|----------|--------------------|----------------|
| AIM2 RMSE improvement threshold | AIM2 | ≥ 5% improvement over posted-layer baseline |
| AIM2 permutation p-value | AIM2 | < 0.05 |
| AIM3 C-statistic floor | AIM3 | > 0.65 |
| AIM3 permutation p-value | AIM3 | < 0.05 |
| AIM4 seasonality p-value and class count | AIM4 | p < 0.05, ≥ 3 of top 5 classes distinct |
| AIM5 precision floor | AIM5 | ≥ 0.99 |
| AIM5 ambiguity confidence threshold | AIM5 | < 0.70 |

*AIM1 has no pass/fail threshold — it is a measurement. The gap between the CCP1 substrate
result and the CCP2 substrate result is the evidence. Any non-zero gap is informative; a
large gap is strong evidence of structural information loss.*

**Do not adjust these thresholds after seeing results.** They were fixed before any test was run.

### Longitudinal evaluation strategy — **CONFIRMED 2026-11-04: 7-window longitudinal**

7 rolling train/test windows across FY2003–FY2016: train on years 1–7, predict year 8; shift
by one year, repeat. Applies to AIM2 and AIM3 on both substrates (CCP1 and CCP3). Python
scripts must implement the loop across all 7 windows.

### AIM1 revision-suffix inclusion — **CONFIRMED 2026-11-04: include revision suffix**

The `PONUMBER` revision suffix (`-Vn`, e.g. `-V2`, `-V3`) is included as a separate dimension
in the non-injectivity count. Rationale (owner): in financial systems, updates are new events —
one does not revise databases but adds new journal entries. A PO revision is a new commitment
act; it carries information the posted record permanently discards. The CKB view schema must
include `revision_suffix` as a distinct field. This also adds one feature column to AIM2.

### Surrogate key — **CONFIRMED 2026-11-04: SHA256(VENDORID) mod 2^32**

`vendorSurrogateKey = SHA256(VENDORID) mod 2^32`. Stable across runs, semantically neutral,
consistent with the insurance corpus pattern (Paper 2 §5.2). The Scala CKB view emits this
field on every vendor-year row. Do not change the formula after any split assignment has been
computed.

---

## 8 — Related Documents

| Document | Location | What it contains |
|----------|----------|-----------------|
| `TEST_DESIGN_PAPER_1.md` | `ledger-lab/docs/` | Full Paper 1 spec: CCP1 and CCP2–CCP5 workload definitions, permutation generator, input snapshots, required metrics, acceptance evidence shape |
| `TEST_DESIGN_PAPER_2.md` | `ledger-lab/docs/` | Full Paper 2 spec: AIM0–AIM5 test specifications, two-substrate design, open-decisions table, author notes |
| `paper2-tests-plan.md` | `ledger-lab/docs/` | Implementation sub-tasks: `transIPID` fix, PO stream merge, CKB view emission, Python fitting scripts |
| `SESSION_HANDOFF.md` | `ledger-lab/` | Session log with key decisions and context |
| `PAPER_02_NOTES.md` | `org-members/research/` | Original working notes; superseded by this document and `TEST_DESIGN_PAPER_2.md` |
| `next-session-plan.md` | `ledger-lab/docs/` | Planning document; superseded by this document |
