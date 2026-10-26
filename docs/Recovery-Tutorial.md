# Recovering the supplied Shadow Fight 2 resources

This is a record of an actual recovery, not a generic proposed attack. The supplied APK remained unchanged. The recovery now includes 88 verified compatible content packs (650,364,978 pristine bytes), the recovered resource key, decrypted configuration/model files and an offline build preserving the original Unity engine. The first APK export decrypted 75 files; retaining duplicate-named TextAssets in the clean reproduction increased this to 85 (78 XML and 7 text). Later runtime and catalog extraction added six XML configs and 478 model XML files. Original C# method bodies and the Unity project have not been recovered. The final section explains the engine restoration and the accompanying restoration kit.

## 1. Identify and preserve the input

Work in `<workspace>/shadowfight` on the authorized server. Hash `aygd2_12479.apk` before doing anything to it and retain that copy. Inspect its manifest, signing certificate, ZIP entries, native libraries and Unity metadata.

The input identifies itself as `com.nekki.shadowfight`, version 2.46.0, version code 30000121. It uses Unity 2022.3.62f2 with IL2CPP metadata version 31. It contains ARMv7 and ARM64 libraries. Its third-party signing identity means we cannot assume it is an unmodified official distribution.

An IL2CPP APK contains native code and metadata rather than a normal managed game assembly. A declaration dump helps locate classes and native functions; it does not restore their original C# implementations. See the [Unity IL2CPP documentation](https://docs.unity.com/en-us/engine/7000.0/manual/programming-environment/scripting-backends/il2cpp) and [Il2CppDumper project](https://github.com/Perfare/Il2CppDumper).

## 2. Extract assets before guessing at encryption

The saved `tools/extract_assets.py` uses UnityPy to inventory and export Unity objects. The initial export produced 234 texture images, 415 TextAssets, 62 audio clips, five fonts, mesh dumps and other serialized data. Five Texture2D exports failed, so 234 images does not mean every texture was recovered. Sprite objects and UI scene information need additional reconstruction.

Classify each exported file:

- Recognized text/XML: inspect directly.
- BZip2 streams: decompress, checking for short prefixes before the stream.
- Motion clips: parse the binary frame structure rather than treating them as ciphertext. Ordinary fighters use 67 three-dimensional points per frame; other objects and fighters can use different rig sizes. Preserve the one-byte frame flags as opaque data: values such as 5 are valid, and their exact meaning has not yet been established.
- Files such as `mdl_body.bytes`, `mdl_head.bytes`, `mdl_skeleton.bytes` and `moves.bytes`: Base64-decode, then investigate the binary result. Their repeated prefix was useful evidence of shared processing, not proof of a specific cipher.

The first reconstruction used 27 parsed motion clips, backgrounds and original sounds. Its rough fighter graphics were a limitation of that reconstruction, not evidence that the APK lacked richer models.

## 3. Map metadata to native functions

Install analysis dependencies in a project-local directory:

```sh
python -m pip install --target work/python pycryptodome capstone py3rijndael
```

Use an appropriate Python environment with UnityPy and Pillow for asset work. On this server an existing virtual environment was reused without changing its shared dependencies.

Download the official Il2CppDumper v6.7.46 net6 release. Remote GitHub downloads timed out, so the successful approach was to download the release on the local computer and transfer the archive over SSH. Run the dumper with the ARM64 `libil2cpp.so` and its matching `global-metadata.dat`:

```sh
DOTNET_ROLL_FORWARD=Major ~/dotnet/dotnet tools/il2cpp/Il2CppDumper.dll \
  work/raw/lib/arm64-v8a/libil2cpp.so \
  work/raw/assets/bin/Data/Managed/Metadata/global-metadata.dat \
  reports/dump < /dev/null > reports/dumper.log 2>&1
```

The successful dump produced `dump.cs`, `script.json`, `il2cpp.h` and DummyDll files. A final console-input exception occurred after the outputs were written. Check the actual log and generated files instead of discarding them because of that final error.

