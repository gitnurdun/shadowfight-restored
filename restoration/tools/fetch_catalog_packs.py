"""Fetch and verify public asset bundles named by the running client's catalog."""
import hashlib,json,urllib.request,urllib.parse,sys,shutil,gc
from pathlib import Path
from xml.etree import ElementTree as E
root=Path(__file__).resolve().parents[1]
from verify_catalog_hashes import bundle_hash,bundle_salt
salt=bundle_salt()
catalog=E.parse(root/'work/runtime/live_config_cdn.xml').getroot()
version=lambda s:tuple(int(x) for x in s.split('.'))
current=version('2.46.0');out=root/'work/catalog-bundles';out.mkdir(exist_ok=True)
report_path=root/'reports/catalog-downloads-v2.json'
prior=json.loads(report_path.read_text()) if report_path.exists() else []
results=[]
requested=sys.argv[1:] or ['ZONE_1','MODELS']
for name in requested:
 candidates=[]
 for e in catalog.iter():
  a=e.attrib;url=a.get('Url','');parsed=urllib.parse.urlparse(url)
  if a.get('Name')!=name or '2' not in a.get('PlatformID','').split(','):continue
  if parsed.scheme!='https' or parsed.hostname!='assets.nekki.com' or '/shadowfight/unity_packs/' not in parsed.path:continue
  if version(a.get('MinVersion','0'))>current:continue
  if a.get('MaxVersion') and current>=version(a['MaxVersion']):continue
  candidates.append(a)
 if not candidates:results.append({'name':name,'error':'No matching catalog entry'});continue
 a=max(candidates,key=lambda x:version(x.get('MinVersion','0')));record={'name':name,'catalog':a}
 try:
  dest=out/name;assert dest.resolve().is_relative_to(out.resolve());dest.parent.mkdir(parents=True,exist_ok=True);part=dest.with_suffix('.partial')
  cached=root/'work/runtime/bundles'/name
  if not part.exists() and not dest.exists() and cached.exists() and cached.stat().st_size==int(a['Size']):shutil.copyfile(cached,part)
  existing=dest if dest.exists() else part
  if not existing.exists() or existing.stat().st_size!=int(a['Size']):
   with urllib.request.urlopen(a['Url'],timeout=45) as response,part.open('wb') as f:
    while True:
     chunk=response.read(1024*1024)
     if not chunk:break
     f.write(chunk)
   existing=part
  digest=bundle_hash(existing,salt)
  assert digest==a['Hash'],('catalog hash mismatch',digest,a['Hash'])
  raw=existing.read_bytes();count=len(raw)
  assert count==int(a['Size']),('size mismatch',count,a['Size'])
  assert raw.startswith(b'UnityFS\0'),'Unexpected bundle header'
  import UnityPy
  env=UnityPy.load(raw);objects=len(list(env.objects));del env;gc.collect()
  if existing!=dest:existing.replace(dest)
  record.update(bytes=count,sha256=hashlib.sha256(raw).hexdigest(),file_md5=hashlib.md5(raw).hexdigest(),catalog_hash_verified=True,catalog_hash_algorithm='salted double MD5 with uppercase hexadecimal intermediate',catalog_hash=digest,size_valid=True,unityfs_parse_valid=True,objects=objects)
  del raw;gc.collect()
 except Exception as e:record['error']=str(e)
 results.append(record)
 merged={r['name']:r for r in prior}
 merged.update({r['name']:r for r in results})
 report_path.write_text(json.dumps(list(merged.values()),indent=2));print(json.dumps(record),flush=True)


