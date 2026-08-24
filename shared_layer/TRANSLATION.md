# The translation north star: residency, not portability

**Where speed actually comes from in a guest-to-host recompiler on this device,
and what one fixed host buys that a portable translator cannot take.**

**Written 2026-08-23**, from a literature survey plus a fleet-wide measurement.
Companion to [`THOR_TARGET.md`](../hardware_ref/thor/THOR_TARGET.md), which sets
the compile target, and [`PATTERNS.md`](PATTERNS.md), which catalogues the
pipelines.

---

> ## SCALE CAVEAT, added 2026-08-24
>
> **This document's spine is that speed is instruction inflation. That holds
> across systems and fails within one.**
>
> xenia measured it on its own code: packing two u32s with `ORR` and storing
> through one `STP` cut a prolog **from 18 instructions to 13 and measured
> slower**, because it serialised two loads through an arithmetic operation into
> one gated store. **The A715 and A710 have three 128-bit load ports and two
> arithmetic ports, so arithmetic is the scarce resource and dependency depth is
> the second axis.**
>
> **Use inflation to choose which subsystem to attack. Do not use it to judge a
> peephole.** See
> [`../research_log/20260824_0520_the_x86_detour_with_receipts.md`](../research_log/20260824_0520_the_x86_detour_with_receipts.md).

## The rule, in one line

> **Translate the console's CPU and GPU straight to ARM64. Never inherit the
> x86 detour.**

**Every recompiler here is a desktop emulator with an ARM64 backend added** —
xenia wrote `x64` before `a64`, Cemu wrote `BackendX64` before
`BackendAArch64`, ARMSX2's `common/emitter/` is the x86 emitter. **So the real
path is console CPU -> an x86-shaped waypoint -> ARM64.**

**That waypoint has a price, and the measurement below is it.** An
SSA-over-a-context IR is nearly free on x86-64, where **memory operands fold
into the consuming instruction** and you only have **16** GPRs so guest state
belongs in memory anyway. **On ARM64 every one of those accesses is its own
instruction, and there are 31 GPRs to hold state in.** Same for flags: implicit
on x86, **opt-in on ARM64**, so eager flag computation is pure waste.

## The chain, in four links

**Each link is measured, and each cites its source.**

### 1. Speed is inflation

**Instruction inflation** — host instructions emitted per guest instruction —
**predicts slowdown by linear regression.** State-of-the-art dynamic binary
translators sit at **1.46 or worse**. Attacking inflation on QEMU measured
**2.99x to 7.12x** on SPEC CPU 2017, with inflation cut **83.59% on CINT2017**
and **94.56% on CFP2017**.

*ACM TACO, March 2024, "An Instruction Inflation Analyzing Framework for Dynamic
Binary Translators".*

**This is the largest, best-evidenced speed lever in the emulation literature,
and this project had not been aiming at it.**

### 2. Inflation is not caused by having an IR

**LATX is built on QEMU 6 and keeps TCG's IR**, yet the same study ranks it a
**minor** slowdown while QEMU is **substantial**. **Box64 (no IR) and FEX (an
IR) land in the same band.**

> **QEMU is slow because it optimises little, not because it has an IR.**

**So "delete the IR" is the wrong instruction.** The 35x figure this project once
cited for that has been withdrawn — it was a three-opcode loop against a
simulator with no memory protection.

### 3. Inflation is caused by the register mapping

The register-mapping literature is blunt about the mechanism:

> a simple approach is to **emulate guest registers in memory and generate
> load/store instructions to access them on the host**. However, this naive
> translation **generates excessive load and store operations, and thus
> drastically degrades the performance**.

**And "improper mapping can incur excessive memory operations."** The problem is
worst **when the guest register file is larger than the host's**, which is
exactly the Xbox 360 case: **128 VMX registers onto 32 NEON.**

### 4. The fleet's own numbers say the same thing

**Measured 2026-08-23, statically, no device.** IR operations per guest
instruction:

| Frontend | Guest to host | Median | Register model |
| --- | --- | --- | --- |
| **Cemu IML** | PowerPC to ARM64 | **2.0** | **virtual registers** |
| **dynarmic A64** | **ARM64 to ARM64** | **4.0** | SSA over a context |
| **xenia HIR** | PowerPC to ARM64 | **5.0** | SSA over a context |
| **dynarmic A32** | ARM32 to ARM64 | **5.0** | SSA over a context |

