#!/usr/bin/env bash
# Bounded report practicality probe. Scala creates/validates fixtures and projections.
set -euo pipefail
set -m
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
[[ $# -eq 2 && "$1" == --out && "$2" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$2" ]] || exit 1
root="$2"; mkdir -p "$root"
export JVM_OPTS="${JVM_OPTS:-} -Xms256m -Xmx1024m -XX:ActiveProcessorCount=4"
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
trap 'rc=$?; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$root/FAILED"; fi' EXIT
printf 'git=%s\nheap_mib=1024\nactive_processors=4\nreport_timeout_seconds=900\nprobe_root_limit_kib=20971520\n' "$(git -C "$repo" rev-parse HEAD)" > "$root/config.txt"
shasum -a 256 "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/ReportingHotKeyProbe.scala" "$repo/scripts/run_reporting_hot_key_probe.sh" > "$root/probe_source_sha256.txt"
run_scala() {
  local log="$1" command="$2"
  if [[ "$(uname -s)" == Darwin ]]; then
    (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -l sbt -batch "$command") > "$log" 2>&1
  else
    (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -v sbt -batch "$command") > "$log" 2>&1
  fi
}
for case_name in repetitive_500k matched_2m diverse_500k; do
  case "$case_name" in repetitive_500k) shape=repetitive; rows=500000 ;; matched_2m) shape=matched; rows=2000000 ;; diverse_500k) shape=diverse; rows=500000 ;; esac
  case_root="$root/$case_name"
  echo "Preparing $case_name"
  run_scala "$root/$case_name.prepare.log" "runMain org.universalledger.foundation.va.corpus.ReportingHotKeyProbe --mode prepare --out $case_root --movementRows $rows --shape $shape"
  printf 'epoch,root_kib\n' > "$case_root/occupancy.csv"
  started=$(date +%s)
  bash "$repo/scripts/run_reporting.sh" --inPaths "$case_root/input" --outPath "$case_root/report" --refPath "$case_root" --syntheticHistories "$repo/data/synthetic/reclass_history_v1" --syntheticAssignments "$case_root/assignments.csv" --syntheticAsOf 2012-06-30 --syntheticRunDate 2012-07-01 --aggregateKeys 50000 --syntheticAggregateKeys 10000 --spillRows 50000 --mergeFanIn 16 > "$root/$case_name.report.log" 2>&1 &
  report_pid=$!
  while kill -0 "$report_pid" 2>/dev/null; do
    now=$(date +%s); root_kib=$(du -sk "$case_root" | awk '{print $1}')
    printf '%s,%s\n' "$now" "$root_kib" >> "$case_root/occupancy.csv"
    if (( now-started > 900 || root_kib > 20971520 )); then
      printf 'limit_exceeded elapsed=%s root_kib=%s\n' "$((now-started))" "$root_kib" > "$case_root/LIMIT_EXCEEDED"
      kill -TERM -- "-$report_pid" 2>/dev/null || true
      wait "$report_pid" || true
      exit 1
    fi
    sleep 5
  done
  wait "$report_pid"
  run_scala "$root/$case_name.inspect.log" "runMain org.universalledger.foundation.va.corpus.ReportingHotKeyProbe --mode inspect --out $case_root"
  echo "Passed $case_name"
done
run_scala "$root/project.log" "runMain org.universalledger.foundation.va.corpus.ReportingHotKeyProbe --mode project --out $root --audit $repo/data/test_evidence/identity_audit_20261114"
printf 'report-zero-key-projection-v1\n' > "$root/PROBE_COMPLETE"
trap - EXIT
