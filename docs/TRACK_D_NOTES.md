# Track D: Unrestrained AI Subject Engine — Summary & Assessment

**State:** Gate 3 Arm B prototype implemented and verified on the golden acceptance fixture in the companion repository [`sharealedger-org/ledger-lab-control`](https://github.com/sharealedger-org/ledger-lab-control) (commit `c3ec922821f37a2c69a5c7a89892b9b3993f7693`). Ready for full-corpus execution on the designated benchmark machine.

---

## 1. What Has Happened

1. **Companion Repository Created:** Established `sharealedger-org/ledger-lab-control` as an isolated, private repository. It contains zero Scala pipeline code, zero GenevaERS references, and zero CKB match-merge engine contracts.
2. **Staged Gating Feeds:**
   * **Gate 0:** Stated hardware limits (16 GB RAM, 4 CPU threads, 400 GB SSD) and 14-year Virginia raw layout ($\approx 100\text{M}$ records, $\approx 20\text{ GB}$).
   * **Gate 1:** Conceptual Papers 1 and 2 (cost bowl theory, permutation explosion, event repositories, and the AI early-accumulation bias footnote).
   * **Gate 2:** Five target reporting workloads (W1 Monthly Actuals, W2 Vendor Survival / AIM3, W3 Commodity Mix / AIM2, W4 Seasonality / AIM4, W5 Within-Stream Lineage / AIM1).
3. **Golden Acceptance Fixture:** Established mini-slice sample data and exact expected CSV outputs for all five workloads (`data/fixture/`).
4. **Codex Arm B Implementation (`src/ledger_control.py`):**
   * Implemented in pure Python 3 standard library with a disk-backed SQLite database.
   * Streams CSV/TSV records, computes SHA-256 hashes, tracks 1-based physical line numbers, and stores raw JSON payloads.
   * Generates balanced double-entry lines in a `journal` table with gross turnover preservation.
   * Creates 4 composite B-tree indexes (`expenditure_lookup`, `commitment_lookup`, `vendor_year`, `journal_cell`).
   * Implements custom 80-digit `Decimal` precision SQL aggregate `dsum` to ensure exact financial arithmetic.
5. **Fixture Verification Passed:**
   * `scripts/verify_fixture.py` passes 100% across all 6 expected outputs (W1: 12 rows, W2: 4, W3: 4, W4: 5, W5 exp: 2, W5 po: 2).
   * `tests/test_engine.py` passes 6/6 tests covering multiline starting-line identity, negative refunds, and gap-year censoring.

---

## 2. Projected Architectural Assessment (CKB Cost Model cost surface)

Codex's design represents the **Relational / Database Substrate Baseline** (Curve 3 on the cost graphs):

### A. Memory Discipline ($O(1)$ Working Set)
* **Observed:** By choosing a disk-backed SQLite engine with a 32 MB page cache, Codex avoided the naive in-memory Pandas/DataFrame OOM crash on $100\text{M}$ rows.

### B. Ingestion & Write Amplification Penalty
* **Projected:** Inserting $100\text{M}$ source records and $190\text{M}$ journal lines into an indexed SQLite B-tree database with JSON payloads will incur substantial write amplification, page splits, and WAL/journaling I/O compared to Ledger Lab's streaming sequential writes ($21\text{m}$ in Scala).
* **Cost Impact:** Elevates the batch ingestion baseline cost by an estimated $5\times\text{–}15\times$.

### C. Query Time & Indexing Dynamics
* **Workloads W1–W4:** Execute as dynamic SQL `GROUP BY` and table-scan queries over millions of rows, rather than being extracted in the same single sorted pass as Ledger Lab does in Step `5v`.
* **Workload W5 (Lineage):** Executes via B-tree index seeks on composite keys, avoiding full event scans for point queries, but paying for it via permanent index storage and maintenance.

### D. The Resulting "Twisted" Cost Bowl (Curve 3)
* **Left Wall:** Moderately higher than CKB streaming due to SQL scan/parse overhead over large SQLite tables.
* **Bowl Minimum:** Significantly elevated above CKB's CCP2 minimum ($\approx \$5$ laptop run vs. multi-hour SQL ingestion/indexing).
* **Right Wall:** Avoids permutation cube explosion by querying dynamically, but pays a continuous storage and write amplification tax on all maintained B-tree indexes and redundant JSON payloads.

---

## 3. Next Execution Action

* **Host:** Designated benchmark host (16 GB RAM, 4 CPU threads, 400 GB SSD).
* **Isolation Rule:** The raw Virginia CSV/TSV directory supplied to `ledger-lab-control` must contain only raw source data, strictly separated from any `ledger-lab` Scala pipeline outputs or directories.
* **Execution:** Run `ledger_control.py build` and `ledger_control.py export` under `scripts/benchmark_harness.sh` to record official wall-clock time, peak OS RSS, and disk footprint for Curve 3.
