#!/usr/bin/env bash
# =============================================================================
#  run_pipeline.sh — Universal Ledger ARE/AL/ALE Pipeline Runner
#
#  Runs the full Financial System Patterns pipeline (or a subset) for one
#  or more fiscal years.  Each step is independently scriptable so a human
#  learner, a shell script, or an AI agent can drive it the same way.
#
#  USAGE:
#    bash run_pipeline.sh [OPTIONS]
#
#  OPTIONS:
#    -i, --inPath   DIR    Input data root  (default: /VAdata/data/)
#        --poPath   DIR    Directory holding VA_opendata_FY20YY.txt for step 2p.
#                          Default: first of IN_PATH/poData, IN_PATH/../VAPOData,
#                          IN_PATH/VAPOData, IN_PATH that contains the file.
#        --paymentPath DIR Directory holding FY<YY>q<Q>exp.txt for step 2.
#                          Default: IN_PATH, else IN_PATH/payment when it has the file.
#    -o, --outPath  DIR    Output data root (default: /VAdata/data/)
#    -y, --years    LIST   Comma-separated two-digit fiscal years (default: 03)
#                          e.g.  --years 03,04,05
#    -s, --steps    LIST   Comma-separated step numbers to run   (default: 2,3)
#                          e.g.  --steps 2,3,5,4
#                          Steps: ttl=APATechnicalTransformLayer,
#                                 1=poToPayMatch(Spark), 2=transStandardize,
#                                 2p=POStandardize, 2m=MergePaymentPO,
#                                 3=post, 4=contraCreation(LAST),
#                                 5=dataAggregation(ViewSpec-driven pivot),
#                                 5v=CKBViews(four analytical view tables),
#                                 6=financialAllocation, 7=consolidation,
#                                 8=forecastingBudgeting,
#                                 9=arrangementReclass(before 4 and 5),
#                                 10=initFiles, 11=DPBSpark(Spark),
#                                 12=booked currency revaluation engine,
#                                 20=GLPost, 21=PermutationGenerator
#                          Experiment curve configurations (Paper 1 cost curves):
#                            CCP1 (GL + permutation table):  --steps 2,2p,2m,3,20,21
#                            CCP2 (instrument ledger):       --steps 2,2p,2m,3
#                            CCP3 (CCP2 + views):            --steps 2,2p,2m,3,5v
#                          Legacy curve configurations:
#                            C1 (Post only):              --steps 2,3
#                            C2 (Post+Analysis):          --steps 2,3,5
#                            C3 (Post+Analysis+Reval):    --steps 2,3,5,8,9
#                            C4 (Full pipeline):          --steps 2,3,5,6,7,8,9,4
#    -c, --curve    LABEL  Experiment curve label: C1, C2, C3, C4 (default: C1)
#    -d, --config   DESC   Short config description written to cost_surface.csv
#                          (e.g. "minimal-materialization" or "full-views")
#        --profile  NAME   Named workload profile for evidence and interpretation
#        --materialization NAME  Materialization policy (derive-report, persist-sjes, etc.)
#    -l, --logFile  FILE   Append control-total output to this file
#                          (default: pipeline_results.log in outPath)
#    -h, --help            Print this help and exit
#
#  STEP GUIDE (no external data or Spark required):
#    Step 2 reads FY<YY>q<quarter>exp.txt plus VendorMaster.csv from --inPath
#    and writes SortedJE*.csv to --outPath. Step 3 reads those SortedJE files
#    from --outPath and writes LDGR<YYYY>.csv. For the checked-in fixture, run:
#      bash scripts/run_repo_fixture_smoke_test.sh
#
#  EXAMPLES:
#    # C1 baseline (fixture data):
#    bash run_pipeline.sh --inPath ./data --outPath ./data/output \
#                         --years 03 --steps 2,3 --curve C1 --config post-only
#
#    # C2 Post+Analysis — measures marginal cost of reporting materialization:
#    bash run_pipeline.sh --inPath ./data --outPath ./data/output \
#                         --years 03 --steps 2,3,5 --curve C2 --config full-views
#
#    # C4 full pipeline (fixture data — requires AllocationRules.csv etc.):
#    bash run_pipeline.sh --inPath ./data --outPath ./data/output \
#                         --years 03 --steps 2,3,5,6,7,8,9,4 --curve C4 --config full
#
#    # Run FY03–FY06 C2 (full VA data required):
#    bash run_pipeline.sh --inPath /VAdata/data --outPath /VAdata/data \
#                         --years 03,04,05,06 --steps 2,3,5 --curve C2
#
#    # Run just contra creation (step 4) to verify reconciliation:
#    bash run_pipeline.sh --steps 4 --years 05
#
#  OUTPUTS (in addition to pipeline_results.log):
#    cost_surface.csv — one row per run, accumulates across runs:
#      run_id, run_timestamp, curve, config_description, years, steps,
#      total_compute_s, total_storage_bytes, master_file_count, total_cost_proxy
#    The master_file_count (AHI metric) = count of enabled=Y rows in ViewSpec.csv.
#    The total_cost_proxy = total_compute_s + (total_storage_bytes / 1e6) +
#                           (master_file_count * 10).
#    An agent can drive successive rows by varying --curve, --config, and
#    ViewSpec.csv between runs.
#
#  AGENTIC / PROGRAMMATIC USE:
#    Each step writes its control totals to stdout AND appends a structured
#    summary line to the log file.  An agent can:
#      - Call this script with different --years, --steps, --curve, --config
#      - Read pipeline_results.log for per-step timing and storage
#      - Read cost_surface.csv for the full cross-run experiment record
#      - Edit ViewSpec.csv between runs to vary materialization level
#      - Vary reference data (AllocationRules.csv etc.) between runs
#
#  NOTES:
#    - Steps 1 and 11 require Apache Spark to be installed.
#    - Steps 2 and 3 are pure Scala — no Spark required.
#    - Run step 10 (initFiles) once before the first pipeline run on new data.
#    - Step 3 (post) reads from outPath (where step 2 writes SortedJE files).
#
#  *(c) Copyright IBM Corporation. 2018
#  * SPDX-License-Identifier: Apache-2.0
#  * By Kip Twitchell
#  2025 - Scriptable pipeline runner added (Bob AI)
# =============================================================================

set -euo pipefail

# ── defaults ──────────────────────────────────────────────────────────────────
IN_PATH="/VAdata/data/"
OUT_PATH="/VAdata/data/"
YEARS="03"
STEPS="2,3"
LOG_FILE=""
CURVE="C1"
CONFIG_DESC="default"
WORKLOAD_PROFILE="legacy"
MATERIALIZATION_PROFILE="unspecified"
PERIOD_GRANULARITY="monthly"
PO_PATH=""
PAYMENT_PATH=""
SEED_FROM_COMMITTED="N"
VIEW_IN_PATHS=""

# ── arg parsing ───────────────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
  case "$1" in
    -i|--inPath)   IN_PATH="$2";     shift 2 ;;
    -o|--outPath)  OUT_PATH="$2";    shift 2 ;;
    -y|--years)    YEARS="$2";       shift 2 ;;
    -s|--steps)    STEPS="$2";       shift 2 ;;
    -l|--logFile)  LOG_FILE="$2";    shift 2 ;;
    -c|--curve)    CURVE="$2";       shift 2 ;;
    -d|--config)   CONFIG_DESC="$2"; shift 2 ;;
    --profile)     WORKLOAD_PROFILE="$2"; shift 2 ;;
    --materialization) MATERIALIZATION_PROFILE="$2"; shift 2 ;;
    --periodGranularity) PERIOD_GRANULARITY="$2"; shift 2 ;;
    --poPath)      PO_PATH="$2";      shift 2 ;;
    --paymentPath) PAYMENT_PATH="$2"; shift 2 ;;
    --seedFromCommitted) SEED_FROM_COMMITTED="Y"; shift ;;
    --viewInPaths) VIEW_IN_PATHS="$2"; shift 2 ;;
    -h|--help)     grep '^#' "$0" | sed 's/^# \{0,2\}//'; exit 0 ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

