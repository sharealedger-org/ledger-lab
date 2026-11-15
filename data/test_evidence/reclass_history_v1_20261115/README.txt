Synthetic reference/hierarchy input generation v1 - owner authorized 2026-11-14

Delivered: data/synthetic/reclass_history_v1, generated with 32 instruments/parties.
207 history rows across six schemas; six change events; 14 lookup expectations;
six hierarchy-path expectations; one quiet-instrument context; three negative
hierarchy variants. See generated README.txt, schema_contract.csv and assumptions.csv.

Owner clarification: official VA publications informed synthetic choices but are
not corpus inputs, runtime dependencies, or validity conditions. The downloaded
PDF/HTML files were removed from current repository data; research URLs/hashes
and observations remain in data/reference_history_research_20261115. A temporary
local cache may disappear without affecting this pack. All generated rows, keys,
scenario assignments and baseline dates are synthetic, including borrowed dates
applied to fictional entities. No real-surrogate/date ambiguity is resolved here.

Validation:
- Unchanged APA baseline before implementation: 11 tests passed (baseline_tests.log).
- Final generator/validator/APA suites: 18 tests passed (7 new + 11 unchanged).
- Actual generated CSV pack separately consumed by the bounded Scala validator:
  PASS, 207 rows, all 14 lookup and six hierarchy expectations.
- Two independent invocations through scripts/generate_reclass_history.sh produce
  byte-identical contents, including metadata and manifest. regeneration.diff is
  empty; regeneration_result.txt records the comparison result.
- All 18 generated data/metadata/negative-file hashes match manifest.csv.
- Shell syntax and git diff whitespace checks pass. No Python added.

A prepublication consistency review found SAL/hierarchy agency disagreement for
the quiet instrument. Corrected the generator; added a regression rejecting that
contradiction; regenerated and independently replayed the corrected pack. Files
prefixed preliminary_ are earlier preparation logs, not final delivery evidence.
The delivered source SHA-256 is in generation_config.csv and source_sha256.txt.

Reproduction (from repository root, new absolute destination):
  bash scripts/generate_reclass_history.sh --out /absolute/new/path --instruments 32
Validation (from 02-foundation/va_pipeline):
  sbt 'runMain org.universalledger.foundation.va.corpus.SyntheticReferenceHistoryCheck --in /absolute/new/path'
The compiled generator uses no downloaded publication, network request, randomness
or machine-current date. Building Scala requires the normal project dependencies.

Scope: input-fixture generation/validation only, outside measured financial work.
No new SJE/reclass engine, posting, backdated walk-forward, model/cost measurement,
full-corpus run, real-identity remap, Gold change or Control-repository reading.
Explicit end dates/gaps and target GenevaERS implementation remain separate work.
