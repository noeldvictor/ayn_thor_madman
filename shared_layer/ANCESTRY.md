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

### Measured drift: the code is gone, the design survived

**Diffed on 2026-08-22, and it corrects the framing above.**

| File | azahar | eden | differing lines |
| --- | --- | --- | --- |
| `AbstractSetting.kt` | 13 | 31 | 34 |
| `SwitchSetting.kt` | 43 | 34 | 53 |
| `SliderSetting.kt` | 78 | 42 | 98 |
| `HeaderSetting.kt` | 9 | 13 | 14 |
| `SettingsViewModel.kt` | **11** | **143** | 142 |

**Every differing-line count exceeds the file it came from.** Whitespace was
stripped before comparing, so this is not formatting. **Essentially no literal
line survives in common.**

`SettingsViewModel.kt` is the clearest case: 11 lines against 143. They share a
name and nothing else.

### So this is a design duplication, not a code duplication

**Extraction here is a rewrite guided by two references, not a merge.** That is
less work saved than "90 shared files" implied, and the earlier framing in this
document was too strong.

What survives is still worth a great deal:

- **The type hierarchy is proven twice.** An abstract setting, typed
  subclasses, a view holder per type. Two teams kept that shape through years
  of independent divergence, which is stronger evidence than either fork's
  version alone.
- **The naming is agreed**, so the contract needs no negotiation.
- **Two reference implementations** show which parts each team found worth
  changing, and where they diverged is where the design was under-specified.

This is exactly what the document predicted: everyone copies once, then
diverges forever. **Here the divergence is complete.** The prediction was right
and the consequence is larger than expected: after enough years, shared
ancestry stops meaning shared code and starts meaning only shared design.

### Why this is the largest finding in the survey

Every earlier candidate was one subsystem. This is a frontend.

| Candidate | Scale |
| --- | --- |
| LRU cache | not duplication at all |
| GPU driver manager | four concerns, one subsystem |
| Touch overlay | 2,369 lines, one subsystem |
| **azahar and eden frontend** | **90 files by name, but the code has fully diverged** |

It was invisible to every feature-based search and took one filename
comparison to find. **That is the method working.**

## The fourth axis: vendored dependencies

Applying the same method to vendored third-party trees, 2026-08-22.

**Corrected count.** The first pass anchored its pattern to the start of the
path and missed nested vendor trees, notably rpcsx's at
`app/src/main/cpp/rpcsx/3rdparty/`. That is the same class of error as claiming
no fork has a feature: **the search was wrong, not the fleet.**

| Library | Forks | Vendored by |
| --- | --- | --- |
| **`ffmpeg`** | **5** | Vita3K, eden, xenia, ARMSX2, rpcsx |
| **`cubeb`**, audio | **5** | azahar, Vita3K, Cemu, ARMSX2, rpcsx |
| `vulkan-headers` | 4 | azahar, Cemu, xenia, rpcsx |
| `imgui` | 4 | Vita3K, Cemu, xenia, ARMSX2 |
| `glslang` | 4 | azahar, Vita3K, xenia, rpcsx |
| `xbyak`, `stb`, `glad`, `fmt` | 4 each | |
| `discord-rpc` | 4 | |
| `spirv-tools` | 3 | azahar, xenia, rpcsx |
| `libadrenotools` | 3 | azahar, Vita3K, Cemu |
| **`dynarmic`**, an ARM JIT | 2 | **azahar, Vita3K** |

**FFmpeg is vendored five times.** FFmpeg is enormous, and five copies in one
binary is not a size problem, it is an impossibility.

**`discord-rpc` is vendored four times** and is Discord Rich Presence. On a
handheld emulator it is pure weight and probably should not ship at all.

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

## The newest edge, and it is happening now: ARMSX2 to ARMSX3

Found 2026-08-22 while checking an unrelated negative. **It is the most recent
copy event in the fleet and nothing recorded it.**

`ps3-thor/rpcs3-upstream/android/armsx3-ui/app/src/main/java/` contains the
package **`com.armsx2`**. ARMSX3's Android frontend is **ARMSX2's Android
frontend, vendored whole**, package name included.

