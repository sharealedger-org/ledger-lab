"""Verify saved directory CSV counts/fingerprints against retained source evidence.
Read-only; run after acquisition/assembly finishes, not during a CSV publication.
"""
import argparse
import collections
import csv
import gzip
import hashlib
import json
import re
from pathlib import Path
from collect_eva_supplier_directory import identity,parse_page

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--snapshot',type=Path,required=True);a=p.parse_args()
    manifest=json.loads((a.snapshot/'manifest.json').read_text(encoding='utf-8'))
    hashes=set();page_errors=[];page_count=0
    for path in (a.snapshot/'source_pages').glob('*.html.gz'):
        try:
            data=gzip.decompress(path.read_bytes());hashes.add(hashlib.sha256(data).hexdigest());page_count+=1
            _,reported,_=parse_page(data.decode('utf-8',errors='replace'))
            ordinal=int(path.name.removesuffix('.html.gz').rsplit('_',1)[1])
            if ordinal>reported:page_errors.append(str(path.name)+': saved ordinal exceeds response page count')
            tag=re.search(r'<INPUT\b[^>]*\bNAME="dtAnalysisGrid-PageNr"[^>]*>',data.decode('utf-8',errors='replace'),re.I)
            value=re.search(r'\bVALUE="([0-9]+)"',tag.group(0),re.I) if tag else None
            if value and int(value.group(1))!=ordinal:page_errors.append(path.name+': returned page number differs from saved ordinal')
        except Exception as e:page_errors.append(path.name+': '+str(e))
    count=missing_ids=0;ids=set();observations=set();failures=[];states=collections.Counter();times=[]
    with (a.snapshot/'suppliers.csv').open(newline='',encoding='utf-8') as f:
        for r in csv.DictReader(f):
            count+=1
            if r['observation_id'] in observations:failures.append('duplicate observation fingerprint')
            observations.add(r['observation_id'])
            if identity(r)!=r['observation_id']:failures.append('observation fingerprint mismatch')
            if r['source_sha256'] not in hashes:failures.append('missing raw source hash')
            if r['eva_supplier_id']:ids.add(r['eva_supplier_id'])
            else:
                missing_ids+=1
                if r.get('quality_flags')!='missing_supplier_id':failures.append('missing supplier ID lacks quality flag')
            states[r['status']]+=1
            if r.get('observed_at_utc'):times.append(r['observed_at_utc'])
    actual=hashlib.sha256((a.snapshot/'suppliers.csv').read_bytes()).hexdigest()
    if actual!=manifest['suppliers_csv_sha256']:failures.append('CSV checksum differs from manifest')
    if count!=manifest['unique_observations']:failures.append('CSV observation count differs from manifest')
    if len(ids)!=manifest['distinct_supplier_ids']:failures.append('distinct supplier count differs from manifest')
    if missing_ids!=manifest.get('observations_missing_supplier_id',0):failures.append('missing-ID count differs from manifest')
    result={'data_integrity_verified':not failures and not page_errors,'observations':count,'distinct_nonblank_supplier_ids':len(ids),
      'observations_missing_supplier_id':missing_ids,'retained_source_pages':page_count,'row_status_counts':dict(states),
      'source_observation_time_range_utc':[min(times),max(times)] if times else [],
      'requested_queries_complete':manifest['execution_complete'],'global_completeness':manifest['global_directory_completeness'],
      'integrity_failures':dict(collections.Counter(failures)),'source_page_errors':page_errors}
    print(json.dumps(result,indent=2))
    return 0 if result['data_integrity_verified'] else 1

if __name__=='__main__':raise SystemExit(main())