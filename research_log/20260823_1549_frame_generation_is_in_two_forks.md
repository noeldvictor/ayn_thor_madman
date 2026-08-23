# Frame generation exists twice, in two different designs

**Goal: verify the live absolute negatives still sitting in
[`CLAUDE.md`](../CLAUDE.md), starting with the highest-value one.**

`tools/supervise.py` found **15 unqualified negative claims** still in
`CLAUDE.md`, written before the rule that a negative needs a second search. The
repo's own table says every such claim it has made has been wrong. This checks
them.

No device. Reading only.

## The claim, and it is wrong

> **ARMSX2 has a complete Vulkan frame-generation subsystem, 31 files, all
> GPL-3.0-or-later.** Nothing else has one, and nothing recorded it until
> 2026-08-22.

**xenia has one too.** Five files, both the Vulkan and D3D12 presenters, three
cvars, a tick thread, a blend pass and a motion-warp estimator.

**That is the thirteenth time an absolute negative in this repo has been wrong.**

**The first search missed it for the usual reason.** Searching filenames for
`framegen|frame_gen|lsfg|fsr3` returns 39 files in ARMSX2 and **zero** in xenia,
because xenia's lives in `src/xenia/ui/presenter.{h,cc}` under names like
`current_paint_synthesize_frame_` and `DoFrameGenSynthPresent`. **A file-name
search cannot see a feature that lives inside another subsystem's files.**

## They are two different designs, with opposite latency costs

**This is the LRU-cache pattern again: one name, two designs.** And here the
difference decides which one belongs on a handheld.

| | ARMSX2 | xenia |
| --- | --- | --- |
| Where | `pcsx2/GS/Renderers/Vulkan/FrameGen/`, 31 files | `src/xenia/ui/presenter.*`, in the presenter |
| Method | **interpolation** | **extrapolation** |
| Source | ported from **eden PR #4263** and **lsfg-vk** | written here |
| Needs the next real frame | **yes** | **no** |
| Latency cost | **a held frame** | **none** |
| Pacing | its own `FrameGenPacer` | a tick thread against a smoothed guest interval |
| Default | — | **off, and byte-identical when off** |

**Verified by vocabulary.** ARMSX2's `FrameGen/` directory contains eight
occurrences of *interpolate/interpolated/interpolation* and **zero** of
*extrapolate*. xenia's flag is literally `present_frame_extrapolation`.

**The tradeoff is the classic one.** Interpolation synthesizes a frame *between*
two real ones, so it cannot present until the later one exists — **one full
guest frame of added latency**. Extrapolation forward-projects from the newest
frame, so it adds none, and pays in accuracy instead.

**On a device with real buttons that difference is not a detail.** `CLAUDE.md`
already argues the Thor's physical controls matter enough to hide the touch
overlay. **A held frame is the same class of cost**, and nothing in the repo had
priced it.

## xenia's is more configurable than the claim implies

Three cvars, all `Display`:

- **`present_frame_extrapolation`** — off by default; when off, "the present path
  is byte-identical to today".
- **`present_frame_gen_factor`**, default **2** — presented frames per guest
  frame. The tick thread subdivides the guest interval into slices and aims at
  the next unfilled boundary, so factor 3 synthesizes at 1/3 and 2/3.
- **`present_frame_gen_motion_warp`** — off by default. Replaces the 50%
  cross-fade with a **global motion-compensated warp**: a separable
  **Lucas-Kanade** estimate in a 1x1 RGBA32F pass, forward-extrapolating the
  newest frame by half the estimated camera translation. Its own text says it is
  "sharper than the cross-fade on camera pans (no ghost)" and that it "captures
  camera/background motion only (fast local motion is not warped)".

**So xenia has two synthesis methods and an A/B between them**, already wired.

## The argument for frame generation that this repo does not have

xenia's motivating comment is stronger than anything in `CLAUDE.md`:

> For logic-locked-framerate guests (e.g. Blue Dragon's fixed 30Hz),
> synthesizing in-between presented frames is the only way to raise the
> *presented* frame rate

**That is a case where optimisation cannot help at all.** `CLAUDE.md` argues
frame generation makes a 30 fps game "feel like 60". **The real argument is
narrower and much harder to refute:** when the guest's game logic is locked to
30 Hz, no amount of CPU or GPU work raises the frame rate, because the guest
will not produce more frames. **Frame generation is the only lever that exists.**

**And xenia named a title.** Blue Dragon is in the fleet's own fake library and
in its bench work.

## What to change in CLAUDE.md

1. **Remove "Nothing else has one."** Record both, and record that they are
   different designs rather than duplicates.
2. **Add the interpolation-against-extrapolation split**, because it is a real
   decision with a latency consequence and it is not written down.
3. **Replace the "feels like 60" argument with the logic-locked one**, which is
   xenia's and is stronger.
4. **Do not merge them.** This is the LRU-cache result again: two designs
   answering two different tradeoffs, not one feature written twice.

## What this does not say

- **No claim that either is fast, or correct, on the Thor.** Both are off by
  default. Nothing here is measured.
- **No claim about which design is better.** The latency difference is
  structural; which matters depends on the title and the input, and that needs
  the device.
- **ARMSX2's 31 files were not re-read** for this. The count and provenance are
  `CLAUDE.md`'s, and only the interpolation-vocabulary check is new.

## Method

Two searches, different words. First over tracked filenames for
`framegen|frame_gen|frameinterp|lsfg|fsr3|dlssg` and over source for
`frame generation|framegen|frame interpolation|generated frame`. Second over
source for `motion vector|opticalflow|interpolated|FrameInterpolat|Pacer`,
excluding vendored trees.

**The second search is what found it.** azahar's three hits were audio and
network-beacon frames and are unrelated; xenia's two were real.

## The remaining fourteen

Still unverified in `CLAUDE.md`, listed by `tools/supervise.py`:

- "no fork plans render passes"
- "Vita3K tracks transient attachments and nothing else does"
- "nobody has priced Anime4K's two dedicated passes"
- "Vita3K also has a content path resolver, and nobody else does"
- "melonDS has `TouchVibrator`, haptics nobody else ships"
- "Storage aggregation remains the one screen with no prior art anywhere"
- and nine more

**Check them before relying on any of them.**
