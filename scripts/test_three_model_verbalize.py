"""Controls for the three-model verbalizer: schema refusal, determinism, substrate isolation, lineage ids.

Run from the repository root:  python3 -m unittest scripts/test_three_model_verbalize.py
Fixtures: data/three_model_fixture/ (output of the fixture pipeline, steps 2,3,20,21, FY03 small fixture).
"""
import importlib.util
import json
import tempfile
import unittest
from pathlib import Path

HERE = Path(__file__).resolve().parent
FIXTURES = HERE.parent / "data" / "three_model_fixture"
spec = importlib.util.spec_from_file_location("verbalize", HERE / "three_model_verbalize.py")
v = importlib.util.module_from_spec(spec)
spec.loader.exec_module(v)

GL_FILE = FIXTURES / "GL2003_sample.csv"
LDGR_FILE = FIXTURES / "LDGR2003_sample.csv"


def texts(substrate, path):
    return [v.verbalize(substrate, values) for _, values in v.read_records(substrate, path)]


class Labels(unittest.TestCase):
    def test_labels(self):
        self.assertEqual(v.label("ldgrIPID"), "IPID")
        self.assertEqual(v.label("ldgrContractID"), "Contract ID")
        self.assertEqual(v.label("ldgrLdgrlID"), "Ldgrl ID")
        self.assertEqual(v.label("ldgrExtensionIDAuditTrail"), "Extension ID Audit Trail")
        self.assertEqual(v.label("ldgrDirVsOffsetFlg"), "Dir Vs Offset Flg")
        self.assertEqual(v.label("nominal_account_id"), "nominal account id")


class GlSubstrate(unittest.TestCase):
    def test_all_fixture_rows_and_exact_first_text(self):
        t = texts("gl", GL_FILE)
        self.assertEqual(len(t), 27)
        self.assertEqual(
            t[0],
            "Posted general ledger balance. agency: 267; nominal account id: 0000; fund: 0000; "
            "object id: 0000; fiscal year: 03; dir flag: O; total amount: -827898.80.",
        )

    def test_gl_text_carries_no_instrument_ledger_field(self):
        for text in texts("gl", GL_FILE):
            for forbidden in ("IPID", "Commitment", "Ledger Period", "Source System"):
                self.assertNotIn(forbidden, text)


class LdgrSubstrate(unittest.TestCase):
    def test_all_fixture_rows_and_selected_fields(self):
        t = texts("ldgr", LDGR_FILE)
        self.assertEqual(len(t), 8)
        first = t[0]
        self.assertTrue(first.startswith("Instrument ledger entry. IPID: 0000001630; Contract ID: 0; "))
        self.assertIn("Source System ID: Payment Sys", first)
        self.assertIn("Trans Amount: -1987.81", first)
        self.assertIn("Dir Vs Offset Flg: O", first)
        self.assertTrue(first.endswith("Extension ID Custom: blank."))

    def test_blank_placeholder_is_written_as_blank(self):
        # The fixture writes " " in Unit Of Measure and the extension columns.
        self.assertIn("Unit Of Measure: blank", texts("ldgr", LDGR_FILE)[0])

    def test_every_ldgr_column_appears_once_in_header_order(self):
        text = texts("ldgr", LDGR_FILE)[0]
        positions = [text.index(v.label(c) + ":") for c in v.LDGR_HEADER]
        self.assertEqual(positions, sorted(positions))
        self.assertEqual(text.count(";") + 1, len(v.LDGR_HEADER))


class Determinism(unittest.TestCase):
    def test_same_input_same_output(self):
        self.assertEqual(texts("ldgr", LDGR_FILE), texts("ldgr", LDGR_FILE))
        self.assertEqual(texts("gl", GL_FILE), texts("gl", GL_FILE))


class Refusals(unittest.TestCase):
    def write(self, directory, name, content):
        path = Path(directory) / name
        path.write_text(content, encoding="utf-8")
        return path

    def test_wrong_substrate_header_is_refused(self):
        with self.assertRaises(ValueError):
            list(v.read_records("gl", LDGR_FILE))
        with self.assertRaises(ValueError):
            list(v.read_records("ldgr", GL_FILE))

    def test_changed_header_is_refused(self):
        with tempfile.TemporaryDirectory() as d:
            header = ",".join(v.GL_HEADER).replace("agency", "agency_id")
            path = self.write(d, "GL.csv", header + "\n267,0000,0000,0000,03,O,1.00\n")
            with self.assertRaises(ValueError):
                list(v.read_records("gl", path))

    def test_short_and_long_rows_are_refused(self):
        with tempfile.TemporaryDirectory() as d:
            header = ",".join(v.GL_HEADER)
            short = self.write(d, "short.csv", header + "\n267,0000,0000,0000,03,O\n")
            long = self.write(d, "long.csv", header + "\n267,0000,0000,0000,03,O,1.00,extra\n")
            with self.assertRaises(ValueError):
                list(v.read_records("gl", short))
            with self.assertRaises(ValueError):
                list(v.read_records("gl", long))

    def test_empty_file_is_refused(self):
        with tempfile.TemporaryDirectory() as d:
            with self.assertRaises(ValueError):
                list(v.read_records("gl", self.write(d, "empty.csv", "")))


class CommandLine(unittest.TestCase):
    def test_output_ids_are_file_and_line(self):
        with tempfile.TemporaryDirectory() as d:
            out = Path(d) / "gl.jsonl"
            self.assertEqual(v.main(["--substrate", "gl", "--in", str(GL_FILE), "--out", str(out)]), 0)
            records = [json.loads(line) for line in out.read_text(encoding="utf-8").splitlines()]
            self.assertEqual(len(records), 27)
            self.assertEqual(records[0]["id"], "GL2003_sample.csv:2")
            self.assertEqual(records[-1]["id"], "GL2003_sample.csv:28")


if __name__ == "__main__":
    unittest.main()