# ── derived values ────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
SBT_PROJECT="$REPO_ROOT/02-foundation/va_pipeline"

COMMITTED_OUT_PATH="$OUT_PATH"
RUN_ID="run_$(date '+%Y%m%d_%H%M%S')_$$"
ATTEMPT_ROOT="${COMMITTED_OUT_PATH%/}.generations"
ATTEMPT_OUT_PATH="${ATTEMPT_ROOT%/}/${RUN_ID}"
OUT_PATH="$ATTEMPT_OUT_PATH"

cleanup_failed_attempt() {
  if [[ -n "${ATTEMPT_OUT_PATH:-}" && -d "$ATTEMPT_OUT_PATH" ]]; then
    if [[ -f "$LOG_FILE" ]]; then
      cp "$LOG_FILE" "$ATTEMPT_ROOT/${RUN_ID}.failed.log"
    fi
    rm -rf "$ATTEMPT_OUT_PATH"
    echo "Failed attempt cleaned: $ATTEMPT_OUT_PATH" >&2
  fi
}

# Git Bash (MSYS) makes `ln -s` silently COPY the target unless native symlinks
# are requested. Native symlinks need Windows Developer Mode (or admin); if they
# are unavailable, fail rather than copy multi-GB run directories.
case "$(uname -s 2>/dev/null)" in
  MINGW*|MSYS*|CYGWIN*) export MSYS=winsymlinks:nativestrict ;;
esac

commit_attempt() {
  local final_dir="$1"
  local attempt_dir="$2"
  if [[ -d "$final_dir" && ! -L "$final_dir" ]]; then
    local legacy_dir
    legacy_dir="$(mktemp -d "$ATTEMPT_ROOT/legacy.XXXXXX")"
    rmdir "$legacy_dir"
    mv "$final_dir" "$legacy_dir"
    ln -s "$legacy_dir" "$final_dir" || { mv "$legacy_dir" "$final_dir"; return 1; }
  fi
  local next_link="${final_dir}.next.$$"
  ln -s "$attempt_dir" "$next_link"
  if [[ ! -L "$next_link" ]]; then
    echo "commit_attempt: ln -s did not create a symlink at $next_link (enable Windows Developer Mode for native symlinks)" >&2
    rm -rf "$next_link"
    return 1
  fi
  if [[ -L "$final_dir" ]]; then
    if [[ "$(uname -s)" == "Darwin" ]]; then
      mv -fh "$next_link" "$final_dir"
    else
      mv -fT "$next_link" "$final_dir"
    fi
  else
    mv "$next_link" "$final_dir"
  fi
  # Prune old generations after a successful commit (keep last 2).
  prune_old_generations "$ATTEMPT_ROOT" 2
}

