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
| **A `WIN` means "decisive result", NOT "got faster".** All 57 classified: **26 are `rearch` milestones of a build-out whose premise was later refuted**, 6 are measurements, 8 are on-device bring-up. **Two are speed levers on working code, and one of those is later `CONFOUNDED`** | `research_log/20260825_0030_*` |
| **An instrument that can return zero must be proved able to return non-zero.** xenia's desktop HLE intercepts read `count=0` for weeks from a dispatch bug, and the fix **corrected an earlier research finding built on that zero** | same |
| **An undated `backfill` entry defeats recency ordering.** Register residency holds a `WIN` dated `backfill` and a `CONFOUNDED` dated 2026-07-24, and the `WIN` prints first | same |
| **State the expected signature before the run.** A run with no prediction cannot fail | `CLAUDE.md`; every `DEVICE_QUEUE.md` entry |
| **THE PROOF PACKET.** A behavioural claim carries **screenshot or video, logcat, the build or APK hash, the cvars and settings, and a reproducible path** — the result is written INTO the packet, never separated from its evidence | xenia `AGENTS.md` |
| **The fuller field list**, from the fork with the strictest reporting convention: **timestamp AND timezone; branch and UPSTREAM COMMIT; the build command; host OS and toolchain versions; APK path and checksum; device model, Android version and firmware build number; the GPU DRIVER SETTING; and the settings CHANGED FROM DEFAULTS** | eden `AGENTS.md` |
| **Record settings as a DELTA, not a dump.** "Changed from defaults" is readable; a full settings dump is not — and this project's `SettingResolver` already produces exactly that sparse set | same |
| **Name the upstream commit, so a result survives a rebase.** A number attached only to a local branch stops being reproducible the next time the fork moves | same |
| **Capture BEFORE force-stopping or clearing the log.** A classifier over an uncleared log can classify a previous run | same; `research_log/20260825_1810_*` |
| **Do not claim a game is fixed until the failing screen or route is shown working** | xenia `AGENTS.md` |
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
| **Junction reads above package, and the gap varies with load** — measured 71.9 C junction against 64.6 C silicon at moderate load, and 90.7 C junction under compile against 55.0 C idle on the SAME zone. This device has **no `skin` zone at all** | same; rpcsx `docs/arm64/thermal.md` |
| **NEVER compare a package-shaped limit against a junction maximum.** 72 C against junction is **a load detector, not a thermal bound** — junction is unremarkable to ~95-105 C. A whole scheduling default in one fork was adopted to satisfy that mistake | rpcsx `docs/arm64/thermal.md` |
| **Classify the zone before you read it.** `cpu-<cluster>-<core>` is junction; `cpuss-*` and `gpuss-*` are subsystem. A classifier matching `cpu` catches both and its `max` is always the junction | same |
| **A USB-attached power reading is a FLOOR, not fiction** | `CLAUDE.md` |
| **USE A CUMULATIVE COUNTER WHEREVER ONE EXISTS.** An instantaneous `current_now * voltage_now` on an IDLE device measured **0.300, 0.588, 1.447, 1.914, 1.961 W** — a 1.66 W spread. Differencing the cumulative `charge_counter` over the window gave **0.627, 0.629, 0.629, 0.628 W**, a 0.002 W spread. **~800x tighter**, and it costs **two adb round trips for any window length** | rpcsx `docs/arm64/instruments.md` |
| **There is no measured USB input current on this device.** `usb/current_now` is frozen at the negotiated input limit and `ucsi` reports 0, so `usb_in - battery_charge` returns **negative watts**. Nothing to subtract | same |
| **THE HARNESS IS NOT THERMALLY FREE.** Same build and game: a direct boot holds `44-57 C`; the same run through an input-macro harness climbs to **70.7 C in ten seconds and trips its own early stop**, because each sample spawns an `adb shell` walking ~50 `thermal_zone*` entries and each readiness poll takes a **1080p `screencap`**. **Treat harness temperatures as an upper bound that includes the observer** | same |
| **Sample thermals at 2 s or finer.** A 4 s sweep reported a 57.8 C peak and missed a **60.2 C spike that collapsed to 46.2 C two seconds later** | same |
| **Total system power is exact; CPU-attributed power is not available** | same |
| **`unknown[+X]` is the JIT arena, not a symbol** | `CLAUDE.md` |
| **Measure the thread, not the process** | `CLAUDE.md` |
| **Temperature proves the run happened** — but see the sensor rule | `CLAUDE.md` |
| **A negative result needs a workload that could have produced a positive one** | `CLAUDE.md` |
| **MEASURE THE LOADING PHASE, NOT GAMEPLAY, for any CPU change.** *"Every scene reachable without playing is vsync capped at 60fps with the GPU around 15% busy and 2 of 8 CPU cores in use, so nothing is saturated and per-instruction work is invisible."* Loading is genuinely CPU bound | Cemu `AGENTS.md` |
| **The false negative it caused, named:** a real 3-instructions-to-2 recompiler win measured **7922 against 7897 ticks — nothing** — because whole-process CPU during gameplay is dominated by threads spinning | same |
| **A 0.2% noise floor on a workload that cannot move is precision without sensitivity.** Check saturation before trusting a tight floor | same |
| **`simpleperf` cannot attach to a build without `<profileable android:shell="true"/>` AT ALL.** Use `/proc/<pid>/stat` fields 14+15 for any A/B spanning an older build — it works everywhere. **An instrument that cannot attach reports nothing, which reads exactly like a change that did nothing** | same |
| **When a probe returns a physically impossible number, THE PROBE is broken.** Negative watts, a 10 ns `WFE`, an `ESR` decode that is always zero — three instances. It is not a surprising result to be explained | rpcsx `docs/arm64/instruments.md` |
| **A run at any guest time scale but 100% is NOT a measurement.** Fast forward, slow motion and pause all invalidate frame time, noise floors and energy per frame | `research_log/20260825_0245_*`; `app/shell/TimeScale.kt` |

