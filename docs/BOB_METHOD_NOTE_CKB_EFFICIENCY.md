# Method Note: Token-Efficient Agentic Work on Large Financial Datasets

> **Audience:** Bob (AI assistant) trainers and future Bob instances working in this repo.
> **Purpose:** Establish the correct computational *method* for agentic work here — grounded
> in GenevaERS architecture (Paper 4) and the theoretical claims of Papers 2 and 5 of the
> monograph — so that Bob operates efficiently on large sorted files rather than burning
> context on whole-file reads, Python-style iteration, or repeated re-scans.
> **Source material:** Paper 4 (GenevaERS Architecture), Paper 2 (Pre-Hierarchy Event
> Repositories), and Paper 5 (Enterprise Financial Transformation Engines) of the monograph
> *"The Architecture of Economic State"* (Twitchell) — link will be added upon publication.
> **Status:** Normative guidance. Should be read before any agentic pipeline run or
> large-file data analysis task in this repo.

---

## 1. The GenevaERS Architecture — And Why It Is the Right Model for Bob

### 1.1 The punch card constraint discipline (Paper 4 §1.0a)

The architectural decisions that define GenevaERS — single-pass execution, sequential
streaming, in-memory key buffering, compiled logic tables — were derived by working
backwards from an extreme constraint: every input and every output had to fit on an
80-column punch card.

> *"Forcing yourself to think about the problem in that way, you start to come down to
> a very few number of operations that can be performed. There are very few types of
> inputs, not measured in lots of things but in few things. And the number of results
> created from those inputs — those are not measured in lots of things either."*
> (Twitchell, 2018c; Paper 4 §1.0a)

This constraint discipline produced a fundamental insight: the class of operations
required for enterprise financial processing is small. There are events (records),
reference tables (master files), and a small set of operations — select, join,
aggregate, emit. Everything else is overhead imposed by general-purpose systems.

**The Bob analog:** Bob's "punch card" is the context window. Every token consumed on
source data that could have been answered by a grep or a one-line summary is overhead.
The discipline of asking *"what is the minimum information I need to answer this
question?"* before opening any file is the direct application of this principle.

### 1.2 Short instruction path length as the primary design goal (Paper 4 §1.0b)

The GenevaERS performance result — **2,533 instructions per record** across 11.2 billion
rows with 19 billion joins in 4.4 wall-clock hours (Heap, 2010; Paper 4 §5) — is not a
consequence of optimization. It is the **primary design goal** from which all
architectural choices follow.

> *"Having very short instruction path lengths is required for doing these posting and
> aggregation processes efficiently. It is not something that is impossible to do; it
> is something we have done for decades."* (Twitchell, 2018d; Paper 4 §1.0b)

For comparison, standard relational DBMS SQL engines consume **50,000 to 150,000
instructions per row** — 20× to 60× more — due to dynamic SQL parsing, lock
management, and buffer pool contention. The 1999 DB2 benchmark (Paper 4 §5.2)
quantified this directly: SQL join execution was **10.3× more CPU expensive and 41×
slower in wall-clock time** than GenevaERS reading the same data via flat sequential
files. Bypassing the SQL layer entirely and reading DB2's underlying VSAM blocks
directly brought performance within **10%** of native flat-file sequential reading.

**The Bob analog:** Every tool call that loads source data into context incurs an
"instruction path length" overhead proportional to the data size. A `read_file` on a
63 MB file is the Bob equivalent of a SQL full-table scan — expensive, general-purpose,
and unnecessary when a targeted query suffices.

### 1.3 The I/O complexity proof (Papers 4 §1.1 and 5 §6.2)

Both papers state the same formal result:

> Legacy I/O complexity: **O(V × M)** — V reports each require an independent full scan
> GenevaERS single-pass: **O(M)** — read exactly once; all V views produced simultaneously

The 1996 SAP engagement (Paper 4 §2.1) provided the earliest empirical proof: a
260-hours-per-week SAP reporting burden reduced to 26 CPU hours per month — a **~100×
throughput improvement** — by replacing multi-pass SQL scans with a single-pass
compiled engine over the same data.

