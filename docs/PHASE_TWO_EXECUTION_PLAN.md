# Phase 2 execution through generation and one full financial pass

## Latest execution sequence (supersedes historical checkpoints below)

Owner now requests a preparation commit before reboot and a separate final
results commit after the integrated run. Finish and test preparation first;
leave the large execution stopped for owner reboot. Then re-read original raw
sources through transformation, normalize historical identity/calendar, combine
with wholly generated sources, post and report once. Reused synthetic financial
copies are excluded from additive inputs. Conditional journal expectation is
266,319,818, not the obsolete reporting-input sum below; ledger cardinality is
unknown until reposting. The authoritative integration contract is
[derived baseline](PHASE_TWO_DERIVED_BASELINE_CONTRACT.md).

Prior derived corpus retirement was explicitly authorized and completed, with
raw originals, immutable v6 package, historical reports and GL/control summaries
preserved. About 347 GiB is free; this is capacity evidence, not full-run admission.
After report validation, iterate only for named findings, commit accepted results,
and create a Zenodo upload script. No upload/publication or Q2 evaluation yet.

2026-11-16. Owner authorized autonomous execution after reviewing the plan;
owner validation is sought on concrete sample/results, not routine implementation.
Paper 3 receives generation methodology/evidence; Papers 1/2 proceed on ARM.
No Q2 model evaluation. No retained corpus deletion without explicit approval.

## Owner scope correction: historical plus synthetic full run

Owner clarifies that “full corpus” means retained historical data plus the new
synthetic addition, not the complete synthetic population alone. Previous v6
execution is a successful addition-only validation; its controls are preserved,
but the original requested combined validation is not complete. Do not commit
the package as fully validated for that scope.

Expected financial reporting inputs: historical 273,380,488 rows plus synthetic
6,988,627 rows = 280,369,115 rows. These are journal and ledger rows, not unique
transactions. The synthetic count of 2,444,027 describes its financial source
events and is a different grain.

Next steps: reconstruct retained producers/consumers; declare combined source/
population and calendar namespaces, consistent identity handling, and treatment of
reused historical activity; verify a bounded integrated fixture; measure combined
storage/temporary partitions and arrange verified recovery/offload if required;
then execute the combined workflow including reporting, reconcile the separate
populations and combined controls, and assess implications for the Q2 papers.
Preserve historical/SY time semantics and provenance; do not interpret the sum of
reused and observed activity as new real economic activity. No model evaluation.
The existing 26.37-GiB comparable-pass budget covers v6 alone and is not admission
for the combined workload. Any retained deletion still requires explicit approval.

The completed-results table below describes the synthetic addition only.

## Current execution outcome — ready for owner validation

The completed v6 attempt is `session-run.tmp/phase2_full_v6`.

| Plan step | Result |
|---|---|
| Specification, calibration map and inspectable sample | Complete; historical v1 sample retained, corrected v6 actual pilot accepted |
| Separately versioned Phase 1 identity preparation | 65,716-key registry and bounded actual post/GL checks; historical dataset not replayed |
| Stable synthetic identities | Explicit population independent of activity seed; extension/order/non-bunching controls and cross-seed source artifacts pass |
| Two-class sample and scale admission | Complete; corrected pilot financial projections byte-identical to predecessor; 10,000-component/two-year admission passed |
| Immutable full corpus | 10,000 components/SY01–SY14; 2,444,027 financial sources and 280,000 responsibility records |
| One full financial/report workflow | Complete without restart: 4,888,054 SJEs, 6,988,627 reporting financial rows, zero reconciliation failures |
| Final read-only integrity/recurrence audit | All 49,091 reused classifications match; 2,966 of 5,744 suppliers recur across years; no financial replay |
| Phase 3 storage assessment | 73.16 GiB free; comparable next-pass budget 26.37 GiB with 25% margin leaves 46.79 GiB free, above the 40-GiB reserve |
| Paper 3 addition and validation packet | Complete in docs/tds-evidence/phase2-generation-paper3 and permanent full-v6 receipt |

No generation/financial repeat is presently needed to close an integrity defect.
Owner validation can motivate a specifically scoped new version. Extensive new
AIM model/feature design, Q2 evaluation, portfolio/risk engines, target-runtime gap
closure and new Phase 3 engine admission remain future work. All original corpus
and prior evidence remain intact; no cloud offloading was needed.

The following implementation/discussion notes retain chronology. Current decisions
and completed receipts supersede historical proposed counts and discussion pauses.

