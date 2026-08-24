# 74% of cycles in a nop-spin, and the cause is an x86 timing constant

**Goal: read `rpcsx/docs/arm64/lv2-ppu-spin.md`, whose title claims 74% of all
cycles are a spin.**

**It holds, it is measured properly, and it changes this repo's spin-wait section
from an instruction debate into a timing debate.**

**It also carries the best experiment prediction in the fleet and an instrument
lesson this project needs.**

## The measurement, and why it is trustworthy

Title screen holding 60.01 fps. `simpleperf record -f 1000 -g --duration 25`,
**31,657 samples, 0 lost**, symbolized against the **matching unstripped library
with the build ID verified rather than assumed**.

| Overhead | Symbol |
| --- | --- |
| **47.82%** | `sys_event_queue_receive` |
| **26.09%** | `_sys_lwcond_queue_wait` |
| 1.96% | `[kernel.kallsyms]` |

**73.9% of all CPU cycles in two lv2 wait functions**, across three PPU threads.

**Three discipline points worth copying:**

1. **The run was verified to be in the right phase first** — a grep for "home
   menu" and "being paused" returned zero, so no phase mismatch.
2. **The symbolization was verified**, build ID matched by
   `binary_cache_builder.py`.
3. **The call graph proves it is spinning rather than blocking, using a contrast
   case in the same profile.** `cpu-cycles` only samples a running thread, so a
   blocked thread contributes nothing. Both hot functions report **self time,
   99.3% "hit in function", and neither reaches `atomic_wait_engine::wait`** —
   while `sys_timer_usleep` at 3.73% **does** reach it, because it parks on a
   futex. **The two hot ones never get there.**

## The cause is a constant that was correct on x86

```cpp
for (usz i = 0; cpu_flag::signal - ppu.state && i < 50; i++) {
    rx::busy_wait(500);
}
```

**`rx::busy_wait` spins until the generic timer advances by its argument, and
`CNTFRQ_EL0` on this chip is 19.2 MHz.**

| | Ticks | Per call | Fifty calls |
| --- | --- | --- | --- |
| **Thor, `cntvct_el0` at 19.2 MHz** | 500 | **26.0 µs** | **1.3 ms** |
| x86, TSC at ~3 GHz | 500 | ~0.02 µs | **~1 µs** |

> **The same source line spins for 1.3 ms here and about 1 µs on the machine its
> constants were written for.**

**This is the x86 detour in its purest form.** Not an instruction selection, not
a register model — **a timing constant that assumed a fast free-running
counter.** `CLAUDE.md`'s detour section has an opcode example and a register-file
example; **this is the third kind, and it is the largest.**

**And it defeats the instruction debate.** `pause()` is `yield`, which retires
doing nothing on this core, so the 1.3 ms is spent issuing instructions at full
rate. rpcsx's own conclusion:

> `ISB` costs 23% more, `nop` is equivalent. **Neither of those is the fix — the
> instruction is not the problem, the 1.3 ms is.**

**`CLAUDE.md` records the yield-against-`ISB` numbers from this very fork and
records the tuned-pair rule.** It does not record this: **that the total spin
time is the thing, and it was inherited.**

**Eight sites, not one.** `sys_cond`, `sys_event`, `sys_event_flag`, `sys_lwcond`,
`sys_lwmutex`, `sys_rwlock` twice, `sys_semaphore`. **The shape is the whole
guest synchronisation layer**, and the profile merely caught the two this title
leans on.

## The instrument lesson, and it is the sharpest one yet

**Why three sessions of prior work missed a 74% cost:**

> `spin.md` records "93% of all emulator spin is the SPU `GETLLAR` wait". That
> number came from **the wait profiler, which counts the sites it was told to
> instrument** — all of them SPU-side. **These eight PPU sites were never
> instrumented, so they could not appear, and their absence read as evidence they
> were not there.**

> **A search that finds nothing and a search that searches nothing look
> identical.**

> **A sampling profiler has no such blind spot, which is exactly why it found
> this and three sessions of counter work did not.**

**This is the same failure as searching for a library name instead of a
mechanism**, and this repo has now met it three ways in two days. **The rule:
for "where does the time go", prefer a sampling profiler over instrumented
counters. Counters answer "how often did the thing I instrumented happen".**

## The prediction, which should be this repo's template

Its `Predicted — not measured` section has four parts, and `DEVICE_QUEUE.md`
entries have at most two.

**1. Mechanism, naming existing code.** Replace the fixed 1.3 ms nop-spin with a
**WFE park on the `ppu.state` cacheline**. `rx::spin_on_cacheline_once` already
implements `LDAXR`, `WFE`, `CLREX` and is used elsewhere in the fork. **A device
fact is stated: `FEAT_WFxT` is absent on this chip, so WFE cannot carry a
timeout** — but the loop rechecks its own conditions each pass.

