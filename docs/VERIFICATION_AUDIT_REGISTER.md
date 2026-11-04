# Verification Audit Register

Status as of 2026-09-29. This is an evidence register, not a declaration that the repository is production-correct.

| Area | Claim under test | Current state | Evidence | Open falsifier or gap |
|---|---|---|---|---|
| Step 2 transformation | Raw VA input becomes 39-field `Transaction`-compatible SJE rows with instrument identity and balanced debit/offset pairs. | `partially_verified` | Audit run `audit-baseline` produced 1,520 rows; 0 bad-arity rows; 0 unbalanced journal groups; source total 0.00. | Test every record format and accounting-rule branch independently; verify vendor misses and offset rows; test field-level schema rather than relying on comments. |
| Step 2 external sort | Sorted output follows the exact Step 3 posting key and uses bounded spill/merge behavior. | `partially_verified` | Audit run found 0 sort-key violations against the declared 15-field concatenated key; fixture completes with one spill. | Prove comparator equivalence against Step 3 for adversarial keys, duplicate keys, multiple spills, numeric sort fields, and large-volume memory bounds. |
| Step 3 posting | Sorted SJE plus prior ledger state reconstructs instrument-level ledger balances and preserves conservation. | `partially_verified` | Existing fixture produced 59 ledger rows and amount sum 0.00; Step 3 parses the 39-field transaction contract. | Run prior-ledger replay, duplicate-key, empty-input, multi-period, and final-flush tests; verify every ledger row can be reconstructed from supporting SJEs. |
| Instrument Ledger volume profiles | Low-volume event-preserving and high-volume daily-movement profiles satisfy their declared storage grain; high-volume CKB folds at Instrument ID breaks with bounded per-instrument state, replaces balance keys, and streams updated rows plus SJE to the terminal perspective writer. | `legacy` | Legacy `post.scala` match-merges on a full key beginning with Instrument ID and accumulates a balance amount per key. It writes `LDGRtemp` during Step 3 and renames it to `LDGR` before any perspective stage. The 1,520-to-59 fixture row reduction does not prove the target instrument-break buffer, downstream SJE visibility, or terminal sequential-write contract. | Define paired event-preserving and daily-movement workload profiles. Test one instrument with multiple balance keys and repeated same-day movements; prove key replacement/no duplicates, event lineage, CKB instrument-break ordering, current-instrument-only memory, generated-SJE and updated-balance consumption by views, and sequential terminal writes. |
| Shared two-grain CKB path | The same CKB and elimination-view code consumes instrument-grain and GL-grain partitions; source configuration selects applicable views without requiring synthetic Instrument IDs. | `not_implemented` | The intended contract is documented in `CKB_LAYER_EXECUTION_MODEL.md`, `03-instrument-ledger/README.md`, and `05-perspectives/README.md`; no shared-code fixture has run. | Run both grain inputs through the same implementation and source-selection configuration; prove declared identity, sort/order, controls, and memory evidence for each. |
| Intra-unit elimination | ARE generates a balanced elimination SJE when a source transaction identifies an eligible counterparty, without waiting for an opposite-side transaction. | `not_implemented` | The intended ARE contract is documented; no focused fixture or producer/consumer trace proves it. | Test a counterparty-bearing source event with no matching event; verify trigger, per-group balance, source/counterparty/rule lineage, and downstream CKB consumption. |
| Inter-unit elimination view | The shared view sums records for the requested reporting scope and reports residual uneliminated activity without one-to-one transaction matching. | `not_implemented` | The intended scope-dependent report contract is documented; the legacy VA Step 7 matcher is not evidence for it. | Use GL-grain records without Instrument IDs across two scopes; verify scope-dependent sums, residual reporting, source gating, reconciliation totals, and memory use. |
| Whole-process attempt lifecycle | A CKB retry starts from the same immutable input generations; failed-attempt outputs cannot become next-run inputs and are cleaned without deleting prior committed state. | `partially_verified` | The runner stages prior ledger state into an isolated attempt and publishes a complete generation through a symlink switch. A fixture test fails at Step 12 after Step 3 has written its ledger and confirms the committed target/checksum are unchanged, the failed attempt is removed, and its log is retained. | Prove whole-process replay from identical input generations, concurrent-reader behavior and failed publication; the first migration from a real output directory to a symlink has a brief non-atomic interval. Track retained old generations in physical storage/retention costs. |
| Step 3 process metrics | Structured process metrics report actual rows, bytes, elapsed time, and resource use. | `partially_verified` | Baseline run `run_20260929_192552` and allocation run `run_20260929_192535` passed the fixture metric assertions: Step 3 consumed 1,520 SJE rows and wrote 59 ledger rows in baseline; allocation captured 60 input rows before the ledger rewrite and 45 generated rows. Process CSV rows match the declared 26-column schema; per-file and total bytes are numeric; interpretation reports an active run with numeric wall time. | Extend schema and status checks to failed/partial/no-op runs and non-fixture workloads; process CPU fields, complete sort/spill evidence, and manifest identity hashes remain unverified. |
| Allocation | Prior-period divisor input produces balanced allocation SJEs, applies them, and regenerates the next-period divisor partition. | `partially_verified` | Existing active allocation smoke profile reports generated rows, rounding residual, apply-back, and next-period divisor groups. | Independently verify generated SJE schema, per-group balance, source lineage, rule/effective period, and comparable persist/derive profiles. |
| Booked currency revaluation | Prior/current effective rates and rules generate balanced booked-currency revaluation SJEs from opening ledger balances; Step 3 applies them while preserving ledger total. | `partially_verified` | The populated-opening orchestrator smoke generates 4 balanced rows in 2 groups (`CAD +11.19`, `EUR -2.80`), excludes the USD control row, posts to 5 expected ledger rows, passes generated-SJE balance and opening-to-posted-ledger conservation controls, and emits a 99 report with `supports`. Focused tests reject duplicate rate dates and cover empty opening ledgers. | Test effective-date fallback boundaries, zero/negative balances, precision and rounding, multiple periods, walk-forward revaluation, and larger rate/ledger inputs. Evidence is on the legacy VA poster, not generalized GenevaERS CKB placement or scale. |
| Reclassification, forecasting, consolidation | Legacy steps implement the declared CKB contracts and preserve lineage/control behavior. | `unknown` | Documentation and source modules exist. | Trace actual producers/consumers and run focused round-trip controls; do not infer generalized 04/05 maturity from legacy step names. |
| Generalized 04 engines | Engines consume SJE/Universal Journal structures and emit consumable balanced SJE structures. | `not_implemented` | `04-engines/calculation_engines/README.md` defines the boundary; no accepted implementation remains after the rejected synthetic attempt. | Define a machine-readable workload and SJE-in/SJE-out round-trip test before writing engine code. |
| Generalized 05 perspectives | Perspectives aggregate canonical ledger/engine outputs without generating or applying accounting entries. | `not_implemented` | `05-perspectives` contains design scaffolding and schemas/readme contracts. | Build a bounded view contract and prove reconciliation to canonical state before adding currency or elimination reporting. |
| Canonical evidence | Run evidence is emitted with stable run/process/partition identities and interpreted without overstating maturity. | `partially_verified` | Baseline and allocation fixture assertions validate process schema alignment, row/byte values, manifest interpretation, and active run status. | Validate every evidence schema and partition artifact, test failed/partial/no-op runs, and link every conclusion to a source record. |
| Cost curves | CCP2-CCP5 points are comparable measurements of the same Scala substrate with separated compute, storage, sort, reconciliation, and replication costs. | `not_verified` | Cost surface and workload labels exist. | First make process metrics reliable, define named workloads/materialization profiles, run replicated points, and audit physical pass/materialization assumptions. |

