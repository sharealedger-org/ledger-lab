#!/usr/bin/env python3
"""
Track C (Berka PKDD'99) -- Substrate Builder
Paper 2: Business Event Repositories as Training Corpora for Enterprise AI

Extracts two distinct representations from the Berka banking data for all 682 loans:
  1. Substrate B (Layer 2): Balance aggregates (monthly closing balances, turnover sums, min/max)
  2. Substrate E (Layer 3): Pre-hierarchy event sequences (atomic transaction tuples, operation modes, k-symbols)

Usage:
  python scripts/berka/build_berka_substrates.py \
    --berka-dir ../org-members/content/examples/banking-berka/data_berka \
    --out-dir data/berka_processed
"""

import csv
import gzip
import json
import math
import os
import sys
from collections import defaultdict
from datetime import datetime

# ---------------------------------------------------------------------------
# CLI Argument Parsing
# ---------------------------------------------------------------------------
BERKA_DIR = "data/berka"
OUT_DIR   = "data/berka_processed"

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--berka-dir" and i + 1 < len(sys.argv):
        BERKA_DIR = sys.argv[i + 1]; i += 2
    elif sys.argv[i] == "--out-dir" and i + 1 < len(sys.argv):
        OUT_DIR = sys.argv[i + 1]; i += 2
    else:
        print(f"Unknown argument: {sys.argv[i]}", file=sys.stderr)
        sys.exit(1)

# Fallback: check sibling directory if default does not exist
if not os.path.exists(BERKA_DIR):
    alt_dir = "../org-members/content/examples/banking-berka/data_berka"
    if os.path.exists(alt_dir):
        BERKA_DIR = alt_dir

os.makedirs(OUT_DIR, exist_ok=True)

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def parse_berka_date(d_str):
    """Parse YYMMDD into datetime.date (assume 1990s)."""
    d_str = str(d_str).strip().strip('"')
    yy = int(d_str[0:2]) + 1900
    mm = int(d_str[2:4])
    dd = int(d_str[4:6])
    return datetime(yy, mm, dd).date()

def safe_float(v):
    if v is None: return 0.0
    v = str(v).strip().strip('"')
    if v in ["?", "", "NA", "null"]: return 0.0
    try: return float(v)
    except: return 0.0

def open_berka_file(base_dir, filename):
    """Open semicolon-delimited file, handling .gz if needed."""
    path = os.path.join(base_dir, filename)
    if not os.path.exists(path) and os.path.exists(path + ".gz"):
        path = path + ".gz"
    
    if path.endswith(".gz"):
        return gzip.open(path, mode="rt", encoding="utf-8-sig", newline="")
    return open(path, mode="r", encoding="utf-8-sig", newline="")

# ---------------------------------------------------------------------------
# 1. Load Districts (Demographics)
# ---------------------------------------------------------------------------
print(f"Loading districts from {BERKA_DIR}...")
districts = {}
with open_berka_file(BERKA_DIR, "district.asc.txt") as f:
    reader = csv.reader(f, delimiter=";")
    header = next(reader)
    for row in reader:
        if not row: continue
        d_id = int(row[0].strip().strip('"'))
        districts[d_id] = {
            "inhabitants": safe_float(row[3]),
            "avg_salary": safe_float(row[10]),
            "unemp_95": safe_float(row[11]),
            "unemp_96": safe_float(row[12]),
            "crime_95": safe_float(row[14]),
            "crime_96": safe_float(row[15]),
        }

# ---------------------------------------------------------------------------
# 2. Load Accounts
# ---------------------------------------------------------------------------
print(f"Loading accounts from {BERKA_DIR}...")
accounts = {}
with open_berka_file(BERKA_DIR, "account.asc.txt") as f:
    reader = csv.reader(f, delimiter=";")
    header = next(reader)
    # Header: "account_id";"district_id";"frequency";"date"
    for row in reader:
        if not row: continue
        acc_id = int(row[0].strip().strip('"'))
        dist_id = int(row[1].strip().strip('"'))
        freq = row[2].strip().strip('"')
        date_created = parse_berka_date(row[3])
        accounts[acc_id] = {
            "district_id": dist_id,
            "date_created": date_created,
            "frequency": freq,
        }

