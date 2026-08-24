# Ground truth from four real builds: nobody ships LSE, one fork ships LTO

**Goal: stop inferring compile flags from CMake and read what actually reached
the compiler.**

**`tools/emitted_flags.py` reads every `compile_commands.json` under each fork —
the command line each translation unit was really compiled with. Four forks have
been built on this machine, so four have ground truth.**

**Five results, and two correct claims this repo made from build files.**

## 1. Of the four forks built here, none ships LSE atomics

**Method: every `-march` string in every `compile_commands.json` under each
fork, vcpkg and buildtrees excluded. Four forks have build output on this
machine; the other five have none, and that is 'not built here', not 'no
flags'.**

**Every `-march` that actually reached the compiler**, across all four builds:

| Fork | Emitted `-march` values |
| --- | --- |
| ARMSX2 | `armv8-a`, **`armv8-a+crc`** (Release), `armv8.2-a+fp16+dotprod` |
| azahar | `armv8-a`, `armv8-a+crc`, `armv8-a+crypto`, `armv8.2-a+dotprod+i8mm`, `armv8.5-a+i8mm+sve2`, `armv9-a+sme` |
| Cemu | **none** — no `-march` at all on arm64 |
| melonDS | `armv7-a` only; **no arm64 `-march`** |

**Not one carries `+lse`.** And **`-mno-outline-atomics` is absent from every
build.**

> **So every atomic in every one of these binaries goes through
> `__aarch64_ldadd8_acq_rel`** — a call, a BTI landing pad, an ADRP, a byte load
> and a branch, to reach the instruction. **Vita3K measured this in its own
> shipped `.so` and this confirms it is the fleet's condition, not one fork's
> mistake.**

**Cemu's is deliberate** — it passes `-moutline-atomics` explicitly and its
comment explains why. **The other three simply never set anything**, and the
NDK's `arm64-v8a` ABI is Armv8.0-A, so **the default does it for them.**

## 2. azahar is the only fork whose build actually gets LTO

**`-flto=thin`, in `RelWithDebInfo`, across 2,183 translation units.**

ARMSX2, Cemu and melonDS: **`-flto` absent.** Vita3K reported its own as absent
in all 973 TUs.

**So the LTO census from build files was directionally right and incomplete**:
several forks configure it, and **only one is confirmed to get it.**

## 3. ARMSX2's Release build has fewer features than its Debug build

| ARMSX2 config | `-march` |
| --- | --- |
| **Release**, 1,337 TUs | **`armv8-a+crc`** |
| Debug and tools, 1,967 TUs | `armv8.2-a+fp16+dotprod` and `armv8-a` |

**The shipping configuration is the baseline one.** `CLAUDE.md` records ARMSX2 as
`armv8-a, +crc` and that is **exactly right for Release** — but the fork does
have an 8.2 line, and it is not the one that ships.

## 4. azahar emits `armv9-a+sme`, and it is SAFE — which refines the SVE finding

**This looked like the live SVE trap.** It is not, and the reason is the rule.

The four files are **libyuv's dedicated variants** — `row_sve.cc`,
`row_sme.cc`, `rotate_sme.cc`, `scale_sme.cc` — each compiled with **its own
per-file `-march`**, and **dispatched at run time**:

```cpp
// externals/libyuv/source/convert.cc:685
if (TestCpuFlag(kCpuHasSME)) { ... }
```

**So the SVE and SME code compiles into the binary and is never entered on a
device without the feature.** libyuv even gets those paths on devices that *do*
have them.

> **The rule this gives: a library with PER-FILE flags and RUNTIME dispatch is
> safe to give aggressive `-march`. A library that dispatches at COMPILE TIME is
> not.**

| Library | Dispatch | On a device without SVE |
| --- | --- | --- |
| **libyuv** | per-file `-march` + `TestCpuFlag` | compiles it, **never calls it** |
| **xxHash** | whole-library `#if defined(__ARM_FEATURE_SVE)` | **selects SVE instead of NEON, no fallback** |

**That sharpens the earlier finding.** The danger is not `-march=armv9-a` in
itself — **it is a GLOBAL `-march=armv9-a` reaching a library that dispatches at
compile time.** **eden's `YUZU_BUILD_PRESET=armv9` sets a global `-march`**, which
is exactly the dangerous form.

## 5. Two claims from build files, confirmed from output

- **Cemu really does pass `-moutline-atomics`**, seen in all 508 TUs.
- **melonDS really does build three ABIs**, and its `armeabi-v7a` build carries
  `-march=armv7-a`. The ABI-waste finding stands.

## Limits

- **Only four forks have been built here.** xenia, Vita3K, eden, GameThor and
  rpcsx have no build output on this machine, and **absence above means "not
  built here", never "no flags".**
- **Cemu's and melonDS's databases are `Debug`**, so their release flags are
  unknown. **azahar's and ARMSX2's cover a shipping configuration.**
- **A `compile_commands.json` describes the last build**, not today's source.
- **Compile-time `-flto` still needs the link step to cooperate**, and the link
  command is not in this database.

## Sources

- `tools/emitted_flags.py`, run against four forks' `app/.cxx/*/*/arm64-v8a/`
- azahar `externals/libyuv/source/convert.cc`, `include/libyuv/cpu_id.h`
- Vita3K `docs/research/20260821-arm64-review-of-the-vita3k-tree.md`
