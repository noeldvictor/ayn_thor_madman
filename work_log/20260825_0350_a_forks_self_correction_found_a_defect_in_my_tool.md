# A fork's self-correction found a defect in my own sweep, and fixing it left one true positive

**Goal: read the tail of `spin.md`, starting at its own correction — the rule
established two hours ago.**

**The correction describes a mistake, and my `x86_only_fastpath` class makes the
identical one by construction.**

## The correction, and why it applies to me

rpcsx concluded that upstream's "spin briefly, then park" design *"was never
written"* for AArch64, and then withdrew it:

> *"**How I got it wrong:** I grepped the `#if defined(ARCH_X64)` guard, read the
> `__tpause` call under it, and concluded from the guard alone that ARM had no
> arm — **without reading to the `#endif`**. The ARM branch was inside the same
> conditional, past my grep window."*

**`__arm64_monitor_wait` is a complete `LDXR` + `WFE` + `CLREX` park, twenty
lines below**, opt-in *"precisely because a missed wakeup would be a hang"*.

**Its own diagnosis names this repo's most repeated failure:** *"the exact
mistake this repo already records twice — a `head`-truncated grep treated as
evidence of absence, and `on_frame_end` 'never called' because the real call site
was one line past the cut."* **Third instance in one fork.**

**And this variant is not a `head -N` truncation. It is conceptual:** you find
the x86 branch and assume the conditional has one arm.

> **`tools/bug_class_sweep.py --class x86_only_fastpath` matched
> `defined(__SSE2__)` on a single line and had no way to see an `#elif
> defined(__aarch64__)` twenty lines below.** Its own `what` text says *"with no
> ARM sibling in the guard"* — **and it could not check that.**

## The fix, and two bugs found by reading the hits

**`scan()` gained `near_absent`**: the inverse of `near`. A hit survives only when
a pattern is **absent** from its window. The x86 guard counts only when **no ARM
token appears within 30 lines.**

**Reading the results found two defects in the fix itself:**

- **The match line was not checked.** Cemu's `fast_float.h:257` is
  `#if defined(_M_X64) || defined(_M_ARM64)` — **correct code naming ARM64 in
  the guard itself** — and checking only the *following* lines reported it as
  x86-only.
- **An architecture-detection block is not a fast path.** Cemu's
  `precompiled.h:29` guards `#define ARCH_X86_64`. **A guard whose body defines
  the architecture token is the definition, not a gate.**

## The result

| | before | after |
| --- | --- | --- |
| rpcsx | 39 | **22** |
| Vita3K | 1 (its `spin_wait.h`, **correct code**) | **0** |
| Cemu | 3 | **0** |
| ARMSX2 | 1 | **1** |
| xenia, azahar, melonDS, eden, GameThor | 0 | **0** |

**One hit across the seven packed-binary forks, and it is a known true
positive**: ARMSX2 `common/TextureDecompress.cpp:473`, the **BC7 decompressor
with an SSE2 fast path and no ARM path**, already recorded in `CLAUDE.md` and
queued as `DEVICE_QUEUE.md` entry 24.

> **That is a positive control the class passed**: it re-found, unaided, the one
> instance a person had found by reading — **while dropping every false positive
> the looser pattern produced.**

## What survives from the fork's correction

Worth recording because the *shape* of a good correction is reusable — three
bullets separating what was right, what still holds, and what is **better** than
the original claim:

- **Right:** `has_um_wait()`, `has_waitpkg()` and `has_waitx()` genuinely return
  false on AArch64, so the x86 arm really is dead.
- **Still holds:** the **other** wait sites do not park — the MFC reservation
  loops and `vm::writer_lock` end in **unbounded `sched_yield`**, and the SPU JIT
  self-loop has no wait at all.
- **Better than claimed:** the primitive *exists*, written and reviewed and
  opt-in. **Wiring the remaining sites to it is reuse, not new work.**

**And the closing line is the one to keep:**

> *"**What is now actually unknown:** whether the GETLLAR park is enabled by
> default, and what it is worth when it is. **That is a measurement, and it
> should have been the first question rather than a claim about missing code.**"*

**Ask whether the thing is ENABLED before claiming it does not exist.**

## Limits

- **`near_absent` uses a 30-line window.** An ARM branch further than that still
  reads as absent, and rpcsx's real case was twenty lines.
- **rpcsx's 22 were not read**, only counted; it is out of the packed binary.
- **Three forks' `#if` formatting may not match the pattern at all**, so a zero
  here is "no matching guard found", not "no x86-only fast path".

## Files

- `tools/bug_class_sweep.py` — `scan(near_absent=, window=)`, and the class's
  `near_absent` clause
