# Perspective Tests

Tests will be organized around invariants rather than only example outputs.

Required invariants include:

- generated debits and credits sum to zero;
- every generated transaction traces to a source event and rule version;
- shared step-ups produce identical totals in every consuming perspective;
- re-running the same specification is deterministic;
- changing a perspective-specific rule does not alter unrelated perspectives;
- effective-date changes produce the intended historical view;
- materialization choices preserve capability and control totals.
