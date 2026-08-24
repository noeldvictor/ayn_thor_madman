# The app shell: screens and what they demand

**Track A step 1.** The screen list, now 16 entries, and for each screen what it needs from a
backend. The backend contract falls out of the last column. It is not argued
in advance.

Written 2026-08-22. This is a specification, not a mockup.

## How to use this

1. Build each screen with fake data. Navigable, on the device, on both
   displays.
2. Whenever a screen needs a fact only the emulator knows, add it to the
   contract.
3. When every screen is fed, the contract is finished.

**Reality check every screen against
[`../capability_inventory.md`](../capability_inventory.md).** Do not design a
screen for a capability no backend has. Do not omit one that four backends
already ship.

**CORRECTED 2026-08-22, from a measured census.** This file said xenia has the
most complete shell in the fleet. **It has the smallest Tier 1 frontend**, at
12,334 lines against melonDS-android's 78,033 and ARMSX2's 63,111.

**Mine three forks, for three different things:**

| Fork | Take | Why |
| --- | --- | --- |
| **melonDS-android** | **structure** | 78,033 lines, the only layered one: `domain`, `impl`, Hilt, a database, 37 migration files |
| **ARMSX2** | **features** | 63,111 lines; per-game overrides, the hotkey enum, the Screen-2 `Presentation`, settings search |
| xenia-thor | the feature **list** only | the hard-won inventory of what a shell needs |

**Almost every screen below already exists somewhere in the fleet.** That
changes Track A from design to harvest and reconcile. The exception is screen 9,
storage, which has no prior art anywhere and was therefore built here.

**Do not copy xenia's structure.** It has the **worst** structure in the fleet,
and being smallest and worst-structured are related facts.

Its shape is Activity-per-manager: `SettingsActivity`,
`GpuDriverManagerActivity`, `GamePatchManagerActivity`,
`ContentManagerActivity`, `TrainerManagerActivity`,
`GameOptimizationsActivity`, `ControllerMappingActivity`. **Seven separate
screens you navigate away into and back out of.**

