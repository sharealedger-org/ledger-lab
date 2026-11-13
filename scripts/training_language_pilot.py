#!/usr/bin/env python3
"""Post-engine field-label pilot; see docs/TRAINING_LANGUAGE_PILOT.md.

Owner approval: three-model Python training/evaluation scope, 2026-11-11;
field-label pilot explicitly authorized 2026-11-13. Could Scala do this?
Financial aggregation belongs in Scala; this script performs none. It selects
existing view rows, constructs research labels, fits models and scores them.
"""
import argparse
import csv
import hashlib
import importlib.metadata
import json
import math
import os
from pathlib import Path
import random
import time
from collections import defaultdict

from three_model_core import fiscal_year_of, last_complete_fy, observable, parse_fy_spec

FIELDS = ('po_count', 'agency_count', 'distinct_nigp_classes', 'total_amount')
LABELS = {
    'meaningful': ('order count', 'agency count', 'commodity class count', 'total amount'),
    'opaque': ('field A', 'field B', 'field C', 'field D'),
    'mismatched': ('agency count', 'commodity class count', 'total amount', 'order count'),
}
MODEL = 'HuggingFaceTB/SmolLM2-135M'


def digest(path):
    with open(path, 'rb') as handle:
        return hashlib.file_digest(handle, 'sha256').hexdigest()


def write_json(path, obj):
    path.write_text(json.dumps(obj, indent=2, sort_keys=True) + '\n')


def prompt(row, condition):
    values = '; '.join(f'{label}: {value}' for label, value in zip(LABELS[condition], row['values']))
    return ('Predict whether this vendor appears in next fiscal year. '
            'Latest observed month: ' + values + '.\n'
            'Answer 0 for absent or 1 for present. Answer:')


def prepare(gold, out):
    relative = 'VIEWS/view_vendor_year_survival.csv'
    source = gold / relative
    with (gold / 'MANIFEST.csv').open() as handle:
        matches = [r for r in csv.DictReader(handle) if r['rel_path'] == relative]
    assert len(matches) == 1, 'Missing or duplicate manifest entry'
    source_hash = digest(source)
    assert source_hash == matches[0]['sha256'], 'Gold hash mismatch'
    latest, months, seen = {}, defaultdict(set), set()
    with source.open(newline='', encoding='utf-8-sig') as handle:
        for line, r in enumerate(csv.DictReader(handle), 2):
            period, vendor = r['fiscal_year'], r['vendor_id']
            fy = fiscal_year_of(period)
            assert not period.startswith('00'), 'Unexpected payment period in PO view'
            assert (vendor, period) not in seen, 'Duplicate vendor/period'
            seen.add((vendor, period))
            months[fy].add(period)
            key = (vendor, fy)
            values = [r[f] for f in FIELDS]
            assert all(math.isfinite(float(v)) for v in values), 'Nonfinite feature'
            if key not in latest or period > latest[key]['period']:
                latest[key] = dict(vendor=vendor, fy=fy, period=period,
                                   values=values, source_line=line)
    complete = last_complete_fy(months)
    assert complete == 2016, f'Unexpected last complete FY {complete}'
    eligible = parse_fy_spec('2006-2008,2010-2017')
    present = set(latest)
    pools = defaultdict(list)
    for (vendor, fy), row in latest.items():
        if not observable(fy, eligible, complete):
            continue
        split = 'train' if 2010 <= fy <= 2012 else 'validation' if fy == 2013 else 'test' if fy in (2014, 2015) else None
        if split:
            row = dict(row, target=int((vendor, fy + 1) in present), split=split)
            pools[split].append(row)
    selected = []
    counts = {}
    for split, size in [('train', 512), ('validation', 128), ('test', 256)]:
        ordered = sorted(pools[split], key=lambda r: hashlib.sha256(
            f"42|{r['vendor']}|{r['fy']}".encode()).hexdigest())
        chosen = ordered[:size]
        assert len(chosen) == size and {r['target'] for r in chosen} == {0, 1}
        counts[split] = dict(available=len(ordered), selected=len(chosen),
                             positives=sum(r['target'] for r in chosen),
                             unique_vendors=len({r['vendor'] for r in chosen}))
        selected.extend(chosen)
    write_json(out / 'samples.json', selected)
    meta = dict(source=str(source), source_sha256=source_hash, source_rows=len(seen),
                vendor_years=len(latest), last_complete_fy=complete, counts=counts,
                sample_sha256=digest(out / 'samples.json'), fields=FIELDS,
                limitation='Latest monthly row only; not annual AIM3 features')
    write_json(out / 'input_manifest.json', meta)
    print(json.dumps(meta), flush=True)


