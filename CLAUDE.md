# ayn_thor_madman — the master repo

## Working rules — read before you touch anything

**These override anything below them. Set 2026-08-23.**

### All work stays in this repo

**Do not modify a fork. Ever, unless asked for that fork by name.**

Read them, measure them, learn from them, and write what you learned **here**.
A change to a fork is not this project's work product; **this repo is.**

**This rule was set after a fleet-wide SDK migration edited nine forks.** The
change built cleanly on four of them and was still wrong, because it put the
work in nine places nobody is tracking. **It was reverted in full**, and the
finding it produced is kept in
[`work_log/20260823_1310_sdk_standardisation.md`](work_log/20260823_1310_sdk_standardisation.md).

**Measurement is not modification.** Building a fork, reading it and running a
lint over it are all fine — they leave no trace. **If a build needs a file
changed to proceed, back it up, and put it back afterwards.**

### Do not use the device

**Somebody else is testing on the Thor.** Every device experiment goes to
[`DEVICE_QUEUE.md`](DEVICE_QUEUE.md) with its prediction and waits.

**This is not a constraint on progress.** Most of what this project needs is
reading, measuring and building, and none of that touches hardware.

### Do not commit emulator adaptation until the core style is settled

**Hold changes that adapt an emulator** until the app's own style is decided.
Committing an adaptation before the target shape exists means adapting twice.

**Repo documents, research and work logs are not adaptation. Keep writing
those.**

### Light mode is the default

**The person using this prefers light mode. Build for that first.**

Offer a theme switch so anybody can choose, but **the default is light** and the
light theme is the one that gets the attention. **A dark-first design with a
light option bolted on is not the same thing.**

### The UI must be cheap. This is a constraint, not a style note

