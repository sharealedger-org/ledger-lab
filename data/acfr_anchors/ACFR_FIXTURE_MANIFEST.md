# ACFR FY2012 Anchor Fixture Manifest

Generated: 2026-11-14

## Source

Virginia Department of Accounts Annual Comprehensive Financial Report FY2012.
Downloaded to `data/acfr_anchors/` via curl from `doa.virginia.gov`.

## Files

| File | Contents | Status |
|---|---|---|
| `FY2012_K_Notes.pdf` | Full Notes to Financial Statements | Downloaded 2026-11-14 (803 KB) |
| `FY2012_O_Internal_Service_Funds.pdf` | Internal Service Funds | Downloaded 2026-11-14 (50 KB) |
| `FY2012_general_fund_anchors.csv` | 7 anchor figures from Section G and L | DERIVED/READ |
| `FY2012_general_fund_adjustments.csv` | 4 named adjustment categories | DERIVED/ESTIMATED |
| `FY2012_tieout_target.csv` | Tie-out target for Strand B validation | DERIVED |

## Accuracy note

The food stamp amount (~$1.4B) is ESTIMATED from survey notes (see
`docs/VA_AUDITED_AND_PUBLIC_DATA_SURVEY_2026-11-12.md` Section 2.2 Note 10).
The exact line-by-line breakdown of the $1,171,053K fund balance reconciliation
requires parsing Note K pages 174-177, which are in `FY2012_K_Notes.pdf`.
All derived totals (ADJ-BASIS-DIFF-EXP, ADJ-BASIS-DIFF-REV, ADJ-FUND-BAL-RECONCILE)
are arithmetic derivations from Section G published figures and are exact.

## Verified next-test inputs, 2026-11-15

Use `FY2012_general_fund_verified_anchors_v1.csv` and
`FY2012_general_fund_verified_bridge_v1.csv` for the proposed next test. Existing
older CSVs above remain historical artifacts, not inputs to a new verified bridge.

Verified directly against official tables, including rendered page inspection:

- Section G, printed page 50/PDF page 6: General Fund modified-accrual statement.
  https://www.doa.virginia.gov/reports/ACFReport/2012/G_Major_Governmental_Funds.pdf
  SHA-256 `2312e5a55c196b19571a3523755caf7f2c869adb808f0ac660b60eafde45b344`.
- Section L, printed page 174/PDF page 2: General Fund budget-basis **Actual** column.
- Section L, printed page 177/PDF page 5: six signed General Fund closing-fund-
  balance reconciliation items, from budget to modified accrual.
  https://www.doa.virginia.gov/reports/ACFReport/2012/L_Required_Supplemental.pdf
  SHA-256 `e578f601412446f195951429f386c3818581caff943436ebc71e6b338a339bee`.

Each input preserves year, fund, basis, measure, units, column scope, page and source
hash. Figures are thousands of USD; convert once at the generation boundary.
Downloaded PDFs/rendered tables are optional research evidence outside corpus
inputs; running the test must not require the URLs to remain available.

**Scope correction:** printed page 177 note 10 attaches the approximately $1.4B
food-stamp item to Federal Trust Fund appropriations. It is not a supported General
Fund adjustment. Exclude the older `ADJ-NONCASH-FOODSTAMPS` estimate from this test.
The older revenue-basis difference is an aggregate difference, not a tax-specific
breakdown. The six newly verified bridge rows explain closing fund balance; do not
reuse them as a decomposition of revenue or expenditure movement differences.
Do not add the total balance difference to its own six components.

Verification here establishes published statement values and their summary bridge,
not a reconciliation of the observed APA corpus or validation of synthetic entity
relationships. No posting adapter or accounting offsets for bridge journals are
approved merely by recording these values.
