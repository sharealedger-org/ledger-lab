# Paper 2: evidence and training-design assessment

2026-11-12 22:37:06 AZT (UTC−07:00). CKB-side Codex. Reviewed current B2 status, the local supplied Paper 2 excerpts, three-model plan and test design, result CSVs, and relevant analysis code at HEAD `a336e17`. No training or evidence rerun; no Python written. The existing command `python3 -m unittest scripts/test_three_model_core.py` passed 28 tests. This is an assessment for discussion, not a replacement for Track B's results.

## Conclusion

The VA work supports a narrower, more useful statement than either “the premise is established” or “the data has no signal.” Posting omits detail; the commitment features predict next-year procurement presence; a combined model reports a small incremental gain over an instrument-ledger baseline. Neither a broad advantage for event-trained AI nor the proposed language-prior explanation is established. A newly identified fiscal-period discrepancy needs resolution before relying on the baseline/combined comparison.

The planned three-model training experiment will not automatically make the result clearer. As specified, it changes information, representation, model family, learning objective, and sample composition. It can compare practical systems, but cannot attribute every score difference to the single causes assigned to its gaps.

## What the evidence currently says

| Evidence | Recorded result | What it can establish / limit |
|---|---|---|
| AIM1 | Non-zero differences in all 14 years; `partially_verified`. | Supports inspecting lost distinctions, not a predictive advantage. `fit_noninjectivity.py` counts commitment/account patterns per instrument on the LDGR side and vendors per GL key on the PERM side. Subtracting those counts is not a common-unit measure of information lost. A direct collision witness—same posted representation, different relevant event histories—is clearer. |
| AIM3 commitment features | Hold-out C=0.7527; walk-forward mean 0.7629. | Above-chance discrimination for presence next year in the procurement view. Not business failure, not causal evidence, and not an incremental comparison with balances. |
| AIM3 balance features | Matched-split C=0.7683 versus commitment-only 0.7527. | Commitment features alone do not win this comparison. This is vendor-level LDGR, not vendor-free GL; it is also not a payment-only baseline. |
| AIM3 combined | C=0.7761, delta +0.0078; reported row-bootstrap interval [+0.0066,+0.0091]. | Candidate incremental predictive evidence on one split. Inspect period mapping, repeat by time window, and account for repeated vendors before treating the interval as robust. Utility at an operational threshold is not measured. |
| AIM4 | Two of five classes significant, FAIL under the executed rule. | Negative evidence for that Q4-ratio test. It is not the locked STL/majority-of-agencies test; running that method would be protocol completion, not permission to search for a pass. |
| Three-model training | No model trained; data/scoring preparations exist. | No measured LLM-versus-structured-model conclusion yet. |

Sources: `data/test_evidence/AIM1_noninjectivity_all_years.csv`, `AIM3_survival_windows.csv`, `AIM3_balance_baseline.csv`, `AIM3_combined_model.csv`, `AIM4_seasonality_results.csv`; `tracks/B2-models/STATUS.md`.

## Concrete cautions found in code

**Fiscal-year assignment differs.** `three_model_core.fiscal_year_of` treats `00YY/MM` payment periods as an already-labelled fiscal year, with no July shift. The Gold audit records why; its existing test asserts `0003/07 → 2003` and passed in this review. Both `fit_vendor_survival_balance.py` and `fit_vendor_survival_combined.py` instead add one after July even for that format, implying `0003/07 → 2004`. This is a source-confirmed discrepancy, not a measured estimate of score impact. The July rule for PO calendar dates remains distinct. Ask Track B to resolve this and rerun affected comparisons before accepting their magnitude; do not silently overwrite prior evidence.

**The baseline already contains more than ordinary GL balances.** Both scripts read vendor-keyed LDGR rows without filtering out commitment accounts. Their totals, row counts, account counts, and period counts may encode commitment-derived information. That is a legitimate baseline if named accurately: the experiment asks what extra view features add beyond those LDGR summaries. It does not directly compare event information against a traditional payment-only GL.

**The reported p-value is not a test of incremental gain.** `p_comb` permutes labels against the combined model's fixed scores. The delta is assessed by the paired bootstrap of hold-out rows. Multiple vendor-years can belong to the same vendor; the bootstrap does not preserve vendor clusters, and a single temporal split does not estimate stability across regimes. These limitations do not prove the gain disappears; they limit the precision claim.

**Feature definitions should match their labels.** The combined script uses the maximum monthly agency count and distinct commodity-class count, not annual distinct unions. This may still be predictive, but should be described as such. A comparison must freeze the exact feature computation, not just its friendly label.

## Why the training comparison needs sharpening

The plan calls Posted-FineTune → IL-FineTune a substrate gap, IL-FineTune → IL-Native a language-prior gap, and Posted-FineTune → IL-Native a total-cost gap. These interpretations exceed the specified controls:

- IL-FineTune receives LDGR verbalizations; IL-Native receives LDGR plus four views. The information available is not identical.
- A pretrained LLM versus a structured classifier changes architecture, features, objective, optimization, and prior together. A matched-pretraining ablation would be needed to isolate pretraining; that may be impractical here. A pragmatic model comparison can still be useful if labelled accordingly.
- Roughly equal sampled row counts do not align source populations or token/training budgets. GL-key sampling and vendor sampling select different units. The inference contract must also specify what test-year history each model receives, context limits, and whether it can use tools; the plan does not yet fix a complete protocol.
- Vendor survival is explicitly `not_answerable` for vendor-free GL. That is a structural capability result, not a model-score contest requiring expensive fine-tuning.
- Net expenditure is an exactly computable rule-based target, with account labels already present in GL and LDGR. A language model's arithmetic failure would not prove that the substrate lacks the information. Keep deterministic answerability and prediction separate.
- Training/evaluation is explicitly untimed in this plan. Accuracy differences cannot establish monetary or total maintenance/use cost. Financial-pipeline costs remain a different evidence stream.

## What would make the next evidence more decisive

1. **Close the small validity issue first.** Resolve fiscal periods, identify the precise baseline population/features, and preserve old versus corrected runs. No expensive training is needed for this step.
2. **Test incremental information with a controlled learner.** Compare baseline summaries versus those same summaries plus event-derived features on identical rows and cutoffs, using the same model family and selection budget. Use temporal validation, paired vendor-aware uncertainty, and a prespecified practically useful improvement. Distinguish rolling evaluation of existing vendors from generalization to unseen vendors.
3. **Keep structural witnesses separate.** A matched pair of histories with identical balances but different order/timing-dependent answers can demonstrate non-recoverability for that question. A synthetic witness demonstrates possibility, not real-world prevalence or predictive value.
4. **Run LLM training only for an additional question.** For example, can textual versus structured access exploit the same retained information under a fixed task and resource envelope? Call it a practical representation/model comparison unless a genuine prior ablation is supplied. Freeze the data, task, inference, and scoring contracts first.
5. **Use Phase 2 financial changes to test temporal fidelity.** Reclass and changed rules can test whether training snapshots preserve economic activity and exclude later knowledge. Keep source events distinct from generated adjustments. This is valuable engineering evidence even if it does not improve prediction.

I would not choose new targets merely because the current target yields a small or negative effect. Identify a real decision for which information plausibly discarded by posting matters, record the mechanism and acceptance rule before evaluation, and accept a null result. The supplied Paper 2's information argument explains a possible ceiling: it does not guarantee that every omitted detail adds target-relevant information or that a finite trained model will exploit it. Broad superiority needs empirical support beyond irreversibility alone.
