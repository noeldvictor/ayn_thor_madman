# Fast-forward is a time scale reaching every clock, and it fails silently in exactly the backends this project keeps

**Goal: examine a required universal hotkey that this repo has never analysed.**

`CLAUDE.md` mandates that **one hotkey set works on every system** and names fast
forward and rewind among them. **It says nothing about what fast forward means to
a backend**, and the answer is not "run the loop faster".

## The evidence: a fork shipped the toggle twice and it did nothing both times

Vita3K's own reports, 2026-05-10, in order:

1. **`fast-forward-timing-fix`** — the command reached the emulator, but pacing
   still depended on real-time kernel waits. Scaled `sceKernelDelayThread` and
   its variants, kernel wait timeouts, and kernel timer event scheduling.
2. **`fast-forward-guest-clock-follow-up`** — *"User reported fast-forward still
   did not appear to affect gameplay."* The log said `Fast forward 200%`. **The
   toggle was firing and the guest was still living in real time.**

The second pass had to add an **anchored speeded process clock** and route
through it: `sceKernelGetProcessTime*`, `sceKernelGetSystemTimeWide`, timer
elapsed reads, `sceKernelLibcClock`, `sceKernelLibcTime`,
`sceKernelLibcGettimeofday`, RTC tick and clock APIs, thread start ticks, **and
NetCtl adhoc peer timing.**

> **The switch moved and nothing happened — the settings symptom again, with a
> fifth cause: the feature is not a value, it is a cross-cutting property of
> every clock in the backend.**

**"Anchored" is the design word.** A speeded clock must be continuous across a
speed change, or time jumps at the toggle. A naive multiply is a discontinuity.

## Why it is hard in some backends and free in others

**A low-level emulator derives guest time from emulated cycles, so running the
loop faster speeds up the guest automatically. A high-level emulator that
implements the guest's `gettimeofday` by asking the HOST what time it is does
not** — the guest gets real time no matter how fast the loop runs.

**Read from the four forks:**

| Fork | Guest time comes from | Fast-forward mechanism | Size |
| --- | --- | --- | --- |
| **melonDS** | emulated DS cycles (LLE) | **a bool the frame limiter reads** | **3 references** |
| **eden** | HLE kernel | **`GetClockTicks()` divides by the speed limit** — behind a `sync_core_speed` setting | one function |
| **Vita3K** | HLE kernel | **an anchored speeded clock across ~12 time APIs**, plus audio and vblank | two attempts |
| ARMSX2 | emulated EE cycles (LLE) | limiter, **plus a `FastForwardVolume` setting** | — |

> **This is the HLE-against-LLE axis again**, the same one that decides whether
> API translation is available. **It predicts which backends are expensive
> before anybody opens them**: the HLE backends — Switch, Vita, Wii U, 3DS,
> PC — are the hard ones, and they are most of the fleet.

**eden also has a second, independent guest-clock scalar in the same function:**
`fast_cpu_time` multiplies the tick rate by 1.7 to 2.6 as a guest **overclock**.
**Two scalars, one function, composing** — and an overclock is a different
feature from a fast-forward that a user would not expect to interact.

## Audio is a third consumer, and three forks relate it to speed three ways

**Searched all eight forks** for a speed-linked audio mechanism —
`speed_percent|speed_multiplier|emulation_speed|speed_limit|playback_rate|
resample_ratio|time_stretch|timestretch` over audio, SPU and sound paths, with
Oboe and cubeb excluded — then read every hit. **Three forks have one, and they
are three different ideas.**

| Fork | Mechanism | Direction it serves |
| --- | --- | --- |
| **Vita3K** | `audio::state::speed_percent`, *"the audio backends retime against this"* | fast-forward |
| **ARMSX2** | a **nominal rate** driving both resampling and time-stretching — *"input samples are assumed to be this amount faster"*, plus `SPU2/Output/FastForwardVolume` | **both**, plus a volume policy |
| **azahar** | **time-stretch when `emulation_speed <= 95`** | **RUNNING SLOW** |

**azahar's is the one nobody would predict, and it may be the most valuable
here.** It engages **below** 100%, to keep audio intact when the emulator cannot
keep up. **On a thermally-limited handheld that is the common case, not the rare
one** — this project's Foundation says speed is the product, and a product that
sometimes misses its target still has to sound acceptable while it does.

**Vita3K's scale reaches three subsystems, each with its own copy of the value:**
the kernel clock, display and vblank pacing, and audio. **ARMSX2 answers the
question the others do not ask — what should it SOUND like** — with a separate
fast-forward volume defaulting to 100.

**Not found in xenia, Cemu, melonDS, eden or rpcsx** by that search. rpcsx's
hits were read and are `sys_rsxaudio` sample-rate handling, which is guest audio
hardware rather than a speed link.

## What this means for the contract

**A `fastForward: Boolean` in the backend contract would ship the Vita3K bug.**
What the contract needs instead:

- **A time scale, not a toggle.** One value covers pause (0), slow motion, normal
  and fast-forward, and **pause is already load-bearing here**: the paused agent
  loop freezes the guest so model latency costs nothing. **That is the same
  mechanism at scale 0.**
- **A backend declares which clock domains it scales**, and a backend that HLEs a
  host-derived time API must declare that domain **or fast-forward silently does
  nothing for it**. Declaring it is how the failure becomes visible at
  integration rather than in a user report.
- **Audio has a policy of its own.** Take ARMSX2's separate fast-forward volume.
- **A measurement taken at scale ≠ 100 is void**, which `MEASUREMENT.md` does not
  say.
- **Guest activity interacts.** A fixed-rate video decode under a time scale is
  precisely where frame extrapolation invents motion, and `GuestActivity`
  already refuses to generate frames there.

## A method note that keeps proving itself

**A single-vocabulary search would have reported two forks as lacking the
feature.** Counting files across four vocabularies:

| | `fast forward` | `speed percent` | `turbo` | `time scale` |
| --- | --- | --- | --- | --- |
| **azahar** | **0** | 8 | 27 | 9 |
| **eden** | **0** | 19 | 29 | 4 |
| melonDS | 39 | 0 | 2 | 0 |
| ARMSX2 | 25 | 17 | 30 | 0 |

**Both forks with zero "fast forward" hits have the feature.**

## Limits

- **Four forks read, four counted only.** azahar, Cemu, xenia and rpcsx were not
  opened; their column counts are a hint, not a mechanism.
- **Vita3K's reports end with the fix unverified in a game** — *"Needs an
  in-game on-device confirmation with a title that has obvious clock
  movement."* **Nothing here says the fix works**, only what it had to touch.
- **Nothing measured.** No frame rate, no audio quality, no device.
- **Rewind is not covered.** It is named in the same hotkey list and is a
  different mechanism — state snapshots, not a clock.

## Sources

- Vita3K `reports/20260510_165501_fast-forward-timing-fix.md` and
  `reports/20260510_172815_fast-forward-guest-clock-follow-up.md`
- Vita3K `vita3k/audio/include/audio/state.h:88`
- eden `src/core/core_timing.cpp:200-212`
- melonDS `app/src/main/cpp/MelonDS.cpp:1396`
- ARMSX2 `pcsx2-qt/Settings/AudioSettingsWidget.cpp:77`
