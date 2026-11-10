#!/usr/bin/env python3
"""
Track C (Berka PKDD'99) -- Substrate Builder (v2, 2026-11-10)
Paper 2: Business Event Repositories as Training Corpora for Enterprise AI

Two views of the SAME pre-loan transaction rows (design: docs/TEST_DESIGN_TRACK_C_BERKA.md):

  Substrate E (event view):  the transaction rows as recorded: type, operation, k_symbol,
                             amount, counterparty bank/account, date.
  Substrate B (product-system view): E rolled up to account-day. One closing balance per
                             account per day, carried forward on days without activity.
                             This is the daily balance a product system holds. It drops the
                             intra-day sequence and every transaction attribute.

E's feature vector is B's feature vector plus event-derived columns, so E is a strict
superset of B by construction (see build_features()). The same loan terms, window and
history-length scaling feed both.

Rules fixed before running (no tuning):
  * Window: transactions with date strictly before the loan date.
  * Sign: PRIJEM is a credit (+); VYDAJ and VYBER are debits (-).
  * The closing balance of a day is the balance on the LAST transaction of that day. The
    source gives no intra-day order (trans_id does not reflect it), so the order is recovered
    from the balance chain: row j may follow row i if balance[j] - balance[i] == signed_amount[j]
    (integer cents). Where no unique closing row can be recovered the day is flagged and the
    row that appears last in the file is used.
  * District demographics are NOT used (the 1996 columns would post-date loans granted 1993-95).
  * order.asc.txt is NOT used (undated; every loan has its own repayment order).

Usage:
  python scripts/berka/build_berka_substrates.py \
    --berka-dir "$BERKA_DATA_ROOT/berka" --out-dir data/berka_processed
"""

import csv
import gzip
import json
import os
import sys
from collections import defaultdict
from datetime import datetime

import numpy as np

# ---------------------------------------------------------------------------
# CLI
# ---------------------------------------------------------------------------
BERKA_DIR = None
OUT_DIR = "data/berka_processed"

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--berka-dir" and i + 1 < len(sys.argv):
        BERKA_DIR = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--out-dir" and i + 1 < len(sys.argv):
        OUT_DIR = sys.argv[i + 1]; i += 2
    else:
        print(f"Unknown argument: {sys.argv[i]}", file=sys.stderr)
        sys.exit(1)

if not BERKA_DIR or not os.path.isdir(BERKA_DIR):
    print("Error: --berka-dir must point at the directory holding the Berka *.asc.txt files "
          "(run scripts/download_berka.sh first).", file=sys.stderr)
    sys.exit(1)

os.makedirs(OUT_DIR, exist_ok=True)

LOW_BALANCE_CZK = 5000.0
MAX_ROWS_FOR_ORDER_SEARCH = 12
MAX_ORDERINGS = 5000

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def parse_berka_date(d_str):
    d_str = str(d_str).strip().strip('"')
    return datetime(int(d_str[0:2]) + 1900, int(d_str[2:4]), int(d_str[4:6])).date()


def to_cents(v):
    v = str(v).strip().strip('"')
    return int(round(float(v) * 100)) if v not in ("", "?") else 0


def open_berka_file(base_dir, filename):
    path = os.path.join(base_dir, filename)
    if not os.path.exists(path) and os.path.exists(path + ".gz"):
        path = path + ".gz"
    if path.endswith(".gz"):
        return gzip.open(path, mode="rt", encoding="utf-8-sig", newline="")
    return open(path, mode="r", encoding="utf-8-sig", newline="")


def strip(v):
    return v.strip().strip('"')


# ---------------------------------------------------------------------------
# 1. Loans (ground truth, loan terms known at grant)
# ---------------------------------------------------------------------------
print(f"Loading loans from {BERKA_DIR}...")
loans = []
with open_berka_file(BERKA_DIR, "loan.asc.txt") as f:
    reader = csv.reader(f, delimiter=";")
    next(reader)
    for row in reader:
        if not row:
            continue
        status = strip(row[6])
        loans.append({
            "loan_id": int(strip(row[0])),
            "account_id": int(strip(row[1])),
            "loan_date": parse_berka_date(row[2]),
            "amount": float(strip(row[3])),
            "duration": int(strip(row[4])),
            "payments": float(strip(row[5])),
            "status": status,
            "target_default": 1 if status in ("B", "D") else 0,   # B,D = distress; A,C = good
        })
print(f"Total loans: {len(loans)} (defaults: {sum(l['target_default'] for l in loans)})")

loan_by_account = {}
for l in loans:
    if l["account_id"] in loan_by_account:
        print("Error: more than one loan on one account; loan_by_account keying is unsafe.", file=sys.stderr)
        sys.exit(1)
    loan_by_account[l["account_id"]] = l

