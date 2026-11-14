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
