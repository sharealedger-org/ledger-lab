# Verified lossless inactive-attempt archival

2026-11-17. Owner explicitly approved: **“Approve verified lossless archival”**
in response to the exact89-file proposal retained as `approved_relative.tsv`.
This superseded the earlier automatic approval rejection. No additional scope
was inferred. Successful financial/report outputs, raw inputs, controls and logs
were excluded. All four source roots were failed/stopped, with no open files at
admission (see retained empty open-file/warning receipts).

`manifest.tsv` records89 VERIFIED entries:40,585,489,959 original bytes,
2,467,501,061 compressed bytes,38,117,988,898 logical bytes saved. Every archive
was decoded and SHA-256 compared before unlinking its original. `events.tsv`
records completion for all89. The executed helper matches repository
`scripts/archive_inactive_phase2_attempts.sh` SHA-256
`bceff699edf22a006912c1ddcdbd165466386ff9b3887d9ac6fa11711668adcc`.
Its selftest passes exact recovery and existing-archive collision rejection.

Original and archive hashes, byte counts, file identity/mode/mtime, before/after
storage and exact selected paths are retained. Concurrent reporting writes mean
filesystem free-space differences are not a direct measure of archive savings.
Archives remain beside their original paths with `.gz` appended. Restore only
to an absent destination and compare the decoded SHA against the original hash;
never overwrite another attempt's file. This is storage recovery, not financial
result acceptance.
