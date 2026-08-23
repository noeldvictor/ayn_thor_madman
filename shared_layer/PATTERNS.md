# The generalized pattern catalogue

**Every emulator is the same eight pipelines with different guest hardware.**

This document names those pipelines, maps each fork's version onto them, and
records where the shape genuinely differs. It is the step between
[`../capability_inventory.md`](../capability_inventory.md), which lists what
exists, and a shared layer, which needs a contract.

Written 2026-08-22. Incomplete. Add to it whenever a fork is read.

## Why this is the missing artifact

The inventory records that six forks each wrote a GPU driver picker. That is a
fact about duplication. It does not say what the shared thing **is**.

A pattern is the shared thing. A fork's implementation is one instance of it.
The contract is written against the pattern, never against one fork's version.

## The eight pipelines

Every emulator in the fleet does these. The guest hardware differs; the
pipeline does not.

| # | Pipeline | Guest side | Host side |
| --- | --- | --- | --- |
| 1 | **Code translation** | guest CPU instructions | ARM64 code + code cache |
| 2 | **Texture upload** | guest texture memory | Vulkan image + texture cache |
| 3 | **Shader translation** | guest shader program | SPIR-V + pipeline cache |
| 4 | **Memory mapping** | guest address space | host pages |
| 5 | **Presentation** | guest screens | Thor displays |
| 6 | **Input** | guest controller | Thor controls + touch |
| 7 | **State serialization** | guest machine state | a save state file |
| 8 | **Configuration** | per-title behaviour | settings, patches, cheats |

Below, each pipeline lists what each fork calls its version.

---

## 1. Code translation

Four forks generate ARM64 code from a different guest ISA.

| Fork | Guest ISA | Implementation |
| --- | --- | --- |
| xenia-thor | PowerPC, Xenon | `src/xenia/cpu/backend/a64/`, `a64_code_cache` |
| Cemu-thor | PowerPC, Espresso | `Cafe/HW/Espresso/Recompiler/BackendAArch64/` |
| melonDS-android | ARM7 and ARM9 | `melonDS-android-lib/src/ARMJIT_A64/` |
| ARMSX2 | MIPS, EE and IOP | `common/emitter/` |

**Shared:** the code cache, its eviction policy, its memory protection, the
flush protocol, and cluster-aware placement of generated code. The Thor has
one X3 prime core, and where generated code runs matters.

**Not shared:** the guest ISA decoder and the instruction lowering. Those are
the emulator.

**Prior art to read first:** xenia has roughly 40 audit scripts on exactly this
(`thor_a64_*`, `thor_hir_*`) and a skill, `aarch64-snapdragon-jit-port`. It is
far ahead. See [`../capability_inventory.md`](../capability_inventory.md).

**Caution:** this is the deepest possible reach into a core. Do not attempt it
before pipelines 2 and 3 are proven.

---

## 2. Texture upload

Every fork has a texture cache. This is the pipeline the flagship feature lives
in.

| Fork | Implementation |
| --- | --- |
| ARMSX2 | `GSTextureCache`, plus `GSTextureUpscaler` |
| Cemu-thor | `LatteTextureCache`, `LatteTextureLoader` |
| melonDS-android | `GPU3D_Texcache` |
| Vita3K-Thor | `renderer/texture_cache.h` |
| azahar-thor | `custom_textures/custom_tex_manager` |
| xenia-thor | `src/xenia/gpu/`, plus `texture_dump.cc` |

**Read 2026-08-22.** See
[`../research_log/20260822_2015_texture_cache_read.md`](../research_log/20260822_2015_texture_cache_read.md).

**Shared:** cache storage and lookup, eviction, the memory budget, the upload
path, the point where replacement and upscaling hook in, pack loading with its
async upload worker, and statistics with separated decline reasons.

**Not shared, and this is a correction:** **hashing is not shared.** It hashes
guest texel formats and guest palettes. The shared cache takes an **opaque key
the backend computes**, never a key the shared layer defines. Also not shared:
overlap and aliasing models, and format conversion including YUV.

**The strongest shared-key insight:** ARMSX2 and melonDS independently arrived
at the same design, a content hash of texel data plus a **separate** hash of
the palette. ARMSX2 goes further with `WithRemovedCLUTHash()`, so the same
texel data can be found independently of its palette. melonDS has no
equivalent. Both target paletted-heavy machines.

Cemu keys on physical address instead, and tracks textures that alias in memory
at slice and mip granularity. Vita3K folds sampler state and YUV conversion
into the same class. azahar's `CustomTexManager` is not a cache at all; it is a
replacement asset manager with a `Material` model and async upload, and it is
the best pack-loading model in the fleet. Read on 2026-08-22, four things to
take from `material.h`:

