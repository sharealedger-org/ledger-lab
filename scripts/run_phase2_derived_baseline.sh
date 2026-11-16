#!/usr/bin/env bash
# Stable orchestration only; Scala owns source transformation, identity preparation and controls.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
root=""; mode=check; years=03,04,05,06,07,08,09,10,11,12,13,14,15,16
historical="$repo/session-run.tmp/corpus_admission_20261115_receipts_v2/inputs"
generated="$repo/session-run.tmp/phase2_full_v6/inputs"
subset="$repo/session-run.tmp/phase2_wholly_sources_v1"
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || exit 1
  case "$1" in
    --out) root="$2" ;; --mode) mode="$2" ;; --years) years="$2" ;;
    --historical) historical="$2" ;; --generated) generated="$2" ;; --subset) subset="$2" ;;
    *) exit 1 ;;
  esac
  shift 2
done
[[ "$mode" == check || "$mode" == run ]] || exit 1
for path in "$historical" "$generated" "$subset"; do
  [[ "$path" =~ ^/[A-Za-z0-9_./-]+$ && -d "$path" ]] || exit 1
done
[[ -f "$subset/WHOLLY_SOURCE_SUBSET_COMPLETE" && -f "$historical/INPUTS_COMPLETE" ]] || exit 1
[[ "$years" =~ ^(0[3-9]|1[0-6])(,(0[3-9]|1[0-6]))*$ ]] || exit 1
[[ -d "$historical/VARawFiles" && -d "$historical/VAPOData" ]] || exit 1
[[ -f "$repo/session-run.tmp/retained_phase1_evidence_v1/preparation/synthetic_assignments.csv" ]] || exit 1
for name in VendorMaster.csv VAAccountingRules.csv aRefAgencyCSV.txt aRefFundCSV.txt aRefObjectCSV.txt aRefProgramCSV.txt aRefSourceCSV.txt; do
  [[ -f "$historical/$name" && -f "$subset/$name" ]] || exit 1
done
free_kib="$(df -Pk "$repo" | awk 'NR==2 {print $4}')"
printf 'mode=%s years=%s free_kib=%s\n' "$mode" "$years" "$free_kib"
if [[ "$mode" == check ]]; then
  printf 'Input paths present; this is not financial validation or storage admission.\n'
  exit 0
fi
if command -v pmset >/dev/null 2>&1; then
  pmset -g batt | head -n 1 | grep -q 'AC Power' || { echo 'Connect AC power before the full run.' >&2; exit 1; }
