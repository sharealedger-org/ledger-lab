# Phase 1 identity upgrade v1

2026-11-16; owner-authorized separate upgrade before synthetic continuation.
Verification entry: inspected existing master loader, PO reader/emitter, canonical
sort and identity audit. APA names resolve through a bounded master with numeric
zero-padding; PO retains source VENDORID literally. Earlier audit labels literal
source overlap as unverified legal-party linkage. Current code does not provide
an opaque non-bunched canonical-ID layer. Cheapest falsifier: numeric master IDs
are visibly retained and PO keys are literal. Do not claim cross-process identity
resolution from recoding alone.

Workload `phase1-identity-upgrade-v1`, logical owner 02 structural identity
preparation. Prepare and freeze a sorted explicit old-literal-ID -> artificial-ID
registry from existing canonical vendor coverage. Preserve every old known key's
grouping, every unresolved/summary sentinel and distinct ambiguous keys. No new
party inference or fictional zero attribution occurs in this upgrade. Numeric
padding variants stay distinct unless separately supported evidence authorizes
merging them. Mapping status records that it is encoding of an existing grouping,
not validation of legal-party identity. Old baselines/results remain immutable.

Algorithm: domain-separated SHA-256 of the exact typed private identity key,
modulo 10^36, fixed-width 36 decimal digits with a constant vendor prefix. The
public vendor ID contains neither component/year nor origin/label fields. It fits
the existing PO vendor shape rule (up to three leading letters then digits).
The identity domain/version is stable and independent of generation RNG seed,
file order, scenario, observation date and population size. Externally verify
one-to-one mapping and fail on collision; do not silently renumber on extension.
This is offline registry preparation, not a target-engine runtime allocator or
closure of GERS-001/002. Shared implementation is reused by synthetic registries.

Registry exact header: `original_id,artificial_id,status,identity_contract`.
Rows with unresolved/special keys retain their original key and explicit status.
All known old keys produce one unique opaque key. Source coverage input header
must include vendor_id; duplicate old keys fail. Registry sort uses a declared
encoded original key. Preparation also produces a reverse-order uniqueness
partition and controls; small bounded stats evaluate adjacent original-vs-new
rank relationships without claiming statistical independence or privacy.

Application inputs: one headerless canonical 39-field SJE partition sorted on
original vendor ID, frozen registry. Ordered merge keeps only the current mapping
row. Replace col0 only; keep all other source/rule/document/date fields byte-for-
byte. Then externally sort by the exact existing 15-field concatenated comparator
(using an order-preserving UTF-16 key encoding for the sidecar sort). Strip the
sidecar key and feed canonical SJEs into actual existing post and GL consumers.
No financial rules, balancing logic or Bash accounting is introduced. New physical
identity merge/sort/materializations are explicit; no old timing is relabeled.

Retained profile: original registry, transformed unsorted rows, ordered source
wrapper and final canonical rows. Source sort cap 100,000 rows/32 MiB, fan-in 16.
Record source/target hashes, rows/bytes, merge levels, wall/CPU/RSS and controls.
No full 95-GB rewritten canonical corpus is admitted merely to demonstrate mapping;
prepare the complete compact registry and validate bounded real downstream cases
before recommending any affected-result replay. Keep zeros and receipt policy.

Validation: baseline source/financial tests; deterministic ID under reorder/extend/
year changes; uniqueness and low adjacency/rank association on a named sample;
actual mapping on complete current coverage; adversarial duplicate/collision/
unmatched/disorder fixtures; real posting/GL equivalence for a bounded SJE case.
Compare GL byte equality and inverse-mapped ledger records at the same grain.
These establish encoding/consumption, not repaired legal identities or improved
AIM performance. Identify affected historical interpretation claims explicitly.

**Trace correction before real application:** The existing concatenated financial
sort comparator can produce literal vendor-key decreases for variable-width prefix
IDs (214 previously recorded by the identity audit). Therefore application always
materializes an explicit original-vendor-key sort before the registry merge; the
financial comparator is restored afterward. The registry/source join uses the same
encoded exact key. This adds a necessary order boundary, not a vendor inference.
Ledger physical line ordinals may change after reordering; compare inverse-mapped
business fields excluding that physical ordinal, and require exact GL equality.
