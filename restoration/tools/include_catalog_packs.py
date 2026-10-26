"""Package verified compatible packs, retaining explicitly modified local configs."""
import json,shutil
from pathlib import Path
from verify_catalog_hashes import bundle_hash,bundle_salt
root=Path(__file__).resolve().parents[1];dest=root/'original-revival-edit/assets/revival';index_path=dest/'index.json';index={e['path']:e for e in json.loads(index_path.read_text())};salt=bundle_salt();count=0
for entry in json.loads((root/'reports/catalog-downloads-v2.json').read_text()):
    assert 'error' not in entry,entry
    name=entry['name'];source=root/'work/catalog-bundles'/name
    assert source.resolve().is_relative_to((root/'work/catalog-bundles').resolve())
    assert bundle_hash(source,salt)==entry['catalog']['Hash'],name
    relative='gamedata/bundles/'+name;target=dest/relative;target.parent.mkdir(parents=True,exist_ok=True)
    if name!='VERSIONAL_CONFIGS' or not target.exists():shutil.copyfile(source,target)
    index[relative]={'path':relative,'bytes':target.stat().st_size};count+=1
index_path.write_text(json.dumps(list(index.values())))
print('Included verified packs:',count)
