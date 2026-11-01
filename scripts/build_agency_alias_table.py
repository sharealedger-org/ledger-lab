#!/usr/bin/env python3
"""
build_agency_alias_table.py -- observe agency names in the APA files and relate them to aRefAgencyCSV.txt.

Preparation/inspection only (AGENTS.md). Streams every APA file once; no file is loaded whole.
Reads the catalog written by scripts/profile_corpus.sh so each input is identified by its SHA-256.

Outputs (in --out, default data/corpus_profile):
  agency_name_observations.csv  one row per (observed name, family, fiscal year): row count
  agency_alias_candidates.csv   one row per distinct observed name, with the reference relation
  agency_alias_run.txt          inputs (file count, catalog digest), script version, counts in/out

No alias is asserted. A relation is one of:
  EXACT_REF_NAME       name equals a reference name (case/space-insensitive); may map to >1 reference key
  NORMALIZED_REF_NAME  equals a reference name only after dropping punctuation and "&"/"AND"/"THE"
  UNMATCHED            no reference name matches; a human decision is needed
valid_from / valid_to are 'unknown' unless the observed first/last fiscal year is quoted explicitly as
`observed_first_fy` / `observed_last_fy` (observation window, NOT an organizational validity period).
"""
import argparse
import csv
import hashlib
import re
import sys
from collections import defaultdict
from pathlib import Path

VERSION = "agency-alias-1"
csv.field_size_limit(sys.maxsize)
FAMILIES = ("apa_expenditure", "apa_revenue", "apa_budget")


def squash(s):
    return re.sub(r"\s+", " ", s.strip()).upper()


def normalize(s):
    s = squash(s).replace("&", " AND ")
    s = re.sub(r"[^A-Z0-9 ]", " ", s)
    s = re.sub(r"\b(THE|OF|AND)\b", " ", s)
    return re.sub(r"\s+", " ", s).strip()


def data_rows(path):
    """Yield dict rows, skipping SQL*Plus trailer lines. Uses the file's own header."""
    with open(path, encoding="utf-8-sig", errors="replace", newline="") as f:
        reader = csv.reader(f)
        header = next(reader)
        ncols = len(header)
        idx = header.index("AGENCY_NAME")
        for row in reader:
            if len(row) == ncols:
                yield row[idx]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--root", default="data")
    ap.add_argument("--out", default="data/corpus_profile")
    a = ap.parse_args()
    out = Path(a.out)

    cat_path = out / "source_catalog.csv"
    cat_bytes = cat_path.read_bytes()
    with open(cat_path, encoding="utf-8", newline="") as f:
        cat = [r for r in csv.DictReader(f)
               if r["source_family"] in FAMILIES and r["inspection_status"].startswith("PROFILED")]
    cat.sort(key=lambda r: r["rel_path"])

    obs = defaultdict(int)  # (name, family, fy) -> rows
    for i, e in enumerate(cat, 1):
        for name in data_rows(Path(a.root) / e["rel_path"]):
            obs[(name, e["source_family"], e["fiscal_year_claimed"])] += 1
        if i % 50 == 0:
            print(f"[{i}/{len(cat)}] {e['rel_path']}", file=sys.stderr, flush=True)

    with open(Path(a.root) / "aRefAgencyCSV.txt", encoding="utf-8-sig", newline="") as f:
        ref = list(csv.DictReader(f))
    by_exact, by_norm = defaultdict(list), defaultdict(list)
    for r in ref:
        by_exact[squash(r["AGY_AGENCY_NAME"])].append(r)
        by_norm[normalize(r["AGY_AGENCY_NAME"])].append(r)

    with open(out / "agency_name_observations.csv", "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["observed_name", "source_family", "fiscal_year", "rows"])
        for (n, fam, fy), c in sorted(obs.items()):
            w.writerow([n, fam, fy, c])

    names = defaultdict(lambda: {"rows": 0, "fys": set(), "fams": set()})
    for (n, fam, fy), c in obs.items():
        d = names[n]
        d["rows"] += c; d["fys"].add(int(fy)); d["fams"].add(fam)

    counts = defaultdict(int)
    rows_by_rel = defaultdict(int)
    with open(out / "agency_alias_candidates.csv", "w", encoding="utf-8", newline="") as f:
        w = csv.writer(f, lineterminator="\n")
        w.writerow(["observed_name", "relation", "ref_agency_keys", "ref_agency_codes", "ref_names",
                    "ambiguous", "observed_first_fy", "observed_last_fy", "families", "rows",
                    "valid_from", "valid_to", "evidence"])
        for n in sorted(names):
            d = names[n]
            hits = by_exact.get(squash(n))
            rel = "EXACT_REF_NAME"
            if not hits:
                hits = by_norm.get(normalize(n)); rel = "NORMALIZED_REF_NAME"
            if not hits:
                rel = "UNMATCHED"; hits = []
            keys = sorted({h["AGY_AGENCY_KEY"] for h in hits}, key=int)
            codes = sorted({h["AGY_AGENCY_CODE"] for h in hits})
            rnames = sorted({h["AGY_AGENCY_NAME"] for h in hits})
            ambiguous = len(codes) > 1 or len(keys) > 1
            ev = {"EXACT_REF_NAME": "observed name equals reference name in aRefAgencyCSV.txt",
                  "NORMALIZED_REF_NAME": "equal after dropping punctuation and the words THE/OF/AND; not an exact match",
                  "UNMATCHED": "no reference name matches; no relation asserted"}[rel]
            w.writerow([n, rel, ";".join(keys), ";".join(codes), ";".join(rnames),
                        "true" if ambiguous else "false", min(d["fys"]), max(d["fys"]),
                        ";".join(sorted(d["fams"])), d["rows"], "unknown", "unknown", ev])
            counts[rel] += 1
            rows_by_rel[rel] += d["rows"]

    total_rows = sum(obs.values())
    with open(out / "agency_alias_run.txt", "w", encoding="utf-8", newline="") as f:
        f.write(f"script_version={VERSION}\n")
        f.write(f"input_files={len(cat)}\n")
        f.write(f"source_catalog_sha256={hashlib.sha256(cat_bytes).hexdigest()}\n")
        f.write(f"agency_reference_rows={len(ref)}\n")
        f.write(f"rows_observed={total_rows}\n")
        f.write(f"distinct_names={len(names)}\n")
        for rel in ("EXACT_REF_NAME", "NORMALIZED_REF_NAME", "UNMATCHED"):
            f.write(f"{rel}.names={counts[rel]}\n{rel}.rows={rows_by_rel[rel]}\n")
        f.write("note=rows_observed counts accepted data rows only; SQL*Plus trailer lines and short/long rows are excluded here\n")
    print(open(out / "agency_alias_run.txt").read())


if __name__ == "__main__":
    main()
