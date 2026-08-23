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

**Mine `xenia-thor` for features. Do not copy its structure.**

It has the most **complete** Android shell in the fleet, 12,313 lines of Java.
It also has the **worst** structure, and the two facts are related.

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
   cached locally. That generalises. What does not generalise is `x360db`
   itself, which is Xbox 360 only. Every system needs its own source, and
   nothing has been surveyed for the other seven.
2. Game identification: every fork does it differently and none was surveyed.
3. Is rewind in scope? Only some backends can do it, and it is expensive.
4. What does the performance readout show by default, given cross-run numbers
   are `CONFOUNDED` and a naive fps counter invites exactly the wrong
   conclusion?