The Tier-1 global bank case (Paper 5 §7.1): balance derivation, FTP, and multi-view
GL/MIS extraction across billions of rows in **under 45 minutes overnight** — because
the data was read once.

### 1.4 The Spark limitation — and why sort order is a contract, not a hint (Paper 4 §6.2)

The VA dataset benchmark (Paper 4 §6): 101 million transactions → 12 million balances
across 13 years, processed in **2 hours 38 minutes on a MacBook Pro** using the Scala
single-pass posting engine. The four input streams (vendor master, updates, journals,
existing balances) were pre-sorted on the same key — the join required **minimal
in-memory footprint** because the sort order was a *contract enforced at write time*.

The Spark limitation (Paper 4 §6.2):

> *"Spark cannot be directed to treat pre-sorted input files as already sorted. Before
> a join, Spark dynamically repartitions and re-sorts the participating datasets...
> an additional full pass of the data that the sorted-file architecture never requires."*

The principle Paper 4 formalizes:

> **Any I/O pass that can be eliminated by architectural contract at write time is
> always cheaper than performing it dynamically at query time.**

**The Bob analog:** Every output file in this pipeline is pre-sorted on the same
14-field composite key (from `post.scala`). Bob should treat this as a *contract*:
first/last line answers range questions, grep on a sorted file is a near-binary-search,
two-file comparison is a two-pointer walk. Exploiting sort order is not an optimization
trick — it is the correct use of the architecture's most important invariant.

### 1.5 What Paper 2 proves about information destruction

Paper 2 §2.1 (Theorem 1, Data Processing Inequality):

> *The posting transformation π is strictly non-injective. No model f_θ(B), regardless
> of parameter count, can reconstruct the discarded mutual information I(E\*; Y | B).*

**The Bob analog:** When Bob reads a full file into context instead of using a targeted
query, it receives compressed, token-expensive data with no more analytical content than
a grep result would provide. Loading the whole file is the AI equivalent of training on
summary balances — the information is not richer, just more expensive to carry.

### 1.6 Why Scala is Bob's GenevaERS — and Python is not

Paper 4 §8 conclusion:

> *"The MacBook Pro results — 100 million records in approximately three minutes using
> JVM bytecode rather than compiled machine code — demonstrate that the **algorithms**
> themselves are the primary source of the GenevaERS advantage, not the z/OS hardware
> platform. The hardware amplifies the advantage; it does not create it."*

**Scala is Bob's GenevaERS** — not because of language speed, but because a Scala
pipeline script in this repo:
1. Runs in a subprocess (`execute_command`) — consumes **zero Bob context** for source data
2. Implements the CKB algorithm: pre-sorted inputs, single pass, O(1) working memory
3. Produces a small structured control report that Bob reads once
4. Processes 80M records in minutes on commodity hardware

### 1.7 The Streaming Contract vs. The In-Memory Trap (The Scale-Invariant Principle)

A subtle failure mode in modern computing (the "Spark Brain" trap) is assuming that because RAM
is abundant for a small dataset, building in-memory hash tables and performing in-memory array sorts
is acceptable.

**This violates the foundational GenevaERS contract.**

True GenevaERS architecture is **scale-invariant**:
- **Time Complexity:** Strictly $O(M)$ linear in the number of records.
- **Memory Footprint:** Strictly $O(1)$ constant buffer size (two record pointers in RAM), whether processing 10 thousand, 100 million, or 10 billion records.

Whenever an operation is written:
1. **Sorting must be External Merge-Sort:** If sorting is required upstream, it must operate via fixed-size memory runs spilled to disk and merged via streaming $k$-way merge — never in-memory `list.sort()`.
2. **Joins must be Streaming Two-Pointer Walks:** Matching transactions against master records or prior balances must stream both pre-sorted inputs concurrently using two pointers — never loading a 50-million-row dictionary into heap.
3. **The Linearity Invariant:** Scaling the dataset $10\times$ or $100\times$ must produce a strictly linear $10\times$ or $100\times$ increase in wall-clock time with **flat, constant memory consumption**.