Sequence: specification/calibration -> producer/consumer trace -> unchanged
baseline -> bounded Scala generation/adapters and adversarial controls -> richer
sample -> intermediate profiling -> immutable full generation -> one full
financial/posting/reporting execution -> results assessment -> justified versioned
regeneration/rerun if defective -> Paper 3 evidence package. Preserve every attempt.

Storage gates occur before full generation, before the financial run and at final
acceptance for Phase 3. Inventory source/outputs/scratch separately, measure peak
occupancy rather than cumulative writes, reserve at least 40 GiB pending a more
specific Phase 3 estimate, and stop new admission below 20 GiB. Current starting
filesystem observation: 109 GiB free; session-run.tmp ~345 GiB, including ~134 GiB
report_sorted_v3 and ~176 GiB synthetic_reference_v2. These are observations, not
permission to delete. OneDrive folder presence is not proof of remote quota or
archival completeness. Verify destination/quota/hashes before archive; active work
stays local. Zenodo exact-version recoverability must be checked before recommending
old corpus removal. No archive or deletion has been performed.

## First builder slice and prior verification

Claim: new fictional APA/PO source records can reach unchanged TTL/ARE and posting,
while nonfinancial lifecycle evidence remains separately identified and cutoff-safe.
Cheapest falsifier before implementation: actual source/header trace and baseline
adapter tests. 22 tests pass: ComponentPilotSpec, StandardizeAndSortSpec and
StandardizeAndSortPOSpec (2026-11-16). The pilot supports APA 9/7-field CSV and
19-field quoted-tab PO. It emits fiscal folder names and source-row locators;
TTL resolves names via local reference/master files; PO uses explicit vendor IDs.
Step 2 emits 39-column canonical SJEs; existing merge/post consumes them. The new
inspection CSV headers are not accepted by these consumers. Responsibility is
nonfinancial, receipt sources remain agency-grain in the financial adapter.

Implementation will generate process-specific raw files plus separate rich source
evidence; it will not reinterpret every invoice/proposal as an accounting event.
Declare the exact financial-recognition mapping in the builder contract. All
financial amounts/control totals are Scala. Bounded multi-level external sorting
replaces pilot-wide sets and single-level fan-in. No financial engine replacement.
Generation remains prototype preparation, subject to GenevaERS gap qualifications.

Initial full-package planning profile: 10,000 components over fictional FY2031–44,
12 cash expense, 12 receipt and 4 commitment events per component/year, plus
nonfinancial responsibility/lifecycle records. That is 3,920,000 financial source
records and 7,840,000 expected SJE legs if every source is accepted. This is a
proposed complete package, not an equal-volume cost comparison with VA. Freeze
actual size only after measured scale admission; do not silently shrink on failure.

Final report must distinguish verified controls, missing capabilities, intended
missingness and modeling assumptions. Include process/partition metrics, measured
space and a Phase 3 storage/recovery recommendation. Full financial validation
is not a forecasting/model evaluation or a target-runtime capability closure.

**Refinement before full freeze:** Existing cross_vendor_totals summaries supplied
28 annual expense/receipt envelopes at an invented 1% share, mapped +28 years.
A small calibrated real-orchestration fixture follows the 10,000-component key/
storage admission. Earlier fixed monetary envelopes remain fixture history. Source
sorts additionally have a 32-MiB byte cap and explicit row/byte conservation reads.
The driver is loaded as one shell function before execution and retains its exact
script, preventing edits to the on-disk launcher from corrupting a running parse.
No source/driver edits are planned during the admitted full execution.

## Owner discussion: vendor identity and artificial calendar

2026-11-16. Execution paused at owner's request for discussion. Do not launch the
calibrated fixture or full corpus until the discussion is complete. Previously
launched admission/reporting processes may finish; inspect their actual state
before resuming. No full corpus has been launched.

Required before full freeze: canonical vendor identifiers must be stable without
natural bunching. Sequential identifiers and the draft's adjacent-component vendor
assignment do not satisfy this requirement. Artificial IDs must not reveal source
row order, component, relationship, scenario, label or mechanism through their
values/proximity. Verify uniqueness/collisions, deterministic regeneration and
assignment stability under input reordering and population extension. Test for
unintended association between identifier order/proximity and generation groups;
stability alone is insufficient. The present sequential vendor-ID implementation
is superseded for full-generation admission and must be replaced.

Owner suggests artificial vendor IDs with a separately maintained mapping to VA
vendor IDs for genuinely observed vendors. Preserve the current self-contained
fictional population as a distinct namespace. A future real-vendor mapping must
retain original source identity, evidence/provenance and mapping status; never
attach invented behavior or relationships to a real vendor by implication. Do
not infer a VA mapping for a fictional vendor or unresolved observed alias. Exact
mixed-population inclusion remains under discussion; no real-vendor crosswalk or
population change is implemented by this proposal.

