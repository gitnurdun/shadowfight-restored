"""Minimal reproducible extraction for the native restoration pipeline."""
import hashlib,json,re,resource
from pathlib import Path
import UnityPy
resource.setrlimit(resource.RLIMIT_NOFILE,(8192,8192))
root=Path(__file__).resolve().parents[1];out=root/'extracted/TextAsset';out.mkdir(parents=True,exist_ok=True);records=[]
for obj in UnityPy.load(str(root/'work/raw/assets/bin/Data')).objects:
 if obj.type.name!='TextAsset':continue
 data=obj.read();name=re.sub(r'[^\w. -]','_',data.m_Name)[:140] or str(obj.path_id)
 raw=data.m_Script.encode('utf-8','surrogateescape') if isinstance(data.m_Script,str) else bytes(data.m_Script)
 digest=hashlib.sha256(raw).hexdigest();p=out/(name+'.bytes')
 if p.exists() and p.read_bytes()!=raw:p=out/(name+'__'+digest[:16]+'.bytes')
 p.write_bytes(raw);records.append({'name':name,'output':p.name,'asset_file':obj.assets_file.name,'path_id':obj.path_id,'bytes':len(raw),'sha256':digest})
(root/'reports/text-asset-extraction.json').write_text(json.dumps(records,indent=2));print('TextAssets exported:',len(records))
