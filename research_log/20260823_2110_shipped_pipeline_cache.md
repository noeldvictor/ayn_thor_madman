# The pinned driver makes a pipeline cache shippable, and nobody in emulation does this

**Goal: find what the fleet is missing at the intersection of unification, Vulkan
and the Rosetta model.**

The answer is not a faster renderer. **It is that this project can ship a warm
Vulkan pipeline cache to its users, and no other emulator can.**

## Method

Read each fork's Vulkan device layer and count the device extensions it requests.
Normalise the macro form against the string form. Compare the fleet against the
device extension list xenia captured from the Thor. Then search for the mechanism
that turns a cache into a shippable artifact.

No device was used. Every number below comes from reading a fork.

## Finding 1: the fleet does not agree on what this GPU can do

Measured with `tools/vk_capability_census.py`, which reads one named device-layer
file per fork. **A fleet-wide grep does not work here**: it returns the vendored
Vulkan headers, which declare every extension that exists. ARMSX2 reads 482 that
way and its real answer is 35.

| Fork | Device extensions requested |
| --- | --- |
| eden | 42 |
| ARMSX2 | 35 |
| Vita3K | 35 |
| Cemu | 27 |
| xenia | 13 |
| azahar | 13 |
| melonDS | 9 |

**Union: 120. Requested by exactly one fork: 84. Requested by four or more
forks: 5** — and those five are `VK_KHR_swapchain`, `VK_KHR_surface`,
`VK_KHR_android_surface`, `VK_EXT_debug_utils` and
`VK_EXT_external_memory_host`. **Four of the five are how you get a window.**

**Seven emulators target one GPU, and 70% of what they ask for is asked for by
one fork alone.**

This inverts the audio result. Three forks chose Oboe independently, which this
repo treated as strong evidence. Here the forks agree on almost nothing.

### The tool was wrong twice first, and both corrections were large

**Recorded because the method matters more than the number.**

**A third spelling exists and missing it understated a fork four-fold.** Forks on
vulkan-hpp write `vk::KHRBufferDeviceAddressExtensionName` — no `VK_` prefix and
no underscores. **Vita3K read as 9 and is 35.** The union moved from 109 to 120.

**A literal control byte replaced a regex escape**, so the new pattern matched
nothing and the run looked unchanged rather than failing. **A silent no-op is the
dangerous failure**; the count simply did not move.

## Finding 2: the capability ceiling is real, and ARMSX2 wrote it down

**Negative result first, because it corrects a plan.** The obvious conclusion is
to delete the negotiation code, since one device needs no runtime capability
check. **Measured, that deletion is small**: availability tests and vendor
branches are **1% to 9%** of each device layer, from 14 lines in melonDS to 168
in ARMSX2. **Deleting them saves about 500 lines fleet-wide and no frames.**

**The value is the ceiling.** A backend's renderer can only use what its own
device layer negotiated, and that set is an accident of each fork's portability
history rather than a property of the Thor.

**This is not a theory. ARMSX2 states it in its own source**, in the frame
generation subsystem that `CLAUDE.md` calls possibly the highest-value feature in
the fleet. `pcsx2/GS/Renderers/Vulkan/FrameGen/LsfgShaders.cpp`:

> PORT: Eden reads `device.IsFloat16Supported()` and the `frame_gen_fp16` user
> setting here. Neither exists for us [...] **PCSX2's Vulkan backend never asks
> for `VK_KHR_shader_float16_int8` when it creates the logical device, so a
> shader module declaring the Float16 capability would be invalid usage on it**
> regardless of what the physical device reports. Loading the fp32 variant is
> therefore the only correct choice here; **restore Eden's two lines if PCSX2
> ever enables the feature.**

The three facts around it:

- **The Thor supports it.** xenia probed `shaderFloat16 = 1` on the device,
  2026-06-20.
- **eden uses it throughout** — `IsFloat16Supported()` gates FSR
  (`present/fsr.cpp`), the present filters (`present/filters.cpp`) and
  `support_float16` in `vk_pipeline_cache.cpp`.
- **ARMSX2 runs the ported frame generator at fp32**, because of its own device
  layer and nothing else.

**A shared device layer removes the blocker in one place.** The comment already
names the fix and its size: two lines.

**State the limit honestly.** Packing does not make a backend use a feature; that
is per-backend renderer work. What packing changes is the unit of that work: **a
capability enabled once in a shared device layer is available to seven backends.
A capability negotiated per fork reaches one.**

**This is a packing argument nobody has stated.** `CLAUDE.md` justifies the
packed binary with one device, one cache, one upload path, one thread pool, one
memory budget, link-time optimisation and one present path. **Those are all
resource arguments.** The capability argument is separate.

### The negatives, after a second search

**The first pass produced eight extensions with no user, and six of those were
wrong.** The device-layer file is not the whole fork. Re-searched across every
fork's own source, excluding vendored headers:

**FINAL, 2026-08-24.** `tools/vk_capability_census.py` now covers rpcsx and
carries the second search as a built-in column, so this table no longer depends
on remembering to do it. **Three of the fourteen are unused anywhere in the
fleet, not eight:**

| Unused by every fork | What it would buy |
| --- | --- |
| **`VK_EXT_load_store_op_none`** | skip redundant tile load and store traffic |
| **`VK_EXT_graphics_pipeline_library`** | **compile shader stages without knowing the full pipeline state** |
| **`VK_EXT_multi_draw`** | batch draw submission |

