# Interpretation Layer

`99-interpretation` is the planned terminal stage of a Ledger Lab run. The run contract is that it
will execute after the financial process topology completes or stops and available evidence is
flushed. It will always be last, but it is not a sixth financial layer and must not imply another
pass over financial records. The interpreter and automatic run hook are not implemented yet.

Its purpose is to turn machine evidence into a reproducible account of what ran, what the results
show, which architectural responsibilities were exercised, and what evidence is still needed.
It reads run evidence; it does not transform, post, generate, apply, or aggregate financial
records.

## Position in a Run

```text
01 Transformation -> 02 Foundation -> 03 Instrument Ledger -> 04 Engines -> 05 Perspectives
           |                |                |                  |               |
           +----------------+----------------+------------------+---------------+
                            run evidence and artifacts
                                         |
                              completion or failure
                                         v
                              99 Interpretation
```

The numbered 01-05 directories describe logical ownership, not mandatory physical passes. When
implemented, interpretation will follow the actual process topology, which may combine layers in
one CKB flow or split work at declared sort and materialization boundaries. It must also run for
failed or partial executions, reporting incomplete evidence rather than treating missing results as
a pass.

## Interpretation Taxonomy

Do not use legacy runner labels as the conceptual experiment taxonomy. Each planned probe should
have a versioned, plain-language research question and state:

- the capability or architecture claim being examined;
- expected logical-layer responsibilities across 01-05;
- expected physical processes, partitions, sorts, and materialization boundaries;
- required controls, metrics, and output capabilities;
- input snapshot, rules, configuration, and code revision identities.

## Research Question Families

These families organize questions by the claim being tested, not by a fixed sequence of passes. A
run may cover several families, and each must be mapped to the actual processes and partitions that
ran.

| Research question | Logical responsibilities | Required evidence |
|---|---|---|
| Source-event fidelity | `01-transformation`, `02-foundation` | Accepted/rejected source counts, source lineage, balanced SJE groups, rule identity, and input/output partition records. |
| Canonical state and replay | `02-foundation`, `03-instrument-ledger` | Ledger grain and row counts, supporting SJE lineage, replay/reconstruction controls, replacement-master deltas, and relevant sort/pass boundaries. |
| Generated economic events | `02-foundation`, `03-instrument-ledger`, `04-engines` | Engine inputs/outputs, generated group counts, balancing deltas, effective dates, rule versions, lineage coverage, and apply-back controls. |
| Perspective capability | `03-instrument-ledger`, `04-engines` when step-ups apply, `05-perspectives` | Requested and produced capability IDs, view definitions, input/output rows and bytes, attribute/step-up hits and misses, and perspective-to-ledger reconciliation. |
| Cost and capability frontier | All in-scope responsibilities | Comparable run/process resource metrics, sort and materialization costs, storage and spill bytes, control outcomes, enabled capabilities, and replicate identity. |

The evidence columns correspond to the canonical records in
[MEASUREMENT_AND_LOGGING.md](../docs/MEASUREMENT_AND_LOGGING.md). Layer coverage is a semantic
mapping; process, sort, and partition records describe the physical execution. Neither should be
inferred from the other. A legacy VA process that resembles an engine or view is not evidence that
the generalized 04 or 05 implementation ran.

Keep these status dimensions separate:

| Dimension | Example values | Question answered |
|---|---|---|
| Implementation maturity | `implemented`, `partial`, `scaffolded`, `not_implemented` | Does the intended capability exist in the codebase? |
| Run activity | `active`, `zero_work`, `failed`, `not_scheduled` | What happened in this execution? |
| Evidence assessment | `supports`, `contradicts`, `inconclusive`, `not_evaluated` | What conclusion does the evidence justify? |

A zero-work execution is not evidence that the active financial behavior works. An implementation
status is not inferred from a successful run of a similarly named legacy process. Reports must map
actual physical processes and artifacts to logical responsibilities explicitly.

## Inputs

The planned canonical runtime evidence root is `$UL_DATA_ROOT/metrics/`. Once its producers exist,
records will be linked by `run_id`, `process_id`, and `partition_id`:

- `run_manifest.csv` for the planned point, inputs, configuration identities, software versions,
  timing, and overall status;
- `process_metrics.csv` and `sort_events.csv` for process boundaries, resource use, ordering,
  spills, and physical pass evidence;
- `partition_catalog.csv` for artifact lineage, size, row counts, checksums, and sort contracts;
- `sje_engine_metrics.csv` and `perspective_metrics.csv` for generated-event and aggregation work;
- `reconciliation_results.csv` for control outcomes and deltas.

The interpreter also needs versioned experiment definitions and a code-scope map that relates
implemented processes to 01-05 responsibilities and maturity. Existing compatibility CSVs may be
read during migration, but `pipeline_results.log` is diagnostic text and must not be the sole
evidence source. See [the measurement contract](../docs/MEASUREMENT_AND_LOGGING.md).

## Outputs

For each `run_id`, the interpreter should produce:

- a machine-readable interpretation record with evidence references and status dimensions;
- a human-readable run report covering intent, actual execution, layer coverage, controls,
  measured costs, interpretation, limitations, and next evidence to collect.

Every conclusion must point back to source metric rows or partition/control IDs. Keep financial
engine cost separate from orchestration and interpretation overhead; record the interpreter as a
post-run process rather than adding it to measured financial compute.

## Build Order

1. Define the experiment, evidence, and implementation-scope schemas.
2. Map current physical VA processes to 01-05, marking mixed responsibilities and unfinished
   target implementations explicitly.
3. Emit and validate the canonical manifest, process, sort, partition, engine, perspective, and
   reconciliation records.
4. Build a post-run interpreter that always emits a report, including for failed or partial runs.
5. Test it against small, labeled metric bundles with expected interpretations, including no-op
   stages, missing evidence, failed controls, and successful active work.

The checked-in VA fixtures are for functional and control-flow evidence. They do not establish
production-scale cost behavior or complete implementation of every 01-05 responsibility.

## Proposed CKB View-Query Prototype

The short-term research plan for an AI-facing, CKB-backed view-request prototype is in
[CKB_VIEW_QUERY_PROTOTYPE.md](CKB_VIEW_QUERY_PROTOTYPE.md). It is a proposal, not an implemented
99 capability. The prototype keeps 99 as the user interaction and evidence layer; any record scan
or aggregation must run as an explicit, measured CKB/view process rather than as a hidden 99 pass.
