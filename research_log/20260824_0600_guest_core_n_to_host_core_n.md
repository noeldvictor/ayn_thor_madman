# Two forks pin guest core N to host core N, and on this SoC that is the little cluster

**Goal: check whether xenia's guest-affinity bug is fleet-wide.**

**It is. eden has the same bug from a different direction, and eden's version is
enabled unconditionally on Android with a comment explaining that it was tuned
for a different SoC.**

**And a second result from the same read: a hardware claim in `CLAUDE.md` was
measured on this device and did not reproduce.**

## The Thor's core indices, which this repo has never written down

From xenia's `thor_topology.h`:

| Host CPU | Core | Clock |
| --- | --- | --- |
| **0, 1, 2** | **Cortex-A510** | **2.0 GHz** |
| 3, 4, 5, 6 | Cortex-A715 / A710 | 2.8 GHz |
| **7** | **Cortex-X3** | **3.19 GHz** |

**`CLAUDE.md` records the 1+4+3 layout and never records which index is which.**
Every affinity decision needs that mapping, and both bugs below are what happens
without it.

## xenia: guest CPU N pinned to host CPU N

From its ledger, verdict `OPEN`, and its own words:

> **FOUND A MAJOR x86-SHAPED STRUCTURAL BUG.** `XThread::SetActiveCpu` default
> path does `thread_->set_affinity_mask(1 << cpu_index)` where `cpu_index` is the
> **GUEST** cpu (0-5). **On homogeneous x86 that is fine.**

**The consequence, stated there:** the 1:1 map pinned guest CPUs 0, 1 and 2 onto
**the three little cores**, and **never gave the X3 any guest work at all**. On
the Xbox 360, **guest hardware thread 0 is conventionally the main game thread**,
so the hottest guest thread ran at 2.0 GHz while the 3.19 GHz core idled.

> **This is a power bug as much as a speed one**: a little core at max voltage
> doing the hot work, big cores idle, everything taking longer, so more total
> energy.

**And the workaround does not ship.** `thor_guest_thread_affinity_mask` exists
and **defaults to 0**, so the 1:1 map was the shipping behaviour.

## eden: the same bug, and the function name asserts the opposite

`src/core/cpu_manager.cpp:196`, in `CpuManager::RunThread(token, core)` where
`core` is the **guest** core index:

```cpp
#ifdef __ANDROID__
    // Aimed specifically for Snapdragon 8 Elite devices
    // This kills performance on desktop, but boosts perf for UMA devices
    // like the S8E. Mediatek and Mali likely won't suffer.
    Common::PinCurrentThreadToPerformanceCore(core);
#endif
```

And `src/common/thread.cpp:122`:

```cpp
void PinCurrentThreadToPerformanceCore(size_t core_id) {
    ASSERT(core_id < 4);
    ...
    CPU_SET(core_id, &set);
    sched_setaffinity(pthread_self(), sizeof(set), &set);
```

**The function pins to host CPU indices 0 to 3 and is named
`PinCurrentThreadToPerformanceCore`.**

**The comment is the key to it.** It names the **Snapdragon 8 Elite**, which has
**no efficiency cores at all** — so on that part, host CPUs 0 to 3 are all
performance cores and **the function's name is accurate there.**

**On the Snapdragon 8 Gen 2 it is not.** Host CPUs 0, 1 and 2 are A510 cores at
2.0 GHz, so **three of the Switch's four guest cores get pinned to the little
cluster**, by a function whose name says the opposite.

**It is inside `#ifdef __ANDROID__` with no device check**, so every Android
device that is not an 8 Elite gets whatever its own index 0 to 3 happens to be.

## The shared root cause, and why it belongs in this project's rules

**Both bugs are one mistake: using a guest core index as a host core index.**

- **It is correct on homogeneous hardware**, which is what both emulators were
  designed for.
- **It is silently wrong on big.LITTLE**, and it is wrong in the worst direction,
  because guest core 0 is conventionally the main thread on both the Xbox 360 and
  the Switch.
- **It cannot be caught by a build or a test.** It produces a working emulator
  that is slower and hotter.

> **Add to the x86-detour rule: the detour is not only in the instruction stream.
> A scheduling decision that was correct for a homogeneous machine is the same
> class of mistake, and it costs more than any peephole on this list.**

**Two forks confirmed. ARMSX2, Cemu, Vita3K and melonDS were searched for the
same shape and produced no hits** — but `CLAUDE.md` already records that melonDS
and Vita3K set **no host affinity at all**, which is a different problem with the
same outcome: the kernel decides, and the kernel does not know which guest thread
is hot.

## Second result: the A510 shared vector unit did not reproduce

**`CLAUDE.md` states as fact that the Cortex-A510 shares a vector unit between
cores in a complex, and warns that two vector-heavy threads on a paired A510
contend.** It is quoted from ARM's optimization guide.

**xenia built a probe and measured it on this device. It did not reproduce.**

`thor_probe_a510_vector_units` pins an independent-chain NEON loop per little
core, then runs pairs concurrently:

| Run | Miter/s |
| --- | --- |
| solo cpu0 / cpu1 / cpu2 | 390.3 / 407.1 / 462.6 |
| pair 0+1 | 785.7 — **98.5% of the solo sum** |
| pair 0+2 | 817.2 — 95.8% |
| pair 1+2 | 859.6 — **98.8%** |

**All pairs scale near-linearly. No pair is halved.**

**Its own stated limitation, which keeps this open rather than closed:** the
probe uses **integer** NEON (`vmlaq_u32`). **If the shared resource is
specifically the floating-point SIMD pipe, an integer test would not expose it**
— rerun with `vmlaq_f32` before calling it closed. And **Qualcomm can configure
A510 complexes with per-core VPUs**, so this part may genuinely differ from the
Odin 2 unit measured in the talk that prompted it.

> **Fourteenth manual-derived prediction measured here, and it did not survive.**
> `CLAUDE.md` already says to check `/proc/cpuinfo` before trusting an ARM
> manual. **This says more: a vendor can configure the part, so the manual
> describes what the core may implement, not what this device does.**

## Limits

- **Neither affinity bug was measured by me.** xenia's is quoted from its own
  ledger; eden's is read from source and its consequence is inferred from the
  documented core indices.
- **eden's pin is guarded by `core_id < total_cores` and by an assert**, so it is
  a performance bug rather than a correctness one.
- **The A510 probe is xenia's**, run once, with the integer-versus-float
  limitation stated by its author.
- **No fork was modified**, and none may be without being asked for by name.

## Sources

- xenia `tools/exp_ledger.py` entries `Guest CPU N was pinned to host cpu N` and
  `thor_a510_shared_vector_unit`; `src/xenia/base/thor_topology.h`
- eden `src/core/cpu_manager.cpp`, `src/common/thread.cpp`
- `hardware_ref/thor/cpu/CORE_COMPARISON.md` for the manual claim being tested
