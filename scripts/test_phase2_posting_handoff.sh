#!/usr/bin/env bash
# Exercise the derived driver's real Bash handoff into isolated Scala posting.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
root="${1:?absolute fresh test output root}"
[[ "$root" == /* && ! -e "$root" ]] || exit 2
mkdir -p "$root/input/VARawFiles" "$root/input/VAPOData" "$root/logs"
cp "$repo/data/FY03q1exp_small.txt" "$root/input/VARawFiles/FY03q1exp.txt"
cp "$repo/data/VendorMaster.csv" "$repo/data/VAAccountingRules.csv" "$root/input/"
export JAVA_OPTS='-Dfile.encoding=UTF-8 -Xms1024m -Xmx1024m -XX:ActiveProcessorCount=4'
bash "$repo/scripts/run_pipeline.sh" --inPath "$root/input/VARawFiles" \
  --outPath "$root/financial" --years 03 --steps 2 --vendorMaster "$root/input/VendorMaster.csv" \
  --periodGranularity monthly --profile phase2-handoff-fixture \
  > "$root/logs/prepare.log" 2>&1
ln "$root/financial/SortedJEFY03q1exp.csv" "$root/financial/SortedJEFY03merged.csv"
before="$(shasum -a 256 "$root/financial/SortedJEFY03merged.csv" | awk '{print $1}')"
# Use the actual driver function, not a duplicated posting invocation.
sed -n '/^pipeline() {/,/^}/p' "$repo/scripts/run_phase2_derived_baseline.sh" > "$root/pipeline_function.sh"
source "$root/pipeline_function.sh"
gate() { :; }
pipeline post_fixture "$root/input" "$root/financial" 03 3,20,21
[[ -L "$root/financial" && -s "$root/financial/LDGR2003.csv" && -s "$root/financial/GL2003.csv" ]]
after="$(shasum -a 256 "$root/financial/SortedJEFY03merged.csv" | awk '{print $1}')"
[[ "$before" == "$after" ]]
for step in 3 20 21; do
  rg -q "RESULT step=$step year=03 .*rc=0" "$root/logs/post_fixture.log"
done
rg -q 'SortedJEFY03merged.csv' "$root/logs/post_fixture.log"
printf 'PASS actual derived Bash handoff: merged source unchanged; posting/GL/permutation completed\n'
