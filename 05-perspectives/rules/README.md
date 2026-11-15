# Step-Up Rules

**Historical scaffold:** Generated financial step-up rules are owned by [04 engines](../../04-engines/calculation_engines/README.md); [05 perspectives](../README.md) may select and aggregate their outputs. This older vocabulary does not authorize accounting generation inside 05. Python work remains subject to [AGENTS](../../AGENTS.md), including explicit owner approval.

This directory will contain versioned rule specifications for generated economics.

Rules must identify:

- rule ID and version;
- source event or balance inputs;
- effective-date behavior;
- generated transaction class;
- consuming perspectives;
- balancing counterpart;
- reuse scope;
- control-total expectations.

Rule specifications are inputs to the Scala mainline engine. Python may validate or summarize them, but must not execute the financial flow.
