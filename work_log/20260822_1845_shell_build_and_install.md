# Track A: build the app shell and put it on the device

Goal: build a navigable Compose shell with fake data, install it on the Thor,
and render on both displays.

Date: 2026-08-22, 18:45 EDT.

## Result

**Built, installed and launched. No crash.**

| Measure | Value |
| --- | --- |
| Project | `app/shell/` |
| Task | `:app:assembleDebug` |
| Exit code | 0 |
| Clean build time | **1m 35s**, 36 tasks, all executed |
| APK | 60.9 MB, `app-debug.apk` |
| Install | `Success`, streamed |
| Launch | `topResumedActivity=com.aynthor.shell/.MainActivity` |
| Process | pid 12579, alive |

This is the **first clean-build number** for this project. melonDS was
incremental and did not give one.

### Toolchain used, matching the standard row

`compileSdk 37`, `minSdk 33`, `targetSdk 37`, `arm64-v8a` only, AGP 9.2.1,
Kotlin 2.4.10, Gradle 9.6.1, JDK 21, Compose BOM 2026.06.01.

## Screenshot not captured of the shell itself

**The device was in use.** The main panel was running
`net.rpcsx.easy/RPCSXActivity`, an Eternal Sonata session, and Screen-2 was
showing the Android secondary launcher. The shell had already been foregrounded
and then backgrounded again.

Capturing the shell requires foregrounding it, which interrupts a running game.
Not done. **The launch is proven by `dumpsys`, not by a picture.**

## Finding: AGP 9 removes the Kotlin plugin

First build failed outright:

> The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin
> support since AGP 9.0.

With AGP 9.x a Compose module needs only `com.android.application` and
`org.jetbrains.kotlin.plugin.compose`. `rpcsx-ui-android` already builds this
way; most of the fleet still applies `kotlin.android` and will fail on the
migration. Recorded in `CLAUDE.md`, The standard row.

## Finding: `screencap -d` takes the SurfaceFlinger display ID

**Not the Android display id.** `screencap -d 0` and `-d 4` silently produce a
zero-byte file. No error, exit code 0.

```
usage: screencap [-hp] [-d display-id] [FILENAME]
   -d: specify the display ID to capture (default: 4630946441858561667)
       see "dumpsys SurfaceFlinger --display-id" for valid display IDs.
```

The Thor's IDs:

| Panel | Android displayId | SurfaceFlinger display ID |
| --- | --- | --- |
| Built-in Screen | 0 | `4630946441858561667` |
| Screen-2 | 4 | `4630946482288158084` |

Both capture correctly with the long form. **Any capture tool must use the
SurfaceFlinger ID, and must treat a zero-byte PNG as a failure**, because the
command reports success.

## Finding: write captures through `exec-out`, not to `/sdcard`

`screencap -p /sdcard/x.png` created a zero-byte file. `adb exec-out screencap
-p > local.png` works. Scoped storage is the likely cause. Stream captures out;
do not stage them on the device.

## Finding: Git-Bash mangles adb remote paths

`adb shell ... /sdcard/x.png` becomes `C:/Program Files/Git/sdcard/x.png`.

`export MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'` fixes the remote path, and
then the **local** path must be Windows-style, because `adb.exe` cannot read
`/c/Users/...`.

This adds to the existing rule that Android work belongs in PowerShell. Any
adb helper written in bash on this box needs both settings and mixed path
styles in one command line.

## Finding: Android runs a real launcher on Screen-2

Screen-2 hosts `com.android.launcher3/.secondarydisplay.SecondaryDisplayLauncher`
as its own resumed activity.

**Screen-2 accepts real Activities, not only a `Presentation`.** That is a
second route for the dual-screen design and it was not considered. An Activity
on Screen-2 gets its own lifecycle, its own back stack and normal Compose
support, where a `Presentation` needs ViewTree owners wired by hand.

Decide between them before the layout work goes further:

- `Presentation`: the main activity owns both panels, one process, simple
  coordination.
- Second Activity: normal lifecycle and Compose, but two activities to keep in
  step.

## What the shell contains

Fourteen screens are specified in [`../app/SCREENS.md`](../app/SCREENS.md).
This build implements the core of them:

- Library, with cover-art placeholders and badges for cheats, override, HD
  pack, patch, dual-screen, plus total size and a sort-by-size toggle.
- Game detail, with the guest screen list, the layout picker for dual-screen
  titles and the companion picker for single-screen ones.
- Storage, per category, marked rebuildable or keep, with the cost warning.
- Settings, seven groups, every row marked inherited.
- Drivers, showing the pinned Turnip and the a7xx validation rule.
- Screen-2 companion, drawn on change only.

Fake data covers seven systems and maps to the real fleet backends.

## Next

1. Foreground the shell when the device is free and capture both panels.
2. Decide `Presentation` against a second Activity for Screen-2.
3. Feed the remaining screens from `SCREENS.md`: in-game overlay, cheats,
   patches, input and hotkeys, systems, diagnostics.
4. The build is 1m 35s clean. That is a usable number for the build-location
   decision, for a Kotlin-only module. Native forks will be far slower.
