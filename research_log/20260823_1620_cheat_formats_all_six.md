# The remaining four cheat formats

**Goal: finish the survey started in
[`20260823_1532`](20260823_1532_cheat_format_mapping.md), which covered pnach,
Gateway and dmnt and left four formats unchecked.**

No device. Reading only.

## The complete picture

| Format | Guest | Shape | Dominant opcode |
| --- | --- | --- | --- |
| **dmnt** | Switch | **bytecode VM** — the proposed target | — |
| Gateway / AR | 3DS | interpreted opcodes | writes |
| **AR DS** | DS | interpreted opcodes **plus loops and a data register** | writes |
| pnach | PS2 | line format with an `extended` sub-opcode set | writes |
| **VitaCheat `.psv`** | Vita | static writes, **plus code writes** | writes |
| **rpcs3 YAML** | PS3 | typed list, hash-keyed | **`be32`, 84%** |
| **`.ncl`** | PS3 | flat text with an author line | **`0`, 66%** |

## The distribution is extremely skewed, and that is the design input

**Measured, not estimated.**

rpcs3 YAML, across 173 bundled files and 183 distinct executable hashes:

| type | entries |
| --- | --- |
| **`be32`** | **5,467** |
| `load` | 1,013 |
| `be16` | 8 |
| `byte` | 6 |
| `bef32` | 2 |
| `jump` | 1 |

`.ncl`, across **2,501 files and 91,372 code lines**:

| type | lines |
| --- | --- |
| **`0`** — 32-bit write | **60,091** |
| `B` | 10,353 |
| `6` | 2,649 |
| `4` | 51 |

> **One opcode — a sized write at an address — is 66% to 84% of every real cheat
> in the fleet's bundled databases.**

**So the VM's complexity lives entirely in the rare tail.** A tiered engine is
the right shape, and this is the number behind it: a fast path for the flat
write covers most of the corpus, and the VM handles the remainder.

**This confirms the ladder `CLAUDE.md` already recorded** — rpcsx typed pokes,
azahar polymorphic objects, eden bytecode — and puts a weight on the bottom rung.

## A fourth gap in dmnt, and it is the interesting one

**VitaCheat has `$A000`, `$A100` and `$A200`: ARM code writes, "with JIT cache
invalidation when bytes change".**

**dmnt cannot express this, and it is not an oversight.** Atmosphere runs on
real Switch hardware. **There is no recompiler to invalidate.** Every other gap
found so far is guest knowledge; **this one is emulator knowledge.**

**It joins the block-cache rule directly.** `BlockCacheKey` in `app/shell` exists
because baking the FP environment into generated code makes that code stale when
the environment changes. **A cheat that writes guest code is the same hazard from
the other direction:** the code changed under a block that was already compiled.

**The shared VM therefore needs a write that declares it targets executable
memory**, and the backend must invalidate the affected range. **A cheat engine
that only writes data will silently do nothing on a recompiled guest**, or worse,
work until the block is recompiled.

**Vita3K is the only fork that has met this**, because it is the only one running
cheats against a JIT with a published code-write opcode.

**Method:** all six formats were read — dmnt's opcode enum, azahar's Gateway
dispatch comments, melonDS's `AREngine.cpp` switch, ARMSX2's `Patch.h` enums,
Vita3K's `cheats/README.md`, and the rpcs3 YAML and `.ncl` corpora. **No other
engine has a code-write opcode.**

## Endianness, confirmed a third time

The first log found dmnt has **zero** endianness handling while pnach carries
`beshort`, `beword` and `bedouble`.

**Two more independent confirmations:** rpcs3's vocabulary is `be32`, `be16` and
`bef32` — and `be32` alone is 84% of its entries. **Both PS2 and PS3 are
big-endian guests**, so this is not a corner case; it is the majority of two of
the fleet's formats.

**`bef32` is a big-endian float, and it is not a fifth width.** A float write is
bit-identical to a 32-bit integer write once the value is encoded. **It is a
presentation type in the format, not an opcode in the VM** — the front end
converts it. Worth stating so nobody adds a float path to the engine.

## AR DS: loops and a data register, all of which map

melonDS's `AREngine.cpp` is the richest interpreted engine in the fleet:

- `0x00`/`0x10`/`0x20` — 32/16/8-bit writes → dmnt `StoreStatic`
- `0x30`–`0x60` — conditionals → `BeginConditionalBlock`
- `0x70`–`0xA0` — masked 16-bit conditionals → same
- `0xB0` — pointer deref → `LoadRegisterMemory`
- **`0xC0` FOR / `0xD1` NEXT / `0xD2` NEXT+FLUSH** → `ControlLoop`
- **`0xD4` data op** — nine operations: `+`, `|`, `&`, `^`, shifts, `*` →
  `PerformArithmeticRegister`
- `0xD5`–`0xDB` — data register load/store at three widths →
  `StoreRegisterToAddress`, `LoadRegisterMemory`
- `0xE0`/`0xF0` — block copy → `ControlLoop` plus a store

**Everything maps.** The layout is close enough to Gateway's that the shared
Action Replay ancestry is obvious — same opcode numbering for writes,
conditionals and pointer deref.

**One documented dead corner.** melonDS does not implement `0xC4`, a
self-modifying-code trick, and its comment asks *"does anything even use it??"*
**Record it as refused rather than missing**, so nobody implements it for
completeness.

## Provenance is already first-class, and CLAUDE.md wants it elsewhere

**Both PS3 formats carry authorship.** `.ncl` has an author line per cheat —
`DANNY G` in the sample read. rpcs3 YAML carries `Author`, `Notes` and
`Patch Version`, and is keyed by **`PPU-<hash>`**, with 183 distinct hashes
across 173 files.

**Two things follow.**

`CLAUDE.md` says GameThor's per-game fixes lack a provenance field and that "a
fix with no recorded source cannot be re-derived when it stops working". **The
cheat formats solved this already.** Take the field list rather than inventing
one: author, notes, version.

**And `PPU-<hash>` is `DumpId` in production.** `app/GAME_DATA.md` separates
`GameKey` from `DumpId` on the argument that a cheat targets a title while a
patch targets a build. **rpcs3's corpus is keyed the second way**, which is
evidence for the split rather than against it.

## The revised addition list for the shared VM

From both logs together:

| # | Addition | Why |
| --- | --- | --- |
| 1 | **byte swap** | pnach and rpcs3; `be32` alone is 84% of one corpus |
| 2 | **backend-declared region list** | `EE`/`IOP`, `MainNso`/`Heap`, VitaCheat's four module segments |
| 3 | **a code write that invalidates the block cache** | **VitaCheat `$A000`; emulator knowledge, not guest knowledge** |
| — | scheduling | **not an opcode.** Metadata on the cheat |
| — | float types | **not an opcode.** The front end encodes it |

## Limits

- **Cemu graphic-pack patches are still unread** as a cheat format. They are
  already recorded as the best *patch* engine in the fleet, which is a different
  question.
- **`.ncl` types `B`, `6` and `4` were not decoded**, only counted. `B` is 11%
  of the corpus and worth identifying before the front end is written.
- **~18,000 `.ncl` lines did not match the three-field pattern** counted here, so
  the type tally is of the 73,144 that did.
- **rpcsx is GPL-2.0-only.** Its formats and corpus are facts; its code is not
  available.
