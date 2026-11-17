#!/usr/bin/env bash
# Reporting continuation from accepted full financial partitions. Never reposts.
# Default check-only; new output generation preserves the original attempt.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mode=check; financial=""; root=""
historical="$repo/session-run.tmp/corpus_admission_20261115_receipts_v2/inputs"
generated="$repo/session-run.tmp/phase2_full_v6/inputs"
retained="$repo/session-run.tmp/retained_phase1_evidence_v1"
while [[ $# -gt 0 ]];do
  [[ $# -ge 2 ]] || exit 2
  case "$1" in
    --mode) mode="$2";; --financialRoot) financial="$2";; --out) root="$2";;
    --historical) historical="$2";; --generated) generated="$2";; --retained) retained="$2";;
    *) exit 2;;
  esac
  shift 2
done
[[ "$mode" == check || "$mode" == run ]]
[[ "$financial" =~ ^/[A-Za-z0-9_./-]+$ && -d "$financial" ]]
for path in "$historical" "$generated" "$retained";do [[ "$path" =~ ^/[A-Za-z0-9_./-]+$ && -d "$path" ]];done
years=03,04,05,06,07,08,09,10,11,12,13,14,15,16
IFS=, read -ra year_list <<< "$years"
printf 'mode=%s financial_root=%s years=%s\n' "$mode" "$financial" "$years"
if [[ "$mode" == check ]];then
  [[ ! -f "$financial/FINANCIAL_COMPLETE" ]] || printf 'Financial completion marker present; run mode verifies full controls and build identity.\n'
  printf 'Check-only: requires accepted full financial controls, compatible frozen classes and a new reporting output.\n'
  exit 0
fi
# All guards precede creating the continuation. Partial year outputs cannot enter.
[[ -f "$financial/FINANCIAL_COMPLETE" ]] || { echo 'Full financial controls have not completed.' >&2;exit 1; }
[[ -f "$financial/stages/financial_controls.complete" && -s "$financial/engine_classes_sha256.txt" ]]
marker="$financial/financial_controls/BASELINE_FINANCIAL_CONTROLS_COMPLETE"
for expected in historical_journal_rows=261529946 wholly_journal_rows=4789872 final_journal_rows=266319818;do
  rg -qx "$expected" "$marker" || { printf 'Required full-population control missing: %s\n' "$expected" >&2;exit 1; }
