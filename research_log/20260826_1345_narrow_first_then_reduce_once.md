# Narrow first, then reduce once — and ARMSX2 already does

**Goal: continue `microarchitecture.md`. The reductions section is concrete
codegen advice, and the shared layer will do reductions.**

**Device-free: one section, plus a grep over fork source. No device used.**

## Reduction cost is not flat in width

| reduce | latency | throughput | pipes |
| --- | --- | --- | --- |
| `ADDV`/`UADDLV`, **4H/4S** | **2** | **2** | `V13` |
| `ADDV`/`UADDLV`, 8B/8H | 4 | 2 | `V13, V` |
| `ADDV`/`UADDLV`, **16B** | **4** | **1** | `V13` |
| `UMAXV`/`UMINV`, **4H/4S** | **2** | **2** | `V13` |
| `UMAXV`/`UMINV`, **16B** | **4** | **1** | `V13` |

> **A 16-byte reduction is twice the latency and half the throughput of a 4-lane
> one, and every form is stuck on `V13` — the same two pipes as every vector
> shift.**

**And §4.7 adds a cycle on top**: ASIMD reductions **belong to no forwarding
region at all**, so whatever consumes the result — *"invariably a move to a
general-purpose register"* — waits one more.

## The rule

> **"Narrow the data with pairwise operations on `V`, then reduce once, as narrow
> as possible."**

**`UMAXP` — pairwise max — is latency 2, throughput 4, ALL FOUR PIPES.** So the
folding happens on the wide, unrestricted part of the machine and **only the final
small reduction touches the contended pair.**

> **"Reaching for `ADDV`/`UMAXV` on a full 16-byte vector is the expensive
> spelling of a reduction, not the natural one."**

## Third instance today of the same discipline note

**The `scan16_rdata` rewrite was recorded as 65 instructions to 42.** The document
now says that **understates it**: *"the change also moved the bulk of the work off
the contended pipe pair."*

| Lever | Recorded reason | Better reason |
| --- | --- | --- |
| `TBL2` for `TBX2` | `TBX2` is 2x `TBL2` | the SEQUENCE is a wash — `TBL`+`ORR` = 4 = `TBX` |
| `MLA` -> `MADD` | fewer instructions | `MLA` is throughput 1 on `V0` alone |
| **`scan16_rdata`** | **65 -> 42 instructions** | **work moved off `V13`** |

**Three levers, three times the instruction count was the stated justification and
the pipe column was the real one.**

## Cross-checked against fork source, and ARMSX2 already follows it

**Grepped ARMSX2's `pcsx2/arm64/` for every reduction and extracted the width:**

```
      3 V4S()
```

**All three of its ARM64 reductions use the 4-lane form** — the **latency 2,
throughput 2** row. **None uses 16B**, the latency 4, throughput 1 row.

> **Following the rule already, in every instance.** Worth recording because this
> project's habit is to find a fork doing something wrong; **here is a fork doing
> the expensive thing the cheap way, without a comment saying why.**

**xenia's a64 emitter returns zero reductions**, so the question does not arise
there.

## Where it lands for the shared layer

**Reductions appear wherever a vector result becomes a scalar decision** — texture
hashing, block validation, any movemask equivalent in a translator, a
`memcmp`-shaped scan. **`CLAUDE.md` records that this fleet uses xxHash heavily
and that ARMSX2 validates recompiler blocks with a software CRC32.**

**Both are reduction-shaped**, and the rule applies to whatever replaces them.

## Limits

- **The table is quoted from that document**, and it does not say whether these
  rows are X3's or A715's — **which, per its own correction, matters.**
- **The ARMSX2 grep counts widths at the call site**, not what the assembler
  finally emits.
- **Three call sites is a small sample**, and finding them all correct is weak
  evidence that the rule is understood rather than incidental.
- **Nothing measured.** No device, no bench.

## Sources

- rpcsx `docs/arm64/microarchitecture.md:446-482`
- ARMSX2 `pcsx2/arm64/`, grepped for `Addv`/`Umaxv`/`Uminv`
