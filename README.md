# ayn_thor_madman

The master repo for a unified emulator on the **AYN Thor**.

> **Personal experiment. Work in progress. Not a product.**
>
> This code is written by AI agents, under my direction. It is a private
> experiment on my own handheld. It is not supported.
>
> - No support. Do not open an issue.
> - No pull requests. Do not send one.
> - No release schedule. No promise that anything here works.
> - Do not contact me about this repo.
>
> Read it if it is useful to you. Copy from it if it helps. Do not expect a
> reply.

The Thor is a Snapdragon 8 Gen 2 handheld with an Adreno 740 GPU, running
Android 13. Every part of this project targets that one device.

## Foundation

**Vulkan, on the AYN Thor, on ARM64. High speed. AI-driven at every level.
Easy to use.**

- **One device, one graphics API.** Vulkan on Adreno 740, ARM64 on Snapdragon
  8 Gen 2. No second GPU backend. No second ABI. Code may assume this
  hardware.
- **Speed is the product.** The Thor is a handheld. Frame time and thermal
  headroom decide whether a game is playable. An abstraction that costs frames
  is rejected, which is why this is not built on libretro.
- **AI-first at every level.** Agents do the development, the QA and the
  experiments. Agents also do work inside the product: neural upscaling, and
  per-game tuning a person would otherwise do by hand. The point is to remove
  human toil.
- **Easy to use, for somebody with a full-time job.** Configuration is not the
  hobby. Sensible defaults, one place for every setting, and nothing that needs
  a file manager, a text editor or a wiki. Cheats are first class. One hotkey
  set works on every system.
- **Quality of life, everywhere.** Texture improvement with per-class routing,
  HD texture packs, mods and translations installed in-app, one cheat library
  across every system, a per-game override for every option, per-game patches
  for speed and for gameplay changes, and known-good settings gathered from
  the community.

That last group is the reason to build one app instead of running eight
separate emulators.

## Why not RetroArch

RetroArch shares the periphery. This project shares the hot path.

RetroArch is a thin frontend over unmodified cores. It cannot make a core
faster, because it must accept hundreds of cores as their authors wrote them.
Its shared layer is UI, input and post-process shaders at the edges.

This project supports a small number of backends on exactly one device. That
buys the right to reach inside a core and take a responsibility away from it.
The shared layer owns the Vulkan device, the texture upload path, the caches
and the scheduler. Backends are modified on purpose.

RetroArch is also the anti-pattern for usability here: cheats are not first
class, hotkeys are not common across cores, and it demands study before it
gives you a game.

**What a core hands over is now an inventory, not a slogan.** See
[`shared_layer/HOST_SIDE.md`](shared_layer/HOST_SIDE.md): the Vulkan device and
allocator, the driver blob cache, the texture upload path, the present path and
frame generation, the driver, the thread pool and cluster affinity, the spin and
park primitive, the memory budget, the JNI boundary, dependencies and symbol
visibility, audio, input and hotkeys, the touch overlay, haptics, system
applets, dual-screen routing, storage and path resolution, settings and
migrations, cheats and patches.

**The core keeps its guest side and nothing else** — the ISA decoder, the guest
GPU model, the guest memory map and the timing.

**Measured 2026-08-23, and it is the first hard evidence the model works:**
25,526 symbols are exported by two or more forks and **zero are emulator code**.
Every collision is a vendored dependency. **Seven cores in one binary is not the
risk; seven dependency sets are.**

## Storage and cache visibility

Look at a game and see where its space went. One view, every system: game
data, saves and states, HD texture packs, texture cache, shader and pipeline
cache, recompiled code cache, mods, cheats and screenshots.

A cache is an asset, not junk. Clearing a shader cache frees space and brings
the stutter back, so the app states the cost before the action. Saves and
states are never near a bulk action.

## Per-game patches

Per-game patches are a first-class feature. They do two jobs: make a specific
game faster, and change a game. Gameplay tweaks, unlocks, restored content and
translations all use the same engine as a speed patch.

