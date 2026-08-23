# Five more negatives checked: storage, haptics, paths, render passes, vector features

**Goal: continue verifying the live absolute negatives `tools/supervise.py`
found in [`CLAUDE.md`](../CLAUDE.md).**

No device. Reading only.

**Result: five checked here and in the render-pass log — three wrong, two confirmed but with their evidence corrected.**

## WRONG: "Storage aggregation remains the one screen with no prior art anywhere"

**GameThor has 2,136 lines of it.**

| File | Lines |
| --- | --- |
| `utils/ContainerStorageManager.kt` | **994** |
| `ui/screen/settings/ContainerStorageManagerDialog.kt` | **911** |
| `utils/StorageUtils.kt` | 231 |

**This is the fourteenth absolute negative in this repo to be wrong**, and it was
missed because GameThor is Tier 2 and the earlier frontend census counted files
rather than reading them.

### What it already does

- **`loadEntries()`** — the aggregation itself, one `Entry` per game.
- **`getAvailableSpace`** via `StatFs`, **`getFolderSize`** by walking the tree,
  and **`formatBinarySize`** which uses **KiB/MiB/GiB** rather than decimal
  units — correct, and a detail most implementations get wrong.
- **`getStorageLocation`, `canMoveToExternal`, `canMoveToInternal`, `moveGame`**
  — moving a game between internal and external storage.
- **`removeContainer`, `uninstallGameAndContainer`** — the delete actions.

### It already has the category split, in miniature

```kotlin
data class Entry(
    val containerSizeBytes: Long,
    val gameInstallSizeBytes: Long? = null,
    ...
) {
    val combinedSizeBytes: Long? get() = ...
}
```

**Two categories, and they are exactly the distinction this project's design
turns on.** The Wine container is **rebuildable**; the game install is **not**.
`CLAUDE.md` states the rule as "a cache is an asset, not junk" and asks the UI
to state the cost before the action. **GameThor already separates the two halves
that rule needs.**

### And it has something the design does not

**Moving a game between internal and external storage.** `CLAUDE.md`'s storage
screen offers accounting and deletion. **It does not offer moving.**

**On a handheld with an SD card, moving is the action a person actually wants.**
Deleting a 12 GB game to free space is a loss; moving it is not. That is a
feature to add, found by checking a claim rather than by designing.

### What is genuinely still missing

**The multi-category breakdown.** GameThor has two categories because a Wine
game has two parts. This project needs nine — game data, saves and states, HD
packs, texture cache, shader cache, recompiled code cache, mods, cheats,
screenshots — **and a `rebuildable` flag per category**, which is the whole
point.

**So the honest claim is much narrower:** *no fork breaks storage down by
category with a rebuildable flag, and GameThor has the rest of the screen.*

## CONFIRMED, with a refinement: the content path resolver

> **Vita3K also has a content path resolver, and nobody else does.**

**Two searches, different words.** First for
`candidate.*path|search.*path.*root|resolve.*path|SearchPaths|CandidatePaths`
across seven forks; then for `/storage/<x>|storage/emulated/0|
getExternalFilesDirs|/sdcard/` and for candidate-list construction.

**The specific mechanism holds.** Vita3K's `util/cheat_paths.h` — enumerate
roots, build candidate paths for a title id, resolve one, report which won — has
no counterpart. Every other hit was unrelated: ARMSX2's `ConfigStore`, azahar's
cheat memory search, melonDS's settings backup, eden's kernel priority queue.

**But eden solves the adjacent half, and the claim should say so.**
`utils/PathUtil.kt` converts a Storage Access Framework `content://` URI to a
real filesystem path, **including removable SD volumes**, by mapping the
document id's volume prefix.

**These are complementary, not competing.** Vita3K answers *where is the
content*; eden answers *what real path did the person just pick*. **The shared
layer needs both**, and the resolver design in `CLAUDE.md` only cites one.

## WRONG, and inverted: "melonDS has `TouchVibrator`, haptics nobody else ships"

**Seven of eight forks ship host-side Android haptics. xenia is the only one
without.**

