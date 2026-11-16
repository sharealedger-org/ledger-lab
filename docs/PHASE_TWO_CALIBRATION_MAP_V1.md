# Phase 2 calibration map v1

2026-11-16; calibration preparation, not a fitted generator. The self-contained
fictional population replaces the mixed companion proposal for this workload.
Sources are retained at [reporting receipt](../data/test_evidence/report_sorted_v3_20261116/README.md),
its `sort_manifest.csv`, `extract_metrics.csv` and `report_profile.csv`.
Calibration uses completed reporting aggregates without a fresh reporting repeat.
The separately declared reused-activity path later scans retained canonical
transactions read-only; it is selection/provenance, not statistical fitting.

| Empirical anchor | Exact meaning | Generation use | Limit / acceptance requirement |
|---|---|---|---|
| 273,380,488 rows; 94,710,620,118 bytes | 28 sorted journal/ledger partitions FY03–FY16 | Reference workload envelope for later scaling | Not independent transactions; new source-to-SJE/ledger expansion must be measured |
| 261,529,946 journal + 11,850,542 ledger rows | Distinct input families in sort manifest | Keep movement/history and state rows separately counted | Never synthesize that many source events merely to match summed reporting rows |
| 65,716 reporting groups | Extract grouping on this assignment policy | Key-cardinality sensitivity profile | Not verified number of vendors or desired fictional entity count |
| 217,406,124-row largest group | Observed hot reporting group | Preserve a hot-key stress profile separately from ordinary fictional linkage profile | Do not force normal economic identity rate to match this group |
| 52,471,280 attributed / 220,909,208 unattributed financial rows | Connected checker classification at financial-row grain | Baseline stress comparison, not fictional missing-party inference | Unattributed includes unsupported/summary grain; not 220.9M unknown vendors |
| 11,835,781 movement-history and same balance-history rows | Reduced output census | Forecast output/materialization cost using measured new reduction ratio | Not an economic frequency or independent population |
| 71,119,226 temporal-cut; 12,985,882 attribute-total rows | Expanded analytical outputs | Plan temporal/reference cardinality sensitivity | New histories and profiles may change expansion |
| 20 history oracles; 2,102,912 conservation rows | Passed validation scope | Retain baseline controls and add new-family controls | Not proof of lifecycle/outcome consumer correctness |
| 1,400,717,312 bytes peak RSS; bounded spill | Actual extract process footprint | Initial resource planning precedent | Not a memory guarantee for generator or new adapters |

## Parameters deliberately not fitted

Amount quantiles, source-event seasonality, delivery/payment delays, employee
counts, payer counts, default rates, dependence, and component memberships are
not supplied by this aggregate receipt. Their calibration status is `unknown`.
No empirical risk probability, financial envelope or real actor connection is
invented from row counts. The small sample uses declared illustrative amounts and
dates, not estimated VA economic behavior. Later bounded Scala summaries may
supply compatible financial distributions if needed, without rerunning identical
reporting merely for repetition. Record source grain and denominator for each fit.

## Profiles and target selection

1. `inspection-v1`: explicit records and controls in the sample; no statistical
   representativeness. Useful for schema and temporal review.
2. `economic-v1` (proposed): most fictional parties resolved; deliberate missing,
   summary-grain and ambiguous cases have separately counted quotas. Proposed
   5% missing-party event quota is an invented source-quality stress setting,
   not an empirical estimate; freeze its denominator before generation.
3. `hot-key-v1` (proposed): reproduce the reporting hot-group/total ratio at the
   financial-row grain after measuring adapter expansion. Keep all summary-grain
   rows summary-grain and never fabricate IDs to reduce the group.
4. `source-quality-v1` (proposed): disconnected, prospective partial modernization,
   and integrated profiles over identical underlying events; paired counts, money
   and outcomes invariant. Only allowed evidence fields/availability differ.

Do not merge these profiles into one assertion of realism. For each scale, record
achieved versus target rows, bytes, keys, largest key, temporal versions and
unresolved reasons; explain deviations. Suggested workload-ratio tolerance is ±5%
for large admitted stress profiles, not for economic parameters lacking evidence.
Money uses exact cents and per-scope controls; no percentage tolerance can excuse
imbalanced journal groups. The tiny inspection sample is exempt from ratio fitting.

## Monetary envelopes and bridge

Opening cash 2,000,000 cents and all sample amounts are fictional assumptions.
They are not a FY2012 General Fund tie-out. The earlier verified FY2012 anchors
remain separate scoped evidence; any later anchor calibration must enumerate
fund/basis/currency, anchor IDs, signed supported bridge items, explicit synthetic
share and unresolved remainder before generation. No residual is automatically a
journal, and no observed amount is added to overlapping synthetic activity.

Freeze full-scale seed, horizon, mechanism frequencies, envelopes, counts and
resource admission only after sample inspection and adapter controls. Archive
aggregate calibration provenance/hashes in the eventual self-contained package;
release of VA inputs, local scratch or a fitted model is not required. Zenodo
publication and a Q2 evaluation protocol remain separate future actions.

## Annual monetary calibration added after bounded reporting inspection

The initial sample above remains illustrative. Full-generation preparation now
uses `data/synthetic/phase2_generation_v1/reported_annual_calibration.csv`:
28 EXP/REV annual envelopes from the completed `cross_vendor_totals.csv`.
`PhaseTwoCalibration` read 1,556,150 existing summary rows, selected 769,052
movement/nominal-account groups, checked total movement balance by currency and
retained only FY2003–16 USD EXP*/REV* net movements. Balance rows, offsets,
commitments and other accounts are excluded from these envelopes. This was a
bounded Scala summary of existing results, not a financial/reporting replay.
Source SHA-256: `cbeae50bc0715da7bab82dbc707136e12027c5d43dbbce7b1783b70423f5015e`.

The invented scale share is 1%. Source FY2003–16 maps to fictional FY2031–44.
Aggregate target cents = ROUND_HALF_UP(source net USD × 0.01 × 100).
The sub-cent residual is recorded separately. Component shares use integer division
and deterministic one-cent remainders; twelve monthly slots use an exact final-slot
remainder. No residual creates a journal. Per-component dates, connections and
within-year distributions remain invented; source annual nominal-row counts are
recorded but not reproduced by the constant monthly event recipe.

For example, reported FY2003 expense net USD 27,920,860,367.07 calibrates fictional
FY2031 expense to 27,920,860,367 cents (USD 279,208,603.67); the scaled rounding
residual is USD 0.0007. Reported receipts calibrate to USD 263,241,465.98.
This is accepted-report movement calibration across all agencies/funds, not an
ACFR tie-out, a recovered economic allocation, gross payment-size distribution or
General Fund statement. The fictional aggregate fund has its own declared scope.
Observed inputs remain immutable and are not combined with these synthetic amounts.

When `annual_calibration.csv` is present it is authoritative for EXP/REV; config
expense/receipt amounts are fixture fallbacks only. PO commitments remain separate
invented envelopes. Opening cash is an assumed quarter of the first expense annual
envelope in calibrated profiles, not a calibrated real opening balance. Preserve
that distinction when interpreting cash pressure. Source and SJE controls must
reconcile every annual envelope before full acceptance.
