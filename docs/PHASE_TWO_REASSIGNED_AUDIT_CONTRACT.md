# Post-execution reassigned-activity audit

2026-11-16. Read-only verification completed after the frozen full workflow ended.
All 49,091 original/public/actual classifications match; 5,744 invented suppliers,
2,966 multi-year and 2,670 multi-context. Two focused adversarial tests pass.
See data/test_evidence/phase2_generation_20261116/full_v6/post_audit/.
This is not another financial execution, model evaluation,
engine or perspective. Scala owns bounded inspection; Bash invokes it.

Claim: every reused source expense preserves its original agency/fund/object,
signed cents and declared calendar-translated date in public source evidence;
its actual nominal debit SJE preserves that classification. Invented suppliers
recur across synthetic years and AFO contexts without implying real-party identity.
Select the nominal/direct D records using the canonical `DirVsOffsetFlg` contract;
the audit does not reinterpret that field as a general debit/credit accounting model.

Actual producers/consumers reconstructed: selected.csv -> ReassignedRouting ->
registry/entity merge -> financial_events.csv/source_lineage.csv -> unchanged
APA/ARE -> SortedJEFYxxmerged.csv. Existing exact financial checker already
verifies two balanced sides, amounts, dates, posting periods, party, document and
rule/version; it does not explicitly assert all reused classification columns.

Cheapest falsifier passed: event N3c5b4ae41a65123f6ffa6b0e63f8b76d has public
agency 0, fund 10828, object 370 and actual FY31 nominal debit fields 0/10828/370,
75.00 USD, 08-28-30 and 0031/08. Its source-use row records original FY2003
expense 7,500 cents. A separate literal supplier lookup demonstrates the same
opaque supplier in multiple original years and contexts. These are spot checks,
not full closure; retain a full bounded audit receipt before claiming closure.

Inputs are immutable full inputs and actual completed outputs. Join original
selection to public reused events in event-ID order using an external sort.
Financial events and source_lineage are emitted in matching order; verify their
event IDs and bridge cents/date/party explicitly while streaming. Sort expected
reused classifications and actual nominal debit classifications by source locator,
then compare the selected subset in a bounded merge. Check all expected records,
duplicate keys, counts and exact values; preserve negative expense signs.

Avoid sorting every actual debit merely to select reused actors. The generated
registry is actor ordered and each completed financial partition is vendor ordered
(fixed-width opaque vendor IDs remove the old numeric-prefix ambiguity). Reopen
the small registry per FY, filter its R-prefixed source keys, assert monotonic
vendor order, and merge it with that FY's canonical stream at current-vendor grain.
Only reused expense debit classifications reach the source-locator resort. Assert
the expected debit/offset counts and distinct actor policy; no whole-party map.

For supplier recurrence, sort reused public records by vendor/AFO/year/event.
Retain one vendor, previous AFO and at most fourteen year flags. Emit per-vendor
event/year/context counts plus compact summary controls. No population-wide party
map or all-history JVM load. Reuse CorpusExternalSort's row/byte bounds and fan-in;
record the extra validation scans/sorts separately from financial measurement.

Focused tests should falsify a changed nominal classification and verify a known
multi-year/multi-context fictional supplier. Use explicit Scala fixtures; no
Python, financial reimplementation, new recognition rule or runtime-gap closure.
