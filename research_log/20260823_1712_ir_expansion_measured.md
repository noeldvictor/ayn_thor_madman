# IR expansion measured, device-free: xenia 5.0, Cemu 2.0

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

## Next

- **Stage B and C need one device run**, already queued as
  [`DEVICE_QUEUE.md`](../DEVICE_QUEUE.md) entry 13, using
  `--disassemble_functions` with `--disassemble_function_filter`.
- **The cheaper lever is already named and already built**:
  `cpu_backend_llvm_context_residency`, default off. **Its own text says the
  problem is device-confirmed.**
- **Do not generalise from two forks.** ARMSX2 and melonDS have no IR to
  measure, and eden's dynarmic IR was not counted here.
