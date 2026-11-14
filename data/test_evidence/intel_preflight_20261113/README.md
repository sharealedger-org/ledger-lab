# Intel expanded-corpus preflight — 2026-11-13

Processing revision: `b10b480762ca516548343bc334a044359e08a16c`.
Workload: three identical monthly FY2003–FY2016 runs with APA expenditure and
revenue plus eVA POs, using the existing Scala TTL/ARE → sorted SJE → posting →
GL/permutation topology and legacy cross-year 5v. These are replications of the
expanded workload, not three different cost profiles or replications of the old
expense-only population. Expanded reporting follows separately.

`plan.tsv` fixes three independent runs; the existing queue validates engine
verdicts and required artifacts and compares final-product hashes. Its 140 GiB
minimum free space before each run remains enabled. Initial free space: 460 GiB;
the revenue-inclusive peak is unmeasured. No automatic deletion or retry.

Preflight claim: the installed sources match the recorded catalog and the current
producer/consumer chain accepts combined source families before launching the full
queue. Cheapest falsifiers: catalog SHA-256 mismatch, focused Scala contract tests,
and a tiny real-input sample through the actual full launcher. Evidence is limited
to these checks; no corpus accounting or performance result is implied.

Focused tests: `sbt -batch 'testOnly *ApaTtlSpec *SortedJEInputsSpec *ReportingPassSpec'`
in `02-foundation/va_pipeline`: 21 tests passed. Includes unchanged TTL fixtures,
combined-family posting, one-read selection, and fiscal labels `0014/07` versus
calendar PO dates `2013/07`. Existing Python analysis fiscal helpers remain
unmodified and must not be used to claim corrected new scores.

`source_sha256.txt` records successfully checked source paths relative to
`/Users/ktwitchell001/workspace/va_fresh_root`; completion is recorded in the
session handoff. `reference_sha256.txt` freezes the selected reference/master
versions. Reference files are staged beside `VARawFiles` for explicit lookup.

Completed source receipt: 349/349 selected catalog files matched; FY2009's
zero-byte PO placeholder was checked separately. Tiny real-input smoke:
`run_20261113_154718`, full launcher exit 0 and validator PASS. See
`smoke_validation.txt`, `smoke_gl.csv`, and `smoke_launcher.txt`.

Background execution: `background_runner.sh` supplies explicit Java/sbt PATH,
launches the existing frozen queue, holds caffeinate, and checks status every
600 seconds. It stops on queue failure without financial replay. Launchd label:
`org.sharealedger.ckb-expanded-20261113-ready`. Queue root:
`/Users/ktwitchell001/workspace/ckb_expanded_queue_20261113_ready`.
The earlier root without `_ready` retains a pre-engine sbt-PATH startup failure.

Vendor master selection preserves the prior measured run's exact repository
`data/VendorMaster.csv` (1,248 data rows), copied by the queue. No full master was
found in the workspace. This is limited name coverage, not evidence of complete
payment-to-PO identity linkage. Unknown payments/revenue retain their established
sentinel; no vendor bridge is fabricated. A sorted, canonical-key dimension
adapter is still needed before supplying this raw master to expanded reporting.

Final outcome: all three runs validated and matched the 46 compared hashes;
`queue_status_snapshot.csv` and `monthly_{a,b,c}_final_sha256.txt` are the receipts.
Expanded reporting was attached through a separate post-queue gate, which pinned
the actual posted generations and waited for all three validations. Its default
250,000 input-row guard failed on unattributed `0000000000`; see
`../reporting_corpus_20261114/`. No guard bypass, retry, complete report, model fit
or language-training result. All jobs were unloaded after completion/failure.

Supervisor caveat: `launchctl submit` restarted exited wrappers; existing
directory guards rejected reentry without financial replay. The actual report
attempt used a run-once plist with KeepAlive=false. The supervisor receipt records
the script as run; future background launches must use explicit run-once job
configuration rather than submit's restart behavior. Startup integration is now
on main in `fe591a9` and `386e933`, applied after the measured runs ended.
