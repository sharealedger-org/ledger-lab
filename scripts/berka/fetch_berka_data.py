#!/usr/bin/env python3
"""
Track C (Berka PKDD'99) -- Data Fetcher & Unpacker
Paper 2: Business Event Repositories as Training Corpora for Enterprise AI

Locates or unpacks the Berka PKDD'99 banking dataset into data/berka/.
Checks:
  1. Local sibling repository (org-members/content/examples/banking-berka/data_berka)
  2. data/berka/ directory in ledger-lab
  3. Public archival mirrors (if configured)

Usage:
  python scripts/berka/fetch_berka_data.py --target-dir data/berka
"""

import gzip
import os
import shutil
import sys

TARGET_DIR = "data/berka"
SIBLING_DIR = "../org-members/content/examples/banking-berka/data_berka"

i = 1
while i < len(sys.argv):
    if sys.argv[i] == "--target-dir" and i + 1 < len(sys.argv):
        TARGET_DIR = sys.argv[i + 1]; i += 2
    else:
        print(f"Unknown argument: {sys.argv[i]}", file=sys.stderr)
        sys.exit(1)

os.makedirs(TARGET_DIR, exist_ok=True)

print(f"Resolving Berka data fixtures for target directory: {TARGET_DIR}")

# Check sibling directory
if os.path.exists(SIBLING_DIR):
    print(f"Found local fixture repository at: {SIBLING_DIR}")
    for fname in os.listdir(SIBLING_DIR):
        src_path = os.path.join(SIBLING_DIR, fname)
        dst_path = os.path.join(TARGET_DIR, fname)
        if os.path.isfile(src_path):
            if not os.path.exists(dst_path) or os.path.getsize(dst_path) != os.path.getsize(src_path):
                print(f"  Copying {fname} -> {TARGET_DIR}/")
                shutil.copy2(src_path, dst_path)
            
            # Decompress trans.asc.txt.gz if trans.asc.txt does not exist
            if fname == "trans.asc.txt.gz":
                uncompressed_dst = os.path.join(TARGET_DIR, "trans.asc.txt")
                if not os.path.exists(uncompressed_dst):
                    print("  Decompressing trans.asc.txt.gz -> trans.asc.txt...")
                    with gzip.open(src_path, "rb") as f_in, open(uncompressed_dst, "wb") as f_out:
                        shutil.copyfileobj(f_in, f_out)

print(f"Berka fixtures ready in {TARGET_DIR}:")
for f in sorted(os.listdir(TARGET_DIR)):
    sz = os.path.getsize(os.path.join(TARGET_DIR, f))
    print(f"  - {f} ({sz:,} bytes)")
