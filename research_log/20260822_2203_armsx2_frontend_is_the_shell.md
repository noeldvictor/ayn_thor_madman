# ARMSX2's Android frontend is the most complete shell in the fleet

**Goal: read ARMSX2's Android frontend systematically, because it has produced
two reversals today and has never been surveyed.**

Session 2026-08-22 22:03.

**Result: 63,111 lines across 153 files. It is five times the size of the fork
this repo calls "the most complete shell in the fleet", it is Compose rather
than Activity-per-manager, and it already implements most of
[`app/SCREENS.md`](../app/SCREENS.md) — including the per-game override system
Track A step 4 says to design.**

---

## The size correction

`CLAUDE.md` says:

> **Read `xenia-thor` for its feature list, not its shape.** It is the most
> complete shell in the fleet and the worst structured: 12,313 lines of Java,
> Activity-per-manager, a menu tree.

| Fork | Frontend | Shape |
| --- | --- | --- |
| **ARMSX2** | **63,111 lines, 153 files** | **Compose, ViewModels, navigation graph** |
| xenia-thor | 12,313 lines | Java, Activity-per-manager, menu tree |

**ARMSX2's frontend is 5.1x larger and modern.** The advice to start from
xenia's feature list was written without measuring the alternative.

### Where the lines are

| Lines | Area |
| --- | --- |
| 8,690 | top level: repos, catalogues, installers, second screen |
| 7,764 | `ui/settings`, 13 tabs |
| 5,491 | `runtime` |
| 4,129 | `ui/touch`, overlay, gestures, lightgun |
| 3,894 | `ui/common`, shared sections |
| 3,493 | `ui/home`, library, XMB GL view, wave background |
| 3,364 | `config`, **`Settings.kt` alone is 2,777 lines** |
| 2,228 | `ui/emulation`, in-game menu |
| 1,766 | `i18n` |
| 1,688 | `ui/patches` |

---

## Mapped against SCREENS.md

| # | Screen | ARMSX2 | Note |
| --- | --- | --- | --- |
| 1 | Library | **yes** | `HomeScreen` 1853, `XmbGlView`, `CoverRegionIndex`, `LibraryKeyboard` |
| 2 | Game detail | **yes** | `GameInfo` 550, `DiscIdentity`, `PlayTime` |
| 3 | In-game overlay | **yes** | `EmulationMenuScreen` 1789 + `InGameOverlay` |
| 4 | Screen-2 companion | **yes** | `SecondScreen` 576 + `SecondScreenTiles` 131 |
| 5 | Settings, global | **yes** | 13 tabs, **plus a generated search index** |
| 6 | Settings, per game | **yes** | `ConfigStore`, see below. **The important one** |
| 7 | Cheats | **badge only** | `CheatPresenceIndex` 127. No cheat manager UI |
| 8 | Patches | **yes** | `PatchManagerScreen` + `ViewModel`, 1688, `PatchRepo` 542 |
| 9 | Storage | **no** | measurement primitives only |
| 10 | Drivers | **yes** | `DriverManagerSection`, `CustomDriver` 420, `GpuInfo` |
| 11 | Display and layout | **partial** | second screen yes; guest-screen routing N/A, PS2 is single-screen |
| 12 | Input and hotkeys | **yes** | `PadTab` 1476, `HotkeysTab`, `ControllerManagerScreen`, touch 4129 |
| 13 | Guest accounts | **N/A** | the PS2 has none |
| 14 | Guest system UI | **N/A** | the PS2 has none |
| 15 | Systems | **N/A** | single-system app |
| 16 | Diagnostics | **yes** | `InfoTab`, `BiosInfo`, `DeviceTier`, `BatteryWatcher` |

**Beyond the screen list**, it also has: onboarding, achievements and
RetroAchievements, friends, news, about, BIOS manager, memory card manager and
backup, save state picker and save manager, texture pack catalogue with an
online section and installer, a shader chain editor with parameter editing,
controller skins, themes, 13 languages, Discord presence, library music, pause
music, menu sound effects, screenshots and home shortcuts.

---

## The finding that matters: per-game overrides are solved, and the naive design is wrong

