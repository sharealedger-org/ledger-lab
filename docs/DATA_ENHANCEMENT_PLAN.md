# Data Enhancement Plan — VA Dataset for Risk/Finance Demonstration

> **Parent:** `PROJECT_BRIEF.md` — Universal Ledger POC  
> **Purpose:** Plan for extending the VA government expenditure data to support a GL vs. source-system reconciliation demonstration, the Basel/risk attribute gap analysis, and a phased enrichment roadmap that unlocks progressively richer risk/finance use cases.  
> **Status:** Draft — written in preparation for implementation.

---

## 1. What We Can Do With the Data As-Is

### 1.1 Current Attribute Inventory

The VA dataset provides five chartfield dimensions plus an instrument (vendor) dimension:

| Dimension | Reference File | Cardinality | Hierarchy levels |
|---|---|---|---|
| **Agency** | `aRefAgencyCSV.txt` | 326 entities | 1 (flat — agency code + name) |
| **Fund** | `aRefFundCSV.txt` | 10,645 fund-details | 2 — Fund Detail → Fund Type (24 top-level types) |
| **Object** | `aRefObjectCSV.txt` | 472 object codes | 2 — 4-digit Object → 2-digit Category (24 categories) |
| **Program** | `aRefProgramCSV.txt` | 2,486 sub-programs | 3 — Sub-Program → Program → Function (10 functions) |
| **Source** | `aRefSourceCSV.txt` | 2,275 sources | 2 — Source → Source Class |
| **Instrument (Vendor)** | `VendorMaster.csv` | 1,065 instruments | — |

The instrument dimension in `VendorMaster.csv` already carries several SAL attributes (populated in Round 0 and partially through Round 1 of Experiment 2):

| SAL Field | Status | Values |
|---|---|---|
| `instTypeID` | Real — complete | EXP (833), REV (232) |
| `instNIGPClass` | Real — partially populated | ~130 NIGP commodity classes |
| `instNIGPDesc` | Real — partially populated | Text description of NIGP class |
| `instGeoRegion` | Derived — partially populated | SE (606), NE (98), MW (82), W (23), SW (23), INT (1), blank (232) |
| `instVendorState` | Real — complete for EXP vendors | 2-letter state code |
| `instVendorAddress`, `instVendorCity`, `instVendorPostalCode` | Real | Street address fields |

Transaction data spans **FY2003–FY2016** (14 fiscal years):
- **FY2003–FY2013**: tab-delimited `.txt`, columns: Agency, Fund Detail, Object, Sub-Program, Vendor Name, Amount (+ Voucher Date from FY2007 onward)
- **FY2014–FY2016**: CSV format, same columns, different column order

Revenue files carry: Agency, Fund Detail, Source, Amount, Deposit Date.

### 1.2 Synthetic Report Set — What Is Constructible Now

With existing attributes, two coherent "views from different systems" can be constructed that will show a structural reconciliation gap:

#### Report Set A — The GL View (Aggregated Feed Perspective)

A traditional GL receives an aggregated feed stripped of vendor/instrument identity. Simulate this by collapsing the B### balance files on chartfield dimensions only, discarding Instrument ID:

| Report | Group-by dimensions | Measures | GL analog |
|---|---|---|---|
| **R-GL-1**: Agency Expenditure Summary | Agency × Object (2-digit) × Fund Type × FY | Sum(Amount) | Trial balance by agency and object category |
| **R-GL-2**: Function Spending Summary | Program Function × Object (2-digit) × FY | Sum(Amount) | Appropriation vs. expenditure by government function |
| **R-GL-3**: Revenue by Source Class | Agency × Source Class × Fund Type × FY | Sum(Amount) | Revenue ledger by source classification |
| **R-GL-4**: Fund Balance Summary | Fund Type × FY | Sum(EXP Amount) − Sum(REV Amount) | Net fund position by fund type |

These are what a state comptroller's office actually publishes — reproducible from publicly available CAFR data, giving a real-world anchor.

#### Report Set B — The Source System View (Instrument-Level Perspective)

A source system (procurement system, accounts payable system) knows about individual vendors. It produces reports the GL *cannot* reproduce because the GL feed aggregated away vendor identity:

| Report | Group-by dimensions | Measures | Source system analog |
|---|---|---|---|
| **R-SS-1**: Spend by Vendor Geography | instGeoRegion × Object (2-digit) × FY | Sum(Amount) | Procurement: in-state vs. out-of-state spend |
| **R-SS-2**: Spend by Commodity Class | instNIGPClass × Agency × FY | Sum(Amount) | Procurement: commodity spend analysis |
| **R-SS-3**: Vendor Concentration | instID × Agency × FY | Sum(Amount), Rank | AP: single-vendor dependency |
| **R-SS-4**: EXP/REV Instrument Split by Fund | instTypeID × Fund Type × FY | Sum(Amount) | Subledger: expense vs. revenue by fund |