# Prune old generations — keep the two most recent run_* directories plus the
# one currently symlinked from the committed path.  Legacy.* directories older
# than the retained run_* dirs are also removed.
# Called automatically at the end of commit_attempt.
prune_old_generations() {
  local attempt_root="$1"
  local keep="${2:-2}"        # number of run_* dirs to keep (default: 2)
  [[ -d "$attempt_root" ]] || return 0

  # Collect all run_* dirs sorted newest-first (by last-modification time).
  local dirs=()
  while IFS= read -r d; do
    dirs+=("$d")
  done < <(find "$attempt_root" -maxdepth 1 -type d -name 'run_*' \
             | xargs -d '\n' ls -dt 2>/dev/null)

  local n=${#dirs[@]}
  if [[ $n -le $keep ]]; then
    return 0
  fi

  # Remove all beyond the most-recent $keep entries.
  local removed=0
  for (( i=keep; i<n; i++ )); do
    local dir="${dirs[$i]}"
    # Safety check: never remove the directory currently pointed to by a symlink.
    local resolved
    resolved="$(readlink -f "${attempt_root%.generations}" 2>/dev/null || true)"
    [[ "$resolved" == "$dir" ]] && continue
    echo "[prune_generations] Removing old run dir: $dir"
    rm -rf "$dir"
    removed=$((removed + 1))
  done

  # Also prune legacy.* directories that are no longer referenced by any symlink.
  while IFS= read -r ldir; do
    # Check if any symlink in attempt_root points to this legacy dir.
    local referenced=0
    while IFS= read -r lnk; do
      if [[ "$(readlink -f "$lnk" 2>/dev/null)" == "$ldir" ]]; then
        referenced=1; break
      fi
    done < <(find "$attempt_root" -maxdepth 1 -type l 2>/dev/null)
    if [[ $referenced -eq 0 ]]; then
      echo "[prune_generations] Removing unreferenced legacy dir: $ldir"
      rm -rf "$ldir"
      removed=$((removed + 1))
    fi
  done < <(find "$attempt_root" -maxdepth 1 -type d -name 'legacy.*' 2>/dev/null)

  [[ $removed -gt 0 ]] && echo "[prune_generations] Pruned $removed old generation(s) from $attempt_root"
  return 0
}

# The engine now lives below the repository root. Normalize relative data paths
# before invoking sbt so callers can continue using --inPath data.
is_absolute_path() {
  [[ "$1" = /* || "$1" =~ ^[A-Za-z]:[\\/] || "$1" = //* ]]
}

is_absolute_path "$IN_PATH" || IN_PATH="$REPO_ROOT/$IN_PATH"
[[ -z "$PO_PATH" ]]      || is_absolute_path "$PO_PATH"      || PO_PATH="$REPO_ROOT/$PO_PATH"
[[ -z "$PAYMENT_PATH" ]] || is_absolute_path "$PAYMENT_PATH" || PAYMENT_PATH="$REPO_ROOT/$PAYMENT_PATH"
is_absolute_path "$COMMITTED_OUT_PATH" || COMMITTED_OUT_PATH="$REPO_ROOT/$COMMITTED_OUT_PATH"
is_absolute_path "$OUT_PATH" || OUT_PATH="$REPO_ROOT/$OUT_PATH"
ATTEMPT_ROOT="${COMMITTED_OUT_PATH%/}.generations"
ATTEMPT_OUT_PATH="${ATTEMPT_ROOT%/}/${RUN_ID}"
OUT_PATH="$ATTEMPT_OUT_PATH"
IN_PATH="${IN_PATH%/}/"
OUT_PATH="${OUT_PATH%/}/"
[[ -z "$LOG_FILE" ]] && LOG_FILE="${OUT_PATH%/}/pipeline_results.log"
COST_SURFACE="${OUT_PATH%/}/cost_surface.csv"
VIEWSPEC="${IN_PATH%/}/ViewSpec.csv"

# ── helpers ───────────────────────────────────────────────────────────────────

# Convert a POSIX path to a Windows drive-qualified path for the JVM when
# running under Git Bash / MINGW where cygpath is available.  No-op elsewhere.
to_jvm_path() {
  if command -v cygpath >/dev/null 2>&1; then
    cygpath -m "$1"
  else
    echo "$1"
  fi
}

log() {
  local msg="[$(date '+%Y-%m-%d %H:%M:%S')] $*"
  echo "$msg"
  echo "$msg" >> "$LOG_FILE"
}

# Row counts use wc -l (newline count). Engine outputs are println-terminated, so this equals the
# former awk NR count; it is several times faster on the 10M+ row merged files.
count_file_rows() {
  local file="$1" n
  if [[ -f "$file" ]]; then n=$(wc -l < "$file" | tr -d ' '); printf '%s\n' "${n:-0}"; else printf '0\n'; fi
}

count_data_rows() {
  local file="$1" n
  if [[ -f "$file" ]]; then
    n=$(wc -l < "$file" | tr -d ' '); n=${n:-0}
    printf '%s\n' "$(( n > 0 ? n - 1 : 0 ))"
  else
    printf '0\n'
  fi
}

count_file_bytes() {
  local file="$1"
  [[ -f "$file" ]] && wc -c < "$file" | tr -d ' ' || printf '0\n'
}

# One-read rule (mirrors SortedJEInputs.scala): when SortedJEFY<YY>merged.csv exists, steps 3/20/21
# read it plus the derived files (_FXR/_ALLOC/_CONTRA/_RECLASS) and skip the 2m inputs, which hold
# the same rows. Prints one file per line. Orchestration of file selection only; no financial logic.
oneread_sje_files() {
  local year="$1" f merged="$OUT_PATH/SortedJEFY${1}merged.csv"
  for f in "$OUT_PATH/SortedJEFY${year}"*.csv; do
    [[ -f "$f" ]] || continue
    if [[ -f "$merged" && "$f" != "$merged" && "$f" != *_FXR* && "$f" != *_ALLOC* \
          && "$f" != *_CONTRA* && "$f" != *_RECLASS* ]]; then
      continue
    fi
    printf '%s\n' "$f"
  done
}

# Directory that holds the step-2 payment file FY<YY>q<Q>exp.txt (explicit --paymentPath wins).
resolve_payment_dir() {
  local year="$1" quarter_number="$2" d
  if [[ -n "$PAYMENT_PATH" ]]; then printf '%s\n' "${PAYMENT_PATH%/}"; return; fi
  for d in "${IN_PATH%/}" "${IN_PATH%/}/payment"; do
    [[ -f "$d/FY${year}q${quarter_number}exp.txt" ]] && { printf '%s\n' "$d"; return; }
  done
  printf '%s\n' "${IN_PATH%/}"
}

# Directory that holds the step-2p PO file VA_opendata_FY20<YY>.txt (explicit --poPath wins).
resolve_po_dir() {
  local year="$1" d
  if [[ -n "$PO_PATH" ]]; then printf '%s\n' "${PO_PATH%/}"; return; fi
  for d in "${IN_PATH%/}/poData" "$(dirname "${IN_PATH%/}")/VAPOData" \
           "${IN_PATH%/}/VAPOData" "${IN_PATH%/}"; do
    [[ -f "$d/VA_opendata_FY20${year}.txt" ]] && { printf '%s\n' "$d"; return; }
  done
  # Nothing has the file: keep the historic behaviour (an existing sibling VAPOData dir, else
  # IN_PATH) so the engine reports the missing file itself.
  d="$(dirname "${IN_PATH%/}")/VAPOData"
  if [[ -d "$d" ]]; then printf '%s\n' "$d"; else printf '%s\n' "${IN_PATH%/}"; fi
}

directory_bytes() {
  local directory="$1"
  [[ -d "$directory" ]] || { printf '0\n'; return; }
  find "$directory" -type f -exec wc -c {} \; | awk '{ total += $1 } END { printf "%.0f\n", total }'
}

run_step() {
  local step="$1" year="$2" quarter="${3:-01}"
  local quarter_number=$((10#$quarter))
  log "START  step=$step  year=$year  quarter=$quarter"

  local vm_arg=""
  if [[ "$step" == "2" || "$step" == "ttl" ]]; then
    local vm_candidate
    for vm_candidate in \
      "$IN_PATH/VendorMaster_full.csv" \
      "$IN_PATH/VendorMaster_enriched.csv" \
      "$IN_PATH/VendorMaster.csv" \
      "$REPO_ROOT/data/VendorMaster_full.csv" \
      "$REPO_ROOT/data/VendorMaster.csv"; do
      if [[ -f "$vm_candidate" ]]; then
        vm_arg=" --vendormaster $vm_candidate"
        break
      fi
    done
  fi
  local ref_arg=""
  if [[ "$step" == "ttl" ]]; then
    # refPath: directory containing aRef*.txt — look beside IN_PATH first, then REPO_ROOT/data
    local ref_candidate
    for ref_candidate in \
      "$(dirname "$IN_PATH")" \
      "$REPO_ROOT/data"; do
      if [[ -f "$ref_candidate/aRefAgencyCSV.txt" ]]; then
        ref_arg=" --refPath $ref_candidate"
        break
      fi
    done
  fi
  local allocation_rules_arg=""
  if [[ "$step" == "6" && -f "$IN_PATH/AllocationRules.csv" ]]; then
    allocation_rules_arg=" --allocationRules $IN_PATH"
  fi
  local fx_args=""
  if [[ "$step" == "12" ]]; then
    fx_args=" --fxRates $IN_PATH/booked_fx_rates.csv --fxRules $IN_PATH/booked_fx_rules.csv"
  fi

  local input_file="" output_file="" input_bytes="0" output_file_bytes="0"
  local records_read="?" records_written="?"
  case "$step" in
    ttl)
      # TTL step: reads the FY subdirectory tree.
      # Output file reference: first SortedJEFY file found (monthly or quarterly naming).
      input_file="$IN_PATH"
      output_file=""
      for _ttl_f in "$OUT_PATH/SortedJEFY${year}_"*"_exp.csv" \
                    "$OUT_PATH/SortedJEFY${year}q1exp.csv"; do
        [[ -f "$_ttl_f" ]] && { output_file="$_ttl_f"; break; }
      done
      [[ -n "$output_file" ]] || output_file="$OUT_PATH/SortedJEFY${year}q1exp.csv"
      # Accumulate input bytes across all 12 monthly source files under the FY directory
      input_bytes=0
      for apa_file in "$IN_PATH/FY ${year:0:2}2${year:1:1}? Expenditure/"*.csv \
                       "$IN_PATH/All Expenditures for Fiscal Year 20${year}"*/*.csv; do
        [[ -f "$apa_file" ]] || continue
        input_bytes=$((input_bytes + $(count_file_bytes "$apa_file")))
      done
      ;;
    2)
      input_file="$(resolve_payment_dir "$year" "$quarter_number")/FY${year}q${quarter_number}exp.txt"
      output_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}exp.csv"
      records_read=$(count_data_rows "$input_file")
      ;;
    2p)
      input_file="$(resolve_po_dir "$year")/VA_opendata_FY20${year}.txt"
      output_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}PO.csv"
      records_read=$(count_data_rows "$input_file")
      ;;
    2m)
      # Input: all payment SortedJE files (monthly or quarterly) + PO file
      output_file="$OUT_PATH/SortedJEFY${year}merged.csv"
      # Use first available payment file as representative input_file for logging
      input_file=""
      for _2m_f in "$OUT_PATH/SortedJEFY${year}_"*"_exp.csv" \
                   "$OUT_PATH/SortedJEFY${year}q"*"exp.csv"; do
        [[ -f "$_2m_f" ]] && { input_file="$_2m_f"; break; }
      done
      [[ -n "$input_file" ]] || input_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}exp.csv"
      records_read=0
      input_bytes=0
      for sje_file in "$OUT_PATH/SortedJEFY${year}_"*"_exp.csv" \
                      "$OUT_PATH/SortedJEFY${year}q"*"exp.csv" \
                      "$OUT_PATH/SortedJEFY${year}q"*"PO.csv" \
                      "$OUT_PATH/SortedJEFY${year}_"*"PO.csv"; do
        [[ -f "$sje_file" ]] || continue
        input_bytes=$((input_bytes + $(count_file_bytes "$sje_file")))
      done
      ;;
    3)
      input_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}exp.csv"
      output_file="$OUT_PATH/LDGR20${year}.csv"
      records_read=0
      input_bytes=0
      while IFS= read -r sje_file; do
        records_read=$((records_read + $(count_file_rows "$sje_file")))
        input_bytes=$((input_bytes + $(count_file_bytes "$sje_file")))
      done < <(oneread_sje_files "$year")
      ;;
    5v)
      # Step 5v produces four view files; use the merged SJE as reference input
      input_file="$OUT_PATH/SortedJEFY${year}merged.csv"
      [[ -f "$input_file" ]] || input_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}exp.csv"
      output_file="$OUT_PATH/view_monthly_order_volume.csv"
      records_read=$(count_file_rows "$input_file")
      ;;
    20)
      # GLPost: accumulates account-keyed balances from all SortedJEFY*.csv files
      input_file="$OUT_PATH/SortedJEFY${year}merged.csv"
      [[ -f "$input_file" ]] || input_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}exp.csv"
      output_file="$OUT_PATH/GL20${year}.csv"
      input_bytes=0
      records_read=0
      while IFS= read -r sje_file; do
        records_read=$((records_read + $(count_file_rows "$sje_file")))
        input_bytes=$((input_bytes + $(count_file_bytes "$sje_file")))
      done < <(oneread_sje_files "$year")
      ;;
    21)
      # PermutationGenerator: same input glob as step 20
      input_file="$OUT_PATH/SortedJEFY${year}merged.csv"
      [[ -f "$input_file" ]] || input_file="$OUT_PATH/SortedJEFY${year}q${quarter_number}exp.csv"
      output_file="$OUT_PATH/PERM_TBL20${year}.csv"
      input_bytes=0
      records_read=0
      while IFS= read -r sje_file; do
        records_read=$((records_read + $(count_file_rows "$sje_file")))
        input_bytes=$((input_bytes + $(count_file_bytes "$sje_file")))
      done < <(oneread_sje_files "$year")
      ;;
    6)
      input_file="$OUT_PATH/LDGR20${year}.csv"
      output_file="$OUT_PATH/ALLOC_SJE20${year}.csv"
      records_read=$(count_data_rows "$input_file")
      ;;
    12)
      input_file="$OUT_PATH/LDGR20${year}_OPENING.csv"
      output_file="$OUT_PATH/SortedJEFY${year}_FXR.csv"
      records_read=$(count_data_rows "$input_file")
      ;;
  esac
  if [[ "$step" != "3" && "$step" != "20" && "$step" != "21" && "$step" != "ttl" ]]; then
    input_bytes=$(count_file_bytes "$input_file")
  fi

  local start_epoch
  start_epoch=$(date +%s)

  # Capture output while still streaming it to terminal.
  # On Windows Git Bash (uname -s = MINGW*), /dev/stderr does not exist —
  # use a temp file for tee so the output is still captured in $out.
  local out
  local time_mode=""
  local _tee_target
  local _uname
  _uname=$(uname -s 2>/dev/null)
  if [[ "$_uname" == MINGW* || "$_uname" == MSYS* || "$_uname" == CYGWIN* ]]; then
    _tee_target=$(mktemp)
  else
    _tee_target=/dev/stderr
  fi
  if [[ "$(uname -s 2>/dev/null)" == "Darwin" && -x /usr/bin/time ]]; then
    time_mode="macos"
  elif /usr/bin/time --version >/dev/null 2>&1; then
    time_mode="gnu"
  fi
  # Convert POSIX paths to Windows drive-qualified paths for the JVM (Git Bash on Windows)
  local jvm_in_path jvm_out_path
  jvm_in_path="$(to_jvm_path "$IN_PATH")"
  jvm_out_path="$(to_jvm_path "$OUT_PATH")"

  # Step 2 reads the payment file and step 2p the PO file from their own directories
  # (--paymentPath / --poPath, else resolved from the layout under IN_PATH).
  if [[ "$step" == "2" ]]; then
    jvm_in_path="$(to_jvm_path "$(resolve_payment_dir "$year" "$quarter_number")")"
  elif [[ "$step" == "2p" ]]; then
    jvm_in_path="$(to_jvm_path "$(resolve_po_dir "$year")")"
  fi

  # Steps 20 and 21 (GLPost / PermutationGenerator) read SortedJE files from outPath,
  # not from the raw inPath — override jvm_in_path to point at the output directory.
  if [[ "$step" == "20" || "$step" == "21" ]]; then
    jvm_in_path="$jvm_out_path"
  fi

  # Also convert any path arguments embedded in vm_arg and ref_arg
  local jvm_vm_arg="$vm_arg" jvm_ref_arg="$ref_arg"
  if command -v cygpath >/dev/null 2>&1; then
    [[ -n "$vm_arg" ]] && jvm_vm_arg=" --vendormaster $(cygpath -m "${vm_arg#* --vendormaster }")"
    [[ -n "$ref_arg" ]] && jvm_ref_arg=" --refPath $(cygpath -m "${ref_arg#* --refPath }")"
  fi

  local period_arg=" --periodGranularity $PERIOD_GRANULARITY"
  # Step 5v can merge several years' output directories in one ordered pass (CKBViews inPaths).
  if [[ "$step" == "5v" && -n "$VIEW_IN_PATHS" ]]; then
    period_arg+=" --inPaths $VIEW_IN_PATHS"
  fi

  # Launch retry (job control only): several parallel lanes share one sbt project directory, and
  # sbt's boot-server socket can be taken by another lane at the same instant. That failure happens
  # before any engine code runs, so relaunching is safe. Only that exact signature is retried; any
  # other non-zero exit (including engine rc 2) stops immediately, as before.
  local rc=0 launch_attempt=0 launch_max=6
  while true; do
    launch_attempt=$((launch_attempt + 1))
    rc=0
    if [[ "$time_mode" == "macos" ]]; then
      out=$(cd "$SBT_PROJECT" && /usr/bin/time -l sbt -batch \
        "run --step $step --inPath $jvm_in_path --outPath $jvm_out_path --year $year --quarter $quarter$jvm_vm_arg$jvm_ref_arg$allocation_rules_arg$fx_args$period_arg" \
        2>&1 | tee "$_tee_target") || rc=$?
    elif [[ "$time_mode" == "gnu" ]]; then
      out=$(cd "$SBT_PROJECT" && /usr/bin/time -v sbt -batch \
        "run --step $step --inPath $jvm_in_path --outPath $jvm_out_path --year $year --quarter $quarter$jvm_ref_arg$allocation_rules_arg$fx_args$period_arg" \
        2>&1 | tee "$_tee_target") || rc=$?
    else
      out=$(cd "$SBT_PROJECT" && sbt -batch \
        "run --step $step --inPath $jvm_in_path --outPath $jvm_out_path --year $year --quarter $quarter$jvm_vm_arg$jvm_ref_arg$allocation_rules_arg$fx_args$period_arg" \
        2>&1 | tee "$_tee_target") || rc=$?
    fi
    if [[ $rc -ne 0 && $launch_attempt -lt $launch_max && "$out" == *"ServerAlreadyBootingException"* ]]; then
      log "RETRY  step=$step year=$year sbt boot-socket collision (rc=$rc); launch attempt $launch_attempt of $launch_max, retrying"
      sleep $(( 5 + RANDOM % 11 ))
      continue
    fi
    break
  done
  [[ $rc -eq 0 ]] || exit $rc
  # Clean up temp tee file if we used one (non-stderr path)
  [[ "$_tee_target" != /dev/stderr ]] && rm -f "$_tee_target"
  local end_epoch
  end_epoch=$(date +%s)
  local elapsed=$(( end_epoch - start_epoch ))
  LAST_ELAPSED=$elapsed  # exported for TOTAL_COMPUTE_S accumulation in main loop
  # Three different measures, kept apart (see docs/MEASUREMENT_AND_LOGGING.md):
  #   LAST_PEAK_RSS_BYTES        OS maximum resident set size of the `sbt` process tree under
  #                              /usr/bin/time (includes sbt start-up). Empty when `time` gives none.
  #   LAST_PEAK_FOOTPRINT_BYTES  macOS "peak memory footprint" from `time -l`. Empty elsewhere.
  #   LAST_JVM_HEAP_BYTES        JVM committed heap at the end of the step, as printed by the Scala
  #                              RESOURCE line (Runtime.totalMemory). Not a peak and not RSS.
  # No value is ever substituted for another: an empty field means "not measured".
  #   LAST_CPU_USER_S / LAST_CPU_SYSTEM_S / LAST_CPU_TOTAL_S
  #                              user and system CPU seconds of the `sbt` process tree, from the same
  #                              `time` output. Unlike wall seconds they do not grow when lanes run in
  #                              parallel, so they are the comparable compute measure. Includes sbt/JVM
  #                              start-up. Empty when `time` gives none.
  LAST_PEAK_RSS_BYTES=""
  LAST_PEAK_FOOTPRINT_BYTES=""
  LAST_JVM_HEAP_BYTES=""
  LAST_CPU_USER_S=""
  LAST_CPU_SYSTEM_S=""
  LAST_CPU_TOTAL_S=""
  if [[ "$time_mode" == "macos" ]]; then
    LAST_PEAK_RSS_BYTES=$(printf '%s\n' "$out" | awk '/maximum resident set size/ { print $1; exit }')
    LAST_PEAK_FOOTPRINT_BYTES=$(printf '%s\n' "$out" | awk '/peak memory footprint/ { print $1; exit }')
    # macOS: "   12.34 real        10.00 user         2.00 sys"
    LAST_CPU_USER_S=$(printf '%s\n' "$out" | awk '/ real .* user .* sys/ { for (i = 1; i < NF; i++) if ($(i+1) == "user") { print $i; exit } }')
    LAST_CPU_SYSTEM_S=$(printf '%s\n' "$out" | awk '/ real .* user .* sys/ { for (i = 1; i < NF; i++) if ($(i+1) == "sys") { print $i; exit } }')
  elif [[ "$time_mode" == "gnu" ]]; then
    local rss_kb
    rss_kb=$(printf '%s\n' "$out" | awk -F': ' '/Maximum resident set size/ { print $2; exit }' | awk '{print $1}')
    [[ "$rss_kb" =~ ^[0-9]+$ ]] && LAST_PEAK_RSS_BYTES=$((rss_kb * 1024))
    LAST_CPU_USER_S=$(printf '%s\n' "$out" | awk -F': ' '/User time \(seconds\)/ { print $2; exit }')
    LAST_CPU_SYSTEM_S=$(printf '%s\n' "$out" | awk -F': ' '/System time \(seconds\)/ { print $2; exit }')
  fi
  [[ "$LAST_PEAK_RSS_BYTES" =~ ^[0-9]+$ ]] || LAST_PEAK_RSS_BYTES=""
  [[ "$LAST_PEAK_FOOTPRINT_BYTES" =~ ^[0-9]+$ ]] || LAST_PEAK_FOOTPRINT_BYTES=""
  [[ "$LAST_CPU_USER_S"   =~ ^[0-9]+(\.[0-9]+)?$ ]] || LAST_CPU_USER_S=""
  [[ "$LAST_CPU_SYSTEM_S" =~ ^[0-9]+(\.[0-9]+)?$ ]] || LAST_CPU_SYSTEM_S=""
  if [[ -n "$LAST_CPU_USER_S" && -n "$LAST_CPU_SYSTEM_S" ]]; then
    LAST_CPU_TOTAL_S=$(awk -v u="$LAST_CPU_USER_S" -v s="$LAST_CPU_SYSTEM_S" 'BEGIN { printf "%.2f", u + s }')
  fi
  # JVM heap: parse the Scala-emitted line (available on all platforms).
  LAST_JVM_HEAP_BYTES=$(printf '%s\n' "$out" | grep -E "RESOURCE step=${step} " | \
    sed -nE 's/.*jvm_heap_bytes=([0-9]+).*/\1/p' | tail -1)
  [[ "$LAST_JVM_HEAP_BYTES" =~ ^[0-9]+$ ]] || LAST_JVM_HEAP_BYTES=""

  local records_written
  case "$step" in
    ttl)
      # TTL: extract read/written from RESULT step=ttl structured log line; sum Q1-Q4 output files
      result_line_ttl=$(echo "$out" | grep -E "RESULT step=ttl " | tail -1 || true)
      records_read=$(printf '%s\n' "$result_line_ttl" | sed -nE 's/.*records_read=([0-9]+).*/\1/p' | tail -1)
      [[ -n "$records_read" ]] || records_read="?"
      records_written=0
      # Count rows across both monthly (SortedJEFY{yr}_{YYYY}_{MM}_exp.csv) and
      # quarterly (SortedJEFY{yr}q{n}exp.csv) output files produced by this run.
      for sje_f in "$OUT_PATH/SortedJEFY${year}_"*"_exp.csv" \
                   "$OUT_PATH/SortedJEFY${year}q"*"exp.csv"; do
        [[ -f "$sje_f" ]] && records_written=$((records_written + $(count_file_rows "$sje_f")))
      done
      [[ "$records_written" == "0" ]] && records_written=$(printf '%s\n' "$result_line_ttl" | sed -nE 's/.*records_written=([0-9]+).*/\1/p' | tail -1)
      [[ -n "$records_written" ]] || records_written="?"
      ;;
    2)
      records_written=$(count_file_rows "$output_file")
      if [[ "$records_read" == "0" || "$records_read" == "?" ]]; then
        records_read=$(echo "$out" | grep -i "Records Read:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      fi
      if [[ "$records_written" == "0" || "$records_written" == "?" ]]; then
        records_written=$(echo "$out" | grep -i "Journal lines produced:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      fi
      ;;
    2p)
      records_written=$(count_file_rows "$output_file")
      result_line_2p=$(echo "$out" | grep -E "RESULT step=2p " | tail -1 || true)
      # records_read is the engine's logical PO record count (physical lines can span several lines).
      engine_2p_read=$(printf '%s\n' "$result_line_2p" | sed -nE 's/.*records_read=([0-9]+).*/\1/p' | tail -1)
      [[ -z "$engine_2p_read" ]] || records_read="$engine_2p_read"
      [[ -n "$records_read" ]] || records_read="?"
      if [[ -z "$records_written" || "$records_written" == "?" || "$records_written" == "0" ]]; then
        records_written=$(printf '%s\n' "$result_line_2p" | sed -nE 's/.*records_written=([0-9]+).*/\1/p' | tail -1)
        [[ -n "$records_written" ]] || records_written="?"
      fi
      ;;
    2m)
      records_written=$(count_file_rows "$output_file")
      result_line_2m=$(echo "$out" | grep -E "RESULT step=2m " | tail -1 || true)
      if [[ -z "$records_written" || "$records_written" == "?" || "$records_written" == "0" ]]; then
        records_written=$(printf '%s\n' "$result_line_2m" | sed -nE 's/.*records_written=([0-9]+).*/\1/p' | tail -1)
        [[ -n "$records_written" ]] || records_written="?"
      fi
      ;;
    3)
      records_written=$(count_data_rows "$output_file")
      if [[ "$records_read" == "0" || "$records_read" == "?" ]]; then
        records_read=$(echo "$out" | grep -i "Journal Records Read:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      fi
      if [[ "$records_written" == "0" || "$records_written" == "?" ]]; then
        records_written=$(echo "$out" | grep -i "Ledger Records Written:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      fi
      ;;
    5v)
      # Step 5v writes four view files; capture per-view row counts from the RESULT line
      result_line_5v=$(echo "$out" | grep -E "RESULT step=5v " | tail -1 || true)
      records_read=$(printf '%s\n' "$result_line_5v" | sed -nE 's/.*records_read=([0-9]+).*/\1/p' | tail -1)
      [[ -n "$records_read" ]] || records_read="?"
      # Represent total view rows written as sum of all four view file counts
      view_a=$(count_data_rows "$OUT_PATH/view_monthly_order_volume.csv")
      view_b=$(count_data_rows "$OUT_PATH/view_vendor_year_survival.csv")
      view_c=$(count_data_rows "$OUT_PATH/view_agency_year_commodity_mix.csv")
      view_d=$(count_data_rows "$OUT_PATH/view_agency_year_payment_totals.csv")
      records_written=$(( view_a + view_b + view_c + view_d ))
      ;;
    20)
      # GLPost: extract records_read and records_written from RESULT line
      result_line_20=$(echo "$out" | grep -E "RESULT step=20 " | tail -1 || true)
      records_written=$(count_data_rows "$output_file")
      if [[ -z "$records_written" || "$records_written" == "0" ]]; then
        records_written=$(printf '%s\n' "$result_line_20" | sed -nE 's/.*records_written=([0-9]+).*/\1/p' | tail -1)
        [[ -n "$records_written" ]] || records_written="?"
      fi
      ;;
    21)
      # PermutationGenerator: extract records_read and records_written from RESULT line
      result_line_21=$(echo "$out" | grep -E "RESULT step=21 " | tail -1 || true)
      records_written=$(count_data_rows "$output_file")
      if [[ -z "$records_written" || "$records_written" == "0" ]]; then
        records_written=$(printf '%s\n' "$result_line_21" | sed -nE 's/.*records_written=([0-9]+).*/\1/p' | tail -1)
        [[ -n "$records_written" ]] || records_written="?"
      fi
      ;;
    6)
      records_written=$(count_data_rows "$output_file")
      if [[ "$records_written" == "0" || "$records_written" == "?" ]]; then
        records_written=$(echo "$out" | grep -i "Allocation SJEs written:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      fi
      ;;
    12)
      records_written=$(count_file_rows "$output_file")
      ;;
    *)
      records_read=$(echo "$out" | grep -i "Records Read:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      records_written=$(echo "$out" | grep -i "Records Written:" | tail -1 | grep -oE '[0-9]+' | tail -1 || echo "?")
      ;;
  esac
  output_file_bytes=$(count_file_bytes "$output_file")

  local out_bytes
  out_bytes=$(directory_bytes "$OUT_PATH")

  # Structured result line — readable by agents and humans
  local result_line="RESULT step=$step year=$year quarter=$quarter elapsed_s=$elapsed rc=$rc records_read=$records_read records_written=$records_written input_bytes=$input_bytes output_bytes=$output_file_bytes outPath_bytes=$out_bytes"
  if [[ "$step" == "2p" ]]; then
    for field in records_accepted skipped_quarantine reader_rejects physical_lines; do
      local value
      value=$(printf '%s\n' "$result_line_2p" | sed -nE "s/.*(^| )${field}=([^ ]+).*/\\2/p" | tail -1)
      [[ -z "$value" ]] || result_line+=" $field=$value"
    done
  fi
  if [[ "$step" == "12" ]]; then
    local engine_result
    engine_result=$(printf '%s\n' "$out" | grep -E 'RESULT step=12 ' | tail -1 || true)
    for field in generated_groups balanced_groups unbalanced_groups amount_delta adjustment_total spills rule_set_id rule_version; do
      local value
      value=$(printf '%s\n' "$engine_result" | sed -nE "s/.*(^| )${field}=([^ ]+).*/\\2/p" | tail -1)
      [[ -z "$value" ]] || result_line+=" $field=$value"
    done
  fi
  log "$result_line"
  log "RESOURCE step=$step year=$year peak_rss_bytes=${LAST_PEAK_RSS_BYTES:-} peak_memory_footprint_bytes=${LAST_PEAK_FOOTPRINT_BYTES:-} jvm_heap_bytes=${LAST_JVM_HEAP_BYTES:-} cpu_user_seconds=${LAST_CPU_USER_S:-} cpu_system_seconds=${LAST_CPU_SYSTEM_S:-} cpu_total_seconds=${LAST_CPU_TOTAL_S:-}"

  if [[ $rc -ne 0 ]]; then
    log "ERROR  step=$step year=$year exited with code $rc — pipeline halted"
    exit $rc
  fi
}

