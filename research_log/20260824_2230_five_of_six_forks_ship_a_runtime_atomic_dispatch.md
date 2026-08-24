# Five of six shipped binaries route every atomic through a runtime dispatch, and one fork wrote down why

> ## CORRECTED THE SAME DAY, AND THE CORRECTION IS THE BIGGER FINDING
>
> **Vita3K is not a victim. It fixed this on 2026-08-21 and measured it**, and
> **the binary measured below is from 2026-05-18 — three months older than the
> fix.** I read `emitted_flags.py`'s own limit, *"a stale `compile_commands.json`
> describes the last build, not the current source"*, and then did not apply it.
> **Every binary needs its build date recorded beside it.** The other five were
> all built within the last week and their rows stand.
>
> **Corrected counts:**
>
> | question | instrument | answer |
> | --- | --- | --- |
> | which shipped binaries dispatch | six binaries, five current | **four of five**; xenia does not |
> | **which forks set `+lse` in current source** | **build files, all eight forks** | **two: xenia and Vita3K** |
>
> **And Vita3K's numbers are far larger than mine**, because it counted call
> sites across the whole shipped library rather than one emulator target:
> **25,276 outline call sites to 682, and 30 inline LSE instructions to 24,552.**
> `__aarch64_swp1_acq_rel` alone was **12,074** and `__aarch64_ldadd8_acq_rel`
> **10,518** — refcount traffic, not cold paths. **That weakens my FLAT
> prediction in `DEVICE_QUEUE.md` entry 25**, which is recorded there.
>
> **The residual 682 is the finding nobody had:** they are all inside **prebuilt
> vcpkg static libraries** — libc++ locale, `boost::filesystem`, OpenSSL —
> *"which vcpkg builds under its own `arm64-android` triplet and so never see our
> `-march`."*
>
> > **A prebuilt dependency does not inherit your compile baseline.**
>
> **That interacts with a decision this project already made.** Cemu is the
> fastest fork to build precisely because **it compiles none of its
> dependencies** — vcpkg supplies them prebuilt. **So a `-march` change cannot
> reach Cemu's dependency atomics at all**, and its 406 outline call sites are
> mostly out of reach of the one-line fix. **Reaching them needs a custom vcpkg
> triplet**, which is the same unit-of-work argument as the shared device layer.
>
> **Vita3K also rejected `-mcpu=cortex-x3`, for a different reason than this
> repo's.** Not SVE: *"that would also pick an X3-specific scheduling model, and
> the same code runs on A510 little cores."* **Two independent reasons to reject
> it, from two forks, neither citing the other.**


**Goal: run the applicability count before proposing a compile-baseline change,
which is the rule this repo adopted from rpcsx's 0.04% audit.**

**The question I opened was whether to raise `-march` to `armv8.4-a` for LSE2.
The count closed that question and found a larger, older one underneath it.**

## Method

**Disassembled six forks' shipped `arm64-v8a` libraries** with the standard row's
`llvm-objdump` (NDK 29.0.14206865), counted mnemonics, and read the outline-atomic
helper symbols with `llvm-nm`. **No device. No fork modified.** eden and GameThor
do not build here and are absent, which means unmeasured, not clean.

## The question I asked, answered NO

**LSE2 changes what clang emits for a 128-bit atomic, and the difference is
large.** Measured on the same NDK, same triple:

| | `armv8.2-a+lse` | `armv8.4-a` |
| --- | --- | --- |
| 16-byte atomic **load** | **`caspa`** — a compare-and-swap, so a *read* takes the line exclusive | **`ldp` + `dmb ishld`** |
| 16-byte atomic **store** | **an 11-instruction `caspl` retry loop** | **`dmb ish` + `stp`** |

**And the fleet does not do it.** `casp` count across all six binaries:

| fork | instructions | `casp` | `ldxp`/`stxp` |
| --- | --- | --- | --- |
| Cemu | 6,272,674 | **0** | 0 |
| ARMSX2 | 5,074,140 | **0** | 0 |
| Vita3K | 7,784,877 | **0** | 0 |
| azahar | 4,163,320 | **0** | 8 |
| xenia | 1,761,001 | **0** | 0 |
| melonDS | 857,606 | **0** | 0 |

> **Zero 128-bit atomics in 25.9 million instructions.** The LSE2 case has no
> consumer here. **Question closed, not queued** — and it cost one command,
> which is the argument for the applicability-count rule.

## What the same count found instead

**The atomics that DO exist mostly go through a function call.**

