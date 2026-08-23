# The host side: what a core hands over

**The inventory of every host-side service a Thor core gives up, and who owns it
instead.**

[`PATTERNS.md`](PATTERNS.md) answers *what does every emulator have*, pipeline by
pipeline. **This answers the other question:** *what exactly does a backend stop
owning, and what replaces it.*

> **A Thor core is an emulator with its host-side pipelines removed.** It keeps
> the ISA decoder, the guest GPU model, the guest memory map and the timing. It
> hands over everything below.

**This is the whole anti-RetroArch argument in one table.** RetroArch cannot make
a core faster because it wraps a black box behind a narrow API — it supports
hundreds of cores and must take each as upstream wrote it. **We support eight on
one device, which buys the right to reach in and take a responsibility away.**

**Every row names the fork evidence.** Nothing here is designed from first
principles.

---

## A. Graphics

### A1. The Vulkan device — instance, physical device, queues, allocator

**Handed over completely.** A backend receives `DeviceHandles` and
`DeviceFacts` and stores them. It never calls `vkCreateInstance` or
`vkCreateDevice`.

**Why:** one binary cannot sensibly hold seven Vulkan devices, and two devices
mean two memory budgets on a device with a hard ceiling.

**Evidence:** all seven forks built their own. **Six built it inside a renderer;
only xenia built it as a module** — `src/xenia/ui/vulkan/` against
`src/xenia/gpu/vulkan/`. Cemu's `VulkanRenderer.cpp` is 4,465 lines doing both.
**Take xenia's**, about 7,000 lines once its Vulkan-drawn UI is dropped, and
**BSD**, so the shared module stays usable by anything.

**The queue carries a lock**, because melonDS already learned it needs one.

### A2. Memory allocation

One allocator, one budget owner. **Three forks vendor a memory allocator
separately today** — ARMSX2 `vk_mem_alloc.cpp`, Vita3K VMA-Hpp, azahar.

### A3. The pipeline and shader-module cache — driver blob only

**Owned:** the `vkGetPipelineCacheData` blob. **All eight forks call it.**

**Not owned:** guest shader source to SPIR-V, keyed on a source hash. That is
guest knowledge and stays with the backend. **The two differ usefully — the
driver blob dies on a driver swap and the translation cache survives it**,
because SPIR-V is portable.

**Invalidation comes from the Vulkan specification, not from us.**
`pipelineCacheUUID` changes when a driver's caches become incompatible. ARMSX2's
`VKShaderCache.cpp` already validates header length, version, vendor ID, device
ID and UUID. **Do not key on a Turnip build string** — a driver can change its
compiled format without changing its package name.

**One shared cache has a cost nobody had priced.** A per-game driver override
changes the UUID, which discards the warm cache for **every backend at once**.
**Name the cache file by `pipelineCacheUUID` and keep the last two.**

### A4. The texture upload path

**Owned:** the upload, the per-class routing, the upscaler and pack replacement.

**Not owned:** the texture key. **The backend computes it**, because hashing a
texture means hashing guest formats and a guest palette. ARMSX2 and melonDS key
on data plus palette; Cemu keys on physical address. **The key is opaque to the
shared layer.**

**Texture classes are a declared list, not an enum.** ARMSX2 has two, melonDS
three, Cemu none.

**The filter list is declared too**, and for a sharper reason: **ARMSX2's
`Anime4K` is a neural network (`anime4k_x2.a2nn`) and melonDS's `Anime4K lite`
is a nine-texel kernel.** Same name, unrelated techniques. A shared enum would
merge them silently.

### A5. The present path, pacing, and frame generation

**Owned, and this is a line drawn on 2026-08-23.**

**A backend never synthesizes a present.** Frame generation is one
**interpolate-against-extrapolate** decision with a **latency** consequence, and
it must be made once for the app.

| | ARMSX2 | xenia |
| --- | --- | --- |
| Method | interpolation, ported from lsfg-vk / eden PR #4263 | extrapolation |
| Needs the next real frame | **yes** | no |
| **Latency cost** | **a held frame** | **none** |

**On a device with real buttons, a held frame is not a detail.**

