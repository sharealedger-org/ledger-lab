#!/usr/bin/env python3
"""
corpus_inspect.py -- read-only inspection helpers for the Virginia source corpus.

Preparation/inspection only (AGENTS.md: Python is permitted here). It reads the files
that scripts/profile_corpus.sh catalogued; it posts nothing and writes only the sample
file you ask for. It is not part of any cost-curve measurement.

Subcommands
  files   list catalogued files for a family and/or fiscal year
  fields  show the header and the per-column profile of one file
  fetch   re-read one record by file + start_byte_offset (optionally verify SHA-256)
  sample  deterministic sample of single-line records plus quarantined originals

All commands take --profile DIR (default data/corpus_profile) and --root DIR
(default data) so they work against any runtime root.
"""
import argparse
import csv
import hashlib
import sys
from pathlib import Path

csv.field_size_limit(sys.maxsize)
DELIMS = {"apa_expenditure": ",", "apa_revenue": ",", "apa_budget": ",", "eva_po": "\t"}


def read_csv(path):
    with open(path, encoding="utf-8", newline="") as f:
        return list(csv.DictReader(f))


def catalog(profile):
    return read_csv(Path(profile) / "source_catalog.csv")


def find_entry(cat, rel):
    for r in cat:
        if r["rel_path"] == rel:
            return r
    sys.exit(f"not in catalog: {rel}")


def selected(cat, family, fy):
    for r in cat:
        if family and r["source_family"] != family:
            continue
        if fy and r["fiscal_year_claimed"] != str(fy):
            continue
        yield r


def cmd_files(a):
    for r in selected(catalog(a.profile), a.family, a.fy):
        print("\t".join([r["rel_path"], r["sha256"][:16], r["bytes"], r["source_family"],
                         r["fiscal_year_claimed"], r["period_claimed"], r["inspection_status"]]))


def physical_lines(path):
    """Yield (start_byte_offset, start_line, raw_bytes_without_newline). Offsets include any BOM."""
    offset = 0
    with open(path, "rb") as f:
        for n, raw in enumerate(f, 1):
            yield offset, n, raw.rstrip(b"\n").rstrip(b"\r")
            offset += len(raw)


def header_of(path, delim):
    for _, _, raw in physical_lines(path):
        text = raw.decode("utf-8", "replace").lstrip("\ufeff")
        if text.strip():
            return next(csv.reader([text], delimiter=delim))
    return []


def cmd_fields(a):
    cat = catalog(a.profile)
    e = find_entry(cat, a.file)
    delim = DELIMS.get(e["source_family"])
    if not delim:
        sys.exit(f"{a.file} has no parsed profile (status {e['inspection_status']})")
    print("schema_id:", e["schema_id"], "| family:", e["source_family"], "| status:", e["inspection_status"])
    prof = {r["col_name"]: r for r in read_csv(Path(a.profile) / "file_field_profile.csv")
            if r["rel_path"] == a.file}
    print("idx\tcolumn\tnonblank\tblank\tmin_len\tmax_len\tnumeric_looking\tleading_zero\tspace_padded")
    for i, name in enumerate(header_of(Path(a.root) / a.file, delim), 1):
        p = prof.get(name, {})
        print("\t".join([str(i), name] + [p.get(k, "") for k in
              ("nonblank", "blank", "min_len", "max_len", "numeric_looking", "leading_zero_digits", "space_padded")]))


def sha256_of(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def cmd_fetch(a):
    cat = catalog(a.profile)
    e = find_entry(cat, a.file)
    path = Path(a.root) / a.file
    if a.verify_checksum:
        if sha256_of(path) != e["sha256"]:
            sys.exit("CHECKSUM MISMATCH: file differs from the catalogued snapshot; offsets are not valid")
        print("# checksum ok", file=sys.stderr)
    with open(path, "rb") as f:
        f.seek(a.offset)
        for _ in range(a.lines):
            line = f.readline()
            if not line:
                break
            sys.stdout.write(line.decode("utf-8", "replace").rstrip("\r\n") + "\n")


def cmd_sample(a):
    cat = list(selected(catalog(a.profile), a.family, a.fy))
    quar = None
    if a.quarantine_per_reason:
        quar = read_csv(Path(a.profile) / "quarantine.csv")
    rows_out = 0
    with open(a.out, "w", encoding="utf-8", newline="") as out:
        w = csv.writer(out)
        w.writerow(["sample_id", "rel_path", "file_id", "start_byte_offset", "start_line",
                    "disposition_hint", "reason", "synthetic", "raw_line"])
        for e in cat:
            delim = DELIMS.get(e["source_family"])
            if not delim or e["inspection_status"] not in ("PROFILED_CLEAN", "PROFILED_WITH_QUARANTINE"):
                continue
            path = Path(a.root) / e["rel_path"]
            file_id = e["sha256"][:16]
            ncols = len(header_of(path, delim))
            header_seen = False
            for offset, line_no, raw in physical_lines(path):
                text = raw.decode("utf-8", "replace").lstrip("\ufeff")
                if not text.strip():
                    continue
                if not header_seen:
                    header_seen = True
                    continue
                sid = f"{file_id}@{offset}"
                if int(hashlib.sha256(sid.encode()).hexdigest()[:8], 16) % a.every != 0:
                    continue
                if len(next(csv.reader([text], delimiter=delim))) != ncols:
                    continue
                w.writerow([sid, e["rel_path"], file_id, offset, line_no,
                            "SINGLE_LINE_FIELD_COUNT_OK", "", "false", text])
                rows_out += 1
            if quar:
                taken = {}
                for q in quar:
                    if q["rel_path"] != e["rel_path"]:
                        continue
                    k = taken.get(q["reason"], 0)
                    if k >= a.quarantine_per_reason:
                        continue
                    taken[q["reason"]] = k + 1
                    w.writerow([f"{file_id}@{q['start_byte_offset']}", e["rel_path"], file_id,
                                q["start_byte_offset"], q["start_line"], "QUARANTINED", q["reason"],
                                "false", q["raw_escaped"]])
                    rows_out += 1
    print(f"wrote {rows_out} rows to {a.out}", file=sys.stderr)


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--profile", default="data/corpus_profile")
    p.add_argument("--root", default="data")
    sub = p.add_subparsers(dest="cmd", required=True)

    s = sub.add_parser("files"); s.add_argument("--family"); s.add_argument("--fy", type=int)
    s.set_defaults(fn=cmd_files)
    s = sub.add_parser("fields"); s.add_argument("--file", required=True); s.set_defaults(fn=cmd_fields)
    s = sub.add_parser("fetch"); s.add_argument("--file", required=True)
    s.add_argument("--offset", type=int, required=True); s.add_argument("--lines", type=int, default=1)
    s.add_argument("--verify-checksum", action="store_true"); s.set_defaults(fn=cmd_fetch)
    s = sub.add_parser("sample"); s.add_argument("--family"); s.add_argument("--fy", type=int)
    s.add_argument("--every", type=int, default=100000, help="keep ~1 in N single-line records")
    s.add_argument("--quarantine-per-reason", type=int, default=3)
    s.add_argument("--out", required=True); s.set_defaults(fn=cmd_sample)

    a = p.parse_args()
    a.fn(a)


if __name__ == "__main__":
    main()