**dynarmic expands 4x translating ARM64 to ARM64** — the easiest translation
problem that exists — **while Cemu does PowerPC to ARM64 in 2.**

> **Expansion tracks the register model, not the distance between guest and
> host. The correlation is perfect across four frontends.**

**And xenia confirmed the consequence on hardware**, in its own flag text:

> **Device-confirmed the IR has ~99 ctx memory ops + 1 alloca = NO register
> residency** (the guest thread is memory-bound, **~half of BD's field CPU**)

**Four independent lines — a peer-reviewed study, a register-mapping literature,
a static count of four frontends, and one device measurement — converge on the
same quantity.**

---

## CONFIRMED BY DISASSEMBLY, 2026-06-26, and the mechanism has a name

**xenia proved this with no device.** `scratch/thor-debug/residency_killtest.c`:
the same guest hot loop compiled two ways, aarch64 `-O2`, disassembled.

| Form | Guest-register memory ops per iteration |
| --- | --- |
| guest regs as context-struct fields | **~4** |
| **guest regs localized to C locals** | **0** |

**The loop contains an opaque `mem_read32(ctx, ...)` call**, which defeats alias
analysis and forces the struct form to reload and spill around it. **The
localized form keeps the registers in `x24`, `x21`, `x23` across the loop and
across the call.**

### The lever is LOCALIZATION, not static recompilation

**Two static recompilers differ on exactly this.** **DolRecomp** emits guest
registers as `ctx->gpr[%u]` struct fields and **gets no residency** — the host
compiler round-trips them just as a JIT does. **XenonRecomp localizes them to C
locals and does.**

> **A recompiler being static does not give residency. Making guest registers
> host-compiler locals does.**

### The in-JIT retrofit is built, host-correct, and crashes

`arm64_register_cache_inherit` carries **320 assertions** and **crashes on
device** — `SIGBUS`, fault address `0x1fffffff8`, a **stale inherited register on
a back edge**. It has consumed **12 or more device fires**, and xenia calls it the
highest-risk unit in its design.

**The crash and the win are the same code shape.** Its hottest function is four
sorted linked-list traversals; each loop is **three basic blocks** because of two
conditional exits, so the node pointer and the search key round-trip **every
block, every iteration**. **That is the tax, concretely** — and stale inheritance
of that node pointer is the fault.

### Three consequences for this document

- **Prefer a targeted hybrid AOT over whole-program.** `CLAUDE.md` objects that
  whole-program static recompilation needs per-game decompilation and symbols,
  which this project cannot pay across eight systems. xenia's recommendation is
  **AOT the hot cluster, JIT everything else, share the dispatcher** — which the
  objection does not reach.
- **Lazy flags compound with residency**, and xenia calls them likely the single
  biggest sub-lever. **This document did not mention them**, and the ACM TACO
  inflation work independently found compare-and-branch worth roughly 18% of
  inflation.
- **There is a device-free kill test with a pass bar stated in advance:**
  **>20-25% fewer host instructions than the JIT is a go; <15% is dead.**

**Four hard parts are enumerated there and are the real cost**: bit-exact CR, XER
and FPSCR; indirect control flow through `bctr`, `bcctr`, `blr` and jump tables;
the guest memory model; and self-modifying code, where the suggested answer is to
detect it and fall back to the JIT for those pages.

**DolRecomp's PC-switch entry table** — one function per guest function with
`switch (ctx->pc)` and a label per address — **is the fix for the indirect-entry
case the in-JIT lever mishandles.**

See [`../research_log/20260824_0445_localization_is_the_residency_win.md`](../research_log/20260824_0445_localization_is_the_residency_win.md).

## The distinction that matters: residency is not allocation

**Do not conflate these. They are worth different amounts.**

| | What it is | What it is worth |
| --- | --- | --- |
| **Residency** | do guest registers live in host registers **at all**, or in memory | **the large lever** — xenia says memory-bound, ~half of a title's CPU |
| **Allocation quality** | given residency, which guest register gets which host register | **single-digit percent** |

**LCCRA (EuroSys 2026), in CrossDBT — an LLVM-based translator, x86-64 to
AArch64 — measures allocation quality at 5.76% to 7.79% end-to-end**, while
cutting **register-allocation time by 69.55% to 74.98%.**

**Read that split carefully.** Better allocation buys a few percent at run time
and a lot at **compile** time. **Residency is the order-of-magnitude item**, and
it is the one xenia does not have.

**So the priority is settled: get guest registers into host registers first.
Tune which ones later, if ever.**

---

## What one fixed host buys, and why this is the project's strongest claim

**This is the part no portable translator can take.**

A portable DBT must choose a register mapping that works on **any** host. It
cannot assume a register count, an ABI, or which registers are reserved. **So it
keeps guest state in memory and lets a general register allocator recover what it
can, per function, at run time.**

**This project has exactly one host, and it is fully known:**

| | Thor, ARM64 |
| --- | --- |
| General-purpose | **31** (`x0`-`x30`) |
| Vector | **32** (`v0`-`v31`), **128-bit, no SVE** |
| ABI, reserved registers | fixed and known |
| Prime core | one Cortex-X3 |

**And every guest register file is fixed and known too:**

| Guest | GPR | FP / vector | Larger than host? |
| --- | --- | --- | --- |
| **Xenon** (360) | 32 | 32 FPR + **128 VMX** | **yes, badly** |
| Espresso (Wii U) | 32 | 32 FPR, paired singles | no |
| EE (PS2) | 32 x 128-bit | VU0/VU1 files | **partly** |
| ARM64 (Switch) | 31 | 32 | **no — identical** |
| ARM32 (3DS, Vita) | 16 | 32 | no |

> **Both sides of the mapping are constants. Therefore the mapping is a constant
> too, and it can be decided once, by hand, per backend — never re-derived at run
> time.**

**That is [`UNIFICATION.md`](UNIFICATION.md)'s DELETE operation applied to the
hottest path in the emulator**, and it is the first time that thesis has measured
evidence behind it. The machinery being deleted is **general register allocation
serving unknown hosts** — variability this device does not have.

**The ARM64-guest case is the proof.** eden's guest register file **is** the host
register file, 31 GPRs and 32 vector registers on both sides. **A fixed mapping
is nearly the identity function.** dynarmic still expands 4x, because it was
written to be portable.

---

## Is Rosetta 2 the guiding star? Half of it.

**Yes for this pipeline. No for the product** — and taking it as a whole-product
star would aim at the wrong bottleneck.

**What transfers:** Rosetta **AOT-translates the entire text segment up front**
and JITs rarely; it **elides flags** overwritten before use; it targets **one
host instruction per guest instruction** and reaches **~1.64x** size expansion on
one binary. **All three are available to us.**

**The flag elision has now been reached three times independently** — Rosetta's
unused-flags pass, **Box64's Kildall backward propagation**, and **LATX's
compare-and-conditional-jump fusion.** The inflation study puts `jcc` at **9.72%**
and `cmp/test` at **8.66%**, so **~18% of inflation is compare-and-branch.**
**This repo treats three-way convergence as its strongest signal.**

**What does not transfer: hardware TSO.** Apple changed the **silicon** so x86
ordering is free on ARM. **We cannot — and we do not need to.** No guest here is
more strongly ordered than ARM64. **We skip Rosetta's hardest problem for free.**

**Why it is the wrong star for the product:** Rosetta has **no guest hardware to
emulate**, its guests do not self-modify or depend on cycle timing, and **the
fleet has already measured a title where the CPU is not the constraint** —
xenia's ledger: *"BD's gap is HLE-vs-LLE ... every incremental GPU lever is
DEAD/FLAT because it patches the emulator instead of replacing it."*

**The star that contains Rosetta is one line:**

> **Translate, do not emulate — per pipeline.**

| Pipeline | Emulate | **Translate** | Proof |
| --- | --- | --- | --- |
| **CPU** | JIT per run | **AOT once, cache forever** | **Rosetta 2** |
| **GPU** | model the guest GPU | **translate the guest API** | **RE2 Remake on this Thor, GameNative/DXVK** |

**`CLAUDE.md` already had the GPU half. This is the CPU half.**

### And install-time AOT could exceed Rosetta

**Rosetta AOTs at first launch. We could AOT at install and never again** — the
console binary never changes, the device never changes. **xenia already has both
halves**: an AOT precompiler at **~97% coverage** and `cpu_llvm_object_cache`,
**default off**.

**It also recovers the static-recompilation win without the per-game cost.**
N64Recomp-style gains come from whole-program compiler optimisation, which needs
per-game decompilation. **An installer has no translation-time budget**, so the
compiler can see the whole text segment at once. **That is the one place this
project could beat Rosetta rather than match it.**

**Unpriced:** translation time, cache size on a handheld, and invalidation on a
driver or emulator update.

### UPDATE 2026-08-23: ARMSX2 has already built the persisted half, with tests

**This section was speculation when written. One of its two halves is now
verified as built.**

`pcsx2/arm64/microVU_ProgCache-arm64.*` and `microVU_Persist-arm64.*`, **2,576
lines plus three test files**, persist translated PS2 vector-unit programs to
disk and hydrate them on the next launch. Its own disk test states the outcome:
the program **"runs with ZERO block compiles and a bit-identical post-state"**.

**It solves the problem this section left unpriced**, which is that emitted code
is position-dependent: a **constant-VA arena layout plus a placement-relative
fixup table** make the vixl output relocatable, patching absolute `armEmitCall`
and `armMoveAddressToReg` targets on load.

**Invalidation is answered too**, by a 64-byte options sentinel covering every
option that changes emitted code, and an ABI-version handshake that evicts a
stale cache directory at startup.

**Two things it does not answer.** It covers the **VU only**, not the EE or IOP.
And **it is default-off**, with the fork's own reason: *"opt-in until the on-disk
cache is validated on the target hardware"* — **the Thor**. See `DEVICE_QUEUE.md`
entry 17.

**The literature calls this persistent code caching** and it predates all of
this. **One reported figure tempers the claim**: the USENIX ATC 2016 framework is
summarised as 76.4% faster without helper threads and **9% with them**, so most
of the win is translation cost that **cannot be hidden on another core**. **The
primary source returns HTTP 403 from here and was not read**, so that split is
recorded, not adopted. See
[`../research_log/20260823_2205_translate_once_ship_it.md`](../research_log/20260823_2205_translate_once_ship_it.md).

### The unifying principle: express guest state as values the compiler can see

**Added 2026-08-24, and it merges this document's two biggest levers into one.**

- **Residency.** XenonRecomp makes guest registers **C locals**, so the host
  compiler keeps them in host registers. Measured: **0 guest-register memory ops
  per loop iteration against ~4** through a context struct.
- **Lazy flags.** rpcs3's `PPUTranslator` puts condition-register bits in **SSA
  locals** through `RegLoad`/`RegStore`, flushed only when observable. **LLVM's
  dead-code elimination then does the laziness** — a CR field computed and
  overwritten before any branch reads it **disappears entirely, including
  transitively across inlined blocks.**

> **These are the same technique. Express guest state as values the host compiler
> can see, rather than as memory it must assume aliases.** Do that and residency
> and laziness both fall out of passes that already exist.

**rpcsx's own words: "Rosetta has to implement that analysis; here it falls out
of emitting IR."**

### Which is an argument FOR an IR that this document did not have

This document and `CLAUDE.md` treat an IR mainly as **portability machinery**, a
DELETE candidate, with correctness verification as its one defence. **This is a
second defence and it is a performance one.**

**The honest counterweight, from the same fork:** rpcs3's JIT targets
`cortex-a78`, Armv8.2, which **omits `flagm`**. `RMIF`/`SETF8`/`SETF16` are the
natural way to move PPC condition bits into and out of NZCV, and **it is unclear
that this is reachable from LLVM IR at all**, because portable IR has no "set the
flags" operation.

> **The IR gives you the optimiser and takes away the instruction.**

**A third instance, 2026-08-24, and it is about control rather than an
instruction.** ARM's own guide recommends spilling GPRs to the vector file rather
than to memory. **xenia can do it — it hand-writes its emitter — and measured
`UMOV` latency 2 against `LDR` 4**, leaving it off because its guest already
squeezes 128 vector registers into 28. **rpcsx cannot do it at all**: its
recompiler emits IR, and *"spill this GPR to a spare V register" is an allocator
policy, not something expressible from the IR we hand over.*

**So the ledger now reads:**

| An IR gives you | An IR takes away |
| --- | --- |
| **residency**, free, from the allocator over SSA values | **`flagm`** — portable IR has no "set the flags" operation |
| **lazy flags**, free, from dead-code elimination | **spill placement** — the allocator owns it |
| **one place to verify** memory-model correctness | |

**Both columns are real and neither is decisive.** What decides is which costs
this fleet's guests actually pay, and the residency measurement — **0 memory ops
against ~4 per iteration** — is the largest number on either side.

See [`../research_log/20260824_0730_rpcsx_checked_rosetta_first.md`](../research_log/20260824_0730_rpcsx_checked_rosetta_first.md).

### The half of Rosetta that does NOT transfer: TSO

**Rosetta's other pillar is hardware TSO** — Apple put x86's strong memory
ordering into the CPU, because x86 is total-store-ordered and ARM64 is weakly
ordered, and emulating that in software is expensive.

**Measured 2026-08-23: this fleet does not have that problem** — and rpcsx
reached the same conclusion independently, with a **stronger reason** than "both
are weak": **since Armv8, AArch64 guarantees multi-copy atomicity and PowerPC
does not**, so *"the translator never has to manufacture ordering the target
lacks — the position Rosetta is permanently in."* **The translation direction is
favourable.** Its instruction to itself: **do not go looking for a TSO problem,
and do not add barriers defensively.**

Counting ordering mnemonics in each ARM64 emitter directory:

| Fork | `dmb` | `ldaxr` / `stlxr` | `ldar` / `stlr` |
| --- | --- | --- | --- |
| xenia | 4 | 15 / 15 | **0 / 0** |
| Cemu | 0 | 1 / 1 | **0 / 0** |
| ARMSX2 | 0 | 0 / 0 | **0 / 0** |

**No fork uses `LDAR`/`STLR`, and that is correct rather than an oversight.**
Those are the cheap way to force ordering on ordinary loads and stores, and
**none of these guests needs it**: PS2 is MIPS, Xbox 360 and Wii U and PS3 are
PowerPC, and Switch, Vita, 3DS and DS are ARM. **PowerPC and ARM are both weakly
ordered**, so the mapping is `sync` to `dmb ish` and `isync` to `isb`, which is
what xenia's four barriers are. xenia's `ldaxr`/`stlxr` pairs are load-linked and
store-conditional for guest atomics — the correct lowering of PowerPC
`lwarx`/`stwcx`.

**One backend does have the TSO problem: GameThor**, because its guest is x86
Windows through Box64 and FEX. **That cost is inside Box64 and FEX, not in this
project's code**, and GameThor is Tier 2.

**So this is a "leave it alone" result**, which `CLAUDE.md` names as the row this
repo kept skipping. **Do not go looking for a TSO win here.** Rosetta's AOT
pillar transfers; its memory-ordering pillar does not.

## What a unified "tight fast layer" actually is

**Not one translator.** The guest ISAs differ, and that difference **is** the
emulator. **What unifies is the substrate underneath**, and each item is forced:

| Shared | Forced by |
| --- | --- |
| one **code cache**, one eviction and flush protocol | one process, one memory budget |
| one **persistent AOT object cache**, keyed by content hash | one device, one fixed binary per game |
| **one host register policy** — who may use which register, which is the context pointer, what is callee-saved | **generated code calls shared helpers and must agree on the ABI** |
| one **spin and park primitive**, calibrated | `yield` is a measured no-op here |
| one **fastmem layer** — reservation, `MAP_FIXED`, the fault handler | Android address-space limits |
| one **flag-elision policy** | three-way convergence; ~18% of inflation |

**Not shareable:** the guest decoder, the instruction lowering, the guest memory
map, the timing model.

**The host register policy is the sharpest new UNIFY candidate.** **Two backends
in one binary cannot hold different opinions about which register is the context
pointer** when their generated code calls the same shared helpers. **That is the
ABI forcing unification** — the exact test [`UNIFICATION.md`](UNIFICATION.md)
sets.

See [`../research_log/20260823_1740_rosetta_as_a_star.md`](../research_log/20260823_1740_rosetta_as_a_star.md).

## CONFIRMED ON THE DEVICE: xenia already measured all of this

**Found 2026-08-23 by surveying xenia's `docs/research/`, which holds 553
documents.** Everything above was derived from literature and static counting.
**xenia had already measured it on hardware.**

**The problem** — `a64-context-traffic-audit`, hottest function `8272A3A4`:

```text
blocks=54 instrs=2467
context_loads=255 context_stores=442
ppc_loads gpr=255   ppc_stores gpr/cr=252/183
barriers ctx=85
```

**697 context memory operations in 2,467 IR instructions — 28% of the IR is
guest-state traffic**, and every load is a GPR. **That is the device-side twin
of the static 5.0-against-2.0 measurement above.**

**The experiment** — `a64-gpr-cache-barrier-negative`:

```text
loads/hits=546/0   stores/cached=562/463   invalid reg=768   barrier_preserves=213
```

**546 context loads, zero cache hits**, even preserving across all 213 barriers.

**And the conclusion is the strongest evidence this document has:**

> the current emit-time cache is **the wrong layer** ... **it has no durable host
> register**: the normal HIR register allocator reuses the same small
> **`x22..x28`** pool, and **`register_invalidations=768`** kills every candidate
> before the next `LOAD_CONTEXT`.

**`x22..x28` is seven registers. The Thor has thirty-one.**

**That gap is the entire argument of this document, in measured form.** The
allocator had seven, the guest has 32, the cache scored zero out of 546 — **and
it had seven only because the design must not assume the host.**

**xenia's own ranked next steps**, verbatim in order: a **compile-time HIR
promotion** removing redundant clean GPR `LOAD_CONTEXT` before register
allocation; a **pinned-register experiment** for one or two hot GPRs, "likely
`r[1]` and `r[11]`"; then a larger allocator change reserving **durable state
registers**. Its store histogram picks the candidates — **`r[11]` at 110 stores**,
`r[10]` at 64.

> **Do not spend more time on the current emit-time cache by merely preserving
> across more barriers; the Thor audit shows that is not where the hits are
> lost.**

See [`../research_log/20260823_1830_xenia_already_measured_residency.md`](../research_log/20260823_1830_xenia_already_measured_residency.md).

## What to do, in order

**1. Prove residency on the backend that already has the switch.**
`cpu_backend_llvm_context_residency` is **written and off**, "pending
qemu-differential + device A/B validation". **Nothing needs fixing. It needs
validating**, and its one run came out `CONFOUNDED` on the documented cross-run
scene trap. **Use the fixed-frame-range bench**, which is already designed.

**2. Measure stages B and C.** Stage A is done. `--disassemble_functions` with
`--disassemble_function_filter` gives raw HIR, optimised HIR and host ARM64 in
one capture. **See [`../DEVICE_QUEUE.md`](../DEVICE_QUEUE.md) entry 13.**

**3. Propagate the register model, not the code.** **Cemu's IML is the lesson**
— virtual registers, 2.0 expansion, same guest family, same host. **It is an
idea, so it crosses the licence boundary freely.**

**4. Improve dynarmic once, benefit three times.** **eden, azahar and Vita3K all
vendor it.** It is the only code-translation component already shared in this
fleet, and its A64 frontend has the clearest headroom — **4x expansion on an
identity translation.**

**5. Do not chase allocation quality.** Worth single digits. **Residency first.**

---

## What this does not claim

- **No frame number.** Nothing here is timed on the Thor. The 2.99x to 7.12x is
  QEMU's, on SPEC CPU 2017, and **QEMU is the worst performer in that study** —
  a fork already at "moderate" has less to recover.
- **Stage A is not inflation.** The optimiser and register allocator collapse
  much of it. **Final host inflation is unmeasured for every fork here.**
- **The literature is CISC-to-RISC.** This fleet is mostly **RISC-to-RISC**, the
  easier case, so **expect a smaller prize than the papers report.**
- **The static counts are unweighted**, count every branch in an emitter, and
  undercount helpers.
- **Rewriting a register model is the deepest possible reach into a core.**
  [`PATTERNS.md`](PATTERNS.md) says do not attempt code translation before
  pipelines 2 and 3 are proven. **That still holds.**

## Sources

- [ACM TACO 10.1145/3640813](https://dl.acm.org/doi/full/10.1145/3640813) —
  instruction inflation predicts slowdown; 2.99x-7.12x on QEMU
- [LCCRA, EuroSys 2026](https://doi.org/10.1145/3767295.3803591) — register
  allocation in LLVM-based binary translation; 5.76%-7.79% end-to-end
- [arXiv:2501.03427](https://arxiv.org/abs/2501.03427) — the IR-removal
  argument, **cited here only to record that its 35x is withdrawn**
- [Risotto, ASPLOS '23](https://dl.acm.org/doi/10.1145/3567955.3567962) — why an
  IR carries the memory-model verification
- [`../research_log/20260823_1712_ir_expansion_measured.md`](../research_log/20260823_1712_ir_expansion_measured.md)
  — the fleet measurement
- [`../research_log/20260823_1642_ir_in_emulators_literature.md`](../research_log/20260823_1642_ir_in_emulators_literature.md)
  — the survey
