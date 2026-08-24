# The AAPCS vector contract, swept: four forks, four different right answers

**Goal: close a gap I recorded in my own limits two hours ago. I wrote that the
AAPCS `v8`-`v15` subtlety "is not swept across the fleet" and made no claim.
Sweep it.**

**Device-free: source reading in four forks. No device used.**

## The contract

**AAPCS guarantees only the LOW 64 BITS of `v8`-`v15` survive a call.** The upper
halves are caller-saved. Knowing the rule as *"`v8`-`v15` are callee-saved"* is
the half-known version, and it is the shape of error that produces a delayed,
data-dependent corruption rather than a crash.

**Four forks hand-write ARM64 emitters, and each one's generated code is entered
from compiled C++.**

## The result: all four are correct, and none for the same reason

| Fork | What it saves across the host-to-guest boundary | Why |
| --- | --- | --- |
| **xenia** | **full `q8`-`q15`** | *"Save callee-saved NEON regs: **full q8-q15 (JIT uses all 128 bits)**"*, beside *"v8-v15 lower 64 bits are callee-saved, but upper 64 bits are not"* |
| **ARMSX2** | **all live `q8`-`q15`** | *"q8-q15 lower 64 bits are callee-saved, **but the JIT uses full 128-bit, so save all live ones**"* |
| **Cemu** | **only `v8.d[0]` .. `v15.d[0]`** | exactly the ABI requirement. Its frame is `STACK_SIZE = 160 /* x19 .. x30 + v8.d[0] .. v15.d[0] */` |
| **melonDS** | **nothing — and the question does not arise** | its A64 JIT emits **no vector code at all** |

**Two of the four save MORE than AAPCS demands, deliberately, because their JITs
use the upper halves.** Cemu saves exactly what AAPCS demands.

**Saving less than the low 64 bits would be the bug. None of the four save sites
READ HERE does that** — xenia's and ARMSX2's commented thunks, Cemu's 160-byte
frame, and melonDS's vector-free JIT. **That is four sites, not four forks
audited**; each fork has more than one boundary and only the commented ones were
opened.

## The melonDS zero is explained, not merely reported

**My first search returned zero for melonDS and I nearly recorded it as one.**
Two checks stopped that:

- **A positive control on the path**: `ARMJIT_A64/` exists and holds **6 tracked
  files**, so the path was right.
- **The mechanism, not the name.** melonDS spells register saving as
  **`ABI_PushRegisters(BitSet32({30}) | CallerSavedPushRegs)`**, a Dolphin-derived
  abstraction. **A grep for literal `D8`/`Q8` cannot see it** — the
  search-for-a-name trap this repo has recorded three times.

**Then the real answer:** across all four `ARMJIT_A64/*.cpp`, references to any
`V`/`D`/`Q` register total **zero**, and **`ARM64FloatEmitter` is never used**.

> **melonDS's A64 JIT emits only integer code, so it cannot clobber `v8`-`v15`.
> The contract is unreachable there.** That is a different statement from
> "nobody got it wrong", and it is why the row says the question does not arise.

## Two things worth keeping

**xenia and ARMSX2 wrote nearly the same comment, independently.** *"lower 64
bits are callee-saved, but the JIT uses full 128-bit."* **Two forks, two
codebases, one AAPCS subtlety, both writing it down at the save site** — the same
convergence signal this project already trusts from three forks choosing Oboe and
two reaching the `SEVL`/`WFE` park.

> **Where a rule is easy to half-know, the forks that got it right wrote it in a
> comment at the point of use.** That is the cheapest durable form available
> without a test, and it is what made this sweep possible at all.

**And a question rather than a claim.** Cemu stores its eight D lanes with
**`st4((v8.d - v11.d)[0], ...)`** — an elegant one-instruction store of four
lanes. azahar measured **A510 Q-form 32-bit `ST4` throughput at `1/50`**.
**Cemu's is the `.d[0]` single-structure form, not Q-form 32-bit, so that figure
does not transfer** — and this is once per thunk crossing rather than in a loop.
**Recorded as adjacent, not as a defect.**

## Limits

- **This checks the host-to-guest boundary only.** Whether each JIT's internal
  register allocation respects its own conventions was not examined.
- **ARMSX2 has more than one save site.** `AsmHelpers.cpp:474` uses the
  `d8`/`d9` form while `RecStubs.cpp` saves full width. **Different call sites
  with different requirements is the expected shape, but only the commented one
  was read.**
- **Cemu's row is read from the frame comment and the `st4`/`ld4` pair.** The
  whole prologue was not traced.
- **Nothing was built or run here.** This is a source read, by `git grep` over each fork's own emitter directory.
- **No defect found, and none claimed.**

## Files

- `research_log/20260825_0540_absence_of_output_means_check_the_gate.md` — where
  the gap was recorded
