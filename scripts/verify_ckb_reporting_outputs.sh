#!/usr/bin/env bash
# Post-engine exact-record audit; never compile into a live measured build.
set -euo pipefail
repo=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
baseline=${1:?accepted baseline root}
run=${2:?completed CKB run root}
[[ "$baseline" = /* && "$run" = /* && -f "$run/WORKFLOW_COMPLETE" && -f "$run/INPUT_EQUIVALENCE_COMPLETE" && -f "$run/REPORT_CHECKS_COMPLETE" && -f "$run/CENSUS_COMPLETE" ]]
audit="$run/exact_output_audit"
[[ ! -e "$audit" ]] || { echo 'Audit directory exists; preserve its receipt and choose an explicitly reviewed retry'; exit 2; }
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
echo 'Exact output equivalence complete; broader study acceptance remains separate.'
