# Repurposing hardware instructions: audited, and the fleet barely does it

**Goal: check whether the forks exploit ARM64 instructions for jobs other than
the one the instruction was named for — crypto units used as bit mixers, and
similar.**

No device. Reading only. **Host-side and guest-side separated**, which is what
this audit turns on.

## Result

**The technique class is real and well documented. This fleet uses exactly one
instance of it, and that instance measured `DEAD`.**

**Everything else that matched a search is guest decode, feature detection, an
encoding table, or genuine crypto.**

## What the search actually found, once the hits were read

**Counting matches produced a completely wrong picture. Reading them fixed it.**

| Fork | Raw match | What it actually is |
| --- | --- | --- |
| eden | CRC32, AESE, PMULL | **guest decode** — dynarmic translating the guest's own crypto instructions, plus feature detection in `native.cpp` |
| melonDS | CRC32 | **an encoding table** in Dolphin's `Arm64Emitter.cpp` — `0x10, // CRC32B` |
| Cemu | AESE/AESD/AESMC | **genuine crypto** — `util/crypto/aes128.cpp`, Wii U content decryption |
| xenia | FJCVTZS | **a feature-flag definition** — `kA64EmitJSCVT` in `platform_arm64.h` |
| azahar | CRC32 | feature detection, IPS patch checksums, a UDP protocol checksum |
| ARMSX2 | crc32 | game-serial identification, and **recompiler block validation** |

**eden is the worst case for this trap, and for the same reason as before: its
guest ISA is the host ISA.** Every ARM64 crypto mnemonic appears there as
something dynarmic must *decode*. **Counting mnemonics in eden measures nothing.**

## The one real instance, and its verdict

**xenia uses `EOR3` and `BCAX` — `FEAT_SHA3` instructions — as three-input
bitwise operations in VMX lowering.** Six files. `CLAUDE.md` already records the
idea: *"nominally crypto but serve as three-input bitwise operations in guest
vector lowering."*

**It is the only genuine repurposing in the fleet, and xenia's experiment ledger
records the follow-up as `DEAD`:**

> `EOR3/BCAX fusion for VMX bitwise chains` — **DEAD**, 2026-08-06

**So the fleet has tried this class once and it did not pay.** That is worth
knowing before anyone proposes it again.

## A second class the fleet does use: permutes

`TBL`/`TBX` are not crypto, but they are the same idea — a general permute unit
doing a guest's arbitrary shuffle.

**xenia benchmarked them on this exact SoC**, in `docs/arm64/bench-results.md`
via rpcsx's harness:

| test | A715 | A710 | A510 |
| --- | --- | --- | --- |
| `tbl2_tp` | **0.178** | **0.183** | 1.300 |
| `tbx2_tp` | **0.377** | **0.388** | 2.517 |

**`TBX2` costs about 2x `TBL2`**, and xenia found its SHUFB lowering uses the
slower one. **But the fix was measured and came back null** — the ledger records
`SHUFB: TBL2 plus an OR is not faster than TBX2`, at 0.555 against 0.555 on the
A715.

**Chased and closed. Do not re-run it.**

## What is genuinely unexploited

**Ranked by how defensible the case is, not by how exciting it sounds.**

### 1. Carryless multiply for texture swizzle — the best candidate

**Console textures are stored swizzled** — Morton order, tiled, or a
vendor-specific interleave — and unswizzling is **bit deinterleaving**, which is
precisely what a carryless multiply implements in one instruction.

**Every fork has a large swizzle surface and none uses `PMULL` for it:**

| Fork | Files matching swizzle / morton / tile / detile | `PMULL` host-side uses |
| --- | --- | --- |
| xenia | **71** | **0** |
| Cemu | 49 | 0 |
| Vita3K | 47 | 0 |
| azahar | 45 | 0 |
| ARMSX2 | 33 | 0 |

**This sits in pipeline 2, the texture upload path**, which is the flagship
shared feature. **It is the one place where a repurposed instruction would land
in a hot, shared path rather than in one backend's lowering.**

**Unproven.** No fork's swizzle code has been read to see whether it is scalar
bit math, a lookup table, or already vectorised. **Read one before proposing
anything.**

### 2. Hardware CRC32 — real, but the case is weaker than it looks

**No fork uses the ARM64 `CRC32` instruction for anything.**

**Searched twice, different words.** First for the mnemonics
`crc32c?[bhwx]|__crc32` across six forks excluding vendored trees; then for the
intrinsic and builtin spellings `__crc32|vmull_p64|__builtin_aarch64_crc|
_mm_crc32|crc32c[bhwx]`. **The second search returned one file, in melonDS, and
reading it shows an encoding table** — `0x10, // CRC32B` in Dolphin's
`Arm64Emitter.cpp`. **Every other `crc32` in the fleet is a software routine.**

**But do not assume a win.** The literature's ~20x figure is **software CRC32
against hardware or carryless-multiply CRC32** — it is not CRC32 against
xxHash. **The fleet already uses xxHash heavily** — ARMSX2 has 109 `XXH32` and 93
`XXH64` matches, melonDS 54 `XXH64`, and xenia and Vita3K use `XXH3_64bits`.
**XXH3 is vectorised and competitive on long keys.**

**Where hardware CRC32 plausibly wins is short keys** — 8 or 16 bytes per
instruction with `crc32cx` — which is **block validation, not texture hashing**.
**ARMSX2 validates recompiler blocks with a software CRC32** in `iR3000A.cpp`
and `iR5900.cpp`. **That is the concrete site.**

### 3. `FJCVTZS` — record it as not applicable

`FEAT_JSCVT` exists to give **x86 and JavaScript** float-to-int truncation
semantics on ARM. **xenia defines a feature flag for it and uses it nowhere.**

**Our guests are PowerPC, MIPS and ARM.** They do not have x86 conversion
semantics. **Rosetta and FEX need this; we do not.** Recorded so nobody chases
it after reading a Rosetta article.

## The honest summary

**The user's observation is correct as a description of the field and wrong as a
description of this fleet.** Emulators do exploit hardware units creatively.
**These forks, on this device, essentially do not** — one instance, measured
dead, plus a permute-selection question that was chased and closed.

**That is an opportunity and a warning at once.** The opportunity is that the
technique is untried here. The warning is that **the two times anyone did try it,
both results were null** — which is the same pattern xenia's ledger reports for
incremental CPU levers generally.

**Anything proposed from this log needs a measurement, and the ledger queried
first.**

## What was not checked

- **No swizzle implementation was read.** The counts are file matches only.
- **`AES` as a bit mixer** — used in some hash functions — was not searched for
  separately from genuine crypto.
- **`RBIT`, `FRECPE`, `FRSQRTE`** were counted but not read.
- **Compute shaders doing CPU-side work** — a real technique elsewhere — was not
  examined at all.
