# Capability inventory

Which fork has which capability, and at what quality.

**Read this before you build anything.** The first question for a new feature
is "which fork already has this?". It is not "how do I write this?".

Update this file when you find a capability. Do not wait for a survey.

Quality values:

| Value | Meaning |
| --- | --- |
| `shipped` | Works on the device. Measured. |
| `verified` | Runs on the device. Not measured in a real scene. |
| `built` | Compiles and passes a self-test. Not run on the device. |
| `design` | A document exists. No code. |

Surveyed 2026-08-22. **The first version of this file was wrong.** It covered
two forks and implied the rest had nothing. Read every "not surveyed" line
below as a gap in the survey, never as an absence in the fork.

## Texture filtering and upscaling

**Four forks implement this independently.** None share code.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Texture-time upscaler, 13 kernels | ARMSX2 | verified | `GSTextureUpscaler`. 26/26 self-tests. |
| Neural upscaler | ARMSX2 | verified | Anime4K, FSRCNN, SESR, ESPCN. `.a2nn`. No weights shipped. |
| Algorithm enum, 24 entries | ARMSX2 | verified | `GSTextureUpscaleAlgorithm`. The fleet superset. |
| Present-time FSR1 | ARMSX2 | built | `GSUpscaler::FSR1`. |
| Per-producer filtering | melonDS-android | shipped | `HdFilterTarget`: 3D, OBJ sprite, BG layer. |
| Plane filter modes 1-12 | melonDS-android | shipped | Vulkan compute shaders. |
| ScaleFX, 5 passes | melonDS-android | shipped | `VulkanScaleFXPass0..4`. |
| **Anime4K, present-time, both backends** | **azahar-thor** | **shipped** | `opengl_present_anime4k.frag` and `vulkan_present_anime4k.frag`. |
| **Texture filtering shader set** | **azahar-thor** | **shipped** | `xbrz_freescale`, `mmpx`, `scale_force`, `bicubic`, `refine`, `x_gradient`, `y_gradient`. |
| FSR1 upscaler | rpcsx-ui-android | shipped | `Emu/RSX/Program/Upscalers/FSR1/`, plus a GL path. |
| Bilinear and nearest passes | rpcsx-ui-android | shipped | `Emu/RSX/GL/upscalers/`. |
| librashader, Vulkan | melonDS-android | shipped | `VulkanRetroArchFilterChain`. Two local patches. |
| librashader, OpenGL ES | melonDS-android | shipped | `OpenGlRetroArchFilterChain`. |
| librashader vendored | ARMSX2 | built | Integration not read. |
| Shader diagnostics | melonDS-android | shipped | `ShaderDiagnostics`. |

azahar `xbrz_freescale` is a free-scale variant. ARMSX2 does not have one.

Not surveyed for texture filtering: Cemu-thor, xenia-thor, Vita3K-Thor,
eden-thor.

## HD texture packs and custom textures

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| **Custom textures with materials** | **azahar-thor** | **shipped** | `custom_textures/`: `custom_tex_manager`, `material`, `custom_format`. Materials carry more than colour. |
| Content-hash pack format | melonDS-android | shipped | Desktop-compatible. Dump and replace. |
| **Graphic packs with ASM patching** | **Cemu-thor** | **shipped** | `GraphicPack2`, `GraphicPack2Patches`, parser and applier. |
| Custom Thor graphic packs | Cemu-thor | shipped | `bin/graphicPacks/cemuThorBuiltin/`. Star Fox Zero packs written here. |
| Texture dump | xenia-thor | shipped | `src/xenia/gpu/texture_dump.cc`. |
| Texture pack getter | ARMSX2 | design | `docs/texture-pack-getter.md`. |

Cemu `GraphicPack2` is the most capable format in the fleet. It combines
texture replacement, shader replacement and runtime ASM patching in one thing.

## Cheats

