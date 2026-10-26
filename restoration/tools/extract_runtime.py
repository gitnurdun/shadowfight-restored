"""Inventory and export TextAssets from bundles captured from the test emulator."""
import collections,hashlib,json,re
from pathlib import Path
import UnityPy
root=Path(__file__).resolve().parents[1]
out=root/'extracted-runtime/TextAsset';out.mkdir(parents=True,exist_ok=True)
report=[]
for p in sorted((root/'work/runtime/bundles').iterdir()):
 raw=p.read_bytes();entry={'name':p.name,'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()}
 env=UnityPy.load(str(p));counts=collections.Counter();assets=[]
 for o in env.objects:
  counts[o.type.name]+=1
  if o.type.name!='TextAsset':continue
  d=o.read();name=re.sub(r'[^\w. -]','_',d.m_Name)
  raw=d.m_Script.encode('utf8','surrogateescape') if isinstance(d.m_Script,str) else bytes(d.m_Script)
  (out/(name+'.bytes')).write_bytes(raw)
  assets.append({'name':d.m_Name,'file':name+'.bytes','bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()})
 entry.update(types=dict(counts),assets=assets);report.append(entry)
(root/'reports/runtime-bundles.json').write_text(json.dumps(report,indent=2))
print([(e['name'],e['types']) for e in report])
