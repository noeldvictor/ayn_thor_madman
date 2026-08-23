# What unification actually means here

**Written 2026-08-23, after a day in which every code-duplication claim in this
repo shrank on reading.**

This document exists because the project's stated thesis — *solve a problem once
and apply it everywhere* — kept being **right in outcome and wrong in
mechanism**. The forks are not duplicating code. Something else is happening,
and naming it changes what to build first.

---

## 1. The observation that started this

Nine extraction candidates have now been read rather than listed. **The
split is total.**

| Survived reading | Shrank on reading |
| --- | --- |
| Vulkan device layer | LRU cache |
| Driver pipeline cache blob | Texture cache hashing |
| GPU driver manager | A shared renderer |
| Code cache | Render pass structure |
| Touch overlay | Upscale algorithm **list** |
| Settings framework | |
| Cheat and patch engines | |

**There is a property that separates these two columns exactly, and it is not
how similar the code looks.**

## 2. The rule: unify what is forced, harvest what is chosen

**Every candidate that survived is one where keeping N implementations is
impossible. Every candidate that shrank is one where keeping N is merely
untidy.**

Three things force unification here:

| Forced by | Consequence |
| --- | --- |
| **The linker** | One binary cannot hold seven Vulkan devices, five copies of cubeb, or four versions of `fmt`. |
| **The device** | One GPU, one loaded driver, one memory budget, one prime core. A second owner of any of them is a bug, not a duplicate. |
| **The product promise** | One app, one settings system, one hotkey set, one cheat library. A backend drawing its own settings screen breaks the promise, however good the screen is. |

**Nothing else forces anything.** Two LRU caches cost maintenance. They do not
cost correctness, frames, or the product.

### The rule, and it is predictive

> **A candidate forced by the linker, the device or the product promise will
> survive reading. A candidate justified only by "these look the same" will
> shrink.**

**This explains 100% of the results so far**, including the ones that were
surprising at the time. It is worth stating as a prediction because it can be
wrong: **the next candidate that shrinks despite being forced falsifies it.**

### What it changes

**Stop opening extraction candidates by looking for similar code.** Start by
asking what the packed binary, the hardware, or the product cannot tolerate two
of. That list is short, mostly known, and every item on it is real.

**And stop treating "six forks have a driver picker" as evidence.** It was not.
What made the driver manager real is that **one process loads one driver**.

---

## 3. The deeper finding: the unit of waste is a lesson, not a function

If the forks are not duplicating code, what is actually being wasted?

**The same lessons, learned separately, by each fork, at full price.**

| Lesson | Learned by | Still unlearned by |
| --- | --- | --- |
| A per-game override must be **sticky**, or a later global change silently takes it | ARMSX2, from a bug report | everyone else |
| Pinning needs **change-tracking**, or a stale write makes a wrong value permanent | ARMSX2, from a second bug report | everyone else |
| A `Presentation` is **not torn down** when the activity stops | ARMSX2, from a bug report | everyone else |
| Depth attachments should default to `DONT_CARE` | Cemu | eden, which loads and stores unconditionally |
| Transient attachments need `DontCare` **both ways** | Vita3K | everyone else |
| A physical controller must **hide** the touch overlay | Vita3K | everyone else |
| Building `x86_64` doubles the native compile | rpcsx, measured | five forks |
| A migration must not deserialise with the **current** class | melonDS | everyone else |

**None of that is code duplication. All of it is duplicated learning**, and it
is the expensive kind because each fork pays in bugs rather than in keystrokes.

**This is why every duplication claim shrank and the project is still right.**
The waste is real. It was being measured with the wrong instrument.

---

## 4. Six kinds of unification, and they are not interchangeable

The repo has been using one word for six different things.

| Kind | Example | Crosses a licence boundary? |
| --- | --- | --- |
| **Forced code** | one Vulkan device | no — inherits the source licence |
| **Design** | azahar and eden's settings framework, fully diverged | **yes** — a design is not expression |
| **Technique** | `SDOT`, `DONT_CARE`, transient attachments | **yes** — an idea is not copyrightable |
| **Asset** | one pinned driver, one shader cache, one test corpus | n/a |
| **Convergence** | three forks chose Oboe independently | n/a — nothing to unify, only to ratify |
| **Gap** | frame pacing, storage view: nobody has one | n/a — build once, shared by construction |

**Two consequences.**

