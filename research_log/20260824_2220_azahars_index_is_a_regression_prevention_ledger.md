# azahar's index is a regression-prevention ledger, and it corrects a negative recorded here

**Goal: read the largest index in the fleet, azahar's 1,458-line `AGENTS.md`.**

**It is a different kind of document from the other seven. The others say how to
work; this one is a list of invariants that must not be silently undone**, at a
level of detail that includes **linked function sizes and expected instruction
sequences.**

## 1. It corrects "PMULL host-side uses: zero"

`CLAUDE.md`'s hardware-repurposing audit records **carryless multiply for texture
swizzle as the best unexploited candidate**, on the grounds that *"every fork has
a large swizzle surface and none uses it — `PMULL` host-side uses: zero"*, and
separately that *"hardware `CRC32` is unused everywhere (searched twice)"*.

**azahar has both.** `externals/cryptopp`: `HasCRC32()`, `HasPMULL()`, and
`gf2n_simd.cpp` using PMULL for Galois-field arithmetic.

**Two qualifications, and they cut in opposite directions:**

- **The searches were right for their scope.** These are inside a vendored
  dependency, which every fleet-wide search here filters out — correctly, since
  a dependency's use of an instruction says nothing about what a fork does.
  **The claim should have said "in fork-own code".**
- **But this is not purely vendored.** azahar's index says `externals/cryptopp`
  is *"intentionally vendored ... so **ARM feature-probe repairs** stay in this
  repository"*, and describes the probe fix in detail. **The fork owns that
  path.**

**And the uses are genuine crypto, not repurposing** — `gf2n` is Galois-field
multiplication, which is what PMULL is for. **So the swizzle candidate is not
refuted.** What changes is better than that:

> **There is a working, runtime-gated PMULL path in this fleet to copy the
> ENABLEMENT pattern from.** This repo proposed PMULL for swizzle and never said
> how to turn it on safely.

## 2. The gating rule, which is the answer to a question this repo left open

> *"Keep CRC32 and PMULL in **specialized translation units** with runtime
> `HasCRC32()` / `HasPMULL()` gates; **never enable optional crypto ISA
> extensions globally.**"*

**That is the opposite of the `-march` baseline decision made here today**, and
both are right for their case:

| | mechanism | why |
| --- | --- | --- |
| **`+lse`, `+dotprod`, `+fp16`** | **global `-march`** | the device is fixed and the features are non-optional on it |
| **`+crypto`, `+aes`, `+pmull`, `+sha3`** | **per-TU flags + a RUNTIME gate** | optional ISA extensions; a global enable lets the compiler emit them **anywhere**, including in code with no runtime check |

> **A global `-march` extension is a licence for the compiler to use the
> instruction in code you did not write and cannot gate.** For a mandatory
> feature that is what you want; for an optional one it removes the fallback.

**And a caution attached to the same entry, which is the honesty this repo
asks for:** *"AES and SHA already use their existing hardware paths, so **do not
attribute their performance to the CRC32/PMULL probe repair**."*

## 3. An LP64 type that silently scalarises a vectorised loop

> *"SoundTouch integer samples require an exact 32-bit `LONG_SAMPLETYPE`;
> **never change it back to C++ `long`, which is 64-bit under Android's AArch64
> LP64 ABI and scalarizes the FIR.**"*

**No crash, no warning, no wrong output — the loop simply stops vectorising.**

**That is a fifth form of the x86 detour and it is not about x86 at all**: a type
whose width is correct on the ABI it was written for (ILP32/LLP64) and wrong
here. **The consequence is invisible in behaviour and visible only in the
emitted code**, which is why the invariant had to be written down.

**The same trap is recorded twice**: the WSOLA correlation state — `corr`,
`lnorm`, `maxnorm` — *"is also intentionally 32-bit. Do not restore C++
`long`/`unsigned long` on Android LP64."*

## 4. Invariants asserted on EMITTED CODE, in an agent instruction file

> *"Final linked code should retain **paired coefficient loads, two sample
> `LD2`, independent `SMLAL`/`SMLAL2` accumulators, and `ADDV` reductions per
> sixteen taps** rather than duplicated coefficient `LD2` or scalar `SMADDL`."*

> *"Final linked code must stay **spill-free** and use two `LD2`, four
> `SMULL`/`SMLAL`, two shifts, two vector adds, and one loop branch per eight
> stereo frames."*

> *"Final `calcCrossCorrBatch4` linked size should remain **568 bytes**, not the
> rejected **3,328-byte** general-loop expansion."*

**This repo has `target_check.py`, which asserts emitted instructions for a
compile TARGET. azahar asserts emitted instruction sequences and a linked SIZE
for a FUNCTION** — and puts them where an agent will read them before touching
the code.

> **A hand-tuned loop needs an assertion about its output, or the next
> well-meaning refactor undoes it and nothing fails.** This is the same idea as
> ARMSX2's `static_assert` on a cache-key struct size, one level up.

## 5. A measured result labelled honestly

> *"Same-session profiling reduced correlation self share **from 1.30% to
> 0.87%** and full SoundTouch processing **from 1.66% to 1.01%**, while a
> six-versus-six process bracket was **neutral**. **Treat this as a
> recurring-hotspot reduction, not an FPS or battery-watt win.**"*

**A real improvement that did not move the frame rate, and it is labelled rather
than promoted.** That is `migration-credit` in this repo's vocabulary, reached
independently — and it is the third fork to state the same discipline in its own
words.

## Limits

- **First 40 lines of 1,458 read.** This is a sample, not a survey.
- **The Crypto++ use was verified by search; `gf2n_simd.cpp` was not read**, so
  "genuine crypto" is inferred from the file's purpose.
- **The instruction-sequence invariants were not verified against a
  disassembly** — azahar builds here, so this is checkable and was not checked.
- **No device, nothing measured.**

## Sources

- azahar `AGENTS.md:1-40`
- azahar `externals/cryptopp/include/cryptopp/cpu.h:532,572`,
  `src/core/gf2n.cpp:1031`
