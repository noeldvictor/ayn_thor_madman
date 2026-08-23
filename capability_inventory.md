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
| One hotkey set | "nobody has this" | ARMSX2 `SysHotkey` enum + `HotkeysTab` |
| Storage accounting | "nobody has this" | measurement in five forks |
| Cheat manager UI | "nobody has this" | melonDS `ui/cheats`, 2,119 lines |
| Settings migrations | "nothing says how" | melonDS `migrations/`, 37 files |
| Most complete shell | "xenia, 12,313 lines" | **xenia is the SMALLEST Tier 1 frontend** |

**Ten negatives have now been checked and nine were wrong.** Treat a negative in
this file as the least reliable kind of row it contains.

Surveyed 2026-08-22. **The first version of this file was wrong.** It covered
two forks and implied the rest had nothing. Read every "not surveyed" line
below as a gap in the survey, never as an absence in the fork.

## The Android frontends — READ, measured 2026-08-22

**Measured, not estimated.** Own code per `src/main/java`, tests and vendored
trees excluded.

| Fork | Lines | Files | Note |
| --- | --- | --- | --- |
| **melonDS-android** | **78,033** | **698** | largest, and the only layered one |
| **ARMSX2** | 63,111 | 153 | most features this project wants |
| ARMSX3 | 61,319 | 157 | **ARMSX2's UI, vendored whole, package `com.armsx2`** |
| eden | 33,671 | 217 | |
| azahar | 26,919 | 156 | |
| Vita3K | 20,744 | 76 | |
| Cemu | 18,501 | 144 | |
| rpcsx | 18,407 | 85 | |
| **xenia** | **12,334** | **25** | **smallest Tier 1 frontend** |

**This file previously repeated `CLAUDE.md`'s claim that xenia has the most
complete shell in the fleet. It is the smallest**, by a factor of six against
melonDS-android.

### melonDS-android: the only layered architecture — READ

`domain` 120 files, `impl` 97, `di` with Hilt, `database`, `parcelables`, and
**`migrations` with 37 files**. Nothing else in the fleet separates these.

| Capability | Path | Quality |
| --- | --- | --- |
| **Cheat manager UI** | `ui/cheats`, 2,119 lines, 20 files | `shipped` |
| Cheat persistence | `impl/RoomCheatsRepository.kt`, 304 lines | `shipped` |
| Cheat DB import | `impl/XmlCheatDatabaseSAXHandler.kt` + `BundledCheatDatabaseImporter.kt` | `shipped` |
| **Settings migrations** | `migrations/`, 16 concrete, `Migration6to7` to `Migration40to41` | `shipped` |
| Screen layout editor | `ui/layouteditor`, 2,925 lines | `shipped` |
| Cache size in the UI | `VideoPreferencesFragment`, scanned off the main thread | `shipped` |

**The cheat stack uses a streaming SAX parser and a `CheatImportProgress`
model.** Both are marks of a database large enough to be slow — this was used in
anger, not demonstrated.

**The migration framework carries two non-obvious rules.** The schema version is
the app's **own version code**, so there is no separate number to bump; and
`legacy/` **freezes a DTO per version**, because a migration that deserializes
with the current class breaks retroactively, silently, and only for users
upgrading from an old version.

### ARMSX2: the features this project specified — READ

| Capability | Path | Quality |
| --- | --- | --- |
| **Per-game overrides** | `config/ConfigStore.kt`, 240 fields, `merge`/`diff` | `shipped` |
| **Hotkey set + binding UX** | `ControllerMappings.SysHotkey` + `ui/settings/HotkeysTab.kt` | `shipped` |
| **Second-screen `Presentation`** | `SecondScreen.kt` 576 + `SecondScreenTiles.kt` 131, **names the Thor** | `shipped` |
| Settings search | `ui/settingshub/SettingsSearchIndex.kt`, generated, i18n-aware | `shipped` |
| Patch manager | `ui/patches/`, 1,688 lines + `PatchRepo.kt` | `shipped` |
| Texture pack catalogue | `TextureCatalog.kt`, `TexturePackInstaller.kt` | `shipped` |
| Upscale algorithm enum | `Config.h` `GSTextureUpscaleAlgorithm`, 24 entries, documented | `shipped` |

