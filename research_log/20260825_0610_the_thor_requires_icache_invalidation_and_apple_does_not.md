# The Thor requires instruction-cache invalidation, Apple Silicon does not, and that is how a bug survives upstream

**Goal: read `memory-model.md`'s instruction-cache section, and sweep the fleet
for the defect it describes.**

**The fleet is clean. The device fact and the harvesting trap are both new
here.**

## The device fact

**x86 instruction caches are coherent with the data caches: write code, jump to
it, done. AArch64 makes no such promise, and what it requires VARIES BY
IMPLEMENTATION and is advertised in `CTR_EL0`:**

    IDC = 1   data cache clean to PoU not required
    DIC = 1   instruction cache invalidation not required

**Read on this Thor's Snapdragon 8 Gen 2, by running a probe rather than
assuming:**

> **`IDC = 1`, `DIC = 0`**, with **64-byte I and D minimum lines.**

**So the data cache does not need cleaning, and the instruction cache genuinely
DOES need invalidating.**

**`hardware_ref/thor/THOR_TARGET.md` records `/proc/cpuinfo` hwcaps, the core
layout and `CNTFRQ_EL0`. It does not record `CTR_EL0`**, and this is the
register that decides what every code writer on the device must do.

## The trap that let it survive upstream

> **"Apple Silicon reports `DIC = 1`, which is almost certainly why this survived
> upstream despite RPCS3 running on arm64 Macs: on that hardware the missing
> maintenance is harmless."**

**This is an "it works on the other ARM machine" trap**, and it is a specific
caution for this project: **arm64 code harvested from a project tested on Apple
Silicon may omit maintenance this chip requires**, and it will not fail there.

**It generalises past i-cache.** Apple's ARM implementation differs in several
places where the architecture leaves a choice — **the architected event stream
fires about every 1 µs on Apple against about 100 µs here**, recorded earlier
today from the same fleet. **Two independent Apple-versus-Android divergences,
both invisible until measured.**

> **When an architecture makes a guarantee OPTIONAL, read the register that says
> whether THIS part provides it.**

## The defect, and the sweep

rpcsx had **seven sites** carrying `asm("ISB"); asm("DSB ISH");`, *"wrong three
separate ways"*:

1. **The barriers are reversed.** `DSB` must complete before `ISB`, so the store
   is visible before the pipeline flush. **As written the flush happens first and
   orders nothing.**
2. **Neither is `volatile` and neither clobbers memory** — the compiler was free
   to move them across the very writes they exist to publish, or delete them.
3. **No cache maintenance at all**, which `DIC = 0` requires.

**And the larger hole was not the hand-written trampolines but MCJIT.**
`llvm::SectionMemoryManager` invalidates in `finalizeMemory`; **both custom
managers overrode `finalizeMemory` to `return false` and do nothing**, so
**nothing was invalidating anything for compiled PPU or SPU code.**

### The sweep: the fleet is clean

**Every code-writing fork has maintenance**, counted in fork-own source with
vendored trees separated:

| Fork | i-cache maintenance calls, fork-own |
| --- | --- |
| ARMSX2 | 19 |
| eden | 13 |
| xenia | 7 |
| azahar | 4 |
| Cemu, melonDS | 3 |
| **Vita3K** | **0 fork-own, 3 vendored** — **correct**: its JIT is dynarmic, and dynarmic does it |

**And the reversed-barrier shape is absent.** Searched all seven for an inline
`asm` containing `isb` with a `dsb` within two following lines: **no hits in any
fork.**

**So this defect was rpcsx's**, and rpcsx is out of the packed binary. **The
sweep is a negative result and worth recording as one**, because the class is
cheap to re-run and the next JIT change is exactly when it would reappear.

## The rule that outlives the defect

> *"This is a correctness fix, **not a speedup**, and its failure mode is a stale
> instruction fetch: **an unreproducible crash or wrong branch, never a slow
> frame. Do not expect to see it in a measurement.**"*

**This project's measurement discipline is built to detect slow.** It has no
instrument for *wrong*, and a whole class of ARM64 defects — this one, the
`fptosi` correction that became a corruption, the `sse2neon` NaN divergences,
Cemu's three-way `psq_st` disagreement — **produce a wrong result and a normal
frame time.**

**The instrument for those is a differential test**, which this project has
almost none of, and `CLAUDE.md` already says so.

## Where this lands

- **`CTR_EL0`: `IDC=1, DIC=0`, 64-byte lines** belongs in the hardware
  reference, beside the hwcaps and `CNTFRQ_EL0`.
- **`__builtin___clear_cache` is the portable right answer** — *"it consults
  `CTR_EL0` itself and emits only the maintenance the implementation needs"*.
- **A persisted code cache writes code at load**, so `PERSIST` inherits this:
  ARMSX2's `.vuprog` cache and eden's NCE patcher both put instructions into
  executable memory.
- **The cheat engine inherits it too.** VitaCheat's `$A000` opcodes write ARM
  code and need JIT invalidation, recorded here as a gap dmnt cannot express;
  **this is the host-side half of that.**

## Limits

- **The `CTR_EL0` reading is rpcsx's probe on its device**, which is this same
  Thor model but not re-run here.
- **The sweep counts calls; it does not verify each is correct or in the right
  place.** A fork can call `__builtin___clear_cache` on the wrong range.
- **`finalizeMemory` returning false was rpcsx's specific MCJIT hole**; no fork
  here uses MCJIT, so that shape was not swept.

## Sources

- rpcsx `docs/arm64/memory-model.md:254-304`
- rpcsx `tools/test_thor_arm64_icache_maintenance.ps1`
- `research_log/20260825_0430_the_wfe_stampede_and_a_12_percent_thermal_drift.md`
