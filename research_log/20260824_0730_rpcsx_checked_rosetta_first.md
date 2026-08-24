# rpcsx checked every Rosetta technique before we did, and found four already present

**Goal: mine `rpcsx/docs/arm64/`, a 23-document ARM64 knowledge base this repo
cites once.**

**Its `rosetta-lessons.md` is the assessment this project made on 2026-08-23,
made earlier, on the same device, by a fork that then checked each technique
against its own code.**

> **Four Rosetta techniques checked, four already present. The one genuine
> difference — TSO — is a problem this emulator does not have.**

## It confirms today's TSO scoping, and adds the fact that makes it stronger

This repo concluded on 2026-08-24 that Rosetta's AOT pillar transfers and its
memory-ordering pillar does not, because PowerPC and ARM are both weakly ordered.
**rpcsx reached the same conclusion and states a stronger reason:**

> AArch64 is **stronger** than PowerPC in one architectural respect that matters:
> since Armv8 it guarantees **multi-copy atomicity**, which PowerPC does not. Any
> ordering a correct PPC program relies on is therefore already provided. **The
> translator never has to manufacture ordering the target lacks — the position
> Rosetta is permanently in.**

**"Both are weak" was the right answer for the wrong reason.** The real point is
that **the translation direction is favourable**, so the mapping is a table:

| PowerPC | AArch64 |
| --- | --- |
| `lwarx` / `stwcx.` | `ldaxr` / `stlxr` |
| `lwsync` | `dmb ishld` |
| `sync` | `dmb ish` |
| `isync` | `isb` |

**Which is exactly the instruction mix I counted in xenia's emitter today** — 4
`dmb`, 15 `ldaxr`/`stlxr` pairs, zero `ldar`/`stlr`. **Two forks, one conclusion,
reached independently.**

Its instruction to itself: **"do not go looking for a TSO problem here, and do
not add barriers defensively."**

## The checklist, and it is nearly all green

| Rosetta technique | rpcs3's status |
| --- | --- |
| Hardware TSO | **not applicable** — and the SoC has no such bit anyway |
| **Lazy flag materialisation** | **done, via SSA locals + LLVM dead-code elimination** |
| **AOT translation with an on-disk cache** | **done** — the PPU and SPU native caches |
| **Guest-to-host register pinning** | **done, by LLVM's allocator over SSA values** |
| Indirect-branch dispatch table | done — block linking and the dispatch table |
| FP denormal and rounding fixup | done — the Thor profile sets DAZ and FTZ |
| Self-modifying code, i-cache invalidation | done |

**Two of those rows are things this project spent 2026-08-23 and 2026-08-24
arguing about as though they were open.**

## The mechanism that unifies residency and lazy flags

**This is the part worth taking, and it changes an argument in
`shared_layer/TRANSLATION.md`.**

rpcs3 does not implement lazy flags. It arranges for them to happen:

- CR fields are written **only when the record bit is set** — an `if (op.oe)`
  guard on every `VCMP*`.
- When written, the bits live in **SSA locals** through `RegLoad`/`RegStore`, and
  are flushed **only when something can observe them**.

> Because the bits are SSA values inside a function, **LLVM's dead code
> elimination does the laziness, and does it better than a hand-written scheme**:
> a CR field computed and then overwritten before any branch reads it disappears
> entirely, **including transitively across inlined blocks**. **Rosetta has to
> implement that analysis; here it falls out of emitting IR.**

**Now put that beside xenia's residency result from this morning.** XenonRecomp
gets register residency by making guest registers **C locals**, so the host
compiler keeps them in registers. rpcs3 gets lazy flags by making condition bits
**SSA values**, so the host compiler deletes the dead ones.

> **Residency and lazy flags are the same technique: express guest state as
> values the host compiler can see, instead of as memory it must assume aliases.**

**Both of this project's two biggest CPU levers are one principle**, and neither
document said so.

### And it is an argument FOR an IR that TRANSLATION.md does not have

`TRANSLATION.md` and `CLAUDE.md` treat an IR mainly as portability machinery —
the DELETE candidate — with correctness verification as the one argument in its
favour. **This is a second argument, and it is a performance one:** an IR that
carries guest state as SSA values gets residency and laziness **from the host
compiler's existing passes**, at no design cost.

**The counterweight is in the same document**, which is why it is credible:

> The JIT targets `cortex-a78`, which is Armv8.2 [...] it omits `v8.3a`/`v8.4a`
> and therefore **`flagm`**. **`RMIF`/`SETF8`/`SETF16` would be the natural way to
> move PPC condition bits into and out of NZCV.** **It is unclear that this is
> reachable from LLVM IR at all**, since portable IR has no "set the flags"
> operation and the backend synthesises flags from `icmp`.

**So the IR gives you the optimiser and takes away the instruction.** That is the
honest trade, stated by the fork that lives with it.

## The rule two forks reached independently

rpcsx's closing line:

> the interesting question is almost never "is there a better instruction", it is
> **"is this code executed, and how much data goes through it"**.

**xenia reached the same rule from the other end today**: its `EOR3` fusion was
right on both hardware axes and died because a counter found **0 of 1 V128 XORs
were fusable**, and its note is **measure applicability before building the
transform.**

**Two forks, two routes, one rule.** It belongs in the measurement discipline,
and this repo has now recorded it twice from two sources.

## One more, checked and closed by them

**The SPU's `CFLTS`/`CFLTU` scale-and-convert is already optimal on ARM64.** The
translator emits `fptosi_sat` and drops the x86 correction sequence, and the
explicit `a * 2^(173-i8)` multiply **folds into the convert**:

```
fptosi.sat(x * 2^8)  ->  fcvtzs v0.4s, v0.4s, #8    ; one instruction
```

**The multiply, the constant and the saturation collapse into one fixed-point
`FCVTZS`.** Recorded because it is the shape of a lever this repo would otherwise
propose: it is already done, by the backend, for free.

## Limits

- **rpcsx is GPL-2.0-only. Every item here is an idea, never code.**
- **None of it is reproduced by me.** It is one fork's audit of its own tree.
- **It is PS3-specific in its particulars** — PPU, SPU, `PPUTranslator`. **The
  principle transfers to any PowerPC guest**, which means xenia and Cemu; the
  MIPS and ARM guests are further away.
- **The `flagm` question is explicitly left open there**, and remains open here.
- **The remaining 22 documents in `docs/arm64/` are unread**, including
  `lv2-ppu-spin.md`, whose title claims **74% of all cycles** are a nop-spin.

## Sources

- rpcsx `docs/arm64/rosetta-lessons.md`
- rpcsx `docs/arm64/x86-isms-sweep.md`, `aes.md`, `codegen.md` — referenced there
- xenia `docs/research/20260626-static-recomp-residency-eval.md` for the
  localization half
