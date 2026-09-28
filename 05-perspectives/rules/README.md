# Step-Up Rules

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
