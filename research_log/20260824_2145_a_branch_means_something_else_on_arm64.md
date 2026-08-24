# A branch offset means something else on ARM64, its reach is sixteen times shorter, and both facts land on the persisted code cache

**Goal: read `codegen.md`'s branch-offset section, and check what its two facts
imply for this project's `PERSIST` operation.**

**No device. Source reading, one arithmetic check, and one constant read from
xenia's shared code-cache header.**

## Fact 1: the offset is measured from a different place

> **x86 `JMP rel32` is relative to the END of the instruction. AArch64 `B` is
> relative to the ADDRESS OF THE BRANCH ITSELF.**

**A hand-written code generator ported across is wrong by exactly one
instruction**, and rpcsx names the failure mode precisely:

> *"it will be wrong in the quietest possible way: **it jumps to a valid
> instruction, four bytes early.**"*

**This is an eighth form of the x86 detour**, and it belongs to the family this
repo has already flagged as having no instrument: **a wrong result and a normal
frame time.** The others are the `fptosi` correction that became a corruption,
the `sse2neon` NaN divergences, and Cemu's three-way `psq_st` disagreement.

**The instance is in rpcsx's PPU symbol trampoline**, in an AArch64 block behind
`#elif 0`. It computed `full_sample - (code + 4)` — **correct for x86** — while
`write_le` had already advanced `code` to the address the `B` will occupy, so the
result branched to `full_sample - 4`.

### The method, which is the transferable part

> *"Verified against the assembler rather than argued, **which is the cheap way
> to settle any encoding question**."*

A reference `B` at `0x8` targeting `0x14` encodes `0x14000003`, so `imm26` is
`(0x14 - 0x8) / 4` — **self-relative, proven from the bits.**

**This project has the same rule for compiler flags** — *disassemble and count* —
and did not have it for **encodings**. Same shape, no device, exact answer.

## Fact 2: `B` reaches sixteen times less far than `JMP rel32`

| | direct branch reach |
| --- | --- |
| x86-64 `JMP rel32` | **+-2 GB** |
| **AArch64 `B`** | **+-128 MB** |

> **An ARM concern with no x86 analogue**, in rpcsx's words. A code generator
> that never had to think about branch range on x86 has to on ARM64.

## What that means for a code cache, checked in the fleet

**xenia reserves a generated-code region of `kGeneratedCodeSize = 0x0FFFFFFF`** —
**268,435,455 bytes, one byte under 256 MiB** — and it is declared in
`src/xenia/cpu/backend/code_cache_base.h`, the **shared base both backends
inherit**, not in the x64 backend.

> **That region is exactly twice `B`'s reach.** A direct branch cannot span it.

**xenia is not exposed, and the reason is worth recording rather than assuming.**
Read in `a64_emitter.cc`: the `b(label)` sites are **intra-function** —
`epilog_label_`, `done`, `after`, `tail_jump` — and one function's output is
bounded by **`kMaxCodeSize = 1_MiB`**, four orders of magnitude inside the reach.
**Cross-function transfer is register-indirect**: `blr(x9)`, and the guest
trampoline materialises its target with a `MOVZ`/`MOVK` chain before branching
through a register.

**So the 256 MiB region forces indirect calls, and it gets them.** That is a real
cost — an indirect branch predicts worse than a direct one — **paid to buy a code
region larger than a direct branch can address.**

## And this is a constraint on `PERSIST` that this repo had not recorded

**`CLAUDE.md` records ARMSX2's `.vuprog` cache as the proof that emitted code can
be persisted, citing its "placement-relative fixup table that makes the emitted
vixl output relocatable". Reading the header shows the fixup table is the SMALLER
half of the mechanism.**

> *"**Constant-VA arena layout** + a placement-relative fixup table make the
> persisted vixl output relocatable (**absolute `armEmitCall` /
> `armMoveAddressToReg` targets are patched on load**)."*

**The arena is mapped at a CONSTANT virtual address, so the code's internal
layout is identical across runs and every internal relative branch stays valid
with no fixup at all.** The fixup table exists for the **absolute** targets —
host function addresses, which ASLR moves between processes.

**That is the design rule, and it is stronger than "keep a fixup table":**

> **Pin the arena to a constant virtual address, and keep it inside `B`'s
> +-128 MB reach. Then internal branches survive a reload untouched, and
> relocation only has to patch the host addresses that actually moved.**

**Without the constant VA, relocation is not merely more work — it can be
impossible**, because a fixup can rewrite an offset only if the new offset is
still encodable. **A branch in range at emit time can be out of range at reload
time**, and that is a failure a fixup table cannot repair.

**And the size matters as well as the base.** xenia's AOT object cache was
measured at **111 MB on disk** in an earlier finding. Nothing says that is one
contiguous executable mapping, and disk size is not arena size — **but 111 MB and
128 MB are the same order of magnitude, which is close enough that the shared
layer should state its arena budget rather than discover it.**

## A discipline rule from the same section

rpcsx fixed the offset arithmetic and **left the block disabled**, deliberately,
because enabling it needs three integration facts it cannot establish without
booting a game — including the i-cache maintenance this device requires.

> *"Correct arithmetic is not the same as a working code generator. **Fixing the
> bug now means whoever enables it is debugging one thing instead of two.**"*

**Fix a latent bug in disabled code rather than leaving it for whoever enables
the feature.** That is cheap, and it is the opposite of the usual instinct to
leave dead code alone.

## And it reinforces a fact already measured here

The third integration requirement is **i-cache maintenance**, which this repo
recorded from the same fork: **this device reports `CTR_EL0` `IDC=1, DIC=0`**, so
instruction-cache invalidation is genuinely required. **Any hand-written codegen
and any persisted code cache inherits it** — the code is written as data and then
executed.

## Limits

- **The trampoline defect is rpcsx's, in rpcsx's code, and rpcsx is out of the
  packed binary.** What transfers is the semantic difference and the range.
- **The xenia reading is a sample, not an audit.** The `b(` sites in
  `a64_emitter.cc` were listed and the cross-function path was checked at one
  call site; the whole emitter was not read. **No claim that xenia emits no
  long-range direct branch anywhere** — only that its structure does not require
  one and the sampled sites do not.
- **The ARMSX2 constant-VA claim is from its own header comment.** Nothing was
  built or run.
- **Nothing here is measured.** The indirect-branch cost is asserted from the
  architecture, not timed.

## Sources

- rpcsx `docs/arm64/codegen.md:355-395`
- xenia `src/xenia/cpu/backend/code_cache_base.h:294`,
  `src/xenia/cpu/backend/a64/a64_emitter.cc`, `a64_backend.cc:3242-3292`
- ARMSX2 `pcsx2/arm64/microVU_ProgCache-arm64.h:20-30`
- `research_log/20260825_0610_the_thor_requires_icache_invalidation_and_apple_does_not.md`
