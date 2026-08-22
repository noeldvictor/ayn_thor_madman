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
| `none` | Not present. |

## Texture filtering and upscaling

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Per-class texture routing | ARMSX2 | verified | `TextureClass { World, Ui }`. 2026-08-21. |
| Per-producer filtering | melonDS-android | shipped | `HdFilterTarget`: 3D, OBJ sprite, BG layer. |
| Texture-time upscaler | ARMSX2 | verified | `GSTextureUpscaler`. 13 kernels, 26/26 self-tests. |
| Neural upscaler | ARMSX2 | verified | Anime4K, FSRCNN, SESR, ESPCN. `.a2nn` format. No weights shipped. |
| Algorithm enum, 24 entries | ARMSX2 | verified | `GSTextureUpscaleAlgorithm`. The fleet superset. |
| ScaleFX, 5 passes | melonDS-android | shipped | `VulkanScaleFXPass0..4`. |
| librashader, Vulkan | melonDS-android | shipped | `VulkanRetroArchFilterChain`. Two local patches. |
| librashader, OpenGL ES | melonDS-android | shipped | `OpenGlRetroArchFilterChain`. |
| librashader | ARMSX2 | built | Vendored. Integration not read yet. |
| HD texture pack, content hash | melonDS-android | shipped | Desktop-compatible format. Fleet candidate. |
| Present-time FSR1 | ARMSX2 | built | `GSUpscaler::FSR1`. Vulkan compute. |
| Shader diagnostics | melonDS-android | shipped | `ShaderDiagnostics`. |

## Tooling and automation

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| On-device MCP server | ARMSX2 | design | `docs/mcp-server.md`. Four capability groups. |
| Neural model converter | ARMSX2 | verified | `tools/make_a2nn.py`. |
| Texture pack getter | ARMSX2 | design | `docs/texture-pack-getter.md`. |
| Per-fork skills | melonds_HD_2 | shipped | `.claude/skills/`. |
| ARM64 review, per cluster | ARMSX2 | design | Not benchmarked on the device. |
| ARM reference manuals | ARMSX2 | shipped | `docs/reference/arm/`. Move to `hardware_ref/thor/cpu/`. |

## Cheats, mods and content

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Cheat source: Sharkive | azahar-thor | shipped | `cheat_sources/Sharkive`. |
| Cheat source: CTRPF-AR | azahar-thor | shipped | `cheat_sources/CTRPF-AR-CHEAT-CODES`. |
| Cheat source: citra wiki | azahar-thor | shipped | `cheat_sources/citra-games-wiki`. |
| AI cheat discovery | ai_cheat_helper_switch | unknown | Separate repo. Not surveyed. |

## GPU driver management — the most duplicated capability

Six forks vendor `libadrenotools` and each wrote its own driver picker. This is
the same feature, six times, for one GPU. It is the clearest shared-layer
candidate in the fleet.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Driver manager UI | xenia-thor | shipped | `GpuDriverManager.java`, `GpuDriverManagerActivity`, `GpuDriverPackage`. |
| Driver helper | azahar-thor | shipped | `GpuDriverHelper.kt`, `GpuDriverMetadata.kt`. Vendors `libadrenotools`. |
| Driver helper | eden-thor | shipped | `GpuDriverHelper.kt`, `GpuDriverMetadata.kt`. Has a local patch. |
| Driver screen and advisor | rpcsx-ui-android | shipped | `GpuDriversScreen.kt`, `GpuDriverAdvisor.kt`. |
| libadrenotools vendored | Vita3K-Thor | shipped | `external/libadrenotools`. No picker found. |
| libadrenotools vendored | Cemu-thor | shipped | `dependencies/libadrenotools`. No picker found. |

`GpuDriverAdvisor` in rpcsx is the only one that advises rather than lists.
Read it first.

## Per-game profiles

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Per-game profiles | xenia-thor | shipped | `GameProfiles.java`. |
| Per-game optimisations UI | xenia-thor | shipped | `GameOptimizationsActivity.java`. |
| Game patch manager | xenia-thor | shipped | `GamePatchManager.java`, plus its activity. |
| Content installer | xenia-thor | shipped | `ContentInstaller.java`, `ContentManagerActivity`. |
| Controller mapping UI | xenia-thor | shipped | `ControllerMappingActivity.java`. |
| Crash reporter | xenia-thor | shipped | `CrashReporter.java`. |

xenia-thor has the most complete Android shell in the fleet. Survey it before
you design the app UI.

## Test and QA infrastructure

This is the largest synergy gap found so far. Items exist in four forks. No
fork has more than two. Nothing is shared.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Headless GPU dump replay | ARMSX2 | shipped | `pcsx2-gsrunner`. Replays a GS dump with no game. |
| RenderDoc capture hook | ARMSX2 | shipped | `pcsx2-gsrunner/RenderDocCapture.cpp`. |
| Golden image comparer | ARMSX2 | shipped | `pcsx2-gsrunner/comparer.js`, `comparer.css`. |
| Headless EE runner | ARMSX2 | shipped | `pcsx2-eerunner`. |
| GPU trace dump and viewer | xenia-thor | shipped | `d3d12_trace_dump_main.cc`, `d3d12_trace_viewer_main.cc`. |
| GPU trace viewer on Android | xenia-thor | shipped | `GpuTraceViewerActivity.java`. |
| RenderDoc replay agent skill | xenia-thor | shipped | `.agents/skills/xenia-renderdoc-replay/`. |
| Record and replay test plan | xenia-thor | design | `docs/research/20260530-130500-automated-test-record-replay-plan.md`. |
| Deterministic input, no movies | xenia-thor | design | `docs/research/20260529-210700-deterministic-input-avoid-movies.md`. |
| On-device regression suite | Vita3K-Thor | shipped | `tools/android/run-thor-regression-suite.ps1`. |
| Savestate regression fixture | Vita3K-Thor | shipped | `tools/android/run-thor-quickstate-regression.ps1`. |
| Render regression matrix | Vita3K-Thor | shipped | `tools/android/thor-render-regression-matrix.json`. |
| Regression ledger agent skill | Vita3K-Thor | shipped | `.agents/skills/vita3k-regression-ledger/`. |
| Input movie record and replay | azahar-thor | shipped | `src/core/movie.cpp`. Record and play dialogs. |
| RSX capture replay | rpcsx-ui-android-thor | shipped | `rsx_replay.cpp`, from rpcs3. |
| Replay hooks in GXM | Vita3K-Thor | shipped | `SceGxmInternalForReplay.cpp`. |

No test or replay capability was found in Cemu-thor, eden-thor,
melonDS-android or GameThor.

## Not yet surveyed

No capability is recorded yet for these forks:

- Cemu-thor
- eden-thor
- GameThor

Partly surveyed. Test infrastructure only:

- xenia-thor
- Vita3K-Thor
- rpcsx-ui-android-thor
- azahar-thor, plus the cheat sources

These areas are not surveyed in any fork:

- Per-game profile systems
- Control overlays
- Save and state conventions
- Shader caches
- Thread and cluster affinity
- Vulkan driver selection

## Source

Findings dated 2026-08-22 come from
[`research_log/20260822_1559_shared_paradigm_survey.md`](research_log/20260822_1559_shared_paradigm_survey.md).