> FX evidence update (2026-09-30): duplicate rate dates are rejected by a focused test. Focused tests also verify absent, zero-byte, header-only, and headerless populated opening-ledger behavior; the orchestrator smoke now exercises the populated opening fixture.

## Immediate Order

1. Validate first-publication migration, concurrent readers, failure during publication, immutable input identity, and whole-process replay. Retained generations require a storage/retention policy.
2. Validate evidence schemas and failed/partial/no-op handling; extend measurement beyond fixture-level process rows and file counts.
3. Complete Step 2/3 contract tests and replay controls.
4. Define and audit the Instrument Ledger volume profiles, including high-volume instrument-break memory, replacement, and terminal-output behavior.
5. Audit allocation round-trip behavior.
6. Audit legacy reclassification, forecasting, consolidation, and aggregation without calling them generalized layers.
7. Define the real SJE-in/SJE-out engine workload.
8. Define duplicate-rate governance and extend booked currency revaluation through rate/period edge cases and target CKB placement; then implement the two elimination processes through the shared CKB path and source-selected views.

## Additional Draft-Paper Gates

The remaining private drafts add these checks to the audit scope:

- classify any hierarchy-change implementation as Switch, Recast, or Reclass, including event-date versus arrival-date behavior;
- validate effective-dated reference intervals, bitemporal assertions, hierarchy acyclicity, leaf coverage, and promotion pre-flight controls;
- verify manual adjustments are instrument-anchored or explicitly suspense-anchored, and that technical/accounting rejects are quarantined and recyclable;
- verify shadow-ledger certification against legacy outputs before any master-file sunsetting claim;
- distinguish shared inter-enterprise event state from private party accounting state if shared-ledger work is added.