# ---------------------------------------------------------------------------
# 3. Load Loans (Ground Truth)
# ---------------------------------------------------------------------------
print(f"Loading loans from {BERKA_DIR}...")
loans = []
with open_berka_file(BERKA_DIR, "loan.asc.txt") as f:
    reader = csv.reader(f, delimiter=";")
    header = next(reader)
    # Header: "loan_id";"account_id";"date";"amount";"duration";"payments";"status"
    for row in reader:
        if not row: continue
        loan_id = int(row[0].strip().strip('"'))
        acc_id = int(row[1].strip().strip('"'))
        loan_date = parse_berka_date(row[2])
        amount = safe_float(row[3])
        duration = int(row[4].strip().strip('"'))
        payments = safe_float(row[5])
        status = row[6].strip().strip('"')
        # Target: 1 = default/distress (B or D), 0 = good (A or C)
        is_default = 1 if status in ["B", "D"] else 0
        loans.append({
            "loan_id": loan_id,
            "account_id": acc_id,
            "loan_date": loan_date,
            "amount": amount,
            "duration": duration,
            "payments": payments,
            "status": status,
            "target_default": is_default,
        })

print(f"Total loans: {len(loans)} (Defaults: {sum(l['target_default'] for l in loans)})")
loan_by_account = {l["account_id"]: l for l in loans}

# ---------------------------------------------------------------------------
# 4. Stream Transactions and Partition Pre-Loan Events
# ---------------------------------------------------------------------------
print(f"Streaming transactions from {BERKA_DIR}...")
account_trans = defaultdict(list)

with open_berka_file(BERKA_DIR, "trans.asc.txt") as f:
    reader = csv.reader(f, delimiter=";")
    header = next(reader)
    # Header: "trans_id";"account_id";"date";"type";"operation";"amount";"balance";"k_symbol";"bank";"account"
    for row in reader:
        if not row: continue
        acc_id = int(row[1].strip().strip('"'))
        if acc_id not in loan_by_account:
            continue
        
        t_date = parse_berka_date(row[2])
        loan_info = loan_by_account[acc_id]
        
        # Only retain pre-loan events (strictly t < loan_date)
        if t_date < loan_info["loan_date"]:
            t_type = row[3].strip().strip('"')
            t_op = row[4].strip().strip('"')
            t_amt = safe_float(row[5])
            t_bal = safe_float(row[6])
            t_ksym = row[7].strip().strip('"')
            t_bank = row[8].strip().strip('"')
            
            account_trans[acc_id].append({
                "date": t_date,
                "type": t_type,
                "operation": t_op,
                "amount": t_amt,
                "balance": t_bal,
                "k_symbol": t_ksym,
                "bank": t_bank,
            })

# Sort events chronologically per account
for acc_id in account_trans:
    account_trans[acc_id].sort(key=lambda x: x["date"])

# ---------------------------------------------------------------------------
# 5. Build Substrate B: Compressed Balance Aggregates (Layer 2)
# ---------------------------------------------------------------------------
print("Building Substrate B (Layer 2: Compressed Balances)...")
substrate_b_rows = []

for loan in loans:
    acc_id = loan["account_id"]
    evts = account_trans.get(acc_id, [])
    acc_meta = accounts.get(acc_id, {})
    dist_meta = districts.get(acc_meta.get("district_id", 0), {})
    
    # Balance trajectory features
    if evts:
        balances = [e["balance"] for e in evts]
        credits = [e["amount"] for e in evts if e["type"] == "PRIJEM"]
        debits = [e["amount"] for e in evts if e["type"] in ["VYDAJ", "VYBER"]]
        
        bal_mean = sum(balances) / len(balances)
        bal_min = min(balances)
        bal_max = max(balances)
        bal_std = math.sqrt(sum((b - bal_mean) ** 2 for b in balances) / len(balances)) if len(balances) > 1 else 0.0
        bal_last = balances[-1]
        
        tot_credit = sum(credits)
        tot_debit = sum(debits)
        net_flow = tot_credit - tot_debit
        tx_count = len(evts)
    else:
        bal_mean = bal_min = bal_max = bal_std = bal_last = 0.0
        tot_credit = tot_debit = net_flow = 0.0
        tx_count = 0

    substrate_b_rows.append({
        "loan_id": loan["loan_id"],
        "account_id": acc_id,
        "loan_amount": loan["amount"],
        "duration": loan["duration"],
        "payments": loan["payments"],
        "target_default": loan["target_default"],
        "bal_mean": bal_mean,
        "bal_min": bal_min,
        "bal_max": bal_max,
        "bal_std": bal_std,
        "bal_last": bal_last,
        "tot_credit": tot_credit,
        "tot_debit": tot_debit,
        "net_flow": net_flow,
        "tx_count": tx_count,
        "district_salary": dist_meta.get("avg_salary", 0.0),
        "district_unemp": dist_meta.get("unemp_96", 0.0),
        "district_crime": dist_meta.get("crime_96", 0.0),
    })

