"""Controls for the three-model label, split, sample, target and metric code, and the results writer.

Run from the repository root:
  python3 -m unittest scripts/test_three_model_core.py
Fixtures: data/three_model_fixture/ (fixture pipeline output; its periods are two-digit, which the
code rejects on purpose, so tests that need a valid period overwrite it with a YYYY/MM value).
"""
import csv
import importlib.util
import os
import tempfile
import unittest
from decimal import Decimal
from pathlib import Path

HERE = Path(__file__).resolve().parent
FIXTURES = HERE.parent / "data" / "three_model_fixture"


def load(name, filename):
    spec = importlib.util.spec_from_file_location(name, HERE / filename)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


c = load("three_model_core", "three_model_core.py")
r = load("three_model_results", "three_model_results.py")

ELIGIBLE = c.parse_fy_spec("2006-2008,2010-2017")


def fixture_rows(name, period_col, period):
    with open(FIXTURES / name, newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))
    for row in rows:
        row[period_col] = period
    return rows


class FiscalYear(unittest.TestCase):
    def test_july_starts_next_fiscal_year(self):
        self.assertEqual(c.fiscal_year_of("2014/06"), 2014)
        self.assertEqual(c.fiscal_year_of("2014/07"), 2015)
        self.assertEqual(c.fiscal_year_of("2014/12"), 2015)

    def test_payment_file_period_is_the_fiscal_year_label(self):
        # Gold set: payment rows carry 00YY/MM, YY is the file's fiscal year, month does not shift it.
        self.assertEqual(c.fiscal_year_of("0003/05"), 2003)
        self.assertEqual(c.fiscal_year_of("0003/07"), 2003)
        self.assertEqual(c.fiscal_year_of("0016/12"), 2016)

    def test_last_complete_counts_months_across_both_formats(self):
        mixed = {2016: [f"00{16}/{m:02d}" for m in range(1, 13)] + ["2015/07", "2016/03"],
                 2017: ["0017/01", "0017/02", "2016/07", "2017/01"]}
        self.assertEqual(c.last_complete_fy(mixed), 2016)

    def test_rejects_bare_year_short_form_and_bad_month(self):
        for bad in ("2014", "03", "14/07", "2014/7", "2014/13", "2014/00", "", "abcd/ef"):
            with self.assertRaises(ValueError, msg=bad):
                c.fiscal_year_of(bad)

    def test_spec_and_last_complete(self):
        self.assertEqual(c.parse_fy_spec("2006-2008,2010"), {2006, 2007, 2008, 2010})
        months = {fy: [f"{fy}/{m:02d}" for m in (7, 8, 9, 10, 11, 12)] for fy in (2014, 2015)}
        months[2013] = [f"2012/{m:02d}" for m in range(7, 13)] + [f"2013/{m:02d}" for m in range(1, 7)]
        self.assertEqual(c.last_complete_fy(months), 2013)
        self.assertIsNone(c.last_complete_fy({2014: ["2013/07"]}))


class SurvivalLabelsAndSplits(unittest.TestCase):
    def test_censoring_and_labels(self):
        present = {("v1", 2014), ("v1", 2015), ("v2", 2014), ("v1", 2016), ("v3", 2008), ("v3", 2007)}
        panel = c.survival_panel(present, ELIGIBLE, last_complete=2016)
        # v1 FY2016 censored (2017 > last complete); v3 FY2008 censored (2009 not eligible);
        # v3 FY2007 has no FY2008?  it does (v3, 2008) -> survived
        self.assertEqual(panel, [("v1", 2014, 1), ("v1", 2015, 1), ("v2", 2014, 0), ("v3", 2007, 1)])

    def test_last_year_is_never_counted_as_churn(self):
        panel = c.survival_panel({("v1", 2016)}, ELIGIBLE, last_complete=2016)
        self.assertEqual(panel, [])

    def test_d4_split_matches_plan(self):
        years = [fy for fy in sorted(ELIGIBLE) if c.observable(fy, ELIGIBLE, 2016)]
        run = c.longest_run(years)
        self.assertEqual(run, [2010, 2011, 2012, 2013, 2014, 2015])
        train, test = c.holdout_split(run, {2015, 2016})
        self.assertEqual((train, test), ([2010, 2011, 2012, 2013], [2014, 2015]))
        windows = c.walk_forward_windows(run, 2)
        self.assertEqual(len(windows), 4)
        self.assertEqual(windows[0], ([2010, 2011], 2012))
        self.assertEqual(windows[-1], ([2010, 2011, 2012, 2013, 2014], 2015))

    def test_longest_run_tie_takes_later(self):
        self.assertEqual(c.longest_run([1, 2, 5, 6]), [5, 6])
        self.assertEqual(c.longest_run([]), [])


