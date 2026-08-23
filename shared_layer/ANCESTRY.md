# The fleet is already sharing code, badly

**Emulators have been copying each other for eighteen years. The sharing
already happens. It just happens in the worst possible way.**

Evidence gathered 2026-08-22 from copyright headers in the forks themselves.

## The web

| Fork | Foreign code it carries |
| --- | --- |
| **Vita3K-Thor** | Dolphin 2013 x6, Dolphin 2016 x2, Citra x3, yuzu x2 |
| **melonDS-android** | **Dolphin 2008 x8, Dolphin 2009 x3** |
| **eden-thor** | yuzu, **over 2,000 files** |
| **azahar-thor** | Citra, hundreds of files |
| **ARMSX2** | PCSX2, wholesale, under SPDX headers |
| **rpcsx-ui-android** | rpcs3, wholesale |
| Cemu-thor | none found |
| xenia-thor | none found |

**melonDS-android is carrying Dolphin code written in 2008.** Vita3K carries
code from three different emulators. Neither fact appears anywhere in either
project's description.

Five ancestors account for most of the fleet: **Dolphin, Citra, yuzu, PCSX2 and
rpcs3.** Only Cemu and xenia are independent lineages.

## Why this matters more than the duplication itself

**The alternative to unification was never "everyone writes their own".** It is
what actually happened: **everyone copies once, then diverges forever, and
nobody receives the fixes.**

Vita3K took Dolphin's Android touch overlay in 2013. Dolphin has improved that
overlay for twelve years since. **Vita3K has none of it.** azahar took the same
code through Citra and diverged separately, so the two copies in this fleet are
now 1302 lines of Kotlin and 1067 lines of Java that started as one file.

That is the failure mode:

1. Project A solves a problem.
2. Projects B and C copy the solution.
3. All three improve it separately.
4. **Nobody can take anybody's improvement, because the code has drifted.**
5. A bug fixed in A stays broken in B and C forever.

**Informal copying gives you the initial value and none of the compounding.**

## What it proves about this project

The unification thesis is not speculative. **It is a formalisation of something
the ecosystem already does.**

The question was never "should emulators share code". They already do, at
scale, across a decade. The question is only whether the sharing is **tracked
and maintained** or **copied and abandoned**.

This repo's structural answer is the difference:

- `capability_inventory.md` tracks who has what, so a copy is visible.
- `shared_layer/OWNED.md` records what the shared layer owns, so a fix lands
  once.
- The build guard makes a fork unable to grow a private second copy.
- The provenance rule records where a thing came from and why.

**None of that exists in the informal version.** Vita3K's overlay does not know
it came from Dolphin except in a copyright line nobody reads.

## The pattern this predicts

**Shared ancestry is a better duplication predictor than shared purpose.**

Confirmed by every survey so far:

| Looked duplicated because | Result |
| --- | --- |
| Three forks have an LRU cache, same purpose | **three different designs** |
| Six forks have a driver picker, same purpose | **four different concerns** |
| Two forks have `InputOverlay*`, **same ancestor** | **one design, twice** |
| Two forks have `DiskShaderCacheProgress.kt`, **same ancestor** | same design |
| Two forks have `id_cache.cpp`, **same ancestor** | same design |

**Same purpose predicts nothing. Same ancestor predicts duplication reliably.**

### How to use it

**Search for shared ancestors, not shared features.** A copyright header, an
identical class name or a matching file name across two forks is stronger
evidence than any amount of "they both need to do X".

```sh
git -C <fork> grep -hoiE 'Copyright [0-9-]* (Dolphin|Citra|yuzu|PCSX2|RPCS3|melonDS) [A-Za-z]*' \
  | sort | uniq -c | sort -rn
```

Two forks with the same ancestor and the same file name are the same code,
diverged. Extract those first.

## Applying the method: 90 shared files between azahar and eden

The method says search for shared ancestors, not shared features. Applied to
Kotlin and Java basenames across the fleet, 2026-08-22.

**106 distinct filenames appear in two or more forks. Ninety of them are shared
between azahar and eden alone.**

Both descend from the Citra and yuzu Android frontends, which were built by the
same team. **This is an entire Android emulator frontend, duplicated.**

### Forty of the ninety are a typed settings framework

```
AbstractSetting          AbstractBooleanSetting   AbstractFloatSetting
AbstractIntSetting       AbstractShortSetting     AbstractStringSetting
BooleanSetting           FloatSetting             DateTimeSetting
HeaderSetting            SwitchSetting            SliderSetting
SubmenuSetting           StringInputSetting       StringSingleChoiceSetting
```

plus a matching `ViewHolder` for each.

**This is the settings schema the backend contract needs, already built,
twice.** `app/shell/.../Backend.kt` defines `SettingSpec` with a `SettingType`
enum and a stable key. That design is a rediscovery of what these two forks
already ship: an abstract setting, typed subclasses, and a view holder per
type.

