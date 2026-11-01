# Test Design: Paper 1 Cost Surface and Paper 2 VA Tests

Status: design only, 2026-10-31. Nothing here has been implemented or run at full scale.
Continues the "Next steps" list in [SESSION_HANDOFF.md](../SESSION_HANDOFF.md).

Each test states one falsifiable claim, its inputs, the oracle (how a result is checked),
and the result that would count as failure. Language boundaries follow [AGENTS.md](../AGENTS.md):
the measured financial path stays Scala/Bash; the Paper 2 analysis below is Python
"one-off research analysis" and is not part of any cost-curve measurement.

---

## Part A: What the existing `data/pipeline_log.csv` already tells us

The only log in the repo is one run, `run_20260917_224112`, FY2003 and FY2004, Steps 2, 3 and 5.
It is a legacy run (it predates the 2026-10-31 metric fixes in commit `dd15907`).

| Observation | Evidence | Consequence for tests |
|---|---|---|
| Step 2 and Step 3 `rows_in` are the literal string `None` | rows 2-13 | Legacy writer. Test M1 must reject non-numeric counts. |
| Step 5 `rows_in` equals Step 3 `rows_out` | 581,744 and 510,741 | Real cross-step invariant; keep as test M2. |
| Step 5 `rows_out / rows_in` is 1.20 (FY03) and 1.23 (FY04) | 700,875 / 581,744; 626,034 / 510,741 | One reporting view expands rows ~20%. Candidate C1-to-C2 marginal-cost quantity. |
| Sum of Step 2 `rows_out` is 12,593,262 (FY03) and 12,224,602 (FY04), against ledger rows of 581,744 and 510,741 | rows 2-13 | Posting reduction is about 21.6x and 23.9x. Invariant M3: ledger rows < SJE rows. |
| Step 5 elapsed is 12.81 s and 12.00 s against about 345 s and 306 s for Steps 2+3 | rows 2-15 | If C1 = Steps 2,3 and C2 = Steps 2,3,5, the log suggests the view adds about 3.7-3.9% of compute. This is one unreplicated number, not a result. |
| Every row has the same `run_timestamp`; no experiment, point, curve, or replicate column | header | The log cannot distinguish C1 from C2 or identify replicates. Needs `run_manifest.csv` per [MEASUREMENT_AND_LOGGING.md](MEASUREMENT_AND_LOGGING.md). |

---

## Part B: Paper 1 measurement tests (C1 / C2 / CAR-attribute)

### M1. Log schema and sanity validator (cheap, build first)
- Claim: every row of a produced `pipeline_log.csv` has numeric `elapsed_s`, `rows_out`, `storage_bytes`, and numeric `rows_in` for steps 3 and 5.
- Oracle: a Python log-checker (post-run analysis, allowed by AGENTS.md). Run it against `data/pipeline_log.csv`.
- Expected today: FAIL on the legacy log (`None` values). Expected after a fresh run on the fixed pipeline: PASS. This also proves the Step 3 `records_read=0` repair.

### M2/M3. Cross-step invariants
- M2: Step 5 `rows_in` == Step 3 `rows_out` for the same file label.
- M3: Step 3 `rows_out` < sum of Step 2 `rows_out` for the same year.
- M4: bytes per row is stable by step (log shows about 220 B/row for Step 2, about 140 B/row for Step 3). Flag drift of more than 10% across years, which may mean a format change rather than a cost change.
- Falsifier: any violation means the log or the pipeline is wrong, not that the architecture is.

### M5. C1 replicate stability (Handoff next step 2)
- Workload: Steps 2,3 on FY03 full data, 3 replicates, same input snapshot.
- Claim: coefficient of variation of wall time is below a threshold fixed before the run (propose 10%); `rows_out` and `storage_bytes` are identical across replicates.
- Oracle: row counts and bytes must be exactly equal (deterministic); only time may vary.
- Failure: any row-count or byte difference means the pipeline is non-deterministic, which blocks C2.
- Note: discard no replicate; record cold-cache versus warm-cache runs separately.

### M6. C2 marginal cost (Handoff next step 3)
- Workload: Steps 2,3,5 on the same snapshot as M5.
- Claim: C2 minus mean C1 equals the Step 5 cost; report compute delta, storage delta, `master_file_count` delta.
- Pass criterion: delta exceeds C1 replicate spread (otherwise the delta is noise).

