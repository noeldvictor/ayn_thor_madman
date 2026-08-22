# Survey: Thor tooling, performance and the unsurveyed areas

Goal: survey Cemu-thor, eden-thor and GameThor, which had no capability
recorded, and cover frame pacing, affinity, audio and overlays across the
fleet.

Date: 2026-08-22, 17:30 EDT.

## Headline: xenia-thor holds the fleet's measurement harness

`xenia-thor-workspace/xenia-thor/tools/thor/` contains **137 scripts**: 88
PowerShell, 32 shell, 8 python, 8 `.mjs` workflow scripts, plus an `mcp/`
directory.

This is far more Thor-specific optimisation work than any other fork has. It
was not recorded anywhere in `capability_inventory.md` before today.

### An MCP server already exists

`tools/thor/mcp/thor_mcp_server.py`, with `README.md`, `requirements.txt` and
`_run_job.ps1`.

**This corrects an earlier claim.** `capability_inventory.md` recorded the
on-device MCP as ARMSX2 `docs/mcp-server.md`, quality `design`, not
implemented. That is true of ARMSX2. It is not true of the fleet. **Read the
xenia server before writing a new one.**

### Adreno-specific optimisation work

No other fork has anything at this level:

| Script family | Subject |
| --- | --- |
| `bd_gmem_ab.sh`, `bd_gmem_report.py` | GMEM, the Adreno tile memory |
| `bd_lrz_census.sh`, `bd_lrz_report.py` | LRZ, low resolution Z |
| `bd_vrs_capture.sh`, `bd_vrs_heavy_pass_ab.sh` | Variable rate shading |
| `bd_resolution_ab.sh`, `bd_render_mode.sh` | Resolution and render mode |
| `bd_texcompress_check.sh` | Texture compression |
| `bd_shader_stats.sh`, `bd_shader_report.py` | Shader statistics |

### ARM64 code generation audits

Roughly 40 scripts named `thor_a64_*` and `thor_hir_*`. They audit register
caching, state carriers, fast entry protocols, block linking, VMX128 to NEON
family mapping, and dead state store elimination.

This is the deepest CPU-side work in the fleet.

### Agent and capture tooling

`thor_codex_goal_loop.ps1`, `thor_tas.ps1`, `thor_gpu_perfetto.ps1`,
`thor_gpu_capture.ps1`, `thor_renderdoc.ps1`, `thor_game_matrix.ps1`,
`ghidra_headless_import.ps1`, `thor_evidence.ps1`, `thor_verify_capture.ps1`.

`ghidra_headless_import.ps1` matters because Ghidra is planned. It already
exists.

Eight `wf_*.mjs` files are multi-agent workflow scripts, including
`wf_arm64_adreno_research.mjs`, `wf_deep_codebase_eval.mjs` and
`wf_gpu_rearch.mjs`.

## Correction: why Wi-Fi adb is preferred

**CLAUDE.md was wrong.** It said Wi-Fi adb is preferred because the Thor can
stay on a charger, so battery level does not confound a measurement. The
opposite is true for power.

From `power_affinity_ab.sh`:

> Plugged in, dumpsys reports status=Charging and current_now flips sign
> between consecutive idle samples (measured: -36988, +225591, +165897,
> +224859, -16846 uA). Any wattage from a USB-attached session is fiction.
> This script REFUSES to report power unless the device says Discharging, and
> connects over wifi so the cable can be removed.

**Wi-Fi adb exists so the cable can come out.** A power measurement requires
the device to be discharging. A charging device cannot be measured for watts
at all.

The script gates on `status=Discharging` before reporting power. Copy that
gate into any measurement tool.

## Finding: guest threads were pinned to the wrong cores

From the same script, a structural finding dated 2026-08-07 and since fixed:

> guest threads were HARD-PINNED to the 2.0GHz A510 little cores while the
> Cortex-X3 sat idle

The A/B has three arms:

- **A** baseline, guest CPU N to host CPU N, the old 1:1 map, guest 0-2 on
  A510.
