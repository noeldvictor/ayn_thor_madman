# A `path_as_identity` sweep: 19 hits, all read, no new instance — and the line it drew

**Goal: the path-as-identity pattern was found three times today by reading.
Turn it into a class and see whether a tool can find a fourth.**

**It cannot, and the attempt produced something better: a rule for telling the
correct uses from the incorrect ones.**

## The sweep, and what it took to make it honest

| version | hits | outcome |
| --- | --- | --- |
| first | **19 across 8 forks** | **GameThor 16, every one a `Timber.tag(...).d("... ${file.path}")`** |
| + `exclude` for logging | 14 | GameThor's remaining 4 were **exception messages** |
| + `exclude` for `Exception(`, `throw`, `Result.failure` | **14, GameThor 0** | usable |

**`scan()` gained an `exclude` parameter** for this. **A path interpolated into a
diagnostic string is correct and extremely common**, and a class that counts
those is measuring logging.

## Every surviving hit read. No new instance.

| Hit | Verdict |
| --- | --- |
| ARMSX2 `invalidateCoverForPath`, `m_cover_pixmap_cache` | **known instance** |
| azahar, eden `GameIconUtils` keyer | **known instance** |
| eden `Game.keyAddedToLibraryTime`, `keyLastPlayedTime` | **known instance** |
| **eden `AddonViewModel.gameKey = "${programId}|${path}"`** | **correct** — an in-memory "is this still the same selection" check across an async load, not persisted |
| **eden `m_rom_metadata_cache[path]`** | **correct** — an in-process cache of FILE metadata, with a `clear()` |
| **Vita3K `m_size_cache.find(app.path)`** | **correct** — a file's size, keyed by the file |
| azahar, eden `chat_room.h icon_cache` | **correct** — multiplayer avatars, not games |
| ARMSX2 `controllerId = "shaderChain:preset:${p.path}"` | **correct** — an ephemeral UI focus id |

## The line the class exists to draw

> **A path is the correct key for a property OF THE FILE, and the wrong key for
> a property OF THE GAME.**

**Of the file:** size, modification time, an in-session metadata cache, "is this
still the same selection". **All correct, all found by the sweep, all dismissed.**

**Of the game:** settings, play history, art, cheats, saves. **Those must key on
`GameKey` or `DumpId`.**

**That distinction is now the class's `why` text**, so the next person reading a
hit spends seconds rather than minutes. **It could not have been written before
the sweep**, because it took eight legitimate uses to see what they had in
common.

## The score on DID_IT_APPLY.md's limit

**Second attempt today to have a tool find a new instance. Second failure.**

**But both attempts improved the instrument rather than nothing:** the first
found a class that could only match its own case and a vendored filter that
missed a whole fork; this one added `exclude` and produced the file-against-game
rule.

**The limit stands and should stay in the document.** Three tests of it now, and
the pattern is that **the tooling gets sharper and the fleet stays clean of the
classes it knows about.** The instances keep coming from reading something
nobody had read.

## Files

- `tools/bug_class_sweep.py` — new class `path_as_identity`, `scan(exclude=)`
