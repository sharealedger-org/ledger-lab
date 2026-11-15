#!/usr/bin/env bash
# Stable orchestration only: deterministic Scala synthetic input preparation.
set -euo pipefail
repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
out=""; instruments=32
while [[ $# -gt 0 ]]; do
  case "$1" in
    --out) out="$2"; shift 2 ;;
    --instruments) instruments="$2"; shift 2 ;;
    *) echo "Usage: $0 --out NEW_ABSOLUTE_DIRECTORY [--instruments 32]" >&2; exit 1 ;;
  esac
done
[[ "$out" =~ ^/[A-Za-z0-9_./-]+$ && ! -e "$out" ]] || { echo "Use a new absolute path containing letters, digits, slash, dot, underscore or hyphen" >&2; exit 1; }
[[ "$instruments" =~ ^[0-9]+$ ]] || exit 1
source_file="$repo/02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/corpus/SyntheticReferenceHistory.scala"
source_sha="$(LC_ALL=C shasum -a 256 "$source_file" | cut -d ' ' -f 1)"
mkdir -p "$(dirname "$out")"
if command -v caffeinate >/dev/null 2>&1; then caffeinate -i -s -w $$ & fi
cd "$repo/02-foundation/va_pipeline"
sbt -batch "runMain org.universalledger.foundation.va.corpus.SyntheticReferenceHistory --out $out --instruments $instruments --sourceSha256 $source_sha"
