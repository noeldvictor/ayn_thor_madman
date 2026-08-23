# The fleet is already sharing code, badly

**Emulators have been copying each other for eighteen years. The sharing
already happens. It just happens in the worst possible way.**

Evidence gathered 2026-08-22 from copyright headers in the forks themselves.

## The web

| Fork | Foreign code it carries |
| --- | --- |
| **Vita3K-Thor** | Dolphin 2013 x6, Dolphin 2016 x2, Citra x3, yuzu x2 |
| **melonDS-android** | **Dolphin 2008 x8, Dolphin 2009 x3** |
| **eden-thor** | yuzu, **over 2,000 files** |
| **azahar-thor** | Citra, hundreds of files |
| **ARMSX2** | PCSX2, wholesale, under SPDX headers |
| **rpcsx-ui-android** | rpcs3, wholesale |
| Cemu-thor | none found |
| xenia-thor | none found |

**melonDS-android is carrying Dolphin code written in 2008.** Vita3K carries
code from three different emulators. Neither fact appears anywhere in either
project's description.

Five ancestors account for most of the fleet: **Dolphin, Citra, yuzu, PCSX2 and
rpcs3.** Only Cemu and xenia are independent lineages.

## Why this matters more than the duplication itself

**The alternative to unification was never "everyone writes their own".** It is
what actually happened: **everyone copies once, then diverges forever, and
nobody receives the fixes.**

Vita3K took Dolphin's Android touch overlay in 2013. Dolphin has improved that
overlay for twelve years since. **Vita3K has none of it.** azahar took the same
code through Citra and diverged separately, so the two copies in this fleet are
now 1302 lines of Kotlin and 1067 lines of Java that started as one file.

That is the failure mode:

1. Project A solves a problem.
2. Projects B and C copy the solution.
3. All three improve it separately.
4. **Nobody can take anybody's improvement, because the code has drifted.**
5. A bug fixed in A stays broken in B and C forever.

**Informal copying gives you the initial value and none of the compounding.**

## What it proves about this project

The unification thesis is not speculative. **It is a formalisation of something
the ecosystem already does.**

The question was never "should emulators share code". They already do, at
scale, across a decade. The question is only whether the sharing is **tracked
and maintained** or **copied and abandoned**.

This repo's structural answer is the difference:

- `capability_inventory.md` tracks who has what, so a copy is visible.
- `shared_layer/OWNED.md` records what the shared layer owns, so a fix lands
  once.
- The build guard makes a fork unable to grow a private second copy.
- The provenance rule records where a thing came from and why.

**None of that exists in the informal version.** Vita3K's overlay does not know
it came from Dolphin except in a copyright line nobody reads.

## The pattern this predicts

**Shared ancestry is a better duplication predictor than shared purpose.**

Confirmed by every survey so far:

| Looked duplicated because | Result |
| --- | --- |
| Three forks have an LRU cache, same purpose | **three different designs** |
| Six forks have a driver picker, same purpose | **four different concerns** |
| Two forks have `InputOverlay*`, **same ancestor** | **one design, twice** |
| Two forks have `DiskShaderCacheProgress.kt`, **same ancestor** | same design |
| Two forks have `id_cache.cpp`, **same ancestor** | same design |

**Same purpose predicts nothing. Same ancestor predicts duplication reliably.**

### How to use it

**Search for shared ancestors, not shared features.** A copyright header, an
identical class name or a matching file name across two forks is stronger
evidence than any amount of "they both need to do X".

```sh
git -C <fork> grep -hoiE 'Copyright [0-9-]* (Dolphin|Citra|yuzu|PCSX2|RPCS3|melonDS) [A-Za-z]*' \
  | sort | uniq -c | sort -rn
```

Two forks with the same ancestor and the same file name are the same code,
diverged. Extract those first.

## What is still unknown

- **How far each copy has drifted.** Vita3K's overlay and azahar's started as
  one file; nobody has diffed them.
- **Whether the ancestors have fixes worth back-porting.** Dolphin's overlay
  has twelve years of improvement that Vita3K never received. That is free work
  sitting upstream of a fork nobody thinks of as having an upstream.
- **ARMSX2 and rpcsx use SPDX headers**, so the search above missed them. A
  second pass keyed on `SPDX-FileCopyrightText` would find more.
- Cemu and xenia showed nothing, which may mean independent, or may mean they
  attribute differently.

## The uncomfortable part

**This fleet's forks have upstreams they do not track.**

melonDS-android carries Dolphin 2008 code. Vita3K carries Dolphin 2013 code.
Neither lists Dolphin as an upstream remote, so neither will ever see a fix.

The provenance rule in `CLAUDE.md` says to record what was taken from where and
why. **These forks took a great deal and recorded it only in a copyright line.**
Doing better than that is most of what this project is.