fi
(cd "$repo" && shasum -a 256 -c data/test_evidence/phase2_derived_preparation_20261116/subset_sha256.txt) >/dev/null
shasum -a 256 -c "$historical/prepared_input_sha256.txt" >/dev/null
[[ "$root" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$root" ]] || exit 1
(( free_kib >= 314572800 )) || { echo 'Full-run admission requires 300 GiB free and a 40 GiB stage reserve.' >&2; exit 1; }
mkdir -p "$root/logs" "$root/output"
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
monitor_pid=""
trap 'rc=$?; if [[ -n "$monitor_pid" ]]; then kill "$monitor_pid" 2>/dev/null || true; fi; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$root/FAILED"; fi' EXIT
bash "$repo/scripts/monitor_synthetic_corpus_v2.sh" "$root" "$$" 60 0 > "$root/logs/monitor.log" 2>&1 &
monitor_pid=$!
export JAVA_OPTS='-Dfile.encoding=UTF-8 -Xms1024m -Xmx1024m -XX:ActiveProcessorCount=4'
cp "$0" "$root/executed_driver.sh"
printf 'git=%s\nyears=%s\nhistorical=%s\ngenerated=%s\nsubset=%s\n' "$(git -C "$repo" rev-parse HEAD)" "$years" "$historical" "$generated" "$subset" > "$root/run_config.txt"
printf 'stage,utc,free_kib,allocated_kib\n' > "$root/storage_gates.csv"
gate() {
  local available allocated
  available="$(df -Pk "$root" | awk 'NR==2 {print $4}')"
  allocated="$(du -sk "$root" | awk '{print $1}')"
  printf '%s,%s,%s,%s\n' "$1" "$(date -u '+%FT%TZ')" "$available" "$allocated" >> "$root/storage_gates.csv"
  (( available >= 41943040 ))
}
scala() {
  local stage="$1" command="$2"
  gate "$stage"; printf '%s\n' "$stage" > "$root/STATUS"
  (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -l sbt -batch "$command") > "$root/logs/$stage.log" 2>&1
}
pipeline() {
  local stage="$1" input="$2" out="$3" year="$4" steps="$5"
  gate "$stage"; printf '%s\n' "$stage" > "$root/STATUS"
  bash "$repo/scripts/run_pipeline.sh" --inPath "$input/VARawFiles" --poPath "$input/VAPOData" --outPath "$out" --years "$year" --steps "$steps" --ttlType B --vendorMaster "$input/VendorMaster.csv" --periodGranularity monthly --curve CCP2 --profile phase2-derived-v1 --materialization persist-sjes --config phase2-derived-v1 > "$root/logs/$stage.log" 2>&1
}
IFS=, read -ra year_list <<< "$years"
paths=""; normalizations=""; ordered=""
for old in "${year_list[@]}"; do
  new="$(printf '%02d' "$((10#$old+28))")"
  native="$root/native/FY$old"; whole="$root/wholly/FY$new"; normalized="$root/normalized/FY$new"; financial="$root/output/FY$new"
  pipeline "historical_$old" "$historical" "$native" "$old" ttl,2p,2m,20
  pipeline "wholly_$new" "$subset" "$whole" "$new" ttl,2p,2m,20
  scala "normalize_$old" "runMain org.universalledger.foundation.va.corpus.PhaseTwoBaselineNormalize --source $native/SortedJEFY${old}merged.csv --objects $historical/aRefObjectCSV.txt --sortSpec $repo/02-foundation/sort_specs/SortedJE.sortspec --out $normalized --year 20$old"
  mkdir -p "$financial"
  scala "merge_$new" "run --step 2m --year $new --outPath $financial --payment $normalized/SortedJEFY${new}merged.csv --po $whole/SortedJEFY${new}merged.csv --sortspec $repo/02-foundation/sort_specs/SortedJE.sortspec"
  pipeline "post_$new" "$subset" "$financial" "$new" 3,20,21
  # Retire only this attempt's consumed SJE intermediates; keep logs, controls,
  # source lineage, native rejection evidence and final merged financial records.
  for dir in "$native" "$whole" "$normalized"; do
    for file in "$dir"/SortedJE*.csv; do [[ ! -f "$file" ]] || rm "$file"; done
  done
  paths="${paths:+$paths,}$financial"
  normalizations="${normalizations:+$normalizations,}$normalized"
  ordered="${ordered:+$ordered,}$root/report_order/FY$new"
done
full=false
[[ "$years" != 03,04,05,06,07,08,09,10,11,12,13,14,15,16 ]] || full=true
scala financial_controls "runMain org.universalledger.foundation.va.corpus.PhaseTwoBaselineControls --root $root --retained $repo/session-run.tmp/retained_phase1_evidence_v1 --years $years --out $root/financial_controls --full $full"
scala dimensions "runMain org.universalledger.foundation.va.corpus.PhaseTwoBaselineDimensions --historical $historical --historicalAssignments $repo/session-run.tmp/retained_phase1_evidence_v1/preparation/synthetic_assignments.csv --generated $generated --normalizations $normalizations --out $root/dimensions"
scala report_order "runMain org.universalledger.foundation.va.corpus.ReportingSort --inPaths $paths --out $root/report_order --chunk 100000 --fanIn 16"
gate reporting
bash "$repo/scripts/run_reporting.sh" --inPaths "$ordered" --outPath "$root/report" --inputOrder presorted --vendorMaster "$root/dimensions/VendorMaster.csv" --refPath "$root/dimensions" --syntheticHistories "$generated/synthetic_histories" --syntheticAssignments "$root/dimensions/synthetic_assignments.csv" --syntheticAsOf 2031-06-30 --syntheticRunDate 2045-07-01 --aggregateKeys 50000 --syntheticAggregateKeys 10000 --mergeFanIn 16 > "$root/logs/reporting.log" 2>&1
scala report_checks "runMain org.universalledger.foundation.va.corpus.ConnectedViewsCheck --report $root/report"
scala census "runMain org.universalledger.foundation.va.corpus.CorpusAdmissionSummary --report $root/report --out $root/census"
gate phase3_reserve
printf 'workflow_complete_owner_result_validation_pending\n' > "$root/STATUS"
printf 'financial_report_workflow_complete_not_final_acceptance\n' > "$root/WORKFLOW_COMPLETE"