**Technique and design cross licence boundaries that code cannot.** rpcsx is
GPL-2.0-only and out of the binary, and its ABI measurement, its
`GpuDriverAdvisor` heuristic and its overlay page framework are all still
available to this project. **The licence wall is lower than it looks, provided
you take the lesson and not the file.**

**Convergence is evidence, not work.** When three forks independently choose the
same library, that is the strongest signal available that it is the right one —
stronger than any single fork's reasoning. **Ratify it and move on.**

---

## 5. Rank unification by how well it resists being forgotten

`CLAUDE.md` already argues that a rule in a document does not stop an agent that
never read it, and that the fix must be structural. **Extend that to every form
of unification.**

| Form | What happens when someone forgets | Cost to create | Licence cost |
| --- | --- | --- | --- |
| **Build guard** | **the build fails** | high — needs the extraction first | inherits source |
| **Test** | **CI fails** | **low** | **none** |
| **Contract type** | **it does not compile** | **low** | **none** |
| Shared implementation | nothing to forget | high | inherits source |
| Inventory row | it is searched for again | low | none |
| Document | **nothing. It is simply not read** | very low | none |

**The cheapest form with high resistance is a test, and this project has almost
none.**

That inverts the current plan. Extraction is expensive, licence-encumbered,
blocked on the toolchain, and cannot start for weeks. **Encoding the fleet's
lessons as tests is cheap, licence-free, needs no toolchain, and can start
immediately.**

**This was demonstrated, not argued.** ARMSX2's three per-game-override bugs
were written as eight JUnit tests. The naive implementation — the one this
repo's own Track A specified — **fails four of them.** A lesson that took ARMSX2
three bug reports to learn now cannot be un-learned by anyone working in this
repo, and it cost an afternoon and no licence at all.

### The concrete proposal

**Build a lesson suite before building the shared layer.**

For each row in section 3, write the test that fails on the naive
implementation. They need no emulator, no device and no toolchain migration —
most are pure logic or a build-file assertion.

**Where a lesson cannot be tested, make it a contract type.** A setting without
a scope should not compile. A backend that declares no decline reason should not
compile.

**Where it can be neither, put it in the inventory, not in prose.**

---

## 6. The bigger idea: the operation is DELETE, not MERGE

Section 2 says unify what is forced. **That rule has a generator, and naming it
turns a list of candidates into one idea.**

**Every structure this project keeps finding "duplicated" is a portability
layer, and this project has already refused to pay for portability everywhere
else.**

| Structure, present N times | Exists to serve | What we actually have |
| --- | --- | --- |
| **Seven Vulkan device layers** | many GPUs, vendors, drivers, API versions | **one Adreno 740, one pinned Turnip** |
| **xenia's HIR; dynarmic's IR** | many host CPUs | **one ARM64** |
| **`glslang` and the GLSL text path**, three forks | many shader targets | **SPIR-V, always** |
| Present-mode selection and capability branching | unknown display stacks | **two measured panels** |
| Runtime feature detection | unknown capability sets | **`/proc/cpuinfo`, measured and fixed** |
| Multi-ABI builds, five forks | many devices | **`arm64-v8a`** |
| Driver pickers, four concerns | user-installed drivers | **one bundled driver** |

**They look duplicated because they solve the same problem — portability — not
because anyone copied anyone.** That is why every "these look the same"
candidate shrank when read: **seven answers to one question are not one answer
written seven times.**

### The smallest clean example: audio backends

The Vulkan device layer makes the argument at a scale that is hard to hold.
**Audio makes it in one screen.**

| Fork | Audio backends | Behind |
| --- | --- | --- |
| **eden** | **cubeb, null, oboe, sdl2** | `sink.h` |
| **ARMSX2** | **Cubeb, Oboe, SDL** | `AudioStream.h` |

**Four implementations and a dispatch layer, to answer one question: which
platform is this.**

**The Thor is one platform.** The DELETE result is **one sink and no dispatch
layer at all** — smaller than any single fork's audio code, not the union of
seven.

**Nothing here is duplicated between the forks.** eden's `oboe_sink.cpp` and
ARMSX2's `OboeAudioStream.cpp` are two files doing the same job, and merging
them saves almost nothing. **Deleting the other six backends and the two
dispatch layers saves the actual weight**, and it is the operation nobody had
named.

### So merging them is the wrong operation

`THOR_RENDER.md` already reached this for renderers: extracting from seven
portable layers "yields the union of seven sets of compromises". **That argument
generalises to every row above.**

**The operation is not merge. It is delete.** Remove the machinery that exists
to serve variability this device does not have, and what remains is smaller than
any one fork's version — not the sum of seven.

