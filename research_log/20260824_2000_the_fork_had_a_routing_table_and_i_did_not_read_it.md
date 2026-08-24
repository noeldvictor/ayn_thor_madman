# The fork had a routing table saying "do not re-derive this", and I re-derived it

**Fourth and largest correction of the day, found by continuing the audit.**

**xenia's own `CLAUDE.md` opens with a routing table**: *if you are about to do X,
read Y first*. **I mined more than twenty of its documents today and never opened
the index to them.**

Its first row:

> **touch GPU / EDRAM / render passes** -> read `EDRAM / GMEM / UMA: ANSWERED ON
> THE DEVICE` (2026-08-16) FIRST.
>
> **MEASURED: the GPU frame is ALU-BOUND, not bandwidth-bound.** Blend is free,
> 2x bytes/pixel is +2.5%, the EDRAM-span RT costs ~1.5%, and **forced GMEM never
> beats autotune** (it converges to parity from 16 draws up). **⇒ The EDRAM
> redesign, screen-sized allocation and every tile-memory lever are all dead -
> they target 1.5% or a resource we are not short of. Do not re-derive them.**

## What this kills, of mine

**Measured on the Thor, Turnip on Adreno 740, at a representative shape —
1280x2048 with depth and up to 256 overlapping draws:**

| Hypothesis | Result |
| --- | --- |
| framebuffer bandwidth is the constraint | **NO.** Blend free; 2x bytes/pixel **+8-10%**; **flat across a 36x working set**, 0.9 MB to 66 MB, no cache cliff |
| the EDRAM-span attachment shape is the cost | **NO.** +1.5% |
| **GMEM/tiling can win** | **NO. +157% worse at 1 draw, exactly neutral at 16/64/256. Best case is parity** |

> **Any design whose payoff is "fewer framebuffer bytes" is dead on arrival here.
> That kills the obvious "map EDRAM onto GMEM" reading, and it kills
> transient/`LAZILY_ALLOCATED` attachments and subpass merging AS PERFORMANCE
> PLAYS** — ARM measures those at 45% fewer reads and 56% fewer writes, **which is
> a large saving of a resource we have spare.**

**So `research_log/20260824_0940_one_heap_and_nobody_uses_tile_memory.md` is
wrong where it matters.** I found that no fork binds `LAZILY_ALLOCATED` memory,
called it a two-half optimisation with one half missing, and **queued two device
experiments on it — `DEVICE_QUEUE.md` entries 21 and 22.**

**The observation stands: no fork binds it.** **The inference does not: on this
device it would buy nothing**, because the resource it saves is not scarce.

**And the pass-merging performance argument goes with it.** I recorded xenia's
2-subpass feedback merge and ARMSX2's in-tile depth read as tiler wins. **They are
correctness and structure wins; as bandwidth plays they target a resource that is
spare.**

## What is actually expensive, measured over 159 gameplay frames

| Counter | Per frame |
| --- | --- |
| `rt_transfers` — EDRAM ownership transfers | **45** |
| `pass_break_rt_change` | **27** |
| `pass_break_barrier` | 18 |
| **`xfer_same_fmt`** — transfer with **no** format change | **24** |
| total passes | ~74-77 |

> **A third of that title's render passes exist only to service EDRAM ownership
> transfers, and 24 of the 45 transfers do not change format at all** — they are
> moves, not reinterpretations.

**And that document retracts its own first explanation, in place, the same day.**
It proposed that the transfers came from a conservative extent estimate, then read
the code and found **ownership is already scissor-narrowed** — `height_used =
min(GetRenderTargetHeight(...), EstimateMaxY(...))`. **Two different numbers had
been conflated: the allocation and the ownership extent.** Its conclusion:
**"the fix is not 'estimate better', it is 'make the transfer not cost a pass
break'."**

## A second thing the same table kills, and it is architectural

> **expect a big win from register residency** -> **AAPCS64 preserves only the LOW
> 64 BITS of `v8`-`v15`, so a 128-bit guest vector CANNOT stay resident across a
> call — vector residency is architecturally impossible, GPRs cap at 8.**

**That is a hard limit on the residency thesis**, and it is stronger than the
`CONFOUNDED` device result I recorded an hour ago. **`shared_layer/TRANSLATION.md`
needs it**: the mechanism is real, the on-device test is confounded, **and the
ceiling is 8 GPRs with vectors excluded across calls by the ABI.**

## And a measurement invalidation worth more than any lever

> **`entry_delta` counts a64-compiled functions ONLY — 14.1M vs 130.6M on one
> flag. Every CPU A/B ever run in the shipping LLVM config scored ~11% of the
> guest.**

**An instrument that saw an eighth of the workload, used across a whole fork's CPU
experiments.** This repo's rule *"a negative result needs a workload that could
have produced a positive one"* has a sibling: **an instrument must see the
workload it is judging.**

## What I did wrong, and the fix

**I read the fork's documents and not its index.** The routing table exists
precisely to stop what I did, and it says **"Do not re-derive them"** in the row I
walked into.

**The rule:** **read a fork's `CLAUDE.md` or `AGENTS.md` routing table before
mining its `docs/`.** This repo's own federation section already says fork
documents are the source of truth for their fork — **it does not say to start with
the index, and it should.**

**Four superseded conclusions today, all from the same cause**: a dated document
read without asking what came after it. **Three tools now answer that** —
`exp_ledger.py check`, `fleet_docs_index.py --after`, and reading the routing
table first.

## Sources

- xenia `CLAUDE.md`, the routing table and `EDRAM / GMEM / UMA: ANSWERED ON THE
  DEVICE` (2026-08-16)
- xenia `docs/research/20260816-edram-uma-host-redesign.md`
- xenia `tools/edram_bench/heavy_pass.sh`
