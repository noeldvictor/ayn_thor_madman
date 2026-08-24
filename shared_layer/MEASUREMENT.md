# The measurement rules, indexed

**This is an index, not a copy.** Every rule is stated in full somewhere else,
and this file says which rules exist and where to read them. **Two copies of a
map disagree**, so nothing here restates a rule's evidence.

**Why it exists.** `CLAUDE.md` grew from 3,935 to 6,305 lines on 2026-08-24, and
most of the growth is measurement discipline mined from the fleet. The rules are
correct and the list had become unnavigable.

## Before the run

| Rule | Stated in |
| --- | --- |
| Query the experiment ledger first. **177 entries: 75 `OPEN`, 57 `WIN`, 32 `DEAD`** | `CLAUDE.md`, device rules |
| **State the expected signature before the run.** A run with no prediction cannot fail | `CLAUDE.md`; every `DEVICE_QUEUE.md` entry |
| **Name the confound that would fake a win** | `research_log/20260824_0810_*` |
| **Measure applicability before building a transform.** `EOR3` died on 0 of 1 candidates | `CLAUDE.md`; `research_log/20260824_0520_*` |
| **Ask whether the code is executed** before asking for a better instruction | `research_log/20260824_0730_*` |
| **Read the persisted config**, not the compiled default | `research_log/20260824_0645_*` |
| **Verify a build flag from the emitted compile commands**, not the CMake | `research_log/20260824_1500_*` |
| **Check which launch path sets a default** | same |

## Choosing the instrument

| Rule | Stated in |
| --- | --- |
| **Sampling profiler, not instrumented counters**, for "where does the time go" | `research_log/20260824_0810_*` |
| **Measure the frame anatomy before designing a render path** | `CLAUDE.md`; `shared_layer/THOR_RENDER.md` banner |
| **Per-stage GPU split is available headlessly**, no root, no GUI — **needs a debuggable app** | `CLAUDE.md`; xenia `xenia-thor-adb-gpu-stage-split` |
| **KGSL ftrace is dead headless** — shell is uid 2000, no `su` | same |
| **`/sys/class/kgsl/kgsl-3d0/gpubusy`** — cumulative, resets on read | `research_log/20260824_0855_*` |
| **Classify by operand provenance**, then cross-check with a control instruction | `research_log/20260824_1015_*` |
| **`git grep` does not see submodules.** Pass `--recurse-submodules` | `CLAUDE.md`, "Read before you claim" |
| **State whether vendored code counts** — it depends on the question | same |

## Making arms comparable

| Rule | Stated in |
| --- | --- |
| **Normalise by something the change does not touch, and the scene may vary.** 59% spread became 1.1% | `CLAUDE.md`; `research_log/20260824_0255_*` |
| Cross-run comparison is untrustworthy; prefer in-place A/B/A | `CLAUDE.md` |
| **Each arm needs a fresh process** — device properties cache per process | `CLAUDE.md` |
| Run a harness from a frozen copy | `CLAUDE.md` |
| **Three backends have no deterministic scene** — Cemu, eden, and xenia's deadlocks | `research_log/20260824_0255_*` |

## Reading the numbers

| Rule | Stated in |
| --- | --- |
| **Noise floors: 0.2% gated title screen, 5% savestate, ~50% cutscenes** | `CLAUDE.md` |
| **Never quote n=1.** Report `[min..max]`, not the mean | `CLAUDE.md` |
| **Energy per frame, not watts** — watts alone rewards a slower core | `CLAUDE.md` |
| **Say which thermal sensor. Never `max` over all zones** | `CLAUDE.md`; `research_log/20260824_1050_*` |
| **Junction reads ~30 C above package**, and this device has no `skin` zone | same |
| **A USB-attached power reading is a FLOOR, not fiction** | `CLAUDE.md` |
| **Total system power is exact; CPU-attributed power is not available** | same |
| **`unknown[+X]` is the JIT arena, not a symbol** | `CLAUDE.md` |
| **Measure the thread, not the process** | `CLAUDE.md` |
| **Temperature proves the run happened** — but see the sensor rule | `CLAUDE.md` |
| **A negative result needs a workload that could have produced a positive one** | `CLAUDE.md` |

## Stacking, and the conflict

| Rule | Stated in |
| --- | --- |
| **rpcsx: one component per proof run**, each individually clean first — protects **attribution** | `CLAUDE.md` |
| **xenia: measure the COMPOUND, never one layer** — protects **detection** | `CLAUDE.md`; `research_log/20260824_0410_*` |
| **Decide which regime you are in, and say which rule you used** | same |
| Do not add deltas arithmetically | `CLAUDE.md` |
| A newest failure outranks an older success | `CLAUDE.md` |

## Verdicts

`DEAD`, `FLAT`, `WIN`, `GFX-LOSS`, `CONFOUNDED`, `OPEN`, plus **`migration-credit`**
(structurally right, not yet faster) and **`route-miss`** (clean capture, wrong
state). Stated in `CLAUDE.md`.

## The standing prior

**Fourteen manual-derived predictions have been measured in this fleet and
fourteen were refuted.** And the pattern behind them:

> **The wins have come from code that was broken, not code that was slow.**

`research_log/20260824_0855_every_win_was_a_bug.md`, and `tools/bug_class_sweep.py`
turns it into a re-runnable sweep.

## The tools

| Tool | Answers |
| --- | --- |
| `tools/supervise.py` | is this session repeating a known failure |
| `tools/capability_probe.py` | which fork has this capability, asked several ways |
| `tools/bug_class_sweep.py` | where else does a bug class that already paid appear |
| `tools/vk_capability_census.py` | what does each fork ask the GPU for |
| `tools/fleet_docs_index.py` | what has the fleet already written, and what failed |
| `tools/hle_coverage.py` | what does each backend implement |
| `tools/fleet_lint.py` | build-configuration drift |
| **`tools/emitted_flags.py`** | **what flags actually reached the compiler**, from real build output |
