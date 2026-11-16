# Reassigned zero-vendor activity preparation v1

2026-11-16; owner-approved second generation class. Source preparation in Scala,
Bash orchestration only. Original VA records and known actors receive no changes.
Status: verified for the declared full v6 workload. All 49,091 original/public/
actual nominal classifications match; actual supplier recurrence is measured in
the [full receipt](../data/test_evidence/phase2_generation_20261116/full_v6/README.md).
This is constructed supplier attribution, not recovery of real parties.

Input is retained accepted canonical SJE output (not a financial replay). Read only
zero-ID EXP nominal debit records from current completed Run 1, with exact source
locator, amount, currency, agency/fund/object and source transaction date retained.
Use existing source-description classification to exclude generic, bookkeeping,
payroll/system and explicitly missing-detail labels. A remaining name candidate
is eligibility for a fictional assignment, not legal-party verification. Exclude
unparseable/out-of-source-FY dates with counters rather than inventing observed
dates. Receipts, offsets, known vendors and commitments are ineligible for this
slice. Negative expense corrections can pass if otherwise eligible; no new refund
engine is required because unchanged ARE accepts signed source amounts.

Selection: deterministic bottom-hash sample per source fiscal year, bounded by
an explicit sample cap (executed 5,000/year, at most 20,000/year). Read all input
rows so selection is independent of input order and avoids first-key bias. Selection
hash uses frozen seed and retained source locator. Keep only the bounded sample
payload; do not load the zero bucket. Record source hashes/row counts, eligibility
and rejection reasons, selected rows/net amounts, sample cap, source/code versions.
Do not call sampled nominal record counts distinct vendors or original source
coverage. Source period/event grain and eligibility assumptions remain explicit.

Sort selected rows by agency/fund/object then fiscal year/date/source locator.
The stable context excludes year. Generate a bounded recurrent supplier roster
per context and a smaller shared object/category pool, with stable opaque IDs from
the accepted identity contract. Assignment is a versioned synthetic rule using
stable source-name/profile draws, independently of public ID numerical order. A supplier
may recur across years and contexts; missing years do not cause renumbering. Keep
deliberate unresolved/summary populations outside invented supplier attribution.
Actor IDs use the population namespace independently of stochastic activity seed;
different draws can change assignments among stable supplier-pool slots.

Emit ordinary entity-addressed instructions, private context/source-use lineage
and registry rows. Determining-key sort/merge then actor/date sort reunites
suppliers; source generation emits the actual APA schema and matching fictional
reference labels. Original amounts/classifications and dates (translated to the
explicit SY axis) are conserved for selected records; availability lags are labeled
synthetic because the original reporting data do not establish observation time.
No synthetic actor is asserted to be a known VA vendor. Wholly generated activity
uses distinct private identity keys but the same public identity/financial contracts.

The calibrated whole-population annual expense envelope is split into selected
reassigned net expense plus wholly generated remainder. Fail if the selected
sample leaves an invalid remainder; do not silently trim expensive records or
rescale their amounts. Receipt envelopes remain wholly generated/summary-grain.
Controls check each class and their combined envelope, exact source-to-SJE pairing,
lineage, identity, temporal selection and actual posting/report consumption.

New instruction attributes beyond the existing core ten fields are declared before
implementation: agency key, fund key, object key, generation_class and origin
source locator. Receiving entity records retain them after the core fourteen.
New source public evidence includes generation_class and explicit SY dates;
original real-source descriptions/locators stay in private provenance where not
needed for business evidence. Reference preparation sorts/coalesces keys and never
loads an unbounded registry. Initial output materializes all order boundaries.

Required cheapest checks: source eligibility/currency/date rejection; input reorder
invariance; identical supplier IDs in two years and across shared contexts; maximum
roster/sample bounds; selected-amount and combined-budget conservation; raw-schema
round trip through actual unchanged TTL/ARE/post/report; source isolation and SY
knowledge cutoffs. Target-runtime gaps remain open and source preparation is not
a new comparable cost point. Refund variety remains optional beyond accepted
signed expense rows already present in selected source data.

**Routed period and roster refinement:** Preserve original fiscal posting month
separately from source transaction date. It is the sixth routed attribute and is
checked on actual SJE output. Fictional supplier pools are at agency/object-category
(32 local slots) or shared object-category (8 slots); AFO context membership can
therefore share suppliers across funds/categories with declared limits. Repeated
source-name patterns select a stable fictional profile; this remains constructed
truth, not real-party matching. At most 40 suppliers are live per current AFO
context. Public registry duplicates coalesce only when all attributes agree;
actor-ID collisions fail. Context definitions, memberships and source-use lineage
are retained separately from observable business evidence.

**Conservative post-sample category gate:** The actual reference hierarchy includes
personal services, transfers, budget redistribution and financing objects that must
not be labeled suppliers merely because a source name contains letters. After the
bounded initial sample is frozen, retain categories 12/13/15/21/22/23 with additional
interest/principal/loan/pension/grant/travel exclusions. Record every rejected row
count/net amount and preserve the initial selection hash/manifests. This is a second
independent classification filter without refilling the sample, not a claim of
5,000 final eligible records/year or proven legal-party identity. It avoids another
95-GB scan while retaining a reproducible random subset of the admitted category
population. Unknown/unsupported categories remain outside supplier assignment.
