"""Probe observed legacy-PO to public-VLIN bridges; stdlib, read-only.
Run from ledger-lab: py -3 scripts/probe_public_vendor_bridge.py
Convenience sample only, not a population coverage estimate or payment linkage.
"""
import argparse
import collections
import concurrent.futures
import datetime
from decimal import Decimal, InvalidOperation
import hashlib
import io
import json
from pathlib import Path
import urllib.parse
import urllib.request
import zipfile
from investigate_vendor_identifiers import rows, shape

BASE = 'https://data.virginia.gov/api/3/action/'
RESOURCES = {'2016':'4f72e337-db1a-4dfc-b4b9-1096a56cc7b2', '2018':'9b5b3b80-8edc-453b-960f-0a7203aa9353'}

def fetch(url):
    with urllib.request.urlopen(url, timeout=25) as r:
        data = r.read()
    return data

def query(action, args):
    url = BASE + action + '?' + urllib.parse.urlencode(args)
    data = fetch(url)
    parsed = json.loads(data, parse_float=Decimal)
    if not parsed.get('success'): raise ValueError('CKAN success=false')
    return parsed['result'], url, hashlib.sha256(data).hexdigest()

def dec(v):
    try: return Decimal(str(v).strip())
    except (InvalidOperation, ValueError): return None

def norm(v): return ' '.join(str(v or '').upper().split())

def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--data-root', type=Path, default=Path('data'))
    p.add_argument('--sample-per-year', type=int, default=12)
    p.add_argument('--spcc-schema', action='store_true')
    p.add_argument('--historical-root', type=Path, default=Path.home()/'Box/CommonKeyBufferingData/VA_2016_New')
    a = p.parse_args()
    if not 1 <= a.sample_per_year <= 30: p.error('sample-per-year must be 1..30')
    output = {'retrieved_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
      'scope':'Observed PO-to-PO namespace bridge; not a payment join or population coverage estimate',
      'selection':'First well-formed row for each distinct nonblank legacy vendor ID in file order, including placeholder as a negative control',
      'match_rule':'Exact full PO number (no revision stripping), whitespace/case-normalized agency, NIGP identifier, Decimal quantity and unit price. Vendor name is a separate concordance check, not a matching prerequisite.',
      'years':{}}
    for year, rid in RESOURCES.items():
        schema, url, digest = query('datastore_search', {'resource_id':rid,'limit':0})
        path = a.data_root/'VAPOData'/f'VA_opendata_FY{year}.txt'
        selected = []; seen = set()
        for ordinal, row in enumerate(rows(path,'\t'),1):
            if None in row or any(v is None for v in row.values()): continue
            vid = row['VENDORID'].strip()
            if not vid or vid in seen: continue
            seen.add(vid); selected.append((ordinal,row))
            if len(selected) == a.sample_per_year: break
        def probe(sample):
            ordinal, local = sample
            item={'source_record_ordinal':ordinal, 'po_number':local['PONUMBER'],
              'legacy_vendor_id':local['VENDORID'], 'legacy_id_shape':shape(local['VENDORID'])}
            try:
                remote, url, digest = query('datastore_search', {'resource_id':rid,
                  'filters':json.dumps({'Order #':local['PONUMBER']}),'limit':250})
                item.update(query_url=url,response_sha256=digest,remote_po_rows=remote['total'])
                if remote['total'] > len(remote['records']):
                    item['status']='truncated_not_classified'; return item
                candidates=[]
                for r in remote['records']:
                    if (r.get('Order #')==local['PONUMBER']
                      and norm(r.get('Entity Description'))==norm(local['AGENCY'])
                      and str(r.get('NIGP #') or '').strip()==local['NIGPCODE'].strip()
                      and dec(r.get('Quantity Ordered'))==dec(local['QUANTITYORDERED'])
                      and dec(r.get('Unit Price'))==dec(local['PRICE'])
                      and dec(local['PRICE']) is not None and dec(local['QUANTITYORDERED']) is not None):
                        candidates.append(r)
                pairs=sorted({(r.get('VLIN ID') or '',r.get('eVA ID') or '') for r in candidates})
                item.update(matching_line_rows=len(candidates),bridge_identifier_pairs=pairs,
                  vendor_name_concordant_rows=sum(norm(r.get('Vendor Name'))==norm(local['VENDORNAME']) for r in candidates))
                if not candidates: item['status']='no_exact_line_candidate'
                elif len(pairs)>1:item['status']='ambiguous_identifiers'
                elif not pairs[0][0]:item['status']='matched_line_missing_vlin'
                elif item['legacy_id_shape']=='placeholder_12_nines':item['status']='placeholder_not_identity_link'
                else:item['status']='observed_singleton_vlin_candidate'
            except Exception as e:item.update(status='error',error=type(e).__name__+': '+str(e))
            return item
        with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
            results=list(pool.map(probe,selected))
        output['years'][year]={'resource_id':rid,'schema_url':url,'schema_response_sha256':digest,
          'remote_total_reported':schema['total'],'fields':schema['fields'],
          'local_path':str(path),'local_file_bytes':path.stat().st_size,
          'status_counts':dict(collections.Counter(r['status'] for r in results)), 'probes':results}
    output['historical_filename_check'] = {}
    for filename in ['EV01-ALL-PO-LESS-THAN-9000.csv','EV02-MATCHED-PO-LESS-THAN-9000.csv',
      'EV03-MATCHED-PAYMENT-GREATER-THAN-9000.csv','EV06-MATCHED-PAYMETS-LESS-THAN-9000.csv']:
        path=a.historical_root/filename
        if not path.exists(): continue
        stats=collections.Counter(); maximum=None
        for row in rows(path):
            stats['rows']+=1
            if 'poNIGPCode' in row:
                code=(row.get('poNIGPCode') or '').strip()
                stats['nigp_class_lt900' if code[:3].isdigit() and int(code[:3])<900 else 'other_or_missing_nigp_class']+=1
                qty=dec(row.get('qunatity')); price=dec(row.get('unit_price'))
                amount=qty*price if qty is not None and price is not None else None
            else: amount=dec(row.get('txn_amt'))
            if amount is None: stats['unparseable_amount']+=1; continue
            stats['amount_ge9000']+=amount>=9000
            maximum=amount if maximum is None else max(maximum,amount)
        h=hashlib.sha256()
        with path.open('rb') as f:
            for chunk in iter(lambda:f.read(1024*1024),b''):h.update(chunk)
        output['historical_filename_check'][filename]=dict(stats,maximum_amount=str(maximum),sha256=h.hexdigest())
    if a.spcc_schema:
        url='https://www.datapoint.apa.virginia.gov/historicaldata/FY%202016%20SPCC.zip'
        with urllib.request.urlopen(url, timeout=25) as r:data=r.read(40*1024*1024+1)
        if len(data)>40*1024*1024: raise ValueError('SPCC download exceeds 40 MiB cap')
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            headers={}
            for name in z.namelist():
                if name.lower().endswith('.csv'):
                    with z.open(name) as f:headers[name]=f.readline().decode('utf-8-sig',errors='replace').strip()
        output['spcc_schema']={'url':url,'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),
          'member_headers':headers,'scope':'Headers only; no transaction linkage tested; no source archive saved'}
    print(json.dumps(output, indent=2, sort_keys=True, default=str))

if __name__=='__main__': main()