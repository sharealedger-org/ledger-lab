# Full Corpus Run Summary: 20261116_061557

- **Run Tag / Timestamp**: `20261116_061557`
- **Source Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/phase2_unchanged_baseline_v1/output`
- **Logs Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/phase2_unchanged_baseline_v1/logs/financial`
- **Archived Date**: 2026-11-16 06:18:44

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261116_061558_46943","sje_balance","fixture","SortedJEFY03merged","0","-0.00","0.00","30","PASS",""
"run_20261116_061658_49446","sje_balance","fixture","SortedJEFY03merged","0","-0.00","0.00","30","PASS",""
"run_20261116_061658_49446","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","10","PASS",""
"run_20261116_061651_49336","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","30","PASS",""
"run_20261116_061759_51426","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","30","PASS",""
"run_20261116_061759_51426","ledger_balance","fixture","ledger_fy2004","0","0.00","0.00","16","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261116_061558_46943","2026-11-16 06:15:58","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",51,24375,0,51.02,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261116_061658_49446","2026-11-16 06:16:58","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",45,37580,0,45.04,"CCP1","monthly","unspecified",10,2051,0,10,36
"run_20261116_061651_49336","2026-11-16 06:16:51","CCP2","fullcorpus-prep-B-fy04","04","ttl,2p,2m",45,24463,0,45.02,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261116_061759_51426","2026-11-16 06:18:00","CCP1","fullcorpus-run2-chain-fy04","04","3,20,21",34,39185,0,34.04,"CCP1","monthly","unspecified",16,2873,0,16,36
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
corpus_start,1794834957,2026-11-16 06:15:57
# years=03,04 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/phase2_unchanged_baseline_v1/inputs/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=38a41c6 dirty_files=15
# host=Kips-MBP cpus=4
chain_start_fy03,1794835018,2026-11-16 06:16:58
prep_all_done,1794835060,2026-11-16 06:17:40
views_start,1794835068,2026-11-16 06:17:48
chain_end_fy03,1794835079,2026-11-16 06:17:59
chain_start_fy04,1794835079,2026-11-16 06:17:59
views_end,1794835084,2026-11-16 06:18:04
chain_end_fy04,1794835124,2026-11-16 06:18:44
chain_all_done,1794835124,2026-11-16 06:18:44
views_all_done,1794835124,2026-11-16 06:18:44
corpus_end,1794835124,2026-11-16 06:18:44
```
