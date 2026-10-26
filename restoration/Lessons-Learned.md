# Shadow Fight recovery — working lessons log

This log records findings from the whole recovery effort. Earlier work is reconstructed from saved scripts, reports, and test artifacts; new findings are added as they happen. A successful check is distinguished from an assumption. Passwords and unrelated embedded credentials are excluded.

## Later restoration work — 25 October

- **Unlimited supplies must change the spending rules.** Seeding a large balance alone is finite. The universal v1.2 build removes equipment/upgrade costs, keeps player stamina full during attacks, jumps and blocking, and makes all existing duels selectable without earlier wins. Enemy stamina and player damage remain normal so combat still has consequences. Updated combat checks and direct entry into the Lynx fight verified this stage. Expanding beyond its six-duel roster remains required.
- **The quota reset was observed, not assumed.** A live timer waited for the recorded reset, then account limits were checked again. Work resumed only after the five-hour window reported zero used. No paid credits or reset credit were consumed.
- **Preserving the original engine offers a second restoration path.** The supplied game runs in the emulator, its profile is readable XML, and compatible public catalog entries supplied all 14 selected story/support bundles, totaling 316,897,116 bytes. This made an original-engine offline prototype practical. It remains separate from the working universal remake until runtime checks succeed.
- **Check every native library before claiming an alignment cause.** All ARM64 libraries in the supplied APK had load-segment alignment of at least 16 KB. Its libraries are compressed and extractNativeLibs is true, so their ZIP offsets alone do not explain the Honor issue. Physical-device failure cause remains unproven.
- **The catalog Hash field is still unresolved.** Standard raw MD5 failed, and the UnityFS internal uncompressed-data-hash field was all zeros on the inspected bundles. The fetcher now records byte-size checks, complete UnityFS parsing and SHA-256 without pretending to verify the publisher's hash. It can reuse complete earlier downloads.
- **Raw manifest flags can conflict.** Combining Apktool's no-resource option with a manifest-only request left the manifest in binary form. A full resource decode with DEX preservation produced editable XML. Inspect file magic before printing a file as text; printing the binary manifest created a large, useless diagnostic dump.
- **Preserve resource IDs while fixing synthetic names.** The SDK's resource compiler rejected generated Material resource names beginning with '$'. Renaming those files and their XML/public-table names, while retaining public IDs, allowed the clone to build. Compiled original DEX files remain unchanged.
- **A direct Android boot classpath can lack lambda bootstrap stubs.** Java compilation of the bootstrap activity failed on LambdaMetafactory.metafactory. Replacing lambdas with ordinary Runnable classes avoided that dependency; D8 was given every generated class, including anonymous classes.
- **Stop build pipelines at the first failure.** An initial shell sequence continued from a compiler error into missing-class errors and a resource build. The dedicated build script now uses set -euo pipefail, producing a meaningful failure boundary.
- **An observation timeout is not a failed build.** A large APK build exceeded the SSH observation timeout but continued. Process inspection, the build log, the final artifact and signature verification established completion. The build was not restarted solely because observation expired.
- **Offline content needs the game's cache and save rules.** The original-engine prototype copied its content and started Unity, but displayed Error 576. Logs showed missing hash files and deletion of the general configuration before failed network retries. The fixes under test target the clone's local-file validators and that cache-deletion method; they do not establish that the prototype is already playable.
- **Trace obfuscated log strings through their data.** The cache-deletion log text was hidden in initialized arrays 425/426. Decoding that pair and locating references to the metadata fields identified the actual ARM64 method, even though a plain string-reference search found nothing.
- **Keep prototypes isolated and protect first-launch seeds.** The restored prototype uses a separate application ID, has no Internet permission, and keeps the supplied APK intact. Seed profiles contain no device/account identifiers. Bootstrap entries marked as seeds are copied only when absent, so reopening the game will not reset a player's progress.

## Game recovery and compatibility

