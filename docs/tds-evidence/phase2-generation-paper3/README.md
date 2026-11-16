# Paper 3 addition: fictional source generation method and evidence

**Current scope and sequence:** Results below validate the synthetic addition
alone. Owner requests a preparation commit, reboot, then one fresh integrated
raw transformation/posting/reporting run and a separate validated-results commit.
The integrated baseline replaces eligible historical zero-vendor attribution and
adds only wholly generated activity. Conditional journal expectation is
266,319,818; the earlier 280,369,115 reporting-row append is withdrawn. Ledger
cardinality and interpretation await the new run. Its v6-only capacity estimate
is not integrated admission. A Zenodo upload script follows validation.

2026-11-16. Working evidence/draft for the separate writing workflow. No private
manuscript was read or edited. Full v6 generation, one financial/report workflow
and the separate read-only integrity audit completed successfully. Use the permanent
[execution receipt](../../../data/test_evidence/phase2_generation_20261116/README.md)
and versioned corpus/implementation contracts for the current evidence state.

## Proposed method passage

Phase 2 introduces a self-contained fictional source population rather than
inventing missing links inside the observed Virginia records. The completed
reporting experiment supplies aggregate workload observations, including financial
row and byte volumes, reporting key cardinality, temporal output expansion and
the concentration of records in an unattributed group. These observations inform
workload selection and resource admission. They do not identify real actors,
establish component relationships, estimate payment-delay distributions or supply
risk probabilities. Annual expense and receipt monetary envelopes are calibrated
at a declared 1% scale from selected nominal movement accounts in those completed
reports, with exact cent rounding and residual controls. Population size, invoice
counts, variability, delays and operational mechanisms remain synthetic
assumptions; they are not fitted distributions.

The population has two declared generation classes. `reassigned_activity` preserves
eligible zero-vendor source expense amounts and classifications, with transaction
and posting dates translated together into the declared synthetic calendar while
preserving month/day and fiscal positions. It assigns fictional suppliers through bounded
agency/fund/object contexts and persistent supplier pools. `wholly_generated`
supplies new actors and recipe activity. The latter's expense budgets subtract
the former's net amounts, so the combined annual envelopes are conserved. No
fictional behavior is attached to a known real vendor. Separate private source
locators support reused-block tracing without a per-random-draw audit trail.

Both classes use a versioned opaque stable identifier contract, independently
tested for extension/order stability, collisions and numeric bunching. A separate
Phase 1 registry upgrade preserves literal source identity groupings and unresolved
sentinels rather than claiming to recover missing parties. Public synthetic dates
use SY01 and subsequent fiscal-year labels; an explicit backing-calendar map
supports existing source adapters. Synthetic labels require calendar parsing,
rather than lexical date comparisons across July–June fiscal boundaries.

The source preparation process begins with bounded component/year recipes. Each
recipe emits process-addressed instructions for cash expenses, receipts,
commitments and nonfinancial responsibility evidence. Source identities are
resolved through a materialized determining-key sort and streaming registry
merge; another external sort brings instructions into process/entity/date order.
An external identity sort checks uniqueness. Sort buffers and merge fan-in are
bounded independently of population size. Current implementation consumes an
ordered context-definition partition before emitting bounded recipe events; it
does not demonstrate the proposed generalized context CKB merge over arbitrary
definition/membership/rule streams. These preparation
boundaries and retained partitions are recorded separately from financial work.

Operational sources contain proposals, later deliveries, invoice replacements,
changed responsibility, receipt observations and explicitly canceled or unobserved
outcomes. Effective time, evidence availability, issue time, target date and
observation cutoff remain distinct. Process-local identifiers and incomplete
business references allow some connections while leaving others unsupported;
private generation truth is held separately. Optional modernization fields add
supported correlations over the same activity. Background records, incidental
overlap and shared suppliers prevent every record from being part of one
unambiguous vendor story. Future hierarchy and driver versions provide source
conditions for later scenario and risk inquiry.

Only declared financial recognition events enter the existing Scala transformation,
ARE and posting path. Proposals, invoices, responsibility evidence, scenario
references and outcome labels do not independently produce journals. Every
financial source is checked against two canonical direct/offset (D/O) SJE legs with
matching amount, date, identity policy, document, source locator and rule/version.
Expanded reporting consumes actual posted outputs and checks reconciliation and
conservation. Separate cutoff consumers inspect operational evidence without
exposing private answers. Opening cash is an analytical source observation, not
an opening ledger or an implicitly posted journal; the present financial report
basis is period movements, with annual processing scopes explicitly retained.

The exercise tests reproducible preparation and consumption of controlled source
conditions. It does not establish economic realism, the recoverability of every
private relationship, the sufficiency of the sources for an envisioned AIM model,
forecasting effectiveness or target GenevaERS runtime support. Detailed model and
feature design and Q2 evaluation remain future work. Published inspection data
cannot serve as a blind evaluation set.

## Declared recipe parameters

These are construction assumptions, not estimates fitted to Virginia activity.
All draws use keyed hashes with a versioned seed/purpose; actor IDs use a separate
population namespace. Normal draws are clipped to ±4 before subsequent transforms.

