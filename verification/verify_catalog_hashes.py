"""Reproduce the supplied build's bundle digest; never modify original packs."""
import hashlib,json,struct
from pathlib import Path
root=Path(__file__).resolve().parents[1]

def bundle_salt():
    b=(root/'work/raw/assets/bin/Data/Managed/Metadata/global-metadata.dat').read_bytes()
    h=struct.unpack_from('<64I',b)
    defaults={struct.unpack_from('<iii',b,p)[0]:struct.unpack_from('<iii',b,p)[1:] for p in range(h[16],h[16]+h[17],12)}
    def field(index,expected_name,expected_type,size):
        at,typ,token=struct.unpack_from('<iII',b,h[24]+index*12)
        start=h[6]+at;name=b[start:b.index(b'\0',start)].decode()
        dt,offset=defaults[index]
        assert name==expected_name and dt==expected_type,(index,name,dt)
        return b[h[18]+offset:h[18]+offset+size]
    # Field names repeat across generated array classes: include type and index.
    key=field(22524,'$$field-6480',39645,64)
    encoded=field(22856,'$$field-82',39613,128)
    decoded=bytes(value^key[i%64] for i,value in enumerate(encoded))
    salt=decoded.split('\ue44f'.encode(),1)[0]
    assert len(salt)==88;salt.decode('utf-8')
    return salt

def bundle_hash(path,salt=None):
    first=hashlib.md5()
    with Path(path).open('rb') as source:
        for block in iter(lambda:source.read(1024*1024),b''):first.update(block)
    return hashlib.md5(first.hexdigest().upper().encode('ascii')+(bundle_salt() if salt is None else salt)).hexdigest().upper()

if __name__=='__main__':
    salt=bundle_salt();results=[]
    for entry in json.loads((root/'reports/catalog-downloads-v2.json').read_text()):
        if 'error' in entry:continue
        path=root/'work/catalog-bundles'/entry['name'];actual=bundle_hash(path,salt)
        results.append({'name':entry['name'],'actual':actual,'expected':entry['catalog']['Hash'],'match':actual==entry['catalog']['Hash'],'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
    report={'algorithm':'uppercase_hex(MD5(ASCII(uppercase_hex(MD5(file))) + UTF8(salt)))','salt_fields':[22524,22856],'salt_length':len(salt),'results':results}
    (root/'reports/catalog-salted-hash-verification.json').write_text(json.dumps(report,indent=2))
    assert results and all(x['match'] for x in results),report
    print('Verified catalog hashes:',len(results),'/',len(results))
