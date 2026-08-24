# Two forks, opposite sysmem defaults, both right — and the cure for a hazard this repo only names

**Goal: XenDroid DEFAULTS to sysmem Turnip rendering. azahar measured forced
sysmem at +21.86% GPU time and rejected it. Resolve the contradiction.**

**Device-free: two commit diffs. No device used.**

## It resolves in XenDroid's own comment

> *"'sysmem' forces sysmem (untiled) rendering, **which masks a class of
> tiled-rendering (GMEM) artifacts and Adreno GPU hangs** — but only on Turnip;
> proprietary drivers ignore it, **so the underlying command-stream bugs must be
> fixed rather than hidden.**"*

> **Sysmem is not a performance choice there. It is a correctness workaround for
> DEVICE LOSS**, adopted knowingly, with the fork recording that it is hiding a
> bug rather than fixing one.

**azahar asked "is it faster" and answered no, by 21.86%. XenDroid asked "does it
hang" and answered yes.** **Both defaults are correct for the question that fork
was asking.**

### This corrects a row I wrote today

`shared_layer/REJECTED.md` lists **"forced-Sysmem Turnip R8 | azahar | +21.86%
GPU time"** among the measured rejections. **That row is incomplete as written**:
it is a rejection on performance grounds, and **another fork adopts the same
setting as a stability workaround.** The row now says so.

> **A rejection is scoped to the question that was asked.** This is the first
> time one of these rows has needed that qualification, and it will not be the
> last.

## The cure for a hazard this repo names and does not solve

**`CLAUDE.md` carries this as a measurement rule:**

> *"READ THE PERSISTED CONFIG BEFORE TRUSTING ANY A/B. **A persisted value
> overrides a compiled default forever** — across the process, the install and
> the app update."*

**xenia found three `rlwinm` fastpaths sitting false on the device while their
compiled defaults said true, worth +2.88%, and concluded that "every device
number taken this session was on a handicapped baseline".**

**XenDroid has machinery for exactly this, in the same diff:**

```cpp
MakeConfigVarUpdateDate(2026, 7, 24, 12);
UPDATE_from_string(turnip_debug, 2026, 7, 24, 12, "");
UPDATE_from_int32(vulkan_mid_frame_submission_draws, 2026, 7, 24, 12, 0);
UPDATE_from_string(readback_resolve, 2026, 7, 24, 12, "fast");
```

**A dated default migration.** Each line says: *at this timestamp the default
changed, and the OLD default was this value.* **A user still holding the old
default is moved to the new one; a user who changed it deliberately is left
alone.**

> **That is the difference between "we changed the default" and "we changed the
> default for the people who never chose one".** Without it, shipping a better
> default reaches only new installs — which is precisely how xenia's three
> fastpaths ended up false on a device whose code said true.

**It is lighter than the migration framework this repo took from melonDS.**
melonDS's migrates a **schema**; this migrates a **value**, and the two are
complementary rather than alternatives. **`app/shell/` has neither.**

## Two more from the same diff

**`vulkan_mid_frame_submission_draws = 1300`** — end and submit the command
buffer every N real draws *"so the GPU overlaps rendering with CPU
command-building instead of idling until swap."* Its own guidance:

> *"Try **~half the title's per-frame draw count**; **too-small values hurt tiled
> GPUs.**"*

**A tiler-aware lever this repo does not have**, and the tuning rule makes it
inherently **per-game** — half of *this title's* draw count. **It belongs with
the per-game override design rather than as a global.**

**`readback_resolve = "uma"`** — a value not in the documented set
(`fast`/`some`/`full`/`none`). **UMA is unified memory**: on a handheld the CPU
and GPU share physical memory, so a readback need not copy. **This repo discusses
readback cost and never mentions the UMA case.** **Unread — the implementation
was not opened, so what "uma" actually does is not established here.**

## Limits

- **Two commit diffs, one fork.** Nothing built or run, no device.
- **The sysmem resolution is from a comment.** No device-loss reproduction was
  shown, and **which titles hang is not stated.** azahar's 21.86% remains the
  only measurement either way.
- **azahar's number is on a 3DS scene and XenDroid's default is for the Xbox
  360.** Different guests, different render-target sizes — **the two may not even
  be measuring comparable workloads**, which is a second reason both can be
  right.
- **`readback_resolve = "uma"` is a config default only.** Its code was not read.
- **`1300` is XenDroid's number for its titles.** Copying it would be copying
  another game's draw count.

## Sources

- XenDroid, `[Android] Default to sysmem Turnip rendering and 1300-draw mid-frame
  submissions`, `[Config] Align shipped defaults with uma readback and sysmem
  Turnip rendering`
- `shared_layer/REJECTED.md`
- `CLAUDE.md`, the persisted-config rule
