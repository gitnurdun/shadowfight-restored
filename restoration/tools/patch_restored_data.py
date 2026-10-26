"""Provide renewable offline supplies, token shop costs and free fight entry."""
import base64,hashlib,json,resource
from pathlib import Path
from xml.etree import ElementTree as E
resource.setrlimit(resource.RLIMIT_NOFILE,(8192,8192))
import UnityPy
from Crypto.Cipher import AES
from Crypto.Util.Padding import pad,unpad
root=Path(__file__).resolve().parents[1];project=root/'original-revival-edit';cfg=json.loads((root/'reports/resource-key.json').read_text());key=bytes.fromhex(cfg['key_hex']);iv=bytes.fromhex(cfg['iv_hex']);reports=[]
def transform(name,raw):
 cipher=base64.b64decode(raw);clear=unpad(AES.new(key,AES.MODE_CBC,iv).decrypt(cipher),16);changed=0
 if name=='BuildSettings':
  settings=json.loads(clear);settings['ShowIntro']=False
  plain=json.dumps(settings,separators=(',',':')).encode();result=base64.b64encode(AES.new(key,AES.MODE_CBC,iv).encrypt(pad(plain,16)))
  assert json.loads(unpad(AES.new(key,AES.MODE_CBC,iv).decrypt(base64.b64decode(result)),16))['ShowIntro'] is False
  return result,1
 tree=E.fromstring(clear)
 if name=='list':
  for item in tree.findall('./Items/Item'):
   if item.get('Type')=='RealMoneyItem':continue
   for k in ['Price','BonusPrice','BonusDeliveryPrice','DeliveryTime']:
    if k in item.attrib:item.set(k,'0' if k=='DeliveryTime' else '1');changed+=1
  for upgrade in tree.iter('Upgrade'):
   for k in ['Price','BonusPrice','BonusDeliveryPrice','DeliveryTime']:
    if k in upgrade.attrib:upgrade.set(k,'0' if k=='DeliveryTime' else '1');changed+=1
 elif name=='stages':
  for zone in tree.findall('./Zones/Zone'):
   if zone.get('Name')=='ZONE_7':
    for battle in zone.findall('Battle'):
     if battle.get('Name') in ['C3_Challenge','Challenge_ECLIPSEMODE']:
      battle.set('X','170');changed+=1
  for fight in tree.findall('./Zones/Zone/Battle/Fight'):
   fight.set('Power','0');fight.set('Replays','1');changed+=2
 elif name=='forge':
  for price in tree.iter('Price'):
   for k in ['ForgeMaterial1','ForgeMaterial2','ForgeMaterial3','DeliveryTime','BonusDeliveryPrice']:
    if k in price.attrib:price.set(k,'0');changed+=1
 elif name=='login_notify':
  quest=next(q for q in tree.findall('Quest') if q.get('Name')=='ApplicationStartAccountLoginNotify')
  actions=quest.find('Actions');actions.clear()
  callback=E.SubElement(actions,'RunCallback',{'Name':'ApplicationStartAccountLoginNotifyComplete'})
  E.SubElement(callback,'Arg',{'Value':'Continue'});changed=1
 elif name=='raid_quests':
  # A high-level offline seed otherwise launches the online raid tutorial after
  # every first story fight. Keep manual raid actions separate from story play.
  matches=[q for q in tree.findall('Quest') if q.get('Name')=='RaidTutorialLauncherFightEnd']
  assert len(matches)==1
  E.SubElement(matches[0].find('Conditions'),'Equal',{'Value1':'0','Value2':'1'});changed=1
 elif name=='quests':
  quest=E.SubElement(tree,'Quest',{'Name':'OfflineSupplyRefill','Priority':'10000','Unresumable':'1'})
  E.SubElement(E.SubElement(quest,'Events'),'ApplicationStart')
  actions=E.SubElement(quest,'Actions')
  for currency in ['Gold','Bonus']:E.SubElement(actions,'GiveCurrency',{'Type':currency,'Value':'1000000'})
  E.SubElement(actions,'ShowForge');changed=3
 plain=E.tostring(tree,encoding='utf-8',xml_declaration=True);result=base64.b64encode(AES.new(key,AES.MODE_CBC,iv).encrypt(pad(plain,16)))
 assert unpad(AES.new(key,AES.MODE_CBC,iv).decrypt(base64.b64decode(result)),16)==plain
 return result,changed
source=root/'work/raw/assets/bin/Data';env=UnityPy.load(str(source));modified={}
for o in env.objects:
 if o.type.name!='TextAsset':continue
 d=o.read()
 if d.m_Name not in ['list','stages','BuildSettings','forge','login_notify','quests','raid_quests']:continue
 raw=d.m_Script.encode('utf8','surrogateescape') if isinstance(d.m_Script,str) else bytes(d.m_Script);result,changed=transform(d.m_Name,raw);d.m_Script=result.decode('ascii');d.save();modified[o.assets_file.name]=o.assets_file
 reports.append({'source':'APK','asset':d.m_Name,'changes':changed,'source_sha256':hashlib.sha256(raw).hexdigest(),'modified_sha256':hashlib.sha256(result).hexdigest()})
assert {'list','stages','BuildSettings','forge','login_notify','quests','raid_quests'} <= {e['asset'] for e in reports},reports
for name,file in modified.items():
 matches=[p for p in source.rglob(name) if p.is_file()];assert len(matches)==1,matches
 relative=matches[0].relative_to(source);target=project/'assets/bin/Data'/relative;target.write_bytes(file.save())
bundle_source=root/'work/catalog-bundles/VERSIONAL_CONFIGS';env=UnityPy.load(str(bundle_source));bundle=next(iter(env.files.values()))
for o in env.objects:
 if o.type.name!='TextAsset':continue
 d=o.read()
 if d.m_Name!='list':continue
 raw=d.m_Script.encode('utf8','surrogateescape') if isinstance(d.m_Script,str) else bytes(d.m_Script);result,changed=transform('list',raw);d.m_Script=result.decode('ascii');d.save();reports.append({'source':'VERSIONAL_CONFIGS','asset':'list','changes':changed})
target=project/'assets/revival/gamedata/bundles/VERSIONAL_CONFIGS';target.write_bytes(bundle.save(packer='original'))
check=UnityPy.load(str(target));assert sum(o.type.name=='TextAsset' for o in check.objects)==18
index_path=project/'assets/revival/index.json';index=json.loads(index_path.read_text())
for entry in index:
 if entry['path']=='gamedata/bundles/VERSIONAL_CONFIGS':entry['bytes']=target.stat().st_size
index_path.write_text(json.dumps(index));(root/'reports/free-economy-data-patches.json').write_text(json.dumps(reports,indent=2));print(json.dumps(reports,indent=2))
