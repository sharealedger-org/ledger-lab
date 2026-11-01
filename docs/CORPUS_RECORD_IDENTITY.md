# Corpus Record Identity and Provenance

Status: specification, 2026-10-31. Implemented in part: `CorpusProfile` already emits the record
locator (`record_ordinal`, `start_line`, `start_byte_offset`) for every quarantined record, and the
file checksum for every file. Stable IDs for accepted records are **specified here but not yet
emitted**, because emitting one per record for 131 million records is a decision about storage
that should follow a demonstrated need (see "When to materialize").

Scope: the Virginia source corpus under `VARawFiles/` and `VAPOData/` (see
[RUNTIME_DATA_LAYOUT.md](RUNTIME_DATA_LAYOUT.md)). This is a data-inspection contract. It does not
change the Scala `Transaction` / SJE contracts; `source_record_id` in
[VA_DATA_MODEL_CONTRACT.md](VA_DATA_MODEL_CONTRACT.md) may be populated from the value defined here.

## Principles

1. Raw files are immutable. A file is identified by the SHA-256 of its bytes, not by its name or path.
   A re-downloaded file with different bytes is a different file, even with the same name.
2. A record is identified by *where it is in an identified file*, not by what it contains. Two
   records with identical business fields are two records. Do not deduplicate on field equality.
3. Raw text and normalized values are different things and are stored under different names.
4. Every derived output names the input snapshot and the transformation version that produced it.

## File identity

`file_id` = first 16 hex characters of the file's SHA-256 (`source_catalog.csv`, column `sha256`).

16 hex characters is 64 bits. For a corpus of a few hundred files collisions are not a practical
concern, but the full 64-character checksum is always the authoritative value; `file_id` is a
short display form and must be checked against `sha256` when a catalog is loaded.

## Record locator

Within a file, a record is located by three values. All are emitted by `CorpusProfile` for
quarantined records, and are computed with the same rules for accepted records.

| Value | Definition |
|---|---|
| `record_ordinal` | 1-based position among all non-blank, non-header **parsed records** in file order, counting quarantined records. A quoted multi-line record is one ordinal. The header record has no ordinal. |
| `start_line` | 1-based physical line number (lines end at `\n`; a preceding `\r` is stripped) of the record's first line. Blank lines and the header count as lines. |
| `start_byte_offset` | 0-based byte offset of the first byte of that line, counted from the start of the file **including** a UTF-8 byte order mark if present. |

`record_ordinal` and `start_line` differ whenever a file has a header, blank lines, or a quoted
multi-line record. Use `record_ordinal` for stable identity and `start_byte_offset` for
retrieval. `start_line` is for human debugging only, because editors disagree about `\r` and
about whether a BOM is a character.

Parsing rules that determine ordinals are in `CorpusProfile.parseRecord` and `scanRecords`
(version `corpus-profile-1`). A change to those rules that moves any ordinal requires a new
profiler version string, and IDs produced under different versions must not be compared.

## Stable source-record ID

```
source_record_id = <file_id> ":" <record_ordinal>
```

Example: `3fa09c71d2e84b10:482113`. The ID is stable as long as the file bytes and the profiler
version are unchanged. It is not a business key and carries no meaning about the vendor, voucher
or PO.

When a record must be re-found without re-parsing the file, store `start_byte_offset` next to the
ID and seek to it.

## Raw versus normalized

| Layer | Naming | Rule |
|---|---|---|
| Raw | field values exactly as parsed from the file, original column names | Never modified. Leading zeros, trailing spaces and case are preserved. Identifiers are strings. |
| Normalized | column names suffixed `_norm` | Produced by a named, versioned function. Records the function version and a quality flag when the value was changed or blanked. |

Examples that must stay distinguishable:

- `VOUCHER_NUMBER` is space-padded to a fixed width in the APA files (for example `"6003040         "`).
  The padded value is the raw value; a trimmed form is `VOUCHER_NUMBER_norm`.
- PO `VENDORID` of `999999999999` is a placeholder in the raw data. It stays in the raw column.
  A derived flag `vendor_id_is_placeholder` records the interpretation.