def train(out, cache):
    import numpy as np
    import torch
    from huggingface_hub import HfApi, snapshot_download
    from transformers import AutoModelForCausalLM, AutoTokenizer
    from peft import LoraConfig, get_peft_model
    from sklearn.metrics import roc_auc_score, log_loss, brier_score_loss
    from sklearn.pipeline import make_pipeline
    from sklearn.preprocessing import StandardScaler
    from sklearn.linear_model import LogisticRegression

    rows = json.loads((out / 'samples.json').read_text())
    manifest = json.loads((out / 'input_manifest.json').read_text())
    assert digest(out / 'samples.json') == manifest['sample_sha256']
    splits = {s: [r for r in rows if r['split'] == s] for s in ('train', 'validation', 'test')}
    revision = HfApi().model_info(MODEL).sha
    model_path = snapshot_download(MODEL, revision=revision, cache_dir=cache,
                                  allow_patterns=['*.json', '*.safetensors', '*.txt', '*.model'])
    tokenizer = AutoTokenizer.from_pretrained(model_path, trust_remote_code=False)
    tokenizer.pad_token = tokenizer.eos_token
    tokenizer.padding_side = 'right'
    answer_ids = [tokenizer.encode(str(i), add_special_tokens=False) for i in (0, 1)]
    assert all(len(ids) == 1 for ids in answer_ids), answer_ids
    answer_ids = [ids[0] for ids in answer_ids]
    device = 'mps' if torch.backends.mps.is_available() else 'cpu'
    torch.set_num_threads(4)
    tokens = {}
    token_counts = {}
    for condition in LABELS:
        tokens[condition] = {}
        token_counts[condition] = {}
        for split, subset in splits.items():
            encoded = tokenizer([prompt(r, condition) for r in subset], add_special_tokens=True)
            lengths = [len(ids) for ids in encoded['input_ids']]
            assert max(lengths) <= 256, 'No silent truncation allowed'
            tokens[condition][split] = encoded['input_ids']
            token_counts[condition][split] = dict(total=sum(lengths), maximum=max(lengths))

    def metrics(y, scores):
        return dict(auc=float(roc_auc_score(y, scores)),
                    log_loss=float(log_loss(y, scores, labels=[0, 1])),
                    brier=float(brier_score_loss(y, scores)))

    def batch(ids, indices):
        return tokenizer.pad({'input_ids': [ids[i] for i in indices]},
                             padding=True, return_tensors='pt').to(device)

    def logits(model, inputs):
        result = model(**inputs, use_cache=False).logits
        last = inputs['attention_mask'].sum(dim=1) - 1
        return result[torch.arange(len(last), device=device), last][:, answer_ids].float()

    def score(model, condition, split):
        model.eval()
        scores = []
        ids = tokens[condition][split]
        with torch.no_grad():
            for start in range(0, len(ids), 8):
                p = logits(model, batch(ids, list(range(start, min(start + 8, len(ids)))))).softmax(-1)
                scores.extend(p[:, 1].cpu().tolist())
        return scores

    versions = {p: importlib.metadata.version(p) for p in ('torch', 'transformers', 'peft', 'numpy', 'scikit-learn', 'huggingface-hub')}
    run = dict(model=MODEL, revision=revision, device=device, versions=versions,
               script_sha256=digest(__file__), sample_sha256=manifest['sample_sha256'],
               token_counts=token_counts, seeds=[42, 43, 44], epochs=2, batch_size=8,
               learning_rate=0.001, lora_rank=8, lora_alpha=16, results=[])
    write_json(out / 'run.json', run)
    y = {s: [r['target'] for r in subset] for s, subset in splits.items()}
    clf = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000))
    clf.fit([[float(v) for v in r['values']] for r in splits['train']], y['train'])
    numeric_scores = clf.predict_proba([[float(v) for v in r['values']] for r in splits['test']])[:, 1]
    run['numeric_baseline'] = metrics(y['test'], numeric_scores)
    predictions = []
    for condition in LABELS:
        for seed in (42, 43, 44):
            start_time = time.monotonic()
            random.seed(seed); np.random.seed(seed); torch.manual_seed(seed)
            base = AutoModelForCausalLM.from_pretrained(model_path, trust_remote_code=False,
                                                       use_safetensors=True, dtype=torch.float32)
            model = get_peft_model(base, LoraConfig(r=8, lora_alpha=16, lora_dropout=0.0,
                                                  target_modules=['q_proj', 'v_proj'], task_type='CAUSAL_LM')).to(device)
            initial = score(model, condition, 'test')
            optimizer = torch.optim.AdamW((p for p in model.parameters() if p.requires_grad), lr=0.001)
            epoch_losses = []
            for epoch in range(2):
                model.train()
                order = list(range(len(y['train'])))
                random.Random(seed + epoch).shuffle(order)
                losses = []
                for start in range(0, len(order), 8):
                    indices = order[start:start + 8]
                    optimizer.zero_grad()
                    pred = logits(model, batch(tokens[condition]['train'], indices))
                    loss = torch.nn.functional.cross_entropy(pred, torch.tensor([y['train'][i] for i in indices], device=device))
                    loss.backward()
                    torch.nn.utils.clip_grad_norm_(model.parameters(), 1.0)
                    optimizer.step()
                    losses.append(float(loss.detach().cpu()))
                epoch_losses.append(sum(losses) / len(losses))
                print(f'{condition} seed={seed} epoch={epoch + 1} loss={epoch_losses[-1]:.5f}', flush=True)
            final = score(model, condition, 'test')
            validation = score(model, condition, 'validation')
            result = dict(condition=condition, seed=seed, initial=metrics(y['test'], initial),
                          final=metrics(y['test'], final), validation=metrics(y['validation'], validation),
                          epoch_losses=epoch_losses, elapsed_seconds=time.monotonic() - start_time)
            run['results'].append(result)
            for row, before, after in zip(splits['test'], initial, final):
                predictions.append(dict(vendor=row['vendor'], fy=row['fy'], target=row['target'],
                                        condition=condition, seed=seed, initial=before, final=after))
            write_json(out / 'predictions.json', predictions)
            write_json(out / 'run.json', run)
            model.save_pretrained(Path(cache) / 'adapters' / f'{condition}-{seed}')
            print(json.dumps(result), flush=True)
            del model, base, optimizer
            if device == 'mps':
                torch.mps.empty_cache()
    # Mean paired AUROC differences across seeds; resample vendors, retaining their years.
    groups = defaultdict(list)
    for i, row in enumerate(splits['test']):
        groups[row['vendor']].append(i)
    vendors = sorted(groups)
    arrays = {(c, s): np.array([r['final'] for r in predictions if r['condition'] == c and r['seed'] == s])
              for c in LABELS for s in (42, 43, 44)}
    target = np.array(y['test'])
    differences = {}
    for condition in ('opaque', 'mismatched'):
        def delta(indices):
            return float(np.mean([roc_auc_score(target[indices], arrays[(condition, s)][indices]) -
                                  roc_auc_score(target[indices], arrays[('meaningful', s)][indices]) for s in (42, 43, 44)]))
        rng = np.random.default_rng(20261113)
        boot = []
        for _ in range(1000):
            indices = [i for v in rng.choice(vendors, len(vendors), replace=True) for i in groups[v]]
            if len(set(target[indices])) == 2:
                boot.append(delta(indices))
        differences[condition + '_minus_meaningful'] = dict(delta=delta(list(range(len(target)))),
            cluster_ci95=np.quantile(boot, [.025, .975]).tolist(), bootstrap_replicates=len(boot))
    run['paired_differences'] = differences
    run['status'] = 'completed_bounded_pilot'
    write_json(out / 'run.json', run)
    print(json.dumps(differences), flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['prepare', 'train'])
    parser.add_argument('--gold', type=Path)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--cache', default='/private/tmp/ckb-language-model-cache')
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    if args.action == 'prepare':
        assert args.gold is not None
        prepare(args.gold, args.out)
    else:
        train(args.out, args.cache)


if __name__ == '__main__':
    main()
