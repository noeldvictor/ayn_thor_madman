# Phase 0.3 — build baseline, melonDS-android

Goal: build the first Tier 1 fork as it stands and record the result.

Date: 2026-08-22, 17:04 EDT.

## Result

**Build succeeded.** The recipe recorded in `melonds_HD/CLAUDE.md` on
2026-07-12 still works on 2026-08-22, unchanged.

| Measure | Value |
| --- | --- |
| Fork | `melonds_HD/melonDS-android` |
| Branch and commit | `master`, `0799a154` |
| Working tree | clean |
| Task | `:app:assembleGitHubProdDebug` |
| Exit code | 0 |
| Elapsed | 1m 00s |
| Gradle tasks | 89 total, 9 executed, 80 up to date |
| APK | 64.9 MB, `app-gitHub-prod-debug.apk` |

**This is an incremental build, not a clean build.** 80 of 89 tasks were up to
date and the APK on disk was dated 2026-08-21. The 1 minute figure is not a
baseline build time. A clean-build number is still needed for the
build-location decision.

## Environment verified

All paths from the recipe still exist:

| Item | Path |
| --- | --- |
| JDK 21 | `C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.11.10-hotspot` |
| SDK | `%LOCALAPPDATA%\\Android\\Sdk` |
| NDK 28 | `...\\Sdk\\ndk\\28.0.13004108` |
| cargo, rustup | `%USERPROFILE%\\.cargo\\bin\\` |
| git | `C:\\Program Files\\Git\\cmd\\git.exe` |

JDK 17 is also installed. The recipe is right that JAVA_HOME must be forced to
21.

The build ran from PowerShell, as the recipe requires. The environment
variables were set in the same PowerShell invocation as `gradlew.bat`.

## Toolchain gap against the standard row

melonDS-android does not match
[the standard row](../CLAUDE.md#the-standard-row):

| Setting | melonDS today | Standard row |
| --- | --- | --- |
| NDK | 28.0.13004108 | 29.0.14206865 |
| Gradle | 9.5.0 | 9.6.1 or newer |
| ABI | armeabi-v7a and arm64-v8a | arm64-v8a only |

Migrating this fork is Phase 1 work. Do not change it in the same session as
the baseline.

## Finding: melonds_HD_2 holds the best test design in the fleet

Found while checking for test fixtures. `melonds_HD_2/renderer_cases/` is a
deterministic renderer case corpus. It combines four QA paradigms in one
artifact:

- **Savestate as the fixture.** `input/savestate.ml0`, with `start_frame` and
  `frame_count` for a deterministic range.
- **Golden images.** `expected/software`, `expected/blackmagic3`,
  `expected/blackmagic3_compute3d`.
- **Differential testing.** The software renderer is the reference. Hardware
  renderers are compared against it.
- **No ROM in the repo.** The ROM is identified by sha256, size and header
  fields. `input/` holds notes and a `.gitkeep` only.

Cases are named by behaviour, not by game: `capture_sync`, `sprite_mosaic`,
`rotscale_bg`, `obj_window`, `blend_priority`, `blendcnt_obj_effect_none`,
`forced_blank`. `guards.json` and python guard scripts add host-side checks.
The README states the corpus is for AI-driven renderer cases.

This corrects an earlier claim. CLAUDE.md said no fork had differential
testing. The GPU form of it already exists here. The CPU form still does not.

`melonds_HD_2` stays dropped as a target and is **not** dropped as a source.

## Next

1. Run a clean build to get a real baseline time. This destroys the current
   build cache, so confirm before running it.
2. Build the next fork. Vita3K-Thor is the smallest Tier 1 fork by tracked
   file count.
3. Read `renderer_cases/README.md` and `case.template.json` before designing
   the shared test harness.