The engine is shared. A patch is data that names a game, a version, an intent
and the bytes to change. A speed patch carries its measurement from the device.

Ghidra is used when a game needs reverse engineering. The finding is recorded.
The Ghidra project is not committed.

## Development

Custom skills live in `.claude/skills/` in this repo. A skill holds a
procedure that would otherwise be retyped.

- `capability-check` — which fork already has this? Run it before writing any
  feature. Agentic coding accelerates duplication, and this is the guard.
- `thor-measure` — how to measure on the device without producing a fake
  number. Power readings while charging are fiction; cross-run comparisons are
  confounded; a run with no temperature rise did not happen.
- `extract-subsystem` — prove the duplication is real first. Three plans have
  been reversed by opening the files.

Research goes in `research_log/`. Work goes in `work_log/`. Console-specific
experiments go in `console_lab/`. Write the log as you go.

## The render target: native quality, not the average of seven forks

The gap between an emulator and a native game on this device is **structural,
not CPU**.

PS2, Xbox 360 and Wii U had eDRAM or immediate-mode GPUs. Switching render
targets and reading back were cheap there. The Adreno 740 is a tile-based
deferred renderer, where each of those is a GMEM resolve out to system memory
and back. A faithful emulator inherits a rendering structure designed for the
opposite architecture.

Faithfulness is required at the pixel. It is not required at the pass boundary.

So the target is not the average of seven portable Vulkan layers. It is what a
renderer looks like if it will only ever run on one Adreno 740, under one
pinned driver, on one device. See
[`shared_layer/THOR_RENDER.md`](shared_layer/THOR_RENDER.md).

The reference point is a well-optimised **native** game captured on the Thor,
not another emulator. The difference between that capture and a backend capture
is the roadmap.

## The fleet is already sharing code, badly

Emulators have copied each other for eighteen years. From the copyright headers
in these forks:

| Fork | Foreign code it carries |
| --- | --- |
| Vita3K-Thor | Dolphin 2013, Dolphin 2016, Citra, yuzu |
| melonDS-android | **Dolphin 2008, 2009, and 2015** |
| eden-thor | yuzu, over 2,000 files |
| azahar-thor | Citra, hundreds of files |
| **ARMSX3** | **ARMSX2's entire Android UI, copied in 2026** |

**The alternative to unification was never "everyone writes their own".** It is
what actually happened: everyone copies once, then diverges forever, and nobody
receives the fixes.

Vita3K took Dolphin's Android touch overlay in 2013. Dolphin has improved it
for twelve years since; Vita3K has none of that. azahar took the same file
through Citra. The two copies in this fleet are now 1302 lines of Kotlin and
1067 lines of Java that began as one file.

The question was never whether emulators should share code. **They already do,
at scale, across a decade.** The only question is whether that sharing is
tracked and maintained, or copied and abandoned.

### It is measurable, and it is still happening

**melonDS's ARM64 code emitter is Dolphin's, from 2015.** Both are on disk here,
so the drift is not an argument:

| | melonDS's copy | Dolphin today |
| --- | --- | --- |
| emitter methods | 308 | **344** |

**Dolphin has added 40 methods that melonDS never received**, including eight
vector compares and the entire `YIELD`/`WFE`/`WFI`/`SEV`/`SEVL` spin-wait family.
Dolphin's file is GPLv2-or-later and melonDS is GPL-3.0, so **the licence never
stood in the way**. Nobody was carrying the fixes across.

**And this is not only history.** ARMSX3's Android frontend is ARMSX2's,
vendored whole, package name included — **copied in 2026, in this fleet, in the
same year.** Every other example above is archaeology. That one is current, and
it is the reason this repo exists.

See [`shared_layer/ANCESTRY.md`](shared_layer/ANCESTRY.md).

## The duplication problem

Agentic coding accelerates duplication. A feature that used to be built once,
because it was expensive, now gets built in three forks in a week.

That already happened here. Three forks independently implemented per-class
texture filtering. Six forks each wrote a GPU driver picker. **All eight persist
a Vulkan pipeline cache.**

