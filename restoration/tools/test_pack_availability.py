"""Execute the actual replacement ARM64 code against mocked runtime calls."""
import struct,json
from pathlib import Path
from unicorn import Uc,UC_ARCH_ARM64,UC_MODE_ARM,UC_HOOK_CODE
from unicorn.arm64_const import *
from pack_availability_code import assemble,RVA,GET_BUNDLE_DIRECTORY,ACCESS_GOT
root=Path(__file__).resolve().parents[1];results=[]

def check(name,base,exists,expected,expect_access=None):
    u=Uc(UC_ARCH_ARM64,UC_MODE_ARM)
    for addr,size in [(RVA&~4095,8192),(GET_BUNDLE_DIRECTORY&~4095,4096),(ACCESS_GOT&~4095,4096),(0x6000000,8192),(0x10000000,65536),(0x20000000,16384)]:u.mem_map(addr,size)
    u.mem_write(RVA,assemble());u.mem_write(ACCESS_GOT,struct.pack('<Q',0x6000000))
    def string(addr,value):u.mem_write(addr,b'\0'*16+struct.pack('<I',len(value))+value.encode('utf-16le')+b'\0\0')
    string(0x20000000,base)
    if name is not None:string(0x20002000,name)
    u.reg_write(UC_ARM64_REG_SP,0x10008000);u.reg_write(UC_ARM64_REG_X1,0 if name is None else 0x20002000)
    u.reg_write(UC_ARM64_REG_X19,1234);u.reg_write(UC_ARM64_REG_X20,5678);u.reg_write(UC_ARM64_REG_X30,0x6001000)
    called=[]
    def hook(uc,address,size,user):
        if address==GET_BUNDLE_DIRECTORY:
            uc.reg_write(UC_ARM64_REG_X0,0x20000000);uc.reg_write(UC_ARM64_REG_PC,uc.reg_read(UC_ARM64_REG_X30))
        elif address==0x6000000:
            p=uc.reg_read(UC_ARM64_REG_X0);path=bytes(uc.mem_read(p,512)).split(b'\0')[0].decode();called.append(path)
            assert path==base+'/'+name and uc.reg_read(UC_ARM64_REG_X1)==0
            uc.reg_write(UC_ARM64_REG_X0,0 if exists else 0xffffffffffffffff);uc.reg_write(UC_ARM64_REG_PC,uc.reg_read(UC_ARM64_REG_X30))
        elif address==0x6001000:uc.emu_stop()
    u.hook_add(UC_HOOK_CODE,hook);u.emu_start(RVA,0,count=10000)
    actual=u.reg_read(UC_ARM64_REG_X0);assert actual==expected,(name,actual,expected)
    if expect_access is not None:assert bool(called)==expect_access,(name,called,expect_access)
    assert u.reg_read(UC_ARM64_REG_SP)==0x10008000 and u.reg_read(UC_ARM64_REG_X19)==1234 and u.reg_read(UC_ARM64_REG_X20)==5678
    results.append({'name':name,'base_length':len(base),'file_exists':exists,'result':actual,'access_called':bool(called)})

base='/storage/emulated/0/Android/data/local.shadowfight.restored/files/gamedata/bundles'
for name,exists,expected in [('ZONE_1',True,1),('OFFERS/WEEKLY_OFFER_13',True,1),('NOT_BUNDLED',False,0),('',True,0),(None,True,0),('../userdata/users.xml',True,0),('A\\B',True,0),('中',True,0),('A'*201,True,0),('A'*200,True,1)]:check(name,base,exists,expected)
check('ZONE_1','A'*301,True,0);check('ZONE_1','',True,0);check('A'*200,'B'*300,True,1)
for name in ['Shadow_fight_ending.mp4','shadow_gate.mp4','OFFERS/clip.mp4','a.b.c','folder.name/clip.mp4']:
    check(name,base,True,1,expect_access=True)
    check(name,base,False,0,expect_access=True)
for name in ['.','..','.hidden','A/./ZONE_1','A/../ZONE_1','A..B','A.','A./B','A/.hidden']:
    check(name,base,True,0,expect_access=False)
(root/'reports/pack-availability-code-tests.json').write_text(json.dumps({'passed':len(results),'code_bytes':len(assemble()),'cases':results},indent=2))
print('ARM64 predicate tests passed:',len(results))
