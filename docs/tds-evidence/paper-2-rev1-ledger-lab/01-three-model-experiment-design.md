# Three-Model Experiment Design — AI Information Loss (Paper 2, Rev 1)

**Status:** Implementation Complete & Unit Tested (`verified` for Steps 1–4; `not run` for Steps 5–7)  
**Reference Document:** [`docs/THREE_MODEL_EXPERIMENT_PLAN.md`](../../THREE_MODEL_EXPERIMENT_PLAN.md)  
**Code Implementations:** [`scripts/three_model_core.py`](../../../scripts/three_model_core.py), [`scripts/three_model_verbalize.py`](../../../scripts/three_model_verbalize.py)  
**Test Suite:** [`scripts/test_three_model_core.py`](../../../scripts/test_three_model_core.py), [`scripts/test_three_model_verbalize.py`](../../../scripts/test_three_model_verbalize.py) (40 passing unit tests)  

---

## 1. Experimental Motivation: The Information Destruction Cascade

When enterprises train AI models on financial data, they typically extract tabular summaries from General Ledgers or data warehouses and verbalize them into natural language strings for LLM fine-tuning. This approach incurs two compounding failure modes:
1. **Representational Loss (Shannon Lossy Compression):** Posting collapses multi-dimensional business events into account-period balances, permanently discarding counterparty identity, event sequence, and contract terms.
2. **The "See Spot Run" Language Prior Trap:** A pretrained LLM carries an internet-trained natural language prior. When presented with verbalized business codes and entity names that coincidentally resemble English words or location fragments (e.g., Spotsylvania, Bull Run), the model forms spurious associations that are reproducible and stable across training runs, learning the linguistics of the verbalization template rather than the economics of the business events.

---

## 2. The Three-Model Matrix & Experimental Gaps

To isolate the substrate effect from the language prior effect, the experiment evaluates three models on identical downstream financial prediction tasks:

```
┌─────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Three-Model Comparison Framework                                                                        │
│                                                                                                         │
│  Model 1: [ Posted GL Data ] ──────────> [ Verbalizer ] ──> [ Pretrained LLM ] ──> Fine-tuned Model 1   │
│                                                                                                         │
│  Model 2: [ Instrument Ledger (LDGR) ] ─> [ Verbalizer ] ──> [ Pretrained LLM ] ──> Fine-tuned Model 2   │
│                                                                                                         │
│  Model 3: [ Instrument Ledger (LDGR) ] ────────────────────> [ Structured GBDT/MLP ] ─> Native Model 3  │
└─────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Model | Architecture | Training Substrate | Natural Language Prior | Target Measurement |
|---|---|---|---|---|
| **Model 1 (Posted-FineTune)** | Pretrained LLM (Apache 2.0) | Verbalized Posted General Ledger (`GL`) | Pretrained Language Prior Active | Industry Status Quo |
| **Model 2 (IL-FineTune)** | Identical Pretrained LLM | Verbalized Instrument Ledger (`LDGR`) | Pretrained Language Prior Active | Substrate Gain Baseline |
| **Model 3 (IL-Native)** | Structured Tabular / Sequence (GBDT/MLP) | Native Instrument Ledger (`LDGR`) | **Zero Language Prior** (Pure Events) | Language-Free Baseline |

### Formal Performance Gaps Measured
* **Substrate Gap ($\Delta_{1 \to 2} = \text{Score}_2 - \text{Score}_1$):** Quantifies what traditional General Ledger posting destroyed, holding model family and language prior constant.
* **Language Prior Penalty ($\Delta_{2 \to 3} = \text{Score}_3 - \text{Score}_2$):** Directly measures the "See Spot Run" contamination cost introduced by forcing structured financial records into natural language serializations.
* **Total Paradigm Gap ($\Delta_{1 \to 3} = \text{Score}_3 - \text{Score}_1$):** Total performance improvement achievable by transitioning from verbalized General Ledger summaries to native Instrument Ledger event modeling.

---

## 3. Methodological Decisions Locked (Decisions D1 to D8)

| Decision | Locked Specification | Justification | Status |
|---|---|---|---|
| **D1: Tasks** | Task A: Net Expenditure per Entity/Year; Task B: Vendor Churn; Task C: Spending Spike | Grounded in exact-cent audited data | `verified` |
| **D2: Scope** | Public Virginia Corpus FY2003–FY2016 (14 years) | Publicly verifiable, 224M SJE rows | `verified` |
| **D3: Base Model** | Apache 2.0 / MIT small open-weights LLM ($\le 3\text{B}$ params) | Fully reproducible on standard GPU/laptop | `verified` |
| **D4: Splits** | **Train:** FY2010–FY2013; **Test (Hold-out):** FY2014–FY2015 | Strict longitudinal separation | `verified` |
| **D5: Verbalizer** | Fixed deterministic YAML/Python template | Eliminates stochastic verbalization drift | `verified` |
| **D6: IL-Native** | LightGBM / XGBoost + TabNet baseline | Pure tabular/sequence execution without NLP | `verified` |
| **D7: Metrics** | R², RMSE, MAE (regression); AUC-ROC / C-statistic, F1 (classification) | Standardized statistical evaluation | `verified` |
| **D8: Sampling** | Fixed-seed deterministic entity hashing (1,000 entities) | Consistent evaluation cohort across all 3 models | `verified` |

---

## 4. Open Decision D9 (Net Income Ground Truth)

* **Issue:** The Gold reference dataset currently contains only expenditure lines (`EXP*`), purchase order commitments (`COMMIT`), and offset accounts (`0000`). While raw files contain 17.75M revenue events (`apa_revenue`), revenue has not yet passed through the Scala TTL.
* **Current Operational Scope:** The three models are evaluated on the exact-cent **audited expenditure basis** (`EXP*` debits), where GL and LDGR agree to the penny across all 14 fiscal years.
* **Phase 2 Expansion:** Decision D9 will select the multi-fund net income ground truth (net expenditure per agency vs. fund reserves vs. ACFR GAAP statement tie-out) once APA revenue is integrated.
