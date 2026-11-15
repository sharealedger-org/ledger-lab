Connected synthetic temporal/vendor reporting fixture v1

All names, keys, dates, amounts and source events are explicitly simulated.
Raw input goes through actual APA/ARE, PO, merge, posting, GL, permutation and
legacy views, followed by expanded reporting over the actual committed outputs.
A prepared synthetic reference bundle replaces this source fixture's reference
surrogates using the same preparation contract as the new corpus workload.

FY2014: five expenses net 175 (supplier c1246=115, c12464=40, pool=20),
one receipt 60, no PO rows. Expected 12 balanced SJE lines.
FY2015: receipt 200 plus commitment 50 for c1246. Expected four balanced SJE lines.
Supplier c1246 has 100 before the 2014-06-23 template leaf-parent change and
25-10=15 on/after. EVENT_DATE shows these under old/new parents; AS_OF 2014-06-30
puts all 115 under the new parent. RUN_DATE 2015-07-01 likewise uses the new parent.
Supplier c12464 has no financial event after July 2013; the supplied run-date
projection still selects its changed 2015 hierarchy ancestor, without journals.
The master size tier for c1246 is SMALL at FY2014 cutoff and LARGE at FY2015 cutoff.
Zero/unattributed pool and agency receipts remain zero; no synthetic party assigned.

synthetic_assignments.csv gives explicit fixture mappings to the separate
reclass_history_v1 template inputs. Full corpus reporting uses the declared stable
SHA-256 scenario assignment instead. This is a synthetic analytical profile link,
not a real legal-party merge, a re-keying, or a claimed vendor identity recovery.
