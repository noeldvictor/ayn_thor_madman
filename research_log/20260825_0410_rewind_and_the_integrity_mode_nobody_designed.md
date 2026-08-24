# Rewind is built, priced at 20 MB a state, and it collides with an integrity mode this project never designed

**Goal: examine `rewind`, the other required universal hotkey this repo names and
has never analysed.**

**Two findings. Rewind exists, complete, with its UI. And chasing why it can be
disabled found a cross-cutting mode that governs five features at once, four of
which this project has already specified separately.**

## 1. melonDS has rewind, and the design is small and worth taking

`RewindManager.h` is 70 lines.

**The instrument, named:**

```sh
git -C <fork> grep -lI --recurse-submodules -E   'rewind|Rewind|REWIND'                                   'runahead|run_ahead|RunAhead'                            'rollback|Rollback'                                      'state_?ring|ring_?buffer.{0,20}state|history.{0,15}state'   -- '*.cpp' '*.h' '*.kt' '*.java'
```

**Run separately per vocabulary over all nine forks**, vendored trees removed by
excluding `externals?/`, `third_party`, `3rdparty` and `dependencies`, counting
files:

| Fork | rewind | runahead | rollback |
| --- | --- | --- | --- |
| **melonDS** | **55** | 1 | 0 |
| **ARMSX2** | 14 | **8** | 2 |
| eden | 10 | 0 | 2 |
| rpcsx | 9 | 0 | 5 |
| Cemu | 7 | 0 | 2 |
| GameThor | 6 | 0 | 1 |
| xenia, Vita3K, azahar | 1-2 | 0 | 0-1 |

**The design:**

```cpp
struct RewindSaveState {
    u8* buffer;  u32 bufferSize;  u32 bufferContentSize;
    u8* screenshot;  u32 screenshotSize;  int frame;
};
```

- **Configured in seconds, not in slots.** Length and capture interval; the
  window size is **derived**: `RewindLengthSeconds / RewindCapturingIntervalSeconds`.
- **A screenshot per state.** That is what makes it a **timeline the user scrubs**
  rather than a button pressed blindly. The strings confirm it: `rewind_now` is
  **"NOW"**, and the labels are `%1$ss` and `%1$dm%2$ss` — *"43.56s"*,
  *"2m37.93s"*.
- **The emulator asks, the manager answers**: `ShouldCaptureState(frame)`, then
  `GetNextRewindSaveState(frame)`, then `OnRewindFromState` truncates forward
  history.
- **Buffers are preallocated at a fixed size.**

## And the price is stated, which is the part that matters here

```cpp
const int kRewindBufferSize     = 1024 * 1024 * 20;   // Use 20MB per savestate
const int kRewindScreenshotSize = 256 * 384 * 4;      // ~384 KB
```

> **~20.4 MB per state, preallocated, for the smallest guest in the fleet.**

**A ten-second window at one-second spacing is ten states — over 200 MB.**

**`CLAUDE.md` says there is ONE memory budget owner**, arbitrating between the
texture cache and the shader cache on a device with a hard ceiling. **A rewind
window is a third claimant, it is large, and it is user-tunable in a settings
screen.** Nothing in the contract lets the budget owner see it.

**And the cost does not generalise**, which is the reason rewind cannot be a
uniform contract feature. A DS state is 20 MB; a Switch, Wii U or PS3 state is a
different order of magnitude. **Rewind is a declared per-backend extension, and
what it must declare is the per-state cost**, so the budget owner can refuse a
window the device cannot hold.

**melonDS tells the user this in plain words**, which is the standard to meet:

> *"you may experience occasional stutters if your device is not powerful
> enough. A considerable amount of memory is also used depending on how often
> you want the state to be captured and for how long it should be kept."*

**Runahead is a lead, and my first count of it was wrong.** Searching only
`runahead|run_ahead|RunAhead` gave ARMSX2 8 files and melonDS 1, which I nearly
wrote up as "ARMSX2 has runahead and nothing else does". **Re-searched with four
vocabularies** — `run.?ahead`, `input.?latency`, `latency.?reduc`,
`frame.?delay` — **and two forks have runahead**, ARMSX2 with 49 occurrences and
**melonDS with 5**. Input-latency vocabulary appears in seven of eight forks
(ARMSX2 56, Vita3K 39, azahar 34, Cemu 33, rpcsx 33), **and much of that is
certainly audio latency from Oboe rather than input**, so no claim is made about
it. **The occurrences were counted and not read.**

