# The fleet-wide spin-wait audit

**Goal: check every host-side spin loop in the fleet for a `yield` that does
nothing, as [`CLAUDE.md`](../CLAUDE.md) asks.**

No device used. Reading only.

## The headline: the check is right and the implied fix is wrong

`CLAUDE.md` says `yield` is a no-op on ARM and that `ISB` is the `pause`
equivalent. **Both halves are confirmed by measurement.** The implied action —
swap `yield` for `ISB` — is **measured harmful** when done alone.

## Measured ground truth, on this exact SoC

`ps3-thor/rpcsx-ui-android/docs/arm64/bench-results.md`, produced by
`tools/bench/thor_bench.cpp` with core pinning. **This is the AYN Thor's
Snapdragon 8 Gen 2**, not a manual.

| shape | ns per iteration |
| --- | --- |
| `yield` | **0.36** |
| `nop` | 0.36 |
| bare load | 0.36 |
| `isb` | **11.42** |
| armed `wfe` | **72,024** |

**`YIELD` is exactly a `nop`, measured rather than inferred.** This repo has
asserted it from the architecture manual. It is now on the silicon.

**`ISB` costs 32 times a `yield`.** That number is the whole problem below.

**An armed `WFE` parks for about 72 µs**, woken by the event stream. `FEAT_WFxT`
is absent on this chip, so `WFE` cannot carry a timeout.

Wake latency, writer's store to waiter observing it:

| delay | spin | futex |
| --- | --- | --- |
| 0 µs | 448 ns | 11,965 ns |
| 200 µs | 424 ns | 9,413 ns |
| 1000 µs | 474 ns | 12,521 ns |

**A futex park costs about 10 µs of extra wake latency against 0.44 µs for a
spin**, flat across delays.

## Why swapping the instruction alone is a regression

**Three independent results, all in the fleet, all pointing the same way:**

| Fork | What was tried | Result |
| --- | --- | --- |
| **rpcsx** | `ISB` replaced `YIELD` | **+23% regression** |
| **xenia** | `a64_spin_hint_isb`, the JIT-side equivalent | **`CONFOUNDED`**, no win visible |
| **Cemu** | made the Latte backoff time-based to restore x86 intent | **worse on the Thor**: `LatteCP_readU32Deprc` went 5.13% → 7.12% of total CPU |

The arithmetic is in rpcsx's own note: **spin counts were hand-tuned around an
instruction that costs nothing.** Substituting one that costs 32× more multiplies
the total backoff time by 32 and changes the shape of every tuned wait.

**Cemu's result is the one that stops a tidy conclusion.** "Budget the spin in
time rather than in iterations" is the obvious fix, and Cemu measured it and it
lost — a shorter ARM backoff that falls through to `sched_yield` sooner was the
better trade for that loop. Its comment ends: **"leave the count alone."**

> **The backoff instruction and the iteration count are one tuned pair.**
> Changing either alone is a regression risk, and the right total backoff time
> is per-loop, not a fleet constant.

## What the fleet actually does

Host-side only. Guest-side hits were excluded: azahar's `arm_dyncom_dec.cpp`
decodes the guest's own `yield` and `wfe`, and melonDS's `Arm64Emitter` `isb` is
an instruction-cache flush, not a spin.

| Tier | Mechanism | Fork | Sites |
| --- | --- | --- | --- |
| **1** | **`SEVL`/`WFE` + `LDAXR`, park on the address** | **ARMSX2** `MonitoredWait` | `ShortSpinOn`, 3 in `Semaphore.cpp` |
| **1** | same, JIT-emitted | **dynarmic** `EmitSpinLockLock` | azahar, eden, Vita3K |
| 2 | `ISB` backoff | Cemu, `_mm_pause` shim | 8 |
| 2 | `ISB` backoff | Vita3K `spin::cpu_relax` | 2 |
| 2 | `ISB` backoff | xenia `SpinLoopHint` | **1** |
| **3** | **`asm("yield")`, the no-op** | **eden** `Common::SpinLock` | **2, both hot** |
| — | no host spin loop at all | melonDS | 0 |

### Tier 1 exists twice, independently

**ARMSX2's `MonitoredWait` is fork work**, not upstream PCSX2 — commit
`2e39fcc216`, *"Optimization: wait on the address instead of spinning the
pipeline"*. It carries its own measurements:

- **3.5 wake-ups per wait as written, against 6708 with a `CLREX` added.**
  Clearing the monitor is itself a wake event, so a `CLREX` on the way out stops
  the loop parking at all. It cites Linux's arm64 `__cmpwait` doing the same.
