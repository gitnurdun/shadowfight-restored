"""Recover a matching resource key from this APK's XOR-hidden constants.

Only a key that yields the expected XML structure is written to the report.
Unrelated hidden strings are not exported.
"""
import base64,hashlib,json,re,struct
from pathlib import Path
from Crypto.Cipher import AES,ARC4
root=Path(__file__).resolve().parents[1]
b=(root/'work/raw/assets/bin/Data/Managed/Metadata/global-metadata.dat').read_bytes()
h=struct.unpack_from('<64I',b)
def name(index):
 off=h[6]+index
 return b[off:b.index(b'\0',off)].decode('utf8','replace')
defaults={}
for p in range(h[16],h[16]+h[17],12):
 index,typ,off=struct.unpack_from('<iii',b,p);defaults[index]=(typ,off)
fields={}
for i,p in enumerate(range(h[24],h[24]+h[25],12)):
 string_index,typ,token=struct.unpack_from('<iII',b,p)
 fields[i]={'name':name(string_index),'type':typ}
arrays={}
for i,(typ,off) in defaults.items():
 if i not in fields or '$field-' not in fields[i]['name']:continue
 # ArrayCopy64 has the same IL2CPP type index throughout this build.
 if typ==39645:arrays[i]=b[h[18]+off:h[18]+off+64]
candidates={};hidden_count=0
def add_variants(text,origin):
 variants=[('utf8',text.encode())]
 try:variants.append(('base64',base64.b64decode(text,validate=True)))
 except Exception:pass
 if re.fullmatch(r'[0-9a-fA-F]+',text) and len(text)%2==0:
  variants.append(('hex',bytes.fromhex(text)))
 for kind,data in variants:
  if len(data) in (16,24,32):candidates.setdefault(data,[]).append({**origin,'encoding':kind})
for i,left in arrays.items():
 if i+1 not in arrays:continue
 raw=bytes(x^y for x,y in zip(left,arrays[i+1]))
 marker=raw.find('\ue44f'.encode())
 if marker<0:continue
 try:text=raw[:marker].decode('utf8')
 except UnicodeDecodeError:continue
 hidden_count+=1
 origin={'fields':[fields[i]['name'],fields[i+1]['name']],'field_indices':[i,i+1],'layer':'xor'}
 add_variants(text,origin)
 try:
  wrapped=base64.b64decode(text,validate=True)
  clear=ARC4.new(b'wYWgbpF8NopZ8s2f').decrypt(wrapped).decode('utf8')
  add_variants(clear,{**origin,'layer':'xor+base64+rc4'})
 except Exception:pass
sample=base64.b64decode((root/'extracted/TextAsset/mdl_body.bytes').read_bytes())
matches=[]
for key,origins in candidates.items():
 body=AES.new(key,AES.MODE_CBC,bytes(16)).decrypt(sample[:160])[16:]
 if b'encoding=' in body or b'<Root' in body:
  matches.append((key,origins,body));print('KEY MATCH',key.hex(),origins,repr(body[:120]))
print('Hidden strings tested:',hidden_count,'candidate AES values:',len(candidates),'matches:',len(matches))
for key,origins,body in matches:
 for iv,iv_origins in candidates.items():
  if len(iv)!=16:continue
  header=AES.new(key,AES.MODE_CBC,iv).decrypt(sample[:64])
  if header.startswith((b'<?xml',b'\xef\xbb\xbf<?xml',b'<Root')):
   result={'algorithm':'AES-CBC','key_hex':key.hex(),'iv_hex':iv.hex(),'key_origin':origins,'iv_origin':iv_origins,'header':header.decode('utf8','replace')}
   (root/'reports/resource-key.json').write_text(json.dumps(result,indent=2))
   print('IV MATCH',iv.hex(),iv_origins,repr(header))
   plain=AES.new(key,AES.MODE_CBC,iv).decrypt(sample)
   (root/'reports/model-test.bin').write_bytes(plain)
