# Owned subsystems

**The ledger of what the shared layer owns. It is the input to the build
guard.**

**A subsystem is either owned or not owned. There is no partly owned.** A
half-converted subsystem is how duplication returns.

---

## Nothing is owned yet

**The owned list is empty on 2026-08-22.** That is the honest state, and this
file exists so the emptiness is visible rather than assumed.

Nothing can be owned before the toolchain is unified, because seven C++ runtimes
cannot share native code. See the standard row in `CLAUDE.md`.

**Do not add a row here as a plan.** A row means the extraction is done, the
fork copies are deleted and the guard is in place. Candidates live in the queue
below.

---

## The mechanism

When the shared layer takes a subsystem, the fork loses the ability to have one.

1. **Extract** into the shared layer.
2. **Delete** the fork's implementation. Do not leave it unused. Dead code is an
   invitation.
3. **Depend.** The fork links the shared module and implements the contract,
   nothing more.
4. **Guard.** The build fails if the subsystem reappears: a new file under a
   deleted path, or a symbol duplicating an owned one, is a build error, not a
   review comment.

An agent that tries to add a texture filter to a fork should find no directory,
no build target and a failing build. **That is cheaper than any review**, and it
is the point: a rule in a document does not stop an agent that never read it.

---

## The row format

Every owned subsystem records these. The first three feed the guard.

| Field | Why |
| --- | --- |
| **Subsystem** | the name used everywhere, in every fork |
| **Guard paths** | paths that must not reappear in a converted fork |
| **Guard symbols** | symbols that must not be redefined |
| **Covers** | precise enough to write the guard from |
| **Does not cover** | the backend's half. Prevents scope creep |
| **Licence** | the most restrictive source licence. See below |
| **Extracted from** | fork and commit, per source |
| **Converted** | fork, commit, and the paths deleted |
| **Not yet converted** | remaining forks |
| **Depth decision** | why this depth, recorded before extracting |
| **Evidence** | the research log that proved the duplication |

**Licence rule: the shared layer inherits the most restrictive licence among its
sources.** A module built from ARMSX2 code is GPL-3.0 and only GPL-3.0 backends
can link it. Prefer the least restrictive source when quality is close.

---

## The queue

Ordered by risk, not by value. **Each entry needs a read that proves the
duplication before it moves.**

| # | Candidate | State | Note |
| --- | --- | --- | --- |
| 0 | **Vulkan device layer** | **read, measured, scoped** | See below. Take xenia's; it is the only fork that already separated device from renderer, and it is BSD |
| 1 | **Touch overlay** | **read, proven** | azahar and Vita3K ship the same four classes from the same 2013 Dolphin ancestor. Eight method names survived twelve years of independent drift |
| 2 | **GPU driver manager** | **read** | Four concerns, not six copies. Folds into the device layer rather than standing alone |
| 3 | **Driver pipeline cache** | **read, confirmed** | Genuine 8-for-8 duplication of a guest-agnostic mechanism. See below. **The shader translation cache is NOT included** |
| 4 | Settings framework | **read** | azahar and eden: same design twice, **code fully diverged**. Extraction is a rewrite guided by two references |
| 5 | Cheat engine | **read** | Three architectures on one axis: flat poke, polymorphic, bytecode VM. Take the whole axis |
| 6 | Code patch engine | **read** | Cemu's symbolic assembler as the engine, xenia's TOML as the authoring format |
| 7 | Texture upload and per-class routing | partially read | The flagship feature. Safe only after the test harness |
| 8 | Code translation | not read | Last. The deepest reach into a core |

### Candidate 0 is scoped: take xenia's `ui/vulkan/`

Measured 2026-08-22. See
[`../research_log/20260822_2255_native_census_and_device_layer.md`](../research_log/20260822_2255_native_census_and_device_layer.md).

**The framing "seven forks each built a device layer" is true and misleading.**
Six of them built it *inside a renderer*. Only xenia built it as a module.

| Fork | Device creation lives in | Separated? |
| --- | --- | --- |
| **xenia** | **`ui/vulkan/`, its own directory** | **yes** |
| ARMSX2 | `GSDeviceVK.cpp` | no |
| Cemu | `VulkanRenderer.cpp`, 4,465 lines | no |
| Vita3K | `renderer/src/vulkan/renderer.cpp` | no |
| eden | `vulkan_common/vulkan_wrapper.cpp` | partly |
| melonDS | `VulkanContext.cpp` | partly |
| azahar | `renderer_vulkan/vk_platform.cpp` | partly |
| rpcsx | `vkutils/device.cpp`, `instance.cpp` | yes, but GPL-2.0-only |

**xenia's `src/xenia/ui/vulkan/` against `src/xenia/gpu/vulkan/` is exactly the
line this project draws between shared and not-shared, already expressed as a
directory.** Nobody has to be persuaded of the boundary.