# ── cost surface helpers ──────────────────────────────────────────────────────

# Count enabled=Y rows in ViewSpec.csv (AHI metric — reconciliation obligation count).
# Skips comment lines and the header; counts only rows where the 'enabled' column = Y.
count_enabled_views() {
  local viewspec="$1"
  if [[ ! -f "$viewspec" ]]; then
    echo "0"
    return
  fi
  # Column index of 'enabled' (1-based) from the header row
  local col_idx
  col_idx=$(grep -v '^#' "$viewspec" | head -1 | tr ',' '\n' | grep -n '^enabled$' | cut -d: -f1)
  if [[ -z "$col_idx" ]]; then
    echo "0"
    return
  fi
  grep -v '^#' "$viewspec" | tail -n +2 | awk -F',' -v col="$col_idx" '$col == "Y"' | wc -l | tr -d ' '
}

# Initialise cost_surface.csv header if file does not exist.
init_cost_surface() {
  local f="$1"
  if [[ ! -f "$f" ]]; then
    echo "\"run_id\",\"run_timestamp\",\"curve\",\"config_description\",\"years\",\"steps\",total_compute_s,total_storage_bytes,master_file_count,total_cost_proxy,\"capability_layer\",\"period_granularity\",\"materialization_scope\",ledger_row_count,ldgr_file_bytes,view_file_bytes,permutation_rows,permutation_capacity" > "$f"
  fi
}

