"""Focused collector controls: pagination, parsing, identity, and resume accounting."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest import mock

spec=importlib.util.spec_from_file_location('collector',Path(__file__).with_name('collect_eva_supplier_directory.py'))
c=importlib.util.module_from_spec(spec);spec.loader.exec_module(c)

def page(supplier='E001',name='A&amp;B&#160;Company',pages=1):
    return ('<DIV rdAjaxCommand="RefreshElement" rdDataCache="cache"><TR Row="1">'
      '<TD id="colVEND_CUST_CD_Row1"><SPAN id="lblVEND_CUST_CD_Row1">'+supplier+'</SPAN></TD>'
      '<TD><SPAN id="lblLGL_NM_Row1">'+name+'</SPAN></TD>'
      '<TD><SPAN id="lblLOC_NM_Row1">East</SPAN></TD></TR>'
      '<SPAN id="dtAnalysisGrid-PageOfPages">'+str(pages)+'</SPAN></DIV>').encode()

class CollectorControls(unittest.TestCase):
    def test_entities_identifiers_and_large_page_count(self):
        rows,pages,cache=c.parse_page(page(pages=197).decode())
        self.assertEqual((rows[0]['supplier_name'],rows[0]['eva_supplier_id'],pages,cache),('A&B Company','E001',197,'cache'))
    def test_missing_supplier_id_is_preserved_and_flagged(self):
        rows,_,_=c.parse_page(page(supplier='').decode())
        self.assertEqual(rows[0]['quality_flags'],'missing_supplier_id')
        self.assertEqual(rows[0]['detail_url'],'')
    def test_location_changes_are_not_supplier_id_deduplicated(self):
        r=c.parse_page(page().decode())[0][0]
        self.assertNotEqual(c.identity(r),c.identity(dict(r,location_name='West')))
    def test_unknown_response_is_not_empty_success(self):
        with self.assertRaises(ValueError):c.parse_page('<html>Unexpected response</html>')
    def test_form_excludes_unchecked_controls(self):
        form=c.Form();form.feed('<input name="key" value="v"><input type="checkbox" name="x"><input type="radio" name="r" value="no"><input type="radio" name="r" value="yes" checked>')
        self.assertEqual(form.values,{'key':'v','r':'yes'})
    def collect(self,responses):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            for folder in ['queries','source_pages']:(root/folder).mkdir()
            with mock.patch.object(c,'session',return_value=(object(),{})),mock.patch.object(c,'request',side_effect=responses):
                return c.collect_prefix('A',root,0,8)
    def test_real_pagination_preserves_both_pages(self):
        result=self.collect([page('E001',pages=2),page('E002',pages=2)])
        self.assertEqual((result['status'],result['row_count']),('collected',2))
    def test_large_query_splits_after_first_page(self):
        result=self.collect([page(pages=197)])
        self.assertEqual(result['status'],'split_capped')
        self.assertEqual(len(result['pages']),1)
        self.assertIn('AA',result['children'])
    def test_failed_empty_export_preserves_existing_snapshot(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory);path=root/'suppliers.csv';path.write_text('preserved'*200)
            with self.assertRaises(RuntimeError):c.export(root,{},[])
            self.assertEqual(path.read_text(),'preserved'*200)
    def test_repeated_page_is_an_error(self):
        result=self.collect([page(pages=2),page(pages=2)])
        self.assertEqual(result['status'],'error')
        self.assertIn('Repeated page',result['error'])
    def test_manifest_does_not_report_completed_roots_as_pending(self):
        with tempfile.TemporaryDirectory() as directory:
            root=Path(directory)
            r={'prefix':'A','status':'collected','rows':[],'children':[]}
            m=c.export(root,{'A':r},['A'])
            self.assertEqual(m['pending_prefixes'],[])
            self.assertTrue(m['execution_complete'])

if __name__=='__main__':unittest.main()