**This also explains the size results.** xenia's device layer is the one worth
taking not because it is best, but because **it is the only one already
separated from the thing it was abstracting for.** Once you are deleting rather
than merging, "which fork drew the boundary already" is exactly the right
question.

### The CPU half has literature behind it, and it is recent

**arXiv:2501.03427, *Boosting Cross-Architectural Emulation Performance by
Foregoing the Intermediate Representation Model*** (Amy I. Parker, January 2025)
argues precisely this for binary translation: QEMU's TCG spends performance on
an IR that exists for retargetability, and a **direct guest-to-host translator
for a fixed architecture pair** removes that step. Its proof of concept reports
**up to 35x faster than QEMU with TCG**, and it proposes a middle tier of direct
translators for "commonly paired architectures".

**The stated tradeoff is portability, which is the one thing this project has
already given up on purpose.**

Measured in the fleet on 2026-08-23:

| Fork | Guest to host | IR? |
| --- | --- | --- |
| **xenia** | PowerPC to ARM64 | **yes — `src/xenia/cpu/hir/`, 36 files** |
| **eden** | ARM to ARM64 | **yes — vendored dynarmic's `ir/`** |
| ARMSX2 | MIPS to ARM64 | no dedicated IR tree |
| Cemu | PPC to ARM64 | minimal |
| melonDS | ARM to ARM64 | none |

**Two forks pay the IR tax and three do not.** And xenia's HIR exists because
xenia targets x86_64 as well — **a portability requirement this project does not
have.**

**State the honesty plainly.** 35x is one proof of concept on one pair, not this
workload, and an IR buys real things: optimisation passes, register allocation
and a place to put correctness. **Nothing here says delete xenia's HIR.** It says
the IR is on the same list as the seven device layers — a portability structure
whose justification does not apply here — and that **nobody in this repo had put
it on that list.**

### What to do with this

1. **Add "what variability does this serve?" to the extraction procedure.** If
   the answer is variability the Thor does not have, the operation is deletion
   and the size estimate should be **smaller** than one fork's implementation,
   not larger.
2. **Treat the IR as a measurable question, not a taboo.** The cheapest version
   is not deleting anything: take one hot guest sequence in one fork and compare
   the HIR path against a hand-written direct lowering. **That is a bounded
   experiment with a published prior.**
3. **Do not confuse this with "rewrite the emulators".** Deleting a portability
   layer is not rewriting a guest model. The guest side — ISA semantics, timing,
   the GPU model — is untouched and stays per-backend forever.

### One thing checked and left alone

`CLAUDE.md` says direct GMEM access extensions arrive at the Adreno 840 and the
740 does not have them. **Checked against Qualcomm's current documentation and
it is correct**: `VK_QCOM_tile_memory_heap` and `VK_QCOM_tile_shading` are
documented for **Adreno 840 and higher**. **Do not design around them**, exactly
as already written.

---

## 7. A third operation: ISOLATE

Sections 2 and 6 give two operations — **unify** the forced singletons, **delete**
the portability layers. **There is a third, it is forced by the linker, and
nothing in this repo had recorded it.**

**Seven emulators in one binary share one global namespace.**

Measured 2026-08-23, counting headers that declare nothing inside a `namespace`:

| Fork | Headers at global scope |
| --- | --- |
| **Cemu** | **342 of 519 — 66%** |
| **ARMSX2** | **231 of 394 — 59%** |
| **Vita3K** | **174 of 331 — 53%** |
| melonDS | 139 of 297 — 47% |
| eden | 90% of `.cpp` files **are** namespaced |
| xenia | 88% **are** namespaced |

**A false alarm, recorded because it was mine.** The first pass looked for
collisions on obvious names — `class Memory`, `class GPU`, `class Renderer` —
and found several. **They are not collisions.** azahar's `GPU` is in
`namespace Frontend`, eden's in `Core::Frontend`, melonDS's in
`namespace melonDS`. **The famous names are the ones people remembered to
namespace.**

**The risk is the long tail**: helper structs, enums, constants and free
functions sitting at global scope in half to two thirds of the headers of four
forks. **Those are the ones that collide**, and they collide at link time, in a
build that takes 15 minutes, after the toolchain migration is already done.

**The proxy was checked rather than trusted.** Counting headers with no
`namespace` only matters if those headers actually declare something. Sampling
120 of ARMSX2's: **65 of the 68 un-namespaced ones declare a type or a free
function at global scope, and only 3 are includes and macros alone.**

