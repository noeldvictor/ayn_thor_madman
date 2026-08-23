# The device queue

**Everything waiting on the Thor, with what each run should show if it works.**

**A run with no prediction cannot fail.** Every entry states its expected
signature before it is run, per the rule in `CLAUDE.md`. An entry with no
prediction is not ready to run.

There is one physical Thor, so device work is a queue and analysis is not.
Ordered by what unblocks the most.

---

## Before any run

| Gate | Why |
| --- | --- |
| `adb -s "$THOR"`, never bare `adb` | a Quest 2 also answers adb on this box |
| verify `ro.product.model` is `AYN Thor` | a wrong `-s` flashes the Quest |
| **`status=Discharging`** | plugged in, `current_now` flips sign. **Any wattage from a USB session is fiction** |
| check the **60 Hz cap** | `PRIORITY_USER_SETTING_PEAK_REFRESH_RATE` votes 60. Both panels do 120. Pacing runs measure the setting otherwise |
| record battery level and charge state | |
| **15 minutes or more** when heat matters | thermal behaviour settles over minutes |
| **temperature must rise** | no heating means an idle or menu scene, so the run is invalid whatever the counter said |
| query the experiment ledger first | `python tools/exp_ledger.py check "<keyword>"` in xenia-thor |

---

## 1. Close the SVE question

**One command, and it settles a claim in three documents.**

```sh
zcat /proc/config.gz | grep -i ARM64_SVE
```

| Result | Conclusion |
| --- | --- |
| `CONFIG_ARM64_SVE=y` and no `sve` in cpuinfo | **fused off in silicon.** Nothing can reach it |
| not set | **a kernel choice.** A custom kernel could expose it |

**Prediction: not set.** Qualcomm disabled SVE across this Snapdragon
generation, so the kernel most likely never enabled the support either.

**It changes no decision either way** — 128-bit SVE is the same width as NEON.
It is recorded because the repo stated the wrong version of it for three
commits. See
[`research_log/20260822_2147_sve2_on_the_thor.md`](research_log/20260822_2147_sve2_on_the_thor.md).

## 2. Pin the Turnip driver

Three candidates already on the device: `turnip_mrpurple_T30-toasted`,
`mesa-turnip-v26.3.0-20260803-r7`, `Turnip_v26.0.0_R8`.

**Provisional pin is T30 and it was chosen from a changelog, not a measurement.**

Run an **in-place alternating A/B inside one run**, on a busy frame. Cross-run
comparison is untrustworthy because scene complexity swings several times a
second.

**Prediction: differences under 5% on frame time, with the spread between
builds smaller than the spread between scenes.** If that holds, **pin the one
with the fewest rendering faults rather than the fastest**, and record the
result as `FLAT` rather than promoting a winner.

State watts and temperature, not only frames.

## 3. The app shell on both displays

The shell builds and installs. **Four behaviours were fixed blind** and none is
verified. See
[`work_log/20260822_2158_shell_second_screen_lifecycle.md`](work_log/20260822_2158_shell_second_screen_lifecycle.md).

| Check | Prediction |
| --- | --- |
| background the app | Screen-2 panel **disappears**; previously it stayed with stale content |
| move the activity between panels | the panel **re-targets** to the other display |
| after re-attach | Screen-2 shows current content, **not blank** |
| add or remove a display | **the listener may never fire.** Both panels are internal |

**If the last one never fires, delete the listener** rather than keeping it for
symmetry.

## 4. Does the compile target do anything — **ANSWERED, no device needed**

**Removed from the queue 2026-08-23.** xenia already ran it, by disassembling
its own binary rather than benchmarking: enabling the features made clang emit
**zero** of them. **The flags are permission for hand-written intrinsics, not a
codegen win.** See
[`research_log/20260823_0150_target_features_are_permission.md`](research_log/20260823_0150_target_features_are_permission.md).

**The lesson for this queue: ask whether a question needs the device at all.**
"Does the compiler emit X" is answered by disassembly — deterministically, with
no scene noise. Only "is it faster" needs the Thor.

### The original entry, kept for the record

`-march=armv8.2-a+...+dotprod+sha3 -mtune=cortex-x3` against the fork's current
baseline, same commit, same scene.

**Prediction: no measurable change from `-mtune` alone.** The honest prior is
that scheduling flags rarely move a real workload. **`+dotprod` and `+sha3` will
also do nothing until an emitter actually emits them** — ARMSX2 and melonDS emit
neither today, so this measures the compiler's own use of them and little else.

**Expect `FLAT`.** Record it as such; a flat result stops the question being
re-argued.

## 5. Thread placement

Two forks set no host affinity at all: **melonDS and Vita3K**. melonDS tunes its
codegen for the X3 and never asks for the X3.

Pin the hot guest thread to the prime core, leave the process mask alone.

**Prediction: a real gain on melonDS or Vita3K**, because they currently express
no preference and xenia measured guest threads landing on the 2.0 GHz A510s
while the 3.2 GHz X3 idled. **This is the most likely win in the queue.**

Watch for the opposite on a fork that already places threads — a second opinion
about placement is worse than one.

## 6. Frame pacing

**No fork uses Swappy or `VK_GOOGLE_display_timing`.** Every one selects a
present mode and stops.

Compare plain `FIFO` against Swappy on a backend that cannot hold 60.

**Prediction: no change in average fps and a large change in frame-time
variance.** That is the whole point — FIFO alternating 16.6 and 33.3 ms is
judder, and a stable 33.3 looks better than a faster average.

**So average fps is the wrong metric here.** Record 1% low and frame-time
standard deviation, or the run will read as `FLAT` while being a win.

## 7. Render pass attachment ops

Four forks give four answers, and the best is not in the newest fork. Vita3K
tracks transient attachments and uses `DontCare` both ways; eden loads and
stores unconditionally.

Change eden's depth ops to `DONT_CARE` where the pass does not need them, as
Cemu already does.

**Prediction: a bandwidth reduction visible in GPU counters, and possibly
nothing in frame time**, because the win is memory traffic and thermal headroom
rather than throughput. **Measure watts.**

## 8. Anime4K's two dedicated passes

azahar gives Anime4K two full-screen render passes, `anime4k_xy_renderpass` and
`anime4k_luma_renderpass`. **On a tiler each is a load and a store of the whole
target unless carefully arranged, and nobody has priced it.**

**Prediction: a measurable per-frame cost proportional to output resolution**,
which would make the flagship feature more expensive on the larger panel than
anybody has assumed.

## 9. Native game reference capture

Capture a well-optimised native game with Perfetto and Snapdragon Profiler:
pass count, resolves, GMEM residency, vertex against fragment split, bandwidth,
watts. Then capture one backend on a comparable scene.

**Prediction: the emulator shows several times the render passes and resolves.**
The gap is structural, not CPU — a faithful emulator inherits a rendering
structure designed for hardware with the opposite tradeoffs.

**The difference between those two captures is the actual roadmap.**

---

## Not ready to run

These need a decision or a build first, not device time.

- **Any shared-layer measurement.** Nothing is extracted yet.
- **Savestate fixture regeneration.** Needs the harness to exist.
- **ADPF tuning.** Measure without it first, or the hint is tuned against an
  unknown baseline. ADPF is currently disabled on this device by persisted
  config.