Terminology clarification: the FY2031 annual envelope is the total amount assigned
to that fictional fiscal year's expense or receipt family. It is not a maturity,
bond instrument, forecast or observed future amount. The draft shifted FY2003–16
reporting patterns to fictional FY2031–44 and chose an illustrative 1% amount
scale. Both the calendar shift and scale are design choices, not owner requirements;
retain original calibration years and explain both choices before freezing them.

**Synthetic year labels, owner discussion, 2026-11-16:** Use `SY01`–`SY14`
in corpus descriptions, result reviews and paper tables. The `SY` prefix explicitly
marks fictional time and avoids presenting the corpus as a near-future forecast.
Represent within-year dates with an explicit fiscal/calendar-month convention;
SY01 is the first synthetic fiscal year, not AD 1. Any ISO-calendar backing needed
by existing Scala date parsers is an implementation encoding only, with a versioned
mapping retained in the manifest. Do not silently substitute synthetic year numbers
into ISO dates or change fiscal/leap-year semantics. Finalize and validate that
mapping before resuming generation; do not expose the draft 2031–44 calendar as
the corpus's substantive reporting horizon. Execution remains paused for discussion.

## Finance and treasury component direction

2026-11-16 owner discussion: include finance and treasury in the component design,
given the financial architecture, forecasting and risk inquiry of the experiment.
This is source-condition design, not authorization to claim implemented treasury
engines, selected AIM features or model evaluation. Execution remains paused.

Develop distinct bounded component recipes rather than applying treasury labels
to the current common service template. Candidate initial slices:

- Cash and liquidity management: cash observations, expected receipts and payments,
  timing mismatches, funding decisions and later realized movements. Separate
  planned, committed, settled and available evidence; reconcile scope/currency.
- Debt and funding administration: financing requests, approvals, issuance or
  drawdown, interest/payment schedules and later amendments/settlement. Distinguish
  contractual maturity from the SY reporting calendar. Select a simple instrument
  contract before adding debt accounting, interest or amortization engines.
- Investment and counterparty management: investment instructions, confirmations,
  holdings, maturity schedules, settlement evidence and counterparty/reference
  changes. Market/credit scenarios and probability assumptions require explicit
  provenance; no risk score or proposed valuation creates a journal implicitly.
- Financial planning and control: budget/funding authorizations, revisions,
  expected-versus-realized activity and responsibility changes across systems.
  Authorization and forecast records are not themselves recognized accounting.

Connect these selectively to operating components: one treasury funding source
may support several components; operating receipts may arrive through collection
and bank systems with separate identifiers; an expected funding event may fail,
be delayed or remain unobserved. Preserve independent treasury activity, summary
grains, partial settlement, negative matches and private relationship truth.
Choose a small number of concrete mechanisms for initial implementation; these
candidate families are not all mandatory new engines before corpus inspection.
Before implementing financial behavior, declare SJE rules, actual consumers,
sort/state boundaries and controls under the architecture gates. Keep observed VA
vendor mappings separate from fictional borrower, issuer, bank and counterparty
roles; do not overload vendor identity to represent every economic participant.

## Long-term funding and portfolio/liability scenarios

2026-11-16 owner discussion: support questions such as expenses growing 20%, future
funding needs, and the risk of a proposed portfolio over twenty years given current
bond obligations. This extends source conditions beyond transaction timing.
Execution remains paused; no forecasting engine, portfolio recommendation, selected
AIM design or Q2 evaluation is implemented or launched.

The corpus contract must distinguish historical evidence, proposed scenarios and
subsequently realized observations. A fourteen-year historical panel alone is not
a twenty-year forward horizon. For example, an issue cutoff at the end of SY14
requires a target through SY34; finalize observation and projection horizons
separately before generation. Future realized mechanisms, if generated for later
validation, must be withheld from decision-time evidence and not treated as blind
evaluation merely because they have later dates.

Required source families: opening liquidity and assets; current bond contractual
cash-flow schedules and instrument terms; funding/revenue assumptions; expense
plans with explicit scope and growth convention; proposed portfolio holdings,
weights, cash flows, currencies and liquidity/credit terms; dated scenario drivers,
policy constraints and alternative funding/refinancing plans. Distinguish a one-time
20% expense increase, 20% cumulative growth and 20% compounded annual growth; never
select one silently. Retain scenario issue date, baseline parent, effective period,
versions, units, nominal/real basis and provenance of assumptions.

