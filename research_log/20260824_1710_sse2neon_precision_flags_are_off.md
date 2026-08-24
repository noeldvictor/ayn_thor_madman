# `sse2neon`'s precision flags default to off, and the four forks carrying it leave them off

**Goal: after finding that `sse2neon`'s `_mm_cvttps_epi32` silently gives ARM
semantics, check whether other intrinsics diverge the same way.**

**They do, and the shim knows: it ships opt-in flags for the divergences and they
default to off.** **Searched all four forks that carry `sse2neon` — Cemu, rpcsx,
ARMSX2 and eden — across their build files and their own source, with the header
itself excluded: none defines either flag.**

## The flags, and their default

`sse2neon.h`:

```c
/* _mm_min|max_ps|ss|pd|sd */
#ifndef SSE2NEON_PRECISE_MINMAX
#define SSE2NEON_PRECISE_MINMAX (0)
#endif
/* _mm_rcp_ps and _mm_div_ps */
#ifndef SSE2NEON_PRECISE_DIV
#define SSE2NEON_PRECISE_DIV (0)
#endif
```

**And the header states the consequence in its own comment**, a few lines above:

> ... x86 SSE. **(e.g. would solve a hole or NaN pixel in the rendering result)**

## What the two branches actually are

```c
FORCE_INLINE __m128 _mm_max_ps(__m128 a, __m128 b)
{
#if SSE2NEON_PRECISE_MINMAX
    return vbslq_f32(vcgtq_f32(_a, _b), _a, _b);   // select a only if a > b
#else
    return vmaxq_f32(_a, _b);                       // ARM's own max
#endif
}
```

**The precise path is a compare-and-select**: take `a` **only when `a > b`
strictly**, otherwise `b`. **A NaN comparison is false, so NaN falls through to
`b`** — which is x86's rule. **`vmaxq_f32` does not do that**, and the shim's
authors wrote the alternative precisely because the two differ.

**`_mm_min_ps` is the mirror image**, with `vcltq_f32`.

## Nobody in this fleet sets either flag

Searched Cemu, rpcsx, ARMSX2 and eden — every fork carrying `sse2neon` — across
their build files and their own source, excluding the header itself:

> **No definition of `SSE2NEON_PRECISE_MINMAX` or `SSE2NEON_PRECISE_DIV` in any
> of them.**

**So every one of them gets `vmaxq_f32`, `vminq_f32` and the fast reciprocal.**

## Why this matters more than the conversion case

**The conversion divergence is undocumented. This one is documented, opt-in, and
still off** — which makes it the "configured and never applies" class **with the
default on the wrong side for this project.**

**And the symptom is stated by the library**: a hole or a NaN pixel in the
rendered result. **That is a visual-correctness bug, not a performance one**, and
it would be attributed to the emulator's renderer rather than to a shim flag.

**It also has a sibling in hand-written code.** xenia's ledger carries an `OPEN`
entry, **`vmaxfp/vminfp a64 NaN fixup`**, on the same problem in its own emitter.
**So guest min/max NaN semantics is unresolved in this fleet in both forms** —
through a shim and by hand.

## What to do

- **For any fork that keeps `sse2neon`, define both flags.** The cost is a
  compare and a select instead of one instruction; **the alternative is
  occasionally wrong pixels.**
- **For the shared layer, prefer neither.** This project has one host ISA, and
  the rule from the conversion finding applies: **make the architecture choice
  visible where the operation is written.** A guest `max` should be written
  against the guest's own NaN rule, not against x86's, and not against ARM's by
  accident.
- **Note which guest actually needs which rule.** x86's `MAXPS` rule is not
  automatically the guest's rule — **it is only relevant because the emulator was
  written on x86.** The PS2, PS3, Xbox 360 and Wii U each define their own.

## Limits

- **Not executed.** The divergence is read from the shim's two branches and its
  own comment; **no NaN case was run on hardware.**
- **The exact ARM `FMAX` NaN behaviour is not characterised here.** It does not
  need to be: the shim ships a separate path specifically to match x86, which is
  itself the statement that they differ.
- **Whether any fork's hot path feeds NaN through `_mm_max_ps` is unmeasured**,
  and this repo's own rule says count applicability before acting.
- **`SSE2NEON_PRECISE_DIV` was not analysed**, only noted as also default-off.

## Sources

- `sse2neon.h` as vendored by rpcsx, lines 61-70 and 2015-2080
- xenia `tools/exp_ledger.py`, entry `vmaxfp/vminfp a64 NaN fixup (RPCS3 item #2)`