**`ConfigStore` is the important one.** It has three fixes the obvious design
lacks, each from a reported bug: an override must be **sticky** or a later global
change silently takes the game's setting; some settings are **process-wide** and
the per-game file refuses them, so they must be **promoted** to global; and
pinning needs **change-tracking**, or a stale whole-object write makes a wrong
value permanent. See
[`research_log/20260822_2203_armsx2_frontend_is_the_shell.md`](research_log/20260822_2203_armsx2_frontend_is_the_shell.md).

**`SecondScreen.kt` carries three lessons for any `Presentation` work**: do not
put Compose inside one; it is not torn down when the activity stops; re-attach
on every resume, because a dual-screen handheld lets a person move the app.

## The native side — READ, measured 2026-08-22

| System | Fork | Lines | Files |
| --- | --- | --- | --- |
| Switch | eden | 645,359 | 3,006 |
| PS3 | rpcsx | 621,097 | 1,497 |
| PS2 | ARMSX2 | 395,883 | 813 |
| Xbox 360 | xenia | 362,019 | 909 |
| DS | melonDS | 334,719 | 519 |
| Wii U | Cemu | 291,183 | 825 |
| 3DS | azahar | 278,525 | 1,263 |
| Vita | Vita3K | 214,233 | 834 |

**About 3.14M lines of native against 310k of frontend.**

**Counting traps, all hit on the first pass.** Cemu appeared to be 7.2M because
`src/android/` holds a vcpkg tree; xenia appeared to be 1.33M because
`gpu/shaders/bytecode/` holds generated SPIR-V headers up to 20k lines each;
melonDS's "Vulkan layer" appeared to be 123k, mostly generated shader arrays and
a DS rasterizer. **List the biggest files after counting.**

### xenia separated three host layers and nobody else did — READ

This is the strongest single result of the survey.

| Layer | xenia | Everyone else |
| --- | --- | --- |
| Vulkan device | **`ui/vulkan/`, own directory, 11,471 lines** | inside the renderer |
| Code cache | **`a64_code_cache_posix.cc`, 632 lines** | inside the emitter |
| Pipeline cache | **not separated**, 120 lines inside a 2,875-line file | also not separated |

**`src/xenia/ui/vulkan/` against `src/xenia/gpu/vulkan/` is exactly the
shared/not-shared line this project draws, already expressed as a directory.**
And xenia is **BSD**, so extracting from it keeps the shared module usable by
anything.

### Pipeline cache — READ, and it is genuine duplication

**All eight forks call `vkGetPipelineCacheData`.** Unlike the LRU cache, the
shape is fixed by the API with no guest semantics.

**But there are two caches in those files and only one is shareable**: the
driver blob dies on a driver swap and is guest-agnostic; the guest-shader-to-
SPIR-V translation cache survives a driver swap and is guest-specific.

**Invalidation is answered by the Vulkan spec**, not by us: `pipelineCacheUUID`
changes when a driver's caches become incompatible. ARMSX2's `VKShaderCache.cpp`
validates header length, version, vendor ID, device ID and UUID. xenia
deliberately does not, relying on the driver — correct, but it produces **no
signal** when a cache is silently dropped, which on this device presents as
stutter.

### melonDS's ARM64 emitter is Dolphin's, from 2015 — READ

A **third** Dolphin import, separate from the 2008 and 2009 frontend code, and
in the JIT.

**308 emitter methods against Dolphin's current 344.** Dolphin has added 40 that
melonDS never received, including eight vector compares, `BIF`/`BIT`/`EXT`,
`ParallelMoves`, and the whole **`YIELD`/`WFE`/`WFI`/`SEV`/`SEVL`** spin-wait
family — which lands on this repo's existing finding that `yield` is a no-op on
ARM. **Dolphin's file is GPLv2-or-later, so the code can be taken, not only the
idea.**

**A wrong hypothesis, recorded so nobody re-checks it:** Dolphin has **not**
added `SDOT`, `UDOT`, `EOR3` or `BCAX` either. melonDS is not behind on the
device's vector features; the whole lineage never had them.

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

### Vita3K has a content path resolver, and nobody else does

`util/cheat_paths.h`:

