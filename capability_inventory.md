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

## Read status

**A row recorded from a file listing is a hypothesis. A row recorded after
opening the file is a finding.** Six claims in this repo were wrong because a
listing cannot tell you what a file does. Each section below carries its read
status.

| Marker | Meaning |
| --- | --- |
| **READ** | The implementations were opened and compared. The conclusion is evidence. |
| **LISTED** | Found by `git ls-files` or grep. The name suggests a capability. **Nobody has opened it.** |
| **PARTIAL** | Some implementations read, others only listed. The line says which. |

**Never plan an extraction against a LISTED row.** Reading has reversed the
conclusion every time it was tried on one:

| Row | Was | After reading |
| --- | --- | --- |
| LRU cache | "same structure, three times" | three designs for three constraints |
| GPU driver pickers | "same feature, six times" | four different concerns |
| Texture cache hashing | "shared" | guest-specific |
| Driver GPU validation | "no fork does this" | rpcsx `GpuDriverAdvisor` does |
| On-device MCP | "design only" | xenia has a working server |
| Thor hardware profile | "to be designed" | rpcsx `ThorPerformanceProfile` exists |

Surveyed 2026-08-22. **The first version of this file was wrong.** It covered
two forks and implied the rest had nothing. Read every "not surveyed" line
below as a gap in the survey, never as an absence in the fork.

## Texture filtering and upscaling — PARTIAL

**READ:** ARMSX2 `GSTextureUpscaler` header, `GSTextureUpscaleAlgorithm` enum, melonDS
`VulkanFilterMode` and `HdFilterTarget`. **LISTED only:** the azahar shader set,
rpcsx upscalers, librashader integrations, melonDS plane filter shaders.

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

## HD texture packs and custom textures — PARTIAL

**READ:** azahar `material.h` in full. **LISTED only:** Cemu `GraphicPack2`,
melonDS content-hash format, xenia `texture_dump`.

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

## Cheats — PARTIAL

**READ:** eden `cheat_engine.h`, azahar `cheats.h`. **LISTED only:** Vita3K
VitaCheat, rpcsx overlay, Cemu graphic pack patches, the three azahar cheat
databases.

Two forks, two architectures, and **eden's generalises**.

### eden runs cheats on a virtual machine, and the host interface is tiny

`DmntCheatVm` executes Atmosphere cheat bytecode. The emulator supplies
`StandardVmCallbacks`:

```cpp
void MemoryReadUnsafe(VAddr address, void* data, u64 size);
void MemoryWriteUnsafe(VAddr address, const void* data, u64 size);
u64  HidKeysDown();
void PauseProcess();
void ResumeProcess();
void DebugLog(u8 id, u64 value);
void CommandLog(std::string_view data);
```

**Six calls, and not one of them is Switch-specific.** Read guest memory, write
guest memory, ask which buttons are held, pause, resume, log.

**Every backend in the fleet can implement that.** So a shared cheat engine is:
the VM shared, the callbacks per backend. Same split as everywhere else — the
shared side holds the algorithm, the backend supplies guest knowledge.

This is the strongest cheat architecture in the fleet and it was not designed
for sharing. It generalises by accident, because a cheat VM has to be
abstracted from the machine anyway.

### azahar keeps a list of polymorphic cheat objects

`Cheats::CheatEngine` with `CheatBase`, `AddCheat`, `RemoveCheat`,
`UpdateCheat`, `GetCheats`, `Connect(process_id)`, guarded by a `shared_mutex`.

More conventional and less general: the cheat is an object with behaviour
rather than data interpreted by a VM. It is the better model for **managing**
a cheat list; eden's is the better model for **executing** one.

**Take both. They solve different halves.**

### rpcsx: a flat typed poke, and a converter

`util/cheat_info.h`, 33 lines:

```cpp
enum class cheat_type { unsigned_8..64, signed_8..64, float_32 };
struct cheat_info {
    std::string game, description;
    cheat_type  type;
    u32         offset;
    std::string red_script;
    bool from_str(const std::string&);  std::string to_str() const;
};
```

**A single typed write at an offset**, plus serialisation both ways and a
`red_script` escape hatch for anything more complex.

Around it, in Kotlin: `CheatRepository`, `CheatSelectionRepository`,
`PatchHashRepository`, and **`ArtemisConverter.kt`**, which converts the
Artemis format.