# Append one row to cost_surface.csv.
# Args: f run_id ts curve cfg years steps compute_s storage_bytes master_count
#       capability_layer period_granularity materialization_scope
#       ledger_row_count ldgr_file_bytes view_file_bytes permutation_rows permutation_capacity
record_cost_point() {
  local f="$1" run_id="$2" ts="$3" curve="$4" cfg="$5" years="$6" steps="$7"
  local compute_s="$8" storage_bytes="$9" master_count="${10}"
  local capability_layer="${11:-}" period_granularity="${12:-}" materialization_scope="${13:-}"
  local ledger_row_count="${14:-}" ldgr_file_bytes="${15:-}" view_file_bytes="${16:-}" permutation_rows="${17:-}"
  local permutation_capacity="${18:-}"
  # total_cost_proxy: compute(s) + storage(MB) + master_files × 10
  local proxy
  if [[ "$storage_bytes" =~ ^[0-9]+$ ]]; then
    proxy=$(awk -v c="$compute_s" -v s="$storage_bytes" -v m="$master_count" \
      'BEGIN { printf "%.2f", c + (s/1000000) + (m*10) }')
  else
    proxy="?"
  fi
  # Quote fields that may contain spaces or commas
  echo "\"${run_id}\",\"${ts}\",\"${curve}\",\"${cfg}\",\"${years}\",\"${steps}\",${compute_s},${storage_bytes},${master_count},${proxy},\"${capability_layer}\",\"${period_granularity}\",\"${materialization_scope}\",${ledger_row_count:-0},${ldgr_file_bytes:-0},${view_file_bytes:-0},${permutation_rows:-0},${permutation_capacity:-0}" >> "$f"
}

