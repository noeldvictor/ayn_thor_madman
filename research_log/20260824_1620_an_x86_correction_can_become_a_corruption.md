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

## The sweep, and two tool bugs it exposed

**Added as a bug class, and the first run was wrong twice.** Both are recorded
because the failures are the reusable part.

**Too broad.** The pattern included bare `0x80000000` and `0x7FFFFFFF`, and a
saturation constant appears everywhere: **2,498 lines in ARMSX2 alone.** **A tool
that returns noise is worse than no tool.** Tightened to the x86 *conversion*
intrinsics only — `cvttps`, `_mm_cvtt`, `fptosi`, `fptoui` — and the counts became
readable.

**A filter that missed a whole tree.** Vita3K then showed **75 lines in 9 files**,
all of them **capstone's x86 opcode constant tables**. The vendored filter carried
`externals` and **Vita3K's directory is `external/`, singular.** Fixed to
`externals?/`, and Vita3K drops to **zero** — correctly.

**The corrected sweep:**

| Fork | Lines | Where they are |
| --- | --- | --- |
| ARMSX2 | 52 | **x86 emitter, x86 recompiler and the software renderer — none in `pcsx2/arm64/`** |
| rpcsx | 51 | the fork that found the bug |
| xenia | 7 | its `x64` and `llvm` backends |
| melonDS | 6 | — |
| eden | 3 | — |
| azahar | 2 | — |
| **Cemu, Vita3K, GameThor** | **0** | — |

**ARMSX2 is verified clean, and for a structural reason.** It wrote **dedicated
ARM64 files** rather than shimming x86 intrinsics: `GSVector4_arm64.h`,
`GSVector4i_arm64.h`, and `GSDrawScanlineCodeGenerator.arm64.h` for the software
renderer. **The x86 intrinsics in its tree are in paths that only x86 builds
compile.**

> **That is the structural answer to this whole class: a separate ARM64 file
> cannot inherit an x86 correction by accident.** A shared file behind a shim
> can.

## The same divergence runs the other way, through `sse2neon`, undocumented

**rpcsx's bug was an x86 correction ADDED where ARM did not need it. The shim
does the opposite: it DROPS the x86 semantics entirely.**

`sse2neon.h`:

```c
FORCE_INLINE __m128i _mm_cvttps_epi32(__m128 a)
{
    return vreinterpretq_m128i_s32(vcvtq_s32_f32(vreinterpretq_f32_m128(a)));
}
```

**Plain `vcvtq_s32_f32`.** So it returns **ARM's saturating result** —
`0x7FFFFFFF` on overflow, `0` on NaN — where the x86 intrinsic it is named after
returns **`0x80000000` for both.**

**And the header does not say so.** Its comment is *"Converts the four
single-precision, floating-point values of a to signed 32-bit integer values
using truncate"* with an MSDN link, **and no note that the edge cases differ.**

> **Both failures have one root: a boundary that claims to be x86 and is not, in
> exactly the cases nobody tests.**

**Exposure in this fleet**, own source, sse2neon users, shim itself excluded:

| Fork | `_mm_cvttps_epi32` call sites |
| --- | --- |
| **rpcsx** | **5** |
| ARMSX2 | 1, **and it never compiles on ARM64** — see below |
| Cemu, eden | 0 |

**ARMSX2 is clean a second time, and the mechanism is worth copying.** Its
conversion branches on architecture **in the same function**, with the ARM64 side
written out and a comment about rounding mode:

```c
#if defined(ARCH_X86)
    m = truncate ? _mm_cvttps_epi32(v) : _mm_cvtps_epi32(v);
#elif defined(ARCH_ARM64)
    // GS thread uses default (nearest) rounding.
    v4s = truncate ? vcvtq_s32_f32(v.v4s) : vreinterpretq_s32_u32(vcvtnq_u32_f32(v.v4s));
#endif
```

**It reaches the same `vcvtq_s32_f32` the shim would have — deliberately and
visibly.** That is the whole difference: **an informed choice at the point of
use, rather than a silent substitution three headers away.**

> **So the design rule is sharper than "prefer a separate ARM64 file". It is:
> make the architecture choice visible where the operation is written.** ARMSX2
> does it with an `#if` in the same function; a separate file does it by
> construction; a shim does neither.

## CONFIRMED AND ENLARGED: the fork calls this its most productive heuristic

**Found later the same day by reading rpcsx's `docs/arm64/ledger.md`, which its
`AGENTS.md` names as the audit ledger — the index I should have opened first.**

> **Auditing x86 *corrections* has high yield where auditing opcodes has low
> yield** — **every real defect in this codebase was shared code compensating for
> an x86 quirk, never LLVM picking a bad instruction from a clean description.**

**Six defects, not the one I found:** `CFLTS`, the **`FCTIW` family**,
**`VPKUHUS`**, **`bswap.i128`**, **`mov_rdata`** and **`VMSUMSHS`.**

**And it has a mechanical tell**, which is much sharper than the conversion-
intrinsic list I swept with:

> **an XOR against a sign-extended comparison** — the shape a saturation fix-up
> takes.

**Swept across both translators: three sites, all correct.** Two are `CFLTS`
inside the `#else` of an `#ifdef ARCH_ARM64`, **the fix working as intended** —
the correction survives for x86 while AArch64 takes hardware saturation. **Finding
those in a grep is the expected result, not a regression.**

**The third is the distinction this whole class turns on.**
`PPUTranslator.cpp:1708`, `VMSUMSHS`: **not an x86 workaround — it implements
PowerPC's own saturation of the `0x80000000` product.** **The "correction" shape
is the guest architecture's semantics**, which is why this lens **needs reading
rather than pattern-matching** — the same conclusion ARMSX2's `iCOP2-arm64.cpp`
comment reaches from the other side.

**The exhaustion is bounded and stated.** The lens is exhausted **for that
shape**. A correction spelled as a `select`, a clamp before a conversion, or a
literal limit would not match — **and the `select` forms were checked by hand in
the same pass** and turned out to be `FREST`/`FRSQEST` exponent handling,
**algorithm rather than compensation.**

**Its reason for recording a negative result is this repo's own philosophy,
written by another fork:**

> the alternative is **re-running the most productive heuristic in this document
> every time someone looks for work, and concluding from silence that they
> searched badly.**

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
