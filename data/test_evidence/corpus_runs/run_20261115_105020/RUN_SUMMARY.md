# Full Corpus Run Summary: 20261115_105020

- **Run Tag / Timestamp**: `20261115_105020`
- **Source Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/corpus_admission_20261115_receipts_v2/fy12/output`
- **Logs Root**: `/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/corpus_admission_20261115_receipts_v2/fy12/logs`
- **Archived Date**: 2026-11-15 11:18:18

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261115_105020_79185","sje_balance","fixture","SortedJEFY12merged","0","-0.00","0.00","22260988","PASS",""
"run_20261115_110611_81754","sje_balance","fixture","SortedJEFY12merged","0","-0.00","0.00","22260988","PASS",""
"run_20261115_110611_81754","ledger_balance","fixture","ledger_fy2012","0","0.00","0.00","1031381","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261115_105020_79185","2026-11-15 10:50:20","CCP2","fullcorpus-prep-B-fy12","12","ttl,2p,2m",416,15610919493,0,16026.92,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261115_110611_81754","2026-11-15 11:06:12","CCP1","fullcorpus-run2-chain-fy12","12","3,20,21",284,15854508821,0,16138.51,"CCP1","monthly","unspecified",1031381,167561274,0,1031381,926611727695882614381
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY12,POQuarantineFY12.csv,5742
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794765020,2026-11-15 10:50:20
# years=12 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/Users/ktwitchell001/workspace/sharealedger/ledger-lab/session-run.tmp/corpus_admission_20261115_receipts_v2/inputs/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=16c4c91 dirty_files=12
# host=Kips-MBP cpus=4
prep_all_done,1794765966,2026-11-15 11:06:06
chain_start_fy12,1794765971,2026-11-15 11:06:11
views_start,1794765971,2026-11-15 11:06:11
views_end,1794766099,2026-11-15 11:08:19
chain_end_fy12,1794766698,2026-11-15 11:18:18
chain_all_done,1794766698,2026-11-15 11:18:18
views_all_done,1794766698,2026-11-15 11:18:18
corpus_end,1794766698,2026-11-15 11:18:18
```
