# eVA supplier-directory snapshot

Completed 2026-10-31. This is current reference-data acquisition, outside the measured Scala financial path. No Parquet conversion, financial-engine change, or automatic VendorMaster update was made.

## Result

| Control | Result |
|---|---:|
| Records in the full public report CSV export | 230,128 |
| Records represented in the richer canonical CSV | 230,128 |
| Distinct nonblank eVA supplier/account IDs | 230,126 |
| Records with missing supplier IDs, retained and flagged | 2 |
| Active status observed in HTML | 230,068 |
| Pending Approval status observed in HTML | 60 |
| Raw export rows with invalid field counts | 0 |
| Retained HTML source pages | 2,912 |
| Final fingerprint, source-hash, count, and page-number audit | Passed |

The capture spans October 31, 2026, approximately 5:34-6:56 PM America/Phoenix (UTC times are retained per observation). It is an acquisition interval, not an atomic source-system snapshot or historical effective-dated master.

## Files

Full data and source evidence are local and covered by the existing data/VendorDownload/ Git ignore rule:

    data/VendorDownload/supplier_snapshot_2026-10-31/
      suppliers.csv                         # canonical reference, rich HTML fields + provenance
      manifest.json                         # scope, counts, completion basis, checksums
      audit.json                            # independent saved-artifact controls
      queries.csv
      queries/*.json                        # resumable query observations
      source_pages/*.html.gz                # immutable response content with per-row hashes
      source_export/
        suppliers_source.csv                # unchanged full source export, UTF-8 BOM
        manifest.json                       # exact request, headers, encoding, SHA-256, counts
        reconciliation.json                 # complete known-field coverage and targeted lookup
      historical_candidate_details/
        historical_candidate_vlin_verification.csv
        manifest.json
        *.html.gz

The canonical CSV is UTF-8. Import IDs and postal codes as strings. Full source data is not committed to Git. A new snapshot should use a new dated directory.

## Essential field meanings

**The raw CSV export's column named Status contains Self/State registration type, not Active/Pending Approval status.** The canonical CSV separates these fields using HTML evidence. This interpretation was checked against 230,127 pre-existing HTML observations; the one remaining export row was then retrieved through a targeted public lookup.

| Source value | Canonical field | Meaning |
|---|---|---|
| Raw CSV Status: Self / State | registration_type | Registration route |
| HTML vendor status | status | Active / Pending Approval in this snapshot |
| VendorID | eva_supplier_id | eVA supplier/location account code; not a DUNS or Cardinal vendor ID |
| eMall Supplier Name | supplier_name | Supplier/location display name; not necessarily the legal entity name |
| eMall Contact | location_name | Ordering-location label, not a contact person |
| Address 1, City, St, Zip | address_line_1, city, state, postal_code | Published ordering address |
| HQ | headquarters_flag | Source HQ flag; not a parent-entity crosswalk |
| SWAM Type | swam_display | Current displayed classifications, without historical certification validity |
| Charge Card / Visa / Electronic Orders flags | accepts_* | Published capabilities, not evidence of actual payment channels |

Source blanks remain blanks. The two missing IDs receive missing_supplier_id quality flags and observation fingerprints rather than fabricated vendor IDs. The canonical CSV also carries detail_url, first_query, first_page, observed_at_utc, and source_sha256. observation_id fingerprints the normalized full list row.

Whitespace and HTML entities are normalized for comparison; originals remain in the source files. Deduplication uses the complete normalized observation, not supplier ID alone. Changed names, locations, addresses, or status can remain separate observations for the same account in future captures.

VLIN and commodity registrations are separate detail attributes, not list columns. The targeted VLIN evidence covers only the 21 supplier IDs in the historical candidate set: 20 of 22 candidate observations agree, and 2 remain unresolved. It is not a full-directory VLIN crosswalk.

## How completeness was established

Source report:
https://logi.cgieva.com/External/rdPage.aspx?rdReport=Public.Reports.Report9001_Data

The browser's generic 1,000-vendor notice is not a universal limit on filtered results. AA returned 329 rows, AB 679, and A exposed 197 pages with a valid end page. All A-Z searches were completed; numeric and symbol controls and retained broad-query pages supplemented them.

The reporting engine also supports a documented CSV format:
https://devnet.logianalytics.com/hc/en-us/articles/1500009531621-Query-String-Parameter-Reference

The saved export records the exact public search (_ with starts-with enabled), report ID, format, table ID, headers, time, and checksum. It returned 230,128 rows. Of these, 230,127 matched the richer HTML capture on every field present in the export. A targeted supplier-ID search retrieved the remaining non-ASCII supplier, SUP323218. All 230,128 unique export field tuples are now represented in the canonical CSV. Actual vendor status remains independently sourced from HTML.

Some early broad HTML queries were intentionally left partially paginated after their rows were covered through the export reconciliation. These are retained as incomplete_redundant_html_queries, not outstanding acquisition work. execution_complete and acquisition_complete refer to the completed hybrid acquisition; pagination_complete is false for those redundant query histories.

This verifies coverage of the recorded public report export. The separately linked inactive directory and every Commonwealth payee are outside that established scope. Current directory observations do not establish FY2003-FY2016 identities, attributes, or PO/payment relationships.

## Reproduce or refresh

From ledger-lab, using Python 3 and the standard library only:

    py -3 scripts/export_eva_supplier_directory.py --out data/VendorDownload/supplier_snapshot_2026-10-31/source_export --search _
    py -3 scripts/collect_eva_supplier_directory.py --out data/VendorDownload/supplier_snapshot_2026-10-31 --workers 3 --max-leaf-pages 10000
    py -3 scripts/assemble_supplier_snapshot.py --out data/VendorDownload/supplier_snapshot_2026-10-31
    py -3 scripts/reconcile_supplier_export.py --snapshot data/VendorDownload/supplier_snapshot_2026-10-31
    py -3 scripts/audit_supplier_snapshot.py --snapshot data/VendorDownload/supplier_snapshot_2026-10-31

Use a new date/path for a fresh capture. The raw export alone is a useful, faster reference when active status is not required. Its mislabeled Status header still needs the interpretation above. HTML collection provides the richer status/provenance observations.

For focused runs, repeat --prefix AA --prefix AB. The conservative default splits searches over ten pages; the explicit larger leaf limit above follows the verified source pagination for letter searches. Up to three page readers and a global minimum request interval control request load. Oversized queries, wildcard semantics, missing pages, repeated pages, and unknown responses are recorded rather than silently treated as complete. Do not assume every submitted string is a literal prefix: the source tokenizes spaces and treats some symbols specially.

Completed query checkpoints and source pages support resumption. Prior canonical observations are retained. Reused page content is qualified by first-page signatures and page counts; this does not prove an unchanged source throughout the capture interval. Assembly is a recovery step before export reconciliation, and can reset pagination-completion metadata. Re-run reconciliation and the final audit after assembling.

Focused controls:

    py -3 scripts/test_collect_eva_supplier_directory.py

All ten controls passed. The final audit also passed for all 230,128 canonical observations, including source hashes, fingerprints, manifest checksum/counts, retained page numbers, and missing-ID flags.

## Use by Bob and the project

Treat this as a dated external reference. Preserve eVA account IDs, VLIN, legacy PO identifiers, legal-party identities, and ordering/remit locations as distinct namespaces. Use the independently evidenced legacy-PO/VLIN/eVA candidate mappings before attempting broad name-based enrichment. Keep ambiguous relationships and unmatched records visible.

This snapshot can support vendor alias, location, geography, and classification analysis. Historical attributes still need historical evidence. The independent payment-side link required for vendor substitution remains open. No live master was enriched and no outbound request or outreach was sent.