# Step 5 — Data Aggregation: Postgres Query Engine Design

**Status:** Design proposal. Implementation deferred pending decisions in the open-questions section.
**Proposed replacement for:** Current [`dataAggregation.scala`](../02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/dataAggregation.scala) in-memory prototype. The PostgreSQL path is not implemented.
**Technology Invariant:** Postgres is infrastructure (like `SortEngine`). All financial logic — CAR join, permutation cost formula, `pivot_results.csv` write — remains in Scala. This does not violate the invariant.

---

## Problem Statement

The current `dataAggregation.scala` has two violations of the CKB single-pass invariant:

1. **O(LDGR_rows) in-memory load** — the entire LDGR is loaded into an `ArrayBuffer[LdgrRow]` before any view is processed. At 581,744 rows for FY2003, this fits in memory. At 14 years × ~500K rows = ~7M rows, it will not.

2. **Multi-pass iteration** — the `allRows` buffer is iterated once per enabled ViewSpec view. For 9 views that is 9 full passes. For Round 3 of Experiment 2 (8+ views) across 14 LDGR files, this is O(views × years × LDGR_rows).

Both violations were accepted as prototype limitations. They must be resolved before running the full historical workload at scale.

---

## Architecture

The redesign splits Step 5 into two layers with a clean interface contract between them.

```
┌──────────────────────────────────────────────────────────────┐
│  PostgreSQL — Selection & Aggregation Layer                  │
│                                                              │
│  Holds LDGR as a table (loaded once per year at Step 5       │
│  start, or at Step 3 completion).                            │
│                                                              │
│  Per ViewSpec row, Scala generates and executes:             │
│    SELECT {group_by_ldgr}, SUM(ldgrTransAmount)              │
│    FROM ldgr_{year}                                          │
│    [WHERE {filterLdgrCol} = '{filterLdgrVal}']               │
│    GROUP BY {group_by_ldgr}                                  │
│    ORDER BY {instID fields first, then period}  ← CONTRACT   │
│                                                              │
│  This is the GenevaERS selection model: field-level          │
│  filtering + grouping, executed natively by the DB.          │
└───────────────────────────┬──────────────────────────────────┘
                            │  sorted cursor stream
                            │  ORDER BY (instID, ldgrLedgerPeriod)
                            ▼
┌──────────────────────────────────────────────────────────────┐
│  Scala Step 5 Engine — Date-Effective CAR Join Layer         │
│                                                              │
│  Reads Postgres cursor as a stream (JDBC fetchSize=1000).    │
│                                                              │
│  If groupBySal is empty:                                     │
│    Write cursor rows directly to view output CSV.            │
│                                                              │
│  If groupBySal is non-empty:                                 │
│    Two-pointer walk over VendorMaster.csv                    │
│    (sorted instID + instEffectDate — same sort as cursor).   │
│    For each cursor row:                                       │
│      Advance CAR pointer to instID + period.                 │
│      Resolve as-of attribute (instEffectDate ≤ period        │
│        ≤ instEffectEndDate).                                  │
│      Apply filter_sal if present.                            │
│      Accumulate into per-view map keyed on CAR attributes.   │
│    Flush accumulated map to output CSV.                       │
│                                                              │
│  O(1) memory. Single pass per view. CKB invariant holds.    │
└──────────────────────────────────────────────────────────────┘
```

### Why the split is at this boundary

**Selection goes to Postgres** because `filterLdgrCol`/`filterLdgrVal` and `group_by_ldgr` dimensions are exactly what SQL `WHERE` + `GROUP BY` + `ORDER BY` does natively, with index support. The DB executes this without loading anything into JVM heap.

**The CAR join stays in Scala** because the date-effective join (`instEffectDate ≤ ldgrLedgerPeriod ≤ instEffectEndDate`) requires resolving a VendorMaster **history file** — multiple rows per instID with non-overlapping date ranges. SQL can express this with a lateral join, but it would re-read the CAR per LDGR row and lose the O(1) single-pass guarantee. The CKB pattern — both files sorted on the same key, two-pointer walk, one pass — is only achievable in Scala when Postgres guarantees the sort order of the cursor it emits.

---

## ViewSpec Column Mapping

Each column in `ViewSpec.csv` maps to exactly one layer:

| ViewSpec column | Layer | Mechanism |
|---|---|---|
| `group_by_ldgr` | Postgres | `SELECT ... GROUP BY {fields}` |
| `filter_ldgr_col` / `filter_ldgr_val` | Postgres | `WHERE {col} = '{val}'` |
| `group_by_sal` | Scala | Group by resolved CAR attribute after two-pointer join |
| `filter_sal_col` / `filter_sal_val` | Scala | Post-join filter on resolved CAR attribute |
| `top_n` | Scala | After accumulation, sort descending by amount, take top N |
| `sal_attr_count` | Scala | Input to permutation cost formula (unchanged) |
| `enabled` | Both | View skipped entirely if N |

---

## Sort Order Contract (Layer Interface)

The interface between Postgres and Scala is an ordered cursor. The contract:

> Postgres `ORDER BY` must lead with the fields that drive the CAR two-pointer walk:  
> `ORDER BY ldgrIPID ASC, ldgrLedgerPeriod ASC`

This matches `VendorMaster.sortspec` (`instID ASC, instEffectDate ASC`). The two-pointer advances monotonically on both sides. No backtracking, no buffering.

If a view has no `group_by_sal` dimensions, the `ORDER BY` is still emitted (it costs nothing and keeps the interface uniform).

---

## Component Design

### 1. LDGR Load (once per year)

