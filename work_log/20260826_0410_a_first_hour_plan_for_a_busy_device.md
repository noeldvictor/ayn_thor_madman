# A first-hour plan, because 27 entries and no order is not a queue

**Goal: the debt update said the next useful session is a measuring one and named
which measurements retire the most rows. Turn that into an order.**

**No device — this is planning for when there is one.**

## The problem it fixes

**`DEVICE_QUEUE.md` says "ordered by what unblocks the most" and then lists 27
entries in the order they were written.** Somebody with a free hour has no way to
choose, and **the entries do not say which share a setup.**

## Three blocks, and the first is the whole argument

**Block A: no game, no timing, about twenty minutes.** Six items — entries 18 and
24, the **entry 26 Turnip probe**, and three sysfs reads this repo has quoted from
other forks and never taken itself: `gpu_model`, `CTR_EL0`, `midr_el1`.

> **Every one is a READ or a probe.** No title, no warm device, no stable scene,
> **so none of this queue's measurement traps apply** — no thermal soak, no noise
> floor, no scene variance, no A/B discipline.

**That is the highest ratio of debt retired to device minutes in the queue**, and
it is available to somebody who has the device for twenty minutes between other
work.

**Entry 26 is the single most valuable item in it.** Two substantial pieces of
borrowed work wait on its answer: **PCSX2's feedback-read technique** and
**XenDroid's 16-commit in-pass EDRAM resolve series.** A negative closes both; a
positive opens both.

## Block B, and it got cheaper while nobody was looking

**Pin the driver, because every later number must name one** — and azahar has
shown the runtime banner is insufficient, since generic and forced-Sysmem R8
expose the same one.

**But the question has narrowed.** azahar has measured three builds on this
device, and **XenDroid's opposite default turns out to be a CORRECTNESS choice,
not a performance one.** So the remaining question is **does the pinned build hang
on a title we care about**, not which is faster.

## Block C, and the reason to run it as a block

**Entries 15, 27 and 17 are all cache-warming questions measured on first play.**
They share one setup — clear caches, cold run, warm run — **and doing them
separately pays that cost three times.**

## What was moved OUT of device work

- **12 and 13, instruction inflation** — the largest item in the queue by stated
  importance, **now a cross build under qemu-user.**
- **20** — listed because it **gates** device work, not because it needs the
  device.
- **Anything predicting under 5%** — the savestate floor is ~5% and the cutscene
  floor is ~50%. **A 3% prediction needs a gated title screen or a fixed frame
  range, and that setup is its own session**, not something to squeeze in.

## Both stores checked before ordering anything

**This orders existing entries rather than proposing new levers**, and each entry
already carries its own ledger query. **Checked both stores again for the three
blocks:**

- **`exp_ledger.py check`** for `attachment`, `pipeline` and `driver`: the
  relevant `DEAD` rows are **`gpu_dynamic_blend_state`** and **`bindless`**,
  neither of which any block re-runs.
- **`shared_layer/REJECTED.md`**: the forced-Sysmem row is the one that touches
  Block B, and **it is the row I qualified today** — rejected on performance,
  adopted elsewhere for stability. **Block B is written around that, not
  against it.**

**Nothing in the three blocks is a lever either store records as dead.**

## Limits

- **The ordering is mine and is argued, not measured.** Nobody has run these, so
  "highest ratio of debt retired to minutes" is an estimate from reading.
- **The twenty-minute figure for Block A is a guess.** Six reads and one Vulkan
  probe, with no title load — but the probe has to be written first, and **entry
  26 does not say by whom.**
- **Block C assumes the three share a setup.** They share a *shape*; whether one
  title serves all three was not checked.
- **This does not reduce the queue.** Twenty-four entries still wait, and most of
  them genuinely need the device.

## Files

- `DEVICE_QUEUE.md` — a "first hour" section above the entries
