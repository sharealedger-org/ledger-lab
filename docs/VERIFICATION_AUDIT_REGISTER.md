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
| Cost curves | C1-C4 points are comparable measurements of the same Scala substrate with separated compute, storage, sort, reconciliation, and replication costs. | `not_verified` | Cost surface and workload labels exist. | First make process metrics reliable, define named workloads/materialization profiles, run replicated points, and audit physical pass/materialization assumptions. |

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

See [SUPPLIER_DIRECTORY_SNAPSHOT.md](SUPPLIER_DIRECTORY_SNAPSHOT.md). This acquisition is outside financial-engine measurement; no engine audit states are upgraded.