Conditional inquiry can compare funding gaps, debt service, maturities and asset
cash flows under declared growth/market scenarios. Probability of shortfall,
portfolio loss and joint disruption requires separately specified uncertainty,
dependence and probability provenance; deterministic scenario execution alone does
not establish those risks. Future interest, amortization, valuation, impairment or
refinancing accounting requires declared Scala SJE contracts and downstream posting
where accounting recognition is intended. Analytical exposures and proposed actions
remain distinct from accepted financial state. Source scenarios should support
later evaluation without presupposing the eventual AIM models or features.

**Owner request for practical CKB organization, 2026-11-16:** Added
PHASE_TWO_CKB_GENERATION_TOPOLOGY.md. It declares calibration, identity/source
routing, context CKB, determining-key merge, entity-temporal CKB, source/knowledge
emission and existing financial consumers, with explicit sorts and shared-order
piping opportunities. Whole corpus streams through processes; only current-key
state is buffered. Distinct treasury source recipes and limited variability precede
large scenario ensembles or AIM model work. Tested laptop evidence is recorded
separately from unimplemented topology/treasury support. Execution remains paused.

**Dedicated zero-vendor preparation, owner discussion 2026-11-16:** Initial vendor
assignment cannot use the unresolved vendor bucket as its business CKB key. Evaluate
agency/fund/object context order with year/period sub-breaks, persistent synthetic
supplier rosters and shared memberships, then resort attributed events to vendor/
date order. Preserve supplier continuity across years without isolating each
context's supplier universe. Distinguish reuse of profiles/blocks from duplication
of original financial events; retain lineage and explicit controls. Details are in
PHASE_TWO_CKB_GENERATION_TOPOLOGY.md. Discussion pause remains in effect.

## Owner decisions: bounded scope, proportionate provenance, Phase 1 upgrade

2026-11-16 discussion. Owner accepts bounded operating components, cash/liquidity
and a simple funding instrument. Refund/reversal variety is optional when cheap;
it must not become a prerequisite or expand the implementation substantially.

Vendor identity is now an explicitly requested Phase 1 upgrade, then a preserved
foundation of synthetic work, not merely a change to generator labels. First audit
what existing Phase 1 identities/groupings actually guarantee. Specify stable
artificial identity and any supported original-VA mapping, cross-year continuity,
non-bunching and unresolved/ambiguous status. Separate ID recoding from inferring
that two source identities are one vendor, and from assigning fictional vendors to
unresolved records. Do not silently resolve real-world identity ambiguity.

Version the upgraded Phase 1 identity inputs/mapping and implementation separately
from the retained historical baseline. Check mapping cardinality, stability across
years/input order/population extension, non-bunching, provenance and unchanged
account-level financial controls where the change is identity-only. Identify which
Phase 1/AIM results depend on the changed identities before recommending targeted
re-evaluation; no automatic full reporting repeats or retroactive baseline claims.
Synthetic preparation must consume the same accepted identity contract afterward.
The detailed Phase 1 audit/implementation remains pending this discussion pause.

Ancestry should serve concrete purposes: distinguish observed/reassigned/wholesale
synthetic data, identify copied empirical blocks that could cross future evaluation
splits, explain systematic recipe artifacts and retain ordinary financial source/
rule/outcome lineage. It does not require storing every stochastic draw or treating
all fictional variation as evidence about a real source. Retain the frozen dataset,
file hashes, generator revision/parameters, identity-map version and run seed;
record block/source mappings when data are actually reused. Small seed/version
metadata is recommended for debugging and controlled regeneration, not as proof of
economic realism. A frozen stored corpus alone suffices to rerun an experiment on
those exact bytes. Do not make exhaustive random-draw ancestry a new workstream.

## Separate synthetic actors: owner clarification

2026-11-16 discussion. Owner observes that stable vendor and other process/entity
identities can provide practical synthetic traceability without mixing generated
activity into existing vendors. Adopt that as the default, superseding candidate
mixed observed/synthetic actor plans for the initial corpus.

Keep the requested Phase 1 identity upgrade separately versioned and preserve its
accepted identity contract in synthetic work. Synthetic vendors, employees,
funding counterparties and instruments have their own stable identities in explicit
namespaces. No new synthetic behavior is attached to an observed VA vendor. A
real-VA crosswalk belongs to the observed Phase 1 identity mapping where supported;
it is not a mandatory linkage from each synthetic vendor to a real vendor.

Trace synthetic activity through stable entity IDs, process-local event/document
IDs, supported cross-process references, source-to-SJE locators and tentative/
resolution/version links. Keep private generation relationships separate from
observable business references; do not expose a universal component answer key.
These links provide operational/financial traceability without a per-random-draw
ancestry system. Use aggregate VA calibration or separately declared reused activity
patterns without implying that the synthetic actor is the original VA actor.
Retain modest source/block provenance only where actual reuse makes it useful.
Execution remains paused for discussion; no population or identity implementation
change has yet been executed.

