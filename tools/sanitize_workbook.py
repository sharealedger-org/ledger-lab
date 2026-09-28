#!/usr/bin/env python3
"""Sanitize an XLSX copy while preserving workbook structure and formulas."""

import re
import sys
import zipfile
from pathlib import Path

EMAIL_RE = re.compile(rb"\b[^\s@<>]+@[^\s@<>]+\.[A-Za-z]{2,}\b")
ADDRESS_RE = re.compile(
    rb"\b\d{1,6}\s+[^,\n<]*(?:street|road|avenue|blvd|drive|highway|parkway|lane|terrace)\b",
    re.IGNORECASE,
)
BOX_RE = re.compile(rb"\bP\.?\s*O\.?\s*BOX\b[^,\n<]*", re.IGNORECASE)
CREATOR_RE = re.compile(rb"(<dc:creator>)[^<]*(</dc:creator>)")
MODIFIED_BY_RE = re.compile(rb"(<cp:lastModifiedBy>)[^<]*(</cp:lastModifiedBy>)")
COMPANY_RE = re.compile(rb"(<Company>)[^<]*(</Company>)")
PATH_RE = re.compile(rb"<x15ac:absPath\b[^>]*/>")


def sanitize_xml(name: str, content: bytes) -> bytes:
    if name == "xl/sharedStrings.xml":
        content = EMAIL_RE.sub(b"redacted@example.invalid", content)
        content = BOX_RE.sub(b"REDACTED_ADDRESS", content)
        content = ADDRESS_RE.sub(b"REDACTED_ADDRESS", content)
    elif name == "docProps/core.xml":
        content = CREATOR_RE.sub(rb"\1ShareALedger\2", content)
        content = MODIFIED_BY_RE.sub(rb"\1ShareALedger\2", content)
    elif name == "docProps/app.xml":
        content = COMPANY_RE.sub(rb"\1ShareALedger\2", content)
    elif name == "xl/workbook.xml":
        content = PATH_RE.sub(b"", content)
    return content


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit("usage: sanitize_workbook.py INPUT.xlsx OUTPUT.xlsx")

    source = Path(sys.argv[1])
    target = Path(sys.argv[2])
    with zipfile.ZipFile(source, "r") as source_zip, zipfile.ZipFile(
        target, "w", compression=zipfile.ZIP_DEFLATED
    ) as target_zip:
        for item in source_zip.infolist():
            content = source_zip.read(item.filename)
            target_zip.writestr(item, sanitize_xml(item.filename, content))


if __name__ == "__main__":
    main()
