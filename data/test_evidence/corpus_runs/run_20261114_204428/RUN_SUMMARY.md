# Full Corpus Run Summary: 20261114_204428

- **Run Tag / Timestamp**: `20261114_204428`
- **Source Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/identity_recovery_v1_final/output`
- **Logs Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/identity_recovery_v1_final/logs`
- **Archived Date**: 2026-11-14 20:47:03

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261114_204429_20910","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","18","PASS",""
"run_20261114_204509_21967","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","18","PASS",""
"run_20261114_204509_21967","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","13","PASS",""
"run_20261114_204501_21862","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","2","PASS",""
"run_20261114_204620_24294","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","2","PASS",""
"run_20261114_204620_24294","ledger_balance","fixture","ledger_fy2004","0","0.00","0.00","2","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261114_204429_20910","2026-11-14 20:44:29","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",31,11786,0,31.01,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261114_204509_21967","2026-11-14 20:45:09","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",50,25141,0,50.03,"CCP1","monthly","unspecified",13,2351,0,13,157
"run_20261114_204501_21862","2026-11-14 20:45:01","CCP2","fullcorpus-prep-B-fy04","04","ttl,2p,2m",48,4138,0,48.00,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261114_204620_24294","2026-11-14 20:46:20","CCP1","fullcorpus-run2-chain-fy04","04","3,20,21",30,15428,0,30.02,"CCP1","monthly","unspecified",2,811,0,2,2
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
corpus_start,1794714268,2026-11-14 20:44:28
# years=03,04 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/Users/ktwitchell001/workspace/sharealedger/ledger-lab/data/synthetic/identity_recovery_v1/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=4cd063c dirty_files=16
# host=Kips-MBP cpus=4
chain_start_fy03,1794714309,2026-11-14 20:45:09
prep_all_done,1794714352,2026-11-14 20:45:52
views_start,1794714359,2026-11-14 20:45:59
views_end,1794714379,2026-11-14 20:46:19
chain_end_fy03,1794714380,2026-11-14 20:46:20
chain_start_fy04,1794714380,2026-11-14 20:46:20
chain_end_fy04,1794714423,2026-11-14 20:47:03
chain_all_done,1794714423,2026-11-14 20:47:03
views_all_done,1794714423,2026-11-14 20:47:03
corpus_end,1794714423,2026-11-14 20:47:03
```
