# Perspective Engine Boundary

**Historical scaffold; ownership corrected by the current layer contract:** The generation/application responsibilities proposed below belong to [04 engines](../../04-engines/calculation_engines/README.md) and the Foundation apply path. [05 perspectives](../README.md) aggregates only. Keep this older proposal as design history; do not implement it as a second financial engine under 05.

The future Scala implementation belongs here or in the existing Scala package structure with this directory as its design boundary.

The engine must support:

1. Reading source events and core balances.
2. Resolving effective-dated attributes and rules.
3. Generating balanced step-up transactions.
4. Applying generated transactions to the appropriate ledger state.
5. Emitting perspective outputs without duplicating the shared core.
6. Writing control totals and provenance suitable for independent validation.

No engine code is introduced by the initial infrastructure work.