**Frame pacing has no incumbent.** **No fork uses Swappy. No fork uses
`VK_GOOGLE_display_timing`** — verified with a second search; the single
build-file hit was Discord's `game_sdk`. Every fork picks a present mode and
stops. **FIFO is vsync, not pacing**: a 20 ms frame at 60 Hz alternates 16.6,
33.3, 16.6, and that judder is more visible than a stable 30.

**Both panels support 120 Hz and the device is capped to 60 by a user setting.
Check the cap before trusting any pacing number.**

### A6. Render pass structure — NOT owned, for now

**Stays with the backend**, pending measurement. Flattening pass structure spills
GMEM to system memory, and on a tiler that gives back more than it gains.

**But the fleet is not tuned here either.** Four forks, four answers: **Vita3K
tracks transient attachments and uses `DontCare` both ways**, Cemu defaults depth
and stencil to `DONT_CARE`, azahar always stores, **eden loads and stores depth
unconditionally**. **Nobody merges passes. Nobody uses input attachments. Nobody
resolves MSAA on-chip.**

### A7. The GPU driver

**A backend never picks one.** One pinned Turnip is the reference configuration,
bundled and loaded through adrenotools.

**adrenotools loads one driver at process start**, so a packed binary holds
exactly one for its lifetime. **The per-game driver choice is real; the per-game
effect is not, until the process restarts.** It is a `PROMOTED` setting with
`liveChangeable = false`.

**Take rpcsx's `GpuDriverAdvisor`**, which returns `INCOMPATIBLE` / `RISKY` /
`COMPATIBLE` rather than listing options, and recovers a target family from a
package name. It is GPL-2.0-only, so **take the shape, not the code**.

---

## B. CPU, threading and synchronisation

### B1. The thread pool and cluster affinity

**Owned.** The 8 Gen 2 is 1+4+3. Two backends with their own pools fight over
the single X3.

**Evidence:** four forks set host affinity — xenia, ARMSX2, eden, rpcsx.
**melonDS sets thread priority but never affinity, and Vita3K sets neither.**
melonDS is the case worth naming: **it tunes its compiler for the prime core
with `-mtune=cortex-x3` and then never asks for the X3.**

**Warning:** searching for `sched_setaffinity` returns **guest** code — Cemu's
Wii U `coreinit`, azahar's 3DS syscall, rpcsx's Orbis kernel. **Three of seven
hits were the guest.**

### B2. The spin and park primitive

**Owned, added 2026-08-23.** A backend never implements its own.

**Measured on this SoC** (rpcsx `thor_bench.cpp`): `yield` **0.36 ns**, `nop`
**0.36 ns**, `isb` **11.42 ns**, armed `wfe` **~72 µs**. Spin wake latency
~0.44 µs; futex wake ~10 µs.

**`YIELD` is exactly a `nop` on the silicon.** And **`ISB` costs 32×**, which is
why a hand-tuned iteration count cannot move between backends: rpcsx measured
**+23%** regression swapping the instruction alone, xenia's equivalent A/B came
out `CONFOUNDED`, and Cemu's time-based attempt was **worse on the Thor**.

**Three tiers exist and the best exists twice:** `SEVL`/`WFE` + `LDAXR` in
ARMSX2's `MonitoredWait` and in dynarmic's `EmitSpinLockLock` — **0BSD**. Take
dynarmic's. **Never `CLREX` on the way out**: ARMSX2 measured **3.5 wake-ups per
wait against 6708** with one added.

**Budget in time, and measure the step cost on the host that booted.** ARMSX2
calibrates at run time; Vita3K derives a wall-clock budget from `CNTFRQ_EL0`.

### B3. The memory budget

**One owner.** A handheld has a hard ceiling, and only one owner can decide
between a texture cache and a shader cache.

### B4. Guest FP environment — NOT owned, but constrained

**Stays with the backend**, because it is guest knowledge. **But the shared
layer requires the hazard be handled.**

**ARMSX2's three-part design is the reference:** EE `DIV`/`SQRT` bake `FPUFPCR`
into the block as an immediate, mVU gates the write with
`mvuNeedsFPCRUpdate`, and **mVU hashes all four environments into its
block-cache sentinel**. **Parts one and two without part three are a correctness
bug.**

