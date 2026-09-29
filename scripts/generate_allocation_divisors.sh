#!/usr/bin/env bash
# Generate an allocation divisor side table from raw VA input or a ledger output.

set -euo pipefail

INPUT_FILE=""
OUTPUT_FILE=""
SOURCE_KIND="raw"
DIVISOR_PERIOD=""
SOURCE_PERIOD=""
SOURCE_PARTITION_ID=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --input) INPUT_FILE="$2"; shift 2 ;;
    --output) OUTPUT_FILE="$2"; shift 2 ;;
    --source-kind) SOURCE_KIND="$2"; shift 2 ;;
    --divisor-period) DIVISOR_PERIOD="$2"; shift 2 ;;
    --source-period) SOURCE_PERIOD="$2"; shift 2 ;;
    --source-partition-id) SOURCE_PARTITION_ID="$2"; shift 2 ;;
    -h|--help)
      cat <<'EOF'
Usage: bash scripts/generate_allocation_divisors.sh OPTIONS

Required:
  --input PATH                 Raw tab-delimited VA input or ledger CSV
  --output PATH                Divisor CSV to write
  --divisor-period PERIOD      Period represented by the divisor state
  --source-period PERIOD       Period that will consume the divisor
  --source-partition-id ID     Lineage ID for the source partition

Optional:
  --source-kind raw|ledger     Input shape (default: raw)

Raw input groups by agency and object code, producing EXP<object> driver accounts.
Ledger input groups by legal entity and nominal account, preserving the ledger account.
The output is deterministic and contains absolute driver totals.
EOF
      exit 0
      ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
done

for required_value in INPUT_FILE OUTPUT_FILE DIVISOR_PERIOD SOURCE_PERIOD SOURCE_PARTITION_ID; do
  if [[ -z "${!required_value}" ]]; then
    echo "Missing required option: $required_value" >&2
    exit 1
  fi
done

[[ -f "$INPUT_FILE" ]] || { echo "Input file not found: $INPUT_FILE" >&2; exit 1; }
mkdir -p "$(dirname "$OUTPUT_FILE")"

tmp_file="$(mktemp)"
cleanup() { rm -f "$tmp_file"; }
trap cleanup EXIT

printf '%s\n' 'divisorPeriod,sourcePeriod,allocationGroupKey,agency,driverAccount,divisorAmount,ruleVersion,sourcePartitionId' > "$OUTPUT_FILE"

if [[ "$SOURCE_KIND" == "raw" ]]; then
  LC_ALL=C awk -F '\t' \
    -v divisor_period="$DIVISOR_PERIOD" \
    -v source_period="$SOURCE_PERIOD" \
    -v source_partition_id="$SOURCE_PARTITION_ID" \
    '$1 !~ /AGY_AGENCY_KEY/ && NF >= 6 {
      gsub(/"/, "", $1); gsub(/"/, "", $3); gsub(/"/, "", $6)
      amount = ($6 + 0); if (amount < 0) amount = -amount
      totals[$1 SUBSEP $3] += amount
    }
    END {
      for (key in totals) {
        split(key, fields, SUBSEP)
        printf "%s\t%s\t%s\t%s\t%.2f\n", fields[1], fields[2], "EXP" fields[2], totals[key], totals[key]
      }
    }' "$INPUT_FILE" | sort -t $'\t' -k1,1 -k2,2n > "$tmp_file"
else
  if [[ "$SOURCE_KIND" != "ledger" ]]; then
    echo "Unsupported --source-kind: $SOURCE_KIND" >&2
    exit 1
  fi
  awk -F ',' \
    -v divisor_period="$DIVISOR_PERIOD" \
    -v source_period="$SOURCE_PERIOD" \
    -v source_partition_id="$SOURCE_PARTITION_ID" \
    'NR > 1 && NF >= 20 {
      agency = $9; account = $13; amount = ($20 + 0)
      if (amount < 0) amount = -amount
      totals[agency SUBSEP account] += amount
    }
    END {
      for (key in totals) {
        split(key, fields, SUBSEP)
        printf "%s\t%s\t%s\t%s\t%.2f\n", fields[1], fields[2], fields[1] "|" fields[2], totals[key], totals[key]
      }
    }' "$INPUT_FILE" | sort -t $'\t' -k1,1 -k2,2n > "$tmp_file"
fi

awk -F '\t' -v divisor_period="$DIVISOR_PERIOD" \
  -v source_period="$SOURCE_PERIOD" \
  -v source_partition_id="$SOURCE_PARTITION_ID" \
  'BEGIN { OFS="," }
  { print divisor_period, source_period, $3, $1, $2, $5, "allocation-v1", source_partition_id }' "$tmp_file" >> "$OUTPUT_FILE"

row_count="$(awk 'END { print NR - 1 }' "$OUTPUT_FILE")"
printf 'Generated %s divisor groups at %s\n' "$row_count" "$OUTPUT_FILE"
