# Seven x86 SIMD tricks audited: all seven already optimal or inapplicable, 0.04% of instructions

**Goal: read `rpcsx/docs/arm64/x86-tricks-arm64-answers.md`, the fork's audit of
seven x86 SIMD techniques against what its JIT actually emits.**

**It closes the instruction-repurposing lane, and it does so with a census method
better than the one this repo used.**

## The verdict table

| # | x86 trick | Guest opcode | AArch64 output | Emitted | Verdict |
| --- | --- | --- | --- | --- | --- |
| 1 | `PSADBW` | `ABSDB` | `UABD` | **0** | not reached by this title |
| 2 | SPU `SUMB` | `SUMB` | 2x `UDOT` + `UZP1` | 9 | **already optimal** |
| 3 | `VPDPBUSD` | `SUMB` | the same `UDOT` | 9 | **already optimal** |
| 4 | `VDBPSADBW` | `SUMB` | the same `UDOT` | 9 | no ARM equivalent |
| 5 | `GF2P8AFFINEQB` | `SHUFB`, `GB` family | `USHR`+`TBL`, `SDOT`/`UMMLA` | 5,859 / 57 | no ARM equivalent |
| 6 | SPU `GBB` | `GBB` | `AND`+`UMMLA`+lane move | 26 | **already optimal** |
| 7 | SPU `FCGT` | `FCGT` | 7 ops around an inline-asm `BSL` | 117 | **already optimal** |

> **No item on this list is a candidate. Together the seven lowerings account for
> about 210 of 509,424 emitted instructions — 0.04%.**

**This is the third independent audit of the instruction-repurposing lane in this
fleet, and the third to come back empty.** xenia's `EOR3`/`BCAX` fusion died on
zero candidates; its `TBL2`-for-`TBX2` rewrite measured 0.555 against 0.555; and
now seven candidates at 0.04% of the instruction stream.

## The census method, which is better than reading hits

**`CLAUDE.md` — this repo's and rpcsx's alike — recorded that 1,661 `udot`
instructions prove the dot-product optimisation is taken.**

**The instruction is taken. The operation is not the one anybody thought.**

rpcsx classified **every `udot` in the corpus by the instructions that define its
two vector sources, inside the same function**:

| Source pair | Count | What it actually is |
| --- | --- | --- |
| `cmeq`, `cmeq` | **1,338** | **SPU block verification** — not a guest opcode at all |
| `cmeq`, `movi` all-ones | 326 | the same, odd-count tail |
| `cnt`/`tbl`/`movi`, `movi #1` | **9** | the actual `SUMB` lowering |

**And it cross-checked with a control instruction.** `addv s, v.4s` appears
**615** times, one per verification site, and **615 sites x 2.7 pairs gives
1,664** — which matches. **The attribution is confirmed by an independent count,
not asserted.**

**So the optimisation everyone believed was firing 1,661 times fires 66 times**
across `SUMB`, `GB`, `GBH` and `GBB`.

> **The count was correct and the attribution was not.** The earlier work asked
> **"is this instruction present"** and never **"which lowering emitted it".**

### This is the exact error this repo made, and the fix is better than the one recorded

`CLAUDE.md`'s hardware-repurposing audit corrected itself the same way — eden's
`EOR3` hits turned out to be **guest decoding**, melonDS's `CRC32` an **encoding
table**, Cemu's `SDOT` mentions **comments**. **The recorded lesson was "read the
hits".**

**Reading the hits is not enough for generated code**, because a JIT emits the
same instruction from many lowerings and there is no comment to read. **The
method that works is:**

1. **Classify each instruction by the provenance of its operands** — what defined
   the registers it consumes, in the same function.
2. **Cross-check with a control instruction** that should appear once per site,
   and confirm the arithmetic.

**That is a real technique this repo did not have**, and it is device-free.

## What the document does not claim

**Its own limits, kept:**

- **One title's compiled corpus.** `PSADBW`'s guest opcode was never reached, so
  row 1 says nothing about the lowering's quality.
- **"Already optimal" means the emitted sequence is the right one**, not that the
  guest operation is cheap.
- **Row 5's 5,859 `tbl` is a large surface** and the verdict is "no ARM
  equivalent", not "nothing to do" — that is a shuffle path, and this repo's
  own swizzle question sits beside it.

## What to change here

- **`CLAUDE.md`'s hardware-repurposing section should record the third empty
  audit**, and the 0.04% figure, because it is the strongest single number
  against that lane.
- **Add the operand-provenance census method to the measurement discipline.**
  This repo already trusts disassembly counting — it settled the target-features
  question that way — **and this is how to make such a count mean something.**
- **The lane is not "unproven" any more.** Three audits, three empties. **Treat a
  new instruction-repurposing proposal as needing an applicability count before
  any build**, which is the rule xenia's `EOR3` result already established.

## Limits

- **Not reproduced by me.** This is rpcsx's audit of rpcsx's JIT.
- **PS3 is out of the packed binary and rpcsx is GPL-2.0-only**, so the method
  transfers and the code does not.
- **SPU-specific.** The guest is a vector machine with unusual opcodes; a MIPS or
  ARM guest emits a different instruction mix, and the 0.04% is that corpus's.

## Sources

- rpcsx `docs/arm64/x86-tricks-arm64-answers.md`, `docs/arm64/jit-emitted-code.md`
- rpcsx `SPULLVMRecompiler.cpp:2172`, `:2201`, `:6476`
