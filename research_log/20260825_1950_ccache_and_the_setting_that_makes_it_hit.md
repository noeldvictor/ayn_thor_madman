# ccache for the NDK build, and the one line that decides whether it ever hits

**Goal: open decision 3 asks where builds happen, and this repo measured that
build time is dominated by SOURCE-BUILT DEPENDENCIES. XenDroid caches its native
build. Read how.**

**Device-free: one commit diff. No device used.**

## The mechanism, and it is four lines

```gradle
if (System.getenv('USE_CCACHE') == 'true') {
    arguments '-DCMAKE_C_COMPILER_LAUNCHER=ccache',
              '-DCMAKE_CXX_COMPILER_LAUNCHER=ccache'
}
```

**`CMAKE_*_COMPILER_LAUNCHER` is the correct way to put ccache in front of an
Android NDK build**, because Gradle's `externalNativeBuild` owns the toolchain
and there is no compiler path to substitute. **Gated on an environment variable,
so a local build is unaffected unless it opts in.**

## The line that decides whether the cache ever hits

```yaml
CCACHE_COMPILERCHECK: content
```

**ccache's default identifies the compiler by its mtime and size.** The NDK is
restored from a cache with **fresh timestamps on every run**, so with the default
**every restore invalidates the entire ccache** and it silently never hits.

> **A cache that is configured, populated and never hit is the same shape as a
> setting that is stored and never applied.** It fails by being slow, which
> nobody investigates, and there is no error.

**`CCACHE_BASEDIR` is the sibling detail**: it rewrites absolute paths to
relative so a cache built in one checkout directory hits in another.

**Both are one line each and neither is discoverable from a failure**, which is
why they are worth recording rather than re-deriving.

## Why this bears on an open decision

**`CLAUDE.md` open decision 3 asks whether builds run locally or on CI**, and its
own evidence is four CLEAN builds — Cemu ~2 min 42 s native, ARMSX2 11 min 25 s,
azahar 14 min 33 s, melonDS 15 min 27 s. **Its stated conclusion is that
source-built dependencies dominate, not emulator size.**

**And it already says the measurement is the wrong one:** *"Measure incremental
separately before choosing, because a clean-checkout figure is the wrong input
for a decision about routine agent work."*

> **ccache attacks exactly the dominant cost.** A dependency that has not changed
> is compiled once and then never again — **which is the same benefit vcpkg gives
> Cemu, obtained without vendoring prebuilt binaries.**

**It applies locally, not only in CI.** The four measured numbers are the cost of
a cold checkout; **nobody has measured what a second build costs with a warm
ccache**, and that is the number the decision actually needs.

**A second cache, separate from ccache**: the NDK and CMake installs themselves,
keyed by version. **Downloading an NDK is a large fixed cost that ccache cannot
touch.**

## What to do with it

**Not a change to any fork.** The recipe is four lines of Gradle plus three
environment variables, and **the useful next step is a measurement this repo can
take with no device**: build one fork clean, build it again with ccache warm, and
report both. **That turns open decision 3 from a guess into a comparison.**

**Recorded rather than run**, because a second full build of a fork is a
long-running job and this session has been avoiding those.

## Limits

- **One commit diff.** Nothing built, nothing measured, no device.
- **The 2 GB cache size and the cache keys are XenDroid's**, tuned for GitHub
  Actions runners rather than for this machine.
- **ccache does not help Rust**, and melonDS's build is dominated by compiling
  `librashader` and its crate graph. **`sccache` is the analogue there and was
  not investigated.**
- **No claim about how much ccache would save here.** The argument is structural:
  it caches the thing this repo measured as dominant.

## Sources

- XenDroid, `[CI] Cache the native build (ccache) and the NDK/CMake install`
- `CLAUDE.md`, open decision 3 and the four clean-build measurements
