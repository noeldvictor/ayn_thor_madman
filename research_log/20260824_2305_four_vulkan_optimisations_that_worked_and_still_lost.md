# Four Vulkan optimisations that hit their target and still lost, and a pre-flight check this project does not run

**Goal: read azahar's rejected-Vulkan cluster, because every one of them is in a
subsystem the shared device layer plans to own.**

**All four were measured on the physical Thor by that fork. No device used
here.**

## The four

| Rejected | Its stated target | What happened to it | The complete path |
| --- | --- | --- | --- |
| **one `StreamBuffer` reservation** for vertex + fixed attributes | `Map()`/`Commit()` pairs | **cycles down 18.64% / 15.27%** | **`SetupVertexArray()` +2.41% REGRESSION**, share +3.55% |
| **one-entry texture-descriptor cache** | Turnip descriptor update/bind calls | **driver leaves down 38.53% / 15.74%**, direct-cost share **down 23.16%** | `AccelerateDrawBatch()` **-0.37% raw**, total sampled work **+0.70%**, `SyncTextureUnits()` **+3.79%** |
| **`VK_EXT_extended_dynamic_state3` blending** | `tu_CmdBindPipeline` work | **1.25297% against 1.25021% — noise** | azahar's own `BindPipeline` share **+9.17% relative** |
| **bypass the pipeline map lookup** from a remembered match | `PipelineInfo::Hash()` + set lookup | — | **crashed Turnip in `tu_cmd_render<chip7>`, null dereference** |

## The pattern, which this repo has not stated

> **Three of the four DID what they set out to do. The metric they targeted moved
> in the right direction, by large margins. The complete path got slower
> anyway.**

**This repo has the adjacent rules and not this one.** It has *"use inflation to
choose which subsystem to attack, do not use it to judge a peephole"*, xenia's
prolog that went **18 instructions to 13 and measured slower**, and azahar's own
*"instruction count is candidate guidance, not sufficient acceptance evidence"*.

**Those are all about a PROXY that failed to predict.** These are different and
worse:

> **The optimisation succeeded at its stated target and lost. Removing work from
> a helper moved that work somewhere else, or paid for it in a way the helper's
> own counter could not see.**

**azahar's own wording is the rule:** *"beats the complete recurring path, **not
just its helper-call count**."*

**And it is a direct warning to the shared device layer.** Every one of these
four is a change somebody writing a shared Vulkan device would propose on sight:
fewer buffer reservations, a descriptor cache, dynamic state instead of pipeline
keys, a fast path around a hash lookup. **All four are already measured and
rejected on this device.**

## The pre-flight check: a win cannot exceed what the function costs

**The fourth rejection carries a number that settles the experiment before it
starts:**

> *"`ShaderDiskCache::GetPipeline()` remained **below 1% of process work**, while
> control/candidate **trace variance was larger than the entire function**."*

**So the maximum achievable win was under 1%, and the measurement noise was
larger than that.** No implementation, however good, could have produced a
readable result.

> **Compute the ceiling before building: the function's total share of process
> work IS the maximum win. If that ceiling sits below the workload's noise floor,
> the experiment cannot produce a result and should not be run.**

**This project has the two halves and not the product.** It records the noise
floors — **0.2% gated title screen, ~5% savestate, ~50% cutscene** — and it
records *"a claim smaller than the floor of its workload is not a result"*. **It
has never said to check the ceiling against that floor before building.**

**It is the same family as xenia's `EOR3` applicability count** — *0 of 1 fusable
candidates*, one cvar instead of a three-input opcode — **but on a different
axis. Applicability asks whether the pattern occurs. The ceiling asks whether the
site is big enough to matter even if it does.**

**Both are answerable with a profile and no build.**

## Two more things worth keeping

**A repetition rate, measured rather than assumed.** The safer variant of the
pipeline shortcut kept the required map lookup and was still rejected because
**only 153,847 of 300,000 live queries — 51.28% — repeated the prior static
state.** A one-entry cache is worth building at 95% repetition and not at 51%.
**That number is cheap to collect and decides the design.**

**A correctness failure that focused tests passed.** The pipeline-lookup bypass
*"passed focused static/dynamic-state tests but crashed the physical Thor's
Turnip worker in `tu_cmd_render<chip7>` with a null dereference"* — and the
safer variant *"passed 53 assertions in six physical-device Vulkan cases"* and
was still rejected on measurement.

> **Passing the tests you wrote for a change is not evidence the change is
> right**, which is this repo's differential-testing gap seen from the Vulkan
> side.

## Ledger queried: one row confirmed by a second fork, with the mechanism

**`exp_ledger.py check` in xenia**, for `descriptor`, `stream buffer`,
`dynamic state` and `pipeline`:

**`gpu_dynamic_blend_state` is `DEAD` in xenia, independently.** Its hypothesis
was that EDS3 dynamic blend would collapse its 208 pipeline binds. Result:
**"pipeline_binds stayed EXACTLY 208 — blend is not a variant driver, binds are
shader/render_pass; render pixel-perfect."**

> **azahar measured that it did not help. xenia measured WHY it could not.**
> Blend is not what drives pipeline variants, so removing it from the key cannot
> reduce binds. **Two forks, two instruments, one conclusion.**

**`stream buffer` and `dynamic state` return zero matches**, so azahar's first
and third rejections are new to xenia. **`descriptor` returns four, none a
descriptor cache** — the near one is `bindless`, `DEAD`, which **regressed
129 ms to 161 ms** while descriptor binds stayed at ~1074, *"because they are
per-draw CONSTANTS not textures"*.

**That is a third instance of the pattern above**, from a third direction: the
count the change targeted did not move, because the change had misidentified what
produced it.

## Where this lands

- **`shared_layer/OWNED.md`, the Vulkan device layer candidate.** These four are
  a do-not-retry list for the extraction, and they are measured on the target
  device. **The shared layer inherits them.**
- **`MEASUREMENT.md`**, for the ceiling check.
- **The extended-dynamic-state3 row is already recorded here**; the other three
  were not.

## Limits

- **Every figure is azahar's, on Super Mario 3D Land, on its Thor.** Nothing
  reproduced, no device used here.
- **These are rejections of specific implementations.** Each entry explicitly
  leaves room for *"a materially different implementation"* or *"a ranked title
  proving higher repetition"*. **They are not proofs that the idea is
  unreachable.**
- **The 51.28% repetition rate is one title and one scene**, and azahar says so
  by asking for a ranked title.
- **No claim that the shared device layer would hit the same results** — a
  different structure could move the complete path differently. **What transfers
  is that the obvious version was tried here and lost.**

## Sources

- azahar `AGENTS.md:516-547`
- `research_log/20260824_2220_four_textbook_arm64_fusions_measured_and_rejected.md`
- `shared_layer/MEASUREMENT.md`