## Active revision — autonomous execution resumed

2026-11-16. Owner explicitly resumes agentic execution and accepts two generation
classes: `reassigned_activity` (eligible zero-vendor expenses supply real activity,
fictional supplier assignments) and `wholly_generated` (component recipes supply
actors/activity). Both use the accepted stable, non-bunched artificial identity
contract; no synthetic activity is added to known real vendors. Previous discussion
pause entries are historical and superseded by this resumption.

Execution order now: (1) trace/test and implement a separately versioned Phase 1
identity upgrade, preserving old baselines and unresolved/summary status; (2) carry
that contract into synthetic preparation and SY calendar; (3) implement bounded
agency/fund/object/time reassignment with persistent/shared synthetic suppliers;
(4) improve distinct operating/cash/simple-funding recipes and limited variability;
(5) inspect/test the combined two-class sample and actual downstream consumers;
(6) profile/admit full generation and one financial/report pass; (7) assess results,
Phase 3 space and Paper 3 evidence, with justified versioned iteration if needed.
Refunds optional, provenance proportionate, forecasting/portfolio modeling and Q2
evaluation deferred. Archive/removal controls and Scala/Bash boundaries unchanged.

### Execution acceptance and iteration

The first combined pilot uses 100 components over SY01–SY02 with completed,
conservatively filtered zero-vendor selections and the frozen annual calibration.
Verify source cardinalities, exact combined monetary envelopes, actor reuse,
separate posting/effective/availability dates, reference completeness, cutoff
isolation and actual SJE/post/report reconciliation. Then use a representative
10,000-component/two-year admission to estimate full retained storage. Admit the
10,000-component/SY01–SY14 corpus only with a 40-GiB free-space floor and a stated
Phase 3 footprint estimate; monitor space throughout. Generate one immutable full
package and execute its recognized financial sources once through the existing
Scala financial and reporting path. This is the requested new-corpus evaluation,
not an old-reporting replicate or Q2 model evaluation.

Assess source/class distributions, calendar/delay conditions, treasury principal
controls, financial lineage/conservation, process memory and disk expansion.
Fix demonstrated defects in a new version and rerun affected validation before
considering another full generation/pass. Retain failures as evidence. Prepare a
Paper 3 methods/results addition and a final validation packet for the owner.
Do not require owner input for routine reversible preparation. Archival/deletion
of retained evidence requires a concrete verified recovery plan and explicit
authorization; no such deletion is currently needed or underway.

### Population-identity correction and measured admission

The v5 10,000-component/SY01–SY02 admission completed with 350,326 financial
sources, 700,652 SJE rows and 1,020,536 reporting financial rows; reconciliation
failures zero. Allocated root 4,301,916 KiB, including 1,076,396 KiB of reusable
inputs. Generation process wall 142.13 s, maximum RSS 1,236,647,936 bytes; report
extract engine elapsed 196.76 s. Timings remain engineering observations.

Review found synthetic callers used recipe seed as population identity. V6 adds
an explicit population setting independent of stochastic seed. Fifteen focused
tests pass, including generated masters, party crosswalks and funding instruments
unchanged across two activity seeds with different actual financial events. This
is an identity-only, same-width revision; v5 remains storage-admission evidence,
qualified by the corrected actual pilot and account-level GL equivalence gate.

Conservative full-run budget: seven times the completed two-year footprint is
about 28.7 GiB; add 25% for a 35.9-GiB admission budget. Current free space is
about 100.6 GiB before the corrected pilot. A subsequent same-input Phase 3 pass
is provisionally 21.5 GiB of additional non-input materialization, or 26.9 GiB
with the same margin. Recalculate after the actual full run; Phase 3 is not yet
admitted. Keep the 40-GiB stage floor and 20-GiB next-stage stop. If the measured
Phase 3 budget does not fit, prepare a verified archive/recovery proposal before
requesting any retained-corpus removal. No archival or removal is underway.

Corrected v6 combined pilot completed all source/financial/report controls.
FY2031 and FY2032 GL files and cross-vendor aggregate report are byte-identical
to v5. These are actual consumer equivalence checks, beyond identifier unit tests.
Full v6 is admitted with 105,193,548 KiB free versus a 35.9-GiB conservative
retained-run budget and the 40-GiB stage floor. Host AC attached; file-descriptor
limit 1,048,575 versus at most 400 source partition writers. Freeze main code,
config and driver for `session-run.tmp/phase2_full_v6`; no edits while active.
