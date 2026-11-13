"""Pilot contract checks; post-engine preparation under the approved experiment scope."""
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from training_language_pilot import LABELS, prompt
from three_model_core import fiscal_year_of, observable


class PilotContracts(unittest.TestCase):
    def test_relabeling_preserves_values_and_hides_target_identity(self):
        row = dict(values=['17', '3', '5', '901.25'], vendor='SECRET_VENDOR',
                   target='SECRET_TARGET', fy=2014)
        for condition in LABELS:
            text = prompt(row, condition)
            self.assertNotIn('SECRET', text)
            actual = text.split('Latest observed month: ')[1].split('.\n')[0]
            self.assertEqual([field.split(': ')[1] for field in actual.split('; ')], row['values'])

    def test_mismatch_changes_mapping_not_vocabulary(self):
        self.assertEqual(set(LABELS['meaningful']), set(LABELS['mismatched']))
        self.assertTrue(all(a != b for a, b in zip(LABELS['meaningful'], LABELS['mismatched'])))

    def test_source_periods_and_censoring_are_distinct(self):
        self.assertEqual(fiscal_year_of('2013/07'), 2014)
        self.assertEqual(fiscal_year_of('0013/07'), 2013)
        self.assertFalse(observable(2016, set(range(2010, 2018)), 2016))
        self.assertTrue(observable(2015, set(range(2010, 2018)), 2016))


if __name__ == '__main__':
    unittest.main()