class TrainingSample(unittest.TestCase):
    def test_deterministic_and_seed_dependent(self):
        keys = [str(i) for i in range(2000)]
        a = [c.keep_entity(k, 42, 0.1) for k in keys]
        self.assertEqual(a, [c.keep_entity(k, 42, 0.1) for k in keys])
        self.assertNotEqual(a, [c.keep_entity(k, 43, 0.1) for k in keys])
        self.assertTrue(120 < sum(a) < 280)   # about 10% of 2000

    def test_fraction_bounds(self):
        self.assertTrue(all(c.keep_entity(str(i), 1, 1.0) for i in range(100)))
        self.assertFalse(any(c.keep_entity(str(i), 1, 0.0) for i in range(100)))
        self.assertEqual(c.sample_fraction(20000, 1000), 1.0)
        self.assertAlmostEqual(c.sample_fraction(20000, 2000000), 0.01)
        with self.assertRaises(ValueError):
            c.sample_fraction(0, 10)

    def test_ldgr_event_lines_stay_together_and_entity_is_kept_every_year(self):
        rows = []
        for period in ("2011/08", "2012/08"):   # FY2012 and FY2013
            rows += fixture_rows("LDGR2003_sample.csv", "ldgrLedgerPeriod", period)
        kept = list(c.select_training_rows("ldgr", rows, [2012, 2013], 42, 0.5))
        by_key = {}
        for row in kept:
            by_key.setdefault((row["ldgrIPID"], row["ldgrLedgerPeriod"]), []).append(row["ldgrDirVsOffsetFlg"])
        for flags in by_key.values():
            self.assertEqual(sorted(flags), ["D", "O"])   # both lines of the event stay together
        decided = {i: c.keep_entity(i, 42, 0.5) for i in {row["ldgrIPID"] for row in rows}}
        self.assertEqual({k[0] for k in by_key}, {i for i, keep in decided.items() if keep})
        for ipid in {k[0] for k in by_key}:   # kept in both years, not just one
            self.assertEqual({p for i, p in by_key if i == ipid}, {"2011/08", "2012/08"})

    def test_only_training_years_and_bad_period_fails(self):
        rows = fixture_rows("GL2003_sample.csv", "fiscal_year", "2009/09")   # FY2010
        self.assertEqual(len(list(c.select_training_rows("gl", rows, [2010], 1, 1.0))), len(rows))
        self.assertEqual(list(c.select_training_rows("gl", rows, [2011], 1, 1.0)), [])
        with self.assertRaises(ValueError):
            list(c.select_training_rows("gl", fixture_rows("GL2003_sample.csv", "fiscal_year", "03"), [2010], 1, 1.0))

    def test_entity_keys(self):
        gl = fixture_rows("GL2003_sample.csv", "fiscal_year", "2009/09")[0]
        self.assertEqual(c.entity_key("gl", gl), "267|0000|0000|0000")
        ld = fixture_rows("LDGR2003_sample.csv", "ldgrLedgerPeriod", "2009/09")[0]
        self.assertEqual(c.entity_key("ldgr", ld), "0000001630")
        with self.assertRaises(ValueError):
            c.entity_key("x", gl)


