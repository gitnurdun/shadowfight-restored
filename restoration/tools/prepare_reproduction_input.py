"""Validate the exact input and extract only required, bounded ZIP paths."""
import hashlib,shutil,zipfile
from pathlib import Path
root=Path(__file__).resolve().parents[1];apk=root/'aygd2_12479.apk'
assert hashlib.file_digest(apk.open('rb'),'sha256').hexdigest()=='4f1cecf66a154da43ee71bca509d351c7349be26a355d76276482d09ddc54375','Wrong APK; this kit targets one exact build'
dest=root/'work/raw';dest.mkdir(parents=True,exist_ok=True)
for p in ['reports','deliverables','work/runtime/bundles','work/catalog-bundles','reborn']:(root/p).mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(apk) as z:
 for info in z.infolist():
  if info.is_dir() or not info.filename.startswith(('lib/','assets/bin/Data/')):continue
  target=dest/info.filename;assert target.resolve().is_relative_to(dest.resolve())
  target.parent.mkdir(parents=True,exist_ok=True)
  with z.open(info) as src,target.open('wb') as dst:shutil.copyfileobj(src,dst)
print('Input hash verified; original APK unchanged')
