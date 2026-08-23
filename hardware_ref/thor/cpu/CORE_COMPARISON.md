# The four cores compared

Distilled from the four ARM optimization guides in
`armsx2-thor/ARMSX2/docs/reference/arm/`. Extracted 2026-08-22.

**The headline: one code layout cannot be optimal on all four cores. The
guides give directly conflicting advice.**

The Thor is 1 + 4 + 3. Everything below is from the manuals and **none of it is
measured on this device.**

## The cores

| | Cortex-X3 | Cortex-A715 | Cortex-A710 | Cortex-A510 |
| --- | --- | --- | --- | --- |
| Count | 1 | 2 | 2 | 3 |
| Execution | out of order | out of order | out of order | **in order** |
| Dispatch in | 8 MOPs/cycle | 5 MOPs/cycle | 5 MOPs/cycle | 3-entry issue queue |
| Dispatch out | 16 uOPs/cycle | 10 uOPs/cycle | 10 uOPs/cycle | 3 co-issued/cycle |
| L pipelines | 6 uOPs | 5 uOPs | 6 uOPs | 2 loads, 1 store |
| AES per cycle | 4 | — | 2 | fused pairs |
| Vector unit | private | private | private | **shared in a dual complex** |

The big cores agree on their V-pipeline caps: 2 uOPs each on V0 and V1, 4 on
S/B, 4 on M, 2 on M0.

## Where the guides conflict

### Branch layout: A715 wants concentration, A510 wants sparsity

| Core | Advice |
| --- | --- |
| Cortex-X3 | Avoid more than **4 branches** per aligned **32-byte** region |
| Cortex-A710 | Avoid more than **4 branches** per aligned **32-byte** region |
| Cortex-A715 | **Prefer branches concentrated.** "It is preferable to have an aligned 32-byte instruction region containing two branches, to having two 32-byte regions containing one branch each." |
| Cortex-A510 | Avoid more than **one conditional branch** per aligned **16-byte** region |

**A715 asks for the opposite of what A510 asks for.** A715's predictor is
optimised for 32-byte regions containing no branches at all, so it wants
branches packed together and the rest left clear. A510 wants at most one
conditional branch in every 16 bytes, which is four instructions.

A recompiled block cannot satisfy both.

A715 adds two more specifics:

- Prefer **taken branches near the end** of an aligned 32-byte region, and
  branch targets near the **beginning** of one.
- **Never place a branch as the last instruction of a 4 MB aligned region.** A
  predictor limitation. Relevant to a code cache, which is exactly the kind of
  large allocation that lands on such a boundary.

### Alignment: the A510 thresholds are twice as strict

| Penalty | X3, A715, A710 | A510 |
| --- | --- | --- |
| Load crossing | 64-byte line | **32-byte** |
| Store crossing | 32-byte | **16-byte** |
| Quad load not 4-byte aligned | penalty | penalty |

Code aligned for the big cores still pays on the A510.

## The A510 findings that matter most

### The vector unit is shared between two cores

> Cortex-A510 shares a VPU between all Cortex-A510 cores in a complex.
> Instructions being executed on VPU pipelines by one core may reduce
> performance of the instructions executed on the VPU by the other core.

Dual-core complexes **share the L2 cache and the VPU**. Single-core complexes
have their own.

**Two vector-heavy threads placed on a paired A510 contend for one vector
unit.** Nothing in the thread's own behaviour reveals this; it looks like
random slowdown.

The Thor has three A510s, so at least one pair shares. **Which cores pair, and
whether the VPU is 2x64-bit or 2x128-bit, is readable from the device**
through `IMP_CPUCFR_EL1.Cores` and `IMP_CPUCFR_EL1.VPU`. **Read it before
placing any vector work on the little cores.**

A 2x64-bit datapath would mean 128-bit vector operations take two passes there.
That is a large difference and it is configurable per SoC.

### In-order issue rules

The issue queue holds three instructions and supports at most four GPR
destinations and six GPR sources.

**An instruction occupies two entries** if it has three or more GPR
destinations, or three or more GPR sources.

Up to three entries co-issue per cycle: at most 3 ALU, 2 load, 1 store, 2 VPU.

**Multicycle entries disable co-issue for every cycle but the last:**

- atomics with acquire or release semantics
- loads of more than 256 bits
- stores of more than 128 bits
- **stores with release semantics**

That last one matters. Emulators use release stores for guest memory ordering,
and on the A510 each one blocks co-issue.

### Fusion differs, and the useful pair is missing

| Core | Fusible pairs |
| --- | --- |
| X3 | AES pairs, `CMP`/`CMN`/`TST`/`BICS` + `B.cond`, `NOP` + anything |
| A510 | AES pairs, `AUT*` + `BR`, `AUT*` + `LDR` |

**Compare-and-branch fusion does not exist on the A510.** The X3 codegen rule
of emitting compare adjacent to branch buys nothing there. It costs nothing
either, so keep it, but do not expect it to help on the little cores.

### Memory tagging blocks write-streaming

> Enabling precise tag checking can prevent the Cortex-A510 core from entering
> write-streaming mode. This can reduce performance and increase power when
> writes miss in the L1 or L2 caches.

## The A710 dispatch stall

A710 has a trap the other guides do not list.

> In the event of a V-pipeline uOP containing more than 1 quad-word register
> source, a portion or all of which was previously written as one or multiple
> single words, that uOP will stall in dispatch for three cycles.

Only the first such use stalls; later consumers of the same register do not.

**This is exactly what emulated guest vector units do.** A guest that writes
vector lanes individually and then uses the whole register produces this
pattern constantly. PS2 VU, Xbox 360 VMX128 and DS geometry all work this way.

**Three cycles, on the first use, on two of the Thor's cores.**

## What the big cores agree on

- **Spill GPRs to the vector register file rather than to memory.** X3, A715
  and A710 all state it. The A510 guide does not.
- FPCR writes are self-synchronising on all three big cores.
- Region-based fast forwarding exists on all three: crossing a vector
  forwarding region costs one cycle.
- Special-purpose registers are mostly not renamed. `NZCV` and `SP` are.
- `DC ZVA` beats `STP` for zeroing, on every core including the A510.

## What this means for the project

**Tune the recompiler for the X3 and place hot code there.** The alternative is
per-cluster codegen variants, which multiplies the code cache and the testing.

Four consequences:

1. **State the cluster in every performance claim.** Already a rule; this is
   the evidence for it.
2. **Guest vector work on the little cores is a trap.** Shared VPU, possibly a
   2x64-bit datapath, and no compare-and-branch fusion.
3. **The A710 dispatch stall is worth a real experiment**, because guest vector
   registers assembled lane by lane are the normal case in emulation.
4. **Thread placement is a design problem, not a flag.** xenia found guest
   threads pinned to the A510s while the X3 idled. rpcsx keeps the full core
   mask so Java, audio and compiler threads are not dragged onto emulation
   cores. Both findings are consistent with these guides.

## Still unread

- Instruction latency and throughput tables, section 3 of each guide. They are
  large and only matter once a specific sequence is being tuned.
- The A715 and A710 special-register tables.
- `arm-architecture-reference-manual-a-profile.pdf` is not tracked, by design.

## Sources

`cortex-x3`, `cortex-a715`, `cortex-a710` and `cortex-a510`
`-software-optimization-guide.pdf`, section 4 of each, in
`armsx2-thor/ARMSX2/docs/reference/arm/`. See also
[`CORTEX_X3_NOTES.md`](CORTEX_X3_NOTES.md) for the prime core in detail.
