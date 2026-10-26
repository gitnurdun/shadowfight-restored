import array,bisect,json,struct,sys
from pathlib import Path
from capstone import Cs,CS_ARCH_ARM64,CS_MODE_ARM
from capstone.arm64_const import ARM64_OP_IMM,ARM64_OP_REG,ARM64_OP_MEM
root=Path(__file__).resolve().parents[1]
blob=(root/'work/raw/lib/arm64-v8a/libil2cpp.so').read_bytes()
phoff=struct.unpack_from('<Q',blob,32)[0]; phsize,phnum=struct.unpack_from('<HH',blob,54)
loads=[]
for i in range(phnum):
 t,flags,off,va,pa,fs,ms,align=struct.unpack_from('<II6Q',blob,phoff+i*phsize)
 if t==1:loads.append((va,off,fs,flags))
j=json.loads((root/'reports/dump/script.json').read_text())
methods={m['Address']:m for m in j['ScriptMethod']}
addresses=sorted(methods)
symbols={x['Address']:x.get('Value',x.get('Name','')) for key in ('ScriptString','ScriptMetadata','ScriptMetadataMethod') for x in j.get(key,[])}
relocations={}
shoff=struct.unpack_from('<Q',blob,40)[0];shsize,shcount=struct.unpack_from('<HH',blob,58)
for index in range(shcount):
 sh=struct.unpack_from('<II4QII2Q',blob,shoff+index*shsize)
 if sh[1]==4:
  for pos in range(sh[4],sh[4]+sh[5],24):
   at,info,addend=struct.unpack_from('<QQq',blob,pos)
   if info&0xffffffff==1027:relocations[at]=addend
def symbol(va):return symbols.get(va,symbols.get(relocations.get(va),''))
def owner(va):
 k=bisect.bisect_right(addresses,va)-1
 return methods[addresses[k]] if k>=0 else {'Address':0,'Name':'unknown'}
def read(va,count):
 for base,off,size,flags in loads:
  if base<=va<base+size:return blob[off+va-base:off+va-base+count]
 return b''
def disasm(va,size=None):
 if size is None:
  idx=bisect.bisect_right(addresses,va);size=min(7000,(addresses[idx]-va) if idx<len(addresses) else 1024)
 cs=Cs(CS_ARCH_ARM64,CS_MODE_ARM);cs.detail=True; regs={}
 lines=[]
 for ins in cs.disasm(read(va,size),va):
  note='';ops=ins.operands
  if ins.mnemonic in ('adrp','adr') and len(ops)>1:regs[ops[0].reg]=ops[1].imm
  elif ins.mnemonic=='add' and len(ops)>2 and ops[2].type==ARM64_OP_IMM:
   if ops[1].reg in regs:
    regs[ops[0].reg]=regs[ops[1].reg]+ops[2].imm
    addr=regs[ops[0].reg]; note=f' ; &{hex(addr)} {symbol(addr)}'
   else:regs.pop(ops[0].reg,None)
  elif ins.mnemonic.startswith('ldr') and len(ops)>1 and ops[1].type==ARM64_OP_MEM:
   mem=ops[1].mem
   if mem.base in regs:
    addr=regs[mem.base]+mem.disp;note=f' ; [{hex(addr)}] {symbol(addr)}'
   regs.pop(ops[0].reg,None)
  elif ins.mnemonic in ('bl','b') and ops[0].type==ARM64_OP_IMM:
   target=ops[0].imm;note=' ; '+methods.get(target,owner(target))['Name']
   if ins.mnemonic=='bl':
    for r in list(regs):
     if cs.reg_name(r) in [f'x{i}' for i in range(18)]:regs.pop(r,None)
  lines.append(f'{ins.address:08x}: {ins.mnemonic:8} {ins.op_str}{note}')
 return '\n'.join(lines)
def xrefs(targets):
 refs=[]
 for va,off,size,flags in loads:
  if not flags&1:continue
  words=array.array('I',blob[off:off+(size//4)*4])
  for i,w in enumerate(words):
   if w>>26 not in (0b100101,0b000101):continue
   rel=w&0x3ffffff
   if rel&0x2000000:rel-=0x4000000
   call=va+i*4;dest=call+rel*4
   if dest in targets:refs.append({'call':hex(call),'target':methods.get(dest,{'Name':hex(dest)})['Name'],'owner':owner(call)})
 return refs
if __name__=='__main__':
 if len(sys.argv)>1:
  print(disasm(int(sys.argv[1],0),int(sys.argv[2],0) if len(sys.argv)>2 else None));sys.exit()
 targets={a:m for a,m in methods.items() if any(s in m['Name'] for s in ['RijndaelManaged$$.ctor','AesManaged$$.ctor','DESCryptoServiceProvider$$.ctor','RC2CryptoServiceProvider$$.ctor','Convert$$FromBase64String','Rfc2898DeriveBytes$$.ctor','PasswordDeriveBytes$$.ctor','RijndaelManaged$$CreateDecryptor','AesManaged$$CreateDecryptor'])}
 refs=[]
 for va,off,size,flags in loads:
  if not flags&1:continue
  words=array.array('I',blob[off:off+(size//4)*4])
  for i,w in enumerate(words):
   if w>>26!=0b100101:continue
   rel=w&0x3ffffff
   if rel&0x2000000:rel-=0x4000000
   call=va+i*4;dest=call+rel*4
   if dest in targets:
    entry={'call':hex(call),'target':targets[dest]['Name'],'owner':owner(call)};refs.append(entry)
 (root/'reports/crypto-xrefs.json').write_text(json.dumps(refs,indent=2))
 for ref in refs:
  n=ref['owner']['Name']
  if not n.startswith(('System.','Mono.','Ionic.','Org.','UnityEngine.','Google.','BestHTTP.','Unity.')):print(ref['call'],ref['target'],hex(ref['owner']['Address']),n)
