#!/usr/bin/env python3
"""
Three-model experiment -- labels, splits, training sample, targets and metrics (plan step 3).

Scope (AGENTS.md): Python for post-engine model preparation and evaluation only.  Owner approval
received 2026-11-11 for the three-model experiment's training and evaluation scripts, not timed.
This module is a library of small pure functions.  It reads no corpus file by itself, posts
nothing and aggregates only bounded per-agency totals needed as forecasting targets.  Standard
library only (numpy and scikit-learn are not installed on this machine).

Decisions implemented (docs/THREE_MODEL_EXPERIMENT_PLAN.md section 4):
  D3  vendor survival label and censoring rule = AIM3 (scripts/fit_vendor_survival_windows.py):
      a fiscal year t is observable only if t+1 is eligible AND t+1 <= last complete fiscal year;
      label = vendor present in t+1.  Anything else is censored and excluded, never churn.
  D4  split = AIM3 hold-out: outcome years 2015-2016, so train years 2010-2013 and test years
      2014-2015 when the eligible set is 2006-2008,2010-2017 and 2016 is the last complete year.
      Walk-forward windows (expanding, minimum 2 training years) are a stability check.
  D5  metrics: C-statistic with permutation p; sMAPE and RMSE with a naive last-year baseline;
      net income scored correct / incorrect / not_answerable.
  D7  "cannot answer" is the status NOT_ANSWERABLE, never a score of zero.
  D8  training sample (see sample_fraction / keep_entity below).

Fiscal year: a period is 'YYYY/MM' (purchase-order rows); Virginia fiscal year runs July-June
(month >= 7 belongs to the next calendar year).  Payment rows carry a file period '00YY/MM' whose
leading '00YY' is the fiscal-year label itself (Gold set, all 14 years: 0 mismatches against the
file's year; CKBViews.scala header line 50), so '00YY/MM' maps to 2000+YY with no July rule.  A bare
year, a two-digit period or any other shape is rejected with ValueError (plan control 3: the old
PO reader defect must not reach a model).
"""

import hashlib
import random
from decimal import Decimal, InvalidOperation

NOT_ANSWERABLE = "not_answerable"

# ---------------------------------------------------------------------------
# Fiscal year
# ---------------------------------------------------------------------------


def fiscal_year_of(period: str) -> int:
    """'YYYY/MM' -> Virginia fiscal year (July-June); '00YY/MM' (payment file period) -> 2000+YY.
    Anything else raises ValueError."""
    text = (period or "").strip()
    parts = text.split("/")
    if len(parts) != 2 or len(parts[0]) != 4 or len(parts[1]) != 2 or not (parts[0] + parts[1]).isdigit():
        raise ValueError(f"fiscal period must be YYYY/MM or 00YY/MM, got {period!r}")
    year, month = int(parts[0]), int(parts[1])
    if not 1 <= month <= 12:
        raise ValueError(f"fiscal period month out of range: {period!r}")
    if parts[0].startswith("00"):
        return 2000 + year
    return year + 1 if month >= 7 else year


def parse_fy_spec(spec: str) -> set:
    """'2006-2008,2010-2017' -> {2006, 2007, 2008, 2010, ..., 2017}."""
    out = set()
    for part in spec.split(","):
        lo, _, hi = part.strip().partition("-")
        out.update(range(int(lo), int(hi or lo) + 1))
    return out


def last_complete_fy(periods_by_fy: dict):
    """Latest fiscal year with all 12 distinct months; None if there is none.  Months are counted,
    not period strings, because one fiscal year mixes 'YYYY/MM' and '00YY/MM' strings."""
    complete = sorted(fy for fy, periods in periods_by_fy.items()
                      if len({p.split("/")[-1] for p in periods}) >= 12)
    return complete[-1] if complete else None


# ---------------------------------------------------------------------------
# D3 / D4: survival label, censoring and splits (AIM3 rule)
# ---------------------------------------------------------------------------


def observable(fy: int, eligible: set, last_complete: int) -> bool:
    return fy in eligible and (fy + 1) in eligible and (fy + 1) <= last_complete


def survival_panel(present: set, eligible: set, last_complete: int) -> list:
    """present = {(vendor_id, fiscal_year)}.  Returns sorted [(vendor_id, fy, label)], label 1 =
    the vendor is present in fy+1 (survived), 0 = absent (churned).  Censored years are omitted."""
    return sorted(
        (vendor, fy, 1 if (vendor, fy + 1) in present else 0)
        for vendor, fy in present
        if observable(fy, eligible, last_complete)
    )


def longest_run(years) -> list:
    """Longest run of consecutive years in `years`; on a tie the later run."""
    runs, current = [], []
    for fy in sorted(set(years)):
        if current and fy == current[-1] + 1:
            current.append(fy)
        else:
            if current:
                runs.append(current)
            current = [fy]
    if current:
        runs.append(current)
    return max(runs, key=lambda r: (len(r), r[-1])) if runs else []


