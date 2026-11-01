"""Verify current VLIN for the small, evidenced historical bridge candidate set.
Writes reference evidence only, not an accounting master or payment linkage.
"""
import argparse
import csv
import datetime as dt
import gzip
import hashlib
import json
from pathlib import Path
import re
import time
import urllib.request
from collect_eva_supplier_directory import clean

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--candidates',type=Path,required=True)
    p.add_argument('--out',type=Path,required=True)
    a=p.parse_args();a.out.mkdir(parents=True,exist_ok=True)
    with a.candidates.open(encoding='utf-8',newline='') as f:inputs=list(csv.DictReader(f))
    ids=sorted({r['eva_id'] for r in inputs})
    evidence={}
    for supplier in ids:
        if not re.fullmatch('[A-Za-z0-9]+',supplier):raise ValueError('Unexpected supplier ID')
        url='https://logi.cgieva.com/External/rdPage.aspx?rdReport=Public.Reports.Report9001_VendorDetail&lnkVendCustCd='+supplier+'&rdAgReset=True'
        result={'eva_id':supplier,'detail_url':url,'observed_at_utc':dt.datetime.now(dt.timezone.utc).isoformat()}
        try:
            with urllib.request.urlopen(url,timeout=30) as response:data=response.read()
            source=data.decode('utf-8',errors='replace')
            result['sha256']=hashlib.sha256(data).hexdigest()
            for label,field in [('VLIN_ID','vlin'),('VEND_CUST_CD','returned_eva_id'),('LGL_NM','supplier_name'),('LOC_NM','location_name'),('HQ_LGL_NM','hq_legal_name')]:
                m=re.search(r'<SPAN[^>]*id="lbl'+label+r'_Row1"[^>]*>(.*?)</SPAN>',source,re.I|re.S)
                result[field]=clean(m.group(1)) if m else ''
            result['status']='observed' if result.get('returned_eva_id')==supplier and result.get('vlin') else 'missing_or_unrecognized'
            with gzip.open(a.out/(supplier+'.html.gz'),'wb') as f:f.write(data)
        except Exception as e:result.update(status='error',error=type(e).__name__+': '+str(e))
        evidence[supplier]=result
        print(json.dumps({'eva_id':supplier,'status':result['status'],'vlin':result.get('vlin','')}),flush=True)
        time.sleep(.5)
    fields=['fiscal_year','legacy_vendor_id','eva_id','candidate_vlin','current_vlin','concordance','supplier_name','location_name','hq_legal_name','observed_at_utc','detail_url','sha256']
    with (a.out/'historical_candidate_vlin_verification.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=fields);writer.writeheader()
        for row in inputs:
            e=evidence[row['eva_id']]
            writer.writerow({'fiscal_year':row['fiscal_year'],'legacy_vendor_id':row['legacy_vendor_id'],'eva_id':row['eva_id'],
              'candidate_vlin':row['vlin'],'current_vlin':e.get('vlin',''),
              'concordance':('agrees' if e.get('vlin')==row['vlin'] else 'differs') if e['status']=='observed' else 'unknown',
              **{k:e.get(k,'') for k in ['supplier_name','location_name','hq_legal_name','observed_at_utc','detail_url','sha256']}})
    (a.out/'manifest.json').write_text(json.dumps({'scope':'Current public details for the historical candidate sample only; not historical master truth or a payment join',
      'candidates_sha256':hashlib.sha256(a.candidates.read_bytes()).hexdigest(),'suppliers':evidence},indent=2),encoding='utf-8')

if __name__=='__main__':main()