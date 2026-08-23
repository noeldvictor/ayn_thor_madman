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

Vulkan on Adreno 740. ARM64 on Snapdragon 8 Gen 2. Android 13. One pinned
Mesa Turnip driver. See [The driver baseline](#the-driver-baseline-pinned-turnip).

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

### 4. Easy to use, for somebody with a full-time job

**The user has limited time and wants to play a game.** Configuration is not
the hobby. Every step the app demands is taken from the time a person had to
play.

RetroArch is the anti-pattern, and these are its named failures:

- **Cheats are not first class.** Finding, installing and enabling a cheat is
  a research task. In this app, cheats are a first-class feature with a
  library, a search and a per-game view. See
  [Cheat databases](#4-cheat-databases).
- **No common hotkeys.** Each core does its own thing. In this app, **one
  hotkey set works on every system**. Save state, load state, fast forward,
  rewind, screenshot, overlay and menu are the same everywhere, always.
- **It demands study.** A person should not read a wiki to play a game.

The rules that follow:

- Sensible defaults. A game runs well with no setup.
- One place for every setting. No hunting across backends.
- Nothing requires a file manager, a text editor or a wiki.
- The same control does the same thing on every system.
- The hard controls stay available for somebody who wants them. They are not
  in the way of somebody who does not.

**Judge a feature by the time it costs the person, not by its power.**

### Frame generation already exists in the fleet

**ARMSX2 has a complete Vulkan frame-generation subsystem, 31 files, all
GPL-3.0-or-later.** Nothing else has one, and nothing recorded it until
2026-08-22.

`pcsx2/GS/Renderers/Vulkan/FrameGen/`, ported from **eden PR #4263** and
**lsfg-vk**. It includes a `FrameGenPacer`, because generated frames need their
own pacing decision.

**On a handheld this may be the highest-value feature in the fleet.** Frame
generation makes a 30 fps game feel like 60 without rendering more frames. On a
device where the bottleneck is GPU-bound and architectural, that buys smoothness
optimisation cannot.

Two further points:

- **Its provenance comment is the only proper one in the fleet.** It names the
  source project, the pull request, the file and what was kept unchanged. That
  is exactly what [Provenance](#provenance-harvest-and-adapt) asks for.
- **The local `eden-thor` checkout does not have it**, because the port came
  from a later PR. The fleet is out of date with its own members.

### 5. Quality of life, everywhere

These are not extras. They are the reason to build a unified app rather than
run eight separate emulators.

- **Texture improvement.** Per-class routing, so sprites and 3D art get
  different treatment. See
  [Per-class routing](#2-per-class-routing--the-first-shared-feature).
- **HD texture packs.** Install one without a guide.
- **Mods and translations.** Install from inside the app.
- **Cheats.** One library across every system.
- **Frame generation.** ARMSX2 already has 31 files of it, GPL-3.0-or-later.
  See above.
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

## Current phase: deep exploration

**We are working out the architecture and the contracts. We are not building
the product yet.** Stated 2026-08-22.

### What this means

- **Decisions and findings are the deliverable.** A recorded decision with its
  evidence is progress. So is a survey that proves a fork already has
  something.
- **Code written now is a probe, not a product.** Build something to learn
  from it, then be willing to throw it away.
- **Reversibility beats completeness.** Prefer a decision that can be undone
  cheaply over one that is thorough and locked in.
- **Do not optimise yet.** There is nothing measured to optimise. Speed is the
  goal, and a guess about speed is not.
- **Do not start a long implementation.** A month of work built on an
  unsettled contract is a month lost.

### Why the documents are the work

This repo holds more prose than code on purpose. In this phase the expensive
mistakes are architectural, and they are cheap to fix in a document and
expensive to fix in seven forks.

Every decision here carries its evidence. See the surveys in
[`research_log/`](research_log/) and the fleet state in
[`capability_inventory.md`](capability_inventory.md). A decision without
evidence is an opinion, and opinions get re-argued.

### How we know this phase is ending

Three tests. All three must pass:

1. **The backend contract is written**, and it came out of a UI shell rather
   than an argument. See [Track A](#track-a--the-ui-shell-defines-the-contract).
2. **One backend runs behind the shell** on the device, on both displays.
3. **One subsystem is extracted and owned**, with the fork's copy deleted and
   the build guard in place. See
   [Duplication must be structurally impossible](#duplication-must-be-structurally-impossible).

Until then, treat any implementation plan longer than a week as premature.

### What is still unknown

Direct the exploration at these:

- The open items in [Open decisions](#open-decisions).
- The survey gaps in [`capability_inventory.md`](capability_inventory.md).
  Cemu-thor, eden-thor and GameThor have no capability recorded at all.
- Control overlays, save conventions, thread and cluster affinity, audio
  latency and frame pacing are not surveyed in any fork.
- Clean build times for every fork. Only melonDS-android has been built, and
  that was incremental.

## What this repo is

This repo is the control plane for a fleet of emulator forks. The forks target
the AYN Thor.

This repo holds the shared parts. An agent starts here to work on any fork.

The goal is one workspace, one MCP, one shared layer and one app.

**The product is one real app. It is not a hub over other emulators.** The app
does not install seven other apps and launch them. The emulators become
backends inside one binary, with one UI, one library and one settings system. The user sees one product. This is a unified emulator.

See [The backend model](#the-backend-model) for how a backend loads and why
the contract between the app and a backend is deliberately thin.

The app is not built on libretro. See
[Upscaling and filters](#1-upscaling-and-filters).

To make this possible, the toolchain must be unified first. See
[One toolchain](#0-one-toolchain--do-this-first).

## The backend model

Decided 2026-08-22. This resolves how a backend loads.

**Pack everything together. One binary. The only exception is PS3, which is an
optional separate install.**

The reason is optimisation across shared flows. A backend that is a separate
module cannot share a link unit, a Vulkan device or a cache with the rest. That
cost is paid at run time, on a handheld, forever. It contradicts
[Foundation](#foundation) point 2.

### What packing together buys

These are the shared flows. None of them work across a module boundary.

- **One Vulkan device.** One instance, one device, one queue plan, one
  allocator. Separate modules mean separate devices and duplicated memory
  budgets.
- **One pipeline and shader cache.** Shader compile stutter is a Thor-wide
  problem. One cache serves every backend and survives between them.
- **One texture upload path.** Per-class routing, upscaling and pack
  replacement live in the upload path. Sharing it is the whole point of
  [Per-class routing](#2-per-class-routing--the-first-shared-feature).
- **One thread pool with cluster affinity.** The 8 Gen 2 is 1+4+3. Two
  backends with their own pools fight over the X3 core. One scheduler does
  not.
- **One memory budget.** A handheld has a hard limit. One owner can decide
  between a texture cache and a shader cache. Two owners cannot.
- **Link-time optimisation across the boundary.** The shared layer sits in the
  hot path. Inlining across it is free speed, and a module boundary blocks it.
- **One frame pacing and present path.** Consistent pacing needs one owner.

### The exception: PS3

`ps3-thor/rpcsx-ui-android` is GPL-2.0-only and cannot share a binary with the
GPL-3.0 code. It is therefore the one optional, separately installed backend.

This is forced by the licence, not chosen for architecture. PS3 accepts the
cost of separation: no shared device, no shared cache, no shared upload path.
It gets the app's library and UI, and little else from the shared layer.

PS3 is deferred until the backend contract takes shape. See
[No GPL-3.0 PS3 emulator exists](#no-gpl-30-ps3-emulator-exists). The
RetroArch separate-distribution pattern is how it returns, and it applies to
PS3 alone. See
[How RetroArch handles a GPL-2 core](#how-retroarch-handles-a-gpl-2-core-under-a-gpl-3-frontend).

**Do not let the PS3 exception shape the main design.** One backend under a
different licence must not force every other backend behind a module boundary.

### This is one app, not a hub

| Rejected hub model | This model |
| --- | --- |
| Installs other apps. | One binary holds the backends. |
| Each app has its own UI. | One UI. The app owns every screen. |
| Launches another app and leaves. | Drives the backend in-process. |
| The user sees seven products. | The user sees one product. |

A user never sees a backend's own interface. There is no other interface.

### The contract is thin, because the cores differ

Packing together does not mean one uniform core API. **Do not force a narrow
API on every backend.** A PS2 recompiler, a Wii U graphic pack system and a DS
plane compositor do not fit one shape. Forcing them into one is what makes
libretro slow and limiting.

Define a **minimum** contract that every backend meets:

- Lifecycle: load, run, pause, stop, save state, load state.
- Surface: how the backend receives the shared Vulkan device and its target.
- Input: how the app delivers controller and touch state.
- Settings: the key namespace, and the resolution order for a per-game
  override.
- Paths: where saves, states, packs, cheats and screenshots live.

Everything past that is a **per-backend extension**. A backend declares what
extra it supports, and the app shows the UI for the ones present. Cemu
declares graphic packs. melonDS declares three filter planes. ARMSX2 declares
two texture classes. None pretends to be the others.

The contract is an internal interface, not a module boundary. It exists for
clarity, and it must stay inlinable.

**[`shared_layer/BACKEND_STANDARD.md`](shared_layer/BACKEND_STANDARD.md) is the
other half.** The header says how a backend plugs in. The standard says what it
must deliver: what it must never do, what is required, what is expected, what it
may declare, the frame and power bar, and the quality gate. **It is the
acceptance test for "is this backend finished".**

### The costs, stated

Packing together is not free. Accept these:

- **A large binary.** Seven cores in one package. The app is sideloaded, so
  size is tolerable, but it is real.
- **Long builds.** A change to the shared layer rebuilds everything. This
  raises the value of the build-location decision in
  [Open decisions](#open-decisions).
- **One crash takes the app down.** A fault in any core kills the process.
  Crash isolation must come from testing, not from a process boundary.
- **The toolchain must be unified first.** Seven C++ runtimes cannot link.
  This is why [One toolchain](#0-one-toolchain--do-this-first) is Phase 1 and
  blocks everything.

## Read before you claim

**Every claim of the form "no fork has this" made in this repo has been wrong.**

| Claim | Reality |
| --- | --- |
| No fork has differential testing | `melonds_HD_2/renderer_cases/` does |
| The on-device MCP is design only | xenia has an implemented server |
| No fork validates driver GPU family | rpcsx `GpuDriverAdvisor` does |
| Three forks duplicate an LRU cache | three different designs |
| Six forks duplicate a driver picker | four different concerns |
| The Thor hardware profile is to be designed | rpcsx `ThorPerformanceProfile` exists |
| No CPU-side differential testing | ARMSX2 `tests/ctest/core/recompilers/` |
| Dual-screen routing must be designed | azahar and melonDS both ship it |
| Only melonDS has haptics | azahar has `hapticFeedback` too |

The cause is the same every time: **the inventory was built from file listings,
and a listing cannot tell you what a file does.**

Rules:

1. **Never write "no fork has this" without a search that names its method.**
   State what you searched for, where, and how.
2. **A capability recorded from a listing is a hypothesis.** Mark it unread
   until somebody opens the file.
3. **Reading is cheap.** Each correction above took under fifteen minutes and
   each reversed a plan that was already written down.
4. **Similar names are not a shared capability.** Three LRU caches were three
   designs. Six driver pickers were four concerns.

## Duplication must be structurally impossible

**The root problem: agentic coding accelerates duplication.**

Before agents, a feature was expensive, so it was built once. An agent builds
it in an afternoon, in whichever fork it was pointed at. The same feature now
appears in three forks in a week, each version slightly different.

The evidence is in this repo. ARMSX2, melonDS-android and azahar-thor each
built per-class texture filtering separately. Six forks each built a GPU driver
picker. None of that is old mess. It is happening now, and it is speeding up.

**A rule in a document does not stop this.** An agent skips what it does not
read. The fix must be structural.

### The mechanism

When the shared layer takes a subsystem, the fork loses the ability to have
one.

1. **Extract** the subsystem into the shared layer.
2. **Delete** the fork's implementation. Do not leave it unused. Dead code is
   an invitation.
3. **Depend.** The fork's build links the shared module. The fork implements
   the contract and nothing more.
4. **Guard.** The build fails if the subsystem reappears in the fork. A new
   file under a deleted path, or a symbol that duplicates an owned one, is a
   build error, not a review comment.

An agent that tries to add a texture filter to a fork finds no directory for
it, no build target, and a failing build. That is cheaper than any review.

### The ownership list

[`shared_layer/OWNED.md`](shared_layer/OWNED.md) records every subsystem the
shared layer owns. It is the input to the build guard.

**The owned list is empty today**, and the file exists so the emptiness is
visible rather than assumed. It also carries the queue, ordered by risk, and the
**rejected** candidates with their evidence. **Recording a rejection matters as
much as recording an extraction**, because both stop the question being
re-argued.

For each owned subsystem, record:

- What it covers, precisely enough to write the guard.
- Which forks are converted, and which are not yet.
- The paths deleted from each converted fork.

**A subsystem is either owned or not owned. There is no partly owned.** A
half-converted subsystem is how duplication returns.

### Depth is decided per subsystem

There is no fixed rule for how deep the shared layer reaches. Judge each hot
path on its own evidence:

- How badly do the forks implement it now, and how many times?
- How active is upstream in that path?
- What does the merge cost become after we replace it?
- What does the fork uniquely know that the shared layer cannot?

Record the decision and its reasons before you extract. Add it to
`shared_layer/OWNED.md`. A depth decision made once, in writing, stops the
question being re-argued per fork.

## What "high performance" means here

Three models exist for running many emulators on one Android handheld. Ours is
the third, and it is not a middle ground between the other two.

| | Separate apps, the Retroid and Odin default | libretro and RetroArch | **This project** |
| --- | --- | --- | --- |
| Emulator | untouched, standalone | wrapped behind a narrow API | **hollowed out on the host side** |
| Vulkan device | one per app | one per core | **one, shared** |
| Shader cache | one per app | one per core | **one, shared** |
| Texture path | one per app | one per core | **one, shared** |
| Driver | chosen per app, by hand | chosen per app | **one pinned build, bundled** |
| Thread scheduling | apps fight for the X3 | apps fight for the X3 | **one scheduler, cluster aware** |
| Memory budget | no owner | no owner | **one owner** |
| Settings | N schemas | one lowest common denominator | **one schema, every option per game** |
| Can it make a core faster | no | no | **yes, that is the point** |

### What a Thor core is

**A Thor core is an emulator with its host-side pipelines removed.**

That is the definition. It is not a libretro core and it is not a standalone
emulator.

- A **libretro core** keeps everything and is wrapped. RetroArch adds UI at
  the edges and cannot reach inside, because it must support hundreds of
  cores.
- A **standalone emulator** keeps everything and shares nothing. Eight
  standalone emulators on one device means eight Vulkan devices, eight shader
  caches and eight fights over the prime core.
- A **Thor core** keeps its guest side and **gives up its host side**. It
  keeps the ISA decoder, the guest GPU model, the guest memory map and the
  timing. It hands over the Vulkan device, the caches, the upload path, the
  scheduler, the memory budget and the present path.

See [`shared_layer/PATTERNS.md`](shared_layer/PATTERNS.md) for the line
between guest side and host side, pipeline by pipeline.

### Where the wins actually come from

Be honest about which of these are proven and which are expectations.

**Structural, and certain:**

- One shader cache warmed across every session, instead of eight cold ones.
- One memory budget arbitrating between texture and shader caches, on a device
  with a hard ceiling.
- One scheduler that knows there is a single X3 prime core. xenia already
  measured guest threads pinned to the A510 cores while the X3 sat idle.
- One pinned driver, so every backend gets the fastest known configuration
  rather than whatever the user last installed.

**Expected, and unproven:**

- Link-time optimisation across the shared layer in the hot path.
- Shared upscaling costing less than eight separate implementations.

**Explicitly not claimed:**

- A large frame win from a shared renderer. xenia's ledger records many
  incremental GPU levers as `DEAD` or `FLAT`. See
  [Expect maintenance wins from the shared layer, not large frame wins](#expect-maintenance-wins-from-the-shared-layer-not-large-frame-wins).

### The features nobody else can copy

Speed is not the only axis, and two items here are structurally unavailable to
a multi-device product:

- **Two internal touch displays.** Three systems in the fleet are dual-screen.
  A frontend that targets many devices cannot rely on a second screen.
- **One device, one driver, one hardware profile.** Every tuning decision can
  be exact rather than defensive.

## Architecture assessment, 2026-08-22

Recorded after reading the LRU caches, the driver managers and the texture
caches. **The reads changed the thesis.**

### What is solid

- One app, packed binary, GPL-3.0, PS3 deferred. Licence-forced, not
  preference.
- The thin contract with declared extensions. **Proven**, by implementing it
  for three deliberately divergent backends in `app/shell/`.
- The eight pipelines and their shared and not-shared lines.
- The pinned driver baseline.
- Two displays as a differentiator no multi-device product can copy.
- The measurement discipline inherited from xenia.

### What is weak: the shared-code thesis

Every duplication claim that has been read has shrunk:

| Claimed | After reading |
| --- | --- |
| Three forks duplicate an LRU cache | three different designs |
| Six forks duplicate a driver picker | four different concerns |
| Texture cache hashing is shared | guest-specific, not shareable |
| A shared renderer wins frames | xenia's ledger: incremental GPU levers `DEAD` or `FLAT` |

**Four for four.** The more carefully the fleet is read, the less genuine code
duplication there is. That is evidence and the plan must answer it.

### The revised position

**The value is not mostly in merging code. It is in three other places.**

1. **The app layer.** One UI, one library, per-game overrides, dual-screen
   routing, cheats, patches, storage. No duplication question arises, because
   none of it exists yet in any fork. This half is strong and it is what the
   project was asked for.
2. **Propagating techniques.** ARMSX2's `WithRemovedCLUTHash` belongs in
   melonDS. rpcsx's `GpuDriverAdvisor` belongs everywhere. The two affinity
   lessons belong in one scheduler. azahar's `hashes` vector belongs in every
   pack loader. **These are ideas, and ideas cross licence boundaries freely.**
3. **Shared assets and infrastructure.** One driver, one shader cache, one
   test harness, one measurement culture, one game library.

**Revised philosophy: share the app, share the knowledge, share the assets, and
be conservative about shared code.** Extract only where reading proves genuine
duplication. The GPU driver manager still qualifies. The LRU cache did not.

This does not cancel [the hot path philosophy](#the-philosophy-share-the-hot-path-not-the-periphery)
below. It narrows where it applies. Reaching into a core is still sanctioned;
it now needs a read that proves the reach is worth it.

### The target is a native-quality render path, not the average of seven forks

Decided 2026-08-22. **This supersedes "extract the seven device layers".**

Every one of those seven Vulkan layers was written to be **portable** across
desktop and mobile vendors, drivers and API versions. Extracting from them
yields the union of seven sets of compromises. Portability is exactly the thing
this project refuses to pay for.

The specification is [`shared_layer/THOR_RENDER.md`](shared_layer/THOR_RENDER.md):
what a renderer looks like if it will only ever run on one Adreno 740, under
one pinned Turnip build, on one device. Eight commitments, of which the central
one is that **the render graph is a GMEM residency plan** rather than a
formality.

Extraction still happens. It now has a target to aim at, instead of averaging.

### The reference is a native game, not another emulator

**The gap between an emulator and native on this device is structural, not
CPU.**

PS2, Xbox 360 and Wii U had eDRAM or immediate-mode GPUs. Switching render
targets, reading back and setting state per draw were cheap there. On a tiler
each one is a GMEM resolve out to system memory and back. **A faithful emulator
inherits a rendering structure designed for the opposite architecture**, and no
amount of CPU work recovers those frames.

| | Well-optimised native | A faithful emulator |
| --- | --- | --- |
| Render passes | one, merged subpasses | one per guest target switch |
| Resolves per frame | zero beyond present | one per target switch |
| Draw order | front to back, LRZ rejects | the guest's, LRZ breaks |
| Pipelines | all precompiled | created as the guest sets state, so stutter |

**Faithfulness is required at the pixel. It is not required at the pass
boundary.** That is the licence to rewrite guest render structure into a
tiler-friendly one, and it is the same insight behind xenia's translation
conclusion.

**The experiment this implies, and it needs no emulator work:** capture a
well-optimised native game on the Thor with Perfetto and Snapdragon Profiler,
record pass count, resolves, GMEM residency, the vertex against fragment split,
bandwidth and watts, then capture one backend on a comparable scene. **The
difference between those two captures is the actual roadmap.** xenia already
has the skills for both captures.

### Start with Vulkan, at the device layer, not the renderer

Decided 2026-08-22 after reading. **This is now the first extraction, ahead of
the driver manager.**

**Seven forks each built their own Vulkan device layer.** Verified, not
assumed:

| Fork | Its own device layer |
| --- | --- |
| ARMSX2 | `GSDeviceVK`, plus a vendored `vk_mem_alloc.cpp` |
| Cemu-thor | `VKRMemoryManager` |
| azahar-thor | `vk_instance`, `vk_memory_util` |
| melonDS-android | `VulkanContext` |
| Vita3K-Thor | `vulkan/context.cpp`, `vulkan/allocator.cpp`, vendored VMA-Hpp |
| xenia-thor | `ui/vulkan/`, `vulkan_shared_memory` |
| eden-thor | `vulkan_device`, `vulkan_instance` |

Three of them vendor a memory allocator separately.

**MEASURED 2026-08-22, and it changes the shape of the job.** Six of the seven
built the device layer **inside a renderer**. Only xenia built it as a module:
`src/xenia/ui/vulkan/` against `src/xenia/gpu/vulkan/` is the shared and
not-shared line already expressed as a directory. Cemu's `VulkanRenderer.cpp` is
4,465 lines doing both; ARMSX2's `GSDeviceVK.cpp` is the same shape.

**Take xenia's.** It is 11,471 lines, about 7,000 once its Vulkan-drawn UI is
dropped, and it is **BSD**, so the shared module stays usable by anything.

**The extraction is therefore not "merge seven implementations". It is "take one
module, then unpick six renderers from their own device creation".** The second
half is per-fork and is where the cost actually is.

**Unlike every other candidate, this one cannot be guest-specific.** Creating an
instance, choosing a physical device, setting up queues and allocating memory
have no guest semantics. That is why this is genuine duplication where the LRU
cache was not.

It is also **required** by the packed binary. One binary cannot sensibly hold
seven Vulkan devices.

#### The line

**Shared, below the renderer:**

- Instance creation, extension and layer selection, validation configuration.
- Physical device selection and queue families.
- The logical device and queues.
- One memory allocator and one memory budget owner.
- One pipeline cache and one shader module cache, warmed across backends.
- Descriptor pools, staging and upload buffers.
- Swapchains for **both** displays, present and frame pacing.
- Driver loading through adrenotools. The GPU driver manager work folds in
  here rather than standing alone.

**Not shared, and this is the important half:**

- **Render pass and subpass structure.** Tiler-critical. Flattening it spills
  GMEM to system memory. See
  [Vulkan is the substrate](#vulkan-is-the-substrate-and-the-adreno-is-a-tiler).
- **LRZ decisions.**
- Draw translation, guest format conversion, and pipeline state derived from
  guest state.

**Do not build a shared renderer.** Build the layer beneath one. The emulators
then stop creating their own device and take the shared one, which is the
"work backwards" direction.

#### Prior art

`xenia-thor/docs/research/20260517-142224-thor-vulkan-device-baseline.md` is a
measured device baseline, dated 2026-05-17. Board platform `kalama`, Vulkan
instance API 1.3.0, device API 1.3.128, vendor ID `0x5143`, GPU clocks from
680 MHz down to 124.8 MHz, stock driver
`com.qualcomm.qti.gpudrivers.kalama.api33`. It also records the target as the
**Thor Max**.

**Read it before writing any device setup.** The capabilities are already
measured on this hardware.

#### The native contract

[`shared_layer/thor_backend.h`](shared_layer/thor_backend.h) is the C++ half,
drafted 2026-08-22. Nothing implements it; it exists so the architecture can be
argued against something concrete.

What it settles:

- **The backend never creates a device.** It receives `DeviceHandles` and
  `DeviceFacts` and stores them. The queue is shared and carries a lock,
  because melonDS already learned it needs one.
- **The texture key is opaque.** The backend computes it, because hashing a
  texture means hashing guest formats and a guest palette. ARMSX2 and melonDS
  key on data plus palette; Cemu keys on physical address. The shared layer
  must not care.
- **Texture classes are a declared list, not an enum.** ARMSX2 has two,
  melonDS three, Cemu none. A fixed enum would impose one emulator's taxonomy
  on the rest.
- **Every decline reason is separate.** ARMSX2 learned that "nothing got
  upscaled" has half a dozen causes needing different fixes.
- **The upscaler stays outside the cache.** Following ARMSX2 over melonDS,
  because a shared upscaler cannot own seven caches' lifetimes.
- **Resolves and LRZ breaks are reported, not inferred.** They are the most
  common way to lose frames on a tiler.

**The render graph question is now settled enough to act on.** Read on
2026-08-22: **no fork plans render passes.** eden keys its pass cache on
attachment formats and sample count alone, then hardcodes `LOAD_OP_LOAD`,
`STORE_OP_STORE`, one subpass, zero input attachments and no resolve
attachments. Vita3K is also a format-keyed lookup.

A shared render graph therefore **adds** planning nobody has, rather than
replacing tuned structure. That is a much smaller commitment than assumed.

**But do the cheap version first, and one fork already did it.**

**Cemu defaults depth and stencil to `DONT_CARE`** and only promotes them to
load and store when the case needs it. **eden loads and stores depth
unconditionally.** That is the exact optimisation, already written, in one
fork, unknown to the others.

All four read. Ranked by how tiler-correct the attachment operations are:

| Rank | Fork | Colour ops | Depth ops | Notable |
| --- | --- | --- | --- | --- |
| 1 | **Vita3K** | **transient-aware, `DontCare` both ways** | `eClear` / `eDontCare` unless forced | the only correct answer |
| 2 | Cemu | LOAD / STORE | **`DONT_CARE` default** | `GENERAL` layout in its dynamic-rendering path |
| 3 | azahar | `eClear` when clearing, always `eStore` | always stores | 2 dedicated Anime4K passes |
| 4 | eden | LOAD / STORE, unconditional | LOAD / STORE, unconditional | one subpass, fixed |

**Vita3K tracks transient attachments** and nothing else does:

```cpp
.loadOp  = is_color_transient ? eDontCare : eLoad,
.storeOp = is_color_transient ? eDontCare : eStore,
```

That is the design that pairs with `LAZILY_ALLOCATED` memory, since a transient
attachment that never leaves tile memory needs no backing allocation.

**Nobody merges passes. Nobody uses input attachments. Nobody resolves MSAA
on-chip.**

### The propagation list

Every item is already written somewhere in the fleet. None needs invention.

| Take | From | Give to |
| --- | --- | --- |
| Transient colour attachments | Vita3K | everyone |
| Depth and stencil `DontCare` by default | Cemu | eden, azahar |
| `eClear` instead of `eLoad` when the pass clears | azahar | eden |
| `COLOR_ATTACHMENT_OPTIMAL` instead of `GENERAL` | — | Cemu |

**The four-way spread is itself the argument for the shared layer.** Four forks
gave four answers to one question, and the best answer is not in the newest or
most active fork.

Two more findings:

- **Cemu already supports `KHR_dynamic_rendering`**, through
  `InitDynamicRenderingData()` and a `VkRenderingInfoKHR`. Nothing else in the
  fleet does, and dynamic rendering changes what a shared graph would even look
  like.
- **azahar gives Anime4K two dedicated full-screen render passes**,
  `anime4k_xy_renderpass` and `anime4k_luma_renderpass`. On a tiler each is a
  load and a store of the whole target unless carefully arranged. **The
  flagship feature has a known pass cost and nobody has priced it.**

What the header deliberately does **not** settle: **render pass structure stays
with the backend for now**, pending that experiment. THOR_RENDER.md commitment 2 wants a shared render graph that
plans GMEM residency. Taking pass structure from a backend before measuring
would be the exact mistake this project keeps finding in its own plans, and the
FlexRender behaviour means the GPU can leave tiled mode mid-frame regardless.

Five open questions are recorded in the header itself, including who owns the
swapchains when a backend presents two guest screens.

#### Why this ordering is safer than it looks

Building a device layer with no consumer is how an API comes out wrong. The
contract avoided that by falling out of real screens.

Apply the same rule: **bring up the shared device against one backend, on one
real workload, measured.** Then a second. Do not design it for seven.

### What to do next

1. Finish the app shell. Device-free, high value, no duplication risk.
2. **The shared Vulkan device layer**, brought up against one backend. See
   above. The driver manager folds into this rather than being separate.
3. **Prove the packed binary on two backends before committing seven.** One
   toolchain across seven C++ codebases is unproven, and the tiler research
   says a shared render path can be slower than what it replaces.

## Two CPU leads worth chasing

From the Cortex-X3 optimization guide, distilled in
[`hardware_ref/thor/cpu/CORTEX_X3_NOTES.md`](hardware_ref/thor/cpu/CORTEX_X3_NOTES.md).
Both are unmeasured and both are cheap to check.

### Guest FP status flags may be serialising the machine

`NZCV` and `SP` are **fully renamed** on the X3, so guest condition flags cost
nothing. `FPSR`, `FPCR` and `APSR` are **not renamed**. Their reads are
non-speculative and in-order, and some `FPCR` writes carry a flush side effect.

> FPSR/FPSCR reads must wait for all prior instructions that may update the
> status flags to execute and retire.

**Emulators commonly emulate guest FPU status faithfully.** On this core that
is a stall per access. Four forks generate ARM64: ARMSX2, xenia, Cemu and
melonDS. Check what each does with guest FP status and rounding mode. Lazy or
deferred handling could be a large win.

### Spilling to the vector register file beats spilling to memory

> Register transfers between general-purpose registers and ASIMD registers are
> lower latency than reads and writes to the cache hierarchy, thus it is
> recommended that GPR registers be filled/spilled to the VPR rather to memory.

Register pressure is the central problem in a guest-to-host recompiler and the
standard answer is a stack spill. On the X3 the vector file is faster than L1.
Check whether any of the four recompilers does this.

### MEASURED: the Thor has no SVE

From `/proc/cpuinfo`, recorded in xenia's research on 2026-08-05:

```
asimddp  i8mm  bf16  fphp  asimdhp  atomics  lrcpc  ilrcpc  sha3
```

**No SVE, no SVE2 exposed.** Be precise about why: **the ARM cores implement
SVE2, and the shipped SoC does not expose it.** Qualcomm disabled SVE across
this Snapdragon generation. **No compiler flag reaches it** — exposure is a
kernel decision through `CONFIG_ARM64_SVE` — **and it would not be a throughput
win if it were reachable**, because every ARMv9 core here implements SVE at
128-bit vector length, the same width as NEON.

So every SVE section of the Cortex-X3 and A510 optimization guides is **not
applicable**, and so are rpcs3's SVE2 optimisations. See
[`research_log/20260822_2147_sve2_on_the_thor.md`](research_log/20260822_2147_sve2_on_the_thor.md).

What is present and useful: **`asimddp`** gives `SDOT` and `UDOT`; **`sha3`**
gives `EOR3` and `BCAX`, which are nominally crypto but serve as three-input
bitwise operations in guest vector lowering.

**Check `/proc/cpuinfo` before trusting an ARM manual.** A core's guide
describes what the core can implement, not what the vendor shipped.

### `yield` is a no-op on ARM, and it may be costing the fleet dearly

rpcs3 found **half of all CPU time** sitting in a four-line `busy_wait`. On
modern ARM cores `yield` does nothing; the x86 `pause` equivalent is **`ISB`**.

**xenia has exactly that shape.** Its `A64Backend` logs
`clock-spin-yield disabled`, and its own notes record that Burnout's main
thread busy-waits on the GPU ring read pointer, which is why that profile
raises the command-processor thread priority.

**This is host-side, so it transfers to every fork**, unlike the vector items.
Check every spin loop in the fleet for a `yield` that does nothing.

### Feature detection may be silently excluding this device

rpcs3 was gating FMA on the CPU **name** containing "cortex", so **every
Qualcomm and Apple core fell off the fast path** despite FMA being baseline on
ARM64. Their fix detects features properly and passes them to LLVM as target
attributes, so LLVM stops generating conservative code.

**This is a cheap fleet-wide check.** For every fork with an LLVM backend: is
it passing real target features for the Thor — `+dotprod`, `+i8mm`, `+fp16`,
`+sha3` — or is LLVM emitting generic AArch64?

xenia records its own instance: `arm64_fma_v128_fastpath` is **default-off** in
its tree, and it does not know whether that is a correctness problem or a
detection problem. **If detection, it is free performance.**

**Checked fleet-wide 2026-08-22, and most forks leave the features off:**

| Fork | Flags found | Verdict |
| --- | --- | --- |
| **xenia** | `armv8-a+crypto+sha3+crc+dotprod` | **complete** |
| Vita3K | `mcpu=cortex-x3` | tuned to the prime core |
| Cemu | `armv8-a+lse`, `mcpu=cortex-a710` | LSE, tuned to a mid core |
| rpcsx | `armv8-a+lse`, `+crypto` | no dotprod or sha3 |
| **ARMSX2** | `armv8-a`, `+crc` | **baseline** |
| **azahar** | `armv8-a` | **baseline** |
| melonDS | `armv4`, `armv5`, `haswell` | those are **guest** targets |
| eden | none found | |

**xenia is the only fork enabling what the device actually has** — and it is
the fork that wrote the research. The rest compile for a generic ARMv8-A this
device stopped being years ago.

**This produced the fleet standard.** Reading the build files rather than
assuming them: **melonDS is the only fork that sets `-mtune`, and it chose
`cortex-x3` with the reasoning written in the file. xenia is the only fork that
raises `-march`. No fork does both. Cemu, Vita3K and azahar set neither.**
ARMSX2 sets `-march=armv8-a` for Android deliberately while selecting
`armv8.4-a -mcpu=apple-m1` for Apple Silicon, so **it targets an M1 more
precisely than it targets the Thor.**

**eden ships a `YUZU_BUILD_PRESET=armv9` option that sets `-march=armv9-a`. Do
not select it on this device.**

The answer is now written down once in
**[`hardware_ref/thor/THOR_TARGET.md`](hardware_ref/thor/THOR_TARGET.md)**:
`-march=armv8.2-a+crc+lse+fp16+dotprod+sha3+i8mm+bf16 -mtune=cortex-x3`.

**Do not target `armv9-a`.** All four cores are ARMv9, ARMv9.0-A mandates SVE2,
and **this device has no SVE**. A compiler told `armv9-a` may emit instructions
that do not exist here. That is why the baseline forks were not simply being
lazy, and it is why the target names its features explicitly.

**Caveat checked, and it rescues nobody.** Searching the ARM64 backends
themselves:

| Fork | `SDOT`/`UDOT` | `EOR3`/`BCAX`/`RAX1`/`XAR` |
| --- | --- | --- |
| **xenia** | **2 files** | **6 files** |
| Cemu | 1 | 1 |
| **ARMSX2** | **0** | **0** |
| **melonDS** | **0** | **0** |

ARMSX2 and melonDS have baseline flags **and** no emitter support. **xenia is
the only fork using the device's vector features at all.**

**The ARMSX2 case is the notable one.** It is the seed of the shared layer and
holds the most Thor-specific research in the fleet, and it emits neither
dot-product nor three-input bitwise instructions. **The PS2's VU is a vector
unit for 3D maths — dot products are what it does**, and `EOR3` and `BCAX`
collapse the three-input bitwise sequences a VU mask synthesis produces
constantly.

A specific, checkable opportunity in the fork this project cares most about.
**It needs a device A/B, not an assumption.**

### Prefer a load over arithmetic on the mid cores

**The A715 and A710 have three 128-bit load ports but only two 128-bit
arithmetic ports.** So materialising a constant with a load can be **cheaper
than computing it**, which is the opposite of the usual advice.

Reported as a novel finding, and expected to apply across the A7xx line, so to
Android SoCs broadly. It bites at constant materialisation in a recompiler and
at any guest vector sequence that synthesises a mask arithmetically. **Guest
threads land on exactly these cores.**

### One code layout cannot suit all four cores

The four optimization guides give **directly conflicting advice**. Distilled in
[`hardware_ref/thor/cpu/CORE_COMPARISON.md`](hardware_ref/thor/cpu/CORE_COMPARISON.md).

| Core | Branch layout advice |
| --- | --- |
| X3, A710 | at most 4 branches per aligned 32-byte region |
| **A715** | **prefer branches concentrated**: one 32-byte region with two branches beats two regions with one each |
| A510 | at most **one** conditional branch per aligned **16-byte** region |

A715 asks for the opposite of A510. Alignment thresholds differ too: the A510
penalises loads crossing 32 bytes and stores crossing 16, where the big cores
penalise 64 and 32.

**Consequence: tune the recompiler for the X3 and place hot code there.** The
alternative is per-cluster codegen variants, which multiplies the code cache
and the testing.

### The A510 vector unit is shared between two cores

> Cortex-A510 shares a VPU between all Cortex-A510 cores in a complex.
> Instructions being executed on VPU pipelines by one core may reduce
> performance of the instructions executed on the VPU by the other core.

The Thor has three A510s, so at least one pair shares a vector unit and an L2.
**Two vector-heavy threads on a paired A510 contend, and nothing in either
thread reveals it.**

The datapath may also be 2x64-bit rather than 2x128-bit, which would halve
128-bit vector throughput there. **Both are readable from the device**
through `IMP_CPUCFR_EL1.Cores` and `IMP_CPUCFR_EL1.VPU`. Read them before
placing vector work on the little cores.

### The A710 stalls three cycles on lane-assembled vector registers

> a V-pipeline uOP containing more than 1 quad-word register source, a portion
> or all of which was previously written as one or multiple single words, will
> stall in dispatch for three cycles.

**This is exactly what an emulated guest vector unit does.** Writing lanes
individually and then using the whole register is the normal case for PS2 VU,
Xbox 360 VMX128 and DS geometry. Three cycles on first use, on two of the
Thor's cores.

**All these leads belong in the experiment ledger before anyone acts on them.**

## The philosophy: share the hot path, not the periphery

This is the central idea. Everything else follows from it.

**RetroArch shares the periphery. This project shares the hot path.**

| | RetroArch | This project |
| --- | --- | --- |
| The core is | a black box behind a narrow API | modified on purpose |
| The shared layer holds | UI, input, post-process shaders | the Vulkan device, the upload path, the caches, the scheduler |
| It makes a core faster | no, it wraps it | yes, that is the point |
| Breadth | ~200 cores, every platform | 8 backends, one device |
| Therefore | lowest common denominator | maximum specialisation |

RetroArch **cannot** make a core faster. It must accept a core as upstream
wrote it, because it supports hundreds. That constraint is correct for
RetroArch and wrong for us.

We support a small number of backends on exactly one device. That buys the
right to reach inside a core and take a responsibility away from it.

### What this means in practice

The shared layer does not wrap the texture upload path. It **replaces** it. The
backend stops owning the Vulkan device, the memory budget, the thread pool and
the shader cache, and hands each one to the shared layer.

This is why [Foundation](#foundation) sanctions reworking major guts and cores.
It is not licence to rewrite for taste. It is the specific work this philosophy
requires.

### The cost, stated plainly

**The deeper the shared layer reaches, the harder upstream harvesting becomes.**

A backend whose upload path we replaced cannot take an upstream change to that
path. The conflict is not textual; the code being merged no longer has a place
to go.

**Accept full divergence in a hot path we own.** Decided 2026-08-22.

Once the shared layer owns a subsystem, that fork's version is dead and gets
deleted. Upstream changes to that path are ignored on purpose. Do not try to
keep it mergeable; a path kept mergeable keeps the fork's structure, and the
fork's structure is the thing costing frames.

Keep harvesting everywhere else. Divergence is bought per subsystem, not
per fork.

This is a real loss. Some upstream work becomes unreachable. Weigh it before
you take a subsystem, using the tests in
[Depth is decided per subsystem](#depth-is-decided-per-subsystem).

## RetroArch is a source of ideas, not a model

**We diverge from RetroArch on purpose.** Mine it for ideas. Do not copy its
shape.

What to take:

- The separate-distribution pattern, for PS3 only. See
  [How RetroArch handles a GPL-2 core](#how-retroarch-handles-a-gpl-2-core-under-a-gpl-3-frontend).
- Slang shader presets, through librashader.
- Its core list, as a map of which emulator is worth looking at per system.

What to reject:

- **The user experience.** RetroArch is hard to use. That is the stated reason
  this project exists. A person should not need to learn a menu system to play
  a game. See [Foundation](#foundation) point 4.
- **The uniform core API.** One narrow interface for every system costs speed
  and expressiveness. See
  [The contract is thin](#the-contract-is-thin-because-the-cores-differ).
- **Core-per-system-per-fork sprawl.** We ship one tuned backend per system,
  not five choices.

Treat RetroArch as the anti-pattern for usability. If a design decision moves
the app closer to RetroArch's menus, it is wrong.

## Future systems

Wanted, but lower priority than the current fleet. No fork is chosen for any
of these, and none is surveyed.

| System | Notes |
| --- | --- |
| SNES | Several mature emulators. Licences differ sharply. |
| GBA | Several mature emulators. |
| N64 | Mature, but accuracy and speed vary a lot per emulator. |
| Sega Genesis | Mature. Licence is the main risk. |
| Dreamcast | One dominant Android-capable emulator. |
| Sega Saturn | Hard to emulate. Fewer options. |
| PSP | Mature, and already Android-native. |
| PS4 | Early. Expect it to be immature. |
| Xbox, original | Early on desktop. Not proven on ARM64. |
| Japanese feature phone | For Rockman: Legend of the 5 Islands. See below. |

### Adding a system: licence first, difficulty second

**Check the licence before you evaluate the emulator.** This is now the gate.

- A **GPL-3.0-compatible** core packs into the binary and inherits the whole
  shared layer.
- A **GPL-2.0-only** core becomes another PS3-style exception: an optional
  separate install with no shared device, no shared cache and no shared upload
  path.
- A **non-commercial or otherwise non-free** core cannot be used at all.

Two well-known landmines to verify early: some popular SNES and Genesis
emulators carry custom non-commercial licences rather than the GPL. **Verify
before you invest.** A permissive alternative usually exists for both systems.

### The payoff compounds

Each new backend is cheaper than the last. A new system inherits the shared
upscaler, the per-class routing, the GPU driver manager, per-game overrides,
the cheat library and the test harness. It supplies only what its own hardware
knows.

That is the argument for finishing the shared layer before adding systems. Add
a system now and you port everything by hand. Add it later and you write a
contract implementation.

### The Japanese feature phone case

Rockman: Legend of the 5 Islands is a Japanese feature phone game. That is a
different problem from a console. The runtime is a Java profile such as
NTT DoCoMo DoJa or i-appli, not a hardware machine.

Consequences:

- The work is a runtime implementation, not hardware emulation.
- The shared layer gives it almost nothing. There is no texture upload path to
  hook and no GPU to tune.
- Treat it as a separate project that happens to live in the same app.

Survey what exists before assuming this is small. It may be the hardest item
on the list, despite the game being the smallest.

## The driver baseline: pinned Turnip

Decided 2026-08-22.

**The app bundles one pinned Mesa Turnip build, loads it by default, and treats
it as the reference configuration.** A different driver is a per-game override,
not the normal case.

### Why the driver is mandated

This is [Foundation](#foundation) point 1 extended one step. One device, one
graphics API, and now one driver.

- **The test harness needs it.** Golden image comparison is the backbone of
  the QA plan. If a reference frame was rendered on Turnip and the run used
  the stock Qualcomm driver, every difference is driver noise and the
  comparison proves nothing. **Determinism requires a fixed driver.**
- **Turnip exposes more extensions than the stock driver.** With the driver
  guaranteed, the shared Vulkan layer can rely on a feature instead of
  branching on availability. Branching in the hot path is what this
  architecture exists to avoid.
- **One driver, one bug surface.** Two drivers double the test matrix and
  force every measurement to name which one ran.
- xenia-thor already runs `mesa-turnip-v26.3.0-20260803-r7-vulkan-1.4.354-7`.
  This makes existing practice official.

### Choosing the pin

**Provisional pin: `turnip_mrpurple_T30-toasted.adpkg.zip`.** Not measured yet.
Surveyed 2026-08-22.

The Adreno 740 is an **a7xx** part. Only a7xx builds apply.

Candidates, all already on the device:

| Build | Date | Source | Note |
| --- | --- | --- | --- |
| `turnip_mrpurple_T30-toasted` | 2026-08-20 | MrPurple666 | Newest. Changelog states it fixed a7xx support and dropped a710 and a720. |
| `mesa-turnip-v26.3.0-20260803-r7` | 2026-08-03 | Mesa build | What xenia-thor runs today. |
| `Turnip_v26.0.0_R8` | 2026-05-10 | K11MCH1 | Widely used and community tested. |
| `turnip_mrpurple_T29-toasted` | earlier | MrPurple666 | Superseded by T30. |
| `Turnip_v26.0.0_R8_Sysmem` | 2026-05-10 | K11MCH1 | **Sysmem variant.** Forces system memory rendering instead of GMEM tiling. Expect it to be slower; it exists to work around bugs. See [Vulkan is the substrate](#vulkan-is-the-substrate-and-the-adreno-is-a-tiler). |

**Do not pin from reading a changelog. Measure.** Three candidates and a
measurement harness already exist. Run an A/B across T30, v26.3.0-r7 and
v26.0.0_R8 on the same scene, and pin the winner. State watts and temperature,
not only frames. See
[AI-driven development, QA and experiments](#ai-driven-development-qa-and-experiments).

### Wrong-target drivers present on the device

`/sdcard/Android/data/dev.eden.eden_emulator.nightly/files/gpu_drivers/`
contains `Turnip_Gen8_V33.zip` and `a8xx-gen8-V24.zip`, and
`/storage/emulated/0/Download/` contains `Turnip_Gen8_V33.zip`.

**These target a8xx, the Snapdragon 8 Elite generation. The Thor is a740,
which is a7xx.** They are the wrong part. Do not ship them, do not test with
them, and treat their presence as a reason for the driver manager to validate
the target GPU before it offers a build.

That validation is a concrete job for the shared driver manager, and it is
something no fork does today.

### Three rules that make it safe

1. **Pin the version. Do not track the latest.** Mesa moves quickly and a
   Turnip regression would break the app silently. Upgrade the baseline
   deliberately, with a measured before and after. Never automatically.
2. **Bundle it. Do not ask the user to find one.** Driver hunting is the kind
   of toil [Foundation](#foundation) point 4 forbids. The app ships the driver
   and loads it through adrenotools. Mesa is MIT licensed, so bundling inside
   a GPL-3.0 app is clean. Confirm the licence of the specific build before
   shipping it.
3. **Keep an escape hatch as a per-game override.** Every option is
   overridable per game, and the driver is an option. Turnip is not
   universally faster; some games regress on any given build. A different
   Turnip, or the stock driver, is selectable per game and carries a warning
   that it leaves the tested configuration.

   **Found 2026-08-22 while wiring the settings screen to the contract: the
   driver override needs a process restart, and nothing said so.** adrenotools
   loads one driver at process start, so a packed binary holds exactly one for
   its lifetime. The per-game *choice* is real; the per-game *effect* is not,
   until the process restarts.

   So the driver is a **`PROMOTED`** setting with `liveChangeable = false`. See
   [Track A](#track-a--the-ui-shell-defines-the-contract). **Offering it per
   game without a restart is ARMSX2's PINE bug in different clothes: the switch
   moves and nothing happens.**

### What this changes

- **The GPU driver manager gets smaller and sharper.** It stops being a
  browser for drivers and becomes: verify the pinned driver loaded, expose the
  override, warn when the configuration is off baseline.

  **`rpcsx` `GpuDriverAdvisor.kt` already does the hard part.** It returns a
  verdict of `INCOMPATIBLE`, `RISKY` or `COMPATIBLE`; `deviceTarget()` reports
  `a7xx` and `Adreno 740` on the Thor; and `claimedFamilies()` recovers the
  target family from a package name, including Qualcomm "Gen N" marketing.
  It also states honestly that this is a heuristic, because AdrenoTools
  metadata carries no target-GPU field. **Take it, do not rewrite it.**
- **Every performance number states the driver build.** xenia's scripts
  already do this. It becomes a fleet rule.

### The cost, accepted

**Mandating a driver means inheriting its bugs.** If the pinned Turnip breaks
one game, that is ours to work around. Upstream Mesa will not prioritise this
handheld.

Therefore the stock driver path must stay **functional**, not merely present.
The shared Vulkan layer must not hard-depend on a Turnip-only extension
without a fallback for the cases where a user has to switch.

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

## The fleet is already sharing code, badly

**Emulators have copied each other for eighteen years. The sharing already
happens; it happens in the worst possible way.**

From copyright headers in the forks themselves, 2026-08-22. Full evidence in
[`shared_layer/ANCESTRY.md`](shared_layer/ANCESTRY.md).

| Fork | Foreign code it carries |
| --- | --- |
| **Vita3K-Thor** | Dolphin 2013 x6, Dolphin 2016 x2, Citra x3, yuzu x2 |
| **melonDS-android** | **Dolphin 2008 x8, Dolphin 2009 x3** |
| **eden-thor** | yuzu, **over 2,000 files** |
| **azahar-thor** | Citra, hundreds |
| ARMSX2 | PCSX2, wholesale |
| rpcsx-ui-android | rpcs3, wholesale |
| Cemu-thor, xenia-thor | none found |

**melonDS-android carries Dolphin code written in 2008.** Vita3K carries code
from three different emulators.

### Why this matters more than the duplication itself

**The alternative to unification was never "everyone writes their own".** It is
what actually happened: everyone copies once, then diverges forever, and
**nobody receives the fixes**.

Vita3K took Dolphin's touch overlay in 2013. Dolphin has improved it for twelve
years since. Vita3K has none of that. azahar took the same code through Citra
and diverged separately, so two copies of one file are now 1302 lines of Kotlin
and 1067 lines of Java.

**Informal copying gives you the initial value and none of the compounding.**

The question was never whether emulators should share code. They already do, at
scale, across a decade. The only question is whether the sharing is **tracked
and maintained** or **copied and abandoned**. That difference is what
[`capability_inventory.md`](capability_inventory.md), `OWNED.md`, the build
guard and the provenance rule exist to provide.

### Shared ancestry predicts duplication. Shared purpose does not.

| Looked duplicated because | Result |
| --- | --- |
| Three forks have an LRU cache, same purpose | three different designs |
| Six forks have a driver picker, same purpose | four different concerns |
| Two forks have `InputOverlay*`, **same ancestor** | **one design, twice** |
| Two forks have `DiskShaderCacheProgress.kt`, **same ancestor** | same design |

**Search for shared ancestors, not shared features.** Applied on 2026-08-22 to
Kotlin and Java basenames, it found the largest duplication in the fleet:

**azahar and eden share 90 files by name**, of which **40 are a typed settings
framework**: `AbstractSetting` and its typed subclasses, plus `BooleanSetting`,
`FloatSetting`, `SliderSetting`, `SwitchSetting`, `SubmenuSetting`,
`StringInputSetting`, `DateTimeSetting` and `HeaderSetting`, each with a
matching view holder. Plus six view models and a working frontend around them.

**That is the settings schema the backend contract needs, already built,
twice.** `Backend.kt` defines `SettingSpec` with a type enum and a stable key;
these two forks already ship exactly that design. **Do not design a settings
framework. Take theirs.** azahar is GPL-2.0-or-later, the more permissive
source.

It was invisible to every feature-based search and took one filename comparison
to find.

**Measured drift, and it tempers this.** Diffing the shared settings files
showed the code has fully diverged: `SettingsViewModel.kt` is 11 lines in
azahar and 143 in eden, and every differing-line count exceeds its own file
even with whitespace stripped. **Almost no literal line survives in common.**

So this is a **design** duplication, not a code duplication, and extraction is
a rewrite guided by two references rather than a merge. What survives is still
valuable: the type hierarchy is proven twice, the naming is agreed so the
contract needs no negotiation, and **where the two diverged is where the
original design was under-specified.**

```sh
git -C <fork> grep -hoiE 'Copyright [0-9-]* (Dolphin|Citra|yuzu|PCSX2|RPCS3|melonDS) [A-Za-z]*'   | sort | uniq -c | sort -rn
```

### These forks have upstreams they do not track

melonDS-android carries Dolphin 2008 code. Vita3K carries Dolphin 2013 code.
**Neither lists Dolphin as a remote**, so neither will ever see a fix.

**Checked 2026-08-22, and the cost is concrete.** Dolphin's overlay today still
uses the same four class names it used in 2013 — so does azahar through Citra,
and so does Vita3K. **Three independent codebases, one design, twelve years, no
coordination.** That is the strongest evidence yet that the shape is right.

But the forks are behind on two things:

- **`InputOverlayPointer.kt` exists upstream and in neither fork.** A fifth
  component for pointer input. The Wii IR pointer is its origin, but a stylus
  is the same abstraction and **the DS and 3DS both use one.**
- **Dolphin migrated the overlay to Kotlin.** azahar did too, independently.
  **Vita3K is still Java.**

### All six ancestors checked, and they split in two

| Ancestor | Status | Consequence |
| --- | --- | --- |
| **Citra** | **dead**, shut down March 2024 | **azahar IS the upstream** |
| **yuzu** | **dead**, ceased 2024-03-04 after a $2.4M settlement | **eden IS the upstream** |
| Dolphin | alive, Kotlin migration plus `InputOverlayPointer` | Vita3K is behind |
| PCSX2 | alive, 2.6.3 in Jan 2026; **2.6.0 landed a faster Vulkan path** | ARMSX2 can back-port |
| rpcs3 | alive, **targets ARM64**, Cell CPU optimisation work | rpcsx can back-port |
| melonDS | alive, 0.11.3 in July 2026 | melonDS-android can back-port |

**azahar and eden are not lagging forks. They are the continuation of dead
projects.** So the 90 shared files between them are two surviving descendants
of a dead parent, which makes the shared design **more** valuable: nobody
upstream will ever reconcile them, and no third party will either.
**Extraction between them is the only reconciliation that will ever happen.**

**Two live upstreams are doing work directly on this project's path.** PCSX2
landed a faster Vulkan path in January 2026 and ARMSX2's renderer is PCSX2's.
rpcs3 now targets ARM64 and reports Cell optimisation work, and rpcsx runs
rpcs3 on ARM64.

**Check what an upstream has landed before optimising its fork.** Re-deriving
an existing fix is the exact failure mode this section describes.

**Read 2026-08-22:**

- **PCSX2 2.6.0 landed feedback reads**, binding one texture as both shader
  resource and render target, with reported gains of 596% and 413% on specific
  titles. The headline numbers are D3D12 and irrelevant here; **the technique
  is not.** Vulkan has attachment feedback loops, the PS2 reads render targets
  constantly, and ARMSX2 renders through Vulkan on the Thor. **Read it before
  touching the ARMSX2 texture path.**
- **rpcs3 landed ARM64 Cell optimisations using `SDOT` and `UDOT`**, the
  ARMv8.2 dot-product instructions, plus an SPU recompiler change worth roughly
  5 to 7% on SPU-heavy titles.

  **PS3 is deferred and this still matters.** `SDOT` and `UDOT` exist on the
  Cortex-X3, A715 and A710. Lowering a **guest vector unit** onto ARM64
  dot-product instructions is not an SPU technique; the fleet has three more
  vector units in ARMSX2's VU, xenia's VMX128 and melonDS's DS geometry.

  **rpcsx is GPL-2.0-only so its code cannot be taken. A technique is not
  code.** Ideas cross a licence boundary that code cannot.

**CORRECTION, and the fleet got there first.** Two forks have already done this
cross-pollination and neither was recorded here: xenia
`docs/research/20260805-rpcs3-arm64-optimizations-applicable.md` and Cemu
`docs/research/20260820-rpcs3-arm64-optimizations-for-cemu.md`, 395 lines,
citing **twelve merged rpcs3 PRs by number**. **Cemu's cites xenia's.** The
fleet is already cross-pollinating and already citing itself.

**Whatcookie's numbers are on this exact SoC.** Their test device was an AYN
Odin 2, the same Snapdragon 8 Gen 2, same 1+4+3 layout. Their headline claim,
**theirs and unverified**, is roughly 60% faster at 25% less power. Cemu's
document is explicit that this must not be restated as ours.

**And it refutes the claim above.** Cemu's guest is Espresso, a 750CL
derivative with **no VMX at all**, only paired-singles, so `UDOT` and `SDOT`
have no guest-side counterpart there. Roughly half the rpcs3 list dies at that
line.

**Rule: separate host-side from guest-side before claiming a technique
transfers.** Host-side crosses freely — spin and wait behaviour, timer
plumbing, compiler target features, memcmp shapes. Guest-side crosses only
where the guest ISAs align: xenia's Xenon has VMX and it transfers, Cemu's
Espresso does not and it does not.

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
| Displays | **Two, both internal** |

### Two displays

Read from the device with `dumpsys display` on 2026-08-22. **Both are
`type=INTERNAL`.** Screen-2 is a real second built-in panel, not an HDMI
output.

| | Display 0 | Display 4 |
| --- | --- | --- |
| Name | `Built-in Screen` | `Screen-2` |
| Native | 1080 x 1920 | 1080 x 1240 |
| Landscape | 1920 x 1080 | 1240 x 1080 |
| Refresh | 60 and 120 Hz | 60 and 120 Hz |
| Max luminance | 420 nits | 500 nits |
| Density | 369 dpi | 369 dpi |
| Touch | INTERNAL | **EXTERNAL, and present** |
| Flags | default display | `FLAG_PRESENTATION` |
| Layer stack | 0 | 4 |

Facts that matter:

- **Screen-2 has touch.** A DS bottom screen, a 3DS bottom screen and a Wii U
  GamePad screen are all touch screens. The hardware matches the guest.
- **Screen-2 carries `FLAG_PRESENTATION`.** Android renders to it through the
  Presentation API.
- **Both panels support 120 Hz, and the device is capped to 60 Hz by a user
  setting.** `PRIORITY_USER_SETTING_PEAK_REFRESH_RATE` votes a 60 Hz maximum.
  Check this before you trust any frame pacing measurement.
- The two panels have different maximum luminance, 420 against 500 nits. A
  colour or brightness match between them is not automatic.

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
| **[`hardware_ref/thor/THOR_TARGET.md`](hardware_ref/thor/THOR_TARGET.md)** | **The north star. One compile target, one thermal budget, one thread policy for every fork. Read it before tuning anything.** |
| `hardware_ref/thor/soc/` | Snapdragon 8 Gen 2 documents |
| `hardware_ref/thor/cpu/` | Cortex-X3, A715, A710, A510. [`CORTEX_X3_NOTES.md`](hardware_ref/thor/cpu/CORTEX_X3_NOTES.md) for the prime core, [`CORE_COMPARISON.md`](hardware_ref/thor/cpu/CORE_COMPARISON.md) for where the four cores disagree. |
| `hardware_ref/thor/gpu/` | Adreno 740, Vulkan, driver notes. Includes [`VULKAN_TIPS.md`](hardware_ref/thor/gpu/VULKAN_TIPS.md), the practical rules sheet for getting the most out of this GPU. |
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

## Per-game patches

**We want per-game patches. They are a first-class feature, not a fallback.**

Use them for two jobs:

- **Speed.** A shared optimisation cannot fix a game that fights the hardware
  in its own specific way. Patch the game.
- **Change.** Gameplay tweaks, quality-of-life fixes, unlocks, restored
  content and translations. We already wrote infinite life and super shot
  patches for Star Fox Zero in `cemuThorBuiltin`.

Both use one engine. A patch that changes a game and a patch that speeds it up
differ only in intent, and both need the same format, loader and UI.

**The engine is shared. The patches are per-game data.**

- The patch format, the parser, the applier and the UI belong to the shared
  layer.
- A patch itself is data. It names a game, a version and the bytes to change.

**File mods and code patches are two different features.** This repo conflated
them. Separate them:

| | File mods | Code patches |
| --- | --- | --- |
| What | replace game assets | modify guest code at run time |
| Mechanism | install files, the guest filesystem serves them | assemble and relocate instructions |
| Example | a texture pack, a translation | 60 FPS, infinite health, disable SSAO |

eden's mod manager is **21 lines**: install a file, list a folder, return a
result. That is the right size for file replacement and it should not grow.

**"Patch" means three different things here, and this repo has now conflated
them twice.**

| Meaning | What it does | Where |
| --- | --- | --- |
| **Content patch** | updates, DLC, mods the guest filesystem serves | eden `patch_manager` |
| **Code patch** | modify guest instructions at run time | Cemu `GraphicPack2Patches`, xenia `patcher` |
| **File mod** | replace game assets | eden `mod_manager` |

eden's `patch_manager` has `PatchType { Update, DLC, Mod }` and belongs with
file mods. Cemu's `GamePatch.h` is two function declarations about HLE
non-returning functions and is not a patch system at all.

**Rule: never file a capability by the word in its filename.** Three
subsystems called "patch" do three unrelated jobs.

**So there are two code patchers, not three. Choose between them; do not write
a third.**

- **xenia `.patch.toml`.** `src/xenia/patcher/patcher.cc` and `patch_db.cc`,
  with `emit_patch_toml.py` authoring a patch from Ghidra. It already carries
  both intents this repo specified: performance fixes such as 60 FPS or
  disabling blur and SSAO, and cheats such as infinite health.
- **Cemu `GraphicPack2Patches`.** Runtime ASM patching with its own parser,
  bundled with texture and shader replacement.

**Read in full 2026-08-22: 1,331 lines of parser and applier.** The parser
already handles **two input formats**, a legacy Cemuhook one and Cemu's own,
which is the format-and-engine split this repo proposed, already working in
production. It supports `[group]` sections, expressions, syntax errors with
line numbers, and **code caves** through `setOrigin` and `setOriginCodeCave`.

The applier is a relocating linker: `PPCAssemblerReloc` carries a **bit
count**, so it patches individual instruction fields rather than bytes, and it
resolves symbols against a live module by name.

**`ResolvePresetConstant` is the mechanism nobody else has.** A graphic pack
preset feeds a constant into a patch, so **a user-selectable setting becomes a
value the assembler substitutes before relocating.**

That is the missing link between per-game settings and code patches. This repo
specified those as separate features; **Cemu already connects them.** A
resolution-multiplier setting can drive the value a patch writes.

**Cemu's is a symbolic assembler with a linker, and it is not close.**
`GraphicPack2Patches.h` carries a symbol table, a matched `RPLModule`, an error
handler reporting line numbers, and multi-pass resolution: `UNKNOWN_VARIABLE`
is documented as "try again", because a patch may reference a symbol defined
later. Its errors include branch targets out of range and variable conflicts.

**Build the shared patch engine from Cemu's.** xenia `.patch.toml` and ARMSX2
`pnach` are flatter formats and can compile into it.

**Read 2026-08-22, and the split mirrors the cheat finding exactly.** xenia's
patcher is four files, **BSD licensed**, TOML-authored, with flat sized-byte
values and no symbols or expressions.

| | Format, authoring | Engine, execution |
| --- | --- | --- |
| Cheats | six formats, two converters exist | eden's bytecode VM |
| Patches | xenia `.patch.toml`, BSD, Ghidra emitter | Cemu's symbolic assembler |

**Take xenia's TOML as the authoring surface and Cemu's resolver as the
engine.** Both licences work in a GPL-3.0 app, so capability decides the engine
and readability decides the format. Neither needs writing.

**Adopt xenia's `NOTE(thor):` convention.** Its `patch_db.h` marks every place
the fork diverges from upstream, including why: upstream parses with
`tomlplusplus`, this fork ships `cpptoml`. **That is the provenance rule at
line level, and it makes a later re-base survivable.**

**Binding a patch to the right game build** is solved three separate ways, by
three forks, none aware of the others:

| Fork | Method |
| --- | --- |
| Cemu | match a loaded `RPLModule` |
| rpcsx | `PatchHashRepository`, match by hash |
| eden | a 32-byte `BuildID` |

**Three solutions to one identical problem is the strongest signal for a shared
design in the whole survey.** Unlike the LRU caches, these are not different
problems wearing one name; they all answer "does this patch belong to the
binary in front of me". Elsewhere a patch can silently apply to the wrong
build.

Cemu also has the more capable pack mechanism:
`GraphicPack2Patches`, `GraphicPack2PatchesParser` and
`GraphicPack2PatchesApply`, with runtime ASM patching. We already wrote patches
with it, in `bin/graphicPacks/cemuThorBuiltin/`. **Read that before designing a
patch format.** xenia has `GamePatchManager` and eden has `patch_manager`.

Rules:

- A patch states the game id, the version and what it changes.
- A patch states its **intent**: speed, fix, or change. The UI groups them by
  intent, because a person choosing a cheat and a person chasing frames want
  different lists.
- A patch states **why**. A speed patch without a measured reason is a guess.
- A speed patch carries its measurement: the scene, the before number and the
  after number, on the device.
- A patch is a per-game override like any other. The user can turn it off.
- Installing a patch takes one action in the app. See
  [Foundation](#foundation) point 4.

### Ghidra

Use Ghidra when a game needs reverse engineering to find the hot spot.

- Record the finding, not the tool session. Write the address, the function,
  what it does and why it is slow.
- **Do not commit a Ghidra project.** They are large and they are not
  reviewable. Commit the finding and the patch.
- Put the analysis in `console_lab/<console>/`, because it is specific to one
  machine and usually to one game.
- Link the analysis from the patch.

## Skills live in this repo

**Custom skills are local, in `.claude/skills/`.** They are how development
gets faster. Write one whenever a procedure gets repeated.

A skill holds a procedure that would otherwise be retyped: a build recipe, a
flash sequence, a measurement run, an extraction checklist.

Rules:

- Keep the skill in this repo, not in a fork, when it applies to more than one
  fork.
- Keep a fork-specific skill in that fork.
- A skill states its preconditions. Most Thor skills need the device on Wi-Fi
  adb and the charger connected.
- Update the skill when the procedure changes. A stale skill is worse than no
  skill, because it is trusted.

### Skills written so far

| Skill | What it does |
| --- | --- |
| `capability-check` | Answer "which fork already has this?" before any feature work. |
| `thor-measure` | Connect, avoid the traps that produce fake numbers, and record a result that can be trusted. |
| `extract-subsystem` | Prove the duplication is real, pass the licence gate, then the five steps and the build guard. |

Still to write:

**Most of these already exist in `xenia-thor/.agents/skills/`, which holds 29
skills. Port them. Do not write them again.**

| Skill | What it does | Prior art |
| --- | --- | --- |
| `experiment-ledger` | Query before an experiment, record after. | `xenia-experiment-ledger` |
| `evidence-discipline` | No performance number without a captured device file. | `xenia-thor-evidence-discipline` |
| `fork-build` | Build one fork with its recipe and record the time. | `xenia-desktop-build`, `thor_build.ps1` |
| `ghidra-finding` | Record a reverse-engineering result in the right place. | `xenia-ghidra-ooda-loop`, `xenia-thor-ghidra-game-patch` |
| `autonomous-driver` | Build, deploy, launch, capture, classify, commit. | `xenia-thor-autonomous-driver` |

**The fleet is inconsistent about where skills live.** xenia-thor and
Vita3K-Thor use `.agents/skills/`. melonds_HD_2 uses `.claude/skills/`. Use
`.claude/skills/` in this repo. Leave the forks alone until a reason appears.

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

`melonds_HD_2` is dropped **as a target**. `melonDS-android` replaced it. The
last commit to `melonds_HD_2` was 2026-07-12; `melonDS-android` was updated
2026-08-21. Do not add features to `melonds_HD_2`.

**It is not dropped as a source.** It holds `renderer_cases/`, the most
complete test design in the fleet. Harvest it. See
[`capability_inventory.md`](capability_inventory.md).

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

**Write the contract against a pattern, never against one fork's version.**
[`shared_layer/PATTERNS.md`](shared_layer/PATTERNS.md) generalises the fleet
into eight pipelines that every emulator has: code translation, texture
upload, shader translation, memory mapping, presentation, input, state
serialization, and configuration. Each entry names what is shared and what
must stay in the backend.

Read it before designing any contract. The contract for texture upload is not
ARMSX2's `GSTextureUpscaler` with the names changed.

**Order extraction by risk, not by value:**

0. **The touch overlay.** Read 2026-08-22 and it is now the best candidate in
   the fleet. azahar and Vita3K ship the **same four classes from the same 2013
   Dolphin ancestor**, 1302 lines of Kotlin against 1067 lines of Java. Vita3K
   still carries Dolphin's copyright header verbatim.

   A touch overlay has **no guest semantics**, both are GPL-2.0-or-later, and
   the class names already match, so the contract is agreed before anyone
   starts. **xenia has no overlay at all**, so it gains a feature rather than
   losing one, and **melonDS has `TouchVibrator`**, haptics nobody else ships.

   **Cemu's design read, and the comparison resolves.** Its
   `OverlayInputConfig` is a 24-line enum of 21 elements, which is far smaller
   but bakes the Wii U controller in: `BUTTON_ZL`, `BUTTON_ONE`, and
   `BUTTON_BLOW_MIC`, which is not a button at all.

   **Take the generic drawables from the Dolphin lineage and the declared
   element list from Cemu — but declared by the backend, not hardcoded.** That
   is the contract's existing guest input row, reached independently.

   `BUTTON_BLOW_MIC` is the edge case worth keeping: an element declaration
   needs a **kind** as well as a name, because not every overlay element is a
   button.

   **Drift measured 2026-08-22, and the API survived.** Eight method names are
   still shared twelve years after the split: `draw`, `onTouch`,
   `onTouchWhileEditing`, `isInEditMode`, `setIsInEditMode`, `refreshControls`,
   `resetButtonPlacement`, `saveControlPosition`.

   **Those eight are the overlay contract**, not because anyone designed them
   as one but because they survived two independent divergences. The edit mode
   is the notable survivor: two distinct touch paths and an explicit mode flag,
   meaning overlay editing is direct manipulation inside the game view rather
   than a settings screen. Both teams kept that.

   **Drift is not uniform across subsystems.** The settings framework kept a
   type hierarchy and lost every line; the overlay kept its API surface.
   Measure it per subsystem, because it changes what extraction means.

   **Each fork solved a different half, and neither is sufficient alone.**
   azahar added construction and layout: per-orientation defaults, element
   construction, bitmaps, `hapticFeedback` and `swapScreen`. Vita3K added
   lifecycle and state: an auto-hide timer, opacity and scale, input state,
   Android view lifecycle, and **physical controller attach and detach**.

   **That last one matters most here. The Thor has physical controls**, and an
   overlay drawn permanently over a game on a device with real buttons is
   wrong. Only Vita3K solved it, through `attachController`,
   `setAllowVirtualController` and `updateVirtualControllerState`.

   Two corrections: **azahar has haptics too**, so that is two forks not one,
   and **azahar has `swapScreen`** for its dual-screen guest, which is directly
   relevant to the Thor's two panels.

1. The GPU driver manager. **Read on 2026-08-22: it is four concerns, not six
   copies.** Compose the shared version from the fork that does each best.

   | Concern | Take from |
   | --- | --- |
   | Install and storage | any; they agree on ADPKG |
   | Launch wiring, safe fallback | xenia-thor `GpuDriverManager` |
   | Remote catalogue, recommendation | azahar-thor `GpuDriverHelper` |
   | Device capability detection | eden-thor `GpuDriverHelper` |
   | Suitability assessment | rpcsx `GpuDriverAdvisor` |

   See [`research_log/20260822_1945_gpu_driver_manager_read.md`](research_log/20260822_1945_gpu_driver_manager_read.md).

   *The LRU cache was first here and has been removed.* Reading the three
   implementations showed three different designs for three constraints, not
   one structure written three times. See
   [`research_log/20260822_1915_lru_cache_extraction_test.md`](research_log/20260822_1915_lru_cache_extraction_test.md).

2. **Read every implementation before recording a duplication.** Counting
   files with similar names is not evidence of waste. A capability row that
   was never read is a hypothesis.
3. **The driver pipeline cache.** Read 2026-08-22 and confirmed as genuine
   duplication: **all eight forks call `vkGetPipelineCacheData`**. Unlike the
   LRU cache, the shape is fixed by the API and has no guest semantics.

   **Own the driver blob only.** The forks keep a second cache in the same
   files — guest shader source to SPIR-V, keyed on a source hash. That one is
   guest specific and stays with the backend, exactly like texture cache
   hashing. The two differ in a useful way: **the driver blob dies on a driver
   swap and the translation cache survives it**, because SPIR-V is portable.

   **Invalidation is answered by the Vulkan specification**, not by us.
   `pipelineCacheUUID` changes when a driver's caches become incompatible.
   ARMSX2's `VKShaderCache.cpp` already validates header length, header version,
   vendor ID, device ID and UUID. **Do not key on a Turnip build string** — a
   driver can change its compiled format without changing its package name.

   **One shared cache has a cost nobody had priced.** A per-game driver override
   changes the UUID, and with one shared cache that discards the warm cache for
   **every backend at once**, not just that game's. **Name the cache file by
   `pipelineCacheUUID` and keep the last two**, so switching back finds the old
   file intact.
4. Texture upload. The flagship feature lives here.
5. Code translation. Last. It is the deepest reach into a core.

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
- **AGP 9 owns Kotlin. Remove the `org.jetbrains.kotlin.android` plugin.**
  Applying it fails the build outright:

  > The 'org.jetbrains.kotlin.android' plugin is no longer required for Kotlin
  > support since AGP 9.0.

  With AGP 9.x a Compose module needs only `com.android.application` and
  `org.jetbrains.kotlin.plugin.compose`. `rpcsx-ui-android` already runs this
  way. Every fork that still applies `kotlin.android` will fail when it moves
  to AGP 9, and most of the fleet still applies it.

  Found while building the app shell on 2026-08-22. See
  [`app/shell/`](app/shell/).

Unify the Vulkan setup too: one loader, one validation configuration and one
extension set.

### The standard row is incomplete: vendored libraries

| Library | Forks | Vendored by |
| --- | --- | --- |
| **`ffmpeg`** | **5** | Vita3K, eden, xenia, ARMSX2, rpcsx |
| **`cubeb`**, audio | **5** | azahar, Vita3K, Cemu, ARMSX2, rpcsx |
| `vulkan-headers` | 4 | azahar, Cemu, xenia, rpcsx |
| `imgui` | 4 | Vita3K, Cemu, xenia, ARMSX2 |
| `glslang` | 4 | azahar, Vita3K, xenia, rpcsx |
| `xbyak`, `stb`, `glad`, `fmt`, `discord-rpc` | 4 each | |
| `libadrenotools` | 3 | azahar, Vita3K, Cemu |
| `dynarmic`, ARM JIT | 2 | azahar, Vita3K |

**FFmpeg is vendored five times.** It is enormous, and five copies in one
binary is not a size problem but an impossibility.

`discord-rpc` is vendored four times and is Discord Rich Presence. On a
handheld emulator it is weight that probably should not ship.

**This is a hard constraint on the packed binary, not a tidiness issue.** You
cannot link five copies of `cubeb` or five of FFmpeg into one binary. Duplicate symbols do not
merge politely, and four *different versions* of one library is worse than four
copies: the same symbol with different behaviour.

**Dependency unification comes before backend packing.** One `cubeb`, one
`imgui`, one `fmt`, one `glslang`, one `vulkan-headers`, at one version each,
before two backends share a link unit.

Add the chosen versions to the row above as they are decided. See
[`shared_layer/ANCESTRY.md`](shared_layer/ANCESTRY.md).

### The device

Screen-2 hosts `com.android.launcher3/.secondarydisplay.SecondaryDisplayLauncher`
as its own resumed activity. **Screen-2 accepts real Activities, not only a
`Presentation`.** That is a second route for the dual-screen design. Decide
between them before the layout work goes further. See
[`work_log/20260822_1845_shell_build_and_install.md`](work_log/20260822_1845_shell_build_and_install.md).

| Property | Value |
| --- | --- |
| Model | AYN Thor |
| Android | 13 |
| API level | 33 |
| ABI | `arm64-v8a` |
| Hardware | qcom |
| Connection | Wi-Fi adb, port 5555 |

**Wi-Fi adb is the preferred connection, so the cable can come out.**

**A power measurement requires the device to be discharging.** Plugged in,
`dumpsys` reports `status=Charging` and `current_now` flips sign between
consecutive idle samples. Measured values from one idle run: -36988, +225591,
+165897, +224859, -16846 uA. **Any wattage read from a USB-attached session is
fiction.**

Gate every power measurement on `status=Discharging` and refuse to report
otherwise. `xenia-thor/tools/thor/power_affinity_ab.sh` already does this.
Copy the gate.

Wi-Fi adb exists to make that possible: the device stays reachable with no
cable attached.

**A second device is attached to this box.** A Quest 2 answers adb as well. A
bare `adb` command fails with "more than one device/emulator".

**Never run a bare `adb` command. Always pass `-s`.**

**Capture rules learned on 2026-08-22:**

- **`screencap -d` takes the SurfaceFlinger display ID, not the Android
  display id.** `-d 0` and `-d 4` silently write a zero-byte file and report
  success. The Thor's IDs are `4630946441858561667` for the built-in screen
  and `4630946482288158084` for Screen-2. **Treat a zero-byte PNG as a
  failure.**
- **Stream captures with `adb exec-out screencap -p > file`.** Writing to
  `/sdcard` produces a zero-byte file, probably from scoped storage.
- **Git-Bash mangles adb remote paths.** `/sdcard/x.png` becomes
  `C:/Program Files/Git/sdcard/x.png`. Set
  `MSYS_NO_PATHCONV=1 MSYS2_ARG_CONV_EXCL='*'`, and then the local path must
  be Windows-style, because `adb.exe` cannot read `/c/Users/...`.
- **Check what is running before foregrounding anything.** The Thor is a
  device somebody uses. Starting an activity interrupts a game in progress.

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
- Record the battery level and the charge state with every measurement.
- **Watts, not only frames.** The stated target for xenia on this device is
  about 5 W and 50 C. Throughput alone answers the wrong question on a
  handheld. A change that holds fps and lowers temperature is a win.
- **State the expected signature before the run.** Name what the numbers
  should do if the change works. A run with no prediction cannot fail.
- **Run for 15 minutes or more when heat matters.** Thermal behaviour only
  settles over a long run. A short run measures a cold device, which is not
  how anybody plays. This follows Google's ADPF guidance.
- **Measure without ADPF first.** Establish the baseline before adding hint
  logic, or the hint is tuned against an unknown.
- **Cross-run comparison is untrustworthy.** Scene complexity swings several
  times a second, so two separate runs are not comparable. Use an in-place
  alternating A/B inside one run, on a busy frame.
- **`CONFOUNDED` is a verdict.** A number that cannot be trusted gets labelled,
  not discarded and not promoted to a win.
- **Temperature proves the run happened.** No heating means an idle or menu
  scene, so the run is invalid whatever the counter said.
- **Query the experiment ledger before running anything.** See
  [The experiment ledger](#the-experiment-ledger).

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

### 4. Cheats

`azahar-thor/cheat_sources/` holds Sharkive, CTRPF-AR-CHEAT-CODES and
citra-games-wiki. `ai_cheat_helper_switch` is a separate AI-driven method.
Unify the format. Do not unify the sources.

**Read 2026-08-22: eden's cheat engine is a virtual machine, and its host
interface is six calls, none of them Switch-specific.**

```cpp
MemoryReadUnsafe(addr, data, size);   MemoryWriteUnsafe(addr, data, size);
HidKeysDown();                        PauseProcess();  ResumeProcess();
DebugLog(id, value);                  CommandLog(data);
```

**Every backend in the fleet can implement those.** So a shared cheat engine is
the VM shared and the callbacks per backend, which is the same split as every
other pipeline: shared algorithm, backend supplies guest knowledge.

It generalises by accident, because a cheat VM has to be abstracted from the
machine anyway.

azahar solves the **other** half: `CheatEngine` with `CheatBase`, add, remove,
update and a `shared_mutex`. That is the better model for **managing** a cheat
list; eden's is the better model for **executing** one. **Take both.**

**Three architectures, forming a ladder of expressiveness:**

| Fork | Model | Expressiveness |
| --- | --- | --- |
| rpcsx | one typed write at an offset, `cheat_info` | data |
| azahar | polymorphic cheat objects | behaviour |
| eden | bytecode virtual machine | programmable |

**Most cheats are just typed pokes.** So the shared engine is **tiered**: a
fast path for the flat case, falling back to the VM only when a cheat needs
conditions, loops or button state. These are not three competing designs; they
sit at different points on one axis and a shared engine needs the whole axis.

**The format problem is separate from the execution problem.** At least six
formats exist: `mch`, `pnach`, `ncl`, Cemu graphic packs, Atmosphere `dmnt` and
3DS AR codes. A VM does not care — a per-format front end compiles them to one
bytecode, turning six engines into six small parsers.

**Two forks already convert formats**, so this works in practice: rpcsx
`ArtemisConverter.kt` and Vita3K `convert_vitacheat.py`.

**Vita3K also has a content path resolver, and nobody else does.**
`util/cheat_paths.h` enumerates roots, builds candidate paths and resolves one,
searching **nine locations** for a single title: app-private storage, internal
storage, SD cards, and three separate community conventions.

**That is the real Android problem.** A person's content is wherever they put
it, or wherever the guide they followed said to. Every other fork assumes one
path.

**It generalises past cheats.** The app needs the same resolver for HD packs,
mods, translations, saves and ROMs. Take the shape: enumerate roots, build
candidates, resolve, report which won.

**The library badge falls out of it.** Vita3K shows a `C` badge when a match
exists, so the badge is the resolver's result rather than a separate feature.
That answers a question `app/SCREENS.md` left open.

**Still unverified, and it decides the design:** whether `pnach` and AR codes
map cleanly onto `dmnt` bytecode.

**rpcsx is GPL-2.0-only.** Take the idea, never the code.

### 5. Mod and translation loading

Existing projects: `shin_2_eng`, `smt_if_eng`, `ever_oasis_mod`,
`radiata_stories_ending_mod`, `wild_arms_5_the_last_loop_mod` and
`toyko_xanadu_vr_etx`.

### 6. The game library and per-game overrides

This is a requirement, not an option.

**Every game must accept a custom override for every option that can have one.**
A user opens one game and changes one setting for that game alone. This applies
to every backend, not to a chosen few.

**The exception is real and ARMSX2 found it the hard way.** Some settings are
**structurally process-wide**: one server, one loaded GPU driver, one device.
ARMSX2's PINE server is one instance for the whole process, so its per-game file
**refuses the key**. Toggling it from the in-game menu, which saves in game
scope, wrote it **nowhere**, and it read as enabled until the process restarted.

So a setting declares a **scope**:

| Scope | Behaviour |
| --- | --- |
| per-game | the normal case. Sparse, and sticky once set |
| **promoted** | edited in game scope, **written to global**, because the per-game tier cannot hold it |
| global-only | not offered per game at all |

**Promote by copying the field onto global, never by saving the resolved
object**, or every per-game value leaks into the global layer.

**Settings need versioning, and melonDS-android says how.** ARMSX2 carries seven
ad-hoc one-time migration keys; melonDS-android has a real framework, 37 files,
16 concrete migrations from `Migration6to7` to `Migration40to41`. Its interface
is three members: `from`, `to`, `migrate()`. The runner refuses a duplicate
`from`, sorts, runs those in range, then records the new version.

Three points to take:

- **The schema version is the app's own version code**, read with
  `getLongVersionCode`, with a floor for installs that predate migrations.
  **There is no separate number to forget to bump.**
- **Freeze the old data shapes.** Its `legacy/` package holds `Rom21`, `Rom22`,
  `RomConfigDto25`, `RomDto31` and more. **A migration must never deserialize
  with the current class**, because the current class keeps changing and the
  migration then breaks retroactively — silently, only on upgrade from an old
  version, which is the hardest case to test.
- **A migration is tiny and states its reason.** `Migration40to41` is six lines
  of code and four lines of why.

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
- How much space the game uses in total. See
  [Storage and cache visibility](#8-storage-and-cache-visibility).

The library is one list across every backend. It is not one list per emulator.

### 7. Guest accounts and guest system UI

**Two different things, and only one of them was designed.**

| | Host per-game profile | Guest user account |
| --- | --- | --- |
| What | our settings override | who the console thinks is playing |
| Owned by | the app | the guest OS |
| Examples | upscale factor for this game | Mii, NNID, PSN id, gamertag, Switch user |

They are orthogonal. See
[The game library and per-game overrides](#6-the-game-library-and-per-game-overrides)
for the first. This section is the second.

Three forks have a guest account system, each different:

| Fork | Implementation |
| --- | --- |
| Cemu-thor | `src/Cafe/Account/Account.cpp`, `Account.h`, `AccountError.h` |
| azahar-thor | `MiiSelector.kt`, `MiiSelectorDialogFragment.kt`, `mii_selector.cpp` |
| eden-thor | `ProfileAdapter.kt` |

**CORRECTION: rpcsx has one.** `overlay_user_list_dialog.h` carries a
`user_list_entry` with username, user id and **avatar path**, and a `show()`
taking a title, a focused index, a list of user ids and an on-close callback.

**That is the `RequestUserSelect` applet drafted in
[`shared_layer/thor_backend.h`](shared_layer/thor_backend.h), already built**,
with an avatar path the draft omitted. The earlier search used
`profile|account|gamertag|mii|nnid|xuid|npid` and matched none of it.
**Fourth time a negative result was a narrow search.** rpcsx is GPL-2.0-only:
take the shape, not the code.

**CORRECTIONS: those two were wrong as well. Guest accounts exist in six of
eight forks**, found by searching more broadly:

| Fork | Implementation |
| --- | --- |
| Cemu | `Cafe/Account/Account.cpp` |
| azahar | `MiiSelector`, `mii_selector.cpp` |
| eden | `ProfileAdapter.kt` |
| rpcsx | `overlay_user_list_dialog.h` |
| **xenia** | **`kernel/xam/profile_manager`, plus a `create_profile_ui`** |
| **Vita3K** | **`NativeUser`, `UserRepository`, `UserManagementScreen`, `UserManagementViewModel`** |

xenia's lives in the Xbox 360 system API layer and includes profile creation.
Vita3K's is a full Android MVVM stack.

**Six of eight. Guest accounts are core, not an edge case**, and this repo
called them a minority feature for two days. **That is the fifth and sixth time
a negative result was a narrow search.**

**One negative held:** xenia has no touch overlay, confirmed by a second search
using `touch`, `virtual pad`, `onscreen`, `screen control`, `softkey` and
`gamepad view`. **A negative is only worth recording after a second search with
different words.**

**rpcsx also has a complete in-game menu framework**, inherited from rpcs3:
`HomeMenu` with 16 files covering a main menu, a **page** abstraction deriving
from `list_view`, shared **components**, cheats, savestate, settings and a
message box, plus Network, Shaders, Trophies, FriendsList and video playback
inside the overlay.

**That is `app/SCREENS.md` screen 3 already built**, as a page-and-component
framework rather than a pile of one-off dialogs. It also has trophies, a
friends list and overlay video, none of which the screen list considered.

### Guest system applets: UI the guest asks the host to show

azahar's Mii selector is an **applet**. The guest OS calls out and expects the
host to present a picker, then hands back a result.

**This is a whole category the screen list missed.** Every console in the fleet
has some of it:

- profile and account pickers
- software keyboards
- avatar and Mii selectors
- error and system dialogs

**In a unified app these must be rendered by the app, in the app's style.**
Seven backends each drawing their own system dialogs is exactly the
inconsistency this project exists to remove, and a person should not be able to
tell which backend asked.

**Contract consequence.** A backend needs a way to request host UI and receive
a result. That is not in
[`shared_layer/thor_backend.h`](shared_layer/thor_backend.h) and it should be.
It is a small interface with a large consistency payoff:

- request a text entry, with a prompt and constraints, receive a string
- request a user selection, receive a user id
- report an error the guest raised, receive an acknowledgement

**Survey before designing it.** azahar has a working applet implementation and
it has not been read.

### 8. Universal hotkeys, save conventions and control overlays

**One hotkey set works on every system.** This is a requirement, not a
convenience. Save state, load state, fast forward, rewind, screenshot, overlay
and menu use the same input on every backend, always.

A backend does not get to define its own hotkey. The app owns the hotkey layer
and tells the backend what happened.

**ARMSX2 already has the action list and the binding interaction. Take them.**
`ControllerMappings.SysHotkey` is an enum, and `HotkeysTab.kt` renders from
`SysHotkey.entries`: menu, quick save and load, slot cycle, texture-dump toggle,
fast forward, resolution up and down, achievements, close game. Binding arms on
a row tap and captures the next controller button through
`ControllerMappings.captureHotkey`, so it needs no keyboard and no key names.

**It was split out of the Pad tab so hotkeys are "easy to find".** That is this
project's own usability complaint, fixed independently inside the fleet before
this repo existed.

### 9. Storage and cache visibility

**Look at a game and see where its space went.** One view, every system, every
category.

Storage is finite on a handheld. Emulator caches grow without limit and hide
in paths a person never sees. Today a person cannot answer "why is this game
using 14 GB" without a file manager, which
[Foundation](#foundation) point 4 forbids.

### The categories

The per-game view breaks space down by category:

| Category | Notes |
| --- | --- |
| Game data | The dump or install itself. |
| Saves and states | Small, and the only category that is irreplaceable. |
| HD texture packs | Often the largest single item. |
| Texture cache | Upscaled and hashed textures. Rebuildable. |
| Shader and pipeline cache | Rebuildable, at the cost of stutter. |
| Recompiled code cache | Translated guest code. Rebuildable. |
| Mods and patches | Small. |
| Cheats | Small. |
| Screenshots and captures | Grows quietly. Often the easy win. |

Recompiled code caches exist across the fleet under different names. xenia and
Cemu translate PowerPC to ARM64. ARMSX2 translates MIPS. Each keeps a cache,
and none of them reports its size to a person.

### The rule that matters

**A cache is an asset, not junk.** Deleting a shader cache frees space and
brings the stutter back. Deleting a recompiled code cache costs the next boot.

The UI must state the cost before the action:

> Clear the shader cache. Frees 2.3 GB. Shader stutter returns until the cache
> rebuilds.

Never offer a "clean up" button that treats every category the same. Sort by
what is rebuildable, and never put saves and states near a bulk action.

### The contract

The backend declares its storage categories, their paths and whether each is
rebuildable. The shared layer does the accounting, the display, the sorting
and the actions.

This is a per-backend extension over the minimum contract. See
[The contract is thin](#the-contract-is-thin-because-the-cores-differ). A
backend that declares nothing still shows its game data and saves, because the
app owns those paths.

Also show the totals across the whole library, sorted by size. The first
question is usually "which game should I delete", not "how big is this one".

### 10. Dual-screen routing

**The Thor has two internal touch displays. Three systems in the fleet are
dual-screen. No other frontend can do this properly.**

| System | Guest screens | Fits the Thor |
| --- | --- | --- |
| DS, melonDS-android | 2 screens, the lower one touch | Directly |
| 3DS, azahar-thor | Top, and a lower touch screen | Directly |
| Wii U, Cemu-thor | TV, plus a GamePad touch screen | Directly |
| Everything else | 1 screen | Screen-2 is free for other use |

This is the strongest hardware-specific feature available to this project. It
cannot be copied by a frontend that targets many devices, because most devices
have one screen.

### The routing contract

A backend declares its guest screens. It does not decide where they go.

Each guest screen declares:

- A name, such as `top`, `bottom`, `tv`, `gamepad`.
- Its native size and aspect.
- Whether it accepts touch.
- Whether the game needs it. A DS game always needs both. A Wii U game may not
  use the GamePad screen at all.

The app owns the routing. The user picks a layout per game, and the choice is
a per-game override like any other. See
[The game library and per-game overrides](#6-the-game-library-and-per-game-overrides).

**Read 2026-08-22: three forks already ship this. Do not design it.**

**ARMSX2 `SecondScreen.kt` is 576 lines plus 131 of `SecondScreenTiles.kt`, and
it names the Thor.** It is not a guest screen router; it is an app panel on the
second display, which is the other half of
[Screen-2 when the game is single-screen](#screen-2-when-the-game-is-single-screen).
**It carries three lessons `app/shell/` needs and does not have:**

- **Do not put Compose inside a `Presentation`.** A `Presentation` has its own
  Window and decor view, and a `ComposeView` needs the ViewTree lifecycle and
  saved-state owners attached to that decor view first. Get it wrong and it
  **throws at inflate time, on hardware almost nobody testing this has.**
- **A `Presentation` is not torn down when the activity stops.** ARMSX2 shipped a
  bug where the panel stayed up with a stale FPS reading while the user was
  elsewhere. Drive it from `onResume` and `onPause`.
- **Re-attach on every resume, not only on a foreground change**, because a
  dual-screen handheld lets a person move the app to the other display.

It also registers a `DisplayManager.DisplayListener` for add and remove.

azahar `display/ScreenLayout.kt` has a **`SecondaryDisplayLayout` with eight
modes**: `NONE`, `TOP_SCREEN`, `BOTTOM_SCREEN`, `SIDE_BY_SIDE`,
`REVERSE_PRIMARY`, `ORIGINAL`, `HYBRID`, `LARGE_SCREEN`. Plus a primary
`ScreenLayout`, a `SmallScreenPosition` with eight positions, a separate
portrait layout, and **layout cycling by hotkey** on the Qt side.

melonDS models the Thor's exact case:

```kotlin
enum class DualScreenPreset {
  OFF, INTERNAL_TOP_EXTERNAL_BOTTOM, INTERNAL_BOTTOM_EXTERNAL_TOP,
}
```

with per-display alignment, insets and a background mode.

| This repo designed | Already exists |
| --- | --- |
| `ONE_EACH` | melonDS `INTERNAL_TOP_EXTERNAL_BOTTOM` |
| `SWAPPED` | melonDS `INTERNAL_BOTTOM_EXTERNAL_TOP`, azahar `REVERSE_PRIMARY` |
| `BOTH_MAIN`, `MAIN_ONLY` | melonDS `OFF`, azahar `ENABLE_SECONDARY_DISPLAY` |
| — | `SIDE_BY_SIDE`, `HYBRID`, `LARGE_SCREEN`, alignment, insets, hotkey cycling |

**Both are richer than what was designed here.** Take melonDS's
internal-and-external model and azahar's layout set.

A design lesson sits in an azahar comment: `NONE` was removed from the
interface and replaced by a boolean `ENABLE_SECONDARY_DISPLAY`, the enum value
kept only for compatibility. **They shipped it, learned, and changed it.**

**The hardware is still the differentiator. The software is not.** Two forks
already route guest screens to a secondary display.

**Touch must follow the screen.** If the DS lower screen renders on Screen-2,
touch on Screen-2 must reach the guest lower screen. A routing change that
does not move the touch mapping is a bug.

### Screen-2 when the game is single-screen

Most systems have one screen, which leaves Screen-2 free. Useful content:

- The cheat list, live and toggleable while playing.
- The performance overlay: FPS, frametime, temperature, charge.
- A map, a guide or notes for the game.
- The storage view for that game.

**Do not render an idle Screen-2 at full rate.** A second panel drawn every
frame costs power and thermal headroom for no benefit. Draw it on change, and
state its cost in any performance measurement.

### 120 Hz is available and currently disabled

Both panels support 120 Hz. The device is capped to 60 Hz by a user setting.
Any frame pacing or refresh rate work must check the current cap first, or it
will measure the setting rather than the hardware.

### Vulkan is the substrate, and the Adreno is a tiler

Every Tier 1 fork renders through Vulkan on Android. A Vulkan interop contract
is the only foundation that exists across all of them. This makes a rework of
the render path and the present path worthwhile.

**The Adreno 740 is a tile-based deferred renderer. Design the shared render
path for that, or it will be slower than the paths it replaces.**

Facts that constrain the design:

- **GMEM is on-chip tile memory.** Rendering goes to a tile in GMEM, then
  resolves to system memory. A resolve that was not needed is wasted
  bandwidth, and bandwidth is the budget on a handheld.
- **Render passes and subpasses are the lever.** Several passes can stay in
  GMEM. This is the main reason Vulkan beats GL here, and it is lost if the
  shared path flattens the pass structure.
- **LRZ, low resolution Z, rejects occluded fragments early.** Since the
  Adreno 650 it survives across render passes when depth is stored and loaded
  again, and its state can be reused between passes. The 740 supports this.
- **Direct GMEM access extensions arrive at the Adreno 840.** The 740 does not
  have them. Do not design around them.

Consequence: **the shared render path must preserve each backend's render pass
structure and its LRZ reuse.** A shared abstraction that reorders passes, or
that forces a store and load where a backend kept data in GMEM, gives back
more than it gains.

xenia-thor should lead this work. Its `bd_gmem_ab.sh`, `bd_lrz_census.sh` and
`bd_vrs_*` scripts already measure this layer, and no other fork has touched
it. See [`capability_inventory.md`](capability_inventory.md).

Open question, unmeasured: does a shared path keep LRZ reuse? Answer it before
taking the render path into the shared layer.

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

7. **Differential testing against a reference implementation.** Run the same
   work through both, compare, and stop at the first divergence. For a CPU
   that is the interpreter against the recompiler, which finds a bug at the
   exact instruction rather than at the crash. For a GPU it is the software
   renderer against the hardware renderer.

   In the fleet, **both forms already exist**:

   - **GPU form:** melonds_HD_2 `renderer_cases/` stores expected frames for
     the software renderer and for each hardware path.
   - **CPU form:** ARMSX2 `tests/ctest/core/recompilers/`. A `StateSnapshot`
     captures register state for **both** CPUs, `R3000A` and `R5900`, plus a
     fixed-size memory window. Around it sit generated `autocases_` suites for
     EE cache, EE load/store, FPU overflow, IOP, and vector unit macros,
     branches and latencies, plus ARM64 basic-block linking tests.

   This repo recorded that nothing did the CPU form. **That was the third such
   claim to be wrong.**

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

### The experiment ledger

`xenia-thor` already has one: `tools/exp_ledger.py`, a SQLite database, driven
by `.agents/skills/xenia-experiment-ledger/SKILL.md`.

```
python tools/exp_ledger.py check "<keyword>"   # BEFORE any experiment
python tools/exp_ledger.py add "<lever>" "<category>" "<verdict>" ...
python tools/exp_ledger.py dead [category]     # the do-not-retry list
python tools/exp_ledger.py wins                # the shipped stack
```

Verdicts: `DEAD`, `FLAT`, `WIN`, `GFX-LOSS`, `CONFOUNDED`, `OPEN`.

**This is the anti-duplication mechanism for experiments.**
[`capability_inventory.md`](capability_inventory.md) stops a feature being
rebuilt. The ledger stops a dead lever being re-run. The fleet needs both, and
the ledger already exists. Adopt it rather than writing one.

### Expect maintenance wins from the shared layer, not large frame wins

**A caution recorded before the work starts.** xenia's ledger holds a standing
conclusion:

> BD's gap is HLE-vs-LLE, proven by RE2 Remake running on the same Thor via
> GameNative/DXVK. xenia EMULATES the 360 GPU (slow); the fix is TRANSLATING
> D3D9->Vulkan like DXVK. Every incremental GPU lever is DEAD/FLAT because it
> patches the emulator instead of replacing it.

Much of the shared layer is incremental: a shared device, shared caches, a
shared upload path. xenia measured many levers of that kind and recorded them
`DEAD` or `FLAT`.

This does not cancel the shared layer. Two things stay true:

- Duplication costs maintenance whatever it costs in frames. Six driver
  managers is six bugs.
- Several shared items are not GPU levers at all: the driver baseline, the
  test harness, per-game overrides, dual-screen routing, storage visibility,
  cheats and patches.

**Set the expectation now.** The shared layer buys maintainability, features
and consistency. Where it buys frames, prove it per subsystem against the
ledger. Do not promise a large speedup from a shared renderer.

The larger speed lever, on xenia's evidence, is architectural: translate the
guest API rather than emulate the guest GPU. `xbox360-d3d-hle-recomp` records
that direction, dated 2026-07-02.

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

### Two tracks run in parallel

Track A is Kotlin and has no native dependency. Track B is the native work.
They do not block each other. Run both.

| | Track A: app and contract | Track B: native and shared layer |
| --- | --- | --- |
| Depends on | nothing | the toolchain |
| Language | Kotlin | C++ |
| Output | the UI shell and the backend contract | the packed binary and the shared layer |

They meet when the first backend is wired to the shell.

### Track A — the UI shell defines the contract

**Build the shell before reading more emulator code.** The shell is not a
mockup. It is how the backend contract gets discovered, and its output is a
specification.

**Step 1 is done.** [`app/SCREENS.md`](app/SCREENS.md) lists 14 screens and,
for each, what it needs from a backend. The contract falls out of that last
column and is drafted at the end of that file.

Do it in this order:

1. **List the screens.** Done. See [`app/SCREENS.md`](app/SCREENS.md).
2. **Build the shell with fake data.** Navigable, on the device, both
   displays. No emulator behind it.
3. **Pin the settings schema.** Every setting gets a stable key, a type, a
   default, an owner and a **scope**. See the scope rule below.
4. **Pin the per-game override resolution.** **Take ARMSX2's `ConfigStore`.**
   It is 240 settings fields with `merge`, `diff` and two storage tiers, and it
   already hit three bugs the naive design does not prevent:

   - **Sparse is not enough; an override must be sticky.** Storing only what
     differs from global cannot tell "the user set this and it matches global"
     from "the user never touched it". ARMSX2's reported symptom: set cheats on
     per game while global was also on, turn global off, **the game silently
     lost its setting**. The fix pins a field once overridden.
   - **Whole-object writes make a pinned value stale.** Pinning causes this, so
     pinning needs change-tracking. ARMSX2's symptom: **a per-game FPS cap of 30
     came back as 0 and stayed 0**, surviving even after the writers were fixed.
     The fix passes the previous state and trusts the update only for keys it
     proves changed.
   - **Some settings cannot be per-game at all.** See below.

   **A contract that specifies pinning without change-tracking ships the second
   bug.**
5. **Write the contract.** The minimum every backend implements, and the
   extensions a backend may declare. This falls out of steps 1 to 4 rather
   than being argued in advance.

**CORRECTED TWICE on 2026-08-22. Census first, superlative second.** The whole
fleet was finally counted, and both earlier claims were wrong.

| Fork | Frontend | Files |
| --- | --- | --- |
| **melonDS-android** | **78,033 lines** | **698** |
| **ARMSX2** | 63,111 | 153 |
| eden-thor | 33,671 | 217 |
| azahar-thor | 26,919 | 156 |
| Vita3K-Thor | 20,744 | 76 |
| Cemu-thor | 18,501 | 144 |
| **xenia-thor** | **12,334** | **25** |

**xenia has the SMALLEST Tier 1 frontend, not the most complete** — smaller than
melonDS-android by a factor of six.

**melonDS-android is the largest and the only properly layered one**: `domain`
with 120 files, `impl` with 97, Hilt injection, a database layer, and a
**migrations package of 37 files**. ARMSX2 has none of those layers.

**So take the structure from melonDS-android and the features from ARMSX2.**
ARMSX2 holds what this project specifically needs — the per-game override
system, the Thor work, the second-screen panel, the hotkey enum — in 153 large
files with repositories as top-level singletons.

It already implements most of [`app/SCREENS.md`](app/SCREENS.md): library with
cover art, game detail, in-game overlay, the Screen-2 panel, 13 settings tabs
with a **generated search index**, patches, drivers, input and hotkeys,
diagnostics — plus onboarding, achievements, friends, news, BIOS and memory card
managers, a texture pack catalogue with an online section, a shader chain editor,
controller skins, themes and 13 languages.

**Mine ARMSX2's frontend for features and melonDS-android's for structure.**

**And melonDS-android has the cheat manager UI**, which this repo recorded as
missing from the fleet. It is a full stack: `ui/cheats` is 2,119 lines across 20
files, with a game list, a folder list, a cheat list, an enabled-cheats list and
a **cheat editing form**; `domain` carries `Cheat`, `CheatDatabase`,
`CheatFolder`, `CheatInFolder` and **`CheatImportProgress`**; `impl` has Room
persistence, a **streaming XML SAX parser** for cheat databases, and a bundled
database importer.

`CheatImportProgress` is the detail that shows it was used in anger. **A cheat
database is large and importing it is slow enough to need a progress report.**

It also has **`ui/layouteditor`, 2,925 lines** — a screen layout editor, which
is [`app/SCREENS.md`](app/SCREENS.md) screen 11.

**Storage aggregation remains the one screen with no prior art anywhere.**

See [`research_log/20260822_2233_fleet_frontend_census.md`](research_log/20260822_2233_fleet_frontend_census.md).

See [`research_log/20260822_2203_armsx2_frontend_is_the_shell.md`](research_log/20260822_2203_armsx2_frontend_is_the_shell.md).

xenia's shell remains the worst structured: Activity-per-manager, a menu tree. Its files: `GameProfiles`, `GameOptimizationsActivity`, `GamePatchManager`,
`ContentInstaller`, `ControllerMappingActivity`, `CrashReporter`. Start from
what it learned. See [`capability_inventory.md`](capability_inventory.md).

**Use the capability inventory as the reality check.** Do not design a screen
for a capability no backend has. Do not omit one that four backends already
have.

The shell runs on the device from the first week. A shell that only runs on a
desktop hides the two-display problem, which is the hardest layout question in
the app.

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

1. **melonds_HD_2 `renderer_cases/`**, as the case format. It combines
   savestate fixtures, golden images, differential comparison and ROM-by-hash
   in one design. Read its `README.md` and `case.template.json` before you
   design anything.
2. The Vita3K-Thor on-device regression suite and its savestate fixture runner.
   It already runs on the Thor.
3. The ARMSX2 golden image comparer, `comparer.js`.
4. The ARMSX2 headless replay pattern, `pcsx2-gsrunner`.
5. The two existing agent skills, from xenia-thor and Vita3K-Thor.

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
  `GameProfiles.java` first. It is the most complete Android shell in the fleet
  and the worst structured; take the features, not the navigation. It is the
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
- **Backends are packed into one binary**, so shared flows can be optimised
  across them. PS3 is the one optional separate install, forced by its
  licence. See [The backend model](#the-backend-model).
- **One pinned Mesa Turnip driver is bundled and is the reference
  configuration.** A different driver is a per-game override. See
  [The driver baseline](#the-driver-baseline-pinned-turnip).
- **PS3 is deferred**, not cancelled.
