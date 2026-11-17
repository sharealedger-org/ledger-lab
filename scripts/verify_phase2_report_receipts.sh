#!/usr/bin/env bash
# Post-engine metadata validation only; no accounting or replacement control totals.
set -euo pipefail
root=${1:?absolute reporting root}; financial=${2:?absolute financial root}; out=${3:?new evidence directory}
fail() { printf 'Receipt verification failed: %s\n' "$*" >&2; exit 1; }
[[ "$root" = /* && "$financial" = /* && "$out" = /* ]] || fail 'absolute paths required'
root=$(cd "$root" && pwd -P); financial=$(cd "$financial" && pwd -P)
[[ "$root$financial$out" != *','* && "$root$financial$out" != *'"'* ]] || fail 'canonical metadata paths must have no comma or quote'
for marker in WORKFLOW_COMPLETE REPORT_CHECKS_COMPLETE CENSUS_COMPLETE report/EXTRACT_COMPLETE report/REPORT_COMPLETE; do
  [[ -f "$root/$marker" ]] || fail "not complete: $marker"
done
[[ ! -e "$root/FAILED" && ! -e "$root/report/FAILED" && ! -e "$out" ]] || fail 'failed attempt or existing evidence output'
mkdir -p "$out"
trap 'rc=$?; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$out/FAILED"; fi' EXIT
printf 'output,kind,rows,source\n' > "$out/expected_partitions.csv"
for year in 31 32 33 34 35 36 37 38 39 40 41 42 43 44; do
  [[ -f "$financial/years/FY$year/FINANCIAL_YEAR_COMPLETE" ]] || fail "missing FY$year completion"
  awk -F, -v y="$year" -v f="$financial" '
    NR==1 {if($0!="historical_year,synthetic_year,historical_journal_rows,wholly_journal_rows,combined_journal_rows,ledger_rows,gl_rows,journal_input_opens,process_seconds,status")exit 1;next}
    {if(NF!=10 || $2!=2000+y || $10!="PASS" || $8!=1 || $5!~/^[0-9]+$/ || $6!~/^[0-9]+$/)exit 1
     printf "FY%s/SortedJEFY%smerged.csv,movement,%s,%s/years/FY%s/output/FY%s/SortedJEFY%smerged.csv\n",y,y,$5,f,y,y,y
     printf "FY%s/LDGR20%s.csv,balance,%s,%s/years/FY%s/output/FY%s/LDGR20%s.csv\n",y,y,$6,f,y,y,y}
    END{if(NR!=2)exit 1}' "$financial/years/FY$year/fused_year_summary.csv" >> "$out/expected_partitions.csv" || fail "invalid FY$year summary"
done
awk -F, -v root="$root" '
  function bad(message){print message > "/dev/stderr";failed=1}
  FILENAME==ARGV[1] {if(FNR==1)next;expected[$1]=$3;kind[$1]=$2;source[$1]=$4;next}
  FILENAME==ARGV[2] {
    if(FNR==1){if($0!="source,output,kind,rows,bytes,source_sha256,sorted_sha256,spills,merge_levels,chunk_rows,fan_in,elapsed_seconds")bad("sort header");next}
    if(NF!=12 || !($2 in expected) || seenSort[$2]++ || $3!=kind[$2] || $4!=expected[$2] || $1!=source[$2])bad("sort population mismatch: " $2)
    if($5!~/^[0-9]+$/ || length($7)!=64 || $7~/[^0-9a-f]/)bad("sort hash/size: " $2)
    path=root "/report_order/" $2;rows[path]=$4;bytes[path]=$5;hash[path]=$7;kinds[path]=$3;sortCount++;total+=$4;next}
  FILENAME==ARGV[3] {
    if(FNR==1){if($0!="kind,path,rows,bytes,sha256")bad("extract header");next}
    if($1!="movement" && $1!="balance" && $1!="opening")next
    if(NF!=5 || !($2 in rows) || seenExtract[$2]++ || $1!=kinds[$2] || $3!=rows[$2] || $4!=bytes[$2] || $5!=hash[$2])bad("extract population/hash mismatch: " $2)
    extractCount++;actual+=$3; print $1 "," $2 "," $3 "," $4 "," $5 ",PASS"}
  END{if(sortCount!=28 || extractCount!=28 || total!=286277734 || actual!=total)bad("financial partition count/total mismatch");exit failed}' \
  "$out/expected_partitions.csv" "$root/report_order/sort_manifest.csv" "$root/report/input_manifest.csv" > "$out/partition_comparison.csv" || fail 'external report population comparison'
while IFS=, read -r source output kind rows bytes remainder; do
  [[ "$source" != source ]] || continue
  [[ -f "$source" && -f "$root/report_order/$output" ]] || fail "missing source/sorted partition: $output"
  [[ $(stat -f '%z' "$source") == "$bytes" && $(stat -f '%z' "$root/report_order/$output") == "$bytes" ]] || fail "partition byte size changed: $output"
done < "$root/report_order/sort_manifest.csv"
printf 'kind,path,bytes,sha256,status\n' > "$out/side_input_comparison.csv"
while IFS=, read -r kind path rows bytes digest; do
  case "$kind" in kind|movement|balance|opening) continue ;; esac
  [[ -f "$path" && $(stat -f '%z' "$path") == "$bytes" ]] || fail "side input missing/changed size: $path"
  actual_digest=$(LC_ALL=C shasum -a 256 "$path" | awk '{print $1}')
  [[ "$actual_digest" == "$digest" ]] || fail "side input changed hash: $path"
  printf '%s,%s,%s,%s,PASS\n' "$kind" "$path" "$bytes" "$digest" >> "$out/side_input_comparison.csv"
done < "$root/report/input_manifest.csv"
# Compare existing producer verdicts, never recompute financial amounts in Bash.
awk -F, 'NR==1{if($0!="check,expected,actual,status")exit 1;next}{n++;if(NF!=4 || $4!="PASS")bad=1}END{if(n!=28 || bad)exit 1}' \
  "$financial/financial_controls/baseline_controls.csv" || fail 'financial verdicts'
for name in reconciliation synthetic_validation synthetic_conservation; do
  # ConnectedViewsCheck uses the actual Scala CSV parser, including quoted fields.
  # Require its unique nonempty all-PASS scan result instead of splitting CSV here.
  awk -v name="$name.csv" '$1=="PASS" && $2==name && NF==3 {
    value=$3;sub(/^rows=/,"",value);if(value!~/^[1-9][0-9]*$/)bad=1;n++;rows=value}
    END{if(n!=1 || bad)exit 1;print rows}' \
    "$root/logs/report_checks.log" > "$out/${name}_verdict_rows.txt" || fail "$name Scala verdict scan"
done
awk -F, '$1=="financial_rows"{n++;if($2!=286277734)bad=1}END{exit(n!=1 || bad)}' \
  "$root/report/synthetic_metrics.csv" || fail 'synthetic financial input count'
LC_ALL=C shasum -a 256 --quiet -c "$root/reporting_classes_sha256.txt" || fail 'frozen class manifest'
cp -p "$root/report/input_manifest.csv" "$root/report_order/sort_manifest.csv" "$out/"
cp -p "$root/logs/report_checks.log" "$out/"
date -u '+%Y-%m-%dT%H:%M:%SZ' > "$out/RECEIPTS_VERIFIED"
printf 'PASS: exact28 financial inputs, counts/hashes, financial/report verdicts and frozen classes.\n'
printf 'Scope: receipt checks only; session exit, semantic review, census and final acceptance remain separate.\n'
