"""
zenodo_upload.py — Upload and correct the Virginia corpus Zenodo archive.

Usage (from ledger-lab/):
    py -3 scripts/zenodo_upload.py --dry-run
    py -3 scripts/zenodo_upload.py

Requires:
    ZENODO_TOKEN environment variable set to a valid Zenodo personal access token.
    data/corpus_profile/source_catalog.csv (produced by scripts/profile_corpus.sh)
    data/VARawFiles/   (APA expenditure/revenue files, git-ignored)
    data/VAPOData/     (eVA PO files)
    data/VendorDownload/supplier_snapshot_2026-10-31/  (supplier snapshot, git-ignored)

What this script does:
    1. Creates a new draft version of the existing eVA/PO record (23846362) and corrects
       its metadata: license -> other-pd, title, description (fixes FY2005/FY2009 gap
       statement, removes "agency code"/"sub-program", notes derived vendor file).
       Does NOT re-upload the 47 existing files; they carry over automatically.
    2. Creates four new records for the APA expenditure and revenue files (84 files each):
         B. APA Expenditure FY2003-FY2009
         C. APA Expenditure FY2010-FY2016
         D. APA Revenue FY2003-FY2009
         E. APA Revenue FY2010-FY2016
    3. Creates one new record for the eVA supplier directory snapshot (2026-10-31).
    4. After all records are created, cross-links them via related_identifiers and publishes.

Each record's token is read from ZENODO_TOKEN; it is never logged or written to disk.
Progress is written to zenodo_upload_progress.json in the ledger-lab directory so that
an interrupted run can resume without re-uploading already-completed files.
"""

import argparse
import csv
import hashlib
import http.client
import json
import mimetypes
import os
import pathlib
import sys
import time
import urllib.request
import urllib.error

# ── constants ─────────────────────────────────────────────────────────────────

ZENODO_BASE = "https://zenodo.org"
EXISTING_RECORD_ID = "23846362"
PROGRESS_FILE = pathlib.Path("zenodo_upload_progress.json")

# Supplier snapshot files to upload (relative to the snapshot dir)
SNAPSHOT_DIR = pathlib.Path("data/VendorDownload/supplier_snapshot_2026-10-31")
SNAPSHOT_FILES = [
    "suppliers.csv",
    "audit.json",
    "manifest.json",
    "source_export/suppliers_source.csv",
    "source_export/manifest.json",
    "source_export/reconciliation.json",
    "historical_candidate_details/historical_candidate_vlin_verification.csv",
]

CORRECTED_TITLE = (
    "Virginia APA Expenditure/Revenue/Budget and eVA Procurement Data "
    "FY2001\u2013FY2018 (Ledger Lab Research Archive)"
)

CORRECTED_DESCRIPTION = """\
<p>This archive contains Virginia state government financial and procurement data used as the \
empirical basis for <em>The Architecture of Economic State</em> (Twitchell), a nine-paper \
monograph examining minimum-cost accounting architectures for financial systems.</p>

<p>The archive preserves two datasets from Virginia open data sources:</p>

<p><strong>1. Virginia APA Expenditure, Revenue, and Budget Data (FY2003&ndash;FY2016)</strong><br>
Source: Virginia Auditor of Public Accounts, datapoint.apa.virginia.gov<br>
Monthly CSV files for each fiscal year covering all state agency expenditures, revenues, and \
budget appropriations. Fields are names only: AGENCY_NAME, FUND_NAME, OBJECT_NAME, PROGRAM_NAME, \
VENDOR_NAME, AMOUNT, VOUCHER_NUMBER, VOUCHER_DATE, LOCALITY. There are no agency codes or \
sub-program codes in these files. See CORPUS_SCHEMAS.md in the related repository for full \
schema documentation.</p>

<p><em>The 336 monthly expenditure and revenue files (21.8 GB) are published in four companion \
records linked as related identifiers: APA Expenditure FY2003&ndash;FY2009, APA Expenditure \
FY2010&ndash;FY2016, APA Revenue FY2003&ndash;FY2009, and APA Revenue FY2010&ndash;FY2016.</em></p>

<p><strong>2. Virginia eVA Procurement / Purchase Order Data (FY2001&ndash;FY2018)</strong><br>
Source: Virginia Department of General Services, eVA electronic procurement system, data.virginia.gov<br>
Tab-delimited files with one row per purchase order line. Fields include PO number, agency, \
vendor ID (documented as DUNS number in the local data dictionary; also includes alphanumeric \
identifiers and a 12-nines placeholder), vendor name, address, NIGP commodity code and description, \
quantity, unit price, and unit of measure. Vendor business contact information (email addresses, \
postal addresses) is included as published in the original government source.</p>

<p><strong>Known data gaps:</strong> FY2005 eVA file contains 1,037 parsed records (not a stub). \
FY2009 has no .txt file; only a zero-byte placeholder exists locally. FY2003 and FY2018 are partial \
years ending in February 2003 and January 2018 respectively. FY2017 extended-price total is \
anomalously large due to six repeated lines; do not quote PO dollar totals without checking \
extremes. 325,843 PO records are quarantined due to embedded line breaks in free-text fields.</p>

<p><strong>Derived file note:</strong> vendor_po_attributes_by_year.csv is a derived summary \
file, not source data. It has known defects (8,101 column-shifted rows from embedded line breaks, \
no provenance header). Do not treat it as ground truth for vendor identity.</p>

<p>FY2003&ndash;FY2015 eVA files are no longer available on data.virginia.gov as of 2026; this \
archive is the only public copy. FY2003&ndash;FY2016 APA expenditure files remain on \
datapoint.apa.virginia.gov but are archived here for long-term reproducibility.</p>

<p>NIGP commodity code descriptions are reproduced only as embedded fields in transaction rows, \
not as a standalone code table.</p>

<p><strong>License:</strong> Released government data, public domain. These are Virginia state \
government records. No license was provided by the source. The repo asserts public domain status; \
this is not a legal finding. See CORPUS_ACCESS.md in the related repository for full redistribution \
notes.</p>"""

