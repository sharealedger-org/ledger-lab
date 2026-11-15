# Identity and concentration audit — 2026-11-14

The audit completed. Additional identities are needed for a useful synthetic
counterparty population, but generating vendors alone will not make the current
expanded reporting process work. Correct the source parser and the reporting
order/bounded-state contracts before choosing a full generated population.

## Evidence and scope

Read-only Scala diagnostics over all 336 APA monthly files, the frozen measured
master, Bob's enriched master snapshot, and one successful FY2003–FY2016
population (`monthly_c`). No source, master, SJE, ledger or Gold file was changed.
The program generated no vendors and did not run a financial corpus pipeline.

All 14 accepted-source count checks match recorded TTL metrics. Independent
source-to-journal identity-class counts and journal/ledger row counts also match.
The scan accounts for 114,164,816 accepted APA records, 259,471,158 canonical
journal lines including POs, and 11,140,226 posted rows. These are row/identity
controls, not certification of accounting dimensions or dollar totals.

Compact evidence: `data/test_evidence/identity_audit_20261114/`.
Full diagnostic tables and input snapshots:
`/Users/ktwitchell001/workspace/identity_audit_20261114/`.
Financial inputs retain their existing measured-run identities.

## 1. Attribute enrichment did not expand identity coverage

Both masters contain 1,248 lookup names. Bob's attribute enrichment changed
**zero legacy name-to-ID lookups**, including zero lookup changes across the
accepted source rows. The actual snapshots have no detected ambiguous names or
plain-versus-quote-aware master-field discrepancies. The quoted-master collision
is a demonstrated fixture vulnerability, not an observed defect in these masters.

The frozen master already maps generic `JOURNAL ENTRY` to `0000099085` and
`AGENCY TRANSACTION VOUCHER` to `0000099001`. Such keys are not proof of a supplier
identity and can themselves become very large aggregation groups.

## 2. Fix source field parsing before building a larger registry

The current APA producer splits physical lines on commas. A comma inside a
quoted object/fund/name field can shift later fields, including VENDOR_NAME.
The audit reproduces current acceptance and compares that interpretation with
the existing CorpusProfile quote-aware physical-line parser.

| Expense-record comparison | Accepted source rows |
|---|---:|
| Different vendor-name field under quote-aware parsing | 23,021,103 |
| Currently unmatched, but a quote-aware existing-master lookup candidate exists | 1,040,212 |
| Existing legacy match absent under quote-aware interpretation | 6,139 |
| Same mapped ID | 7,603,515 |
| Still unattributed in the lookup comparison | 87,815,525 |
| Invalid quote-aware name observation | 51 |

The first row overlaps the following classifications; do not add it to them.
Many lookup candidates are bookkeeping labels, not newly identified suppliers.
Example: FY2008 records interpreted as `HIGHER EDUCATION INSTRUCTION` become
`Journal Entry` (14,041 rows in one aggregated audit example). A real-producer
fixture reproduces the same mechanism using a quoted object field: the program
label becomes the vendor lookup, while the debit/credit pair still balances.

These comparisons cover the already accepted legacy population. They do not
claim recovery of rejected records, full multiline/RFC validation, or a completed
financial parser correction. A future adapter change must validate headers,
field positions, amounts, reference dimensions, dates, rules and lineage.

## 3. Separate missing names from synthetic detail that does not exist

There are **3,794,594 distinct unmapped source-name candidates** after quote-aware
physical-line interpretation and the published exploratory exact-label review.
They account for 51,805,339 accepted expense rows. They are not a count of unique
legal vendors: spelling variants, people, residual generic labels and unresolved
identity relationships remain possible. This count is not a target for automatic
generation or a validated payment-to-PO bridge.

Large missing labels include PAYROLL (8,128,937 rows), JOURNAL VOUCHER (5,085,736),
STRAIGHT TIME (4,034,780), USAGES (3,534,100), and USAGE COST (2,695,265).
The review rules and separate numeric/punctuation category are published in the
evidence. These labels require declared subledger assumptions or residual
unattributed processing, not a claim that their real counterparties were recovered.

There are also clear named-source candidates: DOMINION VIRGINIA POWER occurs in
750,783 accepted records under the current interpretation (757,255 under the
quote-aware interpretation). One stable ID for that name would already produce
more than 1.5 million journal rows before posted rows are added. More vendors
cannot guarantee compliance with a 250,000-row per-vendor guard.

## 4. Reporting needs a bounded large-group contract independently

Across the 65,940 literal instrument keys, **41 groups** exceed the configured
250,000-input-row reporting guard. The failed invocation supplied financial
streams only; dimension rows are therefore excluded from these counts.

| Literal key / description | Journal + posted input rows |
|---|---:|
| `0000000000`, primary unattributed key | 185,296,098 |
| `0000099001`, AGENCY TRANSACTION VOUCHER | 10,129,151 |
| `999999999999`, known PO placeholder | 3,503,026 |
| `0000099085`, JOURNAL ENTRY | 2,067,835 |

There is also a nine-zero literal key with 58 input rows; all-zero keys together
contain 185,296,156 rows. The 40 oversized non-primary-zero groups include the
known PO placeholder; do not describe every nonzero key as a genuine vendor.

Use streaming state where possible and explicit spill/reduction for oversized
bucket/relationship/distinct state. Retain oversized valid-party and unresolved
stress cases. Merely raising the row guard, dropping rows or inventing arbitrary
party shards is not an accepted recovery.

## 5. Literal key encoding and order are separate problems

The canonical scan records **214 literal vendor-order decreases** across the
28 financial files, e.g. `c12464` followed by `c1246`. Existing sorting concatenates
posting-key components; expanded reporting expects monotonic literal vendor keys.
No vendor/year/side group re-entry was observed, so this finding alone does not
prove incorrect posting or repeated legacy-view vendor groups.

Keys include numeric lengths 9/10/12, c-prefixed values and other literals. Only
two literal keys contain both payment and PO source systems: `c16503` and `c9545`.
Numeric padding produces only two additional formatting candidates, and removing
a c prefix produces three numeric resemblances. None is a newly verified identity
bridge. Do not strip prefixes, pad everything, or merge namespaces blindly.

Declare a consistent key/alias contract and a reporting-compatible sort boundary.
Any new sort is an explicit physical pass. Preserve the old measured files.

## Recommended next implementation

1. Correct the APA CSV/schema adapter with semantic fixtures, preserving the old
   measured baseline. Reconcile classification/dates/amounts, not just row counts.
2. Establish canonical key encoding and reporting order without asserting unknown
   payment/PO identity links. Test prefix/length/collision cases.
3. Implement bounded reporting for a large valid key and an unresolved group.
4. Then build a small versioned source-party registry and synthetic-detail pilot.
   Validate coverage/skew first; choose full population size afterward. The million-
   name candidate inventory calls for a bounded sorted join, not an unbounded
   per-JVM name map or one fabricated vendor per source row.

Twenty-six focused tests pass, including thirteen unchanged TTL/one-read tests.
The source and canonical scans ran as separate JVM processes with input SHA-256
and OS resource evidence. Raw/master phases used audit v1. The initial canonical
ordering assertion failed; diagnostic v2 records order/segment findings and
externally combines counts. Failure/probe logs are preserved. This adaptation
changes the diagnostic, not financial processing or the reporting implementation.