```
Scala calls JDBC:
  COPY ldgr_{year} FROM '{outPath}/LDGR{year}.csv' CSV HEADER
```

The table schema mirrors `LdgrCols` exactly (25 columns). The table name carries the year.  
An index on `(ldgrIPID, ldgrLedgerPeriod)` supports the `ORDER BY` contract at query time.

### 2. Per-View Query Loop

For each enabled `ViewDef`, Scala generates a SQL string from the ViewDef fields:

```sql
SELECT {group_by_ldgr fields joined by ", "},
       SUM(ldgrTransAmount) AS totalAmount
FROM   ldgr_{year}
[WHERE {filterLdgrCol} = '{filterLdgrVal}']
GROUP BY {group_by_ldgr fields joined by ", "}
ORDER BY ldgrIPID ASC, ldgrLedgerPeriod ASC
```

This SQL is generated at runtime — no hardcoded queries. Adding a new view requires only a new `ViewSpec.csv` row.

### 3. Scala Cursor Consumer

For views with no `group_by_sal`:
- Stream JDBC `ResultSet` row by row (`fetchSize = 1000`)
- Write each row directly to `{view_name}_{year}.csv`
- O(1) memory

For views with `group_by_sal`:
- Open `VendorMaster.csv` as a sorted flat-file reader
- Two-pointer walk (same pattern as `arrangementReclass.scala`'s streaming CAR walk)
- Accumulate into `mutable.Map[String, BigDecimal]` keyed on resolved CAR attributes
- Map size is bounded by the number of distinct CAR attribute combinations — not by LDGR row count
- At end of cursor: sort map, emit to output CSV

### 4. TOP-N Handling

When `topN > 0`, the sort order for output (descending by amount) conflicts with the sort order needed for the CAR walk (ascending by instID). The accumulation map approach is used:
- Accumulate the full view into the map (same as the CAR join case)
- Sort map descending by value, take top N, emit

The map size is bounded by distinct (instID × nominal account) combinations — small relative to raw LDGR row count. This is acceptable and was acceptable in the prior design for the same reason.

### 5. Pivot Results Log

The canonical ledger row count comes from a single Postgres query:

```sql
SELECT COUNT(*) FROM ldgr_{year}
```

This replaces the current in-memory `allRows.size` count. Other fields in `pivot_results.csv` — `key_embedded_equivalent_balances`, `rebuild_cost_ratio`, `recon_obligations_*` — remain modeled calculations in Scala; they are not measurements of a separately executed key-embedded system.

---

## What This Solves

| Problem | Resolution |
|---|---|
| O(LDGR_rows) JVM heap load | Postgres holds LDGR; Scala streams cursor row-by-row |
| Multi-pass (N passes for N views) | One Postgres query per view (each is a single DB pass); no LDGR held in Scala memory between views |
| Date-effective CAR join correctness | Two-pointer walk in Scala, same pattern as `arrangementReclass.scala` |
| Technology Invariant | Financial logic (CAR join, permutation formula, CSV output) stays in Scala; Postgres is infrastructure |
| ViewSpec-driven (no Scala changes for new views) | SQL generated from ViewSpec at runtime |
| CKB single-pass for CAR join | Cursor `ORDER BY` guarantees sort contract; two-pointer is O(1) |
| 14-year cumulative Curve A count | `SELECT COUNT(*) FROM ldgr_{year}` — free in Postgres |

---

## Open Questions (to resolve before implementation)

These decisions remain open and must be resolved and recorded here before implementation begins.

**Q1 — Postgres as a required dependency**  
Is Postgres already in the target environment, or would it be a new service dependency for the attribute-derived reporting workload? If new, should it be required for that workload only, while compilation and ledger posting remain database-free? Should a CSV-only aggregation fallback remain supported?

**Q2 — LDGR table lifetime**  
Should `ldgr_{year}` tables persist across sessions (Step 3 loads, Step 5 queries, tables remain for re-runs), or should Step 5 always reload from CSV on each run? Persistent tables save reload time on re-runs but introduce a CSV-vs-table sync question if LDGR files are regenerated. Reload-always keeps Postgres stateless.

**Q3 — Single table vs. per-year tables**  
For the 14-year cumulative Curve A measurement, a single `ldgr` table partitioned by `fiscal_year` is cleaner (one `COUNT(*)` across all years). But it couples Step 3's output write to a shared table rather than isolated year files. Per-year tables (`ldgr_2003`, `ldgr_2004`, …) keep the year-file isolation of the current design. Preference?

**Q4 — JDBC driver and runtime configuration**
The VA subproject currently declares `org.postgresql:postgresql:42.7.4` in `build.sbt`. The earlier note that the driver is absent is stale. No current aggregation code uses the driver. Before implementation, verify the supported server/runtime versions and define how connection settings are supplied without embedding credentials.

---

## Files Affected

| File | Change |
|---|---|
| `02-foundation/va_pipeline/src/main/scala/.../dataAggregation.scala` | Rewrite — replace ArrayBuffer load + multi-pass with Postgres query + streaming cursor |
| `02-foundation/va_pipeline/build.sbt` | Add PostgreSQL JDBC dependency |
| `data/ViewSpec.csv` | No change — remains the declarative view spec |
| `data/VendorMaster.csv` | No change — remains the flat-file CAR for the two-pointer join |
| `QUICKSTART.md` | Document PostgreSQL prerequisites only if the implementation makes the service required for a supported workload. |
| `docs/universal-ledger-DESIGN.md` | Update Step 5 description |

---

*Design session: current Bob session. Implementation: next pipeline session after open questions resolved.*  
*See also: `BOB_WORK_LOG.md` for session history and open items.*
