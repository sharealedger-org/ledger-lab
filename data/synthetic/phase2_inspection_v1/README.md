# Fictional Phase 2 inspection package v1

Historical design sample, retained unchanged. The resumed implementation uses
opaque stable identities, SY dates and two generation classes under the current
[execution plan](../../../docs/PHASE_TWO_EXECUTION_PLAN.md). This sample predates
those decisions; its identifiers/calendar are not the accepted full-corpus format.
Actual generated sample and control receipts are tracked in
[Phase 2 evidence](../../test_evidence/phase2_generation_20261116/README.md).

Hand-authored 2026-11-16 from checkpoint `38a41c6`; all names and activity fictional.
This is the first inspection slice of the [corpus specification](../../../docs/PHASE_TWO_CORPUS_SPEC_V1.md),
with [calibration map](../../../docs/PHASE_TWO_CALIBRATION_MAP_V1.md). It is
self-contained for inspection, not the full-scale generated corpus, a financial
engine input accepted by existing parsers, a model evaluation, or a blind test.
No detailed AIM model or feature design is implied.

## Inspect in this order

1. Read `evidence/parties.csv`, `proposals.csv`, `openings.csv` and only records
   available by **2032-02-01T23:59:59Z**. This is the historical decision view.
2. Inspect `scenarios.csv` and `references.csv`: the temporary future hierarchy
   applies from April only inside its child scenario. Its existence is known in
   February; its future classification is not accepted baseline state.
3. Advance availability cutoff to **2032-06-30T23:59:59Z**. The March delivery and
   March staff change now become visible. B104 replaces B103 for the interpreted
   invoice amount; it does not delete the original record. Effective date alone
   must not admit either late record into the February view.
4. Read `private/` separately to inspect intended connections and realized
   observations. A manual reviewer now knows truth: do not claim blinding.
5. Compare adding `modernization.csv` as the integrated profile. Partial
   modernization uses only its rows available from April 1 onward; disconnected
   uses none. All profiles retain the same underlying records/outcomes.

`private/` is a directory convention, not enforced access control. Never give the
whole package to a historical model. The eventual exporter must use an explicit
family/column allowlist and availability cutoff; generating hashes is not leakage
protection. `modernization.csv` is excluded from the disconnected profile. A
partial-modernization profile does not retroactively reveal earlier links.

## Expected inspection controls

| Family | Data rows | Check |
|---|---:|---|
| parties | 7 | Local parties have distinct process roles; B601 remains missing |
| records | 15 | Unique record IDs; local/supersession references exist |
| proposals | 3 | Three distinct tentative outputs |
| references | 2 | Baseline and isolated temporary version |
| scenarios | 2 | One baseline and one child; no assigned probability |
| openings | 1 | Fictional fund/USD only |
| modernization | 5 | Optional observable correlation fields; no truth context ID |
| private relationships | 5 | Positive, negative and unsupported evidence distinctions |
| private outcomes | 5 | Partial, observed, canceled and unobserved measures |

All monetary records belong to the fictional fund in openings; `scope` in proposals
is an analytical component scope, not a distinct fund. Opening is cash, not a
complete opening ledger. Expectations, commitments, invoice versions and cash
movements are different measures and cannot be summed together. P41's expected
cost is 1,000,000 cents; corrected invoice is 650,000; two payments total 650,000.
Its expected receipt is 1,200,000; observed receipt to cutoff is 900,000. That receipt
belongs to P41 only in private truth in the disconnected profile: an evidence-only
consumer must not assert the association as established. B601 is independent
50,000-cent cash expense; R303 is independent 100,000-cent summary receipt.
No posting or financial control-total run has occurred on these records.

P52 is canceled, not an observed zero completion date. P63 is unobserved at the
cutoff, not a failure or zero outcome. P41 delivery quantity is 60 service units;
full planned quantity is not specified, so no completion percentage is defensible.
The sample cannot estimate default probabilities or validate rare-event risk.

## Capability / evidence matrix

| Condition | Concrete records | What inspection establishes | Still absent |
|---|---|---|---|
| Disconnected component | B101 / H201 / R301 | Private connection lacks observable common key | Executable bridge/inference and accuracy evidence |
| Misleading overlap | B17 / H9 / F81 share Suite 4 | Location alone cannot identify the activity | Independent mechanism populations at scale |
| Similar vendor names | B17 / B28; ORDER77 / ORDER88 | Different obligations despite lexical overlap | Automated identity resolution |
| Later outcome/correction | B102–B106 and P41 | Effective/availability split; replacement versus payment | Reconciliation consumer and balanced SJE lineage |
| Changed responsibility | H201 / H202 | Late evidence of new responsibility | Explicit end interval for old responsibility; do not assume one |
| Cancellation / censoring | F402 / P52; P63 / U5 | Distinct observation statuses | Statistical scoring policy |
| Source modernization | Optional LINK83 rows | Same truth with different evidence availability | Enforced paired-profile exporter |
| Conditional future recast | ORG1 version 2 | Child reference version with future effect | Scenario calculation engine or accounting reclass |
| Cash / timing inquiry | Opening, expected receipts, payments | Compatible units and explicit dates | Complete ledger, due-date schedule and forecasts |
| Shared dependencies / regime risk | Specification only | Required future data family identified | Scaled mechanisms and held-out families |

This slice intentionally leaves disputed matching, component splits/merges,
explicit due dates, rare correlated shocks and payroll amounts to the next fixture
expansion; it does not represent every required full-corpus condition. Include
these before calling full-package coverage complete. Employee responsibility is
not payroll and a revenue party is not a vendor.

## Reproduction and next gate

Files are static review fixtures; no PRNG or generator produced them. `SHA256SUMS`
records exact CSV bytes. `inspection_checks.txt` records structural checks only.
No Scala production code or Bash financial logic changed. Next useful work is
review of these contracts, followed by actual producer/consumer tracing and a
Scala adapter/generator fixture under the architecture gates. Full generation
requires its own manifest, seed/registry, calendar and measured admission. Paper
1/2 work on ARM and deferred reporting repeats remain separate. Q2 is unopened.