The saved `tools/native_trace.py` reads ELF load segments, applies relative relocation addends to symbol references and annotates ARM64 disassembly using Capstone and `script.json`. Ignoring relocations made some global references look like zero pointers. Caller searches must also include tail-call `B` instructions, not only `BL`.

## 4. Find the key actually used by resources

Searching the APK revealed several crypto routines. Some used RC4, password derivation, DES or different AES keys. Those were false leads for these particular assets. Recovering a plausible key is insufficient: it must decrypt the resource being investigated.

The matching path in this build is:

| Native location | Observed role |
| --- | --- |
| `0x3493d8c`, `OMHODMCHILE.NPKGDBHJKCK` | XOR two initialized byte arrays and decode a hidden string |
| `0x3051fbc`, `IKMOIKFPPBF.NJLEHCMENAJ` | Obtain the hidden Base64 key string |
| `0x3051e58`, `IKMOIKFPPBF.BIJFFJILEBE` | Obtain the hidden Base64 IV string |
| `0x3051f08`, `IKMOIKFPPBF..cctor` | Base64-decode those strings into static key/IV fields |
| `0x305225c`, `IKMOIKFPPBF.PCPAFLBHBMF` | Decrypt a string through the generic AES helper |
| `0x30526c4`, `IKMOIKFPPBF.FHFADKNMJLB` | Corresponding byte-array decryption wrapper |
| `0x32eb118`, `IBMPAMLCCHF.FHFADKNMJLB` | Create AES, set key and IV, create decryptor |

These are virtual addresses in this exact build. Obfuscated names and addresses can change between releases. `reports/verified-resource-trace.txt` preserves the annotated instructions.

