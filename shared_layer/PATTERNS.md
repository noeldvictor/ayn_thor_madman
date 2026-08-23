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

**Six recompilers, not four**, once dynarmic's two frontends are counted.

| Fork | Guest ISA | Implementation | IR |
| --- | --- | --- | --- |
| xenia-thor | PowerPC, Xenon | `src/xenia/cpu/backend/a64/`, `a64_code_cache` | **HIR**, 6,605 lines |
| Cemu-thor | PowerPC, Espresso | `Cafe/HW/Espresso/Recompiler/BackendAArch64/` | **IML**, 6,327 lines |
| eden-thor | **ARM64**, Switch | `src/dynarmic/` A64 frontend | **dynarmic IR**, 7,382 lines |
| azahar, Vita3K | ARM32 | `dynarmic` A32 frontend | **dynarmic IR**, shared |
| melonDS-android | ARM7 and ARM9 | `melonDS-android-lib/src/ARMJIT_A64/` | **none** — direct |
| ARMSX2 | MIPS, EE and IOP | `pcsx2/arm64/` | **none** — direct |

### MEASURED 2026-08-23: IR expansion, device-free, from emitter source

**Stage A of instruction inflation — guest instruction to IR operation —
counted statically across every IR-based recompiler in the fleet.**

| Frontend | Guest to host | Emitters | **Median** | Mean |
| --- | --- | --- | --- | --- |
| **Cemu IML** | PowerPC to ARM64 | 121 | **2.0** | 3.32 |
| **dynarmic A64** | **ARM64 to ARM64** | 304 | **4.0** | 5.24 |
| **xenia HIR** | PowerPC to ARM64 | 270 | **5.0** | 5.94 |
| **dynarmic A32** | ARM32 to ARM64 | 607 | **5.0** | 6.42 |

**The result that decides it: dynarmic expands 4x when the guest ISA IS the host
ISA.** ARM64 to ARM64 is the easiest translation problem that exists, and it
still costs four IR operations per guest instruction. **Meanwhile Cemu does
PowerPC to ARM64 — a genuinely different machine — in two.**

> **Expansion is a property of the IR's register model, not of the distance
> between guest and host.**

**The mechanism is one design choice, visible in a single opcode.**

**SSA over a context** — xenia's HIR and dynarmic's IR — makes every operand a
load and every result a store:

```cpp
Value* v = f.Add(f.LoadGPR(i.XO.RA), f.LoadGPR(i.XO.RB));   // xenia: 4 ops
f.StoreGPR(i.XO.RT, v);
```

**Virtual registers** — Cemu's IML — make the same instruction one:

```cpp
ppcImlGenContext->emitInst().make_r_r_r(PPCREC_IML_OP_ADD, regD, regA, regB);
```

`_GetRegGPR` returns a reference and emits nothing.

**And xenia already confirmed the consequence on hardware.** Its
`cpu_backend_llvm_context_residency` flag says the IR has **"~99 ctx memory ops
+ 1 alloca = NO register residency (the guest thread is memory-bound)"**. **Those
context memory ops are exactly the loads and stores counted above** — so a
static source count and a device measurement agree.

**Limits, and they matter.** This is **stage A only**: the optimiser and
register allocator collapse much of it, so final host inflation may differ. The
count is **static and unweighted**, counts every branch inside an emitter, and
**undercounts helpers**. Opcode sets differ between frontends. **Nothing here is
timed.** See
[`../research_log/20260823_1712_ir_expansion_measured.md`](../research_log/20260823_1712_ir_expansion_measured.md).

### The overlap, restated

**dynarmic is the largest genuine overlap in this pipeline and the catalogue
missed it.** **Three forks vendor it** — eden in-tree, Vita3K and azahar as
submodules — so **one IR already serves three backends and two guest ISAs.**

**That is the only place in the fleet where a code-translation component is
already shared**, and it was reached without coordination, exactly like Oboe and
the touch overlay.

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

