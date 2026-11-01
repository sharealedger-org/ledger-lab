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

### Test 0: join feasibility — **CLOSED 2026-11-01, finding recorded**

**Finding:** No deterministic or independent PO-to-expenditure link exists in the public corpus.
This test is closed. The decision table it was meant to produce is recorded here.

**Evidence basis:**

| Fact | Value | Source |
|---|---|---|
| Expenditure columns | `AGENCY_NAME, AMOUNT, FUND_NAME, LOCALITY, OBJECT_NAME, PROGRAM_NAME, VENDOR_NAME, VOUCHER_NUMBER, VOUCHER_DATE` | FY2014–2016 files inspected directly |
| PO columns | `PONUMBER, AGENCY, ORDEREDDATE, QUANTITYORDERED, PRICE, VENDORID, VENDORNAME, NIGPCODE, ...` | FY2006–FY2018 files; no Object/Fund code |
| Direct link (VOUCHER_NUMBER = PONUMBER) | 1 match in 500,000 April-2014 rows | Prior probe |
| Natural-key match (agency, vendor name, amount) | Noisy; 2.9% of FY2016 POs matched expenditure vouchers; 2,857 ambiguous | Prior probe |
| Payment-side vendor ID on public expenditure files | None. No VENDORID, VLIN, or PO reference on APA CSVs | Confirmed by header inspection |
| The 2019/2020 EV matched dataset (data/POTest/VA_2016_New/) | **Constructed by a name+amount join** — `(agency_key, amount, vendor_name)` inner/left/right. `ip_rec_id` on payment rows was copied from the matched PO side, not sourced independently from a payment system. | `VA Preprocessing.xlsx` drawing1.xml flowchart, 2026-11-01 |
| EV05 unmatched payments: ip_rec_id blank | 3.5M rows; blank because they never received a PO-side ID during assembly | Confirmed by column inspection |

**Decision table (output of Test 0):**

| Linkage option | Available? | Consequences |
|---|---|---|
| 1. Direct key (VOUCHER_NUMBER = PONUMBER) | No | — |
| 2. Natural key (agency, vendor, amount, date window) | Yes, noisy only | Test 1b feasible with noise caveat; cannot support vendor-identity claims |
| 3. Aggregate (agency, vendor, fiscal year) | Yes | Tests 3, 4 feasible |
| 4. Independent payment-side vendor ID | No public source found | Test 2 (vendor substitution) **not buildable** from this corpus |

**What this means for Test 2:** the EV dataset cannot show vendor substitution because it was *defined*
by vendor-name equality — using name match as the join key then looking for name differences in the
result is circular. An independent payment-side identifier (e.g. from a Cardinal AP705/AP965 extract
with VLIN or voucher line reference) would be required. No such public extract has been located.
Test 2 is replaced below with Tests 2a–2c, which demonstrate the same architectural claim on the
data that actually exists.

### Test 1 redesign: non-injectivity count
Problem found: the notes define the posted image as (Agency + Object code + Amount), but PO lines carry no Object code.
Two feasible definitions, both stated up front so the result cannot be tuned afterwards:
- 1a (PO-side only, no join needed): proxy image = (AGENCY, NIGP 3-digit class, line amount `QUANTITYORDERED * PRICE`, fiscal year). Count distinct (VENDORID, full NIGP code, revision count from the `-Vn` suffix) patterns per proxy image. Uses data already extracted.
- 1b (needs Test 0 option 1 or 2): true posted image (AGENCY, OBJECT_NAME, AMOUNT) of the matched expenditure record, and count of distinct PO patterns mapping to it.
- Report both as a distribution (share of images with 1, 2, 3-5, 6-10, more than 10 preimages) per fiscal year, plus overall.
- Oracle: on the synthetic fixture the expected counts are known by construction; add 3 hand-built cases (two vendors same amount same agency, same vendor two NIGP, single clean PO) and assert the exact distribution.
- Failure: if more than 99% of images have exactly one preimage, Theorem 1's non-injectivity is empirically negligible on this corpus, and the paper must say so.
- Caveat to state in the paper: 1a measures a proxy, so its counts are sensitive to the choice of amount rounding and NIGP granularity; run it at both 3-digit and 5-digit NIGP.

### Test 2: vendor substitution — **REPLACED. Not buildable from public corpus.**

The original Test 2 required an independent payment-side vendor identifier. None exists in the
public APA files. The 2019/2020 EV matched dataset was assembled by name+amount join and cannot
provide it. See Test 0 finding above for the full evidence record.

The architectural claim — that commitment-layer features carry signal not visible in posted-only
records — is preserved and demonstrated in Tests 2a–2c below, using the data that actually exists.

### Test 2a: commodity portfolio forecasting (Layer 3 vs Layer 2)

**Claim:** Agency-level PO commodity mix (Layer 3, commitment view) improves prediction of
next-year goods-category expenditure over a model using only prior-year expenditure patterns
(Layer 2, posted view).

**Why this demonstrates the same architecture point as the original Test 2:**
The original claim was that commitment features carry signal not visible in posted records.
Test 2a operationalizes this at the agency-year level rather than the transaction level —
it does not require a PO-to-payment join because the unit of analysis is an agency, not a
vendor-payment pair.

**Inputs:**
- Layer 2 features: per agency-year posted expenditure by `VENDOR_NAME` (proxy for spending
  pattern). FY2014–FY2016 quarterly files are available locally with full 3-year coverage.
- Layer 3 features: per agency-year PO commodity mix (NIGP 3-digit class share), revision rate
  (`-Vn` suffix count / total POs), inventory vs. services split (NIGP class <900 vs ≥900 share),
  median order size, vendor count. Derivable from `vendor_po_attributes_by_year.csv` and PO files
  for FY2006–FY2016.
