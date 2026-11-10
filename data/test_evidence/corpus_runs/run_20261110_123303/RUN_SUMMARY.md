# Full Corpus Run Summary: 20261110_123303

- **Run Tag / Timestamp**: `20261110_123303`
- **Source Root**: `/Users/ktwitchell001/workspace/va_fresh_root/output_full`
- **Logs Root**: `/Users/ktwitchell001/workspace/va_fresh_logs`
- **Archived Date**: 2026-11-10 15:48:01

## Reconciliation Controls Summary

```csv
run_id,check_id,scope,partition_id,expected_value,actual_value,delta,rows_checked,status,failure_reason
"run_20261110_124444_25253","ledger_balance","fixture","ledger_fy2003","0","-0.00","0.00","396858","PASS",""
"run_20261110_125133_30105","ledger_balance","fixture","ledger_fy2004","0","-0.00","0.00","370529","PASS",""
"run_20261110_125817_43534","ledger_balance","fixture","ledger_fy2005","0","0.00","0.00","364142","PASS",""
"run_20261110_130419_56133","ledger_balance","fixture","ledger_fy2006","0","-0.00","0.00","821695","PASS",""
"run_20261110_131152_71994","ledger_balance","fixture","ledger_fy2007","0","0.00","0.00","842912","PASS",""
"run_20261110_132039_79932","ledger_balance","fixture","ledger_fy2008","0","0.00","0.00","873398","PASS",""
"run_20261110_133050_94151","ledger_balance","fixture","ledger_fy2009","0","-0.00","0.00","345609","PASS",""
"run_20261110_133555_3276","ledger_balance","fixture","ledger_fy2010","0","60.00","60.00","881769","FAIL",""
"run_20261110_134049_11693","ledger_balance","fixture","ledger_fy2011","0","0.00","0.00","934356","PASS",""
"run_20261110_134403_16240","ledger_balance","fixture","ledger_fy2012","0","0.00","0.00","943770","PASS",""
"run_20261110_134718_20929","ledger_balance","fixture","ledger_fy2013","0","0.00","0.00","967076","PASS",""
"run_20261110_135053_28198","ledger_balance","fixture","ledger_fy2014","0","-0.00","0.00","979187","PASS",""
"run_20261110_135430_33272","ledger_balance","fixture","ledger_fy2015","0","0.00","0.00","963075","PASS",""
"run_20261110_135756_38308","ledger_balance","fixture","ledger_fy2016","0","0.00","0.00","967585","PASS",""
```

## Cost Surface Summary

