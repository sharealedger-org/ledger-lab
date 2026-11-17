#!/usr/bin/env bash
# Resource-admitted independent year processes, followed by shared reporting.
# Scala owns source/financial/control/report calculations; Bash owns job control.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mode=check; root=""; years=03,04,05,06,07,08,09,10,11,12,13,14,15,16; workers=4; through=reporting
historical="$repo/session-run.tmp/corpus_admission_20261115_receipts_v2/inputs"
wholly="$repo/session-run.tmp/phase2_wholly_sources_v1"
generated="$repo/session-run.tmp/phase2_full_v6/inputs"
retained="$repo/session-run.tmp/retained_phase1_evidence_v1"
admission="$repo/session-run.tmp/phase2_fused_admission_v3"
historical_manifest=""; wholly_manifest="$repo/data/test_evidence/phase2_derived_preparation_20261116/subset_sha256.txt"
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || exit 2
  case "$1" in
    --mode) mode="$2";; --out) root="$2";; --years) years="$2";; --workers) workers="$2";; --through) through="$2";;
    --historical) historical="$2";; --wholly) wholly="$2";; --generated) generated="$2";; --retained) retained="$2";;
    --admission) admission="$2";; --historicalManifest) historical_manifest="$2";; --whollyManifest) wholly_manifest="$2";;
    *) exit 2;;
  esac
  shift 2
done
[[ "$mode" == check || "$mode" == run ]] && [[ "$through" == financial || "$through" == reporting ]]
[[ "$workers" =~ ^[1-4]$ && "$years" =~ ^(0[3-9]|1[0-6])(,(0[3-9]|1[0-6]))*$ ]]
[[ -n "$historical_manifest" ]] || historical_manifest="$historical/prepared_input_sha256.txt"
for path in "$historical" "$wholly" "$retained"; do [[ "$path" =~ ^/[A-Za-z0-9_./-]+$ && -d "$path" ]]; done
[[ -f "$historical/INPUTS_COMPLETE" && -f "$wholly/WHOLLY_SOURCE_SUBSET_COMPLETE" && -f "$historical_manifest" && -f "$wholly_manifest" ]]
IFS=, read -ra year_list <<< "$years"
seen=,
for old in "${year_list[@]}"; do [[ "$seen" != *",$old,"* ]]; seen="$seen$old,"; done
project="$repo/02-foundation/va_pipeline"
classes="$project/target/scala-2.12/classes"
dependencies="$project/target/streams/compile/dependencyClasspath/_global/streams/export"
[[ -f "$dependencies" && -f "$classes/org/universalledger/foundation/va/corpus/FusedFinancialYear.class" ]]
physical_bytes="$(sysctl -n hw.memsize)"
# Four 2-GiB heaps plus 0.5 GiB/JVM non-heap; reserve 6 GiB for OS/apps.
memory_workers=$(((physical_bytes-6442450944)/2684354560))
((memory_workers>=1))
((workers<=memory_workers)) || workers="$memory_workers"
printf 'mode=%s years=%s max_workers=%s through=%s admission=%s\n' "$mode" "$years" "$workers" "$through" "$admission"
if [[ "$mode" == check ]]; then
  [[ ! -f "$admission/ADMISSION_COMPLETE" ]] || printf 'Completed real-year admission is present.\n'
  printf 'Check-only: full execution requires completed admission, unchanged engine and fresh resource gates.\n'
  exit 0