Report Set B **cannot be reconciled to Report Set A** without the instrument dimension — which is precisely the structural gap the IL is designed to close. R-SS-4 is the simplest reconciling bridge: it should sum to R-GL-1/R-GL-4 exactly, but only if the instrument dimension is preserved through to the ledger.

#### The Demonstration Sequence

1. **Show R-GL-1** — agency expenditure from the aggregated balance. This is what the CFO sees.
2. **Show R-SS-2** — commodity class spend from the instrument balance. This is what the procurement officer sees.
3. **Attempt to reconcile them** — agency total in R-GL-1 for agency X should equal the sum of all commodity classes in R-SS-2 for agency X. *Without the instrument ledger*, this requires re-running the source system report at the GL level, which the GL cannot do — the vendor dimension was discarded in the feed.
4. **Show the IL closes it** — from the B### files with Instrument IDs, produce *both* reports from the *same* balance store. They reconcile because they share the same posting key.
5. **Measure the cost difference** — replicating step 3 in the legacy world requires maintaining a parallel AP subledger and a reconciliation process. The IL eliminates both.

### 1.3 What the Existing Data Proves — and Its Limits

The existing data is sufficient to demonstrate:
- ✅ The structural reconciliation gap exists and is reproducible
- ✅ The instrument ledger closes it without additional data
- ✅ The permutation cost of the legacy approach (reconciliation overhead)
- ✅ Two rounds of SAL attribute expansion (instTypeID, instNIGPClass/instGeoRegion already present in VendorMaster)

It is **not sufficient** to demonstrate:
- ❌ Cross-system reconciliation involving *different accounting rules* (the Basel problem) — requires a second "system" with different rules applied to the same instruments
- ❌ Credit risk, market risk, or liquidity risk reports — no maturity, counterparty type, product type, or risk weight attributes exist
- ❌ Legal entity consolidation with elimination — interagency transfer rules are skeletal fixtures only
- ❌ Multi-currency revaluation — all amounts are USD
- ❌ Time-series instrument state changes — VendorMaster has effect dates but no historical attribute changes recorded

---

## 2. What Enhancement Would Look Like

Enhancement operates on two axes independently: **new instrument attributes** (SAL enrichment) and **new transaction attributes** (richer source feeds). They unlock different capabilities.

### 2.1 SAL Enrichment — New VendorMaster Attributes

Each attribute added to `VendorMaster.csv` is available for free across all 14 years of transaction history — no reprocessing of source data required. This is the Instrument Pivot Theorem in practice.

#### Round 1 (Already Partially Present — Finalize)

| Attribute | Field name | Source | Action needed |
|---|---|---|---|
| NIGP Commodity Class | `instNIGPClass` | Real — in VendorMaster | Fill blanks (232 REV instruments have no class — assign `REV` sentinel or derive from source data) |
| Geographic Region | `instGeoRegion` | Derived from `instVendorState` | Complete the mapping; 232 REV instruments have no state — mark as `GOV-RECEIPT` |

**New reports unlocked at Round 1 completion:**
- Spend by commodity × agency × year (all 14 years, no reprocessing)
- In-state vs. out-of-state spend by program function
- Federal grant receipts vs. tax receipts vs. fee receipts by region

#### Round 2 — Vendor Size Classification

| Attribute | Field name | Source | Derivation |
|---|---|---|---|
| SBA size tier | `instSizeTier` | Synthetic or SBA DSBS | Classify as SMALL / LARGE / MICRO based on spend percentile within NIGP class, or derive from SBA Dynamic Small Business Search by vendor name + state |

**New reports unlocked:**
- Small business utilization rate by agency × commodity class × year
- Contract concentration risk: what % of spend goes to large vendors in each function?
- Year-over-year small business trend — relevant to VA procurement policy

This is a genuine public policy question the VA comptroller data can answer with this one attribute — and it's one the GL *cannot* answer, which makes the demonstration concrete.

#### Round 3 — Contract / Payment Type

| Attribute | Field name | Source | Derivation |
|---|---|---|---|
| Contract type | `instContractType` | Synthetic | Assign based on NIGP class: services → CONTRACTED, goods → PURCHASE-ORDER, interagency → INTERAGENCY, revenue → RECEIPT |
| Payment channel | `instPaymentChannel` | Synthetic | EFT / CHECK / WIRE — derive from amount threshold and vendor type |

**New reports unlocked:**
- Spend by procurement channel × agency (contract compliance)
- EFT adoption rate — relevant to treasury cash management
- Interagency spend identification — prerequisite for consolidation elimination entries

