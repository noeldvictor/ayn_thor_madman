# The first measured audio latency in this project, and a bigger buffer that underran more

**Goal: this repo standardised on Oboe from a convergence argument and recorded
"latency itself is still unmeasured". azahar has measured audio on the physical
Thor. Read it.**

**No device used here.**

## The measurement

**azahar's accepted Cubeb path on the physical Thor:**

> a **32,728 Hz, 1,962-frame** AudioTrack at roughly **118-131 ms reported
> latency** with **zero** current underruns.

**That is the first audio latency figure this project has.** For scale, **118 ms
is about seven frames at 60 Hz.**

## The rejection, which runs the wrong way

> A **4,096-frame** candidate raised reported latency to **271.84 ms** and
> accumulated **989 underruns within roughly one minute.** Fully reverted.

> **A bigger buffer produced MORE underruns, not fewer.** That is the opposite of
> what buffering is for.

**The cause is a platform threshold azahar names: Android Cubeb's
4,000-frame power-saving boundary.** Crossing it moves the track off the fast
path and onto a power-saving path that behaves worse in both directions at once.

**A device fact worth recording beside `CNTFRQ_EL0` and `CTR_EL0`: there is a
4,000-frame cliff in the Android audio path on this device, and the accepted
configuration sits at 1,962 frames — deliberately under half of it.**

## The hypothesis the number suggests, and it is NOT verified

**32,728 Hz is the 3DS's own sample rate.** It is not the device's native output
rate, which on Android hardware of this class is 48,000 Hz.

**Opening an output stream at a non-native rate generally forces resampling
inside the platform mixer, and that commonly costs the fast low-latency path.**
If that is what happened, **the 118-131 ms is partly a consequence of the rate
choice rather than a floor for this device.**

> **Stated as a hypothesis. Nothing here tests it, and azahar does not claim
> it.** What makes it worth writing down is that **every Oboe fork in the fleet
> does the opposite.**

## The three Oboe forks all set an explicit output rate

**Method: `git grep` for `setSampleRate|PerformanceMode|setSharingMode|setFramesPerCallback`
in each Oboe fork's own audio source, then the matched lines read.** The three
Oboe forks are the ones this repo already identified — ARMSX2, eden, melonDS.
**This is a read of three files, not a fleet census**, so it says what these three
do and nothing about the four forks that do not use Oboe.

**Read from their own source:**

| Fork | Output rate | Performance mode |
| --- | --- | --- |
| **melonDS** | **`setSampleRate(48000)`** | `LowLatency`, with `None` and `PowerSaving` selectable |
| **eden** | **`setSampleRate(TargetSampleRate)`**, and `TargetSampleRate = 48'000` | `LowLatency`, plus **`setSampleRateConversionQuality(High)`** |
| ARMSX2 | `setSampleRate(m_sample_rate)` — a variable | **a fallback ladder**, `LowLatency` then `None`, with retries |

**melonDS's guest runs at 32,823 Hz and it opens at 48,000 anyway.** eden goes
further and tells Oboe to convert at high quality rather than letting the
platform do it.

> **The design rule: open the host stream at the DEVICE's native rate and
> resample the guest into it. Do not open a stream at the guest's rate.**
> **Three forks do this. The fork with the measured 118-131 ms does not.**

## And ARMSX2 has the robustness pattern the shared layer needs

It does not assume `LowLatency` is available. It walks
`{LowLatency, None}` with retry attempts, and its comment names the fallback as
*"PerformanceMode::None (shared slow-path) stream before giving up"*.

**A shared audio layer must not fail to produce sound because the fast path was
refused.** melonDS exposes the mode as a user setting including `PowerSaving`;
**ARMSX2 degrades automatically.** The shared layer wants both — automatic
fallback, with the mode visible.

## What this changes for the audio decision

**The Oboe standardisation was argued from convergence alone** — three forks
chose it independently — and `CLAUDE.md` says plainly *"Nothing here is
measured."*

**It now has a baseline to beat: 118-131 ms on Cubeb, on this device, at the
guest's sample rate.** That is a number a matched Oboe measurement can be
compared against, and it is the kind of comparison this project has been unable
to make.

**And azahar's acceptance bar for any future audio-buffer change is the right one
to adopt wholesale:** opt-in, **prove clean interactive audio**, and **beat the
existing path in matched whole-device battery measurements** — not latency alone.

## Limits

- **Every figure is azahar's, on its Thor, with Cubeb.** Nothing reproduced here.
- **"Reported latency" is the audio stack's own report, not a measured
  round-trip.** A loopback measurement would give a different and larger number.
- **The sample-rate causation is UNVERIFIED.** It is a hypothesis from one
  correlation plus three forks doing the opposite. **It is not azahar's claim.**
- **ARMSX2's `m_sample_rate` is a variable and was not traced** to a value; the
  PS2's SPU2 output is 48 kHz, so it probably lands on the native rate, but that
  was not confirmed.
- **No Oboe latency figure was found, and the negative is WEAK — say so.**
  Searched `latency|underrun` in the three Oboe forks' `AGENTS.md`. **Zero in all
  three, and a positive control on `audio` also returned zero in ARMSX2's**,
  because that file is **92 lines about project shape** and never mentions audio.
  **A zero from a 92-line file is not evidence of absence.** Neither the forks'
  `docs/` trees nor their commit history was searched.

## Sources

- azahar `AGENTS.md:49-54`
- ARMSX2 `pcsx2/Host/OboeAudioStream.cpp:66,173-227`
- melonDS `app/src/main/cpp/MelonDSAudio.cpp:47-69`
- eden `src/audio_core/sink/oboe_sink.cpp:114-118`, `src/audio_core/common/common.h:70`
- `research_log/20260823_0005_audio_backends.md`
