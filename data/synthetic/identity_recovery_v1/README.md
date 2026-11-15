# Identity recovery v1: simulated connected fixture

All names, parties, receipts and orders in this directory are explicitly simulated.
No new real Virginia vendors or taxpayers are asserted. `c1246` and `c12464` are
chosen to expose the variable-width key-order defect; they are simulation IDs here,
not a crosswalk to real PO or legal-party identities. `SIM_VENDOR_0001` is a
simulated administrator. Unknown parties and the payroll pool remain unattributed.
The registry is the three-row `VendorMaster.csv`, with namespace, party type,
parent bucket and effective dates. This is a pilot, not a proposed corpus population.

FY03: six expense events net 160 (supplier one 75, supplier two 40,
administrator 20, unattributed 25); one agency-based receipt 200; two PO commitments
50 and 70. Expected 18 balanced journal lines. FY04: supplier one expense 30,
expected two journal lines. The negative expense is an explicit simulated reversal.
Quoted object/vendor text must preserve reference and party identity. Both suppliers
share the same deliberately literal IDs between simulated payments and orders.
Each voucher/order is labeled `SIM-`; production ARE lineage/rules remain in use.

Run with a fresh absolute destination `$run_root`:

```bash
bash scripts/run_full_corpus.sh \
  --root "$PWD/data/synthetic/identity_recovery_v1" \
  --outRoot "$run_root/output" --logs "$run_root/logs" \
  --years 03,04 --lanes 1 --chainWidth 1 --periodGranularity monthly \
  --ttlType B --vendorMaster "$PWD/data/synthetic/identity_recovery_v1/VendorMaster.csv" \
  --noDashboard
bash scripts/run_reporting.sh \
  --inPaths "$run_root/output/FY03,$run_root/output/FY04" \
  --outPath "$run_root/report" \
  --vendorMaster "$PWD/data/synthetic/identity_recovery_v1/VendorMaster.csv" \
  --refPath "$PWD/data/synthetic/identity_recovery_v1" \
  --aggregateKeys 1 --spillRows 1 --mergeFanIn 2
cd 02-foundation/va_pipeline
CKB_IDENTITY_RECOVERY_OUTPUT="$run_root" sbt 'testOnly *IdentityRecoveryFixtureSpec'
```

The Scala checker verifies independent expected amounts, literal party IDs,
per-journal balancing, reference keys, rule/date fields, actual posted outputs,
key-level reporting reconciliation, name coverage and matched payment/PO features.
The regular reporting suite separately tests two groups above 250,000 rows and
small/large spill equivalence. Fixture evidence does not establish corpus scale,
target GenevaERS capabilities, cost comparability, or independent architecture approval.
