# Should Rosetta 2 be the guiding star

**Goal: answer whether Rosetta 2 is the ideal to aim at, and what a unified
"tight fast layer" across the emulators would actually be.**

No device. Reading only.

## Answer

**Yes for pipeline 1. No for the product. And taking it as a whole-product star
would aim the project at the wrong bottleneck.**

## What makes Rosetta 2 fast, and what of it we can have

| Rosetta 2 does | Available to us? |
| --- | --- |
| **AOT-translates the entire text segment up front**, JIT only rarely | **YES.** A console binary is fixed. Translate at install. |
| **Unused-flags elision** — skip computing flags overwritten before use on every path | **YES**, and already proven twice |
| **One fixed guest-host pair**, tuned | **YES.** This is the project's whole premise |
| **~1.64x expansion**, aiming at one host instruction per guest instruction | **a target**, not a given |
| **Hardware TSO** — Apple changed the silicon so x86 ordering is free on ARM | **NO.** We cannot change the SoC |

**Two of those deserve more than a row.**

### The flag optimisation has been reached three times independently

Rosetta 2 elides flag computation when the flags are overwritten before use.
**Box64 does the same with Kildall's algorithm**, propagating flag requirements
backward. **LATX's named lever is compare-and-conditional-jump fusion**, which
attacks the same cost.

**Three systems, three teams, one target.** And the inflation study puts `jcc` at
**9.72%** and `cmp/test` at **8.66%** — **roughly 18% of inflation from
compare-and-branch alone.**

**When three independent implementations converge, this repo treats it as the
strongest available signal.** It did so for Oboe and for the touch overlay.

### We do not need hardware TSO, because we do not have Rosetta's hardest problem

Apple added TSO to the M1 because **x86 is more strongly ordered than ARM**, so
every guest load and store needed ordering guarantees the host does not give.
**That is the single hardest thing Rosetta had to solve, and it took silicon.**

**No guest in this fleet is more strongly ordered than ARM64.** PowerPC, MIPS and
ARM are all weak; eden's guest **is** ARM64. **The one exception is GameThor's
x86-64, and Box64 already owns it.**

> **We skip Rosetta's hardest problem for free, and we should say so rather than
> treating Rosetta as unreachable.**

## Why it is the wrong star for the product

**Three differences, and the third is decisive.**

**1. Rosetta has no guest hardware to emulate.** It translates user applications
running on the same OS, against a known ABI, calling the same system libraries.
**Its job is 100% CPU translation.** An emulator must also model a GPU, timers,
DMA, interrupts and a memory map. **CPU translation may be a minority of frame
time.**

**2. Its guests behave.** Compiled macOS applications do not self-modify, do not
play cache-flush games, and do not depend on cycle timing. **Console games do all
three.**

**3. The fleet has already measured a title where the CPU is not the
constraint.** xenia's experiment ledger carries a standing conclusion:

> BD's gap is **HLE-vs-LLE**, proven by RE2 Remake running on the same Thor via
> GameNative/DXVK. xenia **EMULATES** the 360 GPU (slow); the fix is
> **TRANSLATING** D3D9->Vulkan like DXVK. **Every incremental GPU lever is
> DEAD/FLAT because it patches the emulator instead of replacing it.**

**Making that title's CPU Rosetta-fast would not have moved it.**

## The better star, and it contains Rosetta

**The principle that covers both is not "be like Rosetta". It is:**

> **Translate, do not emulate — and apply it per pipeline.**

| Pipeline | Emulate (slow) | **Translate (fast)** | Existence proof |
| --- | --- | --- | --- |
| **CPU** | interpret, or JIT per run | **AOT-translate the binary once, cache forever** | **Rosetta 2** |
| **GPU** | model the guest GPU | **translate the guest graphics API** | **RE2 Remake on this Thor via GameNative/DXVK** |

**Both proofs already exist, and one of them is on this device.**

**Rosetta is the CPU half of a star this project already had and had not
named.** `CLAUDE.md` records the GPU half as xenia's conclusion; this adds the
CPU half and gives them one sentence.

## So what is the unified "tight fast layer"

**Not one translator. The translators cannot merge — the guest ISAs differ and
that difference is the emulator.**

**What unifies is the substrate underneath them**, and every item is forced by
the packed binary, the device, or the product:

| Shared | Why it is forced |
| --- | --- |
| **One code cache** with one eviction and flush protocol | one process, one memory budget |
| **One persistent AOT object cache**, keyed by **content hash** | one device; translate once per game, ever |
| **One host register policy** — which host registers backends may use, which is the context pointer, what is callee-saved | **generated code calls shared helpers; they must agree on the ABI** |
| **One spin and park primitive**, calibrated | `yield` is a measured no-op here; a per-fork constant does not transfer |
| **One fastmem layer** — reservation, `MAP_FIXED` sub-mappings, the fault handler | Android address-space limits are a device property |
| **One flag-elision policy** | three systems converged on it; ~18% of inflation |

**Per backend, and not shareable:** the guest decoder, the instruction lowering,
the guest memory map, the timing model.

**The host register policy is the new one**, and it is the sharpest UNIFY
candidate found today. **Two backends in one binary cannot hold different
opinions about which register is the context pointer** if their generated code
calls the same shared helpers. **That is the linker and the ABI forcing
unification**, which is exactly the test [`UNIFICATION.md`](../shared_layer/UNIFICATION.md)
sets.

## The AOT case is stronger here than for Rosetta

**Rosetta AOTs at first launch. We could AOT at install**, and never again.

- **A console binary never changes.** Content-hash it, translate it, keep it.
- **The device never changes.** One host, one register file, one driver — so a
  cached translation stays valid indefinitely.
- **xenia already has both halves**: an AOT precompiler with **~97% coverage**,
  and `cpu_llvm_object_cache`, **default off**.

**And it connects the static-recompilation thread.** N64Recomp-style wins come
from handing whole programs to an optimising compiler, which needs per-game
decompilation. **Install-time AOT gets the whole-program view without the
per-game work** — the compiler sees the entire text segment at once, with no
translation-time budget to respect.

**That is the one place this project could exceed Rosetta rather than match it**,
because Rosetta must translate quickly at first launch and an installer does not.

## What this does not claim

- **No number is ours.** ~1.64x is Rosetta's on one sqlite3 binary, and it is a
  **size** ratio, not an instruction count.
- **No claim that AOT is faster at run time.** AOT removes translation cost and
  enables more optimisation; **it does not by itself reduce inflation.**
- **No claim CPU is the bottleneck anywhere here.** For the one title measured,
  it explicitly was not.
- **Install-time AOT is unpriced.** Translation time, cache size on a handheld,
  and invalidation on a driver or emulator update are all unexamined.
