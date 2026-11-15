#!/usr/bin/env bash
# Three controlled local executions. Scala owns financial work and report preparation.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
export PATH="/Users/ktwitchell001/.local/sbt/bin:$PATH"
inputs=""; out=""; years=03,04,05,06,07,08,09,10,11,12,13,14,15,16
minimum=160
while [[ $# -gt 0 ]]; do
  case "$1" in
    --inputs) inputs="$2"; shift 2 ;;
    --out) out="$2"; shift 2 ;;
    --years) years="$2"; shift 2 ;;
    --minFreeGiB) minimum="$2"; shift 2 ;;
    *) echo "Unknown option: $1" >&2; exit 2 ;;
  esac
done
[[ -s "$inputs/INPUTS_COMPLETE" && "$out" = /* && ! -e "$out" && "$minimum" =~ ^[0-9]+$ ]] || exit 2
[[ -s "$inputs/receipt_policy/SOURCE_POLICY_COMPLETE" ]] || { echo "Receipt-safe source policy required" >&2; exit 2; }
for path in "$inputs" "$out"; do [[ "$path" != *[[:space:]]* ]] || exit 2; done
[[ "$years" =~ ^[0-9][0-9](,[0-9][0-9])*$ ]] || exit 2
git -C "$repo" diff --quiet -- scripts 02-foundation/va_pipeline || exit 2
git -C "$repo" diff --cached --quiet -- scripts 02-foundation/va_pipeline || exit 2
mkdir "$out"
revision="$(git -C "$repo" rev-parse HEAD)"
printf 'revision=%s\ninputs=%s\nyears=%s\nminimum_free_GiB=%s\n' "$revision" "$inputs" "$years" "$minimum" > "$out/config.txt"
printf 'run,status,utc\n' > "$out/status.csv"
current=initialization
trap 'rc=$?; if ((rc)); then printf "%s,FAILED_%s,%s\n" "$current" "$rc" "$(date -u +%FT%TZ)" >> "$out/status.csv"; fi' EXIT
if command -v caffeinate >/dev/null; then caffeinate -i -s -w $$ & fi
# The installed sbt launcher reads JAVA_OPTS (not JVM_OPTS).
export JAVA_OPTS='-Dfile.encoding=UTF-8 -Xms1024m -Xmx1024m -XX:ActiveProcessorCount=4'
scala() { (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -l sbt -batch "$1") > "$2" 2>&1; }
IFS=, read -ra year_list <<< "$years"
for current in monthly_a monthly_b monthly_c; do
  [[ ! -e "$out/STOP" && "$(git -C "$repo" rev-parse HEAD)" == "$revision" ]] || exit 2
  git -C "$repo" diff --quiet -- scripts 02-foundation/va_pipeline || exit 2
  available="$(df -Pk "$out" | awk 'END {print $4}')"
  ((available >= minimum * 1024 * 1024)) || { echo "Scratch admission failed before $current" >&2; exit 2; }
  LC_ALL=C shasum -a 256 -c "$inputs/prepared_input_sha256.txt" > "$out/$current.input_check.log"
  run="$out/$current"; mkdir -p "$run/logs"
  printf '%s,RUNNING,%s\n' "$current" "$(date -u +%FT%TZ)" >> "$out/status.csv"
  bash "$repo/scripts/run_full_corpus.sh" --root "$inputs" --outRoot "$run/output" --logs "$run/logs" \
    --years "$years" --ttlType B --periodGranularity monthly --vendorMaster "$inputs/VendorMaster.csv" \
    --lanes 4 --chainWidth 2 --noDashboard > "$run/financial_console.log" 2>&1
  bash "$repo/scripts/validate_corpus_run.sh" "$run/output" "$years" B > "$run/financial_validation.log" 2>&1
  paths=""
  for year in "${year_list[@]}"; do
    physical="$(cd "$run/output/FY$year" && pwd -P)"; paths="${paths:+$paths,}$physical"
  done
  scala "runMain org.universalledger.foundation.va.corpus.CorpusReportingPreparation --inPaths $paths --histories $inputs/synthetic_histories --out $run/preparation" "$run/preparation.log"
  assignments="$out/monthly_a/preparation/synthetic_assignments.csv"
  cmp "$assignments" "$run/preparation/synthetic_assignments.csv" > "$run/assignment_comparison.log" 2>&1
  printf '%s,REPORTING,%s\n' "$current" "$(date -u +%FT%TZ)" >> "$out/status.csv"
  bash "$repo/scripts/run_reporting.sh" --inPaths "$paths" --outPath "$run/report" \
    --vendorMaster "$inputs/VendorMaster.csv" --refPath "$inputs" --syntheticHistories "$inputs/synthetic_histories" \
    --syntheticAssignments "$assignments" --syntheticAsOf 2012-06-30 --syntheticRunDate 2016-07-01 \
    --aggregateKeys 50000 --syntheticAggregateKeys 10000 --spillRows 50000 --mergeFanIn 16 > "$run/reporting.log" 2>&1
  scala "runMain org.universalledger.foundation.va.corpus.ConnectedViewsCheck --report $run/report" "$run/report_validation.log"
  scala "runMain org.universalledger.foundation.va.corpus.CorpusAdmissionSummary --report $run/report --out $run/admission_summary" "$run/admission_summary.log"
  : > "$run/products.sha256"
  for year in "${year_list[@]}"; do
    (cd "$run/output" && LC_ALL=C shasum -a 256 "FY$year/SortedJEFY${year}merged.csv" "FY$year/LDGR20${year}.csv" "FY$year/GL20${year}.csv" "FY$year/PERM_TBL20${year}.csv") >> "$run/products.sha256"
  done
  (cd "$run/output" && LC_ALL=C shasum -a 256 VIEWS/view_*.csv) >> "$run/products.sha256"
  (cd "$run" && LC_ALL=C shasum -a 256 preparation/synthetic_assignments.csv) >> "$run/products.sha256"
  # Engine products have stable semantic names; operational metrics/manifests carry run-specific paths/times.
  for file in "$run"/report/*.csv; do
    name="${file##*/}"
    case "$name" in input_manifest.csv|*_metrics.csv|*_os_metrics.csv|process_metrics.csv) continue ;; esac
    (cd "$run" && LC_ALL=C shasum -a 256 "report/$name") >> "$run/products.sha256"
  done
  if [[ -f "$out/reference.sha256" ]]; then cmp "$out/reference.sha256" "$run/products.sha256" > "$run/repeat_comparison.log" 2>&1
  else cp "$run/products.sha256" "$out/reference.sha256"; fi
  du -sk "$run" > "$run/retained_allocated_kib.txt"
  date -u +%FT%TZ > "$run/EVALUATED"
  printf '%s,EVALUATED,%s\n' "$current" "$(date -u +%FT%TZ)" >> "$out/status.csv"
  if [[ "$current" != monthly_a ]]; then
    bash "$repo/scripts/deduplicate_verified_run_storage.sh" "$out/monthly_a" "$run" > "$run/storage_deduplication.log" 2>&1
  fi
done
date -u +%FT%TZ > "$out/THREE_EXECUTIONS_EVALUATED"
