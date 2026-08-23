# Cemu builds fastest, and that reveals what actually drives build time

**Goal: finish the cheap half of Phase 0.3 and read the numbers together.**

Session 2026-08-23 02:43.

**Result: Cemu is the fastest fork measured, and the determinant of build time
across the fleet is not emulator size or ABI count — it is how many dependencies
are compiled from source.**

---

## Cemu

| | |
| --- | --- |
| **Native clean build** | **2 min 42 s** |
| Object files produced | **498** |
| APK | 73.2 MB |
| ABIs | **`arm64-v8a` only** |
| Variant | debug |
| JDK / Gradle | 21 / 9.3.1 |
| Recipe | its `AGENTS.md` worked unchanged |

**Measured twice, because the first number was wrong.** A first run reported
4 min 27 s having compiled **nothing** — `gradle clean` leaves `.cxx/`, so ninja
found no work. Removing `app/.cxx` and rebuilding produced the figure above,
verified by 498 fresh objects and a rebuilt `libCemuAndroid.so`.

**Neither figure is a full from-nothing build.** The second reused `build/` for
the Java, resource and dex stages. **The honest reading is: native ≈ 2 min 42 s,
whole-project clean somewhere near 4–5 min.**

---

## What the four numbers say together

| Fork | Time | Own native | Dependencies compiled from source |
| --- | --- | --- | --- |
| **Cemu** | **~2 min 42 s** native | 291k lines | **none — vcpkg prebuilt** |
| ARMSX2 | 11 min 25 s | 396k lines | **librashader (Rust), shaderc** |
| azahar | 14 min 33 s | 279k lines | **2,206 targets**: cryptopp, glslang, spirv-tools, catch2 |
| melonDS | 15 min 27 s | 335k lines | **librashader (Rust)**, and **×3 ABIs** |

**Cemu has the second-largest native codebase and the fastest build.** ARMSX2
has the largest and is second fastest.

**Emulator size does not predict build time. Source-built dependencies do.**

- azahar builds **2,206 ninja targets**, most of them third-party.
- melonDS and ARMSX2 both compile **librashader's entire Rust dependency
  graph** — `glslang`, `spirv-cross2`, `gpu-allocator`, then eight
  `librashader-*` crates.
- **Cemu compiles none of its dependencies.** vcpkg supplies them prebuilt, so
  only Cemu's own 498 objects are built.

### This refines the ABI finding rather than contradicting it

**rpcsx measured that adding `x86_64` "doubled the native compile", and that is
still true.** But the native compile is **not the whole build**, and for three
of these four forks it is not even the dominant part.

**melonDS builds three ABIs and is the slowest — but its own C++ is not the
cost.** Its log is dominated by Rust. **Dropping to one ABI would cut the native
third of a build whose largest component is elsewhere**, which is exactly why
the earlier prediction refused to promise a two-thirds saving.

### And it points at a bigger lever

**Prebuilding dependencies is worth more than reducing ABIs**, on this evidence,
and Cemu already demonstrates it.

**That is a real input to the build-location decision.** The question was
whether builds are too slow for an agent to run unattended. **A fork whose
dependencies are prebuilt takes under three minutes.** The cost is not the
emulator; it is rebuilding `glslang` and `librashader` from source, repeatedly,
in four different forks.

**This is the dependency-unification argument arriving from a second
direction.** `CLAUDE.md` wants one `glslang` and one `fmt` because **the packed
binary cannot link five copies.** It turns out the fleet also **pays for them
four times at every clean build.**

---

## Phase 0.3 so far

| Fork | Result | ABIs | APK |
| --- | --- | --- | --- |
| **Cemu** | **~2 min 42 s** native | 1 | 73.2 MB |
| **ARMSX2** | **11 min 25 s** | 1 | 79.9 MB |
| **azahar** | **14 min 33 s** | 1 | 27.7 MB |
| **melonDS** | **15 min 27 s** | **3** | 55.5 MB |
| Vita3K | **fails** — recipe, then `x86_64` | 2 | — |
| eden | **fails** — `pkg-config`, `glslangValidator` | 1 | — |
| xenia, GameThor | not attempted | | |

**Four of six attempted forks build. Every failure was peripheral** — a recipe,
an ABI list, a missing host tool. **Nothing has failed inside an emulator.**
