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

**Nine since 2026-08-23**, when audio was promoted from a list of vendored
backends to a pipeline of its own.

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

**Surveyed 2026-08-23: frame pacing has no incumbent anywhere.** **No fork uses
Swappy. No fork uses `VK_GOOGLE_display_timing`.** Every fork selects a Vulkan
present mode and stops; azahar and melonDS use `Choreographer`, which is the
vsync signal and not the pacing policy.

**`FIFO` is vsync, not pacing.** A 20 ms frame at 60 Hz misses its vsync and
alternates 16.6, 33.3, 16.6 — judder that is more visible than a stable 30.

**This makes presentation the cheapest pipeline in the catalogue to take**:
nothing to extract, nothing to reconcile, no licence question. ARMSX2's
`FrameGenPacer` is the only prior art and it paces generated frames only.

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

## 9. Audio

**Added 2026-08-23.** This catalogue listed audio only as a set of vendored
backends, which is why it was not a pipeline.

Every fork turns guest audio samples into host audio output.

| Android path | Forks |
| --- | --- |
| **Oboe** | **ARMSX2, eden, melonDS** |
| its own Android driver | xenia |
| SDL, reaching AAudio | Vita3K |
| cubeb | rpcsx, azahar, Cemu |

**Shared:** the host stream, buffer sizing, the latency policy, device change
and route change handling, and the mixer.

**Not shared:** guest sample format, guest channel layout, guest mixing
semantics and the guest's own audio timing, which is often tied to guest
interrupts.

**Contract:** the backend produces interleaved PCM at a declared rate and
channel count; the shared layer owns the stream and tells the backend how much
it needs and when.

**This pipeline is the fleet's one clear case of convergence.** Three forks
independently chose Oboe, which is Google's recommended low-latency path,
without coordinating. **Everywhere else in this catalogue, independent forks
diverged.** Standardise on Oboe: it also resolves the five vendored copies of
cubeb that block the packed binary.

**Nothing about latency is measured.** This records which library each fork
calls.

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

- Memory mapping is **still** barely surveyed.
- **Audio: closed 2026-08-23.** It is now pipeline 9.
- **Pipelines 3 to 7 are partly read now, not listed.** Read since this was
  written: the pipeline cache, the code cache, thread affinity, save
  conventions, frame pacing and the upscale algorithm sets. **Shader
  translation and memory mapping remain listed rather than read.**
- **GameThor: surveyed 2026-08-23**, and the difference is real. It translates
  Direct3D to Vulkan through DXVK and runs Windows binaries under Wine, so it
  has no guest CPU to recompile and no guest GPU to model. **Pipelines 1, 3 and
  4 do not apply to it at all.**

  **It is also the existence proof the xenia ledger cites** — RE2 Remake running
  on this same Thor through GameNative and DXVK — for the argument that
  translating an API beats emulating hardware here.

  **And it contributed a pattern this catalogue lacked:** per-game **host config
  fixes**, 29 of them behind six typed kinds, which change an environment
  variable or a launch argument and touch no guest state. That belongs in
  pipeline 8 and is now the fourth thing called a "patch".

## Two rules this catalogue earned on 2026-08-23

**Search the mechanism, not the category.** Looking for `*pipeline_cache*`
missed two forks; looking for `vkCreatePipelineCache` found all eight. The same
failure happened for storage and for hotkeys.

**Separate host-side from guest-side before counting.** An emulator implements
the guest's API **as a feature**, so searching for `sched_setaffinity` returned
the Wii U `coreinit` thread API, a 3DS kernel syscall and Orbis kernel
emulation. **Three of seven hits were the guest.**