| Mechanism | Executed assumption |
|---|---|
| Component monetary shares | Positive weights proportional to exp(0.9 × persistent normal + 0.25 × annual normal), conditioned on the exact annual envelope |
| Financial record counts | Gamma(shape 2) / Poisson mixture; latent EXP means 10/5/18 by archetype, REV 3, PO 4; counts clamped to 1–24 |
| Within-component amounts | Positive exp(normal) weights; integer-cent allocation and final-event remainder conserve the component envelope |
| Event dates | Weighted fiscal-month selection (July 0.8, December 1.3, June 2.0, other months 1.0), then a uniform day within the selected month |
| Generated arrival delays | Rounded exp(1 + normal), clamped to 1–90 days; two responsibility changes retain explicit July/January role dates |
| Reused supplier recurrence | Agency/category pools of 32 slots or shared category pools of 8; 20% shared-pool choice, squared slot draw for concentration, maximum 40 live suppliers per AFO context |
| Reused availability | Later of transaction day and posting-month end, plus an assumed 1–30-day lag; no observed availability distribution is claimed |
| Funding source specimen | Up to 32 instruments, fixed 4% annual coupon, 40 semiannual contractual dates, principal at the twentieth-year maturity; no journals or risk probabilities |

Cash plans are conditioned on the same annual controls as generated activity.
They are budget/source conditions, not independent forecast ground truth. Private
mechanisms, actor/source-block mappings and calendar metadata permit future split
and artifact review. Independent generator mechanisms and model-specific sufficiency
checks remain necessary before generalization or forecasting claims.

## Completed engineering acceptance results

| Item | Evidence currently available |
|---|---|
| Unchanged baseline | Financial/report output controls pass resumed checker; original driver checker compile failure retained |
| Focused tests | Frozen v6: 15 passed across four source/identity suites; unchanged baseline and earlier reporting tests retained separately |
| Initial integration failure | New checker assumed C rather than canonical O; corrected without financial changes |
| Reviewed sample | Corrected v6: 100 components/SY01–SY02; 11,147 sources, 22,294 SJE legs; all controls pass; GL and cross-vendor aggregate report byte-identical to v5 |
| Scale admission | V5 same-width identity-only predecessor: 10,000 components/two years, 350,326 sources, 700,652 SJE rows, 1,020,536 report financial rows; zero reconciliation failures; 4,301,916 KiB retained |
| Reused-activity preparation | Read-only 14-year scan, 70,000 initial candidates, 49,091 retained by conservative v2 policy; 2,398.44 s process wall, 1,228,832,768-byte maximum RSS |
| Full corpus | V6: 10,000 components/SY01–SY14, 2,444,027 financial sources plus 280,000 responsibility records; 475 package hashes verified at both cutoffs |
| Full financial pass | One uninterrupted workflow; 4,888,054 SJEs and 6,988,627 financial reporting rows; zero reconciliation failures |
| Source and reporting memory | Generation maximum RSS 1,234,575,360 bytes; extract maximum RSS 1,199,091,712 bytes; source-sort maximum buffered payload 33,554,423 bytes |
| Source and reporting time | Generation process wall 832.23 s; extract engine elapsed 1,114.24 s and whole-process wall 1,125.02 s; full orchestration 75 min 2 s |
| Reused classification audit | All 49,091 original/public/actual nominal classifications match, with signed cents and calendar-translated dates; 88.64 s process wall, no financial replay |
| Actual recurrence | 5,744 invented reused suppliers; 2,966 appear in multiple years and 2,670 in multiple AFO contexts; 19,746 supplier/context memberships |
| Outcomes | 344,819 observed, 25,181 partial, 17,500 canceled, 32,500 unobserved; missing/canceled values remain nonnumeric |
| Storage/Phase 3 capacity | 27.93 GiB retained, 6.83 GiB reusable inputs, 73.16 GiB free; comparable additional-pass budget 26.37 GiB includes 25% margin and leaves 46.79 GiB free, above the 40-GiB reserve |

Retain per-process wall/CPU/RSS, sort/spill/materialization metrics and controls.
Build/startup, simultaneous development and host-condition qualifications mean
these are exploratory engineering observations, not clean replicated cost points.
Do not replace pending cells with extrapolations presented as completed results.

The [full receipt](../../../data/test_evidence/phase2_generation_20261116/full_v6/README.md)
and [capacity assessment](../../../data/test_evidence/phase2_generation_20261116/full_v6/CAPACITY.md)
contain permanent compact controls and point to the stored corpus. No old-corpus
removal or remote archive was needed. The capacity estimate applies to a comparable
same-input financial/report pass; new Phase 3 engines/models require admission
when their designs and materialization contracts exist.

## Owner validation before publication or scope extension

Review the bounded component mix, the invented 1% monetary scale and the two actor
classes. In particular, reused expense outliers were preserved rather than trimmed;
their relative contribution varies by year. Confirm that the intentionally simple
funding specimen is sufficient for this release, with richer portfolios, debt
recognition and probabilistic risk models deferred. Review the cutoff/inferability/
outcome conditions as foundations for later model design, without treating them
as demonstrated forecasting performance. No additional full pass is justified by
the completed integrity controls alone; subsequent iteration should answer a
specific validation finding or model-data requirement.
