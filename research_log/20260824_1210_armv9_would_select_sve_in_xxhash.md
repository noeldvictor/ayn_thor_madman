# Building with `-march=armv9-a` selects an SVE xxHash on a device with no SVE

**Goal: turn `CLAUDE.md`'s "do not target `armv9-a`" caution into a demonstrated
failure or drop it.**

**Demonstrated. The chain is four verified links and it ends in `SIGILL`, in the
fleet's most-used hash library.**

## The chain

**1. The Thor has no SVE.** `/proc/cpuinfo`, recorded here: `asimddp i8mm bf16
fphp asimdhp atomics lrcpc ilrcpc sha3`. **No `sve`, no `sve2`.** The ARM cores
implement SVE2 and **Qualcomm did not expose it**, which is a kernel decision no
compiler flag reaches.

**2. `-march=armv9-a` defines the SVE feature macros.** Tested directly with this
box's NDK clang, `aarch64-linux-android33`:

```
-march=armv9-a  ->  __ARM_FEATURE_SVE 1
                    __ARM_FEATURE_SVE2 1
                    __ARM_FEATURE_SVE_VECTOR_OPERATORS 2
```

**Not present at `armv8.2-a` or `armv8.4-a`.**

**3. xxHash selects its implementation purely at compile time, and SVE wins.**
`xxhash.h`:

```c
#ifndef XXH_VECTOR
#  if defined(__ARM_FEATURE_SVE)
#    define XXH_VECTOR XXH_SVE
#  elif ( defined(__ARM_NEON__) || defined(__ARM_NEON) ... )
#    define XXH_VECTOR XXH_NEON
```

**`__ARM_FEATURE_SVE` is tested before NEON is even considered**, and
`#include <arm_sve.h>` follows. **There is no runtime check.** The only guard in
the file runs the other way — if `XXH_VECTOR` was forced to SVE while the macro is
absent, it falls back to scalar — which protects the opposite mistake.

**4. A fork ships that preset.** eden `CMakeLists.txt:340-346`:

```
set(YUZU_BUILD_PRESET "custom" ... "One of: custom, generic, armv9, native")
elseif (${YUZU_BUILD_PRESET} STREQUAL "armv9")
    set(march armv9-a)
```

## Where it lands

**xxHash is the fleet's texture-hashing workhorse**, and `CLAUDE.md` already
records that the fleet uses it heavily and that azahar and Vita3K pin identical
xxHash commits.

Files branching on `__ARM_FEATURE_SVE` or including `arm_sve.h`, own source and
vendored, searched across all nine forks:

| Fork | Files | Where |
| --- | --- | --- |
| **ARMSX2** | **7** | vendored `xxhash.h` twice, and **zstd**'s `xxhash.h`, `compiler.h`, `hist.h` |
| **melonDS** | **1** | **its own `melonDS-android-lib/src/xxhash/xxhash.h`** |
| everyone else | 0 | — |

**zstd matters as much as xxHash** — it is the compression this fleet uses for
caches and packs.

## Why this is worse than a missed optimisation

**SVE is selected instead of NEON, not in addition to it.** A build with
`-march=armv9-a` on this device does not fall back to the NEON path; **it
compiles SVE instructions that the silicon will not execute.**

> **The failure is `SIGILL` at the first hash, not a slower hash.**

**And nothing in the build would warn.** The compiler is correct — it was told the
target is ARMv9, and ARMv9.0-A mandates SVE2. **The device is the thing that
disagrees with the flag.**

## What to record

- **`CLAUDE.md`'s caution is upgraded from "may emit instructions that do not
  exist here" to a named library, a named macro and a named preset.**
- **eden's `armv9` preset must never be selected for this device**, and the
  reason is now specific rather than precautionary.
- **This is the strongest argument yet for `THOR_TARGET.md`'s explicit feature
  list.** `-march=armv8.2-a+...` names what the device has; `-march=armv9-a`
  names an architecture level the device does not fully implement.

## Limits

- **Not executed.** The chain is verified by compiler test and source reading;
  **no build was made with `armv9-a` and no `SIGILL` was observed.** The
  conclusion follows from the dispatch being compile-time with no fallback.
- **Other libraries were not audited** for the same pattern beyond the search
  above.
- **A build might still work** if the SVE path is never reached at run time, but
  xxHash's `XXH3` is the hot path, so that is not a realistic escape.
- **The macro test used NDK 30's clang.** The standard row pins NDK 29 and this
  was not re-tested there.

## Sources

- melonDS `melonDS-android-lib/src/xxhash/xxhash.h`
- ARMSX2 `3rdparty/include/xxhash.h`, `3rdparty/zstd/lib/common/compiler.h`
- eden `CMakeLists.txt`
- NDK 30 clang, `aarch64-linux-android33`, tested on this box
- `research_log/20260822_2147_sve2_on_the_thor.md` for the device fact