| fork | direct LSE instructions | `ldaxr`/`stlxr` | outline helper symbols | outline call sites |
| --- | --- | --- | --- | --- |
| **xenia** | **1,998** | 10 | **5** | **53** |
| Vita3K | 29 | 50 | 30 | not counted |
| Cemu | 38 | 60 | 38 | **406** |
| ARMSX2 | 28 | 51 | 28 | **886** |
| azahar | 20 | 34 | 21 | not counted |
| melonDS | 6 | 10 | 6 | not counted |

**All six carry `__aarch64_have_lse_atomics`**, so all six ship the runtime
dispatch. **Only xenia emits LSE at scale.**

An outline atomic is not the instruction. It is a `bl` to a stub that `adrp`s a
global feature byte, loads it, branches, and then executes either the LSE
instruction or an `ldxr`/`stxr` loop. **Per atomic that is a non-inlinable call
plus a load of a global plus a branch, to decide something that is constant for
the life of the device.**

## The chain, verified end to end

**ARMSX2's emitted flags, from `compile_commands.json` rather than a build file:**

```
config=Release  -march=armv8-a+crc                    1337 translation units
config=Debug    -march=armv8.2-a+fp16+dotprod         1967 translation units
                -march=armv8-a
```

**No `+lse` anywhere** — and its binary has **886 outline call sites against 28
direct LSE instructions**. **xenia sets `+lse` and inverts the ratio.**

> **The flag, the emitted code and the shipped binary agree. This is the
> `emitted_flags.py` rule producing a result rather than a caution.**

## And Cemu wrote the whole analysis down, then chose the other way

`CMakeLists.txt:260`, in its own words:

> Baseline armv8-a assumes no FEAT_LSE, so **every `std::atomic` read-modify-write
> compiles to an `ldxr`/`stxr` retry loop** instead of a single CAS/LDADD. Cemu's
> hot paths are full of these (**FSpinlock, coreinit spinlocks, the striped atomic
> HLE**) and **LL/SC also livelocks harder under contention, which is exactly the
> multi-core guest case.**
>
> `-moutline-atomics` **keeps the binary running on pre-ARMv8.1 devices** by
> dispatching at runtime, so it is safe as the default. Building with
> `-march=...+lse` directly **would be faster still but SIGILLs on hardware
> without LSE**; if that is ever wanted **it should be an explicit opt-in build,
> not the default.**

**Nothing in that is wrong.** It is a correct decision for a project that ships
to unknown ARM devices.

**It is the DELETE operation exactly.** Cemu pays a dispatch on every atomic to
serve **variability this device does not have**: one SoC, and
`/proc/cpuinfo` reports **`atomics`**, which is `FEAT_LSE`. **The portability the
dispatch buys is worth nothing here, and Cemu named the condition under which to
drop it — an explicit opt-in build. This project is that build.**

**`hardware_ref/thor/THOR_TARGET.md` already says `+lse`.** What was missing was
evidence that anyone needs telling. **Five of six forks need telling.**

## The trap that sits next to the fix, already pinned

**`-mno-outline-atomics` on its own is worse than the default.** Without `+lse`
the compiler cannot emit an LSE atomic at all, so removing the dispatch removes
the runtime upgrade path and leaves the `ldxr`/`stxr` loop unconditionally.
**`tools/target_check.py` probe 3 fails on exactly that flag combination.**

**The fix is `+lse` in `-march`. `-mno-outline-atomics` is optional after it and
useless before it.**

## What is NOT claimed

- **No frame, no millisecond, no watt.** This counts instructions and call sites.
  **An outline call is cheap in isolation; whether it matters depends on atomic
  density in the hot path, which is unmeasured.**
- **Cemu names FSpinlock and coreinit spinlocks as hot. That is Cemu's claim**,
  not a profile taken here.
- **Debug and Release configurations are mixed** across the six binaries, because
  they are what exists on this machine. ARMSX2's flag reading covers both and
  neither has `+lse`.
- **Call sites were counted for three forks, helper symbols for six.** The two
  columns are different instruments and are labelled as such.
- **eden and GameThor are absent because they do not build here.**
- **`+lse` raises the `SIGILL` floor to Armv8.1** for anyone running the binary
  elsewhere. This project targets one device; any fork keeping the flag as an
  opt-in is making the correct call for itself.

## Sources

- `tools/emitted_flags.py`, `tools/target_check.py`
- Cemu `CMakeLists.txt:260-276`
- rpcsx `docs/arm64/ledger.md`, open opportunity 2
- `hardware_ref/thor/THOR_TARGET.md`
