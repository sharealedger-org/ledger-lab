# Phase 1 identity upgrade v1

2026-11-16; owner requested a separately versioned identity upgrade, carried into
synthetic preparation. Status `partially_verified`: full compact map prepared and
bounded real posting/GL equivalence verified; no full historical financial replay
or new AIM result. Contract: docs/PHASE_ONE_IDENTITY_UPGRADE_CONTRACT.md.

Complete retained coverage: 65,716 exact old identity keys map uniquely, with three
unresolved/special keys preserved. Root session-run.tmp/phase1_identity_upgrade_v1.
No source identity merging or legal-party inference. Original baselines immutable.

22 unchanged adapter/component tests pass. Three new tests cover stable IDs under
reorder/extension, duplicate rejection, non-bunching checks on 10,000 identities,
source column preservation and actual posting/GL consumption. GL is byte-identical;
inverse-mapped ledger business fields match, excluding physical row ordinal.

The historical concatenated financial comparator has literal vendor decreases;
application therefore has explicit old-vendor sorting before the registry merge
and financial-comparator sorting afterward. Both are preparation work with bounded
chunks/fan-in; no hidden in-memory population lookup or target capability closure.
The opaque ID is constant-prefix plus 36 decimal digits, SHA-256 domain-separated
from data generation seed and modulo 10^36. External collisions fail without
renumbering. This is an offline registry, not a measured runtime ID allocator.

Interpretation: known source keys already recur literally across years, but the
old pipeline has no general non-bunched opaque-ID layer. This upgrade preserves
existing grouping, including unresolved source ambiguities. A bijective recoding
preserves ID-independent financial features/targets. Historical split membership
must be translated if any future model comparison is done; a fresh sort/shuffle
would not be the identical split. No automatic claim that original AIM scores
improve or become invalid. Existing fiscal-period qualifications remain unchanged.
