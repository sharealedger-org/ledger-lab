Owner-authorized prior-run generated-data retirement, 2026-11-15

Scope: monthly_a, monthly_b, monthly_c under
/Users/ktwitchell001/workspace/ckb_expanded_queue_20261113_ready/.
Deleted only inventoried regular generated financial files under each output/:
SortedJE*.csv, LDGR*.csv, GL*.csv, PERM_TBL*.csv and view_*.csv. All physical
generations were inventoried; symlink directories were not traversed. Hard-linked
copies were counted separately in logical sizes but once in allocated space.

1,398 paths deleted. Candidate logical sum about 742.51 GiB; unique allocated
estimate about 379.58 GiB before unlink. See actual filesystem availability change
in space_recovered.txt and before/after disk receipts, rather than treating those
logical totals as recovered storage. Free space changed from 75 to 452 GiB.

Preserved: all 1,050 other regular files across these three run directories,
including logs, canonical metrics/controls, interpretation, quarantine evidence,
permutation summaries, cost surfaces, validation verdicts and final hash receipts.
Pre/post SHA-256 and size verification passed for every preserved file. Each run
now occupies about 21-22 MiB. Retirement markers identify the remaining directories
as historical evidence, not reusable financial outputs. No process had an open
file under the queue at cleanup preflight (lsof output and stderr were empty).

Original raw inputs, queue configuration/master, the failed reporting attempt,
Gold copy and earlier measured-baseline data outside these three directories were
not selected for deletion. Data-dependent reruns or canonical audit rescans now
require rebuilding those financial outputs. Existing hashes/controls describe
historical runs; they are not claims that deleted files remain available.

Files: candidate_files.tsv records the inventory; deleted_files.tsv records actual
unlinks. Each file was rechecked for allowed path, regular-file type, byte count,
device/inode and artifact class before any deletion. preserved_sha256.csv and
verification.txt record the retention check. No financial calculation or engine
code was changed; no new Python was written; no Control repository was accessed.
