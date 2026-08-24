# `+nosve` clears LLVM's backend and not clang's frontend, and the leftover macro is incoherent

**Goal: check rpcsx's measured claim that a negative attribute reliably clears
the SVE a real Armv9 scheduling model turns on, against this project's own
compile target.**

**The claim is true for the path rpcsx measured and false for the path this
project compiles on. They are two mechanisms wearing one name.**

## What rpcsx measured

`docs/arm64/ledger.md`, open opportunity 3, counting SVE instructions emitted by
clang at `-O3` on identical IR:

| `-mcpu` | SVE instructions | total |
| --- | --- | --- |
| `cortex-a78` | 0 | 46 |
| `cortex-a710`, `cortex-a715`, `cortex-x3` | **7** | 54 |
| `cortex-x3` + `-target-feature -sve -sve2` | **0** | 46 |

Its conclusion: every scheduling model matching Thor's silicon enables SVE by
default, and the JIT's existing `setMAttrs({"-sve","-sve2"})` switches it back
off, **byte-identical to `cortex-a78`**. It adds that "the `+nosve` spelling
works too".

**That is a measurement of the LLVM backend, through `setMAttrs`, with no
preprocessor involved anywhere.** It is correct for a JIT.

## What the frontend does with the same spelling

Measured here on **NDK 29.0.14206865, the standard row's NDK**, and reproduced on
NDK 30:

| flags | `__ARM_FEATURE_SVE` | `__ARM_FEATURE_SVE2` |
| --- | --- | --- |
| `-mcpu=cortex-x3` | defined | defined |
| **`-mcpu=cortex-x3+nosve`** | **cleared** | **STILL DEFINED** |
| `-mcpu=cortex-x3+nosve2` | defined | still defined |
| **`-mcpu=cortex-x3+nosve+nosve2`** | **cleared** | **STILL DEFINED** |
| `-march=armv9-a+nosve` | cleared | still defined |
| `-march=armv8.4-a` | clear | clear |
| **the recorded target line** | **clear** | **clear** |

Plain `-mcpu=cortex-x3` also defines `__ARM_FEATURE_SVE2_BITPERM`,
`__ARM_FEATURE_SVE_BF16`, `__ARM_FEATURE_SVE_MATMUL_INT8` and
`__ARM_FEATURE_SVE_VECTOR_OPERATORS`.

> **No spelling of a negative attribute clears `__ARM_FEATURE_SVE2` from the
> preprocessor.** `+nosve` removes the feature from the backend and leaves the
> frontend advertising the stronger of the two ISAs.

## The leftover state is incoherent, and that is what makes it a trap

`__ARM_FEATURE_SVE2` defined with `__ARM_FEATURE_SVE` undefined describes no
real machine. A dispatcher that tests for SVE2 — reasonably, since SVE2 is the
superset — takes a path the compiler will then refuse:

```
-mcpu=cortex-x3                 -> compiles, emits  uqadd z0.b, z1.b
-mcpu=cortex-x3+nosve+nosve2    -> error: SVE vector type 'svuint8_t'
                                   cannot be used in a target without sve
```

**Both outcomes are bad and they are bad in opposite directions.**

- **Without the negative attribute the code builds and emits a real SVE2
  instruction**, which is a `SIGILL` on this device.
- **With it the failure is a build error**, which is the safe direction — but it
  reads as a toolchain fault rather than as a target misconfiguration, because
  the macro says the feature is present and the compiler says it is not.

## What this confirms, with an emitted instruction rather than an inference

`CLAUDE.md` already refuses `-march=armv9-a` and `-mcpu=cortex-x3` because they
define `__ARM_FEATURE_SVE`. **That rule was reasoned from a predefine. It now has
an emitted instruction behind it**: `uqadd z0.b, z0.b, z1.b`, from an
SVE2-guarded dispatcher, on the standard row's NDK.

**And the named victim is confirmed by reading rather than assumed.** xxHash
dispatches with no runtime check and puts SVE **ahead of NEON**:

```c
#ifndef XXH_VECTOR
#  if defined(__ARM_FEATURE_SVE)
#    define XXH_VECTOR XXH_SVE
#  elif ... __ARM_NEON ...
```

Present in **four forks** — ARMSX2 (twice), azahar, melonDS-android, Vita3K —
plus further copies vendored inside **zstd** and **tracy**.

**All of them guard on `__ARM_FEATURE_SVE`, not on SVE2**, so `+nosve` would in
fact steer xxHash correctly. **The residual hazard is any dispatcher that tests
SVE2 alone, and none was found.**

**The instrument, named:**

```sh
git -C <fork> grep -lI --recurse-submodules -E '__ARM_FEATURE_SVE2|ARM_FEATURE_SVE'
```

**Run over all nine forks. 41 files: rpcsx 29, ARMSX2 5, Vita3K 3, azahar 3,
melonDS 1; xenia, Cemu, eden and GameThor zero.** Every hit read: all are
xxHash, or xxHash vendored inside zstd or tracy. **`--recurse-submodules` is
load-bearing — this repo has now missed a submodule with a plain `git grep`
three times.**

**The limit of that search: it finds a predefine test.** A dispatcher keyed on a
CMake variable, or one that compiles an SVE translation unit unconditionally with
per-file flags, would not appear.

## The transferable rule

> **A feature can be cleared from the backend and left in the frontend. Ask
> which one a claim measured.**

rpcsx's result is correct and its subject is a JIT, where LLVM is called
directly and no macro exists. **This project compiles C++ ahead of time**, so the
frontend is exactly the half its measurement does not cover. **A fork's finding
about its own compilation model is not automatically a finding about ours** —
the same shape as the guest-side against host-side rule already in `CLAUDE.md`,
one layer down.

`tools/target_check.py` already tested both macros and therefore already caught
this: `--flags=-mcpu=cortex-x3+nosve+nosve2` fails its fourth probe. **That was
luck rather than foresight**, and the probe's stated reason has been corrected to
name the surviving macro.

## A separate question this opened, not answered

`-march=armv8.4-a` is **clean of every SVE macro**, and rpcsx moved its AOT
baseline there deliberately **to get LSE2**. The Thor's `/proc/cpuinfo` reports
`ilrcpc`, which is `FEAT_LRCPC2` and is an Armv8.4 feature, so the hardware is
above the recorded `armv8.2-a` baseline.

**Nothing here justifies changing the target line.** Raising a baseline widens
the `SIGILL` surface for every unguarded instruction the compiler may now choose,
and there is no measurement on either side. **Recorded as a question with its
evidence, not as a change.**

## Limits

- **rpcsx's instruction counts were not reproduced here.** Only the frontend
  behaviour was measured.
- **Two NDKs, one triple**, `aarch64-linux-android33`. A different clang may
  differ; the probe is what detects that.
- **Nothing ran on the device.** `uqadd z0.b` is asserted to be a `SIGILL` on the
  Thor from `/proc/cpuinfo` reporting no `sve`, not from executing it.
- **The fleet search covers the two SVE macros only.** A dispatcher keyed on a
  build-system variable rather than a predefine would not appear.

## Sources

- rpcsx `docs/arm64/ledger.md`, "Open opportunities, ranked", item 3
- `tools/target_check.py`, probe 4
- ARMSX2 `3rdparty/include/xxhash.h:3973`
- `hardware_ref/thor/THOR_TARGET.md`