def holdout_split(run: list, holdout_outcome_fys: set):
    """(train_years, test_years): test years have their outcome year in the hold-out set; train
    years have an outcome year before the hold-out starts."""
    first = min(holdout_outcome_fys)
    test = sorted(t for t in run if (t + 1) in holdout_outcome_fys)
    train = [t for t in run if t + 1 < first]
    return train, test


def walk_forward_windows(run: list, min_train: int) -> list:
    """Expanding windows inside the run: [(train_years, test_year)]."""
    return [(run[:k], run[k]) for k in range(min_train, len(run))]


# ---------------------------------------------------------------------------
# D8: training sample (fixed seed, same rule for both substrates)
# ---------------------------------------------------------------------------
# The sample is an ENTITY hash sample.  An entity is
#   GL   : agency | nominal_account_id | fund | object_id   (the GL key without period or flag)
#   LDGR : ldgrIPID                                         (the vendor / instrument id)
# keep_entity() is a pure function of (seed, entity key), so an entity is kept in every fiscal
# year or in none, both lines (D and O) of an LDGR event stay together, and the same rule and
# seed apply to both substrates.  GL rows carry no instrument id, so the two substrates cannot
# share source events; they share the rule, the seed, the years and the target record count.
# One fraction is used for all training years (sample_fraction), so entities stay consistent
# across years.  The achieved record count is recorded in the results file.

DEFAULT_SEED = 42
DEFAULT_TARGET_TRAIN_RECORDS = 20000   # per substrate, over all training years


def sample_fraction(target_records: int, total_train_rows: int) -> float:
    """Fraction of entities to keep so that about target_records rows are selected."""
    if target_records <= 0 or total_train_rows <= 0:
        raise ValueError("target_records and total_train_rows must be positive")
    return min(1.0, target_records / total_train_rows)


