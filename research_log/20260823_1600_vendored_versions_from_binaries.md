# Vendored dependency versions, read from the binaries

**Goal: answer [`CLAUDE.md`](../CLAUDE.md) Open Decision 2 — which version of
each shared dependency does the fleet use — for the forks that vendor by copying
and have no submodule pin to read.**

No device. Reading version strings out of the same six `arm64-v8a` libraries
used in the collision census.

## Result

**Every plain-C dependency that two forks both embed a version string for
disagrees.** Four libraries, four mismatches, zero agreements.

**And the C++ ones do not matter**, because they version their own ABI. See the
next section — that is the finding, not the table below.

| Library | Fork | Fork | Gap |
| --- | --- | --- | --- |
| **OpenSSL** | Cemu **3.5.0** | Vita3K **3.6.2** | minor |
| **SDL** | ARMSX2 **3.5.0** | Vita3K **3.2.28** | **minor, and its symbols already collide** |
| **imgui** | ARMSX2 **1.92.6** | Vita3K **1.91.3** | minor |
| **zlib** | Cemu **1.3.1** | Vita3K **1.3.2** | patch |

Also read, single-fork: ARMSX2 **libpng 1.8.0**.

## The C++ libraries protect themselves. The C libraries cannot.

**Recovered from the mangled names in the collision census, at no extra cost:**

| Library | armsx2 | azahar | cemu | melonds | vita3k | xenia |
| --- | --- | --- | --- | --- | --- | --- |
| **fmt** ABI namespace | — | **v12** | **v12** | — | **v12** | **v6** |
| **libc++** inline namespace | `__ndk1` | `__ndk1` | `__ndk1` | `__ndk1` | `__ndk1` | `__ndk1` |

**xenia is six major versions behind on fmt** — and **that is safe**, which is
the important part.

**fmt puts its ABI version in an inline namespace.** `fmt::v6::` and
`fmt::v12::` mangle differently, so they **cannot collide**, and both can live in
one binary. That is why fmt contributed only ~142 colliding symbols despite four
forks carrying it, against OpenSSL's ~6,400.

**libc++ agrees everywhere**, at `std::__ndk1`, because it comes from the NDK
rather than from a fork. **It is the one dependency the fleet already shares.**

### So the risk has three tiers, and only one of them matters

| Tier | Libraries | Why |
| --- | --- | --- |
| **Safe by construction** | fmt, and any library with an inline ABI namespace | different versions mangle differently and coexist |
| **Already unified** | libc++ | supplied by the toolchain, identical across all six |
| **Dangerous** | **OpenSSL, zlib, SDL, libpng** | **plain C: same symbol names, different behaviour, no namespace to separate them** |
| **Dangerous** | **imgui** | **C++, but no versioned namespace.** `ImGui::`, `ImDrawList` and `ImFontAtlas` mangle identically at 1.91.3 and 1.92.6 |
| **Dangerous** | **boost, glslang** | **C++, no version namespace either.** Symbols are plain `boost::archive`, `boost::algorithm`, `glslang::`, `TIntermNode` |

**This sharpens the unification list considerably.** A dependency is safe when
it versions its own ABI, not when it is C++. **fmt does; imgui does not**, and
imgui is the one C++ library in the fleet where a measured version mismatch is a
real collision.

**The test to apply is "does it version its own namespace", not "is it C or
C++".**

**It also explains the collision census's shape.** OpenSSL is the largest
collider not because more forks use it, but because it is C and cannot hide.

## Why the C-library mismatches are worse than "four copies"

`CLAUDE.md` already states the rule: *"four different versions of one library is
worse than four copies: the same symbol with different behaviour."*

**This is that case, measured.** It is not a hypothetical any more.

**SDL is the sharpest instance**, because it compounds a problem already found.
ARMSX2 and Vita3K both export `JNI_OnLoad` and the whole
`Java_org_libsdl_app_*` family — and they are **different SDL versions**. So the
packed binary would not merely have to pick one `JNI_OnLoad`; it would have to
pick one whose behaviour matches whichever fork's expectations survive.

**OpenSSL is the one with a second cost.** Three forks carry it and two of those
disagree on the minor version. A TLS stack is the one dependency where running
an old copy is a security question and not only a linking one.

## Method, and its limits

`grep -a` for embedded version strings in the built libraries.

**This is a sample, not a census.** Only libraries that embed a printable
version string are readable this way:

- **azahar and melonDS yielded almost nothing.** That is not evidence they
  vendor fewer libraries — their builds simply do not carry the strings, or
  carry them in a form this pass did not match.
- **fmt, boost, glslang and SPIRV-Tools were not recovered** for any fork. They
  encode versions in macros and mangled names rather than in strings.
- **eden is absent**, because it does not build here.

**So the true number of version mismatches is a lower bound of four.**

## What to change

`CLAUDE.md`'s Open Decision 2 currently carries a table of pinned commits read
from four forks' git trees, with the note that ARMSX2 and eden vendor by copying
and have nothing to read. **This adds the two forks that table could not reach,
by a different method**, and it changes the character of the answer:

