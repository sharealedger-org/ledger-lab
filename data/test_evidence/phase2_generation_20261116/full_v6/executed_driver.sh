#!/usr/bin/env bash
# Ordered job control only. Scala owns generation, source exports and financial checks.
set -euo pipefail
main() {
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
root=""; config=""; years=""; chunk=100000; calibration=""; reused=""
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || exit 1
  case "$1" in
    --out) root="$2" ;;
    --config) config="$2" ;;
    --years) years="$2" ;;
    --chunk) chunk="$2" ;;
    --calibration) calibration="$2" ;;
    --reused) reused="$2" ;;
    *) exit 1 ;;
  esac
  shift 2
done
[[ "$root" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$root" && -f "$config" && "$years" =~ ^[0-9]{2}(,[0-9]{2})*$ && "$chunk" =~ ^[0-9]+$ ]] || exit 1
mkdir -p "$root/logs"
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
sampler_pid=""
trap 'rc=$?; if [[ -n "$sampler_pid" ]]; then kill "$sampler_pid" 2>/dev/null || true; fi; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$root/FAILED"; fi; exit "$rc"' EXIT
printf 'utc,free_kib,allocated_kib\n' > "$root/storage_samples.csv"
(
  while :; do
    sample_free="$(df -Pk "$root" | awk 'NR==2 {print $4}')"
    sample_allocated="$(du -sk "$root" 2>/dev/null | awk 'NR==1 {print $1}')" || sample_allocated="unknown"
    printf '%s,%s,%s\n' "$(date -u '+%FT%TZ')" "$sample_free" "${sample_allocated:-unknown}" >> "$root/storage_samples.csv"
    if [[ "$sample_free" =~ ^[0-9]+$ ]] && (( sample_free < 20971520 )); then : > "$root/LOW_DISK_STOP"; fi
    sleep 15
  done
) &
sampler_pid=$!
printf '%s\n' "$sampler_pid" > "$root/sampler.pid"
cp "$repo/scripts/run_phase2_corpus.sh" "$root/executed_driver.sh"
printf 'git=%s\nworkload=fictional-phase2-v1\nyears=%s\n' "$(git -C "$repo" rev-parse HEAD)" "$years" > "$root/run_config.txt"
git -C "$repo" diff -- 02-foundation/va_pipeline scripts > "$root/source_changes.patch"
shasum -a 256 "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/PhaseTwo"*.scala "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/CorpusExternalSort.scala" "$repo/scripts/run_phase2_corpus.sh" "$config" > "$root/source_sha256.txt"
calibration_arg=""
if [[ -n "$calibration" ]]; then
  [[ "$calibration" =~ ^/[A-Za-z0-9_./-]+$ && -s "$calibration" ]] || exit 1
  calibration_arg=" --calibration $calibration"
  shasum -a 256 "$calibration" >> "$root/source_sha256.txt"
fi
if [[ -n "$reused" ]]; then
  [[ "$reused" =~ ^/[A-Za-z0-9_./-]+$ && -s "$reused/REUSED_SELECTION_COMPLETE" ]] || exit 1
  calibration_arg+=" --reused $reused"
  shasum -a 256 "$reused/selected.csv" >> "$root/source_sha256.txt"
fi
export JVM_OPTS='-Xms256m -Xmx1024m -XX:ActiveProcessorCount=4'
printf 'stage,utc,free_kib\n' > "$root/storage_gates.csv"
gate() {
  local free_kib
  [[ ! -e "$root/LOW_DISK_STOP" ]] || return 1
  free_kib="$(df -Pk "$root" | awk 'NR==2 {print $4}')"
  printf '%s,%s,%s\n' "$1" "$(date -u '+%FT%TZ')" "$free_kib" >> "$root/storage_gates.csv"
  [[ "$free_kib" =~ ^[0-9]+$ ]] && (( free_kib >= 41943040 ))
}
run_scala() {
  local stage="$1" command="$2"
  gate "$stage"
  printf '%s\n' "$stage" > "$root/STATUS"
  (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -l sbt -batch "$command") > "$root/logs/$stage.log" 2>&1
}
run_scala generate "runMain org.universalledger.foundation.va.corpus.PhaseTwoCorpus --config $config --base $repo/data/synthetic/connected_reporting_v1 --histories $repo/data/synthetic/reclass_history_v1 --out $root/inputs --chunk $chunk$calibration_arg"
# Cutoffs are fixed operands within the fictional calendar, not the host clock.
run_scala early_inspection "runMain org.universalledger.foundation.va.corpus.PhaseTwoCheck --input $root/inputs --out $root/early --cutoff 2030-09-01T23:59:59Z"
run_scala later_inspection "runMain org.universalledger.foundation.va.corpus.PhaseTwoCheck --input $root/inputs --out $root/later --cutoff 2045-06-30T23:59:59Z"
run_scala source_assessment "runMain org.universalledger.foundation.va.corpus.CorpusSourceAssessment --input $root/inputs --out $root/source_assessment"
IFS=, read -ra year_list <<< "$years"
paths=""
for year in "${year_list[@]}"; do
  gate "financial_$year"
  printf 'financial_%s\n' "$year" > "$root/STATUS"
  bash "$repo/scripts/run_pipeline.sh" --inPath "$root/inputs/VARawFiles" --poPath "$root/inputs/VAPOData" --outPath "$root/output/FY$year" --years "$year" --steps ttl,2p,2m,3,20,21 --ttlType B --vendorMaster "$root/inputs/VendorMaster.csv" --periodGranularity monthly --curve CCP2 --profile fictional-phase2-v1 --materialization persist-sjes --config fictional-phase2-v1 > "$root/logs/financial_$year.log" 2>&1
  paths="${paths:+$paths,}$root/output/FY$year"
done
run_scala financial_checks "runMain org.universalledger.foundation.va.corpus.PhaseTwoCheck --mode financial --input $root/inputs --output $root/output --out $root/financial_checks --chunk $chunk"
run_scala reporting_sort "runMain org.universalledger.foundation.va.corpus.ReportingSort --inPaths $paths --out $root/report_order --chunk 100000 --fanIn 16"
ordered=""
for year in "${year_list[@]}"; do ordered="${ordered:+$ordered,}$root/report_order/FY$year"; done
gate reporting
printf 'reporting\n' > "$root/STATUS"
bash "$repo/scripts/run_reporting.sh" --inPaths "$ordered" --outPath "$root/report" --inputOrder presorted --vendorMaster "$root/inputs/VendorMaster.csv" --refPath "$root/inputs" --syntheticHistories "$root/inputs/synthetic_histories" --syntheticAssignments "$root/inputs/synthetic_assignments.csv" --syntheticAsOf 2031-06-30 --syntheticRunDate 2045-07-01 --aggregateKeys 50000 --syntheticAggregateKeys 10000 --mergeFanIn 16 > "$root/logs/reporting.log" 2>&1
run_scala connected_checks "runMain org.universalledger.foundation.va.corpus.ConnectedViewsCheck --report $root/report"
run_scala report_census "runMain org.universalledger.foundation.va.corpus.CorpusAdmissionSummary --report $root/report --out $root/census"
gate phase3_reserve
printf 'financial-and-source-controls-passed\n' > "$root/CORPUS_EVALUATED"
printf 'complete\n' > "$root/STATUS"
kill "$sampler_pid" 2>/dev/null || true
sampler_pid=""
trap - EXIT
}
main "$@"