- **An APK is not an Android Studio source project.** Unity IL2CPP ships native machine code plus metadata. Extracting assets and generating C# declarations does not recover original method bodies. Use metadata to locate native functions, then inspect those functions.
- **A remake can be runnable and still fail the user's goal.** The first build reused sounds, backgrounds and motion coordinates, but simplified fighters and UI looked poor. Passing installation and gameplay checks did not demonstrate visual fidelity. Recover original geometry, materials and UI before claiming a faithful resurrection.
- **Do not infer why a phone rejects a game from emulator behavior.** The supplied APK already includes ARM64 code and targets API 35. Emulator graphics failures are a separate issue; they do not establish the Honor 400 failure's cause.
- **Preserve the input and identify it precisely.** Keep the original APK unchanged and record its hash, package, version and signing identity. The supplied APK has a third-party signing identity; do not assume its provenance is official.

## Asset extraction and formats

- **Unity assets contain multiple layers.** Inventory object types before deciding that files are unavailable. The inventory found textures, sprites, audio, TextAssets, fonts and serialized scene objects.
- **Unfamiliar data is not automatically encrypted.** Some TextAssets contain BZip2 data, sometimes after a short prefix. Decompress recognized formats before testing ciphers.
- **Animation coordinates are not finished fighter graphics.** The recovered motion clips describe 67 points per frame. Mapping useful joints enabled basic animation, but did not recreate the original mesh or visual style.
- **Base64 is an encoding, not the encryption itself.** The model files decode to ciphertext. Decoding alone cannot recover their contents.

## Reverse engineering and key recovery

- **Literal searches alone missed the real key.** The useful strings were hidden in pairs of 64-byte initialized arrays. Native code XORs the pair, decodes UTF-8 and stops at U+E44F.
- **Trace resource consumers, not every cryptography function.** The APK contains RC4, DES, password-based encryption and several AES helpers for different purposes. A valid key from one subsystem did not decrypt model files.
- **Generated metadata tables require exact offsets.** A first pass used the parameter table where the field table was required. Parsing the correct field definitions and default-value records located the initialized arrays.
- **Do not invent a prefix in initialized array data.** This build's relevant array bytes start directly at the metadata default-data offset; skipping bytes corrupts the decoded string.
- **Resolve ELF relocations before labeling native references.** Some global pointers are zero in the on-disk image and populated by relocation. Applying relative relocation addends made key-field and string references visible in the disassembly.
- **Search tail calls as well as ordinary calls.** A scan restricted to ARM64 BL misses functions reached through B tail calls. Caller graphs need both where appropriate.
- **Use CBC structure to separate key and IV testing.** For CBC, the IV changes only the first plaintext block. Testing later blocks revealed the matching key; testing XML headers then identified the IV.
- **Verified breakthrough:** one recovered 32-byte key and 16-byte IV decrypted 75 TextAssets: 68 XML documents and seven text documents. Every output passed PKCS7 padding and exact re-encryption checks; all 68 XML documents parsed successfully. The resources include models, skeletons, moves, AI settings, stages, battles and story. There are 1,082 Move elements in moves.xml; their referenced animation payloads are a separate completeness question.
- **A tool's final exit status can mislead.** Il2CppDumper generated its outputs successfully, then failed while waiting for a keypress on redirected input. Inspect the log and produced files before concluding the dump failed.
- **Do not dump every recovered hidden string.** Only export constants needed to reproduce this resource recovery. Unrelated strings may include service credentials or identifiers.

## Toolchain and emulator troubleshooting

