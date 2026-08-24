# Register localization is the residency win, proven device-free, and the in-JIT route crashes

**Goal: continue mining the fleet's negative-verdict documents.**

**The second one opened is not a negative at all. `tools/fleet_docs_index.py`
flagged `20260626-static-recomp-residency-eval.md` as `DEAD` because the document
contains the words "⇒ DEAD" inside its own pass-bar definition.** That is the
false positive the tool's own docstring warns about, and it is recorded here as
the first real instance.

**The document is a GO recommendation, and it is the strongest material found so
far for [`shared_layer/TRANSLATION.md`](../shared_layer/TRANSLATION.md).**

## The thesis, confirmed by disassembly with no device

`TRANSLATION.md` argues that speed in a recompiler is instruction inflation, that
inflation is caused by the register mapping, and that **residency — getting guest
registers into host registers at all — is the large lever.**

**xenia tested exactly that, device-free, by compiling two versions of the same
guest hot loop and disassembling the output.** `scratch/thor-debug/residency_killtest.c`,
aarch64-gcc `-O2`, a load-accumulate-advance-count loop with an opaque
`mem_read32(ctx, ...)` call:

| Form | Guest-register memory ops in the loop body |
| --- | --- |
| **guest regs as context-struct fields** | **~4 per iteration** — `ldp`, `ldr`, two `stp` |
| **guest regs localized to C locals** | **0** — they live in `x24`, `x21`, `x23` across the loop **and across the call** |

**The opaque call is the point.** It defeats alias analysis, so the struct form
must reload and spill around it. **The localized form does not.**

> **Loaded once at entry, stored once at exit. That is residency, and a host
> compiler does it for free once the registers are locals.**

## The distinction that matters: static recompilation is not the win. Localization is.

**Two static recompilers, one difference:**

- **DolRecomp** (GameCube and Wii) emits guest registers as **context-struct
  fields**, `ctx->gpr[%u]`. **It does not get residency** — the host compiler
  round-trips them exactly as a JIT does.
- **XenonRecomp** (Xbox 360, the same guest as xenia) **localizes registers to C
  locals**, and gets it.

**So "go static" is not the lever.** Something calling itself a static
recompiler can pay the same tax. **`TRANSLATION.md` should say localization, not
static recompilation.**

## The in-JIT alternative is built, host-correct, and crashes

`arm64_register_cache_inherit` — cross-block register-residency inheritance
inside the JIT — is **host-correct with 320 assertions** and **crashes on
device**: `SIGBUS`, fault address `0x1fffffff8`, from a **stale inherited
register on a back edge**.

**It has cost 12 or more device fires across sessions**, and xenia calls it the
design document's highest-risk unit.

**And the crash mechanism was traced to the same hot function the win was
measured on.** Its four tight loops are all one shape — a sorted linked-list or
tree traversal:

```
loop: cmplw cr6, r10, r11 ; beq exit                 // end-of-list check
      lhz r8, -8(r11) ; cmplw cr6, r9, r8 ; ble exit // 16-bit key compare
      lwz r11, 0(r11)                                // r11 = node->next
      stw r11, 0x84(r31)                             // update iterator field
      b loop
```

**The loop is three basic blocks because of its two conditional exits**, so the
node pointer, the search key and the bound round-trip through the context **every
block, every iteration**. **That is the tax, stated concretely.** And the in-JIT
inheritance binds `r11` stale across those blocks, giving `r11 = 0xfffffff8` and
the observed fault.

> **The same code shape is where localization pays most and where the in-JIT
> retrofit breaks.**

**One more useful fact from that read: flag cost is about zero here.** Every
`cmplw` is consumed immediately by its branch, so it lowers to a plain
conditional with no condition-register materialisation. **The expensive
static-recomp case — flags read far from their compare — does not occur in this
code.**

## What this changes in this repo

**1. `TRANSLATION.md`'s central claim is now supported by a measurement, not only
by literature.** It cited an inflation paper and a fleet-wide static IR-op count.
**This is a disassembly of the same loop in two forms on the target ISA.**

**2. The recommended shape is a targeted hybrid, not whole-program.**
`CLAUDE.md` currently argues that static recompilation's win comes from handing
whole programs to an optimising compiler, which needs per-game decompilation and
symbols, **"a cost this project cannot pay across eight systems."** xenia's
recommendation sidesteps that:

> build a **targeted hybrid AOT** (hot cluster only), NOT whole-program.

**AOT-recompile the hot cluster and JIT everything else, sharing the dispatcher
and lookup table.** That is a far smaller commitment than the objection assumes.

**3. There is a device-free kill test with a stated pass bar**, and it is an
inflation measurement in this repo's own vocabulary:

> **PASS bar: localized AOT is >20-25% fewer host instructions / dramatically
> fewer memory ops than the JIT. <15% ⇒ DEAD (not worth the architecture).**

**A pass bar stated before the run, with a number that can fail.** That is the
discipline this repo asks for, and the test needs one device fire only for the
JIT-side disassembly.

**4. Lazy flags compound with residency and are easier in C than in a JIT.**
xenia calls this likely the single biggest sub-lever. **`TRANSLATION.md` does not
mention lazy flags at all**, and the arXiv inflation work found compare-and-branch
worth roughly 18% of inflation — **the same target from two directions.**

**5. Four hard parts are enumerated**, and they are the real cost: precise flags
(CR0-7, XER, FPSCR bit-exact), indirect control flow through `bctr`/`bcctr`/`blr`
and jump tables, the memory model, and self-modifying code. **DolRecomp punts on
SMC; the suggested answer is detect and fall back to the JIT for SMC pages.**

**6. DolRecomp's PC-switch entry table is the fix for the control-flow case the
in-JIT lever mishandles** — one C function per guest function with
`switch (ctx->pc) { case 0xADDR: goto label_ADDR; ... }`, so every guest PC is a
valid entry point.

## Limits

- **Nothing in the recommendation has been built.** The kill test is proposed,
  not run. The document explicitly says it needs a user go and a device fire.
- **The residency kill test is a synthetic loop**, not the real function. The
  real-function comparison is step 2 of the proposed test and is unrun.
- **This is xenia's guest.** PowerPC with a large register file onto ARM64. The
  argument transfers to Cemu's Espresso and, more weakly, to MIPS guests with
  fewer registers.
- **`SIGBUS` at `0x1fffffff8` was localised to one function**, not root-caused in
  general.
- **The tool's verdict flag was a false positive here.** Its docstring already
  warns that a document discussing a verdict word is indistinguishable from one
  recording it. **First confirmed instance; the warning stays.**

## Sources

- xenia `docs/research/20260626-static-recomp-residency-eval.md`
- xenia `scratch/thor-debug/residency_killtest.c`, quoted from that document
- xenia `src/xenia/cpu/compiler/passes/register_allocation_pass.cc`, named there
- `tools/fleet_docs_index.py`
