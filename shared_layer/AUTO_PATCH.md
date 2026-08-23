# Can a game analyser make patches by itself

**Yes, for one class, and the evidence says that class is nearly mechanical. No,
for the class people usually mean.**

**Written 2026-08-23** from the fleet's own patch corpus and tooling. Companion
to [`AGENT_LOOP.md`](AGENT_LOOP.md), which supplies the step that was missing.

---

## The corpus says the patches are formulaic

**2,676 per-game files ship in rpcsx alone.** They are labelled with intent, and
the labels are dominated by one shape:

| Intent | Count |
| --- | --- |
| infinite health | 59 |
| infinite ammo | 26 |
| invincibility | 20 |
| infinite HP | 12 |
| health never decreases | 12 |
| rapid fire, infinite stamina | 8 each |
| ammo never decreases, no reload, max accuracy, invisibility | 6-7 each |

**Almost every one is "X never decreases".**

**And the values confirm the implementation.** Of the `be32` entries parsed:

| Value | PowerPC meaning | Count |
| --- | --- | --- |
| **`0x60000000`** | **`nop`** | **67** |
| `0x4e800020` | `blr` — return immediately | 9 |
| `0x48000020` | branch forward, skip a block | 6 |
| `0x388003e7` | `li r0, 999` — force a constant | 3 |

> **The dominant patch in the corpus is "stop this instruction from happening".**

**Authoring is therefore trivial.** The hard part was never writing the patch.
**It was finding the address.**

---

## Every step of finding it already exists, in a different fork each time

**This is the classic cheat-search loop, and the fleet has all of it:**

| Step | What it needs | Where it exists |
| --- | --- | --- |
| 1. Scan memory for a value | a typed comparison scanner | **azahar `cheats/memory_search.cpp`** — `MemorySearchComparison`, `MemorySearchCandidate`, `ScanResult` |
| 2. Change the value in-game | drive the game deterministically | **the paused agent loop** — pause, look, inject, resume |
| 3. Rescan and narrow | greater/less/changed/unchanged | azahar, same file |
| 4. Find the code that writes it | a write watchpoint | **xenia (3 files), ARMSX2, Cemu** |
| 5. Author the patch | replace with `nop` | **67 of the parsed corpus values** |
| 6. Verify | did the value stop changing | step 1 again |

**None of this is speculative. All six exist. The loop does not.**

**Step 2 is the one that was genuinely missing**, and it is the one the paused
agent loop supplies: **a scan needs the value to change between passes**, which
means playing the game. **An agent that can pause, look, press and resume can do
that pass as many times as the search needs.**

---

## What this does NOT automate, and the corpus proves it

**Performance patches are not in that corpus.** It is a cheat corpus. **59
"infinite health" entries and no "60 FPS" cluster.**

**Three reasons the same loop does not transfer to optimisation:**

- **The target is not a value.** "Health" is a number in memory that a scan can
  find. "The frame is 100 ms" is not an address.
- **The intent is not observable.** A scan can prove health stopped decreasing.
  **Nothing equivalently cheap proves a frame got faster** — that needs the
  measurement discipline, and this fleet's own noise floors are **±0.2% on a
  gated title screen and ~±50% pressing through cutscenes.**
- **The fleet's record is split.** Per-game HLE work stands at **39 `WIN`, 33
  `DEAD`, 8 `FLAT`, 5 `CONFOUNDED`.** **An automated patcher aimed at
  performance would generate candidates from the same distribution**, and a
  third of them would be dead ends that each cost a device run to reject.

> **Automate the class where verification is cheap. Cheats qualify. Performance
> does not, yet.**

---

## What a first version would actually be

**A cheat finder, not an optimiser**, and it is a product feature rather than a
harness:

1. The person says what they want — *"stop losing health"*.
2. The agent drives to a scene where health changes.
3. Scan, take damage, rescan, narrow to one address.
4. Watchpoint it, catch the writing instruction.
5. Emit `nop` at that address, keyed to the title and the build hash.
6. Verify by taking damage again.

**Foundation point 4 is the argument for it.** Finding, installing and enabling a
cheat is a research task in RetroArch, and this project calls that a named
failure. **A cheat that the app finds for you is the strongest possible answer to
it.**

**And the output is data, not code** — a patch file keyed by `GameKey` and
`DumpId`, in the shared format, which the existing engine already runs.

---

## Binding it to the right build

**Three forks solve this three ways and rpcs3's corpus proves the method
matters**: Cemu matches a loaded `RPLModule`, rpcsx hashes, eden uses a 32-byte
`BuildID`, and **the rpcs3 corpus is keyed by `PPU-<hash>` across 183 distinct
hashes in 173 files.**

**An auto-generated patch must carry the hash it was found against**, or it will
be applied to a build whose addresses have moved. **That is `DumpId` from
[`../app/GAME_DATA.md`](../app/GAME_DATA.md), and this is the case that
justifies it.**

---

## What is not claimed

- **Nothing is built.** Six pieces exist in six forks; the loop exists nowhere.
- **No accuracy number.** How often a scan narrows to one address on a console
  guest, and how often the watchpoint catches the right instruction, is
  unmeasured.
- **`nop` is not always safe.** Removing a store can desynchronise game state as
  easily as it can grant invincibility. **The corpus contains the patches that
  worked; it does not record the ones that broke a save.**
- **The corpus counts files, not distinct titles**, and only about a quarter of
  its `be32` entries were parsed by the strict pattern used here.
