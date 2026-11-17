# Live extraction activity display

2026-11-17. Dashboard-only change: active extraction shows its current bucket
spill-file count in the existing19-character status cell. State remains RUN;
no column/row added. Count is temporary files, not cumulative completed work:
it can fall when spill files are consolidated or a group is retired.

Bash3.2 syntax check passes. Actual read-only pre/post render has the same
82-character extraction row. The updated script was loaded in the same Terminal
window793, title Ledger Lab Corpus Dashboard, tty/dev/ttys005. Live Terminal
contents confirmed the new cell (live_row.txt). Its11-spill count is a capture,
not a fixed expected number. Previously a separate preview showed52, reflecting
ongoing consolidation in the actual extraction.

Old display66618 was verified to be exactly the dashboard command in a separate
process group before stopping it. Refreshed display95648 is live. Financial
driver66386/session84820 and extraction JVM82547 remained the same; their group
66386 is separate from the dashboard. Frozen reporting class checksum recheck
passes. No financial/configuration/class change, replay or compilation.

Movement/balance/reconciliation outputs have begun growing, while the financial
input manifest remains buffered. Do not require input offsets to advance during
spill reduction/output emission; compare output growth/CPU when appropriate.
No final report controls or result acceptance are claimed by this UI receipt.

At11:33:26Z a further live Terminal capture showed RUN0spills while the same
JVM accumulated CPU. Movement output grew33,226,752->272,678,912bytes, balance
33,783,808->277,274,624, reconciliation45,768,704->376,029,184 between observations.
The earlier exact observation time was not retained. This establishes actual
output progress during temporary-file consolidation, not final row/control
acceptance. No completion ETA is inferred from these file sizes.
