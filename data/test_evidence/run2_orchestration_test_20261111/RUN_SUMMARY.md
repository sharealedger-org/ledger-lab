# Full Corpus Run Summary: 20261111_003210

- **Run Tag / Timestamp**: `20261111_003210`
- **Source Root**: `/Users/ktwitchell001/workspace/va_test_root/output_full`
- **Logs Root**: `/Users/ktwitchell001/workspace/va_test_logs`
- **Archived Date**: 2026-11-11 00:52:47

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261111_003210_2795","sje_balance","fixture","SortedJEFY04merged","0","-0.00","0.00","11086262","PASS",""
"run_20261111_004401_8211","sje_balance","fixture","SortedJEFY04merged","0","-0.00","0.00","11086262","PASS",""
"run_20261111_004401_8211","ledger_balance","fixture","ledger_fy2004","0","-0.00","0.00","370529","PASS",""
"run_20261111_003230_2940","sje_balance","fixture","SortedJEFY05merged","0","0.00","0.00","9309558","PASS",""
"run_20261111_004231_7789","sje_balance","fixture","SortedJEFY05merged","0","0.00","0.00","9309558","PASS",""
"run_20261111_004231_7789","ledger_balance","fixture","ledger_fy2005","0","0.00","0.00","364142","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261111_003210_2795","2026-11-11 00:32:10","CCP2","fullcorpus-run2-prep-fy04","04","ttl,2p,2m",400,5548049825,0,5948.05,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261111_004401_8211","2026-11-11 00:44:01","CCP1","fullcorpus-run2-chain-fy04","04","3,20,21",279,5623871497,0,5902.87,"CCP1","monthly","unspecified",370529,53197818,0,370529,261568885
"run_20261111_003230_2940","2026-11-11 00:32:30","CCP2","fullcorpus-run2-prep-fy05","05","ttl,2p,2m",346,4657103121,0,5003.10,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261111_004231_7789","2026-11-11 00:42:31","CCP1","fullcorpus-run2-chain-fy05","05","3,20,21",218,4732194339,0,4950.19,"CCP1","monthly","unspecified",364142,52319933,0,364142,89830069
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY04,POQuarantineFY04.csv,1
FY05,POQuarantineFY05.csv,3
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794382330,2026-11-11 00:32:10
# years=04,05 lanes=2 periodGranularity=monthly
# chain_width=2 views_early=Y caffeinate=on
# git=8054b7c dirty_files=1
# host=Kips-MBP cpus=4
chain_start_fy05,1794382951,2026-11-11 00:42:31
prep_all_done,1794383035,2026-11-11 00:43:55
views_start,1794383041,2026-11-11 00:44:01
chain_start_fy04,1794383041,2026-11-11 00:44:01
views_end,1794383264,2026-11-11 00:47:44
chain_end_fy05,1794383431,2026-11-11 00:50:31
chain_end_fy04,1794383567,2026-11-11 00:52:47
chain_all_done,1794383567,2026-11-11 00:52:47
views_all_done,1794383567,2026-11-11 00:52:47
corpus_end,1794383567,2026-11-11 00:52:47
```
