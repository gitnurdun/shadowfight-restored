"""Create a clean local profile without device/account identifiers."""
import json,copy
from pathlib import Path
from xml.etree import ElementTree as E
root=Path(__file__).resolve().parents[1];dest=root/'original-revival-edit/assets/revival';r=E.parse(root/'recovered/usersDefault.xml').getroot();w=r.find('.//Warrior')
w.set('Money','999999999');w.set('Bonus','999999999');w.set('Level','52');w.set('Experience','0');w.set('Tutorial','END');w.set('CurrentZone','ZONE_1');w.set('Power','5');w.set('PowerSyncTime','0')
w.set('ShowUpgrades','1')
items=w.find('Items');existing={e.get('Name') for e in items};catalog_path=root/'recovered-runtime/list.xml';catalog=E.parse(catalog_path if catalog_path.exists() else root/'recovered/list.xml').getroot();added=0
for e in catalog.findall('./Items/Item'):
 if e.get('Type') not in ['Weapon','Armor','Helm','Ranged','Magic','Energy','Consumable','RaidConsumable']:continue
 name=e.get('Name')
 if not name or name in existing:continue
 E.SubElement(items,'Item',{'Name':name,'Equipped':'0','Count':'999999' if e.get('Type') in ['Consumable','RaidConsumable'] else '1','UpgradeLevel':e.get('UpgradeLevel','0'),'DeliveryTime':'0','DeliveryUpgradeLevel':'-1','AcquireType':'Item'});existing.add(name);added+=1
if 'Unlimited_Energy' not in existing:E.SubElement(items,'Item',{'Name':'Unlimited_Energy','Equipped':'0','Count':'1'})
equipment={e.get('Name'):e for e in catalog.findall('./Items/Item')}
loadout={'Weapon':'WEAPON_FIRE_BATONS','Armor':'ARMOR_MEMBRANE_CUIRASS','Helm':'HELM_VISOR'}
for slot,name in loadout.items():w.set(slot,name)
for item in items:
 definition=equipment.get(item.get('Name'))
 if definition is not None and definition.get('Type') in loadout:
  item.set('Equipped','1' if item.get('Name')==loadout[definition.get('Type')] else '0')
battles=w.find('Battles');battles.clear();fights=E.SubElement(w,'Fights');stages=E.parse(root/'recovered/stages.xml').getroot();battle_count=fight_count=0
for zone in stages.findall('./Zones/Zone'):
 zn=zone.get('Name','')
 if zn not in {'ZONE_1','ZONE_2','ZONE_3','ZONE_4','ZONE_5','ZONE_6','ZONE_7','ZONE_IM','ZONE_7_2','ZONE_7_3','Punchbag'}:continue
 for battle in zone.findall('Battle'):
  if battle.get('Type')=='FAKE':continue
  bn=battle.get('Name','')
  # Eclipse alternatives share map coordinates with normal battles. Their own
  # quest/toggle controls visibility; forcing both open selects the wrong fight.
  locked=bn.endswith(('_ECLIPSEMODE','_INTERMISSION'))
  if zn=='ZONE_7' and battle.get('X') is not None:
   locked=locked or bn not in {'C3_BOSS_TITAN','C3_Tournament','C3_Challenge','C3_Duel','C3_Survival'}
  # A saved Battle entry makes the node visible. Locked=1 still draws it and
  # can cover the ordinary node, so omit inactive alternatives entirely.
  if not locked:
   E.SubElement(battles,'Battle',{'Name':zn+'|'+bn+'|','Locked':'0'});battle_count+=1
  for fight in battle.findall('Fight'):
   fn=fight.get('Name','')
   main_boss={'ZONE_1':'BOSS_LYNX','ZONE_2':'BOSS_HERMIT','ZONE_3':'BOSS_BUTCHER','ZONE_4':'BOSS_WASP','ZONE_5':'BOSS_HUNTRESS','ZONE_6':'BOSS_SAMURAI','ZONE_7':'C3_BOSS_TITAN'}
   won='1' if bn==main_boss.get(zn) and fn.isdigit() and int(fn)<6 else '0'
   E.SubElement(fights,'Fight',{'ID':'-1','IDS':zn+'|'+bn+'|'+fn,'CompletedCount':won,'LossCount':'0','EclipseCompletedCount':'0','EclipseLossCount':'0','StoryCount':won,'CompletedTime':'0','TimeLeft':'0','RandomizeTimeLeft':'0','Level':'0','RandomGroupSeed':'12345','RandomRuleSeed':'12345'});fight_count+=1
quests=E.SubElement(w,'Quests');variables=E.SubElement(quests,'Variables')
defaults={'GDPROnStartAccepted':'1','StoryTutorialStep':'END','$StoryTutorialStep':'END','OffersEnabled':'0','DailyOffersEnabled':'0','ForgeEnabled':'2','RangedAvailableTag':'1','globZoneToCheck':'ZONE_1','CurrentZone':'ZONE_1','Avatar_Notification':'character_may_1_small','Title_Assistant':'characterMay','Title_Assistant_2':'characterSensei'}
defaults.update({'Avatar_Assistant_'+str(i):'character_may_'+str(i) for i in range(1,6)})
defaults['Avatar_Assistant_6']='character_sensei'
for k,v in defaults.items():E.SubElement(variables,'Variable',{'Name':k,'Value':v})
E.SubElement(quests,'Quests');E.SubElement(w,'SessionSettings')
profile=E.tostring(r,encoding='utf-8',xml_declaration=True);index=json.loads((dest/'index.json').read_text());index=[e for e in index if not e['path'].startswith('userdata/')]
for name in ['users.xml','users_backup.xml']:
 p=dest/'userdata'/name;p.parent.mkdir(exist_ok=True);p.write_bytes(profile);index.append({'path':'userdata/'+name,'bytes':len(profile),'seed':True})
(dest/'index.json').write_text(json.dumps(index));(root/'reports/restored-profile-summary.json').write_text(json.dumps({'owned_items':len(items),'items_added':added,'battle_groups_unlocked':battle_count,'fight_records':fight_count,'hero_level':52,'contains_device_or_account_identifiers':False},indent=2));print('Profile:',len(items),'items,',battle_count,'battle groups,',fight_count,'fight records')

