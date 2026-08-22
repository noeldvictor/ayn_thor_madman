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
  across every system, a per-game override for every option, and known-good
  settings gathered from the community.

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

## The duplication problem

Agentic coding accelerates duplication. A feature that used to be built once,
because it was expensive, now gets built in three forks in a week.

That already happened here. Three forks independently implemented per-class
texture filtering. Six forks each wrote a GPU driver picker.

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
| `hardware_ref/thor/` | Manuals for the Thor: SoC, CPU, GPU, Android, device. |
| `hardware_ref/console/` | Manuals for each emulated console. |
| `console_lab/` | Experiments and speedups for one console only. |

Compress any manual before you commit it. Prefer a link plus extracted notes
over the file itself.

Name a log file `YYYYMMDD_HHMM_<slug>.md`.

## The fleet

The forks stay in their own directories beside this repo. This repo tracks
them. It does not contain them.

Tier 1 targets: xenia-thor, rpcsx-ui-android-thor, Cemu-thor, azahar-thor,
watermelon-DS-THOR, Vita3K-Thor and ARMSX2.

Tier 2: GameThor and eden-thor.

The full map, with paths and upstream sources, is in
[`CLAUDE.md`](CLAUDE.md#the-fleet).

## Conventions

- All writing uses ASD-STE100 Simplified Technical English. Be direct. Be
  concise.
- Record research in `research_log/`. Record work in `work_log/`. Write the log
  as you go.
- Each fork keeps its own `AGENTS.md`. That file is the source of truth for
  that fork.
- Some forks ban AI attribution in commits. Read the `AGENTS.md` of a fork
  before you commit to it.

## Status

Early. The contract is written. The first survey is complete. See
[`research_log/`](research_log/).

The toolchain row is decided: NDK 29.0.14206865, `arm64-v8a` only,
`minSdk` 33, `targetSdk` 37, `compileSdk` 37. The fleet currently spans NDK 22
to NDK 29, so every fork needs the change.

The next work is the shared test harness. Four forks hold automated test and
replay tools. No fork holds more than two, and nothing is shared. Extract that
harness before any renderer feature.