**rpcsx is GPL-2.0-only. Read it for ideas; never copy its code.**

### Three architectures forming a ladder of expressiveness

| Fork | Model | Expressiveness |
| --- | --- | --- |
| rpcsx | one typed write at an offset | data |
| azahar | polymorphic cheat objects | behaviour |
| eden | bytecode virtual machine | programmable |

**Most cheats are just typed pokes.** So the shared engine should be tiered: a
fast path for the flat case, falling back to the VM only when a cheat needs
conditions, loops or button checks.

That is not a compromise between three designs. It is the observation that they
sit at different points on one axis, and a shared engine needs the whole axis.

### The format problem is separate from the execution problem

At least six formats are in the fleet: `mch` (melonDS), `pnach` (ARMSX2),
`ncl` (rpcsx), graphic packs (Cemu), Atmosphere `dmnt` (eden), and the 3DS
AR-code sources azahar bundles.

**A VM does not care.** A per-format front end can compile several of these
into one VM's bytecode, which turns six formats into six small parsers rather
than six engines.

**Two forks already convert cheat formats**, which is evidence the idea works
in practice: rpcsx `ArtemisConverter.kt` and Vita3K `convert_vitacheat.py`,
with `sync_vitacheat_db.ps1` keeping a database in step and `cheat_paths.cpp`
owning where they live.

**Still unverified:** whether `pnach` and AR codes map cleanly onto `dmnt`
bytecode. That check decides whether one VM can serve everything, or whether
the tiered design above is required rather than merely nicer.

### The five cheat implementations

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

## Mods and patches — PARTIAL, and they are two different features

**READ:** eden `mod_manager.h`, Cemu `GraphicPack2Patches.h`. **LISTED only:**
xenia `patcher/`, eden `patch_manager`, Cemu `GamePatch`.

**This repo conflated two unrelated things under one heading. Separate them.**

| | File mods | Code patches |
| --- | --- | --- |
| What | replace game assets | modify guest code at run time |
| Mechanism | install files, guest filesystem serves them | assemble and relocate instructions |
| Example | a texture pack, a translation | 60 FPS, infinite health, disable SSAO |
| Belongs to | the storage and content layer | the patch engine |

### eden's mod manager is a file installer, 21 lines

```cpp
enum ModInstallResult { Cancelled, Failed, Success };
std::vector<std::filesystem::path> GetModFolder(const std::string& root);
ModInstallResult InstallMod(const std::filesystem::path&, u64 program_id, bool copy);
```

That is the whole interface. **Mods on Switch are file replacement**, and the
guest filesystem layer does the serving. The manager only puts files in the
right place.

This is the right size for that job and it should not be made bigger.

### Cemu's patcher is an assembler with a linker, 267 lines of header alone

`GraphicPack2Patches.h` carries a `PatchContext_t` with a symbol table, a
matched `RPLModule`, an error handler that reports line numbers, and a set of
`UnresolvedSymbol` entries holding a line number, a patch group and a name.

Its resolution results:

```cpp
enum class PATCH_RESOLVE_RESULT {
  RESOLVED, EXPRESSION_ERROR, VALUE_ERROR, UNKNOWN_VARIABLE,
  VARIABLE_CONFLICT, INVALID_ADDRESS, UNDEFINED_ERROR,
};
```

`UNKNOWN_VARIABLE` is documented as "try again", which means **multi-pass
resolution**: a patch may reference a symbol defined later, so the resolver
iterates until it converges.

**This is a real symbolic assembler**: expressions, labels, variables,
relocation against a loaded module, branch-range checking (`VALUE_ERROR` is
"branch target out of range"), and conflict detection.

**It is by a wide margin the most sophisticated patch system in the fleet**,
and it is the one to build the shared engine from. xenia `.patch.toml` and
ARMSX2 `pnach` are both flatter formats.

### Binding a patch to the right game build

Two forks solve this differently and both were unrecorded:

- **Cemu** matches against a loaded `RPLModule`, so a patch resolves against
  the module actually present.
- **rpcsx** keeps a `PatchHashRepository`, matching by hash.

**Nothing else in the fleet appears to solve it at all**, which means patches
elsewhere can silently apply to the wrong build.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Mod manager | eden-thor | shipped | `src/frontend_common/mod_manager`. |
| Patch manager | eden-thor | shipped | `src/core/file_sys/patch_manager`. |
| Game patch manager and UI | xenia-thor | shipped | `GamePatchManager.java`, plus its activity. |
| Runtime ASM patching | Cemu-thor | shipped | `GraphicPack2PatchesApply`, with its own parser. |

