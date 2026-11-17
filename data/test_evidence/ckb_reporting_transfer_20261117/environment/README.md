# Mid-run environment observation

Captured2026-11-17T14:47:00Z while the original extractor82547 remained live.
The host reports2 physical cores,4 logical CPUs and16GiB RAM; the actual reporting
configuration remains ActiveProcessorCount1 and Xmx2048MiB. This is host capacity
versus configured JVM capacity, not a measured parallel speedup.

host_jvm.txt records OS/model/memory and the version printed by the exact JDK
installation used in the observed extractor command. build_at_launch.sbt is the
exact declared build from frozen reporting source d9dbd2d. These are mid-run
observations/source declarations, not retroactive proof of launch-time thermal,
cache or competing-workload state. Retain the initial process/config/class-hash
receipts as the execution linkage. No engine restart or configuration change.