Roughly 35 top-level Kotlin files carry across — `BackupManager`, `OverlayRepo`,
`ShaderRepo`, `TextureCatalog`, `TexturePackInstaller`, `SecondScreen`,
`DiscordPresence`, `PlayTime`, `BatteryWatcher`, `DeviceTier`, `GpuInfo`,
`CustomDriver` — plus the whole `config`, `data`, `i18n`, `input`, `navigation`,
`runtime` and `ui` trees. The PS3-specific addition is visible as
**`Ps3PatchRepo.kt`** sitting beside them.

### Why this one matters more than the 2008 Dolphin edge

Every other edge in this document is archaeology. **This one is current.**

- **melonDS carries Dolphin code from 2008.** That is a copy made before agentic
  coding existed, and the argument was that copying is an old habit.
- **ARMSX3 copied ARMSX2's frontend in 2026.** Same fleet, same year, same
  author. **The habit is not historical.**

That is the thesis stated in `CLAUDE.md` — *agentic coding accelerates
duplication* — observed directly rather than inferred. It also means the fleet
now has **two copies of the settings hub, the overlay repository, the shader
repository, the texture catalogue and the second-screen panel**, and they will
diverge on the same schedule everything else did.

### It does not change the licence position

ARMSX3 is GPL-2.0-only and stays out of the app. **The frontend being ARMSX2's
work does not rescue it**, because the licence problem was always the core, not
the frontend: 84 frontend files against 1510 native ones, of which 874 come from
rpcs3.

**If anything it sharpens the earlier measurement.** Rebuilding the frontend
discards 5% of the fork, and it now turns out that 5% was largely ours to begin
with.

## A second Dolphin import in melonDS, and it is measurable

Found 2026-08-22 while reading the recompilers. **This document records melonDS
carrying Dolphin code from 2008 and 2009. There is a third import, from 2015,
and it is in the JIT rather than the frontend.**

`melonDS-android-lib/src/dolphin/Arm64Emitter.cpp`:

```
// Copyright 2015 Dolphin Emulator Project
// Licensed under GPLv2+
```

**Dolphin is on disk**, so for once the drift is measurable rather than
inferred.

| | melonDS's copy | Dolphin today |
| --- | --- | --- |
| `Arm64Emitter.cpp` | 4,496 | 4,474 |
| `Arm64Emitter.h` | 1,157 | **1,495** |
| emitter methods | 308 | **344** |

**Dolphin has added 40 methods that melonDS never received:**

```
ABI_CallFunction  ABI_CallLambdaFunction  BFXIL  BIF  BIT  CMEQ  CMGE  CMGT
CMHI  CMHS  CMLE  CMLT  CMTST  CNEG  EXT  EmitExtract  EmitScalar2RegMisc
EmitScalarPairwise  EmitScalarThreeSame  FACGE  FACGT  FADDP  FMAXNMP  FMAXP
FMINNMP  FMINP  FRINTI  MOVI2RImpl  NEGS  NOP  ORR_BIC  ParallelMoves
PoisonMemory  SEV  SEVL  SHL  SSHR  URSHR  WFE  WFI  YIELD
```

Three groups stand out:

- **Eight vector compares plus `BIF`, `BIT` and `EXT`.** A DS geometry engine is
  exactly the workload that wants vector compare and bit select.
- **`YIELD`, `WFE`, `WFI`, `SEV`, `SEVL`.** **This lands on a finding already in
  `CLAUDE.md`**: `yield` is a no-op on ARM, rpcs3 found half of all CPU time in
  a four-line `busy_wait`, and the fleet needs auditing for it. Dolphin can emit
  the whole family; melonDS's copy can emit none of it.
- **`ABI_CallFunction`, `ABI_CallLambdaFunction`, `ParallelMoves`.**
  `ParallelMoves` is the standard way to shuffle register assignments without a
  scratch spill, which matters given the X3 guidance to spill to the vector file
  rather than to memory.

melonDS added five of its own: `LDRGeneric`, `STRGeneric`, `QuickTailCall`,
`SBFX`, `SetCodeBase`.

**The licence permits taking the code, not only the idea.** Dolphin's file is
GPLv2 **or later**; melonDS-android is GPL-3.0.

**A hypothesis that was wrong, recorded so it is not re-checked.** I expected
Dolphin to have added `SDOT`, `UDOT`, `EOR3` or `BCAX`, since melonDS emits
none. **Dolphin has none of them either.** Nobody in that lineage ever added
them, so melonDS is not behind on the device's vector features — the whole
lineage is.

