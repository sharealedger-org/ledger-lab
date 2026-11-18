#!/bin/bash
# Restore the versioned report and launch the read-only Scala explorer.
set -euo pipefail
repo=$(cd "$(dirname "$0")/.." && pwd)
cd "$repo"
dest="$repo/session-run.tmp/q2-explorer-shared"
mkdir -p "$dest"
shasum -a 256 -c data/q2-development/PART_SHA256SUMS
cat data/q2-development/explorer_20261118_v1/relationships.csv.gz.part-* > "$dest/relationships.csv.gz"
cp data/q2-development/explorer_20261118_v1/SHA256SUMS "$dest/SHA256SUMS"
(cd "$dest" && shasum -a 256 -c SHA256SUMS)
gzip -dc "$dest/relationships.csv.gz" > "$dest/relationships.csv"
if [[ "${1:-}" == "--prepare-only" ]]; then
  printf 'Prepared report: %s\n' "$dest/relationships.csv"
  exit 0
fi
vendor=${1:-v000624713229648432276230057853276173}
year=${2:-2044}
[[ "$vendor" =~ ^[[:alnum:]_-]+$ && "$year" =~ ^[0-9]{4}$ ]] || { echo 'Expected vendor identifier and four-digit year' >&2; exit 2; }
cd "$repo/02-foundation/va_pipeline"
exec sbt "runMain org.universalledger.foundation.va.ledger.FinancialTupleExplorer \"$dest/relationships.csv\" $vendor $year"