```cpp
std::vector<fs::path> get_vitacheat_roots(base, shared, pref);
std::vector<fs::path> get_vitacheat_candidate_files(base, shared, pref, title_id);
std::optional<fs::path> find_vitacheat_file(base, shared, pref, title_id);
```

Its README lists the locations it searches for one title:

```
cheats/<TITLEID>.psv                    cheats/db/<TITLEID>.psv
app shared storage cheats/...           app shared storage cheats/db/...
ux0/vitacheat/db/<TITLEID>.psv          /sdcard/cheats/psvita/<TITLEID>.psv
/storage/<card>/cheats/psvita/...       /storage/<card>/VitaCheat/db/...
/storage/<card>/Roms/psvita/cheats/...
```

**Nine locations, spanning internal storage, app-private storage, SD cards and
three separate community conventions.**

**This is the real Android problem and only one fork models it.** A person's
content is wherever they put it, or wherever the guide they followed told them
to put it. Every other fork assumes one path.

**It generalises past cheats.** The app needs exactly this resolver for HD
packs, mods, translations, saves and ROMs, per system. **Take the shape:
enumerate roots, build candidates, resolve, and report which candidate won.**

Two details worth keeping:

- **The badge falls out of the resolver.** "When a matching file exists, the
  game shows a `C` cheat badge in the app list." The library badge is not a
  separate feature; it is the resolver's result.
- **The README scopes what may ship:** "legally redistributable offline
  single-player cheat files". That is the cheat-database licence question,
  already answered by one fork with a stated policy.

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

### Cemu's patcher, read in full: 1,331 lines of parser and applier

`GraphicPack2PatchesParser.cpp` is 563 lines, `GraphicPack2PatchesApply.cpp` is
768. Together they implement a **PowerPC assembler with a linker**.

**The parser already handles two input formats:**

```cpp
void GraphicPack2::ParseCemuhookPatchesTxtInternal(MemStreamReader&);
bool GraphicPack2::ParseCemuPatchesTxtInternal(MemStreamReader&);
```

A legacy Cemuhook format and Cemu's own. **One engine, two front ends,
already proven inside one codebase** — which is the format-and-engine split
this repo proposed, working in production.

It also has `GetLengthWithoutComment`, `LogPatchesSyntaxError(lineNumber, msg)`
for errors with line numbers, `CancelParsingPatches`, `[group]` sections, and
**`setOrigin` and `setOriginCodeCave`**, so a patch can allocate a code cave
and assemble into it.

**The applier is a relocating linker:**

```cpp
bool _relocateAddress(PatchGroup*, PatchContext_t*, uint32 addr, uint32& out);
PATCH_RESOLVE_RESULT PatchEntryInstruction::resolveReloc(
    PatchContext_t&, PPCAssemblerReloc*);
uint32 mask = 0xFFFFFFFF >> (32 - reloc->m_bitCount);
```

**`PPCAssemblerReloc` carries a bit count**, so it patches individual
instruction *fields*, not bytes. It resolves symbols against a live module
through `RPLLoader_GetHandleByModuleName`.

### `ResolvePresetConstant` is the mechanism nobody else has

```cpp
bool GraphicPack2::ResolvePresetConstant(const std::string& varname,
                                         double& value) const;
```

**A graphic pack preset feeds a constant into a patch.** A user-selectable
option in the UI becomes a value the assembler substitutes before relocating.

That is the missing link between **per-game settings** and **code patches**.
This repo specified per-game performance patches and per-game overrides as
separate features; Cemu already connects them. A "resolution multiplier"
setting can drive the value a patch writes.

**Nothing else in the fleet parameterises a patch from a setting.**

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

### "Patch" means three different things in this fleet

Read 2026-08-22. **This repo conflated two of them once already, and there are
three.**

| Meaning | What it does | Where |
| --- | --- | --- |
| **Content patch** | game updates, DLC, mods the guest filesystem serves | eden `patch_manager` |
| **Code patch** | modify guest instructions at run time | Cemu `GraphicPack2Patches`, xenia `patcher` |
| **File mod** | replace game assets | eden `mod_manager` |

**eden's `patch_manager` is not a code patcher.** Its types say so:

```cpp
enum class PatchType { Update, DLC, Mod };
struct Patch { name, version, type, source, location };
class PatchManager { using BuildID = std::array<u8, 0x20>; ... };
```

