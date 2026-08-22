# Shared paradigm survey — upscaling and per-class routing

Goal: find every fork that implements per-class texture filtering, read each
implementation, and name the differences. This is step 1 and step 2 of
[How to build the shared layer](../CLAUDE.md#how-to-build-the-shared-layer).

Date: 2026-08-22, 15:59 EDT.

## Method

Used `git ls-files` in each fork to exclude build artifacts. A plain recursive
grep over `Documents/` times out, because `armsx2-thor/ARMSX2` contains a
`.cxx/` build tree with a vendored librashader checkout.

Fork sizes, tracked files:

| Fork | Tracked files |
| --- | --- |
| armsx2-thor/ARMSX2 | 13189 |
| azahar-thor/azahar | 5427 |
| ps3-thor/rpcsx-ui-android | 5182 |
| eden-thor | 4458 |
| xenia-thor-workspace/xenia-thor | 2980 |
| melonds_HD/melonDS-android | 2053 |
| cemu-thor-experiment | 1886 |
| gamethor | 1614 |
| psvita/Vita3K-Thor | 1494 |

## Finding 1: two forks implement the same paradigm

Both forks classify a texture, then route the class to an algorithm. Neither
fork applies one global filter. The paradigm is identical. The vocabulary is
not.

### ARMSX2

Files:

- `pcsx2/GS/Renderers/HW/GSTextureUpscaler.h` and `.cpp`
- `pcsx2/GS/Renderers/HW/GSTextureUpscalerNN.h` and `.cpp`
- `pcsx2/Config.h`, lines 478 to 528
- `platforms/android/app/src/main/java/com/armsx2/ui/common/TextureUpscaleSection.kt`
- `tools/make_a2nn.py`

Class taxonomy, `GSTextureUpscaler::TextureClass`:

| Class | Meaning |
| --- | --- |
| `World` | 3D and world art |
| `Ui` | UI and 2D art |

The header states the rule: the two classes never share a setting. Each has its
own enable flag, algorithm and scale factor.

The routing type is `GSTextureUpscaler::Plan`. It holds `scale`, `algorithm`
and `texture_class`. A `scale` of 1 means decline.

`GSTextureUpscaler::Stats` counts one session. It separates every decline
reason: `skipped_guard`, `declined_class_disabled`, `declined_unimplemented`,
`declined_rate_limit`, `declined_budget`, `declined_no_model`.

### melonDS-android

Files:

- `app/src/main/java/me/magnum/melonds/domain/model/HdFilterTarget.kt`
- `app/src/main/cpp/renderer/VulkanFilterMode.h`
- `app/src/main/cpp/renderer/VulkanPlaneFilterMode1..12ShaderData.h`
- `app/src/main/cpp/renderer/VulkanScaleFXPass0..4ShaderData.h`
- `app/src/main/cpp/renderer/VulkanRetroArchFilterChain.cpp`
- `app/src/main/cpp/renderer/OpenGlRetroArchFilterChain.cpp`

Class taxonomy, `HdFilterTarget`:

| Class | Preference key |
| --- | --- |
| `TEXTURE_3D` | `video_hd_texture_filter` |
| `OBJ_SPRITE` | `video_obj_sprite_filter` |
| `BG_LAYER` | `video_bg_layer_filter` |

The file comment names these "the three producers that can be upscaled
independently".

melonDS has a second, separate enum. `VulkanFilterMode` is the present-time
filter: `Nearest`, `Linear`, `Xbr2`, `Hq2x`, `Hq4x`, `Quilez`, `Lcd`,
`Scanlines`, `RetroArch`. Do not confuse the two. One routes a texture class.
The other post-processes the finished frame.

## Finding 2: the algorithm sets already overlap

ARMSX2 `GSTextureUpscaleAlgorithm` has 24 entries in four families: resample,
pixel-art, neural, and a later appended group.

Algorithms present in both forks:

`Anime4K`, `ScaleFX`, `MMPX`, `Scale2x`, `SaI`, `SuperEagle`, `xBR`,
`Nearest`, and bilinear under two names (`Bilinear` and `Linear`).

ARMSX2 only:

`Bicubic`, `Lanczos`, `LanczosCAS`, `Mitchell`, `SharpBilinear`, `ScaleForce`,
`Eagle`, `SuperSaI2x`, `HQx`, `xBRZ`, `SuperxBR`, `OmniScale`, and the neural
set `FSRCNN`, `SESR`, `ESPCN`.

melonDS only:

`Super2xSaI`, `Quilez`, and the display effects `Lcd` and `Scanlines`.
`RetroArch` is not an algorithm. It is a passthrough to librashader.

**ARMSX2 holds the superset.** Use `GSTextureUpscaleAlgorithm` as the base for
the shared algorithm enum. Add `Super2xSaI` and `Quilez` from melonDS.

Note that `Lcd` and `Scanlines` are display effects, not upscalers. Keep them
out of the texture-time enum. They belong to the present-time path.

## Finding 3: shared conventions worth lifting

These conventions appear in the fork code. Take them into the shared layer.

1. **Persist the enum as an integer. Append only.** ARMSX2 documents this trap
   twice, on `GSUpscaler` and on `GSTextureUpscaleAlgorithm`. Inserting a value
   re-points every saved configuration at a different algorithm. The shared
   enum must carry the same rule.
2. **One source of truth for a setting.** `HdFilterTarget` maps the class to
   its preference key inside the enum. The in-game overlay and the settings
   screen read the same key. There is no second copy.
3. **Count every decline reason separately.** The ARMSX2 `Stats` comment gives
   the reason: "nothing got upscaled" has many causes and each needs a
   different fix.
4. **Keep the decision module pure.** The `GSTextureUpscaler` header states
   that the module decides and scales, and never touches the hash cache.
   `GSTextureCache` owns insertion. The stated reason: two owners of one
   lifetime corrupt it.
5. **State the cost model.** The ARMSX2 header records that texture-time cost
   is once per unique texture and returns to zero in steady state.
   Present-time cost is per frame, forever.

## Finding 4: the real difference is the class list

The paradigm matches. The class list does not, and it cannot.

- ARMSX2 splits `World` and `Ui`. The PS2 has no sprite plane.
- melonDS splits `TEXTURE_3D`, `OBJ_SPRITE` and `BG_LAYER`. The DS has
  hardware planes, so the split is free and exact.

This is a real per-emulator need. It is not an accident of history. Each
emulator knows facts about a texture that no other emulator has.

**Design consequence.** Do not force one fixed class list on the fleet. The
shared layer must define the contract, and each fork must declare its own class
list against that contract. The shared parts are the algorithm enum, the plan
type, the statistics, the persistence rule and the settings UI. The class list
is per-fork data.

## Not yet surveyed

- azahar-thor, Vita3K-Thor, Cemu-thor, xenia-thor, rpcsx-ui-android-thor:
  not checked for texture filtering.
- HD texture pack formats. melonDS has a content-hash format. Cemu and azahar
  likely have their own.
- Cheat databases. `azahar-thor/cheat_sources/` holds three sources.
- Per-game profile systems. Every fork has one. None are compared yet.
- Control overlays.

## Next actions

1. Draft the shared classification contract from findings 1 to 4.
2. Survey the remaining five Tier 1 forks for a class list.
3. Survey the per-game profile systems. The app requires a per-game override
   for every option, so this contract is now mandatory.

## Finding 5: the toolchain is fragmented

Measured from each fork's Android `build.gradle` or `build.gradle.kts`.

| Fork | NDK | minSdk | targetSdk | compileSdk | ABI filters |
| --- | --- | --- | --- | --- | --- |
| ARMSX2 | gradle property | not set | 37 | 37 | arm64-v8a |
| melonDS-android | `AppConfig.ndkVersion` | not in this file | not in this file | not in this file | armeabi-v7a, arm64-v8a |
| azahar-thor | 27.3.13750724 | 29 | 37 | not in this file | variable |
| rpcsx-ui-android | 29.0.13113456 | 29 | 35 | 37 | variable |
| GameThor | 22.1.7171670 | 26 | 28 | 35 | arm64-v8a, armeabi-v7a |
| eden-thor | 28.2.13676358 | 24 | 36 | not in this file | arm64-v8a, x86_64 |
| Vita3K-Thor | 29.0.14206865 | 28 | 35 | 35 | arm64-v8a, x86_64 |
| xenia-thor | 25.0.8775105 | 26 | 33 | 33 | arm64-v8a, x86_64 |

Seven different NDK versions are in use. The range is NDK 22 to NDK 29.

GameThor targets API 28. The Thor runs Android 13, which is API 33.

Gradle wrapper versions read from the first matching
`gradle-wrapper.properties` in each fork: 9.6.1, 9.5.0, 9.4.1, 8.13, 8.12.1,
7.3.3. **The values read for azahar-thor (4.4.1) and ARMSX2 (6.1.1) are not
trusted.** The grep may have matched a wrapper inside a dependency. Verify
both before you act on them.

### Consequence

Two native libraries built with different NDK major versions cannot be
guaranteed to share a C++ runtime in one process. The libc++ ABI is not stable
across that range. This blocks one app that links several emulator cores.

### Target

Unify on one row:

- One NDK version. Choose the newest that every fork can build with.
- `arm64-v8a` only. The Thor is arm64. Drop armeabi-v7a and x86_64 from the
  shipping build. ARMSX2 already does this.
- One `minSdk`. It must be 33 or lower, because the Thor runs API 33.
- One `targetSdk` and one `compileSdk`.
- One Gradle version and one AGP version.
- One C++ standard. melonDS declares `-std=c++17`. Read the rest.

Do this before any code extraction. Shared native code cannot exist across
seven C++ runtimes.

## Finding 6: the toolchain row is decided

Read from the device over adb on 2026-08-22.

| Property | Value |
| --- | --- |
| Model | AYN Thor |
| Android | 13 |
| API level | 33 |
| ABI | arm64-v8a |
| Hardware | qcom |
| adb address | 192.168.1.3:5555 |

A Quest 2 is also attached to this box. A bare adb command fails with "more
than one device/emulator". Always pass `-s`.

Installed NDK versions on this box: 25.0.8775105, 27.3.13750724,
28.0.13004108, 28.2.13676358, 29.0.13113456, 29.0.14206865, 30.0.15729638.
Installed platforms: android-33, android-35, android-36, android-37.0.

NDK r29 is the latest stable release as of August 2026. NDK r30 is in beta and
will become the LTS release. Android 17 is API 37, released 2026-06-16.

Decision: NDK 29.0.14206865, arm64-v8a only, minSdk 33, targetSdk 37,
compileSdk 37. Recorded in CLAUDE.md.

## Finding 7: the test infrastructure is the largest synergy gap

Four forks hold automated test and replay tools. No fork holds more than two.
Nothing is shared.

- ARMSX2: `pcsx2-gsrunner` replays a GS dump headless. It has a RenderDoc
  capture hook and a golden image comparer, `comparer.js`. `pcsx2-eerunner`
  runs the EE headless.
- xenia-thor: GPU trace dump and viewer, an Android trace viewer activity, and
  an agent skill at `.agents/skills/xenia-renderdoc-replay/`. Two research
  documents on record and replay.
- Vita3K-Thor: a working on-device regression suite, a savestate fixture
  runner, a render regression matrix as JSON, and an agent skill at
  `.agents/skills/vita3k-regression-ledger/`.
- azahar-thor: input movie record and replay in `src/core/movie.cpp`.
- rpcsx-ui-android-thor: `rsx_replay.cpp`, inherited from rpcs3.

No test or replay capability found in Cemu-thor, eden-thor, melonDS-android or
GameThor.

**Extract the test harness before the renderer features.** Without a shared
harness, an agent cannot separate a good port from a regression. The fan-out
then produces damage instead of work.

Full table in [`capability_inventory.md`](../capability_inventory.md).