**The structural problem is real:** the Xenon has **two** independent FP mode
registers (`FPSCR` scalar, `VSCR.NJ` VMX) and **ARM64 has one**. An `FPCR` write
that changes control fields **introduces a barrier** (A710 SWOG Table 4-3
note 2).

---

## C. Platform and process

### C1. The JNI boundary

**Owned. A backend never owns a JNI entry point.**

**`JNI_OnLoad` cannot exist twice in one binary and cannot be renamed**, because
the Android runtime looks it up by that exact name.

**Measured 2026-08-23:** ARMSX2 and Vita3K both export `JNI_OnLoad` and the whole
`Java_org_libsdl_app_*` family, because both statically link SDL — **at
different major versions**, 3.5.0 against 3.2.28.

### C2. Dependencies and symbol visibility

**Owned.** A backend declares every library it vendors and never adds one
silently.

**Measured:** **25,526 symbols are exported by two or more forks, and zero are
emulator code.** Every collision is a dependency.

**A dependency is safe when it versions its own ABI, not when it is C++:**

| Tier | Libraries |
| --- | --- |
| safe — versioned namespace | **fmt** (`v6` and `v12` coexist) |
| already unified | **libc++** (`std::__ndk1`, identical in all six) |
| **must be unified** | **OpenSSL, zlib, SDL, libpng** — plain C |
| **must be unified** | **imgui, boost, glslang** — C++ with **no** version namespace |

**OpenSSL is the most fragmented and the largest collider:** three versions
across three forks (Cemu 3.5.0, eden 3.6.0, Vita3K 3.6.2), ~6,400 symbols, and a
security dimension the others lack.

**`-fvisibility=hidden` sets the collision surface.** Exports span **43×** —
xenia 2,285 against Vita3K 98,550 — and colliding symbols per backend run
**2,092 to 19,696**.

### C3. Audio

**Owned. Oboe `1.10.0`**, pinned. Version read from eden's CPM cache — a real
working Android arm64 pin.

**Three forks chose Oboe independently** — ARMSX2, eden, melonDS. Oboe selects
AAudio on Android 8.1+ and handles device-specific latency tuning. **cubeb
reaches Android through its own backends, which is a portability layer this
project refuses to pay for.**

**Four conversions remain**, not a design decision: xenia off its own driver,
Vita3K off SDL audio, azahar and Cemu off cubeb.

---

## D. Interaction

### D1. Input, hotkeys and the touch overlay

**Owned.** A backend never defines a hotkey. **One hotkey set works on every
system, always** — that is the named RetroArch failure this project exists to
fix.

**Take ARMSX2's `ControllerMappings.SysHotkey` enum and `HotkeysTab`**, which
arms on a row tap and captures the next controller button, so it needs no
keyboard and no key names. **It was split out of the Pad tab so hotkeys are easy
to find** — this project's own usability complaint, fixed independently inside
the fleet.

**The overlay is the best extraction candidate in the fleet.** azahar and Vita3K
ship the **same four classes from the same 2013 Dolphin ancestor**, and **eight
method names survived twelve years of independent divergence** — `draw`,
`onTouch`, `onTouchWhileEditing`, `isInEditMode`, `setIsInEditMode`,
`refreshControls`, `resetButtonPlacement`, `saveControlPosition`. **Those eight
are the contract.**

**Each fork solved a different half.** azahar added construction and layout;
**Vita3K added lifecycle and physical-controller attach/detach**, which matters
most here because the Thor has real buttons.

**The element list is declared by the backend**, with a **kind** as well as a
name — Cemu's `BUTTON_BLOW_MIC` is not a button at all.

### D2. Haptics — two features, one word

**Owned, both. Corrected 2026-08-23: seven of eight forks ship host-side
haptics; xenia is the only one without.**

| Feature | Reference | Shape |
| --- | --- | --- |
| **Overlay touch feedback** | melonDS `TouchVibrator` | user-settable strength, `VibratorDelegate`, **duration fallback when the device cannot vary amplitude** |
| **Guest rumble routing** | eden `YuzuVibrator` | `getControllerVibrator(device)` against `getSystemVibrator()` — rumble reaches the pad that caused it |

