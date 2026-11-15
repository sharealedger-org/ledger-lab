# Phase 2: Synthetic Data Generation and Engine Demonstration Plan

**Current transition, 2026-11-15:** Complete the Phase 1 results-based revision of
Papers 1 and 2 while producing initial Phase 2 synthetic inputs to inform Papers
3 and 4. A further versioned data-design round may clarify their questions and
protocols before freezing formal experiments and the Control handoff. See the
[strategy](SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md#purpose-and-sequencing).

**Integrated experiment numbering, owner clarification 2026-11-15:** Phase 1 is
direct VA corpus work. Phase 2 is the current initial vendor-centered synthetic
work, likely informing Papers 3 and 4 on improved cost curves and AI models.
Phase 3 extends to the broader CKB topology, from tentative allocations to reclass.
Generation uses these same experiment phases. Earlier broad engine strands below
are a backlog spanning Phases 2 and 3, not all prerequisites for Phase 2.

**2026-11-15 controlling sequencing update:** See the
[connected-process strategy](SYNTHETIC_CONNECTED_PROCESS_STRATEGY.md) for the
two-stage generation plan: vendor-centered evidence with limited employee/revenue
connections for early Papers 3 and 4 AI work, followed by broader process topology,
tentative allocation relationships and temporary future-dated scenarios. It also
records bias controls and the longer-term AI vision. These are planning decisions,
not implemented capabilities; they supersede conflicting earlier sequencing below.

Drafted 2026-11-14. **Implementation started 2026-11-14 by owner direction.**

**Earlier owner sequencing decision, 2026-11-15 (historical numbering):** Reclass
engine creation/extension belongs to the Q2 response, after the questions and
evaluation criteria are fixed. Under the integrated numbering above this broader
reclass work is in Phase 3, not a prerequisite for current Phase 2 generation.
Before question definition: connect/scale the synthetic inputs, build synthetic
validation views, complete multidimensional vendor CKB reporting, verify a connected
fixture, then execute and inspect one new full corpus run. Date-effective lookups
and Switch/Recast validation do not require reclass transfer journals. Existing
legacy reclass code is not a new pre-question deliverable; do not expand it or run
a new reclass-engine demonstration as a prerequisite. This overrides C6's earlier
position in the preparatory timeline. Prior three expanded execution datasets were
retired by owner authorization; compact evidence and Gold remain.

| Strand | Status | Evidence |
|---|---|---|
| A Round 1 | **Complete** | `data/VendorMaster.csv` updated: 232 REV→GOV-RECEIPT/GOV, 183 EXP geo filled from state |
| A Round 2 | **Complete** | `instSizeTier` added: MICRO 211, SMALL 594, LARGE 211, GOVT-AGENCY 232 |
| A Round 3+4 | **Complete** | `instContractType`, `instPaymentChannel`, `instCounterpartySector`, `instExposureType`, `instMaturityProxy`, `instRiskWeightProxy` added |
| A Round A5a | **Complete** | `instFXCurrency`=AUD for HALLPRINT PTY LTD (only INT vendor); `data/FX_RATES.csv` written (180 monthly AUD/USD rows FY2003–FY2016, synthetic from RBA approx.) |
| B Strand | **In progress** | ACFR FY2012 K+O downloaded; anchor fixture written (`data/acfr_anchors/`); fund filter and vendor-sum check pending |
| C–F | Not started | Awaiting Phase 1 quality finding decision (see below) |

**New finding logged 2026-11-14:** CKB PO ingestion quarantines ~24,700 records that the control recovers via RFC-4180 quote-aware boundary detection. See [`VERIFICATION_AUDIT_REGISTER.md`](VERIFICATION_AUDIT_REGISTER.md) (bottom entry) and [`TODO.md`](../TODO.md) Track D section. Owner decision required: fix before or after Strand C–F work.

This plan covers three interlinked strands of Phase 2 work: (1) completing and extending the
Instrument Master (SAL) with rule-derived synthetic attributes; (2) downloading publicly available
Virginia audited financial data and constructing named summary-level adjustment records that allow
the detailed CKB ledger to agree with published statements; and (3) building the engine-generated
journal demonstrations (allocation, multi-currency, reclass, IUE, budget variance, consolidation).
Together these strands give Phase 2 its empirical substance: a richer financial foundation, a
verified tie to published accounts, and a measured engine adaptability story.

None of this work starts without the owner decisions flagged in each section. Python may only be
used for the post-engine preparation steps explicitly approved here (see §6). No Python writes
accounting entries or runs financial logic in place of Scala.

---

**Owner CKB clarification, 2026-11-15:** Multiple business processes require an
explicit multi-CKB topology, not a universally vendor-keyed flow. Temporal reclass
logic stays within each relevant entity's CKB passes; its implementation remains
a Q2 response. Define process/grain/key/state/output contracts before selecting
sorts, physical passes or corpus-generation topology. See
[execution model](CKB_LAYER_EXECUTION_MODEL.md).

## 1. The Three-Strand Architecture

```
Strand A — SAL enrichment (VendorMaster.csv)
  Rounds 1–4: rule-derived attributes on the existing 1,248 instruments
  New international perspective: FX currency, domicile, federal-agency counterpart identities

Strand B — ACFR tie-out and named adjustments
  Download Virginia ACFR summary data
  Construct named adjustment SJE records (non-vendor, named provenance)
  Show: vendor totals + named adjustments = published statement figures

Strand C — Engine demonstration (Scala CKB engines)
  Allocation, budget variance, multi-currency revaluation (synthetic FX), IUE accrual,
  GAAP reclass, consolidation instrument trace
  Each adds CCP-class capability measured as incremental passes/rows/elapsed time
```

These strands are independent to build but feed each other at Q2-draft:
- Strand A attributes gate which engine demonstrations are meaningful (e.g., `instFXCurrency`
  gates multi-currency revaluation).
- Strand B adjustments establish the tie-out baseline that engine-generated journals must
  preserve (a generated revaluation must not break the ACFR reconciliation).
- Strand C measurements supply the incremental cost evidence for the Phase 2 Engine graph.

---

## 2. Strand A — SAL Attribute Rounds

### 2.1 Round 1 — Fill existing gaps (prerequisite for all later rounds)

**Current state:** 1,016 EXP instruments have `instNIGPClass` and `instGeoRegion` populated.
232 REV instruments have blank `instNIGPClass`, `instNIGPDesc`, and `instGeoRegion`.
One EXP instrument (HALLPRINT PTY LTD, instID 218249) has `instGeoRegion = INT`.

**Rule-based assignment — no new data download required:**

| Instrument class | `instNIGPClass` rule | `instNIGPDesc` rule | `instGeoRegion` rule |
|---|---|---|---|
| REV instruments | Assign sentinel `GOV-RECEIPT` | `"Government Receipt — Revenue Source"` | Assign `GOV` sentinel |
| EXP blank `instGeoRegion` (there is 1: empty string) | unchanged | unchanged | Derive from `instVendorState` using the existing region mapping table |

**New sentinel values to document:** `GOV-RECEIPT` (NIGP class), `GOV` (geo region). Both are
declared synthetic assignments, not real NIGP classifications.

**Owner decision A1-1:** Confirm `GOV-RECEIPT` and `GOV` as the sentinel identifiers, or supply
alternatives.

**Output:** Complete Round 1 in the Python preparation script (approved post-engine utility).
Write updated `VendorMaster.csv`; record rule version and date in `instAuditTrail`.

---

### 2.2 Round 2 — Vendor size tier (`instSizeTier`)

**Derivation:** Classify each EXP instrument as SMALL / LARGE / MICRO using spend-percentile
within NIGP class across all 14 fiscal years from the Gold LDGR files.

| Tier | Rule | Basis |
|---|---|---|
| MICRO | Below 25th percentile of total 14-year spend within NIGP class | Instrument-level spend from LDGR |
| SMALL | 25th to 75th percentile | Same |
| LARGE | Above 75th percentile | Same |

REV instruments: assign sentinel `GOVT-AGENCY`.

**New report unlocked:** Small-business utilization rate by agency × NIGP class × year. Answers
a real Virginia procurement policy question the GL cannot answer.

**Owner decision A2-1:** Accept the spend-percentile definition, or substitute an SBA DSBS
lookup by vendor name and state (requires web access and owner approval per AGENTS.md).

**Owner decision A2-2:** Approve the Python preparation script before it is written (AGENTS.md
requires this; Scala cannot classify by percentile within NIGP class without a sort contract
declaration, making Python the right tool for this post-engine preparation step).

---

### 2.3 Round 3 — Contract type and payment channel (`instContractType`, `instPaymentChannel`)

**Derivation from existing reference data — no new download:**

| Field | Rule | Source |
|---|---|---|
| `instContractType` | NIGP class 918 (Interagency) → `INTERAGENCY`; NIGP class range 001–899 services → `CONTRACTED`; goods → `PURCHASE-ORDER`; REV → `RECEIPT` | `aRefObjectCSV.txt` object category, `instNIGPClass` |
| `instPaymentChannel` | Amount > $50,000 → `WIRE`; > $5,000 → `EFT`; otherwise → `CHECK` | 14-year median annual spend per instrument |

**New reports unlocked:** Procurement channel compliance by agency; interagency spend
identification (prerequisite for consolidation elimination); EFT adoption rate.

**Owner decision A3-1:** Accept or adjust the amount thresholds for channel derivation.

---

### 2.4 Round 4 — Basel bridge attributes (`instCounterpartySector`, `instExposureType`,
`instMaturityProxy`, `instRiskWeightProxy`)

**Derivation:**

| Field | Rule |
|---|---|
| `instCounterpartySector` | NIGP class prefix → CORPORATE / GOVERNMENT / NONPROFIT / INDIVIDUAL / FINANCIAL |
| `instExposureType` | Object code 2-digit category → OPERATING-EXP / CAPITAL-EXP / TRANSFER / REVENUE |
| `instMaturityProxy` | `instContractType` × object category → SHORT-TERM / MEDIUM-TERM / LONG-TERM / PERPETUAL |
| `instRiskWeightProxy` | `instCounterpartySector` → Basel standardized approach: GOVERNMENT=0% / FINANCIAL=20% / CORPORATE=100% / INDIVIDUAL=75% / NONPROFIT=50% |

This is the point where the two cost curves diverge in meaning: the GL reports appropriations
and expenditures; the risk view reports counterparty exposure, maturity gaps, and capital proxies.
In the IL these derive from the same balance store.

**Owner decision A4-1:** Approve the NIGP-class → counterparty sector mapping table before the
script is written. The specific classes are in `data/aRefObjectCSV.txt`.

---

### 2.5 International perspective — new attributes

The existing dataset has one real INT vendor (HALLPRINT PTY LTD, an Australian academic publisher)
and one federal-grant source class (class code `84`, "FEDERAL GRANTS AND CONTRACTS") that links
Virginia to federal counterpart agencies. These two anchors justify a small international and
federal-counterpart extension.

