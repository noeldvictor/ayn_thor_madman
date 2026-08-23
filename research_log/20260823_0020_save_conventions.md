# Save conventions: the format cannot be shared, and that breaks the test harness

**Goal: close another "not surveyed in any fork" item.**

Session 2026-08-23 00:20. Save states matter twice over here: `CLAUDE.md` makes
them **mandatory** in the backend contract, and the test harness uses a
savestate as its **fixture** rather than playing to a scene.

**Result: the format is irreducibly per-backend, which is expected. The
consequence for the test harness is not, and nobody had stated it.**

---

## What each fork has

Filename search and mechanism search, per the rule.

| Fork | Implementation | Magic |
| --- | --- | --- |
| **ARMSX2** | `SaveState.cpp/h`, **plus `SaveStateLegacy.cpp`** | `kMagic` |
| melonDS | `Savestate.cpp/h` | **`MELN`**, `SAVESTATE_MAGIC` |
| Cemu | — | **`SSTATE`**, `kMagic` |
| azahar | `core/savestate.cpp/h` | not found |
| eden, Vita3K, xenia | not found by either search | — |

**The three not found are a limit of this survey, not a finding.** eden and
Vita3K certainly have save states; Vita3K's regression suite is built on them.
**Recorded as unread rather than absent**, because nine of ten negatives in this
repo have been wrong.

## The format cannot be shared, and nobody should try

A save state is a serialisation of guest machine state: guest CPU registers,
guest RAM, guest GPU registers, guest timers. **There is no shared structure
because there is no shared machine.** This is the same conclusion as texture
cache hashing and guest shader translation, and it needs no further argument.

**What the shared layer owns instead:**

- the slot convention and file naming
- the screenshot beside the state — melonDS has `SaveStateScreenshotProvider`,
  ARMSX2 has `SaveSlotLookup`
- the picker UI and the hotkeys, which are the same on every system
- **reporting a version mismatch as a mismatch**, rather than as a crash

---

## The finding: savestate versioning breaks the test harness, and it will

**`ARMSX2/SaveState.cpp` has a sibling called `SaveStateLegacy.cpp`.**

That file exists because the state format changed and old states still had to
load. **So this is not a hypothetical: it has already happened at least once in
the fork this project cares most about.**

Now combine that with two decisions already made:

- **The test harness uses a savestate as its fixture**, to cut a test from
  minutes to seconds. Taken from Vita3K, recorded in `CLAUDE.md` Phase 3.
- **The shared layer will reach into backends and change them**, deliberately,
  including hot paths it takes ownership of.

**A change that alters guest state layout invalidates every fixture for that
backend at once.** The golden images survive — they are just images — but
nothing can reach the scene that produced them, so the whole suite for that
console goes dark until the fixtures are regenerated.

**This is a real cost of the extraction plan and it was not priced.**

### What follows

1. **A fixture records the state version it was made with.** A harness that
   loads a fixture must fail loudly on mismatch, not silently produce a
   different scene. **A quietly wrong fixture is worse than a missing one**,
   because it produces a golden-image diff that looks like a rendering
   regression.
2. **Prefer a fixture that can be regenerated automatically.** If the recipe to
   reach the scene is recorded — a ROM hash plus an input replay — the fixture
   is rebuildable. azahar already has deterministic input replay in
   `core/movie.cpp`.
3. **Treat a state-format change as a schema migration**, the same class of
   problem as the settings schema, with the same two rules: version it, and do
   not deserialise old data with the current structure. ARMSX2's
   `SaveStateLegacy.cpp` is that lesson already learned once.

---

## Not read

- eden, Vita3K and xenia savestate implementations. **Found by neither search**,
  which means my terms were wrong, not that the code is absent.
- Whether any fork **refuses** a mismatched version rather than reading past it.
  **That is the question that matters most for the harness** and it needs the
  files opened, not grepped.
- Where each fork writes its states on Android. The app owns paths, so this has
  to be answered before conversion.
