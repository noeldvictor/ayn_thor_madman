# The guest clock is the x86 detour, measured at ~11% of emulator CPU — and a fourth form of it

**Goal: read Cemu's `AGENTS.md`, the fourth-largest index in the fleet.**

**Its performance section contains the clearest instance of this project's
central CPU thesis anywhere in the fleet, with numbers and a shipped fix.**

## The measurement

> `coreinit::OSGetTime()` measured **7.8% of all emulator CPU**, with a further
> **3.6% in its spinlock's atomic swap**, and **every guest `mftb` routes
> through the same function.**

**~11.4% of emulator CPU in one function and its lock.**

## Why the lock existed, and why it does not need to

**The original design** accumulates deltas into a shared 128-bit accumulator
under a **global `FSpinlock`**, with an **`mfence`** and a **128-bit division per
call**.

> **"That exists because x86 cannot assume the TSC is uniform across cores.
> AArch64 can":** `CNTVCT_EL0` is **one monotonic counter shared by every core**,
> so the guest tick count is **a pure function of it** — no shared state, no
> lock, no barrier, no division.

**The ARM path computes it directly with a 32.32 fixed-point multiply**, `mul` +
`umulh`. Only the timer shift factor stays stateful, and it is republished
through a **seqlock** rather than a mutex.

> **"Three emulated cores were serialising on one lock to read a counter that is
> already coherent between them."**

## This is a FOURTH form of the x86 detour, and the sharpest one

`CLAUDE.md` records the detour in three forms: **instruction selection**, the
**register model** — memory operands folding on x86 and not on ARM64 — and
**timing constants** correct on x86 and wrong here. `TRANSLATION.md` covers the
register half.

> **This is none of those. It is a SYNCHRONISATION STRUCTURE that exists to
> paper over a guarantee x86 does not give and AArch64 does.**

**The lock, the barrier and the 128-bit division are all one artefact of the
same missing guarantee.** On a host where the counter is architecturally
coherent, **all three delete together** — and what replaces them is two
instructions.

**That is the DELETE operation with a measured size**, on the CPU side, where
this repo's DELETE examples have been about portability layers rather than
correctness scaffolding.

**And the rule the fork wrote beside it is the transferable part:**

> **"Do not reintroduce a global lock here."**

**A deleted x86 scaffold will grow back** unless somebody records why it went,
because the code looks under-synchronised to a reader who does not know the
counter is coherent.

## A second measurement, and it connects to another fork's

> Profiling Star Fox Zero, 2026-08-20: **23% of all emulator CPU** and **48% of
> `OSSched[core=1]`** in **`__kernel_clock_gettime`**, 99.8% of those samples on
> one thread, **clustered tightly at vdso offset `0x30c` — the `isb` + `mrs
> CNTVCT_EL0` sequence.** *"Real code at an extreme call rate, not
> misattribution."*

**Nobody has connected this to the spin-wait measurement, and the two fit.**
rpcsx measured on this same SoC: **`yield` 0.36 ns, `nop` 0.36 ns, `isb`
11.42 ns** — **`ISB` costs 32 times a `yield`.**

> **The vdso clock read is `isb` + `mrs`. At an extreme call rate, a sequence
> whose barrier alone costs 11.42 ns accounting for 23% of emulator CPU is
> exactly what those two numbers together predict.**

**Two forks, two independent measurements, one instruction.** Neither cites the
other.

**The caller is unidentified and the fork says why:** the vdso has no frame
pointers and DWARF cannot unwind through it. **`OSGetTime()` was ruled out by
disassembly** — it compiles to `mrs CNTVCT_EL0` directly, with no libc call. The
remaining call sites found by disassembly are the FPS overlay, the performance
monitor, a file cache test, `LatteCP_*`, `select`, the mic path, curl and
libusb — *"none of which should be hot on a guest scheduler thread."*

**And the method note is the useful part:** *"Next step is to interpose or count
rather than sample."* **When the sampler cannot attribute, stop sampling.**

## A third item: one NaN, three answers, recorded rather than fixed

`psq_st` quantisation of a NaN to S16:

| | value |
| --- | --- |
| **Espresso, per the Gekko manual** | **32767** (positive-overflow saturation) |
| x64 `cvttsd2si` then clamp | **-32768** |
| AArch64 `fcvtzs` then clamp | **0** |

> **Neither backend matches the hardware and they do not match each other.**

**It is recorded rather than fixed, with the reason**: unmeasured, probably rare,
and a fix costs instructions on a path that is ~1.7% of the executed opcode mix.
**The rule attached to it is the valuable half:**

> **"If it is ever fixed, fix both backends together and add a cpu-test, or the
> divergence just moves."**

**That is a differential-testing finding this repo's test section should carry**,
because it is the case its `StateSnapshot` differential harness exists to catch
and would currently pass: **both backends are self-consistent, and both are
wrong.**

## Limits

- **All three numbers are Cemu's, on Cemu's workload, one title.** Nothing
  reproduced here and no device used.
- **The 7.8% and 23% are from different profiling sessions** and are not additive
  without checking.
- **The `isb` connection is arithmetic consistency, not a measurement.** Nobody
  has timed the vdso path against a hypothetical without the barrier, and the
  `isb` there is architecturally required for counter ordering.
- **The NaN table is read from the fork's own document**, not re-derived.

## Sources

- Cemu `AGENTS.md:244-309`
- rpcsx `docs/arm64/bench-results.md`, via
  `research_log/20260823_1454_spin_wait_audit.md`
