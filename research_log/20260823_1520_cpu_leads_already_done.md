# The three CPU leads are already implemented, in xenia

**Goal: check what the four ARM64-generating forks do with guest FP status and
rounding mode, as [`CLAUDE.md`](../CLAUDE.md) asks.**

No device used. Reading only.

## Result

**All three leads in "Two CPU leads worth chasing" are already implemented, in
xenia, with primary-source citations and partial measurements.** Most are **off
by default with the trade stated**, which is why nothing here was visible from a
feature list.

`CLAUDE.md` says of each that it is "unmeasured and cheap to check". **The check
was already done and written down.** This is the fifth time a "nobody has this"
in this repo has been wrong, and the cause is the same: the capability was
looked for as a feature rather than as a mechanism.

**xenia's `a64_backend.cc` carries 111 tuning flags.**

## Lead 1 — guest FP status may be serialising the machine

**Implemented: `a64_fpcr_single_mode`, plus `a64_fpcr_switch_census` to size it
first.**

xenia's flag text states the root cause better than this repo did:

> the Xenon has **TWO independent FP mode registers** (FPSCR for scalar,
> VSCR.NJ for VMX) and **ARM64 has ONE**.

**That is the real problem, and it is structural.** The two modes differ by
exactly one bit, `FZ`, so every scalar-to-VMX transition rewrites `FPCR`. It
cites the A710 SWOG Table 4-3: an `FPCR` write is **Non-Speculative and
In-Order**, and note 2 says a write that changes the control fields
**introduces a barrier** preventing later instructions from executing.

**The proposed fix is correctness-preserving, not a precision trade.** With `FZ`
never set, VMX denormal flushing falls back to the software path
`PrepareVmxFpSources` already implements for hardware where `FZ` does not flush
inputs. Scalar FP keeps IEEE denormals.

**And it is honest about the trade:** a per-transition barrier against a
per-VMX-op software flush, so **which wins depends on transition density**. Hence
the census flag, to be run first. **Off by default.**

### The other three forks

| Fork | Guest FP mode handling | Verdict |
| --- | --- | --- |
| **ARMSX2** | **bakes the FP environment into the block as an immediate** | **the best answer** |
| **xenia** | switches `FPCR` per mode; census and single-mode flags exist | **analysed, not yet settled** |
| Cemu | sets `FZ` once per guest thread; **guest `FPSCR` writes are unimplemented** — it logs "Unsupported write to FPSCR" | fine by simplification |
| melonDS | `FIELD_FPCR`/`FIELD_FPSR` exist only as encodings in Dolphin's `Arm64Emitter` | nothing to fix |

**ARMSX2's is the design worth propagating.** Three parts, and the third is the
one that makes it safe:

1. **EE `DIV`/`SQRT` bake `FPUFPCR` into the compiled block as an immediate**, so
   the recompiled code never reads `FPCR` at run time.
2. **mVU gates the write** — `mvuNeedsFPCRUpdate` skips it when the value already
   matches.
3. **mVU hashes all four FP environments into its block-cache sentinel**, so a
   block compiled under one environment can never be reused under another.

**Part 3 is the hazard part 1 creates**, and it is easy to miss. Baking a mode
into generated code silently makes that code wrong when the mode changes.

**The ARM64 JIT emits no `MRS`/`MSR FPCR` at all.** Verified: every `FPCR` hit
under ARMSX2's aarch64 paths is inside vendored vixl's constant tables and
simulator, not in an emitter. Its host-side `FPControlRegisterBackup` is
constructed at coarse boundaries only — VM state transitions, thread start, and
**once per `InterpVU1::Execute` call** rather than per instruction.

**PS2 needs three FP environments, not one.** `EmuConfig.Cpu` carries `FPUFPCR`,
`VU0FPCR` and `VU1FPCR` separately, and the aarch64 default is measured live in
`recExecute` as `0x1c00000` — DAZ, FTZ, round-toward-zero.

**So the fleet spans four positions on one question**: bake it (ARMSX2),
switch it and measure the switching (xenia), set it once and ignore the guest
(Cemu), have no guest FP mode at all (melonDS).

## Lead 2 — spill to the vector register file, not to memory

**Implemented: `a64_spill_gprs_to_vector`, and it is measured.**

> Measured on the A710: a fill via `UMOV` is **latency 2 against 4 for `LDR`**,
> and it never touches L1.

It reserves the top N vector registers, using lane 0 of each as one 64-bit slot,
clamped to 8.

**It is off by default, and the reason is the interesting part:**

> the guest is a **128-vector-register machine** already squeezed into 28, so
> every register taken here worsens **VECTOR** pressure to relieve **INTEGER**
> pressure.

**That is the argument this repo did not have.** The optimisation guide's advice
is sound and still may not pay, because the binding constraint on a 360 guest is
vector registers, not integer ones. xenia even names the deciding measurement:
run `arm64_register_allocation_audit`, and it wins only if integer spills are
reported while peak vector use sits well under 28.

**No other fork does this.** ARMSX2, Cemu and melonDS spill to the stack.

**Searched twice, with different words**, after `tools/supervise.py` flagged this
as an unqualified negative. First for `spill.*(vector|vreg|VPR|fmov|umov)` and
the reverse, then for any `spill` at all, excluding vendored trees. The second
search found their register allocators — ARMSX2 `x86emitter`, Cemu
`IMLRegisterAllocator`, melonDS `ARMJIT_Compiler` — and **none of them targets
the vector file.** The claim holds; it did not before the second search.

## Lead 3 — the A710 three-cycle lane-assembly stall

**Also implemented**, and this repo did not connect it to a fork at all.

`a64_vmx_nan_fixup_branchless` addresses exactly the documented hazard: a
V-pipeline uOP with more than one quad-word source, any part of which was
written as single words, **stalls in dispatch for three cycles** — and xenia's
scalar NaN-fixup path writes `v2` lane by lane then feeds it to a full-Q
consumer. The branchless form is 8 vector ops with no branch, no stack spill, no
lane write and no vector-to-GPR transfer.

**Off by default, pending a qemu differential over the four NaN classes.**

**A related one shipped and is ON by default.** `a64_vmx_native_fmax_nan`, since
2026-08-07, is a **correctness fix** that also removes 6 ASIMD uOPs per
`vmaxfp`/`vminfp` from a pipe that is only 2-wide on the A710 and A715. It was
settled from primary sources on both sides plus a bit-exact qemu differential
over 8 NaN cases, citing AltiVec PEM 3.2.5.1 for the guest rule.

## What to change in CLAUDE.md

1. **"Two CPU leads worth chasing" should say they are chased.** Point at
   xenia's flags rather than restating the manual.
2. **Add the fourth position on FP mode**, which the section missed entirely:
   the guest may have **more FP mode registers than ARM64 has**. That is the
   Xenon's actual problem and it is not a "faithfulness" choice.
3. **Record ARMSX2's three-part design** as the propagation candidate, including
   the block-cache sentinel, because parts 1 and 2 without part 3 are a
   correctness bug.

## What this does not say

- **No claim that any of these is a win.** All but one are off by default, and
  xenia says why for each.
- **No number is ours.** `UMOV` latency 2 against `LDR` 4, and the three-cycle
  dispatch stall, are xenia's readings and ARM's documentation.
- **Nothing was measured or built here.** This is a read.

## Method note

Searching for `fpcr|FPSR|fesetround|MXCSR` across the four ARM64-generating
forks found this in one pass. **The earlier surveys missed it because they
searched for capability names.** A tuning flag is not a capability; it is a
`DEFINE_bool` in a backend file with a paragraph of reasoning attached, and the
reasoning is the deliverable.
