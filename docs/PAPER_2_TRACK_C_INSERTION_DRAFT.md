# Paper 2 Insertion Draft: Track C Retail Banking Benchmark (Berka PKDD'99)

> **DO NOT INSERT INTO THE MONOGRAPH AS WRITTEN (2026-11-10).** An audit against the public data found that the claims below are not supported by the current Track C run. The numbers in this file do match `TRACK_C_BERKA_EVIDENCE.json` and reproduce exactly, but:
> - the balance side is **not** monthly closing balances: it is computed from per-transaction running balances (5.8 events per month-end closing), so there is no real compression;
> - the event feature set is not a superset of the balance set;
> - "zero look-ahead bias" is false for district demographics (1996 values joined to 1993-95 loans);
> - the Theorem 1 gain is within noise (ROC-AUC +0.0017, one seed, 76 defaults, no interval), and Brier/log-loss are worse than the base rate (0.0990 / 0.3495) for both models, so they are not calibration evidence;
> - the non-injectivity separation gain is -0.0016 (29 pairs; the 10 pairs sampled in the evidence file reuse only 6 defaulted accounts, and 2 of them fit the story while 3 run the other way), so Account 6400 / 8261 is not representative;
> - the default label is itself a function of the balance path: every default goes negative, no non-default does, and the pre-loan overdraft rule alone catches 27 of 76 defaults with no false positives.
>
> See the 2026-11-10 sections of `docs/VERIFICATION_AUDIT_REGISTER.md` and `SESSION_HANDOFF.md`. Rewrite the section around a month-end Layer 2, a strict-superset Layer 3 with event-only attributes, and the overdraft-rule baseline before reuse.

> **Note for Monograph Editing:**  
> This file contains the ready-to-integrate draft section and empirical evidence summary for **Paper 2 (*Business Event Repositories as Training Corpora for Enterprise AI*)** in the `org-members` monograph repository.
> Recommended insertion location: **`research/02-business-event-repositories.md` as Section 5.1b**, immediately following §5.1a (The Insurance Corpus).

---

## Draft Section for Paper 2 (§5.1b)

```markdown
### 5.1b Track C: The Retail Banking Corpus (Berka PKDD'99 Benchmark)

To test the cross-domain generalizability of Theorem 1 beyond public procurement (Track B) and property/casualty insurance (Track A), Track C implements an open retail banking benchmark using the Petr Berka PKDD'99 financial dataset (4,500 accounts, 1,056,320 transaction events, and 682 granted term loans spanning 1993–1998).

* **The Ground Truth Target ($Y$):** Loan default and credit distress ($Y \in \{0, 1\}$; 76 defaults among 682 loans, 11.14% base rate), evaluated strictly on transaction histories prior to loan issuance ($t < T_{\text{loan}}$) to guarantee zero look-ahead bias.
* **Substrate Comparison:**
  * *Substrate B (Layer 2 Balances):* Simulates standard general ledger / subledger summaries (monthly closing balances, rolling turnovers, min/max/mean balances, and district demographics).
  * *Substrate E (Layer 3 Event Stream):* Preserves atomic transaction tuples (cash vs. card withdrawal modes, purpose symbols including sanction interest, cash-drain velocity ratios, and inter-arrival intervals).
* **Empirical Confirmation:**
  1. *Information Bound (Theorem 1):* Stratified 5-fold cross-validation demonstrates a consistent positive information gain from Substrate E over Substrate B (PR-AUC 0.6021 vs. 0.5919; Brier calibration 0.1290 vs. 0.1381; Log-Loss 0.4103 vs. 0.4279).
  2. *Non-Injectivity (Path vs. State):* In 29 account pairs possessing near-identical monthly balance profiles ($B_1 \approx B_2$) but opposing default outcomes ($Y_1 \neq Y_2$), Layer 2 models generated inverted or unresolvable default probabilities. Layer 3 event sequence representations successfully separated the risk profiles (for example, resolving an inverted 53.7%/37.6% prediction in balance space to 47.2%/31.4% in event space).

This benchmark confirms that the information destruction formalised in Theorem 1 is an invariant mathematical property of balance posting across both public procurement and commercial banking domains.
```

---

## Summary Table of Empirical Evidence

| Metric / Dimension | Layer 2 (Balances: $\mathcal{B}$) | Layer 3 (Events: $\mathcal{E}^*$) | Empirical Lift / Delta | Theoretical Meaning in Paper 2 |
| :--- | :--- | :--- | :--- | :--- |
| **PR-AUC (Precision-Recall)** | 0.5919 | 0.6021 | **+0.0102** | Proves $I(\mathcal{E}^*; Y) > I(\mathcal{B}; Y)$ under severe class imbalance (11.1% default rate). |
| **ROC-AUC** | 0.8471 | 0.8488 | **+0.0017** | Global ranking improvement across all decision thresholds. |
| **Brier Score Calibration** | 0.1381 | 0.1290 | **-0.0091 (better)** | Event-level signals produce better-calibrated risk probabilities. |
| **Log-Loss** | 0.4279 | 0.4103 | **-0.0176 (better)** | Cross-entropy reduction from uncompressed event features. |
| **Ambiguous Balance Pairs** | 29 pairs | 29 pairs | **Resolved** | Non-injectivity proof: identical balances conceal opposite credit outcomes. |

---

## Detailed Non-Injectivity Evidence Sample

Below is an excerpt from `data/test_evidence/TRACK_C_BERKA_EVIDENCE.json` illustrating the pathology of balance-only reporting:

### Case Study: Account 6400 (Default) vs. Account 8261 (Good Credit)
* **Balance Distance:** $d(\mathcal{B}_1, \mathcal{B}_2) = 0.95$ (near-identical monthly closing balances and turnovers).
* **Layer 2 (Balances) Prediction:**
  * Account 6400 (Default): Predicted default probability = **37.6%**
  * Account 8261 (Good Credit): Predicted default probability = **53.7%**
  * *Result:* Inverted error. The balance-only model rated the defaulting customer as safer than the good customer.
* **Layer 3 (Pre-Hierarchy Events) Prediction:**
  * Account 6400 (Default): Predicted default probability = **47.2%**
  * Account 8261 (Good Credit): Predicted default probability = **31.4%**
  * *Result:* Correct risk hierarchy restored by inspecting cash-drain velocity and transaction arrival dynamics.

---

## Verification & Reproducibility in `ledger-lab`

* **Pipeline Script:** `scripts/run_track_c_berka.sh`
* **Evidence File:** `data/test_evidence/TRACK_C_BERKA_EVIDENCE.json`
* **Status in Audit Register:** Verified in `docs/VERIFICATION_AUDIT_REGISTER.md` (2026-11-09).
