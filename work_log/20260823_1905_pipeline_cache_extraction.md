# The first extraction: the driver pipeline cache

**Goal: move the third phase-exit test from "not started" to "started", without
touching a fork and without the device.**

## Why not the touch overlay

**The queue is ordered by risk, and the overlay ranks first because it is safe,
not because it is valuable.** No guest semantics, both sources
GPL-2.0-or-later, eight method names agreed across twelve years of independent
drift — a genuinely easy extraction.

**But the Thor has physical buttons.** This repo's own Vita3K lesson already
says an overlay drawn permanently over a game on such a device is wrong, and
`OverlayPolicy.kt` exists to hide it when a controller is attached. **A virtual
gamepad is near dead weight on this hardware.**

**Low risk is not the same as high value.** Re-ranked for this device, the
pipeline cache is the better first extraction.

## Why the pipeline cache

**Genuine 8-for-8 duplication, verified by reading rather than by counting
names:**

| Fork | File |
| --- | --- |
| ARMSX2 | `VKShaderCache.cpp` |
| xenia | `vulkan_pipeline_cache.cc` |
| Cemu | `VulkanRenderer.cpp` |
| azahar | `vk_pipeline_cache.cpp` |
| melonDS | `GPU3D_Vulkan.cpp` |
| Vita3K | `pipeline_cache.cpp` |
| eden | `vulkan_wrapper.cpp` |
| rpcsx | `VKPipelineCompiler.cpp` |

- **No guest semantics.** The shape is fixed by the Vulkan specification.
- **Shader compile stutter is a Thor-wide problem**, and one cache warmed across
  every backend is the point of the packed binary.
- **Invalidation is the specification's job**, not ours: `pipelineCacheUUID`
  changes when a driver's compiled format changes.

**It owns the driver blob only.** The guest-shader-to-SPIR-V cache stays with the
backend — it is keyed on a guest source hash and is guest knowledge. **The two
have different lifetimes and that difference is the design.**

## The bug one fork already paid for

**ARMSX2's own comment, and it is the reason this extraction is worth doing:**

> the blob is only validated against the Vulkan device header
> (`vendorID`/`deviceID`/`pipelineCacheUUID`), **all of which are identical
> across an app update on the same phone** — so a `SHADER_CACHE_VERSION` bump
> used to wipe the shaders while keeping every pipeline built from the
> *previous* build's shaders. **Nothing prunes or size-caps that blob**, and
> `FlushPipelineCache()` re-serialises it in full on the GS thread every N new
> compiles, so **the dead entries turned into an ever-growing mid-gameplay stall
> that only a clean reinstall cleared** (users reported "clean install improved
> performance").

**Four failures in one paragraph**: the device header cannot see an app update;
nothing prunes; nothing caps; and the re-serialisation happens on the render
thread. **The symptom was a user-visible mystery.**

## What was built

**`app/shell` `PipelineCache.kt`, with 14 tests. 110 tests pass in total.**

| Rule | Source |
| --- | --- |
| Five device-header checks, **each with a distinct verdict** | ARMSX2 logs which field disagreed — the difference between "did not load" and a diagnosis |
| **An app epoch**, checked separately | the bug above. The device header cannot see an app update |
| **Order matters**: length, then version, then fields | a truncated file must not have its vendor id trusted |
| **One file per UUID**, keeping the last two | a per-game driver override must not discard every backend's warm cache at once |
| **A size cap, and discard rather than trim** | a blob is opaque; there is no supported way to remove one entry |
| **Drop the translation cache on an epoch change only** | SPIR-V survives a driver swap; it does not survive the app that generated it changing |

**`shared_layer/pipeline_cache.h`** declares the native side and **compiles
clean** under NDK 29 clang at C++20 for `aarch64-linux-android33`, with `-Wall
-Wextra`.

## State, stated honestly

**This is `policy owned, no fork converted`, and `OWNED.md` says so.**

| Exists | Does not |
| --- | --- |
| the policy, with 14 tests | **a native implementation** |
| the C++ contract, syntax-checked | **any fork converted** |
| the `OWNED.md` row | **the build guard** |

**A subsystem is either owned or not owned. This one is not.** The row exists so
the partial state is visible rather than assumed — the same reason the empty
list was written down in the first place.

**Converting a fork requires deleting that fork's copy**, which the standing rule
forbids without being asked for that fork by name. **That half waits.**

## What this does not claim

- **Nothing is measured.** No stutter number, no cache size, no load time.
- **The cap value is not chosen.** `overCap` takes one; what it should be on a
  handheld with eight backends sharing a budget is unexamined.
- **The epoch source is not decided.** melonDS's migration framework uses the
  app's own version code as its schema version and has no separate number to
  forget to bump. **That is probably the right answer here too, and it is not
  written down as a decision.**
