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

**Read status matters.** A row recorded from a file listing is a hypothesis.
A row recorded after reading the implementation is a finding. When they were
read, the LRU cache entries turned out to be three different designs rather
than one duplication. Mark rows you have actually read.

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

## GPU driver management — READ, and it is four concerns not six copies

**This section previously said "same feature six times, no variation to
preserve." That was wrong.** Read on 2026-08-22. See
[`research_log/20260822_1945_gpu_driver_manager_read.md`](research_log/20260822_1945_gpu_driver_manager_read.md).

| Concern | Fork | Lines | Notes |
| --- | --- | --- | --- |
| Install, select, **launch wiring** | xenia-thor | 582 | `GpuDriverManager.java`. Sets `gpu_vulkan_driver*` cvars. A bad package can never brick a launch; the native loader falls back to the system driver. |
| Install, **remote catalogue and recommendation** | azahar-thor | 467 | `getRecommendedDriverOptions`, `downloadRecommendedTurnipDriver`, `downloadDriverAssetPackage`. |
| Install, **device capability detection** | eden-thor | 267 | `isAdrenoGpu`, `supportsCustomDriverLoading`, `getSystemDriverInfo`, `initializeFreedrenoConfigEarly`. |
| **Suitability assessment** | rpcsx-ui-android | 284 | `GpuDriverAdvisor.kt`. See below. |
| libadrenotools vendored | Vita3K-Thor | — | No picker. |
| libadrenotools vendored | Cemu-thor | — | No picker. |

The shared version is a **composition** of these, each taken from the fork
that does it best. Not "pick one and delete five."

### rpcsx `GpuDriverAdvisor` already validates the GPU family

Recorded three times in this repo that no fork does this. It does.

- `Verdict { INCOMPATIBLE, RISKY, COMPATIBLE }`.
- `deviceTarget()` returns `a7xx`, `Adreno 740` on the Thor.
- `claimedFamilies()` recovers the target family from the package name and
  description, including Qualcomm **"Gen N"** marketing: Gen 1 and 2 map to
  a7xx, Gen 3 and later to a8xx.
- States plainly that this is a heuristic, because AdrenoTools metadata carries
  no target-GPU field, and never presents an unrecognised package as verified.

**Take it. Do not rewrite it.**

## Thor hardware profile

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| **Thor performance profile** | **rpcsx-ui-android** | **shipped** | `net.rpcsx.performance.ThorPerformanceProfile`, 165 lines. `isThorTarget()`, `applyStartupDefaults()`, persisted `PROFILE_VERSION`, `ApplyResult` reporting changed and failed settings. |

Shared-layer item 3 was listed as a thing to design. It is partly built.

It also carries a second affinity lesson: rpcsx keeps the **full** core mask on
purpose, because restricting the process to the big cores drags Java, audio and
compiler threads onto the same cores as emulation work. xenia found the
opposite failure. Two forks, two findings, neither aware of the other.

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
| **On-device MCP server, implemented** | **xenia-thor** | **shipped** | `tools/thor/mcp/thor_mcp_server.py`. Read it before writing a new one. |
| On-device MCP server, design | ARMSX2 | design | `docs/mcp-server.md`. Four capability groups. |
| Neural model converter | ARMSX2 | verified | `tools/make_a2nn.py`. |
| VitaCheat converter | Vita3K-Thor | shipped | `tools/convert_vitacheat.py`. |
| Per-fork agent skills | Vita3K-Thor, xenia-thor | shipped | Both under `.agents/skills/`. |
| ARM64 review, per cluster | ARMSX2 | design | Not benchmarked on the device. |
| ARM reference manuals | ARMSX2 | shipped | `docs/reference/arm/`. Move to `hardware_ref/thor/cpu/`. |

## Thor measurement and optimisation harness

`xenia-thor/tools/thor/` holds **137 scripts**: 88 PowerShell, 32 shell, 8
python, 8 `.mjs` workflows, plus `mcp/`. It is the most developed Thor-specific
work in the fleet and was unrecorded until 2026-08-22.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| MCP server, implemented | xenia-thor | shipped | `tools/thor/mcp/thor_mcp_server.py`. |
| Power and affinity A/B, 3 arms | xenia-thor | shipped | `power_affinity_ab.sh`. Gates on Discharging. |
| ADPF target A/B | xenia-thor | shipped | `bd_adpf_ab.sh`. |
| GMEM census and A/B | xenia-thor | shipped | Adreno tile memory. Nothing else touches this. |
| LRZ census and report | xenia-thor | shipped | Low resolution Z. |
| Variable rate shading | xenia-thor | shipped | `bd_vrs_capture.sh`, `bd_vrs_heavy_pass_ab.sh`. |
| Resolution and render mode A/B | xenia-thor | shipped | `bd_resolution_ab.sh`, `bd_render_mode.sh`. |
| Shader statistics | xenia-thor | shipped | `bd_shader_stats.sh`, `bd_shader_report.py`. |
| ARM64 codegen audits, ~40 scripts | xenia-thor | shipped | `thor_a64_*`, `thor_hir_*`. Deepest CPU work in the fleet. |
| Perfetto and GPU capture | xenia-thor | shipped | `thor_gpu_perfetto.ps1`, `thor_gpu_capture.ps1`. |
| TAS, deterministic input | xenia-thor | shipped | `thor_tas.ps1`. |
| Game matrix | xenia-thor | shipped | `thor_game_matrix.ps1`. |
| Evidence ledger | xenia-thor | shipped | `thor_evidence.ps1`, `thor_verify_capture.ps1`. |
| Ghidra headless import | xenia-thor | shipped | `ghidra_headless_import.ps1`. Ghidra tooling already exists. |
| Agent goal loop | xenia-thor | shipped | `thor_codex_goal_loop.ps1`. |
| Multi-agent workflows | xenia-thor | shipped | 8 `wf_*.mjs`, including `wf_arm64_adreno_research.mjs`. |

