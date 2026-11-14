# Expanded-source Gold snapshot — November 13–14, 2026

This versioned snapshot preserves the combined expenditure/revenue/PO workload separately from the original expense-only Gold files above it.

outputs/: the same 104 final-output files selected for the prior Gold layout, from monthly_c (FY2003–FY2016, monthly granularity). All three runs validated, and 46 compared LDGR/GL/PERM_TBL/view SHA-256 entries match across all three. PERM_SUMMARY, quarantine, timings and logs are preserved too, but the repeat comparison covered only those named 46 files.

runs/: all three original per-year prep/posting/view logs, markers, wall clocks, orchestrator console, validation verdicts and final hash lists.
queue/ and supervisor/: frozen selection/master, state receipts and supervisor/gate logs. launchctl submit restarted completed wrappers; directory guards prevented financial replay. Jobs were unloaded and reporting used a run-once plist. A final supervisor exit 1 caused by rejected reentry does not replace the three VALIDATED corpus states.

evidence/: compact committed run archives, source checksum/preflight receipts and reporting-failure receipt. Measured processing revision b10b480; repository evidence commit 17bd5e7. Existing corpus_totals.csv expenditure labels aggregate both TTL source families for type B; they are not expenditure-only source reconciliation.

reporting_failed/: failed expanded 5v attempt, not completed analytical output. It exceeded the existing 250,000-row guard for unattributed 0000000000. No 5r, completed report, model fit or guard bypass.

Not copied: raw source files, sorted/merged SJE partitions, duplicated independent financial-output trees for the other two runs. Full immutable originals remain on Intel in ckb_expanded_queue_20261113_ready. Do not infer a complete canonical-journal backup from this compact Gold snapshot.

MANIFEST.csv lists rel_path,bytes,sha256 for every copied file including this README, excluding the manifest itself. Each destination file was compared with its source by SHA-256 during copying. This verifies the local OneDrive-folder copy, not completed cloud upload. Keep this snapshot immutable.
