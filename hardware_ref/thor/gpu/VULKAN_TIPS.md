# Vulkan on the Adreno 740: practical rules

A working sheet for getting the most out of the Thor's GPU. Concrete rules,
with the reason attached, because a rule without a reason gets applied where it
does not belong.

Written 2026-08-22. Sources at the end. **Items marked MEASURED come from this
device. Everything else comes from documentation and is a hypothesis until
measured here.**

## The device

| | |
| --- | --- |
| GPU | Adreno 740, vendor `0x5143` |
| Board | `kalama`, Snapdragon 8 Gen 2 |
| Vulkan | instance 1.3.0, device 1.3.128 — MEASURED |
| Clocks | 680 MHz down to 124.8 MHz — MEASURED |
| Panels | 1080x1920 and 1080x1240, both 60 and 120 Hz — MEASURED |
| Driver | pinned Mesa Turnip. Stock is `com.qualcomm.qti.gpudrivers.kalama.api33` |
| Architecture | tile-based deferred renderer with GMEM |

## The one idea everything follows from

**Bandwidth is the budget. GMEM is how you avoid spending it.**

The GPU renders into a small on-chip tile. Work that stays in the tile is
nearly free. Work that leaves the tile costs a write to system memory and
usually a read back.

Every rule below is a way of keeping work on-chip, or of noticing when it left.

## Render passes

**A render pass is a performance instruction, not bookkeeping.**

| Do | Do not |
| --- | --- |
| Merge dependent work into one pass with subpasses | Emit one pass per logical stage |
| Read the previous subpass through an input attachment | Write out, then read back as a texture |
| Keep the pass count low and know why each one exists | Let pass count follow the guest's structure |

Subpasses let the whole chain run per tile, so intermediate results never
resolve to system memory. This is the single biggest lever on this
architecture.

**A deferred renderer that writes a G-buffer out and reads it back is paying
full bandwidth for every attachment. The same renderer expressed as subpasses
pays almost none.**

## Load and store operations

**These are free to get right and expensive to get wrong.**

| Attachment state | Use |
| --- | --- |
| You will overwrite every pixel | `LOAD_OP_DONT_CARE` |
| You need it cleared | `LOAD_OP_CLEAR` |
| You genuinely need last frame's contents | `LOAD_OP_LOAD` — and justify it |
| Nobody reads it after the pass | `STORE_OP_DONT_CARE` |

**`LOAD_OP_CLEAR` beats `LOAD_OP_LOAD`.** A clear is a tile operation. A load
is an external read of every tile.

Depth and stencil almost always want `DONT_CARE` on store. Storing a depth
buffer nobody reads is pure waste, and it is a common accident.

## Transient attachments cost no memory

An attachment that never leaves tile memory needs no backing allocation.

- Create it with `VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT`.
- Back it with `VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT`.

Reported savings reach roughly 50 MB in sample scenes. On a fixed-memory
handheld that is a real fraction of the budget.

**If the arena allocated memory for something that never left GMEM, that is a
bug.**

## FlexRender: the GPU can leave tiled mode without telling you

The Adreno chooses **mid-frame** between binning with GMEM and rendering
directly to system memory. It is not a mode you set once at startup.

**Consequence: never assume a pass ran tiled. Measure which mode it used.**

A render graph built on the assumption that data stayed in GMEM will be wrong
some of the time, and it will be wrong silently. Mode belongs in the frame
statistics.

## LRZ, low resolution Z

Low resolution Z rejects occluded fragments before the fragment shader runs.

| Do | Do not |
| --- | --- |
| Draw opaque geometry front to back | Draw in the order the guest happened to submit |
| Keep depth writes early | Interleave depth-writing and blended draws |
| Store depth when a later pass needs LRZ state | Assume LRZ survives everything |

Since the Adreno 650, LRZ state can survive across render passes if depth is
stored and reloaded, and reused between passes. The 740 has this.

**Things that break LRZ:** writing depth from the fragment shader, discard or
alpha-test in a depth-writing pass, and changing the depth compare direction
mid-pass. Treat each as a reportable event rather than a silent cost.

## Memory and textures

- **ASTC everywhere.** Uncompressed textures cost bandwidth on every sample.
- **One allocator, large device-local blocks, sub-allocated.** Vulkan has a
  hard limit on live allocations and many small ones fragment.
- **The budget is a constant.** The device RAM is known and there is no swap
  worth using. Declare categories up front: texture cache, pipeline cache,
  staging, render targets, guest mirrors.
- **Prefer `HOST_VISIBLE | DEVICE_LOCAL` for streaming uploads** where the
  driver exposes it; the Adreno is a unified memory part, so a staging copy is
  sometimes pure waste. **Verify on this device before relying on it.**

## Pipelines

- **Precompile everything.** A pipeline created during gameplay is a stutter a
  player sees.
- **The pipeline cache never has to invalidate here**, because the driver is
  pinned. That is not true for a portable app and it is a structural advantage.
  Key the cache by the pinned driver identity.