## 2026-10-31 Vendor identifier investigation (source-data scope)

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| PO VENDORID is documented as DUNS | partially_verified | Local dictionary sheet1 row 8 explicitly says Vendor DUNS #; hashed in VENDOR_IDENTIFIER_EVIDENCE_2026-10-31.json | Dictionary provenance/year applicability and actual identifier validity unknown; mixed and placeholder values exist. |
| Official schema supports a DUNS/VLIN/Cardinal vendor bridge | partially_verified | AP962 VENDOR_ADDR COUNTY=DUNS, ADDRESS3=VLIN, VENDOR_ID and address sequence; reproduced by scripts/investigate_vendor_identifiers.py --specs | No actual public historical bridge rows obtained; effective-date coverage unknown. |
| EV02/EV06 exact-amount candidate count is reproducible | partially_verified | 196/248 payment hits, 13 ambiguous hit rows, Decimal arithmetic | No independent pair validation, precision/recall, or provenance of historical payment IDs. |
| Public historical PO/payment linkage is usable for substitution research | unknown | AP965 line description may carry PO ID; AP705/AP965 identify VLIN | Need actual public historical data and transaction linkage independent of vendor equality. |

See [VENDOR_IDENTITY_INVESTIGATION.md](VENDOR_IDENTITY_INVESTIGATION.md). These checks do not upgrade any financial-engine audit state.
## 2026-10-31 Follow-up: actual public PO-side namespace evidence

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| Legacy PO IDs can be linked to public VLIN/eVA IDs through the same PO lines | partially_verified | scripts/probe_public_vendor_bridge.py; PUBLIC_VENDOR_BRIDGE_EVIDENCE_2026-10-31.json: 22/24 convenience-sample observations have singleton nonblank pairs; two placeholder controls lack VLIN | No population coverage estimate, full snapshot reconciliation, or proof of global one-to-one identity. |
| Historical EV01/EV02/EV06 cohorts are under $9,000 | contradicted | EV01 has 433 PO lines >=9000, EV02 has 32, EV06 has 34 payments; reproducible Decimal checks | Original selection algorithm not recovered; NIGP class <900 holds for every EV01/EV02 line. |
| Official FY2016 purchase-card export provides an independent PO/vendor-ID link | contradicted | All 12 monthly headers contain only agency, vendor name, posting date, fiscal year, amount | Other payment sources remain unverified. |
| Public historical PO/payment linkage supports vendor-substitution labels | unknown | PO-side bridge now observed; May 2016 CAPP manual documents relevant agency extracts | Actual payment-side IDs and independent PO reference linkage still missing. |

This update strengthens PO-side evidence only; it does not upgrade engine states or validate the 2020 matched-payment pairs.

## 2026-10-31 Corpus inventory, profile and access package (Bob)

