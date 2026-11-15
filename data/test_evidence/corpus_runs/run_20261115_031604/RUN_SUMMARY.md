# Full Corpus Run Summary: 20261115_031604

- **Run Tag / Timestamp**: `20261115_031604`
- **Source Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/component_pilot_0304_20261115_lineage_v2/output`
- **Logs Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/component_pilot_0304_20261115_lineage_v2/logs/financial`
- **Archived Date**: 2026-11-15 03:18:33

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261115_031604_53913","sje_balance","fixture","SortedJEFY03merged","0","-0.00","0.00","30","PASS",""
"run_20261115_031645_54960","sje_balance","fixture","SortedJEFY03merged","0","-0.00","0.00","30","PASS",""
"run_20261115_031645_54960","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","10","PASS",""
"run_20261115_031637_54852","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","30","PASS",""
"run_20261115_031756_57346","sje_balance","fixture","SortedJEFY04merged","0","0.00","0.00","30","PASS",""
"run_20261115_031756_57346","ledger_balance","fixture","ledger_fy2004","0","0.00","0.00","16","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261115_031604_53913","2026-11-15 03:16:05","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",30,24423,0,30.02,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261115_031645_54960","2026-11-15 03:16:45","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",49,37724,0,49.04,"CCP1","monthly","unspecified",10,2051,0,10,36
"run_20261115_031637_54852","2026-11-15 03:16:37","CCP2","fullcorpus-prep-B-fy04","04","ttl,2p,2m",47,24511,0,47.02,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261115_031756_57346","2026-11-15 03:17:56","CCP1","fullcorpus-run2-chain-fy04","04","3,20,21",28,39340,0,28.04,"CCP1","monthly","unspecified",16,2873,0,16,36
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
corpus_start,1794737764,2026-11-15 03:16:04
# years=03,04 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/component_pilot_0304_20261115_lineage_v2/inputs/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=e4fb330 dirty_files=9
# host=Kips-MBP cpus=4
chain_start_fy03,1794737805,2026-11-15 03:16:45
prep_all_done,1794737848,2026-11-15 03:17:28
views_start,1794737855,2026-11-15 03:17:35
views_end,1794737874,2026-11-15 03:17:54
chain_end_fy03,1794737876,2026-11-15 03:17:56
chain_start_fy04,1794737876,2026-11-15 03:17:56
chain_end_fy04,1794737913,2026-11-15 03:18:33
chain_all_done,1794737913,2026-11-15 03:18:33
views_all_done,1794737913,2026-11-15 03:18:33
corpus_end,1794737913,2026-11-15 03:18:33
```