**Reading the code shrinks most of these claims, and that matters.** Three "LRU
caches" turned out to be three different designs for three different
constraints; six "driver pickers" turned out to be four different concerns.
**So the rule here is that a duplication claim is a hypothesis until somebody
opens the files** — and of ten "no fork has this" claims checked, **nine were
wrong**.

The fix is structural, not a rule in a document. When the shared layer takes a
subsystem, the fork loses the ability to have one: the implementation is
deleted, the fork links the shared module, and the build fails if the
subsystem reappears.

## What this is

A fleet of emulator forks exists. Each fork solves the same problems
separately: upscaling, filters, HD texture packs, cheat databases, mod loading,
ARM64 tuning, per-game profiles and control overlays.

Each fork also holds work that would help the others. Nothing carries a good
idea across a fork boundary today.

This repo closes that gap. It holds the shared code, the shared assets, the
tooling and the reference material. The product is **one real app**, not a hub
that launches other emulators.

## Start here

Read [`CLAUDE.md`](CLAUDE.md). It is the operating contract. It holds the
writing rules, the logging rules, the fleet map, the shared layer design and
the open decisions.

Do not copy content out of `CLAUDE.md` into another document. Two copies of a
map disagree.

## Layout

| Path | Contents |
| --- | --- |
| `CLAUDE.md` | The operating contract. Read it first. |
| `research_log/` | One file for each research session. |
| `work_log/` | One file for each work session. |
| `capability_inventory.md` | Which fork has which capability. Read before you build. |
| **`hardware_ref/thor/THOR_TARGET.md`** | **The north star. One compile target, one thermal budget, one thread policy.** |
| `hardware_ref/thor/` | Manuals for the Thor: SoC, CPU, GPU, Android, device. |
| `hardware_ref/thor/gpu/VULKAN_TIPS.md` | Practical rules for Vulkan on the Adreno 740. |
| `hardware_ref/thor/cpu/CORTEX_X3_NOTES.md` | Codegen rules for the prime core, from the ARM guide. |
| `hardware_ref/thor/cpu/CORE_COMPARISON.md` | Where the four cores disagree. They give conflicting advice. |
| `.claude/skills/` | Local skills: `capability-check`, `thor-measure`, `extract-subsystem`, `supervise`. |
| `tools/fleet_lint.py` | The standard row, executable. Reports; never modifies a fork. |
| `tools/supervise.py` | Catches this repo's repeated unproductive cycles. |
| `hardware_ref/console/` | Manuals for each emulated console. |
| `console_lab/` | Experiments and speedups for one console only. |
| `shared_layer/UNIFICATION.md` | **What unification actually means here. Unify what is forced; harvest what is chosen.** |
| `shared_layer/PATTERNS.md` | The nine pipelines every emulator has. |
| **`shared_layer/HOST_SIDE.md`** | **Every host-side service a core hands over, and who owns it instead.** |
| `shared_layer/PROPAGATION.md` | Lessons one fork learned that the others have not received. |
| `shared_layer/THOR_RENDER.md` | The render architecture for this device. |
| `shared_layer/BACKEND_STANDARD.md` | What a backend must deliver to be part of one app. |
| `shared_layer/OWNED.md` | What the shared layer owns. Empty today, on purpose. |
| `DEVICE_QUEUE.md` | Everything waiting on the hardware, with a prediction for each run. |
| `app/SCREENS.md` | The 14 screens, and the backend contract they imply. |
| `app/GAME_DATA.md` | Game identity, metadata and cover art. Takes EmulationStation's vocabulary, rejects its data model. |
| `app/shell/` | The Compose shell, with fake data. Builds and runs. |

Compress any manual before you commit it. Prefer a link plus extracted notes
over the file itself.

Name a log file `YYYYMMDD_HHMM_<slug>.md`.

## The fleet

The forks stay in their own directories beside this repo. This repo tracks
them. It does not contain them.

Tier 1 targets: xenia-thor, Cemu-thor, azahar-thor, watermelon-DS-THOR,
Vita3K-Thor and ARMSX2.

