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
from CCP1 (a General Ledger — one representative of the fixed-posting-key class that includes
sub-ledgers, data warehouses, and data marts), each successive capability layer of the CKB
instrument-ledger architecture adds a precisely measured marginal cost, and if the full
pipeline lands at approximately the same cost as CCP2 (the instrument ledger baseline), that
is the empirical proof of single-pass universality. **Paper 2** uses the same corpus to test
the AI-consequence claim of Theorem 1: the posting process is lossy compression, and the
pre-hierarchy commitment record carries signal the posted record permanently discards.

The two papers are two facets of the same architectural failure, measured from the same data:
CCP1 is simultaneously the cost-curve right-wall anchor (Paper 1) and the substrate against
which all AIM tests fail (Paper 2). The CKB architecture (CCP2–CCP5) simultaneously achieves
lower cost (Paper 1) and produces AI-ready evidence as a natural by-product (Paper 2).

**Note on CCP1 as a class representative:** The GL is the most collapsed instance of the
fixed-posting-key class — narrowest key, most information discarded at posting time. A
purchasing sub-ledger, data warehouse, or data mart preserves more dimensions but shares the
same structural property: pre-computed answers to questions defined at design time, unable to
answer unanticipated questions without returning to raw events. CCP1 measures the GL as a
right-wall proxy for this class. The cost gap CCP1→CCP2 is a lower bound on the true
architectural savings for institutions whose fixed-key store preserves more dimensions than
a GL.

---

## 2 — Paper 1 Objectives: Minimum Cost Curve

### What CCP1 and CCP2–CCP5 measure

**CCP1** is not a point on the minimum cost curve — it is the right-wall anchor: sort
events → post to a General Ledger (account-keyed) → write GL balances. The GL is the most
collapsed instance of the fixed-posting-key class (sub-ledgers, data warehouses, and data
marts are less collapsed instances of the same class — all sit on the right wall). The
instrument identity (vendor, counterparty) is discarded at posting time. CCP1 measures the
cost of this exactly. It also drives the permutation generator: a separate follow-on step
reads the original transactions and computes every key-embedded balance permutation that a
fixed-key architecture would need to materialise to answer all the questions the instrument
ledger answers for free. The permutation table size is the Paper 1 cost explosion evidence;
the AIM test suite run against both the GL output and the permutation table is the Paper 2
information-loss evidence.

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

### Overriding claim (2026-11-05 reframe)

> No model — no matter how capable, trained on any substrate — can answer Net Income from
> events alone. Net Income is not a fact about the world. It is a fact about how an
> organisation chose to classify and accumulate the world through its chart of accounts and
> posting rules. That classification lives in the posting layer, not in the events.

Paper 2 is structured in two parts:

**Part 1 — Question space (structural proof):** The posting architecture defines a question
space smaller than the space of economically meaningful questions. The AIM tests (AIM1, AIM3,
AIM4) illustrate this claim concretely — they are not the primary evidence. Paper 1's
permutation table provides the structural argument; AIM tests make the information destruction
specific and observable. AIM1 (non-injectivity) is special: it motivates the hardware cost
question (see Engine Graph second-curve note in `COST_GRAPH_DESIGN.md`).

**Part 2 — Substrate quality and the three-model experiment:** The primary evidence for Paper 2.
The dominant institution pattern — pretrained LLM fine-tuned on posted data — fails because
the language prior survives fine-tuning and introduces spurious correlations into financial
structure. The three-model experiment measures this directly.

### The three-model experiment (primary Paper 2 evidence)

| # | Model name | Training substrate | Language prior | Can answer Net Income? | Can answer vendor survival? |
|---|------------|-------------------|---------------|----------------------|----------------------------|
| 1 | **Posted-FineTune** | Pretrained LLM fine-tuned on posted data (`GL20YY.csv`) | Yes — survives | From posting layer only | No — field absent |
| 2 | **IL-FineTune** | Pretrained LLM fine-tuned on Instrument Ledger | Yes — survives | Yes | Yes |
| 3 | **IL-Native** | Structured model built directly from IL — no pretrained LLM | None | Yes | Yes |