That is a menu tree. It is structurally the same complaint this project has
about RetroArch, and [Foundation](../CLAUDE.md#foundation) point 4 forbids it:
one place for every setting, no hunting across screens.

Take from it:

- **The feature list.** It is the most complete inventory of what an emulator
  shell needs, learned the hard way.
- **The mechanisms.** Cover art fetching, driver management, patch and content
  installation, crash reporting.

Do not take:

- The navigation model. Activity-per-feature is what this app exists to
  replace.
- The code. It is Java and Activities; the shell is Kotlin and Compose.

---

## Prior art, per screen

Surveyed 2026-08-22 and 2026-08-23. **Check here before designing a screen.**

| # | Screen | Already built by |
| --- | --- | --- |
| 1 | Library | ARMSX2 `HomeScreen`, `CoverRegionIndex` |
| 2 | Game detail | ARMSX2 `GameInfo`, `PlayTime` |
| 3 | In-game overlay | ARMSX2 `EmulationMenuScreen`; **rpcsx `HomeMenu`, a page-and-component framework** |
| 4 | Screen-2 companion | **ARMSX2 `SecondScreen`, 707 lines, names the Thor** |
| 5 | Settings, global | ARMSX2, 13 tabs **plus a generated search index** |
| 6 | Settings, per game | **ARMSX2 `ConfigStore`, with three fixed bugs** |
| 7 | Cheats | **melonDS `ui/cheats`, 2,119 lines, Room + SAX + import progress** |
| 8 | Patches | ARMSX2 `ui/patches`; Cemu's symbolic assembler; xenia's TOML |
| **9** | **Storage** | **nobody. Built in `app/shell` instead** |
| 10 | Drivers | ARMSX2 `DriverManagerSection`; rpcsx `GpuDriverAdvisor` |
| 11 | Display and layout | **melonDS `ui/layouteditor`, 2,925 lines**; azahar's 8 layout modes |
| 12 | Input and hotkeys | **ARMSX2 `SysHotkey` enum + tap-to-arm binding** |
| 13 | Guest accounts | **six of eight forks** |
| 14 | Guest system UI | azahar applets; rpcsx `overlay_user_list_dialog` |
| 15 | Systems | — |
| 16 | Diagnostics | ARMSX2 `InfoTab`, `GpuInfo`, `BiosInfo`, `DeviceTier` |

### A screen this list was missing

**17. Per-game fixes.** GameThor's `gamefixes/` holds 29 per-game fixes keyed by
store ID behind six typed kinds, applied at launch.

These are **host config fixes**: they change an environment variable, a launch
argument or an INI value and touch **no guest bytes**. That makes them a fourth
kind of patch, distinct from content patches, code patches and file mods.

**A per-game driver override is this kind too**, which means screen 10 and this
one are the same mechanism seen twice. Decide whether they are one screen before
building either.

---

## 1. Library

The home screen. One list across every system.

Shows cover art, and a badge row per game: cheats available, override set, HD
pack installed, patch applied, mod or translation applied, total size.
Sortable by size, because "which game do I delete" is a common question.

**Needs from a backend:** how to identify a game from a file, its title, its
region and version, and its cover art if the backend can supply one.

## 2. Game detail

One game. Launch, plus every per-game surface.

Shows the art, the metadata, the last played time, the size breakdown, and
entry points into cheats, patches, per-game settings, storage and display
layout for this game.

**Needs:** the guest screen list for this title, so the layout picker can be
correct before launch.

## 3. In-game overlay

Reachable by a hotkey from any backend, always the same hotkey.

Save state, load state, fast forward, rewind if the backend supports it,
screenshot, the cheat list, the quick settings the current backend declares as
hot, and the performance readout.

**Needs:** the lifecycle calls, which of them the backend supports, and the
list of settings the backend marks as changeable while running.

**Rule:** a setting that needs a restart must say so before it is changed, not
after.

## 4. Screen-2 companion

What Screen-2 shows when the game uses one guest screen.

Options: the cheat list, live and toggleable; the performance readout; a
guide or notes; the storage view for this game; or nothing.

**Needs:** nothing from the backend. This is the app's own screen.

**Rule:** do not redraw an idle Screen-2 every frame. Draw on change. It costs
power and thermal headroom, and both are the budget.

## 5. Settings, global

One schema, grouped by what a person is trying to do rather than by which
subsystem owns it.

Groups: display and layout, image quality, performance and power, audio,
input and hotkeys, storage, drivers, system.

**Needs:** the backend's declared settings, each with a stable key, a type, a
default, a label, and whether it can change while running.

**Rule:** every setting has a stable key. A setting with no key cannot be
overridden per game, and a per-game override for every option is a
requirement.

**Do not design this framework. azahar and eden both already ship it**, as 40
shared files: `AbstractSetting` with typed subclasses for boolean, float, int,
short and string, then `SwitchSetting`, `SliderSetting`, `SubmenuSetting`,
`StringInputSetting`, `StringSingleChoiceSetting`, `DateTimeSetting` and
`HeaderSetting`, each with a matching view holder.

They share 90 files in total, both descending from the Citra and yuzu Android
frontends. See [`../shared_layer/ANCESTRY.md`](../shared_layer/ANCESTRY.md).
azahar is GPL-2.0-or-later and is the more permissive source.

## 6. Settings, per game

The same schema as global, with an override layer on top.

Every row shows whether it is inherited or overridden, and one action clears
the override. Resolution order is fixed: per-game value, then Thor profile,
then backend default. **One resolver, in one place.** A backend never invents
its own order.

**Needs:** nothing beyond the global schema. This screen is the resolver.

## 7. Cheats

First class, because this is a named failure of RetroArch.

A searchable library across every system, per-game view, enable and disable
without a restart where the backend allows it, and a badge in the library.

**Needs:** how the backend applies a cheat, which formats it accepts, and
whether a cheat can be toggled while running.

Five forks already support cheats in five formats. See the inventory.

## 8. Patches

Grouped by intent, because a person chasing frames and a person wanting
infinite health want different lists.

Intents: **speed**, **fix**, **change**. Each patch shows what it does, why,
and for a speed patch its measured before and after with the scene named.

**Needs:** the patch format the backend accepts and whether patches apply at
load only.

Two patch systems already exist: xenia `.patch.toml` and Cemu
`GraphicPack2Patches`. Choose one before building this screen.

## 9. Storage

Per game and across the library. Categories: game data, saves and states, HD
packs, texture cache, shader and pipeline cache, recompiled code cache, mods
and patches, cheats, screenshots.

Every action states its cost before it runs. Clearing a shader cache frees
space and brings the stutter back.

**Needs:** the backend's storage categories, their paths, and whether each is
rebuildable.

**Rule:** saves and states never sit near a bulk action.

## 10. Drivers

One pinned Turnip build is bundled and is the reference configuration.

Shows the pinned build, whether it loaded, and a per-game override with a
warning that it leaves the tested configuration. **Validates that a driver
matches the GPU generation before offering it** — the Thor is a7xx, and
a8xx builds are already sitting on the device.

**Needs:** nothing. The app owns the driver.

## 11. Display and layout

The dual-screen routing picker. Per game.

Layouts: one guest screen on each display; both guest screens on display 0;
one guest screen only; swapped. Live preview on both panels.

**Needs:** the backend's guest screen list, each with a name, a native size,
an aspect, whether it takes touch, and whether the game needs it.

**Rule:** touch follows the screen. A routing change that does not move the
touch mapping is a bug.

## 12. Input and hotkeys

**One hotkey set works on every system.** The app owns the hotkey layer and
tells the backend what happened. A backend does not define its own hotkey.

Also: controller mapping, touch overlay editing, per-game control profiles.
Cemu already ships a Thor-written per-game profile for Star Fox Zero.

**Needs:** the guest controller shape, and which guest inputs exist.

## 13. Guest accounts

**Missed in the first pass.** Xbox 360, PS3, Wii, Wii U, Switch and 3DS all
have a notion of who is playing, and it is not the same thing as a per-game
override.

Shows the guest users a backend knows about, which one is active, and lets one
be created or picked. Per system, because a Mii is not a PSN id.

**Needs:** the guest users a backend can enumerate, and a way to set the active
one.

Three forks already have this: Cemu `Account.cpp`, azahar `MiiSelector`, eden
`ProfileAdapter`. Read all three before designing.

## 14. Guest system UI

**The guest asks the host to show something, and waits for an answer.**

azahar's Mii selector is an applet: the guest OS calls out, expects a picker,
and receives a result. Every console has some of this — software keyboards,
avatar pickers, account selectors, error dialogs.

**The app renders all of it, in the app's style.** A person should not be able
to tell which backend asked. Seven backends each drawing their own system
dialogs is the inconsistency this project exists to remove.

**Needs:** a request-and-result channel. Text entry with a prompt and
constraints, user selection, error acknowledgement.

**Rule:** a guest applet request blocks the guest. Show it immediately and do
not queue it behind an animation.

## 15. Systems

Which backends are present, their versions, and PS3 as an optional separate
install.

**Needs:** the backend's identity and version.

## 16. Diagnostics

Not for a normal session, and it must exist.

The performance readout, capture triggers, the log, the crash reporter, and
the evidence bundle an agent needs. This is where the measurement discipline
surfaces in the app.

**Needs:** the counters the backend can report.

---

## The contract, as it stands from these screens

Every backend must provide:

| Area | What |
| --- | --- |
| Identity | id, name, version, the systems it handles |
| Game identity | identify a title from a file: id, title, region, version |
| Lifecycle | load, run, pause, stop, save state, load state, and which are supported |
| Guest screens | name, native size, aspect, takes touch, needed by this title |
| Guest input | the controller shape and the inputs that exist |
| Settings | key, type, default, label, changeable while running |
| Storage | categories, paths, rebuildable or not |
| Counters | what it can report for the performance readout |
| Cheats | formats accepted, toggleable while running or not |
| Patches | format accepted, apply at load only or not |
| Guest users | enumerate them, set the active one |
| Guest system UI | request text, a user selection, or an error acknowledgement |

A backend may additionally **declare extensions**, and the app shows the UI
only when present. Cemu declares graphic packs. melonDS declares three filter
planes. ARMSX2 declares two texture classes. None pretends to be the others.

## Open questions this raises

1. Cover art: **partly answered.** xenia `XeniaCoverArt.java`, 406 lines,
   downloads a database from `xenia-manager/x360db`, caches it for 7 days,
   extracts an 8-hex-digit title id with a regex and matches alternative ids.

   So the pattern is: **an external per-system database keyed on title id**,
   cached locally.

   **SURVEYED 2026-08-25, and the premise above was too pessimistic. Six of
   eight systems carry their own art in the dump**, so an external database is
   the **fallback**, not the plan.

   | Tier | Source | Systems | Fork |
   | --- | --- | --- | --- |
   | **1** | **embedded in the dump** | 3DS, Switch, DS/DSiWare, Vita, PS3, Wii U | azahar and eden `GameIconUtils`, **melonDS `RomIconBuilder`** from the DS banner, Vita3K `apps_list.cpp`, rpcsx |
   | 2 | external database by title id | **Xbox 360** | xenia `x360db`, cached 7 days |
   | 3 | a user-supplied file | **override, every system** | ARMSX2 `GetCoverImagePathForEntry` |

   **Corrected 2026-08-24: PS2 has a tier-2 source too** — ARMSX2's `AGENTS.md`
   defaults cover art to **xlenore's PS2/PS1 cover repositories** and calls it a
   hardcoded default to preserve. **Missed first time by reading the fork's
   desktop C++ instead of its Android frontend**, which is where the product
   decisions live.

   **And the badge question is answered by the same file.** A cheat badge must
   work **before a game has ever booted**, so ARMSX2 ships
   `assets/cheats/index.tsv` mapping bundled CRC filenames to serials and titles,
   with **`CheatPresenceIndex` owning the indexing** and an explicit rule to
   invalidate it whenever PNACH files are imported, installed or deleted.
   **Badges come only from real `.pnach` files — never inferred from widescreen,
   60 FPS, compatibility or patch folders**, which is the cheat-versus-fix
   distinction reaching the library screen.

   **Tier 1 always succeeds, offline, and is always right for the copy in
   hand.** Its limit is size — a DS banner icon is 32x32 and a 3DS SMDH icon
   48x48, which is small for a grid read at arm's length — **so tier 2 is an
   upgrade rather than the source.** That maps onto `GAME_DATA.md`'s layers
   exactly: **derived, scraped, user.**

   **And all three implementations key their icon cache on the file path** —
   azahar and eden in Kotlin, ARMSX2 in C++. `GAME_DATA.md` rejects
   path-as-identity already; **for a cache the failure is worse and inverted:
   two dumps at one path collide, so replacing a file serves the old game's
   icon.** The icon is a pure function of the dump, so it keys on `DumpId` and
   belongs in `ArtifactStore`.

   See [`../research_log/20260825_0900_cover_art_has_three_sources_and_all_three_key_on_the_path.md`](../research_log/20260825_0900_cover_art_has_three_sources_and_all_three_key_on_the_path.md).
2. Game identification: **SURVEYED 2026-08-25, and the answer is not eight
   schemes.** Every fork carries a real title id — ARMSX2 a disc `serial`, eden
   a `programId`, xenia an 8-hex title id, Vita3K and Cemu a `TITLEID` — which
   `GAME_DATA.md` already assumed. **What nobody had looked at is what happens
   to a game that has no title id, and that is where every design breaks.**

   **eden's `Game` class holds three identities, the first two four lines
   apart:** settings key on `programId`, **play history keys on `path`**, and
   when `programId` is 0 — homebrew, a bad dump, an unrecognised file —
   **`settingsName` falls back to the filename.** So a rename loses your play
   history, and for homebrew it loses your settings.

   **`GameKey` here had the right shape and no defined behaviour for the case
   that breaks it.** Fixed: `GameKey.forUnidentifiedDump` keys on the **content
   hash**, marked `isDerivedFromDump` so the UI can say the game is
   unrecognised. **A `DumpId` survives a rename; a filename does not.** The cost
   is stated: two near-identical homebrew copies become two entries, which is
   wrong in one direction rather than both.

   See [`../research_log/20260825_1000_one_class_three_identities.md`](../research_log/20260825_1000_one_class_three_identities.md).
3. Is rewind in scope? **ANSWERED 2026-08-25: yes, as a declared per-backend
   extension that must declare its PER-STATE COST.** melonDS has a complete
   implementation — configured in seconds rather than slots, a screenshot per
   state so it is a timeline the user scrubs, and **20.4 MB per state,
   preallocated, for the smallest guest in the fleet.** A Switch or Wii U state
   is a different order of magnitude, which is why it cannot be a uniform
   feature. The one memory-budget owner must see the window.
   See [`../research_log/20260825_0410_rewind_and_the_integrity_mode_nobody_designed.md`](../research_log/20260825_0410_rewind_and_the_integrity_mode_nobody_designed.md).
4. What does the performance readout show by default? **ANSWERED 2026-08-25:
   three lines, and none of them is a bare "FPS".** ARMSX2 shows **FPS, VPS and
   Speed as three different numbers** — frames drawn, the guest's own vsync
   rate, and percent of nominal — and a game rendering 30 into a 60 Hz display
   reads 30 and 60 with neither wrong. So: **guest rate and host rate, labelled
   and backend-declared; frame time as a `[min..max]` RANGE rather than a mean,
   which `PerformanceMetrics` already computes and the OSD already discards;
   and the thermal state beside them.** Take ARMSX2's 19 toggles as the optional
   set. **The default is the decision; the list is not.**
   See [`../research_log/20260825_0800_what_the_in_game_readout_should_show.md`](../research_log/20260825_0800_what_the_in_game_readout_should_show.md).

### A rule screen 3 needs and did not have

**A setting reachable from an in-game menu must take effect WHILE PAUSED,
because paused is the only state in which that menu is open.**

ARMSX2's OSD rebuilt its line strings only while the VM ran, so **every overlay
toggle looked inert at the exact moment a person used it** — its own comment
says *"which is why toggling in the menu looked inert"*. Its invalidation also
noticed the enabled set **emptying** and not **changing**, so turning one line
off and another on left the old text on screen.

**That is the eleventh mechanism in
[`../shared_layer/DID_IT_APPLY.md`](../shared_layer/DID_IT_APPLY.md), and the
first that is a rendering cache rather than a configuration store.**
