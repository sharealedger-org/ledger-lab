# Calculation Engines

Calculation engines operate only on SJE/Universal Journal structures. They read SJE partitions
and generate new balanced SJE partitions; they do not emit aggregated perspectives.

An engine may run as a CKB section and pipe generated SJEs into a later section. Generating a new
SJE partition does not by itself require a new physical pass.

Initial classes:

- FTP and funding cost.
- Credit reserves and impairment.
- Currency revaluation.
- Consolidation and eliminations.
- Allocation divisors and allocated transactions.

Each engine must declare whether it consumes only today's inputs or requires historical state,
identify its generated-event class, preserve Instrument ID lineage, emit balanced transactions,
and return outputs to the Foundation CKB apply-back path. Perspective aggregation belongs in
`05-perspectives`.
