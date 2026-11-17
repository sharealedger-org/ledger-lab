#!/usr/bin/env bash
# Post-engine exact-record audit; never compile into a live measured build.
set -euo pipefail
repo=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
baseline=${1:?accepted baseline root}
run=${2:?completed CKB run root}
[[ "$baseline" = /* && "$run" = /* && -f "$run/WORKFLOW_COMPLETE" && -f "$run/INPUT_EQUIVALENCE_COMPLETE" && -f "$run/REPORT_CHECKS_COMPLETE" && -f "$run/CENSUS_COMPLETE" ]]
audit="$run/exact_output_audit"
[[ ! -e "$audit" ]] || { echo 'Audit directory exists; preserve its receipt and choose an explicitly reviewed retry'; exit 2; }
trap 'rc=$?; if [[ $rc -ne 0 ]]; then printf "FAILED rc=%s\n" "$rc" > "$run/EXACT_OUTPUT_AUDIT_FAILED"; fi' EXIT
free=$(df -Pk "$run" | awk 'NR==2{print $4}')
((free >= 58720256)) || { echo 'Audit requires 40 GiB reserve plus 16 GiB scratch'; exit 2; }
mkdir -p "$audit/classes"
cp "$repo/tools/ReportingOutputEquivalence.scala" "$repo/tools/ReportingOutputEquivalenceCheck.scala" "$audit/"
shasum -a 256 "$audit/"*.scala > "$audit/source_sha256.txt"
java=/Users/ktwitchell001/.local/jdk-21.0.8+9/Contents/Home/bin/java
scala_boot=/Users/ktwitchell001/.sbt/boot/scala-2.12.20/org.scala-sbt/sbt/1.10.11
engine_classes="$repo/02-foundation/va_pipeline/target/scala-2.12/classes"
library="$scala_boot/scala-library-2.12.20.jar"
compiler="$scala_boot/scala-compiler-2.12.20.jar:$scala_boot/scala-reflect-2.12.20.jar:$library"
"$java" -Xmx512m -XX:ActiveProcessorCount=1 -cp "$compiler" scala.tools.nsc.Main -usejavacp \
  -classpath "$engine_classes:$library" -d "$audit/classes" "$audit/ReportingOutputEquivalence.scala" "$audit/ReportingOutputEquivalenceCheck.scala" > "$audit/compile.log" 2>&1
classpath="$audit/classes:$engine_classes:$library"
"$java" -Xmx512m -XX:ActiveProcessorCount=1 -cp "$classpath" ReportingOutputEquivalenceCheck > "$audit/tool_checks.log" 2>&1
/usr/bin/time -l "$java" -Xms256m -Xmx2048m -XX:ActiveProcessorCount=1 -cp "$classpath" ReportingOutputEquivalence \
  "$baseline/report" "$run/report" "$audit/scratch" > "$audit/comparison.log" 2>&1
shasum -a 256 --quiet -c "$audit/source_sha256.txt"
date -u '+%Y-%m-%dT%H:%M:%SZ' > "$run/EXACT_OUTPUT_EQUIVALENCE_COMPLETE"
timing() { awk '$2=="real"{value=$1} END{if(value=="")exit 1;print value}' "$1"; }
baseline_extract=$(timing "$baseline/report/extract.log")
baseline_reduce=$(timing "$baseline/report/reduce.log")
new_extract=$(timing "$run/report/extract.log")
new_reduce=$(timing "$run/report/reduce.log")
{
  printf '# Completed reporting comparison\n\n'
  printf 'Recorded: %s\n\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
  printf 'All 21 declared outputs passed exact full-record multiset comparison, including duplicates. Input manifests, independent reporting controls, output census and frozen reporting classes passed the driver gates.\n\n'
  printf '| Scope | Baseline seconds | CKB seconds |\n|---|---:|---:|\n'
  printf '| Extract JVM including startup | %s | %s |\n' "$baseline_extract" "$new_extract"
  printf '| Reduction JVM including startup | %s | %s |\n\n' "$baseline_reduce" "$new_reduce"
  awk -v be="$baseline_extract" -v br="$baseline_reduce" -v ne="$new_extract" -v nr="$new_reduce" \
    'BEGIN{printf "Combined extract/reduce: %.2f seconds baseline; %.2f seconds CKB; observed ratio %.3fx.\n\n",be+br,ne+nr,(be+br)/(ne+nr)}'
  printf 'Scope: accepted financial preparation and all 28 sorted financial partitions reused. Sorting, financial generation, checks, census and this audit are excluded from that ratio. Per-process timing/RSS and engine counters remain in their original logs/metrics.\n\n'
  printf 'Limits: one sequential observational pair, designated mid-baseline; no replicated paper cost point or causal training claim. Read-only monitoring occurred during processing. Startup/cache/environment differences are not isolated. This comparison does not implement an allocation/denominator feedback loop or establish target GenevaERS capability maturity. No upload or publication occurred.\n'
} > "$run/COMPARISON.md"
date -u '+%Y-%m-%dT%H:%M:%SZ' > "$run/COMPARISON_RECORD_COMPLETE"
echo 'Exact output equivalence complete; broader study acceptance remains separate.'
