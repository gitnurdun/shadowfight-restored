#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
JAVA="$HOME/.sdkman/candidates/java/current/bin"
BT="$HOME/android-sdk/build-tools/35.0.0"
mkdir -p work/restore-classes work/restore-dex
"$JAVA/javac" -source 8 -target 8 -bootclasspath "$HOME/android-sdk/platforms/android-35/android.jar" -d work/restore-classes tools/RestoreBootstrap.java
find work/restore-classes -name '*.class' > work/restore-class-files.txt
"$BT/d8" --lib "$HOME/android-sdk/platforms/android-35/android.jar" --min-api 24 --output work/restore-dex @work/restore-class-files.txt
cp work/restore-dex/classes.dex original-revival-edit/classes9.dex
"$JAVA/java" -jar tools/apktool.jar b original-revival-edit --aapt "$BT/aapt2" -o work/original-restore-unsigned.apk
"$BT/zipalign" -f -P 16 4 work/original-restore-unsigned.apk work/original-restore-aligned.apk
"$BT/apksigner" sign --ks reborn/debug.keystore --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --v4-signing-enabled false --out deliverables/ShadowFight-Original-Restored.apk work/original-restore-aligned.apk
"$BT/apksigner" verify --verbose deliverables/ShadowFight-Original-Restored.apk
"$BT/zipalign" -c -P 16 4 deliverables/ShadowFight-Original-Restored.apk
sha256sum deliverables/ShadowFight-Original-Restored.apk
