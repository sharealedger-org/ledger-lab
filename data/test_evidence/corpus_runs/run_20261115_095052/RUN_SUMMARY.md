# Full Corpus Run Summary: 20261115_095052

- **Run Tag / Timestamp**: `20261115_095052`
- **Source Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/corpus_admission_20261115/fy12/output`
- **Logs Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/corpus_admission_20261115/fy12/logs`
- **Archived Date**: 2026-11-15 10:19:33

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261115_095052_72609","sje_balance","fixture","SortedJEFY12merged","0","0.00","0.00","22260988","PASS",""
"run_20261115_100722_75868","sje_balance","fixture","SortedJEFY12merged","0","0.00","0.00","22260988","PASS",""
"run_20261115_100722_75868","ledger_balance","fixture","ledger_fy2012","0","0.00","0.00","1033458","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261115_095052_72609","2026-11-15 09:50:52","CCP2","fullcorpus-prep-B-fy12","12","ttl,2p,2m",445,15610919452,0,16055.92,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261115_100722_75868","2026-11-15 10:07:23","CCP1","fullcorpus-run2-chain-fy12","12","3,20,21",286,15854814552,0,16140.81,"CCP1","monthly","unspecified",1033458,167866918,0,1033458,926611748696096030793
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY12,POQuarantineFY12.csv,5742
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794761452,2026-11-15 09:50:52
# years=12 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/corpus_admission_20261115/inputs_v2/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=7494e0f dirty_files=6
# host=Kips-MBP cpus=4
prep_all_done,1794762440,2026-11-15 10:07:20
views_start,1794762442,2026-11-15 10:07:22
chain_start_fy12,1794762442,2026-11-15 10:07:22
views_end,1794762574,2026-11-15 10:09:34
chain_end_fy12,1794763173,2026-11-15 10:19:33
chain_all_done,1794763173,2026-11-15 10:19:33
views_all_done,1794763173,2026-11-15 10:19:33
corpus_end,1794763173,2026-11-15 10:19:33
```