- **`std::vector<u64> hashes` per custom texture.** One replacement asset maps
  to many guest hashes, so assets dedupe across textures. No other fork does
  this.
- **An async decode state machine**, `None`, `Pending`, `Decoded`, `Failed`,
  held atomically, so the renderer never blocks on disk.
- **PNG and DDS.** DDS stays compressed on disk and on the GPU, which matters
  on a device with a hard storage and VRAM ceiling.
- **Two maps only**, `Color` and `Normal`. Not a PBR stack. An earlier note in
  this repo overstated this.

**The open design decision, and it is the first one to settle:** melonDS puts
`HDTexPack* TexPack` and `HDTexPack* FilterCache` **inside** the cache entry.
ARMSX2 keeps its upscaler deliberately pure and outside, because
`GSTextureCache` owns insertion and "inventing a second owner of that lifetime
is how it gets corrupted". Opposite answers, each with a stated reason.
Recommendation: take the pure module, because a shared upscaler cannot own six
different caches' lifetimes.

**The classification difference is real and must be preserved:**

| Fork | Classes |
| --- | --- |
| ARMSX2 | `World`, `Ui` |
| melonDS-android | `TEXTURE_3D`, `OBJ_SPRITE`, `BG_LAYER` |

The contract takes a declared class list per backend. It does not impose one.

---

## 3. Shader translation

Every fork translates guest shaders and caches the result. Two forks share a
class name because they share an ancestor.

| Fork | Implementation |
| --- | --- |
| Cemu-thor | `LatteShaderCache`, `ShaderSerializer` |
| Vita3K-Thor | `renderer/vulkan/pipeline_cache.h`, `spirv_recompiler` |
| azahar-thor | `DiskShaderCacheProgress.kt`, disk shader cache |
| eden-thor | `DiskShaderCacheProgress.kt` |
| ARMSX2 | `D3D11ShaderCache` and the GL and Vulkan equivalents |
| xenia-thor | `llvm_object_cache` |

azahar and eden have the **same class name** for the shader cache progress UI,
because both descend from the citra and yuzu lineage. That is inherited
duplication rather than independent invention, and it is still duplication.

**Shared:** the on-disk cache format, the pipeline cache, the compile
scheduling and threading, and the progress UI. Shader compile stutter is a
Thor-wide problem and one cache can serve every backend.

**Not shared:** guest shader decoding and the SPIR-V emission for that guest.

**This is the best first performance target.** It is measurable as frametime
spikes, a player feels it immediately, and it needs no renderer internals.

---

## 4. Memory mapping

Least surveyed pipeline. Every emulator maps a guest address space onto host
pages, and the Thor's page size and address layout are fixed.

Known: melonDS has `ARMJIT_Memory.cpp`; `melonds_HD/CLAUDE.md` records DTCM
fastmem work.

**Shared, probably:** the host reservation strategy, fastmem fault handling,
and the page size assumptions.

**Survey this before assuming anything.** Nothing else is recorded.

---

## 5. Presentation

Every fork puts guest screens onto host displays. **The Thor has two internal
touch displays**, which makes this pipeline unusually valuable here.

| Guest screens | Forks |
| --- | --- |
| Two, lower is touch | melonDS-android, azahar-thor |
| TV plus a touch GamePad | Cemu-thor |
| One | ARMSX2, Vita3K-Thor, xenia-thor, eden-thor |

**Shared:** swapchain management, the routing of a guest screen to a physical
display, layout, aspect handling, frame pacing and the present path.

**Not shared:** how many screens the guest has and what each contains.

**Contract:** a backend declares its guest screens with a name, a size, an
aspect, whether it takes touch, and whether the game needs it. The app owns
the routing. See `../CLAUDE.md`, Dual-screen routing.

---

## 6. Input

Every fork maps Thor controls to a guest controller.

Known: Cemu has `NativeInput.cpp` and a per-game controller profile for Star
Fox Zero; xenia has `ControllerMappingActivity`.

**Shared:** the physical control read, the remap layer, the touch overlay, and
**the universal hotkey set**. One hotkey does the same thing on every system.

**Not shared:** the guest controller's shape, and touch semantics per guest.

---

## 7. State serialization

Every fork saves and restores guest state, and every format differs.

