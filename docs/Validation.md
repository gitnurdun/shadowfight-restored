# Restoration validation — 26 October 2026

The primary build preserves the supplied Unity engine, original rendering,
animation, combat, equipment, AI and interface. The earlier Java remake is a
separate fallback. These results come from an Android 15/API 35 x86_64 emulator
with ARM64 translation and Lavapipe software rendering. The physical Honor 400
was not connected, so its installation and performance are not confirmed.

## Build and recovered content

- Primary package: local.shadowfight.restored, displayed as Shadow Fight Restored.
- APK:737947037 bytes; SHA256
  575b3102fa86a0d97186dc5e0a8fa44f4e07f8c0f0691d25f9f3c335beeaf7fe.
- APK ZIP integrity, v2/v3 signatures and 16KB ZIP alignment pass. ARM64 ELF
  load-segment alignment is at least16KB. ARM32 is omitted from this clone.
- All 88 compatible non-video pristine packs match the reproduced catalog digest;
  their original total is650364978 bytes. Two required MP4 cinematics add12498566 pristine bytes and pass their separate ordinary-MD5 catalog checks. VERSIONAL_CONFIGS is intentionally
  modified in the installed cache, with its new local digest recorded separately.
- The supplied APK remains unchanged, SHA256
  4f1cecf66a154da43ee71bca509d351c7349be26a355d76276482d09ddc54375.
- A clean pipeline from an extracted restoration kit built and signed a second
  APK. The procedure regenerated the key, decrypted data, verified all 88 packs,
  repaired Android resources and applied the five documented native changes.
  The only ZIP payload difference was the cache index JSON; its semantic
  comparison is recorded in Clean-Reproduction-Verification.json.
- Minimal extraction retained duplicate-named TextAssets. The fresh decryption
  validated 78 XML and 7 text documents, improving the earlier68 XML/7 text export.
  The earlier resource archives are historical snapshots, not complete exports
  of all 88 packs. Six downloaded XML configs and 478 additional models were
  independently decrypted and round-trip verified in earlier stages.
- All 32 actual ARM64 predicate tests pass in Unicorn, including missing files,
  nested names, invalid strings, bounds and calling-convention preservation.

## Original-engine runtime checks

- Fresh isolated-package installation starts directly in the original dojo,
  showing level 52, large balances and the unlimited-energy indicator.
- No Internet permission or content download is required. The bootstrap prepares
  bundled packs, verifies copies against SHA256 receipts and preserves existing
  player profiles across subsequent starts/updates.
- The original equipment appears in actual combat after setting both the saved
  slot attributes and item Equipped flags. Normal Hermit tournament history
  records ZONE_2|Tournament|1, with registered HEAD HIT and enemy health loss.
- A one-coin native upgrade changed Flame Clubs UpgradeLevel5200 to 5210 and the
  saved balance1001000009 to 1001000008. A full restart preserved the upgrade and
  the prior fight loss, and refilled another 1000000 coins/gems.
- The native forge applied PERK_ITEM_SPECIAL_WEAKNESS_WEAPON, Aspect1882,
  immediately. The UI showed the new enchantment and all material counters zero;
  the saved item contained the actual perk. The confirmation template still
  displayed old material/time wording; the completed action used the patched
  free/immediate rules. Do not interpret the old wording as a required purchase.
- All seven main boss panels were inspected: each accepts the challenge and
  provides its enabled Fight button. First-five-guard completion in the clean
  seed enables direct access while each main boss remains undefeated.
- The fixed final APK actually entered Titan's original arena offline, with its
  original model, player equipment, combat controls, attacks and health loss.
  The earlier6.17MB movie prompt is absent. This validates fight entry, not a
  completed Titan victory or playback of the ending after a victory.
- During Titan combat, pause froze the timer at 96. Android Home followed by the
  actual launcher returned to that same paused scene at 96. Resume advanced it
  to 91 and combat continued. Quit displayed its original confirmation dialog.
- The final clean kit pipeline passed all 32 ARM64 tests, all 88 UnityFS digests,
  both movie digests, decryption and build/signing checks. All 3278 non-META-INF
  payloads other than cache-index JSON are byte-identical to the tested primary
  APK. Sorting the parsed cache indices proves semantic equality. All 23 current
  source scripts match the exact inputs used by the clean reproduction.
- Final release 575b3102 was tested on a fresh profile in the original Lynx
  arena. The unattended fight finished in a loss, awarded two coins, saved
  LossCount=1, and returned through the original result screen to Lynx's story
  dialogue and map. No automatic raid introduction or Android runtime fatal
  exception appeared. This checks the FightEnd path after disabling the single
  online raid tutorial launcher; it does not establish a boss victory.
- The identical release is published at <workspace>/original-input.apk; the older Java
  fallback is preserved separately as <server-workspace>/aygd-java-fallback.apk.
- The Act7 footer still sometimes labels the preceding chapter despite the
  correct Titan map and fight. This is a known cosmetic limitation.

## Limits of this validation

No full campaign completion or sustained performance benchmark is claimed.
The earlier unattended normal fight ended in a loss. Online accounts, publisher
cloud saves, advertisements, multiplayer raids and server events are not
reconstructed. Some legacy online buttons remain in the preserved interface.
The original Unity/C# project and original C# method bodies were not recovered.
This is a patched personal offline client, not a recovered Unity source project.

## Working server emulator setup

```sh
ANDROID_SDK_ROOT=$HOME/android-sdk \
ANDROID_EMULATOR_LAUNCHER_DIR=$HOME/android-sdk/emulator \
$HOME/glibc231/lib/x86_64-linux-gnu/ld-2.31.so \
  --library-path "$HOME/glibc231/lib/x86_64-linux-gnu:$HOME/android-sdk/emulator/lib64" \
  $HOME/android-sdk/emulator/qemu/linux-x86_64/qemu-system-x86_64-headless \
  -avd zv -no-window -no-audio -no-boot-anim -gpu lavapipe \
  -feature -Vulkan,-GLDirectMem -memory 2048 -cores 2 \
  -no-snapshot -crash-report-mode disabled
```

SwiftShader configurations crashed on this host. The user-space glibc loader
and direct headless binary resolved its host-library/display constraints. Native
startup and map transitions can be slow under software rendering; wait for a
stable screen before testing controls.220ms stationary touches were reliable.

## Earlier Java fallback

ShadowFight-Offline.apk is the 31MB v1.2 Java remake, package
local.shadowfight.reborn. It provides six immediately selectable duels, survival,
practice, free weapons/upgrades and full player stamina. Its combat tests cover
timing, guarding, jumping, sweeps, range, rounds and AI. Emulator checks covered
touch movement, combos, health loss, pause and Android home/resume. It uses
recovered models, selected clips, sprites, font and audio with newly written
combat rules. It is less faithful than the preserved original-engine build.
