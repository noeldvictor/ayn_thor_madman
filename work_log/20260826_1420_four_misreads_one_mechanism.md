# Four instruction-count misreads, one mechanism, one place

**Goal: three findings this week each said "the instruction count was the stated
reason and something else decided it". Put them with the instance already on
file, and name the shared mechanism.**

**No device. One editorial change.**

## What was consolidated

`CLAUDE.md` already carried **xenia's prolog — 18 instructions to 13, measured
slower** — under the rule *"use inflation to choose which subsystem to attack, do
not use it to judge a peephole."* **Three more arrived this week and were sitting
in three separate research logs.**

| Lever | The count said | What decided it |
| --- | --- | --- |
| xenia's prolog | 18 -> 13 | deeper dependency chain, scarce port |
| `TBL2` for `TBX2` | `TBX2` is 2x `TBL2` | **the SEQUENCE is a wash** |
| `MLA` -> `MADD` | fewer instructions | **`MLA` is throughput 1 on `V0`** |
| `scan16_rdata` | 65 -> 42 | **work moved off `V13`** |

**Three of the four are the pipe column.**

## Why consolidating matters more than the individual findings

**Each of the three was recorded correctly in its own log, and each would have
been read alone.** The pattern is only visible with all four in one table — and
**the fourth was already in `CLAUDE.md` under a rule phrased so generally
("instruction count is not the metric") that it did not point at what to look at
instead.**

> **The rule now names its replacement**: *the metric it is usually hiding is
> WHICH PIPES.* **A lever that cannot name the pipes of the instructions it
> removes and adds has not been analysed.**

**That is a checkable demand.** The old phrasing rejected a bad argument; the new
one says what a good one contains.

## And a positive form, deliberately

**A rule that only rejects gives no guidance to somebody writing a new lowering.**
So the entry also carries the constructive half from the same source: **narrow
with pairwise operations on `V`, then reduce once, as narrow as possible** — with
the numbers, and with the note that **ARMSX2's three ARM64 reductions already do
it.**

## Both stores, for all four levers

**None of the four is proposed for a re-run, and all four are already settled in
one store or the other:**

| Lever | Status |
| --- | --- |
| `TBL2` for `TBX2` | **null**, in `REJECTED.md` and the ledger |
| `MLA` -> `MADD` | **rejected by azahar**, in `REJECTED.md` |
| xenia's prolog | **an ACCEPTED win**, +2.04% — the count misled, the lever worked |
| `scan16_rdata` | rpcsx's, **accepted**, and the reason was restated not reversed |

> **Two rejections and two acceptances**, which is why the entry is about how to
> ARGUE a lever rather than about any of these four. **`exp_ledger.py check`
> `bcax` was run earlier today** and returned the `EOR3` `DEAD` row; nothing here
> revisits it.

## Limits

- **An editorial change.** No new evidence; four existing findings placed
  together.
- **The mechanism is a hypothesis across three of four instances.** xenia's
  prolog is a dependency-chain and port-pressure case, not a pipe-assignment one,
  **so the table is not four of a kind.**
- **This repo's own extracted tables still lack the pipe column**, so the demand
  the rule now makes cannot yet be met from this repo's hardware reference alone.

## Files

- `CLAUDE.md` — the four instances, the mechanism, and the positive form