#### Round 4 — Risk Classification Attributes (The Basel Bridge)

This is where the government expenditure data is mapped onto a financial risk framework, creating the bridge between the procurement world and the risk world:

| Attribute | Field name | Mapping logic |
|---|---|---|
| Counterparty sector | `instCounterpartySector` | NIGP class → CORPORATE / GOVERNMENT / INDIVIDUAL / NONPROFIT / FINANCIAL |
| Exposure type | `instExposureType` | Object code → OPERATING-EXP / CAPITAL-EXP / TRANSFER / REVENUE |
| Maturity proxy | `instMaturityProxy` | Contract type + object code → SHORT-TERM (<1yr) / MEDIUM-TERM (1-5yr) / LONG-TERM (>5yr) / PERPETUAL |
| Risk weight proxy | `instRiskWeightProxy` | Counterparty sector → Basel standardized approach risk weight (0%, 20%, 50%, 100%) |

**This is the point where the two curves diverge in meaning:**
- The GL sees this as an expenditure ledger — it produces budget vs. actual reports
- The risk system sees this as an exposure ledger — it produces counterparty concentration, maturity gap, and capital adequacy reports
- In the legacy world these are *two separate systems* with *two separate feeds* that do not reconcile
- With the IL + enriched SAL, both report sets derive from the same balance store

**Risk/finance reports now constructible:**
- Counterparty sector concentration by agency × year
- Exposure maturity profile (short/medium/long-term outstanding obligations)
- Risk-weighted exposure proxy by program function — a government analog to Basel RWA
- Cross-system reconciliation: GL expenditure total = sum of risk exposures (by construction)

### 2.2 Transaction-Level Enrichment — Richer Source Feeds

Unlike SAL enrichment, transaction enrichment requires adding columns to the input files and reprocessing. The value is different: it enables time-series and event-level analysis rather than static attribute pivots.

| Enhancement | What it adds | Use case unlocked |
|---|---|---|
| **Purchase Order linkage** | PO number, PO date, PO amount alongside voucher | Three-way match: PO → receipt → payment. Unmatched items = control failure |
| **NIGP item-level codes** | 7-digit NIGP code (commodity + item) vs. 3-digit class only | Finer commodity analysis; aligns with federal procurement reporting |
| **Appropriation year** | Year the spending was appropriated vs. year it was spent | Multi-year obligation analysis; budget carryover |
| **Approval chain** | Approving officer ID, department ID | Segregation of duties analysis; control environment |
| **Adjustment flags** | Original / adjustment / reversal indicator | True net spend calculation; error correction rate |

These are available from the original VA source systems but were not included in the public data extract. They would need to be synthesized for POC purposes or sourced from USASpending.gov federal contract data as a proxy.

---

## 3. What Becomes Possible With Enhanced Data — Risk/Finance Perspectives

### 3.1 The Reconciliation Proof (Basel Problem in Government Analog)

The Basel III reconciliation requirement (BCBS 239, Principle 6) mandates that risk data and financial data reconcile to a common source. In banking, this means trading book positions must tie to the general ledger. In the government analog:

- **The GL** (R-GL-1 through R-GL-4) represents the *financial accounting* view — appropriations, expenditures, fund balances
- **The procurement/AP system** (R-SS-1 through R-SS-4) represents the *operational* view — vendor activity, commodity flows, contract performance
- **The risk overlay** (Round 4 SAL attributes) represents the *risk* view — counterparty exposure, maturity, risk weight

In the legacy architecture, each view has its own ledger, its own reconciliation process, and its own audit trail. The three views are structurally disconnected.

**With the enhanced IL:**
All three views derive from one balance store. Reconciliation is not a process — it is a mathematical identity. The cost of maintaining three reconciliation processes is eliminated.

### 3.2 The SAL Expansion Curve — Experiment 2 Made Concrete

With four rounds of enrichment:

| Round | SAL attributes | New reporting dimensions | Equivalent key-embedded balances |
|---|---|---|---|
| 0 | instTypeID | 2 (EXP/REV) | 2× current balance store |
| 1 | + instNIGPClass, instGeoRegion | ~130 × 7 = ~910 combinations | ~910× current balance store |
| 2 | + instSizeTier | + 3 size tiers → ~2,700 combinations | ~2,700× |
| 3 | + instContractType, instPaymentChannel | + contract × channel → ~16,000 combinations | ~16,000× |
| 4 | + instCounterpartySector, instExposureType, instMaturityProxy, instRiskWeightProxy | Full risk × finance × procurement pivot space — millions of combinations | Millions× — practically unbuildable |