- **B** prime, guest 0 to the X3, the rest to the performance tiers. This is
  now the default, after commit `759e2b59d`.
- **C** mid-tier, guest 0 to an A715, the rest round-robin, as an efficiency
  control.

Arm C is selectable at run time with
`--ei thor_guest_thread_affinity_mask <mask>`.

**The goal stated is a power target, about 5 W and 50 C, not throughput.** The
script says throughput alone answers the wrong question. That framing belongs
in the shared measurement rules.

## Finding: ADPF is disabled on this device

From `bd_adpf_ab.sh`:

> The persisted device config has it FALSE, which overrides the compiled
> default of true - so ADPF is disabled on this device today and an arm that
> did not force it would measure nothing.

The A/B compares two ADPF targets on Blue Dragon:

- `base`: target 60 fps when uncapped. The old hint.
- `adpf`: target is an EMA of achieved frames,
  `gpu_adpf_target_from_actual`.

The reasoning is worth keeping. Blue Dragon is about 93% GPU-bound at roughly
15 fps and uncapped, so a fixed 60 fps hint reports a deadline miss on every
frame. ADPF answers by pinning CPU clocks high, which is boost that cannot add
a frame. **Expected signature of the fix: same fps, lower temperature rise.**

That is a good example of the rule that a performance claim needs a stated
expected signature before the run.

## Finding: Cemu has the Android Performance Hint API

`cemu-thor-experiment/src/Cafe/Android/AndroidPerformanceHints.h`:

```cpp
void UpdateSchedulerThreads(const std::vector<pid_t>& threadIds,
                            size_t expectedThreadCount);
void ReportFrameBoundary(std::chrono::steady_clock::time_point now = ...);
void CloseSchedulerSession();
```

Cemu and xenia both use ADPF. No other fork does. Two implementations of the
same idea, which is the usual pattern in this fleet.

## Other Cemu findings

Cemu had no capability recorded before today.

| Capability | Path |
| --- | --- |
| Android performance hints | `src/Cafe/Android/AndroidPerformanceHints.*` |
| Android SAF filesystem | `fscDeviceAndroidSAF`, `FileStream_saf` |
| Full Android app | `src/android/app/`, with `NativeInput.cpp` |
| Shader cache | `LatteShaderCache` |
| Texture cache and loader | `LatteTextureCache`, `LatteTextureLoader` |
| On-screen overlay | `LatteOverlay` |
| Per-game controller profile | `bin/controllerProfiles/CemuThor_StarFoxZero_StarFox64ish.xml` |
| Audio | `snd_core/ax_*`, a full AX implementation |

The Star Fox Zero controller profile is a Thor-written per-game control
remap. Cemu already treats controls as per-game data.

## Frame pacing, affinity and audio across the fleet

| Fork | Finding |
| --- | --- |
| ARMSX2 | Oboe audio. **Per-game frame pacing UI, but only on iOS**: `SettingsStore+FramePacing.swift`, `FramePacingSettingsView.swift`, `FramePacingTab.swift`. |
| melonDS-android | Oboe audio, plus an `AudioLatency.kt` domain model. |
| eden-thor | `oboe_sink`, `k_affinity_mask`, `vsync_manager`. |
| xenia-thor | `power_affinity_ab.sh`, and the ADPF work above. |
| Cemu-thor | ADPF, as above. |
| azahar-thor | Nothing found. |
| Vita3K-Thor | Nothing found. |
| GameThor | Nothing found. |

ARMSX2 having per-game frame pacing on iOS and not on Android is a
ready-made port.

## GameThor

