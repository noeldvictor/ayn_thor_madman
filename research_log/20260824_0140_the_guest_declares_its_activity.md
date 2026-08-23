# The guest already declares when it is in a movie, and every fork throws it away

**Goal: find the signal for the hardest part of the paused agent loop — knowing
when NOT to look at the frame.**

**`shared_layer/AGENT_LOOP.md` says to infer it from the video decode path.
There is a much better signal, it needs no inference, and it is already arriving
at every backend.**

> **The guest operating system has an API for "the user is watching something
> they did not trigger". Games call it. Every fork receives the call and
> discards the argument.**

## The Switch case, which is the richest

eden `src/core/hle/service/am/service/self_controller.cpp` implements
`ISelfController`, and the guest volunteers a whole activity vocabulary:

| Call | What the guest is saying |
| --- | --- |
| `{61} SetMediaPlaybackState(bool)` | **"I am playing media"** |
| `{68} SetAutoSleepDisabled(bool)` | "do not sleep" |
| `{65} ReportUserIsActive()` | **"the user just interacted"** |
| `{62} SetIdleTimeDetectionExtension(...)` | how to treat idle time |
| `{72} SetInputDetectionPolicy(...)` | how the game wants input detected |
| `{60} OverrideAutoSleepTimeAndDimmingTime(...)` | adjust the sleep and dim timers |

**A game playing a cutscene typically does three of these at once**: sets media
playback state, disables auto sleep, and stops reporting user activity. **That is
a three-signal confirmation, volunteered, with no frame analysis.**

**`ReportUserIsActive` is the inverse and is worth as much.** For an agent loop
it is the guest's own statement that an input registered.

**All of them are stubbed.** Checked each handler body:

```cpp
Result ISelfController::SetMediaPlaybackState(bool state) {
    LOG_WARNING(Service_AM, "(STUBBED) called, state={}", state);
    R_SUCCEED();
}
```

| Handler | State kept |
| --- | --- |
| `SetMediaPlaybackState` | **no** |
| `SetIdleTimeDetectionExtension` | **no** |
| `ReportUserIsActive` | **no** |
| `SetInputDetectionPolicy` | **no** |
| `OverrideAutoSleepTimeAndDimmingTime` | **no** |
| `SetAutoSleepDisabled` | yes, into `m_applet` |

**Five of six discarded.** Keeping them is a member variable and an assignment
each.

## It generalises: four consoles, four APIs, one idea

**Every console has an API for "do not sleep, the user is watching".** That is
the signal, because a game only needs it when the user is watching without
touching the controls — which is what a cutscene is.

| Console | Fork | The call | State kept |
| --- | --- | --- | --- |
| Switch | eden | `SetMediaPlaybackState` and five more | **no**, five of six |
| **Vita** | Vita3K | **`sceKernelPowerTick(type)`** | **no**, the type is dropped |
| **3DS** | azahar | **`ReplySleepQuery(app_id, reply)`** | **no**, `(STUBBED)` |
| **Wii U** | Cemu | **`OSEnableHomeButtonMenu(bool)`** | **yes** — `g_homeButtonMenuEnabled = enable` |

**Vita's carries granularity the other three do not**, of the four read here.
`SceKernelPowerTickType`:

```
SCE_KERNEL_POWER_TICK_DEFAULT               = 0  // cancel all timers
SCE_KERNEL_POWER_TICK_DISABLE_AUTO_SUSPEND  = 1
SCE_KERNEL_POWER_TICK_DISABLE_OLED_OFF      = 4  // <- the cutscene case
SCE_KERNEL_POWER_TICK_DISABLE_OLED_DIMMING  = 6
```

**A game calls `DISABLE_OLED_OFF` while the user watches and does not touch
anything.** Vita3K receives it and returns `SCE_KERNEL_OK`, discarding the type.

**The 3DS one is a two-way protocol**, which is stronger still: the OS *asks*
whether it may sleep and the application *replies*. azahar logs both the caller
and the reply value and drops them.

**Of the four forks read, Cemu is the only one that keeps anything**, and it
keeps it to emulate the
HOME menu rather than to describe guest activity. **A Wii U game disables the
HOME button exactly when interrupting would be bad** — saving, a cutscene, a
transition.

## Why this is better than the decode path

`AGENT_LOOP.md` proposes inferring the state from whether the video decoder is
running — `cellVdec`, IPU, `nvdec`, `mvd`, `SceAvcdec`, XMA.

**That works and it is a fallback, but it is weaker in three ways:**

1. **It is inference. This is a declaration.** The game is stating its intent in
   its own words.
2. **A decoder running does not mean the frame is a movie.** A game may decode a
   video into a texture on a screen inside the world, or play an audio stream
   through the same path.
3. **It misses the non-video cases.** A real-time engine cutscene renders with
   the normal pipeline and touches no decoder — **but it still disables sleep**,
   because the user is watching.

**Point 3 is the important one.** Most in-engine cutscenes are not videos.

## What to do

- **Store what the guest already tells you.** Five assignments in eden, one in
  Vita3K, one in azahar. **This is the cheapest capability in the whole agent
  loop plan.**
- **Put it in the backend contract as a declared activity state**, not as a
  video-decoder query. The contract row is the state; how a backend derives it is
  backend knowledge, exactly like the texture key.
- **Keep the decode path as a second input**, not the primary one.
- **`ReportUserIsActive` deserves its own contract row.** An agent that injects
  input and wants to know whether it registered has, on Switch, the guest's own
  answer.

## Limits

- **PS3 and Xbox 360 were not checked** for the equivalent API. rpcsx has a
  `thor_playback_probe.h` under active modification by another session, so that
  fork is being worked on and was left alone.
- **No claim that these calls are reliable.** A game may never call them, or call
  them wrongly. **The signal needs calibration against titles whose behaviour is
  known**, the same discipline the HLE stub fraction needs.
- **Nothing is measured**, and no title has been observed making these calls.
- **This is a read of four forks, not eight.** melonDS, ARMSX2, xenia and
  GameThor were not examined; the DS has no such API, and the others are
  unchecked.

## Method note

**Found by `tools/capability_probe.py`**, which searches a capability with
several independent vocabularies. The probe that found it was "an explicit state
word", `GuestActivity|PlaybackState|is_playing_video|playback_probe`. **A search
for the video decoder alone — which is what the existing design document
proposes — would have missed all of it.**

## Sources

- eden `src/core/hle/service/am/service/self_controller.cpp`
- Vita3K `vita3k/modules/SceProcessmgr/SceProcessmgr.cpp`,
  `vita3k/rtc/include/rtc/rtc.h`
- azahar `src/core/hle/service/apt/apt.cpp`
- Cemu `src/Cafe/OS/libs/coreinit/coreinit_Misc.cpp`