**Every frame the shell draws is fill rate and CPU taken from the emulator**, on
a tile-based GPU with a thermal budget. [Foundation](#foundation) point 2 says
speed is the product; **the frontend is not exempt from it.**

**The rules:**

- **Nothing animates unless a finger is on it.** No idle motion, no looping
  background, no ambient shader. An animated library background costs frames
  forever to be looked at for seconds.
- **No blur and no elevation shadows.** Both are fill-rate operations and both
  are worst on a tiler. **Separate surfaces with a flat panel colour and a
  one-pixel line**, which is what the light palette is built for.
- **Minimise overdraw.** One opaque background. No stacked translucent layers.
  A tiler pays for every layer at every pixel.
- **Cover art is decoded once at display size** and cached. Never decode a
  full-resolution image and scale it per frame.
- **Lazy lists with stable keys**, so scrolling recomposes rows rather than
  rebuilding them.
- **Screen-2 draws on change, not per frame.** Already a rule; it belongs here
  too.
- **While a game runs, the shell should draw nothing.** The in-game overlay is
  the exception and it is one cheap layer, not a second UI.

**Judge a screen by what it costs when nobody is looking at it.** The right
answer is almost always nothing.

### The library is per console, not one mixed list

**A console selector sits at the top. Pick a console, see that console's
games.** An **All** entry stays available for anyone who wants the mixed view.

**This reverses an earlier decision here**, which said the library is one list
across every backend. The reasons for the change:

- **It matches intent.** A person reaches for "a PS2 game", not for "a game".
- **It keeps each list short**, which is the cheap option as well as the clear
  one.
- **It removes a whole class of confusion** — two games with the same name on
  two systems, or a cheat badge that belongs to a different backend.

**Losing nothing:** the shared library, per-game overrides, cheats, storage
accounting and badges all still work across every backend. **Only the default
view is split.**

### The navigation model is EmulationStation's: system first, then games

**Pick a console, then pick a game.** Two levels, both legible from arm's
length, both drivable with a d-pad.

**EmulationStation is the reference because it is the cheapest serious frontend
that exists** — it was built to run on a Raspberry Pi 1. That is the same
constraint as the rule above, solved by somebody else first.

**Take:**

- **The system selector as the top level.** Not a filter bolted onto a mixed
  list — the first thing you see.
- **Gamepad-first navigation.** The Thor has real buttons. Every screen must be
  drivable without touching the glass, and touch is the addition rather than
  the assumption.
- **A game list with a metadata panel** beside it, rather than a grid that
  hides the details.

**Do not take:**

- **Themed background art and video snaps.** They are the reason some ES themes
  are slow, and on a tiler a full-screen video behind a menu is the worst thing
  on the list.
- **Deep nested settings menus.** ES inherits the same failure this project
  names in RetroArch. **Settings stay in one place with search.**
- **Per-system theme packs**, at least until the core is settled.

### The look to match is Alekfull NX, which means the Switch home screen

**Chosen 2026-08-23.** Alekfull NX is an EmulationStation theme that mimics the
Nintendo Switch home screen, and it fits the constraints already set better than
the darker, busier alternatives.

**Why it fits:**

- **It is clean by construction.** The Switch home screen is mostly empty
  space, one accent colour and a row of large icons. Low ornament is also low
  fill rate.
- **It has a light variant**, because the Switch does. Most well-known ES
  themes are dark, so copying one of those would undo the light-mode decision.
- **Its system row is horizontal**, which is the shape already chosen for the
  system selector.

**Take:**

- **A horizontal row of large, evenly spaced system entries** as the top level.
- **A high whitespace ratio.** Space does the separating, not boxes and rules.
- **One accent colour**, used for selection and nothing else.
- **Minimal chrome.** No panel border where a gap will do.

**Do not take:**

- **Its icon set, fonts or backgrounds.** Those are the licensed assets. **Take
  the layout, not the files** — see below.
- **The Switch's selection animation.** The home screen scales and glows the
  focused tile; that is per-frame work for a cursor. **Selection is a colour
  change and a rule.**

**Honest limit:** the claim here is that Alekfull NX mimics the Switch home
screen and that the Switch aesthetic is clean, light-capable and cheap. **No
claim is made about which ES theme is most popular**, which is not verifiable
from here and does not matter — the aesthetic is the target, not the ranking.

### Take an ES theme's layout, never its files

**EmulationStation themes differ in skin, not in structure.** ES constrains its
views — a system view and a gamelist view — so nearly every well-known theme
lands on the same shape: a system carousel with a large logo, then a list on one
side with box art and metadata on the other. **They vary in art, fonts and
colour, not in layout.**

**Two consequences.**

**"Copy the most popular theme" mostly resolves to "use ES's standard
layout"**, which is already the plan. The remaining difference is the skin, and
the skin is the part this project has its own opinion about.

**Most of the well-known themes are dark**, because ES's default is dark and the
aesthetic followed. **Copying one wholesale would undo the light-mode
decision**, so take the proven layout and render it in the light palette.

**And themes are asset bundles with their own licences** — logos, fonts,
backgrounds. **Take the layout, not the files.** Same rule as everywhere else
here: a design is not expression, an asset is.

### The UI references are melonDS, ARMSX2 and Dolphin

**Those three are the ones worth looking at.** Take layout, interaction and
structure from them.

Earlier notes here ranked shells by line count, which measured effort rather
than quality. **These three are named because they look and feel right**, and
that is the criterion that matters for the app's own screens.

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

### Translate the console to ARM64. Never inherit the x86 detour.

**Stated 2026-08-23. This is the CPU half of the product's guiding idea.**

**Rosetta 2's core idea is not "be a fast JIT". It is: translate a fixed guest to
a fixed host, ahead of time, once.** Take that. **What we translate is a console
CPU and GPU. What we translate to is ARM64. There is no third machine in the
middle.**

**But every recompiler in this fleet has one, and it is x86-64.** Verified:

| Fork | Backends present | Written first |
| --- | --- | --- |
| xenia | `x64`, `a64`, `arm64`, `llvm` | **`x64`** |
| Cemu | `BackendX64`, `BackendAArch64` | **`BackendX64`** |
| ARMSX2 | `common/emitter/` (x86), `pcsx2/arm64/` | **the x86 emitter** |

**These are desktop emulators with an ARM64 backend added.** The guest decoder,
the IR and the idioms were all shaped against x86-64 first, so the real path
today is **console CPU -> an x86-shaped waypoint -> ARM64.**

**That waypoint is not free, and it is measurable.** Three x86-era choices cost
real instructions on ARM64:

| x86-shaped choice | Free on x86-64 | **Costly on ARM64** |
| --- | --- | --- |
| **Guest state in memory, loaded per operand** | memory operands **fold into the instruction** — `add eax, [ctx+8]` is one instruction | **load/store architecture** — every access is **its own instruction** |
| **Assume few host registers** | **16** GPRs, so spilling is normal | **31** GPRs — nearly twice as many, enough to keep guest registers resident |
| **Compute flags eagerly** | almost every instruction sets flags implicitly | flags are **opt-in** (`ADD` against `ADDS`), so eager flags are pure waste |

**The first row is the whole measurement.** An SSA-over-a-context IR costs
roughly nothing extra on x86 because the loads disappear into the consuming
instruction. **On ARM64 each one is a real instruction.** That is why the two
SSA-over-context IRs — **xenia's HIR and dynarmic's**, both from x86-first
projects — expand **4x to 5x**, while **Cemu's virtual-register IML expands 2x.**

**And it is why dynarmic still expands 4x translating ARM64 to ARM64**, where a
fixed mapping would be nearly the identity function.

> **The rule: translate the console's CPU and GPU straight to ARM64. Do not
> carry a design that was correct for a 16-register machine with folded memory
> operands onto a 31-register load/store machine.**

**TWO CONCRETE INSTANCES, FOUND 2026-08-24 IN XENIA'S LEDGER.** This rule had a
register-model argument and no opcode-level example. It now has two, both `OPEN`
and both unbuilt:

- **`rlwinm` lowers to three instructions because x86 has no rotate-and-mask.**
  The HIR models PPC rotate-and-mask as **rotate + AND**, so the general
  non-wrapping case emits `ROR` + `AND` + `UXTW`. **ARM64's `UBFM` *is*
  rotate-and-mask**, and the shift and mask are compile-time constants. **Three
  instructions to one**, on one of the most common PowerPC instructions.
- **Every condition-register update emits three compares where ARM64 needs one.**
  `PPCHIRBuilder::UpdateCR` models a CR update as **three independent HIR compares
  of the same operand pair**, each lowering to `cmp`+`cset`. **One ARM64 `CMP`
  sets N/Z/C/V and `CSET` reads flags without disturbing them**, so the correct
  form is one `cmp` and three `cset`. **Two redundant compares per update, and
  updates fire on every `cmpw`, every `cmpwi` and every `Rc=1` instruction.**
  Common-subexpression elimination cannot fix it, because the three operations
  have different opcodes.

**The ACM TACO work put compare-and-branch at roughly 18% of inflation. The
second instance is that finding's mechanism, inside a real emulator, unfixed.**

**A structural rule travels with both: this kind of fusion cannot be a sequence
peephole.** By the time the outer operation is emitted, the inner one is already
emitted and register-allocated. **It has to be a HIR opcode.**

See [`research_log/20260824_0520_the_x86_detour_with_receipts.md`](research_log/20260824_0520_the_x86_detour_with_receipts.md).

**The GPU half is the same rule and this repo already had it** — translate the
guest graphics API rather than model the guest GPU, proven by RE2 Remake running
on this Thor through GameNative and DXVK. **Two pipelines, one instruction:
translate, do not emulate, and translate to ARM64 directly.**

**What this does not license.** Rewriting a register model is the deepest reach
into a core, and [`shared_layer/PATTERNS.md`](shared_layer/PATTERNS.md) says do
not attempt code translation before pipelines 2 and 3 are proven. **That still
holds.** Nothing here is timed on the Thor. The measurement is stage A only.

**The north star for CPU speed is now specific, 2026-08-23:
[`shared_layer/TRANSLATION.md`](shared_layer/TRANSLATION.md).**

**Speed in a recompiler is instruction inflation, and inflation is the register
mapping.** Four independent lines converge:

- **Inflation predicts slowdown by regression**, state-of-the-art DBTs sit at
  **1.46 or worse**, and attacking it measured **2.99x to 7.12x** on QEMU
  (ACM TACO, March 2024).
- **It is not caused by having an IR.** LATX keeps QEMU's IR and is fast; Box64
  without an IR and FEX with one land in the same band. **QEMU is slow because
  it optimises little.**
- **It is caused by the register mapping.** The literature: emulating guest
  registers in memory "generates excessive load and store operations, and thus
  drastically degrades the performance", worst **when the guest register file is
  larger than the host's** — the Xenon's **128 VMX onto 32 NEON**.
- **The fleet's own numbers agree.** Measured statically: **Cemu IML 2.0**,
  **dynarmic A64 4.0**, **xenia HIR 5.0** IR ops per guest instruction.
  **dynarmic expands 4x translating ARM64 to ARM64** — the easiest case there
  is. **Expansion tracks the register model, not guest-host distance.**

**And residency is not allocation.** Getting guest registers into host registers
**at all** is the large lever — xenia measured its guest thread **memory-bound,
~half of a title's field CPU**. Choosing *which* register is worth **5.76% to
7.79%** (LCCRA, EuroSys 2026). **Residency first; allocation quality probably
never.**

**This is where one device, one host ISA pays most.** A portable translator
cannot assume a register count, an ABI or which registers are reserved, so it
keeps guest state in memory and hopes a general allocator recovers it. **Here
both register files are constants** — 31 GPRs and 32 vector registers on the
host, and a known file per guest — **so the mapping is a constant too, decidable
once per backend and never re-derived at run time.** That is the DELETE
operation applied to the hottest path in the emulator, with measured evidence
behind it for the first time.


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

#### The paused agent loop -- pause the guest, and model latency is free

**Stated 2026-08-23.** See [`shared_layer/AGENT_LOOP.md`](shared_layer/AGENT_LOOP.md).

**An agent driving a real console loses every race against an animation. An
agent driving an emulator never has to enter the race.**

**Pause. Capture. Ask a vision model which button. Inject. Resume for N frames.**
Steps three and four happen while the guest is frozen, so **from the guest side
the input arrives on the frame after resume** -- no timer expires, no animation
desyncs, no window is missed.

> **Model latency costs the guest nothing. That is only available because we own
> the emulator.**

**It removes the worst measurement problem in the fleet.** Getting to a scene is
the noisy part, not measuring at it: **button presses through cutscenes have a
~50% noise floor** while a gated title screen has 0.2%. **A deterministic route
turns the prologue from the dominant variance into a fixed cost**, unblocks every
queued experiment that waits on a person to reach a scene, and **enables the
compatibility sweep this project specified and never built.**

**And it is a product feature, not only a harness** -- auto-navigating to a save
point, or getting past a menu somebody cannot read, is the same mechanism
pointed at the user.

**The one thing it must know is when NOT to look at the frame.** A vision model
asked "what button?" during a pre-rendered movie will guess. **Every console has
a dedicated video-decode path and every fork implements it** -- PS3 `cellVdec`,
PS2 IPU, Wii U H264, 3DS `mvd`, Switch `nvdec`, Vita `SceAvcdec`, 360 XMA -- **so
"am I in a movie" is answerable in every backend.**

**That signal is worth more than the loop alone.** A backend that declares its
guest activity state can also refuse to be measured in a 50%-noise state, **not
generate frames for a fixed-rate video** where extrapolation invents motion,
not upscale decoded video, not draw the overlay over a cutscene, and change the
thermal policy for a decode-heavy CPU-light workload.

**Every primitive already exists**: pause and resume in all seven forks, burst
capture in rpcsx and Vita3K, input automation in Vita3K's 120-line skill, and
**eden's cheat VM already exposes `PauseProcess`, `ResumeProcess` and
`HidKeysDown` as host callbacks.** **ARMSX2's `docs/mcp-server.md` already
specifies the surface and names the same bottleneck.** **The loop itself exists
nowhere.**

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
  [Cheats](#4-cheats).
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
GPL-3.0-or-later.**

**CORRECTED 2026-08-23: xenia has one too, and it is a different design.** The
claim "nothing else has one" was the **thirteenth** absolute negative in this
repo to be wrong. A filename search for `framegen|lsfg|fsr3` returns 39 files in
ARMSX2 and **zero** in xenia, because xenia's lives inside
`src/xenia/ui/presenter.*`. **A filename search cannot see a feature that lives
in another subsystem's files.**

| | ARMSX2 | xenia |
| --- | --- | --- |
| Method | **interpolation** | **extrapolation** |
| Needs the next real frame | **yes** | **no** |
| **Latency cost** | **a held frame** | **none** |
| Source | eden PR #4263, lsfg-vk | written here |
| Default | — | **off, byte-identical when off** |

**Verified by vocabulary:** ARMSX2's `FrameGen/` has eight occurrences of
*interpolate* and **zero** of *extrapolate*; xenia's flag is
`present_frame_extrapolation`.

**The latency split is the decision, and it was not written down.**
Interpolation cannot present until the later real frame exists, so it costs **a
full guest frame of latency**. Extrapolation forward-projects and costs none.
**On a device with real buttons that is the same class of cost as the touch
overlay this project already hides.**

**xenia has two synthesis methods and an A/B already wired:** a 50% cross-fade,
or `present_frame_gen_motion_warp` — a separable **Lucas-Kanade** global motion
estimate in a 1x1 RGBA32F pass, forward-extrapolating by half the camera
translation. Plus `present_frame_gen_factor`, default 2, which subdivides the
guest interval into slices.

**And xenia has the stronger argument for the feature**, which this document did
not have:

> For logic-locked-framerate guests (e.g. Blue Dragon's fixed 30Hz),
> synthesizing in-between presented frames is the only way to raise the
> *presented* frame rate

**When the guest's logic is locked to 30 Hz, no optimisation raises the frame
rate**, because the guest will not produce more frames. **Frame generation is
the only lever that exists.** That is narrower and much harder to refute than
"makes 30 feel like 60".

**Do not merge them.** Two designs answering two tradeoffs, not one feature
written twice — the LRU-cache result again. See
[`research_log/20260823_1549_frame_generation_is_in_two_forks.md`](research_log/20260823_1549_frame_generation_is_in_two_forks.md).

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

  **GameThor already has two thirds of this.** `app/gamenative/gamefixes/` holds
  **29 per-game fixes** keyed by store ID, behind six typed kinds —
  `WineEnvVarFix`, `RegistryKeyFix`, `IniFileFix`, `LaunchArgFix`,
  `GOGDependencyFix` and `KeyedCompositeGameFix` — dispatched by a
  `GameFixesRegistry`. **Take the shape: a stable game key, a small set of typed
  kinds, composition, one readable file per game.**

  **Change two things.** GameThor's fixes are **code**, so adding one needs a
  rebuild, and [Foundation](#foundation) point 4 requires installing a fix to
  take one action in the app — so ship them as **data**. And **add the
  provenance field GameThor lacks**: a fix with no recorded source cannot be
  re-derived when it stops working.

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
  **Updated 2026-08-22 with measured frontend and native censuses, and GameThor
  surveyed 2026-08-23**, so every fork now has a capability recorded. What
  remains are per-subsystem gaps rather than whole forks.
- **Audio: surveyed 2026-08-23 and answered.** Three forks already use Oboe;
  standardise on it. Latency itself is still unmeasured.
- **ADPF: surveyed 2026-08-24 and answered.** Counted the four real entry points
  per fork, because counting the word "thermal" matches everything. **melonDS has
  the only complete implementation** — ten files, a factory choosing NDK at API
  33+, JNI at 31+, and a no-op tier, plus a **thread-safe hint session** because
  it reports from nine sites. **azahar, Vita3K, eden and GameThor have none.**
  **Only xenia and rpcsx read `getThermalHeadroom`**, so everything else is open
  loop. **Take melonDS's interface and delete two of its three tiers** — the Thor
  is API 33, so the JNI and Dummy tiers serve variability this device does not
  have. See
  [`research_log/20260824_0330_adpf_survey.md`](research_log/20260824_0330_adpf_survey.md).
- **Control overlays: surveyed.** See
  [How to build the shared layer](#how-to-build-the-shared-layer), item 0.
- **Save conventions: surveyed 2026-08-23, partially.** The format is
  irreducibly per-backend. The unpriced cost is to the **test harness**; see
  Phase 3. eden, Vita3K and xenia savestate code was found by neither search and
  is recorded as unread, not absent.
- **Thread and cluster affinity: surveyed 2026-08-23.** Four forks set host-side
  affinity; **melonDS and Vita3K set none at all**, so their threads land
  wherever the kernel puts them. See
  [`research_log/20260823_0030_thread_affinity.md`](research_log/20260823_0030_thread_affinity.md).
- **Frame pacing: surveyed 2026-08-23, and it is the one host subsystem with no
  incumbent.** **No fork uses Swappy. No fork uses `VK_GOOGLE_display_timing`.**
  Every fork picks a Vulkan present mode and stops. Verified with a second
  search; the single build-file hit was Discord's `game_sdk`, not Google's.
  See [`research_log/20260823_0040_frame_pacing.md`](research_log/20260823_0040_frame_pacing.md).

  **This inverts the audio result exactly.** Three forks adopted Google's audio
  library independently; **zero adopted its frame-pacing library.** Same
  platform, same vendor, opposite outcome.

  **It matters because FIFO is vsync, not pacing.** A 20 ms frame at 60 Hz
  misses its vsync and alternates 16.6, 33.3, 16.6 — and that judder is more
  visible than a stable 30. Swappy exists to pick a stable divisor and hold it.
  **That is the emulator case on a handheld.**

  **CORRECTED 2026-08-24: it is not the cheapest subsystem to own, because it
  has an incumbent.** The two Google-library negatives hold and were re-checked.
  **The claim that every fork picks a present mode and stops does not.**

  **Cemu has a four-part frame pacing subsystem in production, on Android:**
  four vsync modes; **`VK_KHR_present_id` plus `VK_KHR_present_wait` queue-depth
  limiting**, applied only on the `FIFO` path with `m_maxQueued = 1` and a 40 ms
  timeout, which is latency control rather than vsync; **host-driven vsync**,
  where `VsyncDriver_startThread` drives the *guest's* vsync from the host
  display; and **dual-screen present serialisation on Android.**

  **That last part answers a question `thor_backend.h` records as open.** Its
  comment: *"Keep TV and GamePad swapchains from forcing each other to idle. A
  single shared previous-frame marker serializes dual-screen presents on
  Android."* **Two swapchains, one per screen, one marker each.** The Wii U's TV
  and GamePad is structurally the Thor's two panels.

  **Why the survey missed it: it searched for two library names.** Cemu spells
  its mechanism in Vulkan core terms, and its most important part lives in
  `LatteTiming.cpp` as guest-timing code rather than in a renderer. **A survey
  that searches for named libraries finds adopters of those libraries, not
  implementations of the capability.**

  **One new fact from the same read:** the Thor's Turnip **does expose
  `VK_GOOGLE_display_timing`** — xenia's only hit is a logcat dump of
  device-supported extensions in a research file, not code.

  See [`research_log/20260824_0010_frame_pacing_has_an_incumbent.md`](research_log/20260824_0010_frame_pacing_has_an_incumbent.md).
- **Clean build times: one fork done, seven to go.** melonDS-android builds
  clean in **15 min 27 s** to a 55.5 MB APK, on **NDK 28 and Gradle 9.5.0**, so
  it is one NDK major and one Gradle minor behind the standard row. **The time
  is dominated by compiling librashader from Rust source, not by the
  emulator's C++** — and **it compiled that C++ three times**, for three ABIs,
  of which the Thor runs one. **Record the ABI list beside every build time.**
  See
  [`work_log/20260823_0006_melonds_clean_build.md`](work_log/20260823_0006_melonds_clean_build.md).
  ARMSX2, Cemu-thor and xenia-thor remain, and are the expensive ones.

  **Vita3K attempted 2026-08-23 and does not build.** Two obstacles, neither in
  its code. Its own `AGENTS.md` recipe runs gradle from the fork root, where
  `./build.gradle` pins AGP 8.13.0 while `./android/build.gradle` asks for
  9.2.1 — **`android/` is a standalone Gradle project and must be built from
  inside itself.** Then, from there, it **fails on `x86_64`** because the fork
  ships a prebuilt FFmpeg matched to a commit SHA and there is none for that
  ABI.

  **`arm64-v8a` configured and compiled cleanly. Only the ABI the Thor cannot
  run failed** — after consuming most of the 12 min 52 s building Boost for
  `x64-android`. See
  [`work_log/20260823_0027_vita3k_build_attempt.md`](work_log/20260823_0027_vita3k_build_attempt.md).

  **azahar builds clean in 14 min 33 s** to a 27.7 MB APK, arm64-only, on JDK
  **17** and Gradle **8.14.5** — the furthest fork from the standard row, and its
  own `AGENTS.md` says to keep it there. **eden does not build**: it needs
  `pkg-config`, and then **`glslangValidator`, which the Android NDK does not
  ship** — NDK 28.2 provides `glslc` instead.

  **After four forks, nothing has failed inside an emulator.** Every obstacle
  has been in the periphery: a recipe, an ABI list, a plugin version, a missing
  host tool.

  **The host-tool class matters most for the agentic thesis**, because it is
  invisible until somebody tries. **A fork that needs `pkg-config` and
  `glslangValidator` cannot be built by an agent on a machine that has neither,
  and nothing in the fork says so.**

  **Known host tools so far:** JDK 17 **and** 21, the Android SDK and NDK, cargo
  and rustup (melonDS), vcpkg (Vita3K), `pkg-config` and `glslangValidator`
  (eden), and `git` with `core.longpaths` on Windows.

  See [`work_log/20260823_0148_azahar_clean_build.md`](work_log/20260823_0148_azahar_clean_build.md)
  and [`work_log/20260823_0207_eden_build_attempt.md`](work_log/20260823_0207_eden_build_attempt.md).

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
  **But verify it from the emitted compile commands, not from the CMake.**
  Vita3K's `USE_LTO` defaulted to `RELEASE_ONLY`, which covers the `Release`
  configuration — **and neither of its shipped builds is `Release`.** Confirmed
  from the flags: **all 973 translation units compiled with `-O2 -DNDEBUG` and no
  `-flto`.** **The default meant "never."** Every CMake fork emits a
  `compile_commands.json`, so this is a build-time check needing no device. See
  [`research_log/20260824_1420_lto_is_easy_to_configure_and_easy_to_have_off.md`](research_log/20260824_1420_lto_is_easy_to_configure_and_easy_to_have_off.md).
- **One frame pacing and present path.** Consistent pacing needs one owner.

### And one nobody had stated: packing raises every backend's capability ceiling

**Every argument above is about resources. This one is about what a backend is
allowed to do at all.**

**A backend's renderer can only use what its own device layer negotiated**, and
that set is an accident of each fork's portability history rather than a property
of the Thor. Measured 2026-08-23 with
[`tools/vk_capability_census.py`](tools/vk_capability_census.py):

| Fork | Device extensions requested |
| --- | --- |
| eden | 42 |
| ARMSX2, Vita3K | 35 |
| Cemu | 27 |
| xenia, azahar | 13 |
| melonDS | 9 |

**Union 135. Asked for by one fork alone: 89. Asked for by four or more forks:
6** — and most of those six are how you get a window. **Eight emulators target
one GPU and agree on almost nothing.**

**Figures revised 2026-08-24 when rpcsx was added to the census.** Omitting a
fork made the tool's no-user list wrong twice, so it now also carries a
**whole-fork second search as a built-in column** — a device-layer miss is not
absence. **After that, three of the fourteen high-value extensions are unused
anywhere in the fleet, not eight**: `VK_EXT_load_store_op_none`,
**`VK_EXT_graphics_pipeline_library`** and `VK_EXT_multi_draw`.

**The middle one matters most here**: precompiling a pipeline needs render state,
and graphics pipeline library removes that requirement for the shader half.
**Nobody uses it and the device exposes it.**

**ARMSX2 states the consequence in its own source.** Its frame generation, which
this file calls possibly the highest-value feature in the fleet, runs at **fp32
because PCSX2's device layer never requests `VK_KHR_shader_float16_int8`** — so a
Float16 shader module "would be invalid usage on it regardless of what the
physical device reports". **eden runs the fp16 path. The Thor probes
`shaderFloat16 = 1`.** The comment names the fix and its size: two lines.

**COUNTED 2026-08-25, and the ARMSX2 instance is a fleet property.** Searched
each fork's own device layer for **both** spellings — the extension name and
`VkPhysicalDeviceShaderFloat16Int8Features`, since the extension is **core in
Vulkan 1.2** and this device reports **1.3.128** — with the vendored Vulkan
headers filtered out, because a first pass counted **12 ARMSX2 hits that were all
in `vulkan_core.h`**:

| Fork | fp16 in its OWN device layer |
| --- | --- |
| **ARMSX2, xenia, Cemu, azahar, melonDS** | **none** |
| **Vita3K** | requests the extension **and** checks the feature bit |
| **eden** | queries `shader_float16_int8.shaderFloat16` |

> **Five of seven backends cannot legally execute a Float16 shader module on this
> device, whatever the hardware reports.**

**Take Vita3K's device layer shape.** Its extension list is a table of
`{extension, &capability_flag}` **with a comment saying what each is FOR**, and
it validates in two stages against a **named feature** rather than a generic
flag: `support_fsr &= shaderInt16`, then a deeper `getFeatures2KHR` check for
`shaderFloat16`, under the comment *"needed for FSR"*.

> **Requesting an extension and having its feature are different things.** A
> device layer that stops at the extension name has not established the
> capability. **That is a third layer under the ceiling argument**, below "does
> the fork ask" and "does the device support".

**And FSR needs `shaderInt16` AND `shaderFloat16`** — so the device requirement
for a well-known upscaler, which is this project's flagship feature area, **is
already discovered and guarded inside the fleet.** One more rule from the same
table: **rasterized order attachment access and fragment shader interlock are
mutually exclusive**, and Vita3K's comment records which wins.

See [`research_log/20260825_1530_five_of_seven_forks_cannot_run_an_fp16_shader.md`](research_log/20260825_1530_five_of_seven_forks_cannot_run_an_fp16_shader.md).

**So the packing argument gains a leg.** One shared device layer enables the
Thor's real feature set once, and every backend then sits above the same ceiling.

**Two limits, stated.** Packing does not make a backend *use* a feature; that is
per-backend renderer work. And **deleting the negotiation code is not the win** —
availability tests are only 1% to 9% of each device layer, about 500 lines
fleet-wide. **What packing changes is the unit of the work: a capability enabled
once reaches seven backends; one negotiated per fork reaches one.**

### The pinned driver makes a pipeline cache shippable

**This property was bought by a decision already made for a different reason.**

**The scope of the claim, stated.** No fork in this fleet ships a pipeline cache
to its users, checked by reading all seven device layers and cache paths. **No
claim is made about emulators outside this fleet**, which have not been surveyed.
What is verifiable is the mechanism: **shipping a cache requires a fixed driver,
and this project pins one.**

A Vulkan pipeline cache is driver-specific, so nobody can ship one to their
users. **The Thor pins one Turnip build**, which is exactly the property that
lets Valve distribute precompiled shaders for the Steam Deck — same hardware for
every user, so one user's cache is valid for all of them.

**The mechanism exists twice in the fleet already**, independently, which is the
convergence signal this file already trusts:

- **Cemu `VulkanPipelineStableCache.cpp`** stores per pipeline the active shaders
  by hash plus "an almost-complete register state of the GPU", then compiles
  against a **placeholder renderpass**. Its two directories,
  `shaderCache/transferable` and `shaderCache/precompiled`, are **the same split**
  this repo reached independently in `PipelineCache.kt`.
- **eden `shader_environment.cpp`** serialises the guest environment per shader
  and replays it through `FileEnvironment` with no guest running.

**Valve's Fossilize is MIT and is a Vulkan layer**, so it records a backend
without modifying that backend's source.

**Read the caveat before acting.** Cemu **explicitly disables Valve's layer** —
`DISABLE_VK_LAYER_VALVE_steam_fossilize_1=1` in `src/main.cpp` — and the reason
is not recorded. Find it first.

**This does not claim shaders can be extracted from a dump at install time.** A
native game's pipelines are fixed; an emulator's are derived from guest state and
are discovered by playing. **It does not claim a frame win either** — a warm
cache removes stutter, and stutter lives in the tail, not the mean.

See [`research_log/20260823_2110_shipped_pipeline_cache.md`](research_log/20260823_2110_shipped_pipeline_cache.md)
and `DEVICE_QUEUE.md` entries 15 and 16.

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

## Did it apply? The most repeated failure in this fleet

**Twelve instances, five forks, found independently by different people at
different times. They are not twelve bugs — they are one disease with twelve
mechanisms**, and this project accumulated defences against them one at a time
without noticing they were the same thing.

> **A setting that exists is not a setting that applies.**

**The user-visible symptom is always the same: a control moves and nothing
happens.** Sometimes the control is a switch, sometimes a compile flag, sometimes
a default in code. **Nobody is told, because from the software's point of view
nothing went wrong.**

**Eleven of the twelve cost frames or convenience. One costs the truth**:
xenia's desktop HLE intercepts returned `count=0` for weeks from a dispatcher
that never reached the handler, and the fix **corrected an earlier research
conclusion that had been built on that zero.**

> **An instrument that can return zero must be proved able to return non-zero.**

**[`shared_layer/DID_IT_APPLY.md`](shared_layer/DID_IT_APPLY.md) is the index**:
every mechanism, how it presents, what detects it, and the four rules — **verify
from the emitted artefact**, **derive rather than enumerate**, **declare rather
than infer**, and **prove the instrument before believing a zero.**

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
| The two CPU leads are unmeasured | **xenia has flags for all three**, with SWOG citations |
| Guest FP status is a faithfulness choice | **the Xenon has two FP mode registers, ARM64 has one** |
| `yield` should be replaced with `ISB` | **three forks measured that as a regression** |
| Seven cores in one binary risks symbol collisions | **zero emulator-code collisions; all 25,526 are dependencies** |
| Only ARMSX2 has frame generation | **xenia has one too, by extrapolation instead of interpolation** |
| Storage aggregation has no prior art anywhere | **GameThor has 2,136 lines of it** |
| Only melonDS ships haptics | **seven of eight do; xenia is the outlier** |
| No fork plans render passes | **xenia plans them, and patches the pass begin retroactively** |
| Nobody resolves MSAA on-chip | **xenia does, with the multisample store elided** |
| Nobody merges passes or uses input attachments | **xenia merges; xenia, ARMSX2 and rpcsx all use input attachments** |
| No fork persists translated guest code | **ARMSX2 has a persisted VU JIT, 2,576 lines, with three test files** |
| Frame pacing has no incumbent | **Cemu has four parts of one, including host-driven vsync and dual-screen present serialisation** |
| Eight Adreno features have zero users | **six of the eight were wrong; the device-layer file is not the whole fork** |

**Three more added 2026-08-23 and 2026-08-24, and their causes differ from the
rest.** The listing problem is now well known here. These came from **searching
for a name instead of a mechanism**:

- **"No fork persists translated code"** searched `SaveCodeCache`,
  `code_cache.*persist` and `AotCache`. ARMSX2 calls it a **program cache** and
  the payload a `.vuprog`.
- **"Frame pacing has no incumbent"** searched **Swappy** and
  `VK_GOOGLE_display_timing`, which are two library names. Cemu spells its
  mechanism in Vulkan core terms, and its most important part is **guest-timing
  code in `LatteTiming.cpp`**, not present code.
- **"Eight Adreno features have zero users"** read one device-layer file per
  fork. **The extension is often requested elsewhere.**

> **A survey that searches for a named library finds adopters of that library,
> not implementations of the capability.**


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
5. **READ A FORK'S ROUTING TABLE BEFORE MINING ITS `docs/`.** xenia's own
   `CLAUDE.md` opens with *if you are about to do X, read Y first* — and its GPU
   row says **"Do not re-derive them."** **On 2026-08-24 this repo mined more than
   twenty of its documents and never opened the index to them**, then re-derived a
   dead lever and queued two device experiments on it. **The fork documents are the
   source of truth; the routing table is the way in.**
6. **`git grep` DOES NOT SEE SUBMODULE CONTENTS.** Three wrong results in this
   project came from that alone — dynarmic in Vita3K, xxHash in Vita3K, and a
   fleet SVE search that reported Vita3K clean while its submodule xxHash carried
   five SVE branches. **Pass `--recurse-submodules`, and say whether a search
   did.** The tools in `tools/` now do.
7. **State whether vendored code counts, because it depends on the question.**
   Excluding it is right for *"which fork implements this"* and **wrong for
   *"what compiles into the binary"***. The same SVE search was wrong both ways
   at once.

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

**And [`shared_layer/HOST_SIDE.md`](shared_layer/HOST_SIDE.md) for the other
axis: the concrete inventory of every host-side service a core hands over, and
who owns it instead.** Five groups — graphics, CPU and synchronisation, platform
and process, interaction, data and content — each row naming the fork evidence.
**Several entries are not pipelines at all**: the JNI boundary, the spin
primitive, dependency and symbol ownership, haptics, path resolution.

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

### The rule, found 2026-08-23: unify what is FORCED, harvest what is CHOSEN

**Nine extraction candidates have now been read. The split is total, and one
property separates them exactly** — and it is not how similar the code looks.

**Every candidate that survived is one where keeping N implementations is
impossible. Every candidate that shrank is one where keeping N is merely
untidy.**

Three things force unification here:

| Forced by | Consequence |
| --- | --- |
| **The linker** | one binary cannot hold seven Vulkan devices or five copies of cubeb |
| **The device** | one GPU, one loaded driver, one memory budget, one prime core |
| **The product promise** | one app, one settings system, one hotkey set, one cheat library |

**Nothing else forces anything.** Two LRU caches cost maintenance; they do not
cost correctness, frames or the product.

> **A candidate forced by the linker, the device or the product promise will
> survive reading. A candidate justified only by "these look the same" will
> shrink.**

**So stop opening candidates by looking for similar code.** Ask what the packed
binary, the hardware or the product cannot tolerate two of. **"Six forks have a
driver picker" was never the evidence. "One process loads one driver" is.**

**And the unit of waste is a lesson, not a function.** ARMSX2 learned three
per-game-override bugs from bug reports; every other fork will hit them.
Dolphin added 40 emitter methods melonDS never received. rpcsx measured the ABI
cost that five forks still pay. **That is why every duplication claim shrank
while the project stayed right: the waste is real and was being measured with
the wrong instrument.**

**Rank unification by how well it resists being forgotten**, and the cheapest
durable form is a **test**, which this project has almost none of.

### And the operation is DELETE, not MERGE

**The forced rule has a generator.** Every structure this repo keeps finding
"duplicated" is a **portability layer**, and this project has already refused to
pay for portability everywhere else.

| Present N times | Exists to serve | What we have |
| --- | --- | --- |
| seven Vulkan device layers | many GPUs and drivers | **one Adreno 740, one pinned Turnip** |
| **xenia's HIR, dynarmic's IR** | **many host CPUs** | **one ARM64** |
| `glslang` and the GLSL text path | many shader targets | **SPIR-V, always** |
| runtime feature detection | unknown capabilities | **measured and fixed** |
| multi-ABI builds | many devices | **`arm64-v8a`** |

**They look duplicated because they answer the same question — portability —
not because anyone copied anyone.** That is why every "these look the same"
candidate shrank: **seven answers to one question are not one answer written
seven times.**

**So merging is the wrong operation.** `THOR_RENDER.md` already says extracting
from seven portable renderers "yields the union of seven sets of compromises".
**That generalises.** Delete the machinery serving variability this device does
not have, and what remains is **smaller than any one fork's version**.

**The CPU half has a recent citation, and it is weaker than it was quoted as.**
arXiv:2501.03427 argues QEMU's TCG pays for an IR that exists for
retargetability, and that a **direct guest-to-host translator for a fixed pair**
removes it. **The stated tradeoff is portability, which this project already
gave up.** Measured here: **xenia carries a full HIR and eden's dynarmic carries
an IR; ARMSX2, Cemu and melonDS do not.** xenia's exists because xenia also
targets x86_64.

**CORRECTED 2026-08-23: Cemu has an IR too.** `src/Cafe/HW/Espresso/Recompiler/
IML/` is **12 files and 6,327 lines** — `IMLInstruction`, `IMLOptimizer`,
`IMLRegisterAllocator`, `IMLRegisterAllocatorRanges`, `IMLSegment`,
`IMLAnalyzer`, `IMLDebug`. **That is the same scale as xenia's HIR at 6,605
lines**, and it is a full intermediate language with its own optimiser and
register allocator, not a helper layer.

**The corrected census is three of five, not two of five:**

| Fork | Guest | IR | Size |
| --- | --- | --- | --- |
| **eden** | ARM64 | dynarmic IR | **7,382 lines**, 19 files |
| **xenia** | PowerPC | HIR | **6,605 lines**, 12 files |
| **Cemu** | PowerPC | **IML** | **6,327 lines**, 12 files |
| ARMSX2 | MIPS | **none** — direct emitters | — |
| melonDS | ARM | **none** — direct `ARMJIT_A64` emitters | — |

**And the split is not arbitrary.** **Both PowerPC recompilers chose an IR
independently.** The two without one translate the simpler guests — MIPS, and
ARM onto ARM64, which is nearly the host ISA. **Guest complexity looks like the
driver, not fashion.**

**Consequence for any IR experiment here: there is no same-guest IR-against-no-IR
pair in this fleet.** Both PowerPC forks have one. **A cross-fork inflation
comparison therefore measures the guest as much as the IR**, and must say so.

**THE 35x FIGURE IS WITHDRAWN, 2026-08-23.** Read in full: the proof of concept
`riscv-um` is a **Rust simulator, not a binary translator** — direct translation
is the paper's *future work*. Its benchmark is `benchgen`, **2 million
instructions of rotating `add`, `sub` and `sll`**. It implements **RV64I only**,
and only the opcodes the benchmark uses, with **no memory protection**. The one
results table gives **77 ms against QEMU's 246 ms, which is 3.2x**, and that
could not be reconciled with the 35x headline. **Do not repeat the number.**

**Far better evidence is already in the fleet, and it is a natural experiment.**
**Box64 and FEX-Emu solve the identical problem — x86-64 guest, ARM64 host — and
made opposite IR choices.** **GameThor ships Box64** (0.3.2, 0.3.4, 0.3.6, with
a presets dialog and env-var tuning), so **this repo already contains a
production no-IR direct translator and had not recorded it.**

**Box64 refutes the standard argument for an IR.** The usual defence is that an
IR gives optimisation passes somewhere to live. **Box64 runs them over the guest
instruction stream** — four passes plus substeps for jump destinations, **dead
code elimination** and **flag propagation via Kildall's algorithm**, so it
computes only the flags a later instruction reads. **What an IR provides is a
place to hang per-instruction metadata, and for a fixed pair the guest stream
plus annotations is enough.**

**But the strongest argument FOR an IR is correctness, not portability.**
Risotto (ASPLOS '23) **found real translation errors in QEMU** emulating x86 on
ARM, then **formalised QEMU's IR memory model** and proved its fence mappings
correct. **That is only possible because there is one IR to formalise.**
**Deleting an IR must say where the verification goes instead.**

**And this fleet is not exposed to that problem.** Risotto's case is
**strong-on-weak**. **No guest here has a stronger memory model than the ARM64
host** — MIPS, PowerPC and ARM guests are not stronger, and eden's guest is
ARM64 itself. **The one exception is GameThor's x86-64 through Box64**, which
already owns the problem. **Do not import fence-insertion machinery**; it serves
variability this device does not have.

**Static recompilation's win is not the absence of an IR.** arXiv:2605.08419
(May 2026) translates x86-64 to AArch64 **fully statically with no runtime
component** and reaches only **parity with QEMU's user-mode JIT** on SPECint
2006, at **substantial code size expansion**. **So the large N64Recomp-style
wins come from handing whole programs to an optimising compiler**, which needs
per-game decompilation and symbols — **a cost this project cannot pay across
eight systems.**

**REVISED the same day, after a challenge to check again.** **Translation
quality IS where the speed is, and it is large — the IR is simply not the
lever.**

**"An Instruction Inflation Analyzing Framework for Dynamic Binary
Translators"**, ACM TACO, March 2024, defines **inflation** as host instructions
emitted per guest instruction, shows by regression that **inflation predicts
slowdown**, and finds **state-of-the-art DBTs run at 1.46 or worse.** Applying
its guidance to QEMU measured **5.47x** — range **2.99x to 7.12x** — on **SPEC
CPU 2017**, cutting inflation **83.59% on CINT2017** and **94.56% on CFP2017**.
**That is incomparably better evidence than the 35x withdrawn above.**

**But the same paper rules the IR out as the cause**, by ranking real systems:
**ExaGear, Rosetta 2 and LATX minor; Box64 and FEX moderate; QEMU substantial.**

- **Box64 and FEX sit in the same band** with **opposite IR choices.**
- **LATX is built on QEMU 6 and keeps TCG's IR**, yet is in the best band while
  QEMU is in the worst. **It fixed what the translator does, not whether it has
  an IR** — compare-and-conditional-jump fusion, push/pop elision, AOT, and
  runtime library pass-through **referencing Box64's source.**

> **QEMU is slow because it optimises little, not because it has an IR.**

**The two winning optimisations are ones an IR is supposed to enable** — **dead
code elimination** of unused result bits and **address pre-calculation** — and
**Box64 does dead code elimination with no IR at all.** So the analysis is the
cure, and it lives in either place.

**Caveat that halves the prize here: this literature is CISC-to-RISC.** Every
system measured is x86 to ARM or LoongArch. **This fleet is mostly RISC-to-RISC**
— MIPS, PowerPC and ARM guests on ARM64 — **the easier case, with less inflation
to recover.**

**The part that does transfer is flag handling**, and it is the strongest
specific lead: `jcc` at **9.72%** and `cmp/test` at **8.66%** of inflation,
roughly **18% from compare-and-branch alone.** PowerPC condition-register fields
and MIPS compare idioms have the same shape. **Box64's Kildall flag propagation
attacks the same target independently.**

**So the next step is a number this project has never had: inflation per fork.**
Host instructions emitted per guest instruction, same guest block, IR fork
against non-IR fork. **A disassembly count — the exact method that settled the
target-features question — needing no device and no fork modification.**

See [`research_log/20260823_1642_ir_in_emulators_literature.md`](research_log/20260823_1642_ir_in_emulators_literature.md).

**This does not say delete xenia's HIR.** 35x is one proof of concept on one
pair, and an IR buys optimisation passes and a place to put correctness. **It
says the IR belongs on the same list as the seven device layers, and nobody had
put it there.**

**Add one question to the extraction procedure: what variability does this
serve?** If the answer is variability the Thor does not have, the operation is
deletion and the estimate should be **smaller** than one fork's implementation.

### Five operations, and how to choose

| # | Ask | If yes | Licence cost |
| --- | --- | --- | --- |
| 1 | Can the binary, device or product tolerate **two** of these? | if **no** → **UNIFY** | inherits source |
| 2 | Does it serve **variability the Thor lacks**? | if **yes** → **DELETE** | none |
| 3 | Must these merely **coexist without colliding**? | if **yes** → **ISOLATE** | none |
| 4 | Has one fork **learned** something the others have not? | if **yes** → **PROPAGATE** | **none** |
| **5** | **Is its output a pure function of guest content and host configuration?** | if **yes** → **PERSIST** | **none** |
| — | none of the above | **leave it alone** | — |

**Row 5 was added 2026-08-23 and it is the one that does not shrink on reading.**
Every other operation moves source around; this one moves **computed results**.
Ask it of any hot path: if the answer is yes, that work should be done **once for
everybody**, not once per launch per user.

**It is already proven twice inside the fleet, on both sides, and neither half
knows the other exists.**

- **CPU. ARMSX2 has a persisted JIT for the PS2 vector unit** —
  `pcsx2/arm64/microVU_ProgCache-arm64.*`, 2,576 lines, three test files,
  content-addressed `.vuprog` payloads, and a **placement-relative fixup table
  that makes the emitted vixl output relocatable.** Its own test states the
  result: the program "runs with **ZERO block compiles and a bit-identical
  post-state**".
- **GPU. Cemu ships a cache merger** — `src/tools/ShaderCacheMerger.cpp` folds
  another user's `shaderCache/transferable` into yours. **That is community cache
  pooling, shipped**, and it is why Wii U shader caches circulated for years.

**Take ARMSX2's validity key wholesale.** `mVUbuildOptionsSentinel` is a 64-byte
fixed-layout snapshot of every option that changes emitted code, including three
FPCR masks and the recording flag itself. Three rules travel with it: a
`static_assert` on the size so layout drift is a compile error, a reserved tail
so a new option does not shift the fields below it, and **reclaim a reserved byte
only where zero means "feature off"**, so enabling a feature does not evict the
cache of every user who never turns it on.

**That last rule is the one `app/shell/.../BlockCacheKey.kt` does not have.**

**And ARMSX2 queued its own device experiment.** `Pcsx2Config.cpp:463` sets the
cache default-off "until the on-disk cache is validated on the target hardware".
**The target hardware is the Thor.** See `DEVICE_QUEUE.md` entry 17.

**The limit, stated.** Nothing here is measured — no boot time, no cache size, no
hit rate. And **the persist rule does not apply to everything**: an artifact
whose output depends on live guest state is not a pure function of content and
configuration.

See [`research_log/20260823_2205_translate_once_ship_it.md`](research_log/20260823_2205_translate_once_ship_it.md).

**The last row is the one this repo kept skipping.** Every candidate that shrank
belonged in it.

### Propagation has a precondition this fleet does not meet

**PROPAGATE is the operation this project was founded to do**, and recent work
says what makes it work. **BackportBench** (arXiv:2512.01396) benchmarks agents
at backporting patches into **divergent** codebases and names three failure
modes — version divergence, dependency complexity, and patches scattered across
files. **All three describe this fleet exactly.**
**Environment-in-the-Loop** (arXiv:2602.09944) argues migration changes
character when the **build and test environment is in the loop**: the agent gets
concrete errors instead of pattern-matching. *Neither paper's headline number
could be extracted from its PDF, and none is quoted here.*

**So an agent needs to build and test a fork to propagate into it — and this
fleet mostly cannot.** melonDS builds in 15 min 27 s; **Vita3K does not build at
all**; six forks have never been attempted; and almost nothing has tests.

**That reframes Phase 0.3 from housekeeping to the enabling condition for the
whole agentic thesis**, and it reframes today's ABI, AGP and recipe findings as
the difference between a fork an agent can work on and one it cannot.

**A propagation lands with a test, or it does not land.**

See [`shared_layer/UNIFICATION.md`](shared_layer/UNIFICATION.md).

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

**CORRECTED 2026-08-24. Both halves were wrong, and by different amounts.**
The 2026-08-23 survey counted `pInputAttachments` across seven forks and read
every hit. **The reading was right; the vocabulary was one word wide.** The
interesting half is spelled `subpassInput` and `subpassLoad` in the shader, and
`vkCmdNextSubpass` in the command stream.

| Fork | Input attachments | **Multi-subpass merge** |
| --- | --- | --- |
| **xenia** | yes, with a `subpassInput` shader variant | **YES, a 2-subpass merged feedback pass** |
| **ARMSX2** | yes, depth `subpassLoad` read in tile | no — **`MAX_SUBPASSES = 1`**, a self-dependency |
| **rpcsx** | yes, a real non-zero array | no — `subpassCount = 1` |
| Cemu, eden | `inputAttachmentCount = 0` initialisers | no |

**xenia's is `gpu_vulkan_feedback_merge`**: producer in subpass 0, the same-pixel
consumer reading it as an input attachment in subpass 1, with a **shader
variant** emitted by the SPIR-V translator and the **same retroactive
BeginRenderPass patch** already recorded for load and store ops.

**ARMSX2's is "mobile ROV"**, opt-in via `HWROV`: read depth in tile through a
depth input attachment so SW-Z, DATE, alpha-test and AA1 passes **fuse in-pass
rather than round-tripping**. It needs **ROAA on the depth aspect**, which
xenia's audit probed as **available on this device**. `DEVICE_QUEUE.md` entry 19.

**A merge is one way to keep data in tile memory. It is not the only way** —
ARMSX2 gets the same benefit with one subpass and an ordered read.

**Both are default-off and unmeasured, and both are title-shaped**: xenia's is
named for Blue Dragon, ARMSX2's targets specific PS2 idioms. **Neither is a
general pass-merging framework.** See
[`research_log/20260824_0210_pass_merging_exists_twice.md`](research_log/20260824_0210_pass_merging_exists_twice.md).

**CORRECTED 2026-08-23: xenia resolves MSAA on-chip, and xenia plans render
passes.** Both claims above were wrong, and the four-fork ranking omitted xenia
entirely because **the earlier survey read the render pass *cache*** — xenia's
planning lives in `vulkan_command_processor.cc`, driven by draw analysis.

**Four things it does that nothing else does:**

1. **On-chip MSAA resolve with the store elided.**
   `subpass.pResolveAttachments = bd_color_resolve ? bd_resolve_refs : nullptr`,
   and the multisample colour gets `STORE_OP_DONT_CARE` — *"the MSAA color need
   not be stored (only the resolved 1x is kept)"*. **That is the whole point of
   an on-chip resolve on a tiler.**
2. **`LOAD_OP_DONT_CARE` proven, not assumed.**
   `gpu_edram_passes_dont_care_safe` verifies the pass's first draw covers the
   whole render area **by replaying its vertex positions on the CPU**, per-pass
   and per-attachment, and **any uncertainty falls back to loading.**
3. **A depth path that knows `STORE_OP_NONE` is not `DONT_CARE`** —
   `STORE_OP_NONE` **preserves** the depth EDRAM memory where `DONT_CARE` would
   undefine it and corrupt aliasing render targets.
4. **It patches the pass begin retroactively.** It records
   `vkCmdBeginRenderPass`, accumulates per-attachment coverage as the pass runs,
   then rewrites the recorded command — **because load and store ops do not
   affect Vulkan render pass compatibility**, so the framebuffer and every
   pipeline stay valid. An ineligible pass simply keeps its original begin.

**Item 4 is the part to take.** It removes the hardest constraint on planning
attachment operations: that a renderer usually cannot know at `BeginRenderPass`
what the pass will contain. **It is BSD and already written.**

**So the premise below is half wrong.** A shared render graph does **not** add
planning nobody has — xenia has it. **It should take xenia's mechanism and its
three provers, and keep the propagation list for the six forks that lack any.**

**The provers are guest knowledge** — `RB_DEPTHCONTROL`, the guest clear idiom,
EDRAM tile rounding — **the mechanism is not.**

**Nothing is measured.** Every flag read is **off by default** and marked
`EXPERIMENTAL (Thor/TBDR)`.

See [`research_log/20260823_1626_xenia_plans_render_passes.md`](research_log/20260823_1626_xenia_plans_render_passes.md).

### The propagation list

Every item is already written somewhere in the fleet. None needs invention.

| Take | From | Give to |
| --- | --- | --- |
| Transient colour attachments — **correctness and hygiene, NOT a performance play**; the `LAZILY_ALLOCATED` memory half is unbound fleet-wide and **would buy nothing here** | Vita3K for the ops | everyone |
| Depth and stencil `DontCare` by default | Cemu | eden, azahar |
| `eClear` instead of `eLoad` when the pass clears | azahar | eden — **but see below: measured null once** |
| **Persisting relocatable emitted code, with a validity sentinel** | **ARMSX2** | **eden's NCE patcher, which has the key type and no cache** |
| **Whole-text AOT patching keyed by build ID** | **eden NCE** | anything with a same-ISA or near-ISA guest |
| **Merging another user's cache into yours** | **Cemu `ShaderCacheMerger`** | everyone |
| **Requesting `VK_KHR_shader_float16_int8`** | eden | **ARMSX2, whose frame generator is forced to fp32 without it** |
| **The ADPF hint-manager interface, with its no-op tier** | **melonDS** | **azahar, Vita3K, eden and GameThor, which have none** |
| **Reading `getThermalHeadroom` to close the loop** | xenia, rpcsx | everyone reporting work duration open-loop |
| **Storing the guest's declared activity state** | — | **eden, Vita3K and azahar all receive it and discard it** |
| `COLOR_ATTACHMENT_OPTIMAL` instead of `GENERAL` | — | Cemu |
| **`fastmem_exclusive_access` DERIVED from fastmem availability, plus a `CpuAccuracy` tier with ~10 named sub-options** | **eden** | **Vita3K, which leaves it at the default `false` and asked for exactly this surface** |
| **The C++20 once-guard: `test(relaxed)` before `test_and_set()`** | **Vita3K** | anything still on the C++17 idiom |
| **`+lse` in the arm64 baseline** | **xenia, Vita3K** | **six forks** |

**Three forks share one JIT and configure it three ways, found 2026-08-25.**
`dynarmic` is vendored by eden, Vita3K and azahar. **eden runs the fastmem arena
with exclusive access on, derived rather than chosen** —
`config.fastmem_exclusive_access = config.fastmem_pointer != std::nullopt` — and
wraps every option in a named setting behind a `CpuAccuracy` tier. **Vita3K runs
the arena with exclusive access off**, so guest `LDREX`/`STREX`, which the Vita
kernel uses for every lock, fall out to callbacks. **azahar does not use the
arena at all**; it takes `config.page_table`, the per-access lookup, so the
question does not arise there.

**eden's configuration surface is this project's per-game override design,
already built, for the exact subsystem Vita3K says needs it**: one tier for
somebody who wants a single switch, named sub-options for somebody who does not,
and a default that follows from a fact. **It must stay a declared per-backend
extension** — a CPU-accuracy tier is meaningful for three dynarmic backends and
meaningless for ARMSX2's emitters, which is the texture-class rule again.

See [`research_log/20260825_0150_once_guards_and_three_dynarmic_configs.md`](research_log/20260825_0150_once_guards_and_three_dynarmic_configs.md).

**Two of those rows are the same missing piece seen from both ends, found
2026-08-23.** **eden patches a Switch module's whole text segment ahead of time**,
keyed by the 32-byte NSO build ID, with relocation and trampolines — Rosetta's
shape — **and then throws the result away, because `PatchText` runs from the
loader on every launch.** Its `PatchCacheKey` type is declared, given a hash
specialisation, and **used nowhere.** **ARMSX2 already built the cache that key
was declared for**, for the PS2 vector unit, with tests. **Neither fork cites the
other.**

**And eden's NCE is the DELETE operation applied to pipeline 1**, code
translation — the deepest reach into a core, which this file orders **last**
precisely because of that depth. **eden runs Switch guest code natively on this
host with no recompiler at all**, because its guest ISA is the host ISA. This
file already recorded that fact as a *difficulty*. **It is also the opportunity.**

See [`research_log/20260823_2250_eden_nce_deletes_the_translator.md`](research_log/20260823_2250_eden_nce_deletes_the_translator.md).

**A FIFTH FORK, AND IT IS WORSE, 2026-08-24.** **rpcsx uses `LOAD_OP_CLEAR` zero
times** — colour, depth and stencil are all `LOAD` and `STORE` unconditionally,
so **even a full-screen clear unresolves the attachment into GMEM first.**

**And rpcsx fixed it and measured no saving.** Correct, applying to 100% of
clears, and **12.39% against 12.65% GPU busy at an identical 615 MHz clock** — so
devfreq is not hiding a win. **Two unseparated explanations: Turnip may already
fold a full-surface `vkCmdClearAttachments` into the load op, or the workload was
too light.** Its instruction: **check what the driver already folds before
blaming the scene.**

> **So treat the two attachment-op rows in the propagation list as correctness
> and hygiene, not as performance**, until somebody checks what Turnip folds.

**And the instrument is one this file never recorded:**
**`/sys/class/kgsl/kgsl-3d0/gpubusy`** — a cumulative busy/total pair that
**resets on read**, needing no root and no perfetto.

**MEASURED ON THE DEVICE 2026-08-16, AND IT CHANGES WHY THESE MATTER.** At a
representative shape — 1280x2048 with depth and up to 256 overlapping draws —
xenia measured **the GPU frame as ALU-bound, not bandwidth-bound**: blend free,
2x bytes/pixel **+8-10%**, **flat across a 36x working set with no cache cliff**,
the EDRAM-span render target **+1.5%**, and **forced GMEM never beating autotune**
— parity from 16 draws up, **+157% worse at one draw.**

> **Any design whose payoff is "fewer framebuffer bytes" is dead on arrival on
> this device.** That kills **transient and `LAZILY_ALLOCATED` attachments and
> subpass merging as PERFORMANCE plays** — ARM measures them at 45% fewer reads
> and 56% fewer writes, **which is a large saving of a resource this device is not
> short of.** xenia's routing table: **"Do not re-derive them."**

**So keep the attachment-op rows as correctness and hygiene**, and expect no
frames from them.

**What IS expensive there, over 159 gameplay frames: EDRAM ownership transfers.**
**45 per frame**, `pass_break_rt_change` **27**, and **24 of the 45 change no
format at all** — moves, not reinterpretations. **A third of that title's passes
exist only to service them.** See
[`research_log/20260824_2000_the_fork_had_a_routing_table_and_i_did_not_read_it.md`](research_log/20260824_2000_the_fork_had_a_routing_table_and_i_did_not_read_it.md).

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

### AUDITED 2026-08-23: all three are already implemented, in xenia

**`a64_backend.cc` carries 111 tuning flags**, and the three leads below are
among them, with primary-source citations and partial measurements. **Most are
off by default with the trade stated**, which is why no feature-shaped search
found them.

| Lead | xenia flag | State |
| --- | --- | --- |
| Guest FP status serialising | `a64_fpcr_single_mode`, `a64_fpcr_switch_census` | off, census first |
| Spill GPRs to the vector file | `a64_spill_gprs_to_vector` | off, **measured**: `UMOV` latency **2** against `LDR` **4** |
| A710 lane-assembly stall | `a64_vmx_nan_fixup_branchless` | off, pending a qemu differential |

**The FP lead had the wrong root cause here.** It is not that emulators are
faithful for its own sake. **The Xenon has two independent FP mode registers —
`FPSCR` for scalar, `VSCR.NJ` for VMX — and ARM64 has one.** The two modes
differ by one bit, `FZ`, so every transition rewrites `FPCR`, and the A710 SWOG
Table 4-3 note 2 says a write that changes the control fields **introduces a
barrier**.

**ARMSX2 has the best answer in the fleet, in three parts**, and the third is
what makes the first two safe:

1. EE `DIV`/`SQRT` **bake the FP environment into the block as an immediate**, so
   recompiled code never reads `FPCR`.
2. mVU **gates the write** — `mvuNeedsFPCRUpdate` skips it when it already
   matches.
3. mVU **hashes all four FP environments into its block-cache sentinel**, so a
   block compiled under one can never be reused under another.

**Parts 1 and 2 without part 3 are a correctness bug.** Baking a mode into
generated code makes that code wrong the moment the mode changes.

**Its ARM64 JIT emits no `MRS`/`MSR FPCR` at all**, and PS2 needs three FP
environments — `FPUFPCR`, `VU0FPCR`, `VU1FPCR` — not one.

**Four positions across the fleet:** bake it (ARMSX2), switch it and measure the
switching (xenia), set `FZ` once and leave guest `FPSCR` writes unimplemented
(Cemu), have no guest FP mode at all (melonDS).

**AND FP CONVERSION IS A SEPARATE, LARGER RISK THAN FP MODE — found 2026-08-24.**
This section discusses rounding and denormals. **The correctness bug the fleet
actually shipped was in float-to-int conversion.**

`fptosi`/`fptoui` are **poison on overflow**, so shared emulator code bolts a
correction on by hand **and that correction is written for x86.** On x86,
`CVTTPS2DQ` returns `0x80000000` on overflow, so the code XORs it to
`0x7fffffff`. **On ARM64 `FCVTZS` already returns `0x7fffffff`, and the same XOR
turns it into `0x80000000`.**

> **rpcsx's SPU `CFLTS` was incorrect on ARM64 — every value at or above 2^31,
> upstream included.** Its sibling `CFLTU` was correct but redundant.
> **`llvm.fptosi.sat` fixed the bug and shortened four instructions to one.**

**The general rule, from the fork that found it:** *"on a target whose hardware
semantics are stronger than the IR's, a portable correction can be worse than
nothing."* **This is the most dangerous x86-detour form: the others cost speed,
this one changes results.**

**ARMSX2 shows the correct method**, in `pcsx2/arm64/iCOP2-arm64.cpp`: *"ARM64
NEON `Fcvtzs` returns 0 for a NaN input, but the PS2 saturates NaN to a
sign-based INT_MAX/INT_MIN. **Finite overflow and ±Inf already saturate correctly
in `Fcvtzs`; only NaN lanes need the fixup.**"*

> **Enumerate what the host already does correctly, and fix only the delta.** The
> ARM64 branch of any guest operation should say which parts the hardware already
> provides.

**AND THE FORK THAT FOUND IT CALLS THIS ITS MOST PRODUCTIVE HEURISTIC:**

> **Auditing x86 *corrections* has high yield where auditing opcodes has low
> yield** — **every real defect in this codebase was shared code compensating for
> an x86 quirk, never LLVM picking a bad instruction from a clean description.**

**Six defects, not one:** `CFLTS`, the `FCTIW` family, `VPKUHUS`, `bswap.i128`,
`mov_rdata`, `VMSUMSHS`. **That is the strongest support anywhere in this fleet
for the "every win was a bug" pattern**, and it is the same fork's own summary of
its own record.

**It has a mechanical tell: an XOR against a sign-extended comparison**, the shape
a saturation fix-up takes. **Swept across both its translators: three sites, all
correct** — two are the fix working as intended, and **one is the guest
architecture's own semantics**, which is why the lens **needs reading rather than
pattern-matching.**

**The exhaustion is bounded**: that shape is done; **a correction spelled as a
`select`, a clamp before a conversion, or a literal limit would not match.** **ARMSX2's comment does; rpcsx's did not, and that is where the bug
> lived.**

**AND THERE IS A STRUCTURAL REMEDY, which is the design rule for the shared
layer.** ARMSX2 is clean of this whole class, and not by care alone: **it wrote
dedicated ARM64 files** — `GSVector4_arm64.h`, `GSVector4i_arm64.h`,
`GSDrawScanlineCodeGenerator.arm64.h`, and the whole `pcsx2/arm64/` tree — **so
the x86 intrinsics in its source sit in paths only an x86 build compiles.**

> **A separate ARM64 file cannot inherit an x86 correction by accident. A shared
> file behind a portability shim can.**

**And the shim diverges in the other direction too, undocumented.**
`sse2neon`'s `_mm_cvttps_epi32` is **plain `vcvtq_s32_f32`**, so it returns
**ARM's saturating result** — `0x7FFFFFFF` on overflow, `0` on NaN — where the
x86 intrinsic it is named after returns **`0x80000000` for both.** **Its comment
is an MSDN link with no note that the edge cases differ.**

> **Both failures have one root: a boundary that claims to be x86 and is not, in
> exactly the cases nobody tests.**

**ARMSX2 avoids it a second time, and the mechanism is the rule.** Its conversion
branches on architecture **inside the same function** — `#if defined(ARCH_X86)`
against `#elif defined(ARCH_ARM64)` — and **reaches the same `vcvtq_s32_f32` the
shim would have, deliberately and visibly**, with a comment about rounding mode.

> **So the rule is sharper: make the architecture choice visible where the
> operation is written.** A separate file does that by construction, an `#if` in
> the function does it explicitly, **and a shim does neither.**

**AND THE SHIM SHIPS OPT-IN FLAGS FOR ITS OWN DIVERGENCES, DEFAULTED OFF.**

```c
#ifndef SSE2NEON_PRECISE_MINMAX
#define SSE2NEON_PRECISE_MINMAX (0)     /* _mm_min|max_ps|ss|pd|sd */
#endif
#ifndef SSE2NEON_PRECISE_DIV
#define SSE2NEON_PRECISE_DIV (0)        /* _mm_rcp_ps and _mm_div_ps */
#endif
```

**With the flag**, `_mm_max_ps` is `vbslq_f32(vcgtq_f32(a,b), a, b)` — take `a`
**only when strictly greater**, so a NaN comparison falls through to `b`, which
is x86's rule. **Without it**, plain `vmaxq_f32`. **The shim's own comment names
the symptom:** *"would solve a hole or NaN pixel in the rendering result."*

**Searched all four forks that carry `sse2neon` — Cemu, rpcsx, ARMSX2 and eden —
in their build files and their own source: none defines either flag.** So all
four get the fast paths, **and this is a visual-correctness difference that would
be blamed on the emulator's renderer rather than on a shim default.**

**It also has a hand-written sibling.** xenia's ledger carries `vmaxfp/vminfp a64
NaN fixup` as **`OPEN`** — the same problem in its own emitter. **Guest min/max
NaN semantics is unresolved in this fleet in both forms.**

**And note which rule is actually wanted.** x86's `MAXPS` behaviour is **not
automatically the guest's** — it matters only because these emulators were
written on x86. **The PS2, PS3, Xbox 360 and Wii U each define their own**, and
the shared layer should write a guest `max` against the guest's rule rather than
inherit either host's by accident. See
[`research_log/20260824_1710_sse2neon_precision_flags_are_off.md`](research_log/20260824_1710_sse2neon_precision_flags_are_off.md).

**That is the same DELETE argument as everywhere else in this file, pointed at
`sse2neon` and its relatives.** A shim exists so one source can serve two
machines; **it is also the vector through which an x86 assumption reaches ARM64
silently.** **This project has one host ISA. Prefer a per-target file over a
shared file behind a shim**, and the class above becomes unreachable rather than
merely audited. See
> [`research_log/20260824_1620_an_x86_correction_can_become_a_corruption.md`](research_log/20260824_1620_an_x86_correction_can_become_a_corruption.md).

**And the spill lead has an argument this repo did not have.** It is off in xenia
because **the 360 guest is a 128-vector-register machine already squeezed into
28**, so reserving vector registers worsens vector pressure to relieve integer
pressure. It only pays in integer-heavy, vector-light functions.

See [`research_log/20260823_1520_cpu_leads_already_done.md`](research_log/20260823_1520_cpu_leads_already_done.md).

**The original section follows, kept because the hardware facts in it are still
the reason these flags exist.**


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

**ASSESSED 2026-08-24 BY TWO FORKS, AND IT IS CLOSED FOR BOTH — for opposite
reasons.**

**rpcsx: the cost is real and the lever does not exist.** Its JIT emits `sp`
accesses as **10.1% of every instruction it emits — 51,474 of them.** But:

- **Reach.** **21,706 are 128-bit `q` spills — already vector, with nowhere
  better to go**, because there is no larger register file to spill a V register
  into. Only **12,777 scalar accesses** are candidates, about **2.5%** of
  emitted instructions.
- **Decisive: it does not choose where spills go.** The recompiler emits IR and
  **LLVM's register allocator decides placement.** *"Spill this GPR to a spare V
  register" is an allocator policy, not something expressible from the IR we hand
  over*, and its SPU JIT already uses the vector file hard for 128 guest
  registers.

> Its own words: **"a guide row that is true, aimed at a cost that is real, and
> still not a change we can make."** Recorded there **"so nobody re-derives it as
> an opportunity."**

**xenia: implementable, and measured off.** It hand-writes its emitter, so it
*can* do this — `a64_spill_gprs_to_vector` exists and it measured **`UMOV`
latency 2 against `LDR` 4.** It is **off by default** because the Xbox 360 guest
is a 128-vector-register machine already squeezed into 28, **so reserving vector
registers worsens vector pressure to relieve integer pressure.**

**The architectural point is the useful part.** Whether this lever is even
*available* depends on whether a backend hand-writes its emitter or hands IR to a
compiler. **That is a third entry in the IR trade-off ledger**, beside `flagm`
and lazy flags: **an IR gives you the optimiser and takes away control.** See
[`shared_layer/TRANSLATION.md`](shared_layer/TRANSLATION.md).

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

#### AUDITED 2026-08-23: the fleet barely repurposes hardware at all

**Using a named-for-crypto unit for a different job is a real technique. This
fleet uses one instance of it, and that instance is `DEAD`.**

**Counting mnemonics gave a completely wrong picture; reading the hits fixed
it.** eden's `CRC32`, `AESE` and `PMULL` matches are **dynarmic decoding the
guest's own crypto instructions** — its guest ISA is the host ISA, so every ARM64
mnemonic appears there. melonDS's `CRC32` is **an encoding table** in Dolphin's
emitter. Cemu's `AESE` is **genuine crypto**, Wii U content decryption. xenia's
`FJCVTZS` is **a feature-flag definition** used nowhere.

**The one genuine instance is xenia's `EOR3`/`BCAX`**, six files — and its
ledger records `EOR3/BCAX fusion for VMX bitwise chains` as **`DEAD`,
2026-08-06.**

**A second, non-crypto case was also chased and closed.** `TBX2` costs about
**2x** `TBL2` on this SoC — 0.377 ns against 0.178 on the A715 — and xenia found
its SHUFB lowering used the slower one. **The fix measured null**: `TBL2` plus an
`ORR` came out 0.555 against 0.555.

**The best unexploited candidate is carryless multiply for texture swizzle.**
Unswizzling a console texture is **bit deinterleaving**, which is what `PMULL`
does in one instruction. **Every fork has a large swizzle surface and none uses
it** — xenia 71 files, Cemu 49, Vita3K 47, azahar 45, ARMSX2 33, **`PMULL`
host-side uses: zero.** It also sits in **pipeline 2**, so it would land in a
hot shared path rather than one backend's lowering. **Unproven — no fork's
swizzle code has been read.**

**Hardware `CRC32` is unused everywhere** (searched twice), **but do not assume a
win.** The ~20x in the literature is software CRC32 against hardware, **not
CRC32 against xxHash**, and the fleet already uses xxHash heavily. **Where it
plausibly pays is short keys** — `crc32cx` hashes 8 bytes per instruction — which
means **block validation, not texture hashing**. ARMSX2 validates recompiler
blocks with a software CRC32 in `iR3000A.cpp` and `iR5900.cpp`.

**`FJCVTZS` is not applicable. Record it and move on.** `FEAT_JSCVT` exists to
give **x86 and JavaScript** float-to-int semantics on ARM. **Our guests are
PowerPC, MIPS and ARM.** Rosetta and FEX need it; we do not.

**The warning matters as much as the opportunity.** The technique is largely
untried here — **and both times anyone did try it, the result was null.**

**QUALIFIED 2026-08-25: the `DEAD` verdict is about applicability, and there is a
SECOND axis nobody measured — the shape of the use.** rpcsx's `tools/bcax_bench.c`
times the instruction itself on this silicon, per core, **with no game and no
boot.** BCAX against the two-op form, best of five:

| shape | X3 | A715 | A510 |
| --- | --- | --- | --- |
| **latency, serial chain** | **1.96x** | **2.01x** | **2.00x** |
| throughput, 4 independent chains | **0.94x** | 1.00x | **2.02x** |

> **The big cores have enough vector pipes to issue the old pair in parallel, so
> a wider instruction wins nothing there and can lose slightly. It wins when the
> result feeds the next instruction.**

**So "three audits, three empties" is still right about candidates and incomplete
about value.** A throughput-bound use on a big core gains nothing even where
candidates exist; **the same instruction is a 2x latency win where the consumer
is the next instruction.** rpcsx checked its own lowering rather than assuming —
its `SHUFB` emits `bcax` immediately followed by the `tbx` that consumes it.

**Take the method more than the number.** A standalone microbenchmark, on the
device, per core, answering a codegen question **without booting a game**. This
project has no such harness and `DEVICE_QUEUE.md` is full of questions shaped
exactly like that one.

**THIRD AUDIT, 2026-08-24, AND IT IS THE STRONGEST NUMBER AGAINST THIS LANE.**
rpcsx audited **seven** x86 SIMD tricks against what its JIT actually emits:
`PSADBW`, `SUMB`, `VPDPBUSD`, `VDBPSADBW`, `GF2P8AFFINEQB`, `GBB` and `FCGT`.
**Four are already optimal, two have no ARM equivalent, one is never reached.**

> **No item is a candidate. Together the seven lowerings account for about 210 of
> 509,424 emitted instructions — 0.04%.**

**Three audits, three empties.** Treat any new instruction-repurposing proposal as
requiring **an applicability count before a build**, per the `EOR3` rule above.

**And it supplies a census method better than "read the hits".** rpcsx's own
notes had recorded **1,661 `udot` instructions as proof the dot-product
optimisation was taken.** Classifying every `udot` by **the instructions that
define its two vector sources** showed **1,338 come from SPU block verification —
not a guest opcode at all — and only 9 are the actual `SUMB` lowering.** The
attribution was confirmed by a **control instruction**: `addv s, v.4s` appears
615 times, one per verification site, and 615 x 2.7 pairs gives 1,664.

> **The count was correct and the attribution was not.** The question asked was
> *"is this instruction present"* rather than *"which lowering emitted it"*.

**Reading the hits is not enough for generated code**, because a JIT emits one
instruction from many lowerings and there is no comment to read. **Classify by
operand provenance, then cross-check with a control that should appear once per
site.** Device-free, and it is how a disassembly count is made to mean something.
See [`research_log/20260824_1015_seven_tricks_all_already_optimal.md`](research_log/20260824_1015_seven_tricks_all_already_optimal.md).

See [`research_log/20260823_1755_hardware_instruction_repurposing.md`](research_log/20260823_1755_hardware_instruction_repurposing.md).

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

#### AUDITED 2026-08-23. The check is right. The obvious fix is measured harmful.

**Measured on this SoC**, by rpcsx's `tools/bench/thor_bench.cpp`, recorded in
its `docs/arm64/bench-results.md`:

| shape | ns per iteration |
| --- | --- |
| `yield` | **0.36** |
| `nop` | 0.36 |
| `isb` | **11.42** |
| armed `wfe` | **72,024** |

**`YIELD` is exactly a `nop` on the silicon, not just in the manual.** And
**`ISB` costs 32 times a `yield`.**

**That 32x is why swapping the instruction alone regresses.** Spin counts were
hand-tuned around an instruction that costs nothing, so substituting one that
costs 32x more multiplies every tuned backoff. **Three forks measured it
separately:** rpcsx **+23% regression**, xenia's equivalent A/B **`CONFOUNDED`**,
and **Cemu tried the tidy fix** — make the backoff time-based to restore x86
intent — and it was **worse on the Thor**, `LatteCP_readU32Deprc` going 5.13% to
7.12% of total CPU. Its comment ends **"leave the count alone."**

> **The backoff instruction and the iteration count are one tuned pair.**
> Changing either alone is a regression risk, and the correct total backoff time
> is per-loop, not a fleet constant.

**AND THE ITERATION COUNT IS THE LARGER HALF, MEASURED 2026-08-24.** rpcsx
profiled a healthy 60 fps run with `simpleperf` — **31,657 samples, 0 lost**,
build ID verified — and found **73.9% of all CPU cycles in two lv2 wait
functions.** The call graph proves it is spinning rather than blocking: both
report **self time and neither reaches `atomic_wait_engine::wait`**, while
`sys_timer_usleep` in the same profile does.

**The cause is a constant that was correct on x86:**

```cpp
for (usz i = 0; cpu_flag::signal - ppu.state && i < 50; i++) { rx::busy_wait(500); }
```

**`busy_wait` counts generic-timer ticks and `CNTFRQ_EL0` is 19.2 MHz here**, so
500 ticks is **26 µs** and fifty of them is **1.3 ms per pass**. **The same line
costs about 1 µs on a 3 GHz TSC.** Its own conclusion: *"`ISB` costs 23% more,
`nop` is equivalent. **Neither of those is the fix — the instruction is not the
problem, the 1.3 ms is.**"* **Eight sites, the whole guest synchronisation
layer.**

**This is a third kind of x86 detour** — not instruction selection, not the
register model, but **a timing constant that assumed a fast free-running
counter** — and it is the largest of the three.

**Vita3K already has the cure**, and its comment names the disease: it derives
the spin budget from `CNTFRQ_EL0` **"rather than from an iteration count tuned on
x86"**, and notes such budgets are *"wall-clock stable across cores and SoCs,
unlike the x86-tuned iteration constants they replace."* **It reads
`mrs cntfrq_el0` directly and falls back to iteration counts only where no such
counter exists.**

**That also resolves the `ISB` disagreement.** Vita3K adopted `ISB` citing a net
power win; rpcsx measured `ISB` as a regression. **Vita3K changed both halves of
the pair at once** — the instruction *and* a wall-clock budget. **Take the pair,
never the instruction alone.**

**Two device facts worth keeping:** **`CNTFRQ_EL0` is 19.2 MHz**, and
**`FEAT_WFxT` is absent**, so `WFE` cannot carry a timeout on this chip.

See [`research_log/20260824_0810_the_spin_is_a_timing_constant_not_an_instruction.md`](research_log/20260824_0810_the_spin_is_a_timing_constant_not_an_instruction.md).

**Three tiers exist in the fleet, and the best one exists twice:**

| Tier | Mechanism | Where |
| --- | --- | --- |
| **1** | **`SEVL`/`WFE` + `LDAXR`, park on the address** | **ARMSX2** `MonitoredWait`; **dynarmic** `EmitSpinLockLock`, **0BSD** |
| 2 | `ISB` backoff | Cemu, Vita3K, xenia |
| **3** | **`asm("yield")`** | **eden** `Common::SpinLock`, 2 hot sites |
| — | no host spin loop | melonDS |

**ARMSX2's is fork work**, not upstream, and it carries its own numbers: **3.5
wake-ups per wait as written against 6708 with a `CLREX` added**, because
clearing the monitor is itself a wake event. It cites Linux's arm64 `__cmpwait`.
**dynarmic reached the identical shape independently** — the same convergence
signal as three forks choosing Oboe.

**eden ships both.** It vendors dynarmic, so the correct 0BSD spin lock is
already in its binary, while `Common::SpinLock` still spins on `yield` in
`k_slab_heap.h` and `KThread::m_context_guard`. **The fix is a DELETE, not a
rewrite.**

**How long to spin matters more than which instruction, and only two forks
decided it.** ARMSX2 **calibrates at run time** — it times its own backoff on the
host it booted on, so the 32x problem cannot reach it — and Vita3K derives a
wall-clock budget from **`CNTFRQ_EL0`**. xenia and eden have no budget at all.

See [`research_log/20260823_1454_spin_wait_audit.md`](research_log/20260823_1454_spin_wait_audit.md).

**A related survey, 2026-08-23: two forks set no host affinity at all.** Four do
— xenia, ARMSX2, eden and rpcsx. **melonDS sets thread priority but never
affinity, and Vita3K sets neither.**

**melonDS is the case worth naming**, because it is the one fork that tuned its
compiler for the prime core with `-mtune=cortex-x3` and reasoning written into
its `CMakeLists.txt`. **It schedules its code generation for the X3 and then
never asks for the X3.** Two decisions, two files, nobody holding both.

**A warning for that survey and any like it.** Searching for
`sched_setaffinity` and `CPU_SET` returns **guest code**: Cemu's hit is the
Wii U `coreinit` thread API, azahar's is a 3DS kernel syscall, and rpcsx's is
Orbis kernel emulation. An emulator implements the guest's affinity API **as a
feature**. A fourth hit was inside vendored Oboe's test app. **Separate
host-side from guest-side before counting anything.**

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

**AND MEASURED FROM THE BUILD OUTPUT, 2026-08-24.** `tools/emitted_flags.py`
reads every `compile_commands.json` — **the command line each translation unit
was actually compiled with.** Four forks have been built on this machine.

| Fork | `-march` that reached the compiler | `-flto` | LSE |
| --- | --- | --- | --- |
| **ARMSX2** | **Release: `armv8-a+crc`**; Debug/tools: `armv8.2-a+fp16+dotprod` | absent | **no** |
| **azahar** | `armv8-a`, `+crc`, `+crypto`, `armv8.2-a+dotprod+i8mm`, and **per-file `armv8.5-a+i8mm+sve2`, `armv9-a+sme`** | **`-flto=thin`, 2,183 TUs** | **no** |
| Cemu | **none on arm64**; passes `-moutline-atomics` | absent | **no** |
| melonDS | **none on arm64**; `armv7-a` on the 32-bit ABI | absent | **no** |

**Three things follow.**

**Not one of the four carries `+lse`, and `-mno-outline-atomics` is absent from
every build**, so **every atomic in these binaries goes through
`__aarch64_ldadd8_acq_rel`.** Cemu's is deliberate; the other three simply never
set anything and **the NDK's Armv8.0-A ABI does it for them.**

**azahar is the only fork confirmed to get LTO**, which the build-file census
could not establish.

**ARMSX2's shipping Release build is the baseline one** — `armv8-a+crc` — while
its Debug and tools builds get `armv8.2-a+fp16+dotprod`. **The fork has an 8.2
line and it is not the one that ships.**

### azahar's `armv9-a+sme` is safe, and that is the rule

Those flags are **per-file**, on libyuv's dedicated `row_sve.cc`, `row_sme.cc`,
`rotate_sme.cc` and `scale_sme.cc`, **dispatched at run time** —
`if (TestCpuFlag(kCpuHasSME))`.

> **A library with per-file flags and runtime dispatch is safe to give aggressive
> `-march`. A library that dispatches at compile time is not.**

**So the SVE danger above is not `-march=armv9-a` in itself — it is a GLOBAL
`-march=armv9-a` reaching a compile-time dispatcher like xxHash.** **eden's
`YUZU_BUILD_PRESET=armv9` sets a global `-march`**, which is the dangerous form.

See [`research_log/20260824_1500_ground_truth_from_emitted_flags.md`](research_log/20260824_1500_ground_truth_from_emitted_flags.md).

**RE-MEASURED 2026-08-24 AND TWO ROWS ABOVE ARE WRONG.** Method: grep every
`-march`, `-mcpu`, `-mtune` and outline-atomics flag from each fork's own build
files — `*.txt`, `*.cmake`, `*.gradle`, `*.lua`, `*.mk` — with vendored trees
excluded, then resolve the CMake variables by hand.

| Fork | What it actually sets | Correction |
| --- | --- | --- |
| xenia | `-march=armv8.2-a+lse+crypto+sha3+crc+dotprod`, `-mno-outline-atomics`, **`-mtune=`**`cortex-a710`/`a715` | confirmed |
| **Vita3K** | **`-march=armv8.2-a+lse+fp16+dotprod`**, via `VITA3K_ARM64_BASELINE`. **No `-mcpu`, no `-mtune` anywhere.** | **row above is wrong** |
| **Cemu** | **`-moutline-atomics`** behind a `check_cxx_compiler_flag`. **No arm64 `-march` or `-mcpu` in its own build files**; the `-march=...+lse` match is inside a comment. | **row above is wrong** |
| melonDS | **`-mtune=cortex-x3`** | confirmed, and **`-mtune` is the safe spelling** |
| rpcsx | `-march=armv8-a+lse`, and a stray `-mcpu=cortex-a53` | partly confirmed |

**A literal grep missed Vita3K entirely**, because its flag is
`-march=${VITA3K_ARM64_BASELINE}`. **A flags census must resolve variables.**

### `-mcpu` would enable SVE. `-mtune` does not. That is why the target says `-mtune`.

**Tested on this box, NDK clang, `aarch64-linux-android33`:**

| Flag | SVE macros defined |
| --- | --- |
| `-march=armv8.2-a+...+rcpc -mtune=cortex-x3` | **none** |
| **`-mcpu=cortex-x3`** | **`SVE`, `SVE2`, `SVE2_BITPERM`, `SVE_BF16`, `SVE_MATMUL_INT8`** |
| **`-mcpu=cortex-a710`** | **the same** |

**Clang models the core, and these cores do implement SVE2 — Qualcomm simply did
not expose it.** So `-mcpu` is a trap on this device in exactly the way
`-march=armv9-a` is, and **`THOR_TARGET.md` naming `-mtune` rather than `-mcpu`
is load-bearing, not stylistic.**

**No fork currently uses `-mcpu=cortex-x3` or `-mcpu=cortex-a710`**, so this risk
is latent rather than live — **the live route is eden's `armv9` preset.**

### And Cemu's atomics comment inverts for this project

Cemu explains why it uses `-moutline-atomics` rather than `+lse` directly:

> `-moutline-atomics` keeps the binary running on pre-ARMv8.1 devices by
> dispatching at runtime, so it is safe as the default. **Building with
> `-march=...+lse` directly would be faster still but SIGILLs on hardware without
> LSE**; if that is ever wanted it should be an explicit opt-in build.

**That reasoning is correct for a portable build and inverted here.** This device
reports `atomics` in `/proc/cpuinfo`, so **`+lse` directly is the right call and
`-moutline-atomics` is a runtime dispatch tax paid for devices this project does
not have.** **xenia already sets `-mno-outline-atomics`.** A clean DELETE
instance, and the same shape as the SVE trap seen from the other side: **a
feature flag that SIGILLs on hardware lacking the feature.**

**xenia is the only fork enabling what the device actually has** — and it is
the fork that wrote the research. The rest compile for a generic ARMv8-A this
device stopped being years ago.

**CORRECTED 2026-08-23 by `tools/fleet_lint.py` on its first accurate run.**
An earlier version said no fork does both. **xenia does both**, in
`premake5.lua` — missed earlier because that survey searched CMake files:

```lua
"-march=armv8.2-a+lse+crypto+sha3+crc+dotprod", "-mno-outline-atomics",
"-mtune=cortex-a710"
```

**That is very nearly the line
[`THOR_TARGET.md`](hardware_ref/thor/THOR_TARGET.md) recommends, reached
independently** — another convergence result, like Oboe.

**melonDS sets `-mtune=cortex-x3` with its reasoning in the file. ARMSX2, Cemu,
azahar and Vita3K set neither.**

### xenia already measured what the features buy: PERMISSION, not speed

It enabled them and **disassembled the result** — 1,779,182 instructions — and
found clang emitted **zero** `eor3`/`bcax`/`rax1`/`xar`, **zero** `aes*`/`sha*`,
**zero** `crc32*`, **zero** `udot`/`sdot`.

> these flags change the compiler's **PERMISSION** and nothing else ... **Do not
> claim a speedup from this line.**

**This answers a queued device experiment without the device.** The reason to
set the flags is that hardware AES and SHA intrinsics **will not compile**
without `+crypto`, and `SDOT` cannot be hand-written without `+dotprod`.

**So the ARMSX2 opportunity below is one step further away than it reads.**
Fixing its flags will not make clang emit `SDOT`; **someone must write the
intrinsics**, and the flags are what lets that compile.

**Take the method.** For *does this flag change what is emitted*, **disassemble
and count** — no device, no scene noise, exact. Only *is it faster* needs the
Thor. See
[`research_log/20260823_0150_target_features_are_permission.md`](research_log/20260823_0150_target_features_are_permission.md).
ARMSX2 sets `-march=armv8-a` for Android deliberately while selecting
`armv8.4-a -mcpu=apple-m1` for Apple Silicon, so **it targets an M1 more
precisely than it targets the Thor.**

### MEASURED IN THE SHIPPED BINARIES 2026-08-24: `+lse` is the flag nobody sets

**This section has argued from build files. Six forks' `arm64-v8a` libraries were
disassembled instead**, with the standard row's `llvm-objdump`, and the flag
question turns out to have one measurable consequence rather than only
"permission".

| fork | direct LSE instructions | `ldaxr`/`stlxr` | outline call sites |
| --- | --- | --- | --- |
| **xenia** | **1,998** | 10 | **53** |
| Cemu | 38 | 60 | **406** |
| **ARMSX2** | **28** | 51 | **886** |
| Vita3K | 29 | 50 | not counted |
| azahar | 20 | 34 | not counted |
| melonDS | 6 | 10 | not counted |

**All six carry `__aarch64_have_lse_atomics`. Only xenia — the one fork that sets
`+lse` — emits LSE at scale.**

**CORRECTED WITHIN THE HOUR, and the correction is the bigger finding.**
**Vita3K's binary here is from 2026-05-18 and it fixed this on 2026-08-21**, so
its row describes a three-month-old artefact. **Record a build date beside every
binary measurement**; the other five were built within the last week.

| question | instrument | answer |
| --- | --- | --- |
| which shipped binaries dispatch | six binaries, five current | **four of five** |
| **which forks set `+lse` in current source** | build files, **all eight forks** | **two: xenia and Vita3K** |

**Vita3K's own numbers are much larger than the ARMSX2 figures above**, because
it counted the whole shipped library: **25,276 outline call sites to 682, and 30
inline LSE instructions to 24,552**, with `__aarch64_swp1_acq_rel` alone at
**12,074** — refcount traffic rather than cold paths.

**And its residual 682 is a consequence nobody in this repo had recorded:** they
are all inside **prebuilt vcpkg static libraries**, which vcpkg builds under its
own `arm64-android` triplet and which **never see the fork's `-march`**.

> **A prebuilt dependency does not inherit your compile baseline.**

**That interacts with a decision already made here.** `Cemu` is the fastest fork
to build precisely because **it compiles none of its dependencies** — so a
`-march` change reaches almost none of Cemu's atomics, and **testing Cemu would
measure the flag not applying rather than the flag not helping.** Reaching them
needs a custom vcpkg triplet, which is the same unit-of-work argument as the
shared device layer: **fix it once for the fleet or once per fork.**

**Vita3K rejected `-mcpu=cortex-x3` too, for a different reason than the SVE one
above:** *"that would also pick an X3-specific scheduling model, and the same
code runs on A510 little cores."* **Two forks, two independent reasons, neither
citing the other** — and it matches
[`CORE_COMPARISON.md`](hardware_ref/thor/cpu/CORE_COMPARISON.md), where the four
cores give directly conflicting layout advice. An outline atomic is a `bl` to a stub that loads a
global feature byte and branches, **to decide something constant for the life of
the device**: `/proc/cpuinfo` reports `atomics`, which is `FEAT_LSE`.

**The chain is verified end to end**: ARMSX2's `compile_commands.json` shows
`-march=armv8-a+crc` in Release and `armv8.2-a+fp16+dotprod` in Debug, **no
`+lse` in either**, and the binary matches.

**Cemu wrote the whole analysis into `CMakeLists.txt:260` and chose the other
way**, on purpose: `ldxr`/`stxr` retry loops in `FSpinlock`, coreinit spinlocks
and the striped atomic HLE, which "livelocks harder under contention, which is
exactly the multi-core guest case" — then took `-moutline-atomics` because
`+lse` "SIGILLs on hardware without LSE", and named the exit: **"an explicit
opt-in build, not the default." This project is that build.**

**This is the DELETE operation, not a tuning change.** The dispatch exists to
serve variability this device does not have.

**Two guards.** `-mno-outline-atomics` **on its own is worse than the default** —
without `+lse` the compiler cannot emit an LSE atomic, so it removes the upgrade
path and leaves the loop unconditionally; `tools/target_check.py` probe 3 fails
on exactly that. And **nothing here is timed**: `DEVICE_QUEUE.md` entry 25
carries the price, predicting **FLAT on throughput** with contention as the only
arm likely to move.

**The Armv8.4 question was asked in the same pass and closed by counting** —
**then the count turned out to use the wrong instrument, and the answer survived
for a different reason.** LSE2 turns a 16-byte atomic store from an
11-instruction `caspl` loop into `stp`, and `casp` appears **zero** times in
25.9 million instructions across the six binaries. **But without LSE2 a 16-byte
atomic is not a `casp` — it is an `ldaxp`/`stlxp` loop, so counting `casp` counts
the fixed case and reports the broken case as absent.**

**With the right instrument**: azahar's 8 exclusives are **all in one function**,
`InputManager::NDKMotion::GetStatus()`, cold. **And rpcsx was not in the census
at all, because PS3 is out of the binary — and rpcsx is the fork where these are
HOT**: SPU mailboxes, where a pure `try_read` peek took the cache line
**exclusive**. **One fork in the packed binary uses them, in one cold function.
Do not raise the baseline.** Ledger queried first, zero prior entries. See
[`research_log/20260824_2230_five_of_six_forks_ship_a_runtime_atomic_dispatch.md`](research_log/20260824_2230_five_of_six_forks_ship_a_runtime_atomic_dispatch.md).

**eden ships a `YUZU_BUILD_PRESET=armv9` option that sets `-march=armv9-a`. Do
not select it on this device.**

**And no negative attribute rescues a wrong `-mcpu`.** `+nosve`, `+nosve2` and
both together all leave **`__ARM_FEATURE_SVE2` defined** on NDK 29 and NDK 30,
producing a macro state that describes no real machine. Plain `-mcpu=cortex-x3`
**emits `uqadd z0.b`** from an SVE2-guarded dispatcher. **rpcsx measured that
`-sve`/`-sve2` clears SVE and is right about the LLVM backend it calls through
`setMAttrs`; the clang frontend is the half this project compiles on.** See
[`research_log/20260824_2140_nosve_clears_the_backend_not_the_frontend.md`](research_log/20260824_2140_nosve_clears_the_backend_not_the_frontend.md).

> **A feature can be cleared from the backend and left in the frontend. Ask which
> one a claim measured.**

The answer is now written down once in
**[`hardware_ref/thor/THOR_TARGET.md`](hardware_ref/thor/THOR_TARGET.md)**:
`-march=armv8.2-a+crc+lse+fp16+dotprod+sha3+i8mm+bf16+rcpc -mtune=cortex-x3`.

**`+rcpc` added 2026-08-24, and it is the exception to the permission rule
above.** The device reports `lrcpc` and `ilrcpc`; **`-mtune` adds no features**,
so the previous line left RCPC off. **Measured on this box: an
`memory_order_acquire` load compiles to `ldar` without it and `ldapr` with it**,
unprompted, with no intrinsic. A `seq_cst` load correctly keeps `ldar`. **Whether
`LDAPR` beats `LDAR` on this SoC is unmeasured.** See
[`research_log/20260824_1130_rcpc_is_missing_from_our_target.md`](research_log/20260824_1130_rcpc_is_missing_from_our_target.md).

**Do not target `armv9-a`.** All four cores are ARMv9, ARMv9.0-A mandates SVE2,
and **this device has no SVE**. A compiler told `armv9-a` may emit instructions
that do not exist here.

**DEMONSTRATED 2026-08-24, and it is specific.** Four verified links: the device
reports no `sve` in `/proc/cpuinfo`; **`-march=armv9-a` defines
`__ARM_FEATURE_SVE`, `__ARM_FEATURE_SVE2` and `__ARM_FEATURE_SVE_VECTOR_OPERATORS`**
on this box's NDK clang, and `armv8.2-a` and `armv8.4-a` do not; **`xxhash.h`
selects its implementation purely at compile time and tests
`__ARM_FEATURE_SVE` BEFORE NEON**, with **no runtime check**; and **eden ships a
`YUZU_BUILD_PRESET=armv9` that sets `-march=armv9-a`.**

**SVE is selected instead of NEON, not in addition to it**, so the failure is
**`SIGILL` at the first hash, not a slower hash** — and **xxHash is the fleet's
texture-hashing workhorse.**

**Six of nine forks carry SVE-conditional code**, counted with
`git grep --recurse-submodules` and **with the vendored filter off, because a
dependency compiles into the product**: **rpcsx 303 lines in 48 files** (asmjit
seeds detected features from compile-time macros), **ARMSX2 43 in 5**, **azahar
27 in 3**, **Vita3K 27 in 3** (a submodule, plus a third xxHash copy via tracy),
**melonDS 13 in 1**, xenia and Cemu 1 each, eden and GameThor none. **Four of
them carry the same xxHash dispatch.**

**A first count of this said "ARMSX2 seven, melonDS one, everyone else zero" and
was wrong twice** — it excluded vendored trees, which is the wrong filter for a
question about what compiles into the binary, **and it did not recurse
submodules.**

**Nothing in the build would warn**, because the compiler is correct — it was told
the target is ARMv9. **The device is the thing that disagrees with the flag.**
**Not executed**; the chain is compiler test plus source reading. See
[`research_log/20260824_1210_armv9_would_select_sve_in_xxhash.md`](research_log/20260824_1210_armv9_would_select_sve_in_xxhash.md). That is why the baseline forks were not simply being
lazy, and it is why the target names its features explicitly.

**Caveat checked, and it rescues nobody.** Searching the ARM64 backends
themselves:

| Fork | `SDOT`/`UDOT` | `EOR3`/`BCAX`/`RAX1`/`XAR` |
| --- | --- | --- |
| **xenia** | **2 files** | **6 files** |
| Cemu | 1 | 1 |
| **ARMSX2** | **0** | **0** |
| **melonDS** | **0** | **0** |

**CORRECTED 2026-08-23: that table conflates three different things, and the
Cemu row is misleading.** Its two hits are **comments** in
`src/Common/cpu_features.h` — `// FEAT_DotProd - UDOT/SDOT` and
`// FEAT_SHA3 - also provides EOR3/BCAX/RAX1/XAR`. **Cemu detects the features
and never emits the instructions.** A row one below xenia implies it uses them
rarely; it uses them not at all.

**And eden was missing from the table**, with five matching files — **all guest
decode.** dynarmic's `simd_crypto_four_register.cpp` and `simd_sha512.cpp`
translate the **guest's** `EOR3` and `SHA512`.

**eden is the worst case for the guest/host trap in the fleet, because its guest
ISA is the host ISA.** Every ARM64 mnemonic appears in eden as guest decoding,
so counting mnemonics there tells you nothing.

| | Fork | Meaning |
| --- | --- | --- |
| **host emission** | **xenia only** | actually uses the device's vector features |
| feature detection | Cemu | knows the CPU has them |
| **guest decode** | eden | must recognise them as guest instructions |

**Search the emitter directory, not the tree.** `src/xenia/cpu/backend/a64/`
answers this in one pass. **The claim itself holds.**

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

**QUANTIFIED 2026-08-24.** Aim for about **0.67 arithmetic instructions per
load**. Whatcookie reached that ratio in RPCS3's comparison loop for **+38% on
the mid cores and +21% on the big core** — **RPCS3's numbers on RPCS3's loop, not
ours.** What transfers is the model: **the two axes worth optimising are
arithmetic-port pressure and dependency depth.**

**And instruction count is not one of them.** xenia measured this on its own
code: packing two u32s with `ORR` and storing through one `STP` cut a prolog
**from 18 instructions to 13 and measured slower**, because it serialised two
loads through an arithmetic operation into one gated store. **Fewer
instructions, deeper dependency chain, more pressure on the scarce port.**

> **Use inflation to choose which subsystem to attack. Do not use it to judge a
> peephole.** Both are true at different scales, and
> [`shared_layer/TRANSLATION.md`](shared_layer/TRANSLATION.md) carries the
> aggregate half.

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

### The Thor's core indices, and they decide every affinity question

| Host CPU | Core | Clock |
| --- | --- | --- |
| **0, 1, 2** | **Cortex-A510** | **2.0 GHz** |
| 3, 4, 5, 6 | Cortex-A715 / A710 | 2.8 GHz |
| **7** | **Cortex-X3** | **3.19 GHz** |

From xenia's `thor_topology.h`. **This file recorded the 1+4+3 layout and never
which index is which**, and both bugs below are what happens without it.

### Guest core N pinned to host core N — two forks, and it lands on the little cluster

**The x86 detour is not only in the instruction stream. A scheduling decision
that was correct for a homogeneous machine is the same class of mistake, and it
costs more than any peephole in this file.**

- **xenia.** `XThread::SetActiveCpu` does `set_affinity_mask(1 << cpu_index)`
  where `cpu_index` is the **guest** CPU. Its own ledger: *"FOUND A MAJOR
  x86-SHAPED STRUCTURAL BUG."* The map pinned guest CPUs 0-2 onto **the three
  little cores and never gave the X3 any guest work at all** — and on the Xbox
  360, **guest thread 0 is conventionally the main game thread.** *"This is a
  power bug as much as a speed one."* The workaround
  `thor_guest_thread_affinity_mask` **defaults to 0**, so this ships.
- **eden.** `CpuManager::RunThread` calls
  `PinCurrentThreadToPerformanceCore(core)` with the **guest** core index, and
  that function pins to **host CPUs 0-3**. Its comment says it is *"Aimed
  specifically for Snapdragon 8 Elite devices"* — **a part with no efficiency
  cores, where indices 0-3 really are performance cores.** On the 8 Gen 2 it
  pins three of four Switch guest cores to the **A510 cluster**, from a function
  whose name says the opposite. **It is inside `#ifdef __ANDROID__` with no
  device check.**

**Neither is catchable by a build or a test**: both produce a working emulator
that is slower and hotter. See
[`research_log/20260824_0600_guest_core_n_to_host_core_n.md`](research_log/20260824_0600_guest_core_n_to_host_core_n.md).

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

**MEASURED ON THIS DEVICE 2026-08-05 AND IT DID NOT REPRODUCE.** xenia's
`thor_probe_a510_vector_units` pins an independent-chain NEON loop per little
core and runs pairs concurrently. Solo: **390.3 / 407.1 / 462.6 Miter/s**. Pairs:
**0+1 at 98.5% of the solo sum, 0+2 at 95.8%, 1+2 at 98.8%.** **All pairs scale
near-linearly and no pair is halved.**

**Its stated limitation keeps this open rather than closed:** the probe uses
**integer** NEON, so **if the shared resource is specifically the FP SIMD pipe an
integer test would not expose it.** Rerun with `vmlaq_f32` before calling it
closed. **And Qualcomm can configure A510 complexes with per-core VPUs**, so this
part may differ from the one measured in the talk that prompted the claim.

> **Fourteenth manual-derived prediction measured here and refuted.** Checking
> `/proc/cpuinfo` is not enough: **a vendor configures the part, so a manual
> describes what a core may implement, not what this device does.**

**AND rpcsx STILL BELIEVES IT, FROM THE SAME MANUAL. The two are compatible, and
resolving them adds a fact neither had alone.**

**They measured different quantities.** xenia measured **scaling between two
cores** and found it near-linear. rpcsx quotes ARM's §4.11 for **sharing**, and
adds a separate row from the A510 tables that xenia's probe could not see:

| Operation | Latency | Throughput (D, Q) |
| --- | --- | --- |
| `CMEQ`, `AND`/`EOR`/`ORR` | **3** (against 2 on the big cores) | **2, 1** |
| `MLA`, `MLS`, `SDOT`/`UDOT` | 4 | **2, 1** |

> **`2,1` means 128-bit Q-form NEON is throughput 1 on the A510** — half rate per
> core, before any sharing.

**Both can hold at once**: if each core is already issue-limited at throughput 1,
a VPU wide enough to serve two of them shows near-linear scaling **and** low
absolute throughput. **xenia measured the scaling; nobody has measured the
absolute rate against a big core.**

**They agree operationally, which is what matters.** rpcsx pins **no vector-heavy
guest thread to the A510s** — its map is `CPU0-3 General, CPU4 PPU, CPU5-6 SPU,
CPU7 RSX` — and warns:

> **"move the SPU threads onto the efficiency cores to run cooler" is an obvious
> next idea, and on this core it is a trap** — two threads on one complex would
> share a VPU *and* run 128-bit NEON at throughput 1, on a cluster clocked at
> **2016 MHz against the mid cluster's 2707 MHz.** **Cooler per core, far slower
> per thread, and quite possibly worse energy per frame.**

**If that experiment is run: pin to different complexes, and measure energy per
frame.**

### The A710 stalls three cycles on lane-assembled vector registers

> a V-pipeline uOP containing more than 1 quad-word register source, a portion
> or all of which was previously written as one or multiple single words, will
> stall in dispatch for three cycles.

**This is exactly what an emulated guest vector unit does.** Writing lanes
individually and then using the whole register is the normal case for PS2 VU,
Xbox 360 VMX128 and DS geometry. Three cycles on first use, on two of the
Thor's cores.

**CONFIRMED AND FIXED 2026-08-24 — and it is the FIRST manual-derived lead in
this fleet not to be refuted.** Fourteen others were measured and failed. **This
one survived because every step was checked against emitted code.**

**§4.11 gives the three conditions that must all hold**, which §4.2 alone does
not: the producer writes an **S-register, not a `D[x]` scalar**; the consumer
reads an **overlapping Q-register**; and the consumer is an **FP/ASIMD µOP**, not
a store or a `MOV`.

**`_mm_set_epi32` produces it verbatim**, at `-O2 -mcpu=cortex-a710`:

```
fmov  s1, w0                 <- S-register write        (4.11)
mov   v1.s[1], w1            <- single-word lane writes (4.2)
add   v0.4s, v1.4s, v0.4s    <- two quad-word sources, FP/ASIMD consumer
```

**The obvious workaround fails.** Writing to a stack array and loading back
**compiles to byte-identical code** — clang folds the round-trip and rebuilds it
lane by lane. **A reasoned fix would have been believed and done nothing.**

**The guide's own mitigation works**, because §4.11 exempts `D[x]` writes: pack
into 64-bit halves first and get `fmov d1, x8` plus `mov v1.d[1], x9` — **two
writes instead of four, no hazard.**

**Applicability, counted before building anything.** Sites using
`_mm_set_epi32`, `_mm_setr_epi32` or `vsetq_lane_*`, submodules included:
**rpcsx 18, ARMSX2 18, eden 5, xenia 2, Vita3K 1, Cemu 0, azahar 0, melonDS 0.**
**ARMSX2 has as many as the fork that found it.** **18 sites is small and whether
any is hot is unknown** — count, then measure, then build.

> **A manual gives a hypothesis. A disassembly gives a finding.** The fourteen
> that failed were mostly the former.

See [`research_log/20260824_1540_the_one_manual_lead_that_survived.md`](research_log/20260824_1540_the_one_manual_lead_that_survived.md).

**All these leads belong in the experiment ledger before anyone acts on them.**

### AND A MEASURED PRIOR AGAINST THEM, FOUND 2026-08-23

**rpcsx's `thor-game-workup` says, verbatim:**

> **Do not pick a lever from a manual. Ten manual-derived predictions were
> measured here and ten were refuted. Profile first.**

**Ten for ten.** And **every lead in this section is manual-derived** — the
Cortex-X3 FP-status stall, the A710 lane-assembly stall, the A715 branch
density, the A510 shared VPU, "prefer a load over arithmetic". **None has been
profiled here.**

**The fleet's own record agrees.** xenia implemented all three CPU leads and left
them **off by default**; `EOR3`/`BCAX` fusion measured **`DEAD`**; the
`TBL2`-for-`TBX2` rewrite measured **null**. **Thirteen for thirteen.**

**This does not make the leads false. It makes them hypotheses with a bad
prior.** Treat every one as `OPEN` at best, and **profile before choosing which
to chase.**

**And prove the workload before believing a null.** Two cheap checks:
**is it capped?** — a flat rate in every sample means frames cannot move — and
**is it loaded?** — half load hides a regression.

> **A negative result needs a workload that could have produced a positive one.**

**Prefer a workload with fixed, finite work.** Best is a **precompile with the
cache cleared**, which is self-timed and has a natural unit. **A free-running
scene has no unit** — which is exactly why xenia's LLVM run came back
`CONFOUNDED` and why its fix is a fixed frame range.

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
- **AND IT IS WORTH 14x ON A GMEM-HEAVY PATH, MEASURED.** Same title, same HLE
  configuration, same session: **the stock Qualcomm driver rendered correctly and
  stably at ~0.8 fps; Turnip ran the identical configuration at 11 fps.** xenia's
  conclusion: *"the Qualcomm proprietary driver handles the native pass/GMEM ~14x
  worse."*

  **Scope it before quoting it.** One title, one backend, one native-HLE render
  path that leans hard on GMEM behaviour. **It is not a general claim that Turnip
  is 14x faster.** What it establishes is that **the gap can exceed an order of
  magnitude on exactly the path this project's render work targets** — which
  turns the driver pin from hygiene into a load-bearing decision. See
  [`research_log/20260824_0645_turnip_is_14x_and_three_config_traps.md`](research_log/20260824_0645_turnip_is_14x_and_three_config_traps.md).

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

  **AND ARMSX2 HAS THE OTHER HALF, found 2026-08-25.**
  `pcsx2/GS/Renderers/Common/GSGPUProfile.h` is a **mobile GPU driver profile
  database**: **29 named `DriverBug` values and 15 named `DriverWorkaround`
  values**, matched on driver and version with a **confidence rank** so a broad
  rule never overrides a precise one, and an explicit **`conservative_fallback`**
  flag for "nothing matched, safe defaults are in force".

  **The two compose rather than compete.** rpcsx's advisor answers *"is this
  driver package suitable for this GPU"* **before install**; ARMSX2's profile
  answers *"what is broken in the driver that is running"* **after**. **Neither
  fork cites the other.**

  **Four decisions to take wholesale:**

  - **Bugs and workarounds are separate bitfields**, because one mitigation
    answers several defects **and because a workaround can be forced on for
    testing without claiming the device has the bug.** That second reason is what
    this project's measurement discipline needs.
  - **Name the DEFECT, not the fix.**
  - **Rank the match confidence:** `Vendor < Model < Driver < DriverVersion`.
  - **State the fallback**, rather than letting unknown hardware be an accident.

  > **Gate on driverID, not vendorID.** ARMSX2's header records why, learned
  > expensively: the same Mali part behaves differently under Arm's driver than
  > under Mesa PanVK, and both the r44p1 `DEVICE_LOST` fix and the 8 Elite
  > push-descriptor disable had to be gated on the driver.

  **And there is a live instance of that distinction for this device:**
  `m_broken_colormask_with_depth = IsDeviceAdreno() && !is_turnip`. **The Adreno
  proprietary driver has a broken colour mask with depth test and Turnip does
  not.** So **pinning Turnip removes at least one correctness workaround, not
  only a performance gap.**

  **Consequence for the per-game driver override, which was not stated:**
  switching driver **switches the whole bug and workaround set with it**, not
  just the binary. And **`conservative_fallback` is what the "off baseline"
  warning should be built on** — a profile that did not match, rather than a
  string comparison against the pinned build name.

  **Seven of eight forks carry Turnip-specific code** — ARMSX2 21 files,
  xenia 17, eden 11, rpcsx 8, Vita3K 5, Cemu 4, azahar 3, melonDS 0. See
  [`research_log/20260825_1620_armsx2_has_a_mobile_driver_bug_database.md`](research_log/20260825_1620_armsx2_has_a_mobile_driver_bug_database.md).
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

  **QUALIFIED 2026-08-25, and the qualification is on the mechanism itself.**
  ARMSX2's own driver-profile table carries
  **`vk-turnip-attachment-self-read`**, marking Turnip on Adreno with **both**
  `BrokenSubpassFeedback` **and** `BrokenAttachmentFeedbackLoopLayout`, with the
  workaround `UseRenderTargetCopyForFeedback` — **a full render-target copy per
  feedback draw**, which also **turns texture barriers off and disables
  framebuffer fetch**, because they are the same in-tile read.

  **Its provenance is a real device A/B, on different hardware.** ARMSX2 #442:
  an HD texture pack made *Tales of the Abyss* lose its entire 2D text layer, and
  the A/B on **Turnip / Mesa 26.1.2 with an Adreno 650** showed both in-pass
  forms dropping the content while a separate RT copy rendered correctly. *"The
  reporter sees the same failure on the proprietary blob"*, so it is an Adreno
  property rather than a Mesa regression.

  > **The Thor is an Adreno 740 and the pin is newer Mesa. There is no evidence
  > either way for this device**, and the rule carries no version bound because
  > it was written from one A/B — not because it was re-tested.

  **And the blast radius is wider than the PCSX2 port**, because there are
  **three** ways to read an attachment in-pass and this fleet uses all three.
  The correction table above records that xenia, ARMSX2 and rpcsx use **input
  attachments**. Searched 2026-08-25 for the other two, vendored trees and Vulkan
  headers removed:

  | Fork | fbfetch / `subpassLoad` | feedback loop |
  | --- | --- | --- |
  | **ARMSX2** | **23 files** | **16 files** |
  | Cemu | 8 | 0 |
  | Vita3K | 8 | 0 |
  | azahar | 6 | 2 |
  | xenia | 4 | 0 |
  | eden | 0 | **6** |
  | melonDS | 0 | 0 |

  **The Turnip rule marks subpass feedback AND the feedback-loop layout broken,
  and its workaround also disables framebuffer fetch**, because it is the same
  in-tile read. **So it reaches all three mechanisms.** These are file counts and
  a file count is not a use — each fork gates its own path and none was read —
  **but the mechanism is not hypothetical in this fleet.**

  **So the premise of the transfer is exactly what is unproven here**: the
  technique needs in-pass attachment self-read, and that is the thing recorded
  broken on the neighbouring part. **`DEVICE_QUEUE.md` entry 26 is a
  game-free Vulkan probe that settles it**, and the device-free half is already
  done — all 30 rules listed, no superseding rule. See
  [`research_log/20260825_1710_turnip_breaks_attachment_self_read.md`](research_log/20260825_1710_turnip_breaks_attachment_self_read.md).
- **rpcs3 landed ARM64 Cell optimisations using `SDOT` and `UDOT`**, the
  ARMv8.2 dot-product instructions, plus an SPU recompiler change worth roughly
  5 to 7% on SPU-heavy titles.

  **PS3 is deferred and this still matters.** `SDOT` and `UDOT` exist on the
  Cortex-X3, A715 and A710. Lowering a **guest vector unit** onto ARM64
  dot-product instructions is not an SPU technique; the fleet has three more
  vector units in ARMSX2's VU, xenia's VMX128 and melonDS's DS geometry.

  **rpcsx is GPL-2.0-only so its code cannot be taken. A technique is not
  code.** Ideas cross a licence boundary that code cannot.

**CORRECTION, and the fleet got there first.** **Three** forks have already done
this cross-pollination and none was recorded here: xenia
`docs/research/20260805-rpcs3-arm64-optimizations-applicable.md`, Cemu
`docs/research/20260820-rpcs3-arm64-optimizations-for-cemu.md`, 395 lines,
citing **twelve merged rpcs3 PRs by number**, and **Vita3K
`docs/research/20260820-rpcs3-arm64-optimizations-for-vita3k.md`**, found
2026-08-23 during the spin-wait audit. **Cemu's cites xenia's.** The fleet is
already cross-pollinating and already citing itself.

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
what quality. Read it before you build anything.

**And read the fork's own research directory. It is the richest unmined seam in
the fleet.**

> **START AT THE FORK'S `AGENTS.md`, NOT AT WHICHEVER DOCUMENT YOU FOUND FIRST.**
>
> **Cost of not doing so, 2026-08-25: a day.** rpcsx's `docs/arm64/ledger.md` was
> treated as its routing table. **It is the audit ledger.** Its entry on a
> thermal A/B was written up here as "a correct measurement supporting the wrong
> decision" — and the same fork's `docs/arm64/thermal.md` says **the measurement
> was of the wrong quantity entirely**, because the guard compared a
> package-shaped 72 C limit against a per-core **junction** maximum, which is a
> load detector rather than a thermal bound.
>
> **`AGENTS.md` indexes that document twice**, once with the line that would have
> saved the day: *"Junction versus package sensors, and the guard that compared a
> limit against the wrong one."*
>
> **This repo's federation rule already said this** — *those files are the source
> of truth for their fork* — and **a ledger is not an index.** Surveyed 2026-08-23:

| Fork | `docs/research` | skills | tools |
| --- | --- | --- | --- |
| **xenia** | **553** | **37** | **142** |
| **rpcsx** | 23 | **51** | 40 |
| Vita3K | 2 | 18 | 4 |

**Roughly 590 documents and 106 skills. This repo had read about a dozen.**

**553 is not a corpus anyone reads exhaustively, but the titles are a searchable
index of every question already asked — and the negatives are the valuable
half.** Three documents read from that seam confirmed the register-residency
thesis on hardware, showed the experiment had already been run and returned
**zero hits out of 546 context loads**, and supplied a ranked plan for what to
try next. The first question for any
new feature is "which fork already has this?", not "how do I write this?".

The survey on 2026-08-22 proved the point. ARMSX2 and melonDS-android both
built per-class texture routing, separately, with different names for the same
idea. Neither fork knows the other exists.

**The "overlap by nine entries" figure recorded here earlier is withdrawn.**
Reading both sets showed the lists are not comparable entry by entry: melonDS's
carries `lite` and `strong` suffixes that are cost variants rather than distinct
algorithms. **Re-derive it or leave it out; do not repeat it.**

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

**MEASURED 2026-08-23, and this is the largest single asset in the fleet.**

| Fork | Per-game files shipped | Format |
| --- | --- | --- |
| **rpcsx** | **2,676** | rpcs3 YAML and `.ncl` |
| **ARMSX2** | **591** | `pnach` |
| eden | 153 | content patches and mods |
| melonDS | 62 | AR codes |
| **GameThor** | 43 | **typed host-config fixes, as code** |
| azahar | 39 | cheats |
| Vita3K | 20 | cheats |
| **Cemu** | 15 | graphic packs — **two are this project's own** |
| xenia | 1 | `.patch.toml` |

**About 3,600 files, community-maintained, in nine incompatible formats behind
nine engines.**

**And going into the game code is the answer to the API-boundary problem.** You
do not reconstruct the API that PM4 erased — **you replace the guest code that
would have called it.** xenia already does this: `SetupExtern` installs HLE
intercepts over guest functions, and **twelve HLE cvars exist for one title.**

**The lane produces wins, which is worth stating because the CPU leads do not.**
xenia's ledger: **39 `WIN`, 33 `DEAD`, 8 `FLAT`, 5 `CONFOUNDED`**, with **77
entries touching HLE alone.** **Per-game analysis beats manual-derived
micro-optimisation by a wide margin in this fleet's own record**, where the
manual-derived CPU levers are **thirteen for thirteen refuted.**

**So what is missing is not the idea and not the mechanism. It is two things:**
**one engine across eight systems** — take Cemu's symbolic assembler and xenia's
TOML format, neither of which needs writing — and **analysis cheap enough to do
per game**, which is exactly what [Foundation](#foundation) point 3 says agents
are for. The lanes already exist: `thor-ghidra-static-lane`,
`vita3k-ghidra-escalation`, and xenia's Ghidra-to-TOML emitter.

**The caution is the same as everywhere else.** xenia's verdict: *if unrelated
guest CPU work already exceeds 33.3 ms, a perfect renderer delivers nothing.*
**Profile, then analyse the game, then patch.** The 33 `DEAD` entries are mostly
what happens when that order is reversed.

See [`research_log/20260823_2015_per_game_specialisation.md`](research_log/20260823_2015_per_game_specialisation.md).

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
| **Host config fix** | **change the host's setup for one game, no guest bytes touched** | **GameThor `gamefixes/`** |

**The fourth was found on 2026-08-23 and it unifies something.** A host config
fix changes an environment variable, a registry key, a launch argument or an INI
value — never guest memory. **A per-game driver override is exactly this kind**,
and so is a per-game thread policy or present mode. This repo was treating those
as settings and treating fixes as patches; **they are the same thing**, and
naming it removes a category from the design.

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
- A patch states its **intent**: **speed, fix, change, or cheat.** The UI groups
  them by intent, because a person choosing a cheat and a person chasing frames
  want different lists.

  **THE INTENT FIELD IS LOAD-BEARING THREE TIMES OVER, found 2026-08-25.** It
  was justified here as a UI grouping. ARMSX2's shipped bugs show it decides:

  1. **whether integrity mode blocks the patch** — hardcore blocks cheats, not
     fixes;
  2. **whether it binds to `GameKey` or `DumpId`** — a cheat matches across all
     CRCs of a serial, a fix stays CRC-specific;
  3. **whether it may auto-apply at boot** — only a dump-bound patch may.

  **`CHEAT` had to be added to the enum for any of that to be expressible.**
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
| `supervise` | Catch the repeated unproductive cycles this repo runs. Runs `tools/supervise.py`. |

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

### The supervision layer, taken from AVO

**Added 2026-08-23.** NVIDIA's AVO architecture has four parts and **this
project already had three** — the agent loop, persistent memory, and domain
tools. **The fourth is a supervision layer** that "monitors the broader
trajectory for stagnation or repeated unproductive cycles and can redirect the
main agent toward alternative strategies".

**That is the one part worth taking**, because this repo's failure modes are
already recorded as repeated cycles rather than one-off mistakes: the
["Read before you claim"](#read-before-you-claim) table, the nine-fork SDK
migration that was reverted in full, and the experiment ledger, which exists
because dead levers were re-run.

**`tools/supervise.py` is the executable half**, and it reads **added lines
only** — scanning whole documents reports the correction tables as the disease.
Six checks: unqualified negatives, work leaking into a fork, a queued experiment
with no prediction, a session with no log, a fleet-wide claim with no named
instrument, and an experiment proposed without querying the ledger.

**It caught a real error on its first run**: a research log asserted "No other
fork does this" about three forks on the strength of reading one. The second
search confirmed the claim, but it was unverified when written.

**It also produced a false positive on its first run**, failing a
`DEVICE_QUEUE.md` entry that states its predictions in a table column. **The
document was right and the tool was wrong** — the same lesson as the ABI lint
and Cemu. **When a tool and a document disagree, read the actual line.**

**Do not quote AVO's benchmark numbers here.** Nothing in this repo has been
measured against them.

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
   losing one, and **melonDS has `TouchVibrator`**.

**CORRECTED 2026-08-23: "haptics nobody else ships" is inverted.** Searched
twice — once across all sources, then for host-only markers in **Kotlin and Java
alone**, to keep guest rumble emulation out. **Seven of eight forks ship
host-side haptics. xenia is the only one without.**

**And there are two different features wearing one word.** melonDS's
`TouchVibrator` is **overlay touch feedback** — user-settable strength, a fixed
100 ms buzz, behind a `VibratorDelegate` with a **duration fallback when the
device cannot vary amplitude**. eden's `YuzuVibrator` is **guest rumble routed to
a physical device** — `getControllerVibrator(device)` against
`getSystemVibrator()`, so rumble reaches the pad that caused it.

**Neither substitutes for the other, so the contract needs two entries.** Take
melonDS's delegate and amplitude fallback for touch feedback, and eden's
per-device routing for rumble.

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
| `minSdk` | 33 | The device reports API 33. **Measured 2026-08-23: the fleet spans 24 to 30 and no fork meets it.** eden's 24 carries compatibility paths for Android 7. |
| `targetSdk` | 37 | Android 17, released 2026-06-16. **Only ARMSX2 is on it.** The rest span 28 to 36. |
| `compileSdk` | 37 | Matches `targetSdk`. |
| Gradle | 9.6.1 or newer | **Measured 2026-08-23: seven distinct versions across eight forks, spanning 7.3.3 to 9.5.0.** Nothing in the fleet is on 9.6.1. |
| C++ standard | C++20 | Verify each fork builds. melonDS declares C++17. |
| Audio | **Oboe `1.10.0`** | Three forks already use it. Replaces five vendored copies of cubeb. **Version read 2026-08-23 from eden's CPM cache — a real working Android arm64 pin, not a guess.** |
| Symbol visibility | **`-fvisibility=hidden`** | **MEASURED FROM THE BINARIES 2026-08-23.** Exported symbols span **43x**: xenia **2,285**, ARMSX2 4,380, melonDS 12,255, azahar 62,507, Cemu 68,740, **Vita3K 98,550**. Colliding symbols per backend run **2,092 to 19,696**. The earlier header-scan percentages ranked the forks **wrongly** and are withdrawn. |

Notes:

- **NDK r30 is beta.** r30 becomes the LTS release. Move to it when it is
  stable, not before. Seven emulator cores on a beta toolchain is a bad trade.
  `30.0.15729638` is installed on this box. Do not use it for a shipping build.
- Drop `armeabi-v7a` and `x86_64` from the shipping build. The Thor is arm64.

  **MEASURED 2026-08-23, and this is the largest unpriced lever on build time.**
  melonDS's 15 min 27 s clean build compiled its native code **three times** —
  `arm64-v8a`, `armeabi-v7a`, `x86_64` — and shipped **four** ABI folders in the
  APK. Native compilation is per-ABI, so most of that C++ work produced code the
  device cannot execute, and it is carried in the 55.5 MB APK afterwards.

  | Fork | ABIs built | Usable |
  | --- | --- | --- |
  | **ARMSX2**, **eden**, **azahar**, **rpcsx**, **Cemu** | `arm64-v8a` | **1 of 1** |
  | Vita3K, xenia | `arm64-v8a`, `x86_64` | 1 of 2 |
  | GameThor | `arm64-v8a`, `armeabi-v7a` | 1 of 2 |
  | **melonDS** | three | **1 of 3** |

  **Five correct, four not.** Cemu was recorded as a failure and is not: its
  build file has a **disabled** `// abiFilters("arm64-v8a", "x86_64")` directly
  above the live arm64-only line, and the lint took the first match without
  skipping comments. **Its own `AGENTS.md` said so correctly and was
  disbelieved because a tool disagreed.**

  **The rule is not "trust the docs".** It is **when a tool and a document
  disagree, read the actual line** — today the document was right, and twice
  earlier today it was wrong.

  **Four forks already fixed this, and rpcsx measured it.** Its
  `build.gradle.kts` records that adding x86_64 put **26 MiB compressed and
  65 MiB uncompressed of unreachable code into a 96 MiB APK, more than half the
  payload, and doubled the native compile** — and it keeps the old behaviour
  behind `-PrpcsxAndroidAbis` rather than deleting it. **Copy that shape: the
  default serves the device, the override serves whoever needs it.**

  **Record the ABI list beside every build time**, or Phase 0.3's numbers are
  not comparable across forks.

  See [`research_log/20260823_0030_abi_waste.md`](research_log/20260823_0030_abi_waste.md).
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

#### MEASURED 2026-08-23: every cross-fork collision is a dependency

**Read from six forks' built `arm64-v8a` libraries with `llvm-nm`**, not from
build files. **25,526 symbols are exported by two or more forks. Zero of them
are emulator code** — searched across `Kernel`, `Core`, `Common`, `PPC`,
`Latte`, `xenia`, `VU`, `melon`, `Vita3K`, `Cemu`, `Service`, `Pcsx2` and `GS`.

**This supports the packed binary and reorders the work.** Seven emulator cores
in one binary is not the problem. **Seven sets of vendored dependencies is**, and
that is now measured rather than argued.

**Three dependencies nobody had recorded:**

- **OpenSSL**, in azahar, Cemu and Vita3K. **The single largest collider,
  ~6,400 symbols**, and it is absent from the table below. Three TLS stacks is a
  security question as well as a linking one.
- **Teakra**, in azahar and melonDS. **The only genuine shared emulation
  dependency found** — both emulate a Teak DSP, the 3DS's and the DSi's.
- **rcheevos**, in ARMSX2 and melonDS. RetroAchievements.

**And one hard blocker, not a merge conflict.** **60 colliding symbols are SDL's
JNI bridge** — `JNI_OnLoad` plus the `Java_org_libsdl_app_*` families, in ARMSX2
and Vita3K. **`JNI_OnLoad` cannot exist twice in one binary**, and it cannot be
renamed, because the Android runtime looks it up by that exact name.

**Limit: this is a lower bound.** Exported dynamic symbols only, so the forks
with the smallest export sets under-report their own dependencies. **A gap in
this list is not evidence a fork lacks a library.** eden is unmeasured because it
does not build.

See [`research_log/20260823_1508_symbol_collision_census.md`](research_log/20260823_1508_symbol_collision_census.md).


| Library | Forks | Vendored by |
| --- | --- | --- |
| **`ffmpeg`** | **5** | Vita3K, eden, xenia, ARMSX2, rpcsx |
| **`cubeb`**, audio | **5** | azahar, Vita3K, Cemu, ARMSX2, rpcsx |
| `vulkan-headers` | 4 | azahar, Cemu, xenia, rpcsx |
| `imgui` | 4 | Vita3K, Cemu, xenia, ARMSX2 |
| `glslang` | 4 | azahar, Vita3K, xenia, rpcsx |
| `xbyak`, `stb`, `glad`, `fmt`, `discord-rpc` | 4 each | |
| `libadrenotools` | 3 | azahar, Vita3K, Cemu |
| `dynarmic`, ARM JIT | **3** | azahar, Vita3K, **eden** |

**FFmpeg is vendored five times.** It is enormous, and five copies in one
binary is not a size problem but an impossibility.

**The cubeb half is now answered: standardise on Oboe.** Surveyed 2026-08-23.
**Three forks already chose it independently** — ARMSX2
(`Host/OboeAudioStream.cpp`), eden (`audio_core/sink/oboe_sink.cpp`) and melonDS
(`MelonDSAudio.cpp`, plus `MicInputOboeCallback.cpp` for the DS microphone).

Oboe selects AAudio on Android 8.1 and later and handles the device-specific
latency tuning a hand-written driver has to redo. cubeb reaches Android through
its own backends, which is a layer that exists to serve desktop portability —
the cost [Foundation](#foundation) point 1 refuses.

**Three independent choices agreeing is stronger evidence than any one of
them**, the same reasoning as the touch overlay API surviving two divergences.

So the vendored-cubeb problem is **four conversions, not a design decision**:
xenia off its own Android driver, Vita3K off SDL audio, azahar and Cemu off
cubeb. rpcsx is out of the binary anyway.

**Nothing here is measured.** See
[`research_log/20260823_0005_audio_backends.md`](research_log/20260823_0005_audio_backends.md).

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

**SHARPENED 2026-08-24, and one word above is wrong.** A USB-attached reading is
**a FLOOR, not fiction** — the charger supplies an unknown share, so the battery
figure understates rather than randomises. rpcsx's probe prints **`on battery,
exact`** against **`FLOOR, USB attached`**, which is the right way to report it.

**And two claims had been merged into one blocker:**

- **Attributing watts to the CPU specifically is genuinely unavailable.** There is
  **no per-rail sensor** separating CPU from GPU, display, memory and radio, so a
  spin reduction cannot be converted to a wattage without assuming a model.
- **Measuring TOTAL SYSTEM power is available and exact.** `charge_counter` is
  cumulative in microamp-hours; **differenced across a window it gives mean
  current**, and the probe measured **0.002 W spread across four idle runs.**

> **So the blocker is a cable, not a missing sensor.** Unplug the device, drive it
> over the Wi-Fi adb endpoint this file already prefers, and system power becomes
> exact. **And system draw is the user's actual question** — "did this use less
> power" — not CPU-attributed draw, which is the one that is not available.

**Its meta-lesson, and this repo should adopt it:** that was **the third
self-declared blocker in that effort found to be partly self-inflicted** — one
blocked on integration rather than analysis, one on a proposal that was wrong
rather than on risk, and this one on a cable. **Re-read your own blockers before
treating them as facts.**

**Two power reference points to keep:** **device idle with the emulator not
running is 0.48 cores busy at 0.628 W**, and the useful metric set is
`residency_mcycles` (**cycles the core was clocked for, not work**), `busy_ratio`
(**fraction of wall time outside cpuidle — the cleanest WFE signal**),
`work_mcycles` = residency x busy_ratio, and `mean_mhz`, **because power rises
faster than linearly with clock.**

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
- **ENERGY PER FRAME, NOT WATTS.** Watts alone rewards a slower core: move work
  to an A510 and the power drops while the frame takes longer, **and total energy
  can rise.** rpcsx's instruction for any little-core experiment is exactly this
  — *"measure energy per frame rather than watts."* **The two disagree precisely
  where a handheld cares most.**
- **Watts, not only frames.** The stated target for xenia on this device is
  about 5 W and 50 C. Throughput alone answers the wrong question on a
  handheld. A change that holds fps and lowers temperature is a win.
- **SAY WHICH THERMAL SENSOR, AND NEVER TAKE A MAX OVER ALL OF THEM.**
  `/sys/class/thermal` on this device exposes **two different quantities**, and
  comparing a limit against the wrong one **manufactured a false alarm twice**.

  | Sensor | Reading | What it is |
  | --- | --- | --- |
  | `cpu-1-9` | **90.7 C** under load, **55.0 C** idle | **per-core junction (Tj)** |
  | `cpu-1-10` | 83.9 C | junction |
  | `cpuss-0` | 68.7 C under load, 49.4 C idle | CPU subsystem |
  | `gpuss-*` | 43-46 C | GPU subsystem |
  | AYN FanBase / on-device readout | **57-61 C** | **package** |

  **A ~35 C swing with load is the junction signature.** **Roughly 90 C Tj under
  load is ordinary for this SoC**, which throttles nearer **95-105 C**.

  **This device exposes NO `skin` zone at all** — nothing matches
  `skin|case|shell|quiet` — so there is no package sensor in `/sys/class/thermal`
  to fall back on.

  > **A limit and a measurement taken from different sensors cannot be compared**,
  > and a `max` over a heterogeneous sensor set silently does exactly that.
  > **Never `sort -rn | head -1` over every zone.**

  **The failure mode is worse than a bad number: it systematically favours the
  slower arm.** rpcsx's guard compared a **junction maximum** against a
  package-shaped 72 C limit, so **the arm using the big cores tripped almost
  immediately and the arm pinned to the A510s did not** — because little cores run
  cooler at the junction. *"The measurement was faithfully recording which arm ran
  on faster cores."* **A510 pinning was then adopted to satisfy a limit that was
  measuring the wrong quantity.**

  **An unthrottled compile was once reported as "81.5 C, above the 72 C gate" and
  nearly reversed a good change. The device was at 57 C.**

  **With the right sensor the same comparison reads:** throttled to three A510s,
  ~10 minutes at 51-58 C package; **unthrottled on all cores, ~3 minutes at
  57-61 C.** **Three times faster for about three degrees.** See
  [`research_log/20260824_1050_read_the_right_thermal_sensor.md`](research_log/20260824_1050_read_the_right_thermal_sensor.md).
- **State the expected signature before the run.** Name what the numbers
  should do if the change works. A run with no prediction cannot fail.
- **Run for 15 minutes or more when heat matters.** Thermal behaviour only
  settles over a long run. A short run measures a cold device, which is not
  how anybody plays. This follows Google's ADPF guidance.
- **Measure without ADPF first.** Establish the baseline before adding hint
  logic, or the hint is tuned against an unknown.
- **NORMALISE BY SOMETHING THE CHANGE DOES NOT TOUCH, AND THE SCENE MAY VARY.**
  **This is the cheapest route to a comparable measurement and it needs no
  determinism at all.** rpcsx failed two experiments trying to stabilise the
  workload — matched settle times, matched windows, savestates for a fixed scene —
  then **fixed the metric instead.** Its reservation wait has two profiled sites
  and **only one sits behind the flag under test**, so the other counts contention
  independently of the change. Dividing by it: **the absolute rate varied 59%
  between two windows of the same build; the normalised ratio varied 1.1%.**
  > **Look for a quantity the change under test provably does not touch, and
  > normalise by it.** Its own note: *"reading which branch the flag sat in was
  > worth more than any amount of measurement discipline."*

  **It does not cover whole-frame questions** — is this title faster, did the tail
  improve — because those have no natural denominator. **Those still need a
  deterministic scene**, which Cemu and eden cannot supply and xenia's deadlock
  blocks.
- **Cross-run comparison is untrustworthy.** Scene complexity swings several
  times a second, so two separate runs are not comparable. Use an in-place
  alternating A/B inside one run, on a busy frame. **Prefer A/B/A** where a live
  toggle exists.
- **THE NOISE FLOORS, MEASURED.** rpcsx measured the spread of **one**
  configuration re-run:

  | workload | spread |
  | --- | --- |
  | gated title screen | **+/-0.2%** |
  | restored savestate | **+/-5%** |
  | pressing through cutscenes | **~+/-50%, unusable** |

  > **A claim smaller than the floor of its workload is not a result.**

  **Check every prediction in [`DEVICE_QUEUE.md`](DEVICE_QUEUE.md) against
  these.** A 3% win on a savestate run is noise.
- **Never quote n=1.** One arm read **10351 mW against 7545** and was reported as
  a win.
- **Report `[min..max]`, not the mean. Overlapping ranges mean "not
  distinguishable".**
- **Each arm needs a fresh process.** Device properties are read once into a
  static and cached for the process lifetime — **the same trap as the per-game
  driver override needing a restart.**
- **Run a harness from a frozen copy.** Editing a script while bash executes it
  killed a run with `unexpected EOF` **at a line with no syntax error**.
- **Measure the thread, not the process.** One fork's `rsx::thread` is **0.51 of
  2.90 cores**, so a lever saving 30% of that thread moves the process total by
  5% and hides in the noise.
- **`unknown[+X]` in a profile is the JIT arena, not a symbol.** All recompiled
  guest code collapses onto one entry. **Never quote a percentage against it as
  if it were a function.**
- **Check whether a `grep` was truncated before concluding something is absent.**
  A `head -12` hid an entire upstream subsystem. **This is this repo's most
  repeated failure, and another fork wrote the rule first.**
- **FOR "WHERE DOES THE TIME GO", USE A SAMPLING PROFILER, NOT INSTRUMENTED
  COUNTERS.** rpcsx's counter-based wait profiler reported *"93% of all emulator
  spin is the SPU `GETLLAR` wait"* — **because every site it had been told to
  instrument was SPU-side.** Eight PPU sites were never instrumented, **so they
  could not appear, and their absence read as evidence they were not there.**
  A sampling profiler then found **74% of cycles** in two of them.
  > **A search that finds nothing and a search that searches nothing look
  > identical.**
- **NAME THE CONFOUND THAT WOULD FAKE A WIN, before the run.** rpcsx's spin
  prediction does this: parking instead of spinning **may let the scheduler
  migrate the thread off its big core onto an A510**, so cores-busy and power
  both drop **while the emulator gets slower**. Its instruction: **check
  per-cluster residency.** **No entry in `DEVICE_QUEUE.md` does this yet.**
- **RE-READ YOUR OWN BLOCKERS BEFORE TREATING THEM AS FACTS.** rpcsx found
  **three self-declared blockers in one effort to be partly self-inflicted**: one
  blocked on integration rather than analysis, one on a proposal that was wrong
  rather than on risk, and **one on a USB cable rather than a missing sensor.**
  **Saying "blocked on hardware" when the truth is "blocked on unplugging it" is
  the difference between an item nobody can act on and thirty seconds of physical
  access.**
- **Stop after two failed or inconclusive guesses in one subsystem.** The next
  move is instrumentation, dumps, RenderDoc or Ghidra — **not a third guess.**
- **A diagnostic toggle is not a fix.** A prop, a draw skip or a forced path is
  not a fix until it becomes emulator-semantics code and passes regression
  checks.
- **A LONG WARM-UP FAILS AS A HANG, NOT AS A SLOW START.** xenia's AOT precompile
  ran at **85 functions per second** and **Android fired an ANR at 18 seconds** —
  *"Waited 5001ms for MotionEvent"* — **and the user force-closed it mid-compile.**
  Three compounding failures: the UI thread blocked in the paint path so the
  progress overlay **could not draw despite correct logic**; the progress numbers
  **grew mid-module and reset per module**, so the bar jumped backwards and read
  as a hang; and **nothing told the user to wait while Android offered "Close
  app".** Its own note: **the log said "budget 1500ms" and the pass ran ~60s**,
  because a drain flag overrode the budget. **Applies to any cache warming this
  project ships.** See
  [`shared_layer/ARTIFACT_STORE.md`](shared_layer/ARTIFACT_STORE.md).
- **READ THE PERSISTED CONFIG BEFORE TRUSTING ANY A/B.** A persisted value
  **overrides a compiled default forever** — across the process, the install and
  the app update. xenia found **three `rlwinm` fastpaths with `defaultEnabled=true`
  in code sitting false on the device**, written back when they were genuinely
  default-off pending validation. **100% of translations were on the generic slow
  path**; forcing them on measured **+2.88%, with 11 of 11 intervals favouring
  on.** Its own note: *"every device number taken this session was on a
  handicapped baseline."*
- **CHECK WHICH LAUNCH PATH SETS A DEFAULT, not that a default exists.** xenia's
  AOT object cache was enabled by a block guarded on *no cvar bundle supplied* —
  **but the launcher always attaches one when a game starts from the app.** So
  headless runs got the cache and filled it to 111 MB, **while every launch a
  person performs recompiled ~10,000 functions from scratch.** That is the
  60-second black screen and the ANR above, and **the full cache directory made
  it look like it was working.**
  **Verify a cache hit; never infer one from a non-empty cache directory.**
- **A FASTER DRIVER EXPOSES RACES A SLOWER ONE HIDES.** The same title ran stably
  on the stock Qualcomm driver at 0.8 fps and **crashed intermittently on Turnip
  at 11 fps** — *"a GPU-TIMING RACE that Turnip's FASTER GPU completion exposes;
  the slow Qualcomm driver never hits the race window."* **A shared device layer,
  a warm cache and a persisted code cache all make things faster, so latent races
  will start firing and will look like new bugs.**
- **Count driver loads in a session.** Turnip's loader degraded into a *"No Vulkan
  physical devices"* restart loop after roughly **30 load cycles in one day**, and
  a reboot and cache clear did not fix it.
- **Attract mode is not gameplay.** xenia's ledger carries this as a standing
  measurement-validity entry. **A benchmark scene must be the workload, not the
  screen the title shows when nobody is playing.**

  All of the above from rpcsx `thor-measurement-validity` and Vita3K
  `vita3k-render-experiment-gate`. See
  [`research_log/20260823_1848_fleet_skills_mined.md`](research_log/20260823_1848_fleet_skills_mined.md).
- **`CONFOUNDED` is a verdict.** A number that cannot be trusted gets labelled,
  not discarded and not promoted to a win.
- **MEASURE THE FRAME ANATOMY BEFORE DESIGNING A RENDER PATH — AND NAME THE
  INSTRUMENT.** The split between **emulation structure** and **intrinsic
  rendering work** decides whether structural work can win at all. **This list
  named fps, frame time, watts and temperature and never named the stage split.**

  **And this fleet has an instance of two instruments disagreeing by a factor of
  three about the same frame**, five days apart, same title:

  | Date | Instrument | Verdict |
  | --- | --- | --- |
  | 2026-07-04 | driver `u_trace` | **~90% fragment execution**, ~6 ms EDRAM structure |
  | **2026-07-06** | **half-resolution A/B** | **not fragment-bound** — 818 ms against 823 ms |
  | 2026-07-09 | per-pass GPU timestamps | **71% between-pass tile store**, 29% in-pass |

  **Three measurements, three answers, within five days.** The 2026-07-09 entry
  says it corrects the first; **the 2026-07-06 half-resolution A/B disproves the
  fragment-overdraw premise outright** — halving the fragments changed nothing.
  **And that fork's own verdict is that the bottleneck is "GENUINELY
  UNIDENTIFIED"**, because the only stable profile is on a driver **14x slower
  than Turnip**, so it measures the driver. **A render-path
  decision resting on one of them rests on the choice of instrument.** What is
  *not* disputed is measured directly: **bindless regressed 129 ms to 161 ms**,
  native GMEM render targets `DEAD`, fragment levers capped low — **and the
  tile-store lever did not pay either**, `CONFOUNDED` once and **`FLAT` twice.**
- **A THIRD WAY TO MAKE ARMS COMPARABLE: FIX THE WORK UNIT.** Time a **fixed
  frame range** of a deterministic opening, **content-matched by construction**.
  It removes the scene confound without a savestate — which matters because
  **Cemu and eden have none.** The three routes: **fix the work unit**,
  **normalise by something the change does not touch**, or **restore a
  savestate**.
- **CHECK THE LEDGER FOR A LATER MEASUREMENT OF THE SAME THING.** This file
  carried the 2026-07-04 anatomy as settled for most of a day because **one
  document was read and the ledger was not queried.** `exp_ledger.py check` is one
  command. **The rule "a newest failure outranks an older success" applies to
  measurements, not only to results.** See
  [`research_log/20260824_1830_two_instruments_disagreed_about_the_frame.md`](research_log/20260824_1830_two_instruments_disagreed_about_the_frame.md).
- **The per-stage GPU split is obtainable headlessly on this device**, with no
  root and no desktop GUI, through Turnip's freedreno perfetto counter producer,
  gfxreconstruct capture and replay, or in-engine per-pass Vulkan timestamps. See
  xenia's `xenia-thor-adb-gpu-stage-split` skill. **It requires the app to be
  debuggable**, which is a build-configuration requirement for the unified app
  that is recorded nowhere else.
- **Headless `adb shell perfetto` with KGSL ftrace events returns EMPTY on the
  retail Thor.** Shell is uid 2000, it can read tracefs but cannot enable events,
  and there is no `su`. **The kernel-ftrace route is dead headless** — use the
  driver's own per-context counters instead.
- **TWO FORKS GIVE OPPOSITE STACKING RULES, AND BOTH ARE RIGHT.** rpcsx: one new
  component per proof run, each individually clean first. **xenia: build and
  measure the COMPOUND, never one layer — "that's the trap that killed every
  lever."** rpcsx's rule protects **attribution**; xenia's protects
  **detection**, because a component of a multiplicative stack is individually
  below the noise floor. **Decide which regime you are in, and say which rule you
  used.**
- **STACKING RULES, from rpcsx.** These are absent from this repo and it would
  have got the last one wrong:
  - **One new component per proof run**, and only after each is individually
    clean on the same route.
  - **A component can be `stackable` without being a speed win** — a CPU-load
    reduction under an FPS cap, for example.
  - **A combined stack that fails where components passed is a
    `stack-regression`. Stop stacking and bisect to the last known-good. Do not
    add another candidate.**
  - **Do not add deltas arithmetically.** The only aggregate claim is the
    measured combined run.
  - **A newest failure outranks an older success.**
- **Two verdicts this repo lacks:** **`migration-credit`** for a change that is
  structurally right but not yet faster, and **`route-miss`** for a capture that
  is clean but of the wrong state.
- **Temperature proves the run happened.** No heating means an idle or menu
  scene, so the run is invalid whatever the counter said.
- **Query the experiment ledger before running anything.** See
  [The experiment ledger](#the-experiment-ledger). **Counted 2026-08-24 it holds
  177 entries: 75 `OPEN`, 57 `WIN`, 32 `DEAD`, 8 `FLAT`, 5 `CONFOUNDED`.** The
  **75 `OPEN` entries are analysed levers awaiting a run**, and are a resource
  this repo had never named.
- **ASK WHETHER THE CODE IS EXECUTED BEFORE ASKING FOR A BETTER INSTRUCTION.**
  **Two forks reached this independently.** rpcsx: *"the interesting question is
  almost never 'is there a better instruction', it is **is this code executed, and
  how much data goes through it**."* xenia reached it from the other end — its
  `EOR3` fusion was right on both hardware axes and died on **0 of 1 fusable
  candidates**.
- **MEASURE APPLICABILITY BEFORE BUILDING A TRANSFORM.** xenia's `EOR3`/`BCAX`
  fusion was right on both hardware axes and died because **the pattern does not
  exist**: a compile-time counter reported **"0 of 1 V128 XORs are fusable
  chains"** over a 70-second run. **One cvar and one device run replaced building
  a three-input opcode, a HIR pass and two backend fallbacks that would have
  folded nothing.**

**[`shared_layer/MEASUREMENT.md`](shared_layer/MEASUREMENT.md) indexes every
measurement rule in one screen** — before the run, choosing the instrument,
making arms comparable, reading the numbers, stacking, and the standing prior.
**It is an index, not a copy**: each row says where the rule is stated in full.
**This section grew by more than half in one day**, and the rules were correct
long before the list was navigable.

**[`DEVICE_QUEUE.md`](DEVICE_QUEUE.md) holds everything waiting on the Thor**,
with the expected signature for each run and the gates to check first. There is
one physical device, so device work is a queue and analysis is not. **Add to it
rather than leaving an experiment in a research log**, where it will not be
found when the device is free.

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

#### An analyser can find them by itself, and the corpus proves it is mechanical

**Assessed 2026-08-23. See [`shared_layer/AUTO_PATCH.md`](shared_layer/AUTO_PATCH.md).**

**The labelled corpus is dominated by one shape.** Of rpcsx's 2,676 files:
**infinite health 59, infinite ammo 26, invincibility 20, infinite HP 12, health
never decreases 12** — almost all "X never decreases".

**And the values give the implementation.** Of the parsed `be32` entries,
**`0x60000000` — PowerPC `nop` — appears 67 times**, `blr` 9, a skip-branch 6,
`li r0, 999` 3.

> **The dominant patch is "stop this instruction from happening". Authoring was
> never the hard part; finding the address was.**

**Every step of finding it already exists, in a different fork each time:** the
**typed memory scanner** is azahar's `cheats/memory_search.cpp`; the **write
watchpoint** is in xenia, ARMSX2 and Cemu; the patch is a `nop`; verification is
the scanner again. **The step that was missing is changing the value in-game
between passes — and that is the paused agent loop.**

**A first version is a cheat finder, and it is a product feature.** The person
says "stop losing health"; the agent drives to a scene, scans, takes damage,
rescans, narrows to one address, watchpoints it, emits a `nop` keyed to the
`DumpId`, and verifies by taking damage again. **Foundation point 4 calls
cheat-hunting a named RetroArch failure — a cheat the app finds for you is the
strongest answer to it.**

**Do not point the same loop at performance.** That corpus is cheats, not frame
rate. **The target is not a value**, "the frame is 100 ms" has no address; **the
intent is not observable**, where a scan can prove health stopped decreasing;
and per-game HLE work stands at **39 `WIN` against 33 `DEAD`**, so an automated
generator would produce dead ends at the same rate and **each one costs a device
run to reject.**

> **Automate the class where verification is cheap.**

**And `nop` is not always safe.** Removing a store can desynchronise game state
as easily as it can grant invincibility. **The corpus records the patches that
worked, not the ones that broke a save.**



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
**Checked twice 2026-08-23 and the mechanism holds** — searched for candidate
enumeration, then for storage-root handling; every other hit was unrelated.
**But eden solves the adjacent half and this should say so:**
`utils/PathUtil.kt` converts a Storage Access Framework `content://` URI to a
real path, **including removable SD volumes**. Vita3K answers *where is the
content*; eden answers *what real path did the person just pick*. **The shared
layer needs both.**
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

**ANSWERED 2026-08-23 by reading three files.** **3DS: yes, by construction.**
`dmnt_cheat_vm.h` marks three opcodes "not implemented by Gateway's VM", and
**Gateway is the 3DS cheat system** — dmnt was built as a superset of it. Every
Gateway opcode azahar documents maps to a dmnt one; only its block-write `E` is
not one-to-one, and that compiles to a `ControlLoop`.

**PS2: mostly, with three gaps.**

| Gap | Fix |
| --- | --- |
| **No byte swap.** `dmnt_cheat_vm.cpp` has **zero** endianness handling, and **six of pnach's nine data types** are width-and-endianness | **add an opcode.** Small |
| **No scheduling.** pnach's `place` field says apply-once-at-load or every-frame; dmnt runs the whole program per frame | **keep it out of the VM.** Carry it as metadata on the cheat |
| **The region selector is guest knowledge.** pnach picks `EE` or `IOP`; dmnt has `MainNso`/`Heap`/`Alias`/`Aslr` | **backend-declared list**, not a shared enum |

**The third gap is the same finding as texture classes and filter lists, for the
third time.** The instruction set is shared; **the region namespace is declared
by the backend.** `MemoryAccessType` must not become the shared type, for
exactly the reason `GSTextureUpscaleAlgorithm` did not.

**And there is a sixth format, which is not a format.** azahar's
`docs/thor-cheat-gaps.md` records 3DS titles whose only cheats are **NTR `.plg`
plugins — compiled ARM code, not codes.** Those compile to no bytecode at all.
That scan also found **29 of 113 games with no cheat available**, which is the
real content of the library's cheat badge.

**ALL SIX FORMATS NOW READ, 2026-08-23.** AR DS, VitaCheat `.psv`, rpcs3 YAML
and `.ncl` complete the survey.

**One opcode dominates every real corpus.** Measured: `be32` is **5,467 of
6,497** rpcs3 patch entries (**84%**), and `0` — a 32-bit write — is **60,091 of
91,372** `.ncl` lines (**66%**), across 2,501 files. **The VM's complexity is
entirely in the rare tail**, which is the number behind the tiered engine.

**A fourth gap, and it is emulator knowledge rather than guest knowledge.**
VitaCheat has `$A000`/`$A100`/`$A200` — **ARM code writes with JIT cache
invalidation**. **dmnt cannot express this and it is not an oversight**:
Atmosphere runs on real hardware, where there is no recompiler to invalidate.
**A cheat engine that only writes data will silently do nothing on a recompiled
guest**, or work until the block is next compiled. Vita3K is the only fork that
has met this.

**Endianness is confirmed a third time.** rpcs3's vocabulary is `be32`, `be16`
and `bef32`, and **both PS2 and PS3 are big-endian guests**. `bef32` is **not** a
fifth width — a float write is bit-identical to a 32-bit write once encoded, so
the front end converts it and the VM never sees a float.

**AR DS maps entirely**, including `0xC0` FOR / `0xD1` NEXT loops and a data
register with nine arithmetic operations. **melonDS refuses `0xC4`**, a
self-modifying-code trick, asking "does anything even use it??" — record it as
refused, not missing.

**Provenance is already solved in the PS3 formats.** `.ncl` carries an author per
cheat; rpcs3 YAML carries `Author`, `Notes` and `Patch Version`, keyed by
**`PPU-<hash>`** — 183 distinct hashes across 173 files. **Take that field list
for the per-game fixes this document says lack one**, and note that a
hash-keyed corpus is `DumpId` working in production.

See [`research_log/20260823_1532_cheat_format_mapping.md`](research_log/20260823_1532_cheat_format_mapping.md)
and [`research_log/20260823_1620_cheat_formats_all_six.md`](research_log/20260823_1620_cheat_formats_all_six.md).

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
  [Storage and cache visibility](#9-storage-and-cache-visibility).

**CHANGED 2026-08-23: the library is per console.** A console selector sits at
the top and an **All** entry keeps the mixed view available. See
[the working rules](#the-library-is-per-console-not-one-mixed-list).

The library still spans every backend — one set of badges, one storage
accounting, one override system. **Only the default view is split by console.**

#### Game data, covers and their API

**Specified 2026-08-23 in [`app/GAME_DATA.md`](app/GAME_DATA.md).** The question
asked was whether to mimic EmulationStation. **The answer is to take its
vocabulary and reject its data model.**

Keep from ES: **named media roles** — `cover`, `logo`, `screenshot` — so the UI
binds to a role and any art satisfying it works. Keep its small, boring
metadata field list.

Reject two ES choices, both foundational and both cheap to avoid now:

- **Identity is the file path.** Move or rename a dump and the metadata, art,
  cheats and overrides are orphaned. **The fleet already rejected this without
  coordinating**: Vita3K keys cheats by `TITLEID`, ARMSX2 by disc serial, xenia
  by an 8-hex-digit title id. **Three forks, three consoles, one answer. ES is
  the outlier.**
- **XML parsed at startup.** A large library stalls the first frame, which the
  cheap-UI rule forbids.

What replaces them:

| Question | Answer |
| --- | --- |
| What is a game | `GameKey = (system, titleId)`. Stable forever. |
| Which copy is this | `DumpId = contentHash`, separate and also needed |
| Who computes them | **the backend**, because both are guest knowledge |
| Where metadata comes from | four layers: user, scraped, bundled, derived |
| How art is requested | **by role and display size**, never as a path |

**`DumpId` is not a duplicate of `GameKey`.** A cheat targets a title; a code
patch usually targets a specific build. That is the binding problem three forks
solve separately.

**The metadata layers are the settings resolver again**, deliberately: same
sparse-override rule, same order, and that design already has tests behind it.

**No video snaps.** A video role is per-frame decode and a full-screen fill
behind a menu, which is the most expensive thing an ES theme does.

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

**READ 2026-08-23, and it is already the contract.** Do not design one; take
azahar's. It is layered exactly as this project would need — the abstract
interface in `src/core/frontend/applets/`, the guest HLE applet in
`src/core/hle/applets/`, and Qt and Android implementations behind it. **It is
GPL-2.0-or-later, so the code is usable, not only the design.**

**The shape:** a config struct in, `Setup`/`Execute`, then `Finalize`, then
`DataReady()` / `ReceiveData()` out. **Asynchronous by construction** — nothing
blocks the guest thread waiting on a person.

**Five things the three-call sketch above misses:**

- **Validation is in the shared layer, in three phases.** `ValidationError` has
  **twelve values**, split across `ValidateFilters` (as the person types),
  `ValidateInput` (on submit) and `ValidateButton` (before closing). The rules
  are guest knowledge; **when** to apply them is a UI decision, and the frontend
  chooses.
- **The guest can validate, so it is a round trip.** `Filters::enable_callback`
  means the guest checks the input itself and `ShowError` carries its rejection
  back to the person. **"Receive a string" cannot express this.**
- **Button labels come from the guest.** `ButtonConfig` is
  Single/Dual/Triple/None and the caller supplies `button_text`. The English
  defaults are `"Ok"`, `"Cancel"` and **`"I Forgot"`** — a 3DS parental-controls
  button no generic host dialog would predict. **The guest supplies the strings,
  the host supplies the styling.**
- **A default implementation is always registered.** `RegisterDefaultApplets`
  installs a `DefaultKeyboard` and `DefaultMiiSelector`, so a guest request never
  meets a missing frontend. **That is what lets a partially-implemented shell
  still boot**, which matters more here than it did in azahar.
- **The extension policy is written into the header**, in the same words this
  project chose independently: anything missing "can be added here and filled in
  by the backend HLE applet".

See [`research_log/20260823_1548_azahar_applets_read.md`](research_log/20260823_1548_azahar_applets_read.md).

### 8. Universal hotkeys, save conventions and control overlays

**One hotkey set works on every system.** This is a requirement, not a
convenience. Save state, load state, fast forward, rewind, screenshot, overlay
and menu use the same input on every backend, always.

A backend does not get to define its own hotkey. The app owns the hotkey layer
and tells the backend what happened.

### Resume where you left off: built twice, off by default twice. Turn it on

**Found 2026-08-25.** The Android-specific product question — what happens to a
game in progress when the OS takes the process away — had never been asked here.

**ARMSX2 has the complete design:** `autoSaveOnExit`, **`autoSaveIntervalMin`**
for a periodic save while playing, and **`autoLoadOnBoot`**. melonDS has
`auto_save_state_on_exit` only. **Both default to `false`.**

**Foundation point 4 says configuration is not the hobby. Save-on-exit plus
load-on-boot IS that feature, and both forks ship it off. Default it on.**

**Take ARMSX2's slot rule, which is what makes it safe.** Its own UI copy:

> *"It writes the **same auto-save slot** as the option above, **so your numbered
> slots stay yours.** Saving pauses the game for a moment, so a short interval is
> felt — **5 minutes is a good starting point**."*

**A dedicated auto-save slot, the cost stated rather than hidden, and a
recommended default.** That is the standard for every automatic feature here.

**And prefer a periodic auto-save to an `onPause` hook**, which is the obvious
design and the weaker one. **Read: melonDS's `onPause` and `onStop` write no
state**, and all three `maybeAutoSaveStateOnExit` call sites are explicit exit
paths — **so an Android process kill loses everything since the last manual
save.** A periodic save bounds the loss **however the process ends** — kill,
crash or flat battery, which ARMSX2's copy names — while a lifecycle hook covers
one of the three and runs when the OS is already reclaiming the process.
**Have both; neither fork does.**

**It collides with the integrity mode above.** `LOAD_STATE` is guarded, so
**`autoLoadOnBoot` must be suppressed while results are claimed**, or a game
launch silently restores a state and contaminates the run before the first frame.
**melonDS already knew the two meet**: `maybeAutoSaveStateOnExit` sits directly
beside `discardHardcoreSubmissions()`.

See [`research_log/20260825_0530_resume_where_you_left_off_is_built_and_off_by_default.md`](research_log/20260825_0530_resume_where_you_left_off_is_built_and_off_by_default.md).

### Rewind exists, costs 20 MB a state, and collides with an integrity mode

**Found 2026-08-25.** Rewind is on the hotkey list above and had never been
examined.

**melonDS has it, complete, in 70 lines of header.** Searched all nine forks with
four vocabularies — `rewind`, `runahead`, `rollback`, a state-ring shape — and
melonDS carries **55 files**, ARMSX2 14, the rest one or two.

**Take the design.** Configured in **seconds, not slots** — length and capture
interval, with the window size derived. **A screenshot per state**, which is what
makes it a **timeline the user scrubs** rather than a button pressed blindly; its
strings are `rewind_now` = **"NOW"** and labels like *"2m37.93s"*. The emulator
asks `ShouldCaptureState(frame)`; the manager answers.

**And it is priced, which is the part that matters here:**

```cpp
const int kRewindBufferSize     = 1024 * 1024 * 20;   // Use 20MB per savestate
const int kRewindScreenshotSize = 256 * 384 * 4;
```

> **~20.4 MB per state, preallocated, for the SMALLEST guest in the fleet.** A
> ten-second window at one-second spacing is over 200 MB.

**This file says there is ONE memory budget owner**, arbitrating the texture
cache against the shader cache on a device with a hard ceiling. **A rewind window
is a third claimant, it is large, and a settings screen tunes it.** **A backend
must declare its per-state cost** so the budget owner can refuse a window the
device cannot hold — and the cost does not generalise, since a Switch or Wii U
state is a different order of magnitude from a DS one.

**melonDS states the cost to the user in plain words**, which is the standard to
meet: *"A considerable amount of memory is also used depending on how often you
want the state to be captured and for how long it should be kept."*

### And one integrity mode governs five features this file specified separately

**melonDS disables rewind under RetroAchievements hardcore.** ARMSX2 names the
rest in its own UI text: hardcore *"prevents the usage of **save states, cheats
and slowdown functionality**"*.

| Governed | Specified in this file as |
| --- | --- |
| save states | the minimum contract |
| **cheats** | a first-class feature with a library and a search |
| **rewind** | the hotkey list |
| **slowdown** | a `TimeScale` below 100% |
| **patches that change play** | the patch engine |

**Five features, five separate specifications, one mode reaching all of them, and
it is in the contract nowhere.** `app/shell/IntegrityMode.kt` adds it, with 8
tests pinning ARMSX2's two shipped bugs.

**The first bug is a sixth variant of the settings symptom**, and worse than the
others: *"the bundled `patches.zip` stays enabled in hardcore, so a
widescreen/no-interlace/bug-fix patch worked when it shipped in our zip and
silently did nothing when the same patch sat on disk. That killed everything the
in-app Patch Manager writes ... **with no message explaining why**."*

> **The user moved a switch and something ELSE stopped working, silently.** So
> the mode must be able to **list what enabling it costs, before it is enabled.**

**Fast forward is deliberately NOT guarded**, because ARMSX2's text names
slowdown and nothing found says otherwise. **Recorded as unanswered rather than
assumed.**

See [`research_log/20260825_0410_rewind_and_the_integrity_mode_nobody_designed.md`](research_log/20260825_0410_rewind_and_the_integrity_mode_nobody_designed.md).

### Fast forward is a TIME SCALE, and a boolean in the contract ships a known bug

**Found 2026-08-25.** The hotkey list above names fast forward and rewind. **It
never said what fast forward means to a backend, and "run the loop faster" is
wrong for most of this fleet.**

**Vita3K shipped the toggle twice and it did nothing both times.** The first pass
scaled `sceKernelDelayThread`, kernel wait timeouts and timer scheduling; the
user reported gameplay still ran at real time. **The second pass had to add an
anchored speeded process clock** and route `sceKernelGetProcessTime`,
`GetSystemTimeWide`, `LibcClock`, `LibcTime`, `LibcGettimeofday`, the RTC APIs,
thread start ticks **and NetCtl adhoc peer timing** through it.

> **The switch moved and nothing happened — the settings symptom with a fifth
> cause. The feature is not a value, it is a cross-cutting property of every
> clock in the backend.**

**And the cost splits on HLE against LLE, the same axis that decides whether API
translation is available:**

| Fork | Guest time from | Mechanism | Size |
| --- | --- | --- | --- |
| **melonDS** | emulated cycles | **a bool the frame limiter reads** | **3 references** |
| ARMSX2 | emulated cycles | limiter, **plus a fast-forward volume** | — |
| eden | HLE kernel | `GetClockTicks()` **divides by the speed limit**, behind `sync_core_speed` | one function |
| **Vita3K** | HLE kernel | **an anchored clock across ~12 time APIs**, plus audio and vblank | two attempts |

**A backend whose guest asks the HOST what time it is gets real time no matter
how fast the loop runs.** Most of this fleet is that kind.

**Three forks link audio to speed and they do three different things**, searched
across all eight with seven vocabularies and every hit read: Vita3K retimes
against `speed_percent`; ARMSX2 drives resampling and time-stretching from a
nominal rate **and adds `SPU2/Output/FastForwardVolume`**; and **azahar
time-stretches when emulation speed drops to 95 or below.**

> **azahar's runs in the opposite direction and is probably worth more here. On
> a thermally-limited handheld, running BELOW 100% is the common case**, and
> keeping audio intact while the emulator cannot keep up is a
> quality-of-life feature a fast-forward design would never have found.

**The contract consequence, implemented in `app/shell/TimeScale.kt` with 12
tests:** a **scale**, not a toggle — pause is the same mechanism at zero, which
is what the paused agent loop already relies on — and a backend **declares which
clock domains it scales**. A backend that HLEs guest time from the host clock and
does not declare `HOST_DERIVED_TIME` is **refused**, so the bug Vita3K shipped
becomes visible at integration rather than in a user report.

See [`research_log/20260825_0245_fast_forward_is_a_time_scale_not_a_toggle.md`](research_log/20260825_0245_fast_forward_is_a_time_scale_not_a_toggle.md).

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

   **AND A STATIC ONE THAT NEEDS NO DEVICE, added 2026-08-23.** See
   [`shared_layer/STATIC_TRIAGE.md`](shared_layer/STATIC_TRIAGE.md).

   **Every console executable declares what it needs** — XEX, RPL, SELF, NSO,
   NCCH and PRX all carry an **import table**. **Every emulator's HLE layer is a
   list of what it provides.** The difference is a risk score, computable from
   the dump with no boot.

   **Measured on Vita3K: 7,377 HLE functions, 6,285 stubbed — 85%.** The
   aggregate is not the useful number; **the distribution is**:

   | Module | Functions | Stubbed |
   | --- | --- | --- |
   | **`SceGxm`** — graphics | 292 | **23%** |
   | `SceLibKernel` | 371 | 61% |
   | `SceLibc` | 1,071 | 98% |
   | **`SceLibMonoBridge`** | 296 | **100%** |
   | **`SceLibXml`** | 186 | **100%** |

   **A title using `SceGxm` and the thread manager sits in the best-covered part
   of the emulator. One using `SceLibMonoBridge` is asking for 296 functions of
   which none is implemented.** That is knowable before it boots.

   **And inverting the query gives a development priority list that is computed
   rather than argued**: which stubbed function is imported by the most titles
   in the library.

   **The same split as API translation applies.** A console that shipped its
   system software as separate modules leaves an import table — **Vita3K, eden,
   Cemu**. One that linked everything into the game does not, so **PS2 and DS
   are bare metal and need a signature scan instead**, which is what xenia's
   `SetupExtern` already is.

   **Calibrate before trusting it.** A `STUBBED` marker does not mean broken —
   many stubs correctly return success, and `SceLibc` at 98% is not 98% broken
   because most of libc is forwarded to the host. **Run the count over titles
   known to work: the stubs they import are proven harmless, and what remains is
   the real signal.**

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

- **A DIFFERENTIAL TEST IS ONLY MEANINGFUL IF BOTH SIDES WERE BUILT WITH THE SAME
  FLOATING-POINT SEMANTICS.** ARMSX2's Release build emits **`-ffp-contract=fast`
  on 320 translation units**, of which **44 are guest-FP code** — `FPU.cpp`,
  `Interpreter.cpp`, `COP2.cpp`, `R5900OpcodeImpl.cpp`. That flag lets the
  compiler fuse `a*b+c` into an **FMA, rounding once instead of twice**.
  **The recompiler is unaffected**, because its output is emitted machine code —
  **so the flag changes the reference, not the implementation.** Its CPU
  differential suite compares interpreter against recompiler, and **a fused
  reference against an unfused implementation would attribute the disagreement to
  the recompiler.** **A hazard to check, not a proven bug**, and checkable with no
  device.
  **And the target decides the effect**: baseline x86-64 has no FMA, so the flag
  is nearly inert there; **ARM64 has FMA in the baseline, so the fusion actually
  happens.** See
  [`research_log/20260824_1745_fp_contraction_reaches_the_reference_interpreter.md`](research_log/20260824_1745_fp_contraction_reaches_the_reference_interpreter.md).
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

### THE PATTERN, 2026-08-24: every win in this fleet was a bug, not an optimisation

**rpcsx's GPU review ends with the most useful sentence found in the fleet:**

> **This emulator's ARM64 and GPU paths are already well matched to the hardware.
> The wins have come from code that was broken, not code that was slow.**

**Tested against everything read that day, the split is total.**

**TESTED AGAINST THE COMPLETE `WIN` LIST 2026-08-25, and the claim gets wider
rather than weaker.** All 57 classified: **26 are `rearch` milestones of a
build-out whose own premise was later refuted**, 8 are on-device bring-up, 6 are
measurements, 6 are GPU-path confirmations, 2 are `fill` isolations of which one
is explicitly lossy. **Nine `cpu` entries carry per-lever speed numbers, and six
of the nine are bugs.**

**One entry is a genuine reasoned optimisation of working code**, and reading it
settles the question rather than opening it. `a64_stackpoint_prolog_fastpath`
took a guest prolog from 18 emitted instructions to 14 for **+2.04%, 11 of 11
intervals**. What it replaced: **`MOV`+`CMP` with a compare against an encoded
immediate; `MOV`+`UMULL`+`ADD` with a shifted-register `ADD` for a 16-byte struct
index; and a re-load of a value already live in `w9`.** **x86 folds
`[base + index*16]` into an addressing mode, so an explicit multiply is free
there and is not free here.**

> **So the statement should be wider: the wins have come from code that was WRONG
> FOR THIS MACHINE — either broken outright, or shaped for a machine this is
> not.** That covers the counter-example instead of being embarrassed by it, and
> it is the same lens rpcsx calls its most productive heuristic.

**And a `WIN` in that ledger means "decisive result", not "got faster".** One WIN
is a route being unblocked; another is a **profile whose content is a negative** —
that the busy-wait fastpath this file calls the highest-value CPU work available
**does not apply to Gears at all**, because its load is distributed with no
dominant function. **Quoting "57 wins" as 57 speedups is wrong by an order of
magnitude.**

**Two ways to misread the ledger, both now recorded.** A lever that silently never
fires **turns every measurement taken through it into a false negative** —
xenia's desktop HLE intercepts read `count=0` for weeks from a `kExtern` dispatch
bug, and the fix **corrected an earlier research conclusion built on that zero**.
And **an undated `backfill` entry defeats recency ordering**: register residency
holds a `WIN` dated `backfill` and a `CONFOUNDED` dated 2026-07-24, and the `WIN`
prints first.

See [`research_log/20260825_0030_fifty_seven_wins_classified.md`](research_log/20260825_0030_fifty_seven_wins_classified.md).

| What paid | What it was |
| --- | --- |
| 74% of cycles in a nop-spin | a **timing constant** correct on x86, wrong by ~1300x here |
| `rlwinm` fastpaths off, **+2.88%** | a **stale persisted config** beating a compiled default |
| AOT object cache never enabled | a **guard on the wrong launch path** |
| guest core N pinned to host core N | a **guest index used as a host index** on big.LITTLE |
| ARMSX2 frame generation stuck at fp32 | an **extension the device layer never requests** |
| eden's NCE patches re-derived every launch | a **cache key declared and never used** |

| What did not pay | Result |
| --- | --- |
| native render path rearch | frame anatomy: **~7 ms of structure to reclaim** |
| bindless resources | **regressed** 129 ms to 161 ms |
| `EOR3`/`BCAX` fusion | **0 of 1 candidates** |
| `TBL2` for `TBX2` | 0.555 against 0.555 |
| `LOAD_OP_CLEAR` conversion | **12.39% against 12.65% GPU busy** |
| A510 shared vector unit | pairs scale **near-linearly** |
| `ISB` for `yield` | **+23% regression** swapped alone |

**Three consequences.**

**It reframes the section below.** Expect maintenance wins, yes — **but the frame
wins do exist, and they are in broken code rather than slow code.**

**It changes what the shared layer is for.** The stated case is one device, one
cache, one budget, one scheduler. **The stronger case is that a shared owner makes
a broken thing visible in eight backends at once** — and every row in the win
column is a **class**, not an instance. A guest index used as a host index was
found in two forks. A capability the device layer never requests is the whole
capability census. **A cache that is off on the path people use is the failure
mode this project's own artifact store is designing toward.**

**And it is a warning about this project's instincts.** Most of what this repo has
proposed is optimisation — a render graph, an upscaler, instruction repurposing,
an IR decision. **The fleet's record says that lane is where the refutations
live.** See
[`research_log/20260824_0855_every_win_was_a_bug.md`](research_log/20260824_0855_every_win_was_a_bug.md).

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

**QUALIFIED 2026-08-23, and the qualification is large.** xenia wrote a 200-line
verdict on this — `20260711-rexglue-gpu-dxvk-for-360-verdict-56sol.md` — and
**this repo had not read it.**

**API translation requires an API boundary, and that is a property of the
console, not of the emulator.** DXVK works because it **replaces `d3d9.dll`**.
On the 360 the XDK's D3D9 runtime is **linked into the XEX and becomes ordinary
PPC game code**, so by the time xenia sees anything, `SetRenderTarget`, `Resolve`
and `DrawIndexedPrimitive` **have already been lowered into PM4**, which has
**discarded logical resource identity** — no surface handles, only EDRAM address,
pitch, format and MSAA state, with height not even directly specified.

> **BD's D3D9 usage is translatable, but only after reconstructing the
> high-level resource/state API that PM4 erased.**

**And "AOT-recompile the GPU draws" is a category error**, because PM4 buffers
are runtime data.

**The fleet splits three ways, verified by reading each fork:**

| Backend | Guest graphics arrives as | Boundary |
| --- | --- | --- |
| **GameThor** | `d3d9.dll`, **replaced** by DXVK | **API, by replacement** |
| **Vita3K** | **`SceGxm`, HLE'd with identity intact** — 5,710 lines, `SceGxmContext` carries the bound programs | **API** |
| **Cemu** | GX2 intercepted in 35 files, **then lowered to PM4 one line into the draw** | **available, declined** |
| **xenia, ARMSX2, melonDS, azahar** | PM4, GIF packets, registers, GSP lists | **none** |

**So the fork that named this direction is one of the four that cannot take
it**, and **Vita3K is already an API translator** whether or not it says so.
**Cemu is the only place where the question is open** — it has the boundary and
gives it up, which is defensible because GX2 is a thin wrapper over Latte and a
Wii U game can bypass it.

**And the existence proof proves less than this document has been claiming.**
xenia's own caution:

> RE2 proves the Thor can run a much larger modern renderer. **It does not prove
> whether BD's current 100 ms is GPU execution, GPU starvation, or guest CPU
> work.**

**If unrelated guest CPU work already exceeds 33.3 ms, a perfect renderer
delivers nothing.**

See [`research_log/20260823_1955_api_translation_boundary.md`](research_log/20260823_1955_api_translation_boundary.md).

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

**Started 2026-08-23. melonDS-android: 15 min 27 s clean, exit 0, 55.5 MB APK,
89 tasks.** Its 2026-07-12 recipe worked unchanged, which is worth knowing
because a stale recipe is worse than none.

**The time is not where anyone would guess.** It is dominated by building
`librashader` and its Rust dependency graph — `glslang`, `spirv-cross2`,
`gpu-allocator`, then eight `librashader-*` crates. **The emulator's own C++
compiled quickly.** So a shared-layer change will not cost 15 minutes on this
fork; a clean checkout will, and the two should be measured separately before
anyone plans CI around the figure.

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

   - **FOUND 2026-08-24: a FOURTH bug, and a fourth mechanism.** Its in-game
     **"Load Texture Packs"** switch **wrote the persisted value and never fired
     the live GS reconfigure**, so an imported pack did not appear until the next
     game boot. **The switch moved and nothing happened** — the PINE symptom
     reached a third distinct way.

     **The cause matters more than the bug.** ARMSX2 decides what needs a live
     reconfigure with a **hand-written chain of `!=` comparisons, one line per
     field**. **A setting added without touching that chain silently gets no live
     apply**, and nobody finds out until somebody flips the switch mid-game.

     > **Derive the live-apply set from the setting specs. Never enumerate it.**
     > `SettingSpec` already carries `liveChangeable`; a second hand-maintained
     > list is the bug. `SettingResolver.applyPlan` in `app/shell/` does this,
     > with a test that adding a spec is sufficient.

   **A contract that specifies pinning without change-tracking ships the second
   bug.** **Four bugs, four mechanisms, one user-visible symptom**: a control
   that moves and does nothing. **That symptom is the settings system's only
   failure mode, and it has at least four causes.**

   **And a fifth cause exists outside ARMSX2: a second writer.** rpcsx's
   `Max LLVM Compile Threads` was set in `config.yml`, **and** by
   `ThorPerformanceProfile` on every boot, **and** in a per-game managed profile.
   **Editing the config alone was silently undone at the next launch.** A profile
   applier that writes unconditionally at startup **bypasses the change-tracking
   in rule 3 entirely.** Swept as
   `tools/bug_class_sweep.py --class setting_written_by_multiple_writers`.
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

**CORRECTED 2026-08-23: GameThor has 2,136 lines of it.**
`utils/ContainerStorageManager.kt` (994), its dialog (911) and
`utils/StorageUtils.kt` (231). **The fourteenth absolute negative in this repo to
be wrong**, missed because GameThor is Tier 2 and the census counted its files
without reading them.

**It already has:** `loadEntries()` aggregation, `getAvailableSpace` via
`StatFs`, `getFolderSize` by tree walk, `formatBinarySize` in **KiB/MiB/GiB**
rather than decimal units, `removeContainer` and `uninstallGameAndContainer`.

**And it already has the category split in miniature** — `Entry` carries
`containerSizeBytes` and `gameInstallSizeBytes` separately, with a
`combinedSizeBytes`. **The Wine container is rebuildable and the game install is
not**, which is exactly the distinction "a cache is an asset, not junk" turns on.

**It also has something this design does not: `moveGame`**, with
`canMoveToExternal` and `canMoveToInternal`. **On a handheld with an SD card,
moving is the action a person actually wants** — deleting a 12 GB game is a loss,
moving it is not. **Add it.**

**What is genuinely still missing is narrower:** no fork breaks storage down by
**nine categories with a `rebuildable` flag**. GameThor has two because a Wine
game has two parts. **Take the screen, add the categories.**

**Method for that narrower negative:** all eight forks were searched for
`formatFileSize|StatFs|getUsableSpace|folderSize|calculateSize|space_info`, then
GameThor's three storage files were read in full. **Every other hit was a memory
card viewer, a game-list size label, or guest-side filesystem code.**

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

**STALE, corrected 2026-08-23. Four forks now build on this box, all on the
standard row**, so melonDS is no longer the only known-good starting point:

| Fork | On the standard row |
| --- | --- |
| **Cemu** | **5 min 8 s** |
| **ARMSX2** | **9 min 3 s** |
| **azahar** | 14 min 49 s |
| **melonDS** | 18 min 12 s |

**Two do not build, and neither failure is in an emulator.** Vita3K's own recipe
runs gradle from the wrong directory and its prebuilt FFmpeg has no `x86_64`
build — **`arm64-v8a` configures and compiles cleanly.** eden needs `pkg-config`
and then **`glslangValidator`, which the NDK does not ship** — it provides
`glslc`. GameThor cannot resolve a SNAPSHOT dependency that no longer exists.

**Start with Cemu**, which is fastest because **it compiles none of its
dependencies** — vcpkg supplies them prebuilt. **Source-built dependencies drive
build time, not emulator size.**

Then continue in rising order of build cost. Leave ARMSX2, Cemu-thor and
xenia-thor until last. They are the most expensive to build.

**Nothing can be shared until this phase ends.** Seven C++ runtimes cannot
share native code.

### The measured spread, 2026-08-23

**Read from each fork's wrapper rather than assumed.**

| Fork | Gradle | JDK needed |
| --- | --- | --- |
| **xenia** | **7.3.3** | — |
| GameThor | 8.12.1 | — |
| eden | 8.13 | 17 |
| azahar | 8.14.5 | **17** |
| Cemu | 9.3.1 | 21 |
| Vita3K | 9.4.1 | 21 |
| ARMSX2 | 9.4.1 | **17** |
| melonDS | 9.5.0 | 21 |
| **standard row** | **9.6.1+** | — |

**Seven distinct Gradle versions, and no fork is on the row's value.** The
spread is 7.3.3 to 9.5.0 — **two major versions** — and at least two JDKs are
required across the fleet.

**That is larger than "the newest already in the fleet" implied**, and it is the
real content of Phase 1. **azahar's own `AGENTS.md` instructs keeping it on
8.14.5**, so moving it is a decision against a fork's standing instruction, not
just a version bump.

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

   **BUT DO NOT MAKE SAVESTATE FIXTURES THE GENERAL MECHANISM. Measured
   2026-08-24: three backends cannot supply a deterministic scene at all.**

   | Fork | Savestate | Input replay |
   | --- | --- | --- |
   | ARMSX2 | full, versioned | candidate |
   | melonDS, azahar | yes | azahar has `core/movie.cpp` |
   | Vita3K, rpcsx | yes | no |
   | **xenia** | **exists and deadlocks** | candidate |
   | **Cemu** | **none** — its only `SaveState` is `SaveStateToConfig()` in the graphic-packs GUI | **none** |
   | **eden** | **none** — `CoreError.ErrorSavestate` is an enum value the core never raises | weak |

   **The noise floors make this decisive.** Without a savestate or a replay, the
   only route to a scene is pressing through cutscenes, measured at **~+/-50%**.
   **A harness that runs on five backends and silently skips three reads as a
   passing suite.**

   **Two consequences.** The paused agent loop stops being a nice capability and
   becomes **the only deterministic route for three backends**. And **xenia's
   save-state deadlock belongs in the queue** — its own audit calls fixing it the
   highest-leverage unblock it has, and this repo had not recorded it.
   `DEVICE_QUEUE.md` entry 20.

   See [`research_log/20260824_0255_three_backends_cannot_be_measured.md`](research_log/20260824_0255_three_backends_cannot_be_measured.md).

   **A cost of the savestate fixture, priced 2026-08-23 and not priced before.**
   A change that alters guest state layout **invalidates every fixture for that
   backend at once**. The golden images survive, but nothing can reach the scene
   that produced them, so that console's suite goes dark until the fixtures are
   regenerated. **This will happen**, because the shared layer changes backends
   on purpose — and ARMSX2 already carries a `SaveStateLegacy.cpp`, so its state
   format has changed at least once already.

   Three rules follow:

   - **A fixture records the state version it was made with**, and the harness
     fails loudly on mismatch. **A quietly wrong fixture is worse than a missing
     one**, because it produces a golden-image diff that reads as a rendering
     regression.
   - **Prefer a rebuildable fixture**: a ROM hash plus a recorded input replay
     can regenerate the state. azahar already has deterministic input replay in
     `core/movie.cpp`.
   - **Treat a state-format change as a schema migration**, with the same two
     rules as the settings schema: version it, and never deserialise old data
     with the current structure.

   See [`research_log/20260823_0020_save_conventions.md`](research_log/20260823_0020_save_conventions.md).
3. The ARMSX2 golden image comparer, `comparer.js`.
4. The ARMSX2 headless replay pattern, `pcsx2-gsrunner`.
5. The two existing agent skills, from xenia-thor and Vita3K-Thor.

Build the MCP on-device surface against
`armsx2-thor/ARMSX2/docs/mcp-server.md`.

**After this phase, an agent can tell a good port from a regression.** Before
it, the fan-out in [Agentic acceleration](#agentic-acceleration) does damage.

### Phase 4 — per-class routing and upscaling

The flagship feature. It is safe to attempt only after phase 3.

**CORRECTED TWICE. There is no shared algorithm enum.** The filter list is
**declared by the backend**, exactly like the texture class list, for exactly
the same reason.

**The names are not a shared vocabulary.** ARMSX2's `Anime4K` is a **neural
network** — `anime4k_x2.a2nn`, `GSTextureUpscalerNN.cpp`. melonDS's `Anime4K
lite` is a **nine-texel kernel** built on `MMPXLiteTexel`. **Same name, unrelated
techniques.** A shared enum keyed on names would merge them silently, and a
person choosing "Anime4K" would get something different per backend with no way
to tell.

melonDS's modes also **compose each other** — `CrispGradientTexel` blends its
Anime4K-lite with `SuperSAIStrongTexel` and sharpens — so its list is a set of
hand-tuned recipes, not published algorithms at different cost points.

**This repo already made this argument, for classes:** *a fixed enum would
impose one emulator's taxonomy on the rest.* **It is true of filters word for
word.**

So the shared layer owns the **routing** — which class goes to which filter —
and the UI. **The filter list is backend knowledge.** ARMSX2's
`GSTextureUpscaleAlgorithm` stays valuable as a vocabulary and for its grouping
comments; it stops being the shared type.

**Two corrections to the earlier instruction:**

**`Quilez` is a present-time filter and must not go in this enum.** melonDS
keeps two separate string arrays: `video_filtering_options` holds None, Linear,
2xBR, HQ2X, HQ4X and **Quilez**, which is whole-frame output filtering. The
texture list is the other one. `Super2xSaI` is genuinely in the texture list;
`Quilez` is not. **Adding it would be the exact category error this document
warns about** in [Per-class routing](#2-per-class-routing--the-first-shared-feature):
a present-time shader sees one finished frame and cannot separate an anime
portrait from a wall.

**The shared type needs two axes, because the forks disagree on which axis
matters.** melonDS's list is `HQ2x lite`, `2xSaI lite`, `SuperEagle lite`,
`MMPX lite`, `Anime4K lite`, `Super2xSaI strong`, `SuperEagle smooth`, plus
`crisp gradient` and `crisp edge AA`. **Five of those are `lite` and two are
strength variants** — the same algorithms at different cost points, not
different algorithms.

| | ARMSX2 | melonDS |
| --- | --- | --- |
| Axis | algorithm identity | **cost and strength tier** |
| Can express "xBR, but cheap" | **no** | yes |
| Can express "Lanczos" | yes | no |

**Flattening melonDS's list into ARMSX2's loses the cost axis, and on a handheld
that is the axis that decides whether a filter is usable.** So carry
`algorithm` and `tier` separately, and let the implemented pairs be **declared**
rather than fixed, exactly as texture classes are.

**A rule worth taking, stated inside ARMSX2's enum:** the enum is persisted as
an integer, so **entries can only ever be appended**. Group in the UI, never in
the numbering.

See [`research_log/20260822_2350_upscale_algorithm_sets.md`](research_log/20260822_2350_upscale_algorithm_sets.md).

### Later

- The game library, cover art and per-game overrides. Survey xenia-thor
  `GameProfiles.java` first. It is the most complete Android shell in the fleet
  and the worst structured; take the features, not the navigation. It is the
  fleet.
- Cheat database unification.
- Mod and translation loading.

## Open decisions

These are not settled. Do not assume an answer. Ask, or mark the assumption.

1. **The toolchain row: chosen, not verified.** The values are written in
   [One toolchain](#0-one-toolchain--do-this-first) and Oboe was added
   2026-08-23. **No fork has been migrated to them, and C++20 is verified for
   exactly one file** — `shared_layer/thor_backend.h` compiles under NDK 29
   clang++ at C++20 for `aarch64-linux-android33`. **That is not evidence about
   any fork.**

   **The gap is now measured for one fork.** melonDS-android builds today on
   **NDK 28 and Gradle 9.5.0**, so it is one NDK major and one Gradle minor
   behind the row. Seven forks unmeasured.

2. **Dependency unification.** Which single version of Oboe, `imgui`, `fmt`,
   `glslang` and `vulkan-headers` does the fleet use? **The packed binary
   cannot link five copies**, and the audio half is answered — Oboe.

   **ANSWERED IN PART 2026-08-23, from the built binaries.** The question has a
   sharper shape than "pick one version of each".

   **A dependency is safe when it versions its own ABI, not when it is C++.**

   | Tier | Libraries | Why |
   | --- | --- | --- |
   | **safe to leave alone** | **fmt** | inline ABI namespace. **xenia is on `v6` and three forks on `v12`, and they cannot collide** — different mangling. ~142 colliding symbols against OpenSSL's ~6,400 |
   | already unified | **libc++** | `std::__ndk1`, identical in all six. Supplied by the NDK |
   | **must be unified** | **OpenSSL, zlib, SDL, libpng** | plain C. Same symbol names, different behaviour, nothing to separate them |
   | **must be unified** | **imgui** | C++ **without** a versioned namespace, so 1.91.3 and 1.92.6 mangle identically |
   | **must be unified** | **boost, glslang** | C++, also without a version namespace — plain `boost::archive`, `glslang::`, `TIntermNode`. **For these the version number is moot: any two copies collide** |

   **Every plain-C dependency readable in two forks disagreed on version:**
   OpenSSL **3.5.0** (Cemu) against **3.6.2** (Vita3K); SDL **3.5.0** (ARMSX2)
   against **3.2.28** (Vita3K); imgui **1.92.6** against **1.91.3**; zlib
   **1.3.1** against **1.3.2**.

   **SDL is the sharpest case**, because it compounds the JNI blocker: ARMSX2 and
   Vita3K both export `JNI_OnLoad` **and they are different SDL versions.**

   **eden is measurable without building it.** Its `.cache/cpm/` is a resolved
   dependency manifest from a real configure run — **28 packages with versions**.
   OpenSSL **3.6.0**, fmt **12.1.0**, zlib **1.3.2**, boost **1.90.0**, Oboe
   **1.10.0**, VMA **3.3.0**, Vulkan-Headers **1.4.345**.

   **So OpenSSL is the most fragmented dependency in the fleet — three distinct
   versions across three forks** (Cemu 3.5.0, eden 3.6.0, Vita3K 3.6.2), with
   azahar carrying a fourth copy unread. **It is also the largest collider. Put
   it first.**

   **eden's Android build is narrower than its dependency list.** `ENABLE_CUBEB`
   is **OFF** on Android so it uses Oboe; **SDL2 is forced off** and no file under
   `src/android/` references SDL, so **eden does not add to the `JNI_OnLoad`
   problem**; Discord depends on Qt, which is off. `ENABLE_WEB_SERVICE=1` is what
   pulls OpenSSL in.

   **Anomaly, unresolved:** the submodule table below records **Vita3K pinning
   `glslang` at the same commit as azahar**, yet **Vita3K exports zero glslang
   symbols** out of 98,550, while azahar exports 1,446 and Cemu 4,539. Either it
   hides them or this configuration does not use it. **Worth knowing before
   counting it as a collision.**

   **Limit: a lower bound.** Only libraries embedding a printable version string
   are readable this way, and azahar and melonDS yielded almost nothing. eden
   does not build. See
   [`research_log/20260823_1600_vendored_versions_from_binaries.md`](research_log/20260823_1600_vendored_versions_from_binaries.md).

   **The submodule data below still stands and does not conflict.** Shared
   ancestry produces agreement — azahar and Vita3K pin identical `glslang` and
   `xxHash` commits — while independent vendoring produces drift.

   **First data, 2026-08-23.** Pinned commits read from each fork's git tree:

   | Library | azahar | Cemu | Vita3K | xenia |
   | --- | --- | --- | --- | --- |
   | `fmt` | `e424e3f2` | — | `1be298e1` | `27e3c0fe` |
   | `Vulkan-Headers` | `409c16be` | `9b9fd871` | — | `31aa7f63` |
   | **`glslang`** | **`fc9889c8`** | — | **`fc9889c8`** | `f4f1d8a3` |
   | `imgui` | — | `f65bcf48` | `cb16568f` | `81160fee` |
   | **`xxHash`** | **`e626a72b`** | — | **`e626a72b`** | `4c881f79` |

   **azahar and Vita3K already pin the identical commit for `glslang` and
   `xxHash`** — the shared-ancestry effect showing up in dependency pins rather
   than in source.

   **So start there: those two cost one fork each to unify, the rest cost
   two.** And `glslang` matters most, because **three forks need it at run
   time** to compile GLSL text.

   **Limits:** a different SHA does not mean far apart, and only four forks are
   covered — ARMSX2 and eden vendor by copying and have no pinned commit to
   read. See
   [`research_log/20260823_0200_vendored_versions.md`](research_log/20260823_0200_vendored_versions.md).

3. **The build location.** Options: local Windows or WSL, GitHub Actions, or a
   split. This decision sets how much an agent can do unattended.

   **Four forks measured 2026-08-23, and the assumption that local builds are
   too slow to automate is wrong.**

   | Fork | Time | ABIs | Dependencies built from source |
   | --- | --- | --- | --- |
   | **Cemu** | **~2 min 42 s** native | 1 | **none — vcpkg prebuilt** |
   | ARMSX2 | 11 min 25 s | 1 | librashader (Rust), shaderc |
   | azahar | 14 min 33 s | 1 | **2,206 targets** |
   | melonDS | 15 min 27 s | **3** | librashader (Rust) |

   **Emulator size does not predict build time. Source-built dependencies do.**
   Cemu has the second-largest native codebase and the fastest build, because
   **it compiles none of its dependencies** — vcpkg supplies them prebuilt, so
   only its own 498 objects are built. ARMSX2 has the largest codebase and is
   second fastest.

   **This refines the ABI finding rather than contradicting it.** rpcsx's
   "doubled the native compile" holds, but **the native compile is not the whole
   build**, and for three of these four it is not the dominant part. melonDS
   builds three ABIs and is slowest, yet its log is dominated by **Rust**.

   **So the bigger lever is prebuilding dependencies, not cutting ABIs** — and
   that is the dependency-unification argument arriving from a second direction.
   The packed binary cannot **link** five copies of `glslang`; the fleet also
   **compiles** it four times at every clean build.

   See [`work_log/20260823_0243_cemu_build_and_what_drives_build_time.md`](work_log/20260823_0243_cemu_build_and_what_drives_build_time.md).

   **And the shape of the cost is the useful part.** The time is dominated by
   compiling `librashader`'s Rust dependency graph, not the emulator's C++, so
   it is **cacheable across builds and across forks** — a shared Cargo and
   Gradle cache attacks the dominant cost directly. **Measure incremental
   separately before choosing**, because a clean-checkout figure is the wrong
   input for a decision about routine agent work.

   ARMSX2, Cemu-thor and xenia-thor remain unmeasured and are the ones the
   worry was really about.
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
