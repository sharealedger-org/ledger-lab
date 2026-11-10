# Track C: Retail Banking Benchmark (Berka PKDD'99 Corpus)
## Paper 2: Business Event Repositories as Training Corpora for Enterprise AI

> **Decision 2026-11-10 (owner): Track C is illustrative only, not evidence for Paper 2.** First limitation: **lineage unknown.** Berka is a public extract and nothing documents how the files were produced from the originating bank's systems. We cannot tell whether the per-row balance came from the bank's system or was computed when the challenge file was built; whether the 1.3% balance breaks and the 355 unreconcilable days are system behaviour or extract artefacts; how loan status was assigned; or how accounts and counterparties were selected and masked. The result therefore shows what a model can learn from this table, not what a bank's posting system keeps or discards, which is what Theorem 1 is about. The design, code and numbers below are kept as a worked example of the audit method and as a reproducible exploratory result. Do not cite them as support for Theorem 1. Stronger evidence needs a source whose path from originating system to ledger is known.

**Status:** v2 protocol implemented and run once on 2026-11-10 (see §6). The v1 design (monthly "compressed" balances, GBDT / sequence model) is withdrawn; see the 2026-11-10 sections of [`VERIFICATION_AUDIT_REGISTER.md`](VERIFICATION_AUDIT_REGISTER.md) for why.
**Corpus:** Berka Financial Dataset (PKDD'99 Discovery Challenge), fetched by `scripts/download_berka.sh` (terms of use unconfirmed; data stays outside the repository).
**Role in Monograph:** None as evidence (see the decision above). At most a footnote-level illustration labelled "public extract of unknown lineage, exploratory". Not an engine measurement.

---

## 1. Question

Theorem 1 says reducing business events to balances can only lose information about outcomes. Track C asks it in a retail-bank setting with a ground-truth outcome:

> Does a model that sees a bank account's transactions predict loan default better than a model that sees only that account's daily balance, using the same loans, the same pre-loan window, the same folds and the same learner?

Nothing in the design depends on the balance column being "posted state". In the Berka file every transaction row carries the running balance after it. A product system holds an end-of-day balance per account, and that is the reduced view tested here.

## 2. Two views of one set of rows

Both views are built from the same pre-loan rows of `trans.asc.txt`. There is no second dataset.

| View | Built from | Keeps | Drops |
|---|---|---|---|
| **E** (event view) | the transaction rows as recorded | everything: `type`, `operation`, `k_symbol`, `amount`, partner `bank` / `account`, date, intra-day multiplicity | nothing |
| **B** (product-system view) | E rolled up to account-day | one closing balance per account per day, carried forward on days with no activity; the three loan terms | intra-day sequence; operation; purpose code; counterparty; amounts of individual transactions (only day-level net movement survives) |

**E is a strict superset of B by construction.** `build_berka_substrates.py` computes B's columns from the daily series and then adds event-derived columns to produce E; an assertion checks that every B column is in E. Both use the same history-length scaling (`*_per30` = per 30 days of observed history).

```mermaid
flowchart LR
    T["trans.asc.txt<br/>pre-loan rows<br/>(date &lt; loan date)"] --> E["E: event view<br/>all columns"]
    T --> D["daily closing balance<br/>per account-day"]
    D --> B["B: product-system view<br/>balance statistics + loan terms"]
    B --> E
    L["loan.asc.txt<br/>status B,D = default"] --> Y["label Y"]
    B --> M1["logit on B"]
    E --> M2["logit on E"]
    M1 --> V["paired comparison<br/>shared folds, bootstrap CI"]
    M2 --> V
    Y --> V
```

### 2.1 Daily closing balance
The file gives no usable intra-day order (`trans_id` does not follow it). The closing balance of an account-day is the balance of the row that ends the day, recovered from the balance chain: row *j* may follow row *i* if `balance[j] - balance[i] = signed_amount[j]` in integer cents (`PRIJEM` = credit, `VYDAJ` and `VYBER` = debits). Results over the 42,671 pre-loan account-days: 32,590 single-row; 9,725 with a unique recovered order; 355 with no consistent order and 1 with an ambiguous close (both use the last row in file order). The cause of the 355 is not examined; the source balance chain is known to break on `UROK` interest and `UVER` loan-payment rows (1.3% of unambiguous transitions), which are candidates.

### 2.2 Why a running balance on every row is not "posting"
Storing a balance per transaction adds nothing to processing or storage: it is a running sum of the signed amounts. It is a statement field. The reduction that Theorem 1 is about is the roll-up from transactions to a balance at a stated grain. Daily is the grain a product system keeps, and it preserves almost all of the overdraft signal (26 of 76 defaults flagged by a negative daily close, against 27 by any per-transaction balance and 14 by month-end closings), so B is not handicapped by a contrived grain. A GL view was considered and rejected: nobody would query a general ledger for a borrower's default risk.

## 3. Target and window
* `loan.asc.txt`: 682 loans, one per account. `Y = 1` for status B or D (76), `Y = 0` for A or C (606).
* Only transactions strictly before the loan date feed either view.
* District demographics are not used (the 1996 columns post-date loans granted 1993-95). `order.asc.txt` is not used (undated; every loan has its own repayment order, which would leak the outcome). Card issuance carried no signal (AUC 0.53) and is not used.

## 4. Features (all counts and amounts per 30 days of history unless a ratio)
* **B (15 columns):** `loan_amount`, `duration`, `payments`, `history_days`; daily-series `bal_mean`, `bal_min`, `bal_max`, `bal_std`, `bal_last`, `bal_last30_mean`, `days_negative`, `days_below_5000`; day-to-day movement `credit_per30`, `debit_per30`, `changed_days_per30`.
* **E adds 22 columns** that a daily balance cannot express: transaction rate and multi-transaction-day share; counts and amounts by `operation` (cash withdrawal, card withdrawal, cash deposit, transfers in and out); counts by `k_symbol` (`SIPO`, `SLUZBY`, `POJISTNE`, `DUCHOD`, `SANKC. UROK`); cash drain ratio; mean gap between transactions; distinct counterparty accounts and banks, counterparty transaction rate and share.

## 5. Protocol (fixed before the first run)
* **Learner:** L2 logistic regression solved by Newton iterations, unweighted classes, `sign(x)·log1p(|x|)` then standardisation inside each training fold. Primary penalty λ = 10. λ = 1 and λ = 100 are reported as sensitivity only. One learner for B and E; no tuning on results.
* **Folds:** stratified 5-fold repeated 20 times (seeds 0-19), identical for B and E.
* **Headline:** ROC-AUC of out-of-fold scores averaged over the 20 repeats, per loan. PR-AUC, Brier and log-loss are also stored, with the constant-prevalence Brier (0.0990) and log-loss (0.3495) as references.
* **Interval:** 2,000 stratified bootstrap resamples of loans, paired between B and E. This reflects loan-sampling uncertainty for the fixed procedure; it does not refit per resample.
* **Baselines:** loan terms only; the overdraft rule (any negative daily close before the loan).
* **Subsets:** all 682 loans, and the 656 with no pre-loan overdraft (50 defaults), because the overdraft rule already separates 26 defaults with no false positives.
* **Test 2 (non-injectivity, redefined):** for each default, find its five nearest non-defaults in balance-profile space (standardised sign-log `bal_mean`, `bal_min`, `bal_std`, `bal_last`, `bal_last30_mean`; no distance cutoff) and compare the default's score with the mean score of those neighbours, for B and for E. The unit is the default, not the pair.
* **Post-hoc arms** (added after the first run, labelled as such in the code and evidence): (a) E without counterparty columns, B plus counterparty columns only, and the same for `SANKC. UROK`; (b) **events only**: loan terms, `history_days` and the 22 event columns, with no balance-derived column, to test whether balances or events carry more on their own.

## 6. Result of the single v2 run (2026-11-10)

| | All 682 loans (76 defaults) | No pre-loan overdraft (656 loans, 50 defaults) |
|---|---|---|
| Loan terms only, ROC-AUC | 0.653 [0.587, 0.719] | 0.633 [0.547, 0.721] |
| **B** (daily balance), ROC-AUC | 0.864 [0.812, 0.911] | 0.790 [0.716, 0.860] |
| **E** (events), ROC-AUC | 0.898 [0.857, 0.937] | 0.837 [0.774, 0.893] |
| **E − B**, ROC-AUC (paired 95%) | **+0.034 [+0.005, +0.064]** | **+0.047 [+0.004, +0.087]** |
| E − B, PR-AUC (paired 95%) | +0.069 [+0.020, +0.122] | +0.085 [+0.002, +0.179] |
| Overdraft rule | flags 26 defaults, 0 non-defaults | not applicable |
| Test 2: score gap default vs 5 nearest non-defaults, E − B | +0.043 [+0.016, +0.070] | +0.067 [+0.034, +0.100] |
| Same, win rate B / E | 0.795 / 0.845 | 0.676 / 0.740 |

Penalty sensitivity (E − B, ROC-AUC): λ = 1: +0.037 / +0.061; λ = 100: +0.020 / +0.023 (all loans / overdraft-free). Brier and log-loss of both models are below the constant-prevalence reference (B 0.0590, E 0.0540 vs 0.0990).

Post-hoc ablation, all loans: B plus counterparty columns only 0.892 (+0.028 over B, CI [+0.007, +0.053]); E without counterparty columns 0.887 (+0.024, CI [−0.004, +0.051]); B plus `SANKC. UROK` only +0.001 (CI [−0.003, +0.004]). The counterparty columns carry most of the gain; penalty interest carries none once the overdraft is in B.

Post-hoc events-only arm (no balance-derived column):

| ROC-AUC | All 682 loans | No pre-loan overdraft |
|---|---|---|
| Events only | 0.864 | 0.805 |
| B (daily balance) | 0.864 | 0.790 |
| E (balance plus events) | 0.898 | 0.837 |
| B − events only (paired 95%) | −0.000 [−0.048, +0.048] | −0.015 [−0.089, +0.066] |
| E − events only (paired 95%) | +0.034 [+0.007, +0.063] | +0.032 [−0.011, +0.077] |

A daily balance and the event features are statistically tied as stand-alone views; the combination is ahead of the events-only view on all loans and inconclusive on the overdraft-free set.

## 7. What can and cannot be claimed
* The sign of E − B is consistently positive across both subsets, all three penalties and both tests, and the paired intervals exclude zero. The size is modest: about three to five AUC points on 76 (or 50) defaults.
* The intervals are conditional on the fixed procedure and on this one corpus; the bootstrap does not reflect model-selection variance and the 20 repeats are not independent samples. Do not read them as a population-level 95% claim.
* **Balance is not redundant, and events are not obsolete.** Events alone (aggregate features, no balance column) match the daily balance alone, and neither dominates the combination. The supported reading is that a ledger should keep the daily balance and retain the events, not choose one. It does not show that a balance-led store beats an event stream: the event view here is aggregates, not a sequence model, so "tied" applies to this feature design; the events-only arm includes `SANKC. UROK` counts, which partly proxy the overdraft.
* Most of the gain comes from counterparty diversity (fewer distinct partner accounts among defaults). This is an event attribute a daily balance cannot hold, which is what the experiment targets, but it may also proxy for account maturity, household type or a customer segment. History length and transaction rate are in both views; independence from other account traits is not shown.
* Berka is one 1990s Czech retail dataset, 76 defaults, and the label is partly a function of the balance path after the observation window. The result is evidence in one domain, not a general proof of Theorem 1.
* The post-hoc ablation and the events-only arm were chosen after seeing the first result and are exploratory.
* Intra-day order was recovered for 9,725 of the 10,081 multi-row days; 356 days use a fallback.

## 8. Artifacts
* **Download (Bash):** `scripts/download_berka.sh`
* **Substrate builder:** `scripts/berka/build_berka_substrates.py` → `substrate_b_daily_balance.csv`, `substrate_e_events.csv`, `daily_balance_series.csv`, `build_stats.json` in `data/berka_processed/` (git-ignored)
* **Fit and evaluation:** `scripts/berka/fit_track_c_models.py`
* **Runner:** `scripts/run_track_c_berka.sh` (needs `BERKA_DATA_ROOT`)
* **Evidence:** `data/test_evidence/TRACK_C_BERKA_EVIDENCE.json`
* **Python status:** Track C Python is owner-approved (2026-11-10), scope Track C only; it is not an engine or cost-curve measurement.
