# Absence of output means check the gate, and an ABI poison test that needs no device

**Goal: close a gap I left in my own limits — the 11 `OPEN` ledger entries dated
2026-07-24, which I had not read.**

**Nine are the blocked Blue Dragon native-render lane. Two generalise, and both
are about instruments rather than levers.**

**Device-free: a ledger query. Everything below is xenia's.**

## 1. A census that produced zero, and the zero was a lie

The instrument was placed **inside** the block it was studying. It produced
**no output at all** — while the same run reported `rt_transfers=45`,
`rt_xfers_dropped=30`, `rt_xfers_executed=15`, so the run was healthy.

**The cause: the block was gated on `cvars::gpu_bd_native_depth_convert &&
depth_blit_supported_`, and the run did not enable that cvar.** The whole block,
including a pre-existing diagnostic, was dead code.

> **"ZERO OUTPUT WAS A SILENT NO-OP, NOT A ZERO RESULT — reading it as 'no MSAA
> depth transfers exist' would have wrongly killed the in-pass depth-resolve
> lever."**

**And it was the fourth of its kind in one session**, which is why the rule is
stated as a property of the codebase rather than as a mistake:

| Silent no-op | What it voided |
| --- | --- |
| an un-allowlisted cvar | the first CPU A/B |
| `cpu_backend_llvm` never passed | the residency levers |
| a frame-capped title-screen benchmark | its own result |
| **a census behind an unrelated gate** | **this census** |

> **"RULE: on this codebase, ABSENCE OF OUTPUT means CHECK THE GATE before it
> means anything about the game."**

### The fix is a placement rule, and this repo did not have it

**The census moved to `AddRenderTargetTransferStats`** — *"the one place EVERY
transfer passes through, **with no experimental cvar between it and the
data**"* — and **a comment was left at the old site explaining why it is not
there.**

> **Place an instrument where the data is unconditional, not inside the feature
> being studied.** A probe behind the flag under test can only report on runs
> where the flag is on, and reports nothing — indistinguishable from zero — on
> every other run.

**`CLAUDE.md` has "an instrument that can return zero must be proved able to
return non-zero".** That is the check. **This is the design that makes the check
unnecessary**, and the comment left behind is what stops the next person moving
it back.

### The corrected census carries its own validity check

The re-placed instrument is the adjacent `WIN`: **n=8192 depth transfers**, and
**86% at each of the 4096, 6144 and 8192 sample points** — `msaa_only` 66.7%,
`both` 20.0%, `pitch_only` 13.3%, **`neither` 0, `same_src` 0.**

> **Stability of the proportion across three sample sizes is evidence the census
> is sampling the population and not an artefact.** Reporting the intermediate
> points costs nothing and turns one number into a converging one.

## 2. An ABI mismatch, and why two passing tests missed it

**The claimed root cause was refuted.** A crashing LLVM-to-a64 guest call was
believed to need a per-call-site return trampoline. **The return path already
works** — the a64 callee recognises guest LR as its entry value and does a host
`ret`.

**The real cause is a register-contract violation.** A raw call into an a64 guest
entry breaks that entry's contract two ways:

- **the a64 backend expects `x19` to hold its backend context, and LLVM never
  reserves `x19`;**
- **a64 code clobbers `x22`-`x28` and the FULL `q8`-`q15`, while AAPCS
  guarantees only the LOW 64 BITS of `v8`-`v15` survive a call.**

**At `opt=2` LLVM allocates exactly those registers for values live across the
call**, so the callee destroys them.

### The testing lesson is the transferable half

> **"Explains why `opt=0` + the qemu differential passed TWICE and the device
> still crashed: they never expose that allocation."**

> **A correctness test at a lower optimisation level cannot exercise a
> register-allocation bug.** The tests were not weak; they were run in a regime
> where the contested registers are never allocated.

**This is the coverage-shape lesson again**, one day after azahar's 439,504
assertions missed a one-second crash. **Two forks, two subsystems: a large
passing suite that never reaches the state that matters.**

### And the device-free test that would have caught it

> **An AArch64 ABI POISON test.** AOT-compile an `opt=2` LLVM caller to a `.o`
> **via `TargetMachine`, NOT ORC — ORC segfaults under qemu-user** — link it
> against a **hand-written a64 "guest" callee that asserts `x19` and deliberately
> clobbers `x22`-`x28` and the full `q8`-`q15`**, and run it under
> `qemu-aarch64`.
>
> **"Use `_dump_asm` not `_dump_ir`: the bug is in REGISTER ALLOCATION."**

**Every part of that runs on a desktop.** It is a differential test of an ABI
contract rather than of results, and this project has nothing of the kind.

**The last line is the general rule**: when the defect is in register allocation,
**the IR is the wrong artefact to inspect.** Same family as this repo's
verify-from-the-emitted-artefact rule, applied to choosing which dump to read.

### The AAPCS detail is worth keeping on its own

> **AAPCS guarantees only the LOW 64 bits of `v8`-`v15` are preserved across a
> call.**

**Any hand-written ARM64 code called from compiled C++ that uses the full 128
bits of `v8`-`v15` must save and restore them itself.** **Four forks in this
fleet hand-write ARM64 emitters** — ARMSX2, xenia, Cemu and melonDS — and each
one's generated code is entered from compiled code.

**Not swept here**, and it is not a claim that any fork has the bug. **It is a
contract that is easy to half-know**, and knowing it as "v8-v15 are callee-saved"
rather than "the low 64 bits of v8-v15 are callee-saved" is exactly the shape of
error that produces a delayed, data-dependent corruption.

## 3. And a dead end recorded as a dead end

> **"do NOT build a blockaddress trampoline — that is the THIRD repeat of the
> same dead end."**

**Three times.** The build order that replaces it is specified in four numbered
steps, including one subtlety worth taking anywhere a cache is published:
**one atomic pointer to an immutable record, never separate atomic target and
pointer fields, because separate fields can be observed as target A with pointer
B.**

## Limits

- **Everything here is xenia's**, on its titles and its device. Nothing
  reproduced, no device used.
- **The ABI root cause is a consult's conclusion recorded in the ledger**, and
  the entry is `OPEN` — the fix is specified, not built or measured.
- **The poison test is described, not written.** Whether it reproduces the fault
  is untested.
- **The AAPCS `v8`-`v15` point is not swept across the fleet.** No fork was
  checked, and no claim is made about any.
- **Nine of the eleven entries in this batch were not analysed** beyond their
  titles; they are the blocked BD lane already characterised.

## Sources

- xenia `tools/exp_ledger.py check "depth_xfer_census_placement_silent_noop"`,
  `"llvm_guestcall_rootcause_abi_not_trampoline"`
- `research_log/20260825_0330_the_75_open_levers_decomposed_and_three_the_repo_lacks.md`
- `research_log/20260825_0015_439504_assertions_did_not_catch_a_one_second_crash.md`
