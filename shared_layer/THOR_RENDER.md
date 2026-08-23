# The Thor render architecture

**What a renderer looks like if it will only ever run on one Adreno 740, under
one pinned Turnip build, on one device.**

Written 2026-08-22. A specification, not an extraction plan.

## Why this exists

The fleet has seven Vulkan device layers. Every one was written to be
**portable**: desktop NVIDIA, AMD and Intel, several mobile vendors, several
drivers, several API versions.

**Extracting from them yields the union of seven sets of compromises.** That is
not the same thing as a fast renderer for this device, and it is not what this
project is for.

xenia already learned the general form of this lesson. Its experiment ledger
records incremental GPU levers as `DEAD` or `FLAT`, and concludes the win was
architectural rather than incremental. Merging seven portable device layers is
incremental by construction.

**This document states the target.** Extraction toward an unstated target
averages compromises. Extraction toward this one has something to aim at.

## The premise

Every assumption below is a fact about the only device this ships on. See
[Target hardware](../CLAUDE.md#target-hardware) and the measured baseline in
`xenia-thor/docs/research/20260517-142224-thor-vulkan-device-baseline.md`.

| Fact | Consequence |
| --- | --- |
| Adreno 740, vendor `0x5143`, board `kalama` | No vendor branching. No capability fallbacks. |
| Vulkan instance 1.3.0, device 1.3.128 | One API version. No extension probing at run time. |
| One pinned Turnip build | Driver bugs are known and fixed in place, not worked around generically. |
| Tile-based deferred renderer, GMEM | Render pass structure is the performance model, not a formality. |
| LRZ, reusable across passes since a650 | Depth is a planned resource, not a side effect. |
| No direct GMEM extensions until a840 | Do not design around them. |
| Fixed RAM, Thor Max | The memory budget is a constant, not a negotiation. |
| Two panels, 1080x1920 and 1080x1240 | Two swapchains, both known, both fixed. |
| 60 and 120 Hz, currently capped to 60 | Pacing targets are known values. |
| 1 Cortex-X3, 4 mid, 3 A510 | Submission threading is designed for one prime core. |

## The eight design commitments

### 1. No enumeration, no fallback, no branching

A portable renderer spends its startup enumerating devices, probing
extensions, and building fallback paths that never run here.

**Assert the device and refuse to start if it is not the expected one.** Fixed
queue family indices, fixed formats, fixed extension set. Every `if (supported)`
that will always be true on this hardware is dead weight in the source and a
branch in the binary.

A debug build may verify the assumptions. A shipping build states them.

### 2. The render graph is a GMEM residency plan

This is the central commitment and it is what a portable design cannot make.

A portable renderer emits render passes because Vulkan requires them. **Here
the pass structure *is* the performance model.** Work that stays in GMEM costs
nothing extra; work that resolves to system memory costs bandwidth, and
bandwidth is the budget on a handheld.

The render graph must:

- Track, per attachment, whether it is resident in GMEM or in system memory.
- Merge adjacent work into one pass when it can stay on-chip.
- Refuse to insert a store and load pair unless something demands it, and name
  what demanded it.
- Report resolves as a first-class statistic, because an unnecessary resolve is
  the most common way to lose frames on this part.

**A backend describes what it needs to draw. The graph decides the passes.**

**Read 2026-08-22: this takes nothing away, because no fork plans passes.**

eden keys its render pass cache on attachment formats and sample count alone,
then fills the rest in as constants: `LOAD_OP_LOAD`, `STORE_OP_STORE`,
`subpassCount = 1`, zero input attachments, no resolve attachments. Vita3K is
also a format-keyed lookup. Both are caches, not plans.

So the graph is **additive**, not a replacement for tuned structure. There is
no tuned structure. See
[`../research_log/20260822_2130_render_pass_construction.md`](../research_log/20260822_2130_render_pass_construction.md).

**Do the cheap version first.** Correct the load and store operations before
building any graph. `LOAD_OP_LOAD` on an attachment the pass fully overwrites
should be `DONT_CARE` or `CLEAR`; `STORE_OP_STORE` on an attachment nobody
reads should be `DONT_CARE`. That is one struct filled in differently, it is
measurable as bandwidth and resolve count, and it needs no shared layer.

If correcting the ops moves nothing, a graph that automates the same decision
will also move nothing. Learn that cheaply.

### 3. LRZ is a planned resource

Low resolution Z rejects occluded fragments before the fragment shader runs. On
a650 and later it survives across render passes when depth is stored and
reloaded, and its state can be reused.

**Plan depth across the frame rather than per pass.** Record which passes can
inherit LRZ state and which break it, and treat a break as a reportable event
rather than a silent cost.

### 4. One arena, a constant budget

The device has a fixed amount of memory and no swap worth using.

**One allocator, one budget owner, constants rather than negotiation.** Sub
allocate from large device-local blocks. Categories declared up front: texture
cache, shader and pipeline cache, staging, render targets, guest memory
mirrors.

When the budget is exhausted the owner evicts by policy and **records why**.
The ARMSX2 statistics design, which separates every decline reason, is the
model.

### 5. The pipeline cache never invalidates

A portable renderer must invalidate its pipeline cache whenever the driver
changes, because it cannot know what the user is running.

**The driver is pinned.** The cache is valid across runs, across sessions, and
across backends.

This is the single clearest structural win available, and it is only available
because the driver is mandated. See
[The driver baseline](../CLAUDE.md#the-driver-baseline-pinned-turnip).

**Two corrections from reading the fleet on 2026-08-22.**

**Do not invent the key. Vulkan supplies it.**
`VkPhysicalDeviceProperties::pipelineCacheUUID` changes when a driver's caches
become incompatible, and the blob carries a 16-byte header containing it.
ARMSX2's `VKShaderCache.cpp` validates header length, header version, vendor ID,
device ID and UUID. **Take that function.** Keying on a Turnip build string
would be worse, because a driver can change its compiled format without changing
its package name.

**"Rebuilds it once" is false while the per-game driver override exists.** A
per-game override changes the driver **per game launch**, not once — and with
**one** shared cache across a packed binary, a single game switching driver
discards the warm cache **for every backend**, then discards it again on the way
back.

**So: name the cache file by `pipelineCacheUUID` and keep the last two.** The
override then costs nothing on return, and the commitment holds as written.

**And validate even though the driver would ignore a stale blob anyway.** xenia
relies on the driver to reject it, which is correct per the specification but
produces **no signal**: the game stutters through recompilation and nothing says
why. On a device where stutter is the symptom this whole subsystem exists to
remove, a silent cache drop is the failure you most need to see.

### 6. Two swapchains, both first class

Both panels are known: 1080x1920 and 1080x1240, both 60 and 120 Hz capable,
different peak luminance.

**Screen-2 is not an afterthought and not a mirror.** It is a second present
target with its own swapchain, its own pacing decision and its own content.

An idle Screen-2 is not redrawn per frame. A game using both guest screens
presents to both. The cost of the second swapchain is **unmeasured** and must
be measured before the dual-screen layout is assumed free.

**"Its own pacing decision" has no prior art anywhere.** Surveyed 2026-08-23:
**no fork uses Swappy, and no fork uses `VK_GOOGLE_display_timing`.** Every one
selects a Vulkan present mode and stops. azahar and melonDS take the
`Choreographer` vsync signal without a pacing policy, and ARMSX2's
`FrameGenPacer` paces generated frames only.

**`FIFO` is vsync, not pacing.** A 20 ms frame at 60 Hz misses its vsync and
alternates 16.6, 33.3, 16.6 — judder more visible than a stable 30.

**Two panels make this harder, not easier**, and nobody has solved even the
one-panel case here. **Check the 60 Hz cap before measuring any of it**: both
panels do 120, and the device is currently capped by a user setting, so a
pacing run measures the setting otherwise.

**Who owns the swapchains when a backend presents two guest screens is still
open**, and it is the largest single question left in the device-layer
extraction — xenia's `vulkan_presenter.cc` is 3,879 lines and answers it for one
swapchain.

### 7. Submission is designed around one prime core

The SoC is 1 + 4 + 3, and there is exactly one Cortex-X3.

Two findings already exist in the fleet and they point in opposite directions:

- xenia found guest threads hard-pinned to the A510 cores while the X3 sat
  idle.
- rpcsx keeps the **full** core mask on purpose, because restricting the
  process to the big cores drags Java, audio and compiler threads onto the same
  cores as emulation work.

**Both are true.** Command buffer recording, pipeline compilation, texture
upload and present must be placed deliberately, not left to the scheduler and
not pinned bluntly. This is a design problem, not a flag.

### 8. Every stage is measurable by construction

The renderer emits its own evidence: pass count, resolve count, GMEM residency,
LRZ breaks, pipeline cache hit rate, allocation failures by reason, present
timing per panel.

**A renderer that cannot explain a regression will not survive an agent fleet
working on it.** This follows the existing rules in
[Tests are mandatory](../CLAUDE.md#tests-are-mandatory).

## The native reference: mine a good game, not the forks

**The reference point is not another emulator. It is a well-optimised native
game running on this Adreno 740.**

That gives three things the forks cannot:

1. **A ceiling.** Real numbers for frame time, pass count, resolves per frame,
   bandwidth and watts on this exact part.
2. **A technique source.** Mobile-first rendering has solved tiler optimisation
   thoroughly. The forks have not, because their guests predate tilers.
3. **A target frame budget.** 16.6 ms at 60 Hz, 8.3 ms at 120 Hz, at roughly
   5 W. Concrete, not aspirational.

### What a good native frame looks like here

| | Well-optimised native | A faithful emulator |
| --- | --- | --- |
| Render passes | one main pass, merged subpasses | one per guest render target switch |
| Resolves per frame | zero beyond the final present | one per target switch |
| Attachment ops | `DONT_CARE` load and store wherever possible | load and store everything, because the guest might read it |
| MSAA | resolved on-chip inside the pass | often a separate resolve |
| Draw order | front to back opaque, LRZ rejects | the guest's order, LRZ breaks |
| Textures | ASTC throughout | whatever the guest had, converted |
| Pipelines | all precompiled, none created in gameplay | created as the guest sets state, so stutter |
| Clocks | stable and sustained | burst and throttle |

### The gap is structural, not CPU

**The guest's render structure is tiler-hostile and the emulator reproduces it
faithfully.**

PS2, Xbox 360 and Wii U had eDRAM or immediate-mode GPUs. Switching render
targets, reading back and setting state per draw were cheap there. On a tiler
each of those is a GMEM resolve out to system memory and back.

So a faithful emulator inherits a rendering structure designed for the opposite
architecture. That is where the frames go, and no amount of CPU work recovers
them.

**This is the argument for commitment 2.** The render graph exists to rewrite
the guest's structure into a tiler-friendly one: merge passes, eliminate
resolves, reorder for LRZ. Faithfulness is required at the pixel; it is not
required at the pass boundary.

It is also why xenia's ledger points at translation rather than emulation. A
translation layer emits a **native** render structure instead of replaying a
guest one.

### The reference to use is not a game

Researched 2026-08-22. **Khronos Vulkan-Samples performance samples beat any
shipped game as the baseline.**

A game gives one data point you cannot vary. The samples give a toggle.

- They are built to demonstrate tiler behaviour, and the `subpasses` sample
  exists specifically to show GMEM bandwidth savings.
- They carry a Stats system reading hardware counters: GPU cycles, fragment
  jobs, memory bandwidth.
- Behaviour is switchable at run time, so subpasses on against off is one
  capture, not two builds.

Sources: [Performance samples](https://docs.vulkan.org/samples/latest/samples/performance/README.html),
[the subpasses sample](https://github.com/KhronosGroup/Vulkan-Samples/blob/main/samples/performance/subpasses/README.adoc),
[Tile Based Rendering best practices](https://github.khronos.org/Vulkan-Site/guide/latest/tile_based_rendering_best_practices.html).

A shipped game still has a use, as a **real-workload ceiling**. Use both: the
samples for mechanism, a game for the honest upper bound.

### Two Adreno facts that change the design

**FlexRender.** The Adreno switches **mid-frame** between binning with GMEM and
direct rendering to system memory. It is not a mode you set once.

Consequence: **the GPU can silently fall out of tiled rendering.** A render
graph that assumes it stays in GMEM is wrong. Which mode a pass actually ran in
must be measured, not assumed, and it belongs in the statistics of
commitment 8.

Source: [Adreno GPU on Mobile best practices](https://docs.qualcomm.com/nav/home/mobile_best_practices.html?product=1601111740035277).

**`VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT`.** Transient attachments that never
leave tile memory need no backing allocation. Reported savings reach roughly
50 MB in sample scenes.

Consequence for commitment 4: **a transient attachment is a declared category,
not an allocation.** If the arena allocates memory for something that never
leaves GMEM, it is wasting a fixed budget.

**`LOAD_OP_CLEAR` beats `LOAD_OP_LOAD` on a tiler**, because a clear is a tile
operation while a load is an external read. An attachment loaded when it could
have been cleared is a per-tile bandwidth cost, and it is a checkable rule.

### The experiment this implies

**Measure the reference before optimising anything.** It needs no emulator
work.

1. Run the Khronos Vulkan-Samples performance samples on the Thor, with the
   subpass and load-op toggles, and record the delta each toggle produces.
   Then run a well-optimised native game for the real-workload ceiling.
2. Capture with Perfetto and Snapdragon Profiler. xenia already has skills for
   both: `xenia-thor-gpu-profile`, `xenia-snapdragon-profiler-gpu-metrics`,
   `xenia-thor-adb-gpu-stage-split`.
3. Record pass count, resolves per frame, GMEM residency, the vertex against
   fragment split, bandwidth, watts and sustained clock.
4. Run the same capture against one backend on a comparable scene.

**The difference between those two captures is the actual roadmap.** Everything
in this document is a guess until that pair of numbers exists.

Record the result in the experiment ledger, and gate power readings on
`status=Discharging`. See
[the measurement rules](../CLAUDE.md#the-device).

## What this is not

- **Not a shared renderer.** Draw translation, guest format conversion and
  pipeline state derived from guest state stay in the backend. See
  [`PATTERNS.md`](PATTERNS.md), pipeline 2 and 3.
- **Not a replacement for reading the forks.** The forks own the guest side and
  they know things this document does not.
- **Not a licence to start writing it now.** See below.

## The risk, stated plainly

**A renderer designed with no consumer comes out wrong.** The backend contract
avoided that by falling out of real screens rather than from an argument. The
same discipline applies here and it is harder, because the consumer is a guest
GPU model rather than a settings list.

Two guards:

1. **Bring it up against one backend, on one real workload, measured.** Then a
   second. Never design for seven.
2. **Every commitment above is a hypothesis until a number supports it.**
   Especially commitment 2, the render graph, which takes something backends
   own today.

## The honest ceiling

xenia's experiment ledger records a standing conclusion: the large win on this
device was **translating the guest API rather than emulating the guest GPU**,
proven by a Windows game running under GameNative and DXVK on the same Thor.

**This document does not contradict that. It is beneath it.** A translation
layer still needs a device layer, a memory budget, a pipeline cache and a
present path. Everything here serves either approach.

But do not expect this specification alone to deliver a large frame win on a
backend that is architecturally GPU-bound. Set that expectation now. See
[Expect maintenance wins from the shared layer, not large frame wins](../CLAUDE.md#expect-maintenance-wins-from-the-shared-layer-not-large-frame-wins).

## First steps

1. Read the xenia Thor Vulkan device baseline. The capabilities are already
   measured.
2. Read one fork's device layer in full, to learn what a real backend asks of
   it. melonDS `VulkanContext` is the smallest.
3. Write the device and arena, commitments 1, 4 and 5. They are the least
   speculative and the pipeline cache win is structural.
4. Measure the pipeline cache hit rate across two sessions before writing
   anything else.
