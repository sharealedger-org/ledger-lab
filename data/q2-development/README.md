# Shared Q2 development data

Owner requested these OneDrive packages be committed on 2026-11-18 so others
can reproduce the initial explorations. Source: `LedgerLabOutputs-Q2-Development`.
Both original compressed packages are preserved byte-for-byte, with the larger
archive represented as ordered 40 MiB parts. No Git LFS or OneDrive access is
needed. Total compressed payload is about 202 MiB.

## Start exploring

Requires Java and sbt. From the repository root:

```bash
bash scripts/start_q2_explorer.sh
```

This verifies and reconstructs the report under ignored `session-run.tmp/`,
then builds/runs the standalone Scala pipeline. Allow about 1 GiB for report
restoration plus build dependencies. Optional arguments are vendor ID and fiscal
year; defaults reproduce the documented 80-cell FY2044 example. Use
`--prepare-only` to restore without launching. The report is scanned on startup.
The standalone project is intentional: the root build currently fails to resolve
the pipeline assembly plugin.

See [Q2 process](../../tracks/D-experiment/aim/Q2_PROCESS.md) for commands and
the experiment proposal. The explorer is exact report access, not a trained model.

## Other starter reports and provenance

`phase2_20261117_v1/` preserves the original README, checksum and 55 MiB archive.
It includes combined AIM features, activity, temporal and attribute summaries,
coverage, controls, and the producer source snapshot. Extract separately:

```bash
(cd data/q2-development/phase2_20261117_v1 && shasum -a 256 -c SHA256SUMS)
mkdir -p session-run.tmp/q2-starter
tar -xzf data/q2-development/phase2_20261117_v1/q2_arm_development_20261117_v1.tar.gz -C session-run.tmp/q2-starter
(cd session-run.tmp/q2-starter/q2_arm_development_20261117_v1 && shasum -a 256 -c SHA256SUMS)
```

The packages have separate provenance; preserve their original receipts rather
than assuming they derive from the same reporting attempt. These are development
data, not held-out evaluation data. Synthetic truth and diagnostics must not
silently become model inputs. Payroll classification has not been established;
staffing responsibility records are not employee payroll records.

## Verification on 2026-11-18

Both original archive checksums passed. All four committed explorer parts and
the reconstructed gzip passed checksum verification. All 16 starter archive
manifest entries passed after extraction. Bash syntax and Git whitespace checks
passed. The shared launcher ran end to end through sbt, loaded the default
vendor's 80 FY2044 report cells, and exited successfully with `quit` on stdin.
No financial engine behavior changed and no model training or scoring ran.