APA_EXP_EARLY_TITLE = "Virginia APA Expenditure Data FY2003\u2013FY2009 (Ledger Lab Research Archive)"
APA_EXP_LATE_TITLE  = "Virginia APA Expenditure Data FY2010\u2013FY2016 (Ledger Lab Research Archive)"
APA_REV_EARLY_TITLE = "Virginia APA Revenue Data FY2003\u2013FY2009 (Ledger Lab Research Archive)"
APA_REV_LATE_TITLE  = "Virginia APA Revenue Data FY2010\u2013FY2016 (Ledger Lab Research Archive)"
SUPPLIER_TITLE      = "Virginia eVA Supplier Directory Snapshot 2026-10-31 (Ledger Lab Research Archive)"

APA_EXP_DESCRIPTION_TEMPLATE = """\
<p>Monthly CSV files for Virginia state agency expenditure data, {fy_range}, from the Virginia \
Auditor of Public Accounts (datapoint.apa.virginia.gov). {n_files} files covering {n_years} \
fiscal years ({fy_list}). Total size approximately {size_gb:.1f} GB.</p>

<p>Fields: AGENCY_NAME, FUND_NAME, OBJECT_NAME, PROGRAM_NAME, VENDOR_NAME, AMOUNT, \
VOUCHER_NUMBER, VOUCHER_DATE, LOCALITY. These are name fields only; there are no numeric agency \
codes or sub-program codes.</p>

<p>Part of the Virginia corpus for <em>The Architecture of Economic State</em> (Twitchell). \
See the parent record (10.5281/zenodo.23846362) for the eVA procurement data, budget files, \
and full corpus documentation.</p>

<p>Released government data, public domain.</p>"""

APA_REV_DESCRIPTION_TEMPLATE = """\
<p>Monthly CSV files for Virginia state agency revenue data, {fy_range}, from the Virginia \
Auditor of Public Accounts (datapoint.apa.virginia.gov). {n_files} files covering {n_years} \
fiscal years ({fy_list}). Total size approximately {size_gb:.1f} GB.</p>

<p>Fields: AGENCY_NAME, FUND_NAME, OBJECT_NAME, PROGRAM_NAME, AMOUNT, VOUCHER_NUMBER, \
VOUCHER_DATE, LOCALITY. These are name fields only.</p>

<p>Part of the Virginia corpus for <em>The Architecture of Economic State</em> (Twitchell). \
See the parent record (10.5281/zenodo.23846362) for the eVA procurement data, budget files, \
and full corpus documentation.</p>

<p>Released government data, public domain.</p>"""

