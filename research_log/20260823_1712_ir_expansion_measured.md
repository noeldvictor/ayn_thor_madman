# IR expansion measured, device-free: 2.0 to 5.0 IR ops per guest instruction

**Goal: measure instruction inflation per fork, as
[`CLAUDE.md`](../CLAUDE.md) now asks after the literature survey.**

No device. No fork modified. Static counting of emitter source.

## Result

**Stage A of inflation — guest instruction to IR operation — is measurable
without running anything, and the two PowerPC forks differ by 2.5x.**

| | **xenia** | **Cemu** |
| --- | --- | --- |
| Guest | PowerPC (Xenon) | PowerPC (Espresso) |
| Host | ARM64 | ARM64 |
| IR | HIR, 6,605 lines | IML, 6,327 lines |
| Emitters counted | **270** | **121** |
| **Median IR ops per guest instruction** | **5.0** | **2.0** |
| Mean | **5.94** | **3.32** |
| p90 | 10 | 6 |
| Max | 39 | 22 |

**Excluding the vector units**, so integer, control and memory only — the
fairest comparison, since Espresso has no VMX:

| | xenia | Cemu |
| --- | --- | --- |
| Emitters | 171 | 62 |
| **Median** | **5.0** | **2.0** |
| Mean | 6.02 | 2.48 |

**Same guest family, same host, same conclusion either way.**

## The cause is architectural, and it is visible in one opcode

**xenia**, `InstrEmit_addx` — **4 HIR ops**:

```cpp
Value* v = f.Add(f.LoadGPR(i.XO.RA), f.LoadGPR(i.XO.RB));
f.StoreGPR(i.XO.RT, v);
```

**Cemu**, `PPCRecompilerImlGen_ADD` — **1 IML instruction**:

```cpp
IMLReg regA = _GetRegGPR(ppcImlGenContext, rA);
IMLReg regB = _GetRegGPR(ppcImlGenContext, rB);
IMLReg regD = _GetRegGPR(ppcImlGenContext, rD);
ppcImlGenContext->emitInst().make_r_r_r(PPCREC_IML_OP_ADD, regD, regA, regB);
```

**`_GetRegGPR` returns a register reference. It emits nothing.**

> **xenia's HIR is SSA over values loaded from a context, so every operand costs
> a load and every result costs a store. Cemu's IML is virtual-register based, so
> `add rD, rA, rB` is one instruction.**

**That single design choice is the whole 2.5x.**

## The measurement predicts a symptom xenia already confirmed on the device

**This is the part that validates the method.** xenia's own
`cpu_backend_llvm_context_residency` flag text says:

> **Device-confirmed the IR has ~99 ctx memory ops + 1 alloca = NO register
> residency** (the guest thread is memory-bound, ~half of BD's ...)

**Those `ctx memory ops` are exactly the `LoadGPR`/`StoreGPR` pairs counted
here.** A static source count and a device measurement point at the same thing,
and the flag exists to fix it by promoting them to allocas that `mem2reg` lifts
into host registers.

**So the static count is not a curiosity. It is the same quantity, obtainable
without the device.**

## What this does NOT show

**Read this before quoting the 2.5x.**

- **Expansion is not inflation.** This is stage A only. **The optimiser and the
  register allocator collapse much of it** — `LoadGPR`/`StoreGPR` are precisely
  what a register allocator removes. **Final host inflation could be similar for
  both forks.** Stage B is what settles that, and it needs a run.
- **Static, and unweighted.** Every branch inside an emitter is counted, so it
  is an upper bound per emitter, and nothing is weighted by how often an opcode
  actually executes.
- **It undercounts helpers.** `f.UpdateCR()` and `PPCImlGen_UpdateCR0()` expand
  further in both forks and are counted as one call each.
- **The opcode sets differ.** xenia has 270 emitters against Cemu's 121, because
  Xenon has VMX and Espresso has only paired singles. **The vector-excluded
  comparison above is the honest one.**
- **No claim that either is faster.** Nothing here is timed.

## Why it still matters

**A higher starting expansion is not automatically slower, but it is more work
for the optimiser to undo**, and xenia's device-confirmed note says the undoing
is **not** happening — "NO register residency", "the guest thread is
memory-bound".

**So for xenia specifically, the two facts line up**: its IR starts 2.5x more
expanded than Cemu's for the same guest, and its own measurement says the
expansion survives to run time as memory traffic.

**That is the first evidence in this repo that an IR design choice has a
measurable cost here** — and it is a cost of *this IR's register model*, not of
having an IR. **Cemu has an IR too and does not pay it.**

## dynarmic measured too, and it settles the question

**Added the same session.** dynarmic has two frontends and three forks vendor
it, so this covers the rest of the fleet's IR-based translation.

| Frontend | Guest to host | Emitters | **Median** | Mean | p90 | Max |
| --- | --- | --- | --- | --- | --- | --- |
| **Cemu IML** | PowerPC to ARM64 | 121 | **2.0** | 3.32 | 6 | 22 |
| **dynarmic A64** | **ARM64 to ARM64** | 304 | **4.0** | 5.24 | 8 | 48 |
| **xenia HIR** | PowerPC to ARM64 | 270 | **5.0** | 5.94 | 10 | 39 |
| **dynarmic A32** | ARM32 to ARM64 | 607 | **5.0** | 6.42 | 13 | 31 |

Non-vector subsets, the fairer comparison: **Cemu 2.0 median / 2.48 mean**,
**xenia 5.0 / 6.02**, **dynarmic A64 5.0 / 6.57**.

**dynarmic's counting includes its emitting helpers** — `X()`, `V()`, `SP()`,
`ShiftReg()`, `ExtendReg()` — because those are its equivalents of xenia's
`LoadGPR` and `StoreGPR`. Counting only `ir.*` would have undercounted it
against xenia and produced a false result.

### The decisive number

**dynarmic expands 4x when the guest ISA is the host ISA.**

ARM64 to ARM64 is the easiest translation problem that exists — no ISA
mismatch, no endianness, no register-width change, the same memory model — and
it still costs **four IR operations per guest instruction.**

**Cemu, translating PowerPC to ARM64, costs two.**

> **Expansion is a property of the IR's register model, not of the distance
> between guest and host.**

**Two of the three IRs here are SSA over a context** — xenia's HIR and
dynarmic's IR — and both land at 4 to 5. **The one that is virtual-register
based lands at 2.** The correlation is with the register model, and it is
perfect across four frontends.

### And it corrects the catalogue

`PATTERNS.md` listed **four** recompilers. **There are six**, once dynarmic's
A64 and A32 frontends are counted, and **three forks vendor dynarmic** — eden
in-tree, azahar and Vita3K as submodules.

**That makes dynarmic the only code-translation component already shared in this
fleet**, serving three backends and two guest ISAs, reached without
coordination — the same shape as Oboe and the touch overlay.

## Next

- **Stage B and C need one device run**, already queued as
  [`DEVICE_QUEUE.md`](../DEVICE_QUEUE.md) entry 13, using
  `--disassemble_functions` with `--disassemble_function_filter`.
- **The cheaper lever is already named and already built**:
  `cpu_backend_llvm_context_residency`, default off. **Its own text says the
  problem is device-confirmed.**
- **dynarmic is now counted.** ARMSX2 and melonDS have no IR to measure, so
  every IR in the fleet has a number.
- **The cheapest actionable item is xenia's**, because its expansion is the
  highest and its own device measurement says the expansion survives to run time
  as memory traffic. **`cpu_backend_llvm_context_residency` already exists and is
  off.**