class ForecastTargets(unittest.TestCase):
    def ldgr_rows(self):
        return fixture_rows("LDGR2003_sample.csv", "ldgrLedgerPeriod", "2011/08")

    def test_ldgr_totals_use_debit_lines_only(self):
        totals = c.agency_year_debit_totals("ldgr", self.ldgr_rows())
        expected = Decimal("1987.81") + Decimal("2411.02") + Decimal("2859") + Decimal("3496.78")
        self.assertEqual(totals, {("267", 2012): expected})

    def test_gl_and_ldgr_agree_when_gl_is_the_sum_of_ldgr(self):
        ldgr_totals = c.agency_year_debit_totals("ldgr", self.ldgr_rows())
        gl_rows = [
            {"agency": "267", "nominal_account_id": "EXP316", "fund": "1552", "object_id": "316",
             "fiscal_year": "2011/08", "dir_flag": "D", "total_amount": "8343.59"},
            {"agency": "267", "nominal_account_id": "EXP312", "fund": "1552", "object_id": "312",
             "fiscal_year": "2011/08", "dir_flag": "D", "total_amount": "2411.02"},
            {"agency": "267", "nominal_account_id": "0000", "fund": "0000", "object_id": "0000",
             "fiscal_year": "2011/08", "dir_flag": "O", "total_amount": "-10754.61"},
        ]
        report = c.compare_totals(c.agency_year_debit_totals("gl", gl_rows), ldgr_totals)
        self.assertTrue(report["agree"], report)
        self.assertEqual(report["n_common"], 1)

    def test_difference_is_reported_not_hidden(self):
        gl = {("267", 2012): Decimal("100.00"), ("300", 2012): Decimal("5")}
        ld = {("267", 2012): Decimal("100.50")}
        report = c.compare_totals(gl, ld)
        self.assertFalse(report["agree"])
        self.assertEqual((report["n_mismatch"], report["n_only_gl"], report["n_only_ldgr"]), (1, 1, 0))
        self.assertEqual(report["max_abs_diff"], Decimal("0.50"))
        self.assertEqual(c.compare_totals(gl, ld, tolerance=Decimal("1"))["n_mismatch"], 0)

    def test_commitment_debits_are_not_in_the_target(self):
        rows = [
            {"agency": "267", "nominal_account_id": "EXP312", "fiscal_year": "0012/03", "dir_flag": "D", "total_amount": "10"},
            {"agency": "Department of X", "nominal_account_id": "COMMIT", "fiscal_year": "2012/03", "dir_flag": "D", "total_amount": "999"},
            {"agency": "267", "nominal_account_id": "0000", "fiscal_year": "0012/03", "dir_flag": "O", "total_amount": "-10"},
        ]
        self.assertEqual(c.agency_year_debit_totals("gl", rows), {("267", 2012): Decimal(10)})

    def test_bad_amount_and_bad_period_raise(self):
        row = {"agency": "1", "nominal_account_id": "EXP1", "fiscal_year": "2011/08", "dir_flag": "D", "total_amount": "x"}
        with self.assertRaises(ValueError):
            c.agency_year_debit_totals("gl", [row])
        row.update(total_amount="1", fiscal_year="03")
        with self.assertRaises(ValueError):
            c.agency_year_debit_totals("gl", [row])

    def test_pairs_censor_last_year_and_count_dropped(self):
        totals = {("a", 2014): Decimal(10), ("a", 2015): Decimal(12), ("a", 2016): Decimal(9),
                  ("b", 2014): Decimal(5)}   # b has no 2015
        pairs, dropped = c.forecast_pairs(totals, last_complete=2016)
        self.assertEqual(pairs, [("a", 2014, Decimal(10), Decimal(12)), ("a", 2015, Decimal(12), Decimal(9))])
        self.assertEqual(dropped, 1)   # ("b", 2014); ("a", 2016) is censored, not dropped


class Metrics(unittest.TestCase):
    def test_c_statistic(self):
        self.assertEqual(c.c_statistic([1, 1, 0, 0], [0.9, 0.8, 0.7, 0.1]), 1.0)
        self.assertEqual(c.c_statistic([1, 1, 0, 0], [0.1, 0.2, 0.7, 0.9]), 0.0)
        self.assertEqual(c.c_statistic([1, 0], [0.5, 0.5]), 0.5)   # ties share rank
        self.assertAlmostEqual(c.c_statistic([1, 0, 1, 0], [0.9, 0.8, 0.3, 0.1]), 0.75)
        self.assertIsNone(c.c_statistic([1, 1], [0.1, 0.2]))

    def test_permutation_p(self):
        labels = [1] * 10 + [0] * 10
        perfect = list(range(20, 10, -1))[:10] + list(range(10, 0, -1))[:10]
        p = c.permutation_p(labels, perfect, 199, seed=1)
        self.assertEqual(p, 1 / 200)
        self.assertEqual(p, c.permutation_p(labels, perfect, 199, seed=1))
        noise = [3, 15, 8, 19, 1, 12, 6, 17, 10, 4, 0, 13, 7, 18, 2, 11, 5, 16, 9, 14]   # C = 0.5
        self.assertGreater(c.permutation_p(labels, noise, 199, seed=1), 0.05)
        self.assertIsNone(c.permutation_p([1, 1], [0.1, 0.2], 10))

    def test_smape_rmse(self):
        self.assertAlmostEqual(c.smape([100], [100]), 0.0)
        self.assertAlmostEqual(c.smape([100], [50]), 2 * 50 / 150)
        self.assertAlmostEqual(c.smape([0, 100], [0, 100]), 0.0)   # 0/0 counts as zero error
        self.assertAlmostEqual(c.smape([0], [10]), 2.0)
        self.assertAlmostEqual(c.rmse([0, 0], [3, 4]), 12.5 ** 0.5)
        with self.assertRaises(ValueError):
            c.rmse([], [])

    def test_net_income_scoring(self):
        self.assertEqual(c.score_net_income("1000.00", Decimal("1000.004")), "correct")
        self.assertEqual(c.score_net_income(999, "1000"), "incorrect")
        self.assertEqual(c.score_net_income("about a thousand", "1000"), "incorrect")
        self.assertEqual(c.score_net_income(c.NOT_ANSWERABLE, "1000"), c.NOT_ANSWERABLE)


