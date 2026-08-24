# Turnip on Adreno breaks attachment self-read in BOTH forms, and that qualifies the technique this repo wanted to take

**Goal: close the two loose ends named in the driver-profile log — which Turnip
versions carry which bugs, and what the per-profile cache tuning is for.**

**The first one produced the most consequential single row in the table.**

## The rule

`GSGPUDriverProfile.cpp:458`:

```cpp
{"vk-turnip-attachment-self-read", Vulkan, Adreno, MesaTurnip,
   /* no version range */ 0, 0, 0, ...,
   Bug(BrokenSubpassFeedback) | Bug(BrokenAttachmentFeedbackLoopLayout),
   Workaround(UseRenderTargetCopyForFeedback)},
```

> **Turnip on Adreno is recorded as breaking attachment self-read in BOTH
> forms** — the input-attachment/subpass read **and** the feedback-loop layout.
> **No version range, so the rule applies to every Turnip build.**

**And the header says what the fallback costs:**

> *"Read the render target from a separate **COPY** instead of in-pass, for
> drivers where no form of attachment self-read works. Turns texture barriers
> off, **which also disables framebuffer fetch (it is the same in-tile read)**,
> so the RT is never bound as an attachment and sampled at once. **Expensive — a
> full render-target copy per feedback draw** — so it is a last resort."*

**The comment beside the rule adds that it is not Turnip-only:** *"The reporter
sees the same failure on the proprietary blob."* **So it is an Adreno property,
not a Mesa regression.**

## The provenance, read, and it narrows the finding sharply

> *"ARMSX2 #442: with an HD texture pack, **Tales of the Abyss loses its entire
> 2D text layer** the moment the replacement's alpha range flips those draws to
> `require_one_barrier` and the RT self-read engages. **Device A/B on
> Turnip/Mesa 26.1.2 + Adreno 650** established that BOTH in-pass forms drop the
> content — the `subpassLoad` input attachment AND the feedback-loop-layout
> `texelFetch` sampler — while reading a separate RT copy renders correctly."*

**This is a real measured defect from a real symptom, and it is not this
device.**

| | the rule's evidence | this project |
| --- | --- | --- |
| GPU | **Adreno 650** (a6xx) | **Adreno 740** (a7xx) |
| driver | **Turnip / Mesa 26.1.2** | pinned candidate **Mesa 26.3.0** or MrPurple **T30** |

> **The rule carries no version bound because it was written from ONE A/B, not
> because it was re-tested and still failed.** Those are very different
> statements and the table cannot tell them apart.

**And the gap is exactly where a fix would live.** `CLAUDE.md` records that the
T30 changelog *"states it fixed a7xx support and dropped a710 and a720"* —
**a different GPU generation from the one the defect was measured on.**

**So the correct reading is: a serious, measured, Adreno-family defect on an
older part and an older Mesa, with no evidence either way for the Thor.** That
raises the value of the device experiment rather than settling it.

## Why this matters more than a driver quirk

**`CLAUDE.md` records PCSX2 2.6.0's feedback reads as a technique to take:**
binding one texture as both shader resource and render target, *"with reported
gains of 596% and 413% on specific titles"*, and it argues the technique
transfers because **"Vulkan has attachment feedback loops, the PS2 reads render
targets constantly, and ARMSX2 renders through Vulkan on the Thor."**

> **On the pinned driver, on this GPU, that path is recorded broken in both of
> its forms, and the fallback is a full render-target copy per feedback draw.**

**The premise of the transfer was the availability of the mechanism, and the
mechanism is exactly what this rule says does not work here.**

**It also reaches the render-pass work.** `THOR_RENDER.md` and the render-pass
survey care about in-pass reads and framebuffer fetch; the workaround **turns
texture barriers off and disables framebuffer fetch with them**, because they are
the same in-tile read. **A tiler-friendly in-pass read is the thing a shared
render graph would most want, and this is a recorded reason it may be
unavailable.**

**Nothing here is measured on this device or on this driver**, and the rule was
written from a single A/B on **Adreno 650 / Mesa 26.1.2**. `DEVICE_QUEUE.md`
entry 26.

## The second loose end: the tuning is per PART, not per driver

I wrote that a driver profile carrying cache sizes claims the right cache size
depends on the driver. **Read, and it is narrower and better founded:**

> *"The pool budget is per-GPU on mobile: the GPU-profile tables resolve a
> `MobileGsTuning` whose pool sizes and ages are sized for the part (**an Adreno
> 200 wants ~48 pooled, a laptop-class Adreno X ~160**), instead of the one-size
> **300** that suits a desktop GPU."*

**So the claim is per-PART, and the profile carries both axes.** For this project
that is one row — but it means **the shared memory budget owner has a fleet
precedent for sizing pools from a hardware profile rather than a constant**,
which is what `CLAUDE.md` asks of it.

**And the same comment records a near-miss of a familiar shape:**

> *"including its `__ANDROID__` scoping: off Android the profile is never
> resolved, so the tuning would still hold its conservative default (96/8/96/6)
> and would **silently cut the desktop pool from 300 to 96**."*

**A default that would apply on a path where the profile never resolves** —
mechanism 10 in `DID_IT_APPLY.md`, caught deliberately and scoped out. **Worth
recording as the first instance in this fleet of that mechanism being AVOIDED on
purpose rather than found afterwards.**

## Limits

- **The rule is a real device A/B, and it is on Adreno 650 with Mesa 26.1.2.**
  The Thor is Adreno 740 and the pin is newer. **No evidence either way for this
  device.**
- **The absence of a version bound is not evidence of breadth.** It reflects one
  A/B, not a sweep.
- **The 596% and 413% figures are PCSX2's, on D3D12**, and `CLAUDE.md` already
  says the headline numbers are irrelevant here and only the technique
  transfers. **This log narrows the technique, not the numbers.**
- **`UseRenderTargetCopyForFeedback` was read in the header, not in its
  implementation.**

## Sources

- ARMSX2 `pcsx2/GS/Renderers/Common/GSGPUDriverProfile.cpp:456-461`
- ARMSX2 `pcsx2/GS/Renderers/Common/GSGPUProfile.h`, the
  `UseRenderTargetCopyForFeedback` comment
- ARMSX2 `pcsx2/GS/Renderers/Common/GSDevice.cpp:828-840`
