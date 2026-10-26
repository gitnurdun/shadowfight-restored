# Shadow Fight original-engine restoration kit

This kit contains the extraction, decryption, native patch definitions, clean
profile generator and Android bootstrap used for this personal offline build.
It does not contain the original Unity/C# project or recovered C# method bodies.
The preserved original executable supplies rendering, AI, animation and combat.

Use a **fresh directory** on Linux. Retain the supplied APK as `aygd2_12479.apk`
at this directory's root. Its SHA256 must be
`4f1cecf66a154da43ee71bca509d351c7349be26a355d76276482d09ddc54375`.
The preparation script checks this before extracting anything. It never changes
the original APK. Native patches additionally verify the original ARM64 library.

The tested server uses JDK21 at `~/.sdkman/candidates/java/current`, Android SDK
at `~/android-sdk`, platform35 and build-tools35.0.0. Install Apktool3.0.3 as
`tools/apktool.jar`; its recorded hash is in `manifests/tools.json`. The official
release is https://github.com/iBotPeaches/Apktool/releases/tag/v3.0.3.

Create a Python environment and install `requirements.txt`. The actual observed
versions are recorded; the server reused an existing Python3.13 environment and
placed additional analysis dependencies under project-local `work/python`.

```sh
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements.txt
export RESTORE_PYTHON="$PWD/.venv/bin/python"
export PYTHONPATH="$PWD/work/python"
bash tools/reproduce_restore.sh
```

The frozen public catalog is under `work/runtime/live_config_cdn.xml`.
The manifest identifies88 compatible non-video packs. The fetcher reuses matching
files already present in `work/catalog-bundles`, or downloads the exact public
URLs and verifies size, the reproduced salted digest, and UnityFS parsing.
These pristine packs total650364978 bytes. Two compatible MP4 cinematics add
12498566 bytes; their ordinary file-MD5 catalog digests are verified separately
in reports/catalog-videos.json. Titan preloads the ending movie before its fight,
so excluding video files leaves a real late-game download gate. The reproduction
pipeline fetches and packages both movies, with Attach=0. Preserve them separately from modified
packaged data. The final APK contains an explicitly modified VERSIONAL_CONFIGS;
its local hash is intentionally different from the pristine catalog digest.

The pipeline exports TextAssets, recovers the actual resource key, decrypts and
validates XML, exports the two runtime packs, decodes Android resources, fixes
synthetic resource names while retaining IDs, prepares the isolated package,
checks32 ARM64 cases (including MP4 filenames and traversal rejection), applies the documented native changes,
prepares one-token virtual-item prices, renewable coins/gems, zero fight-entry
costs and a clean advanced profile, and builds. Zero shop prices hid the original
purchase controls, so the build uses one-token costs and native startup refills.
The output is `deliverables/ShadowFight-Original-Restored.apk`.

The kit excludes emulator saves, device/account identifiers, raw runtime logs,
credentials, signing keys, downloaded tools and the original APK. A new local
debug key is generated if absent. A different key/build environment changes the
final APK hash and cannot update an installation signed with the server's key.
This is a reproducible procedure, not a claim of byte-identical signed builds.

The rebuilt package is `local.shadowfight.restored`. Test it beside the original;
clear only this isolated package when validating a new seed. The bootstrap copies
seed profiles only when absent and preserves subsequent progress. The native
patches apply only to this input build and intentionally change local save/cache
validation; they do not reproduce the original device-dependent save signature.
Content-copy receipts include SHA256, so changed files of the same length are
replaced on an update without overwriting an existing player profile.

Read Recovery-Tutorial.md, Lessons-Learned.md and Validation.md for the recorded
evidence and limitations. Physical Honor 400 testing is still unavailable. Online
publisher services, accounts and multiplayer raids are not reconstructed.
