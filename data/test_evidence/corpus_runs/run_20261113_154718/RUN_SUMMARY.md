# Full Corpus Run Summary: 20261113_154718

- **Run Tag / Timestamp**: `20261113_154718`
- **Source Root**: `/private/tmp/ckb-intel-preflight-20261113/output`
- **Logs Root**: `/private/tmp/ckb-intel-preflight-20261113/logs`
- **Archived Date**: 2026-11-13 15:49:03

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261113_154718_6473","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","12","PASS",""
"run_20261113_154759_7569","sje_balance","fixture","SortedJEFY03merged","0","0.00","0.00","12","PASS",""
"run_20261113_154759_7569","ledger_balance","fixture","ledger_fy2003","0","0.00","0.00","7","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity
"run_20261113_154718_6473","2026-11-13 15:47:18","CCP2","fullcorpus-prep-B-fy03","03","ttl,2p,2m",35,8786,0,35.01,"CCP2","monthly","unspecified",0,0,0,0,0
"run_20261113_154759_7569","2026-11-13 15:47:59","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",51,20742,0,51.02,"CCP1","monthly","unspecified",7,1516,0,7,12
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY03,POQuarantineFY03.csv,0
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794610038,2026-11-13 15:47:18
# years=03 lanes=1 periodGranularity=monthly
# ttl_type=B vendor_master=/private/tmp/ckb-intel-preflight-20261113/VendorMaster.csv
# chain_width=1 views_early=Y caffeinate=on
# git=b10b480 dirty_files=4
# host=Kips-MBP cpus=4
prep_all_done,1794610076,2026-11-13 15:47:56
chain_start_fy03,1794610078,2026-11-13 15:47:58
views_start,1794610078,2026-11-13 15:47:58
views_end,1794610096,2026-11-13 15:48:16
chain_end_fy03,1794610143,2026-11-13 15:49:03
chain_all_done,1794610143,2026-11-13 15:49:03
views_all_done,1794610143,2026-11-13 15:49:03
corpus_end,1794610143,2026-11-13 15:49:03
```
