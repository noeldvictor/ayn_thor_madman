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

## What this repo is

This repo is the control plane for a fleet of emulator forks. The forks target
the AYN Thor.

This repo holds the shared parts. An agent starts here to work on any fork.

The goal is one workspace, one MCP, one shared layer and one app.

**The product is one real app. It is not a hub over other emulators.** The app
does not install seven other apps and launch them. The emulators become
backends inside one application, with one UI, one library and one settings
system. This is a unified emulator.

The app is not built on libretro. See
[Upscaling and filters](#1-upscaling-and-filters).

To make this possible, the toolchain must be unified first. See
[One toolchain](#0-one-toolchain--do-this-first).

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

**Large binary files bloat the repo.** A manual is often a large PDF. Decide on
git-lfs before you commit a large set. This is not decided yet.

## The fleet

The forks stay in their current directories. This repo tracks the forks. This
repo does not contain them.

### Tier 1 — active targets

| Fork | Path below `Documents/` | Harvest from |
| --- | --- | --- |
| xenia-thor | `xenia-thor-workspace/xenia-thor` | xenia-canary, xenia-edge |
| rpcsx-ui-android-thor | `ps3-thor/rpcsx-ui-android` | RPCSX, rpcs3, aps3e, ARMSX3 |
| Cemu-thor | `cemu-thor-experiment`, branch `android-port` | cemu-project, SapphireRhodonite, SSimco |
| azahar-thor | `azahar-thor/azahar` | azahar-emu |
| watermelon-DS-THOR | `melonds_HD/melonDS-android` | WatermelonDS, melonDS-android-lib |
| Vita3K-Thor | `psvita/Vita3K-Thor` | Vita3K |
| ARMSX2 | `armsx2-thor/ARMSX2` | ARMSX2 upstream |

ARMSX2 is an active target. ARMSX2 is also the reference implementation of the
shared layer. See [ARMSX2 is the seed](#armsx2-is-the-seed).

### Tier 2 — carried along

| Fork | Path below `Documents/` | Harvest from |
| --- | --- | --- |
| GameThor | `gamethor` | GameNative |
| eden-thor | `eden-thor` | eden-emu |
| melonds_HD_2 | `melonds_HD/melonds_HD_2` | no upstream set |

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

Unify these:

- **NDK version.** One version. Choose the newest that every fork can build.
- **ABI.** `arm64-v8a` only. The Thor is arm64. Drop `armeabi-v7a` and
  `x86_64` from the shipping build. ARMSX2 already does this.
- **`minSdk`.** One value, 33 or lower. The Thor runs Android 13, which is
  API 33. GameThor targets API 28 today.
- **`targetSdk` and `compileSdk`.** One value each.
- **Gradle and AGP.** One version each. The fleet spans Gradle 7.3.3 to 9.6.1.
- **C++ standard.** One value. melonDS-android declares `-std=c++17`.
- **Vulkan setup.** One loader, one validation configuration, one extension
  set.

Record the chosen row in this repo. Each fork reads it. No fork sets its own.

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

## Open decisions

These are not settled. Do not assume an answer. Ask, or mark the assumption.

1. **The toolchain row.** Which NDK, `minSdk`, `targetSdk`, Gradle, AGP and
   C++ standard does the fleet use? Every other decision waits on this one.
   See [One toolchain](#0-one-toolchain--do-this-first). Decide this first.
2. **How a backend loads.** The app is one app. The open question is whether a
   backend is statically linked, a dynamic feature module, or a `dlopen` module
   inside the same app. This is not the libretro question. libretro is
   rejected. This is about packaging one app that holds several large cores.
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
