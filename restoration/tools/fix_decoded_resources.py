"""Preserve IDs while renaming synthetic '$' resources rejected by the SDK aapt2."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1];res=root/'original-revival-edit/res';mapping={};renamed=[]
for p in sorted(res.rglob('*')):
 if not p.is_file():continue
 name=p.name.split('.')[0]
 if '$' not in name:continue
 new='revival_'+name.replace('$','_');mapping[name]=new
 target=p.with_name(new+p.name[len(name):]);p.rename(target);renamed.append([str(p.relative_to(res)),str(target.relative_to(res))])
for p in res.rglob('*.xml'):
 s=p.read_text();old=s
 for before,after in mapping.items():
  s=s.replace('/'+before,'/'+after).replace('name="'+before+'"','name="'+after+'"')
 if s!=old:p.write_text(s)
(root/'reports/renamed-synthetic-resources.json').write_text(json.dumps(renamed,indent=2));print('Renamed',len(renamed),'synthetic resource files, preserving public IDs')
