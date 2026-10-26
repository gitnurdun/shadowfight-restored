> **Fallback source:** This is an earlier Java remake. It is not the Unity-preserving restored game.

# Shadow Fight Offline

Updated to **v1.2** on 25 October 2026. This build uses the decrypted original fighter geometry, original UI sprites and original font. The recovery tutorial and lessons log describe the verified extraction process and remaining gaps.

A standalone, offline Android remake for personal use, built from the supplied
`aygd2_12479.apk` and a new Java combat engine. This is a playable reconstruction,
not the recovered original Unity project or a complete port of every Shadow Fight 2 feature.

## Install

Copy `shadowfight-offline.apk` to the Honor 400 and open it. Allow installation
from your file manager if Android asks. The installed name is **Shadow Fight
Offline**. It uses a separate package (`local.shadowfight.reborn`), so it can
coexist with the original game. No account, network connection, data download,
Google Play services, or runtime permissions are required.

Requires Android 8.0 or later. The application contains Java bytecode and assets
only, with no native libraries. It is independent of ARM32 support and native
library page alignment. Compilation uses API 35; target API is 34.

## Play

- Story: six best-of-three duels, from Shin to Lynx. Every opponent is selectable immediately.
- Survival: progressively stronger challengers, with a saved best result.
- Practice: an untimed sparring partner; toggle its AI during the session.
- Armory: bare hands, twin blades and a staff; all three weapons and power/armor upgrades are free.
- Three difficulty levels, three recovered arenas, sound effects and looping music.
- Progress, currency, upgrades and settings save on the device.

Drag the left pad to move. Up jumps and down ducks. Hold PUNCH or KICK to
chain attacks. Down + KICK sweeps; attacking while airborne uses an aerial
strike. GUARD reduces damage but does not stop a sweep. POWER is stronger and
slower. Player stamina stays full; enemy stamina and incoming damage retain their normal rules. Pause is at the bottom center.

Keyboard controls: A/D or left/right, W/up to jump, J punch, K kick, L power,
space guard, Escape pause/back.

## Source and rebuild

The included `src/` contains the entire new implementation. `Combat.java`
contains combat, AI, stamina and round rules. `MainActivity.java` contains
Android lifecycle handling, audio, menus, touch controls and rendering.
`assets/` contains the recovered and prepared game resources.

Run `bash build.sh` with JDK 21 and the Android command-line SDK installed at
`$HOME/android-sdk`, or set `ANDROID_SDK_ROOT` to another SDK location.
Required packages: platforms;android-35 and build-tools;35.0.0. No Gradle,
Android Studio, Unity editor, NDK or external Java dependencies are needed.

The server retains the local development signing keystore. The new source archive excludes it; rebuilding elsewhere creates a new development key. To update an installed build without uninstalling it, reuse the server's existing key. This is not the original publisher's signing key.

Combat checks can be run independently of Android:

```
mkdir -p work/test-classes
javac -d work/test-classes src/local/shadowfight/reborn/Combat.java tests/CombatTest.java
java -cp work/test-classes local.shadowfight.reborn.CombatTest
```

## Recovery notes

The supplied APK identifies as `com.nekki.shadowfight`, version 2.46.0, Unity
2022.3.62f2, minSdk 24 and targetSdk 35. It contains both arm64-v8a and
armeabi-v7a binaries. Its main arm64 engine libraries already have 16 KB ELF
segment alignment; an Honor-specific failure cause was not established.

Recovered resources on the server include 234 exported textures, 62 audio
clips, 415 text/binary assets, 13 Unity mesh dumps, 5 fonts and an animation
dump. Five texture objects could not be exported with the available parser;
they are recorded in `reports/asset-index.json`. The original compiled files
remain intact under `work/raw/` and inside the original APK.

The remake uses 29 selected recovered 67-point skeletal motion clips, backgrounds
composed from original atlas sprites and stage XML, and original audio/art.
The silhouettes are rendered with an Android Canvas implementation of the recovered body/head capsules, triangles and weighted skeletal nodes. Cloth motion is approximate. The combat
timing, hit tests, AI, progression, equipment, menus and controls are newly
implemented. The AES key and IV were recovered from hidden native-code constants: 75 APK resources and six downloaded XML configs were decrypted and verified. The original client also downloaded 234 animation assets and 18 configuration assets in the emulator. Later catalog recovery validated 429 unique clips / 16,282 frames and 478 additional model XML documents. All 14 selected story/support bundle downloads match their reproduced salted catalog hashes. The APK's IL2CPP native program is not editable C#
source, and the original Unity source project was not recovered.

Full original story acts, online raids, original item/perk systems, monetization,
cloud saves, and exact original physics/mesh rendering are not implemented.
The original game's assets remain the property of their respective owners.

The source package is `ShadowFight-Source-v1.2.zip`. `Recovered-Resources.zip` contains the exported assets, decrypted data, captured runtime bundles, scripts and manifests. `Recovery-Tutorial.md` explains the successful decryption and dynamic download recovery. `Lessons-Learned.md` retains problems, false leads and solutions throughout the project.

Workspace: `<workspace>/shadowfight/`.
Installable APK: `reborn/shadowfight-offline.apk`.
Original input: `aygd2_12479.apk` (preserved).

## Validation

The APK installs and launches on an Android 15 / API 35 x86_64 emulator.
The combat checks pass for attack timing, blocking, sweeps, jump evasion,
range, exhaustion, practice mode, round scoring and enemy AI. The APK's
v2/v3 signatures, ZIP integrity and alignment pass. Runtime checks cover
practice controls, attacks, pause/resume and returning from the background.
No AndroidRuntime crash was recorded during those checks. Actual Honor 400
hardware has not been connected or tested.