### M7. CAR attribute addition is free of balance records (the Instrument Pivot Theorem demonstration)
- Setup: re-run C2 after adding `instNIGPClass` to VendorMaster from `vendor_po_attributes_by_year.csv`.
- Claim: ledger row count and ledger byte count are unchanged; only the VendorMaster side input and view rows change.
- Oracle: byte-for-byte equality of the Step 3 ledger output (checksum) between C2 and C2+attribute.
- Failure: any ledger difference falsifies "zero new balance records".
- Comparator: the key-embedded alternative, computed analytically as `(1+v)^m - 1`, must be labelled as a model, not a measurement.
- Dependency: handoff next step 6 (VendorMaster_enriched.csv); until it exists, `car_detect_changes.py` defaults every vendor to EXP, so ATTR_NIGP changes are real but instTypeID changes are not.

### M8. Regression tests for the PO attribute extractor (fixture-scale, no external data)
Both bugs fixed on 2026-10-31 have no test. Use `data/VAPOData_fixture/VA_opendata_FY2003_small.txt` plus a few hand-made rows:
1. 5-digit NIGP `96042` yields class `960` (guards the `len==3` bug).
2. `VENDORSTATE` of `Binghamton` or `each` becomes empty; `NY` is kept (guards column shift).
3. `NIGPCODE` of `eac` becomes empty.
4. Blank first line before the header is tolerated.
5. Files under 0.5 MB are reported as STUB and skipped (FY2005, FY2009 behavior).

---

## Part C: Paper 2 VA tests

### Test 0 (new, must precede Tests 1-4): join feasibility
The handoff names the PO-to-expenditure join key as the blocking open question. A first probe
on this machine (read-only, not committed, partial samples) gives this picture:

| Fact | Value |
|---|---|
| Expenditure columns | `AGENCY_NAME, AMOUNT, FUND_NAME, LOCALITY, OBJECT_NAME, PROGRAM_NAME, VENDOR_NAME, VOUCHER_NUMBER, VOUCHER_DATE` |
| PO columns | `PONUMBER, AGENCY, ORDEREDDATE, ... QUANTITYORDERED, PRICE, VENDORID, VENDORNAME, ... NIGPCODE ...` |
| PO `VENDORID` namespace | Documented as Vendor DUNS # (local data-dictionary sheet1 row 8). Also includes alphanumeric `DBxxxxxxx` values and a 12-nines placeholder; do not treat as tax IDs. |
| PO number appears in expenditure files? | No column. `VOUCHER_NUMBER` equals a FY2014 `PONUMBER` in 1 of 500,000 April-2014 rows. |
| PO has Object / Fund code? | No. |
| Natural-key match (agency name, vendor name, rounded amount) | 1,249 of 664,526 August-2013 expenditure rows hit a key from the first 300,000 FY2014 PO lines; 125 of those keys were ambiguous. |
| `PONUMBER` shape | Mixed (`AAA9999999`, `AA9999999`, `A999_9999999`, ...). Suffix `-V2` marks revised versions of a PO. |
| VLIN bridge (PO side) | Public CKAN FY2016/FY2018 data carries VLIN and eVA ID per PO line; 22/24 sampled legacy IDs matched singleton pairs. This is PO-side namespace evidence only; expenditure files still have no vendor ID. |

Caveats: the natural-key probe used only a partial PO sample (about 18% of FY2014) and one expenditure
month, and compared PO order date to a different posting window, so 1,249 is a floor, not a match rate.
Treat these numbers as a reason to design Test 0, not as findings.

Test 0 claim: a deterministic or high-precision linkage from PO line to posted expenditure record exists.
Candidates, in order of cost:
1. Direct: `VOUCHER_NUMBER` or any expenditure field containing the PO number. Probe says unlikely.
2. Natural key: (agency, vendor, amount) with a date window of PO `ORDEREDDATE` to `VOUCHER_DATE`. Needs agency and vendor name normalization (the two files spell them differently, e.g. fixture uses vendor name strings, full data uses `VENDOR_NAME`).
3. Aggregate linkage only: (agency, vendor, fiscal year). Enough for Tests 3 and 4, not for Test 2.
- Oracle: precision measured on the fixture pair (10 synthetic PO rows already match the FY03 expenditure fixture by vendor and amount: 271.93 and 289.74) plus a hand-labelled sample of about 200 real pairs.
- Output: a decision table recording which of Tests 1-4 remain feasible. If only option 3 exists, Test 2 (vendor substitution) is conditional on finding an independent payment-side vendor identifier, and should be removed from Paper 2 or deferred rather than weakened.