- **Remote downloads can fail while local downloads work.** Repeated remote GitHub download timeouts were resolved by downloading the official release locally and transferring the exact archive over SSH.
- **Keep dependencies scoped to the project.** Install reverse-engineering Python packages under the project's work directory rather than changing the server's shared Python environment.
- **Old host libraries can block modern Android tools.** The emulator required the existing user-space glibc loader and library path on this server.
- **Headless rendering needs an explicitly working backend.** SwiftShader configurations crashed and host graphics lacked a display. Starting the headless emulator binary directly with Lavapipe, Vulkan and GLDirectMem disabled, booted successfully.
- **Installation is not runtime validation.** The remake was exercised for movement, hits, damage, a 12-hit combo and pause/resume, with logs checked for crashes. This establishes those tested behaviors only, not behavior on the physical Honor phone.
- **First-run system overlays affect automation.** Android's full-screen explanation initially covered the game. Dismiss the system overlay before interpreting screenshots or touch behavior.
- **Check native translation before abandoning an x86 emulator.** This Google APIs image reports x86_64 and arm64-v8a with libndk_translation.so. The original ARM APK installed successfully. Installation alone still does not prove its native engine can run under translation.
- **Dynamic confirmation:** the original native engine did run and displayed its startup UI. After the terms screen and Google Play Games warning, it explicitly requested a 13.12 MB content download. This establishes that the supplied APK expects additional content; the download itself still needs verification.
- **The runtime download completed successfully.** ANIMATIONS (6,355,087 bytes) and VERSIONAL_CONFIGS (6,757,881 bytes) were saved in the game's bundle directory. UnityPy read both as ordinary UnityFS bundles, exporting 234 animation TextAssets and 18 configuration TextAssets. Six downloaded configs decrypted with the recovered resource key. The updated move table has 1,104 Move elements; 319 of its 569 unique animation references match APK-plus-download exports. The original engine reached Sensei's opening dojo tutorial.
- **Recover geometry before approximating appearance.** Combining the original skeleton, weighted MacroNodes, body capsules and head triangles produced a recognizable fighter silhouette. The inspection render resolved every weighted node and drew 84 capsules plus 247 triangles. Distance constraints approximate cloth; matching its original inertia still needs more work.
- **Names inside atlases matter.** An atlas image is not a ready-made UI component. Export individual Unity Sprites and retain their source names. The UI pass recovered 109 sprites, including original parchment, menu icons, buttons, health bars and attack controls.
- **Extracted fonts must actually be used.** The earlier remake copied a font but still drew text with Android's generic condensed font. A preview of all five extracted fonts identified AGOpusBold as a suitable original UI font; the updated renderer loads it explicitly and measures button labels to fit.
- **Treat animation flags as data until their semantics are established.** A strict validator incorrectly assumed only frame flag 1 (then 0/1) was valid. Real files also contain flag 5 with the same point-count/coordinate record layout. Preserve the flag byte, validate lengths and finite coordinates, and avoid inventing its meaning.
- **Full validation confirmed 334 unique clips and 12,498 frames.** All identified point-animation candidates consumed their complete files and contained finite coordinates. Rigs have 4, 6, 8, 67, 69, 71, 75, 76 or 112 points. Rig size alone does not identify whether a clip can be applied to the ordinary fighter skeleton.
- **A plausible animation filename can still describe the wrong state.** The first remake looped fists1_stance_idle, but the recovered move table identifies it as a start-stance transition. The actual stance_idle clip was already available. Using its 79-frame cycle produces the intended guard stance. The knife start-stance fallback uses its final segment until the real knife idle clip is recovered.
- **Weapons also have recoverable geometry.** The original knife model contributes 14 triangles driven by weighted nodes referring to the ordinary rig. The latest renderer uses these for twin knives instead of simple blade lines. Staff rendering is still approximate.
- **Catalog flags need behavioral evidence.** The live catalog contains many entries with Active=0, including optional content. Filtering that flag as if it meant globally unavailable hid the useful packs. Version range, comma-separated platform IDs and the client's observed download behavior are more reliable selection evidence. Further fetches are checked against catalog size and hash.
- **A field named Hash does not establish its algorithm.** Two additional bundles matched their catalog sizes but failed a guessed file-MD5 comparison. Comparing bundles already downloaded and accepted by the original client showed the same mismatch. The catalog hash is therefore not ordinary file MD5. Its algorithm remains unresolved; preserve this distinction rather than reporting corrupt downloads or verified publisher hashes. The additional files parse as UnityFS, and 478 extracted model XML documents independently passed padding, XML parsing and re-encryption checks.
- **Additional recovery is now concrete.** MODELS and ZONE_1 yielded 573 TextAssets: 478 encrypted model documents and 95 binary animation files. First-act media export recovered all 173 textures, 1,383 sprites and eight audio clips without errors. The combined motion inventory grew to 429 clips / 16,282 frames, with 410 of 569 referenced filenames available. These are later-stage results; the earlier 157 MB recovery archive remains a labeled snapshot of the previous stage.
- **Quoting across two shells can silently defeat a test setup.** A first preference-edit command did not actually equip the test weapon. Pulling the isolated emulator preference XML, changing it with an XML parser, pushing it back and checking the resulting screenshot produced a reliable equipment test. The original test preferences were restored afterward. No physical phone profile was touched.
- **Avoid broad diagnostic output.** Printing every catalog entry whose attributes contained a zone name also printed irrelevant store settings and produced a very large report. Filter by the actual URL field and specific resource names, and print only the chosen entries or counts.
- **Performance evidence needs context.** An early recovered-model build on the software-rendered emulator showed a 28 ms median frame time. It did not establish 60 FPS or performance on Honor hardware. Do not present a low modern jank counter alone as proof of smoothness: the legacy counter used a different deadline and reported a very different percentage.
- **Runtime files are not necessarily downloads.** First launch placed IL2CPP metadata and framework resources in external storage. Compare them to APK entries before counting them as recovered network content.
- **Export coverage is narrower than game coverage.** The move definitions reference 568 unique animation filenames; 97 match the exported TextAsset basenames. Search bundles and runtime files for the other 471 before claiming they are absent from the game entirely.
- **Check environment variables before constructing search paths.** CODEX_HOME was unset in the local shell. A recursive automation search using the empty variable searched the wrong location and was stopped. Use the verified absolute Codex directory as the fallback.
- **A heartbeat needs an explicit destination.** The first automation request omitted its chat destination and was rejected by schema validation. Setting destination to thread created the requested follow-up successfully.

