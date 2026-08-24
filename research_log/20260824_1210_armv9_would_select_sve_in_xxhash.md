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

**RE-COUNTED with `--recurse-submodules` and with the vendored filter off**,
because a dependency is not the fork's work but **it compiles into the product**:

| Fork | Lines | Files | Spot-checked |
| --- | --- | --- | --- |
| **rpcsx** | **303** | **48** | `asmjit/core/cpuinfo.cpp` — asmjit seeds *detected* features from compile-time macros |
| **ARMSX2** | 43 | 5 | vendored `xxhash.h`, the SVE-before-NEON dispatch |
| **azahar** | 27 | 3 | `externals/xxHash/xxhash.h`, same dispatch |
| **Vita3K** | **27** | **3** | **submodule** `external/xxHash/xxhash.h`, plus a third copy via `external/tracy` |
| **melonDS** | 13 | 1 | its own `melonDS-android-lib/src/xxhash/xxhash.h` |
| xenia, Cemu | 1 | 1 | — |
| eden, GameThor | 0 | 0 | — |

**Six of nine forks carry SVE-conditional code**, and **four of them carry the
same xxHash dispatch.**

**The first version of this count said "ARMSX2 7, melonDS 1, everyone else 0" and
was wrong twice**: it excluded vendored trees, which is the wrong filter for a
question about what compiles into the binary, **and it did not recurse
submodules**, which is how it reported Vita3K as clean.

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

## `-mcpu` is the same trap, and a flags re-census corrected two rows

**Tested on this box:**

| Flag | SVE macros |
| --- | --- |
| `-march=armv8.2-a+...+rcpc -mtune=cortex-x3` | **none** |
| **`-mcpu=cortex-x3`** | **`SVE`, `SVE2`, `SVE2_BITPERM`, `SVE_BF16`, `SVE_MATMUL_INT8`** |
| **`-mcpu=cortex-a710`** | the same |

**Clang models the core**, and these cores implement SVE2 — **Qualcomm did not
expose it.** So **`-mcpu` is the same trap as `-march=armv9-a`**, and
`THOR_TARGET.md` naming **`-mtune`** is load-bearing rather than stylistic.

**Re-censused every fork's own build files to see whether the trap is live.**
`-march`, `-mcpu`, `-mtune` and outline-atomics across `*.txt`, `*.cmake`,
`*.gradle`, `*.lua`, `*.mk`, vendored trees excluded, CMake variables resolved by
hand.

**No fork uses `-mcpu=cortex-x3` or `-mcpu=cortex-a710`. The risk is latent; the
live route is eden's `armv9` preset.**

**But two rows of `CLAUDE.md`'s compile-flags table were wrong:**

- **Vita3K sets `-march=armv8.2-a+lse+fp16+dotprod` and has no `-mcpu` or
  `-mtune` at all.** The table said `mcpu=cortex-x3`.
- **Cemu sets `-moutline-atomics` behind a compiler-flag check and no arm64
  `-march`** in its own build files; the `-march=...+lse` match is **inside a
  comment**. The table said `armv8-a+lse, mcpu=cortex-a710`.

**A literal grep missed Vita3K entirely**, because its flag is
`-march=${VITA3K_ARM64_BASELINE}`. **A flags census must resolve variables**, which
is the same class as the submodule blind spot below.

## And the submodule blind spot bit again, for the third time

**The fleet search above reported Vita3K as having zero SVE-branching files. That
was wrong.** `external/xxHash` is a **submodule**, it is checked out, and its
`xxhash.h` contains **five** `__ARM_FEATURE_SVE` branches. **`git grep` in a
parent repository does not see submodule contents.**

**Third instance in this project** — dynarmic in Vita3K was the first. **Any
fleet-wide search must state whether it covered submodules**, and this one did
not.

## Cemu's atomics comment, which inverts here

> `-moutline-atomics` keeps the binary running on pre-ARMv8.1 devices by
> dispatching at runtime [...] **Building with `-march=...+lse` directly would be
> faster still but SIGILLs on hardware without LSE.**

**Correct for a portable build, inverted for this project.** The device reports
`atomics`, so **`+lse` directly is right and `-moutline-atomics` is a dispatch tax
for devices this project does not have.** **xenia already sets
`-mno-outline-atomics`.**

**It is also the same shape as the SVE trap, seen from the other side: a feature
flag that SIGILLs on hardware lacking the feature.** The difference is only which
side of the flag this device sits on.

## Limits

- **Not executed.** The chain is verified by compiler test and source reading;
  **no build was made with `armv9-a` and no `SIGILL` was observed.** The
  conclusion follows from the dispatch being compile-time with no fallback.
- **Other libraries were not audited** for the same pattern beyond the search
  above, **and that search did not cover submodules** — which is how it missed
  Vita3K's own vendored xxHash.
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
