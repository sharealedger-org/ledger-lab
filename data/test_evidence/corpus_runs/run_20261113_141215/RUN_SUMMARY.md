# Full Corpus Run Summary: 20261113_141215

- **Run Tag / Timestamp**: `20261113_141215`
- **Source Root**: `/private/tmp/ckb-connected-smoke/full-output`
- **Logs Root**: `/private/tmp/ckb-connected-smoke/full-logs`
- **Archived Date**: 2026-11-13 14:13:39

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261113_141215_74947","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","4","PASS",""
"run_20261113_141245_76014","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","4","PASS",""
"run_20261113_141245_76014","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","4","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261113_141215_74947","2026-11-13 14:12:15","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",24,4853,0,24.00,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261113_141245_76014","2026-11-13 14:12:45","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",45,16195,0,45.02,"CCP1","monthly","unspecified",4,1103,0,4,4
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY03,POQuarantineFY03.csv,0
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794604335,2026-11-13 14:12:15
# years=03 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/private/tmp/ckb-connected-smoke/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=f3943f0 dirty_files=8
# host=MacBookAir cpus=8
prep_all_done,1794604360,2026-11-13 14:12:40
views_start,1794604365,2026-11-13 14:12:45
chain_start_fy03,1794604365,2026-11-13 14:12:45
views_end,1794604374,2026-11-13 14:12:54
chain_end_fy03,1794604419,2026-11-13 14:13:39
chain_all_done,1794604419,2026-11-13 14:13:39
views_all_done,1794604419,2026-11-13 14:13:39
corpus_end,1794604419,2026-11-13 14:13:39
```