**Searched twice.** First for `vibrat|haptic|VibrationEffect|
performHapticFeedback|rumble` across all sources, which returns hits in seven
forks but mixes in **guest-side rumble emulation** — the trap `CLAUDE.md`
warns about. Second for host-only markers in **Kotlin and Java alone**:
`VibrationEffect|performHapticFeedback|getSystemService(...VIBRATOR|Vibrator`.

| Fork | Host-side haptics |
| --- | --- |
| ARMSX2 | `MainActivityRuntime.kt`, `NativeApp.java` |
| Cemu | `ContextExtensions.kt`, `InputDeviceExtensions.kt` |
| azahar | `overlay/InputOverlay.kt` |
| **melonDS** | `TouchVibrator.kt` + two delegates |
| Vita3K | `AppsListScreen.kt` |
| **eden** | `YuzuVibrator.kt`, `YuzuInputDevice.kt` |
| GameThor | Winlator `InputControlsView`, `WinHandler` |
| **xenia** | **none** |

### And they are two different features wearing one word

**Third time today**, after the LRU caches and frame generation.

**melonDS `TouchVibrator` is overlay touch feedback.**
`performTouchHapticFeedback()`, a user-settable strength read from
`SettingsRepository`, a fixed 100 ms duration, and — the good part — a
`supportsVibrationAmplitude()` check with a **duration fallback**: when the
device cannot vary amplitude, it shortens the buzz instead. Behind a
`VibratorDelegate` with `Api26VibratorDelegate` and `OldVibratorDelegate`.

**eden `YuzuVibrator` is guest rumble routed to a physical device.**
`vibrate(intensity: Float)`, and crucially **`getControllerVibrator(device:
InputDevice)` against `getSystemVibrator()`** — rumble goes to the pad that
caused it, or to the phone if there is no pad. `YuzuVibratorManager` wraps
`VibratorManager`.

**Neither substitutes for the other.** A DS stylus tap needs touch feedback; a
Switch game calling the rumble API needs device routing. **The contract needs
two entries, not one.**

**Take melonDS's delegate and amplitude fallback for the first, and eden's
per-device routing for the second.**

## HOLDS, but its evidence table is wrong: "xenia is the only fork using the device's vector features at all"

**The claim survives. The table under it does not, and it would mislead the next
reader.**

`CLAUDE.md` records:

| Fork | `SDOT`/`UDOT` | `EOR3`/`BCAX`/`RAX1`/`XAR` |
| --- | --- | --- |
| **xenia** | **2 files** | **6 files** |
| Cemu | 1 | 1 |
| ARMSX2 | 0 | 0 |
| melonDS | 0 | 0 |

**Cemu's "1 and 1" are comments.** Both hits are in `src/Common/cpu_features.h`:

```cpp
bool asimddp{ false };  // FEAT_DotProd - UDOT/SDOT
bool sha3{ false };     // FEAT_SHA3 - also provides EOR3/BCAX/RAX1/XAR
```

**Cemu detects the features and never emits the instructions.** A count that
puts it one row below xenia implies it uses them at a tenth of xenia's rate. It
uses them not at all.

**And eden was missing from the table**, with five files matching — **all guest
decode.** `simd_crypto_four_register.cpp` and `simd_sha512.cpp` in dynarmic
translate the **guest's** `EOR3` and `SHA512` instructions, and
`core/arm/nce/visitor_base.h` is a guest visitor.

**eden is the worst case for this trap in the whole fleet**, because **its guest
ISA is the host ISA.** Every ARM64 instruction name appears in eden as guest
decoding. **Counting instruction mnemonics tells you nothing there.**

**So there are three distinct things one grep conflated:**

| | Fork | Meaning |
| --- | --- | --- |
| **host emission** | **xenia only** — `a64_backend.cc`, `a64_sequences.cc` | actually uses the device's vector features |
| feature **detection** | Cemu | knows the CPU has them |
| **guest decode** | eden | must recognise them as guest instructions |

