"""Read-only vendor investigation; stdlib only; outputs JSON to stdout.
Run from ledger-lab: py -3 scripts/investigate_vendor_identifiers.py
Network mode: append --specs to inspect official public layouts in memory.
No raw vendor identifiers or transaction rows are emitted.
"""
import argparse
import collections
import csv
from decimal import Decimal, InvalidOperation
import hashlib
import io
import json
from pathlib import Path
import re
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

NS = {'s': 'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}

def workbook_rows(data):
    with zipfile.ZipFile(io.BytesIO(data)) as z:
        strings = []
        if 'xl/sharedStrings.xml' in z.namelist():
            strings = [''.join(n.itertext()) for n in ET.fromstring(z.read('xl/sharedStrings.xml')).findall('s:si', NS)]
        for name in z.namelist():
            if not (name.startswith('xl/worksheets/sheet') and name.endswith('.xml')):
                continue
            for row in ET.fromstring(z.read(name)).findall('.//s:sheetData/s:row', NS):
                cells = {}
                for c in row.findall('s:c', NS):
                    v = c.find('s:v', NS)
                    text = v.text if v is not None else ''.join(c.itertext())
                    if c.get('t') == 's' and v is not None:
                        text = strings[int(v.text)]
                    cells[c.get('r')] = text or ''
                yield name, cells

def sha(path):
    h = hashlib.sha256()
    with path.open('rb') as f:
        for chunk in iter(lambda: f.read(1024*1024), b''):
            h.update(chunk)
    return h.hexdigest()

def shape(v):
    if not v: return 'blank'
    if v == '999999999999': return 'placeholder_12_nines'
    if re.fullmatch(r'[0-9]{9}', v): return 'nine_digits'
    if re.fullmatch(r'[A-Za-z]{2}[0-9]{7}', v): return 'two_letters_seven_digits'
    if v.startswith(('SUP', 'VS0', 'VC0')): return 'modern_supplier_prefix'
    return 'other'

def rows(path, delimiter=','):
    with path.open(encoding='utf-8-sig', errors='replace', newline='') as f:
        # Source PO files have a blank first line. Do not remove blank lines
        # inside quoted records.
        first = next(f, '')
        while first and not first.strip(): first = next(f, '')
        import itertools
        yield from csv.DictReader(itertools.chain([first], f), delimiter=delimiter)

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--data-root', type=Path, default=Path('data'))
    p.add_argument('--historical-root', type=Path, default=Path.home()/'Box/CommonKeyBufferingData/VA_2016_New')
    p.add_argument('--years', default='2016,2018')
    p.add_argument('--specs', action='store_true')
    a = p.parse_args()
    result = {'scope': 'Identifier semantics, namespace shapes, historical payment vendor overlap; not PO/payment linkage precision', 'files': {}, 'dictionary': [], 'public_layouts': {}, 'po_years': {}}
    dictionary = a.data_root/'VAPOData/data-dictionary.xlsx'
    data = dictionary.read_bytes()
    result['files']['dictionary'] = {'bytes': len(data), 'sha256': hashlib.sha256(data).hexdigest()}
    for sheet, cells in workbook_rows(data):
        if 'VENDORID' in cells.values():
            result['dictionary'].append({'sheet': sheet, 'cells': cells})
    historical_ids = set()
    for filename in ['EV02-MATCHED-PO-LESS-THAN-9000.csv', 'EV06-MATCHED-PAYMETS-LESS-THAN-9000.csv']:
        path = a.historical_root/filename
        if not path.exists():
            result['files'][filename] = {'status': 'missing'}
            continue
        counts = collections.Counter()
        count = 0
        fields = None
        for row in rows(path):
            count += 1
            fields = list(row)
            vid = (row.get('ip_rec_id') or '').strip()
            if vid: historical_ids.add(vid)
            counts[shape(vid)] += 1
        result['files'][filename] = {'sha256': sha(path), 'rows': count, 'columns': fields, 'ip_rec_id_shapes': dict(counts)}
    for year in a.years.split(','):
        path = a.data_root/f'VAPOData/VA_opendata_FY{year}.txt'
        if not path.exists():
            result['po_years'][year] = {'status': 'missing'}
            continue
        count = bad_arity = overlap_rows = 0
        shapes = collections.Counter()
        ids = set()
        overlaps = set()
        names = collections.defaultdict(set)
        for row in rows(path, '\t'):
            count += 1
            if None in row or any(v is None for v in row.values()): bad_arity += 1
            vid = (row.get('VENDORID') or '').strip()
            shapes[shape(vid)] += 1
            if vid: ids.add(vid)
            if vid in historical_ids:
                overlap_rows += 1
                overlaps.add(vid)
            name = re.sub(r'[^A-Z0-9]', '', (row.get('VENDORNAME') or '').upper())
            if name and vid and shape(vid) != 'placeholder_12_nines': names[name].add(vid)
        result['po_years'][year] = {'sha256': sha(path), 'rows': count, 'bad_arity_rows': bad_arity, 'id_shapes': dict(shapes), 'distinct_nonblank_ids': len(ids), 'normalized_names_with_multiple_nonplaceholder_ids': sum(len(v)>1 for v in names.values()), 'historical_distinct_ids_compared': len(historical_ids), 'historical_ids_present': len(overlaps), 'rows_with_historical_ids': overlap_rows}
    # Reconstruct the historical exact-amount candidate rule independently.
    po_path = a.historical_root/'EV02-MATCHED-PO-LESS-THAN-9000.csv'
    payment_path = a.historical_root/'EV06-MATCHED-PAYMETS-LESS-THAN-9000.csv'
    if po_path.exists() and payment_path.exists():
        totals = {}
        failures = 0
        for row in rows(po_path):
            key = (row['poNumber'], row['ip_rec_id'], row['org_unit_cd'])
            try:
                amount = Decimal(row['qunatity']) * Decimal(row['unit_price'])
                totals[key] = totals.get(key, Decimal(0)) + amount
            except InvalidOperation:
                failures += 1
        candidates = collections.Counter((vid, agency, amount) for (_, vid, agency), amount in totals.items())
        hits = collections.Counter()
        for row in rows(payment_path):
            matches = candidates[(row['ip_rec_id'], row['org_unit_cd'], Decimal(row['txn_amt']))]
            hits['payment_rows'] += 1
            hits['exact_candidate_hit_rows'] += matches > 0
            hits['ambiguous_hit_rows'] += matches > 1
            hits['placeholder_payment_rows'] += shape(row['ip_rec_id']) == 'placeholder_12_nines'
        result['historical_amount_probe'] = dict(hits, po_groups=len(totals), po_parse_failures=failures,
            caveat='Candidate equality is not a validated transaction link; no precision or recall established.')
    result['historical_id_comparison_caveat'] = ('Union of EV02 and EV06 IDs; includes one 12-nine placeholder. '
        'Subtract that placeholder from distinct overlap counts before interpreting identity coverage. '
        'Malformed PO rows are counted and flagged, not repaired; their field positions may be unreliable.')
    if a.specs:
        files = ['AP962-Vendor-Data-Extract-Agencies.xlsx', 'AP965-Voucher-Extract-Agencies-File-Layout.xlsx', 'AP705-Payment-Recon-Extract-Agencies.xlsx']
        keep = {'VENDOR_ID','ADDRESS_SEQ_NUM','ADDRESS3','CORPORATE_VENDOR','REMIT_VENDOR','VNDR_LOC','PAY_DOC_ID','INVOICE_ID','VOUCHER_ID','BUSINESS_UNIT','DESCR','NAME1'}
        for filename in files:
            url = 'https://www.cardinalproject.virginia.gov/sites/default/files/AgencyInterfaceLayouts/'+filename
            try:
                with urllib.request.urlopen(url, timeout=30) as response: data = response.read()
                findings = []
                for sheet, cells in workbook_rows(data):
                    text = ' '.join(cells.values())
                    if keep.intersection(cells.values()) or re.search(r'\bVLIN\b|\bDUNS\b|associated PO ID', text, re.I):
                        # Remove empty/style-only cells; preserve cell references as evidence.
                        findings.append({'sheet': sheet, 'cells': {k:v for k,v in cells.items() if v}})
                result['public_layouts'][filename] = {'url': url, 'sha256': hashlib.sha256(data).hexdigest(), 'bytes': len(data), 'findings': findings}
            except Exception as e:
                result['public_layouts'][filename] = {'url': url, 'error': type(e).__name__+': '+str(e)}
    print(json.dumps(result, indent=2, sort_keys=True))

if __name__ == '__main__': main()