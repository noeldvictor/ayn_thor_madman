# GameThor: the fork with no capability recorded, and it has the community-settings feature

**Goal: close the last "no capability recorded at all" gap in
`capability_inventory.md`.**

Session 2026-08-23 00:55. GameThor was carried as Tier 2 and never surveyed.

**Result: it already ships the per-game known-good-settings feature `CLAUDE.md`
specifies, with a typed taxonomy. And it is the fork that proves the
translate-don't-emulate thesis.**

---

## What it is

| Part | Lines | What |
| --- | --- | --- |
| `app/gamenative/` | 104,740 | GameNative: Steam, GOG and Epic integration, library, container management |
| `com/winlator/` | 32,268 | Winlator: the Wine and X server layer |
| `org/freedesktop/` | 157 | |

**It is not an emulator.** It runs Windows games through Wine and translates
Direct3D to Vulkan through DXVK, on the same Adreno 740.

**That makes it the existence proof for a claim already in `CLAUDE.md`.** xenia's
ledger records the standing conclusion that its own gap is HLE against LLE,
"proven by RE2 Remake running on the same Thor via GameNative/DXVK", and that
every incremental GPU lever is `DEAD` or `FLAT` because it patches the emulator
instead of replacing it.

**GameThor is that proof sitting in the fleet, and the inventory had nothing
about it.**

---

## The finding: per-game fixes, typed and keyed by store ID

`app/gamenative/gamefixes/` — **29 fix files**, plus a registry and a type
hierarchy.

**Six fix kinds:**

```
GOGDependencyFix   IniFileFix        KeyedCompositeGameFix
LaunchArgFix       RegistryKeyFix    WineEnvVarFix
```

**Keyed by source and store ID**, one file per game, each with a comment naming
the game:

```kotlin
/** Stardew Valley (Steam) */
val STEAM_Fix_413150: KeyedGameFix = KeyedWineEnvVarFix(
    gameSource = GameSource.STEAM,
    gameId = "413150",
    envVarsToSet = mapOf("WINEDLLOVERRIDES" to "icu=n"),
)
```

`GameFixesRegistry` dispatches them; `XServerScreen` applies them at launch.

### This is a requirement this repo wrote down and did not know was built

`CLAUDE.md`, [Quality of life](../CLAUDE.md):

> **Known-good settings from the community.** A game should arrive with settings
> that are already known to work, rather than defaults a person has to discover.
> Gather them, record where each came from, and let a person override any of
> them.

**GameThor has the first two thirds**: settings that are known to work, arriving
with the game, keyed to it. It does not record provenance — no file says who
found the fix or why `icu=n` is needed.

### What transfers and what does not

**The fix kinds do not transfer.** `WineEnvVarFix` and `RegistryKeyFix` are
Wine concepts. A PS2 backend has no registry.

**The shape transfers completely:**

- a **stable game key** — here `SOURCE_ID`, in this project a title ID
- a **small set of typed fix kinds** rather than free-form configuration
- **composition**, through `KeyedCompositeGameFix`, so one game can carry
  several fixes
- **one file per game**, readable, with the game named in a comment

### The one design point to change

**GameThor's fixes are code. They should be data.**

Code gives type safety and composition, and costs a rebuild to add a fix.
`CLAUDE.md` requires that installing a patch takes **one action in the app**, so
a fix that needs a new APK fails that test.

**Take the taxonomy, not the mechanism.** Typed kinds, declared per backend,
serialised as data the app can ship or fetch without rebuilding.

**And add the provenance field GameThor lacks.** The requirement says to record
where each setting came from, and a fix with no source cannot be re-derived when
it stops working.

---

## Where it sits against the patch work

`CLAUDE.md` already separates three things called "patch": content patches, code
patches and file mods. **GameThor's fixes are a fourth thing: host-side
configuration.** They change no guest bytes at all — they set an environment
variable, a registry key, a launch argument or an INI value.

| Kind | Changes | Example |
| --- | --- | --- |
| content patch | guest filesystem | an update or DLC |
| code patch | guest instructions | 60 FPS, infinite health |
| file mod | guest assets | a texture pack |
| **host config fix** | **the host's setup for one game** | **`WINEDLLOVERRIDES=icu=n`** |

**A per-game driver override is exactly this fourth kind**, and so is a per-game
thread policy or a per-game present mode. **The repo has been treating those as
settings and treating fixes as patches, and they are the same thing.**

That is a small unification and it removes a category from the design.

---

## Not read

- **Winlator's 32,268 lines.** The Wine and X server integration was not opened.
- **Whether GameThor's container management has anything to say about the
  storage view.** It manages Wine prefixes per game, which is a large per-game
  storage category nobody else has.
- The licence of the bundled DXVK, Wine and Box64 components. **GameThor is
  GPL-3.0 per `CLAUDE.md`, but its third-party stack was not checked**, and
  `THIRD_PARTY_NOTICES` exists at its root.