**Size: 11,471 lines, or about 7,000 once `vulkan_immediate_drawer.cc` is
dropped** — the app is Compose and does not need Vulkan-drawn UI. Instance plus
device alone is about 2,400.

**Licence: BSD.** Per the rule above, this leaves the shared module usable by
anything, including a separately distributed GPL-2.0-only PS3 backend. Taking
the same code from ARMSX2 or eden would make it GPL-3.0 and close that door.

**So the work is not merging seven implementations.** It is taking one module,
then unpicking six renderers from their own device creation. **The second half
is per-fork and is the real cost.**

**Open, and it is the largest single file.** `vulkan_presenter.cc` is 3,879
lines and owns the swapchain. The unanswered question in
[`thor_backend.h`](thor_backend.h) — who owns the swapchains when a backend
presents two guest screens — lands exactly here, and xenia's answer covers one
swapchain only.

### Candidate 3 splits in two, and only half is shareable

Read 2026-08-22. See
[`../research_log/20260822_2305_pipeline_cache_read.md`](../research_log/20260822_2305_pipeline_cache_read.md).

**All eight forks call `vkGetPipelineCacheData`.** Unlike the LRU cache, this is
real duplication: `VkPipelineCache` persistence has one correct shape, fixed by
the API, with no guest semantics in it.

**But the forks keep two caches in the same place, and only one is shareable.**

| | Driver blob | Shader translation cache |
| --- | --- | --- |
| Contains | driver-internal compiled state | SPIR-V |
| Keyed by | device and driver identity | guest source hash |
| Survives a driver swap | **no** | **yes** |
| Shareable across backends | **yes** | **no** |

**Own the driver blob. Do not own the translation cache**, which is guest
specific in every fork, exactly as texture cache hashing was.

**The invalidation question is answered by the specification, not by us.**
`pipelineCacheUUID` changes when a driver's caches become incompatible, and the
blob header carries it. ARMSX2's `VKShaderCache.cpp` validates header length,
header version, vendor ID, device ID and UUID. **Take that function.** Do not
key on a Turnip build string: a driver can change its compiled format without
changing its package name.

**A cost of sharing that nobody had considered.** One shared cache across a
packed binary means **one invalidation event for every backend at once**. A
per-game driver override changes the UUID, so with eight separate caches it
costs one game's warm cache, and with one shared cache it discards every
backend's. **Fix: name the cache file by `pipelineCacheUUID` and keep the last
two.** Switching back then finds the old file intact.

**xenia compared, and the licence question dissolves.** xenia relies on the
driver to reject a stale blob, which is correct per the specification. But under
that approach a stale cache gives **no signal at all** — the game stutters
through recompilation and nothing says why. **Validate for diagnostics, then let
the driver decide.** The two are not alternatives, and this is the same rule the
contract already states for texture upload declines.

**Neither fork separated it.** xenia's `vulkan_pipeline_cache.cc` is 2,875 lines
of which about 120 concern the blob; the rest is guest pipeline state. So the
separation that makes xenia right for candidate 0 does not extend here.

**At ~120 lines, licence stops deciding.** The logic is `vkCreatePipelineCache`
with `pInitialData`, `vkGetPipelineCacheData`, and a header layout defined by the
Vulkan specification. A file format and a field list are facts, not expression.
**Write it from the spec, using ARMSX2's validation as the checklist.**

### Rejected candidates

**Recording a rejection matters as much as recording an extraction.** Both stop
the question being re-argued.

| Rejected | Why | Evidence |
| --- | --- | --- |
| **LRU cache** | Three different designs for three constraints, not one structure written three times | [`../research_log/20260822_1915_lru_cache_extraction_test.md`](../research_log/20260822_1915_lru_cache_extraction_test.md) |
| **Texture cache hashing** | Guest-specific. ARMSX2 and melonDS key on data plus palette; Cemu keys on physical address | `CLAUDE.md` |
| **A shared renderer** | xenia's ledger records incremental GPU levers as `DEAD` or `FLAT`. Build the layer **beneath** a renderer | `CLAUDE.md` |
| **Render pass structure** | Tiler-critical and unmeasured. Flattening it spills GMEM | [`THOR_RENDER.md`](THOR_RENDER.md) |

---

## Before you add a row

1. **Read every implementation.** A capability recorded from a file listing is a
   hypothesis. Four duplication claims in this repo shrank on reading.
2. **Search for a shared ancestor, not a shared feature.** Shared ancestry
   predicts duplication; shared purpose does not. See
   [`ANCESTRY.md`](ANCESTRY.md).
3. **Measure the drift.** Shared ancestry after enough years means shared
   *design*, not shared *code*, and that changes extraction into a rewrite.
4. **Pass the licence gate.**
5. **Record the depth decision and its reasons**, before extracting.
6. **Convert one fork and prove the contract.** Do not convert every fork at
   once.

The procedure is in [`../.claude/skills/extract-subsystem/`](../.claude/skills/extract-subsystem/).
