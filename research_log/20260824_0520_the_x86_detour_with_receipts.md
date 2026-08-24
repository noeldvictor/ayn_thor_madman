# The x86 detour, with two concrete instances — and "fewer instructions" measured slower

**Goal: keep mining xenia's research corpus, starting with the ARM64 hardware
map.**

**`20260806-arm64-hardware-exploitation-map.md` and the experiment ledger
together give this project's central CPU argument its first concrete instances,
and simultaneously contradict the metric that argument uses.**

## The ledger is bigger than this repo records

`python tools/exp_ledger.py list`, run 2026-08-24:

| Verdict | Count |
| --- | --- |
| **OPEN** | **75** |
| WIN | 57 |
| DEAD | 32 |
| FLAT | 8 |
| CONFOUNDED | 5 |
| **total** | **177** |

**`CLAUDE.md` records 39 WIN and 33 DEAD.** The wins have grown to 57, and
**this repo has never mentioned the 75 OPEN entries**, which are analysed levers
awaiting a run. **That is a backlog to mine, not a gap.**

## The port model, which turns a manual lead into a target

**The A715 and A710 have three 128-bit load ports and two arithmetic ports.**

`CLAUDE.md` already records "prefer a load over arithmetic on the mid cores" as
an unmeasured manual-derived lead. **This gives it a number and an outside
result:**

> Target **~0.67 arithmetic instructions per load**. Whatcookie reached that
> ratio in RPCS3's comparison loop for **+38% mid-core, +21% big-core**.

**Those are RPCS3's numbers on a different codebase**, and are not this
project's. **What transfers is the model: arithmetic-port pressure and dependency
depth are the two axes.**

## "Fewer instructions" measured slower, on this codebase

**This is the part that argues with `shared_layer/TRANSLATION.md`.**

> **Instruction count is not the objective.** We proved this on our own code:
> packing two u32s with `ORR` and storing via one `STP` cut a prolog **from 18 to
> 13 instructions and measured slower**, because it serialised two loads through
> an arithmetic op into one gated store. **Fewer instructions, deeper dependency
> chain, more pressure on the scarce port.**

**`TRANSLATION.md`'s spine is that speed is instruction inflation**, resting on
the ACM TACO framework where inflation predicts slowdown by regression across
real DBTs.

**Both are true, at different scales, and the document must say so:**

- **Across systems, inflation is a good aggregate predictor.** That is what the
  regression measured.
- **Within one system, instruction count is a bad objective for a specific local
  change.** A change that removes two instructions and adds a serialising
  dependency on the scarce port is slower.

> **Use inflation to choose which subsystem to attack. Do not use it to judge a
> peephole.**

## Instance 1: `rlwinm` lowers to three instructions because x86 has no rotate-and-mask

**This is the x86 detour, named in `CLAUDE.md`, with a specific opcode.**

From the ledger, verdict `OPEN`:

> The HIR models PPC rotate-and-mask as **rotate + AND because x86 has NO
> rotate-and-mask instruction**. **ARM64's `UBFM` IS rotate-and-mask**, and
> `rlwinm` is one of the most common PPC instructions.

The general non-wrapping case currently lowers to **`ROR` + `AND` + `UXTW` = 3
instructions**. For a non-wrapping mask the selected bits are a contiguous run of
the rotated source, which is exactly `UBFM`, and the shift and mask are
compile-time constants. **Three instructions to one, removing two
arithmetic-port operations per `rlwinm`.**

**This is exactly the shape `CLAUDE.md` predicts**: an IR decision that was
correct for x86-64 costing real instructions on ARM64. **Until now that section
had a register-model argument and no opcode-level example. Now it has one.**

## Instance 2: every condition-register update emits three compares instead of one

Also `OPEN`, and larger in reach:

> `PPCHIRBuilder::UpdateCR` models a CR update as **three INDEPENDENT HIR
> compares of the SAME operand pair** (`CompareSLT`, `CompareSGT`, `CompareEQ`).
> Each lowers to `cmp`+`cset`, so one CR update emits **three CMPs where ARM64
> needs one.**

**One ARM64 `CMP` sets N, Z, C and V, and `CSET` reads the flags without
disturbing them**, so the correct lowering is `cmp` + three `cset` + three
stores, not three of each. **That is two redundant compares per condition-register
update.**

**And CR updates fire on every `cmpw`, every `cmpwi`, and every `Rc=1`
instruction — a large fraction of compiled PowerPC code.**

**Common-subexpression elimination cannot fix it**, because the three operations
have different opcodes despite sharing operands.

> **The ACM TACO work found compare-and-branch worth roughly 18% of inflation.
> This is that finding's mechanism, inside a real emulator, unfixed.**

## The `EOR3` result, and why my earlier record of it was thin

**This repo recorded `EOR3`/`BCAX` fusion as `DEAD` and left it there. The reason
is the valuable part and it was not recorded.**

The hypothesis was right on both axes: `EOR3` does `a^b^c` in **one**
arithmetic-port operation at dependency depth 1, instead of two operations at
depth 2.

**It died because the pattern does not exist.** A compile-time counter over a
70-second run reported:

> **"EOR3 applicability: 0 of 1 V128 XORs are fusable chains"** — one V128 XOR in
> the whole compiled set, and it was not the outer half of a chain.

**Three lessons travel with it:**

1. **Measure applicability before building the transform.** One cvar and one
   device run replaced building a new three-input opcode, a HIR pass before
   register allocation, and x64 and LLVM fallbacks — which would have folded
   nothing.
2. **The scope caveat is stated in the entry itself**: a headless launch, so no
   AOT precompile and the LLVM backend off, counting only what the JIT compiled
   for one attract scene. **The counter stays in, default-off, so re-checking
   another title is one flag.**
3. **A structural fact that generalises: this kind of fusion cannot be a sequence
   peephole.** By the time the outer `XOR_V128` is emitted, the inner XOR is
   already emitted and register-allocated. **It has to be a HIR opcode.** The same
   note is attached to the `rlwinm` entry.

## What to change here

- **`TRANSLATION.md`** gains the scale distinction above, and the two concrete
  x86-detour instances.
- **`CLAUDE.md`'s "prefer a load over arithmetic" lead** gains the port counts and
  the target ratio, and stops being purely manual-derived.
- **`CLAUDE.md`'s ledger figures are stale** — 57 WIN, not 39 — and the **75 OPEN
  entries should be named as a resource.**
- **Add the applicability rule to the measurement discipline**: before building a
  transform, count how often its pattern occurs.

## Limits

- **The +38% and +21% are RPCS3's numbers on RPCS3's loop.** They are quoted as
  the origin of the model, not as this project's result.
- **Both instances are `OPEN`, not measured.** `rlwinm` and `UpdateCR` are
  analysed and unbuilt.
- **The prolog result is one measurement on one function**, quoted from the map.
- **The `EOR3` counter ran on one title and one scene**, and the entry says so.
- **This is one fork's ledger.** No other fork in the fleet has one.

## Sources

- xenia `docs/research/20260806-arm64-hardware-exploitation-map.md`
- xenia `tools/exp_ledger.py`, entries for `EOR3/BCAX fusion`,
  `rlwinm -> single UBFM on ARM64`, `UpdateCR emits 3 redundant CMPs`
- xenia `docs/research/20260806-x64-shaped-code-to-rethink-for-arm64.md`
