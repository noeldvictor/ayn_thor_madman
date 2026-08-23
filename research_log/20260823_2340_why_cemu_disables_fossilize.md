# Why Cemu disables Valve's Fossilize layer — answered, and it changes the design

**Goal: find the reason for `DISABLE_VK_LAYER_VALVE_steam_fossilize_1=1` in
Cemu's `src/main.cpp` before adopting Fossilize.**

**It was flagged as the one thing that could invalidate the shipped-pipeline-cache
plan. It does not. It narrows it, and it supplies an acceptance test the plan did
not have.**

## The answer

**Two systems both managing pipeline caching for one process.**

Cemu's own troubleshooting guide states it directly:

> Steam caches shaders on its own unless you turn this off, **this majorly
> conflicts with `Async Shader Compile`.**

And the symptom is the important half:

> **graphics or models failing to render, usually a very broken-looking game.**

**Cemu has async shader compilation and it is a first-class feature**, with a
runtime setting and a **per-game profile override** — `asyncCompile` in
`src/Cafe/GameProfile/GameProfile.cpp`, logged at boot in `CafeSystem.cpp`. It
compiles pipelines on background threads rather than stalling the draw.

**Fossilize is a layer that intercepts pipeline creation.** An application doing
its own asynchronous pipeline creation and a layer recording that creation are
two cooks in one kitchen.

**There is a second, separate reason and it is historical.** Vulkan-Loader
1.2.146 and the Steam Fossilize layer crashed when used together — Vulkan-Loader
issue 433 — and downgrading the loader fixed it. **That one is a fixed loader
bug, not a standing objection.** Fossilize's own issue 139 records another
layer-interaction failure, an infinite recursion with libstrangle.

**So the line is defensive against a layer Cemu does not control, in a process
Cemu does not own.** It sits beside
`DISABLE_LAYER_AMD_SWITCHABLE_GRAPHICS_1=1` in a function named
`reconfigureVkDrivers`, which is a list of third-party layers that break this
application.

## Why it does not invalidate the plan

**The conflict is a two-owner problem, and in this app there is one owner.**

| | Cemu on a desktop | This app |
| --- | --- | --- |
| Who owns the Vulkan device | Cemu | **the shared device layer** |
| Who schedules pipeline compilation | Cemu | **the shared layer** |
| Who else is recording | **the Steam client, uninvited** | **nobody** |
| Can the two be coordinated | **no** | **yes, same binary** |

**Cemu could not turn Steam's precaching off from inside Cemu, so it turned the
layer off.** This project owns both sides.

## What it does change, and these are design rules now

**1. Do not run a recording layer under a backend doing async pipeline
compilation.** Either record from inside the shared device layer, which this
project owns and Cemu did not, or **disable async compilation for the duration of
a capture run** and record that the run was made that way.

**2. The acceptance test is visual, not temporal.** **Cemu's failure mode was
silent visual corruption, not a crash and not a stall.** A cache experiment
judged only on frame time would have passed while rendering a broken game.

> **Any pipeline-cache run must compare frames against a reference, not only
> timings.**

That is the golden-image comparison already specified in
[`CLAUDE.md`](../CLAUDE.md) under Phase 3, and this is the first concrete
requirement for it that came from outside the test plan.

**3. Async compilation and a warm cache attack the same problem and interact.**
Async compile hides a compile behind a placeholder. A warm cache means there is
nothing to hide. **Measuring one while the other is enabled measures neither**,
so a cache A/B must state the async setting and hold it fixed.

**4. Per-game is the right scope, and Cemu already proved it.** `asyncCompile`
is a per-game profile option in Cemu, which matches this project's rule that
every option is overridable per game.

## Limits

- **The conflict mechanism is inferred, not read.** Cemu's guide states the
  conflict and the symptom; **it does not say what Fossilize does that breaks
  async compile.** Fossilize's source was not read for this.
- **No claim is made that Fossilize is safe here.** It is a layer, and layer
  interactions are exactly what produced both known failures. **The safe form is
  recording from inside a device layer this project owns.**
- **Nothing is measured.**

## Sources

- Cemu `src/main.cpp`, `src/Cafe/GameProfile/GameProfile.cpp`,
  `src/Cafe/CafeSystem.cpp`
- <https://cemu.cfw.guide/troubleshooting.html>
- <https://github.com/KhronosGroup/Vulkan-Loader/issues/433>
- <https://github.com/ValveSoftware/Fossilize/issues/139>
