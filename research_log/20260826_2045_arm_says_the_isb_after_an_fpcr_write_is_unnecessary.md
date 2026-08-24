# Arm says the ISB after an FPCR write is unnecessary, and the fleet already agrees

**Goal: `pdftotext` works on the Arm guides. The A715 has SEVENTEEN optimisation
sections and this project has recorded four. One is titled "FPCR
self-synchronization" and this project has a whole CPU lead about FPCR. Read it.
This proposes no lever.**

**Device-free: one PDF section and a fleet grep. No device used.**

## What §4.9 says

> **"writes to the FPCR register are SELF-SYNCHRONIZING, i.e. its effect on
> subsequent instructions can be relied upon WITHOUT AN INTERVENING CONTEXT
> SYNCHRONIZING OPERATION."**

**So no `ISB` is required after writing FPCR.**

**And §4.10 confirms the half this project already had**: most special-purpose
registers are **not renamed**, and accesses are subject to **non-speculative
execution, in-order execution, and flush side-effects.**

## Why this matters here specifically

**`CLAUDE.md`'s first CPU lead is that guest FP status may be serialising the
machine**, built on the X3 guide's non-renaming statement. **A careful
implementer meeting that lead would add an `ISB` after each FPCR write to be
safe.**

> **On this SoC that costs 11.42 ns — measured by rpcsx, already in this repo,
> and 32x a `yield`.** **Arm says it buys nothing.**

**Four FP environments make it worse.** ARMSX2 needs `FPUFPCR`, `VU0FPCR` and
`VU1FPCR`; the Xenon has two independent FP mode registers. **A synchronisation
per switch, on a path that switches often, is exactly the shape of cost this
project hunts.**

## The fleet already does the right thing

**Searched every fork's own code for an FPCR write, then for an `ISB` in the same
file:**

| Fork | FPCR-writing files | `ISB` in them |
| --- | --- | --- |
| ARMSX2 | `common/FPControl.h`, `pcsx2/arm64/iFPU-arm64.cpp`, `iFPUd-arm64.cpp` | **none** |
| xenia | `platform_arm64.cc`, `a64_backend.cc`, `a64_emitter.cc` | **none** |
| Cemu | `coreinit_Thread.cpp` | **none** |
| eden | `nce/patcher.cpp`, dynarmic address spaces | not opened |

**Positive control, because a zero needs one**: the same search finds `ISB` in
**5 ARMSX2 files, 2 xenia files and 2 Cemu files** elsewhere. **The instrument
works; the FPCR paths genuinely have none.**

> **Nobody is paying the unnecessary barrier.** A clean negative, and the useful
> kind — **it is now recorded WHY it is correct, so the next person meeting the
> non-renaming lead does not add one.**

## The bigger finding is the seam

**The A715 guide has 17 sections in chapter 4 and this project has recorded
four**: dispatch constraints, register spills, memory routines, region-based fast
forwarding. **Thirteen are unread**, including **Store to Load Forwarding**,
**Branch instruction alignment**, **Zero Latency Instructions**, **Special
register access**, and **Cache maintenance operation**.

> **And this is the APPLICABLE core** — today's correction established that the
> mid cluster does the most work and its tables are tighter. **The guide for the
> cluster that matters has been read at two hops, in fragments, via another
> fork's notes.**

## Limits

- **One section read.** The other thirteen are listed, not read.
- **The fleet check is file-scoped**: an `ISB` in a different file, emitted near
  an FPCR write at run time, would not appear. **eden's files were not opened.**
- **No claim that adding an `ISB` would be harmless to remove somewhere** — there
  is nothing to remove. **This prevents a cost rather than finding one.**
- **§4.9 is the A715's. The X3 and A710 guides were not checked** for the same
  statement.

## Sources

- `cortex-a715-software-optimization-guide.pdf` §4.9, §4.10
- `CLAUDE.md`, the guest-FP-status lead and the 11.42 ns `ISB` measurement
