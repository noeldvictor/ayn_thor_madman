# Cover art has three sources, the dump is the primary one, and all three fork implementations key their cache on the file path

**Goal: answer `app/SCREENS.md` open question 1.** That question, quoted from
2026-08-22, assumed every system needs its own art source and recorded that
seven of the eight were unsurveyed. **This is the survey.**

**Surveyed. The premise was too pessimistic: most systems carry their own art,
so an external database is the FALLBACK rather than the plan.**

## Three tiers, and the fleet demonstrates all three

**The instrument, named:**

```sh
git -C <fork> grep -loI --recurse-submodules -iE   'ICON0\.PNG|icon0|smdh|iconTex|GetIcon|icon_data|banner|nacp|title_image|GetGameIcon'   -- '*.cpp' '*.h' '*.kt' '*.java'
```

**Run over all nine forks**, vendored trees removed by excluding `externals?/`,
`third_party` and `3rdparty`, **then every surviving hit read.** Two of the
matches were noise and are excluded on that basis: Cemu's were vendored `imgui`,
and rpcsx's `banner` hits were `sys_ppu_thread`.

| Tier | Source | Systems | Fork evidence |
| --- | --- | --- | --- |
| **1** | **Embedded in the dump** | 3DS, Switch, DS and DSiWare, Vita, PS3, **Wii U** | azahar `GameIconUtils`; eden `GameIconUtils` + `game_metadata.cpp`; **melonDS `RomIconBuilder`**, building from the DS banner's `Icon` and `Palette`; Vita3K `apps_list.cpp`; rpcsx |
| 2 | **An external database keyed on title id** | **Xbox 360** | xenia `XeniaCoverArt.java`, 406 lines, `xenia-manager/x360db`, cached 7 days |
| 3 | **A user-supplied file** | **PS2** | ARMSX2 `GameList::GetCoverImagePathForEntry` |

> **Six of eight systems carry their own art. The two that need an external
> source are the two disc-based Western consoles.**

**So the primary path is: read the icon out of the dump.** No network, no
database, no per-system scraper, and it is **always correct for the copy in
hand** — which the other two tiers are not.

**Cemu is the gap, not the counter-example.** A Wii U title carries
`meta/iconTex`, and **searching Cemu's `src/` for `iconTex` returns nothing**, so
Cemu does not extract it. That is a missing feature in one fork rather than a
missing capability in the format.

## The nuance that keeps tier 2 alive

**An embedded icon is small.** A DS banner icon is 32x32 and a 3DS SMDH icon is
48x48. **A library grid read at arm's length on a 1080p panel wants more than
that**, which is the whole reason box-art databases exist.

> **Tier 1 is the guaranteed baseline: every game gets art, offline, correct.
> Tier 2 is an upgrade where a database exists. Tier 3 is the override.**

**That maps onto `GAME_DATA.md`'s existing metadata layers exactly** — user,
scraped, bundled, derived — with **tier 3 = user, tier 2 = scraped, tier 1 =
derived.** The layering was already specified; **what was missing is that the
derived layer is the one that always succeeds.**

## And all three implementations make the same mistake

```kotlin
// azahar GameIconUtils.kt:45
override fun key(data: Game, options: Options): String = data.path
// eden GameIconUtils.kt:57
override fun key(data: Game, options: Options): String = data.path
```

```cpp
// ARMSX2 GameListModel.cpp:171-176
m_cover_pixmap_cache.Insert(std::move(path), std::move(pm));
void GameListModel::invalidateCoverForPath(const std::string& path)
```

**Three forks, two languages, one key: the file path.**

**`app/GAME_DATA.md` already rejects path-as-identity**, on the grounds that
moving or renaming a dump orphans its metadata. **For a CACHE the failure is
worse and in the other direction: two different dumps at the same path collide,
so replacing a file serves the previous game's icon.**

**The fix is already specified here and unused.** An icon decoded from a dump is
**a pure function of the dump's content**, which is `DumpId`, and the `PERSIST`
rule says such an artifact should be computed once. **`ArtifactStore.kt` is the
mechanism; nothing points the cover cache at it.**

**And azahar and eden are the shared-ancestry signal again**, like
`InputOverlay*` and the settings framework: **the same three types — a Coil
`Fetcher`, a `Keyer`, a `MemoryCache` — in both, from Citra and yuzu.** Both
upstreams are dead, so **nobody but this project will ever reconcile them.**

## What this does not settle

- **Which database serves which system.** Only xenia's `x360db` was found, and
  PS2 has none in the fleet — ARMSX2 makes it the user's problem. **No web
  search was done; this is a survey of the forks only.**
- **Cemu's `iconTex` absence** is from one search of `src/`. It is a strong
  negative for that spelling and not proof Cemu has no other path to the icon.
- **Icon sizes are quoted from the console formats, not measured** from a dump —
  no dump was opened.
- **Nothing was built.** The cover cache is not wired to `ArtifactStore`.

## Sources

- azahar `src/android/.../utils/GameIconUtils.kt:45`
- eden `src/android/.../utils/GameIconUtils.kt:57`, `jni/game_metadata.cpp`
- melonDS `app/src/main/cpp/RomIconBuilder.cpp`, `MelonDSNandJNI.cpp:1126`
- ARMSX2 `pcsx2-qt/GameList/GameListModel.cpp:144,171,176`
- xenia `XeniaCoverArt.java`
- `app/GAME_DATA.md`, `app/shell/ArtifactStore.kt`