**So the percentage is not inflated by empty headers.** Roughly 96% of what it
counts is real.

### Why this is a distinct operation

| Operation | Applies to | What you do |
| --- | --- | --- |
| **UNIFY** | forced singletons: device, driver, budget, scheduler | one owner replaces N |
| **DELETE** | portability layers serving variability we lack | remove the machinery |
| **ISOLATE** | everything else that must merely **coexist** | make collision impossible |

**Isolation is not unification and must not be confused with it.** Two forks'
`struct Vertex` do not want to become one `struct Vertex`. **They want to stop
being able to see each other.**

### There are two ISOLATE failures, not one

The percentages above measure **symbol** collisions at link time. **Include
collisions are a separate failure with a separate fix, and they are worse
because they are silent.**

**Measured 2026-08-23: 3,898 distinct header basenames across seven forks, and
241 appear in two or more.** `util.h` is in five. `types.h`, `config.h`,
`hash.h`, `atomic.h`, `input.h`, `file.h`, `ring_buffer.h` and
`shared_memory.h` are in four each.

**These are the names a codebase gives its own foundations**, and every emulator
wrote its own.

**With per-target include directories nothing collides**, which is normal
practice. **But a shared-layer header that writes `#include "types.h"` is
ambiguous by construction** — which one it gets depends on the include order of
whichever target compiles it, so the same shared header can compile against
seven different `types.h` **and never error.** It may simply pick up the wrong
`Config`.

**That is the worst class of build problem: not a failure, a divergence.**

| | Symptom | Fix |
| --- | --- | --- |
| Symbol collision | duplicate definition at **link** | namespaces, `-fvisibility=hidden` |
| **Include collision** | **the wrong header compiles, silently** | **a unique include prefix** |

**The rule, and it must land before the first extraction:** every shared-layer
header is included by a prefixed path — `#include "thor/device.h"` — and uses
prefixed includes only. **A naming decision is cheap before there are callers
and expensive afterwards.**

See [`../research_log/20260823_0230_header_collisions.md`](../research_log/20260823_0230_header_collisions.md).

### It is cheap, and it belongs in the standard row

Two mechanisms, both mechanical:

1. **`-fvisibility=hidden` by default**, exporting only what the backend
   contract needs. **This is one compiler flag and it removes most of the
   surface**, because a symbol that is not exported cannot collide across
   translation units at link time.
2. **A per-backend namespace**, `thor::ps2`, `thor::nds`, and so on, wrapping
   whatever a fork left at global scope.

**Add `-fvisibility=hidden` to the toolchain row.** It is the cheapest item on
the whole packed-binary path and it was not there.

### And it is testable before any extraction

**A build guard can require that a converted fork declares nothing at global
scope**, exactly the mechanism `OWNED.md` describes for deleted subsystems.
Unlike most guards, this one needs no extraction first — **it can be written
against a fork today** and it fails loudly.

**This is the same finding as everything else in this document**: the expensive
thing is not shared code, it is a structural property nobody stated, and the
durable fix is a guard rather than a paragraph.

## 8. The fourth operation: PROPAGATE, and its precondition

Three operations so far — **unify** the forced, **delete** the portability
layers, **isolate** what must merely coexist. **The fourth is the one this
project was actually founded to do**, and it is the only one that moves
something *between* forks rather than out of them.

**PROPAGATE: move a lesson or a technique from the fork that learned it to the
forks that have not.**

Section 3 lists eight of them, unmoved. It is the operation with the highest
value and the lowest licence cost — **an idea is not expression, so it crosses
the GPL-2.0-only wall that code cannot.**

### The literature says this is hard, and names why

Two recent papers describe exactly this problem.

**BackportBench** (arXiv:2512.01396) benchmarks LLM agents at **backporting
patches into divergent codebases** — which is this project's core task with the
names changed. It identifies three failure modes:

- **version divergence**, where the codebases have moved apart enough that
  direct application is impossible
- **dependency complexity**, where versions have incompatible APIs
- **context sensitivity**, where a patch is scattered across files

**All three describe this fleet exactly.** melonDS carries Dolphin's 2015
emitter and has drifted 40 methods; azahar and eden share a settings design with
**no surviving common lines**; Vita3K carries Dolphin's 2013 overlay in Java
while Dolphin moved to Kotlin.