class ResultsWriter(unittest.TestCase):
    def write(self, results):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, "out", "results.csv")
            n = r.write_results(path, results, "train FY2010-2013; test FY2014-2015", 42, "base-not-chosen",
                                "none", timestamp="2026-11-11 00:00:00")
            with open(path, newline="", encoding="utf-8") as handle:
                return n, list(csv.DictReader(handle))

    def test_rows_and_gaps(self):
        results = [
            r.Result("Posted-FineTune", "forecast", "smape", 0.50, n_test=10),
            r.Result("IL-FineTune", "forecast", "smape", 0.40, n_test=10),
            r.Result("IL-Native", "forecast", "smape", 0.30, n_test=10),
        ]
        n, rows = self.write(results)
        self.assertEqual(n, 6)
        self.assertEqual([x["row_type"] for x in rows], ["model"] * 3 + ["gap"] * 3)
        gaps = {x["model"]: x for x in rows if x["row_type"] == "gap"}
        self.assertAlmostEqual(float(gaps["gap_1_2_substrate"]["value"]), -0.10)
        self.assertAlmostEqual(float(gaps["gap_2_3_language_prior"]["value"]), -0.10)
        self.assertAlmostEqual(float(gaps["gap_1_3_total"]["value"]), -0.20)
        self.assertEqual(gaps["gap_1_3_total"]["better"], "lower")
        self.assertEqual(rows[0]["seed"], "42")
        self.assertEqual(rows[0]["split"], "train FY2010-2013; test FY2014-2015")

    def test_not_answerable_is_not_a_zero_and_gap_follows(self):
        results = [
            r.Result("Posted-FineTune", "survival", "c_statistic", None, c.NOT_ANSWERABLE),
            r.Result("IL-FineTune", "survival", "c_statistic", 0.70),
            r.Result("IL-Native", "survival", "c_statistic", 0.72),
        ]
        _, rows = self.write(results)
        by_model = {x["model"]: x for x in rows}
        self.assertEqual(by_model["Posted-FineTune"]["value"], "")
        self.assertEqual(by_model["Posted-FineTune"]["status"], "not_answerable")
        self.assertEqual(by_model["gap_1_2_substrate"]["status"], "not_answerable")
        self.assertEqual(by_model["gap_1_2_substrate"]["value"], "")
        self.assertEqual(by_model["gap_1_3_total"]["status"], "not_answerable")
        self.assertAlmostEqual(float(by_model["gap_2_3_language_prior"]["value"]), 0.02)

    def test_refuses_bad_rows(self):
        bad = [
            r.Result("Other", "t", "smape", 1.0),
            r.Result("IL-Native", "t", "bogus", 1.0),
            r.Result("IL-Native", "t", "smape", None),
            r.Result("IL-Native", "t", "smape", 1.0, c.NOT_ANSWERABLE),
            r.Result("IL-Native", "t", "smape", 1.0, "weird"),
        ]
        for item in bad:
            with self.assertRaises(ValueError, msg=str(item)):
                self.write([item])

    def test_partial_pairs_make_only_available_gaps(self):
        _, rows = self.write([r.Result("IL-FineTune", "t", "rmse", 2.0), r.Result("IL-Native", "t", "rmse", 1.0)])
        gaps = [x["model"] for x in rows if x["row_type"] == "gap"]
        self.assertEqual(gaps, ["gap_2_3_language_prior"])


if __name__ == "__main__":
    unittest.main()
