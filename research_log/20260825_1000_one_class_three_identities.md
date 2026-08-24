# eden's `Game` holds three identities four lines apart, and the fallback is the filename

**Goal: answer the last open question in `app/SCREENS.md` — "game
identification: every fork does it differently and none was surveyed".**

## The survey, and where it landed

**Reading each fork's game-list entry type rather than counting vocabulary**, the
answer is not "eight different schemes". It is **two schemes and a fallback**,
and the fallback is the interesting part.

| Fork | Identity it carries | In |
| --- | --- | --- |
| ARMSX2 | **`serial`** beside `path` | `pcsx2/GameList.h:78-79` |
| eden | **`programId`** beside `path` | `model/Game.kt:28-30` |
| xenia | 8-hex-digit title id | `XeniaCoverArt.java` |
| Vita3K | `TITLEID` | cheat paths |
| Cemu | `titleid` | throughout |

**Everyone has a real title id. `GAME_DATA.md` already said so.** What nobody
had looked at is **what happens to the games that do not have one.**

## eden answers that question, and one 30-line class holds three identities

```kotlin
class Game(val title: String, val path: String, val programId: String = "", ...) {
    val keyAddedToLibraryTime get() = "${path}_AddedToLibraryTime"
    val keyLastPlayedTime     get() = "${path}_LastPlayed"

    val settingsName: String get() {
        val programIdLong = programId.toLong()
        return if (programIdLong == 0L) FileUtil.getFilename(Uri.parse(path))
               else "0" + programIdLong.toString(16).uppercase()
    }
```

**Three identities, and the first two are four lines apart:**

| Data | Keyed on | Survives a rename? |
| --- | --- | --- |
| **settings** | `programId` | **yes** |
| **added-to-library time, last-played time** | **`path`** | **no** |
| settings, **when `programId` is 0** | **the filename** | **no** |

> **Move or rename a file in eden and you keep your settings and lose your play
> history.** Do the same to a homebrew title and **you lose the settings too**,
> because the fallback is the filename.

**The fallback is the finding.** `programId == 0` is not an edge case — it is
**homebrew, a bad dump, and any file the parser did not recognise**. eden must
return something, because refusing would mean the library cannot list the file
at all. **It returns path identity by another name**, in the one place the rest
of the class had avoided it.

## What this project's own model did with the same question: nothing

`GameKey` in `app/shell/GameData.kt` required `titleId.isNotBlank()` **and said
nothing about where a blank one comes from.** The design has the right shape and
**no defined behaviour for the case that actually breaks it.**

**Fixed. The fallback is the content hash, not the filename**:
`GameKey.forUnidentifiedDump(system, dumpId)`, marked `isDerivedFromDump` so the
UI can tell the person their game is unrecognised rather than leaving them to
wonder why it has no cheats and no metadata.

**A `DumpId` is stable under rename and move. A filename is not. That is the
only property that matters here.**

**The cost is real and is stated rather than hidden:** two copies of one
homebrew title that differ by a byte become two library entries, because without
a title id nothing says they are the same game. **A filename fallback would
merge them by accident and split them on rename — wrong in both directions
rather than one.**

Four tests, one of which is just `forUnidentifiedDump` twice with the same hash.
**208 tests, 0 failures.**

## The pattern across today

**Three separate places in this fleet key durable per-game data on a path**, all
found today:

| | Keyed on path | Consequence |
| --- | --- | --- |
| azahar, eden `GameIconUtils` | icon cache | two dumps at one path serve the wrong icon |
| ARMSX2 `GameListModel` | cover pixmap cache | same |
| **eden `Game`** | **play history, and settings for homebrew** | **a rename loses user data, not just a cache** |

**The first two are caches and recover.** The third does not.

## Limits

- **Five forks' entry types read. melonDS, azahar, rpcsx and GameThor were
  not** — their identity handling is asserted from earlier work in this repo,
  not from reading their game-list types today.
- **eden's behaviour is read from the model class, not observed.** Whether the
  play-history keys are actually written under those strings was not traced to
  the store.
- **Nothing here is a bug report against eden.** It is a design that works until
  a file moves, which is a reasonable trade for a desktop emulator and a poor
  one for a handheld whose content lives on a removable card.

## Sources

- eden `src/android/app/src/main/java/org/yuzu/yuzu_emu/model/Game.kt:24-55`
- ARMSX2 `pcsx2/GameList.h:75-83`
- `app/GAME_DATA.md`, `app/shell/GameData.kt`
