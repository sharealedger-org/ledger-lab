# Q2 exploration process

## Explore before freezing the test

Phase 2 explores whether an LLM can learn a financial language from native tuples and use it for interactive, multidimensional inquiry. The two experimental dimensions are how the model learns and which financial substrate it sees. Candidate substrates are source events, posted GL, and Instrument Ledger evidence. Allocation and reclass are outside this proposed Phase 2 scope.

Use the accepted reports to try an interaction, inspect the actuals, and change the question when the result warrants it. Record the source, the attempted interaction, failures, and what changed in the design. Use a query-generating assistant as an interactive baseline and a route to verified actuals. The same interface can later query the trained models. Cases inspected during exploration are development examples.

## 2.1 Financial tuple and metric learning

The local explorer is a post-report development consumer. It reads the accepted
relationship CSV at its existing vendor, year, event, currency, agency, fund,
commodity, and contract grain using the pipeline's CSV library. A session holds
at most 20,000 cells for one vendor and fiscal year. It filters and ranks existing
cells and displays paired encodings. The scan has no key-order dependency or
intermediate materialization. It has no posting, CKB, new financial aggregation,
or cost-curve measurement role. Check displayed values against the acceptance
manifest's report and label results at cell grain.

Start with one small Instrument Ledger-derived slice and encode the same evidence three ways.

1. Verbalized records with numbers as text.
2. Financial tuples with numbers as text.
3. The same tuples with typed numeric input and a numerical prediction head alongside token prediction.

Encode measure identity, unit or currency, grain, period, and availability. Keep identifiers categorical, even when written as digits. Preserve sign and distinguish observed zero, unavailable evidence, and values deliberately hidden for a training task. Keep exact ledger values in the engine-produced reference outputs.

Try predicting a field and its associated measure from a partial tuple. Reverse the direction by supplying a measure or range and asking for matching fields. Rotate the starting fields and inspect what happens when amount is replaced by count. Compare categorical completion and numerical error separately. Use the same underlying training evidence and comparable model conditions when the experiment is measured.

## What could follow

If both directions work, test new combinations of familiar dimensions and measures. If only one works, probe that direction explicitly and inspect sensitivity to zeros, sign, scale, missingness, and ambiguity. When the interaction is useful, compare achievable inquiry across financial substrates and replay whole investigations against the trained variants and the query baseline.

Freeze definitions, comparison conditions, and fresh evaluation cases once exploratory results identify what is worth measuring.

## First exploration, 2026-11-18

**Question.** Holding vendor, period, event, currency, fund, commodity, and contract fixed, does changing from signed amount to row count change the leading agency? Does supplying a different amount range change which agencies qualify?

**Evidence.** Read the corrected `report/relationships.csv` from `session-run.tmp/phase2_reporting_three_way_v1`. Its SHA-256 matched the accepted [report manifest](../../../data/test_evidence/reporting_three_way_20261117/completion/report_hashes.csv). The Scala report groups direct movement records. Its count is the number of contributing rows. Signed amount and sum of absolute input amounts are separate measures.

The first candidate used the known PO placeholder `999999999999`. The agency drilldown is meaningful for that group, but treating it as a supplier profile would misstate the evidence. I repeated the search with the report's attributed-vendor convention and checked the complete selected slice against the accepted report. The case was selected for a ranking change, so it supplies an example rather than a frequency estimate.

| FY2044 USD commitment, fund 0000, commodity 45000, contract 0 | Signed amount | Contributing rows |
|---|---:|---:|
| College of William and Mary | $3,631.42 | 5 |
| New River Community College | $170.17 | 3 |
| Virginia Tech | $1,504.41 | 16 |

Vendor key `v000624713229648432276230057853276173`. William and Mary leads by amount. Virginia Tech leads by count. For half-open amount ranges, $0–$500 returns New River, $500–$2,000 returns Virginia Tech, $2,000–$4,000 returns William and Mary, and $4,000–$5,000 returns an empty result within this slice.

The inspection also found a represented zero commitment with one contributing row and a signed expense-payment amount of −$1,073.70 with an absolute amount of $1,073.70. The negative aggregate alone does not establish an event's reversal lineage.

**What changed for 2.1.** Include a task that changes the ranking measure while keeping the context fixed. Include reverse queries with several different ranges and an explicit empty-result case. Carry identity attribution and contract status into the tuple. Preserve reported zero, an empty result within the selected scope, and unknown evidence as distinct conditions.

One development example can be rendered as “For the selected vendor, Virginia Tech had $1,504.41 in USD commitments in FY2044, from 16 direct movement rows, in fund 0000 and commodity 45000, with contract unattributed.” The equivalent tuple carries the same dimensions, contract status, and the two typed measures. The exact report queries demonstrated the interaction and supplied reference answers. No model was trained or scored.

## Run the interactive explorer

For a fresh checkout, the versioned [starter data](../../../data/q2-development/README.md)
and `bash scripts/start_q2_explorer.sh` restore the report and launch the standalone
Scala project. The historical machine-local paths below describe the first replay.

Run this from the repository root. It opens the full accepted local report and retains only the selected vendor and fiscal year. Filters are exact and intersect. The explorer displays report cells at their stored grain, so select the event, currency, fund, commodity, and contract before interpreting a rank as a comparison among agencies.

```bash
report="$PWD/session-run.tmp/phase2_reporting_three_way_v1/report/relationships.csv"
vendor=v000624713229648432276230057853276173
main=org.universalledger.foundation.va.ledger.FinancialTupleExplorer
sbt "va_pipeline/runMain $main $report $vendor 2044"
```