**This is the document's own thesis with a number attached**: copy once, diverge
forever, never receive the fixes. Twelve years, one file, forty methods, and the
licence never stood in the way.

## What is still unknown

- ~~Whether the ancestors have fixes worth back-porting.~~ **Answered below.**
- **Whether the ancestors have fixes worth back-porting.** Dolphin's overlay
  has twelve years of improvement that Vita3K never received. That is free work
  sitting upstream of a fork nobody thinks of as having an upstream.
- **ARMSX2 and rpcsx use SPDX headers**, so the search above missed them. A
  second pass keyed on `SPDX-FileCopyrightText` would find more.
- Cemu and xenia showed nothing, which may mean independent, or may mean they
  attribute differently.

## Overlay drift, measured: the API survived where the code did not

Kotlin against Java makes a line diff meaningless, so the comparison is
structural: which method names both still carry, 2026-08-22.

| | azahar `InputOverlay.kt` | Vita3K `InputOverlay.java` |
| --- | --- | --- |
| Methods | 19 | 37 |
| Lines | 1302 | 1067 |

**Eight method names survive in both**, twelve years after the split:

```
draw            onTouch              onTouchWhileEditing   refreshControls
isInEditMode    setIsInEditMode      resetButtonPlacement  saveControlPosition
```

That is 42% of azahar's surface and 22% of Vita3K's.

### Those eight methods are the overlay contract

They were not designed as a contract. **They are what survived two independent
twelve-year divergences**, which is a better filter than any design meeting:

| Method | What it means the overlay must do |
| --- | --- |
| `draw` | render itself |
| `onTouch` | handle play-mode input |
| `onTouchWhileEditing` | handle a second, distinct edit-mode input path |
| `isInEditMode`, `setIsInEditMode` | have an edit mode at all |
| `refreshControls` | reload its layout without a restart |
| `resetButtonPlacement` | return to defaults |
| `saveControlPosition` | persist a moved control |

**The edit mode is the interesting survivor.** Two separate `onTouch` paths and
an explicit mode flag mean editing the overlay is not a settings screen; it is
a direct-manipulation mode inside the game view. Both teams kept that.

### This contradicts the settings result, usefully

| | Settings framework | Touch overlay |
| --- | --- | --- |
| Shared names | file names only | **method names too** |
| Literal lines in common | essentially none | not comparable, different languages |
| What survived | the type hierarchy | **the API surface** |

**Drift is not uniform.** The settings framework kept a shape; the overlay kept
an interface. So "how far has it drifted" has to be measured per subsystem, and
the answer changes what extraction means each time.

### The divergence: each fork solved a different half

Reading what each side added alone is more useful than the shared part.

**azahar added construction and layout, 11 methods:**

```
addOverlayControls  initializeOverlayButton  initializeOverlayDpad
initializeOverlayJoystick  defaultOverlay  defaultOverlayLandscape
defaultOverlayPortrait  getBitmap  resizeBitmap  hapticFeedback  swapScreen
```

**Vita3K added lifecycle, state and physical controllers, 30 methods:**

```
attachController  detachController  detachVirtualController  rebindController
setAllowVirtualController  updateVirtualControllerState
setAutoHideEnabled  startHideTimer  stopHideTimer  resetHideTimer  tick  run
setOpacity  setScale  setLayout  setButton  setAxis  setDpadState
setState  setTouchState  applyResolvedState  releaseAllInputs
onAttachedToWindow  onDetachedFromWindow  onSizeChanged
getOverlayMask  refreshOverlayScope  addVitaOverlayControls
```

**Neither fork is sufficient alone.** azahar knows how to *build* an overlay:
per-orientation defaults, element construction, bitmap handling. Vita3K knows
how to *live* with one: auto-hide on a timer, physical controller attach and
detach, opacity and scale, input state, Android view lifecycle.

**A shared overlay needs both halves, and the eight shared methods are only the
seam between them.**

### Two things this corrects

- **azahar has `hapticFeedback`.** This repo recorded haptics as melonDS-only.
  **Two forks have it, not one.**
- **azahar has `swapScreen`.** The 3DS is dual-screen and azahar can swap the
  panels. That is directly relevant to the Thor's two displays and it was
  unrecorded.

### The finding that matters most for this device

Vita3K's `attachController`, `setAllowVirtualController` and
`updateVirtualControllerState` mean **the overlay hides itself when a physical
controller is present.**