# ── main loop ─────────────────────────────────────────────────────────────────
trap 'rc=$?; if [[ $rc -eq 0 ]]; then commit_attempt "$COMMITTED_OUT_PATH" "$ATTEMPT_OUT_PATH" || { cleanup_failed_attempt; exit 1; }; else cleanup_failed_attempt; fi' EXIT
mkdir -p "$ATTEMPT_ROOT" "$OUT_PATH"
# --seedFromCommitted: continue a year whose earlier steps are already committed (e.g. 3,20,21 after
# ttl,2p,2m). Large data files are hard-linked (same filesystem, no copy, never modified in place);
# log, cost surface and evidence directories are copied so this run appends to them.
if [[ "$SEED_FROM_COMMITTED" == "Y" ]]; then
  if [[ ! -d "$COMMITTED_OUT_PATH" ]]; then
    echo "ERROR: --seedFromCommitted but nothing committed at $COMMITTED_OUT_PATH" >&2
    exit 1
  fi
  for seed_item in "${COMMITTED_OUT_PATH%/}"/*; do
    seed_name="$(basename "$seed_item")"
    if [[ -d "$seed_item" ]]; then
      cp -R "$seed_item" "$OUT_PATH/$seed_name"
    elif [[ "$seed_name" == "cost_surface.csv" || "$seed_name" == "pipeline_results.log" ]]; then
      cp "$seed_item" "$OUT_PATH/$seed_name"
    else
      ln "$seed_item" "$OUT_PATH/$seed_name" 2>/dev/null || cp "$seed_item" "$OUT_PATH/$seed_name"
    fi
  done
fi
for year in ${YEARS//,/ }; do
  if [[ -f "$COMMITTED_OUT_PATH/LDGR20${year}.csv" ]]; then
    cp "$COMMITTED_OUT_PATH/LDGR20${year}.csv" "$OUT_PATH/LDGR20${year}.csv"
  fi
  if [[ -f "$IN_PATH/LDGR20${year}_OPENING.csv" ]]; then
    cp "$IN_PATH/LDGR20${year}_OPENING.csv" "$OUT_PATH/LDGR20${year}_OPENING.csv"
    if [[ ! -f "$OUT_PATH/LDGR20${year}.csv" ]]; then
      cp "$OUT_PATH/LDGR20${year}_OPENING.csv" "$OUT_PATH/LDGR20${year}.csv"
    fi
  elif [[ ",$STEPS," == *,12,* && -f "$OUT_PATH/LDGR20${year}.csv" ]]; then
    cp "$OUT_PATH/LDGR20${year}.csv" "$OUT_PATH/LDGR20${year}_OPENING.csv"
  fi
done
init_cost_surface "$COST_SURFACE"
RUN_TS=$(date '+%Y-%m-%d %H:%M:%S')
RUN_START_EPOCH=$(date +%s)
TOTAL_COMPUTE_S=0

log "======================================================================"
log "Universal Ledger Pipeline Run  [$RUN_ID]"
log "  curve              = $CURVE"
log "  config             = $CONFIG_DESC"
log "  profile            = $WORKLOAD_PROFILE"
log "  materialization    = $MATERIALIZATION_PROFILE"
log "  period_granularity = $PERIOD_GRANULARITY"
log "  inPath  = $IN_PATH"
log "  outPath = $OUT_PATH"
log "  years   = $YEARS"
log "  steps   = $STEPS"
log "  logFile = $LOG_FILE"
log "======================================================================"

IFS=',' read -ra YEAR_LIST <<< "$YEARS"
IFS=',' read -ra STEP_LIST <<< "$STEPS"

for year in "${YEAR_LIST[@]}"; do
  log "------  Year $year  -------------------------------------------------------"
  for step in "${STEP_LIST[@]}"; do
    run_step "$step" "$year"
    # Accumulate compute time — run_step sets last elapsed in LAST_ELAPSED
    TOTAL_COMPUTE_S=$(( TOTAL_COMPUTE_S + ${LAST_ELAPSED:-0} ))
  done
done

# Allocation output becomes the next period's divisor side input before evidence is flushed.
if [[ ",$STEPS," == *,6,* ]]; then
  for year in "${YEAR_LIST[@]}"; do
    short_year="${year: -2}"
    next_year=$(printf "%02d" $((10#$short_year + 1)))
    ledger_after_allocation="$OUT_PATH/LDGR20${short_year}.csv"
    if [[ -f "$ledger_after_allocation" ]]; then
      bash "$SCRIPT_DIR/generate_allocation_divisors.sh" \
        --source-kind ledger \
        --input "$ledger_after_allocation" \
        --output "$OUT_PATH/AllocationDivisors_FY${short_year}_after_allocation.csv" \
        --divisor-period "FY${short_year}Q1" \
        --source-period "FY${next_year}Q1" \
        --source-partition-id "LDGR20${short_year}_after_allocation"
    fi
  done
fi

# ── post-run metrics ──────────────────────────────────────────────────────────
FINAL_STORAGE_BYTES="?"
if [[ -d "$OUT_PATH" ]]; then
  FINAL_STORAGE_BYTES=$(directory_bytes "$OUT_PATH")
fi

MASTER_FILE_COUNT=$(count_enabled_views "$VIEWSPEC")

# ── three-axis cost surface columns ──────────────────────────────────────────
# Axis 1 — capability_layer: map CURVE to CCP label; fall back to CURVE itself.
case "$CURVE" in
  CCP1) CAPABILITY_LAYER="CCP1" ;;
  CCP2) CAPABILITY_LAYER="CCP2" ;;
  CCP3) CAPABILITY_LAYER="CCP3" ;;
  CCP4) CAPABILITY_LAYER="CCP4" ;;
  CCP5) CAPABILITY_LAYER="CCP5" ;;
  *)    CAPABILITY_LAYER="$CURVE" ;;
esac

# Axis 2 — period_granularity: use the CLI value (default: annual).
COST_PERIOD_GRANULARITY="$PERIOD_GRANULARITY"

# Axis 3 — materialization_scope: derive from MATERIALIZATION_PROFILE or CONFIG_DESC.
COST_MATERIALIZATION="${MATERIALIZATION_PROFILE:-movement-only}"

# Per-output file metrics (0 if the step did not run or the file is absent).
LDGR_ROW_COUNT=0
LDGR_FILE_BYTES=0
VIEW_FILE_BYTES=0
PERMUTATION_ROWS=0       # sum over GL keys of distinct vendors (continuity with the pre-2026-11-11 row count)
PERMUTATION_CAPACITY=0   # sum over GL keys of the product of per-dimension distinct counts
for year in ${YEARS//,/ }; do
  ldgr_file="$OUT_PATH/LDGR20${year}.csv"
  if [[ -f "$ldgr_file" ]]; then
    LDGR_ROW_COUNT=$(( LDGR_ROW_COUNT + $(count_data_rows "$ldgr_file") ))
    LDGR_FILE_BYTES=$(( LDGR_FILE_BYTES + $(count_file_bytes "$ldgr_file") ))
  fi
done
# Permutation totals exceed 64-bit integers.  Step 21 writes one PERM_SUMMARY20YY.csv per year and
# Scala step 21s sums them (BigInt); this shell only forwards the RESULT line it prints.
if [[ ",$STEPS," == *",21,"* ]]; then
  # Same boot-socket retry as the engine launches (sbt start-up only, before any engine code runs).
  # Before 2026-11-11 this call had no retry and discarded its output on failure, which left FY03's
  # permutation_rows/capacity at 0 in the 2026-11-10 baseline cost surface. A failure is now logged
  # with its reason; it stays non-fatal, as before.
  perm_summary_attempt=0; perm_summary_max=6
  while true; do
    perm_summary_attempt=$((perm_summary_attempt + 1)); perm_summary_rc=0
    perm_summary_out=$(cd "$SBT_PROJECT" && sbt -batch \
      "run --step 21s --outPath $(to_jvm_path "$OUT_PATH") --years $YEARS" 2>&1) || perm_summary_rc=$?
    if [[ $perm_summary_rc -ne 0 && $perm_summary_attempt -lt $perm_summary_max && "$perm_summary_out" == *"ServerAlreadyBootingException"* ]]; then
      log "RETRY  step=21s years=$YEARS sbt boot-socket collision (rc=$perm_summary_rc); launch attempt $perm_summary_attempt of $perm_summary_max, retrying"
      sleep $(( 5 + RANDOM % 11 ))
      continue
    fi
    break
  done
  perm_summary_line=$(printf '%s\n' "$perm_summary_out" | grep -E "RESULT step=21s " | tail -1 || true)
  if [[ -z "$perm_summary_line" ]]; then
    log "WARNING step=21s years=$YEARS produced no RESULT line (rc=$perm_summary_rc, attempts=$perm_summary_attempt); permutation_rows and permutation_capacity stay 0 in cost_surface. Last output:"
    printf '%s\n' "$perm_summary_out" | tail -8 | while IFS= read -r l; do log "  21s> $l"; done
  fi
  perm_rows=$(printf '%s\n' "$perm_summary_line" | sed -nE 's/.*vendor_rows=([0-9]+).*/\1/p')
  perm_cap=$(printf '%s\n' "$perm_summary_line" | sed -nE 's/.* capacity=([0-9]+).*/\1/p')
  [[ -n "$perm_rows" ]] && PERMUTATION_ROWS="$perm_rows"
  [[ -n "$perm_cap"  ]] && PERMUTATION_CAPACITY="$perm_cap"
  log "$perm_summary_line"
