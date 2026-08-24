# Read the addressing mode, not the instruction name — and multi-register is not structured

**Goal: continue `microarchitecture.md` now its self-correction is read. One
section bears directly on a finding I recorded this morning.**

**Device-free: one section. No device used.**

## `LD1`/`ST1` and `LDP`/`STP` are the same instruction to the core

**Identical in all three columns, both directions:**

| instruction | latency | throughput | pipes |
| --- | --- | --- | --- |
| `LDP`/`LDNP` Q, immed offset | 6 | 3/2 | `L` |
| `LD1`, 1 element, multiple, **2 reg**, Q-form | 6 | 3/2 | `L` |
| `STP`/`STNP` Q, immed offset | 2 | 1 | `L01, V01` |
| `ST1`, 1 element, multiple, **2 reg**, Q-form | 2 | 1 | `L01, V01` |

**Arm's own section 4.3 example is written with `LDP`/`STP`, and nothing in the
timing data prefers them.**

> **"The variable that actually mattered was the ADDRESSING MODE."**
> `vld1q_u8_x2` compiled to the **writeback** form —
> `ld1 {v0.16b, v1.16b}, [x8], #32` — **which adds the `I` pipe.** The plain
> vector loads produced non-writeback `LDP` at fixed offsets.
>
> **"Read the addressing mode, not the instruction name."**

## And this guards against over-generalising this morning's finding

**I recorded azahar rejecting `LD2`/`LD4`/`ST4`, citing the A510's Q-form 32-bit
`ST4` throughput of `1/50`.** Somebody reading that could reasonably conclude
"multi-register vector loads are bad on this device."

> **They are not the same instruction class.** `LD1` with two registers **loads
> two registers**. `LD2`/`LD4` **deinterleave lanes**, which is real work in the
> load unit.
>
> **`LD1 {v0,v1}` costs what `LDP` costs. `ST4` costs one store per fifty
> cycles.** Same family of mnemonics, two orders of magnitude apart.

**Conflating them would be exactly the error this section warns about** — reading
the name rather than the semantics. **Both findings are correct and they do not
generalise into each other.**

## A hidden arithmetic in a load

**Writeback adds the `I` pipe**, because the base-register update is an integer
operation folded into the load.

**This repo carries "prefer a load over arithmetic on the mid cores"**, which
rests on the A715/A710 having three load ports against two arithmetic ports.

> **A writeback load is a load that also does arithmetic**, so it partially
> spends the imbalance the rule exists to exploit. **Non-writeback with a fixed
> offset keeps the load on the load pipes.**

**That is not stated anywhere in this repo**, and it is the kind of detail that
decides whether a hand-written sequence realises the port advantage or gives it
back.

## A discipline note worth more than the instruction detail

> **"That reasoning was wrong, even though the conclusion was right."**

**The fork corrected a REASON that had produced a correct outcome.** Nothing was
broken and nothing needed re-deciding — **but the reason travels**, and the next
person applying "prefer `LDP` over `LD1`" would apply it where it buys nothing
and miss the addressing mode where it buys something.

**This project has the same pattern in its own record**: the `VUOverflowHack`
comment blaming x86 for an IEEE-754 constraint, correct in conclusion and
misleading in reason. **A right answer for a wrong reason is a latent wrong
answer.**

## Limits

- **One section, tables quoted from that document**, not read from the ARM guides
  here.
- **The rows are the X3's or the A715's — the document does not say which for
  this table**, and per its own correction that distinction matters. **Treat the
  numbers as indicative and the equivalence as the finding.**
- **The `LD2`/`LD4` distinction is mine**, drawn from what the instructions do;
  neither fork states it. **azahar's `1/50` is a manual figure for `ST4`, not a
  measurement, and this repo already records it as such.**

## Sources

- rpcsx `docs/arm64/microarchitecture.md:328-355`
- `research_log/20260825_0200_a_first_class_ir_operation_measured_3x_to_15x.md`