The metadata stores initialized arrays under names such as `$$field-2861`. For this version's metadata header, the string table offset is header word 6; field default records use words 16–17; default-value data starts at word 18; field definitions use words 24–25. Header words are little-endian 32-bit integers. Field and default records are 12 bytes. The relevant fixed 64-byte array type index is 39645. These build-specific values are encoded in the recovery script; they are not universal IL2CPP constants. Consult the [dumper's metadata definitions](https://github.com/Perfare/Il2CppDumper/blob/master/Il2CppDumper/Il2Cpp/MetadataClass.cs) when adapting it.

For each relevant pair:

```python
hidden = bytes(a ^ b for a, b in zip(array_a, array_b))
text = hidden.split('\ue44f'.encode('utf-8'), 1)[0].decode('utf-8')
value = base64.b64decode(text, validate=True)
```

The delimiter is Unicode U+E44F, represented by three UTF-8 bytes. The array's first byte is at its metadata data offset; there is no extra prefix to skip in these entries.

The verified resource values are:

```text
AES key (32 bytes):
29ec1346e220f5c4c00de924b6cd77d256b8969156a8dcbfd5e1417bb28acdbd

IV (16 bytes):
6075d5b844e1b5ab77c63797dcd0274e
```

Key arrays: `$$field-2861` and `$$field-2862`, global field indices 18905 and 18906. IV arrays: `$$field-2850` and `$$field-2851`, indices 18894 and 18895.

This did not require brute-forcing AES. We recovered constants that the installed client itself needs. To find the matching key among the decoded constants, the script used a CBC property: the IV affects only plaintext block one. A candidate key could therefore be tested against later blocks for an XML header continuation. The correct IV then restored the first block's XML declaration.

## 5. Decrypt and verify the complete files

Run the saved scripts from the project root using the Python environment containing the dependencies:

```sh
PYTHONPATH=work/python python tools/recover_resource_key.py
PYTHONPATH=work/python python tools/decrypt_resources.py
```

The transformation is:

```python
ciphertext = base64.b64decode(encoded_asset, validate=True)
padded = AES.new(key, AES.MODE_CBC, iv).decrypt(ciphertext)
plaintext = unpad(padded, 16)
```

Verification is deliberately stronger than seeing a few readable characters:

1. Require valid Base64 and complete AES blocks.
2. Require valid PKCS7 padding over the full decrypted file.
3. Require UTF-8, then parse XML where applicable.
4. Re-encrypt the padded plaintext and compare every ciphertext byte.
5. Record the output's SHA-256, size, source name and validation flags.

The resulting `reports/recovery-manifest.json` records 68 valid XML files and seven text files. The source scan considered `.bytes` exports; other exported suffixes are outside that scan. The recovered data includes body/head/skeleton and weapon scenes, 1,082 move definitions, AI settings, battles, stages and story. A source-file reference does not guarantee its referenced payload is bundled: of 568 unique animation filenames referenced by `moves.xml`, 97 matched the exported TextAsset basenames and 471 did not. Search other bundles and runtime downloads before declaring those missing permanently.

The model XML includes nodes, edges, triangles, capsules and weighted MacroNodes. It is richer than a set of flat fighter images. Reproducing the original appearance requires implementing those scene relationships and applying animations correctly.

## 6. Combine static analysis with execution

The existing Android 15 Google APIs emulator advertises `x86_64,arm64-v8a` and `libndk_translation.so`. That ARM translation allowed the original ARM APK to install and launch. The original game reached its first-launch UI; this is stronger evidence than an installation success alone.

The server's successful graphics setup used the headless emulator executable, Lavapipe, an existing user-space glibc loader, and Vulkan/GLDirectMem disabled. SwiftShader setups crashed on this host. The exact working launch command is preserved in `Validation.md`.

Install the original and start its declared activity, capture screenshots and logs, and inventory its data directories before and after progressing through startup:

```sh
adb install aygd2_12479.apk
adb shell am start -W -n \
  com.nekki.shadowfight/com.nekki.utils.activity.FCMNekkiUnityPlayerActivity
adb root
adb shell find /data/user/0/com.nekki.shadowfight \
  /sdcard/Android/data/com.nekki.shadowfight -type f
adb logcat -d > reports/original-logcat.txt
```

Use the SDK's actual `adb` path if it is not on PATH. Root access here is provided by the test image; this is not a procedure for rooting the user's phone. Compare hashes and file sizes rather than assuming every runtime file was downloaded: Unity also extracts metadata and library resources from the APK on first launch. Keep runtime identifiers and unrelated service credentials out of shared reports.

The original client requested a 13.12 MB content update. Its own downloader successfully obtained ANIMATIONS (6,355,087 bytes) and VERSIONAL_CONFIGS (6,757,881 bytes), and logged a successful restart. Both files are standard UnityFS bundles. `tools/extract_runtime.py` exports their TextAssets and records SHA-256 hashes: 234 animation assets and 18 configuration assets. The game's saved `packs.xml` also retains the bundle URLs and its expected hash fields.

Run the same decryption process against this separately preserved export:

```sh
PYTHONPATH=work/python python tools/decrypt_resources.py \
  extracted-runtime/TextAsset recovered-runtime runtime-recovery-manifest.json
python tools/inventory_motions.py
```

Six downloaded XML configs decrypt with the same key. The updated move table contains 1,104 moves and 569 unique animation references; 319 references now match the combined exports. A strict binary inventory validated 334 unique clips, containing 12,498 complete frames, finite coordinates, and no unexplained trailing bytes. These include 261 clips with 67-node rigs and additional 4-, 6-, 8-, 69-, 71-, 75-, 76- and 112-node rigs. The reference count and validated clip count answer different questions: some clips need not be referenced by the selected move table.

The original game reached Sensei's opening dojo tutorial after the download. Further content is split into additional packs. This is direct evidence that runtime inspection recovered useful material that was not in the initial APK export.

## 7. Put the recovered geometry and UI to use

`tools/preview_models.py` combines the named skeleton nodes, body/head geometry, MacroNode coefficients and existing point animations. All weighted nodes resolve in the inspected poses. The body and head together render 84 capsules and 247 triangles. The preview uses a simple orthographic camera and distance constraints for cloth; it does not claim to reproduce the original physics exactly.

The Android v1.1 reconstruction implements these recovered scene relationships in `OriginalModel.java`, including 14 original knife triangles driven by their weighted nodes. `tools/extract_ui.py` also recovered 109 original UI sprites. The new interface uses the parchment, logo, menu buttons, health bars, joystick and punch/kick controls, along with the recovered AGOpusBold font. These are actual recovered assets, not newly generated approximations. The move table also exposed a state-selection error: fists1_stance_idle is a start transition. Switching to the actual 79-frame stance_idle cycle corrected the guard stance. The playable package now includes 29 selected clips.

The game's combat engine is still a new implementation. Original move definitions, conditions, damage intervals and missing pack content remain valuable for improving fidelity. Recovering configuration and geometry does not recreate every original engine subsystem automatically. Physical Honor 400 verification is still unavailable.

## What this exercise teaches

### Later catalog recovery

The original client also saved a live plaintext catalog at its `gamedata/config_cdn.xml`. Its 799 bundle records include version ranges and sometimes comma-separated platform IDs. For this APK, selecting compatible Android entries identified ZONE_1 (36,541,298 bytes) and MODELS (81,039,982 bytes). Optional entries can have `Active=0`; that field alone is not a global availability test.

`tools/fetch_catalog_packs.py` retrieved the two public files. Its initial MD5 assertion was a failed assumption: the catalog's `Hash` is not raw file MD5, as confirmed by the same mismatch on bundles already accepted by the original client. At that stage the downloaded files retained `.partial` names because the catalog hash was unresolved. The later native-code trace below solved it; all 88 compatible packs now pass its exact digest. Their stated byte sizes match, UnityPy parses both bundles, and SHA-256 hashes record the exact inspected copies.

Those bundles contain 573 TextAssets. Applying the recovered AES key produced **478 additional model XML files**, all passing the same full validation as before. The remaining 95 assets are point animations. The combined motion inventory now validates **429 unique clips / 16,282 frames**; 410 of the updated move table's 569 references are available. This extends the earlier 334-clip stage rather than changing what was recovered then.

`tools/extract_catalog_media.py` exported all 173 textures, 1,383 sprites and eight audio clips in the first-act bundle without errors. The new model set includes original staff geometry and many other weapon, head, body and equipment definitions. These later-stage files are preserved separately from the earlier recovery archive. Those were the next questions at this historical stage. The subsequent work solved the hash and preserved the original engine instead of extending the approximate renderer.

The decisive step was linking resource ciphertext to the native code and initialized data that process it. Decompression, metadata parsing, relocation-aware disassembly, exact cryptographic verification and runtime inspection each solved a different part of the problem. The accompanying `Lessons-Learned.md` records the dead ends and practical fixes, including the early remake's visual shortcomings.

## Reproducing the catalog integrity algorithm

Static tracing found a separate bundle validator at ARM64 RVA0x3322dcc. It builds the bundle path and calculates a salted digest through0x3322c80 and0x1ce6860. First MD5 the complete file and encode its 16 digest bytes as32 uppercase hexadecimal characters. Append the UTF-8 salt decoded by0x34560cc, MD5 that combined byte string, then encode the result as uppercase hexadecimal. The salt is88 bytes: XOR-repeat the 64-byte initialized field6480 (metadata index22524,type39645) over the 128-byte field82 (index22856,type39613), decode UTF-8 and stop at U+E44F. Array names repeat; checking only the field name is insufficient.

The reproducible verifier is tools/verify_catalog_hashes.py on the server. Run python3 tools/verify_catalog_hashes.py from the project root after extraction and catalog downloads. It asserts the expected field names/types, computes every selected bundle hash, writes reports/catalog-salted-hash-verification.json and fails on any mismatch. The first verification covered 14 packs; the final expanded verification covers all 88 compatible non-video packs, each matching exactly. This validates the downloaded bytes against this catalog; it does not establish that the supplied APK or catalog is authentic or unmodified. Repacked bundles containing personal offline changes require their own new local digest and must not be described as matching the original publisher digest.


## Preserving the original engine: offline restoration

The earlier chapters record successive extraction stages. The later restoration preserves the supplied Unity executable and its original rendering, animation, AI and combat. Its isolated package has reached the original dojo, map and combat offline in the emulator. The separate Java remake remains available as a small fallback. The restored engine has verified native combat with equipped original gear, a normal tournament fight, renewable currency and a completed one-coin upgrade. Chapter access, crafting and final delivery are checked separately in Validation.md. The original Unity/C# project has not been recovered, and physical Honor 400 compatibility has not been confirmed.

Work from the unchanged input APK. Decode resources while preserving the eight original DEX files:

```sh
java -jar tools/apktool.jar d -s aygd2_12479.apk -o original-revival-edit
python3 tools/fix_decoded_resources.py
python3 tools/prepare_original_restore.py
```

The resource repair retains public IDs while renaming generated Material resource names rejected by the SDK compiler. The earlier manifest-only/binary-resource decoding approach failed. These scripts, native addresses and metadata indices apply to this input build, not arbitrary game versions.

Preparation changes the package to local.shadowfight.restored and adds RestoreBootstrap as launcher while retaining the original Unity activity and eight DEX files. It removes Internet, notification and ad-ID permissions, automatic SDK providers, and automatic analytics collection. The bootstrap copies its offline files into this separate application's own storage. Clean seed profiles copy only when absent, preserving later progress. Content files carry SHA256 receipts; a changed file is recopied even if its byte length is unchanged. The copy is hashed before replacement, and a partial copy cannot be accepted as completed content. Emulator account/device identifiers are excluded.

### Verify and package the complete cache

The recovered live catalog contains799 entries. Selecting compatible Android non-video entries for 2.46.0 produced88 distinct packs, totaling 650364978 pristine bytes. All 88 match the native salted digest reproduced above. VERSIONAL_ASSETS has no compatible selected entry and is not counted as a recovered pack. Preserve pristine downloads under work/catalog-bundles, separate from modified packaged copies.

```sh
python3 tools/verify_catalog_hashes.py
python3 tools/include_catalog_packs.py
```

Merely writing accurate versions, sizes, Reload flags and hashes did not remove the 34.85MB download gate. A reversible emulator-only return-true probe of AAPGCAPGBLG.LAAKLBPOMGP at RVA0x325f148 advanced into the original dojo, isolating the availability-policy check. That broad probe is not the production replacement.

The production replacement checks actual file existence. tools/pack_availability_code.py assembles ARM64 code that calls the game's own bundle-directory getter at 0x3104cb0, converts bounded UTF-16 ASCII directory/name strings into a512-byte stack path, rejects invalid or excessive names, and calls the game's imported libc access function through GOT0x41909e0. It preserves the calling-convention registers and stack. Native bundle attachment still checks content integrity. Using the resolved libc import also works under the emulator's ARM translation without assuming raw ARM system-call behavior.

tools/test_pack_availability.py executes those actual assembled instructions in Unicorn, with controlled directory/access callbacks. Thirteen cases cover present and missing files, nested names, null/empty strings, invalid characters, bounds and register preservation. Passing these tests supports the bounded replacement; the separate Android startup test confirms it works inside this client.

Four other documented ARM64 changes accept the clone's local profile data and prevent deletion of its local general configuration:

- CFJANHACGMG.FNIHLBLMOBP, RVA0x35991b4: return true.
- CFJANHACGMG.PBACHEOJBGG, RVA0x3599870: return true.
- CFJANHACGMG.AMELFFLEPNF, RVA0x3598984: return true.
- IDDIABHBDEE.FKLCGHKBKHK, RVA0x33871b4: return immediately.

tools/patch_offline_native.py starts from the pristine extracted library, maps addresses through ELF load segments and records original/replacement bytes and SHA256 hashes in reports/offline-native-patches.json. It intentionally changes local validation; it does not reproduce the original device-dependent save signature. ARM32 is retained in a project backup rather than packaged, so the clone consistently uses ARM64. Original ARM64 ELF load-segment alignment is at least16KB; that check alone does not establish the user's original installation/runtime failure.

### Prepare the local data and starting profile

After initial preparation and cache inclusion, run:

```sh
export PYTHONPATH=work/python
PY="$HOME/android_apps/zombie/work/venv/bin/python"
"$PY" tools/test_pack_availability.py
"$PY" tools/patch_offline_native.py
"$PY" tools/patch_restored_data.py
"$PY" tools/prepare_restored_profile.py
"$PY" tools/finalize_offline_cache.py
bash tools/build_original_restore.sh
```

The data transformation decrypts, modifies, re-encrypts and checks both item catalogs. Virtual purchases/upgrades use one coin or gem and zero delivery time. Setting shop prices to zero hid the original payment buttons, so a native ApplicationStart quest gives 1,000,000 Gold and 1,000,000 Bonus on each startup. Initial balances are 999,999,999 each. Fight entry costs become zero and replay flags are enabled. The forge has independent material/time/delivery prices; its real confirmation and saved enchantment must be checked separately from the price label. Combat damage, defense and timing remain original. BuildSettings disables the intro. The offline cloud-login notification completes through its existing Continue callback, preserving startup sequencing without waiting for a failed online login dialog.

The clean profile contains 789 virtual items, 49 visible/unlocked normal battle groups and 760 fight records, level 52, large coin/gem balances and the unlimited-energy item. Inactive Eclipse/intermission and early Act7 phase nodes are omitted: Locked=1 still renders a node and can cover its normal counterpart. The normal Act7 map uses C3_BOSS_TITAN, C3_Tournament, C3_Challenge, C3_Duel and C3_Survival. The advanced seed marks the first five guards of each main boss completed while leaving the boss undefeated; this is intended to permit direct boss access and must be checked in the UI. It copies encoded catalog UpgradeLevel values (for example5200), rather than assigning zero to every item. An explicit zone list excludes internal ZONE_T, whose missing ordinary map image caused a blank map in the first seed. Ownership and group locks alone do not prove all individual quest gates are open; validate access through the real UI.

Skipping the introductory quest also skips its variable initialization. The advanced seed must copy its ordinary May/Sensei Avatar_Assistant and Title_Assistant defaults; otherwise dialogs use an undefined portrait and the first localization title. Feature flags avoid beginner ranged/forge tutorials that assume a fresh level 1 profile. These values come from the recovered FirstGuardBeaten and feature quest actions, not guessed replacement assets.

The finalizer hashes actual packaged bytes and writes complete Size/Reload/Attach metadata. The modified VERSIONAL_CONFIGS has an explicitly local digest recorded in reports/local-cache-hash-changes.json, different from the pristine catalog digest. Running general preparation again resets pack copies and the bootstrap index, so rerun its dependent transformations and finalization afterward.

### Build and test in isolation

The build uses JDK21, API 35, D8, Apktool and SDK build-tools35. It preserves the eight original DEX files, adds bootstrap classes9.dex, aligns for 16KB native pages and signs with a development key. Anonymous Runnable classes avoid the direct Android boot-classpath LambdaMetafactory failure. A long build can outlast an SSH observation timeout while continuing; check its process, log, signature and final hash before starting another build.

Install beside the original and fallback. Clear data only for local.shadowfight.restored when validating a new clean seed. Use the documented Lavapipe emulator setup. A220ms stationary touch is accepted reliably; very short taps were often missed by this software-rendered Unity session. Startup, real maps, equipment, fights, free upgrades and restart persistence each need their own evidence. Passing signature/alignment checks and installing successfully do not establish behavior on the physical Honor phone. Online raids and publisher services are outside the recovered offline functionality.


### Validate actual gameplay and save state

The equipment is represented twice: Items/Item Equipped flags drive shop state,
while Warrior Weapon/Armor/Helm attributes select the combat loadout. Changing
only the flags made the shop show clubs while combat used fists. Set both and
confirm the actual model in a fight. The native save then records the same slots.

Check the recorded fight ID as well as its map label. Exposing Eclipse nodes
selected Tournament_ECLIPSEMODE instead of the ordinary tournament. Omitting the
inactive alternatives produced ZONE_2|Tournament|1, a proper 24-stage selection
and normal combat with registered hits and reduced opponent health. An unattended
loss is not evidence of a completed victory test.

The one-coin upgrade test changed Flame Clubs UpgradeLevel5200 to 5210. The actual
saved coin balance fell from 1001000009 to 1001000008. Payment opens a confirmation
dialog; accept it to perform the upgrade. Immediately reading users.xml can
precede the save flush. Background the game and inspect the later save, then
restart and verify both the item and fight history. Never replace the original
package's profile while testing the isolated clone.

### Rebuild from the restoration kit

Extract ShadowFight-Restore-Kit.zip into a fresh Linux directory. Follow its
README and manifests/tools.json to supply the unchanged input APK, Apktool3.0.3,
JDK21, Android SDK platform35/build-tools35.0.0 and the pinned Python dependencies.
Run bash tools/reproduce_restore.sh. This checks the input hash, extracts the
required Unity files and all TextAssets, recovers the key, validates decryption,
fetches/verifies the frozen catalog packs, repairs Android resource names,
prepares the isolated package, tests/applies the ARM64 patches, builds the data
and clean profile, then compiles, signs and checks the APK.

Different TextAssets can share a name. The minimal exporter preserves conflicting
contents with a stable SHA256 suffix and records source asset-file/path IDs.
The kit excludes account/device saves, signing keys, credentials, the original
APK and downloaded tools. It generates a new development signing key if absent.
This reproduces the procedure; different signing keys/environments produce a
different APK hash and cannot update a build signed by the server's key.

Read Validation.md for the actual clean-rebuild and runtime results. Physical
Honor 400 testing remains unavailable. The original online accounts, cloud saves,
advertising, multiplayer raids and publisher services are not reconstructed.


### Clean reproduction and additional recovery result

The kit was extracted to a new directory and executed against the hash-checked
original APK and the 88 pristine cached packs. It completed extraction, key
recovery, decryption, all pack digests, all 13 ARM64 tests, resource repair, data
preparation, compilation, alignment and signing. The rebuilt signed hash differs
because it uses a newly generated key; the comparison report records payload
differences separately. Keep verification of signed bytes distinct from semantic
equivalence of an unordered cache index.

The corrected minimal exporter retained ten XML documents that the earlier
name-only export had overwritten. Fresh decryption now validates78 XML and 7 text
documents instead of68 XML and 7 text. This improves the recovery count without
changing the historical results or claiming original source-code recovery.

The free-enchantment check applied the original weakness perk at Aspect1882,
with zero material counters and no delivery wait; users.xml records the actual
perk. Its confirmation template still displays the original material/time text.
The actual native action and saved result establish the applied rules more
reliably than that template. The upgrade and prior fight history also survived a
full restart, alongside a second native coin/gem refill.


## Final movie-gate diagnosis and verified restoration — 26 October

The compatible cache has 88 UnityFS packs plus two MP4 cinematics. The files
Shadow_fight_ending.mp4 and shadow_gate.mp4 total 12,498,566 bytes; unlike the
UnityFS packs, their catalog hashes match ordinary file MD5. Both have Active=0
yet are required by original story actions. Titan preloads the ending movie
before combat. The script tools/include_offline_videos.py validates their size,
MP4 header, catalog digest and SHA256, then adds them with Attach=0.

Bundling the movies initially left the same6.17MB prompt. Local static analysis
of a verified analysis copy traced PackSettingsExtension.IsDownloaded at
0x32cfa9c through0x325f010 to our availability replacement at 0x325f148. The
replacement rejected every period as a traversal precaution, including legitimate
.mp4 extensions. The original13 tests contained no valid extension case.

The fixed code permits internal periods while rejecting leading/trailing dots,
dot path components, consecutive dots, backslashes, invalid characters and
oversized paths. The old instructions reproduce false negatives for both
present movies. All 32 actual Unicorn cases pass after the fix, including
present/missing movie names, dotted nested names, invalid traversal and register/
stack preservation. Valid cases assert that the real access-call boundary is
reached; invalid paths assert that it is not.

A /video path getter at 0x3104d1c was an unhelpful lead: it supplies the embedded
Unity Resources fallback. The original ShowVideo action at 0x3322efc first uses
the bundle-directory getter0x3104cb0, checks bundles/name and creates the original
VideoPlayerController. The movies belong in gamedata/bundles. No video/story
action was deleted and no new blanket availability bypass was added.

The fixed APK was installed and actually entered Titan's original arena offline.
The original fighter equipment, Titan model, controls, attacks and health loss
rendered. Pause survived Android Home and reopening through the launcher with
the same timer value; Resume advanced the timer and continued combat.

The final clean pipeline ran from a newly extracted kit against the unchanged
input APK,88 pristine packs and both movies. It regenerated the key, recovered
78 XML/7 text documents and six runtime XML configs, passed 32 ARM64 tests and
all 90 cache-file checks, then built, aligned and signed the APK. Its3278 payloads
other than cache-index JSON and META-INF match the tested primary APK byte for
byte. The JSON indices are semantically equal; the 23 source scripts match the
tested kit inputs. Signed hashes differ because the clean rebuild generates a
new development key. Clean-Reproduction-Verification.json records the evidence.

The final starting profile exposes the seven main bosses; normal tournaments
still progress through their original stages. It does not reconstruct online
services or every internal quest branch. An Act7 footer-label issue and old
forge-confirmation wording remain cosmetic limitations. Physical Honor 400
installation/performance and ending playback after a Titan victory remain
unverified. The supplied original APK and pristine recovered inputs are retained.


### Keep the offline story flow separate from the online raid tutorial

A high-level seed with the first five guards completed also satisfies the old
RaidTutorialLauncherFightEnd condition: the story tutorial ended and Lynx's
second guard was beaten, while the raid tutorial was never started. Quitting
the tested Titan fight therefore opened Volcano's online raid introduction and
intercepted later navigation. The trigger comes from raid_quests, not the story
boss selection or a failed menu touch.

The offline data transform adds an always-false Equal(0,1) condition to this one
automatic launcher. It preserves manual raid actions and ordinary story quests.
The transformed encrypted asset is round-trip checked like the other modified
configs. Raid services remain outside the offline reconstruction. The release
is rebuilt from pristine resources with this change; the clean comparison
confirms the current 23 script inputs and resulting game payloads.


### Final release verification and delivery

On the final fresh-profile build, the original Lynx fight ran to a normal loss.
The result screen awarded two coins. Accepting OK returned to Lynx's ordinary
story dialogue and the boss map. After Android Home flushed the profile, the
saved ZONE_1|BOSS_LYNX|6 record had LossCount=1 and the wallet included that reward.
No StartRaidTutorialAndDownloadBundles quest or Android runtime fatal exception
appeared. This establishes a completed fight-result/return path, not a boss win.

The release was copied to a temporary file beside <workspace>/original-input.apk,
SHA256 checked, and atomically renamed into place. The old Java fallback was
preserved as aygd-java-fallback.apk. The original supplied APK was rehashed and
remains unchanged. The local download passed SHA256 and every ZIP member's CRC.
Final-Runtime-Verification.json and Server-Delivery-Verification.json contain
the synthetic test summary and delivered hash; private saves/logs are excluded.
The restoration kit includes the tested 23 scripts and the clean-rebuild report.
