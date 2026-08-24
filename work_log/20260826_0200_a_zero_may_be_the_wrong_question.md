# A zero may be the wrong question, and the tool now says so

**Goal: three times in one session a zero I reported was a vocabulary failure.
Make the warning structural instead of remembering it.**

**No device. One tool change.**

## The three, all mine, all this session

| The zero I reported | What it actually was |
| --- | --- |
| **melonDS saves no `D8`/`Q8`** across the AAPCS boundary | it calls **`ABI_PushRegisters(BitSet32(...))`**, a Dolphin-derived abstraction |
| **no Oboe latency figure in three forks' `AGENTS.md`** | ARMSX2's is **92 lines about project shape** and never mentions audio |
| **Cemu, azahar, Vita3K and melonDS have no game quirks** | they **compare a title id inline** — 31, 22, 12 and 1 files |

**The first two I caught with a positive control.** The third I flagged as "weak"
and it was worse than weak — **it was the wrong question**, and the correct search
returned 66 files.

> **This repo has had the rule since before I started**: *"a survey that searches
> for a named library finds adopters of that library, not implementations of the
> capability."* **Knowing it did not stop me doing it three times in a day.**

## What was changed

`tools/bug_class_sweep.py` already prints **"A HIT IS A SHAPE, NOT A DIAGNOSIS"**
after every run. **The complement was missing**, and it now prints:

> **AND A ZERO MAY BE THE WRONG QUESTION, NOT AN ABSENCE.** Every class here
> searches a SHAPE. A zero means that shape is absent, which equals the
> CAPABILITY being absent only if the shape is the only way to express it.

**With the three instances named**, because an abstract warning is what I already
had, and **before reporting a zero: name a second spelling, and run a positive
control proving the search space is non-empty.**

## Why the tool rather than a document

**`CLAUDE.md` carries the rule and I read `CLAUDE.md` constantly.** It did not
help.

> **The tool prints at the moment a zero appears on screen**, which is the moment
> the mistake is made. **That is the same argument this project makes for build
> guards over review comments** — put the check where the action is.

**It is still only a print.** A tool that could actually detect the failure would
need to know the capability's other spellings, which is the hard part and the
reason the trap exists.

## Limits

- **A printed warning, not a check.** Nothing fails, nothing is blocked.
- **It appears on every sweep**, including ones with no zeros, so it will become
  wallpaper the way any always-on banner does. **The three named instances are
  the part likely to survive that**, because they are specific.
- **The three instances are mine and from one session.** No claim that this is
  the fleet's most common error, only that it was mine three times.

## Files

- `tools/bug_class_sweep.py` — footer
