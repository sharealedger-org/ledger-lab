# FY2012 next-test preparation receipt

2026-11-15. No new generator implementation or financial pipeline run.
`5e44a8e` pushed to main before preparation. Runtime setup is local; cloud archive
copy remains conditional on successful future execution and independent verification.

Verified official PDF tables: G printed p50/PDF p6; L printed p174/PDF p2 and
printed p177/PDF p5. Web PDF screenshot requests failed; downloaded the same URLs
to `/private/tmp/component_{G,L}_FY2012.pdf`, rendered those pages with macOS PDFKit,
and visually inspected fund/Actual columns, units and signed bridge items. Do not
rely on PDF metadata titles (which misleadingly mention 1995); printed pages are
FY ended June 30, 2012. URL/SHA/page details are in the anchor fixture manifest.

The new verified anchor and bridge CSVs are self-contained runtime facts. Official
PDFs are optional provenance. Three focused Scala tests passed using only local CSVs:
statement flow identities, six signed closing adjustments, 1% scaling after unit
conversion, and negative scope/measure/sign cases. `anchor_tests.log` preserves the
actual output. No observed-APA fund mapping/tie-out, posted adjustment, scale
measurement or AI result follows from these checks.

The proposed scale ladder is in `data/synthetic/component_anchor_2012_v1/` and
implementation/admission requirements are appended to the existing pilot contract.
Current pilot does not yet support those years/counts. No cap was raised and no
unbounded set/merge was silently adopted. Food stamps excluded from General Fund:
source note 10 belongs to Federal Trust Fund appropriations. No empirical model
claim is encoded by the 1% synthetic calibration fraction.

Final preparation checks: three tests passed, including actual test configuration
and all scale-case source/SJE counts. A separately declared zero-key report probe
was added at the owner's request before any scale admission; see the pilot
contract's unattributed-key practicality projection section. It measures report
runtime and storage, not the unimplemented FY2012 component generator.