- A `VENDORSTATE` that `extract_po_attributes.py` blanks because it is not a two-letter code is a
  **repair by deletion**, not a corrected value. Record it as `state_norm=""` with flag
  `STATE_NULLED_NOT_2LETTER` and keep the raw value. A nulled field must not be counted as a repaired row.

Do not infer a tax ID or any identity from the shape of `VENDORID`. Shape is a profile statistic.

## Dispositions and reason codes

Every parsed record has exactly one disposition: `accepted` or `quarantined`. Quarantined records
carry one reason code and their original text is preserved in `quarantine.csv`
(truncated at 2,000 characters, with a `raw_truncated` flag).

| Reason | Meaning |
|---|---|
| `FIELD_COUNT` | A closed record whose field count differs from the header. Includes PO rows split by an embedded line break inside an unquoted field. |
| `UNTERMINATED_QUOTE` | A quoted field did not close within 20 physical lines or before end of file. Only the first line is quarantined; following lines are re-read as new records so one stray quote cannot swallow good rows. |
| `UNBALANCED_QUOTE_RESYNC` | A multi-line quoted record closed but with the wrong field count, so it was split back into single lines. |
| `BAD_AMOUNT` | `AMOUNT`, or `QUANTITYORDERED` / `PRICE`, is non-blank but not numeric. |
| `EXTRACT_ARTIFACT` | A line written by the source extract tool rather than data (`N rows selected.`, `SQL> spool ...`). The `N` is kept as `source_declared_rows`. |

An accepted record can still have quality flags (blank key, unparseable date, date outside the
fiscal-year window). These are counted per file in `file_profile.csv`; they do not remove the record.

## Accounting identity

For every profiled file:

```
physical_lines = blank_lines + lines_in_records
lines_in_records = header_lines + sum(lines of accepted records) + sum(lines of quarantined records)
data_records_parsed = accepted + quarantined
```

`file_profile.csv` column `line_reconciles` reports the first equation. A file where it is
`false` must not be used until explained.

## Repeated transactions

The profiler counts `duplicate_candidates` (a second or later record whose full raw text hashes
to a value already seen in the same file). This is a **count, not a removal**. Legitimate repeats
exist, for example one payment split into identical lines. Nothing in this specification deletes
or merges them.

The hash is a 64-bit FNV-1a of the record's raw bytes, so a hash collision between different
records would be counted as a duplicate. At the observed scale (at most about 1.7 million parsed
records in any one file; the largest is the FY2017 PO file) the birthday-bound expected number of
false hits per file is about n²/2⁶⁵, roughly 10⁻⁷, and is therefore ignored, but the figure is a
count of *candidates*.

## Revisions

A `-Vn` suffix on a PO number marks a revised version, but the suffix alone does not establish a
complete revision history: the public files do not say which version is current, nor whether
all versions are present. Treat `-Vn` as a raw attribute. Do not collapse versions into one PO
without a documented rule and a count of what was collapsed.

## Derived outputs

Any derived file (a vendor-year summary, a crosswalk, a sample) must carry, in a header comment or
sidecar manifest:

- the `file_id`s (or the corpus `source_catalog.csv` checksum) of every input;
- the name and version of the transformation;
- the date it was produced;
- counts in and counts out, with exclusions named.

`data/VAPOData/vendor_po_attributes_by_year.csv` does not currently meet this. See
[CORPUS_FINDINGS.md](CORPUS_FINDINGS.md).

## When to materialize per-record IDs

Do not write a 131-million-row ID table by default. Materialize IDs only when a named consumer
needs them, for example:

- a sample used in a test (the sample file lists the `source_record_id` of each row);
- a crosswalk or label set (each label points to a `source_record_id`);
- lineage from an SJE back to a source row, once the Scala `Transaction` contract carries the value.

Because the ID is computable from `file_id` and `record_ordinal`, it can be generated on demand by
re-running the profiler's record reader.

## Not verified

- The locator rules have been exercised by the unit tests (`CorpusProfileSpec`) and on the full
  corpus for line reconciliation, but no test yet re-reads a record by `start_byte_offset` on real data.
- Whether Excel-origin `.xlsx` PO copies (`VAPOData/20NN Combined.xlsx`) contain the same records
  as the `.txt` files has not been checked.
- Collision behaviour of the 64-bit duplicate hash was estimated, not measured.