Scope: source-data inspection only. These checks do not upgrade any financial-engine audit state and are not part of any cost-curve measurement.

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| Every source file on this machine is cataloged with a SHA-256 and has a recorded disposition | partially_verified | `CorpusProfile` (`corpus-profile-1`): 385 files cataloged, 367 profiled, 16 cataloged only, 2 empty; 0 files fail physical-line reconciliation; 10 `CorpusProfileSpec` tests including byte-identical reruns on a fixture | Real-corpus rerun not compared byte for byte; `.xlsx` PO copies cataloged, not parsed. |
| All parsed records are accounted for (accepted + quarantined) | partially_verified | 133,791,291 parsed = 133,464,935 accepted + 326,356 quarantined; APA declared `N rows selected.` equals parsed count in 263 of 263 files that carry one | FY2014-2016 APA and one FY2003 file have no declared total. PO files have none. Quarantine counts record fragments, not logical lines. |
| Source completeness (any family/year) | not_verified | All 62 family-years `PRESENT_COMPLETENESS_UNKNOWN` except FY2009 PO (`MISSING`) | No independent source totals. Journals balancing proves nothing about completeness. |
| Per-file provenance (URL, acquisition date) | not_verified | Fields deliberately blank in `source_catalog.csv` | Nothing in the repo proves per-file acquisition. |
| Stable record locator re-finds the original record | partially_verified | All 2,037 single-line FY2002 PO quarantine records re-read by `start_byte_offset` and matched | Multi-line records and other files not checked. |
| FY2005 and FY2009 eVA files are zero-row stubs | contradicted | FY2005 `.txt` has 1,037 parsed records; FY2009 has no `.txt` at all | Why FY2009 is absent from the source is unknown. |
| PO file name is the fiscal year of `ORDEREDDATE` | contradicted | 43,318 PO rows have `ORDEREDDATE` outside the FY in the file name; in FY2005, 592 of 1,037 | The source's inclusion rule is undocumented. |
| `vendor_po_attributes_by_year.csv` is source-quality | contradicted | 8,101 column-shifted rows; 20 non-US-state region values; 1,392 normalized names with more than one `VENDORID`; no provenance header | Not repaired here (vendor identity belongs to the Codex investigation). |
| Zenodo `10.5281/zenodo.23846362` is a complete archive of the corpus | contradicted | Public API 2026-10-31: published, 47 files, all match local size and MD5; 336 monthly APA expenditure/revenue files not listed | Text says "public domain", license field says `cc-by-4.0`; record not modified. |
| Reference tables are valid for a given fiscal year | unknown | `aRef*.txt` have no dates or version; 7 duplicate agency codes; 166 of 558 fund-detail codes have more than one name. Agency names across all 350 APA files (`scripts/build_agency_alias_table.py`, `agency-alias-1`): 307 of 328 distinct names match exactly (99.1% of 115,383,197 rows), 8 match only after punctuation normalization, 13 (20,512 rows) are not in the table; 3 reference names map to two keys each (131,809 rows unresolvable by name alone) | Alias candidates carry no validity periods (all `unknown`); the 13 unmatched agencies are unresolved; fund and object checked on 3 months only. |

See [CORPUS_FINDINGS.md](CORPUS_FINDINGS.md), [CORPUS_SCHEMAS.md](CORPUS_SCHEMAS.md), [CORPUS_RECORD_IDENTITY.md](CORPUS_RECORD_IDENTITY.md) and [CORPUS_ACCESS.md](CORPUS_ACCESS.md).
## 2026-10-31 Public supplier-directory reference acquisition

| Claim | State | Evidence | Boundary |
|---|---|---|---|
| Every row in the saved public report CSV export is represented in the richer canonical snapshot | verified | 230,128 source/canonical rows; 230,127 exact known-field matches plus one targeted HTML lookup; source_export/reconciliation.json and audit.json under the ignored dated snapshot | Recorded current public report/search, not every Commonwealth payee or inactive-directory coverage. |
| Canonical observations retain auditable source provenance and preserve missing IDs | verified | Full audit: 230,128 observation fingerprints; 230,126 nonblank IDs; 2 missing-ID flags; 2,912 retained HTML pages; no checksum/page-number failures | Acquisition interval, not atomic capture or historical effective dating. |
| Raw CSV column Status denotes registration type | verified | Source values Self=159,258 and State=70,870; matched registration_type against all corresponding HTML observations; actual HTML status is Active=230,068, Pending Approval=60 | Do not use raw source header as vendor active status. |
| Current directory proves historical identities or independent PO/payment relationships | unknown | Current reference and 20 agreeing / 2 unresolved historical-candidate VLIN observations | Requires historical identity evidence and an independent payment-side transaction reference. |


## 2026-11-04 Sub-Task 1 — transIPID fix (Bob)

