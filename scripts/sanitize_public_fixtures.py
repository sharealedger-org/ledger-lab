#!/usr/bin/env python3
"""Remove direct vendor contact and street-level location data from fixtures."""

import csv
import re
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

EMAIL_RE = re.compile(r"\b[^\s,;@]+@[^\s,;@]+\.[^\s,;@]+\b")
ADDRESS_RE = re.compile(
    r"(?:\bP\.?\s*O\.?\s*BOX\b|\b\d{1,6}\s+[^,\n]*\b"
    r"(?:ST|STREET|RD|ROAD|AVE|AVENUE|BLVD|BOULEVARD|DR|DRIVE|"
    r"HWY|HIGHWAY|WAY|CT|COURT|PKWY|PARKWAY|PL|PLACE|LN|LANE|"
    r"TER|TERRACE)\b)",
    re.IGNORECASE,
)


def rewrite_csv(path: Path, delimiter: str, transform):
    with path.open("r", newline="", encoding="utf-8") as source:
        rows = list(csv.reader(source, delimiter=delimiter))

    if not rows:
        raise ValueError(f"empty fixture: {path}")

    leading_blank_rows = []
    while rows and not any(rows[0]):
        leading_blank_rows.append(rows.pop(0))
    transformed = leading_blank_rows + transform(rows)
    with tempfile.NamedTemporaryFile(
        "w", newline="", encoding="utf-8", dir=path.parent, delete=False
    ) as target:
        csv.writer(target, delimiter=delimiter, lineterminator="\n").writerows(transformed)
        temporary_path = Path(target.name)
    temporary_path.replace(path)


def sanitize_po(rows):
    header = rows[0]
    positions = {name: index for index, name in enumerate(header)}
    required = {
        "VENDORADDRESS",
        "VENDORCITY",
        "VENDORPOSTALCODE",
        "VENDORLOC_EMAILADDRESS",
    }
    missing = required.difference(positions)
    if missing:
        raise ValueError(f"{missing} missing from PO fixture")

    for row in rows[1:]:
        if not row:
            continue
        if len(row) < len(header):
            row.extend([""] * (len(header) - len(row)))
        row[positions["VENDORADDRESS"]] = "REDACTED_ADDRESS"
        row[positions["VENDORCITY"]] = "REDACTED_CITY"
        row[positions["VENDORPOSTALCODE"]] = "00000"
        row[positions["VENDORLOC_EMAILADDRESS"]] = "redacted@example.invalid"
    return rows


def sanitize_vendor_master(rows):
    header = rows[0]
    positions = {name: index for index, name in enumerate(header)}
    required = {"instVendorAddress", "instVendorCity", "instVendorPostalCode"}
    missing = required.difference(positions)
    if missing:
        raise ValueError(f"{missing} missing from VendorMaster fixture")

    for row in rows[1:]:
        if not row:
            continue
        if len(row) < len(header):
            row.extend([""] * (len(header) - len(row)))
        row[positions["instVendorAddress"]] = "REDACTED_ADDRESS"
        row[positions["instVendorCity"]] = "REDACTED_CITY"
        row[positions["instVendorPostalCode"]] = "00000"
    return rows


def sanitize_vendor_updates(rows):
    for row in rows[1:]:
        for index, value in enumerate(row):
            if EMAIL_RE.search(value):
                row[index] = "REDACTED_EMAIL"
            elif ADDRESS_RE.search(value):
                row[index] = "REDACTED_ADDRESS"
    return rows


def main():
    rewrite_csv(ROOT / "data/VA_opendata_FY2003_small.txt", "\t", sanitize_po)
    rewrite_csv(ROOT / "data/VendorMaster.csv", ",", sanitize_vendor_master)
    rewrite_csv(ROOT / "data/VendorMaster_real_sample.csv", ",", sanitize_vendor_master)
    rewrite_csv(ROOT / "data/VendorUpdate.csv", ",", sanitize_vendor_updates)


if __name__ == "__main__":
    main()
