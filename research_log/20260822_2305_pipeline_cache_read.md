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

Take the validation from ARMSX2, which is the most complete read so far. Check
xenia's `vulkan_pipeline_cache.cc` before choosing, because it is BSD and the
licence rule prefers it when quality is close — **that check is not done.**
