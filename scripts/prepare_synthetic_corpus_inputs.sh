#!/usr/bin/env bash
# Snapshot bounded scenario configuration; link immutable raw sources. Scala prepares reference data.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source_root=""; out=""; master=""
while [[ $# -gt 0 ]]; do
  [[ $# -ge 2 ]] || exit 1
  case "$1" in
    --root) source_root="$2"; shift 2 ;;
    --out) out="$2"; shift 2 ;;
    --vendorMaster) master="$2"; shift 2 ;;
    *) echo "Usage: $0 --root SOURCE_ROOT --out NEW_ROOT --vendorMaster FILE" >&2; exit 1 ;;
  esac
done
[[ -d "$source_root/VARawFiles" && -d "$source_root/VAPOData" && -s "$master" && "$out" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$out" ]] || exit 1
[[ "$source_root" =~ ^/[A-Za-z0-9_./-]+$ && "$master" =~ ^/[A-Za-z0-9_./-]+$ ]] || exit 1
mkdir -p "$(dirname "$out")"
mkdir "$out"
ln -s "$source_root/VARawFiles" "$out/VARawFiles"
ln -s "$source_root/VAPOData" "$out/VAPOData"
cp "$master" "$out/original_vendor_master.csv"
cp "$source_root/VAAccountingRules.csv" "$out/VAAccountingRules.csv"
cp -R "$repo/data/synthetic/reclass_history_v1" "$out/synthetic_histories"
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
(cd "$repo/02-foundation/va_pipeline" && sbt -batch "runMain org.universalledger.foundation.va.corpus.SyntheticReferenceBundle --in $source_root --out $out/reference_bundle --vendorMaster $out/original_vendor_master.csv --preparedMaster $out/VendorMaster.csv") > "$out/prepare_references.log" 2>&1
cp "$out/reference_bundle"/aRef*.txt "$out/"
printf 'source_root=%s\nmaster_source=%s\nreference_policy=synthetic_name_equivalence_v1\nfinancial_identity_rekeyed=false\nreclass_engine=deferred_to_Q2\n' "$source_root" "$master" > "$out/input_config.txt"
LC_ALL=C shasum -a 256 "$out"/aRef*.txt "$out/VendorMaster.csv" "$out/VAAccountingRules.csv" "$out/synthetic_histories"/*.csv > "$out/prepared_input_sha256.txt"
printf 'synthetic_connected_input_v1\n' > "$out/INPUTS_COMPLETE"
echo "Prepared synthetic corpus inputs: $out"
