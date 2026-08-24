# The pipelines column, and why thirteen manual predictions failed and this one did not

**Goal: read `microarchitecture.md`'s section on HOW to read the optimisation
guides, since this repo quotes them constantly and got the core wrong once
already.**

**Device-free: one section. No device used.**

## The column that decides

> **"The third column is the one that is easy to skip and often decides the
> answer."**

```
V    FP/ASIMD 0/1/2/3   all four pipes
V01  FP/ASIMD 0/1       two
V13  FP/ASIMD 1/3       two
V0   FP/ASIMD 0         one
```

**Latency and throughput are the numbers everyone reads. The utilised-pipelines
column is the one that says whether a "faster" instruction can actually issue.**

## A manual-derived prediction that matched to 2%, on three cores

**`BCAX` replaces a dependent `BIC` then `EOR`. The table:**

| | latency | throughput |
| --- | --- | --- |
| `BIC` then `EOR`, dependent | 2 + 2 = **4** | 4/cycle, spread across `V` |
| **`BCAX`** | **2** | **1/cycle, `V0` ONLY** |

> **The guide predicts a 2.00x LATENCY WIN and a THROUGHPUT LOSS.**

**Against `bcax_bench`, run BEFORE any of this was read:**

| shape | predicted | X3 | A715 | A510 |
| --- | --- | --- | --- | --- |
| latency, serial chain | **2.00x** | **1.96x** | **2.01x** | **2.00x** |

**Within 2% on all three cores.**

## This refines one of this repo's most-cited rules

**`CLAUDE.md` carries a hard prior**, quoted verbatim from rpcsx:

> *"Do not pick a lever from a manual. **Ten manual-derived predictions were
> measured here and ten were refuted.** Profile first."*

**and this repo extended it to thirteen for thirteen.**

**Here is a manual-derived prediction that held to 2% on three different cores.**
So the prior needs a sharper statement:

> **The failures were not "manuals are wrong". They were readings that took
> LATENCY and THROUGHPUT and skipped WHICH PIPES.** This one used the third
> column, **predicted a win AND a loss on different axes**, and got both right.

**A two-sided prediction is much harder to hit by luck than a one-sided one.**
"This will be faster" has a 50% prior; "this will be 2x faster in latency and
slower in throughput, and here is which" does not.

**And it is consistent with the thirteen.** Every refuted lever this repo records
— `EOR3` fusion, `TBL2` for `TBX2`, the A510 shared VPU, the FP-status stall —
**was argued from an instruction's own cost, not from what it contends with.**

## It also explains a result recorded yesterday

**azahar measured `MLA` -> `MADD` regressing on both A715 patterns**, and I
recorded the mechanism from rpcsx's correction: **`MLA` is throughput 1 on the
single pipe `V0` on A715.**

> **Same column, second instance in two days.** The fused instruction is not
> slower in isolation; **it is confined to one pipe while the split pair spreads
> across `V`.**

**Two independent measurements, both explained by the column everybody skips.**

## Ledger checked before treating this as a lever

**`exp_ledger.py check "bcax"` and `shared_layer/REJECTED.md`:** xenia's
`EOR3`/`BCAX` fusion is **`DEAD`, 0 of 1 fusable candidates**, and REJECTED
records it. **Nothing here proposes re-running it.**

> **The finding is about HOW TO READ A GUIDE, not about `BCAX`.** The instruction
> is settled — it had nothing to fuse in xenia — **and the prediction that
> matched is evidence about the METHOD, on a bench rather than in an emulator.**

## What to do with it

- **A lever argued from a manual must quote all three columns**, and this repo's
  own table extracts — `CORTEX_X3_NOTES.md`, `CORE_COMPARISON.md` — should be
  checked for whether they carry the pipe column at all.
- **Prefer two-sided predictions.** A lever that predicts only an improvement is
  weak evidence even when it is right.
- **The prior stands for one-sided, single-instruction arguments.** It is
  weakened, not withdrawn: **one matched prediction against thirteen refutations
  is a refinement, not a reversal.**

## Limits

- **One matched prediction.** Thirteen refutations still outnumber it heavily,
  and **the refinement is a hypothesis about WHY they failed**, not a re-audit of
  them.
- **The bench is rpcsx's, on this SoC**, and this repo already records its
  numbers.
- **The X3 table is quoted here**, and per that document's own correction the
  A715 tables are tighter — **the BCAX prediction happened to hold on all three
  cores anyway.**
- **CHECKED THE SAME SESSION, and they do not.** `CORTEX_X3_NOTES.md` has
  **zero** tables with a pipes column; its only `V0`/`V1` mention is a
  **DISPATCH-CAP table** — *pipelines against max uOPs per cycle* — which is
  about pipelines but is **not the per-instruction assignment.**
  `CORE_COMPARISON.md` has one such row. **So this repo's extracted notes carry
  latency and throughput advice and no per-instruction pipe column at all**,
  which is the plausible mechanism for its own thirteen refutations: **the leads
  were derived from a table missing the deciding column.**

## Sources

- rpcsx `docs/arm64/microarchitecture.md:21-70`, `docs/arm64/instruments.md`
- `CLAUDE.md`, the manual-derived prior and the BCAX result
