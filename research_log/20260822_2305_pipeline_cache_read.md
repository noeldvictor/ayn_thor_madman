# The shader and pipeline cache: two layers, and only one is shareable

**Goal: read `OWNED.md` queue item 3, which was marked "not read".**

Session 2026-08-22 23:05. `CLAUDE.md` calls this "measurable, user-visible, no
renderer internals needed" and calls shader stutter a Thor-wide problem.

**Result: real eight-way duplication of a guest-agnostic mechanism, unlike the
LRU cache. The invalidation problem this project would have designed is already
solved by the Vulkan specification, and ARMSX2 implements it correctly.**

---

## The filename search was wrong, again

Searching for `*pipeline_cache*` and `*shader_cache*` returned **zero files for
melonDS and rpcsx**.

Searching for the mechanism — `vkCreatePipelineCache`, `VkPipelineCache` —
returned **all eight forks**, including melonDS's `VulkanDispatch.cpp` and
rpcsx's `VKPipelineCompiler.cpp`.

**Third time today the category name missed and the mechanism found it.** The
rule is now well paid for.

## All eight persist the blob

Searching for `vkGetPipelineCacheData`:

| Fork | Where |
| --- | --- |
| ARMSX2 | `VKShaderCache.cpp` |
| azahar | `vk_pipeline_cache.cpp` |
| Cemu | `VulkanRenderer.cpp` |
| eden | `vulkan_common/vulkan_wrapper.cpp` |
| Vita3K | `vulkan/pipeline_cache.cpp` |
| xenia | `gpu/vulkan/vulkan_pipeline_cache.cc` |
| melonDS | `VulkanDispatch.cpp`, `GPU3D_Vulkan.cpp` |
| rpcsx | `VKPipelineCompiler.cpp` |

**Eight for eight.** This is not the LRU cache situation: `VkPipelineCache`
persistence has essentially one correct shape, fixed by the API, with no guest
semantics anywhere in it.

---

## The important distinction: there are two caches, not one

Reading ARMSX2's `VKShaderCache.cpp` makes the split explicit. It maintains
**both** in one file:

**1. The driver pipeline cache blob.** Opaque, produced by
`vkGetPipelineCacheData`, consumed by `VkPipelineCacheCreateInfo`. **Driver
specific.**

**2. Its own shader translation cache.** `CacheIndexEntry` keyed on
`source_hash_low`, `source_hash_high`, `source_length` and `shader_type`. This
is guest shader source to SPIR-V. **Guest specific, and driver independent**,
because SPIR-V is portable.

| | Driver blob | Translation cache |
| --- | --- | --- |
| Contains | driver-internal compiled state | SPIR-V |
| Keyed by | device and driver identity | guest source hash |
| Survives a driver swap | **no** | **yes** |
| Shareable across backends | **yes** | **no** |

**That is the shared and not-shared line for this subsystem, and it falls out of
the data rather than from an argument.**

---

## The invalidation problem is already solved, by the specification

This project bundles a pinned Turnip and allows a per-game driver override, so
"what happens to the cache when the driver changes" looked like a design
question.

**It is not. Vulkan answers it.** `VkPhysicalDeviceProperties::pipelineCacheUUID`
changes when the driver's caches become incompatible, and the blob carries a
16-byte header with that UUID in it.

ARMSX2 validates all five fields:

```cpp
if (header.header_length  < sizeof(VK_PIPELINE_CACHE_HEADER))   return false;
if (header.header_version != VK_PIPELINE_CACHE_HEADER_VERSION_ONE) return false;
if (header.vendor_id      != props.vendorID)                     return false;
if (header.device_id      != props.deviceID)                     return false;
if (memcmp(header.uuid, props.pipelineCacheUUID, VK_UUID_SIZE))  return false;
```

**Take this function.** It is short, correct, guest-agnostic, and it removes a
design question rather than answering it.

