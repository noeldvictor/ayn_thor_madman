# Residency has not been shown to win on device, and one disabled lowering blocks three optimisations

**Self-audit after finding that I recorded a superseded frame anatomy this
morning: which other conclusions did I take from a single dated document?**

**The largest was the register-residency finding, from a document dated
2026-06-26. The ledger runs to 2026-08-07. Queried it, and there is a later
on-device result.**

## The device test, and it is `CONFOUNDED`

`llvm_residency_ladder_thor`, **2026-07-24** — and it notes it is the **first run
ever with `cpu_backend_llvm` actually on**, because the prior stack never passed
it and it is default-off.

| Arm | VdSwap/s | `LLVMmap` |
| --- | --- | --- |
| a64 baseline | 7.8 | — |
| L0 llvm | 8.1 | 222 |
| **L1 + context_residency + writeback** | **9.9** | **5,685** |
| L2 + residency_abi | 8.1 | 450 |
| L3 + saverest | 8.1 | 354 |

**L1's apparent +27% is not a win.** Its screenshot is the *"Character Design /
Akira Toriyama"* opening-credits camera while L0's is a different, more open one.

> **BD's intro advances at a rate that depends on emulation speed, so a fixed
> wall-clock sample lands on a different frame per run.** That is the documented
> cross-run scene confound. **And the `LLVMmap` spread — 222, 5,685, 450, 354 —
> shows the arms executed very different amounts of code.**

**So residency is measured on device and no win is demonstrated.**

## What this does and does not do to the earlier finding

**Stands, because it is static and does not depend on a scene:** the disassembly
kill test. The same guest loop compiled with guest registers as **context-struct
fields** produces **~4 memory ops per iteration**; **localized to C locals** it
produces **0**, keeping them in `x24`, `x21`, `x23` across the loop and an opaque
call.

**Does not stand as written:** any implication that residency is a demonstrated
device win. **`TRANSLATION.md` presented the mechanism as proven and the payoff
as following from it.** The mechanism is proven. **The payoff is `CONFOUNDED`.**

**And the in-JIT route remains crashed** — `arm64_register_cache_inherit`,
host-correct with 320 assertions, `SIGBUS` on a back-edge, 12+ device fires.

## The bigger finding in the same entry: one disabled lowering blocks three things

**Marked as solid, unlike the timing numbers:**

- The LLVM backend runs on that title on the Thor with **0 faults and no
  regression**.
- **Lowering coverage is high**: `LLVMbegin == LLVMmap == 1865` — every attempted
  lowering succeeded.
- **A systematic hole.** **30+ unique functions log `LLVMfallback -> a64`, and all
  of them are opcode `mul_add`/`mul_sub`** — the deliberate
  `cpu_backend_llvm_lower_vmaddfp=false` workaround.

> **So every `vmaddfp`-using function is excluded from LLVM, and therefore from
> residency, and therefore from the AOT object cache.** **Fixing `vmaddfp` is a
> COVERAGE lever, not just a correctness fix.**

**That is three optimisations gated behind one disabled lowering**, and it is the
kind of second-order cost this repo's "every win was a bug" pattern predicts.

**It is careful about what it does not claim**: the title's known
vertex-transform function was checked and **was not** in that run's fallback
list, so no claim is made about it.

## And the fix for the confound is a third form of comparable measurement

**Its own next step:** `bd_fixed_frames_bench.ps1` — **time to render a fixed
frame range** of the no-input deterministic intro, **content-matched by
construction.**

**This repo now has three ways to make arms comparable, and only one needs a
savestate:**

| Route | Needs | Removes |
| --- | --- | --- |
| **Fixed work unit** — time a fixed frame range | a deterministic opening | the scene confound, by construction |
| **Work-normalised metric** — divide by something the change does not touch | a denominator | the scene confound, by arithmetic |
| Savestate fixture | a savestate | the route, but not run-to-run drift |

**The first is the cheapest of the three where a title has a deterministic
intro**, and it needs no savestate — which matters because **Cemu and eden have
none.**

## Limits

- **The ladder is one title, one run per arm.** This repo's own rule says never
  quote n=1, and the entry's verdict is `CONFOUNDED` rather than a number.
- **`CONFOUNDED` is not `DEAD`.** Residency may still win; **it has not been
  shown to.**
- **The `vmaddfp` coverage claim is that fork's**, read from the ledger, not
  reproduced.
- **This is the second superseded conclusion found by this audit today.** The
  remaining single-document conclusions from today — the `LOAD_OP_CLEAR` null and
  the lv2 spin — **come from rpcsx, which has no queryable ledger**, so the same
  check cannot be run there.

## Sources

- xenia `tools/exp_ledger.py`, entry `llvm_residency_ladder_thor` (2026-07-24)
- xenia `docs/research/20260626-static-recomp-residency-eval.md` (2026-06-26)
