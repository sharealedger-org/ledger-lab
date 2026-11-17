#!/usr/bin/env bash
# One real-year resource/control admission for the fused Scala topology.
# No accounting or view logic here. Default is check-only.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
mode=check; root=""; year=03
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || exit 2
  case "$1" in --mode) mode="$2";; --out) root="$2";; --year) year="$2";; *) exit 2;; esac
  shift 2
done
[[ "$mode" == check || "$mode" == run ]] || exit 2
[[ "$year" =~ ^(0[3-9]|1[0-6])$ ]] || exit 2
historical="$repo/session-run.tmp/corpus_admission_20261115_receipts_v2/inputs"
wholly="$repo/session-run.tmp/phase2_wholly_sources_v1"
retained="$repo/session-run.tmp/retained_phase1_evidence_v1"
project="$repo/02-foundation/va_pipeline"
classes="$project/target/scala-2.12/classes"
dependencies="$project/target/streams/compile/dependencyClasspath/_global/streams/export"
[[ -f "$wholly/WHOLLY_SOURCE_SUBSET_COMPLETE" && -f "$historical/INPUTS_COMPLETE" && -f "$dependencies" ]]
[[ -f "$classes/org/universalledger/foundation/va/corpus/FusedFinancialYear.class" ]]
free_kib="$(df -Pk "$repo" | awk 'NR==2{print $4}')"
# Conservative planning only: source byte footprint, wrapper/state margin and
# generated-input allowance. The run measures its actual peak independently.
budget_kib="$(awk -F, 'NR==1{for(i=1;i<=NF;i++)h[$i]=i;next}{for(i=1;i<=NF;i++)gsub(/"/,"",$i);if($h["pass_id"]=="2m"){n++;b=$h["output_bytes"];r=$h["output_rows"]}}END{if(n!=1)exit 1;printf "%.0f",(4*b+512*r+4294967296)/1024}' "$retained/financial_controls/FY$year/metrics/process_metrics.csv")"
printf 'mode=%s year=%s free_kib=%s worker_budget_kib=%s reserve_kib=41943040\n' "$mode" "$year" "$free_kib" "$budget_kib"
((free_kib >= budget_kib + 41943040))
[[ "$mode" == run ]] || exit 0
[[ "$root" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$root" ]]
[[ "$(ps -p $$ -o pgid= | tr -d ' ')" == "$$" ]] || { echo 'Launch in its own process group.' >&2; exit 2; }
pmset -g batt | head -n 1 | grep -q 'AC Power'
mkdir -p "$root/logs" "$root/workers" "$root/years"
printf 'phase2-fused-v1\n' > "$root/PROFILE"
printf 'input_hash_verification\n' > "$root/STATUS"
printf 'git=%s\nyear=%s\nheap_max_mib=2048\nactive_processors=1\nworker_budget_kib=%s\nreserve_kib=41943040\nentry_point=org.universalledger.foundation.va.corpus.FusedFinancialYear\n' "$(git -C "$repo" rev-parse HEAD)" "$year" "$budget_kib" > "$root/run_config.txt"
cp "$0" "$root/executed_driver.sh"
monitor_pid=""
trap 'rc=$?; [[ -z "$monitor_pid" ]] || kill "$monitor_pid" 2>/dev/null || true; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$root/FAILED"; fi' EXIT
caffeinate -i -s -w $$ &
bash "$repo/scripts/monitor_synthetic_corpus_v2.sh" "$root" "$$" 60 41943040 > "$root/logs/monitor.log" 2>&1 &
monitor_pid=$!
(cd "$repo" && shasum -a 256 -c data/test_evidence/phase2_derived_preparation_20261116/subset_sha256.txt) > "$root/logs/subset_hashes.txt" 2>&1
shasum -a 256 -c "$historical/prepared_input_sha256.txt" > "$root/logs/historical_hashes.txt" 2>&1
new="$(printf '%02d' "$((10#$year+28))")"
printf 'fused_financial_year_%s\n' "$year" > "$root/STATUS"
(cd "$project" && /usr/bin/time -l java -Dfile.encoding=UTF-8 -Xms512m -Xmx2048m -XX:ActiveProcessorCount=1 \
  -cp "$classes:$(cat "$dependencies")" org.universalledger.foundation.va.corpus.FusedFinancialYear \
  --historical "$historical" --wholly "$wholly" --retained "$retained" \
  --sortSpec "$repo/02-foundation/sort_specs/SortedJE.sortspec" --year "20$year" \
  --out "$root/years/FY$new" --statusFile "$root/workers/FY$new.status") > "$root/logs/FY$year.log" 2>&1
[[ -f "$root/years/FY$new/FINANCIAL_YEAR_COMPLETE" ]]
printf 'fused_admission_complete_controls_passed\n' > "$root/STATUS"
printf 'one_year_only_full_corpus_and_reporting_pending\n' > "$root/ADMISSION_COMPLETE"