**And note what it does not need:** no Turnip build string, no driver package
name, no custom versioning. Keying on the app's own idea of "which driver is
installed" would be worse, because it would not catch a driver that changed its
compiled format without changing its package name.

---

## A cost of sharing that nobody has considered

**One shared `VkPipelineCache` across a packed binary means one invalidation
event for every backend at once.**

A per-game driver override changes `pipelineCacheUUID`. With eight separate
caches, switching driver for one game costs that one game's warm cache. **With
one shared cache, it discards the warm cache for every backend**, and switching
back discards it again.

This is a real argument against the per-game driver override as specified, or
for keeping **one blob per driver UUID on disk** rather than one blob. The
second is cheap: the file name carries the UUID, and switching back finds the
old file still there.

**Recommend: key the cache file by `pipelineCacheUUID`, keep the last two.** It
costs disk and removes the penalty entirely.

---

## Verdict for `OWNED.md`

**Queue item 3 is confirmed as genuine duplication and should be split in two.**

- **Own the driver pipeline cache**: creation, load, the five-field validation,
  serialisation, and UUID-keyed files. Guest-agnostic, eight copies today.
- **Do not own the shader translation cache.** It is guest-specific in every
  fork, exactly as the texture cache hashing was.

---

## The xenia comparison, done

Read immediately after the above, because the licence rule prefers BSD when
quality is close. **The two forks chose different strategies and both are
correct.**

**xenia lets the driver validate.** From `vulkan_pipeline_cache.cc`:

> Optionally seed from a previously-saved on-disk blob to cut first-launch
> shader-compile stutter. **The driver validates the header/UUID, so a stale or
> wrong-driver blob is ignored rather than misused.** Keep the buffer alive
> until after `vkCreatePipelineCache` reads it.

**xenia is right about the specification.** Vulkan requires an implementation to
ignore initial data it cannot use. Passing a stale blob is safe, and ARMSX2's
five-field check is not needed for correctness.

**ARMSX2's check is still the one to take, for a different reason.**

With a pinned driver, a per-game driver override and a measurement discipline
that demands knowing *why* a run was slow, **"the cache was silently ignored" is
exactly the failure you need to see.** Under xenia's approach a stale cache
produces no signal: the game stutters through recompilation and nothing says
why. ARMSX2 prints `Pipeline cache failed validation: Incorrect UUID`.

**This is the same rule the contract already states for texture uploads** —
every decline reason is separate and specific, because "nothing got upscaled"
had half a dozen causes needing different fixes. **A silently discarded pipeline
cache is that same class of bug**, and on this device it presents as stutter,
which is the symptom the whole subsystem exists to remove.

**So: validate for diagnostics, then let the driver decide.** The two are not
alternatives.

### xenia did not separate the pipeline cache the way it separated the device

`vulkan_pipeline_cache.cc` is 2,875 lines, of which roughly 120 concern the blob.
The rest is guest pipeline state: `GetCurrentStateDescription`,
`WritePipelineRenderTargetDescription`, `GetVkBlendFactor`, `GetVkBlendOp`,
geometry shader keys, `TranslateAnalyzedShader`.

**So the device-layer separation that makes xenia the right source for
candidate 0 does not extend here.** Both forks fuse the blob into a larger,
guest-specific file, and the extractable part is around 120 lines in xenia and a
similar amount in ARMSX2.

**Licence stops mattering at that size.** The blob logic is
`vkCreatePipelineCache` with `pInitialData`, `vkGetPipelineCacheData`, and a
header layout **defined by the Vulkan specification**. `CLAUDE.md` already draws
this line: a file format and a field list are facts, not expression. **Write it
from the specification, using ARMSX2's validation as the checklist.**

### One more thing worth recording

xenia's persistence is behind `cvars::vulkan_persistent_pipeline_cache` and a
path cvar, and a comment two lines above the loading code still says "Disk
persistence + pre-warm is a follow-up step". **The comment is stale relative to
the code below it.** Do not read a fork's comments as its current state — the
same lesson as reading a fork's `docs/` instead of its code.
