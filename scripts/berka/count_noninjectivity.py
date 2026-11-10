#!/usr/bin/env python3
"""
Track C (Berka PKDD'99) -- Direct non-injectivity count of the daily-balance grain
(added 2026-11-10; AIM1 analogue, no model, standard library only)

Question (docs/TEST_DESIGN_TRACK_C_BERKA.md, section 9): how many distinct event patterns does
the daily-balance view B map to the same stored value? This is a count, like AIM1
(docs/TEST_DESIGN.md section 5), not a prediction.

What is counted. The event rows are the pre-loan transactions of the 682 loan accounts (strictly
before the loan date, as in build_berka_substrates.py). B holds, per account-day, one closing
balance. Its day-to-day change is the net of that day's signed amounts. The count does not use
the recovered intra-day order, so it does not depend on the balance chain.

  Measure 1 (key = account-day, the AIM1 'posted key'):
      per account-day, how many distinct event kinds sit under the one stored number.
      Event kind = (type, operation, k_symbol). Fine kind adds (bank, partner).
  Measure 2 (key = B value, here the signed net change of the day in cents):
      for each net-change value that occurs on at least two account-days, how many distinct
      event patterns produce it. Event pattern = sorted multiset of (type, operation, k_symbol)
      over the day's rows. Reported for all active days and for single-row days only.
      The closing LEVEL is not part of the key: it carries the account's whole history, so with it
      nearly every key is unique and the question is trivial. The change is the part of B that
      a single day contributes.

Limits, stated with the result: this counts how the roll-up we built collapses rows. It says
nothing about what a bank's posting system keeps, because the lineage of the Berka files is
unknown. Collisions on round amounts are expected; the count shows how many event distinctions B
cannot separate, not that the separated events carry information about default.

Usage:
  python3 scripts/berka/count_noninjectivity.py \
    --berka-dir "$BERKA_DATA_ROOT/berka" \
    --evidence-out data/test_evidence/TRACK_C_BERKA_NONINJECTIVITY.json
"""

import csv
import gzip
import json
import os
import sys
from collections import Counter, defaultdict
from datetime import datetime

BERKA_DIR = None
EVIDENCE_OUT = "data/test_evidence/TRACK_C_BERKA_NONINJECTIVITY.json"

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--berka-dir" and i + 1 < len(sys.argv):
        BERKA_DIR = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--evidence-out" and i + 1 < len(sys.argv):
        EVIDENCE_OUT = sys.argv[i + 1]; i += 2
    else:
        print(f"Unknown argument: {sys.argv[i]}", file=sys.stderr)
        sys.exit(1)

if not BERKA_DIR or not os.path.isdir(BERKA_DIR):
    print("Error: --berka-dir must point at the directory holding the Berka *.asc.txt files "
          "(run scripts/download_berka.sh first).", file=sys.stderr)
    sys.exit(1)


def strip(v):
    return v.strip().strip('"')


def parse_berka_date(d_str):
    d_str = strip(str(d_str))
    return datetime(int(d_str[0:2]) + 1900, int(d_str[2:4]), int(d_str[4:6])).date()


def to_cents(v):
    v = strip(str(v))
    return int(round(float(v) * 100)) if v not in ("", "?") else 0


def open_berka_file(base_dir, filename):
    path = os.path.join(base_dir, filename)
    if not os.path.exists(path) and os.path.exists(path + ".gz"):
        path = path + ".gz"
    if path.endswith(".gz"):
        return gzip.open(path, mode="rt", encoding="utf-8-sig", newline="")
    return open(path, mode="r", encoding="utf-8-sig", newline="")


# --- loans: only the account and the loan date are needed -------------------------------------
loan_date = {}
with open_berka_file(BERKA_DIR, "loan.asc.txt") as f:
    reader = csv.reader(f, delimiter=";")
    next(reader)
    for row in reader:
        if row:
            loan_date[int(strip(row[1]))] = parse_berka_date(row[2])
print(f"Loans: {len(loan_date)}")

# --- pre-loan transaction rows grouped by (account, day) -------------------------------------
days = defaultdict(list)        # (acc, day_ordinal) -> list of (type, operation, k_symbol, bank, partner, signed)
n_rows = 0
with open_berka_file(BERKA_DIR, "trans.asc.txt") as f:
    reader = csv.reader(f, delimiter=";")
    next(reader)
    for row in reader:
        if not row:
            continue
        acc = int(strip(row[1]))
        ld = loan_date.get(acc)
        if ld is None:
            continue
        d = parse_berka_date(row[2])
        if d >= ld:
            continue
        t_type = strip(row[3])
        amt = to_cents(row[5])
        signed = amt if t_type == "PRIJEM" else -amt
        days[(acc, d.toordinal())].append(
            (t_type, strip(row[4]), strip(row[7]), strip(row[8]), strip(row[9]), signed))
        n_rows += 1
