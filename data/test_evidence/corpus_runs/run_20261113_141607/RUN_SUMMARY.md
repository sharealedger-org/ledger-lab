# Full Corpus Run Summary: 20261113_141607

- **Run Tag / Timestamp**: `20261113_141607`
- **Source Root**: `/private/tmp/ckb-queue-rehearsal-20261113/repeat_a/output`
- **Logs Root**: `/private/tmp/ckb-queue-rehearsal-20261113/repeat_a/logs`
- **Archived Date**: 2026-11-13 14:18:27

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261113_141708_79317","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","4","PASS",""
"run_20261113_141738_80437","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","4","PASS",""
"run_20261113_141738_80437","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","4","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261113_141708_79317","2026-11-13 14:17:08","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",22,4889,0,22.00,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261113_141738_80437","2026-11-13 14:17:38","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",39,16322,0,39.02,"CCP1","monthly","unspecified",4,1103,0,4,4
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY03,POQuarantineFY03.csv,0
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794604568,2026-11-13 14:16:08
# years=03 lanes=4 periodGranularity=monthly
# ttl_type=B vendor_master=/private/tmp/ckb-queue-rehearsal-20261113/vendor_master.csv
# chain_width=2 views_early=Y caffeinate=on
# git=bdeaad4 dirty_files=0
# host=MacBookAir cpus=8
prep_all_done,1794604654,2026-11-13 14:17:34
views_start,1794604658,2026-11-13 14:17:38
chain_start_fy03,1794604658,2026-11-13 14:17:38
views_end,1794604667,2026-11-13 14:17:47
chain_end_fy03,1794604706,2026-11-13 14:18:26
chain_all_done,1794604706,2026-11-13 14:18:26
views_all_done,1794604706,2026-11-13 14:18:26
corpus_end,1794604706,2026-11-13 14:18:27
```
