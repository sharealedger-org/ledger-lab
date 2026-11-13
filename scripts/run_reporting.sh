#!/usr/bin/env bash
# Expanded analytical reporting only: distinct JVMs for 5v extract and 5r spill reduction.
# No source transformation, accounting logic, posting, or cost-curve mutation in this wrapper.
# Usage: run_reporting.sh --inPaths DIR[,DIR] --outPath NEW_DIR [--vendorMaster SORTED_CSV]
# Optional: --contractDimensions SORTED_CSV --refPath DIR --packetVendors ID[,ID]
# --spillRows N --mergeFanIn N --maxVendorRows N --maxReferenceBytes N --maxInputs N
# --balanceBasis period_movements|opening_plus_movements --openingFiles FILE[,FILE]
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$script_dir/.." && pwd)"
out=""; inputs=""; options=""; fan_in=16
quote() { local value="$1"; value="${value//\\/\\\\}"; value="${value//\"/\\\"}"; printf '"%s"' "$value"; }
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || { echo "Missing value for $1" >&2; exit 1; }
  key="$1"; value="$2"; shift 2
  case "$key" in
    --outPath) out="$value" ;;
    --inPaths) inputs="$value" ;;
    --mergeFanIn) fan_in="$value" ;;
    --vendorMaster|--contractDimensions|--refPath|--packetVendors|--spillRows|--maxVendorRows|--maxReferenceBytes|--maxInputs|--balanceBasis|--openingFiles)
      options+=" $key $(quote "$value")" ;;
    *) echo "Unknown option: $key" >&2; exit 1 ;;
  esac
done
[[ -n "$out" && -n "$inputs" && "$out" = /* ]] || { echo "Absolute new outPath and committed inPaths required" >&2; exit 1; }
[[ "$fan_in" =~ ^[0-9]+$ ]] && (( fan_in >= 2 && fan_in <= 128 )) || exit 1
[[ ! -e "$out" && ! -e "$out.attempt" ]] || { echo "Output/attempt exists; choose a new path" >&2; exit 1; }
IFS=, read -ra input_dirs <<< "$inputs"
for path in "${input_dirs[@]}"; do
  [[ "$path" = /* && -d "$path" ]] || { echo "Missing absolute input directory: $path" >&2; exit 1; }
  [[ -s "$path/metrics/run_manifest.csv" ]] || { echo "Input is not a committed pipeline output: $path" >&2; exit 1; }
  awk -F, '
    NR==1 { for(i=1;i<=NF;i++) if($i=="status") status=i; next }
    { value=$status; gsub(/^"|"$/, "", value); rows++; if(!status || value!="complete") bad=1 }
    END { if(!rows || bad || !status) exit 1 }
  ' "$path/metrics/run_manifest.csv" || { echo "Upstream run is not complete: $path" >&2; exit 1; }
done
mkdir -p "$(dirname "$out")"
mkdir "$out.attempt"
attempt="$out.attempt"
trap 'rc=$?; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$attempt/FAILED"; echo "Failed reporting attempt retained: $attempt" >&2; fi' EXIT
printf 'git=%s\ninputs=%s\noptions=%s\n' "$(git -C "$repo" rev-parse HEAD)" "$inputs" "$options" > "$attempt/run_config.txt"
git -C "$repo" diff -- scripts/run_reporting.sh 02-foundation/va_pipeline > "$attempt/source_changes.patch"
shasum -a 256 "$repo/scripts/run_reporting.sh" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/ReportingPass.scala" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/ReportingSupport.scala" > "$attempt/reporting_source_sha256.txt"
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
run_stage() {
  local stage="$1" command="$2"
  if [[ "$(uname -s)" == Darwin ]]; then
    (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -l sbt -batch "$command") > "$attempt/$stage.log" 2>&1
  else
    (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -v sbt -batch "$command") > "$attempt/$stage.log" 2>&1
  fi
}
run_stage extract "run --step 5v --reportProfile expanded --inPaths $(quote "$inputs") --outPath $(quote "$attempt")$options"
[[ -s "$attempt/EXTRACT_COMPLETE" ]] || exit 1
run_stage reduce "run --step 5r --outPath $(quote "$attempt") --mergeFanIn $fan_in"
[[ -s "$attempt/REPORT_COMPLETE" ]] || exit 1
mv "$attempt" "$out"
trap - EXIT
echo "Reporting complete (separate from cost curve): $out"