## Performance hints, pacing, affinity and audio

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Android Performance Hints, ADPF | Cemu-thor | shipped | `src/Cafe/Android/AndroidPerformanceHints.*`. |
| Android Performance Hints, ADPF | xenia-thor | shipped | `gpu_adpf_performance_hints`, disabled by device config today. |
| Per-game frame pacing UI | ARMSX2 | shipped | **iOS only.** A ready-made port to Android. |
| Audio latency model | melonDS-android | shipped | `AudioLatency.kt`. |
| Oboe audio | ARMSX2, melonDS-android, eden-thor | shipped | Three separate integrations. |
| Affinity mask | eden-thor | shipped | `k_affinity_mask.h`. |
| Vsync manager | eden-thor | shipped | `vsync_manager`. |

Nothing found in azahar-thor, Vita3K-Thor or GameThor.

## Cemu-thor

Cemu had no capability recorded before 2026-08-22.

| Capability | Quality | Notes |
| --- | --- | --- |
| Android SAF filesystem | shipped | `fscDeviceAndroidSAF`, `FileStream_saf`. |
| Full Android app | shipped | `src/android/app/`, `NativeInput.cpp`. |
| Texture cache and loader | shipped | `LatteTextureCache`, `LatteTextureLoader`. |
| On-screen overlay | shipped | `LatteOverlay`. |
| Per-game controller profile | shipped | `bin/controllerProfiles/CemuThor_StarFoxZero_StarFox64ish.xml`. |
| Audio, full AX | shipped | `snd_core/ax_*`. |

## Agent skills and experiment discipline

`xenia-thor/.agents/skills/` holds **29 skills**. This is the prior art for the
whole AI-first pillar. Port these rather than writing new ones.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| **Experiment ledger, SQLite** | **xenia-thor** | **shipped** | `tools/exp_ledger.py`. Query before an experiment, record after. Verdicts DEAD, FLAT, WIN, GFX-LOSS, CONFOUNDED, OPEN. |
| **Evidence discipline, mandatory** | **xenia-thor** | **shipped** | No performance number without a captured device file. |
| Experiment gate | xenia-thor | shipped | Blocks repeated guesses on risky experiments. |
| Autonomous driver loop | xenia-thor | shipped | Preflight, build, deploy, launch, capture, classify, worklog, commit. |
| **Game patch format, `.patch.toml`** | **xenia-thor** | **shipped** | `src/xenia/patcher/`, plus `emit_patch_toml.py` authoring from Ghidra. |
| Ghidra OODA loop | xenia-thor | shipped | Logcat and profiles into Ghidra-assisted fixes. |
| Adreno per-stage GPU split | xenia-thor | shipped | Binning, vertex, fragment, stall, per-draw cost, over adb. |
| Snapdragon Profiler metrics | xenia-thor | shipped | Adreno 740 hardware stage metrics. |
| PowerShell command hygiene | xenia-thor | shipped | The same rules this repo learned separately. |
| External model consult | xenia-thor | shipped | `consult-hard`, red-teams a plan with a heavyweight model. |
| Continual harness refiner | xenia-thor | shipped | Online refinement of the harness itself. |
| Video transcript mining | xenia-thor | shipped | Technical talks into portable techniques. |
| Desktop build workaround | xenia-thor | shipped | Defender quarantine and linker issues on this box. |

The project rule stated in the driver skill matches the one this repo wrote
independently: **no behavioural claim without device proof.**

### The measurement discipline is stricter than ours was

- Cross-run fps and frame time are **CONFOUNDED**. Scene complexity swings
  several times a second.
- Trust only an in-place alternating A/B inside one run on a busy frame,
  screenshot correctness, byte-identical comparison, or code facts.
- **Temperature proves the run happened.** No heating means an idle or menu
  scene, so the run is invalid.

### The standing conclusion

> BD's gap is HLE-vs-LLE, proven by RE2 Remake running on the same Thor via
> GameNative/DXVK. Every incremental GPU lever is DEAD/FLAT because it patches
> the emulator instead of replacing it.

**This reverses the open question about GameThor.** GameNative and DXVK are
cited as the proof that translation beats emulation on this device. GameThor is
not an odd fit for the fleet. It is the working example of the direction xenia
wants to move toward. Keep it, and study it as evidence.

## Survey gaps

Partly surveyed forks:

- Cemu-thor: texture filtering and tests. Everything else now recorded.
- xenia-thor: texture filtering, cheats.
- Vita3K-Thor: texture filtering, packs.
- eden-thor: texture filtering, packs, tests.
- GameThor: everything. Nothing recorded, but it is now strategically relevant as the DXVK translation example.

Not surveyed in any fork:

- Control overlays and touch input
- Save and state conventions
- Save and state conventions

## Source

Findings dated 2026-08-22 come from
[`research_log/20260822_1559_shared_paradigm_survey.md`](research_log/20260822_1559_shared_paradigm_survey.md).