**Why it is a lead at all:** runahead trades CPU for **input latency** by running
ahead and rolling back, and `CLAUDE.md`'s frame-generation section already
reasons carefully about a **held frame of latency** on a device with real
buttons. **The two are the same budget and this repo discusses only one of
them.** Not read here.

## 2. Chasing why rewind can be off found a mode governing five features

melonDS: `rewind_unavailable_ra_hardcore_enabled` — **"Rewind is unavailable
while Hardcore mode is enabled."**

ARMSX2 states the full set in its own UI text:

> *"hardcore mode also prevents the usage of **save states, cheats and slowdown
> functionality**."*

**So achievement integrity mode governs, at least:**

| Feature | This project's status |
| --- | --- |
| save states | in the minimum contract |
| **cheats** | **a first-class feature, with a library and a search** |
| **rewind** | just specified above |
| **slowdown** | **`TimeScale` below 100%**, written today |
| **patches with cheat intent** | the patch engine |

> **Five features this repo specified separately, and one mode that must reach
> all of them. It is in the contract nowhere.**

### ARMSX2 shipped two bugs here and both validate a decision already made

**`Patch.cpp:370`, in its own words.**

**Bug one — the gate was too wide, and asymmetric.**

> *"Hardcore blocks CHEATS, not fixes. The old condition dropped every on-disk
> pnach, which is asymmetric with the fallback below: the bundled `patches.zip`
> stays enabled in hardcore, so a widescreen/no-interlace/bug-fix patch worked
> when it shipped in our zip and silently did nothing when the same patch sat on
> disk. That killed everything the in-app Patch Manager writes (it only ever
> writes to disk) the moment a user turned hardcore on, **with no message
> explaining why**."*

**A sixth variant of the settings symptom: the user moved a switch and something
ELSE stopped working, silently.**

**Bug two — cheats and fixes need different identity scoping.**

> *"CHEATS are matched across ALL CRCs of the serial ... the in-app cheat editor
> writes `{serial}_00000000.pnach` whenever it can't read a live CRC ... and
> PATCHES the user pastes are commonly named for a different revision's CRC —
> either way a CRC-specific boot-time glob silently drops them (**"no cheats
> found" with the file sitting right there in the list**). ... **Real
> fixes/widescreen stay CRC-specific at boot so a wrong-revision graphics patch
> can't auto-apply.**"*

**`app/GAME_DATA.md` already says "a cheat targets a `GameKey`; a code patch
usually targets a `DumpId`."** **ARMSX2 is the shipped-bug proof of that
sentence, and it supplies the reason the design did not have**: the asymmetry is
not preference, it is that **a wrong-revision cheat is harmless and a
wrong-revision graphics patch is not.**

**Drop the word "usually".** And **the intent field earns two more jobs**.
`CLAUDE.md` justifies patch intent — speed, fix, or change — as a **UI
grouping**. It decides three things:

1. **whether integrity mode blocks it**
2. **whether it binds to `GameKey` or to `DumpId`**
3. **whether it may auto-apply at boot**

> **A field justified cosmetically turned out to be load-bearing three times
> over. That is the argument for declaring intent on every patch from the
> start.**

## Limits

- **melonDS's rewind read; its Kotlin UI not read.** The timeline is inferred
  from the string resources and `GetRewindWindow()`, not from the screen.
- **The 20 MB is melonDS's constant, not a measured state size.** It is a
  preallocated bound.
- **No default window length found** in the resources searched, so the 200 MB
  figure is an example at a plausible setting, not a shipped default.
- **Runahead was counted with four vocabularies, and not read in any fork.**
- **RetroAchievements is one integrity regime.** Whether this project wants
  achievements at all is undecided; the mode is worth designing regardless,
  because the same gate serves any "this run should count" claim.
- **Nothing measured. No device.**

## Sources

- melonDS `app/src/main/cpp/RewindManager.h`, `MelonInstance.cpp:46-47`,
  `res/values/strings.xml:137-141,225`
- ARMSX2 `pcsx2/Patch.cpp:366-395`, `pcsx2/ImGui/FullscreenUI_Settings.cpp:5987`
- `app/GAME_DATA.md`, `app/shell/TimeScale.kt`