def keep_entity(entity_key: str, seed: int, fraction: float) -> bool:
    digest = hashlib.sha256(f"{seed}|{entity_key}".encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big") / 2 ** 64 < fraction


def entity_key(substrate: str, row: dict) -> str:
    if substrate == "gl":
        return "|".join(row[c].strip() for c in ("agency", "nominal_account_id", "fund", "object_id"))
    if substrate == "ldgr":
        return row["ldgrIPID"].strip()
    raise ValueError(f"unknown substrate {substrate!r}")


def period_of(substrate: str, row: dict) -> str:
    return row["fiscal_year"] if substrate == "gl" else row["ldgrLedgerPeriod"]


def select_training_rows(substrate: str, rows, train_fys, seed: int, fraction: float):
    """Yield rows (dicts keyed by the file header) in the training years whose entity is kept."""
    train = set(train_fys)
    for row in rows:
        if fiscal_year_of(period_of(substrate, row)) in train and keep_entity(entity_key(substrate, row), seed, fraction):
            yield row


# ---------------------------------------------------------------------------
# D3: forecasting target on a common basis (plan step 1 open check)
# ---------------------------------------------------------------------------
# Target for agency a and fiscal year t = the sum of DEBIT-line amounts (dir flag D) in t+1.
# GL basis  : agency, fiscal_year (period), dir_flag == D, total_amount
# LDGR basis: ldgrLegalEntityID, ldgrLedgerPeriod, ldgrDirVsOffsetFlg == D, ldgrTransAmount
# Both are the same SJE lines (GLPost.scala sums them by key), so the two totals should agree to
# the cent.  compare_totals() reports any difference.  A pair (a, t) has a target only when the
# agency has a debit total in both t and t+1 (otherwise there is no defined next-year amount);
# the number of dropped pairs is returned, not hidden.

_BASIS = {
    "gl": ("agency", "fiscal_year", "dir_flag", "total_amount"),
    "ldgr": ("ldgrLegalEntityID", "ldgrLedgerPeriod", "ldgrDirVsOffsetFlg", "ldgrTransAmount"),
}


def agency_year_debit_totals(substrate: str, rows) -> dict:
    """{(agency, fiscal_year): Decimal} over debit lines."""
    agency_col, period_col, flag_col, amount_col = _BASIS[substrate]
    totals = {}
    for row in rows:
        if row[flag_col].strip() != "D":
            continue
        try:
            amount = Decimal(row[amount_col].strip())
        except InvalidOperation as exc:
            raise ValueError(f"{substrate}: bad amount {row[amount_col]!r}") from exc
        key = (row[agency_col].strip(), fiscal_year_of(row[period_col]))
        totals[key] = totals.get(key, Decimal(0)) + amount
    return totals


def compare_totals(gl_totals: dict, ldgr_totals: dict, tolerance=Decimal(0)) -> dict:
    """Report whether the GL-derived and LDGR-derived targets agree on the common basis."""
    common = set(gl_totals) & set(ldgr_totals)
    mismatches = sorted(k for k in common if abs(gl_totals[k] - ldgr_totals[k]) > tolerance)
    max_diff = max((abs(gl_totals[k] - ldgr_totals[k]) for k in common), default=Decimal(0))
    only_gl = len(set(gl_totals) - common)
    only_ldgr = len(set(ldgr_totals) - common)
    return {
        "n_common": len(common), "n_only_gl": only_gl, "n_only_ldgr": only_ldgr,
        "n_mismatch": len(mismatches), "max_abs_diff": max_diff,
        "agree": not mismatches and only_gl == 0 and only_ldgr == 0,
    }


def forecast_pairs(totals: dict, last_complete: int, eligible=None):
    """Returns (pairs, dropped).  pairs = sorted [(agency, fy, last_year_amount, next_year_amount)].
    The naive baseline forecast is last_year_amount; the target is next_year_amount."""
    pairs, dropped = [], 0
    for (agency, fy), current in totals.items():
        if fy + 1 > last_complete or (eligible is not None and (fy not in eligible or fy + 1 not in eligible)):
            continue   # next year not observable: censored
        nxt = totals.get((agency, fy + 1))
        if nxt is None:
            dropped += 1
            continue
        pairs.append((agency, fy, current, nxt))
    return sorted(pairs), dropped


# ---------------------------------------------------------------------------
# D5: metrics
# ---------------------------------------------------------------------------


def _average_ranks(values) -> list:
    order = sorted(range(len(values)), key=lambda i: values[i])
    ranks = [0.0] * len(values)
    i = 0
    while i < len(order):
        j = i
        while j + 1 < len(order) and values[order[j + 1]] == values[order[i]]:
            j += 1
        for k in range(i, j + 1):
            ranks[order[k]] = (i + j) / 2.0 + 1.0
        i = j + 1
    return ranks


def c_statistic(labels, scores):
    """Area under the ROC curve (Mann-Whitney).  None when only one class is present."""
    n_pos = sum(1 for y in labels if y == 1)
    n_neg = len(labels) - n_pos
    if n_pos == 0 or n_neg == 0:
        return None
    ranks = _average_ranks(list(scores))
    pos_sum = sum(r for r, y in zip(ranks, labels) if y == 1)
    return (pos_sum - n_pos * (n_pos + 1) / 2.0) / (n_pos * n_neg)


def permutation_p(labels, scores, n_perm: int, seed: int = DEFAULT_SEED):
    """One-sided p that a random relabelling gives a C-statistic >= the observed one, with the
    scores held fixed (the AIM3 test).  (ge + 1) / (n_perm + 1).  None if not computable."""
    n_pos = sum(1 for y in labels if y == 1)
    n_neg = len(labels) - n_pos
    if n_pos == 0 or n_neg == 0 or n_perm <= 0:
        return None
    ranks = _average_ranks(list(scores))
    base = n_pos * (n_pos + 1) / 2.0
    observed = (sum(r for r, y in zip(ranks, labels) if y == 1) - base) / (n_pos * n_neg)
    rng = random.Random(seed)
    indexes = range(len(labels))
    ge = sum(
        1 for _ in range(n_perm)
        if (sum(ranks[i] for i in rng.sample(indexes, n_pos)) - base) / (n_pos * n_neg) >= observed
    )
    return (ge + 1) / (n_perm + 1)


def smape(actual, forecast) -> float:
    """Symmetric MAPE in [0, 2]: mean of 2|f-a| / (|a|+|f|); a 0/0 term counts as 0."""
    if len(actual) != len(forecast) or not actual:
        raise ValueError("actual and forecast must be non-empty and the same length")
    total = 0.0
    for a, f in zip(actual, forecast):
        denom = abs(a) + abs(f)
        total += 0.0 if denom == 0 else 2.0 * abs(f - a) / denom
    return total / len(actual)


def rmse(actual, forecast) -> float:
    if len(actual) != len(forecast) or not actual:
        raise ValueError("actual and forecast must be non-empty and the same length")
    return (sum((f - a) ** 2 for a, f in zip(actual, forecast)) / len(actual)) ** 0.5


def score_net_income(answer, truth, tolerance=Decimal("0.01")) -> str:
    """'correct' / 'incorrect' / NOT_ANSWERABLE.  answer is a number, a string or NOT_ANSWERABLE
    (a model that cannot answer is reported as such, not as incorrect: D7)."""
    if answer == NOT_ANSWERABLE:
        return NOT_ANSWERABLE
    try:
        value = Decimal(str(answer).strip())
    except InvalidOperation:
        return "incorrect"
    return "correct" if abs(value - Decimal(str(truth))) <= tolerance else "incorrect"
