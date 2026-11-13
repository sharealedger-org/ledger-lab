#!/usr/bin/env bash
# Validate existing engine evidence and required artifacts. No financial calculations.
# Usage: validate_corpus_run.sh OUTPUT_ROOT YEARS E|R|B
set -euo pipefail
root="${1:?output root required}"; years="${2:?years required}"; family="${3:?TTL type required}"
fail() { echo "VALIDATION FAILED: $*" >&2; exit 1; }
require_file() { [[ -s "$1" ]] || fail "missing/empty artifact: $1"; }
check_csv() {
  local file="$1" mode="$2"
  require_file "$file"
  # Canonical metrics use quoted scalars and semicolon lists. Reject unexpected field counts.
  # This consumes engine verdicts; it does not recompute financial totals.
  awk -F, -v mode="$mode" '
    { for(i=1;i<=NF;i++) { sub(/\r$/, "", $i); gsub(/^"|"$/, "", $i) } }
    NR==1 { width=NF; for(i=1;i<=NF;i++) col[$i]=i; next }
    NF!=width { bad=1; next }
    { rows++; status=$(col["status"])
      if(mode=="controls") {
        if(status!="PASS") bad=1
        checks[$(col["check_id"])]=1
      } else {
        if(status!="complete") bad=1
        if($(col["wall_seconds"])=="" || $(col["peak_rss_bytes"])=="") bad=1
        steps[$(col["process_name"])]=1
      }
    }
    END {
      if(!rows || bad || !("status" in col)) exit 1
      if(mode=="controls" && (!checks["sje_balance"] || !checks["ledger_balance"])) exit 1
    }' "$file" || fail "invalid or failing $mode evidence: $file"
}
IFS=, read -ra year_list <<< "$years"
for year in "${year_list[@]}"; do
  [[ "$year" =~ ^[0-9][0-9]$ ]] || fail "invalid year $year"
  dir="$root/FY$year"
  for file in "SortedJEFY${year}merged.csv" "LDGR20${year}.csv" "GL20${year}.csv" "PERM_TBL20${year}.csv"; do
    require_file "$dir/$file"
  done
  if [[ "$family" != E ]]; then require_file "$dir/SortedJEFY${year}rev.csv"; fi
  check_csv "$dir/metrics/reconciliation_results.csv" controls
  check_csv "$dir/metrics/process_metrics.csv" processes
  echo "PASS FY$year: required outputs and engine controls"
done
for view in monthly_order_volume vendor_year_survival agency_year_commodity_mix agency_year_payment_totals; do
  require_file "$root/VIEWS/view_${view}.csv"
done
check_csv "$root/VIEWS/metrics/process_metrics.csv" processes
echo "PASS corpus: required cross-year views and process evidence"
echo "Scope: engine verdicts/artifact presence; not independent source reconciliation or vendor-name coverage."
