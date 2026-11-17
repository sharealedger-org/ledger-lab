# Full combined-corpus reporting sort completed

2026-11-17. All14 years /28 movement+balance partitions completed. This is
sort-stage evidence; expanded reporting extraction/reduction/acceptance remain
pending in session-run.tmp/phase2_fused_reporting_v2.

Actual sort manifest contains286,277,734 financial rows and102,677,962,038 bytes:
266,319,818 movement rows plus19,957,916 ledger rows. Every partition count
matches the accepted financial year summaries; all28 keys are unique and use
chunk100,000/fan-in16. count_comparison.csv retains the comparison. Each manifest
source path resolves to its declared accepted financial producer; source and
sorted output byte sizes match the manifest in file_size_comparison.csv.

The Scala sorter produced source/sorted SHA-256 values in sort_manifest.csv.
They are retained producer evidence, not a claim of a second independent full
content scan. Final report input hashes must be compared to sorted hashes after
extraction completes. The reporting class manifest was independently rechecked
successfully at the transition, including after SBT extraction started.

Sort process elapsed4,603.96seconds, maximum RSS639,401,984bytes, under the2GiB
heap /one active processor profile. Diagnostic timing only, no controlled cost
point or speedup claim. Live storage monitoring and40GiB reserve remained enabled.

Actual extraction admission passed:125,990,400KiB free vs82,358,090KiB budget
plus41,943,040KiB reserve; margin1,689,270KiB. No gate was weakened. Current
extraction uses the same presorted expanded profile and all14 sorted directories,
full dimensions and generated histories. Run/configuration and final report
controls will be retained separately after completion. No allocation, new
basis-generation process, Q2 model execution or upload/publication is implied.