**Search the emitter directory, not the tree.** `src/xenia/cpu/backend/a64/`
answers this in one pass; the whole tree does not.

## CONFIRMED: transient attachments, and performance-as-a-test

**Two that survive two searches each.**

### "Vita3K tracks transient attachments and nothing else does" — holds

First search: `LAZILY_ALLOCATED|eLazilyAllocated|TRANSIENT_ATTACHMENT|
eTransientAttachment` across seven forks, excluding Vulkan headers. **One hit,
in Vita3K `renderer/src/vulkan/creation.cpp`.**

Second search, different words: `transient|memoryless|tile.?memory|
on.?chip.?only`. **Every other hit is an unrelated use of the word** — a cvar
name, a service, a libretro shim, a main function.

**This is the one genuinely tiler-correct thing in the fleet that only one fork
does**, and it pairs with the `DONT_CARE` work: an attachment that never leaves
tile memory needs no backing allocation.

### "Almost no emulator does performance-as-a-test" — holds

The claim is narrow: **record FPS and frametime per commit, and fail the build on
a regression.**

**A filename count is misleading here and nearly produced a wrong answer.**
Matching `bench|perf.*test|regression` returns 32 files in ARMSX2 and 39 in
azahar; **after excluding vendored trees, ARMSX2 has one and azahar has none.**

| Fork | What is actually there | Is it a per-commit gate |
| --- | --- | --- |
| ARMSX2 | `ee_sa_perf_console_conformance_tests.cpp` | **no** — a conformance test |
| xenia | `tools/edram_bench/` | **no** — a standalone benchmark |
| **Vita3K** | `run-thor-regression-suite.ps1`, `thor-render-regression-matrix.json` | **closest** — an on-device matrix, run by hand |
| azahar, melonDS, Cemu | nothing | — |

**Nobody fails a build on a performance regression.** Vita3K has the closest
thing and it is manual.

## Running tally

`tools/supervise.py` listed **15** live unqualified negatives in `CLAUDE.md`.
Three checked so far:

| Claim | Verdict |
| --- | --- |
| "only ARMSX2 has frame generation" | **WRONG** — xenia has one, by extrapolation |
| "storage aggregation has no prior art anywhere" | **WRONG** — GameThor has 2,136 lines |
| "Vita3K has a content path resolver, nobody else does" | **holds**, refine to name eden's adjacent half |
| "haptics nobody else ships" | **WRONG and inverted** — 7 of 8 have them; xenia is the outlier |
| "no fork plans render passes" | **WRONG** — xenia plans them and patches the begin retroactively |
| "nobody resolves MSAA on-chip" | **WRONG** — xenia does, with the store elided |
| "nobody uses input attachments" | **holds** — every hit is a zero-initialiser |
| "xenia is the only fork using the device's vector features" | **holds**, but its evidence table conflates emission, detection and guest decode |
| "Vita3K tracks transient attachments, nothing else does" | **holds** |
| "almost no emulator does performance-as-a-test" | **holds** — nobody gates a build on it |
| "melonDS is the only fork with a verified build recipe" | **STALE** — four forks now build on the standard row |

**Six of eleven wrong or stale.** The repo's table said every one had been
wrong; the rate is now **18 of 24** across its history — high, but no longer
total. **And of the five that held, three needed their evidence corrected**, so
the claim surviving is not the same as the reasoning surviving.

**All three misses share a cause, and none was a hard search.** Frame generation
was missed because the search was by filename and xenia's lives inside
`presenter.*`. Storage was missed because GameThor is Tier 2 and the census
counted its files without reading them. Haptics was missed because one fork's
class name was taken as the whole answer. **Every one was the wrong
instrument.**

**And two of the three turned out to be one word over two features** — frame
generation splits into interpolation and extrapolation, haptics into touch
feedback and rumble routing. **That is now the most common shape of a wrong
negative in this repo**, ahead of the feature simply existing elsewhere.

## The four still unchecked

- "nobody has priced Anime4K's two dedicated passes"
- and three more, all low-value

**Do not rely on any of them until checked.**
