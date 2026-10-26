#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
# Use a fresh extraction of the kit; do not overwrite an active decoded project.
test ! -e original-revival-edit || { echo 'Use a fresh kit directory: original-revival-edit already exists.' >&2; exit 1; }
PY="${RESTORE_PYTHON:-python3}"
JAVA="$HOME/.sdkman/candidates/java/current/bin"
test -f tools/apktool.jar
test -f "$HOME/android-sdk/platforms/android-35/android.jar"
"$PY" tools/prepare_reproduction_input.py
"$PY" tools/extract_text_assets.py
"$PY" tools/recover_resource_key.py
"$PY" tools/decrypt_resources.py
mapfile -t pack_names < <("$PY" -c 'import json; print("\n".join(x["name"] for x in json.load(open("manifests/catalog-downloads.json"))))')
"$PY" tools/fetch_catalog_packs.py "${pack_names[@]}"
"$PY" tools/verify_catalog_hashes.py
cp work/catalog-bundles/ANIMATIONS work/catalog-bundles/VERSIONAL_CONFIGS work/runtime/bundles/
"$PY" tools/extract_runtime.py
"$PY" tools/decrypt_resources.py extracted-runtime/TextAsset recovered-runtime runtime-recovery-manifest.json
"$JAVA/java" -jar tools/apktool.jar d -s aygd2_12479.apk -o original-revival-edit
"$PY" tools/fix_decoded_resources.py
"$PY" tools/prepare_original_restore.py
"$PY" tools/include_catalog_packs.py
"$PY" tools/include_offline_videos.py
"$PY" tools/test_pack_availability.py
"$PY" tools/patch_offline_native.py
"$PY" tools/patch_restored_data.py
"$PY" tools/prepare_restored_profile.py
"$PY" tools/finalize_offline_cache.py
if test ! -f reborn/debug.keystore; then
 "$JAVA/keytool" -genkeypair -keystore reborn/debug.keystore -storepass android -keypass android -alias androiddebugkey -dname 'CN=Offline Local Build' -keyalg RSA -keysize 2048 -validity 10000
fi
bash tools/build_original_restore.sh
