#!/usr/bin/env bash
# =============================================================================
#  build_vendormaster_full.sh -- Build VendorMaster_full.csv from U### vendor index files
#
#  Source format (no header, 4 columns):
#    col 0: instID (zero-padded 10-digit)
#    col 1: instHolderName (quoted or unquoted)
#    col 2: instTypeID (EXP or REV)
#    col 3: instEffectDate (YYYY/MM/DD)
#
#  Output:
#    instID,instHolderName,instTypeID,instEffectDate
# =============================================================================

set -euo pipefail

BAL_DIR="${1:-data/ParallelProcessRunSept9}"
OUT_FILE="${2:-data/VendorMaster_full.csv}"

# Detect Python command
if command -v py &>/dev/null; then
  PYTHON="py -3"
elif command -v python3 &>/dev/null; then
  PYTHON="python3"
else
  PYTHON="python"
fi

echo "Building VendorMaster full from $BAL_DIR -> $OUT_FILE ..."

$PYTHON - <<EOF
import os, sys, glob, csv

bal_dir = "$BAL_DIR"
out_file = "$OUT_FILE"

vendors = {}

for b_num in range(103, 117):
    yr = b_num - 100
    for shard in range(10):
        for pattern in [f"U{b_num}q1exp.txtInstID{shard}.txt", f"U{b_num}rev.txtInstID{shard}.txt"]:
            fpath = os.path.join(bal_dir, pattern)
            if not os.path.exists(fpath):
                continue
            with open(fpath, "r", encoding="latin-1") as f:
                reader = csv.reader(f)
                for row in reader:
                    if not row or len(row) < 4:
                        continue
                    inst_id = row[0].strip()
                    if inst_id not in vendors:
                        vendors[inst_id] = (row[0].strip(), row[1].strip(), row[2].strip(), row[3].strip())
    print(f"  FY20{yr:02d}: cumulative vendors so far: {len(vendors)}")

print(f"Total unique vendors: {len(vendors)}")

os.makedirs(os.path.dirname(out_file) if os.path.dirname(out_file) else ".", exist_ok=True)
with open(out_file, "w", newline="", encoding="utf-8") as f:
    writer = csv.writer(f)
    writer.writerow(["instID", "instHolderName", "instTypeID", "instEffectDate"])
    for inst_id in sorted(vendors.keys()):
        writer.writerow(vendors[inst_id])

print(f"Written: {out_file}")
EOF
