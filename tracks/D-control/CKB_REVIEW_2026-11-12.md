# CKB execution-model review for Phase 2

2026-11-12 22:01:26 AZT (UTC−07:00). Author: CKB-side Codex, Verification Lead. Source review at working-tree HEAD `08bedfd`; concurrent Track B work is outside scope. No engine edits, compilation, fixture execution, or corpus measurements. This is a focused contract/source evaluation, not certification of the full pipeline.

## Assessment

The [execution model](../../docs/CKB_LAYER_EXECUTION_MODEL.md) makes the potential economy concrete: establish an order once, bring the required state and reference data together for one instrument, apply a declared dependency chain of financial transformations, and let multiple consumers use the resulting records before terminal publication. Consistent SJE structures make generated results usable by subsequent financial processes. Additional capabilities still cost computation, state, output, and sometimes sorting; they need not each pay for a new full-history scan.

The conditions matter as much as the opportunity: compatible keys, available side inputs, bounded state, acyclic dependencies, and reproducible temporal rules. Acyclicity alone does not bound fan-out or the largest instrument. Generated records targeting a different instrument may cross an already-completed key and need another declared boundary. These are constraints to test, not reasons to assume the model fails.

The existing source provides useful pieces, but the inspected legacy reclass path does not instantiate the delivery-group model. The measured view path is a separate ordered consumer. Phase 2 should distinguish extending those pieces from demonstrating the broader architecture.

## Falsifiable claim and cheapest check

**Claim tested:** Step 9 implements reclass through the execution model's in-buffer generated-record chain, with durable ledger output deferred to the terminal stage.

**Cheapest falsifier:** trace `run_pipeline.sh` → `LedgerApp` step 9 → `arrangementReclass` writers and downstream consumer before running anything. Source contradicts this placement claim: it rewrites ledger files, materializes generated SJEs, externally sorts them, and invokes `post`. The output is consumed; it is not an orphan. Same-JVM invocation does not establish the required CKB placement. This is consistent with the audit register's existing `unknown` generalized reclass status, not a newly failed measured workload.

## Actual paths inspected

| Path | Producer, boundary, consumer | Evidence and implication |
|---|---|---|
| Per-year baseline | `run_full_corpus.sh` invokes preparation `ttl,2p,2m`, then `3,20,21`; `run_pipeline.sh` launches selected Scala steps. | Distinct launches and retained sorted partitions. Do not describe the whole baseline as one fused pass. |
| Cross-year views | `run_full_corpus.sh:154` launches `5v` after all preparation markers; posting may still run. `LedgerApp.scala:239` calls `CKBViews`, which opens `SortedJE` inputs and k-way merges them. | Reuses prepared order, but rereads the partitions in its own pass; it does not consume the poster's in-flight updated ledger. This separate-pass choice is explicitly allowed by the model. |
| Reclass input | `LedgerApp.scala:228` calls `arrangementReclass(outPath)`. `arrangementReclass.scala:123` reads `VendorUpdate.csv` into an instrument-keyed map; `:173` rewrites VendorMaster; `:230` scans LDGR files. | Existing ledger is canonical input, but updates are not a delivery-group scheduler. Multiple update rows for one ID overwrite earlier map entries. |
| Reclass output/application | `arrangementReclass.scala:215` opens SJE outputs; `:328` writes old-zero/new-amount ledger rows; `:348` replaces the ledger; `:365` sorts generated rows; `:373` calls `post` for `_RECLASS` files. | Materialization and sorting are real boundaries. The ledger is changed before generated entries are applied. Inspect exact balance results before reuse. |

Paths in this table are under `scripts/` or `02-foundation/va_pipeline/src/main/scala/org/universalledger/foundation/va/ledger/` as applicable.

## Findings relevant to Phase 2

1. **Reclass needs a correctness gate before extension.** Source first writes the changed balance and then posts the same transfer amounts against that changed ledger. The poster adds SJE amounts to matching balances (`post.scala:152,275`). For a simple already-sorted old=100/new=0 example, this suggests old=−100/new=200 instead of old=0/new=100. This is a source-derived prediction, not an executed result; other key/order effects also require checking. The smallest runtime falsifier is one instrument, one affected balance, and one update, with explicit per-account expected balances—not just a zero net sum.

2. **Current reclass does not establish temporal/rule lineage semantics.** The effective date is parsed and written to VendorMaster, but the SJE builder receives no update/rule argument, uses wall-clock dates (`:414–415`), and leaves rule fields blank (`:420–421`). It assigns different journal IDs to the two legs (`:308,322,394`). A balanced file therefore does not establish balanced journal groups. The scan has no effective-date filter on ledger periods. These are directly observed source facts; prospective versus retrospective correctness remains untested. A late-change test cannot be accepted from this implementation by name alone.

3. **The strict one-instrument memory description is not true of all `5v` state.** `CKBViews.scala:301` creates a `completed` vendor HashSet; `:320` adds each closed vendor and `:344` checks reappearance. It is never cleared during the pass, so it grows with distinct vendors. Vendor-year accumulators are cleared and View D has a capacity/flush/reduce path; those are real bounded-processing mechanisms. Maps A/C also span keys under a declared small-domain assumption. No memory impact was measured here. Next falsifier: increase distinct vendor count at fixed per-vendor group size and inspect state/high-water memory. Do not extrapolate a whole-process bound from the local accumulator alone.

4. **The delivery-group contract needs several explicit decisions.** The daily-movement section says to consume events incrementally, while the delivery-group section says all source and generated records remain available at the break. State whether that means retained rows, aggregates, or a spill-backed interface for each profile. Specify a trigger for Group-0-only instruments with changed references but no Group 1 activity. Specify what happens to generated records for another instrument or an earlier sort position. Also distinguish an engine exit invoked by a view from an aggregation-only perspective; the document uses “view” for both contexts. These are contract questions, not verified runtime defects.

5. **Some prose mixes target and implementation scopes.** The cross-period section's “final period's pass” is not the same as the actual overlapping `5v` process. The delivery-group table's paper numbers differ from the supplied monograph's paper titles; clarify whether it names a separate experimental sequence. The large-reference lookup deviation already has GERS-002, and the View D overflow work already has GERS-003; do not file those again or infer target GenevaERS behavior from Scala source alone. GERS-003 contains both a later implementation status and older “does not implement” prose. This review does not resolve target-runtime capability or retroactively reject measured Scala runs.

## Recommended Phase 2 slice

Start with a small, independently calculable reclass chain under an explicit contract: opening state and a reference change → balanced reclass SJEs → one dependent consumer → terminal updated state plus source-only and generated-event views. Include an instrument with no new source activity. Then introduce a late event and repeat from the same inputs. Require identical committed results on retry, correct per-account and per-group controls, and preserved rule/effective-date identity.

Test skew/fan-out and one cross-instrument dependency separately. Record whether each needs spill or a new pass. Preserve the existing source-only AI outputs while adding a separately identified generated-event feature view; otherwise reclass activity could be mistaken for new economic activity.

This is a proposal, not a new engine implementation or Q2 release. Before implementing, resolve the contract questions and any genuine target-runtime capability gap, run the unchanged fixture, and follow the architecture gates. The broad financial comparison remains subject to population/timing reconciliation.