## GPU driver management — READ

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

## Thor hardware profile — READ

rpcsx `ThorPerformanceProfile` header and core-mask comment read directly.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| **Thor performance profile** | **rpcsx-ui-android** | **shipped** | `net.rpcsx.performance.ThorPerformanceProfile`, 165 lines. `isThorTarget()`, `applyStartupDefaults()`, persisted `PROFILE_VERSION`, `ApplyResult` reporting changed and failed settings. |

Shared-layer item 3 was listed as a thing to design. It is partly built.

It also carries a second affinity lesson: rpcsx keeps the **full** core mask on
purpose, because restricting the process to the big cores drags Java, audio and
compiler threads onto the same cores as emulation work. xenia found the
opposite failure. Two forks, two findings, neither aware of the other.

## Per-game profiles and the app shell — PARTIAL

**READ:** the file inventory and `XeniaCoverArt.java`. **LISTED only:** the
bodies of the other twenty files.

**xenia has the most complete shell and the worst structure.** 12,313 lines of
Java across the emulator package. The completeness is real; so is the
accretion.

Its shape is **Activity-per-manager**: `SettingsActivity`,
`GpuDriverManagerActivity`, `GamePatchManagerActivity`,
`ContentManagerActivity`, `TrainerManagerActivity`,
`GameOptimizationsActivity`, `ControllerMappingActivity`. That is a menu tree,
and it is the same complaint this project has about RetroArch.

**Mine it for the feature list and the mechanisms. Do not copy the navigation
model or the code**, which is Java and Activities where the shell is Kotlin and
Compose.

| Capability | Lines | Notes |
| --- | --- | --- |
| `EmulatorActivity` | 2261 | |
| `LauncherActivity` | 1334 | |
| `XeniaOptimizations` | 1212 | |
| `XeniaAndroidSettings` | 987 | |
| `GpuDriverManager` | 582 | read separately |
| `SettingsActivity` | 560 | |
| `ControllerMappingActivity` | 559 | |
| `GameProfiles` | 420 | |
| **`XeniaCoverArt`** | **406** | **answers the cover art question** |
| `GamePatchManager` | 391 | |
| `ContentInstaller` | 380 | |
| `TrainerManagerActivity` | 338 | a trainer system, previously unrecorded |
| `CrashReporter` | 317 | |
| `XeniaInputMapping` | 300 | |
| `TrainerManager` | 260 | |
| `GameOptimizationsActivity` | 258 | |

### Cover art, answered

`XeniaCoverArt.java` downloads `xenia-manager/x360db` `games.json`, caches it
for **7 days**, extracts an 8-hex-digit title id by regex, and supports
alternative id matching.

**The pattern generalises: an external per-system database keyed on title id,
cached locally.** `x360db` itself does not; it is Xbox 360 only. The other
seven systems have no source surveyed.

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

## Vulkan device layers — PARTIAL

**READ:** melonDS `VulkanContext` header in full. **LISTED only:** the other
six. The duplication claim rests on the fact that device setup cannot carry
guest semantics, which is an argument rather than a reading.

Verified 2026-08-22. Every fork built its own. **Unlike the LRU caches and the
driver pickers, this cannot be guest-specific**: creating an instance, choosing
a device, setting up queues and allocating memory have no guest semantics.

| Fork | Implementation |
| --- | --- |
| ARMSX2 | `GSDeviceVK`, vendored `vk_mem_alloc.cpp` |
| Cemu-thor | `VKRMemoryManager` |
| azahar-thor | `vk_instance`, `vk_memory_util` |
| melonDS-android | `VulkanContext` |
| Vita3K-Thor | `vulkan/context.cpp`, `vulkan/allocator.cpp`, vendored VMA-Hpp |
| xenia-thor | `ui/vulkan/`, `vulkan_shared_memory` |
| eden-thor | `vulkan_device`, `vulkan_instance` |

Three vendor a memory allocator separately.

**Thor device baseline already measured:** xenia
`docs/research/20260517-142224-thor-vulkan-device-baseline.md`, 2026-05-17.
Board `kalama`, instance API 1.3.0, device API 1.3.128, vendor `0x5143`, GPU
clocks 680 MHz to 124.8 MHz, target recorded as **Thor Max**. Read it before
writing device setup.