The four GenevaERS core design rules (Paper 4 §7) apply directly to Bob's tool use:

| Paper 4 Design Rule | Bob application |
|---|---|
| **Compile rules into sets** — never interpret queries dynamically at runtime | One `execute_command` computes all needed statistics from a file in one pass — never re-open the same file for a different question |
| **Read base events once** — O(M) sequential scanning | `range` parameter on large files; scripts produce summaries; Bob reads summaries |
| **Buffer reference hierarchies** — pre-load into memory; resolve in CPU cache | `grep` answers lookup questions without loading the file; VendorMaster attributes are in memory during pipeline execution, not in Bob's context |
| **Pipe intermediate states** — spawn into memory; no scratch disk files | Pipeline outputs (`pipeline_results.log`, `cost_surface.csv`) are the pipe outputs; Bob reads them, does not re-derive |

---

## 2. The Four Tool Modes — When to Use Each

### Mode 1 — `grep` / `Select-String` (the index scan)

**Use when:** You need to locate something in a large file without reading the file.  
**GenevaERS analog:** Reading the sort key only; skipping to the relevant block.  
**Rule:** Always try `grep` before `read_file`. If the answer is a line number or a
matching substring, grep is sufficient and costs near-zero context.

```powershell
# Find header structure without reading the file body
Select-String -Path "data\ParallelProcessRunSept9\FY03q1exp.txt" -Pattern "." -List | Select-Object -First 1

# Find which output files contain a specific instrument
Select-String -Path "data\ParallelProcessRunSept9\B103bal.txtInstID0.txt" -Pattern "^0000000042,"
```

**Never do:** `read_file` on a large file "to see what's in it" when `grep` would answer
the structural question. This is the Paper 2 Theorem 1 failure mode applied to Bob:
loading compressed (token-expensive) data when a targeted query suffices.

---

### Mode 2 — `read_file` with `range` (the sorted-file seek)

**Use when:** You know the file and the approximate line range you need.  
**GenevaERS analog:** Starting the merge loop at the correct offset.  
**Rule:** Always supply a `range` parameter on files larger than a few hundred lines.
Never read more than 50–100 lines at a time without a specific reason for needing more.

```
# Good: read header + 5 rows to confirm schema
read_file path: "data/ParallelProcessRunSept9/B103bal.txtInstID0.txt" range: "1-6"

# Bad: read without range on a file of unknown or large size
read_file path: "data/ParallelProcessRunSept9/FY03q1exp.txt"   # ← 63 MB, never do this
```

**The sort-order principle:** Every output file in this pipeline is sorted on the same
composite 14-field key used by `post.scala`. This is the CKB invariant. Bob should
exploit it:
- **Key range:** First and last data lines answer "what period/instrument range does this
  file cover?" without reading the middle.
- **Instrument lookup:** A `grep` on a sorted file is effectively a binary search — fast
  and near-zero token cost.
- **Schema confirmation:** Line 1 (header) + lines 2–3 (first data) is always sufficient
  to confirm structure.

---

### Mode 3 — `execute_command` with a focused summary script (the CKB pass)

**Use when:** You need to compute something across all records — counts, sums, distinct
values, key ranges, samples.  
**GenevaERS analog:** The full CKB single pass — one sequential read, structured control
report, written to stdout.  
**Rule:** Write the script to produce a *small* structured summary and print it to stdout.
Bob reads the summary, not the source data. This is the O(M) vs O(V×M) distinction
from Paper 5 §6.2 applied to agentic work.

```powershell
# One pass: record count + first/last key + year field — everything needed to orient a balance file
$f = Get-Content "data\ParallelProcessRunSept9\B103bal.txtInstID0.txt"
"Records: $(($f | Measure-Object -Line).Lines - 1)"
"First key: $($f[1].Split(',')[0..1] -join ',')"
"Last key: $($f[-1].Split(',')[0..1] -join ',')"
"Year field (col 15): $($f[1].Split(',')[15])"
```

