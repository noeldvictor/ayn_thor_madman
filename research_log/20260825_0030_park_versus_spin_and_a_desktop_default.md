# Park-versus-spin has a break-even, the WFE wake floor is 95 µs, and 93% of one fork's spin is a desktop default

**Goal: read rpcsx's `docs/arm64/spin.md`, the last large topic document, at
1,480 lines.**

**Three findings, and the third is a sixth form of the x86 detour.**

## 1. The number this repo did not have: the WFE wake floor

    one GETLLAR backoff    15.62 us   (300 ticks at 19.2 MHz)
    WFE park wake floor    95.06 us   (measured; FEAT_WFxT absent, so no timeout)

> **Parking is worth it only when the expected REMAINING spin exceeds the wake
> floor.** Break-even here is **depth 6.1**; the implemented threshold is **8**.

| depth | keep spinning | park | parking is |
| --- | --- | --- | --- |
| 1 | 15.6 µs | 95.1 µs | **worse** |
| 4 | 62.5 µs | 95.1 µs | **worse** |
| 8 | 125.0 µs | 95.1 µs | better |

**`CLAUDE.md` records the spin-wait tiers and that ARMSX2 and dynarmic both reach
`SEVL`/`WFE` independently. It has no wake-floor number**, so it cannot say when
parking pays. **95 µs is that number on this SoC**, and it follows from a device
fact already recorded here: **`FEAT_WFxT` is absent, so `WFE` cannot carry a
timeout.**

**And the threshold was reasoned, not fitted.** It was set from the 95 µs
event-stream period when the WFE path was written; **the histogram confirmed it
independently.** Given this fleet's 0-for-13 record on manual-derived
predictions, a manual-derived value that measurement *validates* is worth
recording as such.

## 2. The mean was useless and a histogram located the answer

**Mean GETLLAR spin depth: 135.2. Median: below 8.**

| depth | spins | share |
| --- | --- | --- |
| **`< 8`** | 7,083 | **95.5%** |
| 8-31 | 277 | 3.7% |
| 32-127 | 57 | 0.8% |
| ≥ 128 | 0 | 0.0% |

> **The mean of 135 was a small number of very deep waits dragging the average.**
> **95.5% of spins are ones the park can never catch** — and that is an argument
> *for* the threshold, not against it, because the shallow ones are exactly the
> ones parking would make worse.

**`MEASUREMENT.md` already says report `[min..max]`, never a mean.** This is the
same rule reaching a distribution rather than a set of runs: **bucket it, and put
the first boundary at the decision threshold.**

## 3. A sixth form of the detour: a desktop-era CONFIG DEFAULT

**Two knobs, and the asymmetry is the finding:**

| tunable | upstream default | Thor profile |
| --- | --- | --- |
| SPU **Reservation** Busy Waiting % | 0 | **explicitly 0**, and `Enabled: false` |
| SPU **GETLLAR** Busy Waiting % | **100** | **not overridden** |

**Somebody deliberately turned reservation busy-waiting off for this device,
writing it into two places. The GETLLAR knob sits at its upstream default — and
GETLLAR is 93% of all emulator spin.**

> *"The trade was considered once, applied to the smaller of the two sites, and
> never revisited for the larger."*

**The fork's own framing is the transferable part:** *"it is an **x86-era default
that nobody re-derived for a passively cooled handheld**."*

**The five forms this repo records are instruction selection, the register model,
timing constants, build flags, and a correction that becomes a corruption.
Today's Cemu reading added a sixth — a synchronisation structure papering over a
missing guarantee. This is a seventh, and it is not code at all.**

**And the reason the trade inverts here is this project's Foundation, stated by
another fork:** spinning buys wake latency at the cost of burning a core;
sleeping buys power at the cost of wake latency. **The title already holds its
30 fps cap at twenty-to-thirty percent CPU, so latency headroom exists, and the
machine is passively cooled.** *"That is the exact case where trading some
latency for power is favourable, and it is a config change rather than a
redesign of the hottest lock in the memory subsystem."*

## The fleet sweep: does anyone else carry a desktop spin default?

**Searched all seven packed-binary forks** for `busy.?wait|spin.?wait|spin.?loop|
spin.?count|max.?spin` over `*.h *.cpp *.kt`, vendored trees removed, then read
the configurable ones.

| Fork | Finding |
| --- | --- |
| **ARMSX2** | **`HWSpinCPUForReadbacks` and `HWSpinGPUForReadbacks` both default `false`** — the power-friendly choice, already made |
| **xenia** | **found and fixed one**, see below |
| Vita3K | `spin_count` and `spin_wait_*` are its own primitives, and it derives its budget from `CNTFRQ_EL0` — already recorded here |
| Cemu, azahar, melonDS, eden | no configurable spin-versus-sleep policy found |

**xenia's is the instructive one, and it is the same shape as rpcsx's:**

> `timer_queue_sleep_idle` — *"The disruptor **`spin_wait_strategy` polls the
> clock continuously between timer events** (device-profiled as the **top
> `__kernel_clock_gettime` cost** on Blue Dragon — a core burned for nothing,
> the same spinning-worker pathology as the XMA decoder)."*
>
> Device-validated: **`TimerThreadMain` 2.63% → 1.79%, −0.84pp of the frame.**
> **And it defaults to `false`.**

**A spinning wait strategy inherited from a third-party concurrency library**,
burning a core polling the clock. **Same class as rpcsx's default, in a different
place, found by a different fork, and neither cites the other.**

## A hypothesis this hands to Cemu's open question

**Cemu has 23% of all emulator CPU in `__kernel_clock_gettime` with the caller
unidentified**, because the vdso has no frame pointers and DWARF cannot unwind
it. Its disassembly found the only call sites to be the overlay, the performance
monitor, a file-cache test, `LatteCP_*`, `select`, the mic path, curl and libusb
— *"none of which should be hot on a guest scheduler thread."*

> **xenia's top `clock_gettime` cost turned out to be exactly this: a library's
> spin strategy polling the clock. That hypothesis class is not named anywhere in
> Cemu's investigation.**

**Two honest caveats.** Cemu's dismissal rests on **thread attribution** — the
samples are on `OSSched[core=1]` and `LatteCP_*` is the command-processor thread
— **so the reasoning is sound if the attribution is right.** And **searching
Cemu for a header-only spinning wait strategy found no candidate**:
`wait_strategy|SpinWait|busy_poll` over its own source returns only a BOSS
service and a DSU controller provider.

**But its own next step already covers this**: *"interpose or count rather than
sample."* **An interposed counter settles the caller and the thread attribution
at once**, which is the one action that answers both.

## Limits

- **All numbers are rpcsx's and xenia's**, on their own titles. Nothing
  reproduced, no device used.
- **The GETLLAR percentage sweep is UNRUN.** rpcsx names it *"the first candidate
  in this document that is simultaneously large, cheap, and untested"* and lists
  the instruments; it has not been swept.
- **The 95 µs wake floor is one measurement on this SoC**, quoted from that
  document.
- **Roughly 1,300 lines of `spin.md` remain unread**, including a present-path
  busy-spin on a 2017 desktop-driver workaround and a section that **refutes an
  earlier section of itself** about why a title screen draws ~4 W.

## Sources

- rpcsx `docs/arm64/spin.md:621-734`
- xenia `src/xenia/base/threading_timer_queue.cc:24-40`
- ARMSX2 `pcsx2-qt/Settings/GraphicsSettingsWidget.cpp:250-251`
- `research_log/20260824_2020_the_guest_clock_is_the_x86_detour_measured.md`
