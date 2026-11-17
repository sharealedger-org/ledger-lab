# Validated baseline upload preparation

No upload or publication has been run. The validated results commit is cd242fc,
pushed to origin/main. The helper is Bash orchestration, not a financial engine.

`scripts/upload_phase2_zenodo.sh` defaults to local check mode. It verifies every
payload size/SHA256 and per-record budgets before networking. Upload mode requires
an environment ZENODO_TOKEN, existing unsubmitted draft IDs and matching metadata
version. It uploads through the draft bucket, verifies remote MD5/size and writes
file-granular resumable receipts pinned to the manifest hash/host. Matching remote
files are verified and skipped. Mismatched existing files stop without overwrite;
partial-file cleanup/new-draft decisions remain manual. There is no create,
publish or delete operation. Token headers use a temporary mode600 curl config
and are not persisted in state or command arguments.

The explicit source inventory/version is
`data/test_evidence/phase2_derived_baseline_20261116/zenodo_helper/release_plan.json`:
85 validated financial output/view/reference/control files,186,778,467,131 original
bytes, with original SHA256. It excludes raw/private recipes, sorted-input copies
and analytical spill/merge intermediates. Metadata and source provenance remain
linked to the actual accepted source build. The64GB temporal CSV requires gzip
for the documented default Zenodo file/record budget. Owner-approved archival is
running; payload preparation is separate from script readiness.

After all approved archives are complete, generate the physical payload manifest:

```bash
export JQ="$PWD/session-run.tmp/release_tools/jq-1.8.2/jq" # or installed jq
bash scripts/prepare_phase2_release_manifest.sh \
  --plan data/test_evidence/phase2_derived_baseline_20261116/zenodo_helper/release_plan.json \
  --base-dir "$PWD/session-run.tmp/phase2_fused_reporting_v2" \
  --archive-receipt "$PWD/session-run.tmp/phase2_baseline_archive_v1/manifest.tsv" \
  --out data/test_evidence/phase2_derived_baseline_20261116/zenodo_helper/payload_manifest.json
```

The builder preserves original SHA/size and verified gzip transport SHA/size,
and assigns payloads to logical draft groups with45GB/95-file ceilings. Draft IDs
are initially null. Before a later authorized upload, create/configure the drafts
manually with the exact release version, enter their IDs in the manifest, then
run local verification:

```bash
bash scripts/upload_phase2_zenodo.sh --manifest PATH_TO_PAYLOAD_MANIFEST \
  --base-dir "$PWD/session-run.tmp/phase2_fused_reporting_v2"
# Only during a future explicitly authorized upload:
# ZENODO_TOKEN supplied by environment; never put its value in a command line.
bash scripts/upload_phase2_zenodo.sh --mode upload --manifest PATH_TO_PAYLOAD_MANIFEST \
  --base-dir "$PWD/session-run.tmp/phase2_fused_reporting_v2" --state-dir LOCAL_STATE_DIRECTORY
```

The current task prepares these commands; do not execute upload mode against the
service now. The transport tool also accepts https://sandbox.zenodo.org through
ZENODO_BASE_URL. It rejects other hosts. A changed manifest requires a new state
context. Gzip decompresses to the original byte-identical CSV; stored mode/mtime
and original checksum are retained in the archival receipt for restoration.

Offline tests exercised local/no-network check, mock upload, checksum/size checks,
resume without another PUT, same-size local mutation, remote mismatch, submitted
record, file budget, path traversal and gzip-receipt rejection. Actual service
behavior has not been tested with credentials. Evidence: zenodo_helper/checks.txt.
API contract checked against [official Zenodo documentation](https://developers.zenodo.org/):
bucket PUT uploads, remote MD5/size, default50GB/100-file budgets. This is no claim
of publication readiness beyond the documented local/fixture evidence.