`config/ConfigStore.kt`, 27 KB, with `Settings.kt` at **2,777 lines and 240
settings fields**, `merge`, `diff`, `toJson` and `fromJson`.

`CLAUDE.md` Track A step 4 specifies:

> **Pin the per-game override resolution.** Per-game value, then Thor profile,
> then backend default. One resolver, in one place.

ARMSX2 built exactly that, shipped it, and **hit three bugs that the
specification as written does not prevent.** Each is documented in the source
with the symptom that was reported.

### Bug 1: sparse is not enough. An override must be sticky.

The obvious design stores the fields that differ from global right now. Its
comment:

> The old rule — store exactly the fields that differ from global right now —
> could not tell "the user set this for this game, and it happens to match
> global" from "the user never touched it", so it stored nothing in both cases.
> **Set Cheats on for a game while global also had them on, later turn global
> off, and the game silently lost its setting**; that's the reported "global
> overwrites per-game and vice versa".

The fix is **pinning**: a field is pinned to the game once it is overridden, and
only an explicit clear unpins it.

**Sparseness and stickiness are in tension and you need both.** Sparse so a
later global tweak reaches untouched fields; sticky so an override that happens
to equal global is still an override.

### Bug 2: some settings cannot be per-game at all

> PINE is one server for the whole process, so `Settings.merge` pins it to the
> global value and `Settings.diff` never emits the key — both deliberate. The
> consequence was that toggling PINE from the in-game menu, which saves in Game
> scope, **wrote it NOWHERE**: the override file refuses the key and global was
> not being written. The switch stayed on only because `saveSettings` had already
> updated the in-memory Settings, so it read as "enabled" until the process
> restarted.

**This contradicts a requirement in `CLAUDE.md`**, which says every setting is
overridable per game with no exceptions. **Some settings are structurally
process-wide** — one server, one loaded driver, one device. The requirement
needs a stated exception and a promotion rule.

The fix promotes only those fields onto global, *by copying them onto global*
rather than saving the resolved object, because "writing all of it to global
would leak every per-game value into the global layer."

### Bug 3: whole-object writes make a pinned value stale

> Every screen writes the whole Settings object, so `updated` can be a stale
> snapshot; the old unconditional `full.get(key)` then wrote that stale value
> straight back over a good override. **That is how a per-game FPS cap of 30 came
> back as 0 and STAYED 0** — the pin made the wrong value sticky, so it survived
> even after the writers were fixed.

The fix takes a `previous` argument and trusts `updated` only for keys that
`previous` proves the caller just changed.

**Bug 3 is caused by the fix for bug 1.** Pinning makes a wrong value permanent,
so pinning needs change-tracking to be safe. **A contract that specifies pinning
without specifying change-tracking ships bug 3.**

---

## What this changes

1. **Do not design the settings and override system. Take ARMSX2's**, including
   its three fixes. The design is worth more than the code, because the three
   bugs are the expensive part and they are already paid for.
2. **Correct the contract.** `SettingSpec` needs a **scope** — per-game,
   global-only, or promoted — and the resolver needs pinning plus
   change-tracking.
3. **Correct `CLAUDE.md`'s "every setting is overridable per game".** It is
   nearly right and the exception is real.
4. **Re-rank the shells.** ARMSX2's frontend, not xenia's, is the thing to mine.
5. **The migration constants are a warning.** `ConfigStore` carries seven
   one-time migration keys for settings that changed scope or default. **A
   settings schema that ships will need migrations**, and the contract says
   nothing about versioning.

---

## Method note

**This is the third time today ARMSX2 turned out to hold something this repo
planned to build**, after the hotkey enum and the second-screen `Presentation`.

The cause is now clear enough to name. **This repo surveyed ARMSX2's `docs/`
directory and treated that as having read ARMSX2.** Its documents describe
research — upscaling kernels, neural models, the MCP design, the ARM64 review —
so the survey concluded ARMSX2 is a research fork. **Nobody listed its Kotlin.**

`capability_inventory.md` already warns that a capability recorded from a file
listing is a hypothesis. **This is the same error one level up: a fork
characterised from its documentation rather than its code.**