**Never do:**
- `Import-Csv` on a file with hundreds of thousands of rows — this is the 46 TB
  pre-materialization failure mode
- A pipeline that calls `Get-Content` followed by `ForEach-Object` that prints every row
- Multiple separate `execute_command` calls that each re-open the same large file
  (O(V × M) pattern)

**Do instead:** One `execute_command` that computes everything needed from a file in a
single pass — the Paper 5 §4.3 in-memory spawning model applied to analysis.

---

### Mode 4 — Write and run a local script (the compiled pipeline job)

**Use when:** The task requires transformation, enrichment, or join that would take
dozens of tool calls interactively.  
**GenevaERS analog:** Compiling and running the full pipeline; reading only the control
report output.  
**Rule:** Write the script to a file, run it once, read only its output summary. Do not
attempt to replicate in interactive tool calls what a 50-line script does in one pass.

The existing scripts follow this pattern exactly:
- `scripts/enrich_vendormaster.py` — enriches 380K vendor rows from 42K PO rows in
  one pass; Bob reads only the final line ("833 vendors enriched")
- `scripts/run_pipeline.sh` — runs the full Scala pipeline across
  all steps; Bob reads only `pipeline_results.log` and `cost_surface.csv`

**The agentic loop design in `PROJECT_BRIEF.md` is built on this principle:**
Bob edits two CSV files (`ViewSpec.csv`, `AllocationRules.csv`), runs one shell script
(`run_pipeline.sh`), reads two log files (`pipeline_results.log`, `cost_surface.csv`).
The entire 80M-record pipeline runs inside a single `execute_command` call. This is
the Paper 5 single-pass architecture — Bob is the operator, Scala is GenevaERS.

---

## 3. The Single-Pass Multi-Output Principle — and Why Intermediate Files Are the Wrong Answer

### 3.1 The principle

The most important GenevaERS architectural property is not speed. It is that **one pass
through the input produces all outputs simultaneously.** The O(M) vs O(V×M) result from
Paper 5 §6.2 is a statement about this: V views produced in the same pass as 1 view,
at no additional I/O cost.

This has a direct corollary that must be understood before writing any script in this repo:

> **An intermediate file that is just a format-translated version of an input is never
> correct.** It is a pre-pass. It converts O(M) into O(2M) — exactly the Spark shuffle
> tax applied to a situation that needed no shuffle at all.

The correct question before writing any transformation: *"Can this transformation be
done inline, in the same pass that produces the final output?"* If yes — and for column
remapping, type coercion, field extraction, and filter operations the answer is always yes
— then the intermediate file must not be written.

### 3.2 The session 15 failure mode — and the correct design

In Session 15, Bob wrote `convert_bal_to_ldgr.ps1` to translate B### balance files into
LDGR CSV format before running the aggregation engine. This was wrong on two levels:

1. **The B### files ARE the LDGR.** They are the output of the Sept 9 GenevaERS pipeline
   run — the same data, a different column layout. Writing LDGR2003.csv (88 MB) from
   B103bal files (85 MB) doubled the storage cost and added a full extra read before
   the aggregation pass could even begin.

2. **The column remap is a 19-integer index translation.** It belongs inside the read
   loop of the aggregation engine as three lines of inline code — not as a separate
   script, a separate pass, or a separate file.

The correct design (one pass, all outputs):

```
B###bal.txtInstID0..9 (10 shards, pre-sorted)
  --> [merge shards in sort key order]                    -- O(1) working memory
  --> [inline: positional B### cols -> named LDGR slots]  -- 19 array index assignments
  --> [lookup: ldgrIPID -> VendorMaster (in-memory hash)] -- O(1) per row
  --> [accumulate: all V view aggregations simultaneously] -- V hashtables, one pass
  --> [write: V small view CSV files]                     -- only final outputs
  --> [write: one row in pivot_results.csv]               -- control report
  --> [write: one row in cost_surface.csv]                -- experiment accumulator
```

Zero intermediate files. Single pass. The B### files are never re-read.

### 3.3 Generalizing the principle: what counts as an intermediate file