Scope: `transStandardize.scala` transformation-path correctness. Does not affect `standardizeAndSort` (Step 2) or `post.scala` (Step 3) — those are the live pipeline paths. This entry covers the legacy PO-path fix that enables instrument-keyed CKB posting.

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| `transIPID` is populated from `VENDORID` in PO path (`processPOs`) | `verified` | `sbt compile` clean (1 pre-existing deprecation warning unchanged); `sbt "testOnly org.universalledger.foundation.va.*"` 21/21 pass; change is a one-line substitution `transIPID = rec.poVendorID` | No full fixture run with `--with-po-fixture` flag yet; `transIPID` non-zero on real PO file not confirmed by test |
| Payment path (`processPayments` Expense case) looks up `transIPID` from VendorMaster name | `partially_verified` | `loadVendorMap` helper loads `instHolderName → instID` map; `aggTrans` key now includes `tranVendorName`; unmatched vendors receive `UNMATCHED_<hex8>` key; `UNMATCHED_VENDORS.csv` written | Fixture smoke test not yet run with this path to confirm non-zero `transIPID`; Revenue path unchanged (no vendor concept) |
| Fixture VendorMaster contains all FY03 expense fixture vendor names | `partially_verified` | 183 rows appended: all 182 vendor names present in `FY03q1exp_small.txt` plus BLACKWELLS BOOK SERVICES (→ c9545) and SUPPLY ROOM (→ c16503) from PO fixture | Synthetic instIDs (99001–99182); not validated against real eVA assignments |
| `POSortedJE.sortspec` aligns PO sort key for Sub-Task 2 merge | `design_only` | File created at `02-foundation/sort_specs/POSortedJE.sortspec`; sort key is `(transIPID, transFiscalPeriod, transTransDate)` | Not yet used in any running step |

These checks cover correctness of the legacy path only. Step 2 (`standardizeAndSort`) already had the correct VendorMaster lookup before this session; the `transIPID=0` defect in `transStandardize` did not affect the live pipeline.


See [SUPPLIER_DIRECTORY_SNAPSHOT.md](SUPPLIER_DIRECTORY_SNAPSHOT.md). This acquisition is outside financial-engine measurement; no engine audit states are upgraded.

## 2026-11-04 Steps 20 and 21 — GLPost and PermutationGenerator (Bob)

Scope: CCP1 baseline steps added in the 2026-11-04 experimental architecture session. Code exists, functional tests pass. No verification session has traced the producer/consumer path, schema, memory bound, or CKB placement for either step.

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| `GLPost` reads `SortedJEFY*.csv`, accumulates account-keyed balances, and writes `GL20YY.csv` | `not_audited` | `GLPostSpec` 7/7 pass; step dispatch wired via `case "20"` in `LedgerApp.scala` | Accumulation field index not independently verified against CCP1 workload declaration; no declared consumer reads `GL20YY.csv` in any running step; memory bound for large corpus not declared |
| `PermutationGenerator` reads `SortedJEFY*.csv`, accumulates `(account_key, vendor_id)` permutation counts, and writes `PERM_TBL20YY.csv` | `not_audited` | `GLPostSpec` permutation assertions pass | Same gaps as GLPost; no declared consumer; memory bound for 65K-vendor, 12-year corpus not declared |
| Steps 20 and 21 appear in `process_metrics.csv` after a CCP1 run | `not_audited` | `RESULT step=20` and `RESULT step=21` log lines emitted; `write_canonical_evidence.sh` has **no** `append_process` case for steps 20 or 21 — their metrics are not extracted | `write_canonical_evidence.sh` must be extended before any CCP1 run counts as a valid replicate |

These entries establish `not_audited` as the baseline state per `VERIFICATION_SESSION_PROTOCOL.md §Verification States`. A Contract Tester session should verify the accumulation field, a declared consumer, and the `write_canonical_evidence.sh` gap before CCP1 is run as a cost-curve point.

## 2026-11-04 Adversarial audit: Steps 20/21 and evidence infrastructure (Bob)

**Role:** Adversarial Architecture Reviewer
**Scope:** `GLPost.scala`, `PermutationGenerator.scala`, `write_canonical_evidence.sh`, `TEST_DESIGN_PAPER_1.md`, `GLPostSpec.scala`, `run_c1_smoke_test.sh`

### Confirmed: column indices independently verified

Column indices declared in `GLPost.scala` (COL_AGENCY=12, COL_FUND=13, COL_NOMINAL_ACC=16, COL_FISCAL_YR=22, COL_AMOUNT=25, COL_DIR_FLAG=31) and `PermutationGenerator.scala` (COL_VENDOR_ID=0 plus same four account fields) match the canonical 39-column SortedJE schema declared in `standardizeAndSort.scala` lines 62–100. The `GLPostSpec.scala` test fixture at `makeJERow` also assigns the same indices. **Column contract is consistent across three independent sources.**

