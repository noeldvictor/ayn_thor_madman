# Game data, covers and the API for both

**Internal specification. Written 2026-08-23.**

**Short answer to "mimic EmulationStation or do better": take its vocabulary,
reject its data model.** ES makes two foundational choices that are wrong here,
and both are cheap to avoid at the start and expensive to fix later.

---

## 1. What EmulationStation does, and where it breaks

ES keeps a `gamelist.xml` per system. Each entry carries `path`, `name`,
`desc`, `image`, `marquee`, `thumbnail`, `video`, `rating`, `releasedate`,
`developer`, `publisher`, `genre`, `players`, `playcount`, `lastplayed`,
`favorite`, `hidden`, `sortname`.

**What is good and worth keeping:**

- **Named media roles.** `image`, `marquee`, `thumbnail` are *roles*, not
  files, so a theme binds to a role and any art satisfying it works. **Keep
  this exactly.**
- **A small, boring metadata vocabulary.** Nothing exotic, everything a person
  recognises.
- **Human-editable.** Somebody can fix a wrong title by hand.

**What breaks here, and why it matters:**

| ES choice | Why it fails on this device |
| --- | --- |
| **Identity is the file path** | Move or rename a dump and **all metadata, art, cheats and per-game settings are orphaned**. On Android, scoped storage moves things without asking. |
| **XML parsed at startup** | A large library stalls the first frame. **The UI must be cheap** — that rule applies to loading too. |
| **Media as loose files beside ROMs** | Many small reads through Android's storage layer, the slowest way to fetch art. |
| **Video snaps as a first-class role** | Full-screen video behind a menu is the worst thing on a tiler. **Already ruled out.** |
| **No link to emulator state** | ES has no idea a game has cheats, an override, an HD pack or 6 GB of cache. **Those are this app's badges.** |

**The path-identity problem is the decisive one.** The fleet already rejected it
without coordinating: **Vita3K keys cheats by `TITLEID`, ARMSX2 by disc serial,
xenia by an 8-hex-digit title id.** Three forks, three consoles, one answer.
**ES is the outlier.**

---

## 2. Identity

**A game is identified by what it *is*, never by where it sits.**

```
GameKey  = (system, titleId)          // canonical. Stable forever.
DumpId   = contentHash                // this specific file
```

**`GameKey` is the identity.** A PS2 disc serial identifies its game whether the
file sits on internal storage, on an SD card, renamed, or re-dumped. **Every
per-game thing keys off this**: overrides, cheats, patches, saves, art,
playtime, storage accounting.

**`DumpId` is a separate question — *which copy*.** It answers "is this the
version this patch was made for", which is the problem
[`CLAUDE.md`](../CLAUDE.md) records three forks solving separately (Cemu matches
a loaded `RPLModule`, rpcsx hashes, eden uses a 32-byte `BuildID`).

**Both are needed and they are not the same.** A cheat targets a `GameKey`; a
code patch usually targets a `DumpId`.

**The backend computes both**, because extracting a serial from a PS2 disc and a
title id from a 3DS NCCH are guest knowledge. **The app never parses a ROM.**

## 3. Metadata is layered, and the order is already proven

**Same resolution order as settings**, because it is the same problem and that
design already has tests and three fixed bugs behind it.

| Layer | Source | Wins over |
| --- | --- | --- |
| **user** | edited by the person | everything |
| **scraped** | fetched from a database, cached | bundled |
| **bundled** | shipped with the app | derived |
| **derived** | parsed from the file itself | — |

**A field resolves independently.** Someone fixing a wrong title does not lose
the scraped release date. **This is the sparse-override rule from settings**,
and it is sticky for the same reason: an edited field stays edited even if it
happens to match the scraped value.

**Fields, deliberately few:**

```
title            sortTitle       system        region
releaseYear      developer       publisher     genre
players          description
```

**No rating, no playcount-as-metadata, no favourite-as-metadata.** Rating is
opinion, and the last two are *state*, not metadata — they live with the user's
library state, not the game's description.

## 4. Media roles

**ES's idea, trimmed to what a tiler can afford.**

| Role | What it is | Used by |
| --- | --- | --- |
| `cover` | box art, portrait | the game list and detail |
| `logo` | title treatment, transparent | system strip, detail header |
| `screenshot` | in-game still | detail |
| `banner` | wide art | optional, detail header |

**No `video`.** A video snap is per-frame decode and full-screen fill behind a
menu. **It is the single most expensive thing an ES theme does** and it is
excluded by the cheap-UI rule.

**A role is a request, not a file.** Nothing in the UI holds a path.

## 5. The API

**The shape follows from two rules already set**: cover art is decoded once at
display size, and the UI must never stall.