**2. Magnitude, with the arithmetic shown.**

> Folklore holds 60.01 fps whether or not this spin happens, so **none of it is
> throughput and all of it is power.** Title screen is **2.23 cores busy against
> a 0.68-core idle floor**, so ~1.55 cores of real work, of which 74% is **about
> 1.15 cores**. A leaked-process measurement prices it: **210% CPU cost 1.88 W,
> so ~0.9 W per core**. **Predicted saving ≈ 1.0 W against a ~3.5 W loaded draw**
> — roughly **30% of system power**, and **a "cooler" win, not a "faster" one.**

**3. What would falsify it.** If the signal usually arrives *inside* the 1.3 ms
window, the spin is buying real latency and parking will show as worse frame
pacing. **Measure p95 frame time, not mean, because a capped 60 fps hides it.**

**4. The confound that would fake a win.** Sleeping instead of spinning may let
the scheduler **migrate the PPU thread off its big core onto an A510**.
Cores-busy would drop and power with it **while the emulator got slower**.
**Check per-cluster residency.**

> **Naming in advance the confound that would fake a win is something no entry in
> `DEVICE_QUEUE.md` does.** Adopt it.

## Two device facts to keep

- **`CNTFRQ_EL0` is 19.2 MHz on this chip.** Any code counting generic-timer
  ticks is 156 times slower per tick than a 3 GHz TSC.
- **`FEAT_WFxT` is absent**, so `WFE` cannot carry a timeout here.
- **Power reference: ~0.9 W per busy core, ~3.5 W loaded, 0.68-core idle floor.**

## Limits

- **All of it is rpcsx's, on the PS3 backend, on one title's title screen.**
- **The prediction is explicitly unmeasured.**
- **The 30% power figure is a prediction with stated arithmetic**, not a result.
- **PS3 is out of the packed binary and rpcsx is GPL-2.0-only.** The measurement
  method, the timing-constant lesson and the prediction template are ideas and
  transfer freely; **no code does.**
- **Searched. See below — one fork already has the cure.**

## The cure exists in another fork, and its comment names the disease

**Searched the fleet for spin loops with literal counts and for readers of the
generic timer.** ARMSX2 8 sites, xenia 11, Cemu 5, eden 3, Vita3K 2, azahar and
melonDS none.

**Vita3K is the one that derives a budget instead of counting iterations.**
`vita3k/util/include/util/spin_wait.h`, its own comments:

> On AArch64 this is `CNTFRQ_EL0` (**19.2 MHz on the Qualcomm parts we target**).
> Spin budgets derived from it are **wall-clock stable across cores and SoCs,
> unlike the x86-tuned iteration constants they replace.**

> The spin budget is derived from `CNTFRQ_EL0` where available **rather than from
> an iteration count tuned on x86.**

**It reads the register directly** — `mrs %0, cntfrq_el0`, with an MSVC path —
**and returns 0 when no such counter exists, so callers fall back to iteration
counts.** That fallback is the portability half, and on this device it is dead
code.

**Two forks independently recorded 19.2 MHz**, and **both name x86-tuned
constants as the cause.** rpcsx **retuned its counts by hand** against that
frequency; **Vita3K replaced counting with a wall-clock budget.** Retuning leaves
the structure that assumed a fast counter; deriving a budget removes it.

### And it resolves an apparent conflict about `ISB`

**Vita3K uses `ISB` as its backoff step**, citing RPCS3 PR 18151 as a **net power
win**. **`CLAUDE.md` records the opposite** — rpcsx measured `ISB` at **32x the
cost of `yield`** and a **+23% regression** when substituted.

**Both hold, and the difference is the pair.** `CLAUDE.md` already states the
rule: **the backoff instruction and the iteration count are one tuned pair.**
Swapping `ISB` in under counts tuned for a free instruction multiplies every
backoff. **Vita3K changed both at once** — `ISB` *and* a wall-clock budget — which
is the combination that works.

> **Take Vita3K's shape: a wall-clock budget from `CNTFRQ_EL0`, with the backoff
> instruction chosen to match it.** Not the instruction alone, which this repo
> already records as measured harmful three times.

## Sources

- rpcsx `docs/arm64/lv2-ppu-spin.md`, `docs/arm64/spin.md`,
  `docs/arm64/bench-results.md`
- rpcsx `kernel/cellos/src/sys_event.cpp:476` and the seven sibling sites
- Vita3K `vita3k/util/include/util/spin_wait.h`
