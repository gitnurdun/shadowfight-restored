#!/usr/bin/env bash
set -euo pipefail
PROJ="$(cd "$(dirname "$0")" && pwd)"
SDK="${ANDROID_SDK_ROOT:-$HOME/android-sdk}"
export JAVA_HOME="${JAVA_HOME:-$HOME/.sdkman/candidates/java/current}"
export PATH="$JAVA_HOME/bin:$PATH"
BT="$SDK/build-tools/35.0.0"
PLATFORM="$SDK/platforms/android-35/android.jar"
mkdir -p "$PROJ/work"
BUILD="$(mktemp -d "$PROJ/work/build.XXXXXX")"
mkdir -p "$BUILD/classes" "$BUILD/gen" "$BUILD/dex"
"$BT/aapt2" compile --dir "$PROJ/res" -o "$BUILD/resources.zip"
"$BT/aapt2" link -o "$BUILD/base.apk" -I "$PLATFORM" --manifest "$PROJ/AndroidManifest.xml" --java "$BUILD/gen" -A "$PROJ/assets" --min-sdk-version 26 --target-sdk-version 34 --version-code 3 --version-name 1.2 "$BUILD/resources.zip"
find "$PROJ/src" "$BUILD/gen" -name '*.java' > "$BUILD/java-files.txt"
javac -source 8 -target 8 -bootclasspath "$PLATFORM" -encoding UTF-8 -d "$BUILD/classes" @"$BUILD/java-files.txt"
find "$BUILD/classes" -name '*.class' > "$BUILD/class-files.txt"
"$BT/d8" --lib "$PLATFORM" --min-api 26 --output "$BUILD/dex" @"$BUILD/class-files.txt"
cp "$BUILD/base.apk" "$BUILD/unsigned.apk"
(cd "$BUILD/dex" && zip -q -j "$BUILD/unsigned.apk" classes.dex)
"$BT/zipalign" -f 4 "$BUILD/unsigned.apk" "$BUILD/aligned.apk"
if [ ! -f "$PROJ/debug.keystore" ]; then
 keytool -genkeypair -keystore "$PROJ/debug.keystore" -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Shadow Offline Local Build,O=Personal,C=CN"
fi
"$BT/apksigner" sign --ks "$PROJ/debug.keystore" --ks-pass pass:android --key-pass pass:android --ks-key-alias androiddebugkey --v4-signing-enabled false --out "$PROJ/shadowfight-offline.apk" "$BUILD/aligned.apk"
"$BT/apksigner" verify --verbose "$PROJ/shadowfight-offline.apk"
"$BT/zipalign" -c 4 "$PROJ/shadowfight-offline.apk"
sha256sum "$PROJ/shadowfight-offline.apk"
echo "BUILT: $PROJ/shadowfight-offline.apk"
