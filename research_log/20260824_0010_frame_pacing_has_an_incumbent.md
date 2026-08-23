# Frame pacing has an incumbent after all, and it answers the two-screen question

**Goal: confirm that frame pacing is the one host subsystem with no incumbent,
before owning it.**

**It is not. Cemu has a four-part frame pacing subsystem in production, on
Android, and one of its parts answers an open question in
`shared_layer/thor_backend.h`.**

**Eleventh time an absolute negative in this repo was wrong.**

## What `CLAUDE.md` claimed

> **No fork uses Swappy. No fork uses `VK_GOOGLE_display_timing`.** Every fork
> picks a Vulkan present mode and stops. [...] it is the cheapest subsystem in
> the queue to own: nothing to extract, nothing to reconcile, no licence
> question.

**The first two sentences hold. The third is wrong, and it is the one the plan
rests on.**

## Method

Searched all seven forks for `Swappy`, `GOOGLE_display_timing`, `present_wait`
and `present_id` across source, Kotlin, Java, Gradle and CMake, excluding
vendored trees, then **read every hit**. The previous survey searched for the two
Google libraries only, so a mechanism spelled in Vulkan core terms was invisible
to it.

## What Cemu actually has

**Four parts, and the fourth was not looked for at all.**

### 1. Present mode selection, four vsync modes

`SwapchainInfoVk::ChoosePresentMode` — `MAILBOX`, `Immediate`,
`SYNC_AND_LIMIT`, and a default of `FIFO`. Each falls back with a log line when
the mode is unavailable.

### 2. Queue-depth limiting through `VK_KHR_present_id` and `VK_KHR_present_wait`

`VulkanRenderer.cpp:3120`. Each present carries a `presentId`. When the queue is
full it waits for the present `queueDepth` frames back to have actually reached
the display:

```cpp
if (chainInfo.m_queueDepth >= chainInfo.m_maxQueued) {
    uint64 waitFrameId = chainInfo.m_presentId - chainInfo.m_queueDepth;
    vkWaitForPresentKHR(m_logicalDevice, chainInfo.m_swapchain, waitFrameId, 40'000'000);
    chainInfo.m_queueDepth--;
}
```

**This is latency control, not vsync.** It bounds how many frames may be in
flight, which is what stops the render-ahead latency that makes a game feel
laggy on a handheld with real buttons.

**And it is applied exactly where it is needed.** `m_maxQueued` is **0** for
`MAILBOX`, `Immediate` and `SYNC_AND_LIMIT`, and **1** only on the default
`FIFO` path — **FIFO is the mode that would otherwise let frames queue.** One
frame in flight is the minimum-latency choice.

**It negotiates the feature properly** and logs the outcome:
`"Vulkan: present_wait extension: {supported|unsupported}"`.

### 3. Host-driven vsync

`SYNC_AND_LIMIT` calls `LatteTiming_EnableHostDrivenVSync()`, which starts a
dedicated thread — `VsyncDriver_startThread(LatteTiming_NotifyHostVSync)` — so
**the guest's vsync is driven by the host display's real vsync** rather than by a
free-running guest timer.

**That is what frame pacing means for an emulator**, and it is the part a survey
looking for Swappy could never find, because it is guest-timing code rather than
present code.

### 4. Dual-screen present serialisation, on Android

`VulkanRenderer.cpp:3098`, comment verbatim:

> **Keep TV and GamePad swapchains from forcing each other to idle. A single
> shared previous-frame marker serializes dual-screen presents on Android.**

```cpp
const size_t previousFrameIndex = mainWindow ? 0 : 1;
WaitCommandBufferFinished(m_commandBufferIDOfPrevFrame[previousFrameIndex]);
m_commandBufferIDOfPrevFrame[previousFrameIndex] = currentFrameCmdBufferID;
```

**`shared_layer/thor_backend.h` lists "who owns the swapchains when a backend
presents two guest screens" as an open question.** Cemu answers a large part of
it: **two swapchains, one per screen, with a per-screen previous-frame marker so
neither stalls the other**, and the fix is recorded as Android-specific.

**This is the Wii U's TV and GamePad, which is structurally the Thor's two
panels.**

## What survives, and one new fact

- **No fork uses Swappy.** Confirmed a second time, different search.
- **No fork uses `VK_GOOGLE_display_timing`.** Confirmed. **xenia's single hit is
  a logcat dump inside a research text file**, listing device-supported
  extensions — not code.
- **And that dump is evidence worth keeping: the Thor's Turnip exposes
  `VK_GOOGLE_display_timing`.** So the extension is available if wanted.

## Why the survey missed it

**It searched for two Google library names and stopped.** Cemu's mechanism is
spelled in Vulkan core terms — `present_id`, `present_wait`, `vkWaitForPresentKHR`
— and its most important part, host-driven vsync, is spelled in **guest timing**
terms and lives in `LatteTiming.cpp`, not in a renderer.

> **A subsystem survey that searches for named libraries finds adopters of those
> libraries, not implementations of the capability.**

That is the same failure as counting file names, and the same failure as the
uppercase mnemonic search earlier today.

## What this changes

- **Frame pacing is no longer "nothing to extract".** It is a PROPAGATE
  candidate with a clear source, and Cemu is MPL-2.0, which combines with
  GPL-3.0.
- **It stops being the cheapest thing in the queue**, because there is now a
  design to reconcile rather than a blank page.
- **Take all four parts.** Queue-depth limiting is the piece with the most direct
  benefit on a handheld, and host-driven vsync is the piece that decides whether
  a guest locked to 60 Hz behaves on a 60 or 120 Hz panel.
- **The two-screen presenter question moves from open to partly answered.**

## Limits

- **Nothing is measured.** No latency number, no frame time, no comparison
  against the other forks.
- **Whether `present_wait` and `present_id` are exposed by the Thor's Turnip was
  not confirmed here.** Cemu logs whether they are supported at run time, so the
  answer exists in a log this project has not captured. `tools/vk_capability_census.py`
  reports Cemu requesting both, which is what the fork asks for, not what the
  device grants.
- **The other six forks were not read for equivalent mechanisms** beyond the four
  search terms above. **Treat "only Cemu has this" as unproven**; the search that
  produced this correction was itself a second search of a claim that had already
  been made twice.

## Sources

- Cemu `src/Cafe/HW/Latte/Renderer/Vulkan/VulkanRenderer.cpp`,
  `SwapchainInfoVk.cpp`, `SwapchainInfoVk.h`,
  `src/Cafe/HW/Latte/Core/LatteTiming.cpp`
- xenia `docs/research/20260711-gmem-residency-pass-fusion-30fps-56sol.txt`
- `research_log/20260823_0040_frame_pacing.md`, the survey this corrects