#### 2.5a FX currency assignment (`instFXCurrency`)

For multi-currency revaluation to be demonstrable, a small population of instruments must carry
a non-USD functional currency. The VA corpus is USD-only but the structural argument holds with
assigned currencies.

**Proposed assignment:**

| Condition | `instFXCurrency` assigned |
|---|---|
| `instGeoRegion = INT` (1 vendor: HALLPRINT PTY LTD, Australian publisher) | AUD |
| EXP vendors with `instVendorState` blank and international-pattern names | GBP or EUR (rule to be declared) |
| All other EXP and all REV | USD |

**Why AUD is the right choice for HALLPRINT:** It is an Australian academic publisher. Using AUD
makes the assignment documentably correct, not arbitrary.

**New capability unlocked:** `currencyRevaluation.scala` can generate instrument-specific REVAL_SJE
entries for HALLPRINT's balance each period when AUD/USD moves. The drill-down gap demonstration
becomes concrete: the GL sees "FX adjustment $X"; the IL answers "which publisher, which balance,
which currency pair."

**Owner decision A5-1:** Accept the AUD assignment for HALLPRINT and the rule for any additional
INT-pattern vendors, or supply a different assignment.

**Owner decision A5-2:** Provide a small synthetic AUD/USD rate table (e.g., quarterly rates
FY2003–FY2016) or accept a fixture rate table derived from published historical RBA data
(requires web access — owner approval per AGENTS.md).

#### 2.5b Federal counterpart identity (`instFederalCFDA`, `instFederalAgency`)

The REV source class 84 ("FEDERAL GRANTS AND CONTRACTS") represents $8–10 billion of Virginia's
annual revenue. Each source code under class 84 is a CFDA program number (e.g., `84164` = Eisenhower
Math/Science). The REV instruments for these source codes can be enriched with:

| Field | Rule |
|---|---|
| `instFederalCFDA` | The source code value itself (e.g., `84164`) when `SRCCLS_SOURCE_CLASS_CODE = 84` |
| `instFederalAgency` | CFDA prefix → federal agency: `84` → Dept. of Education, `20` → Dept. of Transportation, `93` → HHS, etc. |
| `instGeoRegion` | Override `GOV` with `FED` for federal grant sources |

**Why this matters for the international perspective:** Federal grants flow from Washington to
Richmond. They cross a legal-entity boundary (federal government → state government) in a way
that is structurally analogous to an intercompany transaction in a corporate group. The
consolidation and elimination demonstrations become more meaningful when these cross-boundary
flows are identifiable at the instrument level.

**USAspending.gov linkage opportunity:** A later sub-task (requires owner approval for web
access) could pull the total federal award amount to Virginia recipients by CFDA program and
compare it to the `apa_revenue` Federal source rows — establishing an independent reconciliation
anchor for Strand B.

**Owner decision A5-3:** Accept the CFDA-code enrichment for REV source-84 instruments.

---

## 3. Strand B — ACFR Tie-Out and Named Adjustments

### 3.1 Why this matters

The owner's stated direction (2026-11-12): "A user who queries the subledger by PO or vendor
should be able to tie the result to a number in those statements. Adjustments with no vendor
(aggregates, allocations, currency effects) always exist. The report should show vendor totals
plus an 'other' made of named adjustment records, not a plug."

This is both the most powerful demonstration of the architecture and the most demanding. It
requires:
1. A published audited figure (the ACFR);
2. A population of instrument-level transactions that partially explains it;
3. Named adjustment records that explain the remainder.

The adjustments are real categories (basis adjustments, non-cash items, intergovernmental
transfers, encumbrances) with published values from the ACFR. They are not plugs.

### 3.2 Target reconciliation — General Fund FY2012

The ACFR FY2012 section G provides the following anchor:

| Item | Basis | Amount ($thousands) |
|---|---|---|
| General Fund total expenditures | GAAP modified accrual | 16,972,740 |
| General Fund total expenditures | Budget (cash) basis | 16,894,014 |
| Difference (accrual vs. cash) | Named in Note 1 | 78,726 |

The corpus expenditure sum for FY2012 (from `file_profile.csv`) is approximately $44.6 billion
across all funds — about 2.6× the ACFR governmental funds total. The difference is explained
by: Enterprise funds (higher education, etc.), gross vs. net presentation, and intra-fund
transfers. These are named categories, not unknown.

**Proposed tie-out approach for one fund-year (General Fund FY2012):**

| Step | Action | Expected result |
|---|---|---|
| B1 | Filter corpus to General Fund expenditure rows (Fund code `01`) for FY2012 | Corpus subset |
| B2 | Sum vendor-level instrument expenditure from LDGR for General Fund FY2012 | Vendor subtotal |
| B3 | Identify named categories that explain the gap: payroll pseudo-vendor, non-cash food stamps (~$1.4B), intergovernmental transfers, accrual adjustments | Named gap components |
| B4 | Construct one named adjustment SJE per gap category | Adjustment SJEs |
| B5 | Vendor total + adjustment total = ACFR published figure | Tie-out check |

The adjustment records are explicitly labeled with provenance class: `ADJ_NONCASH` (food stamps),
`ADJ_ACCRUAL` (basis difference), `ADJ_INTERFUND` (intergovernmental). They are not vendor
records; they carry no `instID` from the instrument dimension. They are the "unattributable but
named" category the owner described.

**Owner decision B-1:** Confirm General Fund FY2012 as the first target fund-year.

**Owner decision B-2:** Supply or confirm access to ACFR section K (notes) and section O
(internal service funds) for FY2012 to complete the basis-reconciliation entries.

**Owner decision B-3:** Confirm the payroll pseudo-vendor identity in the corpus — `0000000000`
is the unattributed group that failed the reporting guard. Is payroll a subset of `0000000000`,
or does it have its own vendor code?

### 3.3 Downloading ACFR data

The Virginia DOA publishes section PDFs at known URLs:
```
https://www.doa.virginia.gov/reports/ACFReport/2012/G_Major_Governmental_Funds.pdf
https://www.doa.virginia.gov/reports/ACFReport/2012/K_Notes.pdf
https://www.doa.virginia.gov/reports/ACFReport/2012/L_Required_Supplemental.pdf
```

These were probed in the 2026-11-12 survey (see `docs/VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md`).
Sections G and L were read; K and O were not opened. The plan is to `curl` specific section PDFs
and extract the named adjustment amounts manually — no Python financial processing. The extracted
values become a small fixture file (`data/acfr_anchors/FY2012_general_fund.csv`) that the
adjustment SJE generator reads.

**Owner decision B-4:** Approve web access to `doa.virginia.gov` for ACFR section downloads.

### 3.4 Adjustment SJE format

Adjustment records use the existing SJE schema but with declared fields:

| Field | Value |
|---|---|
| `ldgrJrnlType` | `ADJ` |
| `ldgrIPID` | Named code (e.g., `ADJ-NONCASH-FOODSTAMPS`, `ADJ-ACCRUAL-BASIS`) |
| `ldgrContractID` | Adjustment category |
| Amount | Published figure from ACFR |
| `ldgrBookCodeID` | `GAAP` or `BUDGET` to track basis |

These records feed the same SJE pipe as vendor transactions. The balance check is:

```
SUM(vendor LDGR for General Fund FY2012) + SUM(named adjustments) = ACFR published total
```

This is the architecture's empirical proof of the owner's claim: not a plug, but a named
accounting population with declared provenance.

---

## 4. Strand C — Engine Demonstrations

Each engine demonstration must pass the architecture gate (`docs/AGENTIC_ARCHITECTURE_GATES.md`)
before implementation. The gate requires: declared workload, record contracts, CKB placement,
materialization profile, controls, and evidence.

### 4.1 C-1 Allocation drill-down (CCP4-class capability)

**Status:** `financialAllocation.scala` exists; `AllocationRules.csv` has two rules; fixture tests
pass. This is the closest-to-complete engine.

**Phase 2 extension:**
- Add a cross-agency shared-services allocation rule (Agency A overhead → Agency B + C based on
  FTE or spend share). This is the intergovernmental analog: the state pays a central IT vendor
  and distributes the cost to program agencies.
- Run on the full corpus after Strand A Round 2 (`instSizeTier`) is complete.
- Measure: incremental physical passes, incremental input rows, incremental elapsed time vs. the
  baseline without allocation.

**New question it answers:** "Which vendors (instruments) absorbed the most overhead from the
central IT allocation?" Unanswerable from the GL allocation entry alone.

### 4.2 C-2 Budget variance drill-down (CCP4-class)

**Status:** `forecastingBudgeting.scala` exists and produces `BUDGET_LDGR{year}.csv`. Fixture
complete.

**Phase 2 extension:**
- Run the full-corpus budget pass for FY2012 so `BUDGET_LDGR2012.csv` exists alongside
  `LDGR2012.csv`.
- Write a ViewSpec join row: actuals LDGR × budget LDGR on `ldgrIPID + ldgrNominalAccountID +
  fiscal year`, computing `actual_amount - budget_amount`.
- Demonstrate: vendor-level budget variance for one over-spending agency in FY2012. The GL shows
  the agency is over budget; the IL names which vendors drove it.

**Tie-out linkage (Strand B):** The ACFR budget vs. actual schedule (section L) shows General
Fund actual expenditure of $16,894,014K on the budget basis. The budget variance demonstration
should agree with this at the fund level before drilling to vendor level.

### 4.3 C-3 Multi-currency revaluation (CCP4-class, `currencyRevaluation.scala`)

