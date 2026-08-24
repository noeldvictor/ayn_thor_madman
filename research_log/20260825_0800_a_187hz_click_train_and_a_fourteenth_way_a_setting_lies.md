# A ~187 Hz click train, realtime-safe instrumentation, and a fourteenth way a setting lies

**Goal: the capability-inventory update showed XenDroid holding three
capabilities while this repo lists it only as a reference clone. Survey it.**

**Device-free: reading a checked-out tree at `xenia-thor-workspace/reference/`.
No device used.**

## XenDroid is a product being polished, not a research fork

**Its last 25 commits, to 2026-07-30**, read as handheld product work, and
several land on features this project has SPECIFIED and not built:

| XenDroid commit | This project calls it |
| --- | --- |
| *"Ask which disc to insert when a multi-disc game requests one"* + *"Offer every disc of the title"* | **nothing — `app/SCREENS.md` has no disc-swap screen** |
| *"Answer guest text prompts with the Android keyboard"* + *"Keep the guest text field visible when the keyboard is up"* | **guest system applets**, section 7 |
| *"Let users trade audio delay against audio breakup"* | the audio decision, which had no user-facing control in any design here |
| *"Distinguish the resolution scale from the reported mode"* | a **setting that did not apply**, by the sound of it |
| *"Fix the profile screens crashing on a moved settings key"* | **settings migration**, which this repo took from melonDS |
| *"Keep controller input off the global critical region"* | input threading |
| an updater with **GitHub rate limiting** | nothing |

**Multi-disc handling is the one to note.** It is an ordinary requirement for
several systems in this fleet and **no screen in `app/SCREENS.md` covers it.**

## The audio concealment, and a number worth keeping

`xe_aaudio_audio_driver` gained **196 lines** to *"conceal audio gaps instead of
gating to silence"*. Its header comment states the defect precisely:

> **"silence would put a step at both edges of every gap, a ~187 Hz CLICK TRAIN
> at a 5.3 ms block."**

**An underrun filled with silence is not a dropout — it is a periodic
discontinuity at one over the block time**, and at a 5.3 ms block that is an
audible tone. **The concealment keeps `last_block_`, repeats it across the gap,
counts `gap_blocks_`, and applies a fade-in on recovery** through
`fade_in_pending_` and `ApplyFadeIn()`.

**This matters here because this project has just recorded an audio buffer
finding and had no vocabulary for what an underrun SOUNDS like.** azahar's
rejected 4,096-frame candidate accumulated **989 underruns in about a minute** —
**at a comparable block time that is a click train, not 989 silent moments.**

## Realtime-safe instrumentation, done correctly

> **"Written by the realtime callback, drained by `recovery_thread_`: relaxed
> atomics only, nothing that could block the callback."**

**Four counters** — `stat_callbacks_`, `stat_gaps_`, `stat_queue_depth_sum_`,
`stat_queue_depth_max_` — **published as relaxed atomics and drained by another
thread.**

**This repo's measurement rules say to instrument the thing you changed.** They
do not say how to instrument a context where you cannot allocate, lock or log.
**This is the pattern: relaxed atomic counters, drained elsewhere, summed and
logged off the realtime path.** It applies to any audio callback and to a render
submission thread.

**And `queue_depth_sum_` beside `queue_depth_max_` is the right pair** — a mean
tells you the buffer is adequate, **a maximum tells you why it broke.** This
project's own rule says to report `[min..max]` rather than a mean; here it is,
built into the instrument.

## The fourteenth way a setting lies: the request was honoured with a SUBSTITUTED value

**One counter carries a comment that names a mechanism
[`DID_IT_APPLY.md`](../shared_layer/DID_IT_APPLY.md) does not have:**

```cpp
// A block size we did not ask for silently changes the drain rate.
std::atomic<int32_t> stat_unexpected_frames_{0};
```

**The twelve original mechanisms are all forms of a setting NOT taking effect.
Number 13 is a COUPLED setting — it applied, and so did another. This is a
third shape:**

> **The setting was accepted, and the system granted a DIFFERENT value. Nothing
> failed, nothing warned, and every calculation downstream is now wrong.**

**It is endemic to graphics and audio APIs**, where you request a format, a
buffer size or a present mode and receive a supported one. **The failure is
silent by design** — the API is behaving correctly.

**And this project probably already has an instance and did not see it.** azahar's
accepted Cubeb path is described as *"a 32,728-Hz, **1,962-frame** AudioTrack"*.
**1,962 is not a number anybody asks for.** It is very likely what the system
granted. **Recorded as a suspicion, not a finding** — azahar does not say what it
requested, and this was not checked.

> **The detector is XenDroid's: compare what you got against what you asked for,
> every time, and count the difference.** Not an assert — a counter, because the
> substitution is legal.

## Limits

- **Nothing here is measured.** Source reading of one fork's commits and one
  header. **No device, no build, no audio captured.**
- **The ~187 Hz figure is XenDroid's arithmetic in a comment**, not a
  measurement, and it depends on their block size.
- **XenDroid's HEAD here is 2026-07-30.** The checkout may be behind; xenia's own
  sweep on 2026-08-06 found itself 27 commits behind this fork, so this tree is
  a snapshot rather than the fork's current state.
- **The commit list was read as SUBJECTS.** Only the two audio commits were
  opened, and only one of those beyond the diffstat.
- **The azahar 1,962-frame suspicion is unverified** and is written as a
  suspicion.
- **No claim that XenDroid's concealment is correct or good** — it was read, not
  run or listened to.

## Sources

- `xenia-thor-workspace/reference/XenDroid`, `xe_aaudio_audio_driver.h`
- `research_log/20260824_2340_the_first_measured_audio_latency_and_a_bigger_buffer_that_underran.md`
- `shared_layer/DID_IT_APPLY.md`
