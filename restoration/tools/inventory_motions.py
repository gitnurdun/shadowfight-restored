"""Validate complete binary point-animation payloads without assuming one rig size."""
import collections,hashlib,json,math,struct
from pathlib import Path
root=Path(__file__).resolve().parents[1];records=[];errors=[]
for folder in ['extracted/TextAsset','extracted-runtime/TextAsset','extracted-catalog/TextAsset']:
 for p in sorted((root/folder).glob('*.bytes')):
  raw=p.read_bytes()
  if len(raw)<9:continue
  frames=struct.unpack_from('<I',raw)[0]
  if not 1<=frames<=10000:continue
  count=struct.unpack_from('<I',raw,5)[0]
  if not 3<=count<=512:continue
  try:
   off=4;counts=collections.Counter();flags=collections.Counter()
   for frame in range(frames):
    flag,n=struct.unpack_from('<BI',raw,off);off+=5
    # Preserve this byte as an opaque bitfield. Values such as 5 occur in real clips.
    if not 3<=n<=512:raise ValueError('Unsupported frame layout')
    vals=struct.unpack_from('<'+'f'*(n*3),raw,off);off+=n*12
    if not all(math.isfinite(x) and abs(x)<100000 for x in vals):raise ValueError('Invalid coordinate')
    counts[n]+=1;flags[flag]+=1
   if off!=len(raw):raise ValueError('Trailing bytes: '+str(len(raw)-off))
   records.append({'source':str(p.relative_to(root)),'name':p.stem,'frames':frames,'node_counts':dict(counts),'frame_flags':dict(flags),'bytes':len(raw),'sha256':hashlib.sha256(raw).hexdigest()})
  except (ValueError,struct.error) as e:errors.append({'source':str(p.relative_to(root)),'reason':str(e)})
summary={'validated_files':len(records),'unique_names':len({r['name'] for r in records}),'total_frames':sum(r['frames'] for r in records),'rig_sizes':dict(collections.Counter(str(sorted(r['node_counts'])) for r in records))}
(root/'reports/motion-inventory.json').write_text(json.dumps({'summary':summary,'files':records,'unsupported_candidates':errors},indent=2));print(json.dumps(summary,indent=2));print('Unsupported candidates',len(errors))
