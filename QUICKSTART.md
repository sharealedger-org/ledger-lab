# ShareALedger Ledger Lab — Quick Start

> This is the public Ledger Lab successor to the original Universal Ledger demonstration system.

> **Purpose:** Get the pipeline running on your laptop in minutes, understand what each step does,
> and run the two cost-surface experiments that ground the
> [nine-paper monograph](https://github.com/KipTwitchell/universal_ledger).

---

## What This Is

A **runnable, ground-up implementation of how financial systems really work** — not a textbook
example, not a black-box ERP, but every step of the process laid bare in readable Scala code,
running on 13 years of real State of Virginia government financial data.

The pipeline implements the **ARE / AL / ALE** architecture described in the monograph:

```
Raw transactions
    │
    ▼  Step 2 — standardizeAndSort   (Accounting Rules Engine — ARE)
Universal Journal entries (SJEs)  +  Vendor Master (Contract Attributes Record / CAR)
    │
    ▼  Step 3 — post               (Instrument Ledger — IL)
Balance file (LDGR)
    │
    ├──▶  Step 5 — dataAggregation      (ViewSpec-driven pivot; Instrument Pivot Theorem)
    ├──▶  Step 6 — financialAllocation  (prior-day divisor design; single-pass universality)
    ├──▶  Step 7 — consolidation        (interagency elimination)
    ├──▶  Step 8 — forecastingBudgeting (balance × growth factor; no A-partition needed)
    ├──▶  Step 9 — arrangementReclass   (VendorMaster-driven reclass events; run before 4,5)
    └──▶  Step 4 — contraCreation       (reconciliation proof; run LAST)
```

The algorithmic engine is **match-merge** (sorted-file join / Common Key Buffering) — the same
approach used by GenevaERS on the mainframe, here made transparent in readable Scala.

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| JDK | 8 or 11 | `java -version` to check |
| sbt | 1.x | [get sbt](https://www.scala-sbt.org/download.html) |
| Scala | 2.11.8 | installed automatically by sbt |

**That's it.** Steps 2–9 (the core pipeline) require no Spark, no database, no VM.
The repo contains small fixture data files so you can run end-to-end immediately.

> **Steps 1 and 11** use Apache Spark. They are not needed to run any of the nine Financial System
> Patterns or either experiment.

---

## 30-Second Run (fixture data, any laptop)

```bash
# 1. Clone and enter the project
git clone https://github.com/KipTwitchell/universal_ledger.git
cd universal_ledger/02-foundation/va_pipeline

# 2. Set up output directory
mkdir -p ../../data/output

# 3. Run Step 2 — Standardize & Journalize
#    Reads raw VA expenditure data → produces Universal Journal entries + Vendor Master
sbt "run --step 2 --inPath ../../data/ --outPath ../../data/output/ --year 03 --quarter 01"

# 4. Run Step 3 — Post
#    Reads sorted journal entries → produces Balance (Ledger) file
sbt "run --step 3 --inPath ../../data/output/ --outPath ../../data/output/ --year 03"
```

After these two commands you will have:
- `data/output/JE*.csv` — Universal Journal entries (one debit + one credit per transaction)
- `data/output/SortedJE03.csv` — Journals sorted on the full balance key, ready for posting
- `data/output/LDGR2003.csv` — The Balance file: one row per unique (vendor × nominal account × period)

---

## Understanding the Output

### Universal Journal (`JE*.csv` / `SortedJE*.csv`)
Every financial transaction becomes **two lines** — a debit and a credit. This is double-entry
bookkeeping made explicit. The `transNominalAccountID` field is the key insight: it encodes
the economic nature of the balance (`EXP202` = expense program 202, `REV101` = revenue source 101).

### Balance File (`LDGR*.csv`)
One row per unique combination of:
`(Vendor ID × Legal Entity × Fund × Object × Nominal Account × Currency × Period)`

The amount is the **accumulated sum of all journals for that key**. This is the Instrument Ledger:
the most granular balance store possible. Any summary view (by agency, by fund, by account)
can be derived from it in a single pass.

### VendorMaster (`VendorMaster.csv`)
The **Contract Attributes Record (CAR)**. One row per vendor (instrument). CAR attributes here —
`instTypeID`, `instNIGPClass`, `instGeoRegion` — are the dimensions that Step 5 pivots on.
Adding a column here unlocks all new reporting views at zero marginal storage cost in the LDGR.
That is the Instrument Pivot Theorem.

---

## Running the Pipeline Script

For automated or agentic execution, use `run_pipeline.sh`:

```bash
cd universal_ledger   # repo root

# C1 — Post only (baseline curve)
bash scripts/run_pipeline.sh \
  --inPath data --outPath data/output \
  --years 03 --steps 2,3 --curve C1 --config post-only

# C2 — Post + Analysis (adds reporting materialization)
bash scripts/run_pipeline.sh \
  --inPath data --outPath data/output \
  --years 03 --steps 2,3,5 --curve C2 --config full-views

# C4 — Full pipeline
bash scripts/run_pipeline.sh \
  --inPath data --outPath data/output \
  --years 03 --steps 2,3,5,6,7,8,9,4 --curve C4 --config full
```

Each run appends one row to `data/output/cost_surface.csv`. Run C1, C2, C4 in sequence →
three cost points. The convergence of C3 and C4 is the empirical proof of single-pass universality.

---

## Interactive Mode (original menu)

The interactive menu is preserved for human exploration:

```bash
cd 02-foundation/va_pipeline
sbt run
# Follow the prompts — all 9 steps are available
```

---

## The Two Experiments

### Experiment 1 — The Four-Curve Cost Surface

**What it measures:** How does total pipeline cost change as process configurations are added?

| Curve | Steps | What it shows |
|---|---|---|
| C1 | `2,3` | Baseline: posting only |
| C2 | `2,3,5` | Marginal cost of reporting materialization |
| C3 | `2,3,5,8,9` | Cost of generated-transaction passes (forecast + reclass) |
| C4 | `2,3,5,6,7,8,9,4` | Full pipeline. If C4 ≈ C3, single-pass universality is proved empirically |

Each run writes one row to `data/output/cost_surface.csv`:
- `total_compute_s` — elapsed time
- `total_storage_bytes` — bytes written
- `master_file_count` — enabled=Y rows in ViewSpec.csv (the AHI metric, Paper 1 §5.2)
- `total_cost_proxy` — weighted sum of all three

### Experiment 2 — The Output-Side Pivot Theorem (CAR Attribute Expansion)

**What it measures:** As CAR attributes are added to VendorMaster one at a time, how many new
audited reporting views become answerable — and what would the equivalent key-embedded cost be?

Edit `data/ViewSpec.csv` to uncomment successive attribute rounds, then run Step 5:

| Round | Attribute | Views unlocked |
|---|---|---|
| 0 | `instTypeID` | EXP vs. REV split |
| 1 | `instNIGPClass` | Spend by commodity class × all existing dims |
| 2 | `instGeoRegion` | Regional breakdowns × all above |
| 3 | `instContractType` | Procurement channel × all above |

Each run appends rows to `data/output/pivot_results.csv`. The `ratio` column
(`key_embedded_equivalent_balances / views_answerable`) grows exponentially. That ratio IS
the Instrument Pivot Theorem on public data.

---

## Agentic Execution

Both experiments can be driven by an AI agent. The agent interface is intentionally simple:

1. **Edit** `data/ViewSpec.csv` — set `enabled=Y/N` flags or uncomment SAL rounds
2. **Run** `bash scripts/run_pipeline.sh` from the repository root with chosen `--curve` and `--steps`
3. **Read** `data/output/cost_surface.csv` and `data/output/pivot_results.csv`

No Scala code involvement. Full API contract: **`scripts/AGENT_INTERFACE.md`**

---

## Full VA Dataset

The fixture files in `data/` are small samples. The full 13-year dataset (FY2003–FY2016)
is available from the Virginia Department of Accounts open data portal:

- **Expenditure files:** Quarterly, by fiscal year — `FY{YY}q{Q}exp.txt`
- **PO files:** Annual — `VA_opendata_FY20{YY}.txt`
- **Reference files:** Already in `data/` — Agency, Fund, Object, Program tables

Expected data volume: ~780M records total, ~12M balance rows after posting.
See Paper 4 §6 of the monograph for the benchmark: 2h38m on a MacBook Pro i7.

---

## Pipeline Step Reference

| Step | Scala object | Description | CKB partition inputs |
|---|---|---|---|
| 2 | `standardizeAndSort` | Standardize raw VA data → SortedJE + VendorMaster | A |
| 3 | `post` | Match-merge posting → LDGR balance file | A + B |
| 4 | `contraCreation` | Reconciliation / contra entries — run **LAST** | B |
| 5 | `dataAggregation` | ViewSpec-driven pivot → view files + pivot_results.csv | B + C |
| 6 | `financialAllocation` | Overhead allocation via prior-day divisor | A + B + C |
| 7 | `consolidation` | Agency consolidation + interagency elimination | B + C |
| 8 | `forecastingBudgeting` | Actuals × growth factors → budget projection | B + C |
| 9 | `arrangementReclass` | VendorMaster reclass → new balance events — run before 4,5 | B + C |

**Partition legend:** A = transaction history (`SortedJE*.csv`), B = balance history (`LDGR*.csv`),
C = attribute history (`VendorMaster.csv`). Steps 8 and 9 have no A input — they are
*revaluation-class* processes that generate new transactions from B and C only.

---

## Relationship to the Monograph

| Pipeline | Monograph | Claim demonstrated |
|---|---|---|
| C1 cost curve | Paper 1 §§3.2, 3.2a | Interior minimum on the posting-only cost curve |
| C1 vs C4 engine comparison | Paper 1 §3.2a | Engine-specific minimum — Scala vs. Spark |
| C3 ≈ C4 equality | Paper 1 §3.2 | Single-pass universality across all nine FSPs |
| Experiment 2 `ratio` | Paper 1 §3.4 | Instrument Pivot Theorem: O(v^m) views at O(1) balance cost |
| `pivot_results.csv` | Paper 5 §§5.0a, 5.1 | Step-up join as operational mechanism |
| All runs (public VA data) | Paper 3 §3.3 | Replicable controlled benchmark |

---

## Learning Path

| If you want to... | Start here |
|---|---|
| Run the core pipeline in 2 commands | The **30-Second Run** section above |
| Drive both experiments automatically | `scripts/AGENT_INTERFACE.md` |
| Understand the posting algorithm | Read `post.scala` — the 3-case match-merge loop |
| Understand the Pivot Theorem empirically | Run Experiment 2; watch the `ratio` column grow |
| Understand the minimum cost curve | Run Experiments 1 (C1→C4); read `cost_surface.csv` |
| Add a new Financial System Pattern | Read `docs/universal-ledger-DESIGN.md` §Adding a New Option |
| Understand all design decisions | Read `PROJECT_BRIEF.md` |

---

*Apache-2.0 License. Contributions welcome — see `README.md` for sign-off requirements.*
