# Connected CKB rerun: revenue and vendor attributes

Owner direction, 2026-11-13: proceed with a richer CKB corpus; matching control's scope is not a constraint. Run on the Intel machine. Its generated control outputs may be removed to recover space, after identifying exact paths and retaining code and compact evidence. No deletion has occurred in this session.

## Workload declaration

Use the existing Scala TTL/ARE expenditure and revenue adapters, PO transformation, sorted SJE merge, posting, GL/permutation outputs and cross-year views. Bash only selects and sequences those existing compiled steps. Preserve the prior expense-only default; explicitly request both source families for the new run.

Inputs: archived APA monthly expenditure/revenue files and eVA PO files, reference tables, and the actual runtime vendor master. TTL emits the existing 39-column SJE contract, consumed by `2m`, then `3`, `20`, `21`, and `5v` through `SortedJEInputs`. SJE files remain externally sorted/materialized; this change does not fuse physical passes. The current full-corpus launcher has prep, posting and views process groups; these are not three measurements of a cost curve.

Canonical outputs are the journal/ledger; GL, permutation and views are analytical outputs. Existing rule identifiers, source identity, fiscal periods and balancing remain required. Revenue is agency/fund/source-grain; do not fabricate vendor attribution. The current revenue adapter can use the unknown-instrument sentinel: explicitly assess its downstream grouping before claiming an instrument-grain revenue result. Receipt/expenditure differences are not automatically GAAP net income.

Controls: unchanged TTL fixture; combined expenditure/revenue fixture; source reconciliation by family/year; two SJE lines per accepted source record; balanced journal groups; revenue partitions consumed exactly once downstream; separate revenue and expenditure account totals; unchanged expense-only default. Preserve per-process RESOURCE records, sort boundaries, memory high-water marks and corpus wall clock. This is a new workload, not a replicate of the old expense/PO corpus.

Vendor names: the existing master schema already includes `instHolderName`, and expenditure source includes `VENDOR_NAME`. Inspect the Intel runtime master and its identifier coverage before selecting it. Retain names as linked attributes, not identity keys. A master being available to TTL does not prove names survive to AI inputs; verify the actual attribute-output/consumer path. Do not replace an established identity mapping with the small repository sample.

## Launch prerequisites

- Intel raw-data root and output destination verified; checksummed source coverage and actual master selected.
- Space recovered from explicitly identified generated control outputs. Prior full CKB output was about 110 GB; this Mac has only 15 GiB free and no recorded raw-data root, so it is not the full-run host.
- Compile and unchanged fixture checks pass; combined-family downstream smoke passes before the full run.
- Revenue source/accounting and vendor-name lineage checks pass; record unresolved names, agency references and missing attribution.
- Freeze code before launching parallel lanes. Save logs and evidence under a new run identity; preserve previous Gold evidence.

Status: preparation only. No full run launched, no control outputs removed, no claim that vendor names reach model inputs yet.

## Revenue adapter falsifier

The combined fixture failed before any corpus launch: a 240.00 receipt produced zero revenue amount. `buildJEPairsAPA` passed normalized `[agency,fund,source,amount,date]` unchanged to `buildRevenuePairs`' RC parser, which expects `[agency,amount,fund,source,date]`. The prior revenue fixture tested row count and zero-sum balance, allowing the incorrect amount to pass. Repair scope is the 01 TTL adapter ordering only; preserve the existing RC consumer and SJE schema. Require amount 240.00, fund 1552, account REV801 and balanced offset to survive posting; rerun expense fixtures unchanged.

## Intel handoff and unattended execution

Owner will start the work on Intel after this session commits and pushes. Current changes expose `--ttlType B` and `--vendorMaster FILE` through both launchers, and `--outRoot DIR` through the full-corpus launcher. Defaults retain expenditure-only behavior. `scripts/run_corpus_queue.sh` accepts an explicit tab-separated plan of run name, granularity (`monthly` or `annual`) and source selection (`E`, `R`, `B`). It gives each run separate output/log directories, checks free space before each run, freezes a supplied master copy, checks the code revision, validates existing engine verdicts/artifacts, and compares compact output hashes for identical configurations. It stops at the first failure. No automatic deletion, cleanup or retry; create `QUEUE_ROOT/STOP` to stop before the next run.

The queue requires a new queue directory and committed processing code. Its default minimum free space is 140 GiB before each run (a conservative initial allowance, not a proven peak bound for revenue). Retaining multiple runs requires that much remaining headroom each time. A full run previously used about 110 GB without revenue. A paused/failed queue is not automatically resumed; inspect it and use a new plan/directory for remaining work. Keep sources immutable and do not edit code while it runs.

Validation reads existing engine reconciliation statuses (`sje_balance`, `ledger_balance`, and all other emitted verdicts), requires complete process evidence with time/memory fields, checks required output files and revenue partition presence, then hashes final ledger/GL/permutation/views. It does not independently prove source accounting, name linkage, or the source completeness. Those acceptance checks must precede the unattended corpus launch.

