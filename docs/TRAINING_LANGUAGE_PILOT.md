# Training-language pilot: field labels

Authorized 2026-11-13: owner requested the test and explicitly selected the field-label scope after inspection found no vendor-name descriptions in the survival view. Status at 13:36:01 AZT: samples prepared and tested; training not run because the download/training execution permission was declined. This is a separate bounded pilot, not completion of the three-model experiment or D9.

## Claim and scope

Falsifiable question: with identical financial values, outcomes, examples, model initialization and training budget, does changing field-label meaning change held-out vendor-presence predictions, and does any difference persist after supervised adaptation?

Three fixed encodings: meaningful labels, opaque labels, and a cyclic permutation of the same meaningful labels. The third condition preserves the label vocabulary while deliberately mismatching its meaning. This tests sensitivity to field semantics and encoding, not whether all language priors are harmful or whether event-native training is superior. Opaque tokenization differs; record token counts. No vendor names are fabricated.

## Input contract and cheapest checks

Producer: Scala `CKBViews`, step 5v, writes Gold `VIEWS/view_vendor_year_survival.csv`. This is actually a vendor/period view. Consumer: pilot preparation reads committed view rows, constructs presence labels using the existing `three_model_core` fiscal-year and censoring contract, and selects bounded records for training. No source transactions, posting, financial aggregation, or engine changes.

Use the latest monthly row per vendor/fiscal year, with four existing values: `po_count`, `agency_count`, `distinct_nigp_classes`, `total_amount`. This deliberately bounded input is **not** the annual AIM3 feature set: it describes the latest observed month only. Preserve that limitation in every report. Do not sum monthly financial values in Python. Outcome: presence anywhere in the next observable fiscal year. Exclude the view's `present_next_year` and `first_order_year` fields from model input. Vendor ID is used for sampling, pairing and clustered intervals, never as a model feature.

Could this be done in Scala/GenevaERS? Financial aggregation belongs there; none is added. Row selection, labels, tokenization, model fitting and statistical scoring are post-engine experiment preparation covered by the existing three-model Python approval, now with owner-approved field-label scope. Training is outside all financial cost measurements.

Cheapest falsifiers: verify input SHA against Gold manifest; reject malformed periods and duplicate vendor/period rows; compare parsed year semantics against existing tests; check paired variants have identical numeric values, row identities and targets; assert temporal split separation and censoring. The existing core/verbalizer suite passed 40 tests on 2026-11-13. The payment-year discrepancy in balance/combined AIM3 scripts remains separate: this pilot consumes the PO view, whose full calendar dates use the July rule, and does not reuse payment-derived balances.

## Fixed pilot design (before model outcomes)

- Train FY2010–2012, validation FY2013, test FY2014–2015. Only outcomes observable through complete FY2016. No tuning on test results.
- Deterministic hash selection of at most 512 training, 128 validation and 256 test vendor-year examples. All conditions use the same examples. Record repeated-vendor counts; bootstrap paired test differences by vendor. This pilot's small sample cannot establish equivalence.
- Small base language model: `HuggingFaceTB/SmolLM2-135M`, Apache 2.0 per its [publisher model card](https://huggingface.co/HuggingFaceTB/SmolLM2-135M). Resolve and record the immutable revision before use. Safetensors only; no remote code. This is an explicit pilot deviation from the three-model plan's preference for a release within six months; compactness makes repeated local training feasible. It is not a newly released or frontier-model test.
- LoRA on query/value projections, rank 8, alpha 16, dropout 0; fixed base weights, paired seeds 42, 43, 44; AdamW learning rate 0.001; batch size 8; two epochs; final checkpoint. Same training-example ordering per seed across encodings. Fixed binary next-token objective; report conditional probability of answer `1` versus `0`.
- Score before training and after each condition's training. Primary metric AUROC; also log loss, Brier score and sample prevalence. Record training loss and validation metrics to expose underlearning. No adaptively selected winning hyperparameters.
- Report all conditions and seeds, paired differences, vendor-cluster bootstrap intervals, token counts, input/code/model hashes, package versions, device and elapsed time. Runtime is reproducibility metadata, not a CKB cost point.

An uninformative model, unstable seed results or wide intervals is an inconclusive pilot, not evidence against either architecture. Meaningful labels may help legitimately. A mismatch penalty can demonstrate sensitivity without demonstrating an irreducible language-prior defect. Random-initialization controls and event-native training remain later work.

## Preparation evidence and resume

`data/test_evidence/language_pilot_20261113/` contains the selected records, source/sample hashes, package versions and dependency audit (no known vulnerabilities after updating the temporary environment's pip). Selection produced 512/128/256 train/validation/test examples; 386/85/172 positives. All 256 test vendors are distinct. Core, verbalizer and pilot tests: 43 passed. The unmodified AIM3 script independently reproduced C=0.7527 and walk-forward mean 0.7629; evidence is `data/test_evidence/language_pilot_baseline_20261113.csv`. That annual-feature result is not the pilot's matched-input baseline.

The training implementation exists but is **not runtime verified**. No model weights have been downloaded by the pilot, no adapters trained, and no label-condition scores exist. Next check is model/tokenizer loading and a first forward/backward batch. The temporary environment and model cache are outside the repository and may need rebuilding after cleanup.

Resume only once execution permission is available:

```bash
/private/tmp/ckb-language-pilot-venv/bin/python -u scripts/training_language_pilot.py train --out data/test_evidence/language_pilot_20261113
```