| Fork | Implementation |
| --- | --- |
| ARMSX2 | `SaveState.cpp`, `SaveStateLegacy.cpp` |
| melonDS-android | `SaveStateSlot`, `SaveStatesRepository`, `SaveStateLocation` |
| Cemu-thor | `RegisterSerializer`, `ShaderSerializer` |
| azahar-thor | `src/common/serialization/` |
| eden-thor | `SerializableHelper.kt` |
| Vita3K-Thor | roadmap only, see `reports/20260511_204953_...md` |

**Read 2026-08-22. Three architectures:** ARMSX2 hand-rolls a versioned binary
with `gzLoadingState` and `gzSavingState`; azahar uses **Boost.Serialization**
with adapters for its container types; melonDS wraps a native payload in a
Kotlin slot model.

**Shared:** slot management, naming, location, thumbnails, the undo slot, the
retention policy, and **version discipline**. Also **the test harness**: a
savestate is the fixture that makes a deterministic test possible.

**Not shared:** the serialized payload. That is the guest machine.

**Version discipline is the part nobody would think to share.** ARMSX2 carries
`g_SaveVersion = (0x9A59 << 16) | 0x0000` and a header comment requiring a
specific line in the commit message when it changes. A format change silently
breaking old states is a problem every fork has; only one answers it with a
process.

**Thumbnails have two valid answers.** ARMSX2 embeds a
`SaveStateScreenshotData` inside the state; melonDS keeps a `screenshot: Uri`
beside the slot. Embedded cannot desynchronise; sidecar can be listed without
parsing the state. **A library screen showing many slots wants the sidecar
property and the embedded one's integrity**, and no fork has both.

---

## 8. Configuration, patches and cheats

Every fork has per-game settings, and several have patches and cheats.

**Shared:** the settings schema with stable keys, the per-game override
resolution order, the patch engine, the cheat library, and the UI for all of
it.

**Not shared:** which settings exist for a given backend, and the guest
addresses a patch touches.

Two patch systems already exist. Choose one:

- xenia `.patch.toml`, with `src/xenia/patcher/` and Ghidra authoring.
- Cemu `GraphicPack2Patches`, with runtime ASM patching and its own parser.

---

## Support patterns

Smaller, and the easiest possible proof that extraction works.

### The LRU cache: read, and NOT duplication

**This entry previously claimed three forks wrote the same data structure.
That was wrong.** Reading them on 2026-08-22 reversed it. See
[`../research_log/20260822_1915_lru_cache_extraction_test.md`](../research_log/20260822_1915_lru_cache_extraction_test.md).

| | ARMSX2 | azahar-thor | eden-thor |
| --- | --- | --- | --- |
| Storage | `std::map` | `std::array`, static | intrusive list plus pool |
| Ordering | access counter | `std::list` | tick based |
| Capacity | runtime, resizable | **compile time** | pool grows |
| Allocation | dynamic | **none** | pooled |
| Licence | GPL-3.0+ | **Boost** | GPL-2.0-or-later |

Three designs for three constraints. azahar's avoids allocation on purpose;
eden's tracks emulated time rather than access order. Consolidating them would
force one design onto three problems, which is a regression dressed as
cleanup.

**The lesson generalises: counting implementations is not evidence of waste.**
Read every implementation before recording a duplication. A capability row
that was never read is a hypothesis, not a finding.

### GPU driver negotiation

Six forks, listed in the inventory. xenia's is BSD, which keeps the shared
module permissive.

### Android platform glue

| Capability | Forks |
| --- | --- |
| `id_cache.cpp`, JNI id caching | azahar-thor, eden-thor |
| Storage Access Framework | Cemu-thor |
| ADPF performance hints | Cemu-thor, xenia-thor |
| Oboe audio | ARMSX2, melonDS-android, eden-thor |

None of this is emulation. All of it is written more than once.

---

## What this catalogue changes

1. **Write the contract against a pattern, never against a fork.** The
   contract for texture upload is not ARMSX2's `GSTextureUpscaler` with the
   names changed.
2. **Extraction order should follow risk, not value.** Start with the LRU
   cache and the driver manager, which have no guest-specific behaviour. Then
   the shader cache. Then texture upload. Code translation last.
3. **"Not shared" is as important as "shared".** Each pipeline above names
   what must stay in the backend. A shared layer that swallows those is wrong
   and will be slower.

## Gaps in this catalogue

- Memory mapping is barely surveyed.
- Audio is listed only as backends, not as a pipeline.
- No fork's implementation has been read in depth for pipelines 3 to 7. The
  names came from file listings.
- GameThor is absent. It translates an API rather than emulating hardware, so
  it may not fit these pipelines at all. That difference is worth
  understanding, because the xenia experiment ledger argues translation beats
  emulation on this device.
