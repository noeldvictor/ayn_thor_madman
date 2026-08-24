# Measure the loading phase, not gameplay — with a named false negative, and an LLVM rejection worth keeping

**Goal: read the rest of Cemu's index. Two sections, and both change rules
here.**

## 1. The workload trap, with a real win that measured as nothing

> **"Measure on the loading phase, not gameplay.** Every scene reachable without
> playing is **vsync capped at 60fps with the GPU around 15% busy and 2 of 8 CPU
> cores in use**, so nothing is saturated and **per-instruction work is
> invisible**. Loading is genuinely CPU bound — roughly half the samples land in
> recompiled guest code — which makes it **the only workload here that can
> detect a codegen change.**"

**And the false negative it caused, named:**

> **"Whole-process CPU during gameplay is useless as a metric — it is dominated
> by threads spinning, so it barely moves no matter what you fix. That is why a
> real 3-instructions-to-2 recompiler win once measured as 7922 vs 7897 ticks,
> i.e. nothing."**

**`MEASUREMENT.md` already carries "is it capped?" and "a negative result needs a
workload that could have produced a positive one".** Those are the right rules
and they were abstract. **This is the concrete instance: a genuine improvement,
measured correctly, reported as zero, because the workload could not express
it.**

**The specific diagnosis generalises past Cemu.** *Everything reachable without
playing is vsync-capped.* **That is true of every backend in this fleet**, and it
means **the default measurement route — boot, reach a menu, sample — is
structurally incapable of detecting a CPU change.** This project's own noise-floor
table says a gated title screen has a 0.2% floor; **a 0.2% floor on a workload
that cannot move is precision without sensitivity.**

## 2. A tooling trap that is mechanism 11 again

> **"Builds from before 2026-08-21 have no `<profileable android:shell="true"/>`,
> so simpleperf CANNOT ATTACH TO THEM AT ALL.** Use `/proc/<pid>/stat` (fields
> 14+15, utime+stime) for any A/B involving an older build; it works
> everywhere."

**An instrument that cannot attach reports nothing, which reads exactly like a
change that did nothing.** That is `DID_IT_APPLY.md` mechanism 11, in the
profiler rather than the emulator — **and the fallback is a counter that is
always present**, which is the cumulative-counter rule from rpcsx's
`instruments.md` reached from a different direction.

## 3. The measured baseline, and it is a clean one

A/B of two commits, three runs each, same device, same game, same phase:

| | baseline | after | |
| --- | --- | --- | --- |
| CPU over 20 s of loading | 3583 / 3619 / 3641 ticks | 2526 / 2549 / 2489 | **−30.2%** |
| startup, Init → Run title | 3.759 / 3.741 / 3.761 s | **0.855 s ×3** | **4.4x** |

> **"The ranges do not overlap — the baseline's best run is above the optimised
> build's worst — and variance is under 1.5% either side, so this is signal."**

**That is this repo's own reporting standard met exactly**: report `[min..max]`,
never n=1, and state non-overlap rather than a mean.

## 4. The LLVM rejection, recorded so it is not re-proposed

**Five reasons, and the first is the one that matters most here:**

> **"~40% of emulator CPU in host-side overhead — clock reads, `OSGetTime`,
> mutexes, atomics — against 40.7% in the JIT'd guest code. A 20% codegen win
> would buy ~8% overall while that 40% sits untouched."**

**Host-side overhead is the same size as guest execution.** That is the strongest
argument in the fleet for this project's own shape — **the shared layer takes the
host side** — and it is the number to quote when somebody proposes a recompiler
project.

**The rest, compressed:**

- **"RPCS3's ARM64 gains came from FIGHTING LLVM, not from having it."** Their
  shipped wins were instruction-selection fixes; one PR exists *because* LLVM
  emitted two junk instructions, and they filed an LLVM bug and shipped a
  workaround meanwhile. **With a hand-written emitter, two equivalent fixes took
  minutes.**
- **The guest is wrong for it.** Espresso is 32-bit PowerPC with paired singles
  and no vector unit, so translation is near 1:1. **The hot-block profile is
  diffuse — top 200 of 24,221 blocks is 46%** — which favours broad
  instruction-selection wins rather than cross-block optimisation.
- **The compile-time model does not fit**, and this is the part that connects to
  something already in this repo.

> **"Cemu recompiles per function on demand and has NO PERSISTENT RECOMPILER
> CACHE, so LLVM would mean multi-minute loads or heavy in-game stutter.
> Building that cache is the MORE VALUABLE PROJECT and would be the prerequisite
> anyway."**

**That is a third independent arrival at the PERSIST operation.** ARMSX2 built a
persisted VU JIT with tests; eden declared a `PatchCacheKey` and never used it;
**Cemu names building the cache as more valuable than the thing it would
enable.** **Three forks, three postures, one conclusion.**

**And the revisit condition is stated**, which is what makes a rejection
reusable: *"Revisit only if a profile shows guest code quality dominating with
large hot blocks."*

## Limits

- **All numbers are Cemu's**, one title, one device, its own commits. Nothing
  reproduced here.
- **The 40% / 40.7% split is one profile** and the document does not say which
  phase it was taken in — **which matters, given section 1.**
- **"Everything reachable without playing is vsync-capped" is Cemu's
  observation.** It is plausible fleet-wide and is not verified for other
  backends.
- **No device used.**

## Sources

- Cemu `AGENTS.md:155-243`
- `shared_layer/MEASUREMENT.md`, `shared_layer/DID_IT_APPLY.md`
