# Three weak-memory shapes added as classes, and a better statement of this repo's own rule

**Goal: `memory-model.md` sweeps for weak-memory defects by SHAPE across a whole
tree. Take the shapes, and take the reason the method works.**

## The methodological finding, which is the larger half

rpcsx's closing paragraph is **a sharper statement of this repo's most repeated
failure than this repo has ever managed**:

> *"Its earlier claim that `lv2` and the HLE modules were architecture-neutral
> rested on a grep across **two directories that do not exist in this fork**, and
> scanning the real one turned up both a live race and a second x86-only power
> path. **The difference is not diligence, it is that a shape-defined search
> cannot silently cover nothing: if the pattern matches zero files, that is a
> fact about the pattern, which is checkable, rather than a fact about a path
> list, which is not.**"*

> **A path-scoped search that covers nothing looks identical to one that found
> nothing.**

**`CLAUDE.md` has eighteen corrected absolute negatives and a rule saying to name
the method. This says WHY naming the method works**, and it distinguishes two
kinds of search that this repo has been treating as one.

**And it audits my own tools.** `bug_class_sweep.py` and `dead_guard.py` search
by pattern over globs — shape-scoped, and safe. **`capability_probe.py`,
`hle_coverage.py` and `vk_capability_census.py` are path-scoped**: a wrong fork
path returns zero silently. **That is exactly why `capability_probe.py
--self-test` exists**, and it is now clear the self-test is not belt-and-braces
but the thing that makes a path-scoped tool usable at all.

> **CLOSED THE SAME SESSION.** Both remaining tools now carry `--self-test` with
> positive controls and a floor below the measured value. The census sees
> **42 / 35 / 35 / 27 / 13 / 13 / 9** extensions per fork; the HLE collectors see
> **Vita3K 7,377** — matching the number `CLAUDE.md` already carried — plus
> **eden 4,986** and **Cemu 470**, which it did not. **Both failure texts refuse
> the conclusion rather than reporting a miss:** *do not report an extension as
> unrequested, or a function as unimplemented, until these pass.*

## The three shapes, added as classes

| Class | Signature | rpcsx's own result |
| --- | --- | --- |
| **`publish_then_flag`** | two **adjacent** relaxed stores to different atomics | 2 sites: one benign, **one real inversion — wrong on x86 too** |
| **`validated_reread`** | the same accessor loaded twice with work between, then compared | 2 sites: one already fixed with an acquire fence, **one safe by construction** |
| **`double_check`** | test, take a lock, test again | **zero, and the zero is STRUCTURAL** |

### Tightening, because the first cut was unusable

**`publish_then_flag` matched a single relaxed store** and returned **58 hits in
ARMSX2 alone.** The signature is a **pair**. With `near` at a 3-line window it
drops to **10**, and across the fleet to single digits everywhere but one fork.

**`double_check` matched every `if (!initialized)`** — 37 hits in eden. Requiring
a **lock within four lines** brings it to 1-4 in most forks.

**First hit read, and it dismisses cleanly**: Cemu's
`PPCRecompilerProfiler.cpp:181`, `entry.hits.store(0, relaxed)` — a profiler
counter reset. **That is exactly the benign case rpcsx names**: *"descriptive
fields in a diagnostic bucket, last-writer-wins, consumed by a stats dump."*

## Two rules that travel with the shapes

**1. The order within the match decides the verdict.**

> *"Two sites that pattern-match identically require **opposite treatment**, and
> the deciding factor is **which of the two reads comes first**."*

**Version-then-data is safe** when the version load is an acquire, because the
data read cannot move above it. **Data-then-version is the trap**, because the
data read can sink below the validating counter read.

**This repo's sweep tool prints "a hit is a shape, not a diagnosis" on every
run.** This is sharper: **for this class a hit is not even a direction.**

**2. A zero that is EXPLAINED is worth more than a zero that is reported.**

`double_check` found **zero hand-rolled instances** in rpcsx, *"and the reason is
structural rather than lucky"*: lazy initialisation there uses **function-local
statics — 413 across 25 files** — which C++11 requires to be thread-safe and for
which the compiler emits a guard with correct acquire/release semantics.

> **There was nothing to get wrong, and that is a different statement from
> "nobody got it wrong".** The class text now says so, and tells the next reader
> to check for that idiom before believing a zero.

## Status of the results, stated plainly

**The hits are UNREVIEWED apart from the one sample above.** Weak-memory
correctness needs the surrounding code read, and these counts are a starting
list, not findings:

| Class | ARMSX2 | xenia | Cemu | azahar | melonDS | Vita3K | eden |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `publish_then_flag` | 10 | 7 | 1 | 0 | 5 | 8 | 2 |
| `validated_reread` | 1 | 0 | 1 | 0 | 0 | 0 | 0 |
| `double_check` | 0 | 0 | 3 | 1 | 4 | 1 | 2 |

**No claim is made about any of them.** What has been established is that the
shapes are now sweepable here at a size a person can read.

## Limits

- **The patterns are approximations of shapes rpcsx describes precisely.** Its
  sweep presumably used better tooling than a line-based grep with a window.
- **`validated_reread`'s pattern is the weakest of the three** — it keys on
  vocabulary (`seqlock`, `version`, `rtime`) rather than on structure, and will
  miss a seqlock nobody named.
- **`double_check`'s counts shifted when the pattern moved into `near`**, so the
  before-and-after numbers are not strictly comparable.
- **Nothing here is a defect claim.**

## Files

- `tools/bug_class_sweep.py` — three classes, each with `near` and a window
- `CLAUDE.md` — the shape-versus-path rule, beside the corrected negatives
