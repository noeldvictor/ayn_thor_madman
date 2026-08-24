# The A710 lane-assembly stall survives, with a verified fix — and it is the first manual lead that has

**Goal: check `CLAUDE.md`'s A710 lane-assembly lead against rpcsx's `codegen.md`.**

**It survives. That makes it the first of fifteen manual-derived predictions in
this fleet not to be refuted, and the reason it survives is the method: it was
verified by disassembly rather than reasoned about.**

## The hazard, made precise from two guide sections

`CLAUDE.md` quotes only §4.2. **rpcsx pairs it with §4.11, which gives the exact
conditions:**

**§4.2, dispatch stall:**

> In the event of a V-pipeline µOP containing more than 1 quad-word register
> source, a portion or all of which was previously written as one or multiple
> single words, that µOP will stall in dispatch for **three cycles**.

**§4.11 gives the three conditions that must all hold:**

- the producer writes an **S-register**, **not** a `D[x]` scalar
- the consumer reads an **overlapping Q-register**
- the consumer is an **FP/ASIMD µOP**, not a store or a `MOV`

> **The shape to avoid: assemble a vector from 32-bit pieces, then feed it into a
> multi-source vector operation.**

## `_mm_set_epi32` produces it verbatim

**Verified at `-O2 -mcpu=cortex-a710`:**

```
fmov  s1, w0                 <- S-register write        (4.11 producer)
mov   v1.s[1], w1            <- single-word lane writes (4.2)
mov   v1.s[2], w2
mov   v1.s[3], w3
add   v0.4s, v1.4s, v0.4s    <- two quad-word sources, FP/ASIMD consumer
```

**All three of §4.11's conditions hold, and §4.2's three-cycle stall applies on
top.**

**And on x86 `_mm_set_epi32` is an unremarkable way to pack four values.** This
is the x86 detour again, in a fourth form: **not an instruction, not a register
model, not a timing constant — an idiom that is free on one machine and stalls on
the other.**

## The obvious workaround fails, and the guide's own fix works

**Writing the four values to a stack array and loading the vector back compiles
to byte-identical code** — clang folds the round-trip away and rebuilds it lane by
lane. **A reasoned fix would have been believed and would have done nothing.**

**§4.11 exempts `D[x]` scalar writes**, so packing into 64-bit halves first
gives:

```
fmov  d1, x8                 <- D-register write, exempt
mov   v1.d[1], x9            <- D[x] scalar write, exempt
add   v0.4s, v0.4s, v1.4s
```

**Two writes instead of four, and no hazard.**

## Applicability, counted before anyone builds anything

Per this project's own rule. Sites using `_mm_set_epi32`, `_mm_setr_epi32`,
`vsetq_lane_s32` or `vsetq_lane_u32`, own source, submodules included:

| Fork | sse2neon files | Lane-assembly sites |
| --- | --- | --- |
| **rpcsx** | **13** | **18** |
| **ARMSX2** | 1 | **18** |
| eden | 1 | 5 |
| xenia | 0 | 2 |
| Vita3K | 0 | 1 |
| Cemu | 5 | 0 |
| azahar, melonDS | 0 | 0 |

**ARMSX2 has as many sites as the fork that found the hazard**, and it is the
seed of the shared layer and a Tier 1 target.

**Bound it honestly: 18 sites is small**, and **whether any of them is on a hot
path is unknown.** The rule says count, then measure, then build. **The count is
done and the measurement is not.**

## Why this one survived

**Fourteen manual-derived predictions in this fleet have been measured and
refuted** — the A510 shared VPU, `EOR3` fusion, `TBL2`-for-`TBX2`, `ISB`-for-
`yield`, the native render rearch, bindless, `LOAD_OP_CLEAR`, and more.

**This one is different in method, not in luck.** Every step was checked against
emitted code:

- the hazard was reproduced from a compiler, not inferred from the text
- **the obvious mitigation was tried and found to compile identically**
- the guide's mitigation was checked to actually change the instructions

> **A manual gives a hypothesis. A disassembly gives a finding.** The fourteen
> that failed were mostly the former.

## Limits

- **No timing.** Three cycles is the guide's number, not a measurement, and the
  end-to-end cost depends on whether dispatch is the bottleneck at that point.
- **A710 and A715 are four of the eight cores**, and the X3 guide does not carry
  these sections — so the hazard does not apply on the prime core.
- **The site count is a grep**, and a site inside a cold path costs nothing.
- **Not reproduced by me**; the disassembly is rpcsx's.

## Sources

- rpcsx `docs/arm64/codegen.md`
- Arm Cortex-A710 Software Optimization Guide, §4.2 and §4.11, vendored in
  rpcsx `docs/hardware/`
- `hardware_ref/thor/cpu/CORE_COMPARISON.md` for this repo's version of the claim
