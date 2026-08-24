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
| Translated guest code | guest bytes + codegen options | **yes** | **rpcs3** (`ppu-<sha1>`, production), ARMSX2 (VU), xenia (off) |
| Guest shader to SPIR-V | guest shader + translation options | **yes** | Cemu, eden, azahar |
| Compiled pipeline blob | SPIR-V + render state + **driver** | **no** | all eight forks |
| AOT module patches | module build ID + patch options | **yes** | **eden NCE**, not persisted |
| **Upscaled textures** | source texels + filter + scale | **yes** | **melonDS, and its output is a standard pack** |

**CORRECTED 2026-08-24.** The first version of this table said upscaled textures
had no prior art and named only ARMSX2 for code. **Both were wrong**, found by
`tools/capability_probe.py`. **PERSIST is the fleet's existing practice in four
forks across three artifact classes**, and nothing connected them. See
[`../research_log/20260824_0055_persist_is_everywhere.md`](../research_log/20260824_0055_persist_is_everywhere.md).

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

- **rpcs3 keys its PPU cache directory on the module SHA-1**:
  `ppu-<base57(sha1)>-<name>/`. **Production scale, years of users.**
- ARMSX2 hashes the guest program and writes `<root>/<hh>/<hash>.vuprog`.
- eden keys NCE patches on the **32-byte NSO build ID**.
- **melonDS names filtered textures by content hash** —
  `tex1_<W>x<H>_<texhash16>_<palhash16>_<fmt>.png` — in a layout **shared with
  the desktop tooling**, so what one person's play session produces, another
  person can install.
- This repo's `GameKey`/`DumpId` split already made the same distinction for
  library metadata.

**2. An options sentinel: every option that changes the emitted artifact.**
Taken from ARMSX2's `mVUbuildOptionsSentinel`, a 64-byte fixed-layout snapshot
covering codegen switches, clamp modes, speedhacks, three FPCR masks, and **the
recording flag itself** — because recording changes the emitted forms.

**Three independent designs of this key exist and two of them put floating-point
behaviour in it.** rpcs3's `enum class ppu_settings` carries `accurate_dfma`,
`fixup_vnan`, `fixup_nj_denormals` and `accurate_fpcc`; ARMSX2 carries three FPCR
masks. **Two teams, two guests, one conclusion.** rpcs3 also carries
`arm64_codegen_v1`, so the ARM64 backend's codegen version is part of the key.

**Three rules travel with it:**

1. **Fixed layout with an asserted size.** ARMSX2 uses
   `static_assert(sizeof(Snapshot) == 64)`. Drift must fail where the layout
   changed, not on a user whose cache silently mismatches.
2. **A reserved tail**, so a new option does not shift the fields below it.
   **rpcs3 shows this in use**: its enum runs
   `thor_es_async_draw_barrier_v1` through `v8` and
   `thor_es_dispatch_provenance_v1` through `v6` — **every codegen change added a
   new bit rather than reusing one.**
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

## Warming the store: four rules xenia paid for on device

**Added 2026-08-24 from xenia's experiment ledger. This is the "replay time"
gate below, partly answered — and the answer is a user-visible failure, not a
number.**

**What happened.** xenia's LLVM AOT precompile ran at **85 functions per second**
on a worker thread. **Android fired an ANR at 18 seconds** — *"Waited 5001ms for
MotionEvent"* — **and the user force-closed the app mid-compile**, believing it
had hung.

> **A cache that warms slowly does not fail as a slow cache. It fails as a hang,
> and the platform offers the user a button to kill it.**

**Four rules follow, and they bind any warming path this store gets.**

1. **The UI thread must never block while the store warms.** xenia's overlay had
   correct logic and **still could not draw**, because the UI thread was blocked
   at least five seconds in the paint path. **Correct progress code behind a
   blocked painter is invisible.**
2. **Progress must be monotonic and cumulative across units.** xenia's native
   estimate **grew from 6,665 to 10,540 mid-module**, and both counters **reset
   per module**. A raw done-over-total bar then jumps backwards, **which reads as
   a hang even when nothing is wrong.** Report cumulative work with a per-unit
   bar that cannot regress.
3. **Say that the platform may accuse the app of hanging, and say to wait.**
   Android offers "Close app" unprompted. **The user needs to be told before the
   dialog appears, not after.**
4. **Honour the budget.** xenia's log said *"budget 1500ms"* and the pass ran
   about **60 seconds**, because a `drain_frontier` flag overrode it. **A budget
   that a flag can silently override is not a budget.**

**Two of these are architecture, not messaging.** xenia shipped the messaging and
**recorded the root cause as still open**: the UI thread must not block in the
paint path during precompile.

**And 85 functions per second is the first real throughput number for warming
anything in this fleet.** It is LLVM AOT on one guest, so it does not transfer as
a rate — **but it does say the order of magnitude is tens of seconds, not
milliseconds**, which is what makes rules 1 to 3 necessary rather than polite.

## Verify a hit. Never infer one from a full cache directory.

**xenia's AOT object cache held 111 MB on the device and was off for every launch
a person performs.**

Its enabling block was guarded on *no cvar bundle supplied*, and the launcher
**always** attaches one when a game starts from the app. **Headless runs got the
cache and filled it. Every real launch recompiled about 10,000 functions from
scratch** — the 60-second black screen, and the ANR recorded above.

> **A lever can be defaulted correctly, allowlisted correctly, and still never
> apply, because the code path that sets it is gated on a condition the real
> launch path does not meet.**

**Two rules for this store:**

1. **The acceptance test is a counted cold-compile of zero**, not a non-empty
   directory. xenia's verification was *"ZERO cold LLVM begin compiles, a full
   warm hit"*, and nothing weaker counts.
2. **Test on the launch path people use.** A store validated only by a headless
   harness will ship cold and the harness will not notice.

**And check the persisted config, not the compiled default.** A persisted value
outlives the process, the install and the app update — xenia lost **2.88%** to
three optimisations that were `defaultEnabled=true` in code and `false` on the
device.

## Open, and none of it is measured

- **Cache size per title.** A store too large to ship fails whatever the frame
  numbers say. `DEVICE_QUEUE` entries 15 and 17.
- **Replay time is partly answered above** — tens of seconds, not milliseconds —
  **and the failure mode is an ANR, not a slow start.** What is still unmeasured
  is replay time for *this* store's artifact kinds.
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
