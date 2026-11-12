# Experiment State and Graph Coverage, 2026-11-11

**Branch:** `aim-three-model` (with `main` at `0ff9255` merged in).
**Purpose:** one place that says what is measured, what each of the three Paper 1 graphs ([`COST_GRAPH_DESIGN.md`](COST_GRAPH_DESIGN.md)) can and cannot show yet, and where Paper 2 stands. Status words follow [`CURRENT_RESULTS.md`](CURRENT_RESULTS.md); the register wins on any disagreement.
**How the numbers below were made:** `awk` over the archived CSVs in `data/test_evidence/corpus_runs/` (`process_metrics_all_years.csv`, `cost_surface_all_years.csv`, `corpus_wallclock.csv`). Nothing was re-run and no Python was written. Charts were drawn from those numbers in the chat session.

## 1. Measured facts (three full 14-year runs)

| Run | Layout | Wall s | Step CPU s | Chain CPU s (steps 3, 20, 21) | Outputs |
|---|---|---|---|---|---|
| Baseline `20261110_210721` | Serial years, `5v` last | 11,695 | 10,670 | 3,913 | 42/42 controls PASS |
| Run 3 `20261111_014229` | 2 chains, `5v` early | 8,147 | 11,179 | 4,506 | identical to baseline on the files compared |
| Run 4 `20261111_064052` | Same as run 3 | 8,544 | 11,207 | 4,593 | 74 final files byte-identical to gold |

Findings that hold up:

1. **Layout trades wall time for CPU.** Wall clock falls 27% to 30%; step CPU rises about 5%. Status: `partially_verified` (one machine).
2. **The extra chain CPU is contention.** FY12 chain alone on a quiet machine: 267.87 CPU s against 266.85 serial (+0.4%), against 349.29 (run 3) and 357.69 (run 4). Same code, same output bytes. Status: `verified` for FY12 only.
3. **Prep CPU scales with volume; chain CPU does not.** Baseline, per year over 14 years: prep CPU against merged SJE rows r = 0.98 (about 30 CPU s per million rows, flat); chain CPU against SJE rows r = 0.35 (24 CPU s per million rows in FY03 to 14 in FY12). Chain CPU follows ledger rows better (r = 0.59), still loosely. Status: `inferred` from 14 points; the cause of the falling chain rate is not tested.
4. **GL has fewer rows than the ledger, by a stable ratio once purchase orders are present.** GL keys are about 0.58 of ledger rows in FY06-08 and FY10-16 (0.74-0.81 in FY03-05 and FY09). 10,651,908 ledger rows, 6,433,714 GL keys in total.
5. **Permutation capacity is bimodal, not smooth.** 10^8 to 10^9 in FY03-05 and FY09; 10^12 to 10^14 in FY06-08 and FY10-16. Sum 503,118,463,033,110 (upper bound). The jump coincides with years that carry purchase order data; that link is `inferred`.

## 2. What each Paper 1 graph can show today

| Graph | Needs | Have | Missing |
|---|---|---|---|
| **Process** (CCP1 to CCP5) | A cost point per layer, three replicates | CCP1 and CCP2 per-year rows in all runs; CCP3 `5v` pass (1,285 s baseline, 1,696 s run 4) | CCP4 and CCP5 on the corpus; replicates; a cost model that is not elapsed seconds (`total_compute_s` is inflated by contention and must not be compared across layouts) |
| **Mix** (events to balances) | Left wall (event replay), bowl minimum (ledger), right wall (GL plus sub-ledgers) | Ledger and GL row counts and bytes; permutation capacity as the right-wall sizing measure | Any measured query cost on the left wall; any measured cost of M sub-ledgers. The bowl itself is a hypothesis, drawn only from the design formulas |
| **Engine** (naive to CKB) | A second substrate running the same logic | One curve: Scala CKB on the laptop | Track D (conventional engine) has no corpus run; its figures are projections |

Consequence: the three bowls cannot be plotted from data yet. `COST_GRAPH_DESIGN.md` now holds a Mix-graph model run (measured corpus constants, assumed unit rates and query volumes; minimum at the monthly ledger, $16.29 a year against $1,830 for event replay and $9,018 for GL plus five sub-ledgers). That curve is model output. The Engine and Process graphs have no model run and no second data point yet. What can be plotted honestly from data is the measured behaviour of the CKB engine itself (charts in the session): CPU per million rows, ledger against GL rows, permutation capacity by year, contention and layout effects.

## 3. Cautions for anyone reading the charts

- Compare CPU seconds and total wall time, not per-step wall time or `total_cost_proxy`.
- 54% of chain wall time in run 3 came after "Pipeline complete" (control passes in Bash). Timing test 1 on FY12 put about 396 s of a 449 s tail in the `sje_balance` awk. The tail is outside every step metric.
- Peak process memory 1.36 GB with a 1 GB heap setting; memory is recorded, not yet charted.
- Permutation capacity is an upper bound.

## 4. Paper 2: three-model experiment

| Step | State |
|---|---|
| 1 Plan, decisions D1-D8 | Done (agent judgment, owner-delegated). |
| 2 Verbalizer, 12 tests | Done; accepts the real Gold headers for FY03, FY09, FY16. |
| 3 Labels, splits, metrics, results writer | Done on fixtures; 40 tests pass. |
| 4 Gold data checks | Done: 104 of 104 SHA-256; GL and LDGR agree 14 of 14 years on the expenditure basis; vendor-years equal the `5v` view. |
| 5 IL-Native | Not started. |
| 6 Model choice | Not started. Needs an Apache 2.0 base model, with version and advisories checked first. |
| 7 Fine-tune, score, results file | Not run. No model has been trained. |

Blocked on the owner: **D9, net income.** The Gold data holds only `EXP*`, `COMMIT` and offset accounts, so revenue minus expense cannot be derived. Proposed: net expenditure per agency and fiscal year. Also open: whether account text stays in the verbalized records, and whether the period text (`0016/01`, `2015/07`) is normalised.

AIM1, AIM3, AIM4 results remain provisional until re-run on the final views.

## 5. Next actions, in order

1. Owner: decide D9 and the verbalizer visibility question (unblocks steps 5 to 7).
2. Owner: decide whether to commission the replicate runs and which layout is the reference (unblocks any published cost point).
3. Decide who runs CCP4 and CCP5 on the corpus; today they are fixture-scale only.
4. Track D corpus run on the other machine; then the Engine graph has a second curve.
5. Re-run AIM1, AIM3, AIM4 on the final views.
