# A first-class IR operation measured 3x to 15x, and it is the positive form of the x86-detour argument

**Goal: finish azahar's remaining ARM64 invariants. One is the largest measured
ARM64 speed-up I have found in the fleet's own documents — a bound on what I have
read, not a census — and it corrects a framing I wrote earlier today.**

**Measured by that fork on the physical Thor. No device used here.**

## The result

**A32/A64 vector rounding shift-right narrowing.** The frontend used to lower it
as a **shift / broadcast / AND / equal / subtract / narrow DAG** — six-plus
operations, overflow-safe, portable. azahar replaced that with a **first-class IR
operation** that ARM64 emits as **one instruction**: `RSHRN`, `SQRSHRN`,
`SQRSHRUN` or `UQRSHRN`.

| Core | Speed-up |
| --- | --- |
| **A510** | **13.13x - 14.81x** |
| A715 | 2.81x - 3.54x |
| A710 | 3.23x - 3.59x |
| X3 | 3.51x - 3.96x |

> **Three to four times on every big core, and thirteen to fifteen on the little
> ones.**

## Why this is the same argument as the x86 detour, pointed the right way

**The DAG was not wrong. It was PORTABLE.** azahar's own instruction beside the
change says exactly why it existed:

> **"x64 and RISC-V must polyfill the first-class operation back into that
> overflow-safe DAG."**

**Those hosts have no single instruction for the guest operation, so the IR
decomposed it for everybody — including the host that does.**

**That is this project's central CPU thesis, measured.** `CLAUDE.md` says
*"translate the console's CPU and GPU straight to ARM64; do not carry a design
that was correct for a 16-register machine"*. **Here is the same disease in a
different organ**: not the register model, not flag handling, not a timing
constant, but **an IR that destroys a guest operation the host implements
directly.**

> **The structural rule, and it is the important half: the IR should carry the
> GUEST operation at high level, and each backend lowers it. The portable DAG is
> the FALLBACK, not the representation.**

**Inverting that is what costs 3x to 15x.**

## And it corrects what I wrote earlier today

**Earlier I recorded four ARM64 fusions azahar built, measured and rejected**, and
framed the lesson as *"fusion trades instruction count for dependency depth, and
on a machine with spare issue width that is a bad trade"*.

**That framing is right about those four and would have rejected this one, which
wins 3x to 15x.** The distinction is not fusion against no fusion:

| | The four rejected | **This one** |
| --- | --- | --- |
| What it replaced | a **2-instruction sequence** | **a six-plus operation DAG** |
| What it emitted | 1 instruction of the same class | **the instruction that IS the guest operation** |
| Where the sequence came from | a reasonable ARM64 lowering | **a polyfill for hosts without the instruction** |
| Result | **0.66x - 0.99x** | **3.5x - 14.8x** |

> **The question is not "can these two instructions be one". It is "does the host
> have an instruction that IS this guest operation, and is the IR preventing the
> backend from using it".**
>
> **The first is a peephole and the fleet's record says it usually loses. The
> second is the DELETE operation and it measured 3x to 15x.**

**Both rules stand. They apply to different things, and I had only one of them.**

## The A510 is the discriminator AGAIN — and this time favourably

**Eight results now, and the A510 is the outlier in all eight.** Seven were
penalties for wide or fused instructions. **This one is the largest reward.**

**It is consistent.** This repo already records that the A510 runs **Q-form
128-bit NEON at throughput 1**, so a six-operation DAG costs it proportionally
far more than it costs a core that can issue in parallel. **Removing five
operations therefore helps it most.**

> **The A510 is not "the core that hates vector work". It is the core with the
> least slack**, so it punishes added operations hardest and rewards removed ones
> hardest. **Both directions are the same fact.**

## The second finding: structured loads and stores are a trap on this part

**azahar rejects `LD2`/`LD4`/`ST4` twice**, and gives the number:

> **"Cortex-A510 documents Q-form 32-bit `ST4` throughput as `1/50`."**

**One store every fifty cycles.** `LD2`/`LD4`/`ST4` are the textbook ARM64
instructions for interleaving and deinterleaving, and they are the obvious answer
to any channel-separation problem.

**azahar's replacement is not a better instruction. It is a better data
layout.** Its HLE audio mixes are stored **channel-major (`PlanarQuadFrame32`)**,
so the transpose never has to happen; and its gain mixing uses **ordinary paired
Q loads plus `UZP`** rather than a structured load.

> **The strongest answer to an expensive instruction is a layout in which it is
> not needed.**

**And it keeps the sample-major layout for the SAVE-STATE archive only** —
storage format and working format deliberately different, converted at the
boundary. **That is a design worth copying wherever a guest's natural layout
fights the host's.**

## Consequence for a candidate this repo has open

**`CLAUDE.md` names texture swizzle as the best unexploited instruction-
repurposing candidate** — unswizzling is bit deinterleaving and `PMULL` does it
in one instruction. **Its recorded evidence, restated with its own
qualification**: a filename-and-symbol search found large swizzle surfaces in
every fork — xenia 71 files, Cemu 49, Vita3K 47, azahar 45, ARMSX2 33 — against
**zero host-side `PMULL` uses in fork-own code**, and that claim was **already
corrected on 2026-08-24**, because azahar ships `PMULL` inside vendored Crypto++
behind a runtime gate, for genuine crypto. **The candidate survives the
correction; the negative is qualified, not absolute.**

**The obvious NEON approach to any deinterleave is `LD4`/`ST4`, and on the A510
that is `1/50` for the store.** So:

- **The candidate is not weakened** — it argues for `PMULL` precisely because the
  structured-load route is bad here.
- **But the same layout question applies first.** Before optimising an
  unswizzle, ask whether the texture can be **stored** in the layout the sampler
  wants, so the unswizzle happens once at upload rather than repeatedly.
- **And any swizzle work must state its core class.** A swizzle loop that lands
  on an A510 is in the worst place for it.

## Limits

- **Every figure is azahar's, on its Thor.** Nothing reproduced, no device used
  here. The fork's own note says **"keep the claim path-local until a matched
  title and power A/B exists"** — so the 3x-15x is a kernel measurement, **not a
  frame-rate claim.**
- **The speed-ups are for the rounding-narrow family specifically.** They do not
  generalise to other IR operations without the same property: the host having a
  single instruction the IR was decomposing.
- **The `1/50` figure is from the Cortex-A510 manual, quoted by azahar**, not
  measured — and this repo's own record is **thirteen refuted manual-derived
  predictions.** It is a warning, not a result. **azahar treats it as one**, which
  is why it asks for ThinLTO inspection across all four manuals rather than
  acting on the number alone.
- **Whether azahar's four other lowerings would win as first-class IR operations
  is unknown**; they were tested as instruction substitutions.

## Sources

- azahar `AGENTS.md:884-898, 1166-1178`
- `research_log/20260824_2220_four_textbook_arm64_fusions_measured_and_rejected.md`
- `shared_layer/TRANSLATION.md`
