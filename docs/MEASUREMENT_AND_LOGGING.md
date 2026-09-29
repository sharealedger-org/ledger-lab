# Measurement and Logging Contract

This document defines the canonical evidence produced by every Ledger Lab experiment run.

The purpose is to measure the physical execution cost of the Scala engine topology while keeping logical engine work, orchestration, sorting, storage, and reconciliation separately identifiable.

## Measurement Principles

- Code-generation speed is not an engine metric.
- The measured financial path remains Scala.
- Bash records orchestration and operating-system process metrics.
- Scala records financial row counts, control totals, SJE lineage, and partition semantics.
- Python may inspect, transform, or summarize logs after a run, but does not produce mainline financial results.
- A logical layer is not a physical pass. Sort and required materialization boundaries must be recorded explicitly.
- Every record is associated with one immutable `run_id`.

## Canonical Metrics Root

Runtime metrics belong under the external runtime data root:

```text
$UL_DATA_ROOT/metrics/
```

The existing `cost_surface.csv`, `pipeline_log.csv`, and `pivot_results.csv` files remain compatibility outputs during migration. They should eventually be derived from the detailed ledgers below rather than serving as the only evidence.

## Run Manifest

`run_manifest.csv` contains one row per execution:

```text
run_id,experiment_id,experiment_version,point_id,curve,configuration_hash,
rules_hash,viewspec_hash,input_snapshot_id,git_revision,engine_version,
host_id,os_version,java_version,scala_version,start_time,end_time,
wall_seconds,status,replicate_number
```

The manifest makes every result reproducible. Configuration files, rules, ViewSpec, and input snapshots must be copied or content-addressed by the recorded hashes.

## Process Metrics

`process_metrics.csv` contains one row per compiled Scala process or orchestration process:

```text
run_id,process_id,parent_process_id,layer,process_name,engine_name,
input_partition_ids,output_partition_ids,ckb_process_id,pass_id,
start_time,end_time,wall_seconds,cpu_user_seconds,cpu_system_seconds,
cpu_total_seconds,peak_rss_bytes,peak_jvm_heap_bytes,pageins,
context_switches,input_rows,input_bytes,output_rows,output_bytes,
spill_bytes,status
```

CPU, RSS, page, and context-switch values are process-level operating-system metrics. Financial row and byte counts are emitted by the Scala process.

## Sort Events

`sort_events.csv` contains one row per actual external sort:

```text
run_id,sort_id,process_id,pass_id,input_partition_id,output_partition_id,
sort_spec_id,sort_spec_hash,sort_key,input_rows,output_rows,input_bytes,
output_bytes,chunk_size,spill_file_count,spill_bytes,peak_memory_bytes,
elapsed_seconds,status
```

This is the authoritative record of physical pass boundaries.

## Partition Catalog

`partition_catalog.csv` contains one row for every significant file or stream artifact:

```text
run_id,partition_id,partition_type,logical_layer,producer_process_id,
parent_partition_ids,path,row_count,byte_count,checksum,sorted,sort_spec_id,
first_key,last_key,min_period,max_period,created_at
```

Partition types include:

```text
raw_source
sje
car
ledger_master
generated_sje
elimination_candidate
replacement_master
perspective_extract
pivot
allocation_divisor
allocation_driver
reconciliation
```

## SJE Engine Metrics

`sje_engine_metrics.csv` contains one row per engine invocation:

```text
run_id,engine_id,engine_version,process_id,input_partition_id,
output_partition_id,engine_class,requires_history,source_sje_rows,
generated_sje_rows,generated_journal_groups,balanced_groups,
unbalanced_groups,amount_delta,lineage_coverage,rule_set_id,rule_version,
elapsed_seconds,status
```

Initial engine classes include:

```text
ARE
RECLASS
CURRENCY_CONVERSION
REVALUATION
FTP
RESERVE
ALLOCATION_DIVISOR
ALLOCATION
CONSOLIDATION
ELIMINATION
```

## Perspective Metrics

`perspective_metrics.csv` contains one row per aggregation output:

```text
run_id,perspective_id,view_id,view_spec_version,process_id,
input_partition_id,output_partition_id,filter_definition,
step_up_definition,grouping_definition,input_rows,output_rows,
step_up_hits,step_up_misses,aggregation_state_peak,spill_bytes,
output_bytes,reconciliation_obligation,capability_id,elapsed_seconds,status
```

Perspectives aggregate only. These metrics describe output capability and aggregation cost; they do not describe generated accounting events.

## Reconciliation Results

`reconciliation_results.csv` contains one row per control:

```text
run_id,check_id,scope,partition_id,expected_value,actual_value,
delta,rows_checked,status,failure_reason
```

Required controls include:

- SJE journal-group balance.
- Debit/credit total.
- Ledger reconstruction from supporting SJEs.
- Replacement master versus prior master plus delta.
- Generated-event lineage coverage.
- Shared-core equality across consuming perspectives.
- Perspective totals versus ledger totals.
- Allocation rounding residual.
- Currency conversion and revaluation residual.
- Consolidation and elimination residual.

## Derived Cost Surface

`cost_surface.csv` is a run-level derived summary:

```text
run_id,experiment_id,point_id,curve,materialization_profile,
total_wall_seconds,total_engine_cpu_seconds,total_sort_seconds,
total_perspective_seconds,total_storage_bytes,total_spill_bytes,
source_sje_rows,generated_sje_rows,ledger_rows,pivot_rows,
perspective_rows,physical_pass_count,sort_count,master_file_count,
reconciliation_obligation_count,perspectives_enabled,capabilities_enabled,
total_cost_proxy
```

The raw dimensions remain available. A cost proxy must record its weights and must not be treated as intrinsic economic cost.

## Run Tracking

A planned experiment point and an executed run are different records:

```text
planned point -> point_id -> one or more replicated run_id values
```

Replicates are required to distinguish architecture cost from JVM warmup, filesystem cache, laptop load, and memory pressure.

## Migration From Existing Logs

Current compatibility files:

- `pipeline_log.csv`: compatibility view of `process_metrics.csv`.
- `pivot_results.csv`: compatibility view of `perspective_metrics.csv` for SAL experiments.
- `cost_surface.csv`: compatibility view of the derived run summary.
- `pipeline_results.log`: human-readable console output only, not canonical evidence.

No experiment result is complete unless it has a run manifest, process metrics, partition catalog, and reconciliation results.
