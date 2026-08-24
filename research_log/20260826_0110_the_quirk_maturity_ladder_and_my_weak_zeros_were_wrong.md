# The quirk maturity ladder, and my weak zeros were wrong

**Goal: I recorded zeros for Cemu, azahar, Vita3K and melonDS on game quirks and
flagged them as weak, because I had searched four NAMES. Search the MECHANISM.**

**Device-free: two greps and one file read. No device used.**

## The name search and the mechanism search disagree completely

**Names** — `gamefix|game_?quirk|GameDatabase|per-title hack`:

| Fork | Files |
| --- | --- |
| ARMSX2 | 67 |
| **Cemu, azahar, Vita3K, melonDS** | **0** |

**Mechanism** — a hardcoded title identifier driving a comparison:

| Fork | Files |
| --- | --- |
| **Cemu** | **31** |
| **azahar** | **22** |
| **Vita3K** | **12** |
| **xenia** | **11** |
| melonDS | 1 |

> **Four zeros became 31, 22, 12 and 1.** I flagged them as weak and they were
> worse than weak — **they were the wrong question.** Third time today the
> search-for-a-name trap has caught me, and the first time I had already written
> the warning into the same document.

## What the hits actually are

**A hit is a shape.** Read one — Cemu's largest cluster,
`coreinit_Memory.cpp:160`:

```cpp
// here we artificially reduce the available memory for the affected games
uint64 titleId = CafeSystem::GetForegroundTitleId();
if (titleId == 0x0005000010132400ULL || // Lego Marvel Super Heroes (EU)
    titleId == 0x0005000010194200ull || // Lego Dimensions (US)
    titleId == 0x00050000101A6200ull || // Lego Jurassic World (US)
    ... ~15 more Lego titles ...
```

**That is a game quirk by any definition** — it changes the emulator's reported
memory for named titles. **It is simply not called one.**

## The ladder, and it is a design argument

| Fork | Form | Consequence |
| --- | --- | --- |
| **ARMSX2** | **18 named bits, persisted, user-toggleable, indexed by `GamefixId`** | listable, overridable, and a stored mask survives |
| **XenDroid** | a declared `game_quirks.{cc,h}` module, flipped per title at launch | listable; **new title needs a rebuild** |
| **Cemu** | **an inline `if` chain of ~15 title IDs inside a memory function** | **not listable, not overridable, new title needs a rebuild** |

> **Same category, three maturities.** And the bottom rung has a cost this
> project cares about specifically: **`CLAUDE.md` promises every option is
> overridable per game.** A quirk with no key **cannot be offered, cannot be
> turned off, and cannot be recorded in a per-game profile** — it is invisible to
> the whole override system.

**And it is the THIRD instance of a lesson this repo already wrote down.** From
GameThor: *"GameThor's fixes are CODE, so adding one needs a rebuild, and
Foundation point 4 requires installing a fix to take one action in the app — so
ship them as DATA."*

> **Quirks are the same argument in a new subsystem.** ARMSX2 is halfway there —
> named and persisted, but the bits are still compiled in. **Nobody in the fleet
> ships quirks as data.**

## What I am correcting

**`CLAUDE.md` now says "nineteen instances across two forks".** That was written
from the name search and is understated: **the category is present in at least
three forks and probably five**, with Cemu's alone covering fifteen titles in one
`if`.

**The corrected claim: ARMSX2 has the only DECLARED quirk taxonomy. The category
itself is everywhere.**

## Limits

- **One file read out of 77 hits.** The other clusters — azahar 22, Vita3K 12,
  xenia 11 — **were not opened**, and some fraction will be save routing, content
  lookup or logging rather than quirks. **The counts are an upper bound on
  quirks and a lower bound on the category being present.**
- **melonDS's single hit was not read.** The DS is a simple guest and one hit may
  well be nothing.
- **No claim about how many of Cemu's 31 files are quirks.** One is, decisively.
- **Nothing built or run, no device.**

## Sources

- Cemu `src/Cafe/OS/libs/coreinit/coreinit_Memory.cpp:160-180`
- `research_log/20260826_0020_the_fifth_patch_kind_has_eighteen_more_instances.md`
- `CLAUDE.md`, the GameThor per-game fixes entry
