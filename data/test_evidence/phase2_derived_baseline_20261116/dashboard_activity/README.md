# Live extraction activity display

2026-11-17. Initial dashboard-only change: active extraction shows its current bucket
spill-file count in the existing19-character status cell. State remains RUN;
no column/row added. Count is temporary files, not cumulative completed work:
it can fall when spill files are consolidated or a group is retired.
The current version's broader coverage is recorded in the correction below.

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

## Coverage correction — current version

The first counter covered only bucket spills. During SyntheticReporting.finishVendor,
bucket spills had been consumed while synthetic/master partial directories were
actively growing; a zero bucket count did not describe those active files.
The current version counts CSV files in all named `*_partials` directories plus
legacy root `spill_*.csv`, during active extraction or reduction. It is still a
current temporary-file count, not cumulative processed work or acceptance.
The earlier bucket-only observations above remain historical snapshots.

Bash3.2 syntax and actual read-only rendering pass; row width remains82. Live
Terminal793 confirmed RUN1492spills (all_partials_live_row.txt); values change
with production/consolidation. Only the exactly verified display95648 was
refreshed, yielding dashboard97164 in the same Terminal/tty. Financial driver
66386/session84820 and extraction82547 remain unchanged in group66386; frozen
class checksum verification passes. No engine compile/replay/configuration change.

A single read-only thread sample identifies SyntheticReporting.finishVendor
fold/output work (output_phase_stack_excerpt.txt). It is phase evidence, not a
bottleneck or speed claim. At11:36:41Z synthetic_cut_partials occupied1,693,044KiB,
synthetic_raw_partials1,164,072KiB and master_cut_partials469,364KiB. Partial-file
growth explains progress while the three main history/reconciliation files pause.
At11:38:36Z the same JVM accumulated78m21s CPU with RSS767,780KiB; free space
120,989,960KiB in the latest monitor sample. Final report acceptance still pending.
