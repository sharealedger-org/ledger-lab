#!/usr/bin/env bash
# Read-only operational display; classes occupy rows, processing steps columns.
set -eu
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
default_root="$repo/session-run.tmp/phase2_derived_baseline_v1"
[[ ! -d "$repo/session-run.tmp/phase2_derived_baseline.current" ]] || default_root="$(cd "$repo/session-run.tmp/phase2_derived_baseline.current" && pwd -P)"
root="${1:-$default_root}"
interval="${2:-30}"
[[ -d "$root" && "$interval" =~ ^[0-9]+$ ]] || exit 2
compact() {
  awk -v n="${1:-0}" 'BEGIN{if(n>=1000000)printf "%.1fM",n/1000000;else if(n>=1000)printf "%.0fk",n/1000;else printf "%.0f",n}'
}
engine_cell() {
  local file="$1" step="$2" stage="$3" kind value rin rout stamp elapsed
  CELL=-; ROWS=-
  [[ -f "$file" ]] || return 0
  read -r kind value rin rout <<< "$(awk -v step="$step" '
    function field(key,i,a){for(i=1;i<=NF;i++){split($i,a,"=");if(a[1]==key)return a[2]}return 0}
    $0 ~ "START +step=" step " +year=" {started=substr($0,2,19)}
    $0 ~ "RESULT step=" step " year=" {found=1;rc=field("rc");seconds=field("elapsed_s");ri=field("records_read");ro=field("records_written")}
    END{if(found)print (rc==0?"ok":"fail"),seconds,ri,ro;else if(started!=""){gsub(/ /,"T",started);print "run",started,0,0}else print "none",0,0,0}
  ' "$file")"
  case "$kind" in
    ok) CELL="${value}s"; ROWS="$(compact "$rin")>$(compact "$rout")" ;;
    fail) CELL=FAILED ;;
    run)
      if [[ -f "$root/STOPPED_BY_OWNER_REORG" && "$current" == "$stage" ]]; then CELL=STOPPED
      elif [[ -f "$root/FAILED" && "$current" == "$stage" ]]; then CELL=FAILED
      else
        stamp="$(TZ=America/Phoenix date -j -f '%Y-%m-%dT%H:%M:%S' "$value" +%s 2>/dev/null || date +%s)"
        elapsed=$(( $(date +%s) - stamp )); CELL="RUN $((elapsed/60)):$((elapsed%60))"
      fi ;;
  esac
}
operation_cell() {
  local stage="$1" marker="$2" seconds
  CELL=-; ROWS=-
  [[ -f "$root/logs/$stage.log" ]] || return 0
  if [[ -f "$marker" ]] || awk -F, -v s="$stage" '$1==s{seen=1;next}seen{done=1}END{exit !done}' "$root/storage_gates.csv"; then
    seconds="$(awk '$2=="real"{printf "%.0f",$1}' "$root/logs/$stage.log")"
    CELL="${seconds:-?}s"
  elif [[ -f "$root/STOPPED_BY_OWNER_REORG" && "$current" == "$stage" ]]; then CELL=STOPPED
  elif [[ -f "$root/FAILED" && "$current" == "$stage" ]]; then CELL=FAILED
  elif [[ "$current" == "$stage" ]]; then CELL=RUN
  else CELL=started; fi
}
class_row() {
  local label="$1" stage="$2" normalization="$3" combined="$4" file="$root/logs/$2.log" step state idx=0 cells=() counts=()
  for step in ttl 2p 2m normalize 3 20 21; do
    CELL=-; ROWS=-
    if [[ "$step" == normalize && -n "$normalization" ]]; then
      operation_cell "$normalization" "$root/normalized/FY$new/NORMALIZATION_COMPLETE"
      if [[ -f "$root/normalized/FY$new/normalization_controls.csv" ]]; then
        ROWS="$(compact "$(awk -F, '$1=="journal_rows_preserved"{print $2}' "$root/normalized/FY$new/normalization_controls.csv")")"
      fi
    elif [[ "$step" == 2m && "$combined" == yes ]]; then
      operation_cell "merge_$new" "$root/output/FY$new/SortedJEFY${new}merged.csv"
      if [[ -f "$root/logs/merge_$new.log" ]]; then
        ROWS="$(awk '/RESULT step=2m /{for(i=1;i<=NF;i++)if($i~/^records_written=/){split($i,a,"=");n=a[2]}}END{if(n!="")print n}' "$root/logs/merge_$new.log")"
        [[ -z "$ROWS" ]] && ROWS=- || ROWS="$(compact "$ROWS")"
      fi
    elif [[ "$step" != normalize ]]; then engine_cell "$file" "$step" "$stage"; fi
    cells[idx]="$CELL"; counts[idx]="$ROWS"; idx=$((idx+1))
  done
  state=queued
  [[ ! -f "$file" ]] || state=prepared
  [[ "$combined" != yes || "${cells[6]}" == - ]] || state=posted
  [[ "$current" != "$stage" && "$current" != "$normalization" ]] || state=ACTIVE
  if [[ "$state" == ACTIVE && -f "$root/STOPPED_BY_OWNER_REORG" ]]; then state=STOPPED
  elif [[ "$state" == ACTIVE && -f "$root/FAILED" ]]; then state=FAILED; fi
  printf '%-18s |' "$label"
  for CELL in "${cells[@]}"; do printf ' %-11s' "$CELL"; done
  printf ' | %s\n' "$state"
  if [[ "$state" != queued && ( "$show_counts" == yes || "${DASHBOARD_COUNTS:-active}" == all ) ]]; then
    printf '%-18s |' '  rows in>out'
    for ROWS in "${counts[@]}"; do printf ' %-11s' "$ROWS"; done
    printf ' |\n'
  fi
}
fused_count() {
  local file="$1" kind="$2" adapter="$3" pair accepted journals
  [[ -s "$file" ]] || { printf '-'; return; }
  pair="$(awk -F, -v k="$kind" -v a="$adapter" '$1==k&&$2==a{print $3,$4}' "$file")"
  [[ -n "$pair" ]] || { printf '-'; return; }
  read -r accepted journals <<< "$pair"
  printf '%s>%s' "$(compact "$accepted")" "$(compact "$journals")"
}
global_stage_row() {
  local label="$1" stage="$2" marker="$3" value=pending report="$root/report" state partial_count partial
  local partials=()
  [[ -d "$report" ]] || report="$report.attempt"
  if [[ -f "$marker" || -f "$root/stages/$stage.complete" ]]; then value=done
  elif [[ "$current" == "$stage" ]]; then value=RUN
  elif [[ "$current" == reporting ]]; then
    case "$stage" in
      report_extract) [[ -f "$report/EXTRACT_COMPLETE" ]] && value=done || value=RUN ;;
      report_reduce) [[ ! -f "$report/EXTRACT_COMPLETE" ]] || value=RUN ;;
    esac
  fi
  if [[ "$value" == RUN && -f "$root/FAILED" ]]; then value=FAILED; fi
  state="$value"
  # Current temporary files show activity; consolidation can reduce this count.
  if [[ ( "$stage" == report_extract || "$stage" == report_reduce ) && "$value" == RUN && -d "$report" ]]; then
    partials=("$report"/*_partials/*.csv "$report"/spill_*.csv)
    partial_count=0
    for partial in "${partials[@]}"; do
      [[ ! -f "$partial" ]] || partial_count=$((partial_count+1))
    done
    value="RUN $partial_count spills"
  fi
  printf '%-23s | %-17s %-19s %-12s | %s\n' "$label" - "$value" - "$state"
}
render_fused() {
  local current old new sy base source phase source_state post_state checks state worker report latest stopped=no
  current="$(cat "$root/STATUS")"
  printf 'PHASE 2 FUSED GENERATION PROOF   %s\n' "$(TZ=America/Phoenix date '+%Y-%m-%d %H:%M:%S AZT')"
  printf 'Workflow: %s\n' "$current"
  [[ ! -f "$root/SCOPE" ]] || cat "$root/SCOPE"
  if [[ -f "$root/STOPPED_FOR_DATE_HOTPATH" ]]; then
    stopped=yes;cat "$root/STOPPED_FOR_DATE_HOTPATH"
    [[ ! -f "$repo/session-run.tmp/phase2_fused_preparation_v1/STATUS" ]] || cat "$repo/session-run.tmp/phase2_fused_preparation_v1/STATUS"
  elif [[ -f "$root/FAILED" ]]; then cat "$root/FAILED";fi
  printf '\n%-23s | %-17s %-19s %-12s | STATE\n' 'YEAR / WORK' 'Source + sort' 'Post / GL / Perm' 'Controls'
  for old in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do
    new="$(printf '%02d' "$((10#$old+28))")";sy="$(printf '%02d' "$((10#$old-2))")"
    worker="$root/workers/FY$new.status"
    source_state=-;post_state=-;checks=-;state=pending
    if [[ -f "$worker" ]]; then
      phase="$(cat "$worker")";base="$root/years/FY$new"
      [[ -d "$base" ]] || base="$base.attempt"
      source="$base/source";[[ -d "$source" ]] || source="$source.attempt"
      source_state=RUN;state=source
      [[ ! -f "$source/SOURCE_PREPARATION_COMPLETE" ]] || { source_state=done;state=prepared; }
      case "$phase" in fused_post_*|financial_controls_*) post_state=RUN;state=posting ;; esac
      if [[ -f "$base/output/FY$new/fused_posting_controls.csv" ]]; then post_state=done;checks=RUN;state=controls;fi
      if [[ -f "$base/FINANCIAL_YEAR_COMPLETE" ]]; then checks=PASS;state=PASS;fi
      if [[ "$stopped" == yes && "$state" != PASS ]]; then
        state=STOPPED
        [[ "$source_state" != RUN ]] || source_state=STOPPED
        [[ "$post_state" != RUN ]] || post_state=STOPPED
        [[ "$checks" != RUN ]] || checks=STOPPED
      elif [[ -f "$root/FAILED" && "$state" != PASS ]]; then state=FAILED;fi
    fi
    printf '%-23s | %-17s %-19s %-12s | %s\n' "FY$old / SY$sy" "$source_state" "$post_state" "$checks" "$state"
  done
  printf -- '------------------------+------------------------------------------------------\n'
  report="$root/report";[[ -d "$report" ]] || report="$report.attempt"
  global_stage_row 'Corpus controls' financial_controls "$root/financial_controls/BASELINE_FINANCIAL_CONTROLS_COMPLETE"
  global_stage_row 'Dimensions / metadata' dimensions "$root/dimensions/DIMENSIONS_COMPLETE"
  global_stage_row 'Reporting sort' report_order "$root/report_order/SORT_COMPLETE"
  global_stage_row 'Reporting extract' report_extract "$report/EXTRACT_COMPLETE"
  global_stage_row 'Reporting reduction' report_reduce "$report/REPORT_COMPLETE"
  global_stage_row 'Reporting checks' report_checks "$root/REPORT_CHECKS_COMPLETE"
  global_stage_row 'Output census' census "$root/CENSUS_COMPLETE"
  global_stage_row 'Result validation' result_review "$root/RESULT_REVIEW_COMPLETE"
  global_stage_row 'Zenodo script ready' zenodo_script "$root/ZENODO_SCRIPT_READY"
  printf '\nEach year combines historical and generated inputs. ID/date runs in ARE; GL/Perm share posting.\n'
  [[ ! -f "$root/ADMISSION_COMPLETE" ]] || { printf 'Admission: ';cat "$root/ADMISSION_COMPLETE"; }
  printf 'Disk: ';df -h "$root" | awk 'NR==2{printf "%s free\n",$4}'
  printf 'Latest sample (UTC, free KiB, allocated KiB): ';tail -n 1 "$root/monitor_storage.csv" 2>/dev/null || true
  latest="$(ls -t "$root/logs"/FY*.log 2>/dev/null | head -n 1 || true)"
  [[ ! -f "$root/logs/$current.log" ]] || latest="$root/logs/$current.log"
  if [[ -n "$latest" ]]; then printf 'Latest log:\n';tail -n 3 "$latest" | tr '\r' '\n';fi
  printf 'Run: %s\nRefresh: %ss; Ctrl-C closes display only.\n' "$root" "$interval"
}
render() {
  if [[ -f "$root/PROFILE" && "$(cat "$root/PROFILE")" == phase2-fused-v1 ]]; then render_fused;return;fi
  local current old new sy stage show_counts
  current="$(cat "$root/STATUS" 2>/dev/null || echo admission)"
  printf 'PHASE 2 — HISTORICAL + GENERATED BASELINE   %s\n' "$(TZ=America/Phoenix date '+%Y-%m-%d %H:%M:%S AZT')"
  printf 'Current stage: %s\n' "$current"
  if [[ -f "$root/STOPPED_BY_OWNER_REORG" ]]; then cat "$root/STOPPED_BY_OWNER_REORG"
  elif [[ -f "$root/FAILED" ]]; then cat "$root/FAILED"; fi
  if [[ -f "$root/STOPPED_BY_OWNER_REORG" && -f "$repo/session-run.tmp/phase2_fused_preparation_v1/STATUS" ]]; then
    printf 'Reorganization: '; cat "$repo/session-run.tmp/phase2_fused_preparation_v1/STATUS"
  fi
  [[ ! -f "$root/WORKFLOW_COMPLETE" ]] || cat "$root/WORKFLOW_COMPLETE"
  printf '\n%-18s | %-11s %-11s %-11s %-11s %-11s %-11s %-11s | STATE\n' 'YEAR / CLASS' 'Pay JEs' 'PO JEs' 'Merge' 'ID/Date' 'Post' 'GL Bal' 'Permute'
  for old in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do
    new="$(printf '%02d' "$((10#$old+28))")"; sy="$(printf '%02d' "$((10#$old-2))")"
    if [[ ! -f "$root/logs/historical_$old.log" && ! -f "$root/logs/wholly_$new.log" && ! -f "$root/logs/post_$new.log" ]]; then
      printf '%-18s | %-11s %-11s %-11s %-11s %-11s %-11s %-11s | queued\n' "FY$old/SY$sy" - - - - - - -
      continue
    fi
    show_counts=no
    case "$current" in historical_$old|normalize_$old|wholly_$new|merge_$new|post_$new) show_counts=yes ;; esac
    class_row "FY$old historical" "historical_$old" "normalize_$old" no
    class_row "SY$sy generated" "wholly_$new" '' no
    class_row "SY$sy combined" "post_$new" '' yes
  done
  printf 'CORPUS STAGES (shared across all years)\n'
  for stage in financial_controls dimensions report_order reporting report_checks census; do
    operation_cell "$stage" "$root/WORKFLOW_COMPLETE"
    printf '  %-20s %s\n' "$stage" "$CELL"
  done
  printf '\nTimes: seconds; active-year rows: input>output. DASHBOARD_COUNTS=all shows every count row.\n'
  printf 'Historical/generated rows prepare inputs; combined rows merge and post them. Completion requires controls.\n'
  printf 'Latest disk sample (UTC, free KiB, allocated KiB): '
  tail -n 1 "$root/monitor_storage.csv" 2>/dev/null || true
  printf 'Run: %s\nRefresh: %ss; Ctrl-C closes this display only.\n' "$root" "$interval"
}
while true; do
  [[ ! -t 1 ]] || printf '\033[H\033[2J'
  render
  [[ "$interval" != 0 ]] || break
  sleep "$interval"
done