Runtime paths must have no whitespace under the legacy sbt argument contract. Stage `aRef*.txt` and `VAAccountingRules.csv` beside `VARawFiles`, so reference lookup does not fall back to the space-containing Dropbox checkout. The first smoke attempt exposed that existing path limitation and the need for an explicit PO fixture; neither failure is evidence about the real corpus.

Example invocation **after** Intel source/master checks and selection of the run plan:

```bash
bash scripts/run_corpus_queue.sh \
  --root "$HOME/workspace/va_fresh_root" \
  --queueRoot "$HOME/workspace/ckb_expanded_queue_20261113" \
  --plan /path/to/approved-plan.tsv \
  --vendorMaster /path/to/verified/VendorMaster.csv
```

The user asked whether three runs could yield a new cost curve. Proposed, **not yet selected or implemented as profiles**: Events; Monthly balances; Monthly plus selected views, on identical expanded source data/attributes. This queue currently runs the existing complete topology; merely putting three lines in its plan does not implement those retention profiles. A maintenance curve requires genuinely different persisted products; an equivalent-output total-cost curve additionally requires the same reports from each profile, including deferred compute. `5v` currently reads SJEs directly, not monthly ledger output. Preserve that distinction when defining the measured topology. Three points do not establish a global minimum. Await the profile decision before treating the queue as a cost-curve experiment.

Owner refinement: the last profile could expand 5v with additional financial and AIM-ready outputs from canonical SJEs in its existing ordered pass, illustrating incremental output capability and preparing Phase 2 adaptability. No extra scan does not establish no extra computation, memory or output cost. Define those views before implementation; record actual buffers/sort needs, controls and added bytes. Freeze this expanded foundation before developing the subsequent Phase 2 questions. No additional views were implemented in this preparation session.

Verification: unchanged TTL suite 9/9; combined TTL/posting and one-read suites 13/13. Actual full launcher with `--outRoot`, `--ttlType B`, explicit master, one synthetic year and empty PO fixture completed through 5v: `data/test_evidence/corpus_runs/run_20261113_141215/`. Receipt 240.00 and expenditure 45.32 both reach GL, with balanced offsets. Metadata validator passes; an injected FAIL verdict is rejected. The unattended queue then completed two identical tiny full runs automatically, validated both and matched all final-product hashes (2026-11-13 14:20:49 AZT); evidence `data/test_evidence/corpus_queue_20261113/`. All of this is fixture evidence, not corpus-scale measurement.

## Latest owner clarification: produce both inputs to the combined AIM3 test

The owner meant the existing AIM3 finding that balances plus movement features had the highest score, **not** a new receipts-versus-payments forecasting task. The latter was an assistant suggestion and is not the selected experiment. Required next implementation:

| Feed | Producer boundary | Existing matched feature definition |
|---|---|---|
| Movement features | Canonical-SJE pass / existing 5v ordered stream | PO count, agency count, distinct NIGP classes, committed amount |
| Balance features | Final reporting pass over posted ledger state | Debit amount, offset/credit amount, ledger row count, distinct accounts, distinct periods |

Both feeds need explicit `(vendor_id, fiscal_year)` keys, source/rule identity and the same observation cutoff. Their financial aggregation belongs in Scala; model preparation/scoring consumes the bounded output tables. Fit movement-only, balance-only and combined models on **identical eligible rows, labels and splits**, with explicit unmatched-feed counts. Do not substitute a balance reconstructed from SJEs for the final reporting input and claim both physical producer paths were tested.

Prior provisional evidence: balance C=0.7683, combined C=0.7761; commitment-only C=0.7527 was a separate fit, so do not imply all three were proven on one identical matched panel. The balance/combined fiscal parser incorrectly shifts `00YY/MM` payment fiscal labels after June. Fix and verify this before new score comparisons. Existing movement features use sums for counts/amount and maxima of monthly agency/class counts, not annual distinct unions; preserve the named replication definition or explicitly version any improvement. The balance input includes commitment accounts as well as expenditure accounts; it is not a payment-only GL baseline. Keep unattributed revenue separate from vendor inference rather than inventing a link.

This two-feed feature production is **not yet implemented** by the queue/TTL changes in this handoff. Complete its architecture declaration, producers/consumers, fiscal and reconciliation tests, then freeze code/configuration before unattended runs. The queue already supports E/B source selections; another B plan row currently repeats the same complete topology, not new AIM feature output. Measure each feed's marginal pass, buffering/sort, computation and output costs. Freeze the resulting foundation before later Phase 2 questions.

Storage follow-up: the current queue retains independent full outputs/SJEs for every run. For a reporting-only final stage, reuse explicitly named immutable SJE/ledger inputs rather than copying them, with shared preparation cost reported separately. That reporting-only profile and its queue selection are still to implement; do not assume the present full-run queue has SJE reuse or space reclamation.