fi
for vf in "$OUT_PATH"/view_*.csv; do
  [[ -f "$vf" ]] && VIEW_FILE_BYTES=$(( VIEW_FILE_BYTES + $(count_file_bytes "$vf") ))
done

log "======================================================================"
log "Pipeline complete.  [$RUN_ID]"
log "  total_compute_s     = $TOTAL_COMPUTE_S"
log "  total_storage_bytes = $FINAL_STORAGE_BYTES"
log "  master_file_count   = $MASTER_FILE_COUNT  (enabled=Y rows in ViewSpec.csv)"
log "  capability_layer    = $CAPABILITY_LAYER"
log "  period_granularity  = $COST_PERIOD_GRANULARITY"
log "  materialization     = $COST_MATERIALIZATION"
log "  ldgr_row_count      = $LDGR_ROW_COUNT"
log "  ldgr_file_bytes     = $LDGR_FILE_BYTES"
log "  view_file_bytes     = $VIEW_FILE_BYTES"
log "  permutation_rows    = $PERMUTATION_ROWS"
log "  permutation_capacity = $PERMUTATION_CAPACITY"
log "  cost_surface        = $COST_SURFACE"
log "======================================================================"

record_cost_point "$COST_SURFACE" "$RUN_ID" "$RUN_TS" "$CURVE" "$CONFIG_DESC" \
  "$YEARS" "$STEPS" "$TOTAL_COMPUTE_S" "$FINAL_STORAGE_BYTES" "$MASTER_FILE_COUNT" \
  "$CAPABILITY_LAYER" "$COST_PERIOD_GRANULARITY" "$COST_MATERIALIZATION" \
  "$LDGR_ROW_COUNT" "$LDGR_FILE_BYTES" "$VIEW_FILE_BYTES" "$PERMUTATION_ROWS" "$PERMUTATION_CAPACITY"

