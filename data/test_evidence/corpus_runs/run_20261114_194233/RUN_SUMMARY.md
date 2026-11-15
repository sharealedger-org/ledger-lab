# Full Corpus Run Summary: 20261114_194233

- **Run Tag / Timestamp**: `20261114_194233`
- **Source Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/identity_recovery_v1/output`
- **Logs Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/identity_recovery_v1/logs`
- **Archived Date**: 2026-11-14 19:44:56

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261114_194233_15672","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","18","PASS",""
"run_20261114_194344_17398","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","18","PASS",""
"run_20261114_194344_17398","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","13","PASS",""
"run_20261114_194337_17289","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","3","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261114_194233_15672","2026-11-14 19:42:33","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",62,11686,0,62.01,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261114_194344_17398","2026-11-14 19:43:44","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",48,24999,0,48.02,"CCP1","monthly","unspecified",13,2351,0,13,157
"run_20261114_194337_17289","2026-11-14 19:43:37","CCP2","fullcorpus-prep-B-fy04","04","ttl,2p,2m",41,4169,0,41.00,"CCP2","monthly","unspecified",0,0,0,0,0
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY03,POQuarantineFY03.csv,0
FY04,POQuarantineFY04.csv,0
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794710553,2026-11-14 19:42:33
# years=03,04 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/Users/ktwitchell001/workspace/sharealedger/ledger-lab/data/synthetic/identity_recovery_v1/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=4cd063c dirty_files=10
# host=Kips-MBP cpus=4
chain_start_fy03,1794710623,2026-11-14 19:43:44
prep_all_done,1794710662,2026-11-14 19:44:22
views_start,1794710664,2026-11-14 19:44:24
views_end,1794710682,2026-11-14 19:44:42
chain_end_fy03,1794710686,2026-11-14 19:44:46
chain_start_fy04,1794710687,2026-11-14 19:44:47
chain_end_fy04,1794710695,2026-11-14 19:44:55
chain_all_done,1794710695,2026-11-14 19:44:55
views_all_done,1794710695,2026-11-14 19:44:55
corpus_end_FAILED,1794710695,2026-11-14 19:44:55
```
