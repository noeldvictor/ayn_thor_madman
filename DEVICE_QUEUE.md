# The device queue

**Everything waiting on the Thor, with what each run should show if it works.**

**A run with no prediction cannot fail.** Every entry states its expected
signature before it is run, per the rule in `CLAUDE.md`. An entry with no
prediction is not ready to run.

There is one physical Thor, so device work is a queue and analysis is not.
Ordered by what unblocks the most.

---

## Before any run

| Gate | Why |
| --- | --- |
| `adb -s "$THOR"`, never bare `adb` | a Quest 2 also answers adb on this box |
| verify `ro.product.model` is `AYN Thor` | a wrong `-s` flashes the Quest |
| **`status=Discharging`** | plugged in, `current_now` flips sign. **Any wattage from a USB session is fiction** |
| check the **60 Hz cap** | `PRIORITY_USER_SETTING_PEAK_REFRESH_RATE` votes 60. Both panels do 120. Pacing runs measure the setting otherwise |
| record battery level and charge state | |
| **15 minutes or more** when heat matters | thermal behaviour settles over minutes |
| **RECORD A PREFLIGHT TEMPERATURE AND MATCH THE ARMS** | **thermal drift measured 12% on identical work across 30 C to 68 C** — larger than almost every effect queued below. Two arms starting tens of degrees apart are measuring temperature |
| **temperature must rise** | no heating means an idle or menu scene, so the run is invalid whatever the counter said |
| query the experiment ledger first | `python tools/exp_ledger.py check "<keyword>"` in xenia-thor |
| **CAN THE WORKLOAD EXPRESS THE CHANGE?** For any CPU experiment, **measure the LOADING phase, not gameplay or a menu** | see below |
| **cumulative counters, never a spot reading** | an instantaneous `current_now * voltage_now` on an idle device spread **1.66 W**; differencing `charge_counter` over the window spread **0.002 W** |
| **the harness is not thermally free** | the same run through an input-macro harness reached **70.7 C in ten seconds** and tripped its own guard, while a direct boot never left the fifties |
| **`simpleperf` needs `<profileable android:shell="true"/>`** | without it, it **cannot attach at all**. Use `/proc/<pid>/stat` fields 14+15 for any A/B spanning an older build |
| **VERIFY THE STOP.** `am force-stop` on a package that is not installed **exits zero and prints nothing** — check with `pidof` against the real `applicationId`, not the APK or build-variant name. A ~1.78 W result was withdrawn because both arms were the same configuration | rpcsx `instruments.md` |

### The package names, because six of nine forks are not what you would type

**`am force-stop` on a package that is not installed exits zero and prints
nothing**, so a wrong name makes an A/B compare a configuration against itself.
**Read from each fork's build files, 2026-08-24:**

| Fork | base `applicationId` | suffixes it applies |
| --- | --- | --- |
| **xenia** | `jp.xenia.emulator` | **`.debug`, `.checked`, `.github`** — and the build measured here is `githubDebug` |
| **eden** | `dev.eden.eden_emulator` | `.nightly`, `.relWithDebInfo`, `.debug` |
| **melonDS** | `me.magnum.melondualds` | `.dev`, `.nightly` |
| **GameThor** | `app.gamenative` | `.hgo`, `.gold` |
| **azahar** | `org.azahar_emu.azahar` | `.debug` |
| **Cemu** | `info.cemu.cemu_thor` | `.debug` |
| ARMSX2 | **`com.armsx2`** | none — **but overridable by `-Parmsx2.applicationId`** |
| Vita3K | `org.vita3k.emulator` | none found in the build files — **yet its own reports launch `org.vita3k.emulator.debug`** |
| rpcsx | `net.rpcsx.easy` | none — **its APK is named `thortest`, which is what caused the incident** |

> **`CLAUDE.md`'s `am force-stop com.armsx2` is correct**, checked directly:
> ARMSX2 is the one Tier 1 fork with no suffix.

**Three cautions, because this table is a build-file reading and not an install
list.**

- **How a flavour and a build type compose into one suffix chain was not
  verified.** `githubDebug` may be `.github`, `.debug`, or both.
- **Vita3K is the warning in the table**: no suffix in its build files, and a
  `.debug` package in its own reports. **Check, do not assume.**
- **The authoritative answer is on the device**: `pm list packages | grep <base>`,
  then `pidof <exact>` after the stop. **This table tells you what to look for,
  not what is installed.**

### Resolving an entry matters as much as adding one

**Taken from Vita3K's `AGENTS.md`, 2026-08-24.** Its rule:

> **"Planned or open attempts are BLOCKERS.** Resolve the current planned
> attempt as `failed`, `inconclusive`, `superseded` or `succeeded` before
> starting a nearby experiment, **otherwise the ledger becomes a pile of
> half-memory."**

**This queue is 26 open entries and had no such rule.** Two consequences:

- **Before adding an entry near an existing one, resolve the existing one** —
  or say explicitly why both must be open.
- **A run produces a resolution, not just a number.** An entry that was run and
  never marked is worse than one never run, because the next reader cannot tell
  which it is.

**And the preflight, which is what makes "query the ledger first" durable:**

> **Write down, before the run: the commit, the platform, the scene, the debug
> props, the matching prior attempts, what those attempts proved, and THE ONE
> REASON THIS RUN IS GENUINELY NEW.** *"If the only difference is 'try it
> again', stop and choose instrumentation or documentation instead."*

**A query you run and do not write down does not stop the next session
repeating it.**

**Outcomes get a symptom label as well as a number**, because a queue entry can
change a symptom without moving a counter: `fixed`, `improved`, `unchanged`,
`worse`, **`mixed-supports-involvement`**, `contaminated-inconclusive`. **`fixed`
requires the original symptom gone, no neighbouring regression, no stale debug
toggles, and proof on every platform affected.**

### The workload gate, and the false negative that produced it

**Cemu measured this and named the cost.** *"Every scene reachable without
playing is **vsync capped at 60fps with the GPU around 15% busy and 2 of 8 CPU
cores in use**, so nothing is saturated and **per-instruction work is
invisible**."*

> **A real 3-instructions-to-2 recompiler win measured 7922 against 7897 ticks —
> nothing** — because whole-process CPU during gameplay is dominated by threads
> spinning.

**Loading is the workload that works**, because roughly half its samples land in
recompiled guest code.

**Consequence for this queue, stated plainly: several entries below specify
"same route, same scene" without saying whether that scene is saturated.**
**A 0.2% noise floor on a gated title screen is precision without sensitivity** —
the floor is tight because nothing is happening. **Before running any CPU entry,
confirm the route is CPU-bound, or the FLAT it returns means nothing.**

**This does not apply to the GPU and thermal entries**, where a fixed, repeatable
scene is the point.

---

## 1. Close the SVE question

**One command, and it settles a claim in three documents.**

```sh
zcat /proc/config.gz | grep -i ARM64_SVE
```

| Result | Conclusion |
| --- | --- |
| `CONFIG_ARM64_SVE=y` and no `sve` in cpuinfo | **fused off in silicon.** Nothing can reach it |
| not set | **a kernel choice.** A custom kernel could expose it |

**Prediction: not set.** Qualcomm disabled SVE across this Snapdragon
generation, so the kernel most likely never enabled the support either.

**It changes no decision either way** — 128-bit SVE is the same width as NEON.
It is recorded because the repo stated the wrong version of it for three
commits. See
[`research_log/20260822_2147_sve2_on_the_thor.md`](research_log/20260822_2147_sve2_on_the_thor.md).

## 2. Pin the Turnip driver — **PARTLY ANSWERED 2026-08-24, by azahar, on this device**

