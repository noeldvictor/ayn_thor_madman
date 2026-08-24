# `-ffp-contract=fast` reaches the PS2 interpreter, and the interpreter is the differential reference

**Goal: check the `-Ofast` seen in ARMSX2's emitted Release flags.**

**`-Ofast` is harmless. What was beside it is not obviously so, and it lands
somewhere that matters more than a hot path.**

## `-Ofast` is closed

**69 translation units, all of them `3rdparty/oboe`.** That is the audio
library's own build choice, and it touches no guest state. **Checked and
closed.**

## `-ffp-contract=fast` is on 320 TUs of PCSX2's own code

**Of those, 44 are guest-FP-relevant**, matched on `FPU`, `VU`, `COP2`,
`Interp`, `R5900`, `VUmicro` and `Vif`:

```
pcsx2/FPU.cpp
pcsx2/Interpreter.cpp
pcsx2/COP2.cpp
pcsx2/R5900OpcodeImpl.cpp
pcsx2/R3000AInterpreter.cpp
pcsx2/MTVU.cpp
...
```

**`-ffp-contract=fast` permits the compiler to fuse `a * b + c` into a single
FMA**, which **rounds once instead of twice**. It is more permissive than clang's
default of `on`, which confines contraction to a single expression.

## Why the target changes what the flag does

**On baseline x86-64 there is no FMA.** SSE2 is the ABI floor and FMA3 arrived
with Haswell, so on a portable x86-64 build **the compiler has nothing to
contract into and the flag is close to inert.**

**On ARM64, FMA is baseline.** `FMADD` and `FMLA` are always available, so **the
fusion actually happens.**

> **The same flag is nearly inert on the machine it was chosen for and changes
> results on this one.** That is the shape of every other finding today, in a
> build flag rather than in source.

## The part that matters most is the test, not the frame

**The recompiler is unaffected.** Its output is machine code the emitter writes,
not C++ the compiler optimises. **So this flag does not change the hot path.**

**It changes the reference.** ARMSX2 carries the fleet's only CPU differential
test — `tests/ctest/core/recompilers/`, with a `StateSnapshot` capturing register
state for both the R3000A and the R5900 and comparing interpreter against
recompiler.

> **If the interpreter is compiled with fast contraction and the recompiler emits
> its own unfused sequences, the differential test compares an FMA-fused
> reference against a non-fused implementation.** A disagreement would then be
> attributed to the recompiler.

**That is a hazard to check, not a proven bug**, and it is checkable without a
device: compile the interpreter both ways and compare the differential suite's
results.

## The other three built forks are clean

**Scanned every `compile_commands.json` available for `-Ofast`, `-ffast-math`,
`-ffp-contract=*`, `-funsafe-math-optimizations`, `-fno-math-errno` and
`-freciprocal-math`:**

| Fork | Config | FP-semantics flags found |
| --- | --- | --- |
| **ARMSX2** | Release | **`-ffp-contract=fast` on 320 TUs, 44 guest-FP** — and `-Ofast` on 69, all vendored oboe |
| **azahar** | RelWithDebInfo | **`-ffast-math` on 14 TUs, all `externals/soundtouch`**; `-fno-math-errno` on 103 |
| Cemu | Debug | **none** |
| melonDS | Debug | **none** |

**azahar's is closed for the same reason ARMSX2's `-Ofast` was**: it is confined
to a **vendored audio DSP library**, where relaxed FP is a deliberate and
appropriate choice, and it touches no guest state.

**Cemu's and melonDS's databases are `Debug`**, so their release flags are
unknown — **absence there is weak evidence.**

> **ARMSX2's `-ffp-contract=fast` on its own guest-FP sources is the only live
> instance found.**

## What is not established

- **Whether PS2 FP emulation is actually sensitive to double rounding here.** The
  PS2 FPU is not IEEE-754 — no denormals, unusual NaN handling — and PCSX2 may
  already force explicit rounding in a way contraction cannot alter. **This was
  not read.**
- **Whether the flag is deliberate.** It may be inherited from a dependency's
  CMake rather than chosen. **Not traced to its source.**
- **Whether any differential test currently fails.** **The suite was not run.**
- **The 44 files are a name match**, and several — the `Dis*` disassemblers — do
  no arithmetic at all.

## The general rule this supports

**This repo already says to verify a build flag from the emitted compile commands
rather than the CMake.** This is why: **the flag was visible only in the emitted
flags**, it appears on 320 translation units, and **its effect depends entirely on
which host the code is compiled for.**

**And it extends the differential-testing rule**: a differential test is only
meaningful if **both sides were built with the same floating-point semantics.**
That is not currently stated anywhere in this repo.

## Sources

- ARMSX2 `app/.cxx/Release/*/arm64-v8a/compile_commands.json`, read by
  `tools/emitted_flags.py`
- ARMSX2 `tests/ctest/core/recompilers/`
