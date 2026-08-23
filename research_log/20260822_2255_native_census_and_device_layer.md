# Native census, and the size of the device-layer extraction

**Goal: measure the native side the way the frontends were measured, and put a
number on extraction candidate 0.**

Session 2026-08-22 22:55. `OWNED.md` ranks the shared Vulkan device layer first
and calls it "read, proven", without anybody measuring how large it is.

**Result: the extraction is far smaller than "seven forks each built one"
implies, and one fork has already done the hard part.**

---

## Native core size

Own code only. Vendored trees, build output and generated shader bytecode
excluded — see the traps below.

| System | Fork | Lines | Files |
| --- | --- | --- | --- |
| Switch | eden | 645,359 | 3,006 |
| PS3 | rpcsx | 621,097 | 1,497 |
| PS2 | ARMSX2 | 395,883 | 813 |
| Xbox 360 | xenia | 362,019 | 909 |
| DS | melonDS | 334,719 | 519 |
| Wii U | Cemu | 291,183 | 825 |
| 3DS | azahar | 278,525 | 1,263 |
| Vita | Vita3K | 214,233 | 834 |

**About 3.14 million lines of native code across the fleet**, against roughly
310,000 lines of Android frontend.

### Three counting traps, all hit on the first pass

1. **Cemu appeared to be 7.2 million lines.** `src/android/` holds a vcpkg
   build tree. Real figure is 291,183.
2. **xenia appeared to be 1.33 million.** `src/xenia/gpu/shaders/bytecode/`
   holds **generated SPIR-V headers at up to 20,000 lines each**. Real figure
   is 362,019.
3. **melonDS's "Vulkan layer" appeared to be 123,260 lines.** About 32,000 of
   that is generated shader data in `*ShaderData.h`, and `GPU3D_Vulkan.cpp` at
   21,273 lines is the **DS rasterizer**, which is guest-specific renderer, not
   device layer.

**A line count is a hypothesis until you look at the biggest files.** Same rule
as the capability inventory, applied to sizes.

---

## The device layer: who creates an instance and a device

Searched for `vkCreateInstance`, `vkCreateDevice` and their C++ wrapper forms.

| Fork | Lines | Files | Where it lives |
| --- | --- | --- | --- |
| ARMSX2 | 9,215 | 4 | **`GSDeviceVK.cpp`**, device and renderer fused |
| rpcsx | 5,718 | 8 | `vkutils/device.cpp`, `vkutils/instance.cpp`, `gpu/Device.cpp` |
| Cemu | 4,465 | 1 | **`VulkanRenderer.cpp`**, device and renderer fused |
| eden | 2,672 | 2 | `vulkan_common/vulkan_wrapper.cpp` |
| Vita3K | 2,399 | 2 | **`renderer/src/vulkan/renderer.cpp`**, fused |
| **xenia** | **2,366** | **4** | **`ui/vulkan/vulkan_instance.cc`, `vulkan_device.cc`, `vulkan_provider.cc`** |
| melonDS | 2,123 | 3 | `VulkanContext.cpp`, `VulkanDispatch.cpp` |
| azahar | 1,313 | 2 | `renderer_vulkan/vk_platform.cpp` |

**A false positive worth recording:** Cemu's first pass returned twelve files,
most of them `boost/regex/.../icu.hpp` inside vcpkg build output, because ICU
has a `createInstance`. **Filter build directories before grepping for a
generic method name.**

---

## The finding: xenia already separated the device layer, and nobody else did

This is the part that changes the extraction.

**xenia splits `src/xenia/ui/vulkan/` from `src/xenia/gpu/vulkan/`.** The first
is device, presentation and pooling. The second is the guest renderer. **That
boundary is the exact line `CLAUDE.md` draws between shared and not-shared, and
it already exists as a directory.**

`ui/vulkan/`, 11,471 lines:

| Lines | File | What it is |
| --- | --- | --- |
| 3,879 | `vulkan_presenter.cc` | swapchain and present |
| 1,274 | `vulkan_device.cc` | logical device, queues |
| 1,097 | `vulkan_immediate_drawer.cc` | UI drawing. **Not needed; the app is Compose** |
| 813 | `vulkan_instance.cc` | instance, layers, extensions |
| 610 + 443 | presenter and device headers | |
| 415 | `linked_type_descriptor_set_allocator.cc` | descriptor pooling |
| 238 | `vulkan_gpu_completion_timeline.cc` | timeline semaphores |
| 238 + 186 | `vulkan_util.cc/.h` | |
| 220 | `vulkan_upload_buffer_pool.cc` | staging and upload |
| 182 | `vulkan_dynamic_buffer_ring.cc` | ring buffer |

**Every other fork fuses device creation into its renderer.** Cemu's
`VulkanRenderer.cpp` is 4,465 lines doing both. ARMSX2's `GSDeviceVK.cpp` is the
same shape. Vita3K creates its device inside `renderer.cpp`.

**So the extraction is not "merge seven implementations". It is "take xenia's
module, then unpick six renderers from their device creation".** The second half
is the work, and it is per-fork rather than shared.

### Three reasons this points at xenia specifically

1. **The separation exists.** Nobody else has to be persuaded of the boundary;
   xenia's directory layout already asserts it.
2. **It is BSD.** `OWNED.md`'s licence rule says the shared layer inherits the
   most restrictive source. Extracting from xenia leaves the module usable by
   anything, including a separately distributed GPL-2.0-only PS3 backend.
   Extracting from ARMSX2 or eden would make it GPL-3.0.
3. **It is about 7,000 lines** once the immediate drawer is dropped, and roughly
   2,400 for instance and device alone. **That is a week, not a quarter.**

### What this does not settle

**`vulkan_presenter.cc` is 3,879 lines and is the largest piece.** It is also
where the unanswered question in
[`thor_backend.h`](../shared_layer/thor_backend.h) lands: **who owns the
swapchains when a backend presents two guest screens.** Taking xenia's presenter
means taking its answer for one swapchain and extending it, and that extension
is unwritten.

---

## Incidental: two forks carry libretro Vulkan glue

`ARMSX2/pcsx2/GS/Renderers/Vulkan/VKLibretro.cpp` and
`azahar/src/citra_libretro/libretro_vk.cpp`.

**libretro is rejected by this project**, so both are dead weight in a packed
binary. Small, but they are exactly the kind of thing that survives an
extraction because nobody asked what it was for.

---

## Method note

**Every headline number in this log was wrong on the first pass**, in three
different ways: a vendored build tree, generated bytecode, and a generic method
name matching an unrelated library.

The rule that catches all three: **after counting, list the largest files and
read their names.** It took one command each time and reversed the conclusion
twice.