> **A live 20-sample bracket at 3x and 615 MHz: generic R8 8.022% mean KGSL busy,
> forced-Sysmem R8 9.775% (21.86% more GPU time), PurpleVK/T26 8.008% — a 0.18%
> noise-scale tie. All reproduced the exact accepted frame.**
>
> **Sysmem is rejected for that workload.** This repo predicted it would be
> slower, because it forces system-memory rendering instead of GMEM tiling;
> **the prediction now has a number.**
>
> **PurpleVK ties generic R8**, so on that scene the pin is **not** a performance
> decision and must be made on other grounds — extension coverage, the
> attachment-self-read rule, or bug surface.
>
> **What remains for this entry:** one 3DS scene at one clock is not the fleet,
> and **the newer builds this repo lists — Mesa 26.3.0 r7 and MrPurple T30 —
> were not in that bracket.** azahar's accepted driver is **R8**.



Three candidates already on the device: `turnip_mrpurple_T30-toasted`,
`mesa-turnip-v26.3.0-20260803-r7`, `Turnip_v26.0.0_R8`.

**Provisional pin is T30 and it was chosen from a changelog, not a measurement.**

Run an **in-place alternating A/B inside one run**, on a busy frame. Cross-run
comparison is untrustworthy because scene complexity swings several times a
second.

**Prediction: differences under 5% on frame time, with the spread between
builds smaller than the spread between scenes.** If that holds, **pin the one
with the fewest rendering faults rather than the fastest**, and record the
result as `FLAT` rather than promoting a winner.

State watts and temperature, not only frames.

## 3. The app shell on both displays

The shell builds and installs. **Four behaviours were fixed blind** and none is
verified. See
[`work_log/20260822_2158_shell_second_screen_lifecycle.md`](work_log/20260822_2158_shell_second_screen_lifecycle.md).

| Check | Prediction |
| --- | --- |
| background the app | Screen-2 panel **disappears**; previously it stayed with stale content |
| move the activity between panels | the panel **re-targets** to the other display |
| after re-attach | Screen-2 shows current content, **not blank** |
| add or remove a display | **the listener may never fire.** Both panels are internal |

**If the last one never fires, delete the listener** rather than keeping it for
symmetry.

## 4. Does the compile target do anything — **ANSWERED, no device needed**

**Removed from the queue 2026-08-23.** xenia already ran it, by disassembling
its own binary rather than benchmarking: enabling the features made clang emit
**zero** of them. **The flags are permission for hand-written intrinsics, not a
codegen win.** See
[`research_log/20260823_0150_target_features_are_permission.md`](research_log/20260823_0150_target_features_are_permission.md).

**The lesson for this queue: ask whether a question needs the device at all.**
"Does the compiler emit X" is answered by disassembly — deterministically, with
no scene noise. Only "is it faster" needs the Thor.

### The original entry, kept for the record

`-march=armv8.2-a+...+dotprod+sha3 -mtune=cortex-x3` against the fork's current
baseline, same commit, same scene.

**Prediction: no measurable change from `-mtune` alone.** The honest prior is
that scheduling flags rarely move a real workload. **`+dotprod` and `+sha3` will
also do nothing until an emitter actually emits them** — ARMSX2 and melonDS emit
neither today, so this measures the compiler's own use of them and little else.

**Expect `FLAT`.** Record it as such; a flat result stops the question being
re-argued.

## 5. Verify the targetSdk raise — a build passing does not test it

**Every fork moved to `targetSdk = 37` on 2026-08-23**, and `targetSdk` is the
**behavioural** level: each step opts the app into new Android restrictions.
**A successful build verifies none of that.**

**azahar and ARMSX2 were already on 37 and azahar recorded what it cost:** a
black emulation root, the named `coordinator_layout`, and one null-safe
display-cutout margin listener attached to **both** the coordinator and the
in-game menu — plus not restoring the deleted `values-v35` opt-out theme.

**Prediction: the forks that just jumped will show edge-to-edge and
display-cutout problems, not build failures.** Watch for content drawn under the
status bar or the cutout, and for a crash casting a null `layoutParams`.

**Worst on this device**, because the Thor has two panels with different
geometry and Screen-2 carries `FLAG_PRESENTATION`.

**Check per fork:** launch, rotate, enter and leave the in-game menu, and move
the app between the two displays.

## 6. Thread placement

Two forks set no host affinity at all: **melonDS and Vita3K**. melonDS tunes its
codegen for the X3 and never asks for the X3.

Pin the hot guest thread to the prime core, leave the process mask alone.

**Prediction: a real gain on melonDS or Vita3K**, because they currently express
no preference and xenia measured guest threads landing on the 2.0 GHz A510s
while the 3.2 GHz X3 idled. **This is the most likely win in the queue.**

Watch for the opposite on a fork that already places threads — a second opinion
about placement is worse than one.

## 7. Frame pacing

**No fork uses Swappy or `VK_GOOGLE_display_timing`.** Every one selects a
present mode and stops.

Compare plain `FIFO` against Swappy on a backend that cannot hold 60.

**Prediction: no change in average fps and a large change in frame-time
variance.** That is the whole point — FIFO alternating 16.6 and 33.3 ms is
judder, and a stable 33.3 looks better than a faster average.

**So average fps is the wrong metric here.** Record 1% low and frame-time
standard deviation, or the run will read as `FLAT` while being a win.

## 8. Render pass attachment ops

Four forks give four answers, and the best is not in the newest fork. Vita3K
tracks transient attachments and uses `DontCare` both ways; eden loads and
stores unconditionally.

Change eden's depth ops to `DONT_CARE` where the pass does not need them, as
Cemu already does.

**Prediction: a bandwidth reduction visible in GPU counters, and possibly
nothing in frame time**, because the win is memory traffic and thermal headroom
rather than throughput. **Measure watts.**

## 9. Anime4K's two dedicated passes

azahar gives Anime4K two full-screen render passes, `anime4k_xy_renderpass` and
`anime4k_luma_renderpass`. **On a tiler each is a load and a store of the whole
target unless carefully arranged, and nobody has priced it.**

**Prediction: a measurable per-frame cost proportional to output resolution**,
which would make the flagship feature more expensive on the larger panel than
anybody has assumed.

## 10. Native game reference capture

Capture a well-optimised native game with Perfetto and Snapdragon Profiler:
pass count, resolves, GMEM residency, vertex against fragment split, bandwidth,
watts. Then capture one backend on a comparable scene.

**Prediction: the emulator shows several times the render passes and resolves.**
The gap is structural, not CPU — a faithful emulator inherits a rendering
structure designed for hardware with the opposite tradeoffs.

**The difference between those two captures is the actual roadmap.**

---

## 11. Spin-wait: does parking on the address beat the ISB spin

**AUDITED 2026-08-23 with no device.** See
[`research_log/20260823_1454_spin_wait_audit.md`](research_log/20260823_1454_spin_wait_audit.md).
Most of this question is already answered; **only two parts need hardware.**

**Already measured, by rpcsx on this SoC.** `yield` 0.36 ns, `nop` 0.36 ns,
`isb` 11.42 ns, armed `wfe` about 72 us. Spin wake latency ~0.44 us, futex wake
~10 us. **Do not re-run any of that.**

**What is left, and it is narrow:**

**11a. Does eden's `yield` spin lock cost anything?** It is provably a `nop` and
it sits on `k_slab_heap` and `KThread::m_context_guard`. **How often those
contend is unknown**, and a lock that never contends costs nothing however it
spins.