- `SEVL` comes first because the event register is one sticky bit.
- Against the `ISB` spin, a co-resident thread keeps **~50%** of its throughput
  either way, against 100% when the waiter blocks in a futex.

**dynarmic reached the identical `SEVL`/`WFE`/`LDAXR` shape**, and it is
**0BSD** — the most permissive licence anywhere in the fleet.

**Two independent implementations converging is the same signal as three forks
choosing Oboe.** It is the strongest evidence in this audit.

### eden ships both the best and the worst, in one binary

eden vendors dynarmic, so its build contains `EmitSpinLockLock` — correct,
0BSD, `SEVL`/`WFE`. It also has `src/common/spin_lock.h` using `asm("yield")`,
**live in two hot places**:

- `k_slab_heap.h:71` — the guest kernel slab allocator
- `k_thread.h:934` — `m_context_guard`, on the context-switch path

**Neither is dead code.** eden is the only fork still on tier 3.

**Method for that negative:** every fork was searched twice — once for
`this_thread::yield|_mm_pause|spin|SpinLock|busy_wait`, then for the ARM
primitives `isb|wfe|sevl|yield` in inline assembly — and every hit was read to
separate host-side from guest-side.

**`KSpinLock` is not a third case.** Despite the name it wraps a `std::mutex`
and never spins, with a yuzu-era `TODO` saying so.

### How each fork decides how long to spin

**This axis matters more than the instruction, and the forks disagree more here.**

| Fork | Budget |
| --- | --- |
| **ARMSX2** | **calibrated at run time.** `MeasurePauseTime()` times `MultiPause` on the actual host, takes the min of five samples, then spins to a ~500 ns target. Overridable with `WAIT_SPIN_MICROSECONDS`. |
| **Vita3K** | **wall-clock from `CNTFRQ_EL0`**, 20 µs default, escalating to yield then to exponential sleep |
| Cemu | fixed 80 iterations, **deliberately kept** after the Thor A/B above |
| xenia, eden | none |

**ARMSX2's is the only design the 32× problem cannot break**, because it never
assumes what the backoff instruction costs — it measures it on the host it
booted on. That is why it can use eight `ISB`s per step without the cost
mattering.

**Vita3K's is second and cheaper**, since a wall-clock budget needs no
calibration loop, but it fixes the budget rather than the cost.

## Corrections to CLAUDE.md

1. **The `yield` lead needs its fix rewritten.** Finding a no-op `yield` is
   correct; replacing it with `ISB` without re-deriving that loop's budget is
   measured harmful in three forks.
2. **dynarmic is vendored by three forks, not two.** The list says azahar and
   Vita3K; **eden also has it**, in-tree at `src/dynarmic/` with 433 tracked
   files. Vita3K's is a submodule, which is why a file count misses it.
3. **Three forks have an rpcs3 ARM64 cross-pollination document, not two.** The
   list names xenia and Cemu. **Vita3K has one too**:
   `docs/research/20260820-rpcs3-arm64-optimizations-for-vita3k.md`.

## Two negatives, each searched twice

Recorded per the rule that a negative is only worth writing after a second
search with different words.

- **melonDS-android has no host-side spin loop.** Searched for
  `this_thread::yield`, `_mm_pause`, `spin`, `SpinLock`, then for `isb`, `wfe`,
  `sevl`, `busy_wait`. Every hit is a Qt spin box or the Dolphin `Arm64Emitter`
  cache flush.
- **azahar's own code has no host spin loop.** Its single
  `std::this_thread::yield()` is in `gcadapter/gc_adapter.cpp`, polling a
  GameCube controller adapter over USB. **That path cannot run on the Thor.**
  Its only real spin lock arrives inside dynarmic, and is tier 1.

## What this does not say

- **No claim that tier 1 is faster in a game.** The wake-latency and
  cost-per-iteration numbers are microbenchmarks. rpcsx's own attempt to park an
  SPU self-loop measured **worse on a real title** — 58.3 fps to 50.0,
  reproducibly.
- **No number is ours.** Every figure here is read from rpcsx's results file or
  from a comment in ARMSX2, Cemu or xenia.
- **eden's `yield` has not been shown to cost anything.** It is on two hot paths
  and it is provably a `nop`; how often those paths contend is unmeasured.

## Next, and none of it needs the device

- **Take dynarmic's `EmitSpinLockLock` as the reference for tier 1**, because
  0BSD carries no licence constraint anywhere. ARMSX2's is the better-documented
  twin and its measurements should travel with it.
- **Fix eden by deletion, not by substitution.** `Common::SpinLock` can call the
  spin lock already in its own binary. That is a DELETE, not a rewrite.
- **Do not touch Cemu's count.** Its comment records a Thor A/B and says so.
