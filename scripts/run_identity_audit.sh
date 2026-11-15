#!/usr/bin/env bash
# Read-only diagnostic orchestration; every count/parser is compiled Scala.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
root=""; run_root=""; master=""; current_master="$repo/data/VendorMaster.csv"; ref=""; out=""
years=03,04,05,06,07,08,09,10,11,12,13,14,15,16
while [[ $# -gt 0 ]]; do
  case "$1" in
    --root) root="$2"; shift 2 ;;
    --runRoot) run_root="$2"; shift 2 ;;
    --master) master="$2"; shift 2 ;;
    --currentMaster) current_master="$2"; shift 2 ;;
    --refPath) ref="$2"; shift 2 ;;
    --out) out="$2"; shift 2 ;;
    --years) years="$2"; shift 2 ;;
    *) echo "Unknown argument: $1" >&2; exit 1 ;;
  esac
done
[[ -d "$root" && -d "$run_root" && -s "$master" && -s "$current_master" && -d "$ref" && "$out" = /* ]] || {
  echo "Source root, successful runRoot, measured/current masters, refPath and absolute new out required" >&2; exit 1;
}
for path in "$repo" "$root" "$run_root" "$master" "$current_master" "$ref" "$out"; do
  [[ "$path" != *[[:space:]]* ]] || { echo "sbt runtime paths must have no whitespace" >&2; exit 1; }
done
[[ ! -e "$out" ]] || { echo "Audit destination exists" >&2; exit 1; }
[[ "$years" =~ ^[0-9][0-9](,[0-9][0-9])*$ ]] || exit 1
mkdir "$out"
mkdir -p "$out/inputs/reference" "$out/inputs/posted" "$out/inputs/audit_source"
cp "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/IdentityAudit.scala" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/IdentityAuditSummary.scala" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/IdentityAuditParserSummary.scala" \
  "$repo/scripts/run_identity_audit.sh" "$repo/docs/IDENTITY_AUDIT_CONTRACT.md" "$out/inputs/audit_source/"
cp "$master" "$out/inputs/measured_master.csv"
cp "$current_master" "$out/inputs/enriched_master.csv"
cp "$ref"/aRef*.txt "$out/inputs/reference/"
IFS=, read -ra year_list <<< "$years"
for year in "${year_list[@]}"; do
  physical="$(cd "$run_root/FY$year" && pwd -P)"
  [[ -s "$physical/SortedJEFY${year}merged.csv" && -s "$physical/LDGR20${year}.csv" ]] || exit 1
  ln -s "$physical" "$out/inputs/posted/FY$year"
done
printf 'git=%s\nsource_root=%s\nrun_root=%s\nyears=%s\nfinancial_state_changed=no\n' \
  "$(git -C "$repo" rev-parse HEAD)" "$root" "$run_root" "$years" > "$out/config.txt"
git -C "$repo" diff -- 02-foundation/va_pipeline scripts/run_identity_audit.sh docs/IDENTITY_AUDIT_CONTRACT.md > "$out/source_changes.patch"
shasum -a 256 "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/IdentityAudit.scala" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/IdentityAuditSummary.scala" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/IdentityAuditParserSummary.scala" \
  "$repo/scripts/run_identity_audit.sh" "$repo/docs/IDENTITY_AUDIT_CONTRACT.md" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/standardizeAndSort.scala" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/SortEngine.scala" \
  "$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/CorpusProfile.scala" > "$out/audit_source_sha256.txt"
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
run_stage() {
  local stage="$1" scala_command="$2"
  echo "Starting diagnostic phase $stage"
  if [[ "$(uname -s)" == Darwin ]]; then
    (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -l sbt -batch "$scala_command") > "$out/$stage.log" 2>&1
  else
    (cd "$repo/02-foundation/va_pipeline" && /usr/bin/time -v sbt -batch "$scala_command") > "$out/$stage.log" 2>&1
  fi
  echo "Diagnostic phase $stage completed"
}
for mode in master raw canonical; do
  command="runMain org.universalledger.foundation.va.corpus.IdentityAudit --mode $mode --out $out/$mode --root $root --runRoot $out/inputs/posted --master $out/inputs/measured_master.csv --currentMaster $out/inputs/enriched_master.csv --refPath $out/inputs/reference --years $years"
  run_stage "$mode" "$command"
  [[ -s "$out/$mode/AUDIT_COMPLETE" ]] || exit 1
done
run_stage summary "runMain org.universalledger.foundation.va.corpus.IdentityAuditSummary --auditRoot $out --runRoot $out/inputs/posted --out $out/summary"
run_stage parser_summary "runMain org.universalledger.foundation.va.corpus.IdentityAuditParserSummary --auditRoot $out --out $out/parser_summary"
date -u +%FT%TZ > "$out/AUDIT_COMPLETE"
echo "Audit complete: $out (diagnostic only; inspect source-count mismatches before conclusions)"
