# The hidden gold: xenia already measured register residency, and wrote the plan

**Goal: mine the forks' own research directories, which have been sampled but
never surveyed.**

No device. Reading only.

## The seam

| Fork | `docs/research` | skills | tools | ledgers |
| --- | --- | --- | --- | --- |
| **xenia** | **553** | **37** | **142** | 2 |
| **rpcsx** | 23 | **51** | 40 | 2 |
| Vita3K | 2 | 18 | 4 | — |
| azahar | 6 | — | — | — |
| eden, ARMSX2, Cemu | 1-2 each | — | — | — |

**Roughly 590 research documents and 106 skills across the fleet. This repo had
read about a dozen.**

**`CLAUDE.md` says xenia holds 29 skills. It holds 37.**

**Splitting xenia's 553 by subject:** about **110 are `a64`/`arm64`** — host-side
and transferable to every fork — and about **110 are two specific titles**, Blue
Dragon and Project Sylpheed, which are not.

## What the ARM64 lane already contains

Titles alone name the whole of today's thesis:

`a64-context-traffic-audit` · `a64-guest-state-cache-design` ·
`a64-gpr-cache-barrier-negative` · `a64-context-cache-cr-branch-negative` ·
`a64-context-cache-and-spinlock-fastpaths` · `a64-gprlr-helper-inline` ·
`a64-fpr-vmx-helper-inline` · nine documents on one `a64-fast-entry-*` design

**Everything derived from first principles today, xenia has already measured.**

## 1. The problem, measured on the device

`20260520-182253-a64-context-traffic-audit.md`, hottest function `8272A3A4`:

```text
blocks=54 instrs=2467
context_loads=255 context_stores=442
ppc_loads  lr/ctr/gpr/cr/... = 0/0/255/0/...
ppc_stores               = 6/0/252/183/1/...
barriers ctx/mem=85/0
store_top=0xA3C:61,0xA3D:61,0xA3E:61,0x078:31,0x070:27,...
```

**697 context memory operations in 2,467 IR instructions — 28% of the IR is
guest-state traffic.** Every load is a GPR. The three most-stored slots are
**CR6 bytes, 61 stores each.**

**This is the device-side confirmation of the static count made today** — xenia's
HIR expanding a median of 5.0 IR ops per guest instruction, against Cemu's 2.0.
**Two independent methods, same conclusion.**

**And it named the hot registers**: `r29, r31, r1, r30, r27, r23, r11, r10`.

## 2. The experiment was run, and it returned zero

`20260521-212700-a64-gpr-cache-barrier-negative.md`:

```text
fn 82282490 loads/hits=546/0 stores/cached=562/463
            invalid offset/reg=1/768
            resets safety/block/barrier=119/30/0
            barrier_preserves=213 fallthrough_preserves=0
```

**546 context loads. Zero cache hits.** Preserving the cache across **all 213**
barriers changed nothing.

**The conclusion is the finding, and it is verbatim:**

> This proves the current emit-time cache is **the wrong layer** for the real
> speed win. **It has no durable host register**: the normal HIR register
> allocator reuses the same small **`x22..x28`** pool, and
> **`register_invalidations=768`** kills every candidate before the next
> `LOAD_CONTEXT`.

## 3. Why this is the strongest evidence yet for the fixed-host argument

**`x22..x28` is seven registers.**

[`TRANSLATION.md`](../shared_layer/TRANSLATION.md) argues that a portable
translator cannot assume a register count, so it keeps guest state in memory and
lets a general allocator recover what it can. **This is that argument in
measured form**: the allocator had seven registers to work with, the guest has
32, and the cache achieved **zero hits out of 546.**

**The Thor has 31 general-purpose registers.** The gap between seven and
thirty-one is the whole opportunity, and it exists **only** because the design
must not assume the host.

## 4. And the correct next steps are already written, ranked by safety

xenia's own "Next Move", verbatim in order:

1. **a compile-time HIR promotion** that removes redundant clean GPR
   `LOAD_CONTEXT` instructions **before A64 register allocation**;
2. **a pinned-register experiment for one or two hot PPC GPRs**, likely `r[1]`
   and `r[11]`, with explicit helper/exit/branch flushes;
3. a larger register allocator change reserving **durable state registers**.

> **Do not spend more time on the current emit-time cache by merely preserving
> across more barriers; the Thor audit shows that is not where the hits are
> lost.**

**And the store histogram picks the candidates for step 2**: `r[11]` at **110**
stores, `r[10]` at 64, `r[9]` and `r[3]` at 44 each.

## What this changes

**Nothing in today's thesis is wrong. All of it is earlier than xenia's work
rather than later.**

| Today's conclusion | xenia's status |
| --- | --- |
| residency is the large lever | **measured**: 28% of IR is context traffic |
| the register model causes expansion | **measured**: 546 loads, 0 hits |
| a portable design cannot assume the host | **measured**: `x22..x28`, seven registers |
| the fix needs a durable host register | **written**, ranked, three options |

**So the honest position is that this repo re-derived, from literature and static
counting, what one fork had already established on hardware — and the value is
that the derivation now spans four forks instead of one.**

**The actionable change is to stop proposing and start reading.**
`cpu_backend_llvm_context_residency` is xenia's option 1 and 3 attempted in the
LLVM backend rather than the a64 one. **Both backends have been tried. Neither
has landed.**

## The rule this proves

**`CLAUDE.md` says to read the inventory before building anything. It should
also say: read the fork's own research directory.**

**553 documents is not a corpus anyone will read exhaustively**, but the titles
are a searchable index of every question already asked, and **the negatives are
the valuable half.** Two of the three documents read here are recorded negative
results — and both save an experiment.

## Limits

- **Three documents read of 553.** Everything above is from those three plus a
  title survey.
- **`82282490` and `8272A3A4` are two functions in one title.** The 28% figure is
  not a fleet constant.
- **rpcsx's 51 skills and 23 research documents are unread**, as are Vita3K's 18
  skills.
- **No claim that pinning registers would work.** xenia ranked it second of
  three and has not run it.