Updates, DLC and mods, with a version and a location. **It belongs with file
mods, not with the code patchers**, and this repo filed it wrongly.

**Cemu's `GamePatch.h` is not a patch system at all.** Its entire contents:

```cpp
void GamePatch_scan();
bool GamePatch_IsNonReturnFunction(uint32 hleIndex);
```

That is HLE function analysis, identifying non-returning functions. **Filed
under patches here purely because of its name.**

So the fleet has **two** code patchers, not three: Cemu `GraphicPack2Patches`
and xenia `patcher`.

**Rule: never file a capability by the word in its filename.** Three
subsystems called "patch" do three unrelated jobs.

### A third way to bind a patch to a build

`PatchManager` uses `BuildID = std::array<u8, 0x20>`, a 32-byte build
identifier.

That makes three approaches, all different, and no fork aware of the others:

| Fork | Method |
| --- | --- |
| Cemu | match a loaded `RPLModule` |
| rpcsx | `PatchHashRepository`, match by hash |
| eden | a 32-byte `BuildID` |

**Three solutions to one problem is the strongest signal for a shared design.**
Unlike the LRU caches, these solve the identical problem: does this patch belong
to the binary in front of me.

### xenia's patcher: BSD, TOML, flat values

`src/xenia/patcher/`, four files, **BSD licensed**. `PatchDataValue` is a sized
byte array with a templated constructor; patches are authored in TOML, parsed
with `cpptoml` in this fork and `tomlplusplus` upstream.

**Flat byte patches, no symbols, no expressions, no relocation.** Far simpler
than Cemu's and far more permissively licensed.

### The split mirrors the cheat finding exactly

| | Format, authoring | Engine, execution |
| --- | --- | --- |
| **Cheats** | six formats, two converters exist | eden's bytecode VM |
| **Patches** | xenia `.patch.toml`, **BSD**, plus Ghidra emitter | Cemu's symbolic assembler, MPL-2.0 |

**Take xenia's TOML as the authoring surface and Cemu's resolver as the
engine.** TOML is human-readable, already has a Ghidra emitter in
`emit_patch_toml.py`, and is BSD. Cemu's resolver has the symbol table,
expressions, relocation and multi-pass resolution that flat values cannot
express.

Both licences work in a GPL-3.0 app, so capability decides the engine and
readability decides the format. **Neither needs writing.**

### A convention worth adopting: `NOTE(thor):`

xenia's `patch_db.h` carries:

```cpp
// NOTE(thor): the upstream patcher parses with tomlplusplus; this fork ships
// cpptoml instead, so the TOML node types in the private signatures below are
// cpptoml's.
```

**A marked annotation for every place the fork diverges from upstream.** That
is the provenance rule applied at line level, and it makes a later re-base
survivable. Adopt it fleet-wide.

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

**CORRECTED 2026-08-22 by measurement. xenia has the SMALLEST Tier 1 frontend
and the worst structure.** 12,334 lines of Java across the emulator package,
against melonDS-android's 78,033 and ARMSX2's 63,111.

**The accretion is real. The completeness was assumed.** What xenia genuinely
has is the most complete *feature list* — it reached for more shell features
than anyone else — and the worst structure to hold them in. Take the list, not
the shape, and take the structure from melonDS-android.

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

**CORRECTION: rpcsx has one, and the search missed it.**

`Emu/RSX/Overlays/overlay_user_list_dialog.h`:

```cpp
struct user_list_dialog : public user_interface {
  struct user_list_entry : horizontal_layout {
    user_list_entry(const std::string& username,
                    const std::string& user_id,
                    const std::string& avatar_path);
  };
  error_code show(const std::string& title, u32 focused,
                  const std::vector<u32>& user_ids, bool enable_overlay,
                  std::function<void(s32 status)> on_close);
};
```

**That is the `RequestUserSelect` applet drafted in
[`thor_backend.h`](shared_layer/thor_backend.h), already built** — and it carries an
`avatar_path` the draft omitted.

The earlier search used `profile|account|gamertag|mii|user_data|nnid|xuid|npid`
and `user_list_dialog` matches none of them. **Fourth time a negative result
was a narrow search.**

**rpcsx is GPL-2.0-only. Take the shape, not the code.**

**CORRECTIONS: both of those were wrong too. Guest accounts exist in six of
eight forks.**