The exponential growth in the "equivalent key-embedded" column is the empirical proof of the Instrument Pivot Theorem. The SAL grows by ~10 columns. The equivalent legacy architecture would need to materialize millions of balance records that will never all be queried. The IL materializes none of them — it answers any subset on demand.

### 3.3 The Cost Comparison — Three Views of the Same Data

Once Round 4 SAL enrichment is complete, the system can produce cost measurements for three report-generation strategies on the same report set:

| Strategy | How it works | Cost proxy |
|---|---|---|
| **Legacy GL** | Maintain separate balance stores for financial, procurement, and risk views; reconcile periodically | Storage × 3 + reconciliation process runtime |
| **IL without SAL** | Single balance store; re-read transactions to answer any attribute-level question | Storage × 1 + re-scan cost per ad-hoc query |
| **IL + SAL** | Single balance store + step-up join at query time; no balance duplication | Storage × 1 + SAL join at query time (near-zero marginal cost) |

The cost curves for these three strategies, plotted against reporting dimensionality (number of active SAL attributes), produce the core empirical chart of the project: the legacy cost grows exponentially, the IL + SAL cost is flat.

---

## 4. Implementation Roadmap

### Phase D-0 — Complete What Exists (Prerequisite)

- [ ] **Fill VendorMaster Round 1 gaps** — assign sentinel values for REV instruments missing `instNIGPClass` and `instGeoRegion`; document assignment rules
- [ ] **Build synthetic report set R-GL-1 through R-GL-4** — write ViewSpec rows for GL-aggregated views (drop Instrument ID from group-by); generate from B### files
- [ ] **Build source system report set R-SS-1 through R-SS-4** — write ViewSpec rows that include instrument dimension; generate from same B### files
- [ ] **Demonstrate the reconciliation gap** — show R-GL-1 agency total vs. sum of R-SS-2 by agency cannot be bridged without the IL

### Phase D-1 — SAL Round 2: Vendor Size

- [ ] **Derive `instSizeTier`** — classify each vendor by spend percentile within NIGP class across all 14 years; assign SMALL / LARGE / MICRO
- [ ] **Add to VendorMaster.csv** — one new column; no source data reprocessing
- [ ] **Uncomment Round 2 ViewSpec rows** — small business utilization reports
- [ ] **Record permutation ratio** — document key-embedded equivalent vs. SAL rows added

### Phase D-2 — SAL Round 3: Contract and Payment Type

- [ ] **Derive `instContractType`** — rule-based from NIGP class + object code
- [ ] **Derive `instPaymentChannel`** — rule-based from amount threshold + vendor type
- [ ] **Add to VendorMaster.csv**
- [ ] **Uncomment Round 3 ViewSpec rows** — procurement channel, interagency identification

### Phase D-3 — SAL Round 4: Risk Classification (The Basel Bridge)

- [ ] **Define mapping rules** — NIGP class → counterparty sector; object code → exposure type; contract type + object → maturity proxy; counterparty sector → risk weight proxy
- [ ] **Apply mappings to VendorMaster.csv** — four new columns
- [ ] **Uncomment Round 4 ViewSpec rows** — risk reports: concentration, maturity profile, RWA proxy
- [ ] **Produce the three-strategy cost comparison** — GL vs. IL-only vs. IL+SAL, across report dimensionality axis

### Phase D-4 — Transaction Enrichment (Optional, Publishability Enhancement)

- [ ] **Evaluate USASpending.gov** — assess whether federal contract data for Virginia agencies provides PO linkage and item-level NIGP codes
- [ ] **Synthesize adjustment flags** — add original/adjustment/reversal indicator to a subset of transaction files; demonstrate true net spend calculation
- [ ] **Document enrichment methodology** — make synthetic rules fully transparent for publishability

---

## 5. Key Design Decisions

**Why synthetic attributes are acceptable for Rounds 2–4:**  
The theorem being demonstrated is about the *architecture*, not the specific attribute values. A risk weight derived from a rule is as valid for demonstrating the cost curve as one sourced from a bank's credit rating system. The rule must be documented and reproducible — which it will be. The VA data is public domain; synthetic attributes derived by documented rules are publishable.

**Why the GL/source-system gap demonstration comes first:**  
You cannot argue that the IL is better until you have shown, concretely, what the legacy architecture costs and what it cannot do. The gap demonstration is the problem statement. Everything else is the solution.

**Why the SAL extension is non-destructive:**  
Every enrichment round adds columns to `VendorMaster.csv` and rows to `ViewSpec.csv`. No existing code changes. No reprocessing of source data. This is not a design convenience — it *is the theorem*. The zero-reprocessing property is what makes the cost curve flat.

---

*For the overall project context and the four-curve cost experiment, see `PROJECT_BRIEF.md`.*
*For session history, see `BOB_WORK_LOG.md`.*

