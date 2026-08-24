# `vkGetFenceStatus` BLOCKS on Turnip-over-KGSL, and it costs a full GPU frame per frame open

**Goal: finish `spin.md`'s last unread section — a fence wait that defaults to
polling — and check the pattern across the fleet.**

**The sweep found something much larger than the pattern: on this device, the
non-blocking query is not non-blocking.**

## The defect, in xenia's own words

> *"On **Turnip-over-KGSL** a fence status query (**`vkGetFenceStatus`, or any
> wait reaching the kernel with timeout 0**) on an **in-flight** fence **BLOCKS
> until that submission retires** (Mesa `tu_knl_kgsl.cc` passes ioctl
> `timeout=0`, and the KGSL kernel documents **`timeout==0` as "wait forever"** —
> `adreno_drawctxt.c`)."*

> **`timeout = 0` means "return immediately" in Vulkan and "wait forever" in
> KGSL.** The inversion happens at the ioctl boundary, inside Mesa, invisible to
> the caller.

**And the measured cost:**

> *"The unconditional pre-poll therefore **drains the pending-fence list into the
> just-submitted fence and serializes the CPU to the GPU for a full GPU frame at
> every frame open** (Burnout B85: **46.9 ms = `gpu_frame_us`**)."*

**A full GPU frame of CPU-GPU serialisation, per frame, from a call whose entire
purpose is to avoid waiting.**

## Why this reaches the shared layer, not just one backend

**`CLAUDE.md`'s case for the packed binary lists one texture upload path, one
memory budget owner, one pipeline cache.** **Every one of those naturally asks
"is this fence done yet?"** to decide whether a buffer, image or descriptor set
can be reused.

> **On this device that question is not cheap. It is the most expensive thing the
> CPU can do.** A shared resource pool doing optional freshness polls would
> serialise the whole app to the GPU — **and it would do so for seven backends at
> once.**

**xenia's API rule is the one to take, and it is precise:**

> *"Callers doing **optional freshness polls** (reuse/reclaim checks) should use
> `GetCompletedSubmissionFromLastUpdate` **instead of**
> `UpdateAndGetCompletedSubmission` when this is set."*

**Two methods, one cached and one querying, and the type tells you which is safe.**
`IsLazyCompletionPolls()` exposes the mode so a caller can choose.

## The fix, which is a design and not a workaround

`lazy_completion_polls_`: **never issue a completion-status query beyond what the
await itself requires.**

- **Skip every query** when the last-known completed value already covers the
  awaited submission.
- Otherwise **go straight to the implementation wait**, which is **bounded to
  fences ≤ the awaited submission — never the in-flight tail.**

> **The bug was not "we polled". It was "we polled the WRONG fence".** The
> unconditional pre-poll reached the just-submitted work, which is exactly the
> fence guaranteed not to be ready.

## The fleet check

**Counted `vkGetFenceStatus`-style polls against `vkWaitForFences`-style blocks**
in each packed-binary fork's own source, vendored trees and the Vulkan headers
removed:

| Fork | poll | block |
| --- | --- | --- |
| **xenia** | **1** | **0** — and that one is the guarded, lazy path above |
| ARMSX2 | 3 | 7 |
| Cemu | 3 | 7 |
| eden | 3 | 3 |
| melonDS | 23 | 43 |
| **azahar, Vita3K** | **0** | 4, 6 |

**Reading is required before any of these becomes a claim** — a `getFenceStatus`
call is correct when its result is advisory and the caller does not spin on it.
**What the table establishes is that the pattern is present in five of seven
forks**, and **xenia is the only one that has met the Turnip behaviour and
designed around it.**

**And rpcsx's is the shape xenia's comment condemns**: `vk::wait_for_fence`
defaults to `timeout = 0` and takes the **polling** branch, spinning on
`vkGetFenceStatus` with `rx::pause()` between attempts — *"and `rx::pause()`
emits `YIELD`, an SMT hint that retires without effect on a non-SMT core"*, so
**the backoff is not a backoff.** On Turnip-over-KGSL that poll is a blocking
kernel wait wearing a spin loop's clothes. **rpcsx is out of the packed binary.**

## Where this belongs

- **Extraction candidate 9's table.** A second Turnip defect beside
  `vk-turnip-attachment-self-read`, and this one is **measured with a frame cost
  on this exact driver stack.**
- **The shared device layer's fence API**, whenever it is written: **two
  accessors, cached and querying**, with the querying one documented as a
  full-frame stall on this platform.
- **`DEVICE_QUEUE.md`**, as a caution rather than an experiment: **anything that
  polls a fence for reclaim must be measured before it ships**, and the failure
  mode is a smooth, plausible-looking frame time that is exactly one GPU frame
  too slow.

## Limits

- **Not reproduced.** xenia's comment, xenia's measurement, one title.
- **The Mesa and KGSL source lines are cited by xenia and were not opened here.**
- **The fleet table is a count, not a reading.** Five forks have the call; how
  each uses it was not checked.
- **Whether the pinned Turnip build still behaves this way is unknown** — the
  same open question as the attachment-self-read rule, and the same answer:
  a probe on the device.

## Sources

- xenia `src/xenia/ui/gpu_completion_timeline.h:55-100`
- rpcsx `docs/arm64/spin.md:1036-1075`, `vk::wait_for_fence`
- `shared_layer/OWNED.md` candidate 9