**Gap 1→2** = substrate effect (same contaminated language prior, better data)
**Gap 2→3** = language prior effect (same IL data, prior removed — "See Spot Run")
**Gap 1→3** = total cost of the current institution paradigm

The three-model experiment requires Python model training code. This is post-engine work
consuming pipeline outputs — it does not substitute for the Scala engine. Owner approval
required before writing Python scripts (AGENTS.md).

### The two-substrate design (AIM tests — Part 1 illustration)

Every AIM test (except AIM0, which is closed) is run **twice**:

1. **Against the CCP1 substrates** (GL output and permutation table): the legacy architecture's
   best effort. Both substrates fail — for different and instructive reasons.

2. **Against the CCP2–CCP5 outputs** (instrument ledger and CCP3 views): the CKB architecture.
   Each AIM test succeeds or produces a meaningful result.

The gap between the two substrates illustrates the information-destruction claim. It is not a
comparison of algorithms — the same algorithm runs against both. The difference is what
survived posting.

### What "done" looks like for Paper 2

Paper 2 (Part 1) is done when AIM1, AIM3, and AIM4 have been run against both substrates and
the gap documented. AIM1 has no pass/fail threshold — any non-zero gap is evidence.

Paper 2 (Part 2) is done when the three-model experiment has been run: Posted-FineTune,
IL-FineTune, and IL-Native trained on their respective substrates, evaluated on the same
downstream tasks, gap between models quantified, and Net Income closing argument demonstrated.

**Note on AIM2 and AIM5:** AIM2 (expenditure forecasting) is subsumed by the Posted-FineTune
vs IL-Native comparison on forecasting tasks — it remains in the test plan but is not a primary
Paper 2 result. AIM5 (agency name classifier) is infrastructure (data quality); it unblocks
AIM2 and CCP5 but is not a primary Paper 2 result.

---

## 5 — AIM0–AIM5 Table

**Role clarification (2026-11-05):** AIM1, AIM3, and AIM4 are the primary Paper 2 Part 1
illustrations — they make the information-destruction claim concrete. AIM2 and AIM5 have
reduced primary-result status (see §4 above). The three-model experiment — Posted-FineTune,
IL-FineTune, IL-Native (§4) — is the primary Paper 2 Part 2 evidence.

| ID | Question | Paper 2 role | CCP1 result | CCP2/3 result | Status |
|----|----------|-------------|------------|--------------|--------|
| **AIM0** | Can PO orders be linked to payments? | Feasibility gate. | **CLOSED 2026-11-01** — aggregate `(AGENCY, fiscal_year)` join available; direct PONUMBER join is not. | — | Closed |
| **AIM1** | How many distinct PO patterns collapse to the same posted key? | **Primary — Part 1 illustration.** Direct non-injectivity count; no ML required. Also motivates the hardware cost question (bridge to TDS/z/OS). | Run against GL output: counts highly collapsed (many-to-one). Run against permutation table: counts appear to improve but event sequence still unrecoverable. | Run against Instrument Ledger: non-injectivity directly measurable; revision sequence preserved. | Not yet run |
| **AIM2** | Does knowing *what* an agency ordered improve expenditure forecasting? | **Secondary.** Subsumed by Posted-FineTune vs IL-Native forecasting comparison in the three-model experiment. Remains in test plan; not a standalone primary result. | Run against permutation table: no commodity sequence; dimensional explosion without event recovery. | CCP3 view C (agency-year commodity mix) enables direct comparison. | Not yet run |
| **AIM3** | Can we predict which vendors will stop doing business with the state? | **Primary — Part 1 illustration.** Demonstrates information present in pre-hierarchy events and absent from any fixed-posting-key store. | Run against GL: no vendor lifecycle concept exists in GL balances. | CCP3 view B (vendor-year survival panel) enables survival model. | Not yet run |
| **AIM4** | Does *when* an agency orders carry information that *when* it pays does not? | **Primary — Part 1 illustration.** Ordering seasonality invisible in payment records — demonstrates temporal destruction. | Run against GL: only payment dates survive; order dates discarded at posting. | CCP3 view A (monthly order volume) enables STL seasonality test. | Not yet run |
| **AIM5** | Can an AI model tell the difference between "same agency, different spelling" and "actually a different entity"? | **Secondary — infrastructure.** Data quality step; unblocks AIM2 and CCP5. Not a primary Paper 2 result. | GL collapses all name variants to one account code; ambiguity silently hidden. | Pre-hierarchy strings preserved; model can flag genuine ambiguity. | Not yet run |

