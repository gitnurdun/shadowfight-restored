"""Write complete cache metadata and hashes for the actual bundled bytes."""
import json,hashlib
from pathlib import Path
from xml.etree import ElementTree as E
from verify_catalog_hashes import bundle_hash,bundle_salt
root=Path(__file__).resolve().parents[1];dest=root/'original-revival-edit/assets/revival'
catalog=json.loads((root/'reports/catalog-downloads-v2.json').read_text());packs=E.Element('Packs');salt=bundle_salt();changes=[]
videos=root/'reports/catalog-videos.json'
if videos.exists():catalog+=json.loads(videos.read_text())
config=E.parse(root/'work/runtime/live_config_cdn.xml')
for entry in catalog:
    if 'error' in entry:continue
    a=entry['catalog'];name=entry['name'];path=dest/'gamedata/bundles'/name
    digest=a['Hash'] if name.lower().endswith('.mp4') else bundle_hash(path,salt)
    if name.lower().endswith('.mp4'):
        assert hashlib.sha256(path.read_bytes()).hexdigest()==entry['sha256'] and entry['catalog_hash_verified']
    E.SubElement(packs,'Pack',{'Name':name,'Url':a['Url'],'Version':'2.46.0','Attach':a.get('Attach','1'),'Reload':a.get('Reload','1'),'Size':str(path.stat().st_size),'Hash':digest,'Priority':'0'})
    if digest!=a['Hash']:
        changes.append({'name':name,'original_hash':a['Hash'],'modified_hash':digest})
        for element in config.getroot().iter():
            if element.get('Name')==name and element.get('Url')==a['Url']:
                element.set('Hash',digest);element.set('Size',str(path.stat().st_size))
E.ElementTree(packs).write(dest/'gamedata/packs.xml',encoding='utf-8',xml_declaration=True)
config.write(dest/'gamedata/config_cdn.xml',encoding='utf-8',xml_declaration=True)
index_path=dest/'index.json';index=json.loads(index_path.read_text())
for entry in index:
    path=dest/entry['path'];entry['bytes']=path.stat().st_size
    with path.open('rb') as source:entry['sha256']=hashlib.file_digest(source,'sha256').hexdigest()
index_path.write_text(json.dumps(index))
(root/'reports/local-cache-hash-changes.json').write_text(json.dumps(changes,indent=2))
print('Cache entries',len(packs),'modified bundles',changes)
