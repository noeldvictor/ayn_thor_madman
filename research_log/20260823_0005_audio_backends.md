# Audio backends: the fleet already converged, and nobody noticed

**Goal: close one of the "not surveyed in any fork" items in `CLAUDE.md`.**

Session 2026-08-23 00:05. Audio latency was listed as unsurveyed, and it decides
playability on a handheld.

**Result: three forks independently chose Oboe. That is convergence, and it
settles the vendored-audio problem that blocks the packed binary.**

---

## What each fork actually uses on Android

Searched for the mechanism — `AAudioStreamBuilder`, `SLObjectItf`, `oboe::` —
then excluded hits inside vendored `cubeb/` and `oboe/`, because cubeb carries
its own AAudio and OpenSL backends and would otherwise credit every fork with
both.

| Fork | Android audio path | Library |
| --- | --- | --- |
| **ARMSX2** | `pcsx2/Host/OboeAudioStream.cpp` | **Oboe** |
| **eden** | `src/audio_core/sink/oboe_sink.cpp` | **Oboe** |
| **melonDS** | `MelonDSAudio.cpp`, `MicInputOboeCallback.cpp` | **Oboe** |
| xenia | `src/xenia/apu/android/android_audio_driver.cc` | its own driver |
| Vita3K | SDL's AAudio backend | SDL |
| rpcsx | `Emu/Audio/Cubeb/CubebBackend.cpp` | cubeb |
| azahar, Cemu | no direct hits; vendored cubeb | cubeb |

**melonDS also does microphone input through Oboe**, in
`MicInputOboeCallback.cpp`. The DS has a microphone, so that is guest hardware
nobody else in the fleet needs.

---

## Why this is convergence rather than duplication

The rest of this repo's surveys found forks solving one problem several
different ways. **This is the opposite: three forks reached the same answer
independently, and the answer is the one Google recommends.**

Oboe selects AAudio on Android 8.1 and later, falls back to OpenSL ES below
that, and handles the device-specific latency tuning that a hand-written AAudio
driver has to redo. cubeb is cross-platform and reaches Android through its own
backends, which adds a layer that exists to serve desktop portability — **the
exact kind of portability cost [Foundation](../CLAUDE.md) point 1 refuses.**

**Three independent choices agreeing is stronger evidence than any one of them
alone**, which is the same reasoning already applied to the touch overlay API
surviving two divergences.

---

## It settles a blocker

`CLAUDE.md` records that **cubeb is vendored five times** and that a packed
binary cannot link five copies of one library, with four different versions
being worse than four copies. That was recorded as an open problem with no
chosen answer.

**The answer is Oboe**, and three forks have already made the change, so the
work is smaller than it looked:

| Fork | Move required |
| --- | --- |
| ARMSX2, eden, melonDS | **none** |
| xenia | replace a hand-written Android driver |
| Vita3K | move off SDL audio |
| azahar, Cemu | move off cubeb |
| rpcsx | out of the binary anyway, GPL-2.0-only |

**So the vendored-cubeb problem is four conversions, not a design decision.**

Add Oboe to the standard row in `CLAUDE.md`, at a pinned version.

---

## What this does not claim

**No latency was measured.** Not one number in this log came from the device.

The claim is that three forks chose the library Google recommends for low
latency on Android, and that this resolves which one the shared layer should
use. **Whether it is audibly better than cubeb-over-AAudio on this device is
unmeasured**, and it belongs in the experiment ledger, not here.

Two things worth measuring when the device is free:

- Round-trip latency per backend, on the Thor, at the same buffer size.
- Whether Oboe's automatic latency tuning picks a different burst size on this
  panel-locked 60 Hz configuration than it does at 120 Hz. **The device is
  currently capped to 60 Hz by a user setting**, and audio buffer sizing that
  tracks frame pacing would be affected by it.
