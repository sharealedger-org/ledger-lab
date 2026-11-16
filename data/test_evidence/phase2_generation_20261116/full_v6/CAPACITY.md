# Retention and Phase 3 capacity

Measured after the full workflow and read-only audit, 2026-11-16. KiB/GiB are
binary units. `du -sk` measures retained allocation; `df -Pk` measures current
filesystem free space. Cumulative bytes sorted are not peak disk occupancy.

| Measure | KiB | GiB |
|---|---:|---:|
| Full retained root, including audit | 29,286,776 | 27.93 |
| Immutable reusable inputs | 7,164,684 | 6.83 |
| Post-execution audit | 123,228 | 0.12 |
| Current filesystem free | 76,717,036 | 73.16 |
| Full-driver sampled peak root | 29,163,536 | 27.81 |
| Full-driver sampled minimum free | 76,836,564 | 73.28 |
| Additional comparable pass budget | 27,652,615 | 26.37 |
| Projected free after that budget | 49,064,421 | 46.79 |
| Required reserve | 41,943,040 | 40.00 |
| Remaining margin above reserve | 7,121,381 | 6.79 |

Additional-pass budget = ceil(1.25 × (retained root − reusable inputs)). It
conservatively includes the audit and other non-input materializations again,
while sharing the existing immutable input package. A comparable full
financial/report execution fits with the 40-GiB floor. Source generation need not
be repeated. This is resource admission for the current materialization profile,
not authorization or a size guarantee for undetermined new Phase 3 engines/models;
re-profile those additions when their contracts exist.

No offloading or old-corpus removal is needed for this budget. Original data and
all attempts remain local. Dropbox/OneDrive remain future options; their quota,
sync state and exact recovery have not been verified. If a later scope outgrows
this margin, prepare a specific checksum-verified archive/recovery plan before
requesting retained-corpus deletion. Do not infer Zenodo recovery of derived
financial results merely from recovery of original source files.