**The middle row is the one this log is about.** Precompiling a pipeline needs
render state; graphics pipeline library is the extension that removes that
requirement for the shader half. **Nobody in the fleet uses it, and the device
exposes it.**

**The original eight, and what the second search found:**

| Extension | After the second search |
| --- | --- |
| `VK_EXT_load_store_op_none` | **no user, confirmed twice** |
| `VK_EXT_graphics_pipeline_library` | **no user, confirmed twice** |
| `VK_EXT_descriptor_buffer` | no user; the hits were `VkDescriptorBufferInfo`, which is core Vulkan |
| `VK_KHR_dynamic_rendering_local_read` | **xenia wants it and is blocked** — its comment says its vendored Vulkan-Headers are 1.3 |
| `VK_KHR_synchronization2` | **Cemu uses it**, via `m_featureControl.deviceExtensions.synchronization2` |
| `VK_KHR_shader_float16_int8` | Vita3K requests it; ARMSX2 names it in the comment above |
| `VK_KHR_buffer_device_address` | Vita3K requests it |
| `VK_EXT_multi_draw` | the ARMSX2 hit was OpenGL ES `glMultiDrawElements` |

**Ninth time a negative claim in this repo was wrong, and the rule held again:
never record one without a second search using different words.**

**xenia's case is a dependency finding, not a capability finding.** It knows the
extension, wants it, and cannot reach it because of a vendored header version.
That belongs with the dependency-unification open decision.

## Finding 3: the mechanism exists twice in the fleet, independently

The obstacle to compiling a pipeline before play is that a pipeline needs render
state, and an emulator derives render state from the guest. **Two forks solved
it the same way and neither cites the other.**

**Cemu, `VulkanPipelineStableCache.cpp`, 447 lines.** Its own comment states what
it stores for each cached pipeline:

> - Active shaders (referenced by hash)
> - An almost-complete register state of the GPU (minus some ALU uniform
>   constants which aren't relevant)

It then **creates a placeholder renderpass** and compiles against it. Cemu splits
storage in two, `shaderCache/transferable` and `shaderCache/precompiled`, where
the generic file is described in the source as **hardware and version independent
shader information**.

**eden, `shader_environment.cpp`.** `SerializePipeline` writes a magic number, a
cache version and the guest environment per shader; `FileEnvironment` replays it
with no guest running; `CanBeSerialized()` marks the cases that cannot.

**Two independent solutions to one problem is the convergence signal this repo
already trusts**, and it is the same signal as the touch overlay API surviving
two divergences.

Three forks show a boot-time compile screen: Cemu, azahar and eden. **azahar and
eden share `DiskShaderCacheProgress.kt`**, which is the shared-ancestry effect
already recorded in `CLAUDE.md`. **ARMSX2 has neither the state-replay cache nor
the boot screen**, and it is the seed of the shared layer.

**Cemu's split is the same split this repo made independently** in
`app/shell/.../PipelineCache.kt` on 2026-08-23: the driver blob dies on a driver
swap and the translation cache survives it. **Cemu named the two halves and
shipped them years earlier.**

## Finding 4: the model ships at scale, on a device shaped like the Thor

A Vulkan pipeline cache is driver-specific, which normally makes it useless to
anyone else. **The Steam Deck removes that objection by fixing the hardware and
the driver**, which is what lets Valve distribute precompiled shaders. The
framing in the reporting is direct: sharing precompiled shaders is an old Steam
feature, and **the Deck benefits because all those users have the same
hardware.**

**The Thor has the same shape**: one device, one pinned Mesa driver.

**Valve's tool is Fossilize and it is MIT licensed.** It is a library and a
**Vulkan layer** that serialises samplers, descriptor set layouts, pipeline
layouts, render passes, shader modules and pipelines to an archive, and replays
them through a `StateCreatorInterface` on a target device.

Two properties matter here:

- **MIT combines with the GPL-3.0 app.** No licence question.
- **It is a layer, so it records a backend without modifying it.** That fits the
  standing rule in `CLAUDE.md` that a fork is not modified unless it is asked
  for by name.

## The caveat, recorded rather than explained away

**Cemu explicitly disables Valve's layer.** `src/main.cpp`:

```cpp
_putenvSafe("DISABLE_VK_LAYER_VALVE_steam_fossilize_1=1");
```

The fork that has the best pipeline cache in the fleet turns Fossilize off.
**The reason is not recorded and is not guessed at here.** Find the reason before
adopting Fossilize, because Cemu paid for that line.

## What this does not claim

- **It does not claim shaders can be extracted from a dump at install time.** A
  native game's pipelines are fixed; **an emulator's are derived from guest state
  and are discovered by playing.** That is why the Cemu community's caches came
  from playthroughs. Any install-time extraction claim is unsupported and is not
  made here.
- **It does not claim a frame win.** A warm cache removes stutter. Stutter is not
  average frame rate, and xenia's ledger already records incremental GPU levers
  as `DEAD` or `FLAT`.
- **Nothing here is measured on the device.** Cache size, replay time at install
  and the stutter reduction are all unmeasured. See `DEVICE_QUEUE.md`.

## Sources

- `xenia-thor/docs/research/20260620-adreno-turnip-feature-gap-audit.md`
- Cemu `VulkanPipelineStableCache.cpp`, `LatteShaderCache.cpp`, `main.cpp`
- eden `src/video_core/shader_environment.h` and `.cpp`
- <https://github.com/ValveSoftware/Fossilize>
- <https://www.phoronix.com/news/Steam-Vulkan-Shader-Pre-Cache>
- <https://steamcommunity.com/app/1675200/discussions/0/3385030647947838304/>
