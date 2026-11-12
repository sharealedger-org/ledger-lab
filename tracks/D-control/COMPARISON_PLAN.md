# Track D comparison plan (draft for the owner)

Drafted 2026-11-12 by Bob. Not adopted. The control's official result is in (digest at `3c50c19`, see [`STATUS.md`](STATUS.md)); the companion baseline is tag `companion-measured-baseline-20261111`. This plan decides what to compare, and puts a second test ahead of any exposure of the companion to Codex.

## 0. What the paper premise needs

Paper 1 says: **anticipating all permutations is costly.** A GL must build the balances it expects to be asked for; an instrument ledger keeps the events and answers a new question by a pass. The runs so far measure the cost of *building*. Neither system has been asked a question it did not anticipate. The control's 6,660 requests sample *existing* cells of views it had already built (`query_metadata.json`: "uniform reservoir over existing distinct cells"), and the companion has no query test at all. So neither result tests the premise. Part B does.

## 1. Facts fixed so far

- **Hardware:** both runs on Intel Macs, 16 GB, four logical threads (owner, 2026-11-12). The control host has two physical cores. The companion runs do not record the CPU model.
- **Population:** control 130,861,904 events, three streams (expenditure, revenue, PO). Companion 224,072,410 SJE rows, about 112.0M events if two rows per event (inferred), expenditure and PO only. Control expenditure plus PO is 113,111,795, about 1% more (inferred, not reconciled).
- **CPU, seconds (user + sys), from the evidence:**

| | Wall s | CPU s | Average cores busy | Peak process RSS |
|---|---:|---:|---:|---:|
| Companion baseline `20261110_210721` (serial) | 11,695 | 10,670 | 0.91 | 1.35 GB |
| Companion run 3 (two chains) | 8,147 | 11,179 | 1.37 | 1.35 GB |
| Companion run 4 (two chains) | 8,544 | 11,207 | 1.31 | 1.36 GB |
| Control, build + export + queries | 26,611 | 17,362 | 0.65 | 0.16 GB |

The control's CPU: build 13,404 (user 9,928, sys 3,476), export 3,842, query driver 116. **Reading:** the control used less memory (about 8 times) and kept the machine less busy, but it used more total CPU seconds (about 1.6 times the companion's) and more wall time (about 2.3 times the serial baseline, 3.3 times run 3). System time is 30% of its CPU, i.e. I/O. Scope differs: the control also processes revenue and writes six full reports; the companion's CPU leaves out the Bash control passes (about 54% of chain wall in run 3, outside every step metric) and has no query phase. So the table does not yet show who is cheaper; it shows which resource each engine spends. "Efficiency" needs a rate card (below).

## 2. Part A: compare what has been measured

1. **Comparison contract.** Fix: the shared population (expenditure plus PO, FY2003 to FY2016, or the full control population with the companion's revenue added later); what is timed (control from ingestion to export; companion from raw files to the four views plus control passes); what must be equal (event counts after quarantine, debit equals credit, vendor-year or commitment totals); hardware (same class, stated).
2. **Reconcile** the items in the baseline record section 3: PO quarantine sets (about 32,000 rows against 8,064 total candidates), year populations (FY2009 PO absent, FY2002), revenue, 2 SJE rows per event.
3. **Price by resource, not elapsed seconds.** One written rate card: core-hours, GB-hours of memory, GB-months of storage, GB of I/O. Apply it to both. The COST_GRAPH_DESIGN unit rates are a starting point; they are assumptions. The control's profile (one core, 160 MB, 183.5 GB, 7.4 h) may well price lower than the companion's (four cores, 1.4 GB, about 116 GB, 2.3 h); the contract decides, not intuition.
4. **Falsify the earlier predictions** in `TRACK_D_NOTES.md` section 2 (ingestion penalty and bowl shape) against the measured values, plainly stated.
5. Output: one page, scope-labelled, with every difference named.

## 3. Part B: the novel-permutation challenge (the test of the premise)

**Question.** When someone asks for a grouping that neither system built in advance, what does each system pay to answer?

**Design (to be pre-registered before either system sees it):**
- **Question set.** 10 to 20 requests, each a grouping of attributes both systems hold, chosen to span size: 2, 3, 4, 5 and 6 attributes (for example agency x month; vendor x commodity class x month; agency x vendor x object x fund x month x commodity class). Include some the control's indexes happen to favour and some they do not. Include questions whose answer needs the order of events (vendor first appearance, order-to-payment gaps), which a posted GL cannot answer at all.
- **Authored blind.** Written by a party who does not build either answer (the owner, or Origin with the companion's names withheld), and sealed before the runs. Each question carries an expected-answer test: a reference computed once from the raw files by a plain method, so correctness is checked, not assumed.
- **Systems.** The control as run (its 183 GB database is retained on the Intel host) and the companion as measured (outputs were deleted; regenerating them is a roughly 2.5 hour chain run). Both frozen. A conventional GL answer is recorded as "not answerable from the GL" or as the cost of the permutation table it would have needed (companion's `PERM_TBL` capacity).
- **Metrics per question and per system:** time to first correct answer, CPU seconds, peak RSS, extra bytes written, whether a rebuild or new index was needed, and correct or not. Plot cost against the number of attributes in the grouping.
- **Output:** measured points for the Mix graph, where today there is only a model run. This is the evidence for the central claim.
- **Python:** the reference-answer and scoring scripts are post-engine analysis and need your approval before I write any; the companion's side runs in Scala.

## 4. Order and the exposure decision

1. Part A first, from existing evidence, no reruns.
2. Part B next, with the question set written and sealed first.
3. **Then decide exposure** with both results in hand. Reasoning: the challenge is a blind test. If Codex sees the companion before it runs, it can tune to the question types, and the premise test loses its meaning. After Part B, an informed phase makes sense: Codex learns from the companion, adopts what helps, and its changes are logged against the frozen result.
4. The owner may reverse this order. Cost of exposing first: the challenge is no longer blind for the control. Cost of waiting: Codex's progress waits for the challenge.

## 5. Decisions for the owner

1. Approve Part B as the next phase before exposing Codex.
2. Population for the first comparison: the shared expenditure and PO population (scope-labelled), or wait for the companion's revenue run.
3. Who writes and seals the question set; whether Origin may write it.
4. The rate card for pricing: use the Mix-graph assumptions, or set new ones.
5. Approval for post-engine scripts for the reference answers and scoring, or an `awk` route.