## Working practices

- Record each significant result promptly, including failures that change the approach.
- Keep a reproducible script and evidence for every claimed recovery; do not label guesses as decryption success.
- Static analysis, emulator execution, downloaded-file inspection and logs answer different questions. Combine them and report where architecture, networking or obsolete services prevent a check.
- A skill catalog request returned HTTP 403. No skill was installed. Useful work continued with the already available tools; optional catalog failure was not a reason to stop.


## Bundle integrity breakthrough — 25 October 04:45 China time

- The separate Unity bundle validator was traced from IAGNPGHOAIC.PBACHEOJBGG (0x3322dcc) through its file digest helper. The catalog algorithm is uppercase hexadecimal MD5 of the uppercase hexadecimal file MD5 followed by an 88-byte UTF-8 salt. The salt comes from ArrayCopy64 field6480 (metadata index22524) XOR-repeated against ArrayCopy128 field82 (index22856), stopping at U+E44F. All 14 pristine selected downloads exactly match their catalog hashes. Earlier reports saying the hash was unresolved describe the earlier state.
- Generated field names repeat across array classes and assemblies. Selecting field82 by name alone chose unrelated data and failed verification. Restricting by the exact metadata field index and expected type fixed it. The XOR helper repeats its 64-byte key over the entire second array; truncating a 128-byte encoded string to 64 bytes loses the delimiter and salt.
- Changing cached pack versions to each catalog MinVersion did not remove the 34.85MB download dialog. Do not treat that hypothesis as a successful fix. All 14 files are present; original-engine menu/combat validation remains incomplete. The existing runnable universal build remains separate.


- A complete cache index with accurate Size, Reload and the modified bundle's new salted hash was tested as a second hypothesis. It still reached the same34.85MB prompt after dismissing the cloud warning. This is a confirmed unsuccessful fix, not restored offline gameplay. The next comparison uses the unmodified emulator client to observe the normal Zone1 cache-registration/download behavior.
- Touch-driven Unity startup takes longer than a short fixed wait. Captures after pressing warning OK often show the splash before the next dialog arrives. Check the next stable screenshot and logs before claiming a button was missed or a loader fix succeeded.