**Prediction: below the noise floor.** A context guard is held briefly and the
Switch guest is not heavily threaded on most titles. **Expect `FLAT`**, and
treat that as the reason to fix it by deletion rather than by tuning.

**11b. Does tier 1 beat tier 2 in a real title?** `SEVL`/`WFE` + `LDAXR` against
the `ISB` spin, same fork, same scene.

**Prediction: `FLAT` or `GFX-LOSS`, not a win.** Three results already point
that way: xenia's own ISB A/B was `CONFOUNDED`, Cemu measured a time-based
backoff as **worse**, and **rpcsx measured a real park as worse on a real
title** — 58.3 fps to 50.0, reproducibly. **A microbenchmark win has not once
survived contact with a frame here.**

**Gate:** eden does not build on this box, so 11a is blocked on `pkg-config` and
`glslangValidator` before it is blocked on the device.

**Do not run either until something cheaper is exhausted.** The audit's value is
already banked in the propagation ledger and does not depend on these.

## 12. What does an IR cost on this device

**Two forks carry one and three do not.** xenia has a full HIR; eden's dynarmic
has an IR; **ARMSX2, Cemu and melonDS translate without one.** Nobody has
measured what the IR costs here.

**This is the only device experiment in this queue with a published mechanism
behind it**, rather than a hunch. See
[`research_log/20260823_1642_ir_in_emulators_literature.md`](research_log/20260823_1642_ir_in_emulators_literature.md).

**Do the cheap half first, and it needs no device.** Count host instructions
emitted per guest instruction for the same guest block, in a fork with an IR and
one without. **That is a disassembly count, exactly like the target-features
work**, and it separates *does the IR change the code* from *is it slower*.

> **THIS IS ENTRY 13, and neither entry cited the other until 2026-08-25.** Entry
> 13 supplies the instrument for exactly this count — xenia's
> `--disassemble_functions` with a function filter — **and wrongly claimed it
> needed the device.** With that corrected, **the cheap half of this entry and
> the whole of entry 13 are one device-free experiment.**
>
> **Which matters, because this entry calls itself "the only device experiment in
> this queue with a published mechanism behind it, rather than a hunch."** The
> best-founded item in the queue has been waiting for hardware it does not need.

**Prediction: `FLAT` for the device half.** Three reasons. **The IR's cost is at
translation time, not run time**, so it shows up as first-run stutter rather than
steady-state frames — and this project already owns that through the shader and
code caches. **The literature's headline numbers do not survive reading**: the
35x is a 3-opcode loop against a simulator, and a fully-static translator reaches
only parity with QEMU's JIT on SPECint. **And xenia's own ledger records
incremental CPU levers as `DEAD` or `FLAT` repeatedly.**

**What would change the answer:** a title that recompiles constantly — heavy
self-modifying code or frequent cache flushes — where translation throughput is
the bottleneck. **Name such a title before running this**, or the run measures
steady state and reports the prediction back.

**Do not rewrite an IR out of a fork on this evidence.** Risotto shows the IR
carries the memory-model verification, and nothing here says where that goes
instead.

### LEDGER QUERIED, and it changes this entry

`python <xenia>/tools/exp_ledger.py check "IR"` returns **146 matches**;
`"backend"` returns 28. **Three findings apply directly.**

**1. The infrastructure exists and has been run once.** xenia has **three** code
paths — the a64 direct backend, an LLVM backend, and the HIR feeding them — and
`cpu_backend_llvm` is a live flag. `llvm_residency_ladder_thor` (2026-07-24) is
the **first run ever with it actually on.**

**2. That run is `CONFOUNDED`, and the confound is the one this queue already
names.** VdSwap/s read 7.8 / 8.1 / 9.9 / 8.1 / 8.1 across the ladder, but the
apparent +27% was **a different scene** — BD's intro advances at a rate that
depends on emulation speed, so a fixed wall-clock sample lands on a different
frame per run. **The fix is already designed**: `bd_fixed_frames_bench.ps1`,
timing a **fixed frame range** of the deterministic no-input intro, content-
matched by construction. **Use it, or this entry will come back `CONFOUNDED`
too.**

**3. Its side findings are recorded as solid and are the real value.** The LLVM
backend runs on the Thor with **0 faults and no regression**; lowering coverage
is complete at `LLVMbegin == LLVMmap == 1865`; and there is **a systematic
hole** — 30-plus functions fall back to a64, **all of them `mul_add`/`mul_sub`**
from the deliberate `cpu_backend_llvm_lower_vmaddfp=false` workaround. **Every
`vmaddfp`-using function is therefore excluded from LLVM, from residency, and
from the AOT object cache.** Fixing `vmaddfp` is a **coverage** lever before it
is a correctness fix.

**CORRECTED within the hour: "fix `vmaddfp`, no device needed" was wrong.**
xenia's own flag text says the opposite, and reading it was the check:

