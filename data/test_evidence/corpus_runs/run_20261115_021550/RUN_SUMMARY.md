# Full Corpus Run Summary: 20261115_021550

- **Run Tag / Timestamp**: `20261115_021550`
- **Source Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/component_pilot_0304_20261115_v1/output`
- **Logs Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/component_pilot_0304_20261115_v1/logs/financial`
- **Archived Date**: 2026-11-15 02:18:42

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261115_021550_44321","sje_balance","fixture","SortedJEFY03merged","0","-0.00","0.00","30","PASS",""
"run_20261115_021630_45376","sje_balance","fixture","SortedJEFY03merged","0","-0.00","0.00","30","PASS",""
"run_20261115_021630_45376","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","10","PASS",""
"run_20261115_021624_45273","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","30","PASS",""
"run_20261115_021746_47465","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","30","PASS",""
"run_20261115_021746_47465","ledger_balance","fixture","ledger_fy2004","0","0.00","0.00","16","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261115_021550_44321","2026-11-15 02:15:50","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",34,18207,0,34.02,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261115_021630_45376","2026-11-15 02:16:30","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",58,31367,0,58.03,"CCP1","monthly","unspecified",10,2051,0,10,34
"run_20261115_021624_45273","2026-11-15 02:16:24","CCP2","fullcorpus-prep-B-fy04","04","ttl,2p,2m",57,18295,0,57.02,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261115_021746_47465","2026-11-15 02:17:46","CCP1","fullcorpus-run2-chain-fy04","04","3,20,21",43,32969,0,43.03,"CCP1","monthly","unspecified",16,2873,0,16,40
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
corpus_start,1794734150,2026-11-15 02:15:50
# years=03,04 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/component_pilot_0304_20261115_v1/inputs/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=62fadf2 dirty_files=15
# host=Kips-MBP cpus=4
chain_start_fy03,1794734190,2026-11-15 02:16:30
prep_all_done,1794734245,2026-11-15 02:17:25
views_start,1794734250,2026-11-15 02:17:30
chain_end_fy03,1794734265,2026-11-15 02:17:46
chain_start_fy04,1794734266,2026-11-15 02:17:46
views_end,1794734281,2026-11-15 02:18:01
chain_end_fy04,1794734321,2026-11-15 02:18:41
chain_all_done,1794734321,2026-11-15 02:18:41
views_all_done,1794734321,2026-11-15 02:18:41
corpus_end,1794734321,2026-11-15 02:18:41
```
