#!/usr/bin/env bash
# Post-evaluation storage only: retain all paths/bytes using hard links for identical new-run files.
set -euo pipefail
reference="${1:?evaluated reference run required}"; target="${2:?evaluated target run required}"
mode="${3:-repeat}"
[[ -s "$reference/EVALUATED" && "$reference" != "$target" ]] || exit 2
case "$mode" in
  repeat) [[ -s "$target/EVALUATED" ]] || exit 2; cmp "$reference/products.sha256" "$target/products.sha256" || exit 2 ;;
  --matching-files-only) [[ -s "$target/EVALUATED" || -s "$target/PROFILE_NOT_ADMITTED" ]] || exit 2 ;;
  *) exit 2 ;;
esac
[[ ! -e "$target/DEDUPLICATED" ]] || exit 2
for path in "$reference" "$target"; do [[ "$path" = /* && "$path" != *[[:space:]]* ]] || exit 2; done
index="$target/storage_reference_index.tsv"
du -sk "$target" > "$target/storage_before_kib.txt"
df -Pk "$target" > "$target/volume_before_storage_links.txt"
: > "$index"
reference_dirs=(); target_dirs=()
for name in output report report_literal_v3; do
  [[ ! -d "$reference/$name" || -L "$reference/$name" ]] || reference_dirs+=("$reference/$name")
  [[ ! -d "$target/$name" || -L "$target/$name" ]] || target_dirs+=("$target/$name")
done
(( ${#reference_dirs[@]} && ${#target_dirs[@]} )) || exit 2
while IFS= read -r -d '' file; do
  digest="$(LC_ALL=C shasum -a 256 "$file")"; digest="${digest%% *}"
  printf '%s\t%s\n' "$digest" "$file" >> "$index"
done < <(find "${reference_dirs[@]}" -type f -name '*.csv' -size +1024k -print0)
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
done < <(find "${target_dirs[@]}" -type f -name '*.csv' -size +1024k -print0)
du -sk "$target" > "$target/storage_after_kib.txt"
df -Pk "$target" > "$target/volume_after_storage_links.txt"
# Per-run du includes blocks shared outside its root; df and the combined queue
# monitor identify actual volume space recovered by these hard links.
date -u +%FT%TZ > "$target/DEDUPLICATED"