```csv
"run_id","run_timestamp","curve","config_description","years","steps",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,"capability_layer","period_granularity","materialization_scope",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows
"run_20261110_123403_20451","2026-11-10 12:34:03","CCP2","fullcorpus-run2-prep-fy03","03","ttl,2p,2m",494,5818900812,0,6312.90,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_124444_25253","2026-11-10 12:44:44","CCP1","fullcorpus-run2-chain-fy03","03","3,20,21",363,5899882399,0,6262.88,"CCP1","monthly","unspecified",396858,57768135,0,383544
"run_20261110_123303_20073","2026-11-10 12:33:03","CCP2","fullcorpus-run2-prep-fy04","04","ttl,2p,2m",510,5548049403,0,6058.05,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_125133_30105","2026-11-10 12:51:33","CCP1","fullcorpus-run2-chain-fy04","04","3,20,21",359,5623648687,0,5982.65,"CCP1","monthly","unspecified",370529,53197818,0,370502
"run_20261110_123323_20216","2026-11-10 12:33:23","CCP2","fullcorpus-run2-prep-fy05","05","ttl,2p,2m",469,4657102698,0,5126.10,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_125817_43534","2026-11-10 12:58:18","CCP1","fullcorpus-run2-chain-fy05","05","3,20,21",328,4731660752,0,5059.66,"CCP1","monthly","unspecified",364142,52319933,0,364029
"run_20261110_123343_20342","2026-11-10 12:33:43","CCP2","fullcorpus-run2-prep-fy06","06","ttl,2p,2m",683,7070942976,0,7753.94,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_130419_56133","2026-11-10 13:04:19","CCP1","fullcorpus-run2-chain-fy06","06","3,20,21",402,7243071376,0,7645.07,"CCP1","monthly","unspecified",821695,132959867,0,628956
"run_20261110_124347_24974","2026-11-10 12:43:47","CCP2","fullcorpus-run2-prep-fy07","07","ttl,2p,2m",811,7864611616,0,8675.61,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_131152_71994","2026-11-10 13:11:52","CCP1","fullcorpus-run2-chain-fy07","07","3,20,21",458,8040749342,0,8498.75,"CCP1","monthly","unspecified",842912,136649273,0,632733
"run_20261110_124326_24650","2026-11-10 12:43:26","CCP2","fullcorpus-run2-prep-fy08","08","ttl,2p,2m",960,9411428282,0,10371.43,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_132039_79932","2026-11-10 13:20:39","CCP1","fullcorpus-run2-chain-fy08","08","3,20,21",539,9595052871,0,10134.05,"CCP1","monthly","unspecified",873398,142508811,0,652019
"run_20261110_124454_25394","2026-11-10 12:44:54","CCP2","fullcorpus-run2-prep-fy09","09","ttl,2p,2m",700,6854809219,0,7554.81,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_133050_94151","2026-11-10 13:30:50","CCP1","fullcorpus-run2-chain-fy09","09","3,20,21",276,6926133872,0,7202.13,"CCP1","monthly","unspecified",345609,49680927,0,345609
"run_20261110_124915_26846","2026-11-10 12:49:15","CCP2","fullcorpus-run2-prep-fy10","10","ttl,2p,2m",1085,8831085747,0,9916.09,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_133555_3276","2026-11-10 13:35:55","CCP1","fullcorpus-run2-chain-fy10","10","3,20,21",257,9016156492,0,9273.16,"CCP1","monthly","unspecified",881769,143702313,0,658836
"run_20261110_130037_47373","2026-11-10 13:00:37","CCP2","fullcorpus-run2-prep-fy11","11","ttl,2p,2m",968,9400773735,0,10368.77,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_134049_11693","2026-11-10 13:40:49","CCP1","fullcorpus-run2-chain-fy11","11","3,20,21",165,9596404546,0,9761.40,"CCP1","monthly","unspecified",934356,152494891,0,682716
"run_20261110_130107_48091","2026-11-10 13:01:07","CCP2","fullcorpus-run2-prep-fy12","12","ttl,2p,2m",1081,9817522962,0,10898.52,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_134403_16240","2026-11-10 13:44:03","CCP1","fullcorpus-run2-chain-fy12","12","3,20,21",166,10014833411,0,10180.83,"CCP1","monthly","unspecified",943770,154849784,0,671361
"run_20261110_130527_59862","2026-11-10 13:05:27","CCP2","fullcorpus-run2-prep-fy13","13","ttl,2p,2m",998,9766291251,0,10764.29,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_134718_20929","2026-11-10 13:47:19","CCP1","fullcorpus-run2-chain-fy13","13","3,20,21",184,9969148349,0,10153.15,"CCP1","monthly","unspecified",967076,159064666,0,693352
"run_20261110_131216_72307","2026-11-10 13:12:16","CCP2","fullcorpus-run2-prep-fy14","14","ttl,2p,2m",1011,9936019388,0,10947.02,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_135053_28198","2026-11-10 13:50:53","CCP1","fullcorpus-run2-chain-fy14","14","3,20,21",183,10142104967,0,10325.10,"CCP1","monthly","unspecified",979187,161275257,0,711009
"run_20261110_132226_81551","2026-11-10 13:22:26","CCP2","fullcorpus-run2-prep-fy15","15","ttl,2p,2m",880,9436080821,0,10316.08,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_135430_33272","2026-11-10 13:54:30","CCP1","fullcorpus-run2-chain-fy15","15","3,20,21",173,9639206438,0,9812.21,"CCP1","monthly","unspecified",963075,158451725,0,708101
"run_20261110_132449_84831","2026-11-10 13:24:49","CCP2","fullcorpus-run2-prep-fy16","16","ttl,2p,2m",717,8977220925,0,9694.22,"CCP2","monthly","unspecified",0,0,0,0
"run_20261110_135756_38308","2026-11-10 13:57:56","CCP1","fullcorpus-run2-chain-fy16","16","3,20,21",180,9181753555,0,9361.75,"CCP1","monthly","unspecified",967585,159883800,0,704049
```

