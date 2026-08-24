# LTO is listed as a packed-binary benefit, and one fork proved it was silently off

**Goal: check Vita3K's finding that its LTO was configured and never applied,
and see what it means for this project's own architecture claim.**

## The finding, and why it is the sharpest kind

Vita3K's `USE_LTO` defaults to `RELEASE_ONLY`, which sets
`CMAKE_INTERPROCEDURAL_OPTIMIZATION_RELEASE`. **That applies to the `Release`
configuration only — and neither shipped build is `Release`:** the APK passes
`-DCMAKE_BUILD_TYPE=RelWithDebInfo` and the Windows build uses
`--config RelWithDebInfo`.

> **So in practice the default means "never."**

**And it was confirmed the only way that counts** — from the emitted flags, not
from the CMake:

> **all 973 translation units compile with `-O2 -DNDEBUG` and no `-flto`.**

**This is the same class as xenia's AOT object cache being enabled only on a
launch path nobody uses**, and as its `rlwinm` fastpaths being `defaultEnabled=true`
in code and `false` on the device. **Three instances now, in three forks: a lever
that is configured correctly and never applies.**

## Why it matters here specifically

**`CLAUDE.md` lists link-time optimisation as a benefit of the packed binary:**

> **Link-time optimisation across the boundary.** The shared layer sits in the
> hot path. Inlining across it is free speed, and a module boundary blocks it.

**That claim is sound and it is not free.** The fleet's own experience is that
**LTO is easy to configure, easy to believe you have, and easy to ship without.**

## The census, and what it can and cannot say

LTO configuration in each fork's own build files, against the build type its
Android gradle actually requests:

| Fork | LTO configs covered | Android build type |
| --- | --- | --- |
| **Cemu** | `_RELEASE` **and** `_RELWITHDEBINFO` | not set in gradle |
| **Vita3K** | `_RELEASE`, `_RELWITHDEBINFO`, bare | **`RelWithDebInfo`, `Release`** |
| **melonDS** | `_RELEASE`, bare | **`RelWithDebInfo`, DEBUG** |
| ARMSX2 | bare | Debug, Release |
| azahar, eden | bare, plus `ENABLE_LTO` | not set in gradle |
| rpcsx | `-flto`, `USE_LTO` | — |
| **xenia, GameThor** | **none found** | — |

**Vita3K now covers `_RELWITHDEBINFO`**, which its own document did not have when
written — so it appears to have been fixed since, and **the document says
"Reported, not changed."**

**What this census cannot say is which fork actually ships with LTO on.** A bare
`INTERPROCEDURAL_OPTIMIZATION` may be set on one target and not another, and a
`set()` in a subdirectory does not reach a parent. **Only the emitted flags
settle it**, which is exactly how Vita3K settled its own.

## The rule

> **If LTO is claimed as a benefit, verify it from the emitted compile commands,
> not from the CMake.**

**Same shape as the two rules this repo already carries** — verify a cache hit
rather than infer it from a full directory, and read the persisted config rather
than the compiled default. **All three are the same failure: a setting that
exists and does not apply.**

**And there is a cheap way to check.** Vita3K read
`android/app/.cxx/RelWithDebInfo/*/arm64-v8a/compile_commands.json`. **Every
CMake-based fork emits one**, so this is a build-time check needing no device.

## Limits

- **No fork's emitted flags were read here except through Vita3K's own report.**
  The table above is configuration, not outcome, and says so.
- **Whether ThinLTO would pay for any fork is unmeasured.** Vita3K calls it "a
  real opportunity" for a header-heavy codebase and explicitly declines to make it
  a drive-by change, because **it moves link time and binary layout enough to want
  its own before and after.**
- **`xenia` and `GameThor` show no LTO configuration**, searched in their own
  build files only; xenia builds through premake rather than CMake, so **absence
  here is weak evidence.**

## Sources

- Vita3K `docs/research/20260821-arm64-review-of-the-vita3k-tree.md`, finding 3
- each fork's own `CMakeLists.txt`, `*.cmake` and gradle files