SUPPLIER_DESCRIPTION = """\
<p>A dated snapshot of the Virginia eVA public supplier directory, captured 2026-10-31. \
Source: logi.cgieva.com/External/rdPage.aspx (Report9001_Data public report).</p>

<p><strong>Contents of this record:</strong></p>
<ul>
<li>suppliers.csv &mdash; 230,128 canonical supplier/location observations with rich HTML \
fields and source provenance. Includes eVA supplier ID (not a DUNS or Cardinal vendor ID), \
supplier name, location name, address, registration type (Self/State), active status from HTML \
(Active=230,068; Pending Approval=60), SWaM classifications, and payment capability flags. \
Two records have missing supplier IDs and are retained with quality flags.</li>
<li>audit.json &mdash; integrity audit: 230,128 observations, 230,126 nonblank IDs, \
2,912 retained source pages, data_integrity_verified=true.</li>
<li>manifest.json &mdash; acquisition scope, counts, completion basis, checksums.</li>
<li>source_export/suppliers_source.csv &mdash; unchanged full source CSV export \
(UTF-8 BOM, 230,128 rows). Note: the column named "Status" in this file contains \
Self/State registration type, not Active/Pending status.</li>
<li>source_export/manifest.json &mdash; exact request, headers, encoding, SHA-256.</li>
<li>source_export/reconciliation.json &mdash; complete field coverage reconciliation.</li>
<li>historical_candidate_details/historical_candidate_vlin_verification.csv &mdash; \
VLIN verification for 21 historical PO candidate suppliers (20 agreeing, 2 unresolved).</li>
</ul>

<p>Import supplier IDs and postal codes as strings. This snapshot covers the public \
main-directory report only; it does not cover the separate inactive directory or every \
Commonwealth payee. Current directory observations do not establish historical \
FY2003&ndash;FY2016 identities.</p>

<p>Acquisition interval: 2026-11-01T00:34&ndash;01:56 UTC. Not an atomic source-system \
snapshot.</p>

<p>Part of the Virginia corpus for <em>The Architecture of Economic State</em> (Twitchell). \
Related to the eVA procurement record (10.5281/zenodo.23846362).</p>

<p>Released government data, public domain.</p>"""

COMMON_METADATA_FIELDS = {
    "creators": [{"name": "Commonwealth of Virginia"}],
    "license": {"id": "other-pd"},
    "access_right": "open",
    "resource_type": {"id": "dataset"},
}


# ── helpers ───────────────────────────────────────────────────────────────────

def get_token():
    token = os.environ.get("ZENODO_TOKEN", "").strip()
    if not token:
        print("ERROR: ZENODO_TOKEN environment variable is not set.", file=sys.stderr)
        sys.exit(1)
    return token


def api(method, path, token, data=None, is_file=False, filepath=None,
        filename=None, content_type=None):
    """Make a Zenodo REST API call. Returns (status_code, parsed_json_or_None)."""
    url = ZENODO_BASE + path
    headers = {"Authorization": f"Bearer {token}"}

    if filepath is not None:
        # File upload — stream from disk
        size = os.path.getsize(filepath)
        headers["Content-Type"] = content_type or "application/octet-stream"
        headers["Content-Length"] = str(size)
        with open(filepath, "rb") as fh:
            req = urllib.request.Request(url, data=fh, headers=headers, method=method)
            try:
                with urllib.request.urlopen(req, timeout=600) as resp:
                    body = resp.read()
                    return resp.status, json.loads(body) if body else None
            except urllib.error.HTTPError as e:
                body = e.read()
                return e.code, json.loads(body) if body else None
    else:
        # JSON request
        body = json.dumps(data).encode() if data is not None else b""
        if body:
            headers["Content-Type"] = "application/json"
        req = urllib.request.Request(url, data=body or None, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=60) as resp:
                rbody = resp.read()
                return resp.status, json.loads(rbody) if rbody else None
        except urllib.error.HTTPError as e:
            rbody = e.read()
            try:
                err = json.loads(rbody)
            except Exception:
                err = rbody.decode(errors="replace")
            return e.code, err


