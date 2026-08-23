# Intermediate representations in emulators: what the literature actually says

**Goal: research the IR question — does an intermediate representation cost
speed, and does it buy unification — against arXiv and the wider field.**

No device. Reading only.

**Headline: the paper [`CLAUDE.md`](../CLAUDE.md) cites for this thesis is much
weaker than the citation implies, and there is far better evidence already
inside the fleet.**

---

## 1. The cited paper does not support the weight put on it

`CLAUDE.md` says:

> arXiv:2501.03427 argues QEMU's TCG pays for an IR that exists for
> retargetability, and that a **direct guest-to-host translator for a fixed
> pair** removes it — up to **35x** in a proof of concept.

**The paper is real and the direction is right. The evidence is thin.**

**"Boosting Cross-Architectural Emulation Performance by Foregoing the
Intermediate Representation Model"**, Amy I. Parker, arXiv:2501.03427,
6 January 2025. Abstract verbatim: *"this emulator performed up to 35x faster
than QEMU with TCG, indicating substantial room for improvement in QEMU's
design."*

**Four caveats, each material:**

- **The proof of concept is not a binary translator.** `riscv-um` is a Rust
  **simulator** — a register array with a memory interface. The paper's own
  proposal for direct binary translation is **future work**. So it measures a
  hand-written interpreter loop against QEMU, and proposes DBT separately.
- **The benchmark is `benchgen`: 2 million instructions of rotating `add`, `sub`
  and `sll`.** Three ALU opcodes in a loop. That is the best possible case for
  removing translation overhead and the worst possible proxy for an emulator
  whose cost is GPU, memory and system emulation.
- **It implements RV64I only**, and only the instructions the benchmark uses.
- **There is no memory protection.** *"Memory access violations are handled with
  a pass-through model."*

**And the arithmetic does not obviously reach 35x.** The one results table found
gives `riscv-um` at **77 ms** real against `qemu-riscv64` at **246 ms**, which is
**3.2x**. **The 35x headline could not be reconciled with that table**, and this
was read through a summarising fetcher rather than by rendering the PDF, so
treat the discrepancy as unresolved rather than as a finding.

**Action: downgrade the citation.** It is a directional argument, not a measured
result, and 35x should not be repeated in this repo.

---

## 2. The better evidence is a natural experiment, and it is already in the fleet

**Two production translators solve the identical problem — x86-64 guest on
ARM64 host — and made opposite choices about the IR.**

| | **Box64** | **FEX-Emu** |
| --- | --- | --- |
| IR | **none** — direct x86 to ARM64 | **a custom IR**, optimised, then JIT to ARM64 |
| Structure | four passes over the **guest instruction stream** | translate to IR, optimise IR, emit |
| In this fleet | **yes — GameThor ships it** | no |

**GameThor bundles Box64 0.3.2, 0.3.4 and 0.3.6** including bionic builds, with a
`Box64PresetsDialog` and a `box64_env_vars.json`. **The fleet already contains a
production no-IR direct translator and this repo had not recorded it.**

### Box64 refutes the standard argument for an IR

The usual defence of an IR is that it gives optimisation passes somewhere to
live. **Box64 does the analysis without one**, over the guest stream with side
metadata:

- **Pass 0** — analyse instructions; gather jump, flag and register-usage
  metadata
- **Passes 0.1 to 0.5** — jump destinations, **dead code elimination**, **flag
  propagation**
- **Pass 1** — floating-point register allocation onto NEON
- **Pass 2** — count native instructions, compute addresses
- **Pass 3** — emit ARM64

**It uses Kildall's algorithm** to propagate flag requirements *backward*, so it
only computes the flags a later instruction actually reads. **That is classic
dataflow analysis with no IR underneath it.**

**Conclusion: "you need an IR to have a place for optimisation" is false.** What
you need is a place to hang per-instruction metadata. For a fixed pair, the guest
stream plus annotations is enough.

---

## 3. The strongest argument *for* an IR is correctness, not portability

**Risotto: A Dynamic Binary Translator for Weak Memory Model Architectures**,
ASPLOS '23.

It **found real translation errors in QEMU** emulating x86 binaries on ARM
hosts, then **formalised QEMU's IR memory model** and proved mapping schemes
correct, using that to place fences minimally rather than conservatively.

**That work is only possible because there is one IR to formalise.** Without it,
the memory model would need re-verifying for every guest-host pair.