- **Prefer fewer, larger pipelines** over many small variants. Specialisation
  constants beat a combinatorial explosion of permutations.
- **Warm the cache across backends.** One shared cache serves every system.

## Descriptors

- Vulkan 1.3 gives dynamic and non-uniform texture indexing. **On this device
  they are available**, so do not carry the fallback path a portable engine
  needs.
- Prefer few large descriptor sets, updated rarely, over many small ones
  updated per draw.
- Push constants for small per-draw data.

## Barriers and synchronisation

- **Narrow barriers.** A full pipeline barrier flushes the tile and undoes the
  work of keeping data on-chip.
- Batch barriers rather than emitting one per resource.
- Prefer subpass dependencies over explicit barriers inside a pass. The driver
  can keep those in the tile.
- Timeline semaphores are available. Use them rather than fences plus manual
  bookkeeping.

## Present and pacing

- **Two swapchains, both known.** 1080x1920 and 1080x1240.
- **Both panels support 120 Hz and the device is currently capped to 60 by a
  user setting.** Check the cap before any pacing measurement, or you measure
  the setting.
- Do not redraw an idle Screen-2 every frame. Draw on change.
- The panels differ in peak luminance, 420 against 500 nits. Colour and
  brightness do not match automatically.

## Threading

The SoC is 1 + 4 + 3 with a single Cortex-X3.

Two findings already exist in this fleet and they point in opposite
directions:

- xenia found guest threads hard-pinned to the A510 cores while the X3 sat
  idle.
- rpcsx keeps the **full** core mask deliberately, because restricting the
  process to the big cores drags Java, audio and compiler threads onto the same
  cores as emulation work.

**Both are true.** Place command buffer recording, pipeline compilation,
texture upload and present deliberately. Do not pin bluntly and do not leave it
entirely to the scheduler.

## What to measure, and with what

| Signal | Why it matters |
| --- | --- |
| Resolves per frame | the most common way to lose frames here |
| GMEM residency and FlexRender mode | proves the pass stayed on-chip |
| LRZ breaks | each one is fragment work that should not have run |
| Vertex against fragment split | tells you which half to fix |
| Pipeline cache hit rate | a miss during gameplay is a visible stutter |
| Bandwidth | the actual budget |
| Watts and temperature | the target is roughly 5 W and 50 C |

Tools already in the fleet: `xenia-thor-adb-gpu-stage-split`,
`xenia-snapdragon-profiler-gpu-metrics`, `thor_gpu_perfetto.ps1`,
`bd_gmem_ab.sh`, `bd_lrz_census.sh`, `bd_vrs_capture.sh`.

**Read [`.claude/skills/thor-measure/SKILL.md`](../../../.claude/skills/thor-measure/SKILL.md)
before taking any number.** Power readings while charging are fiction, and
cross-run comparisons are confounded.

## The best reference is not a game

[Khronos Vulkan-Samples performance samples](https://docs.vulkan.org/samples/latest/samples/performance/README.html)
carry run-time toggles for these exact behaviours and a Stats system reading
hardware counters. Toggling subpasses on and off in one session shows the
bandwidth change directly.

A shipped game gives one data point you cannot vary. Use the samples for
mechanism, and a game for the honest real-workload ceiling.

## Anti-patterns, all of which an emulator does by default

**These are the emulator's problem, stated as GPU rules.**

| Anti-pattern | Why the guest does it |
| --- | --- |
| A render pass per render-target switch | eDRAM and immediate-mode GPUs made switches cheap |
| `LOAD_OP_LOAD` on everything | the guest might read the target back, so the emulator is conservative |
| Pipelines created mid-frame | the guest sets state per draw |
| Submission order that breaks LRZ | the guest's draw order was tuned for a different GPU |
| Full barriers between draws | the emulator cannot prove independence |

**Faithfulness is required at the pixel. It is not required at the pass
boundary.** Rewriting guest render structure into a tiler-friendly one is
allowed, and it is where the frames are.

## Sources

- [Adreno GPU on Mobile: Best Practices](https://docs.qualcomm.com/nav/home/mobile_best_practices.html?product=1601111740035277) — FlexRender, GMEM, subpasses
- [Tile Based Rendering best practices](https://github.khronos.org/Vulkan-Site/guide/latest/tile_based_rendering_best_practices.html)
- [Khronos Vulkan-Samples performance samples](https://docs.vulkan.org/samples/latest/samples/performance/README.html)
- [The subpasses sample](https://github.com/KhronosGroup/Vulkan-Samples/blob/main/samples/performance/subpasses/README.adoc)
- [Low-resolution-Z on Adreno GPUs](https://blogs.igalia.com/dpiliaiev/adreno-lrz/)
- [Turnip and tiled rendering](https://deepwiki.com/sailfishos-mirror/mesa/3.3.1-turnip-vulkan-driver-and-tiled-rendering)
- `xenia-thor/docs/research/20260517-142224-thor-vulkan-device-baseline.md` — the measured device baseline