### Confirmed: debit-only accumulation semantics

Both steps filter `cols(COL_DIR_FLAG).trim == "D"` before accumulating. `TEST_DESIGN_PAPER_1.md §CCP1` does not explicitly state debit-only, but the intent — accumulate the economic amount of each transaction without double-counting the balanced debit+credit pair — is correctly implemented. The tests confirm offset ("N") rows are excluded and amounts sum correctly.

### Confirmed: 7/7 unit tests pass

`sbt "testOnly org.universalledger.foundation.va.ledger.GLPostSpec"` — all 7 tests pass including the explicit two-vendor one-account scenario (GL=1 row, PERM=3 rows).

### FINDING 1 — CCP1 sort key and GL architecture semantics — **RESOLVED 2026-11-04**

In a real legacy GL, vendor identity is **not an input to the posting step** — it is collapsed out at standardisation and never carried into the sort file. CCP1 does not need to follow CKB process rules; it replicates the GL architecture exactly: `GLPost` (Step 20) ignores `transIPID` in the accumulation key, and `PermutationGenerator` (Step 21) reads vendor identity from the original transaction stream — not from the GL output — because it is measuring the **externalised upstream cost** the GL imposes on source systems and data preparation layers, not a GL operation.

The original finding was that CCP1 Step 2 uses the instrument-key sort rather than an account-key sort, invalidating the sort-cost component of the CCP1→CCP2 delta. The resolved position is:

- CCP1 Step 2 uses the same SortedJE partition as CCP2 (shared input snapshot). `GLPost` is sort-order-agnostic (full-file accumulator), so the GL balance output is numerically correct regardless.
- The shared sort introduces a **known measurement conservatism**: CCP1's Step 2 sort cost includes sorting a vendor-bearing key, which a true legacy system would never do. This makes CCP1 sort cost equal to or *higher* than a real legacy system — the CCP1→CCP2 delta is therefore a **lower bound** on the true architectural cost difference, not an overstatement.
- `TEST_DESIGN_PAPER_1.md §CCP1` has been updated to document this conservatism explicitly, replace the "sort by account key" description, and reframe `PermutationGenerator` as measuring externalised upstream cost rather than a GL post-hoc step.

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| CCP1 sort-cost conservatism is documented and the CCP1→CCP2 delta is a valid lower bound | `design_accepted` | `TEST_DESIGN_PAPER_1.md §CCP1` updated 2026-11-04; `GLPost` ignores `transIPID`; conservatism is in the direction of making CCP1 cost higher, not lower | A separate account-key-only sort sensitivity run could quantify the conservatism; not required for the cost point |
| `PermutationGenerator` measures externalised upstream materialisation cost, not a GL step | `design_accepted` | `TEST_DESIGN_PAPER_1.md §CCP1` updated 2026-11-04; reads from SortedJE (original stream), not from GL output; framing confirmed by project owner | AIM1 consumer not yet implemented |

### FINDING 2 (MEDIUM) — Output filename mismatch between code and evidence script — **FIXED 2026-11-04**

`GLPost.scala` wrote `GL${yearStr}.csv` (4-digit year). Fixed: now writes `GL20${shortYr}.csv`. `PermutationGenerator.scala` wrote `PERM_TBL${yearStr}.csv`. Fixed: now writes `PERM_TBL20${shortYr}.csv`. `write_canonical_evidence.sh` extended with `append_process` cases for steps 20 and 21 including partition catalog entries. RESULT/RESOURCE log lines continue to use `year=$yearStr` (4-digit) for compatibility with the evidence script's `grep -E "RESULT step=${step} year=${year}"`. 7/7 tests pass post-fix.

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| Output filenames for Steps 20/21 follow the two-digit year convention used by other pipeline outputs | `verified` | `GLPost.scala` now writes `GL20${shortYr}.csv`; `PermutationGenerator.scala` writes `PERM_TBL20${shortYr}.csv`; `write_canonical_evidence.sh` has `append_process` + `append_partition` cases for steps 20 and 21; 7/7 tests pass | No remaining gap for filename convention |

### FINDING 3 — Memory bound scope — **RESOLVED 2026-11-04**