> **An IR's real product is a single place to state and verify semantics.**
> Retargetability is the advertised benefit; verifiability is the load-bearing
> one.

**Any decision to delete an IR must say where that verification goes instead.**

### And this fleet is not exposed to Risotto's problem at all — except one fork

Risotto's problem is **strong-on-weak**: an x86 guest (TSO) on an ARM host
(weak) needs fences to preserve orderings the guest assumed.

**No guest in this fleet has a stronger memory model than the ARM64 host:**

| Backend | Guest ISA | Ordering against ARM64 host |
| --- | --- | --- |
| ARMSX2 | MIPS (EE/IOP) | not stronger |
| xenia | PowerPC (Xenon) | not stronger |
| Cemu | PowerPC (Espresso) | not stronger |
| azahar, melonDS, Vita3K | ARM | same family |
| eden | **ARM64** | **identical** |
| **GameThor** | **x86-64, via Box64** | **STRONGER — the Risotto case** |

**So the hardest correctness problem in the DBT literature applies to exactly one
backend here**, and that backend is the one already deferred to a translation
layer somebody else maintains.

**Do not import fence-insertion machinery.** It is a portability layer for a
variability this fleet does not have — the same argument
[`UNIFICATION.md`](../shared_layer/UNIFICATION.md) makes about the seven Vulkan
device layers.

---

## 4. The static extreme, and what it really buys

**Static recompilation removes runtime translation entirely.** N64Recomp
converts N64 MIPS to portable C; **XenonRecomp does the same for Xbox 360
PowerPC and is built on xenia's codebase**, replacing the live JIT with
ahead-of-time recompilation.

**Both are already in this repo's reference tree**, per `CLAUDE.md`:
`xenia-thor-workspace/reference/` holds XenonRecomp and XenosRecomp.

**But the newest research tempers what "static" alone is worth.**

**"Deterministic Fully-Static Whole-Binary Translation without Heuristics"**,
Chen, McGowan and Franz, arXiv:2605.08419, May 2026. Their system **Elevator**
translates x86-64 to AArch64 **fully statically with no runtime component**,
composing "tiles" derived automatically from a high-level ISA description, and
handling the fundamental ambiguity head-on: *"any byte may be interpreted as
data, an opcode, or an opcode argument; we generate separate control flow paths
for all interpretations, pruning only those leading to abnormal termination."*

**Its result is the useful part: "on par with or better than QEMU's user-mode
JIT" on SPECint 2006**, at the cost of **substantial code size expansion.**

**Parity, not a multiple.** So the large wins reported for N64Recomp-style ports
do **not** come from being static as such. **They come from handing whole
programs to a real optimising compiler**, which is possible only because those
projects have per-game decompilation work and symbols behind them.

**That is a per-game cost this project cannot pay across eight systems.**

---

## 5. The middle ground, and the one technique that might transfer

**"Partial Cross-Compilation and Mixed Execution for Accelerating Dynamic Binary
Translation"**, Gu, Zheng, Xiao, Lu and Zhang, arXiv:2512.00487, 29 November
2025. Built on **LLVM and QEMU**, it cross-compiles what it can and emulates the
rest, with **selective function offloading** and calling channels between the
two environments. **Reports up to 13x over existing DBT.**

**It needs compilable functions**, so it targets applications and libraries
rather than opaque game binaries. **The transferable idea is the boundary
mechanism**, not the compilation: a working calling channel between natively
executed and emulated code.

**This fleet already has that shape in one place.** GameThor runs native ARM64
Wine and Windows x86-64 game code side by side, with Box64 bridging them.

---

## 6. What this means for this project

**Five conclusions, ordered by confidence.**

1. **Withdraw the 35x number.** The direction survives; the figure does not.
2. **Read Box64 before deciding anything about IRs.** It is production evidence
   for the no-IR position, on a harder pair than any in this fleet, and it is
   already on disk in GameThor.
3. **An IR must be replaced, not just deleted.** Risotto shows what it carries:
   a single place to state and verify semantics. Box64 shows the *optimisation*
   half can move onto the guest stream. **The verification half has no obvious
   home once the IR is gone.**
4. **Do not import strong-on-weak fence machinery.** Only GameThor is exposed,
   and Box64 already owns that problem.
5. **Static recompilation's win is compiler optimisation of whole programs, not
   the absence of an IR.** Elevator reaching only JIT parity is the evidence.
   **Without per-game decompilation, static buys much less than the N64 ports
   suggest.**