Tier 2: GameThor and eden-thor.

**PS3 is deferred, not cancelled.** `rpcsx-ui-android` is **GPL-2.0-only** and
cannot share a binary with the GPL-3.0 forks. Dropping it makes every remaining
licence compatible, so the app is GPL-3.0 and the loading model is an
engineering decision again. **Keep harvesting ideas from it; do not link its
code.**

The full map, with paths and upstream sources, is in
[`CLAUDE.md`](CLAUDE.md#the-fleet).

## Conventions

**Working rules, set 2026-08-23. These override everything below them.**

- **All work stays in this repo.** Do not modify a fork unless asked for that
  fork by name. Read them, build them, measure them, and write what you learned
  **here**. Measurement is not modification.
- **Do not use the device.** Somebody else is testing on the Thor. Every device
  experiment goes to [`DEVICE_QUEUE.md`](DEVICE_QUEUE.md) with a prediction and
  waits. **Most of what this project needs is reading, measuring and building.**
- **Light mode is the default.** Offer a theme switch, but build for light
  first.
- **The UI must be cheap.** Nothing animates unless a finger is on it. No blur,
  no elevation shadows, minimal overdraw.

- All writing uses ASD-STE100 Simplified Technical English. Be direct. Be
  concise.
- Record research in `research_log/`. Record work in `work_log/`. Write the log
  as you go.
- Each fork keeps its own `AGENTS.md`. That file is the source of truth for
  that fork.
- Some forks ban AI attribution in commits. Read the `AGENTS.md` of a fork
  before you commit to it.
- **Run `python tools/supervise.py` before you report.** It catches the loops
  this repo repeats: unverified negative claims, work leaking into a fork, a
  queued experiment with no prediction, a session with no log.
- **A negative claim needs two searches, with different words.** Every claim of
  the form "no fork has this" that this repo has made has been wrong. The
  running rate is **15 of 17**.

## Status: deep exploration

**Working out the architecture and the contracts. Not building the product
yet.**

Decisions and findings are the deliverable in this phase. Code written now is
a probe, not a product. This repo holds more prose than code on purpose,
because the expensive mistakes here are architectural, and they are cheap to
fix in a document and expensive to fix in seven forks.

Every decision carries its evidence. See [`research_log/`](research_log/) and
[`capability_inventory.md`](capability_inventory.md).

### Settled so far

- The product is one real app, not a hub. Backends are packed into one binary
  so shared flows can be optimised across them.
- The app is GPL-3.0. PS3 is deferred, because every PS3 emulator is
  GPL-2.0-only and cannot share the binary.
- libretro is rejected. Slang shaders arrive through librashader.
- The toolchain row: NDK 29.0.14206865, `arm64-v8a` only, `minSdk` 33,
  `targetSdk` 37, `compileSdk` 37. The fleet currently spans NDK 22 to NDK 29.
- The shared layer is built by extraction from the forks, and duplication is
  prevented structurally rather than by a rule.
- The Thor has two internal touch displays. Dual-screen routing is a
  first-class feature.
- One compile target for every fork:
  `-march=armv8.2-a+crc+lse+fp16+dotprod+sha3+i8mm+bf16 -mtune=cortex-x3`.
  **Not `armv9-a`** — these cores are ARMv9 but the shipped SoC does not expose
  SVE, so a compiler told `armv9-a` may emit instructions that trap here.
- Audio is Oboe. Three forks had already chosen it independently, which settles
  the five vendored copies of cubeb that blocked the packed binary.
- **Frame pacing has no incumbent.** No fork uses Swappy or Vulkan display
  timing; every one picks a present mode and stops. It is the cheapest
  subsystem in the queue to own, because there is nothing to reconcile.

### Next

Two tracks run in parallel. Track A is the Kotlin UI shell, which defines the
backend contract and depends on nothing. Track B is the toolchain migration,
which unblocks all native work.

The phase ends when the contract is written, one backend runs behind the shell
on both displays, and one subsystem is extracted with its fork copy deleted.
