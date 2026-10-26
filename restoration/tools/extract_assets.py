import bz2, collections, hashlib, json, re, resource
from pathlib import Path
import UnityPy
resource.setrlimit(resource.RLIMIT_NOFILE, (8192,8192))
root=Path(__file__).resolve().parents[1]
out=root/'extracted'; out.mkdir(exist_ok=True)
env=UnityPy.load(str(root/'work/raw/assets/bin/Data'))
counts=collections.Counter(); errors=[]; index=[]
for o in env.objects:
    typ=o.type.name
    if typ not in ('TextAsset','Texture2D','AudioClip','Mesh','AnimationClip','Font'): continue
    try:
        d=o.read(); name=re.sub(r'[^\w. -]','_',d.m_Name)[:140] or str(o.path_id)
        folder=out/typ; folder.mkdir(exist_ok=True)
        stem=folder/name
        if typ=='TextAsset':
            data=d.m_Script
            if isinstance(data,str): data=data.encode('utf-8','surrogateescape')
            else: data=bytes(data)
            path=stem.with_name(stem.name+'.bytes'); path.write_bytes(data)
            if data[:3]==b'BZh':
                decoded=bz2.decompress(data); path.with_suffix('.decoded').write_bytes(decoded)
            else:
                for offset in (4,8):
                    if data[offset:offset+3]==b'BZh':
                        path.with_suffix('.decoded').write_bytes(bz2.decompress(data[offset:])); break
        elif typ=='Texture2D':
            path=stem.with_name(stem.name+'.png'); d.image.save(path)
        elif typ=='AudioClip':
            paths=[]
            for fname,blob in d.samples.items():
                path=stem.with_name(stem.name+Path(fname).suffix); path.write_bytes(blob); paths.append(str(path.relative_to(root)))
            index.append({'type':typ,'name':name,'paths':paths}); counts[typ]+=len(paths); continue
        elif typ=='Font':
            path=stem.with_name(stem.name+'.ttf'); path.write_bytes(bytes(d.m_FontData))
        else:
            path=stem.with_name(stem.name+'.json'); path.write_text(json.dumps(o.read_typetree(),default=str))
        counts[typ]+=1
        index.append({'type':typ,'name':name,'path':str(path.relative_to(root)),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
    except Exception as e: errors.append({'type':typ,'file':o.assets_file.name,'id':o.path_id,'error':str(e)})
(root/'reports/asset-index.json').write_text(json.dumps({'counts':dict(counts),'errors':errors,'assets':index},indent=2))
print(json.dumps({'counts':dict(counts),'errors':errors[:12]}))
