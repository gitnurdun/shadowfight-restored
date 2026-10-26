> **Fallback source:** This is an earlier Java remake. It is not the Unity-preserving restored game.

# Build and validation

## Recovery update — v1.1, 25 October 2026

The v1.1 update replaces the approximate fighter drawing with a renderer for the recovered original skeleton, weighted nodes, 84 body/head capsules and 247 triangles, plus original knife and staff geometry when equipped. Cloth uses approximate distance constraints. The UI now uses 109 exported original sprites and the original AGOpusBold font; 29 selected original clips, including the corrected 79-frame idle cycle and staff idle, drive the existing combat implementation.

- Compile/package/signature v2/v3 and alignment checks passed.
- Installed and launched on the API 35 emulator with no model-loading or AndroidRuntime errors recorded during the checks.
- Observed touch movement, repeated attacks, a 10-hit combo and enemy health loss using the recovered model renderer.
- Android home/resume and the in-game pause screen were exercised.
- Existing combat checks passed for attack timing, guard, sweep, jumping, range, stamina, practice, rounds and AI.
- An earlier recovered-model build on this software-rendered emulator measured 28 ms median frame time. This does not establish 60 FPS or physical-device performance.
- Honor 400 hardware remains untested. The combat engine is still a new implementation; the entire original game's content and behavior are not reproduced.

The original APK was independently installed and run through its downloader into Sensei's opening tutorial. It downloaded ANIMATIONS and VERSIONAL_CONFIGS successfully. The original APK remains unchanged. Static recovery verified 75 decrypted files; runtime recovery added six decrypted XML configs and 234 animation TextAssets. The combined strict motion inventory validated 334 unique clips / 12,498 frames.

The earlier v1.0 evidence below describes the first build and is retained as history.

Built on 24 October 2026 from the supplied APK in
`<server-workspace>/shadowfight/`.

- Installable package: `local.shadowfight.reborn`, version 1.0.
- SDK: compile 35, target 34, minimum 26.
- Pure Java/Android framework implementation; no native libraries.
- All 27 motion files and required media are included in the APK.
- APK ZIP integrity, alignment, and v2/v3 signatures pass.
- Combat checks pass: attack timing, blocking, sweeps, jump evasion, range,
  stamina, practice, best-of-three scoring and AI attacks.
- Installed and launched on Android 15 / API 35, x86_64 emulator.
- Observed practice mode, story selection, Shin duel, armory, touch movement,
  repeated strikes, a 12-hit combo and enemy health decreasing.
- Pause/resume and returning from the Android home screen work.
- No AndroidRuntime crash was recorded during these checks.
- Actual Honor 400 hardware was not connected or tested.

This is a new offline remake using recovered art, sound and motion data.
It does not reproduce the entire original game or its online services.
The original input APK remains unchanged on the server. The complete source
for the new implementation and its required assets are in the source archive.

## Working server emulator configuration

The installed emulator 37.3.4 crashed with the SwiftShader renderer. Runtime
testing succeeded using its headless executable with Lavapipe. The installed
user-space glibc 2.31 loader was used; the original SDK was not patched.

```
ANDROID_SDK_ROOT=$HOME/android-sdk \
ANDROID_EMULATOR_LAUNCHER_DIR=$HOME/android-sdk/emulator \
$HOME/glibc231/lib/x86_64-linux-gnu/ld-2.31.so \
  --library-path "$HOME/glibc231/lib/x86_64-linux-gnu:$HOME/android-sdk/emulator/lib64" \
  $HOME/android-sdk/emulator/qemu/linux-x86_64/qemu-system-x86_64-headless \
  -avd zv -no-window -no-audio -no-boot-anim -gpu lavapipe \
  -feature -Vulkan,-GLDirectMem -memory 2048 -cores 2 \
  -no-snapshot -crash-report-mode disabled
```

## Current runnable remake — v1.2

The APK now provides free weapons/upgrades, a fixed999999999 balance and full player stamina during attacks, jumps and blocks. All six existing duels are selectable immediately. Updated CombatTest checks verified that an exhausted player can attack and returns to full stamina, an exhausted enemy remains unable to make that attack, and player blocking does not spend stamina. Existing combat checks pass. Direct entry into Lynx without earlier victories was exercised in the emulator. Package version1.2/code3, SHA256 a55a0bbacc884324e01b962ff4612853e32aeb76f258b1aee35f30d4787364b7. Six duels remain a limited reconstruction. Original-engine restoration is experimental and not yet validated for menu/combat access. Honor400 hardware remains untested.

## Catalog integrity

All14 pristine selected story/support packs exactly match the salted catalog digest recovered from the native validator. This supersedes the earlier unresolved-hash notes. The verifier and report are supplied separately. A modified VERSIONAL_CONFIGS bundle has a new explicitly local digest; it is not labeled as matching the pristine publisher digest.

