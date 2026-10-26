"""Prepare an isolated, offline clone preserving the original Unity engine."""
import json,shutil
from pathlib import Path
from xml.etree import ElementTree as E
root=Path(__file__).resolve().parents[1];project=root/'original-revival-edit';ns='{http://schemas.android.com/apk/res/android}'
E.register_namespace('android',ns[1:-1])
p=project/'AndroidManifest.xml';tree=E.parse(p);m=tree.getroot();m.set('package','local.shadowfight.restored')
for e in m:
 if e.tag in ['uses-permission','permission']:
  name=e.get(ns+'name','')
  if name.startswith('com.nekki.shadowfight.'):e.set(ns+'name',name.replace('com.nekki.shadowfight.','local.shadowfight.restored.',1))
app=m.find('application');app.set(ns+'label','Shadow Fight Restored');app.set(ns+'extractNativeLibs','true')
# Offline personal build: no network permissions or automatic SDK providers.
for e in list(m):
 if e.tag=='uses-permission' and e.get(ns+'name') in ['android.permission.INTERNET','android.permission.POST_NOTIFICATIONS','com.google.android.gms.permission.AD_ID']:m.remove(e)
for e in list(app):
 if e.tag=='provider':app.remove(e)
 if e.tag=='activity':
  for f in list(e.findall('intent-filter')):
   if any(a.get(ns+'name')=='android.intent.action.MAIN' for a in f.findall('action')):e.remove(f)
for name,val in [('firebase_analytics_collection_deactivated','true'),('firebase_crashlytics_collection_enabled','false'),('com.facebook.sdk.AutoInitEnabled','false')]:
 for e in list(app.findall('meta-data')):
  if e.get(ns+'name')==name:app.remove(e)
 E.SubElement(app,'meta-data',{ns+'name':name,ns+'value':val})
activity=E.SubElement(app,'activity',{ns+'name':'local.shadowfight.restore.RestoreBootstrap',ns+'exported':'true',ns+'screenOrientation':'landscape',ns+'theme':'@android:style/Theme.Material.NoActionBar.Fullscreen'})
f=E.SubElement(activity,'intent-filter');E.SubElement(f,'action',{ns+'name':'android.intent.action.MAIN'});E.SubElement(f,'category',{ns+'name':'android.intent.category.LAUNCHER'})
tree.write(p,encoding='utf-8',xml_declaration=True)
dest=project/'assets/revival';dest.mkdir(parents=True,exist_ok=True);index=[];packs=E.Element('Packs')
catalog=json.loads((root/'reports/catalog-downloads-v2.json').read_text())
for entry in catalog:
 if 'error' in entry:continue
 name=entry['name'];source=root/'work/catalog-bundles'/name
 relative='gamedata/bundles/'+name;target=dest/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(source,target)
 index.append({'path':relative,'bytes':source.stat().st_size})
 a=entry['catalog'];E.SubElement(packs,'Pack',{'Name':name,'Url':a['Url'],'Version':'2.46.0','Attach':a.get('Attach','1'),'Hash':a['Hash'],'Priority':'0'})
path=dest/'gamedata/packs.xml';E.ElementTree(packs).write(path,encoding='utf-8',xml_declaration=True);index.append({'path':'gamedata/packs.xml','bytes':path.stat().st_size})
source=root/'work/runtime/live_config_cdn.xml';target=dest/'gamedata/config_cdn.xml';shutil.copyfile(source,target);index.append({'path':'gamedata/config_cdn.xml','bytes':target.stat().st_size})
(dest/'index.json').write_text(json.dumps(index))
print('Prepared offline clone',len(catalog),'catalog results',len(index),'bundled files',sum(i['bytes'] for i in index),'bytes')
