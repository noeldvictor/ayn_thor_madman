# The Thor target: one standard for every fork

**The north star. What every fork compiles for, tunes for, and budgets
against.**

Written 2026-08-22. It exists because the fleet has no standard, and every
fork answered the question differently or not at all.

**What the build files actually say**, read rather than assumed:

| Fork | `-march` on Android arm64 | `-mtune` | |
| --- | --- | --- | --- |
| **melonDS-android** | untouched, so `armv8-a` | **`cortex-x3`**, feature-guarded | **the only fork that reasoned about it** |
| **xenia-thor** | **`armv8-a+crypto+sha3+crc+dotprod`** | — | the only fork enabling device features |
| **ARMSX2** | **`armv8-a`, set deliberately** | — | uses `armv8.4-a -mcpu=apple-m1` on Apple |
| **eden-thor** | preset: none by default, `armv8-a`, or **`armv9-a`** | `generic` | **the `armv9` preset is a live trap here** |
| azahar-thor | baseline | — | |
| Cemu-thor | nothing | nothing | |
| Vita3K | nothing | nothing | |

Three findings come out of that table, and they matter more than the
inconsistency itself.

**melonDS is already right, and wrote down why.** Its `CMakeLists.txt` states
that `-mtune` changes scheduling and cost models only, emits no new
instructions, and so keeps the binary running on any `armv8-a` device, while
raising `-march` would pull in dotprod, fp16 and LSE and drop pre-ARMv8.2
hardware, which it calls "a separate call". **That reasoning is correct for
melonDS-android and wrong for this project**, because this app ships to one
device. The tradeoff melonDS is protecting does not exist here.

**ARMSX2 targets an Apple M1 more precisely than it targets the Thor.** The
same file selects `-march=armv8.4-a -mcpu=apple-m1` for Apple Silicon and
`-march=armv8-a` for Android, commented "ARMv8.0-a is the baseline for Android
arm64-v8a". It knows how to raise a target. It treats Android as generic.

**eden ships an `armv9` build preset.** On this device that is not a
theoretical hazard, it is a live one. See below.

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

**eden already ships the hazard.** Its `CMakeLists.txt` offers
`YUZU_BUILD_PRESET=armv9`, which sets `-march=armv9-a`. **Do not select it on
this device.** Its default preset is `custom`, which leaves `march` undefined
and therefore passes no `-march` or `-mtune` at all, so the default arm64 build
is untuned rather than mistuned.

**Box64, vendored under `xenia-thor-workspace/reference/`, is the corroborating
example.** It keeps a per-SoC table of exactly this form — `-march=armv8.2-a+
crc+crypto+fp16+rcpc+dotprod -mtune=cortex-a76` for the SD865 class — and its
Oryon entry names `+sve+sve2` explicitly. **It tracks SVE per SoC precisely
because SVE is not universal on ARMv9.** That is the pattern this document
follows.

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

**melonDS already picked `cortex-x3` and guards it with
`check_cxx_compiler_flag`.** Keep the guard. A toolchain that does not know
`cortex-x3` should fall back rather than fail the build.

**A fork may deviate**, for example by tuning for a mid core when its work
lands there. **State the deviation and its reason.** No fork currently
deviates; several are simply silent, which is different.

**Do not use `-mcpu=` here.** It sets features and scheduling together, so it
hides the ARMv9 question this document exists to answer. Use `-march` for
features and `-mtune` for scheduling, separately.

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
   device's features, only melonDS sets `-mtune`, and no fork does both.
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
`/proc/cpuinfo`, and from research written by forks in this fleet. **The only
measured numbers here are the feature list and the power target.** The build
flag table is read from the build files, which makes it fact about the fleet
and not about performance.

**Nothing here claims the target is faster.** It claims the target matches the
silicon. Whether `+dotprod` and `-mtune=cortex-x3` actually move frames or
watts on this device is an open experiment, and the honest prior is that
`-mtune` alone rarely does much.

Every rule is a hypothesis with a good source until a Thor A/B says otherwise.
Record those in the experiment ledger, not here.
