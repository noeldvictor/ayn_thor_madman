# The verification debt, updated after a day of borrowing other forks' numbers

**Goal: this session quoted a great many figures from azahar and xenia.
`VERIFICATION_DEBT.md` exists so borrowed numbers cannot quietly become ours.
Bring it current.**

## Why this is not housekeeping

**The file's most dangerous section is "taken from another fork's measurement,
not reproduced", because those entries LOOK measured.** This session added about
a dozen such figures to `CLAUDE.md` in one day — fusion ratios, Vulkan
percentages, an audio latency, a frame-time P95, an assertion count.

**Every one of them is azahar's or xenia's, on that fork's workload, on its
device.** Left unrecorded they read, in six months, as this project's own
measurements.

## What moved

**Eight rows added to the borrowed section**, each naming the fork and the
workload: the `RSHRN` 3.5x-14.8x kernel result, the four fusion rejections, the
Vulkan percentages on Super Mario 3D Land, the 439,504-assertion crash on 7th
Dragon, the power-mode P95 table, the Cubeb latency, and the A510 `ST4` `1/50`
figure — **which is a manual quotation and is flagged as belonging with the
argued rows as much as the borrowed ones.**

**Two existing rows gained qualifications rather than resolutions:**

- **Turnip attachment self-read.** xenia device-verified
  `VK_KHR_dynamic_rendering_local_read` as **available** on the 740 under Turnip
  26.3.0. **The row stays**, because availability is not correctness — the
  `extended_dynamic_state3` lesson from the same device.
- **"The Thor is one machine."** The **method** to settle it is now recorded —
  `midr_el1` per core, read with `run-as` — but it still needs a second unit.
  **A method is not an answer.**

**Three new argued-only rows**, including one of mine: **the guest sample rate
explaining azahar's audio latency is my hypothesis, not azahar's claim**, and it
is written into the file as mine so nobody later attributes it to the fork.

## One structural addition

**The file now points at `shared_layer/REJECTED.md` and the experiment ledger**,
with the distinction stated: **this file records what is unverified; those record
what is already measured dead.** A lever can be well-verified here and settled
there, and the two lists answer different questions.

## The rule this file states, and the reason to keep obeying it

> **"When something moves state, move it here — and DELETE the row when it
> reaches `measured`, because a debt list that only grows stops being read."**

**Nothing was deleted today, because nothing reached `measured`.** No device was
used, so nothing could. **That is the honest outcome and it is worth naming**:
a day of good reading moves rows from *argued* to *read* and adds rows to
*borrowed*. **It cannot empty the list.**

## Limits

- **The borrowed section is not exhaustive.** It holds the figures this repo
  quotes in decisions, not every number ever read.
- **No row was verified today.** The update records provenance; it establishes
  nothing new.
- **Some rows belong in two sections** — the `ST4` figure is both borrowed and
  manual-derived — and the file notes that rather than duplicating the row.

## Files

- `VERIFICATION_DEBT.md`
