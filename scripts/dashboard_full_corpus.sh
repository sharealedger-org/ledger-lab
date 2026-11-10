#!/usr/bin/env bash
# Read-only per-year dashboard for scripts/run_full_corpus.sh. Usage: bash scripts/dashboard_full_corpus.sh [seconds]  (default 30; Ctrl-C to stop)
# Parses START/RESULT lines in $HOME/workspace/va_fresh_logs/run2_prep_fy*.log and run2_chain_fy*.log. Changes nothing, runs no engine step.
# The "rows" line under each year shows records read>written for each finished step (written only when the step reports no input
# count, e.g. 2m). Row counts drive step time. Note: 3, 20 and 21 report the SJE rows they read and the ledger/GL/permutation rows they wrote.
# Under Total, the rows line shows all rows read plus written across the year's finished steps (a row passed through several steps is
# counted once per step, so this is volume handled, not distinct records). The footer gives the same sum over all years.
# One pass costs well under one CPU-second; the 30 s default keeps it out of the way of the run.
#
# Total column: sum of the year's finished step times plus the running step's elapsed time (step time, not wall clock).
# Projection (rough): for each of the six steps, the mean of the last 3 completed years; remaining work is the sum over unfinished
# steps (a running step counts only what is left). JE generation is divided across the lanes, posting is serial. It excludes the
# final 5v pass, ignores contention changes as lanes drain, and later fiscal years are larger, so treat it as an estimate.
L="$HOME/workspace/va_fresh_logs"; O="$HOME/workspace/va_fresh_root/output_full"
N="${1:-30}"

# stat <logfile> <step>  ->  sets ST (none|run|ok|fail), SV (seconds, or rc for fail), RI/RO (records_read / records_written)
stat() {
  local f="$1" s="$2" res st t0 now
  ST=none; SV=0; RI=0; RO=0
  [[ -f "$f" ]] || return
  res=$(grep -E "RESULT step=$s year=[0-9]+ quarter" "$f" | tail -n 1)
  if [[ -n "$res" ]]; then
    local rc; rc=$(sed -nE 's/.*[ ]rc=([0-9]+).*/\1/p' <<<"$res")
    if [[ "$rc" == "0" ]]; then
      ST=ok; SV=$(sed -nE 's/.*elapsed_s=([0-9]+).*/\1/p' <<<"$res")
      RI=$(sed -nE 's/.*records_read=([0-9]+).*/\1/p' <<<"$res"); RI="${RI:-0}"
      RO=$(sed -nE 's/.*records_written=([0-9]+).*/\1/p' <<<"$res"); RO="${RO:-0}"
    else ST=fail; SV=$rc; fi
    return
  fi
  st=$(grep -E "START +step=$s +year=" "$f" | tail -n 1)
  if [[ -n "$st" ]]; then
    t0=$(date -j -f '%Y-%m-%d %H:%M:%S' "$(sed -nE 's/^\[([0-9-]+ [0-9:]+)\].*/\1/p' <<<"$st")" +%s 2>/dev/null)
    now=$(date +%s); ST=run; SV=$(( now - ${t0:-$now} ))
  fi
}
fmt_ms() { printf '%d:%02d' $(( $1 / 60 )) $(( $1 % 60 )); }
fmt_hm() { printf '%dh%02dm' $(( $1 / 3600 )) $(( ($1 % 3600) / 60 )); }
fmt_n() {  # compact row count: 364142 -> 364k, 9307494 -> 9.3M
  if (( $1 >= 1000000 )); then awk -v n="$1" 'BEGIN{printf "%.1fM", n/1000000}'
  elif (( $1 >= 1000 )); then printf '%dk' $(( $1 / 1000 ))
  else printf '%d' "$1"; fi
}
rcell() {  # rows cell for the current ST/RI/RO: "in>out", or just "out" when the step reports no input count
  [[ "$ST" == ok ]] || { printf '-'; return; }
  if (( RI > 0 )); then printf '%s>%s' "$(fmt_n "$RI")" "$(fmt_n "$RO")"; else fmt_n "$RO"; fi
}
cell() {  # prints the cell text for the current ST/SV
  case "$ST" in
    ok) printf '%ds ok' "$SV" ;;
    run) printf 'RUN %s' "$(fmt_ms "$SV")" ;;
    fail) printf 'FAIL rc=%s' "$SV" ;;
    *) printf '-' ;;
  esac
}