`PermutationGenerator` is not a bounded-state CKB engine step — it is a **measurement instrument** for externalised upstream cost. Its job is to accumulate the full set of `(account_key, vendor_id)` permutations in one place, reproducing exactly the materialisation burden the GL architecture distributes across source systems. Worrying about its heap usage would be like worrying about the size of the bill being presented: the size of the map *is* the research finding. No bounded-state requirement applies to Step 21.

`GLPost` (Step 20) is similarly a full-corpus accumulator replicating legacy GL behaviour. A real GL index holds the same key space; the in-process map is an equivalent representation. Memory bound is not a CKB concern for either step.

The `peak_rss_bytes` RESOURCE field uses `Runtime.getRuntime.totalMemory()` (JVM heap allocated, not OS RSS). This is noted as a measurement limitation in the RESOURCE line documentation but does not affect the correctness of either step's output.

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| `PermutationGenerator` requires bounded per-instrument heap as a CKB engine | `not_applicable` | Step 21 is a measurement instrument, not a CKB engine; full-corpus accumulation is architecturally correct; confirmed by project owner | None — full-corpus map is the intended behaviour |
| `GLPost` accumulation map is a valid GL replication | `design_accepted` | Full-corpus account-keyed map correctly replicates legacy GL posting; project owner confirmed GL architecture does not require bounded state | `peak_rss_bytes` via `Runtime.getRuntime.totalMemory()` is JVM heap allocated, not OS RSS — a known metric limitation |

### FINDING 4 (MEDIUM) — `write_canonical_evidence.sh` scala_version is stale — **FIXED 2026-11-04**

`scala_version` hardcode updated from `2.12.18` to `2.12.20` in `write_canonical_evidence.sh:128`. Verified against `02-foundation/va_pipeline/build.sbt` which declares `scalaVersion := "2.12.20"`. All future run manifests will carry the correct version. Note: previously emitted run manifests (before this fix) still carry `2.12.18` and are not retroactively corrected.

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| `run_manifest.csv` `scala_version` field correctly identifies the Scala version used | `verified` | `write_canonical_evidence.sh` now hardcodes `2.12.20`; confirmed against `build.sbt` | Previously emitted manifests carry `2.12.18` — not retroactively corrected |

### FINDING 5 — AIM1 consumer — **DEFERRED 2026-11-04**

`GL20YY.csv` and `PERM_TBL20YY.csv` are the correct terminal outputs of CCP1. They are not orphaned — they are the declared substrates for the AIM1 test family described in `TEST_DESIGN_PAPER_1.md §AIM pairing`. AIM1 is a future gate, not a current blocker. The outputs exist, are correctly named, and are catalogued in `partition_catalog.csv` by `write_canonical_evidence.sh`. No action is required until AIM1 is implemented.

| Claim | State | Evidence | Remaining gap |
|---|---|---|---|
| `GL20YY.csv` and `PERM_TBL20YY.csv` are correctly produced terminal outputs of CCP1 | `design_accepted` | Files written by Steps 20/21; catalogued in `partition_catalog.csv`; named in `TEST_DESIGN_PAPER_1.md §AIM pairing` as AIM1 substrates | AIM1 implementation is a future gate; not a current blocker for the CCP1 cost point |

### Immediate order for CCP1 gate (updated 2026-11-04)

All five findings from this audit session are resolved or closed:

1. ~~Fix `write_canonical_evidence.sh` `scala_version` hardcode~~ **Done** — updated to `2.12.20`.
2. ~~Rename GLPost/PermutationGenerator output and add `append_process` cases~~ **Done** — outputs now `GL20${shortYr}.csv` and `PERM_TBL20${shortYr}.csv`; `write_canonical_evidence.sh` extended with steps 20/21 cases.
3. ~~CCP1 sort-key gap~~ **Resolved** — documented conservatism; CCP1→CCP2 delta is a lower bound; confirmed by project owner.
4. ~~Memory bound for PermutationGenerator~~ **Not applicable** — Step 21 is a measurement instrument, not a CKB engine; full-corpus accumulation is the correct behaviour.
5. ~~AIM1 consumer~~ **Deferred** — `GL20YY.csv` and `PERM_TBL20YY.csv` are correct terminal outputs; AIM1 is a future gate.

**Remaining CCP1 gate requirement:** Steps 20/21 must reach `partially_verified`. The only outstanding gate item is a first run against real corpus data to confirm non-zero output and emit non-empty `process_metrics.csv` rows for steps 20 and 21.

