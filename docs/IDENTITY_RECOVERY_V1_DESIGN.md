# Identity recovery v1 workload declaration

Owner authorized implementation, fixture verification, then one new corpus execution
on 2026-11-14. This is a changed input/adapter workload, not a replicate of `b10b480`.
Prior measured evidence and Gold remain preserved. The three expanded execution
datasets were retired with owner authorization on 2026-11-15; see the cleanup receipt.

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
At task start only 75 GiB was available. On 2026-11-15 the owner authorized deletion
of generated data from monthly_a/b/c; 1,398 paths were removed, increasing free
space to 452 GiB. Logs, metrics, controls, compact evidence and Gold were retained.
Receipt: `data/test_evidence/corpus_cleanup_20261115/`. These historical run directories
no longer contain financial datasets and cannot feed new reporting attempts.

Owner sequencing: connect synthetic inputs and finish/test validation and vendor
reporting views before the new full run and Phase 2 question definition. Reclass
engine creation/extension is deferred to the Q2 response, not an admission gate.

Real-snapshot preflight found 371 ambiguous names (3 agency, 268 fund, 2 object,
80 program, 18 source). The frozen reference tables have multiple internal keys
without enough dating/context to select historical keys. Corpus admission also
requires an explicit reference resolution contract and reviewed mapping evidence;
neither last-row selection nor an invented natural-key crosswalk is a correction.

## 2026-11-15 connected synthetic reporting workload (owner: Go)

New exploratory corpus workload, not a replicate or Q2 engine response. The
existing synthetic histories are bounded scenario templates. A stable SHA-256
assignment of each attributed literal vendor ID to a template connects all corpus
vendors to these input histories without changing their financial IDs, asserting
real legal-party equivalence, or splitting the zero group. Party/contract IDs in
views are scoped to the actual vendor plus synthetic template ID. Zero/known
placeholder IDs remain unattributed. Actual vendor-name coverage remains separate
from synthetic-profile coverage. Fixed mapping version, template inputs and
supplied run/as-of dates are hashed/run-manifested.

01 preparation: prepare a new bounded reference snapshot from the existing five
VA tables. For each normalized name, coalesce identical payloads and select a
lexically declared representative where attributes/context differ. Preserve an unambiguous original key; for repeated names choose the lexically first
non-surrogate payload and its largest numeric source surrogate. Emit the complete
original key/payload crosswalk and label these choices synthetic. Project the
vendor master through the existing APA numeric-ID padding contract, retain the
original master/projection receipt, and never pad unrelated PO financial IDs.
These choices are scenario inputs, not recovered VA effective dates or corrected
real-world dimensional identities. Original raw transaction/PO files and rules are
retained; source links and all generated reference files are frozen in a new run
root. The enriched vendor master remains explicit input, not a claimed new legal
vendor census. Never use a last-row-wins lookup silently.

05 reporting: extend expanded 5v, preserving its canonical posting-key controls.
Consume the declared history schemas with a bounded template index. Produce
version-selection/hierarchy validation, vendor-to-template coverage, movement and
balance cuts by dated synthetic attributes/party/hierarchy under EVENT_DATE,
AS_OF and RUN_DATE, and annual cuts by existing master attributes. All are
aggregation-only; no reclass journals or canonical replacement entries.
Movement EVENT_DATE uses the source transaction date where parseable; otherwise
an explicitly labeled period-grain fallback. Posted balances use declared period
cutoff, not an invented event date. Dates before the template baseline remain
visible missing-version cases. All hierarchy hops use the same lookup operand;
recorded/arrival dates are provenance, not a silent database snapshot filter.

Physical placement: history interpretation executes within the current-vendor
reporting section, after real inputs have reached that vendor break. Stream raw
records into bounded partial aggregates; reduce local summaries at the break.
Existing 5r separately reduces cross-vendor totals. New cuts use the existing
bounded ReportingSums spill/fan-in contract. Keep raw scan/index cost, template
joins, financial/reconciliation work and analytical materialization visible.
No in-memory corpus-wide vendor registry or name census. A small generation
validator is not proof of a target GenevaERS runtime capability; GERS-002/003 and
independent architecture promotion remain open.

Falsifiers: unchanged reporting/APA baseline; named connected two-year fixture
with known money and explicit profile assignment; before/on/after attribute and
ancestor changes; all three date operands; missing/zero/placeholder coverage;
exact conservation of amount, direct amount and row counts per vendor/side/mode;
small/large spill equivalence; corrupt actual balance still blocks publication.
After these pass, one frozen-code FY03-16 financial run, legacy views and separately
measured expanded views are authorized. Inspect all controls/output coverage before
Phase 2 question definition. Reclass engine creation/extension remains Q2-response
work and is not an admission condition. No new Python or control-side disclosure.

Synthetic/global master-attribute summaries are also produced from completed
per-vendor cut rows using bounded associative partials. Their final reduction
reads partials after the vendor stream ends, not canonical history again. This is
a declared analytical reduction boundary inside exploratory 5v, not claimed
in-flight CKB or target-runtime functionality. Record its spill bytes and merge
passes in synthetic_metrics; existing financial cross-vendor 5r remains separate.

Final assignment placement: SHA-256 scenario assignment runs only in standalone
input preparation, from the canonicalized master IDs and structurally parsed PO
ID candidates. That bounded candidate inventory (maximum 100000 IDs) is not a
legal-party census or financial acceptance count. Its sorted vendor/template CSV
is then a declared additional input to ReportingPass's existing literal-ID merge;
only the current vendor's assignment is retained. No corpus-wide assignment map
or hashing/generation algorithm executes inside the measured view. Inventory rows,
source sizes and the prepared stream's hash are evidence. Missing assignments for
attributed financial IDs fail publication; zero/placeholder assignments are forbidden.

## Controlling owner clarification: multiple business processes, 2026-11-15

The corpus-generation/reporting plan must model multiple business-process CKB
flows. Common-key choice is not limited to vendor, and a new sort key is not by
itself a new business process. Reclass journal generation always belongs within
the relevant vendor/employee/taxpayer/other entity's temporal CKB passes; it is not
a detached cross-entity financial engine stage. Reclass implementation remains
Q2-response work, but its intended placement is now explicit.

The vendor-only scenario-assignment/reporting changes currently in the working
copy are a narrower prototype, not completion of the multi-process generation
requirement. Before admitting the new corpus run, declare the business-process
graph: actual/synthetic input grain, process and entity namespaces, common-key
components, temporal state/rules, generated record contracts, downstream consumers,
and which compatible flows can pipe versus require sorting/materialization.
Distinguish vendor procurement/settlement from payroll/employee and revenue/
collection processes where the synthetic scenario actually supplies those inputs.
Do not relabel agency-grain public receipts as observed taxpayer activity or
opaque payroll pools as observed employee records. Cross-process generated data
must carry explicit synthetic identity, lineage and amount/row conservation.

The next architecture decision concerns this business-process graph, not adding
one standalone CKB for every view/grouping. Preserve the passing fixture/prototype
evidence, but do not call it a complete multi-CKB experiment or launch the full
corpus on that basis alone. Original raw hashes verified; generated candidate
assignments remain preparation evidence, not validated legal-party or process IDs.