STEPS=(ttl 2p 2m 3 20 21)
while true; do
  M=$(ls -dt "$L"/markers_* 2>/dev/null | head -n 1)
  W=$(ls -t "$L"/corpus_wallclock_*.csv 2>/dev/null | head -n 1)
  YEARS=$(sed -nE 's/^# years=([0-9,]+).*/\1/p' "$W" 2>/dev/null)
  YEARS="${YEARS:-03,04,05,06,07,08,09,10,11,12,13,14,15,16}"
  LANES=$(sed -nE 's/^# years=.* lanes=([0-9]+).*/\1/p' "$W" 2>/dev/null); LANES="${LANES:-4}"
  T0=$(awk -F, '$1=="corpus_start"{print $2}' "$W" 2>/dev/null); now=$(date +%s)

  # ── collect every year's six steps first (ST/SV per step), so the table and the projection use one snapshot ──
  yl=(); i=0
  for y in ${YEARS//,/ }; do
    yl[i]="$y"
    for k in 0 1 2 3 4 5; do
      if (( k < 3 )); then stat "$L/run2_prep_fy$y.log" "${STEPS[k]}"; else stat "$L/run2_chain_fy$y.log" "${STEPS[k]}"; fi
      SS[i*6+k]="$ST"; SSV[i*6+k]="$SV"; SRI[i*6+k]="$RI"; SRO[i*6+k]="$RO"
    done
    i=$((i+1))
  done
  ntot=$i

  # ── per-step reference time: mean of the last 3 completed years ──
  for k in 0 1 2 3 4 5; do
    sum=0; cnt=0
    for (( j=ntot-1; j>=0 && cnt<3; j-- )); do
      if [[ "${SS[j*6+k]}" == ok ]]; then sum=$(( sum + SSV[j*6+k] )); cnt=$((cnt+1)); fi
    done
    (( cnt > 0 )) && REF[k]=$(( sum / cnt )) || REF[k]=0
    NREF[k]=$cnt
  done

  # ── remaining work ──
  je_rem=0; post_rem=0; known=1
  for (( j=0; j<ntot; j++ )); do
    for k in 0 1 2 3 4 5; do
      case "${SS[j*6+k]}" in
        ok) rem=0 ;;
        run) rem=$(( REF[k] - SSV[j*6+k] )); (( rem < 0 )) && rem=0 ;;
        *) rem=${REF[k]} ;;
      esac
      (( NREF[k] == 0 )) && [[ "${SS[j*6+k]}" != ok ]] && known=0
      if (( k < 3 )); then je_rem=$(( je_rem + rem )); else post_rem=$(( post_rem + rem )); fi
    done
  done
  post_year=$(( REF[3] + REF[4] + REF[5] ))
  je_fin=$(( je_rem / LANES + post_year ))
  eta=$post_rem; (( je_fin > eta )) && eta=$je_fin

  clear
  printf '=== VA full corpus   %s   elapsed %s   corpus process: %s ===\n\n' "$(date '+%H:%M:%S')" \
    "$(awk -v e=$(( now - ${T0:-$now} )) 'BEGIN{printf "%d:%02d:%02d", e/3600, (e%3600)/60, e%60}')" \
    "$(pgrep -f run_full_corpus.sh >/dev/null && echo ALIVE || echo STOPPED)"
  printf '%-6s | %-32s | %-32s |\n' '' 'JE GENERATION' 'POSTING'
  printf '%-6s | %-10s %-10s %-10s | %-10s %-10s %-10s | %-7s | %s\n' YEAR 'Pay JEs' 'PO JEs' 'Merge' 'Post' 'GL Bal' 'Permute' Total STATE
  printf -- '-------+----------------------------------+----------------------------------+---------+---------------\n'
  nprep=0; nchain=0; grows=0
  for (( j=0; j<ntot; j++ )); do
    y="${yl[j]}"; cells=(); rows=(); tot=0; rtot=0
    for k in 0 1 2 3 4 5; do
      ST="${SS[j*6+k]}"; SV="${SSV[j*6+k]}"; RI="${SRI[j*6+k]}"; RO="${SRO[j*6+k]}"; cells[k]="$(cell)"; rows[k]="$(rcell)"
      [[ "$ST" == ok || "$ST" == run ]] && tot=$(( tot + SV ))
      [[ "$ST" == ok ]] && rtot=$(( rtot + RI + RO ))
    done
    state="queued"
    [[ -f "$L/run2_prep_fy$y.log" ]] && state="JE gen"
    [[ -f "$M/prep_$y.ok" ]] && { state="JEs done"; nprep=$((nprep+1)); }
    [[ -f "$L/run2_chain_fy$y.log" ]] && state="Posting"
    [[ "${SS[j*6+5]}" == ok ]] && { state="DONE"; nchain=$((nchain+1)); }
    [[ -f "$M/prep_$y.fail" || -f "$M/chain_$y.fail" ]] && state="FAILED"
    totc="-"; (( tot > 0 )) && totc="$(fmt_ms "$tot")"
    printf 'FY20%s | %-10s %-10s %-10s | %-10s %-10s %-10s | %-7s | %s\n' "$y" "${cells[0]}" "${cells[1]}" "${cells[2]}" "${cells[3]}" "${cells[4]}" "${cells[5]}" "$totc" "$state"
    grows=$(( grows + rtot )); rtotc="-"; (( rtot > 0 )) && rtotc="$(fmt_n "$rtot")"
    printf '  rows | %-10s %-10s %-10s | %-10s %-10s %-10s | %-7s |\n' "${rows[0]}" "${rows[1]}" "${rows[2]}" "${rows[3]}" "${rows[4]}" "${rows[5]}" "$rtotc"
  done
  # ── cross-year views pass (5v): one pass over every year's merged file, starts after the chain ──
  stat "$L/run2_views.log" 5v; vst="$ST"
  vcell="$(cell)"; vrows="$(rcell)"; vtot=0
  if [[ "$ST" == ok ]]; then
    vtot=$(( RI + RO )); grows=$(( grows + vtot ))
    vres=$(grep -E "RESULT step=5v year=[0-9]+ rc=" "$L/run2_views.log" 2>/dev/null | tail -n 1)
    vdet=""
    for key in view_monthly view_vendor view_agency_commodity view_agency_payment; do
      n=$(sed -nE "s/.*[ ]${key}=([0-9]+).*/\1/p" <<<"$vres"); [[ -n "$n" ]] && vdet="$vdet ${key#view_}=$(fmt_n "$n")"
    done
  fi
  printf -- '-------+----------------------------------+----------------------------------+---------+---------------\n'
  printf 'VIEWS  | %-10s (5v, all %s years: one pass over the merged files)%s\n' "$vcell" "$ntot" "$( [[ "$vst" == none ]] && echo '  - waiting for posting' )"
  (( vtot > 0 )) && printf '  rows | %s read>written%s   (total %s)\n' "$vrows" "${vdet:+;$vdet}" "$(fmt_n "$vtot")"
  echo
  case "$vst" in none) vword=waiting ;; run) vword=running ;; ok) vword=done ;; fail) vword=FAILED ;; esac
  echo "JE generation done: $nprep/$ntot    posting done: $nchain/$ntot    views (5v): $vword"
  echo "rows handled (read + written, all finished steps, all years): $(printf '%s' "$grows" | awk '{s=$1; o=""; while (length(s)>3) {o="," substr(s,length(s)-2) o; s=substr(s,1,length(s)-3)} print s o}')"
  if (( nchain == ntot )); then
    case "$vst" in
      none) echo "projection: all years posted; the 5v pass has not started (no estimate)" ;;
      run) echo "projection: all years posted; 5v running (no estimate)" ;;
      *) echo "projection: all years posted; 5v $vst" ;;
    esac
  elif (( known == 0 )); then
    echo "projection: not enough completed steps yet"
  else
    echo "projection (rough, excludes 5v): JE generation ~$(date -r $(( now + je_rem / LANES )) +%H:%M)   posting ~$(date -r $(( now + eta )) +%H:%M)   (about $(fmt_hm "$eta") from now)"
    echo "            mean of last 3 completed years per step; later years are larger, so expect later"
  fi
  echo "launch retries: $(cat "$L"/run2_*.log 2>/dev/null | grep -c 'RETRY ')    fail markers: $(find "$M" -name '*.fail' 2>/dev/null | wc -l | tr -d ' ')"
  echo "output: $(du -sh "$O" 2>/dev/null | cut -f1)    free: $(df -h "$HOME" | awk 'NR==2{print $4}')    $(uptime | sed 's/.*load/load/')    java: $(pgrep -f 'java.*sbt' | wc -l | tr -d ' ')"
  sleep "$N"
done
