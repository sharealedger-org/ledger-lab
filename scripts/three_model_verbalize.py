#!/usr/bin/env python3
"""
Three-model experiment -- verbalizer (structured record -> text), plan step 2.

Scope (AGENTS.md): Python for post-engine model preparation only.  Owner approval received
2026-11-11 for the three-model experiment's training and evaluation scripts, not timed.
This script reads one committed output file (GL<yyyy>.csv or LDGR<yyyy>.csv) and writes text.
It does no accounting, posting, aggregation or sampling.  Which rows are used for training is
a separate step (docs/THREE_MODEL_EXPERIMENT_PLAN.md, step 3).

Decision D2 (docs/THREE_MODEL_EXPERIMENT_PLAN.md):
  * One template per substrate, listing only that substrate's columns, in file-header order.
  * No derived fields.  Values are the file's values with surrounding whitespace removed; an
    empty value is written as "blank".
  * A file whose header differs from the expected header is refused, so a schema change cannot
    silently change what a model sees.
  * Posted-FineTune reads the GL template only, so nothing outside the GL columns (for example
    the instrument id) can reach it.

Header sources: GL = GLPost.scala line 151; LDGR = post.scala ldgrHeader() (30 named columns;
the header line ends with a trailing comma, so each data row ends with one empty field).

Usage:
  python3 scripts/three_model_verbalize.py --substrate gl   --in GL2003.csv   --out gl.jsonl
  python3 scripts/three_model_verbalize.py --substrate ldgr --in LDGR2003.csv --out ldgr.jsonl

Output: one JSON object per line: {"id": "<file name>:<line number>", "text": "..."}.
The id is the source file and line, so a verbalized record can be traced to its source row.
"""

import argparse
import csv
import json
import re
import sys
from pathlib import Path

GL_HEADER = ["agency", "nominal_account_id", "fund", "object_id", "fiscal_year", "dir_flag", "total_amount"]

LDGR_HEADER = [
    "ldgrIPID", "ldgrContractID", "ldgrCommitmentID", "ldgrLdgrlID", "ldgrSourceSystemID",
    "ldgrLedgerID", "ldgrJrnlType", "ldgrBookCodeID", "ldgrLegalEntityID", "ldgrCenterID",
    "ldgrProjectID", "ldgrProductID", "ldgrNominalAccountID", "ldgrAltAccountID",
    "ldgrCurrencyCodeSourceID", "ldgrCurrencyTypeCodeSourceID", "ldgrCurrencyCodeTargetID",
    "ldgrCurrencyTypeCodeTargetID", "ldgrLedgerPeriod", "ldgrTransAmount", "ldgrUnitOfMeasure",
    "ldgrStatisticAmount", "ldgrDirVsOffsetFlg", "ldgrReconcileFlg", "ldgrAdjustFlg",
    "ldgrExtensionIDAuditTrail", "ldgrExtensionIDSource", "ldgrExtensionIDClass",
    "ldgrExtensionIDDates", "ldgrExtensionIDCustom",
]

SUBSTRATES = {
    "gl": {"header": GL_HEADER, "lead": "Posted general ledger balance"},
    "ldgr": {"header": LDGR_HEADER, "lead": "Instrument ledger entry"},
}


def label(column: str) -> str:
    """Readable label from a column name: drop the 'ldgr' prefix, split on '_' and camel case."""
    name = column[4:] if column.startswith("ldgr") else column
    name = name.replace("_", " ")
    name = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", " ", name)   # fooBar -> foo Bar
    name = re.sub(r"(?<=[A-Z])(?=[A-Z][a-z])", " ", name)  # IPIDName -> IPID Name
    return name.strip()


def verbalize(substrate: str, values: list) -> str:
    """One text line from one record's values (already matched to the substrate's header)."""
    spec = SUBSTRATES[substrate]
    header = spec["header"]
    if len(values) != len(header):
        raise ValueError(f"{substrate}: expected {len(header)} values, got {len(values)}")
    parts = []
    for column, value in zip(header, values):
        v = value.strip()
        parts.append(f"{label(column)}: {v if v else 'blank'}")
    return f"{spec['lead']}. " + "; ".join(parts) + "."


def read_records(substrate: str, path: Path):
    """Yield (line_number, values) for each data row; refuse a file with an unexpected header."""
    expected = SUBSTRATES[substrate]["header"]
    with open(path, newline="", encoding="utf-8-sig") as handle:
        reader = csv.reader(handle)
        header = next(reader, None)
        if header is None:
            raise ValueError(f"{path}: empty file")
        # The LDGR header ends with a trailing comma, which csv reads as one empty name.
        trailing = 0
        while header and header[-1] == "":
            header.pop()
            trailing += 1
        if header != expected:
            raise ValueError(f"{path}: header does not match the {substrate} schema")
        for row in reader:
            if not row:
                continue
            extra = row[len(expected):]
            if len(row) < len(expected) or len(extra) > trailing or any(c != "" for c in extra):
                raise ValueError(f"{path}: line {reader.line_num} has {len(row)} fields, expected {len(expected)}")
            yield reader.line_num, row[: len(expected)]


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--substrate", choices=sorted(SUBSTRATES), required=True)
    parser.add_argument("--in", dest="source", required=True, type=Path)
    parser.add_argument("--out", required=True, type=Path)
    args = parser.parse_args(argv)
    count = 0
    with open(args.out, "w", encoding="utf-8") as out:
        for line_number, values in read_records(args.substrate, args.source):
            record = {"id": f"{args.source.name}:{line_number}", "text": verbalize(args.substrate, values)}
            out.write(json.dumps(record, ensure_ascii=False) + "\n")
            count += 1
    print(f"verbalized {count} {args.substrate} records from {args.source.name}", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
