# Synthetic-reference corpus v2: execution contract

## Owner topology correction: sort, then sequential reporting CKB

2026-11-15: owner stopped the indexed-report approach before it completed and
authorized changing reporting only, reusing Run 1's validated FY03–FY16 financial
outputs. A separate declared Scala external sort orders each canonical SJE and
actual ledger stream by literal Instrument ID with bounded chunks/fan-in. It
preserves every record/field and ledger header, original financial files, hashes,
source/rule lineage and zero identities. Sort materializations are analytical
inputs, not replacement canonical state. Then one sequential CKB read consumes
all ordered streams and produces all existing expanded views together; no index
pre-scan and no seek-based input reader. Spill reduction remains a separate
analytical summary process and is measured separately.

Cheapest falsifier before implementation: unchanged 47-test baseline and existing
variable-width-ID, literal-quote, actual-posting and small/large-spill fixtures.
New fixture must prove indexed versus sorted/sequential analytical equivalence,
all input rows/bytes/hash lineage, reject wrong order and preserve quote-bearing
canonical fields. Run FY2012 real data through sort plus sequential report to
measure sorting, CKB compute/output and reduction independently before updating
the full projection. Two hours is a hypothesis, not an accepted runtime promise.

Owner authorizes overlapping the next financial execution with revised reporting
if memory/scratch admission permits. All actual timings remain recorded, labeled
contended exploratory work rather than comparable timing replicates. Do not
discard validated financial results or silently alter report capability. Preserve
superseded report scratch and disable the old queue/archival supervisor before
starting the revised topology. Final archival still follows successful evaluation.

Owner authorization, 2026-11-15: implement and verify a new preparation path,
then three local executions with expanded reporting, evaluation and verified
OneDrive archival. This is exploratory preparation for Q2, not Q2 evaluation or
the wider component-generation workload. The excluded old assignment producer
and its outputs are not inputs to this version.

Claim: reporting assignments prepared from the actual committed SJE/ledger
population cover every attributed financial ID, preserve literal IDs and leave
all zero/placeholder IDs unattributed. The downstream consumer is the existing
sorted assignment join in ReportingPass/SyntheticReporting. Cheapest falsifier:
an adversarial fixture with duplicate IDs, variable-width IDs, zero sentinels,
reversed input order and enough runs to require multiple bounded merge levels.

01 input preparation reuses SyntheticReferenceBundle's explicitly synthetic
name-equivalence references and canonical master projection. Immutable VA raw
expenditure/revenue and PO files remain unchanged. Both APA families are included;
receipt identity remains agency-grain, never observed taxpayer identity. Missing
reference lookups and malformed records retain existing adapter policies and
reject evidence; FY2009's absent PO is explicitly represented by an empty local
placeholder. Historical results remain separate. No new financial rules.

Source-policy falsifier, FY2012: the original master contains numeric agency-name
aliases with instTypeID=REV. Only 293,080 of 2,745,656 receipt SJE rows remained
zero under that snapshot; 2,452,576 were assigned agency aliases. This financial
profile is retained as a failed admission attempt, not the selected population.
New receipt-safe input preparation excludes REV alias rows from the TTL/report
master, records every exclusion and preserves the original master. It checks
that no remaining normalized holder name collides with any prepared agency key,
so the unchanged APA receipt adapter's name lookup must miss. Preserve existing
vendor and PO IDs; do not fabricate receipt parties or rewrite source receipts.
Require all actual APA receipt rows to retain the original zero ID in the new
preparation profile. This is a versioned input-policy change, not a historical
baseline correction. Use a fresh root and replay the entire financial process.

Actual corrected FY2012 reporting then exposed a record-parser boundary: journal
and ledger writers use the established literal comma-delimited canonical fields,
with commas sanitized but quote characters retained as data. RFC CSV tokenization
in the reporting-only indexed reader rejects such valid fields. Reproduce this
on a named balanced source with quotes in description/commitment fields; retain
exact raw strings through actual post and report. Only canonical movement,
balance and opening reads use literal field splitting; headered source, dimension,
assignment and reference CSVs keep strict RFC parsing. Existing posting/sorting
schemas and monetary behavior are unchanged. Index scan and ordered read remain
the same two physical passes, bounded by existing record/block allowances.
Retry reporting from the same immutable committed financial inputs into a fresh
report attempt after baseline and quote-boundary tests pass. Preserve the failure;
do not suppress offending records or add a lenient skip policy.

New Scala post-financial preparation scans exactly the merged SJE and actual
ledger selected by the committed run. It records full hashes, row/zero counts,
UTF-8 byte widths and source-family zero counts. It writes ID candidates only at
physical ID breaks, externally sorts/deduplicates them in literal order with a
50,000-ID buffer and at most 16 merge readers, then emits one assignment per
attributed ID. Template selection is sorted-ID ordinal modulo the sorted bounded
SAL template list (version `sorted_population_cycle_v2`), explicitly simulated
and population-dependent, not a learned relationship or recovered identity.
Zero, known PO sentinels and UNMATCHED IDs receive no assignment. No population
registry/map, source rekeying, accounting, or whole-history materialization.
Run-local assignment files are preparation artifacts outside report timings.

Canonical financial flow stays existing Scala TTL/ARE -> SJE external sort/merge
-> posting/GL/permutation; legacy cross-year views remain. Expanded 5v/5r runs in
separate JVMs over those exact committed files with the new assignments, prepared
master/references, unchanged synthetic histories, AS_OF 2012-06-30 and RUN_DATE
2016-07-01. It preserves EVENT_DATE, missing-version evidence and financial
conservation. It does not generate or apply journals. No claim of target GenevaERS
capability closure; GERS-002/003 and existing architecture qualifications remain.

Admission: unchanged focused tests; adversarial preparation checks; connected
fixture through the actual new producer/consumer; fresh FY2012 current-policy
financial plus reporting profile; measured live disk, sampled scratch, time and
RSS. Record actual report cardinalities before full execution. Retain distinct
physical passes and preparation costs. Three runs use identical snapshots,
settings and code; compare canonical financial and expanded analytical outputs,
excluding operational paths/times/metrics from byte-equality requirements.
Do not label them three cost points. Stop on failures, retain failed attempts,
correct and rerun under a new attempt identity; no mixed-code replicates.

Keep active work local with caffeinate and monitoring. Verify run success,
controls and hashes before archive copying; verify destination capacity and
copied bytes. Three retained corpora plus reports need measured admission;
The independently measured runs retain every path and byte. After evaluation and
repeat comparisons, identical large files in subsequent new runs may share hard
links to the first run, after SHA-256 and byte comparison. Record allocation
before/after this post-measurement storage operation separately; it is not an
engine optimization or a change to logical retention. No historical data is
selected and no differing file is replaced. Archive copies remain independent.
Do not delete local results without separate authority.

Admission completed on the corrected FY2012 actual report; compact receipt:
`data/test_evidence/synthetic_reference_corpus_v2_20261115/README.md`.
Optional --shareProfiles permits post-evaluation sharing of byte-identical files
between the first full run and these task-created admission profiles, including
the explicitly rejected first profile. Each file requires SHA-256 plus byte
comparison; every path/content and rejected status remain intact. This is storage
sharing only, not equivalence or acceptance of different populations. Independent
full executions still require complete product equality. Before later runs,
require at least the measured first-run allocation plus a 20-GiB reserve, and
retain a separate 20-GiB low-scratch stop during execution.
