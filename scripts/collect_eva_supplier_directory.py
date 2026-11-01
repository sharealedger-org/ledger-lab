"""Collect a dated eVA public-main-directory reference snapshot, outside engine timing.
Stdlib only. CSV/text outputs; resumable per-query JSON and gzipped source pages.
No financial logic, no Parquet, no automatic master enrichment.
"""
import argparse
import collections
import concurrent.futures
import csv
import datetime as dt
import gzip
import hashlib
import html
from html.parser import HTMLParser
import http.cookiejar
import json
from pathlib import Path
import re
import string
import threading
import time
import urllib.parse
import urllib.request

BASE = 'https://logi.cgieva.com/External/'
REPORT = 'Public.Reports.Report9001_Data'
SEARCH = BASE + 'rdPage.aspx?rdReport=' + REPORT
AJAX = BASE + 'rdTemplate/rdAjax/rdAjax.aspx'
# Literal printable characters. Whitespace and Unicode coverage are tracked separately.
ALPHABET = string.ascii_uppercase + string.digits + string.punctuation + ' '
FIELDS = {'STATUS':'status','REG_TYPE':'registration_type','HQ':'headquarters_flag',
 'VEND_CUST_CD':'eva_supplier_id','LGL_NM':'supplier_name','LOC_NM':'location_name',
 'STR_1_NM':'address_line_1','CITY_NM':'city','ST':'state','ZIP':'postal_code',
 'SWAMTYPE':'swam_display','P_CARD':'accepts_charge_card','VISA':'accepts_visa',
 'E_ORDER':'accepts_electronic_orders'}
CSV_FIELDS = ['observation_id'] + list(FIELDS.values()) + ['quality_flags','detail_url','first_query','first_page','observed_at_utc','source_sha256']
local = threading.local()
rate_lock = threading.Lock()
last_request = 0.0

def now(): return dt.datetime.now(dt.timezone.utc).isoformat()
def clean(value): return re.sub(r'\s+', ' ', html.unescape(re.sub('<[^>]*>', '', value))).strip()
def digest(data): return hashlib.sha256(data).hexdigest()
def query_key(prefix): return hashlib.sha256(prefix.encode()).hexdigest()[:20]

class Form(HTMLParser):
    def __init__(self): super().__init__(); self.values={}
    def handle_starttag(self, tag, attrs):
        a=dict(attrs)
        if tag.lower()=='input' and a.get('name'):
            kind=a.get('type','').lower()
            if kind not in ('checkbox','radio') or 'checked' in a:
                self.values[a['name']]=a.get('value','')

def parse_page(source):
    found=[]
    for ordinal, body in re.findall(r'<TR\b[^>]*\bRow="(\d+)"[^>]*>(.*?)</TR>', source, re.S|re.I):
        if 'colVEND_CUST_CD_Row' not in body: continue
        values={label.upper():clean(value) for label,value in re.findall(r'<SPAN\b[^>]*\bid="lbl([A-Z0-9_]+)_Row[0-9]+"[^>]*>(.*?)</SPAN>',body,re.S|re.I)}
        record={name:values.get(label,'') for label,name in FIELDS.items()}
        record['quality_flags']='missing_supplier_id' if not record['eva_supplier_id'] else ''
        if not record['supplier_name'] and not record['location_name']: raise ValueError('Unrecognized empty vendor row')
        record['detail_url']=BASE+'rdPage.aspx?'+urllib.parse.urlencode({'rdReport':'Public.Reports.Report9001_VendorDetail','lnkVendCustCd':record['eva_supplier_id'],'rdAgReset':'True'})
        if not record['eva_supplier_id']:record['detail_url']=''
        found.append(record)
    page_counts=[int(x) for x in re.findall(r'id="dtAnalysisGrid-PageOfPages"[^>]*>\s*(\d+)\s*<',source,re.I)]
    pages=max(page_counts,default=1)
    cache=re.search(r'rdDataCache="([^"]+)"',source)
    # Table header is absent for genuine empty searches; reject unexpected errors.
    if not found and any(x in source.lower() for x in ['server error','application error','csrf validation','access denied']):
        raise ValueError('Server returned an error page')
    if not found and 'rdReport' not in source and 'rdAjaxCommand' not in source:
        raise ValueError('Unrecognized response; cannot classify as empty')
    return found,pages,cache.group(1) if cache else None

