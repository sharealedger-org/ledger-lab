#!/usr/bin/env bash
# Post-engine raw-row sampling only. No financial sums or replacement views.
set -euo pipefail
report=${1:?report}; out=${2:?new sampling directory}
[[ -d "$out" && ! -e "$out/temporal_selected.csv" && ! -e "$out/master_selected.csv" ]] || exit 2
pattern='^(v253147763127656255907762210784412714|v777305469290280721924923592149096066|v992291562825503351596510708858841042),'
head -1 "$report/vendor_temporal_cuts.csv" > "$out/temporal_selected.csv"
rg --no-heading --color never "$pattern" "$report/vendor_temporal_cuts.csv" | awk -F, '
  NF!=34 {print "Non-flat selected temporal row; use the Scala CSV reader" > "/dev/stderr";exit 1}
  {k=$1 SUBSEP $2 SUBSEP $4 SUBSEP $11 SUBSEP $13 SUBSEP $16 SUBSEP $18 SUBSEP $20;if(!seen[k]++)print}' >> "$out/temporal_selected.csv"
head -1 "$report/vendor_attribute_totals.csv" > "$out/master_selected.csv"
rg --no-heading --color never "$pattern" "$report/vendor_attribute_totals.csv" | awk -F, '
  NF!=17 {print "Non-flat selected master row; use the Scala CSV reader" > "/dev/stderr";exit 1}
  {k=$1 SUBSEP $2 SUBSEP $3 SUBSEP $7 SUBSEP $8 SUBSEP $9;if(!seen[k]++)print}' >> "$out/master_selected.csv"
head -1 "$report/vendor_temporal_cuts.csv" > "$out/zero_temporal_sample.csv"
rg --no-heading --color never -m 6 '^0000000000,' "$report/vendor_temporal_cuts.csv" >> "$out/zero_temporal_sample.csv"