**Neither substitutes for the other.** The contract needs two entries.

### D3. Guest system applets

**Owned. Take azahar's contract** — `src/core/frontend/applets/`, GPL-2.0-or-later
so the code is usable, not only the design.

**Asynchronous by construction:** config struct in, `Setup`/`Execute`,
`Finalize`, then `DataReady()` / `ReceiveData()`. **Nothing blocks the guest
thread waiting on a person.**

**Validation lives in the shared layer, in three phases** — `ValidateFilters` as
the person types, `ValidateInput` on submit, `ValidateButton` before closing,
with a **twelve-value** `ValidationError`.

**The guest can validate, so it is a round trip.** `Filters::enable_callback`
plus `ShowError` — a single "receive a string" call cannot express this.

**A default implementation is always registered**, so a partially-implemented
shell still boots.

### D4. Dual-screen routing

**Owned.** A backend declares its guest screens — name, native size, touch,
whether the title needs it — **and never decides where they go.**

**Do not design this.** melonDS models the Thor's exact case with
`INTERNAL_TOP_EXTERNAL_BOTTOM` / `INTERNAL_BOTTOM_EXTERNAL_TOP`; azahar has an
eight-mode `SecondaryDisplayLayout`. **Both are richer than what was designed
here.**

**Touch must follow the screen.** A routing change that does not move the touch
mapping is a bug.

---

## E. Data and content

### E1. Paths, storage accounting and content resolution

**Owned.** A backend writes only where the app tells it to, and declares its
storage categories and whether each is **rebuildable**.

**Take GameThor's screen** — `ContainerStorageManager` (994 lines), its dialog
(911) and `StorageUtils` (231). It already has aggregation, `StatFs` free space,
tree-walk sizing, **binary units**, delete actions, and **`moveGame` between
internal and external storage**, which this project's design lacked.

**Take Vita3K's resolver** for *where is the content* — enumerate roots, build
candidates, resolve, report which won — **and eden's `PathUtil` for the adjacent
half**, converting a SAF `content://` URI to a real path including removable SD
volumes.

### E2. Settings, per-game overrides and migrations

**Owned. Take ARMSX2's `ConfigStore`**, 240 fields with `merge` and `diff`, and
its three learned bugs: **an override must be sticky**, **pinning needs
change-tracking**, and **some settings are process-wide and must be promoted**.

**Take melonDS's migration framework** — 37 files, 16 concrete migrations, the
**app's own version code** as the schema version, and **frozen legacy DTOs** so a
migration never deserialises with a class that keeps changing.

**The typed settings framework already exists twice**, in azahar and eden — 40
shared files. **The design is proven; the code has fully diverged, so extraction
is a rewrite guided by two references.**

### E3. Cheats and patches

**Owned: the engine, the UI, the library. Declared by the backend: the region
namespace, and whether a write targets code.**

**One opcode dominates every real corpus** — `be32` is **84%** of rpcs3's patch
entries, `0` is **66%** of 91,372 `.ncl` lines. **A tiered engine is the right
shape.**

**Three additions to dmnt as the instruction set:** a **byte swap** (six of
pnach's nine data types, and both PS2 and PS3 are big-endian), a
**backend-declared region list**, and **a code write that invalidates the block
cache** — VitaCheat's `$A000`, which dmnt cannot express because Atmosphere runs
on real hardware with no recompiler.

**Take xenia's `.patch.toml` as the authoring surface and Cemu's resolver as the
engine.** Cemu's is a symbolic assembler with a relocating linker and
`ResolvePresetConstant`, which feeds a user setting into a patch as a value.

---

## What this list is not

- **Not a schedule.** Ownership is decided per subsystem, with evidence, and
  recorded in [`OWNED.md`](OWNED.md). **The owned list is still empty.**
- **Not a promise of frames.** xenia's ledger records many incremental GPU levers
  as `DEAD` or `FLAT`. **The shared layer buys maintainability, features and
  consistency; where it buys frames, prove it per subsystem.**
- **Not complete.** Save-state format stays per-backend and is irreducible; the
  unpriced cost there is to the **test harness**, because a state-layout change
  invalidates every fixture for that backend at once.
