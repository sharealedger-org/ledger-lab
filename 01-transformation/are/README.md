# Accounting Rules Engine (ARE)

The Accounting Rules Engine is a Foundation component. It translates normalized source/product events into canonical Universal Journal entries (SJEs) before CKB sorting and posting.

## Current VA Implementation

The ARE is currently embedded in:

```text
02-foundation/va_pipeline/.../standardizeAndSort.scala
```

That module currently performs several different responsibilities in one execution path:

1. Parse VA source formats.
2. Resolve vendor name to Instrument ID through the CAR.
3. Apply VA accounting mappings.
4. Generate balanced debit/credit SJE lines.
5. Populate Universal Journal fields.
6. Externally sort the generated SJEs for CKB.

The long-term design should expose these as separate boundaries even if they remain one optimized pass in the implementation.

## Conceptual Flow

```text
Raw VA/product event
        |
        v
Technical Transform Layer (TTL)
  parse, normalize, validate source bytes
        |
        v
Accounting Rules Engine (ARE)
  classify event, resolve references, derive accounts,
  generate balanced debit/credit journal lines
        |
        v
Universal Journal / SJE audit trail
        |
        v
External sort contract
        |
        v
CKB posting into Instrument Ledger
```

The raw VA expenditure and revenue files are source/product transactions. `VendorMaster` is the CAR that supplies Instrument ID and current/effective attributes. The SJE is the ARE output, not the raw transaction itself.

## ARE Responsibilities

The ARE owns accounting meaning:

- event classification;
- source-system and business-event mapping;
- Instrument ID assignment or validation;
- legal-entity and chartfield assignment;
- nominal-account derivation;
- debit/credit perspective shift;
- offset and clearing-leg generation;
- book-code and currency treatment;
- accounting-date and fiscal-period assignment;
- rule, rule-set, and source lineage identifiers;
- journal-level balancing and accounting rejects.

The ARE must emit balanced journal groups. A source event may produce two or more SJE lines, but the group must carry a stable business-event/journal ID and sum to zero where the accounting contract requires it.

## What Does Not Belong in the ARE

- Raw byte parsing and transport handling belong to the Technical Transform Layer.
- External merge-sort belongs to the Foundation sort/CKB substrate.
- History-dependent FTP, reserves, revaluation, and allocation calculations belong to the Transformation Layer.
- Finance, Tax, MIS, Risk, and Liquidity policy belongs to the Perspective Layer, although those policies may provide rule specifications to a calculation engine.

## Today-Input Boundary

ARE-generated entries and stored perspectives depend on the current source event plus current effective reference data. They can be generated during today's ingestion/normalization pass without scanning historical ledger state.

This is distinct from history-dependent calculation engines:

```text
ARE:
  today's source event + current rules/reference data
    -> SJE and immediate derived accounting lines

Calculation engine:
  prior balances/history + current rules/reference data
    -> FTP, reserves, revaluation, allocations, or other generated events
```

Both outputs are stored as auditable events when they affect ledger state. Their difference is the input contract and cost model, not whether the output is calculated.

## Parameter-Driven Rule Contract

The current VA mappings are hard-coded in `standardizeAndSort.scala`, including values such as:

- `EXP{program}` and `REV{source}` nominal-account construction;
- `0000` offset account;
- `ACTUALS` ledger;
- `FIN` journal type;
- `SHRD-3RD-PARTY` book code;
- USD source/target currency defaults;
- debit/offset flags;
- source-system descriptions.

These should move behind a versioned ARE specification. The first implementation can be CSV or another simple declarative format, but it must identify:

- `rule_set_id` and `rule_version`;
- source system and input record type;
- business event code;
- event classification;
- debit account expression;
- credit/offset account expression;
- amount/sign expression;
- legal entity, book, currency, and period rules;
- required CAR lookups;
- reject behavior;
- effective start and end dates.

Expressions such as `EXP + program` must be represented as rule data or named rule operations, not hidden in the Scala branch logic. Procedural rules remain possible, but their identity and version must be explicit.

## Planned API Boundary

The eventual Scala boundary should resemble:

```text
AccountingRulesEngine.translate(
  sourceEvent,
  effectiveReferences,
  areRuleSet
) -> Seq[Transaction]
```

The returned transactions are canonical SJEs. Sorting and CKB posting consume them without knowing whether they came from VA expenditure, VA revenue, or another source adapter.

## Controls

Each ARE output must support:

- source event ID;
- generated journal/business-event ID;
- journal line number;
- rule-set and rule ID/version;
- source system ID;
- originating event ID for generated lines;
- balanced group control total;
- accepted, rejected, and recycled counts;
- deterministic replay from the same source and rule version.

## Migration Sequence

1. Preserve current VA output and control totals.
2. Extract the ARE field contract from `standardizeAndSort.scala`.
3. Create a versioned VA rule specification matching current behavior.
4. Separate source parsing, ARE translation, and sort invocation at the Scala API boundary.
5. Add rule-driven tests that compare old and new SJE outputs.
6. Add reject/recycle handling.
7. Add additional source/product adapters without changing the CKB posting engine.

The ARE is therefore identified now as part of Foundation, but its policy is made configurable before new domain perspectives are implemented.