Any file written to disk during agentic work should pass this test:

| Question | If YES | If NO |
|---|---|---|
| Is it a *format translation* of an input already on disk? | Do not write it. Do it inline. | May be legitimate |
| Is it a *filtered subset* of an input that could be filtered in the pass? | Do not write it. Filter inline. | May be legitimate |
| Is it a *join result* that could be held in a hash table? | Do not write it. Join in memory. | May be legitimate |
| Is it a *final analytical output* (view CSV, pivot row, cost row)? | Write it. This is the purpose of the pass. | N/A |
| Is it a *control report* (counts, sums, key range)? | Write it (or print to stdout). | N/A |

The rule: **write only what cannot be reconstructed cheaply from pre-sorted source.
Everything else is overhead.**

### 3.4 The dry-run / test-before-write discipline

Disk writes are expensive. A write-heavy first run that produces incorrect output is
more expensive than two passes — one dry, one real — because the incorrect output
files must be deleted, and the error may not be obvious from the files themselves.

The correct agentic discipline for any new pass:

**Pass 1 — dry run:**
- Read all inputs
- Build all aggregations in memory
- Compute all control totals (record count, amount sum, VM hit rate, permutation costs)
- Print a structured control report to stdout
- Write **nothing** to disk

**Pass 2 — commit (only if pass 1 control report validates):**
- Same code path
- `--write` flag enables StreamWriter calls
- Outputs are identical to what pass 1 computed

This is the `--materialize` flag design that has been on the Phase 1 open items list
since Session 9. It is not just about the Scala pipeline — it is the correct discipline
for every script in this repo. The flag belongs in every new script from the start,
not added later as a feature.

**The cost comparison:**

| Approach | Passes | Disk writes | Error recovery |
|---|---|---|---|
| Write immediately, debug after | 1 + N retries | Incorrect files on disk | Delete and rerun N times |
| Dry run first, then commit | 2 max | Zero until validated | No cleanup needed |
| Old approach (convert + aggregate) | 3 (convert + dry + real) | 88 MB intermediate + outputs | Convert file must also be deleted |

The dry run is not extra work. It replaces error-recovery reruns. On 800K-row files
where each pass takes seconds, this discipline costs near zero.

---

## 4. The Record-Spawning Model for Generated Analysis

Paper 5 §§4.3–4.3b identifies five canonical **event-generator** processes that share
one property: *their input is a position or reference datum, not a source transaction,
and their output must be injected back into the ledger stream as first-class events.*
The five are: Allocations, FTP, Intercompany eliminations, Consolidation, and
Multi-currency revaluation.

These processes **generate new records from existing balances** without an inbound
transaction feed. The volume multiplier is predictable from reference data structure,
not from transaction flow.

**The Bob analog for analysis:** When Bob needs to compare, validate, or cross-reference
large files — e.g., do the T### transaction files sum to the B### balance files? — this
is structurally identical to an allocation or revaluation pass: input is the existing
position (the balance file), the operation derives new records (comparison results), and
the output is a small control report (differences, ratios, pass/fail counts).

The correct pattern is a two-file CKB script — exactly like `post.scala` itself:
1. Open both files as `BufferedIterator` on the same sort key
2. Walk in parallel: on equal key, compute comparison; on unequal key, emit mismatch
3. Print the control report (counts, first N mismatches, totals)
4. Bob reads only the control report

```powershell
# Two-file comparison pattern (PowerShell CKB analog)
$a = [System.IO.File]::ReadLines("B103bal.txtInstID0.txt") | Select-Object -Skip 1
$b = [System.IO.File]::ReadLines("B103bal.txtInstID1.txt") | Select-Object -Skip 1
$ia = $a.GetEnumerator(); $ib = $b.GetEnumerator()
$matches = 0; $mismatches = 0
while ($ia.MoveNext() -and $ib.MoveNext()) {
    $ka = $ia.Current.Split(',')[0]; $kb = $ib.Current.Split(',')[0]
    if ($ka -eq $kb) { $matches++ } else { $mismatches++; if ($mismatches -le 3) { "DIFF: $ka vs $kb" } }
}
"Matches: $matches  Mismatches: $mismatches"
```