## Quarantine File Row Counts

```csv
fiscal_year,quarantine_file,rows
FY03,POQuarantineFY03.csv,20
FY04,POQuarantineFY04.csv,1
FY05,POQuarantineFY05.csv,3
FY06,POQuarantineFY06.csv,1989
FY07,POQuarantineFY07.csv,1675
FY08,POQuarantineFY08.csv,3048
FY09,POQuarantineFY09.csv,0
FY10,POQuarantineFY10.csv,3847
FY11,POQuarantineFY11.csv,4266
FY12,POQuarantineFY12.csv,5747
FY13,POQuarantineFY13.csv,3775
FY14,POQuarantineFY14.csv,2496
FY15,POQuarantineFY15.csv,2673
FY16,POQuarantineFY16.csv,2806
```

## Wallclock Execution Timeline

```csv
event,epoch_s,local_time
corpus_start,1794339183,2026-11-10 12:33:03
# years=03,04,05,06,07,08,09,10,11,12,13,14,15,16 lanes=4 periodGranularity=monthly
# git=846e952 dirty_files=1
# host=Kips-MBP cpus=4
chain_start_fy03,1794339884,2026-11-10 12:44:44
chain_end_fy03,1794340293,2026-11-10 12:51:33
chain_start_fy04,1794340293,2026-11-10 12:51:33
chain_end_fy04,1794340697,2026-11-10 12:58:17
chain_start_fy05,1794340697,2026-11-10 12:58:17
chain_end_fy05,1794341059,2026-11-10 13:04:19
chain_start_fy06,1794341059,2026-11-10 13:04:19
chain_end_fy06,1794341512,2026-11-10 13:11:52
chain_start_fy07,1794341512,2026-11-10 13:11:52
chain_end_fy07,1794342039,2026-11-10 13:20:39
chain_start_fy08,1794342039,2026-11-10 13:20:39
chain_end_fy08,1794342650,2026-11-10 13:30:50
chain_start_fy09,1794342650,2026-11-10 13:30:50
chain_end_fy09,1794342955,2026-11-10 13:35:55
chain_start_fy10,1794342955,2026-11-10 13:35:55
prep_all_done,1794343203,2026-11-10 13:40:03
chain_end_fy10,1794343249,2026-11-10 13:40:49
chain_start_fy11,1794343249,2026-11-10 13:40:49
chain_end_fy11,1794343443,2026-11-10 13:44:03
chain_start_fy12,1794343443,2026-11-10 13:44:03
chain_end_fy12,1794343638,2026-11-10 13:47:18
chain_start_fy13,1794343638,2026-11-10 13:47:18
chain_end_fy13,1794343853,2026-11-10 13:50:53
chain_start_fy14,1794343853,2026-11-10 13:50:53
chain_end_fy14,1794344070,2026-11-10 13:54:30
chain_start_fy15,1794344070,2026-11-10 13:54:30
chain_end_fy15,1794344276,2026-11-10 13:57:56
chain_start_fy16,1794344276,2026-11-10 13:57:56
chain_end_fy16,1794344488,2026-11-10 14:01:28
chain_all_done,1794344488,2026-11-10 14:01:28
views_start,1794344488,2026-11-10 14:01:28
views_end,1794346100,2026-11-10 14:28:20
corpus_end_FAILED,1794346100,2026-11-10 14:28:20
```
