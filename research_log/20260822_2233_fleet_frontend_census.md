# Fleet frontend census, and a correction to my own correction

**Goal: measure every fork's Android frontend, because the ARMSX2 read was
productive and nobody had measured the others either.**

Session 2026-08-22 22:33.

**Result: melonDS-android is the largest frontend in the fleet and the best
architected. The claim committed 30 minutes ago that ARMSX2's is "the most
complete shell in the fleet" is wrong.**

---

## The census

Counted per `src/main/java` root, excluding tests, vendored trees and sample
apps bundled inside dependencies.

| Fork | Lines | Files | Lines/file |
| --- | --- | --- | --- |
| **melonDS-android** | **78,033** | **698** | 112 |
| **ARMSX2** | 63,111 | 153 | 412 |
| ARMSX3, `armsx3-ui` | 61,319 | 157 | — copy of ARMSX2's |
| eden-thor | 33,671 | 217 | 155 |
| dolphin-thor | 31,791 | 242 | 131 |
| melonds_HD_2 | 37,375 | 557 | 67 |
| azahar-thor | 26,919 | 156 | 173 |
| Vita3K-Thor | 20,744 | 76 | 273 |
| Cemu-thor | 18,501 | 144 | 128 |
| rpcsx-ui-android | 18,407 | 85 | 217 |
| **xenia-thor** | **12,334** | **25** | 493 |
| GameThor | 137,165 | 643 | — a PC launcher, different thing |

**`CLAUDE.md` called xenia "the most complete shell in the fleet". It is the
smallest Tier 1 frontend, by a factor of 6 against melonDS-android.**

**And my correction to that was also wrong.** I measured ARMSX2 against xenia
and stopped, which is the same mistake one step along: comparing against the
claim rather than against the field.

---

## melonDS-android is a properly layered Android app

| Lines | Files | Package |
| --- | --- | --- |
| 51,288 | 307 | `ui` |
| 15,492 | 97 | `impl` — repository implementations |
| 3,160 | 48 | `common` |
| 2,491 | **120** | `domain` — entities and interfaces |
| **1,296** | **37** | **`migrations`** |
| 840 | 8 | `di` — Hilt |
| 806 | 31 | `database` |
| 565 | 10 | `parcelables` |

`domain` and `impl` separated, dependency injection, a database layer, and a
migration framework. **ARMSX2 has none of those layers**; it is 153 large files
with repositories as top-level singletons.

**Neither ranking is simply "better".** They answer different questions:

- **melonDS-android has the better architecture.** If the app wants layering,
  DI and testability, this is the reference.
- **ARMSX2 has the better feature set for this project**, because it holds the
  per-game override system, the Thor-specific work, the second-screen panel and
  the hotkey enum.

**Take the structure from melonDS-android and the features from ARMSX2.** That
is a more useful conclusion than either superlative.

### Correction, made while writing this: melonDS-android has the cheat manager

The paragraph above originally ended "neither has a cheat manager UI or a
storage view". **Wrong, and wrong in the same session it was written.**

`ui/cheats` is 2,119 lines across 20 files: `GameListScreen`, `FolderListScreen`,
`CheatListScreen`, `EnabledCheatsListScreen`, `CheatsScreen`, a `CheatForm` with
its own form-state model, per-item composables, navigation and a loading screen.
`domain/model` has `Cheat`, `CheatDatabase`, `CheatFolder`, `CheatInFolder` and
`CheatImportProgress`. `impl` has `RoomCheatsRepository` at 304 lines,
`XmlCheatDatabaseSAXHandler` at 217 and `BundledCheatDatabaseImporter` at 103.

**`CheatImportProgress` and a SAX parser together say this was used in anger.**
SAX is a streaming parser, chosen when the document does not fit comfortably in
memory, and a progress model exists because the import is slow enough to need
one. Those are the marks of a real cheat database, not a demo.

`ui/layouteditor` at 2,925 lines is also present, which is SCREENS.md screen 11.

**Storage aggregation is now the only screen with no prior art anywhere in the
fleet.**

---

## The finding: settings migrations are already solved

I added `SETTINGS_SCHEMA_VERSION = 1` earlier today and wrote that "nothing here
says how" migrations work. **melonDS-android says how, in 37 files.**

`migrations/Migration.kt` is the whole interface:

```kotlin
interface Migration {
    val from: Int
    val to: Int
    fun migrate()
}
```

`Migrator` registers migrations, refuses a duplicate `from`, sorts by `from`,
runs those in range, then records the new version. Sixteen concrete migrations
exist, from `Migration6to7` to `Migration40to41`.

### Three design points worth taking

**1. The schema version is the app's own version code.**

```kotlin
private fun getCurrentVersion(): Long =
    PackageInfoCompat.getLongVersionCode(packageInfo)

private fun getLastVersion(): Long =
    // 6 is the version at which migrations started being supported
    sharedPreferences.getLong("last_version", 6)
```

**There is no separate schema number to forget to bump.** My
`SETTINGS_SCHEMA_VERSION = 1` is a number somebody has to remember; this is not.
The default of 6 also handles installs that predate migrations, which is a case
a fresh constant cannot express.

**2. `legacy/` freezes the old data shapes.** It holds `Rom21`, `Rom22`,
`RomConfigDto25`, `RomConfigDto31`, `RomDto25`, `RomDto31` and
`RomGbaSlotConfigDto31`, plus `input` and `layout` subdirectories.

**This is the part that is easy to get wrong.** A migration must not deserialize
using the *current* data class, because the current class keeps changing and the
migration then breaks retroactively — silently, and only for users upgrading
from an old version, which is the hardest case to test. **Freeze a DTO per
version and let the migration read that.**

**3. A migration is tiny and states its reason.** `Migration40to41` in full:
RetroAchievements usernames were stored with leading or trailing whitespace, the
backend trimmed them and accepted them, so stored signatures were invalid. Six
lines of code and four lines of why.

---

## What changes

1. **Correct `CLAUDE.md` again.** melonDS-android is the largest and
   best-layered frontend; ARMSX2 has the features this project wants; xenia is
   the smallest, not the most complete.
2. **Drop `SETTINGS_SCHEMA_VERSION` as a constant.** Use the app version code,
   with a floor for pre-migration installs.
3. **Record the frozen-DTO rule in the contract**, because it is invisible until
   it bites and it bites only on upgrade.

---

## Method note

**This is the second time today a "most complete" claim was made without
measuring the field**, and the second one was mine, made while correcting the
first.

The cheap rule that would have caught both: **before writing a superlative,
count all the candidates.** The census above took one command.