- The unmodified client reached the real Kenji battle in the recovered bamboo arena, confirming native combat rendering on the API 35 emulator. The first unattended fight ended in a loss with zero successful strikes. A burst of short taps is not evidence that Unity accepted attacks; the next check uses longer stationary touches, as already required for reliable dialog presses. This is UI automation troubleshooting, not proof that the fight mechanics or recovered assets failed.


## Original engine offline dojo reached — 25 October after quota reset

- A reversible emulator-only probe changed AAPGCAPGBLG.LAAKLBPOMGP (RVA0x325f148) to return true. The clone then passed its download gate and reached the original dojo offline. The visible UI confirmed level 52,999999999 coins,999999999 gems and the winged unlimited-energy indicator. This identifies cache-availability policy as a real blocker; it does not prove all missing optional packs exist or that every story/equipment screen is usable. The probe leaves project/APK/native source files unchanged and records installed-library backup and replacement hashes in reports/pack-availability-probe.json. Production changes must be narrower or ensure every reported available pack is actually present.
- Longer220ms stationary touches produced a visible decrease in Kenji's health in the saved original-client test. Short taps previously produced no successful strikes. Match input duration to emulator frame processing and validate actual health changes.


- The blank first map came from exposing internal ZONE_T with the broad prefix-based unlock rule. Its title was absent from localization and its stage definition had no ordinary map image. Selecting the real ZONE_1 displayed the original map correctly. The seed generator now uses an explicit list of supported story zones and Punchbag.
- Replacing URL comparison with hash comparison alone did not remove the download gate. The successful broad bypass was kept only as a probe. A new replacement uses the game's own bundle-directory getter and imported libc access function to check actual paths, with bounded UTF-16-to-ASCII conversion, path-character checks and preserved ARM64 calling-convention registers. Unicorn executed the actual assembled code against13 boundary, missing-file and nested-pack cases successfully. Android runtime validation is still required.
- Broader resource recovery selected88 compatible non-video packs. All 88 pristine downloads exactly matched the reproduced catalog digest. They total 650364978 bytes before the local VERSIONAL_CONFIGS modification (the packaged modified cache is4888 bytes smaller). VERSIONAL_ASSETS had no compatible selected entry; its absence must not be disguised as a verified download.


- The file-presence predicate also passed in the Android emulator and advanced into the normal map/story interface without the forced-download dialog. The fresh build integrates it instead of the broad always-true probe. All paths come from the game's own storage getter; the replacement uses its resolved libc access import rather than assuming ARM64 system calls behave normally under x86 native translation.
- Item UpgradeLevel is an encoded progression value such as5200 for a level 52 item. Seeding every owned item with 0 discarded that catalog state. The new seed copies each definition's value and equips a matching level 52 weapon, armor and helm. Fresh-install UI validation will confirm the resulting equipment.
- The encrypted TextAsset named eng is a credits document, not the main English localization table. Searches there cannot locate startup cloud-save warnings. Asset names need content/path confirmation, just as metadata field names require type/index confirmation.


- A high-level seed must also include the ordinary tutorial's UI defaults. Skipping FirstGuardBeaten left Avatar_Assistant_* and Title_Assistant undefined, producing a blank portrait and the wrong first localization title. The seed now initializes the same May/Sensei assets and names as that quest, plus feature tutorial flags. This diagnosis comes from the recovered quest actions; a clean-install check is still required.
- A cloud-login dialog has an ordinary quest completion callback. Replacing only that offline notification's actions with its existing Continue callback preserves startup sequencing. Deleting the quest outright could leave the caller waiting.
- Forge prices are independent of item purchase/upgrade prices. The forge XML has separate material and delivery fields; those need their own zero-cost transformation for free enchanting.
- The SSH connection and emulator can expire between long sessions. Reconnect with the authorized host key, check actual adb state, and restart the documented emulator configuration before interpreting screenshots or installation results. A denied sandbox socket required the already authorized network execution path.

Fresh native clean startup directly reached dojo with no cloud warning. Shop opened without the earlier tutorial popup and rendered original Flame Clubs at level 52 with UNEQUIP, confirming the intended weapon is owned/equipped in the native UI. This does not yet prove its combat model/behavior. Latest APK hash294420392b05648b515eb05ca1ba7beb961c86929fee5fa8251b55025a3c8bd9.