---

## 4. Context Window as Working Memory — The Concrete Cost Table

Paper 5 §4.4 documents 46 TB pre-materialized vs 250 GB movement-based for the same
data — a 99.5% reduction by choosing the right architecture. The same ratio applies to
Bob's token consumption when the wrong access pattern is chosen:

| Access pattern | Tokens consumed | Paper 5 analog |
|---|---|---|
| `read_file` on full 63MB FY03 input | Context overflow, no result | 46 TB pre-materialized — impossible |
| `read_file range: "1-5"` | ~50 tokens | Stage 1 source record read |
| `grep` for pattern | ~10 tokens per match | Sort-key binary seek |
| `execute_command` computing a summary | ~100 tokens for output | Control report from single pass |
| Write script → run → read output | ~50 tokens for output | Pipeline run → read results log |
| `Import-Csv` on 380K-row VendorMaster | Memory explosion or truncation | Stage 3 ×10–20 volume explosion |

**The 10× efficiency claim** (Paper 2): GenevaERS vs. Python is not about CPU cycles —
it is about the number of times the data is read. Python reads files multiple times for
multiple questions. GenevaERS reads once and answers all questions simultaneously. Bob
should read files at most once per task, extracting all needed information in that pass.

---

## 5. Specific Patterns for This Repo

### 5.1 Inspecting a new data file

```
1. Check file size: (Get-Item "path").Length
2. < 10 KB  → read_file freely
3. 10KB–1MB → read_file range: "1-10" to confirm schema; grep for samples
4. > 1MB    → grep for header; execute_command for counts and key range
5. Never    → read_file with no range on any file > 50 KB
```

### 5.2 Checking consistency across 10 parallel InstID files

```powershell
# One pass, all 10 instances — record counts as the control report
0..9 | ForEach-Object {
    $f = "data\ParallelProcessRunSept9\B103bal.txtInstID$_.txt"
    "$f: $((Get-Content $f | Measure-Object -Line).Lines - 1) records"
}
```

### 5.3 Running the pipeline and reading results

**One `execute_command` per experiment point:**
```bash
bash scripts/run_pipeline.sh \
  --inPath data/ --outPath data/output/ \
  --years 03 --steps 2,3 --curve C1 --config "post-only"
```

Then read **only**:
```
read_file path: "data/output/pipeline_results.log"   # always small — control report
read_file path: "data/cost_surface.csv"              # grows by one row per run
```

Never read LDGR, SortedJE, or balance files to evaluate a run result. The pipeline
writes its own control totals. Trust the control totals. This is the production
discipline: operators read the control report, not the output files.

### 5.4 Understanding balance file content without reading it

```powershell
# Everything needed to orient a B### balance file — one command, five facts
$f = Get-Content "data\ParallelProcessRunSept9\B103bal.txtInstID0.txt"
$cols = $f[0].Split(',')
"Columns ($($cols.Count)): $($cols -join ' | ')"
"Records: $(($f | Measure-Object -Line).Lines - 1)"
"First instrument: $($f[1].Split(',')[0])"
"Last instrument: $($f[-1].Split(',')[0])"
"Year (col 15): $($f[1].Split(',')[15])"
```

### 5.5 What Scala is — and is not — in this context

Scala is **the production engine**. Bob is **the operator who reads control reports**.

Bob interacts with Scala through:
- `run_pipeline.sh` (black box: config files in → pipeline results out)
- `cost_surface.csv` (one row per run — the definitive record of empirical results)

