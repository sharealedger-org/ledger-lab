#!/usr/bin/env bash
# Read-only observer of a reporting attempt. No financial computation or control.
set -euo pipefail
mode=${1:?snapshot or watch}; root=${2:?absolute reporting root}; evidence=${3:?absolute evidence directory}
[[ "$root" = /* && "$evidence" = /* && -d "$root" ]] || exit 2
mkdir -p "$evidence"
snapshot() {
  local label=$1 target="$evidence/$1" relative report file
  mkdir "$target"
  date -u '+%Y-%m-%dT%H:%M:%SZ' > "$target/captured_utc.txt"
  for relative in STATUS FAILED WORKFLOW_COMPLETE REPORT_CHECKS_COMPLETE CENSUS_COMPLETE run_config.txt executed_driver.sh reporting_classes_sha256.txt monitor_storage.csv monitor_processes.log report_admission.csv report_order/sort_manifest.csv; do
    if [[ -f "$root/$relative" ]]; then
      mkdir -p "$target/$(dirname "$relative")"
      cp -p "$root/$relative" "$target/$relative"
    fi
  done
  mkdir "$target/logs"
  for file in "$root"/logs/*.log; do [[ ! -f "$file" ]] || cp -p "$file" "$target/logs/"; done
  report="$root/report.attempt"; [[ ! -d "$root/report" ]] || report="$root/report"
  if [[ -d "$report" ]]; then
    mkdir "$target/report_metadata"
    for file in "$report"/*metrics.csv "$report"/run_config.txt "$report"/reporting_source_sha256.txt "$report"/source_changes.patch "$report"/EXTRACT_COMPLETE "$report"/REPORT_COMPLETE "$report"/FAILED "$report"/extract.log "$report"/reduce.log "$report"/input_manifest.csv; do
      [[ ! -f "$file" ]] || cp -p "$file" "$target/report_metadata/"
    done
    find "$report" -type f -exec stat -f '%z %m %N' {} + > "$target/file_inventory_bytes_mtime_path.txt"
  fi
  (cd "$target" && find . -type f ! -name SHA256SUMS -exec shasum -a 256 {} +) > "$target/SHA256SUMS"
}
sample() {
  local report="$root/report.attempt" file count=0 bytes=0 amount
  [[ ! -d "$root/report" ]] || report="$root/report"
  for file in "$report"/spill_0_*.csv; do
    [[ -f "$file" ]] || continue
    amount=$(stat -f '%z' "$file" 2>/dev/null) || continue
    count=$((count+1)); bytes=$((bytes+amount))
  done
  printf '%s,%s,%s\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')" "$count" "$bytes" >> "$evidence/reducer_input_samples.csv"
}
case "$mode" in
  snapshot) snapshot initial ;;
  watch)
    pid=${4:?driver PID}
    printf 'utc,initial_reducer_files,initial_reducer_bytes\n' > "$evidence/reducer_input_samples.csv"
    while :; do
      sample
      if [[ -f "$root/WORKFLOW_COMPLETE" || -f "$root/FAILED" ]]; then break; fi
      command_line=$(ps -p "$pid" -o command= 2>/dev/null || true)
      if [[ "$command_line" != *run_phase2_fused_reporting.sh* || "$command_line" != *"$root"* ]]; then break; fi
      sleep 60
    done
    snapshot conclusion
    if [[ -f "$root/WORKFLOW_COMPLETE" ]]; then
      printf 'Workflow completed; independent final result acceptance and interpretation remain pending.\n' > "$evidence/CONCLUSION_CAPTURED"
    elif [[ -f "$root/FAILED" ]]; then
      printf 'Workflow failed; retain failed-attempt measurements, not accepted outputs.\n' > "$evidence/CONCLUSION_CAPTURED"
    else
      printf 'Driver no longer observed without terminal marker; outcome unknown, investigate.\n' > "$evidence/CONCLUSION_CAPTURED"
    fi
    ;;
  *) exit 2 ;;
esac