- The corrected May/Sensei seed variables were confirmed in a real May upgrade-reminder dialog, which now renders both her portrait and name correctly. The reminder itself can block scripted navigation; dismiss it before interpreting later taps as combat inputs.
- The restored clone entered Act2 Tournament against Rogue and rendered the original arena, fighters and combat controls. The shop's equipped state alone does not establish the equipped combat model: the current player looked unarmed, despite a seed inspection confirming only the Flame Clubs weapon is equipped. This remains an investigation, not a completed equipment fix.
- Unlocking every internal Act7 battle group makes quest-phase alternatives overlap at identical map coordinates. The final chapter needs deliberate phase/group selection or layout treatment. A bulk Locked=0 transformation is not sufficient evidence of a finished chapter map.

## Equipment and battle-variant diagnosis — 25 October

- The combat loadout also lives in Warrior's Weapon/Armor/Helm attributes. Setting only Items/Item Equipped flags made the shop show Flame Clubs while combat still loaded Fists/Body/Head. Updating both representations in the isolated emulator profile visibly restored the original clubs, armor and helmet in combat. Runtime XML confirmed the selected slot names persisted after restart.
- The first late-game tests accidentally selected Eclipse alternatives. Runtime fight history identified ZONE_2|Tournament_ECLIPSEMODE and ZONE_7|C3_BOSS_TITAN_ECLIPSEMODE, despite the ordinary-looking overlapping buttons. The seed must not force ordinary, Eclipse and intermission variants visible together. They share map coordinates and represent different progression states. Normal/Eclipse selection requires validating the actual recorded fight ID, not just the icon title.
- The item catalog confirms UpgradeLevel is an encoded progression key: e.g.300,310,320,330 followed by400. The loadout issue was separate from that encoding. Some earlier Eclipse tests logged extreme/infinite damage; normal-mode combat must be checked separately before interpreting this as a general combat failure.

## Normal battle visibility and shop-price checks

- Setting Locked=1 does not hide a saved battle node. It still renders and can cover its normal counterpart, producing missing/white locked icons and selecting the wrong gate. Omitting inactive Eclipse/intermission/earlier-phase entries entirely restored the proper normal icons and enabled the ordinary24-stage Hermit tournament. The temporary boss-win-count seed did not solve that overlay and was reverted. No atlas replacement was needed.
- Map transitions across several chapters take longer than a single adjacent transition. A capture five seconds after jumping fromAct2toAct7 can show the destination background while the title, highlighted dot and selected fight still belong toAct6. Wait for a stable final state before interpreting chapter labels or pressing Fight.
- The current normal fight history records ZONE_2|Tournament|1. Screenshots show the restored Flame Clubs/armor/helm, registered HEAD HIT feedback and reduced enemy health. The unattended remainder ended in a loss; this is not a claimed victory or a completed endurance test.
- Minimal TextAsset export found different assets named eng. The earlier export overwrote duplicate names. The new exporter retains conflicting contents with a stable SHA256 suffix and records asset file/path IDs. A familiar name alone cannot identify the localization or credits document.
- Zero item/upgrade prices hide the original shop's payment controls. A live Sai upgrade preview showed the stat increase but no actionable purchase button, and clicking its label did not upgrade it. This is a confirmed UI issue to resolve before claiming working free upgrades.