---

## 6 — Run Order

### Conservative staged sequence (approved 2026-11-04)

The full-corpus run is expensive and runs once. Work proceeds in four stages so that model
code correctness and directional results are confirmed before any 14-year commitment.

```
Stage 1 — No corpus run needed
  AIM5  reads agency_alias_candidates.csv (already exists)
        produces agency_reconciliation_table.csv
        unblocks AIM2 and CCP5

Stage 2 — 1-year validation run (FY2003 only)
  Pipeline correctness gate: confirm all view files produced,
  process_metrics.csv rows non-zero, ledger controls pass.
  Run AIM1/3/4 fitting scripts against 1-year outputs:
    confirm code executes and produces plausible output
    (not against locked thresholds — 1 year is too little data)

Stage 3 — 3–5 year slice (FY2003–FY2007)
  Run pipeline on 5 years.
  Run AIM1, AIM3, AIM4 against slice outputs:
    AIM1: verify non-zero non-injectivity gap (directional check)
    AIM3: C-stat computable and > 0 (survival signal exists)
    AIM4: at least one class shows Q4 pattern (seasonal signal exists)
  Run AIM2 against slice:
    confirm code executes across 7-window loop
    (7-window final verdict requires full 14 years)

Stage 4 — Full 14-year corpus (FY2003–FY2016), 3 replicates each CCP
  All AIM tests run against locked thresholds.
  Final evidence for both papers.
```

| Constraint | Reason |
|-----------|--------|
| **AIM5 first** | No corpus run needed; unblocks AIM2 and CCP5; cheapest possible first result. |
| **1-year validation before any slice** | Confirms pipeline correctness before investing compute. |
| **Fixture for model smoke** | Use existing `FY03q1exp_small.txt` + `VA_opendata_FY2003_small.txt` to confirm script syntax/execution only; too few rows for any statistical result. |
| **FY2003–FY2007 slice for directional check** | AIM3 survival needs ≥4 years of `present_next_year` signal; AIM4 STL needs ≥3 years of monthly volume; 5 years covers both. |
| **Full 14 years for locked thresholds** | AIM2 7-window protocol requires FY2003–FY2016; AIM3/AIM4 thresholds were set against the full corpus. |
| **AIM5 before AIM2** | AIM2 requires clean agency keys from AIM5 reconciliation table. |
| **AIM5 before CCP5** | Allocation (Step 6) requires clean agency keys. |
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
| `TEST_DESIGN.md` | `ledger-lab/docs/` | **Unified** CCP cost-point workload definitions (CCP1–CCP5) and AIM model evidence test specifications (AIM0–AIM5). Pipeline readiness, run sequence, locked decisions, workload manifest template. Supersedes `TEST_DESIGN_PAPER_1.md` and `TEST_DESIGN_PAPER_2.md`. |
| `ENGINE_IMPLEMENTATION_PLAN.md` | `ledger-lab/docs/` | Engine implementation sub-tasks for both papers: `transIPID` fix, PO stream merge, CKB view emission, cost surface axis design, Python fitting scripts |
| `SESSION_HANDOFF.md` | `ledger-lab/` | Session log with key decisions and context |
| `PAPER_02_NOTES.md` | `org-members/research/` | Original working notes; superseded by this document and `TEST_DESIGN.md` |
| `next-session-plan.md` | `ledger-lab/docs/` | Planning document; superseded by this document |
