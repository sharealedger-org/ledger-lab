# Full Corpus Run Summary: 20261113_141827

- **Run Tag / Timestamp**: `20261113_141827`
- **Source Root**: `/private/tmp/ckb-queue-rehearsal-20261113/repeat_b/output`
- **Logs Root**: `/private/tmp/ckb-queue-rehearsal-20261113/repeat_b/logs`
- **Archived Date**: 2026-11-13 14:20:48

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261113_141928_82711","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","4","PASS",""
"run_20261113_141958_83817","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","4","PASS",""
"run_20261113_141958_83817","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","4","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261113_141928_82711","2026-11-13 14:19:28","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",24,4889,0,24.00,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261113_141958_83817","2026-11-13 14:19:58","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",41,16322,0,41.02,"CCP1","monthly","unspecified",4,1103,0,4,4
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY03,POQuarantineFY03.csv,0
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794604707,2026-11-13 14:18:27
# years=03 lanes=4 periodGranularity=monthly
# ttl_type=B vendor_master=/private/tmp/ckb-queue-rehearsal-20261113/vendor_master.csv
# chain_width=2 views_early=Y caffeinate=on
# git=bdeaad4 dirty_files=1
# host=MacBookAir cpus=8
prep_all_done,1794604793,2026-11-13 14:19:53
views_start,1794604798,2026-11-13 14:19:58
chain_start_fy03,1794604798,2026-11-13 14:19:58
views_end,1794604806,2026-11-13 14:20:06
chain_end_fy03,1794604848,2026-11-13 14:20:48
chain_all_done,1794604848,2026-11-13 14:20:48
views_all_done,1794604848,2026-11-13 14:20:48
corpus_end,1794604848,2026-11-13 14:20:48
```