The workspace also has a fresh build from the cached Scala compiler and dependencies. Launch it directly when sbt cannot write to its boot cache.

```bash
java -Xmx512m -cp "$(cat session-run.tmp/q2-explorer/fresh-classpath.txt)" \
  "$main" "$report" "$vendor" 2044
```

Try these commands in the same session.

```text
where event COMMITMENT
where currency USD
where fund 0000
where commodity 45000
where contract_id 0
values agency
rank amount
rank rows
range amount 500 2000
example 1
mask 1 agency
mask 1 amount
range amount 4000 5000
reset
export session-run.tmp/q2-explorer/my-development-cases.csv
quit
```

The export creates a new CSV from the active dimension filters. It emits one masked example per cell and target, with the same evidence in prose, text tuple, and numeric-marker tuple. Pass `typed_tuple` and its ordered `numeric_payload` together. Use `cell_id` to group renderings and `reference_answer` for supervision or scoring. Neither column belongs in the model prompt. Exclude absolute amount from all three paired inputs because it can reveal the hidden signed amount. Retain it in the explorer as a separate measure.

## Interactive replay and implications for 2.1

- The FY2044 replay reproduced the previous amount and count rankings and exercised range selection, an empty range, and numeric and categorical masks. The all-cell export contains 240 paired examples across 80 cells, with two observed zero amounts. The CSV check verified one example per cell and target, a mask in each rendering, and a numeric payload entry per numeric marker.
- In a separate attributed FY2041 expense-payment slice, the explorer preserved `-1073.7` as a numeric value and agency code `32` as a category. When the amount was masked, the numeric payload contained only the row count.
- Replaying the FY2044 commitment filters in FY2043 returned just William and Mary at $2,300 across four rows. The $500–$2,000 band was empty in FY2043 and returned Virginia Tech in FY2044. A range-conditioned question needs a period even when its other dimensions stay fixed.
- These examples reveal the difference between completing a field from a known exact measure and returning a set of plausible fields from a measure range. The current export supplies the first task. The exact range replay supplies reference answers for the second task. A future training batch should represent range endpoints, wildcard fields, and empty results explicitly if we want to test learned range-conditioned suggestions.
- Keep related cases together when preparing fresh test data. All three target masks for a cell share a cell ID, and nearby years can repeat the same vendor, agency, and commodity context. A row-random split could reward recall of that context. Current exports are development data.

Local transcripts, example CSV, and verification logs are under `session-run.tmp/q2-explorer/`. The standard sbt launch could not write its boot lock in the restricted session. A fresh cached Scala compilation of the pipeline and test sources succeeded, and ScalaTest reported 165 passed. The connected identity fixture passed separately using its retained output. The explorer replay used the accepted reports. This run supplies no trained-model result.

## Session log: 2026-11-18, language interface and learned continuation

The ARM session restored the owner-provided OneDrive relationship report after
verifying its archive checksum. Standalone Scala compilation succeeded and the
documented FY2044 vendor loaded 80 cells. The explorer was opened in Terminal.
`rank amount` sorts existing cells by signed amount; it does not aggregate agency
totals. Payroll exploration was discussed, but payroll categories were not mapped
and no payroll result was produced. Employee counts cannot be inferred from
contributing-row counts.

The owner accepted an LLM conversational interface to this exact explorer and
preferred the term **explorations**, rather than queries suggesting generated SQL.
The assistant initially proposed translating language into explicit financial
dimensions, filters and requested measures. The owner corrected that narrowing:
requiring users to specify categories adds little new functionality. The intended
experiment should expose prediction of the next response with an associated
measure, allowing relevant fields or learned concepts to emerge from context.

Working interpretation: financial tuples are a learned representation, not a
form the user must complete. An open request such as “Where should we look next?”
may elicit a proposed financial relationship and numerical prediction with stated
uncertainty. This remains a hypothesis. The current Scala explorer provides exact
reference cells; no trained financial model, conversational continuation score,
or numerical-head result has been demonstrated. Scala mainline financial
processing remains unchanged; a learned consumer does not replace its accounting.

### Proposed next step: a small continuation pilot

1. Author 6–10 short development conversations using supported report cells:
   start with a broad interest, ask an open follow-up, then change a measure,
   period, or direction of inquiry. Include an empty-result case and a request
   beyond available evidence. Do not require every dimension in the user's turn.
2. For each context, capture a candidate next financial response together with
   the relationship it proposes, associated signed measure and unit, period,
   grain, and uncertainty or abstention. Preserve the original language; an
   intermediate representation must not silently close an open question.
3. Use the conversational assistant plus exact Scala access as the first
   baseline. Label retrieved facts separately from predictions. Record the
   assistant's candidate before revealing a withheld numerical reference when
   assessing prediction, and disclose its prior exposure to these development
   examples. Checking a retrieved value is not evidence of learned prediction.
4. Review whether continuations are useful and numerically grounded. There may
   be several useful next responses, so assess relevance and contextual support
   separately from numerical error, sign/unit correctness, and abstention.
   Scala can verify supported cells; broader totals need a declared computation.
5. Use the reviewed conversations to specify a matched prose/text-tuple/typed-
   numeric training comparison. Reserve fresh contexts and related entity/year
   groups before training; development examples are not the final evaluation.
   Model choice, numerical architecture, and training remain future decisions.

This proposes a concrete pilot; it does not authorize or report a training run.
The owner also requested committing the OneDrive starter packages so collaborators
can reproduce the exact-data baseline without access to the original machine.
