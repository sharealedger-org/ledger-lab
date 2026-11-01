"""Reconcile the full public CSV export against richer HTML observations.
The source CSV Status column maps to registration_type, never active status.
"""
import argparse
import collections
import csv
import datetime as dt
import gzip
import hashlib
import json
from pathlib import Path
import re
from collect_eva_supplier_directory import FIELDS,CSV_FIELDS,SEARCH,session,request,parse_page,identity,query_key

MAP={'Status':'registration_type','HQ':'headquarters_flag','VendorID':'eva_supplier_id','eMall Supplier Name':'supplier_name',
'eMall Contact':'location_name','Address 1':'address_line_1','City':'city','St':'state','Zip':'postal_code','SWAM Type':'swam_display',
'Accept Charge Card':'accepts_charge_card','Accepts Visa':'accepts_visa','Accepts Elect Orders':'accepts_electronic_orders'}
KNOWN=[name for name in FIELDS.values() if name!='status']
def normalize(value):return re.sub(r'\s+',' ',value or '').strip()
def key(row):return tuple(row.get(k,'') for k in KNOWN)
def sha(path):
 h=hashlib.sha256()
 with path.open('rb') as f:
  for data in iter(lambda:f.read(1024*1024),b''):h.update(data)
 return h.hexdigest()

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--snapshot',type=Path,required=True);p.add_argument('--max-lookups',type=int,default=100);a=p.parse_args()
 root=a.snapshot;manifest=json.loads((root/'manifest.json').read_text(encoding='utf-8'))
 source_meta=json.loads((root/'source_export/manifest.json').read_text(encoding='utf-8'))
 raw=root/'source_export/suppliers_source.csv'
 if sha(raw)!=source_meta['sha256'] or source_meta['rows_with_invalid_arity']:raise ValueError('Source export failed checksum/arity control')
 with (root/'suppliers.csv').open(encoding='utf-8',newline='') as f:records={r['observation_id']:r for r in csv.DictReader(f)}
 available={key(r) for r in records.values()};pending=[];source_keys=set();count=matched=0
 with raw.open(encoding=source_meta['encoding'],newline='') as f:
  for ordinal,row in enumerate(csv.DictReader(f),2):
   values={target:normalize(row[source]) for source,target in MAP.items()}
   if values['registration_type'] not in ('Self','State',''):raise ValueError('Source Status semantics changed; inspect before mapping')
   count+=1;source_keys.add(key(values))
   if key(values) in available:matched+=1
   else:pending.append((ordinal,values))
 if len(pending)>a.max_lookups:raise ValueError('Too many unmatched rows; inspect mapping before live lookups: '+str(len(pending)))
 lookups=[];unresolved=[]
 for ordinal,wanted in pending:
  label='lookup:'+ (wanted['eva_supplier_id'] or wanted['supplier_name']);name=query_key(label)+'_01.html.gz'
  evidence={'source_csv_row':ordinal,'eva_supplier_id':wanted['eva_supplier_id'],'query':label}
  try:
   opener,fields=session(.5);search=wanted['eva_supplier_id'] or wanted['supplier_name']
   fields.update(txtSearch=search,txtSearchSingleQuoteEscape=search.replace("'","''"),chkStartWith='0',selSwamBusType='ALL',selRegType='ALL',txtNIGPCode='',txtZipCode='')
   fields.pop('dtAnalysisGrid-PageNr',None)
   data=request(opener,SEARCH,fields,.5);candidates,pages,_=parse_page(data.decode('utf-8',errors='replace'))
   with gzip.open(root/'source_pages'/name,'wb') as f:f.write(data)
   matches=[r for r in candidates if key(r)==key(wanted)]
   evidence.update(returned_page_count=pages,returned_rows=len(candidates),matching_rows=len(matches),source_sha256=hashlib.sha256(data).hexdigest(),source_file='source_pages/'+name)
   if not matches:raise ValueError('Target not found on returned lookup page')
   stamp=dt.datetime.now(dt.timezone.utc).isoformat()
   for row in matches:
    row.update(observation_id=identity(row),first_query=label,first_page=1,observed_at_utc=stamp,source_sha256=evidence['source_sha256'])
    records.setdefault(row['observation_id'],row);available.add(key(row))
   evidence['status']='matched_html_observation'
  except Exception as e:
   evidence.update(status='unresolved',error=type(e).__name__+': '+str(e));unresolved.append(evidence)
  lookups.append(evidence)
 if unresolved:
  (root/'source_export/reconciliation.json').write_text(json.dumps({'initially_matched':matched,'lookups':lookups,'unresolved':unresolved},indent=2,ensure_ascii=False),encoding='utf-8')
  raise ValueError('Unresolved targeted lookups; existing canonical CSV retained')
 covered=len(source_keys & available)
 if count!=source_meta['rows'] or covered!=len(source_keys):raise ValueError('Export coverage control failed')
 temp=root/'suppliers.csv.tmp'
 with temp.open('w',newline='',encoding='utf-8') as f:
  writer=csv.DictWriter(f,fieldnames=CSV_FIELDS);writer.writeheader();writer.writerows(sorted(records.values(),key=lambda r:(r['eva_supplier_id'],r['location_name'],r['address_line_1'],r['observation_id'])))
 temp.replace(root/'suppliers.csv')
 comparison={'source_export_sha256':source_meta['sha256'],'source_rows':count,'unique_source_known_field_keys':len(source_keys),
 'initially_matched_on_all_export_fields':matched,'targeted_lookup_rows':len(pending),'lookups':lookups,
 'covered_source_known_field_keys':covered,'all_export_rows_represented':covered==len(source_keys),'unresolved':[],
 'status_semantics':'CSV Status is registration_type; actual vendor status remains sourced from HTML observations',
 'canonical_csv_sha256':sha(root/'suppliers.csv')}
 (root/'source_export/reconciliation.json').write_text(json.dumps(comparison,indent=2,ensure_ascii=False),encoding='utf-8')
 manifest['incomplete_redundant_html_queries']=manifest.get('unresolved_queries',[])
 manifest.update(execution_complete=True,acquisition_complete=True,pagination_complete=False,pending_prefixes=[],unresolved_queries=[],
  completion_basis='All rows in recorded public CSV export reconciled to richer HTML observations; full pagination of redundant broad queries was unnecessary',
  global_directory_completeness='All rows of the recorded public report CSV export are represented. Coverage of the separate inactive directory and all Commonwealth payees is not established.',
  unique_observations=len(records),distinct_supplier_ids=len({r['eva_supplier_id'] for r in records.values() if r['eva_supplier_id']}),
  observations_missing_supplier_id=sum(not r['eva_supplier_id'] for r in records.values()),
  snapshot_updated_at_utc=dt.datetime.now(dt.timezone.utc).isoformat(),suppliers_csv_sha256=comparison['canonical_csv_sha256'],
  source_export=comparison,source_export_manifest='source_export/manifest.json')
 temp=root/'manifest.json.tmp';temp.write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf-8');temp.replace(root/'manifest.json')
 print(json.dumps({k:manifest[k] for k in ['unique_observations','distinct_supplier_ids','observations_missing_supplier_id','acquisition_complete','completion_basis']},ensure_ascii=True))
 print(json.dumps(comparison,ensure_ascii=True))

if __name__=='__main__':main()