**The Thor has physical controls.** A touch overlay drawn permanently over a
game on a device with real buttons is wrong, and only one fork in the fleet
solved it.

## Answered: what the ancestor has that the forks do not

Checked Dolphin's current Android overlay, 2026-08-22.

`Source/Android/app/src/main/java/org/dolphinemu/dolphinemu/overlay/`:

```
InputOverlay.kt   InputOverlayDrawableButton.kt   InputOverlayDrawableDpad.kt
InputOverlayDrawableJoystick.kt   InputOverlayPointer.kt
```

### The design is durable across three codebases and twelve years

**Dolphin still uses the same four class names it used in 2013.** So does
azahar, through Citra. So does Vita3K, which kept the 2013 copyright header.

**Three independent codebases, one design, twelve years, no coordination.**
That is far stronger evidence that the shape is right than either fork alone,
and stronger than the eight shared method names measured earlier.

### Two things the forks are behind on

1. **`InputOverlayPointer.kt` exists upstream and in neither fork.** A fifth
   component, for pointer input. The Wii remote's IR pointer is the obvious
   origin, but a stylus and a mouse are the same abstraction, and **the DS and
   3DS both use a stylus.** Worth reading before designing touch input.

2. **Dolphin migrated the whole overlay to Kotlin.** azahar is Kotlin,
   independently. **Vita3K is still Java**, so it is behind by a full language
   migration.

### This is the "free work upstream" claim, made concrete

The document argued that informal copying gives you the initial value and none
of the compounding. **Here is the compounding, itemised:** a fifth component
and a language migration, sitting in a repository neither fork lists as a
remote.

**Nobody has to guess whether the ancestors moved on. It takes one lookup.**

### All six ancestors checked. They split in two.

Checked 2026-08-22.

| Ancestor | Status | Consequence for the fork |
| --- | --- | --- |
| **Citra** | **dead.** Shut down March 2024, repos offline, collateral to the yuzu settlement | **azahar IS the upstream now** |
| **yuzu** | **dead.** Ceased 2024-03-04, $2.4M settlement, developers barred from Nintendo-infringing work | **eden IS the upstream now** |
| Dolphin | alive. Overlay migrated to Kotlin, gained `InputOverlayPointer` | Vita3K is behind |
| PCSX2 | alive. 2.6.3 in January 2026, and **2.6.0 landed a faster Vulkan path** | ARMSX2 can back-port |
| rpcs3 | alive. Alpha builds through January 2026, **targets ARM64**, and reports Cell CPU optimisation work | rpcsx can back-port |
| melonDS | alive. 0.11.3 in July 2026 | melonDS-android can back-port |

### Two forks have no upstream, and that changes their status

**Citra and yuzu were shut down by Nintendo in March 2024.** azahar and eden are
not forks lagging behind an upstream. **They are the continuation.**

So the 90 shared files between them are not two copies drifting from a living
parent. They are **two surviving descendants of a dead one**, which makes the
shared design more valuable rather than less: nobody upstream will ever
reconcile them, and no third party is going to.

**Extraction between azahar and eden is the only reconciliation that will ever
happen.**

### Four forks have live upstreams doing relevant work

Two are directly on this project's path:

- **PCSX2 2.6.0 landed a faster Vulkan path in January 2026.** ARMSX2's
  renderer is PCSX2's. That is exactly the kind of free work the ancestry
  argument predicts.
- **rpcs3 now targets ARM64 and reports Cell CPU optimisation work.** rpcsx
  runs rpcs3 on ARM64. Upstream is doing the port work the fork needs.

### What those two upstreams actually landed

Read 2026-08-22, because the rule above is worthless unheeded.

#### PCSX2 2.6.0: feedback reads

The headline change is **binding one texture as both a shader resource and a
render target**, called feedback reads. Reported gains are large and
title-specific: **596% in Hitman Blood Money, 413% in Death by Degrees**. It
also added Multidraw Framebuffer Copy, bringing D3D11 and D3D12 accuracy closer
to Vulkan and OpenGL.

**The headline numbers are for D3D12, which is irrelevant here. The technique
is not.** Reading a render target while writing it is a Vulkan feature too,
through attachment feedback loops, and the PS2 does this constantly, which is
why the gains are so large. **ARMSX2 renders through Vulkan on the Thor.**

