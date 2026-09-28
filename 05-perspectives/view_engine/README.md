# Generalized View Engine

This component is an aggregation engine for `05-perspectives`. It will replace the current
year-wide in-memory view loop with a reusable execution contract. It does not generate or apply
accounting transactions.

It must consume sorted Foundation outputs and support:

1. Record filtering.
2. Effective-dated step-up resolution.
3. Row-level calculations.
4. Concurrent aggregation into multiple targets.
5. Replacement pivot and report emission.

The engine must not rely on database physical order. Postgres may be loaded from replacement flat files for downstream access, but the CKB sort contract remains the Foundation responsibility.
