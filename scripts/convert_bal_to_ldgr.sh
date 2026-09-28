#!/usr/bin/env bash
# =============================================================================
#  convert_bal_to_ldgr.sh -- Wrapper for convert_bal_to_ldgr.py
#
#  Usage:
#    bash scripts/convert_bal_to_ldgr.sh [--baldir DIR] [--outdir DIR] [--years 2003,2004]
# =============================================================================

set -euo pipefail

# Detect Python command
if command -v py &>/dev/null; then
  PYTHON="py -3"
elif command -v python3 &>/dev/null; then
  PYTHON="python3"
else
  PYTHON="python"
fi

$PYTHON scripts/convert_bal_to_ldgr.py "$@"