fi
[[ "$root" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$root" ]]
[[ -f "$admission/ADMISSION_COMPLETE" && -s "$admission/engine_classes_sha256.txt" ]]
engine_git="$(sed -n 's/^git=//p' "$admission/run_config.txt")"
[[ -n "$engine_git" && -z "$(git -C "$repo" diff "$engine_git" -- 02-foundation/va_pipeline/src/main/scala 04-engines/calculation_engines/src/main/scala)" ]]
shasum -a 256 -c "$admission/engine_classes_sha256.txt" >/dev/null
[[ "$(ps -p $$ -o pgid= | tr -d ' ')" == "$$" ]] || { echo 'Launch in its own process group.' >&2; exit 2; }
pmset -g batt | head -n 1 | grep -q 'AC Power'
mkdir -p "$root/logs" "$root/workers" "$root/jobs" "$root/years" "$root/stages" "$root/native" "$root/wholly" "$root/output" "$root/normalized"
printf 'phase2-fused-v1\n' > "$root/PROFILE"
printf 'input_hash_verification\n' > "$root/STATUS"
printf 'git=%s\nengine_git=%s\nyears=%s\nmax_workers=%s\nheap_max_mib=2048\nactive_processors_per_worker=1\nreserve_kib=41943040\nthrough=%s\n' "$(git -C "$repo" rev-parse HEAD)" "$engine_git" "$years" "$workers" "$through" > "$root/run_config.txt"
cp "$0" "$root/executed_driver.sh"
cp "$admission/engine_classes_sha256.txt" "$root/engine_classes_sha256.txt"
printf 'wave,years,workers,free_kib,reserved_worker_kib\n' > "$root/wave_admission.csv"
monitor_pid=""
trap 'rc=$?; [[ -z "$monitor_pid" ]] || kill "$monitor_pid" 2>/dev/null || true; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$root/FAILED"; fi' EXIT
trap 'exit 143' TERM
trap 'exit 130' INT
caffeinate -i -s -w $$ &
bash "$repo/scripts/monitor_synthetic_corpus_v2.sh" "$root" "$$" 60 41943040 > "$root/logs/monitor.log" 2>&1 &
monitor_pid=$!
(cd "$repo" && shasum -a 256 -c "$wholly_manifest") > "$root/logs/wholly_hashes.txt" 2>&1
shasum -a 256 -c "$historical_manifest" > "$root/logs/historical_hashes.txt" 2>&1
classpath="$classes:$(cat "$dependencies")"
java_stage() {
  local stage="$1" main="$2"; shift 2
  (( $(df -Pk "$root" | awk 'NR==2{print $4}') >= 41943040 ))
  printf '%s\n' "$stage" > "$root/STATUS"
  (cd "$project" && /usr/bin/time -l java -Dfile.encoding=UTF-8 -Xms512m -Xmx2048m -XX:ActiveProcessorCount=1 -cp "$classpath" "$main" "$@") > "$root/logs/$stage.log" 2>&1
  touch "$root/stages/$stage.complete"
}
year_budget() {
  awk -F, 'NR==1{for(i=1;i<=NF;i++)h[$i]=i;next}{for(i=1;i<=NF;i++)gsub(/"/,"",$i);if($h["pass_id"]=="2m"){n++;b=$h["output_bytes"];r=$h["output_rows"]}}END{if(n!=1)exit 1;printf "%.0f",(4*b+512*r+4294967296)/1024}' "$retained/financial_controls/FY$1/metrics/process_metrics.csv"
}
run_year() {
  local old="$1" new rc
  new="$(printf '%02d' "$((10#$old+28))")"
  if (cd "$project" && /usr/bin/time -l java -Dfile.encoding=UTF-8 -Xms512m -Xmx2048m -XX:ActiveProcessorCount=1 -cp "$classpath" org.universalledger.foundation.va.corpus.FusedFinancialYear \
    --historical "$historical" --wholly "$wholly" --retained "$retained" --sortSpec "$repo/02-foundation/sort_specs/SortedJE.sortspec" \
    --year "20$old" --out "$root/years/FY$new" --statusFile "$root/workers/FY$new.status" --runId "${root##*/}-FY$new") > "$root/logs/FY$old.log" 2>&1; then rc=0;else rc=$?;fi
  printf '%s\n' "$rc" > "$root/jobs/FY$old.exit"
  return "$rc"
}
index=0;wave_number=0
printf 'financial_years_parallel\n' > "$root/STATUS"
while ((index<${#year_list[@]})); do
  shasum -a 256 -c "$root/engine_classes_sha256.txt" >/dev/null
  free_kib="$(df -Pk "$root" | awk 'NR==2{print $4}')"
  wave=();pids=();reserved=0
  while ((index<${#year_list[@]} && ${#wave[@]}<workers)); do
    old="${year_list[index]}";budget="$(year_budget "$old")"
    ((reserved+budget+41943040<=free_kib)) || break
    wave[${#wave[@]}]="$old";reserved=$((reserved+budget));index=$((index+1))
  done
  ((${#wave[@]}>0)) || { echo 'Insufficient scratch to admit the next year.' >&2; exit 1; }
  wave_number=$((wave_number+1))
  printf '%s,%s,%s,%s,%s\n' "$wave_number" "$(IFS=+;echo "${wave[*]}")" "${#wave[@]}" "$free_kib" "$reserved" >> "$root/wave_admission.csv"
  for old in "${wave[@]}"; do run_year "$old" & pids[${#pids[@]}]=$!;done
  while true; do
    all_done=yes
    for wave_index in "${!wave[@]}"; do
      old="${wave[wave_index]}"
      if [[ -f "$root/jobs/FY$old.exit" ]]; then
        [[ "$(cat "$root/jobs/FY$old.exit")" == 0 ]] || { printf 'Year %s failed; stopping this attempt.\n' "$old" >&2;kill -TERM -- "-$$";exit 1; }
      else
        kill -0 "${pids[wave_index]}" 2>/dev/null || { printf 'Year %s exited without a completion record.\n' "$old" >&2;kill -TERM -- "-$$";exit 1; }
        all_done=no
      fi
    done
    [[ "$all_done" != yes ]] || break
    sleep 5
  done
  for pid in "${pids[@]}";do wait "$pid";done
  for old in "${wave[@]}";do
    new="$(printf '%02d' "$((10#$old+28))")";base="$root/years/FY$new"
    [[ -f "$base/FINANCIAL_YEAR_COMPLETE" ]]
    ln -s "$base/native/FY$old" "$root/native/FY$old"
    ln -s "$base/wholly/FY$new" "$root/wholly/FY$new"
    ln -s "$base/output/FY$new" "$root/output/FY$new"
    ln -s "$base/source/normalizations/FY$new" "$root/normalized/FY$new"
  done
done
full=false; ((${#year_list[@]}!=14)) || full=true
java_stage financial_controls org.universalledger.foundation.va.corpus.PhaseTwoBaselineControls --root "$root" --retained "$retained" --years "$years" --out "$root/financial_controls" --full "$full" --historicalPass source:historical --whollyPass source:wholly_generated --postingPass fused_post
printf 'financial_controls_passed_reporting_pending\n' > "$root/FINANCIAL_COMPLETE"
if [[ "$through" == financial ]];then printf 'financial_complete_reporting_pending\n' > "$root/STATUS";exit 0;fi
paths="";normalizations="";ordered=""
for old in "${year_list[@]}";do
  new="$(printf '%02d' "$((10#$old+28))")"
  paths="${paths:+$paths,}$root/output/FY$new"
  normalizations="${normalizations:+$normalizations,}$root/normalized/FY$new"
  ordered="${ordered:+$ordered,}$root/report_order/FY$new"
done
java_stage dimensions org.universalledger.foundation.va.corpus.PhaseTwoBaselineDimensions --historical "$historical" --historicalAssignments "$retained/preparation/synthetic_assignments.csv" --generated "$generated" --normalizations "$normalizations" --out "$root/dimensions"
# Reporting sort retains all final ordered files. Its temporary key-prefixed
# records are at most twice canonical bytes; allow two generations of the
# largest file, in addition to all final ordered outputs and the reserve.
canonical_bytes=0;largest_bytes=0
for old in "${year_list[@]}";do
  new="$(printf '%02d' "$((10#$old+28))")"
  for file in "$root/output/FY$new/SortedJEFY${new}merged.csv" "$root/output/FY$new/LDGR20$new.csv";do
    bytes="$(stat -f %z "$file")"
    canonical_bytes=$((canonical_bytes+bytes))
    ((bytes<=largest_bytes)) || largest_bytes="$bytes"
  done
done
sort_budget_kib=$(((canonical_bytes+4*largest_bytes+1023)/1024))
free_kib="$(df -Pk "$root" | awk 'NR==2{print $4}')"
printf 'stage,free_kib,budget_kib,reserve_kib,basis\nreport_order,%s,%s,41943040,canonical_plus_four_largest_file\n' "$free_kib" "$sort_budget_kib" > "$root/report_admission.csv"
((free_kib>=sort_budget_kib+41943040)) || { echo 'Reporting sort resource admission failed.' >&2;exit 1; }
java_stage report_order org.universalledger.foundation.va.corpus.ReportingSort --inPaths "$paths" --out "$root/report_order" --chunk 100000 --fanIn 16
# Planning estimate from the retained successful reporting footprint, scaled by
# actual new financial bytes with a twofold margin. The live disk stop remains
# authoritative; this estimate is not an engine or cost-curve measurement.
reference_bytes="$(awk -F, 'NR>1 && ($1=="movement" || $1=="balance"){sum+=$4}END{printf "%.0f",sum}' "$retained/historical_report/input_manifest.csv")"
reference_kib="$(du -sk "$retained/historical_report" | awk '{print $1}')"
((reference_bytes>0))
report_budget_kib="$(awk -v b="$canonical_bytes" -v rb="$reference_bytes" -v rk="$reference_kib" 'BEGIN{printf "%.0f",2*b*rk/rb+4194304}')"
free_kib="$(df -Pk "$root" | awk 'NR==2{print $4}')"
printf 'reporting,%s,%s,41943040,twice_scaled_retained_report_plus_4GiB\n' "$free_kib" "$report_budget_kib" >> "$root/report_admission.csv"
((free_kib>=report_budget_kib+41943040)) || { echo 'Reporting resource admission failed.' >&2;exit 1; }
printf 'reporting\n' > "$root/STATUS"
export JAVA_OPTS='-Dfile.encoding=UTF-8 -Xms512m -Xmx2048m -XX:ActiveProcessorCount=1'
bash "$repo/scripts/run_reporting.sh" --inPaths "$ordered" --outPath "$root/report" --inputOrder presorted --vendorMaster "$root/dimensions/VendorMaster.csv" --refPath "$root/dimensions" --syntheticHistories "$generated/synthetic_histories" --syntheticAssignments "$root/dimensions/synthetic_assignments.csv" --syntheticAsOf 2031-06-30 --syntheticRunDate 2045-07-01 --aggregateKeys 50000 --syntheticAggregateKeys 10000 --mergeFanIn 16 > "$root/logs/reporting.log" 2>&1
java_stage report_checks org.universalledger.foundation.va.corpus.ConnectedViewsCheck --report "$root/report"
touch "$root/REPORT_CHECKS_COMPLETE"
java_stage census org.universalledger.foundation.va.corpus.CorpusAdmissionSummary --report "$root/report" --out "$root/census"
touch "$root/CENSUS_COMPLETE"
printf 'financial_report_workflow_complete_validation_pending\n' > "$root/WORKFLOW_COMPLETE"
printf 'workflow_complete_result_validation_pending\n' > "$root/STATUS"
