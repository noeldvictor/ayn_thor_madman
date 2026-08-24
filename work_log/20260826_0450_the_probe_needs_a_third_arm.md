# The probe needs a third arm, and it is the one with a body of work behind it

**Goal: the first-hour plan says entry 26's probe "has to be written first". Before
anybody writes it, check whether the specification is still right.**

**No device. One entry sharpened from today's findings.**

## It was specified for two mechanisms and there are three

**ARMSX2's driver rule — the reason entry 26 exists — covers**
`BrokenSubpassFeedback` **and** `BrokenAttachmentFeedbackLoopLayout`. The probe
was written to test exactly those two.

**`VK_KHR_dynamic_rendering_local_read` is a third mechanism**, it postdates
ARMSX2's Adreno 650 measurement, and — the part that matters —

> **it is the one XenDroid's 16-commit in-pass EDRAM resolve series actually
> uses.**

**And xenia has already device-verified it AVAILABLE on this Adreno 740 under
Turnip 26.3.0**, probed with a title running. **Availability is not
correctness**, which is precisely what this entry exists to settle.

## Why the prediction had to split

**The original prediction was "both still fail".** With three arms that is no
longer the honest statement, because **the third arm has different reasoning
behind it:**

- **(a) and (b)** are the mechanisms ARMSX2 measured broken — **on a different GPU
  generation and an older Mesa.**
- **(c)** is **a different code path in the driver**, not a rename of the old
  ones.

**New prediction: (a) and (b) fail, (c) works.**

> **And that is the MOST USEFUL possible outcome**, which is worth saying before
> the run rather than after. It would mean the old rule holds, the modern path
> does not inherit it, and **XenDroid's series becomes portable work against a
> wall this project has already measured** — 45 EDRAM transfers per frame, 27 of
> them pass breaks.

**A prediction that names which result would be most useful is not the same as
hoping for it.** The split is stated so a `(c) fails` result is a clean
refutation rather than a disappointment.

## The general point

**This entry was written on 2026-08-25 and was already stale on 2026-08-26.** Not
because it was wrong, but because **two findings landed after it** — XenDroid's
resolve series and xenia's availability probe — **and neither updated the entry
that they bear on.**

> **A queue entry is a hypothesis with a date.** The queue has 27 of them, and
> **no mechanism re-reads one when a related finding lands** — checked by reading
> `supervise.py`'s six checks, none of which touches staleness, and by listing
> `tools/`, where no tool cross-references the queue against the logs. **A narrow
> search of one directory, so treat it as "none was found" rather than "none
> exists".** That is the same
> shape as the capability inventory going two days stale, and as entries 12 and
> 13 not citing each other.

**No mechanism is proposed for this here.** It is the third instance of the same
maintenance gap in two days, which is enough to name and not yet enough to
automate.

## Limits

- **The third arm is specified, not written.** Entry 26 still says nothing about
  who writes the probe.
- **`local_read` availability is xenia's probe on its build**, not re-read here.
- **The prediction split is reasoning, not evidence.** Nobody has run any arm.

## Files

- `DEVICE_QUEUE.md` entry 26 — three arms, split prediction