### The swizzle surface is large and unvectorised — AUDITED 2026-08-23

**Console textures are stored swizzled** — Morton order, tiled, or a
vendor-specific interleave — and **unswizzling is bit deinterleaving.**

**Carryless multiply (`PMULL`) implements bit deinterleaving in one
instruction. No fork uses it.**

| Fork | Files matching swizzle / morton / tile / detile | **`PMULL` host-side uses** |
| --- | --- | --- |
| **xenia** | **71** | **0** |
| Cemu | 49 | 0 |
| Vita3K | 47 | 0 |
| azahar | 45 | 0 |
| ARMSX2 | 33 | 0 |

**This is the best unexploited hardware-repurposing candidate in the fleet**,
and it is the only one that would land in a **shared** hot path rather than in a
single backend's instruction lowering.

**Unproven, and the next step is to read rather than to build.** The counts are
file matches. **No fork's swizzle code has been opened** to see whether it is
scalar bit math, a lookup table, or already vectorised by hand. **If it is a
table, `PMULL` may lose.**

See [`../research_log/20260823_1755_hardware_instruction_repurposing.md`](../research_log/20260823_1755_hardware_instruction_repurposing.md).

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


### Read 2026-08-23: two forks have no such pipeline, and the rest split in two

**Not every guest has programmable shaders.**

| Approach | Forks |
| --- | --- |
| **Guest bytecode straight to SPIR-V** | **eden** (`shader_recompiler/backend/spirv/emit_spirv.cpp`), **xenia** (`gpu/spirv_shader_translator`), **Vita3K** (`shader/usse_translator.h`) |
| Guest to GLSL text, then `glslang` | azahar, Cemu, and Vita3K's second path |
| **No pipeline at all** | **ARMSX2, melonDS** |

**ARMSX2 and melonDS translate nothing** because the PS2's GS and the DS's 3D
core are **fixed-function**. They write their own host shaders instead. **So
shader translation is a declared, optional pipeline** — two of seven backends do
not have one, and a contract that assumed it would be wrong for both.

**This explains the vendored `glslang` count.** `CLAUDE.md` records glslang
vendored by four forks and treats it as a dependency to unify. **Three of those
need it at run time**, because their shader backend emits GLSL text and compiles
it. **Dropping glslang is not a packaging decision for them; it is a shader
backend rewrite.**

**Emitting SPIR-V directly is the better target for this device** — no text
round-trip and no runtime compiler — but it is not a change the shared layer can
make on a backend's behalf.

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

- **Memory mapping: surveyed 2026-08-23.** **`fastmem` is the shared concept**,
  named across eden (50 files), ARMSX2 (48), Vita3K (32) and melonDS (29).
  **xenia and Cemu use neither the term nor the technique.**

  Host-side mapping code — `MAP_FIXED`, `memfd_create`, `shm_open` — sits in
  ARMSX2 `Memory.cpp`, eden `common/host_memory.cpp`, xenia
  `base/memory_posix.cc` and melonDS `ARMJIT_Memory.cpp`.

  **Shared:** reserving a large host virtual range, the `MAP_FIXED` sub-mappings,
  the fault handler that catches a miss, and the Android constraints — address
  space limits and whether `memfd_create` is usable on this API level.

  **Not shared:** the guest address space layout, what each region means, and
  what the slow path does when the fast path misses.

  **A caution for whoever takes it.** The naive search for this pipeline returns
  **guest kernel emulation** — rpcsx's `kernel/orbis/sys_uipc_shm.cpp` is the
  PS4 kernel's shared memory, not the host's. Same trap as thread affinity.
- **Audio: closed 2026-08-23.** It is now pipeline 9.
- **Pipelines 3 to 7 are partly read now, not listed.** Read since this was
  written: the pipeline cache, the code cache, thread affinity, save
  conventions, frame pacing and the upscale algorithm sets. **Shader
  translation and memory mapping are now read too**, so every pipeline in this
  catalogue has been opened at least once.
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