> the LLVM lowering is **qemu-byte-correct in isolation**, but on-device it
> **MISCOMPILES** a function that uses `vmaddfp` **together with other vector
> ops** (BD's vertex-transform routine, e.g. `0x82282490`) -> degenerate
> geometry ('cyan-bars'), **at opt=0 AND opt=2**. It is a device
> codegen/regalloc **INTERACTION** bug (**the IR is correct**), not fixable from
> the IR

**So this is not a coverage tidy-up. It is a live miscompile in LLVM's AArch64
backend**, and the a64 fallback is the correct behaviour, device-proven via
`cpu_backend_llvm_skip_opcodes=77`.

**xenia already built the right tool for it**, and its text states the method:

- `cpu_backend_llvm_dump_ir` — the IR, "**device-free-ishly**"
- `cpu_backend_llvm_dump_asm` — the post-codegen AArch64 assembly, "**Unlike
  `_dump_ir` (which shows correct-looking IR), this shows the IR->asm output
  where device codegen/regalloc bugs live**"

Both take `_range_lo`/`_hi` to isolate one function.

### The off-device repro path, and the one thing that blocks it

**The IR is target-independent, so the failing function's IR can be compiled to
AArch64 on any box.** That turns a device asm-debugging job into a desktop one —
the same move as the target-features work, which was settled by disassembling
rather than by running.

**Prerequisites, measured 2026-08-23:**

| Need | State |
| --- | --- |
| The exact LLVM | **20.1.8** — read from both shipped `libLLVM.so` files. **The `18.1.8` in the June build plan is superseded**; do not chase it |
| An x86-64 `llc`/`opt` at that version | **NOT on this box.** No `llc`, `opt` or `clang` on PATH |
| The NDK's clang as a substitute | **NO. It is 21.0.0** on both NDK 29 and 30 — **a different major version, so a 20.1.8 codegen bug may not reproduce** |
| The IR for `0x82282490` | needs one load of the title with `dump_ir`; the **AOT precompiler runs at load**, so no scene navigation |

**So the blocking step is obtaining LLVM 20.1.8 host tools, not device time.**
Once they exist, `llc -mtriple=aarch64-linux-android -mcpu=cortex-x3` on the
dumped IR reproduces the device's codegen path **on this box**, exactly.

**Do not attempt the repro with NDK clang 21** and report a negative from it.
**That would be measuring a different compiler**, and this repo has a rule about
searching with the wrong instrument.

### And the ledger already cites Box64

`cpu_10x_stack_ab_thor` names its next step as *"the per-call-site host return
trampoline (**Box64 CALLRET/SEP analogue**)"*. **xenia was already reading Box64
as a design reference**, which is a fleet cross-pollination this repo had not
recorded — and it strengthens the case for reading Box64 properly before
deciding anything about IRs.

**One related lever is already dead.** `EOR3/BCAX fusion for VMX bitwise chains`
is **`DEAD`** as of 2026-08-06. **xenia uses the device's vector features and
that particular fusion still did not pay.**

## 13. Measure instruction inflation — the recipe exists, one flag, one function

**This is the number the whole IR question turns on, and this project has never
had it.** See
[`research_log/20260823_1642_ir_in_emulators_literature.md`](research_log/20260823_1642_ir_in_emulators_literature.md)
section 8: inflation predicts slowdown by regression, state-of-the-art DBTs sit
at **1.46 or worse**, and attacking it measured **2.99x to 7.12x** on QEMU.

**xenia already has the instrument. No fork needs modifying.**

### The flags

```
--disassemble_functions=true
--disassemble_function_filter=82282490
```

**The filter takes addresses or inclusive ranges and dumps without enabling
global disassembly**, so this is one function and a small log — not a flood.

### What one run gives, and why it beats the literature's single number

`kDebugInfoAllDisasm` turns on **four** stages at once:

| Flag | Output |
| --- | --- |
| `kDebugInfoDisasmSource` | guest PowerPC |
| `kDebugInfoDisasmRawHir` | **HIR before optimisation** |
| `kDebugInfoDisasmHir` | **HIR after optimisation** |
| `kDebugInfoDisasmMachineCode` | host ARM64, **tagged with the guest address** |

**So inflation decomposes into three stages instead of being one ratio:**

- **A — expansion into the IR:** raw HIR ops per guest instruction
- **B — what the optimiser earns:** optimised HIR against raw HIR
- **C — codegen:** host instructions per optimised HIR op
- **Total:** host instructions per guest instruction

**Stage B is the one that answers the IR question directly.** If the optimiser
removes little, the IR is pure cost. If it removes a lot, the IR is paying for
itself and the literature's conclusion — that QEMU is slow because it optimises
little, not because it has an IR — holds here too.

### Counting is mechanical

`DumpMachineCode` emits **one guest address, then the host instructions it
produced**, via Capstone:

```
82282490 <hostaddr>  mnemonic  operands
         <hostaddr>  mnemonic  operands
82282494 <hostaddr>  mnemonic  operands
```

**Inflation = total instruction lines / lines that carry a guest address.** A
five-line script, no judgement calls.

### CORRECTED 2026-08-25: this does NOT need the device

**The first half stands.** The a64 backend is compiled inside
`#elif XE_ARCH_ARM64`, so an x86-64 build does not contain it and would measure
the wrong emitter.

**The claim "there is no ARM64 host here other than the Thor" is false.** Checked
in WSL Ubuntu on this machine:

```
/usr/bin/aarch64-linux-gnu-g++      <- the compiler premake5.lua's --linux-arm64 names
/usr/bin/qemu-aarch64               <- the runner xenia's own ABI-poison test assumes
```

> **The route: cross-build the PPC test harness for ARM64 Linux, run it under
> `qemu-aarch64` with `--cpu=arm64` and the disassembly flags, count.**

**Timing under qemu would be meaningless, and this measurement is not a timing.**
**Inflation is a STATIC COUNT of emitted instructions** — total instruction lines
over lines carrying a guest address — **which is exactly why this route serves
this entry and would serve none of the benchmarks in this queue.**

**THE TOOLCHAIN HALF IS NOW TESTED, 2026-08-25.** A C++20 program was
cross-compiled and executed on this machine, touching no fork:

```
aarch64-linux-gnu-g++ -O2 -std=c++20 -static -o t t.cpp
file t   -> ELF 64-bit LSB executable, ARM aarch64
qemu-aarch64 t   -> "running as aarch64"
```

**So `__aarch64__` is defined at compile time and the binary executes.** That is
exactly what entry 13 needs, because `XE_ARCH_ARM64` gates the a64 backend into
the build and the emitter then runs.

**`-static` is load-bearing and is the next obstacle.** qemu-user running a
DYNAMICALLY linked ARM64 binary needs the ARM64 sysroot and `QEMU_LD_PREFIX`;
a static link sidesteps it. **xenia's cross build will be dynamic**, so either
`QEMU_LD_PREFIX` is set to the aarch64 sysroot or the harness is linked static.
**Named so it is met deliberately rather than discovered as a crash.**

**NARROWED AGAIN, same day.** Checked what the fork build actually needs in that
WSL:

| Tool | State |
| --- | --- |
| `make`, `cmake`, `python3`, `pkg-config` | **present** |
| **`premake5`** | **MISSING — and it is xenia's build system** |
| the aarch64 sysroot | **present** — `/usr/aarch64-linux-gnu/lib/libpthread.so.0` |

> **The `-static` obstacle has an answer**: a sysroot exists, so
> `QEMU_LD_PREFIX=/usr/aarch64-linux-gnu` should serve a dynamically linked
> build. **Untested.**
>
> **And the remaining blocker is one apt package.** `premake5` joins `ccache`,
> `pkg-config` and `glslangValidator` on the known-host-tools list — **the third
> time today a blocker resolved to a missing host tool rather than to the thing
> it was written as.**

**STILL NOT ESTABLISHED: whether the `--linux-arm64` path configures and builds
once `premake5` exists, and whether the PPC harness runs under qemu-user with
threads.** **The toolchain is proven, the sysroot is present, the build system is
absent.**

**One practical note**: WSL path translation failed with a `D:\` drive present in
the environment, and the check had to run from a translatable directory.

**But it is a cheap run**: load a title, dump one function, pull the log. **No
scene navigation, no timing, no thermal soak, no A/B.** It is a capture, not a
benchmark, so none of this queue's measurement traps apply.

**Prediction: total inflation between 2 and 4, and stage B removing 20-40%.**
The guest is PowerPC on ARM64 — **RISC to RISC, the easy case** — so it should
sit well below the CISC-to-RISC figures. **If total inflation comes in near or
below 1.46, the IR is not costing this project anything and the whole thread
closes.**

### Do the same for Cemu, and the comparison is nearly controlled

**Cemu is also PowerPC on ARM64 and also has an IR** (`IML`, 6,327 lines).
**Same guest family, same host, different IR design** — the closest to a
controlled comparison this fleet allows.

**There is no same-guest IR-against-no-IR pair anywhere in the fleet**, so a
cross-fork number measures the guest as much as the IR. **Say so in any result.**

## 14. Carryless multiply for texture swizzle — **CLOSED 2026-08-23, no device needed**

**The read happened. `PMULL` is dead, for two independent reasons.**

**The per-pixel address is a lookup table.** azahar's `MortonInterleave` is two
8-entry `constexpr` tables — **two L1 loads and an add**. A carryless multiply
needs operands moved into a vector register and back, **and cross-file transfers
are what the A710 stalls three cycles on.**

**And the bulk path already uses a better instruction.** azahar moves whole tiles
with **`vld2q_u64`** — ARM's structured `LD2` load, which **de-interleaves as it
loads**, in the memory operation, before the data reaches a register. **`PMULL`
can only act on data already loaded.** Byte fixups use **`vqtbl1q_u8`**, a `TBL`
permute.

> **The interleave problem is solved by the load unit, not the multiplier.**

**xenia solves it a third way: on the GPU**, in 171 `texture_load_*_cs` compute
shaders.

**Entry 14 predicted three outcomes and said two would close this with no
device. The outcome was "already hand-vectorised", which was one of them.** The
read cost about twenty minutes.

See [`research_log/20260823_1812_swizzle_read.md`](research_log/20260823_1812_swizzle_read.md).

### What replaces it: a PROPAGATE, and it still needs a read first

**Vita3K has zero NEON in its texture path. Cemu has two.** The Vita stores
textures swizzled, so Vita3K is de-interleaving in scalar code **while azahar's
`LD2`-plus-`TBL` technique sits in the same fleet, working and
GPL-2.0-or-later.**

**Not proven.** What is measured is the **presence of intrinsics**, not that
Vita3K's swizzle is hot or that its textures are large enough for a vectorised
copy to pay. **Read Vita3K's path first** — the rule that just killed `PMULL`.

## The original entry, kept for the record

## 14b. Carryless multiply for texture swizzle — READ FIRST, do not build

**The best unexploited hardware-repurposing candidate in the fleet**, and the
only one that would land in a **shared** hot path rather than one backend's
lowering.

**Unswizzling a console texture is bit deinterleaving. `PMULL` does that in one
instruction. Zero forks use it**, across 33 to 71 swizzle files each.

**The next step is not a device run and not a patch. It is reading one fork's
swizzle code.** The counts above are file matches; nobody has opened them.

**Three outcomes, and two of them close the question with no device at all:**

| What the code turns out to be | Verdict |
| --- | --- |
| a **lookup table** | **`PMULL` probably loses.** A table hit in L1 beats an instruction that needs operand setup |
| **already hand-vectorised** | **nothing to win** |
| **scalar bit math in a loop** | **the candidate is live** — then measure |

**Prediction if it does reach the device: `FLAT`.** Two reasons, both from this
repo's own record. **Both previous instruction-level cleverness attempts here
measured null** — `EOR3`/`BCAX` fusion `DEAD`, `TBL2`-for-`TBX2` 0.555 against
0.555. And **texture swizzle happens once per texture upload, not per frame**,
so it is amortised by the texture cache that every fork already has.

**The honest case for doing it anyway** is that it is in **pipeline 2**, the
shared upload path, so a win would be a win for every backend at once. **That is
a structural argument, not an instruction-level one**, and structural arguments
are the ones with a track record here.

## 15. Does a shipped pipeline cache remove first-play stutter

**Gate: answerable without editing a fork.** Fossilize is MIT and is a **Vulkan
layer**, so it records a backend without modifying that backend's source, which
is what the standing rule requires. **A capture and replay harness still has to
be written**, and the layer has to be installed on the device. See
[`research_log/20260823_2110_shipped_pipeline_cache.md`](research_log/20260823_2110_shipped_pipeline_cache.md).

**Why it matters.** A Vulkan pipeline cache is driver-specific, so no emulator
can ship one to its users. **The Thor pins one driver**, which is the same
property that lets Valve distribute precompiled shaders for the Steam Deck.
**Nothing else in emulation has that property.**

**The Cemu caveat is answered and this entry survives it.** Cemu disables Valve's
layer because Steam's shader precaching **conflicts with Cemu's async shader
compilation**, producing "graphics or models failing to render". That is two
owners of pipeline caching in one process, and Cemu could not turn off a layer
the Steam client injected. **This app owns both sides.**

**Two rules come out of it and both bind this run:**

- **Do not record through a layer while the backend compiles pipelines
  asynchronously.** Record from inside the shared device layer, or turn async
  compilation off for the capture and say so in the result.
- **Async compilation and a warm cache attack the same problem.** Async hides a
  compile behind a placeholder; a warm cache means there is nothing to hide.
  **Measuring one while the other moves measures neither.** State the async
  setting and hold it fixed across arms.

See [`research_log/20260823_2340_why_cemu_disables_fossilize.md`](research_log/20260823_2340_why_cemu_disables_fossilize.md).

**The run:** on one backend and one title, capture a cold first-play session and
a second session with the cache warmed by the first. Record frame time
percentiles, not the mean.

**A frame comparison against a reference is mandatory, not optional.** Cemu's
failure mode with a second cache owner was **silent visual corruption**, so a run
judged on timing alone can pass while rendering a broken game.

**Expected signature if it works:** **the 99th percentile frame time falls, the
mean does not move, and the frames match the reference.** Stutter lives in the
tail. **A mean improvement would
be evidence of something else and should be treated as `CONFOUNDED`.**

**Also record, because they decide feasibility and nobody has measured them:**
cache size on disk per title, and replay time to warm it. **A cache too large to
ship, or too slow to replay, kills the idea whatever the frame numbers say.**

**Prediction: `WIN` on the tail, `FLAT` on the mean.** This is the one entry in
this queue predicted to win, and it is predicted to win on the axis this repo has
not been measuring.

## 16. FP16 in ARMSX2's frame generation — read before running

**A two-line change, named by ARMSX2's own comment**, gated on a device layer
that never asks for the extension.

`pcsx2/GS/Renderers/Vulkan/FrameGen/LsfgShaders.cpp` records that PCSX2's Vulkan
backend never requests `VK_KHR_shader_float16_int8`, so the ported frame
generator loads its **fp32** variant. **eden runs the fp16 path**, and xenia
probed `shaderFloat16 = 1` on this device on 2026-06-20.

**This is not runnable yet and must not be run as a fork edit.** The standing
rule forbids modifying ARMSX2 unless asked for it by name. **The measurement
belongs to the shared device layer**, which does not exist. Recorded here so it
is not lost.

**Expected signature: lower GPU frame time in the frame-generation pass, no
change to anything else.** **Watch for a quality regression** — fp16 is a
precision change, so a golden-image comparison must run beside the timing.

## 17. ARMSX2's persisted VU program cache — the fork wrote this entry itself

**This is the only queued experiment whose gate was stated by the code under
test.** `pcsx2/Pcsx2Config.cpp:463`:

```cpp
EnableVUProgramCache = false; // default off; opt-in until the on-disk cache
                              // is validated on the target hardware
```

**The target hardware is the Thor.**

**What it is.** A persisted JIT cache for the PS2 vector unit, 2,576 lines with
three test files, content-addressed `.vuprog` payloads, and a placement-relative
fixup table that makes the emitted vixl output relocatable. See
[`research_log/20260823_2205_translate_once_ship_it.md`](research_log/20260823_2205_translate_once_ship_it.md).

**Why it matters beyond ARMSX2.** It is the CPU half of the persist rule. **If it
holds on this device, a translated-code cache is shippable between users**, the
same way the pinned driver makes a pipeline cache shippable. Entry 15 is the GPU
half of the same question.

**Do not enable it by editing the fork.** It is a config bool, so it is set
through settings, not through a source change. **The standing rule forbids
modifying ARMSX2 unless it is asked for by name.**

**The run:** one title, cold launch with the cache off, cold launch with it on
and empty, then a third launch with it warm. Record time to first frame, VU block
compile count, and cache size on disk.

**Expected signature: the third launch compiles far fewer VU blocks than the
second, and time to first frame falls.** **Correctness is the real gate, not
speed** — the test suite already asserts a bit-identical post-state in-process,
so **any guest-visible difference on device is a stop, not a regression to
tune.**

**Prediction: `WIN` on block compile count, `OPEN` on time to first frame.** VU
recompilation is not obviously the dominant cost of a PS2 launch, and nothing
here has measured what is.

**Also read before running:** how the cache handles self-modifying guest code.
It was searched for and not found, which means unread rather than absent.

## 18. Does this device execute AArch32 at EL0 — one property read

**The cheapest entry in this file, and it gates the largest open question found
on 2026-08-23.**

```sh
adb -s "$THOR" shell getprop ro.product.cpu.abilist
adb -s "$THOR" shell getprop ro.product.cpu.abilist32
```

**Why it matters.** eden runs Switch guest code **natively** through NCE, with no
recompiler, because the guest ISA is the host ISA. **Three more fleet guests are
ARM but not ARM64**: Vita is ARMv7, 3DS is ARMv6, DS is ARMv4 and ARMv5. Whether
the same idea can even be attempted for them turns on whether this device runs
32-bit ARM code at all. See
[`research_log/20260823_2250_eden_nce_deletes_the_translator.md`](research_log/20260823_2250_eden_nce_deletes_the_translator.md).

**Do not infer it from a build file.** GameThor and melonDS both build
`armeabi-v7a`, and building an ABI is a choice rather than proof the device runs
it.

**Prediction: `abilist32` is empty.** ARMv9 cores dropped AArch32 in stages and
the Cortex-X3 is one of them, and Android does not usually advertise a 32-bit ABI
that only some cores can run. **Stated so the run can fail.**

**If the prediction is wrong, the question reopens for three backends**, and that
is worth more than anything else in this file.

## 19. ARMSX2 mobile ROV — tile-native ordered depth feedback

**A named flag, a stated fallback, a byte-identical off state, and a capability
this device was measured to have.** See
[`research_log/20260824_0210_pass_merging_exists_twice.md`](research_log/20260824_0210_pass_merging_exists_twice.md).

**What it does.** `HWROV` makes ARMSX2 read the depth buffer **in tile**, through
`subpassLoad` on a depth input attachment, instead of copying depth to a colour
render target. Its own comment: **SW-Z, DATE, alpha-test and AA1 depth passes
"fuse in-pass rather than round-tripping".** On a tiler the round trip is the
cost.

**Why it is runnable.** It needs an ordered in-tile depth read, which means
**ROAA on the depth aspect**. xenia probed this device on 2026-06-20 and recorded
**`roaa_color=1 roaa_depth=1`** — and the same probe read **false** three days
earlier on an older Turnip build, which is itself the argument for the pinned
driver.

**Set it through settings, not by editing the fork.** `HWROV` is a config
toggle. **The standing rule forbids modifying ARMSX2 unless asked for by name.**

**Expected signature: lower GPU frame time on scenes using software Z, DATE or
alpha test, and no change elsewhere.** ARMSX2's comment says the depth half is
**read at device init**, so **each arm needs a fresh process** — the same trap as
the per-game driver override and cached device properties.

**Correctness is a gate, not a footnote.** The toggle-off path is described as
byte-for-byte the well-tested fallback, so **any pixel difference with it on is a
stop.** Compare frames against a reference.

**Prediction: `OPEN`.** ARMSX2 gated it and did not measure it, this repo's
manual-derived levers are thirteen for thirteen refuted, and **the depth
round-trip may not be where this backend's frames go.** Profile first.

## 20. Fix xenia's save-state deadlock — NO DEVICE NEEDED, and it unblocks others

**Listed here because it gates device work, not because it needs the device.**

xenia's own audit records it:

> the save-state path is itself blocked (`SaveToFile` hangs in
> `kernel_state_->Save`, a global-lock deadlock during `Pause` /
> `GetObjectsByType`). **THE META-FIX: fix the save-state hang -> load the same
> BD foliage scene deterministically -> unblocks the A/B for VRS *and* ROAA
> *and* bindless on BD.**

**Its own fork calls this the highest-leverage GPU-validation unblock available,
and this repo's plan does not mention it.**

**Why it matters beyond xenia.** Three backends have no deterministic scene:
Cemu and eden have no savestate at all, and xenia's deadlocks. **Without one, the
only route to a scene is pressing through cutscenes, whose measured noise floor
is about +/-50%.** See
[`research_log/20260824_0255_three_backends_cannot_be_measured.md`](research_log/20260824_0255_three_backends_cannot_be_measured.md).

**It is a lock-ordering bug with a named location.** It can be diagnosed by
reading, and any fix is xenia work — **which the standing rule forbids unless
that fork is asked for by name.** Recorded so it is not lost.

**Expected signature: `SaveToFile` returns instead of hanging, and a restored
state reaches the same scene twice.** Then the workload noise floor drops from
the cutscene band to the savestate band, about +/-5%.

**Prediction: `OPEN`.** A global-lock deadlock during pause may be a symptom of
the object model rather than of the save path.

## 21. WITHDRAWN 2026-08-24 — Does Turnip honour LAZILY_ALLOCATED

**WITHDRAWN.** xenia measured on 2026-08-16 that **framebuffer bandwidth is not
the constraint on this device** — flat across a 36x working set, no cache cliff,
forced GMEM never beating autotune. **That kills transient and `LAZILY_ALLOCATED`
attachments as a performance play**, and its routing table says **"Do not
re-derive them."** **The observation that no fork binds the memory type still
stands; the reason to act on it does not.**

**Kept, not deleted, because the distinction is the useful part.**

**The Thor reports a memory type
`DEVICE_LOCAL | LAZILY_ALLOCATED`, 11,441 MB — **Adreno's on-chip tile memory.**
An attachment backed by it need never touch DRAM.

**No fork binds it** — searched all eight forks' own source for
`LAZILY_ALLOCATED` and `eLazilyAllocated`, vendored trees excluded, and read the
one hit: rpcsx's is a **log-format string** in its memory-type dump, not a use.
Vita3K is the only fork that sets `TRANSIENT_ATTACHMENT` usage and `DONT_CARE`
ops, so it does two of the three parts. See
[`research_log/20260824_0940_one_heap_and_nobody_uses_tile_memory.md`](research_log/20260824_0940_one_heap_and_nobody_uses_tile_memory.md).

**The probe:** allocate a transient depth attachment with
`VK_MEMORY_PROPERTY_LAZILY_ALLOCATED_BIT`, then read
`vkGetDeviceMemoryCommitment`. **A driver is allowed to commit the whole thing
anyway.**

**Expected signature: committed bytes stay far below the allocation size while
the pass runs.** If the driver commits it all, the idea is closed and no A/B is
needed.

**Prediction: it is honoured.** Adreno is a tiler and this is the memory type that
exists for exactly this. **Stated so it can fail.**

## 22. NARROWED 2026-08-24 — Direct-write uploads on a unified-memory device

**NARROWED.** The bandwidth result above is about **framebuffer** traffic, not
**upload** traffic, so this entry is not killed by it — **but its prior is much
worse.** A device measured as ALU-bound and insensitive to a 36x framebuffer
working set is unlikely to be waiting on a staging copy. **Re-prioritise
accordingly and expect `FLAT`.**

**Gated on reading the fleet's upload paths.**

**The Thor has one heap.** Types 0-2 are the same 11.4 GB of DRAM described three
ways, and **type 1 is device-local, host-visible, coherent AND cached.** So the
staging copy every fork performs on the way to device-local memory **has no
destination that differs from its source.** Every fork stages, from 5 files in
Vita3K to 32 in eden.

**Cached is the precondition and it holds** — an uncached or write-combined
mapping would have sunk this.

**The run:** one backend, one title, texture-heavy scene, staged upload against a
direct write into a type-1 mapping. **Hold the async-compile and cache settings
fixed across arms**, and use a **work-normalised metric** — bytes uploaded is the
natural denominator and the change does not touch it — so the scene may vary.

**Expected signature: fewer bytes moved and lower GPU busy at the same clock.**
Read `/sys/class/kgsl/kgsl-3d0/gpubusy`, which resets on read.

**Prediction: `OPEN`, leaning `FLAT`.** rpcsx's own words: **host-visible does not
mean fast to write**, and cached-and-device-local still has to beat cached staging
plus a driver-side copy. **And this repo's record is that reasoned optimisations
lose.**

**The confound that would fake a win:** a direct write moves the copy from the
GPU's timeline onto the CPU's. **GPU busy falling while frame time is unchanged is
not a win** — check CPU cores-busy in the same window.

## 23. Is `LDAPR` actually faster than `LDAR` on this SoC

**Small, and it is the only target feature that changes codegen by itself.**

Adding **`+rcpc`** to the compile target makes clang emit **`ldapr`** instead of
**`ldar`** for every `memory_order_acquire` load — **verified on this box, no
intrinsic, no source change.** A `seq_cst` load correctly keeps `ldar`. The device
reports `lrcpc` and `ilrcpc`. See
[`research_log/20260824_1130_rcpc_is_missing_from_our_target.md`](research_log/20260824_1130_rcpc_is_missing_from_our_target.md).

**Why it needs the device.** `LDAPR` is architecturally weaker than `LDAR`, so it
**should** be no worse — **and this repo's record is fourteen manual-derived
predictions refuted.** *Should* is not a measurement.

**The run:** a microbenchmark first, since this is an instruction-level question —
an acquire-load loop compiled both ways, on the X3 and on an A710, reporting
ns/iteration. **Only if that separates should a whole-backend A/B follow.**

**Expected signature: `ldapr` equal or slightly faster, with the gap larger under
contention**, because RCpc does not order against unrelated prior stores.

**Prediction: `FLAT` on an uncontended loop, `OPEN` under contention.** On a
single-threaded loop there is nothing for the stronger ordering to cost.

**The confound that would fake a win:** the two arms are different builds, so
**anything else that differs between them is attributed to this flag.** Build both
from the same tree with only the flag changed, and diff the disassembly of the
benchmark to confirm exactly one instruction differs.

## 24. Does this device report `textureCompressionBC` — one boolean

**The cheapest entry after 18, and it closes or opens a whole finding.**

**Read `VkPhysicalDeviceFeatures::textureCompressionBC` on the Thor.** Nothing in
this fleet has captured it — searched xenia's device baseline and its Turnip
feature-gap audit.

**Why it matters.** ARMSX2's BC7 texture decompressor has an SSE2 fast path and
**no ARM path at all** — searched the file for `ARM`, `NEON`, `aarch64` and
`__arm`, zero matches. Its two call sites are both the **HD texture pack
loader**, and the decoder only runs when the GPU cannot sample BC7 directly. See
[`research_log/20260824_1300_x86_only_fast_paths.md`](research_log/20260824_1300_x86_only_fast_paths.md).

**Prediction: `false`.** Adreno implements ASTC and ETC2 rather than the BC
family. **Stated so it can fail** — and if it is `true`, BC7 uploads compressed,
the CPU decoder never runs, and the finding closes.

**If `false`, the follow-up is an applicability count, not a build**: how many
textures in a real PS2 pack are BC7 rather than PNG or DXT? **Per the `EOR3`
rule, count before writing a NEON decoder.**

## 25. Price `+lse`: does removing the runtime atomic dispatch move anything

**Everything except the price is already established, device-free.**

**Five of six shipped `arm64-v8a` binaries route every `std::atomic`
read-modify-write through a `__aarch64_*` outline helper** — a `bl` to a stub
that loads a global feature byte, branches, then executes the instruction. Only
xenia, which sets `+lse`, emits LSE directly at scale: **1,998 LSE against 53
outline calls, where ARMSX2 is 28 against 886.** Verified from
`compile_commands.json` to the disassembly: ARMSX2 compiles at
`-march=armv8-a+crc`, with no `+lse` in any configuration.

**The device reports `atomics` in `/proc/cpuinfo`, which is `FEAT_LSE`**, so the
dispatch decides something constant for the life of the device.

**The arm:** rebuild **one** fork with `+lse` added to `-march` and nothing else
changed. **Do not add `-mno-outline-atomics` on its own** — without `+lse` it
removes the upgrade path and leaves the `ldxr`/`stxr` loop unconditionally, which
`tools/target_check.py` probe 3 fails on.

**Prediction, FLAT — and it now has a REASON rather than only a prior.** rpcsx
struck through a per-reservation LSE redesign with the correction: **"LSE was
never the missing piece, because the cost is WHICH LINE an atomic touches rather
than HOW IT IS SPELLED."** **`+lse` changes the spelling.** So if it moves
anything it will be in **atomic-dense UNCONTENDED** code — not in the contended
guest-synchronisation paths that look most tempting. **Pick the arm accordingly.**

**Prediction, REVISED the same day: FLAT on frame time, held with less
confidence.** The original reason was that the atomic count is small relative to
the instruction stream. **Vita3K then supplied a count and it is not small:
25,276 outline call sites in its shipped library, of which `swp1_acq_rel` is
12,074 and `ldadd8_acq_rel` 10,518** — refcount traffic, not cold paths. **The
prediction stays FLAT because this fleet's record is thirteen manual-derived
predictions refuted**, but it is now a weaker prior, and **the arm to run is
Vita3K**, which has the fix and the before-and-after counts already.

**A caveat that decides which fork to test.** Vita3K's residual 682 call sites
are all inside **prebuilt vcpkg static libraries, which never see the fork's
`-march`**. **Cemu compiles none of its dependencies**, so a `-march` change
reaches almost none of its atomics — **testing Cemu would measure the flag not
applying, not the flag not helping.** **What could produce a real result is
contention, not throughput** — Cemu's own `CMakeLists.txt` argues `ldxr`/`stxr`
"livelocks harder under contention, which is exactly the multi-core guest case",
so the arm most likely to move is a **multi-core guest under load**, not a menu.

**Gates.** Same route, same scene, fresh process per arm, `status=Discharging`,
report `[min..max]` and never a mean of one run. **A savestate route has a ~5%
noise floor**, so a claim under 5% there is not a result.

**Cheaper check first, and it needs no device:** count outline call sites in a hot
guest-thread function with `llvm-objdump`. **If the hot path holds no atomics, the
lever is dead before it is built** — the applicability rule that already closed
the Armv8.4 question above.

## 26. Two Turnip behaviours, one game-free probe session

**ARMSX2's driver profile records Turnip on Adreno as breaking BOTH forms of
attachment self-read** — `BrokenSubpassFeedback` and
`BrokenAttachmentFeedbackLoopLayout` — with **no version bound**, so the rule
applies to every Turnip build. Its workaround is
`UseRenderTargetCopyForFeedback`: **a full render-target copy per feedback
draw**, which also **turns texture barriers off and disables framebuffer fetch**,
because they are the same in-tile read.

**Why it is worth device time.** `CLAUDE.md` names PCSX2 2.6.0's feedback reads
as a technique to take, on the grounds that Vulkan has attachment feedback loops
and the PS2 reads render targets constantly. **This rule says the mechanism does
not work on the pinned driver.** The premise of the transfer is exactly what is
in question.

**PROVENANCE READ, and it is the reason to run rather than a reason not to.**
The rule comes from **ARMSX2 #442**: with an HD texture pack, *Tales of the
Abyss* lost its entire 2D text layer, and a **device A/B on Turnip / Mesa 26.1.2
with an Adreno 650** showed both in-pass forms dropping the content while a
separate RT copy rendered correctly.

> **That is a real measured defect on a DIFFERENT GPU generation and an OLDER
> Mesa.** The Thor is Adreno 740; the pin is Mesa 26.3.0 or MrPurple T30, whose
> changelog claims it **fixed a7xx support**. **The rule carries no version
> bound because it was written from one A/B, not because it was re-tested.**

**The run.** On the pinned Turnip build, a Vulkan probe that (a) creates a
subpass with an input attachment reading the colour attachment, and (b) creates a
pipeline with `VK_PIPELINE_CREATE_COLOR_ATTACHMENT_FEEDBACK_LOOP_BIT_EXT`, and
renders a known pattern through each. **Compare against the reference the CPU
computes.** No game, no emulator.

**Prediction: both still fail.** Stated so it can fail — and **if either now
works, the PCSX2 feedback-read technique becomes available and the render-graph
work gains its in-pass read.**

**Gates.** Verify the driver actually loaded and report its `driverID` and
version string with the result; a probe that silently ran on the stock driver
answers a different question. **Run the same probe on the stock Qualcomm driver
too** — ARMSX2's comment says the reporter saw the same failure there, and
confirming that makes it an Adreno property rather than a Mesa regression.

**The device-free step is DONE**: all 30 rules listed, no superseding Turnip
rule, and the provenance comment read. **Nothing further can be learned without
the device.**

### Second test in the same session: does `vkGetFenceStatus` still block?

**Added here rather than as entry 27, because this queue's own rule says to
resolve an entry before opening one beside it** — and both questions are *"does
the pinned Turnip still do X"*, answerable in one game-free probe.

**The behaviour:** xenia records that **on Turnip-over-KGSL a fence status query
on an IN-FLIGHT fence blocks until that submission retires**, because Mesa's
`tu_knl_kgsl.cc` passes ioctl `timeout=0` and **KGSL documents `timeout==0` as
"wait forever"**. Cost when a pre-poll hits it: **a full GPU frame of CPU-GPU
serialisation per frame open, 46.9 ms on one title.**

**The test.** Submit work with a known long duration, then **time
`vkGetFenceStatus` on the fence for that submission.** Vulkan requires it to
return `VK_NOT_READY` immediately.

| result | meaning |
| --- | --- |
| returns in microseconds | **the behaviour is gone on this build** |
| **returns after the submission completes** | **still present** |

**Prediction: still present.** It is a KGSL kernel behaviour reached through
Mesa's ioctl, **not a Turnip bug that a Turnip release would fix** — so a newer
Mesa is unlikely to change it. **Stated so it can fail.**

**Why it matters more than entry 26's first test.** Attachment self-read affects
a technique this project *might* adopt. **This one affects every resource pool
that asks whether a fence is done** — the texture path, the memory budget owner,
the pipeline cache — **all of which the shared layer is meant to own for seven
backends at once.**

**Gate: report `driverID` and the driver version string with both results**, and
run both tests in the same session so the driver identification covers them.

## Not ready to run

These need a decision or a build first, not device time.

- **Any shared-layer measurement.** Nothing is extracted yet.
- **Savestate fixture regeneration.** Needs the harness to exist.
- **ADPF tuning.** Measure without it first, or the hint is tuned against an
  unknown baseline. ADPF is currently disabled on this device by persisted
  config.

**ONE FACT ADDED 2026-08-25, and it does NOT answer the entry.** xenia enabled
**`VK_KHR_dynamic_rendering_local_read`** on this device and probed it with a
title running: **Burnout on verified Turnip 26.3.0 reports
`dynamic_rendering_local_read=true`.** So the modern in-pass attachment-read
extension is **exposed and enabled on an Adreno 740 with Mesa 26.3.0**, where the
ARMSX2 rule was written from an **Adreno 650 on Mesa 26.1.2**.

> **That is availability, not correctness.** azahar confirmed all four
> `extended_dynamic_state3` blending features on this same device, rendered the
> loop correctly, and measured nothing — **"extension availability is not
> optimization evidence."** The probe above raises the value of running this
> entry; **it does not substitute for it.**

**And it raises the stakes.** XenDroid has a **16-commit series** building in-pass
EDRAM resolves on that extension, and xenia's tree has **neither the extension
nor any in-pass path**. **If this probe finds in-pass reads correct on the 740,
that series becomes portable work against a wall this project has already
measured — 45 EDRAM transfers per frame, 27 of them pass breaks.** See
[`research_log/20260825_0415_the_fix_for_the_measured_pass_wall_exists_in_a_sibling_fork.md`](research_log/20260825_0415_the_fix_for_the_measured_pass_wall_exists_in_a_sibling_fork.md).

## 27. `GCM=1`: an upstream Turnip shader optimisation that ships default-off

**Found 2026-08-24 while assessing the Aurora driver, and it is separable from
it.** Upstream Mesa's `src/freedreno/ir3/ir3_nir.c` runs NIR global code motion
behind an environment variable:

```c
gcm = debug_get_num_option("GCM", 0);
if (gcm == 1)      progress |= OPT(s, nir_opt_gcm, true,  true);
else if (gcm == 2) progress |= OPT(s, nir_opt_gcm, false, true);
```

**Default 0, so it is off in every Turnip build anyone ships.** Verified present
in the binary of **both** the 2026-08-20 Aurora build **and** the May 2026
`Turnip_v26.0.0_R8` build, so **it needs no particular driver** — `ir3_nir.c` has
not changed upstream since 2026-07-20.

**Why it is worth device time.** It is a shader compiler pass, so it changes
**what the GPU executes**, not how the driver is configured. This project's own
frame anatomy work says a title can be ALU-bound on this device, which is exactly
the case a code-motion pass targets. **And it costs one environment variable on
the pinned build.**

**The run.** Three arms on one pinned driver, one scene, one session:
`GCM` unset (control), `GCM=1`, `GCM=2`. **The two modes differ in the second
argument to `nir_opt_gcm`, so they are genuinely different passes, not a
strength dial.** Report GPU busy from `/sys/class/kgsl/kgsl-3d0/gpubusy`, frame
time, and **shader compile time**, because a code-motion pass costs compile time
to save execution time.

**Prediction: FLAT on frame time, and a measurable INCREASE in shader compile
time.** Stated so it can fail. The reasoning for the pessimistic arm: **the knob
is upstream, old and default-off**, which usually means it was not a clear win —
and `nir_opt_gcm` is a general NIR pass rather than an a7xx lowering.

**The confound that would fake a win.** A pass that changes shader code changes
**pipeline cache keys**, so arm 2 and arm 3 each start with a cold shader cache
while the control may be warm. **Clear the shader cache before every arm**, or
the control wins on cache state rather than on code quality. This is the same
trap as the persisted-config rule.

**HOW TO SET IT, answered 2026-08-25 — the mechanism is shipping in XenDroid.**
The app sets the driver's environment from inside its own process, before Vulkan
initialises, and logs what it set:

```c
setenv("TU_DEBUG", tu_debug.c_str(), 1);
XELOGI("Set TU_DEBUG={} for the Turnip Vulkan driver", tu_debug);
```

**`GCM` is read the same way — `debug_get_num_option` — so the same route
works.** Two details to copy: **check whether the user already specified the
flag** before appending, so an explicit setting is neither duplicated nor
overridden; and **log the final value**, which is this project's
verify-the-emitted-artefact rule applied to an environment variable and is
exactly what the gate below asks for.

**Gates.** Confirm the variable reached the process — read it back from the
app's own environment, not from the shell that launched it, because
`adb shell` and the app are different processes. **Prove the instrument first:**
if arm 2 and arm 3 produce byte-identical shader binaries, the variable did not
apply, and the run measures nothing.

**Cheap, game-free alternative if device time is short.** Compile a fixed shader
corpus under each arm and **disassemble the output**. If the instruction counts
are identical, the pass is doing nothing on these shaders and no frame
measurement is needed. That is the applicability-before-optimality rule, and it
needs no scene.

See [`research_log/20260824_2030_the_aurora_driver_verified_from_the_artefact.md`](research_log/20260824_2030_the_aurora_driver_verified_from_the_artefact.md).
