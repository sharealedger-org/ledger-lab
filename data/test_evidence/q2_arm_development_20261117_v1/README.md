# ARM Q2 development starter — 2026-11-17 v1

Development/inspection package from the accepted earlier reporting run. It does
not depend on acceptance of the later parallel-run equivalence audit. No Q2 model
or evaluation protocol has been frozen or executed.

reports/aim_combined.csv is the main vendor/fiscal-year feature table; separate
movement/balance columns are already included. vendor_activity.csv adds event
activity. synthetic_vendor_coverage.csv provides coverage/attribution diagnostics.
The two domain summary tables and cross_vendor_totals.csv support controls and
small exploratory tests. Controls retain the accepted producer/input receipts.
Original absolute paths in receipts describe provenance, not ARM runtime paths.

Source archive is commit9bc12a3: three-way Scala reporting plus the tested decimal
context correction. Compile natively on ARM; no x86 binaries are included. The
source archive contains fixtures, contracts and tests. It does not include the
large runtime corpus. Files and full archives on the source machine are retained.

Use a local working copy outside OneDrive for active runs. Verify SHA256SUMS after
copy/extraction. This is a development corpus, not a blind held-out evaluation.
Identity/template/attribution diagnostics are not automatically model features.
Keep generator truth and evaluation-only labels separate; define cutoff-safe
features and mechanism/context holdouts before any Q2 evaluation.