### Test 1 redesign: non-injectivity count
Problem found: the notes define the posted image as (Agency + Object code + Amount), but PO lines carry no Object code.
Two feasible definitions, both stated up front so the result cannot be tuned afterwards:
- 1a (PO-side only, no join needed): proxy image = (AGENCY, NIGP 3-digit class, line amount `QUANTITYORDERED * PRICE`, fiscal year). Count distinct (VENDORID, full NIGP code, revision count from the `-Vn` suffix) patterns per proxy image. Uses data already extracted.
- 1b (needs Test 0 option 1 or 2): true posted image (AGENCY, OBJECT_NAME, AMOUNT) of the matched expenditure record, and count of distinct PO patterns mapping to it.
- Report both as a distribution (share of images with 1, 2, 3-5, 6-10, more than 10 preimages) per fiscal year, plus overall.
- Oracle: on the synthetic fixture the expected counts are known by construction; add 3 hand-built cases (two vendors same amount same agency, same vendor two NIGP, single clean PO) and assert the exact distribution.
- Failure: if more than 99% of images have exactly one preimage, Theorem 1's non-injectivity is empirically negligible on this corpus, and the paper must say so.
- Caveat to state in the paper: 1a measures a proxy, so its counts are sensitive to the choice of amount rounding and NIGP granularity; run it at both 3-digit and 5-digit NIGP.

### Test 2: vendor substitution (conditional on Test 0)
- Claim: a model with commitment features predicts "paid vendor differs from committed vendor" better than a model restricted to posted features.
- Label construction is the weak point: substitution must be defined from matched pairs, so label noise equals linkage error. Report label precision from Test 0 and compute results on the high-confidence subset only.
- Key design point from the notes: the posted-only model cannot see the committed vendor. Use a majority-class baseline and a posted-features model as controls, so a win is attributable to the missing feature and not to model size.
- Split: by fiscal year, never random, to avoid leakage across revisions of the same PO.
- Acceptance thresholds: unset by design (PAPER_02_NOTES open question 3); fix them with the domain expert before running.

### Test 3: modification rate and next-year budget deviation
- Feature (Layer 3): per agency-year count of `-Vn` revised PO lines, share of POs revised, and median order-to-revision gap. Layer 2 baseline: posted expenditure ratios by agency-year.
- Target: next-year deviation from budget. `BudgetRules.csv` and the FY budget files in `data/VARawFiles/` are the candidate source; confirm that agency names reconcile across PO, expenditure, and budget files first, since all three spell agencies differently.
- Sample size warning: PO data is usable for only 11 of FY2003-FY2016 (FY2004, FY2005 and FY2009 are stubs or absent per `po_attributes_by_year_summary.txt`), and FY2003 has only 52,426 PO lines against 1.2-1.7M in later years. Agency count has not been measured; check it before choosing model size. With roughly (agencies x 10 years) rows, models must be small and regularized, and a Layer 3 win needs a permutation test, not just a held-out score.
- Control: shuffle the modification feature across agencies within a year; Layer 3 advantage must vanish.

### Test 4: rolling-window robustness
- The notes' "7 windows" assumes a fixed 7-year training span over 14 years (test years FY10-FY16). PO data is missing for FY2004, FY2005 and FY2009, so most of those windows contain holes, and FY2003 is thin. Decide before running whether holes are skipped or windows are dropped, and publish the final window list. The 7 windows also overlap heavily in training data, so they are not fully independent replications.
- Claim: Layer 3 beats Layer 2 in a pre-declared minimum number of windows (propose at least 5 of the usable windows).
- Report each window separately; no pooled-only result.

---

## Part D: Order of work

| # | Item | Cost | Blocks |
|---|---|---|---|
| 1 | M1-M4 log validator, run on the existing log | hours | M5 |
| 2 | M8 extractor regression tests | hours | nothing, protects the CAR work |
| 3 | Test 0 join feasibility with labelled sample | 1-2 days | Tests 1b, 2, 3, 4 |
| 4 | Test 1a (PO-side) and fixture oracle | 1 day | none |
| 5 | M5 three C1 replicates, then M6 C2 | machine time | M7 |
| 6 | M7 CAR attribute run (needs VendorMaster_enriched.csv) | 1 day | Paper 1 claim |
| 7 | Tests 2-4 after thresholds are agreed | weeks | Test 0 outcome |

Not decided here: acceptance thresholds for Tests 2-4 and the CV threshold in M5. Both should be
fixed and committed before the first run so they cannot be adjusted to the result.
