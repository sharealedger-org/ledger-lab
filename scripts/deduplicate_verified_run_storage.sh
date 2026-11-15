#!/usr/bin/env bash
# Post-evaluation storage only: retain all paths/bytes using hard links for identical new-run files.
set -euo pipefail
reference="${1:?evaluated reference run required}"; target="${2:?evaluated target run required}"
[[ -s "$reference/EVALUATED" && -s "$target/EVALUATED" && "$reference" != "$target" ]] || exit 2
cmp "$reference/products.sha256" "$target/products.sha256" || exit 2
[[ ! -e "$target/DEDUPLICATED" ]] || exit 2
for path in "$reference" "$target"; do [[ "$path" = /* && "$path" != *[[:space:]]* ]] || exit 2; done
index="$target/storage_reference_index.tsv"
du -sk "$target" > "$target/storage_before_kib.txt"
df -Pk "$target" > "$target/volume_before_storage_links.txt"
: > "$index"
while IFS= read -r -d '' file; do
  digest="$(LC_ALL=C shasum -a 256 "$file")"; digest="${digest%% *}"
  printf '%s\t%s\n' "$digest" "$file" >> "$index"
done < <(find "$reference/output" "$reference/report" -type f -name '*.csv' -size +1024k -print0)
printf 'target,reference,sha256\n' > "$target/storage_links.csv"
while IFS= read -r -d '' file; do
  digest="$(LC_ALL=C shasum -a 256 "$file")"; digest="${digest%% *}"
  match="$(awk -F '\t' -v key="$digest" '$1==key {print $2; exit}' "$index")"
  [[ -n "$match" ]] || continue
  [[ ! "$file" -ef "$match" ]] || continue
  cmp -s "$match" "$file" || exit 1
  [[ ! -e "$file.verified-link" ]] || exit 1
  ln "$match" "$file.verified-link"
  mv -f "$file.verified-link" "$file"
  printf '%s,%s,%s\n' "$file" "$match" "$digest" >> "$target/storage_links.csv"
done < <(find "$target/output" "$target/report" -type f -name '*.csv' -size +1024k -print0)
du -sk "$target" > "$target/storage_after_kib.txt"
df -Pk "$target" > "$target/volume_after_storage_links.txt"
# Per-run du includes blocks shared outside its root; df and the combined queue
# monitor identify actual volume space recovered by these hard links.
date -u +%FT%TZ > "$target/DEDUPLICATED"