**Five forks support cheats. Each uses a different format.**

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Cheat engine, Atmosphere VM | eden-thor | shipped | `cheat_engine`, `dmnt_cheat_vm`, `dmnt_cheat_types`. |
| VitaCheat runtime and database | Vita3K-Thor | shipped | `cheats/`, `tools/convert_vitacheat.py`. Database synced to the Thor. |
| **Cheat badges in the library** | **Vita3K-Thor** | **shipped** | `reports/20260510_152227_virtual-cartridges-cheat-badges-hotkeys.md`. The badge idea is already built. |
| Cheat coverage inventory | Vita3K-Thor | shipped | `reports/20260510_192847_cheat-coverage-inventory.md`. |
| Cheat overlay UI | rpcsx-ui-android | shipped | `Emu/RSX/Overlays/HomeMenu/overlay_home_menu_cheats`. |
| Bundled cheat database | rpcsx-ui-android | shipped | `app/src/main/assets/cheats/ncl/`. Thousands of `.ncl` files. |
| Cheat source: Sharkive | azahar-thor | shipped | `cheat_sources/Sharkive`. |
| Cheat source: CTRPF-AR | azahar-thor | shipped | `cheat_sources/CTRPF-AR-CHEAT-CODES`. |
| Cheat source: citra wiki | azahar-thor | shipped | `cheat_sources/citra-games-wiki`. |
| Cheats through graphic packs | Cemu-thor | shipped | `GamePatch`, plus the Star Fox Zero patches. |
| Cheat documentation | eden-thor | shipped | `docs/user/UsingCheats.md`. |

`ai_cheat_helper_switch` is a separate repo. **Not surveyed. No capability is
claimed for it.**

## Mods and patches

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Mod manager | eden-thor | shipped | `src/frontend_common/mod_manager`. |
| Patch manager | eden-thor | shipped | `src/core/file_sys/patch_manager`. |
| Game patch manager and UI | xenia-thor | shipped | `GamePatchManager.java`, plus its activity. |
| Runtime ASM patching | Cemu-thor | shipped | `GraphicPack2PatchesApply`, with its own parser. |

## GPU driver management — the most duplicated capability

Six forks vendor `libadrenotools` and each wrote its own driver picker. Same
feature, six times, for one GPU. There is no per-emulator variation to
preserve. This is the lowest-risk extraction in the fleet.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Driver manager UI | xenia-thor | shipped | `GpuDriverManager`, `GpuDriverManagerActivity`, `GpuDriverPackage`. |
| Driver helper | azahar-thor | shipped | `GpuDriverHelper.kt`, `GpuDriverMetadata.kt`. |
| Driver helper | eden-thor | shipped | `GpuDriverHelper.kt`, `GpuDriverMetadata.kt`. Local patch. |
| Driver screen and advisor | rpcsx-ui-android | shipped | `GpuDriversScreen.kt`, `GpuDriverAdvisor.kt`. |
| libadrenotools vendored | Vita3K-Thor | shipped | No picker found. |
| libadrenotools vendored | Cemu-thor | shipped | No picker found. |

`GpuDriverAdvisor` is the only one that advises rather than lists. Read it
first.

## Per-game profiles and the app shell

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Per-game profiles | xenia-thor | shipped | `GameProfiles.java`. |
| Per-game optimisations UI | xenia-thor | shipped | `GameOptimizationsActivity.java`. |
| Content installer and manager | xenia-thor | shipped | `ContentInstaller`, `ContentManagerActivity`. |
| Controller mapping UI | xenia-thor | shipped | `ControllerMappingActivity.java`. |
| Crash reporter | xenia-thor | shipped | `CrashReporter.java`. |
| Game profile system | Cemu-thor | shipped | `Cafe/GameProfile/`. |

xenia-thor has the most complete Android shell in the fleet. Survey it before
you design the app UI.

## Shader caches

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Shader cache | Cemu-thor | shipped | `LatteShaderCache`. |

Not surveyed elsewhere. Most forks probably have one. Shader compile stutter is
a Thor-wide problem and this table is nearly empty. Close that gap.

## Test and QA infrastructure