def request(opener,url,values,delay):
    global last_request
    for attempt in range(4):
        with rate_lock:
            wait=delay-(time.monotonic()-last_request)
            if wait>0: time.sleep(wait)
            last_request=time.monotonic()
        try:
            req=urllib.request.Request(url,data=urllib.parse.urlencode(values).encode() if values is not None else None,
                headers={'User-Agent':'LedgerLabResearch/1.0 (public directory reference snapshot)','Referer':SEARCH})
            with opener.open(req,timeout=45) as response: return response.read()
        except Exception:
            if attempt==3: raise
            time.sleep(2**attempt)

def session(delay):
    if not hasattr(local,'opener'):
        local.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        data=request(local.opener,SEARCH,None,delay)
        form=Form();form.feed(data.decode('utf-8',errors='replace'))
        local.fields=form.values
    return local.opener,dict(local.fields)

def identity(record):
    # Full observation fingerprint, deliberately not supplier-ID-only deduplication.
    return digest(json.dumps({k:record.get(k,'') for k in FIELDS.values()},sort_keys=True,ensure_ascii=False).encode())

def collect_prefix(prefix,root,delay,max_depth,page_workers=1,max_leaf_pages=10):
    key=query_key(prefix); cached=root/'queries'/f'{key}.json'
    if cached.exists():
        result=json.loads(cached.read_text(encoding='utf-8'))
        if result.get('status')!='error': return result
    result={'prefix':prefix,'started_at_utc':now(),'pages':[],'rows':[], 'children':[]}
    try:
        opener,fields=session(delay)
        fields.update(txtSearch=prefix,txtSearchSingleQuoteEscape=prefix.replace("'","''"),chkStartWith='1',
          selSwamBusType='ALL',selRegType='ALL',txtNIGPCode='',txtZipCode='',LglNmStartWith='',LglNmStartWithOthers='',Alpha='')
        # Paging fields from previous forms must not affect a fresh query.
        fields.pop('dtAnalysisGrid-PageNr',None)
        data=request(opener,SEARCH,fields,delay)
        text=data.decode('utf-8',errors='replace')
        form=Form();form.feed(text)
        if form.values.get('rdCSRFKey'):local.fields['rdCSRFKey']=form.values['rdCSRFKey']
        first,pages,cache=parse_page(text)
        result['reported_pages']=pages
        if pages>10000: raise ValueError('Implausible page count; inspect source response')
        seen_pages=set()
        first_data=data
        old_first=root/'source_pages'/f'{key}_01.html.gz'
        reusable=False
        if old_first.exists():
            old_rows,old_pages,_=parse_page(gzip.decompress(old_first.read_bytes()).decode('utf-8',errors='replace'))
            reusable=old_pages==pages and [identity(r) for r in old_rows]==[identity(r) for r in first]
        def fetch_page(page):
            old=root/'source_pages'/f'{key}_{page:02d}.html.gz'
            if reusable and old.exists():
                data=gzip.decompress(old.read_bytes())
                records,_,_=parse_page(data.decode('utf-8',errors='replace'))
                stamp=dt.datetime.fromtimestamp(old.stat().st_mtime,dt.timezone.utc).isoformat()
                return page,data,records,stamp
            if page==1:data=first_data;records=first
            else:
                if not cache:raise ValueError('Missing pagination cache token')
                paging=dict(fields)
                paging.update({'rdReport':REPORT,'dtAnalysisGrid-PageNr':str(page),'rdDataCache':cache,
                  'rdNewPageNr':'True1','rdAjaxCommand':'RefreshElement','rdDataTablePaging':'True',
                  'rdRefreshElementID':'dtAnalysisGrid','rdRequestForwarding':'Form'})
                data=request(opener,AJAX,paging,delay)
                records,_,_=parse_page(data.decode('utf-8',errors='replace'))
            with gzip.open(old,'wb') as f:f.write(data)
            return page,data,records,now()
        pages_to_fetch=1 if pages>max_leaf_pages else pages
        def page_results():
            if page_workers==1:
                for page in range(1,pages_to_fetch+1):yield fetch_page(page)
            else:
                with concurrent.futures.ThreadPoolExecutor(max_workers=page_workers) as page_pool:
                    yield from page_pool.map(fetch_page,range(1,pages_to_fetch+1))
        for page,data,records,stamp in page_results():
            if not records and page>1: raise ValueError('Empty continuation page')
            signatures=tuple(identity(r) for r in records)
            if signatures and signatures in seen_pages: raise ValueError('Repeated page: pagination may be ignored')
            seen_pages.add(signatures)
            raw_name=f'{key}_{page:02d}.html.gz'
            source_hash=digest(data)
            result['pages'].append({'page':page,'rows':len(records),'sha256':source_hash,'source_file':'source_pages/'+raw_name,'retrieved_at_utc':stamp})
            for record in records:
                record.update(observation_id=identity(record),first_query=prefix,first_page=page,
                  observed_at_utc=stamp,source_sha256=source_hash)
                result['rows'].append(record)
            if page%50==0:print(json.dumps({'prefix':prefix,'page':page,'total_pages':pages,'observations':len(result['rows'])}),flush=True)
        result['row_count']=len(result['rows'])
        # Exact cap is ambiguous: split rather than assume there are exactly 1000.
        capped=pages>max_leaf_pages or (pages==10 and len(result['rows'])==1000)
        if capped:
            # Search behavior is validated before recursively multiplying requests.
            literal=[r for r in result['rows'] if any(r[k].upper().startswith(prefix.upper()) for k in ['supplier_name','location_name','eva_supplier_id'])]
            if not literal:
                result['status']='unresolved_nonliteral_search'
            elif len(prefix)>=max_depth:
                result['status']='unresolved_max_depth'
            else:
                extra=set()
                for r in literal:
                    for field in ['supplier_name','location_name','eva_supplier_id']:
                        value=r[field].upper()
                        if value.startswith(prefix.upper()) and len(value)>len(prefix):extra.add(value[len(prefix)])
                result['children']=[prefix+c for c in sorted(set(ALPHABET)|extra)]
                result['status']='split_capped'
        else:result['status']='collected'
        result['finished_at_utc']=now()
    except Exception as e:
        result.update(status='error',error=type(e).__name__+': '+str(e),finished_at_utc=now())
        # Next attempt should establish a fresh server session.
        if hasattr(local,'opener'):del local.opener
    tmp=cached.with_suffix('.tmp');tmp.write_text(json.dumps(result,ensure_ascii=False),encoding='utf-8');tmp.replace(cached)
    return result

