"""Fetch, validate and package compatible catalog cinematics used by boss gates."""
import hashlib,json,shutil,urllib.parse,urllib.request
from pathlib import Path
from xml.etree import ElementTree as E
from verify_catalog_hashes import bundle_hash,bundle_salt
root=Path(__file__).resolve().parents[1]
catalog=E.parse(root/'work/runtime/live_config_cdn.xml').getroot()
version=lambda s:tuple(map(int,s.split('.')))
selected={}
for e in catalog.iter():
 a=e.attrib;url=urllib.parse.urlparse(a.get('Url',''))
 if not a.get('Name','').lower().endswith('.mp4'):continue
 if '2' not in a.get('PlatformID','').split(','):continue
 if version(a.get('MinVersion','0'))>version('2.46.0'):continue
 if a.get('MaxVersion') and version('2.46.0')>=version(a['MaxVersion']):continue
 if url.scheme!='https' or url.hostname!='assets.nekki.com' or not url.path.startswith('/shadowfight/video/'):continue
 old=selected.get(a['Name'])
 if old is None or version(a.get('MinVersion','0'))>version(old.get('MinVersion','0')):selected[a['Name']]=a
assert set(selected)=={'shadow_gate.mp4','Shadow_fight_ending.mp4'},selected.keys()
directory=root/'work/catalog-videos';directory.mkdir(parents=True,exist_ok=True)
dest=root/'original-revival-edit/assets/revival';index_path=dest/'index.json'
index={e['path']:e for e in json.loads(index_path.read_text())};records=[]
for name,a in sorted(selected.items()):
 source=directory/name;assert source.resolve().is_relative_to(directory.resolve())
 if not source.exists():
  part=source.with_suffix('.partial')
  with urllib.request.urlopen(a['Url'],timeout=45) as response,part.open('wb') as out:shutil.copyfileobj(response,out)
  part.replace(source)
 raw=source.read_bytes();assert len(raw)==int(a['Size']),(name,len(raw),a['Size'])
 assert b'ftyp' in raw[:64],name
 md5=hashlib.md5(raw).digest()
 variants={'raw MD5 hexadecimal':md5.hex(),'raw MD5 unpadded byte hexadecimal':''.join(format(v,'x') for v in md5),'salted double MD5 uppercase hexadecimal':bundle_hash(source,bundle_salt())}
 matched=[alg for alg,value in variants.items() if value.lower()==a['Hash'].lower()]
 assert matched,(name,variants,a['Hash'])
 relative='gamedata/bundles/'+name;target=dest/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
 index[relative]={'path':relative,'bytes':len(raw)}
 records.append({'name':name,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest(),'catalog':a,'catalog_hash_verified':True,'matching_digest':matched})
index_path.write_text(json.dumps(list(index.values())))
(root/'reports/catalog-videos.json').write_text(json.dumps(records,indent=2))
print(json.dumps(records,indent=2))
