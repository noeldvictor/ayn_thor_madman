# Three backends have no deterministic scene, and Phase 3 assumes they all do

**Goal: check the savestate assumption under the test-harness plan.**

**`CLAUDE.md` Phase 3 builds the harness on savestate fixtures, and records eden,
Vita3K and xenia savestate code as "found by neither search". Searched properly:
three backends cannot supply a deterministic scene at all.**

## The measurement

`tools/capability_probe.py`, four probes for savestate and three for
deterministic input replay, then every doubtful hit read.

| Fork | Savestate | Input replay | Deterministic fixture route |
| --- | --- | --- | --- |
| **ARMSX2** | **4/4**, with versioning and an archive format | 2/3 | **both** |
| melonDS | 3/4 | 2/3 | both |
| azahar | 2/4 | **2/3, `core/movie.cpp`** | both |
| Vita3K | 3/4 | 0/3 | savestate only |
| rpcsx | 3/4 | 1/3 | savestate only |
| **xenia** | 1/4 — **and it deadlocks** | 2/3 | **blocked** |
| **Cemu** | **none** | **0/3** | **none** |
| **eden** | **none** | 1/3 | **none** |
| GameThor | none | none | not applicable |

## The three that fail, each for a different reason

### Cemu: no guest savestate at all

**Its only `SaveState` match is `GraphicPacksWindow2::SaveStateToConfig()`** —
saving GUI state to a config file. A second search for `savestate`,
`save_state`, `snapshot` across the fork returns the debugger, the recompiler
profiler and `LatteTexture`, none of which serialise guest state.

### eden: none, and the error code for it is dead scaffolding

**Zero hits on all four probes.** A second search found exactly one thing:
`CoreError.ErrorSavestate` in `NativeLibrary.kt`, an enum value with a string
resource behind it — **and nothing in the core ever raises it.**

**That is the second dead scaffolding type found in eden today**, after
`PatchCacheKey`, which is declared with a hash specialisation and used nowhere.

### xenia: it exists and it hangs

**Not found by a probe — found in xenia's own research file.** Its Turnip
feature-gap audit records the save-state path as blocked:

> the save-state path is itself blocked (`SaveToFile` hangs in
> `kernel_state_->Save`, a global-lock deadlock during `Pause` /
> `GetObjectsByType`). **THE META-FIX: fix the save-state hang → load the same BD
> foliage scene deterministically → unblocks the A/B for VRS *and* ROAA *and*
> bindless on BD. This is the highest-leverage GPU-validation unblock.**

**xenia already identified this as its own top blocker and it is not in this
repo's plan or queue.**

## Why this is not a small gap

**The noise floors make it decisive.** rpcsx measured the spread of re-running
one configuration:

| Workload | Spread |
| --- | --- |
| gated title screen | **+/-0.2%** |
| restored savestate | **+/-5%** |
| pressing through cutscenes | **~+/-50%, unusable** |

**Without a savestate or an input replay, a backend's only route to a scene is
pressing through cutscenes.** That is the unusable row. **A claim smaller than
its workload's floor is not a result**, so for Cemu, eden and xenia today,
almost no GPU or CPU claim can be made at all.

**And it is worse than "no measurement".** A harness that runs on five backends
and silently cannot run on three will read as a passing suite.

## What follows

**1. The paused agent loop is not a feature for these three. It is the only
route.**

`shared_layer/AGENT_LOOP.md` presents the loop as removing the fleet's worst
measurement problem. **For ARMSX2 and Vita3K that is an improvement over an
existing route. For Cemu and eden there is no existing route**, and for xenia the
existing one is deadlocked.

**That reorders its priority.** It stops being an interesting capability and
becomes the enabling condition for measuring three of the fleet's backends.

**2. Phase 3's fixture rule needs a third option.** `CLAUDE.md` says prefer a
rebuildable fixture — a ROM hash plus a recorded input replay — over a raw
savestate. **Cemu has neither.** The third option is an agent-driven route
recorded as a sequence of decisions, which is what the agent loop produces.

**3. xenia's save-state deadlock belongs in the queue.** It is a named bug with a
named location, its own fork calls fixing it the highest-leverage unblock
available, and it needs no device to attempt — it is a lock-ordering problem in
`kernel_state_->Save`.

**4. Do not plan a savestate-fixture harness as the general mechanism.** It works
for five of eight and the plan reads as though it works for all.

## Limits

- **No claim that Cemu or eden could not implement savestates**, only that they
  have not. Switch and Wii U state is genuinely hard to serialise, and yuzu
  never shipped savestates either.
- **The input-replay probe is three probes wide and its hits were not all read.**
  ARMSX2, xenia and melonDS scored 2/3 on generic terms — **treat those as
  candidates, not as working replay systems.** Only azahar's `core/movie.cpp` was
  confirmed by name.
- **xenia's deadlock is quoted from its own research file and was not
  reproduced.**
- **Nothing is measured.**

## Sources

- `tools/capability_probe.py`, capabilities `savestate` and
  `deterministic_input_replay`
- Cemu `src/gui/wxgui/GraphicPacksWindow2.cpp`
- eden `src/android/app/src/main/java/org/yuzu/yuzu_emu/NativeLibrary.kt`
- xenia `docs/research/20260620-adreno-turnip-feature-gap-audit.md`
- `research_log/20260823_1848_fleet_skills_mined.md` for the noise floors