def md5_file(path):
    h = hashlib.md5()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def sha256_file(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def load_progress():
    if PROGRESS_FILE.exists():
        return json.loads(PROGRESS_FILE.read_text(encoding="utf-8"))
    return {}


def save_progress(prog):
    PROGRESS_FILE.write_text(json.dumps(prog, indent=2), encoding="utf-8")


def load_catalog():
    """Return source_catalog rows as a list of dicts."""
    cat_path = pathlib.Path("data/corpus_profile/source_catalog.csv")
    if not cat_path.exists():
        print(f"ERROR: {cat_path} not found. Run scripts/profile_corpus.sh first.", file=sys.stderr)
        sys.exit(1)
    return list(csv.DictReader(open(cat_path, encoding="utf-8-sig")))


def apa_groups(catalog):
    """Return the four APA file groups as (label, title, description, [catalog_row, ...])."""
    def desc(template, rows, fy_range, fy_list):
        n_years = len(set(r["fiscal_year_claimed"] for r in rows))
        size_gb = sum(int(r["bytes"]) for r in rows if r["bytes"].isdigit()) / 1e9
        return template.format(
            fy_range=fy_range, n_files=len(rows), n_years=n_years,
            fy_list=fy_list, size_gb=size_gb
        )

    exp_rows = sorted(
        [r for r in catalog if r["source_family"] == "apa_expenditure"],
        key=lambda r: r["rel_path"]
    )
    rev_rows = sorted(
        [r for r in catalog if r["source_family"] == "apa_revenue"],
        key=lambda r: r["rel_path"]
    )

    exp_early = [r for r in exp_rows if int(r["fiscal_year_claimed"]) <= 2009]
    exp_late  = [r for r in exp_rows if int(r["fiscal_year_claimed"]) >= 2010]
    rev_early = [r for r in rev_rows if int(r["fiscal_year_claimed"]) <= 2009]
    rev_late  = [r for r in rev_rows if int(r["fiscal_year_claimed"]) >= 2010]

    return [
        ("apa_exp_early", APA_EXP_EARLY_TITLE,
         desc(APA_EXP_DESCRIPTION_TEMPLATE, exp_early, "FY2003\u2013FY2009", "FY2003 through FY2009"),
         exp_early),
        ("apa_exp_late",  APA_EXP_LATE_TITLE,
         desc(APA_EXP_DESCRIPTION_TEMPLATE, exp_late,  "FY2010\u2013FY2016", "FY2010 through FY2016"),
         exp_late),
        ("apa_rev_early", APA_REV_EARLY_TITLE,
         desc(APA_REV_DESCRIPTION_TEMPLATE, rev_early, "FY2003\u2013FY2009", "FY2003 through FY2009"),
         rev_early),
        ("apa_rev_late",  APA_REV_LATE_TITLE,
         desc(APA_REV_DESCRIPTION_TEMPLATE, rev_late,  "FY2010\u2013FY2016", "FY2010 through FY2016"),
         rev_late),
    ]


# ── step 1: new version of existing record ────────────────────────────────────

def step1_new_version(token, prog, dry_run):
    print("\n=== Step 1: New version of existing record", EXISTING_RECORD_ID, "===")
    if prog.get("step1_published"):
        print("  Already published. Skipping.")
        return prog.get("step1_new_record_id")

    # Create a draft new version
    if not prog.get("step1_draft_id"):
        if dry_run:
            print("  [dry-run] POST /api/records/{id}/versions")
            prog["step1_draft_id"] = "DRY_RUN"
            save_progress(prog)
            return "DRY_RUN"
        status, resp = api("POST", f"/api/records/{EXISTING_RECORD_ID}/versions", token)
        if status not in (200, 201):
            print(f"  ERROR creating new version: {status} {resp}", file=sys.stderr)
            sys.exit(1)
        draft_id = resp["id"]
        prog["step1_draft_id"] = draft_id
        save_progress(prog)
        print(f"  New draft id: {draft_id}")
    else:
        draft_id = prog["step1_draft_id"]
        print(f"  Resuming draft id: {draft_id}")

    # Update metadata on the draft
    metadata = {
        **COMMON_METADATA_FIELDS,
        "title": CORRECTED_TITLE,
        "description": CORRECTED_DESCRIPTION,
        "keywords": [
            "Virginia", "government accounting", "procurement", "expenditure",
            "purchase orders", "eVA", "APA", "fiscal data", "public domain",
            "ledger", "financial systems research"
        ],
    }
    if not prog.get("step1_metadata_updated"):
        if dry_run:
            print("  [dry-run] PUT /api/records/{draft_id}/draft metadata")
        else:
            status, resp = api("PUT", f"/api/records/{draft_id}/draft",
                               token, data={"metadata": metadata})
            if status not in (200, 201):
                print(f"  ERROR updating metadata: {status} {resp}", file=sys.stderr)
                sys.exit(1)
            prog["step1_metadata_updated"] = True
            save_progress(prog)
            print("  Metadata updated.")

    # Note: existing files carry over automatically in a new version; no re-upload needed.
    prog["step1_new_record_id"] = draft_id
    save_progress(prog)
    return draft_id


# ── step 2: create + upload an APA record ─────────────────────────────────────

def create_apa_record(label, title, description, rows, token, prog, dry_run,
                      data_root=pathlib.Path("data")):
    print(f"\n=== APA record: {label} ({len(rows)} files) ===")
    prog_key_id = f"{label}_record_id"
    prog_key_pub = f"{label}_published"

    if prog.get(prog_key_pub):
        print(f"  Already published. record_id={prog[prog_key_id]}")
        return prog[prog_key_id]

    # Create draft if we don't have one
    if not prog.get(prog_key_id):
        metadata = {
            **COMMON_METADATA_FIELDS,
            "title": title,
            "description": description,
            "keywords": [
                "Virginia", "APA", "government accounting", "expenditure", "revenue",
                "fiscal data", "public domain", "ledger", "financial systems research"
            ],
        }
        if dry_run:
            print(f"  [dry-run] POST /api/records (new deposit)")
            prog[prog_key_id] = f"DRY_RUN_{label}"
            save_progress(prog)
        else:
            status, resp = api("POST", "/api/records", token,
                               data={"metadata": metadata})
            if status not in (200, 201):
                print(f"  ERROR creating record: {status} {resp}", file=sys.stderr)
                sys.exit(1)
            rec_id = resp["id"]
            prog[prog_key_id] = rec_id
            save_progress(prog)
            print(f"  Created draft record id: {rec_id}")

    rec_id = prog[prog_key_id]
    uploaded = set(prog.get(f"{label}_uploaded", []))

    # Upload each file
    for row in rows:
        rel = row["rel_path"]
        filename = pathlib.Path(rel).name
        if filename in uploaded:
            continue
        local_path = data_root / rel
        if not local_path.exists():
            # Try VARawFiles structure (monthly files are nested)
            alt = pathlib.Path("data/VARawFiles") / pathlib.Path(rel).name
            if alt.exists():
                local_path = alt
            else:
                print(f"  WARNING: {local_path} not found, skipping.")
                continue

        size_mb = local_path.stat().st_size / 1e6
        print(f"  Uploading {filename}  ({size_mb:.1f} MB) ...", end="", flush=True)

        if dry_run:
            print(" [dry-run]")
            uploaded.add(filename)
            prog[f"{label}_uploaded"] = list(uploaded)
            save_progress(prog)
            continue

        status, resp = api("PUT",
                           f"/api/records/{rec_id}/draft/files/{filename}/content",
                           token, filepath=str(local_path))
        if status not in (200, 201):
            print(f"\n  ERROR uploading {filename}: {status} {resp}", file=sys.stderr)
            sys.exit(1)

        # Commit the file
        status2, _ = api("POST",
                         f"/api/records/{rec_id}/draft/files/{filename}/commit",
                         token)
        if status2 not in (200, 201):
            print(f"\n  ERROR committing {filename}: {status2}", file=sys.stderr)
            sys.exit(1)

        uploaded.add(filename)
        prog[f"{label}_uploaded"] = list(uploaded)
        save_progress(prog)
        print(" done")

    return rec_id


# ── step 3: supplier snapshot record ─────────────────────────────────────────

def create_supplier_record(token, prog, dry_run):
    print("\n=== Step 3: eVA supplier directory snapshot record ===")
    label = "supplier_snapshot"
    prog_key_id = f"{label}_record_id"
    prog_key_pub = f"{label}_published"

    if prog.get(prog_key_pub):
        print(f"  Already published. record_id={prog[prog_key_id]}")
        return prog[prog_key_id]

    if not prog.get(prog_key_id):
        metadata = {
            **COMMON_METADATA_FIELDS,
            "title": SUPPLIER_TITLE,
            "description": SUPPLIER_DESCRIPTION,
            "keywords": [
                "Virginia", "eVA", "supplier directory", "VLIN", "vendor reference",
                "procurement", "public domain", "ledger", "financial systems research"
            ],
        }
        if dry_run:
            print("  [dry-run] POST /api/records (supplier snapshot)")
            prog[prog_key_id] = "DRY_RUN_supplier"
            save_progress(prog)
        else:
            status, resp = api("POST", "/api/records", token,
                               data={"metadata": metadata})
            if status not in (200, 201):
                print(f"  ERROR creating record: {status} {resp}", file=sys.stderr)
                sys.exit(1)
            rec_id = resp["id"]
            prog[prog_key_id] = rec_id
            save_progress(prog)
            print(f"  Created draft record id: {rec_id}")

    rec_id = prog[prog_key_id]
    uploaded = set(prog.get(f"{label}_uploaded", []))

    for rel in SNAPSHOT_FILES:
        local_path = SNAPSHOT_DIR / rel
        filename = pathlib.Path(rel).name
        # Prefix subdirectory files to keep names unique
        if "/" in rel:
            filename = rel.replace("/", "__")

        if filename in uploaded:
            continue

        if not local_path.exists():
            print(f"  WARNING: {local_path} not found, skipping.")
            continue

        size_mb = local_path.stat().st_size / 1e6
        print(f"  Uploading {filename}  ({size_mb:.1f} MB) ...", end="", flush=True)

        if dry_run:
            print(" [dry-run]")
            uploaded.add(filename)
            prog[f"{label}_uploaded"] = list(uploaded)
            save_progress(prog)
            continue

        status, resp = api("PUT",
                           f"/api/records/{rec_id}/draft/files/{filename}/content",
                           token, filepath=str(local_path))
        if status not in (200, 201):
            print(f"\n  ERROR uploading {filename}: {status} {resp}", file=sys.stderr)
            sys.exit(1)
        status2, _ = api("POST",
                         f"/api/records/{rec_id}/draft/files/{filename}/commit",
                         token)
        if status2 not in (200, 201):
            print(f"\n  ERROR committing {filename}: {status2}", file=sys.stderr)
            sys.exit(1)

        uploaded.add(filename)
        prog[f"{label}_uploaded"] = list(uploaded)
        save_progress(prog)
        print(" done")

    return rec_id


# ── step 4: cross-link and publish ───────────────────────────────────────────

def crosslink_and_publish(epo_draft_id, apa_ids, supplier_id, token, prog, dry_run):
    """Add related_identifiers to every record then publish each draft."""
    print("\n=== Step 4: Cross-link and publish all records ===")

    all_draft_ids = [epo_draft_id] + apa_ids + [supplier_id]
    all_dois_placeholder = [f"record:{rid}" for rid in all_draft_ids]
    # After publishing the first record we get its DOI; for now we use record IDs as
    # identifiers and patch related_identifiers on each draft before publishing.

    # For the main eVA record: hasPart = all 4 APA records + supplier snapshot
    related_for_epo = [
        {"identifier": f"https://zenodo.org/record/{rid}", "relation": "hasPart",
         "resource_type": "dataset"}
        for rid in apa_ids + [supplier_id]
    ]
    # For each APA record: isPartOf eVA record + references siblings
    # For supplier record: references the eVA record

    def patch_related(draft_id, related):
        if dry_run:
            print(f"  [dry-run] PATCH related_identifiers for draft {draft_id}")
            return
        # Read current metadata
        status, rec = api("GET", f"/api/records/{draft_id}/draft", token)
        if status != 200:
            print(f"  ERROR reading draft {draft_id}: {status}", file=sys.stderr)
            return
        meta = rec.get("metadata", {})
        meta["related_identifiers"] = related
        status2, resp2 = api("PUT", f"/api/records/{draft_id}/draft",
                             token, data={"metadata": meta})
        if status2 not in (200, 201):
            print(f"  ERROR patching related_identifiers: {status2} {resp2}", file=sys.stderr)

    def publish_draft(draft_id, label):
        if prog.get(f"{label}_published"):
            print(f"  {label} already published.")
            return prog.get(f"{label}_doi")
        if dry_run:
            print(f"  [dry-run] POST publish {label} draft {draft_id}")
            return f"10.5281/zenodo.{draft_id}"
        status, resp = api("POST", f"/api/records/{draft_id}/draft/actions/publish",
                           token)
        if status not in (200, 201, 202):
            print(f"  ERROR publishing {label}: {status} {resp}", file=sys.stderr)
            sys.exit(1)
        doi = resp.get("doi", resp.get("metadata", {}).get("doi", ""))
        prog[f"{label}_published"] = True
        prog[f"{label}_doi"] = doi
        save_progress(prog)
        print(f"  Published {label}: DOI={doi}")
        return doi

    # Patch and publish the eVA/PO record first to get its DOI
    patch_related(epo_draft_id, related_for_epo)
    epo_doi = publish_draft(epo_draft_id, "step1")
    print(f"  eVA/PO record DOI: {epo_doi}")

    # Publish APA records
    apa_labels = ["apa_exp_early", "apa_exp_late", "apa_rev_early", "apa_rev_late"]
    apa_dois = []
    for label, rec_id in zip(apa_labels, apa_ids):
        related = [
            {"identifier": f"https://zenodo.org/doi/{epo_doi}" if epo_doi else f"https://zenodo.org/record/{epo_draft_id}",
             "relation": "isPartOf", "resource_type": "dataset"},
        ]
        patch_related(rec_id, related)
        doi = publish_draft(rec_id, label)
        apa_dois.append(doi)
        time.sleep(1)  # be gentle to the API

    # Publish supplier record
    supplier_related = [
        {"identifier": f"https://zenodo.org/doi/{epo_doi}" if epo_doi else f"https://zenodo.org/record/{epo_draft_id}",
         "relation": "references", "resource_type": "dataset"},
    ]
    patch_related(supplier_id, supplier_related)
    supplier_doi = publish_draft(supplier_id, "supplier_snapshot")

    print("\n=== All records published ===")
    print(f"  eVA/PO (corrected): {epo_doi}")
    for label, doi in zip(apa_labels, apa_dois):
        print(f"  {label}: {doi}")
    print(f"  Supplier snapshot:  {supplier_doi}")
    return epo_doi, apa_dois, supplier_doi


# ── main ──────────────────────────────────────────────────────────────────────

def main():
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dry-run", action="store_true",
                    help="Print what would be done without making any API calls.")
    ap.add_argument("--data-root", type=pathlib.Path, default=pathlib.Path("data"),
                    help="Root directory for corpus data (default: data/).")
    ap.add_argument("--step", choices=["1", "2", "3", "4", "all"], default="all",
                    help="Run only the specified step (default: all).")
    args = ap.parse_args()

    token = get_token()
    prog = load_progress()
    catalog = load_catalog()
    apa = apa_groups(catalog)

    # Validate all local APA files exist before starting
    if not args.dry_run:
        missing = []
        for _, _, _, rows in apa:
            for row in rows:
                p = args.data_root / row["rel_path"]
                if not p.exists():
                    missing.append(str(p))
        if missing:
            print("ERROR: Missing local files (run scripts/profile_corpus.sh to verify):")
            for m in missing[:10]:
                print(f"  {m}")
            if len(missing) > 10:
                print(f"  ... and {len(missing)-10} more")
            sys.exit(1)

        snap_missing = [str(SNAPSHOT_DIR / f) for f in SNAPSHOT_FILES
                        if not (SNAPSHOT_DIR / f).exists()]
        if snap_missing:
            print("ERROR: Missing supplier snapshot files:")
            for m in snap_missing:
                print(f"  {m}")
            sys.exit(1)

    epo_draft_id = None
    apa_ids = []
    supplier_id = None

    if args.step in ("1", "all"):
        epo_draft_id = step1_new_version(token, prog, args.dry_run)

    if args.step in ("2", "all"):
        for label, title, description, rows in apa:
            rec_id = create_apa_record(label, title, description, rows,
                                       token, prog, args.dry_run, args.data_root)
            apa_ids.append(rec_id)

    if args.step in ("3", "all"):
        supplier_id = create_supplier_record(token, prog, args.dry_run)

    if args.step in ("4", "all"):
        if epo_draft_id is None:
            epo_draft_id = prog.get("step1_draft_id") or prog.get("step1_new_record_id")
        if not apa_ids:
            apa_ids = [prog.get(f"{lbl}_record_id") for lbl in
                       ["apa_exp_early", "apa_exp_late", "apa_rev_early", "apa_rev_late"]]
        if supplier_id is None:
            supplier_id = prog.get("supplier_snapshot_record_id")

        if not all([epo_draft_id, all(apa_ids), supplier_id]):
            print("ERROR: Not all record IDs are known; run steps 1-3 first.", file=sys.stderr)
            sys.exit(1)

        crosslink_and_publish(epo_draft_id, apa_ids, supplier_id, token, prog, args.dry_run)

    if args.dry_run:
        print("\nDry-run complete. No changes made.")
    else:
        print("\nProgress saved to", PROGRESS_FILE)


if __name__ == "__main__":
    main()
