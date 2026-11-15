Synthetic reclassification reference histories v1
All records, keys, names, baseline starts and scenario assignments are synthetic.
Virginia publications informed selected change patterns/dates; they are optional provenance, not inputs.
No network access, downloaded reference publication, random state or system date is used.
Regenerate: bash scripts/generate_reclass_history.sh --out NEW_ABSOLUTE_PATH --instruments 32
Schema: schema_contract.csv. Dates: ISO yyyy-MM-dd; sort business keys then effective_start.
Lookup: fixed AS_OF, supplied RUN_DATE or EVENT_DATE; greatest start <= selected date.
Recorded dates describe change arrival, not an implicit whole-database or knowledge snapshot.
Main histories are start-only; explicit end-date/gap semantics require a separate target contract.
change_events.csv records change identity, effective and recorded dates.
lookup_cases.csv and hierarchy_cases.csv contain manually declared fixture expectations.
negative/ contains intentionally invalid hierarchy variants; never load these as positive inputs.
activity_context.csv marks an instrument requiring evaluation despite no new business activity.
This pack prepares inputs; it does not generate accounting/reclass journals or change a financial ledger.
Existing VA corpus keys, ambiguous real-name mappings and Gold remain untouched.
GENERATION_COMPLETE and manifest.csv identify a complete reproducible pack.
