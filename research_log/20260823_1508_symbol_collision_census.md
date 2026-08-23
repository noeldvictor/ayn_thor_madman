# The symbol collision census

**Goal: measure whether the packed binary can actually link, by reading the
symbols the forks already built rather than by scanning build files.**

No device. No fork modified. Six forks had `arm64-v8a` libraries on disk from
today's build work; this reads them with the NDK's `llvm-nm`.

## The result, in one line

**Every cross-fork symbol collision is a vendored dependency. The emulator cores
do not collide at all.**

**25,526 symbols are exported by two or more forks. Zero of them are emulator
code.** Searched for `Kernel`, `Core`, `Common`, `PPC`, `Latte`, `xenia`, `VU`,
`melon`, `Vita3K`, `Cemu`, `Service`, `Pcsx2` and `GS` namespaces across the
shared set. **No match.**

**This is the first hard evidence for the packed-binary plan**, and it supports
it. [`CLAUDE.md`](../CLAUDE.md) says "dependency unification comes before backend
packing". **That ordering is now measured, not argued.**

## Method

`llvm-nm -D --defined-only` on each fork's main native library, then set
intersection.

| Fork | Library read |
| --- | --- |
| ARMSX2 | `libemucore_4k.so` |
| xenia | `libxenia-app.so` |
| Cemu | `libCemuAndroid.so` |
| azahar | `libcitra-android.so` |
| melonDS | `libmelonDS-android-frontend.so` |
| Vita3K | `libVita3K.so` |

**eden could not be measured.** It has no main library on disk, which is the
same finding as the build attempt: eden does not build here.

## The collision surface is set by visibility, and the spread is 43x

| Fork | Exported | Colliding | Share |
| --- | --- | --- | --- |
| **xenia** | **2,285** | **2,092** | 92% |
| **ARMSX2** | **4,380** | 3,845 | 88% |
| melonDS | 12,255 | 6,055 | 49% |
| azahar | 62,507 | 16,170 | 26% |
| Cemu | 68,740 | 13,580 | 20% |
| **Vita3K** | **98,550** | **19,696** | 20% |

**Read the absolute column, not the percentage.** xenia collides on 2,092
symbols and Vita3K on 19,696 — an order of magnitude — and xenia's high
percentage only says that what little it exports is almost all boilerplate it
shares with everyone.

**This is the `-fvisibility=hidden` row of the standard toolchain row, measured.**
`CLAUDE.md` estimated the problem from header scans — "Cemu leaves 66% of its
headers at global scope, ARMSX2 59%, Vita3K 53%". **The binaries disagree with
that ranking**: ARMSX2 exports 4,380 symbols and Vita3K 98,550. A header at
global scope is not the same as a symbol with default visibility, and only the
second one collides.

## What actually collides

| Category | Symbols | Note |
| --- | --- | --- |
| **OpenSSL** | **~6,400+** | **the single largest collider, and absent from CLAUDE.md's vendored table** |
| libc++ (`std::__ndk1`) | ~5,850 | unavoidable; needs one runtime, not one copy each |
| glslang / SPIRV-Tools | ~1,100+ | already a known duplicate |
| SDL | ~1,160 | **including its JNI bridge — see below** |
| imgui, VMA, pugixml, boost, fmt | ~1,300 | known |
| Teakra | 528 | **not recorded anywhere** |
| xxHash / LZ4 / zstd, curl, zlib, rcheevos, glad, tsl | rest | |

**Only two symbols are exported by all six forks**, both libc++ type info for
`std::__ndk1::__function::__base<void(unsigned)>`.

### Three dependencies nobody had recorded

**OpenSSL, in azahar, Cemu and Vita3K.** It does not appear in `CLAUDE.md`'s
vendored-library table at all, and it is the **largest single source of
collisions in the fleet**. A TLS stack is also the one dependency where carrying
three different versions is a security question and not only a linking one.

**Teakra, in azahar and melonDS.** Both emulate a Teak DSP — the 3DS's and the
DSi's. **That is a genuine shared emulation dependency**, the only one found,
and it is exactly the kind of thing the capability inventory exists to catch.

**rcheevos, in ARMSX2 and melonDS.** RetroAchievements. Relevant because
achievements are a product-level feature that would want one implementation.

### The JNI bridge is a hard blocker, not a merge conflict

**60 colliding symbols are JNI entry points**, and they are SDL's:
`JNI_OnLoad`, and the whole `Java_org_libsdl_app_SDLActivity_*` and
`Java_org_libsdl_app_HIDDeviceManager_*` families, in ARMSX2 and Vita3K.

**`JNI_OnLoad` cannot exist twice in one binary.** It is not a symbol you can
rename away, because the Android runtime looks it up by that exact name. Two
forks statically linking SDL means two SDL Java bridges claiming the same
entry points.

**This is a specific, named blocker for backend packing** and it is cheaper to
find now than during a link.

## What this changes

1. **The architecture's central risk is smaller than feared, and differently
   shaped.** Seven emulator cores in one binary is not the problem. **Seven sets
   of vendored dependencies is.**
2. **Dependency unification is now first on evidence, not on argument.** It also
   gains three entries: OpenSSL, SDL, and a decision about Teakra.
3. **`-fvisibility=hidden` is worth more than a tidiness flag.** It is the
   difference between 2,092 and 19,696 colliding symbols per backend.
4. **The header-scan percentages in `CLAUDE.md` should be replaced by these
   numbers**, because they rank the forks in the wrong order.

## Limits, stated

- **This is a lower bound.** Exported dynamic symbols only. Static archives
  carry more, and the two forks with the smallest export sets are exactly the
  ones whose real dependency list this method under-reports. **A "-" in the
  matrix is not evidence a fork lacks that library** — `CLAUDE.md` records
  ARMSX2 vendoring ffmpeg, cubeb and imgui, and none of them shows here.
- **eden is unmeasured**, because it does not build.
- **Different builds, different flags.** azahar's library is `RelWithDebInfo`,
  ARMSX2's and Cemu's are `Debug`. That affects what is emitted and inlined.
- **No claim about whether the link would succeed.** Duplicate symbols across
  static libraries have resolution rules; this measures the surface, not the
  outcome.

## Next, and none of it needs the device

- **Decide the OpenSSL question**, which is new. One version, or does the app
  need TLS in a backend at all.
- **Price the SDL removal**, since it is a JNI blocker rather than a size one.
  ARMSX2 and Vita3K are the two that carry it.
- **Record Teakra in the capability inventory** as a shared emulation
  dependency between azahar and melonDS.
- **Re-run this over the static archives** for an upper bound, and over eden
  once it builds.