**Read the PCSX2 change before touching the ARMSX2 texture path.**

#### rpcs3: ARM64 SPU work, and it transfers past PS3

Two separate things landed:

1. **An SPU recompiler improvement** giving roughly **5 to 7% on SPU-heavy
   titles**, benefiting every game because all PS3 games use SPUs.
2. **ARM64-specific Cell optimisations using `SDOT` and `UDOT`**, the ARMv8.2
   dot-product instructions, targeting Apple Silicon and Snapdragon
   ARM64 hardware.

**PS3 is deferred from this project, and this still matters.**

`SDOT` and `UDOT` are available on the Cortex-X3, A715 and A710. Lowering a
**guest vector unit** onto ARM64 dot-product instructions is not an SPU
technique; it is a technique for any guest vector unit. **The fleet has three
more**: the PS2 VU in ARMSX2, VMX128 in xenia, and DS geometry in melonDS.

**rpcsx is GPL-2.0-only and its code cannot be taken. The technique is not
code.** This is the clearest instance yet of the rule that ideas cross licence
boundaries freely: a PS3 emulator's ARM64 lowering strategy is readable,
citable and reusable by a PS2 and an Xbox 360 emulator that can never link
against it.

Note also that rpcs3 added ARM64 support in **late 2024**, so this is a settled
platform upstream rather than an experiment.

### CORRECTION: the fleet already did this, twice, and one fork refuted my
### generalisation

**Two forks have already done exactly this cross-pollination**, and neither was
recorded here:

| Fork | Document | Date |
| --- | --- | --- |
| xenia-thor | `docs/research/20260805-rpcs3-arm64-optimizations-applicable.md` | 2026-08-05 |
| Cemu-thor | `docs/research/20260820-rpcs3-arm64-optimizations-for-cemu.md`, **395 lines** | 2026-08-20 |

**Cemu's cites xenia's**, describing it as "same source material, mapped onto
Xenon rather than Espresso". So the fleet is already cross-pollinating, already
citing itself, and this repo did not know.

Cemu's document cites **twelve merged rpcs3 PRs by number**, plus Whatcookie's
write-up and the v0.0.42 release notes, and opens by stating that no code was
changed and nothing has been measured on the Thor.

#### Whatcookie's numbers are on this exact SoC

> Their test device was an AYN Odin 2 — Snapdragon 8 Gen 2, the same SoC as the
> Thor. Their numbers are on our silicon, same 1xX3 / 2xA715 / 2xA710 / 3xA510
> layout.

The headline claim, **theirs and unverified**, is roughly **60% faster at 25%
less power**. The document is explicit that this must not be restated as ours.

#### It refutes the generalisation made one commit ago

This repo claimed the `SDOT` and `UDOT` technique transfers to ARMSX2's VU,
xenia's VMX128 and melonDS's DS geometry. **Cemu's document draws the line
correctly and I did not:**

> Cemu's guest is **Espresso**, a 750CL derivative with **no VMX at all** — its
> only SIMD is paired-singles. So the large RPCS3 vector items
> (`VPERM`→`TBL`, `EOR3`/`BCAX`, SVE2, `UDOT`/`SDOT`, ...) have **no guest-side
> counterpart here**. Roughly half of the RPCS3 list dies at this line.

**A guest-side technique transfers only where the guest has a comparable
feature.** xenia's Xenon has VMX, so it transfers. Cemu's Espresso does not, so
it does not. My claim was true for some forks and false for others, stated as
though it were general.

**What does transfer is host-side**, and the document says so: spin and wait
behaviour, timer plumbing, compiler target features, and memcmp and checksum
shapes. Its own Tier 1 finding is host-side too — guest `mftb` performing a
128-bit software divide under a global spinlock.

**Rule: separate host-side from guest-side before claiming a technique
transfers.** Host-side crosses freely. Guest-side crosses only where the guest
ISAs align.

## The uncomfortable part

**This fleet's forks have upstreams they do not track.**

melonDS-android carries Dolphin 2008 code. Vita3K carries Dolphin 2013 code.
Neither lists Dolphin as an upstream remote, so neither will ever see a fix.

The provenance rule in `CLAUDE.md` says to record what was taken from where and
why. **These forks took a great deal and recorded it only in a copyright line.**
Doing better than that is most of what this project is.