print(f"Pre-loan rows: {n_rows}; active account-days: {len(days)}")


def kind(r):
    return (r[0], r[1], r[2])


def pattern(rows):
    return tuple(sorted(kind(r) for r in rows))


def bucket(n):
    if n <= 1:
        return "1"
    if n == 2:
        return "2"
    if n <= 5:
        return "3-5"
    if n <= 10:
        return "6-10"
    return ">10"


def dist(values, order=("1", "2", "3-5", "6-10", ">10")):
    c = Counter(bucket(v) for v in values)
    return {k: c.get(k, 0) for k in order}


# --- Measure 1: distinct event kinds under one stored account-day number ---------------------
m1_kinds, m1_fine = [], []
n_multi = n_multi_kinds = n_offset = n_zero_net = 0
for (acc, d), rows in days.items():
    kinds = {kind(r) for r in rows}
    fine = {kind(r) + (r[3], r[4]) for r in rows}
    m1_kinds.append(len(kinds))
    m1_fine.append(len(fine))
    if len(rows) > 1:
        n_multi += 1
    if len(kinds) > 1:
        n_multi_kinds += 1
    types = {r[0] for r in rows}
    if "PRIJEM" in types and len(types) > 1:
        n_offset += 1
    if sum(r[5] for r in rows) == 0:
        n_zero_net += 1

n_days = len(days)
measure1 = {
    "account_days": n_days,
    "days_with_more_than_one_row": n_multi,
    "days_with_more_than_one_event_kind": n_multi_kinds,
    "share_days_with_more_than_one_event_kind": round(n_multi_kinds / n_days, 4),
    "days_with_credit_and_debit_netted_into_one_number": n_offset,
    "days_with_zero_net_change_despite_rows": n_zero_net,
    "distinct_event_kinds_per_day": dist(m1_kinds),
    "distinct_event_kinds_incl_counterparty_per_day": dist(m1_fine),
    "max_distinct_event_kinds_in_a_day": max(m1_kinds),
}


# --- Measure 2: distinct event patterns per net-change value ---------------------------------
def measure2(selector):
    by_delta = defaultdict(lambda: [0, set()])      # delta -> [days, set of patterns]
    for key, rows in days.items():
        if not selector(rows):
            continue
        delta = sum(r[5] for r in rows)
        g = by_delta[delta]
        g[0] += 1
        g[1].add(pattern(rows))
    groups = {d: g for d, g in by_delta.items() if g[0] >= 2}
    days_in_groups = sum(g[0] for g in groups.values())
    collide = {d: g for d, g in groups.items() if len(g[1]) > 1}
    days_in_collide = sum(g[0] for g in collide.values())
    pattern_counts = [len(g[1]) for g in groups.values()]
    top = sorted(collide.items(), key=lambda kv: -len(kv[1][1]))[:5]
    return {
        "net_change_values_total": len(by_delta),
        "net_change_values_on_2plus_days": len(groups),
        "values_mapping_to_more_than_one_event_pattern": len(collide),
        "share_of_2plus_values_with_collision": round(len(collide) / len(groups), 4) if groups else None,
        "account_days_on_2plus_values": days_in_groups,
        "account_days_whose_value_has_more_than_one_pattern": days_in_collide,
        "share_of_those_days": round(days_in_collide / days_in_groups, 4) if days_in_groups else None,
        "distinct_patterns_per_value_distribution": dist(pattern_counts),
        "max_distinct_patterns_for_one_value": max(pattern_counts) if pattern_counts else 0,
        "top_collisions_cents": [
            {"net_change_cents": d, "account_days": g[0], "distinct_patterns": len(g[1])}
            for d, g in top
        ],
    }


measure2_all = measure2(lambda rows: True)
measure2_single = measure2(lambda rows: len(rows) == 1)

evidence = {
    "experiment": "Paper 2 - Track C Berka, direct non-injectivity count of the daily-balance grain",
    "date": "2026-11-10",
    "status": ("Exploratory. Counts how our own roll-up collapses event rows; says nothing about a "
               "bank's posting system (lineage of the Berka files is unknown)."),
    "window": "transactions strictly before the loan date, 682 loan accounts",
    "pre_loan_rows": n_rows,
    "event_kind": "(type, operation, k_symbol)",
    "event_pattern": "sorted multiset of event kinds over a day's rows",
    "measure_1_per_account_day": measure1,
    "measure_2_per_net_change_all_active_days": measure2_all,
    "measure_2_per_net_change_single_row_days": measure2_single,
}

os.makedirs(os.path.dirname(EVIDENCE_OUT) or ".", exist_ok=True)
with open(EVIDENCE_OUT, "w", encoding="utf-8") as fh:
    json.dump(evidence, fh, indent=2)
print(json.dumps(evidence, indent=2))
print(f"\nWritten non-injectivity evidence to: {EVIDENCE_OUT}")
