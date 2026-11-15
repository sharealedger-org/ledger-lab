# Perspective Tests

**Historical scaffold:** The generated-SJE invariants below belong to the owning [04 engine](../../04-engines/calculation_engines/README.md) and apply path. [05](../README.md) tests aggregation and conservation over declared inputs; it does not generate or apply accounting transactions. Retain these proposed checks without treating this directory as an implemented test suite.

Tests will be organized around invariants rather than only example outputs.

Required invariants include:

- generated debits and credits sum to zero;
- every generated transaction traces to a source event and rule version;
- shared step-ups produce identical totals in every consuming perspective;
- re-running the same specification is deterministic;
- changing a perspective-specific rule does not alter unrelated perspectives;
- effective-date changes produce the intended historical view;
- materialization choices preserve capability and control totals.