## From the number to the decision

**Every rule above protects the MEASUREMENT. None protects the DECISION**, and
rpcsx recorded a case where the number was right and the default was wrong.

Its A510 pinning A/B, both arms from a 34.7 C preflight: the ordinary scheduler
hit **71.1 C** and the thermal guard stopped it 0.7 s in; the A510 cluster
started at **53.8 C** and survived. **Settled, and reversed.** A real cold PPU
recompile with the pinning in place ran **78 modules over roughly ten minutes at
51-58 C, with the guard at 72 C** — fourteen to twenty-one degrees of headroom
unused, for ten minutes, to avoid a case **the guard already catches**.

> **A measurement can be correct and still support the wrong decision.** An A/B
> that answers "which arm is cooler" does not answer "which default is better".

**Three questions an A/B does not ask. Ask them before changing a default:**

| Question | The A510 answer |
| --- | --- |
| **How often does the bad case arise?** | rarely — one cold boot |
| **What does the defence cost when it does not arise?** | **ten minutes at half the thermal headroom, every time** |
| **Is something already handling it?** | **yes, the guard, and it costs nothing until it fires** |

> **Pre-emptively throttling every run to avoid occasionally reaching a limit
> spends a large certain cost against a small uncertain one.**

**And two rules for when a lever reads FLAT:**

| Rule | Why |
| --- | --- |
| **When a throttle is removed and nothing gets faster, look for the SECOND throttle before doubting the first** | rpcsx freed the CPU affinity mask while `Max LLVM Compile Threads` still capped compilation at 2. **Two limiters in series are common because each was added for a different reason at a different time** |
| **Check who else writes the setting** | that cap was set in **three** places, and two overwrote the config file on every boot. `tools/bug_class_sweep.py --class setting_written_by_multiple_writers` |

**The second is `emitted_flags.py`'s rule for runtime settings**: a setting that
exists is not a setting that applies, and the mechanism here is **a later
writer**, not a wrong default.

`research_log/20260824_2320_a_correct_measurement_can_support_the_wrong_decision.md`

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

### A SECOND vocabulary, for symptoms rather than numbers

**Every verdict above is about a NUMBER.** Vita3K's are about a **SYMPTOM**, and
a project that ships a product needs both:

`fixed`, `improved`, `unchanged`, `worse`, **`mixed-supports-involvement`**,
`contaminated-inconclusive`.

> **`fixed` has a definition**: the original symptom gone, **no obvious
> neighbouring-scene or game regression**, **no stale debug toggles**, and proof
> on every platform the change affects.

**`mixed-supports-involvement` is the one to take outright.** When an experiment
improves the visible output **and damages adjacent geometry**, it is not a fix
and not a failure — **and the next hypothesis must explain both.**

## Before the next experiment, not just before the run

| Rule | Stated in |
| --- | --- |
| **THE ANTI-LOOP GATE.** Write a preflight summary — commit, platform, scene, props, matching prior attempts, what they proved, and **the one reason this run is genuinely new. "If the only difference is 'try it again', stop and choose instrumentation or documentation instead."** | Vita3K `AGENTS.md` |
| **A query you run and do not write down does not stop the next session repeating it.** The preflight summary is what makes "query the ledger first" durable | same |
| **A PLANNED ATTEMPT IS A BLOCKER.** Resolve it before starting a nearby one, *"otherwise the ledger becomes a pile of half-memory"* | same |
| **An active-case LOCK.** Do not let the newest `tmp/` folder, screenshot mtime or stale planned attempt choose what is being worked on | same |
| **Clear and record debug props before every comparison**, then name the intentional ones in the result | same |
| **A disable value can be a valid PREFIX.** `0`, `false`, `off` mean disabled — **check for them before prefix-matching a hash**, or a stale `setprop ... 0` matches every hash beginning with `0` and contaminates the rest of the session | same |
| **Do not write code to fix a ghost frame.** If an old screenshot shows a symptom but the APK, config or props have changed, **retest at defaults in the same session first** | same |
| **Leave the workspace knowable**: current props, whether the installed APK matches the tree, which patches are diagnostic only, and the next single hypothesis. **"Do not leave mystery toggles active for the next agent."** | same |

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
| `tools/capability_probe.py` | which fork has this capability, asked several ways. **`--self-test` proves the search can return non-zero for all nine forks, and zero for a nonsense pattern. Run it before reporting any absence** |
| `tools/bug_class_sweep.py` | where else does a bug class that already paid appear |
| `tools/vk_capability_census.py` | what does each fork ask the GPU for |
| `tools/fleet_docs_index.py` | what has the fleet already written, and what failed |
| `tools/hle_coverage.py` | what does each backend implement |
| `tools/fleet_lint.py` | build-configuration drift |
| **`tools/emitted_flags.py`** | **what flags actually reached the compiler**, from real build output |
| **`tools/target_check.py`** | **does the compile target emit what it claims** — 4 probes, proven against 4 real traps |
| **`tools/dead_guard.py`** | **is this `#if` on a macro nothing defines** — a branch that compiles cleanly and can never run. Definitions come from `compile_commands.json`, not build files. `--self-test` is two-sided on the real case |