---

## 6. Engine-Generated Journals and the Drill-Down Gap

Sections 1–5 address *reporting* from posted actuals. This section addresses what happens when the pipeline itself *generates* new journal entries — the revaluation-class processes (multi-currency, interest accrual, GAAP reclass, IUE, budgeting, allocation, consolidation) — and why doing these processes at summary level creates a structural audit gap that the instrument ledger uniquely resolves.

### 6.1 The Drill-Down Gap — The Core Problem

Every engine-generated journal in a legacy system is produced from a *summary balance*, not from an instrument-level position. The generated entry lands in the GL at the same level of granularity as its input: aggregate. The audit trail reads:

> *"Currency revaluation generated $4.2M gain. Posted to FX_GAIN account."*

A CFO or regulator who asks *"which instruments drove that gain, and what are their current FX exposures?"* cannot get an answer from the GL. The generated entry has no instrument dimension — it was never there. The only path is back to the source system, which may have a different revaluation methodology, different rate, different scope. The gap is not a data quality problem. It is **structural**. Summary-level generation permanently destroys the instrument lineage.

In the IL architecture, every generated journal carries the instrument ID as its anchor. The same question — *"which instruments drove that gain?"* — is answered by a join on the balance store. No source system lookup. No reconciliation process. The audit trail is intact by construction.

### 6.2 Process-by-Process: What Each Engine Adds and What Goes Dark in Legacy

#### Multi-Currency Revaluation

**What it does:** At period end, each foreign-currency balance is revalued to its current domestic equivalent using a spot or average rate. The difference between the prior-period value and the current value is posted as an unrealized FX gain or loss.

**What the legacy GL sees:** One net entry per currency per account per period. E.g., `DR FX_UNREALIZED_GAIN 4.2M / CR USD_EQUIVALENT 4.2M`.

**What goes dark:** Which instruments held those foreign-currency positions. Which counterparties. Whether the gain is concentrated in a few large positions or spread across many small ones. Whether the same instruments that drove the gain last quarter are driving it again — or whether it is a new concentration. None of this is answerable from the GL entry alone.

**What the IL preserves:** Every instrument that held a foreign-currency balance in the prior period has its own revaluation SJE, keyed to its `instID`. The FX gain/loss is instrument-specific. A regulator can ask: *"Show me all instruments with unrealized FX losses exceeding $1M, grouped by counterparty sector."* One ViewSpec row. No source lookup. No reconciliation.

**SAL attributes required to unlock this:**
- `instFXCurrency` — the instrument's functional currency (EUR, GBP, JPY, etc.)
- `instCounterpartySector` — for concentration analysis by counterparty type
- `instDomicile` — for geographic FX exposure analysis

**VA data analog:** The VA data is USD-only, so multi-currency revaluation cannot be demonstrated on the existing data directly. However, the *structural argument* is demonstrable: show that the `forecastingBudgeting.scala` step generates budget entries from `LDGR{year}.csv` at the balance level, and show that budget variance can be tracked to individual vendors (instruments) in the IL but not in the GL. The variance-by-instrument query is the structural analog to FX-gain-by-instrument.

---

#### Interest / Fee Accrual (IUE — Instrument-Level Unwinding Entry)

**What it does:** For instruments that earn or pay interest, fees, or other time-value amounts, the accrual process computes the period's income/expense from the instrument's balance and rate, and generates an accrual journal entry. IUE (Instrument-Level Unwinding Entry) specifically refers to the reversal and re-accrual pattern used when actual cash flows differ from the accrual estimate.

**What the legacy GL sees:** Aggregated accrual entries by account and period. The interest income line on the income statement. The accrual balance on the balance sheet.

**What goes dark:** Which instruments generated that income. Whether the income rate matches the contracted rate on each instrument. Whether any instruments are non-performing (accrual generated, but cash not received). The aging of the accrual balance by instrument. None of this is visible from the GL entry.

**What the IL preserves:** Each instrument's accrual is computed from its own `instInterestRate` SAL attribute and its own balance. The accrual SJE carries the `instID`. Non-performance is visible: instruments where the cash receipt SJE did not follow the accrual SJE within the contractual grace period are identifiable by inspection of the instrument's balance history. This is the foundation of instrument-level credit monitoring — impossible from summary entries.

**SAL attributes required:**
- `instInterestRate` — contractual rate (already present in the banking Demo Data SAL from Session 13)
- `instMaturityDate` — for time-to-maturity-weighted income projection
- `instAccrualBasis` — actual/365, 30/360, etc.
- `instPerformanceStatus` — performing / watch / non-performing (changes over time, requiring effective dating)

