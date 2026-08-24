# Resume-where-you-left-off is built in two forks, off by default in both, and the OS kill path is uncovered

**Goal: examine the Android-specific product question this repo has never asked —
what happens to a game in progress when the operating system takes the process
away.**

## The mechanism exists, twice, and is disabled

**Searched all eight forks** for Android lifecycle and auto-save vocabulary
(`onPause|onStop`, `onSaveInstanceState|onTrimMemory|onLowMemory`,
`auto.?sav|resume.?state|quick.?sav`, `isFinishing|onDestroy`) over `*.kt` and
`*.java`, vendored trees removed. **ARMSX2 and melonDS are the two with real
implementations**; Vita3K, Cemu and xenia have almost no lifecycle code at all.

| | ARMSX2 | melonDS |
| --- | --- | --- |
| auto-save on exit | `autoSaveOnExit` — **default `false`** | `auto_save_state_on_exit` — **default `false`** |
| **auto-save while playing** | **`autoSaveIntervalMin`** | — |
| **auto-load on boot** | **`autoLoadOnBoot`** | — |
| dedicated slot | **yes**, separate from numbered slots | — |
| screenshot for it | **yes**, `savestate.autosave.screenshotDesc` | — |

**ARMSX2's own UI copy is a better design document than anything in this repo**,
and it makes three decisions in one sentence:

> *"Save automatically while you play, so a crash or a flat battery costs at most
> this much progress. **It writes the same auto-save slot as the option above, so
> your numbered slots stay yours.** Saving pauses the game for a moment, so a
> short interval is felt — **5 minutes is a good starting point**."*

1. **A dedicated auto-save slot.** The user's numbered slots are never touched by
   the machine. **Take this** — it is the difference between a safety net and a
   feature that eats your saves.
2. **The cost is stated, not hidden.** Saving pauses the game.
3. **A recommended default is given**, rather than leaving a number blank.

**`autoLoadOnBoot` is the half that closes the loop.** Save on exit plus load on
boot is *resume where you left off*, which is what a handheld user expects
because it is what the console in their other hand does.

## The decision this repo should make

**Foundation point 4 says the app is for somebody with a full-time job and that
configuration is not the hobby.** **Resume-where-you-left-off is exactly that
feature, and both forks that have it ship it OFF.**

> **Default it ON.** A person who wants numbered slots still has them; the
> auto-save slot is separate by construction.

**Two costs to accept and state, both from ARMSX2's own copy:** saving pauses the
game briefly, and a periodic save at too short an interval is felt.

## The gap neither fork covers: the OS taking the process

**Read melonDS's `EmulatorActivity.onPause` and `onStop`.** `onPause` cancels
presentation refreshes, stops shader diagnostics polling, clears the
fast-forward hold, re-enables the screen timeout and stops the renderer.
`onStop` unregisters the display, input and controller listeners. **Neither
writes a savestate**, and all three `maybeAutoSaveStateOnExit` call sites are on
explicit exit paths that end in `stopEmulator()` and `CloseEmulator`.

> **So on melonDS, an Android process kill loses everything since the last manual
> save.** On a handheld that backgrounds a game to answer a message, that is the
> common path, not the rare one.

**ARMSX2's answer is better and it is not a lifecycle hook.** A **periodic**
auto-save bounds the loss to the interval **regardless of how the process
ends** — kill, crash, or flat battery, which its copy names. **A lifecycle hook
covers only one of those three, and it runs at the worst possible moment**, when
the OS is already trying to take the process away and gives limited time.

> **Prefer a periodic auto-save to an `onPause` hook.** The hook is the obvious
> design and it is the weaker one: narrower coverage, worse timing.

**A lifecycle hook is still worth having as well**, because it makes the common
case lose nothing rather than up to one interval. **The two are complementary and
neither fork has both.**

## It collides with the integrity mode found an hour ago

**`LOAD_STATE` is a guarded feature.** So `autoLoadOnBoot` must be **suppressed
while results are claimed**, or launching a game silently restores a state and
the run is contaminated before the first frame.

**melonDS shows the same collision from the other end**, in its own exit path:
`maybeAutoSaveStateOnExit` sits directly beside `discardHardcoreSubmissions()`.
**It already knew the two features meet at exit.**

## Limits

- **Two forks read, eight counted.** The counts are file hits, not mechanisms.
- **ARMSX2's periodic auto-save was read from its settings keys and its UI copy,
  not from the timer that fires it.**
- **No Android version behaviour was tested.** What Android 13 actually
  guarantees between `onPause`, `onStop` and process death is asserted from the
  platform contract, not measured on the Thor.
- **Nothing here is measured.** The claim that saving pauses the game is
  ARMSX2's, not ours.
- **Not examined: whether an auto-save can be interrupted mid-write**, which is
  the failure that would make the feature worse than not having it. The
  tmp-then-rename rule in `ArtifactStore.kt` is the answer this project already
  has; nothing checks that a backend uses it for savestates.

## Sources

- ARMSX2 `platforms/android/.../i18n/I18n.kt:1544-1551`,
  `runtime/MainActivityRuntime.kt:1205,1377`, `ui/saves/SaveStatePicker.kt:87,297`
- melonDS `ui/emulator/EmulatorActivity.kt:4048,4070`,
  `ui/emulator/EmulatorViewModel.kt:667,1806,1847`,
  `impl/SharedPreferencesSettingsRepository.kt:1566`
- `app/shell/IntegrityMode.kt`
