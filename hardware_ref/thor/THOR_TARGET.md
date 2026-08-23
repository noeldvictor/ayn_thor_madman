# The Thor target: one standard for every fork

**The north star. What every fork compiles for, tunes for, and budgets
against.**

Written 2026-08-22. It exists because the fleet has no standard and the results
show it: **Vita3K tunes for `cortex-x3`, Cemu tunes for `cortex-a710`, ARMSX2
and azahar compile baseline `armv8-a`, and only xenia enables the features the
device actually has.** Five forks, five answers, one SoC.

Every number here is either measured on the device or cited to its source.

---

## 1. The compile target

```
-march=armv8.2-a+crc+lse+fp16+dotprod+sha3+i8mm+bf16
-mtune=cortex-x3
```

### Do not target `armv9-a`

The X3, A715, A710 and A510 are all ARMv9 cores. **The SoC is not usable as
ARMv9 here.**

ARMv9.0-A mandates SVE2. **Qualcomm shipped the 8 Gen 2 without SVE at all**,
confirmed from `/proc/cpuinfo`: the feature list is `asimddp i8mm bf16 fphp
asimdhp atomics lrcpc ilrcpc sha3`, with **no `sve` and no `sve2`**.

A compiler told `-march=armv9-a` may emit SVE. **Target `armv8.2-a` and name
the features explicitly.** That is why the forks that chose `armv8-a` were not
simply being lazy, and it is the single most important line in this document.

### Name every feature the device has

| Feature | Flag | What it buys |
| --- | --- | --- |
| dot product | `+dotprod` | `SDOT`, `UDOT`. Guest vector units do dot products constantly. |
| SHA3 | `+sha3` | `EOR3`, `BCAX`, `RAX1`, `XAR`. **Three-input bitwise ops**, not just crypto. |
| LSE atomics | `+lse` | cheaper atomics than load-exclusive loops |
| half precision | `+fp16` | `fphp`, `asimdhp` |
| int8 matmul | `+i8mm` | |
| bfloat16 | `+bf16` | |
| CRC | `+crc` | |

**Anything absent from `/proc/cpuinfo` must not be assumed.** Check the device,
not the core's manual: a manual describes what the core *can* implement, not
what the vendor shipped.

### Tune for the X3, and know why

`-mtune` picks a scheduling model, not a feature set. **Any single choice is
wrong somewhere on a 1+4+3 SoC**, so choose deliberately:

- **Hot code belongs on the X3.** That is where a recompiler's output should
  run.
- Code scheduled for the X3 is *suboptimal* on the A510, not broken. Code
  scheduled for the A510 leaves the X3 badly underused.
- The guides **conflict**: A715 wants branches concentrated, A510 wants at most
  one conditional branch per 16 bytes. See
  [`cpu/CORE_COMPARISON.md`](cpu/CORE_COMPARISON.md). No flag satisfies both.

**Cemu's `-mtune=cortex-a710` is a defensible choice for a fork whose work
lands on mid cores. It is not the fleet default, and it should be stated as a
deviation rather than left implicit.**

---

## 2. The thermal and power budget

**Target: roughly 5 W and 50 C sustained.** Taken from
`xenia-thor/tools/thor/power_affinity_ab.sh`, which states the goal is a power
target and that throughput alone answers the wrong question.

| Rule | Why |
| --- | --- |
| **Report watts, not only frames.** | A change that holds fps and lowers temperature is a win. |
| **Gate power readings on `status=Discharging`.** | Plugged in, `current_now` flips sign between idle samples. **Any wattage from a USB session is fiction.** |
| **Run 15 minutes or more when heat matters.** | Thermal behaviour settles over minutes. A short run measures a cold device. |
| **Record the temperature delta.** | **No heating means the run did not happen** — an idle or menu scene produces a number and no heat. |

**ADPF is disabled on this device** by persisted config overriding a compiled
default. Measure without it first, then force it explicitly if testing it.

Google's guidance: report actual duration as CPU plus GPU against a target from
the render frame rate. **A fixed 60 fps target on a game running at 15 makes
the system boost CPU clocks for frames it cannot deliver.**

---

## 3. Thread placement

**This is a design problem, not a flag.** Two forks learned opposite halves of
it:

- **xenia** found guest threads hard-pinned to the 2.0 GHz A510 cores while the
  Cortex-X3 sat idle.
- **rpcsx** keeps the **full** core mask deliberately, because restricting the
  process to the big cores drags Java, audio and compiler threads onto the same
  cores as emulation work.

**Both are true.** The policy that follows:

1. **Place the hot guest thread on the X3.** Do not let the scheduler decide by
   default.
2. **Do not restrict the whole process mask.** Audio, JIT compilation and
   system threads need somewhere else to run.
3. **Never put two vector-heavy threads on a paired A510.** Dual A510 complexes
   **share a VPU and an L2**, so they contend invisibly. Read
   `IMP_CPUCFR_EL1.Cores` and `IMP_CPUCFR_EL1.VPU` for the pairing and the
   datapath width.
4. **Place command buffer recording, pipeline compilation, texture upload and
   present deliberately.** They compete with emulation for the same prime core.

---

## 4. Codegen rules that follow from the silicon

Details in [`cpu/CORTEX_X3_NOTES.md`](cpu/CORTEX_X3_NOTES.md) and
[`cpu/CORE_COMPARISON.md`](cpu/CORE_COMPARISON.md).

| Rule | Where it comes from |
| --- | --- |
| **`yield` is a no-op on ARM. Use `ISB` for spin backoff.** | rpcs3 found **half of all CPU time** in a four-line `busy_wait` |
| **Spill GPRs to the vector register file, not to memory.** | X3, A715 and A710 guides all say so |
| **On mid cores, a constant load can beat computing it.** | A715 and A710 have **3 load ports against 2 arithmetic ports** |
| **Guest FP status flags serialise.** `NZCV` and `SP` are renamed; `FPSR`, `FPCR` and `APSR` are not. | X3 guide, special register access |
| **Emit compare adjacent to branch, unshifted.** | X3 fuses them; A510 does not, and it costs nothing there |
| **Keep to 4 branches per aligned 32 bytes.** | X3 and A710. A715 wants the opposite; A510 wants one per 16 bytes |
| **Do not interleave vector forwarding regions.** | one cycle penalty on all three big cores |
| **`DC ZVA` beats `STP` for zeroing.** | every core |
| **Avoid `LD4`/`ST4` multi-structure forms.** | decode-limited on the X3 |

---

## 5. What every fork must do

1. **Adopt the compile target in section 1.** Today only xenia enables the
   device's features; ARMSX2 and azahar are baseline `armv8-a`.
2. **Check the emitter, not only the flags.** ARMSX2 and melonDS emit no
   `SDOT`, `UDOT`, `EOR3` or `BCAX` at all.
3. **Pass real target features to LLVM** where a fork uses it. rpcs3 was gating
   FMA on the CPU *name* containing "cortex", so every Qualcomm core fell off
   the fast path.
4. **State a deviation.** Tuning for a different core is allowed and must be
   written down with its reason.
5. **Measure against section 2 before claiming anything**, and name the cluster
   in every claim.

---

## What this document is not

**It is not measured.** Sections 1, 3 and 4 are derived from ARM's guides, from
`/proc/cpuinfo`, and from research written by two forks in this fleet. **The
only measured numbers here are the feature list and the power target.**

Every rule is a hypothesis with a good source until a Thor A/B says otherwise.
Record those in the experiment ledger, not here.