# Write Substrate B
sub_b_path = os.path.join(OUT_DIR, "substrate_b_balances.csv")
with open(sub_b_path, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=list(substrate_b_rows[0].keys()))
    writer.writeheader()
    writer.writerows(substrate_b_rows)
print(f"Written Substrate B: {sub_b_path} ({len(substrate_b_rows)} rows)")

# ---------------------------------------------------------------------------
# 6. Build Substrate E: Pre-Hierarchy Event Sequences & Enriched Tokens (Layer 3)
# ---------------------------------------------------------------------------
print("Building Substrate E (Layer 3: Pre-Hierarchy Event Stream)...")
substrate_e_rows = []

for loan in loans:
    acc_id = loan["account_id"]
    evts = account_trans.get(acc_id, [])
    acc_meta = accounts.get(acc_id, {})
    dist_meta = districts.get(acc_meta.get("district_id", 0), {})
    
    # Event-native feature extraction (capturing micro-dynamics)
    atm_withdrawals = [e["amount"] for e in evts if e["operation"] == "VYBER KARTOU"]
    cash_withdrawals = [e["amount"] for e in evts if e["operation"] == "VYBER"]
    cash_deposits = [e["amount"] for e in evts if e["operation"] == "VKLAD"]
    sanction_interests = [e["amount"] for e in evts if e["k_symbol"] == "SANKC. UROK"]
    household_payments = [e["amount"] for e in evts if e["k_symbol"] == "SIPO"]
    
    # Inter-arrival times
    dt_list = []
    for k in range(1, len(evts)):
        dt_list.append((evts[k]["date"] - evts[k-1]["date"]).days)
    
    avg_dt = sum(dt_list) / len(dt_list) if dt_list else 0.0
    
    # Immediate cash drain velocity (ratio of cash extraction to total credits)
    tot_credits = sum(e["amount"] for e in evts if e["type"] == "PRIJEM")
    cash_drain_ratio = (sum(cash_withdrawals) + sum(atm_withdrawals)) / tot_credits if tot_credits > 0 else 0.0
    
    # Low-balance event counts (e.g., balance < 5,000 CZK)
    low_bal_count = sum(1 for e in evts if e["balance"] < 5000)
    negative_bal_count = sum(1 for e in evts if e["balance"] < 0)

    substrate_e_rows.append({
        "loan_id": loan["loan_id"],
        "account_id": acc_id,
        "loan_amount": loan["amount"],
        "duration": loan["duration"],
        "payments": loan["payments"],
        "target_default": loan["target_default"],
        # Base balance attributes (same as Substrate B)
        "bal_mean": sum(e["balance"] for e in evts) / len(evts) if evts else 0.0,
        "bal_min": min((e["balance"] for e in evts), default=0.0),
        "bal_max": max((e["balance"] for e in evts), default=0.0),
        "bal_last": evts[-1]["balance"] if evts else 0.0,
        "district_salary": dist_meta.get("avg_salary", 0.0),
        "district_unemp": dist_meta.get("unemp_96", 0.0),
        # Pure Layer 3 Pre-Hierarchy Event Features (lost during posting)
        "atm_count": len(atm_withdrawals),
        "atm_sum": sum(atm_withdrawals),
        "cash_with_count": len(cash_withdrawals),
        "cash_with_sum": sum(cash_withdrawals),
        "cash_dep_count": len(cash_deposits),
        "cash_dep_sum": sum(cash_deposits),
        "sanction_int_count": len(sanction_interests),
        "sanction_int_sum": sum(sanction_interests),
        "household_pay_count": len(household_payments),
        "household_pay_sum": sum(household_payments),
        "cash_drain_ratio": cash_drain_ratio,
        "low_bal_count": low_bal_count,
        "negative_bal_count": negative_bal_count,
        "avg_dt_days": avg_dt,
    })

# Write Substrate E
sub_e_path = os.path.join(OUT_DIR, "substrate_e_events.csv")
with open(sub_e_path, "w", newline="", encoding="utf-8") as f:
    writer = csv.DictWriter(f, fieldnames=list(substrate_e_rows[0].keys()))
    writer.writeheader()
    writer.writerows(substrate_e_rows)
print(f"Written Substrate E: {sub_e_path} ({len(substrate_e_rows)} rows)")

print("Track C substrate preparation complete.")