Still nothing recorded. No texture, cheat, driver, audio or pacing hits. It
runs Windows games through Proton, so it shares little with the emulator
backends. **Decide whether GameThor belongs in this app at all.** See
[Open decisions](../CLAUDE.md#open-decisions).

## Method note

The Thor serial and address differ between scripts. `power_affinity_ab.sh`
uses `192.168.1.33:5555` and serial `c3ca0370`. Today the device answers at
`192.168.1.3:5555`. **This confirms the rule against hardcoding the address.**

The driver in use in that script is
`mesa-turnip-v26.3.0-20260803-r7-vulkan-1.4.354-7`, installed through
adrenotools. Relevant to the GPU driver manager extraction.

## Next

1. Read `tools/thor/mcp/README.md` and `thor_mcp_server.py` before designing
   the fleet MCP.
2. Read `thor_game_matrix.ps1` and `thor_evidence.ps1`. They may already be
   the regression matrix and evidence ledger the shared harness needs.
3. Survey the `wf_*.mjs` workflow scripts. They are prior art for agentic
   orchestration in this project.
4. Port the ARMSX2 iOS frame pacing UI to Android.

## Online research, 2026-08-22

Done to check the local findings against published guidance.

### ADPF: the xenia finding matches Google's guidance

Source: [Best practices for ADPF](https://developer.android.com/games/optimize/adpf/best-practices-adpf)
and [Performance Hint API](https://source.android.com/docs/core/perf/performance-hint-api).

Confirmed mechanics:

- Every frame the app reports the **actual** duration, the sum of CPU and GPU
  time, and the **target** duration from the render frame rate.
- The system adjusts CPU frequency and scheduling when actual differs from
  target.

This confirms why the xenia `bd_adpf_ab.sh` reasoning is right. Blue Dragon is
GPU-bound at about 15 fps and uncapped, so a fixed 60 fps target reports a
deadline miss on every frame, and the system answers with CPU boost that
cannot add a frame.

Two rules to adopt from the guidance:

- **Measure without ADPF first.** Establish the baseline before adding the
  hint logic. Otherwise the hint is being tuned against an unknown.
- **Test for 15 minutes or more.** Thermal behaviour only stabilises over a
  long run. A short run measures the cold device, which is not how anybody
  plays.

The guidance also says ADPF should drive the game's own quality settings, not
only report timings. That maps onto per-game profiles: the app can lower
resolution or effects when the thermal headroom drops.

### Adreno is a tiler, and that shapes the shared render path

Sources: [Adreno GPU on Mobile best practices](https://docs.qualcomm.com/nav/home/mobile_best_practices.html?product=1601111740035277),
[Turnip and tiled rendering](https://deepwiki.com/sailfishos-mirror/mesa/3.3.1-turnip-vulkan-driver-and-tiled-rendering),
[Low-resolution-Z on Adreno GPUs](https://blogs.igalia.com/dpiliaiev/adreno-lrz/).

- **GMEM is on-chip tile memory.** Adreno is a tile-based deferred renderer.
  Rendering goes to a tile in GMEM, then resolves to system memory.
- Turnip uses GMEM, VSC for binning, and LRZ for early depth rejection.
- **Vulkan render passes and subpasses are the lever.** Multiple passes can
  stay in GMEM. This is the main reason Vulkan beats GL on this hardware.
- **Since Adreno 650, LRZ survives across render passes** if depth is stored
  in one pass and loaded later, and LRZ state can be reused between passes.
  The Adreno 740 is newer, so this applies.
- Direct GMEM access extensions arrive at Adreno 840. **The 740 does not have
  them.** Do not design around that.

**Design consequence for the shared layer.** A shared render and upload path
must be tiler-aware. Render pass structure is not a detail on this GPU; it
decides whether work stays in GMEM or spills to system memory. A shared path
that ignores this will be slower than the per-fork paths it replaces.

This is the strongest argument found so far for xenia-thor leading the shared
Vulkan work. Its GMEM and LRZ census scripts already measure exactly this, and
no other fork has touched the layer.

### Presentation API is the right route for Screen-2

Source: [Multi-screen management within Android](https://innovorder.dev/multi-screen-management-within-android-56ef9052f066),
[Using the Presentation API with Jetpack Compose](https://medium.com/@ibrahimethemsen/using-android-presentation-api-with-jetpack-compose-998adeae1130).

- `Presentation` has existed since API 17 and is derived from `Dialog`.
- `DisplayManager.getDisplays()` enumerates the displays. `DisplayListener`
  handles connection changes.
- It works with Jetpack Compose.

Note the `Dialog` lineage. A `Presentation` owns a `Window`, so a Vulkan
surface on Screen-2 needs a `SurfaceView` inside the `Presentation`, not a
direct swapchain on the display.

No published guidance was found on game performance with two rendered
displays. **Treat the cost of a second swapchain as unmeasured.** Measure it
before the dual-screen layout is assumed free.

### Turnip on the Adreno 740

Source: [AdrenoToolsDrivers](https://deepwiki.com/K11MCH1/AdrenoToolsDrivers),
[Turnip driver guide 2026](https://pocket-gaming.org/2026/06/15/the-definitive-guide-to-android-turnip-drivers-hardware-compatibility-2026/).

- Mesa v25 and v26 are the series for the Adreno 740 in 2026. Turnip v26.0.0
  r7 is described as a community favourite for stability on Adreno 7xx.
- Drivers load through adrenotools with no root.
- Turnip supports more extensions than the stock Qualcomm driver, and is often
  faster.

xenia currently runs `mesa-turnip-v26.3.0-20260803-r7-vulkan-1.4.354-7`, which
is newer than the community favourite. **The shared driver manager should
curate a known-good list per backend, not only list what is installed.**
`GpuDriverAdvisor` in rpcsx is the only implementation that advises, which is
why it is the one to read first.

## Open questions raised by this survey

1. What does the second swapchain cost on Screen-2? Unmeasured, and the
   dual-screen feature depends on the answer.
2. Does the shared render path preserve LRZ reuse across passes? If not, the
   shared layer loses performance the forks already have.
3. Should the driver manager pin a known-good Turnip per backend, or track
   one fleet-wide version?

## Finding 9: xenia-thor has 29 agent skills and an experiment ledger

`xenia-thor/.agents/skills/` holds **29 skills**. This is the prior art for the
whole AI-first pillar, and it was unrecorded.

### The skills that matter fleet-wide

| Skill | What it encodes |
| --- | --- |
| `xenia-thor-evidence-discipline` | **MANDATORY** before stating any performance number. Capture device evidence to a file first. |
| `xenia-experiment-ledger` | A SQLite database of experiments. Query it **before** running any device experiment. |
| `xenia-thor-experiment-gate` | Gate risky experiments to prevent repeated guesses. |
| `xenia-thor-autonomous-driver` | The full loop: preflight, build, deploy, launch, capture proof, classify, worklog, commit. |
| `xenia-thor-ghidra-game-patch` | Author game patches as `.patch.toml` using Ghidra. |
| `xenia-thor-adb-gpu-stage-split` | Per-stage Adreno 740 split over adb: binning, vertex, fragment, stall, per-draw cost. |
| `xenia-snapdragon-profiler-gpu-metrics` | Adreno 740 hardware stage metrics. |
| `xenia-windows-powershell-command-hygiene` | The PowerShell rules this repo also learned the hard way. |
| `consult-hard` | Consult a heavyweight external model to red-team a plan. |
| `xenia-continual-harness-refiner` | Online refinement of the harness itself. |
| `video-transcript-mining` | Mine technical talks into portable techniques. |

The project rule stated in the driver skill: **"no behavioral claim without
device proof."** That matches the rule this repo wrote independently.

### The experiment ledger is the anti-duplication mechanism for experiments

`tools/exp_ledger.py`, a SQLite database.

```
python tools/exp_ledger.py check "<keyword>"   # BEFORE any experiment
python tools/exp_ledger.py add "<lever>" "<category>" "<verdict>" ...
python tools/exp_ledger.py dead [category]     # the do-not-retry list
python tools/exp_ledger.py wins                # the shipped stack
```

Verdicts: `DEAD`, `FLAT`, `WIN`, `GFX-LOSS`, `CONFOUNDED`, `OPEN`.
Categories: `cpu gpu edram interlock shader rearch draw vertex fill
measurement`.

**This is the same idea as `capability_inventory.md`, applied to experiments
rather than features.** One stops rebuilding a feature; the other stops
re-running a dead lever. The fleet needs both.

### The measurement discipline is stricter than ours

From the ledger skill, and it corrects a looseness in this repo's rules:

> Cross-run fps/gpu_frame_us is CONFOUNDED (BD scene complexity swings ~4x per
> second). Only trust: single-run in-place alternating A/B on a GPU-busy
> frame, screenshot correctness, qemu byte-identical, code facts. If a number
> is cross-run, verdict it CONFOUNDED, not WIN/DEAD. Post-temp change confirms
> the field was reached (no heating = idle/menu scene = invalid run).

Three rules to adopt fleet-wide:

1. **Cross-run comparison is untrustworthy.** Use in-place alternating A/B
   within one run.
2. **`CONFOUNDED` is a verdict**, not a failure to record. A number that
   cannot be trusted must be labelled, not discarded and not promoted.
3. **Temperature proves the run happened.** No heating means the scene was
   idle or a menu, so the run is invalid regardless of what the counter said.

### The `.patch.toml` format already exists

The per-game patch format this repo specified as a design is built:

- `src/xenia/patcher/patcher.cc` and `patch_db.cc`, the patcher.
- `.agents/skills/xenia-thor-ghidra-game-patch/scripts/emit_patch_toml.py`,
  which authors a patch from Ghidra.
- The skill names both intents this repo specified: performance fixes such as
  60 FPS or disabling blur and SSAO, and cheats such as infinite health.

**Read this before designing a patch format.** Compare it against Cemu
`GraphicPack2Patches` and pick one, rather than inventing a third.

## Finding 10: the standing conclusion challenges the shared-layer thesis

From `xenia-experiment-ledger`, recorded as the standing conclusion:

> BD's gap is HLE-vs-LLE, proven by RE2 Remake running on the same Thor via
> GameNative/DXVK. xenia EMULATES the 360 GPU (slow); the fix is TRANSLATING
> D3D9->Vulkan like DXVK. Every incremental GPU lever is DEAD/FLAT because it
> patches the emulator instead of replacing it. Do not propose more
> incremental GPU levers - check first.

The `xbox360-d3d-hle-recomp` skill records the long-term direction, dated
2026-07-02: **D3D9-Xbox to Vulkan HLE, plus AOT static recompilation of the
PPC.**

### Why this matters to this repo

**It is evidence against incremental optimisation as the main lever.** The
shared layer is, in large part, incremental: shared caches, a shared upload
path, a shared device. xenia measured many such levers and recorded them
`DEAD` or `FLAT`.

This does not invalidate the shared layer. Two things stay true:

- The duplication is real and costs maintenance regardless of speed.
- Some shared items are not incremental GPU levers at all: the driver
  baseline, the test harness, per-game overrides, dual-screen routing,
  storage visibility, cheats and patches.

**But it does argue against expecting large frame wins from a shared
renderer.** Set that expectation now, before the work is done and disappoints.

### It also changes the view of GameThor

GameThor was queued for a decision on whether it belongs in this app, because
it runs Windows games through Proton and shares little with the emulator
backends.

**The ledger cites GameNative and DXVK as the proof that translation beats
emulation on this device.** RE2 Remake runs on the Thor that way. GameThor is
not an odd fit; it is the working example of the architecture xenia wants to
move toward.

Keep GameThor. Study it as evidence, not only as a backend.

## Next after this survey

1. Read `tools/exp_ledger.py` and decide whether the fleet adopts it as-is.
2. Read `src/xenia/patcher/` and compare with Cemu `GraphicPack2Patches`.
   Choose one patch format for the fleet.
3. Port `xenia-thor-evidence-discipline` and `xenia-experiment-ledger` into
   fleet skills. They are the measurement culture, and propagating it matters
   more than propagating any single feature.
4. Record the HLE-versus-LLE conclusion in CLAUDE.md as a caution on expected
   gains from a shared renderer.
