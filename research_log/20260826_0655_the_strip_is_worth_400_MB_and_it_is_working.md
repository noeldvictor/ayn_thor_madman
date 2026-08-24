# The strip is worth 400 MB on one library, and it is working in all four forks

**Goal: I wrote that nobody had checked the shipped `.so` files for symbol
tables, and that the instrument was in hand. Check.**

**Device-free, and MEASURED HERE rather than borrowed** — file sizes of build
outputs already on this disk. **No device used.**

## The method is a directory comparison, not a disassembly

**AGP keeps both stages of its own pipeline**: `merged_native_libs` before the
strip and `stripped_native_libs` after.

> **So "did the strip run" is answered by comparing two files, with no `readelf`,
> no toolchain, and nothing to install.** The instrument turned out to be simpler
> than the one I proposed.

## The result: it is working, and the magnitude is the finding

**All four EMULATOR libraries, not just the shared C++ runtime:**

| Fork | Library | merged | stripped | reduction |
| --- | --- | --- | --- | --- |
| **ARMSX2** | **`libemucore_4k.so`** | **464.0 MB** | **27.7 MB** | **-94.0%** |
| **azahar** | `libcitra-android.so` | **438.2 MB** | 37.6 MB | -91.4% |
| **Cemu** | `libCemuAndroid.so` | **260.3 MB** | 44.9 MB | -82.8% |
| **melonDS** | `libmelonDS-android-frontend.so` | 75.7 MB | 9.6 MB | -87.3% |
| — | `libc++_shared.so` (three forks) | ~8.8 MB | ~1.2 MB | ~-86% |

**And two rows that do NOT follow the pattern**, which is why a blanket "the strip
saves 90%" would be wrong:

| Library | merged | stripped | reduction |
| --- | --- | --- | --- |
| `liblibrashader_capi.so` (ARMSX2) | 13.4 MB | 9.6 MB | **-28.4%** |
| `liblibrashader.so` (melonDS) | 10.6 MB | 8.2 MB | **-22.3%** |
| `libGLESv2_angle.so` (ARMSX2) | 6.0 MB | 6.0 MB | **-0.0%** |

**The Rust libraries strip far less**, and **ANGLE arrives already stripped** —
a prebuilt, not a failure. **The reduction is a property of how a library was
built, not a constant.**

**All four forks built on this machine strip correctly.** XenDroid's failure mode
has not occurred here.

> **But look at what it would cost.** A missing `ndkVersion` pin in the packaging
> module would put a **438 MB** library into azahar's APK instead of 37.6 MB —
> **and the build would succeed with no error.**

## And the sum lands on the packed-binary decision

**`CLAUDE.md` packs every backend into one binary.** These four emulator
libraries, unstripped, total **1,238 MB**. Stripped, **120 MB**.

> **A packing decision made without the strip working would produce a
> 1.2 GB artefact from four backends alone**, and the model calls for more.

**State it as an upper bound, not a prediction.** A real packed binary shares
code, deduplicates the C++ runtime and links once — **these are four separate
libraries summed, which no packed binary would be.** What the sum establishes is
the SCALE of the exposure, not a forecast.

## It dwarfs the ABI number this repo already treats as significant

**rpcsx measured that adding `x86_64` put 26 MiB compressed and 65 MiB
uncompressed of unreachable code into a 96 MiB APK**, called it more than half
the payload, and this repo records it as the reason four forks now build
`arm64-v8a` only.

> **One unstripped library is 400 MB.** Same silent failure mode — the build
> succeeds, nothing says the artefact is wrong — **and an order of magnitude
> larger.**

**And the two compound.** A fork that neither restricted its ABIs nor pinned its
packaging NDK would ship unstripped libraries for three architectures.

## What this is and is not

**This is a measurement I took, on this machine, and almost nothing else today
was.** Two days of work have been reading other forks' numbers and recording
their provenance; **this one needs no `VERIFICATION_DEBT.md` row, because its
provenance is this session and the files are on this disk.**

**It is also a small measurement.** File sizes of intermediates, four forks, one
library each. **It establishes that the strip ran, not that the APKs are
otherwise correct.**

## Limits

- **Four forks, and now the emulator library in each** plus three smaller ones.
  **Not every `.so` in every build was compared.**
- **The 1,238 MB sum is four separate libraries added together.** No packed
  binary would look like that; it bounds the exposure rather than predicting a
  size.
- **These are intermediates from builds made earlier in this project**, not
  freshly built, and **not extracted from an installed APK.**
- **It does not verify XenDroid's causal claim** that an absent NDK skips the
  strip. It verifies the strip happened here, which is the useful half.

## Sources

- `merged_native_libs` and `stripped_native_libs` under each fork's build tree
- `research_log/20260826_0610_the_strip_task_uses_the_modules_ndk.md`
