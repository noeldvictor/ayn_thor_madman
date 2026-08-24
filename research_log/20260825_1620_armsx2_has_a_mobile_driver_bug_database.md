# ARMSX2 has a mobile GPU driver bug database: 29 defects, 15 workarounds, confidence-ranked

**Goal: the driver-baseline section plans a driver manager that verifies the pin,
exposes the override and warns when off baseline. Check what the fleet already
knows about the driver being pinned.**

**Seven of eight forks carry Turnip-specific code** — searched all eight for
`MESA_TURNIP|DRIVER_ID_MESA|turnip` over `*.cpp *.h *.kt *.java`, vendored trees
removed: **ARMSX2 21 files, xenia 17, eden 11, rpcsx 8, Vita3K 5, Cemu 4,
azahar 3, melonDS 0.**

**And ARMSX2 has gone a level past a workaround list.**

## `GSGPUProfile.h`: a driver profile, not a driver check

```cpp
struct MobileDriverProfile {
    static constexpr u32 DATABASE_VERSION = 1;
    MobileGpuApi api;  MobileGpuDriver driver;  MobileDriverVersion version;
    u64 bugs = 0;  u64 workarounds = 0;
    u32 matched_rule_count = 0;
    DriverProfileConfidence confidence = DriverProfileConfidence::Unknown;
    /// True when nothing in the table matched and the safe defaults are in force.
    bool conservative_fallback = true;
};
```

**29 named `DriverBug` values** — `BrokenPushDescriptors`,
`BrokenColorWriteMaskWithDepthTest`, `BrokenDynamicRendering`,
`BrokenImagelessFramebuffer`, `BrokenGraphicsPipelineLibrary`,
`SlowCachedReadbackMemory`, `BrokenVSync`,
`BrokenMultithreadedShaderCompilation`, and twenty-one more.

**15 named `DriverWorkaround` values** — `UseDescriptorSets`,
`EmulateColorWriteMask`, `PreferCoherentReadback`, `ForceFifoPresent`,
**`AlignSwapchainWidthTo32`**, `UseRenderTargetCopyForFeedback`, and the rest.

## Four design decisions worth taking wholesale

**1. Bugs and workarounds are SEPARATE bitfields, and the reason is stated:**

> *"Kept separate from `DriverBug` because the same mitigation answers several
> defects, and because **a workaround can be forced on for testing without
> claiming the device has the bug**."*

**The second half is the testing insight.** A single enum would make "try the
mitigation" indistinguishable from "assert the defect", and this project's
measurement discipline needs to do the first without the second.

**2. Naming describes the DEFECT, not the fix.** *"so one bug can drive several
workarounds and the table stays readable."*

**3. Confidence ranking, so a broad rule never overrides a precise one.**
`Unknown < Vendor < Model < Driver < DriverVersion`.

**4. `conservative_fallback` is an explicit field**, true when nothing matched.
**Unknown hardware is a stated state rather than an accident.**

## The lesson the header records, learned expensively

> *"The same Mali part behaves differently under Arm's proprietary driver than
> under Mesa PanVK, which is a lesson this tree learned the expensive way (the
> r44p1 `DEVICE_LOST` fix had to be gated on **driverID, not vendorID**, and the
> 8 Elite push-descriptor disable likewise). Recording it as driver + version +
> a bug set means the next device-specific quirk is a table entry rather than
> another bespoke branch."*

> **Gate on driverID, not vendorID.**

**And this project has a live instance of exactly that distinction**, in the same
fork: `m_broken_colormask_with_depth = IsDeviceAdreno() && !is_turnip`. **The
Adreno proprietary driver has a broken colour mask with depth test and Turnip
does not.** Same GPU, opposite behaviour. **Pinning Turnip does not only fix
performance; it removes at least one correctness workaround.**

## What this means for the pinned-driver plan

**Pinning one driver means exactly one profile row is live — and the per-game
driver override makes every other row live again.** `CLAUDE.md` already
established that the override needs a process restart; **it did not establish
that switching driver switches the whole bug and workaround set with it.**

**`conservative_fallback` is what the "off baseline" warning should be built
on.** Not a string comparison against a pinned build name — **a profile that did
not match, stated as such.**

**Two workarounds land in the subsystem this repo says has no incumbent.**
`ForceFifoPresent` and **`AlignSwapchainWidthTo32`** are present-path defects.
**`CLAUDE.md` records a 2026-08-23 frame-pacing survey** — its method and its
second search are stated there — **which concluded that subsystem has no
incumbent.** That conclusion is about pacing LIBRARIES and is not disturbed here.
**What is new is that a catalogue of present-path driver DEFECTS exists anyway,
in the fork that is the seed of the shared layer**, and the pacing work will
inherit it.

**And it composes with rpcsx's `GpuDriverAdvisor` rather than competing.** The
advisor answers *"is this driver package suitable for this GPU"* before install;
the profile answers *"what is broken in the driver that is running"* after.
**Two halves, two forks, neither citing the other.**

## Limits

- **The header and the enums were read. `GSGPUDriverProfile.cpp`, 559 lines of
  rules, was not** — so which Turnip versions carry which bugs is unread, and
  no claim is made about the pinned build.
- **The 29 and 15 are enum entries, not confirmed device defects.** An entry
  exists because somebody hit it on some mobile GPU.
- **`MobileGsTuning`** — per-profile cache sizing, `pooled_targets`,
  `target_age`, `pooled_textures`, `texture_age` — was seen and not
  investigated. **A driver profile carrying cache sizes is a claim that the
  right cache size depends on the driver**, which this repo has not considered.
- **Nothing measured.**

## Sources

- ARMSX2 `pcsx2/GS/Renderers/Common/GSGPUProfile.h:60-240`,
  `GSGPUDriverProfile.cpp`, `GS/Renderers/Vulkan/GSDeviceVK.cpp:3629,3845`
- eden `src/video_core/vulkan_common/vulkan_device.cpp:488,644`