## Shader caches — PARTIAL

**READ:** Cemu `LatteShaderCache.h`, which is three lines and only exposes
per-title cache version functions, and Vita3K `pipeline_cache.h` structure.
**LISTED only:** eden `vk_pipeline_cache` and `shader_cache`, azahar disk
shader cache, ARMSX2 backends.

| Capability | Fork | Quality | Notes |
| --- | --- | --- | --- |
| Shader cache | Cemu-thor | shipped | `LatteShaderCache`. |

Not surveyed elsewhere. Most forks probably have one. Shader compile stutter is
a Thor-wide problem and this table is nearly empty. Close that gap.

## Test and QA infrastructure — PARTIAL

**READ:** melonds_HD_2 `renderer_cases` README and `case.json`, the
`xenia-experiment-ledger` skill. **LISTED only:** `pcsx2-gsrunner`, the xenia
trace tooling, the Vita3K suite scripts, azahar `movie.cpp`.

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

## Thor measurement and optimisation harness — PARTIAL

**READ:** `power_affinity_ab.sh` and `bd_adpf_ab.sh` headers, the MCP server
README and tool list. **LISTED only:** the other 130-odd scripts.

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

## Performance hints, pacing, affinity and audio — PARTIAL

**READ:** Cemu `AndroidPerformanceHints.h`, the xenia ADPF and affinity script
headers. **LISTED only:** the Oboe integrations, melonDS `AudioLatency`, eden
`vsync_manager`.

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

## Cemu-thor — LISTED

**Only `AndroidPerformanceHints.h` was opened.** Everything else in this
section is from file names.

Cemu had no capability recorded before 2026-08-22.

| Capability | Quality | Notes |
| --- | --- | --- |
| Android SAF filesystem | shipped | `fscDeviceAndroidSAF`, `FileStream_saf`. |
| Full Android app | shipped | `src/android/app/`, `NativeInput.cpp`. |
| Texture cache and loader | shipped | `LatteTextureCache`, `LatteTextureLoader`. |
| On-screen overlay | shipped | `LatteOverlay`. |
| Per-game controller profile | shipped | `bin/controllerProfiles/CemuThor_StarFoxZero_StarFox64ish.xml`. |
| Audio, full AX | shipped | `snd_core/ax_*`. |

## Agent skills and experiment discipline — PARTIAL

**READ:** all 29 skill descriptions, plus the `xenia-experiment-ledger` and
`xenia-thor-autonomous-driver` bodies. **LISTED only:** the other 27 bodies.

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

## Guest accounts and guest system applets — READ

azahar has the most developed applet system in the fleet and it is structured
the way the shared layer needs.

| Layer | Path |
| --- | --- |
| Frontend interface | `src/core/frontend/applets/` — `swkbd`, `mii_selector`, `default_applets` |
| Guest HLE side | `src/core/hle/applets/` — `applet`, `erreula` (the error applet) |
| Qt implementation | `src/citra_qt/applets/` |
| Android implementation | `src/android/.../applets/` Kotlin plus JNI |

**A frontend-agnostic interface with per-frontend implementations and a
headless default.** That is exactly the shape this project needs, already
built.

Its own header states the split:

> Configuration thats relevent to frontend implementation of applets. Anything
> missing that we later learn is needed can be added here and filled in by the
> backend HLE applet

