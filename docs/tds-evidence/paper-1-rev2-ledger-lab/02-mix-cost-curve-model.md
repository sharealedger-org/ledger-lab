# Mix Cost Curve Model — Events ↔ Balances (Paper 1, Rev 2)

**Status:** Model Output (Measured Corpus Constants + Explicit Unit Rate Assumptions)  
**Reference Design:** [`docs/COST_GRAPH_DESIGN.md`](../../COST_GRAPH_DESIGN.md)  
**Companion Spreadsheet:** [`03-charlie-anderson-model.xlsx`](03-charlie-anderson-model.xlsx)  

---

## 1. Architectural Hypothesis: The Cost Bowl

The **Mix Cost Curve** measures the total enterprise lifecycle cost (batch processing, data storage, query evaluation, and multi-master reconciliation) across the spectrum from pure raw transaction replay ($X=0$) to exhaustive combinatorial pre-computed balances ($X=100$).

$$\text{Total Cost}(P) = \text{Cost}_{\text{batch}}(P) + \text{Cost}_{\text{storage}}(P) + \text{Cost}_{\text{query}}(P) + \text{Cost}_{\text{reconciliation}}(P)$$

* **The Left Wall ($X=0$):** Zero pre-computation. To answer any financial question, the engine must scan the entire historical transaction log from inception. Query cost explodes linearly with query volume ($|Q| \times \text{Scan Cost}$).
* **The Right Wall ($X \to 100$):** Pre-computing answers to every anticipated question across $M$ parallel balance stores (GL, purchasing subledgers, cubes, marts). Storage, write amplification, and $\binom{M}{2}$ pairwise reconciliation costs explode combinatorially.
* **The Interior Minimum ($X=30$):** The Instrument Ledger (`LDGR` + CAR). Storing narrow key-period net movements satisfies monthly, quarterly, and annual reporting directly without event replay, eliminating both the left-wall scan tax and right-wall reconciliation tax.

---

## 2. Model Input Parameters

### A. Measured Corpus Constants (14-Year Totals / Annual Averages)
* **Raw Events ($N_{\text{events}}$):** 224,072,410 merged SJE rows (avg. 16.0M rows/year).
* **Raw Event Storage ($S_{\text{raw}}$):** 56.69 GB total (avg. 4.05 GB/year).
* **Monthly Ledger Store ($N_{\text{ldgr\_mon}}$):** 10,651,908 rows (1.715 GB total; avg. 0.123 GB/year).
* **GL Keys ($N_{\text{gl}}$):** 6,433,714 keys (avg. 1.04 GB total; 0.074 GB/year).
* **Permutation Capacity Bound:** $503,118,463,033,110$ combinations (upper bound).
* **Annual Engine CPU:** 11,207 seconds over 14 years (~800.5 CPU seconds/year).

### B. Unit Rate Assumptions (Configurable in Sheet 1 of Companion Workbook)
* **Scan Rate ($\text{Cost}_{\text{scan}}$):** $\$0.02$ per GB scanned.
* **Seek Rate ($\text{Cost}_{\text{seek}}$):** $\$0.0001$ per query lookup in narrow indexed ledger.
* **Storage Rate ($\text{Cost}_{\text{store}}$):** $\$0.023$ per GB-month ($\$0.276$/GB-year).
* **Reconciliation Rate ($\text{Cost}_{\text{reconcile}}$):** $\$50.00$ per master pair-month ($\$600.00$/pair-year).
* **Ingest Compute Rate:** $\$0.05$ per CPU-hour.
* **Annual Query Volumes:** 1,000 annual queries, 500 sub-annual queries, 100 dimensional queries.

---

## 3. Cost Bowl Calculations Across the Mix Dial

Annual operating costs calculated by the model for a single operational year:

| Dial ($X$) | Graph Point | Architecture & Store Topology | Stores ($M$) | Annual Cost | $\log_{10}(\text{Cost})$ |
|---|---|---|---|---|---|
| **0** | **P1: Events** | Pure Event Replay (Raw SJE Store) | 0 | **$1,830.00** | 3.26 |
| **15** | **P2: Annual** | Annual Movements (`T-annual`) + Sub-annual Event Scans | 1 | **$583.00** | 2.77 |
| **30** | **P3: Monthly** | **Instrument Ledger (`LDGR` + CAR) — Bowl Minimum** | 1 | **$16.29** | **1.21** |
| **60** | **P4: Cubes** | Movement Ledger + 2 Pre-computed Rollup Cubes | 2 | **$617.00** | 2.79 |
| **90** | **P5: Posted** | Status-Quo GL + 5 Specialized Sub-ledgers | 6 | **$9,018.00** | 3.96 |
| **100** | **P6: Permuted** | Combinatorial Pre-computed Balances (Capacity Bound) | - | **$22,400,000.00** | 7.35 |

---

## 4. Analytical Insights & Sensitivity

1. **Depth of the Interior Minimum:** At $X=30$ (Monthly Instrument Ledger), annual cost is **$16.29**, representing a **$112\times$ reduction** relative to pure event replay ($1,830) and a **$553\times$ reduction** relative to the multi-master status quo ($9,018).
2. **The Sub-Annual Query Driver:** The left wall steepness is governed entirely by sub-annual query frequency. If sub-annual queries are 0, Annual ($X=15$) costs $15.92. When even moderate sub-annual reporting is required (500 queries/year), Annual cost jumps to $583 because sub-annual boundaries were destroyed by annual aggregation.
3. **The Reconciliation Driver:** The right wall steepness is governed by multi-master synchronization $\frac{M(M-1)}{2} \times \text{Cost}_{\text{reconcile}}$. For $M=6$ stores, reconciliation alone is $9,000/year (15 pairs $\times$ $600/year).