**Status:** `CurrencyRevaluation.scala` implemented for the synthetic one-period fixture (reproduces
the spec's 559.47 × 0.02 = 11.19 example). Not run on the full corpus.

**Phase 2 extension (gated on Strand A Round A5a: `instFXCurrency` = AUD for HALLPRINT):**
- Add HALLPRINT PTY LTD's annual balance to the opening ledger partition.
- Supply a synthetic AUD/USD rate table (quarterly, FY2003–FY2016).
- Run `CurrencyRevaluation.scala` for each period where HALLPRINT has a non-zero balance.
- Measure: how many periods generate a non-zero REVAL_SJE; what is the cumulative revaluation
  effect on the LDGR balance.

**Drill-down demonstration:** The GL sees one FX adjustment line per period. The LDGR shows
the specific Australian vendor, its AUD balance, the rate used, and the resulting USD adjustment.
One ViewSpec row answers: "What is Virginia's unrealized FX exposure by publisher?"

**Architecture gate required before implementation.** HALLPRINT is the only real INT vendor;
any extension to additional vendors requires Strand A A5a completion and an explicit assignment.

### 4.4 C-4 Interest/IUE accrual (CCP4-class, new engine)

**Status:** Planned in `DATA_ENHANCEMENT_PLAN.md` §6.5 as D-5f. No Scala implementation yet.

**Phase 2 design:**
- Assign `instInterestRate` to capital-expenditure vendors (object codes 21, 22, 23 — equipment
  and property leases). Rule: OBJ 21/23 → synthetic rate 4.5% per annum; all others → 0%.
- Write `InterestAccrual.scala`: reads LDGR + VendorMaster, generates `ACCRUAL_SJE` per instrument
  per period using `outstanding_balance × rate / 12`.
- Metric: accrual income attributable to identified instruments = 100% in IL, 0% in GL.

**Architecture gate required before implementation.**

**Owner decision C4-1:** Accept the 4.5% synthetic rate for capital-lease instruments, or supply
an alternative (e.g., the Virginia Treasury's published short-term rate for the period).

### 4.5 C-5 Consolidation instrument trace (CCP5-class)

**Status:** `consolidation.scala` (legacy balance-level implementation) exists. `InteragencyTransfers.csv`
is skeletal — only fixture rows. Not the target CKB design.

**Phase 2 extension:**
- Populate `InteragencyTransfers.csv` with five real interagency pairs from the corpus: agencies
  that appear in both EXP and REV records for the same NIGP class / source in the same year
  (suggesting shared-services or pass-through).
- Run the consolidation pass; show that vendor X (a shared IT services provider) appears in both
  Agency A's expenditure and Agency B's revenue in the same period.
- The consolidated LDGR retains the instrument ID; the consolidated GL-analog view does not.

**Metric:** Count of vendors identifiable as the source of interagency flows in the consolidated
IL vs. zero in the consolidated GL-analog.

**Tie-out linkage (Strand B):** The ACFR consolidation entries (intergovernmental eliminations)
are named in Section G. The instrument-level consolidation should produce elimination SJEs whose
net equals the ACFR elimination line for the agencies involved.

### 4.6 C-6 Arrangement reclass audit trail (deferred to Q2 response)

**Status:** `arrangementReclass.scala` exists; `VendorUpdate.csv` drives it. The reclass fixture
demonstrates instrument-specific reclass SJE generation.

**Scope gate (owner, 2026-11-15):** The following is a candidate response-stage
extension, not preparation before Q2. Existing-code claims remain subject to the
audit register; this historical example is not a selected Q2 or acceptance metric.

**Candidate extension after Q2 is finalized:**
- Construct a `VendorUpdate.csv` scenario where a vendor changes `instTypeID` from `EXP` to `REV`
  in mid-corpus (e.g., FY2010) — modeling a supplier becoming a revenue partner.
- Run the reclass pass; verify the RECLASS_SJE entries balance and that downstream LDGR balances
  reflect the new classification from FY2010 onward.
- Compare: a legacy system would record one net reclassification entry; the IL records N entries
  (one per period × instrument). The audit trail density difference is the metric.

---

## 5. AI Model Connection (Track B2 Phase 2)

The Strand A attributes and Strand C engine outputs connect to the Track B2 AIM and three-model
work in three ways:

### 5.1 AIM3 feature enrichment

The current AIM3 model predicts vendor survival (presence in year t+1) from posting features.
Phase 2 Strand A attributes add new features the GL cannot provide:
- `instSizeTier` (small/large/micro) — does vendor size predict survival?
- `instContractType` (contracted/PO/interagency) — does contract type matter?
- `instCounterpartySector` and `instRiskWeightProxy` — do higher-risk vendors churn faster?

These are IL-only features; the GL has none of them. A matched comparison (AIM3 with vs. without
SAL attributes, same population, same periods) tests whether the instrument dimension adds
predictive signal beyond the posted totals.

**Owner decision M-1:** Authorize the AIM3 enrichment comparison as a Track B2 sub-experiment
after Strand A Round 2 is complete.

### 5.2 Field-label training pilot (pending from 2026-11-13)

The authorized pilot (`docs/TRAINING_LANGUAGE_PILOT.md`) awaits execution permission. The Phase 2
Strand A attributes expand the vocabulary: a verbalizer can now describe a vendor as
"large IT contractor in the Southeast, capital-expenditure counterparty" instead of just a name
and amount. This richer verbalization is what the instrument ledger makes possible and the GL
cannot supply.

**Dependent on:** Strand A Rounds 1–3 complete; owner authorization for model download/training.

### 5.3 Net income / fund balance prediction (D9)

With Strand B's ACFR anchor and named adjustment records, D9 becomes tractable:

- Net expenditure per agency per year from the LDGR (existing, confirmed);
- Plus the named adjustments from the ACFR tie-out (Strand B);
- Equals the ACFR fund balance figure on the declared basis.

A model that learns this reconciliation from the instrument-level LDGR — including the named
adjustments — can answer "what will the General Fund balance be?" in a way a GL-only model
cannot, because the GL-only model cannot distinguish vendor-driven from adjustment-driven
movements.

---

## 6. Python approval scope for this plan

Per `AGENTS.md`, Python requires explicit owner approval for each new script. The following
preparation steps are proposed as Python utilities (post-engine, no financial logic, no accounting
entries):

| Script | Purpose | Approval status |
|---|---|---|
| `scripts/prepare_vendormaster_round1.py` | Fill REV sentinel values and blank `instGeoRegion` | **Requires owner approval (A1-1)** |
| `scripts/prepare_vendormaster_round2.py` | Compute spend-percentile tiers from Gold LDGR | **Requires owner approval (A2-2)** |
| `scripts/prepare_vendormaster_rounds3_4.py` | Rule-derive contract type, payment channel, Basel attributes | **Requires owner approval** |
| `scripts/prepare_vendormaster_international.py` | Assign FX currency, CFDA codes | **Requires owner approval (A5-1, A5-3)** |
| `scripts/build_acfr_adjustment_fixture.py` | Parse ACFR extracted values → adjustment SJE fixture CSV | **Requires owner approval (B-4)** |

The "could Scala do this?" test: the VendorMaster preparation scripts derive attributes from
static rules applied to reference tables. This is configuration generation, not financial
processing. It could be done in Bash/awk for simple rules but Python is more readable for the
multi-rule Basel mapping. The Gold LDGR spend-tier computation requires sorting and percentile
calculation — these are the kind of post-engine summary operations AGENTS.md explicitly permits
in Python. The ACFR script reads a small hand-extracted fixture CSV and generates another small
CSV; it performs no accounting.

---

## 7. Sequence and dependencies

```
Phase 2 start gate: owner reviews this plan and approves sub-tracks

Week 1 — Foundation
  [A1] Round 1 VendorMaster fill (Python, requires A1-1)
  [B0] Download ACFR FY2012 sections G, K, L, O (curl, requires B-4)
  [B1] Hand-extract named adjustment amounts → fixture CSV

Week 2 — Attributes + first engine
  [A2] Round 2 size tier (Python, requires A2-2)
  [A3] Round 3 contract/payment types
  [C1] Allocation full-corpus run (Scala, architecture gate required)
  [C2] Budget variance ViewSpec + FY2012 run

Week 3 — International + Basel
  [A4] Round 4 Basel bridge
  [A5a] FX currency assignment (AUD for HALLPRINT)
  [A5b] CFDA federal-agency enrichment
  [C3] Multi-currency revaluation (Scala, architecture gate, gated on A5a)

Week 4 — Tie-out + AI features
  [B2] Construct adjustment SJEs for General Fund FY2012
  [B3] Run tie-out check: vendor total + adjustments = ACFR
  [M1] AIM3 enrichment comparison (gated on A2 complete, owner M-1)

Week 5+ — Engines and Q2-draft
  [C4] IUE accrual engine (Scala, architecture gate)
  [C5] Consolidation instrument trace
  [C6] Deferred: reclass creation/extension only as a response after Q2 is finalized
  [Q2-draft] Commit and tag Phase 2 question set
```

---

## 8. Open owner decisions summary

| ID | Decision | Blocking what |
|---|---|---|
| A1-1 | Confirm `GOV-RECEIPT` / `GOV` sentinels | Round 1 |
| A2-1 | Accept spend-percentile or use SBA DSBS | Round 2 |
| A2-2 | Approve `prepare_vendormaster_round2.py` | Round 2 |
| A3-1 | Accept payment-channel amount thresholds | Round 3 |
| A4-1 | Approve NIGP → counterparty sector mapping | Round 4 |
| A5-1 | Accept AUD for HALLPRINT, rule for others | FX enrichment |
| A5-2 | Provide or approve AUD/USD rate table source | Multi-currency engine |
| A5-3 | Accept CFDA enrichment for REV source-84 | Federal counterpart identity |
| B-1 | Confirm General Fund FY2012 as first tie-out target | Strand B |
| B-2 | Confirm access to ACFR sections K and O | Adjustment amounts |
| B-3 | Confirm payroll pseudo-vendor identity (`0000000000` vs. separate code) | Large-group reporting |
| B-4 | Approve ACFR PDF download (`curl doa.virginia.gov`) | Strand B |
| C4-1 | Accept 4.5% synthetic rate for capital-lease instruments | IUE accrual |
| M-1 | Authorize AIM3 enrichment comparison | AI feature strand |

---

## 9. What this plan does not include

- Any change to Phase 1 measured outputs. The three validated corpus runs (`companion-measured-baseline-20261111`)
  are unchanged. Phase 2 builds on top of them.
- Any Spark, Derby, Vagrant, Play, or PostgreSQL components (measurement integrity rules).
- Any financial logic in Python. Engine work stays in Scala.
- Any control-side disclosure. The control receives only the Q2-final question set and evaluation
  criteria, not this plan.
- Any commitment to a specific Phase 2 duration. The sequence above is an ordering, not a schedule.
- Any claim that the synthetic attributes make the VA data equivalent to real banking data for
  risk purposes. The synthetic Basel attributes demonstrate the architectural capability; they are
  explicitly labeled as rule-derived assignments.


---

## 10. Strand D — Subledger Detail Generation: New Experiments

The existing corpus has four source families: `apa_expenditure` (97.5M rows), `apa_revenue`
(17.75M rows), `apa_budget` (117K rows), and `eva_po` (18M PO lines). These are payment-level
records — they tell you *what was paid* and *to whom*. They do not tell you *how employee time
was spent*, *what assets are held*, or *what contractual obligations are open*. Each of those
missing subledgers is a standard accounting population that real institutions maintain and that
the instrument-ledger architecture is designed to unify.

This section designs three additional subledger populations that can be generated from public
data anchors and rule-based derivations, producing experiments that are interesting in their
own right and that connect directly to the AI signal and engine-demonstration strands above.

### 10.1 Employee Time Allocation Subledger

#### Why it is interesting

Object code 11 (PERSONAL SERVICES) in the VA expenditure data covers salaries, wages, employee
benefits, and retirement contributions. These account for the largest single share of state
government expenditure — typically 40–60% in education and public safety agencies. But the
payment file records payroll as a lump sum per agency per period, with vendor = `0000000000`
(the unattributed pseudo-vendor that failed the reporting guard). No employee identity and no
activity allocation exists in the public data.

**The subledger opportunity:** Virginia's state government had approximately 147,000–179,000
employees over FY2003–FY2016 (BLS SAE series `SMS51000009091000001`, confirmed live above —
monthly figures, thousands of workers). The object codes distinguish SALARIES (1120), WAGES
(1140), EMPLOYEE BENEFITS (1110), DISABILITY (1150), SPECIAL PAYMENTS (1130), and ten other
sub-categories. The ACFR publishes total state personal services expenditure by function.

A generated employee-time subledger would allocate the observed agency personal-services
expenditure to synthetic employees by program function, using:

1. **BLS total employment** as the annual headcount anchor per year (confirmed accessible,
   no authentication required, monthly resolution FY2003–FY2016).
2. **Object-code breakdown** from the corpus as the allocation basis: salaries vs. wages vs.
   benefits maintain their observed proportions.
3. **Program function** from `aRefProgramCSV.txt` (9 functions: Education, Transportation,
   General Government, etc.) as the activity dimension: each employee-instrument record
   carries a primary program assignment.
4. **Agency** as the legal-entity dimension: BLS employment is at the state level; the corpus
   gives agency-level payroll totals, so the allocation is agency → employee pool → program.

**What this enables:**

| Question | GL answer | Subledger answer |
|---|---|---|
| How much of Education agency X's payroll goes to instruction vs. administration? | Cannot determine — one lump per period | Activity allocation by program function |
| Which program functions grew headcount fastest FY2003–FY2010? | Not answerable | Synthetic headcount × salary rate by function |
| Do agencies with higher per-employee costs have higher vendor spend? | Not answerable | Join employee subledger to vendor subledger by agency |
| Budget vs. actual per-employee cost by program? | GL budget line only | Employee instrument × budget LDGR join |

The last row is especially powerful: linking the budget-variance demonstration (C-2 above) to
the employee population reveals *whether variance is headcount-driven or rate-driven* — the most
important question in a payroll audit.

#### Generation design

Each synthetic employee is an instrument with:

| Field | Value |
|---|---|
| `instID` | `EMP-{agency}-{seq}` (synthetic, clearly labeled) |
| `instTypeID` | `EXP` |
| `instHolderName` | `SYNTHETIC EMPLOYEE {seq}` |
| `instNIGPClass` | Sentinel `EMP` |
| `instGeoRegion` | `SE` (all VA state employees) |
| `instCounterpartySector` | `INDIVIDUAL` |
| `instExposureType` | `OPERATING-EXP` |
| New: `instProgramFunction` | 1–9 from `aRefProgramCSV.txt` (assigned by agency) |
| New: `instJobClass` | SALARY / WAGE / BENEFIT (from object-code distribution) |
| New: `instAgencyKey` | Numeric agency code from `aRefAgencyCSV.txt` |

The SJE records for each employee are generated by distributing the observed agency payroll
total across the synthetic employee population proportionally. Each employee SJE is:
- Debit: `EXP11xx` nominal account (personal services sub-category)
- Credit: `0000` offset (same as all other expenditure transactions)
- Amount: `agency_payroll_period / employee_count_in_agency_period`

The result is a population of instrument-keyed payroll SJEs that balance exactly to the
observed corpus totals — the total is not invented, only the instrument-level attribution.

**Provenance labeling:** Every generated employee record carries `ldgrJrnlType = SYN-PAYROLL`
to distinguish it from real vendor transactions. The generation script, rules, and BLS source
are recorded in a `SYNTHETIC_PAYROLL_MANIFEST.md`.

#### Public data anchor: BLS SAE

Confirmed live:
```
Series: SMS51000009091000001 (VA state government total employment, thousands)
API:    https://api.bls.gov/publicAPI/v2/timeseries/data/SMS51000009091000001
FY2003: ~147K workers/month
FY2016: ~178K workers/month
Resolution: monthly, no API key required for single-series queries
```

This gives the total headcount denominator. Agency-level headcount is not in the BLS data —
the allocation from state total to agency uses the agency payroll share from the corpus
(agency personal-services expenditure / state total personal-services expenditure per year).

**Owner decision D1-1:** Approve this subledger as a Phase 2 experiment.
**Owner decision D1-2:** Approve the Python generation script (post-engine, no financial logic,
generates instrument records from observed corpus totals and BLS headcount).
**Owner decision D1-3:** Confirm whether `0000000000` is exclusively payroll or also covers
other unattributed vendors. If mixed, the split is needed before the headcount allocation.

---

### 10.2 Open Purchase Order / Encumbrance Subledger

#### Why it is interesting

The `eva_po` family (18M rows, FY2001–FY2018) records purchase orders: a commitment to spend
before the payment clears. The existing pipeline processes PO data through `COMMIT` nominal
accounts — these are encumbrances, not expenditures. But the current Gold output excludes PO
commitments from the revenue analysis and treats them as a separate family.

**The subledger opportunity:** Each PO line in the corpus has: vendor, agency, NIGP commodity,
ordered date, and quantity × price. What is *missing* is the fulfilment status — was the PO
fully paid, partially paid, cancelled, or still open at year-end? In a real AP system this is
the open-commitment subledger: orders raised but not yet paid.

**Generation design:** For each PO line, derive a fulfilment status by joining the PO vendor
to the expenditure payment records for the same agency × period. If a matching payment exists
within 90 days (fiscal year close heuristic), mark `FULFILLED`; if not, mark `OPEN` or
`LAPSED`. This creates:

| `instCommitmentStatus` | Meaning |
|---|---|
| `FULFILLED` | PO matched to a payment in the same fiscal year |
| `OPEN` | PO raised, no matching payment found within fiscal year |
| `LAPSED` | PO raised in prior year, never matched |

**What this enables:**

| Question | Without subledger | With subledger |
|---|---|---|
| What is the year-end open commitment balance by agency? | Not answerable from payments alone | Sum of OPEN PO amounts per agency |
| Which vendors have high lapse rates? | Not answerable | lapsed / total PO count per vendor |
| Do vendors with high lapse rates also have low survival rates (AIM3)? | Cannot connect | Join commitment subledger to AIM3 survival labels |
| Budget minus encumbrances minus expenditures = unencumbered balance | Partial (budget and expenditure) | Complete (add open commitments) |

The last row is the full appropriation control: the ACFR publishes unencumbered balances as
a separate line. With the open-commitment subledger, the CKB can replicate that number from
first principles — the instrument dimension is preserved throughout.

**New AI signal:** Commitment lapse rate is an instrument-level feature unavailable from the GL.
A vendor that consistently raises large POs that lapse may be a fraud indicator or a
procurement-process failure indicator — neither is visible from the payment file alone.

**Owner decision D2-1:** Approve the PO fulfilment heuristic (90-day matching window, same
agency, vendor-to-vendor join via `instHolderName` normalization).
**Owner decision D2-2:** Approve the Python matching script.

---

### 10.3 Fixed Asset / Capital Expenditure Subledger

#### Why it is interesting

Object code 22 (EQUIPMENT) and object codes 21/23 (PROPERTY IMPROVEMENTS, PLANT AND
IMPROVEMENTS) in the corpus identify capital expenditures — purchases that create a durable
asset rather than a period expense. In a full accounting system these flow into an asset
register, not just the GL expenditure account. The instrument-ledger architecture claims to
support this naturally: each asset is an instrument, its cost basis and depreciation are
balance attributes, and the asset register is just the LDGR filtered to asset instruments.

The existing corpus has object 22 (EQUIPMENT) rows for every agency across 14 fiscal years.
Notably, object 2211 (DESKTOP CLIENT COMPUTERS) and 2212 (MOBILE CLIENT COMPUTERS) are
separately coded — a natural candidate for a technology asset subledger.

**Generation design:**

Each capital expenditure in the corpus that exceeds a threshold (e.g., $5,000 — Virginia's
capitalization threshold for equipment) becomes an asset instrument:

| Field | Value |
|---|---|
| `instID` | `ASSET-{agency}-{obj}-{seq}` |
| `instTypeID` | `EXP` |
| New: `instAssetClass` | COMPUTER / VEHICLE / EQUIPMENT / PROPERTY (from object code) |
| New: `instAcquisitionDate` | Voucher date from corpus (available from FY2007 onward) |
| New: `instUsefulLifeYears` | Rule-based: computers=3, vehicles=5, equipment=7, property=20 |
| New: `instDepreciationMethod` | `STRAIGHT-LINE` (uniform for demonstration) |

Depreciation is then an IUE-class generated journal (Strand C, C-4): each period the asset
generates a DEPR_SJE debiting DEPRECIATION_EXP and crediting ACCUMULATED_DEPR, both keyed to
`instID`. The LDGR balance for each asset shows its net book value: cost minus accumulated
depreciation.

**What this enables:**

| Question | GL answer | Asset subledger answer |
|---|---|---|
| What is the net book value of computers by agency at FY2012? | Not answerable | LDGR balance filtered to `instAssetClass = COMPUTER` |
| Which agencies are most over-depreciated (old, fully written-off equipment)? | Not answerable | Assets where NBV = 0 but still on balance |
| Technology replacement cycle: when was the last desktop refresh? | Not answerable | Date of most recent COMPUTER acquisition per agency |
| Total capital cost vs. operating cost ratio by program function? | Partial | Complete with asset subledger instrument join |

**CCP4 measurement opportunity:** The depreciation engine is a clean CCP4-class addition.
Every asset instrument generates exactly one DEPR_SJE per period — no new sort required
if assets are sorted by `instID` within the existing order. The incremental pass cost
(additional SJE rows / bytes / elapsed time) is directly measurable. This gives CCP4 a
second engine alongside FX revaluation, with real data backing rather than a purely
synthetic rate table.

**Owner decision D3-1:** Accept $5,000 as the capitalization threshold.
**Owner decision D3-2:** Accept the useful-life rules (computers=3, vehicles=5, etc.) or
supply the Virginia DGS published schedules.
**Owner decision D3-3:** Approve the Python asset-extraction script.

---

### 10.4 Interagency Services / Shared Services Subledger

#### Why it is interesting

Object codes 1296 (INTER-AGENCY RECOVERIES FOR CONTRACTUAL SERVICES) and 1196 (INDIRECT COST
RCVRY AUX PGM FOR PERSONAL SERVICES) already exist in the reference data. VITA (Virginia
Information Technologies Agency, object 1272 VITA PASS THRU CHARGES) is a named shared
services provider appearing across multiple agencies. These are real interagency transfers —
agency A pays VITA for IT services; VITA records it as revenue.

**Current limitation:** The `InteragencyTransfers.csv` fixture is empty except for the header.
No real interagency pairs have been declared.

**Generation design:** From the corpus, identify agency pairs where:
1. Agency A has an EXP object in the range 12xx (CONTRACTUAL SERVICES) with a vendor whose
   `instNIGPClass` matches IT/communications services (class 920, 918, etc.);
2. Agency B has a REV record in the same period from the same vendor at approximately the
   same amount.

This is not invented data — it is a reconciliation of existing corpus records. The generation
step identifies and surfaces these latent interagency relationships. Populating
`InteragencyTransfers.csv` with the discovered pairs enables:

- The consolidation demonstration (Strand C, C-5): real elimination entries, not fixtures.
- The ACFR tie-out (Strand B): VITA charges appear as both expenditure (payer agencies) and
  revenue (VITA) — the elimination is required to avoid double-counting in the consolidated
  statement, exactly as the ACFR shows.
- A new AI feature: `instIsInteragencyProvider` (Y/N) — do agencies that provide shared
  services have different expenditure survival patterns than pure recipients?

**Owner decision D4-1:** Approve the interagency pair discovery from corpus data (Python
script, read-only analysis of LDGR outputs, no new accounting logic).

---

### 10.5 Summary: What the Four Subledgers Unlock Together

| Subledger | Anchor data | New instruments | Key new question |
|---|---|---|---|
| Employee time allocation | BLS SAE monthly headcount + corpus payroll totals | ~147K–179K synthetic employee instruments per year | Is variance headcount-driven or rate-driven? |
| Open PO / encumbrance | Corpus eva_po + apa_expenditure (matching) | ~18M PO lines reclassified to OPEN/FULFILLED/LAPSED | What is the year-end unencumbered appropriation balance? |
| Fixed asset / depreciation | Corpus apa_expenditure OBJ 21/22/23 | Capital purchases above threshold → asset instruments | What is net book value by agency? |
| Interagency services | Corpus apa_expenditure + apa_revenue (VITA matching) | Named interagency pairs → elimination entries | Which VITA-class providers appear in both sides? |

Each is independently valuable. Together they produce a financial foundation that covers the
five standard subledger types (AP/payments ✓ already, AR/revenue ✓ already, Payroll ← D1,
Fixed Assets ← D3, Open Commitments ← D2) plus the interagency consolidation dimension (← D4).

A system that can answer questions across all five subledger types from a single instrument-keyed
balance store — without maintaining five separate reconciliation processes — is the empirical
content of the architecture claim. The VA corpus, with these generated subledgers, is sufficient
to demonstrate it.

**AI signal amplification:** Each subledger adds features that are invisible from the GL:
employee-to-spend ratios, commitment lapse rates, asset age profiles, and interagency dependency
networks. An AIM3-class survival model trained on the enriched LDGR (vendor + employee +
asset dimensions joined at the instrument level) vs. the same model trained on GL-only data
produces the most compelling Paper 2 evidence yet: the instrument ledger does not just preserve
detail — it *creates* the features that make financial AI meaningful.

---

## 11. Updated open decisions (Strand D additions)

| ID | Decision | Blocking what |
|---|---|---|
| D1-1 | Approve employee time-allocation subledger as Phase 2 experiment | Subledger D1 |
| D1-2 | Approve `generate_employee_subledger.py` | Subledger D1 |
| D1-3 | Confirm `0000000000` scope (payroll-only or mixed) | Subledger D1 |
| D2-1 | Approve 90-day PO fulfilment matching heuristic | Subledger D2 |
| D2-2 | Approve `classify_po_fulfilment.py` | Subledger D2 |
| D3-1 | Accept $5,000 capitalization threshold | Subledger D3 |
| D3-2 | Accept useful-life rules or supply DGS schedules | Subledger D3 |
| D3-3 | Approve `extract_capital_assets.py` | Subledger D3 |
| D4-1 | Approve interagency pair discovery script | Subledger D4 |

---

## 12. Strand E — Revenue and Tax Subledgers

The `apa_revenue` family already flows through the Scala TTL (`buildRevenuePairs`,
`ApaTtlSpec` Test 6), but no real-corpus run has included it in Gold, and the 232 REV
instruments in `VendorMaster.csv` carry no source-class identity — their `instHolderName`
is a bare fund-code number with no link to `aRefSourceCSV.txt`. This section grounds
revenue work in the actual source structure and identifies three public data anchors that
make the revenue side as measurable as the expenditure side.

### 12.1 What the revenue data contains

**Source classes in `aRefSourceCSV.txt`** (confirmed from the reference file):

| Class | Name | Source codes |
|---|---|---|
| 01 | TAXES | 114 (income, sales, motor fuels, excise, etc.) |
| 02 | RIGHTS AND PRIVILEGES | 421 (licenses, permits, fees) |
| 03 | SALES OF PROPERTY AND COMMODITIES | 57 |
| 04 | DONATIONS, RECEIPTS AND TRANSFERS | 24 |
| 05 | ASSESSMENTS AND RECEIPTS FOR SPECIAL SERVICES | 89 |
| 06 | INSTITUTIONAL REVENUE | 94 (higher education tuition, hospital fees) |
| 07 | INTEREST, DIVIDEND AND RENTS | 38 |
| 08 | FINES, FORFEITURES, COURT FEES, PENALTIES | 107 |
| 09 | OTHER REVENUE | 59 |
| 10–99 | FEDERAL GRANTS AND CONTRACTS | ~1,100+ CFDA-mapped sources |

The revenue corpus covers FY2003–FY2016 at **884K–1.7M rows per year**, totaling **$26–47B
per year** — growing from $26B in FY2003 to $47B in FY2016. This is the full revenue side
of the state: taxes, federal grants, fees, fines, and interest.

**Current VendorMaster gap:** All 232 REV instruments have blank `instNIGPClass` and
`instGeoRegion` and no source-class linkage. Their `instHolderName` is a fund code (e.g.
`"101"` = fund detail 0101), not a source code. The revenue instruments are classified by
the fund that receives the revenue, not by the revenue source type. This is the right
accounting structure — but it means the source-class enrichment (tax vs. federal grant vs.
fee) requires joining through the `apa_revenue` rows themselves, not just through
`VendorMaster`.

### 12.2 Revenue instrument enrichment strategy

The correct enrichment for REV instruments is:

| New field | Derivation |
|---|---|
| `instFundCode` | The fund-code value from `instHolderName` (already present, rename) |
| `instFundName` | Join `instHolderName` → `aRefFundCSV.txt` FND_FUND_CODE → FND_FUND_NAME |
| `instRevSourceClass` | Modal source-class code from actual `apa_revenue` rows with matching Fund Detail |
| `instRevSourceClassName` | The class name (TAXES, FEDERAL GRANTS, etc.) |
| `instFederalShare` | Fraction of this fund's revenue rows from class 10–99 (federal) |

**Why this matters:** The ACFR fund statements are organized by fund (General Fund, Federal
Trust Fund, Transportation Fund). Linking the REV instruments to their actual fund identity
closes the gap between the corpus revenue rows and the ACFR published figures — the same
connection that Strand B makes on the expenditure side.

---

### 12.3 Subledger E1 — Tax Revenue Disaggregation

#### Why it is interesting

Source class 01 (TAXES) includes 114 distinct tax programs: individual income tax (the
largest single revenue source for Virginia), sales and use tax, motor fuels taxes, corporate
income tax, recordation taxes, and 109 others. The corpus already records each tax receipt as
a separate transaction row keyed to its source code. What is currently missing is the
**taxpayer dimension** — not individual taxpayer identity (not public) but the
**economic sector** and **locality** that generated the tax.

**Two public anchors resolve this at the aggregate level:**

**Anchor E1-a — VA Dept of Taxation annual reports (confirmed downloadable):**
```
URL pattern: https://www.tax.virginia.gov/sites/default/files/inline-files/{year}-annual-report.xlsx
Available:   FY2021–FY2025 as XLSX; FY2003–FY2020 available as PDF at the same domain
Content:     Total collections by tax type, year over year; individual income, corporate,
             sales, motor fuels, recordation, estate, withholding breakdown
```

These reports provide total collections per tax type per year. The corpus source-code
totals should agree with these published figures at the tax-type level — that agreement
is the tie-out for the revenue side of Strand B.

**Anchor E1-b — VA Dept of Taxation taxable sales data (confirmed on data.virginia.gov):**
```
Available:   XLS per year, FY2003–FY2016+ (all years in our corpus range)
Content:     Taxable sales by locality (county/city), by quarter
             "all counties and cities in the Commonwealth"
```

This gives sales-tax collections at the locality level — 133 Virginia localities. Combined
with the corpus sales-tax source codes (01083 GENERAL STATE SALES AND USE TAX, etc.), this
creates a **locality × tax-type** revenue instrument. Each locality becomes an instrument
in the Revenue Subledger:

| New instrument type | `instID` | `instHolderName` |
|---|---|---|
| Locality sales-tax payer | `TAXLOC-{fips}` | `{LOCALITY NAME}` |
| `instCounterpartySector` | `INDIVIDUAL` or `CORPORATE` (rule: residential vs. commercial locality) |
| `instGeoRegion` | Derived from locality FIPS code |
| New: `instLocalityFIPS` | VA locality FIPS code |
| New: `instTaxType` | INCOME / SALES / MOTOR-FUELS / RECORDATION / OTHER |

**New questions this enables:**

| Question | Without subledger | With subledger |
|---|---|---|
| Which localities drive the most sales tax revenue? | Not answerable — one statewide source code | Locality-level sales tax LDGR balance |
| Did Northern Virginia localities hold up better during the 2008–2009 recession? | Not answerable | Year-over-year locality balance change |
| Does vendor spend in a locality correlate with tax revenue from that locality? | Cannot connect | Join locality tax LDGR to vendor expenditure LDGR by region |
| Which tax types are most volatile year-to-year? | ACFR total only | Instrument-level time-series variance per tax type |

The last question is a natural AIM-class test: can a model trained on the instrument-level
tax revenue history predict which tax types will be below budget in the next year? The GL
shows only the aggregate; the instrument ledger exposes the composition.

---

### 12.4 Subledger E2 — Federal Grant Receipt Tracking

#### Why it is interesting

The Federal Trust Fund receives $6–9B per year from ~1,100 federal grant programs spanning
Department of Education (CFDA prefix 84), HHS (93), Transportation (20), HUD (14), and
others. The corpus has these as revenue rows keyed to 5-digit CFDA source codes. What is
missing is the **federal program lifecycle** dimension: was a grant received on-schedule,
delayed, or partial? Did the amount grow or shrink year-over-year (a signal of federal
policy change)?

**Public anchor: USAspending.gov API (confirmed probed in the 2026-11-12 survey):**
```
API:     https://api.usaspending.gov/api/v2/
Content: Federal awards to Virginia state agencies by CFDA program, fiscal year
         Includes obligation amounts, award dates, program names
Example: Dept of Medical Assistance Services ~$3.9B; State Board of Education ~$0.94B
```

Each CFDA program becomes a REV instrument with:

| New field | Value |
|---|---|
| `instFederalCFDA` | The 5-digit CFDA code (e.g., `93.778` = Medicaid) |
| `instFederalAgency` | Federal grantor agency (HHS, DOE, DOT, etc.) from CFDA prefix |
| `instGrantCategory` | FORMULA / COMPETITIVE / ENTITLEMENT (from CFDA database) |
| New: `instFederalObligationFY` | USAspending reported obligation for that program-year |
| New: `instCKBReceiptFY` | Actual receipt recorded in corpus apa_revenue |
| New: `instGrantVariance` | `instCKBReceiptFY - instFederalObligationFY` |

**The grant variance instrument is especially powerful:** It captures the difference between
what the federal government obligated and what Virginia actually received in its books that
year. This lag/shortfall is an institutional phenomenon — federal grants often obligate in
one fiscal year and drawdown across multiple years. The instrument ledger makes this
time-pattern visible across all 1,100+ programs; a GL entry records only the receipt.

**AI signal:** Grant receipt patterns are highly predictive of program continuation. A
federal program that reduces its obligation by >20% for two consecutive years is at risk of
elimination — that signal appears in the instrument-level history years before the grant
closes. No GL model can see this because the GL aggregates all federal receipts into one
fund line.

---

### 12.5 Subledger E3 — Fee and License Revenue by Counterparty Type

#### Why it is interesting

Source class 02 (RIGHTS AND PRIVILEGES) has 421 source codes covering professional licenses,
vehicle registrations, hunting/fishing licenses, corporate registrations, and environmental
permits. Source class 08 (FINES, FORFEITURES, COURT FEES) has 107 codes. Together these are
~$2–3B per year and represent fees paid by identifiable counterparty types (professionals,
businesses, individuals, vehicles).

The source codes already encode the counterparty type implicitly:
- `02303`: DOMESTIC & FOREIGN CORP REGISTRATION FEES → counterparty = CORPORATE
- `02405`: ACCOUNTANCY REGULATORY FEES → counterparty = PROFESSIONAL INDIVIDUAL
- `02721`: WASTE VESSEL RECEIVING FACILITY PERMIT FEE → counterparty = ENVIRONMENTAL

**Enrichment design:** Each class-02 and class-08 source code becomes an instrument with
`instCounterpartySector` derived from its name (rule-based classification covering
CORPORATE, PROFESSIONAL, INDIVIDUAL, VEHICLE, ENVIRONMENTAL). The SAL then enables:

| Question | GL/ACFR answer | Instrument-level answer |
|---|---|---|
| How much does Virginia collect from professional licensing vs. corporate fees? | ACFR "Rights and Privileges" subtotal only | Source-code breakdown by counterparty type |
| Are environmental permit fees growing as industrial activity increases? | Not disaggregated | Year-over-year per-permit-class trend |
| Do counties with more vendor activity also generate more business license fees? | Cannot connect | Join fee LDGR to expenditure LDGR by region |

---

### 12.6 Revenue tie-out to ACFR (extends Strand B)

The expenditure tie-out in Strand B targets General Fund FY2012 expenditures. The revenue
side has a parallel structure: ACFR section G reports General Fund total revenues of
**$17,342,130K** in FY2012, broken down into:

| Revenue category | ACFR FY2012 ($K) | Corpus source class |
|---|---|---|
| Taxes | ~$11.5B (est.) | Class 01 |
| Rights and Privileges | ~$0.5B (est.) | Class 02 |
| Federal Grants and Contracts | ~$9.9B (governmental funds) | Class 10–99 |
| Interest, Dividends, Rents | (included in total) | Class 07 |
| Other | (included in total) | Classes 03–09 |

**The revenue tie-out strategy:**

1. Sum corpus `apa_revenue` rows by source class for General Fund FY2012.
2. Compare each class subtotal to the corresponding ACFR line.
3. Identify named gap categories: timing differences (modified accrual vs. cash),
   accrued taxes not yet received, deferred revenue.
4. Construct named adjustment SJE records for the revenue side, exactly as Strand B
   does for expenditures.

This completes the full financial-statement reconciliation: **vendor payments + payroll
adjustments + named expenditure adjustments = ACFR expenditure total** AND **source-code
receipts + timing adjustments = ACFR revenue total**. The fund balance is then:
`revenues − expenditures = net change`, which must agree with the ACFR published fund
balance change of **$571,136K** for the General Fund in FY2012.

**This is the most rigorous possible demonstration of the architecture's claim:** starting
from millions of individual payment and receipt transactions, the instrument-keyed ledger
reconstructs the published audited financial statement with named entries accounting for
every dollar of difference — with no plugs and no unexplained residuals. A fragmented
ledger system cannot do this without a separate reconciliation process for each of the five
subledger types.

---

### 12.7 New ViewSpec rows enabled by revenue enrichment

Each enrichment round adds ViewSpec rows with no Scala code changes:

| View name | Group-by dimensions | New question answered |
|---|---|---|
| `BY_TAX_TYPE` | `ldgrNominalAccountID` × `instTaxType` × period | Tax-type composition of General Fund revenue |
| `BY_LOCALITY_SALES` | `instLocalityFIPS` × period | Locality sales-tax time series |
| `BY_FEDERAL_AGENCY` | `instFederalAgency` × period | Federal grantor agency share by year |
| `BY_GRANT_CATEGORY` | `instGrantCategory` × `instFederalAgency` | Formula vs. competitive grant balance |
| `BY_FEE_CLASS` | `instCounterpartySector` × `ldgrNominalAccountID` | Professional vs. corporate fee revenue |
| `REV_EXP_AGENCY_PIVOT` | Agency × period (both REV and EXP instruments) | Net position per agency — revenue minus expenditure |

The last row is the most important: joining revenue and expenditure instruments on agency ×
period produces an agency-level net position that the GL publishes only as a fund total.
The instrument ledger produces it at the agency grain — a new dimension of accountability.

---

### 12.8 Owner decisions for Strand E

| ID | Decision | Blocking what |
|---|---|---|
| E1-1 | Approve XLSX download from `tax.virginia.gov/annual-reports` for FY2003–FY2020 | Tax tie-out anchor |
| E1-2 | Approve taxable-sales XLS download from `data.virginia.gov` (confirmed accessible) | Locality sales-tax subledger |
| E1-3 | Approve `prepare_revenue_instruments.py` (links REV instruments to source classes) | All Strand E |
| E2-1 | Approve USAspending.gov API calls for VA federal awards FY2003–FY2016 | Grant receipt tracking |
| E2-2 | Accept `instGrantVariance` as a declared synthetic field (derived, labeled) | Grant variance instrument |
| E3-1 | Approve class-02/08 counterparty-type classification rule | Fee/license subledger |
| EB-1 | Confirm General Fund FY2012 as the revenue tie-out target (same as Strand B) | Full statement reconciliation |
| EB-2 | Confirm modified accrual vs. cash basis interpretation for the revenue comparison | Named revenue adjustments |


---

## 13. Strand F — Multi-Currency and Virtual Asset Investment Subledger

### 13.1 The real investment structure of Virginia (what grounds the simulation)

Virginia's investment landscape across FY2003–FY2016 divides into three tiers with very
different currency exposure:

| Pool | Manager | FY2012 estimated AUM | Currency exposure | In corpus? |
|---|---|---|---|---|
| **SNAP** (State Non-Arbitrage Pool) | Virginia Treasury | ~$6–8B operating cash | USD-only: Treasuries, agency securities, commercial paper | Source 07101 `INTEREST ON STATE MONEY` → YES |
| **VRS** (Virginia Retirement System) | VRS Board | ~$57B pension assets | 20–30% international equities (EUR, GBP, JPY, emerging markets); documented in VRS CAFRs | Source 07114 `INTEREST ON VRS INVESTMENTS` → YES |
| **VEST / CollegeWealth / VPEP** | Virginia 529 | ~$4–6B 529 assets | Domestic equity + bond funds; some international equity | Source 07134/07135/07118 → YES |

**Key finding from the source reference:** `INTEREST ON VRS INVESTMENTS` (07114) already
appears in `aRefSourceCSV.txt` and therefore in `apa_revenue`. VRS *demonstrably* holds
international assets — it is a $57B pension fund with ~25% international equity allocation
published in its CAFRs. The corpus already records the income flow from that international
exposure as a revenue line.

**Key finding from the ACFR K notes:** Note 18 is titled "State Non-Arbitrage Pool" (SNAP),
and there are separate sections for "Cash, Cash Equivalents, and Investments" (Note 6) and
"Investment Commitments — Virginia Retirement System" (Note C). These are real disclosures
of an institution that *already* manages multi-currency assets.

**What is missing:** The instrument-level accounting for *which* VRS portfolio positions
generated the FX gain/loss that produced source 07114's revenue. That gap is precisely what
the multi-currency revaluation engine is designed to close.

---

### 13.2 Why virtual currencies are the right forward-looking extension

The owner's insight is well-timed. As of FY2024–FY2026, the following institutional
movements are documented:

- **State of Wisconsin Investment Board** approved Bitcoin allocation (2024)
- **State of Michigan** pension fund allocated to Bitcoin ETFs (2024)
- **El Salvador** (national level) holds Bitcoin as legal reserve
- **Multiple US city treasuries** (Miami, NYC) have proposed/piloted crypto treasury
  allocations
- **Federal Bitcoin reserve** was proposed in legislation (2024–2025)

The pattern is clear: virtual currencies are entering **exactly the same institutional
investment pools** where VRS and SNAP operate. A state treasury that today holds
USD Treasuries and international equities will face pressure to hold BTC or ETH alongside
them — and the accounting systems that handle FX revaluation *are the same systems* that
must handle crypto revaluation.

The ILC/instrument-ledger architecture is uniquely positioned for this: because every
balance carries an `instFXCurrency` attribute, adding `BTC` or `ETH` as a currency code is
a data change, not a code change. The revaluation engine reads the rate table; it doesn't
know or care whether the rate table has AUD/USD or BTC/USD entries.

---

### 13.3 Simulation design: plausible and grounded

The simulation works at three levels of increasing realism. The owner should choose the
level, since they determine what can be claimed in the papers.

#### Level 1 — VRS international equity revaluation (fully grounded, no invention)

VRS actually holds international equities. Source 07114 is in the corpus. The VRS CAFR
publishes annual returns by asset class including international equity, with currency
effects broken out in the notes.

**Simulation:** Create VRS investment instruments for each major currency allocation:

| `instID` | `instHolderName` | `instFXCurrency` | Basis |
|---|---|---|---|
| `VRS-INTL-EUR` | VRS INTERNATIONAL EQUITY — EUROPE | EUR | ~8–10% of VRS portfolio |
| `VRS-INTL-GBP` | VRS INTERNATIONAL EQUITY — UK | GBP | ~3–5% of VRS portfolio |
| `VRS-INTL-JPY` | VRS INTERNATIONAL EQUITY — JAPAN | JPY | ~3–5% of VRS portfolio |
| `VRS-INTL-EM` | VRS INTERNATIONAL EQUITY — EMERGING | basket (CNY/BRL/INR proxy) | ~5–8% of VRS portfolio |

Rates: published historical central bank rates (EUR/USD, GBP/USD, JPY/USD are freely
available from Federal Reserve H.10 release — confirmed API access via FRED).

**What the engine produces:** A REVAL_SJE per VRS instrument per period. The corpus
already has the aggregate revenue line (07114); the instrument-level revaluation produces
the *composition* of that line — which geographic allocation generated the gain/loss. This
is not invented; it is the instrument-level attribution of a real revenue stream.

**Owner decision F1-1:** Approve VRS international equity instruments as Level 1 simulation.
**Owner decision F1-2:** Approve FRED API access for historical EUR/USD, GBP/USD, JPY/USD
rates (Federal Reserve H.10, confirmed public API).

---

#### Level 2 — SNAP/Treasury short-term foreign currency notes (partly grounded)

Virginia's investment policy (Code of Virginia §2.2-4514) permits state entities to hold
obligations of foreign governments and supranational institutions denominated in USD —
but *not* foreign-currency-denominated instruments for operating cash. However, Virginia
state universities (which are state entities) have more flexible investment policies and
*do* hold foreign-currency-denominated instruments through their endowments.

For the simulation, Level 2 extends a small fraction of SNAP-adjacent cash to
currency-hedged short-term foreign government obligations (e.g., UK T-bills held by UVA
endowment, German Bunds held by VT endowment). These are plausible because:
1. Virginia universities are state entities and their investment income appears in the
   corpus through the higher-education source codes.
2. University endowments of this era routinely held GBP and EUR short-term instruments.
3. The instrument-level accounting is structurally identical to Level 1.

**Owner decision F2-1:** Accept or reject Level 2 (university endowment foreign notes).

---

#### Level 3 — Virtual asset pilot (declared simulation, forward-looking)

This level explicitly declares itself as a *forward-looking simulation* that does not
represent Virginia's actual portfolio at any historical point. It answers the question:
*"If Virginia had allocated 0.1% of SNAP to BTC in FY2014, what would the instrument-level
accounting have looked like?"*

**Why FY2014 is the right starting year:** Bitcoin's first major institutional
acknowledgment period was late FY2013–FY2014. Using FY2014 as the start date is
defensible and not anachronistic.

**Virtual asset instruments:**

| `instID` | `instHolderName` | `instFXCurrency` | Declared provenance |
|---|---|---|---|
| `SIM-SNAP-BTC` | SIMULATED SNAP BITCOIN ALLOCATION | BTC | SYNTHETIC — FORWARD-LOOKING SIMULATION |
| `SIM-SNAP-ETH` | SIMULATED SNAP ETHEREUM ALLOCATION | ETH | SYNTHETIC — FY2016+ only (ETH launched Jul 2015) |
| `SIM-VRS-DEFI` | SIMULATED VRS DIGITAL ASSET FUND | basket | SYNTHETIC — ILLUSTRATIVE ONLY |

Rate tables: Bitcoin historical prices are freely available (CoinGecko API, Coinbase
historical data, Federal Reserve Z.1 supplement) with daily resolution back to 2013.

**What this demonstrates that nothing else does:**

1. **Volatility accounting:** A BTC position that gains 300% in one year and drops 60%
   the next generates revaluation SJEs of a magnitude no traditional foreign-currency
   position produces. The instrument ledger handles this identically to EUR revaluation —
   because the architecture doesn't distinguish currency types. A fragmented system that
   treated crypto as "other assets" outside its revaluation engine would need a manual
   adjustment process for each period.

2. **Unrealized vs. realized distinction:** When the simulated BTC position is partially
   liquidated, the instrument ledger generates both an `UNREALIZED_REVAL` reversal and a
   `REALIZED_GAIN` SJE, both keyed to `SIM-SNAP-BTC`. The audit trail is complete at the
   instrument level. In a legacy system, the realized gain would be a manual journal entry
   disconnected from the unrealized revaluation history.

3. **Multi-asset-class co-movement:** The instrument ledger shows, in one balance store:
   the VRS EUR equity position (gaining in USD terms as EUR strengthens) alongside the
   BTC position (wildly volatile) alongside the SNAP USD Treasuries (stable). A CFO
   can ask: "What is my total FX and digital-asset exposure as of Q2 FY2015?" The
   instrument ledger answers with one ViewSpec query. No legacy system answers this
   without assembling data from three separate subledgers.

**Provenance labeling:** Every Level 3 record carries:
- `ldgrJrnlType = SIM-VIRTUAL-ASSET`
- `instHolderName` explicitly includes `SIMULATED`
- A `VIRTUAL_ASSET_SIMULATION_MANIFEST.md` records the declaration, the rate sources,
  the starting allocation assumption, and the date range.

**Owner decision F3-1:** Accept or reject the Level 3 virtual asset simulation.
**Owner decision F3-2:** If accepted — confirm the 0.1% SNAP allocation assumption
(approximately $6–8M at FY2014 SNAP size), or supply a different fraction.
**Owner decision F3-3:** Confirm ETH inclusion (FY2016+ only, since ETH launched July 2015).

---

### 13.4 The investment income tie-out (connects to Strand B and E)

Investment income is already in the corpus as class-07 revenue. The full revenue tie-out
in Strand B/E now has a fourth component:

```
Corpus class-01 tax receipts
+ Corpus class-10..99 federal grants
+ Corpus class-02/08 fees and fines
+ Corpus class-07 investment income (interest, gains, dividends)
  = broken down by: SNAP interest (07101), VRS income (07114),
    FX revaluation gains (new: instrument-level), virtual asset gains (new: simulated)
+ Named timing adjustments
= ACFR total revenues
```

The VRS income line (07114) is currently a lump sum in the corpus. With Level 1
instruments, it becomes decomposable: how much came from international equity (EUR, GBP,
JPY), how much from domestic equity, how much from fixed income. The ACFR Note C
("Investment Commitments — Virginia Retirement System") provides the published total that
the instrument-level sum must agree with.

---

### 13.5 Rate table design

One rate table covers all currency levels:

```
data/FX_RATES.csv
  date, currency_pair, rate, source, source_date
  2003-07-01, EUR/USD, 1.1310, FRED H.10, 2026-11-14
  2003-07-01, GBP/USD, 1.6050, FRED H.10, 2026-11-14
  2003-07-01, JPY/USD, 0.00844, FRED H.10, 2026-11-14
  2014-01-01, BTC/USD, 820.00, CoinGecko, 2026-11-14   [Level 3 only]
  2015-07-30, ETH/USD, 2.80, CoinGecko, 2026-11-14     [Level 3 only]
```

The `currencyRevaluation.scala` engine reads this table. Adding BTC/USD and ETH/USD rows
requires zero code changes — the engine processes any `instFXCurrency` value that has a
corresponding rate-table entry. This is the demonstration: **virtual currencies integrate
into the existing revaluation architecture without engine modification**.

**Owner decision F4-1:** Approve FRED H.10 API access for EUR/USD, GBP/USD, JPY/USD
historical rates (annual or quarterly, FY2003–FY2016).
**Owner decision F4-2:** Approve CoinGecko or Coinbase historical price data download for
BTC/USD (FY2013–FY2016) and ETH/USD (FY2015–FY2016) if Level 3 is accepted.

---

### 13.6 AI signal from investment instruments

The investment instruments add a new class of AI signal completely absent from the
expenditure side:

| Feature | Source | GL-visible? | Instrument-level only? |
|---|---|---|---|
| FX gain/loss by currency pair | REVAL_SJE per instrument | No — aggregate only | Yes |
| VRS international equity return vs. domestic | Instrument balance delta | No | Yes |
| BTC unrealized vs. realized split | REVAL_SJE type flag | No | Yes |
| Investment income as % of total revenue | class-07 / total revenue | Yes (annual total) | Yes (by asset class and period) |
| Currency pair correlation with tax revenue | FX rate × tax receipt instrument | No | Yes — joinable in LDGR |

The last row is especially interesting for a Paper 2 AIM-class test: *does the EUR/USD rate
correlate with Virginia's federal grant receipts from EU-based programs?* This is a question
that requires joining the investment and revenue subledgers at the instrument level — a join
that is impossible in a GL.

---

### 13.7 Owner decisions for Strand F

| ID | Decision | Blocking what |
|---|---|---|
| F1-1 | Approve VRS international equity instruments (EUR/GBP/JPY allocations) | Level 1 |
| F1-2 | Approve FRED H.10 API for FX rates | Level 1 rate table |
| F2-1 | Accept or reject Level 2 (university endowment foreign notes) | Level 2 |
| F3-1 | Accept or reject Level 3 virtual asset simulation | Level 3 |
| F3-2 | Confirm 0.1% SNAP allocation assumption or supply alternative | Level 3 sizing |
| F3-3 | Confirm ETH inclusion (FY2016+ only) | Level 3 ETH instruments |
| F4-1 | Approve FRED H.10 API access | FX rate table |
| F4-2 | Approve CoinGecko/Coinbase historical data download | Level 3 rate table |

## 14. Identity expansion and large-group reporting recovery

Added 2026-11-14 at the owner's request for a plan. The owner subsequently
authorized the read-only Scala identity/concentration audit; see
`IDENTITY_AUDIT_CONTRACT.md`. Registry generation, synthetic events and reporting
changes remain **design_only**. This supplements the completed attribute
enrichment of the existing 1,248 instruments.

**Audit outcome:** execution completed; [findings](IDENTITY_AUDIT_FINDINGS_2026-11-14.md)
and `data/test_evidence/identity_audit_20261114/`. Counts reconcile, but source
CSV semantics, literal key order and large-group reporting must be addressed
before selecting a full generated population. The 3,794,594 refined unmapped
source-name candidates are not verified legal vendors. No generator or financial
correction was executed. Registry/simulation/reporting implementation remains a
separate next slice.

### 14.1 Objective and observed failure

Build an explicitly synthetic, reproducible counterparty population suitable for
Phase 2, while recovering meaningful source-party distinctions where source data
actually supplies names. Preserve the three measured runs and Gold snapshots.
New identities require new transformed/posted outputs; editing VendorMaster alone
does not change `0000000000` already written into immutable SJEs and ledgers.

Expanded 5v stopped at its default 250,000 input-row limit for `0000000000`.
That is a row-count guard, not a measured memory high-water limit. The actual
working state includes balance-key buckets, relationships, distinct sets and
dimensions. More vendors can reduce concentration, but must not be a device for
hiding an implementation bound: one legitimate large vendor or unattributed
population must still have an explicit bounded processing contract.

Static producer/consumer trace:

- APA expense TTL resolves `VENDOR_NAME` through `loadVendorMaster`; misses share
  the zero sentinel. The loader currently uses a plain comma split and silently
  overwrites duplicate name keys. Quoted-name parsing and ambiguity are therefore
  prerequisites to estimating the actual identity deficit.
- Revenue TTL resolves the source agency name to a reference agency code, then
  looks up that normalized code in the same master and falls back to zero.
  An agency is not evidence of the taxpayer or other receipt counterparty.
- PO transformation takes the source VENDORID directly. Expense lookup pads
  numeric master IDs to ten digits. Canonical formatting and cross-source bridges
  must be checked before asserting payment/PO identity equivalence.
- Expanded reporting merges SJEs, posted balances and dimensions by literal
  instrument key. Generated dimensions must use exactly those canonical keys and
  order; changing only a report label cannot repair upstream identity.

### 14.2 Distinguish the populations before generating IDs

| Population | Proposed treatment | Permitted interpretation |
|---|---|---|
| Existing 1,248 master instruments | Preserve keys and Bob's new attributes; validate aliases and formatting | Existing mapping, subject to its recorded provenance |
| Unmatched, meaningful APA vendor names | Stable generated source-party surrogate; retain raw name and source namespace | Source-name distinction, not verified legal identity or an eVA bridge |
| Generic names such as Journal Entry, blanks, payroll/control labels | Keep unresolved in the observed-data profile; explicitly model counterparties in a separate synthetic-event profile | A declared simulation, not recovered supplier identities |
| Revenue without a named counterparty | Keep original agency/fund/source-grain accounting and explicit unattributed status | No invented Instrument ID on the observed GL-grain source |
| Synthetic receipt/payroll/supplier detail | Generate separate raw events with declared synthetic counterparties and parent lineage | Synthetic subledger scenario only |
| Genuine high-volume vendors and residual unattributed groups | Retain their identities and test bounded reporting | Scale evidence cannot depend on arbitrary party splitting |

Do not create one vendor per transaction, fiscal year or fixed-size row bucket.
Those choices fabricate fragmentation/churn and would contaminate AIM3 labels.
Do not infer payment-to-PO identity from name equality alone, or silently collapse
ambiguous aliases. A source-name surrogate is not automatically a legal entity or
the canonical Instrument ID; its adapter and identity class must be explicit.

### 14.3 Implementation sequence and deliverables

1. **Freeze and profile.** Record the exact source/master/parser versions. Use a
   compiled Scala diagnostic to count matched/unmatched expense names, generic
   labels, revenue groups, ambiguous master keys and key concentration, both by
   year and across the complete reporting population. Include movement, balance
   and dimension input rows in concentration estimates. Produce bounded summary
   tables and a reason-coded coverage report. No accounting or posting in Python.
   Keep the PO quote-recovery change in a separately versioned comparison so a
   parser change is not mistaken for an identity improvement.
2. **Verify the adapters first.** Cheapest falsifiers: a quoted vendor name
   containing a comma; two master rows sharing a normalized name with different
   IDs; numeric IDs with/without leading zeros; whitespace/case/BOM variants;
   empty/generic names; and receipt records whose agency is not a counterparty.
   Require explicit ambiguity/missing coverage rather than last-row-wins mapping.
3. **Create the identity registry.** External-sort normalized source-party keys;
   assign deterministic, persisted IDs from a namespace checked against all
   existing master and PO keys. Specify the canonical encoding and adapter before
   choosing numeric ranges. Keep identity stable across years, file order and
   incremental additions; a sorted enumeration that renumbers existing parties
   is insufficient. Retain raw keys, normalized aliases, mapping version,
   provenance class, effective dates and any explicitly supported bridge evidence.
   Publish `identity_registry.csv`, `identity_aliases.csv`,
   `identity_coverage.csv` and a manifest, not a replacement for the old Gold master.
4. **Generate a separate synthetic scenario.** Extend Bob's attributes onto the
   new synthetic parties with documented rules, a fixed seed and plausible
   distributions. Produce raw synthetic subledger events with party IDs and
   lineage to their parent scenario/source aggregates; process them through TTL
   and ARE. Any decomposition preserves the named parent amount exactly and
   labels all allocation assumptions. Keep observed totals/source semantics
   separate from simulated counterparty detail. Do not relabel original posted
   receipt or Journal Entry rows as proven vendor activity.
5. **Choose population size from evidence.** First count meaningful unmatched
   source-name keys; that determines source-surrogate coverage. Separately specify
   the synthetic population, transactions per party, repeat activity and skew
   from the intended scenario. Profile a small population, a representative
   population and a deliberately skewed population. Do not select an arbitrary
   number of vendors merely to make every group fall below 250,000 rows. Keep
   at least one deliberately oversized vendor and one oversized unresolved group.
6. **Make reporting robust independently.** Declare bounds on live buckets,
   relationship keys, distinct sets and dimension versions. Test streaming
   aggregation where the existing order permits it; use declared spill partitions
   and sort/merge reduction for state that exceeds the memory budget. Preserve
   exact movement-to-posted-balance reconciliation and final vendor/year outputs.
   Spill partials are analytical intermediates, not new financial instruments.
   Keep the existing guard until its replacement has measured bounded-state
   evidence; do not simply raise it or remove difficult records.
7. **Run progressively.** Unchanged baseline fixtures first; named identity
   fixtures next; then one representative real year and the synthetic stress
   populations through the actual Bash/Scala orchestrator. Only after controls
   pass, create a new full identity/scenario workload in separate output roots.
   Re-transform, re-sort and re-post changed identities; reusing old posted
   balances with newly keyed journals is invalid. Run expanded reporting after
   all selected years' posting commits, with immutable physical input paths.

### 14.4 Architecture declaration for implementation

- **Owners:** identity/schema preparation and synthetic source generation belong
  to 01; sort/registry joins, controls and spill contracts to 02; canonical state
  to 03; expanded aggregation to 05. A report never assigns accounting identities
  retroactively. Existing engines continue to consume/emit their SJE contracts.
- **Runtime:** use Scala for profiling/identity resolution that feeds the
  measured path, synthetic event generation, transformation, posting and views;
  Bash for sequencing and file movement. No new Python is proposed. Expanded
  reference lookup must have an explicit byte/key limit; if the registry exceeds
  it, use a declared sorted join rather than an unbounded master map in each JVM.
- **Records:** preserve the 39-column SJE and existing ledger consumer schema;
  retain rule/source identity, journal balancing, effective dates and parent
  lineage. Registry/alias metadata is a versioned side contract. Freeze identity
  rules before applying them to source events.
- **CKB/order:** maintain the established SJE key and vendor-break aggregation.
  Canonicalize dimension keys before sorting. A registry sort/join, identity
  change requiring SJE re-sort, or spill reduction is a declared physical pass,
  not an assumed free extension of a logical layer.
- **Materialization/metrics:** separate source preparation, registry generation,
  external sorts, canonical SJE/ledger writes, reporting and spill reduction.
  Record process/partition lineage, CPU/wall time, per-process RSS, live-state
  peaks, spill bytes, retained bytes and reconciliation time. Record any missing
  GenevaERS runtime contract in the capability-gap register before claiming a
  helper represents a measured target-engine capability.
- **Downstream:** the new master/aliases must be demonstrably consumed by TTL;
  generated SJEs must reach posting; sorted dimensions must join to actual
  report keys; final features must reach a named evaluator before any AI claim.

### 14.5 Acceptance gates and decision points

Required gates: reproducible registry with no ID collisions or renumbering;
explicit ambiguous/unmatched coverage; unchanged journal-group balancing and
source lineage; conservation of amounts by family/agency/fund/account/period
where identity alone changes; movement/posted-state reconciliation; and failure
isolation with no partial output publication. If a synthetic event is split,
test child sums against its parent rather than requiring unchanged event counts.

Use an unchanged baseline fixture plus a named `identity-expansion-v1` workload.
Stress tests must pass for both a single party over 250,000 input rows and an
oversized unattributed group under the declared memory/spill bound. Measure the
largest key after generation; do not infer success from a larger master alone.
Counterfactual reporting on unresolved observed data remains a distinct test.

Before AI evaluation, distinguish observed source-name keys, supported identity
bridges and synthetic parties. No label-dependent generation; freeze rules before
holdout evaluation. Attributes derived from later spend need an observation
cutoff, and payment/PO survival labels require an explicit validated bridge or a
fully labeled synthetic panel. Report synthetic results as synthetic results.

Recommended first implementation slice: profiling plus the adapter falsifiers,
then a small deterministic registry and skewed synthetic fixture. Review measured
coverage and concentration before selecting full population size or scheduling
another corpus run. Payroll/generic-label decomposition assumptions and the
synthetic population specification are the material owner decisions; retain
unresolved groups until those assumptions are chosen. The original diagnostic authorization did not itself authorize generation or a
financial rerun. On 2026-11-14 the owner subsequently approved the CSV/key/bounded
reporting fixes, a small simulated registry fixture, then one new corpus run if
checks pass. See [identity recovery v1](IDENTITY_RECOVERY_V1_DESIGN.md). The
three-party/two-year pilot is separate from a full synthetic population. Real
reference/context mapping and connected-fixture checks still gate full-corpus admission.
The disk-capacity constraint was cleared by owner-authorized retirement on 2026-11-15.

### Reclass synthetic-source independence (owner clarification, 2026-11-14)

The owner authorized source-informed synthetic histories without requiring the
official publications as corpus inputs. `scripts/generate_reclass_history.sh`
reproduces the versioned pack from compiled Scala and fixed parameters. All dates,
keys and assumptions are recorded; selected VA patterns/dates are inspiration for
fictional records, not claims about actual entity histories. Sources need not
remain accessible. The raw research downloads were removed from the current
corpus; URL/hash/observation records remain. Generation has no network/raw-VA/PDF
dependency. [Evidence](../data/test_evidence/reclass_history_v1_20261115/README.txt).

The two logical families are reference/entity/relationship histories and hierarchy
edges. Physical schemas include agency/fund/object/program/source attributes, SAL,
CAR owner/party relationships, parties, contextual aliases and multiple hierarchies.
Start dates are inclusive; later starts delimit versions. Explicit end-date/gap
semantics are intentionally not inferred. RUNDATE is a supplied lookup operand,
not the machine clock or a knowledge snapshot. Recorded change dates are separate.
Generated oracles are input controls, not measured reclass processing. The actual
04 engine must still generate balanced SJE transfers and apply them through CKB;
05 Switch/Recast must consume the declared histories through explicit adapters.
