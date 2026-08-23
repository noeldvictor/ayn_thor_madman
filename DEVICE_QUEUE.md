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
| **temperature must rise** | no heating means an idle or menu scene, so the run is invalid whatever the counter said |
| query the experiment ledger first | `python tools/exp_ledger.py check "<keyword>"` in xenia-thor |

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

## 2. Pin the Turnip driver

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

## Not ready to run

These need a decision or a build first, not device time.

- **Any shared-layer measurement.** Nothing is extracted yet.
- **Savestate fixture regeneration.** Needs the harness to exist.
- **ADPF tuning.** Measure without it first, or the hint is tuned against an
  unknown baseline. ADPF is currently disabled on this device by persisted
  config.
