# Synthetic-reference corpus v2: execution contract

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