done
for old in "${year_list[@]}";do
  new="$(printf '%02d' "$((10#$old+28))")"
  [[ -f "$financial/years/FY$new/FINANCIAL_YEAR_COMPLETE" && -d "$financial/output/FY$new" && -d "$financial/normalized/FY$new" ]]
done
[[ "$root" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$root" ]]
project="$repo/02-foundation/va_pipeline"
classes="$project/target/scala-2.12/classes"
dependencies="$project/target/streams/compile/dependencyClasspath/_global/streams/export"
[[ -f "$dependencies" ]]
engine_git="$(sed -n 's/^engine_git=//p' "$financial/run_config.txt")"
[[ -n "$engine_git" ]]
# Only the declared reference metadata adapter may differ from the financial
# build. All other main source files AND their compiled bytes must remain frozen.
while IFS= read -r changed;do
  [[ -z "$changed" || "$changed" == 02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/PhaseTwoBaselineDimensions.scala ]] || { printf 'Financial source changed outside the metadata repair: %s\n' "$changed" >&2;exit 1; }
done <<< "$(git -C "$repo" diff --name-only "$engine_git" -- 02-foundation/va_pipeline/src/main/scala 04-engines/calculation_engines/src/main/scala)"
awk '$2 !~ /\/PhaseTwoBaselineDimensions[^/]*\.class$/' "$financial/engine_classes_sha256.txt" | shasum -a 256 --quiet -c -
[[ "$(ps -p $$ -o pgid= | tr -d ' ')" == "$$" ]] || { echo 'Launch in its own process group.' >&2;exit 2; }
pmset -g batt | head -n 1 | grep -q 'AC Power'
(( $(df -Pk "$repo" | awk 'NR==2{print $4}') >= 41943040 ))
mkdir -p "$root/logs" "$root/workers" "$root/stages"
printf 'phase2-fused-v1\n' > "$root/PROFILE"
printf 'reporting_continuation_preparation\n' > "$root/STATUS"
printf 'Reporting continuation; financial inputs: %s\n' "$financial" > "$root/SCOPE"
printf 'git=%s\nengine_git=%s\nfinancial_root=%s\nyears=%s\nheap_max_mib=2048\nactive_processors=1\nreserve_kib=41943040\n' "$(git -C "$repo" rev-parse HEAD)" "$engine_git" "$financial" "$years" > "$root/run_config.txt"
cp "$0" "$root/executed_driver.sh"
# Links preserve the actual producer/controls and consume no duplicate corpus.
for name in years native wholly output normalized financial_controls;do ln -s "$financial/$name" "$root/$name";done
ln -s "$financial/FINANCIAL_COMPLETE" "$root/FINANCIAL_COMPLETE"
ln -s "$financial/stages/financial_controls.complete" "$root/stages/financial_controls.complete"
for old in "${year_list[@]}";do
  new="$(printf '%02d' "$((10#$old+28))")"
  ln -s "$financial/workers/FY$new.status" "$root/workers/FY$new.status"
done
find "$classes" -type f -name '*.class' -print0 | xargs -0 shasum -a 256 > "$root/reporting_classes_sha256.txt"
monitor_pid=""
trap 'rc=$?; [[ -z "$monitor_pid" ]] || kill "$monitor_pid" 2>/dev/null || true; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$root/FAILED";fi' EXIT
trap 'exit 143' TERM
trap 'exit 130' INT
caffeinate -i -s -w $$ &
bash "$repo/scripts/monitor_synthetic_corpus_v2.sh" "$root" "$$" 60 41943040 > "$root/logs/monitor.log" 2>&1 &
monitor_pid=$!
classpath="$classes:$(cat "$dependencies")"
java_stage() {
  local stage="$1" main="$2";shift 2
  (( $(df -Pk "$root" | awk 'NR==2{print $4}') >= 41943040 ))
  shasum -a 256 --quiet -c "$root/reporting_classes_sha256.txt"
  printf '%s\n' "$stage" > "$root/STATUS"
  (cd "$project" && /usr/bin/time -l java -Dfile.encoding=UTF-8 -Xms512m -Xmx2048m -XX:ActiveProcessorCount=1 -cp "$classpath" "$main" "$@") > "$root/logs/$stage.log" 2>&1
  touch "$root/stages/$stage.complete"
}
paths="";normalizations="";ordered="";canonical_bytes=0;largest_bytes=0
for old in "${year_list[@]}";do
  new="$(printf '%02d' "$((10#$old+28))")"
  paths="${paths:+$paths,}$root/output/FY$new"
  normalizations="${normalizations:+$normalizations,}$root/normalized/FY$new"
  ordered="${ordered:+$ordered,}$root/report_order/FY$new"
  for file in "$root/output/FY$new/SortedJEFY${new}merged.csv" "$root/output/FY$new/LDGR20$new.csv";do
    bytes="$(stat -f %z "$file")";canonical_bytes=$((canonical_bytes+bytes))
    ((bytes<=largest_bytes)) || largest_bytes="$bytes"
  done
done
java_stage dimensions org.universalledger.foundation.va.corpus.PhaseTwoBaselineDimensions --historical "$historical" --historicalAssignments "$retained/preparation/synthetic_assignments.csv" --generated "$generated" --normalizations "$normalizations" --out "$root/dimensions"
# Same conservative resource gates as the admitted full driver; source financial
# partitions remain untouched. Live reserve monitoring is separate from timing.
sort_budget_kib=$(((canonical_bytes+4*largest_bytes+1023)/1024))
free_kib="$(df -Pk "$root" | awk 'NR==2{print $4}')"
printf 'stage,free_kib,budget_kib,reserve_kib,basis\nreport_order,%s,%s,41943040,canonical_plus_four_largest_file\n' "$free_kib" "$sort_budget_kib" > "$root/report_admission.csv"
((free_kib>=sort_budget_kib+41943040)) || { echo 'Reporting sort resource admission failed.' >&2;exit 1; }
java_stage report_order org.universalledger.foundation.va.corpus.ReportingSort --inPaths "$paths" --out "$root/report_order" --chunk 100000 --fanIn 16
reference_bytes="$(awk -F, 'NR>1 && ($1=="movement" || $1=="balance"){sum+=$4}END{printf "%.0f",sum}' "$retained/historical_report/input_manifest.csv")"
reference_kib="$(du -sk "$retained/historical_report" | awk '{print $1}')"
((reference_bytes>0))
report_budget_kib="$(awk -v b="$canonical_bytes" -v rb="$reference_bytes" -v rk="$reference_kib" 'BEGIN{printf "%.0f",2*b*rk/rb+4194304}')"
free_kib="$(df -Pk "$root" | awk 'NR==2{print $4}')"
printf 'reporting,%s,%s,41943040,twice_scaled_retained_report_plus_4GiB\n' "$free_kib" "$report_budget_kib" >> "$root/report_admission.csv"
((free_kib>=report_budget_kib+41943040)) || { echo 'Reporting resource admission failed.' >&2;exit 1; }
shasum -a 256 --quiet -c "$root/reporting_classes_sha256.txt"
printf 'reporting\n' > "$root/STATUS"
export JAVA_OPTS='-Dfile.encoding=UTF-8 -Xms512m -Xmx2048m -XX:ActiveProcessorCount=1'
bash "$repo/scripts/run_reporting.sh" --inPaths "$ordered" --outPath "$root/report" --inputOrder presorted --vendorMaster "$root/dimensions/VendorMaster.csv" --refPath "$root/dimensions" --syntheticHistories "$generated/synthetic_histories" --syntheticAssignments "$root/dimensions/synthetic_assignments.csv" --syntheticAsOf 2031-06-30 --syntheticRunDate 2045-07-01 --aggregateKeys 50000 --syntheticAggregateKeys 10000 --mergeFanIn 16 > "$root/logs/reporting.log" 2>&1
java_stage report_checks org.universalledger.foundation.va.corpus.ConnectedViewsCheck --report "$root/report"
touch "$root/REPORT_CHECKS_COMPLETE"
java_stage census org.universalledger.foundation.va.corpus.CorpusAdmissionSummary --report "$root/report" --out "$root/census"
touch "$root/CENSUS_COMPLETE"
printf 'financial_report_workflow_complete_validation_pending\n' > "$root/WORKFLOW_COMPLETE"
printf 'workflow_complete_result_validation_pending\n' > "$root/STATUS"
