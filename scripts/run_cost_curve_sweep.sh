#!/usr/bin/env bash
# Run a manifest of cost-curve points with minimal agent interaction.
# The manifest is the experiment plan; this script spends machine time executing it.
#
# Manifest columns (no commas inside fields):
#   point_id,axis_value,curve,config_description,years,steps
#
# Usage:
#   bash scripts/run_cost_curve_sweep.sh \
#     --manifest scripts/cost_curve_manifest.example.csv \
#     --data-root /path/to/runtime-data

set -euo pipefail

MANIFEST=""
DATA_ROOT="${UL_DATA_ROOT:-data}"
MAX_CONCURRENT=2
KEEP_OUTPUT=0
DRY_RUN=0
SWEEP_LOG=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --manifest) MANIFEST="$2"; shift 2 ;;
    --data-root) DATA_ROOT="$2"; shift 2 ;;
    --max-concurrent) MAX_CONCURRENT="$2"; shift 2 ;;
    --keep-output) KEEP_OUTPUT=1; shift ;;
    --sweep-log) SWEEP_LOG="$2"; shift 2 ;;
    --dry-run) DRY_RUN=1; shift ;;
    -h|--help)
      sed -n '2,16p' "$0"
      exit 0
      ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

if [[ -z "$MANIFEST" || ! -f "$MANIFEST" ]]; then
  echo "Manifest is required: --manifest PATH" >&2
  exit 1
fi

DATA_ROOT="${DATA_ROOT%/}"
SWEEP_LOG="${SWEEP_LOG:-$DATA_ROOT/cost_curve_sweep.csv}"
mkdir -p "$(dirname "$SWEEP_LOG")"

if [[ ! -f "$SWEEP_LOG" ]]; then
  echo "point_id,axis_value,curve,config_description,years,steps,run_id,status,run_timestamp" > "$SWEEP_LOG"
fi

header_seen=0
point_count=0
wipe_next=1

while IFS=',' read -r point_id axis_value curve config_description years steps extra; do
  [[ -z "$point_id" || "${point_id:0:1}" == "#" ]] && continue
  if [[ "$header_seen" -eq 0 ]]; then
    header_seen=1
    [[ "$point_id" == "point_id" ]] || { echo "Manifest header must begin with point_id" >&2; exit 1; }
    continue
  fi
  [[ -n "$extra" ]] && { echo "Manifest row $point_id has too many columns" >&2; exit 1; }
  for value in "$axis_value" "$curve" "$config_description" "$years" "$steps"; do
    [[ "$value" == *","* ]] && { echo "Manifest row $point_id contains an unsupported comma" >&2; exit 1; }
  done

  wipe_arg=()
  if [[ "$wipe_next" -eq 1 && "$KEEP_OUTPUT" -eq 0 ]]; then
    wipe_arg=(--wipe-output)
  fi
  wipe_next=0
  point_count=$((point_count + 1))

  echo "======================================================================"
  echo "Cost-curve point $point_count: $point_id (axis=$axis_value, curve=$curve)"
  echo "======================================================================"

  if [[ "$DRY_RUN" -eq 1 ]]; then
    echo "DRY RUN -- would execute years=$years steps=$steps config=$config_description"
    continue
  fi

  run_timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
  bash scripts/run_pipeline_orchestrator.sh \
    --years "$years" \
    --curve "$curve" \
    --config "$config_description" \
    --max-concurrent "$MAX_CONCURRENT" \
    --data-root "$DATA_ROOT" \
    "${wipe_arg[@]}"

  cost_file="$DATA_ROOT/cost_surface.csv"
  [[ -f "$cost_file" ]] || { echo "Missing cost surface: $cost_file" >&2; exit 1; }
  latest_row=$(tail -n 1 "$cost_file")
  run_id="${latest_row%%,*}"
  [[ -n "$run_id" && "$run_id" != "$latest_row" ]] || { echo "Could not identify run_id for $point_id" >&2; exit 1; }
  echo "$point_id,$axis_value,$curve,$config_description,$years,$steps,$run_id,COMPLETE,$run_timestamp" >> "$SWEEP_LOG"
done < "$MANIFEST"

[[ "$point_count" -gt 0 ]] || { echo "Manifest contains no experiment points" >&2; exit 1; }
echo "Sweep complete: $point_count point(s)"
echo "Sweep log: $SWEEP_LOG"
