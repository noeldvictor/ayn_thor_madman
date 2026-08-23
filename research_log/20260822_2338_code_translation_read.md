# Code translation: the shareable part, and 40 methods melonDS never received

**Goal: read `OWNED.md` queue item 8, the last unread candidate.**

Session 2026-08-22 23:38. `CLAUDE.md` puts code translation last because it is
the deepest reach into a core.

**Result: the translation is not shareable and was never going to be. The code
cache underneath it is. And a concrete harvest fell out: melonDS's ARM64 emitter
is Dolphin's from 2015, and Dolphin has added 40 methods since.**

---

## Applying the new rule: search the mechanism

Not "recompiler" or "jit". The mechanisms a code cache must use:
`PROT_EXEC`, `__builtin___clear_cache`, `MAP_JIT`, `memfd_create`.

| Fork | Files | Where |
| --- | --- | --- |
| rpcsx | 11 | `SPUCommonRecompiler.cpp`, `SPULLVMRecompiler.cpp`, `PPUThread.cpp` |
| eden | 5 | `common/host_memory.cpp`, rest inside vendored dynarmic |
| **xenia** | 3 | `base/memory_posix.cc`, `a64_backend.cc`, **`a64_code_cache.cc`** |
| melonDS | 3 | `ARMJIT.cpp`, `ARMJIT_Global.cpp`, `dolphin/Arm64Emitter.cpp` |
| ARMSX2 | 3 | `arm64/AsmHelpers.cpp`, `BaseblockEx-arm64.h`, `Memory.cpp` |
| Cemu | 1 | `util/MemMapper/MemMapperUnix.cpp` |
| Vita3K | 1 | `modules/kubridge/kubridge.cpp` |
| azahar | 0 | uses vendored dynarmic |

**Three forks — azahar, Vita3K and eden — vendor dynarmic**, which already
solves the code cache for them. That is a genuine shared dependency, already
recorded in `ANCESTRY.md`, and it means the question only concerns the five
forks with their own backends.

## The split, and it mirrors every other subsystem

| | Shareable | Not shareable |
| --- | --- | --- |
| What | executable memory, W^X handling, icache invalidation, code buffer lifetime, block linking bookkeeping | guest ISA decode, IR, register allocation, instruction selection |
| Guest semantics | **none** | **all of it** |

**xenia is again the only fork that separated it**, with
`a64_code_cache_posix.cc` at 632 lines standing apart from
`a64_emitter.cc` at 6,641 and `a64_sequences.cc` at 7,475. Its A64 backend is
27,780 lines total, of which the code cache is about 2%.

**That 2% is the whole extractable surface**, and it is the third time today
xenia turns out to have drawn the boundary the project wants.

**Verdict: queue item 8 stays last, and shrinks.** There is no shared
recompiler. There is a shared code cache, it is small, and it should be folded
into the device-layer work rather than treated as its own extraction.

---

## The finding that matters: melonDS carries Dolphin's 2015 ARM64 emitter

`melonDS-android-lib/src/dolphin/Arm64Emitter.cpp` opens:

```
// Copyright 2015 Dolphin Emulator Project
// Licensed under GPLv2+
```

**`ANCESTRY.md` records melonDS carrying Dolphin code from 2008 and 2009.** This
is a **separate, later import**, and it is in the JIT rather than the frontend.

Dolphin is on disk at `dolphin-thor/dolphin`, so the divergence is measurable
rather than assumed.

| | melonDS's copy | Dolphin today |
| --- | --- | --- |
| `Arm64Emitter.cpp` | 4,496 | 4,474 |
| `Arm64Emitter.h` | 1,157 | **1,495** |
| emitter methods | 308 | **344** |

### A hypothesis I had, and it was wrong

I expected Dolphin to have added the device's vector instructions, since
melonDS emits no `SDOT`, `UDOT`, `EOR3` or `BCAX`.

**It has not.** Dolphin's current emitter has none of them either. **melonDS is
not behind Dolphin on dot product or three-input bitwise; nobody in that lineage
ever added them.** Recorded so it is not re-checked.

### What Dolphin actually added: 40 methods

```
ABI_CallFunction  ABI_CallLambdaFunction  BFXIL  BIF  BIT  CMEQ  CMGE  CMGT
CMHI  CMHS  CMLE  CMLT  CMTST  CNEG  EXT  EmitExtract  EmitScalar2RegMisc
EmitScalarPairwise  EmitScalarThreeSame  FACGE  FACGT  FADDP  FMAXNMP  FMAXP
FMINNMP  FMINP  FRINTI  MOVI2RImpl  NEGS  NOP  ORR_BIC  ParallelMoves
PoisonMemory  SEV  SEVL  SHL  SSHR  URSHR  WFE  WFI  YIELD
```

Three groups matter here:

**1. Eight vector compares, and bit select.** `CMEQ`, `CMGE`, `CMGT`, `CMHI`,
`CMHS`, `CMLE`, `CMLT`, `CMTST`, plus `BIF` and `BIT`, plus `EXT` for lane
extraction. **melonDS emits none of these**, and a DS geometry engine is exactly
the workload that wants vector compares and bit select.

**2. The spin and wait family: `YIELD`, `WFE`, `WFI`, `SEV`, `SEVL`.**

**This lands directly on a finding already in `CLAUDE.md`**, which records that
`yield` is a no-op on ARM, that rpcs3 found half of all CPU time in a four-line
`busy_wait`, and that the fleet should be audited for it. **Dolphin's emitter can
emit the whole family and melonDS's copy cannot emit any of it.**

**3. Call and move infrastructure.** `ABI_CallFunction`,
`ABI_CallLambdaFunction`, `ParallelMoves`. `ParallelMoves` in particular is the
standard fix for shuffling register assignments without a scratch spill, which
matters given the X3 guidance to spill to the vector file rather than to memory.

melonDS added five of its own: `LDRGeneric`, `STRGeneric`, `QuickTailCall`,
`SBFX`, `SetCodeBase`.

### The licence permits this harvest

Dolphin's file is **GPLv2 or later**. melonDS-android is GPL-3.0. **An
or-later grant means the newer methods can be taken directly**, as code, not
only as ideas.

**This is the thesis of the project with a name and a number**: copy once,
diverge forever, never receive the fixes. Twelve years, 40 methods, one file,
and the licence never stood in the way.

---

## What this does not claim

**No performance claim is made.** Whether emitting `CMEQ` instead of a scalar
sequence is faster on this device is unmeasured, and the fleet has a ledger for
exactly that question.

**And the emitter having a method does not mean the JIT uses it.** The
`-march`/emitter check earlier today made that distinction and it applies here:
this is 40 methods available to be called, not 40 methods called. **Check the
callers before promising anything.**