- Zero shop/upgrade costs hid the payment controls. The replacement build uses one-token virtual costs with zero delivery time, plus the original GiveCurrency quest action to refill coins and gems at application startup. The refill has been verified in the real UI and quest log (+1000000 of each); the actual one-token upgrade and save/restart checks are still pending.
- ForgeEnabled is a tutorial-state variable, while ShowForge is an actual action that enables the feature. The original forge quests demonstrate both. The advanced startup quest now calls ShowForge and keeps the tutorial-complete flag; crafting still needs a UI check.
- File size alone cannot identify updated config content: a32-character hash can change while the XML size stays identical. The bootstrap now records expected source SHA256 receipts and verifies copied bytes, so changed same-size files are replaced while existing seed profiles are preserved. The updated build booted with the old test profile; deeper upgrade/persistence validation remains.
- The revised minimal extractor completed415TextAsset exports after retaining duplicate names by stable content-hash suffix. The catalog verifier again passed 88/88 and the ARM64 predicate tests again passed 13/13. The frozen manifest now marks all 88 catalog hashes verified, superseding stale initial report flags.
- Latest usage lookup became unavailable during the token-shop check. Project work stops until a later successful lookup verifies the quota reserve; current state and pending screenshot were checkpointed without spending paid/reset credits.

## Supply and upgrade validation — 25 October, resumed session

- A successful quota lookup allowed work to resume. The one-coin upgrade has now completed in the original engine: Flame Clubs changed from encoded UpgradeLevel5200 to 5210, and coins fell from 1001000009 to 1001000008. The native confirmation dialog must be accepted; tapping the initial price only opens it.
- Saving can lag behind an on-screen change. An immediate users.xml pull still showed the old balance; moving the app to the background flushed the upgraded item and one-coin payment. Evidence should include the later saved state, not only an early file read.
- The forge opened with its original recipes and a FREE label, but the confirmation still quoted72 green materials and20 minutes. A price label alone does not prove free crafting. Investigate the separate confirmation calculation before claiming enchanting works without supplies or waiting.
- Screenshot transfers must finish before inspection. Viewing an existing local filename before TRANSFERRED displayed an older capture; waiting for transfer completion revealed the actual upgrade confirmation. Use distinct capture names and completed-transfer checks.


- The free-enchantment test completed after accepting its confirmation: the
  original weakness perk, Aspect1882, appeared in the UI and saved item while
  material counters stayed zero. The confirmation template's old72-material/
  20-minute wording did not reflect the patched native cost/time. Record this
  cosmetic limitation instead of mistaking a template for the executed price.
- Full restart preserved the 5210 upgrade and ZONE_2|Tournament|1 loss history;
  balances rose to 1002000008 coins and1001999999 gems. This verifies persistence
  and repeated native refill rather than a one-time seeded balance.
- The first restoration-kit packaging attempt failed because Validation.md had
  never been copied to remote deliverables. Uploading the documented inputs
  resolved the explicit allowlist failure; the packer does not silently omit a
  missing required report.
- A kit extracted into a fresh directory completed the full build. Its minimal
  exporter recovered78 XML and 7 text documents: ten XML files had been lost by
  the older name-only export. The signed rebuild uses a new development key;
  compare individual payloads separately from the signed APK hash.

## Late-game movie requirement

- The seven map backgrounds and an enabled Titan button did not prove that its
  fight could start. Pressing Fight produced a6.17MB download gate. Unity logs
  named Shadow_fight_ending.mp4 as a forced prerequisite; the earlier88-pack
  recovery had deliberately excluded videos. Validate actual entry after a map
  unlock, not just the map's enabled controls.
- The frozen catalog contains two compatible Android cinematics:
  Shadow_fight_ending.mp4 and shadow_gate.mp4, totaling 12498566 bytes. Both were
  recovered and match their catalog's ordinary file-MD5 digest. The UnityFS
  packs use a different, salted double-MD5 algorithm. Do not apply one format's
  checksum assumptions to every catalog entry.
- Both movie records have Active=0, yet Titan explicitly requires the ending.
  Active=0 therefore does not mean the file is unnecessary. The revised pipeline
  includes both movies with Attach=0 and separate SHA256 verification, without
  trying to parse MP4 files as UnityFS bundles.
- The clean rebuild's only initial payload difference was JSON ordering in the
  cache index. Sorting its parsed entries by path proved semantic equality;
  all 3276 other non-META-INF ZIP payloads matched byte for byte. Signed APK
  hashes still differ because the clean rebuild generated a new signing key.

## Movie gate regression — 26 October, local analysis during server outage

