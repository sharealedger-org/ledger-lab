"""Download the public Logi report's documented CSV representation.
Preserves the source CSV unchanged. Its Status column is registration type.
"""
import argparse
import collections
import csv
import datetime as dt
import hashlib
import json
from pathlib import Path
import urllib.parse
import urllib.request
from collect_eva_supplier_directory import SEARCH,session

HEADERS=['Status','HQ','VendorID','eMall Supplier Name','eMall Contact','Address 1','City','St','Zip','SWAM Type','Accept Charge Card','Accepts Visa','Accepts Elect Orders']

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--out',type=Path,required=True);p.add_argument('--search',default='_');a=p.parse_args()
    a.out.mkdir(parents=True,exist_ok=True)
    opener,fields=session(.5)
    fields.update(txtSearch=a.search,txtSearchSingleQuoteEscape=a.search.replace("'","''"),chkStartWith='1',selSwamBusType='ALL',selRegType='ALL',txtNIGPCode='',txtZipCode='')
    fields.pop('dtAnalysisGrid-PageNr',None)
    url=SEARCH+'&rdReportFormat=CSV&rdExportTableID=dtAnalysisGrid'
    started=dt.datetime.now(dt.timezone.utc).isoformat()
    with opener.open(urllib.request.Request(url,data=urllib.parse.urlencode(fields).encode()),timeout=180) as response:
        kind=response.headers.get('Content-Type','');data=response.read(128*1024*1024+1)
    if len(data)>128*1024*1024:raise ValueError('Source export exceeds 128 MiB acquisition cap')
    if 'csv' not in kind.lower():raise ValueError('Expected CSV response, received '+kind)
    encoding='utf-16' if data.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8-sig'
    import io
    reader=csv.DictReader(io.StringIO(data.decode(encoding),newline=''))
    if reader.fieldnames!=HEADERS:raise ValueError('Unexpected source CSV header: '+str(reader.fieldnames))
    count=invalid=0;ids=set();status=collections.Counter()
    for r in reader:
        count+=1
        if None in r or any(v is None for v in r.values()):invalid+=1
        if r.get('VendorID'):ids.add(r['VendorID'])
        status[r.get('Status','')]+=1
    tmp=a.out/'suppliers_source.csv.tmp';tmp.write_bytes(data);tmp.replace(a.out/'suppliers_source.csv')
    manifest={'source_url':url,'query_search':a.search,'query_starts_with':True,'table_id':'dtAnalysisGrid',
      'started_at_utc':started,'finished_at_utc':dt.datetime.now(dt.timezone.utc).isoformat(),'bytes':len(data),'sha256':hashlib.sha256(data).hexdigest(),
      'encoding':encoding,'headers':HEADERS,'rows':count,'rows_with_invalid_arity':invalid,'distinct_nonblank_supplier_ids':len(ids),'source_status_values':dict(status),
      'status_column_semantics':'Source CSV Status contains registration type Self/State, not vendor active status; do not rename it vendor_status.',
      'scope':'Exact public report export for the recorded search. Not a historical master or a census of every Commonwealth payee.',
      'engine_format_reference':'https://devnet.logianalytics.com/hc/en-us/articles/1500009531621-Query-String-Parameter-Reference'}
    (a.out/'manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print(json.dumps(manifest),flush=True)

if __name__=='__main__':main()