**Environment-in-the-Loop** (arXiv:2602.09944) argues that migration with LLM
agents changes character when the **build and test environment is in the loop**
rather than the source alone: the agent receives concrete compiler and test
errors instead of pattern-matching, and migration becomes interactive rather
than one-shot.

**Honest limit: neither headline number could be extracted from the PDFs, and
none is quoted here.** The claims used are qualitative and are used only as
framing.

### The precondition this fleet does not meet

**If executable feedback is what makes agent propagation work, then propagation
in this fleet is currently blocked**, and today's build work measured how badly:

| Fork | Can an agent get feedback? |
| --- | --- |
| melonDS-android | **yes** — builds clean in 15 min 27 s |
| **Vita3K** | **no — it does not build at all** |
| ARMSX2, Cemu, xenia, azahar, eden, GameThor | **unknown, never attempted** |

**And almost no fork has tests that a propagation could be checked against.**

**That reframes Phase 0.3.** Building every fork looked like housekeeping —
something to finish before the interesting work. **It is the enabling condition
for the entire agentic thesis.** An agent cannot propagate Cemu's `DONT_CARE`
depth default into eden if it cannot build eden, and cannot know it worked if
eden has no test.

**It also reframes today's build findings.** The ABI waste, the AGP conflict and
the wrong recipe were not incidental tidiness. **They are the difference between
a fork an agent can work on and one it cannot.**

### What follows

1. **Finish Phase 0.3, and treat a fork that does not build as blocked for
   propagation**, not merely untidy.
2. **A propagation lands with a test, or it does not land.** The lesson suite is
   the mechanism: propagating Vita3K's overlay rule meant writing the test that
   fails without it. **That is the environment-in-the-loop idea at the smallest
   possible scale.**
3. **Prefer propagating into a fork that builds.** Order the work by whether
   feedback is available, not by how valuable the lesson is. **An unverifiable
   propagation is a guess wearing a commit message.**

---

## 9. The five operations, and how to choose

**Given a candidate, ask in this order.**

| # | Ask | If yes | Cost | Licence |
| --- | --- | --- | --- | --- |
| 1 | Can the binary, the device or the product tolerate **two** of these? | If **no** → **UNIFY** | high | inherits source |
| 2 | Does it exist to serve **variability the Thor does not have**? | If **yes** → **DELETE** | medium, and the result is **smaller** than one fork's version | none |
| 3 | Must these merely **coexist without colliding**? | If **yes** → **ISOLATE** | **low** — a flag and a namespace | none |
| 4 | Has one fork **learned something** the others have not? | If **yes** → **PROPAGATE** | **low** | **none — ideas cross every wall** |
| **5** | **Is its output a pure function of guest content and host configuration?** | If **yes** → **PERSIST** | **low** | **none** |
| — | None of the above | **Leave it alone.** Two implementations are not a problem | — | — |

**The last row is the one this repo kept skipping.** Nine candidates were opened
because code looked similar; the ones that shrank all belonged in that row.

**Row 5 was added 2026-08-23, and it is the first operation that does not shrink
on reading.** Every other row moves source. **This one moves computed results**,
so the question it asks is not "do these look the same" but "why is this being
computed again".

**It is already built twice in the fleet, and neither half knows the other
exists:** ARMSX2 persists translated VU programs, content-addressed and
relocatable, with tests asserting zero block compiles on a cache hit; **Cemu
ships a tool that merges another user's shader cache into yours.**

**It also answers section 10's falsification test in a way that section did not
anticipate.** The test asked for an extraction that is genuine duplication
without being forced. **PERSIST sidesteps it**: the waste is not N
implementations of one thing, it is **one computation repeated per launch and per
user**. Nine candidates were measured with the wrong instrument for this, because
the instrument counted implementations.

See [`../research_log/20260823_2205_translate_once_ship_it.md`](../research_log/20260823_2205_translate_once_ship_it.md).

## 10. What this does not say

**It does not say the shared layer is unnecessary.** The forced list is real and
the packed binary depends on all of it.

**It does not say the forks are well written.** They diverge, they carry dead
libretro glue, they build ABIs the device cannot run, and four of them never
received twelve years of upstream emitter work.

**It says the order is wrong.** The plan opens with the most expensive,
most-blocked, most licence-encumbered form of unification, and leaves the
cheapest and most durable form — tests and contract types — until after the
harness exists.

**And one measurement would falsify all of it:** an extraction that is not
forced by the linker, the device or the product, and that still turns out to be
genuine duplication after every implementation is read. **Nothing in nine
candidates has done that yet.**
