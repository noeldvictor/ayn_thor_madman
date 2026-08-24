# An x86-detour candidate that turns out not to be one

**Goal: I flagged `VUOverflowHack` as a candidate because its comment blames
x86 — *"not really possible on x86 without soft floats"* — and this project's
central CPU thesis is that x86 rationales deserve re-checking on ARM64. Check
it.**

**Device-free: two file reads. No device used.**

## The answer is no, and ARMSX2's own ARM64 port says so

`pcsx2/arm64/microVU_Flags-arm64.inl:81`:

> *"Port of x86 `microVU_Upper.inl` `CHECK_VUOVERFLOWHACK` block. **We can't
> distinguish a genuine FLT_MAX result from a saturated overflow without
> soft-float**, so this stays a per-game gamefix (Superman Returns)."*

**The PS2's VU saturates to FLT_MAX on overflow. IEEE hardware produces Inf.**
Once the hardware has produced its result, **a genuine FLT_MAX and a saturated
overflow are the same bits** — on x86, on ARM64, on anything IEEE-754.

> **The constraint is IEEE-754, not x86.** `Config.h`'s comment names the host
> the code was written on, which is how a general limitation acquires a
> particular scapegoat.

**The ARM64 port re-derived the same conclusion independently and reached the
same design**: keep it a per-title gamefix.

## Why recording a NEGATIVE here matters

**This session and this repo have found many real x86 detours** — the register
model, timing constants, `fptosi` corrections that became corruptions, a
polyfill DAG worth 3x to 15x, an addressing mode that folds on x86 and does not
here.

> **A lens that only ever confirms is a hammer.** `VUOverflowHack` looked exactly
> like the others — an x86 rationale in a comment, on a hot path, in a fork this
> project mines — **and it is not one.**

**The tell, in hindsight**: the other detours are about **how the host expresses
an operation**. This one is about **what information survives the operation**,
and no ISA choice recovers information the format discarded.

## Two things worth keeping from the ARM64 implementation

**It enumerates what the hardware already does correctly**, which is the
discipline this repo records from ARMSX2 elsewhere:

> *"Detect any lane that reached the saturation boundary (`|result| >= FLT_MAX`,
> **which also catches Inf/NaN — matching x86's `CMPNLT.PS`, since for
> sign-stripped IEEE floats the integer ordering equals the float ordering and
> Inf/NaN sit above FLT_MAX**)."*

**One comparison covers three cases, and the comment proves it rather than
asserting it.**

**And the quirk has a test**: `tests/ctest/core/recompilers/vu_overflow_hack_tests.cpp`.
**A per-title gamefix with a regression test is rarer than it should be**, and it
is this project's own "a propagation lands with a test" rule met by a fork.

## Limits

- **Two files read.** Nothing built or run, no device.
- **CLOSED THE SAME SESSION: all eighteen were examined.** Grepping the whole
  `GamefixOptions` block for `x86|sse|soft ?float|intel|amd|host` returns **one
  genuine hit — `VUOverflowHack`, the one above.** The other seventeen comments
  describe **guest** behaviour: a title that hangs, a game needing an unmapped
  address, FIFO timing, VU sync. **The x86 rationale in this quirk list is
  singular.**

  **And the second hit was a FALSE POSITIVE worth a line**: `VIF1StallHack`
  matched because **"proce`sse`s" contains `sse`.** A substring search for an
  ISA name matches ordinary English — **the inverse of the vocabulary trap this
  session keeps hitting, a false POSITIVE from matching too loosely rather than a
  false negative from matching too narrowly.** Both are fixed by reading the hit.
- **No claim that soft-float is impossible**, only that it is what the fix would
  require — and both ports declined it for the same reason.
- **`Config.h`'s comment is misleading and it is not this repo's file to fix.**

## Sources

- ARMSX2 `pcsx2/arm64/microVU_Flags-arm64.inl:78-100`, `pcsx2/Config.h:1405,1822`
- `research_log/20260826_0020_the_fifth_patch_kind_has_eighteen_more_instances.md`
