"""Rebuild an explicitly qualified CSV snapshot from preserved public source pages.
Useful after interrupted acquisition; never treats missing pages as completeness.
"""
import argparse
import collections
import datetime as dt
import gzip
import json
from pathlib import Path
from collect_eva_supplier_directory import parse_page,identity,digest,export,query_key,ALPHABET

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    keys={}
    for path in (a.out/'queries').glob('*.json'):
        record=json.loads(path.read_text(encoding='utf-8'));keys[path.stem]=record['prefix']
    for prefix in ALPHABET:keys.setdefault(query_key(prefix),prefix)
    groups=collections.defaultdict(list)
    for path in (a.out/'source_pages').glob('*.html.gz'):
        key,page=path.name.removesuffix('.html.gz').rsplit('_',1);groups[key].append((int(page),path))
    results={};inventory=[]
    for key,paths in groups.items():
        prefix=keys.get(key,'source-query-'+key)
        result={'prefix':prefix,'rows':[],'pages':[],'children':[]};reported=set();seen=set();issues=[]
        for page,path in sorted(paths):
            try:
                data=gzip.decompress(path.read_bytes());records,pages,_=parse_page(data.decode('utf-8',errors='replace'))
                reported.add(pages);signature=tuple(identity(r) for r in records)
                if signature and signature in seen:issues.append('duplicate_page_'+str(page))
                seen.add(signature)
                stamp=dt.datetime.fromtimestamp(path.stat().st_mtime,dt.timezone.utc).isoformat();h=digest(data)
                for row in records:
                    row.update(observation_id=identity(row),first_query=prefix,first_page=page,observed_at_utc=stamp,source_sha256=h)
                    result['rows'].append(row)
                result['pages'].append({'page':page,'rows':len(records),'sha256':h,'source_file':str(path.relative_to(a.out))})
            except Exception as e:issues.append('page_'+str(page)+': '+type(e).__name__+': '+str(e))
        expected=max(reported,default=0);actual={x['page'] for x in result['pages']}
        missing=sorted(set(range(1,expected+1))-actual)
        if len(reported)>1:issues.append('source_reported_page_count_changed')
        result.update(reported_pages=expected,row_count=len(result['rows']),status='collected' if not missing and not issues else 'unresolved_missing_or_invalid_pages')
        if missing or issues:result['error']=str(len(missing))+' missing pages; '+'; '.join(issues)
        results[prefix]=result
        inventory.append({'prefix':prefix,'saved_pages':len(actual),'reported_pages':expected,'missing_page_count':len(missing),'issues':issues})
    manifest=export(a.out,results,list(results))
    manifest.update(assembly='Reconstructed from retained source-page files; acquisition may be partial',
      source_page_inventory=inventory,source_time_note='Source file modification time used where original query timestamp checkpoint was unavailable',
      requested_full_directory_acquisition='not established by this offline assembly')
    (a.out/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf-8')
    print(json.dumps({k:manifest[k] for k in ['unique_observations','distinct_supplier_ids','observations_missing_supplier_id','execution_complete','unresolved_queries']},ensure_ascii=True))

if __name__=='__main__':main()