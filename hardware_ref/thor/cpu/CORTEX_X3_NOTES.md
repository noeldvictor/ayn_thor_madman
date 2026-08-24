# Cortex-X3: codegen rules that matter here

Distilled from `cortex-x3-software-optimization-guide.pdf`, revision r1p2,
issue 4.0, section 4. Extracted 2026-08-22.

**The X3 is the Thor's single prime core.** It is where emulator hot paths
belong, and where a recompiler's output should be tuned. The other three core
types have their own guides and their own answers; see
[Not the whole story](#not-the-whole-story).

Everything below is from the manual. **None of it is measured on this device
yet.** Treat each item as a hypothesis with a good source.

## The rules a recompiler can act on today

### 1. Spill general-purpose registers to the vector file, not to memory

> Register transfers between general-purpose registers (GPR) and ASIMD
> registers (VPR) are lower latency than reads and writes to the cache
> hierarchy, thus it is recommended that GPR registers be filled/spilled to the
> VPR rather to memory, when possible.

**This is the highest-value item on the page for an emulator.** Register
pressure is the central problem in a guest-to-host recompiler, and the standard
answer is a stack spill. On this core the vector register file is a faster
spill space than L1.

Four forks generate ARM64 and every one of them spills. Worth checking what
each does.

### 2. Fusion only happens when the pair is adjacent and unshifted

Fusible pairs:

- `AESE` + `AESMC`, `AESD` + `AESIMC`
- `CMP`/`CMN` immediate + `B.cond`
- `CMP`/`CMN` register + `B.cond`
- `TST` immediate + `B.cond`
- `TST` register + `B.cond`
- `BICS` register + `B.cond`
- `NOP` + anything

Conditions:

- The pair **must be adjacent in program order**. An instruction scheduled
  between them loses the fusion.
- **No shifted or extended register forms** for `CMP`, `CMN`, `TST`, `BICS`.
- `BICS` must target `XZR` or `WZR`.

**Codegen rule:** emit compare-and-branch as an adjacent pair, and prefer the
plain register form over a shifted one even when the shifted form saves an
instruction.

### 3. Four branches per 32 bytes, maximum

> For best case performance, avoid placing more than four branch instructions
> within an aligned 32-byte instruction memory region.

Thirty-two bytes is eight AArch64 instructions. **A recompiled block with many
exits packs branches densely and will hit this.** It matters for guest code
with tight conditional flow, which is most guest code.

### 4. Zero-latency moves

These use no scheduling or execution resource:

```
MOV Xd, #0      MOV Xd, XZR     MOV Wd, #0      MOV Wd, WZR
MOV Hd, WZR     MOV Hd, XZR     MOV Sd, WZR     MOV Dd, XZR
MOVI Dd, #0     MOVI Vd.2D, #0
MOV Wd, Wn      MOV Xd, Xn      (not always)
```

Register-to-register moves are usually free, so **do not contort codegen to
avoid a move.** Zeroing is free, so prefer it to any clever alternative.

### 5. Do not interleave vector forwarding regions

FP and ASIMD latency **increases by one cycle** when producer and consumer are
in different forwarding regions.

| Region | Contents |
| --- | --- |
| 1 | ASIMD and SVE ALU, shift, insert and move, abs, cmp, max, min |
| 2 | FP, ASIMD and SVE multiply and multiply-accumulate, FP compare, FP add and sub |
| 3 | Crypto, SHA1, SHA256 |
| 4 | AES, polynomial multiply, and everything in region 1 |
| 5 | BFDOT and BFMMLA |

Outside every region: FP divide and square root, FP convert and rounding that
does not write a GPR, ASIMD integer multiply and multiply-accumulate, ASIMD
reduction.

> It is not advisable to interleave instructions belonging to different
> regions.

For region 2, **producer and consumer precision must match** — single with
single, double with double, half with half. A `MOV` into a lane is a region 2
producer but **not** a region 2 consumer, so it breaks a chain.

**This is a scheduling rule a generic ARM64 backend will not know.** It matters
most for guest vector units: PS2 VU, Xbox 360 VMX128, DS geometry.

### 6. Special register access serialises, and this is an emulator trap

Most special-purpose registers are **not renamed**. Reads and writes get
non-speculative execution, in-order execution, or a flush side effect.

| Register | Read | Write |
| --- | --- | --- |
| `NZCV` | fully renamed, free | fully renamed, free |
| `SP` | fully renamed, free | fully renamed, free |
| `FPCR` | in-order | non-speculative, in-order, **maybe flush** |
| `FPSR` | non-speculative, in-order | non-speculative, in-order |
| `FPSCR` | non-speculative, in-order | non-speculative, in-order, **maybe flush** |
| `APSR` | non-speculative, in-order | non-speculative, in-order |

> FPSR/FPSCR reads must wait for all prior instructions that may update the
> status flags to execute **and retire**.

**Consequence.** Guest condition flags mapped onto `NZCV` cost nothing, because
`NZCV` is fully renamed. **Guest floating-point status flags mapped onto
`FPSR`, and guest rounding-mode changes mapped onto `FPCR`, serialise the
machine.**

Emulators commonly emulate guest FPU status faithfully. On this core that is a
pipeline stall per access, and a flush on some `FPCR` writes. **Check what each
recompiler does with guest FP status and rounding mode.** Lazy or deferred
handling could be a large win, and it is exactly the kind of lever the
experiment ledger should record.

### 7. Alignment cases that cost

The core handles most unaligned access without penalty. These are the
exceptions:

- A load crossing a **64-byte cache line** boundary.
- A quad-word load that is not **4-byte** aligned.
- A store crossing a **32-byte** boundary.

**Align stores on 32-byte boundaries where possible.**

### 8. Store-to-load forwarding has rules

Guest memory emulation stores and then immediately loads the same address
constantly, so these matter:

- The load start address must align with the **start or middle** of the older
  store.
- A load larger than 8 bytes can forward from at most **2** stores, and each
  must supply either the first or second half.
- A load of 4 bytes or fewer can forward from only **1** store.

### 9. Dispatch limits shape instruction mix

> ### THIS FILE IS MISSING THE COLUMN THAT DECIDES BORDERLINE CASES
> **Added 2026-08-26.** Arm's guides give three numbers per instruction:
> **latency, throughput, and UTILISED PIPELINES.** These notes carry the first
> two and **no per-instruction pipe assignment** — the table below is a
> DISPATCH CAP, which is about pipelines but is not the same thing.
>
> **`V` is all four FP/ASIMD pipes; `V01` and `V13` are two; `V0` is one.**
> A "faster" instruction confined to `V0` can lose to a slower pair spread
> across `V`.
>
> **This is the likely mechanism for this project's thirteen refuted
> manual-derived predictions.** Every one was argued from an instruction's own
> cost rather than from what it contends with. **The one manual prediction that
> HELD — `BCAX` at 2.00x latency, measured 1.96x / 2.01x / 2.00x on X3 / A715 /
> A510 — used the pipe column and predicted a throughput LOSS alongside the
> latency win.**
>
> **AND THIS IS NOT A SUGGESTION TO USE `BCAX`.** `exp_ledger.py check "bcax"`:
> that fusion is **`DEAD`** in xenia — **0 of 1 V128 XORs were fusable chains**,
> so a HIR pass would have folded nothing. **The instruction is settled; the
> matched prediction is evidence about the METHOD, measured on a bench rather
> than in an emulator.**
>
> **THE COLUMN IS NOW BELOW, at the end of this file**, for the instructions
> this fleet's codegen actually selects. **Transcribed at two hops** — from
> rpcsx's `microarchitecture.md`, which transcribed Arm's guides — **so verify
> against the guide before betting a lever on a row.**
>
> **Quote all three columns, or do not quote the guide.** See
> [`../../../research_log/20260826_1120_the_pipelines_column_is_why_manuals_kept_failing.md`](../../../research_log/20260826_1120_the_pipelines_column_is_why_manuals_kept_failing.md).

Up to 8 MOPs in per cycle, up to 16 μOPs dispatched, with per-cycle caps:

| Pipelines | Max μOPs |
| --- | --- |
| S or B | 4 |
| M | 4 |
| M0 | 2 |
| V0 | 2 |
| V1 | 2 |
| L | 6 |

**A long run of one instruction type throttles on its pipeline cap.** Mixing
scalar and vector work interleaves better than batching each.

### 10. Memory routines

- Unroll. Multiple loads and stores per iteration.
- Use **non-writeback** `LDP` and `STP` forms, interleaved.
- Align stores to 32 bytes.
- **`DC ZVA` beats `STP` for zeroing.** Relevant to guest memory clears and to
  clearing emulated VRAM.
- `LDNP` for non-cacheable source, but keep `STP`/`STR` for the stores.

### 11. Instructions to avoid

Decode-limited, so avoid in hot code: `LD4` and `ST4` multi-structure forms,
`LD4R` post-indexed with 64-bit elements, and several SVE gather forms where
the index register is also the destination.

**A guest vector unit lowered naively to `LD4`/`ST4` will hit this.**

## Not the whole story

The Thor is 1 + 4 + 3 and these cores do not share a pipeline model.

| Cluster | Core | Guide, still to distil |
| --- | --- | --- |
| Prime, 1 | Cortex-X3 | this file |
| Performance, 2 | Cortex-A715 | `cortex-a715-software-optimization-guide.pdf` |
| Performance, 2 | Cortex-A710 | `cortex-a710-software-optimization-guide.pdf` |
| Efficiency, 3 | Cortex-A510 | `cortex-a510-software-optimization-guide.pdf` |

**"Faster on the X3" and "faster on the A510" are different questions.** State
the cluster in every claim. Code tuned to X3 dispatch limits may be wrong on
the A510, which is a much narrower core.

**The Thor Lite is a Snapdragon 865**, Cortex-A77 and A55, and matches none of
these guides. If Lite support is ever wanted, none of this transfers.

## Where the manuals live

`armsx2-thor/ARMSX2/docs/reference/arm/`, about 6 MB of optimization guides.

The 66 MB architecture reference manual is **deliberately not tracked** there,
which matches this repo's rule against committing large manuals. Download it
from ARM if instruction semantics are needed; nothing depends on it.

## What to do with this

1. **Check what the four ARM64 recompilers do about guest FP status flags.**
   Rule 6 says a faithful implementation serialises the machine.
2. **Check whether any of them spill to the vector register file.** Rule 1 says
   they should.
3. Record both in `capability_inventory.md`, and any experiment in the ledger.
4. Distil the A715, A710 and A510 guides. Threads land on those clusters too.

---

## Utilised pipelines, for the instructions this fleet selects

**Added 2026-08-26.** The column this file was missing. **`V` is all four
FP/ASIMD pipes; `V01` and `V13` are two; `V0` and `V1` are one.**

> **PROVENANCE: transcribed from rpcsx `docs/arm64/microarchitecture.md`, which
> transcribed Arm's Software Optimization Guides.** Two hops from the source.
> **Verify a row against the guide before betting a lever on it**, and note the
> correction below about which core's table applies.
>
> **ONE ROW VERIFIED AT SOURCE, 2026-08-26.** The A715 guide's ASIMD crypto
> table reads `BCAX, EOR3, RAX1, XAR | 2 | 1 | V0` — **the transcription
> survived three hops intact.**
>
> ### AND NAME THE TABLE, NOT JUST THE GUIDE
>
> **The same mnemonics appear TWICE in the same guide.** `BCAX`/`EOR3` is
> **latency 2** in the ASIMD crypto table and **latency 4** in **§3.31 SVE
> Cryptographic, Table 3-48**.
>
> **This device has no SVE**, so the second table is inapplicable — **and
> nothing in the row says so.** A reader grepping for an instruction gets two
> answers and the wrong one is double. **Every row below is ASIMD.**

### Cortex-X3

| instruction | latency | throughput | pipes |
| --- | --- | --- | --- |
| `EOR`/`BIC`/`AND`/`ORR`/`ORN`/`MVN` | 2 | 4 | `V` |
| **`BCAX`/`EOR3`** | 2 | **1** | **`V0`** |
| `SQADD`/`UQADD`/`URHADD` | 2 | 4 | `V` |
| `UMAXP`/`UMAX`/`UMIN` | 2 | 4 | `V` |
| `CNT`/`CLZ`/`CLS` | 2 | 4 | `V` |
| `UABD` | 2 | 4 | `V` |
| `XTN` | 2 | 4 | `V` |
| **`SQXTN`/`UQXTN`** | **4** | 2 | **`V13`** |
| `TBL`, 1-2 table regs | 2 | 2 | **`V01`** |
| **`TBX`, 2 table regs** | **4** | 2 | `V` |

### Reductions, where width changes both columns

| reduce | latency | throughput | pipes |
| --- | --- | --- | --- |
| `ADDV`/`SADDLV`/`UADDLV`, **4H/4S** | **2** | **2** | `V13` |
| `ADDV`/`SADDLV`/`UADDLV`, 8B/8H | 4 | 2 | `V13, V` |
| `ADDV`/`SADDLV`/`UADDLV`, **16B** | **4** | **1** | `V13` |
| `UMAXV`/`UMINV`/`SMAXV`/`SMINV`, **4H/4S** | **2** | **2** | `V13` |
| `UMAXV`/`UMINV`/`SMAXV`/`SMINV`, **16B** | **4** | **1** | `V13` |

**A 16-byte reduction is twice the latency and half the throughput of a 4-lane
one, and every form is pinned to `V13`** — the same two pipes as every vector
shift. **Arm 4.7 adds a further cycle**: ASIMD reductions belong to no forwarding
region, so the consumer waits one more.

> **Narrow with pairwise operations on `V`, then reduce once, as narrow as
> possible.** `UMAXP` is latency 2, throughput 4, **all four pipes**.

### A715 is TIGHTER, and it is the cluster that runs the code

**rpcsx measured cluster load: A710/A715 at 1.06 cores busy against the X3's
0.62.** **The mid cluster is where the work is, and its tables are more
restrictive on exactly these instructions:**

| operation | X3 | **A715** |
| --- | --- | --- |
| **`MLA`, `MLS`** | 4(1), thr 2, `V02` | 4(1), thr **1**, **`V0`** |
| **`SSHL`, `USHL`** | 2, thr 2, `V13` | 2, thr **1**, **`V1`** |
| `CMEQ`, `AND`/`EOR`/`ORR` | 2, thr 4, `V` | 2, thr **2**, `V` |
| `SDOT`/`UDOT` 8-bit | 3(1), thr 4, `V` | 3(1), thr **2**, `V` |
| `ADDV` 8H | 4, thr 2, `V13` | **5**, thr 1, `V1`,`V` |

> **The X3 is the right reference only for work pinned to `cpu7`.** For anything
> on the mid cluster, quote the A715/A710 tables.

**`MLA` being single-pipe on A715 is the mechanism behind azahar's measured
rejection of the `MLA` -> `MADD` fusion**, which regressed both its A715 patterns.
