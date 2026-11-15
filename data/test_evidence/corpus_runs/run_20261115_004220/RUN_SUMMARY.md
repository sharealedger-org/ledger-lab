# Full Corpus Run Summary: 20261115_004220

- **Run Tag / Timestamp**: `20261115_004220`
- **Source Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/connected_reporting_20261115_v1/output`
- **Logs Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/connected_reporting_20261115_v1/logs`
- **Archived Date**: 2026-11-15 00:45:18

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261115_004220_32919","sje_balance","fixture","SortedJEFY14merged","0","0.00","0.00","12","PASS",""
"run_20261115_004300_34037","sje_balance","fixture","SortedJEFY14merged","0","0.00","0.00","12","PASS",""
"run_20261115_004300_34037","ledger_balance","fixture","ledger_fy2014","0","0.00","0.00","7","PASS",""
"run_20261115_004259_33934","sje_balance","fixture","SortedJEFY15merged","0","0.00","0.00","4","PASS",""
"run_20261115_004436_36511","sje_balance","fixture","SortedJEFY15merged","0","0.00","0.00","4","PASS",""
"run_20261115_004436_36511","ledger_balance","fixture","ledger_fy2015","0","0.00","0.00","4","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261115_004220_32919","2026-11-15 00:42:20","CCP2","fullcorpus-prep-B-fy14","14","ttl,2p,2m",38,9073,0,38.01,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261115_004300_34037","2026-11-15 00:43:00","CCP1","fullcorpus-run2-chain-fy14","14","3,20,21",59,21991,0,59.02,"CCP1","monthly","unspecified",7,1478,0,7,23
"run_20261115_004259_33934","2026-11-15 00:42:59","CCP2","fullcorpus-prep-B-fy15","15","ttl,2p,2m",55,5116,0,55.01,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261115_004436_36511","2026-11-15 00:44:36","CCP1","fullcorpus-run2-chain-fy15","15","3,20,21",32,16506,0,32.02,"CCP1","monthly","unspecified",4,1118,0,4,4
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY14,POQuarantineFY14.csv,0
FY15,POQuarantineFY15.csv,0
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794728540,2026-11-15 00:42:20
# years=14,15 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/connected_reporting_20261115_inputs_v2/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=94e1919 dirty_files=11
# host=Kips-MBP cpus=4
chain_start_fy14,1794728580,2026-11-15 00:43:00
prep_all_done,1794728639,2026-11-15 00:43:59
views_start,1794728640,2026-11-15 00:44:00
views_end,1794728655,2026-11-15 00:44:15
chain_end_fy14,1794728676,2026-11-15 00:44:36
chain_start_fy15,1794728676,2026-11-15 00:44:36
chain_end_fy15,1794728718,2026-11-15 00:45:18
chain_all_done,1794728718,2026-11-15 00:45:18
views_all_done,1794728718,2026-11-15 00:45:18
corpus_end,1794728718,2026-11-15 00:45:18
```
