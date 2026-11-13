#!/usr/bin/env bash
# Unattended full-run queue. Bash orchestration only; no financial calculations.
# Usage: run_corpus_queue.sh --root RAW_ROOT --queueRoot NEW_DIR --plan PLAN.tsv
#        [--vendorMaster FILE] [--years 03,...,16] [--minFreeGiB 140]
# Plan: TAB-separated run_name, granularity (monthly|annual), TTL type (E|R|B); no header.
# Every run gets independent output/log directories. No automatic deletion or retry.
# Create QUEUE_ROOT/STOP to stop before the next run; the current run completes.
set -euo pipefail
script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo="$(cd "$script_dir/.." && pwd)"
root=""; queue=""; plan=""; master=""
years="03,04,05,06,07,08,09,10,11,12,13,14,15,16"
minimum=140
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) root="$2"; shift 2 ;;
    --queueRoot) queue="$2"; shift 2 ;;
    --plan) plan="$2"; shift 2 ;;
    --vendorMaster) master="$2"; shift 2 ;;
    --years) years="$2"; shift 2 ;;
    --minFreeGiB) minimum="$2"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done
[[ -d "$root/VARawFiles" && -d "$root/VAPOData" && -s "$plan" && -n "$queue" ]] || {
  echo "Raw root, PO root, nonempty plan, and new queueRoot are required" >&2; exit 1;
}
[[ "$minimum" =~ ^[0-9]+$ && "$years" =~ ^[0-9][0-9](,[0-9][0-9])*$ ]] || exit 1
[[ -z "$master" || -s "$master" ]] || { echo "Missing vendor master" >&2; exit 1; }
# The legacy sbt invocation does not safely quote embedded whitespace in runtime paths.
for path in "$root" "$queue" "$master"; do
  [[ "$path" != *[[:space:]]* ]] || { echo "Use runtime paths without whitespace: $path" >&2; exit 1; }
done
[[ ! -e "$queue" ]] || { echo "Queue directory already exists; choose a new one" >&2; exit 1; }
awk -F '\t' 'NF!=3 || $1!~/^[A-Za-z0-9_-]+$/ || ($2!="monthly" && $2!="annual") || $3!~/^[ERB]$/ || seen[$1]++ { exit 1 }' "$plan" || {
  echo "Invalid queue plan (three tab-separated fields, unique run names)" >&2; exit 1;
}
git -C "$repo" diff --quiet -- scripts 02-foundation/va_pipeline || { echo "Commit engine/orchestration changes first" >&2; exit 1; }
git -C "$repo" diff --cached --quiet -- scripts 02-foundation/va_pipeline || exit 1
mkdir -p "$(dirname "$queue")"
mkdir "$queue"  # Atomic claim: a second launcher must not share this queue.
cp "$plan" "$queue/plan.tsv"
revision="$(git -C "$repo" rev-parse HEAD)"
printf 'revision=%s\nroot=%s\nyears=%s\nminimum_free_GiB=%s\n' "$revision" "$root" "$years" "$minimum" > "$queue/config.txt"
if [[ -n "$master" ]]; then
  cp "$master" "$queue/vendor_master.csv"
  master="$queue/vendor_master.csv"
  shasum -a 256 "$master" > "$queue/vendor_master.sha256"
fi
printf 'run,status,time\n' > "$queue/status.csv"
current=initialization
trap 'rc=$?; if [[ $rc -ne 0 ]]; then printf "%s,STOPPED_%s,%s\n" "$current" "$rc" "$(date -u +%FT%TZ)" >> "$queue/status.csv"; fi' EXIT
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
while IFS=$'\t' read -r current granularity family; do
  [[ ! -f "$queue/STOP" ]] || { echo "STOP requested before $current"; exit 2; }
  [[ "$(git -C "$repo" rev-parse HEAD)" == "$revision" ]] || { echo "Code revision changed" >&2; exit 1; }
  git -C "$repo" diff --quiet -- scripts 02-foundation/va_pipeline || exit 1
  git -C "$repo" diff --cached --quiet -- scripts 02-foundation/va_pipeline || exit 1
  available="$(df -Pk "$queue" | awk 'END {print $4}')"
  [[ "$available" =~ ^[0-9]+$ ]] && (( available >= minimum * 1024 * 1024 )) || {
    echo "Insufficient free space before $current; outputs retained" >&2; exit 1;
  }
  run_dir="$queue/$current"
  mkdir -p "$run_dir/logs"
  printf '%s,RUNNING,%s\n' "$current" "$(date -u +%FT%TZ)" >> "$queue/status.csv"
  args=()
  [[ -z "$master" ]] || args=(--vendorMaster "$master")
  bash "$script_dir/run_full_corpus.sh" --root "$root" --outRoot "$run_dir/output" \
    --logs "$run_dir/logs" --years "$years" --ttlType "$family" \
    --periodGranularity "$granularity" "${args[@]}" > "$run_dir/console.log" 2>&1
  bash "$script_dir/validate_corpus_run.sh" "$run_dir/output" "$years" "$family" > "$run_dir/validation.txt" 2>&1
  # Hash compact final products; retain every SJE and generation, with no automatic cleanup.
  IFS=, read -ra year_list <<< "$years"
  : > "$run_dir/final_sha256.txt"
  for year in "${year_list[@]}"; do
    for file in "LDGR20${year}.csv" "GL20${year}.csv" "PERM_TBL20${year}.csv"; do
      (cd "$run_dir/output" && shasum -a 256 "FY$year/$file") >> "$run_dir/final_sha256.txt"
    done
  done
  (cd "$run_dir/output" && shasum -a 256 VIEWS/view_*.csv) >> "$run_dir/final_sha256.txt"
  reference="$queue/reference_${granularity}_${family}.sha256"
  if [[ -f "$reference" ]]; then
    cmp "$reference" "$run_dir/final_sha256.txt" || {
      echo "Repeat output mismatch for $current; stopping with evidence retained" >&2; exit 1;
    }
  else
    cp "$run_dir/final_sha256.txt" "$reference"
  fi
  printf '%s,VALIDATED,%s\n' "$current" "$(date -u +%FT%TZ)" >> "$queue/status.csv"
done < "$queue/plan.tsv"
echo "All requested runs validated; see $queue/status.csv"
