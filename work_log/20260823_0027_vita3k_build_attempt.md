# Vita3K clean build: two failures, and both are one line each

**Goal: continue `CLAUDE.md` Phase 0.3 with the second-cheapest Tier 1 fork.**

Session 2026-08-23 00:27. Phase 0.3 says to record the command, the time and
**the failure**, so this log is mostly failure and that is the deliverable.

**Result: Vita3K does not build as it stands. Neither cause is in its code, and
`arm64-v8a` itself builds fine.**

---

## Failure 1: the fork's own recipe is wrong

`AGENTS.md` says to run from the fork root:

```powershell
.\gradlew.bat ... :android:assembleReldebug
```

**That fails in 21 seconds:**

> Error resolving plugin [id: 'com.android.application', version: '9.2.1'] — the
> plugin is already on the classpath with a different version (8.13.0).

**Two root build files declare two AGP versions.**

| File | AGP |
| --- | --- |
| `./build.gradle` | **8.13.0** |
| `./android/build.gradle` | **9.2.1** |

Running from the fork root loads the outer build first, which pins 8.13.0, and
the inner one then asks for 9.2.1.

**`android/` is a standalone Gradle project.** It has its own
`settings.gradle` — `rootProject.name = "Vita3K"`, `include ':app'` — and its
own wrapper. **Building from inside `android/` works and gets past
configuration.**

**Contrast with melonDS**, whose recipe was written 2026-07-12 and worked
unchanged tonight. **A recipe is only known-good on the day it was run.**

**Not fixed in the fork.** The correction is recorded here rather than committed
to `Vita3K-Thor/AGENTS.md`, because `CLAUDE.md` treats a fork's own documents as
that fork's source of truth and nobody asked for that edit.

## Failure 2: it fails on the ABI the Thor cannot run

Second attempt, from `android/`:

```
BUILD FAILED in 12m 52s
Task :app:configureCMakeRelWithDebInfo[x86_64] FAILED
  CMake Error at external/ffmpeg/CMakeLists.txt:55:
  No FFMPEG prebuilt found with corresponding commit SHA (ccce45f)
```

**The per-ABI outcomes are the finding:**

| ABI | Configure | Build |
| --- | --- | --- |
| **`arm64-v8a`** | **succeeded** | **succeeded** |
| `x86_64` | **FAILED** | never reached |

**Vita3K compiles for the device. It fails only for the architecture the device
does not have**, because the fork ships a prebuilt FFmpeg matched to a commit
SHA and there is none for that ABI.

### And most of the 12m 52s went to the same ABI

The log shows vcpkg building Boost from source for **`x64-android`**, in both
debug and release configurations, before reaching the failure:

```
-- Configuring x64-android-dbg
-- Configuring x64-android-rel
-- Building x64-android-dbg
-- Installing x64-android-dbg
-- Building x64-android-rel
```

**So the unusable ABI cost most of the wall time and then failed the build.**

**Prediction, not yet run: removing `x86_64` from `abiFilters` makes Vita3K
build and cuts the time substantially.** `arm64-v8a` already configured and
compiled, so the remaining risk is in packaging, which was never reached.

This matches what rpcsx measured independently and wrote into its own build
file: adding `x86_64` "doubled the native compile".

---

## The numbers, such as they are

| | |
| --- | --- |
| Attempt 1, from fork root | **failed in 21 s**, AGP conflict |
| Attempt 2, from `android/` | **failed in 12 min 52 s**, x86_64 FFmpeg |
| ABIs attempted | `arm64-v8a`, `x86_64` |
| NDK | `27.3.13750724` |
| Gradle wrapper | 9.4.1 |
| vcpkg deps | already built for `arm64-android`; **`x64-android` built during this run** |

**No clean build time can be recorded for Vita3K**, because it has not
completed one. That is the honest entry.

**Toolchain distance from the standard row is larger than melonDS's**: NDK 27
against 29, and Gradle 9.4.1 against 9.6.1.

## What this says about Phase 0.3

**Two forks attempted, two different obstacles, neither in the emulator.**

- melonDS built, and spent most of its time on a vendored Rust library.
- Vita3K did not build, and spent most of its time on an ABI it should not
  target.

**The pattern so far is that build cost and build failure both live in the
periphery** — vendored dependencies, ABI lists, plugin versions — rather than in
the emulator code that a shared layer would touch.

**That is encouraging for the toolchain migration and discouraging for
estimating it**, because the obstacles are per-fork accidents rather than one
systematic difference.

**Six Tier 1 forks remain.** ARMSX2, Cemu-thor and xenia-thor are still
deliberately last.