A broader search on 2026-08-22 found what `profile|account|gamertag|mii|nnid`
missed:

| Fork | Implementation |
| --- | --- |
| Cemu-thor | `src/Cafe/Account/Account.cpp`, `AccountError.h` |
| azahar-thor | `MiiSelector.kt`, `mii_selector.cpp` |
| eden-thor | `ProfileAdapter.kt` |
| rpcsx-ui-android | `overlay_user_list_dialog.h` |
| **xenia-thor** | **`kernel/xam/profile_manager.{cc,h}`, `kernel/xam/ui/create_profile_ui.cc`, `kernel/user_module.{cc,h}`** |
| **Vita3K-Thor** | **`data/NativeUser.kt`, `data/UserRepository.kt`, `ui/screens/UserManagementScreen.kt`, `ui/viewmodel/UserManagementViewModel.kt`** |

xenia's sits in `kernel/xam/`, the Xbox 360 system API layer, and includes a
**profile creation UI**. Vita3K's is a full Android MVVM stack: a native user
type, a repository, a Compose screen and a view model.

**Six of eight forks. Guest accounts are a core feature, not an edge case**,
and this repo recorded them as a minority one for two days.

**That is the fifth and sixth time a negative result was a narrow search.**

### The one negative that held

**xenia has no touch overlay.** Searched twice, the second time with `touch`,
`virtual pad`, `virtual control`, `onscreen`, `screen control`, `softkey` and
`gamepad view`. Nothing outside documentation.

**A negative is only worth recording after a second search with different
words.** One of seven has now survived that test.

## rpcsx has a complete in-game menu framework

`Emu/RSX/Overlays/`, inherited from rpcs3.

| Group | Files | Contents |
| --- | --- | --- |
| `HomeMenu` | 16 | main menu, **page**, **components**, cheats, savestate, settings, message box |
| `Network` | 4 | |
| `Shaders` | 4 | |
| `Trophies` | 2 | achievements |
| `FriendsList` | 2 | |
| top level | several | `overlay_video`, `overlay_utils`, `overlay_user_list_dialog` |

**`home_menu_page` derives from `list_view`**, and there is a shared
`components` header. So it is a page-and-component framework, not a pile of
one-off dialogs.

**This is `app/SCREENS.md` screen 3, the in-game overlay, already built** —
save state, cheats, settings and a message box, as pages in a reusable
framework.

It also has three things the screen list never considered: **trophies and
achievements**, a **friends list**, and **video playback inside the overlay**.
Whether those belong in this app is a separate question, but they are evidence
of what an in-game overlay grows into.

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
| melonDS-android | `EmulatorOverlayTracker.kt`, `TouchVibrator.kt` | haptics on touch. **azahar has `hapticFeedback` too; this is two forks, not one.** |
| eden-thor | overlay assets under `dist/icons/overlay/` | art, not logic |
| xenia-thor | **nothing.** One research note, `20260527-151500-android-ingame-menu-overlay-controller-start.md` | would gain the feature outright |

**Haptics exist in two forks, not one.** melonDS has `TouchVibrator` and azahar
has `hapticFeedback`. A touch button that does not vibrate feels dead, and five
forks ship one that does not.

**Two more found by reading the overlay divergence:**

- **azahar has `swapScreen`.** The 3DS is dual-screen and azahar can swap the
  panels. Directly relevant to the Thor's two displays.
- **Vita3K hides the overlay when a physical controller attaches**, through
  `attachController`, `setAllowVirtualController` and
  `updateVirtualControllerState`, plus an auto-hide timer.

**The Thor has physical controls.** An overlay drawn permanently over a game on
a device with real buttons is wrong, and only one fork solved it.

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

## Frame generation — READ. Unrecorded until now, and the biggest QOL find.

**ARMSX2 has a complete Vulkan frame-generation subsystem. 31 files. Nothing
else in the fleet has one.**

`pcsx2/GS/Renderers/Vulkan/FrameGen/`:

| Group | Files |
| --- | --- |
| Core | `FrameGen`, `FrameGenTypes`, `FrameGenPacer` |
| Lsfg pipeline | `LsfgAlpha`, `LsfgBeta`, `LsfgGamma`, `LsfgDelta`, `LsfgChain`, `LsfgCommon` |
| Support | `LsfgGenerate`, `LsfgMipmaps`, `LsfgShaders`, `LsfgTranslate`, `LsfgUtil`, `LsfgVkCompat`, `LosslessDll` |