def export(root,results,initial):
    records={}; conflicts=collections.defaultdict(set)
    for result in sorted(results.values(),key=lambda x:x['prefix']):
        for r in result.get('rows',[]):
            records.setdefault(r['observation_id'],r)
            conflicts[r['eva_supplier_id']].add(r['observation_id'])
    current_query_ids=set(records)
    prior_csv=root/'suppliers.csv'
    if prior_csv.exists():
        with prior_csv.open(newline='',encoding='utf-8') as f:
            for row in csv.DictReader(f):
                if row.get('observation_id'):records.setdefault(row['observation_id'],row)
    conflicts=collections.defaultdict(set)
    for record in records.values():conflicts[record['eva_supplier_id']].add(record['observation_id'])
    if not records and (root/'suppliers.csv').exists() and (root/'suppliers.csv').stat().st_size>1000:
        raise RuntimeError('No observations available; existing nonempty CSV and manifest retained. Inspect query checkpoints or run assemble_supplier_snapshot.py.')
    with (root/'suppliers.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=CSV_FIELDS);writer.writeheader()
        writer.writerows(sorted(records.values(),key=lambda r:(r['eva_supplier_id'],r['location_name'],r['address_line_1'],r['observation_id'])))
    with (root/'queries.csv').open('w',newline='',encoding='utf-8') as f:
        writer=csv.DictWriter(f,fieldnames=['prefix','status','reported_pages','row_count','error']);writer.writeheader()
        writer.writerows({k:r.get(k,'') for k in writer.fieldnames} for r in sorted(results.values(),key=lambda x:x['prefix']))
    unresolved=[{'prefix':r['prefix'],'status':r['status'],'error':r.get('error')} for r in results.values() if r['status'].startswith('unresolved') or r['status']=='error']
    pending=sorted((set(initial)|{c for r in results.values() for c in r.get('children',[])})-set(results))
    statuses=collections.Counter(r['status'] for r in results.values())
    manifest={'snapshot_updated_at_utc':now(),'source_url':SEARCH,'scope':'Public main supplier directory, ALL registration/SWaM values; inactive directory is a separate report not included',
      'format':'CSV with UTF-8; identifiers are strings; no Parquet','query_status_counts':dict(statuses),
      'query_count':len(results),'unique_observations':len(records),'distinct_supplier_ids':sum(bool(k) for k in conflicts),'observations_missing_supplier_id':sum(not r['eva_supplier_id'] for r in records.values()),
      'supplier_ids_with_multiple_observations':sum(bool(k) and len(v)>1 for k,v in conflicts.items()),
      'raw_observation_count':sum(len(r.get('rows',[])) for r in results.values()),'retained_prior_observations':len(set(records)-current_query_ids),
      'pending_prefixes':pending,'unresolved_queries':unresolved,
      'execution_complete':not pending and not unresolved,
      'global_directory_completeness':'not independently established: source-reported pagination checked, but no independent census count; source may change during the acquisition interval',
      'initial_prefixes':initial,'child_alphabet':ALPHABET,
      'deduplication':'Exact normalized full list-row observation fingerprint; never supplier-ID-only',
      'vlin':'Not exposed in list rows; detail enrichment is a separate step',
      'historical_applicability':'Observation times are retrieval times; not historical effective dates',
      'suppliers_csv_sha256':digest((root/'suppliers.csv').read_bytes())}
    (root/'manifest.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False),encoding='utf-8')
    return manifest

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--out',type=Path,required=True)
    p.add_argument('--prefix',action='append',help='Repeat for a focused run; default printable ASCII roots')
    p.add_argument('--workers',type=int,default=3)
    p.add_argument('--page-workers',type=int,default=1)
    p.add_argument('--delay',type=float,default=.5,help='Minimum global interval between HTTP requests')
    p.add_argument('--max-leaf-pages',type=int,default=10,help='Split larger results before downloading all pages')
    p.add_argument('--max-depth',type=int,default=8)
    p.add_argument('--max-queries',type=int,default=10000)
    a=p.parse_args()
    if not 1<=a.workers<=3 or not 1<=a.page_workers<=3 or a.workers*a.page_workers>3 or a.delay<.25:p.error('workers must be 1..3 and delay >= .25')
    a.out.mkdir(parents=True,exist_ok=True)
    for folder in ['queries','source_pages']:(a.out/folder).mkdir(exist_ok=True)
    initial=a.prefix or list(ALPHABET.strip())
    # Resume work from prior query checkpoints, retaining all observed rows.
    results={}
    for path in (a.out/'queries').glob('*.json'):
        r=json.loads(path.read_text(encoding='utf-8'))
        if a.prefix and not any(r['prefix'].startswith(x) for x in a.prefix):continue
        if r.get('status')!='error':results[r['prefix']]=r
    todo=set(initial)|{c for r in results.values() for c in r.get('children',[])}
    todo-=set(results); in_flight={}; completed=0
    with concurrent.futures.ThreadPoolExecutor(max_workers=a.workers) as pool:
        while todo or in_flight:
            while todo and len(in_flight)<a.workers and completed+len(in_flight)<a.max_queries:
                prefix=min(todo);todo.remove(prefix)
                in_flight[pool.submit(collect_prefix,prefix,a.out,a.delay,a.max_depth,a.page_workers,a.max_leaf_pages)]=prefix
            if not in_flight:break
            done,_=concurrent.futures.wait(in_flight,return_when=concurrent.futures.FIRST_COMPLETED)
            for future in done:
                prefix=in_flight.pop(future);r=future.result();results[prefix]=r;completed+=1
                todo.update(set(r.get('children',[]))-set(results)-set(in_flight.values()))
                print(json.dumps({'prefix':prefix,'status':r['status'],'rows':r.get('row_count',0),'queries':len(results),'pending':len(todo)},ensure_ascii=True),flush=True)
            if completed%20==0:export(a.out,results,initial)
    manifest=export(a.out,results,initial)
    print(json.dumps({'output':str(a.out.resolve()),'summary':manifest},ensure_ascii=True),flush=True)
    return 0 if manifest['execution_complete'] else 2

if __name__=='__main__':raise SystemExit(main())