# ---------------------------------------------------------------------------
# 2. Stream transactions, keep strictly pre-loan rows (the single event source for B and E)
# ---------------------------------------------------------------------------
print("Streaming transactions...")
account_rows = defaultdict(list)
n_read = 0
with open_berka_file(BERKA_DIR, "trans.asc.txt") as f:
    reader = csv.reader(f, delimiter=";")
    next(reader)
    for pos, row in enumerate(reader):
        if not row:
            continue
        acc_id = int(strip(row[1]))
        loan = loan_by_account.get(acc_id)
        if loan is None:
            continue
        t_date = parse_berka_date(row[2])
        if t_date >= loan["loan_date"]:
            continue
        t_type = strip(row[3])
        amt = to_cents(row[5])
        account_rows[acc_id].append({
            "pos": pos,                               # file position (fallback order only)
            "trans_id": int(strip(row[0])),
            "date": t_date,
            "type": t_type,
            "operation": strip(row[4]),
            "signed": amt if t_type == "PRIJEM" else -amt,
            "amount": amt,
            "balance": to_cents(row[6]),
            "k_symbol": strip(row[7]),
            "bank": strip(row[8]),
            "partner": strip(row[9]),
        })
        n_read += 1
print(f"Pre-loan transaction rows retained: {n_read}")

# ---------------------------------------------------------------------------
# 3. Recover intra-day order from the balance chain; build the daily closing balance
# ---------------------------------------------------------------------------

def day_orderings(rows):
    """All orderings of `rows` in which every consecutive pair satisfies
    balance[next] - balance[prev] == signed[next]. Returns (list of orderings, capped)."""
    n = len(rows)
    ok = [[(rows[j]["balance"] - rows[i]["balance"]) == rows[j]["signed"] if i != j else False
           for j in range(n)] for i in range(n)]
    found = []
    capped = False

    def dfs(path, used):
        nonlocal capped
        if capped:
            return
        if len(path) == n:
            found.append(list(path))
            if len(found) >= MAX_ORDERINGS:
                capped = True
            return
        last = path[-1]
        for j in range(n):
            if not used[j] and ok[last][j]:
                used[j] = True
                path.append(j)
                dfs(path, used)
                path.pop()
                used[j] = False

    for s in range(n):
        used = [False] * n
        used[s] = True
        dfs([s], used)
    return found, capped


day_status = defaultdict(int)


def closing_balance(rows):
    """Closing balance (cents) of one account-day and its status."""
    if len(rows) == 1:
        day_status["single_row"] += 1
        return rows[0]["balance"]
    fallback = max(rows, key=lambda r: r["pos"])["balance"]
    if len(rows) > MAX_ROWS_FOR_ORDER_SEARCH:
        day_status["too_many_rows_fallback"] += 1
        return fallback
    orders, capped = day_orderings(rows)
    if not orders:
        day_status["no_ordering_fallback"] += 1
        return fallback
    lasts = {rows[o[-1]]["balance"] for o in orders}
    if len(orders) == 1:
        day_status["unique_order"] += 1
        return rows[orders[0][-1]]["balance"]
    if len(lasts) == 1:
        day_status["ambiguous_order_same_close"] += 1
        return lasts.pop()
    day_status["ambiguous_close_fallback"] += 1
    return fallback


print("Building daily closing balances (order recovered from the balance chain)...")
daily_close = {}          # acc -> {date_ordinal: close_cents}
rows_by_day = {}          # acc -> {date_ordinal: [rows]}
for acc_id, rows in account_rows.items():
    by_day = defaultdict(list)
    for r in rows:
        by_day[r["date"].toordinal()].append(r)
    rows_by_day[acc_id] = by_day
    daily_close[acc_id] = {d: closing_balance(rs) for d, rs in by_day.items()}

multi_days = sum(v for k, v in day_status.items() if k != "single_row")
print("Account-day status:", dict(day_status))
print(f"Multi-row account-days: {multi_days}")

# ---------------------------------------------------------------------------
# 4. Feature builder: B is computed from the daily series only; E = B + event-derived columns
# ---------------------------------------------------------------------------

def signed_nan_safe_div(a, b):
    return a / b if b else 0.0