- Target: next-year agency goods-category expenditure (sum of payments to vendors with modal NIGP
  class <900 in the preceding year).

**Data available:** ~80 agencies × 3 prediction years (FY2014→2015, FY2015→2016, FY2016→2017) =
~240 labelled agency-year pairs for supervised evaluation. The 10-year PO panel (FY2006–FY2016)
supports unsupervised pattern discovery before the supervised test.

**Method:** Train two models on the same agency-year observations:
- Model L2: prior-year posted expenditure features only.
- Model L3: L2 features plus PO commodity mix and revision features.
Report RMSE and R² for each; permutation test on the Layer 3 feature block. Report results per
year separately; no pooled-only result.

**Acceptance criteria (fix before running):** Layer 3 reduces RMSE by at least X% (propose 5%)
and permutation p-value < 0.05. Fix X before the first run.

**Failure case:** If Layer 3 features do not improve over Layer 2, this is a publishable finding —
it means the posted record already captures the commodity mix information, contradicting the
compression-loss argument for this particular signal at the agency-year grain.

**Controls:** Shuffle Layer 3 features across agencies within a year; improvement must vanish.
Agency name reconciliation across PO, expenditure, and budget files must be verified first (the
three files spell agency names differently — see corpus task 5 in SESSION_HANDOFF.md).

---

### Test 2b: vendor population ecology — survival and churn (PO-only, no join)

**Claim:** The structure of the vendor-year panel (entry, persistence, exit) is non-random and
predictable from observable PO characteristics. This demonstrates that the pre-hierarchy event
record carries party-lifecycle signal that the posted-only view (which has no vendor entry/exit
concept) permanently discards.

**Data:** 65,000 vendors × up to 12 years = `vendor_po_attributes_by_year.csv`.
27,736 vendors appear in only one year; 7,553 appear in 8+ years.
12 available fiscal years with known gap years (FY2004, FY2005, FY2009 absent).

**Features (all from PO data, no join):**
- Entry cohort year, first-year PO line count, first-year NIGP class, geographic region
- Year-over-year change in commodity class (modal NIGP class shifted Y/N)
- Annual PO line count trajectory
- Agency concentration (how many distinct agencies does the vendor serve per year)

**Target:** Survival to year N+1 (binary). Churn in year N+1 (binary).

**Method:** Cox proportional hazards or discrete-time survival model on the panel.
Hold out FY2015–2016 as test period; train on FY2006–2014.

**Acceptance criteria:** C-statistic > 0.65 on held-out years with permutation p < 0.05.

**Key finding to report regardless of outcome:** The single-year churn rate (43% of vendors appear
in only one year) and its composition by commodity class, geography, and entry cohort is itself a
corpus-level finding about public procurement dynamics with no posted-record equivalent.

---

### Test 2c: inventory ordering seasonality — fiscal-year-end acceleration (PO-only)

**Claim:** Goods-category (NIGP class <900) PO ordering volume shows detectable within-year
seasonality driven by fiscal-year-end budget dynamics. This is a commitment-layer-only signal:
the posted expenditure record carries the payment date but the *commitment* (PO order date) is
the leading indicator. A posted-only model cannot see it.

**Data:** PO files FY2006–FY2016; use `ORDEREDDATE` field. Focus on top goods classes:
- Class 615 (Office Supplies, 2.9M lines): best proxy for consumables ordering
- Class 204/208 (Computer hardware/software, ~390K lines): capital procurement
- Class 175 (Lab supplies, 1.5M lines): research institution inventory

**Method:** For each agency-NIGP class combination, decompose monthly order volume into trend,
seasonal, and residual components (STL decomposition). Test whether the seasonal component shows
a statistically significant Q4 (Apr–Jun, Virginia fiscal year-end) spike.

**Acceptance criteria:** At least 3 of the top 5 goods classes show significant Q4 seasonality
(p < 0.05) in the majority of full-panel agencies (5+ years of data).

**Failure case to report:** If ordering is uniform across the fiscal year, the budget-flush
hypothesis is not supported by this corpus — which is itself informative for procurement policy.

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

## Part D: Order of work (updated 2026-11-01)

Test 0 is closed. Test 2 is replaced by Tests 2a–2c. Order of work updated accordingly.

| # | Item | Cost | Blocks |
|---|---|---|---|
| 1 | M1-M4 log validator, run on existing log | hours | M5 |
| 2 | M8 extractor regression tests | hours | nothing, protects CAR work |
| 3 | Test 1a (PO-side non-injectivity) and fixture oracle | 1 day | none |
| 4 | Fix acceptance thresholds for Tests 2a–2c and record in this file | hours | Tests 2a–2c runs |
| 5 | Test 2c seasonality (cheapest; PO dates only, no reference join) | 1 day | none |
| 6 | Agency name reconciliation across PO/expenditure/budget | 1 day | Tests 2a, 3 |
| 7 | Test 2a commodity forecasting (needs agency reconciliation) | 2 days | — |
| 8 | Test 2b vendor survival model | 2 days | — |
| 9 | M5 three C1 replicates (needs Windows validation first), then M6 | machine time | M7 |
| 10 | M7 CAR attribute run (needs VendorMaster_enriched.csv) | 1 day | Paper 1 claim |
| 11 | Tests 3, 4 (modification rate / rolling window) | weeks | agency reconciliation |

Not decided here: acceptance thresholds for Tests 2a–2c, Test 3 model size and regularization
strength, CV threshold in M5. Fix and commit all thresholds before the first run.
