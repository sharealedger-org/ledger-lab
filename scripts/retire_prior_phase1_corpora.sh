#!/usr/bin/env bash
# Owner-authorized retirement of two prior derived corpora. Preserve raw data,
# complete report tables, preparation metadata and yearly financial controls.
set -euo pipefail
main() {
  local repo retained financial oldreport actual year source destination file
  repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
  retained="$repo/session-run.tmp/retained_phase1_evidence_v1"
  financial="$repo/session-run.tmp/synthetic_reference_corpus_v2_20261115"
  oldreport="$repo/session-run.tmp/report_sorted_v3_20261115"
  [[ ! -e "$retained" && -d "$financial" && ! -L "$financial" && -d "$oldreport" && ! -L "$oldreport" ]]
  [[ -s "$oldreport/reboot_attempt/corpus_report/REPORT_COMPLETE" ]]
  [[ -s "$repo/session-run.tmp/phase2_full_v6/CORPUS_EVALUATED" ]]
  [[ -s "$repo/session-run.tmp/phase2_full_v6/reassigned_audit/REASSIGNED_AUDIT_COMPLETE" ]]
  [[ -s "$repo/session-run.tmp/phase1_identity_upgrade_v1/registry.csv" || -d "$repo/session-run.tmp/phase1_identity_upgrade_v1" ]]
  [[ -d /Users/ktwitchell001/workspace/va_fresh_root/VARawFiles && -d /Users/ktwitchell001/workspace/va_fresh_root/VAPOData ]]
  mkdir -p "$retained/financial_controls" "$retained/metadata"
  printf 'operation,source,destination\n' > "$retained/retirement.csv"
  du -sk "$financial" "$oldreport" > "$retained/prior_allocated_kib.txt"
  df -Pk "$repo" > "$retained/free_before.txt"
  for year in 03 04 05 06 07 08 09 10 11 12 13 14 15 16; do
    source="$financial/monthly_a/output/FY$year"
    destination="$retained/financial_controls/FY$year"
    [[ -s "$source/metrics/run_manifest.csv" && -s "$source/GL20$year.csv" ]]
    mkdir "$destination"
    cp "$source/GL20$year.csv" "$source/PERM_SUMMARY20$year.csv" "$destination/"
    cp -R "$source/metrics" "$source/interpretation" "$destination/"
    cmp "$source/GL20$year.csv" "$destination/GL20$year.csv"
    cmp "$source/PERM_SUMMARY20$year.csv" "$destination/PERM_SUMMARY20$year.csv"
  done
  cp -R "$financial/monthly_a/preparation" "$retained/"
  cp -R "$financial/monthly_a/logs" "$retained/financial_logs"
  # Preserve operational traces without copying per-record financial partitions.
  for actual in "$financial" "$oldreport"; do
    while IFS= read -r source; do
      destination="$retained/metadata/${source#"$repo/session-run.tmp/"}"
      mkdir -p "$(dirname "$destination")"
      cp "$source" "$destination"
    done < <(rg --files --hidden --no-ignore "$actual" -g '*.log' -g '*.sh' -g '*.patch' -g '*.sha256' -g '*manifest*.csv' -g '*metrics*.csv' -g '*config*.txt' -g 'STATUS' -g 'status.csv' -g 'EVALUATED' -g '*COMPLETE')
  done
  # Same-filesystem rename preserves every completed report table exactly.
  mv "$oldreport/reboot_attempt/corpus_report" "$retained/historical_report"
  printf 'retain_report,%s,%s\n' "$oldreport/reboot_attempt/corpus_report" "$retained/historical_report" >> "$retained/retirement.csv"
  [[ -s "$retained/historical_report/REPORT_COMPLETE" && -s "$retained/preparation/PREPARATION_COMPLETE" ]]
  (cd "$retained" && find financial_controls preparation financial_logs metadata -type f -exec shasum -a 256 {} +) > "$retained/retained_controls.sha256"
  printf 'remove_derived,%s,\nremove_derived,%s,\n' "$financial" "$oldreport" >> "$retained/retirement.csv"
  rm -rf -- "$financial" "$oldreport"
  # Keep the established report-reading path usable by the independent paper work.
  mkdir -p "$oldreport/reboot_attempt"
  ln -s "$retained/historical_report" "$oldreport/reboot_attempt/corpus_report"
  printf 'restore_report_alias,%s,%s\n' "$oldreport/reboot_attempt/corpus_report" "$retained/historical_report" >> "$retained/retirement.csv"
  df -Pk "$repo" > "$retained/free_after.txt"
  printf 'owner_authorized_prior_derived_corpora_retirement\nraw_VA_and_v6_retained\n' > "$retained/RETIREMENT_COMPLETE"
}
main "$@"
