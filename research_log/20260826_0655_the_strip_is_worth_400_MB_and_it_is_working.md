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

| Fork | Library | merged | stripped | reduction |
| --- | --- | --- | --- | --- |
| **azahar** | **`libcitra-android.so`** | **438.2 MB** | **37.6 MB** | **-91.4%** |
| ARMSX2 | `libc++_shared.so` | 8.8 MB | 1.2 MB | -86.4% |
| Cemu | `libc++_shared.so` | 8.9 MB | 1.3 MB | -85.2% |
| melonDS | `libc++_shared.so` | 8.8 MB | 1.2 MB | -86.4% |

**All four forks built on this machine strip correctly.** XenDroid's failure mode
has not occurred here.

> **But look at what it would cost.** A missing `ndkVersion` pin in the packaging
> module would put a **438 MB** library into azahar's APK instead of 37.6 MB —
> **and the build would succeed with no error.**

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

- **Four forks, one library each**, chosen as the largest `.so` over 2 MB in each
  tree. **Other libraries in the same builds were not compared.**
- **`libc++_shared.so` is a prebuilt NDK library**, so three of the four rows
  measure the same file. **azahar's row is the only one measuring an emulator.**
- **These are intermediates from builds made earlier in this project**, not
  freshly built, and **not extracted from an installed APK.**
- **It does not verify XenDroid's causal claim** that an absent NDK skips the
  strip. It verifies the strip happened here, which is the useful half.

## Sources

- `merged_native_libs` and `stripped_native_libs` under each fork's build tree
- `research_log/20260826_0610_the_strip_task_uses_the_modules_ndk.md`
