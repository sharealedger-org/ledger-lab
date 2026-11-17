#!/usr/bin/env bash
# Metadata/evidence acceptance only. All view arithmetic stays in Scala.
set -euo pipefail
baseline=${1:?accepted baseline root}; run=${2:?three-worker root}; mode=${3:-check}
repo=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
[[ "$mode" == check || "$mode" == finalize ]]
for marker in INPUT_EQUIVALENCE_COMPLETE REPORT_CHECKS_COMPLETE CENSUS_PRECISION_RECHECK_COMPLETE RELATIONSHIP_PRECISION_CORRECTION_COMPLETE; do
  [[ -f "$run/$marker" ]] || { echo "Pending $marker";exit 2; }
done
rg -q '^PASS selected_outputs=18 exactly equivalent$' "$run/remaining_views_audit.log" || { echo 'Pending remaining18 exact-output proof';exit 2; }
rg -q '^PASS selected_outputs=1 exactly equivalent$' "$run/relationship_precision_candidate/exact_audit.log"
rg -q 'Tests: succeeded 20, failed 0' "$repo/data/test_evidence/reporting_three_way_20261117/precision/tests.txt"
expected='movement_history balance_history reconciliation relationships vendor_activity aim_movements aim_balances aim_combined cross_vendor_totals vendor_temporal_cuts vendor_temporal_summary vendor_attribute_totals vendor_attribute_summary synthetic_conservation synthetic_vendor_coverage synthetic_validation analysis_packets coverage dimension_attributes reference_attributes synthetic_view_config'
awk -v expected="$expected" '
 BEGIN{n=split(expected,a," ");for(i=1;i<=n;i++)wanted[a[i]]=1}
 $1=="PASS"&&$NF=="exact_record_multiset=true"{
  view=$2;sub(/\.csv(\.gz)?$/,"",view);if(!wanted[view])exit 1
  row=$3;sub(/^rows=/,"",row);if(row!~/^[0-9]+$/)exit 1
  if((view in seen)&&seen[view]!=row)exit 1;seen[view]=row
 }
 END{for(i=1;i<=n;i++)if(!(a[i] in seen))exit 1}