Items exist in five forks. No fork holds more than three. Nothing is shared.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Headless GPU dump replay | ARMSX2 | shipped | `pcsx2-gsrunner`. |
| RenderDoc capture hook | ARMSX2 | shipped | `pcsx2-gsrunner/RenderDocCapture.cpp`. |
| Golden image comparer | ARMSX2 | shipped | `pcsx2-gsrunner/comparer.js`, `comparer.css`. |
| Headless EE runner | ARMSX2 | shipped | `pcsx2-eerunner`. |
| GPU trace dump and viewer | xenia-thor | shipped | `d3d12_trace_dump_main.cc`, `d3d12_trace_viewer_main.cc`. |
| GPU trace viewer on Android | xenia-thor | shipped | `GpuTraceViewerActivity.java`. |
| RenderDoc replay agent skill | xenia-thor | shipped | `.agents/skills/xenia-renderdoc-replay/`. |
| Record and replay test plan | xenia-thor | design | `docs/research/20260530-130500-...md`. |
| Deterministic input, no movies | xenia-thor | design | `docs/research/20260529-210700-...md`. |
| On-device regression suite | Vita3K-Thor | shipped | `tools/android/run-thor-regression-suite.ps1`. |
| Savestate regression fixture | Vita3K-Thor | shipped | `tools/android/run-thor-quickstate-regression.ps1`. |
| Render regression matrix | Vita3K-Thor | shipped | `tools/android/thor-render-regression-matrix.json`. |
| Regression ledger agent skill | Vita3K-Thor | shipped | `.agents/skills/vita3k-regression-ledger/`. |
| Replay hooks in GXM | Vita3K-Thor | shipped | `SceGxmInternalForReplay.cpp`. |
| Input movie record and replay | azahar-thor | shipped | `src/core/movie.cpp`, with dialogs. |
| RSX capture replay | rpcsx-ui-android | shipped | `rsx_replay.cpp`. |
| **Deterministic renderer case corpus** | **melonds_HD_2** | **shipped** | `renderer_cases/`. The best test artifact in the fleet. See below. |
| **Differential test, hardware against software renderer** | **melonds_HD_2** | **shipped** | `case.json` stores expected frames for software, blackmagic3 and compute3d. |
| Host-side case guards | melonds_HD_2 | shipped | `guards.json`, plus python guard scripts under `tools/`. |

Nothing found in Cemu-thor, eden-thor or GameThor.

### melonds_HD_2 `renderer_cases` is the model for the shared harness

`melonds_HD_2` was dropped as a target. **It is not dropped as a source.** It
holds the most complete test design in the fleet, and it combines four of the
paradigms in one artifact:

- **Savestate as the fixture.** `input/savestate.ml0`, plus `start_frame` and
  `frame_count` for a deterministic range.
- **Golden images.** `expected/software`, `expected/blackmagic3` and
  `expected/blackmagic3_compute3d`.
- **Differential testing.** The software renderer is the reference. A hardware
  renderer is compared against it.
- **No ROM in the repo.** The ROM is identified by sha256, size and header
  fields. `input/` holds only notes and a `.gitkeep`.

Cases are named by behaviour, not by game: `capture_sync`, `sprite_mosaic`,
`rotscale_bg`, `obj_window`, `blend_priority`, `forced_blank`. The README
states the corpus is for AI-driven renderer cases.

**Read `renderer_cases/README.md` and `case.template.json` before designing
the shared harness.** Do not design a new format first.

## Tooling

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| On-device MCP server | ARMSX2 | design | `docs/mcp-server.md`. Four capability groups. |
| Neural model converter | ARMSX2 | verified | `tools/make_a2nn.py`. |
| VitaCheat converter | Vita3K-Thor | shipped | `tools/convert_vitacheat.py`. |
| Per-fork agent skills | Vita3K-Thor, xenia-thor | shipped | Both under `.agents/skills/`. |
| ARM64 review, per cluster | ARMSX2 | design | Not benchmarked on the device. |
| ARM reference manuals | ARMSX2 | shipped | `docs/reference/arm/`. Move to `hardware_ref/thor/cpu/`. |

## Survey gaps

Partly surveyed forks:

- Cemu-thor: texture filtering, tests, per-game overrides.
- xenia-thor: texture filtering, cheats.
- Vita3K-Thor: texture filtering, packs.
- eden-thor: texture filtering, packs, tests.
- GameThor: everything. Nothing recorded.

Not surveyed in any fork:

- Control overlays and touch input
- Save and state conventions
- Thread and cluster affinity
- Audio backends and latency
- Frame pacing and vsync handling

## Source

Findings dated 2026-08-22 come from
[`research_log/20260822_1559_shared_paradigm_survey.md`](research_log/20260822_1559_shared_paradigm_survey.md).
