# VA Corpus — Paper 2 AI Model Evidence Tests (AIM0–AIM5)

**Folder:** `01-evidence/universal-ledger-demo/`
**Status:** Design only, 2026-11-04. No tests have been run at full scale.
**Source document:** [`ledger-lab/docs/TEST_DESIGN_PAPER_2.md`](https://github.com/KipTwitchell/ledger-lab/blob/main/docs/TEST_DESIGN_PAPER_2.md)
**Purpose:** Self-contained summary mapping the Virginia public procurement corpus to Paper 2
("Pre-Hierarchy Event Repositories") claims, recording the AIM0 closure finding, and
specifying AIM1–AIM5 with their AIM IDs and two-substrate experiment design.

---

## How to read this file

Each test corresponds to a specific claim in Paper 2. Every AIM test (except AIM0) is run
**twice** — once against the CCP1 substrates (GL output and permutation table, representing
the legacy architecture's best effort) and once against the CCP2/CCP3 outputs (instrument
ledger and Step 5 views, representing the CKB architecture). The gap between the two substrate
results is the empirical proof of Theorem 1.

**The three-layer framework (from Paper 2 §5.3):**

| Layer | What it uses as training data | What it cannot see |
|-------|-------------------------------|--------------------|
| Layer 1 | Text descriptions of transactions (verbalized records) | Event structure, field-level values, temporal sequence |
| Layer 2 | Pre-computed balance totals from posted accounting records | Any attribute discarded at posting time (counterparty, order date, event sequence) |
| Layer 3 | Raw pre-hierarchy events before any posting or aggregation | Nothing — this is the full record |

**The two-substrate design:**

| Substrate | Produced by | What it represents | Expected AIM result |
|-----------|------------|-------------------|-------------------|
| GL output | CCP1 | Today's General Ledger — instrument identity discarded | All AIM tests miss or degrade |
| Permutation table | CCP1 (follow-on step) | Legacy architecture's best effort: all key-embedded permutations generated from original transactions | All AIM tests still miss — permutations multiply dimensions but cannot restore event sequence |
| Instrument Ledger | CCP2 | CKB architecture — instrument identity preserved | AIM1 succeeds |
| Step 5 views | CCP3 | CKB perspective aggregation — bounded feature tables | AIM2–AIM4 succeed |

**The central claim of Paper 2** is that Layer 3 data contains predictive signal that Layer 2
data *cannot* contain, because posting permanently destroys the information. The tests below
measure that gap by running the same tests against both substrates and reporting the difference.

---

## 1. Corpus facts relevant to Paper 2

| Fact | Value | Source |
|------|-------|--------|
| APA expenditure files | FY2003–FY2016, 350 files, 20.3 GB, tab-delimited | `data/VARawFiles/` (git-ignored) |
| APA expenditure columns | `AGENCY_NAME, AMOUNT, FUND_NAME, LOCALITY, OBJECT_NAME, PROGRAM_NAME, VENDOR_NAME, VOUCHER_NUMBER, VOUCHER_DATE` | Header inspection FY2003/2013/2014/2016 |
| eVA PO files | FY2002–FY2016, 12 usable fiscal years (FY2004/2005/2009 absent or stub) | `data/VAPOData/` |
| eVA PO columns | `PONUMBER, AGENCY, ORDEREDDATE, QUANTITYORDERED, PRICE, VENDORID, VENDORNAME, NIGPCODE, …` | No Object/Fund code |
| Total parsed PO records | 133.46M accepted + 326K quarantined (FY2006–FY2016) | `ledger-lab/docs/CORPUS_FINDINGS.md` |
| Vendor-year rows | 210,496 (FY2002–FY2016 by-year summary) | `data/VAPOData/vendor_po_attributes_by_year.csv` |
| Unique vendors (PO panel) | ~65,000; 27,736 single-year; 7,553 appear 8+ years | `vendor_po_attributes_by_year.csv` |
| Single-year vendor churn | 43% of vendors appear in only one year | Derived from above |
| Agency name reconciliation | 99.1% of APA rows match reference exactly; 13 names (20,512 rows) unmatched; 3 names (131,809 rows) ambiguous between two reference keys | `data/corpus_profile/agency_alias_candidates.csv` |
| Zenodo DOI | `10.5281/zenodo.23846362` (47 files; 336 monthly APA files not yet uploaded as of 2026-11-01) | `ledger-lab/docs/ZENODO_ARCHIVE_PLAN.md` |

---

## AIM0 — Can PO orders be linked to payments? — CLOSED 2026-11-01

> **Paper 2 claim tested:** Before any predictive model can be run, we need to know whether the
> commitment-layer data (purchase orders) can be joined to the posted-layer data (expenditures).
> This test determines the scope of what is buildable from the public corpus.
>
> **Result:** No deterministic or independent PO-to-expenditure link exists in the public corpus.
> Tests using both layers simultaneously must use a noisy natural-key join (agency + vendor name +
> amount + date window) or an aggregate join (agency + vendor + fiscal year). AIM1–AIM5 are
> designed around what the data actually provides.

**Finding:** No deterministic or independent PO-to-expenditure link exists in the public corpus.

### Evidence table

| Fact | Value | Source |
|------|-------|--------|
| Direct link (VOUCHER_NUMBER = PONUMBER exact) | 1 match in 500,000 April-2014 rows | Prior probe |
| Natural-key match (agency, vendor name, amount) | 2.9% of FY2016 POs matched; 2,857 ambiguous vouchers | Prior probe |
| Payment-side vendor ID on public APA files | **None.** No VENDORID, VLIN, or PO reference on any APA CSV | Confirmed by header inspection |
| VENDORID namespace on PO files (FY2016) | 87.6% nine-digit, 6.7% all-nines placeholder, 4.2% two-letter + 7 digits, 0.9% blank | FY2016 PO file, 1,713,126 rows |
| The 2019/2020 EV matched dataset (`data/POTest/VA_2016_New/`) | **Constructed by name+amount join** — `(agency_key, amount, vendor_name)` inner/left/right. `ip_rec_id` on payment rows was copied from the PO side during assembly, not sourced independently from a payment system | `VA Preprocessing.xlsx` drawing1.xml flowchart, inspected 2026-11-01 |
| EV05 unmatched payments (`ip_rec_id` blank) | 3.5M rows; blank because they never received a PO-side ID | Column inspection |

### Decision table (output of AIM0)

| Linkage option | Available? | Consequences |
|----------------|------------|--------------|
| 1. Direct key (VOUCHER_NUMBER = PONUMBER) | No | — |
| 2. Natural key (agency, vendor, amount, date window) | Yes, noisy only | AIM1 feasible with noise caveat; cannot support vendor-identity claims |
| 3. Aggregate (agency, vendor, fiscal year) | Yes | AIM2, AIM4 feasible |
| 4. Independent payment-side vendor ID | No public source found | Test 2 (vendor substitution) **not buildable** from this corpus |

**Implication for original Test 2:** The EV dataset cannot show vendor substitution because it was
*defined* by vendor-name equality. Using name-match as the join criterion then looking for name
differences in the result is circular. An independent payment-side identifier (e.g. Cardinal
AP705/AP965 extract with VLIN or voucher line reference) would be required. No such public extract
has been located. Test 2 is replaced by AIM2–AIM5 below.

---

### AIM1 — How many distinct PO patterns collapse to the same posted key?

> **Paper 2 claim tested (Theorem 1, §2.1 — "The Posting Process as Lossy Compression"):**
> The posting process is non-injective: multiple distinct pre-hierarchy event patterns can produce
> the same posted record. This test measures that non-injectivity directly — no statistical
> inference required. It makes Theorem 1 empirically concrete.

**This test runs as a post-hoc step against aggregated output — not against the live event
stream.** This is deliberate: it measures the information loss from the *outside*, exactly as
any downstream consumer would experience it. A system that intercepts the stream mid-flight
trivially holds both sides; a downstream observer working from the posted output cannot.

**Two-substrate design:**

| Substrate | Method | Expected result |
|-----------|--------|----------------|
| **CCP1: GL output** | For each GL account balance `(AGENCY_NAME, OBJECT_NAME)`, count how many distinct PO patterns `(VENDORID, NIGPCODE_3digit, revision_suffix)` fed into it. | High collapse. Many-to-one. Instrument identity gone. |
| **CCP1: Permutation table** | The permutation table enumerates all key-embedded combinations derived from the same original transactions. Ask the same non-injectivity question against permutation keys. | Collapse appears reduced — but this is an artifact. Permutations were *constructed* from side tables after posting; the original event sequence (ordering, revision chain, temporal pattern) is still unrecoverable. |
| **CCP2: Instrument Ledger** | For each instrument ledger balance `(VENDORID, fiscal_year)`, count how many distinct PO patterns `(NIGPCODE_3digit, revision_suffix)` map to the same ledger key. | Instrument identity preserved. Non-injectivity measurable with full fidelity. Revision sequence visible. |

**Data:** PO files FY2006–FY2016 (133M records); APA expenditure files FY2006–FY2016.
Join via aggregate key `(AGENCY, fiscal_year)` — the only reliable cross-file join available
(established by AIM0).

**Acceptance:** This test has no pass/fail threshold — it is a measurement. Report the
distribution for all three substrates side by side. The gap between the CCP1 substrates and
the CCP2 result is the evidence. Any non-zero gap is informative; a large gap is strong
evidence of structural information loss that cannot be recovered by dimensional explosion.

**Output:** `data/test_evidence/AIM1_noninjectivity_distribution.csv` — one section per
substrate. A summary table comparing all three substrates is a direct citation target in
Paper 2 §2.1.

---

## AIM2–AIM5 — Replacements for the original vendor-substitution test

The original Test 2 required an independent payment-side vendor identifier that does not exist in
the public corpus. AIM2–AIM5 demonstrate the same architectural claim — that commitment-layer
features carry signal not visible in posted-only records — using data that actually exists.

**Start here if you're new:** AIM2–AIM5 each test the same fundamental idea from a different
angle. The idea is: *a purchase order (the commitment) carries information that never appears in
the final payment record (the posted balance).* If you only look at what was paid, you miss what
was ordered, when it was ordered, and what happened to orders that never completed. AIM2–AIM5
each find a different way to show that this missing information is predictively useful.

**Two-substrate protocol for AIM2–AIM5:** Each test is run against the CCP1 permutation table
(the legacy architecture's best effort — all dimensions available, but no event sequence) and
against the CCP3 Step 5 views (the CKB architecture's natural output). The acceptance thresholds
(Section 4) apply to the CCP3-substrate run. The CCP1-substrate run is expected to fail to meet
the threshold — that failure is itself a finding, reported alongside the CCP3 result.

---

### AIM2 — Does knowing what an agency ordered improve forecasting of what it will spend next year?

> **Paper 2 claim tested (§5.3 — "Does adding pre-hierarchy event features improve on that ceiling?"):**
> Commitment-layer features (what was ordered, from what commodity categories, with what revision
> rate) carry predictive signal for future expenditure that is invisible in posted payment records
> alone. A model trained on both layers (Layer 3) outperforms a model trained on payment totals
> only (Layer 2). This is the agency-year-level version of the core Paper 2 hypothesis.

**Inputs:**
- Layer 2 features: per agency-year posted expenditure by `VENDOR_NAME`. FY2014–FY2016 quarterly
  files locally available with full 3-year coverage.
- Layer 3 features: per agency-year PO commodity mix (NIGP 3-digit class share), revision rate
  (`-Vn` suffix count / total POs), inventory vs. services split (class <900 vs ≥900 share), median
  order size, vendor count. Derivable from `vendor_po_attributes_by_year.csv` and PO files FY2006–2016.
- Target: next-year agency goods-category expenditure.

**Data available:** ~80 agencies × 3 prediction years ≈ 240 labelled agency-year pairs.

**Method:** Train Model L2 (posted features only) and Model L3 (posted + PO commodity mix +
revision features) on the same observations. Report RMSE and R² per year; permutation test on the
Layer 3 feature block.

**Acceptance criteria (fix before running):** Layer 3 reduces RMSE by at least X% (propose 5%) and
permutation p < 0.05. Fix X before the first run.

**Controls:** Shuffle Layer 3 features across agencies within a year; improvement must vanish.
Agency name reconciliation across PO, expenditure, and budget files must be verified first (AIM5
produces this).

---

### AIM3 — Can we predict which vendors will stop doing business with the state, using only order history?

> **Paper 2 claim tested (§5.3 — "Does the event stream contain patterns that no balance-derived feature can surface?" / §1.2):**
> The pre-hierarchy event record preserves the full lifecycle of a vendor relationship — entry,
> activity pattern, exit. The posted expenditure record has no concept of vendor lifecycle at all:
> it records payments, not relationships. This test shows that lifecycle signal exists in the
> commitment-layer data, is predictive of vendor churn, and is *structurally absent* from any
> posted-only architecture (not just missing from this dataset, but impossible to include in any
> balance-posting system).

**Data:** 65,000 vendors × up to 12 years (`vendor_po_attributes_by_year.csv`). 27,736 single-year
vendors; 7,553 appear 8+ years. 12 usable fiscal years (FY2004/2005/2009 absent).

**Features (all PO-side, no join):** Entry cohort year, first-year PO line count, first-year NIGP
class, geographic region, year-over-year modal commodity class shift, annual PO line count
trajectory, agency concentration.

**Target:** Survival to year N+1 (binary) / churn in year N+1.

**Method:** Cox proportional hazards or discrete-time survival model. Hold out FY2015–2016; train
on FY2006–2014.

**Acceptance criteria:** C-statistic > 0.65 on held-out years with permutation p < 0.05.

**Key finding to report regardless of outcome:** The 43% single-year churn rate and its composition
by commodity class, geography, and entry cohort is itself a corpus-level finding about public
procurement dynamics with no posted-record equivalent.

---

### AIM4 — Does when an agency orders carry information that when it pays does not?

> **Paper 2 claim tested (§1.1 and the ordering seasonality experiment in §5.1 Track B):**
> Ordering happens at commitment time; payment happens at posting time. The posted expenditure
> record carries payment date — not order date. Fiscal-year-end budget dynamics (agencies racing
> to spend budget before June 30) show up in order dates, not payment dates. This test demonstrates
> that timing signal exists in the commitment layer that is invisible in the posted layer — a
> concrete, measurable instance of the information destroyed by posting. This is the cheapest test
> to run: no cross-file join, no agency reconciliation required.

**Data:** PO files FY2006–FY2016, `ORDEREDDATE` field. Top goods classes:
- Class 615 (Office Supplies, ~2.9M lines)
- Class 204/208 (Computer hardware/software, ~390K lines)
- Class 175 (Lab supplies, ~1.5M lines)

**Method:** For each agency-NIGP class combination, STL decomposition of monthly order volume into
trend, seasonal, and residual. Test whether the seasonal component shows a statistically significant
Q4 (Apr–Jun, Virginia fiscal year-end) spike.

**Acceptance criteria:** At least 3 of the top 5 goods classes show significant Q4 seasonality
(p < 0.05) in the majority of full-panel agencies (5+ years of data).

**Cheapest next test** — no reference join or agency reconciliation required.

---

### AIM5 — Can an AI model tell the difference between "same agency, different spelling" and "actually a different entity"?

> **Paper 2 claim tested (§3.3 — "The Data-Type Navigation Problem" / §1.2):** When a balance
> posting system summarizes transactions to an account code, it permanently discards the original
> name string. All variation in how an agency identified itself — abbreviation, reorganization,
> fund boundary — collapses to a single code. A pre-hierarchy architecture preserves those strings.
> This test shows that an AI model operating on preserved strings can distinguish *spelling
> variation* (same entity, high confidence) from *genuine organizational ambiguity* (different
> legal scope, low confidence). The low-confidence cases are not model failures — they are the
> model correctly identifying information that a
> balance-posting system would have silently hidden.
>
> **This test also unblocks AIM2 and AIM3** by producing the agency reconciliation table that
> cross-file joins require.

**Data:** `data/corpus_profile/agency_alias_candidates.csv`:
- 99.1% of APA rows match the reference table exactly (labeled positive training set)
- 13 agency names (20,512 rows) unmatched in the reference table
- 3 agency names (131,809 rows) ambiguous between two reference keys (test set)

**Method:**
1. Treat 99.1% exact-match pairs as positive examples; generate hard negatives (near-miss strings,
   same first token, different institution).
2. Train binary similarity classifier (logistic regression or lightweight transformer on character
   sequences).
3. Apply to 16 unresolved names; report predicted match, confidence score, and top reference
   candidate.
4. For the 3 ambiguous cases, report whether model confidence is genuinely split.

**Acceptance criteria:**
- Precision ≥ 0.99 on held-out exact-match pairs.
- For 16 unresolved cases: cases with confidence < 0.7 flagged as "genuine ambiguity," documented
  with both candidate agencies and row count affected — **do not force-resolve them**.

**Relationship to AIM2 and AIM3:** AIM5 *produces* the agency reconciliation table rather than
consuming it. High-confidence resolutions unblock AIM2 and AIM3; low-confidence cases are excluded
from cross-file joins and documented as scope limitations.

---

## 4. Decisions (confirmed 2026-11-04)

All values in this table were confirmed by the project owner on 2026-11-04 and are now fixed.
Do not adjust thresholds after any test has been run.

| Decision | AIM tests affected | Confirmed value |
|----------|--------------------|----------------|
| AIM2 RMSE improvement threshold | AIM2 | ≥ 5% |
| AIM2 permutation p-value | AIM2 | < 0.05 |
| AIM3 C-statistic floor | AIM3 | > 0.65 |
| AIM3 permutation p-value | AIM3 | < 0.05 |
| AIM4 seasonality p-value and class count | AIM4 | p < 0.05, ≥ 3 of top 5 classes |
| AIM5 precision floor | AIM5 | ≥ 0.99 |
| AIM5 ambiguity confidence threshold | AIM5 | < 0.70 |
| Surrogate key for ML train/test split | AIM2, AIM3, AIM4 | `vendorSurrogateKey = SHA256(VENDORID) mod 2^32` |
| Longitudinal evaluation strategy | AIM2, AIM3 | **7-window longitudinal** — train on years 1–7, predict year 8; repeat for all 7 windows across FY03–FY16 |
| AIM1: `PONUMBER` revision suffix (`-Vn`) in non-injectivity count | AIM1, AIM2 | **Include** — revisions are new commitment events; the CKB view schema includes `revision_suffix` as a distinct field |
| Original vendor substitution test | Paper 2 scope | Deferred — no independent payment-side ID found in public corpus |

---

## 5. Author notes

- AIM2–AIM4 replace the original Test 2 but preserve its architectural claim: commitment-layer
  features carry signal not visible in the posted record. The substitution is not a weakening; it
  is a shift from a single transaction-level test that requires an unobservable label to three
  corpus-level tests whose labels are directly observable.
- AIM5 is new; it was not in the original Paper 2 design. It was added because the agency
  reconciliation problem is itself an AI-demonstrable architectural claim, not merely a
  preprocessing step.
- AIM1 is new; it was not in the original Paper 2 design. It is the direct empirical proof of
  Theorem 1 (§2.1) and requires no ML model — a count. It should run before AIM2–AIM5.
- The AIM0 closure and the EV dataset finding (name+amount join construction) are the most
  consequential new facts from the 2026-11-01 session. They must be stated in Paper 2 §5
  before any test results.
- Acceptance thresholds must be committed to this file **before** any test run. Do not tune them
  after seeing results.