```kotlin
// Identity — from the backend, never from the app parsing a file.
data class GameKey(val system: System, val titleId: String)

// One row of the library. Cheap to hold: no bitmaps, no paths.
data class GameEntry(
    val key: GameKey,
    val meta: GameMeta,          // resolved through the four layers
    val badges: Badges,          // cheats / override / pack / patch
    val sizeOnDiskMb: Int,
)

interface GameIndex {
    /** The library is per console, so this is the primary query. */
    fun forSystem(system: System): List<GameEntry>
    fun all(): List<GameEntry>
    fun get(key: GameKey): GameEntry?
}

interface MediaStore {
    /**
     * Art at the size it will be drawn.
     *
     * Takes a size because decoding a 1200px cover to draw it at 96px is the
     * mistake the cheap-UI rule exists to prevent. Returns null while it is
     * not resident — the caller draws a placeholder and is recomposed when it
     * arrives. Never blocks.
     */
    fun cover(key: GameKey, role: MediaRole, widthPx: Int, heightPx: Int): ImageHandle?

    /** What art exists, without loading any of it. For badges and pickers. */
    fun available(key: GameKey): Set<MediaRole>
}
```

**Three deliberate choices:**

**Requests carry a size.** The store decodes to that size once and caches the
result. **A path-based API cannot do this** — it hands back a file and the
caller scales per frame.

**`cover()` returning `null` is normal, not an error.** It means *not resident
yet*. The list draws a flat placeholder and never waits.

**`available()` exists separately** so the UI can show that art exists without
paying to load it — the same split as `candidates()` and `locate()` in the
content resolver, and for the same reason.

**Sizes are bucketed to 32 px before they become a cache key.** Without it, a
row that measures 97 px on one layout pass and 100 px on the next decodes the
same cover twice, and a resize would decode it every frame. **Bucketing rounds
up**, so art is downscaled at draw time and never upscaled.

**It narrows the window rather than closing it.** 96 and 97 straddle a bucket
edge and still decode twice. Closing it completely would mean snapping sizes at
layout, which is a bigger change than the problem justifies today. **The test
suite pins the limit rather than hiding it.**

## 6. Storage

**One index, not per-system XML.**

- **The index is a database**, queried per system. melonDS already uses Room
  for exactly this shape and it is the right call.
- **Art is content-addressed on disk**, one directory, filename derived from
  `GameKey` and role. **Not stored beside the ROM**, so moving a dump costs
  nothing.
- **Decoded art is cached in memory at display size**, bounded, evicted by
  least-recently-used.
- **Scraped data is cached with an expiry.** xenia's cover art fetcher already
  does this — `games.json`, cached **7 days** — and that is the pattern.

**Where content actually lives is a solved problem.** Use the resolver taken
from Vita3K: enumerate roots, build candidates, resolve, and **report which
root answered**. See `ContentResolver` in `app/shell`.

## 7. What this buys that ES does not

- **Moving a ROM loses nothing.** Identity is the title, not the path.
- **The first frame does not wait on a library scan.**
- **Badges are real.** ES cannot tell you a game has an override and 6 GB of
  shader cache. **This app's whole storage screen depends on that link.**
- **A patch can target a specific dump** while a cheat targets the title.
- **Art is never decoded twice**, and never at the wrong size.

## 8. Open, and honest about it

- **Where scraped data comes from is not decided.** ScreenScraper, TheGamesDB
  and per-system databases all have terms; **check the licence before shipping
  a scraper**, the same gate as cheat databases.
- **No sizes are measured.** "Decode once at display size" is a rule with no
  number behind it yet, and the number needs the device.
## 9. What is built

**The types and the resolvers are built and tested. Nothing else is.**

In `app/shell`: `GameData.kt` holds `GameKey`, `DumpId`, the metadata layers and
resolver, the media roles, and `MediaRequest` with its bucketed cache key.
`MediaCache.kt` holds `CoverArt`, a bounded LRU `InMemoryMediaStore` and the
`MediaSource` it decodes through. `Cover.kt` draws a slot.
**36 tests** pin the decisions above.

**The cover UI is built**, in three sizes: a 34x46 dp thumbnail in each library
row, a 132x180 dp cover in the metadata panel, and a 116x158 dp cover on the
detail screen. **Three sizes is three cache entries on purpose** — the large one
must not be an upscaled thumbnail.

Four decisions the UI forced, none of which were in the specification before it
was drawn:

- **The pixel size is derived from `Dp` and the density, not measured.**
  Measuring means a zero-size first pass, then a decode, then a recomposition,
  for every row on every scroll.
- **`CoverArt` stores a hue, not a colour.** A resolved colour would be wrong
  the instant the person switches theme, and fixing it would mean discarding
  the entire cache. **The cache now survives a theme switch**, which is the
  cheapest possible response to a toggle.
- **The slot is fixed and art fits inside it.** Box art aspect differs per
  system, and letting it decide row height is what makes a lazy list expensive.
- **No crossfade when art arrives.** A per-row animation is per-frame work for
  a cosmetic transition, and a scroll would run dozens at once.

**The empty state is a first-class path**, not an afterthought: two of the seven
fake games have no cover, so the placeholder is always on screen. Empty is the
common case for a library of personal dumps.

**The shell no longer keys anything on a title.** `Game` carries a `GameKey`,
its title is a resolved metadata value, and the library list is keyed by
`storageId`. That was the ES mistake in miniature and it is gone.

**Not built:** the index database, real image decoding, the scraper, and any
on-disk format. The store is real; what it decodes is a placeholder.

**No sizes are measured** — "decode once at display size" is a rule with a test
behind it now, but no number. The number needs the device.
