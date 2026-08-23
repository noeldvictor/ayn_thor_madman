# The artifact store

**One content-addressed store for everything the app derives from a game.**

**Status: specification. Nothing implements it.** The Kotlin in
`app/shell/.../BlockCacheKey.kt` and `PipelineCache.kt` is the policy half, with
tests. There is no native side and no fork is converted.

## The rule

> **If an artifact's output is a pure function of the guest content and the host
> configuration, it should be computed once for everybody — not once per launch
> per user.**

That is the **PERSIST** operation, the fifth in `CLAUDE.md`'s table. It is the
only one that moves computed results rather than source.

**It works here because this project pins the host configuration harder than
anything else in emulation**: one device, one GPU, one Turnip build, one ABI, one
toolchain. **Every pin was chosen for a different reason.**

## What goes in

| Artifact | Pure function of | Survives a driver swap | Prior art |
| --- | --- | --- | --- |
| Translated guest code | guest bytes + codegen options | **yes** | **ARMSX2**, VU only |
| Guest shader to SPIR-V | guest shader + translation options | **yes** | Cemu, eden, azahar |
| Compiled pipeline blob | SPIR-V + render state + **driver** | **no** | all eight forks |
| AOT module patches | module build ID + patch options | **yes** | **eden NCE**, not persisted |
| Upscaled textures | source texels + filter + scale | **yes** | nobody |

**The driver row is the odd one and it decides the layout.** A pipeline blob dies
when the driver changes; everything else survives. **Two tiers, not one**, which
is what Cemu already ships as `shaderCache/transferable` and
`shaderCache/precompiled`, and what `PipelineCache.kt` reached independently.

**What does not go in.** An artifact whose output depends on **live guest
state** is not a pure function of content and configuration. Save states,
framebuffers and anything sampled from a running guest are excluded by
definition.

## The key

**Two parts, and both are required.**

**1. A content hash of the guest input.** Never a runtime address. A guest
address is where something happened to live in one process; it cannot be part of
an identity that outlives that process. **The address becomes a relocation
input**, not part of the key.

- ARMSX2 hashes the guest program and writes `<root>/<hh>/<hash>.vuprog`.
- eden keys NCE patches on the **32-byte NSO build ID**.
- This repo's `GameKey`/`DumpId` split already made the same distinction for
  library metadata.

**2. An options sentinel: every option that changes the emitted artifact.**
Taken from ARMSX2's `mVUbuildOptionsSentinel`, a 64-byte fixed-layout snapshot
covering codegen switches, clamp modes, speedhacks, three FPCR masks, and **the
recording flag itself** — because recording changes the emitted forms.

**Three rules travel with it:**

1. **Fixed layout with an asserted size.** ARMSX2 uses
   `static_assert(sizeof(Snapshot) == 64)`. Drift must fail where the layout
   changed, not on a user whose cache silently mismatches.
2. **A reserved tail**, so a new option does not shift the fields below it.
3. **Reclaim a reserved byte only where zero means "feature off, old
   behaviour."** **This is the rule this project did not have.** It is the
   difference between shipping a feature and shipping a feature that costs every
   user a cold cache.

**Rule 3 has a test**, `enabling a new default-off option does not change the
sentinel`.

## Sharding, writing and failing

- **Shard by the first byte of the hash.** Not decoration: a flat directory of
  tens of thousands of files is slow to open on a phone filesystem.
- **Write tmp-then-rename**, so a killed process never leaves a half payload.
  ARMSX2 does this.
- **Every failure degrades to recompute, never to wrong output.** A corrupt
  payload, a missing payload and a stale index entry are three separate cases and
  ARMSX2 has a test for each.
- **A version mismatch evicts. It never merges.** ARMSX2 renames the stale
  directory to `<old>.stale.<ns>` and starts fresh.

## What the pins buy

**A pipeline blob is driver-specific, so shipping one requires a fixed driver.**
**No fork in this fleet ships or imports one**, checked by reading all seven
device layers and then searching again for a download or import path. **No claim
is made about emulators outside this fleet**, which have not been surveyed.
The Steam Deck is the exception, because every user has the same hardware — which
is why Valve can distribute precompiled shaders through Fossilize.

**The Thor is the same shape.** So the tier that normally cannot be shared
becomes shareable, **and it is the tier that costs the compile stall.**

**Cemu already pools the other tier.** `src/tools/ShaderCacheMerger.cpp` folds
another user's `shaderCache/transferable` into yours, deduplicated by key. **The
mechanism for pooling exists; the pinned driver extends what it can pool.**

**Fossilize is MIT and is a Vulkan layer**, so it records a backend without
modifying that backend's source — which the standing rule requires.

**The caveat is answered, 2026-08-23, and it becomes two rules.** Cemu disables
Valve's layer because **Steam's shader precaching conflicts with Cemu's async
shader compilation** — its own guide reports "graphics or models failing to
render". That is a **two-owner problem**: Cemu could not switch off a layer the
Steam client injected. **This app owns both the device layer and the compile
schedule**, so the conflict does not arise here.

**But two rules follow and they are binding:**

1. **Record from inside the device layer this project owns, not from a layer
   under a backend doing async compilation** — or disable async compilation for
   the capture run and record that it was disabled.
2. **The acceptance test is visual.** Cemu's failure was **silent visual
   corruption, not a crash and not a stall**, so a cache run judged on frame time
   alone would have passed while rendering a broken game.

See [`../research_log/20260823_2340_why_cemu_disables_fossilize.md`](../research_log/20260823_2340_why_cemu_disables_fossilize.md).

## Open, and none of it is measured

- **Cache size per title, and replay time to warm it.** A store too large to ship
  or too slow to replay fails whatever the frame numbers say. `DEVICE_QUEUE`
  entries 15 and 17.
- **Self-modifying guest code.** The standing risk for any persisted code cache.
  How ARMSX2 handles it was searched for and not found, so it is unread.
- **Trust.** A cache built by one person and run by another is executable content
  from a third party. **The pooling model needs an answer here and does not have
  one.** Cemu's merger sidesteps it because a transferable entry is guest shader
  data, not host code. **A translated-code payload is host code.**
- **What the app update story is.** ARMSX2's ABI-version handshake is the
  starting answer, and `PipelineCache.kt`'s app epoch is the other half.
- **Inter-title reuse.** Guest SDK code is identical across many titles on one
  console, and content addressing would hit it for free. **Unmeasured, and more
  likely on an EE-style cache than a VU one.**

## Sources

- ARMSX2 `pcsx2/arm64/microVU_ProgCache-arm64.h`, `microVU-arm64.cpp`,
  `tests/ctest/core/recompilers/mvu_progcache_disk_tests.cpp`
- Cemu `src/tools/ShaderCacheMerger.cpp`,
  `src/Cafe/HW/Latte/Renderer/Vulkan/VulkanPipelineStableCache.cpp`
- eden `src/core/arm/nce/patcher.h`, `src/video_core/shader_environment.cpp`
- [`../research_log/20260823_2205_translate_once_ship_it.md`](../research_log/20260823_2205_translate_once_ship_it.md)
- [`../research_log/20260823_2250_eden_nce_deletes_the_translator.md`](../research_log/20260823_2250_eden_nce_deletes_the_translator.md)
- [`../research_log/20260823_2110_shipped_pipeline_cache.md`](../research_log/20260823_2110_shipped_pipeline_cache.md)
