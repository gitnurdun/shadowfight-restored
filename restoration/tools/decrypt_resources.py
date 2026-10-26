"""Recover only assets that pass AES padding and text/XML validation."""
import base64, hashlib, json, collections, sys
from pathlib import Path
from xml.etree import ElementTree as ET
from Crypto.Cipher import AES
from Crypto.Util.Padding import unpad

root=Path(__file__).resolve().parents[1]
cfg=json.loads((root/'reports/resource-key.json').read_text())
key=bytes.fromhex(cfg['key_hex']); iv=bytes.fromhex(cfg['iv_hex'])
source=root/(sys.argv[1] if len(sys.argv)>1 else 'extracted/TextAsset')
out=root/(sys.argv[2] if len(sys.argv)>2 else 'recovered');out.mkdir(exist_ok=True)
manifest_name=sys.argv[3] if len(sys.argv)>3 else 'recovery-manifest.json'
records=[];counts=collections.Counter()
for p in sorted(source.glob('*.bytes')):
 raw=p.read_bytes()
 try:
  cipher=base64.b64decode(b''.join(raw.split()),validate=True)
 except Exception:
  counts['not_base64']+=1;continue
 if not cipher or len(cipher)%16:
  counts['not_block_aligned']+=1;continue
 try:
  padded=AES.new(key,AES.MODE_CBC,iv).decrypt(cipher)
  plain=unpad(padded,16)
 except ValueError:
  counts['padding_failed']+=1;continue
 try:text=plain.decode('utf-8-sig')
 except UnicodeDecodeError:
  counts['non_utf8']+=1;continue
 try:
  tree=ET.fromstring(text);kind='xml';tag=tree.tag
 except ET.ParseError:
  if not text or sum(c.isprintable() or c in '\r\n\t' for c in text)/len(text)<.99:
   counts['not_validated_text']+=1;continue
  kind='txt';tag=None
 # Confirm the cipher exactly, including its original padding.
 assert AES.new(key,AES.MODE_CBC,iv).encrypt(padded)==cipher
 dest=out/(p.stem+'.'+kind);dest.write_bytes(plain)
 counts[kind]+=1
 records.append({'source':str(p.relative_to(root)),'output':str(dest.relative_to(root)),
  'bytes':len(plain),'sha256':hashlib.sha256(plain).hexdigest(),
  'xml_root':tag,'pkcs7_valid':True,'roundtrip_valid':True})
manifest={'algorithm':'AES-256-CBC / PKCS7 / Base64','counts':dict(counts),'files':records}
(root/'reports'/manifest_name).write_text(json.dumps(manifest,indent=2))
print(json.dumps({'counts':dict(counts),'recovered_files':[r['output'] for r in records]},indent=2))
