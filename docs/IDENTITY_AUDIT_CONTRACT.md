# Identity and concentration audit v1

2026-11-14. Owner requested execution of Phase 2 plan section 14's first slice.
This is a read-only Scala diagnostic, outside measured financial processing.
It generates no vendors, SJEs, postings or corrected financial records.

Claim to falsify: enriching the 1,248-row master alone removes the oversized
unattributed reporting group. Cheapest falsifiers are tiny master/parser/key
fixtures and the previously failed zero-key reporting attempt. Then inspect one
immutable successful population, not three replicated populations combined.

Inputs: frozen run master, current enriched master snapshot, APA source files,
reference tables used by TTL, and canonical merged SJE/actual posted LDGR from
monthly_c. Snapshot differences are reported; historical output is never inferred
from the current master alone. Raw source acceptance is compared with the
recorded TTL input-row metric. Strict CSV interpretation is a diagnostic
counterfactual, not a rerun of financial processing.

Producer trace: APA expense passes plain-comma field 6 to the vendor lookup.
APA revenue resolves agency name to its reference agency key, then passes that
key to the shared revenue adapter's lookup. Earlier shorthand describing a
direct raw-agency-name lookup was incomplete. PO retains its source VENDORID;
expense master IDs are padded to ten digits. Audit these literal keys before
inferring any payment/PO bridge.

Master probes compare the actual legacy split/last-row-wins behavior with
quote-aware parsing and ambiguity detection. Raw APA diagnostics use physical
lines, matching TTL record boundaries; strict CSV observations do not claim
multiline recovery. Generic-label classification is a published exact-label
heuristic, not proof that payroll or an individual supplier has been identified.

Physical profile: separate master, raw-source and canonical-input JVM processes,
ordered by Bash. Raw name counters have a fixed key allowance and spill to the
existing Scala SortEngine, with bounded-fan-in merge/reduction. Canonical scanning
retains one literal vendor group per file and externally combines counts across
years. No unbounded transaction history or full distinct-name set is retained.
Outputs are diagnostic CSVs, not canonical state or downstream model inputs.

Controls: source disposition reconciliation, legacy source-accepted counts versus
recorded TTL counts; checked leading identity/source fields and vendor ordering
for canonical files (amounts/trailing fields are not reinterpreted); global SJE/ledger counts
equal concentration summaries; ID formatting/collision/duplicate cases explicit;
per-input SHA-256 and stable file size/mtime. A success marker means audit execution
completed, not that identity or accounting is corrected. Record mismatches as
findings rather than silently accepting changed denominators.

Evidence: focused Scala fixtures, unchanged existing TTL/one-read tests, mode
logs with OS CPU/wall/RSS, source manifests, master findings, source-name summary,
zero-key breakdown and complete vendor concentration table. Full raw-name tables
remain external; copy compact summaries and findings into the repository.
No new Python and no financial mainline changes. No target-GenevaERS capability
or new measured cost point is claimed by this diagnostic.

Execution refinement: the first canonical probe found literal vendor decreases
(`c12464` to `c1246`) in the measured concatenated posting-key order. Version 2
records decreases and segment reentry counts, then externally combines group
counts rather than requiring literal-ID order. It does not repair/re-sort the
financial input or certify that vendor groups are contiguous. Failed/probe
artifacts remain separate. Partial read digests carry `PARTIAL_READ:` and are
never represented as complete input checksums. The completed source pass used
version 1; its count logic is unchanged by this canonical-only refinement.

A final Scala summary independently reconciles accepted source rows to journal
identity-class counts and canonical row counts to recorded process metrics. It
ranks groups and classifies the known PO placeholder separately. Candidate-name
sizing excludes numeric/punctuation labels and records an exploratory exact-label
review table for bookkeeping, internal-cost and pooled-payment labels; no label
heuristic proves an actual counterparty identity or payroll decomposition.

Run with new absolute output paths and the master/reference snapshots actually
used by the selected successful population:

```bash
bash scripts/run_identity_audit.sh \
  --root /absolute/raw/VARawFiles \
  --runRoot /absolute/successful-run/output \
  --master /absolute/frozen/vendor_master.csv \
  --currentMaster /absolute/current/VendorMaster.csv \
  --refPath /absolute/frozen/reference-directory \
  --out /absolute/new-audit-root
```

The launcher snapshots masters/references and pins posted generation paths. It
runs master, raw, canonical, independent-summary and parser-summary JVM stages.
An execution-complete marker is not an assertion that every diagnostic finding
passed: inspect source controls, COUNT_CHECKS_PASS/MISMATCH and order findings.