**All 51 licence identifiers are GPL-3.0-or-later**, so it drops straight into
the GPL-3.0 app.

### Why this matters on a handheld

Frame generation turns a 30 fps game into something that feels like 60 without
rendering more frames. On a device where xenia's ledger says the bottleneck is
GPU-bound and architectural, **generating frames buys smoothness that
optimisation cannot.**

`FrameGenPacer` also connects directly to
[`THOR_RENDER.md`](shared_layer/THOR_RENDER.md) commitment 6, since generated frames need
their own pacing decision.

### Its provenance comment is the model this repo asked for

```
// SPDX-FileCopyrightText: Copyright 2026 Eden Emulator Project
// SPDX-License-Identifier: GPL-3.0-or-later
// SPDX-FileCopyrightText: Copyright 2025 lsfg-vk
//
// Ported from Eden (eden-emu PR #4263),
// src/video_core/renderer_vulkan/present/frame_gen.h.
// The orchestration is unchanged - warm-up counting, the rebuild conditions,
// the flow-scale ...
```

**It names the source project, the pull request, the file, and what was kept
unchanged.** That is exactly what the provenance rule in `CLAUDE.md` asks for,
and it is the only example in the fleet that does it properly.

### The local eden checkout does not have it

A search of `eden-thor` for frame generation found nothing, because the port
came from **eden PR #4263** and the local checkout predates it. `eden-thor` is
one commit ahead of a stale upstream reference.

**So the fleet is out of date with its own members.** ARMSX2 has an eden
feature that the eden checkout in this fleet does not.

## Dual-screen routing — READ. The flagship feature already exists, twice.

Surveyed 2026-08-22. **This repo designed dual-screen routing from first
principles while two forks already shipped it.**

### azahar has a secondary-display layout system

`display/ScreenLayout.kt` carries four enums, one of them a **secondary display
layout with eight modes**:

```kotlin
enum class SecondaryDisplayLayout {
  NONE, TOP_SCREEN, BOTTOM_SCREEN, SIDE_BY_SIDE,
  REVERSE_PRIMARY, ORIGINAL, HYBRID, LARGE_SCREEN
}
```

Plus `ScreenLayout` with `ORIGINAL`, `SINGLE_SCREEN`, `LARGE_SCREEN`,
`SIDE_SCREEN`, `HYBRID_SCREEN`, `CUSTOM_LAYOUT`; a `SmallScreenPosition` with
**eight positions** including `ABOVE` and `BELOW`; and a separate
`PortraitScreenLayout`.

There is also `ScreenAdjustmentUtil.kt` and, on the Qt side,
`configure_layout_cycle` — **cycling through layouts with a hotkey.**

**A design lesson is embedded in a comment:**

> NONE is no longer selectable in the interface, having been replaced with the
> boolean ENABLE_SECONDARY_DISPLAY setting, but is left here for backwards
> compatibility

They shipped `NONE` inside the enum, learned a separate boolean was better, and
kept the value only for compatibility. **That is design experience no amount of
reasoning produces.**

### melonDS models exactly the Thor's two panels

```kotlin
enum class DualScreenPreset {
  OFF,
  INTERNAL_TOP_EXTERNAL_BOTTOM,
  INTERNAL_BOTTOM_EXTERNAL_TOP,
}
```

with `defaultInternalAlignment()` and `defaultExternalAlignment()` returning a
`ScreenAlignment` of `TOP` or `BOTTOM`, plus `layout/BackgroundMode.kt` and
`layout/Insets.kt`.

**Internal display and external display, with both orderings and a per-display
alignment.** That is the Thor's routing problem, already solved.

### Against what this repo designed

`app/shell/.../Model.kt` defined `ONE_EACH`, `BOTH_MAIN`, `MAIN_ONLY`,
`SWAPPED`.

