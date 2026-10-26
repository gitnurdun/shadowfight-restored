# Shadow Fight restoration

A curated publication of a verified offline restoration of the supplied Shadow Fight 2 application, its restoration tools, recovered-resource records, tutorial, lessons, and validation. The main downloadable game preserves the Unity engine and original gameplay. The original Unity/C# source project was **not** recovered.

## Play the restored game

Download `ShadowFight-Original-Restored.apk` from the [Releases page](../../releases). The published APK is 737,947,037 bytes, SHA-256 `575b3102fa86a0d97186dc5e0a8fa44f4e07f8c0f0691d25f9f3c335beeaf7fe`. Install it on a compatible Android device and allow the first launch to finish preparing its bundled content. The APK is a restored build, not the original input APK.

## Repository layout

- [`restoration/`](restoration/): verified, hash-pinned restoration kit and its original internal layout. The workflow requires the separately owned original input APK; that input is not included.
- [`fallback/java-remake-v1.2/`](fallback/java-remake-v1.2/): earlier Java remake and assets, clearly separate from the Unity-preserving main build.
- [`docs/`](docs/): recovery tutorial, lessons learned, and validation notes.
- [`verification/`](verification/): selected verification records.
- [`screenshots/`](screenshots/): selected gameplay captures.
- [`experimental/personal-offline/`](experimental/personal-offline/): notes about excluded unfinished personalization work; not part of the verified pipeline.

The [restoration guide](docs/Recovery-Tutorial.md) documents the recovery process. The [validation record](docs/Validation.md) describes the test environment and limits. The restored build was verified in an Android 15/API 35 x86_64 emulator with ARM64 translation and software rendering; physical Honor 400 installation/performance was not confirmed.

## Credits and rights

Existing credits and attribution are preserved in the included source and restoration materials. Game code, artwork, audio, names, and other third-party materials remain the property of their respective owners. This repository adds no license and does not claim official affiliation. Please respect the original rights and applicable terms.