`Frontend::SoftwareKeyboard`, 152 lines, carries a `KeyboardConfig` with button
configuration, accepted-input mode, multiline, max length and digits, hint
text, caller-supplied button labels, and a filter set covering digits, `@`,
`%`, `\`, profanity and a guest callback. It returns `KeyboardData` and a
`ValidationError` enum with twelve cases.

**Taken into [`shared_layer/thor_backend.h`](shared_layer/thor_backend.h)** as
the guest system UI channel. azahar is GPL-2.0-or-later so it can be used as
GPL-3.0.

Note the config is 3DS-shaped in places: a `Triple` button layout exists
because Nintendo's keyboard has an "I Forgot" button, and the profanity filter
is Nintendo's. **The structure generalises; those specifics do not.**

### Guest accounts

| Fork | Implementation |
| --- | --- |
| Cemu-thor | `src/Cafe/Account/Account.cpp`, `Account.h`, `AccountError.h` |
| azahar-thor | `MiiSelector.kt`, `MiiSelectorDialogFragment.kt`, `mii_selector.cpp` |
| eden-thor | `ProfileAdapter.kt` |

Not found in xenia-thor, Vita3K-Thor or rpcsx-ui-android. **The search was by
name and those forks may use different ones**, which has been wrong before.

## Touch overlays — READ. The clearest duplication in the fleet.

Surveyed 2026-08-22. **azahar and Vita3K ship the same four classes, from the
same 2013 ancestor, in two languages.**

| | azahar-thor | Vita3K-Thor |
| --- | --- | --- |
| `InputOverlay` | 1302 lines, Kotlin | 1067 lines, Java |
| `InputOverlayDrawableButton` | yes | yes |
| `InputOverlayDrawableDpad` | yes | yes |
| `InputOverlayDrawableJoystick` | yes | yes |
| Header | Citra / Azahar, GPL-2.0-or-later | **`Copyright 2013 Dolphin Emulator Project`**, GPL-2.0-or-later |

Vita3K kept Dolphin's copyright header verbatim. azahar's arrived through
Citra. **2,369 lines implementing one design twice.**

**This is a better extraction candidate than the GPU driver manager.** The
driver pickers turned out to be four different concerns. These are the same
code, diverged.

- A touch overlay has **no guest semantics**. Buttons, a dpad and a joystick
  drawn on a screen are the same problem on every system.
- **Both are GPL-2.0-or-later**, so both can be used as GPL-3.0.
- The classes already have the same names, so the contract is already agreed.

### The other four are different, and two have things nobody else does

| Fork | Approach | Notable |
| --- | --- | --- |
| Cemu-thor | data-driven: a 24-line enum plus default configs and a touch listener | **read; see below** |
| melonDS-android | `EmulatorOverlayTracker.kt`, **`TouchVibrator.kt`** | **haptics on touch. Nobody else has it.** |
| eden-thor | overlay assets under `dist/icons/overlay/` | art, not logic |
| xenia-thor | **nothing.** One research note, `20260527-151500-android-ingame-menu-overlay-controller-start.md` | would gain the feature outright |

**melonDS's `TouchVibrator` is the kind of quality-of-life detail this project
exists to spread.** A touch button that does not vibrate feels dead, and six
forks ship one that does not.

**xenia has no touch overlay at all**, despite having the largest Android
shell. Extraction would give it a feature rather than replacing one.

### Cemu's design, read: the comparison resolves

`OverlayInputConfig.kt` is **24 lines**, a flat enum of 21 overlay elements:

```kotlin
BUTTON_A, BUTTON_B, BUTTON_ONE, BUTTON_TWO, BUTTON_C, BUTTON_Z, BUTTON_HOME,
BUTTON_L, BUTTON_L_STICK_CLICK, BUTTON_MINUS, BUTTON_PLUS, BUTTON_R,
BUTTON_R_STICK_CLICK, BUTTON_X, BUTTON_Y, BUTTON_ZL, BUTTON_ZR,
BUTTON_BLOW_MIC, JOYSTICK_LEFT, JOYSTICK_RIGHT, DPAD
```

Two designs, and they are not really competing:

| | Dolphin lineage | Cemu |
| --- | --- | --- |
| Shape | a class per drawable type | an enum of element identities |
| Size | 1000+ lines each | 24 lines plus configs |
| Generality | **generic drawables** | **guest-specific list** |

**Cemu's enum bakes the Wii U controller into the overlay.** `BUTTON_ZL`,
`BUTTON_ONE` and `BUTTON_TWO` are Nintendo, and `BUTTON_BLOW_MIC` is not a
button at all. The Dolphin lineage keeps its drawables generic and maps them
elsewhere.

**Take both halves, from the side that got each right:**

- **Generic drawables from the Dolphin lineage.** A button, a dpad and a
  joystick are universal shapes.
- **A declared element list from Cemu's idea** — but **declared by the
  backend**, not hardcoded, because only the backend knows its guest
  controller.

That is exactly the contract's existing guest input row: the backend declares
its controller shape, the shared layer renders it. **The overlay survey
independently arrived at the contract this repo already wrote.**

`BUTTON_BLOW_MIC` is the useful edge case. A shared overlay must handle an
element that is not a button, so the declaration needs a kind as well as a
name.

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