- SSH timed out twice, so analysis continued from the hash-verified local
  restored APK in a separate ANALYSIS-ONLY tree. It contains the five earlier
  patches and must never replace the pristine input used by the build script.
- PowerShell's existing .NET runtime can invoke Il2CppDumper by reflection.
  Its first attempt looked for config.json beside PowerShell; setting
  APP_CONTEXT_BASE_DIRECTORY to the workspace dumper directory resolved that.
  The final redirected-console ReadKey exception occurred after valid outputs.
  Capstone, Keystone and Unicorn were installed only under work/local-analysis.
- A /video getter initially looked like a different download directory. The
  actual ShowVideo action first checks bundles/name, and uses /video only for
  the embedded Unity Resources fallback. Trace the consumer before moving files.
- The true remaining Titan gate was our own availability replacement: rejecting
  every period also rejected valid MP4 filenames. The old13-case suite passed,
  yet both present movies returned unavailable. The local fix permits internal
  filename periods while rejecting traversal/dot components and preserving the
  existing bounds, backslash and ASCII checks. All 32 actual ARM64 cases pass,
  including present/missing movies and verification that valid names reach libc
  access while invalid paths do not. Android runtime validation is still pending;
  the server was unreachable, so the fix has not been uploaded or packaged.
- Usage reached85% used /15% remaining. Work stops at the requested reserve;
  reset is2026-10-26 03:59:20 China time. No paid or reset credits were used.


## Verified final predicate and native lifecycle checks — 26 October

- The server became reachable again after the quota reset. The fixed predicate
  and32-case tests were uploaded; all 32 tests also passed on the server. The
  pristine-input native patch and build/signature/alignment checks succeeded.
- The resulting APK entered the real Titan battle without a download prompt.
  This confirms the period rejection was the gate, rather than a missing movie,
  bad movie directory or a need to remove original quest actions.
- Native pause/home/launcher/resume retained the exact paused timer96, then
  resumed to 91 with combat continuing. An install or dojo-only test would not
  have established this lifecycle behavior.
- A fresh final kit pipeline reproduced the tested payloads:3278 files other
  than cache-index JSON and META-INF match exactly, and parsed index entries
  match after sorting. All 23 source scripts match the clean run's input hashes.
  Signing keys differ, so signed-APK hashes differ as expected.
- A Bash-style heredoc failed in PowerShell. Saving the Python helper as a
  workspace file and executing it avoided cross-shell syntax and quoting issues.


## High-level profile and automatic raid tutorial

- Quitting the real Titan fight invoked RaidTutorialLauncherFightEnd, because
  the advanced seed had already completed Lynx's second guard. Volcano's raid
  dialog then intercepted scripted story navigation. Logs identified the exact
  quest; the apparent failure to enter Lynx was not a broken map button.
- The offline transform now adds Equal(0,1) to that single automatic launcher
  in raid_quests. Manual raid actions and main story quests are retained, while
  publisher raid services remain unsupported. The release was rebuilt from the
  pristine source and tested with a fresh isolated profile.


## Final result-flow and delivery checks

- The earlier short attack script spent much of its time waiting for the native
  loading screen. Its first report showed no completed fight, so it was not
  evidence for the FightEnd fix. A later screenshot showed the actual loss and
  reward screen. Accepting the observed OK button, rather than a remembered
  coordinate, returned to the normal Lynx story dialogue. The saved loss count
  became one, the two-coin reward persisted, and the online raid introduction
  did not run. No boss victory is claimed.
- The server command bridge expired between sessions. Reconnecting with the
  existing verified host key restored access; completed artifacts remained on
  disk. Local APK hashing and ZIP checks confirmed the large download had fully
  completed before it was promoted into outputs.
- A release must reach the user's requested filename. The old Java fallback at
  ~/aygd.apk was still present despite the newer native build succeeding. It was
  preserved separately, then replaced atomically with the verified native APK.
  The original input was independently rehashed and remained unchanged.
- Documentation and the source kit are release artifacts too. Fresh extraction
  manifests replaced older overwritten-name counts, the kit was repackaged with
  final evidence, and delivery hashes were regenerated only after those changes.
