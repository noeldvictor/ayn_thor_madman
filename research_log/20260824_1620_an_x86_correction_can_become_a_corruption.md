# On ARM64 an x86 correction can be worse than nothing, and one fork shipped that bug

**Goal: read rpcsx's "where the wins actually were", and test it against the
fleet.**

**It is the strongest confirmation yet that this fleet's wins are bugs — and this
one was a CORRECTNESS bug, not a speed one, present upstream.**

## The bug

`fptosi` and `fptoui` are **poison on overflow**, so shared emulator code bolts a
correction on by hand — **and that correction is written for x86.**

**On x86**, `CVTTPS2DQ` returns `0x80000000` on overflow, so the code **XORs it
to `0x7fffffff`.**

**On ARM64**, `FCVTZS` **already returns `0x7fffffff`** — and **the same XOR
turns it into `0x80000000`.**

> **SPU `CFLTS` was incorrect on ARM64. Any value at or above 2^31 produced the
> wrong result, upstream included.**

**Its sibling was merely wasteful.** `CFLTU` was correct but redundant: `FCVTZU`
already clamps negatives to zero and saturates at 2^32, so the select and the
sign mask were dead work.

**The fix was one intrinsic.** `llvm.fptosi.sat` and `llvm.fptoui.sat` lower to a
single `FCVTZS`/`FCVTZU` — **fixing the correctness bug and shortening both from
four instructions to one.** Only the ARM64 branches changed, and a test pins it.

## The class, stated by the fork that found it

> **on a target whose hardware semantics are *stronger* than the IR's, a portable
> correction can be worse than nothing. Grep for the other places shared code
> compensates for x86 quirks before assuming they are neutral.**

**This is the most dangerous x86-detour form found so far.** The others cost
speed. **This one changes results**, silently, on values a test suite may never
reach.

## The fleet, checked — and ARMSX2 shows the correct method

Searched every fork's own source, submodules included, for x86 float-convert
corrections. Counts: ARMSX2 12, Vita3K 10, xenia 3, melonDS 2, eden 2, azahar 1,
Cemu 0.

**ARMSX2's hits are not the bug.** Its ARM64 VU path uses `mVUclamp3`/`mVUclamp4`,
which are **the PS2's own clamping semantics** — guest behaviour, correct to
keep.

**And its float-to-int lowering is the positive example of this entire class.**
`pcsx2/arm64/iCOP2-arm64.cpp:2346`:

> ARM64 NEON `Fcvtzs` returns 0 for a NaN input, but the PS2 — like `mVU_FTOIx`
> and the interpreter — saturates NaN to a sign-based INT_MAX/INT_MIN.
> **Finite overflow and ±Inf already saturate correctly in `Fcvtzs`; only NaN
> lanes need the fixup.**

> **That is the method: enumerate what the host already does correctly, and fix
> only the delta.** rpcsx discovered it by shipping the alternative.

**It even records why its constant is materialised differently** — `MVNI` rather
than loading `mVUglob.absclip`, because the COP2 macro path does not set up that
base register. **That is the level of care this class needs.**

## What to take

- **Add the class to `tools/bug_class_sweep.py`**, with ARMSX2's comment as the
  signature of correct code, exactly as the x86-only-fast-path class carries its
  correct-code signature.
- **The rule for the shared layer**: when a guest operation is implemented for
  more than one host, **the ARM64 branch must state which parts of the guest
  semantics the hardware already provides.** ARMSX2's comment does; rpcsx's did
  not, and that is where the bug lived.
- **`CLAUDE.md`'s guest-FP section should carry this**, because it currently
  discusses FP *modes* — rounding and denormals — and not FP *conversion
  semantics*, which is where the correctness risk actually was.

## Limits

- **No fleet instance of the bug was found outside rpcsx.** The search covered
  seven forks and one vocabulary — `0x80000000` near saturation words, `cvttps`,
  `fptosi`, `fptoui`, `_mm_cvttps` — and **a correction spelled another way would
  not appear.**
- **Not reproduced.** rpcsx's analysis is quoted; ARMSX2's comment is read.
- **The counts above are files, not instances**, and most are genuine guest
  semantics rather than x86 corrections.

## Sources

- rpcsx `docs/arm64/codegen.md`, "Where the wins actually were"
- ARMSX2 `pcsx2/arm64/iCOP2-arm64.cpp`, `pcsx2/arm64/microVU_Upper-arm64.inl`