**The unresolved question, and it is the right one to ask next:** xenia carries a
full HIR and eden's dynarmic carries an IR, while ARMSX2, Cemu and melonDS do
not. **Nobody has measured what those two IRs cost on this device.** That is a
device experiment with a clean A/B — and unlike most, it has a plausible
mechanism behind it.

---

## 7. Following the one actionable thread, and correcting myself on it

**Section 6 ended by naming the `vmaddfp` fallback as the cheap next step.
Reading it showed that was wrong, within the hour.**

I wrote that fixing it needed no device. **xenia's own flag text says the
opposite**, and it is unusually precise about why:

> the LLVM lowering is **qemu-byte-correct in isolation**, but on-device it
> **MISCOMPILES** a function that uses `vmaddfp` **together with other vector
> ops** ... **at opt=0 AND opt=2**. It is a device codegen/regalloc
> **INTERACTION** bug (**the IR is correct**), not fixable from the IR

**It is a live miscompile in LLVM's AArch64 backend, not a coverage tidy-up.**
The a64 fallback is correct behaviour and was device-proven with
`cpu_backend_llvm_skip_opcodes=77`.

### But the device-free path is real, and xenia already built the tools

`cpu_backend_llvm_dump_ir` dumps the IR "**device-free-ishly**".
`cpu_backend_llvm_dump_asm` dumps the post-codegen AArch64 assembly, and its own
text names the reason: "**Unlike `_dump_ir` (which shows correct-looking IR),
this shows the IR->asm output where device codegen/regalloc bugs live.**"

**The IR is target-independent**, so the failing function's IR compiles to
AArch64 anywhere. **That converts a device asm-debugging job into a desktop
one** — the same move that settled the target-features question, which was
answered by disassembling rather than by running.

### The prerequisite nobody had checked

| Need | State on this box, 2026-08-23 |
| --- | --- |
| Exact LLVM | **20.1.8**, read from both shipped `libLLVM.so` |
| x86-64 `llc`/`opt` at 20.1.8 | **absent** — no `llc`, `opt` or `clang` on PATH |
| NDK clang as substitute | **21.0.0** on NDK 29 **and** 30 — **wrong major version** |

**Two corrections fall out of this.**

**The docs disagree about the LLVM version and the binaries settle it.**
`docs/research/20260626-llvm-jit-backend-build-plan.md` says LLVM **18.1.8**;
the shipped libraries are **20.1.8**. **The build plan is superseded.** A
codegen-bug report against the wrong version is worthless.

**And the obvious shortcut is a trap.** Reproducing a 20.1.8 codegen bug with
NDK clang **21.0.0** would be measuring a different compiler. **A negative from
that would be a wrong-instrument result**, which is the failure mode this repo
has recorded five times today.

**So the blocker is host LLVM 20.1.8 tools, not device time.** That is a
cheap thing to obtain and nobody had identified it as the gate.

## Sources

- [arXiv:2501.03427](https://arxiv.org/abs/2501.03427) — Parker, *Boosting
  Cross-Architectural Emulation Performance by Foregoing the Intermediate
  Representation Model*, Jan 2025
- [arXiv:2512.00487](https://arxiv.org/abs/2512.00487) — Gu et al., *Partial
  Cross-Compilation and Mixed Execution for Accelerating Dynamic Binary
  Translation*, Nov 2025
- [arXiv:2605.08419](https://arxiv.org/abs/2605.08419) — Chen, McGowan, Franz,
  *Deterministic Fully-Static Whole-Binary Translation without Heuristics*,
  May 2026
- [Risotto (ASPLOS '23)](https://dl.acm.org/doi/10.1145/3567955.3567962) —
  *A Dynamic Binary Translator for Weak Memory Model Architectures*
- [Box64 dynarec design](https://box86.org/2024/07/revisiting-the-dynarec/) —
  primary source for the four-pass, no-IR structure
- [N64Recomp](https://github.com/N64Recomp/N64Recomp) — static recompilation of
  N64 binaries to C

## Limits of this survey

- **No paper was rendered from its PDF.** Everything is read through fetched
  text, so a number quoted here is only as good as that extraction. **The 35x
  discrepancy in section 1 is explicitly unresolved.**
- **Nothing here is measured on the Thor.** No claim in this log is ours.
- **FEX-Emu's IR was not read**, only its published description. A direct Box64
  against FEX comparison would be the strongest evidence available and has not
  been done here.
