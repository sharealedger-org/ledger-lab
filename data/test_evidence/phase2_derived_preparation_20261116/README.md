# Derived Phase 2 baseline preparation checkpoint

2026-11-16. Preparation only; full integrated execution and report interpretation
remain pending after owner reboot. The v6 package remains immutable.

Scala source selection completed: 2,394,936 wholly generated raw events retained,
49,091 reused financial copies excluded. Runtime subset is
`session-run.tmp/phase2_wholly_sources_v1`, including frozen master/references/rules,
original/subset locator map and per-file hashes.

`focused_tests.txt`: 25 tests pass across ten suites, covering generator/identity
regressions, subset exclusion and locator shifts, paired historical normalization,
metadata, control falsifiers, and fresh raw -> normalization -> merge -> post/GL
-> reporting sort -> sequential reporting/reconciliation. The separate
`baseline_and_integrated_tests.txt` records the unchanged 22-test baseline plus
that integrated fixture. These are fixture evidence, not full-scale acceptance.

Scala owns transformation, normalization and financial controls. Bash orders jobs
and retires consumed attempt-local intermediates. The supplier helper extracts the
existing frozen policy; generation regressions pass. V6's captured implementation
remains its provenance. No GenevaERS runtime gap is closed by these prototypes.

The driver defaults to check-only. After owner reboot, verify the committed code,
preserved inputs, AC and actual disk/materialization admission. Initial guard:
300 GiB free; each stage requires 40 GiB reserve. These provisional guards are
not a measured footprint; monitor actual free space/process peaks throughout.

Resume from repository root:

```bash
bash scripts/run_phase2_derived_baseline.sh --mode check
bash scripts/run_phase2_derived_baseline.sh --mode run \
  --out "$PWD/session-run.tmp/phase2_derived_baseline_v1"
```

Full controls require 261,529,946 historical + 4,789,872 wholly generated journal
legs = 266,319,818 journals. Ledger/report cardinality awaits posting. Historical
raw replay GL must match retained original exactly; combined GL must equal shifted
historical plus wholly generated GL at every key. Exclude reused copies before
transformation. Workflow completion is not final acceptance. Inspect reports,
iterate only for named findings, then make the separate validated-results commit.
Create a Zenodo upload script after validation. No Q2 model or publication here.