log "Cost point recorded → $COST_SURFACE"

RUN_END_EPOCH=$(date +%s)
bash "$SCRIPT_DIR/write_canonical_evidence.sh" \
  --run-id "$RUN_ID" \
  --run-timestamp "$RUN_TS" \
  --in-path "$IN_PATH" \
  --out-path "$OUT_PATH" \
  --years "$YEARS" \
  --steps "$STEPS" \
  --curve "$CURVE" \
  --config "$CONFIG_DESC" \
  --start-epoch "$RUN_START_EPOCH" \
  --end-epoch "$RUN_END_EPOCH" \
  --compute-seconds "$TOTAL_COMPUTE_S" \
  --log-file "$LOG_FILE" \
  --workload-profile "$WORKLOAD_PROFILE" \
  --materialization-profile "$MATERIALIZATION_PROFILE" \
  --period-granularity "$COST_PERIOD_GRANULARITY" \
  --ldgr-row-count "$LDGR_ROW_COUNT" \
  --ldgr-file-bytes "$LDGR_FILE_BYTES" \
  --view-file-bytes "$VIEW_FILE_BYTES" \
  --permutation-rows "$PERMUTATION_ROWS" \
  --permutation-capacity "$PERMUTATION_CAPACITY"

bash "$SCRIPT_DIR/interpret_run.sh" \
  "${OUT_PATH%/}/metrics" \
  "${OUT_PATH%/}/interpretation"

# Fail fast if any reconciliation control failed
if [[ -f "${OUT_PATH%/}/metrics/reconciliation_results.csv" ]]; then
  failed_controls=$(awk -F',' 'NR > 1 { gsub(/"/, "", $9); if ($9 == "FAIL") { printf "  check_id=%s partition=%s delta=%s (expected %s, actual %s)\n", $2, $4, $7, $5, $6 } }' "${OUT_PATH%/}/metrics/reconciliation_results.csv")
  if [[ -n "$failed_controls" ]]; then
    log "ERROR: One or more reconciliation controls failed:"
    printf "%s\n" "$failed_controls" >&2
    exit 1
  fi
fi
