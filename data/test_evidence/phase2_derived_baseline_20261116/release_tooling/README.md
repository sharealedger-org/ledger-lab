# Local release-tool prerequisite

2026-11-17. Bash3.2.57, curl8.7.1, shasum, tar and gzip are available. No jq
was on PATH. For future authorized upload-script testing, downloaded the official
jq1.8.2 macOS amd64 binary into ignored workspace storage only:
`session-run.tmp/release_tools/jq-1.8.2/jq`. No global installation, financial
runtime/classpath change, upload or publication.

Official release: https://github.com/jqlang/jq/releases/tag/jq-1.8.2
Asset/checksum listing: https://github.com/jqlang/jq/releases/expanded_assets/jq-1.8.2
Download: https://github.com/jqlang/jq/releases/download/jq-1.8.2/jq-macos-amd64
SHA-256 checked before making executable:
`e94b266e3c26690550006abe63152b782280f4e14374accdf04cbde844f00bc0`.
`--version` returned jq-1.8.2. A bounded JSON fixture check of draft state,
bucket string and empty file list passed. This is not upload-script acceptance;
that script remains scheduled after combined financial/report validation.
No Python added or invoked. A future script can accept an explicit JQ path;
do not rely on this ignored local binary being installed on another machine.

2026-11-17 continuation preparation: rechecked the official developer API before
implementation, https://developers.zenodo.org/ (file-upload quickstart and
Deposition File representation). Bucket PUT uploads are addressed through the
selected draft's links.bucket; remote file metadata exposes MD5 checksum and
size. A resumable helper must check actual remote state against local size/hash,
not trust only a prior local success receipt. SHA-256 remains the artifact
manifest identity; record MD5 separately for remote transport verification.

The documented default record budget is50GB total/up to100 files, requiring an
explicit package/record partition plan for any larger release. Check before
remote mutation rather than silently omitting artifacts or creating records.
No token, draft API mutation, upload, package compression or script implementation
was performed during this check. The after-validation gate remains in effect.
