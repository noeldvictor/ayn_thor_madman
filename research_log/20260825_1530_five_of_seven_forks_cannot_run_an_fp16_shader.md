# Five of seven forks cannot run an fp16 shader, and the one that can does it for FSR

**Goal: take the capability-ceiling argument from an example to a count.**

`CLAUDE.md` records one instance: **ARMSX2's frame generation runs fp32 because
PCSX2's device layer never asks for `VK_KHR_shader_float16_int8`**, so a Float16
shader module *"would be invalid usage on it regardless of what the physical
device reports"*. **The Thor probes `shaderFloat16 = 1`.**

**That instance generalises to five forks, and the exception is instructive.**

## The count

**Two probes are needed, because the extension was PROMOTED.**
`VK_KHR_shader_float16_int8` is core in **Vulkan 1.2**, and the Thor's device API
is **1.3.128**, so a fork may enable it through
`VkPhysicalDeviceShaderFloat16Int8Features` without ever naming the extension.
**Searching only for the extension string would report every fork as lacking
it.**

**And a first pass got this wrong in the other direction.** Counting
`PhysicalDeviceShaderFloat16Int8Features|shaderFloat16` across ARMSX2 returned
**12 hits** — all of them in **vendored Vulkan headers**. Filtering
`vulkan_core`, `volk`, `vulkan.hpp` and the `vulkan_structs`/`_enums`/`_handles`
family leaves the fork's own code:

| Fork | fp16 in its OWN device layer |
| --- | --- |
| ARMSX2 | **none** |
| xenia | **none** |
| Cemu | **none** |
| azahar | **none** |
| melonDS | **none** |
| **Vita3K** | **requests the extension AND checks the feature bit** |
| **eden** | **queries `shader_float16_int8.shaderFloat16`**, and can force it off |

> **Five of seven backends cannot legally execute a Float16 shader module on
> this device, whatever the hardware reports.** The claim recorded for ARMSX2
> alone is a fleet property.

## Vita3K's device layer is the design to take

```cpp
// needed for FSR
{ vk::KHRShaderFloat16Int8ExtensionName, &support_fsr },
```

**Its extension list is a table of `{extension, &capability_flag}` with a comment
saying what each one is FOR** — `support_image_format_specifier`,
`support_buffer_device_address`, `support_standard_layout`,
`support_shader_interlock`. **That is a declarative device layer rather than an
ad-hoc list**, and it makes the ceiling question answerable by reading one table.

**And it validates in two stages, gating a NAMED FEATURE rather than a
capability flag:**

```cpp
support_fsr &= static_cast<bool>(physical_device_features.shaderInt16);
if (support_fsr) {
    // double check for FP16 support
    auto props = physical_device.getFeatures2KHR<..., vk::PhysicalDeviceShaderFloat16Int8Features>();
    support_fsr = static_cast<bool>(props.get<...>().shaderFloat16);
}
```

> **Requesting an extension and having its feature are different things.** The
> extension being in the list does not make `shaderFloat16` true, and a device
> layer that stops at the extension name has not established the capability.

**eden checks the feature bit too** — `features.shader_float16_int8.shaderFloat16`
— so the two forks that handle fp16 both do this correctly. **What is Vita3K's
alone, of the two read, is binding the result to a NAMED FEATURE**
(`support_fsr`) rather than to a generic capability flag, and requiring **two**
features for it: `shaderInt16` first, then `shaderFloat16`. **The consumer, not
the extension, is what the code tracks.**

**That is a third layer under the capability-ceiling argument**, below "does the
fork ask" and "does the device support": **does the fork check the feature bit
after asking.**

**FSR needs `shaderInt16` AND `shaderFloat16`.** This project wants upscaling as
a flagship feature; **the device requirement for one well-known upscaler is
already discovered, written down and guarded inside the fleet.**

**One more rule from the same table, stated in a comment:** rasterized order
attachment access and fragment shader interlock are mutually exclusive, and
*"although both should never be supported at the same time, rasterized order
access is far better than shader interlock"* — **so enabling one explicitly
disables the other.** A shared device layer will need that precedence, and
nobody else records it.

## What this does and does not say

- **It does not say five forks would go faster with fp16.** It says they cannot
  use it at all, so the question has never been askable for them. **ARMSX2's own
  comment names the cost for its case and nobody has measured any of them.**
- **The fix is not per-fork.** That is the packing argument: **one shared device
  layer enables the Thor's real feature set once and five backends rise above
  their current ceiling together.** ARMSX2's comment sizes its own half at two
  lines.
- **eden's handling includes forcing `shaderFloat16 = false`** in some path at
  `vulkan_device.cpp:551`. **Not read** — it may be a driver workaround, which
  would matter.

## Limits

- **Extension-list membership was read, not the created device.** A fork could
  enable the feature through a `pNext` chain this search does not name.
- **Two probes were used** — the extension name and the feature struct. **A
  third spelling would be missed**, and this repo has been caught by exactly
  that twice today.
- **rpcsx and GameThor were not included**; PS3 is out of the binary and
  GameThor's renderer is DXVK's.
- **Nothing measured. No device.**

## Sources

- Vita3K `vita3k/renderer/src/vulkan/renderer.cpp:641-642, 734-740, 742-746`
- eden `src/video_core/vulkan_common/vulkan_device.cpp:551, 579`
- ARMSX2 `pcsx2/GS/Renderers/Vulkan/FrameGen/LsfgShaders.cpp:23-24`