- The submodule table showed **azahar and Vita3K pinning identical commits** for
  `glslang` and `xxHash` — a convergence, and cheap to unify.
- **This pass found the opposite everywhere it could look.** No two forks agree
  on any version it could read.

**Both are true and they are not in tension.** Shared ancestry produces
agreement; independent vendoring produces drift. **azahar and Vita3K agree
because they inherited the same pin. ARMSX2 and Vita3K disagree because neither
inherited anything.**

## boost and glslang: the version question is moot for both

**Checked by looking for a version namespace, which is cheaper than finding a
version number — and it is the question that actually matters.**

**Neither versions its ABI.** boost's symbols are `boost::archive`,
`boost::algorithm`, `boost::addressof` — no `vN` anywhere. glslang's are
`glslang::` and `TIntermNode`. **Two copies at different versions mangle
identically and collide.**

**So for these two, "which version does the fleet use" is the wrong question.**
Whatever the versions are, they must be unified, because the linker cannot keep
them apart. **That is a stronger conclusion than a version number would have
been**, and it is why this lead is closed rather than deferred.

**Who carries them:**

| | armsx2 | azahar | cemu | melonds | vita3k | xenia |
| --- | --- | --- | --- | --- | --- | --- |
| boost | — | **yes** | **yes** | — | **yes** | — |
| glslang | — | **1,446 syms** | **4,539 syms** | — | **0** | — |

**One anomaly worth flagging rather than explaining away.** `CLAUDE.md`'s
submodule table records **Vita3K pinning glslang at the same commit as azahar**,
and **Vita3K exports zero glslang symbols** out of 98,550. Either it links
glslang with hidden visibility, or this build configuration does not use it.
**Not resolved here** — but a pinned dependency that contributes no symbols is
worth knowing about before anyone counts it as a collision.

## eden is measurable after all, without building it

**"eden is unmeasured because it does not build" was too pessimistic.** Its CPM
cache, `.cache/cpm/`, is a **resolved dependency manifest from a real configure
run**, and its `.cxx/RelWithDebInfo/*/arm64-v8a/_deps/` tree lists what that
configuration actually produced. **28 packages, with versions, at no cost.**

| Library | eden | rest of fleet |
| --- | --- | --- |
| **OpenSSL** | **3.6.0** | Cemu **3.5.0**, Vita3K **3.6.2**, azahar (unread) |
| fmt | **12.1.0** | `v12` ABI in azahar, Cemu, Vita3K; **xenia `v6`** |
| zlib | **1.3.2** | Vita3K 1.3.2, **Cemu 1.3.1** |
| boost | 1.90.0 | azahar, Cemu, Vita3K carry it; versions unread |
| **Oboe** | **1.10.0** | melonDS and ARMSX2 carry Oboe; versions unread |
| VMA | 3.3.0 | azahar, Vita3K |
| Vulkan-Headers | 1.4.345 | azahar, Cemu, xenia, rpcsx |

**OpenSSL is now the most fragmented dependency in the fleet: three distinct
versions across three forks** — 3.5.0, 3.6.0, 3.6.2 — with a fourth fork
carrying it unread. It is also the largest collider. **It should be first on the
unification list.**

**fmt is confirmed safe.** eden's 12.1.0 is the same `v12` ABI three other forks
export, and xenia's `v6` cannot collide with it.

**And this fills a blank in the standard row.** `CLAUDE.md` pins Oboe as the
audio backend with "Version not yet chosen". **eden runs Oboe 1.10.0 on Android
arm64 today**, which is a real working pin rather than a guess.

### The Android build is narrower than the dependency list suggests

Read from the CMake guards, because a dependency declared at the root is not
necessarily in the Android build:

- **`ENABLE_CUBEB` is `OFF` on Android**, so eden uses **Oboe**. This confirms
  the audio survey from the other direction.
- **SDL2 is forced off on Android** (`YUZU_USE_EXTERNAL_SDL2` and
  `YUZU_USE_BUNDLED_SDL2` both `OFF` when `ANDROID`), and **no file under
  `src/android/` references SDL**. **eden does not add a third SDL**, and it does
  not add to the `JNI_OnLoad` problem.
- **Discord presence depends on `ENABLE_QT`**, which is off on Android. **No
  Discord in the Android build.**
- **`ENABLE_WEB_SERVICE=1` is passed on Android**, which is what pulls OpenSSL
  through `httplib` and `cpp-jwt`.

**One ambiguity, left as one.** `cubeb` appears in both the CPM cache and the
`_deps` tree despite `ENABLE_CUBEB OFF`, and `externals/CMakeLists.txt` says it
is added "regardless of cpm settings". **Fetched is not the same as linked**, and
this read cannot tell which. Not claimed either way.

## Next, without the device

- **Done.** fmt, boost and glslang are resolved above.
- **Do the same pass on eden once it builds.**
- **Decide OpenSSL first**, because it is the largest collider, carries a
  security dimension, and was not on the dependency list at all before today.
