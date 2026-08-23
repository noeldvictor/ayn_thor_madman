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

## 6. What this does not say

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
