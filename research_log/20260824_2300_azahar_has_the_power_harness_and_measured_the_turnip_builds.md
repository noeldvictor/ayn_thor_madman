# azahar has a real under-6-W power harness, and it already measured three Turnip builds on this device

**Goal: mine azahar's 1,458-line index by topic rather than linearly.**

**Eleven Turnip mentions, and they contain a measured driver A/B this project has
had queued as `DEVICE_QUEUE.md` entry 2 since it was written.**

## 1. The Turnip A/B, measured, on this device

> A live **20-sample bracket at 3x and 615 MHz**: **generic R8 at 8.022% mean
> KGSL busy, forced-Sysmem R8 at 9.775% (21.86% more GPU time)**, and the older
> **PurpleVK/T26 build at 8.008% — a 0.18% noise-scale tie**. **All reproduced
> the exact accepted frame; process CPU activity was also similar.**
>
> **"Reject Sysmem for this workload, and do not call PurpleVK..."**

**`CLAUDE.md` lists five Turnip candidates, marks the pin *provisional*, and says
"Do not pin from reading a changelog. Measure."** **Two of those five have now
been measured against each other on this device, by a fork, with a frame-hash
check that both produced the same output.**

**The Sysmem result confirms this repo's own prediction with a number.** The
driver table says of the Sysmem variant: *"Forces system memory rendering
instead of GMEM tiling. **Expect it to be slower**; it exists to work around
bugs."* **Measured: 21.86% more GPU time.**

**The PurpleVK tie is the more interesting half**, because the pin candidate
here is a MrPurple build. **On this workload it is indistinguishable from generic
R8** — which means the pin decision is not a performance decision on this scene,
and should be made on other grounds.

**Two limits, stated by azahar itself:** it is **one fixed scene** (a 7th Dragon
scene, with Super Mario 3D Land used elsewhere), and the accepted driver is
**R8, not the newer builds** this repo lists.

## 2. The power harness, which is far stricter than this repo's one gate

**`CLAUDE.md` gates power on `status=Discharging`.** azahar's
`tools/measure-thor-power.ps1` gate requires **all** of:

| Gate | Why it exists |
| --- | --- |
| the production ARM64 package **and an accepted config hash** | a different build measures a different thing |
| **Standard performance mode 0, fan mode 4** | and *"changing High Performance to Standard on this firmware **also reset fan mode 4 to 1**; always read and restore both explicitly"* |
| **the driver identified by LOGGED METADATA, not the banner** | *"generic and forced-Sysmem R8 expose the **same banner** despite measurably different work"* |
| a **real** discharging battery | *"reject Android's simulated/stopped battery state, every dumpsys external-power flag, and the Thor's **USB, wireless, or UCSI charger-online** sysfs flag before/during/post run"* |
| **mean AND nearest-rank P95** power at or below 6 W | a mean alone hides the spikes |
| **the fixed scene's expected SCREENSHOT HASH and brightness** | the route is verified, not assumed |
| a **SurfaceFlinger** gate: exactly two BLAST layers, ≥60 intervals on one, ≥29 FPS mean, ≤40 ms P95, **zero intervals over 50 ms** | |
| an **AudioFlinger** gate: same track before and after, 32,728 Hz, ≤2,048 frames, ≤150 ms latency, **zero underruns** | *"A power/FPS win with **audio breakup** or a restarted track is a failure."* |
| **manual brightness mode, two active physical displays**, brightness recorded before warmup, after warmup and after sampling, **and drift rejected** | |

**Three of these are rules this repo does not have and needs.**

- **Identify the driver by logged metadata, not its banner.** Two builds that
  differ measurably present the same string. **This repo's rule is "state the
  driver build in every performance claim" — and the obvious way to do that is
  the banner.**
- **Audio is a failure condition, not a separate concern.** A power or frame win
  accompanied by an underrun is a **failure**. Nothing here says that.
- **Brightness is part of the power measurement.** *"Full-scale brightness is not
  a sensible hidden constant near a 6 W total-device ceiling."* **On a two-panel
  handheld, an unrecorded brightness change can be a larger delta than the
  change being measured.**

**And a trap worth carrying:** the Thor exposes `panel0-backlight` and
`panel1-backlight` sysfs nodes, **but both returned `actual_brightness=0` while
visibly ON** — *"do not treat those raw nodes as luminance evidence."* The
display service reported both panels ON at brightness 1.0 while Android's global
setting was 255. **A physically-impossible reading again: the node is broken, not
the panel.**

## 3. The performance-mode numbers, and a firmware coupling

On a Super Mario 3D Land attract loop:

| mode | FPS | P95 frame time |
| --- | --- | --- |
| High Performance / 615 MHz | **59.256** | **20.673 ms** |
| Performance / 550 MHz | 58.397 | 27.626 ms |
| Standard / 401 MHz | 57.935 | 27.460 ms |

**All had zero intervals over 50 ms and clean audio**, and azahar's conclusion is
the honest one: *"**neither lower mode was a free speed-preserving swap**"* — the
FPS barely moves and the **P95 frame time rises by a third.**

> **A mean that barely moves while P95 rises 34% is exactly why this repo reports
> `[min..max]` and not a mean.** Here is the case.

**And the firmware coupling is a genuine trap:** *"Changing High Performance to
Standard **also reset fan mode 4 to 1**."* **One vendor toggle silently moved
another.** That is `DID_IT_APPLY.md`'s disease in the device's own settings —
**a second writer, in firmware.**

## 4. "Do not enable a feature merely because the driver advertises it"

**Three rejected optimisations, each with numbers and a revisit condition.** The
sharpest:

> **The physical Thor confirmed all four required `VK_EXT_extended_dynamic_state3`
> blending features and rendered the exact loop correctly** — and a
> profiling-off bracket left Turnip's `tu_CmdBindPipeline` self share **unchanged
> at 1.25297% versus 1.25021%**, while azahar's own `BindPipeline` self share
> **rose 9.17% relative**. **Fully reverted.**

> **That is the counterweight to the capability-ceiling argument made here
> today.** Five forks cannot run an fp16 shader because their device layers never
> ask — **and having a feature is not a reason to use it.** **The ceiling
> argument says a capability should be AVAILABLE; it does not say it should be
> TAKEN.**

**And a driver defect found by a fork, on this driver family:** a pipeline-cache
shortcut *"passed focused static/dynamic-state tests but **crashed the physical
Thor's Turnip worker in `tu_cmd_render<chip7>` with a null dereference**."*
**A second Turnip bug for candidate 9's table**, beside the attachment-self-read
rule.

## Limits

- **Topic-sampled, not read.** Eleven Turnip mentions and the power-harness
  entries; **1,300+ lines remain unread.**
- **Every number is azahar's**, on 3DS workloads, its own builds and scenes.
  Nothing reproduced here and no device used.
- **The driver A/B is one scene at one clock**, and azahar says to recalibrate
  for a materially different title.
- **`tools/measure-thor-power.ps1` was not read**, only the gate list its index
  describes.

## Sources

- azahar `AGENTS.md:88-128`, `158-164`, `522-546`
