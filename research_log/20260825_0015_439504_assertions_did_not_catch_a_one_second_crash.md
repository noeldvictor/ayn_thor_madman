# 439,504 assertions did not catch a one-second crash, and the total and the component must agree

**Goal: read azahar's rejected CPU and kernel cluster, from its 2026-08-20
profile. Five rejections, and they carry a different lesson from the ARM64 and
Vulkan clusters already read.**

**All measured by that fork on the physical Thor. No device used here.**

## 1. The headline: a test suite that proves almost nothing

**Deleting `ready_queue.remove()` from `ThreadManager::SwitchContext`:**

> **"The unconditional deletion passed 439,504 of 439,505 assertions in the broad
> ARM64 `[core]` suite** (the sole failure was the Android harness-only missing
> `get_build_flavor` function) **yet crashed 7th Dragon within one second with
> `Thread must be ready to become running`.**"

**The cause is a single special case in one branch of one function.**
`PopNextReadyThread` normally pops the selected thread — but **its
no-better-thread branch returns the CURRENT RUNNING thread without popping it.**
`SwitchContext` then requeues that thread before loading its context, **so it
must remove the self-selected thread again.**

> **Four hundred thousand assertions did not exercise the branch where a thread
> is selected as its own successor.**

**This repo has "compiling is not running" and a recorded differential-testing
gap.** This is the sharpest evidence for both, and it is worse than either
states: **the suite was not thin. It was large, it was passing, and it was blind
to the case that mattered.**

**The transferable rule is about coverage shape, not coverage count.** A
scheduler's dangerous states are the degenerate ones — one runnable thread, the
running thread selected again, an empty queue — **and they are exactly the states
a randomised or workload-derived suite under-samples.**

## 2. The total and the component must AGREE, and this is a new rule

**Two rejections in this cluster fail in OPPOSITE directions, and together they
close a gap in this project's measurement rules.**

| Case | The component | The total | Verdict |
| --- | --- | --- | --- |
| **`FlushRegion` early return** | self share **ROSE** 1.03% -> 1.12% | looked **0.5% LOWER** | **rejected — the total was noise** |
| the four Vulkan rejections read earlier | improved by **15-38%** | **regressed** | **rejected — the component moved, the path did not** |

> **A total that improves while its component worsens is noise. A component that
> improves while the total worsens is displaced work. Neither number is
> sufficient alone, and when they disagree the smaller signal is usually the
> artefact.**

**`MEASUREMENT.md` has the ceiling check, the noise floors, and normalisation.
It did not say to require agreement between the two levels.**

## 3. "It merely moved cost" — and the only way to see it is to profile the callers

**Moving the thread-wakeup `UnscheduleEvent` scan out of `SwitchContext`:**

- `UnscheduleEvent` **0.33% -> 0.08%** — a 76% reduction in the target.
- **`ResumeFromWait` 0.14% -> 0.24%. `ThreadWakeupCallback` 0.16% -> 0.22%.**
- Whole-app task-clock, cycles and instructions regressed **0.510%, 0.532%,
  0.883%.**

**azahar's own words: *"it merely moved cost"*.**

> **A change that relocates work looks like a large win at the site it left.**
> **The only instrument that sees it is a profile that includes the destination**,
> and the destination is usually a function nobody thought to watch.

**It also passed all 85 assertions in four ARM64 `CoreTiming` tests** — the same
lesson as item 1 in miniature.

## 4. An O(1) index can lose to an O(n) scan

**A maintained 64-bit nonempty-priority mask for `ThreadQueueList`**, replacing
empty-queue scans with AArch64 **`RBIT`/`CLZ`** — a genuinely good use of the
instruction set.

> **Task-clock +1.051%, cycles +1.156%, instructions +1.836%**, and
> `PopNextReadyThread` itself rose **0.65% -> 0.73%**. Implementation, tests and
> object-layout change all reverted.

**The scan happens on selection. The maintenance happens on EVERY enqueue and
dequeue.** A structure that makes the rare operation O(1) by taxing the common
ones loses when the scanned list is short.

**This is the fusion lesson in data-structure form** — and it is the fourth
independent instance of the same shape in one fork: **the thing being optimised
got better, measured, and the program got slower.**

## 5. The ceiling rule again, with a risk asymmetry attached

**An atomic fast path around `System::signal_mutex`:**

> **the ordinary no-signal lock was only about 0.05% of whole-app sampled
> cycles**, and *"changing asynchronous reset/save/load/shutdown timing for that
> cost **misses the forest**"*.

**The ceiling was 0.05%. The cost was altered concurrency semantics on reset,
save, load and shutdown.**

> **A ceiling check is not only about whether a win is measurable. It is about
> whether the risk is proportionate to the largest possible reward.** 0.05% does
> not buy a change to save-state timing under any outcome.

## What this means for the shared layer

**`CLAUDE.md` proposes one scheduler with cluster affinity, replacing per-backend
thread pools.** That scheduler is the same kind of object as `ThreadQueueList`
and `SwitchContext`.

- **Item 1 says the test suite will not protect it.** A scheduler needs its
  degenerate states enumerated deliberately, not sampled.
- **Item 4 says the obvious data-structure improvement may lose**, and the
  measurement must include enqueue and dequeue, not only selection.
- **Item 3 says the profile must cover the callers**, or displaced cost reads as
  a win.

**And these are per-backend guest schedulers, not the host scheduler**, so the
rejections do not transfer directly. **What transfers is the shape of the
mistakes**, and the shared host scheduler will be written by somebody who has not
made them yet.

## Limits

- **Every figure is azahar's, on 7th Dragon, on its Thor.** Nothing reproduced,
  no device used here.
- **These are guest-kernel HLE structures.** azahar's `ThreadQueueList` schedules
  emulated 3DS threads. The shared layer's scheduler would place host threads on
  clusters. **Different object, same class of error.**
- **Each rejection leaves room for a different implementation**, and says so.
- **The 439,505-assertion figure is the suite's own count**, not an independent
  measure of coverage. **A large assertion count is what is being criticised
  here, so it should not be quoted as a virtue either.**

## Sources

- azahar `AGENTS.md:281-320`
- `research_log/20260824_2305_four_vulkan_optimisations_that_worked_and_still_lost.md`
- `research_log/20260824_2220_four_textbook_arm64_fusions_measured_and_rejected.md`
