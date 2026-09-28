#!/usr/bin/env bash
# =============================================================================
#  data_aggregation.sh -- Step 5 ViewSpec Aggregation Wrapper
#
#  Usage:
#    bash scripts/data_aggregation.sh --years 2003,2004 --curve C2 --config full-views
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

$PYTHON scripts/data_aggregation.py "$@"