**VA data analog:** The `arrangementReclass.scala` step is the closest structural equivalent: it generates SJEs from a SAL attribute change (`instTypeID` change in `VendorUpdate.csv`), producing instrument-specific journal entries that update the balance file. The demonstration here is: show that a vendor type change generates instrument-specific reclass entries visible per vendor in the IL, but that in a legacy system the same reclassification would be a single aggregate entry with no vendor-level audit trail.

---

#### GAAP Reclass / Multi-Book

**What it does:** The same economic position is reported under multiple accounting standards simultaneously — GAAP for external financial reporting, IFRS for international regulatory reporting, management accounting for internal performance measurement. Each standard applies different recognition rules: e.g., held-to-maturity vs. fair-value-through-OCI under IFRS 9 changes whether an unrealized gain appears in income or equity. The reclass process generates adjusting entries that transform the GAAP basis balance into the IFRS basis balance, creating parallel "books."

**What the legacy GL sees:** Multiple chart-of-account segments tagged to each entry (`GAAP_BOOK` vs. `IFRS_BOOK` vs. `MGMT_BOOK`), or entirely separate ledger instances. Reconciliation between books is a manual process or a separate reconciliation system.

**What goes dark:** Why specific instruments are classified differently across books. Which instrument classifications drive the largest book differences. Whether the book difference is stable or changing as instrument attributes change. The instrument-level audit trail for each classification decision.

**What the IL preserves:** `instCARType` (the Contract Attributes Record type — Finance/Risk/MIS/CRM as named in the 2014 IBM architecture) identifies which SAL governs each book's classification rules. The same instrument, joined to different CARs, produces different book views from the same underlying balance. Book differences are not reconciliation items — they are join results. The classification decision is auditable because the SAL attribute that drove it is date-effective and version-controlled.

**SAL attributes required:**
- `instCARType` — which CAR/SIL governs this instrument's classification (Finance, Risk, MIS, CRM)
- `instAccountingCategory` — held-to-maturity / available-for-sale / FVTPL / FVOCI (IFRS 9 categories)
- `instHedgeDesignation` — hedged / not hedged (affects fair value vs. accrual accounting)

**VA data analog:** The consolidation step (`consolidation.scala`) already demonstrates multi-book logic in miniature: the same transaction appears in Agency A's individual books as an expense AND in Agency B's books as revenue, but in the consolidated view one of them is eliminated. The instrument that generated the interagency transfer is the pivot point. Show this at the instrument level (which vendor caused the interagency exposure) vs. at the summary level (a $X elimination entry with no vendor attribution).

---

#### Financial Allocation

**What it does:** Overhead costs accumulated in a central pool are distributed to cost objects (products, channels, business units, geographies) using a driver-based rule. The allocation generates debit entries in each receiver and a credit to the source pool. The financial position is unchanged — only the distribution of cost changes.

**What the legacy GL sees:** Allocation credit in the overhead account, allocation debits in each cost object. The cost objects are GL account segments, not instruments. No individual vendor or transaction is traceable to any specific allocation.

**What goes dark:** Which *instruments* (contracts, vendors, customer relationships) absorbed the overhead. Whether the allocation is proportional to actual instrument-level activity or is a rough approximation. Whether a specific large vendor relationship absorbs more than its fair share of overhead — which would distort instrument-level profitability.

