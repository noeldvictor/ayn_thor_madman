# ayn_thor_madman — the master repo

## Logging rules — read first

**Record all research in a research log. Record all work in a work log. Write
the log as you go. Do not write it at the end from memory.**

| Activity | Directory | File name |
| --- | --- | --- |
| Research | `research_log/` | `YYYYMMDD_HHMM_<slug>.md` |
| Work | `work_log/` | `YYYYMMDD_HHMM_<slug>.md` |

Example: `research_log/20251215_0255_ididx.md`

Rules:

- Use the local time at the start of the session. Do not change the timestamp
  later.
- Write the slug in lower case. Use an underscore between words.
- Open one file for each session. Do not append to another day's file.
- Start the file with the goal in one sentence.
- Record the commands you ran and the result you got.
- Record the negative results. A method that failed is a finding.
- Name the fork and the commit for every measurement.
- Link the log file from any document that uses its result.

A research log holds investigation. It records what you read, what you tested
and what you learned.

A work log holds change. It records what you built, what you edited, what you
measured and what remains.

## Writing rules

**All writing in this project uses ASD-STE100 Simplified Technical English.
Be direct. Be concise. Do not use AI slop language. This applies to every
document, commit message, code comment, report and chat reply.**

### STE rules

- Write short sentences. Use 20 words maximum in a procedure. Use 25 words
  maximum in a description.
- Write one instruction in one sentence.
- Use the active voice. Name the agent that does the action.
- Start an instruction with the verb. Use the imperative.
- Use one word for one meaning. Do not use synonyms for a technical term.
- Use the same term every time. Do not change "fork" to "repo", "project" or
  "codebase" in the same document.
- Use articles. Write "the fork", not "fork".
- Use simple tenses. Prefer the present tense.
- Do not use an -ing form as a noun when a noun exists.
- Write six sentences maximum in a procedural paragraph.
- Use a vertical list for complex information.
- Put the warning before the step, not after it.
- Do not use metaphor, slang or humour.

### Banned language

Do not use these words and phrases:

`delve`, `leverage`, `seamless`, `robust`, `comprehensive`, `crucial`,
`pivotal`, `underscore`, `showcase`, `empower`, `streamline`, `elevate`,
`unlock`, `harness the power`, `realm`, `landscape`, `tapestry`, `journey`,
`embark`, `testament to`, `game-changer`, `cutting-edge`, `myriad`,
`plethora`, `meticulous`, `vibrant`, `boasts`, `dive into`, `at its core`,
`it is worth noting`, `not only X but also Y`, `in today's world`,
`navigate the complexities`.

Also do not:

- Open a reply with praise for the question.
- Restate the request before you answer it.
- Add a summary that repeats the text above it.
- Add hype adjectives to a technical fact.
- Hedge a measured result. State the number.

## Foundation

**Vulkan, on the AYN Thor, on ARM64. High speed. AI-driven at every level. Easy
to use.**

Read this before any design decision. If a change does not serve one of the
five points below, do not make it.

### 1. One device, one graphics API

Vulkan on Adreno 740. ARM64 on Snapdragon 8 Gen 2. Android 13.