| This repo | Already exists |
| --- | --- |
| `ONE_EACH` | melonDS `INTERNAL_TOP_EXTERNAL_BOTTOM` |
| `SWAPPED` | melonDS `INTERNAL_BOTTOM_EXTERNAL_TOP`; azahar `REVERSE_PRIMARY` |
| `BOTH_MAIN`, `MAIN_ONLY` | melonDS `OFF`; azahar `ENABLE_SECONDARY_DISPLAY` |
| — | azahar `SIDE_BY_SIDE`, `HYBRID`, `LARGE_SCREEN` |
| — | per-display alignment, insets, background mode |
| — | layout cycling by hotkey |

**Both existing designs are richer.** Take melonDS's internal-and-external
model and azahar's layout set, and stop designing this.

### Why this one stings

Dual-screen routing was recorded as **the differentiator no multi-device
frontend can copy**. That is still true of the *hardware*. It is not true of
the *software*: two forks in this fleet already route guest screens to a
secondary display, and one of them names the case exactly as the Thor presents
it.

## Save states, rewind and recompiler tests — READ

### melonDS is the only fork with rewind

`app/src/main/cpp/RewindManager.cpp` and `.h`, plus a UI under
`ui/emulator/rewind/`. **Nothing else in the fleet has it.**

A first search for undo and rewind terms found nothing anywhere. A second pass
with different words found this. **That is twice now that a negative result was
a bad search**, so the rule stands: state the method with the result.

### Nobody has undo save state

Searched twice, with `undo.?(save|load).?state`, `UndoSaveState`,
`backup.?state`, then with `rewind`, `snapshot`, `state_history`. **No fork has
an undo slot.**

`PATTERNS.md` lists an undo slot as part of the shared state pipeline. It
would be a genuinely new feature rather than an extraction.

### Three save-state architectures, and two answers to thumbnails

| Fork | Approach |
| --- | --- |
| ARMSX2 | hand-rolled versioned binary, `SaveStateBase` with `gzLoadingState` and `gzSavingState` derived classes |
| azahar | **Boost.Serialization**, with adapters for `flat_set`, `interval_set`, `small_vector`, `std::variant`, `vector` and `atomic` |
| melonDS | a Kotlin slot model over a native payload |

**ARMSX2 versions explicitly and enforces it socially:**

```cpp
static const u32 g_SaveVersion = (0x9A59 << 16) | 0x0000;
// NOTICE: When updating g_SaveVersion, please make sure you add the following
// line to your commit message somewhere
```

**A process rule embedded in a header comment.** Every emulator has the
problem that a state format change silently breaks old states, and ARMSX2
answers it with a magic-plus-version constant and a commit convention.

Its `freezeData` also carries a note that the old struct was system-dependent
because `int` size differs between systems. **That is a portability scar worth
inheriting the lesson from, not the struct.**

### Thumbnails: embedded or sidecar

| Fork | Where the screenshot lives |
| --- | --- |
| ARMSX2 | **inside the state**, `SaveStateScreenshotData` |
| melonDS | **beside it**, a `screenshot: Uri` on the slot |

Embedded survives a file copy and cannot desynchronise. Sidecar can be read
without parsing the state, which matters for a library screen listing many
slots.

**The library needs the sidecar property and the integrity of the embedded
one.** Neither fork has both, and this is a real design choice rather than a
pick-the-winner.

### melonDS's save state model is the one to take

```kotlin
data class SaveStateSlot(
    val slot: Int, val exists: Boolean,
    val lastUsedDate: Date?, val screenshot: Uri?,
) { companion object { const val QUICK_SAVE_SLOT = 0 } }

enum class SaveStateLocation { SAVE_DIR, ROM_DIR, INTERNAL_DIR }
```

**A screenshot per slot, a last-used date, and quick-save pinned to slot 0.**
That is the thumbnail and slot model `app/SCREENS.md` specified, already built.

### CORRECTION: ARMSX2 has recompiler differential testing

This repo recorded that no fork does the CPU form of differential testing, and
that only the GPU form existed in `melonds_HD_2`. **Wrong.**

`tests/ctest/core/recompilers/` holds a real suite:

