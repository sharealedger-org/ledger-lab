# Identity recovery v1 workload declaration

Owner authorized implementation, fixture verification, then one new corpus execution
on 2026-11-14. This is a changed input/adapter workload, not a replicate of `b10b480`.
Prior measured generations and Gold remain immutable.

## Claim and falsifiers

APA quoted fields must preserve the vendor and accounting reference columns. Two
different quoted names must not collapse to a last-row-wins identity. Reporting
must preserve literal IDs (including variable-width `c` IDs and zero sentinels),
reconcile actual posted balances, and process more than 250,000 rows for one ID
without retaining those rows in memory. The existing quoted-name/object fixtures,
literal-order fixture, spill-equivalence fixture and corrupted-balance fixture
are the cheapest falsifiers. Run the unchanged focused suite before edits.

## Producers, consumers and physical boundaries

* 01 transformation: `standardizeAndSort.applyAPA` consumes declared expenditure
  (nine columns) and revenue (seven columns) CSV schemas, plus bounded current
  reference/master snapshots (16 MiB allowance per file). Quoted CSV fields
  replace comma splitting under APA's one-record-per-physical-line contract.
  Header errors fail the attempt; bad quoting, wrong record width and invalid
  amounts retain the explicit rejected count plus `APARejects_` source/line/reason
  evidence. Duplicate normalized names with different keys fail rather than choose
  a party or accounting reference. Missing names retain the existing zero fallback.
  Existing ARE rules produce the same balanced 39-column SJE contract; Foundation
  sort/post remain the consumers. Revenue remains agency-based, not taxpayer data.
* 05 perspective: `ReportingPass` consumes those canonical SJE partitions and
  actual 30-column posted ledgers (and declared opening/dimension streams).
  Reporting order is literal ID, independent of concatenated posting-key order.
  A reporting-only byte-range index may reorder immutable record blocks without
  changing IDs or canonical posting order. Index preparation/read cost is explicit.
* Large reporting buckets and relationships use bounded partial aggregation and
  bounded fan-in external reduction. Small feature/dimension state has explicit
  key limits; capacity overflow fails publication. Raw row counts are metrics,
  not memory limits. No accounting is performed in Bash and no Python is added.
  Cross-vendor reduction remains the separate `5r` process. These analytical
  materializations are outside the posted financial state and prior measurements.

## Controls and evidence

Unchanged baseline controls, named quoted-identity fixture, per-journal balance,
explicit rule/date/lineage checks, real downstream posting, exact key-level
movement/opening/balance reconciliation, small/large spill equivalence and a
corrupted-balance rejection are required. Zero and known PO placeholder IDs are
unattributed; do not generate parties to evade a group bound. Any synthetic
identities must be visibly simulated and backed by a deterministic source fixture.
Record rows, bytes, sort/spill passes, peak keys, input hashes, wall time and
per-process memory. GERS-002/003 remain target-runtime gaps; Scala fixture success
does not close them or certify a new measured cost point. Independent architecture
review remains required for that promotion.

## Corpus admission

One new full run may start only after the connected fixture passes and capacity
is sufficient for its roughly 128 GiB historical footprint plus working space.
At task start only 75 GiB is available. Existing full runs are not authorized for
deletion. Preserve all prior outputs while preparing concrete validation evidence.

Real-snapshot preflight found 371 ambiguous names (3 agency, 268 fund, 2 object,
80 program, 18 source). The frozen reference tables have multiple internal keys
without enough dating/context to select historical keys. Corpus admission also
requires an explicit reference resolution contract and reviewed mapping evidence;
neither last-row selection nor an invented natural-key crosswalk is a correction.
