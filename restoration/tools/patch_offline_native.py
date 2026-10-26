"""Patch only the clone's local file checks and offline cache deletion, ARM64."""
import hashlib,json,struct,shutil
from pathlib import Path
root=Path(__file__).resolve().parents[1];original=root/'work/raw/lib/arm64-v8a/libil2cpp.so';target=root/'original-revival-edit/lib/arm64-v8a/libil2cpp.so'
raw=original.read_bytes()
assert hashlib.sha256(raw).hexdigest()=='a8d17fa06239b2bff07c22a714b5b5ee9c631fa86b784c8c6154d1a615ec94fe','Native patches require the exact preserved input build'
data=bytearray(raw);phoff=struct.unpack_from('<Q',raw,32)[0];size,count=struct.unpack_from('<HH',raw,54);segments=[]
for i in range(count):
 p=struct.unpack_from('<II6Q',raw,phoff+i*size)
 if p[0]==1:segments.append((p[3],p[2],p[5]))
def offset(rva):
 for va,off,sz in segments:
  if va<=rva<va+sz:return off+rva-va
 raise ValueError('Unmapped address')
patches=[('CFJANHACGMG.FNIHLBLMOBP',0x35991b4,bytes.fromhex('20008052c0035fd6'),'Accept local XML saves in this isolated offline build'),('CFJANHACGMG.PBACHEOJBGG',0x3599870,bytes.fromhex('20008052c0035fd6'),'Accept local binary cache files'),('CFJANHACGMG.AMELFFLEPNF',0x3598984,bytes.fromhex('20008052c0035fd6'),'Accept parsed local XML documents'),('IDDIABHBDEE.FKLCGHKBKHK',0x33871b4,bytes.fromhex('c0035fd6'),'Keep bundled configuration when network is unavailable')]
records=[]
from pack_availability_code import assemble
patches.append(('AAPGCAPGBLG.LAAKLBPOMGP',0x325f148,assemble(),'Check actual local bundle presence; do not advertise missing optional packs'))
for name,rva,replacement,purpose in patches:
 off=offset(rva);before=bytes(data[off:off+len(replacement)]);data[off:off+len(replacement)]=replacement
 records.append({'method':name,'rva':hex(rva),'file_offset':hex(off),'original_bytes':before.hex(),'replacement_bytes':replacement.hex(),'purpose':purpose})
target.write_bytes(data)
arm32=root/'original-revival-edit/lib/armeabi-v7a';backup=root/'work/restoration-original-arm32'
if arm32.exists():
 assert arm32.resolve().is_relative_to((root/'original-revival-edit').resolve())
 if not backup.exists():shutil.move(str(arm32),str(backup))
 else:raise RuntimeError('Unexpected second ARM32 tree; inspect before proceeding')
report={'abi':'arm64-v8a','input_sha256':hashlib.sha256(raw).hexdigest(),'output_sha256':hashlib.sha256(data).hexdigest(),'patches':records,'scope':'Personal offline clone only; original APK remains unchanged'}
(root/'reports/offline-native-patches.json').write_text(json.dumps(report,indent=2));print(json.dumps(report,indent=2))
