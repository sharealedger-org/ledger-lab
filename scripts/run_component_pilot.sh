#!/usr/bin/env bash
# Stable orchestration only; Scala owns all generation, source data and controls.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
root=""; baseline=""
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || exit 1
  case "$1" in
    --out) root="$2"; shift 2 ;;
    --baselineRoot) baseline="$2"; shift 2 ;;
    *) echo 'Usage: run_component_pilot.sh --out NEW_ABSOLUTE_ROOT [--baselineRoot COMPLETED_ROOT]' >&2; exit 1 ;;
  esac
done
[[ "$root" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$root" ]] || exit 1
check_options=""
if [[ -n "$baseline" ]]; then
  [[ "$baseline" =~ ^/[A-Za-z0-9_./-]+$ && -s "$baseline/PILOT_COMPLETE" ]] || exit 1
  check_options=" --baselineRoot $baseline"
fi
mkdir -p "$root/logs"
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
trap 'rc=$?; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$root/FAILED"; fi' EXIT
printf 'git=%s\nworkload=component-pilot-0304-lineage-v2\nbaseline=%s\n' "$(git -C "$repo" rev-parse HEAD)" "$baseline" > "$root/run_config.txt"
git -C "$repo" diff -- 02-foundation/va_pipeline scripts/run_component_pilot.sh > "$root/source_changes.patch"
# Untracked new source hashes supplement the tracked diff.
shasum -a 256 "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/ComponentPilot"*.scala "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/standardizeAndSort.scala" "$repo/scripts/run_component_pilot.sh" > "$root/source_sha256.txt"
run_scala() {
  local log="$1" command="$2"
  if [[ "$(uname -s)" == Darwin ]]; then
    (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -l sbt -batch "$command") > "$log" 2>&1
  else
    (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -v sbt -batch "$command") > "$log" 2>&1
  fi
}
run_scala "$root/logs/generate.log" "runMain org.universalledger.foundation.va.corpus.ComponentPilot --spec $repo/data/synthetic/component_pilot_0304_v1 --base $repo/data/synthetic/connected_reporting_v1 --histories $repo/data/synthetic/reclass_history_v1 --out $root/inputs --chunk 2"
bash "$repo/scripts/run_full_corpus.sh" --root "$root/inputs" --outRoot "$root/output" --logs "$root/logs/financial" --years 03,04 --lanes 1 --chainWidth 1 --periodGranularity monthly --ttlType B --vendorMaster "$root/inputs/VendorMaster.csv" --noDashboard > "$root/logs/financial_pipeline.log" 2>&1
bash "$repo/scripts/run_reporting.sh" --inPaths "$root/output/FY03,$root/output/FY04" --outPath "$root/report" --vendorMaster "$root/inputs/VendorMaster.csv" --refPath "$root/inputs" --syntheticHistories "$root/inputs/synthetic_histories" --syntheticAssignments "$root/inputs/synthetic_assignments.csv" --syntheticAsOf 2003-06-30 --syntheticRunDate 2004-07-01 --spillRows 2 --aggregateKeys 2 --syntheticAggregateKeys 2 --mergeFanIn 2 > "$root/logs/reporting.log" 2>&1
run_scala "$root/logs/check.log" "runMain org.universalledger.foundation.va.corpus.ComponentPilotCheck --root $root --requireLineage true$check_options"
printf 'component-pilot-0304-lineage-v2\n' > "$root/PILOT_COMPLETE"
trap - EXIT
echo "Pilot complete: $root"
