# Expanded reporting pass — design and fixture contract

2026-11-13. Owner authorized design, construction and fixture testing. Exploratory reporting, outside the three Phase 1-comparable corpus measurements. Existing `5v` remains the legacy profile; `5v --reportProfile expanded` selects this reporting topology, followed by `5r` spill reduction. No accounting entries are generated or applied.

## Falsifiable claim and execution contract

**Revised 2026-11-14 after corpus falsification.** See
[identity recovery v1](IDENTITY_RECOVERY_V1_DESIGN.md). Reporting prepares a
bounded byte-range index of each immutable input, then reads its blocks in literal
Instrument-ID order. This explicitly adds a preparation scan; financial files and
posting order stay unchanged. Original byte hashes and physical record ordinals
are retained. Arbitrary literal order and vendor re-entry are supported within
`maxIndexedBlocks` (100,000 blocks per file by default). A record has a 1 MiB
byte limit by default. Input files use the canonical single-physical-line record
contract; malformed records and input mutation fail publication. Inputs are pinned
to physical generations. Preparation time, block counts and index bytes are measured.

Ownership: 05 perspectives over committed 02/03 outputs. Current-vendor posting
buckets, relationships and cross-cut contributions use `aggregateKeys` bounded
partial buffers and `mergeFanIn` bounded external merges, released at each vendor
break. Raw vendor rows are only a metric: the old `maxVendorRows` option is rejected.
Feature distinct sets, year/event maps and dimension versions have the explicit
`maxFeatureKeys` allowance (250,000 by default); exceeding it fails publication.
This supports large row groups without claiming unlimited feature cardinality.
References have an enforced aggregate byte allowance. No completed-vendor
population set or complete financial history is retained in memory.

Vendor/IP dimensions use `instID`, optional `instHolderName`, `instEffectDate`,
`instEffectEndDate`; contract dimensions explicitly use `vendor_id,contract_id`,
optional `effective_from,effective_to`. Dimensions use the same literal-ID index.
No identifier padding/stripping or inferred PO/payment bridge is added. Missing,
overlapping and undated matches remain explicit. Zero IDs, `UNMATCHED_` IDs and
known PO placeholder `999999999999` are unattributed. Source currency is dimension
index 9 (SJE field 18 / ledger field 14), rather than currency type at index 10;
regression tests now enforce this and reject mixed source currencies.
