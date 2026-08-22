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
