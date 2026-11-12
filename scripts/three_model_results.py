#!/usr/bin/env python3
"""
Three-model experiment -- results-file writer (plan step 3).

Scope (AGENTS.md): Python for post-engine evaluation only.  Owner approval received 2026-11-11.
Writes data/test_evidence/three_model_experiment_results.csv: one row per model, task and
metric, then gap rows computed from the model pairs.  Standard library only.

Gap rows (docs/TEST_DESIGN.md section 2):
  gap_1_2_substrate       Posted-FineTune -> IL-FineTune      (same language prior, better data)
  gap_2_3_language_prior  IL-FineTune     -> IL-Native        (same data, prior removed)
  gap_1_3_total           Posted-FineTune -> IL-Native        (total cost of the paradigm)
gap = value(second model) - value(first model).  The sign is NOT normalised: `better` says
whether a higher or a lower value is better for that metric, so the reader can interpret it.
If either model of a pair is not_answerable on that task, the gap row is not_answerable and has
no value (decision D7).
"""

import csv
import datetime
import os
import sys
from dataclasses import dataclass
from typing import Optional

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))   # sibling module, any working directory
from three_model_core import NOT_ANSWERABLE  # noqa: E402

MODELS = ("Posted-FineTune", "IL-FineTune", "IL-Native")
GAP_PAIRS = (
    ("gap_1_2_substrate", "Posted-FineTune", "IL-FineTune"),
    ("gap_2_3_language_prior", "IL-FineTune", "IL-Native"),
    ("gap_1_3_total", "Posted-FineTune", "IL-Native"),
)
BETTER = {
    "c_statistic": "higher",
    "permutation_p": "lower",
    "smape": "lower",
    "rmse": "lower",
    "net_income_correct": "higher",
}
SCORED = "scored"

COLUMNS = [
    "row_type", "task", "metric", "model", "value", "status", "better", "n_test", "split",
    "seed", "base_model", "library_versions", "note", "run_timestamp",
]


@dataclass
class Result:
    model: str
    task: str
    metric: str
    value: Optional[float]
    status: str = SCORED          # SCORED or NOT_ANSWERABLE
    n_test: Optional[int] = None
    note: str = ""


def gap_rows(results) -> list:
    """Gap Results for every (task, metric) where both models of a pair have a row."""
    index = {(r.task, r.metric, r.model): r for r in results}
    gaps = []
    for task, metric in sorted({(r.task, r.metric) for r in results}):
        for name, first, second in GAP_PAIRS:
            a, b = index.get((task, metric, first)), index.get((task, metric, second))
            if a is None or b is None:
                continue
            if a.status == NOT_ANSWERABLE or b.status == NOT_ANSWERABLE:
                gaps.append(Result(name, task, metric, None, NOT_ANSWERABLE, note=f"{first} -> {second}"))
            else:
                gaps.append(Result(name, task, metric, b.value - a.value, SCORED, note=f"{first} -> {second}"))
    return gaps


def write_results(path: str, results, split: str, seed: int, base_model: str,
                  library_versions: str, timestamp: str = "") -> int:
    """Write model rows then gap rows.  Returns the number of data rows written.  Refuses an
    unknown model or metric, a scored row with no value, and a not_answerable row with a value."""
    results = list(results)
    for r in results:
        if r.model not in MODELS:
            raise ValueError(f"unknown model {r.model!r}")
        if r.metric not in BETTER:
            raise ValueError(f"unknown metric {r.metric!r}")
        if r.status == SCORED and r.value is None:
            raise ValueError(f"{r.model}/{r.task}/{r.metric}: scored row has no value")
        if r.status == NOT_ANSWERABLE and r.value is not None:
            raise ValueError(f"{r.model}/{r.task}/{r.metric}: not_answerable row must not carry a value")
        if r.status not in (SCORED, NOT_ANSWERABLE):
            raise ValueError(f"unknown status {r.status!r}")
    stamp = timestamp or datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    rows = [("model", r) for r in results] + [("gap", g) for g in gap_rows(results)]
    if os.path.dirname(path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=COLUMNS)
        writer.writeheader()
        for row_type, r in rows:
            writer.writerow({
                "row_type": row_type, "task": r.task, "metric": r.metric, "model": r.model,
                "value": "" if r.value is None else repr(float(r.value)),
                "status": r.status, "better": BETTER[r.metric],
                "n_test": "" if r.n_test is None else r.n_test,
                "split": split, "seed": seed, "base_model": base_model,
                "library_versions": library_versions, "note": r.note, "run_timestamp": stamp,
            })
    return len(rows)