**Do not design a settings framework. Take this one.**

### The rest of the ninety

Six shared view models: `DriverViewModel`, `EmulationViewModel`,
`GamesViewModel`, `HomeViewModel`, `SettingsViewModel`, `TaskViewModel`.

And a working frontend around them: `DirectoryInitialization`, `DocumentsTree`,
`DriverAdapter`, `DriverManagerFragment`, `DriversLoadingDialogFragment`,
`EmulationActivity`, `EmulationFragment`, `FileUtil`, `Game`, `GameAdapter`,
`GameHelper`, `GameIconUtils`, `DiskShaderCacheProgress`, `CompatUtils`,
`AboutFragment`.

**Licences permit it.** azahar is GPL-2.0-or-later, eden is GPL-3.0-or-later.
Either works in a GPL-3.0 shared layer, and **azahar's is the more permissive
source**, so prefer it.

### Why this is the largest finding in the survey

Every earlier candidate was one subsystem. This is a frontend.

| Candidate | Scale |
| --- | --- |
| LRU cache | not duplication at all |
| GPU driver manager | four concerns, one subsystem |
| Touch overlay | 2,369 lines, one subsystem |
| **azahar and eden frontend** | **90 files, including 40 of settings** |

It was invisible to every feature-based search and took one filename
comparison to find. **That is the method working.**

## The fourth axis: vendored dependencies

Applying the same method to vendored third-party trees, 2026-08-22.

**Vendored by four forks each:** `stb`, `imgui`, `glad`, `fmt`, `ffmpeg`,
`cubeb`.

**By three each:** `xxhash`, `xbyak`, `vulkan-headers`, `libadrenotools`,
`glslang`, `discord-rpc`.

| Library | Vendored by |
| --- | --- |
| `cubeb`, audio | azahar, Vita3K, Cemu, ARMSX2 |
| `imgui` | Vita3K, Cemu, xenia, ARMSX2 |
| `libadrenotools` | azahar, Vita3K, Cemu |
| `vulkan-headers` | azahar, Cemu, xenia |
| **`dynarmic`**, an ARM JIT | **azahar, Vita3K** |

`dynarmic` is the interesting pair: both the 3DS and the Vita have ARM guests,
so both recompile ARM to ARM64 with the same library. **That is a genuine
shared need, not an accident.**

### This is a hard constraint on the packed binary, not a tidiness issue

**You cannot link four copies of `cubeb` into one binary.** Duplicate symbols
do not merge politely, and different vendored versions of the same library are
worse than duplicates: they are the same symbol with different behaviour.

So the packed-binary decision has a prerequisite nobody recorded:

**Dependency unification comes before backend packing.** One `cubeb`, one
`imgui`, one `fmt`, one `glslang`, one `vulkan-headers`, at one version each,
before two backends share a link unit.

That belongs with [the toolchain row](../CLAUDE.md#the-standard-row), which
already unifies NDK, ABI and SDK levels but says nothing about vendored
libraries. **The toolchain row is incomplete.**

### Attribution style differs, which is why the earlier passes missed things

Three forks record ancestry by vendoring rather than by header:

- **Cemu** uses `dependencies/`: `libadrenotools`, `cubeb`, `imgui`, `ih264d`,
  `ZArchive`, `metal-cpp`, `Vulkan-Headers`, `stb`.
- **xenia** uses `third_party/`: **`FidelityFX-CAS`**, **`FidelityFX-FSR`**,
  `VulkanMemoryAllocator`, `SPIRV-Tools`, `DirectXShaderCompiler`, `FFmpeg`,
  `SDL2`.
- **rpcsx** returned nothing on either pass and is still unexplained.

**xenia vendoring FidelityFX-FSR is a third FSR integration.** ARMSX2 has
`GSUpscaler::FSR1` and rpcsx has `Emu/RSX/Program/Upscalers/FSR1`. Three forks,
three integrations of one AMD library.

## What is still unknown

- **How far each copy has drifted.** Vita3K's overlay and azahar's started as
  one file; nobody has diffed them.
- **Whether the ancestors have fixes worth back-porting.** Dolphin's overlay
  has twelve years of improvement that Vita3K never received. That is free work
  sitting upstream of a fork nobody thinks of as having an upstream.
- **ARMSX2 and rpcsx use SPDX headers**, so the search above missed them. A
  second pass keyed on `SPDX-FileCopyrightText` would find more.
- Cemu and xenia showed nothing, which may mean independent, or may mean they
  attribute differently.

## The uncomfortable part

**This fleet's forks have upstreams they do not track.**

melonDS-android carries Dolphin 2008 code. Vita3K carries Dolphin 2013 code.
Neither lists Dolphin as an upstream remote, so neither will ever see a fix.

The provenance rule in `CLAUDE.md` says to record what was taken from where and
why. **These forks took a great deal and recorded it only in a copyright line.**
Doing better than that is most of what this project is.