' "$run/other_views_audit_attempt1.log" "$run/relationship_precision_candidate/exact_audit.log" "$run/remaining_views_audit.log"
cmp "$baseline/report/input_manifest.csv" "$run/report/input_manifest.csv"
cmp "$baseline/census/report_profile.csv" "$run/census/report_profile.csv"
shasum -a 256 --quiet -c "$run/reporting_classes_sha256.txt"
shasum -a 256 --quiet -c "$run/relationship_precision_candidate/source_sha256.txt"
awk -F '\t' -v prefix="$baseline/report/" 'index($1,prefix)==1{if($10!="VERIFIED")exit 1;printf "%s  %s\n",$6,$4;n++}END{if(n!=7)exit 1}' "$repo/session-run.tmp/phase2_baseline_archive_v1/manifest.tsv" | shasum -a 256 --quiet -c -
while IFS=$'\t' read -r relative bytes original_sha;do
  case "$relative" in report/*) [[ ! -f "$baseline/$relative" ]] || printf '%s  %s\n' "$original_sha" "$baseline/$relative";; esac
done < "$repo/data/test_evidence/phase2_derived_baseline_20261116/zenodo_helper/source_inventory.tsv" | shasum -a 256 --quiet -c -
corrected_sha=$(awk -F, '$1=="output_sha256"{print $2}' "$run/relationship_precision_candidate/receipt.csv")
printf '%s  %s\n' "$corrected_sha" "$run/report/relationships.csv" | shasum -a 256 --quiet -c -
rg -q '^PASS reconciliation.csv rows=19943155$' "$run/logs/report_checks.log"
rg -q '^PASS synthetic_conservation.csv rows=3163680$' "$run/logs/report_checks.log"
rg -q '^PASS financial_rows=286277734 vendors=98865 attributed_input_rows=131935804 unattributed_input_rows=154341930$' "$run/logs/report_checks.log"
[[ "$mode" == finalize ]] || { echo 'PASS acceptance gates; finalize not requested';exit 0; }
timing(){ awk '$2=="real"{v=$1}END{if(v!~/^[0-9]+([.][0-9]+)?$/||v<=0)exit 1;print v}' "$1"; }
prep=$(timing "$run/logs/vendor_range_prepare.log")
wave=$(awk -F, '$1=="workers_wall_seconds"{v=$2}END{if(v!~/^[0-9]+$/||v<=0)exit 1;print v}' "$run/wave_metrics.csv")
combine=$(timing "$run/logs/report_combine.log")
repair=$(timing "$run/relationship_precision_candidate/repair.log")
be=$(timing "$baseline/report/extract.log");br=$(timing "$baseline/report/reduce.log")
parent="$repo/data/test_evidence/reporting_three_way_20261117"
[[ ! -e "$parent/completion" ]]
mkdir -p "$parent"
staging=$(mktemp -d "$parent/.completion.XXXXXX")
mkdir "$staging/workers"
for name in run_config.txt vendor_ranges.csv reporting_classes_sha256.txt wave_metrics.csv;do cp "$run/$name" "$staging/";done
for name in vendor_range_prepare report_combine report_checks census census_precision_recheck;do cp "$run/logs/$name.log" "$staging/$name.txt";done
cp "$run/other_views_audit_attempt1.log" "$staging/exact_first_two.txt"
cp "$run/remaining_views_audit.log" "$staging/exact_remaining18.txt"
cp "$run/relationship_precision_candidate/exact_audit.log" "$staging/exact_relationships.txt"
cp "$run/relationship_precision_candidate/receipt.csv" "$run/relationship_precision_candidate/promotion.csv" "$staging/"
cp "$run/census/report_profile.csv" "$staging/report_profile.csv"
cp "$run/census/input_manifest.csv" "$staging/report_hashes.csv"
cp "$run/report/input_manifest.csv" "$staging/input_manifest.csv"
cp "$repo/data/test_evidence/reporting_three_way_20261117/precision/tests.txt" "$staging/precision_tests.txt"
cp "$run/FAILED.initial_census" "$staging/initial_census_rejection.txt"
cp "$run/census_before_precision/report_profile.csv" "$staging/original_census_profile.csv"
cp "$run/relationship_precision_candidate/source_sha256.txt" "$staging/correction_source_sha256.txt"
for worker in 0 1 2;do
  mkdir "$staging/workers/$worker"
  for name in extract.log reduce.log extract_metrics.csv reduce_metrics.csv synthetic_metrics.csv synthetic_stream_metrics.csv;do
    [[ ! -f "$run/workers/$worker/$name" ]] || cp "$run/workers/$worker/$name" "$staging/workers/$worker/${name%.log}$( [[ "$name" == *.log ]] && echo .txt || true )"
  done
done
{
 printf '# Accepted three-worker reporting result\n\n'
 printf 'Accepted at %s after a separately documented precision correction.\n\n' "$(date -u '+%Y-%m-%dT%H:%M:%SZ')"
 printf 'All 21 declared outputs passed strict full-record multiset comparison, including duplicates. Exact input receipts, original frozen classes, financial/synthetic controls and all seven census profiles pass. Financial input: 286,277,734 rows in 28 partitions; 98,865 vendors.\n\n'
 printf '| Reporting scope | Seconds |\n|---|---:|\n'
 printf '| Vendor range preparation | %s |\n| Three workers, extraction and local reduction wall time | %s |\n| Final assembly/global collapse | %s |\n| Report-only precision correction | %s |\n\n' "$prep" "$wave" "$combine" "$repair"
 awk -v p="$prep" -v w="$wave" -v c="$combine" -v r="$repair" -v be="$be" -v br="$br" 'BEGIN{printf "Observed preparation + worker wave + combination + correction: %.2f seconds (%.2f minutes), versus %.2f seconds (%.2f minutes) baseline extract/reduce; engineering ratio %.3fx.\n\n",p+w+c+r,(p+w+c+r)/60,be+br,(be+br)/60,(be+br)/(p+w+c+r)}'
 printf 'Checks, census, regression tests and full-record equivalence audits are separate verification costs, retained in their logs. Worker wall timing has one-second resolution. Individual JVM elapsed/RSS and worker spill counters remain separately recorded.\n\n'
 printf 'Precision exception: original e788d67 output used a shorter MathContext for 303 relationship shares. A bounded Scala report-only correction (two input scans plus output hash/count verification) matches the archived baseline exactly. The original report and initial census rejection remain preserved. This is not a claim that uncorrected e788d67 is fully equivalent. Isolated fix 9bc12a3 passes 20 reporting/precision tests; its future fused runtime has not been remeasured.\n\n'
 printf 'Limits: one observational engineering pair, designated mid-baseline, not replicated paper evidence. Three 2GiB workers, compressed intermediates, direct Java launch and extra range preparation differ from the serial profile; improvement cannot be attributed to one factor. Accepted financial preparation/original sorts were reused; no sort-reuse saving is claimed. Thermal/cache conditions are not isolated. Combiner accumulator spill-byte counters were not emitted by the frozen build; do not infer zero. Target GenevaERS capability gaps and allocation/basis feedback remain open. No upload or publication.\n'
} > "$staging/ACCEPTANCE.md"
(cd "$staging" && find . -type f ! -name SHA256SUMS -exec shasum -a 256 {} + | LC_ALL=C sort > SHA256SUMS && shasum -a 256 --quiet -c SHA256SUMS)
mv "$staging" "$parent/completion"
cp "$parent/completion/ACCEPTANCE.md" "$run/COMPARISON.md"
date -u '+%Y-%m-%dT%H:%M:%SZ' > "$run/EXACT_OUTPUT_EQUIVALENCE_COMPLETE"
cp "$run/EXACT_OUTPUT_EQUIVALENCE_COMPLETE" "$run/RESULT_REVIEW_COMPLETE"
cp "$run/EXACT_OUTPUT_EQUIVALENCE_COMPLETE" "$run/COMPARISON_RECORD_COMPLETE"
touch "$run/stages/result_review.complete"
printf 'accepted_with_documented_precision_correction\n' > "$run/STATUS"
printf 'accepted_with_documented_precision_correction\n' > "$run/WORKFLOW_COMPLETE"
echo "PASS accepted evidence: $parent/completion"
stamp=$(TZ=America/Phoenix date '+%Y-%m-%d %H:%M:%S AZT (UTC−07:00)')
owned=(data/test_evidence/reporting_three_way_20261117/completion)
# Only update clean tracked pages; leave concurrent owner edits untouched.
prefix_clean() {
  local file="$1" entry="$2" before after temporary first
  git -C "$repo" diff --quiet -- "$file" && git -C "$repo" diff --cached --quiet -- "$file" || return 0
  before=$(shasum -a 256 "$repo/$file" | awk '{print $1}')
  temporary=$(mktemp "$repo/$file.acceptance.XXXXXX")
  IFS= read -r first < "$repo/$file"
  { printf '%s\n\n%s\n' "$first" "$entry";tail -n +2 "$repo/$file"; } > "$temporary"
  after=$(shasum -a 256 "$repo/$file" | awk '{print $1}')
  if [[ "$before" == "$after" ]];then mv "$temporary" "$repo/$file";owned+=("$file");else rm "$temporary";fi
}
entry="**$stamp — Three-worker reporting accepted with documented precision correction.** Codex automated gate: all21 full-record comparisons, original inputs/classes, controls and seven census profiles pass. Worker wave80m37s; preparation740.76s; combination358.55s; report-only correction161.66s. Original failed census and303-share defect preserved. Accepted evidence: data/test_evidence/reporting_three_way_20261117/completion/ACCEPTANCE.md. This is an observational multi-factor engineering comparison; fixed-context future runtime is unmeasured. Allocation/basis feedback remains planned; upload deferred."
prefix_clean STATUS.md "$entry"
prefix_clean tracks/D-experiment/STATUS.md "$entry"
prefix_clean docs/VERIFICATION_AUDIT_REGISTER.md "$entry"
study="**$stamp — Owner-directed three-worker follow-up concluded.** Codex automated gate, direct evidence: the accepted completion bundle at data/test_evidence/reporting_three_way_20261117/completion/ACCEPTANCE.md records all21 exact output proofs and scoped timings. The single-CPU CKB attempt was interrupted after43min and is not a completed comparator. The accepted three-worker profile changes CPU/memory, compression, launch and preparation; it also required a separately timed303-share precision correction. Row-count skew did not cause a large tail: worker extraction times4724.23/4828.22/4406.88s for154341930/72524043/59411761 financial rows. Decimal MathContext is part of the analytical contract; incidental spill serialization must not determine derived division precision. Twenty regression tests pass for isolated fix9bc12a3; its fused runtime is not measured. This does not isolate training/retrieval/gate causation, fulfill the original matched single-CPU protocol, establish a replicated paper point, close target-runtime gaps, or implement allocation/basis feedback."
prefix_clean docs/REPORTING_CKB_TRANSFER_STUDY.md "$study"
git -C "$repo" add -- "${owned[@]}"
git -C "$repo" commit --only -m 'Accept three-worker reporting evidence with documented precision correction' -- "${owned[@]}"