**What the IL preserves:** Because balances are instrument-keyed, the allocation driver (e.g., each instrument's share of total agency expenditure) is computed at the instrument level. Each instrument receives an allocation proportional to its actual transaction volume. Instrument-level profitability — revenue minus direct cost minus allocated overhead — is computable for the first time. In banking terms: instrument-level funds transfer pricing (FTP) and cost allocation are prerequisites for product profitability and customer profitability measurement. Both require the instrument dimension to be preserved through allocation.

**SAL attributes required:**
- `instCostCenter` — the cost center that "owns" this instrument relationship
- `instProductLine` — for product-level profitability roll-up
- `instRelationshipManager` — for RM-level P&L attribution

**VA data analog:** The existing `financialAllocation.scala` already allocates EXP999 overhead to all programs in the same agency. Enhance the demonstration by showing: (1) without instrument level, the allocation lands on program accounts and no further disaggregation is possible; (2) with instrument level, the allocation can be traced to each specific vendor relationship, showing which vendors in each program are the largest absorbers of overhead cost.

---

#### Forecasting / Budgeting

**What it does:** Projects future balances from current actuals using growth rules. The budget balance file (`BUDGET_LDGR{year}.csv`) has the same structure as actuals, enabling variance analysis at any level of aggregation.

**What goes dark (in the legacy case):** Budget entries are typically set at the account/department level — not at the instrument level. When budget variance is analyzed, the question "which vendors are driving the overspend?" requires a separate AP system lookup, a separate query, a separate reconciliation. The budget and the actuals are stored in different systems with different keys.

**What the IL preserves:** Because the budget balance file has the same instrument key as the actuals, budget-vs-actual variance is answerable at the instrument level immediately. *"Show me vendors where actual spend exceeded budget by more than 20%, grouped by NIGP class and agency."* One ViewSpec row against two joined LDGR files. No cross-system lookup.

**VA data analog:** `forecastingBudgeting.scala` already produces `BUDGET_LDGR{year}.csv`. The drill-down demonstration is: (1) show the variance by agency at summary level (what the legacy GL provides); (2) show the variance by vendor within an overspending agency (what the IL provides but the legacy GL cannot). The specific vendors driving an agency's overspend are visible in step 2 and invisible in step 1.

---

#### Consolidation and Elimination

**What it does:** Combines legal entity (agency) balances into a state-wide consolidated view, then eliminates interagency transactions to prevent double-counting of intergovernmental transfers.

**What goes dark (in the legacy case):** The elimination entry is a net figure. After consolidation, the information that Agency A paid Agency B for shared services via Vendor X is gone. If the state comptroller later asks "which vendors provided services across multiple agencies?" or "which interagency transfers involved a common third-party contract?", the consolidated GL cannot answer — the instrument dimension was never in the consolidated entry.

**What the IL preserves:** Because instrument IDs survive through consolidation, the vendor that provided the cross-agency service is traceable in the consolidated balance. Cross-agency vendor relationships — which are procurement governance questions, not just accounting questions — are visible by inspection. This is relevant to state-level vendor fraud detection and procurement efficiency analysis.

**VA data analog:** `consolidation.scala` already implements this. The enhancement demonstration: (1) show the CONSOL_LDGR without instrument IDs (legacy analog); (2) show the same file with instrument IDs; (3) answer the question "which vendors appear in expenditures for more than 3 agencies?" from case 2 but not case 1.

---

### 6.3 The Unified Drill-Down Gap Argument

Each process above follows the same structure:

| Process | What the GL summary entry records | What it cannot answer | What the IL instrument entry answers |
|---|---|---|---|
| Multi-currency revaluation | Net FX gain/loss by account | Which instruments, which counterparties | FX exposure by instrument × counterparty sector |
| Interest / IUE accrual | Net accrual by account | Which instruments are non-performing | Accrual aging, non-performance flag, per instrument |
| GAAP reclass / multi-book | Adjustment by account × book | Why instruments differ across books | Classification decision auditable via SAL CAR type |
| Allocation | Overhead credit/debit by account | Which instruments absorbed overhead | Instrument-level cost, profitability, FTP |
| Forecasting | Budget by account | Which instruments drive variance | Vendor-level budget vs. actual, immediately |
| Consolidation | Elimination entry | Which instruments cross entity boundaries | Cross-entity instrument relationships, intact |

In every case the legacy gap has the same shape: **the generated entry is summary-keyed at input, so it can only be summary-keyed at output.** The instrument dimension was never there to survive.

The IL breaks this pattern with one design choice: every balance carries an instrument ID. Every generated entry — regardless of which engine produced it — inherits that key. The drill-down gap does not exist because the instrument dimension was never discarded.

This is the empirical content of Paper 6 §§3.3, 5 (Arrangement Reclass as first-class business event) and the broader claim of Paper 5 §5 (SIL/CAR types as the mechanism for multi-book, multi-purpose pivoting from a single balance store).

---

### 6.4 Additional SAL Attributes Required for Engine-Generated Journal Demonstrations

Building the above demonstrations into the VA pipeline requires extending `VendorMaster.csv` beyond the current four-round plan. These are **Phase D-5** items — dependent on the Basel bridge (Phase D-3) being complete first, since the risk attributes are prerequisites for several of these.

| Attribute | Field name | Required for | Derivation |
|---|---|---|---|
| Functional currency | `instFXCurrency` | Multi-currency revaluation | Assign by `instGeoRegion`: INT→USD (intergovernmental), SE/NE/SW/MW/W→USD (domestic). For a multi-currency demonstration, assign non-USD to international vendors |
| Interest rate | `instInterestRate` | IUE / accrual | Assign by object code: capital leases (OBJ 21/23) → synthetic rate; operational services → 0 (no accrual) |
| Accounting category | `instAccountingCategory` | GAAP/IFRS reclass | Rule from contract type + object code |
| CAR type | `instCARType` | Multi-book | Finance (for GL-equivalent view) / Risk (for exposure view) / MIS (for management view) |
| Cost center | `instCostCenter` | Allocation drill-down | Derived from agency + program function |

**Note on multi-currency in the VA data specifically:** The VA data is domestic USD only. A true multi-currency demonstration requires either (a) assigning synthetic non-USD currencies to a subset of vendors (e.g., international suppliers identifiable by `instGeoRegion = INT`) or (b) using a different dataset. Option (a) is sufficient for structural demonstration — the point is that the *revaluation engine* produces instrument-specific SJEs, not that the specific exchange rates are real.

---

### 6.5 Phase D-5: Engine-Generated Journal Demonstration Roadmap

This phase builds on the complete Phase D-3 (Basel bridge) SAL and extends the pipeline demonstrations to include each generated-journal process.

**Prerequisites:** Phase D-3 complete; `instCounterpartySector`, `instExposureType`, `instMaturityProxy`, `instRiskWeightProxy` populated in VendorMaster.

- [ ] **D-5a — Allocation drill-down:** Extend `AllocationRules.csv` with at least two rules (intra-agency overhead; cross-agency shared services). Run `financialAllocation.scala`. Show overhead absorbed per vendor in IL vs. overhead visible only at account level in legacy. **Metric:** ratio of answerable vendor-level cost questions before vs. after instrument-level allocation.

- [ ] **D-5b — Budget vs. actual drill-down:** Run `forecastingBudgeting.scala` to produce `BUDGET_LDGR{year}.csv`. Write a ViewSpec row joining actuals LDGR to budget LDGR on instrument key. Show vendor-level budget variance for one overspending agency. **Metric:** number of vendors identifiable as the source of the variance; this number is zero from the GL, non-zero from the IL.

- [ ] **D-5c — Consolidation instrument trace:** Populate `InteragencyTransfers.csv` with at least five known interagency pairs from the VA data (agencies that appear in each other's revenue and expenditure records in the same period). Run `consolidation.scala`. Show CONSOL_LDGR vendor query: *"Which vendors appear in both Agency A expenditure and Agency B revenue?"* **Metric:** vendor count answerable from consolidated IL vs. zero from consolidated GL.

- [ ] **D-5d — Reclass audit trail:** Use existing `arrangementReclass.scala` with a set of `VendorUpdate.csv` entries that change `instTypeID`. Show the per-instrument RECLASS_SJE entries. Compare to what a legacy system would record (a single net reclassification entry). **Metric:** audit trail records per reclassified instrument — always 1 in legacy (net entry), always N (one per period per instrument) in IL.

- [ ] **D-5e — Synthetic multi-currency revaluation (structural demonstration):** Assign `instFXCurrency = GBP` to international vendors (`instGeoRegion = INT`). Add a synthetic FX rate table (`FX_RATES.csv`: date, currency pair, rate). Write `currencyRevaluation.scala` (new step, mirrors `forecastingBudgeting.scala` pattern: reads LDGR + FX_RATES + VendorMaster, generates REVAL_SJE per instrument per period per currency). Show FX gain/loss attributable to each instrument. **This is the most powerful single demonstration of the drill-down gap** — because FX revaluation is precisely the process that generated the most notorious reconciliation failures in banking history (multiple banks were fined for inability to explain FX P&L to regulators at instrument level).

- [ ] **D-5f — IUE / interest accrual (structural demonstration):** Assign `instInterestRate` to capital-expenditure vendors (object codes 21, 22, 23 — equipment and property). Write `interestAccrual.scala` (new step: reads LDGR + VendorMaster, generates ACCRUAL_SJE per instrument per period using outstanding balance × rate / 12). Show accrual income by instrument vs. aggregate accrual income line in GL. **Metric:** ratio of accrual income attributable to identified instruments — 100% in IL, 0% in GL.

---

### 6.6 The Argument Structure This Enables

With Phase D-5 complete, the full demonstration sequence is:

1. **The static gap** (Sections 1–3 of this document): Even without any generated journals, the GL/source-system reconciliation gap exists on posted actuals. The IL closes it.

2. **The dynamic gap** (Section 6): Every engine-generated journal process widens the gap further because each process discards instrument detail at generation time. The IL prevents this — generated entries inherit the instrument key.

3. **The compounding case**: In a real enterprise, all six processes run simultaneously — revaluation, accrual, reclass, allocation, budgeting, consolidation — each one adding another layer of un-attributable summary entries. The IL is the only architecture that keeps all six auditable at instrument level without maintaining six separate subledger reconciliation processes.

4. **The cost argument** (Experiment 1 curves C3 and C4): C3 adds revaluation-class processes; C4 adds allocation and consolidation. If C3 ≈ C2 and C4 ≈ C3 on the cost surface, the single-pass universality claim is empirically validated AND the drill-down capability comes at near-zero marginal cost. That combination — same cost, exponentially more capability — is the theorem.