We do not carry a second GPU backend, a second ABI or a second device. Every
line of code may assume this hardware. A portability layer that costs speed is
not welcome. See [Target hardware](#target-hardware).

### 2. Speed is the product

The Thor is a handheld. Frame time and thermal headroom decide whether a game
is playable. Speed is not a feature to add later; it is the reason for the
work.

Rules that follow from this:

- Reject an abstraction that costs frames. This is why libretro is rejected
  and why the backend contract is thin.
- Measure on the device. A number from a desktop proves nothing here.
- State the CPU cluster in every performance claim. See
  [Target hardware](#target-hardware).
- Prefer work that happens once over work that happens per frame. This is why
  texture-time upscaling beats present-time for classified art.

### 3. AI-first at every level

This project is built by agents, on purpose, at every stage.

- **Development.** Agents read the fleet, extract the shared layer and port a
  change across backends.
- **QA.** Agents run the tests, capture from the device and judge the result
  from numbers. See [Tests are mandatory](#tests-are-mandatory).
- **Experiments.** Agents propose, build, measure and keep or revert.
- **In the product.** Neural upscaling, and per-game tuning that a person
  would otherwise do by hand.

The point is to remove human toil, not to add novelty. If an AI feature makes
a person do more work, it is wrong.

### 4. Easy to use

The app must be pleasant for somebody who wants to play a game.

- Sensible defaults. A game runs well with no setup.
- One place for every setting. No hunting across backends.
- Nothing requires a file manager, a text editor or a wiki.
- The hard controls stay available for somebody who wants them. They are not
  in the way of somebody who does not.

### 5. Quality of life, everywhere

These are not extras. They are the reason to build a unified app rather than
run eight separate emulators.

- **Texture improvement.** Per-class routing, so sprites and 3D art get
  different treatment. See
  [Per-class routing](#2-per-class-routing--the-first-shared-feature).
- **HD texture packs.** Install one without a guide.
- **Mods and translations.** Install from inside the app.
- **Cheats.** One library across every system.
- **Per-game performance profiles.** Every option overridable per game. See
  [The game library and per-game overrides](#6-the-game-library-and-per-game-overrides).
- **Known-good settings from the community.** A game should arrive with
  settings that are already known to work, rather than defaults a person has
  to discover. Gather them, record where each came from, and let a person
  override any of them.

### What this rules out

State the conflict rather than quietly widening the scope.

- A second GPU backend, unless Vulkan cannot do the job.
- Desktop or non-Thor targets as a first-class concern.
- Any abstraction whose cost is paid per frame for the sake of neatness.
- A feature that adds a step for the person using the app.

## What this repo is

This repo is the control plane for a fleet of emulator forks. The forks target
the AYN Thor.

This repo holds the shared parts. An agent starts here to work on any fork.

The goal is one workspace, one MCP, one shared layer and one app.

**The product is one real app. It is not a hub over other emulators.** The app
does not install seven other apps and launch them. The emulators become
backends that the app fetches and drives, with one UI, one library and one
settings system. The user sees one product. This is a unified emulator.

See [The backend model](#the-backend-model) for how a backend loads and why
the contract between the app and a backend is deliberately thin.

The app is not built on libretro. See
[Upscaling and filters](#1-upscaling-and-filters).

To make this possible, the toolchain must be unified first. See
[One toolchain](#0-one-toolchain--do-this-first).

## The backend model

Decided 2026-08-22. This resolves how a backend loads.

**One app. Backends are separately distributed modules that the app fetches at
run time.** The app does not link a backend at build time and does not ship
one inside the APK.

This copies the distribution pattern RetroArch uses. It does not adopt
libretro. See
[How RetroArch handles a GPL-2 core](#how-retroarch-handles-a-gpl-2-core-under-a-gpl-3-frontend).

### This is still one app, not a hub

The difference matters and is easy to lose.

| Rejected hub model | This model |
| --- | --- |
| Installs other apps. | Fetches backend modules. |
| Each app has its own UI. | One UI. The app owns every screen. |
| Launches another app and leaves. | Drives the backend itself. |
| The user sees seven products. | The user sees one product. |

A user never sees a backend's own interface. There is no other interface. The
app owns the library, the settings, the overlay, the profiles and the routing.

### The contract is thin, because the cores differ

**Do not force one narrow API on every backend.** The cores are widely
different. A PS2 recompiler, a Wii U graphic pack system and a DS plane
compositor do not fit one shape. Forcing them into one is what makes libretro
slow and limiting.

Define a **minimum** contract that every backend must meet:

- Lifecycle: load, run, pause, stop, save state, load state.
- Surface: how the backend receives a Vulkan surface and reports its size.
- Input: how the app delivers a controller and touch state.
- Settings: the key namespace, and the resolution order for a per-game
  override.
- Paths: where saves, states, packs, cheats and screenshots live.

Everything past that is a **per-backend extension**. A backend declares what
extra it supports, and the app shows the UI for the ones present. Cemu
declares graphic packs. melonDS declares three filter planes. ARMSX2 declares
two texture classes. None of them pretends to be the others.

The shared layer sits above the contract, not inside the cores.

### What this buys, besides the licence hygiene

- **APK size.** Seven emulator cores in one package would be very large. The
  user installs only the systems they use.
- **Update rate.** Ship a backend fix without reshipping the app.
- **Build isolation.** A broken Cemu build does not block a melonDS release.
- **A licence escape hatch.** A GPL-2.0-only backend can be distributed
  separately if PS3 ever returns.

### PS3 is deferred, not cancelled

Punted 2026-08-22, until the backend contract takes shape.

The pattern above is what would let PS3 return. Do not design for it now, and
do not let it constrain the contract. Revisit once two backends work.

## Licences constrain the one-app plan

**Read this before you design how a backend loads.** The licences are not all
compatible. Checked 2026-08-22.

| Fork | Licence | Grant |
| --- | --- | --- |
| ARMSX2 | GPL-3.0 | `COPYING.GPLv3` |
| melonDS-android | GPL-3.0 | `LICENSE` |
| eden-thor | GPL-3.0 | `LICENSE.txt` |
| GameThor | GPL-3.0 | `LICENSE` |
| azahar-thor | GPL-2.0 **or later** | "Licensed under GPLv2 or any later version" |
| Vita3K-Thor | GPL-2.0 **or later** | "either version 2 of the License, or (at your option) any later version" |
| rpcsx-ui-android | **GPL-2.0-only**, verified | `LICENSE` is GPLv2. No or-later grant in any emulator source file. |
| Cemu-thor | MPL-2.0 | `LICENSE.txt` |
| xenia-thor | BSD | `LICENSE`, Ben Vanik |

What this means:

- **GPL-2.0-only and GPL-3.0 cannot be combined in one binary.** They are
  incompatible licences.
- azahar and Vita3K grant "or later", so they can be used as GPL-3.0. They are
  safe to combine with ARMSX2, melonDS and eden.
- **rpcsx is GPL-2.0-only. Verified 2026-08-22.** Of 1509 tracked C and C++
  files, 15 mention the GPL and all 15 are third-party crypto or build
  scripts. No emulator source file carries a licence header. The repository
  `LICENSE` is GPL Version 2 and the README grants no "or later".

  **rpcsx code cannot share one binary with ARMSX2, melonDS-android,
  eden-thor or GameThor.** Those are GPL-3.0.
- MPL-2.0 (Cemu) and BSD (xenia) combine with GPL. Both are compatible.

### Resolved by dropping PS3

With `rpcsx-ui-android` out of the app, **every remaining fork is compatible
under GPL-3.0**:

- ARMSX2, melonDS-android, eden-thor and GameThor are GPL-3.0 already.
- azahar-thor and Vita3K-Thor grant "or later", so both can be used as
  GPL-3.0.
- Cemu is MPL-2.0 and xenia is BSD. Both combine with GPL-3.0.

**The unified app is GPL-3.0.** One linked binary is legally clean. The
combined-work question is no longer a blocker, and the loading model is an
engineering decision again.

Re-open this section if PS3 returns. Nothing else reintroduces the conflict.

### Checked and clear

- **Cemu, MPL-2.0.** MPL-2.0 combines with GPL through its secondary licence
  clause, unless a file carries the Exhibit B "Incompatible With Secondary
  Licenses" notice. The Cemu source files sampled carry no licence header at
  all, and no Exhibit B notice was found outside the licence text itself.
  Cemu is therefore compatible. Re-check if you pull in a new dependency.
- **xenia, BSD.** Permissive. It combines with GPL.
- **librashader.** Dual licensed. The vendored tree carries both `LICENSE.md`
  (MPL-2.0) and `LICENSE-GPL.md`. Either path works for this project.

### Why PS3 was dropped rather than worked around

Private use triggers no licence obligation. Distribution does, and this repo is
public.

The options were: run backends as separate processes, distribute PS3
separately, split the binaries by licence, or drop PS3. Every option except the
last leaves an unanswered combined-work question and splits the app.

PS3 was dropped, decided 2026-08-22. The measurement made the choice easy:

| Part of rpcsx | Files |
| --- | --- |
| Kotlin and Java frontend | 84 |
| Native core | 1510 |
| Native core taken from rpcs3 | 874 |

Rebuilding the frontend discards 5% of the fork and keeps the encumbered 95%.
The core is the licence problem, and the core is the only part worth taking.

**Adapting a GPL work does not change its licence.** An adaptation is a
derivative work. Only a clean-room reimplementation from specifications, by
somebody who never read the source, escapes the licence. No fork here is that.
This rule applies to the whole fleet, not only to rpcsx.

Method note for any future licence scan: use `git grep`. A per-file `head` loop
times out on a repo this size.

### Every PS3 path is GPL-2.0-only

Do not re-open this. All three candidates derive from RPCS3, which is
GPL-2.0-only.

| Candidate | Base | Licence |
| --- | --- | --- |
| rpcsx-ui-android | RPCSX, with an rpcs3 subtree | GPL-2.0, no or-later grant |
| ARMSX3 | RPCS3, direct Android port | **GPL-2.0-only, stated** |
| aps3e | RPCS3 | GPL-2.0-only |

ARMSX3 states it in its README, checked on the fetched `armsx3/master` branch
in `ps3-thor/rpcs3-upstream`:

> GPL-2.0-only, the same as RPCS3. See LICENSE. Some files may be licensed
> differently, check the file headers.

ARMSX3 is not a way around the problem. It is the same problem, stated more
plainly. A relicence is not available either, because RPCS3 has many
copyright holders.

### No GPL-3.0 PS3 emulator exists

Checked 2026-08-22. Every PS3 emulator is GPL-2.

| Emulator | Licence | State |
| --- | --- | --- |
| RPCS3 | GPL-2.0-only | The only mature PS3 emulator. |
| rpcsx | GPL-2.0, no or-later | Contains an rpcs3 subtree. |
| ARMSX3 | GPL-2.0-only, stated | Direct Android port of RPCS3. |
| aps3e | GPL-2.0-only | rpcs3-derived. |
| Nucleus | GPL-2.0 | Independent, but **archived 2026-07-15**. Ran few games. |

Nucleus is the only PS3 emulator not derived from RPCS3. It is dead and it was
never close to usable. It is not a base.

There is no permissive or GPL-3.0 PS3 emulator to fork.

### How RetroArch handles a GPL-2 core under a GPL-3 frontend

Checked 2026-08-22. This is the working precedent for the PS3 problem.

RetroArch is GPL-3.0. Many of its cores are GPL-2.0-only. It uses three
separations:

1. **A stable C ABI.** A core is a shared library that implements the libretro
   API. The frontend calls `retro_run` and passes callbacks for video, audio
   and input. No core-internal type crosses the boundary.
2. **`dlopen` at run time.** The frontend does not link a core at build time.
3. **Separate distribution.** The frontend ships without cores. The user
   fetches a core through the core downloader. The GPL-2 core and the GPL-3
   frontend are never in one distributed package.

**Item 3 is the licence-hygiene step, and it is the one that matters.** Items
1 and 2 alone are the contested part; the Free Software Foundation treats
dynamic linking into one address space as one combined work. Separate
distribution avoids the argument rather than winning it.

RetroArch has run this model for over a decade at scale.

**We can copy the distribution pattern without adopting libretro.** libretro is
rejected for its core system and its UI, not for this. Shipping the app with
the PS3 backend fetched separately at run time is available to us, and it needs
no libretro code.

For PS3 specifically there are two known RetroArch routes: an alpha
`RPCS3-Libretro` core, and "launcher" cores that spawn the standalone RPCS3
binary and return afterwards. The launcher pattern is the cleanest legally,
because no emulation runs in the frontend process at all.

### Do not wait for a relicence

No RPCS3 relicensing effort was found. Do not plan around one.

A relicence needs consent from every copyright holder, or the removal and
rewrite of the code of anyone who refuses. RPCS3 dates from 2011 and has
hundreds of contributors. Projects of that size almost never relicence. The
Linux kernel is stuck on GPL-2 for the same reason.

Nobody is pushing for it because almost nobody hits this problem. A standalone
PS3 fork has no conflict. The conflict appears only when PS3 is combined with
GPL-3.0 emulators in one binary, which is what this project wanted to do.

**Do not block on it.** Ship the GPL-3.0 app with the other eight systems.
`ps3-thor/rpcsx-ui-android` already works as its own app; keep it that way. If
RPCS3 ever relicences, revisit. The cost of revisiting is low, because the
shared layer contract will already exist by then.

### Choose one side of the GPL fence

The fleet splits into two groups that cannot share a linked binary.

| GPL-3.0 side | GPL-2.0-only side |
| --- | --- |
| ARMSX2, PS2 | rpcsx or ARMSX3, PS3 |
| melonDS-android, DS | |
| eden-thor, Switch | |
| GameThor, PC | |
| azahar-thor, 3DS, elects GPL-3.0 | |
| Vita3K-Thor, Vita, elects GPL-3.0 | |
| Cemu-thor, MPL-2.0, compatible | |
| xenia-thor, BSD, compatible | |

Eight systems against one. **The app takes the GPL-3.0 side.**

ARMSX2 alone settles it. ARMSX2 is GPL-3.0 and it is the seed of the shared
layer. Choosing the GPL-2.0-only side to gain PS3 would cost PS2, DS, Switch
and PC, and would leave the shared layer without its reference
implementation.

### Why GPL-2.0-only and GPL-3.0 conflict

Both licences contain the same defensive rule. GPL-2 section 6 and GPL-3
section 10 each say you may not add restrictions beyond the licence itself.

GPL-3 adds conditions that GPL-2 does not have:

- Installation information for user products, the anti-tivoisation rule.
- An explicit patent grant and patent retaliation terms.
- Different termination and cure provisions.

From the GPL-2 side those extra conditions are further restrictions, which
GPL-2 forbids. From the GPL-3 side you cannot drop them. Neither licence can
give way, so one combined work cannot satisfy both.

"Or later" is what breaks the deadlock. A GPL-2.0-or-later file lets you elect
GPL-3.0 for your copy. Both halves then sit under one licence and agree. That
is why azahar and Vita3K are safe and rpcsx is not.

### Clean room is possible and wrong here

A clean-room reimplementation does escape the licence. Copyright covers
expression, not facts. Hardware behaviour, file formats and interfaces are
facts.

It requires two separated teams:

1. A team that reads the source and writes a functional specification. The
   specification must describe behaviour only. It must carry no code, no
   structure and no distinctive names or comments.
2. A team that has never seen the source and implements from that
   specification alone.

Dated records of who saw what are the defence. Without the paper trail there
is no clean room, only a claim.

**Do not attempt it for rpcsx.** Four reasons:

- **We are already the contaminated team.** We have read rpcs3 source in this
  project. A contaminated reader cannot be the clean implementer.
- **The agents make it worse.** This project is agent-driven. An agent that
  held the source in its context is contaminated, and proving what an agent
  saw is hard.
- **The scale is wrong.** 874 rpcs3-derived files, covering Cell SPU
  recompilation and RSX. That is a multi-year effort for a team.
- **It defeats the premise.** This project harvests and adapts. A clean room
  forbids exactly that.

Cheaper routes exist if PS3 returns: run the backend as a separate process, or
distribute it separately. Both cost far less than a clean room.

Note that `aps3e` is also rpcs3-derived. It carries the same licence. It is
not an escape.

### Other licence questions, not yet answered

- **Cheat databases.** Sharkive, CTRPF-AR-CHEAT-CODES, citra-games-wiki and
  the bundled `.ncl` files in rpcsx each have their own terms. Redistributing
  a cheat database is not automatically permitted. Check before shipping one.
- **Neural model weights.** ARMSX2 already declines to ship trained weights and
  states the reason is licensing. Any weights added later need the same check.
- **Shader presets.** Slang presets taken from the RetroArch ecosystem carry
  their own licences, often GPL. Anime4K itself is MIT.
- **GPL and app store distribution.** A GPL-3.0 app and a store's DRM terms can
  conflict. This matters only if you distribute through a store. It does not
  affect a personal build.
- **Firmware and BIOS.** Not code in these repos, but never commit one.

This note is not legal advice. Confirm before you distribute anything.

## The key idea

**Share assets and paradigms across the emulators. Solve a problem once.
Then apply the solution everywhere it fits.**

The forks solve the same problems separately. The list includes upscaling,
filters, HD texture packs, cheat databases, mod loading, ARM64 tuning,
per-game profiles and control overlays. These are not per-emulator problems.
They are Thor problems. Today the fleet solves each one a dozen times.

Solve each problem once, at the correct layer. Then adapt it into each fork.

**Each fork holds work that would help the others. The fleet has no way to
move it.** That is the gap this repo closes. One fork solves a problem well.
The other six never learn about it. Nothing today carries a good idea across a
fork boundary.

Keep a capability inventory. Record which fork has which capability, and at
what quality. Read it before you build anything. The first question for any
new feature is "which fork already has this?", not "how do I write this?".

The survey on 2026-08-22 proved the point. ARMSX2 and melonDS-android both
built per-class texture routing, separately, with different names for the same
idea. Their algorithm sets overlap by nine entries. Neither fork knows the
other exists.

**Harvest and adapt. Do not only merge.** Record what you took from each
upstream, and why. If a sibling fork does something better or faster, take it
and adapt it. There is no single upstream to follow. There is a field of
upstreams to mine. See [Provenance](#provenance-harvest-and-adapt).

## Target hardware

The fleet targets one device. The specification below comes from the fork docs.

| Part | Specification |
| --- | --- |
| SoC | Snapdragon 8 Gen 2 |
| Prime core | 1x Cortex-X3 |
| Performance cores | 2x Cortex-A715, 2x Cortex-A710 |
| Efficiency cores | 3x Cortex-A510 |
| GPU | Adreno 740 |
| OS | Android 13 |

The CPU is a 1+4+3 heterogeneous complex. The cores do not share a pipeline
model.

**State the cluster in every performance claim.** "Faster on the X3" and
"faster on the A510" are different results. Emulator threads run on different
clusters. For detail, read
`armsx2-thor/ARMSX2/docs/arm64-optimization-review.md`. The ARM manuals are in
that fork at `docs/reference/arm/`.

## Hardware reference

`hardware_ref/` holds the manuals and the hardware notes for the fleet. Put a
manual here one time. Every fork reads it from here.

| Directory | Contents |
| --- | --- |
| `hardware_ref/thor/soc/` | Snapdragon 8 Gen 2 documents |
| `hardware_ref/thor/cpu/` | Cortex-X3, A715, A710, A510 manuals. ARM64 ISA. |
| `hardware_ref/thor/gpu/` | Adreno 740, Vulkan, driver notes |
| `hardware_ref/thor/android/` | Android 13 platform notes, NDK notes |
| `hardware_ref/thor/device/` | Panel, thermals, controller, battery |
| `hardware_ref/console/ps2/` | PS2 hardware. Serves ARMSX2. |
| `hardware_ref/console/ps3/` | PS3 hardware. Serves rpcsx-ui-android-thor. |
| `hardware_ref/console/wiiu/` | Wii U hardware. Serves Cemu-thor. |
| `hardware_ref/console/3ds/` | 3DS hardware. Serves azahar-thor. |
| `hardware_ref/console/nds/` | DS hardware. Serves watermelon-DS-THOR. |
| `hardware_ref/console/vita/` | PS Vita hardware. Serves Vita3K-Thor. |
| `hardware_ref/console/xbox360/` | Xbox 360 hardware. Serves xenia-thor. |
| `hardware_ref/console/switch/` | Switch hardware. Serves eden-thor. |
| `hardware_ref/console/pc/` | PC and Proton notes. Serves GameThor. |

Rules:

- Record the source and the date for every document you add.
- Do not copy a manual into a fork. Link to `hardware_ref/`.
- Keep the CPU and the GPU notes separate. They answer different questions.

ARMSX2 holds ARM manuals at `armsx2-thor/ARMSX2/docs/reference/arm/`. Move
them to `hardware_ref/thor/cpu/` when you next work in that fork. Until then,
read them where they are.

**Never commit a large manual uncompressed.** A PDF manual can be hundreds of
megabytes. Git keeps every version forever, so a large binary is permanent
weight in every future clone.

Before you add a document:

1. **Compress it.** Store a PDF as `.pdf.zst` or `.pdf.xz`. Record the
   decompression command next to it.
2. **Prefer a link and a summary** over the file itself. A public ARM manual
   does not need a copy here. Record the URL, the document number, the
   revision and the date. Add the extracted facts as text.
3. **Extract the part you need.** A 900-page manual usually answers one
   question. Put the answer in Markdown. Markdown compresses, diffs and
   searches. A PDF does none of those.
4. **Never commit a file over 50 MB** without deciding on git-lfs first.

git-lfs is not set up. See [Open decisions](#open-decisions).

## Console lab

`console_lab/<console>/` holds experiments and speedups that belong to one
console only. The shared layer holds what propagates. The console lab holds
what does not.

Directories: `ps2`, `ps3`, `wiiu`, `3ds`, `nds`, `vita`, `xbox360`, `switch`,
`pc`.

Name a file `YYYYMMDD_HHMM_<slug>.md`, as in the logs.

Use it for work that is tied to one machine:

- A recompiler change for one CPU, such as the PS3 SPU or the Xbox 360 Xenon.
- A graphics quirk of one GPU, such as the Wii U or the PS Vita GXM.
- A cache or a memory map that only one console has.
- A game-specific fix.

**Default to the shared layer. Use the console lab only after you check.**
Filing a change here is a claim that no other fork can use it. That claim is
how the synergy was lost in the first place.

Before you file here, answer two questions in the file:

1. Which other forks did you check?
2. Why can they not use this?

If you cannot answer both, the work belongs in the shared layer.

Record anything reusable in
[`capability_inventory.md`](capability_inventory.md), even when the code stays
console-specific. The idea can travel when the code cannot.

## The fleet

The forks stay in their current directories. This repo tracks the forks. This
repo does not contain them.

### Tier 1 — active targets

| Fork | Path below `Documents/` | Harvest from |
| --- | --- | --- |
| xenia-thor | `xenia-thor-workspace/xenia-thor` | xenia-canary, xenia-edge |
| Cemu-thor | `cemu-thor-experiment`, branch `android-port` | cemu-project, SapphireRhodonite, SSimco |
| azahar-thor | `azahar-thor/azahar` | azahar-emu |
| watermelon-DS-THOR | `melonds_HD/melonDS-android` | WatermelonDS, melonDS-android-lib |
| Vita3K-Thor | `psvita/Vita3K-Thor` | Vita3K |
| ARMSX2 | `armsx2-thor/ARMSX2` | ARMSX2 upstream |

ARMSX2 is an active target. ARMSX2 is also the reference implementation of the
shared layer. See [ARMSX2 is the seed](#armsx2-is-the-seed).

### Deferred — PS3

`ps3-thor/rpcsx-ui-android` is **out of the unified app**, decided 2026-08-22.

Reason: rpcsx is GPL-2.0-only, and its core is the encumbered part. The
frontend is 84 Kotlin and Java files. The native core is 1510 files, of which
874 come from rpcs3. Rebuilding the frontend discards 5% of the work and keeps
100% of the licence problem.

**Dropping PS3 makes every remaining licence compatible.** See
[Licences](#licences-constrain-the-one-app-plan).

The fork stays on disk. Keep harvesting ideas from it. Do not link its code.

### Tier 2 — carried along

| Fork | Path below `Documents/` | Harvest from |
| --- | --- | --- |
| GameThor | `gamethor` | GameNative |
| eden-thor | `eden-thor` | eden-emu |

**eden-thor holds no custom work.** It is one commit ahead of upstream, and
that commit only adds fork notes. Reset it from upstream when you next touch
it. Nothing is lost.

eden upstream is **not on GitHub**. It is `https://git.eden-emu.dev/eden-emu/eden.git`.
The `upstream` push URL is set to `DISABLED` on purpose. Fetch before you judge
the drift; the local `upstream/master` ref may be stale.

`melonds_HD_2` is dropped from the fleet. `melonDS-android` replaced it. The
last commit to `melonds_HD_2` was 2026-07-12; `melonDS-android` was updated
2026-08-21. Do not invest in `melonds_HD_2`.

### Obsolete

Do not invest work in these forks:

- `nethersx2-thor/NetherSX2-patch` — ARMSX2 replaces it.
- `dolphin-thor/dolphin`
- `pcsx-redux-src`

A PlayStation fork is planned. The target is not chosen. `pcsx-redux-src` is a
candidate. It is not a commitment.

## ARMSX2 is the seed

The Thor layer already exists as a prototype in ARMSX2. The prototype was
verified on the device on 2026-08-21.

Read these documents before you design any shared feature:

- `docs/mcp-server.md` — the on-device MCP design. It covers framebuffer
  capture, settings read and write, emulator control, and per-game
  configuration. It is a design. It is not implemented. Use it as the blueprint
  for the fleet MCP.
- `docs/texture-upscaling-research.md` — texture-time upscaling. Thirteen
  filter kernels pass 26 of 26 self-tests at both scales, with no
  out-of-bounds writes. The cost and the appearance in a real scene are not
  measured.
- `docs/neural-models.md` — neural upscaling with Anime4K and FSRCNN. It
  defines the `.a2nn` model format. The loader, the inference and the pixel
  shuffle produce the expected checksum. The architecture is present. The
  trained weights are not shipped, for licence reasons.
- `docs/arm64-optimization-review.md` — the per-cluster 8 Gen 2 review. The
  document states that nothing in it is benchmarked on the device.
- `docs/texture-pack-getter.md` and `docs/third-party.md`.

Do not rebuild work that is already prototyped and verified on the device.

## The shared layer

The list below gives the shareable parts, in order of value.

### How to build the shared layer

**Build the shared layer by extraction. Do not invent it.**

The paradigms already exist. They are spread across the custom forks. Each fork
holds work that was written for that fork alone. Find that work. Lift it into
shared code. Then change each fork to call the shared code.

Harvest your own forks before you harvest an upstream. That code is already
written. It is already tuned for the Thor. It already runs.

The extraction has two goals:

- **Better shared code.** One implementation replaces many. A fix lands one
  time.
- **AI legibility.** An agent must find and change the code without a guide.

Work in this order for each paradigm:

1. Find every fork that implements it. Read each implementation.
2. Name the differences. Separate a real per-emulator need from an accident of
   history.
3. Define the contract. The shared part holds the algorithm, the data and the
   UI. The fork part supplies the facts only that fork knows.
4. Extract the shared part. Keep the best implementation as the base.
5. Convert one fork. Measure it. Then convert the rest.

Do not convert every fork at once. Convert one fork and prove the contract.

### The shared layer takes the licence of its most restrictive source

**Check the licence before you choose where to extract from.** Extracted code
keeps the licence of the fork it came from. The shared layer inherits the most
restrictive licence among its sources.

| Extract from | Shared layer can be |
| --- | --- |
| xenia-thor, BSD | anything |
| Cemu-thor, MPL-2.0 | MPL-2.0 or GPL |
| azahar-thor, Vita3K-Thor, GPL-2.0-or-later | GPL-2.0-or-later, or GPL-3.0 |
| ARMSX2, melonDS-android, eden-thor, GameThor, GPL-3.0 | **GPL-3.0 only** |

Consequence: a shared module built from ARMSX2 code is GPL-3.0, and only
GPL-3.0 backends can link it.

Prefer the least restrictive source when two forks have the same capability
and the quality is close. Example: the GPU driver manager exists in six forks.
xenia is BSD, so extracting the xenia implementation gives a shared module
that anything can use. Extracting from eden, which is GPL-3.0, does not.

This does not change the fleet decision. The app is GPL-3.0 and PS3 is out.
It matters if you ever want a shared module reusable outside this app, or
usable by a separately distributed PS3 backend.

**Names and interfaces are not the same as implementations.** A list of
algorithm names, a settings key or a file format is a fact, not expression.
The implementation behind it is expression. Keep that distinction in mind when
you copy a contract rather than a function.

Not legal advice.

### AI legibility is a requirement

An agent works across seven forks. Inconsistency costs more than it costs a
person, because the agent cannot ask.

Apply these rules to the shared layer and to each fork:

- Use the same directory name for the same job in every fork.
- Use the same term for the same concept in every fork. Do not rename a concept
  at a fork boundary.
- Keep the document next to the code it describes.
- Give each shared feature one source of truth. See
  [Federation](#federation-not-duplication).
- State the per-fork differences in one file. Do not make an agent infer them
  from a diff.

### 0. One toolchain — do this first

**Unify the toolchain before you extract any code.** Shared native code cannot
exist across seven C++ runtimes.

The fleet measured on 2026-08-22 uses seven NDK versions, from NDK 22 to
NDK 29. Two native libraries built with different NDK major versions cannot be
relied on to share a C++ runtime in one process. The libc++ ABI is not stable
across that range. This blocks the one-app goal directly.

Measured values are in
[`research_log/20260822_1559_shared_paradigm_survey.md`](research_log/20260822_1559_shared_paradigm_survey.md),
Finding 5.

### The standard row

**Every fork uses this row. No fork sets its own value.** Decided 2026-08-22.

| Setting | Value | Reason |
| --- | --- | --- |
| NDK | `29.0.14206865` | Latest stable r29. Installed on this box. |
| ABI | `arm64-v8a` only | The device reports `arm64-v8a`. |
| `minSdk` | 33 | The device reports API 33. |
| `targetSdk` | 37 | Android 17, released 2026-06-16. |
| `compileSdk` | 37 | Matches `targetSdk`. |
| Gradle | 9.6.1 or newer | The newest already in the fleet. |
| C++ standard | C++20 | Verify each fork builds. melonDS declares C++17. |

Notes:

- **NDK r30 is beta.** r30 becomes the LTS release. Move to it when it is
  stable, not before. Seven emulator cores on a beta toolchain is a bad trade.
  `30.0.15729638` is installed on this box. Do not use it for a shipping build.
- Drop `armeabi-v7a` and `x86_64` from the shipping build. The Thor is arm64.
  ARMSX2 already ships arm64 only.
- `minSdk` 33 is exact. The app targets one device. A lower value buys nothing.
- Google Play requires API 37 targeting from August 2027. This project is
  ahead of that date.
- **C++20 is not verified.** Build each fork before you commit to it. Record
  the result in a work log.

Unify the Vulkan setup too: one loader, one validation configuration and one
extension set.

### The device

| Property | Value |
| --- | --- |
| Model | AYN Thor |
| Android | 13 |
| API level | 33 |
| ABI | `arm64-v8a` |
| Hardware | qcom |
| Connection | Wi-Fi adb, port 5555 |

**Wi-Fi adb is the preferred connection.** The Thor stays on a wall charger
during a test run. A long run does not drain the battery, and battery level
does not confound a performance measurement.

**A second device is attached to this box.** A Quest 2 answers adb as well. A
bare `adb` command fails with "more than one device/emulator".

**Never run a bare `adb` command. Always pass `-s`.**

**Do not hardcode the address.** It was `192.168.1.3:5555` on 2026-08-22. A
Wi-Fi address changes when the DHCP lease changes. Resolve it at run time:

```sh
# Pick the Thor by model, not by address.
THOR=$(adb devices | awk '/device$/{print $1}' | while read s; do
  [ "$(adb -s "$s" shell getprop ro.product.model | tr -d '\r')" = "AYN Thor" ] \
    && echo "$s"
done)
adb -s "$THOR" shell ...
```

Wi-Fi adb rules:

- Reconnect with `adb connect <ip>:5555` after the Thor sleeps or the network
  drops. Treat a dropped connection as normal, not as a fault.
- Verify the target model before any `install`, `push` or `shell` that writes.
  A wrong `-s` value flashes the Quest.
- Do not measure performance over Wi-Fi adb while pulling a large capture. The
  transfer competes with the run. Pull after the run ends.
- Record the battery level and the charge state with every measurement. State
  whether the Thor was on the charger.

### 1. Upscaling and filters

Two paths exist. Both are already in the fleet. Neither needs a libretro core.

- **Texture-time.** Upscale each texture when the emulator uploads it. This
  runs one time for each texture, not one time for each frame. It improves the
  source art, not the finished frame. It works on any game. It needs no pack
  and no earlier playthrough. ARMSX2 implements this.
- **Present-time, through librashader.** librashader runs RetroArch slang
  shader presets in Vulkan, without RetroArch. ARMSX2 vendors it.
  melonDS-android also uses it. This gives access to the Anime4K preset set,
  without the frontend that made it slow.

**libretro is rejected.** Its core system is slow. Its UI is not what this
project ships. Take slang shaders from that ecosystem. Take nothing else.

HD texture packs stay complementary. A pack wins where a pack exists.
Texture-time upscaling covers every other case. melonDS-android implements a
content-hash pack format that is compatible with the desktop emulator. Use
that format as the fleet candidate.

### 2. Per-class routing — the first shared feature

Different art needs different algorithms. Anime4K suits sprites, 2D portraits
and UI art. Anime4K is often wrong for 3D world textures. A 3D texture may need
ScaleFX, a classical kernel, or no filter. One global filter is the wrong
shape.

**Two forks already implement this, under two names:**

- ARMSX2 defines its settings as renderer, scale factor, per-texture-class
  algorithm and per-game configuration. See `docs/mcp-server.md`.
- melonDS-android implements per-producer HD filtering. It has 3D texture
  filter modes 0 to 13, which include ScaleFX at texture upload. It also
  filters the OBJ and BG planes separately in the Vulkan compositor. Sprites,
  backgrounds and 3D geometry each get their own treatment.

Generalise this into one classification contract and one routing contract for
the fleet. Each emulator knows facts about a texture that the frame does not.
The facts include the producer, the dimensions, the format, the palette, and
the plane type. These facts are the input to the routing. The facts are
per-emulator. The routing table, the algorithm set and the UI are shared.

**This is the reason to prefer texture-time over present-time.** A
present-time shader sees one finished frame. It cannot separate an anime
portrait from a wall. Classification is available only at upload. Any feature
of the form "Anime4K here, a different algorithm there" must run at
texture-time.

Keep present-time through librashader for whole-frame effects. Those effects
need the composed image.

### 3. Thor hardware profile

Define the GPU driver selection, the cluster affinity, the thermal governor,
the panel resolution and aspect, and the controller and trigger mapping. Define
each one time. Each fork reads the profile. No fork hardcodes these values.

### 4. Cheat databases

`azahar-thor/cheat_sources/` holds Sharkive, CTRPF-AR-CHEAT-CODES and
citra-games-wiki. `ai_cheat_helper_switch` is a separate AI-driven method.
Unify the format. Do not unify the sources.

### 5. Mod and translation loading

Existing projects: `shin_2_eng`, `smt_if_eng`, `ever_oasis_mod`,
`radiata_stories_ending_mod`, `wild_arms_5_the_last_loop_mod` and
`toyko_xanadu_vr_etx`.

### 6. The game library and per-game overrides

This is a requirement, not an option.

**Every game must accept a custom override for every option.** No setting is
global only. A user opens one game and changes one setting for that game
alone. This applies to every backend, not to a chosen few.

Design consequences:

- Every setting needs a stable key. A setting without a key cannot be
  overridden.
- The resolution order is fixed: per-game value, then Thor profile default,
  then backend default. Resolve it in one place. Do not let a backend invent
  its own order.
- The in-game overlay and the settings screen read the same key. melonDS
  `HdFilterTarget` already does this. Copy that rule.

The library view shows cover art. Each entry shows badges for what the game
has:

- A cheat database entry exists.
- A per-game override is set.
- An HD texture pack is installed.
- A mod or a translation patch is applied.

The library is one list across every backend. It is not one list per emulator.

### 7. Save conventions and control overlays

### Vulkan is the substrate

Every Tier 1 fork renders through Vulkan on Android. A Vulkan interop contract
is the only foundation that exists across all of them. This makes a rework of
the render path and the present path worthwhile.

**You may rework major guts, and cores.** This project does not stay a thin
patch set on top of upstream. Divergence is acceptable and expected when it
buys a shared paradigm or a measured speedup.

Do not rework for taste. Do not accept divergence that buys nothing and that
removes the ability to harvest upstream work. State which case applies before
you start.

## Provenance: harvest and adapt

No single upstream exists. Each fork already has several remotes that point at
sibling forks:

- xenia-thor: xenia-canary and xenia-edge.
- Cemu-thor: cemu-project, SapphireRhodonite and SSimco.
- watermelon-DS-THOR: WatermelonDS and melonDS-android-lib.
- rpcsx-ui-android-thor: RPCSX, rpcs3, aps3e and ARMSX3.

The `reference/` and `_research/` directories already do this work by hand.
`xenia-thor-workspace/reference/` holds Box64, FEX, XenonRecomp, XenosRecomp,
N64Recomp, XenDroid and xenia-edge.

**Record the source, the purpose and the result for everything you take.**
State whether the change was better, or only different.

The ledger lets an agent re-derive a change after the upstream drifts. The
ledger also lets you evaluate one improvement against every other fork.

Write the intent in each entry. A diff without its intent cannot be
re-derived. It can only be replayed until it fails to apply.

## Federation, not duplication

Each fork has its own `AGENTS.md`. Several forks also have a `CLAUDE.md` with a
different purpose. **Those files are the source of truth for their fork.**

The house rule comes from `ps3-thor/CLAUDE.md`:

> Do not copy content into this file. Two copies of a map disagree.

This repo points at the fork documents. This repo does not repeat them. Read
fork-specific knowledge from the fork.

The file names are not uniform. Check before you assume:

- In `ps3-thor/`, `CLAUDE.md` is a pointer to `AGENTS.md`.
- In `ps3-thor/rpcsx-ui-android/`, the two names do different jobs.
  `AGENTS.md` is the operating contract. `CLAUDE.md` holds AArch64 hardware
  knowledge. Neither file is a pointer.

## The MCP

Build one MCP server with three surfaces.

- **Fleet state.** Report the current base of each fork, the available upstream
  releases, the shared features applied to each fork, and the drift. Start
  builds. Read build failures.
- **On device.** Use adb to install, to launch, to read logcat, to capture the
  framebuffer and screenshots, and to record FPS, frametime, temperature and
  GPU counters. This surface closes the experiment loop. Without capture, a
  human must compare every A/B pair by hand.
- **Knowledge base.** Serve the manuals, the AI skills, the fork research
  notes, the ARM reference documents, and the analysis spread across the fleet.

`armsx2-thor/ARMSX2/docs/mcp-server.md` scopes this into four capability
groups. Read it first.

## Tests are mandatory

**A change without a test does not land.** This is the only way to develop at
this scale. One person and an agent fleet cannot hold seven emulators in their
head. The test suite holds it instead.

An emulator is easier to test than most software, because it is deterministic
by construction. The same input and the same state produce the same frame.
Most emulator projects never use this. The paradigms below turn that property
into automation.

### The paradigms that make emulator QA automatic

Ranked by value. The fleet already has most of them, in one fork each.

1. **GPU trace capture and replay.** Capture the command stream, then replay it
   headless without the game and without the CPU core. This tests the renderer
   at full speed, in a queue, with no controller input. It is the single
   highest-value test tool an emulator can have.

   In the fleet: ARMSX2 `pcsx2-gsrunner` with `RenderDocCapture`. xenia-thor
   `d3d12_trace_dump_main.cc` and `d3d12_trace_viewer_main.cc`, plus the
   `GpuTraceViewerActivity` on Android. rpcsx `rsx_replay.cpp`.

2. **Savestate as the test fixture.** Boot to a saved state instead of playing
   to the scene. This cuts a test from minutes to seconds, and it removes the
   menus, the intros and the loading from the measurement.

   In the fleet: Vita3K-Thor `tools/android/run-thor-quickstate-regression.ps1`.

3. **Golden image comparison.** Render a fixed frame, then compare it to a
   stored reference with a perceptual metric. An optimisation that breaks the
   rendering fails on its own, with no human looking at a screenshot.

   In the fleet: ARMSX2 `pcsx2-gsrunner/comparer.js` and `comparer.css`.

4. **Deterministic input replay.** Record the input stream, then replay it
   exactly. This makes a whole play session a repeatable test.

   In the fleet: azahar-thor `src/core/movie.cpp`, with record and play
   dialogs. xenia-thor researched the opposite approach and wrote it up in
   `docs/research/20260529-210700-deterministic-input-avoid-movies.md`. Read
   both before you choose.

5. **An on-device regression matrix.** A declared list of games, scenes and
   settings that runs on the real hardware and reports a table.

   In the fleet: Vita3K-Thor `tools/android/thor-render-regression-matrix.json`
   and `run-thor-regression-suite.ps1`.

6. **Performance as a test.** Record FPS, 1% low and frametime for each commit.
   Fail the build on a regression beyond a threshold. Almost no emulator does
   this on the target hardware. The Thor is one fixed device, so the numbers
   are comparable across commits.

7. **Differential testing, interpreter against recompiler.** Run the same code
   through both, compare the state, and stop at the first divergence. This
   finds a recompiler bug at the exact instruction instead of at the crash.

8. **Sanitizer builds.** ASan, UBSan and TSan in the automated build. Many
   emulators cannot even compile with them. Getting there is the work.

9. **Boot and compatibility sweep.** Launch every game in the library headless,
   record how far each reaches. This catches an upstream harvest that broke a
   console without anybody playing it.

### The gap

Items 1 to 5 exist in the fleet **today**, spread across four forks. No fork
has more than two. Nothing shares them.

Two forks already wrote agent skills for this work:

- `xenia-thor-workspace/xenia-thor/.agents/skills/xenia-renderdoc-replay/`
- `psvita/Vita3K-Thor/.agents/skills/vita3k-regression-ledger/`

**Extract the test harness before the renderer features.** A shared harness
makes every later extraction safe to attempt. Without it, an agent cannot tell
a good port from a regression, and the fan-out in
[Agentic acceleration](#agentic-acceleration) produces damage instead of work.

### Rules

- Every shared-layer change needs a test that fails before the change.
- Every performance claim needs a number from the device, and the commit it was
  measured at.
- Every harvested change needs the test that proves it survived the port.
- A test that needs a human to look at a screenshot is not a test. Automate the
  comparison or record it as a known limit.

## AI-driven development, QA and experiments

The MCP exists to make experiments cheap. The ARMSX2 MCP document states the
problem. A person cannot evaluate twenty upscaling algorithms across a game
library by hand. The manual loop is: boot the game, reach a scene, open the
pause menu, change the algorithm, take a screenshot, compare, repeat. The
tooling exists to remove that loop.

Rules for experiments:

- State the measurement before you make the change. Name the scene, the counter
  and the cluster.
- A self-test result is not a performance result. The thirteen ARMSX2 kernels
  pass 26 of 26 self-tests. Nobody knows their cost or appearance in a real
  scene. State which of the two results you have.
- Keep a reference image for visual correctness. An optimisation that breaks
  the rendering must fail the test automatically.
- Report the true result. "Not benchmarked on the device" is a valid finding.
  The existing fork documents meet this standard. Meet it too.

### Agentic acceleration

Run development autonomously wherever the loop closes. The fleet suits this.
"Solve once, apply everywhere" is a fan-out. Seven forks give seven parallel
evaluations of one question.

**Fan out across the fleet.** After a change lands in one fork, ask where else
it applies. Run one agent for each fork, at the same time. Each agent answers
three questions. Does this fork have the same problem? Does the same fix shape
work here? What does it cost? The output is a propagation report. The output is
not seven blind ports.

**Isolate parallel work.** Two agents that build or patch the same fork at the
same time will corrupt each other. Use one git worktree for each experiment.
Builds are long. A corrupted tree costs a full rebuild of Cemu or Xenia.

**Close the experiment loop.** Propose, build, install, measure on the device,
then keep or revert. Decide from the captured numbers. Do not decide from an
argument about the diff.

**Write skills instead of repeating prompts.** Put the build recipe, the flash
steps and the measurement procedure for each fork into a skill.
`melonds_HD/melonds_HD_2/.claude/skills/` already does this. Apply the pattern
to the fleet.

**Schedule the unattended jobs.** Upstream watching, drift detection and
clean-build health are recurring jobs. Run them on a schedule. Do not wait for
a request.

**Treat the ledger as agent memory.** An agent reads the ledger to re-derive a
drifted change. An agent also reads it to check whether a proposal was already
tried and rejected. Write it for that reader.

Two limits are real. Builds are long. There is one physical Thor. Parallel
agents speed up analysis and patch generation. They do not multiply the device.
Device measurements run in a queue.

## Conventions and hazards

- **AI attribution.** `melonDS-android` is public. Its community is hostile to
  AI. Commits and pull requests for that fork must contain no AI attribution.
  Use no `Co-Authored-By` footer. Write plain contributor-style messages. Read
  the `AGENTS.md` of each fork before you commit. Do not assume a fleet
  default.
- **Build Android targets from PowerShell. Do not use Git-Bash or MSYS.**
  Environment variables exported in Git-Bash do not reach the forked gradle
  daemon. AGP still finds the SDK through `local.properties`. Cargo then fails
  while the SDK appears correct. `melonds_HD/CLAUDE.md` records that this
  symptom costs about one hour.
- **Set `git config core.longpaths true` on Windows.** librashader has test
  paths longer than 260 characters. This box already has the setting.
- **Most forks sit one directory below a workspace directory.** The workspace
  directories are `armsx2-thor/`, `azahar-thor/`, `dolphin-thor/`,
  `nethersx2-thor/`, `xenia-thor-workspace/`, `ps3-thor/`, `melonds_HD/` and
  `psvita/`. Some workspace directories have their own `.git`. The fork is
  inside.

## Order of work

Do these in order. Each phase unblocks the next. Do not start a renderer
feature before phase 3.

### Phase 0 — unblock and baseline

**0.1 Verify the rpcsx licence. DONE 2026-08-22.** Result: rpcsx is
GPL-2.0-only. It cannot share a binary with the GPL-3.0 forks. See
[Licences](#licences-constrain-the-one-app-plan).

**0.1b Closed.** PS3 is out of the app, so no GPL-2.0-only code is linked. The
combined-work question does not arise. The app is GPL-3.0.

**0.2 Decide where shared code lives.** A directory in this repo, consumed by
each fork's build. Answer this before any extraction. It is a smaller question
than the packaging of the shipped app, and it does not wait on 0.1.

**0.3 Build every Tier 1 fork as it stands today. Record the result.** You
cannot migrate a toolchain you cannot build. One work log for each fork. Record
the command, the time taken and the failure.

This baseline also answers [Open decisions](#open-decisions) item 3, the build
location, with measured build times instead of a guess.

### Phase 1 — migrate the toolchain

Move each fork to [the standard row](#the-standard-row). One fork at a time.
Build after each change. Verify C++20 per fork.

Start with **melonDS-android**. It is the only fork with a build recipe already
verified on this box, dated 2026-07-12 in `melonds_HD/CLAUDE.md`. A known-good
starting point separates a toolchain failure from a recipe failure.

Then continue in rising order of build cost. Leave ARMSX2, Cemu-thor and
xenia-thor until last. They are the most expensive to build.

**Nothing can be shared until this phase ends.** Seven C++ runtimes cannot
share native code.

### Phase 2 — extract the GPU driver manager

This is the first extraction. It is chosen because it carries the least risk.

Six forks vendor `libadrenotools` and each wrote its own driver picker. There
is one GPU. There is no real per-emulator variation to preserve, unlike the
texture class list.

Read `rpcsx-ui-android` `GpuDriverAdvisor.kt` first. It is the only
implementation that advises rather than lists.

This phase proves the five extraction steps end to end. See
[How to build the shared layer](#how-to-build-the-shared-layer). If the process
fails here, it fails on an easy case, and that is cheap to learn.

### Phase 3 — extract the test harness and build the MCP device surface

These two ship together. The harness needs capture. The MCP surface provides
it.

Take these, in this order:

1. The Vita3K-Thor on-device regression suite and its savestate fixture runner.
   It already runs on the Thor.
2. The ARMSX2 golden image comparer, `comparer.js`.
3. The ARMSX2 headless replay pattern, `pcsx2-gsrunner`.
4. The two existing agent skills, from xenia-thor and Vita3K-Thor.

Build the MCP on-device surface against
`armsx2-thor/ARMSX2/docs/mcp-server.md`.

**After this phase, an agent can tell a good port from a regression.** Before
it, the fan-out in [Agentic acceleration](#agentic-acceleration) does damage.

### Phase 4 — per-class routing and upscaling

The flagship feature. It is safe to attempt only after phase 3.

Base the shared algorithm enum on ARMSX2 `GSTextureUpscaleAlgorithm`. Add
`Super2xSaI` and `Quilez` from melonDS-android. Keep the class list per-fork.

### Later

- The game library, cover art and per-game overrides. Survey xenia-thor
  `GameProfiles.java` first. It is the most complete Android shell in the
  fleet.
- Cheat database unification.
- Mod and translation loading.

## Open decisions

These are not settled. Do not assume an answer. Ask, or mark the assumption.

1. **The toolchain row.** Which NDK, `minSdk`, `targetSdk`, Gradle, AGP and
   C++ standard does the fleet use? Every other decision waits on this one.
   See [One toolchain](#0-one-toolchain--do-this-first). Decide this first.
3. **The build location.** Options: local Windows or WSL, GitHub Actions, or a
   split. Cemu, Xenia and RPCSX are expensive to build locally. This decision
   sets how much an agent can do unattended.
4. **git-lfs for `hardware_ref/`.** Manuals are large PDFs. Decide before you
   commit a large set.
5. **The workspace layout.** The forks stay in place today. This file tracks
   them. Defer a move to one root, and defer submodules, until the tooling
   works.
6. **The PlayStation fork.** Planned. Target not chosen.

### Settled

- **The product is one real app.** It is not a hub over other emulators. See
  [What this repo is](#what-this-repo-is).
- **libretro is rejected.** Take slang shaders through librashader. Take
  nothing else.
- **Build the shared layer by extraction** from the existing forks. See
  [How to build the shared layer](#how-to-build-the-shared-layer).
- **Per-game override for every option** is a requirement. See
  [The game library and per-game overrides](#6-the-game-library-and-per-game-overrides).
- **Backends are separately distributed modules**, fetched at run time. The
  contract is thin. See [The backend model](#the-backend-model).
- **PS3 is deferred**, not cancelled.