def build_features(loan):
    """Returns (b_features, e_extra_features) as dicts. b_features uses only the daily
    closing-balance series and the loan terms; e_extra uses the transaction rows."""
    acc = loan["account_id"]
    last_day = loan["loan_date"].toordinal() - 1
    closes = daily_close.get(acc, {})
    b = {
        "loan_amount": loan["amount"],
        "duration": float(loan["duration"]),
        "payments": loan["payments"],
    }
    if not closes:
        raise RuntimeError(f"loan {loan['loan_id']} has no pre-loan transactions")

    first_day = min(closes)
    days = np.arange(first_day, last_day + 1)
    series = np.empty(len(days))
    cur = None
    for k, d in enumerate(days):
        if d in closes:
            cur = closes[d] / 100.0
        series[k] = cur
    hist_days = len(days)
    scale30 = 30.0 / hist_days
    delta = np.diff(series) if hist_days > 1 else np.array([0.0])

    b.update({
        "history_days": float(hist_days),
        "bal_mean": float(series.mean()),
        "bal_min": float(series.min()),
        "bal_max": float(series.max()),
        "bal_std": float(series.std()),
        "bal_last": float(series[-1]),
        "bal_last30_mean": float(series[-30:].mean()),
        "days_negative": float((series < 0).sum()),
        "days_below_5000": float((series < LOW_BALANCE_CZK).sum()),
        "credit_per30": float(delta[delta > 0].sum() * scale30),
        "debit_per30": float(-delta[delta < 0].sum() * scale30),
        "changed_days_per30": float((delta != 0).sum() * scale30),
    })

    rows = account_rows[acc]
    by_day = rows_by_day[acc]
    n_rows = len(rows)

    def cnt(pred):
        return sum(1 for r in rows if pred(r))

    def amt(pred):
        return sum(r["amount"] for r in rows if pred(r)) / 100.0

    credits = amt(lambda r: r["type"] == "PRIJEM")
    cash_out = amt(lambda r: r["operation"] in ("VYBER", "VYBER KARTOU") and r["type"] != "PRIJEM")
    ordered_dates = sorted(r["date"].toordinal() for r in rows)
    gaps = np.diff(ordered_dates) if n_rows > 1 else np.array([0.0])
    partner_rows = [r for r in rows if r["partner"] != ""]

    e = {
        "txn_per30": n_rows * scale30,
        "multi_txn_day_share": sum(1 for v in by_day.values() if len(v) > 1) / len(by_day),
        "max_rows_per_day": float(max(len(v) for v in by_day.values())),
        "avg_gap_days": float(gaps.mean()),
        "cash_wd_per30": cnt(lambda r: r["operation"] == "VYBER" and r["type"] != "PRIJEM") * scale30,
        "card_wd_per30": cnt(lambda r: r["operation"] == "VYBER KARTOU") * scale30,
        "deposit_per30": cnt(lambda r: r["operation"] == "VKLAD") * scale30,
        "xfer_out_per30": cnt(lambda r: r["operation"] == "PREVOD NA UCET") * scale30,
        "xfer_in_per30": cnt(lambda r: r["operation"] == "PREVOD Z UCTU") * scale30,
        "cash_wd_amt_per30": amt(lambda r: r["operation"] == "VYBER" and r["type"] != "PRIJEM") * scale30,
        "card_wd_amt_per30": amt(lambda r: r["operation"] == "VYBER KARTOU") * scale30,
        "deposit_amt_per30": amt(lambda r: r["operation"] == "VKLAD") * scale30,
        "cash_drain_ratio": signed_nan_safe_div(cash_out, credits),
        "sipo_per30": cnt(lambda r: r["k_symbol"] == "SIPO") * scale30,
        "sluzby_per30": cnt(lambda r: r["k_symbol"] == "SLUZBY") * scale30,
        "pojistne_per30": cnt(lambda r: r["k_symbol"] == "POJISTNE") * scale30,
        "duchod_per30": cnt(lambda r: r["k_symbol"] == "DUCHOD") * scale30,
        "sanction_per30": cnt(lambda r: r["k_symbol"] == "SANKC. UROK") * scale30,
        "distinct_partner_accounts": float(len({(r["bank"], r["partner"]) for r in partner_rows})),
        "distinct_partner_banks": float(len({r["bank"] for r in partner_rows if r["bank"] != ""})),
        "partner_txn_per30": len(partner_rows) * scale30,
        "partner_txn_share": len(partner_rows) / n_rows,
    }
    return b, e


b_rows, e_rows = [], []
for loan in loans:
    b, e = build_features(loan)
    ident = {
        "loan_id": loan["loan_id"],
        "account_id": loan["account_id"],
        "target_default": loan["target_default"],
    }
    b_rows.append({**ident, **b})
    e_rows.append({**ident, **b, **e})        # E = B columns + event columns (strict superset)

assert set(b_rows[0]) <= set(e_rows[0]), "E must contain every B column"

# ---------------------------------------------------------------------------
# 5. Write outputs
# ---------------------------------------------------------------------------

def write_csv(path, rows):
    with open(path, "w", newline="", encoding="utf-8") as fh:
        w = csv.DictWriter(fh, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    print(f"Written {path} ({len(rows)} rows, {len(rows[0])} columns)")


write_csv(os.path.join(OUT_DIR, "substrate_b_daily_balance.csv"), b_rows)
write_csv(os.path.join(OUT_DIR, "substrate_e_events.csv"), e_rows)

daily_path = os.path.join(OUT_DIR, "daily_balance_series.csv")
with open(daily_path, "w", newline="", encoding="utf-8") as fh:
    w = csv.writer(fh)
    w.writerow(["account_id", "day_ordinal", "closing_balance_cents", "rows_that_day"])
    for acc_id, closes in daily_close.items():
        for d in sorted(closes):
            w.writerow([acc_id, d, closes[d], len(rows_by_day[acc_id][d])])
print(f"Written {daily_path}")

stats = {
    "pre_loan_rows": n_read,
    "account_days_with_activity": int(sum(len(v) for v in daily_close.values())),
    "account_day_status": dict(day_status),
    "b_columns": [c for c in b_rows[0].keys()],
    "e_extra_columns": [c for c in e_rows[0].keys() if c not in b_rows[0]],
}
with open(os.path.join(OUT_DIR, "build_stats.json"), "w", encoding="utf-8") as fh:
    json.dump(stats, fh, indent=2)
print("Track C substrate preparation complete.")
