# melonDS-android clean build: the fleet's first recorded build time

**Goal: start `CLAUDE.md` Phase 0.3 — build every Tier 1 fork as it stands
today and record the result.**

Session 2026-08-23 00:06. The rule is *"you cannot migrate a toolchain you
cannot build"*, and **no fork had a recorded clean-build time.** melonDS-android
was chosen because it is the only fork with a build recipe verified on this box.

**Result: BUILD SUCCESSFUL in 15m 27s.**

---

## The numbers

| | |
| --- | --- |
| **Clean build** | **15 min 27 s** |
| `clean` task alone | 1 min 3 s |
| Tasks | 89 actionable, 86 executed |
| APK | **58,229,664 bytes**, 55.5 MB |
| Variant | `gitHubProdDebug`, `arm64-v8a` |
| Exit code | 0 |
| Warnings | ~170 lines, mostly deprecated `codecvt_utf8_utf16` |

**Built as it stands today, not to the standard row.** That is what Phase 0.3
asks for: measure the fork before migrating it.

| | This fork today | The standard row |
| --- | --- | --- |
| NDK | **28.0.13004108** | 29.0.14206865 |
| Gradle | **9.5.0** | 9.6.1 or newer |
| JDK | 21 | — |

**So melonDS is one NDK major and one Gradle minor behind the row**, and both
have to move before it can share native code with anything.

## Where the time goes, and it is not the emulator

**The build is dominated by compiling `librashader` from Rust source.** The log
shows the full dependency graph being built — `glslang`, `spirv-cross2`,
`gpu-allocator`, `image`, `parking_lot`, then eight `librashader-*` crates.
`rustc` alone accumulated 86 seconds of CPU while other tasks waited.

**The emulator's own C++ compiled quickly and quietly**, producing only warnings
about deprecated `codecvt`.

**Two consequences:**

1. **A shared-layer change will not cost 15 minutes on this fork; a clean
   checkout will.** The expensive part is a third-party Rust library that
   changes rarely, so incremental builds should be far cheaper. **That is worth
   measuring separately before anyone plans CI around this number.**
2. **librashader is a build-time dependency for slang shader support**, which
   `CLAUDE.md` keeps deliberately while rejecting the rest of libretro. **It is
   also the single largest build cost in the fork.** If the shared layer takes
   present-time filtering, this cost moves with it.

## An incidental confirmation

The build regenerated `app/src/main/cpp/renderer/VulkanPlaneFilterMode*ShaderData.h`
as a build step.

**That confirms directly what the native census inferred**: melonDS's "Vulkan
layer" looked like 123,260 lines partly because roughly 32,000 of it is
generated shader data. **Generated files inflate a line count and they are not
code anybody wrote.** The census trap was real, and this is the proof rather
than the inference.

## Method notes

- **Built from PowerShell, not Git-Bash.** The fork's own notes record that
  Git-Bash exported variables do not reach gradle's forked daemon, and that the
  resulting cargo failure looks like an SDK problem and wastes an hour.
- The recipe's required environment was set exactly as recorded: JDK 21 over the
  persistent JDK 17, `ANDROID_NDK_HOME`, `CARGO`, `RUSTUP`, `GIT`, and **`TEMP`
  and `TMP` overridden** because non-interactive shells default `TEMP` to
  `C:\WINDOWS\`, which KSP rejects.
- `git config core.longpaths true` was already set; librashader has test paths
  over 260 characters.

**The recipe worked unchanged.** It was written on 2026-07-12 and is still
accurate, which is worth recording because a stale recipe is worse than none.

## A trap that invalidated a different fork's number

**`gradle clean` does not remove `.cxx/`.** It cleans `build/`, and the native
CMake and ninja artifacts survive — so a "clean build" can silently reuse a
previous native compile and report a time several times too fast.

**Cemu was measured at 4 min 27 s and it was not a clean build.** Its log
contains **zero** compilation lines and ninja found nothing to do. Removing
`app/.cxx` explicitly and rebuilding is the only way to get the real figure.

**This build and azahar's and ARMSX2's were checked and are genuine.** The
evidence differs by fork because the output formats differ, so check all three
of:

- `buildCMake…[abi]` tasks actually running,
- ninja progress lines such as `[1428/2206]`,
- **compiler warnings** — melonDS emitted 15 and ARMSX2 77, which proves a
  compiler ran even when no per-file lines are echoed.

**A number nobody checked this way is not a clean-build number.**

## What is not done

**Seven Tier 1 forks remain unbuilt**, and the expensive ones are deliberately
last: ARMSX2, Cemu-thor and xenia-thor.

**This number is one data point, not a baseline.** It is a debug build of one
variant on one machine with a warm Gradle daemon and a warm Cargo registry. **A
cold-cache figure would be higher**, and nothing here says by how much.