| File | What it covers |
| --- | --- |
| `harness/StateSnapshot.{h,cpp}` | register state for **both** CPUs, `R3000A` and `R5900`, plus a fixed-size memory window |
| `arm64_baseblocks_link_tests.cpp` | ARM64 basic-block linking |
| `autocases_eecache.h` ×3 | EE cache behaviour |
| `autocases_eelsu.h` | EE load/store unit |
| `autocases_efu.h`, `autocases_fpuovf.h` | FPU, including overflow |
| `autocases_vu0macro.h`, `autocases_vubranch.h`, `autocases_vulat.h` | vector unit macros, branches, latencies |
| `autocases_iopmisc.h`, `autocases_sa.h` | IOP and shift-amount cases |

**The `autocases_` prefix means generated cases**, so this is systematic
recompiler testing, not a handful of hand-written checks. Capturing registers
plus a memory window is exactly the differential-testing shape.

**That is the third "no fork has this" claim to be wrong.**

### The `yaps2` ancestor, identified

`StateSnapshot.h` carries `SPDX-FileCopyrightText: 2026 yaps2 Dev Team`. The
SPDX pass found 207 files attributed to a "yaps Dev Team" in ARMSX2 and could
not place them. **`yaps2` is a PS2 project ARMSX2 draws from**, and the test
harness is part of what it took.

## ARM64 target features — checked fleet-wide, and most forks leave them off

Surveyed 2026-08-22, following rpcs3's finding that name-based feature
detection silently excluded every Qualcomm core.

**The device has `asimddp` and `sha3`.** Build flags across the fleet:

| Fork | `-march` / `-mcpu` found | Verdict |
| --- | --- | --- |
| **xenia-thor** | `armv8-a+crypto+sha3+crc+dotprod` | **complete.** Enables exactly what the device has |
| Vita3K-Thor | `mcpu=cortex-x3` | tunes for the prime core by name |
| Cemu-thor | `armv8-a+lse`, `mcpu=cortex-a710` | LSE atomics, tuned for a mid core |
| rpcsx-ui-android | `armv8-a+lse`, `+crypto` | LSE, crypto, **no dotprod or sha3** |
| **ARMSX2** | `armv8-a`, `armv8-a+crc` | **baseline. No dotprod, no sha3.** |
| **azahar-thor** | `armv8-a` | **baseline** |
| melonDS-android | `armv4`, `armv5`, `haswell` | those are **guest** targets; no ARM64 host flags found |
| eden-thor | none found | |

**xenia is the only fork enabling the device's actual feature set**, and it is
the fork that wrote the research document. The others are compiling for a
generic ARMv8-A that this device has not been for years.

### Two caveats before acting

- **Build flags are not the whole story.** A recompiler emits its own
  instructions and an LLVM-based backend passes target attributes separately.
  A fork can have baseline `-march` and still emit `SDOT` from its JIT.
- **Vita3K and Cemu tune for one core each**, `cortex-x3` and `cortex-a710`.
  On a 1+4+3 device threads land on all of them, and
  [the core comparison](hardware_ref/thor/cpu/CORE_COMPARISON.md) shows the
  guides give conflicting advice. Tuning for one core is a choice, not an
  oversight, but it should be a stated one.

### The caveat checked: the emitters agree with the flags

Searching the ARM64 backends themselves, not the build files:

| Fork | `SDOT`/`UDOT` in source | `EOR3`/`BCAX`/`RAX1`/`XAR` in source |
| --- | --- | --- |
| **xenia-thor** | **2 files** | **6 files** |
| Cemu-thor | 1 | 1 |
| **ARMSX2** | **0** | **0** |
| **melonDS-android** | **0** | **0** |

**The caveat does not rescue anyone.** ARMSX2 and melonDS have baseline build
flags **and** no emitter support. xenia leads at both levels, consistent with
its flags.

**xenia is the only fork in the fleet using the device's vector features at
all.**

### The ARMSX2 case is the notable one

ARMSX2 is the seed of the shared layer, holds the most Thor-specific research
in the fleet — texture upscaling, the neural path, the MCP design, the ARM64
review — and **emits neither dot-product nor three-input bitwise
instructions.**

**The PS2's VU is a vector unit for 3D maths. Dot products are what it does.**
`SDOT` and `UDOT` exist on this device, and `EOR3` and `BCAX` collapse
three-input bitwise sequences that a VU mask synthesis produces constantly.

That is a specific, checkable opportunity in the fork this project cares most
about, and it needs a device A/B rather than an assumption.

**This is cheap to check and cheap to fix**, which is what makes it worth doing
before any deeper optimisation work.

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
