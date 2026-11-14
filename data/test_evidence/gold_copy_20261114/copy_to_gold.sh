#!/usr/bin/env bash
set -euo pipefail
export LC_ALL=C
repo=/Users/ktwitchell001/workspace/sharealedger/ledger-lab
queue=/Users/ktwitchell001/workspace/ckb_expanded_queue_20261113_ready
gold=/Users/ktwitchell001/Library/CloudStorage/OneDrive-Personal/LedgerLabOutputs-Gold/expanded-20261113
stage="$gold.incomplete"
[[ ! -e "$gold" && ! -e "$stage" ]] || { echo 'Destination already exists'; exit 1; }
mkdir "$stage"
printf 'rel_path,bytes,sha256\n' > "$stage/MANIFEST.csv"
copy_one() {
  local source="$1" relative="$2" source_hash destination_hash
  mkdir -p "$(dirname "$stage/$relative")"
  cp -p "$source" "$stage/$relative"
  source_hash="$(shasum -a 256 "$source")"; source_hash="${source_hash%% *}"
  destination_hash="$(shasum -a 256 "$stage/$relative")"; destination_hash="${destination_hash%% *}"
  [[ "$source_hash" == "$destination_hash" ]] || { echo "Copy mismatch: $relative"; exit 1; }
  printf '%s,%s,%s\n' "$relative" "$(stat -f '%z' "$source")" "$source_hash" >> "$stage/MANIFEST.csv"
}
copy_tree() {
  local source_dir="$1" relative_root="$2" file relative
  while IFS= read -r -d '' file; do
    relative="${file#"$source_dir/"}"
    copy_one "$file" "$relative_root/$relative"
  done < <(find "$source_dir" -type f -print0)
}
for year in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do
  for name in "LDGR20$year.csv" "GL20$year.csv" "PERM_TBL20$year.csv" "PERM_SUMMARY20$year.csv" "POQuarantineFY$year.csv" cost_surface.csv pipeline_results.log; do
    copy_one "$queue/monthly_c/output/FY$year/$name" "outputs/FY$year/$name"
  done
  echo "Verified final outputs FY$year"
done
for name in view_agency_year_commodity_mix.csv view_agency_year_payment_totals.csv view_monthly_order_volume.csv view_vendor_year_survival.csv cost_surface.csv pipeline_results.log; do
  copy_one "$queue/monthly_c/output/VIEWS/$name" "outputs/VIEWS/$name"
done
for run in monthly_a monthly_b monthly_c; do
  copy_tree "$queue/$run/logs" "runs/$run/logs"
  for name in console.log validation.txt final_sha256.txt; do copy_one "$queue/$run/$name" "runs/$run/$name"; done
  echo "Verified raw logs $run"
done
for name in config.txt plan.tsv status.csv vendor_master.csv vendor_master.sha256 reference_monthly_B.sha256; do
  copy_one "$queue/$name" "queue/$name"
done
for file in "$queue".*; do [[ ! -f "$file" ]] || copy_one "$file" "supervisor/$(basename "$file")"; done
copy_tree "$queue/expanded_reporting.attempt" reporting_failed
for tag in 20261113_155156 20261113_182856 20261113_210544; do
  copy_tree "$repo/data/test_evidence/corpus_runs/run_$tag" "evidence/run_$tag"
done
copy_tree "$repo/data/test_evidence/intel_preflight_20261113" evidence/preflight
copy_tree "$repo/data/test_evidence/reporting_corpus_20261114" evidence/reporting_failure
cat > /private/tmp/expanded-gold-README-20261114.txt <<'README'
# Expanded-source Gold snapshot — November 13–14, 2026

This versioned snapshot preserves the combined expenditure/revenue/PO workload separately from the original expense-only Gold files above it.

outputs/: the same 104 final-output files selected for the prior Gold layout, from monthly_c (FY2003–FY2016, monthly granularity). All three runs validated, and 46 compared LDGR/GL/PERM_TBL/view SHA-256 entries match across all three. PERM_SUMMARY, quarantine, timings and logs are preserved too, but the repeat comparison covered only those named 46 files.

runs/: all three original per-year prep/posting/view logs, markers, wall clocks, orchestrator console, validation verdicts and final hash lists.
queue/ and supervisor/: frozen selection/master, state receipts and supervisor/gate logs. launchctl submit restarted completed wrappers; directory guards prevented financial replay. Jobs were unloaded and reporting used a run-once plist. A final supervisor exit 1 caused by rejected reentry does not replace the three VALIDATED corpus states.

evidence/: compact committed run archives, source checksum/preflight receipts and reporting-failure receipt. Measured processing revision b10b480; repository evidence commit 17bd5e7. Existing corpus_totals.csv expenditure labels aggregate both TTL source families for type B; they are not expenditure-only source reconciliation.

reporting_failed/: failed expanded 5v attempt, not completed analytical output. It exceeded the existing 250,000-row guard for unattributed 0000000000. No 5r, completed report, model fit or guard bypass.

Not copied: raw source files, sorted/merged SJE partitions, duplicated independent financial-output trees for the other two runs. Full immutable originals remain on Intel in ckb_expanded_queue_20261113_ready. Do not infer a complete canonical-journal backup from this compact Gold snapshot.

MANIFEST.csv lists rel_path,bytes,sha256 for every copied file including this README, excluding the manifest itself. Each destination file was compared with its source by SHA-256 during copying. This verifies the local OneDrive-folder copy, not completed cloud upload. Keep this snapshot immutable.
README
copy_one /private/tmp/expanded-gold-README-20261114.txt README.md
mv "$stage" "$gold"
printf 'Verified files: %s\n' "$(($(wc -l < "$gold/MANIFEST.csv") - 1))"
du -sh "$gold"
shasum -a 256 "$gold/MANIFEST.csv"
