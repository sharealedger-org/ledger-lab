#!/usr/bin/env bash
# Separate expanded reporting after every requested corpus run validates.
# --wait supports attaching this gate to an already-running, frozen queue.
# No posting, financial calculations, output copying, or retries of failed runs.
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
queue=""; out=""; ref=""; wait_for_queue=0; options=()
while [[ $# -gt 0 ]]; do
  case "$1" in
    --queueRoot) queue="$2"; shift 2 ;;
    --outPath) out="$2"; shift 2 ;;
    --refPath) ref="$2"; shift 2 ;;
    --wait) wait_for_queue=1; shift ;;
    --vendorMaster|--contractDimensions|--packetVendors|--spillRows|--mergeFanIn|--maxVendorRows|--maxReferenceBytes|--maxInputs)
      options+=("$1" "$2"); shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done
[[ "$queue" = /* && -s "$queue/plan.tsv" && -s "$queue/config.txt" && "$out" = /* ]] || {
  echo "Existing absolute queueRoot and new absolute outPath required" >&2; exit 1;
}
[[ ! -e "$out" && ! -e "$out.attempt" ]] || { echo "Reporting output already exists" >&2; exit 1; }
years="$(sed -n 's/^years=//p' "$queue/config.txt")"
[[ "$years" =~ ^[0-9][0-9](,[0-9][0-9])*$ ]] || exit 1
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
while :; do
  [[ ! -f "$queue/STOP" ]] || { echo "Queue STOP requested; reporting not started" >&2; exit 2; }
  if [[ -f "$queue/status.csv" ]] && awk -F, '$2 ~ /^STOPPED_/ {failed=1} END {exit !failed}' "$queue/status.csv"; then
    echo "Queue failed; reporting not started" >&2; exit 1
  fi
  ready=1; selected=""; family=""
  while IFS=$'\t' read -r name granularity source_family; do
    [[ "$name" =~ ^[A-Za-z0-9_-]+$ && "$source_family" =~ ^[ERB]$ ]] || exit 1
    selected="$name"; family="$source_family"
    if [[ ! -s "$queue/status.csv" ]] || ! awk -F, -v name="$name" '
      $1==name {state=$2} END {exit state!="VALIDATED"}' "$queue/status.csv"; then ready=0; fi
  done < "$queue/plan.tsv"
  [[ -n "$selected" ]] || exit 1
  if (( ready )); then break; fi
  (( wait_for_queue )) || { echo "Queue not fully validated; reporting not started" >&2; exit 1; }
  printf '%s Waiting for all corpus runs, including final-year posting\n' "$(date -u +%FT%TZ)"
  sleep 600
done
# Select exactly one successful population. Pin immutable physical generations,
# rather than mutable FY symlinks, before any reporting process opens its inputs.
source_root="$queue/$selected/output"
bash "$script_dir/validate_corpus_run.sh" "$source_root" "$years" "$family"
inputs=""
IFS=, read -ra year_list <<< "$years"
for year in "${year_list[@]}"; do
  physical="$(cd "$source_root/FY$year" && pwd -P)"
  inputs="${inputs:+$inputs,}$physical"
done
[[ -z "$ref" ]] || options+=(--refPath "$ref")
printf '%s Starting separately measured expanded reporting from %s\n' "$(date -u +%FT%TZ)" "$selected"
bash "$script_dir/run_reporting.sh" --inPaths "$inputs" --outPath "$out" ${options[@]+"${options[@]}"}