Bob should **not**:
- Read Scala source to understand data transformations answerable by reading outputs
- Modify Scala source to add temporary debugging (use the pipeline's existing control report)
- Run `sbt` directly for ad-hoc queries (write a Python or PowerShell script instead)

When Bob *does* need to read Scala source — to understand a new FSP, verify an
algorithm, or add a process step — the correct sequence is:
1. `GetSymbolsOverview` — structure without body
2. `read_file range:` — tight range on the specific method needed
3. The header comment (lines 1–60) of every Scala module in this repo describes the
   full algorithm; read the header before reading the body

---

## 6. The Cow Path Warning

Paper 5 §1.0a introduces the cow path as an organizational phenomenon:

> *"A cow path is formed when a cow in a field centuries ago walked through the field...
> Reporting processes can be like this as well. Sometimes someone decides what we're
> going to report on, and that becomes the thing that can never be changed."*

**The AI agentic cow path** is: "I will use `read_file` to look at the data because that
is how I have always explored files." This becomes the default behavior that burns tokens
without producing better analysis than a grep would.

Paper 5 §1.0a also identifies the solution:
> *"the value of the architecture is not visible until the second report is produced —
> at which point the incremental cost approaches zero while the legacy architecture
> still incurs a full additional I/O pass."*

For Bob: the value of the CKB method is not visible on the first file inspection. It is
visible when the fifth file of the same type needs to be inspected and the grep pattern
already written handles it at near-zero cost — while the naive approach would require
five separate full-file reads.

**Build the grep pattern once. Reuse it. That is the single-pass compilation advantage
applied to agentic analysis.**

---

## 7. Summary: The CKB Rules for Bob

| Rule | Paper grounding | Bob application |
|---|---|---|
| **Punch card discipline** | Paper 4 §1.0a: minimum operations; everything else is overhead | Ask "what is the minimum I need?" before opening any file |
| **Short instruction path** | Paper 4 §1.0b: 2,533 instr/record vs 50,000–150,000 for SQL | `grep` = short path; `read_file` no-range = SQL full-table scan |
| **Read once** | Papers 4 §1.1, 5 §6.2: O(M) vs O(V×M) | `range` on large files; never re-read for a different question |
| **Sort order is a contract** | Paper 4 §6.2: sort at write time eliminates query-time re-sort | First/last line answers range questions; two-pointer walk for comparisons |
| **Never load what you don't use** | Paper 2 Theorem 1: loading compressed data burns tokens without adding information | `grep` before `read_file`; scripts before interactive reads |
| **All outputs in one pass** | Paper 5 §6.2: O(M) single pass produces all V views simultaneously | No intermediate files; column remaps and filters are inline, not pre-passes |
| **No intermediate files** | Session 15: convert_bal_to_ldgr.ps1 doubled I/O for zero gain | If a file is just a format translation of an input, do it inline |
| **Dry run before write** | Session 15 learning: disk writes are expensive; incorrect output costs more than two clean passes | Every new script gets a --dry-run flag; validate control totals before committing |
| **Aggregate in the pass** | Paper 4 §4.3: in-memory piping; zero scratch files | One `execute_command` computes all statistics in one pass |
| **Trust the control report** | Paper 4 §5: operators read control reports, not output files | Read `pipeline_results.log`, not LDGR or SortedJE |
| **Scala is the engine; Bob is the operator** | Paper 4 §8: algorithms, not hardware, are the advantage | Bob edits config, runs script, reads log |
| **Build patterns once, reuse them** | Paper 5 §1.0a: incremental cost -> zero on the second report | grep pattern written once applies to all 2,376 files of the same type |

The ~100× improvement of 1996 (Paper 4 §2.1), the 10× GenevaERS advantage over Python
(Paper 2), and the 10.3× CPU advantage over SQL joins (Paper 4 §5.2) all have the same
root cause: **method, not hardware**. The same method applied to Bob's tool use
produces the same ratio of efficiency over naive agentic file exploration.

---

*Source papers: Paper 4 (GenevaERS Architecture), Paper 2 (Pre-Hierarchy Event Repositories),*
*Paper 5 (Enterprise Financial Transformation Engines) — from the monograph*
*"The Architecture of Economic State" (Twitchell). Link will be added upon publication.*
*Companion documents: `PROJECT_BRIEF.md` (experiment design),
`docs/DATA_ENHANCEMENT_PLAN.md` (data enrichment roadmap),
`scripts/run_pipeline.sh` (agentic interface).*
