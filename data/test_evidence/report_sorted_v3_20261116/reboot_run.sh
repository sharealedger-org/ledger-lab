#!/usr/bin/env bash
set -euo pipefail
repo=/Users/ktwitchell001/workspace/sharealedger/ledger-lab
root="$repo/session-run.tmp/report_sorted_v3_20261115/reboot_attempt"
inputs="$repo/session-run.tmp/corpus_admission_20261115_receipts_v2/inputs"
financial="$repo/session-run.tmp/synthetic_reference_corpus_v2_20261115/monthly_a"
export PATH="/Users/ktwitchell001/.local/sbt/bin:$PATH"
export JAVA_OPTS='-Dfile.encoding=UTF-8 -Xms1024m -Xmx1024m -XX:ActiveProcessorCount=4'
mkdir "$root"
echo $$ > "$root/driver.pid"
trap 'rc=$?; printf "%s rc=%s\n" "$(date -u +%FT%TZ)" "$rc" > "$root/EXIT_STATUS"' EXIT
caffeinate -i -s -w $$ &
bash "$repo/scripts/monitor_synthetic_corpus_v2.sh" "$root" "$$" 60 20971520 > "$root/monitor.log" 2>&1 &
git -C "$repo" diff > "$root/source_changes.patch"
cp "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/ReportingSort.scala" "$root/ReportingSort.scala"
scala() { (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -l sbt -batch "$1") > "$2" 2>&1; }
report() {
  bash "$repo/scripts/run_reporting.sh" --inPaths "$1" --outPath "$2" --inputOrder presorted --vendorMaster "$inputs/VendorMaster.csv" --refPath "$inputs" --syntheticHistories "$inputs/synthetic_histories" --syntheticAssignments "$financial/preparation/synthetic_assignments.csv" --syntheticAsOf 2012-06-30 --syntheticRunDate 2016-07-01 --aggregateKeys 50000 --syntheticAggregateKeys 10000 --spillRows 50000 --mergeFanIn 16
}
printf '%s\n' "$(date -u +%FT%TZ) FY2012_SORT" >> "$root/status.log"
scala "runMain org.universalledger.foundation.va.corpus.ReportingSort --inPaths $financial/output/FY12 --out $root/fy12_sorted" "$root/fy12_sort.log"
report "$root/fy12_sorted/FY12" "$root/fy12_report" > "$root/fy12_reporting.log" 2>&1
scala "runMain org.universalledger.foundation.va.corpus.ConnectedViewsCheck --report $root/fy12_report" "$root/fy12_checks.log"
date -u +%FT%TZ > "$root/FY12_EVALUATED"
# Real-year controls must pass before admitting the complete corpus.
[[ $(df -Pk "$root" | awk 'END {print $4}') -ge $((150*1024*1024)) ]]
paths=""
for year in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do paths="${paths:+$paths,}$financial/output/FY$year"; done
printf '%s\n' "$(date -u +%FT%TZ) CORPUS_SORT" >> "$root/status.log"
scala "runMain org.universalledger.foundation.va.corpus.ReportingSort --inPaths $paths --out $root/corpus_sorted" "$root/corpus_sort.log"
paths=""
for year in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do paths="${paths:+$paths,}$root/corpus_sorted/FY$year"; done
printf '%s\n' "$(date -u +%FT%TZ) CORPUS_REPORT" >> "$root/status.log"
report "$paths" "$root/corpus_report" > "$root/corpus_reporting.log" 2>&1
scala "runMain org.universalledger.foundation.va.corpus.ConnectedViewsCheck --report $root/corpus_report" "$root/corpus_checks.log"
scala "runMain org.universalledger.foundation.va.corpus.CorpusAdmissionSummary --report $root/corpus_report --out $root/corpus_summary" "$root/corpus_summary.log"
date -u +%FT%TZ > "$root/CORPUS_EVALUATED"
