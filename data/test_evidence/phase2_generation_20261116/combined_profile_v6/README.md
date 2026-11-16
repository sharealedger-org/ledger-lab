# Corrected population identity pilot

Completed 2026-11-16 at `session-run.tmp/phase2_combined_profile_v6`.
100 components, SY01–SY02, calibrated envelopes plus 7,671 reused expense records.
11,147 financial sources, 22,294 canonical SJE rows. Both availability-cutoff
exports, exact source/date/period/amount/rule lineage, actual posting and expanded
report checks passed; original driver completed without a resume.

V6 separates population identity from the stochastic activity seed. Fifteen
focused tests pass, including generated vendor masters, party crosswalks and
funding contracts stable across alternative activity seeds. Recipe activity changes.
Observed Phase 1 identity mapping is a separate unchanged contract/evidence set.

Actual `cmp` checks returned zero for FY31/FY32 GL files and the cross-vendor
aggregate report against v5. `identity_equivalence.sha256` preserves both hashes
for each pair. Canonical IDs and source physical ordinals may differ; these are
account-level equality claims, not byte equality of rekeyed detailed journals.

The v5 larger admission supplies same-width identity-only resource evidence.
Full v6 remains separately gated. No model evaluation or clean cost-point claim.
